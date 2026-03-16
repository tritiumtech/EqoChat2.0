# frontend 项目说明（EqoChat 前端）

> **目录**：`/frontend`  
> **技术栈**：UniApp 3 + Vue 3 + TypeScript + Vite  
> **运行形态**：多端（H5 / App / 小程序）

---

## 一、目录结构概览

- **根目录**
  - `package.json`：前端依赖与脚本命令。
  - `vite.config.ts`：Vite + UniApp 构建配置。
  - `env/.env*`：多环境配置（开发 / 预发 / 生产）。
  - `pnpm-lock.yaml`：依赖锁定文件。
- **运行配置**
  - `src/pages.json`：UniApp 路由与页面配置（TabBar / 页面路径）。
  - `src/manifest.json`：应用基础信息配置（应用名、图标、App 权限等）。
- **应用入口**
  - `src/main.ts`：Vue 应用入口，初始化 i18n、Pinia 等。
  - `src/App.vue`：全局根组件（包含 TabBar 标题国际化、全局样式）。
- **业务代码（src）**
  - `src/pages/`
    - `pages/index/index.vue`：首页 / 落地页。
    - `pages/auth/login.vue`、`pages/auth/register.vue`：登录 / 注册。
    - `pages/chat/chat-list.vue`：会话列表页。
    - `pages/chat/chat-room.vue`：单会话聊天页。
    - `pages/contact/contact-list.vue`：联系人列表。
    - `pages/discover/discover.vue`：发现页（能力图谱等占位）。
    - `pages/profile/profile.vue`：个人中心 / 语言设置。
  - `src/components/`
    - `AppShell.vue`：页面基础布局容器。
    - `PrimaryButton.vue`、`GhostButton.vue`：按钮组件。
    - `Card.vue`、`ModalSheet.vue`、`EmptyState.vue`：卡片、底部弹窗、空状态。
    - `components/chat/ConversationItem.vue`：会话列表项。
    - `components/chat/MessageBubble.vue`：消息气泡。
  - `src/store/`
    - `store/modules/user.ts`：用户登录态与用户信息。
    - `store/modules/chat.ts`：聊天相关 store（未读等）。
  - `src/api/modules/`
    - `auth.ts`：登录 / 注册 / 验证码接口。
    - `user.ts`：当前用户信息 `me` 等。
    - `contact.ts`：联系人列表与添加。
    - `conversation.ts`：会话列表、会话详情、消息发送与拉取。
  - `src/utils/`
    - `request.ts`：HTTP 请求封装（统一 BASE_URL、错误处理、鉴权）。
    - `websocket.ts`：底层 WebSocket 客户端与重连逻辑。
  - `src/composables/`
    - `useWebSocket.ts`：基于 `wsClient` 的组合式 WebSocket 封装（连接状态、消息回调等）。
  - `src/i18n/`
    - `index.ts`：国际化配置与中英文本地化。
  - `src/styles/`
    - `tokens.css`：全局 Design Tokens（颜色、圆角、阴影、字号、间距）。
  - `src/types/`
    - `uni-pages.d.ts`、`websocket.ts` 等：类型声明。
  - `uni.scss`：全局 SCSS 样式（uni-app 公共样式）。

---

## 二、初始化与启动

### 2.1 安装依赖

- 推荐使用 `pnpm`（根目录已有 `pnpm-lock.yaml`）：

```bash
cd frontend
pnpm install
```

### 2.2 环境配置

- 环境文件位于 `env/` 目录：
  - `.env`：通用默认配置。
  - `.env.development`：本地开发环境。
  - `.env.staging`：预发环境。
  - `.env.production`：生产环境。
- 关键配置项（示例）：
  - 后端 API 地址（如 `VITE_API_BASE_URL`）。
  - WebSocket 地址（如 `VITE_WS_BASE_URL`）。
- 建议：
  - **本地开发**：只修改 `env/.env.development`，避免误改线上配置。

### 2.3 本地开发命令

`package.json` 中定义了 uni-app 的多端脚本：

```json
"scripts": {
  "dev": "uni",              // 按交互选择端
  "dev:app": "uni --app",    // App 端
  "dev:h5": "uni --h5",      // H5 端
  "dev:mp-weixin": "uni --mp-weixin", // 微信小程序
  "build": "uni build",
  "build:staging": "uni build --mode staging",
  "build:app": "uni build --app",
  "build:h5": "uni build --h5",
  "build:mp-weixin": "uni build --mp-weixin"
}
```

- 常用开发方式：

```bash
# H5 调试（浏览器）
pnpm dev:h5

# App 端调试（需要对应开发环境）
pnpm dev:app

# 微信小程序调试（配合开发者工具）
pnpm dev:mp-weixin
```

---

## 三、开发注意事项

### 3.1 代码风格与约定

- **语言与框架**
  - 统一使用 **Vue 3 组合式 API (`<script setup lang="ts">`)**。
  - 全面开启 **TypeScript**，尽量为公共方法与数据结构补全类型。
- **状态管理**
  - 跨页面共享状态（用户、聊天）使用 **Pinia** store；
  - 页面内临时状态使用 `ref` / `reactive` 即可，避免滥用全局 store。
- **模块拆分**
  - 业务 API → 放在 `src/api/modules/*`，禁止在组件内直接写 `request.get('/api/...')` 的裸调用。
  - WebSocket → 统一通过 `useWebSocket` 和 `wsClient` 封装，聊天页只订阅需要的回调。
- **Java 新 package 约定（跨端注意）**
  - 虽然是前端文档，但为了与后端协作统一：后端新建 Java `package` 时必须带 `package-info.java` 说明职责（前端调用新接口前可参考）。

### 3.2 设计与 UI 规范

- **Design Tokens**
  - 所有颜色、圆角、阴影、字号、间距统一来自 `src/styles/tokens.css`：
    - 颜色：`--c-bg`、`--c-surface`、`--c-primary`、`--c-muted` 等。
    - 圆角：`--radius-xl`、`--radius-lg`、`--radius-md`、`--radius-pill`。
    - 字号：`--font-size-title`、`--font-size-body`、`--font-size-caption`。
    - 间距：`--space-1` ~ `--space-5`。
  - 新组件 / 新页面 **不要硬编码颜色和圆角**，优先使用变量。
- **布局与组件**
  - 通用布局用 `AppShell`，会话列表项用 `ConversationItem`，消息气泡用 `MessageBubble`。
  - 空状态统一使用 `EmptyState`，避免每个页面写不同的“暂无XXX”DOM 结构。
  - 按钮统一用 `PrimaryButton` / `GhostButton`，保证交互反馈一致。

### 3.3 接口与错误处理

- 所有 HTTP 请求通过 `src/utils/request.ts`：
  - 自动携带 Token（从 `user` store 读取）。
  - 统一处理错误，组件只关心业务异常文案。
- 业务组件：
  - 捕获接口异常时，用 `uni.showToast` 提示用户，文案尽量复用 i18n 的 `toast.*` key。
  - 不要在多个页面复制粘贴相同的错误处理逻辑，可抽成小函数或封装到 API 层。

### 3.4 多端适配

- 优先遵守 uni-app 的组件用法（`view`、`text`、`scroll-view` 等），不要引入只适用于 H5 的 DOM。
- 手势类交互（长按、滑动）使用 uni-app 官方推荐方式，注意：
  - 小程序端和 H5 的事件行为略有差异，需在关键交互点实测。

### 3.5 调试与联调建议

- **本地后端联调**
  - 确保 `env/.env.development` 中的 API 地址指向本机后端（如 `http://localhost:8080`）。
  - WebSocket 地址保持和后端配置一致（路径和协议）。
- **登录与会话**
  - 聊天页强依赖登录态：`chat-list` / `chat-room` 进入前会检查 `userStore.isLoggedIn`，未登录会自动跳转到登录页。
  - 在调试会话列表和聊天室时，优先使用已有用户数据（例如数据库中准备好的 demo 用户），避免空数据导致 UI 逻辑分支过多。

---

## 四、小结

- `frontend/` 是一个基于 **UniApp + Vue3 + TypeScript** 的多端前端工程，核心业务集中在：
  - 会话列表 `pages/chat/chat-list.vue`、
  - 聊天室 `pages/chat/chat-room.vue`、
  - 联系人 / 发现 / 个人中心等周边页面。
- 设计上通过 `tokens.css` + 一组基础组件，形成了一套偏聊天应用风格的统一 UI。
- 开发时重点关注：
  - 统一使用 API 封装和 WebSocket 封装；
  - 复用 Design Tokens 和公共组件；
  - 保持多端兼容性与代码简洁可扩展。

