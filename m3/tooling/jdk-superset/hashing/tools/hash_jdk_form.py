# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""hash_jdk_form.py <donor_dir> <TYPE_RENAME.tsv> <stage_dir> <report_dir>

HASH_JDK_FORM v1, step 1 of 2 of the zero-allocation-hashing absorption into java.base (step 2 is
absorb_rename_generic.py on <stage_dir> with ABSORB_PKGS=net.openhft.hashing=jdk.internal.m3.hash).

Copies the donor files named in TYPE_RENAME.tsv from <donor_dir> to <stage_dir> and applies the rules below. Every
rule states its expected hit count; any other count stops the run (the donor changed under the recipe). Edits are
code-only: SUB patterns are anchored on code text, DROP finds declarations on a comment/string-masked copy and
removes the member with its annotations, its doc comment and one blank line before it.

  STRIP_JSR305 v1                javax.annotation.ParametersAreNonnullByDefault (compile-only donor dependency)
  UNSAFE_TO_BYTE_ARRAY v2        UnsafeAccess (sun.misc.Unsafe over any object/address) becomes the byte[] access:
                                 Access<byte[]>, little-endian, BYTE_BASE 0, no sun.misc.Unsafe, no reflection, no
                                 pre-Nougat Android fallbacks. The getters (getLong, getInt, ...) read through
                                 jdk.internal.util.ByteArrayLittleEndian, bounds-checked as before; v2 (the JIT leaf):
                                 the short names the kernels call (i64, u32, i32, u16, i16) read through
                                 jdk.internal.misc.Unsafe getLongUnaligned/getIntUnaligned/getShortUnaligned without a
                                 per-read check, because every entry that hands a byte[] to a kernel checks
                                 [off, off + len) first (FACADE_ENTRY_CHECK v1) and XXH3's secret offsets are constants
  ACCESS_DROP_UNSAFE v1          Access.unsafe() (raw object/address access) is not kept
  FACADE_DROP_RAW_MEMORY v1      LongHashFunction / LongTupleHashFunction: the methods that need raw memory reads
                                 (boolean[], char[], short[], int[], long[], addresses) are not kept; byte[] goes
                                 through the byte[] access; direct ByteBuffers through ByteBufferAccess
  STRING_INTERNALS_TO_CHARSEQ v1 hashChars(String) takes the donor's own JVM-independent path (UnknownJvmStringHash:
                                 native-order chars through CharSequenceAccess) instead of the per-JVM String-internals
                                 variants, which are not kept; same hash values by the donor's definition
  UTIL_DROP_JVM_PROBES v1        Util keeps checkArrayOffs only (no java.vm.name probes, no DirectBuffer address)
  XXH3_SECRET_ACCESS v1          XXH3 reads its secrets through the byte[] access (Access<byte[]>)
  LINT_CLEAN v1                  java.base compiles with -Xlint:all -Werror: @SuppressWarnings("fallthrough") on the
                                 MurmurHash3 tail switch (deliberate fall-through), XXH3's redundant (byte) cast dropped
  CHANGE_NOTICE v1               every kept file states above its package line that it was changed (Apache-2.0 4(b))
  XXH3_SPLIT v1                  XXH3's input-length classes (the xxHash reference functions XXH3_len_0to16, 1to3,
                                 4to8, 9to16, 17to128, 129to240, 64 and 128 bit) become private static atoms of the
                                 same file, so that C2 compiles each with its own inlining budget; XXH3_hashLong_64b is
                                 the low half of XXH3_hashLong_128b, so one shared XXH3_hashLong (with XXH3_mergeAccs)
                                 serves both; statements are moved, never rewritten; //FLLDCIM: anchors on each atom
  ACCESS_SHORT_NAMES v1          the other concrete accesses (ByteBufferAccess, CharSequenceAccess, Access's
                                 ReverseAccess) carry the Access short-name defaults verbatim (i64 returns getLong, ...),
                                 so that a kernel's read site, no longer monomorphic once the byte[] access has its own
                                 short names, dispatches once to the access class and not again inside Access.i64
  FACADE_ENTRY_CHECK v1          LongHashFunction / LongTupleHashFunction: the private byte[] entry (unsafeHash), which
                                 every public byte[] and heap ByteBuffer method reaches after its own range check,
                                 checks [off, off + len) again with Objects.checkFromIndexSize before the kernel reads
                                 through the unchecked short names

Checks: no forbidden token is left in code (UnsafeAccess may name jdk.internal.misc.Unsafe, its only Unsafe import);
the rules applied to their own output hit nothing (fixed point).
Reports: <report_dir>/RULE_COUNTS.tsv (file, family, rule, expected, observed) and FILE_HASHES.tsv (donor and staged
sha256 of every kept file)."""
import hashlib
import os
import re
import shutil
import sys

MASK = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])*\'', re.S)
ONE = 1
TAB = "\t"
NL = "\n"
JAVA = ".java"
FORBIDDEN = (r"\bsun\.misc\b", r"\bsun\.nio\b", r"\bUnsafe\b", r"\bUNSAFE\b", r"\bDirectBuffer\b",
             r"\bVALID_STRING_HASH\b", r"\bStringHash\b", r"\bjavax\.annotation\b", r"\bParametersAreNonnullByDefault\b",
             r"\b(?:BOOLEAN|CHAR|SHORT|INT|LONG)_BASE\b", r"\b(?:TRUE|FALSE)_BYTE_VALUE\b", r"\bOLD_INSTANCE\b",
             r"\bCompactLatin1CharSequenceAccess\b", r"\bhashMemory\b", r"\bhashBooleans?\b", r"\bhashShorts\b",
             r"\bhashInts\b", r"\bhashLongs\b", r"\bgetDirectBufferAddress\b", r"\breflect\b")
JSR305_IMPORT = r"^import javax\.annotation\.ParametersAreNonnullByDefault;\n"
JSR305_ANNOTATION = r"^@ParametersAreNonnullByDefault\n"
DIRECT_BUFFER_IMPORT = r"^import sun\.nio\.ch\.DirectBuffer;\n"
U = "UNSAFE_TO_BYTE_ARRAY v2"
# UnsafeAccess reads through jdk.internal.misc.Unsafe (v2): these two tokens are allowed in that file only
ALLOWED = {"UnsafeAccess.java": (r"\bUnsafe\b", r"\bUNSAFE\b")}
INTERNAL_UNSAFE_IMPORT = "import jdk.internal.misc.Unsafe;\n"
F = "FACADE_DROP_RAW_MEMORY v1"
S = "STRING_INTERNALS_TO_CHARSEQ v1"
J = "STRIP_JSR305 v1"
P = "UTIL_DROP_JVM_PROBES v1"
L = "LINT_CLEAN v1"
NOTICE = ("/*\n"
          " * Changed by Synexia (2026-10-31): zero-allocation-hashing 0.16 (Apache License 2.0) absorbed into java.base\n"
          " * as jdk.internal.m3.hash; the changes are listed in src/java.base/share/legal/zero-allocation-hashing.md.\n"
          " */\n")
PACKAGE_LINE = r"^package net\.openhft\.hashing;"

# (file, family, rule id, regex (re.M; (?s) where marked), replacement, expected hits)
SUBS = [(f, J, r, p, "", ONE) for f in ("DualHashFunction.java", "LongTupleHashFunction.java", "MurmurHash_3.java")
        for r, p in (("jsr305-import", JSR305_IMPORT), ("jsr305-annotation", JSR305_ANNOTATION))]
SUBS += [
    ("UnsafeAccess.java", U, "U1-drop-reflect-import", r"^import java\.lang\.reflect\.Field;\n", "", ONE),
    ("UnsafeAccess.java", U, "U2-unsafe-import-to-internal-unsafe-and-byte-array-le", r"^import sun\.misc\.Unsafe;\n",
     INTERNAL_UNSAFE_IMPORT + "import jdk.internal.util.ByteArrayLittleEndian;\n", ONE),
    ("UnsafeAccess.java", U, "U3-object-to-byte-array-type", r"\bAccess<Object>", "Access<byte[]>", 3),
    ("UnsafeAccess.java", U, "U4-drop-old-android-instance",
     r"^\n    // for test only\n    static final UnsafeAccess OLD_INSTANCE = [^;]*;\n", "", ONE),
    ("UnsafeAccess.java", U, "U5-internal-unsafe-field", r"^\n    static final Unsafe UNSAFE;\n",
     "\n    private static final Unsafe UNSAFE = Unsafe.getUnsafe();\n", ONE),
    ("UnsafeAccess.java", U, "U6-drop-other-array-bases", r"^    static final long (?:BOOLEAN|CHAR|SHORT|INT|LONG)_BASE;\n",
     "", 5),
    ("UnsafeAccess.java", U, "U7-byte-base-is-index-zero", r"static final long BYTE_BASE;",
     "static final long BYTE_BASE = 0L;", ONE),
    ("UnsafeAccess.java", U, "U8-drop-boolean-byte-values",
     r"^\n    static final byte TRUE_BYTE_VALUE;\n    static final byte FALSE_BYTE_VALUE;\n", "", ONE),
    ("UnsafeAccess.java", U, "U9-drop-unsafe-probe-init",
     r"(?s)^(    static \{\n)        try \{\n.*?hasGetByte = false;\n        \}\n\n", r"\1", ONE),
    ("UnsafeAccess.java", U, "U10-plain-instance", r"INSTANCE = hasGetByte \? new UnsafeAccess\(\) : OLD_INSTANCE;",
     "INSTANCE = new UnsafeAccess();", ONE),
    ("UnsafeAccess.java", U, "U11-object-to-byte-array-input", r"\(Object input,", "(byte[] input,", 7),
    ("UnsafeAccess.java", U, "U12-object-to-byte-array-order", r"\(Object input\)", "(byte[] input)", ONE),
    ("UnsafeAccess.java", U, "U13-unsafe-get-to-byte-array-le", r"\bUNSAFE\.get(Long|Int|Short)\(input, offset\)",
     r"ByteArrayLittleEndian.get\1(input, (int) offset)", 3),
    ("UnsafeAccess.java", U, "U14-unsafe-get-byte-to-index", r"\bUNSAFE\.getByte\(input, offset\)", "input[(int) offset]",
     ONE),
    ("UnsafeAccess.java", U, "U15-order-little-endian", r"\bByteOrder\.nativeOrder\(\)", "ByteOrder.LITTLE_ENDIAN", ONE),
    ("UnsafeAccess.java", U, "U16-drop-old-android-classes",
     r"(?s)^\n    private static class OldUnsafeAccessLittleEndian extends UnsafeAccess \{\n.*?\n    \}\n\n"
     r"    private static class OldUnsafeAccessBigEndian extends UnsafeAccess \{\n.*?\n    \}\n", "", ONE),
    ("UnsafeAccess.java", U, "U17-unchecked-short-name-reads",
     r"^    private UnsafeAccess\(\) \{\}\n(?!\n    //FLLDCIM: HPCPOP - UNSAFE_TO_BYTE_ARRAY v2)", r"\g<0>" + (
         "\n    //FLLDCIM: HPCPOP - UNSAFE_TO_BYTE_ARRAY v2: the read atom's address (index base 0, no check)\n"
         "    private static long address(final long offset) {\n"
         "        return Unsafe.ARRAY_BYTE_BASE_OFFSET + offset;\n"
         "    }\n"
         + "".join(
             "\n    //FLLDCIM: HPCPOP - UNSAFE_TO_BYTE_ARRAY v2: unchecked " + name + "; the caller checked [off, off + len)\n"
             "    @Override\n"
             "    public " + ret + " " + name + "(final byte[] input, final long offset) {\n"
             "        return " + body + ";\n"
             "    }\n"
             for name, ret, body in (
                 ("i64", "long", "UNSAFE.getLongUnaligned(input, address(offset), false)"),
                 ("u32", "long", "unsignedInt(UNSAFE.getIntUnaligned(input, address(offset), false))"),
                 ("i32", "int", "UNSAFE.getIntUnaligned(input, address(offset), false)"),
                 ("u16", "int", "unsignedShort(UNSAFE.getShortUnaligned(input, address(offset), false))"),
                 ("i16", "int", "UNSAFE.getShortUnaligned(input, address(offset), false)")))),
     ONE),

    ("LongHashFunction.java", F, "direct-buffer-import", DIRECT_BUFFER_IMPORT, "", ONE),
    ("LongHashFunction.java", F, "byte-array-unsafe-hash", r"private long unsafeHash\(Object input,",
     "private long unsafeHash(byte[] input,", ONE),
    ("LongHashFunction.java", F, "direct-buffer-through-buffer-access",
     r"^        \} else if \(input instanceof DirectBuffer\) \{\n"
     r"            return unsafeHash\(null, \(\(DirectBuffer\) input\)\.address\(\) \+ off, len\);\n", "", ONE),
    ("LongHashFunction.java", S, "string-native-chars", r"\bVALID_STRING_HASH\.longHash\(input, this, ",
     "hashNativeChars(input, ", 2),

    ("LongTupleHashFunction.java", F, "direct-buffer-import", DIRECT_BUFFER_IMPORT, "", ONE),
    ("LongTupleHashFunction.java", F, "byte-array-access-field", r"\bAccess<Object> OBJECT_ACCESS\b",
     "Access<byte[]> OBJECT_ACCESS", ONE),
    ("LongTupleHashFunction.java", F, "byte-array-unsafe-hash", r"@Nullable final Object input,",
     "@Nullable final byte[] input,", ONE),
    ("LongTupleHashFunction.java", F, "direct-buffer-through-buffer-access",
     r"^        \} else if \(input instanceof DirectBuffer\) \{\n"
     r"            unsafeHash\(f, null, \(\(DirectBuffer\) input\)\.address\(\) \+ off, len, result\);\n", "", ONE),
    ("LongTupleHashFunction.java", S, "string-native-chars", r"\bVALID_STRING_HASH\.hash\(input, this, ",
     "hashNativeChars(this, input, ", 4),

    ("Util.java", P, "direct-buffer-import", DIRECT_BUFFER_IMPORT, "", ONE),
    ("XXH3.java", "XXH3_SECRET_ACCESS v1", "secret-byte-array-access", r"\bAccess<Object> unsafeLE\b",
     "Access<byte[]> unsafeLE", ONE),

    ("MurmurHash_3.java", L, "fallthrough-tail-switch",
     r'(?<!@SuppressWarnings\("fallthrough"\)\n)^    private static <T> long hash\(long seed, ',
     '    @SuppressWarnings("fallthrough")\n\\g<0>', ONE),
    ("XXH3.java", L, "redundant-byte-cast", r"final int c2 = \(byte\)input;", "final int c2 = input;", ONE),
]

TYPED_ARRAYS = (("hashChars", "char"), ("hashShorts", "short"), ("hashInts", "int"), ("hashLongs", "long"))
# (file, family, declaration line as written, without a trailing "{")
DROPS = [("Access.java", "ACCESS_DROP_UNSAFE v1", "public static <T> Access<T> unsafe()")]
DROPS += [("LongHashFunction.java", F, s) for s in (
    "public long hashBoolean(boolean input)",
    "public long hashBooleans(@NotNull boolean[] input)",
    "public long hashBooleans(@NotNull boolean[] input, int off, int len)",
    "public long hashMemory(long address, long len)")]
DROPS += [("LongHashFunction.java", F, s) for m, t in TYPED_ARRAYS for s in (
    f"public long {m}(@NotNull {t}[] input)",
    f"public long {m}(@NotNull {t}[] input, int off, int len)")]
DROPS += [("LongTupleHashFunction.java", F, s) for s in (
    "public void hashBoolean(final boolean input, final long[] result)",
    "public long[] hashBoolean(final boolean input)",
    "public void hashBooleans(final boolean[] input, final long[] result)",
    "public long[] hashBooleans(final boolean[] input)",
    "public void hashBooleans(final boolean[] input,",
    "public long[] hashBooleans(final boolean[] input, final int off, final int len)",
    "public void hashMemory(final long address, final long len, final long[] result)",
    "public long[] hashMemory(final long address, final long len)")]
DROPS += [("LongTupleHashFunction.java", F, s) for m, t in TYPED_ARRAYS for s in (
    f"public void {m}(final {t}[] input, final long[] result)",
    f"public long[] {m}(final {t}[] input)",
    f"public void {m}(final {t}[] input, final int off, final int len, final long[] result)",
    f"public long[] {m}(final {t}[] input, final int off, final int len)")]
DROPS += [("Util.java", P, s) for s in (
    "static private boolean isHotSpotVM(@NotNull final String name)",
    "static private boolean isJ9VM(@NotNull final String name)",
    "static final StringHash VALID_STRING_HASH;",
    "static",
    "static long getDirectBufferAddress(@NotNull final ByteBuffer buff)")]

# XXH3_SPLIT v1 (the JIT leaf): XXH3's length classes become atoms; see split_xxh3
X = "XXH3_SPLIT v1"
XXH3 = "XXH3.java"
SHORT64 = "final long seed, final T input, final Access<T> access, final long off, final long length"
SHORT128 = SHORT64 + ", final long[] result"
ARGS64 = "seed, input, access, off, length"
ARGS128 = ARGS64 + ", result"
LONG_PARAMS = ("final byte[] secret, final T input, final Access<T> access, final long off, final long length, "
               "final long[] result")
MERGE_PARAMS = ("final long acc_0, final long acc_1, final long acc_2, final long acc_3, final long acc_4, "
                "final long acc_5, final long acc_6, final long acc_7, final byte[] secret, final long offSec, "
                "final long start")
ANCHOR = "    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: {}\n"
DISPATCH_NOTE = "length-class dispatcher; each class is its own atom with its own C2 inlining budget"
# (parent, donor marker = atom name, parameters, arguments, input lengths): inner blocks first
XXH3_BLOCKS = [(parent, f"XXH3_len_{c}_{w}", params, args, lengths)
               for parent, w, params, args in (("XXH3_64bits_internal", "64b", SHORT64, ARGS64),
                                               ("XXH3_128bits_internal", "128b", SHORT128, ARGS128))
               for c, lengths in (("9to16", "9..16"), ("4to8", "4..8"), ("1to3", "1..3"), ("0to16", "0..16"),
                                  ("17to128", "17..128"), ("129to240", "129..240"))]
# the atoms follow their parent in this order
NEW_ORDER = ("0to16", "9to16", "4to8", "1to3", "17to128", "129to240")
MERGE_START = "        // XXH3_mergeAccs\n"
MERGE_64 = ("        // XXH3_mergeAccs\n"
            "        final long result64 = length * XXH_PRIME64_1\n"
            "                + XXH3_mix2Accs(acc_0, acc_1, secret, BYTE_BASE + 11)\n"
            "                + XXH3_mix2Accs(acc_2, acc_3, secret, BYTE_BASE + 11 + 16)\n"
            "                + XXH3_mix2Accs(acc_4, acc_5, secret, BYTE_BASE + 11 + 16 * 2)\n"
            "                + XXH3_mix2Accs(acc_6, acc_7, secret, BYTE_BASE + 11 + 16 * 3);\n"
            "\n"
            "        return XXH3_avalanche(result64);\n")
MERGE_FOLDS = (r"\n {{16,20}}\+ XXH3_mix2Accs\(acc_0, acc_1, secret, {0}\)"
               r"\n {{16,20}}\+ XXH3_mix2Accs\(acc_2, acc_3, secret, {0} \+ 16\)"
               r"\n {{16,20}}\+ XXH3_mix2Accs\(acc_4, acc_5, secret, {0} \+ 16 \* 2\)"
               r"\n {{16,20}}\+ XXH3_mix2Accs\(acc_6, acc_7, secret, {0} \+ 16 \* 3\)\)")
ACCS = "acc_0, acc_1, acc_2, acc_3, acc_4, acc_5, acc_6, acc_7"
SUBS += [
    (XXH3, X, "merge-low-through-mergeAccs",
     r"^        final long low = XXH3_avalanche\(length \* XXH_PRIME64_1" + MERGE_FOLDS.format(r"BYTE_BASE \+ 11") + ";",
     f"        final long low = XXH3_avalanche(XXH3_mergeAccs({ACCS},\n"
     "                secret, BYTE_BASE + 11, length * XXH_PRIME64_1));", ONE),
    (XXH3, X, "merge-high-through-mergeAccs",
     r"^            result\[1\] = XXH3_avalanche\(~\(length \* XXH_PRIME64_2\)"
     + MERGE_FOLDS.format(r"BYTE_BASE \+ 192 - 64 - 11") + ";",
     f"            result[1] = XXH3_avalanche(XXH3_mergeAccs({ACCS},\n"
     "                    secret, BYTE_BASE + 192 - 64 - 11, ~(length * XXH_PRIME64_2)));", ONE),
]
SUBS += [(XXH3, X, "anchor-" + p, "(?<!" + re.escape(ANCHOR.format(DISPATCH_NOTE)) + ")"
          + r"^    private static <T> long " + p + r"\(", ANCHOR.format(DISPATCH_NOTE) + r"\g<0>", ONE)
         for p in ("XXH3_64bits_internal", "XXH3_128bits_internal")]

# ACCESS_SHORT_NAMES v1 (the JIT leaf): every other concrete access carries the M3Access short-name defaults verbatim
A = "ACCESS_SHORT_NAMES v1"
SHORT_NAMES = (("long", "i64", "getLong"), ("long", "u32", "getUnsignedInt"), ("int", "i32", "getInt"),
               ("int", "u16", "getUnsignedShort"), ("int", "i16", "getShort"), ("int", "u8", "getUnsignedByte"),
               ("int", "i8", "getByte"))
SHORT_NOTE = "    {0}//FLLDCIM: HPCPOP - ACCESS_SHORT_NAMES v1: {1} dispatches once, to this class's own getter\n"


def short_names(indent, input_type):
    return "".join(
        "\n" + SHORT_NOTE.format(indent, name)
        + f"    {indent}@Override\n"
        + f"    {indent}public {ret} {name}(final {input_type} input, final long offset) {{\n"
        + f"    {indent}    return {getter}(input, offset);\n"
        + f"    {indent}}}\n"
        for ret, name, getter in SHORT_NAMES)


SUBS += [
    ("Access.java", A, "reverse-access-short-names",
     r"^        private ReverseAccess\(final Access<T> access\) \{\n            this\.access = access;\n        \}\n"
     r"(?!\n        //FLLDCIM: HPCPOP - ACCESS_SHORT_NAMES v1)", r"\g<0>" + short_names("    ", "T"), ONE),
    ("ByteBufferAccess.java", A, "byte-buffer-access-short-names",
     r"^    private ByteBufferAccess\(\) \{\}\n(?!\n    //FLLDCIM: HPCPOP - ACCESS_SHORT_NAMES v1)",
     r"\g<0>" + short_names("", "ByteBuffer"), ONE),
    ("CharSequenceAccess.java", A, "char-sequence-access-short-names",
     r"^    private CharSequenceAccess\(\) \{\}\n(?!\n    //FLLDCIM: HPCPOP - ACCESS_SHORT_NAMES v1)",
     r"\g<0>" + short_names("", "CharSequence"), ONE),
]

# FACADE_ENTRY_CHECK v1 (the JIT leaf): the private byte[] entry checks [off, off + len) before the unchecked reads
E = "FACADE_ENTRY_CHECK v1"
ENTRY_ANCHOR = ("    //FLLDCIM: HPCPOP - FACADE_ENTRY_CHECK v1: every byte[] reaches the kernels here; checked before the"
                " unchecked short-name reads\n")
ENTRY_CHECK = "        java.util.Objects.checkFromIndexSize(off, len, input.length);\n"
SUBS += [
    ("LongHashFunction.java", E, "anchor-unsafeHash",
     "(?<!" + re.escape(ENTRY_ANCHOR) + r")^    private long unsafeHash\(byte\[\] input, long off, long len\) \{\n",
     ENTRY_ANCHOR + r"\g<0>", ONE),
    ("LongHashFunction.java", E, "check-unsafeHash",
     r"(?<=\(byte\[\] input, long off, long len\) \{\n)(?!" + re.escape(ENTRY_CHECK) + ")"
     r"        return hash\(input, UnsafeAccess\.INSTANCE, off, len\);\n", ENTRY_CHECK + r"\g<0>", ONE),
    ("LongTupleHashFunction.java", E, "anchor-unsafeHash",
     "(?<!" + re.escape(ENTRY_ANCHOR) + r")^    private static void unsafeHash\(final LongTupleHashFunction f, ",
     ENTRY_ANCHOR + r"\g<0>", ONE),
    ("LongTupleHashFunction.java", E, "check-unsafeHash",
     r"(?<=final long off, final long len, final long\[\] result\) \{\n)(?!" + re.escape(ENTRY_CHECK) + ")"
     r"        f\.hash\(input, OBJECT_ACCESS, off, len, result\);\n", ENTRY_CHECK + r"\g<0>", ONE),
]


def method_end(lines, name):
    """Index of the closing '    }' line of the member whose header names <name>."""
    head = [i for i, line in enumerate(lines) if re.match(r"    private static .*\b" + name + r"\(", line)]
    if len(head) != ONE:
        raise SystemExit(f"{X}: {name} declared {len(head)} times")
    return head[0], next(i for i in range(head[0] + 1, len(lines)) if lines[i] == "    }")


def dedent(body, n):
    if any(line and not line.startswith(" " * n) for line in body):
        raise SystemExit(f"{X}: a moved line is indented less than {n}")
    return [line[n:] for line in body]


def new_method(note, header, body):
    return ["", ANCHOR.format(note).rstrip(NL), "    " + header + " {"] + body + ["    }"]


def split_xxh3(texts, counts):
    """XXH3_SPLIT v1: every input-length class of XXH3_64bits_internal and XXH3_128bits_internal becomes a private
    static atom named by the donor's own marker comment (the xxHash reference functions); the parent keeps the length
    tests and returns the atom's value. XXH3_hashLong_64b is the low half of XXH3_hashLong_128b (same accumulators,
    same merge at secret offset 11; checked on the donor text), so both parents call one XXH3_hashLong, whose merges
    go through XXH3_mergeAccs. Statement text is moved, never rewritten (only re-indented)."""
    lines = texts[XXH3].split(NL)
    made = {}
    for parent, marker, params, args, lengths in XXH3_BLOCKS:
        start, end = method_end(lines, parent)
        hits = [i for i in range(start, end) if lines[i].strip() == "// " + marker
                and re.match(r" +if \(.*\) \{$", lines[i - 1])]
        counts.append((XXH3, X, "atom " + marker, ONE, len(hits)))
        if len(hits) != ONE:
            continue
        i = hits[0]
        ind = re.match(r" *", lines[i - 1]).group(0)
        close = next(k for k in range(i + 1, end) if lines[k] == ind + "}")
        made[marker] = new_method(f"atom for input lengths {lengths} (xxHash {marker})",
                                  f"private static <T> long {marker}({params})",
                                  dedent(lines[i:close], len(ind) + 4 - 8))
        lines[i:close] = [ind + "    return " + marker + "(" + args + ");"]
    tails = {}
    for parent, marker in (("XXH3_64bits_internal", "XXH3_hashLong_64b_internal"),
                           ("XXH3_128bits_internal", "XXH3_hashLong_128b_internal")):
        start, end = method_end(lines, parent)
        hits = [i for i in range(start, end) if lines[i] == "        // " + marker]
        counts.append((XXH3, X, "tail " + marker, ONE, len(hits)))
        if len(hits) == ONE:
            tails[parent] = (hits[0], end)
    if len(tails) == 2:
        (s64, e64), (s128, e128) = tails["XXH3_64bits_internal"], tails["XXH3_128bits_internal"]
        t64, t128 = NL.join(lines[s64:e64]) + NL, NL.join(lines[s128:e128]) + NL
        acc64, merge64 = t64[:t64.index(MERGE_START)], t64[t64.index(MERGE_START):]
        acc128 = t128[:t128.index(MERGE_START)]
        if acc64.replace("XXH3_hashLong_64b_internal", "XXH3_hashLong_128b_internal") != acc128 or merge64 != MERGE_64:
            raise SystemExit(f"{X}: XXH3_hashLong_64b is no longer the low half of XXH3_hashLong_128b in the donor")
        made["XXH3_hashLong"] = new_method(
            "atom for input lengths above 240, shared: XXH3_hashLong_64b is the low half of XXH3_hashLong_128b",
            f"private static <T> long XXH3_hashLong({LONG_PARAMS})", lines[s128:e128])
        made["XXH3_mergeAccs"] = new_method(
            "XXH3_mergeAccs, the four lane-pair folds of the hashLong accumulators",
            f"private static long XXH3_mergeAccs({MERGE_PARAMS})",
            ["        return start", "                + XXH3_mix2Accs(acc_0, acc_1, secret, offSec)",
             "                + XXH3_mix2Accs(acc_2, acc_3, secret, offSec + 16)",
             "                + XXH3_mix2Accs(acc_4, acc_5, secret, offSec + 16 * 2)",
             "                + XXH3_mix2Accs(acc_6, acc_7, secret, offSec + 16 * 3);"])
        lines[s128:e128] = ["        return XXH3_hashLong(secret, input, access, off, length, result);"]
        lines[s64:e64] = ["        return XXH3_hashLong(secret, input, access, off, length, null);"]
    for parent, w, extra in (("XXH3_128bits_internal", "128b", ("XXH3_hashLong", "XXH3_mergeAccs")),
                             ("XXH3_64bits_internal", "64b", ())):
        block = [line for c in NEW_ORDER for line in made.get(f"XXH3_len_{c}_{w}", [])]
        block += [line for name in extra for line in made.get(name, [])]
        end = method_end(lines, parent)[1]
        lines[end + 1:end + 1] = block
    texts[XXH3] = NL.join(lines)


def masked(s):
    return MASK.sub(lambda m: "".join(c if c == NL else " " for c in m.group(0)), s)


def sha256(s):
    return hashlib.sha256(s.encode("utf-8")).hexdigest()


def declaration_lines(s, decl):
    ml = masked(s).split(NL)
    return [i for i, line in enumerate(ml) if line.strip().removesuffix("{").rstrip() == decl]


def member_span(s, i):
    """[first, last] line indexes of the member declared at line i: annotations, the doc or block comment right above,
    one blank line above that; down to the ';' or the matching '}' of the body."""
    lines, m = s.split(NL), masked(s)
    first = i
    while first > 0 and lines[first - 1].strip().startswith("@"):
        first -= 1
    if first > 0 and lines[first - 1].strip().endswith("*/"):
        first -= 1
        while not lines[first].strip().startswith("/*"):
            first -= 1
    if first > 0 and not lines[first - 1].strip():
        first -= 1
    pos = sum(len(x) + 1 for x in lines[:i])
    semi, brace = m.find(";", pos), m.find("{", pos)
    if brace < 0 or 0 <= semi < brace:
        end = semi
    else:
        depth, end = 0, brace
        while True:
            depth += (m[end] == "{") - (m[end] == "}")
            if depth == 0:
                break
            end += 1
    return first, m.count(NL, 0, end)


def notice_rules(files):
    pat = "(?<!" + re.escape(NOTICE) + ")" + PACKAGE_LINE
    return [(f, "CHANGE_NOTICE v1", "notice-above-package", pat, NOTICE.replace("\\", "\\\\") + r"\g<0>", ONE)
            for f in sorted(files)]


def apply(texts, counts):
    for f, fam, rid, pat, rep, want in SUBS + notice_rules(texts):
        texts[f], n = re.subn(pat, rep, texts[f], flags=re.M)
        counts.append((f, fam, rid, want, n))
    for f, fam, decl in DROPS:
        hits = declaration_lines(texts[f], decl)
        if len(hits) == ONE:
            first, last = member_span(texts[f], hits[0])
            lines = texts[f].split(NL)
            texts[f] = NL.join(lines[:first] + lines[last + 1:])
        counts.append((f, fam, "drop " + decl, ONE, len(hits)))
    split_xxh3(texts, counts)


def main():
    donor, ren, stage, rep = sys.argv[1:5]
    with open(ren, encoding="utf-8") as fh:
        rows = [line.rstrip(NL).split(TAB) for line in fh][1:]
    keep = [a.rsplit(".", 1)[1] + JAVA for a, _ in rows if os.path.exists(os.path.join(donor, a.rsplit(".", 1)[1] + JAVA))]
    texts = {f: open(os.path.join(donor, f), encoding="utf-8", newline="").read() for f in keep}
    before = {f: sha256(t) for f, t in texts.items()}
    counts = []
    apply(texts, counts)
    bad = [c for c in counts if c[3] != c[4]]
    if bad:
        raise SystemExit("rule counts differ from the recipe: " + repr(bad))
    left = [(f, p) for f, t in texts.items() for p in FORBIDDEN if p not in ALLOWED.get(f, ()) and re.search(p, masked(t))]
    left += [(f, "imports other than " + INTERNAL_UNSAFE_IMPORT.strip()) for f in ALLOWED
             if re.findall(r"^import .*\bUnsafe\b.*$", texts[f], re.M) != [INTERNAL_UNSAFE_IMPORT.strip()]]
    if left:
        raise SystemExit("forbidden tokens left in code: " + repr(left))
    again, recount = dict(texts), []
    apply(again, recount)
    moved = [c for c in recount if c[4]] + [f for f in texts if again[f] != texts[f]]
    if moved:
        raise SystemExit("not a fixed point: " + repr(moved))
    shutil.rmtree(stage, ignore_errors=True)
    os.makedirs(stage)
    os.makedirs(rep, exist_ok=True)
    for f, t in texts.items():
        with open(os.path.join(stage, f), "w", encoding="utf-8", newline="") as w:
            w.write(t)
    with open(os.path.join(rep, "RULE_COUNTS.tsv"), "w", encoding="utf-8", newline=NL) as w:
        w.write("file\tfamily\trule\texpected\tobserved\n")
        w.write("".join(TAB.join(map(str, c)) + NL for c in counts))
    with open(os.path.join(rep, "FILE_HASHES.tsv"), "w", encoding="utf-8", newline=NL) as w:
        w.write("file\tdonor_sha256\tstaged_sha256\n")
        w.write("".join(f"{f}\t{before[f]}\t{sha256(texts[f])}\n" for f in sorted(texts)))
    print(f"files={len(texts)} rules={len(counts)} changed={sum(before[f] != sha256(texts[f]) for f in texts)}"
          f" forbidden_left=0 fixed_point=yes")


main()
