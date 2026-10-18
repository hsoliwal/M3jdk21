// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarTokenDispatch.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable token-position to grammar-terminal dispatch generated once for one token stream.
 *
 * <p>The dispatch stores only grammar symbol IDs in primitive CSR lanes. Literal hash lookup is a
 * candidate filter; exact grammar terminal matching is performed before a symbol is admitted.</p>
 */
public final class M3GrammarTokenDispatch {
  private final M3Grammar grammar;
  private final M3TokenStream tokens;
  private final int[] offsets;
  private final int[] terminalSymbols;

  M3GrammarTokenDispatch(
      M3Grammar grammar,
      M3TokenStream tokens,
      int[] offsets,
      int[] terminalSymbols) {
    this.grammar = Objects.requireNonNull(grammar, "grammar");
    this.tokens = Objects.requireNonNull(tokens, "tokens");
    this.offsets = Objects.requireNonNull(offsets, "offsets");
    this.terminalSymbols = Objects.requireNonNull(terminalSymbols, "terminalSymbols");
    if (offsets.length != tokens.size() + 1 || offsets[0] != 0
        || offsets[offsets.length - 1] != terminalSymbols.length) {
      throw new IllegalArgumentException("invalid token dispatch CSR");
    }
  }

  public M3Grammar grammar() {
    return grammar;
  }

  public M3TokenStream tokens() {
    return tokens;
  }

  public int tokenCount() {
    return tokens.size();
  }

  public int terminalCount(int tokenIndex) {
    int checked = Objects.checkIndex(tokenIndex, tokens.size());
    return offsets[checked + 1] - offsets[checked];
  }

  public int terminalSymbol(int tokenIndex, int ordinal) {
    int checked = Objects.checkIndex(tokenIndex, tokens.size());
    int from = offsets[checked];
    int length = offsets[checked + 1] - from;
    return terminalSymbols[from + Objects.checkIndex(ordinal, length)];
  }

  public int[] terminalSymbols(int tokenIndex) {
    int checked = Objects.checkIndex(tokenIndex, tokens.size());
    return Arrays.copyOfRange(terminalSymbols, offsets[checked], offsets[checked + 1]);
  }

  public boolean matches(int tokenIndex, int terminalSymbol) {
    int checked = Objects.checkIndex(tokenIndex, tokens.size());
    int from = offsets[checked];
    int to = offsets[checked + 1];
    return Arrays.binarySearch(terminalSymbols, from, to, terminalSymbol) >= 0;
  }

  public long primitivePayloadBytes() {
    return Integer.BYTES * ((long) offsets.length + terminalSymbols.length);
  }
}
