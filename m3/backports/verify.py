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


JDK_8359706_COMMIT = "b0831572e2cd9dbff9ee2abcdf81a493ddcecc7e"
JDK_8380236_COMMIT = "3a109f49feb19f313632be6a2aa24ba7d9b7269b"


def verify_open_fd_count_8359706_backport(root: Path) -> None:
    rows = read_tsv(root / "m3/backports/recipes/jdk-8359706/adaptation.tsv")
    if len(rows) != 8:
        raise AssertionError(
            f"expected 8 JDK-8359706 adaptation rows, found {len(rows)}"
        )

    by_target = {row["target_path"]: row for row in rows}
    if len(by_target) != len(rows):
        raise AssertionError("duplicate JDK-8359706 target path")

    for row in rows:
        if row["upstream_commit"] != JDK_8359706_COMMIT:
            raise AssertionError(
                f"unexpected JDK-8359706 donor commit for {row['target_path']}"
            )
        path = root / row["target_path"]
        if not path.is_file():
            raise AssertionError(
                f"missing JDK-8359706 target: {row['target_path']}"
            )
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        if actual != row["target_sha256"]:
            raise AssertionError(
                f"JDK-8359706 target drift for {row['target_path']}: "
                f"expected {row['target_sha256']}, actual {actual}"
            )
        if row["baseline_sha256"] == row["target_sha256"]:
            raise AssertionError(
                f"JDK-8359706 row has no delta: {row['target_path']}"
            )

    seeds = read_tsv(root / "m3/backports/UPSTREAM_CHANGE_SEEDS.tsv")
    seed = next((row for row in seeds if row["jbs"] == "JDK-8359706"), None)
    if seed is None:
        raise AssertionError("JDK-8359706 missing from upstream seed")
    if seed["upstream_commit"] != JDK_8359706_COMMIT:
        raise AssertionError("JDK-8359706 seed donor commit drift")
    if seed["disposition"] != "candidate-adapted":
        raise AssertionError(
            f"JDK-8359706 unexpected disposition: {seed['disposition']}"
        )
    followup = next((row for row in seeds if row["jbs"] == "JDK-8380236"), None)
    if followup is None:
        raise AssertionError("JDK-8380236 absorbed dependency missing from upstream seed")
    if followup["upstream_commit"] != JDK_8380236_COMMIT:
        raise AssertionError("JDK-8380236 seed donor commit drift")
    if followup["disposition"] != "absorbed-dependency":
        raise AssertionError(
            f"JDK-8380236 unexpected disposition: {followup['disposition']}"
        )

    linux = (root / "src/hotspot/os/linux/os_linux.cpp").read_text(encoding="utf-8")
    bsd = (root / "src/hotspot/os/bsd/os_bsd.cpp").read_text(encoding="utf-8")
    bsd_hpp = (root / "src/hotspot/os/bsd/os_bsd.hpp").read_text(encoding="utf-8")
    aix = (root / "src/hotspot/os/aix/os_aix.cpp").read_text(encoding="utf-8")
    windows = (root / "src/hotspot/os/windows/os_windows.cpp").read_text(encoding="utf-8")
    os_hpp = (root / "src/hotspot/share/runtime/os.hpp").read_text(encoding="utf-8")
    vm_error = (root / "src/hotspot/share/utilities/vmError.cpp").read_text(encoding="utf-8")
    test = (root / "test/jdk/sun/tools/jcmd/TestJcmdSanity.java").read_text(encoding="utf-8")
    packet_readme = (
        root / "m3/backports/recipes/jdk-8359706/README.md"
    ).read_text(encoding="utf-8")

    if 'opendir("/proc/self/fd")' not in linux:
        raise AssertionError("Linux descriptor counter missing")
    if "Open File Descriptors: unknown" not in linux:
        raise AssertionError("Linux procfs fallback missing")
    if "#include <libproc.h>" not in bsd or "proc_pidinfo(" not in bsd:
        raise AssertionError("macOS libproc descriptor counter missing")
    if "precond(buflen >= sizeof(struct proc_fdinfo))" not in bsd:
        raise AssertionError("JDK-8380236 macOS build repair missing")
    if "print_open_file_descriptors" not in bsd_hpp:
        raise AssertionError("macOS scratch-buffer contract missing")
    if "File descriptor counting not implemented on AIX" not in aix:
        raise AssertionError("AIX compatibility stub missing")
    if "File descriptor counting not supported on Windows" not in windows:
        raise AssertionError("Windows compatibility stub missing")
    if "static void print_open_file_descriptors(outputStream* st);" not in os_hpp:
        raise AssertionError("shared OS diagnostic contract missing")
    if vm_error.count("print_open_file_descriptors") < 2:
        raise AssertionError("VM.info/hs_err descriptor output join missing")
    if "8359706 8380236" not in test or "Open File Descriptors:" not in test:
        raise AssertionError("focused jcmd regression proof missing")
    if JDK_8380236_COMMIT not in packet_readme:
        raise AssertionError("macOS follow-up provenance missing")



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
    verify_open_fd_count_8359706_backport(root)
    print(
        "PASS: 82 JEP rows, non-JEP seed uniqueness, exact JDK-8357439 donor blobs, "
        "JDK-8347112 javadoc adaptation, JDK-8364182 serviceability adaptation, "
        "JDK-8374808 KeyStore Instant compatibility leaf, and JDK-8359706 open-FD diagnostics"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
