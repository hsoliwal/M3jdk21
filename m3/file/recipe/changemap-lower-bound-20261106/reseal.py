#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Re-seal the donor ChangeMap lower-bound crate onto the renamed jdk.m3.file owner.

Donor: hsoliwal/com.synexia 4a2e6452a7a, crate m3-file-changemap-lower-bound-20261029
(preimage blob 87dd9d8293de46d8843c09b2109063b11e682b54, postimage blob
51e301acd29642924dec89f0db3b7d7f3ae26c15). Both templates were pinned to the pre-rename owner
path. This script derives the templates for the live path mechanically:
  1. the package declaration follows the JDK naming rename (NAMING_RULES.tsv PKG_COM_M3);
  2. the postimage gains one //FLLDCIM: anchor line above each of the three changed methods.
The derived preimage must equal the live receiver byte for byte, otherwise the re-seal is refused.
Writes <crate-dir>/pre/M3FileEditBatch.java.txt, <crate-dir>/M3FileEditBatch.java.txt and
<crate-dir>/manifest.tsv; prints one TSV row per hash.
"""
import argparse
import hashlib
import re
import sys
from pathlib import Path

DONOR_PRE_SHA256 = "7612ebe067c2602e4c3efcf6df2824e173ca4d858e85f5395abd0e5e60b890b9"
DONOR_POST_SHA256 = "6329ee71cc7921d6480d83b80a9e5d528d834adbb37f9a2e24349ad24e11f52d"
DONOR_COMMIT = "4a2e6452a7a"
DONOR_CRATE = "m3-file-changemap-lower-bound-20261029"
CRATE = "m3-file-changemap-lower-bound-20261106"
LIVE_PACKAGE = b"package jdk.m3.file;"
PACKAGE_LINE = re.compile(rb"^package [A-Za-z0-9_.]+;$", re.MULTILINE)
TEMPLATE = "M3FileEditBatch.java.txt"
ANCHOR_PREFIX = "        //FLLDCIM: RECIPE — " + CRATE
ANCHORS = (
    (b"        private static int lowerBound(int[] starts, int position) {",
     ANCHOR_PREFIX + " replays donor crate " + DONOR_CRATE + " (hsoliwal/com.synexia "
     + DONOR_COMMIT + "): first-equality lower bound; repeated destination boundaries after"
     " adjacent deletions keep the earliest edit"),
    (b"        public int oldToNew(int position, Affinity affinity) {",
     ANCHOR_PREFIX + ": O(E) ordered scan -> O(log E) lower bound; oracle"
     " M3FileChangeMapBinarySearchProof (50,001 maps / 9,341,740 checks)"),
    (b"        public int newToOld(int position, Affinity affinity) {",
     ANCHOR_PREFIX + ": O(E) ordered scan -> O(log E) lower bound; same affinity and"
     " exception contract as the scan it replaces"),
)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def renamed(text: bytes) -> bytes:
    matches = PACKAGE_LINE.findall(text)
    if len(matches) != 1:
        raise SystemExit("REFUSED: expected exactly one package declaration, found %d" % len(matches))
    return PACKAGE_LINE.sub(LIVE_PACKAGE, text, count=1)


def anchored(text: bytes) -> bytes:
    for marker, anchor in ANCHORS:
        line = marker + b"\n"
        if text.count(line) != 1:
            raise SystemExit("REFUSED: anchor marker not unique: " + marker.decode("ascii"))
        text = text.replace(line, anchor.encode("utf-8") + b"\n" + line)
    return text


def pinned(path: Path, expected: str, label: str) -> bytes:
    data = path.read_bytes()
    actual = sha256(data)
    if actual != expected:
        raise SystemExit("REFUSED: %s sha256 %s != pinned %s" % (label, actual, expected))
    return data


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-pre", required=True, type=Path)
    parser.add_argument("--donor-post", required=True, type=Path)
    parser.add_argument("--receiver", required=True, type=Path,
                        help="live m3/file/src/main/java/jdk/m3/file/M3FileEditBatch.java")
    parser.add_argument("--crate-dir", required=True, type=Path)
    parser.add_argument("--target", default="m3/file/src/main/java/jdk/m3/file/M3FileEditBatch.java")
    args = parser.parse_args(argv)
    donor_pre = pinned(args.donor_pre, DONOR_PRE_SHA256, "donor preimage")
    donor_post = pinned(args.donor_post, DONOR_POST_SHA256, "donor postimage")
    receiver = args.receiver.read_bytes()
    pre = renamed(donor_pre)
    if pre != receiver:
        raise SystemExit("REFUSED: renamed donor preimage differs from the live receiver (%s != %s)"
                         % (sha256(pre), sha256(receiver)))
    post = anchored(renamed(donor_post))
    crate = args.crate_dir
    (crate / "pre").mkdir(parents=True, exist_ok=True)
    (crate / "pre" / TEMPLATE).write_bytes(pre)
    (crate / TEMPLATE).write_bytes(post)
    manifest = ("# re-sealed 2026-11-06 from donor crate " + DONOR_CRATE + " (hsoliwal/com.synexia "
                + DONOR_COMMIT + "): preimage = live jdk.m3.file owner, postimage = donor postimage"
                " + JDK package + FLLDCIM anchors (reseal.py)\n"
                + "\t".join((args.target, sha256(pre), sha256(post), TEMPLATE)) + "\n")
    (crate / "manifest.tsv").write_bytes(manifest.encode("utf-8"))
    rows = (("donor_pre", args.donor_pre, DONOR_PRE_SHA256), ("donor_post", args.donor_post, DONOR_POST_SHA256),
            ("receiver", args.receiver, sha256(receiver)), ("resealed_pre", crate / "pre" / TEMPLATE, sha256(pre)),
            ("resealed_post", crate / TEMPLATE, sha256(post)))
    for kind, path, digest in rows:
        sys.stdout.write("%s\t%s\t%s\n" % (kind, path.as_posix(), digest))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
