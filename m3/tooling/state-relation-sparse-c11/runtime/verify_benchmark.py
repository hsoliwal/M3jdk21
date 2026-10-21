#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify actual bounded paired native timing rows; never promote a microbenchmark to full-JDK proof."""
from __future__ import annotations
import json
import re
import statistics

MASK = (1 << 64) - 1
SEED, MULTIPLIER = 0xcbf29ce484222325, 0x100000001b3
DIMENSIONS = (1, 2, 31, 32, 63, 64, 3, 4, 8, 16)
FAMILIES = ("zero", "sparse", "dense", "highest-bit", "ceiling-early",
            "repeated-frontier", "alternating", "random", "zero-left-only", "zero-right-only")
VARIANTS = ("baseline", "sparse", "portable")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, "duplicate JSON key")
        result[key] = value
    return result


def integer(value, low, high):
    return type(value) is int and low <= value <= high


def rows(value, count):
    require(isinstance(value, list) and len(value) == count, "exact row vector length")
    require(all(isinstance(v, str) and re.fullmatch("[0-9a-f]{16}", v) for v in value),
            "exact unsigned-64 hexadecimal row")
    decoded = [int(v, 16) for v in value]
    require(all(v < (1 << count) for v in decoded), "foreign state bits")
    return decoded


def compose(left, right):
    return [_compose_row(frontier, right) for frontier in left]


def _compose_row(frontier, right):
    value = 0
    for column, row in enumerate(right):
        if frontier & (1 << column):
            value |= row
    return value


def expected_output(left, right, operation):
    if operation == "compose":
        return compose(left, right)
    # Independent linear sevenfold oracle, not the kernel's repeated-squaring algorithm.
    result = [1 << i for i in range(len(left))]
    for _ in range(7):
        result = compose(result, left)
    return result


def checksum_checkpoints(expected, iterations):
    end = max(iterations, 256)
    checkpoints = {256}
    count = 64
    while count <= iterations:
        checkpoints.add(count)
        count *= 2
    checksum, result = SEED, {}
    for i in range(end):
        checksum = ((checksum * MULTIPLIER) ^ expected[i % len(expected)] ^ i) & MASK
        if i + 1 in checkpoints:
            result[i + 1] = checksum
    return result


def validate(raw):
    require(isinstance(raw, bytes) and 0 < len(raw) <= 1048576, "bounded real stdout required")
    lines = raw.decode("utf-8", errors="strict").splitlines()
    require(len(lines) == 3202 and all(0 < len(line) < 16384 for line in lines), "exact trace census")
    records = [json.loads(line, object_pairs_hook=unique_object) for line in lines]
    require(all(isinstance(row, dict) for row in records), "object records required")
    environment, final = records[0], records[-1]
    require(set(environment) == {"kind", "schema", "cases", "samples", "variants", "targetNs",
                                "iterationCap", "clockResolutionNs", "randomInputApi", "randMax"}, "environment fields")
    require(environment["kind"] == "environment"
            and environment["schema"] == "M3_RELATION_NATIVE_PAIRED_TIMING_V2"
            and (environment["cases"], environment["samples"], environment["variants"]) == (200, 5, 3)
            and environment["targetNs"] == 20000000 and environment["iterationCap"] == 1048576
            and integer(environment["clockResolutionNs"], 1, 1000000000)
            and environment["randomInputApi"] == "libc-rand-srand"
            and integer(environment["randMax"], 32767, 2147483647), "timing protocol")
    require(set(final) == {"kind", "cases", "measurements", "consumedChecksum", "state"}
            and final["kind"] == "summary" and final["state"] == "PASS"
            and final["cases"] == 200 and final["measurements"] == 3000
            and isinstance(final["consumedChecksum"], str)
            and re.fullmatch("[0-9a-f]{16}", final["consumedChecksum"]), "terminal summary")
    cases, measurements, consumed = [], 0, 0
    for case_id in range(200):
        offset = 1 + case_id * 16
        item = records[offset]
        require(set(item) == {"kind", "case", "states", "family", "operation", "iterations",
                              "pilotNs", "pilotChecksum", "targetReached", "left", "right", "expected"},
                "case fields")
        if case_id < 160:
            n, family = DIMENSIONS[case_id // 16], FAMILIES[(case_id % 16) // 2]
        else:
            extra = case_id - 160
            n, family = DIMENSIONS[extra // 4], FAMILIES[8 + (extra % 4) // 2]
        operation = "compose" if case_id % 2 == 0 else "repeat-7"
        require(item["kind"] == "case" and type(item["case"]) is int and item["case"] == case_id
                and type(item["states"]) is int and item["states"] == n
                and item["family"] == family and item["operation"] == operation, "complete corpus identity")
        iterations = item["iterations"]
        require(integer(iterations, 64, 1048576) and iterations & (iterations - 1) == 0,
                "bounded power-of-two paired iterations")
        require(integer(item["pilotNs"], 1, 120000000000)
                and type(item["targetReached"]) is bool
                and item["targetReached"] == (item["pilotNs"] >= environment["targetNs"])
                and (item["targetReached"] or iterations == 1048576), "calibration protocol")
        left, right = rows(item["left"], n), rows(item["right"], n)
        expected = rows(item["expected"], n)
        require(expected == expected_output(left, right, operation), "independent exact output oracle")
        if family == "zero":
            require(not any(left) and not any(right), "zero corpus")
        elif family == "zero-left-only":
            require(not any(left) and right == [1 << i for i in range(n)],
                    "exact zero-left/diagonal-right corpus")
        elif family == "zero-right-only":
            require(left == [1 << i for i in range(n)] and not any(right),
                    "exact diagonal-left/zero-right corpus")
        elif family == "dense":
            require(all(v == (1 << n) - 1 for v in left), "dense corpus")
        elif family == "highest-bit":
            require(all(v & (1 << (n - 1)) for v in left), "highest-bit/bit63 corpus")
        elif family == "ceiling-early":
            require(all(v == (1 << n) - 1 for v in right), "ceiling corpus")
        elif family == "repeated-frontier":
            require(len(set(left)) == 1, "repeated frontier corpus")
        checks = checksum_checkpoints(expected, iterations)
        require(item["pilotChecksum"] == format(checks[iterations], "016x"), "pilot checksum")
        consumed ^= checks[256]  # Three equal variant warmups.
        pilot = 64
        while pilot <= iterations:
            consumed ^= checks[pilot]
            pilot *= 2
        samples = {variant: [] for variant in VARIANTS}
        for sample in range(5):
            for ordinal in range(3):
                row = records[offset + 1 + sample * 3 + ordinal]
                variant = VARIANTS[(sample + case_id + ordinal) % 3]
                require(set(row) == {"kind", "case", "sample", "ordinal", "variant", "iterations",
                                     "elapsedNs", "checksum"}, "sample fields")
                require(row["kind"] == "sample"
                        and all(type(row[k]) is int for k in ("case", "sample", "ordinal", "iterations"))
                        and (row["case"], row["sample"], row["ordinal"], row["variant"], row["iterations"])
                            == (case_id, sample, ordinal, variant, iterations), "interleaved paired sample identity")
                require(integer(row["elapsedNs"], 1, 120000000000)
                        and row["checksum"] == format(checks[iterations], "016x"),
                        "measured interval and consumed output checksum")
                consumed ^= checks[iterations]
                samples[variant].append(row["elapsedNs"] / iterations)
                measurements += 1
        medians = {name: statistics.median(value) for name, value in samples.items()}
        noise = {name: (max(value) - min(value)) / medians[name] for name, value in samples.items()}
        below_resolution = any(min(value) * iterations < 1000 * environment["clockResolutionNs"]
                               for value in samples.values())
        cases.append({"case": case_id, "states": n, "family": family, "operation": operation,
                      "iterations": iterations, "nsPerCall": samples, "medianNsPerCall": medians,
                      "rangeOverMedian": noise, "resolutionTooCoarse": below_resolution,
                      "calibrationTargetReached": item["targetReached"],
                      "sparseSpeedupOverBaseline": medians["baseline"] / medians["sparse"],
                      "portableSpeedupOverBaseline": medians["baseline"] / medians["portable"],
                      "noisy": any(value > .20 for value in noise.values())})
    require(measurements == 3000 and format(consumed, "016x") == final["consumedChecksum"],
            "complete warmup/pilot/sample checksum chain")
    return {"schema": "M3_RELATION_NATIVE_PAIRED_TIMING_VERIFICATION_V2",
            "state": "PASS_DATA_INTEGRITY_ONLY", "scope": "NATIVE_KERNEL_WITH_COMMON_DISPATCH_AND_CHECKSUM_OVERHEAD",
            "cases": cases, "caseCount": 200, "measurements": 3000,
            "independentMatrixOracles": 200, "variants": list(VARIANTS),
            "clockResolutionNs": environment["clockResolutionNs"],
            "allSamplesRetained": True, "fullOutputAndChecksumVerified": True,
            "noisyCases": sum(item["noisy"] for item in cases),
            "coarseClockCases": sum(item["resolutionTooCoarse"] for item in cases),
            "calibrationCapCases": sum(not item["calibrationTargetReached"] for item in cases),
            "automaticAdoption": False, "jniEndToEndQualified": False,
            "fullJdkPerformanceQualified": False, "heapNativeMappedRetentionQualified": False}
