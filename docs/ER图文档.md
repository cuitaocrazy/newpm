# 数据库 ER 图文档

> 数据库：`ry-vue` | MySQL 8.x | 字符集：`utf8mb4`
>
> 更新日期：2026-03-31（V1.1，基于 YML specs + 源码校准）

---

## 一、PM 业务模块 ER 图

### 1.1 核心业务关系总览

```mermaid
erDiagram
    pm_project ||--o{ pm_project_approval : "立项审核"
    pm_project ||--o{ pm_project_contract_rel : "关联合同"
    pm_project ||--o{ pm_task : "分解任务"
    pm_project ||--o{ pm_project_member : "项目成员"
    pm_project ||--o{ pm_project_manager_change : "经理变更"
    pm_project ||--o{ pm_project_stage_change : "阶段变更"
    pm_project ||--o{ pm_workload_correct_log : "人天补正"
    pm_project ||--o{ pm_team_revenue_confirmation : "团队收入确认"
    pm_project ||--o{ pm_attachment : "项目附件"

    pm_contract ||--o{ pm_project_contract_rel : "关联项目"
    pm_contract ||--o{ pm_payment : "款项管理"
    pm_contract ||--o{ pm_attachment : "合同附件"
    pm_contract }o--|| pm_customer : "所属客户"

    pm_customer ||--o{ pm_customer_contact : "联系人"

    pm_payment ||--o{ pm_attachment : "款项附件"

    pm_task }o--o| pm_production_batch : "投产批次"

    pm_daily_report ||--o{ pm_daily_report_detail : "日报明细"
    pm_daily_report_whitelist }o--|| sys_user : "白名单用户"

    pm_attachment ||--o{ pm_attachment_log : "操作日志"

    pm_project }o--|| sys_dept : "项目部门"
    pm_project }o--o| sys_user : "项目经理"
    pm_project }o--o| pm_customer : "关联客户"
    pm_project }o--o| pm_secondary_region : "二级区域"

    pm_project_member }o--|| sys_user : "成员用户"
    pm_daily_report }o--|| sys_user : "填报人"
```

### 1.2 项目管理表 `pm_project`

```mermaid
erDiagram
    pm_project {
        bigint project_id PK "项目ID"
        varchar project_code UK "项目编号(行业-区域-省份-简称-年份)"
        varchar project_name "项目名称"
        varchar industry "行业(字典:industry)"
        varchar region "一级区域(字典:sys_yjqy)"
        bigint region_id FK "二级区域ID"
        varchar short_name "简称"
        varchar established_year "立项年度(字典:sys_ndgl)"
        varchar project_category "项目分类(字典:sys_xmfl)"
        varchar project_dept FK "项目部门ID"
        varchar project_status "项目状态(字典:sys_xmzt)"
        varchar project_stage "项目阶段(字典:sys_xmjd)"
        varchar acceptance_status "验收状态(字典:sys_yszt)"
        decimal estimated_workload "预估工作量(人天)"
        decimal actual_workload "实际工作量(小时,日报汇总)"
        decimal adjust_workload "调整工作量(人天)"
        varchar project_address "项目地址"
        text project_plan "项目计划"
        text project_description "项目描述"
        bigint project_manager_id FK "项目经理ID"
        bigint market_manager_id FK "市场经理ID"
        varchar participants "参与人员ID(逗号分隔)"
        bigint sales_manager_id FK "销售负责人ID"
        varchar sales_contact "销售联系方式"
        bigint team_leader_id FK "团队负责人ID"
        bigint customer_id FK "客户ID"
        bigint customer_contact_id FK "客户联系人ID"
        varchar merchant_contact "商户联系人"
        varchar merchant_phone "商户联系方式"
        date start_date "启动日期"
        date end_date "结束日期"
        date production_date "投产日期"
        date acceptance_date "验收日期"
        decimal project_budget "项目预算(元)"
        decimal project_cost "项目费用(元)"
        decimal expense_budget "费用预算(元)"
        decimal cost_budget "成本预算(元)"
        decimal labor_cost "人力费用(元)"
        decimal purchase_cost "采购成本(元)"
        varchar approval_status "审核状态(0待审核/1通过/2拒绝)"
        varchar approval_reason "审核意见"
        datetime approval_time "审批时间"
        varchar approver_id "审批人"
        varchar revenue_confirm_status "收入确认状态(字典:sys_qrzt)"
        varchar revenue_confirm_year "收入确认年度(字典:sys_ndgl)"
        date revenue_confirm_date "收入确认日期"
        decimal confirm_amount "确认金额(含税)"
        decimal tax_rate "税率(%)"
        decimal after_tax_amount "税后金额"
        varchar company_revenue_confirmed_by "公司收入确认人"
        datetime company_revenue_confirmed_time "确认操作时间"
        text remark "备注"
        char del_flag "删除标志(0正常 1删除)"
        varchar create_by "创建者"
        datetime create_time "创建时间"
        varchar update_by "更新者"
        datetime update_time "更新时间"
    }
```

### 1.3 任务管理表 `pm_task`

```mermaid
erDiagram
    pm_task {
        bigint task_id PK "任务ID"
        bigint project_id FK "所属主项目ID"
        varchar task_code "任务编号"
        varchar task_name "任务名称"
        varchar task_stage "任务阶段(字典:sys_xmjd)"
        bigint task_manager_id FK "任务负责人ID"
        varchar product "产品(字典:sys_product)"
        varchar bank_demand_no "总行需求号"
        varchar software_demand_no "软件中心需求编号"
        decimal task_budget "任务预算(元)"
        decimal estimated_workload "预估工作量(人天)"
        decimal actual_workload "实际工作量(小时,日报汇总)"
        varchar production_year "投产年度(字典:sys_ndgl)"
        bigint batch_id FK "投产批次ID"
        varchar schedule_status "排期状态(字典:sys_pqzt)"
        text task_plan "任务计划"
        text task_description "任务描述"
        text function_description "功能点说明"
        text implementation_plan "实施计划"
        date start_date "启动日期"
        date end_date "结束日期"
        date production_date "投产时间"
        date production_version_date "生产版本日期"
        date actual_production_date "实际投产日期"
        date internal_closure_date "内部B包日期"
        date functional_test_date "功能测试版本日期"
        varchar create_by "创建者"
        datetime create_time "创建时间"
        varchar update_by "更新者"
        datetime update_time "更新时间"
        text remark "备注"
    }

    pm_production_batch {
        bigint batch_id PK "批次ID"
        varchar batch_no "批次号"
        int sort_order "排序"
        varchar production_year "投产年份(字典:sys_ndgl)"
        date plan_production_date "计划投产日期"
        varchar remark "备注"
        varchar create_by "创建者"
        datetime create_time "创建时间"
        varchar update_by "更新者"
        datetime update_time "更新时间"
    }

    pm_task }o--o| pm_production_batch : "所属批次"
```

### 1.4 合同与款项

```mermaid
erDiagram
    pm_contract {
        bigint contract_id PK "合同主键ID"
        varchar contract_code UK "合同编号"
        varchar contract_name "合同名称"
        bigint customer_id FK "关联客户ID"
        bigint dept_id FK "部门ID"
        varchar contract_type "合同类型(字典:sys_htlx)"
        varchar contract_status "合同状态(字典:sys_htzt)"
        date contract_sign_date "合同签订日期"
        int contract_period "合同周期(月)"
        decimal contract_amount "合同金额(含税)"
        decimal tax_rate "税率(%)"
        decimal amount_no_tax "不含税金额"
        decimal tax_amount "税金"
        decimal confirm_amount "合同确认金额"
        varchar confirm_year "确认年份(字典:sys_ndgl)"
        int free_maintenance_period "免维期(月)"
        char del_flag "删除标志"
        varchar create_by "创建者"
        datetime create_time "创建时间"
        varchar update_by "更新者"
        datetime update_time "更新时间"
        varchar remark "备注"
    }

    pm_project_contract_rel {
        bigint rel_id PK "关系主键ID"
        bigint project_id FK "项目ID"
        bigint contract_id FK "合同ID"
        varchar rel_status "关系状态(有效/失效)"
        date bind_date "关联日期"
        char del_flag "删除标志"
        varchar create_by "创建者"
        datetime create_time "创建时间"
    }

    pm_payment {
        bigint payment_id PK "款项主键ID"
        bigint contract_id FK "合同ID"
        varchar payment_method_name "付款方式名称"
        decimal payment_amount "付款总金额"
        char has_penalty "是否涉及违约扣款(1是0否)"
        decimal penalty_amount "扣款金额(元)"
        varchar payment_status "付款状态(字典:sys_fkzt)"
        varchar expected_quarter "预计回款季度(字典:sys_jdgl)"
        varchar actual_quarter "实际回款季度(字典:sys_jdgl)"
        date submit_acceptance_date "开票日期"
        date actual_payment_date "实际回款日期"
        varchar confirm_year "款项确认年份(字典:sys_ndgl)"
        char del_flag "删除标志"
        varchar create_by "创建者"
        datetime create_time "创建时间"
        varchar update_by "更新者"
        datetime update_time "更新时间"
        varchar remark "备注"
    }

    pm_contract ||--o{ pm_payment : "包含款项"
    pm_contract ||--o{ pm_project_contract_rel : "关联项目"
    pm_contract }o--|| pm_customer : "所属客户"
```

### 1.5 客户管理

```mermaid
erDiagram
    pm_customer {
        bigint customer_id PK "客户主键ID"
        varchar customer_simple_name "客户简称"
        varchar customer_all_name "客户全称"
        varchar industry "所属行业"
        varchar region "所属区域"
        bigint sales_manager_id FK "销售负责人ID"
        varchar office_address "办公地址"
        char del_flag "删除标志"
        varchar create_by "创建者"
        datetime create_time "创建时间"
        varchar update_by "更新者"
        datetime update_time "更新时间"
        varchar remark "备注"
    }

    pm_customer_contact {
        bigint contact_id PK "联系人主键ID"
        bigint customer_id FK "客户ID"
        varchar contact_name "联系人姓名"
        varchar contact_phone "联系人电话"
        varchar contact_tag "联系人标签"
        char del_flag "删除标志"
        varchar create_by "创建者"
        datetime create_time "创建时间"
        varchar update_by "更新者"
        datetime update_time "更新时间"
        varchar remark "备注"
    }

    pm_customer ||--o{ pm_customer_contact : "拥有联系人"
```

### 1.6 日报管理

```mermaid
erDiagram
    pm_daily_report {
        bigint report_id PK "日报主键ID"
        date report_date "日报日期"
        bigint user_id FK "用户ID"
        bigint dept_id FK "部门ID"
        decimal total_work_hours "当日总工时(小时)"
        char del_flag "删除标志"
        varchar create_by "创建者"
        datetime create_time "创建时间"
        varchar update_by "更新者"
        datetime update_time "更新时间"
        varchar remark "备注"
    }

    pm_daily_report_detail {
        bigint detail_id PK "明细主键ID"
        bigint report_id FK "日报ID"
        bigint project_id FK "项目ID(work时必填)"
        varchar project_stage "项目阶段(字典:sys_xmjd)"
        varchar entry_type "条目类型(work/leave/comp/annual)"
        decimal leave_hours "假期时长(小时)"
        decimal work_hours "工时(小时)"
        text work_content "工作内容"
        bigint sub_project_id FK "任务ID(pm_task.task_id)"
        varchar work_category "工作任务类别(字典:sys_gzlb)"
        char del_flag "删除标志"
        varchar create_by "创建者"
        datetime create_time "创建时间"
    }

    %% DailyReportDetail 扩展字段（非DB列，JOIN查询填充）：
    %% projectName, projectCode, projectManagerName,
    %% subProjectName, subProjectStage, subProjectManagerId/Name,
    %% subProjectTaskCode, subProjectBatchNo,
    %% estimatedWorkload, actualWorkload, revenueConfirmYear,
    %% subProjectEstimatedWorkload, subProjectActualWorkload

    pm_work_calendar {
        bigint id PK "主键"
        date calendar_date UK "日期"
        varchar day_type "类型(holiday/workday)"
        varchar day_name "名称(如春节)"
        int year "年份"
        char del_flag "删除标志"
    }

    pm_daily_report_whitelist {
        bigint id PK "主键"
        bigint user_id FK "白名单用户ID"
        varchar reason "加入原因"
        char del_flag "删除标志"
    }

    pm_daily_report ||--o{ pm_daily_report_detail : "包含明细"
    pm_daily_report }o--|| sys_user : "填报人"
```

### 1.7 辅助业务表

```mermaid
erDiagram
    pm_project_approval {
        bigint approval_id PK "审核主键ID"
        bigint project_id FK "项目ID"
        varchar approval_status "审核状态(0待审核/1通过/2拒绝)"
        text approval_reason "审核意见"
        bigint approver_id FK "审核人ID"
        datetime approval_time "审核时间"
        char del_flag "删除标志"
        varchar create_by "创建者"
        datetime create_time "创建时间"
    }

    pm_project_member {
        bigint member_id PK "成员主键ID"
        bigint project_id FK "项目ID"
        bigint user_id FK "用户ID"
        date join_date "加入日期"
        date leave_date "离开日期"
        char is_active "是否在项目中(1是0否)"
        char del_flag "删除标志"
    }

    pm_project_manager_change {
        bigint change_id PK "变更主键ID"
        bigint project_id FK "项目ID"
        bigint old_manager_id FK "原项目经理ID"
        bigint new_manager_id FK "新项目经理ID"
        varchar change_reason "变更原因"
        char del_flag "删除标志"
        varchar create_by "创建者"
        datetime create_time "创建时间"
    }

    pm_project_stage_change {
        bigint change_id PK "变更记录ID"
        bigint project_id FK "项目ID"
        varchar old_stage "变更前阶段(字典:sys_xmjd)"
        varchar new_stage "变更后阶段(字典:sys_xmjd)"
        varchar old_project_status "变更前状态(字典:sys_xmzt)"
        varchar new_project_status "变更后状态(字典:sys_xmzt)"
        varchar change_reason "变更原因"
        char del_flag "删除标志"
    }

    pm_team_revenue_confirmation {
        bigint team_confirm_id PK "团队确认ID"
        bigint project_id FK "项目ID"
        bigint dept_id FK "部门ID"
        decimal confirm_amount "确认金额"
        datetime confirm_time "确认时间"
        bigint confirm_user_id FK "确认人ID"
        varchar remark "备注"
        char del_flag "删除标志"
    }

    pm_workload_correct_log {
        bigint log_id PK "日志ID"
        bigint project_id FK "项目ID"
        tinyint direction "调整方向(0增加/1减少)"
        decimal delta "调整人天数"
        decimal before_adjust "调整前值"
        decimal after_adjust "调整后值"
        varchar reason "补正理由"
        varchar create_by "创建者"
        datetime create_time "创建时间"
    }

    pm_secondary_region {
        bigint region_id PK "二级区域ID"
        varchar region_code UK "区域代码"
        varchar region_name "区域名称"
        varchar region_type "类型(省/直辖市/自治区/特别行政区)"
        varchar region_dict_value FK "一级区域字典值"
        int sort_order "排序"
        char status "状态"
        char del_flag "删除标志"
    }

    pm_attachment {
        bigint attachment_id PK "附件主键ID"
        varchar business_type "业务类型(project/contract/payment)"
        bigint business_id "业务ID"
        varchar document_type "文档类型(字典:sys_wdlx)"
        varchar file_name "文件名"
        varchar file_path "文件路径"
        bigint file_size "文件大小(字节)"
        varchar file_type "文件类型(扩展名)"
        varchar file_description "文件描述"
        char del_flag "删除标志"
    }

    pm_attachment_log {
        bigint log_id PK "日志主键ID"
        bigint attachment_id FK "附件ID"
        varchar business_type "业务类型"
        bigint business_id "业务ID"
        varchar operation_type "操作类型(upload/delete/download)"
        varchar operation_desc "操作描述"
        bigint operator_id "操作人ID"
        varchar operator_name "操作人姓名"
        datetime operation_time "操作时间"
    }
```

---

## 二、RuoYi 系统管理模块 ER 图

### 2.1 用户-角色-部门-权限 关系

```mermaid
erDiagram
    sys_user ||--o{ sys_user_role : "拥有角色"
    sys_user ||--o{ sys_user_post : "拥有岗位"
    sys_user }o--|| sys_dept : "所属部门"

    sys_role ||--o{ sys_user_role : "分配用户"
    sys_role ||--o{ sys_role_menu : "分配菜单"
    sys_role ||--o{ sys_role_dept : "数据权限"

    sys_menu ||--o{ sys_role_menu : "角色绑定"

    sys_post ||--o{ sys_user_post : "用户绑定"

    sys_dept ||--o{ sys_role_dept : "角色绑定"

    sys_user {
        bigint user_id PK "用户ID"
        bigint dept_id FK "部门ID"
        varchar user_name "用户账号"
        varchar nick_name "用户昵称"
        varchar user_type "用户类型(00系统用户)"
        varchar email "邮箱"
        varchar phonenumber "手机号码"
        char sex "性别(0男/1女/2未知)"
        varchar avatar "头像地址"
        varchar password "密码(加密)"
        char status "状态(0正常/1停用)"
        char del_flag "删除标志(0存在/2删除)"
        varchar login_ip "最后登录IP"
        datetime login_date "最后登录时间"
        varchar create_by "创建者"
        datetime create_time "创建时间"
        varchar update_by "更新者"
        datetime update_time "更新时间"
        varchar remark "备注"
    }

    sys_dept {
        bigint dept_id PK "部门ID"
        bigint parent_id "父部门ID"
        varchar ancestors "祖级列表(逗号分隔)"
        varchar dept_name "部门名称"
        int order_num "显示顺序"
        varchar leader "负责人"
        varchar phone "联系电话"
        varchar email "邮箱"
        char status "状态(0正常/1停用)"
        char del_flag "删除标志(0存在/2删除)"
    }

    sys_role {
        bigint role_id PK "角色ID"
        varchar role_name "角色名称"
        varchar role_key "角色权限字符串"
        int role_sort "显示顺序"
        char data_scope "数据范围(1全部/2自定/3本部门/4部门及以下)"
        tinyint menu_check_strictly "菜单树关联显示"
        tinyint dept_check_strictly "部门树关联显示"
        char status "状态(0正常/1停用)"
        char del_flag "删除标志"
    }

    sys_menu {
        bigint menu_id PK "菜单ID"
        varchar menu_name "菜单名称"
        bigint parent_id "父菜单ID"
        int order_num "显示顺序"
        varchar path "路由地址"
        varchar component "组件路径"
        varchar route_name "路由名称"
        int is_frame "是否外链(0是/1否)"
        int is_cache "是否缓存(0缓存/1不缓存)"
        char menu_type "菜单类型(M目录/C菜单/F按钮)"
        char visible "显示状态(0显示/1隐藏)"
        char status "状态(0正常/1停用)"
        varchar perms "权限标识"
        varchar icon "菜单图标"
    }

    sys_post {
        bigint post_id PK "岗位ID"
        varchar post_code "岗位编码(pm/scjl/xsfzr等)"
        varchar post_name "岗位名称"
        int post_sort "显示顺序"
        char status "状态(0正常/1停用)"
    }
```

### 2.2 关联表（多对多）

```mermaid
erDiagram
    sys_user_role {
        bigint user_id PK_FK "用户ID"
        bigint role_id PK_FK "角色ID"
    }

    sys_role_menu {
        bigint role_id PK_FK "角色ID"
        bigint menu_id PK_FK "菜单ID"
    }

    sys_role_dept {
        bigint role_id PK_FK "角色ID"
        bigint dept_id PK_FK "部门ID"
    }

    sys_user_post {
        bigint user_id PK_FK "用户ID"
        bigint post_id PK_FK "岗位ID"
    }
```

### 2.3 系统辅助表

```mermaid
erDiagram
    sys_dict_type {
        bigint dict_id PK "字典主键"
        varchar dict_name "字典名称"
        varchar dict_type UK "字典类型"
        char status "状态"
    }

    sys_dict_data {
        bigint dict_code PK "字典编码"
        int dict_sort "字典排序"
        varchar dict_label "字典标签"
        varchar dict_value "字典键值"
        varchar dict_type FK "字典类型"
        char is_default "是否默认(Y是/N否)"
        char status "状态"
    }

    sys_config {
        int config_id PK "参数主键"
        varchar config_name "参数名称"
        varchar config_key "参数键名"
        varchar config_value "参数键值"
        char config_type "系统内置(Y是/N否)"
    }

    sys_oper_log {
        bigint oper_id PK "日志主键"
        varchar title "模块标题"
        int business_type "业务类型(0其它/1新增/2修改/3删除)"
        varchar method "方法名称"
        varchar request_method "请求方式"
        varchar oper_name "操作人员"
        varchar dept_name "部门名称"
        varchar oper_url "请求URL"
        varchar oper_ip "主机地址"
        int status "操作状态(0正常/1异常)"
        datetime oper_time "操作时间"
    }

    sys_logininfor {
        bigint info_id PK "访问ID"
        varchar user_name "用户账号"
        varchar ipaddr "登录IP"
        varchar login_location "登录地点"
        varchar browser "浏览器"
        varchar os "操作系统"
        char status "登录状态(0成功/1失败)"
        datetime login_time "访问时间"
    }

    sys_notice {
        int notice_id PK "公告ID"
        varchar notice_title "公告标题"
        char notice_type "类型(1通知/2公告)"
        longblob notice_content "公告内容"
        char status "状态(0正常/1关闭)"
    }

    sys_dict_type ||--o{ sys_dict_data : "包含字典项"
```

---

## 三、完整表清单

### 3.1 PM 业务表（21张）

| 序号 | 表名 | 中文名 | 删除策略 | 说明 |
|------|------|--------|----------|------|
| 1 | `pm_project` | 项目管理表 | **硬删除** | 核心主表 |
| 2 | `pm_project_approval` | 项目审核表 | 软删除 | 审批工作流记录 |
| 3 | `pm_task` | 任务管理表 | **硬删除** | 项目分解任务 |
| 4 | `pm_production_batch` | 投产批次表 | **硬删除**（无 del_flag 列）| 批次管理 |
| 5 | `pm_contract` | 合同管理表 | 软删除 | 合同信息 |
| 6 | `pm_project_contract_rel` | 项目合同关系表 | 软删除 | 多对多关联 |
| 7 | `pm_payment` | 款项表 | 软删除 | 付款里程碑 |
| 8 | `pm_customer` | 客户信息表 | 软删除 | 客户管理 |
| 9 | `pm_customer_contact` | 客户联系人表 | 软删除 | 联系人信息 |
| 10 | `pm_project_member` | 项目人员管理表 | 软删除 | 项目成员 |
| 11 | `pm_project_manager_change` | 项目经理变更表 | 软删除 | 变更记录 |
| 12 | `pm_project_stage_change` | 项目阶段变更表 | 软删除 | 阶段变更记录 |
| 13 | `pm_team_revenue_confirmation` | 团队收入确认表 | 软删除 | 团队维度收入 |
| 14 | `pm_daily_report` | 日报主表 | **硬删除** | 一人一天一条 |
| 15 | `pm_daily_report_detail` | 日报明细表 | **硬删除** | 项目工时明细 |
| 16 | `pm_work_calendar` | 工作日历表 | 软删除 | 节假日/调休 |
| 17 | `pm_daily_report_whitelist` | 日报白名单表 | 软删除 | 免填日报人员 |
| 18 | `pm_secondary_region` | 二级区域表 | 软删除 | 省/直辖市/自治区 |
| 19 | `pm_attachment` | 附件表 | 软删除 | 文件附件 |
| 20 | `pm_attachment_log` | 附件操作日志表 | 无 del_flag | 审计日志 |
| 21 | `pm_workload_correct_log` | 工作量补正日志 | 无 del_flag | 人天补正审计 |

### 3.2 RuoYi 系统表（19张）

| 序号 | 表名 | 中文名 | 说明 |
|------|------|--------|------|
| 1 | `sys_user` | 用户信息表 | 系统用户 |
| 2 | `sys_dept` | 部门表 | 树形结构 |
| 3 | `sys_role` | 角色信息表 | RBAC 角色 |
| 4 | `sys_menu` | 菜单权限表 | 路由+按钮权限 |
| 5 | `sys_post` | 岗位信息表 | pm/scjl/xsfzr 等 |
| 6 | `sys_user_role` | 用户角色关联表 | 多对多 |
| 7 | `sys_role_menu` | 角色菜单关联表 | 多对多 |
| 8 | `sys_role_dept` | 角色部门关联表 | 数据权限 |
| 9 | `sys_user_post` | 用户岗位关联表 | 多对多 |
| 10 | `sys_dict_type` | 字典类型表 | 字典分类 |
| 11 | `sys_dict_data` | 字典数据表 | 字典项 |
| 12 | `sys_config` | 参数配置表 | 系统参数 |
| 13 | `sys_oper_log` | 操作日志记录 | 审计日志 |
| 14 | `sys_logininfor` | 系统访问记录 | 登录日志 |
| 15 | `sys_notice` | 通知公告表 | 站内通知 |
| 16 | `sys_job` | 定时任务调度表 | Quartz 任务 |
| 17 | `sys_job_log` | 定时任务日志表 | 执行日志 |
| 18 | `gen_table` | 代码生成业务表 | 代码生成器 |
| 19 | `gen_table_column` | 代码生成字段表 | 字段配置 |

### 3.3 Quartz 调度表（9张）

| 表名 | 说明 |
|------|------|
| `qrtz_blob_triggers` | Blob 类型触发器 |
| `qrtz_calendars` | 日历信息 |
| `qrtz_cron_triggers` | Cron 类型触发器 |
| `qrtz_fired_triggers` | 已触发触发器 |
| `qrtz_job_details` | 任务详细信息 |
| `qrtz_locks` | 悲观锁信息 |
| `qrtz_paused_trigger_grps` | 暂停的触发器组 |
| `qrtz_scheduler_state` | 调度器状态 |
| `qrtz_simple_triggers` | 简单触发器 |
| `qrtz_simprop_triggers` | 同步行锁 |
| `qrtz_triggers` | 触发器详细信息 |

---

## 四、字典依赖关系

| 字典类型 | 中文名 | 使用表/字段 |
|----------|--------|-------------|
| `industry` | 行业 | `pm_project.industry` |
| `sys_yjqy` | 一级区域 | `pm_project.region`、`pm_secondary_region.region_dict_value` |
| `sys_ndgl` | 年度管理 | `pm_project.established_year/revenue_confirm_year`、`pm_contract.confirm_year`、`pm_payment.confirm_year`、`pm_task.production_year`、`pm_production_batch.production_year` |
| `sys_xmfl` | 项目分类 | `pm_project.project_category` |
| `sys_xmjd` | 项目阶段 | `pm_project.project_stage`、`pm_task.task_stage`、`pm_daily_report_detail.project_stage`、`pm_project_stage_change.old_stage/new_stage` |
| `sys_xmzt` | 项目状态 | `pm_project.project_status` |
| `sys_yszt` | 验收状态 | `pm_project.acceptance_status` |
| `sys_spzt` | 审核状态 | `pm_project.approval_status` |
| `sys_qrzt` | 确认状态 | `pm_project.revenue_confirm_status` |
| `sys_htlx` | 合同类型 | `pm_contract.contract_type` |
| `sys_htzt` | 合同状态 | `pm_contract.contract_status` |
| `sys_fkzt` | 付款状态 | `pm_payment.payment_status` |
| `sys_jdgl` | 季度管理 | `pm_payment.expected_quarter/actual_quarter` |
| `sys_wdlx` | 文档类型 | `pm_attachment.document_type` |
| `sys_gzlb` | 工作任务类别 | `pm_daily_report_detail.work_category` |
| `sys_pqzt` | 排期状态 | `pm_task.schedule_status` |
| `sys_product` | 产品 | `pm_task.product` |

---

## 五、关键约束与索引说明

### 唯一约束

| 表 | 约束 | 字段 |
|----|------|------|
| `pm_project` | `uk_project_code` | `project_code` |
| `pm_contract` | `uk_contract_code` | `contract_code` |
| `pm_project_contract_rel` | `uk_project_contract` | `project_id, contract_id, del_flag` |
| `pm_daily_report` | `uk_user_date` | `user_id, report_date` |
| `pm_secondary_region` | `uk_region_code` | `region_code` |
| `pm_work_calendar` | `uk_calendar_date` | `calendar_date` |

### Collation 注意事项

PM 业务表多数使用 `utf8mb4_0900_ai_ci`，系统表使用 `utf8mb4_unicode_ci`。跨模块 JOIN 时必须显式指定 COLLATE：

```sql
-- 示例：项目表 JOIN 系统用户表
LEFT JOIN sys_user u
  ON p.update_by COLLATE utf8mb4_unicode_ci = u.user_name

-- 示例：项目阶段 JOIN 字典表
LEFT JOIN sys_dict_data d
  ON t.type COLLATE utf8mb4_unicode_ci = d.dict_value
```
