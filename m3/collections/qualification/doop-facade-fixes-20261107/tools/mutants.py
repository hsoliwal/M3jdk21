#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Test-fails-on-master and mutant gates of recipe qualification/doop-facade-fixes-20261107 (stdlib only).

Every row of ../MUTANTS.tsv edits one copy of M3TransientFacadeStore.java (source=head: the module file; source=master:
the pinned preimage blob of ../EDITS.tsv, read with git cat-file) by exact find/replace pairs (each find must match
exactly once; rows with the same id apply in order). The edited file alone is compiled against the head (or master)
classes into an overlay directory that precedes them on the class path, and the JUnit console launcher runs the two
relocated tests plus the store's existing test family. A mutant is KILLED when a relocated test of its leaf fails.
Gate rows: MASTER (master classes, no edit; test A must fail, test B must not compile: the seam is absent), SHIM
(master body behind the package-private seam), HEAD (no edit; every selected test passes).
usage: mutants.py --module DIR --head-classes DIR --head-test-classes DIR --master-classes DIR --console JAR
                  --out DIR [--java-home DIR]
                  [--tmp DIR]   (exit 1 when a gate fails or a mutant survives)"""
from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
STORE = "src/main/java/jdk/m3/collections/M3TransientFacadeStore.java"
PKG = "jdk.m3.collections."
TESTS = {"A": "M3FactoryReentrantDuplicateAdmissionTest", "B": "M3FacadeIdleAtomicGrowthTest"}
FAMILY = ("M3TransientFacadeStoreTest", "M3TransientFacadePressureTest", "M3TransientFacadeRecycleTest",
          "M3FacadeAtomicGrowthTest", "M3FacadeLifecycleReentryTest", "M3FacadeDisposerReentryTest")


def read_tsv(path: Path) -> list:
    lines = [line for line in path.read_text(encoding="utf-8").splitlines() if line and not line.startswith("#")]
    header = lines[0].split("\t")
    return [dict(zip(header, line.split("\t"))) for line in lines[1:]]


def unescape(text: str) -> str:
    return text.replace("\\n", "\n").replace("\\t", "\t")


class Runner:
    def __init__(self, args):
        self.args, self.out = args, Path(args.out)
        self.java = Path(args.java_home) / "bin"
        self.tmp = "-Djava.io.tmpdir=" + args.tmp
        self.sep = ";" if sys.platform.startswith("win") else ":"

    def javac(self, dest: Path, cp: list, sources: list) -> tuple[int, str]:
        shutil.rmtree(dest, ignore_errors=True)
        dest.mkdir(parents=True)
        done = subprocess.run([str(self.java / "javac"), "-J" + self.tmp, "--release", "21", "-proc:none",
                               "-implicit:none", "-nowarn", "-cp", self.sep.join(cp), "-d", str(dest), *sources],
                              capture_output=True, text=True)
        return done.returncode, done.stdout + done.stderr

    def junit(self, label: str, cp: list, classes: list) -> dict:
        reports = self.out / ("reports-" + label)
        shutil.rmtree(reports, ignore_errors=True)
        select = [arg for name in classes for arg in ("--select-class", PKG + name)]
        subprocess.run([str(self.java / "java"), self.tmp, "-jar", self.args.console, "execute", "--disable-banner",
                        "--details=none", "--reports-dir", str(reports), "--class-path", self.sep.join(cp), *select],
                       capture_output=True, text=True)
        result = {name: [0, 0] for name in classes}
        for xml in reports.glob("*.xml"):
            for case in ET.parse(xml).getroot().iter("testcase"):
                name = case.get("classname", "").rsplit(".", 1)[-1]
                if name in result:
                    result[name][0] += 1
                    result[name][1] += any(case.find(k) is not None for k in ("failure", "error"))
        return result


def edited_source(row_group: list, module: Path) -> str:
    source = row_group[0]["source"]
    if source == "head":
        text = (module / STORE).read_text(encoding="utf-8")
    else:
        edits = read_tsv(HERE / "EDITS.tsv")
        blob = next(r["host_blob"] for r in edits if r["target"] == STORE and r["host_blob"] != "-")
        text = subprocess.run(["git", "-C", str(module), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout.decode("utf-8")
    for row in row_group:
        if row["find"] == "-":
            continue
        find, replace = unescape(row["find"]), unescape(row["replace"])
        if text.count(find) != 1:
            raise SystemExit("mutant %s: find matches %d times" % (row["id"], text.count(find)))
        text = text.replace(find, replace)
    return text


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    for name in ("--module", "--head-classes", "--head-test-classes", "--master-classes", "--console", "--out"):
        parser.add_argument(name, required=True)
    parser.add_argument("--java-home", default="C:/Program Files/Java/jdk-21")
    parser.add_argument("--tmp", default=".")
    args = parser.parse_args()
    run, module, out = Runner(args), Path(args.module), Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    tests = [str(module / ("src/test/java/jdk/m3/collections/%s.java" % TESTS[leaf])) for leaf in "AB"]
    rc, log = run.javac(out / "tests-head", [args.head_classes, args.console], tests)
    if rc:
        raise SystemExit("relocated tests do not compile against head:\n" + log)
    groups: dict = {}
    for row in read_tsv(HERE / "MUTANTS.tsv"):
        groups.setdefault(row["id"], []).append(row)
    lines, failed = [], 0
    for mid, group in groups.items():
        kind, leaf, source = group[0]["kind"], group[0]["leaf"], group[0]["source"]
        base = args.head_classes if source == "head" else args.master_classes
        test_dir, overlay = out / "tests-head", out / ("overlay-" + mid)
        if kind == "MASTER":  # unedited master classes: test A compiled against them, test B must not compile
            rc_a, _ = run.javac(out / "tests-master-A", [base, args.console], tests[:1])
            rc_b, log_b = run.javac(out / "tests-master-B", [base, args.console], tests[1:])
            errors = [line.split(": error: ", 1)[1] for line in log_b.splitlines() if ": error: " in line]
            res = run.junit(mid, [str(out / "tests-master-A"), base, args.console], [TESTS["A"]])
            ok = rc_a == 0 and res[TESTS["A"]][1] > 0 and rc_b != 0
            lines.append("%s %s testA=%d/%d failed testB=javac_exit_%d errors=%d (%s) %s" % (
                mid, kind, res[TESTS["A"]][1], res[TESTS["A"]][0], rc_b, len(errors), "; ".join(sorted(set(errors))),
                "PASS" if ok else "FAIL"))
            failed += not ok
            continue
        if source == "head" and group[0]["find"] == "-":
            cp = [str(test_dir), args.head_test_classes, base, args.console]
        else:
            src_dir = out / ("src-" + mid) / "jdk/m3/collections"
            shutil.rmtree(src_dir.parent.parent.parent, ignore_errors=True)
            src_dir.mkdir(parents=True)
            (src_dir / "M3TransientFacadeStore.java").write_text(edited_source(group, module), encoding="utf-8",
                                                                 newline="\n")
            rc, log = run.javac(overlay, [base], [str(src_dir / "M3TransientFacadeStore.java")])
            if rc:
                raise SystemExit("row %s does not compile:\n%s" % (mid, log))
            cp = [str(overlay), str(test_dir), args.head_test_classes, base, args.console]
        selected = [TESTS[leaf]] if kind == "SHIM" else [TESTS["A"], TESTS["B"], *FAMILY]
        res = run.junit(mid, cp, selected)
        detail = " ".join("%s=%d/%d" % (name, res[name][1], res[name][0]) for name in selected)
        if kind == "HEAD":
            ok = all(f == 0 and n > 0 for n, f in res.values())
            verdict = "PASS" if ok else "FAIL"
        elif kind == "SHIM":
            ok, verdict = True, "RECORDED"
        else:
            ok = res[TESTS[leaf]][1] > 0
            verdict = "KILLED" if ok else "SURVIVED"
        failed += not ok
        lines.append("%s %s leaf=%s %s failed/total: %s" % (mid, kind, leaf, verdict, detail))
    (out / "MUTANTS_RESULT.txt").write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    print("\n".join(lines))
    print("MUTANTS rows=%d failed=%d" % (len(lines), failed))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
