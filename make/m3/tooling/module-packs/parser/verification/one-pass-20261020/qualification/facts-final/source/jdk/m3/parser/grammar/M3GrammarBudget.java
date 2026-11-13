// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Objects;

/** Private mutable counters owned by exactly one lexical/parse/recognition invocation. */
final class M3GrammarBudget {
  private final M3GrammarLimits limits;
  private long remainingWork;
  private int states;
  private int nodes;

  M3GrammarBudget(M3GrammarLimits limits) {
    this.limits = Objects.requireNonNull(limits, "limits");
    remainingWork = limits.maxWork();
  }

  M3GrammarLimits limits() { return limits; }

  void source(int length) {
    if (length < 0 || length > limits.maxSourceChars()) exceeded("maxSourceChars");
  }

  String snapshot(CharSequence input) {
    Objects.requireNonNull(input, "source");
    int length = input.length();
    source(length);
    work(length);
    if (input instanceof String text) return text;
    char[] chars = new char[length];
    for (int index = 0; index < length; index++) chars[index] = input.charAt(index);
    return new String(chars);
  }

  void tokens(int count) {
    if (count < 0 || count > limits.maxTokens()) exceeded("maxTokens");
  }

  void state() {
    if (states == limits.maxStates()) exceeded("maxStates");
    states++;
  }

  void node() {
    work(1);
    if (nodes == limits.maxTreeNodes()) exceeded("maxTreeNodes");
    nodes++;
  }

  void children(int count) {
    if (count < 0 || (long) count + 1 > limits.maxTreeNodes() - (long) nodes) {
      exceeded("maxTreeNodes");
    }
    work(count);
  }

  void depth(int depth) {
    if (depth > limits.maxTreeDepth()) exceeded("maxTreeDepth");
  }

  void work(long amount) {
    if (amount < 0 || amount > remainingWork) exceeded("maxWork");
    remainingWork -= amount;
  }

  boolean matches(M3Grammar grammar, int symbol, M3TokenStream tokens, int position) {
    work(1L + tokens.length(position));
    return grammar.terminalMatches(symbol, tokens, position);
  }

  static int capacity(int old, int required, int maximum) {
    if (required < 0 || required > maximum) {
      throw new M3GrammarException("primitive arena coordinate limit exceeded");
    }
    return (int) Math.min(maximum, Math.max((long) required, old + (old >>> 1) + 1L));
  }

  private static void exceeded(String name) {
    throw new M3GrammarException("parser limit exceeded: " + name);
  }
}
