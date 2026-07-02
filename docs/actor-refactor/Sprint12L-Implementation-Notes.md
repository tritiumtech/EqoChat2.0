# Sprint 12L Implementation Notes: User Social Graph Runtime Retirement

Sprint 12L removes the final User-module social graph runtime artifacts found after the Sprint 12K closure audit.

## Scope

- Deleted User-owned legacy relationship storage classes:
  - `UserFriend`
  - `UserFriendMapper`
  - `UserFollow`
  - `UserFollowMapper`
- Deleted the unused User-module Neo4j social graph entity package:
  - `SocialUser`
  - `SocialAgent`
  - `SocialGroup`
  - `FriendWith`
  - `Follows`
  - `Owns`
  - `MemberOf`
  - `InteractsWith`
  - `RelatedTo`
  - `SimilarTo`
- Removed unused Neo4j runtime dependencies/configuration from:
  - `eqochat-user/pom.xml`
  - `eqochat-server/pom.xml`
  - `application.yml`
  - `application-local.yml`
  - `application.example.yml`
- Updated `eqochat-user` package docs so the module boundary is auth/profile only; relationships, contacts, and graph state belong to Actor/Contact/World.
- Added static guard rules blocking:
  - User-module `UserFriend` / `UserFollow` runtime storage ownership.
  - User-module Neo4j social graph runtime ownership.
  - Server-level Neo4j runtime dependency/config reintroduction.
- Expanded the static guard fixture to prove the new rules fail on legacy friend/follow/Neo4j artifacts.

## Boundary Decision

User remains the source module for authentication and human profile persistence. It no longer owns friend/follow/contact/social graph storage or runtime graph models.

The historical `user_follow` table, old migration references, and V26 compatibility views are intentionally left in database history. They are not runtime Java ownership. Any physical data retirement should be handled by a separate data migration sprint after product semantics for subject-native follows are fixed.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-user-parent/eqochat-user -am test`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash -n scripts/smoke/actor-baseline-smoke.sh scripts/smoke-actor-baseline.sh scripts/actor-static-guard.sh scripts/tests/actor-static-guard-test.sh`
- PASS: `python -m py_compile scripts/smoke/actor-baseline-smoke.py`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`
  - Strict summary: `P0=0 WARN_RUNTIME=0 WARN_HISTORY=28 WARN=28`
- PASS: `mvn test`
- PASS: `npm run build:h5`
- PASS: `mvn -pl eqochat-server -am -DskipTests package`
- PASS: backend started from `backend/eqochat-server/target/eqochat-server-1.0.0.jar` on `18080` with `--spring.profiles.active=local`
  - Flyway validated 26 migrations.
  - Schema `eqochat2` was current at v26.
- PASS: `BASE_URL=http://127.0.0.1:18080 scripts/smoke/actor-baseline-smoke.sh`
  - Included `PASS legacy users route retired`.

## Remaining Work

- Retire World human-shaped author surface such as `/api/v1/world/users/{authorId}/posts`.
- Rename Contact group member APIs and Java fields from user-shaped names to subject/member-shaped names.
- Decide whether WebSocket presence is human-session-only or should become active-subject presence.
