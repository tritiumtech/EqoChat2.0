#!/usr/bin/env bash
set -euo pipefail

ALLOW_EXISTING="${ALLOW_EXISTING:-0}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

normalize_tool_path() {
  local raw="$1"
  if [[ "$raw" =~ ^([A-Za-z]):\\(.*)$ ]]; then
    local drive="${BASH_REMATCH[1],,}"
    local rest="${BASH_REMATCH[2]//\\//}"
    if [[ -d "/mnt/$drive" ]]; then
      printf '/mnt/%s/%s\n' "$drive" "$rest"
    else
      printf '/%s/%s\n' "$drive" "$rest"
    fi
  else
    printf '%s\n' "$raw"
  fi
}

find_powershell_rg() {
  command -v powershell.exe >/dev/null 2>&1 || return 1
  local found
  found="$(powershell.exe -NoProfile -Command "(Get-Command rg -ErrorAction Stop).Source" 2>/dev/null | tr -d '\r' || true)"
  [[ -n "$found" ]] || return 1
  normalize_tool_path "$found"
}

RG_BIN_FROM_ENV=0
if [[ -n "${RG_BIN:-}" ]]; then
  RG_BIN_FROM_ENV=1
  RG_BIN="$(normalize_tool_path "$RG_BIN")"
else
  RG_BIN="$(command -v rg || true)"
fi
[[ -n "$RG_BIN" ]] || die "missing dependency: rg"

declare -a SCAN_PATHS=("$@")
if [[ ${#SCAN_PATHS[@]} -eq 0 ]]; then
  [[ -e backend ]] && SCAN_PATHS+=(backend)
  [[ -e frontend ]] && SCAN_PATHS+=(frontend)
fi

[[ ${#SCAN_PATHS[@]} -gt 0 ]] || die "no scan paths found"

declare -a RG_IGNORE=(
  --glob '!**/.git/**'
  --glob '!**/target/**'
  --glob '!**/node_modules/**'
  --glob '!**/dist/**'
  --glob '!**/build/**'
  --glob '!**/.output/**'
)

for path in "${SCAN_PATHS[@]}"; do
  [[ -e "$path" ]] || die "scan path not found: $path"
  [[ -r "$path" ]] || die "scan path is not readable: $path"
done

if rg_version_output="$("$RG_BIN" --version 2>&1)"; then
  status=0
else
  status=$?
  if [[ "$RG_BIN_FROM_ENV" -eq 0 ]]; then
    fallback_rg="$(find_powershell_rg || true)"
    if [[ -n "$fallback_rg" && "$fallback_rg" != "$RG_BIN" ]]; then
      RG_BIN="$fallback_rg"
      if rg_version_output="$("$RG_BIN" --version 2>&1)"; then
        status=0
      else
        status=$?
      fi
    fi
  fi
  if [[ "$status" -ne 0 ]]; then
    printf 'ERROR: rg failed to execute: %s\n' "$RG_BIN" >&2
    printf 'Exit status: %s\n' "$status" >&2
    printf 'rg stderr/stdout:\n%s\n' "$rg_version_output" >&2
    exit 1
  fi
fi
rg_version_line="$(printf '%s\n' "$rg_version_output" | sed -n '1p')"

scan_files() {
  local output
  local status
  local stderr_file
  local stderr_output
  local count

  stderr_file="$(mktemp)" || die "failed to create temporary stderr file for rg --files"
  if output="$("$RG_BIN" --files --hidden "${RG_IGNORE[@]}" "${SCAN_PATHS[@]}" 2>"$stderr_file")"; then
    status=0
  else
    status=$?
  fi
  stderr_output="$(cat "$stderr_file")"
  rm -f "$stderr_file"

  if [[ "$status" -eq 0 ]]; then
    count="$(count_lines "$output")"
    if [[ "$count" -eq 0 ]]; then
      die "scan paths resolved but no files were scanned"
    fi
    printf '%s\n' "$count"
  else
    printf 'ERROR: rg failed while listing scan files\n' >&2
    printf 'Exit status: %s\n' "$status" >&2
    if [[ -n "$stderr_output" ]]; then
      printf 'rg stderr:\n%s\n' "$stderr_output" >&2
    else
      printf 'rg stderr: <empty>\n' >&2
    fi
    exit 1
  fi
}

declare -a P0_LABELS=(
  'hard-coded senderType("USER")'
  'hard-coded senderType: "USER"'
  'SenderType.USER enum usage'
  'RecipientType.USER enum usage'
  'SubjectType.USER enum usage'
  'ReaderType.USER enum usage'
  'readerType(USER) hard-code'
  'ParticipantType.USER enum usage'
)

declare -a P0_PATTERNS=(
  'senderType\s*\(\s*['"'"'"]USER['"'"'"]\s*\)'
  'senderType\s*:\s*['"'"'"]USER['"'"'"]'
  '\bSenderType\.USER\b'
  '\bRecipientType\.USER\b'
  '\bSubjectType\.USER\b'
  '\bReaderType\.USER\b'
  'readerType\s*\(\s*(['"'"'"]USER['"'"'"]|USER|[A-Za-z0-9_.]*ReaderType\.USER)\s*\)'
  '\bParticipantType\.USER\b'
)

declare -a WARN_LABELS=(
  'USER/AGENT schema or code comments'
  'subject_type USER/AGENT transition text'
  'sender_type USER/AGENT transition text'
  'recipient_type USER/AGENT transition text'
  'mirror user transition dependency'
  'is_agent or author_ai transition field'
)

declare -a WARN_PATTERNS=(
  '(//|/\*|\*|--|COMMENT).*USER/AGENT'
  'subject_?type[^[:cntrl:]]*(USER/AGENT|USER)'
  'sender_?type[^[:cntrl:]]*(USER/AGENT|USER)'
  'recipient_?type[^[:cntrl:]]*(USER/AGENT|USER)'
  '(mirror user|mirror profile|mirror user profile)'
  '\b(is_agent|author_ai)\b'
)

p0_count=0
warn_count=0
warn_runtime_count=0
warn_history_count=0

count_lines() {
  local text="$1"
  printf '%s\n' "$text" | sed '/^$/d' | wc -l | tr -d ' '
}

is_history_warn_match() {
  local line="$1"
  local file="${line%%:*}"
  case "$file" in
    *.md|*.MD)
      return 0
      ;;
    *\\src\\main\\resources\\db\\migration\\*|*/src/main/resources/db/migration/*)
      return 0
      ;;
  esac
  return 1
}

classify_warn_output() {
  local output="$1"
  local line
  WARN_RUNTIME_OUTPUT=""
  WARN_HISTORY_OUTPUT=""
  while IFS= read -r line; do
    [[ -n "$line" ]] || continue
    if is_history_warn_match "$line"; then
      WARN_HISTORY_OUTPUT+="$line"$'\n'
    else
      WARN_RUNTIME_OUTPUT+="$line"$'\n'
    fi
  done <<< "$output"
}

scan_rule() {
  local severity="$1"
  local label="$2"
  local pattern="$3"
  shift 3
  local -a extra_rg_args=("$@")
  local output
  local count
  local status
  local stderr_file
  local stderr_output

  stderr_file="$(mktemp)" || die "failed to create temporary stderr file for rg"
  if output="$("$RG_BIN" -n --color never --hidden "${RG_IGNORE[@]}" "${extra_rg_args[@]}" -e "$pattern" "${SCAN_PATHS[@]}" 2>"$stderr_file")"; then
    status=0
  else
    status=$?
  fi
  stderr_output="$(cat "$stderr_file")"
  rm -f "$stderr_file"

  if [[ "$status" -eq 0 ]]; then
    count="$(count_lines "$output")"
    if [[ "$severity" == "P0" ]]; then
      printf '\n[%s] %s (%s)\n' "$severity" "$label" "$count"
      printf '%s\n' "$output"
      p0_count=$((p0_count + count))
    else
      classify_warn_output "$output"
      local runtime_count
      local history_count
      runtime_count="$(count_lines "$WARN_RUNTIME_OUTPUT")"
      history_count="$(count_lines "$WARN_HISTORY_OUTPUT")"
      printf '\n[WARN_RUNTIME] %s (%s)\n' "$label" "$runtime_count"
      if [[ "$runtime_count" -gt 0 ]]; then
        printf '%s\n' "$WARN_RUNTIME_OUTPUT"
      fi
      printf '\n[WARN_HISTORY] %s (%s)\n' "$label" "$history_count"
      if [[ "$history_count" -gt 0 ]]; then
        printf '%s\n' "$WARN_HISTORY_OUTPUT"
      fi
      warn_count=$((warn_count + count))
      warn_runtime_count=$((warn_runtime_count + runtime_count))
      warn_history_count=$((warn_history_count + history_count))
    fi
  elif [[ "$status" -eq 1 ]]; then
    return 0
  else
    printf '\nERROR: rg failed while scanning rule [%s] %s\n' "$severity" "$label" >&2
    printf 'Pattern: %s\n' "$pattern" >&2
    printf 'Exit status: %s\n' "$status" >&2
    if [[ -n "$stderr_output" ]]; then
      printf 'rg stderr:\n%s\n' "$stderr_output" >&2
    else
      printf 'rg stderr: <empty>\n' >&2
    fi
    exit 1
  fi
}

printf 'Actor static guard\n'
printf 'RG binary: %s\n' "$RG_BIN"
printf 'RG version: %s\n' "$rg_version_line"
printf 'Scan paths: %s\n' "${SCAN_PATHS[*]}"
files_scanned="$(scan_files)" || exit 1
printf 'Files scanned: %s\n' "$files_scanned"
printf 'ALLOW_EXISTING=%s\n' "$ALLOW_EXISTING"

for index in "${!P0_LABELS[@]}"; do
  scan_rule P0 "${P0_LABELS[$index]}" "${P0_PATTERNS[$index]}"
done

scan_rule P0 \
  'frontend legacy /api/v1/users client usage' \
  '/api/v1/users' \
  --glob '**/frontend/src/**'

scan_rule P0 \
  'backend legacy /api/v1/users public route' \
  '/api/v1/users' \
  --glob '**/eqochat-user/src/main/java/**/*.java'

scan_rule P0 \
  'legacy UserServiceImpl direct human-profile implementation dependency' \
  '\b(UserProfileService|UserFriendMapper|WorldPostStatsApi|countByAuthorId)\b' \
  --glob '**/eqochat-user/src/main/java/**/UserServiceImpl.java'

scan_rule P0 \
  'User module legacy friend/follow storage ownership' \
  '\b(UserFriend|UserFriendMapper|UserFollow|UserFollowMapper|user_friend|user_follow)\b' \
  --glob '**/eqochat-user/src/main/java/**/*.java'

scan_rule P0 \
  'User module Neo4j social graph runtime ownership' \
  'spring-boot-starter-data-neo4j|org\.springframework\.data\.neo4j|@Node\(|@Relationship(Properties)?|com\.eqochat\.business\.user\.entity\.neo4j|\bclass[[:space:]]+(SocialUser|SocialAgent|SocialGroup|FriendWith|Follows|Owns|MemberOf|InteractsWith|RelatedTo|SimilarTo)\b' \
  --glob '**/eqochat-user/pom.xml' \
  --glob '**/eqochat-user/src/main/java/**/*.java'

scan_rule P0 \
  'Server direct Neo4j runtime dependency or config' \
  'spring-boot-starter-data-neo4j|^[[:space:]]*neo4j:' \
  --glob '**/eqochat-server/pom.xml' \
  --glob '**/eqochat-server/src/main/resources/application*.yml' \
  --glob '**/eqochat-server/src/main/resources/application*.yaml'

scan_rule P0 \
  'WorldPostMapper source-profile author identity fallback join' \
  'LEFT JOIN (user_profile|agent_profile)|\b(ap\.owner_id|owner\.nickname)\b|COALESCE\(sr\.display_name, (ap|u)\.' \
  --glob '**/eqochat-world/src/main/java/**/WorldPostMapper.java'

scan_rule P0 \
  'backend legacy World human author route' \
  '/users/\{authorId\}/posts|/api/v1/world/users' \
  --glob '**/eqochat-world/src/main/java/**/*.java'

scan_rule P0 \
  'frontend legacy World human author client usage' \
  '/api/v1/world/users' \
  --glob '**/frontend/src/**'

scan_rule P0 \
  'World human-only author stats API' \
  '\bcountByAuthorId\b' \
  --glob '**/eqochat-world-api/src/main/java/**/*.java' \
  --glob '**/eqochat-world/src/main/java/**/*.java'

scan_rule P0 \
  'World service legacy human-default viewer overload' \
  'list(Feed|Topics|TopicPosts|MentionedMe|Replies|MyPosts)\(Long viewerId|SubjectRef\.human\(viewerId\)' \
  --glob '**/eqochat-world/src/main/java/**/WorldService.java' \
  --glob '**/eqochat-world/src/main/java/**/WorldServiceImpl.java'

scan_rule P0 \
  'World controller implicit principal HUMAN viewer fallback' \
  'SubjectRef\.human\(principalHumanId\)' \
  --glob '**/eqochat-world/src/main/java/**/WorldController.java'

scan_rule P0 \
  'Chat presence hard-coded HUMAN subject payload' \
  'subjectType\(SubjectType\.HUMAN\.name\(\)\)' \
  --glob '**/eqochat-chat/src/main/java/**/ChatWebSocketHandler.java'

scan_rule P0 \
  'Chat conversation summary human-only online lookup' \
  'isPrincipalHumanOnline\(String\.valueOf\(target\.id\(\)\)\)' \
  --glob '**/eqochat-chat/src/main/java/**/ConversationServiceImpl.java'

scan_rule P0 \
  'Chat conversation legacy human-default viewer overload' \
  'listConversations\(Long principalHumanId\)|getConversation\(Long principalHumanId,[[:space:]]*Long conversationId\)|getMessages\(Long principalHumanId,[[:space:]]*Long conversationId,[[:space:]]*Long lastMessageId,[[:space:]]*Integer limit\)|SubjectRef\.human\(principalHumanId\)' \
  --glob '**/eqochat-chat-api/src/main/java/**/ConversationService.java' \
  --glob '**/eqochat-chat/src/main/java/**/ConversationServiceImpl.java'

scan_rule P0 \
  'WebSocket principal HUMAN active subject fallback' \
  'registerActiveSubjectSession\(principalHumanId,[[:space:]]*principalHumanId,[[:space:]]*"HUMAN"|List\.of\(SubjectRef\.human\(principalHumanId\)\)' \
  --glob '**/eqochat-framework-websocket/src/main/java/**/WebSocketSessionManager.java' \
  --glob '**/eqochat-chat/src/main/java/**/ChatWebSocketHandler.java'

scan_rule P0 \
  'frontend WebSocket principal HUMAN subject fallback' \
  'SubjectType\.HUMAN\).*principalHumanId|principalHumanId.*SubjectType\.HUMAN|subjectType:[[:space:]]*['"'"'"]HUMAN['"'"'"].*principalHumanId|principalHumanId.*subjectType:[[:space:]]*['"'"'"]HUMAN['"'"'"]' \
  --glob '**/frontend/src/utils/websocket.ts' \
  --glob '**/frontend/src/App.vue'

scan_rule P0 \
  'frontend World token-derived HUMAN subject fallback' \
  'currentHumanSubject|parsePrincipalHumanIdFromToken|useUserStore|subjectType:[[:space:]]*['"'"'"]HUMAN['"'"'"]' \
  --glob '**/frontend/src/api/modules/world.ts'

scan_rule P0 \
  'frontend active subject computed HUMAN fallback' \
  'return humanSubject\.value|currentActiveSubjectId\.value[[:space:]]*=[[:space:]]*currentPrincipalHumanId\.value|subjectType:[[:space:]]*['"'"'"]HUMAN['"'"'"].*currentPrincipalHumanId|currentPrincipalHumanId.*subjectType:[[:space:]]*['"'"'"]HUMAN['"'"'"]|if\s*\(!wanted\)\s*\{\s*setHuman\(\)' \
  -U \
  --glob '**/frontend/src/store/modules/activeSubject.ts' \
  --glob '**/frontend/src/store/modules/chat.ts'

scan_rule P0 \
  'Actor/World direct user_friend storage dependency' \
  'UserFriendMapper|com\.eqochat\.business\.user\.entity\.UserFriend' \
  --glob '**/eqochat-actor/src/main/java/**/*.java' \
  --glob '**/eqochat-world/src/main/java/**/*.java'

scan_rule P0 \
  'Actor direct human/agent implementation or API dependency' \
  'com\.eqochat\.business\.(user|agent)\.(api|mapper|entity|service\.impl)|UserProfileMapper|AgentProfileMapper|AgentBindingMapper|UserFriendMapper|com\.eqochat\.business\.user\.entity\.UserFriend|<artifactId>eqochat-(user-api|user|agent)</artifactId>' \
  --glob '**/eqochat-actor/src/main/java/**/*.java' \
  --glob '**/eqochat-actor/pom.xml'

scan_rule P0 \
  'Actor subject directory runtime source fallback' \
  'return refresh(Human|Agent)\(' \
  --glob '**/eqochat-actor/src/main/java/**/SubjectDirectoryServiceImpl.java'

scan_rule P0 \
  'Subject profile implicit principal HUMAN viewer fallback' \
  'SubjectRef\.human\(principalHumanId\)|isFriend\(Long[[:space:]]+principalHumanId' \
  --glob '**/eqochat-actor/src/main/java/**/SubjectProfileServiceImpl.java'

scan_rule P0 \
  'World direct user_friend table SQL dependency' \
  '\buser_friend\b' \
  --glob '**/eqochat-world/src/main/java/**/*.java'

scan_rule P0 \
  'World implementation dependency on eqochat-user module' \
  '<artifactId>eqochat-user</artifactId>' \
  --glob '**/eqochat-world/pom.xml'

scan_rule P0 \
  'Contact direct human/agent implementation or API dependency' \
  'com\.eqochat\.business\.(user|agent)\.(api|mapper|entity|service\.impl)|UserProfileMapper|AgentProfileMapper|UserFriendMapper|com\.eqochat\.business\.user\.entity\.UserFriend|<artifactId>eqochat-(user-api|user|agent)</artifactId>' \
  --glob '**/eqochat-contact/src/main/java/**/*.java' \
  --glob '**/eqochat-contact/pom.xml'

scan_rule P0 \
  'Contact direct legacy contact storage table or type' \
  '\b(user_friend|user_contact_tag|UserContactTag|UserContactTagMapper)\b' \
  --glob '**/eqochat-contact/src/main/java/**/*.java'

scan_rule P0 \
  'Contact group direct human-only owner/member query' \
  'owner_id = #\{ownerId\} AND del_token|user_id = #\{userId\} AND del_token|group_id = #\{groupId\} AND user_id = #\{userId\}|EXISTS\(SELECT 1 FROM group_member WHERE group_id = #\{groupId\} AND user_id = #\{userId\}' \
  --glob '**/eqochat-contact/src/main/java/**/Group*Mapper.java'

scan_rule P0 \
  'Contact group legacy human-default owner/member API' \
  '\bfindByOwnerId\b|\bfindByUserId\b|\bfindByGroupAndUser\b|isMember\(Long groupId, Long userId\)' \
  --glob '**/eqochat-contact/src/main/java/**/Group*Mapper.java'

scan_rule P0 \
  'Contact group member entity user-shaped Java field' \
  '\bprivate[[:space:]]+Long[[:space:]]+userId\b' \
  --glob '**/eqochat-contact/src/main/java/**/GroupMember.java'

scan_rule P0 \
  'Contact controller implicit principal HUMAN owner fallback' \
  'SubjectRef\.human\(principalHumanId\)' \
  --glob '**/eqochat-contact/src/main/java/**/ContactController.java'

scan_rule P0 \
  'FriendRequest inbox legacy implicit principal subject aggregate' \
  'listReceived\(Long userId\)|listSent\(Long userId\)|resolveOptionalSubject|resolveInboxSubjects|principalSubjects|listAssociatedSubjects\(principalHumanId\)|SubjectRef\.human\(principalHumanId\)' \
  --glob '**/eqochat-contact-api/src/main/java/**/FriendRequestService.java' \
  --glob '**/eqochat-contact/src/main/java/**/FriendRequestController.java' \
  --glob '**/eqochat-contact/src/main/java/**/FriendRequestServiceImpl.java'

scan_rule P0 \
  'Contact/FriendRequest null subject type defaults to HUMAN' \
  'type[[:space:]]*!=[[:space:]]*null[[:space:]]*\?[[:space:]]*type[[:space:]]*:[[:space:]]*.*HUMAN|subjectTypeOrHuman|normalizeFriendType' \
  --glob '**/eqochat-contact/src/main/java/**/ContactServiceImpl.java' \
  --glob '**/eqochat-contact/src/main/java/**/FriendRequestServiceImpl.java'

scan_rule P0 \
  'Notification controller implicit principal HUMAN recipient fallback' \
  'SubjectRef\.human\(principalHumanId\)' \
  --glob '**/eqochat-notification/src/main/java/**/NotificationController.java'

scan_rule P0 \
  'Credit audit direct human-only actor query' \
  '\b(operator|reporter|reviewer)_id = #\{(operator|reporter|reviewer)Id\} AND del_token' \
  --glob '**/eqochat-credit/src/main/java/**/*Mapper.java'

scan_rule P0 \
  'Credit audit id-only HUMAN actor overload' \
  '\bfindBy(Operator|Reporter|Reviewer)Id\(Long[[:space:]]+(operator|reporter|reviewer)Id\)' \
  --glob '**/eqochat-credit/src/main/java/**/*Mapper.java'

scan_rule P0 \
  'Project direct human/agent implementation dependency' \
  'com\.eqochat\.business\.(user|agent)\.(mapper|entity|service\.impl)|UserProfileMapper|AgentProfileMapper|UserFriendMapper|com\.eqochat\.business\.user\.entity\.UserFriend|<artifactId>eqochat-(user|agent)</artifactId>' \
  --glob '**/eqochat-project/src/main/java/**/*.java' \
  --glob '**/eqochat-project/pom.xml'

scan_rule P0 \
  'Project service legacy human-default viewer overload' \
  'listMyProjects\(Long viewerId\)|getProjectDetail\(Long viewerId,[[:space:]]*Long projectId\)|shareLink\(Long viewerId,[[:space:]]*Long projectId\)|listSidebar(Tasks|Payments|Files)\(Long viewerId,[[:space:]]*Long projectId\)|resolveOptionalViewerSubject|return listMyProjects\(principalHumanId\)' \
  --glob '**/eqochat-project-api/src/main/java/**/ProjectService.java' \
  --glob '**/eqochat-project/src/main/java/**/ProjectServiceImpl.java'

scan_rule P0 \
  'Project write service principal misnamed as viewerId' \
  'createProject\(Long viewerId|requestBidUpdate\(Long viewerId|transferOwnership\(Long viewerId|createTask\(Long viewerId|createPayment\(Long viewerId|ensureViewerCanAccess\(viewerId,' \
  --glob '**/eqochat-project-api/src/main/java/**/ProjectService.java' \
  --glob '**/eqochat-project/src/main/java/**/ProjectServiceImpl.java'

scan_rule P0 \
  'Wallet settlement subject derived outside actor policy' \
  'settlementSubject\(WalletCapability' \
  --glob '**/eqochat-agent/src/main/java/**/*.java' \
  --glob '**/eqochat-project/src/main/java/**/*.java'

scan_rule P0 \
  'Wallet settlement human id converted to subject outside actor policy' \
  'SubjectRef\.human\([^[:cntrl:]]*settlementHumanId(\(\))?' \
  --glob '**/eqochat-agent/src/main/java/**/*.java' \
  --glob '**/eqochat-project/src/main/java/**/*.java'

scan_rule P0 \
  'Wallet settlement helper returns subject outside actor policy' \
  'SubjectRef[[:space:]]+[^[:cntrl:]]*[Ss]ettlement[^[:cntrl:]]*\([^[:cntrl:]]*WalletCapability' \
  --glob '**/eqochat-agent/src/main/java/**/*.java' \
  --glob '**/eqochat-project/src/main/java/**/*.java'

scan_rule P0 \
  'frontend Project write actor params optional' \
  'actorSubject(Id|Type)\?:' \
  --glob '**/frontend/src/api/modules/project.ts'

for index in "${!WARN_LABELS[@]}"; do
  scan_rule WARN "${WARN_LABELS[$index]}" "${WARN_PATTERNS[$index]}"
done

printf '\nSummary: P0=%d WARN_RUNTIME=%d WARN_HISTORY=%d WARN=%d\n' "$p0_count" "$warn_runtime_count" "$warn_history_count" "$warn_count"

if (( p0_count > 0 )); then
  if [[ "$ALLOW_EXISTING" == "1" ]]; then
    printf 'ALLOW_EXISTING=1: reporting P0 findings without failing.\n'
  else
    printf 'P0 findings detected. Set ALLOW_EXISTING=1 only for Sprint 0 baseline reporting.\n' >&2
    exit 1
  fi
fi

printf 'Actor static guard completed.\n'
