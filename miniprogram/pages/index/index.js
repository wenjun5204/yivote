const { request, ensureLogin } = require('../../utils/request')
const group = require('../../utils/group')

Page({
  data: { items: [], loading: true },

  onLoad() {
    ensureLogin()
      .then(() => this.load())
      .catch(() => this.setData({ loading: false }))
  },

  // 每次回到页面刷新:从详情页返回时列表数字才不滞后
  onShow() {
    if (!wx.getStorageSync('yivote_token')) return
    this.load()
  },

  load() {
    if (this.loading) return
    this.loading = true
    Promise.all([
      request('/api/votes/mine'),
      request('/api/collects/mine'),
      request('/api/todos/mine'),
    ])
      .then(([votes, collects, todos]) => {
        // 「我的事」聚合:投票/收集/待办按更新时间混排(uid 防 id 跨类型撞 key)
        const items = [
          ...(votes.items || []).map((v) => ({ ...v, kind: 'VOTE', refId: v.voteId, uid: 'V' + v.voteId })),
          ...(collects.items || []).map((c) => ({ ...c, kind: 'COLLECT', refId: c.collectId, uid: 'C' + c.collectId })),
          ...(todos.items || []).map((t) => ({ ...t, kind: 'TODO', refId: t.todoId, uid: 'T' + t.todoId })),
        ]
        items.sort((a, b) => (b.updatedAt || '').localeCompare(a.updatedAt || ''))
        this.setData({ items, loading: false })
        this.loading = false
      })
      .catch(() => {
        this.setData({ loading: false })
        this.loading = false
      })
  },

  goCreate() {
    wx.navigateTo({ url: '/pages/create/create' })
  },

  // 「一件事的全流程」四入口:凑时间/凑分工/沉淀/群资产(发起已从 tab 降为 hero 入口)
  goChain(e) {
    const step = e.currentTarget.dataset.step
    if (step === 'vote') {
      wx.navigateTo({ url: '/pages/create/create' })
    } else if (step === 'todo') {
      wx.navigateTo({ url: '/pages/todo-create/todo-create' })
    } else if (step === 'calendar') {
      const gid = group.last()
      if (!gid) {
        wx.showToast({ title: '从群里的卡片进来，日历会长在这里', icon: 'none' })
        return
      }
      wx.navigateTo({ url: '/pages/calendar/calendar?openGId=' + encodeURIComponent(gid) })
    } else if (step === 'group') {
      wx.switchTab({ url: '/pages/group/group' })
    }
  },

  openActivity(e) {
    const { kind, ref } = e.currentTarget.dataset
    const url = kind === 'VOTE'
      ? '/pages/vote/vote?voteId=' + ref
      : kind === 'COLLECT'
        ? '/pages/collect/collect?collectId=' + ref
        : '/pages/todo/todo?todoId=' + ref
    wx.navigateTo({ url })
  },
})
