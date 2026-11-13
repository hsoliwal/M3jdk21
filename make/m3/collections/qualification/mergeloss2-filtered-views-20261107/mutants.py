#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Mutants of the restored members of qualification/mergeloss2-filtered-views-20261107 (CPU, deterministic).

Each mutant changes one restored member of the head source (one exact text replacement, refused unless it occurs
exactly once), recompiles that owner class against the head classes and runs the restored/relocated tests and the
reference model on the JUnit console launcher. KILLED = the run fails; SURVIVED = it passes. The unmutated head must
pass first. Writes MUTANTS.tsv next to this file.
usage: python -I mutants.py --java-home <JDK 21> --root <repo> --classes <head classes> --test-classes <head
       test-classes> --arrays-jar <m3-arrays jar> --console <junit-platform-console-standalone jar> --work <dir>
       [--jvm-opt <option>]...
"""
import argparse
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
SRC = "m3/collections/src/main/java/jdk/m3/collections/"
TESTS = ["M3FilteredDescendingDepthTest", "M3FilteredSortedRepresentativeTest", "M3StringTreeConsumerTest",
         "M3FilteredListProviderInvalidationTest", "M3KeyedParallelismConvergenceTest",
         "M3MergelossFilteredViewsReferenceTest"]
# id, leaf, member, owner file, old, new
MUTANTS = (
    ("M01", "42", "NavigableSetView.descendingIterator (chain peel)", "M3FilteredViews.java",
     "            if (!(nav instanceof NavigableSetView<?>)) {\n                M3TreeMap<?, ?> owner = sortedOwner(nav);",
     "            if (true) {\n                M3TreeMap<?, ?> owner = sortedOwner(nav);"),
    ("M02", "42", "NavigableSetView.descendingIterator (inner-to-outer filter order)", "M3FilteredViews.java",
     "            for (int index = filters.size() - 1; index >= 0; index--) {",
     "            for (int index = 0; index < filters.size(); index++) {"),
    ("M03", "27", "sortedMembershipOwner (sorted owner found)", "M3FilteredViews.java",
     "            if (owner instanceof SortedSet<?> sorted) return (SortedSet<E>) sorted;",
     "            if (owner instanceof SortedSet<?> sorted) return null;"),
    ("M04", "27", "sortedMembershipOwner (private Set layer unwrapped)", "M3FilteredViews.java",
     "                owner = ((SetView<E>) owner).view.source;",
     "                return null;"),
    ("M05", "27", "visibleMember (predicate on the stored representative)", "M3FilteredViews.java",
     "                return predicate.test(navigable.ceiling(alias));",
     "                return predicate.test(alias);"),
    ("M06", "43", "Cursor.checkForComodification (reverse probe)", "M3FilteredLists.java",
     "                    } catch (NoSuchElementException exhausted) {\n                        try {",
     "                    } catch (NoSuchElementException exhausted) {\n                        if (exhausted != null) return false;\n                        try {"),
    ("M07", "43", "Cursor.checkForComodification (delivered element disappeared)", "M3FilteredLists.java",
     "                                throw new ConcurrentModificationException(\"delivered element disappeared\");",
     "                                return true;"),
    ("M08", "43", "Cursor.add (empty provider insertion through the provider iterator)", "M3FilteredLists.java",
     "                if (empty) {\n", "                if (false) {\n"),
    ("M09", "17", "put (fast admission re-checks the published failure)", "M3KeyedParallelismPlan.java",
     "        if (queue.offer(value)) {\n            checkFailure(laneFailure.get());\n",
     "        if (queue.offer(value)) {\n"),
)


def run_tests(args, extra, log):
    sep = ";" if sys.platform.startswith("win") else ":"
    cp = sep.join([*extra, args.classes, args.test_classes, args.arrays_jar, args.console])
    cmd = [str(Path(args.java_home) / "bin" / "java"), *args.jvm_opt, "-cp", cp,
           "org.junit.platform.console.ConsoleLauncher", "execute", "--disable-banner", "--disable-ansi-colors",
           "--include-engine=junit-jupiter", "--details=summary"] + ["--select-class=jdk.m3.collections." + t for t in TESTS]
    result = subprocess.run(cmd, capture_output=True, text=True, errors="replace")
    log.write_text(result.stdout + result.stderr, encoding="utf-8")
    summary = [l.strip() for l in result.stdout.splitlines() if "tests successful" in l or "tests failed" in l]
    return result.returncode, " ".join(summary)


def main(argv=None):
    parser = argparse.ArgumentParser()
    for name in ("java-home", "root", "classes", "test-classes", "arrays-jar", "console", "work"):
        parser.add_argument("--" + name, required=True)
    parser.add_argument("--jvm-opt", action="append", default=[])
    args = parser.parse_args(argv)
    work = Path(args.work)
    shutil.rmtree(work, ignore_errors=True)
    work.mkdir(parents=True)
    rc, summary = run_tests(args, [], work / "head.log")
    if rc != 0:
        raise SystemExit("unmutated head fails: " + summary)
    rows = [("HEAD", "-", "unmutated head", "-", "PASS", summary)]
    for mid, leaf, member, owner, old, new in MUTANTS:
        text = (Path(args.root) / SRC / owner).read_text(encoding="utf-8")
        if text.count(old) != 1:
            raise SystemExit("%s: mutation site occurs %d times" % (mid, text.count(old)))
        src = work / mid / "src" / "jdk" / "m3" / "collections"
        out = work / mid / "classes"
        src.mkdir(parents=True)
        out.mkdir(parents=True)
        (src / owner).write_text(text.replace(old, new), encoding="utf-8")
        sep = ";" if sys.platform.startswith("win") else ":"
        javac = subprocess.run([str(Path(args.java_home) / "bin" / "javac"), *["-J" + o for o in args.jvm_opt],
                                "--release", "21", "-proc:none", "-nowarn", "-encoding", "UTF-8", "-d", str(out),
                                "-cp", sep.join([args.classes, args.arrays_jar]), str(src / owner)],
                               capture_output=True, text=True)
        if javac.returncode != 0:
            raise SystemExit("%s does not compile: %s" % (mid, javac.stderr[-400:]))
        rc, summary = run_tests(args, [str(out)], work / (mid + ".log"))
        state = "KILLED" if rc != 0 else "SURVIVED"
        rows.append((mid, leaf, member, owner, state, summary))
        print(mid, state, summary)
    with open(HERE / "MUTANTS.tsv", "w", encoding="utf-8", newline="\n") as table:
        table.write("# SPDX-License-Identifier: Apache-2.0\n# mutants.py: one mutant per restored member, run against"
                    " the relocated origin tests and the reference model (JUnit console launcher).\n")
        table.write("id\tleaf\tmember\towner\tstate\ttests\n")
        for row in rows:
            table.write("\t".join(row) + "\n")
    survived = [r for r in rows[1:] if r[4] != "KILLED"]
    print("mutants=%d killed=%d survived=%d" % (len(rows) - 1, len(rows) - 1 - len(survived), len(survived)))
    return 1 if survived else 0


if __name__ == "__main__":
    sys.exit(main())
