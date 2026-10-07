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
            self.assertEqual(sorted([added, modified]), [row.path for row in candidates])
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
            self.assertEqual(modified, rows[0][0])
            self.assertEqual(64, len(rows[0][1]))
            self.assertEqual(added, rows[1][0])
            self.assertEqual("ABSENT", rows[1][1])
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


    def test_crate_size_one_emits_true_file_atomic_crates(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            out = Path(temp) / "out"
            first_text = "final class A {}\n"
            second_text = "final class B {}\n"
            candidates = [
                self.mod.Candidate(
                    path="src/java.base/share/classes/p/A.java",
                    status="MODIFIED",
                    before_sha256=self.mod._sha256_text("final class A { int x; }\n"),
                    after_sha256=self.mod._sha256_text(first_text),
                    after_text=first_text,
                ),
                self.mod.Candidate(
                    path="src/java.base/share/classes/p/B.java",
                    status="ADDED",
                    before_sha256="ABSENT",
                    after_sha256=self.mod._sha256_text(second_text),
                    after_text=second_text,
                ),
            ]

            crates = self.mod.materialize(
                out,
                22,
                candidates,
                exclusions=[],
                crate_size=1,
            )

            self.assertEqual(["jdk22-0001", "jdk22-0002"], crates)
            for ordinal, expected_path in enumerate(
                [candidate.path for candidate in candidates], 1
            ):
                crate = out / self.mod.RESOURCE_ROOT / f"jdk22-{ordinal:04d}"
                with (crate / "manifest.tsv").open(
                    "r", encoding="utf-8", newline=""
                ) as handle:
                    rows = list(csv.reader(handle, delimiter="\t"))
                self.assertEqual(1, len(rows))
                self.assertEqual(expected_path, rows[0][0])

            yaml = (
                out
                / self.mod.YAML_ROOT
                / "m3-jdk22-candidate-backports.yml"
            ).read_text(encoding="utf-8")
            self.assertIn("com.m3.generated.jdk22_0001", yaml)
            self.assertIn("com.m3.generated.jdk22_0002", yaml)

    def test_include_text_emits_separate_java_and_plain_text_crates(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = root / "repo"
            out = root / "out"
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")

            java_path = "src/jdk.jlink/share/classes/p/A.java"
            text_path = "make/Images.gmk"
            properties_path = "src/jdk.jlink/share/classes/p/messages.properties"

            self.write(repo, java_path, "package p; final class A { int x() { return 21; } }\n")
            self.write(repo, text_path, "BASE=21\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk21")
            self.git(repo, "tag", "jdk-21+35")

            self.write(repo, java_path, "package p; final class A { int x() { return 24; } }\n")
            self.write(repo, text_path, "BASE=24\n")
            self.write(repo, properties_path, "key=value\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk24")
            self.git(repo, "tag", "jdk-24+36")

            candidates, exclusions = self.mod.candidates(
                repo,
                24,
                selected=None,
                all_candidates=True,
                include_text=True,
            )
            self.assertEqual([], exclusions)
            self.assertEqual(
                [text_path, java_path, properties_path],
                [candidate.path for candidate in candidates],
            )
            self.assertEqual(
                ["TEXT", "JAVA", "TEXT"],
                [candidate.kind for candidate in candidates],
            )

            crates = self.mod.materialize(
                out,
                24,
                candidates,
                exclusions,
                crate_size=1,
            )
            self.assertEqual(
                ["jdk24-java-0001", "jdk24-text-0001", "jdk24-text-0002"],
                crates,
            )

            java_crate = out / self.mod.RESOURCE_ROOT / "jdk24-java-0001"
            text_root = out / self.mod.TEXT_RESOURCE_ROOT
            self.assertTrue((java_crate / "0001.java.txt").is_file())
            self.assertTrue((text_root / "jdk24-text-0001" / "0001.txt").is_file())
            self.assertTrue((text_root / "jdk24-text-0002" / "0001.txt").is_file())

            yaml = (
                out
                / self.mod.YAML_ROOT
                / "m3-jdk24-candidate-backports.yml"
            ).read_text(encoding="utf-8")
            self.assertIn(
                "com.m3.rewrite.backport.M3Jdk21HashPinnedSnapshotRecipe",
                yaml,
            )
            self.assertIn(
                "com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe",
                yaml,
            )
            self.assertIn("crateName: jdk24-java-0001", yaml)
            self.assertIn("crateName: jdk24-text-0001", yaml)

    def test_text_candidates_remain_opt_in_for_backward_compatibility(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = root / "repo"
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")

            path = "make/Images.gmk"
            self.write(repo, path, "BASE=21\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk21")
            self.git(repo, "tag", "jdk-21+35")
            self.write(repo, path, "BASE=22\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk22")
            self.git(repo, "tag", "jdk-22+36")

            candidates, exclusions = self.mod.candidates(
                repo,
                22,
                selected=None,
                all_candidates=True,
            )
            self.assertEqual([], candidates)
            self.assertEqual([], exclusions)

            candidates, exclusions = self.mod.candidates(
                repo,
                22,
                selected=None,
                all_candidates=True,
                include_text=True,
            )
            self.assertEqual([path], [candidate.path for candidate in candidates])
            self.assertEqual("TEXT", candidates[0].kind)
            self.assertEqual([], exclusions)

    def test_text_mode_and_encoding_fail_into_typed_exclusions(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = root / "repo"
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")

            executable = "make/tool.sh"
            binary = "make/blob.dat"
            self.write(repo, executable, "#!/bin/sh\necho 21\n")
            self.write(repo, binary, "text\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk21")
            self.git(repo, "tag", "jdk-21+35")

            self.write(repo, executable, "#!/bin/sh\necho 22\n")
            (repo / executable).chmod(0o755)
            (repo / binary).write_bytes(b"\xff\xfe\xfd")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk22")
            self.git(repo, "tag", "jdk-22+36")

            candidates, exclusions = self.mod.candidates(
                repo,
                22,
                selected=None,
                all_candidates=True,
                include_text=True,
            )
            self.assertEqual([], candidates)
            reasons = dict(exclusions)
            self.assertTrue(reasons[executable].startswith("TYPED_EXCLUSION_FILE_MODE:"))
            self.assertTrue(reasons[binary].startswith("TYPED_EXCLUSION_ENCODING:"))

    def test_crate_size_rejects_zero_and_above_budget(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            out = Path(temp) / "out"
            candidate = self.mod.Candidate(
                path="src/java.base/share/classes/p/A.java",
                status="ADDED",
                before_sha256="ABSENT",
                after_sha256=self.mod._sha256_text("final class A {}\n"),
                after_text="final class A {}\n",
            )
            with self.assertRaises(ValueError):
                self.mod.materialize(out, 22, [candidate], [], crate_size=0)
            with self.assertRaises(ValueError):
                self.mod.materialize(
                    out,
                    22,
                    [candidate],
                    [],
                    crate_size=self.mod.CRATE_LIMIT + 1,
                )



if __name__ == "__main__":
    unittest.main()
