// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarSymbolKind.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

/** Physical symbol kind inside a frozen grammar image. */
public enum M3GrammarSymbolKind {
  NON_TERMINAL,
  LITERAL,
  TOKEN_CLASS
}
