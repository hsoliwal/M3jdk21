#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Replay the re-sealed hash-pinned snapshot crate m3-file-changemap-lower-bound-20261106.

Exactly the text contract of jdk.internal.m3.rewrite.M3HashPinnedJavaSnapshotRecipe: a target whose
UTF-8 bytes hash to the manifest preimage is replaced by the reviewed template (template hash
checked first); a target already at the postimage is a fixed point and is not written; any other
content is refused. Before writing, the preimage and the postimage must agree byte for byte outside
the ChangeMap hunk (lowerBound helper plus the oldToNew/newToOld bodies), so the replay can never
carry an unrelated edit. Prints one TSV row per target: path, state, before, after.
"""
import argparse
import hashlib
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
CRATE = "m3-file-changemap-lower-bound-20261106"
CRATE_DIR = HERE / "src/test/resources/jdk/internal/m3/rewrite/hash-pinned-java" / CRATE
HUNK_PRE_START = b"        /** Returns -1 for interior deleted bytes; endpoints honor caller affinity. */"
HUNK_POST_START = b"        /**\n         * First edit boundary not less than position."
HUNK_END = b"        /** Conservatively includes insertion exactly on a span boundary. */"
APPLIED, FIXED_POINT, REFUSED = "APPLIED", "FIXED_POINT", "REFUSED"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def rows(manifest: Path):
    for line in manifest.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        cells = line.split("\t")
        if len(cells) != 4:
            raise SystemExit("REFUSED: malformed manifest row: " + line)
        yield cells


def hunk_only(pre: bytes, post: bytes) -> None:
    starts = (pre.find(HUNK_PRE_START), post.find(HUNK_POST_START))
    ends = (pre.find(HUNK_END), post.find(HUNK_END))
    if min(starts) <= 0 or ends[0] <= starts[0] or ends[1] <= starts[1]:
        raise SystemExit("REFUSED: ChangeMap hunk boundaries not found exactly once")
    if pre[:starts[0]] != post[:starts[1]] or pre[ends[0]:] != post[ends[1]:]:
        raise SystemExit("REFUSED: preimage and postimage differ outside the ChangeMap hunk")


def replay(root: Path, check_only: bool) -> int:
    worst = 0
    for path, before, after, template in rows(CRATE_DIR / "manifest.tsv"):
        pre = (CRATE_DIR / "pre" / template).read_bytes()
        post = (CRATE_DIR / template).read_bytes()
        if sha256(pre) != before or sha256(post) != after:
            raise SystemExit("REFUSED: template hash drift for " + path)
        hunk_only(pre, post)
        target = root / path
        current = sha256(target.read_bytes())
        if current == after:
            state = FIXED_POINT
        elif current == before:
            state = APPLIED
            if not check_only:
                target.write_bytes(post)
                if sha256(target.read_bytes()) != after:
                    raise SystemExit("REFUSED: written postimage does not hash to the manifest")
        else:
            state, worst = REFUSED, 2
        sys.stdout.write("%s\t%s\t%s\t%s\n" % (path, state if not check_only else "CHECK_" + state,
                                               current, after))
    return worst


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", type=Path, default=HERE.parents[3],
                        help="repository root holding m3/ (default: four levels above this script)")
    parser.add_argument("--check", action="store_true", help="report the state, write nothing")
    args = parser.parse_args(argv)
    return replay(args.root.resolve(), args.check)


if __name__ == "__main__":
    raise SystemExit(main())
