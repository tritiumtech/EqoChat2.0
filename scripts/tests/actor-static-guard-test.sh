#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
GUARD="$ROOT_DIR/scripts/actor-static-guard.sh"

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

find_real_rg() {
  if [[ -n "${RG_BIN:-}" ]]; then
    normalize_tool_path "$RG_BIN"
    return
  fi

  local candidate
  candidate="$(command -v rg || true)"
  if [[ -n "$candidate" && -x "$candidate" ]]; then
    printf '%s\n' "$candidate"
    return
  fi

  if command -v powershell.exe >/dev/null 2>&1; then
    candidate="$(powershell.exe -NoProfile -Command "(Get-Command rg -ErrorAction Stop).Source" 2>/dev/null | tr -d '\r' || true)"
    if [[ -n "$candidate" ]]; then
      normalize_tool_path "$candidate"
      return
    fi
  fi

  die "could not find an executable rg; set RG_BIN to run this test"
}

run_success() {
  local label="$1"
  shift
  if ! "$@"; then
    die "$label failed unexpectedly"
  fi
}

run_failure() {
  local label="$1"
  shift
  if "$@"; then
    die "$label succeeded unexpectedly"
  fi
}

tmp_dir="$(mktemp -d "$ROOT_DIR/.tmp.actor-static-guard-test.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT
tmp_rel="$(basename "$tmp_dir")"

real_rg="$(find_real_rg)"
"$real_rg" --version >/dev/null 2>&1 || die "rg is not executable: $real_rg"

clean_dir="$tmp_dir/clean"
p0_dir="$tmp_dir/p0"
legacy_runtime_dir="$tmp_dir/legacy-runtime"
missing_dir="$tmp_dir/missing"
clean_scan_path="$tmp_rel/clean"
p0_scan_path="$tmp_rel/p0"
legacy_runtime_scan_path="$tmp_rel/legacy-runtime"
missing_scan_path="$tmp_rel/missing"
mkdir -p "$clean_dir" "$p0_dir" "$legacy_runtime_dir/eqochat-user/src/main/java/com/eqochat/business/user/controller/profile" "$legacy_runtime_dir/eqochat-user/src/main/java/com/eqochat/business/user/service/impl" "$legacy_runtime_dir/frontend/src/api/modules" "$legacy_runtime_dir/frontend/src/utils" "$legacy_runtime_dir/frontend/src/store/modules" "$legacy_runtime_dir/frontend/src"
mkdir -p "$clean_dir/eqochat-project/src/main/java/com/eqochat/business/project/service/impl"
mkdir -p "$legacy_runtime_dir/eqochat-world/src/main/java/com/eqochat/business/world/mapper" "$legacy_runtime_dir/eqochat-world/src/main/java/com/eqochat/business/world/service/impl" "$legacy_runtime_dir/eqochat-world" "$legacy_runtime_dir/eqochat-actor/src/main/java/com/eqochat/business/actor/service/impl" "$legacy_runtime_dir/eqochat-contact/src/main/java/com/eqochat/business/contact/service/impl" "$legacy_runtime_dir/eqochat-contact/src/main/java/com/eqochat/business/contact/mapper" "$legacy_runtime_dir/eqochat-contact/src/main/java/com/eqochat/business/contact/controller/friend" "$legacy_runtime_dir/eqochat-contact-api/src/main/java/com/eqochat/business/contact/api/service" "$legacy_runtime_dir/eqochat-contact" "$legacy_runtime_dir/eqochat-credit/src/main/java/com/eqochat/business/credit/mapper" "$legacy_runtime_dir/eqochat-project"
mkdir -p "$legacy_runtime_dir/eqochat-user/src/main/java/com/eqochat/business/user/mapper" "$legacy_runtime_dir/eqochat-user/src/main/java/com/eqochat/business/user/entity/neo4j" "$legacy_runtime_dir/eqochat-user" "$legacy_runtime_dir/eqochat-server/src/main/resources"
mkdir -p "$legacy_runtime_dir/eqochat-world/src/main/java/com/eqochat/business/world/controller/world" "$legacy_runtime_dir/eqochat-world-api/src/main/java/com/eqochat/business/world/api/service"
mkdir -p "$legacy_runtime_dir/eqochat-contact/src/main/java/com/eqochat/business/contact/entity"
mkdir -p "$legacy_runtime_dir/eqochat-chat/src/main/java/com/eqochat/business/chat/websocket" "$legacy_runtime_dir/eqochat-chat/src/main/java/com/eqochat/business/chat/service/impl"
mkdir -p "$legacy_runtime_dir/eqochat-chat-api/src/main/java/com/eqochat/business/chat/api/service"
mkdir -p "$legacy_runtime_dir/eqochat-framework-websocket/src/main/java/com/eqochat/framework/websocket"
mkdir -p "$legacy_runtime_dir/eqochat-project-api/src/main/java/com/eqochat/business/project/api/service" "$legacy_runtime_dir/eqochat-project/src/main/java/com/eqochat/business/project/service/impl"
mkdir -p "$legacy_runtime_dir/eqochat-agent/src/main/java/com/eqochat/business/agent/controller"
mkdir -p "$legacy_runtime_dir/eqochat-notification/src/main/java/com/eqochat/business/notification/controller"

printf 'class Good { String type = "HUMAN"; }\n' > "$clean_dir/Good.java"
printf 'class AllowedProjectSettlementAuditFacts { Object persist(WalletCapability wallet) { return ProjectPayment.builder().settlementHumanId(wallet.settlementHumanId()).build(); } }\n' > "$clean_dir/eqochat-project/src/main/java/com/eqochat/business/project/service/impl/AllowedProjectSettlementAuditFacts.java"
printf 'class Bad { Object value = SenderType.USER; }\n' > "$p0_dir/Bad.java"
printf '@RequestMapping("/api/v1/users") class UserController {}\n' > "$legacy_runtime_dir/eqochat-user/src/main/java/com/eqochat/business/user/controller/profile/UserController.java"
printf 'class UserServiceImpl { UserProfileService service; }\n' > "$legacy_runtime_dir/eqochat-user/src/main/java/com/eqochat/business/user/service/impl/UserServiceImpl.java"
printf 'class UserFriendMapper { String sql = "SELECT * FROM user_friend"; }\n' > "$legacy_runtime_dir/eqochat-user/src/main/java/com/eqochat/business/user/mapper/UserFriendMapper.java"
printf 'class UserFollowMapper { String sql = "SELECT * FROM user_follow"; }\n' > "$legacy_runtime_dir/eqochat-user/src/main/java/com/eqochat/business/user/mapper/UserFollowMapper.java"
printf '<project><dependencies><dependency><artifactId>spring-boot-starter-data-neo4j</artifactId></dependency></dependencies></project>\n' > "$legacy_runtime_dir/eqochat-user/pom.xml"
printf 'import org.springframework.data.neo4j.core.schema.Node; @Node("User") class SocialUser {}\n' > "$legacy_runtime_dir/eqochat-user/src/main/java/com/eqochat/business/user/entity/neo4j/SocialUser.java"
printf '<project><dependencies><dependency><artifactId>spring-boot-starter-data-neo4j</artifactId></dependency></dependencies></project>\n' > "$legacy_runtime_dir/eqochat-server/pom.xml"
printf 'spring:\n  data:\n    neo4j:\n      uri: bolt://localhost:7687\n' > "$legacy_runtime_dir/eqochat-server/src/main/resources/application.yml"
printf 'export const bad = "/api/v1/users/search"\n' > "$legacy_runtime_dir/frontend/src/api/modules/user.ts"
printf 'export interface ProjectActorParams { actorSubjectId?: number; actorSubjectType?: string }\n' > "$legacy_runtime_dir/frontend/src/api/modules/project.ts"
printf 'class WorldPostMapper { String sql = "LEFT JOIN user_profile u ON u.id = p.author_id"; }\n' > "$legacy_runtime_dir/eqochat-world/src/main/java/com/eqochat/business/world/mapper/WorldPostMapper.java"
printf '@GetMapping("/users/{authorId}/posts") class WorldController { Object fallback(Long principalHumanId) { return SubjectRef.human(principalHumanId); } }\n' > "$legacy_runtime_dir/eqochat-world/src/main/java/com/eqochat/business/world/controller/world/WorldController.java"
printf 'interface WorldPostStatsApi { long countByAuthorId(Long authorId); }\n' > "$legacy_runtime_dir/eqochat-world-api/src/main/java/com/eqochat/business/world/api/service/WorldPostStatsApi.java"
printf 'class ChatWebSocketHandler { Object p = PresencePayload.builder().subjectType(SubjectType.HUMAN.name()).build(); Object fallback(Long principalHumanId) { return List.of(SubjectRef.human(principalHumanId)); } }\n' > "$legacy_runtime_dir/eqochat-chat/src/main/java/com/eqochat/business/chat/websocket/ChatWebSocketHandler.java"
printf 'class WebSocketSessionManager { void registerPrincipalHumanSession(String principalHumanId, Object session) { registerActiveSubjectSession(principalHumanId, principalHumanId, "HUMAN", session); } }\n' > "$legacy_runtime_dir/eqochat-framework-websocket/src/main/java/com/eqochat/framework/websocket/WebSocketSessionManager.java"
printf 'interface ConversationService { Object listConversations(Long principalHumanId); Object getMessages(Long principalHumanId, Long conversationId, Long lastMessageId, Integer limit); }\n' > "$legacy_runtime_dir/eqochat-chat-api/src/main/java/com/eqochat/business/chat/api/service/ConversationService.java"
printf 'class ConversationServiceImpl { boolean online(Object target, Object webSocketSessionManager) { return webSocketSessionManager.isPrincipalHumanOnline(String.valueOf(target.id())); } Object resolve(Long principalHumanId) { return SubjectRef.human(principalHumanId); } }\n' > "$legacy_runtime_dir/eqochat-chat/src/main/java/com/eqochat/business/chat/service/impl/ConversationServiceImpl.java"
printf 'class WorldServiceImpl { UserFriendMapper mapper; Object listFeed(Long viewerId) { return SubjectRef.human(viewerId); } }\n' > "$legacy_runtime_dir/eqochat-world/src/main/java/com/eqochat/business/world/service/impl/WorldServiceImpl.java"
printf 'class WorldFriendSql { String sql = "SELECT * FROM user_friend"; }\n' > "$legacy_runtime_dir/eqochat-world/src/main/java/com/eqochat/business/world/mapper/WorldFriendSql.java"
printf '<project><dependencies><dependency><artifactId>eqochat-user</artifactId></dependency></dependencies></project>\n' > "$legacy_runtime_dir/eqochat-world/pom.xml"
printf 'export const badWorld = "/api/v1/world/users/1/posts"; function currentHumanSubject() { return { subjectId: 1, subjectType: "HUMAN" }; }\n' > "$legacy_runtime_dir/frontend/src/api/modules/world.ts"
printf 'const bad = this.principalHumanId ? { subjectId: this.principalHumanId, subjectType: SubjectType.HUMAN } : null\n' > "$legacy_runtime_dir/frontend/src/utils/websocket.ts"
printf 'wsClient.setActiveSubject({ subjectId: principalHumanId, subjectType: "HUMAN" })\n' > "$legacy_runtime_dir/frontend/src/App.vue"
printf 'const currentSubject = computed(() => humanSubject.value); currentActiveSubjectId.value = currentPrincipalHumanId.value\nif (!wanted) {\n  setHuman()\n}\n' > "$legacy_runtime_dir/frontend/src/store/modules/activeSubject.ts"
printf '<project><dependencies><dependency><artifactId>eqochat-agent</artifactId></dependency></dependencies></project>\n' > "$legacy_runtime_dir/eqochat-actor/pom.xml"
printf 'class SubjectDirectoryServiceImpl { Object getHuman() { return refreshHuman(1L); } }\n' > "$legacy_runtime_dir/eqochat-actor/src/main/java/com/eqochat/business/actor/service/impl/SubjectDirectoryServiceImpl.java"
printf 'class SubjectProfileServiceImpl { boolean isFriend(Long principalHumanId, Object target) { return subjectRelationshipApi.areFriends(SubjectRef.human(principalHumanId), target); } }\n' > "$legacy_runtime_dir/eqochat-actor/src/main/java/com/eqochat/business/actor/service/impl/SubjectProfileServiceImpl.java"
printf 'interface FriendRequestService { Object listReceived(Long userId); Object listSent(Long userId); }\n' > "$legacy_runtime_dir/eqochat-contact-api/src/main/java/com/eqochat/business/contact/api/service/FriendRequestService.java"
printf 'class FriendRequestServiceImpl { AgentProfileMapper mapper; Object principalSubjects(Long principalHumanId) { return SubjectRef.human(principalHumanId); } Object subjectTypeOrHuman(Object type) { return type != null ? type : SubjectType.HUMAN; } }\n' > "$legacy_runtime_dir/eqochat-contact/src/main/java/com/eqochat/business/contact/service/impl/FriendRequestServiceImpl.java"
printf 'class ContactServiceImpl { Object normalizeFriendType(Object type) { return type != null ? type : RelationshipSubjectType.HUMAN; } }\n' > "$legacy_runtime_dir/eqochat-contact/src/main/java/com/eqochat/business/contact/service/impl/ContactServiceImpl.java"
printf 'class LegacyContactStorageMapper { String sql = "SELECT * FROM user_contact_tag"; UserContactTagMapper mapper; }\n' > "$legacy_runtime_dir/eqochat-contact/src/main/java/com/eqochat/business/contact/mapper/LegacyContactStorageMapper.java"
printf 'class GroupMemberMapper { String sql = "SELECT * FROM group_member WHERE group_id = #{groupId} AND user_id = #{userId} AND del_token = ''0''"; }\n' > "$legacy_runtime_dir/eqochat-contact/src/main/java/com/eqochat/business/contact/mapper/GroupMemberMapper.java"
printf 'class GroupProfileMapper { void findByOwnerId() {} }\n' > "$legacy_runtime_dir/eqochat-contact/src/main/java/com/eqochat/business/contact/mapper/GroupProfileMapper.java"
printf 'class LegacyGroupMemberMapper { void findByUserId() {} void findByGroupAndUser() {} boolean isMember(Long groupId, Long userId) { return false; } }\n' > "$legacy_runtime_dir/eqochat-contact/src/main/java/com/eqochat/business/contact/mapper/LegacyGroupMemberMapper.java"
printf 'class GroupMember { private Long userId; }\n' > "$legacy_runtime_dir/eqochat-contact/src/main/java/com/eqochat/business/contact/entity/GroupMember.java"
printf 'class ContactController { Object owner(Long principalHumanId) { return SubjectRef.human(principalHumanId); } }\n' > "$legacy_runtime_dir/eqochat-contact/src/main/java/com/eqochat/business/contact/controller/friend/ContactController.java"
printf 'class NotificationController { Object recipient(Long principalHumanId) { return SubjectRef.human(principalHumanId); } }\n' > "$legacy_runtime_dir/eqochat-notification/src/main/java/com/eqochat/business/notification/controller/NotificationController.java"
printf 'class ViolationRecordMapper { String sql = "SELECT * FROM violation_record WHERE reporter_id = #{reporterId} AND del_token = ''0''"; Object findByReporterId(Long reporterId) { return null; } }\n' > "$legacy_runtime_dir/eqochat-credit/src/main/java/com/eqochat/business/credit/mapper/ViolationRecordMapper.java"
printf 'class CreditRecordMapper { Object findByOperatorId(Long operatorId) { return null; } }\n' > "$legacy_runtime_dir/eqochat-credit/src/main/java/com/eqochat/business/credit/mapper/CreditRecordMapper.java"
printf '<project>\n<dependencies>\n<dependency><artifactId>eqochat-user</artifactId></dependency>\n<dependency><artifactId>eqochat-user-api</artifactId></dependency>\n</dependencies>\n</project>\n' > "$legacy_runtime_dir/eqochat-contact/pom.xml"
printf 'class AgentController { Object settlementSubject(WalletCapability wallet, Object directRecipient) { return null; } }\n' > "$legacy_runtime_dir/eqochat-agent/src/main/java/com/eqochat/business/agent/controller/AgentController.java"
printf '<project><dependencies><dependency><artifactId>eqochat-agent</artifactId></dependency></dependencies></project>\n' > "$legacy_runtime_dir/eqochat-project/pom.xml"
printf 'interface ProjectService { Object listMyProjects(Long viewerId); Object shareLink(Long viewerId, Long projectId); Object createTask(Long viewerId, Long projectId, Object request); }\n' > "$legacy_runtime_dir/eqochat-project-api/src/main/java/com/eqochat/business/project/api/service/ProjectService.java"
printf 'class ProjectServiceImpl { Object resolveOptionalViewerSubject(Long principalHumanId) { return null; } Object list(Long principalHumanId) { return listMyProjects(principalHumanId); } Object listMyProjects(Long viewerId) { return null; } Object createPayment(Long viewerId, Long projectId, Object request) { return ensureViewerCanAccess(viewerId, null, projectId, null); } Object ensureViewerCanAccess(Long viewerId, Object subject, Long projectId, Object project) { return null; } }\n' > "$legacy_runtime_dir/eqochat-project/src/main/java/com/eqochat/business/project/service/impl/ProjectServiceImpl.java"
printf 'class ProjectWalletSettlementInlineLeak { Object leak(WalletCapability wallet) { return SubjectRef.human(wallet.settlementHumanId()); } }\n' > "$legacy_runtime_dir/eqochat-project/src/main/java/com/eqochat/business/project/service/impl/ProjectWalletSettlementInlineLeak.java"
printf 'class ProjectWalletSettlementHelperLeak { SubjectRef resolveSettlementSubject(WalletCapability wallet) { Long settlementHumanId = wallet.settlementHumanId(); return SubjectRef.human(settlementHumanId); } }\n' > "$legacy_runtime_dir/eqochat-project/src/main/java/com/eqochat/business/project/service/impl/ProjectWalletSettlementHelperLeak.java"

run_success "clean fixture" env RG_BIN="$real_rg" bash "$GUARD" "$clean_scan_path" > "$tmp_dir/clean.out" 2> "$tmp_dir/clean.err"

run_failure "strict P0 fixture" env RG_BIN="$real_rg" bash "$GUARD" "$p0_scan_path" > "$tmp_dir/p0-strict.out" 2> "$tmp_dir/p0-strict.err"
grep -q 'P0 findings detected' "$tmp_dir/p0-strict.err" || die "strict P0 fixture did not report P0 failure"
grep -q 'Summary: P0=1' "$tmp_dir/p0-strict.out" || die "strict P0 fixture did not report P0=1"
grep -q 'WARN_RUNTIME=' "$tmp_dir/p0-strict.out" || die "strict P0 fixture did not report WARN_RUNTIME summary"

run_success "ALLOW_EXISTING P0 fixture" env RG_BIN="$real_rg" ALLOW_EXISTING=1 bash "$GUARD" "$p0_scan_path" > "$tmp_dir/p0-allow.out" 2> "$tmp_dir/p0-allow.err"
grep -q 'Summary: P0=1' "$tmp_dir/p0-allow.out" || die "ALLOW_EXISTING fixture did not preserve P0 count"
grep -q 'WARN_HISTORY=' "$tmp_dir/p0-allow.out" || die "ALLOW_EXISTING fixture did not report WARN_HISTORY summary"
grep -q 'ALLOW_EXISTING=1: reporting P0 findings without failing.' "$tmp_dir/p0-allow.out" || die "ALLOW_EXISTING fixture did not explain non-failing P0"

run_failure "legacy runtime actor guard fixture" env RG_BIN="$real_rg" bash "$GUARD" "$legacy_runtime_scan_path" > "$tmp_dir/legacy-runtime.out" 2> "$tmp_dir/legacy-runtime.err"
grep -q 'frontend legacy /api/v1/users client usage' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report frontend legacy users client"
grep -q 'backend legacy /api/v1/users public route' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report backend legacy users route"
grep -q 'legacy UserServiceImpl direct human-profile implementation dependency' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report UserServiceImpl dependency"
grep -q 'User module legacy friend/follow storage ownership' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report user legacy friend/follow storage ownership"
grep -q 'User module Neo4j social graph runtime ownership' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report user Neo4j social graph ownership"
grep -q 'Server direct Neo4j runtime dependency or config' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report server Neo4j runtime dependency"
grep -q 'WorldPostMapper source-profile author identity fallback join' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report world source-profile fallback join"
grep -q 'backend legacy World human author route' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report legacy world human author route"
grep -q 'frontend legacy World human author client usage' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report frontend legacy world users client"
grep -q 'World human-only author stats API' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report world human-only stats API"
grep -q 'World service legacy human-default viewer overload' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report world legacy viewer overload"
grep -q 'World controller implicit principal HUMAN viewer fallback' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report world controller implicit human viewer fallback"
grep -q 'Chat presence hard-coded HUMAN subject payload' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report chat human-only presence payload"
grep -q 'Chat conversation summary human-only online lookup' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report chat human-only online lookup"
grep -q 'Chat conversation legacy human-default viewer overload' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report chat legacy viewer overload"
grep -q 'WebSocket principal HUMAN active subject fallback' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report WebSocket principal human active fallback"
grep -q 'frontend WebSocket principal HUMAN subject fallback' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report frontend WebSocket principal human fallback"
grep -q 'frontend World token-derived HUMAN subject fallback' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report frontend World human fallback"
grep -q 'frontend active subject computed HUMAN fallback' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report frontend active subject human fallback"
grep -q 'Actor/World direct user_friend storage dependency' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report direct user_friend dependency"
grep -q 'Actor direct human/agent implementation or API dependency' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report actor implementation dependency"
grep -q 'Actor subject directory runtime source fallback' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report actor runtime source fallback"
grep -q 'Subject profile implicit principal HUMAN viewer fallback' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report subject profile human viewer fallback"
grep -q 'World direct user_friend table SQL dependency' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report world user_friend table dependency"
grep -q 'World implementation dependency on eqochat-user module' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report world eqochat-user dependency"
grep -q 'Contact direct human/agent implementation or API dependency' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report contact implementation dependency"
grep -q 'Contact direct legacy contact storage table or type' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report contact legacy storage dependency"
grep -q 'Contact group direct human-only owner/member query' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report contact group human-only query"
grep -q 'Contact group legacy human-default owner/member API' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report contact group legacy human-default API"
grep -q 'Contact group member entity user-shaped Java field' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report contact group user-shaped field"
grep -q 'Contact controller implicit principal HUMAN owner fallback' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report contact controller human owner fallback"
grep -q 'FriendRequest inbox legacy implicit principal subject aggregate' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report friend request legacy inbox aggregate"
grep -q 'Contact/FriendRequest null subject type defaults to HUMAN' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report contact null type human fallback"
grep -q 'Notification controller implicit principal HUMAN recipient fallback' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report notification controller human recipient fallback"
grep -q 'Credit audit direct human-only actor query' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report credit audit human-only query"
grep -q 'Credit audit id-only HUMAN actor overload' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report credit id-only human actor overload"
grep -q 'Project direct human/agent implementation dependency' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report project implementation dependency"
grep -q 'Project service legacy human-default viewer overload' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report project legacy viewer overload"
grep -q 'Project write service principal misnamed as viewerId' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report project write viewerId principal naming"
grep -q 'Wallet settlement subject derived outside actor policy' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report wallet settlement derivation outside actor policy"
grep -q 'Wallet settlement human id converted to subject outside actor policy' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report wallet settlement human id subject conversion"
grep -q 'Wallet settlement helper returns subject outside actor policy' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report wallet settlement helper returning subject"
grep -q 'frontend Project write actor params optional' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report optional Project actor params"
grep -Eq 'Summary: P0=[1-9][0-9]*' "$tmp_dir/legacy-runtime.out" || die "legacy runtime fixture did not report non-zero P0 summary"

fake_dir="$tmp_dir/fake"
mkdir -p "$fake_dir"
fake_rg="$fake_dir/rg"
cat > "$fake_rg" <<'EOF'
#!/usr/bin/env bash
if [[ "${1:-}" == "--version" ]]; then
  if [[ "${FAKE_RG_VERSION_STATUS:-0}" != "0" ]]; then
    printf 'fake rg version failure\n' >&2
    exit "$FAKE_RG_VERSION_STATUS"
  fi
  printf 'fake-rg 0.0.0\n'
  exit 0
fi
if [[ "${1:-}" == "--files" ]]; then
  status="${FAKE_RG_FILES_STATUS:-${FAKE_RG_STATUS:-2}}"
else
  status="${FAKE_RG_SCAN_STATUS:-${FAKE_RG_STATUS:-2}}"
fi
if [[ "$status" -gt 1 ]]; then
  printf 'fake rg failure\n' >&2
fi
exit "$status"
EOF
chmod +x "$fake_rg"

run_failure "fake rg version status 126" env RG_BIN="$fake_rg" FAKE_RG_VERSION_STATUS=126 bash "$GUARD" "$clean_scan_path" > "$tmp_dir/fake-version126.out" 2> "$tmp_dir/fake-version126.err"
grep -q 'ERROR: rg failed to execute' "$tmp_dir/fake-version126.err" || die "fake rg version status 126 was not reported as startup failure"
grep -q 'Exit status: 126' "$tmp_dir/fake-version126.err" || die "fake rg version status 126 did not preserve exit status"

run_failure "fake rg files-only status 2" env RG_BIN="$fake_rg" FAKE_RG_FILES_STATUS=2 FAKE_RG_SCAN_STATUS=1 bash "$GUARD" "$clean_scan_path" > "$tmp_dir/fake-files2.out" 2> "$tmp_dir/fake-files2.err"
grep -q 'ERROR: rg failed while listing scan files' "$tmp_dir/fake-files2.err" || die "fake rg files-only status 2 was not reported as scan-file failure"
grep -q 'Exit status: 2' "$tmp_dir/fake-files2.err" || die "fake rg files-only status 2 did not preserve exit status"

run_failure "fake rg status 2" env RG_BIN="$fake_rg" FAKE_RG_STATUS=2 bash "$GUARD" "$clean_scan_path" > "$tmp_dir/fake2.out" 2> "$tmp_dir/fake2.err"
grep -q 'ERROR: rg failed while listing scan files' "$tmp_dir/fake2.err" || die "fake rg status 2 was not reported as scan failure"
grep -q 'Exit status: 2' "$tmp_dir/fake2.err" || die "fake rg status 2 did not preserve exit status"

run_failure "fake rg status 126" env RG_BIN="$fake_rg" FAKE_RG_STATUS=126 bash "$GUARD" "$clean_scan_path" > "$tmp_dir/fake126.out" 2> "$tmp_dir/fake126.err"
grep -q 'Exit status: 126' "$tmp_dir/fake126.err" || die "fake rg status 126 did not preserve exit status"

run_failure "missing scan path" env RG_BIN="$real_rg" bash "$GUARD" "$missing_scan_path" > "$tmp_dir/missing.out" 2> "$tmp_dir/missing.err"
grep -q 'scan path not found' "$tmp_dir/missing.err" || die "missing scan path was not reported"

printf 'actor-static-guard self-test passed\n'
