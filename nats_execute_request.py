import argparse
import asyncio
import json
import os
import sys
from pathlib import Path
from typing import Any

import nats


DEFAULT_SCRIPT = """
print("hello from python nats client")
return {
    "message": "worker executed this script",
    "source": source,
}
""".strip()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Send an ExecuteRequest payload to a NATS subject.",
    )
    parser.add_argument(
        "--url",
        default=os.getenv("NATS_URL", "nats://localhost:4222"),
        help="NATS server URL. Default: %(default)s",
    )
    parser.add_argument(
        "--subject",
        default=os.getenv("NATS_SUBJECT", "portal.execute"),
        help="NATS subject. Default: %(default)s",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=float(os.getenv("NATS_TIMEOUT", "30")),
        help="Request timeout in seconds. Default: %(default)s",
    )
    parser.add_argument(
        "--script",
        default=None,
        help="Python script string to execute on the worker.",
    )
    parser.add_argument(
        "--script-file",
        type=Path,
        default=None,
        help="Path to a Python script file to execute on the worker.",
    )
    parser.add_argument(
        "--content-json",
        default='{"source": "python-nats-client"}',
        help="JSON object for ExecuteRequest.content. Default: %(default)s",
    )
    parser.add_argument(
        "--publish-only",
        action="store_true",
        help="Publish the message without waiting for a worker reply.",
    )
    return parser.parse_args()


def load_script(args: argparse.Namespace) -> str:
    if args.script_file is not None:
        return args.script_file.read_text(encoding="utf-8")
    if args.script is not None:
        return args.script
    return DEFAULT_SCRIPT


def load_content(content_json: str) -> dict[str, Any]:
    try:
        content = json.loads(content_json)
    except json.JSONDecodeError as error:
        raise ValueError(f"invalid --content-json: {error}") from error

    if not isinstance(content, dict):
        raise ValueError("--content-json must decode to a JSON object")

    return content


async def main() -> int:
    args = parse_args()

    try:
        payload = {
            "script": load_script(args),
            "content": load_content(args.content_json),
        }
    except ValueError as error:
        print(error, file=sys.stderr)
        return 2

    nc = await nats.connect(args.url)
    try:
        data = json.dumps(payload).encode("utf-8")

        if args.publish_only:
            await nc.publish(args.subject, data)
            await nc.flush()
            print(f"published to {args.subject}")
            return 0

        response = await nc.request(args.subject, data, timeout=args.timeout)
        print(json.dumps(json.loads(response.data.decode("utf-8")), indent=2))
        return 0
    finally:
        await nc.drain()


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main()))
