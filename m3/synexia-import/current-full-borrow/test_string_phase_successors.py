#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Refusal and live-target proof for approved Phase-1 M3 String successor images."""
from __future__ import annotations

import csv
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).resolve().with_name("check_string_phase.py")
spec = importlib.util.spec_from_file_location("m3_phase_successor", SCRIPT)
if spec is None or spec.loader is None:
    raise RuntimeError("current phase checker cannot be imported")
phase = importlib.util.module_from_spec(spec)
spec.loader.exec_module(phase)


class StringTargetSuccessorTest(unittest.TestCase):
    def read_rows(self):
        with phase.SUCCESSORS.open(encoding="utf-8", newline="") as handle:
            return list(csv.DictReader(handle, delimiter="\t"))

    def mapping(self):
        # Independent immutable donor map, not the mutable successor fixture.
        with phase.MAP.open(encoding="utf-8", newline="") as handle:
            return list(csv.DictReader(handle, delimiter="\t"))

    def with_rows(self, changed, callback):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "string-target-successors.tsv"
            with path.open("w", encoding="utf-8", newline="") as handle:
                writer = csv.DictWriter(handle, fieldnames=phase.SUCCESSOR_HEADER,
                                        delimiter="\t", lineterminator="\n")
                writer.writeheader()
                writer.writerows(changed)
            with patch.object(phase, "SUCCESSORS", path):
                callback()

    def test_exact_four_successors_and_repeated_historical_relation(self):
        mapping = self.mapping()
        mapping.append(mapping[0].copy())
        result = phase.approved_successors(mapping)
        self.assertEqual(phase.EXPECTED_SUCCESSOR_PATHS, set(result))
        self.assertEqual(4, len(result))

    def test_actual_owner_sources_match_exact_receipts(self):
        receipts = phase.approved_successors(self.mapping())
        for target, row in receipts.items():
            path = phase.ROOT / target
            self.assertTrue(path.is_file(), target)
            self.assertEqual(phase.current_successor_blobs(self.mapping())[target],
                             phase.git_blob(path), target)
            phase.validate_successor_source(target, path.read_text(encoding="utf-8"))

    def read_chain(self):
        with phase.SUCCESSOR_CHAIN.open(encoding="utf-8", newline="") as handle:
            return list(csv.DictReader(handle, delimiter="\t"))

    def with_chain(self, changed, callback):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "candidate-string-successor-chain.tsv"
            with path.open("w", encoding="utf-8", newline="") as handle:
                writer = csv.DictWriter(handle, fieldnames=phase.SUCCESSOR_CHAIN_HEADER,
                                        delimiter="\t", lineterminator="\n")
                writer.writeheader()
                writer.writerows(changed)
            with patch.object(phase, "SUCCESSOR_CHAIN", path):
                callback()

    def test_first_source_chain_preserves_history_and_exact_identity(self):
        historical = phase.approved_successors(self.mapping())
        self.assertEqual("70d1d03c109126fdda36c54da21b484bbe368fd9",
                         historical[phase.M3_STRING_PATH]["successor_git_blob"])
        expected = phase.current_successor_blobs(self.mapping())
        self.assertEqual("4fb87fd70ffe5c8d75d17db9c28b44bc9b574704",
                         expected[phase.M3_STRING_PATH])
        self.assertEqual(4, len(expected))

    def test_chain_refuses_drift_ambiguity_reordering_and_promotion(self):
        good = self.read_chain()
        cases = []
        cases.append(good[:0])
        cases.append(good + [{
            "schema": "M3JDK21_STRING_TARGET_EVOLUTION_V1",
            "target_path": phase.M3_STRING_PATH,
            "predecessor_git_blob": "4fb87fd70ffe5c8d75d17db9c28b44bc9b574704",
            "successor_git_blob": "42399d411c1516a56a47dcdb10242960157b4677",
            "synexia_pr": "11247",
            "m3jdk_pr": "864",
            "state": "SOURCE_CANDIDATE_NOT_RUNTIME_ACCEPTED",
        }])  # Future-stage receipt cannot be admitted to this first-stage target.
        cases.append(good + [good[-1].copy()])
        for field, replacement in [
            ("target_path", "src/java.base/share/classes/java/lang/FakeM3String.java"),
            ("predecessor_git_blob", "0" * 40),
            ("successor_git_blob", "f" * 40),
            ("synexia_pr", "0"),
            ("m3jdk_pr", "0"),
            ("state", "RUNTIME_ACCEPTED"),
        ]:
            mutated = [row.copy() for row in good]
            mutated[0][field] = replacement
            cases.append(mutated)
        for mutated in cases:
            self.with_chain(mutated, lambda: self.assertRaises(
                ValueError, phase.current_successor_blobs, self.mapping()))

    def test_duplicate_successor_refuses(self):
        rows = self.read_rows()
        self.with_rows(rows + [rows[0].copy()], lambda: self.assertRaisesRegex(
            ValueError, "duplicate successor owner",
            phase.approved_successors, self.mapping()))

    def test_missing_successor_refuses(self):
        rows = self.read_rows()
        self.with_rows(rows[:-1], lambda: self.assertRaisesRegex(
            ValueError, "successor inventory drift",
            phase.approved_successors, self.mapping()))

    def test_historical_pin_mutation_refuses(self):
        rows = self.read_rows()
        rows[0]["historical_git_blob"] = "0" * 40
        self.with_rows(rows, lambda: self.assertRaisesRegex(
            ValueError, "historical target pin altered",
            phase.approved_successors, self.mapping()))

    def test_unqualified_successor_refuses(self):
        rows = self.read_rows()
        extra = rows[0].copy()
        extra["target_path"] = "src/java.base/share/classes/java/lang/InventedString.java"
        mapping = self.mapping() + [{
            "target_path": extra["target_path"],
            "target_git_blob": extra["historical_git_blob"],
        }]
        self.with_rows(rows + [extra], lambda: self.assertRaisesRegex(
            ValueError, "unqualified successor owner",
            phase.approved_successors, mapping))

    def test_invalid_successor_digest_refuses(self):
        rows = self.read_rows()
        rows[0]["successor_git_blob"] = "not-sha"
        self.with_rows(rows, lambda: self.assertRaisesRegex(
            ValueError, "unsealed successor Git blob",
            phase.approved_successors, self.mapping()))

    def test_successor_without_change_refuses(self):
        rows = self.read_rows()
        rows[0]["successor_git_blob"] = rows[0]["historical_git_blob"]
        self.with_rows(rows, lambda: self.assertRaisesRegex(
            ValueError, "successor did not advance",
            phase.approved_successors, self.mapping()))

    def test_invalid_pr_provenance_refuses(self):
        rows = self.read_rows()
        rows[0]["synexia_prs"] = "maybe"
        self.with_rows(rows, lambda: self.assertRaisesRegex(
            ValueError, "unqualified successor PR lineage",
            phase.approved_successors, self.mapping()))

    def test_successor_promotion_refuses(self):
        rows = self.read_rows()
        rows[0]["state"] = "RUNTIME_ACCEPTED"
        self.with_rows(rows, lambda: self.assertRaisesRegex(
            ValueError, "successor promotion policy drift",
            phase.approved_successors, self.mapping()))

    def test_legacy_contradiction_refuses(self):
        rows = self.mapping()
        rows.append({"target_path": rows[0]["target_path"], "target_git_blob": "f" * 40})
        with self.assertRaisesRegex(ValueError, "contradictory historical target pin"):
            phase.approved_successors(rows)

    def test_m3_string_behavior_loss_refuses(self):
        path = "src/java.base/share/classes/java/lang/M3String.java"
        contents = (phase.ROOT / path).read_text(encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "responsibility lost"):
            phase.validate_successor_source(
                path, contents.replace("static M3String joinDesignated(", "static M3String lost("))

    def test_flat_needle_candidate_facts_loss_refuses(self):
        path = "src/java.base/share/classes/java/lang/M3StringFacts.java"
        contents = (phase.ROOT / path).read_text(encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "responsibility lost"):
            phase.validate_successor_source(
                path, contents.replace("boolean mayContain(String needle)", "boolean lost(String needle)"))

    def test_position_duplicate_character_storage_refuses(self):
        path = "src/java.base/share/classes/java/lang/M3StringPositionPrecompute.java"
        contents = (phase.ROOT / path).read_text(encoding="utf-8")
        marker = "private static final class ExactBlock {"
        self.assertIn(marker, contents)
        corrupted = contents.replace(marker, marker + "\n        final char[] units;\n")
        with self.assertRaisesRegex(ValueError, "retained char\\[\\] spelling"):
            phase.validate_successor_source(path, corrupted)

    def test_pool_concat_length_guard_loss_refuses(self):
        path = "src/java.base/share/classes/java/lang/M3StringPool.java"
        contents = (phase.ROOT / path).read_text(encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "responsibility lost"):
            phase.validate_successor_source(
                path,
                contents.replace(
                    'throw new OutOfMemoryError("Required length exceeds implementation limit");',
                    'throw new ArithmeticException("overflow");',
                ),
            )

    def test_pool_concat_length_guard_reordering_refuses(self):
        path = "src/java.base/share/classes/java/lang/M3StringPool.java"
        contents = (phase.ROOT / path).read_text(encoding="utf-8")
        guard = 'if ((long) left.length() + right.length() > Integer.MAX_VALUE) {'
        owner = 'if (left.owner() == right.owner() && left.end() == right.start()) {'
        self.assertLess(contents.index(guard), contents.index(owner))
        corrupted = contents.replace(guard, "/* moved guard */", 1)
        corrupted = corrupted.replace(owner, owner + "\n        " + guard, 1)
        with self.assertRaisesRegex(ValueError, "guard order drift"):
            phase.validate_successor_source(path, corrupted)


if __name__ == "__main__":
    unittest.main()
