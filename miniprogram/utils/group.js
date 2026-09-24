/**
 * 群上下文：记住用户最近从哪个群的卡片进来。
 * openGId 由分享票据解出后随分享路径携带（契约「群维度归档」键），
 * 群 tab 据此展示最近一个群的档案。
 */
const LAST_GID_KEY = 'yivote_last_gid'

function remember(openGId) {
  if (!openGId) return
  try {
    wx.setStorageSync(LAST_GID_KEY, openGId)
  } catch (e) {
    // 存储失败不阻塞浏览
  }
}

function last() {
  try {
    return wx.getStorageSync(LAST_GID_KEY) || ''
  } catch (e) {
    return ''
  }
}

module.exports = { remember, last }
