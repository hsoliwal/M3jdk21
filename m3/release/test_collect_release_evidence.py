# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Real-Git regression fixtures for the evidence collector.

Receipts here are synthetic validator fixtures. They prove collection,
fixed-point and refusal behaviour only; none is a product release receipt.
"""
from __future__ import annotations

import contextlib
import importlib.util
import io
import json
import sys
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent


def load(name: str, filename: str):
    spec = importlib.util.spec_from_file_location(name, HERE / filename)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


release = load("m3_release_admission", "verify_release_admission.py")
collect = load("m3_release_collect", "collect_release_evidence.py")
fixtures = load("m3_release_admission_tests", "test_release_admission.py")


class CollectEvidenceGitTest(unittest.TestCase):
    def setUp(self):
        self.fx = fixtures.ReleaseEvidenceGitTest()
        self.fx.setUp()
        self.addCleanup(self.fx.doCleanups)
        self.root = self.fx.root
        self.out = Path(self.fx.tmp.name) / "current"
        self.receipts = Path(self.fx.tmp.name) / "receipts"
        self.receipts.mkdir()

    def write_receipts(self, gates=None):
        for gate in gates if gates is not None else self.fx.evidence["gates"]:
            folder = self.receipts / gate["id"]
            folder.mkdir(parents=True, exist_ok=True)
            for artifact in gate["artifacts"]:
                (folder / artifact["path"]).write_bytes((self.fx.evidence_dir / artifact["path"]).read_bytes())
            (folder / (gate["id"] + collect.RECEIPT_SUFFIX)).write_text(json.dumps(gate), encoding="utf-8")

    def run_cli(self, *argv) -> int:
        with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            return collect.main(["--root", str(self.root), *argv])

    def assemble(self) -> int:
        return self.run_cli("assemble", "--out", str(self.out), "--receipts", str(self.receipts))

    def test_gate_table_matches_contract(self):
        self.assertEqual(set(collect.GATES), set(self.fx.contract["mandatory_release_gates"]))

    def test_receipt_records_exit_code_environment_and_log_hash(self):
        code = self.run_cli("receipt", "--gate", "RELEASE_CLAIMS_VALIDATION", "--out", str(self.receipts),
                            "--toolchain", "fixture", "--", sys.executable, "-c", "print('synthetic ok')")
        self.assertEqual(code, 0)
        record = json.loads((self.receipts / "RELEASE_CLAIMS_VALIDATION.receipt.json").read_text())
        self.assertEqual((record["status"], record["exit_code"]), ("PASS", 0))
        self.assertEqual(record["source_commit"], self.fx.git("rev-parse", "HEAD"))
        for key in ("os", "architecture", "toolchain"):
            self.assertTrue(record["environment"][key].strip())
        log = self.receipts / record["artifacts"][0]["path"]
        self.assertIn("synthetic ok", log.read_text())
        self.assertEqual(release.sha256(log), record["artifacts"][0]["sha256"])

    def test_failed_command_is_recorded_and_blocks_assembly(self):
        code = self.run_cli("receipt", "--gate", "M3_FOUNDATION_TESTS", "--out", str(self.receipts),
                            "--toolchain", "fixture", "--", sys.executable, "-c", "raise SystemExit(7)")
        self.assertEqual(code, 7)
        record = json.loads((self.receipts / "M3_FOUNDATION_TESTS.receipt.json").read_text())
        self.assertEqual((record["status"], record["exit_code"]), ("FAIL", 7))
        self.assertEqual(self.assemble(), collect.EXIT_INCOMPLETE)
        rows = (self.out / "pending.tsv").read_text().splitlines()
        self.assertIn("M3_FOUNDATION_TESTS\tFAIL\treceipt FAIL exit_code=7", "\n".join(rows))
        self.assertEqual(json.loads((self.out / "evidence.json").read_text())["gates"], [])

    def test_complete_receipts_assemble_to_a_verifier_accepted_fixed_point(self):
        self.write_receipts()
        self.assertEqual(self.assemble(), 0)
        manifest = self.out / "evidence.json"
        first = manifest.read_bytes()
        self.assertEqual(self.assemble(), 0)
        self.assertEqual(first, manifest.read_bytes())
        self.assertEqual((self.out / "pending.tsv").read_text(), "gate\tstate\treason\n")
        result = release.validate(self.root, True, manifest)
        self.assertTrue(result["release_ready"], result)
        data = json.loads(first)
        self.assertEqual(data["schema"], release.EVIDENCE_SCHEMA)
        self.assertEqual([g["id"] for g in data["gates"]], self.fx.contract["mandatory_release_gates"])
        self.assertTrue(all(g["artifacts"][0]["path"].startswith(g["id"] + "/") for g in data["gates"]))

    def test_missing_gate_exits_3_and_keeps_partial_manifest_verifiable(self):
        removed = self.fx.evidence["gates"][-1]
        self.write_receipts(self.fx.evidence["gates"][:-1])
        self.assertEqual(self.assemble(), collect.EXIT_INCOMPLETE)
        self.assertIn(removed["id"] + "\tPENDING\tno receipt", (self.out / "pending.tsv").read_text())
        result = release.validate(self.root, True, self.out / "evidence.json")
        self.assertFalse(result["release_ready"])
        states = {c["name"]: c["status"] for c in result["checks"]}
        self.assertEqual(states["gate:" + removed["id"]], "PENDING")
        self.assertEqual(states["gate:" + self.fx.evidence["gates"][0]["id"]], "PASS")

    def test_stale_receipt_is_pending_not_imported(self):
        self.fx.evidence["gates"][0]["source_commit"] = "a" * 40
        self.write_receipts()
        self.assertEqual(self.assemble(), collect.EXIT_INCOMPLETE)
        text = (self.out / "pending.tsv").read_text()
        self.assertIn(self.fx.evidence["gates"][0]["id"] + "\tPENDING\tstale receipt(s)", text)

    def test_artifact_digest_mismatch_is_refused(self):
        self.write_receipts()
        gate = self.fx.evidence["gates"][0]
        (self.receipts / gate["id"] / gate["artifacts"][0]["path"]).write_text("changed bytes")
        with self.assertRaises(ValueError):
            self.assemble()

    def test_wrong_owner_is_refused(self):
        gate = next(g for g in self.fx.evidence["gates"] if g["id"] == "M3_MAVEN_CONTROL_REACTOR")
        gate["owner_repository"] = "hsoliwal/M3jdk21"
        self.write_receipts()
        with self.assertRaises(ValueError):
            self.assemble()

    def test_check_subcommand_requires_named_checks_to_pass(self):
        self.assertEqual(self.run_cli("check", "--prefix", "file:", "--prefix", "rights."), 0)
        self.assertEqual(self.run_cli("check", "--prefix", "git.single_release_commit"), 0)
        self.assertEqual(self.run_cli("check", "--prefix", "no.such.check"), 1)
        self.fx.write("src/example.java", "class Changed {}\n")
        self.assertEqual(self.run_cli("check", "--prefix", "git.clean_source"), 1)

    def test_run_refuses_gates_without_a_local_producer(self):
        for ident in ("M3_MAVEN_CONTROL_REACTOR", "STRING_UTF16_CONTRACT", "OPENJDK_SANITY_MATRIX"):
            with self.subTest(gate=ident):
                self.assertEqual(self.run_cli("run", "--gate", ident, "--out", str(self.receipts)),
                                 collect.EXIT_NO_PRODUCER)
        self.assertFalse(list(self.receipts.glob("*" + collect.RECEIPT_SUFFIX)))


if __name__ == "__main__":
    unittest.main()
