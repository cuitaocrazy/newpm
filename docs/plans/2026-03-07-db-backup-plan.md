# 数据库自动备份方案

**日期：** 2026-03-07
**方案：** B — 服务器定时备份 + 本地 Mac 定时拉取

---

## 架构

```
凌晨 00:10  k3s001 cron
    → 进入 mysql-0 Pod 执行 mysqldump
    → 压缩 → /backup/newpm-mysql/newpm-YYYYMMDD.sql.gz
    → 自动清理 30 天前的备份

每天 08:00  本地 Mac launchd
    → rsync 从 k3s001 拉取新备份
    → 存到 ~/backup/newpm-mysql/
    → 自动清理本地 30 天前备份
```

---

## Task 1：服务器备份脚本 + cron

### Step 1：创建备份脚本

SSH 到 k3s001，创建脚本：

```bash
ssh k3s001
mkdir -p /backup/newpm-mysql
cat > /usr/local/bin/backup-newpm-db.sh << 'EOF'
#!/bin/bash
set -e

BACKUP_DIR="/backup/newpm-mysql"
DATE=$(date +%Y%m%d)
FILE="$BACKUP_DIR/newpm-${DATE}.sql.gz"
LOG="$BACKUP_DIR/backup.log"
KEEP_DAYS=30

mkdir -p "$BACKUP_DIR"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] 开始备份..." >> "$LOG"

# 进入 mysql-0 Pod 执行 mysqldump
kubectl exec -n newpm mysql-0 -- \
  mysqldump -u root -ppassword \
    --default-character-set=utf8mb4 \
    --single-transaction \
    --routines \
    --triggers \
    ry-vue \
  | gzip > "$FILE"

SIZE=$(du -sh "$FILE" | cut -f1)
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 备份完成：$FILE ($SIZE)" >> "$LOG"

# 清理 30 天前的备份
find "$BACKUP_DIR" -name "newpm-*.sql.gz" -mtime +${KEEP_DAYS} -delete
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 旧备份清理完成（保留最近 ${KEEP_DAYS} 天）" >> "$LOG"
EOF

chmod +x /usr/local/bin/backup-newpm-db.sh
```

### Step 2：验证脚本可运行

```bash
/usr/local/bin/backup-newpm-db.sh
ls -lh /backup/newpm-mysql/
cat /backup/newpm-mysql/backup.log
```

预期输出：
```
-rw-r--r-- 1 root root  8.2M Mar 07 00:10 newpm-20260307.sql.gz
[2026-03-07 00:10:15] 开始备份...
[2026-03-07 00:10:28] 备份完成：/backup/newpm-mysql/newpm-20260307.sql.gz (8.2M)
[2026-03-07 00:10:28] 旧备份清理完成（保留最近 30 天）
```

### Step 3：添加 cron 定时任务

```bash
crontab -e
```

添加以下内容：

```cron
10 0 * * * /usr/local/bin/backup-newpm-db.sh >> /backup/newpm-mysql/cron.log 2>&1
```

### Step 4：验证 cron 已生效

```bash
crontab -l
```

预期看到：
```
10 0 * * * /usr/local/bin/backup-newpm-db.sh >> /backup/newpm-mysql/cron.log 2>&1
```

---

## Task 2：本地 Mac 定时拉取脚本

### Step 1：创建本地备份目录和拉取脚本

在本地 Mac 执行：

```bash
mkdir -p ~/backup/newpm-mysql
cat > ~/backup/sync-newpm-db.sh << 'EOF'
#!/bin/bash
set -e

REMOTE="k3s001:/backup/newpm-mysql/"
LOCAL="$HOME/backup/newpm-mysql/"
LOG="$LOCAL/sync.log"
KEEP_DAYS=30

mkdir -p "$LOCAL"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] 开始同步..." >> "$LOG"

rsync -avz --progress \
  -e "ssh -i $HOME/.ssh/id_ed25519" \
  "$REMOTE" "$LOCAL" >> "$LOG" 2>&1

# 清理本地 30 天前的备份
find "$LOCAL" -name "newpm-*.sql.gz" -mtime +${KEEP_DAYS} -delete

echo "[$(date '+%Y-%m-%d %H:%M:%S')] 同步完成" >> "$LOG"
EOF

chmod +x ~/backup/sync-newpm-db.sh
```

### Step 2：手动验证一次同步

```bash
~/backup/sync-newpm-db.sh
ls -lh ~/backup/newpm-mysql/
```

预期看到备份文件已同步到本地。

### Step 3：创建 launchd 定时任务（每天 08:00 自动运行）

```bash
cat > ~/Library/LaunchAgents/com.kongli.sync-newpm-db.plist << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.kongli.sync-newpm-db</string>
    <key>ProgramArguments</key>
    <array>
        <string>/bin/bash</string>
        <string>/Users/kongli/backup/sync-newpm-db.sh</string>
    </array>
    <key>StartCalendarInterval</key>
    <dict>
        <key>Hour</key>
        <integer>8</integer>
        <key>Minute</key>
        <integer>0</integer>
    </dict>
    <key>StandardOutPath</key>
    <string>/Users/kongli/backup/newpm-mysql/launchd.log</string>
    <key>StandardErrorPath</key>
    <string>/Users/kongli/backup/newpm-mysql/launchd-error.log</string>
    <key>RunAtLoad</key>
    <false/>
</dict>
</plist>
EOF
```

### Step 4：加载 launchd 任务

```bash
launchctl load ~/Library/LaunchAgents/com.kongli.sync-newpm-db.plist
launchctl list | grep sync-newpm-db
```

预期看到任务已注册（exit code 0 或 -）。

---

## 目录结构

**服务器 k3s001：**
```
/backup/newpm-mysql/
├── newpm-20260307.sql.gz   ← 当天备份
├── newpm-20260308.sql.gz
├── ...
├── backup.log              ← 备份日志
└── cron.log                ← cron 输出日志
```

**本地 Mac：**
```
~/backup/newpm-mysql/
├── newpm-20260307.sql.gz   ← 从服务器同步
├── newpm-20260308.sql.gz
├── ...
├── backup.log              ← 服务器备份日志（同步过来）
└── sync.log                ← 本地同步日志
```

---

## 恢复方法（备用）

如需从备份恢复：

```bash
# 在 k3s001 上恢复
gunzip < /backup/newpm-mysql/newpm-20260307.sql.gz | \
  kubectl exec -i mysql-0 -n newpm -- \
  mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

---

## 注意事项

1. 本地同步依赖 Mac 开机，若 Mac 关机则当天跳过，次日开机不会自动补拉（可手动执行 `~/backup/sync-newpm-db.sh`）
2. 服务器备份保留 30 天，超出自动删除
3. 备份文件约 8～15 MB/天（压缩后），30 天约占 250～450 MB
