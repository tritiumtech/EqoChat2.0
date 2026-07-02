# Sprint 12H Implementation Notes: Contact Storage Ownership

Sprint 12H moves Contact relationship/tag storage from legacy user-named tables to Contact-owned physical names.

## Scope

- Remapped `ContactRelationship` from `user_friend` to `contact_relationship`.
- Renamed `UserContactTag` to `ContactTag`.
- Renamed `UserContactTagMapper` to `ContactTagMapper`.
- Remapped tag storage from `user_contact_tag` to `contact_tag`.
- Updated Contact mapper SQL and contract tests to require `contact_relationship` and `contact_tag`.
- Added Flyway `V26__contact_storage_ownership.sql`:
  - renames `user_friend` to `contact_relationship`,
  - renames `user_contact_tag` to `contact_tag`,
  - renames the subject-aware indexes to Contact-owned names,
  - recreates `v_user_full` against `contact_relationship`,
  - leaves compatibility views named `user_friend` and `user_contact_tag` for legacy read paths.
- Added a static guard rule blocking Contact runtime code from reintroducing legacy contact storage table names or `UserContactTag` Java types.

## Boundary Decision

The Contact module now owns both the Java model and the physical storage names for relationships and tags. Legacy table names exist only as database compatibility views after V26; new runtime code must use the Contact-owned names.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-contact-parent/eqochat-contact -am test`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`

## Remaining Work

- Legacy `/api/v1/users` public compatibility surface still needs an explicit retire/deprecate sprint.
- Actor registry source-table fallback remains a controlled migration bridge.
