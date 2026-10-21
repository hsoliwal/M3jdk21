// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexTokenStream.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Objects;

/** Zero-copy lexical result over one immutable source String plus primitive coordinate lanes. */
public final class M3TokenStream {
  private final String source;
  private final int[] starts;
  private final int[] ends;
  private final byte[] classes;

  M3TokenStream(String source, int[] starts, int[] ends, byte[] classes) {
    this.source = Objects.requireNonNull(source, "source");
    this.starts = Objects.requireNonNull(starts, "starts");
    this.ends = Objects.requireNonNull(ends, "ends");
    this.classes = Objects.requireNonNull(classes, "classes");
    if (starts.length != ends.length || starts.length != classes.length) {
      throw new IllegalArgumentException("token lane lengths differ");
    }
    int previousEnd = 0;
    for (int token = 0; token < starts.length; token++) {
      Objects.checkFromToIndex(starts[token], ends[token], source.length());
      if (starts[token] < previousEnd || starts[token] == ends[token]
          || Byte.toUnsignedInt(classes[token]) >= M3TokenClass.values().length) {
        throw new IllegalArgumentException("invalid token coordinate or class");
      }
      previousEnd = ends[token];
    }
  }

  public String source() { return source; }
  public int size() { return starts.length; }
  public int start(int tokenIndex) { return starts[Objects.checkIndex(tokenIndex, starts.length)]; }
  public int end(int tokenIndex) { return ends[Objects.checkIndex(tokenIndex, ends.length)]; }
  public int length(int tokenIndex) { return end(tokenIndex) - start(tokenIndex); }
  public M3TokenClass tokenClass(int tokenIndex) {
    return M3TokenClass.values()[Byte.toUnsignedInt(classes[Objects.checkIndex(tokenIndex, classes.length)])];
  }

  public char charAt(int tokenIndex, int charIndex) {
    int start = start(tokenIndex);
    return source.charAt(start + Objects.checkIndex(charIndex, end(tokenIndex) - start));
  }
  public String textAt(int tokenIndex) {
    return source.substring(start(tokenIndex), end(tokenIndex));
  }

  /** Java String-compatible hash over one token without materializing a String. */
  public int hashCodeAt(int tokenIndex) {
    int start = start(tokenIndex);
    int end = end(tokenIndex);
    int hash = 0;
    for (int index = start; index < end; index++) {
      hash = 31 * hash + source.charAt(index);
    }
    return hash;
  }

  /**
   * Case-folded Java-style hash used only as a literal candidate key.
   *
   * <p>Each UTF-16 code unit is normalized with the same upper-then-lower rule used by
   * {@link #matchesLiteral(int, CharSequence, boolean)}. Exact matching remains the proof.</p>
   */
  public int foldedHashCodeAt(int tokenIndex) {
    int start = start(tokenIndex);
    int end = end(tokenIndex);
    int hash = 0;
    for (int index = start; index < end; index++) {
      hash = 31 * hash + fold(source.charAt(index));
    }
    return hash;
  }

  public boolean matchesLiteral(int tokenIndex, CharSequence literal, boolean ignoreCase) {
    Objects.requireNonNull(literal, "literal");
    int start = start(tokenIndex), length = end(tokenIndex) - start;
    if (length != literal.length()) return false;
    for (int i = 0; i < length; i++) {
      char a = source.charAt(start + i), b = literal.charAt(i);
      if (a == b) continue;
      if (!ignoreCase || !equalIgnoreCase(a, b)) return false;
    }
    return true;
  }
  public long primitivePayloadBytes() {
    return Integer.BYTES * ((long) starts.length + ends.length) + classes.length;
  }
  private static boolean equalIgnoreCase(char a, char b) {
    char ua = Character.toUpperCase(a), ub = Character.toUpperCase(b);
    return ua == ub || Character.toLowerCase(ua) == Character.toLowerCase(ub);
  }

  static char fold(char value) {
    return Character.toLowerCase(Character.toUpperCase(value));
  }

  static int foldedHashCode(CharSequence value) {
    Objects.requireNonNull(value, "value");
    int hash = 0;
    for (int index = 0; index < value.length(); index++) {
      hash = 31 * hash + fold(value.charAt(index));
    }
    return hash;
  }

}
