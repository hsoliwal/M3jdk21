#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Source-bound JDK-8364182 successor-state differential/fail-closed tests."""

from __future__ import annotations

import csv
import hashlib
import importlib.util
import shutil
import sys
import tempfile
import unittest
from pathlib import Path


BACKPORTS = Path(__file__).resolve().parent
REPOSITORY = BACKPORTS.parents[1]
sys.path.insert(0, str(BACKPORTS))
TARGET = "src/hotspot/share/classfile/vmSymbols.hpp"
LEDGER = "m3/backports/recipes/jdk-8364182/adaptation.tsv"
SEEDS = "m3/backports/UPSTREAM_CHANGE_SEEDS.tsv"
SUCCESSOR = "4f752ab79abf854d144e53118a2cc57a4b3cc2432140e9297079cca86dd2abab"


def verifier():
    spec = importlib.util.spec_from_file_location(
        "m3_backport_source_lineage_verify", BACKPORTS / "verify.py"
    )
    if spec is None or spec.loader is None:
        raise RuntimeError("cannot load canonical JDK backport verifier")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def rows(path: Path) -> tuple[list[str], list[dict[str, str]]]:
    with path.open("r", encoding="utf-8", newline="") as source:
        parsed = csv.DictReader(source, delimiter="\t")
        return list(parsed.fieldnames or ()), list(parsed)


def write_rows(path: Path, columns: list[str], values: list[dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as target:
        writer = csv.DictWriter(
            target, fieldnames=columns, delimiter="\t", lineterminator="\n"
        )
        writer.writeheader()
        writer.writerows(values)


class Jdk8364182LineageTest(unittest.TestCase):
    def setUp(self) -> None:
        self.verify = verifier()
        self.temporary = tempfile.TemporaryDirectory(
            prefix="m3-jdk8364182-lineage-"
        )
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        columns, records = rows(REPOSITORY / LEDGER)
        self.columns = columns
        self.records = records
        self.assertEqual(7, len(records))
        self.assertEqual(7, len({record["target_path"] for record in records}))

        for source_path in (LEDGER, SEEDS, *(r["target_path"] for r in records)):
            original = REPOSITORY / source_path
            target = self.root / source_path
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(original, target)

    def test_exact_current_successor_is_compatible_with_original_pin(self) -> None:
        self.verify.verify_security_properties_8364182_backport(self.root)
        symbol = self.root / TARGET
        self.assertEqual(SUCCESSOR, hashlib.sha256(symbol.read_bytes()).hexdigest())
        current = next(r for r in self.records if r["target_path"] == TARGET)
        self.assertEqual(SUCCESSOR, current["superseded_target_sha256"])
        self.assertNotEqual(current["baseline_sha256"], current["target_sha256"])
        self.assertNotIn(
            current["superseded_target_sha256"],
            {current["baseline_sha256"], current["target_sha256"]},
        )

    def test_unknown_hotswap_drift_still_fails(self) -> None:
        target = self.root / TARGET
        target.write_bytes(target.read_bytes() + b"\n// unauthorized drift\n")
        with self.assertRaisesRegex(AssertionError, "target drift"):
            self.verify.verify_security_properties_8364182_backport(self.root)

    def test_modified_successor_pin_fails_even_when_source_is_unchanged(self) -> None:
        for row in self.records:
            if row["target_path"] == TARGET:
                row["superseded_target_sha256"] = "0" * 64
        write_rows(self.root / LEDGER, self.columns, self.records)
        with self.assertRaisesRegex(AssertionError, "superseded pin drift"):
            self.verify.verify_security_properties_8364182_backport(self.root)

    def test_no_other_adaptation_row_can_accept_a_successor(self) -> None:
        for row in self.records:
            if row["target_path"] != TARGET:
                row["superseded_target_sha256"] = SUCCESSOR
                break
        write_rows(self.root / LEDGER, self.columns, self.records)
        with self.assertRaisesRegex(AssertionError, "unexpected.*superseded"):
            self.verify.verify_security_properties_8364182_backport(self.root)

    def test_baseline_and_original_target_cannot_alias_successor(self) -> None:
        for row in self.records:
            if row["target_path"] == TARGET:
                row["baseline_sha256"] = SUCCESSOR
        write_rows(self.root / LEDGER, self.columns, self.records)
        with self.assertRaisesRegex(AssertionError, "aliases a pinned state"):
            self.verify.verify_security_properties_8364182_backport(self.root)


if __name__ == "__main__":
    unittest.main()
