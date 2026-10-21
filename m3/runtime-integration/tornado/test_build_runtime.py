#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
"""Negative acceptance cases for custody, phase ordering and image provenance."""
import hashlib
import json
from pathlib import Path
import tempfile
import unittest

import build_runtime as build


class AdmissionGuards(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)

    def tearDown(self):
        self.temporary.cleanup()

    def lock(self):
        (self.root / "api.java").write_text("final class Api {}\n")
        return {"schema": "m3-tornado-source-lock/1", "source": {"commit": build.PIN, "version": "7.2.0"},
                "files": [{"path": "api.java", "sha256": build.sha(self.root / "api.java"), "mode": "100644"}]}

    def test_source_extra_and_modified_file_refused(self):
        lock = self.lock()
        self.assertEqual(build.verify_source(lock, self.root)["files"], 1)
        (self.root / "unlocked.java").write_text("class Unlocked {}")
        with self.assertRaises(build.Refusal):
            build.verify_source(lock, self.root)
        (self.root / "unlocked.java").unlink()
        (self.root / "api.java").write_text("class Changed {}")
        with self.assertRaises(build.Refusal):
            build.verify_source(lock, self.root)

    def test_duplicate_and_executable_mode_refused(self):
        lock = self.lock()
        lock["files"].append(dict(lock["files"][0]))
        with self.assertRaises(build.Refusal):
            build.verify_source(lock, self.root)
        lock["files"].pop()
        (self.root / "api.java").chmod(0o755)
        with self.assertRaises(build.Refusal):
            build.verify_source(lock, self.root)

    def test_paths_and_symlinks_cannot_escape_custody(self):
        for name in ("../x", "/tmp/x", "x/../y", "x//y", "x\\y", "C:/x", "x\nq"):
            with self.subTest(name=name), self.assertRaises(build.Refusal):
                build.relative_path(name)
        lock = self.lock()
        (self.root / "api.java").unlink()
        (self.root / "api.java").symlink_to("/etc/passwd")
        with self.assertRaises(build.Refusal):
            build.verify_source(lock, self.root)

    def test_git_source_mode_requires_owner_execute_bit(self):
        lock = self.lock()
        source = self.root / "api.java"
        candidate = build.Build.__new__(build.Build)
        candidate.image = self.root / "image"
        for mode in (0o645, 0o654):
            with self.subTest(posix_mode=oct(mode)):
                source.chmod(mode)
                lock["files"][0]["mode"] = "100755"
                with self.assertRaises(build.Refusal):
                    build.verify_source(lock, self.root)
                with self.assertRaises(build.Refusal):
                    build.verify_staged(lock, self.root, {})
                lock["files"][0]["mode"] = "100644"
                build.verify_source(lock, self.root)
                build.verify_staged(lock, self.root, {})
                self.assertFalse(candidate.output_seal(source)["executable"])
        source.chmod(0o744)
        lock["files"][0]["mode"] = "100755"
        build.verify_source(lock, self.root)
        build.verify_staged(lock, self.root, {})
        self.assertTrue(candidate.output_seal(source)["executable"])
        lock["files"][0]["mode"] = "100644"
        with self.assertRaises(build.Refusal):
            build.verify_source(lock, self.root)
        with self.assertRaises(build.Refusal):
            build.verify_staged(lock, self.root, {})

    def test_staged_source_and_generated_source_injection_refused(self):
        lock = self.lock()
        self.assertEqual(build.verify_staged(lock, self.root, {}), {})
        rogue = self.root / "src/main/java/Rogue.java"
        rogue.parent.mkdir(parents=True)
        rogue.write_text("class Rogue {}")
        with self.assertRaises(build.Refusal):
            build.verify_staged(lock, self.root)
        rogue.unlink()
        generated = self.root / "api/target/generated-sources/Added.java"
        generated.parent.mkdir(parents=True)
        generated.write_text("class Added {}")
        with self.assertRaises(build.Refusal):
            build.verify_staged(lock, self.root, {})
        sealed = build.verify_staged(lock, self.root)
        generated.write_text("class Modified {}")
        with self.assertRaises(build.Refusal):
            build.verify_staged(lock, self.root, sealed)
        (self.root / "api.java").write_text("class Changed {}")
        with self.assertRaises(build.Refusal):
            build.verify_staged(lock, self.root)

    def test_failed_skipped_or_reordered_phase_cannot_advance(self):
        build.validate_phase_chain({"phases": []}, "lint")
        for events in ([], [{"phase": "lint", "status": "FAIL"}], [{"phase": "compile", "status": "PASS"}]):
            with self.subTest(events=events), self.assertRaises(build.Refusal):
                build.validate_phase_chain({"phases": events}, "compile")
        build.validate_phase_chain({"phases": [{"phase": "lint", "status": "PASS"}]}, "compile")

    def test_phase_output_inventory_refuses_added_and_removed_classes(self):
        candidate = build.Build.__new__(build.Build)
        candidate.out = self.root
        candidate.image = self.root / "image"
        candidate.m2 = self.root / "m2"
        original = self.root / "receiver-classes/Original.class"
        original.parent.mkdir()
        original.write_bytes(b"sealed class fixture")
        candidate.state = {"output_seals": candidate.seal_outputs(), "commands": [], "reactor_artifacts": []}
        candidate.verify_outputs()
        extra = original.with_name("Unsealed.class")
        extra.write_bytes(b"unsealed class fixture")
        with self.assertRaises(build.Refusal):
            candidate.verify_outputs()
        extra.unlink()
        candidate.verify_outputs()
        original.unlink()
        with self.assertRaises(build.Refusal):
            candidate.verify_outputs()

    def test_launcher_export_file_cannot_add_execution_flags(self):
        path = self.root / "exports"
        path.write_text("--add-exports java.base/java.lang=tornado.runtime\n--add-opens=java.base/java.util=tornado.runtime\n")
        self.assertEqual(len(build.parse_export_list(path)), 2)
        for text in ("-javaagent:untrusted.jar", "--add-opens", "--add-opens=java.base/java.lang=tornado.runtime -XX:-EnableJVMCI"):
            path.write_text(text)
            with self.subTest(text=text), self.assertRaises(build.Refusal):
                build.parse_export_list(path)

    def test_boolean_claim_does_not_admit_stock_jdk(self):
        path = self.root / "receipt.json"
        path.write_text(json.dumps({"schema": "m3-jdk-source-build/1", "status": "PASS", "source_built": True}))
        with self.assertRaises((build.Refusal, KeyError)):
            build.verify_m3_receipt(path, self.root)

    def test_source_receipt_binds_every_jmod_and_build_log(self):
        jdk = self.root / "jdk"
        for name in ("release", "bin/java", "lib/server/libjvm.so", "jmods/java.base.jmod", "jmods/jdk.compiler.jmod"):
            path = jdk / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(name)
        log = self.root / "build.log"
        log.write_text("source image build completed\n")
        receipt = {"schema": "m3-jdk-source-build/1", "status": "PASS", "jdk_home": str(jdk),
                   "source": {"commit": "a" * 40, "tree_sha256": "b" * 64},
                   "build": {"argv": ["make", "images"], "cwd": str(self.root), "exit_code": 0,
                             "log": {"path": str(log), "sha256": build.sha(log)}},
                   "artifacts": [{"path": name, "sha256": build.sha(path)} for name, path in build.file_inventory(jdk).items()],
                   "vm": {"UseM3StringStorage": False}}
        path = self.root / "receipt.json"
        path.write_text(json.dumps(receipt))
        build.verify_m3_receipt(path, jdk)
        (jdk / "jmods/jdk.compiler.jmod").write_text("replaced")
        with self.assertRaises(build.Refusal):
            build.verify_m3_receipt(path, jdk)
        (jdk / "jmods/jdk.compiler.jmod").write_text("jmods/jdk.compiler.jmod")
        log.write_text("replaced log")
        with self.assertRaises(build.Refusal):
            build.verify_m3_receipt(path, jdk)

    def test_dependency_path_cannot_disagree_with_coordinate(self):
        lock = {"runtime_dependencies": [{"group": "safe.group", "artifact": "api", "version": "1",
                 "maven_path": "../../foreign.jar", "role": "runtime", "sha256": "a" * 64}]}
        with self.assertRaises(build.Refusal):
            build.verify_dependencies(lock, self.root)

    def test_lint_baseline_rejects_drift_and_removed_diagnostics(self):
        lock = self.lock()
        report = {"checker_version": "9.3", "reported_files": ["api.java"],
                  "diagnostics": [{"path": "api.java", "line": "1", "message": "existing", "source": "rule"}]}
        baseline = {"schema": "m3-tornado-checkstyle-baseline/1", "backend": "opencl", "source_commit": build.PIN,
                    "source_files_sha256": hashlib.sha256(build.canonical(lock["files"])).hexdigest(),
                    "config_sha256": "a" * 64, "plugin_version": "3.3.0", **report}
        self.assertEqual(build.verify_checkstyle_baseline(baseline, report, lock, "a" * 64, "opencl")["status"], "BASELINE_MATCH")
        for changed in ({**report, "diagnostics": []}, {**report, "checker_version": "9.4"},
                        {**report, "reported_files": []},
                        {**report, "diagnostics": report["diagnostics"] * 2},
                        {**report, "diagnostics": [{**report["diagnostics"][0], "line": "2"}]}):
            with self.subTest(changed=changed), self.assertRaises(build.Refusal):
                build.verify_checkstyle_baseline(baseline, changed, lock, "a" * 64, "opencl")
        with self.assertRaises(build.Refusal):
            build.verify_checkstyle_baseline(baseline, report, lock, "b" * 64, "opencl")
        with self.assertRaises(build.Refusal):
            build.verify_checkstyle_baseline(baseline, report, lock, "a" * 64, "cuda")

    def test_source_runtime_dependency_version_mismatch_refused(self):
        lock = {"runtime_dependencies": [{"group": "org.apache.commons", "artifact": "commons-math3", "version": "3.2"}]}
        graph = [("org.apache.commons", "commons-math3", "3.2", "jar", "compile")]
        build.verify_runtime_coordinates(lock, graph)
        for wrong in ([], [("org.apache.commons", "commons-math3", "3.6.1", "jar", "compile")],
                      graph + [("org.apache.commons", "commons-math3", "3.6.1", "jar", "compile")]):
            with self.subTest(graph=wrong), self.assertRaises(build.Refusal):
                build.verify_runtime_coordinates(lock, wrong)


if __name__ == "__main__":
    unittest.main()
