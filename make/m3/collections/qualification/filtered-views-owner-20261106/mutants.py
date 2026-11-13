#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Mutants of the adopted checks of recipe FILTERED_VIEWS_OWNER v1 (MUTANTS.tsv).

Each mutant edits pinned lines of one head owner (the line must read exactly as listed, and the owner must equal
the apply.py postimage), compiles that owner alone over the head classes, and is KILLED when the relocated P2-098
self test (all modes) fails, or the seeded differential (FilteredViewsOwnerDifferential, same scenarios as
differential.sh) or the B4 repro (FilteredViewsTreeMapRepro) prints anything different from the head run. SURVIVED
rows are reported, never hidden.
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
RO, MU, TM = "M3FilteredViews.java", "M3MutableFilteredViews.java", "M3TreeMap.java"
HEAD_SHA256 = {
    RO: "354738be38ab3398669f887175baf780b6003add592af2fbdf5f78d017d9e224",
    MU: "a32c5525857bebb87d3374d4e2d6ada0f62476fc8d32716ef9ffe73d9b57efae",
    TM: "84a2addedf1644eb6f99d5f490a5c106fafa65bb60dd1c6b5162c8c1899ed4e4",
}
CHECK = "                owner.check(expected);"
# id, file, behaviour/check, [(line, expected text, replacement lines)]
MUTANTS = (
    ("RO-1", RO, "B1/B2 read-only hasNext check before the step", [(156, CHECK, [])]),
    ("RO-2", RO, "B1/B2 read-only hasNext check after the step", [(158, CHECK, [])]),
    ("RO-3", RO, "B1 read-only next check before the step", [(162, CHECK, [])]),
    ("RO-4", RO, "B2 read-only next check after the step (entry copy comparator)", [(164, CHECK, [])]),
    ("RO-5", RO, "B2 read-only epoch before the collection cursor opens",
     [(236, "                int expected = revisionOf(owner);",
       ["                Iterator<E> opened = filtered(source.iterator(), predicate);"]),
      (237, "                return ownerChecked(owner, expected, filtered(source.iterator(), predicate));",
       ["                return ownerChecked(owner, revisionOf(owner), opened);"])]),
    ("RO-6", RO, "B2 read-only epoch before the descending cursor opens",
     [(523, "            int expected = revisionOf(owner);",
       ["            Iterator<E> opened = filtered(nav.descendingSet().iterator(), predicate);"]),
      (524, "            return ownerChecked(owner, expected, filtered(nav.descendingSet().iterator(), predicate));",
       ["            return ownerChecked(owner, revisionOf(owner), opened);"])]),
    ("RO-7", RO, "B2 read-only epoch before the entry cursor opens",
     [(632, "                    int expected = revisionOf(owner);", []),
      (638, "                    return ownerChecked(owner, expected, new Iterator<Entry<K, V>>() {",
       ["                    return ownerChecked(owner, revisionOf(owner), new Iterator<Entry<K, V>>() {"])]),
    ("RO-8", RO, "B1/B2 read-only M3TreeSet owner resolution",
     [(141, "        return source instanceof M3TreeSet<?> set ? set.indexedOwner() : null;",
       ["        return null;"])]),
    ("MU-1", MU, "B1 mutable hasNext entry check", [(121, "            check();", [])]),
    ("MU-2", MU, "B2 mutable check after the predicate", [(125, "                check();", [])]),
    ("MU-3", MU, "B2 mutable check after a rejected slot step", [(131, "                check();", [])]),
    ("MU-4", MU, "B2 mutable next check after the slot step (bounded comparator)", [(140, "            check();", [])]),
    ("MU-5", MU, "B1 mutable remove checks the revision before next-required", [(148, "            check();", [])]),
    ("MU-6", MU, "B1 mutable remove re-reads the epoch after its own slot erase",
     [(151, "            expected = slots.revision();", [])]),
    ("MU-7", MU, "B2 mutable epoch before the first slot",
     [(107, "            expected = slots.revision();",
       ["            pending = fromLast ? slots.lastSlot() : slots.firstSlot();"]),
      (108, "            pending = fromLast ? slots.lastSlot() : slots.firstSlot();",
       ["            expected = slots.revision();"])]),
    ("MU-9", MU, "B3 values() over the slot cursor",
     [(657, "            if (owner == null) return super.values();",
       ["            if (owner == null || owner != null) return super.values();"])]),
    ("MU-10", MU, "B3 own remove keeps the prepared successor",
     [(152, "            last = -1;", ["            last = -1;", "            prepared = false;"])]),
    ("MU-11", MU, "B1/B2 mutable set slot cursor",
     [(244, "            M3TreeMap<E, ?> owner = source instanceof M3TreeSet<E> m3 ? m3.indexedOwner() : null;",
       ["            M3TreeMap<E, ?> owner = null;"])]),
    ("MU-12", MU, "B1/B2/B3 mutable map slot cursors",
     [(631, "            return source instanceof M3TreeMap<K, V> m3 ? m3 : null;", ["            return null;"])]),
    ("TM-1", TM, "B4b eraseAt refuses a released slot",
     [(152, "        if (slot >= tree.used || tree.height[slot] == 0) { throw new java.util.ConcurrentModificationException(); }",
       [])]),
)


def run(command, env=None):
    return subprocess.run(command, capture_output=True, text=True, env=env)


def mutate(name, edits):
    lines = (SRC / name).read_text(encoding="utf-8").split("\n")
    for line, expected, _ in edits:
        if lines[line - 1] != expected:
            raise SystemExit("pinned line %d of %s drifted: %r" % (line, name, lines[line - 1]))
    for line, _, replacement in sorted(edits, reverse=True):
        lines[line - 1:line] = replacement
    return "\n".join(lines)


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
            raise SystemExit("owner %s is not the apply.py postimage" % name)
    sep = ";" if os.name == "nt" else ":"
    java, javac = str(Path(a.java_home) / "bin/java"), str(Path(a.java_home) / "bin/javac")
    head = Path(a.head_diff).read_text(encoding="utf-8")
    head_repro = Path(a.head_repro).read_text(encoding="utf-8")
    rows = ["id\tfile\tcheck\tselftest\tdifferential\trepro\tverdict"]
    killed = 0
    for mid, name, check, edits in MUTANTS:
        work = Path(a.work) / mid
        shutil.rmtree(work, ignore_errors=True)
        (work / "src").mkdir(parents=True)
        (work / "classes").mkdir()
        (work / "src" / name).write_text(mutate(name, edits), encoding="utf-8", newline="\n")
        compiled = run([javac] + ["-J" + o for o in a.jvm_opt] + ["--release", "21", "-nowarn", "-proc:none",
                       "-encoding", "UTF-8", "-cp", a.classes + sep + a.arrays_jar, "-d", str(work / "classes"),
                       str(work / "src" / name)])
        if compiled.returncode != 0:
            raise SystemExit("mutant %s does not compile:\n%s" % (mid, compiled.stderr))
        cp = sep.join([str(work / "classes"), a.oracle_classes, a.test_classes, a.classes, a.arrays_jar])
        self_test = run([java] + a.jvm_opt + ["-cp", cp, "jdk.m3.collections.M3FilteredCursorReadinessSelfTest"])
        first = next((ln for ln in (self_test.stdout + self_test.stderr).splitlines()
                      if "Error" in ln or "Exception in" in ln), "")
        st = "FAIL(" + first.replace("\t", " ")[:140] + ")" if self_test.returncode else "PASS"
        diff = run([java] + a.jvm_opt + ["-cp", cp, "jdk.m3.collections.FilteredViewsOwnerDifferential", a.scenarios,
                                         a.seed])
        out = diff.stdout.replace("\r\n", "\n")
        if diff.returncode:
            df = "FAIL(exit %d)" % diff.returncode
        elif out == head:
            df = "SAME"
        else:
            changed = sum(1 for x, y in zip(out.split("\n"), head.split("\n")) if x != y)
            df = "DIFFERS(%d+ lines)" % max(changed, 1)
        repro = run([java] + a.jvm_opt + ["-cp", cp, "jdk.m3.collections.FilteredViewsTreeMapRepro"])
        rp = "SAME" if repro.stdout.replace("\r\n", "\n") == head_repro else "DIFFERS"
        verdict = "KILLED" if st != "PASS" or df != "SAME" or rp != "SAME" else "SURVIVED"
        killed += verdict == "KILLED"
        rows.append("\t".join((mid, name, check, st, df, rp, verdict)))
        print("%s %s selftest=%s differential=%s repro=%s" % (mid, verdict, st[:60], df, rp))
    (HERE / "MUTANTS.tsv").write_text("\n".join(rows) + "\n", encoding="utf-8", newline="\n")
    print("MUTANTS total=%d killed=%d survived=%d" % (len(MUTANTS), killed, len(MUTANTS) - killed))
    return 0 if killed == len(MUTANTS) else 1


if __name__ == "__main__":
    sys.exit(main())
