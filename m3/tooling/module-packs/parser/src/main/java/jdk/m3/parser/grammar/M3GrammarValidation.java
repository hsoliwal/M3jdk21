// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarValidation.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Arrays;
import java.util.Objects;

/** Immutable parse/conformance result with the furthest token and expected terminal symbols. */
public final class M3GrammarValidation {
  private final boolean accepted;
  private final boolean conforms;
  private final int tokenCount;
  private final int furthestToken;
  private final boolean expectedEndOfInput;
  private final int[] expectedSymbols;

  M3GrammarValidation(
      boolean accepted,
      boolean conforms,
      int tokenCount,
      int furthestToken,
      boolean expectedEndOfInput,
      int[] expectedSymbols) {
    this.accepted = accepted;
    this.conforms = conforms;
    this.tokenCount = tokenCount;
    this.furthestToken = furthestToken;
    this.expectedEndOfInput = expectedEndOfInput;
    this.expectedSymbols = Objects.requireNonNull(expectedSymbols, "expectedSymbols").clone();
  }

  public boolean accepted() {
    return accepted;
  }

  public boolean conforms() {
    return conforms;
  }

  public int tokenCount() {
    return tokenCount;
  }

  public int furthestToken() {
    return furthestToken;
  }

  public boolean expectedEndOfInput() {
    return expectedEndOfInput;
  }

  public int[] expectedSymbols() {
    return expectedSymbols.clone();
  }

  @Override
  public String toString() {
    return "M3GrammarValidation[accepted="
        + accepted
        + ",conforms="
        + conforms
        + ",furthestToken="
        + furthestToken
        + ",expectedEndOfInput="
        + expectedEndOfInput
        + ",expected="
        + Arrays.toString(expectedSymbols)
        + "]";
  }
}
