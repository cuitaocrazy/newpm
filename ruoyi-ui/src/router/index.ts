import { createWebHistory, createRouter } from 'vue-router'
/* Layout */
import Layout from '@/layout/index.vue'

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/system/user'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

// 公共路由
export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login.vue'),
    hidden: true
  },
  {
    path: '/register',
    component: () => import('@/views/register.vue'),
    hidden: true
  },
  {
    path: "/:pathMatch(.*)*",
    component: () => import('@/views/error/404.vue'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401.vue'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: '/index',
    children: [
      {
        path: '/index',
        component: () => import('@/views/index.vue'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile/:activeTab?',
        component: () => import('@/views/system/user/profile/index.vue'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  },
  // 项目详情 / 编辑 / 附件（父路径 /project/list）
  {
    path: '/project/list/detail',
    component: Layout,
    hidden: true,
    children: [
      {
        path: ':projectId(\\d+)',
        component: () => import('@/views/project/project/detail.vue'),
        name: 'ProjectDetail',
        meta: { title: '项目详情', activeMenu: '/project/list' }
      }
    ]
  },
  {
    path: '/project/list/edit',
    component: Layout,
    hidden: true,
    children: [
      {
        path: ':projectId(\\d+)',
        component: () => import('@/views/project/project/edit.vue'),
        name: 'ProjectEdit',
        meta: { title: '编辑项目', activeMenu: '/project/list' }
      }
    ]
  },
  {
    path: '/project/list/bind-contract',
    component: Layout,
    hidden: true,
    children: [
      {
        path: ':projectId(\\d+)',
        component: () => import('@/views/project/project/bind-contract.vue'),
        name: 'ProjectBindContract',
        meta: { title: '关联合同', activeMenu: '/project/list' }
      }
    ]
  },
  {
    path: '/project/list/attachment',
    component: Layout,
    hidden: true,
    children: [
      {
        path: ':projectId(\\d+)',
        component: () => import('@/views/project/project/attachment.vue'),
        name: 'ProjectAttachment',
        meta: { title: '项目附件', activeMenu: '/project/list' }
      }
    ]
  },
  // 合同：新增 / 编辑 / 详情 / 附件（父路径 /htkx/contract）
  {
    path: '/htkx/contract/add',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/project/contract/add.vue'),
        name: 'ContractAdd',
        meta: { title: '新增合同', activeMenu: '/htkx/contract' }
      }
    ]
  },
  {
    path: '/htkx/contract/edit',
    component: Layout,
    hidden: true,
    children: [
      {
        path: ':contractId(\\d+)',
        component: () => import('@/views/project/contract/edit.vue'),
        name: 'ContractEdit',
        meta: { title: '编辑合同', activeMenu: '/htkx/contract' }
      }
    ]
  },
  {
    path: '/htkx/contract/detail',
    component: Layout,
    hidden: true,
    children: [
      {
        path: ':contractId(\\d+)',
        component: () => import('@/views/project/contract/detail.vue'),
        name: 'ContractDetail',
        meta: { title: '合同详情', activeMenu: '/htkx/contract' }
      }
    ]
  },
  {
    path: '/htkx/contract/attachment',
    component: Layout,
    hidden: true,
    children: [
      {
        path: ':contractId(\\d+)',
        component: () => import('@/views/project/contract/attachment.vue'),
        name: 'ContractAttachment',
        meta: { title: '合同附件', activeMenu: '/htkx/contract' }
      }
    ]
  },
  // 款项：新增 / 编辑 / 详情 / 附件（父路径 /htkx/payment）
  {
    path: '/htkx/payment/add',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/project/payment/form.vue'),
        name: 'PaymentAdd',
        meta: { title: '新增款项', activeMenu: '/htkx/payment' }
      }
    ]
  },
  {
    path: '/htkx/payment/edit',
    component: Layout,
    hidden: true,
    children: [
      {
        path: ':paymentId(\\d+)',
        component: () => import('@/views/project/payment/form.vue'),
        name: 'PaymentEdit',
        meta: { title: '编辑款项', activeMenu: '/htkx/payment' }
      }
    ]
  },
  {
    path: '/htkx/payment/detail',
    component: Layout,
    hidden: true,
    children: [
      {
        path: ':paymentId(\\d+)',
        component: () => import('@/views/project/payment/detail.vue'),
        name: 'PaymentDetail',
        meta: { title: '付款里程碑详情', activeMenu: '/htkx/payment' }
      }
    ]
  },
  {
    path: '/htkx/payment/attachment',
    component: Layout,
    hidden: true,
    children: [
      {
        path: ':paymentId(\\d+)',
        component: () => import('@/views/project/payment/attachment.vue'),
        name: 'PaymentAttachment',
        meta: { title: '款项附件', activeMenu: '/htkx/payment' }
      }
    ]
  },
  // 收入确认详情（父路径 /revenue）
  {
    path: '/revenue/company/detail',
    component: Layout,
    hidden: true,
    children: [
      {
        path: ':projectId(\\d+)',
        component: () => import('@/views/revenue/company/detail.vue'),
        name: 'CompanyRevenueDetail',
        meta: { title: '收入确认详情', activeMenu: '/revenue/company' }
      }
    ]
  },
  {
    path: '/revenue/team/detail',
    component: Layout,
    hidden: true,
    children: [
      {
        path: ':projectId(\\d+)',
        component: () => import('@/views/revenue/team/detail.vue'),
        name: 'TeamRevenueDetail',
        meta: { title: '团队收入确认详情', activeMenu: '/revenue/team' }
      }
    ]
  },
  // 任务：编辑 / 详情（父路径 /task/subproject）
  {
    path: '/task/subproject/edit',
    component: Layout,
    hidden: true,
    children: [
      {
        path: ':taskId(\\d+)',
        component: () => import('@/views/project/subproject/edit.vue'),
        name: 'TaskEdit',
        meta: { title: '编辑任务', activeMenu: '/task/subproject' }
      }
    ]
  },
  {
    path: '/task/subproject/detail',
    component: Layout,
    hidden: true,
    children: [
      {
        path: ':taskId(\\d+)',
        component: () => import('@/views/project/subproject/detail.vue'),
        name: 'TaskDetail',
        meta: { title: '任务详情', activeMenu: '/task/subproject' }
      }
    ]
  }
]

// 动态路由，基于用户权限动态去加载
export const dynamicRoutes = [
  {
    path: '/system/user-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:user:edit'],
    children: [
      {
        path: 'role/:userId(\\d+)',
        component: () => import('@/views/system/user/authRole.vue'),
        name: 'AuthRole',
        meta: { title: '分配角色', activeMenu: '/system/user' }
      }
    ]
  },
  {
    path: '/system/role-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:role:edit'],
    children: [
      {
        path: 'user/:roleId(\\d+)',
        component: () => import('@/views/system/role/authUser.vue'),
        name: 'AuthUser',
        meta: { title: '分配用户', activeMenu: '/system/role' }
      }
    ]
  },
  {
    path: '/system/dict-data',
    component: Layout,
    hidden: true,
    permissions: ['system:dict:list'],
    children: [
      {
        path: 'index/:dictId(\\d+)',
        component: () => import('@/views/system/dict/data.vue'),
        name: 'Data',
        meta: { title: '字典数据', activeMenu: '/system/dict' }
      }
    ]
  },
  {
    path: '/monitor/job-log',
    component: Layout,
    hidden: true,
    permissions: ['monitor:job:list'],
    children: [
      {
        path: 'index/:jobId(\\d+)',
        component: () => import('@/views/monitor/job/log.vue'),
        name: 'JobLog',
        meta: { title: '调度日志', activeMenu: '/monitor/job' }
      }
    ]
  },
  {
    path: '/tool/gen-edit',
    component: Layout,
    hidden: true,
    permissions: ['tool:gen:edit'],
    children: [
      {
        path: 'index/:tableId(\\d+)',
        component: () => import('@/views/tool/gen/editTable.vue'),
        name: 'GenEdit',
        meta: { title: '修改生成配置', activeMenu: '/tool/gen' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
})

export default router
