# Sprint 3A Implementation Notes: World Author Subject Cut

## Scope

- World post authors are now rendered from `world_post.author_id + author_type`.
- Active post feed, topic, author, mention, and my-post queries no longer infer author type from `agent_profile`.
- Post author joins are type-gated: `user_profile` is joined only for `HUMAN`, and `agent_profile` only for `AGENT`.
- `my-posts` remains the current-principal human timeline and filters `author_type = 'HUMAN'`.
- The canonical author timeline endpoint is `/api/v1/world/subjects/{authorType}/{authorId}/posts`; the existing `/users/{authorId}/posts` endpoint is isolated to `HUMAN`.
- World replies now store `world_post_reply.author_type`; the human-principal reply path writes `HUMAN`.
- Reply display identity resolves through `SubjectDirectoryApi` from `author_id + author_type`, rather than user-only profile lookup.
- `author.ai` is derived only from canonical subject type `AGENT`.

## Migration

- `V16__world_reply_author_identity.sql`
  - Adds `world_post_reply.author_type` with canonical `HUMAN/AGENT/SYSTEM` comments.
  - Backfills missing, blank, and historical `USER` reply/post author types to `HUMAN`.
  - Adds an author subject index for replies.
  - Updates post/reply author type comments to canonical actor vocabulary.

## Compatibility Position

`USER` is not accepted in runtime World author rendering or canonical author queries. Historical `USER` appears only in migration cleanup and the negative regression test.

The project is pre-consumer, so this sprint also tightened touched WebSocket protocol vocabulary instead of preserving incorrect legacy compatibility. `BaseMessage.recipientId` was removed from backend and frontend protocol types; routing now stays in canonical subject fields and typed payload fields such as `conversationId`.

## Gates

- `mvn -pl eqochat-business/eqochat-world-parent/eqochat-world -am test`: PASS.
- `mvn -pl eqochat-business/eqochat-chat-parent/eqochat-chat -am test`: PASS.
- `mvn -pl eqochat-server -am -DskipTests package`: PASS.
- `npm run build:h5`: PASS.
- `scripts/actor-static-guard.sh`: PASS, `P0=0 WARN=34`.
- `git diff --check`: PASS.
- Local jar startup with `--spring.profiles.active=local`: PASS; Flyway validated 17 migrations and applied V16.
- `scripts/smoke/actor-baseline-smoke.sh`: PASS.

## Evaluator Result

- `Socrates` returned `PASS` for Sprint 3A.
- Non-blocking next debts: contact/world post count still has a human-only path, and World mentions/follows/upvotes/notification fanout still need subject-native identity.
