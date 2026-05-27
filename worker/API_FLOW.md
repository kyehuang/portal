# Worker API Flow

## Overview

The worker does not execute Python directly.

It receives `POST /execute`, validates that `script` is present, then forwards the same request to the Python sidecar through HTTP.

```text
Client
  -> worker: POST http://localhost:8081/execute
  -> ExecuteResource
  -> PythonSidecarClient
  -> python-sidecar: POST http://localhost:8001/execute
  -> FastAPI app/main.py
  -> exec generated Python code in the FastAPI process
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

Validation:

```kotlin
if (request.script.isBlank()) {
    throw WebApplicationException("script is required", Response.Status.BAD_REQUEST)
}
```

Forwarding:

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

## Request Models

File:

```text
worker/src/main/kotlin/com/portal/worker/ExecuteModels.kt
```

Request:

```kotlin
data class ExecuteRequest(
    val script: String = "",
    val content: Map<String, Any?> = emptyMap(),
)
```

Response:

```kotlin
data class ExecuteResponse(
    val result: Map<String, Any?>? = null,
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int = 0,
    val durationMs: Long = 0,
    val error: String? = null,
)
```

## Python Sidecar

File:

```text
python-sidecar/app/main.py
```

Endpoint:

```http
POST http://localhost:8001/execute
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

The sidecar injects `content` top-level keys as Python globals, then inserts `script` at the `<script>` position:

```python
import json
import numpy as np

def execute():
    <script>

__result__ = execute()
```

Expected response:

```json
{
  "result": null,
  "stdout": "Start\n",
  "stderr": "",
  "exitCode": 0,
  "durationMs": 1,
  "error": null
}
```

If the script returns a non-null value, it is returned in `result.value`.

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
  -d '{"script":"print(CurrentContent[\"Worker\"])","content":{"CurrentContent":{"Worker":"Start"}}}'
```

## Current Tradeoffs

The sidecar currently executes request scripts directly in the FastAPI process.

There is no subprocess boundary, no subprocess timeout, and no AST allowlist. If scripts can be slow, blocking, or untrusted, add runtime isolation before relying on this beyond local/internal use.
