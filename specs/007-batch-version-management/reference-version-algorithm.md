# 出入库版本号生成算法（从 yadapm 源码精确提取）

> 源码：`yadapm/src/main/scala/com/yada/mag/service/StorageService.scala` `getOutLibVersion()` (253–304) + `getVersionCodeByOutVersion()` (314–333)
> 用途：本功能的命门算法。`/speckit-plan` 据此设计 service，并以特征测试逐类型锁定。

## 输入

- `sysName` 子系统名称 → 查出 `baseVersioncode`（基准版本号）、`product`（产品）
- `subVersionCode` 子产品ID
- `versionTypeId` 版本类型（1–6）
- `outVersion` 升级包初级版本号（仅类型 5/6 用）
- `addFlag` 1=新增页 2=编辑页
- `id` / `oldSubVersionCode` / `oldVersionType` 编辑时用于判断关键字段是否变更

## 核心查询

- `versioncode = getVersionCode(sysName, versionTypeId)`：该子系统+版本类型下当前**最大版本编号**（数值）。
- 返回值格式：`"{出入库版本号},{versionCode}"` —— 版本号 + 版本编号一起返回，versionCode 另存字段。

## 各版本类型规则

| 类型 | 名称 | 版本号格式 | 序号规则 |
|---|---|---|---|
| 1 | SP升级包 | `{base}_SP{NN}` | `NN = (最大版本编号+1)`，2 位补零 |
| 2 | PTF补丁包 | `{base}_PTF{NN}` | 同上 |
| 3 | B测试包 | `{base}_B{NN}` | 同上 |
| 4 | 临时版本包 | `T_{当前年份}_{NNN}_{product}` | `NNN = (getVersionCodeByYear(subVersionCode, versionType)+1)`，3 位补零，按**子产品+年份**维度 |
| 5 | B包升级包 | `{base}_B{major}.{minor}` | 见下 `getVersionCodeByOutVersion` |
| 6 | SP包升级包 | `{base}_SP{major}.{minor}` | 见下，逻辑同 5 |

### 类型 4 编辑特例（addFlag=2）

- 若 `subVersionCode` 或 `versionType` **未变**：沿用原版本编号（`getOldVersincode(id)`）。
- 若**任一变更**：按新增规则重新计算 `NNN`。

### 类型 5/6：`getVersionCodeByOutVersion(sysName, versionType, outVersion, level)`

1. `versionCode = getCode(sysName, versionType, outVersion)`：取该升级包初级版本号下的最大版本编号。
2. 若为空 → 回退查"初级版本号自身"的版本编号 `getVersionCodeByVersion`：
   - 类型 5（B包升级包）回退查 **类型 3（B测试包）**
   - 类型 6（SP包升级包）回退查 **类型 1（SP升级包）**
3. 按 `.` 切分：
   - 只有 1 段（首个升级包）→ `{major:%02d}.01`
   - 有 2 段（已存在升级包）→ `{major}.{(minor+1):%02d}`
4. 注释明确："解决 .9 升级进位错误问题" —— minor 从 09→10 时按数值进位，不是字符串拼接。

## 迁移到 newpm 的注意点

- 老系统 `getVersionCode` 返回字符串后 `.toInt`，空值/非数字需防御（首条记录 versioncode 可能为空，应视作 0）。
- `%02d` / `%03d` 补零规则必须严格保留。
- 类型 5/6 的"回退查基线版本类型"映射（5→3、6→1）是隐藏依赖，迁移时容易漏。
- 并发新增同子系统+类型时，"取最大编号+1"存在竞态 → newpm 实现需保证唯一性（唯一约束 + 重试，或串行化生成）。
- `getCurYear`、`product`、`baseVersioncode` 的来源需在 newpm 映射到对应字典/配置/任务实体。
