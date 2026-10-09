const { request, ensureLogin } = require('../../utils/request')

/** 高频事项模板一键带入（PRD §7B.2 系统预置） */
const TPL_ITEMS = ['订场', '买水', '带装备', '拍照', '定餐厅']
const CAP_OPTIONS = ['限 1 人', '2 人', '3 人']

Page({
  data: {
    title: '',
    tplItems: TPL_ITEMS,
    tplPicked: [],
    items: [],
    capOptions: CAP_OPTIONS,
  },

  onLoad(query) {
    // 两条入口：投票定档页「给这顿分个工」（带 voteId+title 自动带入）/ 发起 tab 独立发起
    this.voteId = query.voteId || ''
    this.openGId = query.openGId || ''
    this.setData({ title: query.title || '' })
  },

  onTitle(e) {
    this.setData({ title: e.detail.value })
  },

  toggleTpl(e) {
    const name = e.currentTarget.dataset.name
    let picked = this.data.tplPicked.slice()
    let items = this.data.items.slice()
    if (picked.includes(name)) {
      picked = picked.filter((x) => x !== name)
      items = items.filter((x) => x.title !== name)
    } else {
      picked.push(name)
      items.push({ title: name, capIndex: 0 })
    }
    this.setData({ tplPicked: picked, items })
  },

  onItemTitle(e) {
    const i = e.currentTarget.dataset.index
    this.setData({ ['items[' + i + '].title']: e.detail.value })
  },

  onCap(e) {
    const i = e.currentTarget.dataset.index
    this.setData({ ['items[' + i + '].capIndex']: Number(e.detail.value) })
  },

  addItem() {
    if (this.data.items.length >= 10) {
      wx.showToast({ title: '事项最多 10 个', icon: 'none' })
      return
    }
    this.setData({ items: [...this.data.items, { title: '', capIndex: 0 }] })
  },

  removeItem(e) {
    const i = e.currentTarget.dataset.index
    const items = this.data.items.filter((_, idx) => idx !== i)
    const picked = this.data.tplPicked.filter((name) => name !== (this.data.items[i] || {}).title)
    this.setData({ items, tplPicked: picked })
  },

  createTodo() {
    const title = this.data.title.trim()
    if (!title) {
      wx.showToast({ title: '先写个活动名', icon: 'none' })
      return
    }
    const items = []
    for (const it of this.data.items) {
      const t = (it.title || '').trim()
      if (!t) {
        wx.showToast({ title: '有事项名没填', icon: 'none' })
        return
      }
      items.push({ title: t, capacity: it.capIndex + 1 })
    }
    if (!items.length) {
      wx.showToast({ title: '至少加一个事项', icon: 'none' })
      return
    }
    const data = { title, items, openGId: this.openGId || undefined }
    if (this.voteId) {
      data.sourceType = 'VOTE'
      data.sourceId = this.voteId
    }
    ensureLogin()
      .then(() => request('/api/todos', { method: 'POST', data }))
      .then((todo) => {
        wx.redirectTo({ url: '/pages/todo/todo?todoId=' + todo.todoId + (this.openGId ? '&openGId=' + encodeURIComponent(this.openGId) : '') })
      })
      .catch(() => {})
  },
})
