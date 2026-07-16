#!/usr/bin/env python3
"""Perform an offline sanity audit of committed filter-repo commit maps."""
from __future__ import annotations
import subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def object_exists(sha: str) -> bool:
    return subprocess.run(['git','cat-file','-e',f'{sha}^{{commit}}'],cwd=ROOT).returncode == 0
def main() -> int:
    failures=[]; total=0
    for name in ('maven-plugin','vscode-extension','gradle-plugin'):
        path=ROOT/'migration/commit-maps'/f'{name}.txt'
        lines=path.read_text().splitlines()[1:] if path.exists() else []
        if not lines: failures.append(f'{name}: missing or empty map'); continue
        for line in lines:
            old,new=line.split()
            if old == new or not object_exists(new): failures.append(f'{name}: invalid target for {old}')
        total += len(lines)
    if failures: print(*sorted(failures),sep='\n'); return 1
    print(f'Imported-history map audit passed: {total} rewritten commits.')
    return 0
if __name__=='__main__': raise SystemExit(main())
