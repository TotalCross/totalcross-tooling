#!/usr/bin/env python3
"""Check staged text files against the unified ExecPlan size policy."""

# Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import PurePosixPath


MAX_BYTES = 20_480
MAX_LINES = 600
PROTECTED_PREFIXES = (
    "TotalCrossSDK/src/main/java/tc/tools/converter/",
    "TotalCrossSDK/src/test/java/tc/tools/converter/",
    "TotalCrossVM/src/tcvm/ir/",
    "TotalCrossVM/src/tcvm/jit/",
    "TotalCrossVM/src/tcvm/aot/",
    "TotalCrossVM/src/tests/ir/",
    "docs/architecture/bytecode/",
)
SKIPPED_DIRECTORIES = {
    ".git",
    ".gradle",
    ".cxx",
    "__pycache__",
    "build",
    "dist",
    "generated",
    "node_modules",
    "target",
    "vendor",
}


def is_protected(path: str) -> bool:
    return any(path.startswith(prefix) for prefix in PROTECTED_PREFIXES)


def is_skipped(path: str) -> bool:
    parts = PurePosixPath(path).parts
    if any(part in SKIPPED_DIRECTORIES for part in parts):
        return True
    name = parts[-1] if parts else ""
    return ".generated." in name or name.startswith("generated-")


def is_text_bytes(data: bytes) -> bool:
    return b"\0" not in data[:8192]


def _git(repo: str, *args: str) -> bytes:
    return subprocess.check_output(("git", "-C", repo, *args))


def staged_paths(repo: str) -> list[str]:
    raw = _git(
        repo,
        "diff",
        "--cached",
        "--name-status",
        "--find-renames",
        "--find-copies",
        "--diff-filter=ACMR",
        "-z",
    )
    fields = raw.split(b"\0")
    paths: list[str] = []
    index = 0
    while index < len(fields) - 1:
        status = fields[index].decode("utf-8")
        index += 1
        if not status:
            continue
        if index >= len(fields):
            break
        path = fields[index].decode("utf-8")
        index += 1
        if status[0] in {"R", "C"}:
            if index >= len(fields):
                break
            path = fields[index].decode("utf-8")
            index += 1
        paths.append(path)
    return paths


def staged_content(repo: str, path: str) -> bytes:
    return _git(repo, "show", f":{path}")


def check_staged(repo: str) -> tuple[list[str], list[str]]:
    warnings: list[str] = []
    failures: list[str] = []
    checked = 0
    for path in staged_paths(repo):
        if is_skipped(path):
            continue
        try:
            data = staged_content(repo, path)
        except subprocess.CalledProcessError:
            continue
        if not is_text_bytes(data):
            continue
        checked += 1
        if is_protected(path):
            warnings.append(f"protected path exempted: {path}")
            continue
        line_count = data.count(b"\n") + (1 if data and not data.endswith(b"\n") else 0)
        if len(data) > MAX_BYTES:
            failures.append(f"{path}: {len(data)} bytes exceeds {MAX_BYTES}")
        if line_count > MAX_LINES:
            failures.append(f"{path}: {line_count} lines exceeds {MAX_LINES}")
    return failures, warnings


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", required=True, help="repository to inspect")
    parser.add_argument("--staged", action="store_true", help="check staged files")
    args = parser.parse_args(argv)
    if not args.staged:
        parser.error("--staged is required")
    failures, warnings = check_staged(args.repo)
    for warning in warnings:
        print(f"WARNING: {warning}")
    if failures:
        for failure in failures:
            print(f"FAIL: {failure}", file=sys.stderr)
        return 1
    print(f"OK: staged text files comply with the size policy")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
