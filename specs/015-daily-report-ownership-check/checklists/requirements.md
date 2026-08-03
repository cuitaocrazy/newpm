# Specification Quality Checklist: 日报保存的工时保护与项目归属校验

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-07-29
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

### 验证第 2 轮（2026-07-29）— 全部通过

**第 1 轮遗留的 [NEEDS CLARIFICATION] 已消解，但不是靠"回答"，是靠"发现问题问错了"：**

第 1 轮曾就「已结项项目的历史工时能否被删除」向业务方提问。业务方澄清：填写页不展示已结项项目，
填报人**看不到**该项目，故"主动删除"场景不存在。

但顺此线索核查发现了**方向相反的真问题**：正因为看不到，那条工时会在填报人保存其他项目时
**被静默删除**——已端到端复现，并依据操作日志重放取证到历史确证丢失 12 组 / 70 小时 / 8.75 人天。

据此重写 spec，主要变化：

1. 新增 **User Story 1「保存日报不得丢失填报人看不见的工时」** 为 P1（与原 US1 并列最高优先级）
2. 原 US1（阻止填到无关项目）降为 User Story 2，仍为 P1
3. 原 US2 验收场景 4 的 [NEEDS CLARIFICATION] 整条删除——该场景不存在
4. 需求拆为「工时保护（FR-001~005）」与「归属校验（FR-006~012）」两组，防止两类规则相互抵消
5. 成功标准新增 SC-001（现存 133 条日报的工时保留率 0% → 100%）与 SC-008（不得引入新的对账偏差）

**通过项说明：**

- 实现细节全部剔除：正文不出现表名／接口名／字段名／注解名；以「可见工时」「项目成员关系」
  等业务概念表述（技术细节留在 Issue #6 与 plan 阶段）
- 需求可测且无歧义：区分「可见工时未提交＝删除」与「不可见工时未提交＝保留」两种语义，
  避免"到底该不该删"的解读分歧
- 成功标准可验证且与技术无关：用「133 条日报保留率」「100 次尝试成功 0 次」等业务口径
- 边界情形与统计数字全部来自生产实测与操作日志取证，非假想场景
- 范围边界明确：以 Assumptions 显式排除历史丢失数据的补录、指向已删项目的脏数据、
  团队日报数据权限口径、填写页展示规则改动
