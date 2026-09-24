const { request, ensureLogin } = require('../../utils/request')

const SLOT_DEFS = [
  { id: 0, label: '周五晚上' }, { id: 1, label: '周六上午' }, { id: 2, label: '周六下午' },
  { id: 3, label: '周六晚上' }, { id: 4, label: '周日上午' }, { id: 5, label: '周日晚上' },
]

/** 场景模板:标题 + 默认勾选时段(「30 秒发起」的核心——选完模板基本不用再动) */
const TEMPLATES = {
  ball:   { title: '下周球局，哪天人最齐？', slots: [0, 2, 3] },        // 约球:周末三个半天
  dinner: { title: '下次聚餐，大家哪天方便？', slots: [0, 3, 5] },      // 聚餐/团建:晚间为主
  family: { title: '家里聚餐，哪天大家有空？', slots: [1, 2, 4] },      // 家庭:白天为主
  custom: null,
}

Page({
  data: {
    tpl: 'ball',
    title: '下周球局，哪天人最齐？',
    slots: SLOT_DEFS.map((s) => ({ ...s, selected: [0, 2, 3].includes(s.id) })),
  },

  pickTpl(e) {
    const tpl = e.currentTarget.dataset.tpl
    const template = TEMPLATES[tpl]
    this.setData({ tpl })
    if (template) {
      this.setData({
        title: template.title,
        slots: this.data.slots.map((s) => ({ ...s, selected: template.slots.includes(s.id) })),
      })
    }
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
        // 创建成功 → 直达详情页；分享卡片在详情页触发（三段式标题）
        wx.navigateTo({ url: '/pages/vote/vote?voteId=' + vote.voteId })
      })
      .catch(() => {})
  },

})
