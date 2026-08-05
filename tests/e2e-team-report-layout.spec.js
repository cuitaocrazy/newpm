/**
 * 项目日报（/dailyReport/teamReport）列宽与布局回归
 *
 * 业务背景：
 *   项目日报按「项目 × 成员 × 日期」展开。查询条件「年月」自 018 起改为非必填——
 *   不填即查全周期，此时动态日期列数量归零。两种口径的宽度诉求完全相反，因此列结构分流。
 *
 * 缺陷现象（改动前实测，1920 视口 / 容器 1720px / 表可用 1680px）：
 *   1. 全周期口径：「项目」是全表唯一弹性列，独吞全部富余宽度被撑到 1254px（占表宽 74.6%），
 *      而内容只需约 620px，右侧近千像素空白。
 *   2. 按月口径：列宽合计 2132px（项目280 + 人员146 + 31×46 + 80+110+90）远超可用 1680px，
 *      01-31 只能看到前 17 天，其余必须横向滚动。
 *      （146 是 HEAD 版本的人员列宽度，用 git show HEAD:...teamReport.vue 可复核）
 *   3. 合并单元格 vertical-align 为 middle，项目信息漂在数百像素高的单元格正中，与首行成员错位。
 *
 * 修复方案（两种口径共用同一套列结构，唯一差别是有没有日期列）：
 *   按月 —— 压缩列宽并收紧单元格内边距，让 31 个日期列一屏可见；
 *   全周期 —— 同样的列结构，只是不渲染日期列。项目列作为唯一弹性列吸收富余，表格铺满。
 *   两者共有：合并单元格顶部对齐、项目单元格内金额横排、人天/金额不折行。
 *
 * 只读用例：不写入任何业务数据。
 */
import { test, expect } from '@playwright/test'

const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:80'
// 项目二组（资源组），父部门为「一部」——本用例的取数部门
const DEPT_ID = 204
const DEPT_KEYWORD = '项目二组'
const PARENT_DEPT = '一部'
// 该部门确实有日报工时的月份 —— 用空月份会让「日期列装得下工时」这条断言无内容可验
const DATA_MONTH = '2026-07'

/** 用 API 拿 token，直接注入 cookie，跳过登录页与验证码 */
async function loginAndGoto(page) {
  const resp = await page.request.post(`${BASE_URL}/dev-api/login`, {
    data: { username: 'admin', password: '123456789' }
  })
  const body = await resp.json()
  expect(body.code, `登录失败: ${body.msg}`).toBe(200)

  const url = new URL(BASE_URL)
  await page.context().addCookies([
    { name: 'Admin-Token', value: body.token, domain: url.hostname, path: '/' }
  ])

  // 库身份自检：确认目标部门确实有日报数据，避免连错库时用例「无声通过」
  const probe = await page.request.get(
    `${BASE_URL}/dev-api/project/dailyReport/teamMonthly?deptId=${DEPT_ID}`,
    { headers: { Authorization: `Bearer ${body.token}` } }
  )
  const probeBody = await probe.json()
  expect(probeBody.data?.length, '目标部门无日报数据，请检查后端连的是哪个库').toBeGreaterThan(0)

  await page.goto('/dailyReport/teamReport', { waitUntil: 'networkidle' })
  await page.waitForTimeout(1000)
}

/** 选择项目所属部门（el-tree-select 默认折叠，需先展开父节点） */
async function selectDept(page) {
  await page.locator('.el-form-item:has-text("项目所属部门") .el-select__wrapper').first().click()
  await page.waitForTimeout(600)

  await page.evaluate((parent) => {
    const item = [...document.querySelectorAll('.el-tree .el-select-dropdown__item')]
      .find((i) => i.textContent.trim() === parent)
    const node = item?.closest('.el-tree-node')
    if (node && node.getAttribute('aria-expanded') !== 'true') {
      node.querySelector('.el-tree-node__content .el-tree-node__expand-icon')?.click()
    }
  }, PARENT_DEPT)
  await page.waitForTimeout(500)

  const picked = await page.evaluate((kw) => {
    const item = [...document.querySelectorAll('.el-tree .el-select-dropdown__item')]
      .find((i) => i.textContent.includes(kw))
    if (!item) return null
    item.closest('.el-tree-node__content').click()
    return item.textContent.trim()
  }, DEPT_KEYWORD)
  expect(picked, '未在部门树中找到目标部门').not.toBeNull()
  await page.waitForTimeout(400)
}

/** 填入指定年月（按月口径）。必须选该部门确实有工时的月份。 */
async function setYearMonth(page, ym) {
  const item = page.locator('.el-form-item:has-text("年月")')
  await item.locator('input').first().fill(ym)
  await page.keyboard.press('Enter')
  await page.waitForTimeout(300)
  await expect(item.locator('input').first(), '年月未填入').toHaveValue(ym)
}

/** 清空「年月」→ 全周期口径 */
async function clearYearMonth(page) {
  const item = page.locator('.el-form-item:has-text("年月")')
  await item.locator('.el-input__wrapper').hover()
  const clearIcon = item.locator('.el-input__icon.clear-icon, .el-input__clear').first()
  if (await clearIcon.count()) {
    await clearIcon.click({ force: true })
  } else {
    await item.locator('input').fill('')
    await page.keyboard.press('Enter')
  }
  await page.waitForTimeout(300)
  await expect(item.locator('input').first(), '年月应已清空').toHaveValue('')
}

async function query(page) {
  await page.getByRole('button', { name: /查询/ }).click()
  await page.waitForTimeout(2500)
}

/** 读取表格布局指标 */
async function readLayout(page) {
  return page.evaluate(() => {
    const table = document.querySelector('.el-table')
    const container = document.querySelector('.app-container')
    const headers = [...table.querySelectorAll('.el-table__header th')]
      .filter((th) => th.querySelector('.cell'))
      .map((th) => ({
        label: th.querySelector('.cell').textContent.trim(),
        width: Math.round(th.getBoundingClientRect().width)
      }))
    const firstCell = table.querySelector('.el-table__body td')
    // 横向滚动检测：EP 把表体裹在 el-scrollbar__wrap 里
    const wrap = table.querySelector('.el-table__body-wrapper .el-scrollbar__wrap')
      || table.querySelector('.el-table__body-wrapper')
    return {
      containerWidth: Math.round(container.getBoundingClientRect().width),
      tableWidth: Math.round(table.getBoundingClientRect().width),
      headers,
      columnCount: headers.length,
      columnWidthSum: headers.reduce((a, h) => a + h.width, 0),
      dayColumns: headers.filter((h) => /^\d{1,2}$/.test(h.label)),
      cellVerticalAlign: firstCell ? getComputedStyle(firstCell).verticalAlign : null,
      // 合并正确性：项目首行渲染全部列，非首行只剩「行级」列（项目级列被 rowspan 吃掉）
      rowCellCounts: [...table.querySelectorAll('.el-table__body tr')]
        .map((tr) => tr.querySelectorAll('td').length),
      // 「人员」列每行都必须有自己的单元格 —— 它是行级的，一旦被误判为项目级
      // 就会合并成一格、只显示第一个成员
      memberCellCount: table.querySelectorAll('.el-table__body td.member-cell').length,
      bodyRowCount: table.querySelectorAll('.el-table__body tr').length,
      // 合并是否真的发生：项目级列必须存在 rowspan>1 的单元格。
      // 只看 rowCellCounts 是不够的 —— 合并「完全失效」时每行都是满列数，
      // filter 出来的数组为空，而 [].every() 恒为 true，断言会空真通过。
      projectLevelCellCount: table.querySelectorAll('.el-table__body td.col-project-level').length,
      maxRowspan: Math.max(0, ...[...table.querySelectorAll('.el-table__body td.col-project-level')]
        .map((td) => Number(td.getAttribute('rowspan') || 1))),
      // 日期格内容是否被内边距挤到溢出（承重 CSS：.day-cell 的 padding 覆盖）
      dayCellOverflow: [...table.querySelectorAll('.el-table__body td.day-cell .cell')]
        .filter((c) => c.textContent.trim() && c.scrollWidth > c.clientWidth + 1)
        .map((c) => ({ text: c.textContent.trim(), need: c.scrollWidth, have: c.clientWidth })),
      dayCellsWithText: [...table.querySelectorAll('.el-table__body td.day-cell .cell')]
        .filter((c) => c.textContent.trim()).length,
      // ⚠️ 折行检测必须量「span 自身跨了几行」，不能量单元格高度。
      // 金额/人天是 inline 元素，折行时 getClientRects() 返回多个矩形；
      // 而单元格高度会因为项目名长短、标签换行等无关因素变化，用它做判据必然误报。
      // 同样不能只量 scrollWidth > clientWidth —— Element Plus 的 .cell 默认
      // white-space: normal，内容过宽时折行而非溢出，此时 scrollWidth 恒等于 clientWidth。
      wrappedCells: [...table.querySelectorAll('.el-table__body td')]
        .flatMap((td) => [...td.querySelectorAll('.amount-line, .days-cell')].map((span) => ({
          text: span.textContent.trim(),
          colWidth: Math.round(td.getBoundingClientRect().width),
          lines: span.getClientRects().length
        })))
        .filter((x) => x.lines > 1),
      // 确定性列宽量测：把金额/人天列的文本临时换成边界串再量，量完还原。
      // 不这么做的话，这条断言取决于「当前部门恰好有多大的数值」——
      // 实测 dept 204 最大预算人天 583.00（7 字符，79/79 正好卡满），
      // 于是 80px 与 90px 渲染结果完全相同，列宽被改窄也抓不到（变异测试实证）。
      // 6740.00d 是全库最大 estimated_workload 的渲染形态。
      boundaryProbe: (() => {
        // 人天列上界：全库最大 estimated_workload 6740.00 → 「6740.00d」
        // 金额列上界：近 1 亿 →「99,999,999.00」（库内已有 11,700,000.00 的合同额）
        const out = []
        let runs = 0
        for (const td of table.querySelectorAll('.el-table__body td')) {
          const cell = td.querySelector('.cell')
          if (!cell) continue
          for (const span of td.querySelectorAll('.amount-line, .days-cell')) {
            const isDays = span.classList.contains('days-cell')
            const probe = isDays ? '6740.00d' : '99,999,999.00'
            const original = span.textContent
            span.textContent = probe
            runs++
            // 两条判据缺一不可：跨行数抓「折行」，溢出抓「省略号」
            const wrapped = span.getClientRects().length > 1
            const clipped = cell.scrollWidth > cell.clientWidth + 1
            if (wrapped || clipped) {
              out.push({
                probe, mode: wrapped ? 'wrap' : 'clip',
                colWidth: Math.round(td.getBoundingClientRect().width),
                need: cell.scrollWidth, have: cell.clientWidth
              })
            }
            span.textContent = original
          }
        }
        return { runs, violations: out }
      })(),
      // 人天/金额列是否被截断（预算人天曾因 80px + nowrap 把 1300.00d 截成「1300....」）
      truncatedCells: [...table.querySelectorAll('.el-table__body .cell')]
        .filter((c) => c.textContent.trim() && c.scrollWidth > c.clientWidth + 1)
        .map((c) => ({
          text: c.textContent.trim().slice(0, 20),
          need: c.scrollWidth,
          have: c.clientWidth,
          hasTooltip: c.classList.contains('el-tooltip')
        })),
      hasHorizontalScroll: wrap ? wrap.scrollWidth > wrap.clientWidth + 1 : null,
      pageOverflow: document.documentElement.scrollWidth - document.documentElement.clientWidth
    }
  })
}

test.describe('项目日报 - 查询列表布局', () => {
  // 缺陷只在宽屏暴露。playwright.config 的 devices['Desktop Chrome'] 会把 viewport 压到 1280，
  // 这里显式固定为 1920 —— 两个方案的宽度预算都是按 1920 视口（表可用 1680px）算的。
  test.use({ viewport: { width: 1920, height: 1080 } })

  test('全周期口径：列结构与按月一致，仅去掉日期列', async ({ page }) => {
    await loginAndGoto(page)
    await selectDept(page)
    await clearYearMonth(page)
    await query(page)

    const layout = await readLayout(page)
    console.log('全周期布局:', JSON.stringify(layout))

    expect(layout.dayColumns.length, '不选年月时不应有日期列').toBe(0)

    // 1) 列结构与按月口径完全一致，只是没有日期列
    const labels = layout.headers.map((h) => h.label)
    expect(labels, '全周期应与按月共用同一套列，仅去掉日期列').toEqual([
      '项目', '人员', '个人人天', '项目累计人天', '预算人天'
    ])
    expect(layout.columnCount, '全周期应为 5 列').toBe(5)

    // 2) 表格铺满容器且不横向滚动 —— 项目列作为唯一弹性列吸收全部富余
    expect(layout.hasHorizontalScroll, '全周期不应出现横向滚动').toBe(false)
    expect(layout.columnWidthSum, '列宽之和应恰好等于表格宽度')
      .toBeLessThanOrEqual(layout.tableWidth + 2)

    // 3) 项目列宽度必须与按月口径一致 —— 少了 31 个日期列的宽度不得灌进项目列。
    //    改动前实测：日期列归零后项目列被撑到 1254px（占表宽 74.6%），而内容只需约 324px。
    const projectCol = layout.headers.find((h) => h.label === '项目')
    expect(projectCol.width, '项目列不应吸收日期列腾出的宽度').toBeLessThanOrEqual(360)
    expect(layout.tableWidth, '全周期表格应按内容宽度收缩，不铺满容器')
      .toBeLessThan(layout.containerWidth)

    // 4) 合并单元格顶部对齐
    expect(layout.cellVerticalAlign, '单元格应顶部对齐').toBe('top')

    // 5) 合并正确性 —— 列宽断言看不见这一层。
    //    Element Plus 会把 fixed 列重排到最前，DOM 列序 ≠ 模板书写顺序，
    //    按 columnIndex 推算合并会把「人员」（fixed，实际落在索引 1）误合并成一格。
    expect(layout.memberCellCount, '「人员」是行级列，每一行都必须有自己的单元格')
      .toBe(layout.bodyRowCount)

    //    ⚠️ 必须先断言「合并确实发生了」再断言「没有过度合并」。
    //    只写下面的 followerRows 检查是空真的：spanMethod 整体失效时每行都是 10 列，
    //    filter 出空数组，而 [].every() 恒为 true —— 变异测试实测：把 spanMethod
    //    首行插入 `return` 后（合并彻底消失），旧断言依然全绿。
    expect(layout.maxRowspan, '项目级列必须存在跨行合并，否则每个成员行都会重复整块项目信息')
      .toBeGreaterThan(1)
    const followerRowCount = layout.rowCellCounts.filter((n) => n !== 5).length
    expect(followerRowCount, '多成员项目必须产生「非首行」，否则说明合并没生效')
      .toBeGreaterThan(0)
    //    项目首行渲染全部 5 列；同项目的后续成员行只剩「人员」「个人人天」2 列
    const followerRows = layout.rowCellCounts.filter((n) => n !== 5)
    expect(followerRows.every((n) => n === 2), `非首行应只剩 2 列，实际 ${followerRows.join(',')}`)
      .toBe(true)

    // 6) 没有单元格因为列宽压缩被截断（预算人天曾被 80px + nowrap 截成「1300....」）
    const truncatedNoTooltip = layout.truncatedCells.filter((c) => !c.hasTooltip)
    expect(truncatedNoTooltip.length,
      `以下单元格被截断且无 tooltip 兜底：${JSON.stringify(truncatedNoTooltip)}`).toBe(0)

    // 9) 确定性列宽量测：金额/人天列必须放得下业务上界的渲染形态。
    //    只断言「当前数据没被截断」是空转的 —— 变异测试实证：把预算人天从 90 改回 80，
    //    因为该部门最大值只有 583.00（正好卡满 79px），测试全绿。
    expect(layout.boundaryProbe.runs, '边界探针一次都没跑 —— 断言空转').toBeGreaterThan(0)
    expect(layout.boundaryProbe.violations.length,
      `列装不下业务上界：${JSON.stringify(layout.boundaryProbe.violations)}`).toBe(0)

    // 10) 真实内容不得折行（金额列曾因缺 white-space: nowrap 把「11,700,000.00」
    //     断成「11,700,000.」+「00」两行且右对齐，读起来像两个数）
    expect(layout.wrappedCells.length,
      `以下单元格内容折行：${JSON.stringify(layout.wrappedCells)}`).toBe(0)
  })

  test('按月口径：31 个日期列一屏可见，不横向滚动', async ({ page }) => {
    await loginAndGoto(page)
    await selectDept(page)
    // ⚠️ 必须选一个「该部门确实有工时」的月份。
    // 早先这里用默认当月，实测 713 个日期单元格无一有内容 ——
    // 「30px 装得下工时数字」这条根本没被验证过，只测了空表格的几何。
    await setYearMonth(page, DATA_MONTH)
    await query(page)

    const layout = await readLayout(page)
    console.log('按月布局:', JSON.stringify(layout))

    // 1) 日期列必须全部渲染（一个月至少 28 天）
    expect(layout.dayColumns.length, '选了年月应展开整月日期列').toBeGreaterThanOrEqual(28)

    // 2) 核心诉求：所有列一屏放得下，不需要横向滚动
    expect(layout.hasHorizontalScroll, '按月口径不应再需要横向滚动').toBe(false)
    expect(layout.columnWidthSum, '列宽之和不得超出表格可用宽度')
      .toBeLessThanOrEqual(layout.tableWidth + 2)

    // 3) 日期列压缩后仍要放得下两位数工时（如「11」「7.5」）
    const dayWidth = layout.dayColumns[0].width
    expect(dayWidth, '日期列过窄会放不下两位数工时').toBeGreaterThanOrEqual(26)
    expect(dayWidth, '日期列未被压缩，31 列必然溢出').toBeLessThanOrEqual(34)

    // 4) 页面本身不得横向溢出
    expect(layout.pageOverflow, '页面出现横向溢出').toBeLessThanOrEqual(1)

    // 5) 日期格里真的有工时数字，且没有被内边距挤到溢出。
    //    .day-cell 的 padding 覆盖（12px→2px）是承重 CSS：删掉它内容区从 25px 塌到 5px、
    //    工时数字溢出 16px，而只看列的几何宽度是发现不了的。
    expect(layout.dayCellsWithText, '所选月份必须有工时数据，否则本用例只验证了空表格的几何')
      .toBeGreaterThan(0)
    expect(layout.dayCellOverflow.length,
      `日期格内容溢出：${JSON.stringify(layout.dayCellOverflow.slice(0, 5))}`).toBe(0)

    // 6) 合并正确性：人员列仍是行级，且项目级列确实发生了跨行合并
    expect(layout.memberCellCount, '「人员」是行级列，每一行都必须有自己的单元格')
      .toBe(layout.bodyRowCount)
    expect(layout.maxRowspan, '按月口径的项目列同样必须按项目合并').toBeGreaterThan(1)
    const monthlyFollowers = layout.rowCellCounts.filter((n) => n !== layout.columnCount)
    expect(monthlyFollowers.length, '多成员项目必须产生「非首行」，否则合并没生效')
      .toBeGreaterThan(0)
    //    非首行 = 全列数 − 3 个项目级列（项目 / 项目累计人天 / 预算人天）
    expect(monthlyFollowers.every((n) => n === layout.columnCount - 3),
      `非首行应为 ${layout.columnCount - 3} 列，实际 ${[...new Set(monthlyFollowers)].join(',')}`).toBe(true)

    // 7) 顶对齐在按月口径同样成立（原先只有全周期用例断言它）
    expect(layout.cellVerticalAlign, '单元格应顶部对齐').toBe('top')

    // 8) 人天数值不得被截断（预算人天曾因 80px + nowrap 把「1300.00d」截成「1300....」）
    const truncatedNoTooltip = layout.truncatedCells.filter((c) => !c.hasTooltip)
    expect(truncatedNoTooltip.length,
      `以下单元格被截断且无 tooltip 兜底：${JSON.stringify(truncatedNoTooltip)}`).toBe(0)

    // 9) 确定性列宽量测：金额/人天列必须放得下业务上界的渲染形态。
    //    只断言「当前数据没被截断」是空转的 —— 变异测试实证：把预算人天从 90 改回 80，
    //    因为该部门最大值只有 583.00（正好卡满 79px），测试全绿。
    expect(layout.boundaryProbe.runs, '边界探针一次都没跑 —— 断言空转').toBeGreaterThan(0)
    expect(layout.boundaryProbe.violations.length,
      `列装不下业务上界：${JSON.stringify(layout.boundaryProbe.violations)}`).toBe(0)

    // 10) 真实内容不得折行（金额列曾因缺 white-space: nowrap 把「11,700,000.00」
    //     断成「11,700,000.」+「00」两行且右对齐，读起来像两个数）
    expect(layout.wrappedCells.length,
      `以下单元格内容折行：${JSON.stringify(layout.wrappedCells)}`).toBe(0)
  })

  test('窄屏全周期：列结构不变，不产生横向滚动', async ({ page }) => {
    // 全周期口径只有 5 列、定宽合计 426px，项目列是唯一弹性列 ——
    // 任何视口宽度下表格都铺满且不横向滚动。这条守着「窄屏不出现净回归」。
    await page.setViewportSize({ width: 1440, height: 900 })
    await loginAndGoto(page)
    await selectDept(page)
    await clearYearMonth(page)
    await query(page)

    const layout = await readLayout(page)
    console.log('窄屏全周期布局:', JSON.stringify({
      containerWidth: layout.containerWidth,
      tableWidth: layout.tableWidth,
      columnCount: layout.columnCount,
      labels: layout.headers.map((h) => h.label),
      hasHorizontalScroll: layout.hasHorizontalScroll
    }))

    expect(layout.dayColumns.length, '不选年月时不应有日期列').toBe(0)
    expect(layout.columnCount, '窄屏列结构应与宽屏一致（5 列）').toBe(5)
    // 关键：不横向滚动 —— 与改动前行为一致，无净回归
    expect(layout.hasHorizontalScroll, '窄屏全周期不应出现横向滚动').toBe(false)
    expect(layout.maxRowspan, '项目列仍按项目合并').toBeGreaterThan(1)
  })
})
