#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Classify every differing line of the FilteredViewsOwnerDifferential outputs of #2641's head (base) and this leaf
(head) (DIFFERENTIAL.tsv). The base already carries B1-B4 of filtered-views-owner-20261106, so the only expected
differences are the new guard's:
  ABA          head ConcurrentModificationException in a scenario over an M3 owner, raised by the operation (or
               cursor reopen) during which the armed comparator fired (its call number cmAt falls in the line's
               comparator-call range), where base did not throw there, or threw later in the same operation (head
               predicate calls a prefix of base's and no more comparator calls)
  ABA_START    cursor (re)creation during which the armed comparator fired: same result (creation never fails, the
               first call throws), same predicate calls, fewer comparator calls on head (cursorStart stops the walk
               instead of probing released slots); the line is the open/reopen, or the line after a successful
               reopen (which carries the reopen's comparator calls)
  FOLLOW(ABA)  any difference after the scenario's ABA or ABA_START line (the state differs by construction)
  UNCLASSIFIED anything else (gate: must be 0; JDK-owner scenarios must not differ at all)
Comparator-call ranges are cumulative per scenario: every op line carries cmp = calls since the previous counted line
(the first includes the setup's calls); a successful reopen is not counted (its calls go to the next line), a failed
reopen is a counted line.
usage: python -I classify.py <base.out> <head.out> <out.tsv>
"""
from __future__ import annotations

import re
import sys
from collections import Counter

SETUP = re.compile(r"^s(\d+) setup src=(\w+) .* cmAt=(-?\d+) ")
COUNTED = re.compile(r"^s(\d+) t(\d+) (\S+) -> (.*?) \| pred=\[(.*?)\] cmp=(\d+) \| (.*)$")
REOPEN = re.compile(r"^s(\d+) t(\d+) reopen$")
END = re.compile(r"^s(\d+) end ")
CME = "!ConcurrentModificationException"


def parse(path):
    records, setups, totals = {}, {}, Counter()
    with open(path, encoding="utf-8") as handle:
        for raw in handle:
            raw = raw.rstrip("\n")
            if match := SETUP.match(raw):
                scenario = int(match.group(1))
                setups[scenario] = (match.group(2), int(match.group(3)))
                records[(scenario, -1, "setup")] = {"raw": raw}
            elif match := COUNTED.match(raw):
                scenario, step, op = int(match.group(1)), int(match.group(2)), match.group(3)
                before = totals[scenario]
                totals[scenario] += int(match.group(6))
                kind = "reopen" if op == "reopen" else "op"
                records[(scenario, step, kind)] = {"raw": raw, "op": op, "result": match.group(4),
                                                   "pred": [p for p in match.group(5).split(", ") if p],
                                                   "cmp": int(match.group(6)), "range": (before, totals[scenario]),
                                                   "tags": match.group(7)}
            elif match := REOPEN.match(raw):
                records[(int(match.group(1)), int(match.group(2)), "reopen")] = {"raw": raw, "result": "reopened"}
            elif match := END.match(raw):
                records[(int(match.group(1)), 99, "end")] = {"raw": raw}
            else:
                raise SystemExit("unparsed line: " + raw)
    return records, setups


def aba(base, head, armed):
    if head is None or head.get("result") != CME or "range" not in head or "src=M3" not in head["tags"]:
        return False
    low, high = head["range"]
    if not (armed > 0 and low < armed <= high):
        return False
    if base is None or base.get("result") != CME:
        return True
    return base["pred"][:len(head["pred"])] == head["pred"] and head["cmp"] <= base["cmp"]


def aba_start(base, head, armed):
    """Cursor creation during which the armed comparator fired: same result (creation never fails), same predicate
    calls, fewer comparator calls on head (cursorStart's walk stops instead of probing released slots). The line is
    the open/reopen itself, or the line after a successful reopen, which carries the reopen's comparator calls."""
    if base is None or head is None or "range" not in head or "range" not in base:
        return False
    low, high = head["range"]
    return (armed > 0 and low < armed <= high and "src=M3" in head["tags"] and head["result"] == base["result"]
            and head["pred"] == base["pred"] and head["cmp"] < base["cmp"])


def main(argv):
    base, base_setups = parse(argv[1])
    head, head_setups = parse(argv[2])
    order = {"setup": 0, "op": 1, "reopen": 2, "end": 3}
    keys = sorted(set(base) | set(head), key=lambda k: (k[0], k[1], order[k[2]]))
    rows, counts, flagged, differing_jdk = [], Counter(), set(), 0
    for key in keys:
        b, h = base.get(key), head.get(key)
        if b is not None and h is not None and b["raw"] == h["raw"]:
            continue
        scenario = key[0]
        source, armed = head_setups.get(scenario, base_setups.get(scenario))
        if source.startswith("JDK"):
            differing_jdk += 1
            label = "UNCLASSIFIED"
        elif scenario not in flagged and aba(b, h, armed):
            label = "ABA"
            flagged.add(scenario)
        elif scenario not in flagged and aba_start(b, h, armed):
            label = "ABA_START"
            flagged.add(scenario)
        elif scenario in flagged:
            label = "FOLLOW(ABA)"
        else:
            label = "UNCLASSIFIED"
        counts[label] += 1
        rows.append((str(scenario), str(key[1]), key[2], label, b["raw"] if b else "-", h["raw"] if h else "-"))
    with open(argv[3], "w", encoding="utf-8", newline="\n") as out:
        out.write("scenario\tstep\trecord\tclass\tbase\thead\n")
        for row in rows:
            out.write("\t".join(row) + "\n")
    print("DIFFERENTIAL scenarios=%d differing=%d scenarios_differing=%d jdk_differing=%d %s" % (
        len(head_setups), len(rows), len({row[0] for row in rows}), differing_jdk,
        " ".join("%s=%d" % item for item in sorted(counts.items()))))
    return 1 if counts["UNCLASSIFIED"] else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
