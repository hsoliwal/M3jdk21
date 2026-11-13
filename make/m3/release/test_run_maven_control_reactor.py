# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Real Git and synthetic subprocess fixtures; never product Maven proof."""
from __future__ import annotations

import contextlib
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest import mock


SPEC = importlib.util.spec_from_file_location(
    "m3_maven_producer", Path(__file__).with_name("run_maven_control_reactor.py"))
producer = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(producer)
POM = ('<project xmlns="http://maven.apache.org/POM/4.0.0">'
       '<modelVersion>4.0.0</modelVersion><groupId>fixture</groupId>'
       '<artifactId>fixture-product</artifactId><version>1</version>{}</project>\n')


class MavenProducerTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="m3-maven-producer-fixture-")
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)
        self.root = self.directory / "source"
        self.out = self.directory / "evidence"
        self.root.mkdir()
        self.git("init", "-q")
        self.write(".gitignore", "**/target/\n")
        self.write("m3/pom.xml", POM.format('<packaging>pom</packaging><modules><module>leaf</module></modules>'))
        self.write("m3/leaf/pom.xml", POM.format(""))
        self.write(producer.ORIGIN_PATH, json.dumps({
            "repository": producer.release.CANONICAL_RECIPE_REPOSITORY,
            "commit": "a" * 40, "recipe_id": "synthetic.fixture.only"}))
        self.head = self.commit()
        self.calls = self.directory / "calls.jsonl"
        self.maven = self.directory / "mvn"
        self.fake_maven()

    def git(self, *args):
        return subprocess.check_output(
            ["git", "-C", str(self.root), "-c", "user.name=Producer Fixture",
             "-c", "user.email=producer-fixture@example.invalid", *args],
            text=True, stderr=subprocess.PIPE).strip()

    def write(self, path, content):
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")

    def commit(self):
        self.git("add", "-A")
        self.git("commit", "-qm", "synthetic fixture only")
        return self.git("rev-parse", "HEAD")

    def fake_maven(self, *, code=0, java="21.0.13", version="3.10.0", mutate=False, sleep=0):
        # This process records real argv/exit behavior, but it is deliberately not Maven.
        self.maven.write_text(
            "#!/usr/bin/env python3\nimport json, pathlib, sys, time\n"
            f"calls = pathlib.Path({str(self.calls)!r})\n"
            "with calls.open('a') as output: output.write(json.dumps(sys.argv[1:]) + '\\n')\n"
            "if sys.argv[1:] == ['-version']:\n"
            f"    print('Apache Maven {version}\\nJava version: {java}, vendor: SYNTHETIC FIXTURE')\n"
            "    raise SystemExit(0)\n"
            "print('SYNTHETIC MAVEN SUBPROCESS FIXTURE ONLY; NO PRODUCT BUILD')\n"
            + ("pathlib.Path('m3/leaf/pom.xml').write_text('changed by fixture')\n" if mutate else "")
            + f"time.sleep({sleep})\nraise SystemExit({code})\n", encoding="utf-8")
        self.maven.chmod(0o755)

    def invoke(self, *, expected=None, timeout=10, environment=None):
        clean = {key: "" for key in ("MAVEN_ARGS", "MAVEN_OPTS", "JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS")}
        with mock.patch.dict(os.environ, {**clean, **(environment or {})}), \
                contextlib.redirect_stdout(io.StringIO()):
            code = producer.execute(self.root, self.out, expected or self.head,
                                    str(self.maven), timeout)
        manifest = json.loads((self.out / "evidence.json").read_text())
        return code, manifest, manifest["gates"][0]

    def assert_no_maven(self):
        self.assertFalse(self.calls.exists(), "refusal must precede all Maven subprocesses")

    def test_complete_command_and_bound_raw_artifacts(self):
        code, manifest, gate = self.invoke()
        self.assertEqual(0, code)
        self.assertEqual("PASS", gate["status"])
        self.assertEqual(self.head, manifest["source_commit"])
        self.assertEqual(self.git("rev-parse", "HEAD^{tree}"), manifest["source_tree"])
        calls = [json.loads(line) for line in self.calls.read_text().splitlines()]
        self.assertEqual([["-version"], list(producer.release.MAVEN_CONTROL_ARGS)], calls)
        self.assertEqual(producer.release.PRODUCT_REPOSITORY, gate["owner_repository"])
        self.assertEqual(producer.release.CANONICAL_RECIPE_REPOSITORY,
                         gate["canonical_recipe"]["repository"])
        self.assertTrue(gate["reactor"]["source_preserved"])
        self.assertIn("SYNTHETIC", (self.out / "maven.log").read_text())
        for artifact in gate["artifacts"]:
            self.assertEqual(artifact["sha256"], producer.release.sha256(self.out / artifact["path"]))
        self.assertNotIn("evidence.json", [item["path"] for item in gate["artifacts"]])
        # Exercise the actual producer/consumer packet boundary. This remains a
        # synthetic subprocess fixture, never proof of a Maven product build.
        producer.release.validate_maven_receipt(self.root, gate, self.out)

    def test_actual_cli_imports_do_not_create_source_bytecode(self):
        for name in ("run_maven_control_reactor.py", "verify_release_admission.py", "check_standalone.py"):
            self.write("m3/release/" + name, Path(__file__).with_name(name).read_text())
        self.head = self.commit()
        env = dict(os.environ)
        env.pop("PYTHONDONTWRITEBYTECODE", None)
        for name in ("MAVEN_ARGS", "MAVEN_OPTS", "JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS"):
            env.pop(name, None)
        result = subprocess.run(
            [sys.executable, str(self.root / "m3/release/run_maven_control_reactor.py"),
             "--root", str(self.root), "--out", str(self.out),
             "--expected-commit", self.head, "--maven", str(self.maven)],
            capture_output=True, text=True, env=env, timeout=20)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual("", self.git("status", "--porcelain=v1", "--untracked-files=all"))
        self.assertFalse(list(self.root.rglob("*.pyc")))

    def test_maven_failure_is_preserved(self):
        self.fake_maven(code=7)
        code, _, gate = self.invoke()
        self.assertEqual(7, code)
        self.assertEqual("FAIL", gate["status"])
        self.assertTrue(gate["reactor"]["maven_executed"])

    def test_timeout_is_never_success(self):
        self.fake_maven(sleep=15)
        code, _, gate = self.invoke(timeout=1)
        self.assertEqual(124, code)
        self.assertEqual("FAIL", gate["status"])
        self.assertIn("M3_MAVEN_CONTROL_TIMEOUT", (self.out / "maven.log").read_text())

    def test_source_mutation_after_zero_exit_is_refused(self):
        self.fake_maven(mutate=True)
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertEqual("FAIL", gate["status"])
        self.assertFalse(gate["reactor"]["source_preserved"])

    @unittest.skipUnless(Path("/proc").is_dir(), "process-state check requires Linux /proc")
    def test_timeout_kills_term_ignoring_child_after_leader_exits(self):
        pidfile = self.directory / "child.pid"
        self.maven.write_text(
            "#!/usr/bin/env python3\nimport pathlib, subprocess, sys, time\n"
            "if sys.argv[1:] == ['-version']:\n"
            "    print('Apache Maven 3.10.0\\nJava version: 21.0.13, vendor: SYNTHETIC FIXTURE')\n"
            "    raise SystemExit(0)\n"
            "child = subprocess.Popen([sys.executable, '-c', "
            "'import signal,time; signal.signal(signal.SIGTERM,signal.SIG_IGN); time.sleep(30)'])\n"
            f"pathlib.Path({str(pidfile)!r}).write_text(str(child.pid))\n"
            "time.sleep(30)\n", encoding="utf-8")
        code, _, gate = self.invoke(timeout=1)
        self.assertEqual(124, code)
        self.assertEqual("FAIL", gate["status"])
        pid = int(pidfile.read_text())
        # A killed orphan may briefly remain a zombie until the host reaps it.
        stat = Path(f"/proc/{pid}/stat")
        if stat.exists():
            self.assertIn(stat.read_text().rsplit(")", 1)[1].split()[0], ("Z", "X"))

    def test_stale_commit_is_refused_before_maven(self):
        code, _, gate = self.invoke(expected="b" * 40)
        self.assertNotEqual(0, code)
        self.assertEqual("HOLD", gate["status"])
        self.assert_no_maven()

    def test_dirty_worktree_is_refused_before_maven(self):
        self.write("m3/leaf/pom.xml", POM.format("<!-- changed -->"))
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertIn("dirty", gate["detail"])
        self.assert_no_maven()

    def test_sparse_checkout_is_refused(self):
        self.git("config", "core.sparseCheckout", "true")
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertIn("sparse", gate["detail"])
        self.assert_no_maven()

    def test_untracked_source_is_refused(self):
        self.write("m3/leaf/Untracked.java", "class Untracked {}\n")
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertIn("untracked", gate["detail"])
        self.assert_no_maven()

    def test_recipe_custody_hold_never_executes_maven(self):
        self.write("m3/tooling/migration-recipes/pom.xml", POM.format(""))
        self.head = self.commit()
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertIn("custody", gate["detail"])
        self.assert_no_maven()

    def test_absorb_in_place_custody_is_not_permission_to_execute_recipes(self):
        relative = "m3/tooling/migration-recipes/pom.xml"
        self.write(relative, POM.format(""))
        self.commit()
        blob = self.git("rev-parse", "HEAD:" + relative)
        self.write(producer.release.CUSTODY_LEDGER,
                   "\t".join(producer.release.CUSTODY_COLUMNS) + "\n" +
                   "\t".join((relative, blob, "100644", "ABSORB", relative,
                              "synthetic custody", "preserved source only")) + "\n")
        self.head = self.commit()
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertIn("retained executable recipe", gate["detail"])
        self.assert_no_maven()

    def test_rewrite_dependency_is_refused_even_in_an_inactive_profile(self):
        self.write("m3/pom.xml", POM.format('<profiles><profile><id>unused</id><build><plugins>'
                   '<plugin><groupId>org.openrewrite.maven</groupId><artifactId>rewrite-maven-plugin</artifactId>'
                   '</plugin></plugins></build></profile></profiles>'))
        self.head = self.commit()
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertIn("recipe/external", gate["detail"])
        self.assert_no_maven()

    def test_declared_profile_module_is_inspected(self):
        self.write("m3/pom.xml", POM.format('<profiles><profile><id>optional</id><modules>'
                   '<module>absent</module></modules></profile></profiles>'))
        self.head = self.commit()
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertIn("missing tracked", gate["detail"])
        self.assert_no_maven()

    def test_property_valued_plugin_coordinates_cannot_hide_rewrite(self):
        self.write("m3/pom.xml", POM.format('<properties><g>org.openrewrite.maven</g></properties>'
                   '<build><plugins><plugin><groupId>${g}</groupId><artifactId>hidden</artifactId>'
                   '</plugin></plugins></build>'))
        self.head = self.commit()
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertIn("unresolved Maven", gate["detail"])
        self.assert_no_maven()

    def test_project_coordinate_is_not_mistaken_for_external_dependency(self):
        self.write("m3/leaf/pom.xml", POM.format("").replace("<groupId>fixture</groupId>",
                   "<groupId>com.synexia.fixture</groupId>"))
        self.head = self.commit()
        code, _, _ = self.invoke()
        self.assertEqual(0, code)

    def test_maven_core_extension_cannot_run_unreviewed_recipe(self):
        self.write(".mvn/extensions.xml", '<extensions><extension><groupId>org.openrewrite</groupId>'
                   '<artifactId>extension</artifactId></extension></extensions>')
        self.head = self.commit()
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertIn("Maven core extension", gate["detail"])
        self.assert_no_maven()

    def test_ambient_jvm_options_cannot_redirect_maven_project(self):
        code, _, gate = self.invoke(environment={"MAVEN_OPTS": "-Dmaven.multiModuleProjectDirectory=/tmp/other"})
        self.assertNotEqual(0, code)
        self.assertIn("MAVEN_OPTS", gate["detail"])
        self.assert_no_maven()

    def test_ambient_maven_args_cannot_narrow_the_reactor(self):
        code, _, gate = self.invoke(environment={"MAVEN_ARGS": "-pl leaf -DskipTests"})
        self.assertNotEqual(0, code)
        self.assertIn("MAVEN_ARGS", gate["detail"])
        self.assert_no_maven()

    def test_repository_cli_configuration_cannot_narrow_the_reactor(self):
        self.write(".mvn/maven.config", "-N\n")
        self.head = self.commit()
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertIn("Maven CLI/JVM configuration", gate["detail"])
        self.assert_no_maven()

    def test_repository_jvm_configuration_cannot_redirect_extensions(self):
        self.write("m3/.mvn/jvm.config", "-Dmaven.ext.class.path=/tmp/other.jar\n")
        self.head = self.commit()
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertIn("Maven CLI/JVM configuration", gate["detail"])
        self.assert_no_maven()

    def test_mismatched_local_parent_cannot_fall_back_to_cached_external_parent(self):
        self.write("m3/leaf/pom.xml", POM.format('<parent><groupId>other</groupId>'
                   '<artifactId>different</artifactId><version>9</version><relativePath>../pom.xml</relativePath></parent>'))
        self.head = self.commit()
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertIn("parent identity differs", gate["detail"])
        self.assert_no_maven()

    def test_matched_local_parent_remains_in_full_preflight(self):
        self.write("m3/leaf/pom.xml", POM.format('<parent><groupId>fixture</groupId>'
                   '<artifactId>fixture-product</artifactId><version>1</version><relativePath>../pom.xml</relativePath></parent>'))
        self.head = self.commit()
        code, _, _ = self.invoke()
        self.assertEqual(0, code)

    def test_property_valued_parent_coordinates_are_refused(self):
        self.write("m3/leaf/pom.xml", POM.format('<parent><groupId>${g}</groupId>'
                   '<artifactId>fixture-product</artifactId><version>1</version><relativePath>../pom.xml</relativePath></parent>'))
        self.head = self.commit()
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertIn("unresolved Maven", gate["detail"])
        self.assert_no_maven()

    def test_skip_configuration_is_not_qualified(self):
        self.write("m3/leaf/pom.xml", POM.format('<build><plugins><plugin><artifactId>maven-surefire-plugin</artifactId>'
                   '<configuration><skipTests>true</skipTests></configuration></plugin></plugins></build>'))
        self.head = self.commit()
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertIn("skip configuration", gate["detail"])
        self.assert_no_maven()

    def test_wrong_java_toolchain_is_a_hold(self):
        self.fake_maven(java="25.0.1")
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertEqual("HOLD", gate["status"])
        self.assertFalse(gate["reactor"]["maven_executed"])
        self.assertEqual(1, len(self.calls.read_text().splitlines()))

    def test_unproven_recipe_origin_is_not_substituted(self):
        self.write(producer.ORIGIN_PATH, json.dumps({
            "repository": producer.release.PRODUCT_REPOSITORY,
            "commit": "a" * 40, "recipe_id": "synthetic.fixture.only"}))
        self.head = self.commit()
        code, _, gate = self.invoke()
        self.assertNotEqual(0, code)
        self.assertIn("canonical Synexia", gate["detail"])
        self.assert_no_maven()

    def test_existing_evidence_is_not_overwritten(self):
        self.out.mkdir()
        marker = self.out / "previous.log"
        marker.write_text("preserve me")
        with self.assertRaisesRegex(ValueError, "new or empty"):
            self.invoke()
        self.assertEqual("preserve me", marker.read_text())
        self.assert_no_maven()

    def test_evidence_cannot_be_written_inside_source(self):
        self.out = self.root / "target" / "evidence"
        with self.assertRaisesRegex(ValueError, "outside the source"):
            self.invoke()
        self.assert_no_maven()

    def test_workflow_produces_then_consumes_same_run_artifact(self):
        root = Path(__file__).resolve().parents[2]
        workflow = (root / ".github/workflows/m3-first-release-admission.yml").read_text()
        block = workflow.split("  control-reactor:\n", 1)[1].split("  release-proof-summary:", 1)[0]
        self.assertLess(block.index("run_maven_control_reactor.py"), block.index("module.validate("))
        self.assertIn('--expected-commit "$EXPECTED_SOURCE_COMMIT"', block)
        self.assertIn("EXPECTED_SOURCE_COMMIT: ${{ github.sha }}", block)
        self.assertIn("MAVEN_EVIDENCE: ${{ runner.temp }}/m3-maven-control/evidence.json", block)
        self.assertIn("evidence_path=Path(os.environ['MAVEN_EVIDENCE'])", block)
        self.assertNotIn("module.validate(Path('.'))", block)
        self.assertIn("Upload product Maven execution or refusal evidence\n        if: always()", block)


if __name__ == "__main__":
    unittest.main()
