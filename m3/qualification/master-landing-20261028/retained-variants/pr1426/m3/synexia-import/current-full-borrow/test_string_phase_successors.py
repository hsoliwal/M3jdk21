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
            self.assertEqual(phase.observed_candidate_blobs(self.mapping())[target],
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

    def test_serial_chain_preserves_history_and_exact_final_identity(self):
        historical = phase.approved_successors(self.mapping())
        self.assertEqual("70d1d03c109126fdda36c54da21b484bbe368fd9",
                         historical[phase.M3_STRING_PATH]["successor_git_blob"])
        expected = phase.current_successor_blobs(self.mapping())
        self.assertEqual("35ed998540e044eed8cb7596e2e2c019ae369bd3",
                         expected[phase.M3_STRING_PATH])
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
        # The third row was already published. The original two-step receipt
        # remains an exact byte prefix; check both generations independently.
        with tempfile.TemporaryDirectory() as temporary:
            prefix = Path(temporary) / "historical-two-step-chain.tsv"
            prefix.write_bytes(b"".join(
                phase.SUCCESSOR_CHAIN.read_bytes().splitlines(keepends=True)[:3]))
            self.assertEqual("4ef13745f3fdc265fc8df3f2d84a9f2ecd3b7516",
                             phase.git_blob(prefix))


    def test_live_precompute_target_matches_current_receipt_when_present(self):
        row = phase.load_precompute_observation()
        target = phase.ROOT / row["target_path"]
        if not target.is_file():
            self.skipTest("M3JDK receiver source tree is not present")
        self.assertEqual(row["current_target_git_blob"], phase.git_blob(target))


    def read_observations(self):
        with phase.CURRENT_SOURCE_OBSERVATIONS.open(encoding="utf-8", newline="") as handle:
            return list(csv.DictReader(handle, delimiter="\t"))

    def with_observations(self, changed, callback):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "observations.tsv"
            with path.open("w", encoding="utf-8", newline="") as handle:
                writer = csv.DictWriter(handle, fieldnames=phase.CURRENT_SOURCE_HEADER,
                                        delimiter="\t", lineterminator="\n")
                writer.writeheader()
                writer.writerows(changed)
            with patch.object(phase, "CURRENT_SOURCE_OBSERVATIONS", path):
                callback()

    def test_current_observations_preserve_exact_historical_final(self):
        mapping = self.mapping()
        historical = phase.current_successor_blobs(mapping)
        current = phase.observed_candidate_blobs(mapping)
        self.assertEqual("35ed998540e044eed8cb7596e2e2c019ae369bd3",
                         historical[phase.M3_STRING_PATH])
        self.assertEqual("e3bffdb11669a80a8fe8e1ba127b23fb7efc3c1e",
                         current[phase.M3_STRING_PATH])
        self.assertEqual("56cb36e509ff4b700cbc4fc3064289402553fdf8",
                         current["src/java.base/share/classes/java/lang/M3StringPool.java"])
        self.assertEqual(4, len(historical))
        self.assertEqual(5, len(current))
        self.assertEqual("75d949e5aa2c8ff4e5c70d648e81fc0d37b68651",
                         phase.git_blob(phase.SUCCESSOR_CHAIN))

    def test_each_observation_field_refuses_drift(self):
        good = self.read_observations()
        mutations = 0
        for index in range(len(good)):
            for field in phase.CURRENT_SOURCE_HEADER:
                changed = [row.copy() for row in good]
                changed[index][field] = "UNREVIEWED"
                with self.subTest(row=index, field=field):
                    self.with_observations(changed, lambda: self.assertRaises(
                        ValueError, phase.observed_candidate_blobs, self.mapping()))
                mutations += 1
        self.assertEqual(48, mutations)

    def test_observation_missing_duplicate_reordered_and_promotion_refuse(self):
        good = self.read_observations()
        cases = [good[:-1], good + [good[-1].copy()], list(reversed(good)), []]
        for field in ("state", "evidence_kind"):
            changed = [row.copy() for row in good]
            changed[-1][field] = "RUNTIME_ACCEPTED"
            cases.append(changed)
        for changed in cases:
            self.with_observations(changed, lambda: self.assertRaises(
                ValueError, phase.observed_candidate_blobs, self.mapping()))

    def test_observation_map_escape_and_predecessor_drift_refuse(self):
        mapping = self.mapping()
        target = "src/java.base/share/classes/java/lang/M3StringCodePointPrecompute.java"
        removed = [row for row in mapping if row["target_path"] != target]
        with self.assertRaisesRegex(ValueError, "escaped source map"):
            phase.observed_candidate_blobs(removed)
        changed = [row.copy() for row in mapping]
        for row in changed:
            if row["target_path"] == target:
                row["target_git_blob"] = "0" * 40
        with self.assertRaisesRegex(ValueError, "discontinuous"):
            phase.observed_candidate_blobs(changed)

    def test_complete_current_source_gate_preserves_no_promotion(self):
        self.assertEqual(0, phase.main([str(SCRIPT)]))
        self.assertEqual(2, phase.main([str(SCRIPT), "unexpected"]))

    def test_current_source_drift_refuses_in_complete_gate(self):
        # Copy only the fixed source-map owners. The full checker still validates
        # all historical metadata and naming/phase/work-order responsibilities.
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            paths = {row["target_path"] for row in self.mapping()}
            for relative in paths:
                destination = root / relative
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes((phase.ROOT / relative).read_bytes())
            path = root / "src/java.base/share/classes/java/lang/M3StringPool.java"
            path.write_bytes(path.read_bytes() + b"\n// unreviewed drift\n")
            with patch.object(phase, "ROOT", root):
                with self.assertRaisesRegex(ValueError, "target owner drift"):
                    phase.main([str(SCRIPT)])


    def test_every_observation_row_refuses_nonempty_and_empty_extra_cells(self):
        good = phase.CURRENT_SOURCE_OBSERVATIONS.read_text(encoding="utf-8").splitlines()
        for index in range(1, len(good)):
            for extra in ("UNREVIEWED", ""):
                changed = good.copy()
                changed[index] += "\t" + extra
                with tempfile.TemporaryDirectory() as temporary:
                    path = Path(temporary) / "extra-cell.tsv"
                    path.write_text("\n".join(changed) + "\n", encoding="utf-8")
                    with patch.object(phase, "CURRENT_SOURCE_OBSERVATIONS", path):
                        with self.assertRaisesRegex(ValueError, "row arity drift"):
                            phase.observed_candidate_blobs(self.mapping())



if __name__ == "__main__":
    unittest.main()
