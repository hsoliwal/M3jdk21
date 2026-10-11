#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""DEPENDENCY-SOURCE-INLINE, PROVE_FIT gate driver: compare each source-built jar with the published binary jar
(~/.m2 or the store) using ApiSurface.java (ASM) and classify the result. Inputs are either runner outputs
(--runs: <run>/donors/<id>/build/donor.jar for PASS donors) or a module ledger (--modules THIRD_PARTY_MODULES.tsv
+ --repo: the Maven-built <path>/target/<artifact>-<version>.jar, i.e. the artifact consumers actually get).
Multi-release published jars are compared in two views (base, and mr21 = what a JDK 21 runtime loads); the view
with fewer differences is reported.

Verdicts:
  IDENTICAL                    all classes/members/signatures/constants/annotations equal
  IDENTICAL_EXCEPT_SHADED      only classes the published jar relocates from another artifact are absent
  EQUIVALENT_MODULO_COMPILER   remaining differences are javac-version artifacts only: anonymous/local classes
                               (Outer$1, Outer$1Local) losing ACC_FINAL (javac 9+) and the Signature attribute on
                               their constructors
  DIFFERS_MISSING_CLASSES      published classes absent from the source build -> never admitted for consumers
  DIFFERS_MEMBERS              any other member/annotation/signature/class difference -> never admitted
  DIFFERS_RESOURCES            classes equivalent but published resources missing or different (natives,
                               generated properties, service files) -> never admitted
Writes API_SURFACE.tsv and per-donor diff files. CPU only."""
from __future__ import annotations
import argparse, csv, io, os, subprocess, sys, zipfile
from pathlib import Path

SHADED_PREFIXES = {  # published jars that relocate another artifact's classes into their own namespace
    "jackson-core": ("com/fasterxml/jackson/core/io/doubleparser/", "com/fasterxml/jackson/core/internal/shaded/"),
}
ADMISSIBLE = ("IDENTICAL", "IDENTICAL_EXCEPT_SHADED", "EQUIVALENT_MODULO_COMPILER")


def anon(c: str) -> bool:
    last = c.rsplit("/", 1)[-1]
    return "$" in last and last.rsplit("$", 1)[-1][:1].isdigit()


def norm(c: str, line: str) -> str:
    parts = line.split(" ")
    if parts[0] == "CLASS" and anon(c):
        parts[1] = format(int(parts[1], 16) & ~0x10, "x")  # javac 9+ no longer marks anonymous classes final
    if parts[0] == "METHOD" and parts[1].startswith("<init>") and not anon(c):
        parts = [p for p in parts if not p.startswith("sig=")]  # constructor Signature attribute: emitted by newer javac only
    if parts[0] == "METHOD" and anon(c) and parts[1].startswith("<init>"):
        return "METHOD <init>(anonymous-class constructor: compiler-generated captures)"
        parts[2] = "-"  # constructor access of anonymous classes / enum-constant bodies (javac-version dependent)
    return " ".join(parts)


def sig_free(line: str) -> str:  # SIGNATURE_ADDED rule: compare a CLASS header without its sig= token
    return " ".join(p for p in line.split(" ") if not p.startswith("sig="))


def classify(diff: Path, artifact: str):
    lines = io.open(diff, encoding="utf-8").read().splitlines()[1:]
    def empty_pkg(l):
        f = l.split("\t")
        return f[1].endswith("/package-info") and len(f) > 2 and f[2].strip() == "PACKAGE-INFO ann=[]"
    pub_cls = [l.split("\t")[1] for l in lines if l.startswith("PUBLISHED_ONLY_CLASS") and not empty_pkg(l)]
    built_cls = [l.split("\t")[1] for l in lines if l.startswith("BUILT_ONLY_CLASS") and not empty_pkg(l)]
    empty_pkg_count = sum(1 for l in lines if l.startswith(("PUBLISHED_ONLY_CLASS", "BUILT_ONLY_CLASS")) and empty_pkg(l))
    shaded = SHADED_PREFIXES.get(artifact, ())
    missing = [c for c in pub_cls if not (shaded and c.startswith(shaded))]
    pub_m = [(l.split("\t")[1], l.split("\t")[2]) for l in lines if l.startswith("PUBLISHED_ONLY\t")]
    built_m = [(l.split("\t")[1], l.split("\t")[2]) for l in lines if l.startswith("BUILT_ONLY\t")]
    pub_n = set((c, norm(c, x)) for c, x in pub_m)
    built_n = set((c, norm(c, x)) for c, x in built_m)
    real_pub = pub_n - built_n
    real_built = built_n - pub_n
    added = set()
    for c, x in list(real_pub):
        if x.startswith("CLASS ") and " sig=null " in x:
            for c2, y in real_built:
                if c2 == c and y.startswith("CLASS ") and sig_free(y) == sig_free(x):
                    added.add((c, x)); added.add((c2, y))
    real_pub -= added
    real_built -= added
    benign = len(pub_m) - len(real_pub) + empty_pkg_count  # empty package-info: emitted only under javac -Xpkginfo:always
    if missing:
        verdict = "DIFFERS_MISSING_CLASSES"
    elif real_pub or real_built or built_cls:
        verdict = "DIFFERS_MEMBERS"
    elif pub_cls:
        verdict = "IDENTICAL_EXCEPT_SHADED"
    elif benign:
        verdict = "EQUIVALENT_MODULO_COMPILER"
    else:
        verdict = "IDENTICAL"
    return verdict, len(missing), len(built_cls), len(real_pub), len(real_built), benign


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--inventory", type=Path, nargs="+", required=True)
    ap.add_argument("--runs", type=Path, nargs="*", default=[])
    ap.add_argument("--modules", type=Path, help="THIRD_PARTY_MODULES.tsv (gate the Maven-built module jars)")
    ap.add_argument("--repo", type=Path, help="worktree root for --modules")
    ap.add_argument("--store", type=Path, required=True)
    ap.add_argument("--m2", type=Path, default=Path(os.path.expanduser("~")) / ".m2" / "repository")
    ap.add_argument("--asm", type=Path, required=True)
    ap.add_argument("--built-m2", type=Path, help="local repository holding the installed source-built jars (default --m2)")
    ap.add_argument("--tool", type=Path, required=True)
    ap.add_argument("--diffs", type=Path, required=True)
    ap.add_argument("--out", type=Path, required=True)
    ap.add_argument("--java", default=r"C:\Program Files\Java\jdk-21\bin\java.exe")
    args = ap.parse_args()
    rows = {}
    for inv in args.inventory:
        for r in csv.DictReader(io.open(inv, encoding="utf-8", newline=""), delimiter="\t"):
            rows[(r["artifact"] + "-" + r["version"]).replace("_", "-").lower()] = r
    args.diffs.mkdir(parents=True, exist_ok=True)
    built = {}
    for run in args.runs:
        for st in [l.split("\t") for l in io.open(run / "status.tsv", encoding="utf-8").read().splitlines()[1:]]:
            if len(st) >= 2 and st[1] == "PASS" and (run / "donors" / st[0] / "build" / "donor.jar").is_file():
                built[st[0]] = run / "donors" / st[0] / "build" / "donor.jar"
    if args.modules:
        for m in csv.DictReader(io.open(args.modules, encoding="utf-8", newline=""), delimiter="\t"):
            if m["kind"] != "MODULE":
                continue
            tgt = args.repo / m["path"] / "target" / (m["artifact"] + "-" + m["version"] + ".jar")
            if not tgt.is_file():
                tgt = (args.built_m2 or args.m2) / m["group"].replace(".", "/") / m["artifact"] / m["version"] / (m["artifact"] + "-" + m["version"] + ".jar")
            built[m["donor"]] = tgt
    out = []
    for did in sorted(built):
        r = rows.get(did)
        if r is None:
            continue
        rel = Path(r["group"].replace(".", "/")) / r["artifact"] / r["version"] / (r["artifact"] + "-" + r["version"] + ".jar")
        pub = args.m2 / rel if (args.m2 / rel).is_file() else args.store / rel
        if not pub.is_file() or not built[did].is_file():
            out.append((did, "-", "-", "-", "-", "-", "-", "-", "NO_PUBLISHED_JAR" if not pub.is_file() else "NO_BUILT_JAR", "", "", "", "", "")); continue
        with zipfile.ZipFile(pub) as z:
            multi = any(n.startswith("META-INF/versions/") and n.endswith(".class") for n in z.namelist())
        best = None
        err = ""
        for view in (("base", "mr21") if multi else ("base",)):
            diff = args.diffs / ("%s.%s.tsv" % (did, view))
            proc = subprocess.run([args.java, "-cp", str(args.asm), str(args.tool), str(pub), str(built[did]), str(diff), view], capture_output=True, text=True)
            if proc.returncode != 0:
                err = proc.stderr.strip()[-200:].replace("\t", " ").replace("\n", " ")
                continue
            kv = dict(x.split("=", 1) for x in proc.stdout.split() if "=" in x)
            res = classify(diff, r["artifact"])
            score = (res[1] + res[2]) * 100000 + res[3] + res[4]
            if best is None or score < best[0]:
                best = (score, res, kv, diff, view)
        if best is None:
            out.append((did, "-", "-", "-", "-", "-", "-", "-", "TOOL_FAILED", err, "", "", "", "")); continue
        score, (verdict, missing, extra, rp, rb, benign), kv, diff, view = best
        res_missing, res_diff = int(kv.get("resources_published_only", "0")), int(kv.get("resources_differ", "0"))
        if verdict in ADMISSIBLE and (res_missing or res_diff):
            verdict = "DIFFERS_RESOURCES"
        out.append((did, kv["published_classes"], kv["built_classes"], kv["identical_classes"], str(missing), str(extra), str(rp), str(rb), verdict, diff.name, str(benign), view, kv.get("published_versions", ""), kv.get("built_versions", ""), kv.get("resources_published", ""), str(res_missing), str(res_diff), kv.get("resources_built_only", "")))
        print(did, verdict, view, file=sys.stderr)
    args.out.write_text("\n".join(["donor\tpublished_classes\tbuilt_classes\tidentical_classes\tmissing_classes\tbuilt_only_classes\treal_published_only_lines\treal_built_only_lines\tverdict\tdiff\tcompiler_artifact_lines\tview\tpublished_class_versions\tbuilt_class_versions\tresources_published\tresources_missing\tresources_differ\tresources_built_only"] + ["\t".join(x) for x in out]) + "\n", encoding="utf-8", newline="\n")
    counts = {}
    for x in out:
        counts[x[8]] = counts.get(x[8], 0) + 1
    print("api gate", counts)
    return 0


if __name__ == "__main__":
    sys.exit(main())
