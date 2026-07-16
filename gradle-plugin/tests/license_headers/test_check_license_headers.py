#!/usr/bin/env python3
# Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
# SPDX-License-Identifier: Apache-2.0

"""Regression test for the Gradle plugin's monorepo validator entry point."""

from __future__ import annotations

import subprocess
import unittest
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
VALIDATOR = REPOSITORY_ROOT / "gradle-plugin" / "tools" / "check-license-headers.py"


class LicenseHeaderValidatorTest(unittest.TestCase):
    def test_delegates_to_the_creation_year_validator(self) -> None:
        result = subprocess.run(
            ["python3", str(VALIDATOR)],
            cwd=REPOSITORY_ROOT,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("governance baseline is valid", result.stdout)


if __name__ == "__main__":
    unittest.main()
