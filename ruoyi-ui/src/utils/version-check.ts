/**
 * 运行时版本检测 —— 发版后自动静默刷新
 *
 * 原理：Vite 构建产物文件名带内容哈希，发版后 index.html 引用的
 * /static/js|css/*.js 文件名必变。本模块定时（及标签页切回前台时）拉取
 * 服务器最新 index.html，提取其中的哈希资源清单作为「版本指纹」，与启动时
 * 记录的基线比对；一旦不同即判定已发版，直接 location.reload() 静默刷新。
 *
 * 这样即便用户开着标签页长时间不登录/不刷新，也能自动吃到最新版本，
 * 与服务端「index.html 禁缓存」配合，构成发版自动生效的双保险。
 *
 * 注意：dev 环境（vite）入口不带哈希，extractFingerprint 返回 null，
 * 本模块自动空转，不会误刷新。
 */

// 检测间隔：5 分钟
const CHECK_INTERVAL = 5 * 60 * 1000

// 启动时记录的版本指纹基线
let baseline: string | null = null
// 防止极端情况下短时间内重复触发 reload
let reloading = false

/** 从一段 index.html 文本中提取带哈希的静态资源清单作为版本指纹 */
function extractFingerprint(html: string): string | null {
  const matches = html.match(/\/static\/(?:js|css)\/[^"')\s]+/g)
  if (!matches || matches.length === 0) return null
  // 去重 + 排序，保证同一版本指纹稳定
  return Array.from(new Set(matches)).sort().join('|')
}

/** 拉取服务器最新 index.html 的版本指纹（绕过缓存） */
async function fetchLatestFingerprint(): Promise<string | null> {
  try {
    const res = await fetch('/index.html', { cache: 'no-store' })
    if (!res.ok) return null
    return extractFingerprint(await res.text())
  } catch {
    // 网络异常忽略，下次再试，绝不影响正常使用
    return null
  }
}

async function check(): Promise<void> {
  if (reloading) return
  const latest = await fetchLatestFingerprint()
  if (!latest) return
  if (baseline === null) {
    // 首次拿到服务器指纹时建立基线（兜底，正常应已在 start 时建立）
    baseline = latest
    return
  }
  if (latest !== baseline) {
    reloading = true
    window.location.reload()
  }
}

/** 启动版本检测，应在应用挂载后调用 */
export function startVersionCheck(): void {
  // 用当前已加载文档作为基线，避免启动时多发一次请求
  baseline = extractFingerprint(document.documentElement.outerHTML)

  // 定时轮询
  window.setInterval(check, CHECK_INTERVAL)

  // 标签页从后台切回前台时立即检测 —— 用户离开再回来，最容易正好赶上发版
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') check()
  })
}
