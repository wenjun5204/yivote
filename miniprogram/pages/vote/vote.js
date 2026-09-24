const { request, ensureLogin } = require('../../utils/request')

Page({
  data: { vote: null, picked: [], submitted: false, isOwner: false },

  onLoad(query) {
    this.voteId = query.voteId
    ensureLogin()
      .then(() => this.load())
      .catch(() => wx.showToast({ title: '网络不太好，请稍后再试', icon: 'none' }))
  },

  load() {
    return request('/api/votes/' + this.voteId).then((vote) => {
      // picked 预亮：mySlotIds 来自契约（改投场景）
      const picked = vote.mySlotIds || []
      this.setData({
        vote,
        picked,
        submitted: picked.length > 0,
        isOwner: vote.owner.userId === (wx.getStorageSync('yivote_user') || {}).userId,
        voterCount: (vote.slots[0] || { voters: [] }).voters ? this.countVoters(vote) : 0,
      })
      this.fillDerived()
    })
  },

  countVoters(vote) {
    const set = {}
    vote.slots.forEach((s) => s.voters.forEach((v) => (set[v.userId] = 1)))
    return Object.keys(set).length
  },

  fillDerived() {
    const vote = this.data.vote
    if (!vote) return
    const best = vote.slots.filter((s) => s.isBest)
    const maxCount = best.length ? best[0].count : 0
    const totalCount = Math.max(this.countVoters(vote), 1)
    const progress = Math.min(100, Math.round((maxCount / Math.max(totalCount, maxCount)) * 100))
    const pickedSet = {}
    this.data.picked.forEach((id) => (pickedSet[id] = 1))
    const slots = vote.slots.map((s) => ({ ...s, picked: !!pickedSet[s.slotId] }))
    let lockedVoters = []
    let lockedLabel = ''
    let lockedCount = 0
    if (vote.status === 'LOCKED' && vote.lockedSlotId) {
      const ls = vote.slots.find((s) => s.slotId === vote.lockedSlotId)
      lockedVoters = ls ? ls.voters : []
      lockedLabel = ls ? ls.label : ''
      lockedCount = lockedVoters.length
    }
    this.setData({
      vote: { ...vote, slots },
      bestLabel: best.length ? best[0].label + ' · ' : '',
      progress,
      lockedVoters,
      lockedLabel,
      lockedCount,
    })
  },

  toggle(e) {
    if (this.data.vote.status !== 'OPEN') return
    const id = e.currentTarget.dataset.id
    const picked = this.data.picked.includes(id)
      ? this.data.picked.filter((x) => x !== id)
      : [...this.data.picked, id]
    this.setData({ picked })
    this.fillDerived()
  },

  submit() {
    request('/api/votes/' + this.voteId + '/ballots', {
      method: 'PUT',
      data: { slotIds: this.data.picked },
    })
      .then(() => {
        this.setData({ submitted: true })
        wx.showToast({ title: '记上了！随时能改', icon: 'success' })
        this.load()
      })
      .catch(() => {})
  },

  lockBest() {
    const best = this.data.vote.slots.filter((s) => s.isBest)[0]
    if (!best) return
    request('/api/votes/' + this.voteId + '/lock', {
      method: 'POST',
      data: { slotId: best.slotId },
    })
      .then(() => {
        wx.showToast({ title: '定了！', icon: 'success' })
        this.load()
      })
      .catch(() => {})
  },

  shareResult() { /* 转发结果卡，见 onShareAppMessage */ },

  onShareAppMessage() {
    const v = this.data.vote || {}
    const locked = this.data.lockedLabel
    return {
      title: locked
        ? `定了！${locked} · ${this.data.lockedCount} 人有空`
        : `就差你了！${v.title} · 已投 ${this.data.voterCount || ''}人`,
      path: '/pages/vote/vote?voteId=' + this.voteId,
    }
  },
})
