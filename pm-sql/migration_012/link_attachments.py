#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
关联问题单附件物理文件到 pm_attachment(让迁移的附件能下载)。
- 老附件在 F:\\批次任务问题单附件\\<问题单编号>\\<文件名> 和 F:\\问题单附件\\<...>
- 同事提供了 批次任务问题单附件.rar(已解压到 EXTRACT_DIR)
- 本脚本: 解析每条 pm_attachment 老 file_path → 在解压目录找物理文件 → 拷到 uploadPath
  → 回填 file_path(相对) + file_size。问题单附件(未提供)的记录跳过并报告。
用法: /tmp/etl-venv/bin/python pm-sql/migration_012/link_attachments.py [ry-vue]
"""
import os, re, shutil, sys
import pymysql

TARGET_DB = sys.argv[1] if len(sys.argv) > 1 else 'ry-vue'
EXTRACT_DIR = '/tmp/prolist-attach'                          # rar 解压根(下含 批次任务问题单附件/)
UPLOAD_DIR = os.path.expanduser('~/ruoyi/uploadPath')        # RuoYi profile 本地上传目录
REL_PREFIX = 'prolist-migrate'                               # 新相对路径前缀
HAVE_DIR = '批次任务问题单附件'                               # 已拿到备份的老目录

mysql = pymysql.connect(host='127.0.0.1', port=3306, user='root', password='password',
                        database=TARGET_DB, charset='utf8mb4', autocommit=False)
cur = mysql.cursor()
cur.execute("""SELECT attachment_id, business_id, file_name, file_path
               FROM pm_attachment WHERE business_type='prolist' AND create_by='yadapm-migrate'""")
recs = cur.fetchall()

def segs_of(fp):
    return re.sub(r'\\+', '\\\\', fp).replace('\\\\', '\\').split('\\')  # 归一反斜杠后切分

linked = 0
miss_file = []   # 属于已拿到目录但备份里没这个文件
pending = []     # 属于未提供的 问题单附件 目录
for aid, bid, fname, fpath in recs:
    parts = segs_of(fpath)                  # ['F:','<topdir>','<folder>','<file>']
    if len(parts) < 4:
        pending.append((aid, fpath)); continue
    topdir, folder, real_file = parts[1], parts[-2], parts[-1]
    if topdir != HAVE_DIR:
        pending.append((aid, fpath)); continue
    src = os.path.join(EXTRACT_DIR, HAVE_DIR, folder, real_file)
    if not os.path.exists(src):
        miss_file.append((aid, folder, real_file)); continue
    # 拷到 uploadPath/prolist-migrate/<aid>/<file>
    rel = f"{REL_PREFIX}/{aid}/{real_file}"
    dst = os.path.join(UPLOAD_DIR, REL_PREFIX, str(aid), real_file)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copy2(src, dst)
    size = os.path.getsize(dst)
    cur.execute("UPDATE pm_attachment SET file_path=%s, file_size=%s, remark='迁移附件-物理文件已关联' WHERE attachment_id=%s",
                (rel, size, aid))
    linked += 1

mysql.commit()
print(f"==== 附件关联结果(库={TARGET_DB}) ====")
print(f"总迁移附件记录: {len(recs)}")
print(f"✅ 已关联物理文件(可下载): {linked}")
print(f"⚠️ 已拿到目录但备份缺该文件: {len(miss_file)}")
for aid, folder, f in miss_file:
    print(f"    aid={aid} {folder}/{f}")
print(f"⏳ 待提供备份(F:\\问题单附件\\, 26条预期): {len(pending)}")
mysql.close()
print("done")
