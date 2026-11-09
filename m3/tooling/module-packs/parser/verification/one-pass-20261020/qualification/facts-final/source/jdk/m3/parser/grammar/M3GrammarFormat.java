// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarFormat.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

/** Source notations accepted by the one-time grammar compiler. */
public enum M3GrammarFormat {
  BNF,
  EBNF,
  ABNF,
  ANTLR4,
  JAVACC,
  YACC_BISON
}
