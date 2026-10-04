#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed checks for the JDK 22–27 downstream backport catalogue."""

from __future__ import annotations

import argparse
import csv
import hashlib
from pathlib import Path

EXPECTED_RELEASE_COUNTS = {22: 12, 23: 12, 24: 22, 25: 17, 26: 10, 27: 9}
EXPECTED_TOTAL = sum(EXPECTED_RELEASE_COUNTS.values())


def git_blob_sha1(data: bytes) -> str:
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows:
        raise AssertionError(f"empty TSV: {path}")
    return rows


def verify_jeps(root: Path) -> None:
    rows = read_tsv(root / "m3/backports/JEP_CATALOGUE.tsv")
    if len(rows) != EXPECTED_TOTAL:
        raise AssertionError(f"expected {EXPECTED_TOTAL} JEP rows, found {len(rows)}")

    seen: set[int] = set()
    counts = {release: 0 for release in EXPECTED_RELEASE_COUNTS}
    catalog_numbers: set[int] = set()

    for row in rows:
        release = int(row["release"])
        jep = int(row["jep"])
        if release not in counts:
            raise AssertionError(f"unexpected release {release} for JEP {jep}")
        if jep in seen:
            raise AssertionError(f"duplicate JEP {jep}")
        seen.add(jep)
        catalog_numbers.add(jep)
        counts[release] += 1

        if row["domain"] == "language" and row["disposition"] != "reject-language":
            raise AssertionError(f"language JEP {jep} is not reject-language")

    if counts != EXPECTED_RELEASE_COUNTS:
        raise AssertionError(f"release counts differ: {counts}")

    for row in rows:
        superseded = row["superseded_by"].strip()
        if superseded and int(superseded) not in catalog_numbers:
            raise AssertionError(
                f"JEP {row['jep']} superseded_by {superseded} is outside catalogue"
            )


def verify_seed(root: Path) -> None:
    rows = read_tsv(root / "m3/backports/UPSTREAM_CHANGE_SEEDS.tsv")
    ids = [row["jbs"] for row in rows]
    if len(ids) != len(set(ids)):
        raise AssertionError("duplicate JBS ID in upstream change seeds")
    for row in rows:
        if not row["jbs"].startswith("JDK-"):
            raise AssertionError(f"invalid JBS ID: {row['jbs']}")


def verify_jcmd_backport(root: Path) -> None:
    manifest = read_tsv(root / "m3/backports/recipes/jdk-8357439/manifest.tsv")
    if len(manifest) != 2:
        raise AssertionError(f"expected 2 jcmd manifest rows, found {len(manifest)}")
    for row in manifest:
        if row["preimage"] != "ABSENT":
            raise AssertionError(f"unexpected preimage state for {row['path']}")
        path = root / row["path"]
        if not path.is_file():
            raise AssertionError(f"missing backport target: {row['path']}")
        actual = git_blob_sha1(path.read_bytes())
        expected = row["upstream_git_blob"]
        if actual != expected:
            raise AssertionError(
                f"blob drift for {row['path']}: expected {expected}, actual {actual}"
            )



JDK_8347112_COMMIT = "b221cb6ba138672802644f37eebf368521a0a6f4"


def verify_javadoc_8347112_backport(root: Path) -> None:
    rows = read_tsv(root / "m3/backports/recipes/jdk-8347112/adaptation.tsv")
    if len(rows) != 5:
        raise AssertionError(f"expected 5 JDK-8347112 adaptation rows, found {len(rows)}")

    by_target = {row["target_path"]: row for row in rows}
    if len(by_target) != len(rows):
        raise AssertionError("duplicate JDK-8347112 target path")

    for row in rows:
        if row["upstream_commit"] != JDK_8347112_COMMIT:
            raise AssertionError(
                f"unexpected JDK-8347112 donor commit for {row['target_path']}"
            )
        path = root / row["target_path"]
        if not path.is_file():
            raise AssertionError(f"missing JDK-8347112 target: {row['target_path']}")
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        if actual != row["target_sha256"]:
            raise AssertionError(
                f"JDK-8347112 target drift for {row['target_path']}: "
                f"expected {row['target_sha256']}, actual {actual}"
            )
        if row["baseline_sha256"] == row["target_sha256"]:
            raise AssertionError(f"JDK-8347112 row has no delta: {row['target_path']}")

    seeds = read_tsv(root / "m3/backports/UPSTREAM_CHANGE_SEEDS.tsv")
    seed = next((row for row in seeds if row["jbs"] == "JDK-8347112"), None)
    if seed is None:
        raise AssertionError("JDK-8347112 missing from upstream seed")
    if seed["upstream_commit"] != JDK_8347112_COMMIT:
        raise AssertionError("JDK-8347112 seed donor commit drift")
    if seed["disposition"] != "candidate-adapted":
        raise AssertionError(
            f"JDK-8347112 unexpected disposition: {seed['disposition']}"
        )

    handler = (
        root
        / "src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/formats/html/"
        "DocFilesHandlerImpl.java"
    ).read_text(encoding="utf-8")
    if "options.copyDocfileSubdirs()" in handler:
        raise AssertionError("JDK-8347112 recursive copy still gated by legacy option")
    if "if (!configuration.shouldExcludeDocFileDir(srcfile.getName()))" not in handler:
        raise AssertionError("JDK-8347112 default-recursion leaf missing")

    base_options = (
        root
        / "src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/toolkit/"
        "BaseOptions.java"
    ).read_text(encoding="utf-8")
    if "copyDocfileSubdirs" not in base_options or '"-docfilessubdirs"' not in base_options:
        raise AssertionError("Java 21 -docfilessubdirs compatibility contract was removed")

    config = (
        root
        / "src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/toolkit/"
        "BaseConfiguration.java"
    ).read_text(encoding="utf-8")
    if 'excludedDocFileDirs.contains("*")' not in config:
        raise AssertionError("JDK-8347112 wildcard exclusion leaf missing")

    test = (
        root / "test/langtools/jdk/javadoc/doclet/testCopyFiles/TestCopyFiles.java"
    ).read_text(encoding="utf-8")
    if "testDocFilesInPackagesWithWildcardExclusion" not in test:
        raise AssertionError("JDK-8347112 wildcard regression test missing")
    if '"-docfilessubdirs"' not in test:
        raise AssertionError("legacy -docfilessubdirs regression coverage was lost")


JDK_8364182_COMMIT = "f2f8828188f45d16344c82adfbf951f7409b8825"


def verify_security_properties_8364182_backport(root: Path) -> None:
    rows = read_tsv(root / "m3/backports/recipes/jdk-8364182/adaptation.tsv")
    if len(rows) != 7:
        raise AssertionError(
            f"expected 7 JDK-8364182 adaptation rows, found {len(rows)}"
        )

    by_target = {row["target_path"]: row for row in rows}
    if len(by_target) != len(rows):
        raise AssertionError("duplicate JDK-8364182 target path")

    for row in rows:
        if row["upstream_commit"] != JDK_8364182_COMMIT:
            raise AssertionError(
                f"unexpected JDK-8364182 donor commit for {row['target_path']}"
            )
        path = root / row["target_path"]
        if not path.is_file():
            raise AssertionError(
                f"missing JDK-8364182 target: {row['target_path']}"
            )
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        if actual != row["target_sha256"]:
            raise AssertionError(
                f"JDK-8364182 target drift for {row['target_path']}: "
                f"expected {row['target_sha256']}, actual {actual}"
            )
        baseline = row["baseline_sha256"]
        if baseline != "ABSENT" and baseline == row["target_sha256"]:
            raise AssertionError(
                f"JDK-8364182 row has no delta: {row['target_path']}"
            )

    seeds = read_tsv(root / "m3/backports/UPSTREAM_CHANGE_SEEDS.tsv")
    seed = next((row for row in seeds if row["jbs"] == "JDK-8364182"), None)
    if seed is None:
        raise AssertionError("JDK-8364182 missing from upstream seed")
    if seed["upstream_commit"] != JDK_8364182_COMMIT:
        raise AssertionError("JDK-8364182 seed donor commit drift")
    if seed["disposition"] != "candidate-adapted":
        raise AssertionError(
            f"JDK-8364182 unexpected disposition: {seed['disposition']}"
        )

    header = (
        root / "src/hotspot/share/services/diagnosticCommand.hpp"
    ).read_text(encoding="utf-8")
    if 'name() { return "VM.security_properties"; }' not in header:
        raise AssertionError("VM.security_properties command declaration missing")
    if 'name() { return "VM.system_properties"; }' not in header:
        raise AssertionError("VM.system_properties compatibility command missing")

    implementation = (
        root / "src/hotspot/share/services/diagnosticCommand.cpp"
    ).read_text(encoding="utf-8")
    if "PrintSecurityPropertiesDCmd" not in implementation:
        raise AssertionError("VM.security_properties registration/execute path missing")
    if "PrintSystemPropertiesDCmd" not in implementation:
        raise AssertionError("VM.system_properties implementation path missing")

    support = (
        root / "src/java.base/share/classes/jdk/internal/vm/VMSupport.java"
    ).read_text(encoding="utf-8")
    if "serializeSecurityPropertiesToByteArray" not in support:
        raise AssertionError("security properties serialization bridge missing")



JDK_8374808_COMMIT = "264fdc5b4ed5f4e35168048533196e670c3dda6c"


def verify_keystore_instant_8374808_backport(root: Path) -> None:
    rows = read_tsv(root / "m3/backports/recipes/jdk-8374808/adaptation.tsv")
    if len(rows) != 3:
        raise AssertionError(
            f"expected 3 JDK-8374808 adaptation rows, found {len(rows)}"
        )

    by_target = {row["target_path"]: row for row in rows}
    if len(by_target) != len(rows):
        raise AssertionError("duplicate JDK-8374808 target path")

    for row in rows:
        if row["upstream_commit"] != JDK_8374808_COMMIT:
            raise AssertionError(
                f"unexpected JDK-8374808 donor commit for {row['target_path']}"
            )
        path = root / row["target_path"]
        if not path.is_file():
            raise AssertionError(
                f"missing JDK-8374808 target: {row['target_path']}"
            )
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        if actual != row["target_sha256"]:
            raise AssertionError(
                f"JDK-8374808 target drift for {row['target_path']}: "
                f"expected {row['target_sha256']}, actual {actual}"
            )
        baseline = row["baseline_sha256"]
        if baseline != "ABSENT" and baseline == row["target_sha256"]:
            raise AssertionError(
                f"JDK-8374808 row has no delta: {row['target_path']}"
            )

    seeds = read_tsv(root / "m3/backports/UPSTREAM_CHANGE_SEEDS.tsv")
    seed = next((row for row in seeds if row["jbs"] == "JDK-8374808"), None)
    if seed is None:
        raise AssertionError("JDK-8374808 missing from upstream seed")
    if seed["upstream_commit"] != JDK_8374808_COMMIT:
        raise AssertionError("JDK-8374808 seed donor commit drift")
    if seed["disposition"] != "candidate-adapted":
        raise AssertionError(
            f"JDK-8374808 unexpected disposition: {seed['disposition']}"
        )

    key_store = (
        root / "src/java.base/share/classes/java/security/KeyStore.java"
    ).read_text(encoding="utf-8")
    if "public final Instant getCreationInstant(String alias)" not in key_store:
        raise AssertionError("KeyStore.getCreationInstant API missing")
    if "public final Date getCreationDate(String alias)" not in key_store:
        raise AssertionError("legacy KeyStore.getCreationDate API was removed")

    spi = (
        root / "src/java.base/share/classes/java/security/KeyStoreSpi.java"
    ).read_text(encoding="utf-8")
    if "public Instant engineGetCreationInstant(String alias)" not in spi:
        raise AssertionError("KeyStoreSpi.engineGetCreationInstant API missing")
    if "return date == null ? null : date.toInstant();" not in spi:
        raise AssertionError("legacy Date-to-Instant SPI adapter missing")
    if "public abstract Date engineGetCreationDate(String alias);" not in spi:
        raise AssertionError("legacy KeyStoreSpi creation-date API was removed")

    test = (
        root / "test/jdk/java/security/KeyStore/CreationInstant.java"
    ).read_text(encoding="utf-8")
    if "LegacyDateSpi" not in test or "getCreationInstant" not in test:
        raise AssertionError("focused KeyStore Instant compatibility test missing")

    for forbidden in (
        "src/java.base/share/classes/sun/security/provider/JavaKeyStore.java",
        "src/java.base/share/classes/com/sun/crypto/provider/JceKeyStore.java",
        "src/java.base/share/classes/sun/security/pkcs12/PKCS12KeyStore.java",
    ):
        if forbidden in by_target:
            raise AssertionError(
                f"provider-storage rewrite escaped compatibility leaf: {forbidden}"
            )


def verify_passes_and_work_queue(root: Path) -> None:
    passes = read_tsv(root / "m3/backports/BACKPORT_PASSES.tsv")
    if [int(row["ordinal"]) for row in passes] != list(range(len(passes))):
        raise AssertionError("backport pass ordinals are not contiguous")
    pass_ids = [row["pass_id"] for row in passes]
    if len(pass_ids) != len(set(pass_ids)):
        raise AssertionError("duplicate backport pass id")
    for row in passes:
        if not row["stop_condition"].strip():
            raise AssertionError(f"empty stop condition for pass {row['pass_id']}")
        mutation = row["mutation_authority"].strip().lower()
        if mutation not in {"true", "false"}:
            raise AssertionError(f"invalid mutation_authority for pass {row['pass_id']}")
    mutating = [row["pass_id"] for row in passes if row["mutation_authority"] == "true"]
    if mutating != ["apply"]:
        raise AssertionError(f"only apply may mutate, found {mutating}")

    queue = read_tsv(root / "m3/backports/BACKPORT_WORK_QUEUE.tsv")
    keys = [(row["source_type"], row["identity"]) for row in queue]
    if len(keys) != len(set(keys)):
        raise AssertionError("duplicate backport work-queue identity")

    expected_jeps = {
        f"JEP-{row['jep']}" for row in read_tsv(root / "m3/backports/JEP_CATALOGUE.tsv")
    }
    queued_jeps = {row["identity"] for row in queue if row["source_type"] == "JEP"}
    if queued_jeps != expected_jeps:
        missing = sorted(expected_jeps - queued_jeps)
        extra = sorted(queued_jeps - expected_jeps)
        raise AssertionError(f"JEP queue mismatch missing={missing} extra={extra}")

    expected_jbs = {
        row["jbs"] for row in read_tsv(root / "m3/backports/UPSTREAM_CHANGE_SEEDS.tsv")
    }
    queued_jbs = {row["identity"] for row in queue if row["source_type"] == "JBS"}
    if not expected_jbs.issubset(queued_jbs):
        raise AssertionError(
            f"JBS queue missing seeded items: {sorted(expected_jbs - queued_jbs)}"
        )

    allowed_scopes = {
        "FILE",
        "VISIBILITY",
        "PACKAGE",
        "MODULE",
        "MULTI_MODULE",
        "LIBRARY_API",
    }
    for row in queue:
        current_pass = int(row["current_pass"])
        if current_pass < 0 or current_pass >= len(passes):
            raise AssertionError(
                f"invalid current_pass {current_pass} for {row['identity']}"
            )
        if row["required_scope"] not in allowed_scopes:
            raise AssertionError(
                f"invalid required_scope {row['required_scope']} for {row['identity']}"
            )
        disposition = row["disposition"]
        action = row["action"]
        if disposition.startswith("reject-") and not action.startswith("EXCLUDE_"):
            raise AssertionError(f"rejected item is not excluded: {row['identity']}")
        if disposition.startswith("superseded") and action != "REDIRECT_SUPERSEDED":
            raise AssertionError(f"superseded item is not redirected: {row['identity']}")

    for identity in {
        "JDK-8347112",
        "JDK-8364182",
        "JDK-8367584",
        "JDK-8368692",
        "JDK-8374808",
    }:
        row = next((item for item in queue if item["identity"] == identity), None)
        if row is None:
            raise AssertionError(f"materialized backport missing from work queue: {identity}")
        if int(row["current_pass"]) < 5 or row["action"] != "VERIFY_HASH_PINNED_RECIPE":
            raise AssertionError(f"materialized backport not in verification pass: {identity}")
        if row["required_scope"] != "LIBRARY_API":
            raise AssertionError(f"public/tool contract backport lacks LIBRARY_API authority: {identity}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[2],
        help="repository root",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    root = args.root.resolve()
    verify_jeps(root)
    verify_seed(root)
    verify_jcmd_backport(root)
    verify_javadoc_8347112_backport(root)
    verify_security_properties_8364182_backport(root)
    verify_keystore_instant_8374808_backport(root)
    verify_passes_and_work_queue(root)
    print(
        "PASS: 82 JEP rows, non-JEP seed uniqueness, exact JDK-8357439 donor blobs, "
        "JDK-8347112 javadoc adaptation, JDK-8364182 serviceability adaptation, "
        "JDK-8374808 KeyStore Instant compatibility leaf, and resumable pass/work queue"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
