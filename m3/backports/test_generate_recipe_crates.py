#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# Modified 2026-10-17: retain legacy generator tests and add exact target reuse regressions.
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import csv
import importlib.util
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
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

    def test_native_jni_candidates_have_first_class_source_sealed_lane(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = root / "repo"
            out = root / "out"
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")

            cpp_path = "src/hotspot/share/runtime/a3.cpp"
            header_path = "src/java.base/share/native/libjava/a3.h"
            self.write(repo, cpp_path, "int a3() { return 21; }\n")
            self.write(repo, header_path, "#define A3 21\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk21")
            self.git(repo, "tag", "jdk-21+35")

            self.write(repo, cpp_path, "int a3() { return 24; }\n")
            self.write(repo, header_path, "#define A3 24\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk24")
            self.git(repo, "tag", "jdk-24+36")

            candidates, exclusions = self.mod.candidates(
                repo,
                24,
                selected=None,
                all_candidates=True,
                include_native=True,
            )
            self.assertEqual([], exclusions)
            self.assertEqual([cpp_path, header_path], [row.path for row in candidates])
            self.assertEqual(["NATIVE", "NATIVE"], [row.kind for row in candidates])

            crates = self.mod.materialize(
                out,
                24,
                candidates,
                exclusions,
                crate_size=1,
            )
            self.assertEqual(
                ["jdk24-native-0001", "jdk24-native-0002"],
                crates,
            )
            native_root = out / self.mod.TEXT_RESOURCE_ROOT
            for ordinal, expected_path in enumerate([cpp_path, header_path], 1):
                crate = native_root / f"jdk24-native-{ordinal:04d}"
                with (crate / "manifest.tsv").open(
                    "r", encoding="utf-8", newline=""
                ) as handle:
                    rows = list(csv.reader(handle, delimiter="\t"))
                self.assertEqual(1, len(rows))
                self.assertEqual(expected_path, rows[0][0])
                self.assertTrue((crate / "0001.txt").is_file())

            yaml = (
                out
                / self.mod.YAML_ROOT
                / "m3-jdk24-candidate-backports.yml"
            ).read_text(encoding="utf-8")
            self.assertIn(
                "com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe",
                yaml,
            )
            self.assertIn("crateName: jdk24-native-0001", yaml)
            self.assertIn("crateName: jdk24-native-0002", yaml)

            legacy, legacy_exclusions = self.mod.candidates(
                repo,
                24,
                selected=None,
                all_candidates=True,
                include_text=True,
            )
            self.assertEqual([], legacy_exclusions)
            self.assertEqual(["TEXT", "TEXT"], [row.kind for row in legacy])

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

    def test_exact_donor_ref_controls_candidate_bytes_and_crate_provenance(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = root / "repo"
            out = root / "out"
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")

            path = "src/java.base/share/classes/p/A.java"
            self.write(repo, path, "package p; final class A { int x() { return 21; } }\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk21")
            self.git(repo, "tag", "jdk-21+35")

            feature_text = "package p; final class A { int x() { return 22; } }\n"
            self.write(repo, path, feature_text)
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "feature")
            donor = subprocess.check_output(
                ("git", "-C", str(repo), "rev-parse", "HEAD"), text=True
            ).strip()

            self.write(repo, path, "package p; final class A { int x() { return 2200; } }\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "later-ga")
            self.git(repo, "tag", "jdk-22+36")

            candidates, exclusions = self.mod.candidates(
                repo,
                22,
                selected={path},
                all_candidates=False,
                donor_ref=donor,
            )
            self.assertEqual([], exclusions)
            self.assertEqual([path], [row.path for row in candidates])
            self.assertEqual(feature_text, candidates[0].after_text)

            crates = self.mod.materialize(
                out,
                22,
                candidates,
                exclusions,
                crate_size=1,
                donor_ref=donor,
            )
            self.assertEqual(["jdk22-0001"], crates)
            with (out / "CRATES.tsv").open(
                encoding="utf-8", newline=""
            ) as handle:
                rows = list(csv.DictReader(handle, delimiter="\t"))
            self.assertEqual(1, len(rows))
            self.assertEqual("jdk-21+35", rows[0]["baseline_ref"])
            self.assertEqual(donor, rows[0]["donor_ref"])

            crate = out / self.mod.RESOURCE_ROOT / "jdk22-0001"
            with (crate / "manifest.tsv").open(
                encoding="utf-8", newline=""
            ) as handle:
                manifest = list(csv.reader(handle, delimiter="\t"))
            self.assertEqual(self.mod._sha256_text(feature_text), manifest[0][2])
            self.assertEqual(feature_text, (crate / manifest[0][3]).read_text(encoding="utf-8"))


    def test_path_donor_override_uses_last_approved_feature_postimage(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = root / "repo"
            out = root / "out"
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")

            path = "test/hotspot/jtreg/gc/g1/TestFeature.java"
            base_text = "final class TestFeature { int value() { return 21; } }\n"
            feature_text = "final class TestFeature { int value() { return 423; } }\n"
            self.write(repo, path, base_text)
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "jdk21")
            self.git(repo, "tag", "jdk-21+35")

            self.write(repo, path, feature_text)
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "approved-feature")
            feature_ref = subprocess.check_output(
                ("git", "-C", str(repo), "rev-parse", "HEAD"), text=True
            ).strip()

            (repo / path).unlink()
            self.write(repo, "unrelated.txt", "later branch state\n")
            self.git(repo, "add", "-A")
            self.git(repo, "commit", "-q", "-m", "unrelated-later-removal")
            tip_ref = subprocess.check_output(
                ("git", "-C", str(repo), "rev-parse", "HEAD"), text=True
            ).strip()
            self.git(repo, "tag", "jdk-22+36")

            candidates, exclusions = self.mod.candidates(
                repo,
                22,
                selected={path},
                all_candidates=False,
                donor_ref=tip_ref,
            )
            self.assertEqual([], candidates)
            self.assertEqual(
                [(path, "TYPED_EXCLUSION_AUTOMATIC_REMOVAL")],
                exclusions,
            )

            candidates, exclusions = self.mod.candidates(
                repo,
                22,
                selected={path},
                all_candidates=False,
                donor_ref=tip_ref,
                donor_overrides={path: feature_ref},
            )
            self.assertEqual([], exclusions)
            self.assertEqual(1, len(candidates))
            self.assertEqual(path, candidates[0].path)
            self.assertEqual("MODIFIED", candidates[0].status)
            self.assertEqual(feature_ref, candidates[0].donor_ref)
            self.assertEqual(feature_text, candidates[0].after_text)

            crates = self.mod.materialize(
                out,
                22,
                candidates,
                exclusions,
                crate_size=1,
                donor_ref=tip_ref,
            )
            self.assertEqual(["jdk22-0001"], crates)
            with (out / "CRATES.tsv").open(
                encoding="utf-8", newline=""
            ) as handle:
                rows = list(csv.DictReader(handle, delimiter="\t"))
            self.assertEqual(feature_ref, rows[0]["donor_ref"])

    def test_donor_override_map_is_sorted_unique_and_custody_only(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            donor_map = root / "donors.tsv"
            donor_map.write_text(
                "path\tdonor_ref\n"
                "a/A.java\t" + ("a" * 40) + "\n"
                "b/B.java\t" + ("b" * 40) + "\n",
                encoding="utf-8",
            )
            self.assertEqual(
                {"a/A.java": "a" * 40, "b/B.java": "b" * 40},
                self.mod._donor_overrides(donor_map),
            )

            donor_map.write_text(
                "path\tdonor_ref\n"
                "b/B.java\t" + ("b" * 40) + "\n"
                "a/A.java\t" + ("a" * 40) + "\n",
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "noncanonical donor-map row"):
                self.mod._donor_overrides(donor_map)

            donor_map.write_text(
                "path\tdonor_ref\n"
                "a/A.java\tmutable-tag\n",
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "noncanonical donor-map row"):
                self.mod._donor_overrides(donor_map)

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



class TargetReuseRoutingTest(unittest.TestCase):
    """Three immutable trees, with old generator behavior tested independently above."""

    prefix = "src/java.base/share/classes/demo/"
    git = staticmethod(GenerateRecipeCratesTest.git)
    write = staticmethod(GenerateRecipeCratesTest.write)

    def setUp(self) -> None:
        self.mod = load_module()
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.repo, self.target = self.root / "upstream", self.root / "target"
        for repo in (self.repo, self.target):
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "M3 Reuse Fixture")
        self.paths = {
            name: self.prefix + name + ".java"
            for name in ("Existing", "Unchanged", "Drift", "Pending", "Added",
                         "Removed", "Absent", "RemovedAndAbsent", "Mode", "CurrentOnly")
        }
        self.paths["Native"] = "src/hotspot/share/runtime/reuse.cpp"
        self.paths["Resource"] = "make/reuse.txt"
        self.paths["OutsideA3"] = "m3/example/Existing.java"
        self.before = {
            self.paths[name]: f"// baseline {name}\n"
            for name in ("Existing", "Unchanged", "Drift", "Pending", "Removed",
                         "RemovedAndAbsent", "Mode", "Native", "Resource", "OutsideA3")
        }
        self.donor = dict(self.before)
        for name in ("Existing", "Drift", "Pending", "Added", "Native"):
            self.donor[self.paths[name]] = f"// donor {name}\n"
        for name in ("Removed", "RemovedAndAbsent"):
            del self.donor[self.paths[name]]
        self.current = dict(self.before)
        for name in ("Existing", "Native"):
            self.current[self.paths[name]] = self.donor[self.paths[name]]
        self.current[self.paths["Drift"]] = "// retained M3 implementation\n"
        self.current[self.paths["CurrentOnly"]] = "// target-owned source\n"
        del self.current[self.paths["RemovedAndAbsent"]]
        for path, text in self.before.items():
            self.write(self.repo, path, text)
        self.git(self.repo, "add", ".")
        self.git(self.repo, "commit", "-q", "-m", "baseline")
        self.git(self.repo, "tag", "jdk-21+35")
        self.baseline_commit = self.commit(self.repo)
        for path in set(self.before) - set(self.donor):
            (self.repo / path).unlink()
        for path, text in self.donor.items():
            self.write(self.repo, path, text)
        self.git(self.repo, "add", "-A")
        self.git(self.repo, "commit", "-q", "-m", "donor")
        self.git(self.repo, "tag", "jdk-22+36")
        self.donor_commit = self.commit(self.repo)
        for path, text in self.current.items():
            self.write(self.target, path, text)
        self.git(self.target, "add", ".")
        self.git(self.target, "update-index", "--chmod=+x", self.paths["Mode"])
        self.git(self.target, "commit", "-q", "-m", "current M3")
        self.target_commit = self.commit(self.target)

    @staticmethod
    def commit(repo: Path) -> str:
        return subprocess.check_output(
            ("git", "-C", str(repo), "rev-parse", "HEAD"), text=True,
        ).strip()

    def plan(self, **options):
        defaults = dict(
            target_ref="HEAD", target_git=self.target,
            include_text=True, include_native=True,
        )
        defaults.update(options)
        return self.mod.plan_target_reuse(
            self.repo, 22, set(self.paths.values()), False, **defaults,
        )

    def cli(self, output: Path, paths: list[str] | None = None, **options) -> int:
        args = ["--repo", str(self.repo), "--release", "22", "--out", str(output)]
        if paths is None:
            args.append("--all-candidates")
        else:
            selected = self.root / "selected-paths.txt"
            selected.write_text("\n".join(sorted(paths)) + "\n", encoding="utf-8")
            args.extend(["--paths-file", str(selected)])
        if options.get("target", True):
            args.extend(["--target-git", str(self.target), "--target-ref", "HEAD"])
        if options.get("native", False):
            args.append("--include-native")
        if options.get("text", False):
            args.append("--include-text")
        return self.mod.main(args)

    @staticmethod
    def tsv(path: Path):
        with path.open(encoding="utf-8", newline="") as handle:
            return list(csv.DictReader(handle, delimiter="\t"))

    def test_exact_three_way_routes_preserve_existing_and_evolved_owners(self) -> None:
        plan = self.plan()
        rows = {row.path: row for row in plan.rows}
        expected = {
            "Existing": "ALREADY_PRESENT_ATOMIZE_PATTERNIZE",
            "Unchanged": "ALREADY_PRESENT_ATOMIZE_PATTERNIZE",
            "Native": "ALREADY_PRESENT_ATOMIZE_PATTERNIZE",
            "Resource": "ALREADY_PRESENT_ATOMIZE_PATTERNIZE",
            "OutsideA3": "ALREADY_PRESENT_ATOMIZE_PATTERNIZE",
            "Added": "MECHANICAL_FILE_REPLAY",
            "Pending": "MECHANICAL_FILE_REPLAY",
            "Drift": "HOLD_CURRENT_TREE_DRIFT",
            "Mode": "HOLD_CURRENT_TREE_DRIFT",
            "Removed": "HOLD_DONOR_PATH_ABSENT",
            "CurrentOnly": "HOLD_DONOR_PATH_ABSENT",
            "Absent": "NO_DONOR_IMPLEMENTATION",
            "RemovedAndAbsent": "NO_DONOR_IMPLEMENTATION",
        }
        for name, route in expected.items():
            with self.subTest(name=name):
                row = rows[self.paths[name]]
                self.assertEqual(route, row.route)
                self.assertEqual("false", row.source_mutation_authority)
                self.assertEqual("false", row.compatibility_authority)
                self.assertEqual("false", row.feature_acceptance)
                self.assertEqual(self.target_commit, row.current_commit)
                self.assertEqual(self.baseline_commit, row.baseline_commit)
                self.assertEqual(self.donor_commit, row.donor_commit)
        self.assertEqual("NOT_MATERIALIZED", rows[self.paths["Existing"]].current_sha256)
        self.assertEqual("blob", rows[self.paths["Existing"]].current_object_type)
        self.assertEqual("", rows[self.paths["Absent"]].current_object_type)
        self.assertEqual(rows[self.paths["Existing"]].current_git_blob,
                         rows[self.paths["Existing"]].donor_git_blob)
        self.assertNotEqual(rows[self.paths["Mode"]].current_mode,
                            rows[self.paths["Mode"]].donor_mode)
        self.assertEqual("NATIVE", rows[self.paths["Native"]].kind)
        self.assertFalse(self.mod._java_a3_review(rows[self.paths["Native"]]))
        self.assertFalse(self.mod._java_a3_review(rows[self.paths["OutsideA3"]]))
        for path, text in self.current.items():
            self.assertEqual(text, (self.target / path).read_text())

    def test_routing_never_reads_duplicate_payload_bytes(self) -> None:
        with patch.object(self.mod, "_git_bytes", side_effect=AssertionError("payload read")):
            plan = self.plan()
        self.assertGreater(len(plan.rows), 0)
        self.assertTrue(all(row.current_sha256 in {"ABSENT", "NOT_MATERIALIZED"}
                            for row in plan.rows))

    def test_cli_imports_only_exact_baseline_and_writes_java_only_review_input(self) -> None:
        out = self.root / "mixed"
        self.assertEqual(0, self.cli(out, native=True, text=True))
        manifests = list((out / self.mod.RESOURCE_ROOT).glob("*/manifest.tsv"))
        imported = []
        for manifest in manifests:
            with manifest.open(encoding="utf-8", newline="") as handle:
                imported.extend(row[0] for row in csv.reader(handle, delimiter="\t"))
        self.assertEqual(sorted([self.paths["Added"], self.paths["Pending"]]), sorted(imported))
        existing = self.tsv(out / "A3_EXISTING_SOURCES.tsv")
        self.assertEqual(sorted([self.paths["Existing"], self.paths["Unchanged"]]),
                         [row["path"] for row in existing])
        self.assertTrue(all(row["feature_acceptance"] == "false" for row in existing))
        all_rows = {row["path"]: row for row in self.tsv(out / "TARGET_REUSE.tsv")}
        self.assertEqual(64, len(all_rows[self.paths["Pending"]]["current_sha256"]))
        self.assertEqual(64, len(all_rows[self.paths["Added"]]["donor_sha256"]))
        self.assertEqual("ABSENT", all_rows[self.paths["Added"]]["current_sha256"])
        self.assertEqual(self.baseline_commit, self.tsv(out / "CRATES.tsv")[0]["baseline_ref"])
        self.assertEqual("HOLD_DONOR_PATH_ABSENT", all_rows[self.paths["CurrentOnly"]]["route"])

    def test_only_existing_source_succeeds_without_any_replacement_recipe(self) -> None:
        selected = [self.paths[name] for name in ("Existing", "Unchanged", "Native")]
        first, second = self.root / "first", self.root / "second"
        self.assertEqual(0, self.cli(first, selected, native=True))
        self.assertEqual(0, self.cli(second, list(reversed(selected)), native=True))
        self.assertEqual([], self.tsv(first / "CRATES.tsv"))
        self.assertEqual([], list(first.rglob("*.yml")))
        self.assertFalse((first / self.mod.RESOURCE_ROOT).exists())
        self.assertEqual(2, len(self.tsv(first / "A3_EXISTING_SOURCES.tsv")))
        for path in first.rglob("*"):
            if path.is_file():
                self.assertEqual(path.read_bytes(), (second / path.relative_to(first)).read_bytes())

    def test_source_kind_selection_and_legacy_mode_remain_separate(self) -> None:
        plan = self.plan(include_text=False, include_native=False)
        rows = {row.path: row for row in plan.rows}
        self.assertEqual("SOURCE_KIND_NOT_SELECTED", rows[self.paths["Native"]].route)
        self.assertEqual("SOURCE_KIND_NOT_SELECTED", rows[self.paths["Resource"]].route)
        legacy = self.root / "legacy"
        self.assertEqual(0, self.cli(legacy, [self.paths["Existing"]], target=False))
        self.assertTrue((legacy / self.mod.RESOURCE_ROOT / "jdk22-0001" / "manifest.tsv").is_file())
        self.assertFalse((legacy / "TARGET_REUSE.tsv").exists())
        self.assertFalse((legacy / "A3_EXISTING_SOURCES.tsv").exists())

    def test_source_refs_are_frozen_before_any_tree_comparison(self) -> None:
        original_compare = self.mod.DELTA.compare_refs
        observed = []

        def compare(repo, release, base, donor):
            observed.append((base, donor))
            if len(observed) == 1:
                self.write(self.target, self.paths["Existing"], "// later target branch\n")
                self.git(self.target, "add", ".")
                self.git(self.target, "commit", "-q", "-m", "branch advanced")
                self.git(self.repo, "tag", "-f", "jdk-22+36", self.baseline_commit)
            return original_compare(repo, release, base, donor)

        with patch.object(self.mod.DELTA, "compare_refs", side_effect=compare):
            plan = self.plan()
        self.assertEqual(self.target_commit, plan.target_commit)
        self.assertEqual([(self.baseline_commit, self.donor_commit)], observed)
        self.assertEqual("ALREADY_PRESENT_ATOMIZE_PATTERNIZE",
                         next(row.route for row in plan.rows if row.path == self.paths["Existing"]))

    def test_per_file_donor_override_controls_identity_before_routing(self) -> None:
        self.write(self.repo, self.paths["Existing"], "// newer donor implementation\n")
        self.git(self.repo, "add", ".")
        self.git(self.repo, "commit", "-q", "-m", "later feature")
        later = self.commit(self.repo)
        plan = self.plan(donor_ref=later,
                         donor_overrides={self.paths["Existing"]: self.donor_commit})
        row = next(row for row in plan.rows if row.path == self.paths["Existing"])
        self.assertEqual(self.donor_commit, row.donor_commit)
        self.assertEqual("ALREADY_PRESENT_ATOMIZE_PATTERNIZE", row.route)
        self.assertEqual(((self.paths["Existing"], self.donor_commit),), plan.donor_overrides)

    def test_refusal_of_stale_output_missing_ref_and_unsupported_git_modes(self) -> None:
        stale = self.root / "stale"
        stale.mkdir()
        receipt = stale / "kept.txt"
        receipt.write_text("keep", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "new or empty"):
            self.cli(stale)
        self.assertEqual("keep", receipt.read_text())
        with self.assertRaises((ValueError, RuntimeError)):
            self.plan(target_ref="unknown-ref")
        with self.assertRaisesRegex(ValueError, "invalid source ref"):
            self.plan(target_ref="--help")
        link = self.paths["Existing"]
        blob = subprocess.check_output(
            ("git", "-C", str(self.repo), "rev-parse", f"HEAD:{link}"), text=True,
        ).strip()
        self.git(self.repo, "update-index", "--cacheinfo", "120000", blob, link)
        self.git(self.repo, "commit", "-q", "-m", "symlink mode")
        plan = self.plan(donor_ref="HEAD")
        row = next(row for row in plan.rows if row.path == link)
        self.assertEqual("HOLD_UNSUPPORTED_FILE_MODE", row.route)

    def test_target_git_requires_target_ref_and_candidate_tampering_is_refused(self) -> None:
        out = self.root / "invalid"
        with self.assertRaisesRegex(ValueError, "requires --target-ref"):
            self.mod.main(["--repo", str(self.repo), "--release", "22",
                           "--all-candidates", "--target-git", str(self.target),
                           "--out", str(out)])
        self.assertFalse(out.exists())
        plan = self.plan()
        paths = {row.path for row in plan.rows if row.route == "MECHANICAL_FILE_REPLAY"}
        candidates, _ = self.mod.candidates(
            self.repo, 22, paths, False, donor_ref=plan.donor_commit,
            baseline_ref=plan.baseline_commit,
        )
        changed = self.mod.replace(candidates[0], after_text="// unreviewed source\n")
        with self.assertRaisesRegex(ValueError, "custody drift"):
            self.mod._verify_reuse_candidates(plan, [changed])



if __name__ == "__main__":
    unittest.main()
