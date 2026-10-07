// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.sun.source.tree.Tree;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Dense Aho-Corasick automaton for up to 64 AST-kind patterns.
 *
 * <p>The alphabet is the fixed {@link Tree.Kind} ordinal space. Construction precomputes every
 * transition, failure link and output mask so document scans are a tight array lookup per preorder
 * occurrence.</p>
 */
public final class MIndexASTKindAutomaton {
  private final int alphabetSize;
  private final int stateCount;
  private final int[] transitions;
  private final int[] failures;
  private final long[] outputMasks;
  private final int[] patternLengths;

  private MIndexASTKindAutomaton(
      int alphabetSize,
      int stateCount,
      int[] transitions,
      int[] failures,
      long[] outputMasks,
      int[] patternLengths) {
    this.alphabetSize = alphabetSize;
    this.stateCount = stateCount;
    this.transitions = transitions;
    this.failures = failures;
    this.outputMasks = outputMasks;
    this.patternLengths = patternLengths;
  }

  public static MIndexASTKindAutomaton compile(MIndexASTKindPattern... patterns) {
    Objects.requireNonNull(patterns, "patterns");
    if (patterns.length == 0) throw new IllegalArgumentException("no AST kind patterns");
    if (patterns.length > Long.SIZE) {
      throw new IllegalArgumentException("dense AST kind automaton supports at most 64 patterns");
    }

    int alphabet = Tree.Kind.values().length;
    int maxStates = 1;
    int[] lengths = new int[patterns.length];
    for (int pattern = 0; pattern < patterns.length; pattern++) {
      MIndexASTKindPattern checked = Objects.requireNonNull(patterns[pattern], "pattern");
      lengths[pattern] = checked.length();
      maxStates = Math.addExact(maxStates, checked.length());
    }

    int[] transitions = new int[Math.multiplyExact(maxStates, alphabet)];
    Arrays.fill(transitions, -1);
    int[] failures = new int[maxStates];
    long[] outputs = new long[maxStates];
    int states = 1;

    for (int pattern = 0; pattern < patterns.length; pattern++) {
      MIndexASTKindPattern value = patterns[pattern];
      int state = 0;
      for (int index = 0; index < value.length(); index++) {
        int symbol = Byte.toUnsignedInt(value.ordinalAt(index));
        int offset = state * alphabet + symbol;
        int next = transitions[offset];
        if (next < 0) {
          next = states++;
          transitions[offset] = next;
        }
        state = next;
      }
      outputs[state] |= 1L << pattern;
    }

    int[] queue = new int[states];
    int head = 0;
    int tail = 0;
    for (int symbol = 0; symbol < alphabet; symbol++) {
      int next = transitions[symbol];
      if (next < 0) {
        transitions[symbol] = 0;
      } else if (next != 0) {
        failures[next] = 0;
        queue[tail++] = next;
      }
    }

    while (head < tail) {
      int state = queue[head++];
      int failure = failures[state];
      outputs[state] |= outputs[failure];

      int stateBase = state * alphabet;
      int failureBase = failure * alphabet;
      for (int symbol = 0; symbol < alphabet; symbol++) {
        int next = transitions[stateBase + symbol];
        if (next < 0) {
          transitions[stateBase + symbol] = transitions[failureBase + symbol];
        } else {
          failures[next] = transitions[failureBase + symbol];
          queue[tail++] = next;
        }
      }
    }

    return new MIndexASTKindAutomaton(
        alphabet,
        states,
        Arrays.copyOf(transitions, Math.multiplyExact(states, alphabet)),
        Arrays.copyOf(failures, states),
        Arrays.copyOf(outputs, states),
        lengths);
  }

  public int patternCount() {
    return patternLengths.length;
  }

  public int stateCount() {
    return stateCount;
  }

  int alphabetSizeForNative() {
    return alphabetSize;
  }

  int[] transitionsForNative() {
    return transitions.clone();
  }

  long[] outputMasksForNative() {
    return outputMasks.clone();
  }

  int[] patternLengthsForNative() {
    return patternLengths.clone();
  }

  public List<Match> scanPreorder(MIndexASTPrecompute precompute) {
    Objects.requireNonNull(precompute, "precompute");
    List<Match> matches = new ArrayList<>();
    int state = 0;
    for (int preorder = 0; preorder < precompute.occurrenceCount(); preorder++) {
      int symbol = precompute.preorderKindOrdinal(preorder);
      state = transitions[state * alphabetSize + symbol];
      long mask = outputMasks[state];
      while (mask != 0L) {
        int pattern = Long.numberOfTrailingZeros(mask);
        int startPreorder = preorder - patternLengths[pattern] + 1;
        matches.add(
            new Match(
                pattern,
                startPreorder,
                preorder + 1,
                precompute.preorderOccurrence(startPreorder)));
        mask &= mask - 1L;
      }
    }
    return List.copyOf(matches);
  }

  public long primitivePayloadBytes() {
    return Integer.BYTES
            * (long) (transitions.length + failures.length + patternLengths.length)
        + Long.BYTES * (long) outputMasks.length;
  }

  /**
   * Returns the content identity of the detached transition image.
   *
   * <p>The root binds the alphabet, state geometry and every transition/failure/output lane. It
   * is a semantic image identity only: it contains no host path, class name or execution timing.
   * Batch receipts use it to prevent results from one compiled pattern set being reused for a
   * different automaton.</p>
   */
  public String imageRootSha256() {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update("MINDEX_AST_KIND_AUTOMATON_IMAGE_V1".getBytes(StandardCharsets.UTF_8));
      updateInt(digest, alphabetSize);
      updateInt(digest, stateCount);
      updateInts(digest, transitions);
      updateInts(digest, failures);
      updateLongs(digest, outputMasks);
      updateInts(digest, patternLengths);
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException impossible) {
      throw new ExceptionInInitializerError(impossible);
    }
  }

  private static void updateInts(MessageDigest digest, int[] values) {
    updateInt(digest, values.length);
    for (int value : values) updateInt(digest, value);
  }

  private static void updateLongs(MessageDigest digest, long[] values) {
    updateInt(digest, values.length);
    for (long value : values) {
      digest.update((byte) (value >>> 56));
      digest.update((byte) (value >>> 48));
      digest.update((byte) (value >>> 40));
      digest.update((byte) (value >>> 32));
      digest.update((byte) (value >>> 24));
      digest.update((byte) (value >>> 16));
      digest.update((byte) (value >>> 8));
      digest.update((byte) value);
    }
  }

  private static void updateInt(MessageDigest digest, int value) {
    digest.update((byte) (value >>> 24));
    digest.update((byte) (value >>> 16));
    digest.update((byte) (value >>> 8));
    digest.update((byte) value);
  }

  public record Match(
      int patternIndex,
      int startPreorder,
      int endPreorderExclusive,
      int startOccurrence) {}
}
