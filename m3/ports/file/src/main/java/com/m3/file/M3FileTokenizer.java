// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import com.m3.file.M3FileResolver;
import java.util.Objects;

/** Lossless allocation-light tokenizers that admit slices directly into M3FileText. */
public final class M3FileTokenizer {
  private M3FileTokenizer() {}

  public static M3File plainText(
      M3FileResolver resolver, int languageId, CharSequence source) {
    return tokenize(resolver, languageId, source, false, false);
  }

  /** Tokenizes in memory while deriving optional rolling lanes during the same admission pass. */
  public static M3File plainText(
      M3FileResolver resolver,
      int languageId,
      CharSequence source,
      boolean precomputeRollingIndex) {
    return tokenize(resolver, languageId, source, false, precomputeRollingIndex);
  }

  public static M3File codeLike(
      M3FileResolver resolver, int languageId, CharSequence source) {
    return tokenize(resolver, languageId, source, true, false);
  }

  /** Tokenizes code-like text while deriving optional rolling lanes during the same pass. */
  public static M3File codeLike(
      M3FileResolver resolver,
      int languageId,
      CharSequence source,
      boolean precomputeRollingIndex) {
    return tokenize(resolver, languageId, source, true, precomputeRollingIndex);
  }

  private static M3File tokenize(
      M3FileResolver resolver,
      int languageId,
      CharSequence source,
      boolean codeLike,
      boolean precomputeRollingIndex) {
    Objects.requireNonNull(resolver, "resolver");
    Objects.requireNonNull(source, "source");
    M3FileBuilder builder = new M3FileBuilder(resolver);
    if (precomputeRollingIndex) builder.enableRollingIndex();
    int length = source.length();
    int at = 0;
    while (at < length) {
      int end;
      M3FileTokenKind kind;
      char unit = source.charAt(at);
      int codePoint = Character.codePointAt(source, at);

      if (unit == '\r' || unit == '\n') {
        end =
            unit == '\r' && at + 1 < length && source.charAt(at + 1) == '\n'
                ? at + 2
                : at + 1;
        kind = M3FileTokenKind.NEWLINE;
      } else if (Character.isWhitespace(codePoint)) {
        end = at + Character.charCount(codePoint);
        while (end < length) {
          char next = source.charAt(end);
          int nextCodePoint = Character.codePointAt(source, end);
          if (next == '\r' || next == '\n' || !Character.isWhitespace(nextCodePoint)) break;
          end += Character.charCount(nextCodePoint);
        }
        kind = M3FileTokenKind.WHITESPACE;
      } else if (codeLike
          && unit == '/'
          && at + 1 < length
          && source.charAt(at + 1) == '/') {
        end = at + 2;
        while (end < length && source.charAt(end) != '\r' && source.charAt(end) != '\n') end++;
        kind = M3FileTokenKind.COMMENT;
      } else if (codeLike
          && unit == '/'
          && at + 1 < length
          && source.charAt(at + 1) == '*') {
        end = blockCommentEnd(source, at + 2);
        kind = M3FileTokenKind.COMMENT;
      } else if (codeLike && (unit == '\'' || unit == '"' || unit == '`')) {
        end = quotedEnd(source, at, unit);
        kind = M3FileTokenKind.QUOTED;
      } else if (isWordStart(codePoint)) {
        end = wordEnd(source, at);
        kind = M3FileTokenKind.WORD;
      } else if (Character.isDigit(codePoint)) {
        end = numberEnd(source, at + Character.charCount(codePoint));
        kind = M3FileTokenKind.NUMBER;
      } else {
        end = at + Character.charCount(codePoint);
        kind = M3FileTokenKind.SYMBOL;
      }

      builder.add(languageId, new Slice(source, at, end), kind);
      at = end;
    }
    return builder.build();
  }

  private static int blockCommentEnd(CharSequence source, int from) {
    int length = source.length();
    int at = from;
    while (at + 1 < length) {
      if (source.charAt(at) == '*' && source.charAt(at + 1) == '/') return at + 2;
      at++;
    }
    return length;
  }

  private static int quotedEnd(CharSequence source, int start, char quote) {
    int length = source.length();
    boolean escaped = false;
    for (int at = start + 1; at < length; at++) {
      char unit = source.charAt(at);
      if (escaped) {
        escaped = false;
      } else if (unit == '\\') {
        escaped = true;
      } else if (unit == quote) {
        return at + 1;
      }
    }
    return length;
  }

  private static int wordEnd(CharSequence source, int from) {
    int length = source.length();
    int at = from;
    while (at < length) {
      int codePoint = Character.codePointAt(source, at);
      if (!isWordPart(codePoint)) break;
      at += Character.charCount(codePoint);
    }
    return at;
  }

  private static int numberEnd(CharSequence source, int from) {
    int length = source.length();
    int at = from;
    while (at < length) {
      int codePoint = Character.codePointAt(source, at);
      if (!(Character.isLetterOrDigit(codePoint)
          || codePoint == '_'
          || codePoint == '.')) {
        break;
      }
      at += Character.charCount(codePoint);
    }
    return at;
  }

  private static boolean isWordStart(int codePoint) {
    return Character.isLetter(codePoint) || codePoint == '_' || codePoint == '$';
  }

  private static boolean isWordPart(int codePoint) {
    int type = Character.getType(codePoint);
    return Character.isLetterOrDigit(codePoint)
        || codePoint == '_'
        || codePoint == '$'
        || type == Character.NON_SPACING_MARK
        || type == Character.COMBINING_SPACING_MARK;
  }

  private record Slice(CharSequence source, int start, int end) implements CharSequence {
    Slice {
      Objects.requireNonNull(source, "source");
      Objects.checkFromToIndex(start, end, source.length());
    }

    @Override
    public int length() {
      return end - start;
    }

    @Override
    public char charAt(int index) {
      return source.charAt(start + Objects.checkIndex(index, length()));
    }

    @Override
    public CharSequence subSequence(int from, int to) {
      Objects.checkFromToIndex(from, to, length());
      return new Slice(source, start + from, start + to);
    }

    @Override
    public String toString() {
      StringBuilder out = new StringBuilder(length());
      for (int i = 0; i < length(); i++) out.append(charAt(i));
      return out.toString();
    }
  }
}
