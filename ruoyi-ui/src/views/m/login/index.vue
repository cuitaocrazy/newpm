<template>
  <div class="m-login">
    <div class="m-login__header">
      <h2 class="m-login__title">{{ title }}</h2>
      <p class="m-login__sub">日报填写 · 移动端</p>
    </div>

    <van-form @submit="handleLogin">
      <van-cell-group inset>
        <van-field
          v-model="loginForm.username"
          name="username"
          label="账号"
          placeholder="请输入账号"
          clearable
          :rules="[{ required: true, message: '请输入您的账号' }]"
        />
        <van-field
          v-model="loginForm.password"
          type="password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          clearable
          :rules="[{ required: true, message: '请输入您的密码' }]"
        />
        <van-field
          v-if="captchaEnabled"
          v-model="loginForm.code"
          name="code"
          label="验证码"
          placeholder="请输入验证码"
          clearable
          :rules="[{ required: true, message: '请输入验证码' }]"
        >
          <template #button>
            <img :src="codeUrl" class="m-login__captcha" alt="验证码" @click="getCode" />
          </template>
        </van-field>
      </van-cell-group>

      <div class="m-login__remember">
        <van-checkbox v-model="loginForm.rememberMe" shape="square" icon-size="16px">记住密码</van-checkbox>
      </div>

      <div class="m-login__submit">
        <van-button round block type="primary" native-type="submit" :loading="loading" loading-text="登 录 中...">
          登 录
        </van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup name="MobileLogin">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Form as VanForm,
  Field as VanField,
  CellGroup as VanCellGroup,
  Checkbox as VanCheckbox,
  Button as VanButton,
  showToast
} from 'vant'
import 'vant/lib/index.css'
import Cookies from 'js-cookie'
import { getCodeImg } from '@/api/login'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import useUserStore from '@/store/modules/user'

const title = import.meta.env.VITE_APP_TITLE
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loginForm = ref({
  username: '',
  password: '',
  rememberMe: false,
  code: '',
  uuid: ''
})

const codeUrl = ref('')
const captchaEnabled = ref(true)
const loading = ref(false)
const redirect = ref(undefined)

watch(route, (newRoute) => {
  redirect.value = (newRoute.query && newRoute.query.redirect)
}, { immediate: true })

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = 'data:image/gif;base64,' + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get('username')
  const password = Cookies.get('password')
  const rememberMe = Cookies.get('rememberMe')
  loginForm.value.username = username === undefined ? loginForm.value.username : username
  loginForm.value.password = password === undefined ? loginForm.value.password : decrypt(password)
  loginForm.value.rememberMe = rememberMe === undefined ? false : Boolean(rememberMe)
}

async function handleLogin() {
  loading.value = true
  // 记住我：与桌面 login.vue 同一套 Cookie + encrypt 方案，双端互通
  if (loginForm.value.rememberMe) {
    Cookies.set('username', loginForm.value.username, { expires: 30 })
    Cookies.set('password', encrypt(loginForm.value.password), { expires: 30 })
    Cookies.set('rememberMe', loginForm.value.rememberMe, { expires: 30 })
  } else {
    Cookies.remove('username')
    Cookies.remove('password')
    Cookies.remove('rememberMe')
  }
  try {
    await userStore.login(loginForm.value)
    showToast({ type: 'success', message: '登录成功' })
    router.push({ path: redirect.value || '/m' })
  } catch {
    // 登录失败：request 层已有错误提示；刷新验证码
    loading.value = false
    if (captchaEnabled.value) getCode()
  }
}

getCode()
getCookie()
</script>

<style scoped>
.m-login {
  min-height: 100vh;
  background: #f7f8fa;
  padding-top: 12vh;
}
.m-login__header {
  text-align: center;
  margin-bottom: 32px;
}
.m-login__title {
  font-size: 22px;
  color: #323233;
  margin: 0 0 8px;
}
.m-login__sub {
  font-size: 14px;
  color: #969799;
  margin: 0;
}
.m-login__captcha {
  height: 32px;
  vertical-align: middle;
}
.m-login__remember {
  padding: 16px 28px 0;
}
.m-login__submit {
  margin: 24px 16px;
}
/* 触控目标 ≥44px（SC-005） */
.m-login :deep(.van-field) {
  min-height: 44px;
  align-items: center;
}
.m-login :deep(.van-button--normal) {
  height: 44px;
}
</style>
