#!/bin/bash
# newpm 备份健康巡检（纯 OSS 模式）
# 本地稳态为空、OSS 为唯一主存储，因此巡检核心是「OSS 上今天有没有新备份」。
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

say "========== 备份健康巡检（纯 OSS）=========="

# --- 1. 磁盘空间 ---
AVAIL_GB=$(df -BG --output=avail /backup | tail -1 | tr -dc '0-9')
USE_PCT=$(df --output=pcent /backup | tail -1 | tr -dc '0-9')
if [ "${AVAIL_GB:-0}" -lt 10 ]; then
  bad "磁盘空闲仅 ${AVAIL_GB}G（已用 ${USE_PCT}%），低于 10G 阈值"
else
  ok "磁盘空闲 ${AVAIL_GB}G（已用 ${USE_PCT}%）"
fi

# --- 2. OSS 今日数据库备份（核心：上传链路是否在工作）---
# 数据库每 6 小时一次，一天应有 4 份。至少 1 份视为链路正常。
TODAY=$(date +%Y%m%d)
OSS_DB_TODAY=$(ossutil ls "oss://${BUCKET}/newpm-mysql/newpm-${TODAY}" 2>/dev/null | grep -c 'sql.gz' || true)
if [ "${OSS_DB_TODAY:-0}" -lt 1 ]; then
  bad "OSS 上今日($TODAY)无数据库备份——定时任务或上传链路可能已断"
else
  ok "OSS 今日数据库备份 ${OSS_DB_TODAY} 份"
fi

# --- 3. OSS 今日附件备份 ---
# 前缀 newpm-upload/ 一并覆盖历史遗留的 daily/、monthly/ 子目录，用 --recursive 全查。
OSS_UP_TODAY=$(ossutil ls "oss://${BUCKET}/newpm-upload/" 2>/dev/null | grep "newpm-upload-${TODAY}" | grep -c 'tar.gz' || true)
if [ "${OSS_UP_TODAY:-0}" -lt 1 ]; then
  bad "OSS 上今日($TODAY)无附件备份——定时任务或上传链路可能已断"
else
  ok "OSS 今日附件备份 ${OSS_UP_TODAY} 份"
fi

# --- 4. 本地残留检查（纯 OSS 模式本地应为空；有残留=某次上传失败的兜底，需关注）---
LOCAL_DB=$(ls -1 "$DB_DIR"/newpm-*.sql.gz 2>/dev/null | wc -l | tr -d ' ')
LOCAL_UP=$(ls -1 "$UP_DIR"/newpm-upload-*.tar.gz 2>/dev/null | wc -l | tr -d ' ')
if [ "$LOCAL_DB" -gt 0 ] || [ "$LOCAL_UP" -gt 0 ]; then
  bad "本地有残留（数据库 ${LOCAL_DB} 份 / 附件 ${LOCAL_UP} 份）——纯 OSS 模式本地应为空，可能有上传失败未清，请查 oss-sync.log"
else
  ok "本地无残留（纯 OSS 模式正常）"
fi

# --- 5. 近期上传日志中的错误 ---
RECENT_ERR=$(tail -200 /backup/oss-sync.log 2>/dev/null | grep -c 'ERROR' || true)
if [ "${RECENT_ERR:-0}" -gt 0 ]; then
  bad "oss-sync.log 近 200 行中有 ${RECENT_ERR} 条 ERROR"
else
  ok "上传日志无错误"
fi

say "========== 巡检结束：问题数 ${ISSUES} =========="
exit "$ISSUES"
