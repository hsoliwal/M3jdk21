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
from vfs_pack_support import (DEPENDENCY_MODULES, UPSTREAM_COMMIT, digest, load_dependencies,
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

    def source_fixture(self):
        for relative in source_inventory(self.module):
            path = self.module / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            if not path.is_file():
                path.write_text("fixture source\n", encoding="utf-8")
        source = self.module / "src/main/java/com/m3/fs/M3FS.java"
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_text("package com.m3.fs; public class M3FS {}\n", encoding="utf-8")
        lock = self.root / "source.tsv"
        with lock.open("w", encoding="utf-8", newline="") as stream:
            writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
            writer.writerow(("path", "sha256"))
            for relative in source_inventory(self.module):
                writer.writerow((relative, digest(self.module / relative)))
        return source, lock

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
        prefix = "src/main/java/com/m3/fs/vfs/"
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
