# Sprint 6A Implementation Notes: World Create/Reply Actorization

Date: 2026-06-30

## Scope

Sprint 6A actorizes the World create-post and create-reply write paths.

- Create post/reply requests now require an explicit canonical actor subject.
- `principalHumanId` remains the authenticated human session owner.
- `actorSubjectId/actorSubjectType` is the subject performing the World action.
- `HUMAN` actors must match `principalHumanId`.
- `AGENT` actors must resolve liability to `principalHumanId`.
- `SYSTEM` actors are rejected for World user-authored content.
- Post/reply rows persist the actor as `author_id + author_type`.
- Mention notifications use the posting actor as sender.

## Backend Changes

- `CreateWorldPostRequest` requires `actorSubjectId` and `actorSubjectType`.
- `CreateWorldPostReplyRequest` requires `actorSubjectId` and `actorSubjectType`.
- `WorldServiceImpl.createPost` resolves a canonical `SubjectRef` before writing:
  - rejects missing, non-positive, null-type, or `SYSTEM` actors with `world.actor.invalid`;
  - rejects a human actor that differs from the authenticated human with `world.actor.forbidden`;
  - rejects an agent actor unless `LiabilityPolicyApi.resolveLiability(actor).liableHumanId()` equals the authenticated human.
- `WorldServiceImpl.createReply` applies the same actor resolution rules.
- Post and reply creation now writes `authorId=actor.id()` and `authorType=actor.type().name()`.
- Mention storage uses canonical `mentionedSubjectId/mentionedSubjectType`.
- Self-mention filtering compares full `SubjectRef`, so `HUMAN:101` and `AGENT:101` do not collide.
- Mention notification sender is the actor `SubjectRef`, not a forced human sender.
- World share links no longer fall back to a hardcoded local URL. Invalid/missing templates fail closed with `world.share_url_template.invalid`.

## Frontend And Smoke Changes

- `frontend/src/api/modules/world.ts` declares `actorSubjectId/actorSubjectType` on create-post and create-reply payloads.
- `frontend/src/pages/world/world.vue` passes the current principal human as an explicit `HUMAN` actor for normal UI post/reply flows.
- `scripts/smoke/actor-baseline-smoke.sh` sends explicit `actorSubjectId/actorSubjectType=HUMAN` for World post/reply creation.

## Tests

- `WorldServiceImplActorAuthorTest` covers:
  - human post creation persists `HUMAN` author type;
  - human reply creation persists `HUMAN` author type;
  - authorized agent post creation persists `AGENT` author and sends notifications from the agent;
  - authorized agent reply creation persists `AGENT` author;
  - agent actor whose liable human differs from the principal is rejected;
  - `SYSTEM` actors are rejected;
  - full-subject self-mention filtering does not collapse human and agent numeric ids;
  - canonical mention subjects persist and notify both human and agent recipients.

## Verification

- `cd backend && mvn -pl eqochat-business/eqochat-world-parent/eqochat-world -am test`
  - PASS, 15 World actor contract tests.
- `cd backend && mvn -pl eqochat-server -am -DskipTests package`
  - PASS.
- `cd frontend && npm run build:h5`
  - PASS.
- `bash -n scripts/smoke/actor-baseline-smoke.sh scripts/actor-static-guard.sh`
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

- Sprint 6A deliberately does not keep legacy `mentionedUserIds` or implicit-human author behavior in the touched write path.
- The normal frontend UI still posts as the authenticated human. Agent-authored World posting is now enabled at the API/service contract level and can be surfaced in a later UI sprint.
