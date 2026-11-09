#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Build an exact all-85-JEP, contract-preserving implementation/proof matrix.

This is a read-only joining/projection atom, NOT another backport engine or a
source mutation authority. The existing released-JEP authority, residue queue
and source-blob witness retain ownership of their underlying facts.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import sys
from collections import Counter
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Mapping, Sequence

import jep_source_witness as source_witness
import release_jep_authority
import jep_residue

CANDIDATES = frozenset({"candidate", "candidate-high-risk"})
FIELDS = (
    "release", "jep", "title", "domain", "problem_family", "disposition",
    "source_state", "packet_count", "receipt_count", "product_hashes_matched",
    "java21_default", "policy_classification", "implementation_gate",
    "required_proofs", "next_action", "promotion", "donor",
)


@dataclass(frozen=True)
class JepMatrixRow:
    release: int
    jep: int
    title: str
    domain: str
    problem_family: str
    disposition: str
    source_state: str
    packet_count: int
    receipt_count: int
    product_hashes_matched: int
    java21_default: str
    policy_classification: str
    implementation_gate: str
    required_proofs: str
    next_action: str
    promotion: str = "NOT_AUTHORIZED_BY_MATRIX"
    donor: str = "https://github.com/openjdk/jdk"


def problem_family(domain: str) -> str:
    """Category routing only; never conflate an algorithm analogy with a JEP."""
    if domain == "language":
        return "LANGUAGE_SPEC"
    if "security" in domain:
        return "SECURITY_CRYPTOGRAPHY"
    if "jfr" in domain:
        return "JFR_MONITORING"
    if "gc" in domain or domain.startswith("vm-"):
        return "GC_AND_OBJECT_LAYOUT"
    if "compiler" in domain:
        return "JIT_AND_COMPILER"
    if domain.startswith("tool-"):
        return "JDK_TOOLS"
    if "native" in domain:
        return "NATIVE_INTEROP"
    if "removal" in domain or "compatibility" in domain:
        return "COMPATIBILITY"
    if "threads" in domain:
        return "CONCURRENCY"
    if "aot" in domain:
        return "AOT_CDS"
    if "network" in domain:
        return "NETWORK_PROTOCOLS"
    if "library" in domain:
        return "JDK_LIBRARIES"
    return "DOMAIN_REVIEW_REQUIRED"


def required_proofs(domain: str, disposition: str, jep: int) -> str:
    if disposition.startswith("reject"):
        return "JAVA21_SPEC_INCOMPATIBILITY"
    if disposition.startswith("superseded"):
        return "CUMULATIVE_SUCCESSOR_LINEAGE"
    gates = ["UPSTREAM_LICENSE_AND_LINEAGE", "MAVEN_OPENREWRITE", "JDK21_API_AND_GRAMMAR",
             "JDK_IMAGE_BUILD", "JTREG", "SECOND_PASS_FIXED_POINT"]
    if "gc" in domain or "hotspot" in domain or "vm-" in domain or domain == "runtime-threads":
        gates.extend(("HOTSPOT_FASTDEBUG", "GC_AND_JVM_REGRESSION"))
    if "native" in domain or jep in (423, 491):
        gates.append("JNI_JVMTI_NATIVE_PARITY")
    if "jfr" in domain:
        gates.append("JFR_EVENT_REGRESSION")
    if "security" in domain:
        gates.append("CRYPTO_PROVIDER_INTEROP")
    if "compiler" in domain or domain == "language":
        gates.append("JAVAC_LANGTOOLS")
    if "aot" in domain:
        gates.append("AOT_CDS_REPLAY")
    if "network" in domain:
        gates.append("PROTOCOL_INTEROP")
    return ";".join(gates)


def assemble(
    catalogue: Sequence[Mapping[str, str]],
    authority: Mapping[int, Sequence[int]],
    witnesses: Sequence[source_witness.Witness],
    priority: Sequence[Mapping[str, str]],
    residue: Sequence[jep_residue.Residue],
) -> list[JepMatrixRow]:
    """Join exact same-version authorities without guessing unproven feature status."""
    ordered = [(int(row["release"]), int(row["jep"])) for row in catalogue]
    actual = set(ordered)
    expected = {(release, jep) for release, ids in authority.items() for jep in ids}
    if len(actual) != len(ordered) or actual != expected or ordered != sorted(ordered):
        raise ValueError("JEP matrix must equal exact sorted official release denominator")
    by_witness = {item.jep: item for item in witnesses}
    if len(by_witness) != len(witnesses) or set(by_witness) != {jep for _, jep in ordered}:
        raise ValueError("missing, extra or duplicate source witnesses")
    by_priority = {int(item["jep"]): item for item in priority}
    if len(by_priority) != len(priority) or not set(by_priority) <= set(by_witness):
        raise ValueError("duplicate or unrecognized Java21 priority policy")
    by_residue = {item.jep: item for item in residue}
    if len(by_residue) != len(residue) or not set(by_residue) <= set(by_witness):
        raise ValueError("duplicate or unrecognized JEP proof residue")

    rows = []
    for catalogue_row in catalogue:
        release = int(catalogue_row["release"])
        jep = int(catalogue_row["jep"])
        status = catalogue_row["disposition"]
        seen = by_witness[jep]
        if seen.release != release or seen.disposition != status:
            raise ValueError(f"JEP-{jep} witness/release/decision drift")
        policy = by_priority.get(jep, {})
        if policy and int(policy["release"]) != release:
            raise ValueError(f"JEP-{jep} policy release drift")
        pending = by_residue.get(jep)
        if status in jep_residue.PENDING and pending is None:
            raise ValueError(f"JEP-{jep} pending residue lost")
        if status not in jep_residue.PENDING and pending is not None:
            raise ValueError(f"JEP-{jep} residue illegally promotes a decided JEP")
        if seen.witness_state in source_witness.BAD_STATES:
            gate = "SOURCE_PROVENANCE_BLOCKED"
            action = "REPAIR_EXACT_PRODUCT_HASH_OR_RECEIPT"
        elif status.startswith("reject"):
            gate = "NO_JAVA21_DIRECT_IMPORT"
            action = "PRESERVE_JAVA21_LANGUAGE_AND_API"
        elif status.startswith("superseded"):
            gate = "SUPERSEDED_NO_DIRECT_IMPORT"
            successor = catalogue_row.get("superseded_by", "")
            if not successor or int(successor) not in by_witness:
                raise ValueError(f"JEP-{jep} missing superseding released JEP")
            action = "FOLLOW_JEP_" + successor
        elif status.startswith("hold"):
            gate = "EXPLICIT_POLICY_HOLD"
            action = pending.receipt_next_action or pending.next_action
        elif status in CANDIDATES:
            # A verified single product file is not complete JEP coverage.
            gate = ("SOURCE_WITNESSED_NOT_QUALIFIED" if seen.product_hashes_matched
                    else "RECIPE_OR_SOURCE_PROOF_REQUIRED")
            action = (pending.receipt_next_action or pending.next_action)
        else:
            raise ValueError(f"unknown JEP disposition: {status}")
        rows.append(JepMatrixRow(
            release=release, jep=jep, title=catalogue_row["title"],
            domain=catalogue_row["domain"],
            problem_family=problem_family(catalogue_row["domain"]),
            disposition=status, source_state=seen.witness_state,
            packet_count=seen.packet_count, receipt_count=seen.receipt_count,
            product_hashes_matched=seen.product_hashes_matched,
            java21_default=policy.get("default_java21", "UNREVIEWED"),
            policy_classification=policy.get("classification", "UNREVIEWED"),
            implementation_gate=gate,
            required_proofs=required_proofs(catalogue_row["domain"], status, jep),
            next_action=action,
        ))
    return rows


def render_tsv(items: Sequence[JepMatrixRow]) -> str:
    out = io.StringIO(newline="")
    writer = csv.DictWriter(out, fieldnames=FIELDS, delimiter="\t", lineterminator="\n")
    writer.writeheader()
    for row in items:
        writer.writerow(asdict(row))
    return out.getvalue()


def report(items: Sequence[JepMatrixRow]) -> dict[str, object]:
    payload = render_tsv(items)
    return {
        "schema": "M3_ALL_RELEASED_JEPS_SOURCE_AND_PROOF_V1",
        "released_jep_count": len(items),
        "releases": dict(sorted(Counter(str(x.release) for x in items).items())),
        "implementation_gates": dict(sorted(Counter(x.implementation_gate for x in items).items())),
        "source_witnesses": dict(sorted(Counter(x.source_state for x in items).items())),
        "tsv_sha256": hashlib.sha256(payload.encode("utf-8")).hexdigest(),
        "all_java21_compatibility_proven": False,
        "all_features_implemented": False,
        "promotion_authorized": False,
    }


def work(root: Path) -> list[JepMatrixRow]:
    root = root.resolve(strict=True)
    authority = release_jep_authority.verify_repository_authority(root)
    backports = root / "m3" / "backports"
    catalogue = source_witness.read_catalogue(backports / "JEP_CATALOGUE.tsv")
    priority = jep_residue.read_tsv(backports / "POST21_PRIORITY_COMPATIBILITY.tsv")
    rows = assemble(catalogue, authority, source_witness.inspect(root, catalogue),
                    priority, jep_residue.queue(root, catalogue, priority))
    if len(rows) != release_jep_authority.EXPECTED_TOTAL:
        raise ValueError("85 released JEPs must be fully represented")
    return rows


def _write(root: Path, requested: Path, content: str) -> None:
    target = source_witness.checked_output_path(root, requested)
    target.parent.mkdir(parents=True, exist_ok=True)
    # The matrix can be regenerated in every Maven verify; same bytes are a fixed point.
    target.write_text(content, encoding="utf-8", newline="")


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", required=True, type=Path)
    parser.add_argument("--out", type=Path)
    parser.add_argument("--json-out", type=Path)
    parser.add_argument("--fail-on-source-drift", action="store_true")
    args = parser.parse_args(argv)
    root = args.root.resolve(strict=True)
    rows = work(root)
    encoded = render_tsv(rows)
    summary = json.dumps(report(rows), sort_keys=True, indent=2) + "\n"
    destinations = [x for x in (args.out, args.json_out) if x is not None]
    checked = [source_witness.checked_output_path(root, x) for x in destinations]
    if len(checked) != len(set(checked)):
        raise ValueError("JEP matrix outputs must be distinct paths")
    if args.out is None:
        print(encoded, end="")
    else:
        _write(root, args.out, encoded)
    if args.json_out is None:
        print(summary, file=sys.stderr, end="")
    else:
        _write(root, args.json_out, summary)
    if args.fail_on_source_drift and any(r.source_state in source_witness.BAD_STATES for r in rows):
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
