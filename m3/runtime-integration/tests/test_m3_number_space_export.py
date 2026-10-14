#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""End-to-end proof: compiled M3NumberSpace -> M3LEX exporter -> blind verifier."""
from __future__ import annotations

import argparse
import csv
import hashlib
import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
SOURCE_ID = "dictlang.numbers.0-10000"
SOURCE_REVISION = "64a2ea61c73b548413fed6686a9daeeb0b9b0564"
EXPECTED_COUNT = 10_001
FIXTURE_COLUMNS = (
    "source_id",
    "source_revision",
    "min_value",
    "max_value",
    "precomputed_value_count",
    "record_id",
    "canonical_decimal_spelling",
    "shared_utf16_storage",
)

SPEC = importlib.util.spec_from_file_location(
    "synexia_export", ROOT / "m3/runtime-integration/export-synexia-lexicon.py")
EXPORT = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = EXPORT
SPEC.loader.exec_module(EXPORT)

VERIFY_SPEC = importlib.util.spec_from_file_location(
    "synexia_verify", ROOT / "m3/runtime-integration/verify-synexia-lexicon.py")
VERIFY = importlib.util.module_from_spec(VERIFY_SPEC)
assert VERIFY_SPEC.loader is not None
sys.modules[VERIFY_SPEC.name] = VERIFY
VERIFY_SPEC.loader.exec_module(VERIFY)


class M3NumberSpaceExportTest(unittest.TestCase):
    fixture_path: Path
    scratch_root: Path

    def test_current_number_owner_round_trips_all_ids_and_precomputes(self):
        self.assertTrue(self.fixture_path.is_file(), "compiled Java fixture output is required")
        self.assertTrue(self.scratch_root.is_dir(), "explicit scratch directory is required")
        with tempfile.TemporaryDirectory(dir=self.scratch_root) as temporary:
            root = Path(temporary)
            source_manifest = ROOT / "m3/lexicon/synexia-source-manifest.tsv"
            source_rows, _ = EXPORT.read_manifest(source_manifest)
            self.assertIn(SOURCE_ID, source_rows)
            source = source_rows[SOURCE_ID]
            target_map_path = ROOT / "m3/lexicon/synexia-number-target-map.tsv"
            with target_map_path.open("r", encoding="utf-8", newline="") as stream:
                target_rows = csv.DictReader(stream, delimiter="\\t")
                target_row = next(row for row in target_rows if row["source_id"] == SOURCE_ID)
            self.assertEqual(SOURCE_REVISION, target_row["source_revision"])
            self.assertIn("language_tag=und only as an envelope", source["data_policy"])
            self.assertIn("mapping_name=NUMBER_<record_id>", source["data_policy"])
            self.assertEqual("id", source["record_id_field"])
            self.assertEqual("value", source["mapping_fields"])
            self.assertEqual("M3StringFacts + M3NumberSpace", source["precompute_target"])

            projected_manifest = root / "number-source.tsv"
            with projected_manifest.open("w", encoding="utf-8", newline="") as stream:
                writer = csv.DictWriter(
                    stream,
                    fieldnames=EXPORT.MANIFEST_COLUMNS_V2,
                    delimiter="\t",
                    lineterminator="\n",
                )
                writer.writeheader()
                writer.writerow({column: source[column] for column in EXPORT.MANIFEST_COLUMNS_V2})

            records_path = root / "number-records.tsv"
            with self.fixture_path.open("r", encoding="utf-8", newline="") as fixture:
                reader = csv.DictReader(fixture, delimiter="\t")
                self.assertEqual(FIXTURE_COLUMNS, tuple(reader.fieldnames or ()))
                with records_path.open("w", encoding="utf-8", newline="") as records_stream:
                    writer = csv.DictWriter(
                        records_stream,
                        fieldnames=EXPORT.RECORD_COLUMNS_V2,
                        delimiter="\t",
                        lineterminator="\n",
                    )
                    writer.writeheader()
                    count = 0
                    for value, owner_row in enumerate(reader):
                        spelling = str(value)
                        self.assertEqual(SOURCE_ID, owner_row["source_id"])
                        self.assertEqual(SOURCE_REVISION, owner_row["source_revision"])
                        self.assertEqual("0", owner_row["min_value"])
                        self.assertEqual("10000", owner_row["max_value"])
                        self.assertEqual(str(EXPECTED_COUNT), owner_row["precomputed_value_count"])
                        self.assertEqual(spelling, owner_row["record_id"])
                        self.assertEqual(spelling, owner_row["canonical_decimal_spelling"])
                        self.assertEqual("true", owner_row["shared_utf16_storage"])

                        payload = {
                            "canonical_decimal_spelling": owner_row["canonical_decimal_spelling"],
                            "max_value": int(owner_row["max_value"]),
                            "min_value": int(owner_row["min_value"]),
                            "precomputed_value_count": int(owner_row["precomputed_value_count"]),
                            "record_id": owner_row["record_id"],
                            "shared_utf16_storage": owner_row["shared_utf16_storage"] == "true",
                            "source_id": owner_row["source_id"],
                            "source_revision": owner_row["source_revision"],
                        }
                        writer.writerow({
                            "source_id": SOURCE_ID,
                            "source_path": source["synexia_path"],
                            "source_kind": "number",
                            "language_tag": "und",
                            "record_id": owner_row["record_id"],
                            "lexeme": owner_row["canonical_decimal_spelling"],
                            "mapping_id": owner_row["record_id"],
                            "mapping_name": "NUMBER_" + owner_row["record_id"],
                            "translation_profile": "-",
                            "precompute_profile": source["precompute_target"],
                            "precompute_payload": json.dumps(
                                payload, ensure_ascii=False, sort_keys=True,
                                separators=(",", ":")),
                        })
                        count += 1
            self.assertEqual(EXPECTED_COUNT, count)

            field_map = ROOT / "m3/lexicon/synexia-precompute-field-map.tsv"
            output_a = root / "export-a"
            output_b = root / "export-b"
            first = EXPORT.export(
                projected_manifest, records_path, output_a,
                "https://github.com/hsoliwal/com.synexia", SOURCE_REVISION, field_map)
            second = EXPORT.export(
                projected_manifest, records_path, output_b,
                "https://github.com/hsoliwal/com.synexia", SOURCE_REVISION, field_map)
            self.assertEqual(EXPECTED_COUNT, first["counts"]["source_records"])
            self.assertEqual(EXPECTED_COUNT, first["counts"]["image_records"])
            self.assertEqual(1, first["counts"]["source_families"])
            self.assertEqual(first["counts"], second["counts"])

            mapping_path = output_a / "synexia.records.tsv"
            with mapping_path.open("r", encoding="utf-8", newline="") as stream:
                mapping_reader = csv.DictReader(stream, delimiter="\t")
                mapping_rows = list(mapping_reader)
            self.assertEqual(EXPECTED_COUNT, len(mapping_rows))
            by_record_id = {row["record_id"]: row for row in mapping_rows}
            self.assertEqual(EXPECTED_COUNT, len(by_record_id))
            required = set(source["precompute_fields"].split(","))
            self.assertEqual(8, len(required))
            self.assertEqual(required, set(EXPORT.read_field_map(field_map, {SOURCE_ID: source})[SOURCE_ID]))

            for value in range(EXPECTED_COUNT):
                record_id = str(value)
                row = by_record_id[record_id]
                self.assertEqual(SOURCE_ID, row["source_id"])
                self.assertEqual(source["synexia_path"], row["source_path"])
                self.assertEqual("number", row["source_kind"])
                self.assertEqual("und", row["language_tag"])
                self.assertEqual(record_id, row["record_id"])
                self.assertEqual(record_id, row["lexeme"])
                self.assertEqual(record_id, row["mapping_id"])
                self.assertEqual("NUMBER_" + record_id, row["mapping_name"])
                self.assertEqual("-", row["translation_profile"])
                self.assertEqual(source["precompute_target"], row["precompute_profile"])
                self.assertEqual({
                    "canonical_decimal_spelling": record_id,
                    "max_value": 10_000,
                    "min_value": 0,
                    "precomputed_value_count": EXPECTED_COUNT,
                    "record_id": record_id,
                    "shared_utf16_storage": True,
                    "source_id": SOURCE_ID,
                    "source_revision": SOURCE_REVISION,
                }, json.loads(row["precompute_payload"]))

            verified = VERIFY.verify(output_a)
            expected_units = sum(
                len(str(value).encode("utf-16-le", "surrogatepass")) // 2
                for value in range(EXPECTED_COUNT)
            )
            self.assertEqual({
                "source_records": EXPECTED_COUNT,
                "image_records": EXPECTED_COUNT,
                "shards": 1,
                "precompute_profiles": 1,
                "utf16_units": expected_units,
            }, verified)
            self.assertEqual(verified, VERIFY.verify(output_b))

            files_a = sorted(path.relative_to(output_a) for path in output_a.rglob("*") if path.is_file())
            files_b = sorted(path.relative_to(output_b) for path in output_b.rglob("*") if path.is_file())
            self.assertEqual(files_a, files_b)
            for relative in files_a:
                bytes_a = (output_a / relative).read_bytes()
                bytes_b = (output_b / relative).read_bytes()
                self.assertEqual(bytes_a, bytes_b, str(relative))
                self.assertRegex(hashlib.sha256(bytes_a).hexdigest(), r"^[0-9a-f]{64}$")

            duplicate_path = root / "duplicate-records.tsv"
            rows = records_path.read_text(encoding="utf-8").splitlines()
            duplicate_path.write_text(
                "\n".join(rows + [rows[1]]) + "\n", encoding="utf-8")
            rejected_output = root / "duplicate-output"
            with self.assertRaisesRegex(ValueError, "duplicate source/record identity"):
                EXPORT.export(
                    projected_manifest, duplicate_path, rejected_output,
                    "https://github.com/hsoliwal/com.synexia", SOURCE_REVISION, field_map)
            self.assertFalse(rejected_output.exists())


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--fixture", required=True, type=Path)
    parser.add_argument("--scratch-dir", required=True, type=Path)
    args = parser.parse_args()
    M3NumberSpaceExportTest.fixture_path = args.fixture.resolve(strict=True)
    M3NumberSpaceExportTest.scratch_root = args.scratch_dir.resolve(strict=True)
    unittest.main(argv=[sys.argv[0]], verbosity=2)


if __name__ == "__main__":
    main()
