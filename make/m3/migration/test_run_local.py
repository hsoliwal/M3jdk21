# SPDX-License-Identifier: Apache-2.0
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import run_local

class InputScopeTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.port = self.root / 'm3/ports/indexstring'
        self.here = self.root / 'm3/migration'
        self.port.mkdir(parents=True)
        self.here.mkdir(parents=True)
        self.addCleanup(patch.stopall)
        patch.object(run_local, 'ROOT', self.root).start()
        patch.object(run_local, 'PORT', self.port).start()
        patch.object(run_local, 'HERE', self.here).start()

    def test_unrelated_jdk_sources_are_not_receipt_inputs(self):
        unrelated = self.root / 'src/java.base/share/classes/java/lang/String.java'
        unrelated.parent.mkdir(parents=True)
        unrelated.write_text('unrelated JDK source')
        actual = self.port / 'Example.java'
        actual.write_text('class Example {}')
        snapshot = run_local.input_snapshot()
        self.assertEqual(set(snapshot), {'m3/ports/indexstring/Example.java'})
        unrelated.write_text('unrelated source changed')
        self.assertEqual(run_local.input_snapshot(), snapshot)
        actual.write_text('class Example { int changed; }')
        self.assertNotEqual(run_local.input_snapshot(), snapshot)

    def test_evidence_outputs_do_not_self_hash(self):
        evidence = self.here / 'evidence'
        evidence.mkdir()
        (evidence / 'receipt.json').write_text('{}')
        self.assertEqual(run_local.input_snapshot(), {})

    def test_symlink_input_is_refused(self):
        outside = self.root / 'outside.java'
        outside.write_text('outside')
        (self.port / 'alias.java').symlink_to(outside)
        with self.assertRaises(RuntimeError):
            run_local.input_snapshot()

if __name__ == '__main__':
    unittest.main()
