#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Compile exact generated java.util additions and run their Java21 contract oracles."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess

BASE = "2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49"
DEPENDENCIES = {
    "src/java.base/share/classes/java/util/concurrent/ProgressMonitor.java":
        "58d084535f0e1c76adc102cffd1eff2cadae5c80877812908281699a55ce88b3",
    "src/java.base/share/classes/java/util/concurrent/ProgressMonitors.java":
        "5073890ccf101b597e656b2516be70b83f0b77f9accdc157d75fc8d6f525a359",
}
UNCHANGED_SORT_OWNERS = {
    "TimSort.java": "f147bfc6470b811546384b17b7fd3ed278586db806f445d66151a34f617c0d19",
    "ComparableTimSort.java": "f0f160ffeb40b43d61ae6808c86735968dfbd32e118a40d71f54166ba0253537",
    "DualPivotQuicksort.java": "af55f12f11a2821c910629a5701f605e526a99fe85428d4084336c76b124fb8a",
    "Arrays.java": "fb63f716ddcf750357d2c5747896336b45ad343eb4d3c2de329f3212c6bf49ec",
}

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def run(command, log, env):
    result = subprocess.run([str(part) for part in command], text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, env=env)
    log.write_text(result.stdout, encoding="utf-8")
    print(log.name + ": exit=" + str(result.returncode), flush=True)
    print("\n".join(result.stdout.splitlines()[-4:]), flush=True)
    if result.returncode:
        raise SystemExit("Failed command; inspect " + str(log))
    return result.stdout

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[3])
    parser.add_argument("--recipe-crate", type=Path)
    parser.add_argument("--build-dir", type=Path)
    parser.add_argument("--java-home", type=Path)
    parser.add_argument("--skip-benchmark", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    crate = (args.recipe_crate or
             root / "m3/tooling/sort-recipes/m3-java-util-sorts-20261018").resolve()
    build = (args.build_dir or root / "build/m3-sorting-qualification").resolve()
    build.mkdir(parents=True, exist_ok=True)
    java_home = args.java_home or (Path(os.environ["JAVA_HOME"]) if "JAVA_HOME" in os.environ else None)
    java = java_home / "bin/java" if java_home else Path(shutil.which("java") or "java")
    javac = java_home / "bin/javac" if java_home else Path(shutil.which("javac") or "javac")
    javap = java_home / "bin/javap" if java_home else Path(shutil.which("javap") or "javap")
    env = os.environ.copy()
    env["TMPDIR"] = str(build)
    version = run([javac, "-version"], build / "javac-version.log", env).strip()
    if not version.startswith("javac 21"):
        raise SystemExit("Java21 compiler required; observed " + version)
    run([java, "-version"], build / "java-version.log", env)
    checked = {}
    java_rows = []
    for kind in ("java", "text"):
        manifest = crate / "payload" / kind / "manifest.tsv"
        for line in manifest.read_text(encoding="utf-8").splitlines():
            if not line.strip() or line.startswith("#"):
                continue
            target, before, after, template = line.split("\t")
            if before != "ABSENT":
                raise SystemExit("This additive packet only admits new paths")
            target_file = root / target
            template_file = crate / "payload" / kind / template
            if digest(template_file) != after or digest(target_file) != after:
                raise SystemExit("Source/template drift: " + target)
            checked[target] = after
            if kind == "java":
                java_rows.append(target)
    for target, expected in DEPENDENCIES.items():
        if digest(root / target) != expected:
            raise SystemExit("Existing progress owner changed; qualify a successor packet: " + target)
        checked[target] = expected
    for filename, expected in UNCHANGED_SORT_OWNERS.items():
        target = "src/java.base/share/classes/java/util/" + filename
        if digest(root / target) != expected:
            raise SystemExit("Existing sorting owner changed; qualify a successor packet: " + target)
        checked[target] = expected
    (build / "source-hashes.json").write_text(json.dumps(checked, indent=2, sort_keys=True) + "\n")
    patch_source = build / "patch-source"
    classes = build / "patch-classes"
    tests = build / "test-classes"
    for directory in (patch_source, classes, tests):
        directory.mkdir(parents=True, exist_ok=True)
    original_sources = []
    new_sources = []
    for target in list(DEPENDENCIES) + [row for row in java_rows if row.startswith("src/")]:
        relative = Path(target).relative_to("src/java.base/share/classes")
        staged = patch_source / relative
        staged.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(root / target, staged)
        (original_sources if target in DEPENDENCIES else new_sources).append(staged)
    patch = str(patch_source)
    # Preserve deliberate unreferenced try resources in the pre-existing progress owner.
    run([javac, "--release", "21", "--patch-module", "java.base=" + patch,
         "-proc:none", "-Xlint:all,-try", "-Werror", "-d", classes, *original_sources],
        build / "baseline-progress-compile.log", env)
    run([javac, "--release", "21", "--patch-module",
         "java.base=" + str(classes) + os.pathsep + patch,
         "-proc:none", "-Xlint:all", "-Werror", "-d", classes, *new_sources],
        build / "sorting-compile.log", env)
    test_sources = [root / row for row in java_rows if row.startswith("test/")]
    run([javac, "--release", "21", "--patch-module", "java.base=" + str(classes),
         "-proc:none", "-Xlint:all", "-Werror", "-d", tests, *test_sources],
        build / "test-compile.log", env)
    command = [java, "-Xmx512m", "--patch-module", "java.base=" + str(classes), "-cp", tests]
    run([*command, "SortsContract"], build / "contract-default.log", env)
    run([java, "-Xint", "-Xmx512m", "--patch-module", "java.base=" + str(classes),
         "-cp", tests, "SortsContract", "--smoke"], build / "contract-interpreted.log", env)
    run([javap, "-classpath", classes, "-public", "java.util.Sorts", "java.util.SortAlgorithm"],
        build / "public-api.txt", env)
    if not args.skip_benchmark:
        run([*command, "SortsBenchmark", "512", "9"], build / "benchmark.csv", env)
    receipt = {
        "target_base": BASE,
        "checked_revision": subprocess.check_output(["git", "-C", str(root), "rev-parse", "HEAD"], text=True).strip(),
        "compiler": version,
        "generated_java_files": len(java_rows),
        "source_hashes": checked,
        "qualification": "Java21 patch-module actual generated sources and exact existing progress owners",
        "unexecuted": ["full M3JDK image build", "product jtreg harness", "cross-platform/GC/CDS qualification"],
        "benchmark_scope": "exploratory single-VM timings and per-thread allocation, setup included",
    }
    (build / "qualification.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print("SORTS_RECEIVER_QUALIFICATION_PASS sources=" + str(len(java_rows)) + " build=" + str(build))

if __name__ == "__main__":
    main()
