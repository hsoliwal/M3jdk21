#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail closed unless the full Synexia MIndex/M3Index family pin stays exact and non-promoting."""

from __future__ import annotations

import csv
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PIN = Path(__file__).with_name("synexia-full-family-pin.tsv")
CATALOGUE = ROOT / "m3/docs/synexia-full-family-catalogue.json"
PRECOMPUTE = ROOT / "m3/docs/synexia-mindex-precompute-inventory.tsv"
HEX40 = re.compile(r"^[0-9a-f]{40}$")

EXPECTED = {
    "schema": "M3JDK21_SYNEXIA_FULL_FAMILY_PIN_V1",
    "source_repository": "hsoliwal/com.synexia",
    "source_pr": "9648",
    "source_commit": "b24262a92b741c78c44ca61afc8dac7ee1376d8a",
    "machine_manifest_path": "docs/M3-SCALE/invariants/M3-SYNEXIA-FULL-FAMILY-OWNERSHIP-1.json",
    "machine_manifest_git_blob": "c30a30476c12f7511caa124c4f7e0d72cd4e8e55",
    "receiver_invariant_path": "docs/M3-SCALE/invariants/M3-SYNEXIA-RECEIVER-1.md",
    "receiver_invariant_git_blob": "7ac11f77328c12cea7c157253d99937292235151",
    "catalogue_path": "synexia-openrewrite-recipes/verification/synexia-m3jdk-full-family-20261007/CATALOGUE_INDEX.json",
    "catalogue_git_blob": "8145c667c941a9358e9a5149489e455d950afc34",
    "inventory_source_revision": "7d2133f1412a9e7295296c3f86baae577bb3251c",
    "catalogue_total_paths": "4770",
    "catalogue_checked_roots": "13",
    "copyright_notice": "Copyright 2026 Hitesh Soliwal and contributors",
    "first_party_license": "Apache-2.0",
    "abstract_idea_policy": "ABSTRACT_IDEA_NOT_RELABELED_AS_COPYRIGHTED_SOURCE",
    "third_party_policy": "PRESERVE_ORIGINAL_COPYRIGHT_LICENSE_NOTICE",
    "openjdk_policy": "KEEP_EXISTING_OPENJDK_LICENSE_AND_NOTICE",
    "delivery_state": "SOURCE_PR_OPEN_QUALIFICATION_ONLY",
    "automatic_application": "false",
    "target_relicense_authority": "false",
    "family_completion": "false",
}


def load_pin(path: Path = PIN) -> dict[str, str]:
    with path.open("r", encoding="utf-8", newline="") as stream:
        rows = list(csv.reader(stream, delimiter="\t"))
    if not rows or rows[0] != ["field", "value"]:
        raise ValueError("invalid Synexia full-family pin header")
    values: dict[str, str] = {}
    for physical, cells in enumerate(rows[1:], start=2):
        if len(cells) != 2 or not cells[0] or not cells[1]:
            raise ValueError(f"invalid full-family pin row {physical}")
        if cells[0] in values:
            raise ValueError(f"duplicate full-family pin field {cells[0]}")
        values[cells[0]] = cells[1]
    if values != EXPECTED:
        missing = {k: v for k, v in EXPECTED.items() if values.get(k) != v}
        extra = {k: v for k, v in values.items() if k not in EXPECTED}
        raise ValueError(f"Synexia full-family pin drift missing_or_changed={missing!r} extra={extra!r}")
    for field in (
        "source_commit",
        "machine_manifest_git_blob",
        "receiver_invariant_git_blob",
        "catalogue_git_blob",
        "inventory_source_revision",
    ):
        if not HEX40.fullmatch(values[field]):
            raise ValueError(f"invalid Git identity in {field}")
    if values["automatic_application"] != "false":
        raise ValueError("full-family automatic application is forbidden")
    if values["target_relicense_authority"] != "false":
        raise ValueError("Synexia handoff never grants target relicensing")
    if values["family_completion"] != "false":
        raise ValueError("unevaluated family inventory cannot be marked complete")
    return values


def validate_catalogue(pin: dict[str, str]) -> dict:
    data = json.loads(CATALOGUE.read_text(encoding="utf-8"))
    if data.get("invariantId") != "M3-SYNEXIA-RECEIVER-1":
        raise ValueError("unexpected full-family catalogue invariant")
    metadata = data.get("metadata") or {}
    source = metadata.get("sourceTreeSelection") or {}
    counts = metadata.get("counts") or {}
    if metadata.get("sourceRevision") != pin["inventory_source_revision"]:
        raise ValueError("full-family catalogue source revision drift")
    if counts.get("total") != int(pin["catalogue_total_paths"]):
        raise ValueError("full-family catalogue total drift")
    roots = source.get("moduleRoots") or []
    if len(roots) != int(pin["catalogue_checked_roots"]):
        raise ValueError("full-family checked-root count drift")
    if source.get("repositoryWideComplete") is not False:
        raise ValueError("scoped catalogue must not claim repository-wide completion")
    if counts.get("byDisposition") != {"NOT_EVALUATED_IN_THIS_LEDGER": int(pin["catalogue_total_paths"])}:
        raise ValueError("full-family catalogue must remain unevaluated at inventory boundary")
    if data.get("acceptance") != "NOT_EVALUATED_IN_THIS_LEDGER":
        raise ValueError("full-family inventory cannot imply target acceptance")
    if data.get("repositoryCompletion") != "NOT_PROVEN":
        raise ValueError("full-family inventory cannot imply repository completion")
    return data


def validate_family_index() -> int:
    lines = [
        line for line in PRECOMPUTE.read_text(encoding="utf-8").splitlines()
        if line.strip()
    ]
    if not lines:
        raise ValueError("empty MIndex/precompute family index")
    header = lines[0].split("\t")
    if header != [
        "sourceWorld",
        "donorFamily",
        "representativeSources",
        "targetDisposition",
        "m3TargetOrRule",
        "notes",
    ]:
        raise ValueError("invalid MIndex/precompute family index header")
    rows = [line.split("\t") for line in lines[1:]]
    required = {
        "Canonical String identity",
        "Regex precompute",
        "AST precompute",
        "DAG precompute",
        "Generic precompute engine",
        "Universal precompute API",
        "Collections/search structures",
        "Native shadow/boundary",
        "Full named-file migration accounting",
    }
    observed = {row[1] for row in rows if len(row) == 6}
    missing = required - observed
    if missing:
        raise ValueError(f"missing full-family receiving categories: {sorted(missing)}")
    full = [row for row in rows if len(row) == 6 and row[1] == "Full named-file migration accounting"]
    if len(full) != 1 or full[0][3] != "NOT_EVALUATED_IN_THIS_LEDGER":
        raise ValueError("full named-file accounting must remain explicitly unevaluated")
    return len(rows)



# Current continuation contract; a successor must reconcile target proof before changing this baseline.
RECEIVING_ORDER = ("STRING", "ARRAYS", "COLLECTIONS", "AST_COMPILER", "REMAINING_FAMILIES")
RECEIVING_GATES = {'STRING': ('canonical_owner_and_utf16_contract',
            'regex_v7_codec_and_bootstrap_closure',
            'java_jni_parity_and_lifetimes',
            'jdk_build_jtreg_interpreter_c1_c2_gc_cds',
            'cold_warm_cpu_heap_native_memory'),
 'ARRAYS': ('fixed_length_reified_type_and_array_store',
            'bounds_overlap_clone_and_arraycopy',
            'gc_barriers_and_jni_acquire_release',
            'jdk_build_jtreg_interpreter_c1_c2',
            'cold_warm_cpu_heap_native_memory'),
 'COLLECTIONS': ('per_concrete_type_contract',
                 'null_equality_identity_order_views_iterators',
                 'serialization_subclass_jmm_concurrency',
                 'no_retained_per_element_structural_objects',
                 'jdk_build_jtreg_and_resource_measurements'),
 'AST_COMPILER': ('consumer_format_version_and_invalidation',
                  'jdk_compiler_module_boundary',
                  'attributed_multipass_behavior_and_opaque_payloads',
                  'java21_source_classfile_and_bootstrap',
                  'bounded_precompute_and_resource_measurements'),
 'REMAINING_FAMILIES': ('complete_path_and_non_prefix_dependency_accounting',
                        'consumer_module_and_lifetime_contract',
                        'file_generation_dag_effects_and_image_invalidation',
                        'domain_distributed_hardware_explicit_disposition',
                        'target_platform_runtime_and_resource_measurements')}


def unique_json_object(pairs: list[tuple[str, object]]) -> dict:
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"duplicate receiving-map JSON key: {key}")
        result[key] = value
    return result


def validate_receiving_sequence(path: Path = ROOT / "m3/docs/name-mapping.json") -> dict:
    data = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_json_object)
    policy = data.get("porting_invariant")
    if not isinstance(policy, dict) or policy.get("id") != "M3-JDK-PORT-1":
        raise ValueError("missing canonical porting invariant")
    plan = policy.get("receiving_sequence")
    if not isinstance(plan, dict):
        raise ValueError("missing receiving sequence")
    exact = {
        "schema": "M3_RECEIVING_SEQUENCE_V1",
        "order": list(RECEIVING_ORDER),
        "active_phase": "STRING",
        "scope": "ORDERED_QUALIFICATION_PLAN_NOT_RUNTIME_ACCEPTANCE",
        "public_jdk_names_unchanged": True,
        "canonical_payload_owner_unchanged": True,
        "recipe_first": True,
        "serial_file_atom_replacement": True,
        "first_party_license": "Apache-2.0",
        "copyright_notice": "Copyright 2026 Hitesh Soliwal and contributors",
        "abstract_ideas": "PROVENANCE_NOT_COPYRIGHTED_EXPRESSION",
        "third_party_and_openjdk": "PRESERVE_PER_FILE_LICENSE_COPYRIGHT_NOTICE",
        "automatic_application": False,
        "family_completion": False,
        "repository_completion": False,
        "source_inventory_revision": "7d2133f1412a9e7295296c3f86baae577bb3251c",
        "source_inventory_named_paths": 4770,
        "source_inventory_checked_roots": 13,
        "source_inventory_disposition": "NOT_EVALUATED_IN_THIS_LEDGER",
        "donor_review_order": ["EXISTING_CATALOGUE_AND_FIRST_PARTY", "LEETCODE", "HACKERRANK", "GEEKSFORGEEKS", "LICENSED_PINNED_GITHUB"],
        "donor_dispositions": ["COPY", "ADAPT", "CLEAN_ROOM", "EVIDENCE_ONLY", "REJECT"],
    }
    for key, expected in exact.items():
        value = plan.get(key)
        if type(value) is not type(expected) or value != expected:
            raise ValueError(f"receiving sequence contract drift: {key}")
    phases = plan.get("phases")
    if not isinstance(phases, list) or len(phases) != len(RECEIVING_ORDER):
        raise ValueError("missing or extra receiving phase")
    for index, (phase, phase_id) in enumerate(zip(phases, RECEIVING_ORDER)):
        if not isinstance(phase, dict) or phase.get("id") != phase_id:
            raise ValueError("reordered, duplicate or unknown receiving phase")
        if "predecessor" not in phase or phase["predecessor"] != (RECEIVING_ORDER[index - 1] if index else None):
            raise ValueError(f"receiving predecessor drift: {phase_id}")
        expected_state = "QUALIFICATION_REQUIRED" if index == 0 else "WAITING_FOR_PREDECESSOR"
        if phase.get("state") != expected_state or phase.get("acceptance_receipts") != []:
            raise ValueError(f"reviewed successor with target evidence required: {phase_id}")
        if phase.get("required_gates") != list(RECEIVING_GATES[phase_id]):
            raise ValueError(f"missing or changed target gates: {phase_id}")
        if not isinstance(phase.get("scope"), str) or not phase["scope"].strip():
            raise ValueError(f"missing scope: {phase_id}")
    if not HEX40.fullmatch(str(plan.get("target_baseline", ""))):
        raise ValueError("missing exact target baseline")
    for field in ("advance_rule", "dependency_rule", "enforcement_boundary"):
        if not isinstance(plan.get(field), str) or not plan[field].strip():
            raise ValueError(f"missing receiving boundary: {field}")
    if plan.get("atom_evidence") != ['source_revision_path_blob_sha256',
 'license_copyright_notice_and_intake_mode',
 'dependency_effect_and_history_inventory',
 'target_owner_preimage_postimage',
 'synexia_recipe_revision_and_manifest',
 'locked_api_behavior_and_failure_oracle',
 'executed_build_contract_and_applicable_target_gates',
 'fixed_point_replay_and_drift_refusal',
 'cpu_heap_native_memory_and_cold_warm_results',
 'pending_or_excluded_reason']:
        raise ValueError("missing or changed per-atom evidence contract")
    return plan


def main(argv: list[str]) -> int:
    if len(argv) != 1:
        print("usage: check_synexia_full_family.py", file=sys.stderr)
        return 2
    pin = load_pin()
    catalogue = validate_catalogue(pin)
    families = validate_family_index()
    plan = validate_receiving_sequence()
    print(
        "SYNEXIA_FULL_FAMILY_PIN_PASS "
        f"source={pin['source_commit']} paths={catalogue['metadata']['counts']['total']} "
        f"families={families} license={pin['first_party_license']} "
        f"active={plan['active_phase']} ordered_phases={len(plan['phases'])} completion=false"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
