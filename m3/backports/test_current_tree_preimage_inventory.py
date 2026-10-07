# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import hashlib
import io
import tempfile
import unittest
from pathlib import Path

import current_tree_preimage_inventory as inventory


class CurrentTreePreimageInventoryTest(unittest.TestCase):
    def test_inventory_hashes_present_and_records_absent_without_mutation(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            java = root / "src/main/java/demo/A.java"
            java.parent.mkdir(parents=True)
            java.write_text("class A {}\n", encoding="utf-8")

            rows = inventory.inventory(
                root,
                ["missing/Foo.hpp", "src/main/java/demo/A.java"],
            )
            self.assertEqual(2, len(rows))
            self.assertEqual("ABSENT", rows[0].status)
            self.assertEqual("NATIVE", rows[0].kind)
            self.assertEqual("ABSENT", rows[0].sha256)
            self.assertEqual(0, rows[0].bytes)

            expected = hashlib.sha256(java.read_bytes()).hexdigest()
            self.assertEqual("PRESENT", rows[1].status)
            self.assertEqual("JAVA", rows[1].kind)
            self.assertEqual(expected, rows[1].sha256)
            self.assertGreater(rows[1].bytes, 0)
            self.assertEqual("class A {}\n", java.read_text(encoding="utf-8"))

    def test_selected_paths_requires_sorted_unique_canonical_input(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            valid = root / "valid.txt"
            valid.write_text("a/A.java\nb/B.cpp\n", encoding="utf-8")
            self.assertEqual(
                ["a/A.java", "b/B.cpp"],
                inventory.selected_paths(valid),
            )

            duplicate = root / "duplicate.txt"
            duplicate.write_text("a/A.java\na/A.java\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "duplicates"):
                inventory.selected_paths(duplicate)

            unsorted = root / "unsorted.txt"
            unsorted.write_text("b/B.cpp\na/A.java\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "sorted"):
                inventory.selected_paths(unsorted)

            escape = root / "escape.txt"
            escape.write_text("../A.java\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "noncanonical"):
                inventory.selected_paths(escape)

    def test_symlink_and_non_file_preimages_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / "target.txt"
            target.write_text("x", encoding="utf-8")
            link = root / "link.txt"
            try:
                link.symlink_to(target)
            except (OSError, NotImplementedError):
                self.skipTest("symlinks unavailable")
            with self.assertRaisesRegex(ValueError, "symlink"):
                inventory.inventory(root, ["link.txt"])

            directory_path = root / "folder"
            directory_path.mkdir()
            with self.assertRaisesRegex(ValueError, "non-file"):
                inventory.inventory(root, ["folder"])

    def test_tsv_is_deterministic(self) -> None:
        rows = [
            inventory.Preimage("a/A.java", "PRESENT", "a" * 64, 10, "JAVA"),
            inventory.Preimage("b/B.hpp", "ABSENT", "ABSENT", 0, "NATIVE"),
        ]
        first = io.StringIO()
        second = io.StringIO()
        inventory.write_tsv(rows, first)
        inventory.write_tsv(rows, second)
        self.assertEqual(first.getvalue(), second.getvalue())
        self.assertEqual(
            "path\tstatus\tsha256\tbytes\tkind\n",
            first.getvalue().splitlines(keepends=True)[0],
        )


if __name__ == "__main__":
    unittest.main()
