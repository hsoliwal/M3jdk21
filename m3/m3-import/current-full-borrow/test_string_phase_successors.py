#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Refusal and live-target proof for approved Phase-1 M3 String successor images."""
from __future__ import annotations

import csv
import hashlib
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

    def read_facts_rows(self):
        with phase.FACTS_SUCCESSOR_CHAIN.open(encoding="utf-8", newline="") as handle:
            return list(csv.DictReader(handle, delimiter="\t"))

    def with_facts_rows(self, changed, callback):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "string-facts-target-successor-chain.tsv"
            with path.open("w", encoding="utf-8", newline="") as handle:
                writer = csv.DictWriter(
                    handle, fieldnames=phase.FACTS_SUCCESSOR_CHAIN_HEADER,
                    delimiter="\t", lineterminator="\n")
                writer.writeheader()
                writer.writerows(changed)
            with patch.object(phase, "FACTS_SUCCESSOR_CHAIN", path):
                callback()

    def read_pool_rows(self):
        with phase.POOL_SOURCE_OBSERVATION.open(encoding="utf-8", newline="") as handle:
            return list(csv.DictReader(handle, delimiter="\t"))

    def with_pool_rows(self, changed, callback):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "string-pool-source-observation.tsv"
            with path.open("w", encoding="utf-8", newline="") as handle:
                writer = csv.DictWriter(
                    handle, fieldnames=phase.POOL_SOURCE_OBSERVATION_HEADER,
                    delimiter="\t", lineterminator="\n")
                writer.writeheader()
                writer.writerows(changed)
            with patch.object(phase, "POOL_SOURCE_OBSERVATION", path):
                callback()

    def test_serial_chain_preserves_history_and_exact_final_identity(self):
        historical = phase.approved_successors(self.mapping())
        self.assertEqual("70d1d03c109126fdda36c54da21b484bbe368fd9",
                         historical[phase.M3_STRING_PATH]["successor_git_blob"])
        expected = phase.current_successor_blobs(self.mapping())
        self.assertEqual("35ed998540e044eed8cb7596e2e2c019ae369bd3",
                         phase.EXPECTED_STRING_CHAIN[-1][1])
        self.assertEqual("8e6210786da64740068e7b2414fb7adc67ede204",
                         expected[phase.M3_STRING_PATH])
        observed = self.read_merged_rows()
        self.assertEqual(["V1_TIP", "FIRST_PARENT", "SECOND_PARENT", "MERGED_RESULT",
                          "FIRST_PARENT", "SECOND_PARENT", "MERGED_RESULT"],
                         [row["role"] for row in observed[:7]])
        self.assertEqual([
            "35ed998540e044eed8cb7596e2e2c019ae369bd3",
            "1c9574a31bc50013d6d0827f740b4d6fa32d636e",
            "1aea17959495c7fee359d7c13ea4b876f4fb8b31",
            "8a57ca4f085e4e8f1f1d9f2ec0fdb2fc5888597e",
            "8a57ca4f085e4e8f1f1d9f2ec0fdb2fc5888597e",
            "e53f62018eaf8e5dbf9cc85969bf3828e6950712",
            "8ddd767e9198f5716ebde4aada80b91591125d9d"],
            [row["git_blob"] for row in observed[:7]])
        # The second merge continues exactly from the first observed merged result.
        self.assertEqual(observed[3]["git_blob"], observed[4]["git_blob"])
        self.assertEqual(observed[4]["source_commit"], observed[6]["first_parent_commit"])
        self.assertEqual(observed[5]["source_commit"], observed[6]["second_parent_commit"])
        self.assertEqual("OBSERVED_MERGED_SOURCE_NOT_RECIPE_RECONSTRUCTED",
                         observed[6]["state"])
        self.assertTrue(all(row["runtime_acceptance"] == "NOT_CLAIMED"
                            for row in observed))
        self.assertEqual("fcd108ab6181fec11d16eda811b29d18298f3fb1",
                         historical[phase.M3_STRING_FACTS_PATH]["successor_git_blob"])
        facts = self.read_facts_rows()
        self.assertEqual(["1", "2", "3", "4", "5"], [row["step"] for row in facts])
        self.assertEqual([
            "d8dd7dc30db6a9e3897146572bd4a7755c954ffb",
            "3e76b8c52eefc3b1983c701a0b9dfc953262a554",
            "14fc79e035ac7c8fe610b0fc9191ba428b683133",
            "bd4d7878ceb9d3c74f8f012c05cac4aff392a128",
            "1982345327c0b9dbba1954605864ca9bd3c211c5"],
            [row["successor_git_blob"] for row in facts])
        self.assertTrue(all(row["state"] == "CANONICAL_RECIPE_POSTIMAGE_SOURCE_ONLY"
                            and row["runtime_acceptance"] == "NOT_CLAIMED"
                            for row in facts[:4]))
        self.assertEqual("1982345327c0b9dbba1954605864ca9bd3c211c5",
                         expected[phase.M3_STRING_FACTS_PATH])
        self.assertEqual("514247a2e6c3704e03a9b47682f74e481f66585f",
                         historical[phase.M3_STRING_POOL_PATH]["successor_git_blob"])
        pool = self.read_pool_rows()
        self.assertEqual([
            "HISTORICAL_TIP", "COMMON_BASE", "CANONICAL_LEAF_POSTIMAGE",
            "CANONICAL_NATIVE_FORK", "OBSERVED_COMPONENT_INTEGRATION",
            "ALTERNATIVE_PREDECESSOR", "TARGET_SPECIFIC_RECIPE_POSTIMAGE",
            "CURRENT_COMMENT_SUCCESSOR", "CANONICAL_RECIPE_POSTIMAGE_SUCCESSOR"],
            [row["role"] for row in pool[:9]])
        self.assertEqual("2c9032d8d18d0e6f061a14144eb1c771eebb48a5",
                         expected[phase.M3_STRING_POOL_PATH])
        # The canonical recipe postimage successor starts from the observed comment successor.
        self.assertEqual("0d47d7e969086262705460c153583f659237c894", pool[7]["git_blob"])
        self.assertEqual("ab538f9e034e1d09e299618e3dc0fe25235cc2be", pool[8]["artifact_ref"])
        self.assertTrue(all(row["canonical_history"] == "PARTIAL"
                            and row["history_gap"] == "HISTORICAL_514_TO_COMMON_BASE_E5AB_UNRESOLVED"
                            and row["runtime_acceptance"] == "NOT_ACCEPTED"
                            and row["state"] == "SOURCE_OBSERVATION_ONLY"
                            for row in pool))
        with self.assertRaisesRegex(ValueError, "discontinuous Pool historical anchor"):
            phase.pool_source_observation(dict(expected), pool)
        self.assertEqual("c0074f4b144da23040e63e0ba8866d56d0d304ac",
                         historical[phase.M3_STRING_POSITION_PATH]["successor_git_blob"])
        self.assertEqual(phase.EXPECTED_POSITION_CHAIN[-1][1],
                         expected[phase.M3_STRING_POSITION_PATH])
        with self.assertRaisesRegex(ValueError, "discontinuous Facts successor: 1"):
            phase.facts_successor_observation(dict(expected), facts)
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
            (4, "git_blob", "35ed998540e044eed8cb7596e2e2c019ae369bd3"),
            (5, "canonical_resource_git_blob", "f" * 40),
            (5, "canonical_resource_sha256", "f" * 64),
            (6, "first_parent_commit", good[-1]["second_parent_commit"]),
            (6, "git_blob", "8a57ca4f085e4e8f1f1d9f2ec0fdb2fc5888597e"),
            (6, "runtime_acceptance", "ACCEPTED"),
            (6, "state", "RUNTIME_ACCEPTED"),
        ]:
            mutated = [row.copy() for row in good]
            mutated[row_index][field] = replacement
            merged_cases.append(mutated)
        for mutated in merged_cases:
            self.with_merged_rows(mutated, lambda: self.assertRaisesRegex(
                ValueError,
                "merged source observation inventory drift|unreviewed merged source observation",
                phase.current_successor_blobs, self.mapping()))

        facts = self.read_facts_rows()
        facts_cases = [facts[:-1], list(reversed(facts)), facts + [facts[-1].copy()]]
        for row_index in range(len(facts)):
            for field in phase.FACTS_SUCCESSOR_CHAIN_HEADER:
                mutated = [row.copy() for row in facts]
                mutated[row_index][field] = (
                    "ACCEPTED" if field == "runtime_acceptance"
                    else "RUNTIME_ACCEPTED" if field == "state"
                    else mutated[row_index][field] + "-drift")
                facts_cases.append(mutated)
        for mutated in facts_cases:
            self.with_facts_rows(mutated, lambda: self.assertRaisesRegex(
                ValueError, "Facts successor inventory drift|unreviewed Facts successor",
                phase.current_successor_blobs, self.mapping()))

        pool = self.read_pool_rows()
        pool_cases = [pool[:-1], list(reversed(pool)), pool + [pool[-1].copy()]]
        for row_index in range(len(pool)):
            for field in phase.POOL_SOURCE_OBSERVATION_HEADER:
                mutated = [row.copy() for row in pool]
                original = mutated[row_index][field]
                mutated[row_index][field] = (
                    "ACCEPTED" if field == "runtime_acceptance"
                    else "COMPLETE" if field == "canonical_history"
                    else "RESOLVED" if field == "history_gap"
                    else "RUNTIME_ACCEPTED" if field == "state"
                    else str(int(original) + 1) if field == "utf8_bytes"
                    else ("f" if original[0] != "f" else "0") + original[1:]
                    if field in ("source_commit", "git_blob", "sha256", "artifact_ref",
                                 "artifact_git_blob", "producer_git_blob")
                    and len(original) in (40, 64)
                    else original + "-drift")
                pool_cases.append(mutated)
        for mutated in pool_cases:
            self.with_pool_rows(mutated, lambda: self.assertRaisesRegex(
                ValueError, "Pool source observation inventory drift|unreviewed Pool source observation",
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
        # The original three-row V1 chain is an immutable prefix; later owners
        # append source-only chronology without rewriting this historical proof.
        data = b"".join(phase.SUCCESSOR_CHAIN.read_bytes().splitlines(keepends=True)[:4])
        self.assertEqual("75d949e5aa2c8ff4e5c70d648e81fc0d37b68651",
                         hashlib.sha1(f"blob {len(data)}\0".encode("ascii") + data).hexdigest())


    def test_live_precompute_target_matches_current_receipt_when_present(self):
        row = phase.load_precompute_observation()
        target = phase.ROOT / row["target_path"]
        if not target.is_file():
            self.skipTest("M3JDK receiver source tree is not present")
        self.assertEqual(row["current_target_git_blob"], phase.git_blob(target))


    def test_additional_owner_sources_match_separate_exact_receipts(self):
        mapping = self.mapping()
        self.assertEqual("51ddee1a26da2cf3c0aa8cd452da9ed83244ddd7",
                         phase.git_blob(phase.MAP))
        self.assertEqual(4, len(phase.current_successor_blobs(mapping)))
        observed = phase.additional_owner_successors(mapping)
        self.assertEqual(6, len(observed))
        self.assertFalse(set(observed) & set(phase.EXPECTED_SUCCESSOR_PATHS))
        self.assertEqual({
            path: images[1] for path, images in phase.ADDITIONAL_OWNER_BASES.items()},
            observed)
        for target, expected in observed.items():
            source = phase.ROOT / target
            self.assertTrue(source.is_file(), target)
            self.assertEqual(expected, phase.git_blob(source), target)
        rows = phase.load_tsv(phase.ADDITIONAL_OWNER_SUCCESSORS,
                              phase.ADDITIONAL_OWNER_HEADER)
        self.assertEqual(12, len(rows))
        self.assertTrue(all(row["runtime_acceptance"] == "NOT_ACCEPTED"
                            and row["state"] == "SOURCE_CUSTODY_ONLY" for row in rows))
        atom = [row for row in rows if row["owner"] == "M3StringAtom"]
        self.assertEqual(["CANONICAL_POSTIMAGE_BOUND", "TARGET_SOURCE_OBSERVATION_ONLY",
                          "TARGET_SOURCE_OBSERVATION_ONLY"],
                         [row["edge_custody"] for row in atom])
        self.assertTrue(all(row["owner_history"] ==
                            "PARTIAL_FINAL_CANONICAL_EDGE_UNRECONSTRUCTED" for row in atom))
        self.assertEqual("UNRECONSTRUCTED", atom[-1]["canonical_resource_git_blob"])

    def test_additional_owner_receipt_refuses_all_fields_order_and_discontinuity(self):
        mapping = self.mapping()
        good = phase.load_tsv(phase.ADDITIONAL_OWNER_SUCCESSORS,
                              phase.ADDITIONAL_OWNER_HEADER)
        cases = [good[:-1], list(reversed(good)), good + [good[-1].copy()]]
        for row_index in range(len(good)):
            for field in phase.ADDITIONAL_OWNER_HEADER:
                changed = [row.copy() for row in good]
                changed[row_index][field] = (
                    "ACCEPTED" if field == "runtime_acceptance"
                    else "RUNTIME_ACCEPTED" if field == "state"
                    else changed[row_index][field] + "-drift")
                cases.append(changed)
        changed = [row.copy() for row in good]
        changed[0]["unexpected"] = "unsealed"
        cases.append(changed)
        for changed in cases:
            with patch.object(phase, "load_tsv", return_value=changed):
                with self.assertRaisesRegex(
                        ValueError, "additional owner successor inventory drift|"
                        "unreviewed additional owner successor"):
                    phase.additional_owner_successors(mapping)
        for index in range(len(good)):
            changed = [row.copy() for row in good]
            changed[index]["predecessor_git_blob"] = "0" * 40
            expected = tuple(tuple(row[field] for field in phase.ADDITIONAL_OWNER_HEADER)
                             for row in changed)
            with patch.object(phase, "EXPECTED_ADDITIONAL_OWNER_SUCCESSORS", expected):
                with patch.object(phase, "load_tsv", return_value=changed):
                    with self.assertRaisesRegex(
                            ValueError, "discontinuous additional owner successor"):
                        phase.additional_owner_successors(mapping)

    def test_additional_owner_receipt_refuses_missing_duplicate_or_changed_historical_map(self):
        mapping = self.mapping()
        for target in phase.ADDITIONAL_OWNER_BASES:
            selected = [row for row in mapping if row["target_path"] == target]
            self.assertEqual(1, len(selected))
            missing = [row.copy() for row in mapping if row["target_path"] != target]
            duplicate = [row.copy() for row in mapping] + [selected[0].copy()]
            changed = [row.copy() for row in mapping]
            next(row for row in changed if row["target_path"] == target)["target_git_blob"] = "f" * 40
            for candidate in (missing, duplicate, changed):
                with self.assertRaisesRegex(
                        ValueError, "additional owner historical anchor drift"):
                    phase.additional_owner_successors(candidate)


    def immutable_tables(self):
        return [
            (phase.SUCCESSORS, phase.SUCCESSOR_HEADER),
            (phase.SUCCESSOR_CHAIN, phase.SUCCESSOR_CHAIN_HEADER),
            (phase.MERGED_SOURCE_OBSERVATION, phase.MERGED_SOURCE_OBSERVATION_HEADER),
            (phase.FACTS_SUCCESSOR_CHAIN, phase.FACTS_SUCCESSOR_CHAIN_HEADER),
            (phase.POOL_SOURCE_OBSERVATION, phase.POOL_SOURCE_OBSERVATION_HEADER),
            (phase.ADDITIONAL_OWNER_SUCCESSORS, phase.ADDITIONAL_OWNER_HEADER),
            (phase.PRECOMPUTE_OBSERVATION, phase.PRECOMPUTE_OBSERVATION_HEADER),
            (phase.WORK, phase.WORK_HEADER),
        ]

    def test_original_provenance_headers_keep_synexia_identity(self):
        for path, header in self.immutable_tables():
            with self.subTest(table=path.name):
                self.assertTrue(phase.load_tsv(path, header))
        self.assertIn("synexia_prs", phase.SUCCESSOR_HEADER)
        for header in (phase.SUCCESSOR_CHAIN_HEADER, phase.MERGED_SOURCE_OBSERVATION_HEADER,
                       phase.FACTS_SUCCESSOR_CHAIN_HEADER, phase.ADDITIONAL_OWNER_HEADER,
                       phase.WORK_HEADER):
            self.assertIn("synexia_pr", header)
            self.assertNotIn("m3_pr", header)
        self.assertIn("synexia_source_recipe_head", phase.PRECOMPUTE_OBSERVATION_HEADER)
        self.assertIn("synexia_source_head", phase.FACTS_SUCCESSOR_CHAIN_HEADER)
        self.assertIn("synexia_head", phase.WORK_HEADER)

    def test_every_original_table_refuses_renamed_reordered_or_extra_headers(self):
        for original, header in self.immutable_tables():
            lines = original.read_text(encoding="utf-8").splitlines(keepends=True)
            changed_headers = [list(reversed(header)), header + ["unreviewed_field"]]
            for index, name in enumerate(header):
                if name.startswith("synexia_"):
                    renamed = list(header)
                    renamed[index] = name.replace("synexia_", "m3_", 1)
                    changed_headers.append(renamed)
            for changed in changed_headers:
                with self.subTest(table=original.name, header=changed):
                    with tempfile.TemporaryDirectory() as temporary:
                        path = Path(temporary) / original.name
                        path.write_text("\t".join(changed) + "\n" + "".join(lines[1:]),
                                        encoding="utf-8")
                        with self.assertRaisesRegex(ValueError, "invalid header"):
                            phase.load_tsv(path, header)

    def test_historical_observation_literals_equal_immutable_rows(self):
        pairs = [
            (phase.MERGED_SOURCE_OBSERVATION, phase.MERGED_SOURCE_OBSERVATION_HEADER,
             phase.EXPECTED_MERGED_SOURCE_OBSERVATION),
            (phase.FACTS_SUCCESSOR_CHAIN, phase.FACTS_SUCCESSOR_CHAIN_HEADER,
             phase.EXPECTED_FACTS_SUCCESSOR_CHAIN),
            (phase.POOL_SOURCE_OBSERVATION, phase.POOL_SOURCE_OBSERVATION_HEADER,
             phase.EXPECTED_POOL_SOURCE_OBSERVATION),
            (phase.ADDITIONAL_OWNER_SUCCESSORS, phase.ADDITIONAL_OWNER_HEADER,
             phase.EXPECTED_ADDITIONAL_OWNER_SUCCESSORS),
        ]
        for path, header, expected in pairs:
            with self.subTest(table=path.name):
                self.assertEqual([dict(zip(header, values)) for values in expected],
                                 phase.load_tsv(path, header))
        self.assertEqual([phase.EXPECTED_PRECOMPUTE_OBSERVATION],
                         phase.load_tsv(phase.PRECOMPUTE_OBSERVATION,
                                        phase.PRECOMPUTE_OBSERVATION_HEADER))
        self.assertEqual("com.synexia.indexstring.MIndexStringSearchPlan",
                         phase.EXPECTED_PRECOMPUTE_OBSERVATION["source_symbol"])
        self.assertEqual("com.synexia.mindex.MIndexStringPrecomputeCache",
                         phase.EXPECTED_PRECOMPUTE_OBSERVATION["cache_symbol"])
        self.assertEqual("M3JDK_TARGET_LOCAL_CURSOR_AND_M3TQ_REUSE_NO_SYNEXIA_CODE_COPY",
                         phase.EXPECTED_PRECOMPUTE_OBSERVATION["implementation_origin"])

    def test_every_original_table_refuses_missing_or_extra_data_cells(self):
        for original, header in self.immutable_tables():
            lines = original.read_text(encoding="utf-8").splitlines(keepends=True)
            cells = lines[1].rstrip("\n").split("\t")
            self.assertEqual(len(header), len(cells))
            for changed in (cells[:-1], cells + ["unreviewed-extra-field"], cells + [""]):
                with self.subTest(table=original.name, columns=len(changed)):
                    with tempfile.TemporaryDirectory() as temporary:
                        path = Path(temporary) / original.name
                        path.write_text(lines[0] + "\t".join(changed) + "\n"
                                        + "".join(lines[2:]), encoding="utf-8")
                        with self.assertRaisesRegex(ValueError, "invalid row width"):
                            phase.load_tsv(path, header)

    def test_work_orders_preserve_source_identity_and_refuse_promotion(self):
        phase.validate_work_orders()
        good = phase.load_tsv(phase.WORK, phase.WORK_HEADER)
        cases = [good[:-1], good + [good[0].copy()]]
        for field, replacement in (("synexia_pr", "unknown"),
                                   ("synexia_head", "unsealed"),
                                   ("schema", "M3JDK21_STRING_WORK_RENAMED"),
                                   ("state", "RUNTIME_ACCEPTED")):
            changed = [row.copy() for row in good]
            changed[0][field] = replacement
            cases.append(changed)
        for changed in cases:
            with tempfile.TemporaryDirectory() as temporary:
                path = Path(temporary) / "work.tsv"
                with path.open("w", encoding="utf-8", newline="") as handle:
                    writer = csv.DictWriter(handle, fieldnames=phase.WORK_HEADER,
                                            delimiter="\t", lineterminator="\n")
                    writer.writeheader()
                    writer.writerows(changed)
                with patch.object(phase, "WORK", path):
                    with self.assertRaises(ValueError):
                        phase.validate_work_orders()

    def test_estate_source_path_is_original_synexia_owner(self):
        self.assertEqual(("296323958b1019edd59b60b9c05cb148d024cfe5", "Apache-2.0"),
                         phase.estate_snapshot())
        source = phase.ESTATE.read_text(encoding="utf-8")
        old = "synexia-mindex/runtime/src/main/java/com/synexia/mindex/MIndexString.java"
        self.assertIn(old, source)
        changed = source.replace(old,
                "m3-mindex/runtime/src/main/java/jdk/internal/m3/mindex/MIndexString.java")
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "estate.tsv"
            path.write_text(changed, encoding="utf-8")
            with patch.object(phase, "ESTATE", path):
                with self.assertRaisesRegex(ValueError, "estate source-path drift"):
                    phase.estate_snapshot()


    def test_all_original_ledger_bytes_are_preserved_as_prefixes(self):
        prefixes = {
            'string-additional-owner-successors.tsv': (9082, '1c8e5a1061a56f2d3c6349adaa270f383bb3c3385a17d8ff8ad705241149ae15'),
            'string-facts-target-successor-chain.tsv': (3298, '567fe7e914f4e91bda63f8cc6a3dacf021e3abb92667e01f856e32b40c0d0a91'),
            'string-pool-source-observation.tsv': (6649, '8248fe1b5c9554ca7bacd93b04aa6b67a83a2e358f95fe9417341b4b0a857119'),
            'string-target-merged-source-observation.tsv': (3461, '2aa09ffab116870ce657fbbbe61b548d82d6951db148e48fd10e7b6d86c93bb8'),
            'string-target-successor-chain.tsv': (736, 'cfdbd550ca16d6f28e718347409b328b677f473f1f498cb903da69c1cb386ca0'),
        }
        for name, (length, expected) in prefixes.items():
            with self.subTest(table=name):
                data = SCRIPT.with_name(name).read_bytes()
                self.assertGreater(len(data), length)
                self.assertEqual(expected, hashlib.sha256(data[:length]).hexdigest())

    def test_restoration_observations_do_not_claim_recipe_or_runtime_acceptance(self):
        string = self.read_merged_rows()
        self.assertEqual(["FIRST_PARENT", "OBSERVED_SINGLE_PARENT_SOURCE_SUCCESSOR"],
                         [row["role"] for row in string[7:]])
        self.assertEqual("dec547222b30df77f758135609adf2b7bf4948d4", string[7]["source_commit"])
        self.assertEqual(string[7]["source_commit"], string[8]["first_parent_commit"])
        self.assertEqual("a1d954c40142227f05e85e219bfe51373f0b8247", string[8]["source_commit"])
        self.assertEqual("OBSERVED_SOURCE_NOT_RECIPE_RECONSTRUCTED", string[8]["state"])
        facts = self.read_facts_rows()
        self.assertEqual("OBSERVED_SOURCE_NOT_RECIPE_RECONSTRUCTED", facts[4]["state"])
        for row in string[7:] + facts[4:]:
            self.assertEqual("NOT_RECONSTRUCTED", row["canonical_resource_path"])
            self.assertEqual("NOT_RECONSTRUCTED", row["canonical_resource_git_blob"])
            self.assertEqual("NOT_CLAIMED", row["runtime_acceptance"])
        pool = self.read_pool_rows()
        self.assertEqual(11, len(pool))
        for row in pool[9:]:
            self.assertEqual("OBSERVED_SINGLE_PARENT_TARGET_SOURCE", row["role"])
            self.assertEqual("NOT_RECONSTRUCTED", row["producer_class"])
            self.assertEqual("PARTIAL", row["canonical_history"])
            self.assertEqual("NOT_ACCEPTED", row["runtime_acceptance"])
        owners = phase.load_tsv(phase.ADDITIONAL_OWNER_SUCCESSORS, phase.ADDITIONAL_OWNER_HEADER)
        for row in owners[9:]:
            self.assertEqual("TARGET_SOURCE_OBSERVATION_ONLY", row["edge_custody"])
            self.assertEqual("UNRECONSTRUCTED", row["canonical_recipe"])
            self.assertEqual("UNRECONSTRUCTED", row["canonical_source_ref"])
            self.assertEqual("NOT_ACCEPTED", row["runtime_acceptance"])

    def test_single_parent_source_observation_requires_actual_predecessor_coordinates(self):
        good = self.read_merged_rows()
        current = {phase.M3_STRING_PATH: good[0]["git_blob"]}
        for field, bad in (("first_parent_commit", "0" * 40),
                           ("second_parent_commit", good[-1]["source_commit"])):
            changed = [row.copy() for row in good]
            changed[-1][field] = bad
            expected = tuple(tuple(row[key] for key in phase.MERGED_SOURCE_OBSERVATION_HEADER)
                             for row in changed)
            with patch.object(phase, "EXPECTED_MERGED_SOURCE_OBSERVATION", expected):
                with self.assertRaisesRegex(ValueError, "discontinuous single-parent source observation"):
                    phase.merged_source_observation(current, changed)

    def test_position_source_steps_refuse_every_field_and_preserve_history(self):
        good = self.read_chain()
        position = [row for row in good if row["target_path"] == phase.M3_STRING_POSITION_PATH]
        self.assertEqual(len(phase.EXPECTED_POSITION_CHAIN), len(position))
        self.assertEqual("c0074f4b144da23040e63e0ba8866d56d0d304ac", position[0]["predecessor_git_blob"])
        self.assertEqual("7b25004afe2b11ad87de90bae698d51b5533afb8", position[0]["successor_git_blob"])
        self.assertEqual("UNRECONSTRUCTED", position[0]["synexia_pr"])
        self.assertEqual("2626", position[0]["m3jdk_pr"])
        self.assertEqual(2, len(position))
        self.assertEqual("7b25004afe2b11ad87de90bae698d51b5533afb8", position[1]["predecessor_git_blob"])
        self.assertEqual("b4c733004be251a751def941b3bdc27a77dc2fab", position[1]["successor_git_blob"])
        self.assertEqual("NOT_RECORDED", position[1]["synexia_pr"])
        self.assertEqual("2953", position[1]["m3jdk_pr"])
        self.assertEqual("SOURCE_CANDIDATE_NOT_RUNTIME_ACCEPTED", position[1]["state"])
        binding = phase.POSITION_CANONICAL_SOURCE
        self.assertEqual("8ac9585a24f3b69d89874f08e7ae4c9325c2f8cd", binding["synexia_source_head"])
        self.assertEqual("0163b1474862ca463a64f24b45b0c6ca36aa4e73", binding["target_input_commit"])
        self.assertEqual("66ea1dfb06f26f79d9fc7b3c896c5833412c9faf", binding["canonical_manifest_git_blob"])
        self.assertEqual("3dfed2c88a4a78f13701e816ccf550401c2658d8bcbecf91cf35c3b23b8d0cb6",
                         binding["canonical_resource_sha256"])
        self.assertEqual("com.synexia.m3.MigrationVerificationContinuationJava20261112",
                         binding["canonical_recipe"])
        self.assertEqual("synexia-openrewrite-recipes/recipe-crates/m3jdk21-migration-verification-20261112/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-migration-verification-20261112-continuation-java/002.txt",
                         binding["canonical_resource_path"])
        source = (phase.ROOT / phase.M3_STRING_POSITION_PATH).read_bytes()
        current = position[1]["successor_git_blob"]
        phase.validate_position_canonical_binding(current, source)
        for field, bad in (("predecessor_git_blob", "f" * 40),
                           ("successor_git_blob", "f" * 40),
                           ("canonical_resource_git_blob", "f" * 40),
                           ("canonical_resource_sha256", "f" * 64),
                           ("canonical_resource_path", binding["canonical_manifest_path"]),
                           ("synexia_source_head", "UNRECORDED"),
                           ("canonical_recipe", "unreviewed.recipe"),
                           ("state", "RUNTIME_ACCEPTED"),
                           ("runtime_acceptance", "ACCEPTED")):
            with self.subTest(binding_field=field):
                with patch.dict(binding, {field: bad}):
                    with self.assertRaises(ValueError):
                        phase.validate_position_canonical_binding(current, source)
        with self.assertRaisesRegex(ValueError, "Position canonical source SHA-256 drift"):
            phase.validate_position_canonical_binding(current, source + b"\n")
        with self.assertRaisesRegex(ValueError, "Position canonical source binding drift"):
            phase.validate_position_canonical_binding(position[1]["predecessor_git_blob"], source)
        for index in range(3, len(good)):
            for field in phase.SUCCESSOR_CHAIN_HEADER:
                changed = [row.copy() for row in good]
                changed[index][field] = ("RUNTIME_ACCEPTED" if field == "state"
                                         else changed[index][field] + "-drift")
                with self.subTest(step=index, field=field):
                    self.with_chain(changed, lambda: self.assertRaises(
                        ValueError, phase.current_successor_blobs, self.mapping()))


if __name__ == "__main__":
    unittest.main()
