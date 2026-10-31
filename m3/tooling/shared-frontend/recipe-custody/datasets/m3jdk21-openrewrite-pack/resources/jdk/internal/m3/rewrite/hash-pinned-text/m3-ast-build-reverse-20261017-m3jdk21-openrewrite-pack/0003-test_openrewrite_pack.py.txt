# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Actual full-runtime assembly, original-JAR, launcher and refusal contracts."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import openrewrite_pack
from tool_packs import digest, install_assets, load_tool_packs


class OpenRewritePackTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        required = ("M3_TEST_REPOSITORY", "M3_TEST_JDK", "M3_REWRITE_TOOL_PROOF",
                    "M3_REWRITE_RUNTIME_PROOF", "M3_REWRITE_TOOL_POM", "M3_REWRITE_RUNTIME_POM")
        if any(not os.environ.get(name) for name in required):
            raise unittest.SkipTest("Set the documented real M3/OpenRewrite proof paths; no synthetic runtime is substituted")
        cls.repository = Path(os.environ["M3_TEST_REPOSITORY"])
        cls.jdk = Path(os.environ["M3_TEST_JDK"])
        cls.tool = Path(os.environ["M3_REWRITE_TOOL_PROOF"])
        cls.runtime = Path(os.environ["M3_REWRITE_RUNTIME_PROOF"])
        cls.project_pom = Path(os.environ["M3_REWRITE_TOOL_POM"])
        cls.runtime_pom = Path(os.environ["M3_REWRITE_RUNTIME_POM"])
        cls.frontend_pom = cls.repository / "m3/tooling/shared-frontend/openrewrite/pom.xml"
        cls.maven = Path(os.environ.get("M3_TEST_MAVEN_REPOSITORY", str(Path.home() / ".m2/repository")))
        cls.temporary = tempfile.TemporaryDirectory(prefix="m3-rewrite-pack-")
        cls.addClassCleanup(cls.temporary.cleanup)
        cls.root = Path(cls.temporary.name)
        cls.destination = cls.root / "distribution with spaces"
        cls.lock = cls.root / "openrewrite-lock.json"
        cls.arguments = {
            "project_pom": cls.project_pom, "frontend_pom": cls.frontend_pom,
            "runtime_pom": cls.runtime_pom,
            "runner_jar": cls.tool / "m3-openrewrite-tool-1.0-SNAPSHOT.jar",
            "runtime_classpath": cls.runtime / "runtime-classpath.txt",
            "runtime_dependencies": cls.runtime / "runtime-dependencies.txt",
            "repository": cls.maven, "destination": cls.destination, "lock_path": cls.lock,
            "source": "Actual source-owned M3 runner and pinned complete upstream framework runtime",
            "source_licenses": [cls.repository / name for name in
                                ("LICENSE", "ADDITIONAL_LICENSE_INFO", "ASSEMBLY_EXCEPTION")],
            "source_proofs": [cls.tool / "QUALIFICATION_RECEIPT.json"],
        }
        cls.inventory = openrewrite_pack.assemble(**cls.arguments)
        cls.selection = load_tool_packs(cls.lock, ["openrewrite"])

    def changed_arguments(self, name):
        result = dict(self.arguments)
        result["destination"] = self.root / (name + "-distribution")
        result["lock_path"] = self.root / (name + "-lock.json")
        return result

    def test_complete_original_runtime_and_legal_bytes_are_preserved(self):
        originals = [Path(value) for value in self.arguments["runtime_classpath"].read_text().strip().split(os.pathsep)]
        self.assertEqual(len(originals), len(self.inventory["runtime_components"]))
        self.assertEqual(openrewrite_pack.FRAMEWORK_ARTIFACTS, set(self.inventory["framework_artifacts"]))
        self.assertEqual(len(originals) + 1, len(self.inventory["classpath"]))
        for original, component in zip(originals, self.inventory["runtime_components"]):
            retained = self.destination / component["jar"]
            self.assertEqual(digest(original), component["sha256"])
            self.assertEqual(original.read_bytes(), retained.read_bytes())
            with zipfile.ZipFile(retained) as archive:
                for notice in component["embedded_legal_entries"]:
                    data = archive.read(notice["path"])
                    self.assertEqual(len(data), notice["bytes"])
                    self.assertEqual(openrewrite_pack.hashlib.sha256(data).hexdigest(), notice["sha256"])
        for source in self.arguments["source_licenses"]:
            self.assertEqual(source.read_bytes(), (self.destination / "legal/m3" / source.name).read_bytes())
        self.assertEqual("NOT_ESTABLISHED_BY_ASSEMBLY", self.inventory["shared_ast_consumption"])

    def test_manifest_classpath_and_short_launchers_are_exact_and_relocatable(self):
        with zipfile.ZipFile(self.destination / "launch.jar") as archive:
            self.assertEqual(["META-INF/MANIFEST.MF"], archive.namelist())
            text = archive.read("META-INF/MANIFEST.MF").decode("ascii")
        unfolded = text.replace("\r\n ", "")
        attributes = dict(line.split(": ", 1) for line in unfolded.splitlines() if line)
        self.assertEqual(openrewrite_pack.MAIN, attributes["Main-Class"])
        self.assertEqual(self.inventory["classpath"], attributes["Class-Path"].split(" "))
        self.assertTrue(all(len(line) <= 70 for line in text.splitlines()))
        for suffix in ("", ".cmd"):
            text = (self.destination / ("m3-rewrite" + suffix)).read_text()
            self.assertIn("-jar", text)
            self.assertIn("launch.jar", text)
            self.assertNotIn(str(self.maven), text)
            self.assertLess(len(text), 4096)
        self.assertEqual(0o755, (self.destination / "m3-rewrite").stat().st_mode & 0o777)

    def test_real_complete_runtime_runs_typed_multifile_java_recipe_via_manifest(self):
        sources = self.root / "typed sources"
        sources.mkdir()
        api = sources / "Api.java"
        use = sources / "Use.java"
        before_api = "public class Api { public static String oldName(String x) { return x.trim(); } }\n"
        before_use = "class Use { String value = Api.oldName(\"x\"); }\n"
        api.write_text(before_api)
        use.write_text(before_use)
        yaml = self.root / "typed.yml"
        yaml.write_text("""type: specs.openrewrite.org/v1beta/recipe
name: example.Rename
displayName: Rename typed method
description: Exercise the packaged framework on the actual shared compiler units.
recipeList:
  - org.openrewrite.java.ChangeMethodName:
      methodPattern: 'Api oldName(java.lang.String)'
      newMethodName: renamed
      ignoreDefinition: false
""")
        patch = self.root / "typed.patch"
        arguments = [self.jdk / "bin/java", *self.inventory["compiler_exports"], "-jar",
                     self.destination / "launch.jar", "--source-root", sources,
                     "--source", "Api.java", "--source", "Use.java", "--recipe", "example.Rename",
                     "--recipe-config", yaml, "--patch", patch, "--", "-proc:none"]
        result = subprocess.run([str(value) for value in arguments], text=True, capture_output=True, timeout=60)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("String renamed(String x)", patch.read_text())
        self.assertIn("Api.renamed(", patch.read_text())
        self.assertEqual(before_api, api.read_text())
        self.assertEqual(before_use, use.read_text())
        self.assertIn("parsed=2 analyzed=2 inputLsts=2 conversions=2 sourceChecks=2 errors=0 completed=true", result.stderr)
        self.assertEqual("", result.stdout)

    def test_missing_framework_module_is_refused_even_if_both_maven_lists_omit_it(self):
        values = self.changed_arguments("missing")
        classpath = self.root / "missing-classpath.txt"
        classpath.write_text(os.pathsep.join(value for value in self.arguments["runtime_classpath"].read_text().strip().split(os.pathsep)
                                            if "/rewrite-protobuf/" not in value))
        dependencies = self.root / "missing-dependencies.txt"
        dependencies.write_text("\n".join(line for line in self.arguments["runtime_dependencies"].read_text().splitlines()
                                            if ":rewrite-protobuf:" not in line))
        values.update(runtime_classpath=classpath, runtime_dependencies=dependencies)
        with self.assertRaisesRegex(ValueError, "published production framework"):
            openrewrite_pack.assemble(**values)
        self.assertFalse(values["destination"].exists())
        self.assertFalse(values["lock_path"].exists())

    def test_coordinate_and_export_drift_are_refused_before_output(self):
        values = self.changed_arguments("coordinate")
        dependencies = self.root / "coordinate-dependencies.txt"
        dependencies.write_text(self.arguments["runtime_dependencies"].read_text().replace(
            "org.openrewrite:rewrite-json:jar:", "changed.owner:rewrite-json:jar:"))
        values["runtime_dependencies"] = dependencies
        with self.assertRaisesRegex(ValueError, "coordinate disagrees"):
            openrewrite_pack.assemble(**values)
        self.assertFalse(values["destination"].exists())
        values = self.changed_arguments("exports")
        project = self.root / "changed-project.xml"
        project.write_text(self.project_pom.read_text().replace(
            "--add-exports=jdk.compiler/com.sun.tools.javac.m3=ALL-UNNAMED ", ""))
        values["project_pom"] = project
        with self.assertRaisesRegex(ValueError, "exports disagree"):
            openrewrite_pack.assemble(**values)
        self.assertFalse(values["destination"].exists())

    def test_legal_omission_overwrite_and_asset_drift_are_refused(self):
        values = self.changed_arguments("legal")
        values["source_licenses"] = []
        with self.assertRaisesRegex(ValueError, "source LICENSE"):
            openrewrite_pack.assemble(**values)
        with self.assertRaises(FileExistsError):
            openrewrite_pack.assemble(**self.arguments)
        changed = json.loads(self.lock.read_text())
        changed["packs"][0]["assets"][0]["sha256"] = "0" * 64
        changed_lock = self.root / "drift-lock.json"
        changed_lock.write_text(json.dumps(changed))
        with self.assertRaisesRegex(ValueError, "checksum mismatch"):
            load_tool_packs(changed_lock, ["openrewrite"])

    def test_pom_coordinates_cannot_escape_retained_metadata_directories(self):
        for index, (field, value) in enumerate((("artifactId", ".."), ("version", "."),
                                               ("groupId", "org..escape"))):
            values = self.changed_arguments("traversal-%d" % index)
            root = openrewrite_pack.ET.fromstring(self.frontend_pom.read_bytes())
            root.find("m:" + field, openrewrite_pack.NS).text = value
            malicious = self.root / ("traversal-%d.xml" % index)
            malicious.write_bytes(openrewrite_pack.ET.tostring(root))
            values["frontend_pom"] = malicious
            with self.assertRaisesRegex(ValueError, "Invalid Maven"):
                openrewrite_pack.assemble(**values)
            self.assertFalse(values["destination"].exists())

    def test_input_rebuild_after_admission_cannot_publish_a_lock(self):
        values = self.changed_arguments("rebuild")
        classpath = self.root / "rebuild-classpath.txt"
        classpath.write_bytes(self.arguments["runtime_classpath"].read_bytes())
        values["runtime_classpath"] = classpath
        original_copy = openrewrite_pack.copy_pinned
        mutated = False

        def rebuild_during_copy(source, target, expected=None):
            nonlocal mutated
            if not mutated:
                classpath.write_text(classpath.read_text() + "\n")
                mutated = True
            return original_copy(source, target, expected)

        with patch.object(openrewrite_pack, "copy_pinned", side_effect=rebuild_during_copy):
            with self.assertRaisesRegex(ValueError, "changed after runtime admission"):
                openrewrite_pack.assemble(**values)
        self.assertTrue(mutated)
        self.assertFalse(values["lock_path"].exists())

    def test_real_pack_remains_explicit_and_installs_exact_assets(self):
        self.assertEqual((), load_tool_packs(None, []).names)
        with self.assertRaisesRegex(ValueError, "required together"):
            load_tool_packs(self.lock, [])
        image = self.root / "asset installation"
        image.mkdir()
        install_assets(self.selection, image)
        for asset in self.selection.assets:
            self.assertEqual(asset.sha256, digest(image / asset.target))
        self.assertEqual(0o755, (image / "bin/m3-rewrite").stat().st_mode & 0o777)
        self.assertIn("jdk.management", self.selection.roots)
        self.assertIn("jdk.zipfs", self.selection.roots)
        with self.assertRaises(FileExistsError):
            install_assets(self.selection, image)


if __name__ == "__main__":
    unittest.main(verbosity=2)
