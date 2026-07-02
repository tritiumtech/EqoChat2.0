# Sprint 7C Implementation Notes: Notification Recipient Subject Runtime Surface

Date: 2026-07-01

## Scope

Sprint 7C makes Notification inbox and read endpoints accept an explicit recipient subject while preserving the authenticated human session as the authorization boundary.

- `principalHumanId` remains the logged-in human from `UserContext`.
- `recipientSubjectId + recipientSubjectType` selects which notification inbox is read or marked.
- Missing recipient parameters default to `SubjectRef.human(principalHumanId)`.
- Agent recipient access is authorized by `LiabilityPolicyApi`; an Agent recipient must resolve to the current principal human.

## Non-Scope

- No Notification storage migration.
- No push/WebSocket session actorization.
- No Credit violation notification workflow.
- No Agent switcher UI.
- No new Subject Registry.
- No `USER` compatibility alias.

## Key Changes

- `GET /api/v1/notifications` accepts optional `recipientSubjectId` and `recipientSubjectType`.
- `POST /api/v1/notifications/read` accepts optional recipient subject fields in `MarkNotificationReadRequest`.
- Partial recipient parameters and `SYSTEM` recipients are rejected with `notification.recipient.type.invalid`.
- Unauthorized Agent recipients are rejected with `notification.access.denied`.
- `NotificationController` keeps principal human and recipient subject separate, and delegates storage scoping to `NotificationService`.
- Frontend `notificationApi.list` and `notificationApi.markRead` accept optional recipient subject parameters.
- The Python actor baseline smoke now checks explicit Agent recipient notification queries.

## Tests

Added `NotificationControllerRecipientSubjectTest` covering:

- default recipient fallback to the principal human;
- explicit Agent recipient list routing when liability resolves to the principal human;
- explicit Agent recipient read routing when liability resolves to the principal human;
- unauthorized Agent recipient rejection before service calls;
- partial and `SYSTEM` recipient parameter rejection before policy/service calls.

Existing `NotificationServiceImplActorContractTest` continues to cover:

- recipient SQL scoping by `recipient_id + recipient_type`;
- read updates scoped by recipient subject pair;
- canonical response fields for recipient and sender subjects;
- explicit recipient and sender persistence for send paths.

## Verification Checklist

- PASS: `cd backend && mvn -pl eqochat-business/eqochat-notification-parent/eqochat-notification -am test`
  - Notification implementation module result: `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`.
- PASS: `cd backend && mvn -pl eqochat-server -am '-DskipTests' package`.
- PASS: `cd frontend && npm run build:h5`.
- PASS: `bash scripts/tests/actor-static-guard-test.sh`.
- PASS: `bash scripts/actor-static-guard.sh`.
  - Result: `P0=0 WARN=67`, files scanned `1028`.
- PASS: `git diff --check`.
  - Output only showed Git CRLF normalization warnings; no whitespace errors were reported.
- PASS: latest backend jar started with `--spring.profiles.active=local`.
  - Flyway validated `20` migrations and reported schema version `20` up to date.
- PASS: `bash scripts/smoke/actor-baseline-smoke.sh`.
  - Runtime smoke included `notifications agent recipient`.
- PASS: `bash scripts/smoke-actor-baseline.sh`.
  - Compatibility entry point also included `notifications agent recipient`.
- PASS: Evaluator returned explicit `PASS`.

## Known Residuals

- Notification APIs are still human-session APIs. They allow selecting a recipient subject, but they do not authenticate as that subject.
- Runtime smoke does not require a non-empty Agent recipient inbox because local seed data may not include Agent-recipient notifications.
- WebSocket/push notification delivery still follows the existing session model and is intentionally outside this sprint.
