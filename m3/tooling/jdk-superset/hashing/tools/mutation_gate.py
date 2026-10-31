# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""mutation_gate.py <java bin dir> <absorbed src dir> <test classes dir> <junit classpath> <oracle classes> <work dir>

Discriminator gate of the zero-allocation-hashing absorption: each mutant is a plausible wrong java.base form (a
recipe slip). It is compiled into its own patch of java.base and M3HashAtomsTest runs against it; the gate passes
only when every mutant fails the test (KILLED). Prints and writes <work dir>/MUTANTS.tsv: mutant, file, result,
failing tests."""
import os
import re
import shutil
import subprocess
import sys

TAB = "\t"
NL = "\n"
PACKAGE_DIR = os.path.join("jdk", "internal", "m3", "hash")
# (mutant, file, exact code text, replacement)
MUTANTS = [
    ("BYTE_ARRAY_NATIVE_ORDER_BIG", "M3ByteArrayAccess.java", "return ByteOrder.LITTLE_ENDIAN;",
     "return ByteOrder.BIG_ENDIAN;"),
    ("BYTE_ARRAY_BASE_ONE", "M3ByteArrayAccess.java", "static final long BYTE_BASE = 0L;",
     "static final long BYTE_BASE = 1L;"),
    ("CHECK_OFFS_NO_OVERFLOW", "M3Util.java", " || off + len < 0)", ")"),
    ("CHARSEQ_LE_BYTE_SHIFT", "M3CharSequenceAccess.java",
     "return getUnsignedByte(input, offset, ((int) offset & 1) << BYTE_BITS_LOG2);",
     "return getUnsignedByte(input, offset, ((int) offset & 1) << 2);"),
    ("STRING_AS_LATIN1_BYTES", "M3LongHashFunction.java",
     "public long hashChars(String input) {\n        return hashNativeChars(input, 0, input.length());",
     "public long hashChars(String input) {\n        return hashBytes(input.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));"),
    ("MATHS_SIGNED_HIGH", "M3Maths.java", "return INSTANCE.unsignedLongMulHighImp(lhs, rhs);",
     "return Math.multiplyHigh(lhs, rhs);"),
    # the three tuple String rewrites of STRING_INTERNALS_TO_CHARSEQ that no test reached before (review of #2232)
    ("TUPLE_STRING_INTO_EMPTY", "M3LongTupleHashFunction.java",
     "public void hashChars(final String input, final long[] result) {\n"
     "        hashNativeChars(this, input, 0, input.length(), result);",
     "public void hashChars(final String input, final long[] result) {\n"
     "        hashNativeChars(this, input, 0, 0, result);"),
    ("TUPLE_STRING_SLICE_INTO_EMPTY", "M3LongTupleHashFunction.java",
     "public void hashChars(final String input, final int off, final int len, final long[] result) {\n"
     "        checkArrayOffs(input.length(), off, len);\n"
     "        hashNativeChars(this, input, off, len, result);",
     "public void hashChars(final String input, final int off, final int len, final long[] result) {\n"
     "        checkArrayOffs(input.length(), off, len);\n"
     "        hashNativeChars(this, input, 0, 0, result);"),
    ("TUPLE_STRING_SLICE_EMPTY", "M3LongTupleHashFunction.java",
     "public long[] hashChars(final String input, final int off, final int len) {\n"
     "        checkArrayOffs(input.length(), off, len);\n"
     "        final long[] result = newResultArray();\n"
     "        hashNativeChars(this, input, off, len, result);",
     "public long[] hashChars(final String input, final int off, final int len) {\n"
     "        checkArrayOffs(input.length(), off, len);\n"
     "        final long[] result = newResultArray();\n"
     "        hashNativeChars(this, input, 0, 0, result);"),
    # FACADE_DROP_RAW_MEMORY v1's form: a direct buffer read through its limit instead of up to its capacity
    ("DIRECT_BUFFER_LIMIT_BOUND", "M3LongHashFunction.java",
     "return hash(input.duplicate().order(input.order()).limit(input.capacity()),\n"
     "                    M3ByteBufferAccess.INSTANCE, off, len);",
     "return hash(input, M3ByteBufferAccess.INSTANCE, off, len);"),
    ("TUPLE_DIRECT_BUFFER_LIMIT_BOUND", "M3LongTupleHashFunction.java",
     "f.hash(input.duplicate().order(input.order()).limit(input.capacity()), BYTE_BUF_ACCESS, off, len, result);",
     "f.hash(input, BYTE_BUF_ACCESS, off, len, result);"),
]


def run(cmd):
    return subprocess.run(cmd, capture_output=True, text=True)


def main():
    jbin, src, testc, junit, oracle, work = sys.argv[1:7]
    javac, java = os.path.join(jbin, "javac"), os.path.join(jbin, "java")
    rows = []
    for name, f, old, new in MUTANTS:
        root = os.path.join(work, name)
        shutil.rmtree(root, ignore_errors=True)
        psrc, patch = os.path.join(root, "psrc"), os.path.join(root, "patch")
        shutil.copytree(src, os.path.join(psrc, PACKAGE_DIR))
        p = os.path.join(psrc, PACKAGE_DIR, f)
        text = open(p, encoding="utf-8", newline="").read()
        if text.count(old) != 1:
            raise SystemExit(f"{name}: anchor not found exactly once in {f}")
        open(p, "w", encoding="utf-8", newline="").write(text.replace(old, new))
        sources = [os.path.join(psrc, PACKAGE_DIR, x) for x in sorted(os.listdir(os.path.join(psrc, PACKAGE_DIR)))]
        c = run([javac, "-XDstringConcat=inline", "--patch-module", "java.base=" + psrc, "-d", patch] + sources)
        if c.returncode:
            rows.append((name, f, "COMPILE_ERROR", c.stderr.strip().splitlines()[0]))
            continue
        t = run([java, "-Xmx1g", "--patch-module", "java.base=" + patch,
                 "--add-opens", "java.base/jdk.internal.m3.hash=ALL-UNNAMED",
                 "--add-exports", "java.base/sun.nio.ch=ALL-UNNAMED", "-Dm3.hash.oracle=" + oracle,
                 "-cp", os.pathsep.join([testc, junit]), "M3GateJUnitRun", testc])
        failed = sorted(set(re.findall(r"M3HashAtomsTest:(\w+)\(\)", t.stdout)))
        rows.append((name, f, "KILLED" if t.returncode else "SURVIVED", ",".join(failed)))
    with open(os.path.join(work, "MUTANTS.tsv"), "w", encoding="utf-8", newline=NL) as w:
        w.write("mutant\tfile\tresult\tfailing_tests\n" + "".join(TAB.join(r) + NL for r in rows))
    for r in rows:
        print(TAB.join(r))
    survived = [r[0] for r in rows if r[2] != "KILLED"]
    print(f"mutants={len(rows)} killed={len(rows) - len(survived)}")
    sys.exit(1 if survived else 0)


main()
