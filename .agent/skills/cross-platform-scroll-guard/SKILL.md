---
name: cross-platform-scroll-guard
description: 保护 uni-app 页面滚动改动的跨端稳定性。用于修改 chat/world/contacts 等主列表页的高度、scroll-view、safe-area、tabbar 相关样式时；当用户提到 iOS 可见但 H5 不可滚动、页面被锁死、底部大面积空白时必须启用。
---

# Cross Platform Scroll Guard

## 目标

在不破坏现有端行为的前提下修复单端问题，避免出现：

- 修 iOS 后 H5 整页无法滚动
- 修 H5 后 iOS 容器塌陷
- 误把数据问题当布局问题（或反过来）

## 强制流程（必须按顺序）

1. **先定位端与症状**
   - 明确是 `H5`、`iOS`、`Android` 哪个端异常。
   - 明确是“不可滚动”“空白占位”“元素不可见”哪一类。

2. **先读现状再改**
   - 读取页面模板与样式，重点看：
     - 根容器 `.page`
     - 主滚动区（如 `.scroll-feed`、`.list-scroll`、`.main-scroll`）
     - `BottomNav`/`safe-area` 补偿
   - 用 `git diff` 确认最近改动是否动过：
     - `scroll-view` 结构
     - `overflow: hidden`
     - `height/min-height`
     - 动态组件替换（`:is="view/scroll-view"`）

3. **最小改动原则**
   - 只改触发问题的页面。
   - 只动必要样式/结构，不顺手改其他视觉参数。
   - 不把单端修复直接推广到全端。

4. **跨端保护原则**
   - 禁止直接把 `overflow: hidden` 加到所有端的页面根容器。
   - 禁止在没有验证前，把 `scroll-view` 改成动态 `component :is`。
   - 高度改动优先使用“单端隔离”策略，不做全局硬切。

5. **数据与布局分离排查**
   - 如果出现卡片大空白，先检查媒体字段合法性（`imageUrl/videoUrl/mediaType`）。
   - 必要时在卡片组件里做媒体失败降级（`@error` 后折叠占位）。

6. **验证闭环（必做）**
   - H5：强刷后验证鼠标滚轮/触控板滚动。
   - iOS：验证页面高度、底部安全区、是否被 tabbar 遮挡。
   - 至少验证：`chat-list`、`contact-list`、`world` 三页主滚动行为。

## 推荐修复模板

### 模板A：仅 iOS 高度异常

- 只在 iOS 分支样式里处理高度/安全区；
- 不改 H5 的 `scroll-view` 结构。

### 模板B：仅 H5 不可滚动

- 先回退最近结构性改动（动态组件替换、根容器 overflow）；
- 再做最小 H5 样式补丁；
- 验证 iOS 未回归。

### 模板C：World 卡片中部大空白

- 校验媒体 URL 合法性；
- 媒体加载失败时折叠媒体区域；
- 不先改页面高度。

## 输出要求

在回复用户时必须包含：

1. 根因（1-2 条，必须可验证）
2. 改动文件列表
3. 验证步骤（H5 + iOS）
4. 未解决时的下一步取证方案（日志或最小复现）

