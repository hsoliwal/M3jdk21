#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""One-way Java 21 compatibility and unqualified M3 extension queue boundaries."""

from __future__ import annotations

import contextlib
import csv
import importlib.util
import io
import itertools
import sys
import tempfile
import unittest
from pathlib import Path


def load_module():
    path = Path(__file__).with_name("jep_residue.py")
    spec = importlib.util.spec_from_file_location("m3_extension_residue_under_test", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class M3ExtensionResidueTest(unittest.TestCase):
    def setUp(self) -> None:
        self.r = load_module()

    @staticmethod
    def row(jep=511, release=25, domain="language", disposition="reject-language"):
        return {
            "release": str(release),
            "jep": str(jep),
            "title": "Module Import Declarations" if jep == 511 else "Fixture",
            "domain": domain,
            "disposition": disposition,
            "reason": "Requires separate contract review",
            "superseded_by": "",
        }

    @staticmethod
    def priority(jep=511, release=25, default="NO"):
        return {
            "jep": str(jep),
            "release": str(release),
            "title": "Module Import Declarations",
            "classification": "REJECT_LANGUAGE_SPEC",
            "default_java21": default,
        }

    def expanded(self, root, rows, priorities=()):
        return self.r.queue(root, rows, priorities, include_m3_extensions=True)

    def render(self, items, extensions=False):
        out = io.StringIO()
        self.r.write_tsv(items, out, include_m3_extensions=extensions)
        return out.getvalue()

    @staticmethod
    def write_rows(path, rows):
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(rows[0]), delimiter="\t")
            writer.writeheader()
            writer.writerows(rows)

    def test_language_rejection_remains_pending_m3_work(self):
        row = self.row()
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.assertEqual([], self.r.queue(root, [row], [self.priority()]))
            items = self.expanded(root, [row], [self.priority()])
        self.assertEqual([511], [item.jep for item in items])
        item = items[0]
        self.assertEqual("reject-language", item.disposition)
        self.assertEqual("REJECT_LANGUAGE_SPEC", item.priority_classification)
        self.assertEqual("NO", item.default_java21)
        self.assertEqual("NOT_AUTHORIZED_BY_QUEUE", item.promotion)
        self.assertEqual("NO_RECIPE_EVIDENCE", item.evidence_state)
        self.assertEqual(
            "DEFINE_M3_EXTENSION_MODE_AND_PROVE_JAVA21_PRESERVATION",
            item.next_action,
        )
        parsed = list(csv.DictReader(io.StringIO(self.render(items, True)), delimiter="\t"))[0]
        self.assertEqual("M3_EXTENSION", parsed["compatibility_lane"])
        self.assertEqual("PENDING_EXTENSION_CONTRACT_PROOF", parsed["m3_extension_state"])
        for required in (
            "EXPLICIT_M3_EXTENSION_CONTRACT",
            "JAVA21_SOURCE_BINARY_RUNTIME_PRESERVATION",
            "M3_COMPILER_VM_BUILD",
            "EXTENSION_POSITIVE_AND_NEGATIVE_TESTS",
            "JAVA21_JTREG_API_ABI_JNI_REGRESSION",
            "TARGET_RUNTIME_READBACK",
        ):
            self.assertIn(required, parsed["m3_extension_required_proofs"].split(";"))

    def test_existing_queue_rows_and_legacy_columns_are_unchanged(self):
        rows = [
            self.row(jep=423, release=22, domain="gc", disposition="candidate"),
            self.row(),
        ]
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            legacy = self.r.queue(root, rows, [])
            explicit_legacy = self.r.queue(root, rows, [], include_m3_extensions=False)
            extended = self.expanded(root, rows)
        self.assertEqual(legacy, explicit_legacy)
        self.assertEqual(legacy, extended[:len(legacy)])
        plain = list(csv.reader(io.StringIO(self.render(legacy)), delimiter="\t"))
        extra = list(csv.reader(io.StringIO(self.render(extended, True)), delimiter="\t"))
        self.assertEqual(15, len(plain[0]))
        self.assertEqual(18, len(extra[0]))
        self.assertEqual(plain, [row[:15] for row in extra[:len(plain)]])
        self.assertEqual("JAVA21_BACKPORT", extra[1][15])
        self.assertEqual("NOT_IN_EXTENSION_LANE", extra[1][16])

    def test_java21_removals_remain_rejected(self):
        rows = [
            self.row(jep=479, release=24, domain="removal", disposition="reject-compat"),
            self.row(jep=486, release=24, domain="compatibility-security", disposition="reject-compat"),
            self.row(jep=490, release=24, domain="removal-gc", disposition="reject-compat"),
            self.row(jep=503, release=25, domain="removal", disposition="reject-compat"),
            self.row(jep=504, release=26, domain="removal", disposition="reject-compat"),
        ]
        with tempfile.TemporaryDirectory() as temp:
            self.assertEqual([], self.expanded(Path(temp), rows))

    def test_unclassified_domains_and_other_rejections_are_not_upgraded(self):
        rows = [
            self.row(jep=998, domain="unclassified"),
            self.row(jep=999, disposition="reject-compat"),
        ]
        with tempfile.TemporaryDirectory() as temp:
            self.assertEqual([], self.expanded(Path(temp), rows))

    def test_packet_promotion_cannot_authorize_an_extension(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            packet = root / "m3/backports/recipes/jep-511-fixture"
            packet.mkdir(parents=True)
            (packet / "CURRENT_TREE_RECEIPT.tsv").write_text(
                "field\tvalue\n"
                "packet_state\tCLAIMED_QUALIFIED\n"
                "promotion\tAUTHORIZED\n"
                "next_action\tPROMOTE_IMMEDIATELY\n",
                encoding="utf-8",
            )
            items = self.expanded(root, [self.row()], [self.priority()])
        item = items[0]
        self.assertEqual("CLAIMED_QUALIFIED", item.receipt_state)
        self.assertEqual("PROMOTE_IMMEDIATELY", item.receipt_next_action)
        self.assertEqual("NOT_AUTHORIZED_BY_QUEUE", item.promotion)
        self.assertEqual(
            "DEFINE_M3_EXTENSION_MODE_AND_PROVE_JAVA21_PRESERVATION",
            item.next_action,
        )
        self.assertIn("PENDING_EXTENSION_CONTRACT_PROOF", self.render(items, True))

    def test_extension_cannot_inherit_a_default_java21_admission(self):
        for default in ("CANDIDATE", "OPT_IN", "YES"):
            with self.subTest(default=default), tempfile.TemporaryDirectory() as temp:
                with self.assertRaisesRegex(ValueError, "cannot be default Java21"):
                    self.expanded(Path(temp), [self.row()], [self.priority(default=default)])

    def test_missing_priority_row_still_has_explicit_no_default_and_no_promotion(self):
        with tempfile.TemporaryDirectory() as temp:
            item = self.expanded(Path(temp), [self.row()])[0]
        self.assertEqual("NO", item.default_java21)
        self.assertEqual("", item.priority_classification)
        self.assertEqual("NOT_AUTHORIZED_BY_QUEUE", item.promotion)

    def test_extension_identity_errors_are_refused(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            with self.assertRaisesRegex(ValueError, "duplicate JEP"):
                self.expanded(root, [self.row(), self.row()])
            with self.assertRaisesRegex(ValueError, "duplicate priority"):
                self.expanded(root, [self.row()], [self.priority(), self.priority()])
            with self.assertRaisesRegex(ValueError, "priority release mismatch"):
                self.expanded(root, [self.row()], [self.priority(release=24)])

    def test_extension_rows_cannot_be_hidden_in_a_legacy_tsv(self):
        with tempfile.TemporaryDirectory() as temp:
            items = self.expanded(Path(temp), [self.row()])
        out = io.StringIO()
        with self.assertRaisesRegex(ValueError, "explicit extension TSV projection"):
            self.r.write_tsv(items, out)
        self.assertEqual("", out.getvalue())

    def test_row_order_and_generator_inputs_are_deterministic(self):
        rows = [
            self.row(),
            self.row(jep=456, release=22),
            self.row(jep=423, release=22, domain="gc", disposition="candidate"),
        ]
        outputs = []
        with tempfile.TemporaryDirectory() as temp:
            for permutation in itertools.permutations(rows):
                items = self.expanded(Path(temp), (row for row in permutation))
                self.assertEqual([423, 456, 511], [item.jep for item in items])
                outputs.append(self.render(items, True))
        self.assertEqual(1, len(set(outputs)))

    def test_cli_flag_is_explicit_and_output_is_reviewable(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.write_rows(root / "m3/backports/JEP_CATALOGUE.tsv", [self.row()])
            self.write_rows(root / "m3/backports/POST21_PRIORITY_COMPATIBILITY.tsv", [self.priority()])
            plain = io.StringIO()
            with contextlib.redirect_stdout(plain):
                self.assertEqual(0, self.r.main(["--root", str(root)]))
            self.assertEqual(1, len(plain.getvalue().splitlines()))
            out = root / "extension-work.tsv"
            self.assertEqual(
                0,
                self.r.main(["--root", str(root), "--include-m3-extensions", "--out", str(out)]),
            )
            parsed = list(csv.DictReader(io.StringIO(out.read_text()), delimiter="\t"))
        self.assertEqual(["511"], [row["jep"] for row in parsed])
        self.assertEqual("NO", parsed[0]["default_java21"])
        self.assertEqual("NOT_AUTHORIZED_BY_QUEUE", parsed[0]["promotion"])
        self.assertEqual("M3_EXTENSION", parsed[0]["compatibility_lane"])

    def test_released_catalogue_keeps_all_language_history_without_qualification(self):
        root = Path(__file__).resolve().parents[2]
        backports = root / "m3/backports"
        catalogue = self.r.read_tsv(backports / "JEP_CATALOGUE.tsv")
        priorities = self.r.read_tsv(backports / "POST21_PRIORITY_COMPATIBILITY.tsv")
        legacy = self.r.queue(root, catalogue, priorities)
        extended = self.expanded(root, catalogue, priorities)
        expected = {
            int(row["jep"])
            for row in catalogue
            if row["domain"] == "language" and row["disposition"] == "reject-language"
        }
        language = [item for item in extended if item.disposition == "reject-language"]
        self.assertEqual(expected, {item.jep for item in language})
        self.assertIn(511, expected)
        self.assertIn(459, expected)  # Withdrawn donor is review work, never silently admitted.
        self.assertIn(532, expected)  # Current preview remains unqualified.
        self.assertEqual(legacy, extended[:len(legacy)])
        self.assertTrue(all(item.default_java21 == "NO" for item in language))
        self.assertTrue(all(item.promotion == "NOT_AUTHORIZED_BY_QUEUE" for item in language))
        self.assertTrue(all(item.disposition != "reject-compat" for item in extended))


if __name__ == "__main__":
    unittest.main()
