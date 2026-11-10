# Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
import unittest

from retention import inspect, write


class RetentionAuditTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.home = Path(self.temp.name)
        self.repo = self.home / "repo"
        self.repo.mkdir()
        self.git("init", "-b", "main")
        self.git("config", "user.name", "M3 Fixture")
        self.git("config", "user.email", "fixture@example.invalid")

        (self.repo / "src").mkdir()
        (self.repo / "recipes").mkdir()
        (self.repo / "m3/history").mkdir(parents=True)
        self.a = b"class A {}\n"
        self.b = b"class B {}\n"
        (self.repo / "src/A.java").write_bytes(self.a)
        (self.repo / "src/B.java").write_bytes(self.b)
        manifest = (
            "src/A.java\tABSENT\t" + hashlib.sha256(self.a).hexdigest() + "\tA.txt\n"
            "src/B.java\tABSENT\t" + hashlib.sha256(self.b).hexdigest() + "\tB.txt\n"
        )
        (self.repo / "recipes/cap.manifest.tsv").write_text(manifest, encoding="utf-8")
        (self.repo / "m3/history/RETAINED_CAPABILITIES.tsv").write_text(
            "capability_id\tmanifest_path\n"
            "cap\trecipes/cap.manifest.tsv\n",
            encoding="utf-8",
        )
        self.git("add", ".")
        self.git("commit", "-m", "retained baseline")
        self.baseline = self.git("rev-parse", "HEAD")

    def git(self, *args) -> str:
        return subprocess.check_output(
            ["git", "-C", str(self.repo), *args],
            stderr=subprocess.DEVNULL,
        ).decode().strip()

    def test_exact_retention_is_deterministic(self):
        first = inspect(
            self.repo,
            self.baseline,
            "m3/history/RETAINED_CAPABILITIES.tsv",
        )
        second = inspect(
            self.repo,
            self.baseline,
            "m3/history/RETAINED_CAPABILITIES.tsv",
        )
        self.assertEqual(first, second)
        self.assertEqual(1, first["capabilities"])
        self.assertEqual(2, first["targets"])
        self.assertEqual(2, first["retained_exact"])
        self.assertEqual(0, first["present_drifted_review"])
        self.assertEqual(0, first["missing"])

    def test_drift_is_review_and_missing_is_explicit(self):
        (self.repo / "src/A.java").write_text("class A { int x; }\n", encoding="utf-8")
        (self.repo / "src/B.java").unlink()
        self.git("add", "-A")
        self.git("commit", "-m", "drift and loss")
        head = self.git("rev-parse", "HEAD")

        result = inspect(
            self.repo,
            head,
            "m3/history/RETAINED_CAPABILITIES.tsv",
        )
        self.assertEqual(0, result["retained_exact"])
        self.assertEqual(1, result["present_drifted_review"])
        self.assertEqual(1, result["missing"])
        states = {row["path"]: row["state"] for row in result["rows"]}
        self.assertEqual("PRESENT_DRIFTED_REVIEW", states["src/A.java"])
        self.assertEqual("MISSING", states["src/B.java"])

    def test_manifest_is_read_from_exact_commit_not_worktree(self):
        (self.repo / "recipes/cap.manifest.tsv").write_text(
            "broken worktree\n", encoding="utf-8"
        )
        result = inspect(
            self.repo,
            self.baseline,
            "m3/history/RETAINED_CAPABILITIES.tsv",
        )
        self.assertEqual(2, result["retained_exact"])

    def test_invalid_manifest_and_catalogue_fail_closed(self):
        (self.repo / "recipes/cap.manifest.tsv").write_text(
            "src/B.java\tABSENT\t" + hashlib.sha256(self.b).hexdigest() + "\tB.txt\n"
            "src/A.java\tABSENT\t" + hashlib.sha256(self.a).hexdigest() + "\tA.txt\n",
            encoding="utf-8",
        )
        self.git("add", ".")
        self.git("commit", "-m", "unsorted manifest")
        head = self.git("rev-parse", "HEAD")
        with self.assertRaisesRegex(ValueError, "sorted"):
            inspect(self.repo, head, "m3/history/RETAINED_CAPABILITIES.tsv")

        (self.repo / "m3/history/RETAINED_CAPABILITIES.tsv").write_text(
            "capability_id\tmanifest_path\n"
            "cap\t../escape.tsv\n",
            encoding="utf-8",
        )
        self.git("add", ".")
        self.git("commit", "-m", "unsafe catalogue")
        unsafe = self.git("rev-parse", "HEAD")
        with self.assertRaisesRegex(ValueError, "noncanonical"):
            inspect(self.repo, unsafe, "m3/history/RETAINED_CAPABILITIES.tsv")

    def test_write_is_deterministic_and_refuses_existing_output(self):
        result = inspect(
            self.repo,
            self.baseline,
            "m3/history/RETAINED_CAPABILITIES.tsv",
        )
        output = self.home / "out"
        write(result, output)
        self.assertTrue((output / "RETENTION.tsv").is_file())
        summary = json.loads((output / "SUMMARY.json").read_text(encoding="utf-8"))
        self.assertEqual(result["root"], summary["root"])
        self.assertEqual(
            result["root"],
            (output / "STATE_ROOT.sha256").read_text(encoding="ascii").strip(),
        )
        with self.assertRaisesRegex(ValueError, "already exist"):
            write(result, output)


if __name__ == "__main__":
    unittest.main()
