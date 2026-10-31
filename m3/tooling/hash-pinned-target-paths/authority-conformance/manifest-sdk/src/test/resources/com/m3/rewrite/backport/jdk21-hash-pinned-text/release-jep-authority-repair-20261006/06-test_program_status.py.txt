# SPDX-License-Identifier: Apache-2.0
import unittest
from pathlib import Path

import program_status

class ProgramStatusTest(unittest.TestCase):
    def test_disposition_classification_is_total_for_known_states(self):
        for value in program_status.PENDING_JEP:
            self.assertEqual(
                "PENDING_PROOF_OR_IMPLEMENTATION",
                program_status.classify_jep(value),
            )
        for value in program_status.DECIDED_JEP:
            self.assertEqual(
                "DECIDED_NO_DIRECT_BACKPORT",
                program_status.classify_jep(value),
            )
        self.assertEqual("ADMITTED", program_status.classify_seed("admitted"))
        for value in program_status.PENDING_SEED:
            self.assertEqual(
                "PENDING_PROOF_OR_IMPLEMENTATION",
                program_status.classify_seed(value),
            )
        with self.assertRaises(ValueError):
            program_status.classify_jep("mystery")
        with self.assertRaises(ValueError):
            program_status.classify_seed("mystery")

    def test_repository_snapshot_has_locked_denominators(self):
        root = Path(__file__).resolve().parents[2]
        data = program_status.snapshot(root)
        self.assertEqual(85, data["jep_rows"])
        self.assertEqual(85, sum(data["jep_states"].values()))
        self.assertEqual(85, data["jep_unique_rows"])
        self.assertEqual(85, data["jep_authority_rows"])
        self.assertEqual(34, data["priority_jep_rows"])
        self.assertEqual([], data["priority_missing_from_catalogue"])
        self.assertEqual(22, data["released_jdk_floor"])
        self.assertEqual(27, data["released_jdk_ceiling"])
        self.assertEqual(13, data["seed_rows"])
        self.assertEqual(13, sum(data["seed_states"].values()))
        self.assertEqual(12, data["community_fork_rows"])
        self.assertFalse(data["community_fork_source_copy_authority"])
        self.assertFalse(data["community_fork_selected_for_distribution"])
        self.assertFalse(data["completion_claim"])
        self.assertGreaterEqual(data["materialized_packet_count"], 2)

    def test_tsv_is_deterministic(self):
        root = Path(__file__).resolve().parents[2]
        data = program_status.snapshot(root)
        first = program_status.render_tsv(data)
        second = program_status.render_tsv(data)
        self.assertEqual(first, second)
        self.assertTrue(first.startswith("kind\tstate\tcount\n"))
        self.assertIn("jep\ttotal\t85\n", first)
        self.assertIn("jep\tunique\t85\n", first)
        self.assertIn("jep-priority\ttotal\t34\n", first)
        self.assertIn("community-fork\ttotal\t12\n", first)

if __name__ == "__main__":
    unittest.main()
