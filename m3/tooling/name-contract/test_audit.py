# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Defensive tests for the canonical-mapping-only naming census."""
from __future__ import annotations

from hashlib import sha1
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("name_census", HERE / "audit.py")
AUDIT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(AUDIT)


def write(root: Path, path: str, value: str) -> None:
    dst = root / path
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(value, encoding="utf-8")


class TargetCensusTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.candidates = [
            dict(path="src/java.base/share/classes/jdk/internal/mindex/M3CI.java",
                 module="java.base", symbol="jdk.internal.mindex.M3CI"),
            dict(path="src/java.base/share/native/libjava/M3Index.c",
                 module="java.base", symbol="Java_jdk_internal_mindex_M3Index_copy0"),
        ]
        self.records = [dict(id="cap.one", status="implemented-unverified", targets=list(self.candidates)),
                        dict(id="cap.two", status="pending", targets=[self.candidates[0]])]
        write(self.root, self.candidates[0]["path"],
              "package jdk.internal.mindex; public final class M3CI {}\n")
        write(self.root, self.candidates[1]["path"], "// native symbol\n")
        self.save()

    def tearDown(self):
        self.tmp.cleanup()

    def save(self):
        text = json.dumps({"migration": {"records": self.records}})
        write(self.root, AUDIT.MAP, text)
        raw = (self.root / AUDIT.MAP).read_bytes()
        digest = sha1(b"blob " + str(len(raw)).encode("ascii") + b"\0" + raw).hexdigest()
        write(self.root, AUDIT.PLAN, json.dumps({"authority": AUDIT.MAP, "authority_blob": digest}))

    def test_present_and_shared_paths_are_explicit(self):
        rows = AUDIT.snapshot(self.root)
        self.assertEqual(len(rows), 3)
        self.assertEqual([r["result"] for r in rows], ["PRESENT_NAME_MATCH", "NATIVE_PRESENT_UNVERIFIED", "PRESENT_NAME_MATCH"])
        self.assertEqual(rows[0]["path_refs"], "2")
        self.assertEqual(rows[2]["path_refs"], "2")
        self.assertEqual(AUDIT.as_tsv(rows), AUDIT.as_tsv(AUDIT.snapshot(self.root)))

    def test_pending_java_missing_is_not_a_pass(self):
        (self.root / self.candidates[0]["path"]).unlink()
        rows = AUDIT.snapshot(self.root)
        self.assertEqual(rows[0]["result"], "REVIEW_MISSING")
        self.assertEqual(rows[2]["result"], "PENDING_MISSING")

    def test_mismatched_package_is_flagged(self):
        write(self.root, self.candidates[0]["path"], "package wrong; class M3CI {}")
        self.assertEqual(AUDIT.snapshot(self.root)[0]["result"], "REVIEW_DECLARATION")

    def test_bad_java_path_is_flagged(self):
        self.records[0]["targets"][0] = dict(self.candidates[0], symbol="jdk.internal.mindex.M3Other")
        self.save()
        self.assertEqual(AUDIT.snapshot(self.root)[0]["result"], "REVIEW_PATH_SYMBOL")

    def test_absent_implemented_native_target_needs_review(self):
        (self.root / self.candidates[1]["path"]).unlink()
        rows = AUDIT.snapshot(self.root)
        self.assertEqual(rows[1]["result"], "REVIEW_NATIVE_MISSING")

    def test_absent_pending_native_target_is_not_accepted(self):
        self.records[0]["status"] = "pending"
        self.save()
        (self.root / self.candidates[1]["path"]).unlink()
        rows = AUDIT.snapshot(self.root)
        self.assertEqual(rows[1]["result"], "PENDING_NATIVE_MISSING")
        self.assertIn("target_path", AUDIT.as_tsv(rows).splitlines()[0])

    def test_map_drift_refused(self):
        write(self.root, AUDIT.MAP, "{}")
        with self.assertRaisesRegex(ValueError, "drift"):
            AUDIT.snapshot(self.root)

    def test_duplicate_mapping_id_refused(self):
        self.records.append(dict(self.records[0]))
        self.save()
        with self.assertRaisesRegex(ValueError, "duplicate"):
            AUDIT.snapshot(self.root)

    def test_escape_path_refused(self):
        self.records[0]["targets"][0]["path"] = "../../secret.java"
        self.save()
        with self.assertRaisesRegex(ValueError, "unsafe"):
            AUDIT.snapshot(self.root)


if __name__ == "__main__":
    unittest.main()
