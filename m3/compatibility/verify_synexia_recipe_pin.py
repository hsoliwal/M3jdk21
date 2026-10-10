# SPDX-License-Identifier: Apache-2.0
"""Read-only verification of the pinned external Synexia recipe-owner checkout."""

from __future__ import annotations

import csv
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PIN = ROOT / "compatibility" / "synexia-recipe-home-pin.tsv"
SHA1 = re.compile(r"[0-9a-f]{40}\Z")
EXPECTED_SCHEMA = "M3JDK21_SYNEXIA_RECIPE_HOME_PIN_V1"
FIELDS = (
    ("canonical_manifest_path", "canonical_manifest_git_blob"),
    ("convergence_invariant_path", "convergence_invariant_git_blob"),
)


def _git(checkout: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", "-C", str(checkout), *args],
        check=True,
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    return result.stdout.strip()


def _path(value: str) -> str:
    if (
        not value
        or value.startswith("/")
        or "\\" in value
        or "\x00" in value
        or any(part in ("", ".", "..") for part in value.split("/"))
    ):
        raise ValueError(f"unsafe Synexia pin path: {value!r}")
    return value


def _blob_from_tree(tree_line: str, expected_path: str) -> str:
    lines = tree_line.splitlines()
    if len(lines) != 1:
        raise ValueError(f"expected exactly one pinned source blob: {expected_path}")
    meta, separator, path = lines[0].partition("\t")
    tokens = meta.split()
    if (
        not separator
        or path != expected_path
        or len(tokens) != 3
        or tokens[1] != "blob"
        or SHA1.fullmatch(tokens[2]) is None
    ):
        raise ValueError(f"invalid pinned Git tree entry: {expected_path}")
    return tokens[2]


def read_pin(pin_file: Path = PIN) -> dict[str, str]:
    with pin_file.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if len(rows) != 1:
        raise ValueError("exactly one canonical Synexia pin row required")
    pin = rows[0]
    if (
        None in pin
        or pin.get("schema") != EXPECTED_SCHEMA
        or pin.get("canonical_repository") != "hsoliwal/com.synexia"
        or pin.get("license") != "Apache-2.0"
        or pin.get("state") != "PINNED_CANONICAL_SOURCE"
        or SHA1.fullmatch(pin.get("canonical_revision", "")) is None
    ):
        raise ValueError("invalid canonical Synexia recipe-home pin")
    for path_column, blob_column in FIELDS:
        _path(pin.get(path_column, ""))
        if SHA1.fullmatch(pin.get(blob_column, "")) is None:
            raise ValueError(f"invalid canonical Git blob pin: {blob_column}")
    return pin


def verify(checkout: Path, pin_file: Path = PIN) -> str:
    """Return the verified commit; never modify checkout, refs, or manifest."""
    pin = read_pin(pin_file)
    head = _git(checkout, "rev-parse", "--verify", "HEAD")
    if head != pin["canonical_revision"]:
        raise ValueError(
            f"canonical Synexia revision mismatch: expected {pin['canonical_revision']}, "
            f"observed {head}"
        )
    for path_column, blob_column in FIELDS:
        path = pin[path_column]
        actual = _blob_from_tree(_git(checkout, "ls-tree", "-r", "HEAD", "--", path), path)
        if actual != pin[blob_column]:
            raise ValueError(f"canonical Synexia blob mismatch: {path}")
    return head


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: verify_synexia_recipe_pin.py <checked-out-Synexia>")
    try:
        verified = verify(Path(sys.argv[1]).resolve())
    except (OSError, subprocess.CalledProcessError, ValueError) as error:
        raise SystemExit(f"FAIL|SYNEXIA_CANONICAL_PIN|{error}") from error
    print(f"PASS|SYNEXIA_CANONICAL_PIN|{verified}")
