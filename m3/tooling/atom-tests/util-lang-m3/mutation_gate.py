# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""mutation_gate.py <out_dir> : discriminator proof for the generated per-atom tests (CPU, deterministic).

For every row of MUTANTS.tsv (id, source path under the staged java.base tree, exact original text, mutated
text, test class, test method) the gate copies that one staged source, applies the single textual mutation,
compiles only that file (-implicit:none) into <out_dir>/mutants/<id>, and runs the named test with the
mutant patched in front of the unmutated classes. A mutant is KILLED when the named test fails; SURVIVED
when it passes (a weak test); INVALID when the original text is absent or the mutant does not compile.
Requires run_atom_tests.sh to have staged and compiled <out_dir>. Writes MUTATION_GATE.tsv next to this file."""
import os
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
JDK = os.environ.get("JDK", "C:/Program Files/Java/jdk-21")
JUNIT = os.environ.get("JUNIT", os.path.expanduser(
    "~/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.11.4/"
    "junit-platform-console-standalone-1.11.4.jar"))
OPENS = ["java.lang", "java.util", "java.util.concurrent", "jdk.internal.m3", "jdk.internal.util"]


def run(cmd, cwd=None):
    return subprocess.run(cmd, cwd=cwd, capture_output=True, text=True, encoding="utf-8", errors="replace")


def main():
    out = sys.argv[1]
    stage, classes, tests = (os.path.join(out, d) for d in ("stage", "classes", "testclasses"))
    rows = [l.rstrip("\n").split("\t") for l in open(os.path.join(HERE, "MUTANTS.tsv"), encoding="utf-8")][1:]
    results = []
    for mid, path, original, mutated, test_class, test_method in rows:
        work = os.path.join(out, "mutants", mid)
        shutil.rmtree(work, ignore_errors=True)
        src_root = os.path.join(work, "src")
        target = os.path.join(src_root, path)
        os.makedirs(os.path.dirname(target))
        text = open(os.path.join(stage, path), encoding="utf-8").read()
        if text.count(original) != 1:
            results.append((mid, path, test_class, test_method, "INVALID", "original text count != 1"))
            continue
        with open(target, "w", encoding="utf-8", newline="\n") as w:
            w.write(text.replace(original, mutated))
        for others in (classes, stage):  # compiled siblings first; staged M3 core API sources as fallback
            compiled = run([JDK + "/bin/javac", "-d", os.path.join(work, "classes"), "--patch-module",
                            "java.base=" + src_root + os.pathsep + others, "-implicit:none",
                            "-XDignore.symbol.file", "-nowarn", target])
            if compiled.returncode == 0:
                break
        if compiled.returncode != 0:
            results.append((mid, path, test_class, test_method, "INVALID", "mutant does not compile"))
            continue
        opens = [arg for p in OPENS for arg in ("--add-opens", f"java.base/{p}=ALL-UNNAMED")]
        verdict = run([JDK + "/bin/java", "-ea", "-esa", "--patch-module",
                       "java.base=" + os.path.join(work, "classes") + os.pathsep + classes] + opens
                      + ["-jar", JUNIT, "execute", "--class-path", tests, "--select-method",
                         f"{test_class}#{test_method}", "--disable-banner", "--details=summary",
                         "--disable-ansi-colors"])
        failed = "tests failed" in verdict.stdout and " 0 tests failed" not in verdict.stdout
        ran = " 1 tests found" in verdict.stdout
        status = "KILLED" if failed else ("SURVIVED" if ran else "INVALID")
        results.append((mid, path, test_class, test_method, status, "exit=" + str(verdict.returncode)))
        print(mid, status, flush=True)
    with open(os.path.join(HERE, "MUTATION_GATE.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("mutant\tsource\ttest_class\ttest_method\tstatus\tdetail\n")
        w.writelines("\t".join(r) + "\n" for r in results)
    killed = sum(r[4] == "KILLED" for r in results)
    print(f"mutants={len(results)} killed={killed} survived={sum(r[4] == 'SURVIVED' for r in results)} "
          f"invalid={sum(r[4] == 'INVALID' for r in results)}")
    sys.exit(0 if killed == len(results) else 1)


main()
