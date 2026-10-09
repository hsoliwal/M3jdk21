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
                     with_payload: bool = False,
                     with_requirements: bool = False) -> tuple[Path, Path]:
        root.mkdir(parents=True, exist_ok=True)
        manifest = root / "sources.tsv"
        manifest_columns = (EXPORT.MANIFEST_COLUMNS_V2 if with_requirements
                            else EXPORT.MANIFEST_COLUMNS)
        number_requirement = "frequency_rank" if with_requirements else "-"
        translation_requirement = (
            "code_point_length,concept_ids,corpus_count,document_frequency,expansion_word_ids,"
            "first_code_point,last_code_point,lemma_id,lexical_rank,"
            "memberships,morphology_mask,phonetic_id,pos_mask,presence64,script_ordinal,"
            "sim_hash64,stem_id,subjects,utf16_length")
        number_row = ("numbers", "Numbers", "synexia-dictlang/src/main/java/com/synexia/dictlang/NumberLexicon.java",
                      "number", "value,spelling", "M3StringFacts + NumberPrecompute", "Apache-2.0", "fixture")
        translation_row = ("translations", "Translations", "synexia-translate/src/main/java/com/synexia/translate/Language.java",
                           "translation_id", "language_tag,source_id,target_id", "M3StringFacts + TranslationMapping",
                           "Apache-2.0", "fixture")
        if with_requirements:
            number_row += (number_requirement,)
            translation_row += (translation_requirement,)
        manifest.write_text(
            "\t".join(manifest_columns) + "\n"
            + "\t".join(number_row) + "\n"
            + "\t".join(translation_row) + "\n",
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
        manifest, records = self.write_inputs(root, conflict, with_payload,
                                              with_requirements=with_payload)
        return EXPORT.export(manifest, records, output, "https://github.com/hsoliwal/com.synexia",
                             "3e85c872adf556901a341a9eb1c3b59864918da1",
                             ROOT / "m3/lexicon/synexia-precompute-field-map.tsv")

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
            export_manifest = json.loads((root / "first/synexia.export.json").read_text(encoding="utf-8"))
            self.assertEqual(["frequency_rank"],
                             export_manifest["source"]["precompute_fields"]["numbers"])
            self.assertIn("concept_ids", export_manifest["source"]["precompute_fields"]["translations"])
            self.assertEqual("long[]", export_manifest["source"]["precompute_field_types"]["concept_ids"])
            self.assertRegex(export_manifest["source"]["precompute_field_map_sha256"], r"^[0-9a-f]{64}$")
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

    def test_manifest_requirements_reject_missing_owner_field(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest, records = self.write_inputs(root / "input", with_payload=True,
                                                  with_requirements=True)
            original = records.read_text(encoding="utf-8")
            records.write_text(original.replace('"frequency_rank": 0', '"other_rank": 0', 1),
                                encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "missing required fields"):
                EXPORT.export(manifest, records, root / "output", "fixture", "0" * 40)

    def test_admitted_field_map_rejects_wrong_owner_shape(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest, records = self.write_inputs(root / "input", with_payload=True,
                                                  with_requirements=True)
            original = records.read_text(encoding="utf-8")
            records.write_text(original.replace('"frequency_rank": 0', '"frequency_rank": "0"', 1),
                                encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "field frequency_rank is not int"):
                EXPORT.export(manifest, records, root / "output", "fixture", "0" * 40,
                              ROOT / "m3/lexicon/synexia-precompute-field-map.tsv")

    def test_reviewed_source_manifest_has_explicit_owner_field_coverage(self):
        sources, _ = EXPORT.read_manifest(ROOT / "m3/lexicon/synexia-source-manifest.tsv")
        self.assertEqual(10, len(sources))
        self.assertEqual(12, len(sources["dictlang.dictionary"]["precompute_fields"].split(",")))
        self.assertEqual(15, len(sources["dictlang.frequency"]["precompute_fields"].split(",")))
        self.assertEqual(10, len(sources["unicodex.langdex.lexemes"]["precompute_fields"].split(",")))
        self.assertEqual(4, len(sources["dictlang.si-units"]["precompute_fields"].split(",")))
        self.assertEqual("", sources["translate.rows"]["precompute_fields"])
        self.assertEqual("", sources["dictlang.numbers.0-10000"]["precompute_fields"])

    def test_source_requirements_are_backed_by_admitted_field_map(self):
        sources, _ = EXPORT.read_manifest(ROOT / "m3/lexicon/synexia-source-manifest.tsv")
        with (ROOT / "m3/lexicon/synexia-precompute-field-map.tsv").open(
                encoding="utf-8", newline="") as stream:
            rows = list(csv.DictReader(stream, delimiter="\t"))
        mapped = {row["canonical_payload_field"] for row in rows if row["status"] == "MAPPED"}
        allowed_types = {"boolean", "double", "int", "long", "int[]", "long[]"}
        field_types: dict[str, str] = {}
        for row in rows:
            self.assertIn(row["donor_java_type"], allowed_types)
            previous = field_types.setdefault(row["canonical_payload_field"], row["donor_java_type"])
            self.assertEqual(previous, row["donor_java_type"], row["canonical_payload_field"])
        self.assertEqual("long[]", field_types["concept_ids"])
        self.assertEqual("int[]", field_types["memberships"])
        self.assertEqual("long", field_types["lexicon_fingerprint"])
        self.assertEqual("double", field_types["si_offset"])
        self.assertEqual("boolean", field_types["si_prefixable"])
        self.assertTrue(all(field_types.values()))
        for source_id in ("dictlang.dictionary", "dictlang.frequency",
                          "dictlang.thesaurus", "dictlang.antonyms"):
            required = set(sources[source_id]["precompute_fields"].split(","))
            self.assertTrue(required.issubset(mapped), source_id)

    def test_langdex_owner_payload_round_trips_with_admitted_shapes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source_id = "unicodex.langdex.lexemes"
            source_path = "synexia-unicodex/docs/langdex/UPSTREAM_SCHEMA.tsv"
            sources, _ = EXPORT.read_manifest(ROOT / "m3/lexicon/synexia-source-manifest.tsv")
            requirement = sources[source_id]["precompute_fields"]
            manifest = root / "sources.tsv"
            manifest.write_text(
                "\t".join(EXPORT.MANIFEST_COLUMNS_V2) + "\n"
                + "\t".join((source_id, "LangDex lexemes", source_path, "lexeme_id",
                              "glottocode,lemma,source,concept_id",
                              "M3StringFacts + LangDexCoordinate", "CC-BY-SA-3.0",
                              "fixture", requirement)) + "\n",
                encoding="utf-8",
            )
            payload = {
                "langdex_concept_id": 42,
                "langdex_confidence_permille": 950,
                "langdex_evidence_mask": 3,
                "langdex_feature_bits": 7,
                "langdex_flags": 1,
                "langdex_frequency": 9,
                "langdex_lexical_class_mask": 1,
                "langdex_semantic_class_mask": 2,
                "langdex_subject_id": 4,
                "langdex_target_lexeme_id": 77,
            }
            records = root / "records.tsv"
            records.write_text(
                "\t".join(EXPORT.RECORD_COLUMNS_V2) + "\n"
                + "\t".join((source_id, source_path, "lexeme", "x-glotto-abcd1234",
                              "17", "bonjour", "concept:42", "BONJOUR", "-",
                              "M3StringFacts + LangDexCoordinate",
                              json.dumps(payload, separators=(",", ":")))) + "\n",
                encoding="utf-8",
            )
            output = root / "output"
            EXPORT.export(manifest, records, output, "fixture", "3e85c872adf556901a341a9eb1c3b59864918da1",
                          ROOT / "m3/lexicon/synexia-precompute-field-map.tsv")
            mapping = list(csv.DictReader(
                (output / "synexia.records.tsv").read_text(encoding="utf-8").splitlines(),
                delimiter="\t"))[0]
            self.assertEqual(payload, json.loads(mapping["precompute_payload"]))
            export_manifest = json.loads((output / "synexia.export.json").read_text(encoding="utf-8"))
            self.assertEqual(10, len(export_manifest["source"]["precompute_fields"][source_id]))
            self.assertEqual("long", export_manifest["source"]["precompute_field_types"]["langdex_concept_id"])
            self.assertEqual("int", export_manifest["source"]["precompute_field_types"]["langdex_flags"])
            VERIFY.verify(output)

    def test_si_unit_owner_payload_round_trips_scalar_java_types(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source_id = "dictlang.si-units"
            source_path = "synexia-dictlang/shared/api/src/main/java/com/synexia/dictshared/api/SiUnit.java"
            sources, _ = EXPORT.read_manifest(ROOT / "m3/lexicon/synexia-source-manifest.tsv")
            requirement = sources[source_id]["precompute_fields"]
            manifest = root / "sources.tsv"
            manifest.write_text(
                "\t".join(EXPORT.MANIFEST_COLUMNS_V2) + "\n"
                + "\t".join((source_id, "SI units", source_path, "unit_id",
                              "canonical_name,symbol", "M3StringFacts + SiUnitPrecompute",
                              "Apache-2.0", "fixture", requirement)) + "\n",
                encoding="utf-8",
            )
            payload = {
                "si_decimal_exponent": -3,
                "si_dimension_packed": 281474976710656,
                "si_offset": 273.15,
                "si_prefixable": True,
            }
            records = root / "records.tsv"
            records.write_text(
                "\t".join(EXPORT.RECORD_COLUMNS_V2) + "\n"
                + "\t".join((source_id, source_path, "si-unit", "und", "metre", "m",
                              "si:metre", "METRE", "-", "M3StringFacts + SiUnitPrecompute",
                              json.dumps(payload, separators=(",", ":")))) + "\n",
                encoding="utf-8",
            )
            output = root / "output"
            EXPORT.export(manifest, records, output, "fixture", "3e85c872adf556901a341a9eb1c3b59864918da1",
                          ROOT / "m3/lexicon/synexia-precompute-field-map.tsv")
            mapping = list(csv.DictReader(
                (output / "synexia.records.tsv").read_text(encoding="utf-8").splitlines(),
                delimiter="\t"))[0]
            self.assertEqual(payload, json.loads(mapping["precompute_payload"]))
            export_manifest = json.loads((output / "synexia.export.json").read_text(encoding="utf-8"))
            self.assertEqual("double", export_manifest["source"]["precompute_field_types"]["si_offset"])
            self.assertEqual("boolean", export_manifest["source"]["precompute_field_types"]["si_prefixable"])
            VERIFY.verify(output)
            records.write_text(
                records.read_text(encoding="utf-8").replace(
                    '"si_prefixable":true', '"si_prefixable":1', 1),
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "field si_prefixable is not boolean"):
                EXPORT.export(manifest, records, root / "bad-output", "fixture",
                              "3e85c872adf556901a341a9eb1c3b59864918da1",
                              ROOT / "m3/lexicon/synexia-precompute-field-map.tsv")

    def test_directed_related_lexeme_families_round_trip_and_require_input(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / "sources.tsv"
            source_rows = (
                ("dictlang.antonyms", "Antonyms",
                 "synexia-dictlang/src/main/java/com/synexia/dictlang/AntonymLexicon.java",
                 "antonym"),
                ("dictlang.thesaurus", "Thesaurus",
                 "synexia-dictlang/src/main/java/com/synexia/dictlang/ThesaurusLexicon.java",
                 "thesaurus"),
            )
            manifest.write_text(
                "\t".join(EXPORT.MANIFEST_COLUMNS) + "\n"
                + "".join(
                    "\t".join((source_id, name, source_path, "record_id",
                               "lexeme,related_lexeme",
                               "M3StringFacts + IndexWordFacts", "Apache-2.0",
                               "fixture")) + "\n"
                    for source_id, name, source_path, _ in source_rows
                ),
                encoding="utf-8",
            )
            records = root / "records.tsv"
            records.write_text(
                "\t".join(EXPORT.RECORD_COLUMNS) + "\n"
                + "".join(
                    "\t".join((source_id, source_path, kind, "und", "cold", "cold",
                               f"{kind}:cold", "COLD", "-", "M3StringFacts + IndexWordFacts")) + "\n"
                    for source_id, _, source_path, kind in source_rows
                ),
                encoding="utf-8",
            )
            relations = root / "relations.tsv"
            relations.write_text(
                "\t".join(EXPORT.RELATION_COLUMNS) + "\n"
                + "\n".join((
                    "dictlang.antonyms\tcold\tcold\thot",
                    "dictlang.antonyms\tcold\tcold\twarm",
                    "dictlang.thesaurus\tcold\tcold\tchilly",
                )) + "\n",
                encoding="utf-8",
            )
            output = root / "output"
            result = EXPORT.export(
                manifest, records, output, "fixture", "0" * 40,
                relations_path=relations,
            )
            self.assertEqual(3, result["counts"]["relation_records"])
            self.assertEqual(2, result["counts"]["source_records"])
            self.assertEqual(1, result["counts"]["image_records"])
            self.assertEqual(
                [
                    "dictlang.antonyms\tcold\tcold\thot\t0\t0",
                    "dictlang.antonyms\tcold\tcold\twarm\t0\t0",
                    "dictlang.thesaurus\tcold\tcold\tchilly\t0\t0",
                ],
                (output / "synexia.related.tsv").read_text(
                    encoding="utf-8"
                ).splitlines()[1:],
            )
            export_manifest = json.loads(
                (output / "synexia.export.json").read_text(encoding="utf-8")
            )
            self.assertEqual(
                ["dictlang.antonyms", "dictlang.thesaurus"],
                export_manifest["source"]["relation_sources"],
            )
            self.assertEqual(
                "normalized-directed-v1",
                export_manifest["source"]["relation_policy"],
            )
            self.assertEqual("synexia.related.tsv",
                             export_manifest["target"]["relation_sidecar"])
            self.assertEqual("synexia.related-sources.tsv",
                             export_manifest["target"]["relation_sources_sidecar"])
            self.assertEqual(
                "source_id\n"
                "dictlang.antonyms\n"
                "dictlang.thesaurus\n",
                (output / "synexia.related-sources.tsv").read_text(encoding="utf-8"),
            )
            self.assertEqual(3, VERIFY.verify(output)["relation_records"])
            with self.assertRaisesRegex(ValueError, "related-lexeme input is required"):
                EXPORT.export(
                    manifest, records, root / "missing-output",
                    "fixture", "0" * 40,
                )

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
