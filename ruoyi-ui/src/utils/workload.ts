/**
 * 工时转换工具函数
 * actual_workload 在数据库中存储为小时，前端显示为人天（/8 + 调整量）
 */

/**
 * 将工时（小时）转换为人天显示值
 * @param hours      实际工时（小时），来自 actual_workload
 * @param adjustDays 调整人天，来自 adjust_workload（已经是人天单位）
 * @returns 格式化后的人天字符串（3位小数）
 */
export function toPersonDays(hours: number | string | null | undefined, adjustDays: number | string | null | undefined = 0): string {
  const h = parseFloat(String(hours ?? 0)) || 0
  const a = parseFloat(String(adjustDays ?? 0)) || 0
  return (h / 8 + a).toFixed(3)
}
