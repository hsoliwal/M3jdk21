#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""REUSE step of inline -> atomize -> patternize -> reuse. Reads the patternize output (PATTERNS_EXACT.tsv: h1 exact
L-Hash classes; PATTERNS_STRUCTURAL.tsv: structural classes) and writes, CPU only and proposal-only (GOD law: no
source is rewritten; vendored sources stay byte-for-byte upstream so the equivalence gate holds):
  REUSE_CATALOG.tsv      one row per pattern shared by >= 2 donors with >= --min-tokens tokens:
                         scope CROSS_ARTIFACT (different artifacts) | CROSS_VERSION (one artifact, several versions)
  REUSE_PAIRS.tsv        artifact pairs ranked by shared exact atoms and tokens (relocated or embedded copies show here)
  REUSE_SUMMARY.json     totals per scope and per kind; input SHA-256s
"""
from __future__ import annotations
import argparse, csv, hashlib, io, json, re, sys
from collections import Counter, defaultdict
from itertools import combinations
from pathlib import Path

csv.field_size_limit(1 << 30)


def artifact_of(donor: str) -> str:
    m = re.match(r"(.+?)-(\d[\w.\-]*)$", donor)
    return m.group(1) if m else donor


def rows(path: Path):
    with io.open(path, encoding="utf-8", newline="") as f:
        yield from csv.DictReader(f, delimiter="\t")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--atoms", type=Path, required=True)
    ap.add_argument("--out", type=Path, required=True)
    ap.add_argument("--min-tokens", type=int, default=30)
    args = ap.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    catalog, pairs, totals = [], defaultdict(lambda: [0, 0]), Counter()
    inputs = {}
    for layer, name in (("exact", "PATTERNS_EXACT.tsv"), ("structural", "PATTERNS_STRUCTURAL.tsv")):
        src = args.atoms / name
        if not src.is_file():
            continue
        inputs[name] = hashlib.sha256(src.read_bytes()).hexdigest()
        for r in rows(src):
            donors = sorted(set(d for d in (r.get("donors") or "").split("|") if d))
            ntok = int(r.get("n_tokens") or 0)
            if len(donors) < 2 or ntok < args.min_tokens:
                continue
            arts = sorted(set(artifact_of(d) for d in donors))
            scope = "CROSS_ARTIFACT" if len(arts) > 1 else "CROSS_VERSION"
            key = r.get("h1") or r.get("key") or r.get(list(r.keys())[0])
            catalog.append((layer, key, scope, r.get("kinds", ""), str(ntok), r.get("members", ""), str(len(donors)), ",".join(arts), "|".join(donors), (r.get("representative") or "")[:160]))
            totals[(layer, scope)] += 1
            if layer == "exact" and scope == "CROSS_ARTIFACT":
                for a, b in combinations(arts, 2):
                    pairs[(a, b)][0] += 1
                    pairs[(a, b)][1] += ntok
    catalog.sort(key=lambda x: (x[0], x[2], -int(x[4]) * int(x[6])))
    with io.open(args.out / "REUSE_CATALOG.tsv", "w", encoding="utf-8", newline="\n") as f:
        f.write("layer\tpattern\tscope\tkinds\tn_tokens\tmembers\tdonors\tartifacts\tdonor_ids\trepresentative\n")
        f.writelines("\t".join(c) + "\n" for c in catalog)
    with io.open(args.out / "REUSE_PAIRS.tsv", "w", encoding="utf-8", newline="\n") as f:
        f.write("artifact_a\tartifact_b\tshared_exact_patterns\tshared_tokens\n")
        for (a, b), (n, t) in sorted(pairs.items(), key=lambda kv: -kv[1][1]):
            f.write("%s\t%s\t%d\t%d\n" % (a, b, n, t))
    summary = {"min_tokens": args.min_tokens, "inputs_sha256": inputs,
               "patterns": {"%s/%s" % k: v for k, v in sorted(totals.items())},
               "artifact_pairs": len(pairs),
               "top_pairs": [{"a": a, "b": b, "patterns": n, "tokens": t} for (a, b), (n, t) in sorted(pairs.items(), key=lambda kv: -kv[1][1])[:15]],
               "policy": "proposal-only: vendored sources stay byte-for-byte upstream; reuse is realized by version convergence and by consumers choosing one artifact, never by editing donor code"}
    (args.out / "REUSE_SUMMARY.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(json.dumps({k: summary[k] for k in ("patterns", "artifact_pairs")}))
    for p in summary["top_pairs"][:8]:
        print("  %-40s %-40s %6d patterns %9d tokens" % (p["a"], p["b"], p["patterns"], p["tokens"]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
