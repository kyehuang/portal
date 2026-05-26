# Python Sidecar

Runs Python work through `POST /execute`.

Supported modes:

- `task + params`: runs a system-defined allowlisted task.
- `code + state`: runs experimental restricted Python code.

The restricted code mode is for experiments, not a complete security sandbox.

## Run locally

```bash
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8001
```

## Example

```bash
curl -s http://localhost:8001/execute \
  -H 'Content-Type: application/json' \
  -d '{"task":"csv_column","params":{"path":"data/input.csv","column":"name"}}'
```

## Restricted Code Example

```bash
curl -s http://localhost:8001/execute \
  -H 'Content-Type: application/json' \
  -d '{"code":"import numpy as np\nprint(np.array([1, 2, 3]).mean())"}'
```

Expected stdout:

```text
2.0
```

## Allowed Packages

Installed packages are managed in:

```text
requirements.txt
```

Imports allowed inside runner code are managed separately in:

```text
allowed_packages.json
```

Example:

```json
{
  "imports": ["numpy", "pandas"]
}
```

When adding a package, update both files and rebuild the sidecar image.

## Pandas Example

```bash
curl -s http://localhost:8001/execute \
  -H 'Content-Type: application/json' \
  -d '{"code":"import pandas as pd\nprint(pd.Series([1, 2, 3]).mean())"}'
```

## State Map Example

Small text values can be passed as a JSON object. The runner writes them into a temporary working directory before executing code, so each state key behaves like a relative file path.

```bash
curl -s http://localhost:8001/execute \
  -H 'Content-Type: application/json' \
  -d '{"code":"import numpy as np\nprint(np.loadtxt(\"numbers.csv\", delimiter=\",\" ).mean())","state":{"numbers.csv":"1,2,3\n4,5,6\n"}}'
```

Use this only for small text state. For large files or binary data, use a mounted volume, object storage, or multipart upload.
