# Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
# SPDX-License-Identifier: Apache-2.0
"""Regression tests for the provenance-header validator primitives."""

import importlib.util
import tempfile
import unittest
from pathlib import Path


MODULE = Path(__file__).parents[2] / "tools/check-license-headers.py"
SPEC = importlib.util.spec_from_file_location("license_check", MODULE)
CHECK = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(CHECK)
CURRENT_YEAR = CHECK.datetime.now(CHECK.timezone.utc).year


def amalgam(start: int) -> str:
    year = str(start) if start == CURRENT_YEAR else f"{start}-{CURRENT_YEAR}"
    return f"Copyright (C) {year} Amalgam Solucoes em TI Ltda."


def record(path="src/example.ts", year=2019, excluded=False):
    lines, license_name = CHECK.expected_header(year)
    return {"final_path": path, "introduction_year": year,
            "expected_copyright_lines": lines, "expected_license": license_name,
            "excluded": excluded}


class HeaderValidationTests(unittest.TestCase):
    def validate(self, text, **kwargs):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / kwargs.pop("path", "src/example.ts")
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding="utf-8")
            return CHECK.validate_record(root, record(path=str(path.relative_to(root)), **kwargs))

    def test_valid_current_apache_c_style_header(self): self.assertEqual([], self.validate(f"/*\n * {amalgam(2022)}\n * SPDX-License-Identifier: Apache-2.0\n */", year=2022))
    def test_valid_historical_header(self): self.assertEqual([], self.validate(f"/*\n * Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\n * {amalgam(2022)}\n * SPDX-License-Identifier: Apache-2.0\n */"))
    def test_incorrect_initial_year(self): self.assertTrue(self.validate("Copyright (C) 2020-2021 TotalCross Global Mobile Platform Ltda.\nSPDX-License-Identifier: MIT"))
    def test_2019_not_generic_2020(self): self.assertTrue(self.validate("Copyright (C) 2020-2021 TotalCross Global Mobile Platform Ltda.\nSPDX-License-Identifier: MIT", year=2019))
    def test_2020_not_generic_2019(self): self.assertTrue(self.validate("Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\nSPDX-License-Identifier: MIT", year=2020))
    def test_single_2021(self): self.assertTrue(self.validate("Copyright (C) 2021-2021 TotalCross Global Mobile Platform Ltda.\nSPDX-License-Identifier: MIT", year=2021))
    def test_missing_totalcross(self): self.assertTrue(self.validate("SPDX-License-Identifier: MIT"))
    def test_missing_amalgam(self): self.assertTrue(self.validate("Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\nSPDX-License-Identifier: Apache-2.0"))
    def test_wrong_amalgam_start_year(self): self.assertTrue(self.validate(f"Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\n{amalgam(2023)}\nSPDX-License-Identifier: Apache-2.0"))
    def test_wrong_spdx(self): self.assertTrue(self.validate(f"Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\n{amalgam(2022)}\nSPDX-License-Identifier: MIT"))
    def test_missing_spdx(self): self.assertTrue(self.validate(f"Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\n{amalgam(2022)}"))
    def test_excluded_path(self): self.assertEqual([], self.validate("binary", excluded=True))
    def test_generated_exclusion(self): self.assertEqual([], self.validate("generated", path="generated/a.js", excluded=True))
    def test_path_with_spaces(self): self.assertEqual([], self.validate(f"Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\n{amalgam(2022)}\nSPDX-License-Identifier: Apache-2.0", path="src/a file.ts"))
    def test_shebang_placement(self): self.assertEqual([], self.validate(f"#!/usr/bin/env python3\n# Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\n# {amalgam(2022)}\n# SPDX-License-Identifier: Apache-2.0", path="tool.py"))
    def test_xml_placement(self): self.assertEqual([], self.validate(f"<?xml version='1.0'?>\n<!--\n Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\n {amalgam(2022)}\n SPDX-License-Identifier: Apache-2.0\n-->", path="a.xml"))
    def test_diagnostics_are_sorted(self): self.assertEqual(sorted(self.validate("")), self.validate(""))


if __name__ == "__main__":
    unittest.main()
