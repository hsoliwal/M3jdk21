#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Generate apply.py of census2-c4-20261106 (recipe CENSUS2_C4 v1) from the donor's pure-addition hunks.

Reads the six hash-pinned donor pre/post pairs (three Java crates, three C text crates) at the census pass-2 pin,
normalises each added block to whole declarations (hunks.py) and writes the RULES table of apply.py with the blocks as
literal payloads, so apply.py replays without donor access. Declared transformations, each recorded in the rule note:
the donor JNI symbol prefix -> its JDK-naming rename (DONOR.tsv jni_from -> jni_to, fe35ea98841) and the drop of
the donor's mid-file "/* SPDX-License-Identifier: Apache-2.0 */" line in the C block (the target keeps its own
header). Every other byte of every block is the donor's. Donor path prefixes and the JNI prefix pair live in
DONOR.tsv next to this script (TSV is outside the JDK-naming scope, which would rename them inside a .py file).
usage: python -I gen_apply.py DONOR_GIT TEMPLATE_APPLY_PY OUT_APPLY_PY"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import hunks  # noqa: E402

GIT, TEMPLATE, OUT = sys.argv[1:4]
REV = "d5ad5090d87ef8477a56317e60ac0d29c65029f7"
DONOR = dict(line.split("\t")[:2] for line in (Path(__file__).resolve().parent / "DONOR.tsv")
             .read_text(encoding="utf-8").splitlines()[1:] if line.strip())
H, T, JNI_FROM, JNI_TO = DONOR["java_prefix"], DONOR["text_prefix"], DONOR["jni_from"], DONOR["jni_to"]
PAIR, LC315, KTH = ("m3jdk21-lc719-pair-distance-java-20261026", "m3jdk21-lc315-count-smaller-20261026",
                    "m3jdk21-lc719-kth-pair-distance-20261026")
CPAIR, C315, CKTH = ("m3jdk21-lc719-pair-distance-jni-20261026", "m3jdk21-lc315-count-smaller-native-20261026",
                     "m3jdk21-lc719-kth-pair-distance-jni-20261026")
SPDX_LINE = "/* SPDX-License-Identifier: Apache-2.0 */\n"


def java(crate, kind):
    return hunks.blocks(GIT, REV, H + crate + "/pre/" + kind + ".java.txt", H + crate + "/" + kind + ".java.txt")


def c(crate):
    return hunks.blocks(GIT, REV, T + crate + "/pre/m3_long_rank_batch_jni.c.txt", T + crate + "/m3_long_rank_batch_jni.c.txt")


def anchor(leaf, crate, post, what, indent="    "):
    return "%s//FLLDCIM: MRL - census %s, crate %s (donor blob %s): %s\n" % (indent, leaf, crate, post[:12], what)


def text(lines):
    return "".join(lines)


def jni(lines):
    return [l.replace(JNI_FROM, JNI_TO) for l in lines if l != SPDX_LINE]


pair, l315, kth = java(PAIR, "M3LongRankBatchNative"), java(LC315, "M3LongRankBatchNative"), java(KTH, "M3LongRankBatchNative")
ipair, i315, ikth = java(PAIR, "M3LongRankIndex"), java(LC315, "M3LongRankIndex"), java(KTH, "M3LongRankIndex")
cpair, c315, ckth = c(CPAIR), c(C315), c(CKTH)
assert [len(x) for x in (pair, l315, kth, ipair, i315, ikth, cpair, c315, ckth)] == [3, 2, 1, 1, 1, 1, 1, 1, 1]

FIELDS = "    private static volatile boolean TOP_K_FREQUENCY_ABI_MISSING;\n"
LOAD = "    private static boolean loadOptIn() {\n"
NATIVE = "    /** Additive LC #347 source-owned m3rankbatch JNI symbol. */\n"
BOUNDS = "    /** Java or explicitly enabled JNI batched lower bounds, without per-query boxing. */\n"
STREAM = "    /** Primitive stream over the immutable sorted snapshot, with no boxed values. */\n"
C_TAIL = "    free(keys);\n    free(frequencies);\n    if ((*env)->ExceptionCheck(env)) return NULL;\n    return output;\n}\n"

native_edits = [
    (FIELDS, FIELDS + anchor("P2-073", PAIR, pair[0][1], "LC 719 pair-distance batch thresholds and ABI flag")
     + text(pair[0][2]) + anchor("P2-073", LC315, l315[0][1], "LC 315 ABI flag") + text(l315[0][2])),
    (LOAD, anchor("P2-073", PAIR, pair[1][1], "LC 719 pair-distance counts (Java oracle + optional JNI batch)")
     + text(pair[1][2]) + "\n" + anchor("P2-073", LC315, l315[1][1], "LC 315 count smaller to the right (merge inversion + optional JNI)")
     + text(l315[1][2]) + "\n" + anchor("P2-073", KTH, kth[0][1], "LC 719 k-th smallest pair distance (bisection over the pair counts + optional JNI)")
     + text(kth[0][2]) + "\n" + LOAD),
    (NATIVE, anchor("P2-073", PAIR, pair[2][1], "LC 719 pair-distance JNI entry") + text(pair[2][2]) + "\n" + NATIVE),
]
index_edits = [
    (BOUNDS, anchor("P2-074", LC315, i315[0][1], "LC 315 facade") + text(i315[0][2]) + "\n" + BOUNDS),
    (STREAM, anchor("P2-074", PAIR, ipair[0][1], "LC 719 pair-distance facade") + text(ipair[0][2]) + "\n"
     + anchor("P2-074", KTH, ikth[0][1], "LC 719 k-th pair-distance facade") + text(ikth[0][2]) + "\n" + STREAM),
]
C_INCLUDES = "#include <jni.h>\n#include <stddef.h>\n#include <stdlib.h>\n"
c_edits = [
    (C_INCLUDES, "#include <jni.h>\n#include <stddef.h>\n"
     + "//FLLDCIM: MRL - census P2-073, crates " + CPAIR + " " + C315 + " " + CKTH
     + ": the crates' preimage include set (limits.h, stdint.h: INT_MAX, INT64_MAX, SIZE_MAX, uint64_t)\n"
     + "#include <limits.h>\n#include <stdint.h>\n#include <stdlib.h>\n"),
    (C_TAIL, C_TAIL + "\n" + anchor("P2-073", CPAIR, cpair[0][1], "LC 719 pair-distance counts kernel", "")
     + text(jni(cpair[0][2])) + "\n" + anchor("P2-073", C315, c315[0][1], "LC 315 count-smaller kernel", "")
     + text(jni(c315[0][2])) + "\n" + anchor("P2-073", CKTH, ckth[0][1], "LC 719 k-th pair-distance kernel", "")
     + text(jni(ckth[0][2]))),
]


def donors(*parts):
    return " ".join("%s:pre=%s,post=%s" % (name, pre[:12], post[:12]) for name, (pre, post, _b) in parts)


def rule(leaf, var, path, blob, crate_var, donor_note, note, edits):
    out = ['    ("%s", %s + "%s", "%s", %s,\n' % (leaf, var, path, blob, crate_var),
           "     %r,\n" % donor_note, "     %r, [\n" % note]
    for old, new in edits:
        out.append("         (%r,\n          %r),\n" % (old, new))
    out.append("     ]),\n")
    return "".join(out)


rules = "".join([
    rule("P2-073", "SRC", "M3LongRankBatchNative.java", "1672a1c590ce4ca55961c6f6749d11503eea1f5d", "JAVA_CRATES",
         donors(("pair", pair[0]), ("lc315", l315[0]), ("kth", kth[0])),
         "MAPPED: the three crates' pure-addition blocks (fields after the existing ABI flags, methods before"
         " loadOptIn, the pair-distance native before the LC 347 native); the crates' preimages carry the unlanded"
         " pr1882 rank stack, but every block uses only AVAILABLE, NATIVE_MIN_BATCH, Objects and its own members",
         native_edits),
    rule("P2-074", "SRC", "M3LongRankIndex.java", "6f3ad756ef31ed270078e217fca9c57c57bc1510", "JAVA_CRATES",
         donors(("pair", ipair[0]), ("lc315", i315[0]), ("kth", ikth[0])),
         "MAPPED: the three public facades over the batch owner, at the crates' own neighbours (before the batched"
         " lower bounds, before the primitive stream)", index_edits),
    rule("P2-073", "NATIVE", "m3_long_rank_batch_jni.c", "a4276617061bd509807843de68772b0379dd9c38", "C_CRATES",
         donors(("pair", cpair[0]), ("lc315", c315[0]), ("kth", ckth[0])),
         "MAPPED: the three C text crates' appended kernels at the end of the existing m3rankbatch source; declared:"
         " the donor JNI prefix -> " + JNI_TO + " (tools/DONOR.tsv), the donor's mid-file SPDX line dropped,"
         " <limits.h> and <stdint.h> added from the crates' preimage include set (the kernels use INT_MAX, INT64_MAX,"
         " SIZE_MAX and uint64_t; master's own topKFrequent0 already needs SIZE_MAX)",
         c_edits),
])
src = Path(TEMPLATE).read_text(encoding="utf-8")
start = src.index("RULES = (\n") + len("RULES = (\n")
end = src.index("\n)\n", start)
src = src[:start] + rules + src[end + 1:]
Path(OUT).write_text(src, encoding="utf-8", newline="\n")
print("rules written:", rules.count('("P2-'), "edits:", len(native_edits) + len(index_edits) + len(c_edits))
