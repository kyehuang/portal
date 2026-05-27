# PythonSidecarClient Usage

## Purpose

`PythonSidecarClient` is the worker-side REST client for calling the Python sidecar directly.

Use it when Kotlin code needs to execute a small Python script through `python-sidecar`.

The worker does not execute Python locally. It sends the request to:

```http
POST http://localhost:8001/execute
```

The URL is configured in:

```properties
quarkus.rest-client.python-sidecar.url=http://localhost:8001
```

## Inject The Client

File:

```text
worker/src/main/kotlin/com/portal/worker/PythonSidecarClient.kt
```

Inject it with `@RestClient`:

```kotlin
import org.eclipse.microprofile.rest.client.inject.RestClient

class ExampleService(
    @param:RestClient private val pythonSidecarClient: PythonSidecarClient,
) {
    fun run(): ExecuteResponse {
        return pythonSidecarClient.execute(
            ExecuteRequest(
                script = """
                    scores = np.array(employee_scores)
                    average = float(scores.mean())
                    passed = [
                        name
                        for name, score in zip(employee_names, employee_scores)
                        if score >= pass_score
                    ]
                    print(f"average={average}")
                    return {
                        "average": average,
                        "passedCount": len(passed),
                        "passedEmployees": passed,
                    }
                """.trimIndent(),
                content = mapOf(
                    "employee_names" to listOf("Amy", "Ben", "Cara", "Dylan"),
                    "employee_scores" to listOf(82, 64, 91, 73),
                    "pass_score" to 75,
                ),
            ),
        )
    }
}
```

## Request Body

`ExecuteRequest` has two fields:

```kotlin
data class ExecuteRequest(
    val script: String = "",
    val content: Map<String, Any?> = emptyMap(),
)
```

Example HTTP body:

```json
{
  "script": "scores = np.array(employee_scores)\naverage = float(scores.mean())\npassed = [name for name, score in zip(employee_names, employee_scores) if score >= pass_score]\nprint(f'average={average}')\nreturn {\n    'average': average,\n    'passedCount': len(passed),\n    'passedEmployees': passed\n}",
  "content": {
    "employee_names": ["Amy", "Ben", "Cara", "Dylan"],
    "employee_scores": [82, 64, 91, 73],
    "pass_score": 75
  }
}
```

## Script Position

The `script` value is inserted at the `<script>` position inside the generated Python `execute()` function body.

Generated wrapper:

```python
import json
import numpy as np

def execute():
    <script>

__result__ = execute()
```

So this request field:

```json
{
  "script": "print(\"hello\")\nreturn 100"
}
```

becomes:

```python
import json
import numpy as np

def execute():
    print("hello")
    return 100

__result__ = execute()
```

Users should write only the body of the function:

```python
scores = np.array(employee_scores)
average = float(scores.mean())
passed = [name for name, score in zip(employee_names, employee_scores) if score >= pass_score]
print(f"average={average}")
return {
    "average": average,
    "passedCount": len(passed),
    "passedEmployees": passed,
}
```

Do not wrap it with `def execute():`.

The sidecar automatically imports:

```python
import json
import numpy as np
```

So users do not need to include those imports in `script`.

## Content Usage

`content` is injected into Python globals before the script runs.

Top-level keys become Python variables:

```json
{
  "content": {
    "employee_names": ["Amy", "Ben", "Cara", "Dylan"],
    "employee_scores": [82, 64, 91, 73],
    "pass_score": 75
  }
}
```

The script can read:

```python
scores = np.array(employee_scores)
return {
    "average": float(scores.mean()),
    "passScore": pass_score,
}
```

If a key should be used directly as a Python variable, it must be a valid Python variable name.

Use:

```json
{
  "employee_scores": [82, 64, 91, 73]
}
```

Avoid:

```json
{
  "employee-scores": [82, 64, 91, 73]
}
```

because `employee-scores` is not a valid Python variable name.

## Generated Python Code

For this request:

```json
{
  "script": "scores = np.array(employee_scores)\naverage = float(scores.mean())\npassed = [name for name, score in zip(employee_names, employee_scores) if score >= pass_score]\nprint(f'average={average}')\nreturn {\n    'average': average,\n    'passedCount': len(passed),\n    'passedEmployees': passed\n}",
  "content": {
    "employee_names": ["Amy", "Ben", "Cara", "Dylan"],
    "employee_scores": [82, 64, 91, 73],
    "pass_score": 75
  }
}
```

The sidecar builds and executes:

```python
import json
import numpy as np

def execute():
    scores = np.array(employee_scores)
    average = float(scores.mean())
    passed = [name for name, score in zip(employee_names, employee_scores) if score >= pass_score]
    print(f'average={average}')
    return {
        'average': average,
        'passedCount': len(passed),
        'passedEmployees': passed
    }

__result__ = execute()
```

## Response

`print()` output is returned in `stdout`.

The value returned by `return` is returned in `result.value`.

Example response:

```json
{
  "result": {
    "value": {
      "average": 77.5,
      "passedCount": 2,
      "passedEmployees": ["Amy", "Cara"]
    }
  },
  "stdout": "average=77.5\n",
  "stderr": "",
  "exitCode": 0,
  "durationMs": 1,
  "error": null
}
```

If the script does not return anything, `result` is `null`.

If the script raises an exception, `exitCode` is `1`, `stderr` contains the traceback, and `error` is `"script failed"`.

## Notes

- The sidecar currently executes scripts directly in the FastAPI process.
- There is no subprocess timeout.
- There is no AST validation or sandbox.
- `stdout` and `stderr` are limited to 64 KiB characters before being returned.
- Return values should be JSON-compatible. Numpy arrays and numpy scalar values are converted before returning.
