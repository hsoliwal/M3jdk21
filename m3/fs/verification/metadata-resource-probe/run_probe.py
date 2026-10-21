#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Compile exact inputs and compare two finite, separately launched Java 21 probes."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import statistics
import subprocess
import sys
import time


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def source_receipt(path):
    data = path.read_bytes()
    return {"path": str(path.resolve()), "bytes": len(data), "sha256": hashlib.sha256(data).hexdigest(),
            "git_blob": hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()}


def run(command, stdout, stderr):
    with stdout.open("w") as out, stderr.open("w") as err:
        subprocess.run([str(part) for part in command], stdout=out, stderr=err, check=True)


def run_measured(command, stdout, stderr, process_log):
    with stdout.open("w") as out, stderr.open("w") as err:
        with subprocess.Popen([str(part) for part in command], stdout=out, stderr=err) as process:
            _, status, usage = os.wait4(process.pid, 0)
            process.returncode = os.waitstatus_to_exitcode(status)
            values = {"source": "Linux wait4 child-specific resource usage", "exit_code": process.returncode,
                      "user_cpu_seconds": usage.ru_utime, "system_cpu_seconds": usage.ru_stime,
                      "maximum_rss_kib": usage.ru_maxrss, "minor_page_faults": usage.ru_minflt,
                      "major_page_faults": usage.ru_majflt, "voluntary_context_switches": usage.ru_nvcsw,
                      "involuntary_context_switches": usage.ru_nivcsw}
            process_log.write_text(json.dumps(values, indent=2) + "\n")
            if process.returncode != 0:
                raise subprocess.CalledProcessError(process.returncode, command)
            return values


def parse_samples(log):
    columns = None
    result = {"samples": [], "canonical": [], "environment": {}, "nmt_total_lines": []}
    for line in log.read_text().splitlines():
        fields = line.split("\t")
        if fields[0] == "COLUMNS":
            columns = fields[1:]
        elif fields[0] == "SAMPLE":
            if columns is None:
                raise ValueError("sample before column schema")
            result["samples"].append(dict(zip(columns, map(int, fields[1:]), strict=True)))
        elif fields[0] == "CANONICAL":
            result["canonical"].append({"repetition": int(fields[1]), "bytes": int(fields[2]), "sha256": fields[3]})
        elif fields[0] == "ENV":
            result["environment"][fields[1]] = fields[2]
        elif fields[0] == "INPUT":
            result["input_rows"], result["input_sha256"] = int(fields[1]), fields[2]
        elif line.lstrip().startswith(("Total: reserved=", "malloc:", "mmap:")):
            result["nmt_total_lines"].append(line.strip())
    if not result["samples"]:
        raise ValueError("probe produced no samples")
    result["summary"] = {}
    for name in ("constructor_cpu_ns", "constructor_allocated_bytes", "constructor_wall_ns",
                 "heap_delta_bytes", "vm_rss_kib", "vm_hwm_kib", "rss_anon_kib", "rss_file_kib"):
        values = [sample[name] for sample in result["samples"]]
        result["summary"][name] = {"minimum": min(values), "median": statistics.median(values), "maximum": max(values)}
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--donor", type=Path, required=True, help="exact pinned synexia-indexstring checkout")
    parser.add_argument("--receiver", type=Path, action="append", required=True,
                        help="receiver com/m3/fs source directory; repeat for split staging")
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--rows", type=int, default=50_000)
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    work = args.out / "classes"
    work.mkdir(exist_ok=True)
    here = Path(__file__).resolve().parent
    harness = here / "MetadataResourceProbe.java"
    java, javac = args.jdk / "bin/java", args.jdk / "bin/javac"
    donor_sources = [args.donor / "src/main/java/com/synexia/indexstring/fs" / name
                     for name in ("M3ContentId.java", "M3GenerationRoot.java")]
    receiver_sources = []
    for name in ("M3ContentId.java", "M3GenerationRoot.java", "M3IdTable.java", "M3Columns.java"):
        candidates = [directory / name for directory in args.receiver if (directory / name).is_file()]
        if len(candidates) != 1:
            raise ValueError(f"expected exactly one receiver source for {name}, found {candidates}")
        receiver_sources.append(candidates[0])
    # These are authoritative blobs of the donor tree, not expected hashes calculated from this run.
    pinned_blobs = {
        "M3ContentId.java": "b8dffdfaf6809e130b7c95ae38c79cb2beb937c6",
        "M3GenerationRoot.java": "394ef480d165d9c48b5e4f6d9ca6df467147e6c4",
    }
    for path in donor_sources:
        if source_receipt(path)["git_blob"] != pinned_blobs[path.name]:
            raise ValueError(f"untouched pinned donor verification failed: {path}")

    flags = ["-XX:+UseSerialGC", "-Xms32m", "-Xmx512m", "-XX:NativeMemoryTracking=summary",
             "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintNMTStatistics"]
    receipt = {
        "schema": 1, "created_utc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "donor_repository": "hsoliwal/com.synexia",
        "donor_commit": "289e32f130c9c8e4a1cf0edc1066327d699393bf",
        "rows": args.rows, "warmups": 3, "measured_repetitions": 3,
        "jvm_flags": flags, "os": platform.platform(), "cpu_count": os.cpu_count(),
        "jdk_release": (args.jdk / "release").read_text(),
        "java_sha256": sha(java), "javac_sha256": sha(javac),
        "probe_sources": [source_receipt(harness), source_receipt(Path(__file__))],
        "limitations": [
            "Finite construction workload only; no timing assertion and no general performance guarantee.",
            "Each mode runs in one fresh, separate JVM; three warmups and three measured constructions.",
            "The same deterministic prepared input shape is retained for all samples in each mode.",
            "Input map, strings and public ID objects are prepared outside measured intervals and kept reachable.",
            "Heap delta is incremental owner retention after three requested full GCs, against that prepared input.",
            "Heap used sums heap-pool collection-usage snapshots; separate live Runtime heap counters are also recorded.",
            "Donor can share input ID objects; receiver copies IDs into packed bytes. This is part of this workload.",
            "CPU and allocation cover the current calling thread including reflective constructor invocation, not all JVM threads.",
            "GC, JIT, OS scheduling and shared-host noise affect results; no CPU affinity was imposed.",
            "RSS and peak RSS describe the complete process, including heap, code, libraries and native bookkeeping.",
            "NMT committed/reserved virtual memory is not resident native memory and does not isolate M3FS allocations.",
            "No native filesystem operations, JDBC, Eclipse workload or API enumeration is measured.",
        ], "modes": {},
    }
    for mode, package, sources in (("donor", "com.synexia.indexstring.fs", donor_sources),
                                    ("receiver", "com.m3.fs", receiver_sources)):
        classes = work / mode
        classes.mkdir(exist_ok=True)
        compile_command = [javac, "-proc:none", "-Xlint:all", "-Werror", "-d", classes, harness, *sources]
        run(compile_command, args.out / f"{mode}-javac.stdout.log", args.out / f"{mode}-javac.stderr.log")
        jvm = [java, *flags, "-cp", classes, "MetadataResourceProbe", package, str(args.rows), "3", "3"]
        command = jvm
        output, errors = args.out / f"{mode}.stdout.log", args.out / f"{mode}.stderr.log"
        process_usage = run_measured(command, output, errors, args.out / f"{mode}-process-usage.json")
        data = parse_samples(output)
        if len(data["samples"]) != 3 or len(data["canonical"]) != 3:
            raise ValueError("incomplete sample or canonical evidence")
        data.update({"sources": [source_receipt(path) for path in sources],
                     "classes": [source_receipt(path) for path in sorted(classes.rglob("*.class"))],
                     "compile_command": [str(part) for part in compile_command],
                     "run_command": [str(part) for part in command],
                     "process_usage": process_usage})
        receipt["modes"][mode] = data
    donor, receiver = receipt["modes"]["donor"], receipt["modes"]["receiver"]
    if donor["input_sha256"] != receiver["input_sha256"] or donor["canonical"] != receiver["canonical"]:
        raise AssertionError("donor/receiver input or canonical byte identity mismatch")
    receipt["canonical_identity_verified"] = True
    (args.out / "resource-measurements.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({"rows": args.rows, "canonical_identity_verified": True,
                      "donor": donor["summary"], "receiver": receiver["summary"]}, indent=2))


if __name__ == "__main__":
    main()
