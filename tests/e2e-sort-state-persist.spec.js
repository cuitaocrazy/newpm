/**
 * Issue #28 — 合同管理 / 付款里程碑列表：排序状态在应用内路由往返时保留（P1）
 *
 * ── 为什么红测路径是「关闭页签后重进」而不是 Issue 字面写的「进详情再返回」 ──
 * Issue 原文描述的场景（排序 → 进详情/编辑 → 返回 → 排序丢失）在当前构建上复现不出来：
 * RuoYi 的 keep-alive 把列表组件整个缓存住了，SPA 内往返时组件实例根本没被销毁。
 *   - ruoyi-ui/src/layout/components/AppMain.vue:6-8  <keep-alive :include="tagsViewStore.cachedViews">
 *   - ruoyi-ui/src/store/modules/tagsView.ts:49-53     noCache 为假即收录路由名
 *   - SysMenuServiceImpl.java:181                      noCache = ("1" == menu.isCache)
 *   - 本地 / 生产库 sys_menu.is_cache：2186(合同管理)=0、2196(付款里程碑)=0 → 均被缓存
 * 实测（scratchpad/probe2.mjs）：goBack 后表头 class 仍是 descending、数据顺序一致、
 * 连列表请求都不再发。因此照字面写的用例会「必绿」，绿的是 keep-alive 不是新代码。
 *
 * 真正会丢状态、且仍属应用内 SPA 导航（P1 范畴）的路径是**关闭 tagsView 页签后重新进入**：
 * closeSelectedTag → $tab.closePage → delCachedView（tagsView.ts:89-91）销毁缓存实例，
 * 重进时组件全新挂载，queryParams / el-table 内部 sorting state 一并回落默认值
 * （实测 el-table 实例 id 由 el-table_1_column_3 变成 el-table_6_column_71，
 *  请求退化为 ?pageNum=1&pageSize=10，不带任何排序参数）。本套件全部走这条路径。
 *
 * P2（F5 / 整页刷新）明确不在本次范围内。每个用例都用 window.__spaMarker 断言
 * 全程没有发生整页重新加载 —— 一旦标记丢失，说明测的是 P2 而不是 P1。
 *
 * 运行前置：与套件内其他用例一致，需关闭登录验证码（sys.account.captchaEnabled=false）。
 * 本 spec 只读不写业务数据（不新增/修改任何记录），retries 重跑天然幂等。
 */

import { test, expect } from '@playwright/test';

const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:80';

/** 两个被测列表页的全部差异都收在这里 */
const CONTRACT = {
  name: '合同管理',
  tag: '合同管理',                       // sys_menu 2186 menu_name，即 tagsView 页签标题
  path: '/htkx/contract',
  listUrl: '/project/contract/list',
  storageKey: 'contract_search_state',
};

const PAYMENT = {
  name: '付款里程碑',
  tag: '付款里程碑',                     // sys_menu 2196 menu_name
  path: '/htkx/payment',
  listUrl: '/project/payment/listWithContracts',
  storageKey: 'payment_search_state',
};

/** 前端 handleSortChange 拼出的复合排序串（Issue #16 的唯一次级键，必须原样保留） */
const CODE_DESC = 'contract_code desc, c.contract_id';
const CODE_ASC = 'contract_code asc, c.contract_id';
const SIGN_DATE_ASC = 'contract_sign_date asc, c.contract_id';

// ─────────────────────────────────────────────────────────────
// 公共工具
// ─────────────────────────────────────────────────────────────

/** UI 登录（照抄 e2e-contract-code-sort.spec.js:33-43，依赖验证码关闭） */
async function login(page, username = 'admin', password = '123456789') {
  await page.goto(BASE_URL);
  await page.fill('input[placeholder="账号"]', username);
  await page.fill('input[placeholder="密码"]', password);
  await page.locator('button.el-button--primary').click();
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), { timeout: 15000 });
  const cancelBtn = page.locator('.el-message-box .el-button--default');
  if (await cancelBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
    await cancelBtn.click();
  }
}

/**
 * 在页面上打一个只活在当前 document 里的标记。
 * 任何整页重新加载（F5 / location 跳转）都会把它抹掉 —— 用它把 P1（SPA 导航）
 * 与 P2（浏览器刷新）在断言层面区分开。
 */
async function markSpa(page) {
  await page.evaluate(() => { window.__spaMarker = 'issue28'; });
}

async function expectStillSpa(page, hint) {
  const marker = await page.evaluate(() => window.__spaMarker);
  expect(marker,
    `${hint}：window.__spaMarker 丢失，说明途中发生了整页重新加载。` +
    `本用例要验证的是应用内 SPA 导航（P1），整页刷新属 P2 不在本次范围`).toBe('issue28');
}

/** 收集指定接口的请求 URL（已 decode，便于直接断言中文与空格） */
function trackRequests(page, urlFragment) {
  const urls = [];
  page.on('request', r => {
    const u = r.url();
    if (u.includes(urlFragment)) urls.push(decodeURIComponent(u));
  });
  return {
    all: () => urls.slice(),
    last: () => (urls.length ? urls[urls.length - 1] : null),
    count: () => urls.length,
    clear: () => { urls.length = 0; },
  };
}

/** 从 URL 中取单个查询参数值；取不到返回 null（用于断言「参数不存在」） */
function paramOf(url, name) {
  if (!url) return null;
  const m = decodeURIComponent(url).match(new RegExp(`[?&]${name}=([^&]*)`));
  return m ? m[1] : null;
}

/**
 * 取数组型参数。前端 tansParams（src/utils/ruoyi.ts:193-213）把数组序列化成
 * `name[0]=v0&name[1]=v1`，空数组则整个不出现。
 */
function arrayParamsOf(url, name) {
  if (!url) return [];
  return (decodeURIComponent(url).match(new RegExp(`${name}\\[\\d+\\]=[^&]*`, 'g')) || []).sort();
}

async function waitTableReady(page) {
  await page.waitForSelector('.el-table', { timeout: 25000 });
  await page.waitForFunction(
    () => document.querySelectorAll('.el-table__body tr').length > 0,
    undefined,
    { timeout: 25000 }
  );
}

/** 首次进入列表页（整页加载，属于测试入口，不算被测路径） */
async function gotoList(page, target) {
  await page.goto(`${BASE_URL}${target.path}`);
  await waitTableReady(page);
}

const headerOf = (page, text) =>
  page.locator('.el-table th').filter({ hasText: text }).first();

const formItem = (page, label) =>
  page.locator('.el-form-item').filter({ hasText: label }).first();

/**
 * 从表头 th 的 class 中解出列类名 el-table_N_column_M。
 * 【硬要求】读单元格必须用列类名，禁止「表头下标 + row.children[idx]」：
 * 两页的 spanMethod 对合并行返回 {rowspan:0, colspan:0}，Element Plus 直接不渲染这些 <td>
 * （实测同页 td 数 29 → 13），按下标会读到完全不相干的列。
 */
async function columnClassOf(page, headerText) {
  const header = headerOf(page, headerText);
  await expect(header, `表格中应存在「${headerText}」列表头`).toHaveCount(1);
  const cls = (await header.getAttribute('class')) || '';
  const colClass = cls.split(/\s+/).find(c => /^el-table_\d+_column_\d+$/.test(c));
  expect(colClass,
    `未能从「${headerText}」表头 class 中解析出列类名，实际 class="${cls}"`).toBeTruthy();
  return colClass;
}

/** 按列类名读整列文本（含首行合计行） */
async function readColumnRaw(page, headerText) {
  const colClass = await columnClassOf(page, headerText);
  return page.locator(`.el-table__body td.${colClass}`).allTextContents();
}

/**
 * 读一页的数据快照。
 * - codes：合同编号列文本（去掉恒在首行的前端合计行，contract/index.vue:471 `[summary, ...rows]`）
 * - ids：从合同名称列的链接 href 里解出的 contractId，是真正的行身份
 *   （合同编号存在大量并列值，仅靠编号做集合比较会误判）
 */
async function snapshotRows(page) {
  const firstRow = page.locator('.el-table__body tr').first();
  await expect(firstRow, '首行应为前端拼的合计行（快照需跳过它）').toContainText('合计');

  const codes = (await readColumnRaw(page, '合同编号')).slice(1).map(s => s.trim());
  const ids = await page.locator('.el-table__body a.contract-name-link').evaluateAll(
    els => els.map(e => (e.getAttribute('href') || '').split('?')[0].split('/').pop())
  );

  expect(codes.length,
    `合同编号列单元格数（${codes.length}）应与合同名称链接数（${ids.length}）一致；` +
    `不一致说明列类名选错了列`).toBe(ids.length);

  return { codes, ids };
}

/** 点排序表头并等这次列表请求回来 */
async function clickSortHeader(page, target, headerText) {
  const header = headerOf(page, headerText);
  const resp = page.waitForResponse(
    r => r.url().includes(target.listUrl) && r.request().method() === 'GET',
    { timeout: 25000 }
  );
  await header.click();
  await resp;
  return header;
}

/** 点第一条带「详情」入口的数据行，进入详情页（RowLinkButton 渲染成真 <a>，左键走 router.push） */
async function openFirstDetail(page, target) {
  const link = page.locator('.el-table__body a.el-button', { hasText: '详情' }).first();
  await expect(link, `${target.name}列表应至少有一行带「详情」入口（本用例的数据前提）`).toHaveCount(1);
  await link.click();
  await page.waitForURL(u => new URL(u).pathname.startsWith(`${target.path}/detail/`), { timeout: 20000 });
}

/**
 * 关闭 tagsView 上的**列表页**页签 —— 这是本套件的核心动作：
 * 它会 delCachedView 销毁 keep-alive 缓存实例，使列表页在下次进入时真正重建。
 * 当前停留在详情页，关闭的是非激活页签，不会触发跳转（TagsView/index.vue:179-185）。
 *
 * 【为什么按 data-path 定位而不是按标题文本】
 * TagsView 把每个页签渲染成 router-link 并带 `:data-path="tag.path"`（TagsView/index.vue:6）。
 * 按标题子串匹配会误伤：付款详情页的标题是「付款里程碑详情」（router/index.ts:249），
 * 它包含列表页标题「付款里程碑」。实测此刻 tagsView 为
 *   [{text:'首页',dataPath:'/index'},
 *    {text:'付款里程碑',dataPath:'/htkx/payment'},
 *    {text:'付款里程碑详情',dataPath:'/htkx/payment/detail/400'}]
 * filter({hasText:'付款里程碑'}) 命中 2 个，关掉列表页签后另一个仍在，
 * 「已移除」断言便永远不可能成立。data-path 是页签自身的唯一身份，没有这个歧义。
 * （合同页只是碰巧躲过：详情标题「合同详情」不含列表标题「合同管理」。）
 */
async function closeTag(page, target) {
  const tag = page.locator(`.tags-view-item[data-path="${target.path}"]`);
  await expect(tag, `tagsView 中应存在「${target.tag}」列表页签（${target.path}）`).toHaveCount(1);
  await tag.locator('.el-icon-close').click();
  await expect(tag,
    `点击关闭后「${target.tag}」列表页签应从 tagsView 中移除（缓存实例随之销毁）`).toHaveCount(0);
}

/** 点详情页的「返回」按钮（内部就是 router.back()）回到列表，并等重建后的列表请求 */
async function backToList(page, target) {
  const resp = page.waitForResponse(
    r => r.url().includes(target.listUrl) && r.request().method() === 'GET',
    { timeout: 25000 }
  );
  await page.getByRole('button', { name: '返回' }).first().click();
  await page.waitForURL(u => new URL(u).pathname === target.path, { timeout: 20000 });
  await resp;
  await page.waitForSelector('.el-table', { timeout: 25000 });
}

/** 一次完整的「离开 → 关页签 → 返回」，即本 Issue 真正会丢状态的应用内路径 */
async function leaveCloseTagAndReturn(page, target) {
  await openFirstDetail(page, target);
  await closeTag(page, target);
  await backToList(page, target);
}

/**
 * 读某个查询条件控件当前展示出来的文本。
 * EP 2.13 的 select 把已选标签渲染在 .el-select__placeholder 上，未选中时该元素带
 * is-transparent（element-plus/es/components/select/src/select2.mjs 中 nsSelect.is("transparent", !hasModelValue)），
 * 因此跳过 is-transparent 才不会把「请选择部门」这类占位符误当成已选值。
 */
async function filterDisplayText(page, label) {
  return await formItem(page, label).evaluate(el => {
    const parts = [];
    el.querySelectorAll('input').forEach(i => { if (i.value) parts.push(i.value); });
    el.querySelectorAll('.el-select__placeholder, .el-select__selected-item, .el-tag').forEach(s => {
      const t = (s.textContent || '').trim();
      if (t && !s.classList.contains('is-transparent')) parts.push(t);
    });
    return parts.join(' | ');
  });
}

async function expectNoOpenDropdown(page) {
  await expect(page.locator('.el-select-dropdown:visible'),
    '继续操作前下拉面板应已收起').toHaveCount(0);
}

/** 点「查询」并等这次列表请求回来 */
async function runQuery(page, target) {
  const resp = page.waitForResponse(
    r => r.url().includes(target.listUrl) && r.request().method() === 'GET', { timeout: 25000 });
  await page.locator('.el-form button', { hasText: '查询' }).first().click();
  await resp;
  await waitTableReady(page);
}

/** 整串精确匹配。客户简称之间存在包含关系（"软件中心" ⊂ "中国银行软件中心总体部"），子串匹配会选错项 */
const exactText = (s) => new RegExp(`^\\s*${s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`);

/**
 * 从当前列表里取一个真实存在的客户名称，用作后续筛选的取值来源。
 * 跳过前端拼的合计行与空单元格（spanMethod 合并后部分 td 不渲染）。
 */
async function firstCustomerNameInTable(page) {
  const texts = (await readColumnRaw(page, '客户名称')).map(s => s.trim());
  const name = texts.find(t => t && t !== '-' && !t.includes('合计'));
  expect(name,
    `当前列表的「客户名称」列没有可用取值，实际读到：${JSON.stringify(texts)}`).toBeTruthy();
  return name;
}

// ─────────────────────────────────────────────────────────────
// 用例
// ─────────────────────────────────────────────────────────────
test.describe('Issue #28 排序状态在应用内往返时保留', () => {
  test.beforeEach(async ({ page }) => { await login(page); });

  /**
   * 【当前为何失败】两页对 sessionStorage / onBeforeRouteLeave 零使用
   * （`grep -n "sessionStorage\|onBeforeRouteLeave" contract/index.vue payment/index.vue` 退出码 1），
   * 所以在详情页读 contract_search_state 得到 null，第一处断言当场红。
   * 即使跳过该断言：实测关闭页签后重进，组件被重建，发出的请求是
   * `/project/contract/list?pageNum=1&pageSize=10` —— 不带任何排序参数，数据回默认序。
   */
  test('合同管理：排序后关闭页签重进，排序参数与数据顺序都应保持', async ({ page }) => {
    const target = CONTRACT;
    const listReqs = trackRequests(page, target.listUrl);

    await gotoList(page, target);
    await markSpa(page);

    // 点两次 → 降序
    let header = await clickSortHeader(page, target, '合同编号');
    await expect(header, '第一次点击应为升序').toHaveClass(/ascending/);
    header = await clickSortHeader(page, target, '合同编号');
    await expect(header, '第二次点击应为降序').toHaveClass(/descending/);
    await waitTableReady(page);

    expect(paramOf(listReqs.last(), 'orderByColumn'),
      `排序后的列表请求应带复合排序串，实际请求：${listReqs.last()}`).toBe(CODE_DESC);

    const before = await snapshotRows(page);
    expect(before.ids.length, '本用例需要列表中至少有一条合同数据').toBeGreaterThan(0);

    // 进详情 —— 此刻 onBeforeRouteLeave 应把排序状态写入 sessionStorage
    await openFirstDetail(page, target);

    const cached = await page.evaluate(k => sessionStorage.getItem(k), target.storageKey);
    expect(cached,
      `离开列表页时应把状态写入 sessionStorage['${target.storageKey}']；` +
      `当前 contract/index.vue 完全没有 onBeforeRouteLeave 保存逻辑，故为 null`).toBeTruthy();

    const state = JSON.parse(cached);
    expect(state.queryParams?.orderByColumn,
      '缓存必须原样保留含唯一次级键的排序串，不得解析或重拼').toBe(CODE_DESC);
    expect(state.queryParams?.isAsc, '缓存应保留 isAsc').toBe('desc');
    expect(state.sort,
      'orderByColumn 是拼好的 SQL 片段，反推不出 el-table 需要的 {prop, order}，' +
      '必须另存一份 sort 才能恢复表头箭头').toBeTruthy();
    expect(state.sort.prop, 'sort.prop 应为 el-table 的列 prop（驼峰）').toBe('contractCode');
    expect(state.sort.order, 'sort.order 应为 el-table 的 order 取值').toBe('descending');

    // 关页签 → 组件缓存实例被销毁 → 返回时真正重建
    await closeTag(page, target);
    listReqs.clear();
    await backToList(page, target);
    await waitTableReady(page);

    await expectStillSpa(page, '关闭页签后返回列表');

    const rebuildReq = listReqs.last();
    expect(paramOf(rebuildReq, 'orderByColumn'),
      `重建后的列表请求应恢复排序参数，实际请求：${rebuildReq}`).toBe(CODE_DESC);
    expect(paramOf(rebuildReq, 'isAsc'), `isAsc 应一并恢复，实际请求：${rebuildReq}`).toBe('desc');

    const after = await snapshotRows(page);
    expect(after.ids, '重建后数据顺序（按 contractId）应与排序后完全一致').toEqual(before.ids);
    expect(after.codes, '重建后合同编号列顺序应与排序后完全一致').toEqual(before.codes);

    const leftover = await page.evaluate(k => sessionStorage.getItem(k), target.storageKey);
    expect(leftover,
      `恢复后应一次性消费缓存（removeItem），否则旧快照会在后续重建时被再次套用`).toBeNull();
  });

  /**
   * 【当前为何失败】contract/index.vue:132-140 的 el-table 既没有 ref 也没有 :default-sort
   * （`grep -n "default-sort\|defaultSort" contract/index.vue payment/index.vue` 零命中），
   * 没有任何机制能恢复表头箭头。实测关闭页签重进后该 th 的 class 为
   * "el-table_6_column_71 is-center is-leaf is-sortable el-table__cell"，ascending/descending 均已消失。
   * 本用例刻意选「合同签订日期」而不是「合同编号」：合同页有 7 个 sortable 列，
   * 恢复逻辑不能只对合同编号写死。
   */
  test('合同管理：非默认列（合同签订日期）的排序箭头与后端排序一起恢复', async ({ page }) => {
    const target = CONTRACT;
    const listReqs = trackRequests(page, target.listUrl);

    await gotoList(page, target);
    await markSpa(page);

    const dateHeader = await clickSortHeader(page, target, '合同签订日期');
    await expect(dateHeader, '点一次应为升序').toHaveClass(/ascending/);
    await waitTableReady(page);
    expect(paramOf(listReqs.last(), 'orderByColumn'),
      `实际请求：${listReqs.last()}`).toBe(SIGN_DATE_ASC);

    await leaveCloseTagAndReturn(page, target);
    await waitTableReady(page);
    await expectStillSpa(page, '关闭页签后返回列表');

    // 箭头与后端排序必须一起断：只断数据顺序会漏掉「箭头没恢复」，
    // 只断箭头会漏掉「箭头是假的、后端并没按它排」
    await expect(headerOf(page, '合同签订日期'),
      '重建后「合同签订日期」表头应恢复升序箭头').toHaveClass(/ascending/);
    await expect(headerOf(page, '合同编号'),
      '未被排序的「合同编号」表头不应带任何排序箭头').not.toHaveClass(/ascending|descending/);
    expect(paramOf(listReqs.last(), 'orderByColumn'),
      `重建后的列表请求应仍按签订日期升序，实际请求：${listReqs.last()}`).toBe(SIGN_DATE_ASC);
  });

  /**
   * 【当前为何失败】关闭页签后组件全新挂载：showMoreSearch 回到 ref(false)（payment/index.vue:355）、
   * actualPaymentDateRange 回到 ref([])（payment/index.vue:379）、queryParams 回到字面量默认值。
   * 更关键的是——即使有人只缓存 queryParams 也救不了日期条件：getList() 第 433-439 行
   * 每次都用 actualPaymentDateRange 反向覆写 actualPaymentDateStart/End，ref 为空就写成 null，
   * 还原后第一次 getList 会当场把日期条件抹掉。
   */
  test('付款里程碑：「更多」区筛选 + 实际回款日期范围 + 排序 一并恢复且展开态可见', async ({ page }) => {
    const target = PAYMENT;
    const listReqs = trackRequests(page, target.listUrl);

    await gotoList(page, target);
    await markSpa(page);

    // 展开「更多」
    await page.locator('.el-form button', { hasText: '更多' }).first().click();
    await expect(formItem(page, '实际回款日期'), '点「更多」后折叠区应展开').toBeVisible();

    // 选两个「实际回款季度」（取字典前两项，不硬编码字典值）
    await formItem(page, '实际回款季度').locator('.el-select__wrapper').click();
    const quarterOptions = page.locator('.el-select-dropdown:visible .el-select-dropdown__item');
    await expect(quarterOptions.first(), '实际回款季度下拉应能打开').toBeVisible();
    expect(await quarterOptions.count(),
      '本用例需要字典 sys_jdgl 至少有 2 个选项').toBeGreaterThanOrEqual(2);
    const quarterLabels = [
      (await quarterOptions.nth(0).innerText()).trim(),
      (await quarterOptions.nth(1).innerText()).trim(),
    ];
    await quarterOptions.nth(0).click();
    await quarterOptions.nth(1).click();
    await page.keyboard.press('Escape');
    await expectNoOpenDropdown(page);

    // 填「实际回款日期」范围（value-format YYYY-MM-DD）
    const rangeInputs = formItem(page, '实际回款日期').locator('.el-range-input');
    await rangeInputs.nth(0).fill('2000-01-01');
    await rangeInputs.nth(0).press('Enter');
    await rangeInputs.nth(1).fill('2099-12-31');
    await rangeInputs.nth(1).press('Enter');
    await page.keyboard.press('Escape');

    // 查询
    const queryResp = page.waitForResponse(
      r => r.url().includes(target.listUrl) && r.request().method() === 'GET', { timeout: 25000 });
    await page.locator('.el-form button', { hasText: '查询' }).first().click();
    await queryResp;
    await waitTableReady(page);

    // 排序（降序）
    let header = await clickSortHeader(page, target, '合同编号');
    await expect(header).toHaveClass(/ascending/);
    header = await clickSortHeader(page, target, '合同编号');
    await expect(header).toHaveClass(/descending/);
    await waitTableReady(page);

    const beforeReq = listReqs.last();
    expect(paramOf(beforeReq, 'orderByColumn'), `实际请求：${beforeReq}`).toBe(CODE_DESC);
    expect(paramOf(beforeReq, 'actualPaymentDateStart'), `实际请求：${beforeReq}`).toBe('2000-01-01');
    expect(paramOf(beforeReq, 'actualPaymentDateEnd'), `实际请求：${beforeReq}`).toBe('2099-12-31');
    const beforeQuarters = arrayParamsOf(beforeReq, 'actualQuarters');
    expect(beforeQuarters.length,
      `实际回款季度应作为数组参数上行，实际请求：${beforeReq}`).toBe(2);

    expect(await page.locator('.el-table__body a.el-button', { hasText: '详情' }).count(),
      '本用例需要至少一条「实际回款日期在 2000-01-01 ~ 2099-12-31」的付款记录作为数据前提')
      .toBeGreaterThan(0);

    await leaveCloseTagAndReturn(page, target);
    await waitTableReady(page);
    await expectStillSpa(page, '关闭页签后返回付款里程碑列表');

    // ① 展开态必须一起恢复，否则「结果被筛过、界面上却看不到任何筛选条件」
    await expect(formItem(page, '实际回款日期'),
      '「更多」区有 5 个条件藏在 v-if 里，恢复了值却不恢复展开态等于筛选条件不可见').toBeVisible();

    // ② 控件上要看得见原来的值
    const restoredRange = formItem(page, '实际回款日期').locator('.el-range-input');
    await expect(restoredRange.nth(0), '实际回款日期起始应恢复').toHaveValue('2000-01-01');
    await expect(restoredRange.nth(1), '实际回款日期截止应恢复').toHaveValue('2099-12-31');
    const quarterText = await filterDisplayText(page, '实际回款季度');
    expect(quarterText,
      `实际回款季度应恢复为已选状态，当前控件展示："${quarterText}"`).toContain(quarterLabels[0]);

    // ③ 重建后的请求要同时带筛选与排序
    const afterReq = listReqs.last();
    expect(paramOf(afterReq, 'actualPaymentDateStart'),
      `日期条件必须恢复；只还原 queryParams 而不还原 actualPaymentDateRange 时，` +
      `getList() 会把它反写成 null。实际请求：${afterReq}`).toBe('2000-01-01');
    expect(paramOf(afterReq, 'actualPaymentDateEnd'), `实际请求：${afterReq}`).toBe('2099-12-31');
    expect(arrayParamsOf(afterReq, 'actualQuarters'),
      `实际回款季度应原样恢复。实际请求：${afterReq}`).toEqual(beforeQuarters);
    expect(paramOf(afterReq, 'orderByColumn'),
      `排序应一并恢复。实际请求：${afterReq}`).toBe(CODE_DESC);

    // ④ 表头箭头
    await expect(headerOf(page, '合同编号'),
      '重建后「合同编号」表头应恢复降序箭头').toHaveClass(/descending/);
  });

  /**
   * 【当前为何失败】关闭页签后重进，queryParams.deptId / customerId 回到 null，
   * 两个控件都显示 placeholder，请求里也没有 deptId / customerId。
   *
   * 本用例同时正面验证「本页异步下拉 options 无需入缓存」这条判定：
   * contract/index.vue:910-912 的 getDeptTree() / getCustomerList() 在 setup 顶层无条件执行，
   * 每次组件创建都会重新拉取（不同于 subproject 的条件加载）。若该判定错了，
   * 恢复的 id 会因为 options 为空而渲染成空白或裸 id，断言①就会红。
   */
  test('合同管理：异步下拉（部门 / 客户）的筛选值恢复后仍显示为名称', async ({ page }) => {
    const target = CONTRACT;
    const listReqs = trackRequests(page, target.listUrl);

    await gotoList(page, target);
    await markSpa(page);

    // 选部门（el-tree-select，选项来自异步 getDeptTree）
    // 节点选择器不能用 el-tree 默认的 .el-tree-node__label：el-tree-select 用自己的
    // renderContent 覆写了节点内容（element-plus/es/components/tree-select/src/tree.mjs:100-111），
    // 渲染出来的是 ElOption 派生组件。实测该下拉展开时全页 .el-tree-node__label 计数为 0，
    // 真实 DOM 是 <div class="el-tree-node__content">…<li class="el-select-dropdown__item"><span>一部</span></li></div>
    await formItem(page, '合同所属部门').locator('.el-select__wrapper').click();
    const deptNodes = page.locator('.el-tree-node__content .el-select-dropdown__item:visible');
    await expect(deptNodes.first(), '部门树下拉应能打开且有可选节点').toBeVisible();
    const deptName = (await deptNodes.first().innerText()).trim();
    await deptNodes.first().click();
    await page.keyboard.press('Escape');
    await expectNoOpenDropdown(page);

    // 先只按部门查一次，从结果里取一个真实客户 —— 不能盲取客户下拉的第一项：
    // 两个筛选是「与」关系，任取一个客户极可能与该部门无交集，查出空列表，
    // 后面所有断言就都因为「没有数据」而失败，测不到本用例真正要验的恢复行为。
    // 实测：deptId=103（一部）单独筛选 total=109；叠加客户下拉首项 customerId=49
    // （中总行-个金-客户权益）后 total=0，表体一行不剩。
    await runQuery(page, target);
    const customerName = await firstCustomerNameInTable(page);

    // 选客户（el-select，选项来自异步 listAllCustomer）
    await formItem(page, '关联客户').locator('.el-select__wrapper').click();
    const customerOptions = page.locator('.el-select-dropdown:visible .el-select-dropdown__item');
    await expect(customerOptions.first(), '关联客户下拉应能打开且有选项').toBeVisible();
    const customerOption = customerOptions.filter({ hasText: exactText(customerName) });
    expect(await customerOption.count(),
      `列表里出现的客户「${customerName}」应能在关联客户下拉中找到同名选项`).toBeGreaterThan(0);
    await customerOption.first().click();
    await page.keyboard.press('Escape');
    await expectNoOpenDropdown(page);

    await runQuery(page, target);

    // 排序（降序）
    await clickSortHeader(page, target, '合同编号');
    const header = await clickSortHeader(page, target, '合同编号');
    await expect(header).toHaveClass(/descending/);
    await waitTableReady(page);

    const beforeReq = listReqs.last();
    const deptId = paramOf(beforeReq, 'deptId');
    const customerId = paramOf(beforeReq, 'customerId');
    expect(deptId, `筛选后的请求应带 deptId，实际请求：${beforeReq}`).toBeTruthy();
    expect(customerId, `筛选后的请求应带 customerId，实际请求：${beforeReq}`).toBeTruthy();

    await leaveCloseTagAndReturn(page, target);
    await page.waitForSelector('.el-table', { timeout: 25000 });
    await expectStillSpa(page, '关闭页签后返回合同列表');

    // ① 控件上必须显示成名称，而不是空 placeholder，也不是裸 id
    const deptText = await filterDisplayText(page, '合同所属部门');
    expect(deptText,
      `部门筛选应恢复并显示为部门名称「${deptName}」，当前控件展示："${deptText}"`).toContain(deptName);
    const customerText = await filterDisplayText(page, '关联客户');
    expect(customerText,
      `客户筛选应恢复并显示为客户简称「${customerName}」，当前控件展示："${customerText}"`)
      .toContain(customerName);

    // ② 重建后的请求要带回同样的 id 与排序
    const afterReq = listReqs.last();
    expect(paramOf(afterReq, 'deptId'), `实际请求：${afterReq}`).toBe(deptId);
    expect(paramOf(afterReq, 'customerId'), `实际请求：${afterReq}`).toBe(customerId);
    expect(paramOf(afterReq, 'orderByColumn'), `实际请求：${afterReq}`).toBe(CODE_DESC);

    // ③ 表头箭头
    await expect(headerOf(page, '合同编号'),
      '重建后「合同编号」表头应恢复降序箭头').toHaveClass(/descending/);
  });

  /**
   * 【当前为何失败】实测：点「重置」后合同页请求仍是
   * `/project/contract/list?pageNum=1&pageSize=10&orderByColumn=contract_code desc, c.contract_id&isAsc=desc`，
   * 表头仍是 descending；付款页 listWithContracts 同样仍带 orderByColumn、表头仍 descending。
   * 根因：resetQuery（contract/index.vue:731-734 / payment/index.vue:619-623）只调
   * `proxy.resetForm("queryRef")`，而 orderByColumn / isAsc 根本不是 el-form 的 prop 字段，
   * 且清 queryParams 也清不掉 el-table 组件内部的 sorting state（需要 clearSort()）。
   * 第三条断言另外守住「重置要顺手清掉 sessionStorage 缓存」，否则会出现
   * 「点了重置，下次重建又把旧排序恢复回来」。
   */
  for (const target of [CONTRACT, PAYMENT]) {
    test(`${target.name}：点「重置」后不残留旧排序（参数 / 箭头 / 缓存三清）`, async ({ page }) => {
      const listReqs = trackRequests(page, target.listUrl);

      await gotoList(page, target);
      await markSpa(page);

      await clickSortHeader(page, target, '合同编号');
      const header = await clickSortHeader(page, target, '合同编号');
      await expect(header, '点两次应为降序').toHaveClass(/descending/);
      await waitTableReady(page);
      expect(paramOf(listReqs.last(), 'orderByColumn'),
        `重置前请求应带排序参数，实际请求：${listReqs.last()}`).toBe(CODE_DESC);

      // 预置一条缓存，模拟「此前离开过页面写下的快照」，用于验证重置会清掉它
      await page.evaluate(k => sessionStorage.setItem(k, '{"queryParams":{}}'), target.storageKey);

      const resetResp = page.waitForResponse(
        r => r.url().includes(target.listUrl) && r.request().method() === 'GET', { timeout: 25000 });
      listReqs.clear();
      await page.locator('.el-form button', { hasText: '重置' }).first().click();
      await resetResp;

      const afterReset = listReqs.last();
      // ① 参数
      expect(paramOf(afterReset, 'orderByColumn'),
        `重置后请求不应再带 orderByColumn，实际请求：${afterReset}`).toBeNull();
      expect(paramOf(afterReset, 'isAsc'),
        `重置后请求不应再带 isAsc，实际请求：${afterReset}`).toBeNull();

      // ② 表头箭头（只清 queryParams 不调 clearSort() 的话，箭头会留在原地）
      await expect(headerOf(page, '合同编号'),
        '重置后「合同编号」表头不应残留任何排序箭头').not.toHaveClass(/ascending|descending/);

      // ③ 缓存
      const leftover = await page.evaluate(k => sessionStorage.getItem(k), target.storageKey);
      expect(leftover,
        `重置应清掉 sessionStorage['${target.storageKey}']，否则下次重建会把旧排序恢复回来`)
        .toBeNull();

      await expectStillSpa(page, '重置操作');
    });
  }

  /**
   * 【当前为何失败】第一处断言就红：关闭页签重进后请求根本不含 orderByColumn（实测 URL 为
   * `?pageNum=1&pageSize=10`）。
   * 这条用例是防「实现时图省事去解析 / 重建 orderByColumn 字符串而丢掉次级键 c.contract_id」——
   * contract_code 在该库有 143 行并列值，一旦丢了次级键，MySQL 对并列行顺序不保证、
   * 每页 limit 独立排序，第 1、2 页就会重复与遗漏（Issue #16 的原始缺陷）。
   */
  test('合同管理：恢复排序后翻页仍不重复不遗漏（Issue #16 唯一次级键回归）', async ({ page }) => {
    const target = CONTRACT;
    const listReqs = trackRequests(page, target.listUrl);

    await gotoList(page, target);
    await markSpa(page);

    await clickSortHeader(page, target, '合同编号');
    const header = await clickSortHeader(page, target, '合同编号');
    await expect(header).toHaveClass(/descending/);
    await waitTableReady(page);

    await leaveCloseTagAndReturn(page, target);
    await waitTableReady(page);
    await expectStillSpa(page, '关闭页签后返回合同列表');

    // ① 恢复出来的排序串必须一字不差（含次级键），不得被解析后重拼
    expect(paramOf(listReqs.last(), 'orderByColumn'),
      `恢复后的排序串必须精确等于 "${CODE_DESC}"，实际请求：${listReqs.last()}`).toBe(CODE_DESC);

    const page1 = await snapshotRows(page);

    const nextBtn = page.locator('.el-pagination button.btn-next');
    await expect(nextBtn, '本用例需要至少两页数据').toBeEnabled();
    const pageResp = page.waitForResponse(
      r => r.url().includes(target.listUrl) && r.request().method() === 'GET', { timeout: 25000 });
    await nextBtn.click();
    await pageResp;
    await waitTableReady(page);

    const pageTwoReq = listReqs.last();
    expect(paramOf(pageTwoReq, 'pageNum'), `实际请求：${pageTwoReq}`).toBe('2');
    expect(paramOf(pageTwoReq, 'orderByColumn'),
      `翻页请求的排序串同样必须完整带次级键，实际请求：${pageTwoReq}`).toBe(CODE_DESC);

    const page2 = await snapshotRows(page);
    const overlap = page2.ids.filter(id => page1.ids.includes(id));
    expect(overlap,
      `第 1、2 页出现重复合同（contractId：${overlap}）——排序键不唯一会同时造成重复与遗漏`)
      .toHaveLength(0);
  });

  /**
   * 【当前为何失败】关闭页签重进后 queryParams 全部回落默认值：pageNum=1、无排序，
   * 断言①③直接红。
   * 断言②是长期防线：若将来有人用 `tableRef.sort(prop, order)` 去恢复箭头，它会 emit
   * sort-change（element-plus/es/components/table/src/store/index.mjs:145 的守卫只排除
   * silent/init，而 utils-helper.mjs:27-29 的 sort() 两者都不带），进而触发
   * handleSortChange → handleQuery() → pageNum 被打回 1 且多发一次列表请求，
   * 届时①②会同时红。正确做法是 :default-sort（走 init:true 分支，不 emit）。
   */
  test('合同管理：恢复过程只发一次列表请求，且页码不被打回第 1 页', async ({ page }) => {
    const target = CONTRACT;
    const listReqs = trackRequests(page, target.listUrl);

    await gotoList(page, target);
    await markSpa(page);

    await clickSortHeader(page, target, '合同编号');
    const header = await clickSortHeader(page, target, '合同编号');
    await expect(header).toHaveClass(/descending/);
    await waitTableReady(page);

    // 翻到第 2 页（handleSortChange → handleQuery 已把 pageNum 置 1，这里再手动翻到 2）
    const nextBtn = page.locator('.el-pagination button.btn-next');
    await expect(nextBtn, '本用例需要至少两页数据').toBeEnabled();
    const pageResp = page.waitForResponse(
      r => r.url().includes(target.listUrl) && r.request().method() === 'GET', { timeout: 25000 });
    await nextBtn.click();
    await pageResp;
    await waitTableReady(page);
    expect(paramOf(listReqs.last(), 'pageNum'),
      `翻页后应停在第 2 页，实际请求：${listReqs.last()}`).toBe('2');

    await openFirstDetail(page, target);
    await closeTag(page, target);

    // 只统计「重建过程」中的列表请求
    listReqs.clear();
    await backToList(page, target);
    await waitTableReady(page);
    // 留出窗口，捕捉恢复逻辑可能多发的第二次请求
    await page.waitForTimeout(1500);

    await expectStillSpa(page, '关闭页签后返回合同列表');

    // ① 页码不被打回第 1 页
    const rebuildReq = listReqs.last();
    expect(paramOf(rebuildReq, 'pageNum'),
      `重建后应恢复到第 2 页，实际请求：${rebuildReq}`).toBe('2');
    await expect(page.locator('.el-pagination .el-pager li.is-active'),
      '分页器应高亮第 2 页').toHaveText('2');

    // ② 重建过程中只应发一次列表请求
    expect(listReqs.count(),
      `重建过程中 ${target.listUrl} 应恰好请求 1 次，实际 ${listReqs.count()} 次：\n` +
      listReqs.all().join('\n')).toBe(1);

    // ③ 表头箭头
    await expect(headerOf(page, '合同编号'),
      '重建后「合同编号」表头应恢复降序箭头').toHaveClass(/descending/);
  });
});

/**
 * 登出清理搜索状态缓存。
 *
 * 快照存在 sessionStorage（生命周期 = 浏览器标签页），退出登录本身不会清掉它。
 * 同一标签页里换人登录后，新用户首次打开列表会静默套用上一个会话的筛选条件——
 * 数据有后端 @DataScope 兜底不越权，但界面上出现自己没设过的筛选值，
 * 极易被误判成「我的数据不见了」。
 *
 * 清理逻辑统一放在 store/modules/user.ts 的 logOut() 里（按 _search_state 后缀批量删），
 * 覆盖全部 8 个采用该范式的列表页，故这里同时校验其他页面的键也被清掉。
 */
test.describe('Issue #28 登出清理搜索状态缓存', () => {
  test('退出登录会清掉所有 *_search_state 快照，换人登录不残留上个会话的筛选', async ({ page }) => {
    await login(page);

    // 造一个真实快照：进合同列表 → 排序 → 离开（触发 onBeforeRouteLeave 落盘）
    await gotoList(page, CONTRACT);
    await clickSortHeader(page, CONTRACT, '合同编号');
    await openFirstDetail(page, CONTRACT);

    const saved = await page.evaluate(k => sessionStorage.getItem(k), CONTRACT.storageKey);
    expect(saved, '前置条件：离开列表页后应已写入快照，否则本用例验不到清理效果').not.toBeNull();

    // 再塞两个别的页面的键，验证清理是按后缀批量而非只删合同页
    await page.evaluate(() => {
      sessionStorage.setItem('task_search_state', '{"queryParams":{}}');
      sessionStorage.setItem('version_out_search_state', '{"queryParams":{}}');
      sessionStorage.setItem('unrelated_key', 'keep-me');   // 不该被误删
    });

    // 走 UI 退出登录
    await page.locator('.avatar-container').click();
    await page.locator('.el-dropdown-menu__item', { hasText: '退出登录' }).click();
    const confirmBtn = page.locator('.el-message-box__btns .el-button--primary');
    if (await confirmBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await confirmBtn.click();
    }
    await page.waitForFunction(() => location.pathname.startsWith('/login'), { timeout: 20000 });

    const after = await page.evaluate(() => ({
      contract: sessionStorage.getItem('contract_search_state'),
      payment: sessionStorage.getItem('payment_search_state'),
      task: sessionStorage.getItem('task_search_state'),
      versionOut: sessionStorage.getItem('version_out_search_state'),
      unrelated: sessionStorage.getItem('unrelated_key'),
    }));

    expect(after.contract, '登出后合同页快照应被清除').toBeNull();
    expect(after.task, '登出后应按 _search_state 后缀批量清除，任务页快照也要清掉').toBeNull();
    expect(after.versionOut, '登出后出入库版本页快照也要清掉').toBeNull();
    expect(after.unrelated, '不带 _search_state 后缀的键不得被误删').toBe('keep-me');

    // 重新登录后进列表，确认不再套用上个会话的排序
    await login(page);
    const listReqs = trackRequests(page, CONTRACT.listUrl);
    await gotoList(page, CONTRACT);
    const firstReq = listReqs.last();
    expect(paramOf(firstReq, 'orderByColumn'),
      `换人登录后首次进入列表不应携带上个会话的排序，实际请求：${firstReq}`).toBeNull();
  });
});
