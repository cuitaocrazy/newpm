#!/bin/bash
# newpm 数据库备份 -> 本地热层 + 阿里云 OSS 归档（主存储）
# 策略：OSS 为主，本地仅留少量应急副本。上传失败时拒绝清理本地。
set -eo pipefail
export PATH=/usr/local/bin:/usr/bin:/bin

BACKUP_DIR="/backup/newpm-mysql"
DATE=$(date +%Y%m%d-%H%M)          # 带小时：每 6 小时一次，不能只到天粒度否则互相覆盖
FILE="$BACKUP_DIR/newpm-${DATE}.sql.gz"
LOG="$BACKUP_DIR/backup.log"
OSS_PREFIX="newpm-mysql"
KEEP_COUNT=0                       # 纯 OSS：上传成功后删掉本次这份，本地稳态为空
                                   # （上传失败则本闸门跳过清理，该份留本地兜底，不会两头皆空）
MIN_FREE_GB=3                      # 低于此空闲空间则拒绝开始
MIN_SIZE_BYTES=1048576             # dump 小于 1MB 视为异常（正常约 5MB）
DB_PWD="${NEWPM_DB_PWD:-password}" # DB 密码：可用环境变量覆盖，默认取本环境既定值

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >> "$LOG"; }

mkdir -p "$BACKUP_DIR"
log "===== 开始数据库备份 ====="

# --- 闸门1：磁盘空间 ---
AVAIL_GB=$(df -BG --output=avail "$BACKUP_DIR" | tail -1 | tr -dc '0-9')
if [ "${AVAIL_GB:-0}" -lt "$MIN_FREE_GB" ]; then
  log "ERROR 磁盘空间不足（剩 ${AVAIL_GB}G < ${MIN_FREE_GB}G），中止备份"
  exit 1
fi

# --- 导出 ---
kubectl exec -n newpm mysql-0 -- \
  mysqldump -u root -p"$DB_PWD" \
    --default-character-set=utf8mb4 \
    --single-transaction \
    --routines \
    --triggers \
    --events \
    ry-vue \
  | gzip > "$FILE"

# --- 闸门2：文件大小 ---
# kubectl exec 失败时 gzip 仍会成功并产出空文件，管道退出码不可靠，必须查文件本身
ACTUAL_SIZE=$(stat -c%s "$FILE" 2>/dev/null || echo 0)
if [ "$ACTUAL_SIZE" -lt "$MIN_SIZE_BYTES" ]; then
  log "ERROR 备份文件异常（${ACTUAL_SIZE} bytes < ${MIN_SIZE_BYTES}），删除并中止"
  rm -f "$FILE"
  exit 1
fi

# --- 闸门3：mysqldump 完整性金标准 ---
# dump 中途失败（连接断/磁盘满/被 kill）不会写出这一行，而截断的文件大小看起来完全正常
if ! zcat "$FILE" | tail -5 | grep -q "Dump completed on"; then
  log "ERROR dump 未正常结束（缺少 'Dump completed on' 标记），删除并中止"
  rm -f "$FILE"
  exit 1
fi

log "备份完成：$(basename "$FILE") ($(du -h "$FILE" | cut -f1))"

# --- 上传 OSS（主存储）---
UPLOAD_OK=0
if /usr/local/bin/sync-backup-to-oss.sh "$FILE" "$OSS_PREFIX"; then
  UPLOAD_OK=1
  log "OSS 上传成功"
else
  log "ERROR OSS 上传失败 —— 本地文件全部保留，跳过清理"
fi

# --- 闸门4：仅在上传成功后才清理本地 ---
# 本地是应急热层，OSS 才是主存储；上传没成功就绝不能动本地副本
if [ "$UPLOAD_OK" = "1" ]; then
  OLD_LIST=$(ls -t "$BACKUP_DIR"/newpm-*.sql.gz 2>/dev/null | tail -n +$((KEEP_COUNT+1)) || true)
  DELETED=$(echo -n "$OLD_LIST" | grep -c . || true)
  [ -n "$OLD_LIST" ] && echo "$OLD_LIST" | xargs -r rm -f
  log "本地清理完成（保留最新 ${KEEP_COUNT} 份，删除 ${DELETED} 份）"
else
  log "跳过本地清理（OSS 未确认）"
fi

log "===== 结束 ====="
