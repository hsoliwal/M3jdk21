# SPDX-License-Identifier: Apache-2.0
import tempfile
import unittest
from pathlib import Path

import release_donor_refs


class ReleaseDonorRefsTest(unittest.TestCase):
    def test_repository_donor_refs_are_exact_and_jdk27_is_not_ga(self):
        root = Path(__file__).resolve().parents[2]
        refs = release_donor_refs.verify_with_release_authority(root)
        self.assertEqual("jdk-24+36", refs[24])
        self.assertEqual(
            "f3701c80216900f3ded26f9de1befe43813be95c",
            refs[27],
        )
        self.assertFalse(refs[27].startswith("jdk-27+"))

    def test_snapshot_must_be_immutable_commit(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "refs.tsv"
            path.write_text(
                "release\tstatus\tref_kind\tdonor_ref\treviewed_utc\n"
                "22\tRELEASED\tTAG\tjdk-22+36\t2026-10-04\n"
                "23\tRELEASED\tTAG\tjdk-23+37\t2026-10-04\n"
                "24\tRELEASED\tTAG\tjdk-24+36\t2026-10-04\n"
                "25\tRELEASED\tTAG\tjdk-25+36\t2026-10-04\n"
                "26\tRELEASED\tTAG\tjdk-26+35\t2026-10-04\n"
                "27\tIN_DEVELOPMENT_SNAPSHOT\tTAG\tjdk-27+35\t2026-10-04\n",
                encoding="utf-8",
            )
            with self.assertRaises(AssertionError):
                release_donor_refs.read(path)


if __name__ == "__main__":
    unittest.main()
