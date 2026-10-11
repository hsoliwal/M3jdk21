#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""DEPENDENCY-SOURCE-INLINE: resolve the exact compile classpath a donor was built against, from its own
published pom (compile + provided + optional, as Maven sees the donor as a project), into a separate local
repository on K: (never ~/.m2). Used for donors whose optional adapters/binders import APIs that are not
inlined (micrometer binders, assertj's junit4/hamcrest bridges, byte-buddy's findbugs/jna, jandex's ant task).
Those jars are compile-only build inputs: never inlined, never shipped, never a runtime dependency.

Output POM_CLASSPATH.tsv: donor, coordinate (g:a:v[:classifier]), declared (TOP|TRANSITIVE), scope, optional,
path, sha256. CPU + Maven Central only."""
from __future__ import annotations
import argparse, csv, hashlib, io, os, re, shutil, subprocess, sys
import xml.etree.ElementTree as ET
from pathlib import Path

NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def sha256(p: Path) -> str:
    h = hashlib.sha256()
    with open(p, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def coord_of(path: Path, repo: Path) -> str:
    rel = path.resolve().relative_to(repo.resolve()).as_posix().split("/")
    a, v, fname = rel[-3], rel[-2], rel[-1]
    g = ".".join(rel[:-3])
    stem = fname[:-4]
    classifier = stem[len(a) + 1 + len(v):].lstrip("-") if stem.startswith(a + "-" + v) else ""
    return "%s:%s:%s%s" % (g, a, v, (":" + classifier) if classifier else "")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--inventory", type=Path, nargs="+", required=True)
    ap.add_argument("--store", type=Path, required=True)
    ap.add_argument("--repo-local", type=Path, required=True)
    ap.add_argument("--work", type=Path, required=True)
    ap.add_argument("--out", type=Path, required=True)
    ap.add_argument("--java-home", default=r"C:\Program Files\Java\jdk-21")
    ap.add_argument("donors", nargs="+")
    args = ap.parse_args()
    rows = {}
    for inv in args.inventory:
        for r in csv.DictReader(io.open(inv, encoding="utf-8", newline=""), delimiter="\t"):
            rows[(r["artifact"] + "-" + r["version"]).replace("_", "-").lower()] = r
    out = []
    env = dict(os.environ, JAVA_HOME=args.java_home, MAVEN_OPTS="-Xmx768m")
    for did in args.donors:
        did = did.lower()
        r = rows[did]
        pom = args.store / r["group"].replace(".", "/") / r["artifact"] / r["version"] / (r["artifact"] + "-" + r["version"] + ".pom")
        work = args.work / did
        if work.exists():
            shutil.rmtree(work)
        work.mkdir(parents=True)
        text = pom.read_text(encoding="utf-8")
        text = re.sub(r"<packaging>[^<]*</packaging>", "<packaging>jar</packaging>", text, count=1)
        (work / "pom.xml").write_text(text, encoding="utf-8", newline="\n")
        t = ET.fromstring(pom.read_bytes())
        top = {}
        for d in t.findall("m:dependencies/m:dependency", NS):
            sc = d.findtext("m:scope", "compile", NS)
            if sc in ("compile", "provided", "system"):
                top[(d.findtext("m:groupId", "", NS), d.findtext("m:artifactId", "", NS))] = (sc, d.findtext("m:optional", "false", NS))
        cp_file = work / "cp.txt"
        cmd = ["mvn", "-B", "-ntp", "-q", "-f", str(work / "pom.xml"), "-Dmaven.repo.local=" + str(args.repo_local),
               "org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath", "-Dmdep.outputFile=" + str(cp_file), "-Dmdep.includeScope=compile"]
        with open(work / "mvn.log", "wb") as log:
            rc = subprocess.run(cmd, stdout=log, stderr=subprocess.STDOUT, env=env, shell=True).returncode
        if rc != 0 or not cp_file.exists():
            out.append((did, "-", "-", "-", "-", str(work / "mvn.log"), "RESOLVE_FAILED_EXIT_%d" % rc))
            print("FAIL", did, rc, file=sys.stderr)
            continue
        paths = [Path(p) for p in cp_file.read_text(encoding="utf-8").strip().split(os.pathsep) if p.strip()]
        for p in paths:
            c = coord_of(p, args.repo_local)
            g, a = c.split(":")[:2]
            sc, opt = top.get((g, a), ("transitive", "-"))
            out.append((did, c, "TOP" if (g, a) in top else "TRANSITIVE", sc, opt, str(p.resolve()), sha256(p)))
        print("OK", did, len(paths), "jars", file=sys.stderr)
    args.out.write_text("\n".join(["donor\tcoordinate\tdeclared\tscope\toptional\tpath\tsha256"] + ["\t".join(x) for x in out]) + "\n", encoding="utf-8", newline="\n")
    print("rows", len(out), "failed", sum(1 for x in out if x[6].startswith("RESOLVE_FAILED")))
    return 0


if __name__ == "__main__":
    sys.exit(main())
