# System Architecture

## High-Level Flow

```mermaid
flowchart LR
    Client[Client] -->|POST /execute| Worker[Quarkus Worker<br/>localhost:8081]
    Worker -->|REST Client<br/>POST /execute| Sidecar[Python Sidecar<br/>FastAPI localhost:8001]
    Sidecar -->|exec generated code| Runtime[FastAPI Python Process]
    Runtime -->|stdout/stderr/exitCode| Sidecar
    Sidecar -->|ExecuteResponse| Worker
    Worker -->|ExecuteResponse| Client
```

## Runtime Boundary

```mermaid
flowchart TB
    subgraph JVM["worker container / JVM"]
        ExecuteResource[ExecuteResource.kt<br/>POST /execute]
        SidecarClient[PythonSidecarClient.kt<br/>Quarkus REST Client]
        ExecuteResource --> SidecarClient
    end

    subgraph PY["python-sidecar container"]
        FastAPI[FastAPI app/main.py<br/>POST /execute]
        Template[Build Python code<br/>imports + execute wrapper]
        Exec[exec in FastAPI process]
        Requirements[requirements.txt<br/>fastapi, numpy, uvicorn]

        FastAPI --> Template
        Template --> Exec
        Exec --> Requirements
    end

    SidecarClient -->|HTTP localhost:8001| FastAPI
```

## Execute Request

```mermaid
flowchart TD
    Request[ExecuteRequest] --> HasScript{script present?}
    HasScript -->|no| Error[400 / script is required]
    HasScript -->|yes| Forward[Worker forwards to sidecar]
    Forward --> Inject[Inject content as globals]
    Inject --> Build[Build imports + execute wrapper]
    Build --> Run[exec generated code]
    Run --> Response[stdout / stderr / exitCode / durationMs]
```

Request:

```json
{
  "script": "print(CurrentContent[\"Worker\"])",
  "content": {
    "CurrentContent": {"Worker": "Start"}
  }
}
```

Generated Python shape. The request `script` is inserted at `<script>`:

```python
import json
import numpy as np

def execute():
    <script>

__result__ = execute()
```

## Current Limits

The Python sidecar is the risky runtime boundary. The current implementation executes script directly in the FastAPI process.

Current behavior:

- No subprocess boundary.
- No subprocess timeout.
- No AST validation.
- No import allowlist.
- `json` and `numpy` are imported automatically.
- `content` top-level keys become Python globals.
- stdout and stderr are limited before being returned.

Recommended Docker Compose settings still matter if this is used outside local development:

```yaml
services:
  python-sidecar:
    mem_limit: 256m
    cpus: "0.5"
    pids_limit: 64
    read_only: true
    tmpfs:
      - /tmp
    security_opt:
      - no-new-privileges:true
    cap_drop:
      - ALL
```

## Important Note

This is not a secure Python sandbox.

If scripts are untrusted, long-running, or allowed to perform expensive work, add an isolation boundary such as a subprocess, worker process, container job, timeout, or queue before production use.
