#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed checks for the JDK 22–27 downstream backport catalogue."""

from __future__ import annotations

import argparse
import csv
import hashlib
from pathlib import Path

import release_jep_authority

EXPECTED_RELEASE_COUNTS = {22: 12, 23: 12, 24: 24, 25: 18, 26: 10, 27: 9}
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
    release_jep_authority.verify_repository_authority(root)
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

# The JDK21u update merge (f97e75f2; parents 3569c790, 8b241db2)
# retained the adapted recursive-copy leaf and introduced the upstream
# isIndexable() override and updated copyright header. The historical
# adaptation TSV remains immutable. This exact hash pair authorizes only
# the observed merged-successor source, not an arbitrary regenerated hash.
JDK_8347112_HANDLER_PATH = (
    "src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/"
    "formats/html/DocFilesHandlerImpl.java"
)
JDK_8347112_HANDLER_HISTORICAL_SHA256 = (
    "374269a8f8913fd88954c89cb55efbf14cc93ac1ecda8c290092cddb4036ebe7"
)
JDK_8347112_HANDLER_21U_SHA256 = (
    "e6a27731b1d92aaae05e9c64dd231fe3b5791759fb6022a009b104bd7b176d41"
)
JDK_8347112_HANDLER_21U_GIT_BLOB = (
    "e86016480f297dadc2ce5c729b10e8d2594dd787"
)


def _verified_javadoc_21u_successor(
    row: dict[str, str], actual: str, data: bytes
) -> bool:
    """Admit only the exact upstream-update successor of one historic Javadoc leaf."""
    return (
        row["target_path"] == JDK_8347112_HANDLER_PATH
        and row["target_sha256"] == JDK_8347112_HANDLER_HISTORICAL_SHA256
        and actual == JDK_8347112_HANDLER_21U_SHA256
        and hashlib.sha256(data).hexdigest() == actual
        and git_blob_sha1(data) == JDK_8347112_HANDLER_21U_GIT_BLOB
        and b"if (!configuration.shouldExcludeDocFileDir(srcfile.getName()))" in data
        and b"options.copyDocfileSubdirs()" not in data
        and b"public boolean isIndexable()" in data
    )


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
        data = path.read_bytes()
        actual = hashlib.sha256(data).hexdigest()
        if actual != row["target_sha256"] and not _verified_javadoc_21u_successor(
            row, actual, data
        ):
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

# Original JDK-8364182 adaptation remains immutable. This exact successive
# source state is inherited from JDK21u f97e75f2 + single-line SIMD commit cb0cc9a.
JDK_8364182_VMSYMBOLS_PATH = "src/hotspot/share/classfile/vmSymbols.hpp"
JDK_8364182_HISTORICAL_SHA256 = (
    "eeffc686c1eef000be5e3be5eb02e13257c744a63ea926762e9d4749fadecc67"
)
JDK_8364182_21U_SHA256 = (
    "691bd90288f30d9d25504526f234abc7db862e577544a7733d3fe0d09e585ff2"
)
JDK_8364182_21U_GIT_BLOB = "07a4452d5fabdf6f8f520421c3c668ae390bf666"
JDK_8364182_SIMDSORT_SHA256 = (
    "db65f0fdf1b3eba49a7819ab59a2c01f04b02c6e82daf8b55774d3c2404990aa"
)
JDK_8364182_SIMDSORT_GIT_BLOB = "65860effc9f00df74c1dd0dcea1e2c1605f54cfa"
JDK_8364182_SIMDSORT_SYMBOL = (
    b'template(java_util_DualPivotQuicksort,              '
    b'"java/util/DualPivotQuicksort")'
)


def _verified_jdk8364182_vmsymbols_successor(
    row: dict[str, str], data: bytes
) -> bool:
    """Admit only exact 21u vmSymbols or the one-line SIMD descendant."""
    if (
        row["target_path"] != JDK_8364182_VMSYMBOLS_PATH
        or row["target_sha256"] != JDK_8364182_HISTORICAL_SHA256
        or row.get("superseded_target_sha256") !=
            "4f752ab79abf854d144e53118a2cc57a4b3cc2432140e9297079cca86dd2abab"
    ):
        return False
    required = (
        b"template(serializeSecurityPropertiesToByteArray_name,",
        b"template(java_lang_M3StringImmediate,",
        b"template(java_lang_M3String,",
        b"template(java_util_Arrays,",
    )
    if any(data.count(marker) != 1 for marker in required):
        return False
    sha = hashlib.sha256(data).hexdigest()
    blob = git_blob_sha1(data)
    if sha == JDK_8364182_21U_SHA256:
        return blob == JDK_8364182_21U_GIT_BLOB
    if sha != JDK_8364182_SIMDSORT_SHA256:
        return False
    if blob != JDK_8364182_SIMDSORT_GIT_BLOB:
        return False
    matches = [
        line for line in data.splitlines(keepends=True)
        if JDK_8364182_SIMDSORT_SYMBOL in line
    ]
    if len(matches) != 1:
        return False
    previous = data.replace(matches[0], b"", 1)
    return (
        hashlib.sha256(previous).hexdigest() == JDK_8364182_21U_SHA256
        and git_blob_sha1(previous) == JDK_8364182_21U_GIT_BLOB
    )



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
        source_data = path.read_bytes()
        actual = hashlib.sha256(source_data).hexdigest()
        baseline = row["baseline_sha256"]
        superseded = row.get("superseded_target_sha256", "")
        if row["target_path"] == "src/hotspot/share/classfile/vmSymbols.hpp":
            if row["target_sha256"] != JDK_8364182_HISTORICAL_SHA256:
                raise AssertionError("JDK-8364182 historical vmSymbols pin drift")
            expected_superseded = (
                "4f752ab79abf854d144e53118a2cc57a4b3cc2432140e9297079cca86dd2abab"
            )
            if superseded != expected_superseded:
                raise AssertionError("JDK-8364182 vmSymbols superseded pin drift")
        elif superseded:
            raise AssertionError(
                "unexpected JDK-8364182 superseded target: " + row["target_path"]
            )
        if superseded:
            if len(superseded) != 64 or any(
                char not in "0123456789abcdef" for char in superseded
            ):
                raise AssertionError(
                    "invalid JDK-8364182 superseded SHA-256: " + row["target_path"]
                )
            if superseded in {baseline, row["target_sha256"]}:
                raise AssertionError(
                    "JDK-8364182 superseded state aliases a pinned state: "
                    + row["target_path"]
                )
        allowed_targets = {row["target_sha256"]}
        if superseded:
            allowed_targets.add(superseded)
        if _verified_jdk8364182_vmsymbols_successor(row, source_data):
            allowed_targets.add(actual)
        if actual not in allowed_targets:
            raise AssertionError(
                f"JDK-8364182 target drift for {row['target_path']}: "
                f"expected {row['target_sha256']}"
                + (f" or exact superseded {superseded}" if superseded else "")
                + f", actual {actual}"
            )
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



def verify_community_fork_catalog(root: Path) -> None:
    rows = read_tsv(root / "m3/backports/COMMUNITY_FORKS.tsv")
    ids = [row["fork_id"] for row in rows]
    if len(ids) != len(set(ids)):
        raise AssertionError("duplicate community fork id")
    if rows[0]["fork_id"] != "upstream21u":
        raise AssertionError("community fork baseline row must be upstream21u")
    if rows[0]["repository"] != "openjdk/jdk21u" or rows[0]["ref"] != "master":
        raise AssertionError("community fork baseline must be openjdk/jdk21u master")

    required_products = {
        "corretto",
        "sapmachine",
        "microsoft",
        "tencent-kona",
        "dragonwell-standard",
        "jetbrains-runtime",
    }
    missing = required_products - set(ids)
    if missing:
        raise AssertionError(f"community fork catalogue missing required product rows: {sorted(missing)}")

    for row in rows:
        if not row["repository"] or "/" not in row["repository"]:
            raise AssertionError(f"invalid community fork repository: {row['fork_id']}")
        if not row["ref"]:
            raise AssertionError(f"blank community fork ref: {row['fork_id']}")
        if len(row["pinned_head"]) != 40 or any(
            ch not in "0123456789abcdef" for ch in row["pinned_head"]
        ):
            raise AssertionError(f"invalid community fork pinned head: {row['fork_id']}")
        if row["source_copy_authority"] != "false":
            raise AssertionError(f"community fork source-copy authority granted: {row['fork_id']}")
        if row["selected_for_distribution"] != "false":
            raise AssertionError(f"community fork distribution authority granted: {row['fork_id']}")
        if row["license_policy"] != "REVIEW_REPOSITORY_AND_PATH_LICENSES" and row["fork_id"] != "upstream21u":
            raise AssertionError(f"community fork license policy drift: {row['fork_id']}")

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
    verify_community_fork_catalog(root)
    verify_jcmd_backport(root)
    verify_javadoc_8347112_backport(root)
    verify_security_properties_8364182_backport(root)
    verify_keystore_instant_8374808_backport(root)
    print(
        "PASS: 85 authoritative JEP rows, non-JEP seed uniqueness, community fork catalogue authority, "
        "exact JDK-8357439 donor blobs, "
        "JDK-8347112 javadoc adaptation, JDK-8364182 serviceability adaptation, "
        "and JDK-8374808 KeyStore Instant compatibility leaf"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
