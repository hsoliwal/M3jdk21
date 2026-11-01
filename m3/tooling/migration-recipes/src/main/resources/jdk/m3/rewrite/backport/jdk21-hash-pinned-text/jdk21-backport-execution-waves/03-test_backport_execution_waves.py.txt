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

        held = {item.atom_id: item for item in self.w.plan(atoms, {})}
        self.assertEqual(self.w.SCOPE_APPROVAL_HOLD, held["a.recipe"].execution_state)
        self.assertEqual(self.w.DEPENDENCY_HOLD, held["a.compile"].execution_state)
        self.assertEqual(-1, held["a.recipe"].wave)

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
            self.assertEqual(3, raw_tsv.count(b"\\n"), "header + two physical TSV records")
            self.assertTrue(raw_tsv.endswith(b"\\n"))
            self.assertNotIn(b"\\\\n", raw_tsv, "no escaped row separator")
            with tsv.open("r", encoding="utf-8", newline="") as handle:
                parsed = list(csv.DictReader(handle, delimiter="\\t"))
            self.assertEqual(2, len(parsed))
            self.assertEqual(["file.alpha", "file.beta"],
                             [row["atom_id"] for row in parsed])
            self.assertEqual(["0", "1"], [row["wave"] for row in parsed])

            raw_json = json_path.read_bytes()
            self.assertTrue(raw_json.endswith(b"\\n"))
            self.assertFalse(raw_json.endswith(b"\\\\n"))
            self.assertEqual(1, len(raw_json) - len(raw_json.rstrip(b"\\n"))))
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
