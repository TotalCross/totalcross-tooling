#!/usr/bin/env python3

# Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
# SPDX-License-Identifier: Apache-2.0

import importlib.util
import subprocess
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).parents[2] / "scripts" / "check-file-size-policy.py"
SPEC = importlib.util.spec_from_file_location("file_size_policy", SCRIPT)
assert SPEC and SPEC.loader
POLICY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(POLICY)


class FileSizePolicyTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.repo = Path(self.temp_dir.name)
        subprocess.run(("git", "init", "-q", str(self.repo)), check=True)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def stage(self, path: str, data: bytes) -> tuple[list[str], list[str]]:
        target = self.repo / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        subprocess.run(("git", "-C", str(self.repo), "add", path), check=True)
        return POLICY.check_staged(str(self.repo))

    def test_path_matching(self) -> None:
        self.assertTrue(POLICY.is_protected("TotalCrossVM/src/tcvm/ir/node.c"))
        self.assertFalse(POLICY.is_protected("src/main/java/Example.java"))
        self.assertTrue(POLICY.is_skipped("build/generated.txt"))
        self.assertTrue(POLICY.is_skipped("src/foo.generated.java"))

    def test_byte_limit(self) -> None:
        failures, _ = self.stage("docs/large.md", b"x" * (POLICY.MAX_BYTES + 1))
        self.assertTrue(any("bytes exceeds" in failure for failure in failures))

    def test_line_limit(self) -> None:
        failures, _ = self.stage("docs/large.md", b"x\n" * (POLICY.MAX_LINES + 1))
        self.assertTrue(any("lines exceeds" in failure for failure in failures))

    def test_binary_detection(self) -> None:
        failures, warnings = self.stage("assets/icon.bin", b"PNG\0binary")
        self.assertEqual([], failures)
        self.assertEqual([], warnings)
        self.assertFalse(POLICY.is_text_bytes(b"a\0b"))

    def test_protected_exception(self) -> None:
        failures, warnings = self.stage(
            "TotalCrossVM/src/tcvm/ir/large.c", b"x" * (POLICY.MAX_BYTES + 1)
        )
        self.assertEqual([], failures)
        self.assertTrue(any("protected path exempted" in warning for warning in warnings))


if __name__ == "__main__":
    unittest.main()
