/**
 * 后端接口封装。
 *
 * 开发期 BASE 为空串，请求 /api/** 由 Vite 代理到 Spring Boot（http://127.0.0.1:8080）。
 * 生产部署时在 .env.production 里设置 VITE_API_BASE=http://后端地址:8080 即可。
 */
const BASE = (import.meta.env.VITE_API_BASE || '').replace(/\/+$/, '')

/** 统一解析响应体；后端约定 HTTP 200 + body 里的 code 表示业务结果 */
async function parse(response) {
  let body = null
  try {
    body = await response.json()
  } catch (error) {
    body = null
  }
  if (!body || typeof body.code === 'undefined') {
    throw new Error('服务器返回异常（HTTP ' + response.status + '）')
  }
  return body
}

async function request(path, options) {
  let response
  try {
    response = await fetch(BASE + path, options)
  } catch (error) {
    throw new Error('无法连接服务器，请确认后端已启动')
  }
  return parse(response)
}

function post(path, payload) {
  return request(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })
}

function get(path) {
  return request(path, { method: 'GET' })
}

/** 注册：POST /api/register */
export function registerUser(username, password) {
  return post('/api/register', { username, password })
}

/** 登录：POST /api/login */
export function loginUser(username, password) {
  return post('/api/login', { username, password })
}

/** 修改密码：POST /api/resetPwd */
export function changePassword(username, oldPassword, newPassword) {
  return post('/api/resetPwd', { username, oldPassword, newPassword })
}

/** 反馈 / 投诉作者：POST /api/feedback */
export function sendFeedback(content, username) {
  return post('/api/feedback', { content, username: username || '' })
}

/** 照片墙列表：GET /api/photos（带 username 只看某个人的） */
export function listPhotos(username) {
  const query = username ? '?username=' + encodeURIComponent(username) : ''
  return get('/api/photos' + query)
}

/** 上传图片：POST /api/photos（multipart 表单，图片存在数据库里） */
export function uploadPhoto(file, username) {
  const form = new FormData()
  form.append('file', file)
  form.append('username', username)
  return request('/api/photos', { method: 'POST', body: form })
}
