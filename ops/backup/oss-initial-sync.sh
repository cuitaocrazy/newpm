#!/bin/bash
# 一次性补传存量备份到 OSS。可重复执行（ossutil 覆盖同名对象），失败项标 FAILED。
# 注意：不加 set -e，单个文件失败不中断整批。
export PATH=/usr/local/bin:/usr/bin:/bin
SYNC=/usr/local/bin/sync-backup-to-oss.sh
OK=0
FAIL=0

echo "===== 存量补传开始 $(date '+%F %T') ====="

echo "--- [1/2] 数据库备份 -> newpm-mysql/ ---"
for f in /backup/newpm-mysql/*.sql.gz; do
  [ -e "$f" ] || continue
  if "$SYNC" "$f" "newpm-mysql"; then
    OK=$((OK+1)); echo "  ok   $(basename "$f")"
  else
    FAIL=$((FAIL+1)); echo "  FAILED $f"
  fi
done

# 存量附件统一进 monthly/（180天保留），不进 60 天的 daily/：
# 这 6 份是目前唯一的历史副本，且生命周期按上传时间起算。
echo "--- [2/2] 附件备份 -> newpm-upload/monthly/ ---"
for f in /backup/newpm-upload/*.tar.gz; do
  [ -e "$f" ] || continue
  if "$SYNC" "$f" "newpm-upload/monthly"; then
    OK=$((OK+1)); echo "  ok   $(basename "$f")"
  else
    FAIL=$((FAIL+1)); echo "  FAILED $f"
  fi
done

echo "===== 补传结束 成功=$OK 失败=$FAIL $(date '+%F %T') ====="
[ "$FAIL" -eq 0 ]
