#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
附件迁移脚本：将旧格式 file_path 更新为新格式，并移动物理文件
旧格式：profile/upload/contract\{id}/{uuid}.ext
新格式：合同/{id}_{name}/{yyyyMMdd}/{uuid}.ext
"""

import os
import re
import shutil
import mysql.connector
from pathlib import Path

# ===== 配置 =====
DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 3306,
    'user': 'root',
    'password': 'password',
    'database': 'ry-vue',
    'charset': 'utf8mb4',
}

# 旧文件根目录（252服务器附件已下载到本地）
OLD_BASE = Path('/Users/kongli/Documents/companyProjectDoc/ConfigDoc/ConfigItemDocuments/PM/newPM资料/252服务器上的附件/ruoyi/uploadPath/upload')

# 新文件根目录（本地开发 profile 路径）
NEW_BASE = Path.home() / 'ruoyi' / 'uploadPath'

# 业务类型 → 中文目录名
TYPE_CN = {
    'contract': '合同',
    'project':  '项目',
    'payment':  '款项',
}

# 文件名中不允许的字符（替换为 _）
ILLEGAL_CHARS = re.compile(r'[/\\:*?"<>|]')

# ===== 工具函数 =====

def sanitize(name: str) -> str:
    """清理业务名称中的非法字符"""
    return ILLEGAL_CHARS.sub('_', name).strip()

def extract_uuid(file_path: str) -> str | None:
    """从旧 file_path 提取 UUID 文件名，例如 0d7c...pdf"""
    normalized = file_path.replace('\\', '/').lstrip('/')
    normalized = re.sub(r'^profile/', '', normalized)
    parts = normalized.split('/')
    return parts[-1] if parts else None

def extract_old_type_and_id(file_path: str):
    """从旧 file_path 提取 type 和 id，用于定位旧物理文件"""
    normalized = file_path.replace('\\', '/').lstrip('/')
    normalized = re.sub(r'^profile/', '', normalized)
    parts = normalized.split('/')
    if len(parts) >= 4 and parts[0] == 'upload':
        return parts[1], parts[2]
    return None, None

def build_new_path(type_cn: str, business_id: int, business_name: str,
                   date_str: str, uuid_filename: str) -> str:
    """构建新相对路径"""
    safe_name = sanitize(business_name)
    folder = f"{type_cn}/{business_id}_{safe_name}/{date_str}"
    return f"{folder}/{uuid_filename}"

# ===== 主逻辑 =====

def migrate():
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor(dictionary=True)

    cursor.execute("""
        SELECT
            a.attachment_id,
            a.business_type,
            a.business_id,
            a.file_name,
            a.file_path,
            DATE_FORMAT(a.create_time, '%Y%m%d') AS date_folder,
            CASE
                WHEN a.business_type = 'contract' THEN c.contract_name
                WHEN a.business_type = 'project'  THEN p.project_name
                WHEN a.business_type = 'payment'  THEN pm.payment_method_name
            END AS business_name
        FROM pm_attachment a
        LEFT JOIN pm_contract c  ON a.business_type = 'contract' AND a.business_id = c.contract_id
        LEFT JOIN pm_project  p  ON a.business_type = 'project'  AND a.business_id = p.project_id
        LEFT JOIN pm_payment  pm ON a.business_type = 'payment'  AND a.business_id = pm.payment_id
        WHERE a.del_flag = '0'
        ORDER BY a.attachment_id
    """)
    rows = cursor.fetchall()

    success, skipped, failed = 0, 0, 0

    for row in rows:
        aid          = row['attachment_id']
        btype        = row['business_type']
        bid          = row['business_id']
        bname        = row['business_name'] or f"ID{bid}"
        old_path_str = row['file_path']
        date_folder  = row['date_folder'] or '20260101'
        type_cn      = TYPE_CN.get(btype, btype)

        # 0. 跳过已经是新格式的记录（以 合同/ 项目/ 款项/ 开头）
        if old_path_str.startswith(('合同/', '项目/', '款项/')):
            print(f"  ⏭️  [{aid}] 已是新格式，跳过: {old_path_str}")
            continue

        # 1. 提取 UUID 文件名
        uuid_filename = extract_uuid(old_path_str)
        if not uuid_filename:
            print(f"  ❌ [{aid}] 无法提取UUID: {old_path_str}")
            failed += 1
            continue

        # 2. 定位旧物理文件
        old_type, old_id = extract_old_type_and_id(old_path_str)
        if old_type and old_id:
            old_file = OLD_BASE / old_type / old_id / uuid_filename
        else:
            old_file = None

        # 3. 构建新路径
        new_rel_path = build_new_path(type_cn, bid, bname, date_folder, uuid_filename)
        new_file = NEW_BASE / new_rel_path

        # 4. 复制物理文件
        if old_file and old_file.exists():
            new_file.parent.mkdir(parents=True, exist_ok=True)
            if not new_file.exists():
                shutil.copy2(str(old_file), str(new_file))
                print(f"  ✅ [{aid}] 复制: {new_rel_path}")
            else:
                print(f"  ⏭️  [{aid}] 已存在，跳过复制: {new_rel_path}")
        else:
            print(f"  ⚠️  [{aid}] 旧文件不存在: {old_file}，仅更新DB路径")
            skipped += 1

        # 5. 更新数据库 file_path
        try:
            cursor.execute(
                "UPDATE pm_attachment SET file_path = %s WHERE attachment_id = %s",
                (new_rel_path, aid)
            )
            conn.commit()
            if old_file and old_file.exists():
                success += 1
        except Exception as e:
            conn.rollback()
            print(f"  ❌ [{aid}] DB更新失败: {e}")
            failed += 1

    cursor.close()
    conn.close()

    print("\n" + "="*50)
    print(f"迁移完成：")
    print(f"  ✅ 成功（文件+DB）：{success} 条")
    print(f"  ⚠️  文件不存在（仅更新DB）：{skipped} 条")
    print(f"  ❌ 失败：{failed} 条")

if __name__ == '__main__':
    migrate()
