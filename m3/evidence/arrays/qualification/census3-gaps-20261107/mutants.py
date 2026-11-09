#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""census3 m3/arrays mutants (P3-007): each mutant edits one restored or received member of the applied source, is
compiled over the head classes and replayed through the differential owner side; KILLED = digest differs from the
donor digest.

usage: python -I mutants.py <repo-root> <head-classes> <diff-out-dir (from differential.sh)> <work-dir> <donor-classes> [cases]
"""
import hashlib
import shutil
import subprocess
import sys
from pathlib import Path

SRC = "m3/arrays/src/main/java/jdk/m3/arrays/"
TYPE, ROLE = SRC + "M3StringTypeBits.java", SRC + "M3ArgumentRoleBits.java"
MUTANTS = (
    ("A01 NUMERIC is INTEGER only", TYPE, "NUMERIC = INTEGER | DECIMAL;", "NUMERIC = INTEGER;"),
    ("A02 type requireKnown keeps known bits", TYPE, "            throw new IllegalArgumentException(\"unknown M3String",
     "            if (bits == 0) throw new IllegalArgumentException(\"unknown M3String"),
    ("A03 type add inverted", TYPE, "        if ((bits & mask) != 0) out.add(name);", "        if ((bits & mask) == 0) out.add(name);"),
    ("A04 type names drops SYMBOL", TYPE, "        add(names, bits, SYMBOL, \"SYMBOL\");\n", ""),
    ("A05 type has is hasAny", TYPE, "        return (bits & required) == required;", "        return (bits & required) != 0;"),
    ("A06 type hasAny skips requireKnown", TYPE, "        requireKnown(mask);\n        return (bits & mask) != 0;",
     "        return (bits & mask) != 0;"),
    ("A07 role requireKnown inverted", ROLE, "        if ((bits & ~KNOWN_MASK) != 0) {", "        if ((bits & ~KNOWN_MASK) == 0) {"),
    ("A08 role add inverted", ROLE, "        if ((bits & mask) != 0) names.add(name);", "        if ((bits & mask) == 0) names.add(name);"),
    ("A09 role names order", ROLE, "        add(names, bits, CLAIM, \"CLAIM\");\n        add(names, bits, FACT, \"FACT\");\n",
     "        add(names, bits, FACT, \"FACT\");\n        add(names, bits, CLAIM, \"CLAIM\");\n"),
    ("A10 role has checks bits", ROLE, "        requireKnown(required);\n        return (bits & required) == required;",
     "        requireKnown(bits);\n        return (bits & required) == required;"),
)


def main():
    root, head, diff, work, donor = map(Path, sys.argv[1:6])
    cases = sys.argv[6] if len(sys.argv) > 6 else "20000"
    jdk = Path("C:/Program Files/Java/jdk-21/bin")
    tmp = "-Djava.io.tmpdir=S:/m3-scratch/eb-conv/c3g/tmp"
    want = subprocess.run([str(jdk / "java"), tmp, "-cp", "%s;%s" % (diff / "donor", donor),
                           "census3.Census3BitsDifferential", "donor", cases], capture_output=True, text=True,
                          check=True).stdout
    killed = 0
    for name, rel, old, new in MUTANTS:
        text = (root / rel).read_text(encoding="utf-8")
        if text.count(old) != 1:
            print("MUTANT %s ANCHOR_COUNT=%d" % (name, text.count(old)))
            continue
        mdir = work / name.split()[0]
        shutil.rmtree(mdir, ignore_errors=True)
        (mdir / "src").mkdir(parents=True)
        (mdir / "classes").mkdir()
        msrc = mdir / "src" / Path(rel).name
        msrc.write_text(text.replace(old, new), encoding="utf-8", newline="\n")
        subprocess.run([str(jdk / "javac"), "-J" + tmp, "-nowarn", "-cp", str(head), "-d", str(mdir / "classes"),
                        str(msrc)], check=True, capture_output=True)
        run = subprocess.run([str(jdk / "java"), tmp, "-cp", "%s;%s;%s" % (mdir / "classes", diff / "owner", head),
                              "census3.Census3BitsDifferential", "owner", cases], capture_output=True, text=True)
        got = run.stdout if run.returncode == 0 else "EXIT %d %s" % (run.returncode, run.stderr[-200:])
        state = "SURVIVED" if got == want else "KILLED"
        killed += state == "KILLED"
        print("MUTANT %s %s %s" % (name, state, hashlib.sha256(got.encode()).hexdigest()[:12]))
    print("MUTANTS killed=%d/%d cases=%s" % (killed, len(MUTANTS), cases))


if __name__ == "__main__":
    main()
