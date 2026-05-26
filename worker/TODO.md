# TODO

## Worker 目標

第一版 worker 不接收使用者輸入的 Python code，也不做通用 Python sandbox。

目前目標是：worker 透過 localhost HTTP 呼叫 Python sidecar，由 sidecar 執行固定的 Python script。

Python script 內容不一定是讀檔，第一個 use case 先做「讀取 CSV 並取得指定欄位」。

## Quarkus + Python Sidecar

### 架構

```text
worker module
  -> POST http://localhost:8001/execute
  -> 傳入 task 名稱與 task 參數
  -> 接收 stdout / stderr / exitCode / result / error

python sidecar
  -> FastAPI
  -> 根據 task 執行系統內建的固定 Python script
  -> 用 subprocess 執行時加 timeout
```

### Worker 呼叫範例

```kotlin
val result = httpClient.post("http://localhost:8001/execute")
    .body(ExecuteRequest(code = code, state = state))
    .execute()
```

### API 草案

```http
POST http://localhost:8001/execute
```

Request:

```json
{
  "task": "csv_column",
  "params": {
    "path": "/data/input.csv",
    "column": "name"
  }
}
```

Response:

```json
{
  "task": "csv_column",
  "result": {
    "columns": ["id", "name"],
    "rowCount": 100,
    "column": "name",
    "values": ["Alice", "Bob"]
  },
  "stdout": "",
  "stderr": "",
  "exitCode": 0,
  "durationMs": 42,
  "error": null
}
```

## 注意事項

- 固定 Python 任務，不接受任意 Python code。
- `/execute` 只接受 task 名稱與參數，不接受 script/code 字串。
- sidecar 需要 allowlist task，例如 `csv_column` 對應固定的 Python script。
- 欄位名稱由 worker 傳入；sidecar 只用它選取 CSV column，不把它當 Python code 執行。
- 如果欄位不存在，要回傳明確錯誤。
- subprocess 仍需要 timeout。
- stdout / stderr 或 response size 要有限制。
- CSV 檔案來源先用固定 volume 或固定路徑。
- 後續如果需要更多 Python 任務，再擴充成 `/jobs/...` 類型 API。
目標：worker 不直接執行 Python，而是透過 localhost HTTP 呼叫 python sidecar；sidecar 每次請求用
subprocess 執行 script，並提供 timeout、stdout/stderr、exitCode 與錯誤結果。

## 實驗：restricted code runner

研究用 `/execute` 也支援傳入 `code` 與小型文字 `state` map：

```json
{
  "code": "import numpy as np\nprint(np.array([1, 2, 3]).mean())",
  "state": {}
}
```

或：

```json
{
  "code": "import numpy as np\nprint(np.loadtxt(\"numbers.csv\", delimiter=\",\").mean())",
  "state": {
    "numbers.csv": "1,2,3\n4,5,6\n"
  }
}
```

限制：

- 只允許 `import numpy` / `import numpy as np` / `from numpy import ...`。
- 允許 import 的套件由 `python-sidecar/allowed_packages.json` 管理。
- 已安裝的套件由 `python-sidecar/requirements.txt` 管理。
- 拒絕 `import os`、`import sys`、`import subprocess` 等其他 import。
- 拒絕 `open()`、`eval()`、`exec()`、`__import__()` 等危險呼叫。
- 拒絕 dunder name / dunder attribute。
- 這是受限 runner，不是完整安全 sandbox。
