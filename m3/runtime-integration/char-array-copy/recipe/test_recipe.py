#!/usr/bin/env python3
"""Exercise historical atom reversibility plus current superseding fixed-point custody."""
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
    @staticmethod
    def current_name(name):
        hashes = MANIFEST["files"][name]
        source = RECIPE.ROOT / name
        if source.is_file():
            return name
        replacement = hashes.get("superseded_by")
        if replacement is None:
            return name
        return replacement["path"]

    def seed_current(self, target):
        for name in MANIFEST["files"]:
            current_name = self.current_name(name)
            source = RECIPE.ROOT / current_name
            target_file = target / current_name
            target_file.parent.mkdir(parents=True, exist_ok=True)
            target_file.write_bytes(source.read_bytes())

    def snapshot(self, target):
        return {
            self.current_name(name): (target / self.current_name(name)).read_bytes()
            for name in MANIFEST["files"]
        }

    def test_current_canonical_tree_is_sealed_superseding_fixed_point(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed_current(target)
            before = self.snapshot(target)
            self.assertEqual("superseded", RECIPE.apply(target, check=True))
            self.assertEqual("superseded", RECIPE.apply(target))
            self.assertEqual("superseded", RECIPE.apply(target, reverse=True))
            self.assertEqual(before, self.snapshot(target))

    def test_historical_transform_atoms_remain_forward_reverse_reversible(self):
        engine_before = ("prefix\n" + RECIPE.OLD_COPY + "suffix\n").encode("utf-8")
        engine_after = RECIPE.transform(RECIPE.ENGINE, engine_before)
        self.assertIn(RECIPE.NEW_COPY.encode("utf-8"), engine_after)
        self.assertEqual(
            engine_before,
            RECIPE.transform(RECIPE.ENGINE, engine_after, reverse=True))

        test_before = (
            "prefix\n"
            + RECIPE.OLD_CALL
            + "\nseparator\n"
            + RECIPE.OLD_HELPER
            + "suffix\n"
        ).encode("utf-8")
        test_after = RECIPE.transform(RECIPE.TEST, test_before)
        self.assertIn(RECIPE.NEW_CALL.encode("utf-8"), test_after)
        self.assertIn(RECIPE.NEW_HELPER.encode("utf-8"), test_after)
        self.assertEqual(
            test_before,
            RECIPE.transform(RECIPE.TEST, test_after, reverse=True))

    def test_each_unknown_drift_is_refused_without_partial_write(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed_current(target)
            for name in MANIFEST["files"]:
                current_name = self.current_name(name)
                path = target / current_name
                original = path.read_bytes()
                path.write_bytes(original + b"\n")
                snapshot = self.snapshot(target)
                with self.assertRaisesRegex(
                        ValueError, "source drift|superseded owner drift"):
                    RECIPE.apply(target)
                self.assertEqual(snapshot, self.snapshot(target))
                path.write_bytes(original)

    def test_symlink_target_is_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed_current(target)
            first_name = self.current_name(RECIPE.ENGINE)
            first = target / first_name
            first.unlink()
            first.symlink_to(RECIPE.ROOT / first_name)
            with self.assertRaisesRegex(ValueError, "symlink path"):
                RECIPE.apply(target)

    def test_git_blob_uses_the_repository_nul_framing(self):
        source = RECIPE.ROOT / "src/java.base/share/classes/java/lang/M3String.java"
        self.assertEqual(
            "70d1d03c109126fdda36c54da21b484bbe368fd9",
            RECIPE.git_blob(source.read_bytes()),
        )

    def test_manifest_superseded_hashes_are_current_exact_sources(self):
        for name, hashes in MANIFEST["files"].items():
            source = RECIPE.ROOT / name
            replacement = hashes.get("superseded_by")
            if replacement is not None and not source.exists():
                replacement_source = RECIPE.ROOT / replacement["path"]
                self.assertTrue(replacement_source.is_file(), replacement["path"])
                self.assertEqual(
                    replacement["git_blob"],
                    RECIPE.git_blob(replacement_source.read_bytes()),
                    replacement["path"],
                )
                continue
            actual = RECIPE.digest(source.read_bytes())
            self.assertIn(actual, hashes.get("superseded", []), name)
            self.assertNotEqual(hashes["before"], actual)
            self.assertNotEqual(hashes["after"], actual)

    def test_missing_or_drifted_replacement_owner_refuses_without_recreating_history(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed_current(target)
            replacement = MANIFEST["files"][RECIPE.ENGINE]["superseded_by"]["path"]
            replacement_path = target / replacement
            replacement_path.unlink()
            with self.assertRaisesRegex(ValueError, "superseded owner missing"):
                RECIPE.apply(target)
            self.assertFalse((target / RECIPE.ENGINE).exists())

        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed_current(target)
            replacement = MANIFEST["files"][RECIPE.ENGINE]["superseded_by"]["path"]
            replacement_path = target / replacement
            replacement_path.write_bytes(replacement_path.read_bytes() + b"\n")
            with self.assertRaisesRegex(ValueError, "superseded owner drift"):
                RECIPE.apply(target)
            self.assertFalse((target / RECIPE.ENGINE).exists())


if __name__ == "__main__":
    unittest.main()
