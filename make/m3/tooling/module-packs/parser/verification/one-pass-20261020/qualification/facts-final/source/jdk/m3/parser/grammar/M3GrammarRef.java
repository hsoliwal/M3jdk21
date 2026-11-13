// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarRef.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Objects;

/** Symbol reference used while compiling a grammar; compiled images replace it with integer IDs. */
public record M3GrammarRef(
    M3GrammarSymbolKind kind,
    String value,
    M3TokenClass tokenClass,
    M3LiteralCase literalCase) {
  public M3GrammarRef {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(value, "value");
    if (kind == M3GrammarSymbolKind.TOKEN_CLASS) Objects.requireNonNull(tokenClass, "tokenClass");
    else if (tokenClass != null) throw new IllegalArgumentException("tokenClass only valid for TOKEN_CLASS");
    Objects.requireNonNull(literalCase, "literalCase");
    if (kind != M3GrammarSymbolKind.LITERAL && literalCase != M3LiteralCase.PROFILE) {
      throw new IllegalArgumentException("literalCase only valid for literal symbols");
    }
  }
  public static M3GrammarRef nonTerminal(String name) {
    return new M3GrammarRef(
        M3GrammarSymbolKind.NON_TERMINAL,
        Objects.requireNonNull(name, "name"),
        null,
        M3LiteralCase.PROFILE);
  }
  public static M3GrammarRef literal(String text) {
    return new M3GrammarRef(
        M3GrammarSymbolKind.LITERAL,
        Objects.requireNonNull(text, "text"),
        null,
        M3LiteralCase.PROFILE);
  }
  public static M3GrammarRef literalSensitive(String text) {
    return new M3GrammarRef(
        M3GrammarSymbolKind.LITERAL,
        Objects.requireNonNull(text, "text"),
        null,
        M3LiteralCase.SENSITIVE);
  }
  public static M3GrammarRef literalInsensitive(String text) {
    return new M3GrammarRef(
        M3GrammarSymbolKind.LITERAL,
        Objects.requireNonNull(text, "text"),
        null,
        M3LiteralCase.INSENSITIVE);
  }
  public static M3GrammarRef token(M3TokenClass tokenClass) {
    Objects.requireNonNull(tokenClass, "tokenClass");
    return new M3GrammarRef(
        M3GrammarSymbolKind.TOKEN_CLASS,
        "<" + tokenClass.name() + ">",
        tokenClass,
        M3LiteralCase.PROFILE);
  }
}
