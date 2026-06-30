# Sprint 3B Implementation Notes: World Engagement Subject Cut

## Scope

- Contact detail World stats now call `countByAuthor(contactId, friendType.name())`, so agent contacts do not inherit human post counts when numeric ids overlap.
- World create-post mentions now accept `mentionedSubjects: [{ subjectId, subjectType }]`.
- Runtime World mention writes use `mentioned_subject_id + mentioned_subject_type`.
- Mention feed reads the current viewer as `HUMAN` through `mentioned_subject_id + mentioned_subject_type`.
- Post upvotes use `voter_id + voter_type`; feed `is_upvoted` scopes the current viewer as `HUMAN`.
- Reply upvotes use `voter_id + voter_type`; reply liked state and toggle paths scope the current viewer as `HUMAN`.
- Topic follows use `follower_id + follower_type`; topic favorite flags and topic-priority feed sorting scope the current viewer as `HUMAN`.

## Migration

- `V17__world_engagement_subject_identity.sql`
  - Adds canonical subject columns to `world_post_mention`, `world_post_upvote`, `world_post_reply_upvote`, and `world_topic_follow`.
  - Backfills old human-only columns into canonical subject columns with `HUMAN`.
  - Makes old columns nullable and marks them historical/backfill-only.
  - Replaces old id-only unique keys with subject-aware unique keys.

## Compatibility Position

The old `mentionedUserIds` DTO field is removed from the positive create-post path. Old DB columns remain only for migration/backfill history; runtime engagement logic reads and writes canonical subject columns.

## Gates

- `mvn -pl eqochat-business/eqochat-world-parent/eqochat-world -am test`: PASS.
- `mvn -pl eqochat-business/eqochat-contact-parent/eqochat-contact -am test`: PASS.
- `mvn -pl eqochat-server -am -DskipTests package`: PASS.
- `npm run build:h5`: PASS.
- `git diff --check`: PASS.
