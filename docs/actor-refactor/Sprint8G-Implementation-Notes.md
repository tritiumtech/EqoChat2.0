# Sprint 8G Implementation Notes - Project Write Runtime Smoke

## Scope

Sprint 8G closes the runtime smoke gap left by Sprint 8E.

Project business writes already require explicit `actorSubjectId/actorSubjectType`. This sprint exercises the highest-risk Project writes through the running HTTP API instead of only through unit tests.

## Changes

- `scripts/smoke/actor-baseline-smoke.py`
  - Creates an Agent-owned project through the active Agent subject.
  - Creates a Project payment with explicit Agent actor.
  - Verifies frozen payment wallet facts: `AGENT_DIRECT`, direct recipient Agent, settlement Agent, enabled wallet policy, and liable human.
  - Verifies the payment is visible through the Agent viewer sidebar.
  - Verifies ownership transfer rejects a non-member target on a newly created Agent-owned project.
  - Uses the seeded multi-member Agent-owned project as a recoverable transfer fixture.
  - Transfers ownership from Agent to Human, verifies the old Agent actor can no longer mutate owner-only Project state, verifies the new Human owner can update the bid, then restores ownership back to Agent.
- `scripts/smoke/README.md`
  - Documents the Sprint 8G Project write smoke coverage.

## Deferred

- Subject-specific WebSocket/push delivery remains Sprint 8H.
- Subject registry/read-model consolidation remains Sprint 9A.
- Mirror profile retirement remains Sprint 9B.

## Verification

- `mvn -pl eqochat-business/eqochat-project-parent/eqochat-project -am test` passed during implementation.
- `python -m py_compile scripts/smoke/actor-baseline-smoke.py` passed.
- `bash scripts/smoke/actor-baseline-smoke.sh` passed.
- `bash scripts/smoke-actor-baseline.sh` passed.
