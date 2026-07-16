#!/usr/bin/env python3
# Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
# SPDX-License-Identifier: Apache-2.0
"""Validate current-tree license, provenance, attribution, and contacts."""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path


def read_paths(root: Path) -> list[str]:
    result = subprocess.run(
        ["git", "-C", str(root), "ls-files", "-z"],
        check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
    )
    return sorted(path for path in result.stdout.decode("utf-8").split("\0") if path)


def header_offset(text: str) -> int:
    lines = text.splitlines()
    if lines and lines[0].startswith("#!"):
        return 1
    if lines and lines[0].startswith("<?xml"):
        return 1
    return 0


def validate_record(root: Path, record: dict[str, object]) -> list[str]:
    path = str(record["final_path"])
    source = root / path
    if record.get("excluded"):
        return [] if source.exists() else [f"{path}: excluded provenance path is missing"]
    if not source.exists():
        return [f"{path}: provenance path is missing"]
    try:
        text = source.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return [f"{path}: applicable provenance path is not UTF-8 text"]
    top = "\n".join(text.splitlines()[header_offset(text):][:14])
    errors: list[str] = []
    expected_lines = list(record.get("expected_copyright_lines", []))
    for line in expected_lines:
        if line not in top:
            errors.append(f"{path}: missing or incorrect copyright line: {line}")
    actual_lines = [line.strip(" /*#").strip() for line in top.splitlines()
                    if "Copyright (C)" in line]
    if actual_lines != expected_lines:
        errors.append(f"{path}: copyright lines do not exactly match provenance")
    spdx = f"SPDX-License-Identifier: {record['expected_license']}"
    if spdx not in top:
        errors.append(f"{path}: missing or incorrect SPDX identifier: {record['expected_license']}")
    return sorted(errors)


def validate(root: Path, project: str | None = None) -> list[str]:
    policy = json.loads((root / "tools/license-policy.json").read_text(encoding="utf-8"))
    errors: list[str] = []
    license_text = (root / "LICENSE").read_text(encoding="utf-8") if (root / "LICENSE").exists() else ""
    if "Apache License" not in license_text or "Version 2.0" not in license_text:
        errors.append("LICENSE: canonical Apache License 2.0 text is required")
    authors = (root / "AUTHORS.md").read_text(encoding="utf-8") if (root / "AUTHORS.md").exists() else ""
    if "Fabio Sobral" not in authors or "sole current" not in authors:
        errors.append("AUTHORS.md: Fabio Sobral must be identified as sole current maintainer")
    owners = (root / ".github/CODEOWNERS").read_text(encoding="utf-8") if (root / ".github/CODEOWNERS").exists() else ""
    if policy["codeowner"] not in owners:
        errors.append(".github/CODEOWNERS: required default owner is missing")
    provenance_path = root / "migration/license-provenance.json"
    records: list[dict[str, object]] = []
    if provenance_path.exists():
        records = json.loads(provenance_path.read_text(encoding="utf-8"))["files"]
    for record in records:
        if project is None or record["project"] == project:
            errors.extend(validate_record(root, record))
    tracked = read_paths(root)
    for email in policy.get("obsolete_emails", []):
        for rel in tracked:
            if rel in policy.get("exclusions", []):
                continue
            file = root / rel
            try:
                if email.lower() in file.read_text(encoding="utf-8").lower():
                    errors.append(f"{rel}: obsolete current-tree contact address: {email}")
            except (UnicodeDecodeError, IsADirectoryError):
                pass
    return sorted(set(errors))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--project")
    parser.add_argument("--staged", action="store_true", help="accepted for CI compatibility")
    args = parser.parse_args()
    errors = validate(args.root.resolve(), args.project)
    if errors:
        print("License and provenance validation failed:")
        print("\n".join(f"  {error}" for error in errors))
        return 1
    print("License and provenance validation passed:")
    print("  governance baseline is valid")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
