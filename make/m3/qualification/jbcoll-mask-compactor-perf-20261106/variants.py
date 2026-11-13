#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""Variants of leaf jbcoll-mask-compactor-perf-20261106 (JBCOLL_MASK_COMPACTOR_VARIANTS v1) for the differential and
the timing probe.

Writes renamed copies (package jbcoll, class MaskCompactor_<label>, the L003 CompactorDifferential convention) of:
  master               the master owner blob (apply.PRE_BLOB)
  postimage            the census postimage / owner blob of ff0b94fc2a3b (the historical scalar dispatch)
  statonly             master + apply.STAT_EDITS (complete statistic, old compactRuns threshold length >>> 7)
  threshold            master + STAT_EDITS + DISPATCH_EDITS (compactRuns threshold length >>> 4, compactScalar fallback)
  fixed                apply.postimage_text(master): the recipe postimage (threshold + pr1908 survivors, bound dropped)
  threshold_prefix     threshold + NEC idea pr1754/pr1802 (prefix skip in compactScalar)
  threshold_words1024  threshold + NEC idea pr1908 verbatim (survivor words for the fallback of masks >= 1024)
  fixed_prefix         fixed + prefix skip (compactScalar now serves masks below 512 only)
  mutant               fixed with the L003 one-token scalar mutant ((read & 63) -> (read & 62)); must be refused
Idea edits are anchored substitutions (each anchor exactly once). Their payload comes from the retained blobs: the
prefix-skip statements are checked line by line against pr1754 (stripped), the survivor method is apply.survivor_method
(sliced verbatim out of pr1908, renamed compactWords -> compactSurvivors: the owner already has a compactWords).
The one prefix-skip adaptation, `return to;` -> `return from + length;` (compactScalar has no `to`), is in ADAPTED.

usage: python -I variants.py --root DIR --out DIR [label...]   (default: all labels); prints one VARIANT line per file
"""
from __future__ import annotations

import argparse
import hashlib
import re
import sys
from pathlib import Path

sys.dont_write_bytecode = True   # no __pycache__ in the leaf dir
sys.path.insert(0, str(Path(__file__).resolve().parent))
import apply  # noqa: E402  (the owner recipe of this leaf)

POSTIMAGE_BLOB = "51ea3a72bb191afc250a58be964363bd5680467c"
PR1754_BLOB = "f85e7359010bd25e0a909a145e283007cff25d7c"   # = pr1802
LABELS = ("master", "postimage", "statonly", "threshold", "fixed", "threshold_prefix", "threshold_words1024",
          "fixed_prefix", "mutant")
ADAPTED = ("if (firstRemoved == length) return to;", "if (firstRemoved == length) return from + length;")

SCALAR_OLD = (
    "    private static int compactScalar(Object[] values, int from, int length, long[] deleted) {\n"
    "        int write = from;\n"
    "        for (int read = 0; read < length; read++) {\n"
    "            if ((deleted[read >>> 6] & (1L << (read & 63))) == 0) {\n"
    "                // The untouched prefix is already in place; preserve every\n"
    "                // reference without writing it again.\n"
    "                if (write != from + read) values[write] = values[from + read];\n"
    "                write++;\n"
    "            }\n"
    "        }\n")
SCALAR_NEW = (
    "    private static int compactScalar(Object[] values, int from, int length, long[] deleted) {\n"
    "        // Prefix before the first deletion is already at its final physical slots.\n"
    "        // Do not issue redundant reference stores or GC barriers for those entries.\n"
    "        int firstRemoved = next(deleted, 0, length, true);\n"
    "        if (firstRemoved == length) return from + length;\n"
    "        int write = from + firstRemoved;\n"
    "        for (int read = firstRemoved + 1; read < length; read++) {\n"
    "            if ((deleted[read >>> 6] & (1L << (read & 63))) == 0) {\n"
    "                values[write++] = values[from + read];\n"
    "            }\n"
    "        }\n")
DISPATCH_OLD = (
    "            return compactWords(values, from, length, deleted);\n"
    "        }\n"
    "        return compactScalar(values, from, length, deleted);\n")
DISPATCH_NEW = (
    "            return compactWords(values, from, length, deleted);\n"
    "        }\n"
    "        // Retain the scalar path for small fragmented windows.\n"
    "        if (length >= 1024) return compactSurvivors(values, from, length, deleted);\n"
    "        return compactScalar(values, from, length, deleted);\n")
STATISTICS_ANCHOR = "    /**\n     * Returns {@code runs << 32 | removed} over the whole mask;"


def check_prefix_provenance(pr1754: str) -> None:
    donor = {line.strip() for line in pr1754.splitlines()}
    for line in SCALAR_NEW.splitlines():
        stripped = line.strip()
        if stripped in ("", "}") or stripped == ADAPTED[1] or stripped.startswith("private static int compactScalar"):
            continue
        if stripped not in donor:
            raise apply.Refused("prefix-skip line not in pr1754 blob: %r" % stripped)
    if ADAPTED[0] not in donor:
        raise apply.Refused("declared adaptation source not in pr1754 blob: %r" % ADAPTED[0])


def rename(text: str, label: str) -> str:
    text = text.replace("package jdk.internal.mindex;", "package jbcoll;", 1)
    return re.sub(r"\bM3CollectionMaskCompactor\b", "MaskCompactor_" + label, text)


def variants(root: Path) -> dict[str, str]:
    blob = lambda b: apply.git_blob(root, b).decode("utf-8")  # noqa: E731
    master = blob(apply.PRE_BLOB)
    threshold = apply.postimage_text(master, apply.STAT_EDITS + apply.DISPATCH_EDITS)
    fixed = apply.postimage_text(master)
    check_prefix_provenance(blob(PR1754_BLOB))
    prefix = lambda t: apply.substitute(t, SCALAR_OLD, SCALAR_NEW, "prefix")  # noqa: E731
    method = apply.survivor_method(root) + "\n"

    def words1024(text: str) -> str:
        text = apply.substitute(text, DISPATCH_OLD, DISPATCH_NEW, "threshold_words1024")
        return apply.substitute(text, STATISTICS_ANCHOR, method + STATISTICS_ANCHOR, "threshold_words1024")

    mutant = apply.substitute(fixed, "(1L << (read & 63))) == 0) {", "(1L << (read & 62))) == 0) {", "mutant")
    return {"master": master, "postimage": blob(POSTIMAGE_BLOB),
            "statonly": apply.postimage_text(master, apply.STAT_EDITS), "threshold": threshold, "fixed": fixed,
            "threshold_prefix": prefix(threshold), "threshold_words1024": words1024(threshold),
            "fixed_prefix": prefix(fixed), "mutant": mutant}


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(apply.RECIPE_DIR.parents[2]))
    parser.add_argument("--out", required=True)
    parser.add_argument("labels", nargs="*")
    args = parser.parse_args(argv)
    try:
        texts = variants(Path(args.root).resolve())
    except apply.Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2
    out = Path(args.out) / "jbcoll"
    out.mkdir(parents=True, exist_ok=True)
    for label in args.labels or LABELS:
        data = rename(texts[label], label).encode("utf-8")
        (out / ("MaskCompactor_%s.java" % label)).write_bytes(data)
        print("VARIANT %s sha256=%s owner_sha256=%s" % (label, hashlib.sha256(data).hexdigest()[:16],
                                                       hashlib.sha256(texts[label].encode("utf-8")).hexdigest()[:16]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
