#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Prepare frozen Java snapshot probes, then run at most three sequential matched fork pairs.

Reuses the source seals, ThreadMXBean workload and alternating fork order from
run-linked-resource-probes.py. The prepare command compiles without running workloads.
The run command uses only the sealed class snapshots and refuses an existing result set.
"""
import argparse
import csv
import datetime
from fractions import Fraction
import hashlib
import io
import json
import os
from pathlib import Path
import platform
import statistics
import subprocess
import sys


PROBE = "M3PackedSnapshotResourceProbe"
DRIVER = "run-packed-snapshot-resource-probes.py.txt"
VM_FLAGS = ["-Xms256m", "-Xmx256m", "-XX:+UseSerialGC", "-ea"]
VARIANTS = ("baseline", "candidate")
CASES = {(operation, size, selection) for operation in ("filtered", "filteredSorted")
         for size in (64, 1025, 65536) for selection in ("none", "half", "all")}


def seal(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1 << 20), b""):
            digest.update(block)
    return {"path": str(path), "bytes": path.stat().st_size, "sha256": digest.hexdigest()}


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2) + "\n")


def same_files(items):
    for expected in items:
        if seal(Path(expected["path"])) != expected:
            raise SystemExit("sealed file changed: " + expected["path"])


def command(log_dir, record, label, argv):
    argv = [str(value) for value in argv]
    print(label, flush=True)
    try:
        result = subprocess.run(argv, text=True, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, timeout=60, check=False)
    except subprocess.TimeoutExpired as failure:
        text = failure.stdout or b""
        text = text.decode(errors="replace") if isinstance(text, bytes) else text
        (log_dir / (label + ".log")).write_text(json.dumps(argv) + "\n" + text + "\nTIMEOUT 60 seconds\n")
        raise SystemExit("command timed out: " + label) from failure
    log = log_dir / (label + ".log")
    log.write_text(json.dumps(argv) + "\n" + result.stdout + "\nexit_code=" + str(result.returncode) + "\n")
    record["commands"].append({"argv": argv, "exit_code": result.returncode, "log": seal(log)})
    if result.returncode:
        raise SystemExit("command failed: " + label + "; see " + str(log))
    return result.stdout


def lint(path):
    text = path.read_bytes().decode("utf-8")
    if "\r" in text or "\0" in text or not text.endswith("\n"):
        raise SystemExit("encoding/newline lint: " + str(path))
    if any(line != line.rstrip() for line in text.splitlines()):
        raise SystemExit("trailing whitespace: " + str(path))
    if "SPDX-License-Identifier: Apache-2.0" not in text:
        raise SystemExit("missing canonical license header: " + str(path))


def prepare(args):
    output = args.output.resolve()
    inputs = {name: getattr(args, name).resolve() for name in (
        "java_home", "templates", "baseline_classes", "candidate_classes", "baseline_source", "candidate_source")}
    if output.exists():
        raise SystemExit("output must be absent; preserve earlier qualification records")
    for path in inputs.values():
        if not path.exists() or output == path or path in output.parents or output in path.parents:
            raise SystemExit("missing or overlapping input: " + str(path))
    templates = inputs["templates"]
    driver, source = templates / DRIVER, templates / (PROBE + ".java.txt")
    if driver.read_bytes() != Path(__file__).read_bytes():
        raise SystemExit("runner differs from its canonical template")
    lint(driver)
    lint(source)
    compile(driver.read_text(), str(driver), "exec")
    for variant in VARIANTS:
        expected = getattr(args, variant + "_sha256")
        if len(expected) != 64 or any(char not in "0123456789abcdef" for char in expected):
            raise SystemExit("explicit lowercase SHA256 source pin required")
        if seal(inputs[variant + "_source"])["sha256"] != expected:
            raise SystemExit("snapshot source pin differs: " + variant)
    output.mkdir(parents=True)
    record = {"schema": "synexia.m3.packed-snapshot-resources.v1", "stage": "preparing",
              "created_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
              "python": sys.version, "platform": platform.platform(), "argv": sys.argv,
              "driver": seal(driver), "probe_template": seal(source), "vm_flags": VM_FLAGS,
              "paired_forks": args.forks, "commands": [], "variants": {}, "frozen_files": [],
              "input_sources": [], "input_classes": {}, "toolchain": []}
    java_home = inputs["java_home"]
    for path in (java_home / "bin/java", java_home / "bin/javac", java_home / "release",
                 java_home / "lib/modules", java_home / "lib/server/libjvm.so"):
        record["toolchain"].append(seal(path))
    record["java"] = str(java_home / "bin/java")
    version = command(output, record, "00-java-version", [record["java"], "-version"])
    if 'version "21.' not in version and 'version "21"' not in version:
        raise SystemExit("this bounded qualification requires an actual JDK21 runtime")
    probe_source = output / (PROBE + ".java")
    probe_source.write_bytes(source.read_bytes())
    record["frozen_files"].append(seal(probe_source))
    probe_maps = {}
    for variant in VARIANTS:
        classes = inputs[variant + "_classes"]
        copied = output / (variant + "-classes")
        original = sorted(classes.rglob("*.class"))
        if not (classes / "com/m3/collections/M3IndexedListSnapshot.class").is_file():
            raise SystemExit("actual compiled snapshot owner missing: " + variant)
        record["input_classes"][variant] = []
        for path in original:
            if path.is_symlink():
                raise SystemExit("class symlink refused: " + str(path))
            before = seal(path)
            target = copied / path.relative_to(classes)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(path.read_bytes())
            if seal(target)["sha256"] != before["sha256"]:
                raise SystemExit("class changed during snapshot: " + str(path))
            record["input_classes"][variant].append(before)
            record["frozen_files"].append(seal(target))
        if sorted(classes.rglob("*.class")) != original:
            raise SystemExit("class inventory changed during snapshot: " + variant)
        same_files(record["input_classes"][variant])
        snapshot_source = inputs[variant + "_source"]
        for path in (snapshot_source, snapshot_source.with_name("M3IndexMaskCompactor.java")):
            before = seal(path)
            if path == snapshot_source and before["sha256"] != getattr(args, variant + "_sha256"):
                raise SystemExit("snapshot source pin changed during preparation: " + variant)
            target = output / (variant + "-sources") / path.name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(path.read_bytes())
            if seal(target)["sha256"] != before["sha256"]:
                raise SystemExit("source changed during snapshot: " + str(path))
            record["input_sources"].append(before)
            record["frozen_files"].append(seal(target))
        probes = output / (variant + "-probe-classes")
        probes.mkdir()
        command(output, record, "01-compile-" + variant, [java_home / "bin/javac", "--release", "21",
                "-Xlint:all", "-Werror", "-cp", copied, "-d", probes, probe_source])
        probe_maps[variant] = {str(path.relative_to(probes)): seal(path)["sha256"]
                               for path in sorted(probes.rglob("*.class"))}
        record["frozen_files"].extend(seal(path) for path in sorted(probes.rglob("*.class")))
        record["variants"][variant] = {"classes": str(copied), "input_class_count": len(original),
                                        "snapshot_source": seal(snapshot_source)}
    if probe_maps["baseline"] != probe_maps["candidate"]:
        raise SystemExit("probe bytecode differs between baseline and candidate compilation")
    record["probe_classes"] = str(output / "baseline-probe-classes")
    record["probe_bytecode_identical"] = True
    record["empty_native"] = str(output / "empty-native")
    Path(record["empty_native"]).mkdir()
    same_files(record["input_sources"] + record["frozen_files"] + record["toolchain"])
    record["stage"] = "prepared_only_no_workloads_run"
    manifest = output / "PREPARED.json"
    write_json(manifest, record)
    (output / "PREPARED.sha256").write_text(seal(manifest)["sha256"] + "\n")
    print("PREPARED strict compilation passed; no timed workload executed", flush=True)
    print(str(manifest), flush=True)


def per_operation(values, key):
    data = [Fraction(row[key], row["measuredRounds"]) for row in values]
    return {"min": str(min(data)), "median": str(statistics.median(data)), "max": str(max(data))}


def report(rows, summary, forks):
    lines = ["# Packed snapshot resource observations", "",
        "This bounded Java21 sampler uses " + str(forks) + " sequential matched fork pairs.",
        "Each JVM runs the same 18 warmed cases. Payloads, slice offset 7, predicates, comparator,",
        "probe bytecode, JVM flags and round counts match across the frozen implementations.",
        "ThreadMXBean reports application-thread allocated bytes and CPU time; this is not a",
        "retained-heap measurement, whole-process CPU measurement or a universal performance claim.", "",
        "Each operation includes volatile result publication and three allocation-free physical",
        "position/reference observations. Input construction, predicate construction, independent",
        "stable-sort oracle and full result/source validation occur outside measurement.",
        "Counter-call overhead remains included; no estimated overhead has been subtracted.", "",
        "Allocation cells give median [minimum, maximum] exact average bytes per operation.",
        "CPU cells give median [minimum, maximum] nanoseconds per operation, rounded for display.",
        "Raw integer totals and round counts preserve exact per-operation ratios in raw.tsv/json.", "",
        "| Operation | Size | Selection | Baseline bytes/op | Candidate bytes/op | Allocation delta | Baseline CPU ns/op | Candidate CPU ns/op |",
        "|---|---:|---|---:|---:|---:|---:|---:|"]
    for operation, size, selection in sorted(CASES):
        pair = {item["variant"]: item for item in summary if
                (item["operation"], item["size"], item["selection"]) == (operation, size, selection)}
        base, candidate = pair["baseline"], pair["candidate"]
        def cell(item, key, decimal=False):
            value = item[key]
            render = (lambda text: format(float(Fraction(text)), ".1f")) if decimal else str
            return render(value["median"]) + " [" + render(value["min"]) + ", " + render(value["max"]) + "]"
        delta = Fraction(candidate["allocatedBytesPerOperation"]["median"]) - Fraction(base["allocatedBytesPerOperation"]["median"])
        lines.append("| " + " | ".join((operation, str(size), selection,
            cell(base, "allocatedBytesPerOperation"), cell(candidate, "allocatedBytesPerOperation"), str(delta),
            cell(base, "cpuNanosPerOperation", True), cell(candidate, "cpuNanosPerOperation", True))) + " |")
    lines += ["", "The fixed suite order shares JIT history within each JVM. Fresh suite forks do not",
              "establish a steady-state or independent pristine-JVM result for every case. Allocation",
              "counts are VM-specific; CPU/wall variation can include compilation, scheduling and GC effects.",
              "The snapshot and mask-compactor source pins, complete copied class inventories, toolchain",
              "hashes, raw logs and exact commands are preserved in PREPARED.json and RESULTS.json.", ""]
    return "\n".join(lines)


def run_prepared(args):
    prepared = args.prepared.resolve()
    manifest = prepared / "PREPARED.json"
    if seal(manifest)["sha256"] != (prepared / "PREPARED.sha256").read_text().strip():
        raise SystemExit("prepared manifest seal differs")
    frozen = json.loads(manifest.read_text())
    if frozen["stage"] != "prepared_only_no_workloads_run" or not 1 <= frozen["paired_forks"] <= 3:
        raise SystemExit("unexpected preparation state or fork bound")
    if seal(Path(__file__).resolve())["sha256"] != frozen["driver"]["sha256"]:
        raise SystemExit("runner changed since preparation")
    same_files(frozen["frozen_files"] + frozen["toolchain"] + frozen["input_sources"])
    output = prepared / "results"
    output.mkdir(exist_ok=False)
    record = {"schema": frozen["schema"], "prepared_manifest": seal(manifest), "commands": [],
              "started_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(), "stage": "running"}
    rows = []
    for fork in range(1, frozen["paired_forks"] + 1):
        order = VARIANTS if fork & 1 else tuple(reversed(VARIANTS))
        for variant in order:
            raw = command(output, record, variant + "-fork" + str(fork), [frozen["java"], *frozen["vm_flags"],
                "-Djava.library.path=" + frozen["empty_native"], "-cp", os.pathsep.join((frozen["probe_classes"],
                frozen["variants"][variant]["classes"])), "com.m3.collections." + PROBE, "suite"])
            current = list(csv.DictReader(io.StringIO("\n".join(line for line in raw.splitlines()
                                                                  if not line.startswith("#"))), delimiter="\t"))
            parsed = [{key: value if key in ("operation", "selection") else int(value)
                       for key, value in row.items()} for row in current]
            if len(parsed) != 18 or {(row["operation"], row["size"], row["selection"]) for row in parsed} != CASES:
                raise SystemExit("unexpected resource case matrix")
            for row in parsed:
                if row["measuredRounds"] <= 0 or min(row[key] for key in ("cpuNanos", "elapsedNanos", "allocatedBytes")) < 0:
                    raise SystemExit("unavailable/invalid resource counter")
                row.update(variant=variant, fork=fork)
                rows.append(row)
            write_json(output / "RESULTS.json", record)
    summary = []
    for operation, size, selection in sorted(CASES):
        matched = [row for row in rows if (row["operation"], row["size"], row["selection"]) == (operation, size, selection)]
        if len({(row["checksum"], row["selected"], row["warmupRounds"], row["measuredRounds"]) for row in matched}) != 1:
            raise SystemExit("matched result observations or round counts differ")
        for variant in VARIANTS:
            selected = [row for row in matched if row["variant"] == variant]
            summary.append({"operation": operation, "size": size, "selection": selection,
                            "variant": variant, "forks": len(selected), "measuredRounds": selected[0]["measuredRounds"],
                            **{key + "PerOperation": per_operation(selected, key)
                               for key in ("allocatedBytes", "cpuNanos", "elapsedNanos")}})
    same_files(frozen["frozen_files"] + frozen["toolchain"] + frozen["input_sources"])
    write_json(output / "raw.json", rows)
    write_json(output / "summary.json", summary)
    with (output / "raw.tsv").open("w", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]), delimiter="\t")
        writer.writeheader()
        writer.writerows(rows)
    (output / "RESOURCE_RESULTS.md").write_text(report(rows, summary, frozen["paired_forks"]))
    record.update(stage="passed", frozen_bytes_unchanged=True,
                  finished_utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                  artifacts=[seal(output / name) for name in ("raw.json", "raw.tsv", "summary.json", "RESOURCE_RESULTS.md")])
    write_json(output / "RESULTS.json", record)
    (output / "RESULTS.sha256").write_text(seal(output / "RESULTS.json")["sha256"] + "\n")
    print("PASS " + str(len(rows)) + " resource samples; matched observations and sealed inputs unchanged", flush=True)
    print(str(output / "RESOURCE_RESULTS.md"), flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    actions = parser.add_subparsers(dest="action", required=True)
    preparation = actions.add_parser("prepare", help="freeze sources/classes and strict-compile; no workload execution")
    for option in ("java-home", "templates", "baseline-classes", "candidate-classes", "baseline-source", "candidate-source", "output"):
        preparation.add_argument("--" + option, type=Path, required=True)
    for variant in VARIANTS:
        preparation.add_argument("--" + variant + "-sha256", required=True)
    preparation.add_argument("--forks", type=int, choices=(1, 2, 3), default=3)
    execution = actions.add_parser("run", help="execute the previously frozen workload without recompiling")
    execution.add_argument("--prepared", type=Path, required=True)
    args = parser.parse_args()
    prepare(args) if args.action == "prepare" else run_prepared(args)


if __name__ == "__main__":
    main()
