const { request, ensureLogin } = require('../../utils/request')

const WEEKS = ['一', '二', '三', '四', '五', '六', '日']

Page({
  data: { loaded: false, weeks: WEEKS, profile: {}, month: '', cells: [], selectedDate: '', selectedActivities: [] },

  onLoad(query) {
    this.openGId = query.openGId || ''
    if (!this.openGId) {
      wx.showToast({ title: '从群里的卡片进来，就能看到这个群的日历', icon: 'none' })
    }
    const now = new Date()
    this.ym = { y: now.getFullYear(), m: now.getMonth() + 1 }
    ensureLogin().then(() => this.load()).catch(() => {})
  },

  load() {
    const month = this.ym.y + '-' + String(this.ym.m).padStart(2, '0')
    const path = '/api/groups/' + encodeURIComponent(this.openGId) + '/calendar?month=' + month
    request(path)
      .then((data) => {
        // 按 date 分组
        const byDate = {}
        data.activities.forEach((a) => {
          byDate[a.date] = byDate[a.date] || []
          a.dayNum = a.date.slice(8)
          byDate[a.date].push(a)
        })
        this.byDate = byDate
        this.todayStr = this.formatDate(new Date())
        this.setData({
          loaded: true,
          profile: data.profile,
          month: data.month.replace('-', ' 年 ') + ' 月',
          cells: this.buildCells(data.activities),
        })
      })
      .catch(() => {})
  },

  /** 月格子:2026-09-01 是周二(周一起排 offset=1);每天可带活动/选中态 */
  buildCells(activities) {
    const { y, m } = this.ym
    const daysInMonth = new Date(y, m, 0).getDate()
    const firstDow = (new Date(y, m - 1, 1).getDay() + 6) % 7 // 周一=0
    const actDates = new Set(activities.map((a) => a.date))
    const selDate = this.selectedDate || ''

    const cells = []
    for (let i = 0; i < firstDow; i++) {
      cells.push({ type: 'blank' })
    }
    for (let d = 1; d <= daysInMonth; d++) {
      const dateStr = y + '-' + String(m).padStart(2, '0') + '-' + String(d).padStart(2, '0')
      const isToday = dateStr === this.todayStr
      const isEvent = actDates.has(dateStr)
      const isSel = dateStr === selDate
      let type = 'normal'
      if (isSel) type = 'sel'
      else if (isEvent) type = 'event'
      else if (isToday) type = 'today'
      cells.push({ day: d, date: dateStr, type, chain: (this.byDate[dateStr] || []).length ? this.byDate[dateStr][0].chain : [] })
    }
    return cells
  },

  formatDate(date) {
    return date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0')
  },

  pickDay(e) {
    const cell = this.data.cells[e.currentTarget.dataset.index]
    if (!cell || !cell.date) return
    this.selectedDate = cell.date
    this.setData({
      cells: this.buildCells(Object.values(this.byDate).flat()),
      selectedDate: cell.date,
      selectedActivities: this.byDate[cell.date] || [],
    })
  },

  prevMonth() {
    this.ym.m--
    if (this.ym.m < 1) { this.ym.m = 12; this.ym.y-- }
    this.selectedDate = ''
    this.setData({ selectedDate: '', selectedActivities: [] })
    this.load()
  },

  nextMonth() {
    this.ym.m++
    if (this.ym.m > 12) { this.ym.m = 1; this.ym.y++ }
    this.selectedDate = ''
    this.setData({ selectedDate: '', selectedActivities: [] })
    this.load()
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

  goCreate() {
    wx.navigateTo({ url: '/pages/create/create' })
  },

  onShareAppMessage() {
    const p = this.data.profile
    return {
      title: `${p.name}张罗过 ${p.activityCount} 场活动 · 凝聚力 ${p.cohesionScore} 分`,
      path: '/pages/calendar/calendar?openGId=' + this.openGId,
    }
  },
})
