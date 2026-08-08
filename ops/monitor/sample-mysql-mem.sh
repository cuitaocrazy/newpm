#!/bin/bash
# ============================================================================
# newpm MySQL 内存采样器（Issue #34）
#
# 目的：回答「2Gi 限额下，RSS 增长是线性还是饱和」——这是判断 OOM 会不会复发的
#      唯一手段。集群没有 Prometheus，cgroup 计数器随 pod 销毁清零，
#      1GiB 时代的增长曲线已永久不可重建，所以必须从现在开始自己攒数据。
#
# ⚠️ 必须部署在 mysql-0 所在的节点（当前是 k3s003），全程读宿主机 /proc 与 cgroup。
#    **绝对不要用 kubectl exec 采样** —— 每次 exec 会往被测 cgroup 里打 4 MB
#    （实测值），采样器自己就成了污染源，这正是 Issue #34 里探针的问题。
#
# 全程只读：只 cat /proc 与 /sys/fs/cgroup 下的文件，不连数据库、不改任何配置。
#
# 用法：sample-mysql-mem.sh            # 采一次，追加到 CSV
#      sample-mysql-mem.sh --header   # 打印表头说明后退出
#
# 版本管理副本：ops/monitor/sample-mysql-mem.sh（改脚本要同步宿主机与仓库两边）
# ============================================================================

set -uo pipefail

CSV="/var/log/newpm-mysql-mem.csv"
ALERT_LOG="/var/log/newpm-mysql-alert.log"

# 预警阈值（依据 Issue #34：OOM 前 cgroup 会长期贴顶抖动，有数天预警期）
WARN_RSS_MB=1600        # 🟡 距 2Gi 天花板约 400MB，历史推算余量 8.5~15 天
WARN_FILE_MB=50         # 🟠 页缓存被挤压（健康值约 150MB；OOM 现场只剩 188KB）
CRIT_RSS_MB=1850        # 🔴 随时可能被任一次内存申请引爆

MB=$((1024 * 1024))

if [ "${1:-}" = "--header" ]; then
    cat <<'EOF'
CSV 字段说明（/var/log/newpm-mysql-mem.csv）

  ts                  采样时刻
  pod_uid             pod 的 UID 前 8 位。变化 = pod 被重建过，
                      此后的计数器全部从零开始，绘图时必须按此分段
  mysqld_uptime_s     mysqld 进程运行秒数（判断增长速率的分母）
  vmrss_kb            进程驻留内存 —— 【主指标】
  vmdata_kb           堆虚拟大小。与 vmrss 的差额反映 glibc 已向 OS 索取
                      但未归还的空间
  cg_current_mb       cgroup 当前用量（含页缓存）
  cg_peak_mb          cgroup 历史峰值（不随 current 回落）
  cg_max_mb           limit
  anon_mb             匿名内存（不可回收）—— 真正逼近天花板的是它
  file_mb             页缓存（可回收）。持续下降 = 内存压力，预警信号
  anon_thp_mb         透明大页占用
  thp_fault_alloc     缺页时直接分配大页的累计次数
  thp_collapse_alloc  khugepaged 事后折叠的累计次数
  pgscan_direct       直接回收扫描页数。由 0 转正 = 已进入内存压力
  pgsteal_direct      直接回收实际回收页数
  ws_refault_file     页缓存重新换入次数。高 = 缓存被反复挤掉又读回，性能已劣化
  oom_cnt             memory.events 的 oom 计数
  oom_kill_cnt        memory.events 的 oom_kill 计数 —— 不依赖 journalctl 的 OOM 直接证据
  arena_cnt           glibc 的 64MiB 对齐匿名映射数量（arena 个数，上限 8×nproc）

判读要点
  线性增长 → 第 3 天与第 7 天的日增速无明显下降 ⇒ 2Gi 只是延期，需计划内重启兜底
  饱和增长 → 日增速逐日衰减，vmrss 在某水位形成平台（连续 3 天增幅 <5MB/天）⇒ 2Gi 够用
  arena_cnt 停止增长而 vmrss 仍涨 ⇒ 增量在既有 arena 内部，是碎片而非新分配
EOF
    exit 0
fi

# ── 定位 mysqld（pod 重建后 PID 会变，每次重新解析）────────────────────────
PID=$(pgrep -f "mysqld --character-set-server" | head -1)
if [ -z "$PID" ]; then
    echo "$(date '+%F %T'),mysqld_not_found" >> "$ALERT_LOG"
    exit 0   # 不报错刷屏：pod 正在重建时这是正常状态
fi

CG_REL=$(sed 's/^0:://' "/proc/$PID/cgroup" 2>/dev/null | head -1)
CG="/sys/fs/cgroup${CG_REL%/*}"
[ -d "$CG" ] || { echo "$(date '+%F %T'),cgroup_not_found,$CG" >> "$ALERT_LOG"; exit 0; }

# pod UID：cgroup 路径里的 pod<uid> 段，用于识别 pod 重建
POD_UID=$(echo "$CG_REL" | grep -oE 'pod[0-9a-f_]{8}' | head -1 | cut -c4-11)

read_stat() { grep -m1 "^$1 " "$CG/memory.stat" 2>/dev/null | awk '{print $2}'; }
read_evt()  { grep -m1 "^$1 " "$CG/memory.events" 2>/dev/null | awk '{print $2}'; }

TS=$(date '+%F %T')
UPTIME_S=$(awk '{print int($1)}' /proc/uptime)
PROC_START_TICKS=$(awk '{print $22}' "/proc/$PID/stat" 2>/dev/null)
HZ=$(getconf CLK_TCK 2>/dev/null || echo 100)
MYSQLD_UPTIME=$(( UPTIME_S - PROC_START_TICKS / HZ ))

VMRSS=$(grep -m1 '^VmRSS:'  "/proc/$PID/status" | awk '{print $2}')
VMDATA=$(grep -m1 '^VmData:' "/proc/$PID/status" | awk '{print $2}')

CG_CUR=$(cat "$CG/memory.current" 2>/dev/null || echo 0)
CG_PEAK=$(cat "$CG/memory.peak" 2>/dev/null || echo 0)
CG_MAX=$(cat "$CG/memory.max" 2>/dev/null || echo 0)

ANON=$(read_stat anon);            FILE=$(read_stat file)
ANON_THP=$(read_stat anon_thp)
THP_FAULT=$(read_stat thp_fault_alloc); THP_COLLAPSE=$(read_stat thp_collapse_alloc)
PGSCAN=$(read_stat pgscan_direct);  PGSTEAL=$(read_stat pgsteal_direct)
WS_REFAULT=$(read_stat workingset_refault_file)
OOM=$(read_evt oom);                OOM_KILL=$(read_evt oom_kill)

# arena 数量：64MiB 对齐的匿名 rw 映射（NF==5 即 pathname 为空 = 匿名）。
# ⚠️ 不能用 strtonum()：那是 gawk 扩展，Debian 默认的 mawk 不支持，
#    会静默返回 0（实测踩过）。改用正则判断对齐——64MiB = 0x4000000，
#    故地址末 6 位必为 000000，且第 7 位只能是 0/4/8/c。
ARENA_CNT=$(awk '$2 ~ /^rw/ && NF == 5 {
    split($1, a, "-");
    if (a[1] ~ /[048c]000000$/) n++
} END { print n+0 }' "/proc/$PID/maps" 2>/dev/null || echo 0)

[ -f "$CSV" ] || echo "ts,pod_uid,mysqld_uptime_s,vmrss_kb,vmdata_kb,cg_current_mb,cg_peak_mb,cg_max_mb,anon_mb,file_mb,anon_thp_mb,thp_fault_alloc,thp_collapse_alloc,pgscan_direct,pgsteal_direct,ws_refault_file,oom_cnt,oom_kill_cnt,arena_cnt" > "$CSV"

printf '%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n' \
    "$TS" "${POD_UID:-unknown}" "$MYSQLD_UPTIME" "${VMRSS:-0}" "${VMDATA:-0}" \
    "$(( CG_CUR / MB ))" "$(( CG_PEAK / MB ))" "$(( CG_MAX / MB ))" \
    "$(( ${ANON:-0} / MB ))" "$(( ${FILE:-0} / MB ))" "$(( ${ANON_THP:-0} / MB ))" \
    "${THP_FAULT:-0}" "${THP_COLLAPSE:-0}" "${PGSCAN:-0}" "${PGSTEAL:-0}" \
    "${WS_REFAULT:-0}" "${OOM:-0}" "${OOM_KILL:-0}" "$ARENA_CNT" >> "$CSV"

# ── 预警（只记日志，不做任何自动处置）──────────────────────────────────────
RSS_MB=$(( ${VMRSS:-0} / 1024 ))
FILE_MB=$(( ${FILE:-0} / MB ))

if [ "${OOM_KILL:-0}" -gt 0 ]; then
    echo "$TS,[RED],oom_kill=$OOM_KILL,本 pod 生命周期内已发生 OOM 击杀" >> "$ALERT_LOG"
elif [ "$RSS_MB" -gt "$CRIT_RSS_MB" ]; then
    echo "$TS,[RED],vmrss=${RSS_MB}MB>${CRIT_RSS_MB}MB,距天花板不足200MB，立即安排重启" >> "$ALERT_LOG"
elif [ "$FILE_MB" -lt "$WARN_FILE_MB" ] || { [ "${PGSCAN:-0}" -gt 0 ] && [ "${PGSTEAL:-0}" -gt 0 ]; }; then
    echo "$TS,[ORANGE],file=${FILE_MB}MB pgscan=${PGSCAN:-0},页缓存被挤压/已进入直接回收，数据库性能正在劣化" >> "$ALERT_LOG"
elif [ "$RSS_MB" -gt "$WARN_RSS_MB" ]; then
    echo "$TS,[YELLOW],vmrss=${RSS_MB}MB>${WARN_RSS_MB}MB,排期一次低峰期计划内重启" >> "$ALERT_LOG"
fi
