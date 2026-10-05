/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.ArrayList;
import java.util.Objects;

/**
 * M3JDK canonical String value.
 *
 * <p>This is the M3JDK counterpart of Synexia MIndexString. The representation invariant is
 * deliberately the same shape while the target name is different:</p>
 *
 * <pre>
 * M3String = canonical owner + packed coordinate
 * </pre>
 *
 * <p>Text payload belongs to canonical native/mapped atom owners. Composition belongs to
 * persistent tuple owner nodes. This value retains no byte[], char[], segment[], offset[] or
 * end[] payload. Contiguous arrays exist only as explicit JNI compatibility shadows.</p>
 */
final class M3String implements CharSequence {
    private static final int SPAN_SHIFT = Integer.SIZE;
    private static final long SPAN_MASK = 0xffff_ffffL;

    private static final M3StringAtom EMPTY_OWNER =
            new M3StringAtom(null, 0L, (byte) 1, true, 0, String.LATIN1, 0, 0L,
                    0x9e3779b97f4a7c15L);
    private static final M3String EMPTY = new M3String(EMPTY_OWNER, span(0, 0));

    private static volatile boolean ready;
    private static final ThreadLocal<Boolean> ADMITTING = new ThreadLocal<>();

    /*
     * Absolute M3 representation invariant: these are the only instance fields.
     * HotSpot/recipes/tests must fail if payload fields are added here.
     */
    private final M3StringOwner owner;
    private final long value;

    private M3String(M3StringOwner owner, long value) {
        this.owner = Objects.requireNonNull(owner, "owner");
        int start = start(value);
        int length = count(value);
        Objects.checkFromIndexSize(start, length, owner.length);
        this.value = value;
    }

    static M3String whole(M3StringOwner owner) {
        return owner.length == 0 ? EMPTY : new M3String(owner, span(0, owner.length));
    }

    static M3String range(M3StringOwner owner, int start, int length) {
        if (length == 0) return EMPTY;
        if (start == 0 && length == owner.length) return whole(owner);
        return new M3String(owner, span(start, length));
    }

    static M3String empty() {
        return EMPTY;
    }

    static void activate(String lexiconFile) {
        if (ready) return;
        synchronized (M3String.class) {
            if (ready) return;
            ADMITTING.set(Boolean.TRUE);
            try {
                M3StringPool.initializeLexicon(lexiconFile);
            } finally {
                ADMITTING.remove();
            }
            ready = true;
        }
    }

    static boolean ready() {
        return ready;
    }

    static boolean admissionEnabled() {
        return ready && ADMITTING.get() != Boolean.TRUE;
    }

    static M3String admit(byte[] compactValue, byte coder) {
        Objects.requireNonNull(compactValue, "compactValue");
        ADMITTING.set(Boolean.TRUE);
        try {
            return M3StringPool.internScalar(compactValue, coder);
        } finally {
            ADMITTING.remove();
        }
    }

    static M3String join(String first, String second) {
        return join(new String[] {first, second});
    }

    static M3String join(String[] parts) {
        Objects.requireNonNull(parts, "parts");
        ArrayList<M3String> level = new ArrayList<>(parts.length);
        for (String part : parts) {
            String checked = Objects.requireNonNull(part, "part");
            M3String storage = checked.m3();
            if (storage != null && storage.length() != 0) level.add(storage);
        }
        if (level.isEmpty()) return EMPTY;
        while (level.size() > 1) {
            ArrayList<M3String> next = new ArrayList<>((level.size() + 1) >>> 1);
            for (int index = 0; index < level.size(); index += 2) {
                if (index + 1 == level.size()) next.add(level.get(index));
                else next.add(M3StringPool.concat(level.get(index), level.get(index + 1)));
            }
            level = next;
        }
        return level.getFirst();
    }

    static M3String sliceOf(String source, int beginIndex, int endIndex) {
        Objects.requireNonNull(source, "source");
        M3String storage = source.m3();
        return storage == null ? null : storage.slice(beginIndex, endIndex);
    }

    M3String slice(int beginIndex, int endIndex) {
        Objects.checkFromToIndex(beginIndex, endIndex, length());
        if (beginIndex == 0 && endIndex == length()) return this;
        int length = endIndex - beginIndex;
        return range(owner, Math.addExact(start(), beginIndex), length);
    }

    M3String concat(M3String other) {
        return M3StringPool.concat(this, Objects.requireNonNull(other, "other"));
    }

    M3String repeat(int repetitions) {
        if (repetitions < 0) throw new IllegalArgumentException("count is negative: " + repetitions);
        if (repetitions == 0 || length() == 0) return EMPTY;
        if (repetitions == 1) return this;
        Math.multiplyExact(length(), repetitions);

        M3String result = EMPTY;
        M3String base = this;
        int remaining = repetitions;
        while (remaining != 0) {
            if ((remaining & 1) != 0) result = M3StringPool.concat(result, base);
            remaining >>>= 1;
            if (remaining != 0) base = M3StringPool.concat(base, base);
        }
        return result;
    }

    M3StringOwner owner() {
        return owner;
    }

    long coordinate() {
        return value;
    }

    int start() {
        return start(value);
    }

    int end() {
        return start() + length();
    }

    @Override
    public int length() {
        return count(value);
    }

    byte coder() {
        return owner.coder;
    }

    long structuralHash64() {
        return owner.structuralHash64;
    }

    boolean sameCoordinate(M3String other) {
        return other != null && owner == other.owner && value == other.value;
    }

    long identityHash64() {
        long identity = Integer.toUnsignedLong(System.identityHashCode(owner));
        long mixed = identity ^ Long.rotateLeft(value, 17) ^ owner.structuralHash64;
        mixed ^= mixed >>> 30;
        mixed *= 0xbf58476d1ce4e5b9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94d049bb133111ebL;
        return mixed ^ (mixed >>> 31);
    }

    @Override
    public char charAt(int index) {
        Objects.checkIndex(index, length());
        return owner.charAt(start() + index);
    }

    @Override
    public M3String subSequence(int beginIndex, int endIndex) {
        return slice(beginIndex, endIndex);
    }

    int hashCodeValue() {
        if (start() == 0 && length() == owner.length) return owner.javaHash;
        int hash = 0;
        for (int index = 0; index < length(); index++) hash = 31 * hash + charAt(index);
        return hash;
    }

    M3StringFacts facts() {
        return start() == 0 && length() == owner.length
                ? owner.facts()
                : owner.rangeFacts(value, this);
    }

    boolean contentEquals(String other) {
        Objects.requireNonNull(other, "other");
        M3String that = other.m3();
        if (that != null && sameCoordinate(that)) return true;
        if (other.length() != length()) return false;
        for (int index = 0; index < length(); index++) {
            if (charAt(index) != other.charAt(index)) return false;
        }
        return true;
    }

    void getChars(int sourceBegin, int sourceEnd, char[] destination, int destinationBegin) {
        String.checkBoundsBeginEnd(sourceBegin, sourceEnd, length());
        Objects.checkFromIndexSize(destinationBegin, sourceEnd - sourceBegin, destination.length);
        owner.getChars(
                Math.addExact(start(), sourceBegin),
                Math.addExact(start(), sourceEnd),
                destination,
                destinationBegin);
    }

    void getBytes(byte[] destination, int sourceBegin, int destinationBegin, byte destinationCoder,
            int count) {
        Objects.checkFromIndexSize(sourceBegin, count, length());
        Objects.checkFromIndexSize(
                destinationBegin << destinationCoder,
                count << destinationCoder,
                destination.length);
        for (int index = 0; index < count; index++) {
            char unit = charAt(sourceBegin + index);
            if (destinationCoder == String.LATIN1) {
                destination[destinationBegin + index] = (byte) unit;
            } else {
                StringUTF16.putChar(destination, destinationBegin + index, unit);
            }
        }
    }

    /**
     * JNI-created Compact-String compatibility shadow. Never cached by M3String or its owners.
     */
    byte[] materialize() {
        return nativeByteShadow(this, 0, length(), coder());
    }

    byte[] compatibilityValue() {
        return materialize();
    }

    /** JNI-created final Java UTF-16 array shadow. */
    char[] charShadow() {
        return nativeCharShadow(this, 0, length());
    }

    boolean mayContain(M3String needle) {
        Objects.requireNonNull(needle, "needle");
        if (needle.length() > length()) return false;
        long required = needle.facts().bitSignal64;
        return (facts().bitSignal64 & required) == required;
    }

    static int pow31(int length) {
        int result = 1;
        int base = 31;
        for (int remaining = length; remaining != 0; remaining >>>= 1) {
            if ((remaining & 1) != 0) result *= base;
            base *= base;
        }
        return result;
    }

    private static long span(int start, int length) {
        if (start < 0 || length < 0) throw new IllegalArgumentException("negative M3 range");
        return ((long) start << SPAN_SHIFT) | (length & SPAN_MASK);
    }

    private static int start(long coordinate) {
        return (int) (coordinate >>> SPAN_SHIFT);
    }

    private static int count(long coordinate) {
        return (int) (coordinate & SPAN_MASK);
    }

    private static native byte[] nativeByteShadow(
            M3String value, int start, int length, byte coder);

    private static native char[] nativeCharShadow(
            M3String value, int start, int length);
}
