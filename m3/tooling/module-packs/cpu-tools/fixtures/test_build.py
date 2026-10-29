#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
"""Independent build admission and real subprocess timeout regressions."""

import importlib.util
import json
import os
import shutil
import sys
import tempfile
import time
import unittest
import warnings
import zipfile
from pathlib import Path

spec = importlib.util.spec_from_file_location("cpu_tools_build", Path(__file__).resolve().parents[1] / "build.py")
build = importlib.util.module_from_spec(spec)
spec.loader.exec_module(build)


class BuildContracts(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix="m3-cpu-build-contract-")
        self.root = Path(self.directory.name).resolve()

    def tearDown(self):
        self.directory.cleanup()

    def test_fresh_output_preserves_existing_content(self):
        output = self.root / "proof"
        output.mkdir()
        evidence = output / "prior-proof.txt"
        evidence.write_text("retained", encoding="utf-8")
        with self.assertRaises(FileExistsError):
            build.fresh_output(output, ())
        self.assertEqual("retained", evidence.read_text(encoding="utf-8"))

    def test_output_cannot_enter_jdk_or_source(self):
        source = self.root / "source"
        source.mkdir()
        with self.assertRaises(ValueError):
            build.fresh_output(source / "nested/proof", (source,))
        self.assertFalse((source / "nested").exists())
        self.assertEqual(self.root / "proof", build.fresh_output(self.root / "proof", (source,)))

    def test_symlink_output_components_refused(self):
        source = self.root / "source"
        source.mkdir()
        link = self.root / "indirect"
        link.symlink_to(source, target_is_directory=True)
        with self.assertRaises(ValueError):
            build.fresh_output(link / "proof", ())
        broken = self.root / "broken"
        broken.symlink_to(self.root / "missing")
        with self.assertRaises(FileExistsError):
            build.fresh_output(broken, ())

    def source_jdk(self, modules="java.base jdk.compiler"):
        jdk = self.root / "jdk"
        (jdk / "jmods").mkdir(parents=True)
        (jdk / "release").write_text('JAVA_VERSION="21"\nMODULES="' + modules + '"\n', encoding="utf-8")
        (jdk / "jmods/java.base.jmod").write_bytes(b"baseline witness")
        return jdk

    def test_source_jdk_without_optional_collisions_is_admitted(self):
        jdk = self.source_jdk()
        self.assertEqual(("java.base", "jdk.compiler"), build.source_jdk_modules(jdk))
        self.assertFalse((self.root / "output").exists())

    def test_every_admitted_stale_jmod_is_refused_before_output(self):
        jdk = self.source_jdk()
        output = self.root / "output"
        for module in build.MODULES:
            with self.subTest(module=module):
                stale = jdk / "jmods" / (module + ".jmod")
                stale.write_bytes(b"stale optional module must never win jlink resolution")
                with self.assertRaisesRegex(ValueError, "already contains admitted tool modules"):
                    build.source_jdk_modules(jdk)
                    output.mkdir()  # Mirrors qualify: output creation follows this admission.
                self.assertFalse(output.exists())
                self.assertTrue(stale.is_file(), "admission must not mutate the supplied JDK")
                stale.unlink()

    def test_linked_release_collision_refuses_even_without_jmod_file(self):
        jdk = self.source_jdk("java.base jdk.compiler com.m3.tools.cpu")
        self.assertFalse((jdk / "jmods/com.m3.tools.cpu.jmod").exists())
        with self.assertRaisesRegex(ValueError, "com.m3.tools.cpu"):
            build.source_jdk_modules(jdk)

    def test_source_jdk_missing_or_ambiguous_module_inventory_refuses(self):
        jdk = self.source_jdk()
        for declaration in ('JAVA_VERSION="21"\n', 'MODULES="java.base"\nMODULES="jdk.compiler"\n',
                            'MODULES="java.base java.base"\n', 'MODULES=""\n'):
            with self.subTest(declaration=declaration):
                (jdk / "release").write_text(declaration, encoding="utf-8")
                with self.assertRaises(ValueError):
                    build.source_jdk_modules(jdk)

    def module(self, name="example.cpu"):
        source = self.root / "src"
        owner = source / name
        owner.mkdir(parents=True)
        (owner / "module-info.java").write_text("// source comment\nmodule " + name + " { }\n", encoding="utf-8")
        return source, owner

    def test_module_inventory_and_descriptor_drift_refused(self):
        source, owner = self.module()
        self.assertEqual(1, len(build.module_sources(source, ("example.cpu",))))
        with self.assertRaises(ValueError):
            build.module_sources(source, ("another.cpu",))
        (owner / "module-info.java").write_text("module wrong.cpu {}", encoding="utf-8")
        with self.assertRaises(ValueError):
            build.module_sources(source, ("example.cpu",))

    def test_precompiled_source_payload_refused(self):
        source, owner = self.module()
        (owner / "Old.class").write_bytes(b"\xca\xfe\xba\xbe")
        with self.assertRaises(ValueError):
            build.module_sources(source, ("example.cpu",))

    def test_symlink_module_owner_refused(self):
        source, owner = self.module()
        relocated = self.root / "elsewhere"
        owner.rename(relocated)
        owner.symlink_to(relocated, target_is_directory=True)
        with self.assertRaises(ValueError):
            build.module_sources(source, ("example.cpu",))

    def commands(self):
        output = self.root / "output"
        (output / "logs").mkdir(parents=True)
        return build.Commands(output, self.root, self.root / "jdk")

    def test_commands_pass_arguments_without_a_shell(self):
        commands = self.commands()
        literal = "value;$(false) with spaces"
        result = commands.run("literal", sys.executable, "-c", "import sys; print(sys.argv[1])", literal)
        self.assertEqual(literal + "\n", result)
        self.assertEqual("PASS", commands.rows[0]["status"])
        self.assertEqual(64, len(commands.rows[0]["log_sha256"]))

    def test_java_tmp_override_quotes_real_path_and_does_not_leak(self):
        commands = self.commands()
        before = dict(commands.environment)
        java_home = before.get("JAVA_HOME")
        java_from_home = Path(java_home) / "bin" / "java" if java_home else None
        java = (str(java_from_home) if java_from_home is not None
                and java_from_home.is_file() and os.access(java_from_home, os.X_OK)
                else shutil.which("java", path=before.get("PATH", os.defpath)))
        self.assertIsNotNone(java, "A source-file-capable Java is required for the real probe")
        staging = self.root / 'java temp \'single\' "double" -Dm3.injected=true'
        staging.mkdir()
        source = self.root / "JavaTmpProbe.java"
        source.write_text("""import java.nio.file.Files;
import java.nio.file.Path;
public class JavaTmpProbe {
    public static void main(String[] args) throws Exception {
        if (System.getProperty("m3.injected") != null)
            throw new AssertionError("injected property escaped the quoted path");
        if (!args[2].equals(System.getenv("PATH")))
            throw new AssertionError("PATH changed");
        String actual = System.getProperty("java.io.tmpdir");
        if ("override".equals(args[0])) {
            if (!args[1].equals(actual)) throw new AssertionError("temporary property differs: " + actual);
            Path file = Files.createTempFile("m3-java-tmp-", ".probe");
            try {
                if (!file.getParent().toRealPath().equals(Path.of(args[1]).toRealPath()))
                    throw new AssertionError("real temporary file used the wrong directory");
            } finally { Files.delete(file); }
            System.out.println("JAVA_TMP_OVERRIDE_PASS");
        } else {
            if (System.getenv("JAVA_TOOL_OPTIONS") != null)
                throw new AssertionError("per-command environment leaked");
            if (args[1].equals(actual)) throw new AssertionError("temporary override leaked");
            System.out.println("JAVA_TMP_NORMAL_PASS");
        }
    }
}
""", encoding="utf-8")
        result = commands.run("java-tmp-override", java, source,
                              "override", str(staging), before.get("PATH", ""), java_tmp_dir=staging)
        self.assertIn("JAVA_TMP_OVERRIDE_PASS", result)
        self.assertEqual(commands.normalized(staging), commands.rows[0]["java_tmp_dir"])
        self.assertEqual(before, commands.environment)
        normal = commands.run("java-tmp-normal", java, source,
                              "normal", str(staging), before.get("PATH", ""))
        self.assertIn("JAVA_TMP_NORMAL_PASS", normal)
        self.assertNotIn("java_tmp_dir", commands.rows[1])
        self.assertEqual(before, commands.environment)

    def test_jmod_legal_readback_rejects_missing_or_changed_notice(self):
        notice = self.root / "NOTICE.txt"
        notice.write_text("reviewed donor notice", encoding="utf-8")
        expected = {"cpu-tools/NOTICE.txt": build.sha256(notice)}
        archive = self.root / "fixture.jmod"
        archive.write_bytes(b"JM\x01\x00")
        with zipfile.ZipFile(archive, "a") as zipped:
            zipped.writestr("legal/cpu-tools/NOTICE.txt", notice.read_bytes())
        build.verify_jmod_legal(archive, expected)
        with self.assertRaisesRegex(ValueError, "missing or duplicate"):
            build.verify_jmod_legal(archive, {"text-tools/NOTICE.txt": build.sha256(notice)})
        with self.assertRaisesRegex(ValueError, "bytes differ"):
            build.verify_jmod_legal(archive, {"cpu-tools/NOTICE.txt": "0" * 64})

    def test_linked_legal_readback_allows_internal_dedup_and_refuses_escape(self):
        image = self.root / "image"
        shared = image / "legal/first/cpu-tools/NOTICE.txt"
        shared.parent.mkdir(parents=True)
        shared.write_text("reviewed notice", encoding="utf-8")
        linked = image / "legal/second/cpu-tools/NOTICE.txt"
        linked.parent.mkdir(parents=True)
        linked.symlink_to("../../first/cpu-tools/NOTICE.txt")
        expected = {"cpu-tools/NOTICE.txt": build.sha256(shared)}
        build.verify_image_legal(image, "second", expected)
        shared.write_text("changed notice", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "bytes differ"):
            build.verify_image_legal(image, "second", expected)
        linked.unlink()
        external = self.root / "external-notice"
        external.write_text("reviewed notice", encoding="utf-8")
        linked.symlink_to(external)
        with self.assertRaisesRegex(ValueError, "escapes image"):
            build.verify_image_legal(image, "second", expected)

    def native_archive(self, name, members):
        path = self.root / name
        path.write_bytes(b"JM\x01\x00")
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(path, "a") as archive:
                for member, value in members:
                    archive.writestr(member, value)
        return path

    def test_native_jmod_missing_duplicate_and_changed_bytes_refuse(self):
        original = self.root / "original.so"
        original.write_bytes(b"exact compiled JNI owner")
        expected = build.sha256(original)
        member = "lib/libm3textio.so"
        valid = self.native_archive("valid.jmod", [(member, original.read_bytes())])
        self.assertEqual(expected, build.verify_jmod_native(valid, expected))
        cases = (
            ("missing.jmod", [("lib/other.so", original.read_bytes())], "missing or duplicate"),
            ("duplicate.jmod", [(member, original.read_bytes()), (member, original.read_bytes())], "missing or duplicate"),
            ("changed.jmod", [(member, b"different JNI code")], "bytes differ"),
        )
        for name, members, message in cases:
            with self.subTest(name=name):
                with self.assertRaisesRegex(ValueError, message):
                    build.verify_jmod_native(self.native_archive(name, members), expected)

    def test_native_text_binding_retains_exact_member_and_drift_refusals(self):
        original = self.root / "text-owner.so"
        original.write_bytes(b"exact independent compiled native text owner")
        expected = build.sha256(original)
        member = "lib/libm3textops.so"
        valid = self.native_archive("valid-text.jmod", [(member, original.read_bytes())])
        self.assertEqual(expected, build.verify_jmod_native(valid, expected, member))
        with self.assertRaisesRegex(ValueError, "missing or duplicate"):
            build.verify_jmod_native(valid, expected)
        cases = (
            ("text-missing.jmod", [], "missing or duplicate"),
            ("text-wrong-owner.jmod", [("lib/libm3textio.so", original.read_bytes())], "missing or duplicate"),
            ("text-duplicate.jmod", [(member, original.read_bytes()), (member, original.read_bytes())], "missing or duplicate"),
            ("text-changed.jmod", [(member, b"different compiled owner")], "bytes differ"),
        )
        for name, members, message in cases:
            with self.subTest(name=name):
                with self.assertRaisesRegex(ValueError, message):
                    build.verify_jmod_native(self.native_archive(name, members), expected, member)

    @unittest.skipUnless(sys.platform.startswith("linux") and shutil.which("cc") and shutil.which("objcopy"),
                         "Linux C compiler and objcopy are required for real ELF provenance")
    def test_real_elf_stripping_accepts_expected_and_refuses_unstripped_or_drift(self):
        commands = self.commands()
        path = commands.environment.get("PATH", os.defpath)
        compiler = build.native_tool_identity("cc", path, self.root)
        objcopy = build.native_tool_identity("objcopy", path, self.root)
        source = self.root / "native-witness.c"
        source.write_text("int m3_native_witness(int value) { return value + 17; }\n", encoding="utf-8")
        original = self.root / "original.so"
        expected = self.root / "expected.so"
        linked = self.root / "linked.so"
        commands.run("native-witness-compile", compiler["resolved"], "-shared", "-fPIC", "-g",
                     "-o", original, source)
        original_hash = build.sha256(original)
        archive = self.native_archive("real-native.jmod", [("lib/libm3textio.so", original.read_bytes())])
        self.assertEqual(original_hash, build.verify_jmod_native(archive, original_hash))
        shutil.copyfile(original, expected)
        commands.run("native-witness-strip", objcopy["resolved"], "-g", expected)
        expected_hash = build.sha256(expected)
        self.assertNotEqual(original_hash, expected_hash, "real debug-bearing ELF must change when stripped")
        shutil.copyfile(expected, linked)
        self.assertEqual(expected_hash, build.verify_linked_native(linked, expected_hash))
        shutil.copyfile(original, linked)
        with self.assertRaisesRegex(ValueError, "expected objcopy -g"):
            build.verify_linked_native(linked, expected_hash)
        linked.write_bytes(expected.read_bytes() + b"unexpected drift")
        with self.assertRaisesRegex(ValueError, "expected objcopy -g"):
            build.verify_linked_native(linked, expected_hash)
        self.assertEqual(original_hash, build.sha256(original), "stripping must preserve compiled provenance")
        self.assertEqual(compiler, build.verify_native_tool_identity(compiler, path, self.root))
        self.assertEqual(objcopy, build.verify_native_tool_identity(objcopy, path, self.root))

    def test_native_tool_path_resolution_and_executable_drift_refuse(self):
        first, second = self.root / "first", self.root / "second"
        first.mkdir(); second.mkdir()
        for directory, content in ((first, b"#!/bin/sh\nexit 0\n"), (second, b"#!/bin/sh\nexit 1\n")):
            executable = directory / "objcopy"
            executable.write_bytes(content)
            executable.chmod(0o755)
        before = build.native_tool_identity("objcopy", str(first), self.root)
        self.assertEqual(before, build.verify_native_tool_identity(before, str(first), self.root))
        with self.assertRaisesRegex(ValueError, "resolution or bytes changed"):
            build.verify_native_tool_identity(before, str(second), self.root)
        (first / "objcopy").write_bytes(b"#!/bin/sh\nexit 2\n")
        with self.assertRaisesRegex(ValueError, "resolution or bytes changed"):
            build.verify_native_tool_identity(before, str(first), self.root)
        with self.assertRaisesRegex(ValueError, "unavailable"):
            build.native_tool_identity("objcopy", str(self.root / "absent"), self.root)

    @unittest.skipUnless(os.name == "posix", "POSIX process-group timeout contract")
    def test_timeout_kills_spawned_child_and_records_failure(self):
        commands = self.commands()
        marker = self.root / "child-survived"
        child = "import pathlib,time,sys; time.sleep(0.7); pathlib.Path(sys.argv[1]).write_text('alive')"
        parent = "import subprocess,sys,time; subprocess.Popen([sys.executable,'-c',sys.argv[1],sys.argv[2]]); time.sleep(10)"
        previous = build.COMMAND_SECONDS
        build.COMMAND_SECONDS = 0.15
        started = time.monotonic()
        try:
            with self.assertRaisesRegex(RuntimeError, "exceeded time budget"):
                commands.run("timeout", sys.executable, "-c", parent, child, str(marker))
        finally:
            build.COMMAND_SECONDS = previous
        self.assertLess(time.monotonic() - started, 2)
        time.sleep(0.8)
        self.assertFalse(marker.exists(), "spawned child escaped timeout cleanup")
        receipt = json.loads((commands.output / "COMMANDS.json").read_text(encoding="utf-8"))
        self.assertEqual("TIMEOUT", receipt[0]["status"])
        self.assertEqual(64, len(receipt[0]["log_sha256"]))


if __name__ == "__main__":
    unittest.main(verbosity=2)
