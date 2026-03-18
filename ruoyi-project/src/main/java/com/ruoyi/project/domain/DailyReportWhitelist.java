package com.ruoyi.project.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 日报填写白名单实体
 */
public class DailyReportWhitelist extends BaseEntity {

    private Long id;

    @NotNull(message = "用户不能为空")
    private Long userId;

    @NotBlank(message = "加入原因不能为空")
    private String reason;

    private String delFlag;

    /** 显示字段（JOIN 查询结果） */
    @Excel(name = "姓名")
    private String nickName;

    @Excel(name = "部门")
    private String deptName;

    /** 搜索条件（前端传入，非数据库字段） */
    private String keyword;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
