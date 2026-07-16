#!/usr/bin/env python3
"""Create a deterministic per-file import provenance inventory from Git."""
from __future__ import annotations
import argparse, json, subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
PROJECTS = {"maven-plugin": "unlicensed-before-migration", "vscode-extension": "MIT", "gradle-plugin": "Apache-2.0"}
def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()
def introduced(path: str) -> tuple[str, int]:
    commits = git("log", "--follow", "--diff-filter=A", "--format=%H", "--", path).splitlines()
    commit = commits[-1] if commits else git("log", "--format=%H", "--", path).splitlines()[-1]
    return commit, int(git("show", "-s", "--format=%ad", "--date=format:%Y", commit))
def main() -> int:
    parser = argparse.ArgumentParser(); parser.add_argument("--output", default="migration/license-provenance.json"); args = parser.parse_args()
    paths = git("ls-files", "-z").split("\0")
    records=[]
    for path in sorted(p for p in paths if p.split('/', 1)[0] in PROJECTS):
        project=path.split('/',1)[0]; commit, year=introduced(path)
        records.append({"project":project,"source_path":path.split('/',1)[1],"final_path":path,"introduction_commit":commit,"introduction_year":year,"first_post_2021_substantive_year":None,"original_license":PROJECTS[project],"expected_license":PROJECTS[project],"expected_copyright_lines":[],"excluded":True,"exclusion_reason":"Imported file retained without migration-only header edits; consult its authoritative notice or source header."})
    output={"format_version":1,"records":records}
    target=ROOT/args.output; target.parent.mkdir(parents=True,exist_ok=True); target.write_text(json.dumps(output,indent=2,sort_keys=True)+"\n")
    print(f"Wrote {len(records)} provenance records to {target.relative_to(ROOT)}")
    return 0
if __name__ == '__main__': raise SystemExit(main())
