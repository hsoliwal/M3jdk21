# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""mutation_gate.py <java bin dir> <absorbed src dir> <test classes dirs> <junit classpath> <oracle classes> <work dir>

Discriminator gate of the zero-allocation-hashing absorption: each mutant is a plausible wrong java.base form (a
recipe slip). It is compiled into its own patch of java.base and the tests run against it (<test classes dirs>: one
or more comma-separated class roots, each run on its own: M3HashAtomsTest, and M3HashJitAtomsTest for the JIT
leaf's mutants); the gate passes only when every mutant fails a test (KILLED). Prints and writes
<work dir>/MUTANTS.tsv: mutant, file, result, failing tests (Class.method beyond M3HashAtomsTest)."""
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
    ("CHARSEQ_LE_BYTE_SHIFT", "M3CharSequenceAccess.java", "return getUnsignedByte(input, offset, ((int) offset & 1) << 3);",
     "return getUnsignedByte(input, offset, ((int) offset & 1) << 2);"),
    ("STRING_AS_LATIN1_BYTES", "M3LongHashFunction.java",
     "public long hashChars(String input) {\n        return hashNativeChars(input, 0, input.length());",
     "public long hashChars(String input) {\n        return hashBytes(input.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));"),
    ("MATHS_SIGNED_HIGH", "M3Maths.java", "return INSTANCE.unsignedLongMulHighImp(lhs, rhs);",
     "return Math.multiplyHigh(lhs, rhs);"),
    # the JIT leaf: XXH3_SPLIT v1, UNSAFE_TO_BYTE_ARRAY v2, FACADE_ENTRY_CHECK v1
    ("XXH3_DISPATCH_EDGE", "M3XXH3.java", "        if (length <= 128) {\n            return XXH3_len_17to128_64b(",
     "        if (length < 128) {\n            return XXH3_len_17to128_64b("),
    ("XXH3_ATOM_SWAP", "M3XXH3.java", "        if (length > 8) {\n            return XXH3_len_9to16_128b(",
     "        if (length > 8) {\n            return XXH3_len_4to8_128b("),
    ("XXH3_MERGE_OFFSET", "M3XXH3.java", "+ XXH3_mix2Accs(acc_4, acc_5, secret, offSec + 16 * 2)",
     "+ XXH3_mix2Accs(acc_4, acc_5, secret, offSec + 16 * 2 + 1)"),
    ("XXH3_HASHLONG_HIGH_START", "M3XXH3.java", "secret, BYTE_BASE + 192 - 64 - 11, ~(length * XXH_PRIME64_2)",
     "secret, BYTE_BASE + 192 - 64 - 11, length * XXH_PRIME64_2"),
    ("READ_ATOM_BIG_ENDIAN", "M3ByteArrayAccess.java", "return UNSAFE.getLongUnaligned(input, address(offset), false);",
     "return UNSAFE.getLongUnaligned(input, address(offset), true);"),
    ("READ_ATOM_ADDRESS", "M3ByteArrayAccess.java", "return Unsafe.ARRAY_BYTE_BASE_OFFSET + offset;",
     "return Unsafe.ARRAY_BYTE_BASE_OFFSET + offset + 1;"),
    ("REVERSE_SHORT_NAME_SIGNED", "M3Access.java",
     "        public long u32(final T input, final long offset) {\n            return getUnsignedInt(input, offset);",
     "        public long u32(final T input, final long offset) {\n            return getInt(input, offset);"),
    ("BUFFER_SHORT_NAME_SWAPPED", "M3ByteBufferAccess.java",
     "    public long i64(final ByteBuffer input, final long offset) {\n        return getLong(input, offset);",
     "    public long i64(final ByteBuffer input, final long offset) {\n        return Long.reverseBytes(getLong(input, offset));"),
    ("ENTRY_CHECK_DROPPED", "M3LongHashFunction.java",
     "        java.util.Objects.checkFromIndexSize(off, len, input.length);\n        return hash(input,",
     "        return hash(input,"),
    ("TUPLE_ENTRY_CHECK_DROPPED", "M3LongTupleHashFunction.java",
     "        java.util.Objects.checkFromIndexSize(off, len, input.length);\n        f.hash(input, OBJECT_ACCESS,",
     "        f.hash(input, OBJECT_ACCESS,"),
]
FAILED = re.compile(r"(M3Hash\w*Test):(\w+)\(\)")
BASE_TEST = "M3HashAtomsTest"


def run(cmd):
    return subprocess.run(cmd, capture_output=True, text=True)


def main():
    jbin, src, roots, junit, oracle, work = sys.argv[1:7]
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
        killed, failed = False, set()
        for testc in roots.split(","):
            t = run([java, "-Xmx1g", "-XX:ErrorFile=" + os.path.join(root, "hs_err_%p.log"),
                     "--patch-module", "java.base=" + patch,
                     "--add-opens", "java.base/jdk.internal.m3.hash=ALL-UNNAMED",
                     "--add-exports", "java.base/sun.nio.ch=ALL-UNNAMED", "-Dm3.hash.oracle=" + oracle,
                     "-cp", os.pathsep.join([testc, junit]), "M3GateJUnitRun", testc])
            killed |= t.returncode != 0
            failed |= {m if c == BASE_TEST else c + "." + m for c, m in FAILED.findall(t.stdout)}
        rows.append((name, f, "KILLED" if killed else "SURVIVED", ",".join(sorted(failed))))
        print(TAB.join(rows[-1]), flush=True)
    with open(os.path.join(work, "MUTANTS.tsv"), "w", encoding="utf-8", newline=NL) as w:
        w.write("mutant\tfile\tresult\tfailing_tests\n" + "".join(TAB.join(r) + NL for r in rows))
    survived = [r[0] for r in rows if r[2] != "KILLED"]
    print(f"mutants={len(rows)} killed={len(rows) - len(survived)}")
    sys.exit(1 if survived else 0)


main()
