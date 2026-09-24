Page({
  onMenu(e) {
    const tips = {
      calendar: '群日历 v1.3 上线（数据依赖：等活动攒起来）',
      history: '历史活动列表（随群日历一起上）',
      rename: '群名备注编辑（首个进群的成员起名，全群共享）',
    }
    wx.showToast({ title: tips[e.currentTarget.dataset.k] || '', icon: 'none' })
  },
})
