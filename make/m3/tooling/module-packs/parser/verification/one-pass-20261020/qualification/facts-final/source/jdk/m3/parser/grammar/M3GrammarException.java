// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarException.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

/** Syntax/import error for frozen grammar compilation or strict validation. */
public final class M3GrammarException extends IllegalArgumentException {
  private static final long serialVersionUID = 1L;

  public M3GrammarException(String message) {
    super(message);
  }

  public M3GrammarException(String message, Throwable cause) {
    super(message, cause);
  }
}
