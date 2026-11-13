#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed ownership check for Synexia-canonical reusable recipe mirrors/residue.

Recipes are authored and stored in the donor (AGENTS.md); this repository freezes what it keeps of them in three
records, each pinned whole by SHA-256 (LF bytes): an edited record fails until it is reviewed and re-pinned here.
- m3-recipe-ownership.tsv, one row per recipe family. Every BORROWED_CANONICAL_* row pins the canonical copy received
  into this repository. BORROWED_CANONICAL_WITH_FROZEN_LEGACY, BORROWED_CANONICAL_TARGET_ADAPTER and the
  SYNEXIA_CANONICAL_PENDING_MERGE_*_RETAINED rows also pin the legacy file kept here (the frozen legacy recipe, the
  target adapter, the retained adapter or overlay) by its Git blob; the *_LEGACY_RETIRED rows require it to be gone.
- m3-canonical-residue-gitblobs.tsv: the reusable residue files, each pinned by its Git blob; RESIDUE_DIRS are closed
  sets (a Java file there without a row fails).
- m3-recipe-home-pin.tsv: the donor recipe-home pin, donor identities as recorded at the donor revision.
previous_* columns are lineage of the re-freeze of 2026-11-13: the earlier pin and 06495fea4157, the last commit
before the JDK-naming rename (fe35ea988411, e2e9e0d3e6e7), where that earlier pin was already in the record. The rename
rewrote the frozen files, and the earlier pins did not match them at 06495fea4157 either, so those rows are re-frozen
at the current blobs; none is dropped. Rows that still matched carry '-' twice.
The records keep the donor's vocabulary (column names, states, schema, donor paths); this checker reads it as written.
"""

from __future__ import annotations

import csv
import hashlib
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LEDGER = Path(__file__).with_name("m3-recipe-ownership.tsv")
PIN = Path(__file__).with_name("m3-recipe-home-pin.tsv")
RESIDUE = Path(__file__).with_name("m3-canonical-residue-gitblobs.tsv")
HEX40 = re.compile(r"[0-9a-f]{40}")
# SHA-256 of each record (LF bytes); re-pin after a reviewed record change
LEDGER_SHA256 = "391d44d771de8b3ddf1b5d6f70a587adf3361e19d19b94e450b2f8da5af84dd4"
RESIDUE_SHA256 = "7e385345277bfcef27347e4df9a7f75a383758acfbe570421e1d73cfab147a5e"
PIN_SHA256 = "6675f11687f8db693b739852937e843a78a7576c1f859383e53423bc851ce07a"
NO_LINEAGE = "-"
# The home pin was re-bound on 2026-10-18 (11d59e663574, recipe source custody) to a donor revision that no family row
# carries; that custody execution seals the pin record, so it is not edited and no family row is invented for it: the
# pin must name a family revision or exactly this recorded one.
HOME_PIN_REBOUND_REVISION = "215546d1ca0e724eb2c8e2e8ef6b4adce724733a"
MIGRATION_JAVA = "m3/tooling/migration-recipes/src/main/java/"
# legacy recipe roots: the pre-rename com/m3 spelling of the retired rows and the current jdk/m3 one
LEGACY_ROOTS = tuple(MIGRATION_JAVA + root for root in (
    "com/m3/rewrite/atom/", "com/m3/rewrite/scope/", "com/m3/rewrite/pass/", "com/m3/rewrite/semantic/",
    "jdk/m3/rewrite/atom/", "jdk/m3/rewrite/scope/", "jdk/m3/rewrite/pass/", "jdk/m3/rewrite/semantic/"))
# target adapters this repository keeps over a borrowed canonical recipe
ADAPTER_ROOTS = (MIGRATION_JAVA + "jdk/m3/rewrite/M3Java21ConvergenceRecipe.java", MIGRATION_JAVA + "jdk/m3/rewrite/a3/")
CANONICAL_ROOTS = (
    "m3/m3-import/pure-int-recipe-custody/src/main/java/jdk/internal/m3/rewrite/",
    MIGRATION_JAVA + "jdk/internal/m3/rewrite/semantic/",
)
# a pending canonical recipe lives in the donor only: its path is the donor's
PENDING_CANONICAL_ROOT = "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/"
FROZEN = "BORROWED_CANONICAL_WITH_FROZEN_LEGACY"
RETIRED = "BORROWED_CANONICAL_LEGACY_RETIRED"
ADAPTER = "BORROWED_CANONICAL_TARGET_ADAPTER"
# what an adapter row carried instead of a blob before 2026-11-13 (the checker did not know the state)
UNFROZEN_ADAPTER = "TARGET_ADAPTER"
PENDING_RETAINED = frozenset({
    "SYNEXIA_CANONICAL_PENDING_MERGE_TARGET_ADAPTER_RETAINED",
    "SYNEXIA_CANONICAL_PENDING_MERGE_TARGET_OVERLAY_RETAINED",
})
PENDING_RETIRED = "SYNEXIA_CANONICAL_PENDING_MERGE_LEGACY_RETIRED"
STATES = frozenset({FROZEN, RETIRED, ADAPTER, PENDING_RETIRED} | PENDING_RETAINED)
LEDGER_HEADER = ["family", "legacy_path", "legacy_git_blob", "canonical_path", "synexia_revision", "synexia_git_blob",
                 "state", "previous_legacy_git_blob", "previous_pin_commit"]
RESIDUE_HEADER = ["path", "git_blob_sha1", "disposition", "canonical_synexia_owner", "license",
                  "previous_git_blob_sha1", "previous_pin_commit"]
PIN_HEADER = ["schema", "canonical_repository", "canonical_revision", "canonical_manifest_path",
              "canonical_manifest_git_blob", "convergence_invariant_path", "convergence_invariant_git_blob", "license",
              "state"]
# Closed residue directories: every Java file under them must be a residue row. Scope refreshed 2026-11-13: the atom
# directory is gone; the target adapters (M3Java21ConvergenceRecipe, a3/) are frozen as adapter rows of the ownership
# ledger and their two unrecorded neighbours are ABSORB rows of m3/release/LEGACY_RECIPE_CUSTODY_LEDGER.tsv (blob-pinned
# there); m3/indexdb is a live module that received 12 files since the freeze, so its 8 residue rows stay pinned by blob
# but the directory is no longer a closed set.
RESIDUE_DIRS = (MIGRATION_JAVA + "jdk/m3/rewrite/semantic",)


def git_blob(path: Path) -> str:
    data = path.read_bytes()
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def read_record(path: Path, header: list[str], digest: str, name: str) -> list[dict[str, str]]:
    """The rows of a record whose bytes are the pinned ones."""
    actual = hashlib.sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()
    if actual != digest:
        raise ValueError(f"{name} record drift: sha256={actual} pinned={digest}")
    with path.open("r", encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        if reader.fieldnames != header:
            raise ValueError(f"invalid {name} header")
        rows = list(reader)
    if not rows:
        raise ValueError(f"empty {name} record")
    for physical, row in enumerate(rows, start=2):
        if any(not row[field] for field in header) or None in row:
            raise ValueError(f"blank or extra {name} field at row {physical}")
    return rows


def check_lineage(row: dict[str, str], current: str, previous: str, physical: int, marker: str = "") -> None:
    """previous_* is either '-' twice or an earlier pin (a blob that is not the current one, or for a target adapter
    the marker it carried before it was frozen) and the commit that earlier pin was recorded at."""
    blob, commit = row[previous], row["previous_pin_commit"]
    if (blob, commit) == (NO_LINEAGE, NO_LINEAGE):
        return
    if not ((HEX40.fullmatch(blob) or blob == marker) and HEX40.fullmatch(commit)) or blob == row[current]:
        raise ValueError(f"invalid re-freeze lineage at row {physical}")


def load_pin() -> dict[str, str]:
    rows = read_record(PIN, PIN_HEADER, PIN_SHA256, "Synexia recipe-home pin")
    if len(rows) != 1:
        raise ValueError("Synexia recipe-home pin must contain exactly one row")
    row = rows[0]
    if row["schema"] != "M3JDK21_SYNEXIA_RECIPE_HOME_PIN_V1":
        raise ValueError("invalid Synexia recipe-home pin schema")
    if row["canonical_repository"] != "hsoliwal/com.synexia":
        raise ValueError("unexpected Synexia canonical repository")
    if not HEX40.fullmatch(row["canonical_revision"]):
        raise ValueError("invalid Synexia canonical revision")
    if row["canonical_manifest_path"] != "synexia-openrewrite-recipes/CANONICAL_RECIPE_HOME.tsv":
        raise ValueError("unexpected Synexia canonical manifest")
    if row["convergence_invariant_path"] != "docs/M3-SCALE/invariants/SYNEXIA-PUBLIC-TARGET-CONVERGENCE-1.json":
        raise ValueError("unexpected Synexia convergence invariant")
    for field in ("canonical_manifest_git_blob", "convergence_invariant_git_blob"):
        if not HEX40.fullmatch(row[field]):
            raise ValueError(f"invalid pinned Git blob: {field}")
    if row["license"] != "Apache-2.0":
        raise ValueError("canonical Synexia recipe fast lane must remain Apache-2.0")
    if row["state"] != "PINNED_CANONICAL_SOURCE":
        raise ValueError("invalid Synexia recipe-home pin state")
    return row


def load() -> list[dict[str, str]]:
    """Validate borrowed, adapter and source-pinned pending Synexia recipe ownership rows."""
    rows = read_record(LEDGER, LEDGER_HEADER, LEDGER_SHA256, "Synexia recipe ownership")
    families: set[str] = set()
    revisions: set[str] = set()
    for physical, row in enumerate(rows, start=2):
        if row["family"] in families:
            raise ValueError(f"duplicate recipe family {row['family']}")
        families.add(row["family"])
        state = row["state"]
        if state not in STATES:
            raise ValueError(f"invalid recipe ownership state at row {physical}")
        if not HEX40.fullmatch(row["synexia_revision"]):
            raise ValueError(f"invalid Synexia revision at row {physical}")
        revisions.add(row["synexia_revision"])
        for field in ("legacy_git_blob", "synexia_git_blob"):
            if not HEX40.fullmatch(row[field]):
                raise ValueError(f"invalid Git blob {field} at row {physical}")
        check_lineage(row, "legacy_git_blob", "previous_legacy_git_blob", physical,
                      UNFROZEN_ADAPTER if state == ADAPTER else "")
        legacy_path = row["legacy_path"]
        if state == ADAPTER:
            if not (legacy_path == ADAPTER_ROOTS[0] or legacy_path.startswith(ADAPTER_ROOTS[1:])):
                raise ValueError(f"target adapter escaped its namespace at row {physical}")
        elif not legacy_path.startswith(LEGACY_ROOTS):
            raise ValueError(f"legacy recipe escaped frozen namespace at row {physical}")
        pending = state in PENDING_RETAINED or state == PENDING_RETIRED
        if pending:
            if not row["canonical_path"].startswith(PENDING_CANONICAL_ROOT):
                raise ValueError(f"pending canonical path escaped Synexia namespace at row {physical}")
        elif not row["canonical_path"].startswith(CANONICAL_ROOTS):
            raise ValueError(f"canonical recipe escaped borrowed namespace at row {physical}")

        legacy = ROOT / legacy_path
        if state in (FROZEN, ADAPTER) or state in PENDING_RETAINED:
            if not legacy.is_file() or git_blob(legacy) != row["legacy_git_blob"]:
                kind = {FROZEN: "frozen legacy recipe", ADAPTER: "target adapter"}.get(state, "retained target adapter")
                raise ValueError(f"{kind} drift: {legacy_path}")
        elif legacy.exists():
            raise ValueError(f"retired legacy recipe reintroduced: {legacy_path}")

        if not pending:
            canonical = ROOT / row["canonical_path"]
            if not canonical.is_file() or git_blob(canonical) != row["synexia_git_blob"]:
                raise ValueError(f"Synexia canonical mirror drift: {row['canonical_path']}")

    pin = load_pin()
    if pin["canonical_revision"] not in revisions | {HOME_PIN_REBOUND_REVISION}:
        raise ValueError("recipe-home pin revision is absent from exact family custody")
    return rows


def reusable_residue_paths() -> set[str]:
    result: set[str] = set()
    for directory in RESIDUE_DIRS:
        start = ROOT / directory
        if start.is_dir():
            result.update(path.relative_to(ROOT).as_posix() for path in start.rglob("*.java") if path.is_file())
    return result


def load_residue() -> list[dict[str, str]]:
    """Validate the full reusable target-local migration residue freeze."""
    rows = read_record(RESIDUE, RESIDUE_HEADER, RESIDUE_SHA256, "Synexia canonical residue")
    seen: set[str] = set()
    for physical, row in enumerate(rows, start=2):
        target = row["path"]
        if target in seen:
            raise ValueError(f"duplicate residue path: {target}")
        seen.add(target)
        if not HEX40.fullmatch(row["git_blob_sha1"]):
            raise ValueError(f"invalid residue Git blob: {target}")
        check_lineage(row, "git_blob_sha1", "previous_git_blob_sha1", physical)
        if row["disposition"] != "MIGRATION_RESIDUE_NOT_CANONICAL":
            raise ValueError(f"invalid residue disposition: {target}")
        owner = row["canonical_synexia_owner"]
        if not (owner.startswith("com.synexia") or owner.startswith("synexia-")):
            raise ValueError(f"invalid canonical Synexia owner: {target}")
        if row["license"] != "Apache-2.0":
            raise ValueError(f"non-Apache reusable residue: {target}")

        source = ROOT / target
        if not source.is_file():
            raise ValueError(f"frozen reusable residue missing: {target}")
        if git_blob(source) != row["git_blob_sha1"]:
            raise ValueError(
                "frozen reusable residue drift: "
                f"{target}; improve {owner} in hsoliwal/com.synexia and consume a pinned handoff"
            )

    actual = reusable_residue_paths()
    closed = {target for target in seen if target.startswith(tuple(d + "/" for d in RESIDUE_DIRS))}
    if closed != actual:
        missing = sorted(actual - closed)
        stale = sorted(closed - actual)
        raise ValueError(
            f"reusable residue set drift: unsealed={missing} retired_without_manifest_update={stale}"
        )
    return rows


def main(argv: list[str]) -> int:
    if len(argv) > 1:
        print("usage: check_m3_recipe_ownership.py", file=sys.stderr)
        return 2
    rows = load()
    residue = load_residue()
    pin = load_pin()
    revisions = ",".join(sorted({row["synexia_revision"] for row in rows}))
    borrowed = sum(1 for row in rows if row["state"].startswith("BORROWED_CANONICAL_"))
    pending = sum(1 for row in rows if row["state"].startswith("SYNEXIA_CANONICAL_PENDING_"))
    refrozen = sum(1 for row in rows + residue if row["previous_pin_commit"] != NO_LINEAGE)
    print(
        "M3_RECIPE_OWNERSHIP_PASS "
        f"borrowed_rows={borrowed} pending_rows={pending} total_rows={len(rows)} "
        f"frozen_residue={len(residue)} refrozen={refrozen} revisions={revisions} "
        f"manifest_blob={pin['canonical_manifest_git_blob']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
