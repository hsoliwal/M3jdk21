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

    def test_active_jep_lanes_match_current_policy_and_receipts(self):
        root = Path(__file__).resolve().parents[2]
        backports = root / "m3" / "backports"

        catalogue = {
            int(row["jep"]): row
            for row in release_jep_authority.read_tsv(backports / "JEP_CATALOGUE.tsv")
        }
        queue = {
            row["identity"]: row
            for row in release_jep_authority.read_tsv(backports / "BACKPORT_WORK_QUEUE.tsv")
            if row["source_type"] == "JEP"
        }
        priority = {
            row["jep"]: row
            for row in release_jep_authority.read_tsv(
                backports / "POST21_PRIORITY_COMPATIBILITY.tsv"
            )
        }

        self.assertEqual("candidate-adapted", catalogue[458]["disposition"])
        self.assertEqual("candidate-high-risk", catalogue[467]["disposition"])
        self.assertEqual("hold-compat", catalogue[485]["disposition"])
        self.assertEqual("candidate", catalogue[493]["disposition"])

        self.assertEqual(
            "VERIFY_CURRENT_TREE_BUILD_JTREG_RUNTIME",
            queue["JEP-458"]["action"],
        )
        self.assertEqual("PR#116/#119", queue["JEP-458"]["dependency_or_commit"])
        self.assertEqual(
            "GENERATE_251_FILE_ATOMIC_CRATES_AND_REVIEW_LIBRARY_API_JOIN",
            queue["JEP-467"]["action"],
        )
        self.assertEqual("LIBRARY_API", queue["JEP-467"]["required_scope"])
        self.assertEqual(
            "VERIFY_EXPLICIT_OPT_IN_SE_API_EXTENSION",
            queue["JEP-485"]["action"],
        )
        self.assertEqual("hold-compat", queue["JEP-485"]["disposition"])
        self.assertEqual(
            "RUN_47_FILE_GENERATED_CRATE_MATERIALIZATION_AND_RUNTIME_IMAGE_PROOF",
            queue["JEP-493"]["action"],
        )
        self.assertEqual("MULTI_MODULE", queue["JEP-493"]["required_scope"])

        self.assertEqual("COMPATIBLE_TOOLING", priority["458"]["classification"])
        self.assertEqual("OPT_IN_SOURCE_TOOLING", priority["467"]["classification"])
        self.assertEqual("OPT_IN_SE_API_EXTENSION", priority["485"]["classification"])
        self.assertEqual("COMPATIBLE_TOOLING", priority["493"]["classification"])
        self.assertEqual("NO", priority["467"]["default_java21"])
        self.assertEqual("NO", priority["485"]["default_java21"])

        def receipt(path):
            rows = release_jep_authority.read_tsv(path)
            return {row["field"]: row["value"] for row in rows}

        jep458 = receipt(
            backports / "recipes/jep-458-current/CURRENT_TREE_RECEIPT.tsv"
        )
        self.assertEqual("0", jep458["recipe_replay_source_delta"])
        self.assertEqual(
            "REVIEWED_POSTIMAGES_ALREADY_PRESENT",
            jep458["current_tree_state"],
        )
        self.assertEqual("NOT_AUTHORIZED", jep458["promotion"])

        jep467 = receipt(
            backports / "recipes/jep-467-markdown/CURRENT_TREE_RECEIPT.tsv"
        )
        self.assertEqual("251", jep467["implementation_paths"])
        self.assertEqual("false", jep467["source_materialized"])
        paths467 = [
            line
            for line in (
                backports / "recipes/jep-467-markdown/PATHS.txt"
            ).read_text(encoding="utf-8").splitlines()
            if line
        ]
        self.assertEqual(251, len(paths467))
        self.assertIn(
            "test/langtools/tools/javac/processing/model/util/elements/"
            "TestGetDocComments.java",
            paths467,
        )

        jep493 = receipt(
            backports / "recipes/jep-493-runtime-image/CURRENT_TREE_RECEIPT.tsv"
        )
        self.assertEqual("47", jep493["implementation_paths"])
        self.assertEqual("PACKET_READY", jep493["packet_state"])
        self.assertEqual("false", jep493["source_materialized"])
        self.assertEqual("NOT_AUTHORIZED", jep493["promotion"])

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
