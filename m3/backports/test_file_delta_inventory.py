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
            self.write(repo, "src/hotspot/share/runtime/a3.cpp", "int a3() { return 21; }\n")
            self.write(repo, "src/java.base/share/native/libjava/a3.h", "#define A3 21\n")
            self.write(repo, "remove.txt", "remove\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk21")
            self.git(repo, "tag", "jdk-21+35")

            self.write(repo, "src/B.java", "final class B { int x() { return 2; } }\n")
            self.write(repo, "src/C.java", "final class C {}\n")
            self.write(repo, "src/hotspot/share/runtime/a3.cpp", "int a3() { return 22; }\n")
            self.write(repo, "src/java.base/share/native/libjava/a3.h", "#define A3 22\n")
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

            self.assertTrue(by_path["src/hotspot/share/runtime/a3.cpp"].native_source)
            self.assertFalse(by_path["src/hotspot/share/runtime/a3.cpp"].java_source)
            self.assertEqual(
                "SOURCE_SEALED_NATIVE_PAIR",
                by_path["src/hotspot/share/runtime/a3.cpp"].recipe_lane,
            )
            self.assertTrue(by_path["src/java.base/share/native/libjava/a3.h"].native_source)
            self.assertEqual(
                "SOURCE_SEALED_NATIVE_PAIR",
                by_path["src/java.base/share/native/libjava/a3.h"].recipe_lane,
            )

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
            self.assertIn("SOURCE_SEALED_NATIVE_PAIR", text)
            self.assertIn("native_source", text)

    def test_main_requires_only_selected_donor_refs(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            repo = Path(temp)
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")

            self.write(repo, "src/A.java", "final class A { int x() { return 21; } }\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk21")
            self.git(repo, "tag", "jdk-21+35")

            self.write(repo, "src/A.java", "final class A { int x() { return 24; } }\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk24")
            self.git(repo, "tag", "jdk-24+36")

            output = repo / "jdk24.tsv"
            self.assertEqual(
                0,
                self.mod.main(
                    (
                        "--repo",
                        str(repo),
                        "--release",
                        "24",
                        "--out",
                        str(output),
                    )
                ),
            )
            text = output.read_text(encoding="utf-8")
            self.assertIn("jdk-24+36", text)
            self.assertNotIn("jdk-22+36", text)
            self.assertNotIn("jdk-23+37", text)

    def test_main_accepts_exact_donor_ref_for_one_release(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            repo = Path(temp)
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")

            path = "src/A.java"
            self.write(repo, path, "final class A { int x() { return 21; } }\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk21")
            self.git(repo, "tag", "jdk-21+35")

            self.write(repo, path, "final class A { int x() { return 22; } }\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "feature")
            donor = subprocess.check_output(
                ("git", "-C", str(repo), "rev-parse", "HEAD"), text=True
            ).strip()

            self.write(repo, path, "final class A { int x() { return 2200; } }\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "later-ga")
            self.git(repo, "tag", "jdk-22+36")

            output = repo / "exact.tsv"
            self.assertEqual(
                0,
                self.mod.main(
                    (
                        "--repo",
                        str(repo),
                        "--release",
                        "22",
                        "--donor-ref",
                        donor,
                        "--out",
                        str(output),
                    )
                ),
            )
            text = output.read_text(encoding="utf-8")
            self.assertIn(donor, text)
            self.assertNotIn("jdk-22+36", text)
            self.assertIn("MODIFIED", text)

    def test_exact_donor_ref_requires_one_release(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            repo = Path(temp)
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")
            self.write(repo, "A.java", "final class A {}\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "base")
            self.git(repo, "tag", "jdk-21+35")
            head = subprocess.check_output(
                ("git", "-C", str(repo), "rev-parse", "HEAD"), text=True
            ).strip()
            with self.assertRaises(SystemExit):
                self.mod.main(
                    (
                        "--repo",
                        str(repo),
                        "--release",
                        "22",
                        "--release",
                        "23",
                        "--donor-ref",
                        head,
                    )
                )

    def test_fixed_jdk21_baseline_is_used_for_every_donor(self) -> None:
        self.assertEqual((21, "jdk-21+35"), self.mod.BASELINE)
        self.assertEqual(
            (
                (22, "jdk-22+36"),
                (23, "jdk-23+37"),
                (24, "jdk-24+36"),
                (25, "jdk-25+36"),
                (26, "jdk-26+35"),
                (27, "jdk-27+35"),
            ),
            self.mod.DONORS,
        )


if __name__ == "__main__":
    unittest.main()
