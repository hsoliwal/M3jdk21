#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""Timing table of leaf jbcoll-mask-compactor-perf-20261106 (MASK_COMPACTOR_TIMING_TABLE v2).

Reads the timing-<set>-<run>.log files of timing.sh and writes TIMING.tsv: one row per CPU set x mask class x length
with the run count, deletion runs, removed, first deletion, the restore baseline, the path every variant takes
(derived here from length, runs and removed, the dispatch of each variant's source) and per variant the net ns/op
(median over the runs of variant - restore), plus every run's gross values.

Comparisons (RULE lines). A cell is AFFECTED by a comparison when the two variants execute different paths there;
only affected cells can carry a regression or a gain. Unaffected cells run the same code and their spread is the
noise floor (JIT profile and code-layout effects of the other cells; printed as noise_max, never a verdict):
  STAT       statonly vs master       (complete statistic alone, old compactRuns threshold length >>> 7)
  THRESHOLD  threshold vs master      (complete statistic + compactRuns threshold length >>> 4)
  IDEA       candidate vs base: prefix skip (pr1754/pr1802) and survivor-word compaction (pr1908 verbatim with its
             1024 bound; and with the bound dropped = fixed) on threshold; prefix skip again on fixed
  FIX        fixed (the recipe postimage) vs master, and fixed vs postimage on fragmented 4096 (NEAR)
Fix rule: no affected cell slower than master by more than 5% on any CPU set; fragmented 4096 within NEAR x
postimage on every CPU set. Idea rule: ADOPT only when the geometric mean of its target cells is <= 0.85 x base on
every CPU set and no affected cell is slower than base by more than 5% on any CPU set; else REJECTED.
Targets: prefix skip = affected cells whose first deletion is not index 0; survivor words = all affected cells.

usage: python -I timing_table.py <scratch-dir-with-timing-logs> [--out TIMING.tsv] [--fixed LABEL]
"""
from __future__ import annotations

import argparse
import math
import re
import statistics
import sys
from pathlib import Path

LINE = re.compile(r"^TIMING run=(\S+)-(\d+) (.*)$")
NEAR = 1.25
REGRESS = 1.05
ADOPT = 0.85
IDEAS = (("threshold_prefix", "threshold", "prefix skip pr1754/pr1802"),
         ("threshold_words1024", "threshold", "pr1908 survivor words, verbatim 1024 bound"),
         ("fixed", "threshold", "pr1908 survivor words, bound dropped: the recipe postimage"),
         ("fixed_prefix", "fixed", "prefix skip on the recipe postimage"))


def path(label: str, n: int, runs: int, removed: int) -> str:
    """The compactor method a variant runs for one mask (the dispatch of its source)."""
    prefix = "_prefix" in label
    scalar = "scalar_prefix" if prefix else "scalar"
    if n < 512:
        return scalar
    if label == "postimage":
        return scalar if runs > max(4, n >> 7) else "runs"
    if removed == 0:
        return "noop"
    if label == "master":
        return "runs"                      # early exit: runs field 0
    limit = max(4, n >> 7) if label == "statonly" else max(4, n >> 4)
    if runs <= limit:
        return "runs"
    if removed <= n // 8 or removed >= n - n // 64:
        return "words"
    if label.startswith("fixed") or ("words1024" in label and n >= 1024):
        return "survivors"
    return scalar


def parse(scratch: Path):
    cells = {}
    for log in sorted(scratch.glob("timing-*-*.log")):
        for line in log.read_text(encoding="utf-8", errors="replace").splitlines():
            m = LINE.match(line)
            if m:
                fields = dict(kv.split("=", 1) for kv in m.group(3).split())
                key = (m.group(1), fields["class"], int(fields["length"]))
                cell = cells.setdefault(key, {"runs": [], "meta": fields})
                cell["runs"].append({k: float(v) for k, v in fields.items()
                                     if k not in ("class", "length", "runs", "removed", "first", "reps")})
    return cells


def compare(cells, paths, nets, cand, base, target_fn):
    cpus = sorted({k[0] for k in cells})
    gmeans, bad, noise = [], [], 1.0
    for cpu in cpus:
        target = []
        for key in (k for k in cells if k[0] == cpu):
            ratio = nets[key][cand] / nets[key][base]
            if paths[key][cand] == paths[key][base]:
                noise = max(noise, ratio, 1 / ratio)
                continue
            if ratio > REGRESS:
                bad.append("%s/%s/%d=%.3f" % (key[0], key[1], key[2], ratio))
            if target_fn(key):
                target.append(ratio)
        gmeans.append((cpu, math.exp(sum(map(math.log, target)) / len(target)) if target else float("nan"),
                       len(target)))
    return gmeans, sorted(bad), noise


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("scratch")
    parser.add_argument("--out", default=str(Path(__file__).resolve().parent / "TIMING.tsv"))
    parser.add_argument("--fixed", default="fixed", help="label of the recipe postimage in the logs")
    args = parser.parse_args(argv)
    cells = parse(Path(args.scratch))
    if not cells:
        print("RULE NONE no timing logs", file=sys.stderr)
        return 2
    labels = [k for k in next(iter(cells.values()))["runs"][0] if k != "restore"]
    paths, nets = {}, {}
    rows = ["\t".join(["cpu_set", "class", "length", "runs", "deletion_runs", "removed", "first_deletion",
                       "restore_ns"] + ["path_" + l for l in labels] + ["net_" + l for l in labels]
                      + ["gross_per_run(restore," + ",".join(labels) + ")"])]
    for key in sorted(cells, key=lambda k: (k[0], k[1], k[2])):
        cell, meta = cells[key], cells[key]["meta"]
        n, runs, removed = key[2], int(meta["runs"]), int(meta["removed"])
        paths[key] = {l: path(l, n, runs, removed) for l in labels}
        nets[key] = {l: max(0.1, statistics.median(r[l] - r["restore"] for r in cell["runs"])) for l in labels}
        gross = ";".join(",".join("%.1f" % r[k] for k in ["restore"] + labels) for r in cell["runs"])
        rows.append("\t".join([key[0], key[1], str(n), str(len(cell["runs"])), meta["runs"], meta["removed"],
                               meta["first"], "%.1f" % statistics.median(r["restore"] for r in cell["runs"])]
                              + [paths[key][l] for l in labels] + ["%.1f" % nets[key][l] for l in labels] + [gross]))
    Path(args.out).write_text("# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0\n# generated by timing_table.py "
                              "(MASK_COMPACTOR_TIMING_TABLE v2) from timing.sh logs; net = median over runs of "
                              "(variant - restore) ns/op; path = compactor method the variant runs\n"
                              + "\n".join(rows) + "\n", encoding="utf-8", newline="\n")
    status = 0
    fixed = args.fixed if args.fixed in labels else None
    for rule, cand in (("STAT", "statonly"), ("THRESHOLD", "threshold"), ("FIX", fixed)):
        if cand not in labels:
            continue
        gmeans, bad, noise = compare(cells, paths, nets, cand, "master", lambda key: True)
        near = {cpu: nets[(cpu, "fragmented", 4096)][cand] / nets[(cpu, "fragmented", 4096)]["postimage"]
                for cpu in sorted({k[0] for k in cells}) if (cpu, "fragmented", 4096) in cells}
        frag = {cpu: nets[(cpu, "fragmented", 4096)][cand] / nets[(cpu, "fragmented", 4096)]["master"] for cpu in near}
        state = "PASS" if not bad and all(v <= NEAR for v in near.values()) else "FAIL"
        if rule == "FIX" and state != "PASS":
            status = 1
        print("RULE %s %s %s_vs_master affected_gmean %s regressions>5%%=%d %s | fragmented4096 vs_postimage %s "
              "vs_master %s | noise_max(unaffected)=%.3f" % (
                  rule, state, cand, ",".join("%s=%.3f(n=%d)" % g for g in gmeans), len(bad), ",".join(bad),
                  ",".join("%s=%.3f" % kv for kv in near.items()), ",".join("%s=%.3f" % kv for kv in frag.items()),
                  noise))
    for cand, base, name in IDEAS:
        if cand not in labels or base not in labels:
            continue
        first_late = lambda key: int(cells[key]["meta"]["first"]) > 0  # noqa: E731
        target = first_late if cand.endswith("_prefix") else (lambda key: True)
        gmeans, bad, noise = compare(cells, paths, nets, cand, base, target)
        adopt = all(g <= ADOPT for _, g, _ in gmeans) and not bad
        print("RULE IDEA %s %s vs %s (%s) target_gmean %s regressions>5%%=%d %s | noise_max(unaffected)=%.3f" % (
            "ADOPT" if adopt else "REJECTED", cand, base, name, ",".join("%s=%.3f(n=%d)" % g for g in gmeans),
            len(bad), ",".join(bad), noise))
    return status


if __name__ == "__main__":
    sys.exit(main())
