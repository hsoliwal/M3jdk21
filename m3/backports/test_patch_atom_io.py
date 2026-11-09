#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
"""Differential and refusal proof for FILE patch atom filesystem custody."""

from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path


def load():
    path = Path(__file__).with_name("patch_atom_io.py")
    spec = importlib.util.spec_from_file_location("m3_patch_atom_io", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class PatchAtomIoTest(unittest.TestCase):
    def setUp(self) -> None:
        self.io = load()

    def test_path_refuses_traversal_absolute_and_control_characters(self) -> None:
        bad = (
            "../outside", "a//b", "/etc/passwd", "C:/Windows", "a\\b",
            "a/./b", "a/../b", ".git/config", "a\tb", "a\nb", "a\0b", "",
        )
        for value in bad:
            with self.subTest(path=repr(value)), self.assertRaises(ValueError):
                self.io.relative_git_path(value)
        self.assertEqual(
            "src/java.base/share/classes/X.java",
            self.io.relative_git_path("src/java.base/share/classes/X.java"),
        )

    def test_output_and_target_cannot_alias_source(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            donor, target = root / "donor", root / "target"
            donor.mkdir()
            target.mkdir()
            for output in (target, target / "inside", donor / "inside", root):
                with self.subTest(output=output), self.assertRaises(ValueError):
                    self.io.safe_output_root(output, donor, target)
            self.assertEqual(
                root / "receipt",
                self.io.safe_output_root(root / "receipt", donor, target),
            )

    def test_symlink_and_hardlink_inputs_are_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / "src").mkdir()
            owner = root / "src/A.java"
            owner.write_bytes(b"A")
            (root / "src/B.java").hardlink_to(owner)
            with self.assertRaises(ValueError):
                self.io.target_blob(root, "src/A.java")
            owner.unlink()
            (root / "src/External.java").symlink_to("/etc/hosts")
            with self.assertRaises(ValueError):
                self.io.target_blob(root, "src/External.java")
            self.assertEqual(
                (False, b""), self.io.target_blob(root, "src/Missing.java")
            )

    def test_output_is_idempotent_and_refuses_alias_or_drift(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp) / "out"
            root.mkdir()
            self.io.write_exact(root, "patches/0001.patch", b"one")
            self.io.write_exact(root, "patches/0001.patch", b"one")
            with self.assertRaises(ValueError):
                self.io.write_exact(root, "patches/0001.patch", b"two")
            target = root / "patches/0002.patch"
            target.symlink_to(root / "patches/0001.patch")
            with self.assertRaises(ValueError):
                self.io.write_exact(root, "patches/0002.patch", b"one")
            target.unlink()
            target.hardlink_to(root / "patches/0001.patch")
            with self.assertRaises(ValueError):
                self.io.write_exact(root, "patches/0002.patch", b"one")

    def test_symlink_output_root_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            for name in ("donor", "target", "real"):
                (root / name).mkdir()
            (root / "alias").symlink_to(root / "real")
            with self.assertRaises(ValueError):
                self.io.safe_output_root(
                    root / "alias", root / "donor", root / "target"
                )


if __name__ == "__main__":
    unittest.main()
