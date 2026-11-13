#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Regression coverage for immutable donor names and fail-closed receiving.

Data validation reads the actual checked-in receiver packet. Only the Git
ancestry boundary is mocked in unit tests, with its exact arguments checked;
these tests do not claim a local Git-history or product-runtime qualification.
"""

from __future__ import annotations

import contextlib
import csv
import importlib.util
import io
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


CHECKER_PATH = Path(__file__).with_name("check_m3_full_borrow_receiver.py")
SPEC = importlib.util.spec_from_file_location("full_borrow_receiver", CHECKER_PATH)
assert SPEC is not None and SPEC.loader is not None
receiver = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(receiver)

PACKET = Path("m3/m3-import/current-full-borrow")
SOURCE_BASELINE = "c9b07049c57ecdf43175f5885e11998918416a00"
DONOR_BLOBS = {
    "m3-estate.tsv": "7ffcc48f68e3804899e45c5d058b583ac655d5ea",
    "m3-dag.tsv": "3e6c56b8555231fb36d0ac620ab112aa5970d45a",
    "promotion-phases.tsv": "c4e8582f99caba32d718b2c1e86dae0854cdce3d",
}


class FullBorrowReceiverTest(unittest.TestCase):
    """Exercise real packet bytes in a disposable target-shaped directory."""

    def setUp(self) -> None:
        self.scratch = tempfile.TemporaryDirectory(prefix="m3-full-borrow-")
        self.addCleanup(self.scratch.cleanup)
        self.root = Path(self.scratch.name)
        self.base = self.root / PACKET
        self.base.mkdir(parents=True)
        source_root = CHECKER_PATH.resolve().parents[2]
        source_base = source_root / PACKET
        files = (
            "pin.tsv", "m3-estate.tsv", "m3-dag.tsv", "promotion-phases.tsv",
            "receiver-status.tsv", "historical-evidence.tsv",
        )
        for name in files:
            shutil.copyfile(source_base / name, self.base / name)
        with (source_base / "historical-evidence.tsv").open(
            encoding="utf-8", newline=""
        ) as stream:
            for row in csv.DictReader(stream, delimiter="\t"):
                relative = Path(row["evidence_path"])
                self.assertFalse(relative.is_absolute())
                self.assertNotIn("..", relative.parts)
                target = self.root / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(source_root / relative, target)
        locations = {
            "ROOT": self.root,
            "BASE": self.base,
            "PIN": self.base / "pin.tsv",
            "ESTATE": self.base / "m3-estate.tsv",
            "DAG": self.base / "m3-dag.tsv",
            "PHASES": self.base / "promotion-phases.tsv",
            "STATUS": self.base / "receiver-status.tsv",
            "HISTORICAL": self.base / "historical-evidence.tsv",
        }
        for field, path in locations.items():
            replacement = patch.object(receiver, field, path)
            replacement.start()
            self.addCleanup(replacement.stop)

    def pin(self) -> dict[str, str]:
        with receiver.PIN.open(encoding="utf-8", newline="") as stream:
            return dict(list(csv.reader(stream, delimiter="\t"))[1:])

    @staticmethod
    def read_rows(path: Path) -> tuple[list[str], list[dict[str, str]]]:
        with path.open(encoding="utf-8", newline="") as stream:
            reader = csv.DictReader(stream, delimiter="\t")
            return list(reader.fieldnames or []), list(reader)

    @staticmethod
    def write_rows(path: Path, header: list[str], rows: list[dict[str, str]]) -> None:
        with path.open("w", encoding="utf-8", newline="") as stream:
            writer = csv.DictWriter(
                stream, fieldnames=header, delimiter="\t", lineterminator="\n"
            )
            writer.writeheader()
            writer.writerows(rows)

    def test_pinned_donor_names_and_blob_identities(self) -> None:
        pin = self.pin()
        self.assertEqual("M3JDK21_SYNEXIA_FULL_BORROW_PIN_V1", pin["schema"])
        self.assertEqual(
            "docs/M3-SCALE/invariants/SYNEXIA-M3JDK21-CANONICAL-OWNERSHIP-1.md",
            pin["source_canonical_invariant"],
        )
        self.assertEqual(pin, receiver.PIN_EXPECTED)
        for name, expected in DONOR_BLOBS.items():
            with self.subTest(file=name):
                self.assertEqual(expected, receiver.git_blob(self.base / name))

    def test_pin_acceptance_checks_exact_git_ancestry(self) -> None:
        result = subprocess.CompletedProcess([], 0, "")
        with patch.object(receiver, "git", return_value=result) as ancestry:
            self.assertEqual(self.pin(), receiver.load_pin())
        ancestry.assert_called_once_with(
            "merge-base", "--is-ancestor", SOURCE_BASELINE, "HEAD", check=False
        )

    def test_missing_or_unrelated_git_history_is_refused(self) -> None:
        for returncode in (1, 128):
            with self.subTest(returncode=returncode):
                result = subprocess.CompletedProcess([], returncode, "no ancestry")
                with patch.object(receiver, "git", return_value=result) as ancestry:
                    with self.assertRaisesRegex(ValueError, "not an ancestor"):
                        receiver.load_pin()
                ancestry.assert_called_once_with(
                    "merge-base", "--is-ancestor", SOURCE_BASELINE, "HEAD", check=False
                )

    def test_pin_drift_is_refused_before_git(self) -> None:
        original = receiver.PIN.read_text(encoding="utf-8")
        changes = (
            ("M3JDK21_SYNEXIA_FULL_BORROW_PIN_V1", "M3JDK21_M3_FULL_BORROW_PIN_V1"),
            ("SYNEXIA-M3JDK21-CANONICAL", "M3-M3JDK21-CANONICAL"),
            ("296323958b1019edd59b60b9c05cb148d024cfe5", "0" * 40),
            ("automatic_application\tfalse", "automatic_application\ttrue"),
            ("family_completion\tfalse", "family_completion\ttrue"),
        )
        for before, after in changes:
            with self.subTest(value=before):
                self.assertIn(before, original)
                receiver.PIN.write_text(original.replace(before, after), encoding="utf-8")
                with patch.object(receiver, "git") as ancestry:
                    with self.assertRaisesRegex(ValueError, "pin drift"):
                        receiver.load_pin()
                    ancestry.assert_not_called()

    def test_exact_packet_validation_without_ancestry_stub(self) -> None:
        pin = self.pin()
        estate = receiver.load_estate(pin)
        self.assertEqual(32, len(estate))
        receiver.load_dag(pin, estate)
        receiver.load_phases(pin, estate)
        receiver.load_status(estate)
        receiver.load_historical_evidence(estate)

    def test_each_donor_file_rejects_byte_drift(self) -> None:
        pin = self.pin()
        estate = receiver.load_estate(pin)
        checks = (
            (receiver.ESTATE, lambda: receiver.load_estate(pin)),
            (receiver.DAG, lambda: receiver.load_dag(pin, estate)),
            (receiver.PHASES, lambda: receiver.load_phases(pin, estate)),
        )
        for path, check in checks:
            original = path.read_bytes()
            with self.subTest(file=path.name):
                path.write_bytes(original + b"\n")
                with self.assertRaisesRegex(ValueError, "pinned source Git blob"):
                    check()
                path.write_bytes(original)

    def test_schema_checks_remain_strict_after_component_hash(self) -> None:
        # Only the isolated component hash argument is refreshed. The production
        # pin validator would reject such a rewritten receipt before this point.
        pin = self.pin()
        estate = receiver.load_estate(pin)
        checks = (
            (receiver.ESTATE, "source_estate_git_blob", receiver.load_estate),
            (receiver.DAG, "source_dag_git_blob", lambda p: receiver.load_dag(p, estate)),
            (receiver.PHASES, "source_phase_git_blob", lambda p: receiver.load_phases(p, estate)),
        )
        for path, key, check in checks:
            original = path.read_bytes()
            header, rows = self.read_rows(path)
            with self.subTest(file=path.name):
                rows[0]["schema"] = rows[0]["schema"].replace("SYNEXIA_", "M3_", 1)
                self.write_rows(path, header, rows)
                with self.assertRaisesRegex(ValueError, "schema"):
                    check({**pin, key: receiver.git_blob(path)})
                path.write_bytes(original)

    def test_phase_order_and_state_remain_guarded(self) -> None:
        pin = self.pin()
        estate = receiver.load_estate(pin)
        original = receiver.PHASES.read_bytes()
        for mutation in ("order", "predecessor", "state"):
            header, rows = self.read_rows(receiver.PHASES)
            if mutation == "order":
                rows[0], rows[1] = rows[1], rows[0]
            elif mutation == "predecessor":
                rows[1]["predecessor"] = "-"
            else:
                rows[0]["state"] = "ACCEPTED"
            with self.subTest(mutation=mutation):
                self.write_rows(receiver.PHASES, header, rows)
                test_pin = {**pin, "source_phase_git_blob": receiver.git_blob(receiver.PHASES)}
                with self.assertRaisesRegex(ValueError, "order drift|predecessor drift|self-promote"):
                    receiver.load_phases(test_pin, estate)
                receiver.PHASES.write_bytes(original)

    def test_dependency_cycle_is_refused(self) -> None:
        pin = self.pin()
        estate = receiver.load_estate(pin)
        header, rows = self.read_rows(receiver.DAG)
        rows[0]["depends_on"] = rows[1]["family"]
        rows[1]["depends_on"] = rows[0]["family"]
        self.write_rows(receiver.DAG, header, rows)
        test_pin = {**pin, "source_dag_git_blob": receiver.git_blob(receiver.DAG)}
        with self.assertRaisesRegex(ValueError, "cycle"):
            receiver.load_dag(test_pin, estate)

    def test_receiver_status_and_receipts_cannot_self_promote(self) -> None:
        estate = receiver.load_estate(self.pin())
        original = receiver.STATUS.read_bytes()
        for field, value in (
            ("target_state", "VENDOR_CUSTODY"),
            ("target_state", "TARGET_ADAPTER"),
            ("target_state", "ACCEPTED"),
            ("proof_receipt", "fabricated-proof.json"),
            ("source_tree_or_blob", "0" * 40),
        ):
            header, rows = self.read_rows(receiver.STATUS)
            rows[0][field] = value
            with self.subTest(field=field, value=value):
                self.write_rows(receiver.STATUS, header, rows)
                with self.assertRaisesRegex(ValueError, "self-promote|invent proof|identity drift"):
                    receiver.load_status(estate)
                receiver.STATUS.write_bytes(original)

    def test_historical_evidence_must_exist_and_remain_historical(self) -> None:
        estate = receiver.load_estate(self.pin())
        original = receiver.HISTORICAL.read_bytes()
        for field, value in (
            ("evidence_path", "m3/missing.tsv"),
            ("evidence_path", "../escape.tsv"),
            ("evidence_state", "ACCEPTED"),
        ):
            header, rows = self.read_rows(receiver.HISTORICAL)
            rows[0][field] = value
            with self.subTest(field=field, value=value):
                self.write_rows(receiver.HISTORICAL, header, rows)
                with self.assertRaisesRegex(ValueError, "missing historical|invalid historical|self-promoted"):
                    receiver.load_historical_evidence(estate)
                receiver.HISTORICAL.write_bytes(original)

    def test_cli_reports_only_source_custody_with_mocked_ancestry(self) -> None:
        output = io.StringIO()
        result = subprocess.CompletedProcess([], 0, "")
        with patch.object(receiver, "git", return_value=result) as ancestry:
            with contextlib.redirect_stdout(output):
                self.assertEqual(0, receiver.main([str(CHECKER_PATH)]))
        ancestry.assert_called_once()
        self.assertIn("families=32", output.getvalue())
        self.assertIn("state=SOURCE_PIN_ONLY", output.getvalue())
        self.assertIn("completion=false", output.getvalue())
        self.assertIn("historical_mapping_evidence=preserved_not_accepted", output.getvalue())


if __name__ == "__main__":
    unittest.main()
