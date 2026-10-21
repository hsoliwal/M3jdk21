#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed exact Git-tree superset proof for the integrated M3JDK21 product.

The donor ledger is a sealed catalogue of original source blob IDs. This tool
never edits product files. In a full Git checkout it verifies original parent
histories, source modes and every received byte, including one documented
multi-pass reconciliation of current master trim and primitive removal masks.
"""
from __future__ import annotations

from pathlib import Path, PurePosixPath
import re
import subprocess
import sys

MASTER = "2779aba3f98f31c324be43f731be7914e1a05935"
BASE = "4316758d7e59b643d433418a7e440ab16ed1fd29"
DONORS = {
    1619: BASE,
    1613: "16d91a9794ae6c044d55cc5be30fd96dd0cf6cf0",
    1615: "293c48228d40ba061e784d259dcd6dbda9ad8e80",
    1597: "19d77ba95b99e67b31966f8d52cc9800e0b754dd",
    1614: "4076ca2133436a90f95efb27096743ba066eff64",
}
MERGED_ARRAY = "m3/collections/src/main/java/com/m3/collections/M3ArrayList.java"
MERGED_SHA = "b93a1e746271957e9b3105c3ac1a5db87ae09818"
RECEIVER = "m3/qualification/unified-superset-20261021"
RECIPE = "m3/tooling/shared-frontend/recipe-custody/datasets/m3-superset-array-20261021"
ADDITIONS = {
    "m3/collections/src/test/java/com/m3/collections/M3ArrayListSupersetContract.java",
    "m3/collections/src/test/java/com/m3/collections/M3ArrayListSupersetTest.java",
}
SHA = re.compile(r"[0-9a-f]{40}\Z")


def run(root: Path, *args: str, allow_failure: bool = False) -> subprocess.CompletedProcess[bytes]:
    result = subprocess.run(["git", *args], cwd=root, capture_output=True, check=False)
    if result.returncode and not allow_failure:
        raise ValueError("Git verification failed: " + " ".join(args)
                         + ": " + result.stderr.decode("utf-8", "replace")[:400])
    return result


def entry(root: Path, commit: str, relative: str) -> tuple[str, str]:
    raw = run(root, "ls-tree", "-z", commit, "--", relative).stdout.split(b"\0")
    if len(raw) != 2 or not raw[0]:
        raise ValueError("missing or ambiguous source " + commit + ":" + relative)
    record = raw[0].decode("utf-8", "strict")
    meta, path = record.split("\t", 1)
    parts = meta.split(" ")
    if len(parts) != 3 or path != relative or parts[1] != "blob" or not SHA.fullmatch(parts[2]):
        raise ValueError("invalid Git blob identity: " + relative)
    return parts[0], parts[2]


def validate_path(path: str) -> None:
    parts = PurePosixPath(path)
    if (path != str(parts) or parts.is_absolute() or ".." in parts.parts
            or "\\" in path or "\t" in path or "\n" in path or "\r" in path):
        raise ValueError("unsafe file path: " + repr(path))


def main() -> None:
    root = Path(__file__).resolve().parents[3]
    ledger = root / RECEIVER / "DONOR_BLOBS.tsv"
    rows = [line.split("\t") for line in ledger.read_text(encoding="utf-8").splitlines()
            if line and not line.startswith("#")]
    if len(rows) != 193 or any(len(row) != 7 for row in rows):
        raise ValueError("incomplete/invalid exact donor receipt")
    active = run(root, "rev-parse", "HEAD").stdout.decode().strip()
    for commit in [MASTER, *DONORS.values()]:
        if not SHA.fullmatch(commit):
            raise ValueError("invalid parent commit")
        if run(root, "merge-base", "--is-ancestor", commit, "HEAD", allow_failure=True).returncode:
            raise ValueError("a selected source parent is not in Git ancestry: " + commit)

    selected: set[str] = set()
    policy_count = 0
    for raw in rows:
        pr, head, path, donor_sha, installed_sha, status, policy = raw
        validate_path(path)
        if (not pr.isdecimal() or int(pr) not in DONORS or DONORS[int(pr)] != head
                or path in selected or status not in ("added", "modified")
                or not SHA.fullmatch(donor_sha) or not SHA.fullmatch(installed_sha)):
            raise ValueError("invalid provenance or duplicate file in donor ledger")
        selected.add(path)
        donor_mode, original = entry(root, head, path)
        product_mode, actual = entry(root, active, path)
        if original != donor_sha or actual != installed_sha:
            raise ValueError("donor/product content drift: " + path)
        if policy == "EXACT":
            if donor_sha != installed_sha or product_mode != donor_mode:
                raise ValueError("an exact donor path was replaced: " + path)
        elif policy == "RECONCILE_MASTER_TRIM_AND_LONG_MASK":
            policy_count += 1
            master_mode, master_sha = entry(root, MASTER, path)
            if (path != MERGED_ARRAY or installed_sha != MERGED_SHA
                    or product_mode != master_mode or original == installed_sha
                    or master_sha == installed_sha):
                raise ValueError("unreviewed source substitution: " + path)
        else:
            raise ValueError("unknown source reconciliation policy")
    if policy_count != 1 or len(selected) != 193:
        raise ValueError("missing exact superset or unexpected conflict count")

    modifications = run(root, "diff", "--name-only", BASE, "HEAD").stdout.decode().splitlines()
    allowed = ADDITIONS | selected
    for path in modifications:
        validate_path(path)
        if path not in allowed and not path.startswith(RECEIVER + "/") and not path.startswith(
                RECIPE + "/") and path != ".github/workflows/m3-superset-convergence-20261021.yml":
            raise ValueError("unregistered product file changed: " + path)
    code = (root / MERGED_ARRAY).read_text(encoding="utf-8")
    for required in ("trimToSize(boolean invalidateIterators)", "private static long[] markRemoval",
                     "private void compactRemovals"):
        if required not in code:
            raise ValueError("lost original array-contract behavioral atom: " + required)
    if not ADDITIONS.issubset(set(modifications)):
        raise ValueError("missing independent combined regression oracles")
    print(f"M3_SUPERSET_GIT_PASS donors={len(DONORS)} files={len(selected)} "
          f"exact={len(selected)-policy_count} reconciled={policy_count} "
          f"changed={len(modifications)} commit={active}")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError) as problem:
        print("M3_SUPERSET_GIT_REFUSED:", problem, file=sys.stderr)
        sys.exit(1)
