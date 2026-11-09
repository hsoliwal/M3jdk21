# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Target-owned read-only naming gate: ambiguous paths/JSON must fail closed."""
from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

HERE = Path(__file__).resolve().parent


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, HERE / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


check = load("naming_receiver_strict", "check.py")
audit = load("naming_target_census_strict", "audit.py")


class ReceiverInputSafetyTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.data = self.root / "docs"
        self.data.mkdir()

    def test_canonical_relative_path_is_accepted(self):
        expected = self.root / "docs/file.java"
        self.assertEqual(check.local_file(self.root, "docs/file.java"), expected)
        self.assertEqual(audit.safe_path(self.root, "docs/file.java"), expected)

    def test_path_normalization_alias_is_refused(self):
        for path in ("docs//file.java", "docs/./file.java", "docs/../file.java",
                     "/tmp/file.java", "C:/file.java", "docs\\bad.java",
                     "docs/\x00bad.java"):
            with self.subTest(path=path):
                for guard in (check.local_file, audit.safe_path):
                    with self.assertRaises(ValueError):
                        guard(self.root, path)

    def test_symlinked_file_and_parent_refused(self):
        (self.data / "real.java").write_text("class Real {}", encoding="utf-8")
        (self.data / "link.java").symlink_to("real.java")
        (self.root / "alias").symlink_to(self.data, target_is_directory=True)
        for path in ("docs/link.java", "alias/real.java"):
            with self.subTest(path=path):
                for guard in (check.local_file, audit.safe_path):
                    with self.assertRaisesRegex(ValueError, "symlink"):
                        guard(self.root, path)

    def test_receiver_rejects_duplicate_root_json_key(self):
        data = '{"schema":1,"schema":2}'
        with self.assertRaisesRegex(ValueError, "duplicate JSON object key: schema"):
            json.loads(data, object_pairs_hook=check.unique_keys)

    def test_receiver_rejects_duplicate_nested_json_key(self):
        data = '{"records":[{"id":"first","id":"second"}]}'
        with self.assertRaisesRegex(ValueError, "duplicate JSON object key: id"):
            json.loads(data, object_pairs_hook=check.unique_keys)

    def test_target_census_rejects_duplicate_json_key(self):
        data = '{"authority":"ok","authority":"forged"}'
        with self.assertRaisesRegex(ValueError, "duplicate JSON object key: authority"):
            json.loads(data, object_pairs_hook=audit.unique_keys)

    def test_valid_json_preserves_exact_semantics(self):
        data = '{"migration":{"records":[{"id":"alpha"}]}}'
        expected = json.loads(data)
        self.assertEqual(json.loads(data, object_pairs_hook=check.unique_keys), expected)
        self.assertEqual(json.loads(data, object_pairs_hook=audit.unique_keys), expected)


if __name__ == "__main__":
    unittest.main()
