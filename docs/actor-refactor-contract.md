# Actor Model 重构合同：Sprint 0 基线与门禁

本文档是 Actor Model 重构期间的防回退合同。后续 Chat、Project、World、Contact、Credit、Notification 等 Sprint 都必须以 `backend/ACTOR-MODEL-ARCHITECTURE.md` 为设计源，以本文为验收与静态守卫依据。

## 1. Protected Baseline

Sprint 0 冻结以下基线：

1. 领域主体统一使用 `SubjectRef(id, type)` 表达，`type` 的规范值只允许是 `HUMAN`、`AGENT`、`SYSTEM`。
2. 旧口径 `USER/AGENT` 只允许出现在历史说明、旧迁移清理和一次性数据回填中。运行时代码、DTO、表字段说明、枚举、WebSocket 事件和正常写路径不得接受或发出 `USER`。
3. 跨主体写路径必须持久化主体 id 与主体类型。任何“谁发起、谁接收、谁阅读、谁付款、谁收款、谁被扣分、谁被通知”的写入都必须能还原为 `SubjectRef(HUMAN/AGENT/SYSTEM)`。
4. `user_profile` 与 `agent_profile` 仍是当前存量事实，但跨模块协作不得继续把 `userId` 当作唯一主体身份。需要人类登录上下文时可使用当前用户 id；写入业务主体时必须显式转换为 `SubjectRef.human(userId)`。
5. World 中为 Agent 建立 mirror user profile 只能作为迁移期兼容，不得作为长期方案。后续 World 必须以 `author_id + author_type` 或主体目录解析作者身份。
6. 支付、钱包、收款、项目资金路由必须持久化 wallet/liability 审计。任何“模拟路由”“只在响应中拼文案”“只靠 ownerId 推断”的实现都不能作为最终验收。
7. Chat 未读/已读/WebSocket 口径必须按 `subject id + subject type` 处理。不得只按 user id、session user、`readerType=USER`、`senderType=USER` 推断。
8. `scripts/smoke-actor-baseline.sh` 与 `scripts/actor-static-guard.sh` 是后续 Sprint 的共同门禁脚本。Sprint 0 可以用 `ALLOW_EXISTING=1` 记录存量遗留；后续 Sprint 默认不得引入 P0。

## 2. Actor Model Hard Rules

### 2.1 主体类型

- 新增模型、枚举、接口、DTO、事件、数据库字段说明中的主体类型必须使用 `HUMAN`、`AGENT`、`SYSTEM`。
- 不得新增 `SubjectType.USER`、`RecipientType.USER`、`ReaderType.USER`、`ParticipantType.USER`、`senderType("USER")` 等旧口径。
- 需要处理旧数据 `USER` 时，必须通过迁移或显式历史清理转换为 `HUMAN`；不得在正常运行时代码中保留 `USER -> HUMAN` 兼容分支。
- UI 可保留展示层的 `isAgent`、`ai` 等派生字段，但后端权威身份必须来自 `SubjectRef` 或等价的 id+type 字段。

### 2.2 跨主体写路径

以下写路径必须显式保存主体 id 与主体类型：

- Chat：conversation participant、message sender、read receipt、unread counter、WebSocket sender/recipient。
- Project：owner、member、task assignee、payment payer/recipient、file uploader、ownership transfer target。
- World：post author、reply author、mention target、topic follow subject、notification sender/recipient。
- Contact：requester、recipient、friend/contact target、block/follow target。
- Credit：credit subject、violation subject、reporter、reviewer、关联 owner liability。
- Notification：recipient、sender、业务跳转对象中的主体引用。
- Payment：payer、recipient、settlement wallet owner、liability human、agent direct wallet 状态。

禁止用单独的 `Long userId`、`Boolean isAgent`、`String ownerType` 作为跨主体写入的唯一依据。允许 Controller 从登录态得到当前人类用户 id，但写业务记录前必须转换为 `SubjectRef(HUMAN)`。

### 2.3 Chat 特别规则

- 未读计数必须以 `conversation_id + reader_subject_id + reader_subject_type` 计算。
- 已读回执必须以 `message_id + reader_subject_id + reader_subject_type` 持久化。
- WebSocket 事件必须携带 sender 与 recipient 的 subject id/type，不得默认 `senderType=USER`。
- Agent 消息必须记录可审计的责任链，至少能回溯到 owner human。
- 系统消息使用 `SubjectRef(SYSTEM)`，不得伪装成某个人类用户。

### 2.4 支付与责任

- Human 钱包默认启用；Agent 钱包默认禁用。
- Agent 达到 PRD 规定的 500 行为积分并由 owner 启用后，未来收入才可直达 Agent 钱包。
- Agent 钱包禁用时，收入路由到 owner human 钱包；这一路由必须持久化审计，不能只在响应文案中体现。
- Agent 钱包启用后，直接收款与 owner liability 必须同时可审计：收款归 Agent，责任链仍能回到 owner human。
- 项目支付、任务结算、退款、争议、信用扣减必须引用相同的主体与责任链事实。

### 2.5 Mirror User 限制

- mirror user 仅用于兼容当前依赖 `user_profile` 的存量查询。
- 新表、新迁移、新接口不得把 mirror user 设计成长期身份源。
- 迁移 World、Contact、Project 时应优先通过 `SubjectDirectory` 或等价查询解析主体资料。
- 如果临时读取 mirror user，必须同时读取或推导真实 `SubjectRef`，并在 Sprint 验收中记录后续移除点。

## 3. Sprint Gates

每个 Sprint 合入前必须满足：

1. 运行静态守卫：`scripts/actor-static-guard.sh`。
   - Sprint 0 允许使用 `ALLOW_EXISTING=1 scripts/actor-static-guard.sh` 记录存量遗留。
   - Sprint 1 起，默认模式发现 P0 必须失败。
2. 运行基线 smoke：`scripts/smoke/actor-baseline-smoke.sh`，兼容入口为 `scripts/smoke-actor-baseline.sh`。
   - 默认后端为 `http://localhost:8080`。
   - 可通过 `BASE_URL`、`PHONE`、`PASSWORD` 覆盖。
3. 变更涉及模块必须补充或更新对应模块测试，至少覆盖新旧主体类型各一条关键路径。
4. 涉及数据库写路径必须有迁移脚本或回填策略；不得用长期运行时兼容读取替代清理。
5. 涉及支付、信用、责任链的变更必须证明审计记录可追溯，而不是只依赖实时 join 或展示字段。
6. 涉及前端展示的变更必须证明后端响应中的主体身份不退回 `USER/AGENT` 旧口径。
7. Sprint 总结必须列出已清理的旧口径、仍属后续 Sprint 范围的历史债务、下个 Sprint 必须继续拆除的点。

## 4. Per-Sprint Acceptance Checklist

### 通用清单

- [ ] 新增主体类型只使用 `HUMAN`、`AGENT`、`SYSTEM`。
- [ ] 所有跨主体写路径都有 subject id/type。
- [ ] 没有新增 `USER/AGENT` 旧口径硬编码。
- [ ] 旧数据通过迁移、回填或历史清理处理，正常运行时代码没有 `USER` 兼容分支。
- [ ] 运行 `scripts/actor-static-guard.sh` 通过，或 Sprint 0 用 `ALLOW_EXISTING=1` 输出基线报告。
- [ ] 运行 `scripts/smoke-actor-baseline.sh` 通过或记录清楚的环境原因。
- [ ] 支付、信用、责任链相关变更有可持久化审计。

### Chat Sprint

- [ ] conversation participant 从 `USER` 迁移到 `HUMAN/AGENT/SYSTEM`，正常读写不再输出旧口径。
- [ ] message sender 使用 subject id/type，不再默认 `senderType("USER")`。
- [ ] read receipt 使用 reader subject id/type，不再写死 `readerType(USER)`。
- [ ] unread counter 按 reader subject id/type 聚合。
- [ ] WebSocket 入站、出站、广播事件均携带 sender/recipient subject id/type。
- [ ] Agent 消息能持久化 liability human。

### Project Sprint

- [ ] owner、member、task assignee、file uploader、payment recipient 均使用 subject id/type。
- [ ] Agent owner 项目必须记录 owner human liability。
- [ ] 所有权转移目标必须是合法 `SubjectRef`。
- [ ] Project payment 必须持久化 wallet route、liability human、direct recipient 或 settlement human。
- [ ] 不再把 Agent 项目只作为 human 项目的展示变体。

### World Sprint

- [ ] post/reply author 使用 `author_id + author_type`。
- [ ] 不再依赖 mirror user 判断作者是否 Agent。
- [ ] mention、topic follow、share、notification 均使用 subject id/type。
- [ ] Agent 发帖与 Human 发帖走同一主体目录渲染路径。
- [ ] World 旧数据有明确回填或清理方案。

### Contact Sprint

- [ ] friend/contact/requester/recipient 均使用 subject id/type。
- [ ] Human 与 Agent 联系人关系不再借 `user_friend.friend_type=USER` 表达。
- [ ] 搜索、详情、标签、好友申请都能返回主体类型。
- [ ] Agent owner 关系只作为透明度信息，不替代主体身份。

### Credit Sprint

- [ ] credit subject 与 violation subject 使用 `HUMAN/AGENT/SYSTEM`。
- [ ] 旧 `USER` 信用记录有迁移或回填方案。
- [ ] Agent 违规必须能联动 owner human liability。
- [ ] 信用分范围与 PRD 的 `300-850` 口径一致，历史 `0-100` 只允许适配展示。
- [ ] 记录行为积分与信用分的用途边界，不混用钱包解锁和信用评价。

### Notification Sprint

- [ ] recipient 与 sender 使用 subject id/type。
- [ ] 系统通知使用 `SYSTEM`，不伪装成 human。
- [ ] 业务 payload 中引用主体时必须携带 id/type。
- [ ] 未读通知按 recipient subject id/type 聚合。
- [ ] Chat、World、Project、Credit 触发的通知都不新增 `RecipientType.USER`。

## 5. P0 与 Warn 分类

P0 必须阻断：

- 新增或继续扩散 `senderType("USER")`、`senderType: "USER"`、`SenderType.USER`。
- 新增或继续扩散 `RecipientType.USER`、`ReaderType.USER`、`ParticipantType.USER`、`SubjectType.USER`。
- 新增或继续扩散 `readerType(USER)` 或等价写死。
- 新增跨主体写路径但没有 subject type。
- 支付路径没有 wallet/liability 审计。

Warn 需要记录并逐步清理：

- schema 注释或旧迁移中仍写 `USER/AGENT`。
- 存量 SQL seed、历史 DTO、前端展示字段中存在旧口径。
- `is_agent`、`author_ai`、mirror user 等过渡字段。
- 当前仍以 `userId` 命名但实际承载主体身份的参数。

Warn 不一定阻断 Sprint 0，但后续 Sprint 修改到相关模块时必须顺手收敛，不能把 warn 当成长期豁免。
