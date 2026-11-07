#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""mutants.py <work-dir> <classpath-extra> : COMPILER_REFERENCE_MUTANTS v1.
Each mutant changes exactly one site of the relocated main sources (refused when the site is not unique), recompiles
the module's main and test sources with javac into <work-dir>/<id>, and runs the three donor proofs plus the fact
probe (150 units). A mutant is KILLED when a proof exits non-zero or the probe digest differs from the unmutated
digest. Prints one row per mutant and the kill count; exits non-zero when any mutant survives."""
import os
import shutil
import subprocess
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
PKG = MODULE / "src/main/java/jdk/m3/compiler/reference"
TEST = MODULE / "src/test/java/jdk/m3/compiler/reference"
PROBE = MODULE / "recipe/CrefFactProbe.java"
JAVA = Path(os.environ.get("JAVA_HOME", "")) / "bin"
PROOFS = ["M3CRefPathCertaintyProof", "M3CompilerReferenceCensusProof", "M3ReferenceFlowQualification"]
MUTANTS = [
    ("liveness-def-kill", "M3Liveness.java", "next.andNot(def[node]); next.or(use[node]);",
     "next.or(use[node]);"),
    ("path-must-join-or", "M3CRefPathCertainty.java", "mustJoin.and(other.must);", "mustJoin.or(other.must);"),
    ("path-unknown-guard", "M3CRefPathCertainty.java", "mayLocal[0]==0", "mayLocal[0]>=0"),
    ("census-ending-diff", "M3CompilerReferenceCensus.java", "        scratch.andNot(after);\n", "\n"),
    ("census-upper-before", "M3CompilerReferenceCensus.java",
     "public int possibleBefore(int site) { return upper(knownBefore, site); }",
     "public int possibleBefore(int site) { return upper(knownAfter, site); }"),
    ("flow-escaped-mask", "M3ReferenceFlow.java", "escaped.and(exposed);", "escaped.andNot(exposed);"),
]


def run(args):
    return subprocess.run([str(a) for a in args], capture_output=True, text=True)


def build_and_check(out, sources, extra):
    if out.exists():
        shutil.rmtree(out)
    (out / "probe").mkdir(parents=True)
    main = [p for p in sources.rglob("*.java") if p.name != "module-info.java"]
    r = run([JAVA / "javac", "--release", "21", "-proc:none", "-nowarn", "-cp", extra, "-d", out, *main,
             *TEST.glob("*Proof.java"), TEST / "M3ReferenceFlowQualification.java", PROBE])
    if r.returncode:
        return "COMPILE_FAIL", r.stderr[:200]
    cp = os.pathsep.join([str(out), extra])
    failed = [p for p in PROOFS if run([JAVA / "java", "-cp", cp, f"jdk.m3.compiler.reference.{p}"]).returncode]
    probe = run([JAVA / "java", "-cp", cp, "jdk.m3.compiler.reference.CrefFactProbe", "0x4d334d5554", "150"])
    return ",".join(failed) or "-", probe.stdout.strip().splitlines()[-1] if probe.stdout.strip() else probe.stderr[:200]


def main():
    work, extra = Path(sys.argv[1]), sys.argv[2]
    _, base = build_and_check(work / "base", PKG.parent.parent.parent, extra)
    print(f"base\tprobe\t{base}")
    killed = 0
    for mid, name, old, new in MUTANTS:
        src = work / "src" / mid
        if src.exists():
            shutil.rmtree(src)
        shutil.copytree(MODULE / "src/main/java", src)
        path = src / "jdk/m3/compiler/reference" / name
        text = path.read_text(encoding="utf-8")
        if text.count(old) != 1:
            sys.exit(f"REFUSE {mid}: site found {text.count(old)}x")
        path.write_text(text.replace(old, new), encoding="utf-8", newline="")
        failed, probe = build_and_check(work / mid, src, extra)
        dead = failed != "-" or probe != base
        killed += dead
        print(f"{mid}\t{'KILLED' if dead else 'SURVIVED'}\tproofs_failed={failed}\tprobe_changed={probe != base}")
    print(f"mutants killed={killed}/{len(MUTANTS)}")
    sys.exit(0 if killed == len(MUTANTS) else 1)


main()
