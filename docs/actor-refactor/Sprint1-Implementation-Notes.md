# Sprint 1 Implementation Notes

本次只交付 Actor 基础层最小硬化，不改 Chat/Project/World/Contact/Credit/Notification 业务模块，不改 DB 配置或 seed 数据。

## 变更摘要

1. `SubjectRef` 不再把 null type 静默转换为 `HUMAN`；canonical `SubjectType.from` 只接受 `HUMAN/AGENT/SYSTEM`。
2. 旧 `USER -> HUMAN` 仅通过显式 `SubjectType.fromLegacy` / `SubjectRef.fromLegacy` 适配。
3. `LiabilityPolicyServiceImpl` 对 Agent 严格校验：Agent active、owner human active、`OWNER` binding active、`liabilityAccepted=true`；失败返回 unresolved chain，并带 reason。
4. `WalletPolicyServiceImpl` 对 null/system 返回 unavailable/no wallet；Agent wallet 复用同等 owner/binding/liability 校验，`agent_wallet_state` 优先，legacy `source_config` 钱包 fallback 默认关闭。
5. `CapabilityQueryServiceImpl` 对 missing、inactive、banned、suspended、system subject 返回 disabled，不给普通 enabled 能力。
6. `ActorDataAccess` 的表存在缓存只缓存 true，不永久缓存 table-missing false；demo/local fallback 统一由 `eqochat.actor.demo-fallback.enabled` 控制，默认 false。
7. `MilestonePolicyServiceImpl` 先校验主体，missing/inactive/system 返回 `Unknown` 基线福利，不授予 wallet、owned-agent、免押、仲裁或治理权益。

## 测试

新增最小 JUnit/Mockito 单测：

- `SubjectRef/SubjectType` canonical 与 legacy adapter。
- Liability success 与 Agent/owner/binding/liability failure reason。
- Wallet null/system、wallet_state 优先、Agent direct。
- Capability missing/inactive/system disabled 与 valid subject enabled。
- Credit legacy score adapter/clamp。
- Table-missing false 不永久缓存。
- Demo points fallback 默认关闭，显式开启后才读 `system_config demo.*.points.*`。
- Legacy `source_config` 钱包 fallback 默认关闭，显式开启后才允许 Agent direct。
- Milestone 对 missing/inactive/system subject 返回 Unknown/no wallet/no owned agents。

验证命令：

```bash
mvn -pl eqochat-business/eqochat-actor-parent/eqochat-actor -am clean test
```

结果：`BUILD SUCCESS`，`ActorCoreHardeningTest` 13 tests passed，0 failures，0 errors。

## Sprint 1 验证记录

时间：2026-06-29。

1. `mvn -pl eqochat-business/eqochat-actor-parent/eqochat-actor -am test`
   - 结果：`BUILD SUCCESS`，8 tests passed。
   - 备注：早先一次 surefire dump 中的 `AgentProfile$AgentStatus` discovery failure 来自旧/不完整编译产物；clean reactor 后不可复现。
2. `mvn -pl eqochat-business/eqochat-actor-parent/eqochat-actor -am clean test`
   - 结果：`BUILD SUCCESS`，8 tests passed。
3. `mvn -pl eqochat-server -am -DskipTests package`
   - 结果：`BUILD SUCCESS`。
   - 备注：第一次完整 package 曾因本地 reactor/依赖解析瞬态状态尝试从远程解析 `eqochat-framework-common:1.0.0` 而失败；先构建最小 redis reactor 后重跑完整 package 通过。
4. `bash -n scripts/smoke/actor-baseline-smoke.sh scripts/smoke-actor-baseline.sh scripts/actor-static-guard.sh`
   - 结果：通过。
5. `ALLOW_EXISTING=1 scripts/actor-static-guard.sh`
   - 结果：通过 baseline 模式，P0=17，WARN=27；这些 legacy 命中均在 Sprint 1 范围外。
6. `scripts/smoke/actor-baseline-smoke.sh`
   - 结果：通过。
   - 覆盖：phone login、auth me、agents me、contacts、projects list/detail/sidebar、world feed/create/reply、conversation create/list/messages/send/read。

运行时验证使用最新 `eqochat-server` jar，并通过 `--spring.profiles.active=local` 启动；未修改任何环境特定配置。
