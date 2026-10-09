const { request, ensureLogin } = require('../../utils/request')
const group = require('../../utils/group')

Page({
  data: { todo: null, isOwner: false },

  onLoad(query) {
    this.todoId = query.todoId
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
    return request('/api/todos/' + this.todoId + qs).then((todo) => {
      const user = wx.getStorageSync('yivote_user') || {}
      this.setData({ todo, isOwner: todo.owner.userId === user.userId })
    })
  },

  claim(e) {
    const itemId = e.currentTarget.dataset.id
    request('/api/todos/' + this.todoId + '/items/' + itemId + '/claim', { method: 'POST', data: {} })
      .then(() => {
        wx.showToast({ title: '记上了，就等你了', icon: 'success' })
        this.load()
      })
      .catch(() => {})
  },

  done(e) {
    const itemId = e.currentTarget.dataset.id
    request('/api/todos/' + this.todoId + '/items/' + itemId + '/done', { method: 'POST', data: {} })
      .then(() => {
        wx.showToast({ title: '搞定！辛苦啦', icon: 'success' })
        this.load()
      })
      .catch(() => {})
  },

  /** 转给别人：放回池子，全群可见（不放回唯一键是确认，不是挽留） */
  release(e) {
    const itemId = e.currentTarget.dataset.id
    wx.showModal({
      title: '放回池子换个人搭把手？',
      content: '放回后这项会重新空出来，群里都能看到。',
      confirmText: '放回',
      success: (res) => {
        if (!res.confirm) return
        request('/api/todos/' + this.todoId + '/items/' + itemId + '/release', { method: 'POST', data: {} })
          .then(() => {
            wx.showToast({ title: '已放回，等下一位', icon: 'none' })
            this.load()
          })
          .catch(() => {})
      },
    })
  },

  /** 发起人盯催：复制召唤话术（质问感红线——从群体需要出发，不从「你欠群体」出发） */
  nudge() {
    const s = (this.data.todo.summary) || {}
    const text = `${this.data.todo.title}还差 ${s.open || 0} 件事没人领，谁能搭把手？点卡片就能认领，30 秒的事`
    wx.setClipboardData({
      data: text,
      success: () => wx.showToast({ title: '话术已复制，去群里喊一声', icon: 'none' }),
    })
  },

  onShareAppMessage() {
    const t = this.data.todo || {}
    const s = t.summary || {}
    const path = '/pages/todo/todo?todoId=' + this.todoId
      + (this.openGId ? '&openGId=' + encodeURIComponent(this.openGId) : '')
    if (t.status === 'DONE') {
      return { title: `${t.title} · ${s.total} 件事全办成了`, path }
    }
    if (!s.open) {
      return { title: `${t.title} · ${s.total} 件事都有着落了`, path }
    }
    return { title: `${t.title} · ${s.open} 件事还没人领，位置给你留着`, path }
  },
})
