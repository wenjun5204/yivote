const { request, ensureLogin } = require('../../utils/request')
const group = require('../../utils/group')

/** 群 tab：展示最近从卡片进来的群的协作档案（PRD 群主页，v1.3） */
Page({
  data: { state: 'loading', profile: null, gid: '' },

  onShow() {
    const gid = group.last()
    if (!gid) {
      this.setData({ state: 'empty', profile: null, gid: '' })
      return
    }
    this.setData({ state: 'loading', gid })
    ensureLogin()
      .then(() => request('/api/groups/' + encodeURIComponent(gid)))
      .then((profile) => this.setData({ state: 'loaded', profile }))
      .catch(() => this.setData({ state: 'empty', profile: null, gid: '' }))
  },

  onMenu(e) {
    const k = e.currentTarget.dataset.k
    if (k === 'calendar' || k === 'history') {
      wx.navigateTo({ url: '/pages/calendar/calendar?openGId=' + encodeURIComponent(this.data.gid) })
      return
    }
    if (k === 'rename') {
      this.rename()
    }
  },

  rename() {
    const current = (this.data.profile && this.data.profile.name) || ''
    wx.showModal({
      title: '给这个群起个备注名',
      editable: true,
      placeholderText: '如：常打球的这群人',
      content: current === '这个群' ? '' : current,
      success: (res) => {
        if (!res.confirm) return
        const name = (res.content || '').trim()
        if (!name) {
          wx.showToast({ title: '名字不能为空', icon: 'none' })
          return
        }
        request('/api/groups/' + encodeURIComponent(this.data.gid) + '/name', {
          method: 'PUT',
          data: { name },
        })
          .then((profile) => this.setData({ profile }))
          .catch(() => {})
      },
    })
  },
})
