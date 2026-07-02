# Sprint 6C Implementation Notes: World Read And Engagement Actorization

Date: 2026-07-01

## Scope

Sprint 6C finishes the World read/engagement actorization started in Sprint 6A and stabilized in Sprint 6B.

- World read paths accept an explicit viewer subject while keeping `principalHumanId` as the authenticated human session owner.
- World engagement write paths accept an explicit actor subject for post upvote, reply upvote, and topic follow.
- World queries and engagement records must distinguish `HUMAN` and `AGENT` subjects even when their numeric ids overlap.
- Controller-level subject parameters are normalized into `SubjectRef(id, type)` before entering service logic.
- Service-level authorization keeps `principalHumanId`, viewer/actor `SubjectRef`, and liable human distinct.
- Static guard diagnostics are hardened so guard failures cannot be mistaken for a clean zero-match scan.

## Non-Scope

- No big-bang storage merge into a unified users table.
- No new `SubjectType.USER`, `senderType("USER")`, `RecipientType.USER`, `ReaderType.USER`, or equivalent compatibility alias.
- No business-code fallback that silently maps legacy `USER` to `HUMAN`.
- No long-term replacement of all World mirror-profile debt beyond the touched read/engagement paths.
- No independent Agent World posting UI; normal frontend flows may still use the principal human as the explicit subject until product UI exposes agent switching.
- No Credit, Project, Chat, Contact, wallet, or liability-policy redesign beyond calls needed to authorize World subjects.

## Key Changes

- `WorldService` now has SubjectRef-aware overloads for:
  - feed listing;
  - posts-by-author listing;
  - topic listing;
  - topic-post listing;
  - mentioned-me listing;
  - my-post listing;
  - post upvote;
  - topic follow;
  - reply listing;
  - reply upvote.
- `WorldController` accepts `viewerSubjectId/viewerSubjectType` on World read endpoints and `actorSubjectId/actorSubjectType` on engagement write endpoints, then resolves them into canonical `SubjectRef` values.
- Read controller fallback remains human-principal based when no viewer params are provided, preserving existing human UI behavior without introducing a `USER` compatibility type.
- Engagement writes require explicit `actorSubjectId/actorSubjectType`; viewer parameters are not accepted as actor aliases.
- `WorldServiceImpl.requireAuthorizedSubject(...)` rejects null, non-positive, null-type, or `SYSTEM` subjects with `world.actor.invalid`.
- Human subjects must match `principalHumanId`; otherwise `world.actor.forbidden` is returned.
- Agent subjects must resolve through `LiabilityPolicyApi.resolveLiability(actor)` to the authenticated `principalHumanId`; otherwise `world.actor.forbidden` is returned.
- World SQL reads now carry both viewer id and viewer type:
  - friend joins use `uf.user_id = #{viewerId}` and `uf.user_type = #{viewerType}`;
  - post upvote joins use `up.voter_id = #{viewerId}` and `up.voter_type = #{viewerType}`;
  - topic follow joins use `tf/f.follower_id = #{viewerId}` and `follower_type = #{viewerType}`;
  - mention feeds use `mentioned_subject_id + mentioned_subject_type`;
  - my-post feeds use `author_id + author_type`.
- Engagement writes now persist full subject identity:
  - post upvotes write `voter_id + voter_type`;
  - reply upvotes write `voter_id + voter_type`;
  - topic follows write `follower_id + follower_type`.
- `listPostsByAuthor` friend checks now compare full subject pairs, including an agent viewer to human/agent author case.
- Reply read state uses `selectActiveByVoterAndReplyIds(viewerId, viewerType, replyIds)` so `HUMAN:101` and `AGENT:101` do not collide.
- Tests were expanded in `WorldServiceImplActorAuthorTest` for agent read paths, agent engagement writes, unauthorized agent rejection, `SYSTEM` rejection, same-numeric human/agent collision protection, and SQL contract checks.
- `scripts/actor-static-guard.sh` now reports the resolved `rg` binary, `rg` version, scan paths, and scanned file count.
- Static guard now treats `rg` exit code `1` as "no matches" only for rule scans, while other `rg` failures fail with rule label, pattern, exit status, and stderr.
- `docs/actor-refactor/HARNESS.md` records the updated guard behavior and the guard self-test command.

## Frontend Subject Parameters

- `frontend/src/api/modules/world.ts` now derives the principal human subject from `userInfo.id`, with JWT `principalHumanId/sub` fallback.
- Existing frontend World create/reply flows from Sprint 6A/6B continue to pass explicit `actorSubjectId/actorSubjectType=HUMAN`.
- Sprint 6C frontend helpers now pass explicit `viewerSubjectId/viewerSubjectType` or `actorSubjectId/actorSubjectType` when calling:
  - feed/topic/mention/my-post/reply read endpoints;
  - post upvote;
  - reply upvote;
  - topic follow.
- Engagement helper parameters are serialized into the query string because the backend contract uses `@RequestParam`.
- Until an agent switcher exists, the expected UI subject is the principal human with type `HUMAN`; helper methods accept an optional subject argument so future agent-subject UI can pass `AGENT` without another backend contract change.

## Verification Checklist

- Backend unit tests:
  - PASS: `cd backend && mvn -pl eqochat-business/eqochat-world-parent/eqochat-world -am '-Dtest=WorldServiceImplActorAuthorTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
  - Result: `Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`.
- Backend package:
  - PASS after stopping the old backend process that held the target jar open: `cd backend && mvn -pl eqochat-server -am '-DskipTests' package`.
  - First attempt failed at Spring Boot repackage because Windows could not rename the running `eqochat-server-1.0.0.jar`; this was a file-lock issue, not a compilation failure.
- Frontend build after subject-parameter helper changes:
  - PASS: `cd frontend && npm run build:h5`.
- Static guard syntax/self-test:
  - PASS: `bash -n scripts/smoke/actor-baseline-smoke.sh scripts/smoke-actor-baseline.sh scripts/actor-static-guard.sh`.
  - PASS: `bash scripts/tests/actor-static-guard-test.sh`.
- Static guard contract:
  - PASS: `bash scripts/actor-static-guard.sh`.
  - Result: resolved `rg` binary and version shown; scan paths `backend frontend`; files scanned `1021`; `P0=0 WARN=67`.
- Diff hygiene:
  - PASS: `git diff --check`.
  - Output only showed Git's CRLF normalization warnings; no whitespace errors were reported.
- Runtime smoke:
  - PASS: latest backend jar restarted with `--spring.profiles.active=local`; `/api/v1/health` returned code `200`.
  - PASS: `scripts/smoke/actor-baseline-smoke.sh`.
  - PASS: `scripts/smoke-actor-baseline.sh`.
  - The smoke no longer depends on `jq`; the Bash entry point delegates JSON and HTTP checks to Python 3 standard-library code.
  - Runtime coverage includes login, auth me, agents, contacts, project list/detail/sidebar, World explicit-viewer feed, World create, World explicit-actor upvote, World reply, World explicit-viewer replies, conversation create/list/messages/send/read.
- Evaluator handoff:
  - include exact gate output;
  - wait for explicit Evaluator `PASS` before declaring Sprint 6C complete.

## Known Residuals

- Baseline smoke now requires Python 3 instead of `jq`; the root compatibility entry point remains `scripts/smoke-actor-baseline.sh`.
- Do not run multiple smoke instances concurrently with the same seeded login; session replacement can make one run receive `Session expired`.
- Read-only service overloads that accept only `viewerId` remain as human-principal adapters for existing call sites; engagement write overloads now require explicit `SubjectRef`.
- World still relies on existing `user_friend` and World tables rather than a unified `subject_registry`.
- World post rendering still carries display fields such as `ai` for UI compatibility; these are presentation hints, not authoritative identity.
- Existing mirror-profile and historical World data cleanup remain long-term debt outside Sprint 6C.
- Guard hardening improves scan reliability, but Windows/Git Bash path behavior should remain part of verification evidence because local tool resolution can vary.
