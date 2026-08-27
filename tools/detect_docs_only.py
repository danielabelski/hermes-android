#!/usr/bin/env python3
"""Report whether the current Git diff contains Markdown files only."""

from __future__ import annotations

import argparse
import os
import subprocess
import sys
from collections.abc import Iterable


def classify_paths(paths: Iterable[str]) -> bool:
    paths = list(paths)
    return bool(paths) and all(path.lower().endswith(".md") for path in paths)


def changed_paths(base: str, head: str, before: str, sha: str) -> list[str] | None:
    if base and head:
        revision_range = f"{base}...{head}"
    elif before and sha:
        revision_range = f"{before}...{sha}"
    else:
        return None

    if "0" * 40 in revision_range:
        return None

    try:
        result = subprocess.run(
            ["git", "diff", "--name-only", "--no-renames", revision_range],
            capture_output=True,
            text=True,
            check=False,
        )
    except OSError:
        print("Could not run git diff - running the full suite.", file=sys.stderr)
        return None
    if result.returncode != 0:
        print(f"Could not diff {revision_range} - running the full suite.", file=sys.stderr)
        return None
    return result.stdout.splitlines()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default=os.environ.get("BASE_SHA", ""))
    parser.add_argument("--head", default=os.environ.get("HEAD_SHA", ""))
    parser.add_argument("--before", default=os.environ.get("BEFORE_SHA", ""))
    parser.add_argument("--sha", default=os.environ.get("SHA", ""))
    args = parser.parse_args(argv)

    paths = changed_paths(args.base, args.head, args.before, args.sha)
    if paths is None:
        print("false")
        return 0

    if not paths:
        print("Empty diff - running the full suite.", file=sys.stderr)
        print("false")
        return 0

    print("true" if classify_paths(paths) else "false")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())