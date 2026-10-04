# SPDX-License-Identifier: Apache-2.0
import tempfile
import unittest
from pathlib import Path

import release_jep_authority


class ReleaseJepAuthorityTest(unittest.TestCase):
    def test_repository_authority_is_exact(self):
        root = Path(__file__).resolve().parents[2]
        authority = release_jep_authority.verify_repository_authority(root)
        self.assertEqual(tuple(range(22, 28)), tuple(sorted(authority)))
        self.assertEqual(24, len(authority[24]))
        self.assertIn(404, authority[24])
        self.assertIn(483, authority[24])
        self.assertEqual(18, len(authority[25]))
        self.assertIn(521, authority[25])
        self.assertEqual(85, sum(len(values) for values in authority.values()))

    def test_parse_ids_rejects_duplicate_unsorted_and_empty_lists(self):
        for value in ("", "423,423", "447,423"):
            with self.subTest(value=value):
                with self.assertRaises(AssertionError):
                    release_jep_authority.parse_ids(value)
        self.assertEqual((423, 447), release_jep_authority.parse_ids("423,447"))

    def test_authority_rejects_non_openjdk_source(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            backports = root / "m3" / "backports"
            backports.mkdir(parents=True)
            rows = ["release\tstatus\tofficial_feature_jep_ids\tsource\treviewed_utc"]
            for release in range(22, 28):
                status = "RELEASED"
                source = (
                    "https://example.invalid/"
                    if release == 24
                    else f"https://openjdk.org/projects/jdk/{release}/"
                )
                rows.append(f"{release}\t{status}\t{400 + release}\t{source}\t2026-10-04")
            (backports / "RELEASE_JEP_AUTHORITY.tsv").write_text(
                "\n".join(rows) + "\n", encoding="utf-8"
            )
            with self.assertRaises(AssertionError):
                release_jep_authority.authority(root)


if __name__ == "__main__":
    unittest.main()
