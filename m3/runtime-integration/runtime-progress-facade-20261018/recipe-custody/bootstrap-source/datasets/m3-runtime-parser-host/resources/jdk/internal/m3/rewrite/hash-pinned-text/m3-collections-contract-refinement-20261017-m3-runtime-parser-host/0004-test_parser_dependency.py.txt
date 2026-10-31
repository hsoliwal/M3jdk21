#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Positive and negative evidence for the additional parser identity gate."""
from pathlib import Path
import argparse
import tempfile
import unittest
import verify_parser_dependency as owner

parser = argparse.ArgumentParser()
parser.add_argument("--jar", type=Path, required=True)
args = parser.parse_args()

class ParserDependencyContract(unittest.TestCase):
    def test_source_qualified_artifact(self):
        owner.verify(args.jar)

    def test_modified_artifact_refused(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "modified.jar"
            path.write_bytes(args.jar.read_bytes() + b"drift")
            with self.assertRaisesRegex(ValueError, "identity drift"):
                owner.verify(path)

    def test_missing_artifact_refused(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(ValueError, "regular artifact"):
                owner.verify(Path(directory) / "missing.jar")

    def test_directory_refused(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(ValueError, "regular artifact"):
                owner.verify(Path(directory))

if __name__ == "__main__":
    unittest.main(argv=[__file__])
