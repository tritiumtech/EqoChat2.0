# Sprint 12F Implementation Notes: Contact Group Subject Boundary

Sprint 12F moves Contact group owner/member identity from human-shaped Java and SQL queries to subject-aware identity.

## Scope

- Added `GroupProfile.ownerType` mapped to `group_profile.owner_type`.
- Added `GroupMember.memberType` mapped to `group_member.member_type`.
- Kept physical legacy id columns (`owner_id`, `user_id`) to avoid a risky rename in the same sprint.
- Updated `GroupProfileMapper` with subject-aware owner queries:
  - `findByOwner(Long ownerId, SubjectType ownerType)`
  - `findByOwner(SubjectRef owner)`
  - legacy `findByOwnerId(Long)` now delegates to `HUMAN`.
- Updated `GroupMemberMapper` with subject-aware member queries:
  - `findByMember(Long memberId, SubjectType memberType)`
  - `findByMember(SubjectRef member)`
  - `findByGroupAndMember(...)`
  - `isMember(...)`
  - legacy `findByUserId`, `findByGroupAndUser`, and `isMember(groupId, userId)` now delegate to `HUMAN`.
- Added Flyway `V24__contact_group_subject_identity.sql`:
  - backfills historical group owners/members to `HUMAN`,
  - drops hard FK coupling from group owner/member ids to `user_profile`,
  - adds subject-aware indexes and unique key.
- Added `ContactGroupSubjectContractTest`.
- Added a static guard rule blocking Contact group mapper SQL from regressing to direct human-only owner/member queries.

## Boundary Decision

Group storage keeps historical id column names for now, but all new mapper APIs must carry a `SubjectType` dimension. Existing human-only methods are compatibility wrappers, not the canonical API.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-contact-parent/eqochat-contact -am test`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`

## Remaining Work

- Physical Contact relationship/tag storage still uses `user_friend` and `user_contact_tag` table names.
- Credit audit actor fields still need subject-aware operator/reporter/reviewer semantics.
- Actor registry source-table fallback remains a controlled migration bridge.
