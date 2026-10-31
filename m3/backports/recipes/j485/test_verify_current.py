#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0

import csv
import importlib.util
import tempfile
import unittest
from pathlib import Path

MODULE = Path(__file__).with_name("verify_current.py")
SPEC = importlib.util.spec_from_file_location("j485_verify_current", MODULE)
assert SPEC is not None and SPEC.loader is not None
mod = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(mod)


class VerifyCurrentTest(unittest.TestCase):
    def test_guard_and_mutation_bind_actual_tree(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            resources = root / (
                "m3/tooling/migration-recipes/src/main/resources/"
                "com/m3/rewrite/backport/jdk21-hash-pinned"
            )
            rows = []
            for index, (mode, before_text, after_text) in enumerate(
                    [("GUARD", "same\n", "same\n"), ("MUTATE", "old\n", "new\n")], 1):
                path = (
                    "src/java.base/share/classes/java/util/stream/X.java"
                    if index == 1
                    else "test/jdk/java/util/stream/XTest.java"
                )
                target = root / path
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(after_text, encoding="utf-8")
                crate_name = f"jdk24-j485-cur-{index:02d}"
                crate = resources / crate_name
                crate.mkdir(parents=True, exist_ok=True)
                before = crate / "before.java.txt"
                after = crate / "after.java.txt"
                before.write_text(before_text, encoding="utf-8")
                after.write_text(after_text, encoding="utf-8")
                rows.append({
                    "crate": crate_name,
                    "path": path,
                    "mode": mode,
                    "before_sha256": mod.sha256(before),
                    "after_sha256": mod.sha256(after),
                    "before_resource": before.name,
                    "after_resource": after.name,
                })
            catalogue = resources / "j485-current-catalogue.tsv"
            with catalogue.open("w", encoding="utf-8", newline="") as handle:
                writer = csv.DictWriter(handle, fieldnames=list(rows[0]), delimiter="\t")
                writer.writeheader()
                writer.writerows(rows)

            self.assertEqual(
                (1, 1),
                mod.verify(root, expected_rows=2, expected_guards=1, expected_mutations=1),
            )

            (root / rows[1]["path"]).write_text("drift\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "current-tree drift"):
                mod.verify(root, expected_rows=2, expected_guards=1, expected_mutations=1)


if __name__ == "__main__":
    unittest.main()
