/**
 * 统一网络层：双通道（与 api/openapi.yaml 契约对齐）
 * - USE_CLOUD=false：wx.request 直连本地后端（开发者工具勾选「不校验合法域名」）
 * - USE_CLOUD=true：wx.cloud.callContainer 走微信云托管（平台注入 X-WX-OPENID，不可伪造）
 * 云托管环境 ID / 服务名：部署后在微信云托管控制台「环境信息」页回填
 */
const USE_CLOUD = false // 骨架期先本地联调；部署后改 true 并回填环境 ID
const CLOUD_ENV = 'REPLACE_WITH_CLOUD_ENV_ID'
const SERVICE_NAME = 'REPLACE_WITH_SERVICE_NAME'

const BASE_URL = 'http://127.0.0.1:8080'
const TOKEN_KEY = 'yivote_token'
const USER_KEY = 'yivote_user'

function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || ''
}

function setToken(token) {
  wx.setStorageSync(TOKEN_KEY, token)
}

function getUser() {
  try {
    return wx.getStorageSync(USER_KEY) || null
  } catch (e) {
    return null
  }
}

function setUser(user) {
  try {
    wx.setStorageSync(USER_KEY, user)
  } catch (e) {
    // 存储失败不阻塞登录
  }
}

/**
 * 登录（对应契约 POST /api/auth/login）：
 * - 云托管通道：callContainer 自动注入 X-WX-OPENID，body 无需 code；
 * - 本地通道：设备稳定标识作为 code（后端视为 openid，仅限内网联调）。
 */
function ensureLogin() {
  if (getToken()) {
    return Promise.resolve(getToken())
  }
  const options = { method: 'POST', data: {} }
  if (!USE_CLOUD) {
    const OPENID_KEY = 'yivote_openid'
    let openid = wx.getStorageSync(OPENID_KEY)
    if (!openid) {
      openid = 'dev-' + Math.random().toString(36).slice(2, 10)
      wx.setStorageSync(OPENID_KEY, openid)
    }
    options.data = { code: openid }
  }
  return request('/api/auth/login', options)
    .then((data) => {
      setToken(data.token)
      setUser({
        userId: data.userId,
        nickname: data.nickname || '',
        avatarUrl: data.avatarUrl || '',
      })
      return data.token
    })
}

function request(path, options) {
  options = options || {}
  if (USE_CLOUD) {
    return requestViaCloud(path, options)
  }
  return requestViaHttp(path, options)
}

/** 云托管通道 */
function requestViaCloud(path, options) {
  return new Promise((resolve, reject) => {
    const header = { 'content-type': 'application/json' }
    const token = options.token || getToken()
    if (token) {
      header['Authorization'] = 'Bearer ' + token
    }
    wx.cloud.callContainer({
      config: { env: CLOUD_ENV },
      service: SERVICE_NAME,
      path: path,
      method: options.method || 'GET',
      data: options.data,
      header,
      success: (res) => handleResponse(res, path, resolve, reject),
      fail: () => {
        toastError('网络不太好，请您稍后再试')
        reject(new Error('network error'))
      },
    })
  })
}

/** 本地直连通道 */
function requestViaHttp(path, options) {
  return new Promise((resolve, reject) => {
    const header = { 'content-type': 'application/json' }
    const token = options.token || getToken()
    if (token) {
      header['Authorization'] = 'Bearer ' + token
    }
    wx.request({
      url: BASE_URL + path,
      method: options.method || 'GET',
      data: options.data,
      header,
      success: (res) => handleResponse(res, path, resolve, reject),
      fail: () => {
        toastError('网络不太好，请您稍后再试')
        reject(new Error('network error'))
      },
    })
  })
}

/** 两通道响应结构一致（statusCode/data），统一按契约错误结构 {code,message} 处理 */
function handleResponse(res, path, resolve, reject) {
  if (res.statusCode >= 200 && res.statusCode < 300) {
    const body = res.data
    if (body && body.code && parseInt(body.code, 10) >= 40000) {
      toastError(body.message || '服务开小差了，请您稍后再试')
      reject(new Error(body.message))
      return
    }
    resolve(body)
  } else {
    console.error('[request] HTTP ' + res.statusCode, { path, body: res.data })
    toastError('服务开小差了，请您稍后再试')
    reject(new Error('HTTP ' + res.statusCode))
  }
}

function toastError(message) {
  wx.showToast({ title: message, icon: 'none', duration: 2200 })
}

module.exports = { request, ensureLogin, getToken, setToken, getUser, setUser, BASE_URL, USE_CLOUD }
