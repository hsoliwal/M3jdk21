#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""C6 DEPENDENCY-SOURCE-INLINE, reactor gate: build every generated third_party module standalone with Maven
(the sparse worktree cannot run the root reactor), in dependency order derived from each module's
dependencies column, `mvn -B -ntp install -DskipTests` per module on JDK 21. Records BUILD_GATE.tsv
(module, result, seconds, log) and prints the admitted list for insert_modules.py. Resumable: modules with a
PASS row are skipped unless --force."""
from __future__ import annotations
import argparse, csv, io, os, subprocess, sys, time
from pathlib import Path

SUFFIX = "-synexia-source-1"


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--repo", type=Path, required=True)
    ap.add_argument("--modules", type=Path, required=True, help="THIRD_PARTY_MODULES.tsv")
    ap.add_argument("--out", type=Path, required=True, help="BUILD_GATE.tsv")
    ap.add_argument("--logs", type=Path, required=True)
    ap.add_argument("--java-home", default=r"C:\Program Files\Java\jdk-21")
    ap.add_argument("--force", action="store_true")
    ap.add_argument("--only", default="")
    args = ap.parse_args()
    mods = [m for m in csv.DictReader(io.open(args.modules, encoding="utf-8", newline=""), delimiter="\t") if m["kind"] == "MODULE"]
    by_coord = {"%s:%s:%s" % (m["group"], m["artifact"], m["version"]): m for m in mods}
    # topological order on source-module dependencies
    order, seen = [], set()

    def visit(m, stack=()):
        key = "%s:%s:%s" % (m["group"], m["artifact"], m["version"])
        if key in seen:
            return
        if key in stack:
            raise SystemExit("cycle: " + " -> ".join(stack + (key,)))
        for dep in [d for d in m["dependencies"].split(";") if d]:
            g, a, v = dep.split(":")[:3]
            if v.endswith(SUFFIX) and "%s:%s:%s" % (g, a, v) in by_coord:
                visit(by_coord["%s:%s:%s" % (g, a, v)], stack + (key,))
        seen.add(key)
        order.append(m)

    for m in mods:
        visit(m)
    previous = {}
    if args.out.exists() and not args.force:
        previous = {r["module"]: r for r in csv.DictReader(io.open(args.out, encoding="utf-8", newline=""), delimiter="\t")}
    only = set(x for x in args.only.split(",") if x)
    args.logs.mkdir(parents=True, exist_ok=True)
    rows = []
    env = dict(os.environ, JAVA_HOME=args.java_home, MAVEN_OPTS="-Xmx1g")
    for m in order:
        path = m["path"]
        if only and path not in only:
            continue
        prev = previous.get(path)
        if prev and prev["result"] == "PASS" and not args.force:
            rows.append((path, "PASS", prev["seconds"], prev["log"], "(previous)")); continue
        log = args.logs / (path.replace("/", "_") + ".log")
        started = time.time()
        with open(log, "wb") as lf:
            rc = subprocess.run(["mvn", "-B", "-ntp", "-f", str(args.repo / path / "pom.xml"), "install", "-DskipTests"], stdout=lf, stderr=subprocess.STDOUT, env=env, shell=True).returncode
        seconds = "%.0f" % (time.time() - started)
        text = log.read_text(encoding="utf-8", errors="replace")
        first_error = next((l.strip()[:200] for l in text.splitlines() if "[ERROR]" in l and ("error:" in l or "ERROR] Failed" in l)), "")
        rows.append((path, "PASS" if rc == 0 else "FAIL", seconds, str(log), first_error))
        print(path, "PASS" if rc == 0 else "FAIL", seconds + "s", first_error[:120], file=sys.stderr)
    args.out.write_text("\n".join(["module\tresult\tseconds\tlog\tfirst_error"] + ["\t".join(r) for r in rows]) + "\n", encoding="utf-8", newline="\n")
    print("\n".join(r[0] for r in rows if r[1] == "PASS"))
    print("built", len(rows), "pass", sum(1 for r in rows if r[1] == "PASS"), file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
