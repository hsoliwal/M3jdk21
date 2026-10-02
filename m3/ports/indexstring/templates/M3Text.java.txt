// SPDX-License-Identifier: Apache-2.0
package com.m3.text.compat;

import com.synexia.indexstring.FrozenChars;
import com.synexia.indexstring.MIndexJoinedChars;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * Immutable M3-named facade over the existing IndexString descriptor owner.
 * This class owns no character/byte payload, dictionary, interner, or native arena.
 * It is an explicit stock-JVM view, not java.lang.String or String.intern().
 * Equal M3Text values have String-compatible UTF-16 hashes; equality with String
 * is intentionally false in both directions. Use contentEquals at that boundary.
 */
public final class M3Text implements CharSequence, Comparable<M3Text> {
  /** Fixed 256 KiB per-call prefix budget; not a retained search index/cache. */
  private static final int MAX_PREFIX_UNITS = 65536;
  private final MIndexJoinedChars storage;
  private int cachedHash;
  private volatile boolean hashReady;

  private M3Text(MIndexJoinedChars storage) {
    this.storage = Objects.requireNonNull(storage, "storage");
  }

  /** Retains the existing immutable owner without admission or copying. */
  public static M3Text fromJoined(MIndexJoinedChars storage) {
    return new M3Text(storage);
  }

  /** Each atom is already immutable; canonical admission remains its owner's job. */
  public static M3Text fromFrozen(FrozenChars... atoms) {
    return fromJoined(MIndexJoinedChars.of(atoms));
  }

  /**
   * Calls an explicitly supplied existing admission policy exactly once and
   * validates exact UTF-16 content. This check does not prove canonicality,
   * namespace identity, or a shared/local lookup policy. There is deliberately
   * no second default interner and no automatic persistent publication.
   */
  public static M3Text fromString(String text,
      Function<String, MIndexJoinedChars> admission) {
    Objects.requireNonNull(text, "text");
    M3Text admitted = fromJoined(Objects.requireNonNull(admission, "admission").apply(text));
    if (!admitted.contentEquals(text)) {
      throw new IllegalArgumentException("admission changed UTF-16 content");
    }
    return admitted;
  }

  /** Original storage owner; its body ID is not a portable text or slice identity. */
  public MIndexJoinedChars storage() { return storage; }
  @Override public int length() { return storage.length(); }
  public boolean isEmpty() { return length() == 0; }
  @Override public char charAt(int index) { return storage.charAt(index); }

  @Override public M3Text subSequence(int start, int end) {
    MIndexJoinedChars result = storage.subSequence(start, end);
    return result == storage ? this : fromJoined(result);
  }
  public M3Text substring(int start, int end) { return subSequence(start, end); }
  public M3Text substring(int start) { return subSequence(start, length()); }

  /** Retains payloads; flat descriptor assembly is not constant-time concatenation. */
  public M3Text concat(M3Text other) {
    Objects.requireNonNull(other, "other");
    if (other.isEmpty()) return this;
    if (isEmpty()) return other;
    return fromJoined(storage.concat(other.storage));
  }

  /** Direct traversal into independent mutable output; no flattened cache. */
  public char[] toCharArray() { return storage.copy(); }
  public void getChars(int start, int end, char[] target, int targetStart) {
    Objects.checkFromToIndex(start, end, length());
    storage.copyTo(start, target, targetStart, end - start);
  }
  public Reader asReader() { return storage.asReader(); }

  /** Ordinary String conversion is a deliberate materialization boundary. */
  public String asString() { return storage.toString(); }
  @Override public String toString() { return asString(); }

  public boolean contentEquals(CharSequence other) {
    Objects.requireNonNull(other, "other");
    if (other.length() != length()) return false;
    if (other instanceof M3Text text) return compareTo(text) == 0;
    Cursor cursor = cursor();
    for (int index = 0; cursor.hasNext(); index++) {
      if (cursor.nextChar() != other.charAt(index)) return false;
    }
    return true;
  }

  @Override public boolean equals(Object other) {
    return this == other || other instanceof M3Text text
        && length() == text.length() && compareTo(text) == 0;
  }

  @Override public int hashCode() {
    if (hashReady) return cachedHash;
    int hash = 0;
    Cursor cursor = cursor();
    while (cursor.hasNext()) hash = 31 * hash + cursor.nextChar();
    cachedHash = hash;
    hashReady = true;
    return hash;
  }

  @Override public int compareTo(M3Text other) {
    Objects.requireNonNull(other, "other");
    Cursor left = cursor(), right = other.cursor();
    while (left.hasNext() && right.hasNext()) {
      int difference = left.nextChar() - right.nextChar();
      if (difference != 0) return difference;
    }
    return length() - other.length();
  }

  public int codePointAt(int index) { return storage.codePointAt(index); }
  public int codePointBefore(int index) {
    if (index <= 0 || index > length()) throw new IndexOutOfBoundsException(index);
    return Character.codePointBefore(this, index);
  }
  public int codePointCount(int start, int end) {
    Objects.checkFromToIndex(start, end, length());
    Cursor cursor = subSequence(start, end).cursor();
    int count = 0;
    while (cursor.hasNext()) { cursor.nextCodePoint(); count++; }
    return count;
  }

  /** Independent linear segment cursor; no tree lookup per emitted code unit. */
  @Override public IntStream chars() { return stream(false); }
  /** Reconstructs surrogate pairs even when the pair crosses two segments. */
  @Override public IntStream codePoints() { return stream(true); }
  private IntStream stream(boolean codePoints) {
    Cursor cursor = cursor();
    PrimitiveIterator.OfInt iterator = new PrimitiveIterator.OfInt() {
      @Override public boolean hasNext() { return cursor.hasNext(); }
      @Override public int nextInt() {
        return codePoints ? cursor.nextCodePoint() : cursor.nextChar();
      }
    };
    int flags = Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL;
    Spliterator.OfInt spliterator = codePoints
        ? Spliterators.spliteratorUnknownSize(iterator, flags)
        : Spliterators.spliterator(iterator, length(), flags);
    return StreamSupport.intStream(spliterator, false);
  }

  public int indexOf(String pattern) { return indexOf(pattern, 0); }
  /** Exact UTF-16 search; bounded KMP, with a slower no-prefix fallback for large patterns. */
  public int indexOf(String pattern, int fromIndex) {
    Objects.requireNonNull(pattern, "pattern");
    int start = Math.max(0, fromIndex);
    if (pattern.isEmpty()) return Math.min(start, length());
    if (start >= length() || pattern.length() > length() - start) return -1;
    return search(pattern, start, length(), false);
  }
  public int lastIndexOf(String pattern) { return lastIndexOf(pattern, length()); }
  public int lastIndexOf(String pattern, int fromIndex) {
    Objects.requireNonNull(pattern, "pattern");
    if (fromIndex < 0) return -1;
    if (pattern.isEmpty()) return Math.min(fromIndex, length());
    int lastStart = Math.min(fromIndex, length() - pattern.length());
    if (lastStart < 0) return -1;
    return search(pattern, 0, lastStart + pattern.length(), true);
  }
  public boolean contains(String pattern) { return indexOf(pattern) >= 0; }
  public boolean startsWith(String prefix) {
    Objects.requireNonNull(prefix, "prefix");
    return prefix.length() <= length() && subSequence(0, prefix.length()).contentEquals(prefix);
  }
  public boolean endsWith(String suffix) {
    Objects.requireNonNull(suffix, "suffix");
    return suffix.length() <= length()
        && subSequence(length() - suffix.length(), length()).contentEquals(suffix);
  }

  private int search(String pattern, int start, int end, boolean last) {
    if (pattern.length() > MAX_PREFIX_UNITS) return boundedFallback(pattern, start, end, last);
    int[] prefix = new int[pattern.length()];
    for (int i = 1, matched = 0; i < pattern.length(); i++) {
      while (matched > 0 && pattern.charAt(i) != pattern.charAt(matched)) {
        matched = prefix[matched - 1];
      }
      if (pattern.charAt(i) == pattern.charAt(matched)) matched++;
      prefix[i] = matched;
    }
    Cursor cursor = subSequence(start, end).cursor();
    int position = start, matched = 0, answer = -1;
    while (cursor.hasNext()) {
      char unit = cursor.nextChar();
      while (matched > 0 && unit != pattern.charAt(matched)) matched = prefix[matched - 1];
      if (unit == pattern.charAt(matched)) matched++;
      if (matched == pattern.length()) {
        answer = position - pattern.length() + 1;
        if (!last) return answer;
        matched = prefix[matched - 1];
      }
      position++;
    }
    return answer;
  }

  /** No prefix allocation for oversized patterns; worst-case O(n*m*log(s)) fallback. */
  private int boundedFallback(String pattern, int start, int end, boolean last) {
    int answer = -1;
    for (int at = start; at <= end - pattern.length(); at++) {
      int offset = 0;
      while (offset < pattern.length() && charAt(at + offset) == pattern.charAt(offset)) offset++;
      if (offset == pattern.length()) {
        if (!last) return at;
        answer = at;
      }
    }
    return answer;
  }

  /** Uses java.util.regex itself; no substitute regex engine or approximate acceptance. */
  public Matcher matcher(Pattern pattern) {
    return Objects.requireNonNull(pattern, "pattern").matcher(this);
  }
  public boolean matches(String regex) { return Pattern.matches(regex, this); }
  public String replaceAll(String regex, String replacement) {
    return Pattern.compile(regex).matcher(this).replaceAll(replacement);
  }
  public String[] split(String regex, int limit) { return Pattern.compile(regex).split(this, limit); }

  /** One whole-input encoder, explicit policies, independent read-only output, no cache. */
  public ByteBuffer encode(Charset charset, CodingErrorAction malformed,
      CodingErrorAction unmappable) throws CharacterCodingException {
    return Objects.requireNonNull(charset, "charset").newEncoder()
        .onMalformedInput(Objects.requireNonNull(malformed, "malformed"))
        .onUnmappableCharacter(Objects.requireNonNull(unmappable, "unmappable"))
        .encode(CharBuffer.wrap(this)).asReadOnlyBuffer();
  }

  /** Writes directly to the requested output without making an intermediate String. */
  public void writeTo(Appendable target) throws IOException {
    Objects.requireNonNull(target, "target");
    Cursor cursor = cursor();
    while (cursor.hasNext()) target.append(cursor.nextChar());
  }

  private Cursor cursor() { return new Cursor(storage.asReadOnlyBuffers()); }
  private static final class Cursor {
    private final CharBuffer[] parts;
    private int index;
    Cursor(CharBuffer[] parts) { this.parts = parts; }
    boolean hasNext() {
      while (index < parts.length && !parts[index].hasRemaining()) index++;
      return index < parts.length;
    }
    char nextChar() {
      if (!hasNext()) throw new NoSuchElementException();
      return parts[index].get();
    }
    int nextCodePoint() {
      char first = nextChar();
      if (Character.isHighSurrogate(first) && hasNext()) {
        char second = parts[index].get(parts[index].position());
        if (Character.isLowSurrogate(second)) {
          nextChar();
          return Character.toCodePoint(first, second);
        }
      }
      return first;
    }
  }
}
