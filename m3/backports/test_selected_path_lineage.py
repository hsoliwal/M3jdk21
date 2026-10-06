#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


MODULE_PATH = Path(__file__).with_name("selected_path_lineage.py")
SPEC = importlib.util.spec_from_file_location("m3_selected_path_lineage", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
lineage = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = lineage
SPEC.loader.exec_module(lineage)


class SelectedPathLineageTest(unittest.TestCase):
    def repo(self) -> tuple[Path, str, str, str]:
        root = Path(tempfile.mkdtemp(prefix="m3-lineage-"))
        subprocess.run(["git", "-C", str(root), "init", "-q"], check=True)
        subprocess.run(["git", "-C", str(root), "config", "user.email", "m3@example.invalid"], check=True)
        subprocess.run(["git", "-C", str(root), "config", "user.name", "M3 Test"], check=True)

        (root / "a.txt").write_text("a0\n", encoding="utf-8")
        (root / "b.txt").write_text("b0\n", encoding="utf-8")
        subprocess.run(["git", "-C", str(root), "add", "."], check=True)
        subprocess.run(["git", "-C", str(root), "commit", "-q", "-m", "base"], check=True)
        base = self.sha(root)

        (root / "a.txt").write_text("a1\n", encoding="utf-8")
        subprocess.run(["git", "-C", str(root), "commit", "-q", "-am", "allowed"], check=True)
        allowed = self.sha(root)

        (root / "b.txt").write_text("b1\n", encoding="utf-8")
        subprocess.run(["git", "-C", str(root), "commit", "-q", "-am", "unexpected"], check=True)
        unexpected = self.sha(root)
        return root, base, allowed, unexpected

    @staticmethod
    def sha(root: Path) -> str:
        return subprocess.check_output(
            ["git", "-C", str(root), "rev-parse", "HEAD"], text=True
        ).strip()

    def test_audit_is_path_sorted_and_marks_allowed(self) -> None:
        root, base, allowed, end = self.repo()
        rows = lineage.audit(root, base, end, ["b.txt", "a.txt"], {allowed})
        self.assertEqual([allowed, end], [row.commit for row in rows])
        self.assertTrue(rows[0].allowed)
        self.assertFalse(rows[1].allowed)
        self.assertEqual(("a.txt",), rows[0].paths)
        self.assertEqual(("b.txt",), rows[1].paths)
        self.assertEqual(lineage.semantic_root(rows), lineage.semantic_root(rows))

    def test_missing_allowed_commit_fails_closed(self) -> None:
        root, base, _allowed, end = self.repo()
        with self.assertRaisesRegex(ValueError, "allowed commit"):
            lineage.audit(root, base, end, ["a.txt"], {end})

    def test_non_ancestor_range_fails_closed(self) -> None:
        root, base, allowed, end = self.repo()
        with self.assertRaisesRegex(ValueError, "not an ancestor"):
            lineage.audit(root, end, base, ["a.txt"], {allowed})

    def test_paths_are_canonical_sorted_unique_and_nonempty(self) -> None:
        root, _base, _allowed, _end = self.repo()
        paths = root / "paths.txt"
        paths.write_text("b.txt\na.txt\n", encoding="utf-8")
        self.assertEqual(["a.txt", "b.txt"], lineage.load_paths(paths))

        paths.write_text("a.txt\na.txt\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "duplicate"):
            lineage.load_paths(paths)

        paths.write_text("../a.txt\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "noncanonical"):
            lineage.load_paths(paths)

        paths.write_text("", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "empty"):
            lineage.load_paths(paths)

    def test_write_is_deterministic(self) -> None:
        root, base, allowed, end = self.repo()
        rows = lineage.audit(root, base, end, ["a.txt", "b.txt"], {allowed, end})
        first = root / "first.tsv"
        second = root / "second.tsv"
        first_root = lineage.write(rows, first, root / "first.root")
        second_root = lineage.write(rows, second, root / "second.root")
        self.assertEqual(first.read_bytes(), second.read_bytes())
        self.assertEqual(first_root, second_root)
        self.assertRegex(first_root, r"^[0-9a-f]{64}$")


if __name__ == "__main__":
    unittest.main()
