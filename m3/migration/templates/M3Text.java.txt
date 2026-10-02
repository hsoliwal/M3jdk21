// SPDX-License-Identifier: Apache-2.0
package com.m3.indexstring;

import com.synexia.indexstring.FrozenBytes;
import com.synexia.indexstring.FrozenChars;
import com.synexia.indexstring.MIndexJoinedChars;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Explicit stock-JVM value view over the ported Synexia owner; not java.lang.String.
 * No payload is stored here. Equality is UTF-16 value equality between M3Text values;
 * contentEquals is the explicit cross-type operation. String conversion materializes.
 * The directory is flat. Joining walks segments; partial-atom ranges can additionally
 * scan code units for hashes. Coalescing and canonical-bucket lookup add work.
 */
public final class M3Text implements CharSequence, Comparable<M3Text> {
  private static final M3Text EMPTY = new M3Text(MIndexJoinedChars.ofRetained());
  private final MIndexJoinedChars value;
  private int cachedHash;
  private volatile boolean hashComputed;

  private M3Text(MIndexJoinedChars value) { this.value = value; }

  /** Local scalar admission; source interning may discard a temporary copied candidate. */
  public static M3Text fromString(String text) {
    Objects.requireNonNull(text, "text");
    return text.isEmpty() ? EMPTY : new M3Text(MIndexJoinedChars.of(FrozenChars.fromString(text)));
  }

  /** Retain the supplied immutable owners. Equal text is not permission to replace an owner. */
  public static M3Text of(FrozenChars... parts) {
    MIndexJoinedChars joined = MIndexJoinedChars.ofRetained(parts);
    return joined.length() == 0 ? EMPTY : new M3Text(joined);
  }

  /** Zero-payload-copy projection of an existing legacy value, preserving its selected backing. */
  public static M3Text fromLegacy(MIndexJoinedChars value) {
    Objects.requireNonNull(value, "value");
    return value.length() == 0 ? EMPTY : new M3Text(value);
  }

  public MIndexJoinedChars asLegacyView() { return value; }
  @Override public int length() { return value.length(); }
  @Override public char charAt(int index) { return value.charAt(index); }
  @Override public boolean isEmpty() { return length() == 0; }
  @Override public M3Text subSequence(int start, int end) { return substring(start, end); }
  public M3Text substring(int start) { return substring(start, length()); }
  public M3Text substring(int start, int end) {
    Objects.checkFromToIndex(start, end, length());
    if (start == 0 && end == length()) return this;
    return start == end ? EMPTY : new M3Text(value.subSequence(start, end));
  }

  public M3Text concat(M3Text other) {
    Objects.requireNonNull(other, "other");
    if (other.isEmpty()) return this;
    if (isEmpty()) return other;
    checkedSize((long) length() + other.length());
    FrozenChars[] left = value.retainedParts(), right = other.value.retainedParts();
    FrozenChars[] parts = new FrozenChars[checkedSize((long) left.length + right.length)];
    System.arraycopy(left, 0, parts, 0, left.length);
    System.arraycopy(right, 0, parts, left.length, right.length);
    return of(parts);
  }

  public M3Text repeat(int count) {
    if (count < 0) throw new IllegalArgumentException("negative repeat count: " + count);
    if (count == 0 || isEmpty()) return EMPTY;
    if (count == 1) return this;
    checkedSize((long) length() * count);
    FrozenChars[] atomRanges = value.retainedParts();
    FrozenChars[] parts = new FrozenChars[checkedSize((long) atomRanges.length * count)];
    for (int i = 0; i < count; i++) System.arraycopy(atomRanges, 0, parts, i * atomRanges.length, atomRanges.length);
    return of(parts);
  }

  private static int checkedSize(long value) {
    if (value > Integer.MAX_VALUE) throw new OutOfMemoryError("M3 logical length or descriptor count exceeds int range");
    return (int) value;
  }

  @Override public int hashCode() {
    if (!hashComputed) { cachedHash = value.stringHash(); hashComputed = true; }
    return cachedHash;
  }
  @Override public boolean equals(Object other) {
    if (this == other) return true;
    return other instanceof M3Text text && length() == text.length()
        && hashCode() == text.hashCode() && contentEquals(text);
  }
  public boolean contentEquals(CharSequence other) {
    Objects.requireNonNull(other, "other");
    if (other.length() != length()) return false;
    Cursor left = cursor();
    if (other instanceof M3Text text) {
      Cursor right = text.cursor();
      for (int i = 0; i < length(); i++) if (left.next() != right.next()) return false;
    } else {
      for (int i = 0; i < length(); i++) if (left.next() != other.charAt(i)) return false;
    }
    return true;
  }
  @Override public int compareTo(M3Text other) {
    Objects.requireNonNull(other, "other");
    Cursor left = cursor(), right = other.cursor();
    int common = Math.min(length(), other.length());
    for (int i = 0; i < common; i++) { int difference = left.next() - right.next(); if (difference != 0) return difference; }
    return length() - other.length();
  }
  public boolean startsWith(String prefix) { return startsWith(prefix, 0); }
  public boolean startsWith(String prefix, int offset) {
    Objects.requireNonNull(prefix, "prefix");
    if (offset < 0 || offset > length() - prefix.length()) return false;
    Cursor cursor = cursor(); cursor.skip(offset);
    for (int i = 0; i < prefix.length(); i++) if (cursor.next() != prefix.charAt(i)) return false;
    return true;
  }
  public boolean endsWith(String suffix) {
    Objects.requireNonNull(suffix, "suffix"); return startsWith(suffix, length() - suffix.length());
  }
  public boolean contains(CharSequence needle) { return indexOf(Objects.requireNonNull(needle, "needle").toString()) >= 0; }
  public int indexOf(String needle) { return indexOf(needle, 0); }
  public int indexOf(String needle, int fromIndex) { return search(needle, fromIndex, false); }
  public int lastIndexOf(String needle) { return lastIndexOf(needle, length()); }
  public int lastIndexOf(String needle, int fromIndex) { return search(needle, fromIndex, true); }

  /**
   * Non-indexed exact fallback, O(n*m + segments) worst-case time and O(segments)
   * cursor metadata. No new KMP/trie owner or precomputation cache is introduced.
   * UTF-16 unit matching intentionally finds an individual surrogate inside a pair.
   */
  private int search(String needle, int fromIndex, boolean last) {
    Objects.requireNonNull(needle, "needle");
    if (last && fromIndex < 0) return -1;
    int start = last ? 0 : Math.max(0, fromIndex);
    if (needle.isEmpty()) return Math.min(last ? fromIndex : start, length());
    int finalCandidate = length() - needle.length();
    if (last) finalCandidate = Math.min(finalCandidate, fromIndex);
    if (start > finalCandidate) return -1;
    Cursor cursor = cursor(); cursor.skip(start);
    int found = -1;
    for (int candidate = start; candidate <= finalCandidate; candidate++) {
      int segment = cursor.segment, offset = cursor.offset;
      int matched = 0;
      while (matched < needle.length() && cursor.next() == needle.charAt(matched)) matched++;
      if (matched == needle.length()) { if (!last) return candidate; found = candidate; }
      cursor.segment = segment; cursor.offset = offset; cursor.next();
    }
    return found;
  }

  public Matcher matcher(Pattern pattern) { return Objects.requireNonNull(pattern, "pattern").matcher(this); }
  public boolean matches(String regex) { return Pattern.matches(regex, this); }
  public char[] toCharArray() { return value.copy(); }
  public void getChars(int sourceStart, int sourceEnd, char[] target, int targetStart) {
    Objects.checkFromToIndex(sourceStart, sourceEnd, length());
    value.copyTo(sourceStart, target, targetStart, sourceEnd - sourceStart);
  }
  public FrozenBytes encode(Charset charset) { return value.encode(charset); }
  public Reader asReader() { return value.asReader(); }
  /** Explicit contiguous stock-String boundary; no flattened value is cached. */
  @Override public String toString() { return value.toString(); }

  private Cursor cursor() { return new Cursor(value.asReadOnlyBuffers()); }
  private static final class Cursor {
    private final CharBuffer[] parts;
    private int segment;
    private int offset;
    Cursor(CharBuffer[] parts) { this.parts = parts; }
    char next() {
      char result = parts[segment].get(offset++);
      if (offset == parts[segment].limit()) { segment++; offset = 0; }
      return result;
    }
    void skip(int count) {
      while (count > 0) {
        int take = Math.min(count, parts[segment].limit() - offset);
        offset += take; count -= take;
        if (offset == parts[segment].limit()) { segment++; offset = 0; }
      }
    }
  }
}
