// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Frozen native regex image intake. The existing compiler owns production of these lanes. */
public final class M3FsRegexImage {
  public enum Semantics { RE2, JDK }
  private static final byte OP_CHAR = 1, OP_SPLIT = 2, OP_JUMP = 3,
      OP_ASSERT_START = 4, OP_ASSERT_END = 5, OP_MATCH = 6,
      OP_ASSERT_WORD_BOUNDARY = 7, OP_ASSERT_NOT_WORD_BOUNDARY = 8;
  private final Image image;
  public M3FsRegexImage(Image image) { this.image = Objects.requireNonNull(image, "image"); }
  public static M3FsRegexImage fromImage(Image image) { return new M3FsRegexImage(image); }
  public Image image() { return image; }
  public String expression() { return image.expression(); }
  public Semantics semantics() { return image.semantics(); }
  public int captureCount() { return image.captureCount(); }
  public record Image(
      String expression,
      Semantics semantics,
      byte[] op,
      int[] out1,
      int[] out2,
      int[] classId,
      long[] asciiLow,
      long[] asciiHigh,
      int[] rangeOffset,
      int[] rangeCount,
      char[] rangeLow,
      char[] rangeHigh,
      int startState,
      int matchState,
      int minLength,
      int maxLength,
      int captureCount,
      long requiredPresence64,
      int requiredCharacterFlags,
      boolean mayConsumeSurrogateUnit) {

    public Image {
      expression = Objects.requireNonNull(expression, "expression");
      semantics = Objects.requireNonNull(semantics, "semantics");
      op = Objects.requireNonNull(op, "op").clone();
      out1 = Objects.requireNonNull(out1, "out1").clone();
      out2 = Objects.requireNonNull(out2, "out2").clone();
      classId = Objects.requireNonNull(classId, "classId").clone();
      asciiLow = Objects.requireNonNull(asciiLow, "asciiLow").clone();
      asciiHigh = Objects.requireNonNull(asciiHigh, "asciiHigh").clone();
      rangeOffset = Objects.requireNonNull(rangeOffset, "rangeOffset").clone();
      rangeCount = Objects.requireNonNull(rangeCount, "rangeCount").clone();
      rangeLow = Objects.requireNonNull(rangeLow, "rangeLow").clone();
      rangeHigh = Objects.requireNonNull(rangeHigh, "rangeHigh").clone();

      int states = op.length;
      if (out1.length != states || out2.length != states || classId.length != states) {
        throw new IllegalArgumentException("regex image state lanes differ in length");
      }
      if (states == 0
          || startState < 0
          || startState >= states
          || matchState < 0
          || matchState >= states) {
        throw new IllegalArgumentException("invalid regex image state coordinates");
      }
      int classes = asciiLow.length;
      if (asciiHigh.length != classes
          || rangeOffset.length != classes
          || rangeCount.length != classes) {
        throw new IllegalArgumentException("regex image class lanes differ in length");
      }
      if (rangeLow.length != rangeHigh.length) {
        throw new IllegalArgumentException("regex image range lanes differ in length");
      }
      for (int index = 0; index < classes; index++) {
        int offset = rangeOffset[index];
        int count = rangeCount[index];
        if (offset < 0
            || count < 0
            || offset > rangeLow.length
            || count > rangeLow.length - offset) {
          throw new IllegalArgumentException("invalid regex image range coordinates");
        }
      }
      for (int index = 0; index < rangeLow.length; index++) {
        if (rangeLow[index] > rangeHigh[index]) {
          throw new IllegalArgumentException("invalid regex image character range");
        }
      }
      for (int state = 0; state < states; state++) {
        int opcode = Byte.toUnsignedInt(op[state]);
        if (opcode < OP_CHAR || opcode > OP_ASSERT_NOT_WORD_BOUNDARY) {
          throw new IllegalArgumentException("invalid regex image opcode");
        }
        if (out1[state] < -1 || out1[state] >= states
            || out2[state] < -1 || out2[state] >= states) {
          throw new IllegalArgumentException("invalid regex image transition");
        }
        if (op[state] == OP_CHAR) {
          if (classId[state] < 0 || classId[state] >= classes || out1[state] < 0) {
            throw new IllegalArgumentException("invalid regex image character state");
          }
        } else if (classId[state] != -1) {
          throw new IllegalArgumentException("non-character regex state carries class id");
        }
        if (op[state] == OP_SPLIT && (out1[state] < 0 || out2[state] < 0)) {
          throw new IllegalArgumentException("invalid regex image split state");
        }
        if ((op[state] == OP_JUMP
                || op[state] == OP_ASSERT_START
                || op[state] == OP_ASSERT_END
                || op[state] == OP_ASSERT_WORD_BOUNDARY
                || op[state] == OP_ASSERT_NOT_WORD_BOUNDARY)
            && out1[state] < 0) {
          throw new IllegalArgumentException("invalid regex image epsilon/assert state");
        }
      }
      if (op[matchState] != OP_MATCH) {
        throw new IllegalArgumentException("regex image match coordinate is not MATCH opcode");
      }
      if (minLength < 0 || maxLength < -1 || captureCount < 0) {
        throw new IllegalArgumentException("invalid regex image metadata");
      }
    }

    @Override public byte[] op() { return op.clone(); }
    @Override public int[] out1() { return out1.clone(); }
    @Override public int[] out2() { return out2.clone(); }
    @Override public int[] classId() { return classId.clone(); }
    @Override public long[] asciiLow() { return asciiLow.clone(); }
    @Override public long[] asciiHigh() { return asciiHigh.clone(); }
    @Override public int[] rangeOffset() { return rangeOffset.clone(); }
    @Override public int[] rangeCount() { return rangeCount.clone(); }
    @Override public char[] rangeLow() { return rangeLow.clone(); }
    @Override public char[] rangeHigh() { return rangeHigh.clone(); }

    /** Stable identity of the portable automaton image used by detached providers. */
    public String rootHash() {
      return imageRoot(this);
    }
  }

  private static String imageRoot(Image image) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] expressionBytes = image.expression().getBytes(StandardCharsets.UTF_8);
      if (image.expression().equals(new String(expressionBytes, StandardCharsets.UTF_8))) {
        updateUtf8(digest, "M_INDEX_REGEX_IMAGE_V1");
        updateBytes(digest, expressionBytes);
      } else {
        updateUtf8(digest, "M_INDEX_REGEX_IMAGE_UTF16_V4");
        updateChars(digest, image.expression().toCharArray());
      }
      updateInt(digest, image.semantics().ordinal());
      updateBytes(digest, image.op());
      updateInts(digest, image.out1());
      updateInts(digest, image.out2());
      updateInts(digest, image.classId());
      updateLongs(digest, image.asciiLow());
      updateLongs(digest, image.asciiHigh());
      updateInts(digest, image.rangeOffset());
      updateInts(digest, image.rangeCount());
      updateChars(digest, image.rangeLow());
      updateChars(digest, image.rangeHigh());
      updateInt(digest, image.startState());
      updateInt(digest, image.matchState());
      updateInt(digest, image.minLength());
      updateInt(digest, image.maxLength());
      updateInt(digest, image.captureCount());
      updateLong(digest, image.requiredPresence64());
      updateInt(digest, image.requiredCharacterFlags());
      updateInt(digest, image.mayConsumeSurrogateUnit() ? 1 : 0);
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException impossible) {
      throw new ExceptionInInitializerError(impossible);
    }
  }
  private static void updateUtf8(MessageDigest digest, String value) {
    updateBytes(digest, value.getBytes(StandardCharsets.UTF_8));
  }
  private static void updateBytes(MessageDigest digest, byte[] values) {
    updateInt(digest, values.length);
    digest.update(values);
  }
  private static void updateInts(MessageDigest digest, int[] values) {
    updateInt(digest, values.length);
    for (int value : values) updateInt(digest, value);
  }
  private static void updateLongs(MessageDigest digest, long[] values) {
    updateInt(digest, values.length);
    for (long value : values) updateLong(digest, value);
  }
  private static void updateChars(MessageDigest digest, char[] values) {
    updateInt(digest, values.length);
    for (char value : values) updateInt(digest, value);
  }
  private static void updateInt(MessageDigest digest, int value) {
    digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
  }
  private static void updateLong(MessageDigest digest, long value) {
    digest.update(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
  }
}
