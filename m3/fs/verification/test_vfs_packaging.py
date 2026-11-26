#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Admission regression tests; the real linked-image integration is qualify_vfs.py."""
import csv
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
import zipfile

MODULE = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(MODULE / "tools"))
from vfs_pack_support import (DEPENDENCY_MODULES, PRODUCT_MODULES, UPSTREAM_COMMIT, digest, load_dependencies,
                              load_product_dependencies, runtime_dependencies,
                              selected_vfs_source, source_inventory, verify_source_lock,
                              write_pack_inputs)


class PackagingAdmissionTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix="m3fs-vfs-admission-")
        self.root = Path(self.directory.name)
        self.cache = self.root / "cache"
        self.cache.mkdir()
        self.module = self.root / "module"
        self.module.mkdir()
        self.descriptor = self.module / "src/main/dependency-modules/commons.logging/module-info.java"
        self.descriptor.parent.mkdir(parents=True)
        self.descriptor.write_text("module commons.logging { requires java.logging; }\n", encoding="utf-8")
        self.rows = []
        for name in sorted(DEPENDENCY_MODULES):
            relative = name + "/fixture.jar"
            archive = self.cache / relative
            archive.parent.mkdir()
            legal = {"META-INF/LICENSE.txt": b"fixture license\n", "META-INF/NOTICE.txt": b"fixture notice\n"}
            with zipfile.ZipFile(archive, "w") as output:
                for path, data in legal.items():
                    output.writestr(path, data)
                if name != "commons.logging":
                    output.writestr("module-info.class", b"fixture descriptor, never passed to javac/jmod")
            row = {"module": name, "repository_path": relative, "sha256": digest(archive),
                   "descriptor_kind": "injected" if name == "commons.logging" else "published",
                   "legal_sha256": {path: hashlib.sha256(data).hexdigest() for path, data in legal.items()}}
            if name == "commons.logging":
                row.update(descriptor_target=self.descriptor.relative_to(self.module).as_posix(),
                           descriptor_sha256=digest(self.descriptor))
            self.rows.append(row)
        self.lock = self.root / "dependencies.json"
        self.save_lock()

    def tearDown(self):
        self.directory.cleanup()

    def save_lock(self):
        self.lock.write_text(json.dumps({"schema": 1, "vfs_upstream_commit": UPSTREAM_COMMIT,
                                         "dependencies": self.rows}), encoding="utf-8")

    def test_complete_dependency_admission_and_single_path_descriptor_manifest(self):
        dependencies = load_dependencies(self.lock, self.cache, self.module)
        self.assertEqual(5, len(dependencies))
        jar = self.root / "product.jar"
        jar.write_bytes(b"manifest fixture only")
        image, manifest = write_pack_inputs(self.root, jar, dependencies)
        with image.open(encoding="utf-8", newline="") as stream:
            named = list(csv.DictReader(stream, delimiter="\t"))
        with manifest.open(encoding="utf-8", newline="") as stream:
            injected = list(csv.DictReader(stream, delimiter="\t"))
        self.assertEqual({"jdk.m3fs", "org.apache.commons.lang3", "org.apache.commons.io",
                          "org.apache.commons.compress", "org.apache.commons.codec"}, {row["module"] for row in named})
        self.assertEqual(1, len(injected))
        self.assertEqual("commons.logging", injected[0]["module"])
        self.assertEqual(str((self.cache / "commons.logging/fixture.jar").resolve()), injected[0]["unit_jars"])
        self.assertEqual("src/main/dependency-modules/commons.logging/module-info.java", injected[0]["target"])

    def test_missing_dependency_refused(self):
        (self.cache / self.rows[0]["repository_path"]).unlink()
        with self.assertRaises(FileNotFoundError):
            load_dependencies(self.lock, self.cache, self.module)

    def test_dependency_byte_drift_refused(self):
        with (self.cache / self.rows[0]["repository_path"]).open("ab") as stream:
            stream.write(b"drift")
        with self.assertRaisesRegex(ValueError, "dependency SHA-256 drift"):
            load_dependencies(self.lock, self.cache, self.module)

    def test_missing_original_notice_refused_even_with_changed_archive_pin(self):
        row = self.rows[0]
        jar = self.cache / row["repository_path"]
        with zipfile.ZipFile(jar, "w") as archive:
            archive.writestr("META-INF/LICENSE.txt", b"fixture license\n")
        row["sha256"] = digest(jar)
        self.save_lock()
        with self.assertRaisesRegex(ValueError, "dependency legal text drift"):
            load_dependencies(self.lock, self.cache, self.module)

    def test_logging_descriptor_drift_refused(self):
        self.descriptor.write_text("module commons.logging {}\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "descriptor SHA-256 drift"):
            load_dependencies(self.lock, self.cache, self.module)

    def test_incomplete_closure_refused(self):
        self.rows.pop()
        self.save_lock()
        with self.assertRaisesRegex(ValueError, "complete five-module"):
            load_dependencies(self.lock, self.cache, self.module)

    def test_duplicate_module_refused(self):
        self.rows[-1] = self.rows[0]
        self.save_lock()
        with self.assertRaisesRegex(ValueError, "duplicate or unexpected"):
            load_dependencies(self.lock, self.cache, self.module)

    def test_parent_traversal_refused(self):
        self.rows[0]["repository_path"] = "../escape.jar"
        self.save_lock()
        with self.assertRaisesRegex(ValueError, "unsafe relative path"):
            load_dependencies(self.lock, self.cache, self.module)

    def product_fixture(self):
        rows = []
        for name in sorted(PRODUCT_MODULES):
            archive = self.cache / (name + ".jar")
            legal = b"original product license\n"
            with zipfile.ZipFile(archive, "w") as output:
                output.writestr("module-info.class", b"fixture only; real descriptor tested by image integration")
                output.writestr("META-INF/LICENSE", legal)
            rows.append({"module": name, "repository_path": archive.name, "sha256": digest(archive),
                         "legal_sha256": {"META-INF/LICENSE": hashlib.sha256(legal).hexdigest()}})
        return rows

    def admit_product(self, rows):
        lock = self.root / "product-dependencies.json"
        lock.write_text(json.dumps({"schema": 1, "dependencies": rows}), encoding="utf-8")
        return load_product_dependencies(lock, self.cache)

    def test_current_product_closure_uses_inlined_logging_owner(self):
        old = load_dependencies(self.lock, self.cache, self.module)
        self.assertEqual(old, runtime_dependencies(old, False))
        current = runtime_dependencies(old, True) + self.admit_product(self.product_fixture())
        self.assertEqual(7, len(current))
        self.assertNotIn("commons.logging", {row["module"] for row in current})
        self.assertTrue(PRODUCT_MODULES.issubset({row["module"] for row in current}))
        jar = self.root / "product.jar"
        jar.write_bytes(b"manifest fixture only")
        image, manifest = write_pack_inputs(self.root, jar, current)
        self.assertEqual(9, len(image.read_text().splitlines()))
        self.assertEqual(["module\tunit_jars\ttarget"], manifest.read_text().splitlines())

    def test_product_closure_missing_and_duplicate_modules_refused(self):
        rows = self.product_fixture()
        with self.assertRaisesRegex(ValueError, "complete three-module"):
            self.admit_product(rows[:-1])
        rows[-1] = rows[0]
        with self.assertRaisesRegex(ValueError, "duplicate or unexpected product"):
            self.admit_product(rows)

    def test_product_dependency_byte_drift_refused(self):
        rows = self.product_fixture()
        with (self.cache / rows[0]["repository_path"]).open("ab") as stream:
            stream.write(b"drift")
        with self.assertRaisesRegex(ValueError, "product dependency SHA-256 drift"):
            self.admit_product(rows)

    def test_product_legal_drift_and_missing_pins_refused(self):
        rows = self.product_fixture()
        rows[0]["legal_sha256"]["META-INF/LICENSE"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "product legal text drift"):
            self.admit_product(rows)
        rows[0]["legal_sha256"] = {}
        with self.assertRaisesRegex(ValueError, "original product legal"):
            self.admit_product(rows)

    def test_product_descriptor_required_even_when_archive_resealed(self):
        rows = self.product_fixture()
        archive = self.cache / rows[0]["repository_path"]
        with zipfile.ZipFile(archive, "w") as output:
            output.writestr("META-INF/LICENSE", b"original product license\n")
        rows[0]["sha256"] = digest(archive)
        with self.assertRaisesRegex(ValueError, "explicit product module descriptor"):
            self.admit_product(rows)

    def test_product_notice_cannot_be_omitted_from_lock(self):
        rows = self.product_fixture()
        archive = self.cache / rows[0]["repository_path"]
        with zipfile.ZipFile(archive, "a") as output:
            output.writestr("META-INF/NOTICE", b"additional original notice\n")
        rows[0]["sha256"] = digest(archive)
        with self.assertRaisesRegex(ValueError, "complete original product legal"):
            self.admit_product(rows)

    def test_duplicate_product_zip_entries_refused(self):
        import warnings
        rows = self.product_fixture()
        archive = self.cache / rows[0]["repository_path"]
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(archive, "a") as output:
                output.writestr("META-INF/LICENSE", b"duplicate license\n")
        rows[0]["sha256"] = digest(archive)
        with self.assertRaisesRegex(ValueError, "duplicate product ZIP entries"):
            self.admit_product(rows)

    def test_product_parent_and_symlink_escape_refused(self):
        rows = self.product_fixture()
        original = rows[0]["repository_path"]
        rows[0]["repository_path"] = "../escape.jar"
        with self.assertRaisesRegex(ValueError, "unsafe relative path"):
            self.admit_product(rows)
        rows[0]["repository_path"] = original
        archive = self.cache / original
        outside = self.root / "outside.jar"
        archive.rename(outside)
        archive.symlink_to(outside)
        with self.assertRaisesRegex(ValueError, "inside the supplied cache"):
            self.admit_product(rows)

    def source_fixture(self):
        for relative in source_inventory(self.module):
            path = self.module / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            if not path.is_file():
                path.write_text("fixture source\n", encoding="utf-8")
        source = self.module / "src/main/java/jdk/m3/fs/M3FS.java"
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_text("package jdk.m3.fs; public class M3FS {}\n", encoding="utf-8")
        lock = self.root / "source.tsv"
        with lock.open("w", encoding="utf-8", newline="") as stream:
            writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
            writer.writerow(("path", "sha256"))
            for relative in source_inventory(self.module):
                writer.writerow((relative, digest(self.module / relative)))
        return source, lock

    def reviewed_module_fixture(self):
        # A verification fixture only: copy the exact finite source-lock inputs.
        for relative in source_inventory(MODULE):
            destination = self.module / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(MODULE / relative, destination)
        return self.seal_module_fixture()

    def seal_module_fixture(self):
        lock = self.root / "reviewed-source.tsv"
        lock.write_text("path\tsha256\n" + "".join(
            relative + "\t" + digest(self.module / relative) + "\n"
            for relative in source_inventory(self.module)), encoding="utf-8")
        return lock

    def test_first_party_helper_is_preserved_without_changing_apache_counts(self):
        lock = self.reviewed_module_fixture()
        proof = verify_source_lock(self.module, lock)
        self.assertEqual(311, proof["donor_java_custody"])
        self.assertEqual(237, proof["donor_java_compiled"])
        self.assertEqual(["src/main/java/jdk/m3/fs/vfs/util/M3UriMask.java"],
                         proof["owned_vfs_java"])
        self.assertTrue(selected_vfs_source(proof["owned_vfs_java"][0]))

    def test_unknown_provider_source_is_refused_even_when_resealed(self):
        self.reviewed_module_fixture()
        (self.module / "src/main/java/jdk/m3/fs/vfs/util/M3Unexpected.java").write_text(
            "package jdk.m3.fs.vfs.util; final class M3Unexpected {}\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "311 relocated Apache"):
            verify_source_lock(self.module, self.seal_module_fixture())

    def test_missing_donor_is_refused_even_when_resealed(self):
        self.reviewed_module_fixture()
        (self.module / "src/main/java/jdk/m3/fs/vfs/FileContentInfoFactory.java").unlink()
        with self.assertRaisesRegex(ValueError, "311 relocated Apache"):
            verify_source_lock(self.module, self.seal_module_fixture())

    def test_owned_helper_byte_drift_is_still_refused(self):
        lock = self.reviewed_module_fixture()
        helper = self.module / "src/main/java/jdk/m3/fs/vfs/util/M3UriMask.java"
        helper.write_text(helper.read_text(encoding="utf-8") + "// drift\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "source SHA-256 drift"):
            verify_source_lock(self.module, lock)

    def test_source_byte_drift_refused_before_compilation(self):
        source, lock = self.source_fixture()
        source.write_text("changed source\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "source SHA-256 drift"):
            verify_source_lock(self.module, lock)

    def test_unsealed_source_addition_refused(self):
        source, lock = self.source_fixture()
        source.with_name("Unexpected.java").write_text("added source\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "source-lock inventory mismatch"):
            verify_source_lock(self.module, lock)

    def test_source_only_remote_profile_and_real_url_fallback(self):
        prefix = "src/main/java/jdk/m3/fs/vfs/"
        for path in ("provider/ftp/FtpFileObject.java", "provider/http5/Http5FileObject.java",
                     "provider/sftp/SftpFileObject.java", "cache/LRUFilesCache.java",
                     "provider/URLFileName.java", "provider/URLFileNameParser.java"):
            self.assertFalse(selected_vfs_source(prefix + path), path)
        for path in ("provider/url/UrlFileObject.java", "provider/url/UrlFileNameParser.java",
                     "provider/GenericURLFileName.java", "provider/ram/RamFileObject.java"):
            self.assertTrue(selected_vfs_source(prefix + path), path)

    def test_native_gate_and_strict_owned_compile_are_preserved(self):
        ns = {"m": "http://maven.apache.org/POM/4.0.0"}
        pom = ET.parse(MODULE / "pom.xml")
        executions = {element.findtext("m:id", namespaces=ns): element for element in pom.findall(".//m:execution", ns)}
        for name in ("configure-native", "build-native", "verify-native", "module-descriptor"):
            self.assertIn(name, executions)
        self.assertEqual("verify", executions["verify-native"].findtext("m:phase", namespaces=ns))
        self.assertEqual("required", pom.findtext(".//m:m3fs.native", namespaces=ns))
        self.assertEqual("-Xcheck:jni", pom.findtext(".//m:argLine", namespaces=ns))
        self.assertIsNotNone(pom.find(".//m:compilerArgs/m:arg[.='-Werror']", ns))
        spec = importlib.util.spec_from_file_location("runtime_package", MODULE / "tools/package_runtime.py")
        loaded = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(loaded)
        with self.assertRaises(SystemExit) as failure:
            loaded.main(["--jdk-home", str(self.root), "--jar", str(self.root / "x.jar"),
                         "--out", str(self.root / "out")])
        self.assertEqual(2, failure.exception.code)


if __name__ == "__main__":
    unittest.main(verbosity=2)
