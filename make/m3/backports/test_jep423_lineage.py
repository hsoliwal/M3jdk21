# SPDX-License-Identifier: Apache-2.0
import importlib.util
import pathlib
import unittest

PACKET = pathlib.Path(__file__).resolve().parent / "recipes" / "jep-423-region-pinning"
SPEC = importlib.util.spec_from_file_location("j423_lineage", PACKET / "validate_lineage.py")
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class J423LineageTest(unittest.TestCase):
    def test_complete_required_lineage_and_path_closure(self):
        summary = MODULE.validate()
        self.assertEqual(3, summary["lineage_commits"])
        self.assertEqual(2, summary["preserved_java21_deletions"])
        self.assertEqual(64, summary["unique_paths"])
        self.assertEqual(47, summary["product_paths"])
        self.assertEqual(17, summary["test_paths"])
        self.assertEqual(62, len(MODULE.path_list(MODULE.CANDIDATES)))

    def test_cumulative_packet_and_evidence_cover_every_mutation_candidate(self):
        packet = MODULE.rows(PACKET / "packet.tsv")
        evidence = MODULE.rows(PACKET / "atom-evidence.tsv")

        file_rows = [row for row in packet if row["scope"] == "FILE"]
        self.assertEqual(62, len(file_rows))
        file_ids = {row["atom_id"] for row in file_rows}

        evidence_file_ids = {
            row["atom_id"]
            for row in evidence
            if row["atom_id"].startswith("file-")
        }
        self.assertEqual(file_ids, evidence_file_ids)

        g1_core = next(row for row in packet if row["atom_id"] == "g1-core")
        g1_dependencies = {
            value for value in g1_core["depends_on"].split(",") if value
        }
        self.assertEqual(43, len(g1_dependencies))
        self.assertTrue(
            {
                "file-src-hotspot-share-gc-g1-g1barrierset-cpp",
                "file-src-hotspot-share-gc-g1-g1regionpincache-hpp",
                "file-src-hotspot-share-gc-g1-g1regionpincache-inline-hpp",
                "file-src-hotspot-share-gc-g1-g1threadlocaldata-hpp",
                "file-src-hotspot-share-gc-g1-g1younggcpreevacuatetasks-cpp",
            }.issubset(g1_dependencies)
        )

    def test_followup_repairs_are_required(self):
        rows = MODULE.rows(MODULE.LINEAGE)
        roles = [row["role"] for row in rows]
        self.assertEqual(
            [
                "JEP_IMPLEMENTATION",
                "PIN_COUNT_OVERFLOW_REPAIR",
                "PIN_CACHE_REGRESSION_REPAIR",
            ],
            roles,
        )
        self.assertTrue(all(row["required"] == "true" for row in rows))
        self.assertTrue(all(row["promotion_authority"] == "false" for row in rows))


if __name__ == "__main__":
    unittest.main()
