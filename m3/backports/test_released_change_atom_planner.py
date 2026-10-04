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
    path = Path(__file__).with_name("released_change_atom_planner.py")
    spec = importlib.util.spec_from_file_location(
        "m3_released_change_atom_planner", path
    )
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class ReleasedChangeAtomPlannerTest(unittest.TestCase):
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

    def fixture(self, root: Path) -> Path:
        repo = root / "repo"
        subprocess.run(("git", "init", "-q", str(repo)), check=True)
        self.git(repo, "config", "user.email", "fixture@example.invalid")
        self.git(repo, "config", "user.name", "Fixture")

        self.write(
            repo,
            "src/jdk.jlink/share/classes/p/A.java",
            "package p; class A { int v=21; }\n",
        )
        self.write(
            repo,
            "src/jdk.jlink/share/classes/p/B.java",
            "package p; class B {}\n",
        )
        self.write(repo, "make/Images.gmk", "BASE=21\n")
        self.write(repo, "make/tool.sh", "#!/bin/sh\necho 21\n")
        self.git(repo, "add", ".")
        self.git(repo, "commit", "-q", "-m", "jdk21")
        self.git(repo, "tag", "jdk-21+35")

        self.write(
            repo,
            "src/jdk.jlink/share/classes/p/A.java",
            "package p; class A { int v=22; }\n",
        )
        self.write(repo, "make/Images.gmk", "BASE=22\n")
        self.write(repo, "make/tool.sh", "#!/bin/sh\necho 22\n")
        (repo / "make/tool.sh").chmod(0o755)

        # Transient file exists in an intermediate commit but not in either endpoint.
        self.write(repo, "make/transient.tmp", "temporary\n")
        self.git(repo, "add", ".")
        self.git(repo, "commit", "-q", "-m", "intermediate")
        (repo / "make/transient.tmp").unlink()
        self.git(repo, "add", "-u")
        self.git(repo, "commit", "-q", "-m", "remove transient")
        self.git(repo, "tag", "jdk-22+36")
        return repo

    @staticmethod
    def queue(path: Path) -> None:
        header = (
            "order\trelease\tcommit\tjbs_ids\tsubject\tdomain\t"
            "inventory_disposition\trisk\tscope_floor\tproof_lane\t"
            "recipe_strategy\tpriority\tcompatibility_state\tnext_action\tpaths\n"
        )
        rows = [
            (
                "0\t22\t" + "a" * 40
                + "\tJDK-0000001\tTool update\ttools\treview-tool\tLOW\tMODULE\t"
                "TOOLING\tMIXED_PACKET_OPENREWRITE_PLUS_VERBATIM\t10\t"
                "PENDING_COMPATIBILITY_PROOF\tPROVE_TOOLING_BACKPORT\t"
                "src/jdk.jlink/share/classes/p/A.java,make/Images.gmk"
            ),
            (
                "1\t22\t" + "b" * 40
                + "\tJDK-0000002\tSame Java leaf\ttools\treview-tool\tLOW\tFILE\t"
                "TOOLING\tOPENREWRITE_OR_HASH_PINNED_JAVA\t10\t"
                "PENDING_COMPATIBILITY_PROOF\tPROVE_TOOLING_BACKPORT\t"
                "src/jdk.jlink/share/classes/p/A.java"
            ),
            (
                "2\t22\t" + "c" * 40
                + "\tJDK-0000003\tNo final delta\ttools\treview-tool\tLOW_MEDIUM\tFILE\t"
                "TOOLING\tOPENREWRITE_OR_HASH_PINNED_JAVA\t20\t"
                "PENDING_COMPATIBILITY_PROOF\tPROVE_TOOLING_BACKPORT\t"
                "src/jdk.jlink/share/classes/p/B.java"
            ),
            (
                "3\t22\t" + "d" * 40
                + "\tJDK-0000004\tTransient path\tbuild\treview\tMEDIUM\tFILE\t"
                "GENERAL\tHASH_PINNED_VERBATIM_PATCH\t40\t"
                "PENDING_COMPATIBILITY_PROOF\tPROVE_GENERAL_BACKPORT\t"
                "make/transient.tmp"
            ),
            (
                "4\t22\t" + "e" * 40
                + "\tJDK-0000005\tMode change\tbuild\treview\tHIGH\tFILE\t"
                "GENERAL\tHASH_PINNED_VERBATIM_PATCH\t50\t"
                "PENDING_COMPATIBILITY_PROOF\tPROVE_GENERAL_BACKPORT\t"
                "make/tool.sh"
            ),
            (
                "5\t22\t" + "f" * 40
                + "\tJDK-0000006\tNo paths\tmixed-or-other\treview\tCRITICAL\tFILE\t"
                "GENERAL\tHASH_PINNED_VERBATIM_PATCH\t90\t"
                "PENDING_COMPATIBILITY_PROOF\tPROVE_GENERAL_BACKPORT\t"
            ),
        ]
        path.write_text(header + "\n".join(rows) + "\n", encoding="utf-8")

    def test_content_addressed_atoms_are_reused_across_commit_edges(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = self.fixture(root)
            queue_path = root / "queue.tsv"
            out = root / "out"
            self.queue(queue_path)

            queue = self.mod.read_queue(queue_path)
            rows = self.mod.plan(
                repo,
                queue,
                {"LOW", "LOW_MEDIUM"},
                out,
            )
            by_order = {int(row[0]): row for row in rows}

            self.assertEqual("FILE_ATOMS_IDENTIFIED", by_order[0][10])
            self.assertEqual("2", by_order[0][7])
            self.assertEqual("FILE_ATOMS_IDENTIFIED", by_order[1][10])
            self.assertEqual("1", by_order[1][7])
            self.assertEqual("NO_FINAL_GA_DELTA", by_order[2][10])
            self.assertEqual("SKIPPED_RISK_FILTER", by_order[3][10])
            self.assertEqual("SKIPPED_RISK_FILTER", by_order[4][10])
            self.assertEqual("SKIPPED_RISK_FILTER", by_order[5][10])

            with (out / "FILE_ATOMS.tsv").open(
                encoding="utf-8", newline=""
            ) as handle:
                atoms = list(csv.DictReader(handle, delimiter="\t"))
            self.assertEqual(2, len(atoms))
            self.assertTrue(all(row["atom_id"].startswith("") and len(row["atom_id"]) == 64 for row in atoms))
            self.assertTrue(all(row["authority"] == self.mod.AUTHORITY for row in atoms))

            with (out / "COMMIT_ATOM_EDGES.tsv").open(
                encoding="utf-8", newline=""
            ) as handle:
                edges = list(csv.DictReader(handle, delimiter="\t"))
            self.assertEqual(3, len(edges))
            java_edges = [
                row for row in edges
                if row["path"] == "src/jdk.jlink/share/classes/p/A.java"
            ]
            self.assertEqual(2, len(java_edges))
            self.assertEqual(
                java_edges[0]["atom_id"],
                java_edges[1]["atom_id"],
                "identical final-GA file transformation should reuse one atom",
            )

            summary = (out / "SUMMARY.tsv").read_text(encoding="utf-8")
            self.assertIn("unique_file_atoms\t2", summary)
            self.assertIn("commit_atom_edges\t3", summary)
            self.assertIn("authority\t" + self.mod.AUTHORITY, summary)

    def test_all_risks_classify_transient_mode_and_empty_path_rows(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = self.fixture(root)
            queue_path = root / "queue.tsv"
            out = root / "out"
            self.queue(queue_path)

            rows = self.mod.plan(
                repo,
                self.mod.read_queue(queue_path),
                self.mod.VALID_RISKS,
                out,
            )
            by_order = {int(row[0]): row for row in rows}

            self.assertEqual("ONLY_TYPED_EXCLUSIONS", by_order[3][10])
            self.assertEqual("1", by_order[3][8])
            self.assertEqual("ONLY_TYPED_EXCLUSIONS", by_order[4][10])
            self.assertEqual("1", by_order[4][8])
            self.assertEqual("NO_TOUCHED_PATHS", by_order[5][10])

            with (out / "COMMIT_EXCLUSIONS.tsv").open(
                encoding="utf-8", newline=""
            ) as handle:
                exclusions = list(csv.DictReader(handle, delimiter="\t"))
            reasons = {row["path"]: row["reason"] for row in exclusions}
            self.assertEqual(
                "TYPED_EXCLUSION_TRANSIENT_PATH_NOT_IN_JDK21_OR_FINAL_GA",
                reasons["make/transient.tmp"],
            )
            self.assertTrue(
                reasons["make/tool.sh"].startswith("TYPED_EXCLUSION_FILE_MODE:")
            )
            self.assertTrue(all(row["authority"] == self.mod.AUTHORITY for row in exclusions))

    def test_max_items_bounds_eligible_prefix_without_reordering_queue(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = self.fixture(root)
            queue_path = root / "queue.tsv"
            out = root / "out"
            self.queue(queue_path)

            rows = self.mod.plan(
                repo,
                self.mod.read_queue(queue_path),
                {"LOW", "LOW_MEDIUM"},
                out,
                max_items=1,
            )
            by_order = {int(row[0]): row for row in rows}
            self.assertEqual("FILE_ATOMS_IDENTIFIED", by_order[0][10])
            self.assertEqual("SKIPPED_BATCH_LIMIT", by_order[1][10])
            self.assertEqual("SKIPPED_BATCH_LIMIT", by_order[2][10])

    def test_materialize_one_atom_uses_existing_hash_pinned_generator(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = self.fixture(root)
            out = root / "crate"
            crates = self.mod.materialize_atom(
                repo,
                22,
                "src/jdk.jlink/share/classes/p/A.java",
                out,
            )
            self.assertEqual(["jdk22-0001"], crates)
            crate = out / self.mod.GEN.RESOURCE_ROOT / "jdk22-0001"
            self.assertTrue((crate / "manifest.tsv").is_file())
            self.assertTrue((crate / "0001.java.txt").is_file())

    def test_invalid_queue_risk_state_and_empty_risk_filter_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            bad = root / "bad.tsv"
            bad.write_text(
                "order\trelease\tcommit\tjbs_ids\tsubject\tdomain\t"
                "inventory_disposition\trisk\tscope_floor\tproof_lane\t"
                "recipe_strategy\tpriority\tcompatibility_state\tnext_action\tpaths\n"
                "0\t22\t" + "a" * 40
                + "\t\tx\ttools\treview\tUNKNOWN\tFILE\tTOOLING\tX\t1\t"
                "PENDING_COMPATIBILITY_PROOF\tX\tmake/X.gmk\n",
                encoding="utf-8",
            )
            with self.assertRaises(ValueError):
                self.mod.read_queue(bad)

            queue_path = root / "queue.tsv"
            self.queue(queue_path)
            queue = self.mod.read_queue(queue_path)
            with self.assertRaises(ValueError):
                self.mod.plan(root, queue, set(), root / "out")
            with self.assertRaises(ValueError):
                self.mod._selected_rows(queue, frozenset({"LOW"}), 0)


if __name__ == "__main__":
    unittest.main()
