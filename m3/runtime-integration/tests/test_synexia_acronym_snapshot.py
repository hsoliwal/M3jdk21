"""Replay the pinned 40-entry AcronymLexicon snapshot through m3lex-acronym-v1."""
from __future__ import annotations
import csv
import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).parents[2]
SNAPSHOT = ROOT / "lexicon" / "synexia-acronyms.tsv"
MODULE = Path(__file__).parents[1] / "synexia-acronym-sidecar.py"
SPEC = importlib.util.spec_from_file_location("m3_acronym_sidecar", MODULE)
assert SPEC and SPEC.loader
SIDECAR = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SIDECAR)

class AcronymSnapshotTest(unittest.TestCase):
    def test_pinned_snapshot_replays_and_preserves_domains(self):
        with SNAPSHOT.open(encoding="utf-8", newline="") as stream:
            rows = list(csv.DictReader(stream, delimiter="\t"))
        self.assertEqual(len(rows), 40)
        self.assertEqual(
            {domain: sum(row["domain"] == domain for row in rows) for domain in
             ("computing", "networking", "java", "data", "standards")},
            {"computing": 12, "networking": 12, "java": 7, "data": 6, "standards": 3},
        )
        files = SIDECAR.render(rows)
        self.assertEqual(SIDECAR.verify(files), {"rows": 40, "files": 2})
        self.assertEqual([row["acronym"] for row in rows],
                         sorted(row["acronym"] for row in rows))
        self.assertEqual({row["record_id"] for row in rows},
                         {row["acronym"] for row in rows})

if __name__ == "__main__":
    unittest.main()
