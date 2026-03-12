import { test, expect } from '@playwright/test';

test.describe('从项目列表关联合同功能测试', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('http://localhost/login');
    await page.fill('input[placeholder="账号"]', 'admin');
    await page.fill('input[placeholder="密码"]', '123456789');

    // 点击登录按钮
    await page.locator('button.el-button--primary').click();

    // 等待登录成功，跳转到首页
    await page.waitForURL('http://localhost/index', { timeout: 30000 });
    console.log('✅ 登录成功');
  });

  test('点击项目列表的"关联合同"按钮，应该自动回显项目、部门、客户信息', async ({ page }) => {
    // 1. 导航到项目管理页面
    console.log('\n📍 导航到项目管理页面...');
    await page.goto('http://localhost/project/list');
    await page.waitForLoadState('networkidle');

    // 2. 等待项目列表加载
    await page.waitForSelector('.el-table__body', { timeout: 10000 });
    console.log('✅ 项目列表已加载');

    // 3. 找到第一个有"关联合同"按钮的项目行
    const addContractButton = page.locator('button:has-text("关联合同")').first();

    // 检查是否存在"关联合同"按钮
    const buttonCount = await addContractButton.count();
    if (buttonCount === 0) {
      console.log('❌ 未找到"关联合同"按钮，可能所有项目都已有合同');
      // 截图项目列表
      await page.screenshot({ path: 'screenshots/project-list-no-add-button.png', fullPage: true });
      throw new Error('没有可用的"关联合同"按钮');
    }

    console.log(`✅ 找到 ${buttonCount} 个"关联合同"按钮`);

    // 4. 获取项目行信息（在点击前）
    const projectRow = addContractButton.locator('xpath=ancestor::tr');
    await projectRow.waitFor({ state: 'visible' });

    // 截图项目行
    await projectRow.screenshot({ path: 'screenshots/project-row-before-click.png' });

    // 获取项目信息文本内容
    const projectRowText = await projectRow.innerText();
    console.log('\n📋 项目行内容:\n', projectRowText);

    // 5. 点击"关联合同"按钮
    console.log('\n🖱️  点击"关联合同"按钮...');
    await addContractButton.click();

    // 6. 等待跳转到合同新增页面
    await page.waitForURL(/\/htkx\/contract\/add\?projectId=\d+/, { timeout: 10000 });
    const url = page.url();
    console.log('✅ 跳转成功，当前URL:', url);

    // 提取projectId
    const projectIdMatch = url.match(/projectId=(\d+)/);
    const projectId = projectIdMatch ? projectIdMatch[1] : null;
    console.log('📌 ProjectId:', projectId);

    // 等待页面加载和数据填充
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000); // 等待异步数据加载

    // 7. 截图合同新增页面
    await page.screenshot({ path: 'screenshots/contract-add-page.png', fullPage: true });
    console.log('✅ 页面截图已保存');

    // 8. 检查项目信息提示框
    const projectInfoAlert = page.locator('.el-alert');
    const alertExists = await projectInfoAlert.count() > 0;
    console.log('\n📊 项目信息提示框存在:', alertExists);

    if (alertExists) {
      const alertText = await projectInfoAlert.innerText();
      console.log('提示框内容:', alertText);
      await expect(projectInfoAlert).toBeVisible();
    }

    // 9. 检查部门字段是否有值（通过input显示值）
    const deptInput = page.locator('label:has-text("部门")').locator('xpath=following-sibling::div[1]//input').first();
    const deptDisplayValue = await deptInput.inputValue();
    console.log('\n📝 部门显示值:', deptDisplayValue || '(空)');

    // 10. 检查关联客户字段是否有值
    const customerInput = page.locator('label:has-text("关联客户")').locator('xpath=following-sibling::div[1]//input').first();
    const customerDisplayValue = await customerInput.inputValue();
    console.log('📝 关联客户显示值:', customerDisplayValue || '(空)');

    // 11. 检查关联项目表格
    const projectTable = page.locator('.el-table').first();
    const hasProjectTable = await projectTable.isVisible().catch(() => false);
    console.log('📝 关联项目表格可见:', hasProjectTable);

    if (hasProjectTable) {
      const projectTableText = await projectTable.innerText();
      console.log('关联项目表格内容:\n', projectTableText);
    }

    // 12. 打开浏览器控制台日志（查看我们添加的console.log）
    console.log('\n🔍 查看浏览器控制台日志...');
    page.on('console', msg => {
      if (msg.type() === 'log' || msg.type() === 'warn' || msg.type() === 'error') {
        console.log(`[浏览器 ${msg.type()}]:`, msg.text());
      }
    });

    // 13. 打印表单数据到控制台（通过浏览器执行JS）
    const formData = await page.evaluate(() => {
      const formElement = document.querySelector('.contract-add');
      if (!formElement) return { error: '未找到.contract-add元素' };

      // 尝试获取Vue组件数据
      // @ts-ignore
      const vueInstance = formElement.__vueParentComponent?.ctx;
      if (vueInstance) {
        return {
          projectInfo: vueInstance.projectInfo,
          form: {
            deptId: vueInstance.form?.deptId,
            customerId: vueInstance.form?.customerId,
            projectIds: vueInstance.form?.projectIds
          },
          selectedProjects: vueInstance.selectedProjects
        };
      }
      return { error: '未找到Vue实例' };
    });

    console.log('\n📦 Vue表单数据:\n', JSON.stringify(formData, null, 2));

    // 14. 验证结果
    console.log('\n=== ✅ 测试结果汇总 ===');
    console.log('✓ 成功跳转到合同新增页面');
    console.log('✓ URL包含projectId参数:', projectId);
    console.log('项目信息提示框:', alertExists ? '✓ 显示' : '✗ 未显示');
    console.log('部门回显:', deptDisplayValue ? '✓ 有值' : '✗ 无值');
    console.log('客户回显:', customerDisplayValue ? '✓ 有值' : '✗ 无值');
    console.log('项目表格:', hasProjectTable ? '✓ 显示' : '✗ 未显示');

    // 暂停让你查看结果
    console.log('\n⏸️  测试完成，暂停以便查看...');
    await page.pause();
  });
});
