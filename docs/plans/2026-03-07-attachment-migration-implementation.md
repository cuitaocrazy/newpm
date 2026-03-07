# 附件存储迁移实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复附件上传/下载逻辑，统一路径格式，并将旧系统 163 条附件文件迁移到新目录结构。

**Architecture:** 一次性 Python 迁移脚本处理旧数据（文件移动 + DB 更新），同时修改 AttachmentServiceImpl 的上传逻辑（磁盘用 UUID）和下载逻辑（路径规范化兼容旧数据）。

**Tech Stack:** Python 3 (mysql-connector-python)、Java 17、Spring Boot 3、Docker MySQL

---

## 关键路径说明

- **旧文件来源：** `/Users/kongli/Documents/companyProjectDoc/ConfigDoc/ConfigItemDocuments/PM/newPM资料/252服务器上的附件/ruoyi/uploadPath/upload/{type}/{id}/{uuid}.ext`
- **新文件目标：** `~/ruoyi/uploadPath/合同/{id}_{name}/{yyyyMMdd}/{uuid}.ext`
- **本地数据库：** Docker 容器 `3523a41063b7`，库 `ry-vue`，密码 `password`
- **AttachmentServiceImpl：** `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/AttachmentServiceImpl.java`

---

## Task 1: 备份数据库附件表

**Files:**
- 无需改动代码

**Step 1: 执行备份**

```bash
echo "CREATE TABLE pm_attachment_backup_20260307 AS SELECT * FROM pm_attachment;" | \
  docker exec -i 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

**Step 2: 验证备份**

```bash
echo "SELECT COUNT(*) as total FROM pm_attachment_backup_20260307;" | \
  docker exec -i 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

预期输出：`total = 163`（或当前总数）

---

## Task 2: 创建 Python 迁移脚本

**Files:**
- Create: `pm-sql/migrate_attachments.py`

**Step 1: 确认 mysql-connector-python 已安装**

```bash
python3 -c "import mysql.connector; print('OK')"
```

如未安装：`pip3 install mysql-connector-python`

**Step 2: 创建脚本 `pm-sql/migrate_attachments.py`**

```python
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
    # 统一替换 \ 为 /，去掉前缀
    normalized = file_path.replace('\\', '/').lstrip('/')
    # 去掉 profile/ 前缀
    normalized = re.sub(r'^profile/', '', normalized)
    # 取最后一段（文件名）
    parts = normalized.split('/')
    return parts[-1] if parts else None

def extract_old_type_and_id(file_path: str):
    """从旧 file_path 提取 type 和 id，用于定位旧物理文件"""
    normalized = file_path.replace('\\', '/').lstrip('/')
    normalized = re.sub(r'^profile/', '', normalized)
    # 格式：upload/{type}/{id}/{uuid}.ext
    parts = normalized.split('/')
    if len(parts) >= 4 and parts[0] == 'upload':
        return parts[1], parts[2]   # type, id
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

    # 查询所有未删除附件，JOIN 业务表取名称
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
```

**Step 3: 赋予执行权限**

```bash
chmod +x /Users/kongli/ws-claude/PM/newpm/pm-sql/migrate_attachments.py
```

---

## Task 3: 运行迁移脚本（预演）

**Step 1: 确认目标目录存在**

```bash
mkdir -p ~/ruoyi/uploadPath
ls ~/ruoyi/uploadPath
```

**Step 2: 运行脚本**

```bash
cd /Users/kongli/ws-claude/PM/newpm
python3 pm-sql/migrate_attachments.py
```

**预期输出：**
```
  ✅ [1] 复制: 合同/5_福建建行.../20260202/bef932....pdf
  ✅ [2] 复制: 合同/11_IT技术.../20260203/a3873b....pdf
  ...
迁移完成：
  ✅ 成功（文件+DB）：163 条
  ⚠️  文件不存在（仅更新DB）：0 条
  ❌ 失败：0 条
```

**Step 3: 验证数据库已更新**

```bash
echo "SELECT attachment_id, file_path FROM pm_attachment WHERE del_flag='0' LIMIT 5;" | \
  docker exec -i 3523a41063b7 mysql -u root -ppassword --default-character-set=utf8mb4 ry-vue
```

预期：`file_path` 不再含 `profile/` 前缀或 `\`，格式为 `合同/17_名称/20260202/uuid.pdf`

**Step 4: 验证文件已复制**

```bash
ls ~/ruoyi/uploadPath/合同/ | head -5
ls ~/ruoyi/uploadPath/项目/ | head -5
ls ~/ruoyi/uploadPath/款项/ | head -5
```

---

## Task 4: 修复下载逻辑（兼容旧格式路径）

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/AttachmentServiceImpl.java:198-199`

**Step 1: 找到下载方法中拼接路径的代码（第 198-199 行）**

当前代码：
```java
String basePath = RuoYiConfig.getProfile();
String filePath = basePath + File.separator + attachment.getFilePath();
```

**Step 2: 替换为规范化路径拼接**

```java
String basePath = RuoYiConfig.getProfile();
String normalizedPath = attachment.getFilePath()
        .replaceAll("^/?profile/", "")   // 去掉 profile/ 前缀（兼容旧数据）
        .replace("\\", "/");             // \ 换成 /（兼容 Windows 遗留）
String filePath = basePath + File.separator + normalizedPath;
```

**Step 3: 确认文件改动无其他影响**

检查同文件内其他引用 `getFilePath()` 的位置：

```bash
grep -n "getFilePath\|setFilePath" ruoyi-project/src/main/java/com/ruoyi/project/service/impl/AttachmentServiceImpl.java
```

---

## Task 5: 修改上传逻辑（磁盘存 UUID）

**Files:**
- Modify: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/AttachmentServiceImpl.java:112-152`

**Step 1: 在文件顶部找到 import 区域，确认 UUID 已导入**

```bash
grep -n "import java.util.UUID\|import java.util" ruoyi-project/src/main/java/com/ruoyi/project/service/impl/AttachmentServiceImpl.java
```

如无 UUID import，在第 8 行附近添加：
```java
import java.util.UUID;
```

**Step 2: 修改上传路径构建逻辑（第 112-135 行附近）**

当前代码：
```java
String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
// ... 校验 ...
String basePath = RuoYiConfig.getProfile();
String dateFolder = new SimpleDateFormat("yyyyMMdd").format(new Date());
String businessFolder = getBusinessFolder(businessType, businessId);
// ...
String relativePath = businessFolder + File.separator + dateFolder + File.separator + fileName;
String filePath = basePath + File.separator + relativePath;
```

改为（只改 relativePath 和 filePath 两行）：
```java
String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
// ... 校验不变 ...
String basePath = RuoYiConfig.getProfile();
String dateFolder = new SimpleDateFormat("yyyyMMdd").format(new Date());
String businessFolder = getBusinessFolder(businessType, businessId);
// ...
// 磁盘文件名用 UUID，避免同名覆盖和特殊字符问题
String uuid = UUID.randomUUID().toString().replace("-", "");
String storedFileName = uuid + extension;
String relativePath = businessFolder + "/" + dateFolder + "/" + storedFileName;
String filePath = basePath + File.separator + relativePath;
```

**Step 3: 确认 setFileName 仍用原始文件名（第 150 行）**

```java
attachment.setFileName(fileName);       // ← 原始文件名，供下载 Content-Disposition 用，保持不变
attachment.setFilePath(relativePath);   // ← UUID 路径
```

确认这两行未被改动，`fileName` 依然是原始文件名。

---

## Task 6: 本地启动后端验证

**Step 1: 编译项目**

```bash
cd /Users/kongli/ws-claude/PM/newpm
mvn clean compile -pl ruoyi-project -am -q
```

预期：`BUILD SUCCESS`，无编译错误

**Step 2: 启动后端**

```bash
./ry.sh start
# 或
java -Xms512m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar
```

等待日志出现 `Started Application`

**Step 3: 测试下载旧附件**

浏览器打开 `http://localhost`，登录后进入合同管理页，找一条有附件的合同，点击下载。

预期：
- 文件正常下载
- 文件名是原始中文文件名（如 `福建建行...协议.pdf`）
- 不出现"文件不存在"错误

**Step 4: 测试上传新附件**

在任意合同/项目/款项下上传一个文件，然后：

```bash
ls ~/ruoyi/uploadPath/合同/ | tail -3
```

预期看到新目录，进入查看文件名为 UUID 格式（32位十六进制 + 扩展名）

**Step 5: 测试下载新上传附件**

下载刚上传的文件，确认文件名是原始文件名（不是 UUID）

---

## Task 7: 提交代码

**Step 1: 确认改动文件**

```bash
git diff --name-only
```

预期只有：
- `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/AttachmentServiceImpl.java`
- `pm-sql/migrate_attachments.py`（新增）
- `docs/plans/` 下的设计文档（新增）

**Step 2: 提交**

```bash
git add ruoyi-project/src/main/java/com/ruoyi/project/service/impl/AttachmentServiceImpl.java
git add pm-sql/migrate_attachments.py
git add docs/plans/2026-03-07-attachment-migration-design.md
git add docs/plans/2026-03-07-attachment-migration-implementation.md
git commit -m "$(cat <<'EOF'
feat: 统一附件存储路径格式，修复下载逻辑，新增迁移脚本

- 上传：磁盘改用 UUID 文件名，避免同名覆盖，file_name 字段保留原始名
- 下载：规范化 file_path，兼容旧数据的 profile/ 前缀和 \ 分隔符
- 新增 pm-sql/migrate_attachments.py：迁移旧附件到新目录结构并更新 DB

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## 注意事项

1. **迁移脚本只复制文件，不删除旧文件**，可随时回滚
2. 回滚方法：`RENAME TABLE pm_attachment_backup_20260307 TO pm_attachment_restore;` 然后恢复字段
3. 项目/款项类型的附件文件在旧目录中也存在，脚本会一并处理
4. 生产环境（k3s001）执行迁移时，旧文件来源改为 Pod 内 `/app/uploadPath/upload/`
