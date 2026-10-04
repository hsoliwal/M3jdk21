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
    path = Path(__file__).with_name("jep_seed_packet_planner.py")
    spec = importlib.util.spec_from_file_location("m3_jep_seed_packet_planner", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class JepSeedPacketPlannerTest(unittest.TestCase):
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

        self.write(repo, "src/jdk.jlink/share/classes/p/A.java", "package p; class A { int v=21; }\n")
        self.write(repo, "make/Images.gmk", "BASE=21\n")
        self.write(repo, "make/Same.gmk", "SAME\n")
        self.write(repo, "make/tool.sh", "#!/bin/sh\necho 21\n")
        self.git(repo, "add", ".")
        self.git(repo, "commit", "-q", "-m", "jdk21")
        self.git(repo, "tag", "jdk-21+35")

        self.write(repo, "src/jdk.jlink/share/classes/p/A.java", "package p; class A { int v=24; }\n")
        self.write(repo, "make/Images.gmk", "BASE=24\n")
        self.write(repo, "make/tool.sh", "#!/bin/sh\necho 24\n")
        (repo / "make/tool.sh").chmod(0o755)
        self.git(repo, "add", ".")
        self.git(repo, "commit", "-q", "-m", "jdk24")
        self.git(repo, "tag", "jdk-24+36")
        return repo

    @staticmethod
    def queue(path: Path) -> None:
        path.write_text(
            "release\tjep\ttitle\tdomain\tdisposition\tdonor_ref\tseed_commit_count\tseed_path_count\tnext_action\tauthority\n"
            "24\t493\tJlink\ttool\tcandidate\tjdk-24+36\t1\t3\tREVIEW_SEED_COMMITS_AND_CLOSE_DEPENDENCIES\tNONE\n"
            "24\t494\tSame\ttool\tcandidate\tjdk-24+36\t1\t1\tREVIEW_SEED_COMMITS_AND_CLOSE_DEPENDENCIES\tNONE\n"
            "24\t495\tMode\ttool\tcandidate\tjdk-24+36\t1\t1\tREVIEW_SEED_COMMITS_AND_CLOSE_DEPENDENCIES\tNONE\n"
            "24\t496\tNoPaths\ttool\tcandidate\tjdk-24+36\t1\t1\tREVIEW_SEED_COMMITS_AND_CLOSE_DEPENDENCIES\tNONE\n"
            "24\t999\tRejected\tlanguage\treject-language\tjdk-24+36\t0\t0\tNO_PRODUCT_PACKET_LANGUAGE_LOCK\tNONE\n",
            encoding="utf-8",
        )

    def test_plan_generates_mixed_atoms_and_preserves_non_authority(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = self.fixture(root)
            queue = root / "queue.tsv"
            paths = root / "paths"
            out = root / "out"
            paths.mkdir()
            self.queue(queue)

            (paths / "jep-493.txt").write_text(
                "make/Images.gmk\n"
                "make/Same.gmk\n"
                "src/jdk.jlink/share/classes/p/A.java\n",
                encoding="utf-8",
            )
            (paths / "jep-494.txt").write_text("make/Same.gmk\n", encoding="utf-8")
            (paths / "jep-495.txt").write_text("make/tool.sh\n", encoding="utf-8")

            rows = self.mod.plan(
                repo, queue, paths, out, {24: "jdk-24+36"}
            )
            by_jep = {int(row[1]): row for row in rows}

            self.assertEqual("SEED_FILE_ATOMS_GENERATED", by_jep[493][7])
            self.assertEqual("3", by_jep[493][4])
            self.assertEqual("2", by_jep[493][5])
            self.assertEqual("0", by_jep[493][6])

            jep493 = out / "jep-493"
            self.assertTrue(
                (jep493 / self.mod.GEN.RESOURCE_ROOT / "jdk24-java-0001").is_dir()
            )
            self.assertTrue(
                (jep493 / self.mod.GEN.TEXT_RESOURCE_ROOT / "jdk24-text-0001").is_dir()
            )

            self.assertEqual("NO_DONOR_TREE_DELTA", by_jep[494][7])
            self.assertEqual("0", by_jep[494][5])
            self.assertEqual("0", by_jep[494][6])

            self.assertEqual("ONLY_TYPED_EXCLUSIONS", by_jep[495][7])
            self.assertEqual("0", by_jep[495][5])
            self.assertEqual("1", by_jep[495][6])

            self.assertEqual("NO_SEED_PATH_LIST", by_jep[496][7])
            self.assertEqual("SKIPPED_NON_REVIEW_ACTION", by_jep[999][7])

            self.assertTrue(
                all(
                    row[8]
                    == "INVENTORY_ONLY_NO_COMPATIBILITY_OR_MUTATION_AUTHORITY"
                    for row in rows
                )
            )

            with (out / "JEP_SEED_PACKET_STATUS.tsv").open(
                encoding="utf-8", newline=""
            ) as handle:
                written = list(csv.DictReader(handle, delimiter="\t"))
            self.assertEqual(5, len(written))
            self.assertEqual(
                "INVENTORY_ONLY_NO_COMPATIBILITY_OR_MUTATION_AUTHORITY",
                written[0]["authority"],
            )

    def test_queue_donor_ref_drift_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = self.fixture(root)
            queue = root / "queue.tsv"
            paths = root / "paths"
            out = root / "out"
            paths.mkdir()
            self.queue(queue)
            text = queue.read_text(encoding="utf-8").replace(
                "jdk-24+36", "wrong-ref", 1
            )
            queue.write_text(text, encoding="utf-8")
            with self.assertRaises(ValueError):
                self.mod.plan(repo, queue, paths, out, {24: "jdk-24+36"})

    def test_empty_queue_and_empty_path_list_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            empty = root / "empty.tsv"
            empty.write_text(
                "release\tjep\tdisposition\tnext_action\n",
                encoding="utf-8",
            )
            with self.assertRaises(ValueError):
                self.mod._read_queue(empty)

            paths = root / "paths.txt"
            paths.write_text("# comment only\n", encoding="utf-8")
            with self.assertRaises(ValueError):
                self.mod._selected(paths)


if __name__ == "__main__":
    unittest.main()
