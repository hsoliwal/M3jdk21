// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexLexerProfile.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/** Immutable lexical policy shared by one or more frozen grammars. */
public final class M3LexerProfile {
  private final String identity;
  private final boolean caseInsensitiveLiterals;
  private final boolean singleQuoteString;
  private final boolean doubleQuoteString;
  private final boolean dollarInIdentifier;
  private final boolean doubleQuoteIdentifier;
  private final boolean backtickIdentifier;
  private final boolean backtickJsonLiteral;
  private final boolean lineCommentDashDash;
  private final boolean lineCommentSlashSlash;
  private final boolean blockComments;
  private final boolean hashNamePlaceholder;
  private final boolean colonValuePlaceholder;
  private final boolean questionParameter;
  private final boolean bracketQuotedIdentifier;
  private final boolean colonParameter;
  private final boolean atParameter;
  private final boolean dollarParameter;
  private final boolean hexNumbers;
  private final boolean specialNumberLiterals;
  private final String[] operators;

  private M3LexerProfile(Builder builder) {
    identity = builder.identity;
    caseInsensitiveLiterals = builder.caseInsensitiveLiterals;
    singleQuoteString = builder.singleQuoteString;
    doubleQuoteString = builder.doubleQuoteString;
    dollarInIdentifier = builder.dollarInIdentifier;
    doubleQuoteIdentifier = builder.doubleQuoteIdentifier;
    backtickIdentifier = builder.backtickIdentifier;
    backtickJsonLiteral = builder.backtickJsonLiteral;
    lineCommentDashDash = builder.lineCommentDashDash;
    lineCommentSlashSlash = builder.lineCommentSlashSlash;
    blockComments = builder.blockComments;
    hashNamePlaceholder = builder.hashNamePlaceholder;
    colonValuePlaceholder = builder.colonValuePlaceholder;
    questionParameter = builder.questionParameter;
    bracketQuotedIdentifier = builder.bracketQuotedIdentifier;
    colonParameter = builder.colonParameter;
    atParameter = builder.atParameter;
    dollarParameter = builder.dollarParameter;
    hexNumbers = builder.hexNumbers;
    specialNumberLiterals = builder.specialNumberLiterals;
    operators = builder.operators.clone();
    Arrays.sort(operators, Comparator.comparingInt(String::length).reversed().thenComparing(s -> s));
  }

  public static Builder builder(String identity) { return new Builder(identity); }
  public String identity() { return identity; }
  public boolean caseInsensitiveLiterals() { return caseInsensitiveLiterals; }
  public boolean singleQuoteString() { return singleQuoteString; }
  public boolean doubleQuoteString() { return doubleQuoteString; }
  public boolean dollarInIdentifier() { return dollarInIdentifier; }
  public boolean doubleQuoteIdentifier() { return doubleQuoteIdentifier; }
  public boolean backtickIdentifier() { return backtickIdentifier; }
  public boolean backtickJsonLiteral() { return backtickJsonLiteral; }
  public boolean lineCommentDashDash() { return lineCommentDashDash; }
  public boolean lineCommentSlashSlash() { return lineCommentSlashSlash; }
  public boolean blockComments() { return blockComments; }
  public boolean hashNamePlaceholder() { return hashNamePlaceholder; }
  public boolean colonValuePlaceholder() { return colonValuePlaceholder; }
  public boolean questionParameter() { return questionParameter; }
  public boolean bracketQuotedIdentifier() { return bracketQuotedIdentifier; }
  public boolean colonParameter() { return colonParameter; }
  public boolean atParameter() { return atParameter; }
  public boolean dollarParameter() { return dollarParameter; }
  public boolean hexNumbers() { return hexNumbers; }
  public boolean specialNumberLiterals() { return specialNumberLiterals; }
  String[] operatorsForReadOnlyKernel() { return operators; }

  /** Stable semantic fingerprint of the immutable lexer policy. */
  public long fingerprint() {
    long hash = mix(0x4d494c4558455231L, identity.hashCode());
    hash = mix(hash, caseInsensitiveLiterals ? 1 : 0);
    hash = mix(hash, singleQuoteString ? 1 : 0);
    hash = mix(hash, doubleQuoteString ? 1 : 0);
    hash = mix(hash, dollarInIdentifier ? 1 : 0);
    hash = mix(hash, doubleQuoteIdentifier ? 1 : 0);
    hash = mix(hash, backtickIdentifier ? 1 : 0);
    hash = mix(hash, backtickJsonLiteral ? 1 : 0);
    hash = mix(hash, lineCommentDashDash ? 1 : 0);
    hash = mix(hash, lineCommentSlashSlash ? 1 : 0);
    hash = mix(hash, blockComments ? 1 : 0);
    hash = mix(hash, hashNamePlaceholder ? 1 : 0);
    hash = mix(hash, colonValuePlaceholder ? 1 : 0);
    hash = mix(hash, questionParameter ? 1 : 0);
    hash = mix(hash, bracketQuotedIdentifier ? 1 : 0);
    hash = mix(hash, colonParameter ? 1 : 0);
    hash = mix(hash, atParameter ? 1 : 0);
    hash = mix(hash, dollarParameter ? 1 : 0);
    hash = mix(hash, hexNumbers ? 1 : 0);
    hash = mix(hash, specialNumberLiterals ? 1 : 0);
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
    private boolean caseInsensitiveLiterals;
    private boolean singleQuoteString;
    private boolean doubleQuoteString;
    private boolean dollarInIdentifier;
    private boolean doubleQuoteIdentifier;
    private boolean backtickIdentifier;
    private boolean backtickJsonLiteral;
    private boolean lineCommentDashDash;
    private boolean lineCommentSlashSlash;
    private boolean blockComments;
    private boolean hashNamePlaceholder;
    private boolean colonValuePlaceholder;
    private boolean questionParameter;
    private boolean bracketQuotedIdentifier;
    private boolean colonParameter;
    private boolean atParameter;
    private boolean dollarParameter;
    private boolean hexNumbers;
    private boolean specialNumberLiterals;
    private String[] operators = new String[0];

    private Builder(String identity) { this.identity = Objects.requireNonNull(identity, "identity"); }
    public Builder caseInsensitiveLiterals(boolean v) { caseInsensitiveLiterals = v; return this; }
    public Builder singleQuoteString(boolean v) { singleQuoteString = v; return this; }
    public Builder doubleQuoteString(boolean v) { doubleQuoteString = v; return this; }
    public Builder dollarInIdentifier(boolean v) { dollarInIdentifier = v; return this; }
    public Builder doubleQuoteIdentifier(boolean v) { doubleQuoteIdentifier = v; return this; }
    public Builder backtickIdentifier(boolean v) { backtickIdentifier = v; return this; }
    public Builder backtickJsonLiteral(boolean v) { backtickJsonLiteral = v; return this; }
    public Builder lineCommentDashDash(boolean v) { lineCommentDashDash = v; return this; }
    public Builder lineCommentSlashSlash(boolean v) { lineCommentSlashSlash = v; return this; }
    public Builder blockComments(boolean v) { blockComments = v; return this; }
    public Builder hashNamePlaceholder(boolean v) { hashNamePlaceholder = v; return this; }
    public Builder colonValuePlaceholder(boolean v) { colonValuePlaceholder = v; return this; }
    public Builder questionParameter(boolean v) { questionParameter = v; return this; }
    public Builder bracketQuotedIdentifier(boolean v) { bracketQuotedIdentifier = v; return this; }
    public Builder colonParameter(boolean v) { colonParameter = v; return this; }
    public Builder atParameter(boolean v) { atParameter = v; return this; }
    public Builder dollarParameter(boolean v) { dollarParameter = v; return this; }
    public Builder hexNumbers(boolean v) { hexNumbers = v; return this; }
    public Builder specialNumberLiterals(boolean v) { specialNumberLiterals = v; return this; }
    public Builder operators(String... values) {
      operators = Objects.requireNonNull(values, "values").clone();
      for (String value : operators) if (value == null || value.isEmpty()) throw new IllegalArgumentException("empty operator");
      return this;
    }
    public M3LexerProfile build() {
      if (backtickIdentifier && backtickJsonLiteral) {
        throw new IllegalStateException("backtick role conflict");
      }
      if (colonValuePlaceholder && colonParameter) {
        throw new IllegalStateException("colon token role conflict");
      }
      if (dollarInIdentifier && dollarParameter) {
        throw new IllegalStateException("dollar token role conflict");
      }
      return new M3LexerProfile(this);
    }
  }
}
