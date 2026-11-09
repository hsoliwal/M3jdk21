#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe FOLDAUDIT_1755 v1 (m3/fs): fold-close audit leaves L0035 and L0063 of closed PR #1755, decision-only.

Fold-close audit (ledger foldaudit/LEAVES.tsv + HUNK_VERDICTS.tsv, pr=1755): PR #1755 (head 3e70045b5ed2, closed
2026-10-28 as LANDED_VIA #2070, merge 2028b1fb5f42) is an ancestor of master, but the #2070 landing kept master's
M3BuildStateStore live and retained the PR's files byte for byte under
m3/qualification/master-landing-20261028/retained-variants/pr1755/. Its six MISSING hunks are:

  L0035  m3/fs/src/main/java/jdk/m3/fs/M3BuildStateStore.java (owner ON master), 57 lines: keyed
         readOffline(fs, contextSha256, ..), profileHead(contextSha256), keyed publishOffline(fs, contextSha256, ..)
  L0063  M3OfflineProjectScan.java (never on mainline), 6 lines: the two keyed store calls, nested "/pom.xml" role

Every hunk is SUPERSEDED (b2321d7b550, #1758: profiled readObserved/observe/promoteObserved on the V2 store;
0a89ef92ed4, #1782: M3HostFileInventory, the scanner's successor) or PRESENT_EVOLVED (M3HostFileInventory.role():
lower.endsWith("/pom.xml")), each with the owner's own directive not to graft the V1 store over master (#1755 body
and comments, #1758 README). Per-hunk rows with evidence are in DECISION.tsv. Nothing is restored.

The recipe therefore edits no source. It pins the two master owner files (git blob and sha256 at BASE_COMMIT),
requires every superseding member to be present with its exact count, requires every superseded V1 member and the
never-landed scanner file to be absent, and writes EDITS.tsv (before == after). A drifted owner, a missing
successor or a resurrected V1 member is reported in full and refused (exit 2): that re-opens the decision.
Idempotent; --check writes nothing and exits 1 when EDITS.tsv would change.

usage: python -I apply.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import sys
from pathlib import Path

RECIPE_NAME = "FOLDAUDIT_1755"
RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
ROOT_DEPTH = 3
PR = "1755"
PR_HEAD = "3e70045b5ed2ac07406f046784901725740e3507"
LANDING_MERGE = "2028b1fb5f422c14e024da14b8e5408dde539de6"
BASE_COMMIT = "74402d981fa00a5ba22c938746a3f44d4725c419"
STORE_SUCCESSOR = "b2321d7b550dfa9b31d7c75848530ff6ed987398"  # #1758, merged de37098a75af (2026-10-23)
SCAN_SUCCESSOR = "0a89ef92ed4bd2a3a34119586bc58867a660c189"  # #1782 head b3eda7f99c2a, landed via #2070
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "hunks", "decision", "rule")
FS = "m3/fs/src/main/java/jdk/m3/fs/"

# path, pinned git blob, pinned sha256, leaf, hunks, decision, present [(needle, count)], absent [needle], rule
OWNERS = (
    (FS + "M3BuildStateStore.java", "6bb259f37fe244721a131886a1977782de54a0d6",
     "1658a10714e7d5638145cc3edf3dfdd97075d86b51317f0fe630f8753e610bb4",
     "L0035", "58-84,85-91,138-160", "SUPERSEDED", [
         ("public static Optional<Version> readObserved(M3FS snapshot, byte[] expectedSuccessful,\n"
          "            byte[] profileSha256, int maxBytes, int maxRows, int maxEdges)", 1),
         ("public static Version observe(M3FS fs, M3BuildState completed, byte[] expectedSuccessful,", 2),
         ("public static Version promoteObserved(M3FS fs, byte[] expectedSuccessful, byte[] expectedObserved,", 2),
         ("private static String observedPath(byte[] successful, byte[] profile)", 1),
         ("private static byte[] requiredProfile(byte[] profile)", 1),
     ], ["readOffline(", "publishOffline(", "profileHead(", "publishAt(", "OFFLINE_HEAD"],
     "SUPERSEDED by " + STORE_SUCCESSOR[:12] + " (#1758): one 32-byte profile per Maven/Gradle/IDE consumer in the"
     " same SQLite DB through readObserved/observe/promoteObserved(.., profile, ..), observedPath(successful, profile)"
     " and requiredProfile; no V1 keyed offline member on master"),
    (FS + "M3HostFileInventory.java", "de9c317d44b0ef2967eb812124647a8465d1a7fa",
     "dc31d59b36df7e6050333bb9574e97446a63b1b509d07a4d4de5661e8aaa06a1",
     "L0063", "127,193-195;226-227", "SUPERSEDED;PRESENT_EVOLVED", [
         ("public static Result capture(Path project, Stream<M3NioFile> entries,", 1),
         ("byte[] toolContextSha256, M3BuildState previous,", 1),
         ("return M3BuildStateStore.observe(", 1),
         ("result.inventory().contextSha256(), maxStateBytes, maxRows, 0);", 1),
         ('if (lower.equals("pom.xml") || lower.endsWith("/pom.xml")', 1),
     ], ["readOffline(", "publishOffline(", "M3OfflineProjectScan"],
     "SUPERSEDED by " + SCAN_SUCCESSOR[:12] + " (#1782): capture(.., previous, ..) takes the prior generation from"
     " the caller and stage() observes the profile through M3BuildStateStore.observe; PRESENT_EVOLVED: role()"
     " classifies a nested /pom.xml as BUILD_INPUT (case-folded)"),
)
NEVER_LANDED = (FS + "M3OfflineProjectScan.java", "m3/fs/src/test/java/jdk/m3/fs/M3OfflineProjectScanTest.java")


class Refused(Exception):
    pass


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def audit_owner(root: Path, rel: str, blob: str, digest: str, present, absent) -> tuple[list[str], str]:
    target = root / rel
    if not target.exists():
        return ["missing owner %s (pinned blob %s)" % (rel, blob)], "-"
    data = target.read_bytes()
    text = data.decode("utf-8").replace("\r\n", "\n")
    failures = []
    if blob_sha(data) != blob or sha256(data) != digest:
        failures.append("drifted owner %s: on disk blob %s sha256 %s, pinned blob %s" % (
            rel, blob_sha(data), sha256(data)[:16], blob))
    failures.extend("successor absent or miscounted in %s: %r occurs %d (need %d)" % (rel, needle[:60], text.count(needle), count)
                    for needle, count in present if text.count(needle) != count)
    failures.extend("superseded V1 member present in %s: %r occurs %d (need 0)" % (rel, needle, text.count(needle))
                    for needle in absent if text.count(needle) != 0)
    return failures, sha256(data)


def run(root: Path, check: bool) -> int:
    failures, rows = [], []
    for rel, blob, digest, leaf, hunks, decision, present, absent, rule in OWNERS:
        owner_failures, digest_now = audit_owner(root, rel, blob, digest, present, absent)
        failures.extend(owner_failures)
        state = "DRIFT" if owner_failures else "PINNED"
        print("OWNER %s %s %s %s" % (leaf, state, decision, rel))
        rows.append((rel, digest_now, digest_now, leaf, hunks, decision, rule))
    failures.extend("never-landed file present: %s" % rel for rel in NEVER_LANDED if (root / rel).exists())
    if failures:
        raise Refused("\n".join(failures))
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("%s recipe=%s %s pr=%s base=%s owners=%d restored=0 edits=%s" % (
        RECIPE_NAME, RECIPE, VERSION, PR, BASE_COMMIT[:12], len(rows), "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and table_state else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[ROOT_DEPTH]), help="repository root")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when EDITS.tsv would change")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
