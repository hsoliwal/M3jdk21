#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path


def load_plan_module():
    path = Path(__file__).with_name("plan.py")
    spec = importlib.util.spec_from_file_location("m3_backport_plan", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class BackportPlanTest(unittest.TestCase):
    def setUp(self) -> None:
        self.plan = load_plan_module()
        self.root = Path(__file__).resolve().parents[2]

    def test_checked_in_queue_is_exact_regeneration(self) -> None:
        items = self.plan.build(self.root)
        self.assertEqual(95, len(items))
        expected = self.plan.render(items)
        actual = (self.root / "m3/backports/BACKPORT_WORK_QUEUE.tsv").read_text(
            encoding="utf-8"
        )
        self.assertEqual(expected, actual)

    def test_language_and_compatibility_items_are_excluded_not_materialized(self) -> None:
        by_id = {item.identity: item for item in self.plan.build(self.root)}
        self.assertEqual("EXCLUDE_LANGUAGE", by_id["JEP-511"].action)
        self.assertEqual("LIBRARY_API", by_id["JEP-511"].required_scope)
        self.assertEqual("INCOMPATIBLE", by_id["JEP-511"].risk)

        self.assertEqual("EXCLUDE_COMPAT", by_id["JEP-486"].action)
        self.assertEqual("INCOMPATIBLE", by_id["JEP-486"].risk)

    def test_final_or_runtime_candidates_enter_dependency_closure(self) -> None:
        by_id = {item.identity: item for item in self.plan.build(self.root)}
        self.assertEqual(2, by_id["JEP-485"].current_pass)
        self.assertEqual("INVESTIGATE_DEPENDENCY_CLOSURE", by_id["JEP-485"].action)
        self.assertEqual("NORMAL", by_id["JEP-485"].risk)

        self.assertEqual(2, by_id["JEP-534"].current_pass)
        self.assertEqual("HIGH", by_id["JEP-534"].risk)
        self.assertEqual("MULTI_MODULE", by_id["JEP-534"].required_scope)

    def test_superseded_and_preview_items_do_not_silently_freeze(self) -> None:
        by_id = {item.identity: item for item in self.plan.build(self.root)}
        self.assertEqual("REDIRECT_SUPERSEDED", by_id["JEP-457"].action)
        self.assertEqual("JEP-484".replace("JEP-", ""), by_id["JEP-457"].dependency_or_commit)

        self.assertEqual("HOLD_DEPENDENCY_OR_POLICY", by_id["JEP-533"].action)
        self.assertEqual("HOLD", by_id["JEP-533"].risk)

    def test_materialized_jcmd_change_is_verify_not_promote(self) -> None:
        by_id = {item.identity: item for item in self.plan.build(self.root)}
        item = by_id["JDK-8357439"]
        self.assertEqual(5, item.current_pass)
        self.assertEqual("VERIFY_MATERIALIZED", item.action)
        self.assertEqual("ADMITTED_UNVERIFIED", item.risk)
        self.assertEqual(
            "8549d1896054dd230ba3038c83bce23b10dcda22",
            item.dependency_or_commit,
        )

    def test_scope_mapping_is_conservative(self) -> None:
        self.assertEqual("LIBRARY_API", self.plan.required_scope("language"))
        self.assertEqual("MULTI_MODULE", self.plan.required_scope("vm-gc-runtime"))
        self.assertEqual("MULTI_MODULE", self.plan.required_scope("monitoring-jfr-native"))
        self.assertEqual("MODULE", self.plan.required_scope("tool-jlink-runtime-image"))
        self.assertEqual("MODULE", self.plan.required_scope("security-library"))


if __name__ == "__main__":
    unittest.main()
