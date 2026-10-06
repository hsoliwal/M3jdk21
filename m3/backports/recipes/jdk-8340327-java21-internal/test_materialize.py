#!/usr/bin/env python3
import importlib.util
import pathlib
import tempfile
import unittest

HERE = pathlib.Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("materialize", HERE / "materialize.py")
MOD = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MOD)


class MaterializeTest(unittest.TestCase):
    def test_add_modify_fixed_point_and_drift(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            resources = root / "resources"
            resources.mkdir()
            target_existing = root / "src/main/java/p/A.java"
            target_existing.parent.mkdir(parents=True)
            target_existing.write_text("class A {}\n", encoding="utf-8")
            target_add = root / "test/jdk/p/B.java"

            a_after = b"class A { int x; }\n"
            b_after = b"class B {}\n"
            (resources / "A.after").write_bytes(a_after)
            (resources / "B.after").write_bytes(b_after)

            manifest = resources / "manifest.tsv"
            manifest.write_text(
                "src/main/java/p/A.java\t"
                + MOD.sha256(b"class A {}\n")
                + "\t"
                + MOD.sha256(a_after)
                + "\tA.after\n"
                + "test/jdk/p/B.java\tABSENT\t"
                + MOD.sha256(b_after)
                + "\tB.after\n",
                encoding="utf-8",
            )
            receipt = root / "receipt.tsv"
            MOD.materialize(root, manifest, receipt)
            self.assertEqual(a_after, target_existing.read_bytes())
            self.assertEqual(b_after, target_add.read_bytes())
            first = receipt.read_text(encoding="utf-8")
            self.assertIn("MATERIALIZED", first)

            MOD.materialize(root, manifest, receipt)
            self.assertIn("FIXED_POINT", receipt.read_text(encoding="utf-8"))

            target_existing.write_text("drift\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "preimage drift"):
                MOD.materialize(root, manifest, receipt)

    def test_rejects_unsafe_manifest_path(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            manifest = root / "manifest.tsv"
            manifest.write_text("../A.java\tABSENT\t" + "0" * 64 + "\tA.after\n")
            with self.assertRaisesRegex(ValueError, "unsafe target path"):
                MOD.rows(manifest)


if __name__ == "__main__":
    unittest.main()
