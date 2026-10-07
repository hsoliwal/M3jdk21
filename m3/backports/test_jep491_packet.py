# SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
from __future__ import annotations

import csv
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PACKET = ROOT / "m3/backports/recipes/jep-491-vthread-unpinning"


def fields(path: Path) -> dict[str, str]:
    with path.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.reader(handle, delimiter="\t"))
    if not rows or rows[0] != ["field", "value"]:
        raise AssertionError(f"bad field/value header: {path}")
    return dict(rows[1:])


class Jep491PacketTest(unittest.TestCase):
    def test_exact_reviewed_denominator_and_plane_counts(self) -> None:
        paths = [
            line.strip()
            for line in (PACKET / "PATHS.txt").read_text(encoding="utf-8").splitlines()
            if line.strip()
        ]
        self.assertEqual(246, len(paths))
        self.assertEqual(246, len(set(paths)))
        self.assertEqual(sorted(paths), paths)

        with (PACKET / "PLANE_INVENTORY.tsv").open(
            encoding="utf-8", newline=""
        ) as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))
        counts = {row["plane"]: int(row["paths"]) for row in rows}
        self.assertEqual(
            {
                "HOTSPOT_CPU_PRODUCT": 91,
                "HOTSPOT_SHARED_PRODUCT": 82,
                "JAVA_BASE_LIBRARY_PRODUCT": 20,
                "JAVA_BASE_NATIVE_PRODUCT": 2,
                "SERVICEABILITY_AGENT_PRODUCT": 3,
                "JFR_LIBRARY_PRODUCT": 2,
                "TEST_NATIVE_EVIDENCE": 9,
                "TEST_JAVA_EVIDENCE": 37,
            },
            counts,
        )
        self.assertEqual(246, sum(counts.values()))
        self.assertTrue(all(row["mutation_authority"] == "false" for row in rows))

    def test_upstream_authority_is_pinned(self) -> None:
        upstream = fields(PACKET / "UPSTREAM_PR.tsv")
        self.assertEqual("491", upstream["jep"])
        self.assertEqual("JDK-8338383", upstream["implementation_issue"])
        self.assertEqual("21565", upstream["upstream_pr"])
        self.assertRegex(upstream["reviewed_pr_head"], r"^[0-9a-f]{40}$")
        self.assertEqual("jdk-24+36", upstream["released_postimage_tag"])
        self.assertEqual("246", upstream["changed_paths"])

    def test_java21_default_and_owner_layout_are_real(self) -> None:
        globals_hpp = (ROOT / "src/hotspot/share/runtime/globals.hpp").read_text(
            encoding="utf-8"
        )
        synchronizer = (ROOT / "src/hotspot/share/runtime/synchronizer.cpp").read_text(
            encoding="utf-8"
        )
        self.assertIn("product(int, LockingMode, LM_LEGACY", globals_hpp)
        self.assertIn("LockingMode == LM_LIGHTWEIGHT", synchronizer)
        self.assertTrue((ROOT / "src/hotspot/share/runtime/lockStack.hpp").is_file())
        self.assertTrue((ROOT / "src/hotspot/share/runtime/threadIdentifier.hpp").is_file())
        self.assertFalse(
            (ROOT / "src/hotspot/share/runtime/lightweightSynchronizer.hpp").exists()
        )

    def test_receipt_keeps_mutation_and_promotion_closed(self) -> None:
        receipt = fields(PACKET / "CURRENT_TREE_RECEIPT.tsv")
        self.assertEqual("DEPENDENCY_CLOSURE_IN_PROGRESS", receipt["packet_state"])
        self.assertEqual("false", receipt["source_materialized"])
        self.assertEqual("LM_LEGACY_PRESERVED", receipt["java21_locking_default"])
        self.assertEqual("PRESERVE", receipt["java21_tracePinnedThreads"])
        self.assertEqual("NOT_AUTHORIZED", receipt["promotion"])

    def test_compatibility_split_rejects_default_and_diagnostic_removals(self) -> None:
        with (PACKET / "COMPATIBILITY_SPLIT.tsv").open(
            encoding="utf-8", newline=""
        ) as handle:
            rows = {row["capability"]: row for row in csv.DictReader(handle, delimiter="\t")}
        self.assertEqual(
            "KEEP_JAVA21_LM_LEGACY_DEFAULT", rows["LOCKING_DEFAULT"]["java21_decision"]
        )
        self.assertEqual(
            "REJECT_REMOVAL", rows["PINNED_TRACE_PROPERTY"]["java21_decision"]
        )
        self.assertEqual(
            "ADMIT_OPT_IN", rows["CORE_MONITOR_UNPINNING"]["java21_decision"]
        )


if __name__ == "__main__":
    unittest.main()
