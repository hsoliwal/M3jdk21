/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 */
package jdk.internal.mindex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Synexia-owned minimal acyclic UTF-16 term automaton.
 *
 * <p>The design borrows the mechanical idea of a sorted/minimized term automaton/FST, but this is
 * an independent implementation with no Lucene runtime dependency or Lucene types in its API.
 * States are immutable primitive arrays. Equivalent suffix states are folded deterministically.</p>
 */
public final class M3TermFst {
  private static final int MAX_TERM_UNITS = 4096;

  private final int rootState;
  private final int[] offsets;
  private final char[] labels;
  private final int[] targets;
  private final boolean[] terminal;
  private final long[] outputs;
  private final int termCount;
  private final String rootHash;

  private M3TermFst(
      int rootState,
      int[] offsets,
      char[] labels,
      int[] targets,
      boolean[] terminal,
      long[] outputs,
      int termCount) {
    this.rootState = rootState;
    this.offsets = offsets;
    this.labels = labels;
    this.targets = targets;
    this.terminal = terminal;
    this.outputs = outputs;
    this.termCount = termCount;

    long[] longs = new long[outputs.length + targets.length + offsets.length + labels.length + 2];
    int cursor = 0;
    longs[cursor++] = Integer.toUnsignedLong(rootState);
    longs[cursor++] = Integer.toUnsignedLong(termCount);
    for (int value : offsets) longs[cursor++] = Integer.toUnsignedLong(value);
    for (char value : labels) longs[cursor++] = value;
    for (int value : targets) longs[cursor++] = Integer.toUnsignedLong(value);
    for (long value : outputs) longs[cursor++] = value;
    int[] flags = new int[terminal.length];
    for (int i = 0; i < terminal.length; i++) flags[i] = terminal[i] ? 1 : 0;
    this.rootHash = M3PrecomputeHash.sha256("SYNEXIA_TERM_FST_V1", longs, flags);
  }

  /** Freeze a deterministic dictionary. Input ordering does not affect the resulting image. */
  public static M3TermFst freeze(Map<String, Long> terms) {
    Objects.requireNonNull(terms, "terms");
    TreeMap<String, Long> ordered = new TreeMap<>();
    for (Map.Entry<String, Long> entry : terms.entrySet()) {
      String term = Objects.requireNonNull(entry.getKey(), "term");
      Long output = Objects.requireNonNull(entry.getValue(), "output");
      if (term.length() > MAX_TERM_UNITS) {
        throw new IllegalArgumentException("term exceeds UTF-16 unit bound: " + term.length());
      }
      Long previous = ordered.putIfAbsent(term, output);
      if (previous != null && previous.longValue() != output.longValue()) {
        throw new IllegalArgumentException("duplicate term with different output: " + term);
      }
    }

    MutableNode root = new MutableNode();
    for (Map.Entry<String, Long> entry : ordered.entrySet()) {
      MutableNode node = root;
      String term = entry.getKey();
      for (int index = 0; index < term.length(); index++) {
        node = node.children.computeIfAbsent(term.charAt(index), ignored -> new MutableNode());
      }
      node.terminal = true;
      node.output = entry.getValue();
    }

    HashMap<NodeKey, Integer> canonical = new HashMap<>();
    ArrayList<FrozenNode> nodes = new ArrayList<>();
    int rootState = minimize(root, canonical, nodes);

    int transitions = nodes.stream().mapToInt(node -> node.labels.length).sum();
    int[] offsets = new int[nodes.size() + 1];
    char[] labels = new char[transitions];
    int[] targets = new int[transitions];
    boolean[] terminal = new boolean[nodes.size()];
    long[] outputs = new long[nodes.size()];

    int edge = 0;
    for (int state = 0; state < nodes.size(); state++) {
      FrozenNode node = nodes.get(state);
      offsets[state] = edge;
      terminal[state] = node.terminal;
      outputs[state] = node.output;
      System.arraycopy(node.labels, 0, labels, edge, node.labels.length);
      System.arraycopy(node.targets, 0, targets, edge, node.targets.length);
      edge += node.labels.length;
    }
    offsets[nodes.size()] = edge;

    return new M3TermFst(
        rootState, offsets, labels, targets, terminal, outputs, ordered.size());
  }

  public int stateCount() { return terminal.length; }
  public int transitionCount() { return labels.length; }
  public int termCount() { return termCount; }
  public String rootHash() { return rootHash; }

  public long retainedBytes() {
    return (long) offsets.length * Integer.BYTES
        + (long) labels.length * Character.BYTES
        + (long) targets.length * Integer.BYTES
        + (long) terminal.length
        + (long) outputs.length * Long.BYTES;
  }

  public boolean contains(CharSequence term) {
    return lookup(term, Long.MIN_VALUE) != Long.MIN_VALUE;
  }

  public long lookup(CharSequence term, long defaultValue) {
    Objects.requireNonNull(term, "term");
    int state = rootState;
    for (int index = 0; index < term.length(); index++) {
      state = transition(state, term.charAt(index));
      if (state < 0) return defaultValue;
    }
    return terminal[state] ? outputs[state] : defaultValue;
  }

  /** Number of UTF-16 units in the longest dictionary term that prefixes {@code input}. */
  public int longestPrefixLength(CharSequence input) {
    Objects.requireNonNull(input, "input");
    int state = rootState;
    int best = terminal[state] ? 0 : -1;
    for (int index = 0; index < input.length(); index++) {
      state = transition(state, input.charAt(index));
      if (state < 0) break;
      if (terminal[state]) best = index + 1;
    }
    return best;
  }

  public long longestPrefixOutput(CharSequence input, long defaultValue) {
    Objects.requireNonNull(input, "input");
    int state = rootState;
    long best = terminal[state] ? outputs[state] : defaultValue;
    for (int index = 0; index < input.length(); index++) {
      state = transition(state, input.charAt(index));
      if (state < 0) break;
      if (terminal[state]) best = outputs[state];
    }
    return best;
  }

  /** True when at least one stored term begins with {@code prefix}. */
  public boolean hasPrefix(CharSequence prefix) {
    Objects.requireNonNull(prefix, "prefix");
    int state = rootState;
    for (int index = 0; index < prefix.length(); index++) {
      state = transition(state, prefix.charAt(index));
      if (state < 0) return false;
    }
    return terminal[state] || offsets[state] != offsets[state + 1];
  }

  private int transition(int state, char label) {
    int low = offsets[state];
    int high = offsets[state + 1] - 1;
    while (low <= high) {
      int middle = (low + high) >>> 1;
      char candidate = labels[middle];
      if (candidate == label) return targets[middle];
      if (candidate < label) low = middle + 1;
      else high = middle - 1;
    }
    return -1;
  }

  private static int minimize(
      MutableNode node,
      Map<NodeKey, Integer> canonical,
      List<FrozenNode> nodes) {
    int size = node.children.size();
    char[] labels = new char[size];
    int[] targets = new int[size];
    int index = 0;
    for (Map.Entry<Character, MutableNode> entry : node.children.entrySet()) {
      labels[index] = entry.getKey();
      targets[index] = minimize(entry.getValue(), canonical, nodes);
      index++;
    }

    NodeKey key = new NodeKey(node.terminal, node.output, labels, targets);
    Integer existing = canonical.get(key);
    if (existing != null) return existing;

    int state = nodes.size();
    nodes.add(new FrozenNode(node.terminal, node.output, labels, targets));
    canonical.put(key, state);
    return state;
  }

  private static final class MutableNode {
    final TreeMap<Character, MutableNode> children = new TreeMap<>();
    boolean terminal;
    long output;
  }

  private record FrozenNode(boolean terminal, long output, char[] labels, int[] targets) {}

  private static final class NodeKey {
    private final boolean terminal;
    private final long output;
    private final char[] labels;
    private final int[] targets;
    private final int hash;

    NodeKey(boolean terminal, long output, char[] labels, int[] targets) {
      this.terminal = terminal;
      this.output = output;
      this.labels = labels.clone();
      this.targets = targets.clone();
      int result = Boolean.hashCode(terminal);
      result = 31 * result + Long.hashCode(output);
      result = 31 * result + Arrays.hashCode(this.labels);
      result = 31 * result + Arrays.hashCode(this.targets);
      this.hash = result;
    }

    @Override public int hashCode() { return hash; }

    @Override
    public boolean equals(Object other) {
      if (this == other) return true;
      if (!(other instanceof NodeKey that)) return false;
      return terminal == that.terminal
          && output == that.output
          && Arrays.equals(labels, that.labels)
          && Arrays.equals(targets, that.targets);
    }
  }
}
