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


def record(path="src/example.ts", year=2019, later=None, license="MIT", excluded=False):
    if year >= 2022:
        lines = [f"Copyright (C) {year}-2026 Amalgam Solucoes em TI Ltda."]
        license = "Apache-2.0"
    else:
        historical_year = str(year) if year == 2021 else f"{year}-2021"
        lines = [f"Copyright (C) {historical_year} TotalCross Global Mobile Platform Ltda."]
    if later and year < 2022:
        lines.append(f"Copyright (C) {later}-2026 Amalgam Solucoes em TI Ltda.")
        license = "Apache-2.0"
    return {"final_path": path, "expected_copyright_lines": lines,
            "expected_license": license, "excluded": excluded}


class HeaderValidationTests(unittest.TestCase):
    def validate(self, text, **kwargs):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / kwargs.pop("path", "src/example.ts")
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding="utf-8")
            return CHECK.validate_record(root, record(path=str(path.relative_to(root)), **kwargs))

    def test_valid_current_apache_c_style_header(self): self.assertEqual([], self.validate("/*\n * Copyright (C) 2022-2026 Amalgam Solucoes em TI Ltda.\n * SPDX-License-Identifier: Apache-2.0\n */", year=2022, later=2022))
    def test_valid_historical_mit_header(self): self.assertEqual([], self.validate("/*\n * Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\n * SPDX-License-Identifier: MIT\n */"))
    def test_valid_mixed_period_header(self): self.assertEqual([], self.validate("/*\n * Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\n * Copyright (C) 2023-2026 Amalgam Solucoes em TI Ltda.\n * SPDX-License-Identifier: Apache-2.0\n */", later=2023))
    def test_incorrect_initial_year(self): self.assertTrue(self.validate("Copyright (C) 2020-2021 TotalCross Global Mobile Platform Ltda.\nSPDX-License-Identifier: MIT"))
    def test_2019_not_generic_2020(self): self.assertTrue(self.validate("Copyright (C) 2020-2021 TotalCross Global Mobile Platform Ltda.\nSPDX-License-Identifier: MIT", year=2019))
    def test_2020_not_generic_2019(self): self.assertTrue(self.validate("Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\nSPDX-License-Identifier: MIT", year=2020))
    def test_single_2021(self): self.assertTrue(self.validate("Copyright (C) 2021-2021 TotalCross Global Mobile Platform Ltda.\nSPDX-License-Identifier: MIT", year=2021))
    def test_missing_totalcross(self): self.assertTrue(self.validate("SPDX-License-Identifier: MIT"))
    def test_missing_amalgam(self): self.assertTrue(self.validate("Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\nSPDX-License-Identifier: Apache-2.0", later=2023))
    def test_unwanted_amalgam(self): self.assertTrue(self.validate("Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\nCopyright (C) 2023-2026 Amalgam Solucoes em TI Ltda.\nSPDX-License-Identifier: MIT"))
    def test_wrong_spdx(self): self.assertTrue(self.validate("Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\nSPDX-License-Identifier: Apache-2.0"))
    def test_missing_spdx(self): self.assertTrue(self.validate("Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda."))
    def test_excluded_path(self): self.assertEqual([], self.validate("binary", excluded=True))
    def test_generated_exclusion(self): self.assertEqual([], self.validate("generated", path="generated/a.js", excluded=True))
    def test_path_with_spaces(self): self.assertEqual([], self.validate("Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\nSPDX-License-Identifier: MIT", path="src/a file.ts"))
    def test_shebang_placement(self): self.assertEqual([], self.validate("#!/usr/bin/env python3\n# Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\n# SPDX-License-Identifier: MIT", path="tool.py"))
    def test_xml_placement(self): self.assertEqual([], self.validate("<?xml version='1.0'?>\n<!--\n Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.\n SPDX-License-Identifier: MIT\n-->", path="a.xml"))
    def test_diagnostics_are_sorted(self): self.assertEqual(sorted(self.validate("")), self.validate(""))


if __name__ == "__main__":
    unittest.main()
