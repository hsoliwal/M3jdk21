#!/usr/bin/env python3
from __future__ import annotations

import csv
import pathlib

HERE = pathlib.Path(__file__).resolve().parent

MILESTONES = {
    "JEP-457": "2b00ac0d02a110326846c75ea7ea535dccbb1924",
    "JEP-466": "19a99d023e32fa9f4d26b76bd36993719e1dfe21",
    "JEP-484": "84ffb64cd73f8af11cf3670c6f19d282c2ac6961",
}


def rows(name: str):
    with (HERE / name).open(encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream, delimiter="\t"))


def verify() -> None:
    dependency = rows("DEPENDENCY_GRAPH.tsv")
    if [r["identity"] for r in dependency] != [
        "JDK21-INTERNAL-CLASSFILE", "JEP-457", "JEP-466", "JEP-484"
    ]:
        raise ValueError("dependency order")
    if dependency[-1]["default_java21"] != "NO":
        raise ValueError("JEP484 default Java21 must be NO")
    if dependency[-1]["physical_scope"] != "LIBRARY_API":
        raise ValueError("JEP484 scope")

    history = rows("HISTORY.tsv")
    if len(history) != 59:
        raise ValueError(f"history denominator: {len(history)}")
    if [int(r["ordinal"]) for r in history] != list(range(59)):
        raise ValueError("history ordinals")
    shas = [r["commit"] for r in history]
    if len(set(shas)) != len(shas):
        raise ValueError("duplicate history commit")
    if shas != sorted(shas, key=lambda sha: next(
            i for i, r in enumerate(history) if r["commit"] == sha)):
        raise ValueError("history order")
    for identity, sha in MILESTONES.items():
        found = [r for r in history if r["commit"] == sha]
        if len(found) != 1 or found[0]["milestone"] != identity:
            raise ValueError(f"missing milestone {identity}")

    summary = {r["metric"]: int(r["value"]) for r in rows("TREE_SUMMARY.tsv")}
    expected = {
        "jdk21_internal_java_files": 241,
        "jdk21_public_java_files": 0,
        "jdk24_public_java_files": 161,
        "jdk24_internal_java_files": 84,
        "jdk24_combined_java_files": 245,
        "normalized_direct_descendants": 238,
        "normalized_removed": 3,
        "normalized_added": 7,
        "public_history_commits": 59,
        "jep457_transition_touched_paths": 300,
        "jep466_transition_touched_paths": 1,
        "jep484_finalization_touched_paths": 165,
    }
    if summary != expected:
        raise ValueError(f"tree summary drift: {summary}")

    delta = rows("PATH_DELTA.tsv")
    if sum(r["kind"] == "REMOVED" for r in delta) != 3:
        raise ValueError("removed denominator")
    if sum(r["kind"] == "ADDED" for r in delta) != 7:
        raise ValueError("added denominator")

    policy_rows = rows("POLICY.tsv")
    if len(policy_rows) != 1:
        raise ValueError("policy row count")
    policy = policy_rows[0]
    if policy != {
        "jep": "484",
        "default_java21": "NO",
        "opt_in_extension": "YES",
        "scope": "LIBRARY_API",
        "contract": "EXPLICIT_CONTRACT_CHANGE",
        "classfile_default": "MUST_REMAIN_JAVA21_COMPATIBLE",
        "promotion_authority": "false",
    }:
        raise ValueError("policy drift")


if __name__ == "__main__":
    verify()
    print("JEP484_CLASSFILE_INVENTORY_OK")
