# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Current authority schema regressions, not installed-JDK acceptance.

The stable-ID and refusal intent was reviewed from retained PR1868
test_schema_history.py (Git blob 86d6ef87858fb86588c578abcaf99ad08cdda0a1).
That historical single-ID vocabulary exception is reference-only: the current
authority already normalized it, so these tests do not restore the exception.
Schema validation uses the existing m3/migration/requirements.txt dependency.
"""
from __future__ import annotations

from copy import deepcopy
import hashlib
import json
from pathlib import Path
import unittest

from jsonschema import Draft202012Validator, ValidationError

ROOT = Path(__file__).resolve().parents[3]
MAP = "m3/docs/name-mapping.json"
SCHEMA = "m3/docs/name-mapping.schema.json"
PLAN = "m3/tooling/name-contract/name-plan.json"
SIGNAL = "m3.code-text-signal-batch"
AST = "synexia.counterpart.MIndexCompilerASTPrecompute"
FS = "m3.fs.provider-defaults"
COMPANION = "m3.arrays.api-companion"
QUEUE = "m3.collections.evicting-queue.owner-naming"
# Sealed receiving snapshot 5f1ac46f: future additions are allowed; removal needs explicit review.
HISTORICAL_IDS = frozenset({
    "bridge.MIndexCanonicalBridge",
    "bridge.MIndexDagDeferredSwap",
    "bridge.MIndexDagPage",
    "bridge.MIndexDagPageCodec",
    "bridge.MIndexKnowledgeArgumentCompiler",
    "bridge.MIndexKnowledgeArguments",
    "bridge.MIndexLexiconCoordinateCompilerCli",
    "bridge.MIndexLexiconCoordinateManifest",
    "bridge.MIndexLexiconCoordinatePublisher",
    "bridge.MIndexLexiconCoordinateSpace",
    "bridge.MIndexRuntimeStructuralAdapterProvider",
    "bridge.MIndexStringConversionBridge",
    "family.application-structures",
    "family.entire-source-closure",
    "family.structural-owners",
    "family.substring-conflict",
    "family.word-facts",
    "m3.arrays.api-companion",
    "m3.arrays.packed.native",
    "m3.arrays.packed.signed",
    "m3.arrays.packed.vint64",
    "m3.build.change-journal-doclint",
    "m3.code-text-signal-batch",
    "m3.collections.evicting-queue.owner-naming",
    "m3.collections.indexed-snapshot.packed-filter-directory",
    "m3.fs.provider-defaults",
    "m3.p0-view-dependencies",
    "m3.prefix-z",
    "route-a.canonical-storage",
    "route-b.compiler-lowering",
    "route-c.complete-jdk",
    "synexia.collections.PackedBitAtoms",
    "synexia.collections.SegmentedAddress28",
    "synexia.collections.SegmentedBitLane28",
    "synexia.collections.SegmentedIntLane28",
    "synexia.collections.SegmentedLongLane28",
    "synexia.collections.tree-window-sibling-shift",
    "synexia.counterpart.MIndexAST",
    "synexia.counterpart.MIndexASTPrecompute",
    "synexia.counterpart.MIndexClass",
    "synexia.counterpart.MIndexClassBoundary",
    "synexia.counterpart.MIndexClassFlags",
    "synexia.counterpart.MIndexClassIndex",
    "synexia.counterpart.MIndexCompilerASTPrecompute",
    "synexia.counterpart.MIndexConstructor",
    "synexia.counterpart.MIndexField",
    "synexia.counterpart.MIndexJvmDescriptor",
    "synexia.counterpart.MIndexLibraryRelease",
    "synexia.counterpart.MIndexMember",
    "synexia.counterpart.MIndexMemberFlags",
    "synexia.counterpart.MIndexMemberKind",
    "synexia.counterpart.MIndexMethod",
    "synexia.counterpart.MIndexRecordComponent",
    "synexia.counterpart.MIndexRegexTrigramQuery",
    "synexia.counterpart.MIndexRegexTrigramQuery.sparseLookup",
    "synexia.counterpart.MIndexVersionTable",
    "synexia.counterpart.MIndexVmPrecomputeKey",
    "synexia.counterpart.MIndexVmSession",
    "synexia.frozen-chars",
    "synexia.inventory-cli",
    "synexia.inventory-producer",
    "synexia.jcc-java-jni-regression",
    "synexia.jcc-recipe-laboratory",
    "synexia.regex.hierarchical-route-acceptance",
    "synexia.regex.metadata-pressure",
    "synexia.regex.mr",
    "synexia.regex.ordered-relations.adapter",
    "synexia.regex.ordered-relations.dispatch",
    "synexia.regex.prepared-utf16-effects.receiver",
    "synexia.regex.rxa",
    "synexia.regex.rxm",
})
TEST_DESCRIPTIONS = [
    "test/jdk/java/lang/String/M3CodeTextSignalBatchNativeTest.java",
    "Strict Java21, javac-h, C11/C17, Xcheck:jni and UBSan component parity in canonical packet.",
]


def examine(mapping: dict, schema: dict) -> None:
    """Validate the existing authority and preserve the reviewed history anchors."""
    Draft202012Validator(schema).validate(mapping)
    ids = [row["id"] for row in mapping["migration"]["records"]]
    if len(set(ids)) != len(ids):
        raise ValueError("duplicate stable migration IDs")
    if not HISTORICAL_IDS.issubset(ids):
        raise ValueError("missing historical migration record")


class NamingSchemaCompatibilityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.map_bytes = (ROOT / MAP).read_bytes()
        cls.mapping = json.loads(cls.map_bytes)
        cls.schema = json.loads((ROOT / SCHEMA).read_text(encoding="utf-8"))
        Draft202012Validator.check_schema(cls.schema)
        cls.validator = Draft202012Validator(cls.schema)

    def setUp(self):
        self.doc = deepcopy(self.mapping)

    def record(self, name=SIGNAL):
        return next(row for row in self.doc["migration"]["records"] if row["id"] == name)

    def assert_refused(self):
        with self.assertRaises(ValidationError):
            self.validator.validate(self.doc)

    def test_complete_current_authority_validates(self):
        examine(self.doc, self.schema)

    def test_name_plan_pins_exact_authority_bytes(self):
        plan = json.loads((ROOT / PLAN).read_text(encoding="utf-8"))
        expected = hashlib.sha1(
            b"blob " + str(len(self.map_bytes)).encode("ascii") + b"\0" + self.map_bytes
        ).hexdigest()
        self.assertEqual(MAP, plan["authority"])
        self.assertEqual(expected, plan["authority_blob"])

    def test_duplicate_stable_ids_are_refused(self):
        self.doc["migration"]["records"].append(deepcopy(self.record()))
        with self.assertRaisesRegex(ValueError, "duplicate stable"):
            examine(self.doc, self.schema)

    def test_missing_historical_record_is_refused(self):
        for identity in sorted(HISTORICAL_IDS):
            with self.subTest(identity=identity):
                self.doc = deepcopy(self.mapping)
                self.doc["migration"]["records"] = [
                    row for row in self.doc["migration"]["records"] if row["id"] != identity
                ]
                with self.assertRaisesRegex(ValueError, "missing historical"):
                    examine(self.doc, self.schema)

    def test_historical_vocabulary_is_not_a_new_general_admission(self):
        mutations = [
            ("kind", "internal-relocation-proposal"),
            ("status", "proposed"),
            ("source_state", "implemented-proving-module-unpromoted"),
            ("source_state", "target-owned"),
        ]
        for name in (QUEUE, SIGNAL):
            for field, value in mutations:
                with self.subTest(record=name, field=field, value=value):
                    self.doc = deepcopy(self.mapping)
                    self.record(name)[field] = value
                    self.assert_refused()
            for value in ("same-repository internal name review", "canonical-recipe-to-target"):
                with self.subTest(record=name, direction=value):
                    self.doc = deepcopy(self.mapping)
                    self.record(name)["sync"]["direction"] = value
                    self.assert_refused()
        self.doc = deepcopy(self.mapping)
        self.record()["dependencies"][0]["resolution"] = "existing-owner"
        self.assert_refused()

    def test_schema_repair_does_not_promote_status_or_receiving_phase(self):
        self.assertEqual("implemented-unverified", self.record()["status"])
        self.assertEqual("implemented-unverified", self.record(COMPANION)["status"])
        self.assertEqual("pending", self.record(AST)["status"])
        self.assertEqual("pending", self.record(QUEUE)["status"])
        self.assertEqual("implemented-tested", self.record(FS)["status"])
        sequence = self.doc["porting_invariant"]["receiving_sequence"]
        self.assertEqual("STRING", sequence["active_phase"])
        self.assertEqual(
            ["STRING", "ARRAYS", "COLLECTIONS", "AST_COMPILER", "REMAINING_FAMILIES"],
            sequence["order"],
        )
        for flag in ("automatic_application", "family_completion", "repository_completion"):
            self.assertIs(sequence[flag], False)

    def test_historical_test_descriptions_remain_verbatim_without_receipt_claims(self):
        record = self.record()
        self.assertEqual(TEST_DESCRIPTIONS, record["test_descriptions"])
        self.assertEqual([], record["tests"])
        self.assertTrue(any("no original canonical executed receipt was located" in text
                            for text in record["sync"]["pending"]))

    def test_test_descriptions_do_not_satisfy_receipt_shape(self):
        self.record()["tests"] = list(TEST_DESCRIPTIONS)
        self.assert_refused()

    def test_test_descriptions_are_typed_nonblank_strings(self):
        for invalid in ("free-form list replacement", [None], [{}], [""], ["   "]):
            with self.subTest(value=invalid):
                self.record()["test_descriptions"] = invalid
                self.assert_refused()

    def test_fingerprint_stays_required_and_unknown_is_explicit_null(self):
        for side in ("sources", "targets"):
            with self.subTest(side=side):
                self.doc = deepcopy(self.mapping)
                self.assertIs(self.record()[side][0]["fingerprint"], None)
                del self.record()[side][0]["fingerprint"]
                self.assert_refused()

    def test_canonical_recipe_locator_requires_both_fields(self):
        for missing in ("canonical_repository", "canonical_path"):
            with self.subTest(missing=missing):
                self.doc = deepcopy(self.mapping)
                del self.record()["recipe"][missing]
                self.assert_refused()

    def test_canonical_locator_cannot_claim_target_local_recipe_or_digest(self):
        for field, value in (("path", "m3/local-recipe.yml"), ("sha256", "a" * 64)):
            with self.subTest(field=field):
                self.doc = deepcopy(self.mapping)
                self.record()["recipe"][field] = value
                self.assert_refused()

    def test_canonical_locator_stays_in_the_existing_custody_repository(self):
        self.record()["recipe"]["canonical_repository"] = "someone/other-repository"
        self.assert_refused()

    def test_canonical_path_is_relative_and_cannot_escape(self):
        for path in ("", "/absolute", "../outside", "a/../outside", "a/./b",
                     "a//b", "a/b/", "a\\b", "https://example.invalid/recipe", "a\nb", "a\n", "a\r", "a\t", "a\x00"):
            with self.subTest(path=path):
                self.record()["recipe"]["canonical_path"] = path
                self.assert_refused()

    def test_legacy_recipe_without_canonical_locator_remains_supported(self):
        recipe = self.record(FS)["recipe"]
        del recipe["canonical_repository"]
        del recipe["canonical_path"]
        examine(self.doc, self.schema)

    def test_manifest_assertion_is_not_relabeled_as_recipe_directory_digest(self):
        record = self.record()
        manifest = self.record(AST)["receiving_observations"][0]["java_manifest_sha256"]
        self.assertIs(record["recipe"]["sha256"], None)
        self.assertIn(manifest, record["observation"])
        self.assertIn("java_manifest_sha256", record["observation"])
        self.assertIn("not a verified directory or recipe-file digest", record["observation"])
        for name in (SIGNAL, COMPANION):
            recipe = self.record(name)["recipe"]
            self.assertIs(recipe["path"], None)
            self.assertEqual("hsoliwal/com.synexia", recipe["canonical_repository"])

    def test_visibility_is_explicit_java_visibility(self):
        target = self.record(FS)["targets"][0]
        for visibility in ("public", "protected", "package-private", "private"):
            with self.subTest(visibility=visibility):
                target["visibility"] = visibility
                examine(self.doc, self.schema)
        for visibility in ("exported", "internal", True, None):
            with self.subTest(visibility=visibility):
                target["visibility"] = visibility
                self.assert_refused()

    def test_receiving_observation_is_closed_and_does_not_admit_runtime_status(self):
        for field, value in (
                ("unknown_approval", True),
                ("state", "INSTALLED_JDK_ACCEPTED"),
                ("columns", 0),
                ("columns", True),
                ("columns", "24"),
                ("target_base", "master"),
                ("java_manifest_sha256", "not-a-hash"),
                ("date", "2026-13-99"),
                ("date", "2026-10-22\n"),
                ("target_base", "a" * 40 + "\n"),
                ("java_manifest_sha256", "a" * 64 + "\n"),
                ("relations", "CALL"),
                ("remaining_gates", []),
                ("context_rule", " ")):
            with self.subTest(field=field, value=value):
                self.doc = deepcopy(self.mapping)
                self.record(AST)["receiving_observations"][0][field] = value
                self.assert_refused()

    def test_receiving_observation_cannot_drop_context_or_pending_gates(self):
        for field in ("target_base", "context_rule", "contract_rule", "remaining_gates"):
            with self.subTest(missing=field):
                self.doc = deepcopy(self.mapping)
                del self.record(AST)["receiving_observations"][0][field]
                self.assert_refused()

    def test_donor_boundary_is_a_nonblank_description(self):
        for value in ("", "   ", None, [], {}):
            with self.subTest(value=value):
                self.record()["provenance"]["donor_boundary"] = value
                self.assert_refused()

    def test_unknown_nested_properties_are_still_refused(self):
        for boundary in ("migration", "record", "artifact", "recipe", "provenance", "test"):
            with self.subTest(boundary=boundary):
                self.doc = deepcopy(self.mapping)
                objects = {
                    "migration": self.doc["migration"],
                    "record": self.record(),
                    "artifact": self.record()["targets"][0],
                    "recipe": self.record()["recipe"],
                    "provenance": self.record()["provenance"],
                    "test": self.record(COMPANION)["tests"][0],
                }
                objects[boundary]["unknown_runtime_admission"] = True
                self.assert_refused()

    def test_legacy_top_level_extensions_are_preserved(self):
        self.doc["existing_unrelated_extension"] = {"retained": True}
        examine(self.doc, self.schema)


if __name__ == "__main__":
    unittest.main()
