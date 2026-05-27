# TODO

## Worker 目標

目前 worker 不直接執行 Python。

worker 收到 `POST /execute` 後，只做基本 request 驗證，接著透過 localhost HTTP 呼叫 python-sidecar 的 FastAPI `/execute`。

python-sidecar 目前集中在 `python-sidecar/app/main.py`，收到 request 後直接在 FastAPI process 內執行 script，不再啟動 subprocess。

## Request

`/execute` 目前只接受 `script` 與 `content`：

```json
{
  "script": "print(CurrentContent[\"Worker\"])",
  "content": {
    "CurrentContent": {"Worker": "Start"}
  }
}
```

`content` 是一個 map。python-sidecar 會把 top-level key 注入成 Python globals，所以 script 可以直接讀：

```python
print(CurrentContent["Worker"])
```

## Python 執行模板

python-sidecar 會自動補上 `json` 與 `numpy` import，並把 request 的 `script` 放在 `<script>` 位置。使用者不需要自己寫 import，也不要自己包 `def execute()`：

```python
import json
import numpy as np

def execute():
    <script>

__result__ = execute()
```

如果 script 使用 `return` 回傳非 null 值，response 會放在 `result.value`。

## 注意事項

- 目前沒有 AST 限制。
- 目前沒有 subprocess timeout。
- script 會在 FastAPI process 內直接 `exec`。
- `content` 只當資料注入，不會當 code 執行。
- stdout / stderr 會回傳在 response 中。
- response stdout / stderr 目前各自限制為 64 KiB 字元長度。
- 如果 script 會長時間執行或可能卡住，需要後續再補 timeout / worker isolation。
