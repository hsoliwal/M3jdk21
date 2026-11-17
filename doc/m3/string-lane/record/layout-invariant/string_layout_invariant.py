# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""string_layout_invariant.py <String.java in> <String.java out> <M3String.java in> <M3String.java out>

STRING_LAYOUT_INVARIANT v1 (2026-11-17), owner decision 47: every String of the JDK holds
  private final byte[] value;   // Java 21 compatibility: the interned bytes
  private final char[] chValue; // Java 21 compatibility: the interned chars, JNI array, protected internally
both filled at construction. Java side of the invariant:
  1. value is the real bytes at every construction site; the zero-length M3 layout sentinel is retired
     (M3String.compatibilityValue() and its cache go), M3-admitted Strings built without bytes take the JNI byte
     shadow (M3String.materialize()), value() returns the field, m3AdmitNative keeps the payload.
  2. chValue is declared after value and assigned once in every constructor, right after the m3 field:
     String(String) shares the original's array; every other site takes layoutChars(value, coder, m3)
     (M3-backed: the JNI char shadow; flat: StringLatin1.toChars / StringUTF16.toChars).
javac's definite-assignment rule for the final field proves that no constructor path is missed.
Every anchor must match the stated number of times; the recipe refuses otherwise."""
import io
import re
import sys

STRING_IN, STRING_OUT, M3_IN, M3_OUT = sys.argv[1:5]


def read(path):
    with io.open(path, encoding="utf-8", newline="") as stream:
        return stream.read()


def write(path, text):
    with io.open(path, "w", encoding="utf-8", newline="") as stream:
        stream.write(text)


def replace(text, old, new, label, count=1):
    if text.count(old) != count:
        sys.exit("REFUSE %s: anchor found %d times, expected %d" % (label, text.count(old), count))
    return text.replace(old, new)


s = read(STRING_IN)

OLD_DOC = """    /**
     * Compact-String compatibility shadow.
     *
     * <p>For M3-backed Strings this array is not canonical identity or canonical payload; it is a
     * compatibility projection retained while remaining JDK/HotSpot consumers are migrated.
     * Shadows produced from M3String are allocated by the JNI shadow boundary. Bootstrap Strings
     * created before M3 activation retain the stock representation.</p>
     *
     * <p>M3JDK 21 retains the ordinary Java 21 String layout <em>as well as</em> its
     * canonical M3 owner/coordinate. For a nonempty M3-backed String, this field refers
     * to the VM-wide shared zero-length byte[] sentinel, not a duplicate spelling.
     * The coder and m3 fields describe the real immutable text; native region readers
     * traverse the interned arrays and OS-mapped owner ranges.</p>
     *
     * <p>A returned byte[] or char[] must remain a genuine mutable Java array with
     * the ordinary identity and copy semantics. It is an explicit JNI-produced snapshot,
     * never an alias to read-only or discontiguous OS-mapped storage. Java 21 String
     * has no additional char[] instance field.</p>
     *
     * @implNote The field remains VM-trusted during the migration and is never mutated.
     */
    @Stable
    private final byte[] value;
"""
NEW_DOC = """    /**
     * The bytes of this string: Java 21 compatibility is the interned bytes.
     *
     * <p>M3JDK layout invariant (doc/m3/string-lane/STRING_LAYOUT_INVARIANT.md): every String of
     * the JDK holds its text twice in Java 21 form, as these bytes (Latin-1 or UTF-16 by
     * {@code coder}, exactly as Java 21 lays them out) and as the chars of {@link #chValue}. Both
     * arrays are filled when the String is made, by every constructor and by every VM site that
     * makes a String, and neither is ever written again. An M3-backed String keeps its canonical
     * M3 owner and coordinate in {@code m3} as well; these bytes are then the JNI byte shadow of
     * that text, never a sentinel.</p>
     *
     * @implNote This field is trusted by the VM and is a subject to constant folding if the
     * String instance is constant. Overwriting it after construction will cause problems.
     */
    @Stable
    private final byte[] value;

    /**
     * The chars of this string: Java 21 compatibility is the interned chars.
     *
     * <p>A JNI-ready {@code char[]} of {@link #length()} UTF-16 units, filled when the String is
     * made (layout invariant, see {@link #value}) and protected internally: it is never handed to
     * a caller, which only ever receives a copy, and never written after construction.</p>
     */
    @Stable
    private final char[] chValue;
"""
s = replace(s, OLD_DOC, NEW_DOC, "value doc + chValue field")

# 1. value is the real bytes at every construction site
s = replace(s, "        this.value = storage == null ? original.value : storage.compatibilityValue();\n",
            "        this.value = original.value;\n", "String(String) value")
s = replace(s, "            this.value = direct.compatibilityValue();\n",
            "            this.value = direct.materialize();\n", "direct admission value", 3)
s = replace(s, "        this.value = storage == null ? candidate : storage.compatibilityValue();\n",
            "        this.value = candidate;\n", "candidate value")
s = replace(s, "            this.value = construction.storage.compatibilityValue();\n",
            "            this.value = construction.storage.materialize();\n", "decoded M3 value")
s = replace(s, "        this.value = storage == null ? construction.value : storage.compatibilityValue();\n",
            "        this.value = construction.value;\n", "decoded flat value")
s = replace(s, "        this.value = storage == null ? value : storage.compatibilityValue();\n",
            "        this.value = value;\n", "shared-array value")
s = replace(s, "        this.value = checked.compatibilityValue();\n",
            "        this.value = checked.materialize();\n", "String(M3String) value")
s = replace(s, """        M3String storage = checked.ensureM3();
        // JNI NewString/NewStringUTF used a temporary Compact-String construction shadow.
        // Once canonical M3 admission succeeds, discard that payload and retain only the VM
        // layout sentinel. Future byte/char arrays are explicit JNI compatibility shadows.
        return storage == null ? checked.value : storage.compatibilityValue();
""", """        checked.ensureM3();
        // Layout invariant: the bytes the VM made stay this String's bytes after M3 admission.
        return checked.value;
""", "m3AdmitNative keeps the payload")
s = replace(s, """    byte[] value() {
        M3String storage = m3();
        return storage == null ? value : storage.materialize();
    }
""", """    byte[] value() {
        return value;
    }
""", "value() returns the field")

# 2. chValue right after every m3 field assignment of a constructor
s = replace(s, """        this.m3 = storage;
        this.hash = original.hash;
""", """        this.m3 = storage;
        this.chValue = original.chValue;
        this.hash = original.hash;
""", "String(String) shares chValue")
assigned = re.compile(r"^( +)this\.m3 = ([A-Za-z.]+);\n(?! +this\.chValue)", re.M)
s, sites = assigned.subn(lambda m: m.group(0) + m.group(1)
                         + "this.chValue = layoutChars(this.value, this.coder, this.m3);\n", s)
if sites != 17:
    sys.exit("REFUSE chValue sites: %d m3 assignments, expected 17" % sites)

HELPER_ANCHOR = """    M3String m3() {
        return m3;
    }
"""
s = replace(s, HELPER_ANCHOR, HELPER_ANCHOR + """
    /**
     * The chars of the layout invariant for a String under construction: the JNI char shadow
     * of its M3 text when it is M3-backed, otherwise its bytes inflated to UTF-16 units.
     */
    private static char[] layoutChars(byte[] bytes, byte coder, M3String storage) {
        if (storage != null) return storage.charShadow();
        return COMPACT_STRINGS && coder == LATIN1 ? StringLatin1.toChars(bytes)
                                                 : StringUTF16.toChars(bytes);
    }
""", "layoutChars helper")
write(STRING_OUT, s)

m = read(M3_IN)
m = replace(m, """    /** Shared zero-length VM compatibility shadow, created by JNI after M3 activation. */
    private static volatile byte[] emptyCompatibilityShadow;
""", "", "M3String sentinel cache")
m = replace(m, """    byte[] compatibilityValue() {
        // VM layout sentinel only. Canonical text lives behind owner+coordinate.
        // Even the empty sentinel is created by the explicit JNI shadow boundary.
        byte[] shadow = emptyCompatibilityShadow;
        if (shadow != null) return shadow;
        synchronized (M3String.class) {
            shadow = emptyCompatibilityShadow;
            if (shadow == null) {
                shadow = nativeByteShadow(EMPTY, 0, 0, String.LATIN1);
                emptyCompatibilityShadow = shadow;
            }
            return shadow;
        }
    }

""", "", "M3String sentinel accessor")
write(M3_OUT, m)
print("STRING_LAYOUT_INVARIANT v1 applied: chValue at %d sites + String(String)" % sites)
