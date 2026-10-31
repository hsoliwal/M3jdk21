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
    path = Path(__file__).with_name("nebula_upstream_proof_admission.py")
    spec = importlib.util.spec_from_file_location(
        "nebula_upstream_proof_admission", path
    )
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class NebulaUpstreamProofAdmissionTest(unittest.TestCase):
    def setUp(self):
        self.mod = load_module()
        self.receipt = Path(__file__).with_name(
            "NEBULA_UPSTREAM_PROOF_ADMISSION_20261016.tsv"
        )

    def test_current_receipt_is_exact_hold_and_content_addressed(self):
        row = self.mod.validate(self.mod.read(self.receipt))
        first = self.mod.summary(row)
        second = self.mod.summary(self.mod.validate(self.mod.read(self.receipt)))

        self.assertEqual(first, second)
        self.assertEqual("HOLD", first["disposition"])
        self.assertEqual("EclipseNebula/nebula", first["upstreamRepository"])
        self.assertEqual(
            "5d0bcbb9c6e3a118685a5ec91c6f5d8eb7833893",
            first["upstreamRevision"],
        )
        self.assertEqual(11749, first["synexiaPr"])
        self.assertEqual(
            "HOLD_TRUSTED_VERIFIER_REQUIRED",
            first["physicalAtomExtraction"],
        )
        self.assertFalse(first["backportPacketExecution"])
        self.assertFalse(first["mutationAuthority"])
        self.assertFalse(first["promotionAuthority"])
        self.assertRegex(first["root"], r"^[0-9a-f]{64}$")

    def test_authority_or_atom_extraction_escalation_fails_closed(self):
        row = self.mod.read(self.receipt)
        for field in (
            "backport_packet_execution",
            "mutation_authority",
            "promotion_authority",
        ):
            changed = dict(row)
            changed[field] = "true"
            with self.assertRaises(ValueError):
                self.mod.validate(changed)

        changed = dict(row)
        changed["physical_atom_extraction"] = "VERIFIED"
        with self.assertRaises(ValueError):
            self.mod.validate(changed)

    def test_proof_identity_or_state_drift_fails_closed(self):
        row = self.mod.read(self.receipt)
        for field, value in (
            ("upstream_revision", "0" * 40),
            ("synexia_commit", "0" * 40),
            ("synexia_pr", "1"),
            ("synexia_workflow_state", "GREEN"),
        ):
            changed = dict(row)
            changed[field] = value
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
            self.assertIn('"synexiaPr": 11749', first)


if __name__ == "__main__":
    unittest.main()
