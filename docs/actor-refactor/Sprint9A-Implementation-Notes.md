# Sprint 9A Implementation Notes: Subject Registry Read Model

## Scope

Sprint 9A introduces `subject_registry` as the canonical read-model index for Human and Agent public identity.

The registry is intentionally a read model, not a replacement for source ownership tables yet:

- `user_profile` and `agent_profile` remain the write/source tables.
- `SubjectDirectoryApi` now prefers `subject_registry`.
- Source profile fallback remains in place so partially migrated environments do not fail closed.
- Search prefers registry and preserves profile-based fallback during migration.

## Backend

- Added `V21__subject_registry_read_model.sql`.
  - Creates `subject_registry`.
  - Backfills HUMAN subjects from `user_profile`.
  - Backfills AGENT subjects from `agent_profile`.
  - Freezes registry facts such as display name, status, points, credit score, associated human summary, source table, and source id.
  - Uses `(subject_id, subject_type)` as the canonical uniqueness boundary, so `HUMAN:101` and `AGENT:101` cannot collide.

- Added `SubjectRegistryRepository`.
  - Reads registry records by `SubjectRef`.
  - Searches registry by numeric id, DID, phone, email, display name, and search text.
  - Best-effort upserts registry rows after source fallback reads.
  - Treats registry failures as read-model cache misses; source profile lookup remains authoritative fallback.

- Updated `SubjectDirectoryServiceImpl`.
  - `getSubject(HUMAN/AGENT)` now checks registry first.
  - Credit, points, capability, and liability facts remain policy-derived at read time.
  - Registry miss still reads `user_profile` / `agent_profile` and refreshes registry.
  - `SYSTEM` behavior remains unchanged and does not use registry.

- Updated `SubjectProfileServiceImpl`.
  - `/api/v1/subjects/search` now tries registry search before legacy mapper searches.
  - Numeric search still returns both HUMAN and AGENT subjects when both exist.
  - Public profile continues to resolve through `SubjectDirectoryApi`.

## Tests

Added `SubjectDirectoryServiceImplRegistryContractTest` covering:

- registry hit builds an Agent summary without requiring mirror `user_profile`;
- registry miss falls back to source profile and refreshes registry;
- `HUMAN:{id}` and `AGENT:{id}` remain isolated when numeric ids match.

Updated `SubjectProfileServiceImplActorContractTest` for the registry dependency.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-actor-parent/eqochat-actor -am '-Dtest=SubjectDirectoryServiceImplRegistryContractTest,SubjectProfileServiceImplActorContractTest,SubjectControllerPublicProfileTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
  - Result: `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`.

## Remaining Work

- Sprint 9B should remove long-term runtime dependency on Agent mirror `user_profile` rows in World/Contact/social graph surfaces.
- Profile create/update flows should eventually dual-write or event-publish into `subject_registry`; Sprint 9A provides fallback upsert on reads but not a full event pipeline.
- Full Flyway startup should be kept in the integration gate because V21 is a real schema migration.
