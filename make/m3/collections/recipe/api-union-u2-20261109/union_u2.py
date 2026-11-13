# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""union_u2.py <repo root>   API_UNION U2 (user decision 2026-11-09: admit API unions): LongBitSet becomes one interface.

Synexia: interface com.synexia.common.collections.LongBitSet, implemented by PackedLongBitSet (and depended on by the lane
index classes). M3: public final class jdk.m3.collections.bits.LongBitSet with the same word-array model. One owner per
concept:
  1. jdk.m3.collections.bits.LongBitSet becomes the public INTERFACE: the Synexia contract plus the M3 queries; the only
     defaults are pure aliases (set(int, boolean), clearAll, toLongArray) and the factories of() / of(int);
  2. the M3 class body moves unchanged (query-fact caching included) to jdk.internal.m3.collections.bits.PackedLongBitSet,
     which implements it; an operand of another implementation is read through wordsCopy() (the Synexia boundary), and
     Synexia's intersectionCardinality / containsAll / wordsCopy are added;
  3. `new LongBitSet(...)` call sites of the module become `LongBitSet.of(...)`.
Exact, idempotent edits; refuses when an anchor is missing."""
import os
import re
import sys

root = sys.argv[1]
CL = os.path.join(root, "src", "jdk.m3.collections", "share", "classes")
pub = os.path.join(CL, "jdk", "m3", "collections", "bits", "LongBitSet.java")
impl = os.path.join(CL, "jdk", "internal", "m3", "collections", "bits", "PackedLongBitSet.java")
src = open(pub, encoding="utf-8", newline="").read()
if "public interface LongBitSet" in src:
    print("U2 already applied"); sys.exit(0)
nl = "\r\n" if "\r\n" in src else "\n"


def need(t, a, n=1):
    if t.count(a) != n:
        raise SystemExit(f"U2 anchor count {t.count(a)} != {n}: {a[:70]}")


# --- 1. implementation: the M3 class body, moved ---
t = src
for a, b in (
    ("package jdk.m3.collections.bits;", "package jdk.internal.m3.collections.bits;"),
    ("import java.util.function.IntConsumer;", "import java.util.function.IntConsumer;" + nl + "import jdk.m3.collections.bits.LongBitSet;"),
    ("public final class LongBitSet {", "public final class PackedLongBitSet implements LongBitSet {"),
    ("    public LongBitSet() { this(64); }", "    public PackedLongBitSet() { this(64); }"),
    ("    public LongBitSet(int bits) {", "    public PackedLongBitSet(int bits) {"),
):
    need(t, a)
    t = t.replace(a, b)
need(t, " * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names)." + nl)
t = t.replace(" * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names)." + nl,
              " * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names)." + nl
              + " *" + nl
              + " * <p>API_UNION U2 (2026-11-09): this body was the public class jdk.m3.collections.bits.LongBitSet; it is now the" + nl
              + " * one implementation of that interface and also owns Synexia's {@code PackedLongBitSet} (intersectionCardinality," + nl
              + " * containsAll, wordsCopy). An operand of another implementation is read through {@link LongBitSet#wordsCopy()}." + nl)
# operand access: other.words -> right (same class: the array itself; foreign: a snapshot)
for sig in ("    public boolean intersects(LongBitSet other) {", "    public void or(LongBitSet other) {",
            "    public void and(LongBitSet other) {", "    public void xor(LongBitSet other) {",
            "    public void andNot(LongBitSet other) {"):
    need(t, sig)
    start = t.index(sig)
    end = t.index(nl + "    }" + nl, start)
    body = t[start:end]
    if "if (other == null) throw new NullPointerException(\"other\");" in body:
        body = body.replace("if (other == null) throw new NullPointerException(\"other\");",
                            "if (other == null) throw new NullPointerException(\"other\");" + nl + "        long[] right = operandWords(other);", 1)
    else:
        body = body.replace(sig, sig + nl + "        long[] right = operandWords(other);", 1)
    body = body.replace("other.words", "right")
    t = t[:start] + body + t[end:]
add = nl.join([
    "    @Override",
    "    public long[] wordsCopy() { return words.clone(); }",
    "",
    "    /** Counts common bits without a result bitmap or int overflow (Synexia PackedLongBitSet). */",
    "    public long intersectionCardinality(LongBitSet other) {",
    "        long[] right = operandWords(other);",
    "        int common = Math.min(words.length, right.length);",
    "        long count = 0L;",
    "        for (int i = 0; i < common; i++) count += Long.bitCount(words[i] & right[i]);",
    "        return count;",
    "    }",
    "",
    "    /** Tests whether every bit of the operand is present in this set (Synexia PackedLongBitSet). */",
    "    public boolean containsAll(LongBitSet other) {",
    "        long[] right = operandWords(other);",
    "        for (int i = 0; i < right.length; i++) {",
    "            long left = i < words.length ? words[i] : 0L;",
    "            if ((right[i] & ~left) != 0L) return false;",
    "        }",
    "        return true;",
    "    }",
    "",
    "    /* Same-owner arrays never escape; a foreign implementation is read through one wordsCopy() snapshot. */",
    "    private static long[] operandWords(LongBitSet other) {",
    "        return other instanceof PackedLongBitSet packed ? packed.words : other.wordsCopy();",
    "    }",
    "", ""])
anchor = "    public long[] toLongArray() { return words.clone(); }" + nl
need(t, anchor)
t = t.replace(anchor, add + anchor)
os.makedirs(os.path.dirname(impl), exist_ok=True)
if os.path.exists(impl):
    raise SystemExit("U2: " + impl + " already exists")
open(impl, "w", encoding="utf-8", newline="").write(t)

# --- 2. the public interface ---
header = src[:src.index("package jdk.m3.collections.bits;")]
iface = header + nl.join([
    "package jdk.m3.collections.bits;",
    "",
    "import java.util.function.IntConsumer;",
    "import jdk.internal.m3.collections.bits.PackedLongBitSet;",
    "",
    "/**",
    " * Dense boolean set: one bit per non-negative ordinal, backed by 64-bit words.",
    " *",
    " * <p>API_UNION U2 (2026-11-09): one owner for the concept. The Synexia contract (com.synexia.common.collections",
    " * LongBitSet) and the queries of the former M3 class meet here; {@link #of(int)} returns the packed implementation.</p>",
    " */",
    "public interface LongBitSet {",
    "    /** Returns an empty set sized for 64 bits. */",
    "    static LongBitSet of() { return new PackedLongBitSet(); }",
    "",
    "    /** Returns an empty set sized for {@code bits} bits; it grows on demand. */",
    "    static LongBitSet of(int bits) { return new PackedLongBitSet(bits); }",
    "",
    "    /** Returns whether bit {@code bit} is set. */",
    "    boolean get(int bit);",
    "",
    "    /** Sets bit {@code bit}. */",
    "    void set(int bit);",
    "",
    "    /** Sets or clears bit {@code bit}. */",
    "    default void set(int bit, boolean value) {",
    "        if (value) set(bit); else clear(bit);",
    "    }",
    "",
    "    /** Clears bit {@code bit}. */",
    "    void clear(int bit);",
    "",
    "    /** Flips bit {@code bit}. */",
    "    void flip(int bit);",
    "",
    "    /** Clears every bit. */",
    "    void clear();",
    "",
    "    /** Clears every bit (Synexia name of {@link #clear()}). */",
    "    default void clearAll() { clear(); }",
    "",
    "    /** Returns the number of set bits. */",
    "    int cardinality();",
    "",
    "    /** Returns the first set bit at or after {@code from}, or -1. */",
    "    int nextSetBit(int from);",
    "",
    "    /** Returns the first clear bit at or after {@code from}. */",
    "    int nextClearBit(int from);",
    "",
    "    /** Returns the last set bit at or before {@code from}, or -1. */",
    "    int previousSetBit(int from);",
    "",
    "    /** Returns the last clear bit at or before {@code from}, or -1. */",
    "    int previousClearBit(int from);",
    "",
    "    /** Returns one past the highest set bit, or 0. */",
    "    int length();",
    "",
    "    /** Returns whether no bit is set. */",
    "    boolean isEmpty();",
    "",
    "    /** Returns the bit capacity of the current words. */",
    "    int capacityBits();",
    "",
    "    /** Returns the payload size in bytes. */",
    "    long payloadBytes();",
    "",
    "    /** Returns whether this set and {@code other} share a set bit. */",
    "    boolean intersects(LongBitSet other);",
    "",
    "    /** Calls {@code action} for every set bit in ascending order. */",
    "    void forEachSetBit(IntConsumer action);",
    "",
    "    /** Sets this set to its union with {@code other}. */",
    "    void or(LongBitSet other);",
    "",
    "    /** Sets this set to its intersection with {@code other}. */",
    "    void and(LongBitSet other);",
    "",
    "    /** Sets this set to its symmetric difference with {@code other}. */",
    "    void xor(LongBitSet other);",
    "",
    "    /** Clears every bit that is set in {@code other}. */",
    "    void andNot(LongBitSet other);",
    "",
    "    /** Returns a copy of the backing words. */",
    "    long[] wordsCopy();",
    "",
    "    /** Returns a copy of the backing words (M3 name of {@link #wordsCopy()}). */",
    "    default long[] toLongArray() { return wordsCopy(); }",
    "}",
    ""])
open(pub, "w", encoding="utf-8", newline="").write(iface)

# --- 3. call sites ---
n = 0
for d in (os.path.join(root, "src", "jdk.m3.collections"), os.path.join(root, "test", "jdk", "jdk")):
    for base, _, fs in os.walk(d):
        for f in fs:
            if not f.endswith(".java"):
                continue
            p = os.path.join(base, f)
            s = open(p, encoding="utf-8", newline="").read()
            if "new LongBitSet(" not in s or "jdk.m3.collections.bits" not in s and "package jdk.m3.collections.bits;" not in s \
                    and "import jdk.m3.collections.bits.*;" not in s:
                continue
            s2 = re.sub(r"\bnew LongBitSet\(", "LongBitSet.of(", s)
            if s2 != s:
                n += s.count("new LongBitSet(")
                open(p, "w", encoding="utf-8", newline="").write(s2)
print(f"U2 applied: interface jdk.m3.collections.bits.LongBitSet, implementation jdk.internal.m3.collections.bits.PackedLongBitSet, {n} call sites")
