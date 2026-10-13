#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
"""Mutation and repeatability evidence for the exact active M3JDK21 receiver."""
from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from check_m3string_concat_overflow_current import (
    DIGESTS,
    JTREG,
    POOL,
    WORKFLOW,
    SourceRefusal,
    verify,
)


class M3StringConcatOverflowCurrentTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        source = Path(__file__).resolve().parents[2]
        for name in DIGESTS:
            destination = self.root / name
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes((source / name).read_bytes())

    def change(self, relative: str, old: str, new: str) -> None:
        file = self.root / relative
        data = file.read_text(encoding="utf-8")
        self.assertIn(old, data)
        file.write_text(data.replace(old, new), encoding="utf-8")

    def test_actual_three_sources_replay_identically_twice(self) -> None:
        first = verify(self.root)
        self.assertEqual(first, verify(self.root))
        self.assertEqual(first["java_targets"], 2)
        self.assertEqual(first["workflow_selectors"], 24)

    def test_removed_size_limit_guard_refuses(self) -> None:
        self.change(POOL,
                    "if ((long) left.length() + right.length() > Integer.MAX_VALUE)",
                    "if (false)")
        with self.assertRaisesRegex(SourceRefusal, "sha256_drift"):
            verify(self.root)

    def test_modified_pool_retention_budget_refuses(self) -> None:
        self.change(POOL, "MAX_RETAINED_TUPLES", "UNREVIEWED_TUPLE_BUDGET")
        with self.assertRaisesRegex(SourceRefusal, "sha256_drift"):
            verify(self.root)

    def test_old_jtreg_to_string_false_boundary_refuses(self) -> None:
        self.change(JTREG, "expectOutOfMemory(() -> joiner.add(billion)",
                    "expectOutOfMemory(joiner::toString")
        with self.assertRaisesRegex(SourceRefusal, "sha256_drift"):
            verify(self.root)

    def test_missing_noncompact_configuration_refuses(self) -> None:
        self.change(JTREG, "-XX:-CompactStrings", "-XX:+CompactStrings")
        with self.assertRaisesRegex(SourceRefusal, "sha256_drift"):
            verify(self.root)

    def test_removed_selected_overflow_jtreg_refuses(self) -> None:
        self.change(WORKFLOW,
                    "test/jdk/java/lang/String/M3StringConcatLengthOverflowTest.java",
                    "test/jdk/java/lang/String/ForeignTest.java")
        with self.assertRaisesRegex(SourceRefusal, "sha256_drift"):
            verify(self.root)

    def test_lost_prebuild_source_guard_refuses(self) -> None:
        self.change(WORKFLOW, "check-m3string-concat-overflow-current.py",
                    "ignored-overflow-guard.py")
        with self.assertRaisesRegex(SourceRefusal, "sha256_drift"):
            verify(self.root)

    def test_missing_pool_refuses(self) -> None:
        (self.root / POOL).unlink()
        with self.assertRaisesRegex(SourceRefusal, "missing_source"):
            verify(self.root)

    def test_symlink_in_source_path_refuses(self) -> None:
        file = self.root / JTREG
        file.unlink()
        file.symlink_to(self.root / POOL)
        with self.assertRaisesRegex(SourceRefusal, "symlink"):
            verify(self.root)

    def test_altered_workflow_and_updated_other_source_still_refuse(self) -> None:
        self.change(WORKFLOW, "Run M3 String backing jtreg",
                    "Skip M3 String backing jtreg")
        self.change(POOL, "MAX_RETAINED_TUPLES", "MUTATED_TUPLE_BUDGET")
        with self.assertRaisesRegex(SourceRefusal, "sha256_drift"):
            verify(self.root)


if __name__ == "__main__":
    unittest.main()
