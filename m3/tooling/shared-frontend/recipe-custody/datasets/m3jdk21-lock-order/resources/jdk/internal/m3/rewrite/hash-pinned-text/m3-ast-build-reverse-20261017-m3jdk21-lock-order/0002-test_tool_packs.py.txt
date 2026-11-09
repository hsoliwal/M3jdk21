# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Lock/refusal contracts and real Java 21 JMOD/jlink optional-tool execution."""
import copy
import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import tool_packs

OWNER = Path(__file__).resolve().parent
STAMP = "2026-01-01T00:00:00Z"


def execute(arguments, check=True, timeout=180):
    environment = os.environ.copy()
    for name in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"):
        environment.pop(name, None)
    result = subprocess.run([str(value) for value in arguments], text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            env=environment, timeout=timeout)
    if check and result.returncode:
        raise AssertionError("Command failed: " + str(arguments) + "\n" + result.stdout)
    return result


def java21():
    value = os.environ.get("M3_TEST_JDK", os.environ.get("JAVA_HOME"))
    if not value:
        executable = shutil.which("javac")
        value = str(Path(executable).resolve().parent.parent) if executable else ""
    home = Path(value)
    if not value or not (home / "bin/jmod").is_file() or not (home / "release").is_file():
        raise unittest.SkipTest("Real JMOD tests require M3_TEST_JDK pointing to Java 21")
    import build_pack
    try:
        return build_pack.java_home(home)
    except ValueError as error:
        raise unittest.SkipTest(str(error)) from error


class ToolPackLockTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.input = self.root / "input.txt"
        self.input.write_text("retained distribution asset\n")
        self.lock_path = self.root / "tools.json"
        self.lock = {"schema_version": 1, "packs": [{
            "name": "maven", "version": "fixture", "source": "test fixture", "license": "Apache-2.0",
            "roots": ["jdk.compiler"], "modules": [], "assets": [{
                "file": "input.txt", "sha256": tool_packs.digest(self.input),
                "target": "lib/m3-tools/maven/input.txt", "executable": False,
            }],
        }]}

    def load(self, selections=("maven",)):
        self.lock_path.write_text(json.dumps(self.lock))
        return tool_packs.load_tool_packs(self.lock_path, selections)

    def test_default_is_empty(self):
        self.assertEqual(tool_packs.ToolPacks(), tool_packs.load_tool_packs(None, []))

    def test_lock_never_selects_everything_implicitly(self):
        with self.assertRaisesRegex(ValueError, "required together"):
            self.load(())
        with self.assertRaisesRegex(ValueError, "required together"):
            tool_packs.load_tool_packs(None, ["maven"])

    def test_unknown_selection_and_duplicate_pack_refused(self):
        with self.assertRaisesRegex(ValueError, "Unknown tool pack"):
            self.load(("gradle",))
        self.lock["packs"].append(copy.deepcopy(self.lock["packs"][0]))
        with self.assertRaisesRegex(ValueError, "Duplicate tool pack"):
            self.load()

    def test_unselected_pack_may_have_unavailable_payload(self):
        self.lock["packs"].append({"name": "gradle", "assets": [{"file": "absent"}]})
        self.assertEqual(("maven",), self.load().names)

    def test_selection_order_and_repetition_are_deterministic(self):
        second = copy.deepcopy(self.lock["packs"][0])
        second["name"] = "ant"
        self.lock["packs"].append(second)
        first = self.load(("maven", "ant", "maven"))
        self.assertEqual(first, self.load(("ant", "maven")))
        self.assertEqual(1, len(first.assets))

    def test_schema_boolean_is_not_version_one(self):
        self.lock["schema_version"] = True
        with self.assertRaisesRegex(ValueError, "schema_version"):
            self.load()

    def test_provenance_and_explicit_roots_required(self):
        for field in ("version", "source", "license"):
            with self.subTest(field=field):
                old = self.lock["packs"][0][field]
                self.lock["packs"][0][field] = ""
                with self.assertRaisesRegex(ValueError, "provenance"):
                    self.load()
                self.lock["packs"][0][field] = old
        self.lock["packs"][0]["roots"] = []
        with self.assertRaisesRegex(ValueError, "explicit module roots"):
            self.load()

    def test_checksum_mismatch_and_missing_file_refused(self):
        self.input.write_text("changed")
        with self.assertRaisesRegex(ValueError, "checksum mismatch"):
            self.load()
        self.input.unlink()
        with self.assertRaises(FileNotFoundError):
            self.load()

    def test_traversal_absolute_and_ambiguous_targets_refused(self):
        for target in ("../outside", "/tmp/outside", "bin/../outside", "lib//x", "bin/x/",
                       "bin/C:drive", "bin\\outside", "bin/./outside", "bin/line\nfeed", "other/file"):
            with self.subTest(target=target):
                self.lock["packs"][0]["assets"][0]["target"] = target
                with self.assertRaises(ValueError):
                    self.load()

    def test_asset_mode_must_be_boolean(self):
        self.lock["packs"][0]["assets"][0]["executable"] = "yes"
        with self.assertRaisesRegex(ValueError, "boolean"):
            self.load()

    def test_asset_file_directory_conflict_refused(self):
        asset = copy.deepcopy(self.lock["packs"][0]["assets"][0])
        asset["target"] += "/child"
        self.lock["packs"][0]["assets"].append(asset)
        with self.assertRaisesRegex(ValueError, "file/directory collision"):
            self.load()

    def test_conflicting_pack_assets_refused(self):
        second = copy.deepcopy(self.lock["packs"][0])
        second["name"] = "ant"
        second["assets"][0]["executable"] = True
        self.lock["packs"].append(second)
        with self.assertRaisesRegex(ValueError, "Conflicting tool asset"):
            self.load(("maven", "ant"))

    def test_distribution_inventory_refuses_omission_or_changed_identity(self):
        import hashlib
        asset = self.lock["packs"][0]["assets"][0]
        identity = "input.txt\t" + asset["sha256"] + "\t644\n"
        self.lock["packs"][0]["distribution"] = {
            "file_count": 1, "tree_sha256": hashlib.sha256(identity.encode()).hexdigest()}
        self.assertEqual(1, self.load().provenance[0]["distribution"]["file_count"])
        self.lock["packs"][0]["assets"] = []
        with self.assertRaisesRegex(ValueError, "distribution inventory mismatch"):
            self.load()

    def test_generated_distribution_inventory_accepts_nested_prefix_paths(self):
        import lock_tool_distribution
        layouts = (
            ("lib/jline-native-4.4.8.jar", "lib/jline-native.license",
             "lib/jline-native/FreeBSD/x86/libjlinenative.so"),
            ("lib/jline-native/Linux-musl/x86/libjlinenative.so",
             "lib/jline-native/Linux/x86/libjlinenative.so"),
        )
        for index, layout in enumerate(layouts):
            with self.subTest(layout=layout):
                distribution = self.root / ("distribution-" + str(index))
                paths = tuple(lock_tool_distribution.REQUIRED["maven"]) + (
                    "boot/plexus-classworlds-fixture.jar", "lib/maven-core-fixture.jar") + layout
                for relative in paths:
                    target = distribution / relative
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_text("Inventory fixture only: " + relative + "\n")
                (distribution / "bin/mvn").chmod(0o755)
                pack = lock_tool_distribution.distribution_pack(
                    "maven", distribution, "fixture", "inventory regression fixture", "Apache-2.0")
                self.lock = {"schema_version": 1, "packs": [pack]}
                selection = self.load()
                self.assertEqual(len(paths), selection.provenance[0]["distribution"]["file_count"])
                self.assertEqual(pack["distribution"]["tree_sha256"],
                                 selection.provenance[0]["distribution"]["tree_sha256"])
                # JSON asset order is not part of the distribution's identity.
                pack["assets"].reverse()
                reordered = self.load()
                self.assertEqual(selection.assets, reordered.assets)
                self.assertEqual(selection.provenance, reordered.provenance)

    def test_duplicate_and_misnamed_locked_modules_refused(self):
        archive = self.root / "com.m3test.tool.jmod"
        archive.write_bytes(b"LOCK CONTRACT FIXTURE, NOT JMOD ADMISSION")
        module = {"module": "com.m3test.tool", "file": archive.name,
                  "sha256": tool_packs.digest(archive)}
        self.lock["packs"][0]["modules"] = [module, copy.deepcopy(module)]
        with self.assertRaisesRegex(ValueError, "Duplicate module within"):
            self.load()
        self.lock["packs"][0]["modules"] = [module]
        module["module"] = "com.m3test.wrong"
        with self.assertRaisesRegex(ValueError, "filename mismatch"):
            self.load()

    def test_image_collision_precedes_all_copying(self):
        selection = self.load()
        image = self.root / "image"
        target = image / selection.assets[0].target
        target.parent.mkdir(parents=True)
        target.write_text("existing image asset")
        with self.assertRaises(FileExistsError):
            tool_packs.install_assets(selection, image)
        self.assertEqual("existing image asset", target.read_text())

    def test_symlink_parent_refused(self):
        selection = self.load()
        image = self.root / "image"
        image.mkdir()
        outside = self.root / "outside"
        outside.mkdir()
        (image / "lib").symlink_to(outside, target_is_directory=True)
        with self.assertRaisesRegex(ValueError, "Unsafe existing"):
            tool_packs.install_assets(selection, image)
        self.assertEqual([], list(outside.iterdir()))

    def test_drift_after_lock_and_during_copy_refused(self):
        selection = self.load()
        image = self.root / "image"
        image.mkdir()
        with patch.object(tool_packs, "digest", return_value="0" * 64):
            with self.assertRaisesRegex(ValueError, "drift before installation"):
                tool_packs.install_assets(selection, image)
        checksum = selection.assets[0].sha256
        with patch.object(tool_packs, "digest", side_effect=[checksum, checksum, "0" * 64]):
            with self.assertRaisesRegex(ValueError, "during copy"):
                tool_packs._copy(self.input, image / "copy.txt", checksum)

    def test_installed_bytes_modes_and_honest_receipt(self):
        self.lock["packs"][0]["assets"][0]["executable"] = True
        selection = self.load()
        image = self.root / "image"
        image.mkdir()
        tool_packs.install_assets(selection, image)
        installed = image / selection.assets[0].target
        self.assertEqual(self.input.read_bytes(), installed.read_bytes())
        self.assertEqual(0o755, installed.stat().st_mode & 0o777)
        self.assertEqual("NOT_ESTABLISHED_BY_PACKAGING", selection.receipt({})["shared_ast_consumption"])


class RealToolPackTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.jdk = java21()
        cls.directory = tempfile.TemporaryDirectory(prefix="m3-real-tool-packs-")
        cls.addClassCleanup(cls.directory.cleanup)
        cls.root = Path(cls.directory.name)
        cls.archives = cls.root / "archives"
        cls.archives.mkdir()
        cls.classes = {}
        cls.create_module("com.m3test.shared", "module com.m3test.shared { exports com.m3test.shared; }", {
            "com/m3test/shared/Shared.java": '''package com.m3test.shared;
public final class Shared {
    private Shared() {}
    public static String value() { return "shared"; }
    public static void main(String[] args) { System.out.println("M3_SHARED_PASS"); }
}''',
            "com/m3test/shared/ToolService.java": "package com.m3test.shared; public interface ToolService { String name(); }",
        })
        cls.create_module("com.m3test.optional", "module com.m3test.optional {}", {})
        cls.create_module("com.m3test.unselected", "module com.m3test.unselected {}", {})
        cls.create_module("com.m3test.provider", '''module com.m3test.provider {
requires com.m3test.shared;
provides com.m3test.shared.ToolService with com.m3test.provider.Provider;
}''', {"com/m3test/provider/Provider.java": '''package com.m3test.provider;
public final class Provider implements com.m3test.shared.ToolService {
    public Provider() {}
    public String name() { return "provider"; }
}'''})
        cls.create_module("com.m3test.tool", '''module com.m3test.tool {
requires com.m3test.shared;
requires static com.m3test.optional;
uses com.m3test.shared.ToolService;
}''', {"com/m3test/tool/Main.java": '''package com.m3test.tool;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceLoader;
public final class Main {
    private Main() {}
    public static void main(String[] args) throws Exception {
        Path home = Path.of(System.getProperty("java.home"));
        String config = Files.readString(home.resolve("conf/m3-tools/fixture.properties")).trim();
        String payload = Files.readString(home.resolve("lib/m3fixture/payload.txt")).trim();
        String resource;
        try (var input = Main.class.getResourceAsStream("/fixture.txt")) {
            resource = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
        long providers = ServiceLoader.load(com.m3test.shared.ToolService.class).stream().count();
        System.out.println("M3_TOOL_PASS|" + com.m3test.shared.Shared.value()
            + "|" + config + "|" + payload + "|" + resource + "|providers=" + providers);
    }
}'''}, assets=True)
        wrapper = cls.root / "m3-extra"
        wrapper.write_text('#!/bin/sh\nbase=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)\nexec "$base/m3-fixture" "$@"\n')
        cls.lock = {"schema_version": 1, "packs": [{
            "name": "fixture", "version": "1", "source": "real Java21 test compilation", "license": "Apache-2.0",
            "roots": ["com.m3test.tool", "com.m3test.provider"],
            "modules": [{"module": path.stem, "file": str(path), "sha256": tool_packs.digest(path)}
                        for path in sorted(cls.archives.glob("*.jmod"))],
            "assets": [{"file": str(wrapper), "target": "bin/m3-extra", "executable": True,
                        "sha256": tool_packs.digest(wrapper)}],
        }, {"name": "unavailable", "assets": [{"file": "not-present"}]}]}
        cls.lock_path = cls.root / "tool-lock.json"
        cls.lock_path.write_text(json.dumps(cls.lock))
        cls.output = cls.root / "selected"
        cls.build_result = execute([sys.executable, OWNER / "build_pack.py", "--jdk", cls.jdk,
                                    "--out", cls.output, "--tool-pack-lock", cls.lock_path,
                                    "--tool-pack", "fixture"], timeout=240)

    @classmethod
    def create_module(cls, module, descriptor, sources, assets=False):
        source = cls.root / "sources" / module
        source.mkdir(parents=True)
        (source / "module-info.java").write_text(descriptor)
        for path, content in sources.items():
            target = source / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(content)
        classes = cls.root / "classes" / module
        module_path = os.pathsep.join(str(path) for path in cls.classes.values())
        options = ["--module-path", module_path] if module_path else []
        execute([cls.jdk / "bin/javac", "--release", "21", "-Xlint:all", "-Werror",
                 *options, "-d", classes, *sorted(source.rglob("*.java"))])
        cls.classes[module] = classes
        extra = []
        if assets:
            (classes / "fixture.txt").write_text("resource\n")
            commands = cls.root / "cmds"
            commands.mkdir()
            launcher = commands / "m3-fixture"
            launcher.write_text('#!/bin/sh\nbase=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)\nexec "$base/java" -m com.m3test.tool/com.m3test.tool.Main "$@"\n')
            launcher.chmod(0o755)
            config = cls.root / "config"
            (config / "m3-tools").mkdir(parents=True)
            (config / "m3-tools/fixture.properties").write_text("configuration\n")
            libraries = cls.root / "libs"
            (libraries / "m3fixture").mkdir(parents=True)
            (libraries / "m3fixture/payload.txt").write_text("payload\n")
            legal = cls.root / "legal"
            legal.mkdir()
            (legal / "NOTICE").write_text("Copyright 2026 Hitesh Soliwal and contributors\n")
            extra = ["--cmds", commands, "--config", config, "--libs", libraries, "--legal-notices", legal]
        execute([cls.jdk / "bin/jar", "--create", "--file", cls.archives / (module + ".jar"),
                 "--date", STAMP, "-C", classes, "."])
        execute([cls.jdk / "bin/jmod", "create", "--date", STAMP, "--class-path", classes,
                 *extra, cls.archives / (module + ".jmod")])

    def test_actual_dependency_closure_and_explicit_service_provider(self):
        modules = execute([self.output / "image/bin/java", "--list-modules"]).stdout
        for module in ("com.m3test.tool", "com.m3test.shared", "com.m3test.provider"):
            self.assertIn(module, modules)
            self.assertTrue((self.output / "jmods" / (module + ".jmod")).is_file())
        for module in ("com.m3test.optional", "com.m3test.unselected"):
            self.assertNotIn(module, modules)
            self.assertFalse((self.output / "jmods" / (module + ".jmod")).exists())

    def test_real_launcher_config_resources_and_retained_executable(self):
        expected = "M3_TOOL_PASS|shared|configuration|payload|resource|providers=1"
        for command in ("m3-fixture", "m3-extra"):
            self.assertIn(expected, execute([self.output / "image/bin" / command]).stdout)
        self.assertTrue((self.output / "image/legal/com.m3test.tool/NOTICE").is_file())

    def test_receipt_distinguishes_packaging_from_tool_acceptance(self):
        receipt = json.loads((self.output / "RECEIPT.json").read_text())
        self.assertFalse(receipt["m3jdk21_product_acceptance"])
        self.assertEqual(["fixture"], receipt["tool_packs"]["packs"])
        self.assertEqual("NOT_ESTABLISHED_BY_PACKAGING", receipt["tool_packs"]["shared_ast_consumption"])
        self.assertIn("M3_MODULE_PACK_PASS", self.build_result.stdout)

    def test_missing_required_module_refused_without_success_receipt(self):
        lock = copy.deepcopy(self.lock)
        lock["packs"][0]["modules"] = [row for row in lock["packs"][0]["modules"]
                                          if row["module"] != "com.m3test.shared"]
        path = self.root / "missing-lock.json"
        path.write_text(json.dumps(lock))
        output = self.root / "missing-output"
        result = execute([sys.executable, OWNER / "build_pack.py", "--jdk", self.jdk,
                          "--out", output, "--tool-pack-lock", path,
                          "--tool-pack", "fixture"], check=False)
        self.assertNotEqual(0, result.returncode)
        self.assertFalse((output / "RECEIPT.json").exists())
        self.assertIn("MISSING_MODULE: com.m3test.shared", (output / "MODULES.tsv").read_text())

    def pack_jars(self, output, selected):
        image_tsv = self.root / "input-image.tsv"
        image_tsv.write_text("artifact\tversion\tjar\tmodule\trequires\tdisposition\n" + "".join(
            "\t".join((path.stem, "1", str(path), path.stem, "", "JMOD")) + "\n"
            for path in sorted(self.archives.glob("*.jar"))))
        manifest = self.root / "input-descriptors.tsv"
        manifest.write_text("module\tunit_jars\ttarget\tsha256\n")
        arguments = [sys.executable, OWNER.parent / "jmod-recipes/tools/jmod_pack.py",
                     "--jdk-home", self.jdk, "--image-tsv", image_tsv, "--manifest", manifest,
                     "--layer-root", self.root, "--out", output, "--add-modules", "java.base"]
        if selected:
            arguments += ["--root-module", "com.m3test.tool", "--root-module", "com.m3test.provider"]
        execute(arguments, timeout=240)

    def test_existing_jmod_packer_selects_runtime_roots(self):
        output = self.root / "jmod-selected"
        self.pack_jars(output, True)
        modules = execute([output / "image/bin/java", "--list-modules"]).stdout
        for module in ("com.m3test.tool", "com.m3test.shared", "com.m3test.provider"):
            self.assertIn(module, modules)
        for module in ("com.m3test.optional", "com.m3test.unselected"):
            self.assertNotIn(module, modules)
            self.assertFalse((output / "jmods" / (module + ".jmod")).exists())
        self.assertIn("M3_SHARED_PASS", execute([output / "image/bin/java", "-m",
                                               "com.m3test.shared/com.m3test.shared.Shared"]).stdout)
        receipt = json.loads((output / "SELECTION.json").read_text())
        self.assertEqual(3, len(receipt["selected_vendored_modules"]))

    def test_existing_jmod_packer_default_still_links_all_supplied_modules(self):
        output = self.root / "jmod-default"
        self.pack_jars(output, False)
        modules = execute([output / "image/bin/java", "--list-modules"]).stdout
        for module in self.classes:
            self.assertIn(module, modules)
        self.assertFalse((output / "SELECTION.json").exists())


if __name__ == "__main__":
    unittest.main()
