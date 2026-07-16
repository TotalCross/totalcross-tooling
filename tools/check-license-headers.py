#!/usr/bin/env python3
# Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
# SPDX-License-Identifier: Apache-2.0
"""Validate current-tree license, provenance, attribution, and contacts."""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from datetime import datetime, timezone
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


def year_or_range(start: int, end: int) -> str:
    return str(start) if start == end else f"{start}-{end}"


def expected_header(introduction_year: int, current_year: int | None = None) -> tuple[list[str], str]:
    """Return the mandatory header from the file's Git introduction year."""
    current_year = current_year or datetime.now(timezone.utc).year
    if introduction_year <= 2021:
        return [
            f"Copyright (C) {year_or_range(introduction_year, 2021)} TotalCross Global Mobile Platform Ltda.",
            f"Copyright (C) {year_or_range(2022, current_year)} Amalgam Solucoes em TI Ltda.",
        ], "Apache-2.0"
    return [f"Copyright (C) {year_or_range(introduction_year, current_year)} Amalgam Solucoes em TI Ltda."], "Apache-2.0"


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
    expected_lines, expected_license = expected_header(int(record["introduction_year"]))
    if record.get("expected_copyright_lines") != expected_lines:
        errors.append(f"{path}: provenance copyright lines do not follow the creation-year rule")
    if record.get("expected_license") != expected_license:
        errors.append(f"{path}: provenance license does not follow the creation-year rule")
    for line in expected_lines:
        if line not in top:
            errors.append(f"{path}: missing or incorrect copyright line: {line}")
    header_lines = top.splitlines()
    try:
        header_end = next(index for index, line in enumerate(header_lines)
                          if "SPDX-License-Identifier:" in line) + 1
    except StopIteration:
        header_end = 0
    actual_lines = [line.strip(" /*#").strip() for line in header_lines[:header_end]
                    if "Copyright (C)" in line]
    if actual_lines != expected_lines:
        errors.append(f"{path}: copyright lines do not exactly match provenance")
    spdx = f"SPDX-License-Identifier: {expected_license}"
    if spdx not in top:
        errors.append(f"{path}: missing or incorrect SPDX identifier: {expected_license}")
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
        active = policy.get("projects", {}).get(record["project"], {}).get("active", False)
        if active and (project is None or record["project"] == project):
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
