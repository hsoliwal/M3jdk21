#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Gate 7 of recipe/corpus-search-20261108: one-line mutants of every relocated computing class, all killed.

Each mutant replaces one unique line fragment of a relocated source (never in place: the mutated source is written to
<scratch>/<id>/src and compiled with javac --release 21 against target/classes into <scratch>/<id>/classes, which
shadows the module class on the run classpath). A mutant is killed by either oracle: (1) the donor-anchored dump of
M3CorpusSearchDump differs from the relocated dump digest that differential.sh proved equal to the donor's, or
(2) the JUnit run of the ported donor tests and M3CorpusSearchReferenceTest fails (JUnit Platform console
launcher). Writes MUTANTS.tsv (one row per mutant with both verdicts) and prints MUTANTS PASS|FAIL killed=K/N.
usage: python -I mutants.py <scratch-dir> --dump-sha256 SHA --console JAR --dep-cp CP [--java-home DIR] [--tmp DIR]
"""
from __future__ import annotations

import argparse
import hashlib
import os
import subprocess
import sys
from pathlib import Path

RECIPE_DIR = Path(__file__).resolve().parent
MODULE = RECIPE_DIR.parent.parent
SOURCES = MODULE / "src" / "main" / "java" / "jdk" / "m3" / "algorithm"
SEED, CASES = "20261108", "4000"
TESTS = ("M3CorpusSearchReferenceTest", "M3MultiPatternSearchTest", "M3PrefixIndexTest")
# id, class, unique old fragment, new fragment, what it breaks
MUTANTS = (
    ("A1", "M3MultiPatternSearch", "int fail = transition == null || transition == child ? 0 : transition;",
     "int fail = 0;", "every failure link falls to the root: overlapping patterns (hers after she) are missed"),
    ("A2", "M3MultiPatternSearch", "Math.addExact(prefixSubtreeCount[0], emptyPatternCount);",
     "prefixSubtreeCount[0];", "empty patterns leave the empty-prefix multiplicity"),
    ("A3", "M3MultiPatternSearch", "if (candidate < label) low = middle + 1;", "if (candidate <= label) low = middle + 1;",
     "edge binary search steps past an equal label: no transition is ever found"),
    ("A4", "M3MultiPatternSearch", "int offset = position + 1 - prepared.patterns[pattern].length;",
     "int offset = position - prepared.patterns[pattern].length;", "every emitted offset is one byte early"),
    ("A5", "M3MultiPatternSearch", "return query.length == 0 ? stable.emptyPatternCount : stable.outputCount[state];",
     "return stable.outputCount[state];", "the empty pattern has no exact multiplicity"),
    ("X1", "M3PrefixIndex", "return exactCount(value) != 0;", "return prefixCount(value) != 0;",
     "contains answers startsWith"),
)


def run(command: list, timeout: int = 300) -> subprocess.CompletedProcess:
    try:
        return subprocess.run(command, capture_output=True, timeout=timeout)
    except subprocess.TimeoutExpired:
        return subprocess.CompletedProcess(command, 124, b"", b"TIMEOUT")


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("scratch")
    parser.add_argument("--dump-sha256", required=True)
    parser.add_argument("--console", required=True, help="junit-platform-console-standalone jar")
    parser.add_argument("--dep-cp", required=True, help="the module's dependency jars (m3-collections)")
    parser.add_argument("--java-home", default=os.environ.get("JAVA_HOME", ""))
    parser.add_argument("--tmp", default="", help="java.io.tmpdir for every JVM")
    args = parser.parse_args(argv)
    java = str(Path(args.java_home) / "bin" / "java")
    javac = str(Path(args.java_home) / "bin" / "javac")
    tmp = ["-Djava.io.tmpdir=" + args.tmp] if args.tmp else []
    sep = os.pathsep
    scratch = Path(args.scratch)
    rows, killed = [], 0
    for mutant_id, owner, old, new, effect in MUTANTS:
        source = (SOURCES / (owner + ".java")).read_text(encoding="utf-8")
        if source.count(old) != 1:
            print("REFUSED %s: fragment occurs %d times in %s" % (mutant_id, source.count(old), owner))
            return 2
        work = scratch / mutant_id
        (work / "src").mkdir(parents=True, exist_ok=True)
        (work / "classes").mkdir(parents=True, exist_ok=True)
        mutated = work / "src" / (owner + ".java")
        mutated.write_text(source.replace(old, new), encoding="utf-8", newline="")
        cp_compile = str(MODULE / "target" / "classes") + sep + args.dep_cp
        compiled = run([javac] + ["-J" + flag for flag in tmp] + ["--release", "21", "-g", "-cp", cp_compile, "-d",
                                                                  str(work / "classes"), str(mutated)])
        if compiled.returncode != 0:
            print("REFUSED %s: mutant does not compile\n%s" % (mutant_id, compiled.stderr.decode(errors="replace")))
            return 2
        cp = sep.join([str(work / "classes"), str(MODULE / "target" / "classes"),
                       str(MODULE / "target" / "test-classes"), args.dep_cp])
        dump = run([java, "-Xmx256m"] + tmp + ["-cp", cp, "jdk.m3.algorithm.M3CorpusSearchDump", "--seed", SEED,
                                              "--cases", CASES])
        digest = hashlib.sha256(dump.stdout).hexdigest()
        dump_kill = dump.returncode != 0 or digest != args.dump_sha256
        selectors = [part for name in TESTS for part in ("--select-class", "jdk.m3.algorithm." + name)]
        junit = run([java, "-Xmx384m"] + tmp + ["-jar", args.console, "execute", "--disable-banner",
                                               "--details=summary", "--class-path", cp] + selectors, timeout=600)
        junit_kill = junit.returncode != 0
        failed = [line.strip() for line in junit.stdout.decode(errors="replace").splitlines()
                  if "tests failed" in line]
        state = "KILLED" if dump_kill or junit_kill else "SURVIVED"
        killed += state == "KILLED"
        rows.append((mutant_id, owner, effect, "KILL" if dump_kill else "survive",
                     "KILL" if junit_kill else "survive", (failed[0] if failed else "-"), state))
        print("MUTANT %s %s %s dump=%s junit=%s %s" % (mutant_id, owner, state, rows[-1][3], rows[-1][4], rows[-1][5]))
    table = ["# SPDX-License-Identifier: Apache-2.0",
             "# One-line mutants of recipe/corpus-search-20261108/mutants.py; dump = donor-anchored M3CorpusSearchDump"
             " digest (seed %s, %s cases) vs %s; junit = ported donor tests + M3CorpusSearchReferenceTest."
             % (SEED, CASES, args.dump_sha256[:16]),
             "id\tclass\teffect\tdump\tjunit\tjunit_summary\tverdict"]
    table += ["\t".join(row) for row in rows]
    (RECIPE_DIR / "MUTANTS.tsv").write_text("\n".join(table) + "\n", encoding="utf-8", newline="\n")
    status = "PASS" if killed == len(MUTANTS) else "FAIL"
    print("MUTANTS %s killed=%d/%d classes=%d" % (status, killed, len(MUTANTS), len({m[1] for m in MUTANTS})))
    return 0 if status == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
