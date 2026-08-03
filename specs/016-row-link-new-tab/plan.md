# 016 方案 — RowLinkButton 组件化改造

需求见 [spec.md](./spec.md) / [Issue #12](https://github.com/cuitaocrazy/newpm/issues/12)。

## 1. 技术选型

### 为什么是 `el-button tag="a"` 而不是换成 `el-link`

Element Plus 2.13.1 的 `el-button` **支持 `tag` prop**（`node_modules/element-plus/es/components/button/src/button.d.ts:32`，默认 `"button"`），渲染走 `createBlock(resolveDynamicComponent(_ctx.tag), ...)`（`button2.mjs:54`）。传 `tag="a"` 即渲染成 `<a>`，**class 与子节点结构完全不变**，因此视觉像素级一致。

改用 `el-link` 则会换一套 class（`.el-link` vs `.el-button.is-link`），字号、内边距、hover 下划线均不同，违反"视觉零变化"红线。

### 为什么抽组件而不是 9 个页面各写一遍

17 个入口若各自手写 `<el-button tag="a" :href="router.resolve(...).href" @click="...">`，修饰键放行逻辑要复制 17 遍，任何一处漏写 `metaKey` 就是一个静默失效的入口。抽成 `RowLinkButton` 后，行为定义只有一处。

## 2. 组件设计

`ruoyi-ui/src/components/RowLinkButton/index.vue`，全局注册于 `main.ts`（本仓库**没装** `unplugin-vue-components`，必须手工 `app.component`）。

```vue
<row-link-button
  :to="`/project/list/detail/${scope.row.projectId}`"
  icon="View" label="详情"
  v-hasPermi="['project:project:query']"
  @navigate="handleDetail(scope.row)" />
```

| Prop | 说明 |
|---|---|
| `to` | 目标路由，支持字符串路径与 `{ path, query }` 对象两种形态 |
| `label` | 按钮文字（默认插槽优先） |
| `icon` | 透传 el-button，字符串形态可用（EP 图标已全量全局注册） |
| `disabled` | 禁用时**不生成 href** 并把 tag 退回 `button`（见 §3 坑 1） |

事件 `navigate`：**仅**纯左键点击时触发（触发前已 `preventDefault`）；未绑定时组件兜底 `router.push(to)`。

### 双路径分流

```js
function onClick(e) {
  if (e.defaultPrevented) return
  // 修饰键 / 非左键：不拦截，交还浏览器 → 新标签
  if (e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return
  e.preventDefault()          // 纯左键：走 SPA
  emit('navigate', e)
}
```

`href` 由 `router.resolve(to).href` 生成（同时覆盖字符串与对象两种 `to`），`try/catch` 兜底：解析失败退化为普通 `<button>`，功能不丢。

## 3. 关键坑位（均已实地取证）

1. **`el-button` 在 `tag !== 'button'` 时 disabled 完全失效** —— `use-button.mjs:54-64` 的 `_props` 直接返回 `{}`，disabled 既不落 DOM 也不落 aria；而 `handleClick`（`:77-81`）在 disabled 时只 `stopPropagation()` 后 return，**不 preventDefault**。结果 `<a href>` + disabled 点下去会**整页刷新跳转**且我们的 handler 收不到事件。
   → 对策：disabled 时 href 返回 `undefined` 且 tag 退回 `button`（"禁用态就不是链接"）。

2. **必须单根节点** —— `v-hasPermi` 靠 `el.parentNode.removeChild(el)`（`directive/permission/hasPermi.ts:21`）删元素，多根节点会导致无权限元素删不掉，**权限泄露到界面**。

3. **不要写 `@click.prevent`** —— 一把梭会连 Ctrl/⌘+点击一起拦掉，新标签功能失效。必须在 handler 内按修饰键分支。

4. **不要加 `target="_blank"`** —— 那会让左键也开新标签，破坏现有行为。

5. **`<a>` 的 UA 默认样式不会污染** —— 仓库全局 `assets/styles/index.scss:49-54` 已置 `a { color: inherit; text-decoration: none }`，且 `.el-button`（0,1,0）特异性高于 `a`（0,0,1）。**组件内绝不要再写 color / text-decoration**。

6. **按钮间距不受影响** —— 间距规则是 `button.scss:72-73` 的 `.el-button + .el-button`，按 class 匹配，与标签名无关。

7. **新标签打不到列表页的查询条件缓存** —— 这是预期行为：新标签只是详情页本身。前提是详情/编辑页**必须能只凭 URL 自洽渲染**（本次 9 个页面的目标路由 id/query 全在 URL 里，已确认）。

## 4. 测试策略

本项目**无前端单测框架**（无 vitest/jest），测试基础设施只有 Playwright。故 TDD 红绿走 E2E：`tests/e2e-row-link-new-tab.spec.js`，覆盖项目管理列表与合同管理列表两个代表页面。

四层断言：
1. **DOM 语义**（核心）—— `tagName === 'A'` 且 href 严格等于目标路径，并调后端接口校验该 id 真实存在（防拼错占位值）
2. **左键** —— SPA 跳转不整页重载（点击前在 `window` 打标记，跳转后验证标记存活）
3. **Ctrl/⌘+点击** —— 开新标签 **且当前页 URL 不变**（防"既开新标签又跳走当前页"）
4. **中键** —— 同上

### E2E 踩坑

- **新标签初始 URL 是 `about:blank`**，导航随后发生。`waitForLoadState('domcontentloaded')` 会在 about:blank 阶段**立刻满足**，此时读 `page.url()` 拿到 `about:blank` → 假失败。必须以「URL 落到目标路径」为等待条件（`waitForNewTabUrl()` 辅助函数）。
- 记录 id **不从操作列自身取**，而从名称列既有链接的 href 取，避免自证循环。

## 5. BDD 场景

场景文件 [bdd/row-link-new-tab.feature](./bdd/row-link-new-tab.feature)（16 场景 + 1 场景大纲 5 例），
覆盖矩阵 [bdd/coverage.md](./bdd/coverage.md)。本项目无 Cucumber 工具链，场景经 Playwright 落地。

覆盖小结：✅ 自动化 11 ／ 🔶 部分 3 ／ ⬜ 未自动化 3（均附理由与替代证据）。

⌘+点击、中键、左键 SPA、查询条件保留、弹窗页隔离、安全边界为完全自动化覆盖。

**右键菜单**本身读不到（属浏览器 UI，不在 DOM 内；曾试图有头右键后用 `screencapture` 截原生菜单，
但 Playwright 的独立 Chrome 在另一个 macOS Space，只截到桌面），故改为把它**归约成两个充要条件**
并各自用例化：① 目标是带 href 的 `<a>`；② `contextmenu` 未被 `preventDefault`。
条件 ② 是实打实的风险点——`el-table` 或任何全局指令一旦拦截 contextmenu，右键能力直接归零，
而此前从未验证过。仅菜单文案呈现留待人工秒验。

未自动化的 3 项分属「无权限账号造数成本过高」「路由解析失败与 disabled 态在真实页面无法触发」。

## 6. 验证结果（实测）

| 项 | 结果 |
|---|---|
| 新增 E2E（**有头 `--headed`**） | **17 passed / 0 skipped / 0 failed** |
| 类型检查 | worktree 39 = 主工作区基线 39，**零新增** |
| 生产构建 | `npm run build:prod` 成功（built in 26.74s） |
| 回归（6 套件） | **55 passed / 3 skipped / 0 failed** |
| 安全 | 10 条恶意载荷实测全部约束在同源 http(s) 地址内，已固化为常驻用例 |

E2E 的 17 个用例分五组：新标签能力 12（两页面 × 6）、回归红线 3、安全边界 1、右键前置条件 1。

### code review 修正项

1. **消除隐式依赖**：`icon` 原声明为 `string | Component`，`Component` 从 `@vue/runtime-core` 导入——
   该包**不在 `ruoyi-ui/package.json`**（只有 `vue: 3.5.26`），属传递依赖。9 个调用点实际只传字符串，
   故收窄为 `icon?: string`，导入一并删除。
2. **修正错误注释**：原注释断言"EP 图标已全量全局注册"却未给出处，核实后补明来源为
   `main.ts:100` 的 `app.use(elementIcons)` → `components/SvgIcon/svgicon.ts` 遍历注册。

### 排障记录（两次假失败 + 一次 flaky）

第一次自测出现 8 个失败，实为 **vite 端口占用后静默顺延**：配置写 5173，日志里一行
`Port 5173 is in use, trying another one...` 后实际起在 5174，而 5173 上另有一个服务主工作区旧代码的
遗留实例，测试连了后者。教训：断言前先确认被测对象是不是自己改的那份。

第二次剩余 5 个失败是**测试自身的竞态**：浏览器新标签初始 URL 为 `about:blank`，
`waitForLoadState('domcontentloaded')` 在那一刻即满足，随后读到的 URL 自然是空白页。
已抽出 `waitForNewTabUrl()`，改以「URL 落到目标路径」为等待条件。

对照实验确认非环境限制：无任何 JS 的原生 `<a href>` 在同一 headless Chromium 中，
⌘+点击与中键点击**均能打开新标签**。

**第三次是真 flaky**：右键用例单独跑绿、整套跑红，失败信息是
`Tearing down "context" exceeded the test timeout` —— **断言全部通过，卡在清理阶段**。

根因：有头模式下真实右键会弹出浏览器**原生上下文菜单**，它是模态的；菜单不关，
浏览器就不再接收后续指令，连关闭 context 都会卡住。而该菜单位于浏览器 UI 层，
`keyboard.press('Escape')` 经 CDP 发往页面，**关不掉它**（已实测无效）。
单独跑之所以"通过"，只是 teardown 恰好赶在 30s 超时前完成，属假绿。

修法：该用例改为**自带 headless 浏览器实例**，不使用外层 `page` fixture。
它验证的是事件层行为（contextmenu 是否被 preventDefault），无头 Chromium
照常派发该事件、只是不渲染原生菜单——想测的一点没少，还避开了模态阻塞，
且对外层 `--headed` 免疫。

修复后**连跑 3 轮**（有头 ×2 + 无头 ×1）均 17/17 通过。

> 顺带一提：这个缺陷在无头模式下**永远不会暴露**（无头不弹菜单）。
> 它是「有头测试有独立价值」的直接例证。
