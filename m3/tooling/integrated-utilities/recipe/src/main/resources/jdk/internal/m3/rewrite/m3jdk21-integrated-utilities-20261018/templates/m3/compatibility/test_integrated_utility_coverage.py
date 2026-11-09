#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Qualify source inventory admission/refusal against actual pinned repository blobs."""
from __future__ import annotations
import argparse
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("utility_coverage", HERE / "check_integrated_utility_coverage.py")
assert spec is not None and spec.loader is not None
coverage = importlib.util.module_from_spec(spec)
spec.loader.exec_module(coverage)
REPOSITORY = Path(".")
OVERLAY = None


class SourceCoverageTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory(prefix="m3-utility-coverage-")
        self.addCleanup(self.temporary.cleanup)
        self.overlay = Path(self.temporary.name)
        self.mapping = json.loads(coverage.load_candidate(REPOSITORY, OVERLAY, coverage.MAPPING))
        self.fragment = json.loads(coverage.load_candidate(REPOSITORY, OVERLAY, coverage.FRAGMENT))
        self.catalogue = coverage.load_candidate(REPOSITORY, OVERLAY, self.fragment["existing_catalogue"])
        self.module_index = json.loads(coverage.load_candidate(REPOSITORY, OVERLAY, self.fragment["module_wide_index"]))
        self.save()

    def save(self) -> None:
        for path, content in ((coverage.MAPPING, json.dumps(self.mapping).encode()),
                              (coverage.FRAGMENT, json.dumps(self.fragment).encode()),
                              (self.fragment["existing_catalogue"], self.catalogue),
                              (self.fragment["module_wide_index"], json.dumps(self.module_index).encode())):
            file = self.overlay / path
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_bytes(content)

    def refused(self, expected: str) -> None:
        self.save()
        with self.assertRaisesRegex(ValueError, expected):
            coverage.verify(REPOSITORY, self.overlay)

    def test_exact_baseline_inventory_is_source_only(self) -> None:
        report = coverage.verify(REPOSITORY, self.overlay)
        self.assertEqual(31, report["families"])
        self.assertEqual(111, report["public_api_sources"])
        self.assertEqual(4, report["generated_api_templates"])
        self.assertEqual(3, len(report["internal_candidate_observations"]))
        self.assertEqual(70, report["module_wide_coverage"]["source_modules"])
        self.assertEqual(["jdk.crypto.mscapi"], report["module_wide_coverage"]["platform_specific_modules"])
        for key in ("runtime_qualified", "performance_qualified", "receiving_sequence_advanced"):
            self.assertIs(False, report[key])

    def test_existing_map_state_cannot_change(self) -> None:
        self.mapping["unauthorized_receiving_phase"] = "PROMOTED"
        self.refused("existing naming-map values or receiving-plan states changed")

    def test_prior_family_mapping_cannot_change(self) -> None:
        self.mapping["family_name_mapping"][0]["unauthorized_change"] = True
        self.refused("existing family mappings changed")

    def test_missing_family_is_refused(self) -> None:
        self.fragment["families"].pop()
        self.fragment["required_family_ids"].pop()
        self.refused("requested utility family coverage")

    def test_source_sha_drift_is_refused(self) -> None:
        self.fragment["public_api_sources"][0]["sha256"] = "0" * 64
        self.refused("SHA-256 drift")

    def test_source_git_blob_drift_is_refused(self) -> None:
        self.fragment["public_api_sources"][0]["git_blob"] = "0" * 40
        self.refused("Git blob drift")

    def test_nonpublic_type_cannot_be_counted_as_public(self) -> None:
        self.fragment["public_api_sources"][0]["visibility"] = "PACKAGE_PRIVATE"
        self.refused("nonpublic source mislabeled as public API")

    def test_candidate_runtime_admission_cannot_be_fabricated(self) -> None:
        candidate = next(f["new_candidates"][0] for f in self.fragment["families"] if f["new_candidates"])
        candidate["status"] = "RUNTIME_QUALIFIED"
        self.refused("candidate status falsely claims source/runtime admission")

    def test_private_receiver_cannot_become_public_companion(self) -> None:
        self.fragment["internal_receiving_candidates"][0]["visibility"] = "PUBLIC"
        self.refused("internal candidate became a public companion API")

    def test_jni_symbol_drift_is_refused(self) -> None:
        self.fragment["internal_receiving_candidates"][-1]["jni_symbol"] = "Java_com_m3_Masks_selected0"
        self.refused("JNI owner drift")

    def test_inventory_cannot_promote_receiving_phase(self) -> None:
        self.fragment["receiving_sequence_advanced"] = True
        self.refused("source inventory cannot claim receiving_sequence_advanced")

    def test_donor_branch_url_is_not_an_immutable_blob(self) -> None:
        self.catalogue = self.catalogue.replace(b"https://api.github.com/repos/", b"https://github.com/", 1)
        self.refused("donor catalogue source is not pinned to a Git blob")

    def test_source_paths_cannot_escape_the_repository(self) -> None:
        for value in ("../owner.java", "/tmp/owner.java", "src/../owner.java", "src\\owner.java", ".git/config"):
            with self.subTest(value=value), self.assertRaises(ValueError):
                coverage.relative(value)

    def test_generated_api_requires_its_actual_generator(self) -> None:
        row = next(row for row in self.fragment["public_api_sources"] if row["source_kind"] == "GENERATED_TYPE_TEMPLATE")
        row["generation_owner"] = "make/missing-generator.gmk"
        self.refused("generated API lacks template/generator source")


    def test_missing_platform_module_cannot_claim_full_source_set(self) -> None:
        row = next(row for row in self.module_index["modules"] if row["module"] == "jdk.crypto.mscapi")
        self.module_index["modules"].remove(row)
        self.module_index["module_source_paths"].remove(row["path"])
        self.module_index["source_module_count"] -= 1
        self.refused("module index does not cover every pinned source module descriptor")

    def test_tool_module_cannot_claim_an_unexported_public_package(self) -> None:
        row = next(row for row in self.module_index["modules"] if row["module"] == "jdk.jlink")
        row["unqualified_exports"].append("jdk.tools.jlink")
        self.refused("module declaration/export/provider drift")

    def test_module_index_cannot_invent_public_facade(self) -> None:
        self.module_index["new_public_utility_candidates"] = ["com.m3.JdkUtils"]
        self.refused("module inventory must not invent utility facades")

    def test_conditional_platform_owner_cannot_be_mislabeled_shared(self) -> None:
        row = next(row for row in self.module_index["modules"] if row["module"] == "jdk.crypto.mscapi")
        row["source_platform"] = "share"
        self.refused("module platform ownership drift")

    def test_internal_module_cannot_be_mislabeled_as_public_utility(self) -> None:
        row = next(row for row in self.module_index["modules"] if row["module"] == "jdk.internal.opt")
        row["owner_disposition"] = "EXPORTED_API_MODULE_KEEP_EXISTING_PUBLIC_OWNERS"
        self.refused("module owner disposition drift")

    def test_unsupported_export_is_not_stable_public_api_acceptance(self) -> None:
        row = next(row for row in self.module_index["additional_public_api_sources"] if row["module"] == "jdk.unsupported")
        row["support_disposition"] = "EXISTING_EXPORTED_API_OWNER"
        self.refused("preview/incubator/unsupported API status drift")

    def test_module_row_cannot_claim_completion(self) -> None:
        self.module_index["modules"][0]["module_completion"] = True
        self.refused("module row falsely claims completion")

    def test_extended_public_owner_source_seal_is_enforced(self) -> None:
        self.module_index["additional_public_api_sources"][0]["sha256"] = "0" * 64
        self.refused("module index source seal drift")

    def test_public_tool_service_owner_cannot_be_dropped(self) -> None:
        row = next(row for row in self.module_index["modules"] if row["module"] == "jdk.jpackage")
        row["standard_entry_point_refs"].remove("java.util.spi.ToolProvider")
        self.refused("declared public provider service owner is missing")

    def token_extension(self) -> dict:
        return next(row for row in self.fragment["families"] if row["id"] == "regex")["token_sequence_extension"]

    def test_token_extension_cannot_fabricate_public_owner(self) -> None:
        self.token_extension()["public_owners"][0] = "java.util.regex.UnreviewedTokens"
        self.refused("token sequence public owner drift")

    def test_token_inventory_cannot_claim_runtime_qualification(self) -> None:
        self.token_extension()["runtime_qualified"] = True
        self.refused("token sequence inventory cannot claim runtime or performance qualification")

    def test_named_character_groups_are_not_token_matching(self) -> None:
        self.token_extension()["named_character_groups_are_token_aware"] = True
        self.refused("character regex must remain distinct from token sequence matching")

    def test_serialized_characters_cannot_be_labeled_token_units(self) -> None:
        self.token_extension()["unit"] = "STRING_DELIMITER_ENCODING"
        self.refused("token sequence unit or span contract drift")

    def test_token_candidate_needs_a_source_seal(self) -> None:
        regex = next(row for row in self.fragment["families"] if row["id"] == "regex")
        regex["new_candidates"][0]["candidate_source_sha256"] = ""
        self.refused("token candidate lacks exact source and declaration seals")

    def test_observed_candidate_hash_drift_is_not_admitted(self) -> None:
        path = "src/java.base/share/classes/example/Utility.java"
        source = self.overlay / path
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_text("package example;\npublic final class Utility { public static int value() { return 1; } }\n")
        candidate = {"target_path": path, "public_api": "example.Utility", "proposed_methods": [],
                     "candidate_source_sha256": "0" * 64, "declaration_tokens": ["public final class Utility"]}
        report = coverage.candidate_source(REPOSITORY, self.overlay, candidate)
        self.assertTrue(report["public_declaration"])
        self.assertFalse(report["source_hash_matches"])
        self.assertFalse(report["source_observed"])

    def test_missing_nested_public_declaration_is_not_admitted(self) -> None:
        path = "src/java.base/share/classes/example/Utility.java"
        source = self.overlay / path
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_text("package example;\npublic final class Utility {}\n")
        candidate = {"target_path": path, "public_api": "example.Utility", "proposed_methods": [],
                     "declaration_tokens": ["public enum Quantifier"]}
        report = coverage.candidate_source(REPOSITORY, self.overlay, candidate)
        self.assertTrue(report["public_declaration"])
        self.assertFalse(report["source_observed"])

    def test_extra_aspect_cannot_reference_invented_utility(self) -> None:
        self.module_index["additional_aspect_ownership"][0]["owner_source_refs"].append("java.awt.DesktopUtils")
        self.refused("unknown additional aspect public owner")

    def test_real_record_and_legacy_modifier_forms_are_recognized(self) -> None:
        self.assertTrue(coverage.public_declaration("public record Principal(String user) {}", "example.Principal"))
        self.assertTrue(coverage.public_declaration("final public class Utility {}", "example.Utility"))
        self.assertFalse(coverage.public_declaration("final class Utility {}", "example.Utility"))


def main() -> int:
    global REPOSITORY, OVERLAY
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, default=Path("."))
    parser.add_argument("--candidate-root", type=Path)
    args = parser.parse_args()
    REPOSITORY = args.repository.resolve()
    OVERLAY = args.candidate_root.resolve() if args.candidate_root else None
    result = unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(SourceCoverageTest))
    return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    raise SystemExit(main())
