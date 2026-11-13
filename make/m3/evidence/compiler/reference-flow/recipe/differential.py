#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""differential.py <donor-checkout> <work-dir> <classpath-extra> [seed] [units] : COMPILER_REFERENCE_DIFFERENTIAL v1.
Compiles the donor closure (the FILE rows of RELOCATION_MAP.tsv in the donor package, plus the donor progress types
of the TYPE rows and their two implementation siblings named in PROGRESS_SIBLINGS) and the module's own main and test
sources, then runs, under JIT and -Xint, the three donor proofs and the fact probe (recipe/CrefFactProbe.java, whose
package line is mapped back to the donor package for the donor run). Every output must be byte-identical. All donor
names come from RELOCATION_MAP.tsv."""
import os
import shutil
import subprocess
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROWS = [l.split("\t") for l in (MODULE / "recipe/RELOCATION_MAP.tsv").read_text(encoding="utf-8").splitlines()]
COL = {n: i for i, n in enumerate(ROWS[0])}
JAVA = Path(os.environ.get("JAVA_HOME", "")) / "bin"
PROOFS = ["M3CRefPathCertaintyProof", "M3CompilerReferenceCensusProof", "M3ReferenceFlowQualification"]
PROGRESS_SIBLINGS = ["ChildProgressMonitor", "DefaultProgressMonitor"]


def rows(kind):
    return [r for r in ROWS[1:] if r[COL["kind"]] == kind]


def run(args):
    return subprocess.run([str(a) for a in args], capture_output=True)


def main():
    donor, work, extra = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3]
    seed, units = (sys.argv[4:6] + ["0x4d3343524546", "400"][len(sys.argv[4:6]):])[:2]
    module = rows("MODULE")[0][COL["donor"]]
    dpkg, tpkg = rows("PACKAGE")[0][COL["donor"]], rows("PACKAGE")[0][COL["target"]]
    progress = [t[COL["donor"]] for t in rows("TYPE")]
    common_dir = progress[0].rsplit(".", 1)[0].replace(".", "/")
    common_root = next(p for p in donor.glob(f"*/src/main/java/{common_dir}") if p.is_dir())
    if work.exists():
        shutil.rmtree(work)
    (work / "donor").mkdir(parents=True)
    (work / "relocated").mkdir()
    donor_src = [common_root / (t.rsplit(".", 1)[1] + ".java") for t in progress]
    donor_src += [common_root / f"{s}.java" for s in PROGRESS_SIBLINGS]
    for r in rows("FILE"):
        name = r[COL["donor"]]
        if r[COL["side"]] == "main" or name in PROOFS:
            donor_src.append(donor / module / "src" / r[COL["side"]] / "java" / dpkg.replace(".", "/") / f"{name}.java")
    probe = (MODULE / "recipe/CrefFactProbe.java").read_text(encoding="utf-8")
    (work / "DonorProbe").mkdir()
    (work / "DonorProbe/CrefFactProbe.java").write_text(
        probe.replace(f"package {tpkg};", f"package {dpkg};", 1), encoding="utf-8", newline="")
    r1 = run([JAVA / "javac", "--release", "21", "-proc:none", "-nowarn", "-d", work / "donor", *donor_src,
              work / "DonorProbe/CrefFactProbe.java"])
    main_src = [p for p in (MODULE / "src/main/java").rglob("*.java") if p.name != "module-info.java"]
    r2 = run([JAVA / "javac", "--release", "21", "-proc:none", "-nowarn", "-cp", extra, "-d", work / "relocated",
              *main_src, *[MODULE / "src/test/java" / tpkg.replace(".", "/") / f"{p}.java" for p in PROOFS],
              MODULE / "recipe/CrefFactProbe.java"])
    if r1.returncode or r2.returncode:
        sys.exit("COMPILE_FAIL " + (r1.stderr + r2.stderr).decode()[:400])
    rcp = os.pathsep.join([str(work / "relocated"), extra])
    bad = 0
    for mode in ("jit", "xint"):
        flags = ["-Xint"] if mode == "xint" else []
        for main_class, args in [(p, []) for p in PROOFS] + [("CrefFactProbe", [seed, units])]:
            d = run([JAVA / "java", *flags, "-cp", work / "donor", f"{dpkg}.{main_class}", *args])
            t = run([JAVA / "java", *flags, "-cp", rcp, f"{tpkg}.{main_class}", *args])
            same = d.returncode == t.returncode == 0 and d.stdout == t.stdout
            bad += not same
            last = t.stdout.decode().strip().splitlines()[-1] if t.stdout.strip() else t.stderr.decode()[:200]
            print(f"{mode}\t{main_class}\t{'IDENTICAL' if same else 'DIFFERENT'}\trc={d.returncode}/{t.returncode}\t{last}")
    sys.exit(1 if bad else 0)


main()
