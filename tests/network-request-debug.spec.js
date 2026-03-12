/**
 * 网络请求调试测试
 * 专门用于验证立项申请保存时的网络请求
 */

import { test, expect } from '@playwright/test';

// 测试配置
const BASE_URL = 'http://localhost:80';
const API_URL = 'http://localhost:8080';

// 测试用户
const TEST_USER = {
  username: 'admin',
  password: '123456789'
};

test.describe('网络请求调试', () => {

  test('监控立项申请保存的网络请求', async ({ page }) => {
    console.log('🎬 开始监控网络请求');

    // 收集所有网络请求
    const requests = [];
    const responses = [];

    // 监听所有请求
    page.on('request', request => {
      const url = request.url();
      const method = request.method();
      const postData = request.postData();

      requests.push({
        url,
        method,
        postData: postData ? JSON.parse(postData) : null,
        timestamp: new Date().toISOString()
      });

      console.log(`📤 请求: ${method} ${url}`);
      if (postData) {
        console.log(`   数据:`, JSON.parse(postData));
      }
    });

    // 监听所有响应
    page.on('response', async response => {
      const url = response.url();
      const status = response.status();

      let body = null;
      try {
        // 只解析 JSON 响应
        if (response.headers()['content-type']?.includes('application/json')) {
          body = await response.json();
        }
      } catch (e) {
        // 忽略解析错误
      }

      responses.push({
        url,
        status,
        body,
        timestamp: new Date().toISOString()
      });

      console.log(`📥 响应: ${status} ${url}`);
      if (body) {
        console.log(`   数据:`, body);
      }
    });

    // 1. 登录
    await page.goto(BASE_URL);
    await page.waitForLoadState('networkidle');
    await page.fill('input[placeholder="用户名"]', TEST_USER.username);
    await page.fill('input[placeholder="密码"]', TEST_USER.password);
    await page.click('button:has-text("登录")');
    await page.waitForURL(`${BASE_URL}/index`);

    console.log('✓ 登录成功');

    // 清空之前的请求记录（登录相关的）
    requests.length = 0;
    responses.length = 0;

    // 2. 导航到立项申请页面
    await page.click('text=项目管理');
    await page.click('text=立项申请');
    await page.waitForURL(`${BASE_URL}/project/apply`);

    console.log('✓ 进入立项申请页面');

    // 3. 填写表单（最小必填字段）
    const timestamp = Date.now();

    // 项目名称
    await page.fill('input[placeholder*="项目名称"]', `网络测试项目_${timestamp}`);

    // 行业
    await page.click('.el-select:has-text("行业") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("综合")`);

    // 一级区域
    await page.click('.el-select:has-text("一级区域") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("北京")`);
    await page.waitForTimeout(500);

    // 二级区域
    await page.click('.el-select:has-text("二级区域") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("北京市")`);

    // 简称
    await page.fill('input[placeholder*="简称"]', 'NET');

    // 立项年度
    await page.click('.el-select:has-text("立项年度") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("2026")`);

    // 项目分类
    await page.click('.el-select:has-text("项目分类") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("软件开发")`);

    // 项目部门
    await page.click('.el-tree-select:has-text("项目部门")');
    await page.click('.el-tree-node__content:has-text("深圳组")');

    // 项目状态
    await page.click('.el-select:has-text("项目状态") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("立项")`);

    // 验收状态
    await page.click('.el-select:has-text("验收状态") .el-input');
    await page.click(`.el-select-dropdown__item:has-text("未验收")`);

    // 预估工作量
    await page.fill('input[placeholder*="预估工作量"]', '50');

    // 项目预算
    await page.fill('input[placeholder*="项目预算"]', '500000');

    // 项目地址
    await page.fill('input[placeholder*="项目地址"]', '测试地址');

    // 项目计划
    await page.fill('textarea[placeholder*="项目计划"]', '测试计划');

    // 项目描述
    await page.fill('textarea[placeholder*="项目描述"]', '这是一个网络请求测试项目');

    // 项目经理
    await page.click('.el-select:has-text("项目经理") .el-input');
    await page.click(`.el-select-dropdown__item:first-child`);

    // 市场经理
    await page.click('.el-select:has-text("市场经理") .el-input');
    await page.click(`.el-select-dropdown__item:first-child`);

    // 销售负责人
    await page.click('.el-select:has-text("销售负责人") .el-input');
    await page.click(`.el-select-dropdown__item:first-child`);

    // 参与人员
    await page.click('.el-select:has-text("参与人员") .el-input');
    await page.click(`.el-select-dropdown__item:first-child`);
    await page.keyboard.press('Escape');

    // 客户名称
    await page.click('.el-select:has-text("客户名称") .el-input');
    await page.click(`.el-select-dropdown__item:first-child`);
    await page.waitForTimeout(500);

    // 客户联系人
    await page.click('.el-select:has-text("客户联系人") .el-input');
    await page.click(`.el-select-dropdown__item:first-child`);

    console.log('✓ 表单填写完成');

    // 清空之前的请求记录（表单加载相关的）
    requests.length = 0;
    responses.length = 0;

    // 4. 点击提交按钮
    console.log('\n🚀 准备提交表单...');
    await page.click('button:has-text("提交")');

    // 等待一段时间，确保请求发送
    await page.waitForTimeout(2000);

    // 5. 分析网络请求
    console.log('\n📊 网络请求分析：');
    console.log(`总请求数: ${requests.length}`);
    console.log(`总响应数: ${responses.length}`);

    // 查找项目创建的 POST 请求
    const projectCreateRequest = requests.find(req =>
      req.method === 'POST' && req.url.includes('/project/project')
    );

    if (projectCreateRequest) {
      console.log('\n✅ 找到项目创建请求！');
      console.log('请求详情：');
      console.log(`  URL: ${projectCreateRequest.url}`);
      console.log(`  方法: ${projectCreateRequest.method}`);
      console.log(`  时间: ${projectCreateRequest.timestamp}`);
      console.log(`  数据:`, JSON.stringify(projectCreateRequest.postData, null, 2));

      // 查找对应的响应
      const projectCreateResponse = responses.find(res =>
        res.url.includes('/project/project')
      );

      if (projectCreateResponse) {
        console.log('\n✅ 找到项目创建响应！');
        console.log('响应详情：');
        console.log(`  状态码: ${projectCreateResponse.status}`);
        console.log(`  时间: ${projectCreateResponse.timestamp}`);
        console.log(`  数据:`, JSON.stringify(projectCreateResponse.body, null, 2));

        // 断言：请求应该存在
        expect(projectCreateRequest).toBeDefined();
        // 断言：响应状态码应该是 200
        expect(projectCreateResponse.status).toBe(200);
        // 断言：响应应该包含成功标识
        expect(projectCreateResponse.body?.code).toBe(200);
      } else {
        console.log('\n❌ 未找到项目创建响应！');
      }
    } else {
      console.log('\n❌ 未找到项目创建请求！');
      console.log('\n所有请求列表：');
      requests.forEach((req, index) => {
        console.log(`  ${index + 1}. ${req.method} ${req.url}`);
      });
    }

    // 6. 验证是否有成功提示
    const successMessage = page.locator('.el-message--success');
    if (await successMessage.isVisible()) {
      const messageText = await successMessage.textContent();
      console.log(`\n✅ 成功提示: ${messageText}`);
    } else {
      console.log('\n❌ 未找到成功提示');
    }

    // 7. 验证是否跳转
    const currentUrl = page.url();
    console.log(`\n当前 URL: ${currentUrl}`);

    if (currentUrl.includes('/project/list')) {
      console.log('✅ 已跳转到项目列表页');
    } else {
      console.log('❌ 未跳转到项目列表页');
    }

    // 最终断言
    expect(projectCreateRequest, '应该发送项目创建请求').toBeDefined();
  });

});
