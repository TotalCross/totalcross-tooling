#!/usr/bin/env python3
# Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
# SPDX-License-Identifier: Apache-2.0

"""Delegate monorepo copyright validation for the Gradle plugin."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path, PurePosixPath


COPYRIGHT = "SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda."
LICENSE = "SPDX-License-Identifier: Apache-2.0"
APPLICABLE_SUFFIXES = {
    ".c",
    ".cc",
    ".cpp",
    ".gradle",
    ".groovy",
    ".java",
    ".js",
    ".json5",
    ".kt",
    ".kts",
    ".md",
    ".m",
    ".mm",
    ".py",
    ".sh",
    ".ts",
    ".tsx",
    ".xml",
    ".yaml",
    ".yml",
}
EXCLUDED_COMPONENTS = {
    ".git",
    ".gradle",
    "bin",
    "build",
    "dist",
    "generated",
    "node_modules",
    "out",
    "target",
    "third_party",
    "vendor",
    "vendored",
}
EXCLUDED_PATHS = {
    ".github/CODEOWNERS",
    "LICENSE",
    "NOTICE",
    "gradlew",
}


def git_paths(staged: bool) -> list[str]:
    command = ["git", "diff", "--cached", "--name-only", "-z"] if staged else ["git", "ls-files", "-z"]
    result = subprocess.run(command, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return sorted(path.decode("utf-8") for path in result.stdout.split(b"\0") if path)


def is_applicable(path: str) -> bool:
    candidate = PurePosixPath(path)
    if path in EXCLUDED_PATHS or candidate.parts[:2] == ("gradle", "wrapper"):
        return False
    if any(part in EXCLUDED_COMPONENTS for part in candidate.parts):
        return False
    return candidate.name == ".gitignore" or candidate.suffix.lower() in APPLICABLE_SUFFIXES


def diagnostics_for(path: str) -> list[str]:
    content = Path(path).read_text(encoding="utf-8")
    diagnostics: list[str] = []
    if "SPDX-FileCopyrightText:" not in content:
        diagnostics.append(f"{path}: missing SPDX-FileCopyrightText")
    elif COPYRIGHT not in content:
        diagnostics.append(f'{path}: expected copyright holder "2026 Amalgam Solucoes em TI Ltda."')
    if "SPDX-License-Identifier:" not in content:
        diagnostics.append(f"{path}: missing SPDX-License-Identifier")
    elif LICENSE not in content:
        diagnostics.append(f'{path}: expected SPDX license "Apache-2.0"')
    if re.search(r"(?m)^\s*(?://|#|/\*|\*)\s*Copyright \(C\)", content):
        diagnostics.append(f"{path}: contains obsolete Copyright (C) notice")
    return diagnostics


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--staged", action="store_true", help="accepted for CI compatibility")
    arguments = parser.parse_args(argv)
    monorepo_root = Path(__file__).resolve().parents[2]
    root_validator = monorepo_root / "tools/check-license-headers.py"
    if root_validator.exists():
        return subprocess.run(
            [sys.executable, str(root_validator), "--root", str(monorepo_root),
             "--project", "gradle-plugin", *( ["--staged"] if arguments.staged else [])],
            check=False,
        ).returncode
    paths = [path for path in git_paths(arguments.staged) if is_applicable(path)]
    diagnostics = sorted(diagnostic for path in paths for diagnostic in diagnostics_for(path))
    if diagnostics:
        print("\n".join(diagnostics))
        return 1
    print(f"License header validation passed: {len(paths)} applicable files checked.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
