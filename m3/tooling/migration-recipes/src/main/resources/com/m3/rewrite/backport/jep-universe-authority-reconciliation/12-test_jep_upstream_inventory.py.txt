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
    path = Path(__file__).with_name("jep_upstream_inventory.py")
    spec = importlib.util.spec_from_file_location("m3_jep_upstream_inventory", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class JepUpstreamInventoryTest(unittest.TestCase):
    def setUp(self) -> None:
        self.mod = load_module()

    @staticmethod
    def git(repo: Path, *args: str) -> str:
        proc = subprocess.run(
            ("git", "-C", str(repo), *args),
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        return proc.stdout.strip()

    @staticmethod
    def write(repo: Path, path: str, text: str) -> None:
        target = repo / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding="utf-8")

    @staticmethod
    def catalogue(path: Path) -> None:
        path.write_text(
            "release\tjep\ttitle\tdomain\tdisposition\treason\tsuperseded_by\n"
            "24\t493\tLinking Run-Time Images without JMODs\ttool-jlink-runtime-image\tcandidate\ttool\t\n"
            "24\t485\tStream Gatherers\tlibrary\tcandidate\tapi\t\n"
            "24\t999\tOld Preview\tlibrary\tsuperseded\tuse final\t493\n"
            "24\t998\tNew Grammar\tlanguage\treject-language\tgrammar\t\n"
            "24\t997\tCompiler Hold\tlibrary-hotspot-compiler\thold-jit\thotspot\t\n"
            "24\t996\tNo Named Commit\ttool\tcandidate\tmanual\t\n",
            encoding="utf-8",
        )

    def fixture(self, root: Path) -> Path:
        repo = root / "upstream"
        subprocess.run(("git", "init", "-q", str(repo)), check=True)
        self.git(repo, "config", "user.email", "fixture@example.invalid")
        self.git(repo, "config", "user.name", "Fixture")

        self.write(repo, "make/Images.gmk", "BASE=21\n")
        self.write(repo, "src/jdk.jlink/share/classes/p/A.java", "package p; class A {}\n")
        self.git(repo, "add", ".")
        self.git(repo, "commit", "-q", "-m", "JDK 21 baseline")
        self.git(repo, "tag", "jdk-21+35")

        self.write(repo, "make/Images.gmk", "BASE=24\n")
        self.write(
            repo,
            "src/jdk.jlink/share/classes/p/B.java",
            "package p; class B {}\n",
        )
        self.git(repo, "add", ".")
        self.git(repo, "commit", "-q", "-m", "8311302: Implement JEP 493")
        self.git(repo, "mv", "src/jdk.jlink/share/classes/p/A.java", "src/jdk.jlink/share/classes/p/Renamed.java")
        self.git(repo, "commit", "-q", "-m", "JEP-493 follow-up rename")

        self.write(
            repo,
            "src/java.base/share/classes/java/util/stream/Gatherer.java",
            "package java.util.stream; public interface Gatherer {}\n",
        )
        self.git(repo, "add", ".")
        self.git(repo, "commit", "-q", "-m", "Implement JEP 485: Stream Gatherers")

        self.write(repo, "README", "unrelated\n")
        self.git(repo, "add", ".")
        self.git(repo, "commit", "-q", "-m", "Unrelated maintenance")
        self.git(repo, "tag", "jdk-24+36")
        return repo

    def test_inventory_finds_jep_named_seed_commits_and_exact_paths(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = self.fixture(root)
            catalogue_path = root / "JEP_CATALOGUE.tsv"
            self.catalogue(catalogue_path)

            rows = self.mod.read_catalogue(catalogue_path)
            commits, paths = self.mod.inventory(repo, rows, {24: "jdk-24+36"})

            seeds_493 = [seed for seed in commits if seed.jep == 493]
            self.assertEqual(2, len(seeds_493))
            self.assertTrue(all(seed.donor_ref == "jdk-24+36" for seed in seeds_493))
            self.assertTrue(all(seed.disposition == "candidate" for seed in seeds_493))

            path_rows = [row for row in paths if row.jep == 493]
            path_values = {row.path for row in path_rows}
            self.assertIn("make/Images.gmk", path_values)
            self.assertIn("src/jdk.jlink/share/classes/p/B.java", path_values)
            self.assertIn("src/jdk.jlink/share/classes/p/A.java", path_values)
            self.assertIn("src/jdk.jlink/share/classes/p/Renamed.java", path_values)
            self.assertTrue(any(row.status.startswith("R") and row.status.endswith("_OLD") for row in path_rows))
            self.assertTrue(any(row.status.startswith("R") and row.status.endswith("_NEW") for row in path_rows))

            seeds_485 = [seed for seed in commits if seed.jep == 485]
            self.assertEqual(1, len(seeds_485))
            self.assertIn("JEP 485", seeds_485[0].subject)

            self.assertFalse(any(seed.jep == 996 for seed in commits))
            self.assertFalse(any(row.path == "README" for row in paths))

    def test_writer_emits_candidate_path_lists_and_non_authoritative_queue(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = self.fixture(root)
            catalogue_path = root / "JEP_CATALOGUE.tsv"
            out = root / "out"
            self.catalogue(catalogue_path)

            rows = self.mod.read_catalogue(catalogue_path)
            commits, paths = self.mod.inventory(repo, rows, {24: "jdk-24+36"})
            self.mod.write_inventory(out, rows, commits, paths, {24: "jdk-24+36"})

            with (out / "JEP_UPSTREAM_COMMITS.tsv").open(
                encoding="utf-8", newline=""
            ) as handle:
                commit_rows = list(csv.DictReader(handle, delimiter="\t"))
            self.assertTrue(commit_rows)
            self.assertTrue(
                all(
                    row["authority"]
                    == "DISCOVERY_SEED_ONLY_NOT_DEPENDENCY_CLOSURE"
                    for row in commit_rows
                )
            )

            with (out / "JEP_PACKET_QUEUE.tsv").open(
                encoding="utf-8", newline=""
            ) as handle:
                queue = {int(row["jep"]): row for row in csv.DictReader(handle, delimiter="\t")}

            self.assertEqual("jdk-24+36", queue[493]["donor_ref"])
            self.assertEqual(
                "REVIEW_SEED_COMMITS_AND_CLOSE_DEPENDENCIES",
                queue[493]["next_action"],
            )
            self.assertEqual(
                "SKIP_SUPERSEDED_USE_FINAL_LINEAGE",
                queue[999]["next_action"],
            )
            self.assertEqual(
                "NO_PRODUCT_PACKET_LANGUAGE_LOCK",
                queue[998]["next_action"],
            )
            self.assertEqual(
                "HOLD_DEPENDENCY_OR_COMPATIBILITY_REVIEW",
                queue[997]["next_action"],
            )
            self.assertEqual(
                "MANUAL_UPSTREAM_COMMIT_DISCOVERY",
                queue[996]["next_action"],
            )
            self.assertTrue(
                all(
                    row["authority"]
                    == "INVENTORY_ONLY_NO_COMPATIBILITY_OR_MUTATION_AUTHORITY"
                    for row in queue.values()
                )
            )

            paths_493 = (
                out / "paths" / "jep-493.txt"
            ).read_text(encoding="utf-8").splitlines()
            self.assertEqual(sorted(set(paths_493)), paths_493)
            self.assertIn("make/Images.gmk", paths_493)
            self.assertFalse((out / "paths" / "jep-996.txt").exists())

            summary = (out / "SUMMARY.tsv").read_text(encoding="utf-8")
            self.assertIn("catalogue_rows\t6", summary)
            self.assertIn("semantic_authority\tNONE", summary)

    def test_duplicate_catalogue_and_unsupported_release_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            duplicate = root / "duplicate.tsv"
            duplicate.write_text(
                "release\tjep\ttitle\tdomain\tdisposition\treason\tsuperseded_by\n"
                "24\t493\tA\ttool\tcandidate\tx\t\n"
                "24\t493\tB\ttool\tcandidate\tx\t\n",
                encoding="utf-8",
            )
            with self.assertRaises(ValueError):
                self.mod.read_catalogue(duplicate)

            unsupported = root / "unsupported.tsv"
            unsupported.write_text(
                "release\tjep\ttitle\tdomain\tdisposition\treason\tsuperseded_by\n"
                "28\t600\tFuture\ttool\tcandidate\tx\t\n",
                encoding="utf-8",
            )
            with self.assertRaises(ValueError):
                self.mod.read_catalogue(unsupported)

    def test_next_action_never_turns_seed_presence_into_compatibility(self) -> None:
        row = self.mod.JepRow(
            24, 493, "Jlink", "tool", "candidate", "reason", ""
        )
        seed = self.mod.CommitSeed(
            24,
            493,
            "candidate",
            "jdk-24+36",
            "a" * 40,
            "Implement JEP 493",
        )
        self.assertEqual(
            "REVIEW_SEED_COMMITS_AND_CLOSE_DEPENDENCIES",
            self.mod.next_action(row, [seed]),
        )


if __name__ == "__main__":
    unittest.main()
