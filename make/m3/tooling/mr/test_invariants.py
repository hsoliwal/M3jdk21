# SPDX-License-Identifier: Apache-2.0
"""Execute the retained String invariant gate on exact and deliberately broken fixtures."""
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import unittest

CRATE = Path(__file__).resolve().parent
ROOT = CRATE.parents[2]
source = Path(sys.argv.pop(1)) if len(sys.argv) > 1 else ROOT / 'm3/runtime-integration/check-m3string-invariants.py'
script = source.read_text()
paths = list(dict.fromkeys(re.findall(r'read\("([^"]+)"\)', script)))


class InvariantTest(unittest.TestCase):
    def trial(self, path=None, before=None, after=None):
        with tempfile.TemporaryDirectory(prefix='m3-invariant-') as directory:
            root = Path(directory)
            for name in paths:
                content = (ROOT / name).read_text()
                if name == path:
                    self.assertIn(before, content)
                    content = content.replace(before, after)
                target = root / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(content)
            gate = root / 'm3/runtime-integration/check-m3string-invariants.py'
            gate.parent.mkdir(parents=True, exist_ok=True)
            gate.write_text(script)
            return subprocess.run([sys.executable, str(gate)], capture_output=True, text=True, timeout=10)

    def test_current_source_and_valid_test_arguments_pass(self):
        result = self.trial()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn('M3_STRING_SOURCE_INVARIANTS_PASS', result.stdout)

    def test_unsafe_utf8_shortcut_refuses(self):
        result = self.trial('src/java.base/share/classes/java/lang/M3String.java',
                            'prepared.unpairedSurrogateCount == 0', 'true')
        self.assertNotEqual(0, result.returncode)
        self.assertIn('strict UTF-8', result.stderr)

    def test_lost_exact_equality_refuses(self):
        result = self.trial('src/java.base/share/classes/java/lang/M3String.java',
                            'if (charAt(index) != other.charAt(index)) return false;',
                            'if (false) return false;')
        self.assertNotEqual(0, result.returncode)
        self.assertIn('collision-safe equality', result.stderr)

    def test_lost_exact_compare_refuses(self):
        result = self.trial('src/java.base/share/classes/java/lang/String.java',
                            'int difference = charAt(index) - anotherString.charAt(index);',
                            'int difference = 0;')
        self.assertNotEqual(0, result.returncode)
        self.assertIn('exact comparison route', result.stderr)

    def test_concatenated_path_filter_refuses(self):
        result = self.trial('.github/workflows/mindex-string-backing.yml',
                            "      - 'test/jdk/java/lang/String/M3StringPrecomputeSearchTest.java'",
                            "      - 'test/jdk/java/lang/String/M3StringPrecomputeSearchTest.java test/jdk/Broken.java'")
        self.assertNotEqual(0, result.returncode)
        self.assertIn('concatenated path entries', result.stderr)


if __name__ == '__main__':
    unittest.main()
