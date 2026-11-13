# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""mutate.py <worktree> <out_dir> <MUTANTS.tsv> <MUTATION.tsv> : MUTATION gate (CPU): do the per-atom tests assert?

For every row of MUTANTS.tsv (a planted behaviour bug: one exact source substring and its replacement) it copies the
M3 sources of sun.swing.{list,table,tree} to <out>/mutants/<id>/src, applies the replacement (it must match exactly
once), compiles the mutated java.desktop patch with javac and runs the named generated test class with the JUnit
console on stock JDK 21 (the test classes compiled by run_atom_tests.sh in <out>/tests/<pkg>). A mutant is KILLED
when at least one dynamic test fails. Writes MUTATION.tsv; exit status 1 when a mutant survives. Prints progress."""
import csv
import os
import re
import shutil
import subprocess
import sys
import time

JDK = os.environ.get("JDK", r"C:/Program Files/Java/jdk-21")
JUNIT = os.environ.get("JUNIT_JAR", os.path.expanduser(
    "~/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.11.4/"
    "junit-platform-console-standalone-1.11.4.jar"))
TIMEOUT_S = 600


def run(cmd, cwd=None):
    return subprocess.run(cmd, cwd=cwd, capture_output=True, text=True, timeout=TIMEOUT_S)


def header(test_file):
    text = open(test_file, encoding="utf-8").read()
    flags = re.search(r"@run junit/othervm (.*) \w+AtomsTest", text).group(1).split()
    opens = [f"--add-opens={m[:-len(':open')]}=ALL-UNNAMED" for m in re.search(r"@modules (.*)", text).group(1).split()]
    return flags, opens


def main():
    wt, out, mutants_tsv, report = sys.argv[1:5]
    src_root = os.path.join(wt, "src", "java.desktop", "share", "classes")
    rows = list(csv.DictReader(open(mutants_tsv, encoding="utf-8"), delimiter="\t"))
    results, survived = [], 0
    for r in rows:
        started = time.time()
        base = os.path.join(out, "mutants", r["id"])
        shutil.rmtree(base, ignore_errors=True)
        src = os.path.join(base, "src")
        for pkg in ("list", "table", "tree"):
            shutil.copytree(os.path.join(src_root, "sun", "swing", pkg), os.path.join(src, "sun", "swing", pkg),
                            ignore=shutil.ignore_patterns("DefaultTableCellHeaderRenderer.java"))
        target = os.path.join(src, "sun", "swing", *r["file"].split("/"))
        text = open(target, encoding="utf-8").read()
        if text.count(r["original"]) != 1:
            raise SystemExit(f"{r['id']}: the original text occurs {text.count(r['original'])} times in {r['file']}")
        with open(target, "w", encoding="utf-8", newline="\n") as w:
            w.write(text.replace(r["original"], r["mutant"]))
        patch = os.path.join(base, "patch")
        files = [os.path.join("sun", "swing", p, f) for p in ("list", "table", "tree")
                 for f in sorted(os.listdir(os.path.join(src, "sun", "swing", p))) if f.endswith(".java")]
        c = run([os.path.join(JDK, "bin", "javac"), "-nowarn", "--patch-module", "java.desktop=.", "-d", patch] + files,
                cwd=src)
        if c.returncode != 0:
            raise SystemExit(f"{r['id']}: mutant does not compile:\n{c.stderr}")
        test = r["test"]
        pkg = r["file"].split("/")[0]
        flags, opens = header(os.path.join(wt, "test", "jdk", "sun", "swing", pkg, test + ".java"))
        j = run([os.path.join(JDK, "bin", "java"), "-Xmx768m"] + flags + ["--patch-module", "java.desktop=" + patch] + opens
                + ["-jar", JUNIT, "execute", "-cp", os.path.join(out, "tests", pkg), "--disable-banner",
                   "--details=summary", "--select-class", test])
        failed = re.search(r"(\d+) tests failed", j.stdout)
        found = re.search(r"(\d+) tests found", j.stdout)
        n_failed = int(failed.group(1)) if failed else -1
        verdict = "KILLED" if n_failed > 0 else "SURVIVED"
        survived += verdict == "SURVIVED"
        names = re.findall(r"JUnit Jupiter:\w+:atoms\(\):(\S+)", j.stdout)
        results.append((r["id"], r["file"], r["bug"], test, str(n_failed), found.group(1) if found else "?", verdict,
                        ",".join(names[:6]) or "-"))
        print(f"{r['id']} {verdict} failed={n_failed} {r['bug']} ({time.time() - started:.0f}s)", flush=True)
        shutil.rmtree(base, ignore_errors=True)
    with open(report, "w", encoding="utf-8", newline="\n") as w:
        w.write("id\tfile\tbug\ttest\ttests_failed\ttests_run\tverdict\tfailing_dynamic_tests\n")
        w.writelines("\t".join(x) + "\n" for x in results)
    print(f"mutants={len(results)} killed={len(results) - survived} survived={survived}")
    sys.exit(1 if survived else 0)


main()
