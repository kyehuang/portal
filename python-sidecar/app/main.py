import contextlib
import io
import textwrap
import time
import traceback
from typing import Any

import numpy as np
from fastapi import FastAPI
from pydantic import BaseModel, Field

app = FastAPI()
DEFAULT_IMPORTS = "import json\nimport numpy as np\n"
MAX_OUTPUT_CHARS = 64 * 1024


class ExecuteRequest(BaseModel):
    script: str = ""
    content: dict[str, Any] = Field(default_factory=dict)


class ExecuteResponse(BaseModel):
    result: dict[str, Any] | None = None
    stdout: str = ""
    stderr: str = ""
    exitCode: int = 0
    durationMs: int = 0
    error: str | None = None


@app.post("/execute", response_model=ExecuteResponse)
def execute(request: ExecuteRequest) -> ExecuteResponse:
    started = time.monotonic()
    if not request.script.strip():
        return ExecuteResponse(
            exitCode=2,
            durationMs=duration_ms(started),
            error="script is required",
        )

    globals_dict = dict(request.content)
    stdout = io.StringIO()
    stderr = io.StringIO()

    try:
        with contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
            exec(build_code(request.script), globals_dict)
    except Exception:
        traceback.print_exc(file=stderr)
        return ExecuteResponse(
            stdout=limit(stdout.getvalue()),
            stderr=limit(stderr.getvalue()),
            exitCode=1,
            durationMs=duration_ms(started),
            error="script failed",
        )

    return ExecuteResponse(
        result=build_result(globals_dict.get("__result__")),
        stdout=limit(stdout.getvalue()),
        stderr=limit(stderr.getvalue()),
        exitCode=0,
        durationMs=duration_ms(started),
        error=None,
    )


def duration_ms(started: float) -> int:
    return int((time.monotonic() - started) * 1000)


def build_code(script: str) -> str:
    body = textwrap.indent(script, "    ")
    return f"{DEFAULT_IMPORTS}\ndef execute():\n{body}\n\n__result__ = execute()\n"


def build_result(value: Any) -> dict[str, Any] | None:
    if value is None:
        return None
    return {"value": normalize_result(value)}


def normalize_result(value: Any) -> Any:
    if isinstance(value, np.ndarray):
        return value.tolist()
    if isinstance(value, np.generic):
        return value.item()
    if isinstance(value, dict):
        return {key: normalize_result(item) for key, item in value.items()}
    if isinstance(value, list):
        return [normalize_result(item) for item in value]
    if isinstance(value, tuple):
        return [normalize_result(item) for item in value]
    return value


def limit(value: str) -> str:
    return value[:MAX_OUTPUT_CHARS]
