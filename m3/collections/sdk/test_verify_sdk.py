# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
import tempfile
import unittest
from pathlib import Path
import verify_sdk as sdk

class SDKReceiptTest(unittest.TestCase):
    def test_current_exact_sources(self):
        self.assertEqual(5, sdk.verify())
    def test_order_duplicate_and_missing_fail(self):
        with tempfile.TemporaryDirectory() as temp:
            p=Path(temp)/"receipt.tsv"
            rows=sdk.MANIFEST.read_text().splitlines()
            p.write_text("\n".join(reversed(rows))+"\n")
            with self.assertRaises(ValueError): sdk.source_rows(p)
            p.write_text("\n".join(rows[:-1]+[rows[0]])+"\n")
            with self.assertRaises(ValueError): sdk.source_rows(p)
            for path in sdk.EXPECTED:
                dest=Path(temp)/path
                dest.parent.mkdir(parents=True,exist_ok=True)
                dest.write_bytes((sdk.ROOT/path).read_bytes())
            self.assertEqual(5,sdk.verify(Path(temp)))
            path=Path(temp)/sdk.EXPECTED[0]
            path.write_bytes(path.read_bytes()+b"# drift")
            with self.assertRaises(ValueError): sdk.verify(Path(temp))
            path.unlink()
            with self.assertRaises(ValueError): sdk.verify(Path(temp))
