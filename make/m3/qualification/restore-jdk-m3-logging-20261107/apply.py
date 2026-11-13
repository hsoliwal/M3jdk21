#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe RESTORE_JDK_M3_LOGGING v1: restore the jdk.m3.logging module dropped by the fold merge fc917d80ea3c.

The module landed through #2234 (head f12245e0b7a8) and the receiver #2390 (head 7c23ff3612ef). The fold merge
fc917d80ea3c (2026-11-06, "fold #2390 head 7c23ff3612ef through API_FOLD v1") recorded 7c23ff3612ef as its second
parent but wrote its first parent's tree (9bda130301a4, no module) for src/jdk.m3.logging,
make/modules/jdk.m3.logging and test/jdk/jdk/m3/logging, and its first parent's make/conf/module-loader-map.conf
(PLATFORM_MODULES without jdk.m3.logging). master therefore has 0 files under the three prefixes.

Two kinds of edit, both hash-pinned:
RESTORE: each of the eight files is the blob of 7c23ff3612ef, byte for byte (the same blobs as f12245e0b7a8, so
no fallback was needed). A missing file is written from `git cat-file blob <pinned sha1>` after its sha256 is
checked; a present file must equal the pinned sha256 (anything else is a drifted target: refused, nothing written).
m3/tooling/jdk-naming/jdk_naming.py plan/apply --only the three prefixes is a fixed point on these bytes
(moves=0 rewrites=0), so RENAME_APPLIED is empty.
REGISTER: make/conf/module-loader-map.conf gets `    jdk.m3.logging \\` after `    jdk.m3.graphics \\` inside
PLATFORM_MODULES (the sorted position; 7c23ff3612ef had it after jdk.localedata, before master's two jdk.m3
modules existed) and one `# //FLLDCIM: MRL` comment line above the PLATFORM_MODULES declaration (a make comment;
the anchor cannot sit inside the backslash-continued list). The target must equal the pinned preimage or the pinned
postimage; anything else is a drifted preimage (exit 2, nothing written).

Every preimage and postimage is verified before the first write. Idempotent; EDITS.tsv is a fixed point; --check
writes nothing and exits 1 when a run would change a file. Staging (git add) is left to the caller.

usage: python -I apply.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "aa2372843bfffd44e9b87e200914e18a0863a373"
ORIGIN_HEAD = "f12245e0b7a85f851e991a4135c48c39000fdd94"
RECEIVER_HEAD = "7c23ff3612ef08dc265bd3bab5461e4dcf3ee47c"
DROPPING_MERGE = "fc917d80ea3cb2a888d327240766311a03e14d42"
FOLD_FIRST_PARENT = "9bda130301a4b0121ec24fc4f3ea5d8a939c1c1d"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "op", "before_sha256", "after_sha256", "source_blob", "source_commit", "dropping_merge",
                 "rule")
ABSENT = "ABSENT"
RESTORE_RULE = ("RESTORE: byte-exact blob of 7c23ff3612ef (#2390 head; identical at f12245e0b7a8, #2234 head);"
                " jdk_naming fixed point, RENAME_APPLIED none")
REGISTER_RULE = ("REGISTER: PLATFORM_MODULES gains jdk.m3.logging after jdk.m3.graphics (7c23ff3612ef line 85 had"
                 " it after jdk.localedata) plus one //FLLDCIM: MRL make-comment anchor above the declaration")

# path, blob sha1 at 7c23ff3612ef, sha256 of the bytes
RESTORED = (
    ("make/modules/jdk.m3.logging/Java.gmk", "649aa9048f59e48fccd21ca124b582498103fcfe",
     "def624bda979da89807941ea8e147e836b500b7400b2e34d13c6088987c9ccb3"),
    ("src/jdk.m3.logging/share/classes/jdk/m3/logging/M3StructuredFormatter.java",
     "1b5819b08b4f22d65c2a42aa7d3ca8bc33f3a7b3",
     "09e2b738f6e1413c1fba19fbf889fe8da6da6fdbfd65fd449156ee1d4b2c3bce"),
    ("src/jdk.m3.logging/share/classes/jdk/m3/logging/package-info.java",
     "281a3402baded3a9aa9b7ac9824bc05a3dfa541a",
     "a29689a7c880ad9ba4f1b56e45a1cfc6b673deb30548580e84bd864c3aca5279"),
    ("src/jdk.m3.logging/share/classes/module-info.java", "712a724624079e0ddd663093ee24fc8a319a2bf1",
     "111f9de0046b6253018e83c464cd413c870f81c6bf678ea0fda0fe642831d8f7"),
    ("test/jdk/jdk/m3/logging/M3StructuredFormatterJitDriver.java", "1c7b1cd8822c80ba53a793e09fe039833c3856fc",
     "4ec34e9a4d55d81b160eadc8704aedaf5e97e4f992ac8c1c220bc7ee9c978778"),
    ("test/jdk/jdk/m3/logging/M3StructuredFormatterPassesTest.java", "1373e81cd991c0936a182eabc9663ae30712a9ac",
     "00d53d984b77b8a9a32e9287a54eb1b29721f97874439bfad1be92eaa7df29b4"),
    ("test/jdk/jdk/m3/logging/M3StructuredFormatterPerformanceTest.java",
     "6903514c07302a42acd7d2069e44240ec943d026",
     "f4f004c48454047a29484d4b3d111e54d32c30c56a9f7d208b5cc7b05dcee328"),
    ("test/jdk/jdk/m3/logging/M3StructuredFormatterTest.java", "edd8d2eabb54e69fb0551d24f8d7eca553e40949",
     "23c5fb373eabb1261b88e634662836727990f39eae29441d0dfc340f0623a78f"),
)

REGISTRATION = "make/conf/module-loader-map.conf"
# master's blob (4063827c188) and the 7c23ff3612ef blob (4182f2c46cb) that carried the line
REGISTRATION_BEFORE = "d14583991981d7d469d5e72bfbc43755eb23eba7b590f63120b649d6eb2760ba"
REGISTRATION_AFTER = "74fcb2422f3052516d63cb0e35ecfb194abafbd420bc8dbfcbf11e6ea1384ba2"
REGISTRATION_SOURCE_BLOB = "4182f2c46cb8feeeec100e8fbedd8b200e60a7d9"
DECLARATION = b"\nPLATFORM_MODULES= \\\n"
ANCHOR = (b"# //FLLDCIM: MRL - jdk.m3.logging restored to PLATFORM_MODULES (fold fc917d80ea3c dropped #2234/#2390"
          b" head 7c23ff3612ef); recipe m3/qualification/restore-jdk-m3-logging-20261107/apply.py\n")
PREDECESSOR_LINE = b"    jdk.m3.graphics \\\n"
MODULE_LINE = b"    jdk.m3.logging \\\n"


class Refused(Exception):
    pass


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def git_blob(root: Path, blob: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("blob %s is not in this repository (%s)" % (blob, failure)) from failure


def register(data: bytes) -> bytes:
    """The registration postimage: exactly one declaration, one predecessor line and no module line yet."""
    counts = (data.count(DECLARATION), data.count(PREDECESSOR_LINE), data.count(MODULE_LINE))
    if counts != (1, 1, 0):
        raise Refused("%s shape (declaration, jdk.m3.graphics, jdk.m3.logging) = %s, need (1, 1, 0)"
                      % (REGISTRATION, counts))
    out = data.replace(DECLARATION, b"\n" + ANCHOR + DECLARATION[1:], 1)
    return out.replace(PREDECESSOR_LINE, PREDECESSOR_LINE + MODULE_LINE, 1)


def registration(root: Path) -> tuple:
    """(state, pre sha256, write or None) for the registration file."""
    target = root / REGISTRATION
    if not target.exists():
        raise Refused("missing preimage %s" % REGISTRATION)
    on_disk = target.read_bytes()
    digest = sha256(on_disk)
    if digest == REGISTRATION_AFTER:
        return "UNCHANGED", REGISTRATION_BEFORE, None
    if digest != REGISTRATION_BEFORE:
        raise Refused("drifted preimage %s: on disk %s, pinned %s" % (REGISTRATION, digest, REGISTRATION_BEFORE))
    post = register(on_disk)
    if sha256(post) != REGISTRATION_AFTER:
        raise Refused("postimage of %s is %s, pinned %s" % (REGISTRATION, sha256(post), REGISTRATION_AFTER))
    return "CHANGED", digest, (target, post)


def restored(root: Path, path: str, blob: str, digest: str) -> tuple:
    """(state, write or None) for one restored file: present and pinned, or absent and taken from the blob."""
    target = root / path
    if target.exists():
        on_disk = sha256(target.read_bytes())
        if on_disk != digest:
            raise Refused("drifted target %s: on disk %s, pinned %s" % (path, on_disk, digest))
        return "RESTORED_UNCHANGED", None
    data = git_blob(root, blob)
    if blob_sha(data) != blob or sha256(data) != digest:
        raise Refused("blob %s for %s does not hash to the pinned %s" % (blob, path, digest))
    return "RESTORED", (target, data)


def run(root: Path, check: bool) -> int:
    rows, writes, changed = [], [], 0
    state, pre, write = registration(root)
    rows.append((REGISTRATION, "REGISTER", pre, REGISTRATION_AFTER, REGISTRATION_SOURCE_BLOB, RECEIVER_HEAD,
                 DROPPING_MERGE, REGISTER_RULE))
    print("EDIT REGISTER %s %s" % (state, REGISTRATION))
    if write:
        writes.append(write)
        changed += 1
    for path, blob, digest in RESTORED:
        state, write = restored(root, path, blob, digest)
        rows.append((path, "RESTORE", ABSENT, digest, blob, RECEIVER_HEAD, DROPPING_MERGE, RESTORE_RULE))
        print("EDIT RESTORE %s %s" % (state, path))
        if write:
            writes.append(write)
            changed += 1
    if not check:  # every preimage and postimage verified before the first write
        for target, data in writes:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("RESTORE_JDK_M3_LOGGING recipe=%s %s base=%s receiver=%s rules=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], RECEIVER_HEAD[:12], len(rows), changed,
        "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[2]), help="repository root (default: three up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
