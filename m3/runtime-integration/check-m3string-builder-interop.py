#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0
"""Target-local fail-closed gate for Synexia-owned M3 String builder interop proof."""

import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BUILDER = "src/java.base/share/classes/java/lang/AbstractStringBuilder.java"
ORACLE = "test/jdk/java/lang/String/M3StringBuilderInteropDifferentialTest.java"
WORKFLOW = ".github/workflows/mindex-string-backing.yml"


def require(condition, message):
    if not condition:
        raise ValueError(message)


def verify(builder, oracle, workflow):
    start = builder.find("    private final void appendChars(String s, int off, int end) {")
    end = builder.find("    private final void appendChars(CharSequence s, int off, int end) {", start)
    require(start >= 0 and end > start, "builder String range owner unavailable")
    body = builder[start:end]

    for expected in (
        "M3String storage = s.m3();",
        "M3String range = storage.slice(off, end);",
        "range.factsIfPrepared();",
        "s.getBytes(this.value, off, this.count, LATIN1, end - off);",
        "s.getBytes(this.value, i, j, UTF16, end - i);",
        "s.getBytes(this.value, off, this.count, UTF16, end - off);",
    ):
        require(expected in body, "M3 builder bulk branch lost: " + expected)
    # Do not pin the stock fallback implementation: compatible future rewrites remain admissible.

    for expected in (
        "M3StringBuilderInteropDifferentialTest",
        "-Dtest.m3=true",
        "-XX:-CompactStrings",
        "String.class.getDeclaredField(\"m3\")",
        "new char[source.length()]",
        "2_000",
        "builder append range",
        "builder insert range",
        "buffer append range",
        "buffer insert range",
        "M3_BUILDER_INTEROP_DIFFERENTIAL_PASS",
    ):
        require(expected in oracle, "differential corpus/gate missing: " + expected)

    require(
        "      - 'test/jdk/java/lang/String/M3StringBuilderInteropDifferentialTest.java'"
        in workflow,
        "new jtreg missing individual change-trigger path",
    )
    require(
        "test/jdk/java/lang/String/M3StringBuilderInteropDifferentialTest.java"
        in workflow.split("      - name: Run M3 String backing jtreg", 1)[-1],
        "new jtreg missing from executing test command",
    )
    require("check-m3string-builder-interop.py --self-test" in workflow,
            "target source checker not executed")
    require("make images" in workflow, "matching image build gate removed")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--self-test", action="store_true")
    arguments = parser.parse_args()
    root = arguments.root.resolve()
    try:
        builder = (root / BUILDER).read_text(encoding="utf-8")
        oracle = (root / ORACLE).read_text(encoding="utf-8")
        workflow = (root / WORKFLOW).read_text(encoding="utf-8")
        verify(builder, oracle, workflow)
        count = 0
        if arguments.self_test:
            mutants = [
                (builder.replace(
                    "s.getBytes(this.value, off, this.count, LATIN1, end - off);",
                    "s.value();", 1), oracle, workflow),
                (builder, oracle.replace("-XX:-CompactStrings", ""), workflow),
                (builder, oracle, workflow.replace(
                    "      - 'test/jdk/java/lang/String/M3StringBuilderInteropDifferentialTest.java'",
                    "", 1)),
                (builder, oracle, workflow.replace("make images", "", 1)),
            ]
            for mutant_builder, mutant_oracle, mutant_workflow in mutants:
                try:
                    verify(mutant_builder, mutant_oracle, mutant_workflow)
                except ValueError:
                    count += 1
                else:
                    raise ValueError("hostile mutant escaped source guard: " + str(count))
    except (OSError, ValueError) as problem:
        raise SystemExit("M3_BUILDER_INTEROP_GATE_FAIL|" + str(problem)) from problem
    print("M3_BUILDER_INTEROP_GATE_PASS|source=1|oracle=1|mutants=" + str(count))


if __name__ == "__main__":
    main()
