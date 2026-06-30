# Actor Refactor Contract

This is the canonical contract for the Actor Model refactor. It follows `backend/ACTOR-MODEL-ARCHITECTURE.md` and freezes the rules that Generator must obey and Evaluator must enforce.

## Core Identity Contract

- `SubjectRef(id, type)` is the minimum cross-module identity unit.
- `SubjectType` canonical values are `HUMAN`, `AGENT`, and `SYSTEM`.
- `USER` is a historical-storage cleanup concern only. Normal APIs, WebSocket payloads, domain services, and write paths must reject or avoid `USER` rather than adapt it for compatibility.
- This codebase is pre-consumer. Do not preserve legacy request fields, WebSocket fields, enum values, or DTO aliases for backward compatibility when they conflict with the Actor Model vocabulary.
- Keep `user_profile + agent_profile + agent_binding` for the near-term storage model. Do not perform a big-bang merge into a single users table.

## Three IDs

- `principalHumanId`: the authenticated human session owner.
- `actorSubject`: the subject performing the business action, represented by `SubjectRef`.
- `liableHumanId`: the human who is legally/business accountable for the action.

No module may collapse these three concepts into a single `userId` when writing cross-subject business records.

## Hard Rules

- New cross-subject fields must be stored as `{role}_id + {role}_type`.
- Do not add `is_agent` as an authoritative identity field.
- Do not add new `senderType("USER")`, `RecipientType.USER`, `ReaderType.USER`, `ParticipantType.USER`, or `SubjectType.USER` usage.
- Subject display should go through `SubjectDirectoryApi`.
- Wallet routing should go through `WalletPolicyApi`.
- Liability should go through `LiabilityPolicyApi`.
- Behavior permission should converge on capability or authorization policy, not scattered controller checks.
- Environment-specific configuration must use Spring Boot profiles or environment variables. Do not hardcode server IP, database strings, Redis/Neo4j URLs, JWT secrets, or local personal paths.

## Module Requirements

### Chat

- Participants, senders, read receipts, unread counters, and WebSocket events must use subject id and type.
- WebSocket base messages must not expose overloaded `userId`/`recipientId` style fields; routing data belongs in canonical subject fields or typed payload fields such as `conversationId`.
- Human and Agent message senders must render through the same subject directory path.
- Agent messages must be auditable back to the liable human.

### Project

- Owner, member, assignee, uploader, payment recipient, and transfer target must use subject id and type.
- Payment records must persist wallet routing and liability facts at creation time.
- Agent owner projects must preserve associated human transparency without making the human the displayed project owner.

### World

- Post and reply authors must use author id and type.
- Mentions, follows, upvotes, replies, and notifications must stop depending on mirror user rows as the long-term source of identity.

### Contact

- Contact relations and friend requests must support human and agent subjects without requiring an agent mirror user profile.
- Owner/associated human is transparency metadata, not identity.

### Credit

- Credit and violation subjects use `HUMAN/AGENT/SYSTEM`.
- Credit score output follows the PRD `300-850` scale.
- Agent violations must be able to notify or affect the liable human according to policy.

### Notification

- Recipient and sender must use subject id and type.
- Unread counts and mark-read must not collide when a human and agent share numeric ids.

## Required Gates

- `scripts/smoke/actor-baseline-smoke.sh`
- `ALLOW_EXISTING=1 scripts/actor-static-guard.sh` for Sprint 0
- `scripts/actor-static-guard.sh` when the touched modules have cleared their legacy P0s
- Backend package build for every Sprint
- Module-specific tests for the Sprint under work

Any regression in login, agents, contacts, chat, world, project list/detail, or Flyway startup blocks the Sprint.
