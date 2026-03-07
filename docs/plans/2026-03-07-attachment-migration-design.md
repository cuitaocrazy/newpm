# 附件存储迁移设计方案

**日期：** 2026-03-07
**范围：** pm_attachment 表 + AttachmentServiceImpl + 迁移脚本

---

## 背景

旧系统（252服务器）附件数据库记录中 `file_path` 格式混乱：
- 含 `/profile/` 或 `profile/` 前缀
- 路径分隔符使用 `\`（Windows 遗留）
- 文件名为 UUID

导致当前程序下载逻辑拼接路径错误，附件无法下载。

---

## 目标格式

**磁盘存储路径（`file_path` 字段）：**

```
合同/{contractId}_{contractName}/{yyyyMMdd}/{uuid}.{ext}
项目/{projectId}_{projectName}/{yyyyMMdd}/{uuid}.{ext}
款项/{paymentId}_{paymentMethodName}/{yyyyMMdd}/{uuid}.{ext}
```

**示例：**
```
合同/17_合同名称/20260202/0d7ccbebcc874c7693d5a353015ecb27.pdf
项目/26_项目名称/20260203/49f696b60e494dbaa02d7d5a1a2f5b6a.pdf
款项/37_款项名称/20260201/8a22bafa284c45ada0a028fe6c3ccb75.docx
```

**设计原则：**
- 磁盘文件名用 **UUID**（避免覆盖、特殊字符、长度问题）
- `file_name` 字段保存**原始文件名**（下载时通过 `Content-Disposition` 返回给用户）
- 业务名称中 `/ : * ? " < > |` 等非法字符替换为 `_`
- 日期取 `pm_attachment.create_time`

---

## 一、迁移脚本（一次性执行）

### 脚本语言
Python 3

### 输入
- 旧文件目录：`/Users/kongli/Documents/companyProjectDoc/ConfigDoc/ConfigItemDocuments/PM/newPM资料/252服务器上的附件/ruoyi/uploadPath/upload/`
- 本地数据库：Docker 容器 `3523a41063b7`，库名 `ry-vue`

### 查询逻辑

```sql
SELECT
    a.attachment_id,
    a.business_type,
    a.business_id,
    a.file_name,
    a.file_path,
    a.file_type,
    a.create_time,
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
```

### 每条记录处理步骤

```
1. 从旧 file_path 提取 UUID
   /profile/upload/contract\17/0d7c...pdf → uuid = 0d7c...pdf

2. 清理 business_name（替换非法字符 / : * ? " < > | 为 _）

3. 生成中文类型目录前缀
   contract → 合同
   project  → 项目
   payment  → 款项

4. 构建新相对路径
   合同/17_合同名称/20260202/0d7c...pdf

5. 在本地 ~/ruoyi/uploadPath/ 下创建目录

6. 从旧目录查找物理文件并复制到新路径
   旧：.../upload/contract/17/0d7c...pdf
   新：~/ruoyi/uploadPath/合同/17_合同名称/20260202/0d7c...pdf

7. 更新数据库 file_path = 新相对路径

8. 记录处理结果（成功/跳过/失败）
```

### 安全原则
- **只复制，不删除旧文件**（可随时回滚）
- 同名文件已存在时跳过（不覆盖）
- 数据库更新失败时打印错误，继续处理下一条

### 输出报告
```
迁移完成：
  ✅ 成功：152 条
  ⚠️  跳过（文件不存在）：8 条
  ❌ 失败：0 条
```

---

## 二、代码改动

### 2.1 下载修复（`downloadAttachment`）

**问题：** 旧数据 `file_path` 含 `profile/` 前缀和 `\` 分隔符，直接拼接路径错误。

**修改：** 规范化 `file_path` 后再拼接。

```java
// 原来
String filePath = basePath + File.separator + attachment.getFilePath();

// 改为
String normalizedPath = attachment.getFilePath()
    .replaceAll("^/?profile/", "")  // 去掉 profile/ 前缀（兼容旧数据）
    .replace("\\", "/");            // \ 换成 /（兼容 Windows 遗留数据）
String filePath = basePath + "/" + normalizedPath;
```

### 2.2 上传改造（`uploadAttachment`）

**问题：** 当前用原始文件名存磁盘，存在同名覆盖风险。

**修改：** 磁盘存 UUID，`file_name` 字段保留原始文件名。

```java
// 原来：直接用原始文件名
String relativePath = businessFolder + "/" + dateFolder + "/" + fileName;

// 改为：磁盘用 UUID 命名
String uuid = UUID.randomUUID().toString().replace("-", "");
String storedFileName = uuid + extension;
String relativePath = businessFolder + "/" + dateFolder + "/" + storedFileName;

// file_name 保留原始名称（不变）
attachment.setFileName(fileName);       // 原始文件名，供下载 Content-Disposition 使用
attachment.setFilePath(relativePath);   // UUID 路径，供磁盘定位使用
```

---

## 三、执行步骤

### 本地测试

```
Step 1  运行迁移脚本
        旧文件 → ~/ruoyi/uploadPath/合同/.../
        数据库 file_path 全部更新为新格式

Step 2  修改代码（上传 + 下载）

Step 3  本地启动后端
        测试：下载旧附件 ✅
        测试：上传新附件，检查路径格式 ✅
        测试：下载新上传附件 ✅
```

### 生产部署（k3s001）

```
Step 4  在 k3s001 服务器执行迁移脚本
        旧文件来源：k3s001 服务器 Pod 内 /app/uploadPath/upload/

Step 5  代码随 CI/CD 正常部署（push main 自动触发）

Step 6  验证生产环境附件下载
```

---

## 四、回滚方案

- 迁移脚本只复制文件，旧文件原地保留
- 数据库 `file_path` 更新前建议备份：
  ```sql
  CREATE TABLE pm_attachment_backup AS SELECT * FROM pm_attachment;
  ```
- 回滚时从备份表恢复 `file_path`，删除新目录即可
