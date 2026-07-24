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
