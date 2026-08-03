/**
 * 操作列「详情 / 编辑」可右键新标签打开 —— E2E 验证
 *
 * 业务背景：
 *   列表页操作列原本是 <el-button @click="router.push(...)">，渲染成 <button>，
 *   没有 href。用户无法右键「在新标签页中打开」、也无法 Ctrl/⌘+点击 或中键点击开新标签，
 *   只能一条条来回跳，效率低。
 *
 * 本次改造的本质：把操作列的「详情」「编辑」由 <button> 换成带 href 的真 <a>，
 *   同时保持普通左键仍走 SPA 路由（不整页重载）。
 *
 * 因此本套件的断言分四层：
 *   1. DOM 语义   —— tagName === 'A' 且 href 指向正确的路由路径（核心，缺了这条改造就没意义）
 *   2. 左键       —— SPA 跳转，页面不重载（用 window 上的标记存活与否来判定）
 *   3. Ctrl/⌘+点击 —— 开新标签，且**当前页 URL 不变**（不能既开新标签又把当前页跳走）
 *   4. 中键点击   —— 开新标签，同上
 *
 * 覆盖页面（代表性抽样，非全量 9 页）：
 *   - 项目管理列表 /project/list        → /project/list/detail|edit/{projectId}
 *   - 合同管理列表 /htkx/contract       → /htkx/contract/detail|edit/{contractId}
 *
 * 运行前提（与套件内其他用例一致）：
 *   - 前端 :80 / 后端 :8085 均已启动
 *   - 登录验证码已临时关闭（sys.account.captchaEnabled=false）
 *   - 路由为 createWebHistory()（history 模式），href 才是可直接打开的真实路径
 */

import { test, expect, chromium } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:80';

/** macOS 用 Meta（⌘），其余平台用 Control */
const NEW_TAB_MODIFIER = process.platform === 'darwin' ? 'Meta' : 'Control';

/**
 * 等待新标签导航到目标路径。
 *
 * 坑：浏览器新开的标签初始 URL 是 about:blank，导航是随后发生的。
 * 此时 waitForLoadState('domcontentloaded') 会**立刻**满足（about:blank 本身已加载完），
 * 紧接着读 page.url() 拿到的就是 'about:blank' 而非目标路径，断言必假失败。
 * 因此必须以「URL 落到目标路径」为等待条件，而不是以加载状态为条件。
 */
async function waitForNewTabUrl(newPage, expectedPath, timeout = 15000) {
  await newPage.waitForURL(u => new URL(u).pathname === expectedPath, { timeout });
}

// ─────────────────────────────────────────────────────────────
// 公共工具
// ─────────────────────────────────────────────────────────────

/** UI 登录（与 contract-filter.spec.js 约定一致，依赖验证码关闭） */
async function login(page, username = 'admin', password = '123456789') {
  await page.goto(BASE_URL);
  await page.fill('input[placeholder="账号"]', username);
  await page.fill('input[placeholder="密码"]', password);
  await page.locator('button.el-button--primary').click();
  await page.waitForFunction(() => !location.pathname.startsWith('/login'), { timeout: 15000 });
  // 首登可能弹「修改密码」提示框，关掉以免遮挡表格
  const cancelBtn = page.locator('.el-message-box .el-button--default');
  if (await cancelBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
    await cancelBtn.click();
  }
}

/** 打开列表页并等待列表接口返回 + 表格渲染出至少一行 */
async function openList(page, cfg) {
  const listResp = page.waitForResponse(
    res => res.url().includes(cfg.listApiPath) && res.request().method() === 'GET',
    { timeout: 20000 }
  );
  await page.goto(`${BASE_URL}${cfg.listPath}`);
  await listResp;
  const rows = mainTableRows(page);
  await expect(rows.first(), `${cfg.name}应至少渲染一行数据`).toBeVisible({ timeout: 20000 });
}

/** 主表格（排除对话框内嵌套表格）的数据行 */
function mainTableRows(page) {
  // .first() 取页面主表格；附件/明细对话框里的表格排在其后，不会被选中
  return page.locator('.app-container .el-table').first()
    .locator('.el-table__body-wrapper tbody tr');
}

/**
 * 操作列单元格：列定义里 class-name="small-padding fixed-width"，
 * Element Plus 会把它加到该列的 <td> 上。
 */
function actionCell(row) {
  return row.locator('td.fixed-width').first();
}

/**
 * 操作列中承载指定文案的**可点击元素**。
 * 故意同时匹配 a 与 button —— 改造前是 button，改造后是 a，
 * 这样 tagName 断言能给出「实际是 BUTTON」的明确红，而不是定位不到元素的超时。
 */
function actionEl(row, label) {
  return actionCell(row).locator(`a:has-text("${label}"), button:has-text("${label}")`).first();
}

/** 在表格中找到第一行「操作列同时包含 labels 中所有文案」的数据行；找不到返回 null */
async function findRowWithActions(page, labels) {
  const rows = mainTableRows(page);
  const count = await rows.count();
  for (let i = 0; i < count; i++) {
    const row = rows.nth(i);
    const cell = actionCell(row);
    if ((await cell.count()) === 0) continue; // 合计行没有操作列内容
    let matched = true;
    for (const label of labels) {
      if ((await actionEl(row, label).count()) === 0) { matched = false; break; }
    }
    if (matched) return row;
  }
  return null;
}

/** 从本行「名称列」的链接 href 中取出这条记录的主键（与操作列彼此独立，避免自证循环） */
async function readRowRecordId(row, cfg) {
  const nameLink = row.locator(cfg.nameLinkSelector).first();
  if ((await nameLink.count()) === 0) return null;
  const href = await nameLink.getAttribute('href');
  const m = /(\d+)\s*$/.exec(href || '');
  return m ? m[1] : null;
}

// ─────────────────────────────────────────────────────────────
// 两个代表页面的配置
// ─────────────────────────────────────────────────────────────
const PAGES = [
  {
    name: '项目管理列表',
    listPath: '/project/list',
    listApiPath: '/project/project/list',
    // 项目名称列的链接（详情列之外的独立取 id 来源）
    nameLinkSelector: '.project-name-cell a',
    detailPath: id => `/project/list/detail/${id}`,
    editPath: id => `/project/list/edit/${id}`,
    recordApiPath: id => `/project/project/${id}`,
    idField: 'projectId'
  },
  {
    name: '合同管理列表',
    listPath: '/htkx/contract',
    listApiPath: '/project/contract/list',
    nameLinkSelector: '.contract-name-cell a',
    detailPath: id => `/htkx/contract/detail/${id}`,
    editPath: id => `/htkx/contract/edit/${id}`,
    recordApiPath: id => `/project/contract/${id}`,
    idField: 'contractId'
  }
];

// ─────────────────────────────────────────────────────────────
// 用例
// ─────────────────────────────────────────────────────────────
for (const cfg of PAGES) {
  test.describe(`操作列可新标签打开 - ${cfg.name}（${cfg.listPath}）`, () => {
    let api;

    test.beforeAll(async () => {
      api = await setupApi();
    });

    test.afterAll(async () => {
      if (api) await api.dispose();
    });

    test.beforeEach(async ({ page }) => {
      await login(page);
      await openList(page, cfg);
    });

    test('「详情」是带 href 的 <a>，href 指向本行记录的详情路由，且该记录真实存在', async ({ page }) => {
      const row = await findRowWithActions(page, ['详情']);
      test.skip(!row, `${cfg.name}没有带「详情」操作的数据行`);

      const recordId = await readRowRecordId(row, cfg);
      expect(recordId, `应能从名称列链接解析出本行 ${cfg.idField}`).toBeTruthy();

      const el = actionEl(row, '详情');
      await expect(el, '操作列应存在「详情」元素').toHaveCount(1);

      // 1) 必须是锚点，不是 button —— 这是右键菜单「在新标签页中打开」出现的前提
      const tagName = await el.evaluate(node => node.tagName);
      expect(tagName, '「详情」必须渲染为 <a>（改造前是 <button>，右键无法新开标签）').toBe('A');

      // 2) href 必须非空且指向正确的详情路由
      const href = await el.getAttribute('href');
      expect(href, '「详情」的 href 不能为空').toBeTruthy();
      expect(href.trim().length, '「详情」的 href 不能是空串').toBeGreaterThan(0);
      expect(href, `「详情」href 应指向 ${cfg.detailPath(recordId)}`).toBe(cfg.detailPath(recordId));

      // 3) href 里的 id 必须是后端真实存在的记录（不是拼错的占位值）
      const detail = await api.get(cfg.recordApiPath(recordId));
      expect(detail.code, `${cfg.recordApiPath(recordId)} 应返回 200，证明 href 中的 id 真实存在`).toBe(200);
      expect(String(detail.data[cfg.idField]), '返回记录的主键应与 href 中的 id 一致').toBe(String(recordId));
    });

    test('「编辑」是带 href 的 <a>，href 指向本行记录的编辑路由', async ({ page }) => {
      const row = await findRowWithActions(page, ['编辑']);
      test.skip(!row, `${cfg.name}没有带「编辑」操作的数据行（如项目已审核通过则不显示编辑）`);

      const recordId = await readRowRecordId(row, cfg);
      expect(recordId, `应能从名称列链接解析出本行 ${cfg.idField}`).toBeTruthy();

      const el = actionEl(row, '编辑');
      const tagName = await el.evaluate(node => node.tagName);
      expect(tagName, '「编辑」必须渲染为 <a>').toBe('A');

      const href = await el.getAttribute('href');
      expect(href, '「编辑」的 href 不能为空').toBeTruthy();
      expect(href, `「编辑」href 应指向 ${cfg.editPath(recordId)}`).toBe(cfg.editPath(recordId));
    });

    test('普通左键点击「详情」走 SPA 路由跳转，页面不发生整页重载', async ({ page }) => {
      const row = await findRowWithActions(page, ['详情']);
      test.skip(!row, `${cfg.name}没有带「详情」操作的数据行`);

      const recordId = await readRowRecordId(row, cfg);
      const expectedPath = cfg.detailPath(recordId);

      // 在当前 window 上打标记：整页重载会重建 window，标记随之消失
      await page.evaluate(() => { window.__spaMarker = 1; });
      const markerBefore = await page.evaluate(() => window.__spaMarker);
      expect(markerBefore, '标记应先成功写入').toBe(1);

      await actionEl(row, '详情').click();
      await page.waitForURL(url => url.pathname === expectedPath, { timeout: 15000 });

      const markerAfter = await page.evaluate(() => window.__spaMarker);
      expect(markerAfter, 'window 标记应存活 —— 存活=SPA 路由跳转，丢失=浏览器整页重载了').toBe(1);
      expect(new URL(page.url()).pathname, '应跳到详情路由').toBe(expectedPath);
    });

    test('Ctrl/⌘ + 点击「详情」在新标签打开，且当前页 URL 不变', async ({ page }) => {
      const row = await findRowWithActions(page, ['详情']);
      test.skip(!row, `${cfg.name}没有带「详情」操作的数据行`);

      const recordId = await readRowRecordId(row, cfg);
      const expectedPath = cfg.detailPath(recordId);
      const originalUrl = page.url();

      const newPagePromise = page.context().waitForEvent('page', { timeout: 10000 });
      await actionEl(row, '详情').click({ modifiers: [NEW_TAB_MODIFIER] });
      const newPage = await newPagePromise;
      // 新标签刚创建时 URL 仍是 about:blank，必须等它导航到目标路径再断言
      await waitForNewTabUrl(newPage, expectedPath);

      expect(new URL(newPage.url()).pathname, `新标签应打开 ${expectedPath}`).toBe(expectedPath);

      // 留出时间让「错误的 SPA 跳转」暴露出来：修饰键点击时不应同时调用 router.push
      await page.waitForTimeout(800);
      expect(page.url(), '修饰键点击后当前页 URL 必须保持不变（不能既开新标签又跳走当前页）').toBe(originalUrl);

      await newPage.close();
    });

    test('中键点击「详情」在新标签打开，且当前页 URL 不变', async ({ page }) => {
      const row = await findRowWithActions(page, ['详情']);
      test.skip(!row, `${cfg.name}没有带「详情」操作的数据行`);

      const recordId = await readRowRecordId(row, cfg);
      const expectedPath = cfg.detailPath(recordId);
      const originalUrl = page.url();

      const newPagePromise = page.context().waitForEvent('page', { timeout: 10000 });
      await actionEl(row, '详情').click({ button: 'middle' });
      const newPage = await newPagePromise;
      await waitForNewTabUrl(newPage, expectedPath);

      expect(new URL(newPage.url()).pathname, `中键点击应在新标签打开 ${expectedPath}`).toBe(expectedPath);

      await page.waitForTimeout(800);
      expect(page.url(), '中键点击后当前页 URL 必须保持不变').toBe(originalUrl);

      await newPage.close();
    });

    test('中键点击「编辑」在新标签打开编辑页', async ({ page }) => {
      const row = await findRowWithActions(page, ['编辑']);
      test.skip(!row, `${cfg.name}没有带「编辑」操作的数据行`);

      const recordId = await readRowRecordId(row, cfg);
      const expectedPath = cfg.editPath(recordId);
      const originalUrl = page.url();

      const newPagePromise = page.context().waitForEvent('page', { timeout: 10000 });
      await actionEl(row, '编辑').click({ button: 'middle' });
      const newPage = await newPagePromise;
      await waitForNewTabUrl(newPage, expectedPath);

      expect(new URL(newPage.url()).pathname, `新标签应打开 ${expectedPath}`).toBe(expectedPath);
      expect(page.url(), '当前页 URL 必须保持不变').toBe(originalUrl);

      await newPage.close();
    });
  });
}

// ─────────────────────────────────────────────────────────────
// 回归红线：改造不得破坏的既有行为
// 对应 specs/016-row-link-new-tab/bdd/row-link-new-tab.feature 第二、三节
// ─────────────────────────────────────────────────────────────
test.describe('操作列改造的回归红线', () => {

  test('左键进详情再返回，列表的查询条件仍然保留', async ({ page }) => {
    await login(page);
    await page.goto(`${BASE_URL}/project/list`);
    await page.waitForResponse(r => r.url().includes('/project/project/list') && r.request().method() === 'GET', { timeout: 20000 });

    // 在「项目名称」查询框填入条件（该页缓存 key 见 project/index.vue:986 project_search_state）
    const nameInput = page.locator('.el-form-item:has-text("项目名称") input').first();
    await nameInput.fill('回归校验用条件');
    await page.waitForTimeout(300);

    const row = await findRowWithActions(page, ['详情']);
    test.skip(!row, '项目列表没有带「详情」的数据行');

    // 左键进详情 —— 必须触发 onBeforeRouteLeave 才会写入 sessionStorage
    await actionEl(row, '详情').click();
    await page.waitForURL(u => new URL(u).pathname.startsWith('/project/list/detail/'), { timeout: 15000 });

    const cached = await page.evaluate(() => sessionStorage.getItem('project_search_state'));
    expect(cached, '离开列表页时应已写入查询条件缓存').toBeTruthy();
    expect(JSON.parse(cached).queryParams.projectName, '缓存里应含刚填的查询条件').toBe('回归校验用条件');

    // 返回列表，条件应被还原（不等接口——SPA 返回时组件可能复用而不重新请求）
    await page.goBack();
    await page.waitForURL(u => new URL(u).pathname === '/project/list', { timeout: 15000 });
    await expect(page.locator('.el-form-item:has-text("项目名称") input').first(),
      '返回列表后查询条件应仍在').toHaveValue('回归校验用条件', { timeout: 20000 });
  });

  test('详情为弹窗的页面保持普通按钮，不被误改成链接', async ({ page }) => {
    await login(page);
    // 客户管理是后端菜单驱动的动态路由，实际路径为 /market/customer（sys_menu 2208 父级 market）
    const listResp = page.waitForResponse(r => r.url().includes('/project/customer/list') && r.request().method() === 'GET', { timeout: 20000 });
    await page.goto(`${BASE_URL}/market/customer`);
    await listResp;
    await expect(mainTableRows(page).first(), '客户列表应渲染出数据行').toBeVisible({ timeout: 20000 });

    const row = await findRowWithActions(page, ['详情']);
    test.skip(!row, '客户列表没有数据行');

    // customer/index.vue:426 的 handleDetail 是 openDetail.value = true，没有对应 URL
    const tag = await actionEl(row, '详情').evaluate(el => el.tagName);
    expect(tag, '弹窗型详情不应变成 <a>（否则新标签打开会是死链）').toBe('BUTTON');

    await actionEl(row, '详情').click();
    await expect(page.locator('.el-dialog:visible').first(), '点击后应弹出对话框').toBeVisible({ timeout: 10000 });
    expect(new URL(page.url()).pathname, '弹窗不应改变地址').toBe('/market/customer');
  });

  test('改造后的入口与同列其他按钮外观一致', async ({ page }) => {
    await login(page);
    await page.goto(`${BASE_URL}/project/list`);
    await page.waitForResponse(r => r.url().includes('/project/project/list') && r.request().method() === 'GET', { timeout: 20000 });

    const row = await findRowWithActions(page, ['详情', '关联合同']);
    test.skip(!row, '项目列表没有同时含「详情」与「关联合同」的数据行');

    const pick = el => el.evaluate(e => {
      const s = getComputedStyle(e);
      return { fontSize: s.fontSize, color: s.color, padding: s.padding, height: s.height, textDecoration: s.textDecorationLine };
    });
    // 「详情」已改成 <a>，「关联合同」仍是 <button>，两者视觉必须无法区分
    const converted = await pick(actionEl(row, '详情'));
    const untouched = await pick(actionEl(row, '关联合同'));

    expect(converted.fontSize, '字号应一致').toBe(untouched.fontSize);
    expect(converted.color, '颜色应一致').toBe(untouched.color);
    expect(converted.padding, '内边距应一致').toBe(untouched.padding);
    expect(converted.height, '高度应一致').toBe(untouched.height);
    expect(converted.textDecoration, '不应出现 <a> 的默认下划线').toBe('none');
  });
});

// ─────────────────────────────────────────────────────────────
// 安全：href 由记录主键拼接而成，必须无法被数据内容劫持
// 对应 feature 第四节。改造把原本只存在于 JS 闭包里的目标地址暴露成了 DOM 属性，
// 因此需要长期守着「拼接结果不会变成可执行地址或站外地址」这条底线。
// ─────────────────────────────────────────────────────────────
test.describe('操作列链接的安全边界', () => {

  test('记录主键含恶意内容时，生成的 href 仍被约束在同源普通网页地址', async ({ page }) => {
    await login(page);
    await page.goto(`${BASE_URL}/project/list`);
    await page.waitForResponse(r => r.url().includes('/project/project/list') && r.request().method() === 'GET', { timeout: 20000 });

    const result = await page.evaluate(() => {
      // 取应用运行时真实的 router，而非另造一个，确保验证的是线上同一套解析规则
      const router = document.querySelector('#app')?.__vue_app__?.config?.globalProperties?.$router;
      if (!router) return { err: 'router 不可达' };

      const payloads = [
        'javascript:alert(1)',
        '../../../evil',
        '"><img src=x onerror=alert(1)>',
        '%0Ajavascript:alert(1)',
        'https://evil.example.com',
        '//evil.example.com',
      ];
      const rows = [];
      for (const p of payloads) {
        // 两种 to 形态各验一次：路径参数（project）与 query 参数（prolistDefect）
        const hrefs = [
          router.resolve('/project/list/detail/' + p).href,
          router.resolve({ path: '/project/prolistDefect/detail', query: { problemId: p } }).href,
        ];
        for (const href of hrefs) {
          // 交给浏览器按真实规则解析，而不是自己写正则判断
          const a = document.createElement('a');
          a.href = href;
          rows.push({ payload: p, href, protocol: a.protocol, host: a.host });
        }
      }
      return { origin: location.host, rows };
    });

    expect(result.err, 'router 应可达').toBeUndefined();

    for (const r of result.rows) {
      expect(['http:', 'https:'], `payload ${r.payload} 生成的 href(${r.href}) 协议必须是 http(s)，不得是 javascript:/data: 等可执行协议`)
        .toContain(r.protocol);
      expect(r.host, `payload ${r.payload} 生成的 href(${r.href}) 必须留在本站，不得指向站外`)
        .toBe(result.origin);
    }
  });
});

// ─────────────────────────────────────────────────────────────
// 右键菜单：浏览器上下文菜单属于浏览器自身 UI，不在页面 DOM 内，
// Playwright 无法读取其菜单项。故把「菜单里会出现『在新标签页中打开链接』」
// 归约为两个可在页面内验证的充要条件：
//   ① 右键目标是带 href 的 <a>（由前述 DOM 语义用例保证）
//   ② contextmenu 事件未被任何代码 preventDefault
// 二者同时成立时，Chromium 必然渲染链接类上下文菜单。
// 条件 ② 有真实风险：el-table 或全局指令一旦拦截 contextmenu，右键菜单就没了。
//
// ⚠️ 本用例**自带无头浏览器实例**，不使用外层 page fixture，原因见下方说明。
// ─────────────────────────────────────────────────────────────
test.describe('右键上下文菜单的前置条件', () => {

  /**
   * 为什么这里自己 launch 一个 headless 浏览器，而不用 { page } fixture：
   *
   * 有头模式下真实右键会弹出浏览器**原生上下文菜单**，它是模态的——菜单不关，
   * 浏览器就不再接收任何后续指令，连关闭 context 都会卡住。表现为
   * 「断言全部通过，却在 teardown 阶段超时」，且整套跑与单独跑结果不一致（flaky）。
   * 更麻烦的是菜单位于浏览器 UI 层，keyboard.press('Escape') 由 CDP 发往页面，
   * 根本关不掉它。
   *
   * 而本用例要验证的是**事件层行为**（contextmenu 是否被 preventDefault），
   * 无头 Chromium 照常派发该事件、只是不渲染原生菜单——想测的东西一点不少，
   * 还避开了模态阻塞。故显式固定为无头，使本用例对外层 --headed 免疫。
   */
  test('在「详情」上右键，事件到达 <a> 且未被拦截', async () => {
    const browser = await chromium.launch({ headless: true });
    try {
      const page = await browser.newPage();
      await login(page);
      await page.goto(`${BASE_URL}/project/list`);
      await page.waitForResponse(r => r.url().includes('/project/project/list') && r.request().method() === 'GET', { timeout: 20000 });

      const row = await findRowWithActions(page, ['详情']);
      test.skip(!row, '项目列表没有带「详情」的数据行');

      // 在 document 末端观察：能观察到即说明事件完成冒泡、没有被中途 stopPropagation
      await page.evaluate(() => {
        window.__ctxLog = [];
        document.addEventListener('contextmenu', e => {
          const a = e.target.closest && e.target.closest('a');
          window.__ctxLog.push({
            targetIsAnchor: !!a,
            href: a ? a.getAttribute('href') : null,
            defaultPrevented: e.defaultPrevented,
          });
        });
      });

      await actionEl(row, '详情').click({ button: 'right' });
      await page.waitForTimeout(500);

      const log = await page.evaluate(() => window.__ctxLog);
      expect(log.length, 'contextmenu 事件应冒泡到 document（未被 stopPropagation 掐断）').toBeGreaterThan(0);

      const ev = log[log.length - 1];
      expect(ev.targetIsAnchor, '右键目标应落在 <a> 上（而非 <button>）').toBe(true);
      expect(ev.href, '该 <a> 应带有指向详情页的 href').toMatch(/^\/project\/list\/detail\/\d+$/);
      expect(ev.defaultPrevented,
        'contextmenu 不得被 preventDefault —— 一旦被拦，浏览器就不会弹出上下文菜单，右键新标签能力直接失效')
        .toBe(false);
    } finally {
      await browser.close();
    }
  });
});
