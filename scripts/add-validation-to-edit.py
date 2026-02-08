#!/usr/bin/env python3
"""
批量为 edit.vue 添加表单验证属性
"""
import re

# 读取文件
with open('ruoyi-ui/src/views/project/project/edit.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. 为所有 el-form-item 添加 data-prop 属性（如果还没有）
# 匹配 <el-form-item ... prop="xxx" 但没有 data-prop 的情况
pattern1 = r'(<el-form-item[^>]*?prop="([^"]+)"(?![^>]*data-prop))'
replacement1 = r'\1 data-prop="\2"'
content = re.sub(pattern1, replacement1, content)

# 2. 为 el-input 添加 @blur 事件（如果还没有）
# 需要找到对应的 prop 值
lines = content.split('\n')
new_lines = []
current_prop = None

for i, line in enumerate(lines):
    # 检查是否是 form-item 行
    form_item_match = re.search(r'prop="([^"]+)"', line)
    if form_item_match:
        current_prop = form_item_match.group(1)

    # 检查是否是输入控件行，并且没有 @blur
    if current_prop and '@blur' not in line:
        # el-input
        if '<el-input ' in line and 'v-model' in line and 'readonly' not in line and 'disabled' not in line:
            line = re.sub(r'(v-model="[^"]+")(\s*/?)', rf'\1 @blur="validateOnBlur(\'{current_prop}\')"', line)
        # el-select
        elif '<el-select ' in line and 'v-model' in line:
            line = re.sub(r'(v-model="[^"]+")(\s)', rf'\1 @blur="validateOnBlur(\'{current_prop}\')"', line)
        # el-input-number
        elif '<el-input-number ' in line and 'v-model' in line:
            line = re.sub(r'(v-model="[^"]+")(\s)', rf'\1 @blur="validateOnBlur(\'{current_prop}\')"', line)
        # el-date-picker
        elif '<el-date-picker' in line and 'v-model' in line:
            line = re.sub(r'(v-model="[^"]+")(\s)', rf'\1 @blur="validateOnBlur(\'{current_prop}\')"', line)
        # el-tree-select
        elif '<el-tree-select' in line and 'v-model' in line:
            line = re.sub(r'(v-model="[^"]+")(\s)', rf'\1 @blur="validateOnBlur(\'{current_prop}\')"', line)

    new_lines.append(line)

    # 如果遇到 </el-form-item>，清除当前 prop
    if '</el-form-item>' in line:
        current_prop = None

content = '\n'.join(new_lines)

# 写回文件
with open('ruoyi-ui/src/views/project/project/edit.vue', 'w', encoding='utf-8') as f:
    f.write(content)

print("✅ edit.vue 批量修改完成！")
print("已添加:")
print("  - data-prop 属性到所有 el-form-item")
print("  - @blur 事件到所有表单控件")
