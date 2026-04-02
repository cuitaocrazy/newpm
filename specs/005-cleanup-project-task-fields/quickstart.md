# Quickstart: 清理 pm_project 废弃任务字段

## 概述

本次变更仅涉及 **1 个文件**的编辑：`docs/gen-specs/pm_project.yml`。

Java 代码、Mapper XML、DDL 均已在此前迁移中清理完毕，无需修改。

## 操作步骤

1. 打开 `docs/gen-specs/pm_project.yml`
2. 删除所有标记为 `# DEPRECATED: migrated to pm_task` 的字段块（共 21 个）
3. 删除分隔注释行（`# ========== 任务相关字段...`）
4. 验证 yml 格式正确性（缩进、结构）
5. 确认 `batchNo` 和 `planProductionDate` 保留未动

## 验证

```bash
# 确认无 DEPRECATED 残留
grep -c "DEPRECATED" docs/gen-specs/pm_project.yml
# 期望输出: 0

# 确认 yml 格式可解析
python3 -c "import yaml; yaml.safe_load(open('docs/gen-specs/pm_project.yml'))"

# 确认编译无影响（理论上不会有，因为 spec 文件不参与编译）
mvn clean compile -pl ruoyi-project -am -Dmaven.test.skip=true
```

## 风险

**极低** — spec 文件是文档性质，不参与编译和运行时。唯一风险是误删活跃字段定义，通过 `batchNo`/`planProductionDate` 的保留检查即可规避。
