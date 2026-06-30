#!/usr/bin/env bash
set -euo pipefail

ALLOW_EXISTING="${ALLOW_EXISTING:-0}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

command -v rg >/dev/null 2>&1 || die "missing dependency: rg"

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
  'subject_?type[^[:cntrl:]]*(USER/AGENT|USER|AGENT)'
  'sender_?type[^[:cntrl:]]*(USER/AGENT|USER|AGENT)'
  'recipient_?type[^[:cntrl:]]*(USER/AGENT|USER|AGENT)'
  '(mirror user|mirror profile|mirror user profile)'
  '\b(is_agent|author_ai)\b'
)

p0_count=0
warn_count=0

count_lines() {
  local text="$1"
  printf '%s\n' "$text" | sed '/^$/d' | wc -l | tr -d ' '
}

scan_rule() {
  local severity="$1"
  local label="$2"
  local pattern="$3"
  local output
  local count

  if output="$(rg -n --color never --hidden "${RG_IGNORE[@]}" -e "$pattern" "${SCAN_PATHS[@]}")"; then
    count="$(count_lines "$output")"
    printf '\n[%s] %s (%s)\n' "$severity" "$label" "$count"
    printf '%s\n' "$output"
    if [[ "$severity" == "P0" ]]; then
      p0_count=$((p0_count + count))
    else
      warn_count=$((warn_count + count))
    fi
  fi
}

printf 'Actor static guard\n'
printf 'Scan paths: %s\n' "${SCAN_PATHS[*]}"
printf 'ALLOW_EXISTING=%s\n' "$ALLOW_EXISTING"

for index in "${!P0_LABELS[@]}"; do
  scan_rule P0 "${P0_LABELS[$index]}" "${P0_PATTERNS[$index]}"
done

for index in "${!WARN_LABELS[@]}"; do
  scan_rule WARN "${WARN_LABELS[$index]}" "${WARN_PATTERNS[$index]}"
done

printf '\nSummary: P0=%d WARN=%d\n' "$p0_count" "$warn_count"

if (( p0_count > 0 )); then
  if [[ "$ALLOW_EXISTING" == "1" ]]; then
    printf 'ALLOW_EXISTING=1: reporting P0 findings without failing.\n'
  else
    printf 'P0 findings detected. Set ALLOW_EXISTING=1 only for Sprint 0 baseline reporting.\n' >&2
    exit 1
  fi
fi

printf 'Actor static guard completed.\n'
