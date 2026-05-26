/**
 * 付款状态筛选回归测试
 *
 * 背景 (2026-05-26)：付款里程碑页面按「付款状态」筛选返回 0 条。
 * 根因：sys_fkzt 字典值与 pm_payment.payment_status 混入 Tab 制表符
 *      （如 'YTJFP\t\t'）。不加筛选时 dict-tag「带 Tab 对带 Tab」能匹配并正常显示，
 *      但筛选走 GET 参数被 BaseController 的 StringTrimmerEditor trim 掉 Tab 后，
 *      SQL `payment_status in ('YTJFP')` 无法匹配表里的 'YTJFP\t\t' → 0 条。
 *
 * 该测试数据驱动地守住两条线：
 *   1. 数据清洁度：任何状态值若被空白字符再次污染，trim 后筛不到 → 失败
 *   2. 筛选逻辑：listWithContracts 的 paymentStatuses 过滤必须生效
 *
 * 不硬编码具体状态码/条数：先枚举数据里真实存在的付款状态，
 * 再逐个按该状态筛选 listWithContracts，断言能筛出数据。
 *
 * PaymentController: /project/payment
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;

test.describe.serial('付款状态筛选回归', () => {

  test.beforeAll(async () => {
    api = await setupApi();
    console.log('✅ 登录成功，开始付款状态筛选回归测试');
  });

  test.afterAll(async () => {
    await api?.dispose();
  });

  // ─────────────────────────────────────────────
  // 1. 枚举数据中真实存在的付款状态
  // ─────────────────────────────────────────────
  let distinctStatuses = [];

  test('枚举数据中存在的付款状态', async () => {
    // 全量拉取扁平付款列表，收集去重后的 payment_status
    const res = await api.get('/project/payment/list', { pageNum: 1, pageSize: 10000 });
    expect(res.code, '接口应返回200').toBe(200);
    expect(res.rows, '应返回数组').toBeInstanceOf(Array);

    const set = new Set();
    for (const p of res.rows) {
      if (p.paymentStatus != null && p.paymentStatus !== '') {
        set.add(p.paymentStatus);
      }
    }
    distinctStatuses = [...set];
    console.log(`📋 数据中共出现 ${distinctStatuses.length} 种付款状态：${JSON.stringify(distinctStatuses)}`);

    // 至少要有数据才能做后续断言（空库无意义）
    expect(distinctStatuses.length, '应至少存在一种付款状态').toBeGreaterThan(0);
  });

  // ─────────────────────────────────────────────
  // 2. 状态值不得含首尾空白/Tab（数据清洁度直接断言）
  // ─────────────────────────────────────────────
  test('付款状态值不含首尾空白或制表符', async () => {
    const dirty = distinctStatuses.filter(s => s !== s.trim() || /[\t\n\r]/.test(s));
    expect(dirty, `存在被空白/Tab污染的状态值：${JSON.stringify(dirty)}`).toEqual([]);
  });

  // ─────────────────────────────────────────────
  // 3. 逐个状态筛选 listWithContracts，必须能筛出数据
  //    （直接复现原 bug：筛选返回 0 条）
  // ─────────────────────────────────────────────
  test('按每个付款状态筛选里程碑都能命中', async () => {
    for (const status of distinctStatuses) {
      const res = await api.get('/project/payment/listWithContracts', {
        pageNum: 1,
        pageSize: 10,
        paymentStatuses: status,   // 单值 → Spring 绑定为 List<String>
      });
      expect(res.code, `状态[${status}]接口应返回200`).toBe(200);
      expect(
        res.total,
        `按付款状态[${status}]筛选应能命中数据，实际 total=${res.total}（trim/脏数据不一致会导致此处为0）`
      ).toBeGreaterThan(0);
      console.log(`🔎 状态[${status}] 筛选命中 ${res.total} 条`);
    }
  });

  // ─────────────────────────────────────────────
  // 4. 多状态组合筛选：命中数应 >= 任一单状态
  // ─────────────────────────────────────────────
  test('多状态组合筛选生效', async () => {
    test.skip(distinctStatuses.length < 2, '状态种类不足 2 种，跳过组合筛选');

    const [s1, s2] = distinctStatuses;
    const r1 = await api.get('/project/payment/listWithContracts', { pageNum: 1, pageSize: 10, paymentStatuses: s1 });
    // 组合传两个状态（逗号拼接 → Spring split）
    const rBoth = await api.get('/project/payment/listWithContracts', { pageNum: 1, pageSize: 10, paymentStatuses: [s1, s2] });

    expect(rBoth.code).toBe(200);
    expect(
      rBoth.total,
      `组合筛选[${s1},${s2}]命中数(${rBoth.total})应 >= 单状态[${s1}]命中数(${r1.total})`
    ).toBeGreaterThanOrEqual(r1.total);
    console.log(`🔎 组合[${s1},${s2}] 命中 ${rBoth.total} 条，单[${s1}] 命中 ${r1.total} 条`);
  });
});
