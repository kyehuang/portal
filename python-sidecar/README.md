# Python Sidecar

Runs Python scripts through `POST /execute`.

The sidecar accepts only `script` and `content`. It executes the script directly in the FastAPI process and automatically prepends:

```python
import json
import numpy as np
```

The `content` object is injected as Python globals before `execute()` is called.

## Run Locally

```bash
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8001
```

## Example

```bash
curl -s http://localhost:8001/execute \
  -H 'Content-Type: application/json' \
  -d '{"script":"scores = np.array(employee_scores)\naverage = float(scores.mean())\npassed = [name for name, score in zip(employee_names, employee_scores) if score >= pass_score]\nprint(f\"average={average}\")\nreturn {\"average\": average, \"passedCount\": len(passed), \"passedEmployees\": passed}","content":{"employee_names":["Amy","Ben","Cara","Dylan"],"employee_scores":[82,64,91,73],"pass_score":75}}'
```

Expected response:

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

## Execution Shape

The sidecar inserts `script` at the `<script>` position inside `execute()`:

```python
import json
import numpy as np

def execute():
    <script>

__result__ = execute()
```

For example, this request field:

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

If `execute()` returns a non-null value, it is returned as:

```json
{
  "result": {
    "value": 100
  }
}
```
