#!/usr/bin/env python3
"""Exercise forward/reverse, idempotence, and no-write drift rejection."""
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("char_array_copy_recipe", HERE / "apply.py")
RECIPE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RECIPE)
MANIFEST = json.loads((HERE / "manifest.json").read_text())


class CharArrayCopyRecipeTest(unittest.TestCase):
    def seed(self, target):
        for name in MANIFEST["files"]:
            source = RECIPE.ROOT / name
            target_file = target / name
            target_file.parent.mkdir(parents=True, exist_ok=True)
            target_file.write_bytes(source.read_bytes())
        self.assertEqual("before", RECIPE.apply(target, reverse=True))

    def snapshot(self, target):
        return {name: (target / name).read_bytes() for name in MANIFEST["files"]}

    def test_forward_reverse_idempotence(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed(target)
            before = self.snapshot(target)
            self.assertEqual("after", RECIPE.apply(target))
            after = self.snapshot(target)
            self.assertEqual("after", RECIPE.apply(target))
            self.assertEqual(after, self.snapshot(target))
            self.assertEqual("before", RECIPE.apply(target, reverse=True))
            self.assertEqual(before, self.snapshot(target))
            self.assertEqual("before", RECIPE.apply(target, reverse=True))

    def test_each_drift_refused_without_partial_write(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed(target)
            for name in MANIFEST["files"]:
                path = target / name
                original = path.read_bytes()
                path.write_bytes(original + b"\n")
                snapshot = self.snapshot(target)
                with self.assertRaisesRegex(ValueError, "source drift"):
                    RECIPE.apply(target)
                self.assertEqual(snapshot, self.snapshot(target))
                path.write_bytes(original)

    def test_mixed_state_and_symlink_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed(target)
            first = target / RECIPE.ENGINE
            first.write_bytes(RECIPE.transform(RECIPE.ENGINE, first.read_bytes()))
            snapshot = self.snapshot(target)
            with self.assertRaisesRegex(ValueError, "mixed source state"):
                RECIPE.apply(target)
            self.assertEqual(snapshot, self.snapshot(target))
            first.unlink()
            first.symlink_to(RECIPE.ROOT / RECIPE.ENGINE)
            with self.assertRaisesRegex(ValueError, "symlink path"):
                RECIPE.apply(target)

    def test_original_runtime_recipe_remains_replayable(self):
        old_dir = RECIPE.ROOT / "m3/runtime-integration/recipe"
        old_spec = importlib.util.spec_from_file_location("original_runtime_recipe",
                                                           old_dir / "apply.py")
        original = importlib.util.module_from_spec(old_spec)
        old_spec.loader.exec_module(original)
        old_manifest = json.loads((old_dir / "manifest.json").read_text())
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            for name in set(old_manifest["files"]) | set(MANIFEST["files"]):
                source = RECIPE.ROOT / name
                destination = target / name
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(source.read_bytes())
            self.assertEqual("before", RECIPE.apply(target, reverse=True))
            self.assertEqual("after", original.apply(target, check=True))
            self.assertEqual("before", original.apply(target, reverse=True))
            self.assertEqual("after", original.apply(target))
            self.assertEqual("after", RECIPE.apply(target))


if __name__ == "__main__":
    unittest.main()
