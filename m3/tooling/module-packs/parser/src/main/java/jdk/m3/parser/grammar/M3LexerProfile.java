// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexLexerProfile.java
// Modified 2026-10-20: compact private flags while preserving the ordered fingerprint.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/** Immutable lexical policy shared by one or more frozen grammars. */
public final class M3LexerProfile {
  private static final int CASE_INSENSITIVE_LITERALS = 1 << 0;
  private static final int SINGLE_QUOTE_STRING = 1 << 1;
  private static final int DOUBLE_QUOTE_STRING = 1 << 2;
  private static final int DOLLAR_IN_IDENTIFIER = 1 << 3;
  private static final int DOUBLE_QUOTE_IDENTIFIER = 1 << 4;
  private static final int BACKTICK_IDENTIFIER = 1 << 5;
  private static final int BACKTICK_JSON_LITERAL = 1 << 6;
  private static final int LINE_COMMENT_DASH_DASH = 1 << 7;
  private static final int LINE_COMMENT_SLASH_SLASH = 1 << 8;
  private static final int BLOCK_COMMENTS = 1 << 9;
  private static final int HASH_NAME_PLACEHOLDER = 1 << 10;
  private static final int COLON_VALUE_PLACEHOLDER = 1 << 11;
  private static final int QUESTION_PARAMETER = 1 << 12;
  private static final int BRACKET_QUOTED_IDENTIFIER = 1 << 13;
  private static final int COLON_PARAMETER = 1 << 14;
  private static final int AT_PARAMETER = 1 << 15;
  private static final int DOLLAR_PARAMETER = 1 << 16;
  private static final int HEX_NUMBERS = 1 << 17;
  private static final int SPECIAL_NUMBER_LITERALS = 1 << 18;

  private final String identity;
  private final int flags;
  private final String[] operators;

  private M3LexerProfile(Builder builder) {
    identity = builder.identity;
    flags = builder.flags;
    operators = builder.operators.clone();
    Arrays.sort(operators, Comparator.comparingInt(String::length).reversed().thenComparing(s -> s));
  }

  public static Builder builder(String identity) { return new Builder(identity); }
  public String identity() { return identity; }
  public boolean caseInsensitiveLiterals() { return (flags & CASE_INSENSITIVE_LITERALS) != 0; }
  public boolean singleQuoteString() { return (flags & SINGLE_QUOTE_STRING) != 0; }
  public boolean doubleQuoteString() { return (flags & DOUBLE_QUOTE_STRING) != 0; }
  public boolean dollarInIdentifier() { return (flags & DOLLAR_IN_IDENTIFIER) != 0; }
  public boolean doubleQuoteIdentifier() { return (flags & DOUBLE_QUOTE_IDENTIFIER) != 0; }
  public boolean backtickIdentifier() { return (flags & BACKTICK_IDENTIFIER) != 0; }
  public boolean backtickJsonLiteral() { return (flags & BACKTICK_JSON_LITERAL) != 0; }
  public boolean lineCommentDashDash() { return (flags & LINE_COMMENT_DASH_DASH) != 0; }
  public boolean lineCommentSlashSlash() { return (flags & LINE_COMMENT_SLASH_SLASH) != 0; }
  public boolean blockComments() { return (flags & BLOCK_COMMENTS) != 0; }
  public boolean hashNamePlaceholder() { return (flags & HASH_NAME_PLACEHOLDER) != 0; }
  public boolean colonValuePlaceholder() { return (flags & COLON_VALUE_PLACEHOLDER) != 0; }
  public boolean questionParameter() { return (flags & QUESTION_PARAMETER) != 0; }
  public boolean bracketQuotedIdentifier() { return (flags & BRACKET_QUOTED_IDENTIFIER) != 0; }
  public boolean colonParameter() { return (flags & COLON_PARAMETER) != 0; }
  public boolean atParameter() { return (flags & AT_PARAMETER) != 0; }
  public boolean dollarParameter() { return (flags & DOLLAR_PARAMETER) != 0; }
  public boolean hexNumbers() { return (flags & HEX_NUMBERS) != 0; }
  public boolean specialNumberLiterals() { return (flags & SPECIAL_NUMBER_LITERALS) != 0; }
  String[] operatorsForReadOnlyKernel() { return operators; }

  /** Stable semantic fingerprint of the immutable lexer policy. */
  public long fingerprint() {
    long hash = mix(0x4d494c4558455231L, identity.hashCode());
    hash = mix(hash, caseInsensitiveLiterals() ? 1 : 0);
    hash = mix(hash, singleQuoteString() ? 1 : 0);
    hash = mix(hash, doubleQuoteString() ? 1 : 0);
    hash = mix(hash, dollarInIdentifier() ? 1 : 0);
    hash = mix(hash, doubleQuoteIdentifier() ? 1 : 0);
    hash = mix(hash, backtickIdentifier() ? 1 : 0);
    hash = mix(hash, backtickJsonLiteral() ? 1 : 0);
    hash = mix(hash, lineCommentDashDash() ? 1 : 0);
    hash = mix(hash, lineCommentSlashSlash() ? 1 : 0);
    hash = mix(hash, blockComments() ? 1 : 0);
    hash = mix(hash, hashNamePlaceholder() ? 1 : 0);
    hash = mix(hash, colonValuePlaceholder() ? 1 : 0);
    hash = mix(hash, questionParameter() ? 1 : 0);
    hash = mix(hash, bracketQuotedIdentifier() ? 1 : 0);
    hash = mix(hash, colonParameter() ? 1 : 0);
    hash = mix(hash, atParameter() ? 1 : 0);
    hash = mix(hash, dollarParameter() ? 1 : 0);
    hash = mix(hash, hexNumbers() ? 1 : 0);
    hash = mix(hash, specialNumberLiterals() ? 1 : 0);
    for (String operator : operators) hash = mix(hash, operator.hashCode());
    return hash;
  }

  private static long mix(long left, long right) {
    long z = left ^ Long.rotateLeft(right + 0x9e3779b97f4a7c15L, 29);
    z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
    z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
    return z ^ (z >>> 31);
  }

  public static final class Builder {
    private final String identity;
    private int flags;
    private String[] operators = new String[0];

    private Builder(String identity) { this.identity = Objects.requireNonNull(identity, "identity"); }
    public Builder caseInsensitiveLiterals(boolean v) { return set(CASE_INSENSITIVE_LITERALS, v); }
    public Builder singleQuoteString(boolean v) { return set(SINGLE_QUOTE_STRING, v); }
    public Builder doubleQuoteString(boolean v) { return set(DOUBLE_QUOTE_STRING, v); }
    public Builder dollarInIdentifier(boolean v) { return set(DOLLAR_IN_IDENTIFIER, v); }
    public Builder doubleQuoteIdentifier(boolean v) { return set(DOUBLE_QUOTE_IDENTIFIER, v); }
    public Builder backtickIdentifier(boolean v) { return set(BACKTICK_IDENTIFIER, v); }
    public Builder backtickJsonLiteral(boolean v) { return set(BACKTICK_JSON_LITERAL, v); }
    public Builder lineCommentDashDash(boolean v) { return set(LINE_COMMENT_DASH_DASH, v); }
    public Builder lineCommentSlashSlash(boolean v) { return set(LINE_COMMENT_SLASH_SLASH, v); }
    public Builder blockComments(boolean v) { return set(BLOCK_COMMENTS, v); }
    public Builder hashNamePlaceholder(boolean v) { return set(HASH_NAME_PLACEHOLDER, v); }
    public Builder colonValuePlaceholder(boolean v) { return set(COLON_VALUE_PLACEHOLDER, v); }
    public Builder questionParameter(boolean v) { return set(QUESTION_PARAMETER, v); }
    public Builder bracketQuotedIdentifier(boolean v) { return set(BRACKET_QUOTED_IDENTIFIER, v); }
    public Builder colonParameter(boolean v) { return set(COLON_PARAMETER, v); }
    public Builder atParameter(boolean v) { return set(AT_PARAMETER, v); }
    public Builder dollarParameter(boolean v) { return set(DOLLAR_PARAMETER, v); }
    public Builder hexNumbers(boolean v) { return set(HEX_NUMBERS, v); }
    public Builder specialNumberLiterals(boolean v) { return set(SPECIAL_NUMBER_LITERALS, v); }
    private Builder set(int flag, boolean value) {
      flags = value ? flags | flag : flags & ~flag;
      return this;
    }
    public Builder operators(String... values) {
      operators = Objects.requireNonNull(values, "values").clone();
      for (String value : operators) if (value == null || value.isEmpty()) throw new IllegalArgumentException("empty operator");
      return this;
    }
    public M3LexerProfile build() {
      if ((flags & (BACKTICK_IDENTIFIER | BACKTICK_JSON_LITERAL)) == (BACKTICK_IDENTIFIER | BACKTICK_JSON_LITERAL)) {
        throw new IllegalStateException("backtick role conflict");
      }
      if ((flags & (COLON_VALUE_PLACEHOLDER | COLON_PARAMETER)) == (COLON_VALUE_PLACEHOLDER | COLON_PARAMETER)) {
        throw new IllegalStateException("colon token role conflict");
      }
      if ((flags & (DOLLAR_IN_IDENTIFIER | DOLLAR_PARAMETER)) == (DOLLAR_IN_IDENTIFIER | DOLLAR_PARAMETER)) {
        throw new IllegalStateException("dollar token role conflict");
      }
      return new M3LexerProfile(this);
    }
  }
}
