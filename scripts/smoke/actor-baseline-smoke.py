#!/usr/bin/env python3
import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request


BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080").rstrip("/")
PHONE = os.environ.get("PHONE", "13900000001")
PASSWORD = os.environ.get("PASSWORD", "Test1234")
TARGET_SUBJECT_ID = int(os.environ.get("TARGET_SUBJECT_ID", os.environ.get("TARGET_USER_ID", "11")))
TARGET_SUBJECT_TYPE = os.environ.get("TARGET_SUBJECT_TYPE", "HUMAN")
SMOKE_TAG = os.environ.get("SMOKE_TAG", "ACTOR_SMOKE")
ACTIVE_BASE_URL = None


def fail(message):
    raise SystemExit(f"ERROR: {message}")


def wsl_windows_host():
    try:
        with open("/etc/resolv.conf", "r", encoding="utf-8") as handle:
            for line in handle:
                parts = line.strip().split()
                if len(parts) >= 2 and parts[0] == "nameserver":
                    return parts[1]
    except OSError:
        return None
    return None


def candidate_base_urls():
    parsed = urllib.parse.urlsplit(BASE_URL)
    candidates = [BASE_URL]
    if parsed.hostname in ("localhost", "127.0.0.1", "::1"):
        replacements = ["host.docker.internal", wsl_windows_host()]
        for host in replacements:
            if not host:
                continue
            netloc = host
            if parsed.port:
                netloc = f"{host}:{parsed.port}"
            candidate = urllib.parse.urlunsplit((
                parsed.scheme or "http",
                netloc,
                parsed.path,
                parsed.query,
                parsed.fragment,
            )).rstrip("/")
            if candidate not in candidates:
                candidates.append(candidate)
    return candidates


def http_json(method, path, token=None, payload=None):
    global ACTIVE_BASE_URL

    body = None
    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if payload is not None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")

    attempted = []
    bases = [ACTIVE_BASE_URL] if ACTIVE_BASE_URL else candidate_base_urls()
    last_error = None
    status = None
    raw = ""
    for base_url in bases:
        url = f"{base_url}{path}"
        attempted.append(url)
        request = urllib.request.Request(url, data=body, method=method, headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=15) as response:
                status = response.status
                raw = response.read().decode("utf-8")
                ACTIVE_BASE_URL = base_url
                break
        except urllib.error.HTTPError as exc:
            status = exc.code
            raw = exc.read().decode("utf-8", errors="replace")
            ACTIVE_BASE_URL = base_url
            break
        except urllib.error.URLError as exc:
            last_error = exc
    else:
        fail(f"request failed: {method} {path}: {last_error}; attempted: {', '.join(attempted)}")

    if status < 200 or status >= 300:
        fail(f"HTTP {status} {method} {path}\nResponse body:\n{raw}")

    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        fail(f"response is not valid JSON: {method} {path}\nResponse body:\n{raw}")

    code = data.get("code", 200) if isinstance(data, dict) else 200
    if code != 200:
        message = data.get("message") or data.get("msg") or data.get("error") or "unknown error"
        fail(f"API code={code} message={message} path={path}\nResponse body:\n{raw}")
    return data


def http_status(method, path, token=None, payload=None):
    global ACTIVE_BASE_URL

    body = None
    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if payload is not None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")

    attempted = []
    bases = [ACTIVE_BASE_URL] if ACTIVE_BASE_URL else candidate_base_urls()
    last_error = None
    for base_url in bases:
        url = f"{base_url}{path}"
        attempted.append(url)
        request = urllib.request.Request(url, data=body, method=method, headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=15) as response:
                ACTIVE_BASE_URL = base_url
                return response.status
        except urllib.error.HTTPError as exc:
            ACTIVE_BASE_URL = base_url
            return exc.code
        except urllib.error.URLError as exc:
            last_error = exc
    fail(f"request failed: {method} {path}: {last_error}; attempted: {', '.join(attempted)}")


def api_data(response):
    return response.get("data") if isinstance(response, dict) and "data" in response else response


def expect(label, condition):
    if not condition:
        fail(f"{label}: assertion failed")
    print(f"PASS {label:<28}")


def expect_api_failure(label, method, path, token=None, payload=None):
    try:
        http_json(method, path, token, payload)
    except SystemExit as exc:
        message = str(exc)
        if message.startswith("ERROR: HTTP") or message.startswith("ERROR: API code="):
            print(f"PASS {label:<28}")
            return
        raise
    fail(f"{label}: expected API failure")


def expect_http_status(label, method, path, expected_status, token=None, payload=None):
    status = http_status(method, path, token, payload)
    expect(label, status == expected_status)


def subject_type(value):
    return value in ("HUMAN", "AGENT")


def all_match(items, predicate):
    return isinstance(items, list) and all(predicate(item) for item in items)


def any_match(items, predicate):
    return isinstance(items, list) and any(predicate(item) for item in items)


def first_non_null(*values):
    for value in values:
        if value is not None:
            return value
    return None


def query_path(path, params):
    encoded = urllib.parse.urlencode({key: value for key, value in params.items() if value is not None})
    return f"{path}?{encoded}" if encoded else path


def find_agent_by_subject_id(items, subject_id):
    if not isinstance(items, list):
        return None
    for item in items:
        if item.get("agentSubjectId") == subject_id:
            return item
    return None


def is_expected_api_error(exc):
    message = str(exc)
    return message.startswith("ERROR: HTTP") or message.startswith("ERROR: API code=")


def main():
    print("Actor baseline smoke")
    print(f"Base URL: {BASE_URL}")
    print(f"Login phone: {PHONE}")

    login = http_json("POST", "/api/v1/auth/login", payload={
        "phone": PHONE,
        "password": PASSWORD,
    })
    login_data = api_data(login)
    token = login_data.get("token")
    principal_human_id = first_non_null(
        (login_data.get("userInfo") or {}).get("id"),
        (login_data.get("user") or {}).get("id"),
        login_data.get("id"),
    )
    expect("auth login token", isinstance(token, str) and len(token) > 20)
    expect("principal human id", principal_human_id is not None and int(principal_human_id) > 0)
    principal_human_id = int(principal_human_id)

    me = api_data(http_json("GET", "/api/v1/auth/me", token))
    expect("auth me", me.get("id") is not None and me.get("nickname") is not None)

    agents = api_data(http_json("GET", "/api/v1/agents/me", token))
    expect(
        "agents me",
        any_match(agents, lambda item: item.get("ownerId") is not None
                  and item.get("walletRouting") is not None
                  and item.get("responsibilityChain") is not None),
    )
    expect(
        "agents canonical policy",
        any_match(
            agents,
            lambda item: item.get("agentSubjectId") is not None
            and item.get("agentSubjectType") == "AGENT"
            and item.get("ownerSubjectId") is not None
            and item.get("ownerSubjectType") == "HUMAN"
            and item.get("walletPolicyState") in ("ENABLED", "DISABLED", "PENDING_APPROVAL", "SUSPENDED")
            and item.get("directRecipientSubjectId") is not None
            and subject_type(item.get("directRecipientSubjectType"))
            and isinstance(item.get("financialAutonomy"), bool)
            and item.get("liableHumanId") is not None
            and item.get("liabilityRoute") is not None
            and item.get("liabilityRoute") == item.get("responsibilityChain")
            and item.get("agentSubjectType") != "USER"
            and item.get("ownerSubjectType") != "USER",
        ),
    )
    owned_agent = next(
        (
            item for item in agents
            if item.get("agentSubjectId") is not None and item.get("agentSubjectType") == "AGENT"
        ),
        None,
    )
    expect("owned agent subject", owned_agent is not None)

    wallet_agent = next(
        (
            item for item in agents
            if item.get("agentSubjectId") is not None
            and item.get("agentSubjectType") == "AGENT"
            and item.get("walletEnabled") is True
            and item.get("walletRouting") == "AGENT_DIRECT"
        ),
        None,
    )
    if wallet_agent is None:
        for candidate in agents:
            candidate_id = candidate.get("id") if isinstance(candidate, dict) else None
            candidate_subject_id = candidate.get("agentSubjectId") if isinstance(candidate, dict) else None
            if candidate_id is None or candidate_subject_id is None:
                continue
            try:
                restored_wallet = api_data(http_json(
                    "POST",
                    f"/api/v1/agents/{candidate_id}/wallet/enable",
                    token,
                ))
            except SystemExit as exc:
                if is_expected_api_error(exc):
                    continue
                raise
            if (
                isinstance(restored_wallet, dict)
                and restored_wallet.get("walletEnabled") is True
                and restored_wallet.get("walletRouting") == "AGENT_DIRECT"
            ):
                refreshed_agents = api_data(http_json("GET", "/api/v1/agents/me", token))
                wallet_agent = find_agent_by_subject_id(refreshed_agents, candidate_subject_id)
                break
    expect("agent wallet mutable", wallet_agent is not None)
    wallet_agent_id = wallet_agent.get("id")
    wallet_agent_subject_id = wallet_agent.get("agentSubjectId")

    disabled_wallet = api_data(http_json(
        "POST",
        f"/api/v1/agents/{wallet_agent_id}/wallet/disable",
        token,
        {"reason": "actor smoke wallet toggle"},
    ))
    expect(
        "agent wallet disable",
        isinstance(disabled_wallet, dict)
        and disabled_wallet.get("walletEnabled") is False
        and disabled_wallet.get("walletPolicyState") == "DISABLED"
        and disabled_wallet.get("walletRouting") == "AGENT_TO_OWNER"
        and disabled_wallet.get("settlementSubjectId") == principal_human_id
        and disabled_wallet.get("settlementSubjectType") == "HUMAN",
    )
    agents_after_disable = api_data(http_json("GET", "/api/v1/agents/me", token))
    disabled_agent = find_agent_by_subject_id(agents_after_disable, wallet_agent_subject_id)
    expect(
        "agents me wallet disabled",
        disabled_agent is not None
        and disabled_agent.get("walletEnabled") is False
        and disabled_agent.get("walletPolicyState") == "DISABLED"
        and disabled_agent.get("walletRouting") == "AGENT_TO_OWNER",
    )

    enabled_wallet = api_data(http_json(
        "POST",
        f"/api/v1/agents/{wallet_agent_id}/wallet/enable",
        token,
    ))
    expect(
        "agent wallet enable",
        isinstance(enabled_wallet, dict)
        and enabled_wallet.get("walletEnabled") is True
        and enabled_wallet.get("walletPolicyState") == "ENABLED"
        and enabled_wallet.get("walletRouting") == "AGENT_DIRECT"
        and enabled_wallet.get("directRecipientSubjectId") == wallet_agent_subject_id
        and enabled_wallet.get("directRecipientSubjectType") == "AGENT"
        and enabled_wallet.get("financialAutonomy") is True,
    )
    agents_after_enable = api_data(http_json("GET", "/api/v1/agents/me", token))
    enabled_agent = find_agent_by_subject_id(agents_after_enable, wallet_agent_subject_id)
    expect(
        "agents me wallet enabled",
        enabled_agent is not None
        and enabled_agent.get("walletEnabled") is True
        and enabled_agent.get("walletPolicyState") == "ENABLED"
        and enabled_agent.get("walletRouting") == "AGENT_DIRECT",
    )

    subject_search = api_data(http_json(
        "GET",
        query_path("/api/v1/subjects/search", {
            "keyword": owned_agent.get("agentSubjectId"),
            "viewerSubjectId": owned_agent.get("agentSubjectId"),
            "viewerSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "subject search agent",
        isinstance(subject_search, list)
        and any(
            item.get("subjectId") == owned_agent.get("agentSubjectId")
            and item.get("subjectType") == "AGENT"
            and item.get("subjectType") != "USER"
            for item in subject_search
        ),
    )
    agent_public_profile = api_data(http_json(
        "GET",
        query_path(f"/api/v1/subjects/AGENT/{owned_agent.get('agentSubjectId')}/public", {
            "viewerSubjectId": owned_agent.get("agentSubjectId"),
            "viewerSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "subject public profile agent",
        isinstance(agent_public_profile, dict)
        and agent_public_profile.get("subjectId") == owned_agent.get("agentSubjectId")
        and agent_public_profile.get("subjectType") == "AGENT"
        and agent_public_profile.get("subjectType") != "USER"
        and isinstance(agent_public_profile.get("worldPostCount"), int)
        and isinstance(agent_public_profile.get("creditScore"), int)
        and 300 <= agent_public_profile.get("creditScore") <= 850,
    )
    expect_http_status(
        "legacy users route retired",
        "GET",
        query_path("/api/v1/users/search", {
            "keyword": owned_agent.get("agentSubjectId"),
        }),
        404,
        token,
    )

    contacts = api_data(http_json(
        "GET",
        query_path("/api/v1/contacts", {
            "ownerSubjectId": principal_human_id,
            "ownerSubjectType": "HUMAN",
        }),
        token,
    ))
    expect(
        "contacts list",
        isinstance(contacts, list)
        and len(contacts) >= 1
        and any(item.get("targetSubjectType") == "HUMAN" for item in contacts)
        and any(item.get("targetSubjectType") == "AGENT" for item in contacts),
    )
    first_contact = contacts[0] if isinstance(contacts, list) and contacts else {}
    credit_profile = api_data(http_json(
        "GET",
        query_path("/api/v1/credits/subject", {
            "subjectId": first_contact.get("targetSubjectId"),
            "subjectType": first_contact.get("targetSubjectType"),
        }),
        token,
    ))
    expect(
        "credit profile prd score",
        isinstance(credit_profile, dict)
        and isinstance(credit_profile.get("creditScore"), int)
        and 300 <= credit_profile.get("creditScore") <= 850
        and first_contact.get("targetSubjectType") in ("HUMAN", "AGENT")
        and first_contact.get("targetSubjectType") != "USER",
    )
    agent_owner_contacts = api_data(http_json(
        "GET",
        query_path("/api/v1/contacts", {
            "ownerSubjectId": owned_agent.get("agentSubjectId"),
            "ownerSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "contacts agent owner",
        isinstance(agent_owner_contacts, list)
        and all(
            item.get("ownerSubjectType") == "AGENT"
            and item.get("targetSubjectType") in ("HUMAN", "AGENT")
            and item.get("ownerSubjectType") != "USER"
            and item.get("targetSubjectType") != "USER"
            for item in agent_owner_contacts
        ),
    )
    agent_received_requests = api_data(http_json(
        "GET",
        query_path("/api/v1/friend-requests/received", {
            "recipientSubjectId": owned_agent.get("agentSubjectId"),
            "recipientSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "friend requests agent received",
        isinstance(agent_received_requests, list)
        and all(
            item.get("recipientSubjectId") == owned_agent.get("agentSubjectId")
            and item.get("recipientSubjectType") == "AGENT"
            and item.get("requesterSubjectType") in ("HUMAN", "AGENT")
            and item.get("recipientSubjectType") != "USER"
            and item.get("requesterSubjectType") != "USER"
            for item in agent_received_requests
        ),
    )
    agent_sent_requests = api_data(http_json(
        "GET",
        query_path("/api/v1/friend-requests/sent", {
            "requesterSubjectId": owned_agent.get("agentSubjectId"),
            "requesterSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "friend requests agent sent",
        isinstance(agent_sent_requests, list)
        and all(
            item.get("requesterSubjectId") == owned_agent.get("agentSubjectId")
            and item.get("requesterSubjectType") == "AGENT"
            and item.get("recipientSubjectType") in ("HUMAN", "AGENT")
            and item.get("requesterSubjectType") != "USER"
            and item.get("recipientSubjectType") != "USER"
            for item in agent_sent_requests
        ),
    )

    agent_notifications = api_data(http_json(
        "GET",
        query_path("/api/v1/notifications", {
            "limit": 5,
            "recipientSubjectId": owned_agent.get("agentSubjectId"),
            "recipientSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "notifications agent recipient",
        isinstance(agent_notifications, list)
        and all(
            item.get("recipientSubjectType") == "AGENT"
            and item.get("recipientSubjectType") != "USER"
            and item.get("senderSubjectType") != "USER"
            for item in agent_notifications
        ),
    )

    human_project_viewer = {
        "viewerSubjectId": principal_human_id,
        "viewerSubjectType": "HUMAN",
    }
    projects = api_data(http_json(
        "GET",
        query_path("/api/v1/projects", human_project_viewer),
        token,
    ))
    first_project = projects[0] if isinstance(projects, list) and projects else {}
    project_id = first_project.get("id")
    expect(
        "projects list",
        project_id is not None
        and first_project.get("ownerSubjectId") is not None
        and subject_type(first_project.get("ownerSubjectType"))
        and first_project.get("walletRouting") is not None
        and first_project.get("responsibilityChain") is not None,
    )

    project = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{project_id}", human_project_viewer),
        token,
    ))
    expect(
        "project detail",
        project.get("id") is not None
        and project.get("ownerSubjectId") is not None
        and subject_type(project.get("ownerSubjectType"))
        and any_match(
            project.get("members"),
            lambda item: item.get("memberSubjectId") is not None
            and subject_type(item.get("memberSubjectType")),
        ),
    )

    tasks = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{project_id}/sidebar/tasks", human_project_viewer),
        token,
    ))
    expect(
        "project tasks",
        all_match(
            tasks,
            lambda item: item.get("assigneeSubjectId") is not None
            and subject_type(item.get("assigneeSubjectType")),
        ),
    )

    payments = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{project_id}/sidebar/payments", human_project_viewer),
        token,
    ))
    expect(
        "project payments",
        all_match(
            payments,
            lambda item: item.get("recipientSubjectId") is not None
            and subject_type(item.get("recipientSubjectType"))
            and item.get("walletRouting") is not None
            and item.get("directRecipientSubjectId") is not None
            and subject_type(item.get("directRecipientSubjectType"))
            and item.get("settlementSubjectId") is not None
            and subject_type(item.get("settlementSubjectType"))
            and isinstance(item.get("financialAutonomy"), bool)
            and item.get("liableHumanId") is not None
            and item.get("liabilityRoute") is not None,
        ),
    )

    files = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{project_id}/sidebar/files", human_project_viewer),
        token,
    ))
    expect(
        "project files",
        all_match(
            files,
            lambda item: item.get("uploaderSubjectId") is not None
            and subject_type(item.get("uploaderSubjectType")),
        ),
    )

    agent_subject_id = owned_agent.get("agentSubjectId")
    project_name = f"{SMOKE_TAG} agent project {int(time.time())}"
    agent_project = api_data(http_json("POST", "/api/v1/projects", token, {
        "name": project_name,
        "bid": 125,
        "ownerSubjectId": agent_subject_id,
        "ownerSubjectType": "AGENT",
    }))
    agent_project_id = agent_project.get("id")
    expect(
        "project agent create",
        agent_project_id is not None
        and agent_project.get("ownerSubjectId") == agent_subject_id
        and agent_project.get("ownerSubjectType") == "AGENT",
    )

    agent_projects = api_data(http_json(
        "GET",
        query_path("/api/v1/projects", {
            "viewerSubjectId": agent_subject_id,
            "viewerSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "project agent viewer list",
        isinstance(agent_projects, list)
        and any(
            item.get("id") == agent_project_id
            and item.get("ownerSubjectId") == agent_subject_id
            and item.get("ownerSubjectType") == "AGENT"
            for item in agent_projects
        )
        and all(
            item.get("ownerSubjectType") != "USER"
            for item in agent_projects
        ),
    )

    human_projects = api_data(http_json(
        "GET",
        query_path("/api/v1/projects", {
            "viewerSubjectId": principal_human_id,
            "viewerSubjectType": "HUMAN",
        }),
        token,
    ))
    expect(
        "project human viewer isolated",
        isinstance(human_projects, list)
        and all(item.get("id") != agent_project_id for item in human_projects),
    )

    agent_project_detail = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{agent_project_id}", {
            "viewerSubjectId": agent_subject_id,
            "viewerSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "project agent detail",
        agent_project_detail.get("ownerSubjectId") == agent_subject_id
        and agent_project_detail.get("ownerSubjectType") == "AGENT"
        and any_match(
            agent_project_detail.get("members"),
            lambda item: item.get("memberSubjectId") == agent_subject_id
            and item.get("memberSubjectType") == "AGENT",
        ),
    )

    agent_share = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{agent_project_id}/share-link", {
            "viewerSubjectId": agent_subject_id,
            "viewerSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "project agent share link",
        isinstance(agent_share, dict)
        and isinstance(agent_share.get("url"), str)
        and str(agent_project_id) in agent_share.get("url"),
    )

    agent_sidebar_tasks = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{agent_project_id}/sidebar/tasks", {
            "viewerSubjectId": agent_subject_id,
            "viewerSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "project agent sidebar tasks",
        all_match(
            agent_sidebar_tasks,
            lambda item: item.get("assigneeSubjectId") is not None
            and subject_type(item.get("assigneeSubjectType"))
            and item.get("assigneeSubjectType") != "USER",
        ),
    )

    agent_sidebar_payments = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{agent_project_id}/sidebar/payments", {
            "viewerSubjectId": agent_subject_id,
            "viewerSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "project agent sidebar payments",
        all_match(
            agent_sidebar_payments,
            lambda item: item.get("recipientSubjectId") is not None
            and subject_type(item.get("recipientSubjectType"))
            and item.get("recipientSubjectType") != "USER"
            and item.get("directRecipientSubjectType") != "USER"
            and item.get("settlementSubjectType") != "USER",
        ),
    )

    agent_sidebar_files = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{agent_project_id}/sidebar/files", {
            "viewerSubjectId": agent_subject_id,
            "viewerSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "project agent sidebar files",
        all_match(
            agent_sidebar_files,
            lambda item: item.get("uploaderSubjectId") is not None
            and subject_type(item.get("uploaderSubjectType"))
            and item.get("uploaderSubjectType") != "USER",
        ),
    )

    updated_bid = 175
    http_json("POST", f"/api/v1/projects/{agent_project_id}/bid-update", token, {
        "newBid": updated_bid,
        "actorSubjectId": agent_subject_id,
        "actorSubjectType": "AGENT",
    })
    agent_project_after_bid = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{agent_project_id}", {
            "viewerSubjectId": agent_subject_id,
            "viewerSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "project agent actor bid",
        agent_project_after_bid.get("bid") == updated_bid
        and agent_project_after_bid.get("ownerSubjectId") == agent_subject_id
        and agent_project_after_bid.get("ownerSubjectType") == "AGENT",
    )

    expect_api_failure("project human actor bid blocked", "POST", f"/api/v1/projects/{agent_project_id}/bid-update", token, {
        "newBid": updated_bid + 1,
        "actorSubjectId": principal_human_id,
        "actorSubjectType": "HUMAN",
    })

    agent_task_title = f"{SMOKE_TAG} agent task {int(time.time())}"
    agent_task = api_data(http_json("POST", f"/api/v1/projects/{agent_project_id}/tasks", token, {
        "title": agent_task_title,
        "value": 100,
        "deadline": "2026-12-31",
        "priority": "medium",
        "assigneeSubjectId": agent_subject_id,
        "assigneeSubjectType": "AGENT",
        "actorSubjectId": agent_subject_id,
        "actorSubjectType": "AGENT",
    }))
    agent_task_id = agent_task.get("id")
    expect(
        "project agent actor task",
        agent_task_id is not None
        and agent_task.get("assigneeSubjectId") == agent_subject_id
        and agent_task.get("assigneeSubjectType") == "AGENT",
    )

    agent_sidebar_tasks_after_create = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{agent_project_id}/sidebar/tasks", {
            "viewerSubjectId": agent_subject_id,
            "viewerSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "project agent task visible",
        any_match(
            agent_sidebar_tasks_after_create,
            lambda item: item.get("id") == agent_task_id
            and item.get("assigneeSubjectId") == agent_subject_id
            and item.get("assigneeSubjectType") == "AGENT",
        ),
    )

    payment_amount = 222
    agent_payment = api_data(http_json("POST", f"/api/v1/projects/{agent_project_id}/payments", token, {
        "amount": payment_amount,
        "recipientSubjectId": agent_subject_id,
        "recipientSubjectType": "AGENT",
        "status": "PENDING",
        "date": "2026-12-31",
        "actorSubjectId": agent_subject_id,
        "actorSubjectType": "AGENT",
    }))
    agent_payment_id = agent_payment.get("id")
    expect(
        "project agent actor payment",
        agent_payment_id is not None
        and agent_payment.get("recipientSubjectId") == agent_subject_id
        and agent_payment.get("recipientSubjectType") == "AGENT"
        and agent_payment.get("walletRouting") == "AGENT_DIRECT"
        and agent_payment.get("directRecipientSubjectId") == agent_subject_id
        and agent_payment.get("directRecipientSubjectType") == "AGENT"
        and agent_payment.get("settlementSubjectId") == agent_subject_id
        and agent_payment.get("settlementSubjectType") == "AGENT"
        and agent_payment.get("walletPolicyState") == "ENABLED"
        and agent_payment.get("liableHumanId") == principal_human_id
        and agent_payment.get("liabilityRoute") is not None,
    )

    agent_sidebar_payments_after_create = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{agent_project_id}/sidebar/payments", {
            "viewerSubjectId": agent_subject_id,
            "viewerSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "project agent payment visible",
        any_match(
            agent_sidebar_payments_after_create,
            lambda item: item.get("id") == agent_payment_id
            and item.get("recipientSubjectId") == agent_subject_id
            and item.get("recipientSubjectType") == "AGENT"
            and item.get("walletRouting") == "AGENT_DIRECT",
        ),
    )

    expect_api_failure("project transfer member required", "POST", f"/api/v1/projects/{agent_project_id}/transfer", token, {
        "newOwnerSubjectId": principal_human_id,
        "newOwnerSubjectType": "HUMAN",
        "actorSubjectId": agent_subject_id,
        "actorSubjectType": "AGENT",
    })

    seed_transfer_project_id = 10002
    seed_transfer_detail = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{seed_transfer_project_id}", {
            "viewerSubjectId": agent_subject_id,
            "viewerSubjectType": "AGENT",
        }),
        token,
    ))
    if seed_transfer_detail.get("ownerSubjectType") == "HUMAN":
        http_json("POST", f"/api/v1/projects/{seed_transfer_project_id}/transfer", token, {
            "newOwnerSubjectId": agent_subject_id,
            "newOwnerSubjectType": "AGENT",
            "actorSubjectId": principal_human_id,
            "actorSubjectType": "HUMAN",
        })
        seed_transfer_detail = api_data(http_json(
            "GET",
            query_path(f"/api/v1/projects/{seed_transfer_project_id}", {
                "viewerSubjectId": agent_subject_id,
                "viewerSubjectType": "AGENT",
            }),
            token,
        ))
    expect(
        "project transfer seed ready",
        seed_transfer_detail.get("ownerSubjectId") == agent_subject_id
        and seed_transfer_detail.get("ownerSubjectType") == "AGENT"
        and any_match(
            seed_transfer_detail.get("members"),
            lambda item: item.get("memberSubjectId") == principal_human_id
            and item.get("memberSubjectType") == "HUMAN",
        ),
    )

    http_json("POST", f"/api/v1/projects/{seed_transfer_project_id}/transfer", token, {
        "newOwnerSubjectId": principal_human_id,
        "newOwnerSubjectType": "HUMAN",
        "actorSubjectId": agent_subject_id,
        "actorSubjectType": "AGENT",
    })
    transferred_project = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{seed_transfer_project_id}", {
            "viewerSubjectId": principal_human_id,
            "viewerSubjectType": "HUMAN",
        }),
        token,
    ))
    expect(
        "project agent transfer to human",
        transferred_project.get("ownerSubjectId") == principal_human_id
        and transferred_project.get("ownerSubjectType") == "HUMAN",
    )

    expect_api_failure("project old agent actor blocked", "POST", f"/api/v1/projects/{seed_transfer_project_id}/bid-update", token, {
        "newBid": updated_bid + 2,
        "actorSubjectId": agent_subject_id,
        "actorSubjectType": "AGENT",
    })

    human_owner_bid = updated_bid + 3
    http_json("POST", f"/api/v1/projects/{seed_transfer_project_id}/bid-update", token, {
        "newBid": human_owner_bid,
        "actorSubjectId": principal_human_id,
        "actorSubjectType": "HUMAN",
    })
    transferred_project_after_bid = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{seed_transfer_project_id}", {
            "viewerSubjectId": principal_human_id,
            "viewerSubjectType": "HUMAN",
        }),
        token,
    ))
    expect(
        "project human owner bid",
        transferred_project_after_bid.get("bid") == human_owner_bid
        and transferred_project_after_bid.get("ownerSubjectId") == principal_human_id
        and transferred_project_after_bid.get("ownerSubjectType") == "HUMAN",
    )

    http_json("POST", f"/api/v1/projects/{seed_transfer_project_id}/transfer", token, {
        "newOwnerSubjectId": agent_subject_id,
        "newOwnerSubjectType": "AGENT",
        "actorSubjectId": principal_human_id,
        "actorSubjectType": "HUMAN",
    })
    restored_seed_project = api_data(http_json(
        "GET",
        query_path(f"/api/v1/projects/{seed_transfer_project_id}", {
            "viewerSubjectId": agent_subject_id,
            "viewerSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "project transfer restored",
        restored_seed_project.get("ownerSubjectId") == agent_subject_id
        and restored_seed_project.get("ownerSubjectType") == "AGENT",
    )

    world_feed = api_data(http_json(
        "GET",
        f"/api/v1/world/posts?limit=5&viewerSubjectId={principal_human_id}&viewerSubjectType=HUMAN",
        token,
    ))
    expect(
        "world feed explicit viewer",
        any_match(world_feed.get("items"), lambda item: (item.get("author") or {}).get("type") == "agent"),
    )

    post = api_data(http_json("POST", "/api/v1/world/posts", token, {
        "content": f"{SMOKE_TAG} world post {int(time.time())}",
        "mediaType": "TEXT",
        "actorSubjectId": principal_human_id,
        "actorSubjectType": "HUMAN",
    }))
    post_id = post.get("id")
    expect("world create post", post_id is not None and (post.get("author") or {}).get("type") == "human")

    upvote = api_data(http_json(
        "POST",
        f"/api/v1/world/posts/{post_id}/upvote?actorSubjectId={principal_human_id}&actorSubjectType=HUMAN",
        token,
    ))
    expect("world upvote explicit actor", upvote.get("upvoted") is not None)

    reply = api_data(http_json("POST", f"/api/v1/world/posts/{post_id}/replies", token, {
        "content": f"{SMOKE_TAG} reply {int(time.time())}",
        "actorSubjectId": principal_human_id,
        "actorSubjectType": "HUMAN",
    }))
    expect("world reply", reply.get("replyCount") is not None)

    replies = api_data(http_json(
        "GET",
        f"/api/v1/world/posts/{post_id}/replies?viewerSubjectId={principal_human_id}&viewerSubjectType=HUMAN&limit=20",
        token,
    ))
    expect("world replies explicit viewer", isinstance(replies, list) and len(replies) >= 1)

    conversation = api_data(http_json("POST", "/api/v1/conversations", token, {
        "targetSubjectId": TARGET_SUBJECT_ID,
        "targetSubjectType": TARGET_SUBJECT_TYPE,
        "creatorSubjectId": principal_human_id,
        "creatorSubjectType": "HUMAN",
    }))
    conversation_id = conversation.get("id")
    expect("conversation create", conversation_id is not None and conversation.get("targetSubjectType") is not None)

    conversations = api_data(http_json(
        "GET",
        query_path("/api/v1/conversations", {
            "viewerSubjectId": principal_human_id,
            "viewerSubjectType": "HUMAN",
        }),
        token,
    ))
    expect("conversation list", isinstance(conversations, list) and len(conversations) >= 1)

    messages = api_data(http_json(
        "GET",
        query_path(f"/api/v1/conversations/{conversation_id}/messages", {
            "limit": 5,
            "viewerSubjectId": principal_human_id,
            "viewerSubjectType": "HUMAN",
        }),
        token,
    ))
    expect("conversation messages", isinstance(messages.get("items"), list))

    message = api_data(http_json("POST", f"/api/v1/conversations/{conversation_id}/messages", token, {
        "content": f"{SMOKE_TAG} message {int(time.time())}",
        "messageType": "TEXT",
        "actorSubjectId": principal_human_id,
        "actorSubjectType": "HUMAN",
    }))
    message_id = message.get("id")
    expect(
        "message send",
        message_id is not None
        and message.get("senderSubjectType") == "HUMAN"
        and message.get("liableHumanId") is not None,
    )

    http_json("POST", f"/api/v1/conversations/{conversation_id}/read", token, {
        "messageId": message_id,
        "readerSubjectId": principal_human_id,
        "readerSubjectType": "HUMAN",
    })
    expect("message read", True)

    agent_conversation = api_data(http_json("POST", "/api/v1/conversations", token, {
        "targetSubjectId": TARGET_SUBJECT_ID,
        "targetSubjectType": TARGET_SUBJECT_TYPE,
        "creatorSubjectId": agent_subject_id,
        "creatorSubjectType": "AGENT",
    }))
    agent_conversation_id = agent_conversation.get("id")
    expect(
        "conversation agent create",
        agent_conversation_id is not None
        and agent_conversation.get("targetSubjectType") is not None,
    )

    agent_conversations = api_data(http_json(
        "GET",
        query_path("/api/v1/conversations", {
            "viewerSubjectId": agent_subject_id,
            "viewerSubjectType": "AGENT",
        }),
        token,
    ))
    expect(
        "conversation agent list",
        isinstance(agent_conversations, list)
        and any(item.get("id") == agent_conversation_id for item in agent_conversations),
    )

    agent_messages = api_data(http_json(
        "GET",
        query_path(f"/api/v1/conversations/{agent_conversation_id}/messages", {
            "limit": 5,
            "viewerSubjectId": agent_subject_id,
            "viewerSubjectType": "AGENT",
        }),
        token,
    ))
    expect("conversation agent messages", isinstance(agent_messages.get("items"), list))

    agent_message = api_data(http_json("POST", f"/api/v1/conversations/{agent_conversation_id}/messages", token, {
        "content": f"{SMOKE_TAG} agent message {int(time.time())}",
        "messageType": "TEXT",
        "actorSubjectId": agent_subject_id,
        "actorSubjectType": "AGENT",
    }))
    agent_message_id = agent_message.get("id")
    expect(
        "message agent send",
        agent_message_id is not None
        and agent_message.get("senderSubjectId") == agent_subject_id
        and agent_message.get("senderSubjectType") == "AGENT"
        and agent_message.get("liableHumanId") == principal_human_id,
    )

    http_json("POST", f"/api/v1/conversations/{agent_conversation_id}/read", token, {
        "messageId": agent_message_id,
        "readerSubjectId": agent_subject_id,
        "readerSubjectType": "AGENT",
    })
    expect("message agent read", True)

    print("Actor baseline smoke passed.")


if __name__ == "__main__":
    main()
