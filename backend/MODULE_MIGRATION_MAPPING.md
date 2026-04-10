# EqoChat 后端迁移映射清单（用于 Maven 多模块拆分）

> 说明：此文件用于实现过程的“归属确认”，不改变接口路径。后续迁移阶段会逐步把源代码迁移到对应模块，并按 `MODULE-ARCHITECTURE.md` 的包命名规范完成重命名。

## 1. Maven 模块归属（按业务领域）

| 当前代码归属（类/包特征） | 目标模块（实现模块） | 目标模块（API 模块） |
|---|---|---|
| `com.eqochat.controller.*` 中与认证/用户资料相关：`AuthController`、`UserController` | `eqochat-business/eqochat-user/eqochat-user` | `eqochat-business/eqochat-user/eqochat-user-api` |
| `com.eqochat.world.*`（`WorldService`/`WorldUploadService` 与 `WorldController`） | `eqochat-world` | `eqochat-world-api` |
| `com.eqochat.controller/Conversation*` 与 `com.eqochat.websocket/*`（聊天实时）相关 | `eqochat-chat` | `eqochat-chat-api` |
| `com.eqochat.controller/*Contact*`、`FriendRequestController` 相关 | `eqochat-contact` | `eqochat-contact-api` |
| `com.eqochat.controller/ProjectController` 相关 | `eqochat-project` | `eqochat-project-api` |
| `com.eqochat.controller/CreditController` 与信用相关 mapper/entity | `eqochat-credit` | `eqochat-credit-api` |
| `com.eqochat.controller/NotificationController` | `eqochat-notification` | `eqochat-notification-api` |
| `com.eqochat.controller/AgentController` 与 Agent 相关 mapper/entity | `eqochat-agent` | `eqochat-agent-api` |

## 2. 技术框架层归属（与业务无关/跨模块复用）

| 当前代码特征 | 目标框架模块 |
|---|---|
| `com.eqochat.common/*`（`ApiResponse`、异常体系、i18n 工具等） | `eqochat-framework-common` |
| `com.eqochat.config/*`（安全/鉴权/事务/Redis/文件/S3/Neo4j/MyBatis 配置等） | 视职责拆分到：`eqochat-framework-security` / `eqochat-framework-datasource` / `eqochat-framework-file` / `eqochat-framework-websocket` |
| `com.eqochat.security/*`（JWT、WebSocket 鉴权拦截器） | `eqochat-framework-security` |
| `com.eqochat.websocket/*`（WebSocket 消息协议与 handler） | `eqochat-framework-websocket`（或按计划将协议/类型留在框架层，handler 交由 `eqochat-chat` 实现） |
| `com.eqochat.file/*`（MinIO/S3 客户端、预签名 DTO）与 `FileUploadService` | `eqochat-framework-file` |

## 3. DTO / Request / Response 归属（按文件名/路由特征）

| DTO 命名特征 | 目标 API 模块 |
|---|---|
| `Login*/Register*/VerifyCode*/Email*` 等认证类请求/响应 | `eqochat-user-api` |
| `CreateWorldPost*/World*Response` 等世界动态/话题请求/响应 | `eqochat-world-api` |
| `SendFriendRequest* / AddContactRequest / UpdateContactTagsRequest` 等联系人请求/响应 | `eqochat-contact-api` |
| `CreateProject*/Project*Response` 等项目请求/响应 | `eqochat-project-api` |
| `SendMessageRequest / MarkConversationReadRequest / Message*Response / Conversation*Response` 等聊天请求/响应 | `eqochat-chat-api` |
| `CreditProfileResponse` 等信用请求/响应 | `eqochat-credit-api` |
| `AgentMeResponse` 等 Agent 请求/响应 | `eqochat-agent-api` |
| `MarkNotificationReadRequest / NotificationResponse` 等通知请求/响应 | `eqochat-notification-api` |

## 4. 关键依赖耦合点（后续迁移时必须处理）

| 位置（当前代码现象） | 风险 | 后续要做什么 |
|---|---|---|
| 聊天会话管理 `com.eqochat.service.UserSessionService` 注入了聊天实现类 `ChatWebSocketHandler` / `WebSocketSender` | 违反“只能依赖 `-api` 模块”的模块规则 | 新增 `eqochat-chat-api` 实时通知接口，让安全/会话层只依赖该接口 |
| `AgentController` 直接注入 `CreditRecordMapper` | Agent 实现层会间接依赖 Credit 实现/mapper（包名迁移后更明显） | 改为依赖 `eqochat-credit-api` 暴露的查询接口，或把 Credit 查询聚合到 Agent 允许的 API |
| `MyBatis @MapperScan("com.eqochat.mapper")` 当前指向旧根包 | 迁移后会导致 Mapper 找不到 | 将 `@MapperScan` 改为覆盖新 mapper 包根 |

