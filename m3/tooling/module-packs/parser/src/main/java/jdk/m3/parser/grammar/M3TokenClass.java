// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexTokenClass.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

/** Generic lexical classes usable by imported and built-in grammars. */
public enum M3TokenClass {
  IDENTIFIER,
  QUOTED_IDENTIFIER,
  NUMBER,
  STRING,
  BOOLEAN,
  NULL,
  NAME_PLACEHOLDER,
  VALUE_PLACEHOLDER,
  POSITIONAL_PARAMETER,
  JSON_LITERAL,
  SYMBOL,
  ANY
}
