# Actor Refactor Harness

Last updated: 2026-06-29.

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
  - Evaluator: `019f1248-67a6-7c53-ada6-54f46b093558` (`Socrates`)
  - Generator: `019f126c-d4e1-7981-8461-63b80d9cafda` (`Peirce`)

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

- Repository root: `/Users/drz/workspace/EqoChat2.0`.
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

Baseline smoke:

```bash
scripts/smoke/actor-baseline-smoke.sh
```

Smoke login:

- phone: `13900000001`
- password: `Test1234`

Static guard baseline at the end of Sprint 1:

- `ALLOW_EXISTING=1 scripts/actor-static-guard.sh` passes.
- Expected legacy findings remain `P0=17`, `WARN=27`.
- These findings are not Sprint 1 failures unless a touched module claims to have cleared them.

Static guard at the end of Sprint 2:

- Strict `scripts/actor-static-guard.sh` should pass with `P0=0`.
- Known warnings may remain for historical migrations, architecture transition text, Credit legacy scope, and mirror-profile debt.
- Sprint 2 runtime/API/WebSocket/JWT/session paths must not accept or emit `USER` for compatibility.

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
- Actor hardening test count is 13.

Known residual risks from Evaluator:

- Some DB read failures degrade conservatively with generic unavailable reasons. This is acceptable while it does not wrongly enable capabilities.
- `WalletCapability.agentToOwner` should persist explicit settlement and audit facts in the payment sprint.
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
- agents me
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
