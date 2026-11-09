#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""retained-port-20261107 (m3/file): one mutant per adapted or edited class, replayed through the ported tests.

usage: mutants.py <repo-root> <deps-classpath> <junit-console-standalone-jar> <out-dir> [java-home]

Each mutant copies m3/file/src/main/java, applies one textual mutation (exactly one occurrence), compiles the whole
module main tree plus the killer test sources, and runs the killer: a JUnit class through the console launcher or a
contract main. A non-zero exit kills the mutant. The unmutated tree is run first as the baseline (must pass, else the
mutant is INCONCLUSIVE). MUTANTS.tsv in <out-dir> records the result.
"""
import os, shutil, subprocess, sys, glob

MUTANTS = [
    ("M1_scope_identity_ignores_path", "m3/file/src/main/java/jdk/m3/file/M3FileScope.java",
     "path.getFileSystem() == previous.getFileSystem() && path.equals(previous)",
     "path.getFileSystem() == previous.getFileSystem()",
     "junit", "jdk.m3.file.M3FileScopeTest", ["M3FileScopeTest.java"]),
    ("M2_source_byteAt_signed", "m3/file/src/main/java/jdk/m3/file/M3FileSource.java",
     "return Byte.toUnsignedInt(bytes[Objects.checkIndex(offset, bytes.length)]);",
     "return bytes[Objects.checkIndex(offset, bytes.length)];",
     "main", "jdk.m3.file.M3FileTextCoordinatesContract", ["M3FileTextCoordinatesContract.java"]),
]


def run(cmd, log):
    with open(log, "w", encoding="utf-8") as handle:
        return subprocess.run(cmd, stdout=handle, stderr=subprocess.STDOUT).returncode


def build_and_run(root, deps, console, work, java_home, mutation, tmp):
    main_src = os.path.join(work, "main")
    shutil.copytree(os.path.join(root, "m3/file/src/main/java"), main_src)
    if mutation is not None:
        name, rel, before, after = mutation
        path = os.path.join(main_src, rel[len("m3/file/src/main/java/"):])
        with open(path, encoding="utf-8") as handle:
            text = handle.read()
        if text.count(before) != 1:
            return "INCONCLUSIVE_ANCHOR(%d)" % text.count(before)
        with open(path, "w", encoding="utf-8", newline="") as handle:
            handle.write(text.replace(before, after))
    javac = os.path.join(java_home, "bin", "javac")
    java = os.path.join(java_home, "bin", "java")
    classes = os.path.join(work, "classes"); os.makedirs(classes)
    sources = [p for p in glob.glob(os.path.join(main_src, "**", "*.java"), recursive=True) if not p.endswith("module-info.java")]
    rc = run([javac, "-J-Djava.io.tmpdir=" + tmp, "-proc:none", "--release", "21", "-cp", deps, "-d", classes] + sources,
             os.path.join(work, "javac-main.log"))
    if rc != 0:
        return "KILLED_BY_JAVAC"
    return classes


def killer(root, deps, console, work, java_home, classes, kind, target, test_files, tmp):
    javac = os.path.join(java_home, "bin", "javac")
    java = os.path.join(java_home, "bin", "java")
    tests = os.path.join(work, "test-classes"); os.makedirs(tests, exist_ok=True)
    cp = classes + os.pathsep + deps + (os.pathsep + console if kind == "junit" else "")
    sources = [os.path.join(root, "m3/file/src/test/java/jdk/m3/file", f) for f in test_files]
    rc = run([javac, "-J-Djava.io.tmpdir=" + tmp, "-proc:none", "--release", "21", "-cp", cp, "-d", tests] + sources,
             os.path.join(work, "javac-test.log"))
    if rc != 0:
        return "KILLED_BY_TEST_JAVAC"
    if kind == "junit":
        cmd = [java, "-Djava.io.tmpdir=" + tmp, "-jar", console, "execute", "--disable-banner", "--fail-if-no-tests",
               "--class-path", tests + os.pathsep + classes + os.pathsep + deps, "--select-class", target]
    else:
        cmd = [java, "-Djava.io.tmpdir=" + tmp, "-cp", tests + os.pathsep + classes + os.pathsep + deps, target]
    rc = run(cmd, os.path.join(work, "run.log"))
    return "PASS" if rc == 0 else "FAIL(exit %d)" % rc


def main():
    root, deps, console, out = [os.path.abspath(a) for a in sys.argv[1:5]]
    java_home = sys.argv[5] if len(sys.argv) > 5 else os.environ.get("JAVA_HOME", "C:/Program Files/Java/jdk-21")
    if os.path.exists(out):
        shutil.rmtree(out)
    os.makedirs(out)
    tmp = os.path.join(out, "tmp"); os.makedirs(tmp)
    rows = []
    for name, rel, before, after, kind, target, test_files in MUTANTS:
        base = os.path.join(out, name + "-baseline")
        classes = build_and_run(root, deps, console, base, java_home, None, tmp)
        baseline = killer(root, deps, console, base, java_home, classes, kind, target, test_files, tmp) if os.path.isdir(classes) else classes
        work = os.path.join(out, name)
        classes = build_and_run(root, deps, console, work, java_home, (name, rel, before, after), tmp)
        if os.path.isdir(classes):
            outcome = killer(root, deps, console, work, java_home, classes, kind, target, test_files, tmp)
            verdict = "KILLED" if outcome.startswith("FAIL") else "SURVIVED"
        else:
            outcome = classes
            verdict = "KILLED" if classes.startswith("KILLED") else "INCONCLUSIVE"
        if baseline != "PASS":
            verdict = "INCONCLUSIVE_BASELINE"
        rows.append((name, rel, before, after, kind + ":" + target, baseline, outcome, verdict))
        print("%s baseline=%s mutant=%s %s" % (name, baseline, outcome, verdict))
    with open(os.path.join(out, "MUTANTS.tsv"), "w", encoding="utf-8", newline="") as handle:
        handle.write("mutant\tfile\tbefore\tafter\tkiller\tbaseline\tmutant_outcome\tverdict\n")
        for row in rows:
            handle.write("\t".join(row) + "\n")
    killed = sum(1 for row in rows if row[-1] == "KILLED")
    print("mutants=%d killed=%d" % (len(rows), killed))
    sys.exit(0 if killed == len(rows) else 1)


if __name__ == "__main__":
    main()
