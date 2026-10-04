# SPDX-License-Identifier: Apache-2.0
import tempfile
import unittest
from pathlib import Path

import m3_dna_id as identity


class SourceConvergenceIdentityTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.path = Path(self.temp.name) / "identity.tsv"

    def tearDown(self) -> None:
        self.temp.cleanup()

    def write(self, role="DONOR", revision="jdk-22+36", root=None, files=3, changed=1, holds=1):
        self.path.write_text(
            "role\trevision\tsemanticRoot\tfiles\tchanged\tholds\n"
            f"{role}\t{revision}\t{root or ('a' * 64)}\t{files}\t{changed}\t{holds}\n",
            encoding="utf-8",
        )

    def test_load_validates_role_revision_root_and_counts(self) -> None:
        self.write()
        row = identity.load(
            self.path,
            expected_role="DONOR",
            expected_revision="jdk-22+36",
        )
        self.assertEqual("DONOR", row.role)
        self.assertEqual("jdk-22+36", row.revision)
        self.assertEqual("a" * 64, row.semantic_root)

    def test_mismatch_and_invalid_counts_fail_closed(self) -> None:
        self.write(role="BASELINE")
        with self.assertRaisesRegex(ValueError, "role mismatch"):
            identity.load(self.path, expected_role="DONOR")

        self.write(revision="jdk-23+37")
        with self.assertRaisesRegex(ValueError, "revision mismatch"):
            identity.load(self.path, expected_revision="jdk-22+36")

        self.write(files=1, changed=1, holds=1)
        with self.assertRaisesRegex(ValueError, "counts"):
            identity.load(self.path)

        self.write(root="bad")
        with self.assertRaisesRegex(ValueError, "semantic root"):
            identity.load(self.path)


if __name__ == "__main__":
    unittest.main()
