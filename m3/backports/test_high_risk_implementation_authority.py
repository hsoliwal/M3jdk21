#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


MODULE_PATH = Path(__file__).with_name("high_risk_implementation_authority.py")
SPEC = importlib.util.spec_from_file_location("m3_high_risk_authority", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
authority = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = authority
SPEC.loader.exec_module(authority)


class HighRiskImplementationAuthorityTest(unittest.TestCase):
    ROOT = Path(__file__).resolve().parents[2]

    def test_ledger_exactly_covers_high_risk_no_recipe_residue_except_dedicated_packets(self) -> None:
        rows = authority.verify_repository_coverage(self.ROOT)
        self.assertEqual(
            [483, 506, 509, 514, 515, 516, 517, 518, 520, 521, 522, 527, 534, 536],
            [row.jep for row in rows],
        )
        self.assertEqual(14, len(rows))
        self.assertTrue(all(row.authority_status == authority.STATUS for row in rows))

    def test_known_lineage_and_dependency_bindings_are_preserved(self) -> None:
        by_jep = {row.jep: row for row in authority.load_authority(self.ROOT)}

        self.assertEqual(
            "ace70a6d6aca619da34b2f9cac2586cc88cefb5a",
            by_jep[509].implementation_commit,
        )
        self.assertIn("REDO", by_jep[509].lineage_note)
        self.assertEqual((483,), by_jep[514].dependency_jep_ids)
        self.assertEqual((483,), by_jep[515].dependency_jep_ids)
        self.assertEqual((483, 514, 515), by_jep[516].dependency_jep_ids)
        self.assertEqual((404,), by_jep[521].dependency_jep_ids)

    def test_matrix_is_stable_and_authority_only(self) -> None:
        first = authority.matrix(self.ROOT)
        replay = authority.matrix(self.ROOT)
        self.assertEqual(first, replay)
        self.assertEqual(14, len(first["include"]))
        encoded = json.dumps(first, sort_keys=True, separators=(",", ":"))
        self.assertNotIn("compatibility", encoded.lower())
        self.assertNotIn("promotion", encoded.lower())

    def test_git_verifier_checks_ancestor_and_exact_changed_path_count(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            repo = Path(temp)
            subprocess.run(["git", "-C", str(repo), "init", "-q"], check=True)
            subprocess.run(["git", "-C", str(repo), "config", "user.name", "M3 Test"], check=True)
            subprocess.run(["git", "-C", str(repo), "config", "user.email", "m3@example.invalid"], check=True)

            (repo / "a.txt").write_text("one\n", encoding="utf-8")
            subprocess.run(["git", "-C", str(repo), "add", "a.txt"], check=True)
            subprocess.run(["git", "-C", str(repo), "commit", "-qm", "base"], check=True)

            (repo / "a.txt").write_text("two\n", encoding="utf-8")
            (repo / "b.txt").write_text("new\n", encoding="utf-8")
            subprocess.run(["git", "-C", str(repo), "add", "a.txt", "b.txt"], check=True)
            subprocess.run(["git", "-C", str(repo), "commit", "-qm", "implementation"], check=True)
            commit = subprocess.check_output(
                ["git", "-C", str(repo), "rev-parse", "HEAD"], text=True
            ).strip()
            subprocess.run(["git", "-C", str(repo), "tag", "jdk-24+36"], check=True)

            row = authority.Authority(
                jep=999,
                release=24,
                title="fixture",
                implementation_jbs=1,
                implementation_commit=commit,
                implementation_date="2026-01-01",
                implementation_path_count=2,
                donor_tag="jdk-24+36",
                dependency_jep_ids=(),
                authority_status=authority.STATUS,
                lineage_note="",
            )
            self.assertEqual(["a.txt", "b.txt"], authority.verify_upstream_commit(repo, row))

            drift = authority.Authority(
                **{**row.__dict__, "implementation_path_count": 3}
            )
            with self.assertRaisesRegex(ValueError, "path count drift"):
                authority.verify_upstream_commit(repo, drift)

    def test_noncanonical_paths_fail_closed(self) -> None:
        for path in ("../x", "/x", "a\\b", "a/../b", "a//b"):
            with self.subTest(path=path):
                with self.assertRaises(ValueError):
                    authority.canonical_path(path)


if __name__ == "__main__":
    unittest.main()
