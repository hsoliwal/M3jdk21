#!/usr/bin/env python3
"""Freeze the finite candidate evidence intake without binaries or output trees."""
from __future__ import annotations

import json
from pathlib import Path

from run_text_owner_gate import ROOT, WORK, digest, write_json


def main() -> None:
    base = WORK / "source-lock-correction"
    combined = base / "combined-candidate-001"
    gate = json.loads((base / "combined-candidate-001-receipts/RESULT.json").read_text())
    if gate["state"] != "PASS":
        raise AssertionError("Candidate gate is not passing")
    pins = json.loads((base / "HANDOFF.json").read_text())
    report = json.loads((combined / "BUILD_REPORT.json").read_text())
    lock = json.loads((base / "candidates/m3/tooling/module-packs/nlp-search/current-source-lock.json").read_text())
    summary = {
        "schema": "M3_NLP_CURRENT_SOURCE_LOCK_CANDIDATE_QUALIFICATION_1",
        "state": "QUALIFIED_ISOLATED_CANDIDATE",
        "canonical_namespace": "m3-nlp-current-receiver-lock-20261021",
        "target_changes": pins,
        "source_coverage": {"locked_files": sum(len(x["files"]) for x in lock["modules"].values()),
                            "com_m3_text_java_files_including_descriptor": 27,
                            "m3_production_java_sources_excluding_three_descriptors": 142,
                            "logical_manifest_changes": 4,
                            "all_other_guards_and_metadata_unchanged": True},
        "owner_gates": {"gvas": {"processes": 3, "checks_per_child": 17530, "native_same_address_and_inode": True,
                                  "initial_sandbox_proc_failure_preserved": True},
                        "bigstring": {"checks": 3213, "literal_owning_workflow_executed": True},
                        "rope": {"original_whole_module_gate": "PASS", "checks_per_mode": 83941,
                                 "modes": ["normal JIT", "-Xint", "-XX:-CompactStrings", "-Xbatch -XX:-TieredCompilation"],
                                 "initial_missing_explicit_ctor_lint_refusal_preserved": True,
                                 "earlier_finite_dependency_closure_diagnostic_retained_separately": True},
                        "constructor": {"public_classes_compared": 9, "public_descriptors_equal": True,
                                        "builder_full_executable_disassembly_equal": True,
                                        "existing_owning_checks_baseline": 50, "existing_owning_checks_candidate": 50,
                                        "debug_line_table_equivalence_claimed": False}},
        "combined_gate": {"receipt": "combined-candidate-001-receipts/RESULT.json", "modules": len(report["module_jars"]),
                          "source_built_named_modules": 36, "locked_external_named_modules": 26,
                          "lucene_production_java_sources": 3626, "opennlp_production_java_sources": 1000,
                          "m3_production_java_sources": 142, "source_input_readbacks": gate["input_count"],
                          "all_input_readbacks_equal": True, "elapsed_seconds": gate["elapsed_seconds"],
                          "installed_image_probes": report["tests"]},
        "runtime_warnings_retained": ["Optional jdk.management is not readable by Lucene for JVM internals/object sizing; no optimization or sizing-performance acceptance is claimed.",
                                      "Lucene core invokes Java21 foreign Linker downcallHandle without --enable-native-access=org.apache.lucene.core; the original warning and unchanged launch flags are retained."],
        "historical_source_custody": "LINEAGE_REVIEW.json records matching receiving commits/manifests and the differing immutable BigString source/test templates. No historical regeneration claim or extra streaming APIs.",
        "pending_at_candidate_seal": ["Canonical two-file SDK emission and actual receiving-tree source readback are separate authorities.",
                                      "Fresh actual-target runtime gate is separate from this isolated proof.",
                                      "Original Java25 upstream lane/full suites, global String/JDK/HotSpot, platform, optional native/model and performance gates are not promoted."]}
    write_json(base / "CANDIDATE_QUALIFICATION.json", summary)
    files: dict[str, Path] = {}
    def add(path: Path, relative: str) -> None:
        if relative in files or not path.is_file():
            raise AssertionError("Duplicate or missing intake file: " + relative)
        files[relative] = path
    for name in ("HANDOFF.json", "CANDIDATE_PATHS.json", "CANDIDATE_SCOPE.json", "CANDIDATE_QUALIFICATION.json",
                 "LINEAGE_REVIEW.json", "RECEIVING_COMMIT_READBACK.json", "OWNER_CONTRACTS.md",
                 "SOURCE_LOCK_GUARDS.json", "bigstring_template.diff", "bigstring_test_template.diff",
                 "SharedLexiconPrecomputeCatalog.java.diff", "current-source-lock.json.diff", "actual-classpath.txt"):
        add(base / name, name)
    for name in ("run_text_owner_gate.py", "run_text_prerequisite_followup.py", "prepare_receiver_candidate.py",
                 "qualify_builder_candidate.py", "verify_candidate_lock_guards.py", "run_combined_candidate.py",
                 "run_combined_receiver.py", "curate_lock_candidate_evidence.py"):
        add(WORK / name, "qualification-commands/" + name)
    for row in pins:
        add(Path(row["candidate_path"]), "candidate-postimages/" + row["target_path"])
    add(WORK.parent / "jobs-work/nlp-lineage-readonly/HANDOFF.json", "lineage/PINNED_PR_OBSERVATIONS.json")
    for name in ("gvas-owner-001", "gvas-owner-002", "bigstring-owner-001", "rope-owner-001", "rope-owner-002",
                 "builder-and-whole-rope-001", "combined-candidate-001-receipts"):
        for path in sorted((base / name).iterdir()):
            if path.is_file() and path.suffix in (".json", ".log", ".sh"):
                add(path, str(path.relative_to(base)))
    for path in sorted(combined.iterdir()):
        if path.is_file() and path.suffix in (".json", ".log", ".tsv", ".sources", ".txt"):
            add(path, str(path.relative_to(base)))
    for name in ("opennlp/BUILD.json", "lucene/BUILD_REPORT.json",
                 "lucene/source-overlay/SDK_RECEIPT.json", "lucene/dependency-descriptors/SDK_RECEIPT.json"):
        add(combined / name, "combined-candidate-001/" + name)
    source_paths = {str(p.relative_to(ROOT)) for p in (ROOT / "m3/core/src").rglob("*.java")}
    source_paths.update([
        "m3/core/test/com/m3/text/M3GvasLiteralImageContractTest.java",
        "m3/core/test/com/m3/text/M3BigStringSelfTest.java", "m3/core/test/M3RopeTextIndexTest.java",
        "m3/core/test/M3LexiconPrecomputeTest.java", "m3/verify-gvas-literal-precompute.py", "m3/run-rope-proof.sh",
        "m3/lexicon/native/m3_fixed_address_mapping_jni.c",
        "m3/ports/compiletime/src/main/java/com/m3/compiletime/M3GvasLiteralImageWriter.java",
        ".github/workflows/m3-bigstring-rope-summary-20261018.yml",
        "m3/tooling/module-packs/nlp-search/current_forks.py",
        "m3/tooling/module-packs/nlp-search/current-source-lock.json",
        "m3/tooling/module-packs/nlp-search/tests/SourceFactsProbe.java",
        "m3/tooling/module-packs/nlp-search/tests/SourceIntegrationProbe.java",
        "m3/tooling/module-packs/nlp-search/tests/ModelSourceProbe.java"])
    for relative in sorted(source_paths):
        add(ROOT / relative, "baseline2779/" + relative)
    rows = [{"source_path": str(path.resolve()), "relative_path": relative,
             "bytes": path.stat().st_size, "sha256": digest(path.read_bytes())}
            for relative, path in sorted(files.items())]
    handoff = WORK / "EVIDENCE_HANDOFF-LOCK-CANDIDATE.json"
    if handoff.exists():
        raise AssertionError("Evidence intake is already frozen")
    write_json(handoff, rows)
    print(json.dumps({"handoff": str(handoff), "files": len(rows), "bytes": sum(r["bytes"] for r in rows),
                      "sha256": digest(handoff.read_bytes())}))


if __name__ == "__main__":
    main()
