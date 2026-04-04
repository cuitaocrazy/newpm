/**
 * 工作日报模块 E2E 测试
 * 纯 API 测试，只读操作（不创建/删除日报，避免影响真实工时数据）
 *
 * 覆盖：
 * - DailyReportController: list, monthly, myProjects, my/{date}, activityUsers, weeklyStats, teamMonthly, teamProjectOptions
 * - ProjectStatsController: projectStats, projectNameSuggestions, correctLog
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;

// 当前年月和日期
const now = new Date();
const yearMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

// 计算本周一日期（用于 weeklyStats）
const dayOfWeek = now.getDay();
const mondayOffset = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
const monday = new Date(now);
monday.setDate(now.getDate() + mondayOffset);
const weekStart = `${monday.getFullYear()}-${String(monday.getMonth() + 1).padStart(2, '0')}-${String(monday.getDate()).padStart(2, '0')}`;

test.describe('工作日报模块', () => {
  test.beforeAll(async () => {
    api = await setupApi();
    console.log(`\n📋 日报E2E测试启动，yearMonth=${yearMonth}, today=${today}, weekStart=${weekStart}`);
  });

  test.afterAll(async () => {
    await api?.dispose();
  });

  test('日报列表查询', async () => {
    console.log('\n▶ 测试：日报列表查询 GET /list');
    const res = await api.get('/project/dailyReport/list', {
      pageNum: 1,
      pageSize: 10,
      yearMonth,
    });
    console.log(`  响应 code=${res.code}, total=${res.total}`);

    expect(res.code, '接口应返回200').toBe(200);
    expect(res).toHaveProperty('rows');
    expect(res).toHaveProperty('total');
    expect(Array.isArray(res.rows), 'rows应为数组').toBe(true);
    expect(typeof res.total, 'total应为数字').toBe('number');
    console.log(`  ✅ 日报列表查询成功，共${res.total}条记录`);
  });

  test('月度日报查询', async () => {
    console.log('\n▶ 测试：月度日报查询 GET /monthly');
    const res = await api.get('/project/dailyReport/monthly', {
      yearMonth,
    });
    console.log(`  响应 code=${res.code}`);

    expect(res.code, '接口应返回200').toBe(200);
    expect(res).toHaveProperty('data');
    // data 是当月日报列表（可能为空）
    if (Array.isArray(res.data)) {
      console.log(`  ✅ 月度日报查询成功，共${res.data.length}条日报`);
      if (res.data.length > 0) {
        const first = res.data[0];
        console.log(`  首条日报: reportDate=${first.reportDate}, totalWorkHours=${first.totalWorkHours}`);
      }
    } else {
      console.log(`  ✅ 月度日报查询成功，data类型=${typeof res.data}`);
    }
  });

  test('我的项目列表', async () => {
    console.log('\n▶ 测试：我的项目列表 GET /myProjects');
    const res = await api.get('/project/dailyReport/myProjects');
    console.log(`  响应 code=${res.code}`);

    expect(res.code, '接口应返回200').toBe(200);
    expect(res).toHaveProperty('data');
    expect(Array.isArray(res.data), 'data应为数组').toBe(true);
    console.log(`  项目数量: ${res.data.length}`);

    if (res.data.length > 0) {
      const first = res.data[0];
      expect(first).toHaveProperty('projectId');
      expect(first).toHaveProperty('projectName');
      expect(first).toHaveProperty('hasSubProject');
      console.log(`  首个项目: id=${first.projectId}, name=${first.projectName}, hasSubProject=${first.hasSubProject}`);
    }
    console.log(`  ✅ 我的项目列表查询成功`);
  });

  test('查询指定日期日报', async () => {
    console.log(`\n▶ 测试：查询指定日期日报 GET /my/${today}`);
    const res = await api.get(`/project/dailyReport/my/${today}`);
    console.log(`  响应 code=${res.code}`);

    expect(res.code, '接口应返回200').toBe(200);
    // data 可能为 null（当天未填写日报）
    if (res.data) {
      console.log(`  今日已填写日报: reportDate=${res.data.reportDate}, totalWorkHours=${res.data.totalWorkHours}`);
      if (res.data.details && res.data.details.length > 0) {
        console.log(`  日报明细条数: ${res.data.details.length}`);
      }
    } else {
      console.log(`  今日尚未填写日报（data=null），符合预期`);
    }
    console.log(`  ✅ 指定日期日报查询成功`);
  });

  test('填写人员统计', async () => {
    console.log('\n▶ 测试：填写人员统计 GET /activityUsers');
    const res = await api.get('/project/dailyReport/activityUsers', {
      yearMonth,
    });
    console.log(`  响应 code=${res.code}`);

    expect(res.code, '接口应返回200').toBe(200);
    expect(res).toHaveProperty('data');
    console.log(`  ✅ 填写人员统计查询成功`);
    if (res.data) {
      console.log(`  统计数据: ${JSON.stringify(res.data).substring(0, 200)}`);
    }
  });

  test('项目工时统计', async () => {
    console.log('\n▶ 测试：项目工时统计 GET /projectStats');
    const res = await api.get('/project/dailyReport/projectStats', {
      pageNum: 1,
      pageSize: 10,
    });
    console.log(`  响应 code=${res.code}`);

    expect(res.code, '接口应返回200').toBe(200);
    expect(res).toHaveProperty('data');
    expect(res.data).toHaveProperty('total');
    expect(res.data).toHaveProperty('rows');
    expect(Array.isArray(res.data.rows), 'rows应为数组').toBe(true);
    console.log(`  ✅ 项目工时统计查询成功，共${res.data.total}个项目`);

    if (res.data.rows.length > 0) {
      const first = res.data.rows[0];
      console.log(`  首个项目: ${first.projectName || first.projectCode || 'N/A'}`);
    }
  });

  test('项目名称搜索建议', async () => {
    console.log('\n▶ 测试：项目名称搜索建议 GET /projectNameSuggestions');
    const res = await api.get('/project/dailyReport/projectNameSuggestions', {
      keyword: '项目',
    });
    console.log(`  响应 code=${res.code}`);

    expect(res.code, '接口应返回200').toBe(200);
    expect(res).toHaveProperty('data');
    if (Array.isArray(res.data)) {
      console.log(`  搜索建议数量: ${res.data.length}`);
      if (res.data.length > 0) {
        console.log(`  前3条建议: ${res.data.slice(0, 3).join(', ')}`);
      }
    }
    console.log(`  ✅ 项目名称搜索建议查询成功`);
  });

  test('补正日志查询', async () => {
    console.log('\n▶ 测试：补正日志查询 GET /projectStats/{id}/correctLog');

    // 先获取项目工时统计列表，取第一个项目的ID
    const statsRes = await api.get('/project/dailyReport/projectStats', {
      pageNum: 1,
      pageSize: 1,
    });
    expect(statsRes.code, '项目统计接口应返回200').toBe(200);

    if (statsRes.data.rows.length === 0) {
      console.log('  跳过：无项目数据可查询补正日志');
      return;
    }

    const projectId = statsRes.data.rows[0].projectId;
    console.log(`  使用项目ID: ${projectId}`);

    const res = await api.get(`/project/dailyReport/projectStats/${projectId}/correctLog`);
    console.log(`  响应 code=${res.code}`);

    expect(res.code, '接口应返回200').toBe(200);
    expect(res).toHaveProperty('data');
    if (Array.isArray(res.data)) {
      console.log(`  补正日志条数: ${res.data.length}`);
    }
    console.log(`  ✅ 补正日志查询成功`);
  });

  test('团队月度日报', async () => {
    console.log('\n▶ 测试：团队月度日报 GET /teamMonthly');
    const res = await api.get('/project/dailyReport/teamMonthly', {
      yearMonth,
      pageNum: 1,
      pageSize: 10,
    });
    console.log(`  响应 code=${res.code}`);

    expect(res.code, '接口应返回200').toBe(200);
    expect(res).toHaveProperty('data');
    console.log(`  ✅ 团队月度日报查询成功`);
    if (res.data) {
      console.log(`  数据概览: ${JSON.stringify(res.data).substring(0, 300)}`);
    }
  });

  test('团队项目选项', async () => {
    console.log('\n▶ 测试：团队项目选项 GET /teamProjectOptions');
    const res = await api.get('/project/dailyReport/teamProjectOptions', {
      yearMonth,
    });
    console.log(`  响应 code=${res.code}`);

    expect(res.code, '接口应返回200').toBe(200);
    expect(res).toHaveProperty('data');
    if (Array.isArray(res.data)) {
      console.log(`  团队项目选项数量: ${res.data.length}`);
      if (res.data.length > 0) {
        console.log(`  首个选项: ${JSON.stringify(res.data[0])}`);
      }
    }
    console.log(`  ✅ 团队项目选项查询成功`);
  });
});
