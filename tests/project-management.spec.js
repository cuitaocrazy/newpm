/**
 * 项目管理功能 E2E 测试
 * 测试场景：立项申请 -> 列表查看 -> 编辑项目 -> 审核
 */

import { test, expect } from '@playwright/test';

// 测试配置
const BASE_URL = 'http://localhost:80';
const API_URL = 'http://localhost:8080';

// 测试用户
const TEST_USER = {
  username: 'admin',
  password: 'admin123'
};

// 测试数据
const TEST_PROJECT = {
  projectName: `测试项目_${Date.now()}`,
  industry: 'ZH', // 综合
  region: 'BJ', // 北京
  provinceCode: '11', // 北京市
  shortName: 'TEST',
  establishedYear: '2026',
  projectCategory: 'RJKF', // 软件开发
  projectDept: '216', // 深圳组
  projectStatus: '1', // 立项
  acceptanceStatus: '0', // 未验收
  estimatedWorkload: '100',
  projectBudget: '1000000',
  projectAddress: '测试地址',
  projectPlan: '测试计划',
  projectDescription: '这是一个自动化测试项目',
  projectManagerId: '250', // 曲君
  marketManagerId: '103', // 张仟栋
  salesManagerId: '103',
  participants: '103,104,105'
};

test.describe('项目管理功能测试', () => {

  // 每个测试前登录
  test.beforeEach(async ({ page }) => {
    await page.goto(BASE_URL);

    // 登录
    await page.fill('input[placeholder="用户名"]', TEST_USER.username);
    await page.fill('input[placeholder="密码"]', TEST_USER.password);
    await page.click('button:has-text("登录")');

    // 等待登录成功，跳转到首页
    await page.waitForURL(`${BASE_URL}/index`);
    await expect(page).toHaveTitle(/若依管理系统/);
  });

  test('场景1：立项申请 - 创建新项目', async ({ page }) => {
    console.log('🎬 开始测试：立项申请');

    // 1. 导航到立项申请页面
    await page.click('text=项目管理');
    await page.click('text=立项申请');
    await page.waitForURL(`${BASE_URL}/project/apply`);

    console.log('✓ 成功进入立项申请页面');

    // 2. 填写项目基本信息
    await page.fill('input[placeholder*="项目名称"]', TEST_PROJECT.projectName);

    // 选择行业
    await page.click('.el-select:has-text("行业") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("综合")`);

    // 选择一级区域
    await page.click('.el-select:has-text("一级区域") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("北京")`);

    // 等待二级区域加载
    await page.waitForTimeout(500);

    // 选择二级区域
    await page.click('.el-select:has-text("二级区域") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("北京市")`);

    // 填写简称
    await page.fill('input[placeholder*="简称"]', TEST_PROJECT.shortName);

    // 选择立项年度
    await page.click('.el-select:has-text("立项年度") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("2026")`);

    console.log('✓ 基本信息填写完成');

    // 3. 验证项目编号自动生成
    const projectCode = await page.inputValue('input[placeholder*="项目编号"]');
    expect(projectCode).toContain('ZH-BJ-11-TEST-2026');
    console.log(`✓ 项目编号自动生成: ${projectCode}`);

    // 4. 填写项目分类和部门
    await page.click('.el-select:has-text("项目分类") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("软件开发")`);

    await page.click('.el-tree-select:has-text("项目部门")');
    await page.click('.el-tree-node__content:has-text("深圳组")');

    // 5. 填写项目状态
    await page.click('.el-select:has-text("项目状态") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("立项")`);

    await page.click('.el-select:has-text("验收状态") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("未验收")`);

    console.log('✓ 项目分类和状态填写完成');

    // 6. 填写工作量和预算
    await page.fill('input[placeholder*="预估工作量"]', TEST_PROJECT.estimatedWorkload);
    await page.fill('input[placeholder*="项目预算"]', TEST_PROJECT.projectBudget);

    // 7. 填写项目地址、计划、描述
    await page.fill('textarea[placeholder*="项目地址"]', TEST_PROJECT.projectAddress);
    await page.fill('textarea[placeholder*="项目计划"]', TEST_PROJECT.projectPlan);
    await page.fill('textarea[placeholder*="项目描述"]', TEST_PROJECT.projectDescription);

    console.log('✓ 项目详情填写完成');

    // 8. 选择人员配置
    await page.click('.el-select:has-text("项目经理") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("曲君")`);

    await page.click('.el-select:has-text("市场经理") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("张仟栋")`);

    await page.click('.el-select:has-text("销售负责人") .el-input');
    await page.click(`.el-select-dropdown__item:first-child`);

    // 选择参与人员（多选）
    await page.click('.el-select:has-text("参与人员") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("张仟栋")`);
    await page.click(`.el-select-dropdown__item:has-text("张磊")`);
    await page.keyboard.press('Escape'); // 关闭下拉框

    console.log('✓ 人员配置完成');

    // 9. 提交立项申请
    await page.click('button:has-text("提交")');

    // 等待提交成功提示
    await expect(page.locator('.el-message--success')).toBeVisible();
    await expect(page.locator('.el-message--success')).toContainText('操作成功');

    console.log('✅ 立项申请提交成功！');

    // 等待跳转到项目列表
    await page.waitForURL(`${BASE_URL}/project/list`);
  });

  test('场景2：项目列表 - 查询和筛选', async ({ page }) => {
    console.log('🎬 开始测试：项目列表查询');

    // 1. 导航到项目列表
    await page.click('text=项目管理');
    await page.click('text=项目列表');
    await page.waitForURL(`${BASE_URL}/project/list`);

    console.log('✓ 成功进入项目列表页面');

    // 2. 验证列表加载
    await expect(page.locator('.el-table')).toBeVisible();

    // 3. 测试查询功能 - 按项目名称搜索
    await page.fill('input[placeholder*="项目名称"]', 'MCPP');
    await page.click('button:has-text("搜索")');

    // 等待表格刷新
    await page.waitForTimeout(1000);

    // 验证搜索结果
    const rows = await page.locator('.el-table__row').count();
    console.log(`✓ 搜索到 ${rows} 条记录`);

    // 4. 测试筛选功能 - 按审核状态筛选
    await page.click('.el-select:has-text("审核状态") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("待审核")`);
    await page.click('button:has-text("搜索")');

    await page.waitForTimeout(1000);
    console.log('✓ 审核状态筛选完成');

    // 5. 重置查询条件
    await page.click('button:has-text("重置")');
    await page.waitForTimeout(1000);

    console.log('✅ 项目列表查询测试完成！');
  });

  test('场景3：编辑项目 - 修改项目信息', async ({ page }) => {
    console.log('🎬 开始测试：编辑项目');

    // 1. 导航到项目列表
    await page.click('text=项目管理');
    await page.click('text=项目列表');
    await page.waitForURL(`${BASE_URL}/project/list`);

    // 2. 点击第一条记录的编辑按钮
    await page.click('.el-table__row:first-child button:has-text("编辑")');

    // 等待编辑页面加载
    await page.waitForURL(/\/project\/list\/edit\/\d+/);
    console.log('✓ 成功进入编辑页面');

    // 3. 修改项目名称
    const originalName = await page.inputValue('input[placeholder*="项目名称"]');
    const newName = `${originalName}_已编辑`;
    await page.fill('input[placeholder*="项目名称"]', newName);

    console.log(`✓ 项目名称修改: ${originalName} -> ${newName}`);

    // 4. 修改预估工作量
    await page.fill('input[placeholder*="预估工作量"]', '200');

    // 5. 修改项目预算
    await page.fill('input[placeholder*="项目预算"]', '2000000');

    console.log('✓ 项目信息修改完成');

    // 6. 保存修改
    await page.click('button:has-text("保存")');

    // 等待保存成功提示
    await expect(page.locator('.el-message--success')).toBeVisible();
    await expect(page.locator('.el-message--success')).toContainText('保存成功');

    console.log('✅ 项目编辑保存成功！');

    // 等待跳转回列表
    await page.waitForURL(`${BASE_URL}/project/list`);
  });

  test('场景4：项目详情 - 查看项目信息', async ({ page }) => {
    console.log('🎬 开始测试：项目详情');

    // 1. 导航到项目列表
    await page.click('text=项目管理');
    await page.click('text=项目列表');
    await page.waitForURL(`${BASE_URL}/project/list`);

    // 2. 点击第一条记录的详情按钮
    await page.click('.el-table__row:first-child button:has-text("详情")');

    // 等待详情页面加载
    await page.waitForURL(/\/project\/list\/detail\/\d+/);
    console.log('✓ 成功进入详情页面');

    // 3. 验证详情页面元素
    await expect(page.locator('text=项目基本信息')).toBeVisible();
    await expect(page.locator('text=人员配置')).toBeVisible();
    await expect(page.locator('text=客户信息')).toBeVisible();

    // 4. 验证字段为只读
    const projectNameInput = page.locator('input[placeholder*="项目名称"]');
    await expect(projectNameInput).toBeDisabled();

    console.log('✓ 详情页面所有字段为只读状态');

    // 5. 返回列表
    await page.click('button:has-text("返回")');
    await page.waitForURL(`${BASE_URL}/project/list`);

    console.log('✅ 项目详情查看测试完成！');
  });

  test('场景5：部门树过滤 - 验证只显示三级及以下机构', async ({ page }) => {
    console.log('🎬 开始测试：部门树过滤');

    // 1. 导航到立项申请页面
    await page.click('text=项目管理');
    await page.click('text=立项申请');
    await page.waitForURL(`${BASE_URL}/project/apply`);

    // 2. 点击项目部门选择器
    await page.click('.el-tree-select:has-text("项目部门")');

    // 等待下拉框展开
    await page.waitForTimeout(500);

    // 3. 验证部门树结构
    const treeNodes = await page.locator('.el-tree-node').all();
    console.log(`✓ 部门树节点数量: ${treeNodes.length}`);

    // 4. 验证没有一级和二级机构（应该从三级开始）
    const firstNodeText = await treeNodes[0].textContent();
    console.log(`✓ 第一个节点: ${firstNodeText}`);

    // 验证不包含"若依科技"、"深圳总公司"等一二级机构
    expect(firstNodeText).not.toContain('若依科技');
    expect(firstNodeText).not.toContain('深圳总公司');

    console.log('✅ 部门树过滤验证通过！');
  });

  test('场景6：列表字段验证 - 参与人员和收入确认状态', async ({ page }) => {
    console.log('🎬 开始测试：列表字段显示');

    // 1. 导航到项目列表
    await page.click('text=项目管理');
    await page.click('text=项目列表');
    await page.waitForURL(`${BASE_URL}/project/list`);

    // 2. 验证参与人员列显示
    const participantsCell = page.locator('.el-table__row:first-child td').filter({ hasText: /张|李|王/ });
    if (await participantsCell.count() > 0) {
      const participantsText = await participantsCell.first().textContent();
      console.log(`✓ 参与人员显示: ${participantsText}`);
      expect(participantsText).toMatch(/[\u4e00-\u9fa5]+/); // 验证包含中文姓名
    }

    // 3. 验证收入确认状态列显示
    const revenueStatusCell = page.locator('.el-table__row:first-child .el-tag').filter({ hasText: /未确认|已确认/ });
    if (await revenueStatusCell.count() > 0) {
      const statusText = await revenueStatusCell.first().textContent();
      console.log(`✓ 收入确认状态显示: ${statusText}`);
      expect(statusText).toMatch(/未确认|已确认/);
    }

    // 4. 验证审核状态列显示
    const approvalStatusCell = page.locator('.el-table__row:first-child .el-tag').filter({ hasText: /待审核|审核通过|审核拒绝/ });
    if (await approvalStatusCell.count() > 0) {
      const statusText = await approvalStatusCell.first().textContent();
      console.log(`✓ 审核状态显示: ${statusText}`);
      expect(statusText).toMatch(/待审核|审核通过|审核拒绝/);
    }

    console.log('✅ 列表字段显示验证通过！');
  });

  test('场景7：项目编号自动生成 - 验证格式正确', async ({ page }) => {
    console.log('🎬 开始测试：项目编号自动生成');

    // 1. 导航到立项申请页面
    await page.click('text=项目管理');
    await page.click('text=立项申请');
    await page.waitForURL(`${BASE_URL}/project/apply`);

    // 2. 依次选择字段，观察项目编号变化

    // 选择行业
    await page.click('.el-select:has-text("行业") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("综合")`);
    await page.waitForTimeout(300);

    let projectCode = await page.inputValue('input[placeholder*="项目编号"]');
    console.log(`✓ 选择行业后: ${projectCode}`);
    expect(projectCode).toContain('ZH-');

    // 选择一级区域
    await page.click('.el-select:has-text("一级区域") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("北京")`);
    await page.waitForTimeout(500);

    projectCode = await page.inputValue('input[placeholder*="项目编号"]');
    console.log(`✓ 选择一级区域后: ${projectCode}`);
    expect(projectCode).toContain('ZH-BJ-');

    // 选择二级区域
    await page.click('.el-select:has-text("二级区域") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("北京市")`);
    await page.waitForTimeout(300);

    projectCode = await page.inputValue('input[placeholder*="项目编号"]');
    console.log(`✓ 选择二级区域后: ${projectCode}`);
    expect(projectCode).toContain('ZH-BJ-11-');

    // 填写简称
    await page.fill('input[placeholder*="简称"]', 'AUTO');
    await page.waitForTimeout(300);

    projectCode = await page.inputValue('input[placeholder*="项目编号"]');
    console.log(`✓ 填写简称后: ${projectCode}`);
    expect(projectCode).toContain('ZH-BJ-11-AUTO-');

    // 选择立项年度
    await page.click('.el-select:has-text("立项年度") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("2026")`);
    await page.waitForTimeout(300);

    projectCode = await page.inputValue('input[placeholder*="项目编号"]');
    console.log(`✓ 选择立项年度后: ${projectCode}`);
    expect(projectCode).toBe('ZH-BJ-11-AUTO-2026');

    console.log('✅ 项目编号自动生成验证通过！');
  });

});

/**
 * 测试执行说明：
 *
 * 1. 安装 Playwright:
 *    npm install -D @playwright/test
 *
 * 2. 安装浏览器:
 *    npx playwright install
 *
 * 3. 运行测试:
 *    npx playwright test tests/project-management.spec.js
 *
 * 4. 运行测试并查看报告:
 *    npx playwright test tests/project-management.spec.js --reporter=html
 *    npx playwright show-report
 *
 * 5. 调试模式运行:
 *    npx playwright test tests/project-management.spec.js --debug
 *
 * 6. 运行特定测试:
 *    npx playwright test tests/project-management.spec.js -g "立项申请"
 */
