#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import importlib.util
import io
import sys
import tempfile
import unittest
from pathlib import Path


def load_module():
    path = Path(__file__).with_name("jep_residue.py")
    spec = importlib.util.spec_from_file_location("m3_jep_residue", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class JepResidueTest(unittest.TestCase):
    def setUp(self) -> None:
        self.r = load_module()

    def row(
        self,
        *,
        release: int,
        jep: int,
        disposition: str,
        title: str = "Feature",
        domain: str = "tools",
    ) -> dict[str, str]:
        return {
            "release": str(release),
            "jep": str(jep),
            "title": title,
            "domain": domain,
            "disposition": disposition,
            "reason": "",
            "superseded_by": "",
        }

    def priority(
        self,
        *,
        release: int,
        jep: int,
        classification: str = "COMPATIBLE_TOOLING",
        default_java21: str = "OPT_IN",
    ) -> dict[str, str]:
        return {
            "jep": str(jep),
            "release": str(release),
            "title": "Feature",
            "classification": classification,
            "default_java21": default_java21,
            "dependency": "",
            "owner_lane": "TOOLS",
            "evidence_url": "",
            "note": "",
        }

    def test_orders_candidates_before_high_risk_and_holds(self) -> None:
        catalogue = [
            self.row(release=24, jep=493, disposition="candidate"),
            self.row(release=22, jep=423, disposition="candidate"),
            self.row(release=24, jep=483, disposition="candidate-high-risk"),
            self.row(release=25, jep=500, disposition="hold-compat"),
            self.row(release=27, jep=531, disposition="hold-preview"),
            self.row(release=27, jep=537, disposition="hold-jit"),
            self.row(release=26, jep=401, disposition="reject-language"),
        ]
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            items = self.r.queue(root, catalogue, [])
        self.assertEqual([423, 493, 483, 500, 531, 537], [item.jep for item in items])
        self.assertEqual([20, 20, 40, 60, 70, 80], [item.priority for item in items])
        self.assertNotIn(401, [item.jep for item in items])
        self.assertTrue(all(item.evidence_state == "NO_RECIPE_EVIDENCE" for item in items))

    def test_detects_packet_and_recipe_class_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            packet = root / "m3/backports/recipes/jep-458-current"
            packet.mkdir(parents=True)
            (packet / "README.md").write_text("packet", encoding="utf-8")
            (packet / "CURRENT_TREE_RECEIPT.tsv").write_text(
                "field\tvalue\n"
                "packet_state\tPACKET_READY\n"
                "promotion\tNOT_AUTHORIZED\n"
                "next_action\tGENERATE_FILE_ATOMIC_CRATES\n",
                encoding="utf-8",
            )

            source = (
                root
                / "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport"
            )
            source.mkdir(parents=True)
            (source / "M3Jep485BackportRecipe.java").write_text(
                "class M3Jep485BackportRecipe {}",
                encoding="utf-8",
            )

            catalogue = [
                self.row(release=22, jep=458, disposition="candidate"),
                self.row(release=24, jep=485, disposition="candidate"),
            ]
            by_jep = {item.jep: item for item in self.r.queue(root, catalogue, [])}

        self.assertEqual({458, 485}, set(by_jep))
        self.assertEqual("MATERIALIZED_PACKET", by_jep[458].evidence_state)
        self.assertIn("jep-458-current", by_jep[458].evidence_paths)
        self.assertEqual("REVIEWED_POSTIMAGES_ALREADY_PRESENT", by_jep[458].receipt_state)
        self.assertEqual("NOT_AUTHORIZED", by_jep[458].promotion)
        self.assertEqual(
            "GENERATE_FILE_ATOMIC_CRATES",
            by_jep[458].receipt_next_action,
        )
        self.assertEqual("RECIPE_CLASS", by_jep[485].evidence_state)
        self.assertIn("M3Jep485BackportRecipe.java", by_jep[485].evidence_paths)

    def test_detects_compact_jep_packet_directory_names(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            packet = root / "m3/backports/recipes/j510"
            packet.mkdir(parents=True)
            (packet / "README.md").write_text("packet", encoding="utf-8")

            catalogue = [
                self.row(release=25, jep=510, disposition="candidate"),
            ]
            item = self.r.queue(root, catalogue, [])[0]

        self.assertEqual("PACKET_EVIDENCE", item.evidence_state)
        self.assertIn("m3/backports/recipes/j510", item.evidence_paths)

    def test_priority_matrix_metadata_is_joined_without_granting_compatibility(self) -> None:
        catalogue = [self.row(release=24, jep=493, disposition="candidate")]
        priorities = [
            self.priority(
                release=24,
                jep=493,
                classification="COMPATIBLE_TOOLING",
                default_java21="OPT_IN",
            )
        ]
        with tempfile.TemporaryDirectory() as temp:
            item = self.r.queue(Path(temp), catalogue, priorities)[0]
        self.assertEqual("COMPATIBLE_TOOLING", item.priority_classification)
        self.assertEqual("OPT_IN", item.default_java21)
        self.assertEqual(
            "PROVE_OR_IMPLEMENT_COMPATIBLE_BACKPORT",
            item.next_action,
        )

    def test_duplicate_jep_rows_fail_closed(self) -> None:
        rows = [
            self.row(release=24, jep=493, disposition="candidate"),
            self.row(release=24, jep=493, disposition="candidate"),
        ]
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaises(ValueError):
                self.r.queue(Path(temp), rows, [])

    def test_tsv_contains_mechanical_evidence_fields(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            items = self.r.queue(
                Path(temp),
                [self.row(release=22, jep=423, disposition="candidate")],
                [],
            )
        out = io.StringIO()
        self.r.write_tsv(items, out)
        text = out.getvalue()
        self.assertIn("evidence_state", text)
        self.assertIn("evidence_paths", text)
        self.assertIn("next_action", text)
        self.assertIn("receipt_state", text)
        self.assertIn("promotion", text)
        self.assertIn("receipt_next_action", text)
        self.assertIn("NO_RECIPE_EVIDENCE", text)

    def test_live_repository_queue_matches_released_pending_denominator(self) -> None:
        root = Path(__file__).resolve().parents[2]
        backports = root / "m3" / "backports"
        items = self.r.queue(
            root,
            self.r.read_tsv(backports / "JEP_CATALOGUE.tsv"),
            self.r.read_tsv(backports / "POST21_PRIORITY_COMPATIBILITY.tsv"),
        )
        self.assertEqual(42, len(items))
        by_jep = {item.jep: item for item in items}
        self.assertEqual("MATERIALIZED_PACKET", by_jep[458].evidence_state)
        self.assertEqual(
            "REVIEWED_POSTIMAGES_ALREADY_PRESENT",
            by_jep[458].receipt_state,
        )
        self.assertEqual("NOT_AUTHORIZED", by_jep[458].promotion)
        self.assertEqual("MATERIALIZED_PACKET", by_jep[467].evidence_state)
        self.assertEqual("PROOF_REPAIR_REQUESTED", by_jep[467].receipt_state)
        self.assertEqual("NOT_AUTHORIZED", by_jep[467].promotion)
        self.assertEqual(
            "RUN_REPAIRED_250_FILE_ATOMS_PLUS_DELETION_EVIDENCE_THEN_BUILD_LANGTOOLS_FIXED_POINT",
            by_jep[467].receipt_next_action,
        )
        self.assertEqual("MATERIALIZED_PACKET", by_jep[493].evidence_state)
        self.assertEqual("PACKET_READY", by_jep[493].receipt_state)
        self.assertEqual("NOT_AUTHORIZED", by_jep[493].promotion)
        self.assertEqual("MATERIALIZED_PACKET", by_jep[496].evidence_state)
        self.assertIn("m3/backports/recipes/j496", by_jep[496].evidence_paths)
        self.assertEqual("MATERIALIZED_PACKET", by_jep[497].evidence_state)
        self.assertIn("m3/backports/recipes/j497", by_jep[497].evidence_paths)
        self.assertEqual(
            "CANDIDATE_MATERIALIZED_UNVERIFIED",
            by_jep[496].receipt_state,
        )
        self.assertEqual("NOT_AUTHORIZED", by_jep[496].promotion)
        self.assertEqual(
            "PROVE_SHAKE_RECIPE_BUILD_FIPS203_JTREG_PROVIDER_FIXED_POINT_READBACK",
            by_jep[496].receipt_next_action,
        )
        self.assertEqual(
            "CANDIDATE_MATERIALIZED_UNVERIFIED",
            by_jep[497].receipt_state,
        )
        self.assertEqual("NOT_AUTHORIZED", by_jep[497].promotion)
        self.assertEqual(
            "FAN_IN_PR285_PROVE_JEP496_SHAKE_A3_BUILD_FIPS204_SECURITY_JTREG_FIXED_POINT_READBACK",
            by_jep[497].receipt_next_action,
        )
        self.assertEqual("MATERIALIZED_PACKET", by_jep[485].evidence_state)
        self.assertIn("m3/backports/recipes/j485", by_jep[485].evidence_paths)
        self.assertEqual(
            "CANDIDATE_MATERIALIZED_UNVERIFIED",
            by_jep[485].receipt_state,
        )
        self.assertEqual("NOT_AUTHORIZED", by_jep[485].promotion)
        self.assertEqual(
            "RUN_RECIPE_DAG_BUILD_GATHERER_JTREG_STREAM_REGRESSIONS_API_SMOKE_FIXED_POINT",
            by_jep[485].receipt_next_action,
        )
        self.assertEqual("MATERIALIZED_PACKET", by_jep[510].evidence_state)
        self.assertIn("m3/backports/recipes/j510", by_jep[510].evidence_paths)
        self.assertEqual("MATERIALIZED_PACKET", by_jep[523].evidence_state)
        self.assertIn("m3/backports/recipes/jep-523-g1-default", by_jep[523].evidence_paths)
        self.assertEqual(
            "REVIEWED_POSTIMAGES_ALREADY_PRESENT",
            by_jep[523].receipt_state,
        )
        self.assertEqual("NOT_AUTHORIZED", by_jep[523].promotion)
        self.assertEqual(
            "RUN_RECIPE_BUILD_G1_AND_NO_G1_JTREG_RUNTIME_FIXED_POINT",
            by_jep[523].receipt_next_action,
        )
        self.assertEqual(
            "CANDIDATE_MATERIALIZED_UNVERIFIED",
            by_jep[510].receipt_state,
        )
        self.assertEqual("NOT_AUTHORIZED", by_jep[510].promotion)
        self.assertEqual(
            "BUILD_JDK_IMAGE_RUN_KDF_JTREG_A3_FIXED_POINT_CANONICAL_READBACK",
            by_jep[510].receipt_next_action,
        )
        self.assertEqual("NO_RECIPE_EVIDENCE", by_jep[404].evidence_state)
        self.assertEqual("NO_RECIPE_EVIDENCE", by_jep[483].evidence_state)
        self.assertEqual("NO_RECIPE_EVIDENCE", by_jep[521].evidence_state)
        self.assertNotIn(401, by_jep)

    def test_live_repository_queue_file_matches_current_tree_evidence(self) -> None:
        root = Path(__file__).resolve().parents[2]
        backports = root / "m3" / "backports"
        items = self.r.queue(
            root,
            self.r.read_tsv(backports / "JEP_CATALOGUE.tsv"),
            self.r.read_tsv(backports / "POST21_PRIORITY_COMPATIBILITY.tsv"),
        )
        out = io.StringIO()
        self.r.write_tsv(items, out)
        self.assertEqual(
            out.getvalue(),
            (backports / "JEP_RESIDUE_QUEUE.tsv").read_text(encoding="utf-8"),
            "checked-in JEP residue queue is stale relative to current packet/recipe evidence",
        )


if __name__ == "__main__":
    unittest.main()
