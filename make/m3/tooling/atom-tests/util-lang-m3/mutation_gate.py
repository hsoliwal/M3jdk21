# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""mutation_gate.py <out_dir> [result_tsv] : discriminator proof for the generated per-atom tests (CPU, deterministic).

For every row of MUTANTS.tsv (id, source path under the staged java.base tree, exact original text, mutated
text, test class, test method) the gate copies that one staged source, applies the single textual mutation,
compiles only that file (-implicit:none) into <out_dir>/mutants/<id>, and runs the named test with the
mutant patched in front of the unmutated classes. Every run uses the access the test's own jtreg header declares
(jtreg_flags.py; the test source is found through TEST_PATTERNS.tsv), and each named test must first pass on the
unmutated classes. A mutant is KILLED when the named test then fails; SURVIVED when it passes (a weak test);
INVALID when the original text is absent, the mutant does not compile, the test is not found or its baseline run
fails. Requires run_atom_tests.sh to have staged and compiled <out_dir>. Writes result_tsv (default
MUTATION_GATE.tsv next to this file)."""
import os
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.normpath(os.path.join(HERE, "..", "..", "..", ".."))
sys.path.insert(0, HERE)
import jtreg_flags  # noqa: E402

JDK = os.environ.get("JDK", "C:/Program Files/Java/jdk-21")
JUNIT = os.environ.get("JUNIT", os.path.expanduser(
    "~/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.11.4/"
    "junit-platform-console-standalone-1.11.4.jar"))


def run(cmd, cwd=None):
    return subprocess.run(cmd, cwd=cwd, capture_output=True, text=True, encoding="utf-8", errors="replace")


def test_sources():
    out = {}
    with open(os.path.join(HERE, "TEST_PATTERNS.tsv"), encoding="utf-8") as f:
        next(f)
        for line in f:
            test = line.split("\t")[3]
            if "::" in test:
                path = test.split("::")[0]
                out[os.path.basename(path)[:-len(".java")]] = os.path.join(REPO, path)
    return out


def junit(patch, flags, tests, selector):
    return run([JDK + "/bin/java", "-ea", "-esa", "--patch-module", "java.base=" + patch] + flags
               + ["-jar", JUNIT, "execute", "--class-path", tests, "--select-method", selector,
                  "--disable-banner", "--details=summary", "--disable-ansi-colors"])


def verdict(result):
    if " 1 tests found" not in result.stdout:
        return "NOT_FOUND"
    return "FAILED" if " 1 tests failed" in result.stdout else "PASSED"


def main():
    out = sys.argv[1]
    result_tsv = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, "MUTATION_GATE.tsv")
    stage, classes, tests = (os.path.join(out, d) for d in ("stage", "classes", "testclasses"))
    rows = [l.rstrip("\n").split("\t") for l in open(os.path.join(HERE, "MUTANTS.tsv"), encoding="utf-8")][1:]
    sources = test_sources()
    baselines, results = {}, []
    for mid, path, original, mutated, test_class, test_method in rows:
        selector = f"{test_class}#{test_method}"
        if test_class not in sources:
            results.append((mid, path, test_class, test_method, "INVALID", "test class not in TEST_PATTERNS.tsv"))
            continue
        flags = jtreg_flags.flags(sources[test_class], "run")
        if selector not in baselines:
            baselines[selector] = verdict(junit(classes, flags, tests, selector))
        if baselines[selector] != "PASSED":
            results.append((mid, path, test_class, test_method, "INVALID", "baseline " + baselines[selector]))
            continue
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
        mutant = junit(os.path.join(work, "classes") + os.pathsep + classes, flags, tests, selector)
        state = verdict(mutant)
        status = {"FAILED": "KILLED", "PASSED": "SURVIVED"}.get(state, "INVALID")
        results.append((mid, path, test_class, test_method, status,
                        "baseline=PASSED exit=" + str(mutant.returncode)))
        print(mid, status, flush=True)
    with open(result_tsv, "w", encoding="utf-8", newline="\n") as w:
        w.write("mutant\tsource\ttest_class\ttest_method\tstatus\tdetail\n")
        w.writelines("\t".join(r) + "\n" for r in results)
    killed = sum(r[4] == "KILLED" for r in results)
    print(f"mutants={len(results)} killed={killed} survived={sum(r[4] == 'SURVIVED' for r in results)} "
          f"invalid={sum(r[4] == 'INVALID' for r in results)}")
    sys.exit(0 if killed == len(results) else 1)


if __name__ == "__main__":
    main()
