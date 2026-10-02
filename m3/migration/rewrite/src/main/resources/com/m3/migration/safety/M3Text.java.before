/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stock-JVM explicit M3 text value backed by immutable M3 pieces.
 *
 * <p>Admission of an ordinary String copies its UTF-16 code units once into VM-local immutable
 * storage. Concatenation, slicing and repeat retain existing immutable payload pieces and allocate
 * descriptor metadata only. Conversion back to String, char[] or bytes is an explicit
 * materialization boundary.</p>
 *
 * <p>Canonical wrapper identity is JVM-local and weak: equal live values admitted through
 * {@link #fromString(String)} or structural operations may reuse the same M3Text object. Java
 * String object identity and String.intern semantics remain separate.</p>
 */
public final class M3Text implements CharSequence, Comparable<M3Text> {
    private static final LocalM3Arena LOCAL_ARENA = new LocalM3Arena();
    private static final WeakInterner INTERNER = new WeakInterner();
    private static final M3Text EMPTY = new M3Text(M3StringPiece.join(), 0);

    static {
        INTERNER.publish(EMPTY);
    }

    private final M3StringPiece piece;
    private final int hash;

    private M3Text(M3StringPiece piece, int hash) {
        this.piece = Objects.requireNonNull(piece, "piece");
        this.hash = hash;
    }

    public static M3Text empty() {
        return EMPTY;
    }

    /**
     * Admits exact Java UTF-16 content into VM-local immutable storage.
     * Existing live canonical content is reused before allocating a new payload.
     */
    public static M3Text fromString(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isEmpty()) return EMPTY;
        int hash = value.hashCode();
        M3Text existing = INTERNER.find(value, hash);
        if (existing != null) return existing;
        char[] chars = value.toCharArray();
        M3StringPiece piece = LOCAL_ARENA.copyUtf16(chars);
        return INTERNER.intern(piece, hash);
    }

    /** Exact admission from a CharSequence without retaining the caller's mutable representation. */
    public static M3Text from(CharSequence value) {
        Objects.requireNonNull(value, "value");
        if (value instanceof M3Text text) return text;
        if (value instanceof String string) return fromString(string);
        int length = value.length();
        if (length == 0) return EMPTY;
        int hash = hashOf(value);
        M3Text existing = INTERNER.find(value, hash);
        if (existing != null) return existing;
        char[] snapshot = new char[length];
        for (int i = 0; i < length; i++) snapshot[i] = value.charAt(i);
        return INTERNER.intern(LOCAL_ARENA.copyUtf16(snapshot), hash);
    }

    @Override
    public int length() {
        return piece.length();
    }

    @Override
    public char charAt(int index) {
        return piece.charAt(index);
    }

    @Override
    public M3Text subSequence(int start, int end) {
        return substring(start, end);
    }

    /** Java String UTF-16 code-unit slicing; splitting a surrogate pair is permitted. */
    public M3Text substring(int beginIndex, int endIndex) {
        Objects.checkFromToIndex(beginIndex, endIndex, length());
        if (beginIndex == 0 && endIndex == length()) return this;
        if (beginIndex == endIndex) return EMPTY;
        M3StringPiece range = piece.subSequence(beginIndex, endIndex);
        return INTERNER.intern(range, hashOf(range));
    }

    public M3Text substring(int beginIndex) {
        return substring(beginIndex, length());
    }

    /** Joins immutable payload references; it does not create a joined character payload. */
    public M3Text concat(M3Text other) {
        Objects.requireNonNull(other, "other");
        if (other.isEmpty()) return this;
        if (isEmpty()) return other;
        Math.addExact(length(), other.length());
        M3StringPiece joined = M3StringPiece.join(piece, other.piece);
        int joinedHash = composeHash(hash, other.hash, other.length());
        return INTERNER.intern(joined, joinedHash);
    }

    /** Admits the right operand once, then performs descriptor-only concatenation. */
    public M3Text concat(String other) {
        return concat(fromString(Objects.requireNonNull(other, "other")));
    }

    /** Repeats through balanced descriptor composition; no result-sized text payload is created. */
    public M3Text repeat(int count) {
        if (count < 0) throw new IllegalArgumentException("count is negative: " + count);
        if (count == 0 || isEmpty()) return EMPTY;
        if (count == 1) return this;
        long expanded = (long) length() * count;
        if (expanded > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("Required length exceeds implementation limit");
        }
        M3Text result = EMPTY;
        M3Text power = this;
        int remaining = count;
        while (remaining != 0) {
            if ((remaining & 1) != 0) result = result.concat(power);
            remaining >>>= 1;
            if (remaining != 0) power = power.concat(power);
        }
        return result;
    }

    public boolean isEmpty() {
        return length() == 0;
    }

    public boolean contentEquals(CharSequence other) {
        return other != null && contentEquals(this, other);
    }

    public boolean startsWith(CharSequence prefix) {
        Objects.requireNonNull(prefix, "prefix");
        if (prefix.length() > length()) return false;
        return regionEquals(this, 0, prefix, 0, prefix.length());
    }

    public boolean endsWith(CharSequence suffix) {
        Objects.requireNonNull(suffix, "suffix");
        if (suffix.length() > length()) return false;
        return regionEquals(this, length() - suffix.length(), suffix, 0, suffix.length());
    }

    public int indexOf(CharSequence needle) {
        Objects.requireNonNull(needle, "needle");
        int n = needle.length();
        if (n == 0) return 0;
        int limit = length() - n;
        for (int i = 0; i <= limit; i++) {
            if (charAt(i) == needle.charAt(0) && regionEquals(this, i, needle, 0, n)) return i;
        }
        return -1;
    }

    public int lastIndexOf(CharSequence needle) {
        Objects.requireNonNull(needle, "needle");
        int n = needle.length();
        if (n == 0) return length();
        for (int i = length() - n; i >= 0; i--) {
            if (charAt(i) == needle.charAt(0) && regionEquals(this, i, needle, 0, n)) return i;
        }
        return -1;
    }

    /** Stock java.util.regex works directly because M3Text is an exact CharSequence. */
    public boolean matches(Pattern pattern) {
        return Objects.requireNonNull(pattern, "pattern").matcher(this).matches();
    }

    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        Objects.requireNonNull(dst, "dst");
        Objects.checkFromToIndex(srcBegin, srcEnd, length());
        Objects.checkFromIndexSize(dstBegin, srcEnd - srcBegin, dst.length);
        for (int i = srcBegin, j = dstBegin; i < srcEnd; i++, j++) dst[j] = charAt(i);
    }

    /** Explicit mutable-array materialization boundary. */
    public char[] toCharArray() {
        char[] result = new char[length()];
        getChars(0, length(), result, 0);
        return result;
    }

    /** Explicit ordinary-String materialization boundary on a stock JVM. */
    public String asString() {
        return new String(toCharArray());
    }

    /** Explicit encoded-byte materialization boundary. */
    public byte[] getBytes(Charset charset) {
        return asString().getBytes(Objects.requireNonNull(charset, "charset"));
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof M3Text text
                && hash == text.hash
                && contentEquals(this, text);
    }

    @Override
    public int compareTo(M3Text other) {
        Objects.requireNonNull(other, "other");
        int common = Math.min(length(), other.length());
        for (int i = 0; i < common; i++) {
            int delta = charAt(i) - other.charAt(i);
            if (delta != 0) return delta;
        }
        return length() - other.length();
    }

    @Override
    public String toString() {
        return asString();
    }

    static int composeHash(int leftHash, int rightHash, int rightLength) {
        return leftHash * pow31(rightLength) + rightHash;
    }

    private static int pow31(int exponent) {
        int result = 1;
        int base = 31;
        int e = exponent;
        while (e != 0) {
            if ((e & 1) != 0) result *= base;
            e >>>= 1;
            if (e != 0) base *= base;
        }
        return result;
    }

    private static int hashOf(CharSequence value) {
        int result = 0;
        for (int i = 0; i < value.length(); i++) result = 31 * result + value.charAt(i);
        return result;
    }

    private static boolean contentEquals(CharSequence left, CharSequence right) {
        if (left.length() != right.length()) return false;
        return regionEquals(left, 0, right, 0, left.length());
    }

    private static boolean regionEquals(
            CharSequence left, int leftStart, CharSequence right, int rightStart, int count) {
        for (int i = 0; i < count; i++) {
            if (left.charAt(leftStart + i) != right.charAt(rightStart + i)) return false;
        }
        return true;
    }

    /** Weak content interner that never retains a caller String as a map key. */
    private static final class WeakInterner {
        private final Map<Integer, List<Entry>> buckets = new HashMap<>();
        private final ReferenceQueue<M3Text> queue = new ReferenceQueue<>();

        synchronized M3Text find(CharSequence value, int hash) {
            drainQueue();
            List<Entry> bucket = buckets.get(hash);
            if (bucket == null) return null;
            for (Iterator<Entry> iterator = bucket.iterator(); iterator.hasNext();) {
                M3Text candidate = iterator.next().get();
                if (candidate == null) {
                    iterator.remove();
                } else if (contentEquals(candidate, value)) {
                    return candidate;
                }
            }
            if (bucket.isEmpty()) buckets.remove(hash);
            return null;
        }

        synchronized M3Text intern(M3StringPiece piece, int hash) {
            M3Text existing = find(piece, hash);
            if (existing != null) return existing;
            M3Text created = new M3Text(piece, hash);
            publish(created);
            return created;
        }

        synchronized void publish(M3Text text) {
            drainQueue();
            buckets.computeIfAbsent(text.hash, ignored -> new ArrayList<>())
                    .add(new Entry(text, queue, text.hash));
        }

        private void drainQueue() {
            Entry stale;
            while ((stale = (Entry) queue.poll()) != null) {
                List<Entry> bucket = buckets.get(stale.hash);
                if (bucket != null) {
                    bucket.remove(stale);
                    if (bucket.isEmpty()) buckets.remove(stale.hash);
                }
            }
        }
    }

    private static final class Entry extends WeakReference<M3Text> {
        private final int hash;

        Entry(M3Text referent, ReferenceQueue<M3Text> queue, int hash) {
            super(referent, queue);
            this.hash = hash;
        }
    }
}
