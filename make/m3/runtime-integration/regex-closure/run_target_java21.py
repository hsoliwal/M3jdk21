#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# Copyright 2026 Hitesh Soliwal and contributors
"""Thin target-owned qualification over actual source inputs; no Synexia runtime dependency.

Uses the existing regex-rolling-cursor source-extraction pattern. Does not apply
or rewrite production source. This proves an extracted actual Java leaf and the
complete actual target C via JNI on a host Java21. Real M3JDK image/jtreg, backing
lifetime and product dispatch remain separate gates.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys

HERE = Path(__file__).resolve().parent


# A fresh observer has exactly one direct child, so RUSAGE_CHILDREN is per workload.
CHILD_RESOURCE_OBSERVER = r"""
import json
from pathlib import Path
import resource
import subprocess
import sys
import time

if not sys.platform.startswith("linux"):
    raise SystemExit("resource observer currently qualifies Linux ru_maxrss in KiB only")
report = Path(sys.argv[1])
limit = float(sys.argv[2])
command = sys.argv[3:]
started = time.monotonic()
child_started = True
failure = None
try:
    result = subprocess.run(command, check=False, timeout=limit)
    exit_code = result.returncode
    outcome = "EXITED"
except subprocess.TimeoutExpired:
    exit_code = 124
    outcome = "TIMEOUT_DIRECT_CHILD_KILLED_AND_REAPED"
except OSError as exception:
    child_started = False
    exit_code = 127
    outcome = "CHILD_LAUNCH_FAILED"
    failure = str(exception)
usage = resource.getrusage(resource.RUSAGE_CHILDREN)
observation = {
    "schema": "M3_REGEX_CHILD_RESOURCES_V1",
    "observer": "fresh_python_process_single_direct_child",
    "child_started": child_started,
    "child_exit_code": exit_code,
    "outcome": outcome,
    "failure": failure,
    "wall_seconds": time.monotonic() - started,
    "user_cpu_seconds": usage.ru_utime if child_started else None,
    "system_cpu_seconds": usage.ru_stime if child_started else None,
    "max_rss_kib": usage.ru_maxrss if child_started else None,
    "max_rss_unit": "KiB",
    "resource_scope": "child_and_reaped_descendants_excludes_observer",
    "rss_scope": "Linux_process_high_water_mark_not_retained_Java_heap"
}
report.write_text(json.dumps(observation, indent=2, sort_keys=True) + "\n", encoding="utf-8")
sys.exit(exit_code)
"""


def digest(data):
    return hashlib.sha256(data).hexdigest()


def output_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def save(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def extract(text, start, end):
    if text.count(start) != 1:
        raise ValueError("nonunique extraction anchor: " + start)
    first = text.index(start)
    return text[first:text.index(end, first + len(start))]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stage", required=True, choices=("static", "compile", "test", "benchmark"))
    parser.add_argument("--jdk", required=True, type=Path)
    parser.add_argument("--repo", type=Path, default=HERE.parents[2])
    parser.add_argument("--baseline", type=Path,
                        help="Optional exact before-image directory; otherwise read pinned git blobs.")
    parser.add_argument("--output", type=Path, default=HERE / "evidence/local")
    parser.add_argument("--cc", default="cc")
    args = parser.parse_args()
    args.jdk, args.repo, args.output = args.jdk.resolve(), args.repo.resolve(), args.output.resolve()
    seal_path = HERE / "source-seals.json"
    seals = json.loads(seal_path.read_text(encoding="utf-8"))
    seal_hash = digest(seal_path.read_bytes())
    sources = {}
    for item in seals["sources"]:
        if args.baseline is None:
            before = subprocess.run(
                ["git", "-C", str(args.repo), "show", seals["baseline"] + ":" + item["path"]],
                check=True, stdout=subprocess.PIPE).stdout
        else:
            before = (args.baseline / (Path(item["path"]).name + ".txt")).read_bytes()
        after = (args.repo / item["path"]).read_bytes()
        for version, data in (("before", before), ("after", after)):
            if digest(data) != item[version + "_sha256"]:
                raise ValueError("source drift: " + version + " " + item["path"])
            sources[version, Path(item["path"]).name] = data.decode("utf-8", "strict")
    regression = seals["target_regression"]
    if digest((args.repo / regression["path"]).read_bytes()) != regression["sha256"]:
        raise ValueError("target jtreg source drift")
    for relative, expected in seals["fixture_seals"].items():
        if digest((HERE / relative).read_bytes()) != expected:
            raise ValueError("fixture drift: " + relative)
    prior = {"compile": "static", "test": "compile", "benchmark": "test"}.get(args.stage)
    if prior is not None:
        previous = json.loads((args.output / (prior + ".json")).read_text(encoding="utf-8"))
        if previous["status"] != "PASS" or previous["source_seals_sha256"] != seal_hash:
            raise ValueError("previous stage absent, failed or stale")
    commands = []
    receipt = {"stage": args.stage, "status": "NOT_RUN", "source_seals_sha256": seal_hash,
               "jdk": str(args.jdk), "repo": str(args.repo),
               "coverage": "ISOLATED_SOURCE_AND_ACTUAL_C_JNI_NOT_M3JDK_PRODUCT"}
    def call(label, command, resource=False):
        log = args.output / "logs" / (args.stage + "-" + label + ".log")
        log.parent.mkdir(parents=True, exist_ok=True)
        command = list(map(str, command))
        resource_path = None
        if resource:
            resource_path = log.with_suffix(".resource.json")
            command = [sys.executable, "-c", CHILD_RESOURCE_OBSERVER,
                       str(resource_path), "175"] + command
        environment = dict(os.environ)
        environment["LC_ALL"] = "C"
        environment["UBSAN_OPTIONS"] = "halt_on_error=1:print_stacktrace=1"
        result = subprocess.run(command, cwd=HERE, env=environment, text=True,
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                timeout=180, check=False)
        log.write_text(result.stdout, encoding="utf-8")
        record = {"label": label, "command": command, "exit": result.returncode,
                  "log": str(log), "log_sha256": digest(log.read_bytes())}
        if resource_path is not None:
            record["resource_log"] = str(resource_path)
            record["resource_log_sha256"] = digest(resource_path.read_bytes())
        commands.append(record)
        print(args.stage + " " + label + " exit=" + str(result.returncode), flush=True)
        if result.returncode != 0:
            raise RuntimeError(label + ": see " + str(log))
        return result.stdout
    generated, headers, classes = (args.output / name for name in ("generated", "headers", "classes"))
    try:
        if args.stage == "static":
            for version, owner, arg in (("before", "BeforeClose", "long[] closure"),
                                         ("after", "AfterClose", "long star")):
                source = sources[version, "M3StringRegexDp.java"]
                method = extract(source, "    private static long close(",
                                 "    /** Larger admitted patterns")
                value = "closure" if version == "before" else "star"
                notice = source[:source.index("package java.lang;")]
                save(generated / (owner + ".java"), notice + "\nfinal class " + owner + " {\n"
                     + "    static long apply(long bits, " + arg + ") { return close(bits, "
                     + value + "); }\n" + method + "}\n")
                save(generated / version / "M3StringRegexDp.c",
                     sources[version, "M3StringRegexDp.c"])
            source = sources["after", "M3StringRegexDp.java"]
            start = "    private static native long advance0("
            first = source.index(start)
            declaration = source[first:source.index(";", first) + 1]
            save(generated / "binding/java/lang/M3StringRegexDp.java",
                 source[:source.index("package java.lang;")] + "package java.lang;\n"
                 + "/** Header declaration only; never loaded as a production owner. */\n"
                 + "final class M3StringRegexDp {\n" + declaration + "\n}\n")
            call("python-syntax", [sys.executable, "-m", "py_compile", str(Path(__file__))])
        elif args.stage == "compile":
            if not sys.platform.startswith("linux"):
                raise ValueError("this received host fixture currently qualifies Linux only")
            headers.mkdir(parents=True, exist_ok=True)
            classes.mkdir(parents=True, exist_ok=True)
            javac = [args.jdk / "bin/javac", "--release", "21", "-proc:none", "-Xlint:all", "-Werror"]
            java_sources = sorted((HERE / "src/test/java").glob("*.java"))
            call("javac-fixtures", javac + ["-h", headers, "-d", classes] + java_sources
                 + [generated / "BeforeClose.java", generated / "AfterClose.java",
                    args.repo / "test/jdk/java/lang/String/M3StringRegexClosureTest.java"])
            binding = generated / "binding"
            call("javac-real-declaration", javac + ["--patch-module", "java.base=" + str(binding),
                 "-h", headers, "-d", args.output / "header-classes",
                 binding / "java/lang/M3StringRegexDp.java"])
            expected = set()
            for name in ("java_lang_M3StringRegexDp.h", "RegexDpNativeHarness.h"):
                expected.update(re.findall(r"JNICALL\s+(Java_[A-Za-z0-9_]+)\s*\(",
                                           (headers / name).read_text(encoding="utf-8")))
            if expected != {"Java_java_lang_M3StringRegexDp_advance0",
                            "Java_RegexDpNativeHarness_run"}:
                raise ValueError("generated JNI contract mismatch")
            for version in ("before", "after"):
                for mode in ("normal", "ubsan"):
                    library = args.output / "native" / version / mode / "libm3regexproof.so"
                    library.parent.mkdir(parents=True, exist_ok=True)
                    flags = ["-std=c17", "-O2", "-Wall", "-Wextra", "-Werror", "-pedantic",
                             "-fPIC", "-shared"]
                    if mode == "ubsan":
                        flags += ["-fsanitize=undefined", "-fno-sanitize-recover=undefined"]
                    call("cc-" + version + "-" + mode, [args.cc] + flags
                         + ["-I" + str(headers), "-I" + str(args.jdk / "include"),
                            "-I" + str(args.jdk / "include/linux"),
                            generated / version / "M3StringRegexDp.c",
                            HERE / "src/test/native/RegexDpNativeAdapter.c", "-o", library])
                    symbols = call("symbols-" + version + "-" + mode,
                                   ["nm", "-D", "--defined-only", library])
                    actual = {line.split()[-1] for line in symbols.splitlines()
                              if line.split() and line.split()[-1].startswith("Java_")}
                    if actual != expected:
                        raise ValueError("exported JNI contract mismatch")
        elif args.stage == "test":
            call("resource-observer", [sys.executable, "-c",
                 "print('M3_RESOURCE_OBSERVER_CHILD_PASS')"], resource=True)
            observation = json.loads((args.output / "logs/test-resource-observer.resource.json")
                                     .read_text(encoding="utf-8"))
            if not observation["child_started"] or observation["child_exit_code"] != 0 \
                    or observation["max_rss_unit"] != "KiB" or observation["max_rss_kib"] <= 0 \
                    or observation["user_cpu_seconds"] < 0 or observation["system_cpu_seconds"] < 0:
                raise ValueError("child-resource observation is missing or invalid")
            base = [args.jdk / "bin/java", "-Xms256m", "-Xmx256m", "-ea"]
            call("closure-graph", base + ["-cp", classes, "RegexClosureProof"])
            observations = {}
            for version in ("before", "after"):
                for mode in ("normal", "ubsan"):
                    library = args.output / "native" / version / mode
                    observations[version, mode] = call("native-" + version + "-" + mode,
                         base + ["-Xcheck:jni", "-Djava.library.path=" + str(library),
                                 "-cp", classes, "RegexDpNativeHarness"])
            for mode in ("normal", "ubsan"):
                if observations["before", mode] != observations["after", mode]:
                    raise ValueError("before/after native observations differ")
        else:
            base = [args.jdk / "bin/java", "-Xms256m", "-Xmx256m"]
            for fork in range(3):
                order = ("before", "after") if fork % 2 == 0 else ("after", "before")
                for version in order:
                    call("closure-" + version + "-" + str(fork),
                         base + ["-cp", classes, "RegexClosureBenchmark", version], resource=True)
                    library = args.output / "native" / version / "normal"
                    call("native-" + version + "-" + str(fork),
                         base + ["-Djava.library.path=" + str(library), "-cp", classes,
                                 "RegexDpNativeHarness", "benchmark"], resource=True)
        receipt["status"] = "PASS"
    except Exception as failure:
        receipt["status"] = "FAIL"
        receipt["kind"] = type(failure).__name__
        receipt["signature"] = str(failure)
        receipt["commands"] = commands
        fingerprint = digest(json.dumps(receipt, sort_keys=True).encode())[:16]
        output_json(args.output / "failures" / (args.stage + "-" + fingerprint + ".json"), receipt)
        output_json(args.output / (args.stage + ".json"), receipt)
        raise
    receipt["commands"] = commands
    output_json(args.output / (args.stage + ".json"), receipt)
    print("M3_REGEX_TARGET_STAGE " + args.stage + " PASS; actual M3JDK jtreg remains separate")


if __name__ == "__main__":
    main()
