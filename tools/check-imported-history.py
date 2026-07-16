#!/usr/bin/env python3
# Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
# SPDX-License-Identifier: Apache-2.0
"""Audit rewritten commits against immutable source mirrors and commit maps."""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path


PROJECTS = ("maven-plugin", "vscode-extension", "gradle-plugin")


def git(repo: Path, *args: str) -> bytes:
    return subprocess.run(["git", "-C", str(repo), *args], check=True,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout


def commit(repo: Path, sha: str) -> tuple[list[str], bytes, bytes, bytes]:
    raw = git(repo, "cat-file", "commit", sha)
    headers, message = raw.split(b"\n\n", 1)
    parents, author, committer = [], b"", b""
    for line in headers.splitlines():
        if line.startswith(b"parent "):
            parents.append(line[7:].strip())
        elif line.startswith(b"author "):
            author = line[7:]
        elif line.startswith(b"committer "):
            committer = line[10:]
    return [p.decode() for p in parents], author, committer, message


def load_map(path: Path) -> dict[str, str]:
    return {old: new for old, new in (line.split() for line in path.read_text().splitlines()
                                      if not line.startswith("old "))}


def audit(source: Path, target: Path, project: str) -> list[str]:
    mapping = load_map(target / "migration/commit-maps" / f"{project}.txt")
    revisions = json.loads((target / "migration/source-revisions.json").read_text())
    source_commits = git(source / f"totalcross-{project.replace('-plugin', '')}-plugin.git" if project != "vscode-extension" else source / "totalcross-vscode-plugin.git", "rev-list", revisions[project]).decode().split()
    errors = []
    for old in source_commits:
        if old not in mapping:
            errors.append(f"{project}: missing map for {old}")
            continue
        old_parents, old_author, old_committer, old_message = commit(source / ("totalcross-vscode-plugin.git" if project == "vscode-extension" else f"totalcross-{project}.git"), old)
        new_parents, new_author, new_committer, new_message = commit(target, mapping[old])
        if (old_author, old_committer, old_message) != (new_author, new_committer, new_message):
            errors.append(f"{project}: identity or message mismatch for {old}")
        if [mapping.get(parent) for parent in old_parents] != new_parents:
            errors.append(f"{project}: parent topology mismatch for {old}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--project", choices=PROJECTS)
    args = parser.parse_args()
    target = Path.cwd()
    projects = [args.project] if args.project else list(PROJECTS)
    errors = []
    for project in projects:
        errors.extend(audit(args.source_root, target, project))
    if errors:
        print("Imported history audit failed:")
        print("\n".join(f"  {line}" for line in sorted(errors)))
        return 1
    print("Imported history audit passed:")
    print(f"  {len(projects)} project(s) verified")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
