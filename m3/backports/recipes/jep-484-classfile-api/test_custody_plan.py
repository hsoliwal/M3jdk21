#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import csv
import hashlib
import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


MODULE = Path(__file__).with_name("custody_plan.py")
SPEC = importlib.util.spec_from_file_location("m3_jep484_custody", MODULE)
assert SPEC is not None and SPEC.loader is not None
mod = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = mod
SPEC.loader.exec_module(mod)


def digest(text: str) -> str:
    return hashlib.sha256(text.encode()).hexdigest()


def write_tsv(path: Path, fields, rows) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


class CustodyPlanTest(unittest.TestCase):
    def test_current_tree_states_are_hash_bound_and_non_mutating(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            target = root / "target"
            maps = root / "maps"
            out = root / "out"

            source_same = "src/java.base/share/classes/jdk/internal/classfile/Same.java"
            source_move = "src/java.base/share/classes/jdk/internal/classfile/ClassfileFoo.java"
            donor_move = "src/java.base/share/classes/java/lang/classfile/ClassFileFoo.java"
            source_unmapped = "src/java.base/share/classes/jdk/internal/classfile/Old.java"
            donor_added = "src/java.base/share/classes/java/lang/classfile/Added.java"
            for path, text in (
                (source_same, "same21\n"),
                (source_move, "move21\n"),
                (source_unmapped, "old21\n"),
            ):
                file = target / path
                file.parent.mkdir(parents=True, exist_ok=True)
                file.write_text(text)

            mapped_fields = [
                "source_path","source_relative","source_sha256","donor_path","donor_relative",
                "donor_sha256","donor_plane","normalized_key","state"
            ]
            write_tsv(maps / "JDK21_INTERNAL_TO_JDK24.tsv", mapped_fields, [
                {
                    "source_path":source_same,"source_relative":"Same.java","source_sha256":digest("same21\n"),
                    "donor_path":source_same,"donor_relative":"Same.java","donor_sha256":digest("same24\n"),
                    "donor_plane":"JDK24_INTERNAL","normalized_key":"Same.java","state":"DIRECT_DESCENDANT"
                },
                {
                    "source_path":source_move,"source_relative":"ClassfileFoo.java","source_sha256":digest("move21\n"),
                    "donor_path":donor_move,"donor_relative":"ClassFileFoo.java","donor_sha256":digest("move24\n"),
                    "donor_plane":"JDK24_PUBLIC","normalized_key":"ClassFileFoo.java","state":"DIRECT_DESCENDANT"
                },
            ])
            write_tsv(maps / "UNMAPPED_JDK21.tsv",
                ["source_path","source_relative","source_sha256","normalized_key","state"], [{
                    "source_path":source_unmapped,"source_relative":"Old.java","source_sha256":digest("old21\n"),
                    "normalized_key":"Old.java","state":"NO_DIRECT_DESCENDANT"
                }])
            write_tsv(maps / "AMBIGUOUS.tsv",
                ["source_path","normalized_key","candidate_paths","state"], [])
            write_tsv(maps / "ADDED_JDK24.tsv",
                ["donor_path","donor_relative","donor_sha256","donor_plane","normalized_key"], [{
                    "donor_path":donor_added,"donor_relative":"Added.java","donor_sha256":digest("added24\n"),
                    "donor_plane":"JDK24_PUBLIC","normalized_key":"Added.java"
                }])
            write_tsv(maps / "VERSION_SIGNALS.tsv",
                ["donor_path","donor_plane","signal_kind","signal","line","line_sha256"], [{
                    "donor_path":donor_added,"donor_plane":"JDK24_PUBLIC","signal_kind":"CLASSFILE_MAJOR_LITERAL",
                    "signal":"68","line":"1","line_sha256":"a"*64
                }])

            before = {path: (target / path).read_bytes() for path in (source_same,source_move,source_unmapped)}
            rows = mod.materialize(target, maps, out)
            states = {(row["source_path"], row["donor_path"]): row["state"] for row in rows}
            self.assertEqual("SAME_PATH_REPLACE", states[(source_same, source_same)])
            self.assertEqual("MOVED_PUBLIC_ADD", states[(source_move, donor_move)])
            self.assertEqual("RETAIN_UNMAPPED_SOURCE", states[(source_unmapped, "")])
            self.assertEqual("ADD_DONOR_ONLY", states[("", donor_added)])
            for path,payload in before.items():
                self.assertEqual(payload, (target / path).read_bytes())
            self.assertEqual(donor_added + "\n", (out / "VERSION_SIGNAL_TARGETS.txt").read_text())

    def test_drift_occupied_already_present_and_ambiguity_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp); target=root/"target"; maps=root/"maps"; out=root/"out"
            src="src/java.base/share/classes/jdk/internal/classfile/A.java"
            dst="src/java.base/share/classes/java/lang/classfile/A.java"
            (target/src).parent.mkdir(parents=True,exist_ok=True)
            (target/src).write_text("drift\n")
            (target/dst).parent.mkdir(parents=True,exist_ok=True)
            (target/dst).write_text("occupied\n")
            fields=["source_path","source_relative","source_sha256","donor_path","donor_relative","donor_sha256","donor_plane","normalized_key","state"]
            write_tsv(maps/"JDK21_INTERNAL_TO_JDK24.tsv",fields,[{
                "source_path":src,"source_relative":"A.java","source_sha256":digest("base\n"),
                "donor_path":dst,"donor_relative":"A.java","donor_sha256":digest("donor\n"),
                "donor_plane":"JDK24_PUBLIC","normalized_key":"A.java","state":"DIRECT_DESCENDANT"}])
            write_tsv(maps/"UNMAPPED_JDK21.tsv",["source_path","source_relative","source_sha256","normalized_key","state"],[])
            write_tsv(maps/"ADDED_JDK24.tsv",["donor_path","donor_relative","donor_sha256","donor_plane","normalized_key"],[])
            write_tsv(maps/"VERSION_SIGNALS.tsv",["donor_path","donor_plane","signal_kind","signal","line","line_sha256"],[])
            write_tsv(maps/"AMBIGUOUS.tsv",["source_path","normalized_key","candidate_paths","state"],[])
            self.assertEqual("SOURCE_DRIFT",mod.materialize(target,maps,out)[0]["state"])

            write_tsv(maps/"AMBIGUOUS.tsv",["source_path","normalized_key","candidate_paths","state"],[{
                "source_path":src,"normalized_key":"A.java","candidate_paths":dst+",other","state":"AMBIGUOUS"}])
            with self.assertRaises(ValueError):
                mod.materialize(target,maps,root/"bad")


if __name__ == "__main__":
    unittest.main()
