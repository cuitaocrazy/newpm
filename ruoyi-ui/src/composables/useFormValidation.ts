import { nextTick, ref, computed } from 'vue'
import type { Ref } from 'vue'
import type { FormInstance } from 'element-plus'

/**
 * 表单验证增强 Composable
 * 提供实时验证、错误定位、自动滚动等功能
 */
export function useFormValidation(
  formRef: Ref<FormInstance | undefined>,
  activeNames?: Ref<string[]>
) {
  // 错误字段映射
  const errorFields = ref<Record<string, string>>({})

  // 错误数量
  const errorCount = computed(() => Object.keys(errorFields.value).length)

  /**
   * 单字段实时验证（blur 触发）
   * @param prop 字段名
   */
  const validateOnBlur = (prop: string) => {
    if (!formRef.value) return

    formRef.value.validateField(prop, (valid, invalidFields) => {
      if (!valid && invalidFields) {
        // 记录错误信息
        errorFields.value[prop] = invalidFields[prop][0].message || '验证失败'
      } else {
        // 清除错误信息
        delete errorFields.value[prop]
      }
    })
  }

  /**
   * 提交验证 + 滚动定位
   * @param callback 验证通过后的回调函数
   */
  const validateAndScroll = async (callback: () => void) => {
    if (!formRef.value) return

    formRef.value.validate((valid, fields) => {
      if (!valid && fields) {
        // 记录所有错误字段
        errorFields.value = {}
        Object.keys(fields).forEach(prop => {
          errorFields.value[prop] = fields[prop][0].message || '验证失败'
        })

        // 滚动到第一个错误字段
        const firstErrorProp = Object.keys(fields)[0]
        scrollToError(firstErrorProp)
      } else {
        // 验证通过，清空错误记录
        errorFields.value = {}
        callback()
      }
    })
  }

  /**
   * 滚动到错误字段位置
   * @param prop 字段名
   */
  const scrollToError = (prop: string) => {
    nextTick(() => {
      // 查找对应的 form-item 元素
      const selector = `[data-prop="${prop}"]`
      const element = document.querySelector(selector) as HTMLElement

      if (!element) {
        console.warn(`未找到字段: ${prop}`)
        return
      }

      // 如果有折叠面板，先展开对应面板
      if (activeNames) {
        const panel = element.closest('[data-panel]') as HTMLElement
        const panelIndex = panel?.getAttribute('data-panel')

        if (panelIndex && !activeNames.value.includes(panelIndex)) {
          activeNames.value.push(panelIndex)
        }
      }

      // 延迟滚动，确保面板展开动画完成
      setTimeout(() => {
        element.scrollIntoView({
          behavior: 'smooth',
          block: 'center'
        })

        // 添加抖动高亮效果
        element.classList.add('shake-error')
        setTimeout(() => {
          element.classList.remove('shake-error')
        }, 600)
      }, 300)
    })
  }

  /**
   * 清空所有验证错误
   */
  const clearValidate = () => {
    formRef.value?.clearValidate()
    errorFields.value = {}
  }

  /**
   * 重置表单
   */
  const resetFields = () => {
    formRef.value?.resetFields()
    errorFields.value = {}
  }

  return {
    validateOnBlur,
    validateAndScroll,
    scrollToError,
    clearValidate,
    resetFields,
    errorFields,
    errorCount
  }
}
