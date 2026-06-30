#!/usr/bin/env python3
"""Start and stop the local EqoChat backend/frontend dev services."""

from __future__ import annotations

import argparse
import json
import os
import shutil
import signal
import socket
import subprocess
import sys
import time
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
BACKEND_DIR = ROOT / "backend"
FRONTEND_DIR = ROOT / "frontend"
STATE_DIR = ROOT / ".workbuddy" / "dev-services"
LOG_DIR = STATE_DIR / "logs"
BACKEND_JAR = BACKEND_DIR / "eqochat-server" / "target" / "eqochat-server-1.0.0.jar"
COMPOSE_FILE = ROOT / "docker-compose.dev.yml"
MYSQL_CONTAINER = "eqochat-mysql"
REDIS_CONTAINER = "eqochat-redis"
IS_WINDOWS = os.name == "nt"


def main() -> int:
    refresh_windows_environment()

    parser = argparse.ArgumentParser(
        description="Start/stop EqoChat backend and frontend for local manual testing."
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    start_parser = subparsers.add_parser("start", help="Start one or both services")
    add_service_arg(start_parser)
    start_parser.add_argument(
        "--no-build",
        action="store_true",
        help="Do not run Maven package before starting the backend.",
    )
    start_parser.add_argument(
        "--backend-profile",
        default="local",
        help="Spring profile for the backend. Default: local.",
    )
    start_parser.add_argument(
        "--frontend-script",
        default="dev:h5",
        help="npm script for the frontend. Default: dev:h5.",
    )
    start_parser.add_argument(
        "--env",
        action="append",
        default=[],
        metavar="KEY=VALUE",
        help="Extra environment variable for started services. Repeatable.",
    )

    stop_parser = subparsers.add_parser("stop", help="Stop one or both services")
    add_service_arg(stop_parser)

    restart_parser = subparsers.add_parser("restart", help="Restart one or both services")
    add_service_arg(restart_parser)
    restart_parser.add_argument("--no-build", action="store_true")
    restart_parser.add_argument("--backend-profile", default="local")
    restart_parser.add_argument("--frontend-script", default="dev:h5")
    restart_parser.add_argument("--env", action="append", default=[], metavar="KEY=VALUE")

    status_parser = subparsers.add_parser("status", help="Show service status")
    add_service_arg(status_parser)

    logs_parser = subparsers.add_parser("logs", help="Print log file paths")
    add_service_arg(logs_parser)

    args = parser.parse_args()
    ensure_dirs()

    if args.command == "start":
        return start_services(args.service, args)
    if args.command == "stop":
        return stop_services(args.service)
    if args.command == "restart":
        stop_code = stop_services(args.service)
        start_code = start_services(args.service, args)
        return stop_code or start_code
    if args.command == "status":
        return status_services(args.service)
    if args.command == "logs":
        return print_log_paths(args.service)

    parser.error(f"unknown command: {args.command}")
    return 2


def add_service_arg(parser: argparse.ArgumentParser) -> None:
    parser.add_argument(
        "service",
        nargs="?",
        default="all",
        choices=("mysql", "redis", "backend", "frontend", "all"),
        help="Service to operate on. Default: all.",
    )


def ensure_dirs() -> None:
    STATE_DIR.mkdir(parents=True, exist_ok=True)
    LOG_DIR.mkdir(parents=True, exist_ok=True)


def start_services(service: str, args: argparse.Namespace) -> int:
    env = build_env(args.env)
    code = 0
    if service in ("mysql", "all"):
        code = start_mysql(env) or code
    if service in ("redis", "all"):
        code = start_redis(env) or code
    if service in ("backend", "all"):
        code = start_backend(args, env) or code
    if service in ("frontend", "all"):
        code = start_frontend(args, env) or code
    return code


def stop_services(service: str) -> int:
    code = 0
    if service in ("frontend", "all"):
        code = stop_service("frontend") or code
    if service in ("backend", "all"):
        code = stop_service("backend") or code
    if service in ("redis", "all"):
        code = stop_compose_service("redis", REDIS_CONTAINER) or code
    if service in ("mysql", "all"):
        code = stop_compose_service("mysql", MYSQL_CONTAINER) or code
    return code


def status_services(service: str) -> int:
    for name in service_names(service):
        if name == "mysql":
            status_compose_service("mysql", MYSQL_CONTAINER, mysql_endpoints(os.environ))
            continue
        if name == "redis":
            status_compose_service("redis", REDIS_CONTAINER, redis_endpoints(os.environ))
            continue
        state = read_state(name)
        if not state:
            print(f"{name}: stopped")
            continue
        pid = state.get("pid")
        running = bool(pid and is_running(pid))
        if running:
            print(f"{name}: running pid={pid} log={state.get('log')}")
        else:
            print(f"{name}: stopped stale_pid={pid} log={state.get('log')}")
        print_endpoints(name, state.get("endpoints") or default_endpoints(name, os.environ))
    return 0


def print_log_paths(service: str) -> int:
    for name in service_names(service):
        if name == "mysql":
            print("mysql: docker logs eqochat-mysql")
        elif name == "redis":
            print("redis: docker logs eqochat-redis")
        else:
            print(f"{name}: {LOG_DIR / (name + '.log')}")
    return 0


def service_names(service: str) -> tuple[str, ...]:
    if service == "all":
        return ("mysql", "redis", "backend", "frontend")
    return (service,)


def build_env(extra_pairs: list[str]) -> dict[str, str]:
    env = os.environ.copy()
    for pair in extra_pairs:
        if "=" not in pair:
            raise SystemExit(f"--env must be KEY=VALUE, got: {pair}")
        key, value = pair.split("=", 1)
        if not key:
            raise SystemExit(f"--env key cannot be empty: {pair}")
        env[key] = value
    return env


def start_backend(args: argparse.Namespace, env: dict[str, str]) -> int:
    if already_running("backend"):
        return 0

    if not args.no_build:
        mvn = require_executable("mvn")
        print("backend: packaging eqochat-server...")
        result = subprocess.run(
            [mvn, "-pl", "eqochat-server", "-am", "-DskipTests", "package"],
            cwd=BACKEND_DIR,
            env=env,
        )
        if result.returncode != 0:
            print("backend: Maven package failed", file=sys.stderr)
            return result.returncode

    if not BACKEND_JAR.exists():
        print(f"backend: jar not found: {BACKEND_JAR}", file=sys.stderr)
        print("backend: run without --no-build, or package the backend first.", file=sys.stderr)
        return 1

    java = require_executable("java")
    cmd = [
        java,
        "-jar",
        str(BACKEND_JAR),
        f"--spring.profiles.active={args.backend_profile}",
    ]
    return spawn_service("backend", cmd, ROOT, env, backend_endpoints(env))


def start_mysql(env: dict[str, str]) -> int:
    return start_compose_service("mysql", MYSQL_CONTAINER, mysql_endpoints(env), env)


def start_redis(env: dict[str, str]) -> int:
    return start_compose_service("redis", REDIS_CONTAINER, redis_endpoints(env), env)


def start_compose_service(
    service: str,
    container: str,
    endpoints: list[dict[str, str]],
    env: dict[str, str],
) -> int:
    docker = require_executable("docker")
    result = subprocess.run(
        [docker, "compose", "-f", str(COMPOSE_FILE), "up", "-d", service],
        cwd=ROOT,
        env=env,
    )
    if result.returncode != 0:
        print(f"{service}: docker compose up failed", file=sys.stderr)
        return result.returncode

    print(f"{service}: waiting for healthy container...")
    deadline = time.time() + 180
    while time.time() < deadline:
        status = compose_health_status(container, env)
        if status == "healthy":
            print(f"{service}: running container={container}")
            print_endpoints(service, endpoints)
            return 0
        if status in ("exited", "dead"):
            print(f"{service}: container stopped while starting status={status}", file=sys.stderr)
            return 1
        time.sleep(3)

    print(f"{service}: timed out waiting for healthy container", file=sys.stderr)
    status_compose_service(service, container, endpoints)
    return 1


def start_frontend(args: argparse.Namespace, env: dict[str, str]) -> int:
    if already_running("frontend"):
        return 0
    npm = require_executable("npm")
    node = require_executable("node")
    ensure_frontend_dependencies()
    env = ensure_path_contains(env, Path(node).parent)
    env = ensure_path_contains(env, Path(npm).parent)
    cmd = [npm, "run", args.frontend_script]
    return spawn_service("frontend", cmd, FRONTEND_DIR, env, frontend_endpoints(env))


def ensure_frontend_dependencies() -> None:
    uni_cmd = FRONTEND_DIR / "node_modules" / ".bin" / ("uni.cmd" if IS_WINDOWS else "uni")
    if uni_cmd.exists():
        return

    print(f"frontend: local uni CLI not found: {uni_cmd}", file=sys.stderr)
    print("frontend: install dependencies first:", file=sys.stderr)
    print("frontend:   cd frontend", file=sys.stderr)
    print("frontend:   npm install", file=sys.stderr)
    raise SystemExit(1)


def ensure_path_contains(env: dict[str, str], directory: Path) -> dict[str, str]:
    path_key = "Path" if IS_WINDOWS else "PATH"
    path_value = env.get(path_key) or env.get("PATH") or ""
    path_parts = [part for part in path_value.split(os.pathsep) if part]
    directory_text = str(directory)

    if IS_WINDOWS:
        exists = any(part.lower() == directory_text.lower() for part in path_parts)
    else:
        exists = directory_text in path_parts

    if not exists:
        env = env.copy()
        env[path_key] = os.pathsep.join([directory_text, *path_parts])
    return env


def already_running(name: str) -> bool:
    state = read_state(name)
    if not state:
        return False
    pid = state.get("pid")
    if pid and is_running(pid):
        print(f"{name}: already running pid={pid}")
        print_endpoints(name, state.get("endpoints") or default_endpoints(name, os.environ))
        return True
    remove_state(name)
    return False


def spawn_service(
    name: str,
    cmd: list[str],
    cwd: Path,
    env: dict[str, str],
    endpoints: list[dict[str, str]],
) -> int:
    log_path = LOG_DIR / f"{name}.log"
    log_file = log_path.open("ab")
    log_file.write(f"\n\n=== {timestamp()} starting {' '.join(cmd)} ===\n".encode("utf-8"))
    log_file.flush()

    creationflags = 0
    start_new_session = False
    if IS_WINDOWS:
        creationflags = subprocess.CREATE_NEW_PROCESS_GROUP
    else:
        start_new_session = True

    process = subprocess.Popen(
        cmd,
        cwd=cwd,
        env=env,
        stdout=log_file,
        stderr=subprocess.STDOUT,
        stdin=subprocess.DEVNULL,
        creationflags=creationflags,
        start_new_session=start_new_session,
    )
    log_file.close()

    write_state(
        name,
        {
            "pid": process.pid,
            "command": cmd,
            "cwd": str(cwd),
            "log": str(log_path),
            "endpoints": endpoints,
            "started_at": timestamp(),
        },
    )
    print(f"{name}: started pid={process.pid}")
    print(f"{name}: log={log_path}")
    print_endpoints(name, endpoints)
    return 0


def stop_service(name: str) -> int:
    state = read_state(name)
    if not state:
        print(f"{name}: already stopped")
        return 0

    pid = state.get("pid")
    if not pid or not is_running(pid):
        remove_state(name)
        print(f"{name}: already stopped")
        return 0

    print(f"{name}: stopping pid={pid}")
    if IS_WINDOWS:
        result = subprocess.run(
            ["taskkill", "/PID", str(pid), "/T", "/F"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        remove_state(name)
        return 0 if result.returncode in (0, 128) else result.returncode

    try:
        os.killpg(pid, signal.SIGTERM)
    except ProcessLookupError:
        remove_state(name)
        return 0

    deadline = time.time() + 10
    while time.time() < deadline:
        if not is_running(pid):
            remove_state(name)
            return 0
        time.sleep(0.2)

    try:
        os.killpg(pid, signal.SIGKILL)
    except ProcessLookupError:
        pass
    remove_state(name)
    return 0


def stop_compose_service(service: str, container: str) -> int:
    docker = require_executable("docker")
    result = subprocess.run(
        [docker, "compose", "-f", str(COMPOSE_FILE), "stop", service],
        cwd=ROOT,
    )
    if result.returncode == 0:
        print(f"{service}: stopped")
    return result.returncode


def status_compose_service(
    service: str,
    container: str,
    endpoints: list[dict[str, str]],
) -> None:
    status = compose_health_status(container, os.environ)
    if not status:
        print(f"{service}: stopped")
    else:
        print(f"{service}: {status} container={container}")
        print_endpoints(service, endpoints)


def require_executable(name: str) -> str:
    path = shutil.which(name)
    if not path:
        raise SystemExit(f"missing executable on PATH: {name}")
    return path


def backend_endpoints(env: dict[str, str]) -> list[dict[str, str]]:
    port = env.get("SERVER_PORT") or "8080"
    return [{"label": "Backend API", "url": f"http://localhost:{port}"}]


def mysql_endpoints(env: dict[str, str]) -> list[dict[str, str]]:
    host = env.get("MYSQL_HOST") or "127.0.0.1"
    port = env.get("MYSQL_PORT") or "3306"
    database = env.get("MYSQL_DATABASE") or "eqochat2"
    return [
        {"label": "MySQL JDBC", "url": f"jdbc:mysql://{host}:{port}/{database}"},
        {"label": "MySQL CLI", "url": f"mysql -h {host} -P {port} -u root {database}"},
    ]


def redis_endpoints(env: dict[str, str]) -> list[dict[str, str]]:
    host = env.get("REDIS_HOST") or "127.0.0.1"
    port = env.get("REDIS_PORT") or "6379"
    database = env.get("REDIS_DATABASE") or "5"
    return [{"label": "Redis", "url": f"redis://{host}:{port}/{database}"}]


def frontend_endpoints(env: dict[str, str]) -> list[dict[str, str]]:
    vite_env = load_frontend_env("development")
    merged = {**vite_env, **env}
    port = merged.get("VITE_APP_PORT") or "3000"
    endpoints = [{"label": "Frontend H5", "url": f"http://localhost:{port}"}]
    lan_ip = local_lan_ip()
    if lan_ip:
        endpoints.append({"label": "Frontend H5 LAN", "url": f"http://{lan_ip}:{port}"})
    return endpoints


def default_endpoints(name: str, env: dict[str, str]) -> list[dict[str, str]]:
    if name == "mysql":
        return mysql_endpoints(env)
    if name == "redis":
        return redis_endpoints(env)
    if name == "backend":
        return backend_endpoints(env)
    if name == "frontend":
        return frontend_endpoints(env)
    return []


def print_endpoints(name: str, endpoints: object) -> None:
    if not isinstance(endpoints, list) or not endpoints:
        return

    for endpoint in endpoints:
        if not isinstance(endpoint, dict):
            continue
        label = endpoint.get("label") or f"{name} endpoint"
        url = endpoint.get("url")
        if url:
            print(f"{name}: {label}: {url}")


def load_frontend_env(mode: str) -> dict[str, str]:
    env_dir = FRONTEND_DIR / "env"
    merged: dict[str, str] = {}
    for filename in (".env", ".env.local", f".env.{mode}", f".env.{mode}.local"):
        merged.update(read_env_file(env_dir / filename))
    return merged


def read_env_file(path: Path) -> dict[str, str]:
    if not path.exists():
        return {}

    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8", errors="ignore").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        if key:
            values[key] = value
    return values


def local_lan_ip() -> str | None:
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
            sock.connect(("8.8.8.8", 80))
            ip = sock.getsockname()[0]
            return None if ip.startswith("127.") else ip
    except OSError:
        return None


def compose_health_status(container: str, env: dict[str, str]) -> str | None:
    docker = shutil.which("docker")
    if not docker:
        return None

    result = subprocess.run(
        [
            docker,
            "inspect",
            "-f",
            "{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}",
            container,
        ],
        cwd=ROOT,
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
        text=True,
    )
    if result.returncode != 0:
        return None
    return result.stdout.strip() or None


def refresh_windows_environment() -> None:
    if not IS_WINDOWS:
        return

    try:
        import winreg
    except ImportError:
        return

    machine_env = read_windows_environment(
        winreg.HKEY_LOCAL_MACHINE,
        r"SYSTEM\CurrentControlSet\Control\Session Manager\Environment",
    )
    user_env = read_windows_environment(winreg.HKEY_CURRENT_USER, "Environment")

    path_parts = []
    for value in (machine_env.get("Path"), user_env.get("Path")):
        if value:
            path_parts.append(os.path.expandvars(value))
    if path_parts:
        os.environ["PATH"] = ";".join(path_parts)

    for key in ("JAVA_HOME", "MAVEN_HOME"):
        value = user_env.get(key) or machine_env.get(key)
        if value:
            os.environ[key] = os.path.expandvars(value)


def read_windows_environment(root: int, subkey: str) -> dict[str, str]:
    try:
        import winreg

        with winreg.OpenKey(root, subkey) as key:
            values: dict[str, str] = {}
            index = 0
            while True:
                try:
                    name, value, _ = winreg.EnumValue(key, index)
                except OSError:
                    break
                if isinstance(value, str):
                    values[name] = value
                index += 1
            return values
    except OSError:
        return {}


def is_running(pid: int) -> bool:
    if IS_WINDOWS:
        result = subprocess.run(
            ["tasklist", "/FI", f"PID eq {int(pid)}", "/NH"],
            stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL,
            text=True,
        )
        return result.returncode == 0 and str(pid) in result.stdout

    try:
        os.kill(int(pid), 0)
        return True
    except ProcessLookupError:
        return False
    except PermissionError:
        return True
    except OSError:
        return False


def state_path(name: str) -> Path:
    return STATE_DIR / f"{name}.json"


def read_state(name: str) -> dict[str, object] | None:
    path = state_path(name)
    if not path.exists():
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return None


def write_state(name: str, state: dict[str, object]) -> None:
    state_path(name).write_text(json.dumps(state, indent=2), encoding="utf-8")


def remove_state(name: str) -> None:
    try:
        state_path(name).unlink()
    except FileNotFoundError:
        pass


def timestamp() -> str:
    return time.strftime("%Y-%m-%d %H:%M:%S")


if __name__ == "__main__":
    raise SystemExit(main())
