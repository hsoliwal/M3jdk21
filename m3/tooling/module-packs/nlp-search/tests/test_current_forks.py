#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Regression checks for JMOD aggregation and legal-notice ownership."""
import importlib.util
import os
from pathlib import Path
import tempfile
import unittest
import zipfile


SPEC = importlib.util.spec_from_file_location(
    "current_forks", Path(__file__).resolve().parents[1] / "current_forks.py")
DRIVER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(DRIVER)


class ModuleCollectionTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(dir=os.environ.get("M3_NLP_TEST_TEMP"))
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.jars = self.root / "jars"
        self.jars.mkdir()
        self.legal = self.root / "legal"
        self.legal.mkdir()
        self.owners = {}

    def donor(self, donor, module, notices, payload=b"original resource"):
        build = self.root / donor
        target = build / "pack/jmods" / (module + ".jmod")
        target.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(target, "w") as archive:
            archive.writestr("classes/module-info.class", module.encode())
            archive.writestr("classes/example/resource.txt", payload)
            for path, data in notices.items():
                archive.writestr("legal/" + path, data)
        return build

    def collect(self, build):
        return DRIVER.collect_jmods(build, self.jars, self.legal, self.owners)

    def notices(self):
        return {path.relative_to(self.legal).as_posix(): path.read_bytes()
                for path in self.legal.rglob("*") if path.is_file()}

    def test_repeated_notices_keep_original_scope_once(self):
        original = {"opennlp/LICENSE": b"OpenNLP license"}
        self.collect(self.donor("opennlp", "shared.module", original))
        before = (self.jars / "shared.module.jar").read_bytes()
        combined = {**original, "lucene/NOTICE": b"Lucene notice"}
        build = self.donor("lucene", "shared.module", combined)
        self.donor("lucene", "second.module", combined)
        self.collect(build)
        self.assertEqual(combined, self.notices())
        self.assertEqual(before, (self.jars / "shared.module.jar").read_bytes())
        self.assertEqual({"shared.module", "second.module"}, set(self.owners))
        with zipfile.ZipFile(self.jars / "shared.module.jar") as archive:
            self.assertEqual({"module-info.class", "example/resource.txt"},
                             set(archive.namelist()))
            self.assertEqual(b"original resource", archive.read("example/resource.txt"))

    def test_conflicting_original_notice_path_is_refused(self):
        self.collect(self.donor("first", "first.module", {"project/LICENSE": b"one"}))
        with self.assertRaisesRegex(ValueError, "conflicting legal notice"):
            self.collect(self.donor("second", "second.module", {"project/LICENSE": b"two"}))
        self.assertEqual({"project/LICENSE": b"one"}, self.notices())

    def test_distinct_notice_scopes_preserve_distinct_contents(self):
        self.collect(self.donor("first", "first.module", {"first/LICENSE": b"one"}))
        self.collect(self.donor("second", "second.module", {"second/LICENSE": b"two"}))
        self.assertEqual({"first/LICENSE": b"one", "second/LICENSE": b"two"}, self.notices())

    def test_conflicting_module_payload_is_still_refused(self):
        self.collect(self.donor("first", "shared.module", {}))
        before = (self.jars / "shared.module.jar").read_bytes()
        with self.assertRaisesRegex(ValueError, "different implementations claim the same module"):
            self.collect(self.donor("second", "shared.module", {}, payload=b"changed resource"))
        self.assertEqual(before, (self.jars / "shared.module.jar").read_bytes())

    def test_legal_path_traversal_is_refused(self):
        with self.assertRaisesRegex(ValueError, "unsafe JMOD entry"):
            self.collect(self.donor("first", "first.module", {"../../outside": b"escape"}))
        self.assertFalse((self.root / "outside").exists())




class ReceiverAdmissionTest(unittest.TestCase):
    """Real locked bytes and owned fixtures; image orchestration is separately mocked."""
    version = 5
    catalog_name = "m3/core/src/com/m3/text/SharedLexiconCatalog.java"
    payload_name = "m3/core/src/com/m3/text/SynexiaPrecomputePayload.java"

    @staticmethod
    def _identity(raw):
        import hashlib
        return {"bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(),
                "gitBlob": hashlib.sha1(b"blob " + str(len(raw)).encode("ascii")
                                        + b"\0" + raw).hexdigest()}

    @classmethod
    def setUpClass(cls):
        import json
        # Each concrete version owns its mutable fixture state and counters.
        cls.checks = {"content": 0, "missing": 0, "mixtures": 0, "coverage": 0}
        cls.temp_parent = Path(os.environ["M3_NLP_TEST_TEMP"]).resolve()
        if not cls.temp_parent.is_dir():
            raise ValueError("explicit existing M3_NLP_TEST_TEMP required")
        cls.input_root = Path(os.environ.get("M3_NLP_RECEIVER_INPUT_ROOT", str(DRIVER.ROOT))).resolve()
        cls.lock_raw = (DRIVER.HERE / "current-source-lock.json").read_bytes()
        if cls._identity(cls.lock_raw)["sha256"] != DRIVER._RECEIVER_LOCK_SHA256:
            raise ValueError("unsealed original test lock")
        cls.lock = json.loads(cls.lock_raw)
        cls.transitions, cls.catalogs = {}, {}
        fixtures = Path(__file__).resolve().parent
        choices = ((5, DRIVER._RECEIVER_TRANSITION_PATH, DRIVER._RECEIVER_TRANSITION_SHA256),
                   (6, DRIVER._RECEIVER_TRANSITION_V6_PATH, DRIVER._RECEIVER_TRANSITION_V6_SHA256),
                   (7, DRIVER._RECEIVER_TRANSITION_V7_PATH, DRIVER._RECEIVER_TRANSITION_V7_SHA256),
                   (8, DRIVER._RECEIVER_TRANSITION_V8_PATH, DRIVER._RECEIVER_TRANSITION_V8_SHA256),
                   (9, DRIVER._RECEIVER_TRANSITION_V9_PATH, DRIVER._RECEIVER_TRANSITION_V9_SHA256),
                   (10, DRIVER._RECEIVER_TRANSITION_V10_PATH, DRIVER._RECEIVER_TRANSITION_V10_SHA256),
                   (11, DRIVER._RECEIVER_TRANSITION_V11_PATH, DRIVER._RECEIVER_TRANSITION_V11_SHA256))
        for version, relative, seal in choices:
            declaration = (cls.input_root / relative).read_bytes()
            if cls._identity(declaration)["sha256"] != seal:
                raise ValueError("unsealed test declaration: " + relative)
            document = json.loads(declaration)
            successors = DRIVER._RECEIVER_V8_SUCCESSORS if version >= 8 else DRIVER._RECEIVER_SUCCESSORS
            changes = {r["path"]: r for r in document["files"] if r["path"] in successors}
            if set(changes) != successors or len(document["files"]) != (9 if version >= 8 else 8):
                raise ValueError("complete four-source declaration required")
            catalog = (fixtures / "receiver-history" / changes[cls.catalog_name]["after"]["gitBlob"]
                       / "SharedLexiconCatalog.java.txt").read_bytes()
            if cls._identity(catalog) != changes[cls.catalog_name]["after"]:
                raise ValueError("unsealed catalog fixture: V%d" % version)
            cls.transitions[version] = dict(path=relative, seal=seal, raw=declaration, changes=changes)
            cls.catalogs[version] = catalog
        selected = cls.transitions[cls.version]
        cls.decl_relative, cls.decl_raw = selected["path"], selected["raw"]
        cls.changes = selected["changes"]
        cls.cohort = ("declared-v%d-five-lexicon-successors" % cls.version if cls.version >= 8
                      else "declared-v%d-four-lexicon-successors" % cls.version)
        cls.payload_before = (fixtures / "receiver-original" / "SynexiaPrecomputePayload.java.txt").read_bytes()
        cls.payloads = {}
        for version in (8, 9, 11):
            payload_row = cls.transitions[version]["changes"][cls.payload_name]
            payload = (fixtures / "receiver-history" / payload_row["after"]["gitBlob"]
                       / "SynexiaPrecomputePayload.java.txt").read_bytes()
            if (cls._identity(cls.payload_before) != payload_row["before"]
                    or cls._identity(payload) != payload_row["after"]):
                raise ValueError("unsealed payload fixture: V%d" % version)
            cls.payloads[version] = payload
        cls.payloads[10] = cls.payloads[9]  # V10 changes the catalog only.
        cls.payload_after = cls.payloads[max(8, cls.version)]

        cls.original = {}
        for name in sorted(cls.changes):
            original = (fixtures / "receiver-original" / (Path(name).name + ".txt")).read_bytes()
            if cls._identity(original) != cls.changes[name]["before"]:
                raise ValueError("unsealed original fixture: " + name)
            cls.original[name] = original
        cls.current = {}
        # Check every physical input before selecting the historical/current test catalog.
        # Substitution is confined to the owned ephemeral fixture, never the input root.
        for module, entry in cls.lock["modules"].items():
            for name, digest in entry["files"].items():
                physical = (cls.input_root / name).read_bytes()
                identity = cls._identity(physical)
                if name == cls.catalog_name:
                    known = [v["changes"][name]["after"] for v in cls.transitions.values()]
                    if identity not in known:
                        raise ValueError("test input is not a sealed V5/V6/V7 catalog: " + name)
                    current = cls.catalogs[cls.version]
                elif name == cls.payload_name:
                    if identity not in [cls._identity(cls.payload_before),
                                       *[cls._identity(p) for p in cls.payloads.values()]]:
                        raise ValueError("test input is not a sealed original/V8/V9 payload")
                    current = cls.payload_after if cls.version >= 8 else cls.payload_before
                else:
                    expected = cls.changes[name]["after"]["sha256"] if name in cls.changes else digest
                    if (identity["sha256"] != expected or
                            (name in cls.changes and identity != cls.changes[name]["after"])):
                        raise ValueError("test input is not the sealed current cohort: " + name)
                    current = physical
                cls.current[name] = current
            if entry.get("source_roots"):
                actual = {p.relative_to(cls.input_root).as_posix() for source in entry["source_roots"]
                          for p in (cls.input_root / source).rglob("*.java")}
                expected = {p for p in entry["files"] if p.endswith(".java")}
                if actual != expected:
                    raise ValueError("test input source coverage drift: " + module)
        if len(cls.current) != 145 or len(cls.original) != (5 if cls.version >= 8 else 4):
            raise ValueError("complete145-file/four-source cohort required")
        cls.temporary = tempfile.TemporaryDirectory(dir=cls.temp_parent)
        cls.root = Path(cls.temporary.name).resolve()
        cls.addClassCleanup(cls.cleanup_fixture)
        if not cls.root.is_relative_to(cls.temp_parent):
            raise ValueError("fixture escaped explicit temporary parent")
        for name, current in cls.current.items():
            path = cls.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(current)
        for transition in cls.transitions.values():
            path = cls.root / transition["path"]
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(transition["raw"])
        cls.decl = cls.root / cls.decl_relative

    @classmethod
    def cleanup_fixture(cls):
        if not cls.root.is_relative_to(cls.temp_parent):
            raise ValueError("fixture cleanup escaped temporary parent")
        cls.temporary.cleanup()

    def tearDown(self):
        for name, raw in self.current.items():
            path = self.root / name
            if not path.is_file() or path.read_bytes() != raw:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(raw)
        self.decl.write_bytes(self.decl_raw)

    def declared(self):
        return DRIVER._receiver_admission(self.root, self.decl)

    def test_default_strict_api_and_original_return_are_preserved(self):
        for name, raw in self.original.items():
            (self.root / name).write_bytes(raw)
        self.assertEqual(self.lock, DRIVER.verify_receiver(self.root))
        strict = DRIVER._receiver_admission(self.root)
        report = DRIVER._receiver_report(strict, None, [], None)
        self.assertEqual("original-strict-lock", report["cohort"])
        self.assertEqual("original-physical-inputs", report["compilation_inputs"])
        self.assertIsNone(report["declared_transition"])
        for name in self.original:
            (self.root / name).write_bytes(self.current[name])
        with self.assertRaisesRegex(ValueError, "receiver source drift: .*JoinedM3StringPiece"):
            DRIVER.verify_receiver(self.root)
        with self.assertRaisesRegex(ValueError, "receiver source drift: .*JoinedM3StringPiece"):
            DRIVER._receiver_admission(self.root)

    def test_current_cohort_reports_actual_manifest_without_mutating_lock(self):
        import hashlib
        import json
        admission = self.declared()
        self.assertEqual(self.lock, admission["lock"])
        self.assertEqual(self.lock_raw, (DRIVER.HERE / "current-source-lock.json").read_bytes())
        self.assertEqual(145, len(admission["files"]))
        self.assertEqual(145, len(admission["bodies"]))
        self.assertEqual(len(self.changes), len(admission["changes"]))
        for row in admission["files"]:
            raw = self.current[row["path"]]
            self.assertEqual(hashlib.sha256(raw).hexdigest(), row["sha256"])
            self.assertEqual(hashlib.sha1(b"blob " + str(len(raw)).encode("ascii")
                                         + b"\0" + raw).hexdigest(), row["gitBlob"])
            self.assertEqual(len(raw), row["bytes"])
        report = DRIVER._receiver_report(admission, "a" * 40, [], Path("snapshot"))
        self.assertEqual("a" * 40, report["observed_revision"])
        self.assertEqual(self.cohort, report["cohort"])
        self.assertEqual(dict(path=self.decl_relative, **self._identity(self.decl_raw)),
                         report["declared_transition"])
        self.assertEqual(self.transitions[self.version]["seal"],
                         report["declared_transition"]["sha256"])
        relative = DRIVER._receiver_admission(self.root, Path(self.decl_relative))
        self.assertEqual(admission["files"], relative["files"])
        self.assertEqual(admission["transition"], relative["transition"])
        self.assertEqual("verified-owned-snapshot", report["compilation_inputs"])
        self.assertEqual(admission["files"], report["effective_manifest"])
        expected = hashlib.sha256(json.dumps(admission["files"], sort_keys=True,
                                              separators=(",", ":")).encode("utf-8")).hexdigest()
        self.assertEqual(expected, report["effective_manifest_sha256"])

    def test_unqualified_mixed_cohorts_are_refused(self):
        names = sorted(self.original)
        for mask in range(1 << len(names)):
            with self.subTest(mask=mask):
                for index, name in enumerate(names):
                    (self.root / name).write_bytes(self.current[name] if mask & (1 << index)
                                                   else self.original[name])
                if mask == (1 << len(names)) - 1:
                    self.assertEqual(len(names), len(self.declared()["changes"]))
                else:
                    with self.assertRaises(ValueError):
                        self.declared()
                if mask == 0:
                    self.assertEqual(self.lock, DRIVER.verify_receiver(self.root))
                else:
                    with self.assertRaises(ValueError):
                        DRIVER.verify_receiver(self.root)
                self.checks["mixtures"] += 1

    def test_every_locked_content_change_and_missing_file_is_refused(self):
        import re
        for name, raw in self.current.items():
            path = self.root / name
            try:
                with self.subTest(kind="content", path=name):
                    path.write_bytes(raw + b"\nunknown-source-drift\n")
                    with self.assertRaisesRegex(ValueError, "receiver source drift: " + re.escape(name)):
                        self.declared()
                    self.checks["content"] += 1
                path.write_bytes(raw)
                with self.subTest(kind="missing", path=name):
                    path.unlink()
                    with self.assertRaisesRegex(ValueError, "receiver source drift: " + re.escape(name)):
                        self.declared()
                    self.checks["missing"] += 1
            finally:
                path.write_bytes(raw)

    def test_each_declared_source_root_refuses_extra_java(self):
        for module, entry in self.lock["modules"].items():
            for source in entry.get("source_roots", []):
                extra = self.root / source / "UnexpectedNlpAdmission.java"
                try:
                    extra.write_bytes(b"final class UnexpectedNlpAdmission {}\n")
                    with self.subTest(module=module):
                        with self.assertRaisesRegex(ValueError, "receiver source coverage drift: " + module):
                            self.declared()
                        self.checks["coverage"] += 1
                finally:
                    extra.unlink()

    def test_transition_name_seal_missing_and_original_lock_seal_are_refused(self):
        import json
        from unittest.mock import patch
        wrong = self.decl.with_name("RECEIVER_TRANSITIONS_V4.json")
        wrong.write_bytes(self.decl_raw)
        try:
            with self.assertRaisesRegex(ValueError, "unknown or redirected receiver transition"):
                DRIVER._receiver_admission(self.root, wrong)
        finally:
            wrong.unlink()
        for raw in [self.decl_raw + b"\n", b"not-json", b"{}", self.decl_raw[:-1]]:
            self.decl.write_bytes(raw)
            with self.assertRaises(ValueError):
                self.declared()
        self.decl.unlink()
        with self.assertRaises(OSError):
            self.declared()
        self.decl.write_bytes(self.decl_raw)
        with tempfile.TemporaryDirectory(dir=self.temp_parent) as temporary:
            here = Path(temporary)
            altered = json.loads(self.lock_raw)
            altered["receiver"]["base_commit"] = "0" * 40
            (here / "current-source-lock.json").write_text(json.dumps(altered), encoding="utf-8")
            with patch.object(DRIVER, "HERE", here):
                with self.assertRaisesRegex(ValueError, "original lock seal differs"):
                    self.declared()

    def test_owned_snapshot_and_pre_post_revalidation_refuse_mutation(self):
        admission = self.declared()
        with tempfile.TemporaryDirectory(dir=self.temp_parent) as temporary:
            out = Path(temporary)
            staged = DRIVER._stage_receiver_inputs(out, admission)
            for name, raw in self.current.items():
                self.assertEqual(raw, (staged / name).read_bytes())
            DRIVER._revalidate_receiver(self.root, admission, staged)
            name = self.payload_name if self.version >= 8 else next(iter(self.current))
            path = self.root / name
            path.write_bytes(self.current[name] + b"drift")
            with self.assertRaises(ValueError):
                DRIVER._revalidate_receiver(self.root, admission, staged)
            path.write_bytes(self.current[name])
            path = staged / name
            path.write_bytes(self.current[name] + b"drift")
            with self.assertRaises(ValueError):
                DRIVER._revalidate_receiver(self.root, admission, staged)
            path.write_bytes(self.current[name])
            DRIVER._revalidate_receiver(self.root, admission, staged)
            self.decl.write_bytes(self.decl_raw + b"\n")
            try:
                with self.assertRaises(ValueError):
                    DRIVER._revalidate_receiver(self.root, admission, staged)
            finally:
                self.decl.write_bytes(self.decl_raw)
            DRIVER._revalidate_receiver(self.root, admission, staged)
        with tempfile.TemporaryDirectory(dir=self.temp_parent) as temporary:
            admission["bodies"][name] += b"bad snapshot"
            with self.assertRaises(ValueError):
                DRIVER._stage_receiver_inputs(Path(temporary), admission)

    def test_build_compiles_snapshots_and_records_revalidation_without_image_claim(self):
        import argparse
        import contextlib
        import io
        import json
        import os
        import hashlib
        from unittest.mock import patch
        with tempfile.TemporaryDirectory(dir=self.temp_parent) as temporary:
            work = Path(temporary)
            jdk = work / "jdk"
            (jdk / "bin").mkdir(parents=True)
            for name in ("java", "javac", "jar", "jmod", "jlink"):
                (jdk / "bin" / (name + (".exe" if os.name == "nt" else ""))).write_bytes(b"unit-placeholder")
            args = argparse.Namespace(receiver=self.root, jdk_home=jdk, out=work / "out",
                receiver_transition=self.decl, opennlp_source=work / "opennlp",
                lucene_source=work / "lucene", opennlp_dependencies=work / "deps",
                lucene_dependencies=work / "deps", recipe_runner=work / "recipes",
                recipe_resources=work / "resources", maven="not-launched", maven_settings=None)
            compiled = []
            def compile_probe(jdk_arg, root, out, module, entry):
                self.assertNotEqual(self.root, root)
                self.assertTrue(root.is_relative_to(out))
                self.assertEqual(jdk, jdk_arg)
                for name in entry["files"]:
                    self.assertEqual(self.current[name], (root / name).read_bytes())
                    self.assertEqual(hashlib.sha256(self.current[name]).hexdigest(), entry["files"][name])
                # The compiler source remains frozen even through transient receiver mutation.
                name = self.payload_name if self.version >= 8 and self.payload_name in entry["files"] else next(iter(entry["files"]))
                original = self.root / name
                original.write_bytes(self.current[name] + b"temporary receiver mutation")
                try:
                    self.assertEqual(self.current[name], (root / name).read_bytes())
                finally:
                    original.write_bytes(self.current[name])
                compiled.append(module)
            # This tests orchestration/provenance only: no donor builds, compiler or image is launched.
            with contextlib.redirect_stdout(io.StringIO()),                  patch.object(DRIVER, "run"),                  patch.object(DRIVER, "collect_jmods", return_value={}),                  patch.object(DRIVER, "compile_owner", side_effect=compile_probe),                  patch.object(DRIVER, "verify_image", return_value={"scope": "MOCKED_IMAGE_NOT_QUALIFIED"}),                  patch.object(DRIVER, "_receiver_revision", return_value="a" * 40):
                DRIVER.build(args)
            self.assertEqual(["com.m3.text", "jdk.m3.compiletime", "jdk.m3.nlp.search"], compiled)
            report = json.loads((args.out / "BUILD_REPORT.json").read_bytes())
            observed = report["receiver_input_admission"]
            self.assertEqual(self.lock["receiver"], report["receiver"])
            self.assertEqual("a" * 40, observed["observed_revision"])
            self.assertEqual(self.cohort, observed["cohort"])
            self.assertEqual(dict(path=self.decl_relative, **self._identity(self.decl_raw)),
                             observed["declared_transition"])
            self.assertEqual({"scope": "MOCKED_IMAGE_NOT_QUALIFIED"}, report["tests"])
            self.assertEqual(145, len(observed["effective_manifest"]))
            self.assertEqual(len(self.changes), len(observed["transitions"]))
            self.assertEqual(7, len(observed["revalidation"]))
            self.assertEqual([phase + ":" + module for module in compiled
                              for phase in ("before", "after")] + ["before-report"],
                             [r["phase"] for r in observed["revalidation"]])
            self.assertEqual(["before:" + m for m in compiled],
                             [r["phase"] for r in observed["revalidation"] if r["phase"].startswith("before:")])
            self.assertEqual("verified-owned-snapshot", observed["compilation_inputs"])
            self.assertEqual({}, report["module_jars"])  # No image success is inferred from this model.

    def test_cli_keeps_default_strict_and_parses_only_explicit_transition(self):
        import contextlib
        import io
        import sys
        from unittest.mock import patch
        base = ["current_forks.py", "--jdk-home", "jdk", "--out", "out",
                "--receiver", str(self.root), "--opennlp-source", "opennlp",
                "--lucene-source", "lucene", "--recipe-runner", "runner",
                "--recipe-resources", "resources"]
        with patch.object(DRIVER, "build") as build, patch.object(sys, "argv", base):
            DRIVER.main()
        self.assertIsNone(build.call_args.args[0].receiver_transition)
        self.assertEqual(self.root, build.call_args.args[0].receiver)
        with patch.object(DRIVER, "build") as build,              patch.object(sys, "argv", base + ["--receiver-transition", str(self.decl)]):
            DRIVER.main()
        self.assertEqual(self.decl, build.call_args.args[0].receiver_transition)
        with patch.object(DRIVER, "build") as build,              patch.object(sys, "argv", base + ["--accept-unknown-source-drift"]),              contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit) as refused:
                DRIVER.main()
        self.assertEqual(2, refused.exception.code)
        build.assert_not_called()


class ReceiverV6AdmissionTest(ReceiverAdmissionTest):
    """Run all nine receiver contracts with the independently sealed V6 catalog."""
    version = 6

    def test_cross_version_catalogs_with_correct_sealed_declarations_are_refused(self):
        import re
        path = self.root / self.catalog_name
        try:
            for version in (5, 6):
                declaration = self.root / self.transitions[version]["path"]
                with self.subTest(declaration=version, catalog=11 - version):
                    path.write_bytes(self.catalogs[11 - version])
                    with self.assertRaisesRegex(ValueError,
                            "receiver source drift: " + re.escape(self.catalog_name)):
                        DRIVER._receiver_admission(self.root, declaration)
                # The same declaration still admits its exact entire cohort.
                path.write_bytes(self.catalogs[version])
                admitted = DRIVER._receiver_admission(self.root, declaration)
                self.assertEqual(145, len(admitted["files"]))
                self.assertEqual(4, len(admitted["changes"]))
                self.assertEqual(self._identity(self.catalogs[version]),
                                 {k: next(r for r in admitted["files"]
                                          if r["path"] == self.catalog_name)[k]
                                  for k in ("bytes", "sha256", "gitBlob")})
        finally:
            path.write_bytes(self.current[self.catalog_name])



class ReceiverV7AdmissionTest(ReceiverAdmissionTest):
    """Inherited physical145-input contracts with the independently sealed V7 catalog."""
    version = 7

    def test_v7_cross_version_catalogs_and_declarations_are_refused(self):
        import re
        path = self.root / self.catalog_name
        try:
            for selected, other in ((5, 7), (6, 7), (7, 5), (7, 6)):
                declaration = self.root / self.transitions[selected]["path"]
                with self.subTest(declaration=selected, catalog=other):
                    path.write_bytes(self.catalogs[other])
                    with self.assertRaisesRegex(ValueError,
                            "receiver source drift: " + re.escape(self.catalog_name)):
                        DRIVER._receiver_admission(self.root, declaration)
                path.write_bytes(self.catalogs[selected])
                admitted = DRIVER._receiver_admission(self.root, declaration)
                self.assertEqual(145, len(admitted["files"]))
                self.assertEqual(4, len(admitted["changes"]))
        finally:
            path.write_bytes(self.current[self.catalog_name])

    def test_unsealed_identity_array_without_singleton_guard_is_refused(self):
        import re
        branch = """        // A singleton has no competing slot: compare before hashing caller text.
        if (index.length == 2) {
            SourceMapping only = index[0] != null ? index[0] : index[1];
            return sourceId.equals(only.sourceId()) && recordId.equals(only.recordId())
                    ? only : null;
        }
"""
        current = self.catalogs[7]
        self.assertEqual(1, current.count(branch.encode()))
        rejected = current.replace(branch.encode(), b"", 1)
        self.assertEqual("8d41fa9595ace9ff6874b5b3cbe66172f502ea04",
                         self._identity(rejected)["gitBlob"])
        path = self.root / self.catalog_name
        try:
            path.write_bytes(rejected)
            for selected in (5, 6, 7):
                with self.subTest(declaration=selected):
                    with self.assertRaisesRegex(ValueError,
                            "receiver source drift: " + re.escape(self.catalog_name)):
                        DRIVER._receiver_admission(self.root,
                            self.root / self.transitions[selected]["path"])
        finally:
            path.write_bytes(self.current[self.catalog_name])


class ReceiverV8AdmissionTest(ReceiverAdmissionTest):
    """Five-source V8 with original-lock and historical four-source compatibility."""
    version = 8

    def test_payload_version_confusion_is_refused_with_matching_catalogs(self):
        import re
        payload = self.root / self.payload_name
        catalog = self.root / self.catalog_name
        try:
            for version in range(5, self.version):
                catalog.write_bytes(self.catalogs[version])
                payload.write_bytes(self.payloads[version] if version >= 8 else self.payload_before)
                decl = self.root / self.transitions[version]["path"]
                admitted = DRIVER._receiver_admission(self.root, decl)
                self.assertEqual(145, len(admitted["files"]))
                self.assertEqual(5 if version >= 8 else 4, len(admitted["changes"]))
                payload.write_bytes(self.payload_after)
                with self.subTest(legacy=version):
                    with self.assertRaisesRegex(ValueError, "receiver source drift: " + re.escape(self.payload_name)):
                        DRIVER._receiver_admission(self.root, decl)
            catalog.write_bytes(self.catalogs[self.version])
            payload.write_bytes(self.payload_before)
            with self.assertRaisesRegex(ValueError, "receiver source drift: " + re.escape(self.payload_name)):
                self.declared()
            payload.write_bytes(self.payload_after)
            self.assertEqual(5, len(self.declared()["changes"]))
        finally:
            catalog.write_bytes(self.current[self.catalog_name])
            payload.write_bytes(self.current[self.payload_name])

    def test_v8_retains_prior_rows_custody_and_complete_payload_identity(self):
        import json
        before = json.loads(self.transitions[self.version - 1]["raw"])
        after = json.loads(self.transitions[self.version]["raw"])
        self.assertEqual(before["files"][:8], after["files"][:8])
        self.assertEqual(before["custodySha256"], after["custodySha256"])
        self.assertEqual(before["frozenProfileSha256"], after["frozenProfileSha256"])
        self.assertEqual(self.transitions[self.version - 1]["seal"], after["predecessorDeclarationSha256"])
        row = after["files"][8]
        self.assertEqual(self.payload_name, row["path"])
        self.assertEqual(9, len(after["files"]))
        if self.version == 9:
            self.assertEqual(before["files"][8]["before"], row["before"])
            self.assertEqual(self._identity(self.payloads[8]), before["files"][8]["after"])
            self.assertNotEqual(before["files"][8]["after"], row["after"])
        self.assertEqual(self._identity(self.payload_before), row["before"])
        self.assertEqual(self._identity(self.payload_after), row["after"])

    def test_unguarded_catalog_is_refused_with_each_matching_payload_version(self):
        import re
        current = self.catalogs[7]
        branch = b"        // A singleton has no competing slot: compare before hashing caller text.\n        if (index.length == 2) {\n            SourceMapping only = index[0] != null ? index[0] : index[1];\n            return sourceId.equals(only.sourceId()) && recordId.equals(only.recordId())\n                    ? only : null;\n        }\n"
        self.assertEqual(1, current.count(branch))
        rejected = current.replace(branch, b"", 1)
        self.assertEqual("8d41fa9595ace9ff6874b5b3cbe66172f502ea04",
                         self._identity(rejected)["gitBlob"])
        catalog = self.root / self.catalog_name
        payload = self.root / self.payload_name
        try:
            for version in range(5, self.version + 1):
                payload.write_bytes(self.payloads[version] if version >= 8 else self.payload_before)
                catalog.write_bytes(self.catalogs[version])
                decl = self.root / self.transitions[version]["path"]
                self.assertEqual(145, len(DRIVER._receiver_admission(self.root, decl)["files"]))
                catalog.write_bytes(rejected)
                with self.subTest(declaration=version):
                    with self.assertRaisesRegex(ValueError, "receiver source drift: " + re.escape(self.catalog_name)):
                        DRIVER._receiver_admission(self.root, decl)
        finally:
            catalog.write_bytes(self.current[self.catalog_name])
            payload.write_bytes(self.current[self.payload_name])


class ReceiverV9AdmissionTest(ReceiverV8AdmissionTest):
    """Exact private-read successor; retain V8 and original frozen identities."""
    version = 9

    def test_prior_payloads_are_rejected_by_the_exact_v9_declaration(self):
        import re
        payload = self.root / self.payload_name
        try:
            for prior in (self.payload_before, self.payloads[8]):
                payload.write_bytes(prior)
                with self.assertRaisesRegex(ValueError, "receiver source drift: " + re.escape(self.payload_name)):
                    self.declared()
            payload.write_bytes(self.payload_after)
            self.assertEqual(5, len(self.declared()["changes"]))
        finally:
            payload.write_bytes(self.current[self.payload_name])


    def test_exact_payload_matrix_without_catalog_masking(self):
        import re
        payload = self.root / self.payload_name
        values = (self.payload_before, self.payloads[8], self.payloads[9])
        try:
            for name, raw in self.original.items():
                (self.root / name).write_bytes(raw)
            for index, raw in enumerate(values):
                payload.write_bytes(raw)
                with self.subTest(selection="strict", payload=index):
                    if index == 0:
                        self.assertEqual(self.lock, DRIVER.verify_receiver(self.root))
                        self.assertEqual(0, len(DRIVER._receiver_admission(self.root)["changes"]))
                    else:
                        for call in (DRIVER.verify_receiver, DRIVER._receiver_admission):
                            with self.assertRaisesRegex(ValueError, "receiver source drift: " + re.escape(self.payload_name)):
                                call(self.root)
            for name in self.original:
                (self.root / name).write_bytes(self.current[name])
            catalog = self.root / self.catalog_name
            for version in (5, 6, 7, 8, 9):
                catalog.write_bytes(self.catalogs[version])
                selected = self.root / self.transitions[version]["path"]
                for index, raw in enumerate(values):
                    payload.write_bytes(raw)
                    expected = 0 if version < 8 else version - 7
                    with self.subTest(selection=version, payload=index):
                        if index == expected:
                            self.assertEqual(5 if version >= 8 else 4,
                                             len(DRIVER._receiver_admission(self.root, selected)["changes"]))
                        else:
                            with self.assertRaisesRegex(ValueError, "receiver source drift: " + re.escape(self.payload_name)):
                                DRIVER._receiver_admission(self.root, selected)
        finally:
            for name in self.original:
                (self.root / name).write_bytes(self.current[name])

    def test_v8_and_v9_sealed_documents_cannot_exchange_paths(self):
        try:
            for selected, wrong in ((8, 9), (9, 8)):
                path = self.root / self.transitions[selected]["path"]
                path.write_bytes(self.transitions[wrong]["raw"])
                with self.assertRaisesRegex(ValueError, "receiver transition or original lock seal differs"):
                    DRIVER._receiver_admission(self.root, path)
                path.write_bytes(self.transitions[selected]["raw"])
        finally:
            for version in (8, 9):
                (self.root / self.transitions[version]["path"]).write_bytes(self.transitions[version]["raw"])


class ReceiverV10AdmissionTest(ReceiverV9AdmissionTest):
    """Catalog-only successor retaining the exact V9 payload and all old seals."""
    version = 10

    def test_v8_retains_prior_rows_custody_and_complete_payload_identity(self):
        import json
        before = json.loads(self.transitions[9]["raw"])
        after = json.loads(self.transitions[10]["raw"])
        self.assertEqual(9, len(after["files"]))
        self.assertEqual(self.transitions[9]["seal"], after["predecessorDeclarationSha256"])
        self.assertEqual(before["custodySha256"], after["custodySha256"])
        self.assertEqual(before["frozenProfileSha256"], after["frozenProfileSha256"])
        for index, row in enumerate(after["files"]):
            if index == 6:
                self.assertEqual(self.catalog_name, row["path"])
                self.assertEqual(before["files"][index]["before"], row["before"])
                self.assertEqual(self._identity(self.catalogs[9]), before["files"][index]["after"])
                self.assertEqual(self._identity(self.catalogs[10]), row["after"])
                self.assertNotEqual(before["files"][index]["after"], row["after"])
            else:
                self.assertEqual(before["files"][index], row)
        self.assertEqual(self.payloads[9], self.payload_after)

    def test_payload_version_confusion_is_refused_with_matching_catalogs(self):
        # V9 and V10 intentionally share payload9; catalog selection discriminates.
        import re
        payload = self.root / self.payload_name
        catalog = self.root / self.catalog_name
        try:
            for selected in range(5, 11):
                catalog.write_bytes(self.catalogs[selected])
                expected = self.payloads[selected] if selected >= 8 else self.payload_before
                payload.write_bytes(expected)
                declaration = self.root / self.transitions[selected]["path"]
                self.assertEqual(145, len(DRIVER._receiver_admission(self.root, declaration)["files"]))
                wrong = [self.payload_before, self.payloads[8], self.payloads[9]]
                for raw in wrong:
                    if raw == expected:
                        continue
                    payload.write_bytes(raw)
                    with self.assertRaisesRegex(ValueError, "receiver source drift: " + re.escape(self.payload_name)):
                        DRIVER._receiver_admission(self.root, declaration)
                    payload.write_bytes(expected)
        finally:
            catalog.write_bytes(self.current[self.catalog_name])
            payload.write_bytes(self.current[self.payload_name])

    def test_v9_v10_catalogs_cannot_cross_with_identical_payload(self):
        import re
        catalog = self.root / self.catalog_name
        try:
            self.assertEqual(self.payloads[9], self.payloads[10])
            for selected, wrong in ((9, 10), (10, 9)):
                declaration = self.root / self.transitions[selected]["path"]
                catalog.write_bytes(self.catalogs[selected])
                self.assertEqual(145, len(DRIVER._receiver_admission(self.root, declaration)["files"]))
                catalog.write_bytes(self.catalogs[wrong])
                with self.assertRaisesRegex(ValueError, "receiver source drift: " + re.escape(self.catalog_name)):
                    DRIVER._receiver_admission(self.root, declaration)
        finally:
            catalog.write_bytes(self.current[self.catalog_name])

    def test_v9_v10_documents_cannot_exchange_named_paths(self):
        try:
            for selected, wrong in ((9, 10), (10, 9)):
                path = self.root / self.transitions[selected]["path"]
                path.write_bytes(self.transitions[wrong]["raw"])
                with self.assertRaisesRegex(ValueError, "receiver transition or original lock seal differs"):
                    DRIVER._receiver_admission(self.root, path)
                path.write_bytes(self.transitions[selected]["raw"])
        finally:
            for version in (9, 10):
                (self.root / self.transitions[version]["path"]).write_bytes(self.transitions[version]["raw"])

class ReceiverV11AdmissionTest(ReceiverAdmissionTest):
    """Payload-only successor; V10 and V11 share the exact catalog."""

    version = 11

    def test_v11_retains_v10_rows_custody_and_exact_original_basis(self):
        import json
        before = json.loads(self.transitions[10]["raw"])
        after = json.loads(self.transitions[11]["raw"])
        self.assertEqual(self.transitions[10]["seal"], after["predecessorDeclarationSha256"])
        self.assertEqual(before["custodySha256"], after["custodySha256"])
        self.assertEqual(before["frozenProfileSha256"], after["frozenProfileSha256"])
        self.assertEqual(9, len(after["files"]))
        changed = []
        for old, new in zip(before["files"], after["files"], strict=True):
            self.assertEqual(old["path"], new["path"])
            self.assertEqual(old["before"], new["before"])
            if old != new:
                changed.append(new["path"])
                self.assertEqual(self._identity(self.payloads[10]), old["after"])
                self.assertEqual(self._identity(self.payloads[11]), new["after"])
        self.assertEqual([self.payload_name], changed)
        self.assertEqual(self.catalogs[10], self.catalogs[11])
        self.assertNotEqual(self.payloads[10], self.payloads[11])
        self.assertEqual(self._identity(self.payload_before),
                         self.changes[self.payload_name]["before"])

    def test_all_named_profiles_discriminate_every_distinct_payload(self):
        import re
        catalog = self.root / self.catalog_name
        payload = self.root / self.payload_name
        values = (self.payload_before, self.payloads[8], self.payloads[9], self.payloads[11])
        try:
            for selected in range(5, 12):
                catalog.write_bytes(self.catalogs[selected])
                expected = self.payloads[selected] if selected >= 8 else self.payload_before
                declaration = self.root / self.transitions[selected]["path"]
                for index, raw in enumerate(values):
                    payload.write_bytes(raw)
                    with self.subTest(profile=selected, payload=index):
                        if raw == expected:
                            result = DRIVER._receiver_admission(self.root, declaration)
                            self.assertEqual(145, len(result["files"]))
                            self.assertEqual(5 if selected >= 8 else 4, len(result["changes"]))
                        else:
                            with self.assertRaisesRegex(ValueError,
                                    "receiver source drift: " + re.escape(self.payload_name)):
                                DRIVER._receiver_admission(self.root, declaration)
        finally:
            catalog.write_bytes(self.current[self.catalog_name])
            payload.write_bytes(self.current[self.payload_name])

    def test_v10_v11_documents_cannot_exchange_names_with_shared_catalog(self):
        try:
            for selected, wrong in ((10, 11), (11, 10)):
                path = self.root / self.transitions[selected]["path"]
                path.write_bytes(self.transitions[wrong]["raw"])
                with self.assertRaisesRegex(ValueError,
                                             "receiver transition or original lock seal differs"):
                    DRIVER._receiver_admission(self.root, path)
                path.write_bytes(self.transitions[selected]["raw"])
        finally:
            for version in (10, 11):
                (self.root / self.transitions[version]["path"]).write_bytes(
                    self.transitions[version]["raw"])


if __name__ == "__main__":
    unittest.main()
