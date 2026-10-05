#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import hashlib
import importlib.util
from pathlib import Path
import tempfile
import unittest


MODULE_PATH = Path(__file__).with_name("materialize.py")
SPEC = importlib.util.spec_from_file_location("m3_jep485_materialize", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
materialize = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(materialize)


class MaterializeTest(unittest.TestCase):
    def fixture(self) -> tuple[Path, str, bytes]:
        root = Path(tempfile.mkdtemp(prefix="m3-jep485-test-"))
        crate = root / materialize.CRATE
        crate.mkdir(parents=True)
        target = "src/java.base/share/classes/java/util/stream/Foo.java"
        before = b"class Foo {}\n"
        after = b"class Foo { int value() { return 1; } }\n"
        resource = "00-Foo.java.txt"
        (crate / resource).write_bytes(after)
        (crate / "manifest.tsv").write_text(
            f"{target}\t{hashlib.sha256(before).hexdigest()}\t"
            f"{hashlib.sha256(after).hexdigest()}\t{resource}\n",
            encoding="utf-8",
        )
        path = root / target
        path.parent.mkdir(parents=True)
        path.write_bytes(before)
        return root, target, after

    def test_check_apply_verify_post_and_fixed_point(self) -> None:
        root, target, after = self.fixture()
        receipt = root / "target" / "receipt.tsv"

        self.assertEqual(1, materialize.materialize(root, "check", receipt))
        self.assertNotEqual(after, (root / target).read_bytes())

        self.assertEqual(1, materialize.materialize(root, "apply", receipt))
        self.assertEqual(after, (root / target).read_bytes())

        self.assertEqual(1, materialize.materialize(root, "verify-post", receipt))
        self.assertEqual(after, (root / target).read_bytes())

        # Re-applying exact postimages is idempotent.
        self.assertEqual(1, materialize.materialize(root, "apply", receipt))
        self.assertEqual(after, (root / target).read_bytes())

    def test_absent_addition_is_created_but_check_remains_read_only(self) -> None:
        root = Path(tempfile.mkdtemp(prefix="m3-jep485-add-"))
        crate = root / materialize.CRATE
        crate.mkdir(parents=True)
        target = "test/jdk/java/util/stream/Added.java"
        after = b"class Added {}\n"
        resource = "00-Added.java.txt"
        (crate / resource).write_bytes(after)
        (crate / "manifest.tsv").write_text(
            f"{target}\tABSENT\t{hashlib.sha256(after).hexdigest()}\t{resource}\n",
            encoding="utf-8",
        )

        self.assertEqual(1, materialize.materialize(root, "check", None))
        self.assertFalse((root / target).exists())

        self.assertEqual(1, materialize.materialize(root, "apply", None))
        self.assertEqual(after, (root / target).read_bytes())

    def test_drift_and_path_escape_fail_closed(self) -> None:
        root, target, _ = self.fixture()
        (root / target).write_text("drift\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "JEP485_PREIMAGE_DRIFT"):
            materialize.materialize(root, "check", None)

        self.assertRaises(ValueError, materialize.canonical_relative, "../escape")
        self.assertRaises(ValueError, materialize.canonical_relative, "/absolute")
        self.assertRaises(ValueError, materialize.canonical_relative, "a\\b")

    def test_repository_manifest_is_current_master_admissible(self) -> None:
        root = Path(__file__).resolve().parents[4]
        targets = materialize.load_manifest(root)
        self.assertEqual(15, len(targets))
        self.assertEqual(15, materialize.materialize(root, "check", None))


if __name__ == "__main__":
    unittest.main()
