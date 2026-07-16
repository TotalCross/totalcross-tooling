#!/usr/bin/env python3
# Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
# SPDX-License-Identifier: Apache-2.0
"""Audit rewritten commits against explicit source and mapping inputs.

This command is intentionally not a default repository check. It is useful only
when a source mirror, a source-revision manifest, and old-to-new commit maps are
provided by a separately evidenced history migration.
"""

from __future__ import annotations

import argparse
import json
import subprocess
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


def source_repository(source: Path, project: str) -> Path:
    name = "totalcross-vscode-plugin" if project == "vscode-extension" else f"totalcross-{project}"
    return source / f"{name}.git"


def audit(source: Path, target: Path, mapping_root: Path, revisions_file: Path,
          project: str) -> list[str]:
    mapping_file = mapping_root / f"{project}.txt"
    if not mapping_file.is_file():
        return [f"{project}: mapping file not found: {mapping_file}"]
    if not revisions_file.is_file():
        return [f"{project}: source-revisions file not found: {revisions_file}"]
    source_repo = source_repository(source, project)
    if not source_repo.is_dir():
        return [f"{project}: source mirror not found: {source_repo}"]
    mapping = load_map(mapping_file)
    revisions = json.loads(revisions_file.read_text())
    if project not in revisions:
        return [f"{project}: source revision is not declared in {revisions_file}"]
    source_commits = git(source / f"totalcross-{project.replace('-plugin', '')}-plugin.git" if project != "vscode-extension" else source / "totalcross-vscode-plugin.git", "rev-list", revisions[project]).decode().split()
    errors = []
    for old in source_commits:
        if old not in mapping:
            errors.append(f"{project}: missing map for {old}")
            continue
        old_parents, old_author, old_committer, old_message = commit(source_repo, old)
        try:
            new_parents, new_author, new_committer, new_message = commit(target, mapping[old])
        except subprocess.CalledProcessError:
            errors.append(f"{project}: mapped target commit not found: {mapping[old]}")
            continue
        if (old_author, old_committer, old_message) != (new_author, new_committer, new_message):
            errors.append(f"{project}: identity or message mismatch for {old}")
        if [mapping.get(parent) for parent in old_parents] != new_parents:
            errors.append(f"{project}: parent topology mismatch for {old}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, required=True,
                        help="directory containing the source mirror repositories")
    parser.add_argument("--target-root", type=Path, default=Path.cwd(),
                        help="target checkout containing mapped commits (default: cwd)")
    parser.add_argument("--mapping-root", type=Path,
                        help="directory containing one mapping file per project")
    parser.add_argument("--revisions-file", type=Path,
                        help="JSON file declaring the source revision for each project")
    parser.add_argument("--project", choices=PROJECTS)
    args = parser.parse_args()
    target = args.target_root.resolve()
    mapping_root = (args.mapping_root or target / "migration" / "commit-maps").resolve()
    revisions_file = (args.revisions_file or target / "migration" / "source-revisions.json").resolve()
    projects = [args.project] if args.project else list(PROJECTS)
    errors = []
    for project in projects:
        errors.extend(audit(args.source_root.resolve(), target, mapping_root,
                            revisions_file, project))
    if errors:
        print("Imported history audit failed:")
        print("\n".join(f"  {line}" for line in sorted(errors)))
        return 1
    print("Imported history audit passed:")
    print(f"  {len(projects)} project(s) verified")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
