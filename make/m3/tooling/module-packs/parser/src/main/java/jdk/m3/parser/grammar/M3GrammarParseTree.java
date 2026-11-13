// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarParseTree.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Arrays;
import java.util.Objects;

/** Compact immutable parse tree over one frozen grammar and zero-copy token stream. */
public final class M3GrammarParseTree {
  private final M3Grammar grammar;
  private final M3TokenStream tokens;
  private final int root;
  private final int[] symbols;
  private final int[] productions;
  private final int[] tokenIndices;
  private final int[] fromTokens;
  private final int[] toTokens;
  private final int[] childStarts;
  private final int[] childCounts;
  private final int[] childArena;

  M3GrammarParseTree(
      M3Grammar grammar,
      M3TokenStream tokens,
      int root,
      int[] symbols,
      int[] productions,
      int[] tokenIndices,
      int[] fromTokens,
      int[] toTokens,
      int[] childStarts,
      int[] childCounts,
      int[] childArena) {
    this.grammar = Objects.requireNonNull(grammar, "grammar");
    this.tokens = Objects.requireNonNull(tokens, "tokens");
    this.root = root;
    this.symbols = symbols;
    this.productions = productions;
    this.tokenIndices = tokenIndices;
    this.fromTokens = fromTokens;
    this.toTokens = toTokens;
    this.childStarts = childStarts;
    this.childCounts = childCounts;
    this.childArena = childArena;
    Objects.checkIndex(root, symbols.length);
  }

  public M3Grammar grammar() { return grammar; }
  public M3TokenStream tokens() { return tokens; }
  public int root() { return root; }
  public int nodeCount() { return symbols.length; }
  public int symbol(int node) { return symbols[Objects.checkIndex(node, symbols.length)]; }
  public String symbolName(int node) { return grammar.symbolName(symbol(node)); }

  /** Exact production chosen for a non-terminal node, or -1 for a terminal node. */
  public int production(int node) {
    return productions[Objects.checkIndex(node, productions.length)];
  }

  public boolean terminal(int node) { return tokenIndices[Objects.checkIndex(node, tokenIndices.length)] >= 0; }
  public int tokenIndex(int node) { return tokenIndices[Objects.checkIndex(node, tokenIndices.length)]; }
  public int fromToken(int node) { return fromTokens[Objects.checkIndex(node, fromTokens.length)]; }
  public int toToken(int node) { return toTokens[Objects.checkIndex(node, toTokens.length)]; }
  public int childCount(int node) { return childCounts[Objects.checkIndex(node, childCounts.length)]; }

  public int child(int node, int ordinal) {
    int checked = Objects.checkIndex(node, childCounts.length);
    return childArena[childStarts[checked] + Objects.checkIndex(ordinal, childCounts[checked])];
  }

  public int[] children(int node) {
    int checked = Objects.checkIndex(node, childCounts.length);
    return Arrays.copyOfRange(
        childArena, childStarts[checked], childStarts[checked] + childCounts[checked]);
  }

  public String tokenText(int node) {
    int token = tokenIndex(node);
    if (token < 0) throw new IllegalStateException("non-terminal node has no direct token");
    return tokens.textAt(token);
  }

  public long primitivePayloadBytes() {
    return Integer.BYTES
        * ((long) symbols.length
                + productions.length
                + tokenIndices.length
                + fromTokens.length
                + toTokens.length
                + childStarts.length
                + childCounts.length
                + childArena.length);
  }
}
