#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Classify every differing line of the FilteredViewsOwnerDifferential base/head outputs (DIFFERENTIAL.tsv).

Lines are aligned by (scenario, step, record). A differing step line gets the first rule that matches:
  B4      head ConcurrentModificationException on a direct owner write, base none, after the armed comparator fired
          (M3TreeMap.eraseAt refuses the slot the comparator released; base erased it and damaged the tree)
  B1     head ConcurrentModificationException, base none; a structural owner change since the cursor opened (ext>0)
  B2      head ConcurrentModificationException, base none; a predicate or comparator mutation since it opened
  B3      same result, mutable view, head predicate calls a strict subsequence of base's (values() once per entry, or
          an own remove that keeps the prepared successor)
  SLOT    same result and predicate calls, mutable view over an M3 owner, only the comparator count differs (the
          slot walk replaces the navigation re-search per step)
  SLOT_CM mutable view over an M3 owner with an armed comparator whose firing moved (cm tag or result differs before
          any other difference): the one-shot action is bound to a comparator call number the slot walk shifts
  FOLLOW  a difference no rule above matches, after the scenario's first classified difference (the cursor was
          reopened, or an operation was refused, so the remaining state differs by construction); named after that
          first class (every line is first tried against the rules above)
  UNCLASSIFIED anything else (gate: must be 0; a JDK-owner scenario must have no difference at all)
usage: python -I classify.py <base.out> <head.out> <out.tsv>
"""
from __future__ import annotations

import re
import sys
from collections import Counter

LINE = re.compile(r"^s(\d+) (t(\d+) (\S+)(?: -> (.*?) \| pred=\[(.*?)\] cmp=(\d+) \| (.*))?|setup .*|end .*)$")


def parse(path):
    records = {}
    with open(path, encoding="utf-8") as handle:
        for raw in handle:
            raw = raw.rstrip("\n")
            match = LINE.match(raw)
            if not match:
                raise SystemExit("unparsed line: " + raw)
            scenario = int(match.group(1))
            if match.group(3) is None:
                key = (scenario, -1 if raw.split(" ")[1] == "setup" else 99, raw.split(" ")[1])
                records[key] = {"raw": raw}
                continue
            step, op = int(match.group(3)), match.group(4)
            if op == "reopen":
                records[(scenario, step, "reopen")] = {"raw": raw}
                continue
            tags = dict(item.split("=", 1) for item in match.group(8).split(" "))
            records[(scenario, step, "op")] = {"raw": raw, "op": op, "result": match.group(5),
                                               "pred": match.group(6), "cmp": int(match.group(7)), "tags": tags}
    return records


def subsequence(short, long):
    items = iter(long)
    return all(any(token == other for other in items) for token in short)


def classify(base, head, previous_remove):
    tags = head["tags"]
    cme = "!ConcurrentModificationException"
    mutable_m3 = tags["view"] == "mutable" and tags["src"] == "M3"
    if tags["src"] == "JDK" or base["op"] != head["op"]:
        # a different operation at the same step means the scenario's random stream already diverged
        # (callbacks that draw from it ran a different number of times): only FOLLOW may explain it
        return "UNCLASSIFIED"
    if head["result"] == cme and base["result"] != cme:
        if head["op"] == "owner.mutate" and tags["cm"] == "1":
            return "B4"
        if int(tags["ext"]) > 0:
            return "B1"
        if tags["pm"] == "1" or tags["cm"] == "1":
            return "B2"
        return "UNCLASSIFIED"
    if mutable_m3 and base["tags"]["cm"] != tags["cm"]:
        return "SLOT_CM"
    if base["result"] == head["result"] and tags["view"] == "mutable":
        bp = [p for p in base["pred"].split(", ") if p]
        hp = [p for p in head["pred"].split(", ") if p]
        if len(hp) < len(bp) and subsequence(hp, bp) and (tags["cursor"] == "values" or previous_remove):
            return "B3"
        if bp == hp and base["cmp"] != head["cmp"] and mutable_m3:
            return "SLOT"
    return "UNCLASSIFIED"


def main(argv):
    base, head = parse(argv[1]), parse(argv[2])
    rows, counts, first_class = [], Counter(), {}
    scenarios = sorted({key[0] for key in base} | {key[0] for key in head})
    for scenario in scenarios:
        keys = sorted({k for k in base if k[0] == scenario} | {k for k in head if k[0] == scenario},
                      key=lambda k: (k[1], k[2] != "op"))
        previous_remove = False
        for key in keys:
            b, h = base.get(key), head.get(key)
            if b is not None and h is not None and b["raw"] == h["raw"]:
                previous_remove = key[2] == "op" and h["op"] == "cursor.remove"
                continue
            label = "UNCLASSIFIED"
            if key[2] == "op" and b is not None and h is not None:
                label = classify(b, h, previous_remove)
            if label == "UNCLASSIFIED" and scenario in first_class:
                label = "FOLLOW(" + first_class[scenario] + ")"
            elif label != "UNCLASSIFIED":
                first_class.setdefault(scenario, label)
            counts[label] += 1
            rows.append((str(scenario), str(key[1]), key[2], label,
                         b["raw"] if b else "-", h["raw"] if h else "-"))
            if key[2] == "op" and h is not None:
                previous_remove = h["op"] == "cursor.remove"
    with open(argv[3], "w", encoding="utf-8", newline="\n") as out:
        out.write("scenario\tstep\trecord\tclass\tbase\thead\n")
        for row in rows:
            out.write("\t".join(row) + "\n")
    differing = len(rows)
    print("DIFFERENTIAL scenarios=%d base_lines=%d head_lines=%d differing=%d %s" % (
        len(scenarios), len(base), len(head), differing,
        " ".join("%s=%d" % item for item in sorted(counts.items()))))
    return 1 if counts["UNCLASSIFIED"] else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
