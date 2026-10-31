// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Objects;
import java.util.StringJoiner;

/** Named primitive properties, shared per admitted spelling rather than per occurrence. */
public final class M3CharacterFlags {
  public static final int ASCII = 1;
  public static final int LATIN1 = 1 << 1;
  public static final int HAS_ASCII_UPPER = 1 << 2;
  public static final int HAS_ASCII_LOWER = 1 << 3;
  public static final int HAS_SURROGATE = 1 << 4;
  public static final int BLANK = 1 << 5;
  public static final int EMPTY = 1 << 6;
  public static final int HAS_ASCII_DIGIT = 1 << 7;
  public static final int HAS_ASCII_WORD = 1 << 8;
  public static final int HAS_ASCII_SPACE = 1 << 9;
  public static final int REGEX_PRESENCE =
      HAS_ASCII_UPPER | HAS_ASCII_LOWER | HAS_ASCII_DIGIT | HAS_ASCII_WORD | HAS_ASCII_SPACE;
  public static final int ALL = (1 << 10) - 1;

  private M3CharacterFlags() {}

  public static int of(CharSequence text) {
    Objects.requireNonNull(text, "text");
    int flags = initial(text.length());
    for (int i = 0; i < text.length(); i++) flags = add(flags, text.charAt(i));
    return flags;
  }

  static int initial(int length) {
    if (length < 0) throw new IllegalArgumentException("length");
    int flags = ASCII | LATIN1 | BLANK;
    return length == 0 ? flags | EMPTY : flags;
  }

  static int add(int flags, char value) {
    if (value > 0x7f) flags &= ~ASCII;
    if (value > 0xff) flags &= ~LATIN1;
    if (value >= 'A' && value <= 'Z') flags |= HAS_ASCII_UPPER | HAS_ASCII_WORD;
    if (value >= 'a' && value <= 'z') flags |= HAS_ASCII_LOWER | HAS_ASCII_WORD;
    if (value >= '0' && value <= '9') flags |= HAS_ASCII_DIGIT | HAS_ASCII_WORD;
    if (value == '_') flags |= HAS_ASCII_WORD;
    if (isRegexAsciiSpace(value)) flags |= HAS_ASCII_SPACE;
    if (Character.isSurrogate(value)) flags |= HAS_SURROGATE;
    if (!Character.isWhitespace(value)) flags &= ~BLANK;
    return flags;
  }

  /** Intended for debugger detail formatters; never needed on the query hot path. */
  public static String labels(int flags) {
    if ((flags & ~ALL) != 0) throw new IllegalArgumentException("unknown character flags");
    StringJoiner out = new StringJoiner("|");
    if ((flags & ASCII) != 0) out.add("ASCII");
    if ((flags & LATIN1) != 0) out.add("LATIN1");
    if ((flags & HAS_ASCII_UPPER) != 0) out.add("HAS_ASCII_UPPER");
    if ((flags & HAS_ASCII_LOWER) != 0) out.add("HAS_ASCII_LOWER");
    if ((flags & HAS_SURROGATE) != 0) out.add("HAS_SURROGATE");
    if ((flags & BLANK) != 0) out.add("BLANK");
    if ((flags & EMPTY) != 0) out.add("EMPTY");
    if ((flags & HAS_ASCII_DIGIT) != 0) out.add("HAS_ASCII_DIGIT");
    if ((flags & HAS_ASCII_WORD) != 0) out.add("HAS_ASCII_WORD");
    if ((flags & HAS_ASCII_SPACE) != 0) out.add("HAS_ASCII_SPACE");
    return out.toString();
  }

  static boolean isRegexAsciiSpace(char value) {
    return value == ' ' || (value >= '\t' && value <= '\r');
  }
}
