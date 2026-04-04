/**
 * E2E 测试公共 API 客户端
 * 使用手动创建的 APIRequestContext，避免 beforeAll fixture 不能跨 test 复用的问题
 */

import { request as playwrightRequest } from '@playwright/test';

const BASE_URL = 'http://localhost:80';

/**
 * 创建带认证的 API 客户端（手动管理 APIRequestContext 生命周期）
 * 用法:
 *   let api;
 *   test.beforeAll(async () => { api = await setupApi(); });
 *   test.afterAll(async () => { await api.dispose(); });
 */
export async function setupApi(username = 'admin', password = '123456789') {
  const ctx = await playwrightRequest.newContext({ baseURL: BASE_URL });

  // 登录获取 token
  const resp = await ctx.post(`/dev-api/login`, {
    data: { username, password }
  });
  const body = await resp.json();
  if (body.code !== 200) {
    await ctx.dispose();
    throw new Error(`Login failed: ${body.msg}`);
  }
  const token = body.token;
  const headers = { Authorization: `Bearer ${token}` };

  return {
    token,

    async get(path, params = {}) {
      const query = new URLSearchParams(params).toString();
      const url = query ? `/dev-api${path}?${query}` : `/dev-api${path}`;
      const resp = await ctx.get(url, { headers });
      return resp.json();
    },

    async post(path, data = {}) {
      const resp = await ctx.post(`/dev-api${path}`, { headers, data });
      return resp.json();
    },

    async put(path, data = {}) {
      const resp = await ctx.put(`/dev-api${path}`, { headers, data });
      return resp.json();
    },

    async del(path) {
      const resp = await ctx.delete(`/dev-api${path}`, { headers });
      return resp.json();
    },

    async dispose() {
      await ctx.dispose();
    }
  };
}
