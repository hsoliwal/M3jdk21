# SPDX-License-Identifier: Apache-2.0
"""Execute the retained patch backend on exact owned fixtures; not a HotSpot build."""
import hashlib
import json
import tempfile
import unittest
from pathlib import Path
import Fix

MANIFEST = json.loads((Fix.HERE / "manifest.json").read_text())


class FixTest(unittest.TestCase):
    def seed(self, target):
        for name in MANIFEST["files"]:
            path = target / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes((Fix.ROOT / name).read_bytes())
        backend = Fix.engine()
        if backend.apply(target, check=True) == "after":
            self.assertEqual("before", backend.apply(target, reverse=True))

    @staticmethod
    def snapshot(target):
        return {name: (target / name).read_bytes() if (target / name).exists() else None
                for name in MANIFEST["files"]}

    def test_exact_apply_fixedpoint_and_reverse(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed(target)
            original = self.snapshot(target)
            backend = Fix.engine()
            self.assertEqual("before", backend.apply(target, check=True))
            self.assertEqual(original, self.snapshot(target))
            self.assertEqual("after", backend.apply(target))
            after = self.snapshot(target)
            for name, data in after.items():
                self.assertEqual(MANIFEST["files"][name]["after"], hashlib.sha256(data).hexdigest())
            self.assertEqual("after", backend.apply(target))
            self.assertEqual(after, self.snapshot(target))
            self.assertEqual("before", backend.apply(target, reverse=True))
            self.assertEqual(original, self.snapshot(target))

    def test_each_drift_refuses_before_any_write(self):
        for name in MANIFEST["files"]:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as folder:
                target = Path(folder)
                self.seed(target)
                path = target / name
                path.write_bytes(path.read_bytes() + b"\n// drift\n")
                before = self.snapshot(target)
                with self.assertRaisesRegex(ValueError, "source drift"):
                    Fix.engine().apply(target)
                self.assertEqual(before, self.snapshot(target))

    def test_missing_file_refuses(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed(target)
            (target / next(iter(MANIFEST["files"]))).unlink()
            before = self.snapshot(target)
            with self.assertRaisesRegex(ValueError, "source drift"):
                Fix.engine().apply(target)
            self.assertEqual(before, self.snapshot(target))

    def test_symlink_refuses(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed(target)
            name = next(iter(MANIFEST["files"]))
            path = target / name
            path.unlink()
            path.symlink_to(Fix.ROOT / name)
            with self.assertRaisesRegex(ValueError, "symlink path"):
                Fix.engine().apply(target)

    def test_mixed_state_refuses(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed(target)
            before = self.snapshot(target)
            backend = Fix.engine()
            backend.apply(target)
            name = next(iter(before))
            (target / name).write_bytes(before[name])
            mixed = self.snapshot(target)
            with self.assertRaisesRegex(ValueError, "mixed runtime patch state"):
                backend.apply(target)
            self.assertEqual(mixed, self.snapshot(target))

    def test_patch_and_dependency_identity(self):
        self.assertEqual(MANIFEST["patch_sha256"],
                         hashlib.sha256((Fix.HERE / "runtime.patch").read_bytes()).hexdigest())
        self.assertEqual(MANIFEST["engine_sha256"],
                         hashlib.sha256((Fix.HERE.parent / "recipe/apply.py").read_bytes()).hexdigest())
        original = Fix.engine()
        self.assertEqual(Fix.HERE, original.HERE)
        self.assertEqual(Fix.HERE.parent / "recipe/apply.py", Path(original.__file__))


if __name__ == "__main__":
    unittest.main()
