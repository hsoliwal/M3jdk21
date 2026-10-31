#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import csv
import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace


def load_module():
    path = Path(__file__).with_name("backport_execution_waves.py")
    spec = importlib.util.spec_from_file_location("m3_backport_execution_waves", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def atom(
    order: int,
    atom_id: str,
    phase: str,
    *,
    work_id: str = "JEP-999",
    scope: str = "FILE",
    executable: bool = True,
    depends_on: str = "",
):
    return SimpleNamespace(
        order=order,
        work_id=work_id,
        atom_id=atom_id,
        phase=phase,
        scope=scope,
        executable=executable,
        proof_lane="CORE_LIBRARY",
        recipe_ref="j999",
        depends_on=depends_on,
    )


class BackportExecutionWavesTest(unittest.TestCase):
    def setUp(self) -> None:
        self.w = load_module()

    def test_independent_file_atoms_fan_out_and_dependencies_advance_waves(self) -> None:
        atoms = [
            atom(0, "a.inventory", "INVENTORY", work_id="JEP-1"),
            atom(1, "a.recipe", "RECIPE", work_id="JEP-1", depends_on="a.inventory"),
            atom(2, "b.inventory", "INVENTORY", work_id="JEP-2"),
            atom(3, "b.recipe", "RECIPE", work_id="JEP-2", depends_on="b.inventory"),
        ]
        items = self.w.plan(atoms, {})
        by_id = {item.atom_id: item for item in items}

        self.assertEqual(0, by_id["a.inventory"].wave)
        self.assertEqual(0, by_id["b.inventory"].wave)
        self.assertEqual(1, by_id["a.recipe"].wave)
        self.assertEqual(1, by_id["b.recipe"].wave)
        self.assertTrue(all(item.execution_state == self.w.READY for item in items))

    def test_broader_scope_is_held_until_exact_atom_approval(self) -> None:
        atoms = [
            atom(0, "a.inventory", "INVENTORY"),
            atom(
                1,
                "a.recipe",
                "RECIPE",
                scope="MODULE",
                depends_on="a.inventory",
            ),
            atom(
                2,
                "a.compile",
                "COMPILE",
                scope="MODULE",
                depends_on="a.recipe",
            ),
        ]

        unapproved = {item.atom_id: item for item in self.w.plan(atoms, {})}
        self.assertEqual(self.w.SCOPE_APPROVAL_HOLD, unapproved["a.recipe"].execution_state)
        self.assertEqual(self.w.SCOPE_APPROVAL_HOLD, unapproved["a.compile"].execution_state)
        self.assertFalse(unapproved["a.compile"].scope_approved)
        self.assertEqual(-1, unapproved["a.compile"].wave)

        # Approve only the child: its held parent must still block execution.
        held = {
            item.atom_id: item
            for item in self.w.plan(atoms, {"a.compile": "review/child-only"})
        }
        self.assertEqual(self.w.SCOPE_APPROVAL_HOLD, held["a.recipe"].execution_state)
        self.assertEqual(self.w.DEPENDENCY_HOLD, held["a.compile"].execution_state)
        self.assertEqual(-1, held["a.recipe"].wave)
        self.assertTrue(held["a.compile"].scope_approved)
        self.assertEqual(-1, held["a.compile"].wave)

        approved = {
            item.atom_id: item
            for item in self.w.plan(
                atoms,
                {"a.recipe": "review/1", "a.compile": "review/2"},
            )
        }
        self.assertEqual(self.w.READY, approved["a.recipe"].execution_state)
        self.assertEqual(1, approved["a.recipe"].wave)
        self.assertEqual(2, approved["a.compile"].wave)

    def test_non_executable_evidence_never_enters_a_wave(self) -> None:
        items = self.w.plan(
            [atom(0, "held.inventory", "INVENTORY", executable=False)],
            {},
        )
        self.assertEqual(self.w.EVIDENCE_HOLD, items[0].execution_state)
        self.assertEqual(-1, items[0].wave)
        self.assertFalse(items[0].scope_approved)

    def test_scheduler_projections_are_deterministic_and_authority_free(self) -> None:
        items = self.w.plan([atom(0, "JEP-1.Inventory", "INVENTORY")], {})
        first = self.w.summary(items, {})
        replay = self.w.summary(
            self.w.plan([atom(0, "JEP-1.Inventory", "INVENTORY")], {}),
            {},
        )

        self.assertEqual(first["root"], replay["root"])
        self.assertFalse(first["mutation_authority"])
        self.assertFalse(first["promotion_authority"])
        self.assertFalse(first["compatibility_authority"])
        self.assertFalse(first["scheduler_projections"]["camel"]["authority"])
        self.assertFalse(first["scheduler_projections"]["airflow"]["authority"])
        self.assertFalse(first["scheduler_projections"]["drools"]["authority"])
        self.assertEqual("m3.backport.jep_1_inventory", items[0].camel_route_id)
        self.assertEqual("m3_backport_jep_1_inventory", items[0].airflow_task_id)
        self.assertEqual("M3_BACKPORT_JEP_1_INVENTORY", items[0].drools_fact_id)

    def test_header_only_approval_ledger_is_valid_but_invalid_headers_fail(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "approvals.tsv"
            path.write_text("atom_id\tapproved\tproof_ref\n", encoding="utf-8")
            self.assertEqual({}, self.w.read_approvals(path))
            path.write_text("atom_id\tapproved\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "missing columns"):
                self.w.read_approvals(path)

    def test_duplicate_approval_ids_are_rejected_even_when_earlier_row_is_false(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "approvals.tsv"
            path.write_text(
                "atom_id\tapproved\tproof_ref\n"
                "a.recipe\tfalse\t\n"
                "a.recipe\ttrue\treview/real\n",
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "duplicate approval atom_id"):
                self.w.read_approvals(path)

    def test_malformed_approval_record_is_not_accepted_as_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "approvals.tsv"
            path.write_text(
                "atom_id\tapproved\tproof_ref\n"
                "a.recipe\ttrue\treview/real\textra\n",
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "malformed approval ledger"):
                self.w.read_approvals(path)

    def test_nonexecutable_file_cannot_inherit_scope_approval(self) -> None:
        held = atom(0, "held.file", "INVENTORY", executable=False)
        item = self.w.plan([held], {"held.file": "review/evidence-only"})[0]
        self.assertEqual(self.w.EVIDENCE_HOLD, item.execution_state)
        self.assertFalse(item.scope_approved)
        self.assertEqual(-1, item.wave)

    def test_collision_after_scheduler_id_normalization_fails_closed(self) -> None:
        atoms = [
            atom(0, "jep-458.file", "RECIPE"),
            atom(1, "jep_458.file", "RECIPE"),
        ]
        with self.assertRaisesRegex(ValueError, "scheduler identity collision"):
            self.w.plan(atoms, {})

    def test_direct_planner_rejects_unknown_approvals_and_invalid_scope(self) -> None:
        atoms = [atom(0, "jep-458.inventory", "INVENTORY")]
        with self.assertRaisesRegex(ValueError, "unknown atoms"):
            self.w.plan(atoms, {"unrelated.atom": "review/other"})
        with self.assertRaisesRegex(ValueError, "unrecognized edit scope"):
            self.w.plan([atom(0, "jep-458.inventory", "INVENTORY", scope="REACTOR")], {})

    def test_direct_planner_rejects_order_gaps_and_cross_work_dependencies(self) -> None:
        with self.assertRaisesRegex(ValueError, "non-contiguous"):
            self.w.plan([atom(3, "orphan.file", "RECIPE")], {})
        atoms = [
            atom(0, "jep-423.recipe", "RECIPE", work_id="JEP-423"),
            atom(1, "jep-475.recipe", "RECIPE", work_id="JEP-475",
                 depends_on="jep-423.recipe"),
        ]
        with self.assertRaisesRegex(ValueError, "cross-work-item dependency"):
            self.w.plan(atoms, {})

    def test_unapproved_g1_multimodule_backport_is_held_with_dependents(self) -> None:
        atoms = [
            atom(0, "jep475.inventory", "INVENTORY", work_id="JEP-475"),
            atom(1, "jep475.recipe", "RECIPE", work_id="JEP-475",
                 scope="MULTI_MODULE", depends_on="jep475.inventory"),
            atom(2, "jep475.compile", "COMPILE", work_id="JEP-475",
                 scope="MULTI_MODULE", depends_on="jep475.recipe"),
        ]
        items = {item.atom_id: item for item in self.w.plan(atoms, {})}
        self.assertEqual(self.w.READY, items["jep475.inventory"].execution_state)
        self.assertEqual(self.w.SCOPE_APPROVAL_HOLD, items["jep475.recipe"].execution_state)
        self.assertEqual(self.w.SCOPE_APPROVAL_HOLD, items["jep475.compile"].execution_state)
        self.assertEqual(-1, items["jep475.recipe"].wave)
        self.assertEqual(-1, items["jep475.compile"].wave)

    def test_approval_ledger_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "approvals.tsv"
            path.write_text(
                "atom_id\tapproved\tproof_ref\n"
                "a.recipe\ttrue\t\n",
                encoding="utf-8",
            )
            with self.assertRaises(ValueError):
                self.w.read_approvals(path)

            path.write_text(
                "atom_id\tapproved\tproof_ref\n"
                "a.recipe\tmaybe\treview/1\n",
                encoding="utf-8",
            )
            with self.assertRaises(ValueError):
                self.w.read_approvals(path)

    def test_emitted_waves_are_real_tsv_rows_and_single_terminated_json(self) -> None:
        items = self.w.plan(
            [
                atom(0, "file.alpha", "RECIPE"),
                atom(1, "file.beta", "RECIPE", depends_on="file.alpha"),
            ],
            {},
        )
        with tempfile.TemporaryDirectory() as temp:
            tsv = Path(temp) / "waves.tsv"
            json_path = Path(temp) / "waves.json"
            self.w.write_tsv(items, tsv)
            self.w.write_json(items, {}, json_path)

            raw_tsv = tsv.read_bytes()
            self.assertEqual(3, raw_tsv.count(b"\n"), "header + two physical TSV records")
            self.assertTrue(raw_tsv.endswith(b"\n"))
            self.assertNotIn(b"\\n", raw_tsv, "no escaped row separator")
            with tsv.open("r", encoding="utf-8", newline="") as handle:
                parsed = list(csv.DictReader(handle, delimiter="\t"))
            self.assertEqual(2, len(parsed))
            self.assertEqual(["file.alpha", "file.beta"],
                             [row["atom_id"] for row in parsed])
            self.assertEqual(["0", "1"], [row["wave"] for row in parsed])

            raw_json = json_path.read_bytes()
            self.assertTrue(raw_json.endswith(b"\n"))
            self.assertFalse(raw_json.endswith(b"\\n"))
            self.assertEqual(1, len(raw_json) - len(raw_json.rstrip(b"\n")))
            document = json.loads(raw_json.decode("utf-8"))
            self.assertEqual(self.w.root(items), document["root"])
            self.assertEqual(["file.alpha", "file.beta"],
                             [row["atom_id"] for row in document["items"]])

    def test_json_output_binds_items_and_root(self) -> None:
        items = self.w.plan([atom(0, "a.inventory", "INVENTORY")], {})
        with tempfile.TemporaryDirectory() as temp:
            out = Path(temp) / "waves.json"
            self.w.write_json(items, {}, out)
            payload = json.loads(out.read_text(encoding="utf-8"))
            self.assertEqual("M3_BACKPORT_EXECUTION_WAVES_V1", payload["schema"])
            self.assertEqual(self.w.root(items), payload["root"])
            self.assertEqual("a.inventory", payload["items"][0]["atom_id"])


if __name__ == "__main__":
    unittest.main()
