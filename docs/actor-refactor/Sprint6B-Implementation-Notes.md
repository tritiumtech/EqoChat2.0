# Sprint 6B Implementation Notes: Frontend Runtime Config And World Helper Cleanup

Date: 2026-06-30

## Scope

Sprint 6B cleans up frontend runtime configuration and the World API helper after Sprint 6A.

- Shared/production frontend source must not hardcode `localhost`, `127.0.0.1`, or personal LAN backend URLs.
- Development-local fallbacks are allowed only behind `import.meta.env.DEV` or ignored local env files.
- World API helper signatures must not default `actorSubjectType` to `HUMAN`.
- Current human-authored UI flows must keep passing explicit `HUMAN` actor subjects.

## Frontend Changes

- Added `frontend/src/utils/runtime-config.ts` to centralize runtime endpoint resolution.
- REST requests now use `API_BASE_URL` from the runtime config utility.
- Upload APIs now use `buildApiUrl(...)` instead of each module carrying its own `VITE_API_BASE_URL` fallback.
- WebSocket default config now uses `WS_BASE_URL` from the runtime config utility:
  - explicit `VITE_WS_URL` wins;
  - development mode falls back to `ws://localhost:8080`;
  - production H5 can infer the current page host as `ws/wss`;
  - non-browser production runtimes fail explicitly when `VITE_WS_URL` is missing.
- `frontend/vite.config.ts` only falls back to `http://localhost:8080` in development mode.
- `worldApi.replyToPost(...)` now requires an explicit `actorSubjectType`; it no longer defaults to `HUMAN`.
- Existing World UI reply/post flows continue to pass `actorSubjectId=principalHumanId` and `actorSubjectType='HUMAN'` explicitly.

## Local Environment Cleanup

- The ignored local file `frontend/env/.env.production` was changed on this machine to blank production API/WS values, so local `npm run build:h5` no longer embeds a personal LAN backend address.
- This env file is intentionally not a tracked artifact because `frontend/env/.env*` is gitignored; tracked examples already document deployment-provided endpoints.

## Verification

- `npm run build:h5`
  - PASS.
  - Build environment showed `VITE_API_BASE_URL=''`, `VITE_WS_URL=''`, and `VITE_PROXY_TARGET=''`.
- `pnpm build:h5`
  - BLOCKED by pnpm 11 dependency build approval policy during automatic install: `ERR_PNPM_IGNORED_BUILDS`.
  - This was a local package-manager policy issue, not a source compile failure. Generated pnpm workspace/lock artifacts were removed.
- Frontend grep checks:
  - no `127.0.0.1`, `10.10.7.8`, or `192.168.130.70` remains in tracked frontend source checks;
  - `localhost:8080` remains only in `import.meta.env.DEV` branches.
  - no `actorSubjectType = 'HUMAN'` default remains in `worldApi.replyToPost`.
- `cd backend && mvn -pl eqochat-server -am -DskipTests package`
  - PASS.
- `scripts/actor-static-guard.sh`
  - PASS, `P0=0`, `WARN=67`.
- `git diff --check`
  - PASS.
- Backend runtime:
  - started latest `backend/eqochat-server/target/eqochat-server-1.0.0.jar` with `--spring.profiles.active=local`;
  - Flyway validated 21 migrations;
  - schema current version is v20;
  - server started on port 8080.
- `scripts/smoke/actor-baseline-smoke.sh`
  - PASS across login, auth me, agents, contacts, projects, project sidebars, world feed/create/reply, conversation create/list/messages, message send, and message read.

## Notes

- Production App builds should provide `VITE_API_BASE_URL` and `VITE_WS_URL` through deployment config because they cannot infer a browser page host.
- This sprint does not attempt to finish all remaining frontend display aliases such as `isAgent`; those remain presentation-layer vocabulary and are outside this cleanup scope unless they become authoritative identity.
