#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Deterministic, source-blind proof for the Synexia -> M3JDK export lane."""
from __future__ import annotations

import importlib.util
import csv
import hashlib
import json
import struct
import subprocess
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

    def write_inputs(self, root: Path, conflict: bool = False,
                     with_payload: bool = False) -> tuple[Path, Path]:
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
        columns = EXPORT.RECORD_COLUMNS
        if with_payload:
            enriched = []
            for row in rows:
                if row[0] == "numbers":
                    payload = {"corpus_count": int(row[4]) + 1,
                               "frequency_rank": int(row[4])}
                else:
                    payload = {
                        "concept_ids": [101, 102],
                        "corpus_count": 7,
                        "document_frequency": 3,
                        "expansion_word_ids": [201, 202],
                        "language": row[3],
                        "lemma_id": 11,
                        "lexical_rank": 5,
                        "mapping_id": row[6],
                        "memberships": [301],
                        "morphology_mask": 16,
                        "phonetic_id": 13,
                        "pos_mask": 8,
                        "presence64": 17,
                        "sim_hash64": 19,
                        "script_ordinal": 25,
                        "stem_id": 7,
                        "subjects": [401],
                        "utf16_length": len(row[5].encode("utf-16-le", "surrogatepass")) // 2,
                        "code_point_length": len(row[5]),
                        "first_code_point": ord(row[5][0]),
                        "last_code_point": ord(row[5][-1]),
                    }
                enriched.append(row + (json.dumps(payload, ensure_ascii=False),))
            rows = enriched
            columns = EXPORT.RECORD_COLUMNS_V2
        records = root / "records.tsv"
        with records.open("w", encoding="utf-8", newline="") as stream:
            stream.write("\t".join(columns) + "\n")
            stream.writelines("\t".join(row) + "\n" for row in rows)
        return manifest, records

    def run_export(self, root: Path, output: Path, conflict: bool = False,
                   with_payload: bool = False):
        manifest, records = self.write_inputs(root, conflict, with_payload)
        return EXPORT.export(manifest, records, output, "https://github.com/hsoliwal/com.synexia",
                             "3e85c872adf556901a341a9eb1c3b59864918da1")

    def test_preserves_ids_mappings_numbers_and_precompute(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            first = self.run_export(root / "input", root / "first", with_payload=True)
            self.assertEqual(10004, first["counts"]["source_records"])
            self.assertEqual(10004, first["counts"]["image_records"])
            self.assertEqual(2, first["counts"]["source_families"])
            mapping = (root / "first/synexia.records.tsv").read_text(encoding="utf-8")
            self.assertIn("common.proper_name.london", mapping)
            self.assertIn("si.metre", mapping)
            self.assertIn("NUMBER_10000", mapping)
            mapping_rows = list(csv.DictReader(mapping.splitlines(), delimiter="\t"))
            number_zero = next(row for row in mapping_rows if row["record_id"] == "0")
            self.assertEqual('{"corpus_count":1,"frequency_rank":0}',
                             number_zero["precompute_payload"])
            london = next(row for row in mapping_rows
                          if row["record_id"] == "en:common.proper_name.london")
            london_payload = json.loads(london["precompute_payload"])
            self.assertEqual("en", london_payload["language"])
            self.assertEqual([101, 102], london_payload["concept_ids"])
            self.assertEqual([201, 202], london_payload["expansion_word_ids"])
            self.assertEqual(13, london_payload["phonetic_id"])
            self.assertEqual(19, london_payload["sim_hash64"])
            self.assertEqual(25, london_payload["script_ordinal"])
            facts = (root / "first/synexia.precompute.tsv").read_text(encoding="utf-8")
            self.assertIn("NumberPrecompute", facts)
            self.assertIn("M3StringFacts", facts)
            profiles = (root / "first/synexia.precompute-index.tsv").read_text(encoding="utf-8")
            self.assertIn("M3StringFacts + NumberPrecompute", profiles)
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
                              "precompute_profiles": 2,
                              "utf16_units": expected_units}, VERIFY.verify(root / "first"))

    def test_legacy_input_defaults_to_empty_owner_payload(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.run_export(root / "input", root / "output")
            mapping = (root / "output/synexia.records.tsv").read_text(encoding="utf-8")
            self.assertIn("precompute_payload", mapping.splitlines()[0])
            self.assertIn("\t{}\n", mapping)
            VERIFY.verify(root / "output")

    def test_owner_payload_rejects_ambiguous_or_non_object_json(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest, records = self.write_inputs(root / "input", with_payload=True)
            original = records.read_text(encoding="utf-8")
            for attempt, invalid in enumerate(('"[1]"', '"{""a"":1,""a"":2}"', '"NaN"')):
                records.write_text(original.replace(
                    '{"corpus_count": 1, "frequency_rank": 0}', invalid.strip('"'), 1),
                    encoding="utf-8")
                with self.assertRaisesRegex(ValueError, "precompute payload", msg=f"attempt={attempt}"):
                    EXPORT.export(manifest, records, root / ("output-" + str(attempt)),
                                  "fixture", "0" * 40)
            records.write_text(original, encoding="utf-8")

    def test_replay_is_byte_identical_except_for_output_location(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.run_export(root / "input", root / "one")
            self.run_export(root / "input", root / "two")
            for name in ("synexia.m3lex", "synexia.shards.tsv", "synexia.records.tsv",
                         "synexia.precompute-index.tsv", "synexia.precompute.tsv", "synexia.export.json"):
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

    def test_precompute_profile_drift_is_rejected_before_output(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest, records = self.write_inputs(root / "input")
            changed = records.read_text(encoding="utf-8").replace(
                "M3StringFacts + NumberPrecompute", "WrongOwner", 1)
            records.write_text(changed, encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "precompute profile mismatch"):
                EXPORT.export(manifest, records, root / "output",
                              "https://github.com/hsoliwal/com.synexia",
                              "3e85c872adf556901a341a9eb1c3b59864918da1")
            self.assertFalse((root / "output").exists())

    def test_unpaired_utf16_surrogate_round_trips_through_utf8_sidecars(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "sources.tsv"
            manifest.write_text(
                "\t".join(EXPORT.MANIFEST_COLUMNS) + "\n"
                "surrogate\tSurrogate\tfixture/source\tid\tname\tM3StringFacts + Surrogate\tApache-2.0\tfixture\n",
                encoding="utf-8",
            )
            records = root / "records.tsv"
            records.write_text(
                "\t".join(EXPORT.RECORD_COLUMNS) + "\n"
                "surrogate\tfixture/source\tfixture\tund\tS1\t\ud800\tS1\tSURROGATE\t-\tM3StringFacts + Surrogate\n",
                encoding="utf-8",
                errors="surrogatepass",
            )
            output = root / "output"
            EXPORT.export(manifest, records, output, "fixture", "0" * 40)
            sidecar = (output / "synexia.records.tsv").read_text(encoding="utf-8")
            self.assertIn("\\uD800", sidecar)
            self.assertEqual({"source_records": 1, "image_records": 1, "shards": 1,
                              "precompute_profiles": 1, "utf16_units": 1},
                             VERIFY.verify(output))

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

    def test_java_catalog_opens_actual_python_export(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            output = root / "output"
            self.run_export(root / "input", output, with_payload=True)
            classes = root / "classes"
            sources = [ROOT / "m3/core/src/module-info.java"]
            sources.extend(sorted((ROOT / "m3/core/src/com/m3/text").glob("*.java")))
            sources.append(ROOT / "m3/core/test/FoundationTest.java")
            compile_run = subprocess.run(
                ["javac", "-d", str(classes), *(str(source) for source in sources)],
                cwd=ROOT, text=True, capture_output=True, check=False,
            )
            self.assertEqual(0, compile_run.returncode, compile_run.stderr)
            proof_run = subprocess.run(
                ["java", "-cp", str(classes), "FoundationTest", str(output)],
                cwd=ROOT, text=True, capture_output=True, check=False,
            )
            self.assertEqual(0, proof_run.returncode, proof_run.stderr + proof_run.stdout)
            self.assertIn("FOUNDATION_PASS", proof_run.stdout)

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
