const { request, ensureLogin } = require('../../utils/request')
const group = require('../../utils/group')

Page({
  data: { collect: null, values: [], myValues: [], isOwner: false, filledAll: false, editing: false },

  onLoad(query) {
    this.collectId = query.collectId
    // 群卡片携带的 openGId：上报归档 + 群 tab 记住最近群 + 转发时继续携带
    this.openGId = query.openGId || ''
    if (this.openGId) {
      group.remember(this.openGId)
    }
    ensureLogin()
      .then(() => this.load())
      .catch(() => {})
  },

  load() {
    const qs = this.openGId ? '?openGId=' + encodeURIComponent(this.openGId) : ''
    return request('/api/collects/' + this.collectId + qs).then((collect) => {
      const user = wx.getStorageSync('yivote_user') || {}
      this.setData({
        collect,
        myValues: collect.myValues || [],
        values: collect.myValues || [],
        isOwner: collect.owner.userId === user.userId,
      })
      this.checkFilled()
    })
  },

  onValue(e) {
    const index = e.currentTarget.dataset.index
    this.setData({ ['values[' + index + ']']: e.detail.value })
    this.checkFilled()
  },

  pickChip(e) {
    const { findex, opt } = e.currentTarget.dataset
    this.setData({ ['values[' + findex + ']']: opt })
    this.checkFilled()
  },

  checkFilled() {
    const fields = (this.data.collect && this.data.collect.fields) || []
    const filledAll = fields.every((f, i) => (this.data.values[i] || '').trim())
    this.setData({ filledAll })
  },

  submit() {
    request('/api/collects/' + this.collectId + '/entries', {
      method: 'PUT',
      data: { values: this.data.values },
    })
      .then(() => {
        wx.showToast({ title: '填好了！头像已上屏', icon: 'success' })
        this.load()
      })
      .catch(() => {})
  },

  refill() {
    this.setData({ values: this.data.myValues.slice(), myValues: [] })
  },

  copyExport() {
    request('/api/collects/' + this.collectId + '/export')
      .then((data) => {
        wx.setClipboardData({ data: data.text })
      })
      .catch(() => {})
  },

  onShareAppMessage() {
    const c = this.data.collect || {}
    return {
      title: `表还开着，30 秒就能填完 · ${c.title || ''}`,
      path: '/pages/collect/collect?collectId=' + this.collectId
        + (this.openGId ? '&openGId=' + encodeURIComponent(this.openGId) : ''),
    }
  },
})
