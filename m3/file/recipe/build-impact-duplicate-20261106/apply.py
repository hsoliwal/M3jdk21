#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Remove the duplicate M3BuildImpactPlan.requireSnapshot left by the 2026-11-06 superset merge.

master 48537c8e741 carries two `void requireSnapshot(M3FileDeltaLedger.Snapshot)` methods in
m3/file/src/main/java/jdk/m3/file/M3BuildImpactPlan.java: the identity-only one from 2f708ccfd817
(2026-10-30, "expose exact snapshot admission to reactor planner") and the one from ce7051f77ca0
(2026-11-02, "Reject stale reactor plans ...") that checks null, slot dimensions and identity. javac
refuses the class ("method requireSnapshot(Snapshot) is already defined"), so the module has not
compiled since the merge. This step removes exactly the older identity-only method (its Javadoc, body
and the blank line after it), because the newer method is a strict superset of its checks, and puts one
//FLLDCIM: anchor above the kept method. Hash-pinned preimage and postimage, idempotent, refuses anything
else. Prints one TSV row: path, state, before_sha256, after_sha256.
"""
import argparse
import hashlib
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
TARGET = "m3/file/src/main/java/jdk/m3/file/M3BuildImpactPlan.java"
PREIMAGE_SHA256 = "74a13dbcef49f04a34c6756c82d2e15ae039e24c6bbd1918d19844516aed3816"
POSTIMAGE_SHA256 = "1864b923be7d962254e0686a9ebc50e63fa7d90807f71fd5462604fe1b6e93f2"
REMOVED = (
    b"    /**\n"
    b"     * Require the exact M3File snapshot instance that produced these impact facts.\n"
    b"     * Equal changed-slot masks do not establish identical source generations.\n"
    b"     */\n"
    b"    void requireSnapshot(M3FileDeltaLedger.Snapshot snapshot) {\n"
    b"        if (snapshot != sourceSnapshot) {\n"
    b"            throw new IllegalArgumentException(\"file impact belongs to a different M3File generation\");\n"
    b"        }\n"
    b"    }\n"
    b"\n"
)
KEPT = b"    void requireSnapshot(M3FileDeltaLedger.Snapshot snapshot) {\n"
ANCHOR = ("    //FLLDCIM: RECIPE — build-impact-duplicate-20261106: the identity-only requireSnapshot of"
          " 2f708ccfd817 was removed; this superset check (ce7051f77ca0: null, slot dimensions, identity)"
          " is the one owner\n").encode("utf-8")
APPLIED, FIXED_POINT, REFUSED = "APPLIED", "FIXED_POINT", "REFUSED"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def postimage(pre: bytes) -> bytes:
    if pre.count(REMOVED) != 1:
        raise SystemExit("REFUSED: the duplicate identity-only method is not present exactly once")
    post = pre.replace(REMOVED, b"")
    if post.count(KEPT) != 1:
        raise SystemExit("REFUSED: exactly one requireSnapshot must remain")
    return post.replace(KEPT, ANCHOR + KEPT)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", type=Path, default=HERE.parents[3],
                        help="repository root holding m3/ (default: four levels above this script)")
    parser.add_argument("--check", action="store_true", help="report the state, write nothing")
    args = parser.parse_args(argv)
    target = args.root.resolve() / TARGET
    current = target.read_bytes()
    before = sha256(current)
    if before == POSTIMAGE_SHA256:
        state, after = FIXED_POINT, before
    elif before == PREIMAGE_SHA256:
        post = postimage(current)
        after = sha256(post)
        if after != POSTIMAGE_SHA256:
            sys.stdout.write("%s\tREFUSED_POSTIMAGE_PIN\t%s\t%s\n" % (TARGET, before, after))
            return 2
        state = APPLIED
        if not args.check:
            target.write_bytes(post)
            if sha256(target.read_bytes()) != after:
                raise SystemExit("REFUSED: written postimage does not hash to the pin")
    else:
        state, after = REFUSED, POSTIMAGE_SHA256
    sys.stdout.write("%s\t%s\t%s\t%s\n" % (TARGET, ("CHECK_" if args.check else "") + state, before, after))
    return 2 if state == REFUSED else 0


if __name__ == "__main__":
    raise SystemExit(main())
