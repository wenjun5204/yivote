const { request, ensureLogin } = require('../../utils/request')

Page({
  data: { items: [], loading: true },

  onLoad() {
    ensureLogin()
      .then(() => request('/api/votes/mine'))
      .then((data) => this.setData({ items: data.items || [], loading: false }))
      .catch(() => this.setData({ loading: false }))
  },

  goCreate() {
    wx.switchTab({ url: '/pages/create/create' })
  },

  openVote(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/vote/vote?voteId=' + id })
  },
})
