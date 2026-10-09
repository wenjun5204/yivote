const { request, ensureLogin } = require('../../utils/request')

Page({
  data: { items: [], loading: true },

  onLoad() {
    ensureLogin()
      .then(() => Promise.all([
        request('/api/votes/mine'),
        request('/api/collects/mine'),
        request('/api/todos/mine'),
      ]))
      .then(([votes, collects, todos]) => {
        // 「我的事」聚合:投票/收集/待办按更新时间混排(uid 防 id 跨类型撞 key)
        const items = [
          ...(votes.items || []).map((v) => ({ ...v, kind: 'VOTE', refId: v.voteId, uid: 'V' + v.voteId })),
          ...(collects.items || []).map((c) => ({ ...c, kind: 'COLLECT', refId: c.collectId, uid: 'C' + c.collectId })),
          ...(todos.items || []).map((t) => ({ ...t, kind: 'TODO', refId: t.todoId, uid: 'T' + t.todoId })),
        ]
        items.sort((a, b) => (b.updatedAt || '').localeCompare(a.updatedAt || ''))
        this.setData({ items, loading: false })
      })
      .catch(() => this.setData({ loading: false }))
  },

  goCreate() {
    wx.switchTab({ url: '/pages/create/create' })
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
