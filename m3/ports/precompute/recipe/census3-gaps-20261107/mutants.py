#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""census3 m3/ports/precompute mutants (P3-010): each mutant edits one restored member of the applied source, is compiled
over the head classes and replayed through the differential owner side; KILLED = digest differs from the donor digest.

usage: python -I mutants.py <repo-root> <head-classes> <diff-out-dir (from differential.sh)> <work-dir> <donor-classes> [episodes]
"""
import hashlib
import shutil
import subprocess
import sys
from pathlib import Path

PLAN = "m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java"
MUTANTS = (
    ("F01 advanceRow diagonal from left", "            diagonal = above;\n", "            diagonal = left;\n"),
    ("F02 input budget exclusive", "            if (total > limits.maxInputUnits()) {", "            if (total >= limits.maxInputUnits()) {"),
    ("F03 cell budget off", "            if (cells > limits.maxCellUpdates()) {", "            if (cells < 0) {"),
    ("F04 empty append charges bytes", "            long bytes = count == 0 ? 0L : 2L * Integer.BYTES * row.length;",
     "            long bytes = 2L * Integer.BYTES * row.length;"),
    # (the boundary variant '<=' -> '<' is an equivalent mutant: row[0] == inputUnits on every checkpoint)
    ("F05 minimum bound beyond the pattern", "                    : (long) inputUnits + remainingUnits - width;",
     "                    : (long) inputUnits + remainingUnits;"),
    ("F06 within exclusive", "            return distance() <= maximum;", "            return distance() < maximum;"),
    ("F07 empty extend allocates", "            if (count == 0) return this;\n", "            if (count < 0) return this;\n"),
    ("F08 frontier row not identity", "            row[j] = j;\n", "            row[j] = 0;\n"),
    ("F09 flag inverted on the DP path", "            return new M3EditDistancePlan(snapshot, null, null, 0L, 0L, utf16CodeUnits);",
     "            return new M3EditDistancePlan(snapshot, null, null, 0L, 0L, !utf16CodeUnits);"),
    ("F10 cellUpdates without max(1)", "            return (long) inputUnits * Math.max(1, owner.pattern.length);",
     "            return (long) inputUnits * owner.pattern.length;"),
    ("F11 monitor never checked", "        if (monitor != null) monitor.checkCanceled();\n        if (Thread",
     "        if (Thread"),
    ("F12 limits admit MAX_VALUE", "            if (maxInputUnits < 0 || maxInputUnits == Integer.MAX_VALUE",
     "            if (maxInputUnits < 0 || maxInputUnits == Integer.MIN_VALUE"),
    ("F13 progress reported once", "                if ((index & 1023) == 1023 || index == count - 1) {",
     "                if (index == count - 1) {"),
    ("F14 extend consumed off by one", "                advanceRow(owner.pattern, previous, current, unit, inputUnits + index + 1, monitor);",
     "                advanceRow(owner.pattern, previous, current, unit, inputUnits + index, monitor);"),
)


def main():
    root, head, diff, work, donor = map(Path, sys.argv[1:6])
    episodes = sys.argv[6] if len(sys.argv) > 6 else "2000"
    jdk = Path("C:/Program Files/Java/jdk-21/bin")
    tmp = "-Djava.io.tmpdir=S:/m3-scratch/eb-conv/c3g/tmp"
    want = subprocess.run([str(jdk / "java"), tmp, "-cp", "%s;%s" % (diff / "donor", donor),
                           "census3.Census3FrontierDifferential", "donor", episodes], capture_output=True, text=True,
                          check=True).stdout
    killed = 0
    for name, old, new in MUTANTS:
        text = (root / PLAN).read_text(encoding="utf-8")
        if text.count(old) != 1:
            print("MUTANT %s ANCHOR_COUNT=%d" % (name, text.count(old)))
            continue
        mdir = work / name.split()[0]
        shutil.rmtree(mdir, ignore_errors=True)
        (mdir / "src").mkdir(parents=True)
        (mdir / "classes").mkdir()
        msrc = mdir / "src" / Path(PLAN).name
        msrc.write_text(text.replace(old, new), encoding="utf-8", newline="\n")
        subprocess.run([str(jdk / "javac"), "-J" + tmp, "-nowarn", "-cp", str(head), "-d", str(mdir / "classes"),
                        str(msrc)], check=True, capture_output=True)
        run = subprocess.run([str(jdk / "java"), tmp, "-cp", "%s;%s;%s" % (mdir / "classes", diff / "owner", head),
                              "census3.Census3FrontierDifferential", "owner", episodes], capture_output=True, text=True)
        got = run.stdout if run.returncode == 0 else "EXIT %d %s" % (run.returncode, run.stderr[-200:])
        state = "SURVIVED" if got == want else "KILLED"
        killed += state == "KILLED"
        print("MUTANT %s %s %s" % (name, state, hashlib.sha256(got.encode()).hexdigest()[:12]))
    print("MUTANTS killed=%d/%d episodes=%s" % (killed, len(MUTANTS), episodes))


if __name__ == "__main__":
    main()
