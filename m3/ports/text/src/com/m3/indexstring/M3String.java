/* Copyright 2026 Hitesh Soliwal; SPDX-License-Identifier: Apache-2.0 */
package com.m3.indexstring;

import com.synexia.indexstring.FrozenChars;
import com.synexia.indexstring.MIndexJoinedChars;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/** Value facade over the established Frozen/Joined owner; ordinary String conversion allocates. */
public final class M3String implements CharSequence, Comparable<M3String> {
    private final FrozenChars[] atoms;
    private final MIndexJoinedChars text;
    private final int hash;
    private static final class Local {
        static final M3StringArena ARENA = new M3StringArena(4096, 8L * 1024 * 1024);
    }
    M3String(FrozenChars[] parts) {
        this(parts, true);
    }
    private M3String(FrozenChars[] parts, boolean canonicalize) {
        int count = 0, length = 0;
        for (FrozenChars part : parts) {
            Objects.requireNonNull(part);
            length = Math.addExact(length, part.length());
            if (part.length() != 0) count++;
        }
        FrozenChars[] normalized = new FrozenChars[count];
        int next = 0, value = 0;
        for (FrozenChars part : parts) if (part.length() != 0) {
            if (next > 0 && normalized[next - 1].adjacentTo(part))
                normalized[next - 1] = normalized[next - 1].coalesce(part);
            else normalized[next++] = part;
            value = value * power31(part.length()) + part.hash32();
        }
        hash = value;
        FrozenChars[] ranges = next == count ? normalized : Arrays.copyOf(normalized, next);
        text = canonicalize ? MIndexJoinedChars.of(ranges) : MIndexJoinedChars.ofUncached(ranges);
        // Content canonicalization may select an older owner; adopt that sole descriptor authority.
        atoms = text.frozenSegments();
    }
    private static int power31(int exponent) {
        int result = 1, power = 31;
        while (exponent != 0) {
            if ((exponent & 1) != 0) result *= power;
            power *= power; exponent >>>= 1;
        }
        return result;
    }
    public static M3String fromString(String text) { return Local.ARENA.fromString(text); }
    /** Compiler opt-in boundary: both String operands evaluate before any atom admission. */
    public static M3String fromConcatOperands(String left, String right) {
        return fromString(String.valueOf(left)).concat(fromString(String.valueOf(right)));
    }
    /** Defensive mutable-array admission. No ordinary array ownership is transferred. */
    public static M3String fromChars(char[] units) { return new M3String(new FrozenChars[]{FrozenChars.copyOf(units)}); }
    @Override public int length() { return text.length(); }
    public boolean isEmpty() { return length() == 0; }
    @Override public char charAt(int index) { return text.charAt(index); }
    public M3String concat(M3String suffix) {
        Objects.requireNonNull(suffix);
        Math.addExact(length(), suffix.length());
        if (suffix.isEmpty()) return this;
        if (isEmpty()) return suffix;
        FrozenChars[] parts = Arrays.copyOf(atoms, Math.addExact(atoms.length, suffix.atoms.length));
        System.arraycopy(suffix.atoms, 0, parts, atoms.length, suffix.atoms.length);
        return new M3String(parts);
    }
    public M3String substring(int start) { return substring(start, length()); }
    public M3String substring(int start, int end) {
        Objects.checkFromToIndex(start, end, length());
        if (start == 0 && end == length()) return this;
        FrozenChars[] parts = new FrozenChars[atoms.length];
        int origin = 0, count = 0;
        for (FrozenChars atom : atoms) {
            int a = Math.max(start, origin), b = Math.min(end, origin + atom.length());
            if (a < b) parts[count++] = atom.subSequence(a - origin, b - origin);
            origin += atom.length();
        }
        return new M3String(Arrays.copyOf(parts, count));
    }
    @Override public M3String subSequence(int start, int end) { return substring(start, end); }
    /** Process-local segment-tuple reuse, independent of text equality and lexical IDs. */
    public boolean sharesBackingWith(M3String other) { return text.sharesBackingWith(Objects.requireNonNull(other).text); }
    public int segmentCount() { return atoms.length; }
    /** Deduplicated retained owner payload; excludes lookup caches and descriptor overhead. */
    public long retainedPayloadBytes() {
        long total = 0;
        for (int i = 0; i < atoms.length; i++) {
            boolean seen = false;
            for (int j = 0; j < i; j++) if (atoms[i].sharesStorageWith(atoms[j])) { seen = true; break; }
            if (!seen) total = Math.addExact(total, atoms[i].retainedBytes());
        }
        return total;
    }
    private M3Utf16Cursor cursor() { return new M3Utf16Cursor(text.asReadOnlyBuffers()); }
    @Override public int hashCode() { return hash; }
    @Override public boolean equals(Object other) {
        return other == this || other instanceof M3String that && hash == that.hash && contentEquals(that);
    }
    public boolean contentEquals(CharSequence other) {
        Objects.requireNonNull(other);
        if (length() != other.length()) return false;
        M3Utf16Cursor left = cursor();
        if (other instanceof M3String that) {
            M3Utf16Cursor right = that.cursor();
            while (left.hasNext()) if (left.next() != right.next()) return false;
        } else for (int i = 0; left.hasNext(); i++) if (left.next() != other.charAt(i)) return false;
        return true;
    }
    @Override public int compareTo(M3String other) {
        Objects.requireNonNull(other);
        M3Utf16Cursor left = cursor(), right = other.cursor();
        while (left.hasNext() && right.hasNext()) {
            int difference = left.next() - right.next(); if (difference != 0) return difference;
        }
        return length() - other.length();
    }
    public int indexOf(String pattern) { return indexOf(pattern, 0); }
    public int indexOf(String pattern, int fromIndex) { return search(pattern, fromIndex, false); }
    public int lastIndexOf(String pattern) { return lastIndexOf(pattern, length()); }
    public int lastIndexOf(String pattern, int fromIndex) { return search(pattern, fromIndex, true); }
    private int search(String pattern, int fromIndex, boolean last) {
        Objects.requireNonNull(pattern);
        int size = pattern.length();
        if (last && fromIndex < 0) return -1;
        if (size == 0) return Math.min(length(), Math.max(0, fromIndex));
        int start = last ? 0 : Math.max(0, fromIndex);
        int limit = last ? Math.min(fromIndex, length() - size) : length() - size;
        if (start > limit) return -1;
        // Per-call KMP facts belong to the pattern; no approximate hash proves a match.
        int[] prefix = new int[size];
        for (int i = 1, matched = 0; i < size; i++) {
            while (matched > 0 && pattern.charAt(i) != pattern.charAt(matched)) matched = prefix[matched - 1];
            if (pattern.charAt(i) == pattern.charAt(matched)) matched++;
            prefix[i] = matched;
        }
        M3Utf16Cursor units = new M3Utf16Cursor(text.subSequence(start, last ? limit + size : length()).asReadOnlyBuffers());
        int found = -1, position = start, matched = 0;
        while (units.hasNext()) {
            char unit = units.next();
            while (matched > 0 && unit != pattern.charAt(matched)) matched = prefix[matched - 1];
            if (unit == pattern.charAt(matched)) matched++;
            if (matched == size) {
                found = position - size + 1;
                if (!last) return found;
                matched = prefix[matched - 1];
            }
            position++;
        }
        return found;
    }
    public boolean contains(CharSequence pattern) { return indexOf(Objects.requireNonNull(pattern).toString()) >= 0; }
    public int codePointAt(int index) { return text.codePointAt(index); }
    public int codePointCount(int start, int end) {
        Objects.checkFromToIndex(start, end, length());
        return (int)substring(start, end).codePoints().count();
    }
    @Override public IntStream chars() { return stream(false); }
    @Override public IntStream codePoints() { return stream(true); }
    private IntStream stream(boolean points) {
        M3Utf16Cursor cursor = cursor();
        return StreamSupport.intStream(new Spliterators.AbstractIntSpliterator(length(),
                Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL) {
            @Override public boolean tryAdvance(IntConsumer action) {
                Objects.requireNonNull(action);
                if (!cursor.hasNext()) return false;
                action.accept(points ? cursor.nextCodePoint() : cursor.next()); return true;
            }
        }, false);
    }
    public char[] toCharArray() { return text.copy(); }
    public void getChars(int start, int end, char[] destination, int offset) {
        Objects.checkFromToIndex(start, end, length());
        Objects.checkFromIndexSize(offset, end - start, Objects.requireNonNull(destination).length);
        for (CharBuffer segment : text.subSequence(start, end).asReadOnlyBuffers()) {
            int count = segment.remaining(); segment.get(destination, offset, count); offset += count;
        }
    }
    public byte[] getBytes(Charset charset) { return text.copyEncoded(charset); }
    public Matcher matcher(Pattern pattern) { return Objects.requireNonNull(pattern).matcher(this); }
    public boolean matches(String expression) { return matcher(Pattern.compile(expression)).matches(); }
    public Reader asReader() { return text.asReader(); }
    public void writeTo(Writer destination) throws IOException {
        Objects.requireNonNull(destination);
        try (Reader reader = asReader()) { reader.transferTo(destination); }
    }
    /** Deliberate owner detachment: copies payload, separate from retained slicing. */
    public M3String compact() { return new M3String(new FrozenChars[]{text.detach()}, false); }
    @Override public String toString() { return text.toString(); }
}
