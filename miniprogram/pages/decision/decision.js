const { request, ensureLogin } = require('../../utils/request')

/** 定档确认页:最优时段 + 到场名单 → 就定这个时间(POST lock) → 返回 vote 页出已定档视图 */
Page({
  data: { loaded: false, bestLabel: '', bestCount: 0, secondCount: 0, people: [], peopleCount: 0 },

  onLoad(query) {
    this.voteId = query.voteId
    this.openGId = query.openGId || ''
    this.locking = false
    ensureLogin()
      .then(() => request('/api/votes/' + this.voteId))
      .then((vote) => {
        const best = vote.slots.filter((s) => s.isBest)[0] || vote.slots[0]
        if (!best) {
          wx.showToast({ title: '这个投票还没有候选时间', icon: 'none' })
          wx.navigateBack()
          return
        }
        const others = vote.slots.filter((s) => s.slotId !== best.slotId)
        const secondCount = others.reduce((max, s) => Math.max(max, s.count || 0), 0)
        this.bestSlotId = best.slotId
        this.setData({
          loaded: true,
          bestLabel: best.label,
          bestCount: best.count,
          secondCount,
          people: (best.voters || []).slice(0, 6),
          peopleCount: (best.voters || []).length,
        })
      })
      .catch(() => wx.showToast({ title: '网络不太好，请稍后再试', icon: 'none' }))
  },

  lock() {
    if (this.locking) return
    wx.showModal({
      title: '就定 ' + this.data.bestLabel + '？',
      content: this.data.bestCount + ' 人有空。定档后投票停止，大家都能看到结果。',
      confirmText: '就定这个时间',
      cancelText: '再等等',
      success: (res) => {
        if (!res.confirm) return
        this.locking = true
        request('/api/votes/' + this.voteId + '/lock', {
          method: 'POST',
          data: { slotId: this.bestSlotId },
        })
          .then(() => {
            wx.showToast({ title: '定了！', icon: 'success' })
            // 返回 vote 页,onShow 刷新出已定档视图(名单/双 CTA)
            setTimeout(() => wx.navigateBack(), 600)
          })
          .catch(() => { this.locking = false })
      },
    })
  },
})
