# newpm 备份异地化方案：本地 → 阿里云 OSS 归档存储

> 日期：2026-07-24 ｜ 目标服务器：k3s001 ｜ Bucket：`yada-newpm-backup`（cn-beijing）
> **状态：全部完成**（2026-07-24）——存量已上 OSS、脚本已提频改造、crontab 已更新、OSS 3 条生命周期规则已配置、本地 Mac 第三副本已建立、健康巡检已上线。
>
> ⚠️ **最终方案在实施中经过多轮调整，与本文档早期章节的推演值不同，一切以第 10、11 节的「最终落地参数」为准。**

## 1. 背景

当前 newpm 的数据库与附件备份**全部只存在 k3s001 本机 `/backup/` 目录**，机器损毁 / 磁盘故障 / 误删 / 被入侵即全部丢失，没有任何异地副本。

同时本机磁盘已达 **77%**（148G 用 109G，剩 33G），附件备份以每月约 +20% 的速度增长，本地保留空间也在被持续挤压。

已在阿里云购买**归档存储类型 OSS**，本方案将备份异地化到该 OSS，同时借机把本地保留期收敛，释放宿主机磁盘。

## 2. 现状实测数据（2026-07-24）

| 对象 | 频率 | 本地保留 | 份数 | 单份 | 本地占用 |
|---|---|---|---|---|---|
| 数据库 `ry-vue` | 每日 00:10 | 30 天 | 31 | ~4.9M | 132M |
| 附件 upload-pvc | 每周日 01:20 | 30 天 | 6 | ~2.4G（月增 20%） | 13G |

- 备份脚本：`/usr/local/bin/backup-newpm-db.sh`、`/usr/local/bin/backup-newpm-upload.sh`，均在 root crontab。
- 宿主机：阿里云 ECS `ecs.hfg7.xlarge`，可用区 **cn-beijing-k**，Debian 12。

### 已验证的技术前提

| 检查项 | 结果 | 含义 |
|---|---|---|
| OSS 北京**内网** endpoint | ✅ 通，34ms | **走内网，上传零流量费**，不占公网带宽 |
| OSS 公网 endpoint | ✅ 通，81ms | 备选，但收流量费 |
| 直连公网（`ifconfig.me`） | ❌ 失败 | 出网受限；**内网 OSS 不受影响**，这是选内网口的硬理由 |
| ECS 绑定 RAM 角色 | ❌ 未绑定（404） | 必须用 AccessKey，需配套最小权限策略 |
| ossutil / rclone | ❌ 均未安装 | 需先安装 ossutil |

## 3. 归档存储的三个硬约束（方案的设计约束来源）

> 计费细则以阿里云官网/控制台为准，此处为设计时必须绕开的点。

1. **最短存储时长 60 天**。不足 60 天就删除的对象，仍按 60 天计费。
   → **OSS 侧保留期必须 ≥60 天**，本方案取 DB 365 天 / 附件日备 60 天 / 附件月存 365 天。**绝不能照抄本地的 30 天。**
2. **读取前需解冻（restore）**。归档对象不能直接下载，须先发起解冻请求并等待，之后在解冻有效期内才可读。
   → 紧急恢复会有延迟。**对策：本地保留一个"热层"**，日常恢复走本地，OSS 只做灾备。若希望免解冻，可在 Bucket 开启**归档直读 / 实时访问归档**（按读取量计费）。
3. **最小计量单位 64KB**。本方案文件为 MB~GB 级，无影响。

## 4. 方案设计

### 4.0 已购资源包（决定保留期的关键前提）

**已购买：归档存储资源包 2TB，到期 2027-06-17 15:00。**

资源包是**预付费抵扣制：用不完不退款、不结转**。这与按量付费的成本直觉正好相反——按量付费下保留期越短越省钱，资源包下保留期定得太短等于白扔钱。

因此保留期不按"省容量"来定，而是**按业务真正需要的追溯期来定**，把已付额度换成实际的容灾价值。

⚠️ 但不为填满额度而堆冗余：备份份数越多，恢复时"该用哪一份"的判断成本越高。额度要换成**更长的追溯期 + 更短的 RPO**，而不是换数字好看。

**关键推论：资源包有效期只剩 328 天（2026-07-24 → 2027-06-17）**

- 从零开始积累，到资源包到期时数据最老也才 328 天，**「365 天删除」的生命周期规则在整个资源包有效期内一次都不会触发**。它只是一道防止无限增长的兜底闸门，实际稳态容量 = 328 天的纯积累。
- 归档类型**最短计费 60 天**，所以任何前缀的保留期都不得低于 60 天。
- **2027-06-17 到期后未续费则转按量计费**（约 0.033 元/GB/月）。届时若积累 ~500G，即约 16 元/月。**需在到期前一个月设提醒**，决定续费或裁剪。

### 4.1 冷热分层（**已选定方案 B：全面提频**）

| 对象 | 新频率 | 本地热层保留 | OSS 冷层前缀 | OSS 保留 |
|---|---|---|---|---|
| 数据库 | **每 6 小时**（00/06/12/18 时） | 7 天（28 份，~140M） | `newpm-mysql/` | 365 天 |
| 附件（日常） | **每日** 01:20 | 2 份（~4.8G） | `newpm-upload/daily/` | **60 天** |
| 附件（月度存档） | 每月 1 号那份 | 同上 | `newpm-upload/monthly/` | 180 天 |

- 热层：秒级恢复，应对日常误删/回滚。
- 冷层：灾备 + 一年追溯，需解冻。
- 附件日备保留期取 **60 天**——恰好等于归档最短计费期，一天不多一天不少，既不浪费也不产生提前删除费。

**收益**：本地 `/backup` 从 14G 降到约 **5G**（省 9G）；数据库 RPO 24h→6h，附件 RPO 7天→1天。

### 4.1.0 提频带来的两处必改（否则实施必翻车）

1. **数据库文件名必须带小时**。现有命名 `newpm-YYYYMMDD.sql.gz` 只到天粒度，一天跑 4 次会**互相覆盖**，等于只备了最后一次。
   → 改为 `newpm-YYYYMMDD-HHMM.sql.gz`。
   → 同时清理逻辑的 glob `newpm-*.sql.gz` 仍能匹配，无需改。

2. **附件月度存档需在上传时分流前缀**。按执行日判断，1 号进 `monthly/`，其余进 `daily/`，让生命周期规则能用前缀区分保留期。

### 4.1.1 附件为何仍用全量 tar（而非目录同步）

附件是只增不减的文件目录，每周全量 tar 内容重复度极高：52 份约 250G，而实际内容只有 2.7G。理论上 `ossutil sync` 增量同步能把容量压到 2.7G。

**但仍选全量 tar**，理由：

- **2T 额度下容量不是瓶颈**（250G 仅占 12%），省容量没有收益；
- 归档类型**每个对象都要单独解冻**。同步方式在 OSS 上会散落数万个小文件，灾难恢复时逐个解冻几乎不可操作；tar 只需解冻 1 个对象；
- 归档**最小计量 64KB**，大量小附件会产生显著计量损耗，实际省不到理论值；
- 同步语义下"本地删了远端也删"，与本方案"备份不可被删"的安全前提冲突。

**结论：灾备场景优先恢复路径的简单可靠，用额度换确定性。**

**⚠️ 执行顺序不可颠倒**：必须先跑通 OSS 上传并验证成功，**再**缩短本地保留期。否则中间存在"本地已删、异地没传上去"的裸奔窗口。

### 4.2 删除权责分离（安全设计）

- **上传脚本只有写权限，没有删除权限**。
- OSS 侧的过期清理**全部交给 Bucket 生命周期规则**，不由脚本执行。

理由：备份是勒索软件的首要目标。服务器上的 AccessKey 一旦泄露，若带 delete 权限，攻击者可以先删异地备份再加密本机——异地备份就失去了意义。

### 4.3 容量测算（对照 2TB 额度）

单份大小按增长趋势取区间均值：DB 4.9M→6M（均值 5.5M）；附件 2.4G→7.2G（月增约 0.4G，均值 4.5G）。

**方案 A — 维持现有频率**（DB 每日 / 附件每周）

| 对象 | 328 天份数 | 容量 |
|---|---|---|
| 数据库 | 328 | ~1.8G |
| 附件 | 47 | ~211G |
| **合计** | | **~213G ＝ 额度的 10.6%** |

**方案 B — 提升频率**（DB 每 6 小时 / 附件每日）✅ **已选定**

| 对象 | 保留策略 | 份数 | 容量 |
|---|---|---|---|
| 数据库 | 每 6 小时，全程保留 | 1312 | ~7.2G |
| 附件（日） | 每日，保留 60 天 | 60 | ~270G |
| 附件（月度存档） | 每月 1 号那份，全程保留 | 11 | ~50G |
| **合计** | | | **~327G ＝ 额度的 16%** |

方案 B 多花 114G（额度的 5.4%），换来的是：

| 指标 | 方案 A | 方案 B |
|---|---|---|
| 数据库 RPO（最坏丢失） | **24 小时** | **6 小时** |
| 附件 RPO（最坏丢失） | **7 天** | **1 天** |

数据库每 6 小时一次几乎免费（+5.4G，库仅 4.9M，`--single-transaction` 不锁表，秒级完成）。附件从周备改日备是本方案性价比最高的一笔——**最坏情况下丢失的用户上传文件从"一周的量"降到"一天的量"**。

> 两种方案都远未用满 2TB。**剩余额度不必强行填满**——继续加份数只会增加恢复时的选择成本，不产生业务价值。

**其他费用**：内网上传流量免费；请求数每天个位数，可忽略。**取回（解冻+下载流量）按量另计，不被存储资源包抵扣**，仅灾难恢复时产生。

## 5. 实施步骤

> **分两阶段执行。** 阶段一先把存量备份推上 OSS 建立异地基线、验证通路，**期间不改动任何现有备份脚本**（风险最低）；确认稳定后再进入阶段二做自动化与提频。

| 阶段 | 内容 | 步骤 | 状态 |
|---|---|---|---|
| **阶段一** | 存量备份上传 OSS | 1 → 2 → 3 → 4 → 5 | **本次执行** |
| **阶段二** | 自动化 + 提频 + 收敛本地 | 6 → 7 → 8 | 后续 |

**⚠️ 生命周期规则按对象在 OSS 上的最后修改时间计算，不是原文件 mtime。** 存量补传的历史备份，OSS 一律视为「今天创建」，保留期从上传日起算。因此：

- 存量 31 份数据库 → `newpm-mysql/`，365 天保留。
- **存量 6 份附件 → 放 `newpm-upload/monthly/`（180 天保留）**，不放 60 天的 `daily/`。它们是目前唯一的历史副本，总共才 13G，不值得 60 天后被清掉。

---

## 阶段一：存量上传

### 步骤 1：阿里云控制台准备（需人工操作）

1. **创建 Bucket** —— 实际采用 **`yada-newpm-backup`**

| 配置项 | 值 | 说明 |
|---|---|---|
| 名称 | `yada-newpm-backup` | **全局唯一**（跨全体阿里云用户）。删除后名称不会立即释放，重建同名会失败——直接换名更快 |
| 地域 | **华北2（北京）** | 必须与 ECS 同地域，否则内网口失效 |
| 存储类型 | **归档存储** | 创建后不可改 |
| **存储冗余类型** | **本地冗余存储（LRS）** | ⚠️ **最大的坑，见下** |
| 读写权限 | 私有 | |
| 阻止公共访问 | 已开通 | |
| 版本控制 | 关闭 | 文件名自带日期不会互相覆盖，开了每个版本单独计费 |

> ⚠️ **存储冗余类型必须选「本地冗余（LRS）」，不能用控制台默认推荐的「同城冗余（ZRS）」。**
> 阿里云原文：*"同城冗余存储将采用较高的计费标准，产生的存储费用**仅可被同城冗余存储包抵扣**"*，且**选择后不支持转换**。
> 我们买的是普通归档存储包 → 选 ZRS 会导致 **2T 额度一分钱抵扣不到**，同时按更高单价按量扣费，且只能删库重建。
> LRS 本身已是 11 个 9 的持久性，对备份（本机热层之外的第三份副本）完全够用。

2. **创建 RAM 子用户**（实际：`li.kong`），仅勾选「OpenAPI 调用访问」，保存 AccessKey ID/Secret。

3. **绑定自定义权限策略**（实际策略名「OSS存储策略」）——注意 **Action 中没有任何 Delete**：

```json
{
  "Version": "1",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "oss:PutObject",
        "oss:GetObject",
        "oss:ListObjects",
        "oss:ListParts",
        "oss:AbortMultipartUpload"
      ],
      "Resource": [
        "acs:oss:*:*:yada-newpm-backup",
        "acs:oss:*:*:yada-newpm-backup/*"
      ]
    }
  ]
}
```

**关于这份策略的三个实测要点：**

- **模板里的 Bucket 占位符必须替换成真名**。留着占位符时 JSON 语法完全合法，控制台校验显示「错误 0、安全警告 0」——但那只验语法，验不了语义。错误要到运行时才以 `AccessDenied` 发作，而 AccessDenied 的成因有十几种，排查成本极高。
- **改 Bucket 名后策略要同步改**，保存会生成新版本（v2/v3），需确认新版本被设为**当前版本**。
- **故意不含 `oss:GetObjectMeta`**，因此 `ossutil stat` 会返回 403。这是预期行为，**不要为此放宽权限**——上传脚本改用 `ossutil ls` 读取校验字段即可（ls 输出已含 Size 与 StorageClass）。

4. **配置生命周期规则**（Bucket → 基础设置 → 生命周期）：

> ⚠️ **下表是早期推演版本，已被最终方案取代。实际配置见 §11。**
> 最终去掉了 monthly 分层、数据库保留期改 30 天、附件单目录 60 天。原因见 §11 的决策说明。

| 规则名 | 前缀 | 动作（最终值见 §11） |
|---|---|---|
| `expire-db` | `newpm-mysql/` | ~~365 天~~ → **30 天** |
| `expire-upload` | `newpm-upload/` | **60 天**（原 daily/monthly 分层已废弃，合并为单目录） |
| `clean-parts` | （全局） | 碎片 **7 天**后删除 |

`clean-parts` 别漏——13G 附件走分片上传，中断会残留碎片，这些碎片**在文件列表里看不见但照常计费**。

⚠️ 附件保留期**不得低于 60 天**（归档最短计费期），否则提前删除仍按 60 天收费。数据库单份仅 5M，30 天虽触发最短计费但金额可忽略，故按需求定 30 天。

### 步骤 2：安装 ossutil（实际版本 v1.7.18）

```bash
ssh k3s001 "sudo curl -sS -o /usr/local/bin/ossutil \
  https://gosspublic.alicdn.com/ossutil/1.7.18/ossutil64 && \
  sudo chmod 755 /usr/local/bin/ossutil && ossutil --version"
```

> **版本路径要先探测**：`1.7.19/ossutil64` 返回 **404**（该版本只发布了 zip 包）；实测可直接下载裸二进制的是 `1.7.14 / 1.7.16 / 1.7.18`。装之前先 `curl -sI <url> | head -1` 确认 200，别假设某个版本号一定存在。

若出网受限导致下载失败，改为本地下载后推送：

```bash
curl -o /tmp/ossutil64 https://gosspublic.alicdn.com/ossutil/1.7.18/ossutil64
scp /tmp/ossutil64 k3s001:/tmp/
ssh k3s001 "sudo install -m 755 /tmp/ossutil64 /usr/local/bin/ossutil && ossutil --version"
```

### 步骤 3：写入凭证配置

`/root/.ossutilconfig`，**权限必须 600**（含明文 AccessKey）：

```ini
[Credentials]
language=CH
endpoint=oss-cn-beijing-internal.aliyuncs.com
accessKeyID=<AK>
accessKeySecret=<SK>
```

```bash
ssh k3s001 "sudo chmod 600 /root/.ossutilconfig && sudo chown root:root /root/.ossutilconfig"
```

> 用**内网** endpoint。若日后需从办公网直接拉备份，在本机另配公网 endpoint，不要改服务器上这份。

### 步骤 4：新增上传脚本 `/usr/local/bin/sync-backup-to-oss.sh`

独立成一个脚本，便于手动补传和单独排障：

```bash
#!/bin/bash
# 上传单个备份文件到 OSS 归档存储，含大小 + 存储类型双校验与重试
# 用法: sync-backup-to-oss.sh <本地文件> <OSS前缀>
set -eo pipefail
export PATH=/usr/local/bin:/usr/bin:/bin

FILE="$1"
PREFIX="$2"
BUCKET="yada-newpm-backup"
LOG="/backup/oss-sync.log"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >> "$LOG"; }

[ -n "$FILE" ] && [ -n "$PREFIX" ] || { log "ERROR 用法: $0 <文件> <前缀>"; exit 2; }
[ -f "$FILE" ] || { log "ERROR 文件不存在: $FILE"; exit 1; }
# 防呆：0 字节文件绝不上传（kubectl exec 失败时 gzip 仍会产出空文件）
[ -s "$FILE" ] || { log "ERROR 文件为空，拒绝上传: $FILE"; exit 1; }

NAME=$(basename "$FILE")
TARGET="oss://${BUCKET}/${PREFIX}/${NAME}"
LOCAL_SIZE=$(stat -c%s "$FILE")

log "开始上传 $NAME ($(du -h "$FILE" | cut -f1)) -> $TARGET"

# --retry-times 网络重试；大文件分片并发；显式声明归档存储类型（双保险）
ossutil cp "$FILE" "$TARGET" \
  --meta "X-oss-Storage-Class:Archive" \
  --retry-times 5 \
  --parallel 4 \
  --part-size 10485760 \
  --force \
  >> "$LOG" 2>&1

# 校验用 ls 而非 stat：RAM 策略未授予 GetObjectMeta（最小权限），stat 会 403。
# ls 输出字段: $1..$4=时间 $5=Size $6=StorageClass $7=ETAG $8=ObjectName
META=$(ossutil ls "$TARGET" 2>/dev/null | awk -v t="$TARGET" '$NF==t {print $5" "$6}')
REMOTE_SIZE=$(echo "$META" | awk '{print $1}')
REMOTE_CLASS=$(echo "$META" | awk '{print $2}')

if [ "$LOCAL_SIZE" != "$REMOTE_SIZE" ]; then
  log "ERROR 大小校验失败 $NAME 本地=${LOCAL_SIZE} 远端=${REMOTE_SIZE:-<空>}"
  exit 1
fi

if [ "$REMOTE_CLASS" != "Archive" ]; then
  log "ERROR 存储类型异常 $NAME 期望=Archive 实际=${REMOTE_CLASS:-<空>}（会导致资源包无法抵扣）"
  exit 1
fi

log "OK $NAME (${LOCAL_SIZE} bytes, ${REMOTE_CLASS})"
```

**为什么校验用 `ls` 不用 `stat`**：策略最小权限不含 `oss:GetObjectMeta`，`stat` 必然 403。而 `ls` 的输出本就带 `Size` 和 `StorageClass` 两个字段，一次调用同时校验**大小 + 存储类型**，信息比 `stat` 方案更全。**能改自己代码绕开的，就不要放宽线上权限。**

**为什么不用 MD5 校验**：大文件走分片上传，ETAG 形如 `FC026AEC...-208`（各分片 MD5 的二次哈希 + 分片数），**不等于文件 MD5**，无法与本地 `md5sum` 对照。故以字节数为准。

安装：

```bash
ssh k3s001 "sudo chmod 755 /usr/local/bin/sync-backup-to-oss.sh"
```

### 步骤 5：存量补传（阶段一收尾）

把现有 31 份数据库 + 6 份附件全部推上 OSS，建立异地基线。**本步骤不触碰任何现有备份脚本**。

先落一个补传脚本 `/usr/local/bin/oss-initial-sync.sh`：

```bash
#!/bin/bash
# 一次性补传存量备份到 OSS（可重复执行，失败项会在日志中标 FAILED）
export PATH=/usr/local/bin:/usr/bin:/bin
SYNC=/usr/local/bin/sync-backup-to-oss.sh
OK=0; FAIL=0

echo "===== 存量补传开始 $(date '+%F %T') ====="

for f in /backup/newpm-mysql/*.sql.gz; do
  if "$SYNC" "$f" "newpm-mysql"; then OK=$((OK+1)); else FAIL=$((FAIL+1)); echo "FAILED: $f"; fi
done

# 存量附件统一进 monthly/（180天保留），不进 60 天的 daily/
for f in /backup/newpm-upload/*.tar.gz; do
  if "$SYNC" "$f" "newpm-upload/monthly"; then OK=$((OK+1)); else FAIL=$((FAIL+1)); echo "FAILED: $f"; fi
done

echo "===== 补传结束 成功=$OK 失败=$FAIL $(date '+%F %T') ====="
[ "$FAIL" -eq 0 ]
```

执行（13G 附件耗时较长，放后台跑）：

```bash
ssh k3s001 "sudo chmod 755 /usr/local/bin/oss-initial-sync.sh && \
  sudo nohup /usr/local/bin/oss-initial-sync.sh > /backup/oss-initial-sync.log 2>&1 &"

# 观察进度
ssh k3s001 "tail -f /backup/oss-initial-sync.log"
```

脚本**可重复执行**：ossutil 默认覆盖同名对象，中途失败直接重跑即可，无需手工挑拣。

**阶段一验证**：

```bash
# 份数应为 31 和 6
ssh k3s001 "ossutil ls oss://yada-newpm-backup/newpm-mysql/ | grep -c '.sql.gz'"
ssh k3s001 "ossutil ls oss://yada-newpm-backup/newpm-upload/monthly/ | grep -c '.tar.gz'"

# 确认存储类型确实是 Archive（而非默认标准型）
ssh k3s001 "ossutil stat oss://yada-newpm-backup/newpm-mysql/newpm-20260724.sql.gz | grep -i storage-class"

# 确认无失败项
ssh k3s001 "grep -c ERROR /backup/oss-sync.log; grep FAILED /backup/oss-initial-sync.log || echo '无失败项'"
```

到此**异地容灾能力已经建立**——即使 k3s001 当场报废，数据也还在。后续阶段二属于优化，可从容进行。

---

## 阶段二：自动化与提频（后续执行）

### 步骤 6：改造备份脚本 + 提频到方案 B

**6.1 `backup-newpm-db.sh`** —— 三处改动：

```bash
# ① 文件名加小时，避免一天 4 次互相覆盖
DATE=$(date +%Y%m%d-%H%M)          # 原为 date +%Y%m%d
FILE="$BACKUP_DIR/newpm-${DATE}.sql.gz"

# ② 修既有隐患：kubectl exec 失败时 gzip 仍成功，会产出 0 字节文件而脚本报"成功"
#    位置：mysqldump 之后、上传之前
if [ ! -s "$FILE" ] || [ "$(stat -c%s "$FILE")" -lt 1048576 ]; then
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR 备份文件异常（<1MB），中止" >> "$LOG"
  exit 1
fi

# ③ 上传（放在 find -delete 清理之前：先保异地，再清本地）
/usr/local/bin/sync-backup-to-oss.sh "$FILE" "newpm-mysql" \
  || echo "[$(date '+%Y-%m-%d %H:%M:%S')] 警告：OSS 上传失败，本地备份已保留" >> "$LOG"
```

（当前正常备份约 4.9M，1MB 阈值有充足余量。）

**6.2 `backup-newpm-upload.sh`** —— 增加月度分流：

```bash
# 每月 1 号进 monthly/（180天），其余进 daily/（60天）
if [ "$(date +%d)" = "01" ]; then
  OSS_PREFIX="newpm-upload/monthly"
else
  OSS_PREFIX="newpm-upload/daily"
fi

/usr/local/bin/sync-backup-to-oss.sh "$FILE" "$OSS_PREFIX" \
  || echo "[$(date '+%Y-%m-%d %H:%M:%S')] 警告：OSS 上传失败，本地备份已保留" >> "$LOG"
```

**6.3 crontab 提频**：

```cron
# 数据库：每日 1 次 → 每 6 小时 1 次
10 0,6,12,18 * * * /usr/local/bin/backup-newpm-db.sh >> /backup/newpm-mysql/cron.log 2>&1

# 附件：每周日 → 每日
20 1 * * *        /usr/local/bin/backup-newpm-upload.sh >> /backup/newpm-upload/cron.log 2>&1
```

> 附件改日备后，每天会新增一次 2.7G 目录的 `tar -czf`（数分钟 CPU）。实例为 4 核 `ecs.hfg7.xlarge`，01:20 执行，业务影响可忽略；但需确保本地有 ≥3G 空闲用于生成临时 tar。

### 步骤 7：收敛本地保留期（**确认阶段一 + 步骤 6 稳定运行数日后再做**）

- `backup-newpm-db.sh`：`KEEP_DAYS=30` → `KEEP_DAYS=7`（每 6 小时一份 → 28 份，约 140M）
- `backup-newpm-upload.sh`：`KEEP_DAYS=30` → `KEEP_DAYS=2`（日备 → 2 份，约 4.8G）

预计释放本地约 **9G**。

**⚠️ 顺序不可颠倒**：必须先确认 OSS 上传稳定，再缩短本地保留。反过来会出现"本地已删、异地没传成功"的裸奔窗口。

## 6. 恢复演练（方案必须验证才算完成）

异地备份最常见的失败模式不是没备份，而是**备份存在但恢复不出来**。上线后必须实测一次：

```bash
# 1. 列出 OSS 上的备份
ssh k3s001 "ossutil ls oss://yada-newpm-backup/newpm-mysql/ | tail -5"

# 2. 发起解冻（归档对象必须先解冻；已开启归档直读则可跳过）
ssh k3s001 "ossutil restore oss://yada-newpm-backup/newpm-mysql/newpm-20260724.sql.gz"

# 3. 轮询解冻状态，等待 x-oss-restore 变为 ongoing-request="false"
ssh k3s001 "ossutil stat oss://yada-newpm-backup/newpm-mysql/newpm-20260724.sql.gz | grep -i restore"

# 4. 下载并验证完整性（只验证，不导入生产库）
ssh k3s001 "ossutil cp oss://yada-newpm-backup/newpm-mysql/newpm-20260724.sql.gz /tmp/verify.sql.gz && \
            gzip -t /tmp/verify.sql.gz && echo 'gzip 完整性 OK' && \
            zcat /tmp/verify.sql.gz | tail -5"
```

**记录实际解冻耗时**并写回 CLAUDE.md——这个数字决定灾难发生时的 RTO 预期。

## 7. 需要你提供的信息（阻塞阶段一开工）

| 项 | 说明 |
|---|---|
| **Bucket 名称** | 必须确认地域为 **cn-beijing（华北2·北京）**。不在北京则内网口失效，需改走公网——而本机直连公网受限，风险显著上升 |
| **AccessKey ID / Secret** | RAM 子用户的，**不要用主账号**；权限按 §5 步骤 1.3 的策略（无 Delete） |

保留期已定，无需再确认：DB 365 天 / 附件日备 60 天 / 附件月存 365 天。

## 8. 验收标准

**阶段一（存量上传）**

- [ ] `ossutil ls` 列出 **31 份** DB（`newpm-mysql/`）+ **6 份**附件（`newpm-upload/monthly/`）
- [ ] `ossutil stat` 确认对象 `Storage-Class` 为 **Archive**（不是默认标准型，否则不走已购资源包）
- [ ] `/backup/oss-initial-sync.log` 结尾为 `失败=0`，且 `oss-sync.log` 无 ERROR
- [ ] 生命周期规则（含 `clean-parts`）在控制台可见且状态为「启用」
- [ ] 完成一次解冻+下载+`gzip -t` 恢复演练，**并记录实际解冻耗时**（决定灾难时的 RTO 预期）
- [ ] 故意用错误 AK 跑一次，确认脚本**失败退出且不删本地备份**

**阶段二（自动化 + 提频）**

- [ ] 数据库文件名含小时（`newpm-YYYYMMDD-HHMM.sql.gz`），一天 4 份**互不覆盖**
- [ ] 次日观察：OSS 上出现 4 份 DB + 1 份附件，大小与本地一致
- [ ] 每月 1 号那份附件确实落在 `newpm-upload/monthly/`
- [ ] 收敛保留期后 `df -h` 确认本地释放约 9G
- [ ] CLAUDE.md 的 Backup Strategy 章节更新为新架构

## 9. 遗留风险

| 风险 | 缓解 |
|---|---|
| AccessKey 明文存于 `/root/.ossutilconfig` | 权限 600 + RAM 最小权限（无 delete）+ 定期轮换 |
| 上传失败仅写日志，无主动告警 | 后续可加 webhook/邮件；当前靠验收项定期人工核对 |
| 附件脚本硬编码 PVC UUID 路径 | 与本方案无关的既有问题，PVC 重建时需同步改脚本 |
| 归档解冻延迟拉长 RTO | 本地热层兜底日常恢复；如需免解冻，开启归档直读 |

---

## 10. 实施记录（阶段一，2026-07-24 完成）

### 落地结果

| 项 | 实际值 |
|---|---|
| Bucket | `yada-newpm-backup`（华北2·北京 / 归档 / **LRS** / 私有 / 阻止公共访问） |
| RAM 用户 | `li.kong`，绑定自定义策略「OSS存储策略」（无 Delete 权限） |
| ossutil | v1.7.18，`/usr/local/bin/ossutil` |
| 凭证 | `/root/.ossutilconfig`，权限 600，内网 endpoint |
| 脚本 | `/usr/local/bin/sync-backup-to-oss.sh`、`/usr/local/bin/oss-initial-sync.sh` |
| 日志 | `/backup/oss-sync.log`（逐文件）、`/backup/oss-initial-sync.log`（批次） |

**存量补传：成功 37 / 失败 0**，耗时 **75 秒**（10:43:53 → 10:45:08）。

| 前缀 | 份数 | 大小 | 非 Archive |
|---|---|---|---|
| `newpm-mysql/` | 31 | 131.6 MB | 0 |
| `newpm-upload/monthly/` | 6 | 12.94 GB | 0 |

内网实测吞吐：单文件 22.8 MB/s，整批 13G 约 175 MB/s（分片并发）。

### 踩过的坑（按发生顺序）

1. **ossutil 下载 404** —— `1.7.19/ossutil64` 路径不存在（该版本只发 zip）。装之前先 `curl -sI` 探测，可用的裸二进制是 1.7.14 / 1.7.16 / 1.7.18。

2. **策略里的 `<BUCKET>` 占位符没替换** —— JSON 语法合法、控制台校验「错误 0」，但等价于给一个不存在的资源授权。**语法校验器验不了语义。**

3. **策略创建 ≠ 授权** —— 建好自定义策略后，还必须在「授权管理」里绑定到 RAM 用户。这是两个独立动作。

4. **改策略后要确认新版本是当前版本** —— 编辑策略会生成 v2/v3，需手动确认已切换。

5. **RAM 策略 ≠ 创建资源** —— 策略里写了 Bucket 名，不代表 Bucket 存在。权限和资源是两条独立的生命线，报错却长得差不多。

6. **`NoSuchBucket` 不能证明权限正确** —— OSS 对不存在的 Bucket 直接返回 404，根本走不到权限检查。别据此认为策略没问题。

7. **⚠️ 存储冗余类型默认推荐 ZRS，必须改 LRS** —— ZRS 的费用**只能被同城冗余存储包抵扣**，普通归档包抵扣不了；且**创建后不可转换**，只能删库重建。本次因此删过一次 Bucket。

8. **Bucket 名全局唯一，且删除后不立即释放** —— 删掉 `newpm-backup-bj` 后无法立即重建同名，最终改用 `yada-newpm-backup`。**改名后策略里的 Resource 必须同步改。**

9. **`ossutil stat` 返回 403** —— 最小权限策略不含 `oss:GetObjectMeta`。**不放宽权限，改用 `ossutil ls`** 读取 Size + StorageClass 完成校验（信息还更全）。

10. **分片上传的 ETAG 不是文件 MD5** —— 形如 `FC026AEC...-208`（分片 MD5 的二次哈希 + 分片数），无法与本地 `md5sum` 对照，故以字节数校验。

### 附带发现

`newpm-upload-20260628.tar.gz` 与 `20260630.tar.gz` 的 **ETAG 与字节数完全相同** → 这两天附件目录零变化，6/30 那份手动备份是纯冗余副本。

---

## 11. 最终落地参数（权威版，2026-07-24 全部完成）

> **本节是唯一权威。前面 §4、§5 的方案 A/B 对比、365/180/monthly 等均为决策过程的推演，实施中经多轮调整后已被本节取代。以本节为准。**

### 11.1 最终备份策略

| 对象 | 频率 | 本地保留 | OSS 前缀 | OSS 保留 | RPO |
|---|---|---|---|---|---|
| 数据库 `ry-vue` | 每 6 小时（00/06/12/18:10） | **0 份（纯 OSS）** | `newpm-mysql/` | **30 天** | 6 小时 |
| 附件 upload-pvc | 每日 01:20 | **0 份（纯 OSS）** | `newpm-upload/`（**单目录**） | **60 天** | 1 天 |

- **纯 OSS 模式**（2026-07-25 起，`KEEP_COUNT=0`）：本地一份不留，备份生成→上传 OSS 成功→删本地这份，本地稳态为空。最省服务器磁盘。
- **安全垫**：删本地在「上传成功」之后；上传失败则该份留本地兜底，**绝不会本地+OSS 两头皆空**。
- **代价**：恢复必走 OSS 归档解冻（有延迟，非秒级），且失去本地秒级回退。
- **OSS 为主存储**：数据库 30 天、附件 60 天，到期由生命周期规则自动删。

### 11.2 OSS 生命周期规则（控制台已配 3 条）

| 规则名 | 生效范围 | 动作 |
|---|---|---|
| `expire-db` | 前缀 `newpm-mysql/` | 最后修改 30 天后**删除** |
| `expire-upload` | 前缀 `newpm-upload/` | 最后修改 60 天后**删除** |
| `clean-parts` | 整个 Bucket | 未完成分片 7 天后清理（文件策略「不启用」） |

> `newpm-upload/` 前缀一并覆盖历史遗留的 `newpm-upload/daily/`、`newpm-upload/monthly/` 子目录旧文件——它们会在各自满 60 天后自动清除，无需手动删。

### 11.3 相比早期推演的关键调整及原因

| 项 | 早期推演 | 最终 | 为什么改 |
|---|---|---|---|
| 附件目录 | daily/ + monthly/ 分层 | **单目录 newpm-upload/** | 全量备份 + 2T 额度充裕，分层是过度设计；monthly 与当天 daily 内容完全重复（同一 tar 存两遍） |
| 数据库 OSS 保留 | 365 天 | **30 天** | 保留期应按业务需求定，非按容量。单份 5M，30 天足够；历史全靠 OSS 无需一年 |
| 附件 OSS 保留 | 60（daily）+180（monthly） | **统一 60 天** | 60 天卡归档最短计费下限，最省且覆盖误删发现窗口；砍掉 monthly 半年存档（无合规回溯需求） |
| 本地附件 | 2 份 | **1 份** | 极致省服务器磁盘（单份 2.4G） |

### 11.4 自动化落地（全部已生效）

- **crontab（root）**：`10 */6 * * *` 数据库；`20 1 * * *` 附件；`0 8 * * *` 健康巡检。cron 服务 `active + enabled`，**抗服务器重启**。
- **脚本**（`/usr/local/bin/`，版本管理副本在 `ops/backup/`）：`backup-newpm-db.sh`、`backup-newpm-upload.sh`、`sync-backup-to-oss.sh`、`check-backup-health.sh`。
- **四道安全闸门**：磁盘不足拒备 / 文件异常拒传 / 完整性校验（`Dump completed on` + `tar -tzf`）/ **上传失败拒清本地**。
- **本地 Mac 第三副本**：`PM/PM-backups/20260724/`（数据库 + 附件全量，SHA256 已核验）。3-2-1 原则达成。

### 11.5 剩余待办

- [ ] **恢复演练**：主账号解冻一份 OSS 备份 → 下载 → `gzip -t`，记录解冻耗时（唯一未实战验证的环节）
- [ ] 轮换 AccessKey（本次 AK 曾在对话中明文传输）
- [ ] 可选：Bucket 开启服务端加密「OSS 完全托管」（免费，对脚本透明）
