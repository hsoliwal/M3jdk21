#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Independent fail-closed proofs of the source image receipt."""

import importlib.util
from pathlib import Path
import unittest

MODULE_PATH = Path(__file__).with_name("verify_current.py")
SPEC = importlib.util.spec_from_file_location("cursor_objects_verify", MODULE_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError("unable to load exact image verifier")
VERIFY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFY)


class CursorObjectsReceiverProof(unittest.TestCase):
    def setUp(self) -> None:
        self.root = Path(__file__).resolve().parents[4]
        self.original = (self.root / VERIFY.RELATIVE).read_bytes()

    def test_source_on_current_branch_is_exact_postimage(self) -> None:
        VERIFY.verify(self.original)

    def test_delete_import_is_rejected(self) -> None:
        with self.assertRaisesRegex(ValueError, "postimage drift"):
            VERIFY.verify(self.original.replace(VERIFY.IMPORT, VERIFY.ANCHOR, 1))

    def test_unrelated_source_change_is_rejected(self) -> None:
        with self.assertRaisesRegex(ValueError, "postimage drift"):
            VERIFY.verify(self.original.replace(
                b"MAX_PATTERN_UNITS = 8_192", b"MAX_PATTERN_UNITS = 8_193", 1
            ))

    def test_duplicate_objects_import_is_rejected(self) -> None:
        with self.assertRaisesRegex(ValueError, "postimage drift"):
            VERIFY.verify(self.original.replace(
                VERIFY.IMPORT, VERIFY.IMPORT + VERIFY.ADDITION, 1
            ))


if __name__ == "__main__":
    unittest.main()
