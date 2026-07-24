# newpm 备份运维脚本

生产服务器 **k3s001**（阿里云 ECS，cn-beijing）上 newpm 数据库与附件的备份脚本。
本目录是这些脚本的**版本管理副本**——线上真实运行位置是 `/usr/local/bin/`，两者应保持一致。

> 设计方案、决策依据与踩坑全记录见 `docs/plans/2026-07-24-backup-to-oss-archive.md`。

## 架构：OSS 为主，本地为辅

服务器磁盘紧张（148G，78% 已用），因此**阿里云 OSS 归档存储是主存储**，本地只保留极少量应急热层。

```
数据库 mysqldump ─┬─► 本地 /backup/newpm-mysql/   (热层，留 2 份 ≈ 10M)
                  └─► OSS newpm-mysql/            (主存储，30 天)

附件 tar(PVC)  ─┬─► 本地 /backup/newpm-upload/   (热层，留 1 份 ≈ 2.4G)
                └─► OSS newpm-upload/            (主存储，单目录，60 天)
```

| 对象 | 频率 | 本地保留 | OSS 保留 | RPO |
|---|---|---|---|---|
| 数据库 `ry-vue` | 每 6 小时 | **2 份**（~10M） | **30 天** | 6 小时 |
| 附件 upload-pvc | 每日 01:20 | **1 份**（~2.4G） | **60 天** | 1 天 |

> 本地保留量刻意压到极小（省服务器磁盘）：数据库留 2 份消除“新旧交替空窗”，附件留 1 份（单份 2.4G 是省磁盘大头）。**历史版本全靠 OSS**。
> 附件 OSS 用**单目录**（无 daily/monthly 分层）：全量备份 + 2T 额度充裕，分层是过度设计；60 天卡归档最短计费下限。数据库 30 天按需求定（单份 5M，罚金可忽略）。

## 脚本清单

| 脚本 | 作用 |
|---|---|
| `backup-newpm-db.sh` | 数据库备份：mysqldump → 本地 → OSS → 清理。四道闸门。 |
| `backup-newpm-upload.sh` | 附件备份：tar PVC → 本地 → OSS（单目录 `newpm-upload/`）→ 清理。 |
| `sync-backup-to-oss.sh` | 底层上传器：`<文件> <前缀>`，上传后校验大小 + 存储类型。被上面两个调用。 |
| `check-backup-health.sh` | 每日巡检：7 项检查（磁盘/新鲜度/OSS今日对象/本地-OSS一致性/日志错误）。退出码=问题数。 |
| `oss-initial-sync.sh` | 一次性存量补传（历史遗留，首次迁移用，日常不需要）。 |

## 四道安全闸门（本地保留量骤降后的关键保护）

本地只留 2 天，"上传成功"从此是**关键路径**。脚本因此内置：

1. **磁盘空间不足 → 拒绝开始**（避免产出截断文件）
2. **文件大小异常 → 删除中止**（`kubectl exec` 失败时 gzip 仍产出 0 字节文件，退出码不可靠）
3. **完整性校验**：数据库查 `-- Dump completed on` 金标准；附件用 `tar -tzf` 验 gzip 流 + tar 结构
4. **上传失败 → 拒绝清理本地**（OSS 没确认前，本地副本一份都不删）

## crontab（root）

```cron
10 */6 * * * /usr/local/bin/backup-newpm-db.sh     >> /backup/newpm-mysql/cron.log 2>&1
20 1   * * * /usr/local/bin/backup-newpm-upload.sh  >> /backup/newpm-upload/cron.log 2>&1
0  8   * * * /usr/local/bin/check-backup-health.sh  >> /backup/backup-health-cron.log 2>&1
```

## OSS 接入（一次性，已完成）

| 项 | 值 |
|---|---|
| Bucket | `yada-newpm-backup`（华北2·北京 / 归档 / **LRS 本地冗余** / 私有） |
| 工具 | `ossutil` v1.7.18 |
| 凭证 | `/root/.ossutilconfig`（权限 600，**内网** endpoint `oss-cn-beijing-internal`） |
| RAM 用户 | `li.kong`，策略「OSS存储策略」——**只写不删、无 GetObjectMeta**（防勒索 + 内容不可读出） |

生命周期规则（Bucket 侧配置，非脚本，控制台已配 3 条）：
- `expire-db`：前缀 `newpm-mysql/`，30 天后删除
- `expire-upload`：前缀 `newpm-upload/`，60 天后删除（一并覆盖历史遗留的 daily/、monthly/ 子目录）
- `clean-parts`：整个 Bucket，未完成分片 7 天清理

## 部署 / 更新

改脚本后同步回服务器（务必保持仓库与线上一致）：

```bash
scp ops/backup/<脚本> k3s001:/tmp/
ssh k3s001 "sudo install -m 755 /tmp/<脚本> /usr/local/bin/ && sudo bash -n /usr/local/bin/<脚本>"
```

首次改动前，服务器上原始脚本已备份为 `/usr/local/bin/*.sh.orig`，可回滚。

## 常用运维命令

```bash
# 手动触发
ssh k3s001 "sudo /usr/local/bin/backup-newpm-db.sh"
ssh k3s001 "sudo /usr/local/bin/backup-newpm-upload.sh"

# 巡检（退出码=问题数，日志在 /backup/backup-health.log）
ssh k3s001 "sudo /usr/local/bin/check-backup-health.sh; echo 问题数=\$?"

# 查看 OSS 备份（用 ls 不用 stat——策略无 GetObjectMeta，stat 会 403）
ssh k3s001 "sudo /usr/local/bin/ossutil ls oss://yada-newpm-backup/newpm-mysql/"

# 从 OSS 恢复：归档对象必须先解冻（restore 需主账号，li.kong 无此权限）
ssh k3s001 "sudo /usr/local/bin/ossutil restore oss://yada-newpm-backup/newpm-mysql/<文件>"
# 解冻完成后下载 + 校验
ssh k3s001 "sudo /usr/local/bin/ossutil cp oss://yada-newpm-backup/newpm-mysql/<文件> /tmp/ && gzip -t /tmp/<文件>"
```

## 注意事项

- **DB 密码**：`backup-newpm-db.sh` 默认取 `password`（本环境既定值，已遍布仓库示例）；可用环境变量 `NEWPM_DB_PWD` 覆盖。
- **附件脚本硬编码 PVC 宿主机路径**（含 UUID）。upload-pvc 重建后 UUID 变化，脚本会在闸门0 报错中止（不会静默备份空目录）——需同步更新 `UPLOAD_DIR`。
- **归档三约束**：最短计费 60 天；读取前必须解冻；最小计量 64KB。
- **资源包 2TB 到期 2027-06-17**，到期前需决定续费或转按量。
