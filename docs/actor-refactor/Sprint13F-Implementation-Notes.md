# Sprint 13F Implementation Notes: Contact, Notification, FriendRequest Explicit Subject Cleanup

Sprint 13F removes implicit principal-human subject fallbacks from Contact, Notification, and FriendRequest inbox flows.

## Scope

- `ContactController.resolveOwner` no longer defaults missing owner identity to `SubjectRef.human(principalHumanId)`.
- `NotificationController.resolveRecipient` no longer defaults missing recipient identity to `SubjectRef.human(principalHumanId)`.
- `MarkNotificationReadRequest.recipientSubjectId` and `recipientSubjectType` are now required.
- `FriendRequestController` received/sent inbox endpoints now require explicit recipient/requester subject identity.
- Removed `FriendRequestService.listReceived(Long)` and `listSent(Long)` overloads.
- `FriendRequestServiceImpl` now queries only the explicit authorized inbox subject; principal subject aggregation was removed.
- Frontend friend-request APIs and active-subject helpers now require subject params for received/sent inbox calls.
- Replaced null subject-type defaults with fail-fast validation in Contact/FriendRequest service code.
- Removed id-only human-default Credit audit mapper overloads.
- Added controller/service tests across Contact, Notification, FriendRequest, and Credit touched paths.
- Added static guards for:
  - Contact controller implicit principal `HUMAN` owner fallback.
  - Notification controller implicit principal `HUMAN` recipient fallback.
  - FriendRequest inbox legacy implicit principal subject aggregate.
  - Contact/FriendRequest null subject type defaults to `HUMAN`.
  - Credit audit id-only `HUMAN` actor overload.

## Boundary Decision

Inbox, contact owner, notification recipient, and audit actor identity must be explicit subject identity. The authenticated human remains an authorization anchor, not a default business subject.

## Verification

- PASS: `mvn -pl eqochat-business/eqochat-contact-parent/eqochat-contact -am test`
- PASS: `mvn -pl eqochat-business/eqochat-contact-parent/eqochat-contact,eqochat-business/eqochat-notification-parent/eqochat-notification -am test`
- PASS: `mvn -pl eqochat-business/eqochat-contact-parent/eqochat-contact,eqochat-business/eqochat-credit-parent/eqochat-credit -am test`
- PASS: `bash scripts/tests/actor-static-guard-test.sh`
- PASS: `bash scripts/actor-static-guard.sh backend frontend`
