# Quickstart: 日报手机端 H5 适配（一期）

**Feature**: 014-daily-report-mobile

## 本地开发

```bash
# 1. 安装新增依赖（仅 vant）
cd ruoyi-ui && npm i

# 2. 起后端（本地端口 8085，见 application.yml）
mvn clean package -Dmaven.test.skip=true
java -Xms512m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar

# 3. 起前端（vite 80 端口，/dev-api → localhost:8085）
cd ruoyi-ui && npm run dev
```

- 桌面浏览器移动仿真调试：Chrome DevTools → Toggle device toolbar → iPhone 13 → 访问 `http://localhost/m/login`
- 本地账号：`admin` / `123456789`（验证码 math 型，已启用）

## 真机联调（同一局域网）

```bash
# vite 已配置 host: true，手机连同一 Wi-Fi 后直接访问电脑 IP
ipconfig getifaddr en0        # 查本机 IP，例如 192.168.1.8
# 手机浏览器打开: http://192.168.1.8/m/login
```

真机必测清单（对应 spec Edge Cases / SC-005）：
- [ ] iOS Safari + Android Chrome + 微信内置浏览器 三内核
- [ ] 软键盘弹起不遮挡正在输入的 Field（textarea 获焦自动滚动）
- [ ] 底部固定保存栏在 iPhone 刘海屏下不被 Home 指示条遮挡（safe-area-inset-bottom）
- [ ] 验证码图片清晰可读、点击可刷新
- [ ] 弱网（DevTools/真机限速 4G）保存失败提示明确、已填内容不丢

## E2E

```bash
# 跑移动端 E2E 前：临时关验证码（跑完恢复）——流程见 memory feedback_e2e_captcha_toggle
npx playwright test e2e-mobile-daily-report.spec.js

# 桌面日报套件回归（SC-003 必跑）
npx playwright test e2e-daily-report-*.spec.js
```

## 构建验证（FR-011 桌面零影响）

```bash
cd ruoyi-ui && npm run build:prod
# 验证 1: dist/static/js 中出现独立的移动 chunk（write-mobile / m-login 等哈希文件）
# 验证 2: 入口 chunk (index-*.js) 体积与改动前相比无明显增长（vant 不得进入入口 chunk）
ls -lah dist/static/js/ | sort -k5 -h -r | head
```

## 上线后验证

- 生产地址 `https://<生产域名>/m/login`（同域同 Ingress，无需新增配置）
- 用一个真实驻场账号走通：登录 → 填写含子任务项目 + 一条假期 → 保存 → 桌面端核对同日数据一致
- 将地址做成二维码发驻场群（运营动作，见 spec Assumptions）
