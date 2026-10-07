// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.synexia.indexstring.IndexDictionary;
import com.synexia.indexstring.IndexRuntime;
import com.synexia.indexstring.IndexSpace;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/** Focused proof that MIAST count declarations are rejected before unsafe allocations. */
public final class MIndexASTCodecBoundedReadProof {
  private static final String SOURCE =
      "class Sample { int f(int x) { return x + 1; } int g(int x) { return x + 1; } }";

  private MIndexASTCodecBoundedReadProof() {}

  public static void main(String[] args) throws Exception {
    byte[] image = encode(parse());
    int atomCountOffset = atomCountOffset(image);
    int pathTextLengthOffset = pathTextLengthOffset(image);
    int childCountOffset = childCountOffset(image);
    int occurrenceCountOffset = occurrenceCountOffset(image);
    int diagnosticCountOffset = diagnosticCountOffset(image);

    expectRejected(image, atomCountOffset, "atom count");
    expectTextRejected(image, pathTextLengthOffset);
    expectRejected(image, childCountOffset, "child count");
    expectRejected(image, occurrenceCountOffset, "occurrence count");
    expectRejected(image, diagnosticCountOffset, "diagnostic count");

    MIndexASTDocument restored =
        MIndexASTCodec.read(newPool(), new ByteArrayInputStream(image), image.length);
    if (!parse().root().contentEquals(restored.root())) {
      throw new AssertionError("bounded codec read changed the valid AST");
    }
    byte[] rewritten = encode(restored);
    if (!Arrays.equals(image, rewritten)) {
      throw new AssertionError("bounded codec read changed valid MIAST bytes");
    }
    expectBudgetRejected(image);

    ByteBuffer fields = ByteBuffer.wrap(image);
    int atoms = fields.getInt(atomCountOffset);
    int occurrences = fields.getInt(occurrenceCountOffset);
    int diagnostics = fields.getInt(diagnosticCountOffset);
    System.out.println(
        "MINDEX_AST_CODEC_BOUNDED_READ_PASS"
            + "|imageBytes="
            + image.length
            + "|atomCount="
            + atoms
            + "|occurrences="
            + occurrences
            + "|diagnostics="
            + diagnostics
            + "|countGuards=4"
            + "|textGuard=1"
            + "|roundTrip=1"
            + "|wireFormat=1"
            + "|budgetGuard=1");
  }

  private static void expectRejected(byte[] image, int offset, String label) throws Exception {
    byte[] malformed = image.clone();
    ByteBuffer.wrap(malformed).putInt(offset, Integer.MAX_VALUE);
    try {
      MIndexASTCodec.read(newPool(), new ByteArrayInputStream(malformed));
      throw new AssertionError("accepted oversized " + label);
    } catch (IOException expected) {
      if (!expected.getMessage().contains(label)) {
        throw new AssertionError("wrong rejection for " + label + ": " + expected, expected);
      }
    }
  }

  private static void expectBudgetRejected(byte[] image) throws Exception {
    try {
      MIndexASTCodec.read(
          newPool(), new ByteArrayInputStream(image), Math.max(12, image.length - 1L));
      throw new AssertionError("accepted an image beyond the configured byte budget");
    } catch (IOException expected) {
      // The exact failure may be truncation at the bounded stream or a later field read.
    }
  }

  private static void expectTextRejected(byte[] image, int offset) throws Exception {
    byte[] malformed = image.clone();
    ByteBuffer.wrap(malformed).putInt(offset, Integer.MAX_VALUE);
    try {
      MIndexASTCodec.read(newPool(), new ByteArrayInputStream(malformed));
      throw new AssertionError("accepted oversized path text");
    } catch (IOException expected) {
      if (!expected.getMessage().contains("UTF-8 field length")) {
        throw new AssertionError("wrong rejection for oversized path text", expected);
      }
    }
  }

  private static int atomCountOffset(byte[] image) {
    ByteBuffer fields = header(image);
    skipText(fields);
    skipText(fields);
    fields.position(fields.position() + 32 + Integer.BYTES);
    return fields.position();
  }

  private static int pathTextLengthOffset(byte[] image) {
    ByteBuffer fields = header(image);
    skipText(fields);
    return fields.position();
  }

  private static int childCountOffset(byte[] image) {
    ByteBuffer fields = header(image);
    skipText(fields);
    skipText(fields);
    fields.position(fields.position() + 32 + Integer.BYTES);
    int atoms = fields.getInt();
    if (atoms < 1) throw new AssertionError("fixture has no atoms");
    skipText(fields);
    fields.position(fields.position() + Long.BYTES);
    boolean hasLabel = fields.get() != 0;
    if (hasLabel) skipText(fields);
    return fields.position();
  }

  private static int occurrenceCountOffset(byte[] image) {
    ByteBuffer fields = header(image);
    skipText(fields);
    skipText(fields);
    fields.position(fields.position() + 32 + Integer.BYTES);
    int atoms = fields.getInt();
    for (int atom = 0; atom < atoms; atom++) {
      skipText(fields);
      fields.position(fields.position() + Long.BYTES);
      if (fields.get() != 0) skipText(fields);
      int childCount = fields.getInt();
      fields.position(fields.position() + Math.multiplyExact(childCount, Integer.BYTES));
      fields.position(fields.position() + 3 * Long.BYTES);
    }
    return fields.position();
  }

  private static int diagnosticCountOffset(byte[] image) {
    ByteBuffer fields = ByteBuffer.wrap(image);
    int occurrenceOffset = occurrenceCountOffset(image);
    int occurrences = fields.getInt(occurrenceOffset);
    return occurrenceOffset + Integer.BYTES + Math.multiplyExact(occurrences, 36);
  }

  private static ByteBuffer header(byte[] image) {
    ByteBuffer fields = ByteBuffer.wrap(image);
    fields.position(12);
    return fields;
  }

  private static void skipText(ByteBuffer fields) {
    int length = fields.getInt();
    if (length < 0 || length > fields.remaining()) throw new AssertionError("bad fixture text");
    fields.position(fields.position() + length);
  }

  private static MIndexASTDocument parse() throws Exception {
    return new MIndexASTParser(newPool()).parse("Sample.java", SOURCE);
  }

  private static MIndexASTPool newPool() {
    IndexDictionary java =
        IndexDictionary.of(
            1,
            "java",
            "v1",
            List.of("Sample.java", "Sample", "f", "g", "x", "int", "1"));
    IndexRuntime runtime = new IndexRuntime(IndexSpace.builder().add(java).build());
    return new MIndexASTPool(runtime, 1);
  }

  private static byte[] encode(MIndexASTDocument document) throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    MIndexASTCodec.write(document, bytes);
    return bytes.toByteArray();
  }
}
