#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Source coverage is checked independently of the builder's module allowlist."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
import unittest


class SourceCoverageTest(unittest.TestCase):
    def test_every_production_source_is_accounted_for(self):
        script = self.templates / "dev/m3-jmod/build.py"
        self.assertTrue(script.is_file(), "current-fork JMOD source builder is missing")
        result = subprocess.run(
            [sys.executable, str(script), "--source-root", str(self.source), "--inventory"],
            capture_output=True, text=True, check=False)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        inventory = json.loads(result.stdout)
        actual = {str(p.relative_to(self.source)) for p in self.source.glob(
            "opennlp*/**/src/main/java/**/*.java")}
        accounted = set(inventory["source_files"])
        for item in inventory["extensions"].values():
            accounted.update(item["source_files"])
        self.assertEqual(actual, accounted)
        self.assertEqual(923, len(inventory["source_files"]))
        self.assertEqual(89, len(inventory["packages"]))
        self.assertIn("opennlp.tools.document", inventory["packages"])
        self.assertIn("opennlp.tools.ml.libsvm.doccat", inventory["packages"])
        self.assertIn("opennlp.tools.models.classgraph", inventory["packages"])
        self.assertEqual("org.apache.opennlp.tools", inventory["module"])
        for name, count in (("morfologik", 10), ("spellcheck", 23), ("uima", 44)):
            self.assertEqual("BUILT", inventory["extensions"][name]["disposition"])
            self.assertEqual(count, len(inventory["extensions"][name]["source_files"]))
        self.assertEqual("OPTIONAL_SEPARATE_DISTRIBUTION",
                         inventory["extensions"]["dl"]["disposition"])


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--templates", type=Path, required=True)
    args, rest = parser.parse_known_args()
    SourceCoverageTest.source = args.source_root.resolve()
    SourceCoverageTest.templates = args.templates.resolve()
    unittest.main(argv=[sys.argv[0], *rest])
