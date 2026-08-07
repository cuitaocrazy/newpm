/**
 * 合同编号判重 E2E（Issue #32 / specs/020-contract-code-unique）
 *
 * BDD 场景来源：specs/020-contract-code-unique/bdd/contract-code-unique.feature
 * 场景→用例映射：specs/020-contract-code-unique/bdd/coverage.md §二
 *
 * 本文件只落地「单测层原理上够不到」的跨层场景：
 *   - 归一化表达式写在 SQL 里（MySQL TRIM 不去 TAB，表达式对错只有真库知道）
 *   - 清空编号的 <if> 守卫在 ContractMapper.xml 里（mock 掉 mapper 即失效）
 *   - 软删记录不占用编号由 where del_flag='0' + 生成列条件协同保证，Mockito 看不见
 *   - 响应体形状（不泄露对方合同内容）
 *
 * ⚠️ 本套件是破坏性的（会创建与删除合同）。
 *    本项目发生过 e2e 环境变量设错、静默打到真实库并造成损坏的事故，
 *    因此下方 test.beforeAll 内置库身份断言：连的不是本地就直接中止，绝不继续跑。
 *
 * 执行前置：见 specs/020-contract-code-unique/quickstart.md
 */

import { test, expect } from '@playwright/test';
import { setupApi } from './helpers/api-client.js';

let api;

// 本次运行的唯一命名空间，避免与真实数据撞车
const RUN = `${Date.now()}${Math.floor(Math.random() * 1000)}`;
const CODE_BASE = `E2EUNIQ-${RUN}`;
const NAME_PREFIX = `E2E编号判重_${RUN}`;

// 所有本次创建的合同 id，afterAll 统一清理
const created = [];
let customerId = null;

/** 组装一份最小可用的合同 payload */
function contractPayload(code, nameSuffix, extra = {}) {
  return {
    contractName: `${NAME_PREFIX}_${nameSuffix}`,
    contractCode: code,
    contractType: '1',
    contractStatus: '0',
    contractAmount: 10000.0,
    customerId,
    projectIds: [],
    ...extra,
  };
}

/** 新增合同并返回 { body, contractId }；contractId 在失败时为 null */
async function addContract(code, nameSuffix, extra = {}) {
  const name = `${NAME_PREFIX}_${nameSuffix}`;
  const body = await api.post('/project/contract', contractPayload(code, nameSuffix, extra));
  if (body.code !== 200) return { body, contractId: null };

  // 新增接口不回传 id，按名称回查（名称带 RUN 命名空间，唯一）
  const list = await api.get('/project/contract/list', {
    pageNum: 1,
    pageSize: 20,
    contractName: name,
  });
  const row = (list.rows || []).find((r) => r.contractName === name);
  const contractId = row ? row.contractId : null;
  if (contractId) created.push(contractId);
  return { body, contractId };
}

// 刻意不用 describe.serial：serial 下首个用例失败会把后续全部标记为 did-not-run，
// 红阶段就只能看到 1 红 + 14 skipped，无法判断每条场景各自的真实状态。
// 因此每个用例自带前置造数、互不依赖，playwright.config.js 的 workers:1 已保证顺序执行。
test.describe('合同编号判重（Issue #32）', () => {
  test.beforeAll(async () => {
    // ── 安全闸门：库身份断言（连错库直接中止，不继续执行任何写操作）──
    const baseUrl = process.env.E2E_BASE_URL || 'http://localhost:80';
    const isLocal = /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?$/i.test(baseUrl);
    if (!isLocal) {
      throw new Error(
        `[安全闸门] 本套件会创建与删除合同，只允许在本地环境执行。\n` +
          `当前 E2E_BASE_URL = ${baseUrl}\n` +
          `若确需对非本地环境执行，请人工确认后临时修改本断言 —— 不要靠改环境变量绕过。`
      );
    }
    console.log(`✅ 库身份断言通过，目标环境：${baseUrl}`);

    api = await setupApi();

    const customerList = await api.get('/project/customer/list', { pageNum: 1, pageSize: 1 });
    expect(customerList.code, '需要至少一个客户用于造数').toBe(200);
    customerId = customerList.rows.length > 0 ? customerList.rows[0].customerId : null;
    expect(customerId, '库中需要至少一条客户数据').toBeTruthy();

    console.log(`✅ 登录成功，本次运行命名空间 RUN=${RUN}`);
  });

  test.afterAll(async () => {
    // 兜底回查：不能只删 created 数组里的。
    // 拦截类用例是直接 api.post 提交的（不走 addContract），在「判重尚未实现」的红阶段
    // 这些提交会成功却不进 created —— 实测残留过 5 条孤儿合同，还会让后续的
    // 唯一索引前置检查查出假冲突。故按本次运行的名称前缀回查后取并集。
    let sweep = [];
    try {
      const list = await api.get('/project/contract/list', {
        pageNum: 1,
        pageSize: 100,
        contractName: NAME_PREFIX, // 后端是 like 模糊匹配，能覆盖本次全部造数
      });
      sweep = (list.rows || []).map((r) => r.contractId);
    } catch (e) {
      console.error(`⚠️ 兜底回查失败，仅按 created 清理：${e.message}`);
    }

    const toDelete = [...new Set([...created, ...sweep])];

    // 清理本次创建的全部合同；删除失败要报出来而不是吞掉
    const failed = [];
    for (const id of toDelete) {
      try {
        const res = await api.del(`/project/contract/${id}`);
        if (res.code !== 200) failed.push(`${id}(code=${res.code} msg=${res.msg})`);
      } catch (e) {
        failed.push(`${id}(${e.message})`);
      }
    }
    if (failed.length) {
      console.error(`⚠️ 以下合同未能清理，需人工处理：${failed.join(', ')}`);
    } else {
      console.log(`✅ 已清理 ${toDelete.length} 条测试合同（created ${created.length} + 兜底回查 ${sweep.length} 去重后）`);
    }
    await api?.dispose();
  });

  // ═══════════════════ 一、重复编号被当场拦住 ═══════════════════

  test('[1.1a] 新增：编号被占用时保存被拒绝', async () => {
    const code = `${CODE_BASE}-A`;

    const first = await addContract(code, 'A首次');
    expect(first.body.code, '首次使用该编号应当成功').toBe(200);
    expect(first.contractId, '应能回查到刚创建的合同').toBeTruthy();

    const second = await addContract(code, 'A重复');
    expect(second.body.code, '同编号再建应被拒绝').not.toBe(200);
    expect(second.body.msg, '提示应说明编号已存在').toContain('已存在');
  });

  test('[1.5] 拒绝提示是人话：含编号本身，不含堆栈或 SQL', async () => {
    const code = `${CODE_BASE}-A5`;
    const seed = await addContract(code, 'A5占位');
    expect(seed.body.code, '前置：首次占用该编号应成功').toBe(200);

    const res = await api.post('/project/contract', contractPayload(code, '人话提示'));

    expect(res.code).not.toBe(200);
    expect(res.msg, '提示应带上被占用的编号本身').toContain(code);
    expect(res.msg, '提示不应泄露异常类名').not.toMatch(/Exception|java\./i);
    expect(res.msg, '提示不应泄露 SQL').not.toMatch(/select|insert|update|from\s+pm_/i);
  });

  test('[6.2] 判重不泄露对方合同的业务内容', async () => {
    const code = `${CODE_BASE}-A6`;
    const seed = await addContract(code, 'A6占位');
    expect(seed.body.code, '前置：首次占用该编号应成功').toBe(200);

    const res = await api.post('/project/contract', contractPayload(code, '不泄露'));

    expect(res.code).not.toBe(200);
    // 响应体不应携带冲突方的任何业务字段
    expect(res.data, '拒绝响应不应带 data 载荷').toBeFalsy();
    const raw = JSON.stringify(res);
    expect(raw, '不应出现金额字段').not.toMatch(/contractAmount/);
    expect(raw, '不应出现客户字段').not.toMatch(/customerId|customerName/);
    expect(raw, '不应出现签订日期').not.toMatch(/contractSignDate/);
  });

  test('[1.4] 拦截是硬的：连续两次提交同一重复编号都被拒', async () => {
    const code = `${CODE_BASE}-A4`;
    const seed = await addContract(code, 'A4占位');
    expect(seed.body.code, '前置：首次占用该编号应成功').toBe(200);

    const r1 = await api.post('/project/contract', contractPayload(code, '硬拦截1'));
    const r2 = await api.post('/project/contract', contractPayload(code, '硬拦截2'));

    expect(r1.code, '第一次应被拒').not.toBe(200);
    expect(r2.code, '第二次仍应被拒，不存在「确认后放行」').not.toBe(200);
  });

  test('[1.1b/1.3] 编辑：把编号改成别人的编号同样被拒', async () => {
    const occupied = `${CODE_BASE}-B0`;
    const seed = await addContract(occupied, 'B0被占用方');
    expect(seed.body.code, '前置：占位合同应能创建').toBe(200);

    const codeB = `${CODE_BASE}-B`;
    const b = await addContract(codeB, 'B独立');
    expect(b.body.code, '独立编号应能创建').toBe(200);
    expect(b.contractId).toBeTruthy();

    const detail = await api.get(`/project/contract/${b.contractId}`);
    expect(detail.code).toBe(200);

    // 把 B 的编号改成别人已占用的编号
    const res = await api.put('/project/contract', {
      ...detail.data,
      contractCode: occupied,
    });
    expect(res.code, '改成别人的编号应被拒').not.toBe(200);
    expect(res.msg).toContain('已存在');
  });

  test('[4.2] 编辑时不动编号，不会被自己拦住', async () => {
    const codeC = `${CODE_BASE}-C`;
    const c = await addContract(codeC, 'C自排除');
    expect(c.body.code).toBe(200);

    const detail = await api.get(`/project/contract/${c.contractId}`);
    expect(detail.code).toBe(200);

    // 全量表单提交：编号原样带回，只改名称
    const res = await api.put('/project/contract', {
      ...detail.data,
      contractName: `${NAME_PREFIX}_C自排除_改名`,
    });
    expect(res.code, '携带自身编号的全量 PUT 必须成功，否则存量合同全改不动').toBe(200);
  });

  // ═══════════════════ 二、不填编号照样能干活 ═══════════════════

  test('[2.1/2.2] 不填编号可以正常保存，且多份空编号可并存', async () => {
    const e1 = await addContract(null, '空编号1');
    expect(e1.body.code, '不填编号应能保存').toBe(200);

    const e2 = await addContract(null, '空编号2');
    expect(e2.body.code, '第二份空编号合同也应能保存（空值不参与判重）').toBe(200);
  });

  test('[2.3] 空 / 空格 / TAB / 字面「无」都等同于没填编号', async () => {
    const blanks = [
      ['空串', ''],
      ['纯空格', '   '],
      ['纯TAB', '\t'],
      ['空格加TAB', ' \t '],
      ['字面无', '无'],
    ];

    for (const [label, value] of blanks) {
      const r = await api.get('/project/contract/checkContractCodeUnique', {
        contractCode: value,
      });
      expect(r.code).toBe(200);
      expect(r.data, `${label} 应判为「唯一」（不参与判重）`).toBe(true);
    }

    // 两份都填「无」也应能并存
    const n1 = await addContract('无', '无1');
    const n2 = await addContract('无', '无2');
    expect(n1.body.code, '编号填「无」应能保存').toBe(200);
    expect(n2.body.code, '两份「无」应能并存').toBe(200);
  });

  test('[2.4] 把已有编号清空并保存，编号真的被清掉', async () => {
    const codeD = `${CODE_BASE}-D`;
    const d = await addContract(codeD, 'D待清空');
    expect(d.body.code).toBe(200);

    const detail = await api.get(`/project/contract/${d.contractId}`);
    expect(detail.data.contractCode, '创建后应有编号').toBeTruthy();

    const res = await api.put('/project/contract', { ...detail.data, contractCode: '' });
    expect(res.code, '清空编号应保存成功').toBe(200);

    // 回读证明真的清掉了（这条是 <if> 守卫缺陷的探针，单测抓不到）
    const after = await api.get(`/project/contract/${d.contractId}`);
    expect(after.data.contractCode, '清空后回读应为空').toBeFalsy();
  });

  // ═══════════════════ 三、看起来一样的，就是一样的 ═══════════════════

  test('[3.1] 只差看不见的空白，仍算同一个编号', async () => {
    const codeE = `${CODE_BASE}-E`;
    const e = await addContract(codeE, 'E基准');
    expect(e.body.code).toBe(200);

    const variants = [
      ['前导空格', ` ${codeE}`],
      ['尾随空格', `${codeE} `],
      ['尾随TAB', `${codeE}\t`],
      ['前导TAB', `\t${codeE}`],
    ];

    for (const [label, value] of variants) {
      const res = await api.post('/project/contract', contractPayload(value, `E变体_${label}`));
      expect(res.code, `${label} 归一化后与已有编号相同，应被拒`).not.toBe(200);
    }
  });

  test('[3.3] 提交带 TAB 的编号，落库的是干净值', async () => {
    const codeF = `${CODE_BASE}-F`;
    const f = await addContract(`\t${codeF} `, 'F归一化');
    expect(f.body.code, '带空白的新编号本身不重复，应能保存').toBe(200);
    expect(f.contractId).toBeTruthy();

    const detail = await api.get(`/project/contract/${f.contractId}`);
    expect(detail.data.contractCode, '落库编号应已去掉首尾空白与 TAB').toBe(codeF);
  });

  // ═══════════════════ 四、不能误伤正常操作 ═══════════════════

  test('[4.1] 编号是别人编号的前缀时，不算重复', async () => {
    const longCode = `${CODE_BASE}-PREFIX-001`;
    const g = await addContract(longCode, 'G长编号');
    expect(g.body.code).toBe(200);

    // 校验接口层面
    const check = await api.get('/project/contract/checkContractCodeUnique', {
      contractCode: `${CODE_BASE}-PREFIX`,
    });
    expect(check.data, '前缀不应被判为重复（锁死 like 模糊匹配缺陷）').toBe(true);

    // 真实创建层面
    const h = await addContract(`${CODE_BASE}-PREFIX`, 'H前缀');
    expect(h.body.code, '前缀编号应能正常创建').toBe(200);
  });

  test('[4.1b] 校验接口：编辑模式排除自身', async () => {
    const codeI = `${CODE_BASE}-I`;
    const i = await addContract(codeI, 'I排除自身');
    expect(i.body.code).toBe(200);

    const withSelf = await api.get('/project/contract/checkContractCodeUnique', {
      contractCode: codeI,
      contractId: i.contractId,
    });
    expect(withSelf.data, '传入自身 id 应判为唯一').toBe(true);

    const withoutSelf = await api.get('/project/contract/checkContractCodeUnique', {
      contractCode: codeI,
    });
    expect(withoutSelf.data, '不传 id 时该编号已被占用').toBe(false);
  });

  // ═══════════════════ 五、删除之后编号可以再用 ═══════════════════

  test('[5.1] 已删除合同占用过的编号可以被重新使用', async () => {
    const codeJ = `${CODE_BASE}-J`;
    const j1 = await addContract(codeJ, 'J待删除');
    expect(j1.body.code).toBe(200);
    expect(j1.contractId).toBeTruthy();

    const delRes = await api.del(`/project/contract/${j1.contractId}`);
    expect(delRes.code, '删除应成功').toBe(200);
    // 已删除，从清理列表移除避免 afterAll 重复删
    const idx = created.indexOf(j1.contractId);
    if (idx >= 0) created.splice(idx, 1);

    const j2 = await addContract(codeJ, 'J复用');
    expect(j2.body.code, '软删记录不应占用编号（生成列 del_flag 条件的活性验证）').toBe(200);
  });

  // ═══════════════════ 六、既有行为不被误伤 ═══════════════════

  test('[4.4] 按合同编号模糊搜索照常可用', async () => {
    // 判重改精确匹配后，搜索必须仍是模糊的（INV-1）
    const kw = CODE_BASE.slice(0, 12);
    const list = await api.get('/project/contract/list', {
      pageNum: 1,
      pageSize: 20,
      contractCode: kw,
    });
    expect(list.code).toBe(200);
    expect(list.rows.length, '用编号片段应能模糊搜到本次创建的合同').toBeGreaterThan(0);
    for (const row of list.rows) {
      expect(row.contractCode, '搜索结果应包含关键词').toContain(kw);
    }
  });
});
