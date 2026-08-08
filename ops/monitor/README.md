# newpm 生产 MySQL 内存采样

配合 [Issue #34](https://github.com/cuitaocrazy/newpm/issues/34)。目的只有一个：

> **回答「2Gi 限额下 RSS 增长是线性还是饱和」——这决定 OOM 会不会复发。**

集群没有 Prometheus / VictoriaMetrics / Grafana，`metrics-server` 只提供瞬时值，cgroup 计数器随 pod 销毁清零，`kubectl logs --previous` 对已重建的 pod 返回 not found。**1 GiB 时代的增长曲线已经永久不可重建**——所以只能从现在开始自己攒。

## 部署

| 项 | 值 |
|---|---|
| 脚本 | `/usr/local/bin/sample-mysql-mem.sh`（本目录是版本管理副本，改动要同步两边） |
| 节点 | **k3s003**（`mysql-0` 当前所在节点。若 pod 被调度到别的节点，采样会静默停止，需重新部署） |
| 频率 | `*/5 * * * *`（root crontab），288 行/天 |
| 数据 | `/var/log/newpm-mysql-mem.csv`（约 43 KB/天，一年 15 MB） |
| 预警 | `/var/log/newpm-mysql-alert.log`（只记录，不做任何自动处置） |

## ⚠️ 为什么必须在宿主机采，不能用 `kubectl exec`

每次 `kubectl exec` 会在被测 pod 的 memory cgroup 内 fork `runc init`，**瞬时占用 4 MB**（宿主机 0.8 ms 采样 48903 次实测）。用它采样等于让采样器自己成为污染源——这正是 Issue #34 里那个 exec 探针的问题（17280 次/天，是 5/5 次 OOM 的引爆器）。

本脚本全程只读宿主机的 `/proc/<pid>/*` 与 `/sys/fs/cgroup/**`，对被测对象零扰动。

## 查看

```bash
# 字段说明
ssh k3s003 "sudo /usr/local/bin/sample-mysql-mem.sh --header"

# 最近 12 个采样点的核心指标
ssh k3s003 "sudo tail -12 /var/log/newpm-mysql-mem.csv" | \
  awk -F, 'NF>5 {printf "%s  rss=%.0fMB anon=%sMB file=%sMB arena=%s\n", $1, $4/1024, $9, $10, $19}'

# 日增速（判读线性 vs 饱和的关键）
ssh k3s003 "sudo cat /var/log/newpm-mysql-mem.csv" | \
  awk -F, 'NR>1 {d=substr($1,1,10); if(!(d in mn)||$4<mn[d])mn[d]=$4; if($4>mx[d])mx[d]=$4}
           END{for(d in mx) printf "%s  日增 %.1f MB\n", d, (mx[d]-mn[d])/1024}' | sort

# 预警
ssh k3s003 "sudo cat /var/log/newpm-mysql-alert.log"
```

## 判读

| 观测特征 | 结论 | 动作 |
|---|---|---|
| 第 3 天与第 7 天的日增速无明显下降 | **线性** ⇒ 2Gi 只是延期 | 启用月度计划内重启；并继续定位链 A |
| 日增速逐日衰减，`vmrss` 形成平台（连续 3 天增幅 <5 MB/天） | **饱和** ⇒ 2Gi 够用 | 采样降频到 30 分钟长期保留即可 |
| `arena_cnt` 不再增长但 `vmrss` 仍涨 | 增量落在既有 arena 内部 | 指向碎片而非新分配 |
| 日增速上升 | **加速**，存在新变量 | 复查同期部署与业务变化 |

**只需 3~7 天就能判读**，不必等一个完整的 OOM 周期。探针改造（Issue #34）之后 cgroup 扰动从每 5 秒一次降到每 60 秒一次，曲线噪声降低 91.7%，判读比改造前容易得多。

## 预警阈值

| 等级 | 触发条件 | 距 OOM 估计余量 | 动作 |
|---|---|---|---|
| 🟡 | `vmrss > 1600 MB` | 8.5~15 天 | 排期一次低峰期计划内重启 |
| 🟠 | `file < 50 MB` 或 `pgscan_direct/pgsteal_direct` 由 0 转正 | 数天，此时数据库已在 I/O 降级 | 48 小时内执行计划内重启 |
| 🔴 | `oom_kill > 0` 或 `vmrss > 1850 MB` | 4~7 天，随时可能被引爆 | 立即在当班时段重启 |

依据：Issue #34 的内核 dump 显示，5 次 OOM 前 cgroup 都已长期贴顶抖动（`failcnt` 148 万~7801 万、页缓存从 152 MB 被榨到 **188 KB**）。**OOM 不是突然发生的，有长达数天、信号极强的预警期，只是此前无人监视。**

## 基线（2026-08-08 20:59，探针改造后新 pod 运行 6.3 小时）

```
vmrss 477 MB / anon 443 MB / anon_thp 428 MB (96.6%) / file 13 MB
arena 25 个 / cg_peak 466 MB / cg_max 2048 MB
pgscan=0 pgsteal=0 ws_refault=0 oom_kill=0     ← 无内存压力
```

对照：改造前的旧 pod 在运行约 26 小时时 `vmrss` 已达 742 MB。重启使其回落到 477 MB —— 这也侧面印证了「增长是单调累积的，重启即清零」。

## 已知坑

- **`strtonum()` 是 gawk 扩展**，Debian 默认的 mawk 不支持，会**静默返回 0**（本脚本因此一度把 `arena_cnt` 全部记成 0）。现改用正则判断 64 MiB 对齐（末 6 位 `000000` 且第 7 位为 `0/4/8/c`）。
- **pod 重建后 `pod_uid` 会变**，此后所有 cgroup 计数器从零开始。绘图分析时**必须按 `pod_uid` 分段**，否则会把重启当成"内存突然下降"。
- pod 若被调度到 k3s003 以外的节点，本脚本会找不到进程并静默退出（只在 alert log 记一行），需重新部署到新节点。
