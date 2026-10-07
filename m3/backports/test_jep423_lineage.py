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
        self.assertGreater(summary["unique_paths"], 59)
        self.assertGreater(summary["product_paths"], 40)
        self.assertGreater(summary["test_paths"], 15)

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
