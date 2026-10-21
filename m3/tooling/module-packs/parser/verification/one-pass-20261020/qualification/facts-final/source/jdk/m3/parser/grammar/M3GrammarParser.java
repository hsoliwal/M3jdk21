// Copyright 2026 Hitesh Soliwal and contributors
// Adapted from hsoliwal/com.synexia@50f48cbbe11f17fb8dc209bac3812f8940a1e267: MIndexGrammarParser.java
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.grammar;

import java.util.Arrays;
import java.util.Objects;

/**
 * Earley parser retaining one deterministic derivation through primitive backpointer lanes.
 *
 * <p>Use {@link M3GrammarRecognizer} when only conformance is needed; this class pays the
 * additional backpointer cost only when a parse tree is requested.</p>
 */
public final class M3GrammarParser {
  private static final int MAX_24 = 0x00ff_ffff;
  private static final int MAX_16 = 0x0000_ffff;
  private static final byte CHILD_NONE = 0;
  private static final byte CHILD_TERMINAL = 1;
  private static final byte CHILD_NON_TERMINAL = 2;

  private final M3Grammar grammar;
  private final M3GrammarDecisionIndex decisions;
  private final boolean indexedCompletion;
  private final boolean nullableReplay;
  private final M3GrammarLimits limits;

  public M3GrammarParser(M3Grammar grammar) {
    this(grammar, M3GrammarLimits.defaults());
  }

  public M3GrammarParser(M3Grammar grammar, M3GrammarLimits limits) {
    this(grammar, limits, true, true, false);
  }

  M3GrammarParser(M3Grammar grammar, boolean optimized) {
    this(grammar, M3GrammarLimits.defaults(), optimized, optimized, false);
  }

  M3GrammarParser(M3Grammar grammar, boolean predictiveDispatch, boolean indexedCompletion) {
    this(grammar, M3GrammarLimits.defaults(), predictiveDispatch, indexedCompletion, false);
  }

  private M3GrammarParser(M3Grammar grammar, M3GrammarLimits limits,
      boolean predictiveDispatch, boolean indexedCompletion, boolean nullableReplay) {
    this.grammar = Objects.requireNonNull(grammar, "grammar");
    this.limits = Objects.requireNonNull(limits, "limits");
    this.decisions = predictiveDispatch ? grammar.decisionIndex() : null;
    this.indexedCompletion = indexedCompletion;
    this.nullableReplay = nullableReplay;
  }

  public M3GrammarLimits limits() { return limits; }

  public M3Grammar grammar() { return grammar; }

  public M3GrammarParseResult parse(CharSequence source) {
    M3GrammarBudget budget = new M3GrammarBudget(limits);
    M3TokenStream tokens = new M3GrammarLexer(grammar.lexerProfile(), limits).lex(source, budget);
    return parse(tokens, budget);
  }

  public M3GrammarParseResult parse(M3TokenStream tokens) {
    return parse(tokens, new M3GrammarBudget(limits));
  }

  private M3GrammarParseResult parse(M3TokenStream tokens, M3GrammarBudget budget) {
    Objects.requireNonNull(tokens, "tokens");
    budget.source(tokens.source().length());
    budget.tokens(tokens.size());
    int minimum = grammar.minimumTokens(grammar.startSymbol());
    if (minimum >= 0 && tokens.size() < minimum) {
      M3GrammarValidation validation =
          new M3GrammarValidation(
              grammar.strictness().acceptNonConforming(),
              false,
              tokens.size(),
              0,
              false,
              grammar.firstSymbols(grammar.startSymbol()));
      return new M3GrammarParseResult(validation, null);
    }
    requirePackedBounds(tokens);

    M3GrammarTokenDispatch dispatch = decisions == null ? null : decisions.dispatch(tokens, budget);
    BackStateSet[] chart = new BackStateSet[tokens.size() + 1];
    for (int i = 0; i < chart.length; i++) {
      chart[i] = new BackStateSet(grammar, indexedCompletion, budget);
    }

    int start = grammar.startSymbol();
    addPredictions(chart[0], start, 0, dispatch);

    int furthest = 0;
    for (int position = 0; position <= tokens.size(); position++) {
      BackStateSet current = chart[position];
      for (int stateIndex = 0; stateIndex < current.size(); stateIndex++) {
        budget.work(1);
        long state = current.state(stateIndex);
        int production = production(state);
        int dot = dot(state);
        int origin = origin(state);
        int rhsLength = grammar.productionRhsLength(production);

        if (dot < rhsLength) {
          int symbol = grammar.productionRhsSymbol(production, dot);
          if (grammar.isNonTerminal(symbol)) {
            addPredictions(current, symbol, position, dispatch);
            if (nullableReplay && grammar.nullable(symbol)) {
              // Only old completions need replay; later entries run their normal completer.
              for (int completeIndex = 0; completeIndex < stateIndex; completeIndex++) {
                budget.work(1);
                long completedState = current.state(completeIndex);
                int completedProduction = production(completedState);
                if (origin(completedState) == position
                    && grammar.productionLhs(completedProduction) == symbol
                    && dot(completedState) == grammar.productionRhsLength(completedProduction)) {
                  current.add(pack(production, dot + 1, origin), position, stateIndex,
                      CHILD_NON_TERMINAL, position, completeIndex);
                }
              }
            }
          } else if (position < tokens.size()
              && budget.matches(grammar, symbol, tokens, position)) {
            chart[position + 1].add(
                pack(production, dot + 1, origin),
                position,
                stateIndex,
                CHILD_TERMINAL,
                position,
                -1);
          }
        } else {
          int completed = grammar.productionLhs(production);
          BackStateSet waiting = chart[origin];
          if (indexedCompletion) {
            for (int waitingIndex = waiting.firstWaiting(completed);
                waitingIndex >= 0;
                waitingIndex = waiting.nextWaiting(waitingIndex)) {
              budget.work(1);
              long parent = waiting.state(waitingIndex);
              int parentProduction = production(parent);
              int parentDot = dot(parent);
              current.add(
                  pack(parentProduction, parentDot + 1, origin(parent)),
                  origin,
                  waitingIndex,
                  CHILD_NON_TERMINAL,
                  position,
                  stateIndex);
            }
          } else {
            for (int waitingIndex = 0; waitingIndex < waiting.size(); waitingIndex++) {
              budget.work(1);
              long parent = waiting.state(waitingIndex);
              int parentProduction = production(parent);
              int parentDot = dot(parent);
              if (parentDot < grammar.productionRhsLength(parentProduction)
                  && grammar.productionRhsSymbol(parentProduction, parentDot) == completed) {
                current.add(
                    pack(parentProduction, parentDot + 1, origin(parent)),
                    origin,
                    waitingIndex,
                    CHILD_NON_TERMINAL,
                    position,
                    stateIndex);
              }
            }
          }
        }
      }
      if (current.size() != 0) furthest = position;
    }

    int acceptingState = -1;
    BackStateSet end = chart[tokens.size()];
    for (int i = 0; i < end.size(); i++) {
      long state = end.state(i);
      int production = production(state);
      if (origin(state) == 0
          && grammar.productionLhs(production) == start
          && dot(state) == grammar.productionRhsLength(production)) {
        acceptingState = i;
        break;
      }
    }

    boolean conforms = acceptingState >= 0;
    if (!conforms && decisions != null) {
      // Preserve historical invalid-input diagnostics and fail safe against a missed prediction.
      return new M3GrammarParser(grammar, limits, false, false, false).parse(tokens, budget);
    }

    if (!conforms && decisions == null && !nullableReplay) {
      M3GrammarParseResult repaired =
          new M3GrammarParser(grammar, limits, false, false, true).parse(tokens, budget);
      if (repaired.conforms()) return repaired;
      // A failed repair does not replace the original unfiltered diagnostic.
    }

    int[] expected = conforms ? new int[0] : expectedTerminals(chart[furthest]);
    boolean expectedEnd = !conforms && expectsEndOfInput(chart[furthest], start);
    M3GrammarValidation validation =
        new M3GrammarValidation(
            conforms || grammar.strictness().acceptNonConforming(),
            conforms,
            tokens.size(),
            furthest,
            expectedEnd,
            expected);
    if (!conforms) return new M3GrammarParseResult(validation, null);

    TreeBuilder builder = new TreeBuilder(grammar, tokens, budget);
    int root =
        buildCompleted(
            chart,
            tokens.size(),
            acceptingState,
            builder,
            new long[Math.min(16, limits.maxTreeDepth())],
            0);
    return new M3GrammarParseResult(validation, builder.build(root));
  }

  private void addPredictions(
      BackStateSet states,
      int nonTerminal,
      int position,
      M3GrammarTokenDispatch dispatch) {
    if (decisions == null) {
      addAllPredictions(states, nonTerminal, position);
      return;
    }

    int selected =
        position == dispatch.tokenCount()
            ? decisions.uniqueEofCandidateProduction(nonTerminal)
            : decisions.uniqueCandidateProduction(nonTerminal, dispatch, position);

    if (selected >= 0) {
      states.add(
          pack(selected, 0, position),
          -1,
          -1,
          CHILD_NONE,
          -1,
          -1);
    } else if (selected == M3GrammarDecisionIndex.MULTIPLE_CANDIDATES) {
      // Preserve historical production-order ambiguity behavior.
      addAllPredictions(states, nonTerminal, position);
    }
  }

  private void addAllPredictions(BackStateSet states, int nonTerminal, int position) {
    for (int ordinal = 0; ordinal < grammar.productionCountFor(nonTerminal); ordinal++) {
      states.add(
          pack(grammar.productionFor(nonTerminal, ordinal), 0, position),
          -1,
          -1,
          CHILD_NONE,
          -1,
          -1);
    }
  }

  private int buildCompleted(
      BackStateSet[] chart,
      int chartPosition,
      int stateIndex,
      TreeBuilder builder,
      long[] active,
      int activeSize) {
    builder.budget.depth(activeSize + 1);
    builder.budget.work(1L + activeSize);
    long activeKey = ((long) chartPosition << Integer.SIZE) | Integer.toUnsignedLong(stateIndex);
    for (int i = 0; i < activeSize; i++) {
      if (active[i] == activeKey) {
        throw new M3GrammarException("cyclic nullable derivation while building parse tree");
      }
    }
    if (activeSize == active.length) active = Arrays.copyOf(active, Math.min(limits.maxTreeDepth(), active.length << 1));
    active[activeSize++] = activeKey;

    BackStateSet set = chart[chartPosition];
    long completedState = set.state(stateIndex);
    int production = production(completedState);
    int completedDot = dot(completedState);
    if (completedDot != grammar.productionRhsLength(production)) {
      throw new M3GrammarException("parse-tree backpointer does not reference a completed state");
    }

    builder.budget.children(completedDot);
    int[] children = new int[completedDot];
    int childWrite = completedDot - 1;
    int currentChart = chartPosition;
    int currentStateIndex = stateIndex;

    while (childWrite >= 0) {
      BackStateSet currentSet = chart[currentChart];
      long currentState = currentSet.state(currentStateIndex);
      int currentProduction = production(currentState);
      int currentDot = dot(currentState);
      if (currentProduction != production || currentDot != childWrite + 1) {
        throw new M3GrammarException("broken Earley derivation chain");
      }

      byte childKind = currentSet.childKind(currentStateIndex);
      if (childKind == CHILD_TERMINAL) {
        builder.budget.depth(activeSize + 1);
        int token = currentSet.childA(currentStateIndex);
        int symbol = grammar.productionRhsSymbol(production, childWrite);
        children[childWrite] = builder.terminal(symbol, token);
      } else if (childKind == CHILD_NON_TERMINAL) {
        children[childWrite] =
            buildCompleted(
                chart,
                currentSet.childA(currentStateIndex),
                currentSet.childB(currentStateIndex),
                builder,
                active,
                activeSize);
      } else {
        throw new M3GrammarException("missing Earley backpointer");
      }

      int previousChart = currentSet.previousChart(currentStateIndex);
      int previousState = currentSet.previousState(currentStateIndex);
      currentChart = previousChart;
      currentStateIndex = previousState;
      childWrite--;
    }

    int lhs = grammar.productionLhs(production);
    return builder.nonTerminal(
        lhs, production, origin(completedState), chartPosition, children);
  }

  private boolean expectsEndOfInput(BackStateSet states, int startSymbol) {
    for (int index = 0; index < states.size(); index++) {
      long state = states.state(index);
      int production = production(state);
      if (origin(state) == 0
          && grammar.productionLhs(production) == startSymbol
          && dot(state) == grammar.productionRhsLength(production)) {
        return true;
      }
    }
    return false;
  }

  private int[] expectedTerminals(BackStateSet states) {
    boolean[] seen = new boolean[grammar.symbolCount()];
    int count = 0;
    for (int i = 0; i < states.size(); i++) {
      long state = states.state(i);
      int production = production(state);
      int dot = dot(state);
      if (dot >= grammar.productionRhsLength(production)) continue;
      int symbol = grammar.productionRhsSymbol(production, dot);
      if (!grammar.isNonTerminal(symbol) && !seen[symbol]) {
        seen[symbol] = true;
        count++;
      }
    }
    int[] result = new int[count];
    int out = 0;
    for (int symbol = 0; symbol < seen.length; symbol++) {
      if (seen[symbol]) result[out++] = symbol;
    }
    return result;
  }

  private void requirePackedBounds(M3TokenStream tokens) {
    if (grammar.productionCount() > MAX_24) {
      throw new M3GrammarException("too many productions for packed Earley state");
    }
    if (tokens.size() > MAX_24) {
      throw new M3GrammarException("too many tokens for packed Earley origin");
    }
    for (int production = 0; production < grammar.productionCount(); production++) {
      if (grammar.productionRhsLength(production) > MAX_16) {
        throw new M3GrammarException("production RHS too long for packed Earley state");
      }
    }
  }

  private static long pack(int production, int dot, int origin) {
    if ((production & ~MAX_24) != 0
        || (dot & ~MAX_16) != 0
        || (origin & ~MAX_24) != 0) {
      throw new IllegalArgumentException("Earley state coordinate overflow");
    }
    return ((long) production << 40) | ((long) dot << 24) | origin;
  }

  private static int production(long state) {
    return (int) ((state >>> 40) & MAX_24);
  }

  private static int dot(long state) {
    return (int) ((state >>> 24) & MAX_16);
  }

  private static int origin(long state) {
    return (int) (state & MAX_24);
  }

  private static final class BackStateSet {
    private final M3Grammar grammar;
    private final M3GrammarBudget budget;
    private final M3EarleyWaitingIndex waiting;
    private long[] states = new long[16];
    private int[] previousChart = filled(16, -1);
    private int[] previousState = filled(16, -1);
    private byte[] childKinds = new byte[16];
    private int[] childA = filled(16, -1);
    private int[] childB = filled(16, -1);
    private int[] slots = new int[32];
    private int size;

    BackStateSet(M3Grammar grammar, boolean indexedCompletion, M3GrammarBudget budget) {
      this.grammar = grammar;
      this.budget = budget;
      this.waiting = indexedCompletion ? new M3EarleyWaitingIndex(budget) : null;
    }

    int size() { return size; }
    long state(int index) { return states[Objects.checkIndex(index, size)]; }
    int previousChart(int index) { return previousChart[Objects.checkIndex(index, size)]; }
    int previousState(int index) { return previousState[Objects.checkIndex(index, size)]; }
    byte childKind(int index) { return childKinds[Objects.checkIndex(index, size)]; }
    int childA(int index) { return childA[Objects.checkIndex(index, size)]; }
    int childB(int index) { return childB[Objects.checkIndex(index, size)]; }
    int firstWaiting(int symbol) { return waiting == null ? -1 : waiting.first(symbol); }
    int nextWaiting(int stateIndex) { return waiting == null ? -1 : waiting.next(stateIndex); }

    int add(
        long state,
        int previousChartValue,
        int previousStateValue,
        byte childKind,
        int childAValue,
        int childBValue) {
      budget.work(1);
      int mask = slots.length - 1;
      int slot = mix(state) & mask;
      while (true) {
        budget.work(1);
        int encoded = slots[slot];
        if (encoded == 0) break;
        int existing = encoded - 1;
        if (states[existing] == state) return existing;
        slot = (slot + 1) & mask;
      }

      budget.state();
      if ((size + 1L) * 10 >= slots.length * 6L) {
        rehash(slots.length << 1);
        slot = mix(state) & (slots.length - 1);
        while (slots[slot] != 0) { budget.work(1); slot = (slot + 1) & (slots.length - 1); }
      }
      ensure(size + 1);
      int index = size++;
      states[index] = state;
      previousChart[index] = previousChartValue;
      previousState[index] = previousStateValue;
      childKinds[index] = childKind;
      childA[index] = childAValue;
      childB[index] = childBValue;
      slots[slot] = index + 1;
      if (waiting != null) {
        int stateProduction = production(state);
        int stateDot = dot(state);
        if (stateDot < grammar.productionRhsLength(stateProduction)) {
          int symbol = grammar.productionRhsSymbol(stateProduction, stateDot);
          if (grammar.isNonTerminal(symbol)) waiting.register(index, symbol);
        }
      }
      return index;
    }

    private void ensure(int required) {
      if (required <= states.length) return;
      int old = states.length;
      int next = M3GrammarBudget.capacity(old, required, budget.limits().maxStates());
      states = Arrays.copyOf(states, next);
      previousChart = growFilled(previousChart, next, -1);
      previousState = growFilled(previousState, next, -1);
      childKinds = Arrays.copyOf(childKinds, next);
      childA = growFilled(childA, next, -1);
      childB = growFilled(childB, next, -1);
    }

    private void rehash(int capacity) {
      int[] replacement = new int[capacity];
      int mask = capacity - 1;
      for (int index = 0; index < size; index++) {
        budget.work(1);
        int slot = mix(states[index]) & mask;
        while (replacement[slot] != 0) { budget.work(1); slot = (slot + 1) & mask; }
        replacement[slot] = index + 1;
      }
      slots = replacement;
    }

    private static int[] filled(int size, int value) {
      int[] result = new int[size];
      Arrays.fill(result, value);
      return result;
    }

    private static int[] growFilled(int[] source, int size, int value) {
      int old = source.length;
      int[] result = Arrays.copyOf(source, size);
      Arrays.fill(result, old, size, value);
      return result;
    }

    private static int mix(long value) {
      long z = value;
      z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
      z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
      z ^= z >>> 31;
      return (int) (z ^ (z >>> 32));
    }
  }

  private static final class TreeBuilder {
    private final M3Grammar grammar;
    private final M3GrammarBudget budget;
    private final M3TokenStream tokens;
    private int[] symbols = new int[32];
    private int[] productions = filled(32, -1);
    private int[] tokenIndices = filled(32, -1);
    private int[] fromTokens = new int[32];
    private int[] toTokens = new int[32];
    private int[] childStarts = new int[32];
    private int[] childCounts = new int[32];
    private int[] childArena = new int[64];
    private int nodeSize;
    private int childSize;

    TreeBuilder(M3Grammar grammar, M3TokenStream tokens, M3GrammarBudget budget) {
      this.grammar = grammar;
      this.tokens = tokens;
      this.budget = budget;
    }

    int terminal(int symbol, int token) {
      Objects.checkIndex(token, tokens.size());
      int node = reserveNode();
      symbols[node] = symbol;
      tokenIndices[node] = token;
      fromTokens[node] = token;
      toTokens[node] = token + 1;
      return node;
    }

    int nonTerminal(int symbol, int production, int from, int to, int[] children) {
      int node = reserveNode();
      symbols[node] = symbol;
      productions[node] = production;
      fromTokens[node] = from;
      toTokens[node] = to;
      childStarts[node] = childSize;
      childCounts[node] = children.length;
      reserveChildren(Math.addExact(childSize, children.length));
      System.arraycopy(children, 0, childArena, childSize, children.length);
      childSize += children.length;
      return node;
    }

    M3GrammarParseTree build(int root) {
      return new M3GrammarParseTree(
          grammar,
          tokens,
          root,
          Arrays.copyOf(symbols, nodeSize),
          Arrays.copyOf(productions, nodeSize),
          Arrays.copyOf(tokenIndices, nodeSize),
          Arrays.copyOf(fromTokens, nodeSize),
          Arrays.copyOf(toTokens, nodeSize),
          Arrays.copyOf(childStarts, nodeSize),
          Arrays.copyOf(childCounts, nodeSize),
          Arrays.copyOf(childArena, childSize));
    }

    private int reserveNode() {
      budget.node();
      if (nodeSize == symbols.length) {
        int old = symbols.length;
        int next = M3GrammarBudget.capacity(old, nodeSize + 1, budget.limits().maxTreeNodes());
        symbols = Arrays.copyOf(symbols, next);
        productions = growFilled(productions, next, -1);
        tokenIndices = growFilled(tokenIndices, next, -1);
        fromTokens = Arrays.copyOf(fromTokens, next);
        toTokens = Arrays.copyOf(toTokens, next);
        childStarts = Arrays.copyOf(childStarts, next);
        childCounts = Arrays.copyOf(childCounts, next);
      }
      return nodeSize++;
    }

    private void reserveChildren(int required) {
      if (required <= childArena.length) return;
      childArena = Arrays.copyOf(
          childArena, M3GrammarBudget.capacity(childArena.length, required, budget.limits().maxTreeNodes()));
    }

    private static int[] filled(int size, int value) {
      int[] result = new int[size];
      Arrays.fill(result, value);
      return result;
    }

    private static int[] growFilled(int[] source, int size, int value) {
      int old = source.length;
      int[] result = Arrays.copyOf(source, size);
      Arrays.fill(result, old, size, value);
      return result;
    }
  }
}
