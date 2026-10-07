// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable equivalence-class projection of a bounded ASCII DFA.
 *
 * <p>ASCII values with identical transition columns across every DFA state share one compact
 * alphabet class. The mapping is computed once from the existing exact {@link
 * MIndexRegexProgram.AsciiDfaImage}; execution remains exact because every value in one class has
 * the same target state from every source state.</p>
 */
public record MIndexCompactAsciiDfaImage(
    int startState,
    byte[] asciiClassByValue,
    int classCount,
    short[] transitions,
    byte[] accepting) {

  private static final int ASCII_CARDINALITY = 128;

  public MIndexCompactAsciiDfaImage {
    asciiClassByValue =
        Objects.requireNonNull(asciiClassByValue, "asciiClassByValue").clone();
    transitions = Objects.requireNonNull(transitions, "transitions").clone();
    accepting = Objects.requireNonNull(accepting, "accepting").clone();
    if (accepting.length == 0 || startState < 0 || startState >= accepting.length) {
      throw new IllegalArgumentException("invalid compact ASCII DFA state geometry");
    }
    if (asciiClassByValue.length != ASCII_CARDINALITY
        || classCount < 1
        || classCount > ASCII_CARDINALITY) {
      throw new IllegalArgumentException("invalid compact ASCII DFA alphabet geometry");
    }
    boolean[] used = new boolean[classCount];
    for (byte encoded : asciiClassByValue) {
      int id = Byte.toUnsignedInt(encoded);
      if (id >= classCount) {
        throw new IllegalArgumentException("compact ASCII DFA class id out of range");
      }
      used[id] = true;
    }
    for (boolean present : used) {
      if (!present) throw new IllegalArgumentException("compact ASCII DFA class id has no member");
    }
    if (transitions.length != Math.multiplyExact(accepting.length, classCount)) {
      throw new IllegalArgumentException("invalid compact ASCII DFA transition geometry");
    }
    for (short transition : transitions) {
      if (transition < -1 || transition >= accepting.length) {
        throw new IllegalArgumentException("invalid compact ASCII DFA transition target");
      }
    }
    for (byte value : accepting) {
      if (value != 0 && value != 1) {
        throw new IllegalArgumentException("invalid compact ASCII DFA accepting lane");
      }
    }
  }

  @Override
  public byte[] asciiClassByValue() {
    return asciiClassByValue.clone();
  }

  @Override
  public short[] transitions() {
    return transitions.clone();
  }

  @Override
  public byte[] accepting() {
    return accepting.clone();
  }

  public int stateCount() {
    return accepting.length;
  }

  public long primitivePayloadBytes() {
    return asciiClassByValue.length
        + (long) transitions.length * Short.BYTES
        + accepting.length;
  }

  public int nextState(int state, int asciiValue) {
    Objects.checkIndex(state, accepting.length);
    if (asciiValue < 0 || asciiValue >= ASCII_CARDINALITY) {
      throw new IndexOutOfBoundsException("asciiValue=" + asciiValue);
    }
    int symbolClass = Byte.toUnsignedInt(asciiClassByValue[asciiValue]);
    return transitions[state * classCount + symbolClass];
  }

  public String rootHash() {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update("MINDEX_COMPACT_ASCII_DFA_V1".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
      digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(startState).array());
      digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(classCount).array());
      digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(asciiClassByValue.length).array());
      digest.update(asciiClassByValue);
      digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(transitions.length).array());
      for (short transition : transitions) {
        digest.update(ByteBuffer.allocate(Short.BYTES).putShort(transition).array());
      }
      digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(accepting.length).array());
      digest.update(accepting);
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException impossible) {
      throw new ExceptionInInitializerError(impossible);
    }
  }

  public static MIndexCompactAsciiDfaImage from(MIndexRegexProgram.AsciiDfaImage dense) {
    MIndexRegexProgram.AsciiDfaImage checked = Objects.requireNonNull(dense, "dense");
    short[] denseTransitions = checked.transitions();
    byte[] accepting = checked.accepting();
    int states = accepting.length;

    int[] hashes = new int[ASCII_CARDINALITY];
    for (int value = 0; value < ASCII_CARDINALITY; value++) {
      int hash = 1;
      for (int state = 0; state < states; state++) {
        hash = 31 * hash + denseTransitions[state * ASCII_CARDINALITY + value];
      }
      hashes[value] = hash;
    }

    int[] representative = new int[ASCII_CARDINALITY];
    byte[] classes = new byte[ASCII_CARDINALITY];
    int classCount = 0;
    for (int value = 0; value < ASCII_CARDINALITY; value++) {
      int resolved = -1;
      for (int candidate = 0; candidate < classCount; candidate++) {
        int rep = representative[candidate];
        if (hashes[rep] == hashes[value]
            && sameColumn(denseTransitions, states, rep, value)) {
          resolved = candidate;
          break;
        }
      }
      if (resolved < 0) {
        resolved = classCount;
        representative[classCount++] = value;
      }
      classes[value] = (byte) resolved;
    }

    short[] compact = new short[Math.multiplyExact(states, classCount)];
    for (int state = 0; state < states; state++) {
      int denseRow = state * ASCII_CARDINALITY;
      int compactRow = state * classCount;
      for (int symbolClass = 0; symbolClass < classCount; symbolClass++) {
        compact[compactRow + symbolClass] =
            denseTransitions[denseRow + representative[symbolClass]];
      }
    }
    MIndexCompactAsciiDfaImage image =
        new MIndexCompactAsciiDfaImage(
            checked.startState(), classes, classCount, compact, accepting);
    image.requireEquivalent(checked);
    return image;
  }

  public void requireEquivalent(MIndexRegexProgram.AsciiDfaImage dense) {
    MIndexRegexProgram.AsciiDfaImage checked = Objects.requireNonNull(dense, "dense");
    if (startState != checked.startState()
        || !Arrays.equals(accepting, checked.accepting())
        || checked.stateCount() != stateCount()) {
      throw new IllegalArgumentException("compact ASCII DFA identity differs from dense DFA");
    }
    short[] denseTransitions = checked.transitions();
    for (int state = 0; state < stateCount(); state++) {
      for (int value = 0; value < ASCII_CARDINALITY; value++) {
        if (nextState(state, value)
            != denseTransitions[state * ASCII_CARDINALITY + value]) {
          throw new IllegalArgumentException("compact ASCII DFA is not transition-equivalent");
        }
      }
    }
  }

  private static boolean sameColumn(
      short[] transitions, int states, int leftValue, int rightValue) {
    if (leftValue == rightValue) return true;
    for (int state = 0; state < states; state++) {
      int row = state * ASCII_CARDINALITY;
      if (transitions[row + leftValue] != transitions[row + rightValue]) return false;
    }
    return true;
  }
}
