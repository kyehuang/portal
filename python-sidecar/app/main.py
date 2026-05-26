from typing import Any

from fastapi import FastAPI
from pydantic import BaseModel, Field

from app.executor import execute_request

app = FastAPI()


class ExecuteRequest(BaseModel):
    task: str = ""
    params: dict[str, str] = Field(default_factory=dict)
    code: str = ""
    state: dict[str, str] = Field(default_factory=dict)
    files: dict[str, str] = Field(default_factory=dict)


class ExecuteResponse(BaseModel):
    task: str
    result: dict[str, Any] | None = None
    stdout: str = ""
    stderr: str = ""
    exitCode: int = 0
    durationMs: int = 0
    error: str | None = None


@app.post("/execute", response_model=ExecuteResponse)
def execute(request: ExecuteRequest) -> ExecuteResponse:
    return ExecuteResponse(**execute_request(
        task=request.task,
        params=request.params,
        code=request.code,
        state=request.state or request.files,
    ))
