// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarRecognizer.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Arrays;
import java.util.Objects;

/**
 * Earley recognizer over a frozen {@link M3Grammar}.
 *
 * <p>Chart states are packed longs: 24-bit production, 16-bit dot and 24-bit origin token.
 * This keeps left recursion and ambiguous CFG support without allocating one object per state.</p>
 */
public final class M3GrammarRecognizer {
  private static final int MAX_24 = 0x00ff_ffff;
  private static final int MAX_16 = 0x0000_ffff;
  private final M3Grammar grammar;
  private final M3GrammarDecisionIndex decisions;
  private final boolean indexedCompletion;
  private final boolean nullableReplay;
  private final M3GrammarLimits limits;

  public M3GrammarRecognizer(M3Grammar grammar) {
    this(grammar, M3GrammarLimits.defaults());
  }

  public M3GrammarRecognizer(M3Grammar grammar, M3GrammarLimits limits) {
    this(grammar, limits, true, true, false);
  }

  M3GrammarRecognizer(M3Grammar grammar, boolean optimized) {
    this(grammar, M3GrammarLimits.defaults(), optimized, optimized, false);
  }

  M3GrammarRecognizer(M3Grammar grammar, boolean predictiveDispatch, boolean indexedCompletion) {
    this(grammar, M3GrammarLimits.defaults(), predictiveDispatch, indexedCompletion, false);
  }

  private M3GrammarRecognizer(M3Grammar grammar, M3GrammarLimits limits,
      boolean predictiveDispatch, boolean indexedCompletion, boolean nullableReplay) {
    this.grammar = Objects.requireNonNull(grammar, "grammar");
    this.limits = Objects.requireNonNull(limits, "limits");
    this.decisions = predictiveDispatch ? grammar.decisionIndex() : null;
    this.indexedCompletion = indexedCompletion;
    this.nullableReplay = nullableReplay;
  }

  public M3GrammarLimits limits() { return limits; }

  public M3Grammar grammar() { return grammar; }

  public M3GrammarValidation validate(CharSequence source) {
    M3GrammarBudget budget = new M3GrammarBudget(limits);
    M3TokenStream tokens = new M3GrammarLexer(grammar.lexerProfile(), limits).lex(source, budget);
    return validate(tokens, budget);
  }

  public M3GrammarValidation validate(M3TokenStream tokens) {
    return validate(tokens, new M3GrammarBudget(limits));
  }

  private M3GrammarValidation validate(M3TokenStream tokens, M3GrammarBudget budget) {
    Objects.requireNonNull(tokens, "tokens");
    budget.source(tokens.source().length());
    budget.tokens(tokens.size());
    if (grammar.productionCount() == 0 && grammar.strictness() == M3GrammarStrictness.FREE) {
      return new M3GrammarValidation(
          true, true, tokens.size(), tokens.size(), false, new int[0]);
    }
    int minimum = grammar.minimumTokens(grammar.startSymbol());
    if (minimum >= 0 && tokens.size() < minimum) {
      int[] expected = grammar.firstSymbols(grammar.startSymbol());
      boolean accepted = grammar.strictness().acceptNonConforming();
      return new M3GrammarValidation(
          accepted, false, tokens.size(), 0, false, expected);
    }
    if (grammar.productionCount() > MAX_24) throw new M3GrammarException("too many productions for packed Earley state");
    if (tokens.size() > MAX_24) throw new M3GrammarException("too many tokens for packed Earley origin");
    for (int p = 0; p < grammar.productionCount(); p++) if (grammar.productionRhsLength(p) > MAX_16) throw new M3GrammarException("production RHS too long for packed Earley state");

    M3GrammarTokenDispatch dispatch = decisions == null ? null : decisions.dispatch(tokens, budget);
    StateSet[] chart = new StateSet[tokens.size() + 1];
    for (int i = 0; i < chart.length; i++) {
      chart[i] = new StateSet(grammar, indexedCompletion, budget);
    }
    int start = grammar.startSymbol();
    addPredictions(chart[0], start, 0, dispatch);

    int furthest = 0;
    for (int position = 0; position <= tokens.size(); position++) {
      StateSet current = chart[position];
      for (int stateIndex = 0; stateIndex < current.size(); stateIndex++) {
        budget.work(1);
        long state = current.get(stateIndex);
        int production = production(state), dot = dot(state), origin = origin(state);
        int rhsLength = grammar.productionRhsLength(production);
        if (dot < rhsLength) {
          int symbol = grammar.productionRhsSymbol(production, dot);
          if (grammar.isNonTerminal(symbol)) {
            addPredictions(current, symbol, position, dispatch);
            if (nullableReplay && grammar.nullable(symbol)) {
              // Only old completions need replay; later entries run their normal completer.
              for (int completeIndex = 0; completeIndex < stateIndex; completeIndex++) {
                budget.work(1);
                long completedState = current.get(completeIndex);
                int completedProduction = production(completedState);
                if (origin(completedState) == position
                    && grammar.productionLhs(completedProduction) == symbol
                    && dot(completedState) == grammar.productionRhsLength(completedProduction)) {
                  current.add(pack(production, dot + 1, origin));
                }
              }
            }
          } else if (position < tokens.size() && budget.matches(grammar, symbol, tokens, position)) {
            chart[position + 1].add(pack(production, dot + 1, origin));
          }
        } else {
          int completed = grammar.productionLhs(production);
          StateSet waiting = chart[origin];
          if (indexedCompletion) {
            for (int waitingIndex = waiting.firstWaiting(completed);
                waitingIndex >= 0;
                waitingIndex = waiting.nextWaiting(waitingIndex)) {
              budget.work(1);
              long parent = waiting.get(waitingIndex);
              int parentProduction = production(parent);
              int parentDot = dot(parent);
              current.add(pack(parentProduction, parentDot + 1, origin(parent)));
            }
          } else {
            for (int waitingIndex = 0; waitingIndex < waiting.size(); waitingIndex++) {
              budget.work(1);
              long parent = waiting.get(waitingIndex);
              int parentProduction = production(parent), parentDot = dot(parent);
              if (parentDot < grammar.productionRhsLength(parentProduction)
                  && grammar.productionRhsSymbol(parentProduction, parentDot) == completed) {
                current.add(pack(parentProduction, parentDot + 1, origin(parent)));
              }
            }
          }
        }
      }
      if (current.size() != 0) furthest = position;
    }

    boolean conforms = false;
    StateSet end = chart[tokens.size()];
    for (int i = 0; i < end.size(); i++) {
      long state = end.get(i); int p = production(state);
      if (origin(state) == 0 && grammar.productionLhs(p) == start && dot(state) == grammar.productionRhsLength(p)) { conforms = true; break; }
    }
    if (!conforms && decisions != null) {
      // Preserve historical invalid-input diagnostics and fail safe against a missed prediction.
      return new M3GrammarRecognizer(grammar, limits, false, false, false).validate(tokens, budget);
    }

    if (!conforms && decisions == null && !nullableReplay) {
      M3GrammarValidation repaired =
          new M3GrammarRecognizer(grammar, limits, false, false, true).validate(tokens, budget);
      if (repaired.conforms()) return repaired;
      // A failed repair does not replace the original unfiltered diagnostic.
    }

    int[] expected = conforms ? new int[0] : expectedTerminals(chart[furthest]);
    boolean expectedEnd = !conforms && expectsEndOfInput(chart[furthest], start);
    boolean accepted = conforms || grammar.strictness().acceptNonConforming();
    return new M3GrammarValidation(
        accepted, conforms, tokens.size(), furthest, expectedEnd, expected);
  }

  private void addPredictions(
      StateSet states,
      int nonTerminal,
      int position,
      M3GrammarTokenDispatch dispatch) {
    if (decisions == null) {
      for (int ordinal = 0; ordinal < grammar.productionCountFor(nonTerminal); ordinal++) {
        states.add(pack(grammar.productionFor(nonTerminal, ordinal), 0, position));
      }
      return;
    }

    if (position == dispatch.tokenCount()) {
      for (int ordinal = 0; ordinal < decisions.eofCandidateCount(nonTerminal); ordinal++) {
        states.add(pack(decisions.eofCandidateProduction(nonTerminal, ordinal), 0, position));
      }
      return;
    }

    for (int terminalOrdinal = 0;
        terminalOrdinal < dispatch.terminalCount(position);
        terminalOrdinal++) {
      int terminal = dispatch.terminalSymbol(position, terminalOrdinal);
      for (int candidate = 0;
          candidate < decisions.candidateProductionCount(nonTerminal, terminal);
          candidate++) {
        states.add(
            pack(decisions.candidateProduction(nonTerminal, terminal, candidate), 0, position));
      }
    }
  }

  private boolean expectsEndOfInput(StateSet states, int startSymbol) {
    for (int index = 0; index < states.size(); index++) {
      long state = states.get(index);
      int production = production(state);
      if (origin(state) == 0
          && grammar.productionLhs(production) == startSymbol
          && dot(state) == grammar.productionRhsLength(production)) {
        return true;
      }
    }
    return false;
  }

  private int[] expectedTerminals(StateSet states) {
    boolean[] seen = new boolean[grammar.symbolCount()]; int count = 0;
    for (int i = 0; i < states.size(); i++) {
      long state = states.get(i); int p = production(state), d = dot(state);
      if (d >= grammar.productionRhsLength(p)) continue;
      int symbol = grammar.productionRhsSymbol(p, d);
      if (!grammar.isNonTerminal(symbol) && !seen[symbol]) { seen[symbol] = true; count++; }
    }
    int[] result = new int[count]; int out = 0;
    for (int s = 0; s < seen.length; s++) if (seen[s]) result[out++] = s;
    return result;
  }

  private static long pack(int production, int dot, int origin) {
    if ((production & ~MAX_24) != 0 || (dot & ~MAX_16) != 0 || (origin & ~MAX_24) != 0) throw new IllegalArgumentException("Earley state coordinate overflow");
    return ((long) production << 40) | ((long) dot << 24) | origin;
  }
  private static int production(long state) { return (int) ((state >>> 40) & MAX_24); }
  private static int dot(long state) { return (int) ((state >>> 24) & MAX_16); }
  private static int origin(long state) { return (int) (state & MAX_24); }

  private static final class StateSet {
    private final M3Grammar grammar;
    private final M3GrammarBudget budget;
    private final M3EarleyWaitingIndex waiting;
    private long[] states = new long[16];
    private int[] slots = new int[32];
    private int size;

    StateSet(M3Grammar grammar, boolean indexedCompletion, M3GrammarBudget budget) {
      this.grammar = grammar;
      this.budget = budget;
      this.waiting = indexedCompletion ? new M3EarleyWaitingIndex(budget) : null;
    }

    int size() { return size; }
    long get(int index) { return states[Objects.checkIndex(index, size)]; }
    int firstWaiting(int symbol) { return waiting == null ? -1 : waiting.first(symbol); }
    int nextWaiting(int stateIndex) { return waiting == null ? -1 : waiting.next(stateIndex); }

    boolean add(long state) {
      budget.work(1);
      int mask = slots.length - 1, slot = mix(state) & mask;
      while (true) {
        budget.work(1);
        int encoded = slots[slot];
        if (encoded == 0) break;
        if (states[encoded - 1] == state) return false;
        slot = (slot + 1) & mask;
      }
      budget.state();
      if ((size + 1L) * 10 >= slots.length * 6L) {
        rehash(slots.length << 1);
        slot = mix(state) & (slots.length - 1);
        while (slots[slot] != 0) { budget.work(1); slot = (slot + 1) & (slots.length - 1); }
      }
      if (size == states.length) {
        states = Arrays.copyOf(states, M3GrammarBudget.capacity(states.length, size + 1, budget.limits().maxStates()));
      }
      int index = size++;
      states[index] = state;
      slots[slot] = index + 1;
      if (waiting != null) {
        int stateProduction = production(state);
        int stateDot = dot(state);
        if (stateDot < grammar.productionRhsLength(stateProduction)) {
          int symbol = grammar.productionRhsSymbol(stateProduction, stateDot);
          if (grammar.isNonTerminal(symbol)) waiting.register(index, symbol);
        }
      }
      return true;
    }
    private void rehash(int capacity) {
      int[] replacement = new int[capacity]; int mask = capacity - 1;
      for (int index = 0; index < size; index++) { budget.work(1); int slot = mix(states[index]) & mask; while (replacement[slot] != 0) { budget.work(1); slot = (slot + 1) & mask; } replacement[slot] = index + 1; }
      slots = replacement;
    }
    private static int mix(long value) { long z = value; z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L; z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL; z ^= z >>> 31; return (int) (z ^ (z >>> 32)); }
  }
}
