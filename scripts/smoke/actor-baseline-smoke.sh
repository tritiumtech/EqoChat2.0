#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
PHONE="${PHONE:-13900000001}"
PASSWORD="${PASSWORD:-Test1234}"
TARGET_SUBJECT_ID="${TARGET_SUBJECT_ID:-11}"
TARGET_SUBJECT_TYPE="${TARGET_SUBJECT_TYPE:-HUMAN}"
SMOKE_TAG="${SMOKE_TAG:-ACTOR_SMOKE}"

STATUS=""
BODY=""

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

require_cmd() {
  local cmd="$1"
  command -v "$cmd" >/dev/null 2>&1 || die "missing dependency: $cmd"
}

http_json() {
  local method="$1"
  local path="$2"
  local token="${3:-}"
  local payload="${4:-}"
  local url="${BASE_URL%/}${path}"
  local tmp
  local -a curl_args

  tmp="$(mktemp)"
  curl_args=(-sS -m 15 -X "$method" -H "Accept: application/json" -H "Content-Type: application/json" -o "$tmp" -w "%{http_code}")

  if [[ -n "$token" ]]; then
    curl_args+=(-H "Authorization: Bearer $token")
  fi

  if [[ -n "$payload" ]]; then
    curl_args+=(--data "$payload")
  fi

  if ! STATUS="$(curl "${curl_args[@]}" "$url")"; then
    BODY="$(<"$tmp")"
    rm -f "$tmp"
    printf 'FAIL request: %s %s\n' "$method" "$url" >&2
    [[ -n "$BODY" ]] && printf 'Response body:\n%s\n' "$BODY" >&2
    return 1
  fi

  BODY="$(<"$tmp")"
  rm -f "$tmp"
}

expect_api_success() {
  local label="$1"
  local path="$2"
  local code
  local message

  if [[ ! "$STATUS" =~ ^2[0-9][0-9]$ ]]; then
    printf 'FAIL %s: HTTP %s %s\n' "$label" "$STATUS" "$path" >&2
    [[ -n "$BODY" ]] && printf 'Response body:\n%s\n' "$BODY" >&2
    return 1
  fi

  if ! jq -e . >/dev/null 2>&1 <<<"$BODY"; then
    printf 'FAIL %s: response is not valid JSON: %s\n' "$label" "$path" >&2
    printf 'Response body:\n%s\n' "$BODY" >&2
    return 1
  fi

  code="$(jq -r 'if type == "object" and has("code") then .code else 200 end' <<<"$BODY")"
  if [[ "$code" != "200" ]]; then
    message="$(jq -r '.message // .msg // .error // "unknown error"' <<<"$BODY")"
    printf 'FAIL %s: API code=%s message=%s path=%s\n' "$label" "$code" "$message" "$path" >&2
    printf 'Response body:\n%s\n' "$BODY" >&2
    return 1
  fi

  printf 'PASS %-28s %s\n' "$label" "$path"
}

expect_jq() {
  local label="$1"
  local expr="$2"

  if ! jq -e "$expr" >/dev/null <<<"$BODY"; then
    printf 'FAIL %s: jq assertion failed: %s\n' "$label" "$expr" >&2
    printf 'Response body:\n%s\n' "$BODY" >&2
    return 1
  fi
}

check_get() {
  local label="$1"
  local path="$2"
  local token="$3"
  local expr="${4:-.code == 200}"

  http_json GET "$path" "$token"
  expect_api_success "$label" "$path"
  expect_jq "$label" "$expr"
}

require_cmd curl
require_cmd jq

printf 'Actor baseline smoke\n'
printf 'Base URL: %s\n' "$BASE_URL"
printf 'Login phone: %s\n' "$PHONE"

login_payload="$(jq -nc --arg phone "$PHONE" --arg password "$PASSWORD" '{phone: $phone, password: $password}')"
http_json POST "/api/v1/auth/login" "" "$login_payload"
expect_api_success "auth login" "/api/v1/auth/login"
expect_jq "auth login token" '.data.token | type == "string" and length > 20'

token="$(jq -er '.data.token' <<<"$BODY")" || die "login succeeded but token was not found in response"
principal_human_id="$(jq -er '.data.userInfo.id // .data.user.id // .data.id' <<<"$BODY")" || die "login succeeded but principal human id was not found in response"

check_get "auth me" "/api/v1/auth/me" "$token" '.data.id != null and .data.nickname != null'
check_get "agents me" "/api/v1/agents/me" "$token" '.data | type == "array" and length >= 1 and any(.[]; .ownerId != null and .walletRouting != null and .responsibilityChain != null)'
check_get "contacts list" "/api/v1/contacts" "$token" '.data | type == "array" and length >= 1 and any(.[]; .targetSubjectType == "HUMAN") and any(.[]; .targetSubjectType == "AGENT")'

check_get "projects list" "/api/v1/projects" "$token" '.data | type == "array" and length >= 1 and .[0].id != null and .[0].ownerSubjectId != null and (.[0].ownerSubjectType == "HUMAN" or .[0].ownerSubjectType == "AGENT") and .[0].walletRouting != null and .[0].responsibilityChain != null'
project_id="$(jq -er '.data[0].id' <<<"$BODY")"
check_get "project detail" "/api/v1/projects/${project_id}" "$token" '.data.id != null and .data.ownerSubjectId != null and (.data.ownerSubjectType == "HUMAN" or .data.ownerSubjectType == "AGENT") and (.data.members | type == "array") and any(.data.members[]; .memberSubjectId != null and (.memberSubjectType == "HUMAN" or .memberSubjectType == "AGENT"))'
check_get "project tasks" "/api/v1/projects/${project_id}/sidebar/tasks" "$token" '.data | type == "array" and all(.[]; .assigneeSubjectId != null and (.assigneeSubjectType == "HUMAN" or .assigneeSubjectType == "AGENT"))'
check_get "project payments" "/api/v1/projects/${project_id}/sidebar/payments" "$token" '.data | type == "array" and all(.[]; .recipientSubjectId != null and (.recipientSubjectType == "HUMAN" or .recipientSubjectType == "AGENT") and .walletRouting != null and .directRecipientSubjectId != null and (.directRecipientSubjectType == "HUMAN" or .directRecipientSubjectType == "AGENT") and .settlementSubjectId != null and (.settlementSubjectType == "HUMAN" or .settlementSubjectType == "AGENT") and (.financialAutonomy | type == "boolean") and .liableHumanId != null and .liabilityRoute != null)'
check_get "project files" "/api/v1/projects/${project_id}/sidebar/files" "$token" '.data | type == "array" and all(.[]; .uploaderSubjectId != null and (.uploaderSubjectType == "HUMAN" or .uploaderSubjectType == "AGENT"))'

check_get "world feed" "/api/v1/world/posts?limit=5" "$token" '.data.items | type == "array" and length >= 1 and any(.[]; .author.type == "agent")'
world_post_payload="$(jq -nc --arg content "${SMOKE_TAG} world post $(date +%s)" --argjson actorSubjectId "$principal_human_id" '{content: $content, mediaType: "TEXT", actorSubjectId: $actorSubjectId, actorSubjectType: "HUMAN"}')"
http_json POST "/api/v1/world/posts" "$token" "$world_post_payload"
expect_api_success "world create post" "/api/v1/world/posts"
expect_jq "world create post id" '.data.id != null and .data.author.type == "human"'
post_id="$(jq -er '.data.id' <<<"$BODY")"

reply_payload="$(jq -nc --arg content "${SMOKE_TAG} reply $(date +%s)" --argjson actorSubjectId "$principal_human_id" '{content: $content, actorSubjectId: $actorSubjectId, actorSubjectType: "HUMAN"}')"
http_json POST "/api/v1/world/posts/${post_id}/replies" "$token" "$reply_payload"
expect_api_success "world reply" "/api/v1/world/posts/${post_id}/replies"
expect_jq "world reply count" '.data.replyCount != null'

conversation_payload="$(jq -nc --argjson targetSubjectId "$TARGET_SUBJECT_ID" --arg targetSubjectType "$TARGET_SUBJECT_TYPE" '{targetSubjectId: $targetSubjectId, targetSubjectType: $targetSubjectType}')"
http_json POST "/api/v1/conversations" "$token" "$conversation_payload"
expect_api_success "conversation create" "/api/v1/conversations"
expect_jq "conversation id" '.data.id != null and .data.targetSubjectType != null'
conversation_id="$(jq -er '.data.id' <<<"$BODY")"

check_get "conversation list" "/api/v1/conversations" "$token" '.data | type == "array" and length >= 1'
check_get "conversation messages" "/api/v1/conversations/${conversation_id}/messages?limit=5" "$token" '.data.items | type == "array"'

message_payload="$(jq -nc --arg content "${SMOKE_TAG} message $(date +%s)" --argjson actorSubjectId "$principal_human_id" '{content: $content, messageType: "TEXT", actorSubjectId: $actorSubjectId, actorSubjectType: "HUMAN"}')"
http_json POST "/api/v1/conversations/${conversation_id}/messages" "$token" "$message_payload"
expect_api_success "message send" "/api/v1/conversations/${conversation_id}/messages"
expect_jq "message send id" '.data.id != null and .data.senderSubjectType == "HUMAN" and .data.liableHumanId != null'
message_id="$(jq -er '.data.id' <<<"$BODY")"

read_payload="$(jq -nc --argjson messageId "$message_id" --argjson readerSubjectId "$principal_human_id" '{messageId: $messageId, readerSubjectId: $readerSubjectId, readerSubjectType: "HUMAN"}')"
http_json POST "/api/v1/conversations/${conversation_id}/read" "$token" "$read_payload"
expect_api_success "message read" "/api/v1/conversations/${conversation_id}/read"

printf 'Actor baseline smoke passed.\n'
