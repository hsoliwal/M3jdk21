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
    checks = {"content": 0, "missing": 0, "mixtures": 0, "coverage": 0}

    @classmethod
    def setUpClass(cls):
        import os
        import json
        import hashlib
        cls.temp_parent = Path(os.environ["M3_NLP_TEST_TEMP"]).resolve()
        if not cls.temp_parent.is_dir():
            raise ValueError("explicit existing M3_NLP_TEST_TEMP required")
        cls.input_root = Path(os.environ.get("M3_NLP_RECEIVER_INPUT_ROOT", str(DRIVER.ROOT))).resolve()
        cls.lock_raw = (DRIVER.HERE / "current-source-lock.json").read_bytes()
        cls.lock = json.loads(cls.lock_raw)
        cls.temporary = tempfile.TemporaryDirectory(dir=cls.temp_parent)
        cls.root = Path(cls.temporary.name).resolve()
        if not cls.root.is_relative_to(cls.temp_parent):
            raise ValueError("fixture escaped explicit temporary parent")
        cls.current = {}
        cls.decl_relative = DRIVER._RECEIVER_TRANSITION_PATH
        cls.decl_raw = (cls.input_root / cls.decl_relative).read_bytes()
        cls.document = json.loads(cls.decl_raw)
        cls.changes = {r["path"]: r for r in cls.document["files"]
                       if r["path"] in DRIVER._RECEIVER_SUCCESSORS}
        for entry in cls.lock["modules"].values():
            for name, digest in entry["files"].items():
                raw = (cls.input_root / name).read_bytes()
                expected = cls.changes[name]["after"]["sha256"] if name in cls.changes else digest
                if hashlib.sha256(raw).hexdigest() != expected:
                    raise ValueError("test input is not the sealed current cohort: " + name)
                cls.current[name] = raw
                path = cls.root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(raw)
        cls.decl = cls.root / cls.decl_relative
        cls.decl.parent.mkdir(parents=True, exist_ok=True)
        cls.decl.write_bytes(cls.decl_raw)
        cls.original = {}
        for name in sorted(cls.changes):
            raw = (Path(__file__).resolve().parent / "receiver-original"
                   / (Path(name).name + ".txt")).read_bytes()
            if hashlib.sha256(raw).hexdigest() != cls.changes[name]["before"]["sha256"]:
                raise ValueError("unsealed original fixture: " + name)
            cls.original[name] = raw
        if len(cls.current) != 145 or len(cls.original) != 4:
            raise ValueError("complete145-file/four-source cohort required")

    @classmethod
    def tearDownClass(cls):
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
        for name in self.original:
            (self.root / name).write_bytes(self.current[name])
        with self.assertRaisesRegex(ValueError, "receiver source drift: .*JoinedM3StringPiece"):
            DRIVER.verify_receiver(self.root)

    def test_current_cohort_reports_actual_manifest_without_mutating_lock(self):
        import hashlib
        import json
        admission = self.declared()
        self.assertEqual(self.lock, admission["lock"])
        self.assertEqual(self.lock_raw, (DRIVER.HERE / "current-source-lock.json").read_bytes())
        self.assertEqual(145, len(admission["files"]))
        self.assertEqual(145, len(admission["bodies"]))
        self.assertEqual(4, len(admission["changes"]))
        for row in admission["files"]:
            raw = self.current[row["path"]]
            self.assertEqual(hashlib.sha256(raw).hexdigest(), row["sha256"])
            self.assertEqual(hashlib.sha1(b"blob " + str(len(raw)).encode("ascii")
                                         + b"\0" + raw).hexdigest(), row["gitBlob"])
            self.assertEqual(len(raw), row["bytes"])
        report = DRIVER._receiver_report(admission, "a" * 40, [], Path("snapshot"))
        self.assertEqual("a" * 40, report["observed_revision"])
        self.assertEqual("declared-v5-four-lexicon-successors", report["cohort"])
        self.assertEqual("verified-owned-snapshot", report["compilation_inputs"])
        self.assertEqual(admission["files"], report["effective_manifest"])
        expected = hashlib.sha256(json.dumps(admission["files"], sort_keys=True,
                                              separators=(",", ":")).encode("utf-8")).hexdigest()
        self.assertEqual(expected, report["effective_manifest_sha256"])

    def test_unqualified_mixed_cohorts_are_refused(self):
        names = sorted(self.original)
        for mask in range(16):
            with self.subTest(mask=mask):
                for index, name in enumerate(names):
                    (self.root / name).write_bytes(self.current[name] if mask & (1 << index)
                                                   else self.original[name])
                if mask == 15:
                    self.assertEqual(4, len(self.declared()["changes"]))
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
            name = next(iter(self.current))
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
                name = next(iter(entry["files"]))
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
            self.assertEqual(145, len(observed["effective_manifest"]))
            self.assertEqual(4, len(observed["transitions"]))
            self.assertEqual(7, len(observed["revalidation"]))
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


if __name__ == "__main__":
    unittest.main()
