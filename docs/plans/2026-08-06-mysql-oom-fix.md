# newpm 生产 MySQL 反复 OOMKilled — 诊断与解决方案

- **日期**：2026-08-06
- **环境**：k3s 集群 / namespace `newpm` / StatefulSet `mysql` / Pod `mysql-0`（调度在节点 **k3s003**）
- **镜像**：`mysql:8.0`（实际 8.0.44）
- **状态**：根因已定位；生产已被手工临时缓解（见 §4 配置漂移），仓库侧修复**未完成**

---

## 1. 业务描述

`mysql-0` 是 newpm 项目管理系统的**唯一数据库实例**，承载全部业务数据：项目、合同、客户、款项、工作日报、收入确认等 58 张表。无主从、无读写分离，`ruoyi-app` 所有请求最终都落到它身上。

期望行为：数据库应长期稳定运行，仅在发版或运维操作时重启。

实际影响：MySQL 被内核强制杀死（SIGKILL）时，所有在途事务中断、连接池全部失效，`ruoyi-app` 在数据库恢复前（约 30~60 秒）持续返回 500。**属于全站不可用故障**，且发生在无人值守的任意时刻。

---

## 2. 现象

Pod 在 78 天内重启 **9 次**，容器状态记录为 `OOMKilled` / `Exit Code: 137`。

```
Last State:     Terminated
  Reason:       OOMKilled
  Exit Code:    137            # 137 = 128 + 9(SIGKILL)
  Started:      Mon, 27 Jul 2026 19:47:51 +0800
  Finished:     Wed, 05 Aug 2026 18:10:13 +0800
Restart Count:  9
Limits:
  memory:  1Gi
```

节点 k3s003 内核日志（近 30 天）记录到 3 次 mysqld 被 OOM killer 处决：

| 时间 | 被杀进程 RSS | 距上次 |
|---|---|---|
| Jul 14 20:23:37 | 1042596 kB (1018 MiB) | — |
| Jul 27 19:47:50 | 1040776 kB (1016 MiB) | 13 天 |
| Aug 05 18:10:13 | 1040396 kB (1016 MiB) | 9 天 |

**三次 RSS 差异不到 2MB。** 这是「内存缓慢单调爬升 → 撞上固定天花板」的签名；如果是某个大查询瞬间打爆，RSS 会呈现明显方差。

**排除项**：不是节点内存不足。k3s003 总内存 14Gi，事发时段可用 11Gi。内核日志明确标注 `constraint=CONSTRAINT_MEMCG`，即**受限于 cgroup 配额**而非物理内存。

---

## 3. 根因

### 3.1 直接原因

`k8s/mysql.yml` 设定 `limits.memory: 1Gi`，落到 cgroup 即 `memory.max = 1073741824`。mysqld 常驻内存逼近该值时被内核 SIGKILL。

### 3.2 为什么 1Gi 不够 —— 内存账本

事发前一轮运行的实测数据（`sys.x$memory_global_by_current_bytes`）：

| 区域 | 当前占用 | 历史峰值 |
|---|---|---|
| `memory/performance_schema` | **229.2 MB** | 229.2 MB |
| `memory/innodb` | 204.4 MB | 206.6 MB |
| `memory/sql` | 28.1 MB | **320.4 MB** |
| `memory/mysys` | 9.1 MB | 25.5 MB |
| `memory/temptable` | 2.0 MB | 8.0 MB |
| **合计** | **~473 MB** | — |

cgroup 侧交叉验证（该轮运行 16 小时后）：

```
memory.max      1073741824   (1024 MiB)
memory.current   710463488   ( 677 MiB)   ← 66%
memory.peak      714985472   ( 682 MiB)
anon             549236736   ( 524 MiB)
```

**稳态就消耗掉 66%，可用余量仅约 340 MB。**

`memory/sql` 峰值 320 MB 拆开看：

| 事件 | 当前 | 峰值 |
|---|---|---|
| `memory/sql/String::value` | 0.1 MB | **186.5 MB** |
| `memory/sql/THD::main_mem_root` | 0.2 MB | **104.6 MB** |

即单轮运行中确实出现过 SQL 层瞬时占用 ~300MB 的时刻。`473 + 320 = 793 MB`，已非常贴近 1024 MB 天花板。

### 3.3 为什么会浪费这么多 —— 配置从未定制

**`k8s/mysql.yml` 没有挂载任何 `my.cnf`**，容器仅通过 `args` 传入三个字符集参数。除此之外**全部使用 MySQL 8.0 默认值**，而这些默认值是按「独占一台大内存服务器」设计的：

| 变量 | 现值 | 与实际需求的落差 |
|---|---|---|
| `performance_schema` | **ON**，实测占 **229 MB** | 全库仅 39 MB，日常不做性能剖析 |
| `temptable_max_ram` | **1073741824 (1 GiB)** | **等于整个容器 limit**，等同不设防 |
| `table_open_cache` | 4000 | 全库仅 **58 张表** |
| `max_connections` | 151 | 历史峰值 `Max_used_connections = **13**` |
| `innodb_buffer_pool_size` | 134217728 (128 MB) | 全库 39 MB，128 MB 已足以全量装载 |

**数据库实际体量：`ry-vue` 全库 39.0 MB / 58 张表。**

其中 `performance_schema` 是最大一笔：它在**启动时按 `max_connections` × `table_open_cache` 预分配**内存表，与实际数据量无关。151 连接 × 4000 表缓存的配置规模，直接换来 229 MB 的固定开销。

### 3.4 未能验证的部分（如实标注）

从推算的 ~793 MB 到 OOM 实际 RSS 1016 MB，仍有约 **220 MB 缺口未能归属**。

合理怀疑是 glibc malloc 释放后不归还 OS 造成的堆碎片累积（可解释「只涨不落、9~13 天撞线」的爬升模式），**但本次未取得直接证据，不作为结论采信**。

障碍：`events_statements_summary_by_digest` 与内存统计在每次容器重启后清零，OOM 现场随进程一起丢失。若需坐实，需要在稳态运行期间定期采样 RSS 与内存事件（见 §8 后续观测）。

**这一缺口不影响本方案成立**：即便不计这 220 MB，473 MB 稳态 + 320 MB 峰值在 1 GiB 下也已无安全余量。

---

## 4. ⚠️ 已发现的配置漂移（必须一并处理）

诊断过程中（2026-08-06 10:29:52）Pod 发生了一次**非 OOM 的删除重建**：

```
Killing → SuccessfulDelete → SuccessfulCreate    # 事件序列
restartCount: 0                                   # 计数归零 = Pod 重建而非容器重启
lastState: {}                                     # 无上次终止记录
```
同时段 k3s003 内核**无任何 OOM 记录**。

核对 StatefulSet 后确认原因：**有人直接修改了生产 live 对象的内存 limit**。

| 来源 | `limits.memory` |
|---|---|
| 生产 live spec | **2Gi** |
| `last-applied-configuration` annotation | **1Gi** |
| 仓库 `k8s/mysql.yml:45` | **1Gi** |

`last-applied-configuration` 仍为 1Gi 是铁证：**该修改走的是 `kubectl edit` / `kubectl patch`，未经 `kubectl apply`，也未回写仓库。**（`metadata.generation = 4`）

### 风险：这是一颗定时炸弹

**任何人下次执行 `kubectl apply -f k8s/`，都会把 limit 静默打回 1Gi**，Pod 重建，OOM 问题原样复发——而且届时没人会想到是 apply 导致的。

因此 §5 方案的第一步不是优化，而是**把生产现状固化回仓库**。

---

## 5. 解决方案

三步，按顺序执行。

### 步骤 1（必做，最高优先级）：仓库同步 2Gi，消除漂移

修改 `k8s/mysql.yml`：

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "2Gi"     # 1Gi → 2Gi：1Gi 下稳态即占 66%，无安全余量（见 §3.2）
    cpu: "500m"
```

节点余量充足：k3s003 总 14Gi、可用 11Gi，三节点内存使用率均在 49~55%。

> 此步骤**不会触发 Pod 重建**——生产 live 已经是 2Gi，apply 后无差异。

### 步骤 2（推荐）：挂载 my.cnf ConfigMap，收敛不受控的上限

**配置加载路径已实测确认**：容器内 `/etc/my.cnf` 末尾为 `!includedir /etc/mysql/conf.d/`，且该目录当前为空。挂载到 `/etc/mysql/conf.d/custom.cnf` 即会被加载。

新增 ConfigMap（追加到 `k8s/mysql.yml`）：

```yaml
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: mysql-custom-config
  namespace: newpm
data:
  custom.cnf: |
    [mysqld]
    # ---- 依据：全库 39MB / 58 张表 / 历史连接峰值 13（2026-08-06 实测）----

    # 临时表内存池上限。默认 1GiB == 容器 limit，等同不设防：
    # 单个坏查询即可打爆整个实例。收敛为 64M 后超出部分自动落盘
    # （temptable_use_mmap=ON），结果是「该查询变慢」而非「整库被杀」。
    temptable_max_ram = 64M

    # 表缓存。默认 4000 是为数百张表的库准备的；全库仅 58 张。
    table_open_cache = 400
    table_definition_cache = 400

    # 最大连接数。默认 151；历史峰值 Max_used_connections = 13，50 留近 4 倍余量。
    # 每连接自带 sort/join/read buffer，连接数直接抬高内存基线。
    max_connections = 50

    # InnoDB 数据与索引缓存。全库 39MB，128M 已可全量装载，无需上调。
    # 此处显式写明是为固化决策，避免后人误以为「没配过」而随手调大。
    innodb_buffer_pool_size = 128M
```

对应挂载（加入既有 `volumeMounts` / `volumes`，与现有 `initdb-wrapper` 写法一致）：

```yaml
          volumeMounts:
            - name: custom-config
              mountPath: /etc/mysql/conf.d/custom.cnf
              subPath: custom.cnf
      volumes:
        - name: custom-config
          configMap:
            name: mysql-custom-config
```

### 步骤 3：`performance_schema` 如何取舍 —— 两个选项

**这一项建议与初诊时不同，因为前提变了。** 初诊是在 limit 仍为 1Gi 时给出的「关掉省 229MB」；现在 limit 已是 2Gi，省内存不再迫切，而诊断能力的价值上升。

| | 方案 A：保留 P_S（**推荐**） | 方案 B：关闭 P_S |
|---|---|---|
| 配置 | 不加 `performance_schema` 项 | `performance_schema = OFF` |
| 稳态内存（估） | ~470 MB | ~240 MB |
| 占 2Gi 比例 | **23%** | 12% |
| 诊断能力 | 保留 | **丧失** |

**推荐方案 A**，理由：

1. 2Gi 下 470 MB 稳态仅占 23%，余量 1.5 GB 以上，229 MB 不值得省
2. **本次根因定位完全依赖 performance_schema**——`sys.x$memory_global_by_current_bytes` 是唯一能回答「内存被谁吃掉」的工具。关掉它，下次再出问题就只剩猜测
3. §3.4 那 220 MB 缺口尚未查清，保留观测手段是必要的

若未来内存重新吃紧再考虑方案 B，届时应同步开启慢查询日志作为替代（写文件，几乎不占内存）：

```ini
slow_query_log = ON
long_query_time = 2
slow_query_log_file = /var/lib/mysql/slow.log
```

---

## 6. 预期效果

| 指标 | 修复前 | 修复后（预期） |
|---|---|---|
| 容器 limit | 1024 MB | 2048 MB |
| 稳态占用 | ~473 MB（实测） | ~470 MB（方案 A，基本不变） |
| 占 limit 比例 | **66%** | **23%** |
| 临时表可吃内存上限 | **1024 MB（= limit）** | 64 MB（封顶） |
| OOM 频率 | 9~13 天一次 | 预期消除 |

> 标注：稳态数值为实测，比例为据此推算。真实效果需按 §8 验证。

**两类改动解决的是两个不同问题，缺一不可：**
- 提高 limit → 解决**余量太薄**（稳态就占 66%）
- 收敛 temptable → 解决**故障隔离**（把「一个坏查询打死全库」降级为「该查询自己变慢」）

---

## 7. 实施步骤

```bash
# 1. 修改 k8s/mysql.yml（步骤 1 + 2）后应用
kubectl apply -f k8s/mysql.yml

# 2. ConfigMap 变更不会自动触发 Pod 重建，需手动滚动
#    ⚠️ 单实例数据库，此操作会造成 30~60 秒不可用，请在低峰期执行
kubectl rollout restart statefulset/mysql -n newpm
kubectl rollout status statefulset/mysql -n newpm

# 3. 确认新参数已生效
kubectl exec -n newpm mysql-0 -- mysql -u root -ppassword -e \
  "SHOW VARIABLES WHERE Variable_name IN \
   ('temptable_max_ram','table_open_cache','max_connections','innodb_buffer_pool_size');"
```

**执行前提醒**：
- 建议先手动触发一次数据库备份：`ssh k3s001 "sudo /usr/local/bin/backup-newpm-db.sh"`
- 本仓库 CI 的 `paths-ignore` 中 `'*.md'` **仅匹配根目录**，改动 `docs/**` 下的 md **照常触发一次约 6 分钟的生产部署**（见 CLAUDE.md）。本文档提交时需知悉。

---

## 8. 验证

### 立即验证（重启后即可）

```bash
# cgroup 限额已是 2Gi
kubectl exec -n newpm mysql-0 -- cat /sys/fs/cgroup/memory.max
# 期望：2147483648

# 参数已生效
kubectl exec -n newpm mysql-0 -- mysql -u root -ppassword -N -e \
  "SELECT @@temptable_max_ram, @@table_open_cache, @@max_connections;"
# 期望：67108864 / 400 / 50
```

### 持续观测（关键，14 天）

单次检查无法证明问题解决——**故障周期本身就是 9~13 天**。需连续观测：

```bash
# 每日采样：当前占用 / 历史峰值 / 上限
kubectl exec -n newpm mysql-0 -- sh -c \
  'cat /sys/fs/cgroup/memory.current /sys/fs/cgroup/memory.peak /sys/fs/cgroup/memory.max'

# 重启计数应保持不变
kubectl get pod mysql-0 -n newpm -o jsonpath='{.status.containerStatuses[0].restartCount}'

# 内核 OOM 记录应为空
ssh k3s003 "sudo journalctl -k --since '1 day ago' | grep -i 'Memory cgroup out of memory'"
```

**判定标准**：连续 14 天（覆盖 1 个以上历史故障周期）满足——
1. `restartCount` 无增长
2. 内核无新增 OOM 记录
3. `memory.peak` 稳定低于 1.2 GB（若持续爬升逼近 2 GB，则 §3.4 的 glibc 碎片怀疑成立，需转向 `jemalloc` 或定期重启方案）

**第 3 条同时是 §3.4 那个未解缺口的验证实验**：若 peak 稳定不涨，说明爬升源于 SQL 层峰值叠加，已被 temptable 封顶解决；若仍单调爬升，则是分配器层面的问题，本方案只是把撞线时间从 9 天推迟到约 20 天，需要进一步处理。

---

## 9. 回滚

改动均为声明式配置，回滚成本低：

```bash
git revert <commit>
kubectl apply -f k8s/mysql.yml
kubectl rollout restart statefulset/mysql -n newpm
```

风险提示：**回滚 = 退回 1Gi + 无 my.cnf**，即恢复到会 OOM 的状态。仅在新配置引发未预期问题时使用。

---

## 10. 遗留问题

1. **谁改的 limit？** 2026-08-06 10:29 有人绕过 `kubectl apply` 直接修改生产 StatefulSet。改动本身方向正确，但**未回写仓库**造成漂移。需明确操作人并约定：生产变更一律走仓库 + `apply`。
2. **§3.4 的 220 MB 缺口**未归属，由 §8 第 3 条判定标准负责证伪或证实。
3. **单点数据库无高可用**：`replicas: 1`，任何重启都是全站不可用。本方案降低重启频率，但未解决单点问题。如业务对可用性要求提高，需另立主从或托管数据库方案。

---

## 附录：证据采集命令

所有结论均可按以下命令复现（采集于 2026-08-06）。

```bash
# Pod 状态与 OOM 记录
ssh k3s001 "kubectl describe pod mysql-0 -n newpm"

# 内核 OOM 日志（注意：mysql-0 调度在 k3s003，不在 k3s001）
ssh k3s003 "sudo journalctl -k --since '30 days ago' | grep -i 'Memory cgroup out of memory'"

# cgroup 内存实况
ssh k3s001 "kubectl exec -n newpm mysql-0 -- sh -c 'cat /sys/fs/cgroup/memory.current /sys/fs/cgroup/memory.peak /sys/fs/cgroup/memory.max'"

# 内存分区占用
ssh k3s001 "kubectl exec -n newpm mysql-0 -- mysql -u root -ppassword -t -e \"
  SELECT SUBSTRING_INDEX(event_name,'/',2) AS area,
         ROUND(SUM(current_alloc)/1024/1024,1) AS cur_MB,
         ROUND(SUM(high_alloc)/1024/1024,1) AS peak_MB
  FROM sys.x\\\$memory_global_by_current_bytes GROUP BY area ORDER BY SUM(current_alloc) DESC;\""

# 库体量与连接峰值
ssh k3s001 "kubectl exec -n newpm mysql-0 -- mysql -u root -ppassword -e \"
  SELECT ROUND(SUM(data_length+index_length)/1024/1024,1) db_MB, COUNT(*) tables
  FROM information_schema.tables WHERE table_schema='ry-vue';
  SHOW GLOBAL STATUS LIKE 'Max_used_connections';\""

# 配置漂移核对（live spec vs last-applied）
ssh k3s001 "kubectl get sts mysql -n newpm -o jsonpath='{.spec.template.spec.containers[0].resources}'"
ssh k3s001 "kubectl get sts mysql -n newpm -o jsonpath='{.metadata.annotations}'"

# 配置加载路径确认
ssh k3s001 "kubectl exec -n newpm mysql-0 -- sh -c 'tail -3 /etc/my.cnf; ls -la /etc/mysql/conf.d/'"
```
