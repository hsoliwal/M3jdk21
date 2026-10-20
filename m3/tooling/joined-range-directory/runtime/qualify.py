#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Run original modular core contracts and matched joined-range workloads over actual Maven outputs."""
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import statistics
import subprocess
import time

HERE = Path(__file__).resolve().parent
SOURCE = Path(os.environ["M3_SOURCE_ROOT"]).resolve()
PROOF = Path(os.environ["M3_PROOF_ROOT"]).resolve()
ROOT = PROOF / "joined-range-runtime"
SDK = HERE.parent / "sdk"
TARGET = "m3/core/src/com/m3/text/JoinedM3StringPiece.java"
RESOURCE = "src/main/resources/com/synexia/rewrite/joined-range-directory-20261020"
MODES = {"jit": [], "int": ["-Xint"], "nocompact": ["-XX:-CompactStrings"],
         "c2": ["-Xbatch", "-XX:-TieredCompilation"]}
MARKERS = [
    "FOUNDATION_PASS", "M3JDK_TYPED_PRECOMPUTE_PASS", "M3JDK_DICTIONARY_FREQUENCY_CONTRACT_PASS",
    "M3JDK_TRANSLATION_RECEIVER_MAP_PASS", "M3JDK_SYNXIA_PAYLOAD_PASS", "M3JDK_SYNXIA_RECEIVER_PASS",
    "M3JDK_LANGDEX_RECEIVER_PASS", "M3_LANGDEX_SOURCE_ENVELOPE_PASS", "M3JDK_FAMILY_SIDECAR_PASS",
    "M3JDK_CATALOG_FAMILY_ADMISSION_PASS", "M3JDK_FAMILY_CONTRACT_PASS", "M3JDK_SI_UNIT_PASS",
    "M3JDK_NUMBER_SPACE_PASS", "M3JDK_NUMBER_CONTRACT_PASS", "M3JDK_GRAMMAR_SUPPORT_PASS",
    "M3JDK_INSTANCE_INDEX_PRECOMPUTE_PASS",
]
records = []
failures = []


def require(ok, message):
    if not ok:
        raise RuntimeError(message)


def regular(path):
    require(path.is_file() and all(not p.is_symlink() for p in (path, *path.parents)),
            "missing or redirected input:" + str(path))
    return path


def ident(raw):
    return {"bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(),
            "gitBlob": hashlib.sha1(b"blob " + str(len(raw)).encode("ascii") + b"\0" + raw).hexdigest()}


def relative(name):
    path = PurePosixPath(name)
    require(name and not path.is_absolute() and "\\" not in name and ":" not in name
            and all(p not in ("", ".", "..") for p in name.split("/")), "unsafe source path")
    return path


def run(name, argv, cwd, timeout=300):
    log = ROOT / (name + ".log")
    require(not log.exists(), "fresh command log required")
    begun = time.monotonic()
    child = None
    code = None
    try:
        with log.open("xb") as output:
            child = subprocess.Popen(argv, cwd=cwd, stdin=subprocess.DEVNULL,
                                     stdout=output, stderr=subprocess.STDOUT)
            while child.poll() is None:
                require(time.monotonic() - begun <= timeout, "inner command deadline:" + name)
                require(log.stat().st_size <= 1024**2, "inner command log cap:" + name)
                time.sleep(.1)
            code = child.wait()
    finally:
        if child is not None:
            if child.poll() is None:
                child.kill()
                child.wait(timeout=5)
            record = {"name": name, "argv": argv, "returncode": child.returncode,
                      "parentReaped": True, "log": log.name, "logIdentity": ident(log.read_bytes())}
            records.append(record)
            journal = ROOT / "inner-commands.jsonl"
            require(not journal.is_symlink(), "inner command journal redirected")
            with journal.open("a", encoding="utf-8") as stream:
                stream.write(json.dumps(record, sort_keys=True) + "\n")
    require(log.stat().st_size <= 1024**2, "terminal command log cap:" + name)
    if code != 0:
        failures.append(name + ":exit" + str(code))
    return code, log


def original_build(label, directory, java):
    code, log = run(label + "-original-build", ["bash", str(directory / "m3/build.sh")],
                    directory, timeout=600)
    build_logs = directory / "m3/build/logs"
    raw_logs = sorted(path for path in build_logs.rglob("*") if path.is_file()) if build_logs.is_dir() else []
    require(len(raw_logs) <= 80, "bounded complete original build log census")
    aggregate = ROOT / (label + "-original-build-logs.jsonl")
    total_log_bytes = 0
    with aggregate.open("x", encoding="utf-8") as stream:
        for path in raw_logs:
            raw = regular(path).read_bytes()
            total_log_bytes += len(raw)
            require(len(raw) <= 1024**2 and total_log_bytes <= 2*1024**2,
                    "complete original build log payload cap")
            stream.write(json.dumps({"path": path.relative_to(directory).as_posix(),
                **ident(raw), "content": raw.decode("utf-8")}, sort_keys=True) + "\n")
    lines = log.read_text(encoding="utf-8").splitlines()
    counts = {}
    for marker in MARKERS:
        matched = [line for line in lines if re.match(re.escape(marker) + r" checks=[1-9][0-9]*(?:\s|$)", line)]
        counts[marker] = matched
        if len(matched) != 4:
            failures.append(label + ":missing-original-four-mode-contract:" + marker)
    jar = directory / "m3/build/com.m3.text.jar"
    require(jar.is_file() and not jar.is_symlink(), "original build must compile the complete module")
    classes = directory / "m3/build/classes"
    compiled = {}
    for path in sorted(classes.rglob("*.class")):
        raw = regular(path).read_bytes()
        require(raw[:4] == bytes.fromhex("cafebabe") and int.from_bytes(raw[6:8], "big") == 65,
                "actual Java21 core class required")
        compiled[path.relative_to(classes).as_posix()] = ident(raw)
    require(len(compiled) >= 27, "complete core module class census required")
    return {"returncode": code, "logIdentity": ident(log.read_bytes()), "contracts": counts,
            "jar": ident(regular(jar).read_bytes()), "classes": compiled,
            "completeBuildLogBundle": {"path": aggregate.name, **ident(aggregate.read_bytes())}}


def sample(raw, label, ordinal):
    values = []
    expected_fields = {"schema", "sample", "leaves", "family", "from", "to", "iterations", "warmup",
                       "elapsedNanos", "allocatedBytes", "checksum", "resultLeaves", "retainedBackingBytes"}
    def unique(pairs):
        result = {}
        for key, value in pairs:
            require(key not in result, "duplicate benchmark JSON key")
            result[key] = value
        return result
    for line in raw.decode("utf-8").splitlines():
        if not line.startswith("{"):
            continue
        row = json.loads(line, object_pairs_hook=unique)
        require(set(row) == expected_fields and row["schema"] == "M3_JOINED_RANGE_SAMPLE_V1",
                "exact workload sample schema")
        for key in expected_fields - {"schema", "family"}:
            require(type(row[key]) is int, "exact integer workload counters")
        require(row["sample"] == ordinal and row["leaves"] in (32, 1024, 8192)
                and row["iterations"] == 1024 and row["warmup"] == 128
                and row["elapsedNanos"] > 0 and row["allocatedBytes"] >= 0, "workload context/counters")
        n = row["leaves"]
        mid = n // 2
        ranges = {"empty-middle": (mid, mid), "one-middle": (mid, mid + 1),
                  "one-tail": (n - 1, n), "two-middle": (mid - 1, mid + 1), "full": (0, n)}
        require(row["family"] in ranges and (row["from"], row["to"]) == ranges[row["family"]],
                "identical workload coordinates")
        start, end = ranges[row["family"]]
        def unit(at):
            return 0xd800 if at % 7 == 0 else 0xdc00 if at % 7 == 1 else at % 256
        per_call = end - start + (unit(start) + unit(end - 1) if start != end else 0)
        require(row["checksum"] == per_call * 1024 and row["resultLeaves"] == end - start
                and row["retainedBackingBytes"] == 2 * (end - start), "independent workload/backing oracle")
        row["cohort"] = label
        values.append(row)
    require(len(values) == 15 and len({(r["leaves"], r["family"]) for r in values}) == 15,
            "complete fifteen-case sample inventory")
    return values


def qualify():
    require(SOURCE.is_dir() and PROOF.is_dir()
            and all(not p.is_symlink() for root in (SOURCE, PROOF) for p in (root, *root.parents)),
            "explicit unredirected source/proof roots")
    require(not ROOT.exists(), "runtime output must start absent")
    ROOT.mkdir()
    metadata_raw = regular(HERE / "BUILD_INPUTS.json").read_bytes()
    metadata = json.loads(metadata_raw)
    require(metadata["schema"] == "M3_JOINED_RANGE_ORIGINAL_BUILD_INPUTS_V1"
            and metadata["commit"] == "73f59218dc4ef1a3ef79aad990e77273db2a2d60"
            and len(metadata["files"]) == 58 and len(metadata["testMainClasses"]) == 16
            and metadata["modes"] == list(MODES), "complete original build model required")
    module = __import__("runpy").run_path(str(regular(SDK / "verify.py")), run_name="m3_joined_range_runtime_sdk")
    module["inputs"]()
    emitted = SDK / "target/emitted"
    before = regular(emitted / "inverse" / TARGET).read_bytes()
    after = regular(emitted / "forward" / TARGET).read_bytes()
    require(before == regular(SDK / RESOURCE / "forward/before" / TARGET).read_bytes()
            and after == regular(SDK / RESOURCE / "forward/out" / TARGET).read_bytes(),
            "fresh authentic inverse/forward recipe outputs required")
    require(ident(before)["gitBlob"] == "cc3c84f5ea0230e2f57e3d3ae0f5d29b9241e597"
            and ident(after)["gitBlob"] == "3edc23c22362b08af00f7332ddc445a0274a6dd4",
            "exact admitted range atom required")
    source_values = {}
    for row in metadata["files"]:
        name = str(relative(row["path"]))
        require(name not in source_values, "duplicate original build owner")
        raw = regular(SOURCE / name).read_bytes()
        if name == TARGET:
            require(raw in (before, after), "unknown current joined owner drift")
        else:
            require(ident(raw) == {k: row[k] for k in ("bytes", "sha256", "gitBlob")},
                    "unchanged original build input drift:" + name)
        source_values[name] = raw
    require(len(source_values) == 58 and TARGET in source_values, "exact original input closure")
    java_home = Path(os.environ["JAVA_HOME"])
    java = str(regular(java_home / "bin/java"))
    javac = str(regular(java_home / "bin/javac"))
    environment = os.environ.copy()
    environment["M3_JDK"] = str(java_home)
    # The supervised parent sets all temp/cache paths; no fallback path is introduced.
    os.environ["M3_JDK"] = environment["M3_JDK"]
    cohorts = {}
    candidate = source_values[TARGET] if source_values[TARGET] == after else after
    for label, owner in (("baseline", before), ("candidate", candidate)):
        directory = ROOT / label
        directory.mkdir()
        for name, current in source_values.items():
            destination = directory / name
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(owner if name == TARGET else current)
        build = original_build(label, directory, java)
        jar = directory / "m3/build/com.m3.text.jar"
        tests = directory / "range-proof-classes"
        compile_argv = [javac, "--release", "21", "-Xlint:all", "-Werror", "--module-path", str(jar),
                        "--add-modules", "com.m3.text,jdk.management", "-d", str(tests),
                        str(regular(HERE / "M3JoinedRangeProof.java"))]
        compile_code, compile_log = run(label + "-range-compile", compile_argv, directory)
        require(compile_code == 0, "range proof source must compile with original module")
        base = [java, "-ea", "-esa", "--module-path", str(jar), "--add-modules", "com.m3.text,jdk.management",
                "--add-opens", "com.m3.text/com.m3.text=ALL-UNNAMED", "-cp", str(tests), "M3JoinedRangeProof"]
        contracts = {}
        for mode, flags in MODES.items():
            argv = [java, *flags, *base[1:], "contract"]
            code, log = run(label + "-range-" + mode, argv, directory, timeout=120)
            matches = re.findall(r"^JOINED_RANGE_CONTRACT_PASS directories=([1-9][0-9]*) cases=([1-9][0-9]*) backingUnits=([1-9][0-9]*) digest=([0-9a-f]{64})$", log.read_text(encoding="utf-8"), re.M)
            require(code == 0 and len(matches) == 1, "actual complete range corpus required")
            contracts[mode] = matches[0]
        for name, current in source_values.items():
            require(regular(directory / name).read_bytes() == (owner if name == TARGET else current),
                    "original build changed its source/fixture input:" + name)
        cohorts[label] = {"directory": directory, "argv": base, "build": build, "contracts": contracts}
    contract_values = [value for cohort in cohorts.values() for value in cohort["contracts"].values()]
    require(len(set(contract_values)) == 1, "baseline/candidate and four-mode corpus differs")
    require(contract_values[0][:3] == ("118", "4026", "7424"),
            "independent complete partition/range/backing corpus inventory required")
    baseline_classes = cohorts["baseline"]["build"]["classes"]
    candidate_classes = cohorts["candidate"]["build"]["classes"]
    require(set(baseline_classes) == set(candidate_classes), "compiled core class inventory differs")
    changed_classes = sorted(name for name in baseline_classes if baseline_classes[name] != candidate_classes[name])
    require(changed_classes == ["com/m3/text/JoinedM3StringPiece.class"],
            "only the admitted joined implementation class may differ")
    samples = []
    if not failures:
        for ordinal in range(5):
            order = ("baseline", "candidate") if ordinal % 2 == 0 else ("candidate", "baseline")
            for label in order:
                cohort = cohorts[label]
                code, log = run(label + "-benchmark-" + str(ordinal),
                                [*cohort["argv"], "benchmark", str(ordinal)], cohort["directory"], timeout=180)
                require(code == 0, "actual allocation/timing workload required")
                samples.extend(sample(log.read_bytes(), label, ordinal))
    summary = []
    if samples:
        require(len(samples) == 150, "fifteen cases times five samples times two cohorts")
        for n in (32, 1024, 8192):
            for family in ("empty-middle", "one-middle", "one-tail", "two-middle", "full"):
                result = {"leaves": n, "family": family}
                for label in ("baseline", "candidate"):
                    group = [r for r in samples if r["leaves"] == n and r["family"] == family and r["cohort"] == label]
                    require(len(group) == 5 and {r["sample"] for r in group} == set(range(5)), "paired sample closure")
                    nanos = [r["elapsedNanos"] for r in group]
                    result[label] = {"elapsedMedian": statistics.median(nanos), "elapsedMin": min(nanos),
                                     "elapsedMax": max(nanos), "allocatedMedian": statistics.median(r["allocatedBytes"] for r in group),
                                     "timingNoisy": max(nanos) - min(nanos) > statistics.median(nanos) / 5,
                                     "timingBelowOneMillisecond": min(nanos) < 1000000,
                                     "retainedBackingBytes": group[0]["retainedBackingBytes"]}
                summary.append(result)
    receipt = {"schema": "M3_JOINED_RANGE_RUNTIME_RECEIPT_V1", "state": "PASS" if not failures else "FAIL",
               "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"], "qualificationId": os.environ["M3_QUALIFICATION_ID"],
               "inputManifestIdentity": ident(metadata_raw),
               "environment": {"javaHome": str(java_home), "jvmOptions": os.environ.get("JAVA_TOOL_OPTIONS", ""),
                               "platform": __import__("platform").platform(), "machine": __import__("platform").machine(),
                               "affinity": sorted(os.sched_getaffinity(0)) if hasattr(os, "sched_getaffinity") else None},
               "targetBefore": ident(before), "targetAfter": ident(after),
               "receiverOwner": ident(source_values[TARGET]),
               "candidateOrigin": "ACTUAL_RECEIVER" if source_values[TARGET] == after else "ACTUAL_MAVEN_EMISSION",
               "cohorts": {label: {"build": row["build"], "contracts": row["contracts"]}
                           for label, row in cohorts.items()},
               "commands": records, "failures": failures, "samples": samples, "summary": summary,
               "coreModuleOriginalBuildQualified": not failures,
               "originalTestProgramsPerCohort": 16, "originalJvmModesPerCohort": 4,
               "rangeCorpusQualified": True, "changedCompiledClasses": changed_classes, "pairedAllocationTimingMeasured": len(samples) == 150,
               "physicalReceiverPostimageQualified": not failures and source_values[TARGET] == after,
               "targetMutationByQualificationJob": False, "nativeCodeChanged": False,
               "fullMatchedJdkQualified": False, "stringDispatchQualified": False,
               "performanceRetentionQualified": False, "phaseAdvanced": False}
    (ROOT / "runtime-receipt.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print("M3_JOINED_RANGE_RUNTIME_" + receipt["state"])
    require(not failures, "original build contracts failed:" + repr(failures))


if __name__ == "__main__":
    try:
        qualify()
    except BaseException as error:
        if ROOT.is_dir() and not ROOT.is_symlink():
            failure = ROOT / "failure.json"
            if not failure.exists() and not failure.is_symlink():
                failure.write_text(json.dumps({"schema": "M3_JOINED_RANGE_RUNTIME_FAILURE_V1",
                    "type": type(error).__name__, "message": str(error), "commands": records,
                    "runtimeQualified": False, "performanceRetentionQualified": False,
                    "phaseAdvanced": False}, indent=2) + "\n", encoding="utf-8")
        raise
