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
        self.assertEqual(9, len(authority[27]))
        self.assertEqual(
            85, sum(len(values) for values in authority.values())
        )

    def test_parse_ids_rejects_duplicate_unsorted_and_empty_lists(self):
        for value in ("", "423,423", "447,423"):
            with self.subTest(value=value):
                with self.assertRaises(AssertionError):
                    release_jep_authority.parse_ids(value)
        self.assertEqual(
            (423, 447), release_jep_authority.parse_ids("423,447")
        )

    def test_authority_requires_released_status_and_openjdk_source(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            backports = root / "m3" / "backports"
            backports.mkdir(parents=True)
            rows = [
                "release\tstatus\tofficial_feature_jep_ids\tsource\treviewed_utc"
            ]
            for release in range(22, 28):
                status = "IN_DEVELOPMENT_SNAPSHOT" if release == 27 else "RELEASED"
                source = (
                    "https://example.invalid/"
                    if release == 24
                    else f"https://openjdk.org/projects/jdk/{release}/"
                )
                rows.append(
                    f"{release}\t{status}\t{400 + release}\t{source}\t2026-10-06"
                )
            (backports / "RELEASE_JEP_AUTHORITY.tsv").write_text(
                "\n".join(rows) + "\n", encoding="utf-8"
            )
            with self.assertRaises(AssertionError):
                release_jep_authority.authority(root)

    def test_catalogue_missing_authority_row_fails_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            backports = root / "m3" / "backports"
            backports.mkdir(parents=True)
            source_root = Path(__file__).resolve().parent
            repo_root = source_root.parents[1]
            authority_text = (
                repo_root / "m3" / "backports" / "RELEASE_JEP_AUTHORITY.tsv"
            ).read_text(encoding="utf-8")
            catalogue_lines = (
                repo_root / "m3" / "backports" / "JEP_CATALOGUE.tsv"
            ).read_text(encoding="utf-8").splitlines()
            catalogue_lines = [
                line
                for line in catalogue_lines
                if not line.startswith("24\t483\t")
            ]
            (backports / "RELEASE_JEP_AUTHORITY.tsv").write_text(
                authority_text, encoding="utf-8"
            )
            (backports / "JEP_CATALOGUE.tsv").write_text(
                "\n".join(catalogue_lines) + "\n", encoding="utf-8"
            )
            with self.assertRaisesRegex(
                AssertionError, r"missing=\[483\]"
            ):
                release_jep_authority.verify_repository_authority(root)


if __name__ == "__main__":
    unittest.main()
