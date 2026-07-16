#!/usr/bin/env python3
# Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
# SPDX-License-Identifier: Apache-2.0

"""Regression tests for the dependency-free SPDX header validator."""

from __future__ import annotations

import subprocess
import tempfile
import unittest
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
VALIDATOR = REPOSITORY_ROOT / "tools" / "check-license-headers.py"
COPYRIGHT = "SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda."
LICENSE = "SPDX-License-Identifier: Apache-2.0"


class LicenseHeaderValidatorTest(unittest.TestCase):
    def run_validator(self, files: dict[str, str]) -> subprocess.CompletedProcess[str]:
        temporary_directory = tempfile.TemporaryDirectory()
        self.addCleanup(temporary_directory.cleanup)
        root = Path(temporary_directory.name)
        subprocess.run(["git", "init", "--quiet"], cwd=root, check=True)
        for name, content in files.items():
            path = root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8")
        subprocess.run(["git", "add", "-A"], cwd=root, check=True)
        return subprocess.run(
            ["python3", str(VALIDATOR), "--staged"],
            cwd=root,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )

    def test_accepts_comment_styles_and_ignores_excluded_files(self) -> None:
        result = self.run_validator(
            {
                "src/Valid.java": f"// {COPYRIGHT}\n// {LICENSE}\nclass Valid {{}}\n",
                "script.py": f"#!/usr/bin/env python3\n# {COPYRIGHT}\n# {LICENSE}\n",
                "docs/guide.md": f"<!--\n{COPYRIGHT}\n{LICENSE}\n-->\n",
                "workflow.yml": f"# {COPYRIGHT}\n# {LICENSE}\nname: test\n",
                "manifest.xml": f"<?xml version=\"1.0\"?>\n<!--\n{COPYRIGHT}\n{LICENSE}\n-->\n",
                "vendor/upstream.java": "class Upstream {}\n",
                "generated/output.java": "class Output {}\n",
                ".github/CODEOWNERS": "* @flsobral\n",
            }
        )
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("5 applicable files checked", result.stdout)

    def test_reports_invalid_headers_in_sorted_order_and_handles_spaces(self) -> None:
        result = self.run_validator(
            {
                "z.java": f"// {COPYRIGHT}\n// SPDX-License-Identifier: MIT\n",
                "a missing.java": f"// {LICENSE}\n",
                "b.py": f"# SPDX-FileCopyrightText: 2025 Amalgam Solucoes em TI Ltda.\n",
            }
        )
        self.assertEqual(1, result.returncode)
        lines = result.stdout.splitlines()
        self.assertEqual(sorted(lines), lines)
        self.assertIn("a missing.java: missing SPDX-FileCopyrightText", lines)
        self.assertIn("b.py: expected copyright holder \"2026 Amalgam Solucoes em TI Ltda.\"", lines)
        self.assertIn("b.py: missing SPDX-License-Identifier", lines)
        self.assertIn("z.java: expected SPDX license \"Apache-2.0\"", lines)


if __name__ == "__main__":
    unittest.main()
