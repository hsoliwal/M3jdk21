#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Prove current runtime custody, including exact superseded-owner fixed points."""
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
spec = importlib.util.spec_from_file_location("integration_recipe", HERE / "apply.py")
recipe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(recipe)
manifest = json.loads((HERE / "manifest.json").read_text())


class RecipeTest(unittest.TestCase):
    def seed_current(self, target):
        for name in manifest["files"]:
            source = ROOT / name
            destination = target / name
            destination.parent.mkdir(parents=True, exist_ok=True)
            if source.exists():
                destination.write_bytes(source.read_bytes())

    def snapshot(self, target):
        return {
            name: (target / name).read_bytes() if (target / name).exists() else None
            for name in manifest["files"]
        }

    def test_manifest_is_upstream_to_master_v2(self):
        self.assertEqual("M3_RUNTIME_INTEGRATION_RECIPE_V2", manifest["schema"])
        self.assertEqual(manifest["base_commit"], manifest["upstream_commit"])
        self.assertEqual(66, len(manifest["files"]))
        for name, hashes in manifest["files"].items():
            self.assertIsNotNone(hashes["after"], name)
            superseded = hashes.get("superseded", [])
            self.assertEqual(len(superseded), len(set(superseded)), name)
            if hashes["before"] is None:
                self.assertTrue(superseded, name)
            else:
                self.assertNotIn(hashes["before"], superseded, name)
                self.assertNotIn(hashes["after"], superseded, name)

    def test_current_tree_is_exact_superseded_fixed_point(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed_current(target)
            before = self.snapshot(target)
            self.assertEqual("superseded", recipe.apply(target, check=True))
            self.assertEqual(before, self.snapshot(target))
            self.assertEqual("superseded", recipe.apply(target))
            self.assertEqual(before, self.snapshot(target))
            self.assertEqual("superseded", recipe.apply(target, reverse=True))
            self.assertEqual(before, self.snapshot(target))

    def test_every_current_target_matches_after_or_exact_superseded(self):
        for name, hashes in manifest["files"].items():
            source = ROOT / name
            actual = recipe.digest(source.read_bytes()) if source.exists() else None
            allowed = {hashes["after"], *hashes.get("superseded", [])}
            self.assertIn(actual, allowed, name)
            if hashes["before"] is None:
                self.assertIn(actual, hashes.get("superseded", []), name)

    def test_patch_bytes_remain_content_addressed(self):
        patch = HERE / "runtime.patch"
        self.assertEqual(manifest["patch_sha256"], recipe.digest(patch.read_bytes()))
        self.assertNotIn(b"\r", patch.read_bytes())

    def test_each_unknown_drift_is_refused_without_partial_write(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed_current(target)
            for name in manifest["files"]:
                path = target / name
                original = path.read_bytes() if path.exists() else None
                if original is None:
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_bytes(b"drift")
                else:
                    path.write_bytes(original + b"\n")
                before = self.snapshot(target)
                with self.assertRaisesRegex(ValueError, "source drift"):
                    recipe.apply(target)
                self.assertEqual(before, self.snapshot(target))
                if original is None:
                    path.unlink()
                else:
                    path.write_bytes(original)

    def test_modified_utf8_owner_fanin_matches_canonical_donor(self):
        expected = {
            "src/hotspot/share/classfile/javaClasses.cpp",
            "src/hotspot/share/classfile/javaClasses.hpp",
            "src/hotspot/share/classfile/javaClasses.inline.hpp",
            "src/java.base/share/classes/java/lang/M3StringFacts.java",
        }
        self.assertEqual(expected, set(recipe.MUTF8_FANIN))
        observed = {}
        for path, (prior, legacy, current) in recipe.MUTF8_FANIN.items():
            row = manifest["files"][path]
            self.assertEqual(prior, row["after"])
            self.assertEqual([*legacy, current], row["superseded"])
            observed[path] = recipe.digest((ROOT / path).read_bytes())
            self.assertEqual(current, observed[path], path)
        self.assertIsNone(recipe.verify_mutf8_fanin(manifest, observed))

    def test_modified_utf8_partial_and_unreviewed_sources_are_refused(self):
        current = {path: new for path, (_, _, new) in recipe.MUTF8_FANIN.items()}
        historic = {
            path: legacy[-1] if legacy else prior
            for path, (prior, legacy, _) in recipe.MUTF8_FANIN.items()
        }
        self.assertIsNone(recipe.verify_mutf8_fanin(manifest, current))
        self.assertIsNone(recipe.verify_mutf8_fanin(manifest, historic))
        for path in current:
            with self.subTest(path=path, stage="partial"):
                partial = dict(current)
                partial[path] = historic[path]
                with self.assertRaisesRegex(ValueError, "partial modified UTF-8"):
                    recipe.verify_mutf8_fanin(manifest, partial)
            with self.subTest(path=path, stage="unknown"):
                damaged = dict(current)
                damaged[path] = "0" * 64
                with self.assertRaisesRegex(ValueError, "modified UTF-8 source drift"):
                    recipe.verify_mutf8_fanin(manifest, damaged)
            with self.subTest(path=path, stage="receipt"):
                damaged = json.loads(json.dumps(manifest))
                damaged["files"][path]["superseded"].pop()
                with self.assertRaisesRegex(ValueError, "modified UTF-8 recipe seal drift"):
                    recipe.verify_mutf8_fanin(damaged, current)

    def test_symlink_target_is_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed_current(target)
            name = next(iter(manifest["files"]))
            path = target / name
            path.unlink()
            path.symlink_to(ROOT / name)
            with self.assertRaisesRegex(ValueError, "symlink path"):
                recipe.apply(target)


if __name__ == "__main__":
    unittest.main()
