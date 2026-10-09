const { request, ensureLogin } = require('../../utils/request')
const group = require('../../utils/group')

/** 群 tab：展示最近从卡片进来的群的协作档案（PRD 群主页，v1.3）；菜单对齐优化版原型 index-optimized */
Page({
  data: { state: 'loading', profile: null, gid: '', todos: [], openTotal: 0 },

  onShow() {
    const gid = group.last()
    if (!gid) {
      this.setData({ state: 'empty', profile: null, gid: '', todos: [], openTotal: 0 })
      return
    }
    this.setData({ state: 'loading', gid })
    ensureLogin()
      .then(() => Promise.all([
        request('/api/groups/' + encodeURIComponent(gid)),
        // 「进行中的分工」:本群 OPEN 待办(DONE 的已沉淀进群日历)
        request('/api/groups/' + encodeURIComponent(gid) + '/todos'),
      ]))
      .then(([profile, todoResp]) => {
        const todos = todoResp.items || []
        const openTotal = todos.reduce((sum, t) => sum + ((t.summary && t.summary.open) || 0), 0)
        this.setData({ state: 'loaded', profile, todos, openTotal })
      })
      .catch(() => this.setData({ state: 'empty', profile: null, gid: '', todos: [], openTotal: 0 }))
  },

  onMenu(e) {
    const k = e.currentTarget.dataset.k
    if (k === 'calendar') {
      wx.navigateTo({ url: '/pages/calendar/calendar?openGId=' + encodeURIComponent(this.data.gid) })
      return
    }
    if (k === 'rename') {
      this.rename()
    }
  },

  openTodo(e) {
    const todoId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/todo/todo?todoId=' + todoId + '&openGId=' + encodeURIComponent(this.data.gid),
    })
  },

  // 把群战功发到群里:open-type=share,见 onShareAppMessage
  shareGlory() {},

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

  // 战功卡:落群日历页(tab 页不能带 query 作分享落地页)
  onShareAppMessage() {
    const p = this.data.profile || {}
    return {
      title: `${p.name || '这个群'}张罗过 ${p.activityCount || 0} 场活动 · ${p.attendeeTotal || 0} 人次到场`,
      path: '/pages/calendar/calendar?openGId=' + encodeURIComponent(this.data.gid),
    }
  },
})
