#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Qualify actual recipe emissions, current core consumers and real JNI in matched JVMs."""
from pathlib import Path
from hashlib import sha256
import argparse
import csv
import io
import json
import os
import re
import shutil
import statistics
import subprocess

HERE = Path(__file__).resolve().parent
MAIN = "m3/collections/src/main/java/com/m3/collections"
SELF = "m3/collections/src/test/java/com/m3/collections/M3IndexMaskCompactorSelfTest.java"
NATIVE = "m3/collections/src/main/native/collections/m3_index_mask_compactor.c"
SYMBOL = "Java_com_m3_collections_M3IndexMaskCompactor_selectNative"


def digest(path):
    return sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--target", type=Path, required=True)
    parser.add_argument("--sdk", type=Path, required=True)
    parser.add_argument("--work", type=Path, required=True)
    args = parser.parse_args()
    args.target, args.sdk, args.work, args.jdk = (path.resolve() for path in (args.target, args.sdk, args.work, args.jdk))
    args.work.mkdir(parents=True, exist_ok=True)
    env = os.environ.copy()
    env["JAVA_HOME"] = str(args.jdk)
    commands = []
    checks = {}

    def run(label, command, cwd=HERE):
        command = list(map(str, command))
        log = args.work / (label + ".log")
        with log.open("w") as output:
            completed = subprocess.run(command, cwd=cwd, env=env, stdout=output, stderr=subprocess.STDOUT)
        commands.append({"label": label, "argv": command, "exit_code": completed.returncode, "log_sha256": digest(log)})
        if completed.returncode != 0:
            raise RuntimeError(f"{label} failed with {completed.returncode}; see {log}")
        print(label + ": PASS", flush=True)
        return log.read_text()

    pins = json.loads(HERE.joinpath("SOURCE_PINS.json").read_text())
    for item in pins["sources"]:
        for label, key in (("before", "before_sha256"), ("after", "after_sha256")):
            emitted = args.sdk / label / item["path"]
            assert digest(emitted) == item[key], (label, item["path"])
        assert digest(args.target / item["path"]) == item["after_sha256"], item["path"]
    historical = args.target / "m3/collections/qualification/mask-selection-postimages-20261016.tsv"
    historical_rows = [line.split("\t") for line in historical.read_text().splitlines() if line and not line.startswith("#")]
    assert len(historical_rows) == 6
    for path, expected, stage in historical_rows:
        assert stage == "RECEIVER_CANDIDATE_NOT_INSTALLED_JDK"
        assert digest(args.sdk / "predecessor" / path) == expected, path
    assert (args.sdk / "before" / (MAIN + "/M3IndexMaskCompactor.java")).read_bytes() == (args.sdk / "after" / (MAIN + "/M3IndexMaskCompactor.java")).read_bytes()

    javac, java, jar = (args.jdk / "bin" / name for name in ("javac", "java", "jar"))
    compiler = shutil.which("gcc")
    assert compiler
    tool_versions = {"java": run("java-version", [java, "-version"]).strip(),
                     "gcc": run("gcc-version", [compiler, "--version"]).splitlines()[0]}
    production = sorted((args.target / MAIN).glob("*.java"))
    assert production
    native_flags = ["-std=c17", "-O2", "-g", "-Wall", "-Wextra", "-Wconversion", "-Wshadow", "-Werror", "-fPIC", "-shared",
                    "-I" + str(args.jdk / "include"), "-I" + str(args.jdk / "include/linux")]
    no_library = args.work / "missing-library"
    no_library.mkdir(exist_ok=True)
    assert not any(no_library.iterdir())
    no_symbol = args.work / "missing-symbol"
    no_symbol.mkdir(exist_ok=True)
    run("compile-native-missing-symbol-fixture", [compiler, *native_flags, HERE / "proof/NoNativeSymbol.c", "-o", no_symbol / "libm3_collections_mask.so"])
    native_artifacts, source_inputs = {}, {}

    for label in ("before", "after"):
        folder = args.work / label
        classes, headers, native = (folder / name for name in ("classes", "headers", "native"))
        for path in (classes, headers, native): path.mkdir(parents=True, exist_ok=True)
        sources = []
        for current in production:
            relative = current.relative_to(args.target)
            emitted = args.sdk / label / relative
            sources.append(emitted if emitted.exists() else current)
        source_inputs[label] = [{"path": str(path), "sha256": digest(path)} for path in sources]
        arguments = folder / "sources.args"
        arguments.write_text("".join('"' + str(path).replace('\\', '\\\\').replace('"', '\\"') + '"\n' for path in sources))
        run(label + "-core-javac", [javac, "-J-Xmx512m", "--release", "21", "-proc:none", "-Xlint:all", "-Werror", "-h", headers, "-d", classes, "@" + str(arguments)])
        assert SYMBOL in headers.joinpath("com_m3_collections_M3IndexMaskCompactor.h").read_text()
        run(label + "-contract-javac", [javac, "-J-Xmx384m", "--release", "21", "-proc:none", "-Xlint:all", "-Werror", "-cp", classes,
                                            "-d", classes, args.sdk / "after" / SELF])
        library = native / "libm3_collections_mask.so"
        run(label + "-native-c17", [compiler, *native_flags, "-fstack-usage", args.sdk / label / NATIVE, "-o", library])
        symbols = run(label + "-native-symbols", ["nm", "-D", "--defined-only", library])
        assert {line.split()[-1] for line in symbols.splitlines() if "Java_" in line} == {SYMBOL}
        stack_files = sorted(native.glob("*.su"))
        assert stack_files
        native_artifacts[label] = {"sha256": digest(library), "jni_symbol": SYMBOL, "stack_usage": {path.name: path.read_text() for path in stack_files}}
        runtime = [java, "-Xmx384m", "-Xverify:all", "-Xcheck:jni", "-cp", classes]
        for mode, directory, option in (("java", no_library, None), ("native", native, "--native"),
                                        ("missing-native", no_library, "--native-missing"), ("missing-symbol", no_symbol, "--native-symbol-missing")):
            output = run(label + "-" + mode, [*runtime, "-Djava.library.path=" + str(directory), "com.m3.collections.M3IndexMaskCompactorSelfTest", *([] if option is None else [option])])
            assert "PASS M3IndexMaskCompactorSelfTest" in output
            checks[label + "-" + mode] = int(re.search(r"checks=(\d+)", output).group(1))

    sanitized = args.work / "after/native-ubsan"
    sanitized.mkdir(parents=True, exist_ok=True)
    run("after-native-ubsan-c17", [compiler, *native_flags, "-fsanitize=undefined", "-fno-sanitize-recover=all", args.sdk / "after" / NATIVE,
                                   "-o", sanitized / "libm3_collections_mask.so"])
    run("after-native-ubsan-runtime", [java, "-Xmx384m", "-Xverify:all", "-Xcheck:jni", "-Djava.library.path=" + str(sanitized), "-cp",
                                       args.work / "after/classes", "com.m3.collections.M3IndexMaskCompactorSelfTest", "--native"])

    proof_classes = args.work / "cost/classes"
    proof_classes.mkdir(parents=True, exist_ok=True)
    run("cost-probe-javac", [javac, "-J-Xmx384m", "--release", "21", "-proc:none", "-Xlint:all", "-Werror", "-cp", args.work / "after/classes",
                              "-d", proof_classes, HERE / "proof/SizeAgent.java", HERE / "proof/MaskZeroCostProbe.java"])
    agent_manifest = args.work / "cost/agent.mf"
    agent_manifest.write_text("Premain-Class: SizeAgent\n\n")
    agent = args.work / "cost/size-agent.jar"
    run("size-agent-jar", [jar, "--create", "--file", agent, "--manifest", agent_manifest, "-C", proof_classes, "SizeAgent.class"])
    measurements = {}
    for label in ("before", "after"):
        for mode in ("jit", "interpreter"):
            classpath = os.pathsep.join((str(args.work / label / "classes"), str(proof_classes)))
            output = run("cost-" + label + "-" + mode, [java, "-Xms64m", "-Xmx384m", *( ["-Xint"] if mode == "interpreter" else []),
                         "-XX:+UnlockDiagnosticVMOptions", "-XX:NativeMemoryTracking=summary", "-XX:+PrintNMTStatistics", "-javaagent:" + str(agent),
                         "-Djava.library.path=" + str(args.work / label / "native"), "-cp", classpath, "MaskZeroCostProbe", "--native",
                         *(["--interpreted"] if mode == "interpreter" else [])])
            lines = [line for line in output.splitlines() if line.startswith("operation,") or re.match(r"[a-zA-Z]+,\d+,", line)]
            data = list(csv.DictReader(io.StringIO("\n".join(lines))))
            assert len(data) == 90
            for row in data:
                assert row["retained_bytes_before"] == row["retained_bytes_after"]
            nmt = re.search(r"Total: reserved=(\d+), committed=(\d+)", output)
            assert nmt, "NMT total missing"
            args.work.joinpath("cost-" + label + "-" + mode + ".csv").write_text("\n".join(lines) + "\n")
            metrics = []
            for operation, length in sorted({(row["operation"], int(row["length"])) for row in data}):
                selected = [row for row in data if row["operation"] == operation and int(row["length"]) == length]
                if operation in ("selectedCount", "listRetain", "dequeRetain"):
                    assert statistics.median(int(row["allocated_bytes"]) for row in selected) == 0
                metrics.append({"operation": operation, "length": length,
                                "median_cpu_ns_per_operation": statistics.median(int(row["cpu_ns"]) / int(row["iterations"]) for row in selected),
                                "median_wall_ns_per_operation": statistics.median(int(row["wall_ns"]) / int(row["iterations"]) for row in selected),
                                "allocated_bytes_per_operation": sorted({int(row["allocated_bytes"]) / int(row["iterations"]) for row in selected}),
                                "retained_owner_and_arrays_bytes": sorted({int(row["retained_bytes_after"]) for row in selected}),
                                "observed_rss_kb": [min(int(row["rss_kb_after"]) for row in selected), max(int(row["rss_kb_after"]) for row in selected)]})
            measurements[label + "-" + mode] = {"cases": len(data), "nmt_reserved_bytes": int(nmt.group(1)), "nmt_committed_bytes": int(nmt.group(2)), "metrics": metrics}

    result = {"schema": "M3_MASK_ZERO_QUALIFICATION_V1", "status": "PASSED", "target_preimage_revision": pins["target_revision"],
              "selector_retained_upstream_commit": pins["selector_upstream_commit"], "current_java_selector_unchanged": True,
              "historical_receiving_postimages_verified": 6, "current_sealed_owners": 6, "strict_core_sources_each_variant": len(production),
              "contract_checks": checks, "actual_native_ubsan": "PASSED", "explicit_native_missing_library_and_symbol": "REFUSED_AS_REQUIRED",
              "native_artifacts": native_artifacts, "finite_cost_measurements": measurements, "tool_versions": tool_versions,
              "source_inputs": source_inputs, "commands": commands,
              "limits": ["Linux x86_64 / stock Java21 / GCC only", "matched finite resource probe, not a JMH or universal throughput claim",
                         "NMT and RSS are whole JVM/process observations, not attribution of every native byte to one kernel",
                         "retained byte measurement covers the existing list/deque owner objects and backing arrays; caller inputs and explicit detached outputs are separate",
                         "all allocation samples are retained; median count/retain allocation is checked, allowing visible one-time JVM/probe allocation outliers",
                         "unchanged nonzero selector behavior comes from current upstream; concurrent input mutation remains unsupported",
                         "owning Maven custom source artifact defaults, full JDK image, jtreg/JCK and hosted CI are separate gates"]}
    args.work.joinpath("RESULTS.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({"status": result["status"], "strict_core_sources_each_variant": len(production), "contract_checks": checks, "finite_cost_cases": 360}))


if __name__ == "__main__":
    main()
