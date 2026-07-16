#!/usr/bin/env python3
# Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
# SPDX-License-Identifier: Apache-2.0
"""Apply reviewed provenance headers without changing excluded material."""

from __future__ import annotations

import argparse
import json
from pathlib import Path


def comment(path: Path, lines: list[str], spdx: str) -> str:
    content = lines + [f"SPDX-License-Identifier: {spdx}"]
    name = path.name
    suffix = path.suffix.lower()
    if suffix == ".md":
        return "<!--\n" + "\n".join(f"{line}" for line in content) + "\n-->\n\n"
    if suffix == ".xml":
        return "<!--\n" + "\n".join(f"  {line}" for line in content) + "\n-->\n"
    if suffix in {".java", ".ts", ".gradle"} or suffix == ".template" and ("java" in path.name or "gradle" in path.name):
        return "/*\n" + "\n".join(f" * {line}" for line in content) + "\n */\n\n"
    if suffix == ".bat":
        return "\r\n".join(f"REM {line}" for line in content) + "\r\n\r\n"
    return "\n".join(f"# {line}" for line in content) + "\n\n"


def remove_existing_header(text: str, offset: int) -> str:
    rest = text[offset:]
    end = None
    if rest.startswith("/*"):
        end = rest.find("*/") + 2
    elif rest.startswith("<!--"):
        end = rest.find("-->") + 3
    elif rest.startswith("#"):
        lines = rest.splitlines(keepends=True)
        position = 0
        for line in lines:
            if line.startswith("#") or not line.strip():
                position += len(line)
            else:
                break
        end = position
    elif rest.startswith("REM "):
        lines = rest.splitlines(keepends=True)
        end = sum(len(line) for line in lines if line.startswith("REM ") or not line.strip())
    if end and "SPDX-License-Identifier:" in rest[:end]:
        return text[:offset] + rest[end:].lstrip("\r\n")
    return text


def apply(root: Path, project: str) -> int:
    records = json.loads((root / "migration/license-provenance.json").read_text())["files"]
    changed = 0
    for record in records:
        if record["project"] != project or record["excluded"]:
            continue
        path = root / record["final_path"]
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8")
        offset = 0
        if text.startswith("#!") or text.startswith("<?xml"):
            offset = text.find("\n") + 1
        text = remove_existing_header(text, offset)
        offset = 0
        if text.startswith("#!") or text.startswith("<?xml"):
            offset = text.find("\n") + 1
        header = comment(path, record["expected_copyright_lines"], record["expected_license"])
        updated = text[:offset] + header + text[offset:]
        if updated != text:
            path.write_text(updated, encoding="utf-8", newline="")
            changed += 1
    print(f"applied headers to {changed} {project} file(s)")
    return 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--project", required=True)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    args = parser.parse_args()
    raise SystemExit(apply(args.root.resolve(), args.project))
