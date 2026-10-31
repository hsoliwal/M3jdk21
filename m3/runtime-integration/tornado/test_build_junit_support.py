# Copyright 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
"""Refusal contracts for preparation of existing source-owned JUnit support."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import tempfile
import unittest
from xml.sax.saxutils import escape

import build_junit_support as support


class SourceSupportTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.repo = self.root / "repository"
        vendor = self.repo / support.PREFIX
        vendor.mkdir(parents=True)
        module_xml = "".join("<module>" + escape(name) + "</module>" for name in support.SELECTED)
        (vendor / "pom.xml").write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><modules>'
                                        + module_xml + "</modules></project>")
        for name, coordinate in support.SELECTED.items():
            module = vendor / name
            source = module / "src/main/java/Fixture.java"
            source.parent.mkdir(parents=True)
            source.write_text("// Fixture bytes only; no production JUnit implementation.\n")
            fields = "".join("<" + key + ">" + escape(value) + "</" + key + ">"
                             for key, value in zip(("groupId", "artifactId", "version"), coordinate))
            (module / "pom.xml").write_text('<project xmlns="http://maven.apache.org/POM/4.0.0">'
                                             + fields + "</project>")
            (module / "PROVENANCE.json").write_text(json.dumps({
                "coordinate": ":".join(coordinate), "files": [{
                    "repository": source.relative_to(self.repo).as_posix(),
                    "sha256": hashlib.sha256(source.read_bytes()).hexdigest()}]}))

    def test_exact_existing_models_and_source_inventory(self):
        inputs, evidence = support.source_contract(self.repo)
        self.assertEqual(evidence["selected_modules"], 10)
        self.assertEqual(evidence["provenance_files_checked"], 10)
        self.assertEqual(len(inputs), 31)
        self.assertEqual(inputs["pom.xml"].read_bytes(), (self.repo / support.PREFIX / "pom.xml").read_bytes())

    def test_source_drift_refuses_support(self):
        path = self.repo / support.PREFIX / "apiguardian-api/src/main/java/Fixture.java"
        path.write_text("changed source")
        with self.assertRaisesRegex(support.Refusal, "provenance drift"):
            support.source_contract(self.repo)

    def test_coordinate_drift_refuses_support(self):
        path = self.repo / support.PREFIX / "apiguardian-api/pom.xml"
        path.write_text(path.read_text().replace("1.1.2-m3-source-1", "1.1.2"))
        with self.assertRaisesRegex(support.Refusal, "coordinate drift"):
            support.source_contract(self.repo)

    def test_unqualified_parent_model_refuses_support(self):
        path = self.repo / support.PREFIX / "apiguardian-api/pom.xml"
        path.write_text(path.read_text().replace("</project>", "<parent/></project>"))
        with self.assertRaisesRegex(support.Refusal, "parent/nested"):
            support.source_contract(self.repo)

    def test_paths_cannot_escape_or_follow_source_symlinks(self):
        for name in ("../outside", "/absolute", "a//b", "a/./b", "c:relative", "a\\b"):
            with self.subTest(name=name), self.assertRaises(support.Refusal):
                support.checked(self.repo, name)
        (self.repo / "linked").symlink_to(self.repo / support.PREFIX / "pom.xml")
        with self.assertRaisesRegex(support.Refusal, "symlink"):
            support.checked(self.repo, "linked")

    def test_timeout_retains_failed_command_evidence(self):
        log = self.root / "timeout.log"
        event = support.run([sys.executable, "-c", "import time; time.sleep(10)"],
                            self.root, dict(os.environ), log, 0.05)
        self.assertEqual(event["exit_code"], -1)
        self.assertIn("M3_JUNIT_SUPPORT_TIMEOUT", log.read_text())
        self.assertEqual(event["log"]["sha256"], support.sha(log))

    @unittest.skipUnless(shutil.which("false"), "requires a deterministic failing command")
    def test_failed_maven_cannot_emit_a_support_lock(self):
        jdk = self.root / "fixture-jdk"
        (jdk / "bin").mkdir(parents=True)
        (jdk / "release").write_text('JAVA_VERSION="21.0.2"\n')
        for name in ("java", "javac"):
            (jdk / "bin" / name).write_text("fixture tool never executed\n")
        args = argparse.Namespace(repo_root=self.repo, jdk_home=jdk, maven=Path(shutil.which("false")),
                                  maven_repo=self.root / "m2", out=self.root / "evidence",
                                  maven_settings=None, maven_truststore=None,
                                  resolve_build_tools=False, command_timeout=10)
        saved = {name: os.environ.pop(name) for name in
                 ("JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS", "MAVEN_ARGS", "MAVEN_OPTS")
                 if name in os.environ}
        try:
            with self.assertRaisesRegex(support.Refusal, "Maven build failed"):
                support.prepare(args)
        finally:
            os.environ.update(saved)
        self.assertFalse((args.out / "JUNIT_LOCK.json").exists())
        result = json.loads((args.out / "BUILD_RESULT.json").read_text())
        self.assertEqual(result["status"], "FAILED")
        self.assertEqual(result["commands"][0]["exit_code"], 1)


if __name__ == "__main__":
    unittest.main()
