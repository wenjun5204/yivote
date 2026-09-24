# AGENTS — 咦投票 (yivote)

## 小程序组件选型原则

**尽量使用微信官方组件**，不自绘、不引第三方 UI 库：

- 单选/选择类交互 → `picker`（官方 5 种 mode：`selector` / `multiSelector` / `time` / `date` / `region`）
- 精确日期候选（如"10 月 3 日周六下午"）→ `picker mode="date"`，不自绘日历
- 多选类交互 → 官方 `picker` 不支持多选，优先考虑官方 `checkbox-group` / `checkbox`；确需 chips 网格观感时可用自绘 `button`（现状即如此），但样式须与官方组件观感一致
- 弹层/确认 → `wx.showModal`（支持 `editable` 输入框）；提示 → `wx.showToast`；强提醒 → `wx.showToast` + 订阅消息，不自绘 toast
- 分享 → `onShareAppMessage` / `button open-type="share"`，不自绘转发按钮逻辑
- 新增页面前先查官方组件文档（https://developers.weixin.qq.com/miniprogram/dev/component/ ），确认无现成组件再自绘，并在 PR/commit 里说明原因

## 其他约定

- API 契约以 `api/openapi.yaml` 为 SSOT，前后端出入参严格对齐
- 后端改动需跑 `./mvnw test`（backend/ 下）全绿再提交
- 文案遵守 PRD §1.2 召唤感红线（禁止质问感话术）
