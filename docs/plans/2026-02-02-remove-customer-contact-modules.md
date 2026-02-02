# 删除客户信息管理和联系人信息管理功能实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 完全删除客户信息管理（Customer）和联系人信息管理（CustomerContact）的所有功能模块

**Architecture:** 这是一个删除任务，需要清理后端Java代码、前端Vue页面、数据库表定义、API接口和相关配置。按照从前端到后端、从代码到数据库的顺序进行删除，确保不留残余。

**Tech Stack:**
- Backend: Spring Boot 3.5.8, MyBatis
- Frontend: Vue 3.5, TypeScript 5.6, Element Plus
- Database: MySQL 8.x

---

## Task 1: 删除前端客户管理页面

**Files:**
- Delete: `ruoyi-ui/src/views/project/customer/index.vue`
- Delete: `ruoyi-ui/src/api/project/customer.js`

**Step 1: 删除客户管理页面**

```bash
rm -f ruoyi-ui/src/views/project/customer/index.vue
```

**Step 2: 删除客户管理API文件**

```bash
rm -f ruoyi-ui/src/api/project/customer.js
```

**Step 3: 删除客户管理目录（如果为空）**

```bash
rmdir ruoyi-ui/src/views/project/customer 2>/dev/null || true
```

**Step 4: 验证文件已删除**

Run: `ls ruoyi-ui/src/views/project/customer 2>&1`
Expected: "No such file or directory"

**Step 5: 提交更改**

```bash
git add -A
git commit -m "feat: 删除前端客户管理页面和API

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 2: 删除前端联系人管理页面

**Files:**
- Delete: `ruoyi-ui/src/views/project/contact/index.vue`
- Delete: `ruoyi-ui/src/api/project/contact.js`

**Step 1: 删除联系人管理页面**

```bash
rm -f ruoyi-ui/src/views/project/contact/index.vue
```

**Step 2: 删除联系人管理API文件**

```bash
rm -f ruoyi-ui/src/api/project/contact.js
```

**Step 3: 删除联系人管理目录（如果为空）**

```bash
rmdir ruoyi-ui/src/views/project/contact 2>/dev/null || true
```

**Step 4: 验证文件已删除**

Run: `ls ruoyi-ui/src/views/project/contact 2>&1`
Expected: "No such file or directory"

**Step 5: 提交更改**

```bash
git add -A
git commit -m "feat: 删除前端联系人管理页面和API

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 3: 删除后端客户管理Controller

**Files:**
- Delete: `ruoyi-project/src/main/java/com/ruoyi/project/controller/CustomerController.java`

**Step 1: 删除CustomerController**

```bash
rm -f ruoyi-project/src/main/java/com/ruoyi/project/controller/CustomerController.java
```

**Step 2: 验证文件已删除**

Run: `ls ruoyi-project/src/main/java/com/ruoyi/project/controller/CustomerController.java 2>&1`
Expected: "No such file or directory"

**Step 3: 提交更改**

```bash
git add -A
git commit -m "feat: 删除后端CustomerController

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 4: 删除后端联系人管理Controller

**Files:**
- Delete: `ruoyi-project/src/main/java/com/ruoyi/project/controller/CustomerContactController.java`

**Step 1: 删除CustomerContactController**

```bash
rm -f ruoyi-project/src/main/java/com/ruoyi/project/controller/CustomerContactController.java
```

**Step 2: 验证文件已删除**

Run: `ls ruoyi-project/src/main/java/com/ruoyi/project/controller/CustomerContactController.java 2>&1`
Expected: "No such file or directory"

**Step 3: 提交更改**

```bash
git add -A
git commit -m "feat: 删除后端CustomerContactController

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 5: 删除后端客户管理Service层

**Files:**
- Delete: `ruoyi-project/src/main/java/com/ruoyi/project/service/ICustomerService.java`
- Delete: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/CustomerServiceImpl.java`

**Step 1: 删除ICustomerService接口**

```bash
rm -f ruoyi-project/src/main/java/com/ruoyi/project/service/ICustomerService.java
```

**Step 2: 删除CustomerServiceImpl实现类**

```bash
rm -f ruoyi-project/src/main/java/com/ruoyi/project/service/impl/CustomerServiceImpl.java
```

**Step 3: 验证文件已删除**

Run: `ls ruoyi-project/src/main/java/com/ruoyi/project/service/*Customer* 2>&1`
Expected: "No such file or directory"

**Step 4: 提交更改**

```bash
git add -A
git commit -m "feat: 删除后端客户管理Service层

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 6: 删除后端联系人管理Service层

**Files:**
- Delete: `ruoyi-project/src/main/java/com/ruoyi/project/service/ICustomerContactService.java`
- Delete: `ruoyi-project/src/main/java/com/ruoyi/project/service/impl/CustomerContactServiceImpl.java`

**Step 1: 删除ICustomerContactService接口**

```bash
rm -f ruoyi-project/src/main/java/com/ruoyi/project/service/ICustomerContactService.java
```

**Step 2: 删除CustomerContactServiceImpl实现类**

```bash
rm -f ruoyi-project/src/main/java/com/ruoyi/project/service/impl/CustomerContactServiceImpl.java
```

**Step 3: 验证文件已删除**

Run: `ls ruoyi-project/src/main/java/com/ruoyi/project/service/*Contact* 2>&1`
Expected: "No such file or directory"

**Step 4: 提交更改**

```bash
git add -A
git commit -m "feat: 删除后端联系人管理Service层

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 7: 删除后端Mapper层

**Files:**
- Delete: `ruoyi-project/src/main/java/com/ruoyi/project/mapper/CustomerMapper.java`
- Delete: `ruoyi-project/src/main/java/com/ruoyi/project/mapper/CustomerContactMapper.java`
- Delete: `ruoyi-project/src/main/resources/mapper/project/CustomerMapper.xml`
- Delete: `ruoyi-project/src/main/resources/mapper/project/CustomerContactMapper.xml`

**Step 1: 删除CustomerMapper接口**

```bash
rm -f ruoyi-project/src/main/java/com/ruoyi/project/mapper/CustomerMapper.java
```

**Step 2: 删除CustomerContactMapper接口**

```bash
rm -f ruoyi-project/src/main/java/com/ruoyi/project/mapper/CustomerContactMapper.java
```

**Step 3: 删除CustomerMapper XML配置**

```bash
rm -f ruoyi-project/src/main/resources/mapper/project/CustomerMapper.xml
```

**Step 4: 删除CustomerContactMapper XML配置**

```bash
rm -f ruoyi-project/src/main/resources/mapper/project/CustomerContactMapper.xml
```

**Step 5: 验证文件已删除**

Run: `ls ruoyi-project/src/main/java/com/ruoyi/project/mapper/*Customer* 2>&1`
Expected: "No such file or directory"

**Step 6: 提交更改**

```bash
git add -A
git commit -m "feat: 删除后端Mapper层和MyBatis XML配置

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 8: 删除后端Domain实体类

**Files:**
- Delete: `ruoyi-project/src/main/java/com/ruoyi/project/domain/Customer.java`
- Delete: `ruoyi-project/src/main/java/com/ruoyi/project/domain/CustomerContact.java`

**Step 1: 删除Customer实体类**

```bash
rm -f ruoyi-project/src/main/java/com/ruoyi/project/domain/Customer.java
```

**Step 2: 删除CustomerContact实体类**

```bash
rm -f ruoyi-project/src/main/java/com/ruoyi/project/domain/CustomerContact.java
```

**Step 3: 验证文件已删除**

Run: `ls ruoyi-project/src/main/java/com/ruoyi/project/domain/Customer*.java 2>&1`
Expected: "No such file or directory"

**Step 4: 提交更改**

```bash
git add -A
git commit -m "feat: 删除后端Domain实体类

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 9: 删除数据库表定义

**Files:**
- Modify: `pm-sql/init/00_tables_ddl.sql` (删除 pm_customer 和 pm_customer_contact 表的DDL)

**Step 1: 读取当前DDL文件找到表定义位置**

Run: `grep -n "pm_customer" pm-sql/init/00_tables_ddl.sql`
Expected: 显示行号479-502（pm_customer）和503-526（pm_customer_contact）

**Step 2: 删除pm_customer表的DDL（包括DROP和CREATE语句）**

需要删除从 "DROP TABLE IF EXISTS \`pm_customer\`;" 到下一个 "DROP TABLE" 之前的所有内容。

**Step 3: 删除pm_customer_contact表的DDL（包括DROP和CREATE语句）**

需要删除从 "DROP TABLE IF EXISTS \`pm_customer_contact\`;" 到下一个 "DROP TABLE" 之前的所有内容。

**Step 4: 验证修改**

Run: `grep -c "pm_customer" pm-sql/init/00_tables_ddl.sql`
Expected: 0

**Step 5: 提交更改**

```bash
git add pm-sql/init/00_tables_ddl.sql
git commit -m "feat: 删除客户和联系人表的DDL定义

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 10: 删除数据库初始化数据中的相关字典和菜单

**Files:**
- Modify: `pm-sql/init/01_tables_data.sql` (删除客户和联系人相关的字典数据和菜单数据)

**Step 1: 查找相关字典数据**

Run: `grep -n "客户\|联系人\|industry\|contact_tag" pm-sql/init/01_tables_data.sql | head -20`
Expected: 显示相关字典类型和字典数据的行号

**Step 2: 删除字典类型数据**

需要删除以下字典类型：
- `industry` (所属行业)
- `contact_tag` (联系人标签)

在 sys_dict_type 表的 INSERT 语句中删除这两行。

**Step 3: 删除字典数据**

在 sys_dict_data 表的 INSERT 语句中删除所有 dict_type 为 'industry' 和 'contact_tag' 的数据行。

**Step 4: 查找并删除菜单数据**

Run: `grep -n "客户管理\|联系人管理" pm-sql/init/01_tables_data.sql`
Expected: 显示菜单相关的行号

删除 sys_menu 表中所有与客户管理和联系人管理相关的菜单项。

**Step 5: 验证修改**

Run: `grep -c "客户\|联系人" pm-sql/init/01_tables_data.sql`
Expected: 应该只剩下少量不相关的匹配（如"市场经理负责客户关系"等描述性文字）

**Step 6: 提交更改**

```bash
git add pm-sql/init/01_tables_data.sql
git commit -m "feat: 删除客户和联系人相关的字典和菜单数据

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Task 11: 清理编译产物

**Files:**
- Delete: `ruoyi-project/target/classes/mapper/project/CustomerMapper.xml`
- Delete: `ruoyi-project/target/classes/mapper/project/CustomerContactMapper.xml`
- Delete: `ruoyi-project/target/classes/com/ruoyi/project/controller/CustomerController.class`
- Delete: `ruoyi-project/target/classes/com/ruoyi/project/controller/CustomerContactController.class`
- And other compiled classes

**Step 1: 清理Maven编译产物**

```bash
cd ruoyi-project && mvn clean
```

**Step 2: 验证target目录已清理**

Run: `ls ruoyi-project/target 2>&1`
Expected: "No such file or directory" 或目录为空

**Step 3: 提交更改（如果有.gitignore之外的文件被删除）**

```bash
git add -A
git commit -m "chore: 清理编译产物

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>" || echo "No changes to commit"
```

---

## Task 12: 验证删除完整性

**Step 1: 搜索代码中是否还有Customer相关引用**

Run: `grep -r "CustomerController\|CustomerService\|CustomerMapper\|Customer\.java" ruoyi-project/src --include="*.java" | grep -v "Binary file"`
Expected: 无输出或只有注释中的引用

**Step 2: 搜索代码中是否还有CustomerContact相关引用**

Run: `grep -r "CustomerContactController\|CustomerContactService\|CustomerContactMapper\|CustomerContact\.java" ruoyi-project/src --include="*.java" | grep -v "Binary file"`
Expected: 无输出或只有注释中的引用

**Step 3: 搜索前端代码中的引用**

Run: `grep -r "customer\|contact" ruoyi-ui/src/views/project --include="*.vue" --include="*.js" --include="*.ts"`
Expected: 无输出或只有其他模块（如project）中的无关引用

**Step 4: 搜索API引用**

Run: `grep -r "/project/customer\|/project/contact" ruoyi-ui/src --include="*.js" --include="*.ts" --include="*.vue"`
Expected: 无输出

**Step 5: 验证数据库脚本**

Run: `grep -i "pm_customer" pm-sql/init/*.sql`
Expected: 无输出

**Step 6: 记录验证结果**

如果所有验证都通过，说明删除完整。如果发现遗漏，需要补充删除。

---

## 完成标准

1. ✅ 所有前端页面和API文件已删除
2. ✅ 所有后端Controller、Service、Mapper、Domain类已删除
3. ✅ 所有MyBatis XML配置文件已删除
4. ✅ 数据库DDL中的表定义已删除
5. ✅ 数据库初始化数据中的相关字典和菜单已删除
6. ✅ 编译产物已清理
7. ✅ 代码搜索验证无残留引用
8. ✅ 所有更改已提交到Git

---

## 注意事项

1. **不要删除项目管理（Project）模块**：虽然Project模块可能引用了Customer，但我们只删除Customer和CustomerContact模块本身
2. **保留字典类型 sys_yjqy**：这个字典（区域分类）可能被其他模块使用，不要删除
3. **检查依赖关系**：如果Project模块中有外键引用Customer，需要先处理这些引用
4. **备份数据库**：在执行数据库脚本前建议备份现有数据

---

## 执行方式选择

**1. Subagent-Driven (当前会话)** - 我在当前会话中逐任务派发子代理执行，任务间进行代码审查，快速迭代

**2. Parallel Session (独立会话)** - 在新会话中使用 executing-plans skill，批量执行并设置检查点
