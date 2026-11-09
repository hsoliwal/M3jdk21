#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Thin recovery proof over actual owners, adapted from target #1371/#1376 runners.

This script copies sealed source inputs into an isolated compiler context. It does
not edit or apply target production source. OpenRewrite must emit the candidate
before this runner is invoked. Stock host String.class remains loaded.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import statistics
import subprocess
import sys

HERE = Path(__file__).resolve().parent
OWNER_ROOT = "src/java.base/share/classes/"
REGEX_JAVA = OWNER_ROOT + "java/lang/M3StringRegexDp.java"
CURSOR_JAVA = OWNER_ROOT + "java/lang/M3String.java"
REGEX_C = "src/java.base/share/native/libjava/M3StringRegexDp.c"
CURSOR_TEST = "test/jdk/java/lang/String/M3StringPathCursorTest.java"
CHILD_RESOURCE_OBSERVER = r"""
import json
from pathlib import Path
import resource
import subprocess
import sys
import time
if not sys.platform.startswith("linux"):
    raise SystemExit("resource observer requires Linux ru_maxrss KiB")
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
record = {
    "schema": "M3_REGEX_CHILD_RESOURCES_V1",
    "observer": "fresh_python_process_single_direct_child",
    "child_started": child_started, "child_exit_code": exit_code,
    "outcome": outcome, "failure": failure,
    "wall_seconds": time.monotonic() - started,
    "user_cpu_seconds": usage.ru_utime if child_started else None,
    "system_cpu_seconds": usage.ru_stime if child_started else None,
    "max_rss_kib": usage.ru_maxrss if child_started else None,
    "max_rss_unit": "KiB",
    "resource_scope": "child_and_reaped_descendants_excludes_observer",
    "rss_scope": "Linux_process_high_water_mark_not_retained_Java_heap"
}
report.write_text(json.dumps(record, indent=2, sort_keys=True) + "\n")
sys.exit(exit_code)
"""


def sha(data):
    return hashlib.sha256(data).hexdigest()


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")


def source_blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--stage", choices=("static", "compile", "test", "benchmark"), required=True)
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--symbols", type=Path, required=True,
                        help="Exact actual target java.base/share/classes symbol root")
    parser.add_argument("--before", type=Path, required=True,
                        help="Exact published #1376 repository-shaped Java/C baseline")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--cc", default="cc")
    args = parser.parse_args()
    for name in ("jdk", "repo", "symbols", "before", "output"):
        setattr(args, name, getattr(args, name).resolve())
    args.output.mkdir(parents=True, exist_ok=True)
    receipt_path = args.output / (args.stage + ".json")
    if receipt_path.exists():
        raise ValueError("stage receipt is immutable; use a new attempt directory")
    seals_path = HERE / "source-seals.json"
    seal_bytes = seals_path.read_bytes()
    seals = json.loads(seal_bytes)
    seal_hash = sha(seal_bytes)
    commands = []
    record = {"SPDX-FileCopyrightText": "2026 Hitesh Soliwal and Contributors to the Synexia Project",
              "SPDX-License-Identifier": "Apache-2.0", "schema": "M3_REGEX_RECOVERY_STAGE_V1",
              "stage": args.stage, "status": "RUNNING", "source_seals_sha256": seal_hash,
              "jdk": str(args.jdk), "repo": str(args.repo),
              "coverage": "ACTUAL_OWNERS_HOST_PATCH_MODULE_AND_ACTUAL_PRODUCTION_C_JNI",
              "phase": "STRING", "full_image": False, "commands": commands}

    def call(label, command, timeout=300):
        log = args.output / "logs" / (args.stage + "-" + label + ".log")
        log.parent.mkdir(parents=True, exist_ok=True)
        resources = log.with_suffix(".resources.json")
        command = list(map(str, command))
        observer = [sys.executable, "-c", CHILD_RESOURCE_OBSERVER, str(resources),
                    str(timeout)] + command
        env = dict(os.environ, LC_ALL="C", UBSAN_OPTIONS="halt_on_error=1:print_stacktrace=1")
        with log.open("w") as output:
            result = subprocess.run(observer, stdout=output, stderr=subprocess.STDOUT,
                                    cwd=HERE, env=env, timeout=timeout + 10, check=False)
        entry = {"label": label, "argv": command, "exit_code": result.returncode,
                 "log": str(log.relative_to(args.output)), "log_sha256": sha(log.read_bytes()),
                 "resources": str(resources.relative_to(args.output)),
                 "resources_sha256": sha(resources.read_bytes())}
        commands.append(entry)
        observation = json.loads(resources.read_text())
        if (not observation["child_started"] or observation["max_rss_unit"] != "KiB"
                or observation["max_rss_kib"] <= 0):
            raise ValueError("invalid child resource observation: " + label)
        print(args.stage + " " + label + " exit=" + str(result.returncode), flush=True)
        if result.returncode:
            raise RuntimeError(label + ": " + str(log))
        return log.read_text()

    def artifacts(directories):
        return {str(path.relative_to(args.output)): sha(path.read_bytes())
                for directory in directories
                for path in sorted((args.output / directory).rglob("*")) if path.is_file()}

    def seal_artifacts(name, directories):
        path = args.output / name
        if path.exists():
            raise ValueError("intermediate seal is immutable: " + name)
        write_json(path, artifacts(directories))

    def verify_artifacts(name, directories):
        expected = json.loads((args.output / name).read_text())
        if artifacts(directories) != expected:
            raise ValueError("missing, extra or changed intermediate artifact: " + name)

    def patch(version):
        return args.output / "classes" / version

    def library(version, mode):
        return args.output / "native" / version / mode / "libm3regexproof.so"

    def java(version, interpreted=False):
        result = [args.jdk / "bin/java", "-Xms128m", "-Xmx256m", "-ea"]
        if interpreted:
            result.append("-Xint")
        return result + ["--patch-module", "java.base=" + str(patch(version))]

    try:
        for path, expected in seals["fixture_seals"].items():
            if sha((HERE / path).read_bytes()) != expected:
                raise ValueError("fixture drift: " + path)
        if sha((args.repo / CURSOR_TEST).read_bytes()) != seals["cursor_test_sha256"]:
            raise ValueError("cursor fixture drift")
        for item in seals["sources"]:
            if sha((args.repo / item["path"]).read_bytes()) != item["after_sha256"]:
                raise ValueError("target candidate source drift: " + item["path"])
            if item["path"] != CURSOR_JAVA and sha((args.before / item["path"]).read_bytes()) != item["before_sha256"]:
                raise ValueError("before source drift: " + item["path"])
        for item in seals["compiler_context"]:
            if OWNER_ROOT + item["path"] in (CURSOR_JAVA, REGEX_JAVA):
                continue  # These two complete owners are separately sealed above.
            if sha((args.symbols / item["path"]).read_bytes()) != item["sha256"]:
                raise ValueError("actual compiler symbol drift: " + item["path"])
        previous = {"compile": "static", "test": "compile", "benchmark": "test"}.get(args.stage)
        if previous:
            prior = json.loads((args.output / (previous + ".json")).read_text())
            if prior["status"] != "PASS" or prior["source_seals_sha256"] != seal_hash:
                raise ValueError("previous stage is absent, failed or stale")
        if args.stage != "static":
            verify_artifacts("SOURCE_CONTEXT_SEALS.json", ("source",))
        if args.stage in ("test", "benchmark"):
            verify_artifacts("COMPILED_ARTIFACT_SEALS.json", ("classes", "headers", "fixture-classes", "native"))

        if args.stage == "static":
            write_json(args.output / "SOURCE_SEALS_SNAPSHOT.json", seals)
            call("python-syntax", [sys.executable, "-c",
                 "import ast,pathlib,sys; ast.parse(pathlib.Path(sys.argv[1]).read_text()); print('PYTHON_SYNTAX_PASS')",
                 Path(__file__)])
            call("java-version", [args.jdk / "bin/java", "-version"])
            call("javac-version", [args.jdk / "bin/javac", "-version"])
            call("cc-version", [args.cc, "--version"])
            call("host", ["uname", "-a"])
            for version in ("before", "after"):
                context = args.output / "source" / version
                for item in seals["compiler_context"]:
                    target = context / item["path"]
                    target.parent.mkdir(parents=True, exist_ok=True)
                    full = OWNER_ROOT + item["path"]
                    if full == CURSOR_JAVA:
                        data = (args.repo / full).read_bytes()
                    elif full == REGEX_JAVA:
                        data = ((args.before if version == "before" else args.repo) / full).read_bytes()
                    else:
                        data = (args.symbols / item["path"]).read_bytes()
                    target.write_bytes(data)
            record["materialization"] = "byte-exact isolated source context; no source rewrite"
            record["benchmark_baseline"] = seals["benchmark_baseline"]
            seal_artifacts("SOURCE_CONTEXT_SEALS.json", ("source",))
        elif args.stage == "compile":
            headers = args.output / "headers"
            api = {}
            for version in ("before", "after"):
                context = args.output / "source" / version
                owners = sorted((context / "java/lang").glob("M3String*.java"))
                if len(owners) != 15:
                    raise ValueError("complete actual owner source inventory changed")
                patch(version).mkdir(parents=True, exist_ok=True)
                (headers / version).mkdir(parents=True, exist_ok=True)
                javac = [args.jdk / "bin/javac", "-J-Xmx512m", "--release", "21",
                         "-proc:none", "-implicit:none", "-Xlint:all", "-Werror"]
                call("owners-" + version, javac + ["-verbose", "--patch-module", "java.base=" + str(context),
                     "-d", patch(version), "-h", headers / version] + owners)
                emitted = sorted(patch(version).rglob("*.class"))
                if not emitted or any(not str(p.relative_to(patch(version))).startswith("java/lang/M3String") for p in emitted):
                    raise ValueError("implicit or platform String class emitted")
                names = [str(p.relative_to(patch(version)))[:-6].replace("/", ".") for p in emitted]
                api[version] = call("api-" + version, [args.jdk / "bin/javap", "-p", "-s",
                     "-classpath", patch(version)] + names)
                fixture = HERE / "src/test/java/java/lang/M3StringRegexRecoveryTest.java"
                call("owner-fixtures-" + version, javac + ["--patch-module",
                     "java.base=" + os.pathsep.join(map(str, [patch(version), fixture.parent, (args.repo / CURSOR_TEST).parent])),
                     "-d", patch(version), fixture, args.repo / CURSOR_TEST])
            if api["before"] != api["after"]:
                raise ValueError("complete actual-owner Java member/descriptor inventory changed")
            if (headers / "before/java_lang_M3StringRegexDp.h").read_bytes() != (headers / "after/java_lang_M3StringRegexDp.h").read_bytes():
                raise ValueError("generated actual-owner JNI header changed")
            classes = args.output / "fixture-classes"
            classes.mkdir(exist_ok=True)
            sources = sorted((HERE / "src/test/java").glob("*.java"))
            call("plain-fixtures", javac + ["--patch-module", "java.base=" + str(patch("after")),
                 "-d", classes, "-h", headers / "fixture"] + sources)
            expected = {"Java_java_lang_M3StringRegexDp_advance0", "Java_RegexDpNativeHarness_run"}
            generated = set()
            for path in (headers / "after/java_lang_M3StringRegexDp.h", headers / "fixture/RegexDpNativeHarness.h"):
                generated.update(re.findall(r"JNICALL\s+(Java_[A-Za-z0-9_]+)\s*\(", path.read_text()))
            if generated != expected:
                raise ValueError("generated actual JNI ABI differs")
            for version in ("before", "after"):
                for mode in ("normal", "ubsan"):
                    output = library(version, mode)
                    output.parent.mkdir(parents=True, exist_ok=True)
                    flags = ["-std=c17", "-O2", "-Wall", "-Wextra", "-Werror", "-pedantic", "-fPIC", "-shared"]
                    if mode == "ubsan":
                        flags += ["-fsanitize=undefined", "-fno-sanitize-recover=undefined"]
                    source = (args.before if version == "before" else args.repo) / REGEX_C
                    call("cc-" + version + "-" + mode, [args.cc] + flags + ["-I" + str(headers / "after"),
                         "-I" + str(headers / "fixture"), "-I" + str(args.jdk / "include"),
                         "-I" + str(args.jdk / "include/linux"), source,
                         HERE / "src/test/native/RegexDpNativeAdapter.c", "-o", output])
                    symbols = call("symbols-" + version + "-" + mode, ["nm", "-D", "--defined-only", output])
                    actual = {row.split()[-1] for row in symbols.splitlines()
                              if row.split() and row.split()[-1].startswith("Java_")}
                    if actual != expected:
                        raise ValueError("exported production JNI symbols changed")
            record["actual_owner_sources_per_version"] = 15
            record["complete_owner_member_descriptors_equal"] = True
            record["production_jni_headers_equal"] = True
            seal_artifacts("COMPILED_ARTIFACT_SEALS.json", ("classes", "headers", "fixture-classes", "native"))
        elif args.stage == "test":
            observations = {}
            for version in ("before", "after"):
                for interpreted in (False, True):
                    mode = "xint" if interpreted else "jit"
                    loadlog = args.output / "logs" / ("loaded-" + version + "-" + mode + ".log")
                    observations[version, "owner", mode] = call("owner-" + version + "-" + mode,
                         java(version, interpreted) + ["-Xlog:class+load=info:file=" + str(loadlog),
                         "java.lang.M3StringRegexRecoveryTest"], timeout=600)
                    loaded = loadlog.read_text()
                    for name in ("M3String", "M3StringAtom", "M3StringOwner", "M3StringTuple", "M3StringRegexDp"):
                        if not re.search(r" java.lang." + name + r" source: .*" + re.escape(str(patch(version))), loaded):
                            raise ValueError("actual owner not loaded from sealed patch: " + name)
                    if re.search(r" java.lang.String source: .*" + re.escape(str(patch(version))), loaded):
                        raise ValueError("modified platform String.class was loaded")
                    observations[version, "cursor", mode] = call("cursor-" + version + "-" + mode,
                         java(version, interpreted) + ["java.lang.M3StringPathCursorTest"], timeout=600)
                for mode in ("normal", "ubsan"):
                    observations[version, "native", mode] = call("native-" + version + "-" + mode,
                         [args.jdk / "bin/java", "-Xms128m", "-Xmx256m", "-ea", "-Xcheck:jni",
                          "-Djava.library.path=" + str(library(version, mode).parent), "-cp",
                          args.output / "fixture-classes", "RegexDpNativeHarness"], timeout=600)
                    observations[version, "owner-native", mode] = call("owner-native-" + version + "-" + mode,
                         java(version) + ["-Xcheck:jni", "java.lang.M3StringRegexRecoveryTest",
                          "native", library(version, mode)], timeout=600)
            for kind, modes in (("owner", ("jit", "xint")), ("cursor", ("jit", "xint")),
                                ("native", ("normal", "ubsan")), ("owner-native", ("normal", "ubsan"))):
                for mode in modes:
                    if observations["before", kind, mode] != observations["after", kind, mode]:
                        raise ValueError("before/after observation differs: " + kind + " " + mode)
                if observations["after", kind, modes[0]] != observations["after", kind, modes[1]]:
                    raise ValueError("execution-mode observation differs: " + kind)
            record["differential_observations"] = {"/".join(key): value.strip() for key, value in observations.items()}
            record["checked_jni_runs"] = 8
        else:
            measured = {}
            for fork in range(3):
                order = ("before", "after") if fork % 2 == 0 else ("after", "before")
                for version in order:
                    commands_for_kind = {
                        "java": java(version) + ["-cp", args.output / "fixture-classes", "RegexJavaBenchmark"],
                        "native": [args.jdk / "bin/java", "-Xms128m", "-Xmx256m",
                                   "-Djava.library.path=" + str(library(version, "normal").parent),
                                   "-cp", args.output / "fixture-classes", "RegexDpNativeHarness", "benchmark"]}
                    for kind, command in commands_for_kind.items():
                        output = call(kind + "-" + version + "-fork" + str(fork), command, timeout=300)
                        parsed = {}
                        for line in output.splitlines():
                            if line.startswith("M3_REGEX_" + kind.upper() + "_BENCH "):
                                cells = dict(cell.split("=", 1) for cell in line.split()[1:])
                                key = (kind, cells["scenario"], int(cells["width"]))
                                if key in parsed:
                                    raise ValueError("duplicate benchmark case")
                                parsed[key] = cells
                        if len(parsed) != (10 if kind == "java" else 14):
                            raise ValueError("incomplete frozen benchmark matrix")
                        measured[fork, version, kind] = parsed
            rows = []
            for kind in ("java", "native"):
                for key in sorted(measured[0, "before", kind]):
                    pairs = []
                    for fork in range(3):
                        before = measured[fork, "before", kind][key]
                        after = measured[fork, "after", kind][key]
                        ratio = float(after["median_ns_per_call"]) / float(before["median_ns_per_call"])
                        pairs.append({"fork": fork, "before": before, "after": after, "ratio": ratio})
                    median = statistics.median(pair["ratio"] for pair in pairs)
                    violations = sum(pair["ratio"] > 1.10 for pair in pairs)
                    rows.append({"kind": key[0], "scenario": key[1], "width": key[2], "pairs": pairs,
                                 "median_paired_ratio": median, "paired_ratios_above_1_10": violations,
                                 "rejects_candidate": median > 1.10 and violations >= 2})
            if len(rows) != 24:
                raise ValueError("benchmark selection requires all24 cases")
            selection = {"schema": "M3_REGEX_RECOVERY_BENCHMARK_SELECTION_V1", "case_count": len(rows),
                         "paired_forks": 3, "rule": seals["benchmark_rule"],
                         "decision": "REJECT" if any(row["rejects_candidate"] for row in rows) else "ADMIT_BOUNDED_CANDIDATE",
                         "cases": rows, "universal_performance_claim": False}
            write_json(args.output / "BENCHMARK_SELECTION.json", selection)
            record["selection"] = selection["decision"]
            record["benchmark_selection_sha256"] = sha((args.output / "BENCHMARK_SELECTION.json").read_bytes())
        if args.stage != "static":
            verify_artifacts("SOURCE_CONTEXT_SEALS.json", ("source",))
        if args.stage in ("compile", "test", "benchmark"):
            verify_artifacts("COMPILED_ARTIFACT_SEALS.json", ("classes", "headers", "fixture-classes", "native"))
        record["status"] = "PASS"
    except Exception as failure:
        record.update(status="FAIL", failure_kind=type(failure).__name__, failure=str(failure))
        write_json(receipt_path, record)
        raise
    write_json(receipt_path, record)
    print("M3_REGEX_RECOVERY_STAGE " + args.stage + " PASS; phase STRING, full-image gate remains separate")


if __name__ == "__main__":
    main()
