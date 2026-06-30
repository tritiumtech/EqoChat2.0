# Sprint 4A Implementation Notes

Date: 2026-06-30

## Scope

Sprint 4A cuts Contact and FriendRequest runtime paths over to canonical Actor Model subject identity.

- Contact API responses now expose `ownerSubjectId/ownerSubjectType` and `targetSubjectId/targetSubjectType`.
- Contact detail and tag routes use `/api/v1/contacts/{targetType}/{targetId}` and `/api/v1/contacts/{targetType}/{targetId}/tags`.
- Friend request requests use `actorSubjectId/actorSubjectType` and `recipientSubjectId/recipientSubjectType`.
- Friend request responses use `requesterSubjectId/requesterSubjectType` and `recipientSubjectId/recipientSubjectType`.
- Frontend positive paths use `HUMAN` and `AGENT`; no runtime `USER` compatibility alias was kept.

## Backend Changes

- `ContactServiceImpl` now takes `SubjectRef owner` and `SubjectRef target`, checks liability through `LiabilityPolicyApi`, resolves display through `SubjectDirectoryApi`, rejects `SYSTEM`, and scopes relation/tag queries by subject type.
- `FriendRequestServiceImpl` now:
  - validates requester and recipient as relationship subjects;
  - authorizes the requester/recipient through liability policy;
  - stores requester and recipient type;
  - creates reciprocal typed `user_friend` rows on accept;
  - sends notifications using canonical `SubjectRef` sender and recipient;
  - includes canonical subject fields in notification payloads.
- `UserFriendMapper`, `FriendRequestMapper`, and `UserContactTagMapper` queries include subject type predicates.
- V19 migration adds/backfills contact subject columns, removes user-profile FKs that blocked agent subjects, and creates subject-aware indexes/uniques.
- `scripts/smoke/actor-baseline-smoke.sh` now checks contact response `targetSubjectType` instead of old `friendType`.

## Frontend Changes

- Friend request API/store/pages were updated from `friendId/requesterId/recipientId` to canonical subject fields.
- Contact API/pages now use `targetSubjectId/targetSubjectType` and the canonical detail/tag routes.
- Contact list/detail/user profile/friend requests/search routes pass subject identity in query params.
- World mention contact picker uses `targetSubjectId/targetSubjectType`.

## Verification

- `cd backend && mvn -pl eqochat-business/eqochat-contact-parent/eqochat-contact -am test`
  - PASS, 10 contact tests.
- `cd backend && mvn -pl eqochat-business/eqochat-world-parent/eqochat-world -am test`
  - PASS, 10 world tests.
- `cd backend && mvn -pl eqochat-server -am -DskipTests package`
  - PASS.
- `cd frontend && npm run build:h5`
  - PASS.
- `scripts/actor-static-guard.sh`
  - PASS, `P0=0`, `WARN=43`.
- `git diff --check`
  - PASS.
- Backend runtime:
  - started latest `backend/eqochat-server/target/eqochat-server-1.0.0.jar` with `--spring.profiles.active=local`;
  - Flyway applied V19 successfully;
  - local schema now has `user_friend.uk_user_friend_subject`, `user_contact_tag.uk_user_contact_tag_subject`, and typed friend request indexes;
  - no user-profile FKs remain on `user_friend`, `friend_request`, or `user_contact_tag`.
- `scripts/smoke/actor-baseline-smoke.sh`
  - PASS.

## Notes

- Java entity fields such as `FriendRequest.requesterId`, `FriendRequest.recipientId`, `UserFriend.friendId`, and `UserContactTag.friendId` still mirror historical DB column names, but every runtime query/write path now pairs them with canonical type fields.
- The strict static guard warnings are historical migration/comment/documentation debt, not Sprint 4A runtime `USER` usage.
- Evaluator found one post-implementation P0 in World friend checks: feed/list-by-author used `user_friend.user_id` without owner type. The fix scopes the SQL join with `uf.user_type = 'HUMAN'` and routes service checks through `UserFriendMapper.areFriends(viewerId, HUMAN, authorId, authorType)`.
