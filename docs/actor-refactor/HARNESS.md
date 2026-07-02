# Actor Refactor Harness

Last updated: 2026-07-02.

This file records operational knowledge learned while running the Actor Model refactor so future agent runs do not repeat the same mistakes. It complements `Actor-Refactor-Contract.md`, `Sprint0-Baseline.md`, and `Sprint1-Implementation-Notes.md`.

## Mission

- Refactor EqoChat toward the Actor Model in `backend/ACTOR-MODEL-ARCHITECTURE.md`.
- Preserve existing product behavior while making identity explicit across humans, agents, and system actors.
- Use sprint-sized iterations. Do not attempt a big-bang rewrite.
- Do not declare a sprint complete until Evaluator returns an explicit `PASS`.

## Agent Loop

- Planner owns product and sprint planning.
- Generator owns implementation for one sprint or sub-sprint at a time.
- Evaluator owns contract enforcement and must return either `PASS` or `BLOCK`.
- If Evaluator returns `BLOCK`, Generator fixes only the listed contract violations, then the full relevant gate set runs again.
- The main agent may rerun tests, inspect artifacts, and stabilize services, but should not soften Evaluator findings.
- Current thread sub-agents, if still available:
  - Planner: `019f1dee-df89-7a31-9b0c-03948fbe60d5` (`Goodall`)
  - Evaluator: `019f1def-184b-7bc3-ba90-35337ccc3144` (`Banach`)

## Timeout And Transport Protocol

Transport or decoding interruptions do not prove the sub-agent failed. They mean the reporting channel is unreliable and the sub-agent state is unknown.

Default response:

1. Do not kill the sub-agent immediately.
2. Wait or query again when the official result is needed.
3. Continue only with non-conflicting work, such as reading docs, running tests, or checking service health.
4. Treat any artifacts on disk as untrusted until local gates verify them.
5. If a sub-agent may still be writing to the same files, avoid overlapping edits.
6. Close or kill a sub-agent only when it is clearly stuck, obsolete, or likely to conflict with stabilization.

Timeout of a `wait_agent` call is an orchestration timeout, not a sprint decision. Sprint completion still requires an Evaluator `PASS`.

## CLI Environment Facts

- Repository root in the current Windows Codex desktop environment: `D:\workspace\Codex\EqoChat2.0`.
- Network downloads are allowed in the current working agreement.
- Current permission profile has full filesystem access, but future runs should still read the environment context before assuming this.
- When approval policy is `never`, do not request approval and do not pass `sandbox_permissions`.
- The worktree may already be dirty. Never revert unrelated user or agent changes.
- Prefer `rg` / `rg --files` for searches.
- Prefer `multi_tool_use.parallel` for independent reads or status checks.
- Use `apply_patch` for manual file edits.

## Service Runtime Facts

- Backend default URL: `http://localhost:8080`.
- Frontend default URL: `http://localhost:3000`.
- Backend jar path: `backend/eqochat-server/target/eqochat-server-1.0.0.jar`.
- Local backend profile: `--spring.profiles.active=local`.
- Backend log used during verification: `/tmp/eqochat-backend-local.log`.
- Plain `nohup ... &` or short-lived shell background processes launched from the tool wrapper may be cleaned up or leave empty logs. For reliable verification, run Spring Boot in a long-lived `exec_command` session and stop it explicitly after smoke tests.
- After packaging a new backend jar, restart the running backend from that jar before smoke tests. Do not trust an old PID.

Backend restart recipe:

```bash
pid=$(lsof -tiTCP:8080 -sTCP:LISTEN || true)
if [ -n "$pid" ]; then
  kill "$pid" || true
  sleep 2
fi

cd /Users/drz/workspace/EqoChat2.0
java -jar backend/eqochat-server/target/eqochat-server-1.0.0.jar --spring.profiles.active=local
```

Health check recipe:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN || true
sed -n '1,140p' /tmp/eqochat-backend-local.log
```

## Configuration Rule

Never hardcode server IPs, database strings, Redis/Neo4j URLs, JWT secrets, or local personal paths into production code.

Use Spring Boot SOP:

- shared defaults in `application.yml`
- local-only values in `application-local.yml`
- production/staging through profiles, environment variables, or deployment secrets
- feature flags through typed properties or `@Value`, with production-safe defaults

Example learned in Sprint 1: demo fallback behavior must be gated by `eqochat.actor.demo-fallback.enabled:false` and disabled by default.

## Required Gates

Run the smallest relevant gate first, then broaden before handing to Evaluator.

Actor module:

```bash
cd backend
mvn -pl eqochat-business/eqochat-actor-parent/eqochat-actor -am clean test
```

Backend package:

```bash
cd backend
mvn -pl eqochat-server -am -DskipTests package
```

Static guard baseline:

```bash
bash -n scripts/smoke/actor-baseline-smoke.sh scripts/smoke-actor-baseline.sh scripts/actor-static-guard.sh
ALLOW_EXISTING=1 scripts/actor-static-guard.sh
```

The static guard treats `rg` exit code 1 as "no matches" and counts that rule as 0. Any other `rg` failure, including a non-executable `rg` on Git Bash/MSYS PATH, must fail the guard and print the failed rule, pattern, exit status, and stderr.

Guard self-test:

```bash
bash scripts/tests/actor-static-guard-test.sh
```

The guard output must include the resolved `rg` binary, `rg` version, scan paths, and scanned file count. A summary line alone is not enough evidence that scanning succeeded.

Baseline smoke:

```bash
scripts/smoke/actor-baseline-smoke.sh
```

The smoke entry point is a Bash wrapper around a Python 3 implementation. It uses Python's standard library for HTTP/JSON checks and does not require `jq`.
When invoked from WSL against a Windows-hosted backend, the Python smoke retries the Windows host address for localhost URLs.
The smoke covers `/api/v1/agents/me` legacy compatibility fields and Sprint 7A canonical policy fields such as `agentSubjectId/agentSubjectType`, `ownerSubjectId/ownerSubjectType`, `walletPolicyState`, `directRecipientSubjectId/directRecipientSubjectType`, `liableHumanId`, and `liabilityRoute`.
It also covers the Sprint 7B explicit Contact owner query by calling `/api/v1/contacts?ownerSubjectId={agentId}&ownerSubjectType=AGENT` for an owned agent and verifying the endpoint succeeds without emitting legacy `USER` subject types.
Sprint 7C extends the smoke with `/api/v1/notifications?recipientSubjectId={agentId}&recipientSubjectType=AGENT` for an owned agent, using the same endpoint-success and no-`USER` assertion because local seed data may not include an Agent recipient inbox.
Sprint 7D extends the smoke with `/api/v1/credits/subject` for a contact target and verifies `creditScore` is exposed in the PRD `300-850` range.
Sprint 7E extends the smoke with explicit Agent friend-request inbox queries for `/api/v1/friend-requests/received` and `/api/v1/friend-requests/sent`, verifying endpoint success, subject scoping for returned rows, and no legacy `USER` subject types.

Sprint 8A extends the smoke with `/api/v1/subjects/search` and `/api/v1/subjects/AGENT/{id}/public`, verifying canonical Agent discovery, subject public profile shape, subject-aware World stats presence, PRD-scale credit score, and no legacy `USER` subject type.

Smoke login:

- phone: `13900000001`
- password: `Test1234`

Static guard baseline at the end of Sprint 1:

- `ALLOW_EXISTING=1 scripts/actor-static-guard.sh` passes.
- Expected legacy findings remain `P0=17`, `WARN=27`.
- These findings are not Sprint 1 failures unless a touched module claims to have cleared them.

Static guard at the end of Sprint 10 / Sprint 11:

- Strict `scripts/actor-static-guard.sh` should pass with `P0=0`.
- Current strict summary: `P0=0 WARN_RUNTIME=0 WARN_HISTORY=28 WARN=28`.
- `WARN_HISTORY` is allowed for historical Flyway migrations and architecture notes.
- Runtime/API/WebSocket/JWT/session paths must not accept or emit `USER` for compatibility.
- The smoke entry point uses Python 3 standard library JSON/HTTP handling and does not require `jq`.
- Sprint 11 added runtime closure checks for registry-authoritative subject search, actor-backed `/api/v1/users` compatibility, registry-only World author reads, `SubjectRelationshipApi`, and Contact inbox scoping through `SubjectDirectoryApi.listAssociatedSubjects`.
- Sprint 12A removed Project implementation dependencies on `eqochat-user` / `eqochat-agent` and added a guard against Project direct human/agent implementation coupling.
- Sprint 12B removed World SQL coupling to `user_friend`; World relationship flags and friends sorting now consume `SubjectRelationshipApi.listFriends`, and the guard blocks `user_friend` table SQL in World runtime code.
- Sprint 12C moved Contact relationship Java ownership to `ContactRelationship` / `ContactRelationshipMapper`, removed Contact dependencies on `eqochat-user` and `eqochat-user-api`, and expanded the guard against Contact direct human/agent implementation or API coupling. The physical table remains `user_friend` until a later database migration sprint.
- Sprint 12D removed Actor dependencies on `eqochat-user-api`, `eqochat-user`, and `eqochat-agent`; Actor source-profile fallback now goes through Actor-owned `ActorSourceRepository`, and the guard blocks Actor direct human/agent implementation or API coupling.
- Sprint 12E made WebSocket runtime delivery subject-aware: connection establishment subscribes the socket to `SubjectDirectoryApi.listAssociatedSubjects(principalHumanId)`, conversation fanout uses subject indexes, and same-socket multi-subject broadcasts are de-duplicated. `principalHumanId` remains the authentication principal, and explicit `SUBJECT_SUBSCRIBE` remains the active-subject switch.
- Sprint 12F made Contact group owner/member identity subject-aware by adding `owner_type` / `member_type`, backfilling historical rows to `HUMAN`, dropping group hard FKs to `user_profile`, and guarding group mappers against direct human-only owner/member queries.
- Sprint 12G made Credit audit actor fields subject-aware by adding `operator_type`, `reporter_type`, and `reviewer_type`, backfilling historical audit actors to `HUMAN`, and guarding Credit mappers against direct human-only audit actor queries.
- Sprint 12H moved Contact relationship/tag runtime storage from `user_friend` / `user_contact_tag` to `contact_relationship` / `contact_tag`, renamed the Java tag model to `ContactTag`, added compatibility views for old table names, and guarded Contact runtime code against reintroducing legacy storage names.
- Sprint 12I retired the legacy public `/api/v1/users` search/profile compatibility surface; subject search and public profiles are now exposed only through `/api/v1/subjects`, and the guard blocks backend or frontend reintroduction of the old route.
- Sprint 12J made Actor subject runtime reads registry-only: `SubjectDirectoryServiceImpl.getSubject` no longer falls through to source tables on registry miss; `refreshHuman` / `refreshAgent` remain explicit registry sync adapters.
- Sprint 12K closed the run with full backend tests, frontend H5 build, strict guard (`P0=0 WARN_RUNTIME=0 WARN_HISTORY=28 WARN=28`), latest-jar startup at Flyway v26, and actor baseline smoke including the legacy `/api/v1/users/search` 404 assertion.
- Sprint 12L retired User-owned social graph runtime artifacts: `UserFriend` / `UserFollow` Java storage mappers, User-module Neo4j social entities, and the unused Neo4j runtime starter/config are removed. The guard now blocks reintroducing User-owned friend/follow storage, User-module Neo4j graph models, or Server-level Neo4j runtime config. Historical migrations and compatibility views remain as history/DB compatibility, not runtime ownership.
- Sprint 13A retired the human-shaped World author surface: `/api/v1/world/users/{authorId}/posts`, `WorldPostStatsApi.countByAuthorId`, and source mappers now use canonical subject author identity.
- Sprint 13B made WebSocket presence and conversation online state subject-aware. Presence payloads are emitted per subscribed subject, including owned/associated agents, and fanout is scoped to shared conversation subject sessions instead of all online principals; summary `online` now uses the subject session index. `principalHumanId` remains the authenticated connection/session owner.
- Sprint 13C made World read viewer identity explicit. World read endpoints now reject missing `viewerSubjectId/viewerSubjectType`, legacy `Long viewerId` service overloads were removed, and the guard blocks implicit `SubjectRef.human(principalHumanId)` viewer fallback in World runtime paths.
- Sprint 13D made Project read viewer identity explicit. Project list/detail/sidebar/share paths now require `viewerSubjectId/viewerSubjectType`, legacy `Long viewerId` service overloads were removed, and project visibility no longer aggregates through principal-human fallback.
- Sprint 13E made Chat conversation viewer/creator identity explicit. Conversation list/detail/messages require a viewer subject, create-conversation requires a creator subject, and legacy principal-human read/create fallbacks were removed.
- Sprint 13F made Contact, Notification, FriendRequest, and Credit touched paths fail fast instead of defaulting missing subject type or owner/recipient/inbox/audit actor identity to `HUMAN`.
- Sprint 13G made Subject search/public-profile viewer identity explicit. Viewer-dependent fields such as `isFriend` now use the active viewer subject and the guard blocks `SubjectProfileServiceImpl` from reintroducing `SubjectRef.human(principalHumanId)` viewer fallback.
- Sprint 13H removed the remaining frontend, WebSocket, and World active-subject fallbacks that derived business identity from the authenticated human principal. Realtime startup, WebSocket sends/subscriptions, World viewer/actor params, and conversation read params now require explicit active subject identity.
- Sprint 13I made `SubjectDirectoryApi.listAssociatedSubjects(principalHumanId)` registry-authoritative for representation authority: the authenticated human's own `HUMAN` subject is returned only when present in `subject_registry`, and business services must still receive explicit viewer/actor/owner subjects without synthesizing them from `principalHumanId`.
- Sprint 13J closed Project write principal/actor vocabulary: Project write services use `principalHumanId` for the authenticated human, actor/owner subjects remain explicit request facts, frontend write actor fields are required, and the guard blocks Project write `Long viewerId` regressions.
- Sprint 13K centralized wallet settlement subject derivation in `WalletCapability.settlementSubject()`. Agent and Project callers consume Actor policy settlement facts, Project payment creation persists those facts, and the guard blocks caller-side settlement-subject reconstruction.
- Sprint 13L completed final guard/doc/runtime closure: strict guard reports `P0=0 WARN_RUNTIME=0 WARN_HISTORY=24 WARN=24`, frontend H5 build passes, backend `mvn test` passes, server package build passes, and actor baseline smoke passes on port `18080`.

## Project Knowledge Snapshot

Canonical Actor Model identity:

- `SubjectRef(id, type)` is the cross-module identity unit.
- Canonical subject types are `HUMAN`, `AGENT`, and `SYSTEM`.
- `USER` is only a historical-storage cleanup concern; normal APIs, WebSocket payloads, domain services, and write paths must not accept or emit it for compatibility.
- Because the app is pre-consumer, backward compatibility is not a reason to keep incorrect Actor Model vocabulary. Remove legacy request aliases and overloaded protocol fields when they are in touched scope.
- Keep `principalHumanId`, `actorSubject`, and `liableHumanId` distinct.
- New cross-subject storage fields should be `{role}_id + {role}_type`.

Sprint 1 accepted state:

- Evaluator returned `PASS` after the second audit.
- `SubjectType.from` is canonical-only; there is no runtime `USER -> HUMAN` adapter. Historical rows must be corrected by migrations.
- `SubjectRef` rejects null type.
- Liability validates active agent, active owner, active OWNER binding, and accepted liability.
- Wallet rejects null/system, validates agent liability, uses `agent_wallet_state`, and does not silently route system actors.
- Demo points and legacy `source_config` wallet fallback are disabled unless demo fallback is explicitly enabled.
- Capability and Milestone policies do not grant ordinary benefits to missing, inactive, or system subjects.
- `ActorDataAccess` table existence checks cache true only, not false.
- Actor hardening test count is 18.

Known residual risks from Evaluator:

- Some DB read failures degrade conservatively with generic unavailable reasons. This is acceptable while it does not wrongly enable capabilities.
- `WalletCapability.agentToOwner` settlement and payment audit facts are now frozen through Project payment creation; callers must use `WalletCapability.settlementSubject()`.
- Some Sprint 1 notes contain historical "8 tests passed" lines before the later 13-test state; keep final docs clear when preparing external summaries.

## Sprint 2 Handoff

Recommended next sprint: Chat Actorization.

Focus:

- participants use `subject_id + subject_type`
- senders use `subject_id + subject_type`
- read receipts use `subject_id + subject_type`
- unread counts must not collide when human and agent IDs overlap
- WebSocket events include actor subject identity
- existing human chat must keep passing
- agent messages must remain auditable to the liable human

Do not regress baseline flows:

- login
- auth me
- agents me, including Sprint 7A policy surface fields
- contacts
- project list/detail/sidebar
- world feed/create/reply
- conversation create/list/messages/send/read
- Flyway startup

## Finalization Rule

Before saying a sprint is done:

1. Confirm the latest code is packaged if runtime behavior changed.
2. Restart backend from the latest jar with the `local` profile.
3. Run baseline smoke against that running process.
4. Send exact gate evidence to Evaluator.
5. Wait for explicit Evaluator `PASS`.

## Local Runtime Notes

- In this Codex app environment, starting the Spring Boot jar as a short-lived shell background job can leave no running process and an empty log because the execution wrapper cleans up child processes. Run the backend in a long-lived `exec_command` session instead.
- If Flyway reports a failed local migration, inspect `flyway_schema_history` and the partially changed schema before rerunning. For the local development DB, removing the failed `success=0` history row is equivalent to the needed `repair` step when the failed migration has no successful checksum yet.
- V12 depends on `agent_binding.binding_status`, not `agent_binding.status`.
