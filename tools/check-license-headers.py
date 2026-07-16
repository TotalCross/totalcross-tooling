#!/usr/bin/env python3
"""Validate the root governance baseline without modifying tracked files."""
from __future__ import annotations
from pathlib import Path
import json
import subprocess

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = ("LICENSE", "NOTICE", "README.md", "AUTHORS.md", "CONTRIBUTING.md", "AGENTS.md", ".agent/PLANS.md", ".github/CODEOWNERS")

def main() -> int:
    failures = [name for name in REQUIRED if not (ROOT / name).is_file()]
    if "Apache License" not in (ROOT / "LICENSE").read_text(): failures.append("LICENSE is not Apache-2.0 text")
    if "* @flsobral" not in (ROOT / ".github/CODEOWNERS").read_text(): failures.append("CODEOWNERS lacks default @flsobral rule")
    manifest = ROOT / "migration/license-provenance.json"
    if not manifest.is_file(): failures.append("missing provenance manifest")
    else:
        recorded = {record["final_path"] for record in json.loads(manifest.read_text())["records"]}
        tracked = subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT, text=True).split("\0")
        imported = {path for path in tracked if path.split("/", 1)[0] in {"maven-plugin", "vscode-extension", "gradle-plugin"}}
        if recorded != imported: failures.append("provenance manifest does not cover exactly the imported files")
    if failures:
        print("Governance validation failed:"); print(*sorted(failures), sep="\n"); return 1
    print("License and provenance validation passed: root governance baseline and imported provenance inventory are present.")
    return 0
if __name__ == "__main__": raise SystemExit(main())
