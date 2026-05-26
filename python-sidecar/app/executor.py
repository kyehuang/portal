from __future__ import annotations

import json
import subprocess
import sys
import time
from pathlib import Path
from typing import Any


BASE_DIR = Path(__file__).resolve().parents[1]
SCRIPT_DIR = BASE_DIR / "scripts"
MAX_OUTPUT_BYTES = 64 * 1024
TASKS = {
    "csv_column": SCRIPT_DIR / "csv_column.py",
}
RESTRICTED_RUNNER = SCRIPT_DIR / "restricted_runner.py"


def execute_request(
    task: str,
    params: dict[str, str],
    code: str = "",
    state: dict[str, str] | None = None,
) -> dict[str, Any]:
    if code.strip():
        return execute_restricted_code(code=code, state=state or {})
    if task.strip():
        return execute_task(task=task, params=params)
    return {
        "task": "",
        "result": None,
        "stdout": "",
        "stderr": "",
        "exitCode": 2,
        "durationMs": 0,
        "error": "task or code is required",
    }


def execute_restricted_code(code: str, state: dict[str, str]) -> dict[str, Any]:
    started = time.monotonic()
    payload = json.dumps({
        "code": code,
        "state": state,
    })
    try:
        completed = subprocess.run(
            [sys.executable, str(RESTRICTED_RUNNER)],
            input=payload,
            capture_output=True,
            check=False,
            encoding="utf-8",
            timeout=10,
        )
    except subprocess.TimeoutExpired as exc:
        return {
            "task": "restricted_code",
            "result": None,
            "stdout": _limit(exc.stdout or ""),
            "stderr": _limit(exc.stderr or ""),
            "exitCode": 124,
            "durationMs": _duration_ms(started),
            "error": "task timed out",
        }

    stdout = _limit(completed.stdout)
    stderr = _limit(completed.stderr)
    return {
        "task": "restricted_code",
        "result": None,
        "stdout": stdout,
        "stderr": stderr,
        "exitCode": completed.returncode,
        "durationMs": _duration_ms(started),
        "error": None if completed.returncode == 0 else (stderr or f"task failed with exit code {completed.returncode}"),
    }


def execute_task(task: str, params: dict[str, str]) -> dict[str, Any]:
    started = time.monotonic()
    script_path = TASKS.get(task)
    if script_path is None:
        return {
            "task": task,
            "result": None,
            "stdout": "",
            "stderr": "",
            "exitCode": 2,
            "durationMs": _duration_ms(started),
            "error": f"unsupported task: {task}",
        }

    payload = json.dumps(params)
    try:
        completed = subprocess.run(
            [sys.executable, str(script_path)],
            input=payload,
            capture_output=True,
            check=False,
            encoding="utf-8",
            timeout=10,
        )
    except subprocess.TimeoutExpired as exc:
        return {
            "task": task,
            "result": None,
            "stdout": _limit(exc.stdout or ""),
            "stderr": _limit(exc.stderr or ""),
            "exitCode": 124,
            "durationMs": _duration_ms(started),
            "error": "task timed out",
        }

    stdout = _limit(completed.stdout)
    stderr = _limit(completed.stderr)
    result = None
    error = None

    if completed.returncode == 0:
        try:
            result = json.loads(stdout or "{}")
            stdout = ""
        except json.JSONDecodeError:
            error = "task returned invalid json"
    else:
        error = stderr or f"task failed with exit code {completed.returncode}"

    return {
        "task": task,
        "result": result,
        "stdout": stdout,
        "stderr": stderr,
        "exitCode": completed.returncode,
        "durationMs": _duration_ms(started),
        "error": error,
    }


def _duration_ms(started: float) -> int:
    return int((time.monotonic() - started) * 1000)


def _limit(value: str) -> str:
    encoded = value.encode("utf-8")
    if len(encoded) <= MAX_OUTPUT_BYTES:
        return value
    return encoded[:MAX_OUTPUT_BYTES].decode("utf-8", errors="replace")
