# Sprint 12B Implementation Notes: World Relationship Boundary Closure

Sprint 12B removes World runtime coupling to the legacy `user_friend` relationship table.

## Scope

- Added `SubjectRelationshipApi.listFriends(SubjectRef owner)`.
- Implemented `listFriends` in Contact over the existing relationship storage.
- Removed the `LEFT JOIN user_friend` dependency from `WorldPostMapper`.
- Changed World feed `friends` sorting to receive typed friend id lists from `SubjectRelationshipApi`.
- Changed World response `friend` flags to be marked in `WorldServiceImpl` from `SubjectRelationshipApi.listFriends`, not SQL table joins.
- Added a static guard P0 rule blocking `user_friend` table SQL in World runtime code.

## Boundary Decision

Contact still owns the current relationship storage implementation. World no longer depends on the storage table or `UserFriend` types. This preserves behavior while moving the module boundary to:

- `SubjectRelationshipApi.areFriends`
- `SubjectRelationshipApi.listFriends`

## Verification

- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`
- PASS: `mvn -pl eqochat-business/eqochat-world-parent/eqochat-world,eqochat-business/eqochat-contact-parent/eqochat-contact -am test`

## Remaining Work

- Contact still imports `UserFriend` / `UserFriendMapper` from the User implementation module. The next storage-ownership sprint should move that relationship model into Contact-owned storage, or rename it to a subject relationship model.
- Actor still has controlled source-profile adapters for registry sync and validation.
- WebSocket presence/session runtime remains principal-human first and should be converted to subject-first behavior in a later sprint.
