#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
import csv
import io
import tempfile
import unittest
from pathlib import Path

import fork_queue


class ForkQueueTest(unittest.TestCase):
    def test_checked_in_inventory_is_pinned_sorted_and_never_selected(self):
        rows = fork_queue.read(Path(__file__).with_name("forks.tsv"))
        queued = fork_queue.queue(rows)
        self.assertEqual(5, len(queued))
        self.assertEqual("F-SIMDSORT", queued[0].candidate_id)
        self.assertTrue(all(len(row.commit) == 40 for row in queued))
        self.assertTrue(all(row.status in fork_queue.ALLOWED_STATES for row in queued))
        output = io.StringIO()
        fork_queue.write_tsv(rows, output)
        parsed = list(csv.DictReader(io.StringIO(output.getvalue()), delimiter="\t"))
        self.assertTrue(all(row["selected_for_distribution"] == "false" for row in parsed))

    def test_priority_order_is_deterministic(self):
        rows = fork_queue.read(Path(__file__).with_name("forks.tsv"))
        first = [row.candidate_id for row in fork_queue.queue(rows)]
        second = [row.candidate_id for row in fork_queue.queue(list(reversed(rows)))]
        self.assertEqual(first, second)

    def test_duplicate_candidate_refuses(self):
        row = {
            "candidate_id": "F-X", "donor": "a/b", "commit": "a" * 40,
            "capability": "x", "plane": "CORE_BACKPORT", "license": "Apache-2.0",
            "join_keys": "X", "status": "PENDING_COMPATIBILITY_PROOF",
            "priority": "1", "scope_floor": "FILE", "next_proof": "prove",
        }
        with self.assertRaisesRegex(ValueError, "duplicate"):
            fork_queue.parse([row, row])

    def test_floating_or_short_revision_refuses(self):
        row = {
            "candidate_id": "F-X", "donor": "a/b", "commit": "master",
            "capability": "x", "plane": "CORE_BACKPORT", "license": "Apache-2.0",
            "join_keys": "X", "status": "PENDING_COMPATIBILITY_PROOF",
            "priority": "1", "scope_floor": "FILE", "next_proof": "prove",
        }
        with self.assertRaisesRegex(ValueError, "commit"):
            fork_queue.parse([row])

    def test_unknown_status_refuses(self):
        row = {
            "candidate_id": "F-X", "donor": "a/b", "commit": "b" * 40,
            "capability": "x", "plane": "CORE_BACKPORT", "license": "Apache-2.0",
            "join_keys": "X", "status": "ADMITTED",
            "priority": "1", "scope_floor": "FILE", "next_proof": "prove",
        }
        with self.assertRaisesRegex(ValueError, "status"):
            fork_queue.parse([row])


if __name__ == "__main__":
    unittest.main()
