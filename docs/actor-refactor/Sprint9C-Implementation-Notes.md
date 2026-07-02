# Sprint 9C Implementation Notes: Credit Profile Consolidation

## Scope

Sprint 9C makes `subject_credit_profile` the canonical credit profile read source for actor-facing credit responses.

## Backend

- Replaced direct profile-table credit reads with `SubjectCreditProfileMapper`.
  - Credit module no longer reads `user_profile.credit_score` or `agent_profile.credit_score`.
  - Existing `credit_record.current_score` remains a fallback.
  - Final fallback is PRD minimum score `300`.

- Added `V23__subject_credit_profile_consolidation.sql`.
  - Normalizes legacy `subject_type='USER'` rows to `HUMAN`.
  - Backfills/upserts `subject_credit_profile`.
  - Updates `subject_registry.credit_score` and `credit_rating` from consolidated credit facts.

## Tests

- Replaced `CreditProfileServiceImplActorContractTest`.
- Tests verify:
  - canonical subject type parsing;
  - PRD-scale `300..850` score adaptation;
  - no SQL dependency on `user_profile` or `agent_profile` in the credit mapper.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-credit-parent/eqochat-credit -am '-Dtest=CreditProfileServiceImplActorContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`

## Notes

- `user_profile.credit_score` and `agent_profile.credit_score` remain historical/source fields while migration backfills consolidated actor credit state.
