#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


def load_module():
    path = Path(__file__).with_name("nebula_proof_admission.py")
    spec = importlib.util.spec_from_file_location("nebula_proof_admission", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class NebulaProofAdmissionTest(unittest.TestCase):
    def setUp(self):
        self.mod = load_module()
        self.receipt = Path(__file__).with_name(
            "NEBULA_PROOF_GATED_ADMISSION_20261015.tsv"
        )

    def test_current_receipt_is_exact_hold_and_content_addressed(self):
        row = self.mod.validate(self.mod.read(self.receipt))
        first = self.mod.summary(row)
        second = self.mod.summary(self.mod.validate(self.mod.read(self.receipt)))

        self.assertEqual(first, second)
        self.assertEqual("HOLD", first["disposition"])
        self.assertEqual("HOLD_UNTIL_PROVEN", first["nebulaState"])
        self.assertEqual("DRAFT_UNMERGED", first["synexiaState"])
        self.assertFalse(first["packetExecution"])
        self.assertFalse(first["mutationAuthority"])
        self.assertFalse(first["promotionAuthority"])
        self.assertRegex(first["root"], r"^[0-9a-f]{64}$")

    def test_any_execution_or_authority_escalation_fails_closed(self):
        row = self.mod.read(self.receipt)
        for field in ("packet_execution", "mutation_authority", "promotion_authority"):
            changed = dict(row)
            changed[field] = "true"
            with self.assertRaises(ValueError):
                self.mod.validate(changed)

    def test_nebula_or_synexia_identity_drift_fails_closed(self):
        row = self.mod.read(self.receipt)
        for field in ("nebula_commit", "synexia_commit", "canonical_recipe_dag"):
            changed = dict(row)
            changed[field] = "0" * 40 if field.endswith("_commit") else "other.Recipe"
            with self.assertRaises(ValueError):
                self.mod.validate(changed)

    def test_cli_writes_deterministic_hold_receipt(self):
        with tempfile.TemporaryDirectory() as temp:
            out = Path(temp) / "admission.json"
            self.assertEqual(0, self.mod.main(["--out", str(out)]))
            first = out.read_text(encoding="utf-8")
            self.assertEqual(0, self.mod.main(["--out", str(out)]))
            self.assertEqual(first, out.read_text(encoding="utf-8"))
            self.assertIn('"disposition": "HOLD"', first)


if __name__ == "__main__":
    unittest.main()
