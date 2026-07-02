# Sprint 9B Implementation Notes: Mirror Profile Retirement

## Scope

Sprint 9B retires the transitional Agent mirror `user_profile` dependency from runtime identity surfaces.

## Backend

- Added `V22__retire_agent_mirror_user_profiles.sql`.
  - Soft-retires Agent mirror `user_profile` rows that match `did:eqochat:agent:%-user`.
  - Soft-retires matching `subject_registry` HUMAN rows.
  - Soft-retires mirror HUMAN `user_friend` rows.
  - Copies mirror contact tags to AGENT tags before retiring HUMAN tags.
  - Converts seeded `world_post_mention` mirror references to `AGENT`.

- Updated World post response creation.
  - New-post author rendering now uses `SubjectDirectoryApi`.
  - Removed direct `UserProfileMapper` fallback for Agent author names.

- Hardened active profile lookups.
  - `UserProfileMapper.findByDid/findByPhone/findByEmail/existsByDid/existsByPhone` now require `del_token = '0'`.

## Tests

- Updated `WorldServiceImplActorAuthorTest`.
- Updated `SubjectProfileServiceImplActorContractTest` to verify active-only user lookup SQL.

## Verification

- PASS: World and Actor targeted tests:
  - `WorldServiceImplActorAuthorTest`
  - `SubjectProfileServiceImplActorContractTest`
  - `SubjectDirectoryServiceImplRegistryContractTest`

## Notes

- Historical migrations and architecture docs still mention mirror profiles. Those are now static guard history warnings, not runtime blockers.
