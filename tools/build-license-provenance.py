#!/usr/bin/env python3
# Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
# SPDX-License-Identifier: Apache-2.0
"""Build deterministic, reviewable file provenance from an unmodified source clone."""

from __future__ import annotations

import argparse
import json
import subprocess
from datetime import datetime
from pathlib import Path


TEXT_SUFFIXES = {".java", ".ts", ".py", ".sh", ".yml", ".yaml", ".md", ".gradle",
                 ".properties", ".xml", ".bat", ".template", ".gitignore", ".vscodeignore"}
EXCLUDED_NAMES = {"LICENSE", "NOTICE", "gradlew", "gradlew.bat", "package-lock.json"}


def git(source: Path, *args: str) -> str:
    return subprocess.run(["git", "-C", str(source), *args], check=True,
                          stdout=subprocess.PIPE, text=True).stdout


def commits_for(source: Path, path: str, since: str | None = None) -> list[tuple[str, datetime, str]]:
    args = ["log", "--format=%H%x09%aI%x09%s"]
    if since:
        args.append(f"--since={since}")
    args.extend(["--", path])
    result = []
    for row in git(source, *args).splitlines():
        sha, date, subject = row.split("\t", 2)
        result.append((sha, datetime.fromisoformat(date), subject))
    return result


def introduction(source: Path, path: str) -> tuple[str, int]:
    rows = git(source, "log", "--follow", "--diff-filter=A", "--format=%H%x09%aI", "--", path).splitlines()
    if rows:
        sha, date = rows[-1].split("\t", 1)
        return sha, datetime.fromisoformat(date).year
    all_rows = commits_for(source, path)
    if not all_rows:
        raise RuntimeError(f"no history found for {path}")
    sha, date, _ = min(all_rows, key=lambda row: row[1])
    return sha, date.year


def is_excluded(path: str, source: Path) -> tuple[bool, str | None]:
    name = Path(path).name
    if name in EXCLUDED_NAMES or path.endswith(".jar") or path.endswith((".gif", ".png")):
        return True, "binary, lockfile, license text, or generated wrapper material"
    if path.startswith("gradle/wrapper/"):
        return True, "generated Gradle Wrapper material"
    if path == "resources/maven-metadata.xml":
        return True, "external Maven metadata fixture"
    if name not in {".gitignore", ".vscodeignore"} and Path(path).suffix.lower() not in TEXT_SUFFIXES:
        return True, "format has no safe portable comment syntax"
    return False, None


def first_substantive_after_2021(source: Path, path: str) -> int | None:
    ignored = ("governance", "license", "copyright", "header", "plan")
    candidates = [row for row in commits_for(source, path, "2022-01-01")
                  if not any(word in row[2].lower() for word in ignored)]
    return min((date.year for _, date, _ in candidates), default=None)


def expected(year: int, later: int | None, original_license: str) -> tuple[list[str], str]:
    if year >= 2022:
        return [f"Copyright (C) {year if year == 2026 else f'{year}-2026'} Amalgam Solucoes em TI Ltda."], "Apache-2.0"
    old = str(year) if year == 2021 else f"{year}-2021"
    lines = [f"Copyright (C) {old} TotalCross Global Mobile Platform Ltda."]
    if later:
        current = str(later) if later == 2026 else f"{later}-2026"
        lines.append(f"Copyright (C) {current} Amalgam Solucoes em TI Ltda.")
        return lines, "Apache-2.0"
    return lines, original_license


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--project", required=True)
    parser.add_argument("--original-license", required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--append", action="store_true")
    parser.add_argument("--report", type=Path, help="write the required VS Code year-audit report")
    args = parser.parse_args()
    files = []
    for path in sorted(git(args.source, "ls-files").splitlines()):
        introduced, year = introduction(args.source, path)
        excluded, reason = is_excluded(path, args.source)
        later = None if excluded else first_substantive_after_2021(args.source, path)
        lines, license_name = expected(year, later, args.original_license)
        files.append({"project": args.project, "source_repository": args.source.name,
                      "source_path": path, "final_path": f"{args.project}/{path}",
                      "introduction_commit": introduced, "introduction_year": year,
                      "rename_following": "automatic", "first_post_2021_substantive_year": later,
                      "original_license": args.original_license, "expected_license": license_name,
                      "expected_copyright_lines": lines, "excluded": excluded,
                      "reason": reason or "Git introduction and first non-governance post-2021 change"})
    existing = json.loads(args.output.read_text(encoding="utf-8")) if args.append and args.output.exists() else {"files": []}
    existing["files"] = sorted(existing["files"] + files, key=lambda row: row["final_path"])
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(existing, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    if args.report:
        report_rows = [row for row in files if not row["excluded"]]
        lines = ["# VS Code file-year audit", "", "Generated from the unmodified source history before path rewriting.", "",
                 "| path | introduction commit | introduction year | first post-2021 substantive year | expected header | status |",
                 "| --- | --- | ---: | ---: | --- | --- |"]
        for row in report_rows:
            lines.append("| {source_path} | {introduction_commit} | {introduction_year} | {later} | {header} | reviewed |".format(
                **row, later=row["first_post_2021_substantive_year"] or "—",
                header="<br>".join(row["expected_copyright_lines"])))
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {len(files)} records for {args.project}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
