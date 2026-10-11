# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Discriminators for offline donor pinning, corruption, absence, and license-marker drift."""
from pathlib import Path
import tempfile
import unittest

from synexia_reference import (
    LICENSE_BLOB_SHA,
    LICENSE_FILE,
    SOURCE_BLOBS,
    SYNEXIA_COMMIT,
    git_blob_sha,
    pinned_sources,
    verify_source_bytes,
)

REFERENCE_ROOT = Path(__file__).resolve().parent / "reference-src"


class SynexiaReferencePinTests(unittest.TestCase):
    def test_committed_sources_and_license_match_upstream_git_blobs(self):
        paths = pinned_sources(REFERENCE_ROOT)
        self.assertEqual(set(path.name for path in paths), set(SOURCE_BLOBS))
        self.assertEqual(git_blob_sha((REFERENCE_ROOT / LICENSE_FILE).read_bytes()), LICENSE_BLOB_SHA)

    def test_modified_source_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source_root = root / SYNEXIA_COMMIT / "com/synexia/indexstring"
            source_root.mkdir(parents=True)
            (root / LICENSE_FILE).write_bytes((REFERENCE_ROOT / LICENSE_FILE).read_bytes())
            for source in pinned_sources(REFERENCE_ROOT):
                target = source_root / source.name
                target.write_bytes(source.read_bytes())
            target = source_root / "FrozenChars.java"
            target.write_bytes(target.read_bytes() + b"\n")
            with self.assertRaisesRegex(ValueError, "blob pin mismatch"):
                pinned_sources(root)

    def test_missing_source_fails_closed(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / LICENSE_FILE).write_bytes((REFERENCE_ROOT / LICENSE_FILE).read_bytes())
            with self.assertRaises(FileNotFoundError):
                pinned_sources(root)

    def test_matching_hash_does_not_excuse_missing_apache_marker(self):
        unlicensed = b"package com.synexia.indexstring;\n"
        with self.assertRaisesRegex(ValueError, "SPDX marker changed"):
            verify_source_bytes("FrozenChars.java", unlicensed, git_blob_sha(unlicensed))


if __name__ == "__main__":
    unittest.main(verbosity=2)
