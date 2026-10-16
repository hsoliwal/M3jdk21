#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import csv
import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


def load_module():
    path = Path(__file__).with_name("recipe_dag_handoff.py")
    spec = importlib.util.spec_from_file_location("m3_recipe_dag_handoff", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class RecipeDagHandoffTest(unittest.TestCase):
    def setUp(self) -> None:
        self.mod = load_module()

    def current_rows(self):
        path = Path(__file__).with_name("SYNEXIA_NEBULA_RECIPE_DAG_HANDOFF.tsv")
        return self.mod.read_rows(path)

    def test_current_receipt_is_valid_and_content_addressed(self) -> None:
        rows = self.mod.validate(self.current_rows())
        first = self.mod.summary(rows)
        second = self.mod.summary(self.mod.validate(self.current_rows()))

        self.assertEqual(first, second)
        self.assertEqual("M3_SYNEXIA_NEBULA_RECIPE_DAG_HANDOFF_V1", first["schema"])
        self.assertEqual("hsoliwal/M3jdk21", first["target"])
        self.assertEqual("PROVEN_RECIPE_DAG_ONLY", first["applicationPolicy"])
        self.assertFalse(first["mutationAuthority"])
        self.assertFalse(first["promotionAuthority"])
        self.assertTrue(first["nebulaReferenceOnly"])
        self.assertEqual("HOLD_PENDING_NEBULA_PROOF", first["nebulaJavaGradeState"])
        self.assertEqual(
            "7a5a3cb5ffd82bca671b20e1496db80369657578",
            first["nebulaJavaGradeCommit"],
        )
        self.assertRegex(first["root"], r"^[0-9a-f]{64}$")

    def test_successor_nebula_receiver_is_exact_and_keeps_native_current_head_pending(self) -> None:
        row = self.mod.validate_nebula_receiver()

        self.assertEqual("M3_NEBULA_MASTERED_DAG_RECEIVER_V2", row["schema"])
        self.assertEqual("ADMITTED", row["recipe_dag_transfer"])
        self.assertEqual("CANONICAL_SYNEXIA_APACHE_HANDOFF", row["recipe_custody"])
        self.assertEqual(
            "GREEN_ZERO_DRIFT_FROM_QUALIFIED_RUNTIME",
            row["runtime_source_equivalence"],
        )
        self.assertEqual("PENDING_EXTERNAL_CI", row["native_current_head"])
        self.assertEqual("PROVEN_RECIPE_DAG_ONLY", row["application_policy"])
        self.assertEqual("false", row["source_mutation"])
        self.assertEqual("false", row["semantic_equivalence"])
        self.assertEqual("false", row["compatibility_authority"])
        self.assertEqual("false", row["promotion"])

    def test_required_reusable_assets_are_exact_and_apache2(self) -> None:
        rows = self.mod.validate(self.current_rows())
        by_id = {row["id"]: row for row in rows}

        for asset_id, (path, digest) in self.mod.REQUIRED_ASSETS.items():
            row = by_id[asset_id]
            self.assertEqual("hsoliwal/com.synexia", row["repository"])
            self.assertEqual(self.mod.SYNEXIA_ASSET_COMMIT, row["commit"])
            self.assertEqual(path, row["path"])
            self.assertEqual("SHA-256", row["contentHashAlgorithm"])
            self.assertEqual(digest, row["contentHash"])
            self.assertEqual("Apache-2.0", row["license"])
            self.assertEqual("SYNEXIA_ORIGINAL", row["authorship"])
            self.assertEqual("COPY_ADAPT", row["delivery"])
            self.assertEqual("false", row["mutationAuthority"])
            self.assertEqual("false", row["promotionAuthority"])

    def test_nebula_java_grade_candidate_is_exact_and_hold_only(self) -> None:
        rows = self.mod.validate(self.current_rows())
        by_id = {row["id"]: row for row in rows}

        self.assertEqual(
            set(self.mod.NEBULA_JAVA_GRADE_ASSETS),
            {
                "nebula-java-grade-candidate",
                "nebula-v2-backport-plan",
                "nebula-v2-transfer-contract",
                "nebula-java-grade-doc",
            },
        )
        for asset_id, (kind, path, blob_sha1) in self.mod.NEBULA_JAVA_GRADE_ASSETS.items():
            row = by_id[asset_id]
            self.assertEqual(kind, row["kind"])
            self.assertEqual("hsoliwal/nebula", row["repository"])
            self.assertEqual(self.mod.NEBULA_JAVA_GRADE_REF, row["ref"])
            self.assertEqual(self.mod.NEBULA_JAVA_GRADE_COMMIT, row["commit"])
            self.assertEqual(path, row["path"])
            self.assertEqual("GIT-BLOB-SHA1", row["contentHashAlgorithm"])
            self.assertEqual(blob_sha1, row["contentHash"])
            self.assertEqual("EPL-2.0", row["license"])
            self.assertEqual("REFERENCE_ONLY", row["delivery"])
            self.assertEqual("HOLD_PENDING_NEBULA_PROOF", row["applicationPolicy"])
            self.assertEqual("false", row["mutationAuthority"])
            self.assertEqual("false", row["promotionAuthority"])

    def test_nebula_java_grade_candidate_drift_fails_closed(self) -> None:
        rows = [dict(row) for row in self.current_rows()]
        for row in rows:
            if row["id"] == "nebula-v2-backport-plan":
                row["contentHash"] = "0" * 40
        with self.assertRaises(ValueError):
            self.mod.validate(rows)

        rows = [dict(row) for row in self.current_rows()]
        for row in rows:
            if row["id"] == "nebula-java-grade-candidate":
                row["applicationPolicy"] = "PROVEN_RECIPE_DAG_ONLY"
        with self.assertRaises(ValueError):
            self.mod.validate(rows)

    def test_mutation_or_promotion_authority_fails_closed(self) -> None:
        rows = self.current_rows()
        mutated = [dict(row) for row in rows]
        mutated[0]["mutationAuthority"] = "true"
        with self.assertRaises(ValueError):
            self.mod.validate(mutated)

        promoted = [dict(row) for row in rows]
        promoted[0]["promotionAuthority"] = "true"
        with self.assertRaises(ValueError):
            self.mod.validate(promoted)

    def test_missing_or_divergent_canonical_asset_fails_closed(self) -> None:
        rows = [dict(row) for row in self.current_rows()]
        missing = [row for row in rows if row["id"] != "recipe-dag"]
        with self.assertRaises(ValueError):
            self.mod.validate(missing)

        drift = [dict(row) for row in rows]
        for row in drift:
            if row["id"] == "recipe-first-invariant":
                row["contentHash"] = "0" * 64
        with self.assertRaises(ValueError):
            self.mod.validate(drift)

    def test_wrong_target_policy_or_nebula_role_fails_closed(self) -> None:
        rows = [dict(row) for row in self.current_rows()]
        for row in rows:
            if row["id"] == "synexia-target-policy":
                row["applicationPolicy"] = "DIRECT_EDIT"
        with self.assertRaises(ValueError):
            self.mod.validate(rows)

        rows = [dict(row) for row in self.current_rows()]
        for row in rows:
            if row["id"] == "nebula-proving-transfer":
                row["delivery"] = "COPY_ADAPT"
        with self.assertRaises(ValueError):
            self.mod.validate(rows)

    def test_cli_writes_deterministic_json_receipt(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            out = Path(temp) / "handoff.json"
            self.assertEqual(0, self.mod.main(["--out", str(out)]))
            first = out.read_text(encoding="utf-8")
            self.assertEqual(0, self.mod.main(["--out", str(out)]))
            second = out.read_text(encoding="utf-8")
            self.assertEqual(first, second)


if __name__ == "__main__":
    unittest.main()
