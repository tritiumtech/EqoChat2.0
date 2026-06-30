# Sprint 3C Implementation Notes: Notification Subject Cut

## Scope

- Notification list, unread, count, mark-read, and mark-all-read mapper queries now scope recipients by `recipient_id + recipient_type`.
- `NotificationService` positive-path APIs use `SubjectRef` for recipient and sender identity.
- The old id-only `sendNotification(Long recipientId, ..., Long senderId)` API was removed.
- Notification senders are explicit `SubjectRef`s. `SYSTEM` notifications use `SubjectRef.system(0L)` rather than null-sender heuristics.
- Notification responses expose canonical `recipientSubjectId/recipientSubjectType` and `senderSubjectId/senderSubjectType`.
- Friend request notification call sites now pass `SubjectRef.human(...)`.
- World mention notification fanout now calls `NotificationService` with canonical recipient/sender subjects.
- World mention validation rejects `SYSTEM` targets with `world.mention.subject_type.invalid`; `USER` remains forbidden by canonical `SubjectType`.
- World depends on the Notification API boundary for mention fanout instead of the Notification implementation/entity layer.

## Migration

- `V18__notification_subject_identity_indexes.sql`
  - Normalizes historical blank/`USER` notification subject types.
  - Normalizes historical system senders to `sender_id=0 + sender_type=SYSTEM`.
  - Tightens notification subject comments/defaults.
  - Adds recipient-subject and unread-subject indexes for runtime queries.

## Compatibility Position

No runtime Notification API accepts id-only cross-subject identity for notification creation, listing, unread, or mark-read. `USER` appears only in migration and historical documentation.
