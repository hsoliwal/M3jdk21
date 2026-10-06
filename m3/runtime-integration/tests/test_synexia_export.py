#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Deterministic, source-blind proof for the Synexia -> M3JDK export lane."""
from __future__ import annotations

import importlib.util
import hashlib
import json
import struct
import sys
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
SPEC = importlib.util.spec_from_file_location("synexia_export", ROOT / "m3/runtime-integration/export-synexia-lexicon.py")
EXPORT = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = EXPORT
SPEC.loader.exec_module(EXPORT)
VERIFY_SPEC = importlib.util.spec_from_file_location("synexia_verify", ROOT / "m3/runtime-integration/verify-synexia-lexicon.py")
VERIFY = importlib.util.module_from_spec(VERIFY_SPEC)
assert VERIFY_SPEC.loader is not None
sys.modules[VERIFY_SPEC.name] = VERIFY
VERIFY_SPEC.loader.exec_module(VERIFY)


class SynexiaExportTest(unittest.TestCase):
    columns = EXPORT.RECORD_COLUMNS

    def write_inputs(self, root: Path, conflict: bool = False) -> tuple[Path, Path]:
        root.mkdir(parents=True, exist_ok=True)
        manifest = root / "sources.tsv"
        manifest.write_text(
            "\t".join(EXPORT.MANIFEST_COLUMNS) + "\n"
            "numbers\tNumbers\tsynexia-dictlang/src/main/java/com/synexia/dictlang/NumberLexicon.java\tnumber\tvalue,spelling\tM3StringFacts + NumberPrecompute\tApache-2.0\tfixture\n"
            "translations\tTranslations\tsynexia-translate/src/main/java/com/synexia/translate/Language.java\ttranslation_id\tlanguage_tag,source_id,target_id\tM3StringFacts + TranslationMapping\tApache-2.0\tfixture\n",
            encoding="utf-8",
        )
        rows = []
        for number in range(10001):
            rows.append(("numbers", "synexia-dictlang/src/main/java/com/synexia/dictlang/NumberLexicon.java",
                         "number", "und", str(number), str(number), str(number), f"NUMBER_{number}", "-", "M3StringFacts + NumberPrecompute"))
        rows.extend(("translations", "synexia-translate/src/main/java/com/synexia/translate/Language.java",
                     "translation", language, identity, word, identity, name, profile, "M3StringFacts + TranslationMapping")
                    for language, identity, word, name, profile in (
                        ("en", "en:common.proper_name.london", "London", "LONDON", "en->hi"),
                        ("hi", "hi:common.proper_name.london", "लंदन", "LONDON", "hi->en"),
                        ("en", "si.metre", "m", "METRE", "en->unit"),
                    ))
        if conflict:
            rows.append(rows[0][:-9] + ("different",) + rows[0][-8:])
        records = root / "records.tsv"
        with records.open("w", encoding="utf-8", newline="") as stream:
            stream.write("\t".join(self.columns) + "\n")
            stream.writelines("\t".join(row) + "\n" for row in rows)
        return manifest, records

    def run_export(self, root: Path, output: Path, conflict: bool = False):
        manifest, records = self.write_inputs(root, conflict)
        return EXPORT.export(manifest, records, output, "https://github.com/hsoliwal/com.synexia",
                             "3e85c872adf556901a341a9eb1c3b59864918da1")

    def test_preserves_ids_mappings_numbers_and_precompute(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            first = self.run_export(root / "input", root / "first")
            self.assertEqual(10004, first["counts"]["source_records"])
            self.assertEqual(10004, first["counts"]["image_records"])
            self.assertEqual(2, first["counts"]["source_families"])
            mapping = (root / "first/synexia.records.tsv").read_text(encoding="utf-8")
            self.assertIn("common.proper_name.london", mapping)
            self.assertIn("si.metre", mapping)
            self.assertIn("NUMBER_10000", mapping)
            facts = (root / "first/synexia.precompute.tsv").read_text(encoding="utf-8")
            self.assertIn("NumberPrecompute", facts)
            self.assertIn("M3StringFacts", facts)
            image = (root / "first/synexia.m3lex").read_bytes()
            magic, version, count, payload, units = struct.unpack_from(">QIIQQ", image)
            self.assertEqual(EXPORT.MAGIC, magic)
            self.assertEqual(2, version)
            self.assertEqual(10004, count)
            self.assertEqual(payload + 2 * units, len(image))
            self.assertEqual(image[32:64], hashlib.sha256(image[:32] + image[64:]).digest())
            expected_units = sum(len(str(number).encode("utf-16-le", "surrogatepass")) // 2
                                 for number in range(10001)) + sum(
                                     len(value.encode("utf-16-le", "surrogatepass")) // 2
                                     for value in ("London", "लंदन", "m"))
            self.assertEqual({"source_records": 10004, "image_records": 10004, "shards": 1,
                              "utf16_units": expected_units}, VERIFY.verify(root / "first"))

    def test_replay_is_byte_identical_except_for_output_location(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.run_export(root / "input", root / "one")
            self.run_export(root / "input", root / "two")
            for name in ("synexia.m3lex", "synexia.shards.tsv", "synexia.records.tsv",
                         "synexia.precompute.tsv", "synexia.export.json"):
                self.assertEqual((root / "one" / name).read_bytes(), (root / "two" / name).read_bytes(), name)
            manifest = json.loads((root / "one/synexia.export.json").read_text(encoding="utf-8"))
            self.assertEqual("M3LEX001", manifest["target"]["image_format"])
            self.assertEqual(2, manifest["target"]["image_version"])

    def test_conflicting_identity_is_rejected_before_output(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            with self.assertRaisesRegex(ValueError, "duplicate source/record identity"):
                self.run_export(root / "input", root / "output", conflict=True)
            self.assertFalse((root / "output").exists())

    def test_source_blind_verifier_rejects_post_export_mutation(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.run_export(root / "input", root / "output")
            self.assertEqual(10004, VERIFY.verify(root / "output")["image_records"])
            mapping = root / "output/synexia.records.tsv"
            original = mapping.read_bytes()
            mapping.write_bytes(original.replace(b"London", b"Lond0n", 1))
            with self.assertRaisesRegex(ValueError, "output hash mismatch"):
                VERIFY.verify(root / "output")
            mapping.write_bytes(original)

    def test_large_projection_is_sharded_without_renumbering_source_ids(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest, records = self.write_inputs(root / "input")
            with records.open("a", encoding="utf-8", newline="") as stream:
                for number in range(10001, 65537):
                    stream.write("\t".join((
                        "numbers", "synexia-dictlang/src/main/java/com/synexia/dictlang/NumberLexicon.java",
                        "number", "und", str(number), str(number), str(number),
                        f"NUMBER_{number}", "-", "M3StringFacts + NumberPrecompute")) + "\n")
            result = EXPORT.export(manifest, records, root / "output",
                                   "https://github.com/hsoliwal/com.synexia",
                                   "3e85c872adf556901a341a9eb1c3b59864918da1")
            self.assertEqual(2, result["counts"]["shards"])
            self.assertTrue((root / "output/synexia-000001.m3lex").exists())
            mapping = (root / "output/synexia.records.tsv").read_text(encoding="utf-8")
            rows = [line.split("\t") for line in mapping.splitlines()[1:]]
            self.assertTrue(any(row[4] == "en:common.proper_name.london" and row[6] == "1" for row in rows))


if __name__ == "__main__":
    unittest.main()
