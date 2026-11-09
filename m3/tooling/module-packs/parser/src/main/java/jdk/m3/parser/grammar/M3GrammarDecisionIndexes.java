// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarDecisionIndexes.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Objects;

/** Convenience access to a decision index retained by its exact grammar owner. */
public final class M3GrammarDecisionIndexes {
  private M3GrammarDecisionIndexes() {}

  public static M3GrammarDecisionIndex forGrammar(M3Grammar grammar) {
    return Objects.requireNonNull(grammar, "grammar").decisionIndex();
  }
}
