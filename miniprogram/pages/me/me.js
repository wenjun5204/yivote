const { request, ensureLogin } = require('../../utils/request')

/** 我的 tab:我发起的 / 我参与的(三个 mine 接口按 myRole 分桶;聚合写法复用 index 页) */
Page({
  data: { user: null, owned: [], joined: [], loading: true },

  onLoad() {
    ensureLogin()
      .then(() => this.load())
      .catch(() => this.setData({ loading: false }))
  },

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
        // 混排 + myRole 分桶:OWNER=我发起;VOTER/FILLER/CLAIMER=我参与(uid 防 id 跨类型撞 key)
        const items = [
          ...(votes.items || []).map((v) => ({ ...v, kind: 'VOTE', refId: v.voteId, uid: 'V' + v.voteId })),
          ...(collects.items || []).map((c) => ({ ...c, kind: 'COLLECT', refId: c.collectId, uid: 'C' + c.collectId })),
          ...(todos.items || []).map((t) => ({ ...t, kind: 'TODO', refId: t.todoId, uid: 'T' + t.todoId })),
        ]
        items.sort((a, b) => (b.updatedAt || '').localeCompare(a.updatedAt || ''))
        this.setData({
          user: wx.getStorageSync('yivote_user') || null,
          owned: items.filter((i) => i.myRole === 'OWNER'),
          joined: items.filter((i) => i.myRole !== 'OWNER'),
          loading: false,
        })
        this.loading = false
      })
      .catch(() => {
        this.setData({ loading: false })
        this.loading = false
      })
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
