#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Synthetic target-source fixtures for the M3JDK receiver preflight."""

from __future__ import annotations

import copy
import json
from pathlib import Path
import tempfile
import unittest

from check import OWNER_PATHS, RECEIPT, ReceiverRefusal, git_blob, verify


REPO = Path(__file__).resolve().parents[3]


class QualifiedCompositeReceiverTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory(prefix="m3jdk-receiver-")
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.receipt_path = self.root / RECEIPT
        self.receipt_path.parent.mkdir(parents=True)
        self.data = json.loads((REPO / RECEIPT).read_text(encoding="utf-8"))
        for record in self.data["target_owner_preimages"]:
            path = self.root / record["path"]
            path.parent.mkdir(parents=True, exist_ok=True)
            content = ("// synthetic source preimage: " + record["symbol"] + "\n").encode()
            path.write_bytes(content)
            record["git_blob"] = git_blob(content)
        self.write_receipt()

    def write_receipt(self) -> None:
        self.receipt_path.write_text(json.dumps(self.data, indent=2) + "\n", encoding="utf-8")

    def refused(self, message: str) -> None:
        with self.assertRaisesRegex(ReceiverRefusal, message):
            verify(self.root)

    def test_exact_source_preimages_and_pending_stage_pass(self) -> None:
        result = verify(self.root)
        self.assertEqual(result["owners_verified"], len(OWNER_PATHS))
        self.assertEqual(result["families_pending"], 20)
        self.assertEqual(result["receipt"], "SOURCE_PINNED_NOT_RUNTIME_ADMITTED")

    def test_owner_drift_refused(self) -> None:
        target = self.root / self.data["target_owner_preimages"][0]["path"]
        target.write_bytes(target.read_bytes() + b"// changed\n")
        self.refused("TARGET_SOURCE_GIT_BLOB_DRIFT")

    def test_target_source_missing_refused(self) -> None:
        target = self.root / self.data["target_owner_preimages"][0]["path"]
        target.unlink()
        self.refused("TARGET_SOURCE_MISSING_OR_SYMLINK")

    def test_target_source_symlink_refused(self) -> None:
        target = self.root / self.data["target_owner_preimages"][0]["path"]
        elsewhere = self.root / "elsewhere.java"
        elsewhere.write_bytes(target.read_bytes())
        target.unlink()
        target.symlink_to(elsewhere)
        self.refused("TARGET_SOURCE_MISSING_OR_SYMLINK")

    def test_phase_advance_refused(self) -> None:
        self.data["qualification"]["active_stage"] = "COLLECTIONS"
        self.write_receipt()
        self.refused("QUALIFICATION_PREMATURE")

    def test_fabricated_runtime_admission_refused(self) -> None:
        self.data["qualification"]["target_admission"] = True
        self.write_receipt()
        self.refused("QUALIFICATION_PREMATURE")

    def test_fabricated_jtreg_pass_refused(self) -> None:
        self.data["qualification"]["target_jtreg"] = "PASS"
        self.write_receipt()
        self.refused("QUALIFICATION_GATE_UNPROVEN")

    def test_canonical_content_qualified_by_program_refused(self) -> None:
        self.data["canonicalization"]["immutable_leaf_qualification"] = "PROGRAM_VERSION"
        self.write_receipt()
        self.refused("CANONICAL_LEAF_SCOPE_DRIFT")

    def test_weak_class_identity_refused(self) -> None:
        self.data["canonicalization"]["class_runtime_identity"] = "BINARY_NAME_ONLY"
        self.write_receipt()
        self.refused("SEMANTIC_IDENTITY_DRIFT")

    def test_source_manifest_substitution_refused(self) -> None:
        self.data["source_recipes"][0]["source_manifest_git_blob"] = "0" * 40
        self.write_receipt()
        self.refused("SOURCE_SEAL_DRIFT")

    def test_recipe_oracle_is_not_installed_jdk_evidence(self) -> None:
        self.data["source_recipes"][1]["installed_jdk"] = "PASS"
        self.write_receipt()
        self.refused("SOURCE_GATES_UNPROVEN")

    def test_missing_family_refused(self) -> None:
        self.data["families"].pop()
        self.write_receipt()
        self.refused("REQUIRED_FAMILY_MISSING")

    def test_class_promoted_ahead_of_string_refused(self) -> None:
        klass = next(f for f in self.data["families"] if f["name"] == "M3Class")
        klass["state"] = "ADMITTED"
        self.write_receipt()
        self.refused("FAMILY_PREMATURE_PROMOTION")

    def test_const_promoted_without_compiler_and_jni_refused(self) -> None:
        self.data["canonicalization"]["const_alias_and_jni_array_lowering"] = "IMPLEMENTED"
        self.write_receipt()
        self.refused("CONST_SYNTAX_UNVERIFIED")

    def test_duplicate_recipe_refused(self) -> None:
        self.data["source_recipes"][1] = copy.deepcopy(self.data["source_recipes"][0])
        self.write_receipt()
        self.refused("SOURCE_RECIPE_DUPLICATE")


if __name__ == "__main__":
    unittest.main()
