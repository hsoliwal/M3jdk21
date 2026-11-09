#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: GPL-2.0-only
"""Source-custody mutation tests, including a full-checkout integration gate."""
from __future__ import annotations

import contextlib
import hashlib
import io
import json
from pathlib import Path
import sys
import types
import unittest

HERE = Path(__file__).resolve().parent
HISTORICAL = "check_object_adapter_progress_receiver_20261017"
if not (HERE / (HISTORICAL + ".py")).exists():
    sys.modules[HISTORICAL] = types.ModuleType(HISTORICAL)
sys.path.insert(0, str(HERE))
import check_object_source_lineage_20261104 as check  # noqa: E402


def stage(name: str, content: str) -> tuple:
    data = content.encode("utf-8")
    return (name, check.sha256(data), check.git_blob(data), len(content), "recipe", "manifest")


def atom(current: str, old: str, later: tuple, earlier: tuple) -> dict:
    start = 0
    while start < min(len(current), len(old)) and current[start] == old[start]:
        start += 1
    end = 0
    while (end < len(current) - start and end < len(old) - start
           and current[len(current) - 1 - end] == old[len(old) - 1 - end]):
        end += 1
    return {
        "from_stage": later[0], "to_stage": earlier[0], "prefix_chars": start,
        "suffix_chars": end, "current_length_chars": len(current),
        "previous_length_chars": len(old),
        "current_fragment": current[start:len(current) - end],
        "previous_fragment": old[start:len(old) - end],
    }


class ObjectSourceLineageTest(unittest.TestCase):
    def setUp(self):
        self.old = "public Object old() { return this; }\n"
        self.new = "public Object newName() { return this; }\n"
        self.earlier = stage("old", self.old)
        self.later = stage("new", self.new)
        self.valid = atom(self.new, self.old, self.later, self.earlier)

    def test_sha256_and_git_blob_known_vectors(self):
        self.assertEqual("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                         check.sha256(b"abc"))
        self.assertEqual("f2ba8f84ab5c1bce84a7b441cb1959cfc7093b7f", check.git_blob(b"abc"))

    def test_exact_reverse_and_forward_replay(self):
        self.assertEqual(self.old, check.rewind(self.new, self.valid, self.later, self.earlier))

    def test_wrong_current_fragment_refused(self):
        wrong = dict(self.valid, current_fragment="unqualified")
        with self.assertRaisesRegex(RuntimeError, "current fragment drift"):
            check.rewind(self.new, wrong, self.later, self.earlier)

    def test_wrong_bounded_region_refused(self):
        for bad in (-1, len(self.new) + 1, True):
            with self.subTest(bad=bad), self.assertRaisesRegex(RuntimeError, "bounds drift"):
                check.rewind(self.new, dict(self.valid, prefix_chars=bad),
                             self.later, self.earlier)

    def test_wrong_stage_or_length_refused(self):
        for override in ({"from_stage": "unknown"}, {"previous_length_chars": 1000}):
            with self.subTest(override=override), self.assertRaisesRegex(RuntimeError, "identity drift"):
                check.rewind(self.new, dict(self.valid, **override), self.later, self.earlier)

    def test_reconstructed_sha256_mismatch_refused(self):
        tampered = stage("old", self.old.replace("old", "odd", 1))
        with self.assertRaisesRegex(RuntimeError, "reconstructed length drift|source .*drift"):
            check.rewind(self.new, self.valid, self.later, tampered)

    def test_mutated_ledger_blob_refused(self):
        raw = b'{"format": "untrusted"}\n'
        with self.assertRaisesRegex(RuntimeError, "ledger Git blob drift"):
            check.parse_ledger(raw)

    def test_duplicate_json_key_refused_after_exact_blob_pin(self):
        raw = b'{"a":1,"a":2}\n'
        with self.assertRaisesRegex(RuntimeError, "invalid Object lineage JSON"):
            check.parse_ledger(raw, check.git_blob(raw))

    def test_exact_stage_authorities_cannot_be_substituted(self):
        entries = [dict(label=s[0], sha256=s[1], git_blob=s[2], recipe_id=s[4],
                        recipe_manifest_git_blob=s[5]) for s in check.STAGES]
        base = {"format": "M3_OBJECT_TOADAPTER_EXACT_REVERSE_SOURCE_V1",
                "current_source_path": check.TARGET, "authority": "source-only",
                "stages": entries, "reverse_steps": [self.valid, self.valid]}
        raw = (json.dumps(base, indent=2) + "\n").encode()
        self.assertEqual(base, check.parse_ledger(raw, check.git_blob(raw)))
        entries[0]["sha256"] = "0" * 64
        bad = (json.dumps(base, indent=2) + "\n").encode()
        with self.assertRaisesRegex(RuntimeError, "stage authority drift"):
            check.parse_ledger(bad, check.git_blob(bad))

    def test_real_full_checkout_reconstruction(self):
        root = HERE.parents[1]
        if not (root / check.TARGET).is_file() or not (root / check.LEDGER).is_file():
            self.skipTest("complete M3JDK21 tree and exact source image absent")
        with contextlib.redirect_stdout(io.StringIO()) as out:
            self.assertEqual(3, check.verify(root))
        self.assertIn("SOURCE_LINEAGE_3_OF_3", out.getvalue())
        self.assertIn("HOLD historical", out.getvalue())


if __name__ == "__main__":
    unittest.main()
