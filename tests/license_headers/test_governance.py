import subprocess
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

class GovernanceValidatorTest(unittest.TestCase):
    def test_governance_validator_passes(self):
        result = subprocess.run([sys.executable, 'tools/check-license-headers.py'], cwd=ROOT)
        self.assertEqual(result.returncode, 0)
