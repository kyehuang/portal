# System Architecture

## High-Level Flow

```mermaid
flowchart LR
    Client[Client] -->|POST /execute| Worker[Quarkus Worker<br/>localhost:8081]
    Worker -->|REST Client<br/>POST /execute| Sidecar[Python Sidecar<br/>FastAPI localhost:8001]
    Sidecar -->|subprocess + timeout| Runner[Restricted Python Runner]
    Runner -->|AST validation| Policy[Import Allowlist<br/>allowed_packages.json]
    Runner -->|writes small text state| TempDir[Temporary Work Dir]
    Runner -->|imports installed packages| Packages[Python Packages<br/>requirements.txt]
    Runner -->|stdout/stderr/exitCode| Sidecar
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
        Executor[executor.py<br/>dispatch code or task]
        RestrictedRunner[restricted_runner.py<br/>restricted code mode]
        TaskRunner[csv_column.py<br/>fixed task mode]
        Allowlist[allowed_packages.json<br/>numpy, pandas]
        Requirements[requirements.txt<br/>installed packages]
        Temp[Temp directory<br/>state map entries]

        FastAPI --> Executor
        Executor -->|code present| RestrictedRunner
        Executor -->|task present| TaskRunner
        RestrictedRunner --> Allowlist
        RestrictedRunner --> Requirements
        RestrictedRunner --> Temp
    end

    SidecarClient -->|HTTP localhost:8001| FastAPI
```

## Execute Request Modes

```mermaid
flowchart TD
    Request[ExecuteRequest] --> HasCode{code present?}
    HasCode -->|yes| Restricted[restricted code mode]
    HasCode -->|no| HasTask{task present?}
    HasTask -->|yes| Task[fixed task mode]
    HasTask -->|no| Error[400 / task or code is required]

    Restricted --> Validate[AST validation]
    Validate --> Imports[Allow imports from allowed_packages.json only]
    Imports --> RunCode[Run code in subprocess]
    RunCode --> CodeResponse[stdout / stderr / exitCode]

    Task --> Lookup[Task allowlist lookup]
    Lookup --> RunTask[Run fixed script in subprocess]
    RunTask --> TaskResponse[result / stdout / stderr / exitCode]
```

## State Map

`state` is a small text map sent with code execution:

```json
{
  "code": "import numpy as np\nprint(np.loadtxt(\"numbers.csv\", delimiter=\",\").mean())",
  "state": {
    "numbers.csv": "1,2,3\n4,5,6\n"
  }
}
```

The runner writes each `state` entry into a temporary working directory before executing code.

Use `state` only for small text values. For large files or binary data, use a mounted volume, object storage, or multipart upload.

## Container Limits

The Python sidecar is the risky runtime boundary. CPU, memory, pids, filesystem, and privilege limits should be applied to the sidecar container.

```mermaid
flowchart TB
    subgraph Limits["python-sidecar container limits"]
        CPU[CPU limit]
        Memory[Memory limit]
        Pids[pids limit]
        ReadOnly[read_only filesystem]
        Tmpfs[tmpfs /tmp]
        NoPriv[no-new-privileges]
        CapDrop[cap_drop ALL]
    end

    Runner[Restricted Python Runner] --> Limits
```

Recommended Docker Compose settings:

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

This design is a restricted Python runner, not a complete secure sandbox.

The stronger isolation boundary should come from container or OS-level restrictions. AST checks and import allowlists are useful filters, but they should not be treated as the only security control.
