#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Mutation gate of primitive-method-gaps-20261107: every absorbed member has at least one killed mutant.

Each mutant is one anchored text substitution (anchor exactly once) in one head owner. The mutated owner is compiled
alone into an overlay directory placed before the head classes, then the two focused test classes run on the JUnit
console launcher; a mutant is KILLED when at least one test fails or errors, SURVIVED otherwise. Writes MUTANTS.tsv.

usage: python -I mutants.py --src M3_COLLECTIONS_SRC_MAIN --classes HEAD_CLASSES_PATH --tests TEST_CLASSES_DIR
                            --console JUNIT_CONSOLE_JAR --work WORK_DIR --out MUTANTS.tsv
"""
import argparse
import re
import shutil
import subprocess
import sys
from pathlib import Path

JDK = Path("C:/Program Files/Java/jdk-21/bin")
PKG = "jdk/m3/collections/"
TESTS = ("jdk.m3.collections.M3PrimitiveMethodGapsDifferentialTest", "jdk.m3.collections.M3LeanGapSortedPortTest")

# id, member, owner, old, new
MUTANTS = (
    ("M01", "M3LongTreeSet.get(int)", "M3LongTreeSet", "            page = -page - 2;\n", "            page = -page - 1;\n"),
    ("M02", "M3LongTreeSet.get(int)", "M3LongTreeSet", "        if (prefix != null && prefixRevision == modCount) {\n",
     "        if (prefix != null) {\n"),
    ("M03", "M3LongTreeSet.indexOf(long)", "M3LongTreeSet", "        return found >= 0 ? found + shift : found - shift;\n",
     "        return found >= 0 ? found + shift : -1;\n"),
    ("M04", "M3LongTreeSet.indexOf(long)", "M3LongTreeSet",
     "        if (pageCount == 0) {\n            return -1;\n        }\n        int page = locatePage(value);\n        int offset = pageOffset(page);\n        int found",
     "        if (pageCount == 0) {\n            return 0;\n        }\n        int page = locatePage(value);\n        int offset = pageOffset(page);\n        int found"),
    ("M05", "M3LongTreeSet.addAllSorted(long[],int,int)", "M3LongTreeSet", "            if (sorted[i] < sorted[i - 1]) {\n",
     "            if (sorted[i] <= sorted[i - 1]) {\n"),
    ("M06", "M3LongTreeSet.addAllSorted(long[],int,int)", "M3LongTreeSet", "M3LaneSearch.upperBound(sorted, from, to, last())",
     "M3LaneSearch.lowerBound(sorted, from, to, last())"),
    ("M07", "M3LongTreeSet.addAllSorted(long[],int,int)", "M3LongTreeSet",
     "            if (i == from || sorted[i] != sorted[i - 1]) {\n", "            if (i == from || i != from) {\n"),
    ("M08", "M3LongTreeSet.addAllSorted(long[],int,int)", "M3LongTreeSet",
     "        // Count the change first: a capacity failure part-way must still invalidate the prefix lane.\n        modCount++;\n",
     "        // Count the change first: a capacity failure part-way must still invalidate the prefix lane.\n"),
    ("M09", "M3LongTreeSet.payloadBytes() prefix lane", "M3LongTreeSet",
     "        long prefixBytes = pagePrefix == null ? 0L : (long) pagePrefix.length * Integer.BYTES;\n",
     "        long prefixBytes = 0L;\n"),
    ("M10", "M3LongTreeSet.trimToSize() prefix lane", "M3LongTreeSet",
     "        int[] nextPrefix = pagePrefix == null ? null : Arrays.copyOf(pagePrefix, pages);\n",
     "        int[] nextPrefix = pagePrefix;\n"),
    ("M11", "M3LongLongTreeMap.keyAt(int)", "M3LongLongTreeMap", "        return keys[Objects.checkIndex(index, size)];\n",
     "        return values[Objects.checkIndex(index, size)];\n"),
    ("M12", "M3LongLongTreeMap.keyAt(int)", "M3LongLongTreeMap", "        return keys[Objects.checkIndex(index, size)];\n",
     "        return keys[index];\n"),
    ("M13", "M3LongLongTreeMap.valueAt(int)", "M3LongLongTreeMap", "        return values[Objects.checkIndex(index, size)];\n",
     "        return keys[Objects.checkIndex(index, size)];\n"),
    ("M14", "M3LongLongTreeMap.indexOfKey(long)", "M3LongLongTreeMap", "        return Arrays.binarySearch(keys, 0, size, key);\n    }\n\n    @Override\n    public boolean containsKey",
     "        return Math.max(-1, Arrays.binarySearch(keys, 0, size, key));\n    }\n\n    @Override\n    public boolean containsKey"),
    ("M15", "M3LongLongTreeMap.putAllSorted(long[],long[],int,int)", "M3LongLongTreeMap",
     "        if (from == 0 && to == sourceKeys.length) {\n", "        if (to == sourceKeys.length) {\n"),
    ("M16", "M3LongLongTreeMap.putAllSorted(long[],long[],int,int)", "M3LongLongTreeMap",
     "        Objects.checkFromToIndex(from, to, sourceKeys.length);\n        if (from == 0",
     "        if (from == 0"),
    ("M17", "M3IntIntHashMap.trimToSize()", "M3IntIntHashMap", "        if (target < keys.length) {\n            rebuildProbeLane",
     "        if (target < 0) {\n            rebuildProbeLane"),
    ("M18", "M3IntIntHashMap.trimToSize()", "M3IntIntHashMap", "            rebuildProbeLane(target);\n            modCount++;\n",
     "            rebuildProbeLane(target);\n"),
    ("M19", "M3IntIntHashMap.trimToSize()", "M3IntIntHashMap", "        int target = capacityFor(size - (hasZero ? 1 : 0));\n",
     "        int target = capacityFor(size + 1);\n"),
    ("M20", "M3IntLongHashMap.trimToSize()", "M3IntLongHashMap", "        if (target < keys.length) {\n            rebuildProbeLane",
     "        if (target < 0) {\n            rebuildProbeLane"),
    ("M21", "M3IntLongHashMap.trimToSize()", "M3IntLongHashMap", "                nextValues[pos] = values[i];\n",
     "                nextValues[pos] = values[pos];\n"),
    ("M22", "M3LongLongHashMap.trimToSize()", "M3LongLongHashMap", "        if (target < keys.length || occupied != size) {\n",
     "        if (target < keys.length && occupied != size) {\n"),
    ("M23", "M3LongLongHashMap.trimToSize()", "M3LongLongHashMap", "            rehash(target);\n            modCount++;\n",
     "            rehash(target);\n"),
    ("M24", "M3IntArrayDeque.get(int)", "M3IntArrayDeque", "java.util.Objects.checkIndex(index, size)) & (elements.length - 1)];",
     "java.util.Objects.checkIndex(index, size))];"),
    ("M25", "M3IntArrayDeque.get(int)", "M3IntArrayDeque", "java.util.Objects.checkIndex(index, size)) & (elements.length - 1)];",
     "java.util.Objects.checkIndex(index, elements.length)) & (elements.length - 1)];"),
)


def run_one(args, mutant):
    mid, member, owner, old, new = mutant
    source = Path(args.src) / PKG / (owner + ".java")
    text = source.read_bytes().decode("utf-8").replace("\r\n", "\n")
    if text.count(old) != 1:
        return mid, member, owner, "ANCHOR_%d" % text.count(old), "", ""
    work = Path(args.work) / mid
    shutil.rmtree(work, ignore_errors=True)
    (work / "src" / PKG).mkdir(parents=True)
    (work / "classes").mkdir()
    (work / "src" / PKG / (owner + ".java")).write_bytes(text.replace(old, new).encode("utf-8"))
    tmp = "-J-Djava.io.tmpdir=" + str(Path(args.work) / "tmp")
    compiled = subprocess.run([str(JDK / "javac"), tmp, "--release", "21", "-nowarn", "-cp", args.classes,
                               "-d", str(work / "classes"), str(work / "src" / PKG / (owner + ".java"))],
                              capture_output=True, text=True)
    if compiled.returncode != 0:
        return mid, member, owner, "COMPILE_ERROR", "", compiled.stderr.splitlines()[0] if compiled.stderr else ""
    cp = ";".join([str(work / "classes"), args.tests, args.classes])
    command = [str(JDK / "java"), "-Djava.io.tmpdir=" + str(Path(args.work) / "tmp"), "-jar", args.console, "execute",
               "-cp", cp, "--disable-banner", "--details=summary", "--disable-ansi-colors"]
    for test in TESTS:
        command += ["--select-class", test]
    result = subprocess.run(command, capture_output=True, text=True)
    failed = re.search(r"(\d+) tests failed", result.stdout)
    count = int(failed.group(1)) if failed else -1
    state = "KILLED" if count > 0 else ("SURVIVED" if count == 0 else "NO_RESULT")
    first = next((line.strip() for line in result.stdout.splitlines() if "=>" in line or "Exception" in line), "")
    return mid, member, owner, state, str(count), first[:160]


def main() -> int:
    parser = argparse.ArgumentParser()
    for name in ("--src", "--classes", "--tests", "--console", "--work", "--out"):
        parser.add_argument(name, required=True)
    args = parser.parse_args()
    (Path(args.work) / "tmp").mkdir(parents=True, exist_ok=True)
    rows = []
    for mutant in MUTANTS:
        row = run_one(args, mutant)
        print("\t".join(row), flush=True)
        rows.append(row)
    with open(args.out, "w", encoding="utf-8", newline="\n") as out:
        out.write("mutant\tmember\towner\tstate\tfailed_tests\tfirst_signal\n")
        for row in rows:
            out.write("\t".join(row) + "\n")
    members = {}
    for row in rows:
        members.setdefault(row[1].split(" ")[0], set()).add(row[3])
    unkilled = sorted(m for m, states in members.items() if "KILLED" not in states)
    print("mutants=%d killed=%d members=%d members_without_kill=%s" % (
        len(rows), sum(r[3] == "KILLED" for r in rows), len(members), ",".join(unkilled) or "none"))
    return 0 if not unkilled else 1


if __name__ == "__main__":
    sys.exit(main())
