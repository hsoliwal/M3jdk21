// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

/** Lossless lexical class attached to one M3File token occurrence. */
public enum M3FileTokenKind {
  WORD(0),
  NUMBER(1),
  WHITESPACE(2),
  NEWLINE(3),
  QUOTED(4),
  COMMENT(5),
  SYMBOL(6),
  OTHER(7);

  private final byte code;

  M3FileTokenKind(int code) {
    this.code = (byte) code;
  }

  byte code() {
    return code;
  }

  static M3FileTokenKind fromCode(byte code) {
    return switch (Byte.toUnsignedInt(code)) {
      case 0 -> WORD;
      case 1 -> NUMBER;
      case 2 -> WHITESPACE;
      case 3 -> NEWLINE;
      case 4 -> QUOTED;
      case 5 -> COMMENT;
      case 6 -> SYMBOL;
      case 7 -> OTHER;
      default -> throw new IllegalArgumentException("unknown M3File token kind");
    };
  }
}
