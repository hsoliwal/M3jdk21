# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""SDK inventory/retention/refusal tests. Synthetic files do not prove JDK compatibility."""
import contextlib
import io
import json
import os
import shutil
import stat
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import build_pack
import developer_tools
from tool_packs import LockedAsset, LockedModule, ToolPacks


class SdkProfileTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.home = self.root / "jdk"
        self.output = self.root / "output"
        self.output.mkdir()
        self.image = self.output / "image"
        self.logs = self.output / "logs"
        self.logs.mkdir()
        self.modules = ("java.base", "jdk.compiler", "jdk.jlink")
        self.release = ('SOURCE="fixture-source"\nJAVA_VERSION="21"\n'
                        'MODULES="java.base jdk.compiler jdk.jlink"\nIMPLEMENTOR="fixture owner"\n')
        files = {
            "release": self.release.encode(), "lib/modules": b"original runtime",
            "lib/src.zip": b"synthetic source ZIP", "lib/classlist": b"java/lang/Object\n",
            "include/jni.h": b"JNI header", "include/jvmti.h": b"JVMTI header",
            "include/linux/jni_md.h": b"Linux JNI header", "man/man1/java.1": b"manual page",
            "lib/server/libjvm.so": b"original native debugful bytes",
            "lib/server/libjvm.debuginfo": b"external native symbols",
            "lib/server/classes.jsa": b"old compressed archive",
            "lib/server/classes_nocoops.jsa": b"old uncompressed archive",
            "conf/security/java.security": b"original security configuration",
            "legal/java.base/LICENSE": b"original legal content", "README": b"original auxiliary asset",
        }
        files.update({"bin/" + name: ("tool " + name).encode()
                      for name in ("java", "javac", "jar", "jmod", "jlink")})
        files.update({"jmods/" + name + ".jmod": ("original JMOD " + name).encode() for name in self.modules})
        for name, content in files.items():
            path = self.home / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content)
        for path in (self.home / "bin").iterdir():
            path.chmod(0o755)
        (self.home / "lib/server/libjvm.debuginfo").chmod(0o444)
        (self.home / "empty-sdk-directory").mkdir()
        (self.home / "legal/jdk.compiler").mkdir()
        (self.home / "legal/jdk.compiler/LICENSE").symlink_to("../java.base/LICENSE")
        self.environment = patch.dict(os.environ, {"JAVA_TOOL_OPTIONS": "", "JDK_JAVA_OPTIONS": "", "_JAVA_OPTIONS": ""})
        self.environment.start()
        self.addCleanup(self.environment.stop)

    def baseline(self, tools=ToolPacks()):
        return build_pack.sdk_preflight(self.home, tools, [])

    def selected(self):
        return {name: build_pack.digest(self.home / "jmods" / (name + ".jmod")) for name in self.modules}

    def linked_image(self, image=None):
        image = image or self.image
        shutil.copytree(self.home, image, symlinks=True)
        for name in ("lib/src.zip", "lib/server/libjvm.debuginfo", "README"):
            (image / name).unlink()
        (image / "empty-sdk-directory").rmdir()
        (image / "lib/modules").write_bytes(b"composed runtime")
        # Native jlink sorts release properties, unlike this intentionally noncanonical fixture layout.
        (image / "release").write_text('IMPLEMENTOR="fixture owner"\nJAVA_VERSION="21"\n'
                                     'MODULES="java.base jdk.jlink jdk.compiler"\nSOURCE="fixture-source"\n')
        (image / "lib/server/classes.jsa").write_bytes(b"new compressed archive")
        (image / "lib/server/classes_nocoops.jsa").write_bytes(b"new uncompressed archive")
        (image / "bin/java").chmod(0o700)
        return image

    def retained(self):
        baseline = self.baseline()
        self.linked_image()
        retained = build_pack.sdk_restore_assets(self.home, self.image, baseline, self.selected())
        return baseline, retained

    def test_existing_profile_link_arguments_are_unchanged(self):
        expected = [self.home / "bin/jlink", "--module-path", "original:path", "--add-modules", "java.base",
                    "--strip-debug", "--no-header-files", "--no-man-pages", "--output", self.image]
        for profile in ("diagnostics", "developer"):
            with self.subTest(profile=profile):
                self.assertEqual(expected, build_pack.link_arguments(self.home, "original:path", ["java.base"],
                                                                    self.image, profile))

    def test_sdk_uses_existing_native_plugins_without_stripping(self):
        arguments = build_pack.link_arguments(self.home, "original:path", list(self.modules),
                                              self.image, "sdk", self.baseline())
        for forbidden in ("--strip-debug", "--no-header-files", "--no-man-pages"):
            self.assertNotIn(forbidden, arguments)
        self.assertEqual(self.home / "release", arguments[arguments.index("--release-info") + 1])
        self.assertEqual(self.image / "jmods", arguments[arguments.index("--keep-packaged-modules") + 1])
        self.assertIn("--generate-cds-archive", arguments)

    def test_sdk_without_source_cds_does_not_request_archive_generation(self):
        for name in build_pack.SDK_CDS:
            (self.home / name).unlink()
        baseline = self.baseline()
        arguments = build_pack.link_arguments(self.home, "path", list(self.modules), self.image, "sdk", baseline)
        self.assertNotIn("--generate-cds-archive", arguments)

    def test_invalid_profile_or_missing_sdk_inventory_refuses(self):
        for profile in ("unknown", "sdk"):
            with self.subTest(profile=profile), self.assertRaises(ValueError):
                build_pack.link_arguments(self.home, "path", list(self.modules), self.image, profile)

    def test_linux_gate_precedes_output_creation(self):
        destination = self.root / "not-created"
        with patch.object(build_pack.platform, "system", return_value="Windows"):
            with self.assertRaisesRegex(ValueError, "Linux-only"):
                build_pack.main(["--jdk", str(self.home), "--out", str(destination), "--profile", "sdk"])
        self.assertFalse(destination.exists())

    def test_source_release_jmod_set_must_match(self):
        (self.home / "jmods/jdk.jlink.jmod").unlink()
        with self.assertRaisesRegex(ValueError, "release/JMOD"):
            self.baseline()

    def test_source_sdk_assets_are_required(self):
        for name in ("lib/src.zip", "include/jni.h", "include/jvmti.h", "include/linux/jni_md.h", "man/man1/java.1"):
            with self.subTest(name=name):
                path = self.home / name
                original = path.read_bytes()
                path.unlink()
                try:
                    with self.assertRaisesRegex(ValueError, "Incomplete source SDK"):
                        self.baseline()
                finally:
                    path.write_bytes(original)

    def test_ambient_vm_options_refuse_before_linking(self):
        for variable in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"):
            with self.subTest(variable=variable), patch.dict(os.environ, {variable: "-Dunexpected=true"}):
                with self.assertRaisesRegex(ValueError, "Uncontrolled VM"):
                    self.baseline()

    def test_unknown_cds_and_missing_classlist_refuse(self):
        unexpected = self.home / "lib/custom.jsa"
        unexpected.write_bytes(b"unknown archive")
        with self.assertRaisesRegex(ValueError, "Unsupported.*CDS"):
            self.baseline()
        unexpected.unlink()
        (self.home / "lib/classlist").unlink()
        with self.assertRaisesRegex(ValueError, "classlist"):
            self.baseline()

    def test_literal_safe_symlink_identity_is_recorded(self):
        record = self.baseline()["entries"]["legal/jdk.compiler/LICENSE"]
        self.assertEqual("symlink", record["kind"])
        self.assertEqual("../java.base/LICENSE", record["target"])
        self.assertEqual(stat.S_IMODE((self.home / "legal/jdk.compiler/LICENSE").lstat().st_mode), record["mode"])

    def test_directory_symlink_is_retained_without_duplicate_target_traversal(self):
        link = self.home / "lib/sdk-conf"
        link.symlink_to("../conf", target_is_directory=True)
        baseline = self.baseline()
        self.assertEqual("symlink", baseline["entries"]["lib/sdk-conf"]["kind"])
        self.assertFalse(any(name.startswith("lib/sdk-conf/") for name in baseline["entries"]))
        self.linked_image()
        (self.image / "lib/sdk-conf").unlink()
        retained = build_pack.sdk_restore_assets(self.home, self.image, baseline, self.selected())
        with patch.object(build_pack, "command", return_value="openjdk 21"):
            cds = build_pack.sdk_verify_cds(self.image, baseline, self.logs)
        build_pack.sdk_finish(self.home, self.image, self.output, baseline, retained,
                              self.selected(), ToolPacks(), cds, dict.fromkeys(self.modules, "21"))
        self.assertTrue((self.image / "lib/sdk-conf").is_symlink())
        self.assertEqual("../conf", os.readlink(self.image / "lib/sdk-conf"))
        self.assertEqual((self.home / "conf/security/java.security").read_bytes(),
                         (self.image / "lib/sdk-conf/security/java.security").read_bytes())

    def test_external_dangling_and_cyclic_links_refuse(self):
        outside = self.root / "outside"
        outside.write_bytes(b"outside")
        link = self.home / "unsafe"
        for target in (str(outside), "../outside", "missing", "unsafe"):
            with self.subTest(target=target):
                link.symlink_to(target)
                try:
                    with self.assertRaises(ValueError):
                        self.baseline()
                finally:
                    link.unlink()

    def test_special_files_are_not_silently_omitted(self):
        fifo = self.home / "unsupported-pipe"
        os.mkfifo(fifo)
        with self.assertRaisesRegex(ValueError, "Special SDK"):
            self.baseline()

    def test_selected_assets_cannot_replace_source_assets_or_symlink_parents(self):
        source = self.root / "selected-file"
        source.write_bytes(b"selected")
        (self.home / "lib/sdk-link").symlink_to("../conf")
        for target in ("lib/src.zip", "legal/jdk.compiler/LICENSE", "lib/sdk-link/new-file"):
            with self.subTest(target=target):
                asset = LockedAsset(source, target, build_pack.digest(source), False)
                with self.assertRaisesRegex(ValueError, "collides with a source SDK asset"):
                    self.baseline(ToolPacks(assets=(asset,)))

    def test_selected_modules_cannot_replace_platform_modules(self):
        archive = self.home / "jmods/java.base.jmod"
        module = LockedModule("java.base", archive, build_pack.digest(archive))
        with self.assertRaisesRegex(ValueError, "collides with a source SDK module"):
            self.baseline(ToolPacks(modules=(module,)))

    def test_release_retains_exact_layout_and_all_other_properties(self):
        original = self.release.replace("\n", "\r\n") + "# retained comment\r\n"
        linked = ('IMPLEMENTOR="fixture owner"\nJAVA_VERSION="21"\n'
                  'MODULES="java.base jdk.compiler jdk.jlink com.m3.arrays"\nSOURCE="fixture-source"\n')
        expected = original.replace('MODULES="java.base jdk.compiler jdk.jlink"',
                                    'MODULES="java.base jdk.compiler jdk.jlink com.m3.arrays"')
        self.assertEqual(expected.encode(), build_pack.sdk_release_bytes(original, linked,
                                                                         {*self.modules, "com.m3.arrays"}))

    def test_release_property_drift_and_module_drift_refuse(self):
        for candidate in (self.release.replace('JAVA_VERSION="21"', 'JAVA_VERSION="21.0.1"'),
                          self.release.replace('SOURCE="fixture-source"', 'SOURCE="other"'),
                          self.release + 'EXTRA="unaccounted"\n'):
            with self.subTest(candidate=candidate), self.assertRaisesRegex(ValueError, "property drift"):
                build_pack.sdk_release_bytes(self.release, candidate, self.modules)
        with self.assertRaisesRegex(ValueError, "module inventory"):
            build_pack.sdk_release_bytes(self.release, self.release, {*self.modules, "com.m3.arrays"})
        with self.assertRaisesRegex(ValueError, "duplicate"):
            build_pack.sdk_release(self.release + 'MODULES="java.base"\n')

    def test_runtime_module_parser_rejects_ignored_signals_and_duplicates(self):
        self.assertEqual({"java.base": "21", "com.m3.arrays": ""},
                         build_pack.sdk_module_versions("java.base@21\ncom.m3.arrays\n"))
        for text in ("", "java.base@21\njava.base@21\n", "WARNING: ignored\njava.base@21\n", "java.base@\n"):
            with self.subTest(text=text), self.assertRaises(ValueError):
                build_pack.sdk_module_versions(text)

    def test_all_auxiliary_assets_and_modes_are_retained_without_old_cds_copy(self):
        baseline, retained = self.retained()
        for name in ("lib/src.zip", "lib/server/libjvm.debuginfo", "include/jni.h", "man/man1/java.1", "README"):
            self.assertEqual((self.home / name).read_bytes(), (self.image / name).read_bytes(), name)
            self.assertEqual(stat.S_IMODE((self.home / name).stat().st_mode),
                             stat.S_IMODE((self.image / name).stat().st_mode), name)
        self.assertTrue((self.image / "empty-sdk-directory").is_dir())
        self.assertEqual(0o755, stat.S_IMODE((self.image / "bin/java").stat().st_mode))
        self.assertEqual("../java.base/LICENSE", os.readlink(self.image / "legal/jdk.compiler/LICENSE"))
        for name in build_pack.SDK_CDS:
            self.assertNotEqual((self.home / name).read_bytes(), (self.image / name).read_bytes())
        self.assertEqual(baseline["entries"], build_pack.sdk_tree(self.home))
        self.assertEqual(build_pack.digest(self.image / "release"), retained["release_sha256"])

    def test_ordinary_byte_collision_refuses_before_copy_or_release_rewrite(self):
        baseline = self.baseline()
        self.linked_image()
        original_release = (self.image / "release").read_bytes()
        (self.image / "lib/server/libjvm.so").write_bytes(b"unexplained native drift")
        with self.assertRaisesRegex(ValueError, "asset collision"):
            build_pack.sdk_restore_assets(self.home, self.image, baseline, self.selected())
        self.assertFalse((self.image / "lib/src.zip").exists())
        self.assertEqual(original_release, (self.image / "release").read_bytes())

    def test_legal_alias_representation_is_restored_only_for_equal_bytes(self):
        baseline = self.baseline()
        self.linked_image()
        reference = self.image / "legal/com.example.extra/LICENSE"
        reference.parent.mkdir()
        reference.write_bytes((self.home / "legal/java.base/LICENSE").read_bytes())
        target = self.image / "legal/java.base/LICENSE"
        target.unlink()
        target.symlink_to("../com.example.extra/LICENSE")
        compiler = self.image / "legal/jdk.compiler/LICENSE"
        compiler.unlink()
        compiler.write_bytes((self.home / "legal/jdk.compiler/LICENSE").read_bytes())
        retained = build_pack.sdk_restore_assets(self.home, self.image, baseline, self.selected())
        self.assertEqual(["legal/java.base/LICENSE", "legal/jdk.compiler/LICENSE"],
                         retained["legal_aliases_restored"])
        self.assertFalse(target.is_symlink())
        self.assertEqual((self.home / "legal/java.base/LICENSE").read_bytes(), target.read_bytes())
        self.assertTrue(compiler.is_symlink())
        self.assertEqual("../java.base/LICENSE", os.readlink(compiler))

    def test_different_legal_content_is_not_overwritten(self):
        baseline = self.baseline()
        self.linked_image()
        (self.image / "legal/java.base/LICENSE").write_bytes(b"different legal rights")
        with self.assertRaisesRegex(ValueError, "asset collision"):
            build_pack.sdk_restore_assets(self.home, self.image, baseline, self.selected())

    def test_source_drift_after_inventory_is_refused(self):
        baseline = self.baseline()
        self.linked_image()
        (self.home / "lib/src.zip").write_bytes(b"concurrently replaced source archive")
        with self.assertRaisesRegex(ValueError, "Source SDK changed"):
            build_pack.sdk_restore_assets(self.home, self.image, baseline, self.selected())

    def test_cds_verification_names_both_archives_and_matching_modes(self):
        baseline, _ = self.retained()
        with patch.object(build_pack, "command", return_value="openjdk 21") as command:
            records = build_pack.sdk_verify_cds(self.image, baseline, self.logs)
        self.assertEqual(2, len(records))
        self.assertEqual(2, command.call_count)
        for call, record in zip(command.call_args_list, records):
            arguments = call.args[0]
            self.assertIn("-Xshare:on", arguments)
            self.assertIn(build_pack.SDK_CDS[record["path"]], arguments)
            self.assertIn("-XX:SharedArchiveFile=" + str(self.image / record["path"]), arguments)
            self.assertEqual("native-jlink-generate-cds-archive", record["generation"])
            self.assertNotIn("-Xmx128m", record["generation_options"])

    def test_cds_failure_or_missing_archive_cannot_be_reported_as_success(self):
        baseline, _ = self.retained()
        with patch.object(build_pack, "command", side_effect=RuntimeError("incompatible archive")):
            with self.assertRaisesRegex(RuntimeError, "incompatible archive"):
                build_pack.sdk_verify_cds(self.image, baseline, self.logs)
        (self.image / "lib/server/classes_nocoops.jsa").unlink()
        with self.assertRaisesRegex(ValueError, "missing composed"):
            build_pack.sdk_verify_cds(self.image, baseline, self.logs)
        self.assertFalse((self.output / "RECEIPT.json").exists())

    def test_final_coverage_and_jmod_hashes_are_exact(self):
        baseline, retained = self.retained()
        with patch.object(build_pack, "command", return_value="openjdk 21"):
            cds = build_pack.sdk_verify_cds(self.image, baseline, self.logs)
        receipt = build_pack.sdk_finish(self.home, self.image, self.output, baseline, retained,
                                        self.selected(), ToolPacks(), cds, dict.fromkeys(self.modules, "21"))
        self.assertTrue(receipt["all_source_assets_accounted"])
        self.assertTrue(receipt["source_tree_unchanged"])
        self.assertFalse(receipt["m3jdk21_product_acceptance"])
        self.assertEqual(self.selected(), receipt["jmods"])
        self.assertEqual(baseline["entries"], json.loads((self.output / "SDK_SOURCE_ASSETS.json").read_text()))
        coverage = (self.output / "SDK_ASSET_COVERAGE.tsv").read_text()
        self.assertIn("source_link_target\timage_link_target", coverage)
        self.assertIn("CDS_REGENERATED_AND_VERIFIED", coverage)
        self.assertNotIn("com.m3pack.diagnostics", receipt["installed_modules"])

    def test_final_original_directory_root_and_regenerated_file_modes_are_restored(self):
        (self.home / "conf/security").chmod(0o750)
        (self.home / "lib/server/classes.jsa").chmod(0o640)
        self.home.chmod(0o750)
        baseline, retained = self.retained()
        (self.image / "conf/security").chmod(0o700)
        (self.image / "lib/server/classes.jsa").chmod(0o600)
        self.image.chmod(0o700)
        with patch.object(build_pack, "command", return_value="openjdk 21"):
            cds = build_pack.sdk_verify_cds(self.image, baseline, self.logs)
        build_pack.sdk_finish(self.home, self.image, self.output, baseline, retained,
                              self.selected(), ToolPacks(), cds, dict.fromkeys(self.modules, "21"))
        for name in ("conf/security", "lib/server/classes.jsa"):
            self.assertEqual(stat.S_IMODE((self.home / name).stat().st_mode),
                             stat.S_IMODE((self.image / name).stat().st_mode), name)
        self.assertEqual(0o750, stat.S_IMODE(self.image.stat().st_mode))

    def test_cds_tampering_after_verification_cannot_produce_coverage_receipt(self):
        baseline, retained = self.retained()
        with patch.object(build_pack, "command", return_value="openjdk 21"):
            cds = build_pack.sdk_verify_cds(self.image, baseline, self.logs)
        (self.image / "lib/server/classes.jsa").write_bytes(b"archive changed after explicit verification")
        with self.assertRaisesRegex(ValueError, "Verified SDK CDS archive changed"):
            build_pack.sdk_finish(self.home, self.image, self.output, baseline, retained,
                                  self.selected(), ToolPacks(), cds, dict.fromkeys(self.modules, "21"))
        self.assertFalse((self.output / "SDK_ASSET_COVERAGE.tsv").exists())

    def test_final_source_image_and_jmod_drift_refuse_without_coverage_receipt(self):
        baseline, retained = self.retained()
        with patch.object(build_pack, "command", return_value="openjdk 21"):
            cds = build_pack.sdk_verify_cds(self.image, baseline, self.logs)
        target = self.image / "jmods/java.base.jmod"
        original = target.read_bytes()
        target.write_bytes(b"changed installed JMOD")
        with self.assertRaisesRegex(ValueError, "not retained exactly"):
            build_pack.sdk_finish(self.home, self.image, self.output, baseline, retained,
                                  self.selected(), ToolPacks(), cds, dict.fromkeys(self.modules, "21"))
        target.write_bytes(original)
        (self.image / "unexpected-file").write_bytes(b"unexpected")
        with self.assertRaisesRegex(ValueError, "asset path drift"):
            build_pack.sdk_finish(self.home, self.image, self.output, baseline, retained,
                                  self.selected(), ToolPacks(), cds, dict.fromkeys(self.modules, "21"))
        self.assertFalse((self.output / "SDK_ASSET_COVERAGE.tsv").exists())

    def test_main_sdk_uses_full_roots_external_fixture_and_existing_developer_proof(self):
        destination = self.root / "full-sdk"
        observed = []
        selected = self.selected()
        inventory = "module\tversion\tkind\tsha256\tpath\n" + "".join(
            name + "\t21\tJMOD\t" + selected[name] + "\t" + str(self.home / "jmods" / (name + ".jmod")) + "\n"
            for name in sorted(selected))

        def command(arguments, log, timeout=180):
            values = [str(value) for value in arguments]
            observed.append(values)
            text = ""
            if "--list-modules" in values:
                text = "".join(name + "@21\n" for name in sorted(self.modules))
            elif "com.m3.tools.modulepack.M3ModuleInventory" in values:
                self.assertEqual(set(self.modules), set(values[4].split(",")))
                text = inventory
            elif Path(values[0]).name == "jlink":
                self.linked_image(Path(values[values.index("--output") + 1]))
            elif Path(values[0]).name == "jmod" and values[1] == "create":
                Path(values[-1]).write_bytes(b"synthetic outside-image fixture archive")
            elif "com.m3pack.diagnostics/com.m3.pack.diagnostics.M3DiagnosticsSmoke" in values:
                self.assertIn("--module-path", values)
                self.assertEqual(str(destination / "diagnostics-classes"), values[values.index("--module-path") + 1])
                text = "M3_DIAGNOSTICS_PASS"
            Path(log).write_text(text)
            return text

        def compile_module(home, source, classes, log, module_path=None):
            classes.mkdir()

        with patch.object(build_pack, "command", side_effect=command), \
                patch.object(build_pack, "compile_module", side_effect=compile_module), \
                patch.object(developer_tools, "verify_developer_tools", return_value={"status": "SYNTHETIC_TEST_ONLY"}) as developer, \
                patch.object(build_pack.platform, "system", return_value="Linux"), \
                contextlib.redirect_stdout(io.StringIO()):
            build_pack.main(["--jdk", str(self.home), "--out", str(destination), "--profile", "sdk"])
        developer.assert_called_once_with(destination / "image", destination, destination / "image/jmods")
        receipt = json.loads((destination / "RECEIPT.json").read_text())
        self.assertEqual("sdk", receipt["profile"])
        self.assertEqual(set(self.modules), set(receipt["roots"]))
        self.assertEqual("NOT_EXECUTED_BY_THIS_SCRIPT", receipt["maven_junit_jacoco"])
        self.assertFalse(receipt["m3jdk21_product_acceptance"])
        self.assertEqual(set(self.modules), set(receipt["sdk"]["installed_modules"]))
        self.assertFalse((destination / "link-inputs").exists(), "SDK reuses installed JMODs without another full copy")
        self.assertTrue(any("--keep-packaged-modules" in row for row in observed))


if __name__ == "__main__":
    unittest.main()
