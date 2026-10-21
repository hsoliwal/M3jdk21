# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Target receiver fixtures; tests only the queue gate, not JDK acceptance."""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("m3_name_receiver", HERE / "check.py")
GATE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(GATE)


def write(root: Path, rel: str, text: str) -> None:
    file = root / rel
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_text(text, encoding="utf-8")


class NameContractReceiverTests(unittest.TestCase):
    def setUp(self):
        self.workdir = tempfile.TemporaryDirectory()
        self.root = Path(self.workdir.name)
        write(self.root, GATE.DOC, "Naming contract - fixture only\n")
        write(self.root, "AGENTS.md", "Read " + GATE.DOC + " before implementation.\n")
        self.mapping = {"schema": 1, "migration": {"records": [{
            "id": "class.view", "status": "implemented-unverified", "targets": [{
                "symbol": "jdk.internal.mindex.M3Class",
                "path": "src/java.base/share/classes/jdk/internal/mindex/M3Class.java",
            }]
        }]}}
        write(self.root, "src/java.base/share/classes/jdk/internal/mindex/M3Class.java",
              "package jdk.internal.mindex; public final class M3Class {}\n")
        write(self.root, "src/java.base/share/classes/java/lang/Class.java",
              "package java.lang; public final class Class {}\n")
        self.plan = {"schema": GATE.SCHEMA, "authority": GATE.MAP,
                     "authority_blob": None, "operations": [{
                         "mapping_id": "class.view", "kind": "JDK_BACKEND",
                         "state": "QUEUED", "before_type": "jdk.internal.mindex.M3Class",
                         "after_type": "java.lang.Class",
                         "reason": "Actual java.lang.Class differential and runtime acceptance is pending.",
                     }]}
        self.save()

    def tearDown(self):
        self.workdir.cleanup()

    def save(self):
        write(self.root, GATE.MAP, json.dumps(self.mapping))
        self.plan["authority_blob"] = GATE.git_blob((self.root / GATE.MAP).read_bytes())
        write(self.root, GATE.PLAN, json.dumps(self.plan))

    def test_existing_canonical_target_queue(self):
        count, ids = GATE.validate(self.root)
        self.assertEqual(count, 1)
        self.assertEqual(ids, ["class.view"])

    def test_map_drift_refused(self):
        write(self.root, GATE.MAP, "{}")
        with self.assertRaisesRegex(ValueError, "map drift"):
            GATE.validate(self.root)

    def test_unknown_mapping_refused(self):
        self.plan["operations"][0]["mapping_id"] = "invented.type"
        self.save()
        with self.assertRaisesRegex(ValueError, "unknown capability"):
            GATE.validate(self.root)

    def test_missing_class_refused(self):
        (self.root / self.mapping["migration"]["records"][0]["targets"][0]["path"]).unlink()
        with self.assertRaisesRegex(ValueError, "mapped source file missing"):
            GATE.validate(self.root)

    def test_package_mismatch_refused(self):
        write(self.root, "src/java.base/share/classes/jdk/internal/mindex/M3Class.java",
              "package another.package; public class M3Class {}")
        with self.assertRaisesRegex(ValueError, "declared package"):
            GATE.validate(self.root)

    def test_ready_promotion_unproven_refused(self):
        self.plan["operations"][0]["state"] = "READY_FOR_STAGING"
        self.save()
        with self.assertRaisesRegex(ValueError, "unproven active"):
            GATE.validate(self.root)

    def test_public_m3_api_refused(self):
        self.plan["operations"][0]["after_type"] = "java.lang.M3Class"
        self.save()
        with self.assertRaisesRegex(ValueError, "public JDK backend"):
            GATE.validate(self.root)

    def test_duplicate_refused(self):
        self.plan["operations"].append(dict(self.plan["operations"][0]))
        self.save()
        with self.assertRaisesRegex(ValueError, "duplicate naming"):
            GATE.validate(self.root)

    def test_missing_reason_refused(self):
        self.plan["operations"][0]["reason"] = ""
        self.save()
        with self.assertRaisesRegex(ValueError, "concrete open-gate"):
            GATE.validate(self.root)

    def test_format_only_keeps_existing_internal_type(self):
        row = self.plan["operations"][0]
        row["kind"] = "FORMAT_ONLY"
        row["after_type"] = row["before_type"]
        self.save()
        self.assertEqual(GATE.validate(self.root)[0], 1)

    def test_internal_move_remains_candidate(self):
        row = self.plan["operations"][0]
        row["kind"] = "INTERNAL_MOVE"
        row["after_type"] = "jdk.internal.mindex.M3ClassView"
        self.save()
        self.assertEqual(GATE.validate(self.root)[0], 1)


if __name__ == "__main__":
    unittest.main()
