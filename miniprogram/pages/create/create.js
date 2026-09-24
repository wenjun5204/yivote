const { request, ensureLogin } = require('../../utils/request')

const SLOT_DEFS = [
  { id: 0, label: '周五晚上' }, { id: 1, label: '周六上午' }, { id: 2, label: '周六下午' },
  { id: 3, label: '周六晚上' }, { id: 4, label: '周日上午' }, { id: 5, label: '周日晚上' },
]

Page({
  data: {
    tpl: 'ball',
    title: '下周球局，哪天人最齐？',
    slots: SLOT_DEFS.map((s) => ({ ...s, selected: [0, 2, 3].includes(s.id) })),
  },

  pickTpl(e) {
    const { tpl, title } = e.currentTarget.dataset
    this.setData({ tpl })
    if (title) this.setData({ title })
  },

  onTitle(e) {
    this.setData({ title: e.detail.value })
  },

  toggleSlot(e) {
    const id = e.currentTarget.dataset.id
    const slots = this.data.slots.map((s) => (s.id === id ? { ...s, selected: !s.selected } : s))
    this.setData({ slots })
  },

  createVote() {
    const selected = this.data.slots.filter((s) => s.selected)
    if (!this.data.title.trim()) {
      wx.showToast({ title: '先写个标题吧', icon: 'none' })
      return
    }
    if (!selected.length) {
      wx.showToast({ title: '至少勾一个时间', icon: 'none' })
      return
    }
    ensureLogin()
      .then(() =>
        request('/api/votes', {
          method: 'POST',
          data: {
            title: this.data.title.trim(),
            type: 'TIME',
            slots: selected.map((s) => ({ label: s.label })),
          },
        })
      )
      .then((vote) => {
        // 创建成功 → 转发卡片到群（三段式标题：缺口 + 数字 + 署名）
        this._createdVote = vote
        wx.showShareMenu({ withShareTicket: true })
        wx.showToast({ title: '建好了，去分享到群', icon: 'success' })
      })
      .catch(() => {})
  },

  onShareAppMessage() {
    const v = this._createdVote || {}
    return {
      title: `就差你了！${v.title || '大家来投票'} · 点开就能投`,
      path: '/pages/vote/vote?voteId=' + (v.voteId || ''),
    }
  },
})
