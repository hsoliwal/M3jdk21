#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""tree-lanes-port-20261107 (m3/file): one mutant per adapted class, killed by the seeded tree-corpus differential.

usage: mutants.py <repo-root> <deps-classpath> <donor-output-file> <out-dir> [java-home] [episodes] [seed]

Each mutant copies m3/file/src/main/java, applies one textual mutation (exactly one occurrence), compiles the whole
module main tree (module-info excluded, classpath form) plus differential/TreeLeafDifferential.java, runs the
differential driver on the head side and compares its output with the donor-side output of differential.sh: a
different sha256 (or a javac failure) kills the mutant. The unmutated tree is run first as the baseline and must
reproduce the donor output, else every mutant is INCONCLUSIVE. MUTANTS.tsv in <out-dir> records the result.
"""
import glob, hashlib, os, shutil, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
MUTANTS = [
    ("M1_scanspec_lazy_default_residency_halved", "m3/file/src/main/java/jdk/m3/file/M3FileScanSpec.java",
     "        return new M3FileScanSpec(depth, DEFAULT_RESIDENCY);",
     "        return new M3FileScanSpec(depth, DEFAULT_RESIDENCY / 2);"),
]


def sha256_file(path):
    with open(path, "rb") as handle:
        return hashlib.sha256(handle.read()).hexdigest()


def run(cmd, log):
    with open(log, "w", encoding="utf-8") as handle:
        return subprocess.run(cmd, stdout=handle, stderr=subprocess.STDOUT).returncode


def compile_and_run(root, deps, work, java_home, mutation, tmp, episodes, seed):
    main_src = os.path.join(work, "main")
    shutil.copytree(os.path.join(root, "m3/file/src/main/java"), main_src)
    if mutation is not None:
        name, rel, before, after = mutation
        path = os.path.join(main_src, rel[len("m3/file/src/main/java/"):])
        with open(path, encoding="utf-8") as handle:
            text = handle.read()
        if text.count(before) != 1:
            return "INCONCLUSIVE_ANCHOR(%d)" % text.count(before), None
        with open(path, "w", encoding="utf-8", newline="") as handle:
            handle.write(text.replace(before, after))
    shutil.copy(os.path.join(HERE, "differential", "TreeLeafDifferential.java"), os.path.join(main_src, "jdk", "m3", "file"))
    javac = os.path.join(java_home, "bin", "javac")
    java = os.path.join(java_home, "bin", "java")
    classes = os.path.join(work, "classes"); os.makedirs(classes)
    sources = [p for p in glob.glob(os.path.join(main_src, "**", "*.java"), recursive=True) if not p.endswith("module-info.java")]
    rc = run([javac, "-J-Djava.io.tmpdir=" + tmp, "-proc:none", "--release", "21", "-cp", deps, "-d", classes] + sources,
             os.path.join(work, "javac-main.log"))
    if rc != 0:
        return "KILLED_BY_JAVAC", None
    out = os.path.join(work, "head.txt")
    rc = run([java, "-Djava.io.tmpdir=" + tmp, "-cp", classes + os.pathsep + deps, "jdk.m3.file.TreeLeafDifferential",
              str(seed), str(episodes), out, os.path.join(tmp, "corpus-" + os.path.basename(work))], os.path.join(work, "run.log"))
    if rc != 0:
        return "KILLED_BY_EXIT(%d)" % rc, None
    return "RAN", sha256_file(out)


def main():
    root, deps, donor_out, out_dir = sys.argv[1:5]
    java_home = sys.argv[5] if len(sys.argv) > 5 else os.environ.get("JAVA_HOME", "C:/Program Files/Java/jdk-21")
    episodes = int(sys.argv[6]) if len(sys.argv) > 6 else 200
    seed = int(sys.argv[7], 0) if len(sys.argv) > 7 else 0x54524545
    tmp = os.path.join(out_dir, "tmp"); os.makedirs(tmp, exist_ok=True)
    expected = sha256_file(donor_out)
    base_work = os.path.join(out_dir, "baseline"); shutil.rmtree(base_work, ignore_errors=True)
    state, digest = compile_and_run(root, deps, base_work, java_home, None, tmp, episodes, seed)
    baseline_ok = state == "RAN" and digest == expected
    rows = ["mutant\tfile\tstate\tkiller"]
    rows.append("BASELINE\tm3/file/src/main/java\t%s\tdonor sha256 %s" % ("PASS" if baseline_ok else "FAIL(%s %s)" % (state, digest), expected[:12]))
    killed = 0
    for mutation in MUTANTS:
        work = os.path.join(out_dir, mutation[0]); shutil.rmtree(work, ignore_errors=True)
        if not baseline_ok:
            rows.append("%s\t%s\tINCONCLUSIVE_BASELINE\t" % (mutation[0], mutation[1])); continue
        state, digest = compile_and_run(root, deps, work, java_home, mutation, tmp, episodes, seed)
        if state == "RAN":
            state = "KILLED" if digest != expected else "SURVIVED"
        elif state.startswith("KILLED"):
            state = "KILLED(%s)" % state
        if state.startswith("KILLED"):
            killed += 1
        rows.append("%s\t%s\t%s\tTreeLeafDifferential sha256 %s vs donor %s" % (mutation[0], mutation[1], state, (digest or "-")[:12], expected[:12]))
    with open(os.path.join(out_dir, "MUTANTS.tsv"), "w", encoding="utf-8", newline="") as handle:
        handle.write("\n".join(rows) + "\n")
    print("\n".join(rows))
    print("killed=%d/%d baseline=%s" % (killed, len(MUTANTS), "PASS" if baseline_ok else "FAIL"))
    sys.exit(0 if baseline_ok and killed == len(MUTANTS) else 1)


if __name__ == "__main__":
    main()
