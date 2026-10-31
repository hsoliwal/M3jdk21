# SPDX-License-Identifier: Apache-2.0
"""Execute the ROOT-ASCII case repair packet on exact owned fixtures."""
import hashlib
import json
import tempfile
import unittest
from pathlib import Path
import Fix

MANIFEST = json.loads((Fix.HERE / "manifest.json").read_text())


class FixTest(unittest.TestCase):
    def seed(self, target):
        for name in list(MANIFEST["files"]) + list(MANIFEST["verification_files"]):
            source = Fix.ROOT / name
            destination = target / name
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(source.read_bytes())

    @staticmethod
    def snapshot(target):
        names = list(MANIFEST["files"]) + list(MANIFEST["verification_files"])
        return {
            name: (target / name).read_bytes() if (target / name).exists() else None
            for name in names
        }

    def test_exact_apply_fixedpoint_and_reverse(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed(target)
            self.assertEqual("after", Fix.apply(target, check=True))
            self.assertEqual("before", Fix.apply(target, reverse=True))
            before = self.snapshot(target)
            self.assertEqual("before", Fix.apply(target, check=True))
            self.assertEqual(before, self.snapshot(target))
            self.assertEqual("after", Fix.apply(target))
            after = self.snapshot(target)
            for name, hashes in MANIFEST["files"].items():
                self.assertEqual(
                    hashes["after"],
                    hashlib.sha256(after[name]).hexdigest(),
                    name,
                )
            self.assertEqual("after", Fix.apply(target))
            self.assertEqual(after, self.snapshot(target))
            self.assertEqual("before", Fix.apply(target, reverse=True))
            self.assertEqual(before, self.snapshot(target))

    def test_source_drift_refuses_without_partial_write(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed(target)
            source = target / next(iter(MANIFEST["files"]))
            source.write_bytes(source.read_bytes() + b"\n// drift\n")
            before = self.snapshot(target)
            with self.assertRaisesRegex(ValueError, "source drift"):
                Fix.apply(target)
            self.assertEqual(before, self.snapshot(target))

    def test_verification_drift_refuses_before_product_write(self):
        for name in MANIFEST["verification_files"]:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as folder:
                target = Path(folder)
                self.seed(target)
                product = target / next(iter(MANIFEST["files"]))
                product_before = product.read_bytes()
                proof = target / name
                proof.write_bytes(proof.read_bytes() + b"\n# drift\n")
                with self.assertRaisesRegex(ValueError, "verification drift"):
                    Fix.apply(target, reverse=True)
                self.assertEqual(product_before, product.read_bytes())

    def test_missing_or_symlink_verification_refuses(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed(target)
            name = next(iter(MANIFEST["verification_files"]))
            path = target / name
            path.unlink()
            with self.assertRaisesRegex(ValueError, "verification drift"):
                Fix.apply(target)

        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed(target)
            name = next(iter(MANIFEST["verification_files"]))
            path = target / name
            path.unlink()
            path.symlink_to(Fix.ROOT / name)
            with self.assertRaisesRegex(ValueError, "verification drift"):
                Fix.apply(target)

    def test_patch_engine_and_proof_identity(self):
        self.assertEqual(
            MANIFEST["patch_sha256"],
            hashlib.sha256((Fix.HERE / "runtime.patch").read_bytes()).hexdigest(),
        )
        self.assertEqual(
            MANIFEST["engine_sha256"],
            hashlib.sha256((Fix.HERE.parent / "recipe/apply.py").read_bytes()).hexdigest(),
        )
        for name, expected in MANIFEST["verification_files"].items():
            self.assertEqual(expected, hashlib.sha256((Fix.ROOT / name).read_bytes()).hexdigest())

    def test_runtime_contract_is_explicit(self):
        source = (Fix.ROOT / "src/java.base/share/classes/java/lang/String.java").read_text()
        self.assertIn("storage != null && locale.equals(Locale.ROOT)", source)
        self.assertIn("M3StringFacts prepared = storage.facts();", source)
        self.assertIn("if (prepared.ascii) {", source)
        self.assertIn("storage.asciiCase(false)", source)
        self.assertIn("storage.asciiCase(true)", source)


if __name__ == "__main__":
    unittest.main()
