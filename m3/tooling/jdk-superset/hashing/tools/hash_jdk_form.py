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
  UNSAFE_TO_BYTE_ARRAY v1        UnsafeAccess (sun.misc.Unsafe over any object/address) becomes the byte[] access over
                                 jdk.internal.util.ByteArrayLittleEndian: Access<byte[]>, little-endian, BYTE_BASE 0,
                                 no Unsafe, no reflection, no pre-Nougat Android fallbacks
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

Checks: no forbidden token is left in code; the rules applied to their own output hit nothing (fixed point).
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
U = "UNSAFE_TO_BYTE_ARRAY v1"
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
    ("UnsafeAccess.java", U, "U2-unsafe-import-to-byte-array-le", r"^import sun\.misc\.Unsafe;\n",
     "import jdk.internal.util.ByteArrayLittleEndian;\n", ONE),
    ("UnsafeAccess.java", U, "U3-object-to-byte-array-type", r"\bAccess<Object>", "Access<byte[]>", 3),
    ("UnsafeAccess.java", U, "U4-drop-old-android-instance",
     r"^\n    // for test only\n    static final UnsafeAccess OLD_INSTANCE = [^;]*;\n", "", ONE),
    ("UnsafeAccess.java", U, "U5-drop-unsafe-field", r"^\n    static final Unsafe UNSAFE;\n", "", ONE),
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
    left = [(f, p) for f, t in texts.items() for p in FORBIDDEN if re.search(p, masked(t))]
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
