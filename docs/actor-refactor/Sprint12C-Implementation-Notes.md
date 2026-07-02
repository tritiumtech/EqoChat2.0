# Sprint 12C Implementation Notes: Contact Relationship Storage Ownership

Sprint 12C moves Java ownership of contact relationship storage out of the User implementation module and into Contact.

## Scope

- Added Contact-owned `ContactRelationship` and `ContactRelationshipMapper`.
- Mapped `ContactRelationship` to the existing `user_friend` table to avoid a risky storage rename in the same sprint.
- Changed `ContactServiceImpl`, `FriendRequestServiceImpl`, `UserContactTag`, and `UserContactTagMapper` to use Contact-owned relationship subject/status enums.
- Removed `eqochat-contact` dependencies on `eqochat-user` and `eqochat-user-api`.
- Updated Contact tests to use `ContactRelationshipMapper` instead of `UserFriendMapper`.
- Expanded the static guard to block Contact from reintroducing direct human/agent implementation or API dependencies.

## Boundary Decision

The Contact module now owns the relationship Java model and mapper. The physical table name remains `user_friend` for compatibility with existing migrations and local data.

The remaining table rename should be handled as a later database migration sprint, with a deliberate compatibility plan for Flyway, seed data, and any local development state.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-contact-parent/eqochat-contact -am test`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`

## Remaining Work

- Rename or migrate the underlying relationship table from `user_friend` to a Contact-owned subject relationship name.
- Actor still contains controlled source-profile adapters for registry sync and validation.
- WebSocket session and presence runtime remain principal-human first.
- Contact group tables still have human-shaped ownership fields.
