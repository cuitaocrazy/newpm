package com.ruoyi.project.domain.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 人天补正请求 DTO
 */
public class WorkloadCorrectRequest {

    @NotNull(message = "补正方向不能为空")
    private Integer direction;

    @NotNull(message = "补正值不能为空")
    private BigDecimal delta;

    private BigDecimal afterAdjust;

    private String reason;

    public Integer getDirection() { return direction; }
    public void setDirection(Integer direction) { this.direction = direction; }

    public BigDecimal getDelta() { return delta; }
    public void setDelta(BigDecimal delta) { this.delta = delta; }

    public BigDecimal getAfterAdjust() { return afterAdjust; }
    public void setAfterAdjust(BigDecimal afterAdjust) { this.afterAdjust = afterAdjust; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
