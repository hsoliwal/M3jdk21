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
HISTORICAL_SUPERSEDED = "4f752ab79abf854d144e53118a2cc57a4b3cc2432140e9297079cca86dd2abab"
PREVIOUS_21U = "691bd90288f30d9d25504526f234abc7db862e577544a7733d3fe0d09e585ff2"
SUCCESSOR = "db65f0fdf1b3eba49a7819ab59a2c01f04b02c6e82daf8b55774d3c2404990aa"
SIMD_SOURCE_BLOB = "65860effc9f00df74c1dd0dcea1e2c1605f54cfa"
SIMD_NAME = b'template(java_util_DualPivotQuicksort,              "java/util/DualPivotQuicksort")'


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
            prefix="m3jdk8364182-lineage-"
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
        self.assertEqual(HISTORICAL_SUPERSEDED, current["superseded_target_sha256"])
        self.assertNotEqual(current["baseline_sha256"], current["target_sha256"])
        self.assertNotIn(
            current["superseded_target_sha256"],
            {current["baseline_sha256"], current["target_sha256"]},
        )

    def test_only_exact_one_line_simd_source_succeeds_after_jdk21u(self) -> None:
        source = (self.root / TARGET).read_bytes()
        self.assertEqual(SUCCESSOR, hashlib.sha256(source).hexdigest())
        self.assertEqual(SIMD_SOURCE_BLOB, self.verify.git_blob_sha1(source))
        lines = [
            line for line in source.splitlines(keepends=True)
            if SIMD_NAME in line
        ]
        self.assertEqual(1, len(lines))
        parent = source.replace(lines[0], b"", 1)
        self.assertEqual(PREVIOUS_21U, hashlib.sha256(parent).hexdigest())
        self.assertEqual(
            self.verify.JDK_8364182_21U_GIT_BLOB,
            self.verify.git_blob_sha1(parent),
        )
        for marker in (
            b"template(serializeSecurityPropertiesToByteArray_name,",
            b"template(java_lang_M3StringImmediate,",
            b"template(java_lang_M3String,",
        ):
            self.assertEqual(1, source.count(marker))

    def test_exact_previous_21u_source_remains_admissible(self) -> None:
        target = self.root / TARGET
        source = target.read_bytes()
        line = next(
            line for line in source.splitlines(keepends=True)
            if SIMD_NAME in line
        )
        target.write_bytes(source.replace(line, b"", 1))
        self.verify.verify_security_properties_8364182_backport(self.root)
        self.assertEqual(PREVIOUS_21U, hashlib.sha256(target.read_bytes()).hexdigest())

    def test_simd_duplication_mutation_or_lost_jmx_symbol_is_refused(self) -> None:
        target = self.root / TARGET
        source = target.read_bytes()
        line = next(
            line for line in source.splitlines(keepends=True)
            if SIMD_NAME in line
        )
        changes = (
            source.replace(b'"java/util/DualPivotQuicksort"',
                           b'"java/util/BrokenQuicksort"', 1),
            source.replace(line, line + line, 1),
            source.replace(
                b"serializeSecurityPropertiesToByteArray_name",
                b"serializeLostPropertiesToByteArray_name", 1,
            ),
        )
        for changed in changes:
            with self.subTest(sha=hashlib.sha256(changed).hexdigest()):
                target.write_bytes(changed)
                with self.assertRaisesRegex(AssertionError, "target drift"):
                    self.verify.verify_security_properties_8364182_backport(self.root)
        target.write_bytes(source)

    def test_historical_vm_symbols_pin_cannot_be_reassigned(self) -> None:
        for row in self.records:
            if row["target_path"] == TARGET:
                row["target_sha256"] = SUCCESSOR
        write_rows(self.root / LEDGER, self.columns, self.records)
        with self.assertRaisesRegex(AssertionError, "historical vmSymbols pin drift"):
            self.verify.verify_security_properties_8364182_backport(self.root)

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
