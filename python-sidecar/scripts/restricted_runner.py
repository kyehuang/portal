from __future__ import annotations

import ast
import contextlib
import io
import json
import os
import sys
import tempfile
from pathlib import Path
from typing import Any


BASE_DIR = Path(__file__).resolve().parents[1]
ALLOWED_PACKAGES_FILE = BASE_DIR / "allowed_packages.json"
MAX_CODE_CHARS = 20_000
MAX_FILE_COUNT = 20
MAX_FILE_BYTES = 256 * 1024
FORBIDDEN_CALLS = {
    "__import__",
    "compile",
    "eval",
    "exec",
    "input",
    "open",
}
SAFE_BUILTINS = {
    "abs": abs,
    "all": all,
    "any": any,
    "bool": bool,
    "dict": dict,
    "enumerate": enumerate,
    "float": float,
    "int": int,
    "len": len,
    "list": list,
    "max": max,
    "min": min,
    "print": print,
    "range": range,
    "round": round,
    "set": set,
    "str": str,
    "sum": sum,
    "tuple": tuple,
    "zip": zip,
}


def main() -> int:
    payload = json.load(sys.stdin)
    code = payload.get("code", "")
    state = payload.get("state") or payload.get("files", {})
    allowed_imports = load_allowed_imports()

    if not isinstance(code, str) or not code.strip():
        return fail("code is required")
    if len(code) > MAX_CODE_CHARS:
        return fail("code is too large")
    if not isinstance(state, dict):
        return fail("state must be an object")

    validation_error = validate_code(code, allowed_imports)
    if validation_error:
        return fail(validation_error)

    with tempfile.TemporaryDirectory(prefix="restricted-runner-") as work_dir:
        work_path = Path(work_dir)
        file_error = write_state_files(work_path, state)
        if file_error:
            return fail(file_error)

        os.chdir(work_path)
        output = io.StringIO()
        globals_dict: dict[str, Any] = {
            "__builtins__": {
                **SAFE_BUILTINS,
                "__import__": build_import_allowlist(allowed_imports),
            },
        }

        try:
            compiled = compile(code, "<restricted-runner>", "exec")
            with contextlib.redirect_stdout(output):
                exec(compiled, globals_dict, {})
        except Exception as exc:
            return fail(f"{exc.__class__.__name__}: {exc}")

    print(output.getvalue(), end="")
    return 0


def load_allowed_imports() -> set[str]:
    with ALLOWED_PACKAGES_FILE.open(encoding="utf-8") as config_file:
        config = json.load(config_file)
    imports = config.get("imports", [])
    if not isinstance(imports, list) or not all(isinstance(item, str) for item in imports):
        raise ValueError("allowed_packages.json imports must be a string array")
    return set(imports)


def validate_code(code: str, allowed_imports: set[str]) -> str | None:
    try:
        tree = ast.parse(code)
    except SyntaxError as exc:
        return f"syntax error: {exc}"

    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                if alias.name.split(".", 1)[0] not in allowed_imports:
                    return f"import is not allowed: {alias.name}"
        elif isinstance(node, ast.ImportFrom):
            module = node.module or ""
            if module.split(".", 1)[0] not in allowed_imports:
                return f"import is not allowed: {module}"
        elif isinstance(node, ast.Name):
            if node.id.startswith("__"):
                return f"dunder name is not allowed: {node.id}"
            if node.id in FORBIDDEN_CALLS:
                return f"call is not allowed: {node.id}"
        elif isinstance(node, ast.Attribute):
            if node.attr.startswith("__"):
                return f"dunder attribute is not allowed: {node.attr}"
        elif isinstance(node, ast.Call):
            func = node.func
            if isinstance(func, ast.Name) and func.id in FORBIDDEN_CALLS:
                return f"call is not allowed: {func.id}"

    return None


def write_state_files(work_path: Path, state: dict[str, Any]) -> str | None:
    if len(state) > MAX_FILE_COUNT:
        return "too many state entries"

    for raw_name, content in state.items():
        if not isinstance(raw_name, str) or not raw_name:
            return "state key must be a non-empty string"
        if not isinstance(content, str):
            return f"state value must be a string: {raw_name}"
        if len(content.encode("utf-8")) > MAX_FILE_BYTES:
            return f"state value is too large: {raw_name}"

        relative_path = Path(raw_name)
        if relative_path.is_absolute() or ".." in relative_path.parts:
            return f"invalid file path: {raw_name}"

        target = work_path / relative_path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")

    return None


def build_import_allowlist(allowed_imports: set[str]):
    def import_allowlist(name: str, globals=None, locals=None, fromlist=(), level=0):
        top_level = name.split(".", 1)[0]
        if level != 0 or top_level not in allowed_imports:
            raise ImportError(f"import is not allowed: {name}")
        return __import__(name, globals, locals, fromlist, level)

    return import_allowlist


def fail(message: str) -> int:
    print(message, file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
