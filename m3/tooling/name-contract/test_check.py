# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Target receiver fixtures; declaration and queue checks are not JDK acceptance."""
from __future__ import annotations

import importlib.util
import copy
import hashlib
import json
from pathlib import Path
import tempfile
import unittest

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
            "module": "com.m3.arrays", "symbol": "com.m3.arrays.M3Arrays",
            "path": "m3/arrays/src/main/java/com/m3/arrays/M3Arrays.java",
        }
        self.pin_content(self.source, "package java.util; public final class Arrays {}\n")
        self.pin_content(self.target, "package com.m3.arrays; public final class M3Arrays {}\n")
        write(self.root, "src/java.base/share/classes/module-info.java",
              "module java.base { exports java.util; }\n")
        write(self.root, "m3/arrays/src/main/java/module-info.java",
              "module com.m3.arrays { exports com.m3.arrays; }\n")
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

    def test_typed_view_form_does_not_promote_runtime(self):
        self.record["companion_of"]["form"] = "TYPED_VIEW"
        self.assert_queued_only()

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
                 "module java.base { exports java.util to com.m3.arrays; }\n"),
                ("m3/arrays/src/main/java/module-info.java",
                 "module com.m3.arrays { exports com.m3.arrays to java.base; }\n")):
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
                      "module wrong.owner { exports java.util; exports com.m3.arrays; }\n")
                self.assert_refused("declared module disagrees")
                write(self.root, descriptor, original)

    def test_public_companion_cannot_be_owned_by_java_base(self):
        self.target.update(module="java.base",
                           path="src/java.base/share/classes/com/m3/arrays/M3Arrays.java")
        self.pin_content(self.target, "package com.m3.arrays; public class M3Arrays {}\n")
        write(self.root, "src/java.base/share/classes/module-info.java",
              "module java.base { exports java.util; exports com.m3.arrays; }\n")
        self.assert_refused("must not be owned by java.base")

    def test_export_does_not_make_internal_companion_public(self):
        self.target.update(symbol="com.m3.arrays.internal.M3Slots",
                           path="m3/arrays/src/main/java/com/m3/arrays/internal/M3Slots.java")
        self.pin_content(self.target,
                         "package com.m3.arrays.internal; public class M3Slots {}\n")
        self.record["companion_of"]["target_symbol"] = self.target["symbol"]
        write(self.root, "m3/arrays/src/main/java/module-info.java",
              "module com.m3.arrays { exports com.m3.arrays.internal; }\n")
        self.assert_refused("must not expose an internal namespace")

    def test_open_companion_module_does_not_admit_private_storage(self):
        write(self.root, "m3/arrays/src/main/java/module-info.java",
              "open module com.m3.arrays { exports com.m3.arrays; }\n")
        self.assert_refused("module must not be open")

    def test_opened_companion_package_refused_even_when_qualified(self):
        for directive in ("opens com.m3.arrays;", "opens com.m3.arrays to java.base;"):
            with self.subTest(directive=directive):
                write(self.root, "m3/arrays/src/main/java/module-info.java",
                      "module com.m3.arrays { exports com.m3.arrays; " + directive + " }\n")
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
        self.record["companion_of"]["target_symbol"] = "com.m3.arrays.NotMapped"
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
                self.assert_refused("distinct com.m3 type")
        self.record["companion_of"]["target_symbol"] = self.target["symbol"]
        self.record["companion_of"]["java_api"]["symbol"] = self.target["symbol"]
        self.assert_refused("distinct com.m3 type")

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
        self.pin_content(self.target, "package com.m3.arrays; public final class M3Arrays {}\n")
        self.target["path"] = "m3/arrays/src/main/java/com/m3/arrays/Other.java"
        self.pin_content(self.target, "package com.m3.arrays; public final class M3Arrays {}\n")
        self.assert_refused("top-level source owner disagrees")

    def test_source_path_must_be_owned_by_named_jdk_module(self):
        self.source["module"] = "java.xml"
        self.record["companion_of"]["java_api"]["module"] = "java.xml"
        self.assert_refused("source module/path disagrees")

    def test_public_declaration_is_not_established_by_comment_or_string(self):
        for content in (
                "package com.m3.arrays; /* public class M3Arrays {} */ class Hidden {}\n",
                'package com.m3.arrays; class Hidden { String x = "public class M3Arrays {}"; }\n',
                "package com.m3.arrays; class M3Arrays {}\n"):
            with self.subTest(content=content):
                self.pin_content(self.target, content)
                self.assert_refused("public type declaration")

    def test_commented_module_export_does_not_count(self):
        write(self.root, "m3/arrays/src/main/java/module-info.java",
              "module com.m3.arrays { /* exports com.m3.arrays; */ }\n")
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
        target.update(module="com.m3.another",
                      path="m3/another/src/main/java/com/m3/arrays/M3Arrays.java")
        self.pin_content(target, "package com.m3.arrays; public final class M3Arrays {}\n")
        write(self.root, "m3/another/src/main/java/module-info.java",
              "module com.m3.another { exports com.m3.arrays; }\n")
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


if __name__ == "__main__":
    unittest.main()
