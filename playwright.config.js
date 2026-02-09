import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright 配置文件
 * 用于项目管理功能的 E2E 测试
 */
export default defineConfig({
  // 测试目录
  testDir: './tests',

  // 测试超时时间（30秒）
  timeout: 30 * 1000,

  // 每个测试的重试次数
  retries: 2,

  // 并行执行的 worker 数量
  workers: 1,

  // 测试报告配置
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['list'],
    ['json', { outputFile: 'test-results.json' }]
  ],

  // 全局配置
  use: {
    // 基础 URL
    baseURL: 'http://localhost:80',

    // 浏览器上下文选项
    viewport: { width: 1920, height: 1080 },

    // 操作超时时间
    actionTimeout: 10 * 1000,

    // 导航超时时间
    navigationTimeout: 30 * 1000,

    // 截图配置
    screenshot: 'only-on-failure',

    // 视频录制
    video: 'retain-on-failure',

    // 追踪配置
    trace: 'retain-on-failure',

    // 忽略 HTTPS 错误
    ignoreHTTPSErrors: true,

    // 浏览器语言
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai',
  },

  // 项目配置 - 不同浏览器
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },

    // {
    //   name: 'firefox',
    //   use: { ...devices['Desktop Firefox'] },
    // },

    // {
    //   name: 'webkit',
    //   use: { ...devices['Desktop Safari'] },
    // },

    // 移动端测试
    // {
    //   name: 'Mobile Chrome',
    //   use: { ...devices['Pixel 5'] },
    // },
  ],

  // Web Server 配置（可选）
  // 如果需要在测试前自动启动前端服务
  // webServer: {
  //   command: 'npm run dev',
  //   port: 80,
  //   timeout: 120 * 1000,
  //   reuseExistingServer: !process.env.CI,
  // },
});
