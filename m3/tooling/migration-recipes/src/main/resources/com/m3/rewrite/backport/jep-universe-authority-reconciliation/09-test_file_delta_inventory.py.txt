#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import importlib.util
import io
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


def load_module():
    path = Path(__file__).with_name("file_delta_inventory.py")
    spec = importlib.util.spec_from_file_location("m3_file_delta_inventory", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class FileDeltaInventoryTest(unittest.TestCase):
    def setUp(self) -> None:
        self.mod = load_module()

    @staticmethod
    def git(repo: Path, *args: str) -> None:
        subprocess.run(
            ("git", "-C", str(repo), *args),
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )

    @staticmethod
    def write(repo: Path, path: str, text: str) -> None:
        target = repo / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding="utf-8")

    def test_verbatim_union_marks_same_modified_added_and_removed(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            repo = Path(temp)
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")

            self.write(repo, "src/A.java", "final class A {}\n")
            self.write(repo, "src/B.java", "final class B { int x() { return 1; } }\n")
            self.write(repo, "conf/same.txt", "same\n")
            self.write(repo, "remove.txt", "remove\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk21")
            self.git(repo, "tag", "jdk-21+35")

            self.write(repo, "src/B.java", "final class B { int x() { return 2; } }\n")
            self.write(repo, "src/C.java", "final class C {}\n")
            (repo / "remove.txt").unlink()
            self.git(repo, "add", "-A")
            self.git(repo, "commit", "-q", "-m", "jdk22")
            self.git(repo, "tag", "jdk-22+36")

            for tag in (
                "jdk-23+37",
                "jdk-24+36",
                "jdk-25+36",
                "jdk-26+35",
                "jdk-27+35",
            ):
                self.git(repo, "tag", tag)

            self.mod._verify(repo, ("jdk-21+35", "jdk-22+36"))
            rows = self.mod.compare(repo, (22,))
            by_path = {row.path: row for row in rows}

            self.assertEqual("SAME", by_path["src/A.java"].status)
            self.assertEqual("NO_RECIPE", by_path["src/A.java"].recipe_lane)

            self.assertEqual("MODIFIED", by_path["src/B.java"].status)
            self.assertEqual("OPENREWRITE_JAVA_PAIR", by_path["src/B.java"].recipe_lane)
            self.assertNotEqual(
                by_path["src/B.java"].baseline_oid,
                by_path["src/B.java"].donor_oid,
            )

            self.assertEqual("ADDED", by_path["src/C.java"].status)
            self.assertEqual("OPENREWRITE_ADD_JAVA", by_path["src/C.java"].recipe_lane)
            self.assertEqual("", by_path["src/C.java"].baseline_oid)

            self.assertEqual("REMOVED", by_path["remove.txt"].status)
            self.assertEqual("REVIEW_REMOVAL", by_path["remove.txt"].recipe_lane)
            self.assertEqual("", by_path["remove.txt"].donor_oid)

            self.assertEqual("SAME", by_path["conf/same.txt"].status)
            self.assertEqual(
                by_path["conf/same.txt"].baseline_oid,
                by_path["conf/same.txt"].donor_oid,
            )

            output = io.StringIO()
            self.mod.write_tsv(rows, output)
            text = output.getvalue()
            self.assertIn("baseline_oid", text)
            self.assertIn("donor_oid", text)
            self.assertIn("OPENREWRITE_JAVA_PAIR", text)

    def test_fixed_jdk21_baseline_and_authority_defined_donors_are_used(self) -> None:
        self.assertEqual((21, "jdk-21+35"), self.mod.BASELINE)
        donors = self.mod.default_donors()
        self.assertEqual("jdk-22+36", donors[22])
        self.assertEqual("jdk-26+35", donors[26])
        self.assertEqual(
            "f3701c80216900f3ded26f9de1befe43813be95c",
            donors[27],
        )
        self.assertFalse(donors[27].startswith("jdk-27+"))

    def test_compare_accepts_commit_pinned_snapshot_ref(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            repo = Path(temp)
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")
            self.write(repo, "src/A.java", "final class A { int v = 21; }\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk21")
            self.git(repo, "tag", "jdk-21+35")
            self.write(repo, "src/A.java", "final class A { int v = 27; }\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "snapshot")
            snapshot = subprocess.run(
                ("git", "-C", str(repo), "rev-parse", "HEAD"),
                check=True,
                stdout=subprocess.PIPE,
                text=True,
            ).stdout.strip()

            rows = self.mod.compare(repo, (27,), {27: snapshot})
            self.assertEqual(1, len(rows))
            self.assertEqual(snapshot, rows[0].donor_ref)
            self.assertEqual("MODIFIED", rows[0].status)


if __name__ == "__main__":
    unittest.main()
