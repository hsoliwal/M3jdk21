package com.synexia.mindex.experiment;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/** Immutable token tuple with exact Java UTF-16 content semantics and explicit boundaries. */
public final class MIndexString implements CharSequence, Comparable<MIndexString> {
    private static final MIndexPool LITERALS = new MIndexPool(TokenDictionary.of(java.util.List.of()), 65536, 65536);
    private final TokenSpace space;
    private final int[] ids;
    private final int[] ends;
    private final int hash;
    private volatile String sha256;
    private volatile String literalBoundary;

    private MIndexString(TokenSpace space, int[] ids) {
        this.space = Objects.requireNonNull(space, "space");
        this.ids = ids.clone();
        ends = new int[ids.length];
        int length = 0;
        int valueHash = 0;
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] < 0 || ids[i] >= space.size()) { throw new IndexOutOfBoundsException("token ID"); }
            String token = Objects.requireNonNull(space.token(ids[i]), "token");
            if (token.isEmpty()) { throw new IllegalArgumentException("empty token"); }
            length = Math.addExact(length, token.length());
            ends[i] = length;
            valueHash = StringHashes.append(valueHash, token.hashCode(), token.length());
        }
        hash = valueHash;
    }

    public static MIndexString of(TokenSpace space, int... ids) { return new MIndexString(space, ids); }
    /** Bounded compiler-literal pool; saturation falls back to an independent immutable value. */
    public static MIndexString literal(String text) {
        Objects.requireNonNull(text, "text");
        try {
            MIndexString value = LITERALS.intern(text);
            value.literalBoundary = text.intern();
            return value;
        }
        catch (MIndexPool.CapacityException full) {
            if (text.isEmpty()) {
                MIndexString value = of(TokenDictionary.of(java.util.List.of()));
                value.literalBoundary = text.intern(); return value;
            }
            TokenDictionary dictionary = TokenDictionary.of(java.util.List.of(text));
            MIndexString value = of(dictionary, dictionary.id(text));
            value.literalBoundary = text.intern(); return value;
        }
    }
    public TokenSpace space() { return space; }
    public int[] tokenIds() { return ids.clone(); }
    public int tokenCount() { return ids.length; }
    public int tokenIdAt(int index) { return ids[index]; }
    public int length() { return ends.length == 0 ? 0 : ends[ends.length - 1]; }
    public boolean isEmpty() { return ids.length == 0; }

    public char charAt(int index) {
        if (literalBoundary != null) { return literalBoundary.charAt(index); }
        if (index < 0 || index >= length()) { throw new StringIndexOutOfBoundsException(index); }
        int found = Arrays.binarySearch(ends, index + 1);
        int token = found >= 0 ? found : -found - 1;
        return space.token(ids[token]).charAt(index - (token == 0 ? 0 : ends[token - 1]));
    }
    public String subSequence(int start, int end) { return substring(start, end); }
    public String substring(int start) { return toString().substring(start); }
    public String substring(int start, int end) { return toString().substring(start, end); }
    public boolean startsWith(String prefix) { return toString().startsWith(prefix); }
    public boolean endsWith(String suffix) { return toString().endsWith(suffix); }
    public boolean contains(CharSequence value) { return toString().contains(value); }
    public int indexOf(String value) { return toString().indexOf(value); }
    public int indexOf(int value) { return toString().indexOf(value); }
    public int compareToString(String value) { return compareContent(Objects.requireNonNull(value, "value")); }
    public boolean equalsString(Object value) { return value instanceof String s && contentEquals(s); }

    public boolean contentEquals(CharSequence value) {
        if (value == null || length() != value.length()) { return false; }
        for (int i = 0; i < length(); i++) { if (charAt(i) != value.charAt(i)) { return false; } }
        return true;
    }
    public MIndexString concat(MIndexString other) {
        Objects.requireNonNull(other, "other");
        if (space != other.space) { throw new IllegalArgumentException("different token-space views"); }
        int[] joined = Arrays.copyOf(ids, Math.addExact(ids.length, other.ids.length));
        System.arraycopy(other.ids, 0, joined, ids.length, other.ids.length);
        return of(space, joined);
    }
    public int compareTo(MIndexString other) { return compareContent(Objects.requireNonNull(other, "other")); }
    private int compareContent(CharSequence other) {
        int count = Math.min(length(), other.length());
        for (int i = 0; i < count; i++) {
            int delta = charAt(i) - other.charAt(i);
            if (delta != 0) { return delta; }
        }
        return length() - other.length();
    }
    public boolean equals(Object other) {
        return this == other || other instanceof MIndexString s && hash == s.hash && contentEquals(s);
    }
    public int hashCode() { return hash; }
    public String sha256Utf8() {
        String result = sha256;
        if (result == null) { result = Digests.sha256(toString().getBytes(StandardCharsets.UTF_8)); sha256 = result; }
        return result;
    }
    public String toString() {
        if (literalBoundary != null) { return literalBoundary; }
        StringBuilder result = new StringBuilder(length());
        for (int id : ids) { result.append(space.token(id)); }
        return result.toString();
    }
}
