#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0

import csv
import importlib.util
import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
MODULE_PATH = pathlib.Path(__file__).with_name("backport_whole_programme_status.py")
SPEC = importlib.util.spec_from_file_location("m3_backport_whole_programme_status_tested", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


def rows(path: pathlib.Path):
    with path.open("r", encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle, delimiter="\t"))


class WholeProgrammeStatusTest(unittest.TestCase):
    def test_current_repository_contains_every_jep_and_jbs_seed_once(self):
        compiled = MODULE.compile_root(ROOT)
        by_id = {row.work_id: row for row in compiled}
        self.assertEqual(len(compiled), len(by_id))
        self.assertEqual(list(range(len(compiled))), [row.order for row in compiled])

        active_jeps = [
            row
            for row in rows(ROOT / "m3" / "backports" / "JEP_CATALOGUE.tsv")
            if not row["disposition"].strip().lower().startswith("superseded")
        ]
        seeds = rows(ROOT / "m3" / "backports" / "UPSTREAM_CHANGE_SEEDS.tsv")

        self.assertEqual(len(active_jeps), sum(row.source_kind == "JEP" for row in compiled))
        self.assertEqual(len(seeds), sum(row.source_kind == "UPSTREAM_CHANGE" for row in compiled))

        for row in active_jeps:
            item = by_id[f"JEP-{row['jep']}"]
            self.assertEqual("JEP", item.source_kind)
            self.assertEqual(int(row["jep"]), item.jep)

        for row in seeds:
            item = by_id[row["jbs"]]
            self.assertEqual("UPSTREAM_CHANGE", item.source_kind)
            self.assertEqual(0, item.jep)
            self.assertEqual(int(row["release"]), item.release)

    def test_known_concrete_jbs_backports_are_visible_to_execution_planning(self):
        by_id = {row.work_id: row for row in MODULE.compile_root(ROOT)}
        expected = {
            "JDK-8347112",
            "JDK-8357439",
            "JDK-8359706",
            "JDK-8364182",
            "JDK-8367584",
            "JDK-8368692",
            "JDK-8374808",
        }
        self.assertTrue(expected.issubset(by_id))
        for work_id in expected:
            self.assertEqual("UPSTREAM_CHANGE", by_id[work_id].source_kind)

    def test_summary_is_deterministic_and_grants_no_authority(self):
        first = MODULE.compile_root(ROOT)
        second = MODULE.compile_root(ROOT)
        self.assertEqual(first, second)
        self.assertEqual(MODULE.root(first), MODULE.root(second))
        summary = MODULE.summary(first)
        self.assertRegex(summary["root"], r"^[0-9a-f]{64}$")
        self.assertFalse(summary["product_write_authority"])
        self.assertFalse(summary["compatibility_authority"])
        self.assertFalse(summary["promotion_authority"])
        self.assertGreater(summary["sources"].get("JEP", 0), 0)
        self.assertGreater(summary["sources"].get("UPSTREAM_CHANGE", 0), 0)


if __name__ == "__main__":
    unittest.main()
