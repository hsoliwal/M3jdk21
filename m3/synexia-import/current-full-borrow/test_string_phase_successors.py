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

    def read_merged_rows(self):
        with phase.MERGED_SOURCE_OBSERVATION.open(encoding="utf-8", newline="") as handle:
            return list(csv.DictReader(handle, delimiter="\t"))

    def with_merged_rows(self, changed, callback):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "string-target-merged-source-observation.tsv"
            with path.open("w", encoding="utf-8", newline="") as handle:
                writer = csv.DictWriter(
                    handle, fieldnames=phase.MERGED_SOURCE_OBSERVATION_HEADER,
                    delimiter="\t", lineterminator="\n")
                writer.writeheader()
                writer.writerows(changed)
            with patch.object(phase, "MERGED_SOURCE_OBSERVATION", path):
                callback()

    def test_serial_chain_preserves_history_and_exact_final_identity(self):
        historical = phase.approved_successors(self.mapping())
        self.assertEqual("70d1d03c109126fdda36c54da21b484bbe368fd9",
                         historical[phase.M3_STRING_PATH]["successor_git_blob"])
        expected = phase.current_successor_blobs(self.mapping())
        self.assertEqual("35ed998540e044eed8cb7596e2e2c019ae369bd3",
                         phase.EXPECTED_STRING_CHAIN[-1][1])
        self.assertEqual("8a57ca4f085e4e8f1f1d9f2ec0fdb2fc5888597e",
                         expected[phase.M3_STRING_PATH])
        observed = self.read_merged_rows()
        self.assertEqual(["V1_TIP", "FIRST_PARENT", "SECOND_PARENT", "MERGED_RESULT"],
                         [row["role"] for row in observed])
        self.assertEqual([
            "35ed998540e044eed8cb7596e2e2c019ae369bd3",
            "1c9574a31bc50013d6d0827f740b4d6fa32d636e",
            "1aea17959495c7fee359d7c13ea4b876f4fb8b31",
            "8a57ca4f085e4e8f1f1d9f2ec0fdb2fc5888597e"],
            [row["git_blob"] for row in observed])
        self.assertEqual("OBSERVED_MERGED_SOURCE_NOT_RECIPE_RECONSTRUCTED",
                         observed[-1]["state"])
        self.assertTrue(all(row["runtime_acceptance"] == "NOT_CLAIMED"
                            for row in observed))
        self.assertEqual(4, len(expected))

    def test_chain_refuses_drift_ambiguity_reordering_and_promotion(self):
        good = self.read_chain()
        cases = []
        cases.append(good[:1])
        cases.append(list(reversed(good)))
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

        good = self.read_merged_rows()
        merged_cases = [good[:-1], list(reversed(good)), good + [good[-1].copy()]]
        for row_index, field, replacement in [
            (1, "git_blob", "0" * 40),
            (2, "canonical_resource_git_blob", "f" * 40),
            (2, "canonical_resource_sha256", "f" * 64),
            (3, "first_parent_commit", good[-1]["second_parent_commit"]),
            (3, "second_parent_commit", good[-1]["first_parent_commit"]),
            (3, "git_blob", "f" * 40),
            (3, "git_blob", "4676ab8cf780d92b0cdac2145e12f09c265492a8"),
            (3, "runtime_acceptance", "ACCEPTED"),
            (3, "state", "RUNTIME_ACCEPTED"),
        ]:
            mutated = [row.copy() for row in good]
            mutated[row_index][field] = replacement
            merged_cases.append(mutated)
        for mutated in merged_cases:
            self.with_merged_rows(mutated, lambda: self.assertRaisesRegex(
                ValueError,
                "merged source observation inventory drift|unreviewed merged source observation",
                phase.current_successor_blobs, self.mapping()))

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


    def read_precompute_rows(self):
        with phase.PRECOMPUTE_OBSERVATION.open(encoding="utf-8", newline="") as handle:
            return list(csv.DictReader(handle, delimiter="\t"))

    def with_precompute_rows(self, changed, callback):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "m3-string-precompute-observation-v2.tsv"
            with path.open("w", encoding="utf-8", newline="") as handle:
                writer = csv.DictWriter(
                    handle, fieldnames=phase.PRECOMPUTE_OBSERVATION_HEADER,
                    delimiter="\t", lineterminator="\n")
                writer.writeheader()
                writer.writerows(changed)
            with patch.object(phase, "PRECOMPUTE_OBSERVATION", path):
                callback()

    def test_precompute_projection_keeps_mindex_and_m3_names_separate(self):
        row = phase.load_precompute_observation()
        self.assertEqual("com.synexia.indexstring.MIndexStringSearchPlan",
                         row["source_symbol"])
        self.assertEqual("java.lang.M3StringSearchPrecompute",
                         row["target_symbol"])
        self.assertEqual("NOT_CLAIMED", row["runtime_acceptance"])
        self.assertEqual("NOT_MEASURED", row["performance_evidence"])
        phase.validate_name_mapping()
        mapping = phase.load_tsv(phase.MAP, phase.HEADER)
        phase.validate_precompute_mapping(mapping, row)

    def test_precompute_observation_refuses_identity_and_status_drift(self):
        rows = self.read_precompute_rows()
        cases = []
        for field, replacement in [
            ("source_symbol", "java.lang.M3StringSearchPrecompute"),
            ("source_git_blob", "0" * 40),
            ("target_symbol", "java.lang.MIndexStringSearchPlan"),
            ("observation_target_git_blob", "f" * 40),
            ("current_target_git_blob", "f" * 40),
            ("synexia_source_recipe_state_at_capture", "MERGED"),
            ("implementation_origin", "COPIED_DONOR_CODE"),
            ("runtime_acceptance", "ACCEPTED"),
            ("performance_evidence", "MEASURED"),
            ("state", "RUNTIME_ACCEPTED"),
        ]:
            changed = [row.copy() for row in rows]
            changed[0][field] = replacement
            cases.append(changed)
        cases.extend([rows[:-1], rows + [rows[0].copy()]])
        for changed in cases:
            self.with_precompute_rows(changed, lambda: self.assertRaises(
                ValueError, phase.load_precompute_observation))

    def test_live_precompute_target_blob_drift_refuses(self):
        row = phase.load_precompute_observation()
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            target = root / row["target_path"]
            target.parent.mkdir(parents=True)
            target.write_text("mutated target\n", encoding="utf-8")
            with patch.object(phase, "ROOT", root):
                with self.assertRaisesRegex(ValueError, "live M3JDK precompute target blob drift"):
                    phase.load_precompute_observation()

    def test_source_precompute_blob_pins_and_v1_chain_remain_exact(self):
        row = phase.load_precompute_observation()
        for key in ("source_path", "cache_path"):
            path = phase.ROOT / row[key]
            if path.is_file():
                self.assertEqual(row["source_git_blob"] if key == "source_path"
                                 else row["cache_git_blob"],
                                 phase.git_blob(path), key)
        self.assertEqual("75d949e5aa2c8ff4e5c70d648e81fc0d37b68651",
                         phase.git_blob(phase.SUCCESSOR_CHAIN))


    def test_live_precompute_target_matches_current_receipt_when_present(self):
        row = phase.load_precompute_observation()
        target = phase.ROOT / row["target_path"]
        if not target.is_file():
            self.skipTest("M3JDK receiver source tree is not present")
        self.assertEqual(row["current_target_git_blob"], phase.git_blob(target))


if __name__ == "__main__":
    unittest.main()
