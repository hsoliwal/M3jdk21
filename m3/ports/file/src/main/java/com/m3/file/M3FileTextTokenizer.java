// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
/** Exact CharSequence partition required by the donor file descriptor builder convenience. */
final class M3FileTextTokenizer {
  enum TokenKind { WORD, WHITESPACE, SYMBOL }
  @FunctionalInterface interface TokenConsumer {
    void accept(TokenKind kind, int startUtf16, int endUtf16, String token);
  }
  private static final byte[] BMP_KIND = buildBmpKindTable();
  private M3FileTextTokenizer() { }
  public static List<String> tokenize(CharSequence input) {
    Objects.requireNonNull(input, "input");
    List<String> tokens = new ArrayList<>();
    forEachToken(input, tokens::add);
    return List.copyOf(tokens);
  }

  public static void forEachToken(CharSequence input, Consumer<? super String> consumer) {
    Objects.requireNonNull(consumer, "consumer");
    forEachTokenSpan(input, (kind, start, end, token) -> consumer.accept(token));
  }

  private static byte[] buildBmpKindTable() {
    byte[] table = new byte[Character.MAX_VALUE + 1];
    for (int codePoint = 0; codePoint < table.length; codePoint++) {
      table[codePoint] = (byte) kindOfCodePointSlow(codePoint);
    }
    return table;
  }

  /**
   * Emits exact text tokens plus their UTF-16 source span.
   *
   * <p>Word-like runs, whitespace runs and individual punctuation/symbol code points partition the
   * input exactly. Concatenating the emitted token strings reconstructs the supplied characters.
   */
  public static void forEachTokenSpan(CharSequence input, TokenConsumer consumer) {
    Objects.requireNonNull(input, "input");
    Objects.requireNonNull(consumer, "consumer");
    StringBuilder run = new StringBuilder();
    int runKind = -1;
    int runStart = -1;
    for (int offset = 0; offset < input.length();) {
      int start = offset;
      int codePoint = Character.codePointAt(input, offset);
      int kind = kindOfCodePoint(codePoint);
      offset += Character.charCount(codePoint);
      if (kind == 2) {
        if (!run.isEmpty()) {
          consumer.accept(tokenKind(runKind), runStart, start, run.toString());
          run.setLength(0);
          runKind = -1;
          runStart = -1;
        }
        consumer.accept(TokenKind.SYMBOL, start, offset, new String(Character.toChars(codePoint)));
      } else {
        if (runKind != -1 && runKind != kind) {
          consumer.accept(tokenKind(runKind), runStart, start, run.toString());
          run.setLength(0);
          runStart = start;
        } else if (runKind == -1) {
          runStart = start;
        }
        runKind = kind;
        run.appendCodePoint(codePoint);
      }
    }
    if (!run.isEmpty()) {
      consumer.accept(tokenKind(runKind), runStart, input.length(), run.toString());
    }
  }

  private static TokenKind tokenKind(int kind) {
    return switch (kind) {
      case 0 -> TokenKind.WORD;
      case 1 -> TokenKind.WHITESPACE;
      default -> throw new IllegalArgumentException("kind=" + kind);
    };
  }

  /**
   * Token class for one decoded Unicode code point.
   *
   * <p>All BMP values are one primitive array read. Only supplementary code points consult JDK
   * Unicode property logic at runtime.</p>
   */
  static int kindOfCodePoint(int codePoint) {
    if ((codePoint & ~0xffff) == 0) {
      return Byte.toUnsignedInt(BMP_KIND[codePoint]);
    }
    return kindOfCodePointSlow(codePoint);
  }

  private static int kindOfCodePointSlow(int codePoint) {
    if (Character.isWhitespace(codePoint)) return 1;
    int type = Character.getType(codePoint);
    if (Character.isLetterOrDigit(codePoint)
        || type == Character.NON_SPACING_MARK
        || type == Character.COMBINING_SPACING_MARK
        || type == Character.CONNECTOR_PUNCTUATION
        || codePoint == '\''
        || codePoint == 0x2019) return 0;
    return 2;
  }
}
