const { request, ensureLogin } = require('../../utils/request')

Page({
  data: {
    title: '',
    kindOptions: ['文本', '点选'],
    fields: [
      { label: '手机号', kind: 'TEXT', kindIndex: 0, showOptions: false, optionsText: '' },
    ],
  },

  onTitle(e) {
    this.setData({ title: e.detail.value })
  },

  onFieldLabel(e) {
    const i = e.currentTarget.dataset.index
    this.setData({ ['fields[' + i + '].label']: e.detail.value })
  },

  onKind(e) {
    const i = e.currentTarget.dataset.index
    const kindIndex = Number(e.detail.value)
    this.setData({
      ['fields[' + i + '].kindIndex']: kindIndex,
      ['fields[' + i + '].kind']: kindIndex === 0 ? 'TEXT' : 'CHIP',
      ['fields[' + i + '].showOptions']: kindIndex === 1,
    })
  },

  onOptions(e) {
    const i = e.currentTarget.dataset.index
    this.setData({ ['fields[' + i + '].optionsText']: e.detail.value })
  },

  addField() {
    if (this.data.fields.length >= 6) {
      wx.showToast({ title: '最多 6 个字段', icon: 'none' })
      return
    }
    this.setData({ fields: [...this.data.fields, { label: '', kind: 'TEXT', kindIndex: 0, showOptions: false, optionsText: '' }] })
  },

  removeField(e) {
    const i = e.currentTarget.dataset.index
    const fields = this.data.fields.filter((_, idx) => idx !== i)
    this.setData({ fields })
  },

  createCollect() {
    const title = this.data.title.trim()
    if (!title) {
      wx.showToast({ title: '先写个标题', icon: 'none' })
      return
    }
    const fields = []
    for (const f of this.data.fields) {
      const label = (f.label || '').trim()
      if (!label) {
        wx.showToast({ title: '有字段名没填', icon: 'none' })
        return
      }
      const field = { label, kind: f.kind }
      if (f.kind === 'CHIP') {
        const options = (f.optionsText || '').split(/[,，]/).map((o) => o.trim()).filter(Boolean)
        if (!options.length) {
          wx.showToast({ title: `「${label}」要加候选项`, icon: 'none' })
          return
        }
        field.options = options
      }
      fields.push(field)
    }
    if (!fields.length) {
      wx.showToast({ title: '至少加一个字段', icon: 'none' })
      return
    }
    ensureLogin()
      .then(() => request('/api/collects', { method: 'POST', data: { title, fields } }))
      .then((collect) => {
        wx.redirectTo({ url: '/pages/collect/collect?collectId=' + collect.collectId })
      })
      .catch(() => {})
  },
})
