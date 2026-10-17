#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Assemble handoff inputs from executed, source-bound qualification receipts."""
import hashlib
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
TASK = HERE.parent
MIRROR = "m3/vendor/synexia/synexia-openrewrite-recipes/recipe-crates/m3-ast-sharing-20261017"
LEVELS = [
    {"level": 0, "scope": "PER_FILE_FACTS_AND_ATOMS"},
    {"level": 1, "scope": "FILE_SUMMARY"},
    {"level": 2, "scope": "PACKAGE"},
    {"level": 3, "scope": "MODULE"},
    {"level": 4, "scope": "PROJECT"},
    {"level": 5, "scope": "REPOSITORY"},
]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    evidence = {}

    def read(relative):
        path = TASK / relative
        data = json.loads(path.read_text())
        evidence[relative] = sha(path)
        return data

    def require(condition, message):
        if not condition:
            raise ValueError(message)

    def positive(data, label):
        require(str(data.get("status", "")).upper() in
                ("PASS", "PASSED", "PASSED_WITH_CANONICAL_REACTOR_UNAVAILABLE"), label + " did not pass")

    artifacts = read("frontend/evidence/FINAL_COMPOSED_ARTIFACTS.json")
    custody = read("integration/evidence/provider-custody/QUALIFICATION.json")
    positive(custody, "exact recipe-provider custody")
    require(custody["exact_sources"] == 4 and custody["clean_compiled_projects"] == 7,
            "public recipe-provider closure incomplete")
    custody_sdk = read("integration/evidence/provider-custody/CACHE_SDK.json")
    positive(custody_sdk, "custodied recipe-provider SDK")
    require(custody_sdk["tests"] == 4, "custodied provider SDK incomplete")
    receiver = read("frontend/hierarchy/evidence/target-source-readback/actual-target.json")
    positive(receiver, "actual receiving sources")
    require(receiver["result"]["sources_verified"] == 128 and not receiver["product_writes"],
            "actual receiving source closure incomplete")
    refusal = read("frontend/hierarchy/evidence/target-source-readback/REFUSAL.json")
    positive(refusal, "altered source refusal")
    require(refusal["exit_code"] == 1 and refusal["fixture_contains_every_final_sealed_source"],
            "changed source refusal incomplete")
    read("frontend/evidence/FINAL_CANONICAL_RECONCILIATION.json")
    plugin, patch = artifacts["plugin_sha256"], artifacts["patch_sha256"]
    require((artifacts["runtime_sources"], artifacts["facade_sources"], artifacts["compiler_sources"])
            == (103, 10, 4), "standalone AST artifact closure changed")
    sdk = read("frontend/evidence/FINAL_UPSTREAM_COMPOSITION_SDK.json")
    positive(sdk, "composed frontend SDK")
    require(sdk["no_external_parser_classpath"] and sdk["tests"] == 3, "frontend SDK scope changed")
    application = read("frontend/evidence/FINAL_COMPOSED_APPLICATION.json")
    positive(application, "actual application")
    runtime = read("frontend/runtime-tests/evidence/QUALIFICATION.json")
    positive(runtime, "frontend runtime")
    require(runtime["plugin_sha256"] == plugin and runtime["patch_sha256"] == patch,
            "frontend runtime used different artifacts")
    require(runtime["tests"] == 26 and runtime["failures"] == runtime["errors"] == runtime["skipped"] == 0,
            "frontend runtime is incomplete")
    cache = read("cache/evidence/QUALIFICATION.json")
    positive(cache, "cache")
    native = read("cache/evidence/ISOLATED_NATIVE_RECEIPT.json")
    positive(native, "native")
    require(native["freshCompilationGeneration"] and len(native["jniSymbols"]) == 16,
            "native ABI generation incomplete")
    require(native["librarySha256"] == runtime["native_library_sha256"], "native artifact differs")
    hierarchy = read("frontend/hierarchy/evidence/QUALIFICATION.json")
    positive(hierarchy, "hierarchy")
    require(hierarchy["runtime"]["tests"] == 40, "hierarchy suite incomplete")
    final_hierarchy = read("frontend/hierarchy/evidence/final-composed/QUALIFICATION.json")
    positive(final_hierarchy, "final composed hierarchy")
    require(final_hierarchy["base_frontend_sha256"] == plugin
            and final_hierarchy["compiler_patch_sha256"] == patch
            and final_hierarchy["runtime_tests"] == 40
            and all(row["errors"] == row["failures"] == row["skipped"] == 0
                    for row in final_hierarchy["test_suites"]), "final hierarchy evidence differs")
    read("frontend/hierarchy/evidence/final-composed/COMPILE.json")
    upstream = read("frontend/upstream-contract/evidence/QUALIFICATION.json")
    positive(upstream, "upstream compiler contract")
    require(upstream["patch_sha256"] == patch and upstream["upstream_checks"] == 70,
            "upstream compiler contract used a different artifact")
    coexistence = read("frontend/upstream-contract/evidence/COEXISTENCE.json")
    positive(coexistence, "plugin coexistence")
    require(coexistence["compiler_patch_sha256"] == patch and coexistence["frontend_jar_sha256"] == plugin
            and coexistence["checks"] == 9, "plugin coexistence evidence differs")
    admission = read("frontend/upstream-contract/evidence/receipt-admission/QUALIFICATION.json")
    positive(admission, "reproducible artifact admission")
    primitive = read("frontend/primitive-image/evidence/RESULTS.json")
    positive(primitive, "primitive image")
    require(primitive["plugin_sha256"] == plugin and primitive["patch_sha256"] == patch
            and primitive["checks"] == 18, "primitive image evidence differs")
    module = read("frontend/module-wiring/evidence/RESULTS.json")
    positive(module, "ordinary Maven modules")
    require(module["source_counts"]["actual_maven_compiler_inputs"] == 138
            and module["source_counts"]["vendor"] == 103, "ordinary AST module closure differs")
    require(module["compiler_patch_sha256"] == patch, "module proof used a different compiler patch")
    require(sum(row["tests"] for row in module["normal_tests"]) == 52
            and all(row["errors"] == row["failures"] == row["skipped"] == 0 for row in module["normal_tests"]),
            "ordinary module tests incomplete")
    require(sum(row["tests"] for row in module["native_positive_profile_tests"]) == 54
            and all(row["errors"] == row["failures"] == row["skipped"] == 0
                    for row in module["native_positive_profile_tests"]), "native module tests incomplete")
    optional = read("frontend/openrewrite/evidence/RESULTS.json")
    positive(optional, "optional OpenRewrite")
    require(optional["host_plugin_sha256"] == plugin, "optional proof used a different frontend")
    require(optional.get("compiler_patch_sha256", optional.get("patch_sha256")) == patch,
            "optional proof used a different compiler patch")
    require(optional["bytecode_equality"] == "PASSED" and not optional["independent_openrewrite_parser_class_loaded"],
            "optional source/bytecode contract incomplete")
    registry = read("frontend/registry/evidence/QUALIFICATION.json")
    positive(registry, "name mapping")
    require(registry["scope"]["new_mapping_count"] == 12 and registry["scope"]["global_gates_unchanged"],
            "name mapping scope changed")

    rows = []

    def row(area, result, relative, status="PASS"):
        target = "../../../" + MIRROR + "/" + relative if relative else ""
        rows.append({"area": area, "status": status, "result": result,
                     "evidence": "[receipt](" + target + ")" if target else "Separate repository gate"})

    row("Maven/OpenRewrite recipes", "Exact Java/text outputs, drift and missing/duplicate-owner refusal, fixed points, and actual application passed.", "frontend/evidence/FINAL_UPSTREAM_COMPOSITION_SDK.json")
    row("Public recipe and source custody", "Four exact providers; seven clean Maven authoring compilations; four repeated Java/text SDK tests; all 128 receiving sources match, and a one-file change is refused.", "integration/evidence/provider-custody/QUALIFICATION.json")
    row("Immutable source, compiler views and codec", "26 tests; 103 canonical runtime sources, 10 receiving facades and 4 compiler sources compiled strictly.", "frontend/runtime-tests/evidence/QUALIFICATION.json")
    row("Bounded cache and real JNI", "14 cache runtime tests including 2 SQLite JNI tests; 16 javac-generated native exports; fresh native rebuild matches the tested binary.", "cache/evidence/QUALIFICATION.json")
    row("L0–L5 hierarchy", "40 canonical/receiving hierarchy contracts on the final composed artifacts; immutable coordinate-prefix snapshots and overflow-safe existing partial-cache accounting.", "frontend/hierarchy/evidence/final-composed/QUALIFICATION.json")
    row("Existing compiler API and service registration", "Unmodified upstream test: 70 checks in a linked 5-module image with hash checks enabled; 9 separate coexistence checks for both plugins.", "frontend/upstream-contract/evidence/QUALIFICATION.json")
    row("Primitive images", "18 checks; image and sidecar bytes preserved in both modes, using the original compiler task.", "frontend/primitive-image/evidence/RESULTS.json")
    row("Ordinary Maven module", "138 exact inputs; 52 normal tests and 54 selected-native tests; required missing-library failure; upstream leaf self-test and service discovery passed.", "frontend/module-wiring/evidence/RESULTS.json")
    row("Optional OpenRewrite 8.17.1", "Stock visitor consumes the same javac units; source/type identity and reference class bytes preserved; core works without Rewrite; reentry refuses publication.", "frontend/openrewrite/evidence/RESULTS.json")
    row("Authoritative name mapping", "12 scoped mappings, 38 source/manifest pins; existing mappings and broader phase gates preserved.", "frontend/registry/evidence/QUALIFICATION.json")
    if module["canonical_reactor"]["status"] != "PASSED":
        row("Full canonical reactor", "Unavailable: the sparse checkout omits child modules required by the pinned parent. Canonical standalone sources and the M3 receiving modules have separate successful proofs.", "frontend/module-wiring/evidence/RESULTS.json", "NOT_QUALIFIED")
    row("Custom source-built dependency coordinates", "Normal Maven proof uses the receiver's documented ASM 9.6 / JUnit 5.10.2 CI overrides; the custom source-artifact defaults were not qualified here.", "frontend/module-wiring/evidence/RESULTS.json", "NOT_QUALIFIED")
    row("Whole JDK, jtreg/JCK and other platforms", "Separate gates; no broader phase promotion is inferred from these finite Linux/Java21 proofs.", "", "NOT_RUN")
    data = {
        "levels": LEVELS,
        "hierarchy_description": "The hierarchy stores child ranges and indexes in primitive arrays and evaluates admitted providers from leaves upward. Each published result binds source, complete caller-declared compilation context, provider, phase, and explicit dependency closure. Existing partial-AST and swap owners enforce lazy access and leases; a changed source generation receives a different identity.",
        "plugin_sha256": plugin, "patch_sha256": patch,
        "native_library_sha256": native["librarySha256"],
        "artifact_source_counts": {key: artifacts[key] for key in ("runtime_sources", "facade_sources", "compiler_sources")},
        "compiler_source_sha256": artifacts["compiler_source_sha256"],
        "runtime_source_sha256": artifacts["source_sha256"],
        "receiving_source_closure_sha256": receiver["result"]["source_closure_sha256"],
        "module_source_counts": module["source_counts"],
        "receipt_sha256": evidence,
        "qualifications": rows,
        "reproduction_markdown": (HERE / "REPRODUCE.template.md").read_text(),
        "catalogue_index_tsv": (HERE / "CATALOGUE_INDEX.template.tsv").read_text(),
        "global_receiving_phases_promoted": False,
    }
    destination = HERE / "QUALIFICATION_INPUT.json"
    destination.write_text(json.dumps(data, indent=2) + "\n")
    print(json.dumps({"status": "PASS", "receipts": len(evidence), "rows": len(rows), "input_sha256": sha(destination)}))


if __name__ == "__main__":
    main()
