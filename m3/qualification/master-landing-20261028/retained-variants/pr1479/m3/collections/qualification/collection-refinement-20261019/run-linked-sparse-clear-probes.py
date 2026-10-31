#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Pin and qualify the focused flat-list bounded-clear candidate in one declared epoch."""
import argparse
import datetime
import hashlib
import json
from pathlib import Path
import platform
import statistics
import subprocess
import sys

SOURCE_NAMES = ("M3LinkedListClearRangeProof.java", "M3LinkedListSparseClearProbe.java")
CLASS_PATTERNS = ("M3LinkedList*.class", "M3SlotArrays*.class", "M3SlotSpliterators*.class")
VM_FLAGS = ("-Xms256m", "-Xmx256m", "-XX:+UseSerialGC")


def seal(path):
    digest = hashlib.sha256()
    size = 0
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1 << 20), b""):
            digest.update(block)
            size += len(block)
    return {"path": str(path), "bytes": size, "sha256": digest.hexdigest()}


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2) + "\n")


def refill_list(value):
    try:
        counts = [int(token) for token in value.split(",")]
    except ValueError as failure:
        raise argparse.ArgumentTypeError("refills must be comma-separated integers") from failure
    if not counts or any(count <= 0 or count > 65536 for count in counts):
        raise argparse.ArgumentTypeError("refills must be between 1 and 65536")
    if len(counts) != len(set(counts)):
        raise argparse.ArgumentTypeError("refills must not repeat")
    return counts


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("java-home", "baseline-classes", "candidate-classes", "baseline-source",
                 "candidate-source", "sources", "templates", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    parser.add_argument("--baseline-sha256", required=True)
    parser.add_argument("--candidate-sha256", required=True)
    parser.add_argument("--epoch-label", required=True)
    parser.add_argument("--forks", type=int, choices=(1, 2, 3), default=3)
    parser.add_argument("--refills", type=refill_list,
                        default=[1, 64, 512, 2048, 4096, 16384],
                        help="comma-separated sparse controls and dense fallback refill sizes")
    args = parser.parse_args()
    output = args.output.resolve()
    if output.exists():
        raise SystemExit("output must be new; do not overwrite historical qualification")
    inputs = {name: getattr(args, name.replace("-", "_")).resolve()
              for name in ("java-home", "baseline-classes", "candidate-classes", "baseline-source",
                           "candidate-source", "sources", "templates")}
    for variant in ("baseline", "candidate"):
        observed = seal(inputs[variant + "-source"])
        if observed["sha256"] != getattr(args, variant + "_sha256"):
            raise SystemExit("source pin mismatch: " + variant)
    canonical_owner = inputs["templates"] / "M3LinkedList.java.txt"
    if canonical_owner.read_bytes() != inputs["candidate-source"].read_bytes():
        raise SystemExit("active canonical owner differs from candidate source")
    for name in ("baseline-classes", "candidate-classes", "sources", "templates"):
        if inputs[name] == output or inputs[name] in output.parents:
            raise SystemExit("output must be disjoint from class/source/template inputs")
    sources = [inputs["sources"] / name for name in SOURCE_NAMES]
    for source in sources:
        if not source.is_file():
            raise SystemExit("missing fixture: " + str(source))
    output.mkdir(parents=True)
    metadata = output / "PROVENANCE.json"
    record = {
        "schema": "synexia.m3.linked-sparse-clear.v1",
        "epoch": args.epoch_label,
        "started_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "argv": sys.argv, "python": sys.version, "platform": platform.platform(),
        "driver": seal(Path(__file__).resolve()),
        "baseline_source": seal(inputs["baseline-source"]),
        "candidate_source": seal(inputs["candidate-source"]),
        "canonical_candidate": seal(canonical_owner),
        "fixtures": [seal(path) for path in sources],
        "vm_flags": list(VM_FLAGS), "refills": args.refills,
        "commands": [], "stage": "inventory",
        "limitations": ["focused retained-peak clear/refill workload",
                        "initial one-round samples are not pristine-JVM claims",
                        "three suite forks do not establish universal speedup",
                        "no allocation-failure injection or full product/platform qualification"],
    }
    write_json(metadata, record)

    def run(name, command):
        print(name, flush=True)
        log = output / (name + ".log")
        command = [str(value) for value in command]
        try:
            result = subprocess.run(command, text=True, stdout=subprocess.PIPE,
                                    stderr=subprocess.STDOUT, timeout=60, check=False)
        except subprocess.TimeoutExpired as failure:
            partial = failure.stdout or ""
            if isinstance(partial, bytes):
                partial = partial.decode(errors="replace")
            log.write_text("$ " + json.dumps(command) + "\n" + partial + "\ntimeout_seconds=60\n")
            record["commands"].append({"name": name, "argv": command,
                                       "timeout_seconds": 60, "log": seal(log)})
            record["stage"] = "timeout"
            write_json(metadata, record)
            raise SystemExit("gate timed out: " + str(log)) from failure
        log.write_text("$ " + json.dumps(command) + "\n" + result.stdout
                       + "\nexit_code=" + str(result.returncode) + "\n")
        record["commands"].append({"name": name, "argv": command,
                                   "exit_code": result.returncode, "log": seal(log)})
        write_json(metadata, record)
        if result.returncode:
            raise SystemExit("gate failed: " + str(log))
        return result.stdout

    lint = []
    for source in sources:
        content = source.read_bytes()
        if b"\r" in content or b"\0" in content or not content.endswith(b"\n"):
            raise SystemExit("fixture encoding lint: " + source.name)
        if any(line != line.rstrip() for line in content.decode().splitlines()):
            raise SystemExit("fixture whitespace lint: " + source.name)
        if (inputs["templates"] / (source.name + ".txt")).read_bytes() != content:
            raise SystemExit("fixture canonical byte mismatch: " + source.name)
        lint.append("PASS source lint and canonical identity: " + source.name)
    compile(Path(__file__).read_text(), str(Path(__file__)), "exec")
    if (inputs["templates"] / "run-linked-sparse-clear-probes.py.txt").read_bytes() != Path(__file__).read_bytes():
        raise SystemExit("driver canonical byte mismatch")
    lint.append("PASS driver syntax and canonical identity")
    (output / "00-lint.log").write_text("\n".join(lint) + "\n")
    record["lint"] = seal(output / "00-lint.log")
    record["stage"] = "lint_passed"

    snapshots = {}
    record["input_classes"] = {}
    for variant in ("baseline", "candidate"):
        source_root = inputs[variant + "-classes"]
        package = source_root / "com/m3/collections"
        selected = sorted({path for pattern in CLASS_PATTERNS for path in package.glob(pattern)})
        if not any(path.name == "M3LinkedList.class" for path in selected):
            raise SystemExit("missing compiled owner: " + variant)
        target_root = output / (variant + "-classes")
        for source in selected:
            target = target_root / source.relative_to(source_root)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(source.read_bytes())
        snapshots[variant] = target_root
        record["input_classes"][variant] = [seal(path) for path in selected]
        record.setdefault("snapshot_classes", {})[variant] = [
            seal(target_root / path.relative_to(source_root)) for path in selected]
    java_home = inputs["java-home"]
    java = java_home / "bin/java"
    javac = java_home / "bin/javac"
    record["toolchain"] = [seal(path) for path in (
        java, javac, java_home / "release", java_home / "lib/modules",
        java_home / "lib/server/libjvm.so")]
    write_json(metadata, record)
    run("01-java-version", [java, "-version"])
    run("02-jvm-flags", [java, *VM_FLAGS, "-XX:+PrintFlagsFinal", "-version"])
    classes = output / "probe-classes"
    classes.mkdir()
    run("03-probe-compile", [javac, "--release", "21", "-Xlint:all,-module", "-Werror",
                            "-cp", snapshots["baseline"], "-d", classes, *sources])
    record["compiled_fixtures"] = [seal(path) for path in sorted(classes.rglob("*.class"))]
    proofs = []
    for variant in ("baseline", "candidate"):
        result = run("04-clear-proof-" + variant,
                     [java, *VM_FLAGS, "-ea", "-cp",
                      str(snapshots[variant]) + ":" + str(classes),
                      "com.m3.collections.M3LinkedListClearRangeProof"])
        proofs.append(result.strip())
    if proofs[0] != proofs[1] or not proofs[0].startswith("PASS M3LinkedListClearRangeProof"):
        raise SystemExit("baseline/candidate proof outcome differs")
    record["stage"] = "contract_tests_passed"
    write_json(metadata, record)

    rows = []
    for fork in range(1, args.forks + 1):
        order = ("baseline", "candidate") if fork % 2 else ("candidate", "baseline")
        for variant in order:
            raw = run("05-sparse-clear-" + variant + "-fork" + str(fork),
                      [java, *VM_FLAGS, "-ea", "-cp",
                       str(snapshots[variant]) + ":" + str(classes),
                       "com.m3.collections.M3LinkedListSparseClearProbe", "suite",
                       ",".join(str(refill) for refill in args.refills)])
            for line in raw.splitlines():
                cells = line.split("\t")
                if len(cells) == 9 and cells[0] == "65536":
                    row = dict(zip(("peak", "refill", "warmupRounds", "measuredRounds",
                                    "elapsedNanos", "cpuNanos", "allocatedBytes",
                                    "checksum", "retainedSlots"), map(int, cells)))
                    row.update(variant=variant, fork=fork,
                               phase="initial" if row["warmupRounds"] == 0 else "warm")
                    rows.append(row)
    if len(rows) != 2 * args.forks * len(args.refills) * 2:
        raise SystemExit("unexpected focused sample count")
    summaries = []
    for phase in ("initial", "warm"):
        for refill in args.refills:
            selected = [row for row in rows if row["phase"] == phase and row["refill"] == refill]
            if len({(row["checksum"], row["retainedSlots"], row["measuredRounds"])
                    for row in selected}) != 1:
                raise SystemExit("matched checksum, capacity or rounds differ")
            for variant in ("baseline", "candidate"):
                matched = [row for row in selected if row["variant"] == variant]
                summary = {"phase": phase, "refill": refill, "variant": variant,
                           "forks": len(matched), "measuredRounds": matched[0]["measuredRounds"],
                           "retainedSlots": matched[0]["retainedSlots"]}
                for name in ("elapsedNanos", "cpuNanos", "allocatedBytes"):
                    values = [row[name] for row in matched]
                    summary[name] = {"min": min(values), "median": statistics.median(values),
                                     "max": max(values)}
                summaries.append(summary)
    write_json(output / "samples.json", rows)
    write_json(output / "summary.json", summaries)
    record["results"] = [seal(output / "samples.json"), seal(output / "summary.json")]
    record["stage"] = "focused_resource_probes_passed"
    record["finished_utc"] = datetime.datetime.now(datetime.timezone.utc).isoformat()
    write_json(metadata, record)
    print("PASS focused samples=" + str(len(rows)), flush=True)
    print(str(output / "summary.json"), flush=True)


if __name__ == "__main__":
    main()
