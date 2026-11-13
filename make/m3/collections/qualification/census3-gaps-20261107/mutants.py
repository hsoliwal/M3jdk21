#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""census3 m3/collections mutants: each mutant edits one restored member of the applied source, is compiled over the
head classes and replayed through the differential owner side; KILLED = digest differs from the donor digest.

usage: python -I mutants.py <repo-root> <head-classes> <diff-out-dir (from differential.sh)> <work-dir> [episodes]
"""
import hashlib
import shutil
import subprocess
import sys
from pathlib import Path

SRC = "m3/collections/src/main/java/jdk/m3/collections/"
DEQUE, TREE = SRC + "M3MonotoneLongDeque.java", SRC + "M3PersistentLongSumTree.java"
MUTANTS = (
    ("M01 lastIndex reads the head", DEQUE, "        return indexes[physical(size - 1)];\n", "        return indexes[head];\n"),
    ("M02 lastValue reads the head", DEQUE, "        requireNonEmpty();\n        return values[physical(size - 1)];\n",
     "        requireNonEmpty();\n        return values[physical(0)];\n"),
    ("M03 requireNonEmpty never fires", DEQUE, "    private void requireNonEmpty() {\n        if (size == 0) {",
     "    private void requireNonEmpty() {\n        if (size < 0) {"),
    ("M04 zero delta copies the path", TREE, "        if (delta == 0L) {\n", "        if (delta == 1L) {\n"),
    ("M05 unchecked path sums", TREE, "            scratchSums[depth] = Math.addExact(sums[oldNode], delta);\n",
     "            scratchSums[depth] = sums[oldNode] + delta;\n"),
    ("M06 split goes right only above middle", TREE, "            boolean goRight = index >= middle;\n",
     "            boolean goRight = index > middle;\n"),
    ("M07 node ids pre-increment", TREE, "            int created = nodeCount++;\n", "            int created = ++nodeCount;\n"),
    ("M08 rootNode returns version 0", TREE, "        checkVersion(version);\n        return roots[version];\n    }\n\n"
     "    private int update(", "        checkVersion(version);\n        return roots[0];\n    }\n\n    private int update("),
    ("M09 presizing ctor admits length 0", TREE, "        if (length <= 0) {\n", "        if (length < 0) {\n"),
    ("M10 presizing ctor admits expected -1", TREE, "        if (expectedAdditionalVersions < 0) {\n",
     "        if (expectedAdditionalVersions < -1) {\n"),
    ("M11 length() off by one", TREE, "    public int length() { return size; }\n", "    public int length() { return size - 1; }\n"),
    ("M12 forkAdd admits index == length", TREE, "        Objects.checkIndex(index, size);\n\n        ensureRootCapacity",
     "        Objects.checkIndex(index, size + 1);\n\n        ensureRootCapacity"),
)


def main():
    root, head, diff, work = map(Path, sys.argv[1:5])
    episodes = sys.argv[5] if len(sys.argv) > 5 else "2000"
    jdk = Path("C:/Program Files/Java/jdk-21/bin")
    tmp = "-Djava.io.tmpdir=S:/m3-scratch/eb-conv/c3g/tmp"
    donor_cp = "%s;%s" % (diff / "donor", sys.argv[6] if len(sys.argv) > 6 else "S:/m3-scratch/eb-conv/c3g/donor-classes-mindex")
    want = subprocess.run([str(jdk / "java"), tmp, "-cp", donor_cp, "census3.Census3CollectionsDifferential", "donor",
                           episodes], capture_output=True, text=True, check=True).stdout
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
        cp = "%s;%s;%s" % (mdir / "classes", diff / "owner", head)
        run = subprocess.run([str(jdk / "java"), tmp, "-cp", cp, "census3.Census3CollectionsDifferential", "owner",
                              episodes], capture_output=True, text=True)
        got = run.stdout if run.returncode == 0 else "EXIT %d %s" % (run.returncode, run.stderr[-200:])
        state = "SURVIVED" if got == want else "KILLED"
        killed += state == "KILLED"
        print("MUTANT %s %s %s" % (name, state, hashlib.sha256(got.encode()).hexdigest()[:12]))
    print("MUTANTS killed=%d/%d episodes=%s" % (killed, len(MUTANTS), episodes))


if __name__ == "__main__":
    main()
