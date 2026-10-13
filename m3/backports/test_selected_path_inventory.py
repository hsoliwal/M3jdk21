#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


def load_module():
    path = Path(__file__).with_name("selected_path_inventory.py")
    spec = importlib.util.spec_from_file_location("m3_selected_path_inventory", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class SelectedPathInventoryTest(unittest.TestCase):
    def setUp(self) -> None:
        self.mod = load_module()

    @staticmethod
    def delta(path: str, status: str, lane: str) -> dict[str, str]:
        return {
            "path": path,
            "status": status,
            "recipe_lane": lane,
            "baseline_ref": "jdk-21+35",
            "donor_ref": "donor",
        }

    def test_classifies_ga_materialization_equivalence_and_removal(self) -> None:
        selected = ["a.java", "b.cpp", "c.hpp", "d.java"]
        integration = {
            "a.java": self.delta("a.java", "MODIFIED", "OPENREWRITE_JAVA_PAIR"),
            "b.cpp": self.delta("b.cpp", "ADDED", "SOURCE_SEALED_ADD_NATIVE"),
            "c.hpp": self.delta("c.hpp", "MODIFIED", "SOURCE_SEALED_NATIVE_PAIR"),
            "d.java": self.delta("d.java", "MODIFIED", "OPENREWRITE_JAVA_PAIR"),
        }
        ga = {
            "a.java": self.delta("a.java", "MODIFIED", "OPENREWRITE_JAVA_PAIR"),
            "b.cpp": self.delta("b.cpp", "SAME", "NO_RECIPE"),
            "c.hpp": self.delta("c.hpp", "REMOVED", "REVIEW_REMOVAL"),
            "d.java": self.delta("d.java", "ADDED", "OPENREWRITE_ADD_JAVA"),
        }

        rows = self.mod.classify(selected, integration, ga)

        self.assertEqual(
            [
                "MATERIALIZE_GA_POSTIMAGE",
                "ALREADY_EQUIVALENT",
                "GA_REMOVAL_HOLD",
                "MATERIALIZE_GA_POSTIMAGE",
            ],
            [row.disposition for row in rows],
        )
        self.assertEqual(
            {
                "ALREADY_EQUIVALENT": 1,
                "GA_REMOVAL_HOLD": 1,
                "MATERIALIZE_GA_POSTIMAGE": 2,
            },
            self.mod.summary(rows),
        )

    def test_integration_added_path_absent_from_java21_and_ga_is_held(self) -> None:
        rows = self.mod.classify(
            ["new.cpp"],
            {"new.cpp": self.delta("new.cpp", "ADDED", "SOURCE_SEALED_ADD_NATIVE")},
            {},
        )

        self.assertEqual("GA_REMOVED_HOLD", rows[0].disposition)
        self.assertEqual("ABSENT", rows[0].ga_status)

    def test_missing_integration_path_fails_closed(self) -> None:
        with self.assertRaises(ValueError):
            self.mod.classify(["missing.java"], {}, {})

    def test_ga_absence_of_non_added_integration_path_fails_closed(self) -> None:
        with self.assertRaises(ValueError):
            self.mod.classify(
                ["existing.java"],
                {
                    "existing.java": self.delta(
                        "existing.java", "MODIFIED", "OPENREWRITE_JAVA_PAIR"
                    )
                },
                {},
            )

    def test_path_file_requires_sorted_unique_canonical_paths(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "paths.txt"
            path.write_text("b.java\na.java\n", encoding="utf-8")
            with self.assertRaises(ValueError):
                self.mod.read_paths(path)

            path.write_text("a.java\na.java\n", encoding="utf-8")
            with self.assertRaises(ValueError):
                self.mod.read_paths(path)

            path.write_text("../a.java\n", encoding="utf-8")
            with self.assertRaises(ValueError):
                self.mod.read_paths(path)

            path.write_text("a.java\nb.cpp\n", encoding="utf-8")
            self.assertEqual(["a.java", "b.cpp"], self.mod.read_paths(path))


if __name__ == "__main__":
    unittest.main()
