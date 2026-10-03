#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import csv
import importlib.util
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


def load_module():
    path = Path(__file__).with_name("generate_recipe_crates.py")
    spec = importlib.util.spec_from_file_location("m3_generate_recipe_crates", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class GenerateRecipeCratesTest(unittest.TestCase):
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

    def test_verbatim_pairs_generate_hash_pinned_openrewrite_crate(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = root / "repo"
            out = root / "out"
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")

            modified = "src/java.base/share/classes/java/lang/A.java"
            added = "src/java.base/share/classes/java/lang/B.java"
            removed = "src/java.base/share/classes/java/lang/Removed.java"

            self.write(repo, modified, "package java.lang; final class A { int x() { return 21; } }\n")
            self.write(repo, removed, "package java.lang; final class Removed {}\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk21")
            self.git(repo, "tag", "jdk-21+35")

            self.write(repo, modified, "package java.lang; final class A { int x() { return 22; } }\n")
            self.write(repo, added, "package java.lang; final class B {}\n")
            (repo / removed).unlink()
            self.git(repo, "add", "-A")
            self.git(repo, "commit", "-q", "-m", "jdk22")
            self.git(repo, "tag", "jdk-22+36")

            candidates, exclusions = self.mod.candidates(
                repo, 22, selected=None, all_candidates=True
            )
            self.assertEqual([added, modified], [row.path for row in candidates])
            self.assertEqual(
                [(removed, "TYPED_EXCLUSION_AUTOMATIC_REMOVAL")],
                exclusions,
            )

            crates = self.mod.materialize(out, 22, candidates, exclusions)
            self.assertEqual(["jdk22-0001"], crates)

            crate = (
                out
                / self.mod.RESOURCE_ROOT
                / "jdk22-0001"
            )
            with (crate / "manifest.tsv").open(
                "r", encoding="utf-8", newline=""
            ) as handle:
                rows = list(csv.reader(handle, delimiter="\t"))

            self.assertEqual(2, len(rows))
            self.assertEqual(added, rows[0][0])
            self.assertEqual("ABSENT", rows[0][1])
            self.assertEqual(modified, rows[1][0])
            self.assertEqual(64, len(rows[1][1]))
            self.assertEqual(64, len(rows[0][2]))
            self.assertEqual(64, len(rows[1][2]))
            self.assertTrue((crate / rows[0][3]).is_file())
            self.assertTrue((crate / rows[1][3]).is_file())

            yaml = (
                out
                / self.mod.YAML_ROOT
                / "m3-jdk22-candidate-backports.yml"
            ).read_text(encoding="utf-8")
            self.assertIn(
                "com.m3.rewrite.backport.M3Jdk21HashPinnedSnapshotRecipe",
                yaml,
            )
            self.assertIn("crateName: jdk22-0001", yaml)
            self.assertIn(
                "name: com.m3.generated.jdk22.CandidateBackports",
                yaml,
            )

            exclusions_text = (out / "EXCLUSIONS.tsv").read_text(encoding="utf-8")
            self.assertIn("TYPED_EXCLUSION_AUTOMATIC_REMOVAL", exclusions_text)

    def test_requires_explicit_selection_or_all_candidates(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            repo = Path(temp)
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")
            self.write(repo, "src/java.base/share/classes/A.java", "final class A {}\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "base")
            self.git(repo, "tag", "jdk-21+35")
            self.git(repo, "tag", "jdk-22+36")

            with self.assertRaises(ValueError):
                self.mod.candidates(
                    repo, 22, selected=None, all_candidates=False
                )


if __name__ == "__main__":
    unittest.main()
