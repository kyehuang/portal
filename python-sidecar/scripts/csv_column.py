from __future__ import annotations

import csv
import json
import sys
from pathlib import Path


MAX_VALUES = 1000


def main() -> int:
    params = json.load(sys.stdin)
    csv_path = Path(params.get("path", ""))
    column = params.get("column", "")

    if not csv_path:
        return fail("path is required")
    if not column:
        return fail("column is required")
    if not csv_path.exists() or not csv_path.is_file():
        return fail(f"csv file not found: {csv_path}")

    with csv_path.open(newline="", encoding="utf-8-sig") as csv_file:
        reader = csv.DictReader(csv_file)
        columns = reader.fieldnames or []
        if column not in columns:
            return fail(f"column not found: {column}")

        values = []
        row_count = 0
        for row in reader:
            row_count += 1
            if len(values) < MAX_VALUES:
                values.append(row.get(column, ""))

    print(json.dumps({
        "columns": columns,
        "rowCount": row_count,
        "column": column,
        "values": values,
        "truncated": row_count > MAX_VALUES,
    }, ensure_ascii=False))
    return 0


def fail(message: str) -> int:
    print(message, file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
