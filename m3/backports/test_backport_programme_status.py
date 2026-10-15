#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace


def load_module():
    path = Path(__file__).with_name("backport_programme_status.py")
    spec = importlib.util.spec_from_file_location("m3_backport_programme_status", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def row(jep: int, disposition: str, *, successor: str = ""):
    return {
        "release": "24",
        "jep": str(jep),
        "title": f"JEP {jep}",
        "domain": "library",
        "disposition": disposition,
        "reason": "reason",
        "superseded_by": successor,
    }


def atom(
    jep: int,
    state: str,
    recipe_ref: str = "",
    *,
    number: int = 0,
    scope: str = "FILE",
    executable: bool = True,
):
    return SimpleNamespace(
        work_id=f"JEP-{jep}",
        state=state,
        recipe_ref=recipe_ref,
        atom_id=f"jep_{jep}.atom_{number}",
        scope=scope,
        executable=executable,
    )


def wave(jep: int, execution_state: str, *, number: int = 0, scope: str = "FILE"):
    return SimpleNamespace(
        work_id=f"JEP-{jep}",
        atom_id=f"jep_{jep}.atom_{number}",
        scope=scope,
        execution_state=execution_state,
    )


class BackportProgrammeStatusTest(unittest.TestCase):
    def setUp(self) -> None:
        self.s = load_module()

    def test_author_recipe_is_explicit(self) -> None:
        result = self.s.classify(row(485, "candidate"), [atom(485, "AUTHOR_RECIPE")], [])
        self.assertEqual(self.s.AUTHOR_RECIPE, result.programme_state)
        self.assertEqual("AUTHOR_TEST_AND_FIXED_POINT_RECIPE", result.next_action)

    def test_materialized_file_recipe_can_be_ready(self) -> None:
        result = self.s.classify(
            row(485, "candidate"),
            [atom(485, "MATERIALIZED_RECIPE", "jep-485-stream-gatherers", number=0),
             atom(485, "MATERIALIZED_RECIPE", "jep-485-stream-gatherers", number=1)],
            [wave(485, "READY", number=0), wave(485, "READY", number=1)],
        )
        self.assertEqual(self.s.FILE_READY, result.programme_state)
        self.assertEqual(2, result.ready_atoms)
        self.assertEqual("jep-485-stream-gatherers", result.recipe_refs)

    def test_scope_hold_is_never_reported_ready(self) -> None:
        result = self.s.classify(
            row(475, "candidate-high-risk"),
            [atom(475, "MATERIALIZED_RECIPE", "jep-475-g1", number=i, scope="MULTI_MODULE")
             for i in range(3)],
            [wave(475, "READY", number=0, scope="MULTI_MODULE"),
             wave(475, "SCOPE_APPROVAL_HOLD", number=1, scope="MULTI_MODULE"),
             wave(475, "DEPENDENCY_HOLD", number=2, scope="MULTI_MODULE")],
        )
        self.assertEqual(self.s.SCOPE_HOLD, result.programme_state)
        self.assertEqual(1, result.ready_atoms)
        self.assertEqual(2, result.held_atoms)

    def test_truncated_ready_wave_set_is_not_file_ready(self) -> None:
        result = self.s.classify(
            row(485, "candidate"),
            [atom(485, "MATERIALIZED_RECIPE", number=0),
             atom(485, "MATERIALIZED_RECIPE", number=1)],
            [wave(485, "READY", number=0)],
        )
        self.assertEqual(self.s.EVIDENCE_HOLD, result.programme_state)
        self.assertEqual("REPAIR_BACKPORT_DAG_WAVE_ALIGNMENT", result.next_action)

    def test_duplicate_or_stale_wave_identity_is_not_file_ready(self) -> None:
        atoms = [atom(485, "MATERIALIZED_RECIPE", number=0),
                 atom(485, "MATERIALIZED_RECIPE", number=1)]
        for waves in (
            [wave(485, "READY", number=0), wave(485, "READY", number=0)],
            [wave(485, "READY", number=0), wave(485, "READY", number=999)],
        ):
            with self.subTest(waves=waves):
                result = self.s.classify(row(485, "candidate"), atoms, waves)
                self.assertEqual(self.s.EVIDENCE_HOLD, result.programme_state)

    def test_scope_approved_module_is_not_mislabeled_file_ready(self) -> None:
        result = self.s.classify(
            row(485, "candidate"),
            [atom(485, "MATERIALIZED_RECIPE", scope="MODULE")],
            [wave(485, "READY", scope="MODULE")],
        )
        self.assertEqual(self.s.SCOPE_READY, result.programme_state)
        self.assertEqual(
            "EXECUTE_SCOPE_APPROVED_ATOMS_AND_PROVE_JAVA21_CONTRACT", result.next_action
        )

    def test_wave_scope_mismatch_unknown_state_and_nonexecutable_ready_hold(self) -> None:
        fixtures = (
            ([atom(485, "MATERIALIZED_RECIPE", scope="MODULE")],
             [wave(485, "READY", scope="FILE")]),
            ([atom(485, "MATERIALIZED_RECIPE")],
             [wave(485, "UNRECOGNIZED")]),
            ([atom(485, "MATERIALIZED_RECIPE", executable=False)],
             [wave(485, "READY")]),
        )
        for atoms, waves in fixtures:
            with self.subTest(atoms=atoms, waves=waves):
                result = self.s.classify(row(485, "candidate"), atoms, waves)
                self.assertEqual(self.s.EVIDENCE_HOLD, result.programme_state)

    def test_hold_and_reject_remain_compatibility_decisions(self) -> None:
        held = self.s.classify(row(531, "hold-preview"), [], [])
        rejected = self.s.classify(row(479, "reject-compat"), [], [])
        self.assertEqual(self.s.HOLD, held.programme_state)
        self.assertEqual(self.s.REJECT, rejected.programme_state)

    def test_superseded_rows_are_omitted_from_active_programme(self) -> None:
        rows = self.s.compile_rows(
            [row(457, "superseded", successor="484"), row(484, "candidate")],
            [atom(484, "AUTHOR_RECIPE")],
            [],
        )
        self.assertEqual([484], [value.jep for value in rows])

    def test_serialization_is_deterministic_and_authority_free(self) -> None:
        rows = self.s.compile_rows(
            [row(485, "candidate"), row(479, "reject-compat")],
            [atom(485, "AUTHOR_RECIPE")],
            [],
        )
        first = self.s.summary(rows)
        replay = self.s.summary(list(rows))
        self.assertEqual(first["root"], replay["root"])
        self.assertFalse(first["mutation_authority"])
        self.assertFalse(first["promotion_authority"])
        self.assertFalse(first["compatibility_authority"])

        with tempfile.TemporaryDirectory() as temp:
            tsv = Path(temp) / "status.tsv"
            json_path = Path(temp) / "status.json"
            self.s.write_tsv(rows, tsv)
            self.s.write_json(rows, json_path)
            self.assertTrue(tsv.read_bytes().endswith(b"\n"))
            self.assertTrue(json_path.read_bytes().endswith(b"\n"))


if __name__ == "__main__":
    unittest.main()
