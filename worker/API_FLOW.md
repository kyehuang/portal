# Worker API Flow

## Overview

The worker does not execute Python directly.

It receives an HTTP request, then calls the Python sidecar through HTTP. The sidecar decides how to run Python and returns the result to the worker.

```text
Client
  -> worker: POST http://localhost:8081/execute
  -> ExecuteResource
  -> PythonSidecarClient
  -> python-sidecar: POST http://localhost:8001/execute
  -> FastAPI app/main.py
  -> app/executor.py
  -> Python subprocess
  -> response back to client
```

## Worker Entry Point

File:

```text
worker/src/main/kotlin/com/portal/worker/ExecuteResource.kt
```

Endpoint:

```http
POST http://localhost:8081/execute
```

The worker validates that either `task` or `code` is present:

```kotlin
if (request.task.isBlank() && request.code.isBlank()) {
    throw WebApplicationException("task or code is required", Response.Status.BAD_REQUEST)
}
```

Then it forwards the request to the Python sidecar:

```kotlin
return pythonSidecarClient.execute(request)
```

## Python Sidecar Client

File:

```text
worker/src/main/kotlin/com/portal/worker/PythonSidecarClient.kt
```

The client is a Quarkus REST Client:

```kotlin
@RegisterRestClient(configKey = "python-sidecar")
interface PythonSidecarClient {
    @POST
    @Path("/execute")
    fun execute(request: ExecuteRequest): ExecuteResponse
}
```

The base URL comes from:

```text
worker/src/main/resources/application.properties
```

```properties
quarkus.rest-client.python-sidecar.url=http://localhost:8001
```

So this Kotlin call:

```kotlin
pythonSidecarClient.execute(request)
```

becomes:

```http
POST http://localhost:8001/execute
```

## Request Models

File:

```text
worker/src/main/kotlin/com/portal/worker/ExecuteModels.kt
```

Request:

```kotlin
data class ExecuteRequest(
    val task: String = "",
    val params: Map<String, String> = emptyMap(),
    val code: String = "",
    val state: Map<String, String> = emptyMap(),
)
```

Response:

```kotlin
data class ExecuteResponse(
    val task: String = "",
    val result: Map<String, Any?>? = null,
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int = 0,
    val durationMs: Long = 0,
    val error: String? = null,
)
```

## Sidecar Entry Point

File:

```text
python-sidecar/app/main.py
```

Endpoint:

```http
POST http://localhost:8001/execute
```

The FastAPI handler receives the request and passes it to `execute_request(...)`:

```python
return ExecuteResponse(**execute_request(
    task=request.task,
    params=request.params,
    code=request.code,
    state=request.state,
))
```

## Sidecar Dispatch

File:

```text
python-sidecar/app/executor.py
```

Dispatch logic:

```text
if code is present:
  execute_restricted_code(...)
else if task is present:
  execute_task(...)
else:
  return error
```

## Mode 1: Restricted Code Runner

Request:

```json
{
  "code": "import numpy as np\nprint(np.array([1, 2, 3]).mean())"
}
```

Flow:

```text
executor.py
  -> execute_restricted_code(...)
  -> subprocess runs python-sidecar/scripts/restricted_runner.py
```

The runner:

```text
restricted_runner.py
  -> parses code with AST
  -> allows imports listed in python-sidecar/allowed_packages.json
  -> rejects open/eval/exec/__import__ and dunder access
  -> writes state map entries into a temporary directory
  -> executes code
  -> returns stdout/stderr/exitCode
```

Allowed imports are configured in:

```text
python-sidecar/allowed_packages.json
```

Example:

```json
{
  "imports": ["numpy", "pandas"]
}
```

Installed packages are configured in:

```text
python-sidecar/requirements.txt
```

When adding a package, update both `requirements.txt` and `allowed_packages.json`, then rebuild the sidecar image.

Expected response shape:

```json
{
  "task": "numpy_code",
  "result": null,
  "stdout": "2.0\n",
  "stderr": "",
  "exitCode": 0,
  "durationMs": 42,
  "error": null
}
```

## Passing State To Runner

Small text state can be passed through the `state` map:

```json
{
  "code": "import numpy as np\nprint(np.loadtxt(\"numbers.csv\", delimiter=\",\").mean())",
  "state": {
    "numbers.csv": "1,2,3\n4,5,6\n"
  }
}
```

The sidecar writes each state entry into a temporary working directory before running the code. The state key is treated as a relative file path.

This is only suitable for small text state. For large files or binary data, use a mounted volume, object storage, or multipart upload instead.

## Mode 2: Fixed Task Runner

Request:

```json
{
  "task": "csv_column",
  "params": {
    "path": "data/input.csv",
    "column": "name"
  }
}
```

Flow:

```text
executor.py
  -> execute_task(...)
  -> task allowlist maps csv_column to scripts/csv_column.py
  -> subprocess runs csv_column.py
```

Task allowlist:

```python
TASKS = {
    "csv_column": SCRIPT_DIR / "csv_column.py",
}
```

## Run Commands

Start the Python sidecar:

```bash
docker compose up --build python-sidecar
```

Start the worker:

```bash
mvn quarkus:dev -pl worker
```

Call the worker:

```bash
curl -s http://localhost:8081/execute \
  -H 'Content-Type: application/json' \
  -d '{"code":"import numpy as np\nprint(np.array([1, 2, 3]).mean())"}'
```

## Security Note

The numpy code runner is a restricted experimental runner, not a complete security sandbox.

Do not rely on import filtering alone for strong isolation. Run the sidecar in a constrained container with CPU, memory, network, filesystem, and privilege limits.
