#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Read-only inventory of the existing M3 collection donation source-receipt chain.

Uses the unchanged original donor/transition admissions in verify_donation.py.
The script does not create successor receipts, rewrite source, update hashes,
suppress failing JNI tests, or issue an installed-module PASS.
"""
import argparse
import hashlib
import json
from pathlib import Path
import sys

HERE = Path(__file__).resolve().parent
COLLECTIONS = HERE.parent
sys.path.insert(0, str(COLLECTIONS))
import verify_donation as verifier  # noqa: E402


TRANSITIONS = (
    "include_filtered_view_successor",
    "include_stored_representative_successor",
    "include_path_mask_successor",
    "include_object_state_offset_successor",
    "include_tools4j_refinement_successor",
    "include_superset_source_successor",
    "include_master_source_successor",
    "include_module_source_successor",
    "include_current_context_source_successor",
    "include_current_receiver_source_successor",
    "include_current_probe_source_successor",
    "include_longlane_required_native_source_successor",
)


def exact_rows():
    """Replay the same historical source chain and refusal order as the real verifier."""
    packet = verifier.read_donation_packet()
    name = packet.get(
        "installed_source_manifest", "qualification/installed-source-enum-set-20261008.tsv"
    )
    relative = Path(name)
    if (relative.is_absolute() or ".." in relative.parts or
            "\\" in name or not name.startswith("qualification/")):
        raise ValueError("unsafe installed source-manifest location")
    manifest = COLLECTIONS / relative
    if (not manifest.is_file() or manifest.is_symlink() or
            verifier.digest(manifest) != packet["installed_source_manifest_sha256"]):
        raise ValueError("historical installed manifest custody drift")
    rows = []
    for line in manifest.read_text(encoding="utf-8").splitlines():
        if line and not line.startswith("#"):
            parts = line.split("\t")
            if len(parts) != 2:
                raise ValueError("malformed installed source receipt")
            rows.append((parts[0], parts[1]))
    for name in TRANSITIONS:
        rows = getattr(verifier, name)(rows)
    if len(rows) != len({path for path, _ in rows}):
        raise ValueError("duplicate source in historical receipt chain")
    for path, digest in rows:
        relative = Path(path)
        if (relative.is_absolute() or ".." in relative.parts or
                "\\" in path or not path.startswith("m3/collections/") or
                len(digest) != 64 or any(c not in "0123456789abcdef" for c in digest)):
            raise ValueError("unsafe source identity in sealed receipt chain: " + path)
    return rows


def guarded_source(path):
    """Use the original module-move resolver and refuse aliased source components."""
    result = verifier.located(path)
    try:
        relative = result.relative_to(verifier.REPO)
    except ValueError as error:
        raise ValueError("escaped collection source: " + path) from error
    current = verifier.REPO
    for part in relative.parts:
        current = current / part
        if current.is_symlink() or getattr(current, "is_junction", lambda: False)():
            return "ALIEN_ALIAS", None
    if not result.is_file():
        return "MISSING", None
    return "FILE", hashlib.sha256(result.read_bytes()).hexdigest()


def audit(expected, observed_hashes, actual_java):
    """Pure deterministic comparison; no disk writes or weakened acceptance."""
    expected = dict(expected)
    declared = {path for path in expected if path.endswith(".java") and "/src/" in path}
    missing = sorted(path for path in expected if path not in observed_hashes)
    changed = sorted(
        (path, expected[path], observed_hashes[path])
        for path in expected.keys() & observed_hashes.keys()
        if expected[path] != observed_hashes[path]
    )
    unlisted = sorted(actual_java - declared)
    unobserved = sorted(declared - actual_java)
    return {
        "expected": len(expected),
        "declared_java": len(declared),
        "observed": len(observed_hashes),
        "missing_paths": missing,
        "missing_source_preimages": [
            {"path": path, "sha256": expected[path]} for path in missing
        ],
        "source_digest_drift": [
            {"path": p, "expected": e, "actual": a} for p, e, a in changed
        ],
        "java_unlisted_in_receipts": unlisted,
        "java_declared_not_in_source_roots": unobserved,
    }


def mat_prefix_review_candidates(unobserved, unlisted):
    """Candidates only: never infer equivalent Java behavior from matching names.

    This reports matching parent-path + Mat-prefix removal, without changing
    the original source identities or authorizing any successor receipt.
    """
    current = set(unlisted)
    suggestions = []
    for original in sorted(unobserved):
        parent, separator, leaf = original.rpartition("/")
        if not separator or not leaf.startswith("Mat") or len(leaf) <= 3:
            continue
        candidate = parent + "/" + leaf[3:]
        if candidate in current:
            suggestions.append({"historical": original, "candidate": candidate,
                                "identity": "UNVERIFIED_SOURCE_SEMANTICS"})
    return suggestions


def unlisted_postimages(paths, read_digest):
    """Capture actual SHA-256 for source-review candidates without admitting them."""
    records = []
    for path in sorted(paths):
        category, digest = read_digest(path)
        if (category != "FILE" or digest is None or len(digest) != 64
                or any(c not in "0123456789abcdef" for c in digest)):
            raise ValueError("unlisted Java source is missing, aliased, or unreadable: " + path)
        records.append({"path": path, "sha256": digest})
    return records


def self_test():
    rows = [("m3/collections/src/main/java/jdk/a/Foo.java", "a" * 64),
            ("m3/collections/src/main/c/a.c", "b" * 64)]
    declared = {rows[0][0]}
    good = audit(rows, dict(rows), declared)
    assert not any(good[key] for key in (
        "missing_paths", "source_digest_drift", "java_unlisted_in_receipts",
        "java_declared_not_in_source_roots"))
    changed = audit(rows, {rows[0][0]: "c" * 64}, set())
    assert changed["missing_paths"] == [rows[1][0]]
    assert changed["missing_source_preimages"] == [
        {"path": rows[1][0], "sha256": "b" * 64}]
    assert changed["source_digest_drift"] == [
        {"path": rows[0][0], "expected": "a" * 64, "actual": "c" * 64}]
    assert changed["java_declared_not_in_source_roots"] == [rows[0][0]]
    new = audit(rows, dict(rows), declared | {"m3/collections/src/main/java/New.java"})
    assert new["java_unlisted_in_receipts"] == [
        "m3/collections/src/main/java/New.java"]
    mat_pair = mat_prefix_review_candidates(
        ["m3/collections/src/main/java/jdk/internal/m3/collections/mat/MatFoo.java"],
        ["m3/collections/src/main/java/jdk/internal/m3/collections/mat/Foo.java"])
    assert len(mat_pair) == 1 and mat_pair[0]["identity"] == "UNVERIFIED_SOURCE_SEMANTICS"
    assert not mat_prefix_review_candidates(
        ["m3/collections/src/main/java/jdk/internal/m3/collections/mat/MatFoo.java"],
        ["m3/collections/src/test/java/jdk/internal/m3/collections/mat/Foo.java"])
    fake = unlisted_postimages(
        ["m3/collections/src/main/java/Foo.java"],
        lambda path: ("FILE", "d" * 64))
    assert fake == [{"path": "m3/collections/src/main/java/Foo.java",
                     "sha256": "d" * 64}]
    try:
        unlisted_postimages(
            ["m3/collections/src/main/java/Foo.java"],
            lambda path: ("ALIEN_ALIAS", None))
        raise AssertionError("aliased unlisted source accepted")
    except ValueError:
        pass
    print("M3_COLLECTION_SOURCE_AUDIT_SELF_TEST_PASS cases=8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return 0
    try:
        rows = exact_rows()
        observed = {}
        aliases = []
        missing = []
        for path, _ in rows:
            category, digest = guarded_source(path)
            if category == "FILE":
                observed[path] = digest
            elif category == "ALIEN_ALIAS":
                aliases.append(path)
            else:
                missing.append(path)
        actual_java = {
            verifier.recorded(path)
            for path in (COLLECTIONS / "src").rglob("*.java")
        }
        for root in verifier.MOVED_JAVA_ROOTS:
            actual_java.update(
                verifier.recorded(path)
                for path in (verifier.REPO / root).rglob("*.java")
            )
        report = audit(rows, observed, actual_java)
        if report["missing_paths"] != sorted(missing + aliases):
            raise ValueError("source inventory discrepancy")
        report["aliased_paths"] = sorted(aliases)
        report["candidate_mat_prefix_renames"] = mat_prefix_review_candidates(
            report["java_declared_not_in_source_roots"],
            report["java_unlisted_in_receipts"])
        report["unlisted_java_postimages"] = unlisted_postimages(
            report["java_unlisted_in_receipts"], guarded_source)
        report["source_receipt_chain"] = list(TRANSITIONS)
        report["source_manifest_sha256"] = verifier.read_donation_packet()[
            "installed_source_manifest_sha256"]
        counts = {
            "source_digest_drift": len(report["source_digest_drift"]),
            "source_missing_or_aliased": len(report["missing_paths"]),
            "java_unlisted_in_receipts": len(report["java_unlisted_in_receipts"]),
            "java_declared_not_in_source_roots": len(
                report["java_declared_not_in_source_roots"])
        }
        report["counts"] = counts
        report["status"] = "PASS" if not any(counts.values()) else "HOLD"
        print(json.dumps(report, indent=2, sort_keys=True))
        print("M3_COLLECTION_SOURCE_INVENTORY_" + report["status"] +
              " " + " ".join(k + "=" + str(v) for k, v in counts.items()),
              file=sys.stderr)
        return 0 if report["status"] == "PASS" else 1
    except (ValueError, OSError, KeyError, SystemExit) as error:
        print("M3_COLLECTION_SOURCE_INVENTORY_REFUSED: " + str(error),
              file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
