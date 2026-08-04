import router from '@/router'
import { ElMessageBox } from 'element-plus'
import { login, logout, getInfo } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { isHttp, isEmpty } from "@/utils/validate"
import defAva from '@/assets/images/profile.jpg'

interface UserState {
  token: string | undefined
  id: string | number
  name: string
  nickName: string
  avatar: string
  roles: string[]
  permissions: string[]
}

/**
 * 清除各列表页的搜索状态缓存（键名统一以 _search_state 结尾）。
 *
 * 这些快照存在 sessionStorage，生命周期是浏览器标签页——退出登录不会清掉它。
 * 于是同一标签页里换人登录后，新用户首次打开列表会静默套用上一个会话的筛选条件：
 * 数据本身有后端 @DataScope 兜底不会越权，但界面上会出现一个自己没设过的筛选值，
 * 极易被误判成「我的数据不见了」。
 *
 * 统一在登出时清理，覆盖全部 8 个采用该范式的列表页（合同 / 付款 / 项目 / 任务 /
 * 出入库版本 / 手工版本 / 批次问题单 / 非批次问题单），避免逐页维护清理逻辑。
 */
function clearSearchStateCache() {
  try {
    Object.keys(sessionStorage)
      .filter(key => key.endsWith('_search_state'))
      .forEach(key => sessionStorage.removeItem(key))
  } catch {
    // sessionStorage 不可用（隐私模式 / 站点数据被禁）时忽略，不能影响登出流程
  }
}

const useUserStore = defineStore(
  'user',
  {
    state: (): UserState => ({
      token: getToken(),
      id: '',
      name: '',
      nickName: '',
      avatar: '',
      roles: [],
      permissions: []
    }),
    actions: {
      // 登录
      login(userInfo: { username: string; password: string; code: string; uuid: string }) {
        const username = userInfo.username.trim()
        const password = userInfo.password
        const code = userInfo.code
        const uuid = userInfo.uuid
        return new Promise<void>((resolve, reject) => {
          login(username, password, code, uuid).then(res => {
            setToken(res.token)
            this.token = res.token
            resolve()
          }).catch(error => {
            reject(error)
          })
        })
      },
      // 获取用户信息
      getInfo() {
        return new Promise((resolve, reject) => {
          getInfo().then(res => {
            const user = res.user
            let avatar = user.avatar || ''
            if (!isHttp(avatar)) {
              avatar = (isEmpty(avatar)) ? defAva : import.meta.env.VITE_APP_BASE_API + avatar
            }
            if (res.roles && res.roles.length > 0) { // 验证返回的roles是否是一个非空数组
              this.roles = res.roles
              this.permissions = res.permissions
            } else {
              this.roles = ['ROLE_DEFAULT']
            }
            this.id = user.userId || ''
            this.name = user.userName || ''
            this.nickName = user.nickName || ''
            this.avatar = avatar
            /* 初始密码提示 */
            if(res.isDefaultModifyPwd) {
              ElMessageBox.confirm('您的密码还是初始密码，请修改密码！',  '安全提示', {  confirmButtonText: '确定',  cancelButtonText: '取消',  type: 'warning' }).then(() => {
                router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
              }).catch(() => {})
            }
            /* 过期密码提示 */
            if(!res.isDefaultModifyPwd && res.isPasswordExpired) {
              ElMessageBox.confirm('您的密码已过期，请尽快修改密码！',  '安全提示', {  confirmButtonText: '确定',  cancelButtonText: '取消',  type: 'warning' }).then(() => {
                router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
              }).catch(() => {})
            }
            resolve(res)
          }).catch(error => {
            reject(error)
          })
        })
      },
      // 退出系统
      logOut() {
        return new Promise<void>((resolve, reject) => {
          logout().then(() => {
            this.token = ''
            this.roles = []
            this.permissions = []
            removeToken()
            clearSearchStateCache()
            resolve()
          }).catch((error: any) => {
            reject(error)
          })
        })
      }
    }
  })

export default useUserStore
