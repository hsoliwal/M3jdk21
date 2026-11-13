#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
"""Verify every restored JMOD SDK source blob against its exact PR #788 Git identity.

This is source custody, not Java 21 compatibility, image runtime, or release promotion.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path, PurePosixPath

HEADER = "path\tgit_blob_sha1\tsource_pr\tsource_commit"
SOURCE_COMMIT = "3c7f331a65e31fdbe4c198d79c7bfcb4c226cda2"
PREFIXES = ("make/m3/tooling/jmod-recipes/", "m3/vendor/third_party-m3/")

# The PR #788 receipt remains immutable. These two exact successors were emitted
# and qualified by the Synexia text/JNI recipe, then received in target PR #855.
# This is a finite source-custody transition, never permission to refresh hashes.
SUCCESSOR_COMMIT = "c49cca384837ebd9dc71b6141c71ab0bcb938d10"
SUCCESSORS = {
    "make/m3/tooling/jmod-recipes/src/main/java/jdk/m3/rewrite/jmod/M3ModuleDescriptorRecipe.java": (
        "83b7ecc23b00c2fe026de54c8fa03ce9000612d3",
        "9882f9773cc299ef80477fbf4de080ba763ab781",
        "m3-openrewrite-recipes/src/main/java/jdk/m3/rewrite/jmod/M3ModuleDescriptorRecipe.java",
    ),
    "make/m3/tooling/jmod-recipes/tools/jmod_pack.py": (
        "6824ea4419d769b71af846c5b6eee64f9a7454d1",
        "1b20225a72caf20a7c02f9ef1ca2cb068294bff6",
        "m3-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017/datasets/m3jdk21-build/resources/jdk/internal/m3/rewrite/hash-pinned-text/m3-ast-build-reverse-20261017-m3jdk21-build/0001-jmod_pack.py.txt",
    ),
    # Exact source-qualified successors from integration packet A.
    "make/m3/tooling/jmod-recipes/src/main/java/jdk/m3/rewrite/jmod/M3ModuleDescriptorApply.java": (
        "4cbc985224b08979d4834d59f3ac1fb71a76fcb8",
        "4555e7423b3b1696518067141cf1540457764b9e",
        "m3-openrewrite-recipes/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/m3-nlp-search-integration-20261018/0080-M3ModuleDescriptorApply.java.txt",
    ),
    "make/m3/tooling/jmod-recipes/src/test/java/jdk/m3/rewrite/jmod/M3ModuleDescriptorRecipeTest.java": (
        "4f0a50066835ec3db08969f71d932f45c2d5ba22",
        "2e89346c5fab68ef33bc7d1132841036cca83089",
        "m3-openrewrite-recipes/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/m3-nlp-search-integration-20261018/0081-M3ModuleDescriptorRecipeTest.java.txt",
    ),
}

# The shared-frontend source-qualified successor is already in the retained target tree.
# Preserve the earlier text/JNI identity as ancestry evidence; exact current bytes still refuse drift.
SUCCESSOR_COMMITS = {
    "make/m3/tooling/jmod-recipes/tools/jmod_pack.py": "c05613123249b74ac4243fe0b924b68494116931",
    # Exact source-qualified successors from integration packet A.
    "make/m3/tooling/jmod-recipes/src/main/java/jdk/m3/rewrite/jmod/M3ModuleDescriptorApply.java": "c4ae7e361c9253aaced47e843549a06e505d924d",
    "make/m3/tooling/jmod-recipes/src/test/java/jdk/m3/rewrite/jmod/M3ModuleDescriptorRecipeTest.java": "c4ae7e361c9253aaced47e843549a06e505d924d",
}
PREVIOUS_SUCCESSORS = {
    "make/m3/tooling/jmod-recipes/tools/jmod_pack.py": "83b1483c6c5e6a39c519f82af603382914eed3e5",
}


# Current M3JDK21-owned source evolutions, layered over the immutable PR #788
# ledger and the earlier Synexia source-qualified successors. These are exact
# source identities; neither recipe-boot policy nor image/runtime is admitted.
CURRENT_SOURCE_SUCCESSORS = {
    "make/m3/tooling/jmod-recipes/pom.xml": (
        "bea579cf4ffe9ebfa6cbe8bfd445b4f96dce9543",
        "5e330226ed95bebe3870fe2bd6d0913e69f0753a",
        "49791e92b1c063a2cb65875831976cc9c599e8e0",
        "PENDING_RECIPE_BOOT_POLICY_AND_MAVEN",
    ),
    "make/m3/tooling/jmod-recipes/tools/descriptor_gen.py": (
        "fb25328725a1ccd6398083c43e8d851eacbb0b74",
        "43e21664d531443095106e7605c8e4d6eaa039c4",
        "87e01c7d4045c6e321aef3c76522c781b0667806",
        "PENDING_DESCRIPTOR_RELEASE_BEHAVIOR",
    ),
    "make/m3/tooling/jmod-recipes/tools/jmod_pack.py": (
        "1b20225a72caf20a7c02f9ef1ca2cb068294bff6",
        "31def48398799524beeddf912dcbc05bc263f826",
        "6ac06f20bcbdce518581b5c17c9e8b85c3752d96",
        "PENDING_ZIP_JMOD_RUNTIME_AND_PLATFORM",
    ),
}


# The current packager additionally retains target-owned archive/release evolution
# 4ecd7758577a1802335c5b4c7c6a292e9490f3c5 at 87e01c7d4045c6e321aef3c76522c781b0667806.
# Its opt-in default-resolution successor 31def48398799524beeddf912dcbc05bc263f826
# is the exact canonical packed-integers output at e2bb24902ccd971cc6a7611658557c732e6ef178.
# Source custody preserves this intermediate lineage; policy/platform/runtime
# promotion remains pending and the receipt's acceptance flags remain false.

# Original PR #788 custody: immutable 52-file source ledger, not merely a
# replaceable table of individually matching Git blobs. These are the original
# target Git-object and SHA-256 identities at M3JDK 21 master 0ab28ea86ccb.
ORIGINAL_LEDGER_RELATIVE = "make/m3/tooling/jmod-recipes/RECEIVED_SDK_JMOD_BLOBS.tsv"
ORIGINAL_LEDGER_GIT_BLOB = "9c7b294e35a14c057d4431d96deb5c6f49ecc9e3"
ORIGINAL_LEDGER_SHA256 = "09662cb722cb36a19d22f4e020eaed5771177b485756b921c04c6f339939da46"


def read_original_ledger(root, ledger, expected_count):
    """Require the exact canonical ledger before checking any 52 product sources.

    Smaller explicit denominators are the existing unit-test-only seam: no
    smaller result can claim original PR #788 source ledger authority.
    """
    candidate = Path(ledger).absolute()
    if expected_count != 52:
        return candidate.read_bytes()
    canonical = root / ORIGINAL_LEDGER_RELATIVE
    if candidate != canonical:
        raise ValueError("SDK_RECEIVER_LEDGER_PATH_DRIFT")
    current = root
    for name in ORIGINAL_LEDGER_RELATIVE.split("/"):
        current = current / name
        if current.is_symlink():
            raise ValueError("SDK_RECEIVER_LEDGER_UNSAFE_PATH")
    if not current.is_file():
        raise ValueError("SDK_RECEIVER_LEDGER_MISSING")
    content = current.read_bytes()
    if (git_blob_sha1(content) != ORIGINAL_LEDGER_GIT_BLOB
            or hashlib.sha256(content).hexdigest() != ORIGINAL_LEDGER_SHA256):
        raise ValueError("SDK_RECEIVER_LEDGER_IDENTITY_DRIFT")
    return content


def rows_from_text(text, expected_count=52):
    lines = text.splitlines()
    if not lines or lines[0] != HEADER:
        raise ValueError("SDK_RECEIVER_INVALID_HEADER")
    seen, aliases, rows = set(), set(), []
    for line in lines[1:]:
        cells = line.split("\t")
        if len(cells) != 4:
            raise ValueError("SDK_RECEIVER_INVALID_ROW")
        name, blob, pr, commit = cells
        # Inspect the raw path: PurePosixPath.parts silently normalizes // and /./.
        # Source custody must bind one literal Git path to one filesystem owner.
        parts = name.split("/")
        if (not name.startswith(PREFIXES)
                or name.startswith("/")
                or "\\" in name
                or any(part in ("", ".", "..") for part in parts)
                or any(ord(char) < 32 or ord(char) == 127 for char in name)
                or not re.fullmatch(r"[0-9a-f]{40}", blob)
                or pr != "788" or commit != SOURCE_COMMIT):
            raise ValueError("SDK_RECEIVER_INVALID_IDENTITY")
        # Distinct case spellings are aliases on common Windows/macOS filesystems.
        if name in seen or name.casefold() in aliases:
            raise ValueError("SDK_RECEIVER_DUPLICATE_PATH")
        seen.add(name)
        aliases.add(name.casefold())
        rows.append((name, blob))
    if len(rows) != expected_count:
        raise ValueError("SDK_RECEIVER_DENOMINATOR_MISMATCH")
    return tuple(rows)


def git_blob_sha1(data):
    header = b"blob " + str(len(data)).encode("ascii") + b"\0"
    return hashlib.sha1(header + data).hexdigest()


def verify(repo, ledger, expected_count=52):
    root = Path(repo).resolve(strict=True)
    payload = read_original_ledger(root, ledger, expected_count)
    rows = rows_from_text(payload.decode("utf-8"), expected_count)
    proof = hashlib.sha256()
    successors = []
    current_source_successors = []
    for name, expected in rows:
        if name in SUCCESSORS:
            predecessor, successor, canonical_path = SUCCESSORS[name]
            if expected != predecessor:
                raise ValueError("SDK_RECEIVER_SUCCESSOR_LINEAGE_DRIFT:" + name)
            expected = successor
            successors.append({
                "path": name, "predecessor_git_blob": predecessor,
                "git_blob": successor, "canonical_repository": "hsoliwal/com.synexia",
                "canonical_commit": SUCCESSOR_COMMITS.get(name, SUCCESSOR_COMMIT), "canonical_path": canonical_path,
                "previous_successor_git_blob": PREVIOUS_SUCCESSORS.get(name),
            })
        if name in CURRENT_SOURCE_SUCCESSORS:
            previous, current, source_commit, gate = CURRENT_SOURCE_SUCCESSORS[name]
            if expected != previous:
                raise ValueError("SDK_RECEIVER_CURRENT_LINEAGE_DRIFT:" + name)
            expected = current
            current_source_successors.append({
                "path": name, "previous_git_blob": previous,
                "git_blob": current, "source_repository": "hsoliwal/M3jdk21",
                "source_commit": source_commit, "source_path": name,
                "policy_and_runtime_gate": gate,
                "jdk_runtime_verified": False, "product_accepted": False,
            })
        # Reject every symlink component even when it resolves *inside* root.
        # The former leaf-only guard silently accepted a symlinked parent.
        source = root
        for component in PurePosixPath(name).parts:
            source = source / component
            if source.is_symlink():
                raise ValueError("SDK_RECEIVER_MISSING_OR_UNSAFE_SOURCE:" + name)
        if (not source.is_file()
                or not source.resolve(strict=True).is_relative_to(root)):
            raise ValueError("SDK_RECEIVER_MISSING_OR_UNSAFE_SOURCE:" + name)
        actual = git_blob_sha1(source.read_bytes())
        if actual != expected:
            raise ValueError("SDK_RECEIVER_BLOB_DRIFT:" + name)
        proof.update(name.encode("utf-8"))
        proof.update(b"\0")
        proof.update(actual.encode("ascii"))
        proof.update(b"\n")
    return {
        "schema": "M3SDK_JMOD_RECEIVER_V1",
        "status": "SOURCE_BLOBS_VERIFIED",
        "files": len(rows),
        "source_pr": 788,
        "source_commit": SOURCE_COMMIT,
        "ledger_authority": ("IMMUTABLE_PR_788" if expected_count == 52
                             else "TEST_ONLY_NONPRODUCTION_DENOMINATOR"),
        "source_ledger_git_blob": (ORIGINAL_LEDGER_GIT_BLOB if expected_count == 52
                                   else git_blob_sha1(payload)),
        "source_ledger_sha256": hashlib.sha256(payload).hexdigest(),
        "qualified_source_successors": successors,
        "current_source_successors": current_source_successors,
        "content_root_sha256": proof.hexdigest(),
        "jdk_runtime_verified": False,
        "java21_spec_compatible": "NOT_DETERMINED_BY_SOURCE_CUSTODY",
        "product_accepted": False,
    }


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path("."))
    parser.add_argument("--ledger", type=Path)
    args = parser.parse_args(argv)
    ledger = args.ledger or args.repo / "make/m3/tooling/jmod-recipes/RECEIVED_SDK_JMOD_BLOBS.tsv"
    result = verify(args.repo, ledger)
    print(json.dumps(result, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
