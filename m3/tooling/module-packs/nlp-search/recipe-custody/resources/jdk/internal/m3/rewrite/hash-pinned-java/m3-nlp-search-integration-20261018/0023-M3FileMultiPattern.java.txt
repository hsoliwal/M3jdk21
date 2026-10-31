// Copyright 2026 Hitesh Soliwal and contributors
// Full file-family receiver; source and target mapping in FILE_PORT_MAP.json.
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.util.Arrays;
import java.util.Objects;

/** Primitive Aho-Corasick matcher for many prepared token patterns over one M3File. */
public final class M3FileMultiPattern {
  @FunctionalInterface
  public interface MatchConsumer {
    void accept(int patternIndex, int startToken, int endToken);
  }

  private static final int NONE = -1;

  private final M3File file;
  private final int[] patternLengths;

  private int[] nodeFirstEdge = filled(16, NONE);
  private int[] nodeFailure = new int[16];
  private int[] nodeDictionary = filled(16, NONE);
  private int[] nodeOutputHead = filled(16, NONE);
  private int nodeCount = 1;

  private int[] edgeOwner = new int[32];
  private int[] edgeToken = new int[32];
  private int[] edgeTarget = new int[32];
  private int[] edgeNext = new int[32];
  private int edgeCount;
  private int[] transitionSlots = new int[64];

  private int[] outputPattern = new int[16];
  private int[] outputNext = new int[16];
  private int outputCount;

  private M3FileMultiPattern(M3File file, M3FilePattern[] patterns) {
    this.file = Objects.requireNonNull(file, "file");
    Objects.requireNonNull(patterns, "patterns");
    patternLengths = new int[patterns.length];
    for (int patternIndex = 0; patternIndex < patterns.length; patternIndex++) {
      M3FilePattern pattern =
          Objects.requireNonNull(patterns[patternIndex], "pattern");
      if (pattern.file() != file) {
        throw new IllegalArgumentException("prepared pattern belongs to another file");
      }
      patternLengths[patternIndex] = pattern.length();
      if (pattern.presentInFile()) addPattern(patternIndex, pattern);
    }
    buildFailureLinks();
  }

  public static M3FileMultiPattern compile(
      M3File file, M3FilePattern... patterns) {
    return new M3FileMultiPattern(file, patterns.clone());
  }

  public M3File file() {
    return file;
  }

  public int patternCount() {
    return patternLengths.length;
  }

  /** Emits matches in ascending end-token order; overlapping matches are retained. */
  public void scan(MatchConsumer consumer) {
    Objects.requireNonNull(consumer, "consumer");
    emitOutputs(0, 0, consumer);
    int state = 0;
    for (int position = 0; position < file.tokenCount(); position++) {
      int token = file.localHandleAt(position);
      int next = transition(state, token);
      while (state != 0 && next < 0) {
        state = nodeFailure[state];
        next = transition(state, token);
      }
      state = next < 0 ? 0 : next;
      int endToken = position + 1;
      emitOutputs(state, endToken, consumer);
      for (int outputState = nodeDictionary[state];
          outputState >= 0;
          outputState = nodeDictionary[outputState]) {
        emitOutputs(outputState, endToken, consumer);
      }
      if (state != 0) emitOutputs(0, endToken, consumer);
    }
  }

  public long retainedPayloadBytes() {
    return Integer.BYTES
        * (long)
            (patternLengths.length
                + nodeFirstEdge.length
                + nodeFailure.length
                + nodeDictionary.length
                + nodeOutputHead.length
                + edgeOwner.length
                + edgeToken.length
                + edgeTarget.length
                + edgeNext.length
                + transitionSlots.length
                + outputPattern.length
                + outputNext.length);
  }

  private void addPattern(int patternIndex, M3FilePattern pattern) {
    int state = 0;
    for (int i = 0; i < pattern.length(); i++) {
      int token = pattern.handleAt(i);
      int next = transition(state, token);
      if (next < 0) {
        next = newNode();
        addEdge(state, token, next);
      }
      state = next;
    }
    addOutput(state, patternIndex);
  }

  private void buildFailureLinks() {
    int[] queue = new int[nodeCount];
    int head = 0;
    int tail = 0;
    for (int edge = nodeFirstEdge[0]; edge >= 0; edge = edgeNext[edge]) {
      int child = edgeTarget[edge];
      nodeFailure[child] = 0;
      nodeDictionary[child] = NONE;
      queue[tail++] = child;
    }

    while (head < tail) {
      int state = queue[head++];
      for (int edge = nodeFirstEdge[state]; edge >= 0; edge = edgeNext[edge]) {
        int token = edgeToken[edge];
        int child = edgeTarget[edge];
        int fallback = nodeFailure[state];
        int candidate = transition(fallback, token);
        while (fallback != 0 && candidate < 0) {
          fallback = nodeFailure[fallback];
          candidate = transition(fallback, token);
        }
        if (candidate < 0) candidate = transition(0, token);
        nodeFailure[child] = candidate < 0 ? 0 : candidate;
        int failure = nodeFailure[child];
        nodeDictionary[child] =
            failure != 0 && nodeOutputHead[failure] >= 0
                ? failure
                : nodeDictionary[failure];
        queue[tail++] = child;
      }
    }
  }

  private void emitOutputs(int state, int endToken, MatchConsumer consumer) {
    for (int output = nodeOutputHead[state];
        output >= 0;
        output = outputNext[output]) {
      int pattern = outputPattern[output];
      consumer.accept(pattern, endToken - patternLengths[pattern], endToken);
    }
  }

  private int transition(int node, int token) {
    int mask = transitionSlots.length - 1;
    int slot = mix(node, token) & mask;
    while (true) {
      int encoded = transitionSlots[slot];
      if (encoded == 0) return NONE;
      int edge = encoded - 1;
      if (edgeOwner[edge] == node && edgeToken[edge] == token) {
        return edgeTarget[edge];
      }
      slot = (slot + 1) & mask;
    }
  }

  private void addEdge(int owner, int token, int target) {
    reserveEdges(edgeCount + 1);
    reserveTransitionTable(edgeCount + 1);
    int edge = edgeCount++;
    edgeOwner[edge] = owner;
    edgeToken[edge] = token;
    edgeTarget[edge] = target;
    edgeNext[edge] = nodeFirstEdge[owner];
    nodeFirstEdge[owner] = edge;

    int mask = transitionSlots.length - 1;
    int slot = mix(owner, token) & mask;
    while (transitionSlots[slot] != 0) slot = (slot + 1) & mask;
    transitionSlots[slot] = edge + 1;
  }

  private int newNode() {
    reserveNodes(nodeCount + 1);
    int node = nodeCount++;
    nodeFirstEdge[node] = NONE;
    nodeDictionary[node] = NONE;
    nodeOutputHead[node] = NONE;
    return node;
  }

  private void addOutput(int node, int patternIndex) {
    if (outputCount == outputPattern.length) {
      int next = grownLength(outputPattern.length, outputCount + 1);
      outputPattern = Arrays.copyOf(outputPattern, next);
      outputNext = Arrays.copyOf(outputNext, next);
    }
    int output = outputCount++;
    outputPattern[output] = patternIndex;
    outputNext[output] = nodeOutputHead[node];
    nodeOutputHead[node] = output;
  }

  private void reserveNodes(int required) {
    if (required <= nodeFirstEdge.length) return;
    int old = nodeFirstEdge.length;
    int next = grownLength(old, required);
    nodeFirstEdge = growFilled(nodeFirstEdge, next, NONE);
    nodeFailure = Arrays.copyOf(nodeFailure, next);
    nodeDictionary = growFilled(nodeDictionary, next, NONE);
    nodeOutputHead = growFilled(nodeOutputHead, next, NONE);
  }

  private void reserveEdges(int required) {
    if (required <= edgeToken.length) return;
    int next = grownLength(edgeToken.length, required);
    edgeOwner = Arrays.copyOf(edgeOwner, next);
    edgeToken = Arrays.copyOf(edgeToken, next);
    edgeTarget = Arrays.copyOf(edgeTarget, next);
    edgeNext = Arrays.copyOf(edgeNext, next);
  }

  private void reserveTransitionTable(int expectedEdges) {
    if (expectedEdges * 10 < transitionSlots.length * 6) return;
    int[] previous = transitionSlots;
    transitionSlots = new int[previous.length << 1];
    int mask = transitionSlots.length - 1;
    for (int owner = 0; owner < nodeCount; owner++) {
      for (int edge = nodeFirstEdge[owner]; edge >= 0; edge = edgeNext[edge]) {
        int slot = mix(owner, edgeToken[edge]) & mask;
        while (transitionSlots[slot] != 0) slot = (slot + 1) & mask;
        transitionSlots[slot] = edge + 1;
      }
    }
  }

  private static int mix(int node, int token) {
    long value =
        (Integer.toUnsignedLong(node) << 32) | Integer.toUnsignedLong(token);
    value ^= value >>> 33;
    value *= 0xff51afd7ed558ccdL;
    value ^= value >>> 33;
    value *= 0xc4ceb9fe1a85ec53L;
    value ^= value >>> 33;
    return (int) value;
  }

  private static int grownLength(int oldLength, int required) {
    return Math.max(required, oldLength + (oldLength >>> 1) + 1);
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
