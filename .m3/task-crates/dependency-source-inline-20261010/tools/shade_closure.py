#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Closure check for the shaded tooling jar: run `mvn dependency:list` on the shade module and fail if any
compile/runtime artifact is not a vendored -m3-source-1 module (that artifact would be a downstream dependency
merged into the jar). Writes SHADE_CLOSURE.tsv."""
from __future__ import annotations
import argparse, os, re, subprocess, sys
from pathlib import Path


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--pom", type=Path, required=True)
    ap.add_argument("--out", type=Path, required=True)
    ap.add_argument("--suffix", default="-m3-source-1")
    ap.add_argument("--repo-local", default="")
    args = ap.parse_args()
    listing = args.out.with_suffix(".mvn.txt")
    env = dict(os.environ, MAVEN_OPTS="-Xmx768m" + (" -Dmaven.repo.local=" + args.repo_local if args.repo_local else ""))
    rc = subprocess.run(["mvn", "-B", "-ntp", "-q", "-f", str(args.pom), "dependency:list", "-DincludeScope=runtime", "-DoutputFile=" + str(listing)], env=env, shell=True).returncode
    if rc != 0 or not listing.exists():
        print("dependency:list failed", rc); return 2
    rows, bad = [], 0
    for line in listing.read_text(encoding="utf-8").splitlines():
        m = re.match(r"\s*([^:\s]+):([^:\s]+):jar:([^:\s]+):(compile|runtime)", line)
        if not m:
            continue
        g, a, v, sc = m.groups()
        ok = v.endswith(args.suffix)
        bad += 0 if ok else 1
        rows.append((g, a, v, sc, "VENDORED" if ok else "DOWNSTREAM_BINARY"))
    args.out.write_text("\n".join(["group\tartifact\tversion\tscope\tstate"] + ["\t".join(r) for r in rows]) + "\n", encoding="utf-8", newline="\n")
    print("shade closure", len(rows), "artifacts;", bad, "downstream binaries")
    for r in rows:
        if r[4] != "VENDORED":
            print("  DOWNSTREAM", ":".join(r[:3]))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
