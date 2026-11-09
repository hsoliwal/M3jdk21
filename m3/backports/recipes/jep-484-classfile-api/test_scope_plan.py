#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import csv
import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path

MODULE = Path(__file__).with_name("scope_plan.py")
SPEC = importlib.util.spec_from_file_location("m3_jep484_scope", MODULE)
assert SPEC is not None and SPEC.loader is not None
mod = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = mod
SPEC.loader.exec_module(mod)


def write_tsv(path: Path, fields, rows) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


class Jep484ScopePlanTest(unittest.TestCase):
    FIELDS = [
        "source_path",
        "source_sha256",
        "donor_path",
        "donor_sha256",
        "donor_plane",
        "normalized_key",
        "state",
        "source_state",
    ]

    def test_scope_split_preserves_api_and_version_holds(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            maps = root / "map"
            custody = root / "custody"
            out = root / "out"

            rows = [
                self.row("Same.java", "JDK24_INTERNAL", "SAME_PATH_REPLACE"),
                self.row("AddedInternal.java", "JDK24_INTERNAL", "ADD_DONOR_ONLY", source=False),
                self.row("Versioned.java", "JDK24_INTERNAL", "SAME_PATH_REPLACE"),
                self.row("Public.java", "JDK24_PUBLIC", "MOVED_PUBLIC_ADD"),
                self.row("Present.java", "JDK24_INTERNAL", "ALREADY_PRESENT"),
                self.row("Drift.java", "JDK24_INTERNAL", "SOURCE_DRIFT"),
                self.row("Old.java", "", "RETAIN_UNMAPPED_SOURCE", donor=False),
            ]
            write_tsv(custody / "CUSTODY_PLAN.tsv", self.FIELDS, rows)
            write_tsv(
                maps / "VERSION_SIGNALS.tsv",
                ["donor_path", "donor_plane", "signal_kind", "signal", "line", "line_sha256"],
                [{
                    "donor_path": rows[2]["donor_path"],
                    "donor_plane": "JDK24_INTERNAL",
                    "signal_kind": "CLASSFILE_MAJOR_LITERAL",
                    "signal": "68",
                    "line": "1",
                    "line_sha256": "a" * 64,
                }],
            )

            planned = mod.plan(maps, custody, out)
            by_key = {row["normalized_key"]: row for row in planned}
            self.assertEqual(mod.FILE_CANDIDATE, by_key["Same.java"]["disposition"])
            self.assertEqual("FILE", by_key["Same.java"]["scope"])
            self.assertEqual(mod.MODULE_CANDIDATE, by_key["AddedInternal.java"]["disposition"])
            self.assertEqual("MODULE", by_key["AddedInternal.java"]["scope"])
            self.assertEqual(mod.VERSION_ADAPTATION_HOLD, by_key["Versioned.java"]["disposition"])
            self.assertEqual(mod.LIBRARY_API_HOLD, by_key["Public.java"]["disposition"])
            self.assertEqual("LIBRARY_API", by_key["Public.java"]["scope"])
            self.assertEqual(mod.ALREADY_PRESENT_PROOF, by_key["Present.java"]["disposition"])
            self.assertEqual(mod.CURRENT_TREE_HOLD, by_key["Drift.java"]["disposition"])
            self.assertEqual(mod.RETAIN_JDK21_SOURCE, by_key["Old.java"]["disposition"])
            self.assertTrue(all(row["mutation_authority"] == "false" for row in planned))
            self.assertTrue(all(row["promotion_authority"] == "false" for row in planned))

            self.assertEqual(
                rows[0]["donor_path"] + "\n",
                (out / "FILE_CANDIDATES.txt").read_text(encoding="utf-8"),
            )
            self.assertEqual(
                rows[1]["donor_path"] + "\n",
                (out / "MODULE_CANDIDATES.txt").read_text(encoding="utf-8"),
            )

    def test_public_plane_wins_over_version_signal(self):
        row = self.row("PublicVersioned.java", "JDK24_PUBLIC", "ADD_DONOR_ONLY", source=False)
        disposition, scope, _ = mod.classify(row, {row["donor_path"]})
        self.assertEqual(mod.LIBRARY_API_HOLD, disposition)
        self.assertEqual("LIBRARY_API", scope)

    def test_unknown_plane_and_state_fail_to_hold_not_mutation(self):
        unknown_plane = self.row("Unknown.java", "OTHER", "ADD_DONOR_ONLY", source=False)
        self.assertEqual(mod.CURRENT_TREE_HOLD, mod.classify(unknown_plane, set())[0])

        unknown_state = self.row("UnknownState.java", "JDK24_INTERNAL", "WHAT", source=False)
        self.assertEqual(mod.CURRENT_TREE_HOLD, mod.classify(unknown_state, set())[0])

    @staticmethod
    def row(name: str, plane: str, state: str, source: bool = True, donor: bool = True):
        source_path = (
            "src/java.base/share/classes/jdk/internal/classfile/" + name if source else ""
        )
        donor_prefix = (
            "src/java.base/share/classes/java/lang/classfile/"
            if plane == "JDK24_PUBLIC"
            else "src/java.base/share/classes/jdk/internal/classfile/"
        )
        donor_path = donor_prefix + name if donor else ""
        return {
            "source_path": source_path,
            "source_sha256": "1" * 64 if source else "",
            "donor_path": donor_path,
            "donor_sha256": "2" * 64 if donor else "",
            "donor_plane": plane,
            "normalized_key": name,
            "state": state,
            "source_state": "SEALED" if source else "NOT_APPLICABLE",
        }


if __name__ == "__main__":
    unittest.main()
