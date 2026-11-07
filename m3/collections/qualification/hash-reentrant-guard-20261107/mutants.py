#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Mutants of the guards of recipe HASH_REENTRANT_GUARD v1 (MUTANTS.tsv).

Each mutant edits pinned lines of head owners (each line must read exactly as listed, and the owners must equal the
recipe postimages), compiles them over the head classes, and is KILLED when an oracle notices:
M3HashReentrantLookupSelfTest (all, with the m3_arrays JNI library so the linked owners run), and, because M3SlotMap
is shared with M3TreeMap, #2676's M3TreeMapReentrantLookupSelfTest and the P2-098 M3FilteredCursorReadinessSelfTest.
SURVIVED rows are reported, never hidden; BACKSTOP marks a check kept deliberately redundant (insertAbsent's
precondition: its callers already proved the structure unchanged).
usage: python -I mutants.py --java-home DIR --classes DIR --test-classes DIR --arrays-jar JAR --native-lib FILE
                            --work DIR [--jvm-opt OPT] [--path-prepend DIR]
"""
from __future__ import annotations

import argparse
import hashlib
import os
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
SRC = HERE.parents[1] / "src/main/java/jdk/m3/collections"
SM, HM, LH, WH = "M3SlotMap.java", "M3HashMap.java", "M3LinkedHashMap.java", "M3WeakHashMap.java"
HEAD_SHA256 = {
    SM: "2206adec3e18b96b23a4fc70db1d09475dc618c05f1e059b4a78e5e9fa76d83d",
    HM: "0d3c317cb81bada479bbd27f911b28beacefbfc207775d6e22f475d4b1f78e4a",
    LH: "722b825f759f817d24bb2d765bf5996d66693b0230056d83c3d2b2d7c0fa0858",
    WH: "60b371cc1a17175277dde2f3ac9ad141b1b2daec18da85649d8522859ff6e12b",
}
ABSENT = "insertAbsent(key, v, (int) (located >>> 32), expected); }"
# id, check, [(file, line, expected text, replacement lines)], note
MUTANTS = (
    ("HM-1", "S1 M3HashMap find walks again after a key equals changed the structure",
     [(HM, 108, "                    if (modifications != expected) return rewalk(key, hash, walks); // equals changed"
       " the structure", [])], ""),
    ("LH-1", "S1 M3LinkedHashMap Table.find walks again after a key equals changed the structure",
     [(LH, 128, "                    if (modifications != expected) { return rewalk(key, hash, walks); } // equals"
       " changed it", [])], ""),
    ("WH-1", "S1 M3WeakHashMap find walks again after a key equals changed the structure",
     [(WH, 59, "                if (modifications != expected) { return rewalk(target, hash, walks); } // equals"
       " changed it", [])], ""),
    ("WH-2", "S1 M3WeakHashMap.put looks again when its pinned second hash call changed the structure",
     [(WH, 90, "            if (modifications == expected) { insertNew(key, value, hash); return null; }",
       ["            if (modifications == expected || true) { insertNew(key, value, hash); return null; }"])], ""),
    ("SM-1", "S2 remove(k,v) looks again after the value's equals changed the structure",
     [(SM, 114, "            if (revision() == expected) { if (same) { eraseAt(s); } return same; }",
       ["            if (revision() == expected || true) { if (same) { eraseAt(s); } return same; }"])], ""),
    ("SM-2", "S2 replace(k,old,new) looks again after the value's equals changed the structure",
     [(SM, 140, "            if (revision() == expected) { if (same) { writeAt(s, value); accessed(s); } return same; }",
       ["            if (revision() == expected || true) { if (same) { writeAt(s, value); accessed(s); } return same; }"])],
     ""),
    ("SM-3", "S1 putIfAbsent inserts an absent key without a second lookup",
     [(SM, 125, "        if (s < 0) { insertAbsent(key, value, (int) (located >>> 32), revision()); return null; }",
       ["        if (s < 0) { return put(key, value); }"])], ""),
    ("SM-4", "S1 computeIfAbsent inserts an absent key without a second lookup",
     [(SM, 150, "        if (s >= 0) { writeAt(s, v); accessed(s); } else { " + ABSENT,
       ["        if (s >= 0) { writeAt(s, v); accessed(s); } else { put(key, v); }"])], ""),
    ("SM-5", "S1 compute writes a present key through its slot",
     [(SM, 165, "        else if (s >= 0) { writeAt(s, v); accessed(s); }", ["        else if (s >= 0) { put(key, v); }"])],
     ""),
    ("SM-6", "S1 compute inserts an absent key without a second lookup",
     [(SM, 166, "        else { " + ABSENT, ["        else { put(key, v); }"])], ""),
    ("SM-7", "S1 merge writes a present key through its slot",
     [(SM, 175, "        else if (s >= 0) { writeAt(s, v); accessed(s); }", ["        else if (s >= 0) { put(key, v); }"])],
     ""),
    ("SM-8", "S1 merge inserts an absent key without a second lookup",
     [(SM, 176, "        else { " + ABSENT, ["        else { put(key, v); }"])], ""),
    ("SM-9", "not stricter than the JDK: a disturbed walk is repeated, not refused",
     [(SM, 62, "    static final int REWALKS = 8;", ["    static final int REWALKS = 0;"])], ""),
    ("HM-2", "insertAbsent precondition (M3HashMap)",
     [(HM, 166, "        if (modifications != expected) throw new java.util.ConcurrentModificationException();", [])],
     "BACKSTOP"),
    ("LH-2", "insertAbsent precondition (M3LinkedHashMap)",
     [(LH, 78, "        if (table.modifications != expected) { throw new java.util.ConcurrentModificationException(); }",
       [])], "BACKSTOP"),
    ("WH-3", "insertAbsent precondition (M3WeakHashMap)",
     [(WH, 99, "        if (modifications != expected) { throw new java.util.ConcurrentModificationException(); }", [])],
     "BACKSTOP"),
)
ORACLES = ("jdk.m3.collections.M3HashReentrantLookupSelfTest", "jdk.m3.collections.M3TreeMapReentrantLookupSelfTest",
           "jdk.m3.collections.M3FilteredCursorReadinessSelfTest")


def run(command, env=None):
    return subprocess.run(command, capture_output=True, text=True, env=env)


def mutate(name, edits):
    lines = (SRC / name).read_text(encoding="utf-8").split("\n")
    for line, expected, _ in edits:
        if lines[line - 1] != expected:
            raise SystemExit("pinned line %d of %s drifted: %r" % (line, name, lines[line - 1]))
    for line, _, replacement in sorted(edits, key=lambda edit: edit[0], reverse=True):
        lines[line - 1:line] = replacement
    return "\n".join(lines)


def first_failure(result):
    text = result.stdout + result.stderr
    line = next((ln for ln in text.splitlines() if "Error" in ln or "Exception in" in ln), "")
    return "FAIL(" + line.replace("\t", " ")[:140] + ")" if result.returncode else "PASS"


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    for flag in ("--java-home", "--classes", "--test-classes", "--arrays-jar", "--native-lib", "--work"):
        p.add_argument(flag, required=True)
    p.add_argument("--jvm-opt", action="append", default=[])
    p.add_argument("--path-prepend", default="", help="directory prepended to PATH (the JNI library's runtime)")
    a = p.parse_args(argv)
    for name, digest in HEAD_SHA256.items():
        if hashlib.sha256((SRC / name).read_bytes()).hexdigest() != digest:
            raise SystemExit("owner %s is not the recipe postimage" % name)
    sep = ";" if os.name == "nt" else ":"
    env = dict(os.environ)
    if a.path_prepend:
        env["PATH"] = a.path_prepend + os.pathsep + env.get("PATH", "")
    java, javac = str(Path(a.java_home) / "bin/java"), str(Path(a.java_home) / "bin/javac")
    opts = a.jvm_opt + ["-Dm3.arrays.native.path=" + a.native_lib]
    rows = ["id\tfiles\tcheck\thash_shapes\ttree_shapes\tp2098\tverdict"]
    killed = survived_unexpected = 0
    for mid, check, edits, note in MUTANTS:
        files = {}
        for name, line, expected, replacement in edits:
            files.setdefault(name, []).append((line, expected, replacement))
        work = Path(a.work) / mid
        shutil.rmtree(work, ignore_errors=True)
        (work / "src").mkdir(parents=True)
        (work / "classes").mkdir()
        for name, file_edits in files.items():
            (work / "src" / name).write_text(mutate(name, file_edits), encoding="utf-8", newline="\n")
        compiled = run([javac] + ["-J" + o for o in a.jvm_opt] + ["--release", "21", "-nowarn", "-proc:none",
                       "-encoding", "UTF-8", "-cp", a.classes + sep + a.arrays_jar, "-d", str(work / "classes")]
                       + [str(work / "src" / name) for name in files])
        if compiled.returncode != 0:
            raise SystemExit("mutant %s does not compile:\n%s" % (mid, compiled.stderr))
        cp = sep.join([str(work / "classes"), a.test_classes, a.classes, a.arrays_jar])
        results = [first_failure(run([java] + opts + ["-cp", cp, oracle], env)) for oracle in ORACLES]
        noticed = any(result != "PASS" for result in results)
        verdict = "KILLED" if noticed else ("SURVIVED(BACKSTOP)" if note == "BACKSTOP" else "SURVIVED")
        killed += noticed
        survived_unexpected += verdict == "SURVIVED"
        rows.append("\t".join([mid, ",".join(files), check] + results + [verdict]))
        print("%s %s %s" % (mid, verdict, " ".join(result[:60] for result in results)))
    (HERE / "MUTANTS.tsv").write_text("\n".join(rows) + "\n", encoding="utf-8", newline="\n")
    print("MUTANTS total=%d killed=%d survived=%d (unexpected %d)" % (len(MUTANTS), killed, len(MUTANTS) - killed,
                                                                    survived_unexpected))
    return 1 if survived_unexpected else 0


if __name__ == "__main__":
    sys.exit(main())
