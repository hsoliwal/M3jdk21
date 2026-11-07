#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Per-module test counts and failing set from one Maven reactor log (recipe REACTOR_TEST_SUMMARY; BLLSE-LAW).

Reads the log mechanically: the module header `[INFO] ---< groupId:artifactId >---`, the surefire module summary
`Tests run: N, Failures: F, Errors: E, Skipped: S` (last one per module), every `<<< FAILURE!`/`<<< ERROR!` test id,
every `Failed to execute goal ... on project X` and the Reactor Summary status per module name.
Output: TSV module_ga, name, status, tests_run, failures, errors, skipped, failing_tests, goal_failure (one row per
module, reactor order). `--failing` prints only the failing set (module_ga<TAB>test_or_goal) sorted, for diffing.
CLI: python -I testsum.py LOG [--failing]
"""
from __future__ import annotations
import re
import sys

HEADER = re.compile(r"^\[INFO\] -+< ([^ >]+) >-+$")
BUILDING = re.compile(r"^\[INFO\] Building (.+?) +\[\d+/\d+\]$")
SUMMARY = re.compile(r"^\[(?:INFO|WARNING|ERROR)\] Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)$")
FAILED_TEST = re.compile(r"^\[ERROR\] ([A-Za-z0-9_.$]+(?:\.[A-Za-z0-9_$]+)(?:\([^)]*\))?) -- Time elapsed: .* <<< (FAILURE|ERROR)!")
GOAL = re.compile(r"^\[ERROR\] Failed to execute goal(?: .*?)? on project ([^ :]+)(?:: (.*?))?(?: -> \[Help 1\])?$")
STATUS = re.compile(r"^\[INFO\] (.+?) +(?:\.{2,} *)?(SUCCESS|FAILURE|SKIPPED)(?: +\[.*)?$")


def parse(path: str):
    mods: list[dict] = []
    cur = None
    by_name: dict[str, dict] = {}
    by_artifact: dict[str, dict] = {}
    in_summary = False
    for raw in open(path, encoding="utf-8", errors="replace"):
        line = raw.rstrip("\r\n")
        m = HEADER.match(line)
        if m:
            cur = {"ga": m.group(1), "name": "", "status": "-", "tests": None, "failing": [], "goal": "-"}
            mods.append(cur)
            by_artifact[m.group(1).split(":")[-1]] = cur
            continue
        if cur is not None and not cur["name"]:
            b = BUILDING.match(line)
            if b:
                cur["name"] = b.group(1)
                by_name[b.group(1)] = cur
                continue
        g = GOAL.match(line)
        if g:
            target = by_artifact.get(g.group(1), cur)
            if target is not None:
                target["goal"] = (g.group(2) or "goal failure").replace("\t", " ")[:200]
            continue
        if "Reactor Summary" in line:
            in_summary = True
            continue
        if in_summary:
            s = STATUS.match(line)
            if s:
                target = by_name.get(s.group(1)) or by_name.get(s.group(1).rsplit(" ", 1)[0])
                if target:
                    target["status"] = s.group(2)
            continue
        if cur is None:
            continue
        s = SUMMARY.match(line)
        if s:
            cur["tests"] = tuple(int(x) for x in s.groups())
            continue
        f = FAILED_TEST.match(line)
        if f:
            cur["failing"].append(f.group(1))
    return mods


def main(argv: list[str]) -> int:
    mods = parse(argv[0])
    if "--failing" in argv:
        rows = set()
        for m in mods:
            for t in m["failing"]:
                rows.add(f"{m['ga']}\t{t}")
            if m["goal"] != "-":
                rows.add(f"{m['ga']}\tGOAL: {m['goal']}")
        print("\n".join(sorted(rows)))
        return 0
    print("module_ga\tname\tstatus\ttests_run\tfailures\terrors\tskipped\tfailing_tests\tgoal_failure")
    for m in mods:
        t = m["tests"] or ("-", "-", "-", "-")
        print("\t".join([m["ga"], m["name"], m["status"], *map(str, t), ";".join(m["failing"]) or "-", m["goal"]]))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
