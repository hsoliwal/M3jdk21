#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Regression checks for JMOD aggregation and legal-notice ownership."""
import importlib.util
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
        self.temporary = tempfile.TemporaryDirectory()
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
        return {str(path.relative_to(self.legal)): path.read_bytes()
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


if __name__ == "__main__":
    unittest.main()
