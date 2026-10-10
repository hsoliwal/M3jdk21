#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0
"""Fail-closed target-only composition proof for canonical Synexia String recipes.

This is a receiver guard, not a second reusable recipe implementation.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RECEIPT = "m3/runtime-integration/evidence/20261010-string-history-convergence/stacked-builder-union.json"
WORKFLOW = ".github/workflows/mindex-string-backing.yml"
ORACLE = "test/jdk/java/lang/String/M3StringBuilderInteropDifferentialTest.java"
BUILDER_CHECK = "m3/runtime-integration/check-m3string-builder-interop.py"
RECEIPT_GIT_BLOB = "65c21f48fe31215b4de8ecb3de52ef77620ceee0"
ORACLE_GIT_BLOB = "e553acd6b837c4a11935bd41b51558e01f8d741e"
BUILDER_CHECK_GIT_BLOB = "76ec73041417db5c43401398c9247f8da049495e"

RUN = re.compile(r'^[ \t]+run: make test TEST="([^"\n]+)"[ \t]*$', re.MULTILINE)


def require(condition: bool, reason: str) -> None:
    if not condition:
        raise ValueError(reason)


def git_blob(content: str) -> str:
    data = content.encode("utf-8")
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def verify(workflow: str, oracle: str, builder_guard: str, receipt: dict) -> int:
    baseline = receipt.get("required_baseline_selectors")
    require(receipt.get("schema") == "M3JDK21_STRING_BUILDER_OVERFLOW_JOIN_UNION_V1",
            "unknown receiver receipt")
    require(receipt.get("source_authority") == "hsoliwal/com.synexia"
            and receipt.get("runtime_authority") == "hsoliwal/M3jdk21",
            "Synexia donor / M3JDK21 runtime boundary changed")
    require(receipt.get("canonical_donor_prs") == [10158, 10161, 10162, 10179],
            "canonical source recipe provenance changed")
    require(receipt.get("no_synexia_runtime_dependency") is True
            and receipt.get("runtime_code_changed_in_this_receiver") is False,
            "thin receiver boundary violated")
    require(isinstance(baseline, list) and len(baseline) == 35
            and len(set(baseline)) == 35, "35-test baseline inventory changed")
    extra = receipt.get("additional_selector")
    require(extra == ORACLE and extra not in baseline and receipt.get("selectors_after") == 36,
            "builder test addition changed")
    require(all(isinstance(path, str) and path.startswith("test/jdk/")
                and path.endswith(".java") for path in baseline),
            "malformed baseline jtreg path")
    require(git_blob(oracle) == ORACLE_GIT_BLOB
            and receipt.get("builder_oracle_git_blob") == ORACLE_GIT_BLOB,
            "builder differential is not the exact Synexia donor postimage")
    require(git_blob(builder_guard) == BUILDER_CHECK_GIT_BLOB
            and receipt.get("builder_guard_git_blob") == BUILDER_CHECK_GIT_BLOB,
            "builder source guard differs from verified receiver")
    matches = RUN.findall(workflow)
    require(len(matches) == 1, "expected exactly one explicit String jtreg execution")
    selected = matches[0].split()
    require(len(selected) == len(set(selected)), "duplicate String jtreg selector")
    require(len(selected) >= 36, "String receiver lost the 36-test additive minimum")
    missing = sorted((set(baseline) | {extra}) - set(selected))
    require(not missing, "String receiver lost selectors: " + ", ".join(missing))
    for required in (
        "test/jdk/java/lang/String/M3StringBuilderInteropDifferentialTest.java",
        "test/jdk/java/lang/String/M3StringConcatLengthOverflowTest.java",
        "test/jdk/java/lang/String/M3StringGeneralJoinCarryTest.java",
    ):
        require(
            workflow.count("      - '" + required + "'") == 1,
            "missing or duplicate workflow change trigger: " + required,
        )
    for command in (
        "check-m3string-concat-overflow.py --self-test",
        "check-m3string-general-join-carry.py --self-test",
        "check-m3string-builder-interop.py --self-test",
        "make java.base-java-only",
        "make images",
    ):
        require(command in workflow, "missing existing runtime or source gate: " + command)
    return len(selected)


def self_test(workflow: str, oracle: str, builder_guard: str, receipt: dict) -> int:
    expected = verify(workflow, oracle, builder_guard, receipt)
    command = RUN.search(workflow).group(0)
    old = receipt["required_baseline_selectors"][0]
    tag = receipt["additional_selector"]

    candidates = [
        (workflow.replace(command, command.replace(old, "", 1), 1), oracle, builder_guard, receipt),
        (workflow.replace(command, command.replace(tag, "", 1), 1), oracle, builder_guard, receipt),
        (workflow.replace(command, command.replace(tag, tag + " " + tag, 1), 1), oracle, builder_guard, receipt),
        (workflow.replace("make images", "echo skipped image", 1), oracle, builder_guard, receipt),
        (workflow.replace("      - '" + tag + "'", "", 1), oracle, builder_guard, receipt),
        (workflow, oracle + " ", builder_guard, receipt),
        (workflow, oracle, builder_guard + " ", receipt),
    ]
    rejected = 0
    for bad_workflow, bad_oracle, bad_guard, bad_receipt in candidates:
        try:
            verify(bad_workflow, bad_oracle, bad_guard, bad_receipt)
        except ValueError:
            rejected += 1
        else:
            raise AssertionError("receiver union accepted a hostile mutation: " + str(rejected))
    require(rejected == len(candidates), "hostile mutation testing incomplete")
    return expected


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve()
    try:
        receipt_text = (root / RECEIPT).read_text(encoding="utf-8")
        require(git_blob(receipt_text) == RECEIPT_GIT_BLOB, "composition receipt drift")
        receipt = json.loads(receipt_text)
        workflow = (root / WORKFLOW).read_text(encoding="utf-8")
        oracle = (root / ORACLE).read_text(encoding="utf-8")
        builder_guard = (root / BUILDER_CHECK).read_text(encoding="utf-8")
        count = (self_test(workflow, oracle, builder_guard, receipt)
                 if args.self_test else verify(workflow, oracle, builder_guard, receipt))
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        raise SystemExit("M3_STRING_RECEIVER_UNION_FAIL|" + str(exc)) from exc
    print("M3_STRING_RECEIVER_UNION_PASS|selectors=" + str(count)
          + "|donor_blobs=2|mutants=" + str(7 if args.self_test else 0))


if __name__ == "__main__":
    main()
