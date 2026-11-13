# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Target receiver fixtures; declaration and queue checks are not JDK acceptance."""
from __future__ import annotations

import importlib.util
import copy
import contextlib
import hashlib
import io
import json
from pathlib import Path
import runpy
import sys
import tempfile
import unittest
from unittest import mock

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("m3_name_receiver", HERE / "check.py")
GATE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(GATE)


def write(root: Path, rel: str, text: str) -> None:
    file = root / rel
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_text(text, encoding="utf-8")


class NameContractReceiverTests(unittest.TestCase):
    def setUp(self):
        self.workdir = tempfile.TemporaryDirectory()
        self.root = Path(self.workdir.name)
        write(self.root, GATE.DOC, "Naming contract - fixture only\n")
        write(self.root, "AGENTS.md", "Read " + GATE.DOC + " before implementation.\n")
        self.mapping = {"schema": 1, "migration": {"records": [{
            "id": "class.view", "status": "implemented-unverified", "targets": [{
                "symbol": "jdk.internal.mindex.M3Class",
                "path": "src/java.base/share/classes/jdk/internal/mindex/M3Class.java",
            }]
        }]}}
        write(self.root, "src/java.base/share/classes/jdk/internal/mindex/M3Class.java",
              "package jdk.internal.mindex; public final class M3Class {}\n")
        write(self.root, "src/java.base/share/classes/java/lang/Class.java",
              "package java.lang; public final class Class {}\n")
        self.plan = {"schema": GATE.SCHEMA, "authority": GATE.MAP,
                     "authority_blob": None, "operations": [{
                         "mapping_id": "class.view", "kind": "JDK_BACKEND",
                         "state": "QUEUED", "before_type": "jdk.internal.mindex.M3Class",
                         "after_type": "java.lang.Class",
                         "reason": "Actual java.lang.Class differential and runtime acceptance is pending.",
                     }]}
        self.save()

    def tearDown(self):
        self.workdir.cleanup()

    def save(self):
        write(self.root, GATE.MAP, json.dumps(self.mapping))
        self.plan["authority_blob"] = GATE.git_blob((self.root / GATE.MAP).read_bytes())
        write(self.root, GATE.PLAN, json.dumps(self.plan))

    def test_existing_canonical_target_queue(self):
        count, ids = GATE.validate(self.root)
        self.assertEqual(count, 1)
        self.assertEqual(ids, ["class.view"])

    def test_map_drift_refused(self):
        write(self.root, GATE.MAP, "{}")
        with self.assertRaisesRegex(ValueError, "map drift"):
            GATE.validate(self.root)

    def test_unknown_mapping_refused(self):
        self.plan["operations"][0]["mapping_id"] = "invented.type"
        self.save()
        with self.assertRaisesRegex(ValueError, "unknown capability"):
            GATE.validate(self.root)

    def test_missing_class_refused(self):
        (self.root / self.mapping["migration"]["records"][0]["targets"][0]["path"]).unlink()
        with self.assertRaisesRegex(ValueError, "mapped source file missing"):
            GATE.validate(self.root)

    def test_package_mismatch_refused(self):
        write(self.root, "src/java.base/share/classes/jdk/internal/mindex/M3Class.java",
              "package another.package; public class M3Class {}")
        with self.assertRaisesRegex(ValueError, "declared package"):
            GATE.validate(self.root)

    def test_package_in_comment_does_not_establish_source_owner(self):
        write(self.root, self.mapping["migration"]["records"][0]["targets"][0]["path"],
              "/*\npackage jdk.internal.mindex;\n*/\n"
              "package another.owner; public class M3Class {}\n")
        with self.assertRaisesRegex(ValueError, "declared package"):
            GATE.validate(self.root)

    def test_type_in_comment_string_or_nested_class_is_not_top_level_owner(self):
        for content in (
                "package jdk.internal.mindex; /* class M3Class {} */ class Other {}\n",
                'package jdk.internal.mindex; class Other { String label = "class M3Class"; }\n',
                'package jdk.internal.mindex; class Other { String label = """\n'
                'class M3Class {}\n"""; }\n',
                "package jdk.internal.mindex; class Other { class M3Class {} }\n",
                "package jdk.internal.mindex; class M3Class {} class M3Class {}\n"):
            with self.subTest(content=content):
                write(self.root, self.mapping["migration"]["records"][0]["targets"][0]["path"],
                      content)
                with self.assertRaisesRegex(ValueError, "declared type"):
                    GATE.validate(self.root)

    def test_package_private_top_level_owner_remains_eligible(self):
        write(self.root, self.mapping["migration"]["records"][0]["targets"][0]["path"],
              "package jdk.internal.mindex; final class M3Class {}\n")
        self.assertEqual(GATE.validate(self.root), (1, ["class.view"]))

    def test_ready_promotion_unproven_refused(self):
        self.plan["operations"][0]["state"] = "READY_FOR_STAGING"
        self.save()
        with self.assertRaisesRegex(ValueError, "unproven active"):
            GATE.validate(self.root)

    def test_public_m3_api_refused(self):
        self.plan["operations"][0]["after_type"] = "java.lang.M3Class"
        self.save()
        with self.assertRaisesRegex(ValueError, "public JDK backend"):
            GATE.validate(self.root)

    def test_duplicate_refused(self):
        self.plan["operations"].append(dict(self.plan["operations"][0]))
        self.save()
        with self.assertRaisesRegex(ValueError, "duplicate naming"):
            GATE.validate(self.root)

    def test_missing_reason_refused(self):
        self.plan["operations"][0]["reason"] = ""
        self.save()
        with self.assertRaisesRegex(ValueError, "concrete open-gate"):
            GATE.validate(self.root)

    def test_format_only_keeps_existing_internal_type(self):
        row = self.plan["operations"][0]
        row["kind"] = "FORMAT_ONLY"
        row["after_type"] = row["before_type"]
        self.save()
        self.assertEqual(GATE.validate(self.root)[0], 1)

    def test_internal_move_remains_candidate(self):
        row = self.plan["operations"][0]
        row["kind"] = "INTERNAL_MOVE"
        row["after_type"] = "jdk.internal.mindex.M3ClassView"
        self.save()
        self.assertEqual(GATE.validate(self.root)[0], 1)

    def assert_cli_refused(self):
        output, error = io.StringIO(), io.StringIO()
        with mock.patch.object(GATE, "ROOT", self.root), \
                contextlib.redirect_stdout(output), contextlib.redirect_stderr(error):
            self.assertEqual(GATE.main(["check.py"]), 2)
        self.assertEqual(output.getvalue(), "")
        self.assertIn("M3JDK21_NAME_RECEIVER_REFUSED:", error.getvalue())

    def test_malformed_document_shapes_have_a_structured_cli_refusal(self):
        original = copy.deepcopy(self.mapping)
        for document in (None, [], "not an object", 1):
            with self.subTest(document=document, file="plan"):
                self.mapping = copy.deepcopy(original)
                self.save()
                write(self.root, GATE.PLAN, json.dumps(document))
                self.assert_cli_refused()
            with self.subTest(document=document, file="map"):
                self.mapping = document
                self.save()
                self.assert_cli_refused()
        for migration in (None, [], "not an object"):
            with self.subTest(migration=migration):
                self.mapping = copy.deepcopy(original)
                self.mapping["migration"] = migration
                self.save()
                self.assert_cli_refused()

    def test_unhashable_queue_fields_have_a_structured_cli_refusal(self):
        original = copy.deepcopy(self.plan["operations"][0])
        for field in ("mapping_id", "kind", "state"):
            for invalid in ([], {}):
                with self.subTest(field=field, value=invalid):
                    self.plan["operations"] = [dict(original, **{field: invalid})]
                    self.save()
                    self.assert_cli_refused()

    def test_target_collection_must_be_an_array(self):
        record = self.mapping["migration"]["records"][0]
        for invalid in (None, {}, "not an array"):
            with self.subTest(targets=invalid):
                record["targets"] = invalid
                self.save()
                self.assert_cli_refused()

    def test_two_distinct_files_cannot_both_own_the_queued_type(self):
        record = self.mapping["migration"]["records"][0]
        duplicate = dict(record["targets"][0],
                         path="m3/alternate/src/jdk/internal/mindex/M3Class.java")
        write(self.root, duplicate["path"],
              "package jdk.internal.mindex; final class M3Class { int otherOwner; }\n")
        record["targets"].append(duplicate)
        self.save()
        with self.assertRaisesRegex(ValueError, "ambiguous canonical target"):
            GATE.validate(self.root)

    def test_unicode_identifier_suffix_does_not_own_the_shorter_queued_type(self):
        path = self.mapping["migration"]["records"][0]["targets"][0]["path"]
        # Java permits currency symbols, combining marks and connector punctuation.
        for suffix in ("\u20ac", "\u0301", "\u203f"):
            with self.subTest(suffix=ascii(suffix)):
                write(self.root, path, "package jdk.internal.mindex; class M3Class"
                      + suffix + "Peer {}\n")
                with self.assertRaisesRegex(ValueError, "declared type disagrees"):
                    GATE.validate(self.root)

    def test_policy_document_and_agent_link_are_required(self):
        (self.root / GATE.DOC).unlink()
        with self.assertRaisesRegex(ValueError, "missing M3JDK21 naming"):
            GATE.validate(self.root)
        write(self.root, GATE.DOC, "Fixture naming policy\n")
        write(self.root, "AGENTS.md", "No naming policy link\n")
        with self.assertRaisesRegex(ValueError, "lacks naming contract link"):
            GATE.validate(self.root)

    def test_plan_cannot_substitute_an_authority_or_malformed_identity(self):
        for field, value, reason in (
                ("schema", "OTHER_PLAN", "wrong schema"),
                ("authority", "alternate-map.json", "competing map"),
                ("authority_blob", None, "invalid authoritative map"),
                ("authority_blob", "a" * 39, "invalid authoritative map"),
                ("authority_blob", "A" * 40, "invalid authoritative map")):
            with self.subTest(field=field, value=value):
                write(self.root, GATE.PLAN, json.dumps(dict(self.plan, **{field: value})))
                with self.assertRaisesRegex(ValueError, reason):
                    GATE.validate(self.root)

    def test_map_schema_and_capability_records_cannot_be_ambiguous(self):
        original = copy.deepcopy(self.mapping)
        for records in (None, {}, [None], [{}], [{"id": 1}],
                        original["migration"]["records"] * 2):
            with self.subTest(records=records):
                self.mapping = copy.deepcopy(original)
                self.mapping["migration"]["records"] = records
                self.save()
                with self.assertRaises(ValueError):
                    GATE.validate(self.root)
        self.mapping = copy.deepcopy(original)
        self.mapping["schema"] = 2
        self.save()
        with self.assertRaisesRegex(ValueError, "unexpected canonical naming map schema"):
            GATE.validate(self.root)

    def test_empty_or_non_array_queue_is_not_acceptance(self):
        for operations in (None, {}, []):
            with self.subTest(operations=operations):
                self.plan["operations"] = operations
                self.save()
                with self.assertRaisesRegex(ValueError, "empty naming plan"):
                    GATE.validate(self.root)

    def test_operation_requires_typed_names_and_no_promotion_material(self):
        original = copy.deepcopy(self.plan["operations"][0])
        mutations = [
            ("kind", "RENAME"), ("proofs", {"claimed": "PASS"}),
            ("installer_crate", "unreviewed"), ("reason", None),
            ("before_type", None), ("before_type", "Unqualified"),
            ("after_type", "java..Class"), ("after_type", [])]
        for field, value in mutations:
            with self.subTest(field=field, value=value):
                self.plan["operations"] = [dict(original, **{field: value})]
                self.save()
                self.assert_cli_refused()
        self.plan["operations"] = [None]
        self.save()
        self.assert_cli_refused()

    def test_queued_operation_kinds_cannot_cross_owner_boundaries(self):
        original = copy.deepcopy(self.plan["operations"][0])
        for kind, destination, reason in (
                ("FORMAT_ONLY", "jdk.internal.mindex.Other", "format-only"),
                ("INTERNAL_MOVE", original["before_type"], "different internal"),
                ("INTERNAL_MOVE", "java.lang.Class", "different internal"),
                ("JDK_BACKEND", "com.m3.Class", "canonical JDK class"),
                ("JDK_BACKEND", "java.util.M3Arrays", "M3 name cannot"),
                ("JDK_BACKEND", "javax.xml.parsers.DocumentBuilder", "explicit owner/module review")):
            with self.subTest(kind=kind, destination=destination):
                self.plan["operations"] = [dict(original, kind=kind, after_type=destination)]
                self.save()
                with self.assertRaisesRegex(ValueError, reason):
                    GATE.validate(self.root)
        self.mapping["migration"]["records"][0]["targets"] = [{
            "symbol": "java.lang.Class",
            "path": "src/java.base/share/classes/java/lang/Class.java"}]
        self.plan["operations"] = [dict(original, before_type="java.lang.Class")]
        self.save()
        with self.assertRaisesRegex(ValueError, "mapped M3JDK21 owner"):
            GATE.validate(self.root)

    def test_queued_source_must_have_the_exact_canonical_path_and_symbol(self):
        record = self.mapping["migration"]["records"][0]
        original = copy.deepcopy(record["targets"][0])
        for path, reason in (
                (None, "invalid canonical target path"),
                ("elsewhere/jdk/internal/mindex/M3Class.java", "invalid canonical target path"),
                ("src/java.base/share/classes/jdk/internal/mindex/Other.java", "path and type disagree")):
            with self.subTest(path=path):
                record["targets"] = [dict(original, path=path)]
                self.save()
                with self.assertRaisesRegex(ValueError, reason):
                    GATE.validate(self.root)
        record["targets"] = [None, {"symbol": "some.other.Owner"}, original]
        self.save()
        self.assertEqual(GATE.validate(self.root), (1, ["class.view"]))
        record["targets"].pop()
        self.save()
        with self.assertRaisesRegex(ValueError, "not recorded as a canonical"):
            GATE.validate(self.root)

    def test_receiving_paths_refuse_aliases_and_symlink_owners(self):
        for path in (None, "", "/outside.java", "C:/outside.java", "a\\b", "a\x00b",
                     "m3//owner.java", "m3/./owner.java", "m3/../owner.java"):
            with self.subTest(path=path), self.assertRaises(ValueError):
                GATE.local_file(self.root, path)
        path = self.mapping["migration"]["records"][0]["targets"][0]["path"]
        owner = self.root / path
        actual = self.root / "actual.java"
        owner.rename(actual)
        owner.symlink_to(actual)
        with self.assertRaisesRegex(ValueError, "symlink target path forbidden"):
            GATE.validate(self.root)

    def test_duplicate_json_keys_cannot_override_a_pinned_document(self):
        text = json.dumps(self.mapping).replace('"schema": 1', '"schema": 1, "schema": 1', 1)
        write(self.root, GATE.MAP, text)
        self.plan["authority_blob"] = GATE.git_blob(text.encode("utf-8"))
        write(self.root, GATE.PLAN, json.dumps(self.plan))
        self.assert_cli_refused()
        self.save()
        text = json.dumps(self.plan).replace('"schema": ', '"schema": "OTHER", "schema": ', 1)
        write(self.root, GATE.PLAN, text)
        self.assert_cli_refused()

    def test_cli_success_and_input_errors_never_claim_product_acceptance(self):
        output, error = io.StringIO(), io.StringIO()
        with mock.patch.object(GATE, "ROOT", self.root), \
                contextlib.redirect_stdout(output), contextlib.redirect_stderr(error):
            self.assertEqual(GATE.main(["check.py"]), 0)
        self.assertIn("queued=1 mappingIDs=class.view", output.getvalue())
        self.assertIn("qualifiedJdkPromotions=0 productSourceEdits=0", output.getvalue())
        self.assertEqual(error.getvalue(), "")
        for content in (b"{", b"\xff"):
            with self.subTest(content=content):
                (self.root / GATE.PLAN).write_bytes(content)
                self.assert_cli_refused()
        (self.root / GATE.PLAN).unlink()
        self.assert_cli_refused()

    def test_script_entrypoint_refuses_unrecognized_arguments(self):
        error = io.StringIO()
        with mock.patch.object(sys, "argv", ["check.py", "--accept"]), \
                contextlib.redirect_stderr(error), self.assertRaises(SystemExit) as raised:
            runpy.run_path(str(HERE / "check.py"), run_name="__main__")
        self.assertEqual(raised.exception.code, 2)
        self.assertIn("usage: check.py", error.getvalue())

    def test_existing_jdk_m3_owner_remains_a_queued_candidate(self):
        target = self.mapping["migration"]["records"][0]["targets"][0]
        target.update(symbol="jdk.m3.algorithm.M3PrefixZ",
                      path="m3/algorithms/src/jdk/m3/algorithm/M3PrefixZ.java")
        write(self.root, target["path"],
              "package jdk.m3.algorithm; public final class M3PrefixZ {}\n")
        row = self.plan["operations"][0]
        row.update(kind="INTERNAL_MOVE", before_type=target["symbol"],
                   after_type="jdk.m3.algorithms.M3PrefixZ")
        self.save()
        self.assertEqual(GATE.validate(self.root), (1, ["class.view"]))
        self.assertEqual(row["state"], "QUEUED")

    def test_neighbor_namespace_is_not_a_mapped_m3_owner(self):
        target = self.mapping["migration"]["records"][0]["targets"][0]
        target.update(symbol="jdk.m30.algorithm.M3PrefixZ",
                      path="m3/algorithms/src/jdk/m30/algorithm/M3PrefixZ.java")
        write(self.root, target["path"],
              "package jdk.m30.algorithm; public final class M3PrefixZ {}\n")
        self.plan["operations"][0].update(
            kind="FORMAT_ONLY", before_type=target["symbol"], after_type=target["symbol"])
        self.save()
        with self.assertRaisesRegex(ValueError, "mapped M3JDK21 owner"):
            GATE.validate(self.root)

    def test_queued_type_cannot_be_declared_by_comment_or_literal(self):
        target = self.mapping["migration"]["records"][0]["targets"][0]
        for content in (
                "package jdk.internal.mindex; /* class M3Class {} */ class Hidden {}\n",
                'package jdk.internal.mindex; class Hidden { String s = "class M3Class {}"; }\n'):
            with self.subTest(content=content):
                write(self.root, target["path"], content)
                with self.assertRaisesRegex(ValueError, "declared type disagrees"):
                    GATE.validate(self.root)


class PublicCompanionContractTests(unittest.TestCase):
    """Real declaration fixtures exercise the existing map without runtime promotion."""

    save = NameContractReceiverTests.save
    tearDown = NameContractReceiverTests.tearDown

    def setUp(self):
        NameContractReceiverTests.setUp(self)
        self.source = {
            "module": "java.base", "symbol": "java.util.Arrays",
            "path": "src/java.base/share/classes/java/util/Arrays.java",
        }
        self.target = {
            "module": "jdk.m3.arrays", "symbol": "jdk.m3.arrays.M3Arrays",
            "path": "m3/arrays/src/main/java/jdk/m3/arrays/M3Arrays.java",
        }
        self.pin_content(self.source, "package java.util; public final class Arrays {}\n")
        self.pin_content(self.target, "package jdk.m3.arrays; public final class M3Arrays {}\n")
        write(self.root, "src/java.base/share/classes/module-info.java",
              "module java.base { exports java.util; }\n")
        write(self.root, "m3/arrays/src/main/java/module-info.java",
              "module jdk.m3.arrays { exports jdk.m3.arrays; }\n")
        self.record = {
            "id": "arrays.companion", "status": "implemented-unverified",
            "sources": [self.source], "targets": [self.target],
            "companion_of": {
                "java_api": {key: self.source[key] for key in ("module", "symbol", "path")},
                "target_symbol": self.target["symbol"], "form": "STATIC_OPERATIONS",
            },
        }
        self.mapping["migration"]["records"].append(self.record)
        self.save()

    def pin_content(self, artifact, content):
        write(self.root, artifact["path"], content)
        raw = (self.root / artifact["path"]).read_bytes()
        artifact["sha256"] = hashlib.sha256(raw).hexdigest()
        artifact["git_blob_sha1"] = GATE.git_blob(raw)

    def replace_api(self, module, symbol, path, content, package):
        self.source.update(module=module, symbol=symbol, path=path)
        self.pin_content(self.source, content)
        self.record["companion_of"]["java_api"] = {
            key: self.source[key] for key in ("module", "symbol", "path")
        }
        write(self.root, "src/" + module + "/share/classes/module-info.java",
              "module " + module + " { exports " + package + "; }\n")

    def assert_queued_only(self):
        self.save()
        self.assertEqual(GATE.validate(self.root), (1, ["class.view"]))
        self.assertEqual(self.record["status"], "implemented-unverified")
        self.assertEqual(self.plan["operations"][0]["state"], "QUEUED")

    def assert_refused(self, pattern):
        self.save()
        with self.assertRaisesRegex(ValueError, pattern):
            GATE.validate(self.root)

    def test_exported_static_companion_preserves_queued_acceptance(self):
        self.assert_queued_only()

    def test_companion_namespace_is_exact_not_a_similar_prefix(self):
        for symbol in ("com.m3.arrays.M3Arrays", "jdk.m30.arrays.M3Arrays",
                       "jdk.m3x.arrays.M3Arrays",
                       "jdk.internal.m3.arrays.M3Arrays"):
            with self.subTest(symbol=symbol):
                self.record["companion_of"]["target_symbol"] = symbol
                self.assert_refused("distinct jdk.m3 type")

    def test_companion_module_is_m3_owned_even_with_exported_package(self):
        for module in ("example.arrays", "jdk.m30.arrays", "jdk.internal.m3.arrays"):
            with self.subTest(module=module):
                self.target["module"] = module
                write(self.root, "m3/arrays/src/main/java/module-info.java",
                      "module " + module + " { exports jdk.m3.arrays; }\n")
                self.assert_refused("must use a jdk.m3 module")

    def test_typed_view_form_does_not_promote_runtime(self):
        self.record["companion_of"]["form"] = "TYPED_VIEW"
        self.assert_queued_only()

    def test_public_companion_requires_exact_com_m3_namespace(self):
        # Preserve this regression ID while enforcing the current jdk.m3 policy.
        for symbol in ("com.m3.arrays.M3Arrays", "java.util.M3Arrays",
                       "javax.m3.M3Arrays", "jdk.m3evil.arrays.M3Arrays"):
            with self.subTest(symbol=symbol):
                self.record["companion_of"]["target_symbol"] = symbol
                self.assert_refused("distinct jdk.m3 type")

    def test_org_w3c_dom_api_uses_actual_module_without_namespace_whitelist(self):
        self.replace_api("java.xml", "org.w3c.dom.Node",
                         "src/java.xml/share/classes/org/w3c/dom/Node.java",
                         "package org.w3c.dom; public interface Node {}\n", "org.w3c.dom")
        self.assert_queued_only()

    def test_nested_map_entry_uses_public_top_level_source_owner(self):
        self.replace_api("java.base", "java.util.Map.Entry",
                         "src/java.base/share/classes/java/util/Map.java",
                         "package java.util; public interface Map<K,V> {\n"
                         "    interface Entry<K,V> {}\n}\n", "java.util")
        self.assert_queued_only()

    def test_private_nested_type_is_not_an_exported_public_api(self):
        self.replace_api("java.base", "java.util.Map.Entry",
                         "src/java.base/share/classes/java/util/Map.java",
                         "package java.util; public class Map { private class Entry {} }\n",
                         "java.util")
        self.assert_refused("requires public type")

    def test_unexported_original_api_is_not_eligible(self):
        write(self.root, "src/java.base/share/classes/module-info.java",
              "module java.base { exports java.lang; }\n")
        self.assert_refused("not publicly exported: java.util")

    def test_qualified_exports_are_not_public_companion_eligibility(self):
        for descriptor, content in (
                ("src/java.base/share/classes/module-info.java",
                 "module java.base { exports java.util to jdk.m3.arrays; }\n"),
                ("m3/arrays/src/main/java/module-info.java",
                 "module jdk.m3.arrays { exports jdk.m3.arrays to java.base; }\n")):
            with self.subTest(descriptor=descriptor):
                original = (self.root / descriptor).read_text(encoding="utf-8")
                write(self.root, descriptor, content)
                self.assert_refused("not publicly exported")
                write(self.root, descriptor, original)

    def test_missing_module_descriptor_refused_for_both_owners(self):
        for descriptor in ("src/java.base/share/classes/module-info.java",
                           "m3/arrays/src/main/java/module-info.java"):
            with self.subTest(descriptor=descriptor):
                file = self.root / descriptor
                content = file.read_text(encoding="utf-8")
                file.unlink()
                self.assert_refused("module descriptor missing")
                write(self.root, descriptor, content)

    def test_exact_declared_module_required_for_both_owners(self):
        for descriptor in ("src/java.base/share/classes/module-info.java",
                           "m3/arrays/src/main/java/module-info.java"):
            with self.subTest(descriptor=descriptor):
                original = (self.root / descriptor).read_text(encoding="utf-8")
                write(self.root, descriptor,
                      "module wrong.owner { exports java.util; exports jdk.m3.arrays; }\n")
                self.assert_refused("declared module disagrees")
                write(self.root, descriptor, original)

    def test_public_companion_cannot_be_owned_by_java_base(self):
        self.target.update(module="java.base",
                           path="src/java.base/share/classes/jdk/m3/arrays/M3Arrays.java")
        self.pin_content(self.target, "package jdk.m3.arrays; public class M3Arrays {}\n")
        write(self.root, "src/java.base/share/classes/module-info.java",
              "module java.base { exports java.util; exports jdk.m3.arrays; }\n")
        self.assert_refused("must not be owned by java.base")

    def test_export_does_not_make_internal_companion_public(self):
        self.target.update(symbol="jdk.m3.arrays.internal.M3Slots",
                           path="m3/arrays/src/main/java/jdk/m3/arrays/internal/M3Slots.java")
        self.pin_content(self.target,
                         "package jdk.m3.arrays.internal; public class M3Slots {}\n")
        self.record["companion_of"]["target_symbol"] = self.target["symbol"]
        write(self.root, "m3/arrays/src/main/java/module-info.java",
              "module jdk.m3.arrays { exports jdk.m3.arrays.internal; }\n")
        self.assert_refused("must not expose an internal namespace")

    def test_open_companion_module_does_not_admit_private_storage(self):
        write(self.root, "m3/arrays/src/main/java/module-info.java",
              "open module jdk.m3.arrays { exports jdk.m3.arrays; }\n")
        self.assert_refused("module must not be open")

    def test_opened_companion_package_refused_even_when_qualified(self):
        for directive in ("opens jdk.m3.arrays;", "opens jdk.m3.arrays to java.base;"):
            with self.subTest(directive=directive):
                write(self.root, "m3/arrays/src/main/java/module-info.java",
                      "module jdk.m3.arrays { exports jdk.m3.arrays; " + directive + " }\n")
                self.assert_refused("package must not be opened")

    def test_source_relation_requires_exact_module_symbol_and_path(self):
        original = dict(self.record["companion_of"]["java_api"])
        for key, value in (("module", "java.xml"), ("symbol", "java.util.Map"),
                           ("path", "src/java.base/share/classes/java/util/Map.java")):
            with self.subTest(field=key):
                self.record["companion_of"]["java_api"] = dict(original, **{key: value})
                self.assert_refused("missing or ambiguous companion artifact ownership")

    def test_missing_and_duplicate_artifact_owners_refused(self):
        for field in ("sources", "targets"):
            original = self.record[field]
            for owners in ([], original + [dict(original[0])]):
                with self.subTest(field=field, owners=len(owners)):
                    self.record[field] = owners
                    self.assert_refused("missing or ambiguous companion artifact ownership")
            self.record[field] = original

    def test_unknown_target_symbol_refused(self):
        self.record["companion_of"]["target_symbol"] = "jdk.m3.arrays.NotMapped"
        self.assert_refused("missing or ambiguous companion artifact ownership")

    def test_internal_and_malformed_forms_refused(self):
        for form in ("INTERNAL_HELPER", "", None, [], {}):
            with self.subTest(form=form):
                self.record["companion_of"]["form"] = form
                self.assert_refused("unknown public companion form")

    def test_relation_fields_are_explicit(self):
        original = copy.deepcopy(self.record["companion_of"])
        for relation in (None, {}, dict(original, ready=True),
                         {key: value for key, value in original.items() if key != "form"}):
            with self.subTest(relation=relation):
                self.record["companion_of"] = relation
                self.assert_refused("invalid companion_of relation")
        self.record["companion_of"] = original
        self.record["companion_of"]["java_api"]["qualified"] = True
        self.assert_refused("invalid companion java_api ownership")

    def test_java_namespace_companion_and_self_relation_refused(self):
        for target in ("java.util.M3Arrays", "java.util.Arrays", "jdk.internal.m3.M3Arrays"):
            with self.subTest(target=target):
                self.record["companion_of"]["target_symbol"] = target
                self.assert_refused("distinct jdk.m3 type")
        self.record["companion_of"]["target_symbol"] = self.target["symbol"]
        self.record["companion_of"]["java_api"]["symbol"] = self.target["symbol"]
        self.assert_refused("distinct jdk.m3 type")

    def test_artifact_hash_drift_refused_for_both_owners_and_pins(self):
        for artifact in (self.source, self.target):
            for field in ("sha256", "git_blob_sha1"):
                with self.subTest(symbol=artifact["symbol"], pin=field):
                    original = artifact[field]
                    artifact[field] = "0" * len(original)
                    self.assert_refused("source hash drift")
                    artifact[field] = original

    def test_missing_and_null_artifact_pins_refused_for_both_owners(self):
        for artifact in (self.source, self.target):
            for field in ("sha256", "git_blob_sha1"):
                original = artifact[field]
                for missing in (True, False):
                    with self.subTest(symbol=artifact["symbol"], pin=field, missing=missing):
                        if missing:
                            del artifact[field]
                        else:
                            artifact[field] = None
                        self.assert_refused("source hash drift")
                        artifact[field] = original

    def test_changed_source_bytes_refused(self):
        write(self.root, self.source["path"],
              "package java.util; public final class Arrays { int changed; }\n")
        self.assert_refused("source hash drift")

    def test_package_and_top_level_source_owner_must_agree(self):
        self.pin_content(self.target, "package other; public final class M3Arrays {}\n")
        self.assert_refused("declared package disagrees")
        self.pin_content(self.target, "package jdk.m3.arrays; public final class M3Arrays {}\n")
        self.target["path"] = "m3/arrays/src/main/java/jdk/m3/arrays/Other.java"
        self.pin_content(self.target, "package jdk.m3.arrays; public final class M3Arrays {}\n")
        self.assert_refused("top-level source owner disagrees")

    def test_source_path_must_be_owned_by_named_jdk_module(self):
        self.source["module"] = "java.xml"
        self.record["companion_of"]["java_api"]["module"] = "java.xml"
        self.assert_refused("source module/path disagrees")

    def test_public_declaration_is_not_established_by_comment_or_string(self):
        for content in (
                "package jdk.m3.arrays; /* public class M3Arrays {} */ class Hidden {}\n",
                'package jdk.m3.arrays; class Hidden { String x = "public class M3Arrays {}"; }\n',
                "package jdk.m3.arrays; class M3Arrays {}\n"):
            with self.subTest(content=content):
                self.pin_content(self.target, content)
                self.assert_refused("public type declaration")

    def test_commented_module_export_does_not_count(self):
        write(self.root, "m3/arrays/src/main/java/module-info.java",
              "module jdk.m3.arrays { /* exports jdk.m3.arrays; */ }\n")
        self.assert_refused("not publicly exported")

    def test_source_path_traversal_refused(self):
        self.source["path"] = "src/java.base/../java.base/share/classes/java/util/Arrays.java"
        self.record["companion_of"]["java_api"]["path"] = self.source["path"]
        self.assert_refused("invalid companion relation source path")

    def test_duplicate_companion_relation_refused(self):
        duplicate = copy.deepcopy(self.record)
        duplicate["id"] = "arrays.companion.duplicate"
        self.mapping["migration"]["records"].append(duplicate)
        self.assert_refused("duplicate public companion relation")

    def test_distinct_java_apis_can_share_one_exact_companion_owner(self):
        second = copy.deepcopy(self.record)
        second["id"] = "objects.companion"
        source = second["sources"][0]
        source.update(symbol="java.util.Objects",
                      path="src/java.base/share/classes/java/util/Objects.java")
        self.pin_content(source, "package java.util; public final class Objects {}\n")
        second["companion_of"]["java_api"] = {
            key: source[key] for key in ("module", "symbol", "path")
        }
        self.mapping["migration"]["records"].append(second)
        self.assert_queued_only()

    def test_shared_companion_symbol_requires_consistent_module_path_and_pins(self):
        second = copy.deepcopy(self.record)
        second["id"] = "objects.companion"
        source = second["sources"][0]
        source.update(symbol="java.util.Objects",
                      path="src/java.base/share/classes/java/util/Objects.java")
        self.pin_content(source, "package java.util; public final class Objects {}\n")
        second["companion_of"]["java_api"] = {
            key: source[key] for key in ("module", "symbol", "path")
        }
        target = second["targets"][0]
        target.update(module="jdk.m3.another",
                      path="m3/another/src/main/java/jdk/m3/arrays/M3Arrays.java")
        self.pin_content(target, "package jdk.m3.arrays; public final class M3Arrays {}\n")
        write(self.root, "m3/another/src/main/java/module-info.java",
              "module jdk.m3.another { exports jdk.m3.arrays; }\n")
        self.mapping["migration"]["records"].append(second)
        self.assert_refused("conflicting public companion ownership")

    def test_public_companion_cannot_become_an_internal_replacement(self):
        operation = self.plan["operations"][0]
        operation.update(mapping_id=self.record["id"], before_type=self.target["symbol"])
        for kind, after in (("JDK_BACKEND", "java.util.Arrays"),
                            ("INTERNAL_MOVE", "jdk.internal.m3.M3Arrays")):
            with self.subTest(kind=kind):
                operation.update(kind=kind, after_type=after)
                self.assert_refused("public companion is not an internal replacement owner")

    def test_format_only_preserves_companion_name_without_promotion(self):
        self.plan["operations"][0].update(mapping_id=self.record["id"], kind="FORMAT_ONLY",
                                          before_type=self.target["symbol"],
                                          after_type=self.target["symbol"])
        self.save()
        self.assertEqual(GATE.validate(self.root), (1, [self.record["id"]]))

    def test_dollar_suffix_is_not_the_mapped_public_class(self):
        self.pin_content(self.target,
                         "package jdk.m3.arrays; public class M3Arrays$Other {}\n")
        self.assert_refused("public type declaration")

    def test_unicode_identifier_suffix_is_not_the_mapped_public_class(self):
        for suffix in ("\u20ac", "\u0301", "\u203f"):
            with self.subTest(suffix=ascii(suffix)):
                self.pin_content(self.target, "package jdk.m3.arrays; public class M3Arrays"
                                 + suffix + "Peer {}\n")
                self.assert_refused("public type declaration")

    def test_explicit_dollar_identifier_is_preserved(self):
        self.target.update(symbol="jdk.m3.arrays.M3Arrays$View",
                           path="m3/arrays/src/main/java/jdk/m3/arrays/M3Arrays$View.java")
        self.record["companion_of"]["target_symbol"] = self.target["symbol"]
        self.pin_content(self.target,
                         "package jdk.m3.arrays; public class M3Arrays$View {}\n")
        self.assert_queued_only()

    def test_public_type_bodies_must_be_present_and_balanced(self):
        for body, reason in (("public class M3Arrays", "missing public type body"),
                             ("public class M3Arrays {", "unbalanced public type body"),
                             ("public class M3Arrays {} public class M3Arrays {}",
                              "ambiguous public type declaration")):
            with self.subTest(body=body):
                self.pin_content(self.target, "package jdk.m3.arrays; " + body)
                self.assert_refused(reason)

    def test_record_component_annotation_braces_do_not_become_the_type_body(self):
        self.pin_content(self.target,
                         "package jdk.m3.arrays; @interface Mark { int[] value(); }\n"
                         "public record M3Arrays(@Mark({1, 2}) int value) {}\n")
        self.assert_queued_only()

    def test_companion_artifact_collections_require_arrays(self):
        for field in ("sources", "targets"):
            original = self.record[field]
            for value in (None, {}, "unreviewed"):
                with self.subTest(field=field, value=value):
                    self.record[field] = value
                    self.assert_refused("mapped source and target artifacts")
            self.record[field] = original

    def test_companion_requires_exact_named_module_and_mapped_source_root(self):
        for module in (None, "", "com..m3", []):
            with self.subTest(module=module):
                self.target["module"] = module
                self.assert_refused("exact named module")
        self.target["module"] = "jdk.m3.arrays"
        self.target["path"] = "m3/generated/jdk/m3/arrays/M3Arrays.java"
        self.pin_content(self.target, "package jdk.m3.arrays; public class M3Arrays {}\n")
        self.assert_refused("mapped module source root")

    def test_missing_companion_source_is_not_covered_by_its_receipt(self):
        (self.root / self.target["path"]).unlink()
        self.assert_refused("mapped companion relation source missing")

    def test_companion_in_an_explicit_jdk_module_root_remains_eligible(self):
        self.target["path"] = "src/jdk.m3.arrays/share/classes/jdk/m3/arrays/M3Arrays.java"
        self.pin_content(self.target, "package jdk.m3.arrays; public class M3Arrays {}\n")
        write(self.root, "src/jdk.m3.arrays/share/classes/module-info.java",
              "module jdk.m3.arrays { exports jdk.m3.arrays; }\n")
        self.assert_queued_only()


if __name__ == "__main__":
    unittest.main()
