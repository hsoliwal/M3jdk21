#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Exact release denial/acceptance boundaries for all-JEP source/proof projection."""
from __future__ import annotations

import csv
import io
import json
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace

import all_jep_matrix as module
import jep_source_witness


class AllJepMatrixTest(unittest.TestCase):
    def fixture(self):
        entries = [
            (22, 457, "Classfile preview", "library", "superseded", "484"),
            (22, 458, "Multi-file launcher", "tool-launcher", "candidate", ""),
            (24, 484, "Classfile final", "library", "candidate", ""),
            (25, 511, "Language grammar change", "language", "reject-language", ""),
            (27, 523, "G1 default", "gc-runtime-config", "candidate", ""),
        ]
        catalogue = [dict(release=str(r), jep=str(j), title=t,
                          domain=d, disposition=status, superseded_by=next_jep)
                     for r, j, t, d, status, next_jep in entries]
        authority = {22: [457, 458], 24: [484], 25: [511], 27: [523]}
        witnesses = [jep_source_witness.Witness(r, j, t, status, 1, 0, 0, 0,
                    "PACKET_WITHOUT_RECEIPT") for r, j, t, d, status, _ in entries]
        witnesses[-1] = jep_source_witness.Witness(27, 523, "G1 default", "candidate",
                                                  1, 1, 1, 1, "RECORDED_PRODUCT_HASH_MATCHED")
        priority = [dict(jep="484", release="24", classification="OPT_IN_SE_API_EXTENSION",
                         default_java21="NO")]
        residue = [SimpleNamespace(jep=j, next_action="PROVE_OR_IMPLEMENT_COMPATIBLE_BACKPORT",
                                   receipt_next_action="") for j in (458, 484, 523)]
        return catalogue, authority, witnesses, priority, residue

    def matrix(self):
        return module.assemble(*self.fixture())

    def test_crosses_complete_authority_and_never_authorizes_direct_import(self):
        rows = self.matrix()
        self.assertEqual([457, 458, 484, 511, 523], [r.jep for r in rows])
        self.assertTrue(all(r.promotion == "NOT_AUTHORIZED_BY_MATRIX" for r in rows))
        self.assertTrue(all(r.donor == "https://github.com/openjdk/jdk" for r in rows))
        self.assertEqual("NO_JAVA21_DIRECT_IMPORT", rows[3].implementation_gate)
        self.assertEqual("SUPERSEDED_NO_DIRECT_IMPORT", rows[0].implementation_gate)
        self.assertEqual("FOLLOW_JEP_484", rows[0].next_action)
        self.assertEqual("NO", rows[2].java21_default)
        self.assertEqual("SOURCE_WITNESSED_NOT_QUALIFIED", rows[4].implementation_gate)
        self.assertIn("HOTSPOT_FASTDEBUG", rows[4].required_proofs)
        self.assertIn("JNI_JVMTI_NATIVE_PARITY", module.required_proofs("gc", "candidate", 423))

    def test_nonmatching_release_and_witness_are_rejected(self):
        catalogue, authority, witness, priority, residue = self.fixture()
        with self.assertRaisesRegex(ValueError, "denominator"):
            module.assemble(catalogue, {22: [457], 24: [484], 25: [511], 27: [523]},
                            witness, priority, residue)
        with self.assertRaisesRegex(ValueError, "witness"):
            module.assemble(catalogue, authority, witness[:-1], priority, residue)
        with self.assertRaisesRegex(ValueError, "release/decision drift"):
            witness[0] = jep_source_witness.Witness(23, 457, "wrong", "superseded", 0, 0, 0, 0, "NO_PACKET")
            module.assemble(catalogue, authority, witness, priority, residue)

    def test_policy_and_pending_residue_are_nonoptional(self):
        catalogue, authority, witnesses, priority, residue = self.fixture()
        with self.assertRaisesRegex(ValueError, "pending residue lost"):
            module.assemble(catalogue, authority, witnesses, priority, residue[:-1])
        priority.append(dict(jep="484", release="24", default_java21="OPT_IN"))
        with self.assertRaisesRegex(ValueError, "duplicate"):
            module.assemble(catalogue, authority, witnesses, priority, residue)

    def test_superseded_requires_concrete_released_successor(self):
        data = self.fixture()
        catalogue = data[0]
        catalogue[0]["superseded_by"] = "999"
        with self.assertRaisesRegex(ValueError, "superseding released"):
            module.assemble(*data)

    def test_invalid_source_never_disappears_under_policy_or_language_hold(self):
        data = self.fixture()
        data[2][-1] = jep_source_witness.Witness(27, 523, "G1 default", "candidate",
                    1, 1, 1, 0, "SOURCE_DRIFT")
        result = module.assemble(*data)
        self.assertEqual("SOURCE_PROVENANCE_BLOCKED", result[-1].implementation_gate)
        self.assertEqual("REPAIR_EXACT_PRODUCT_HASH_OR_RECEIPT", result[-1].next_action)

    def test_jfr_crypto_native_and_aot_categories_require_specific_proofs(self):
        self.assertEqual("JFR_MONITORING", module.problem_family("monitoring-jfr-runtime"))
        self.assertEqual("SECURITY_CRYPTOGRAPHY", module.problem_family("security-library"))
        self.assertEqual("NATIVE_INTEROP", module.problem_family("library-runtime-native"))
        self.assertIn("CRYPTO_PROVIDER_INTEROP", module.required_proofs("security-library", "candidate", 527))
        self.assertIn("JFR_EVENT_REGRESSION", module.required_proofs("monitoring-jfr", "candidate", 536))
        self.assertIn("AOT_CDS_REPLAY", module.required_proofs("runtime-aot-cds", "candidate", 483))
        self.assertEqual("JAVA21_SPEC_INCOMPATIBILITY", module.required_proofs("language", "reject-language", 511))

    def test_deterministic_tsv_root_and_physical_fields(self):
        rows = self.matrix()
        first = module.render_tsv(rows)
        self.assertEqual(first, module.render_tsv(rows))
        parsed = list(csv.DictReader(io.StringIO(first), delimiter="\t"))
        self.assertEqual(5, len(parsed))
        self.assertEqual("523", parsed[-1]["jep"])
        self.assertTrue(first.endswith("\n"))
        self.assertEqual(6, len(first.splitlines()))
        report = module.report(rows)
        self.assertFalse(report["all_features_implemented"])
        self.assertFalse(report["promotion_authorized"])
        self.assertEqual(5, report["released_jep_count"])
        self.assertEqual(64, len(report["tsv_sha256"]))
        self.assertEqual(json.dumps(report, sort_keys=True), json.dumps(module.report(rows), sort_keys=True))

    def test_output_fence_is_reused_not_bypassed(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            out = root / "m3/backports/target/all-jep-matrix.tsv"
            module._write(root, out, module.render_tsv(self.matrix()))
            self.assertTrue(out.is_file())
            with self.assertRaises(ValueError):
                module._write(root, root / "src/java.base/share/classes/java/lang/String.java", "illegal")
            source = root / "source.txt"
            source.write_text("preserve")
            link = root / "m3/backports/target/link.tsv"
            link.hardlink_to(source)
            with self.assertRaises(ValueError):
                module._write(root, link, "altered")
            self.assertEqual("preserve", source.read_text())


if __name__ == "__main__":
    unittest.main()
