#!/bin/bash
# newpm 附件备份 -> 本地热层 + 阿里云 OSS 归档（主存储）
# 策略：OSS 为主，本地仅留少量应急副本。上传失败时拒绝清理本地。
# 全量备份，统一进 newpm-upload/（OSS 生命周期 60 天删除）。
set -eo pipefail
export PATH=/usr/local/bin:/usr/bin:/bin

UPLOAD_DIR="/var/lib/rancher/k3s/storage/pvc-01f86b13-e8e1-4a3e-a152-1c1449fecf1b_newpm_upload-pvc"
BACKUP_DIR="/backup/newpm-upload"
DATE=$(date +%Y%m%d)
FILE="$BACKUP_DIR/newpm-upload-${DATE}.tar.gz"
LOG="$BACKUP_DIR/backup.log"
KEEP_COUNT=1                       # 本地仅留最新 1 份（应急热层，历史版本靠 OSS）
MIN_FREE_GB=8                      # tar 峰值需同时容纳新旧两份

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >> "$LOG"; }

mkdir -p "$BACKUP_DIR"
log "===== 开始附件备份 ====="

# --- 闸门0：源目录必须存在（PVC 重建会换 UUID，路径失效则静默备份空目录）---
if [ ! -d "$UPLOAD_DIR" ]; then
  log "ERROR 源目录不存在：$UPLOAD_DIR（PVC 是否已重建？需同步更新本脚本），中止"
  exit 1
fi
SRC_FILES=$(find "$UPLOAD_DIR" -type f | wc -l)
if [ "$SRC_FILES" -lt 1 ]; then
  log "ERROR 源目录为空（0 个文件），拒绝生成空备份，中止"
  exit 1
fi

# --- 闸门1：磁盘空间 ---
AVAIL_GB=$(df -BG --output=avail "$BACKUP_DIR" | tail -1 | tr -dc '0-9')
if [ "${AVAIL_GB:-0}" -lt "$MIN_FREE_GB" ]; then
  log "ERROR 磁盘空间不足（剩 ${AVAIL_GB}G < ${MIN_FREE_GB}G），中止备份"
  exit 1
fi

# --- 打包 ---
tar -czf "$FILE" -C "$(dirname "$UPLOAD_DIR")" "$(basename "$UPLOAD_DIR")"

# --- 闸门2：完整性校验 ---
# tar -tzf 一次遍历同时验证 gzip 流与 tar 结构，并顺便点出包内文件数，
# 比先 gzip -t 再 tar -tzf 少解压一遍 2.4G。
TMPLIST="/tmp/.newpm-tarlist.$$"
if ! tar -tzf "$FILE" > "$TMPLIST" 2>/dev/null; then
  log "ERROR tar.gz 校验失败（文件损坏），删除并中止"
  rm -f "$FILE" "$TMPLIST"
  exit 1
fi
TAR_FILES=$(grep -vc '/$' "$TMPLIST" || true)
rm -f "$TMPLIST"

if [ "${TAR_FILES:-0}" -lt 1 ]; then
  log "ERROR 备份包内 0 个文件，删除并中止"
  rm -f "$FILE"
  exit 1
fi

log "备份完成：$(basename "$FILE") ($(du -h "$FILE" | cut -f1))，源 ${SRC_FILES} 文件 / 包内 ${TAR_FILES} 文件"

# --- 上传 OSS（主存储）---
# 全量备份，单目录统一保留：OSS 生命周期规则对 newpm-upload/ 前缀设 60 天删除
# （60 天 = 归档最短计费期，卡在计费下限，既覆盖误删发现窗口又不产生提前删除费）
OSS_PREFIX="newpm-upload"

UPLOAD_OK=0
if /usr/local/bin/sync-backup-to-oss.sh "$FILE" "$OSS_PREFIX"; then
  UPLOAD_OK=1
  log "OSS 上传成功 -> $OSS_PREFIX/"
else
  log "ERROR OSS 上传失败 —— 本地文件全部保留，跳过清理"
fi

# --- 闸门3：仅在上传成功后才清理本地 ---
if [ "$UPLOAD_OK" = "1" ]; then
  OLD_LIST=$(ls -t "$BACKUP_DIR"/newpm-upload-*.tar.gz 2>/dev/null | tail -n +$((KEEP_COUNT+1)) || true)
  DELETED=$(echo -n "$OLD_LIST" | grep -c . || true)
  [ -n "$OLD_LIST" ] && echo "$OLD_LIST" | xargs -r rm -f
  log "本地清理完成（保留最新 ${KEEP_COUNT} 份，删除 ${DELETED} 份）"
else
  log "跳过本地清理（OSS 未确认）"
fi

log "===== 结束 ====="
