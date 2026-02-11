import { nextTick } from 'vue'

/**
 * 表单验证增强 Composable
 * 提供失焦验证和滚动到错误字段的功能
 */
export function useFormValidation(formRef, activeNames) {
  /**
   * 失焦时验证单个字段
   * @param {string} prop - 字段名
   */
  const validateOnBlur = (prop) => {
    if (!formRef.value) return
    formRef.value.validateField(prop, () => {})
  }

  /**
   * 验证表单并滚动到第一个错误字段
   * @param {Function} callback - 验证成功后的回调函数
   */
  const validateAndScroll = (callback) => {
    if (!formRef.value) return

    formRef.value.validate((valid, fields) => {
      if (valid) {
        // 验证通过，执行回调
        callback()
      } else {
        // 验证失败，找到第一个错误字段并滚动到它
        const firstErrorField = Object.keys(fields)[0]
        scrollToField(firstErrorField)
      }
    })
  }

  /**
   * 滚动到指定字段
   * @param {string} prop - 字段名
   */
  const scrollToField = (prop) => {
    nextTick(() => {
      // 查找包含该字段的表单项
      const formItem = document.querySelector(`[data-prop="${prop}"]`)
      if (!formItem) return

      // 查找该字段所在的折叠面板
      const panel = formItem.closest('[data-panel]')
      if (panel && activeNames) {
        const panelName = panel.getAttribute('data-panel')
        // 如果面板是折叠状态，先展开它
        if (!activeNames.value.includes(panelName)) {
          activeNames.value.push(panelName)
          // 等待面板展开动画完成后再滚动
          setTimeout(() => {
            formItem.scrollIntoView({ behavior: 'smooth', block: 'center' })
          }, 300)
          return
        }
      }

      // 直接滚动到字段
      formItem.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
  }

  return {
    validateOnBlur,
    validateAndScroll,
    scrollToField
  }
}
