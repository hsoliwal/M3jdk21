#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Mutants of the guards of recipe TREEMAP_ABA_GUARD v1 and of FILTERED_VIEWS_OWNER v1 (#2641) (MUTANTS.tsv).

Each mutant edits pinned lines of one head owner (the line must read exactly as listed, and the owners must equal the
recipe postimages), compiles that owner alone over the head classes, and is KILLED when any oracle notices:
M3TreeMapReentrantLookupSelfTest (all), M3FilteredCursorReadinessSelfTest (all modes), the seeded differential
(FilteredViewsOwnerDifferential, same scenarios as differential.sh) or the B4 repro printing anything different
from the head run. SURVIVED rows are reported, never hidden: BACKSTOP marks a check kept deliberately redundant (the
review asked to keep eraseAt's height check behind the lookup guards); COVERED marks a check whose pair mutant (the
check removed together with the guards claimed to cover it, PAIRS) is KILLED, so it is redundant, not dead.
usage: python -I mutants.py --java-home DIR --classes DIR --test-classes DIR --arrays-jar JAR --oracle-classes DIR
                            --head-diff FILE --head-repro FILE --work DIR [--scenarios N] [--seed S] [--jvm-opt OPT]
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
RO, MU, TM, SM = "M3FilteredViews.java", "M3MutableFilteredViews.java", "M3TreeMap.java", "M3SlotMap.java"
HEAD_SHA256 = {
    RO: "354738be38ab3398669f887175baf780b6003add592af2fbdf5f78d017d9e224",
    MU: "0f2ae498c1a43d1cd6caf8b181e10756ea7c2bce4b0ecde65e76f75e53d58050",
    TM: "d01665823d54fb754657da9a960aef171b47ee0b18e52c84e84242246b0b5878",
    SM: "7567a9e57c5aead305ab248a2850022a3d31269d72b17361313aae72a8be7ec0",
}
CHECK = "                owner.check(expected);"
LOCATED = "        return located(expected, slotWithin(s) ? s : -1);"
UNLOCATED = ["        return slotWithin(s) ? s : -1;"]
WALK = "                int c = compare(key, M3SlotArrays.get(keys, s)); walked(expected);"
UNWALKED = ["                int c = compare(key, M3SlotArrays.get(keys, s));"]
DESCENT = "            cmp = tree.compare(key, keyAt(s)); check(expected);"
UNCHECKED_DESCENT = ["            cmp = tree.compare(key, keyAt(s));"]
# id, file, behaviour/check, [(line, expected text, replacement lines)], note
MUTANTS = (
    ("AG-1", TM, "ABA Tree.find walk check (findSlot, entrySlot)", [(476, WALK, UNWALKED)], ""),
    ("AG-2", TM, "ABA Tree.near walk check (neighbor and bounded endpoint lookups)", [(495, WALK, UNWALKED)], ""),
    ("AG-3", TM, "ABA firstSlot located() after the bound probe",
     [(112, "    @Override int firstSlot() { return located(revision(), descending ? highest() : lowest()); }",
       ["    @Override int firstSlot() { return descending ? highest() : lowest(); }"])], ""),
    ("AG-4", TM, "ABA lastSlot located() after the bound probe",
     [(113, "    @Override int lastSlot() { return located(revision(), descending ? lowest() : highest()); }",
       ["    @Override int lastSlot() { return descending ? lowest() : highest(); }"])], ""),
    ("AG-5", TM, "ABA nextSlot located() after the bound probe", [(131, LOCATED, UNLOCATED)], ""),
    ("AG-6", TM, "ABA previousIndexedSlot located() after the bound probe", [(136, LOCATED, UNLOCATED)], ""),
    ("AG-7", TM, "ABA near check after the high-bound probe",
     [(350, "            int c = tree.compare(keyAt(s), high); check(expected);",
       ["            int c = tree.compare(keyAt(s), high);"])], ""),
    ("AG-8", TM, "ABA near check after the low-bound probe",
     [(353, "            int c = tree.compare(keyAt(s), low); check(expected);",
       ["            int c = tree.compare(keyAt(s), low);"])], ""),
    ("AG-9", TM, "ABA near located() after the final slotWithin", [(356, LOCATED, UNLOCATED)], ""),
    ("AG-10", TM, "ABA put descent check after every comparator call", [(210, DESCENT, UNCHECKED_DESCENT)], ""),
    ("AG-11", TM, "ABA put reads the revision before its admission probes (compute/merge write half)",
     [(201, "        int expected = revision(); // before the admission and descent comparator calls", []),
      (208, "        int p = -1, s = tree.root, cmp = 0;",
       ["        int expected = revision(), p = -1, s = tree.root, cmp = 0;"])], ""),
    ("AG-12", TM, "ABA defer descent check after every comparator call", [(261, DESCENT, UNCHECKED_DESCENT)], ""),
    ("AG-13", TM, "ABA cursorStart turns a structural change during creation into -1 (creation never fails)",
     [(124, "            if (revision() == expected) { throw changed; }", ["            if (changed != null) { throw changed; }"])],
     ""),
    ("AG-14", TM, "ABA cursorStart rethrows the comparator's own ConcurrentModificationException",
     [(124, "            if (revision() == expected) { throw changed; }", [])], ""),
    ("AG-15", SM, "ABA M3SlotMap.slots reads the revision before its first slot",
     [(216, "            private int expected = revision(); // before the first slot: its lookup may call the comparator",
       ["            private int cursor = firstCursorSlot();"]),
      (217, "            private int cursor = firstCursorSlot();", ["            private int expected = revision();"])],
     ""),
    ("AG-16", MU, "ABA the #2641 mutable slot cursor starts through cursorStart",
     [(108, "            pending = slots.cursorStart(fromLast); // TREEMAP_ABA_GUARD: creation never fails",
       ["            pending = fromLast ? slots.lastSlot() : slots.firstSlot();"])], ""),
    ("TM-1", TM, "B4b eraseAt refuses a released slot",
     [(180, "        if (slot >= tree.used || tree.height[slot] == 0) { throw new java.util.ConcurrentModificationException(); }",
       [])], "BACKSTOP"),
    ("RO-1", RO, "B1/B2 read-only hasNext check before the step", [(156, CHECK, [])], ""),
    ("RO-2", RO, "B1/B2 read-only hasNext check after the step", [(158, CHECK, [])], ""),
    ("RO-3", RO, "B1 read-only next check before the step", [(162, CHECK, [])], ""),
    ("RO-4", RO, "B2 read-only next check after the step (entry copy comparator)", [(164, CHECK, [])], ""),
    ("RO-5", RO, "B2 read-only epoch before the collection cursor opens",
     [(236, "                int expected = revisionOf(owner);",
       ["                Iterator<E> opened = filtered(source.iterator(), predicate);"]),
      (237, "                return ownerChecked(owner, expected, filtered(source.iterator(), predicate));",
       ["                return ownerChecked(owner, revisionOf(owner), opened);"])], ""),
    ("RO-6", RO, "B2 read-only epoch before the descending cursor opens",
     [(523, "            int expected = revisionOf(owner);",
       ["            Iterator<E> opened = filtered(nav.descendingSet().iterator(), predicate);"]),
      (524, "            return ownerChecked(owner, expected, filtered(nav.descendingSet().iterator(), predicate));",
       ["            return ownerChecked(owner, revisionOf(owner), opened);"])], ""),
    ("RO-7", RO, "B2 read-only epoch before the entry cursor opens",
     [(632, "                    int expected = revisionOf(owner);", []),
      (638, "                    return ownerChecked(owner, expected, new Iterator<Entry<K, V>>() {",
       ["                    return ownerChecked(owner, revisionOf(owner), new Iterator<Entry<K, V>>() {"])], ""),
    ("RO-8", RO, "B1/B2 read-only M3TreeSet owner resolution",
     [(141, "        return source instanceof M3TreeSet<?> set ? set.indexedOwner() : null;",
       ["        return null;"])], ""),
    ("MU-1", MU, "B1 mutable hasNext entry check", [(121, "            check();", [])], ""),
    ("MU-2", MU, "B2 mutable check after the predicate", [(125, "                check();", [])], ""),
    ("MU-3", MU, "B2 mutable check after a rejected slot step", [(131, "                check();", [])], ""),
    ("MU-4", MU, "B2 mutable next check after the slot step (bounded comparator)", [(140, "            check();", [])],
     ""),
    ("MU-5", MU, "B1 mutable remove checks the revision before next-required", [(148, "            check();", [])], ""),
    ("MU-6", MU, "B1 mutable remove re-reads the epoch after its own slot erase",
     [(151, "            expected = slots.revision();", [])], ""),
    ("MU-7", MU, "B2 mutable epoch before the first slot",
     [(107, "            expected = slots.revision();",
       ["            pending = slots.cursorStart(fromLast); // TREEMAP_ABA_GUARD: creation never fails"]),
      (108, "            pending = slots.cursorStart(fromLast); // TREEMAP_ABA_GUARD: creation never fails",
       ["            expected = slots.revision();"])], ""),
    ("MU-9", MU, "B3 values() over the slot cursor",
     [(657, "            if (owner == null) return super.values();",
       ["            if (owner == null || owner != null) return super.values();"])], ""),
    ("MU-10", MU, "B3 own remove keeps the prepared successor",
     [(152, "            last = -1;", ["            last = -1;", "            prepared = false;"])], ""),
    ("MU-11", MU, "B1/B2 mutable set slot cursor",
     [(244, "            M3TreeMap<E, ?> owner = source instanceof M3TreeSet<E> m3 ? m3.indexedOwner() : null;",
       ["            M3TreeMap<E, ?> owner = null;"])], ""),
    ("MU-12", MU, "B1/B2/B3 mutable map slot cursors",
     [(631, "            return source instanceof M3TreeMap<K, V> m3 ? m3 : null;", ["            return null;"])], ""),
)


# A pair mutates a surviving single together with the guards claimed to cover it: the claim holds when the pair is
# KILLED (the single's check is redundant behind those guards, not dead).
PAIRS = (
    ("P-AG-6", ("AG-6", "MU-3", "MU-4"), "previousIndexedSlot unguarded and the mutable cursor's checks after a step"
     " removed"),
    ("P-MU-3", ("MU-3", "AG-5", "AG-6"), "rejected-step check removed with nextSlot/previousIndexedSlot unguarded"),
    ("P-MU-4", ("MU-4", "AG-5", "AG-6"), "next check after the step removed with nextSlot/previousIndexedSlot unguarded"),
    ("P-RO-1", ("RO-1", "AG-15"), "read-only hasNext pre-check removed with M3SlotMap.slots' late epoch restored"),
    ("P-RO-5", ("RO-5", "AG-15"), "read-only collection epoch late with M3SlotMap.slots' late epoch restored"),
    ("P-RO-6", ("RO-6", "AG-15"), "read-only descending epoch late with M3SlotMap.slots' late epoch restored"),
    ("P-RO-7", ("RO-7", "AG-15"), "read-only entry epoch late with M3SlotMap.slots' late epoch restored"),
)
COVERED = {"AG-6": "P-AG-6", "MU-3": "P-MU-3", "MU-4": "P-MU-4", "RO-1": "P-RO-1", "RO-5": "P-RO-5", "RO-6": "P-RO-6",
           "RO-7": "P-RO-7"}
SINGLES = {mutant[0]: mutant for mutant in MUTANTS}
MEMBERS = {pair[0]: pair[1] for pair in PAIRS}


def edits_of(mid):
    """{file: [(line, expected, replacement)]} of a single mutant or of a pair's members."""
    if mid in SINGLES:
        return {SINGLES[mid][1]: list(SINGLES[mid][3])}
    merged = {}
    for member in MEMBERS[mid]:
        for name, edits in edits_of(member).items():
            merged.setdefault(name, []).extend(edits)
    return merged


def run(command):
    return subprocess.run(command, capture_output=True, text=True)


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
    for flag in ("--java-home", "--classes", "--test-classes", "--arrays-jar", "--oracle-classes", "--head-diff",
                 "--head-repro", "--work"):
        p.add_argument(flag, required=True)
    p.add_argument("--scenarios", default="4000")
    p.add_argument("--seed", default="20261106")
    p.add_argument("--jvm-opt", action="append", default=[])
    a = p.parse_args(argv)
    for name, digest in HEAD_SHA256.items():
        if hashlib.sha256((SRC / name).read_bytes()).hexdigest() != digest:
            raise SystemExit("owner %s is not the recipe postimage" % name)
    sep = ";" if os.name == "nt" else ":"
    java, javac = str(Path(a.java_home) / "bin/java"), str(Path(a.java_home) / "bin/javac")
    head = Path(a.head_diff).read_text(encoding="utf-8")
    head_repro = Path(a.head_repro).read_text(encoding="utf-8")
    rows = ["id\tfile\tcheck\tshapes\tselftest\tdifferential\trepro\tverdict"]
    results = {}
    for mid in [m[0] for m in MUTANTS] + [pair[0] for pair in PAIRS]:
        files = edits_of(mid)
        work = Path(a.work) / mid
        shutil.rmtree(work, ignore_errors=True)
        (work / "src").mkdir(parents=True)
        (work / "classes").mkdir()
        for name, edits in files.items():
            (work / "src" / name).write_text(mutate(name, edits), encoding="utf-8", newline="\n")
        compiled = run([javac] + ["-J" + o for o in a.jvm_opt] + ["--release", "21", "-nowarn", "-proc:none",
                       "-encoding", "UTF-8", "-cp", a.classes + sep + a.arrays_jar, "-d", str(work / "classes")]
                       + [str(work / "src" / name) for name in files])
        if compiled.returncode != 0:
            raise SystemExit("mutant %s does not compile:\n%s" % (mid, compiled.stderr))
        cp = sep.join([str(work / "classes"), a.oracle_classes, a.test_classes, a.classes, a.arrays_jar])
        shapes = first_failure(run([java] + a.jvm_opt + ["-cp", cp, "jdk.m3.collections.M3TreeMapReentrantLookupSelfTest"]))
        selftest = first_failure(run([java] + a.jvm_opt + ["-cp", cp,
                                                           "jdk.m3.collections.M3FilteredCursorReadinessSelfTest"]))
        diff = run([java] + a.jvm_opt + ["-cp", cp, "jdk.m3.collections.FilteredViewsOwnerDifferential", a.scenarios,
                                         a.seed])
        out = diff.stdout.replace("\r\n", "\n")
        if diff.returncode:
            df = "FAIL(exit %d)" % diff.returncode
        elif out == head:
            df = "SAME"
        else:
            df = "DIFFERS(%d+ lines)" % max(1, sum(1 for x, y in zip(out.split("\n"), head.split("\n")) if x != y))
        repro = run([java] + a.jvm_opt + ["-cp", cp, "jdk.m3.collections.FilteredViewsTreeMapRepro"])
        rp = "SAME" if repro.stdout.replace("\r\n", "\n") == head_repro else "DIFFERS"
        noticed = shapes != "PASS" or selftest != "PASS" or df != "SAME" or rp != "SAME"
        results[mid] = (",".join(files), shapes, selftest, df, rp, noticed)
    killed = survived_unexpected = 0
    for mid, name, check, edits, note in MUTANTS + tuple((pid, None, check, None, "PAIR") for pid, _, check in PAIRS):
        files, shapes, selftest, df, rp, noticed = results[mid]
        pair = COVERED.get(mid)
        if noticed:
            verdict = "KILLED"
        elif note == "BACKSTOP":
            verdict = "SURVIVED(BACKSTOP)"
        elif pair and results[pair][5]:
            verdict = "SURVIVED(COVERED: %s KILLED)" % pair
        else:
            verdict = "SURVIVED"
        killed += noticed
        survived_unexpected += verdict == "SURVIVED"
        rows.append("\t".join((mid, files, check, shapes, selftest, df, rp, verdict)))
        print("%s %s shapes=%s selftest=%s differential=%s repro=%s" % (mid, verdict, shapes[:50], selftest[:30], df,
                                                                        rp))
    (HERE / "MUTANTS.tsv").write_text("\n".join(rows) + "\n", encoding="utf-8", newline="\n")
    total = len(MUTANTS) + len(PAIRS)
    print("MUTANTS total=%d (singles %d, pairs %d) killed=%d survived=%d (unexpected %d)" % (
        total, len(MUTANTS), len(PAIRS), killed, total - killed, survived_unexpected))
    return 1 if survived_unexpected else 0


if __name__ == "__main__":
    sys.exit(main())
