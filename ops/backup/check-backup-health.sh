#!/bin/bash
# newpm 备份健康巡检
# 本地只留 2 天热层、OSS 为主存储，因此"上传静默失败"必须能被主动发现。
# 退出码：0=健康，非0=发现问题数（cron 会把输出发到 root 邮箱/日志）
export PATH=/usr/local/bin:/usr/bin:/bin

BUCKET="yada-newpm-backup"
DB_DIR="/backup/newpm-mysql"
UP_DIR="/backup/newpm-upload"
LOG="/backup/backup-health.log"
ISSUES=0

say()  { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG"; }
bad()  { say "  [异常] $*"; ISSUES=$((ISSUES+1)); }
ok()   { say "  [正常] $*"; }

say "========== 备份健康巡检 =========="

# --- 1. 磁盘空间 ---
AVAIL_GB=$(df -BG --output=avail /backup | tail -1 | tr -dc '0-9')
USE_PCT=$(df --output=pcent /backup | tail -1 | tr -dc '0-9')
if [ "${AVAIL_GB:-0}" -lt 10 ]; then
  bad "磁盘空闲仅 ${AVAIL_GB}G（已用 ${USE_PCT}%），低于 10G 阈值"
else
  ok "磁盘空闲 ${AVAIL_GB}G（已用 ${USE_PCT}%）"
fi

# --- 2. 本地最新数据库备份新鲜度（每 6 小时一次，容忍 7 小时）---
FRESH_DB=$(find "$DB_DIR" -name 'newpm-*.sql.gz' -mmin -420 2>/dev/null | wc -l)
LOCAL_DB=$(ls -1 "$DB_DIR"/newpm-*.sql.gz 2>/dev/null | wc -l)
if [ "$FRESH_DB" -lt 1 ]; then
  bad "7 小时内无新数据库备份（本地共 ${LOCAL_DB} 份）——定时任务可能已停"
else
  ok "数据库备份新鲜（7小时内 ${FRESH_DB} 份，本地共 ${LOCAL_DB} 份）"
fi

# --- 3. 本地最新附件备份新鲜度（每日一次，容忍 26 小时）---
FRESH_UP=$(find "$UP_DIR" -name 'newpm-upload-*.tar.gz' -mmin -1560 2>/dev/null | wc -l)
LOCAL_UP=$(ls -1 "$UP_DIR"/newpm-upload-*.tar.gz 2>/dev/null | wc -l)
if [ "$FRESH_UP" -lt 1 ]; then
  bad "26 小时内无新附件备份（本地共 ${LOCAL_UP} 份）——定时任务可能已停"
else
  ok "附件备份新鲜（26小时内 ${FRESH_UP} 份，本地共 ${LOCAL_UP} 份）"
fi

# --- 4. OSS 今日对象（核心：验证上传链路真的在工作）---
TODAY=$(date +%Y%m%d)
OSS_DB_TODAY=$(ossutil ls "oss://${BUCKET}/newpm-mysql/newpm-${TODAY}" 2>/dev/null | grep -c 'sql.gz' || true)
if [ "${OSS_DB_TODAY:-0}" -lt 1 ]; then
  bad "OSS 上今日($TODAY)无数据库备份——上传链路可能已断"
else
  ok "OSS 今日数据库备份 ${OSS_DB_TODAY} 份"
fi

OSS_UP_TODAY=$(( $(ossutil ls "oss://${BUCKET}/newpm-upload/daily/newpm-upload-${TODAY}" 2>/dev/null | grep -c 'tar.gz' || true) \
              + $(ossutil ls "oss://${BUCKET}/newpm-upload/monthly/newpm-upload-${TODAY}" 2>/dev/null | grep -c 'tar.gz' || true) ))
if [ "$OSS_UP_TODAY" -lt 1 ]; then
  bad "OSS 上今日($TODAY)无附件备份——上传链路可能已断"
else
  ok "OSS 今日附件备份 ${OSS_UP_TODAY} 份"
fi

# --- 5. 本地热层每一份都必须在 OSS 上有副本（本地随时会被清理，OSS 是主存储）---
MISSING=0
for f in "$DB_DIR"/newpm-*.sql.gz; do
  [ -e "$f" ] || continue
  n=$(basename "$f"); lsz=$(stat -c%s "$f")
  rsz=$(ossutil ls "oss://${BUCKET}/newpm-mysql/$n" 2>/dev/null | awk -v t="oss://${BUCKET}/newpm-mysql/$n" '$NF==t{print $5}')
  [ "$lsz" = "$rsz" ] || { MISSING=$((MISSING+1)); say "    缺失/不一致: $n (本地=$lsz OSS=${rsz:-无})"; }
done
if [ "$MISSING" -gt 0 ]; then
  bad "有 ${MISSING} 份本地数据库备份未同步到 OSS"
else
  ok "本地数据库备份全部已同步 OSS"
fi

# --- 6. 近期上传日志中的错误 ---
RECENT_ERR=$(tail -200 /backup/oss-sync.log 2>/dev/null | grep -c 'ERROR' || true)
if [ "${RECENT_ERR:-0}" -gt 0 ]; then
  bad "oss-sync.log 近 200 行中有 ${RECENT_ERR} 条 ERROR"
else
  ok "上传日志无错误"
fi

say "========== 巡检结束：问题数 ${ISSUES} =========="
exit "$ISSUES"
