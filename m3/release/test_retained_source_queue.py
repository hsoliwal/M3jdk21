# SPDX-License-Identifier: Apache-2.0
"""Real Git-tree tests for read-only retained-source path census."""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("retained_source_queue", HERE / "retained_source_queue.py")
QUEUE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(QUEUE)


def git(root: Path, *args: str) -> None:
    subprocess.run(["git", "-C", str(root), *args], check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)


def put(root: Path, path: str, data: str) -> None:
    name = root / path
    name.parent.mkdir(parents=True, exist_ok=True)
    name.write_text(data, encoding="utf-8")


class RetainedSourceQueueTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        git(self.root, "init", "-q")
        git(self.root, "config", "user.email", "fixture@example.invalid")
        git(self.root, "config", "user.name", "Fixture")
        self.prefix = QUEUE.ARCHIVE_ROOT
        self.archive("2084", "src/java.base/share/classes/java/lang/Exact.java", "exact\n")
        self.archive("2084", "src/java.base/share/classes/java/lang/Missing.java", "missing\n")
        self.archive("1888", "m3/tools/Evolved.java", "original\n")
        self.archive("1888", "m3/tools/Mode.java", "mode\n")
        self.archive("1888", "m3/tools/README.md", "not code\n")
        put(self.root, "src/java.base/share/classes/java/lang/Exact.java", "exact\n")
        put(self.root, "m3/tools/Evolved.java", "refined\n")
        put(self.root, "m3/tools/Mode.java", "mode\n")
        git(self.root, "add", ".")
        git(self.root, "update-index", "--chmod=+x", "m3/tools/Mode.java")
        git(self.root, "commit", "-qm", "fixture")

    def archive(self, pr: str, path: str, content: str) -> None:
        put(self.root, self.prefix + "pr" + pr + "/" + path, content)

    def tearDown(self):
        self.temp.cleanup()

    def test_queue_scans_exact_head_without_claiming_code_loss(self):
        tsv, raw, summary = QUEUE.scan(self.root)
        self.assertEqual(summary["source_rows"], 4)
        self.assertEqual(summary["non_source_rows_skipped"], 1)
        self.assertEqual(summary["unresolved_rows"], 3)
        self.assertEqual(summary["states"], {
            "EXACT_ACTIVE_PATH": 1,
            "REVIEW_ABSENT_ACTIVE_PATH": 1,
            "REVIEW_DIVERGENT_ACTIVE_PATH": 1,
            "REVIEW_MODE_DIFFERENCE": 1,
        })
        self.assertFalse(json.loads(raw)["merge_authorized"])
        self.assertIn("pr2084/src/java.base/", tsv)
        self.assertEqual(QUEUE.scan(self.root)[:2], (tsv, raw))
        self.assertIn("\t-\t-\tREVIEW_ABSENT_ACTIVE_PATH\t", tsv)

    def test_uncommitted_candidate_does_not_fake_head_readback(self):
        before = QUEUE.scan(self.root)
        put(self.root, "src/java.base/share/classes/java/lang/Missing.java", "missing\n")
        self.assertEqual(before, QUEUE.scan(self.root))

    def test_strict_writes_queue_even_when_unresolved(self):
        path = self.root / "output/queue.tsv"
        summary = self.root / "output/summary.json"
        self.assertEqual(QUEUE.main(["--root", str(self.root),
                                     "--tsv-out", str(path), "--summary-out", str(summary),
                                     "--strict"]), 1)
        self.assertTrue(path.read_text(encoding="utf-8").startswith("priority\tpr\t"))
        self.assertEqual(json.loads(summary.read_text())["unresolved_rows"], 3)
        self.assertEqual(QUEUE.main(["--root", str(self.root),
                                     "--tsv-out", str(path), "--summary-out", str(summary)]), 0)

    def test_archived_path_malformed_is_refused_not_misclassified(self):
        self.archive("0", "m3/tools/Wrong.java", "not a valid PR\n")
        git(self.root, "add", ".")
        git(self.root, "commit", "-qm", "malformed")
        with self.assertRaisesRegex(ValueError, "ARCHIVE_PATH_MISSING_PR"):
            QUEUE.scan(self.root)

    def test_no_archive_or_non_root_checkout_refuses(self):
        self.assertRaisesRegex(ValueError, "ROOT_MUST_BE_GIT_TOPLEVEL",
                               QUEUE.scan, self.root / "m3")
        git(self.root, "rm", "-qr", self.prefix)
        git(self.root, "commit", "-qm", "remove archive")
        with self.assertRaisesRegex(ValueError, "RETAINED_VARIANTS_TREE_MISSING"):
            QUEUE.scan(self.root)


if __name__ == "__main__":
    unittest.main()
