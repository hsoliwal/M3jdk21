// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Deterministic bounded binary codec for portable and fully precomputed MIndex regex images.
 *
 * <p>Version 1 preserves {@link MIndexRegexProgram.Image} compatibility. Version 2 preserves
 * {@link MIndexRegexProgram.ExecutionImage}, including every bounded derived execution table, so
 * recipe-produced hot artifacts restore without parsing or derived-table reconstruction. Both
 * formats are length-bounded, versioned, SHA-256 protected, and fail closed on malformed lanes.
 * Versions 4, 5 and 6 retain those respective layouts with raw UTF-16 expression metadata when
 * unpaired surrogate units cannot be represented exactly by the legacy UTF-8 field. Legacy
 * representable expressions retain byte-for-byte v1, v2 and v3 encoding.</p>
 */
public final class MIndexRegexProgramImageCodec {
  private static final int MAGIC = 0x534e5852; // SNXR
  private static final int VERSION = 1;
  private static final int EXECUTION_VERSION = 2;
  private static final int ACCELERATED_VERSION = 3;
  private static final int UTF16_VERSION = 4;
  private static final int UTF16_EXECUTION_VERSION = 5;
  private static final int UTF16_ACCELERATED_VERSION = 6;
  private static final int PRECOMPUTED_VERSION = 7;
  private static final int DIGEST_BYTES = 32;
  private static final int HEADER_BYTES = Integer.BYTES * 3 + DIGEST_BYTES;
  private static final int MAX_IMAGE_BYTES = 32 * 1024 * 1024;
  private static final int MAX_EXECUTION_IMAGE_BYTES = 64 * 1024 * 1024;
  private static final int MAX_EXPRESSION_BYTES = 1024 * 1024;
  private static final int MAX_STATES = 100_000;
  private static final int MAX_CLASSES = 65_536;
  private static final int MAX_RANGES = 1_000_000;

  private MIndexRegexProgramImageCodec() {}

  public static byte[] encode(MIndexRegexProgram.Image image) {
    MIndexRegexProgram.Image checked = Objects.requireNonNull(image, "image");
    boolean utf16 = requiresUtf16(checked.expression());
    try {
      ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(payloadBytes)) {
        byte[] expression = utf16 ? null : checked.expression().getBytes(StandardCharsets.UTF_8);
        if (utf16 ? checked.expression().length() > MAX_EXPRESSION_BYTES / Character.BYTES
            : expression.length > MAX_EXPRESSION_BYTES) {
          throw new IllegalArgumentException("regex expression exceeds codec byte budget");
        }
        byte[] op = checked.op();
        long[] asciiLow = checked.asciiLow();
        char[] rangeLow = checked.rangeLow();
        requireCount("states", op.length, MAX_STATES);
        requireCount("classes", asciiLow.length, MAX_CLASSES);
        requireCount("ranges", rangeLow.length, MAX_RANGES);

        out.writeInt(checked.semantics().ordinal());
        if (utf16) {
          out.writeInt(checked.expression().length());
          writeChars(out, checked.expression().toCharArray());
        } else {
          out.writeInt(expression.length);
          out.write(expression);
        }
        out.writeInt(checked.startState());
        out.writeInt(checked.matchState());
        out.writeInt(checked.minLength());
        out.writeInt(checked.maxLength());
        out.writeInt(checked.captureCount());
        out.writeLong(checked.requiredPresence64());
        out.writeInt(checked.requiredCharacterFlags());
        out.writeBoolean(checked.mayConsumeSurrogateUnit());

        out.writeInt(op.length);
        out.writeInt(asciiLow.length);
        out.writeInt(rangeLow.length);
        out.write(op);
        writeInts(out, checked.out1());
        writeInts(out, checked.out2());
        writeInts(out, checked.classId());
        writeLongs(out, asciiLow);
        writeLongs(out, checked.asciiHigh());
        writeInts(out, checked.rangeOffset());
        writeInts(out, checked.rangeCount());
        writeChars(out, rangeLow);
        writeChars(out, checked.rangeHigh());
      }
      return envelope(utf16 ? UTF16_VERSION : VERSION, payloadBytes.toByteArray(), MAX_IMAGE_BYTES);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Encode a fully precomputed v2 execution image, or v5 for exact raw UTF-16 metadata.
   *
   * <p>The respective v1 or v4 program image is nested for independent validation; all
   * derived runtime kernels follow it in deterministic primitive lanes.</p>
   */
  public static byte[] encode(MIndexRegexProgram.ExecutionImage image) {
    MIndexRegexProgram.ExecutionImage checked = Objects.requireNonNull(image, "image");
    boolean utf16 = requiresUtf16(checked.program().expression());
    try {
      ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(payloadBytes)) {
        byte[] base = encode(checked.program());
        out.writeInt(base.length);
        out.write(base);
        writeCountedLongs(out, checked.asciiStateMasks());
        out.writeInt(checked.epsilonWordsPerRow());
        writeCountedInts(out, checked.epsilonRowByState());
        writeCountedLongs(out, checked.epsilonTerminals());
        out.writeInt(checked.asciiSubsetBits());
        out.writeInt(checked.asciiSubsetSourceCount());
        writeCountedLongs(out, checked.asciiSubsetTransitions());
        out.writeInt(checked.bmpClassWordCount());
        writeCountedInts(out, checked.bmpPageByHighByte());
        writeCountedLongs(out, checked.bmpClassMasks());
        writeDfa(out, checked.anchoredAsciiDfa());
        writeDfa(out, checked.searchAsciiDfa());
      }
      return envelope(
          utf16 ? UTF16_EXECUTION_VERSION : EXECUTION_VERSION,
          payloadBytes.toByteArray(), MAX_EXECUTION_IMAGE_BYTES);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * Encode a v3 accelerated image, or v6 for exact raw UTF-16 metadata.
   *
   * <p>The respective v2 or v5 execution image remains independently verifiable.</p>
   */
  public static byte[] encode(MIndexRegexAcceleratedImage image) {
    MIndexRegexAcceleratedImage checked = Objects.requireNonNull(image, "image");
    boolean utf16 = requiresUtf16(checked.executionImage().program().expression());
    try {
      ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(payloadBytes)) {
        byte[] base = encode(checked.executionImage());
        out.writeInt(base.length);
        out.write(base);
        writeCompactDfa(out, checked.anchoredCompactAsciiDfa());
        writeCompactDfa(out, checked.searchCompactAsciiDfa());
      }
      return envelope(
          utf16 ? UTF16_ACCELERATED_VERSION : ACCELERATED_VERSION,
          payloadBytes.toByteArray(), MAX_EXECUTION_IMAGE_BYTES);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /** Encode complete execution kernels and precomputed matcher-dispatch metadata as v7. */
  public static byte[] encode(MIndexRegexProgram.PrecomputedImage image) {
    MIndexRegexProgram.PrecomputedImage checked = Objects.requireNonNull(image, "image");
    try {
      ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(payloadBytes)) {
        byte[] execution = encode(checked.execution());
        out.writeInt(execution.length);
        out.write(execution);
        out.writeLong(checked.firstAsciiLow());
        out.writeLong(checked.firstAsciiHigh());
        out.writeInt(checked.branchingStateCount());
      }
      return envelope(PRECOMPUTED_VERSION, payloadBytes.toByteArray(), MAX_EXECUTION_IMAGE_BYTES);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  public static MIndexRegexProgram.PrecomputedImage decodePrecomputed(byte[] encoded) {
    byte[] bytes = Objects.requireNonNull(encoded, "encoded").clone();
    return decodePrecomputed(ByteBuffer.wrap(bytes));
  }

  /** Restore stored kernels and dispatch facts without changing the caller's buffer position. */
  public static MIndexRegexProgram.PrecomputedImage decodePrecomputed(ByteBuffer encoded) {
    ByteBuffer input = payload(encoded, PRECOMPUTED_VERSION, MAX_EXECUTION_IMAGE_BYTES);
    int executionLength = boundedCount(input, "nested execution image", MAX_EXECUTION_IMAGE_BYTES);
    requireRemaining(input, executionLength, "nested execution image");
    ByteBuffer executionBytes = input.slice().order(ByteOrder.BIG_ENDIAN);
    executionBytes.limit(executionLength);
    input.position(Math.addExact(input.position(), executionLength));
    MIndexRegexProgram.ExecutionImage execution = decodeExecution(executionBytes);
    long firstLow = getLong(input, "first ASCII low");
    long firstHigh = getLong(input, "first ASCII high");
    int branching = getInt(input, "branching state count");
    if (input.hasRemaining()) {
      throw new IllegalArgumentException("regex precomputed image trailing payload");
    }
    return new MIndexRegexProgram.PrecomputedImage(execution, firstLow, firstHigh, branching);
  }

  public static MIndexRegexProgram.Image decode(byte[] encoded) {
    byte[] bytes = Objects.requireNonNull(encoded, "encoded").clone();
    return decode(ByteBuffer.wrap(bytes));
  }

  /**
   * Decode from heap, direct, or memory-mapped storage without modifying the caller's position.
   *
   * <p>The encoded bytes are never retained by the resulting image. Primitive lanes are copied into
   * the existing immutable {@link MIndexRegexProgram.Image} owner after checksum/geometry checks.</p>
   */
  public static MIndexRegexProgram.Image decode(ByteBuffer encoded) {
    int version = imageVersion(encoded, VERSION, UTF16_VERSION);
    return decodePayload(payload(encoded, version, MAX_IMAGE_BYTES), version == UTF16_VERSION);
  }

  public static MIndexRegexAcceleratedImage decodeAccelerated(byte[] encoded) {
    byte[] bytes = Objects.requireNonNull(encoded, "encoded").clone();
    return decodeAccelerated(ByteBuffer.wrap(bytes));
  }

  public static MIndexRegexAcceleratedImage decodeAccelerated(ByteBuffer encoded) {
    int version = imageVersion(encoded, ACCELERATED_VERSION, UTF16_ACCELERATED_VERSION);
    boolean utf16 = version == UTF16_ACCELERATED_VERSION;
    ByteBuffer input = payload(encoded, version, MAX_EXECUTION_IMAGE_BYTES);
    int baseLength = boundedCount(input, "nested v2 image", MAX_EXECUTION_IMAGE_BYTES);
    requireRemaining(input, baseLength, "nested v2 image");
    ByteBuffer base = input.slice().order(ByteOrder.BIG_ENDIAN);
    base.limit(baseLength);
    input.position(Math.addExact(input.position(), baseLength));
    requireNestedVersion(base, utf16 ? UTF16_EXECUTION_VERSION : EXECUTION_VERSION);
    MIndexRegexProgram.ExecutionImage execution = decodeExecution(base);
    MIndexCompactAsciiDfaImage anchored = readCompactDfa(input, "anchored");
    MIndexCompactAsciiDfaImage search = readCompactDfa(input, "search");
    if (input.hasRemaining()) {
      throw new IllegalArgumentException("regex accelerated image trailing payload");
    }
    return new MIndexRegexAcceleratedImage(execution, anchored, search);
  }

  public static MIndexRegexProgram.ExecutionImage decodeExecution(byte[] encoded) {
    byte[] bytes = Objects.requireNonNull(encoded, "encoded").clone();
    return decodeExecution(ByteBuffer.wrap(bytes));
  }

  /** Decode a v2 or exact-UTF-16 v5 execution image without changing caller position. */
  public static MIndexRegexProgram.ExecutionImage decodeExecution(ByteBuffer encoded) {
    int version = imageVersion(encoded, EXECUTION_VERSION, UTF16_EXECUTION_VERSION);
    return decodeExecutionPayload(
        payload(encoded, version, MAX_EXECUTION_IMAGE_BYTES), version == UTF16_EXECUTION_VERSION);
  }

  public static void write(Path path, MIndexRegexProgram.Image image) throws IOException {
    Path target = Objects.requireNonNull(path, "path");
    Files.write(target, encode(image), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
  }

  public static void write(Path path, MIndexRegexAcceleratedImage image) throws IOException {
    Path target = Objects.requireNonNull(path, "path");
    Files.write(target, encode(image), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
  }

  public static void write(Path path, MIndexRegexProgram.ExecutionImage image) throws IOException {
    Path target = Objects.requireNonNull(path, "path");
    Files.write(target, encode(image), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
  }

  public static MIndexRegexProgram.Image read(Path path) throws IOException {
    Path source = Objects.requireNonNull(path, "path");
    long size = Files.size(source);
    if (size < HEADER_BYTES || size > MAX_IMAGE_BYTES) {
      throw new IllegalArgumentException("regex image file byte geometry");
    }
    return decode(Files.readAllBytes(source));
  }

  /**
   * Restore a precompiled image through a read-only memory mapping.
   *
   * <p>The mapping is only the encoded input transport; decoded primitive arrays remain owned by the
   * immutable program image and no file descriptor or mapped buffer escapes this method.</p>
   */
  public static MIndexRegexProgram.Image readMapped(Path path) throws IOException {
    Path source = Objects.requireNonNull(path, "path");
    try (FileChannel channel = FileChannel.open(source, StandardOpenOption.READ)) {
      long size = channel.size();
      if (size < HEADER_BYTES || size > MAX_IMAGE_BYTES) {
        throw new IllegalArgumentException("regex image file byte geometry");
      }
      return decode(channel.map(FileChannel.MapMode.READ_ONLY, 0L, size));
    }
  }

  public static MIndexRegexAcceleratedImage readAccelerated(Path path)
      throws IOException {
    Path source = Objects.requireNonNull(path, "path");
    long size = Files.size(source);
    if (size < HEADER_BYTES || size > MAX_EXECUTION_IMAGE_BYTES) {
      throw new IllegalArgumentException("regex accelerated image file byte geometry");
    }
    return decodeAccelerated(Files.readAllBytes(source));
  }

  public static MIndexRegexAcceleratedImage readAcceleratedMapped(Path path)
      throws IOException {
    Path source = Objects.requireNonNull(path, "path");
    try (FileChannel channel = FileChannel.open(source, StandardOpenOption.READ)) {
      long size = channel.size();
      if (size < HEADER_BYTES || size > MAX_EXECUTION_IMAGE_BYTES) {
        throw new IllegalArgumentException("regex accelerated image file byte geometry");
      }
      return decodeAccelerated(channel.map(FileChannel.MapMode.READ_ONLY, 0L, size));
    }
  }

  public static MIndexRegexProgram.ExecutionImage readExecution(Path path) throws IOException {
    Path source = Objects.requireNonNull(path, "path");
    long size = Files.size(source);
    if (size < HEADER_BYTES || size > MAX_EXECUTION_IMAGE_BYTES) {
      throw new IllegalArgumentException("regex execution image file byte geometry");
    }
    return decodeExecution(Files.readAllBytes(source));
  }

  public static MIndexRegexProgram.ExecutionImage readExecutionMapped(Path path)
      throws IOException {
    Path source = Objects.requireNonNull(path, "path");
    try (FileChannel channel = FileChannel.open(source, StandardOpenOption.READ)) {
      long size = channel.size();
      if (size < HEADER_BYTES || size > MAX_EXECUTION_IMAGE_BYTES) {
        throw new IllegalArgumentException("regex execution image file byte geometry");
      }
      return decodeExecution(channel.map(FileChannel.MapMode.READ_ONLY, 0L, size));
    }
  }

  private static MIndexRegexProgram.ExecutionImage decodeExecutionPayload(
      ByteBuffer input, boolean utf16) {
    int baseLength = boundedCount(input, "nested v1 image", MAX_IMAGE_BYTES);
    requireRemaining(input, baseLength, "nested v1 image");
    byte[] base = new byte[baseLength];
    input.get(base);
    requireNestedVersion(ByteBuffer.wrap(base), utf16 ? UTF16_VERSION : VERSION);
    MIndexRegexProgram.Image program = decode(ByteBuffer.wrap(base));

    long[] asciiStateMasks = readCountedLongs(input, "execution ASCII state masks");
    int epsilonWordsPerRow = getInt(input, "execution epsilon words");
    int[] epsilonRowByState = readCountedInts(input, "execution epsilon rows");
    long[] epsilonTerminals = readCountedLongs(input, "execution epsilon terminals");
    int asciiSubsetBits = getInt(input, "execution ASCII subset bits");
    int asciiSubsetSourceCount = getInt(input, "execution ASCII subset sources");
    long[] asciiSubsetTransitions =
        readCountedLongs(input, "execution ASCII subset transitions");
    int bmpClassWordCount = getInt(input, "execution BMP class words");
    int[] bmpPageByHighByte = readCountedInts(input, "execution BMP pages");
    long[] bmpClassMasks = readCountedLongs(input, "execution BMP masks");
    MIndexRegexProgram.AsciiDfaImage anchored = readDfa(input, "anchored");
    MIndexRegexProgram.AsciiDfaImage search = readDfa(input, "search");
    if (input.hasRemaining()) {
      throw new IllegalArgumentException("regex execution image trailing payload");
    }
    return new MIndexRegexProgram.ExecutionImage(
        program,
        asciiStateMasks,
        epsilonWordsPerRow,
        epsilonRowByState,
        epsilonTerminals,
        asciiSubsetBits,
        asciiSubsetSourceCount,
        asciiSubsetTransitions,
        bmpClassWordCount,
        bmpPageByHighByte,
        bmpClassMasks,
        anchored,
        search);
  }

  private static MIndexRegexProgram.Image decodePayload(ByteBuffer input, boolean utf16) {
    int semanticsOrdinal = getInt(input, "semantics");
    MIndexRegexProgram.Semantics[] semantics = MIndexRegexProgram.Semantics.values();
    if (semanticsOrdinal < 0 || semanticsOrdinal >= semantics.length) {
      throw new IllegalArgumentException("regex image semantics");
    }
    String expression = readExpression(input, utf16);

    int startState = getInt(input, "start state");
    int matchState = getInt(input, "match state");
    int minLength = getInt(input, "minimum length");
    int maxLength = getInt(input, "maximum length");
    int captureCount = getInt(input, "capture count");
    long requiredPresence64 = getLong(input, "presence");
    int requiredCharacterFlags = getInt(input, "character flags");
    requireRemaining(input, 1, "surrogate flag");
    byte surrogate = input.get();
    if (surrogate != 0 && surrogate != 1) {
      throw new IllegalArgumentException("regex image surrogate flag");
    }

    int states = boundedCount(input, "states", MAX_STATES);
    int classes = boundedCount(input, "classes", MAX_CLASSES);
    int ranges = boundedCount(input, "ranges", MAX_RANGES);
    requireRemaining(input, states, "opcodes");
    byte[] op = new byte[states];
    input.get(op);
    int[] out1 = readInts(input, states, "out1");
    int[] out2 = readInts(input, states, "out2");
    int[] classId = readInts(input, states, "class ids");
    long[] asciiLow = readLongs(input, classes, "ASCII low");
    long[] asciiHigh = readLongs(input, classes, "ASCII high");
    int[] rangeOffset = readInts(input, classes, "range offsets");
    int[] rangeCount = readInts(input, classes, "range counts");
    char[] rangeLow = readChars(input, ranges, "range low");
    char[] rangeHigh = readChars(input, ranges, "range high");
    if (input.hasRemaining()) throw new IllegalArgumentException("regex image trailing payload");

    return new MIndexRegexProgram.Image(
        expression, semantics[semanticsOrdinal], op, out1, out2, classId,
        asciiLow, asciiHigh, rangeOffset, rangeCount, rangeLow, rangeHigh,
        startState, matchState, minLength, maxLength, captureCount,
        requiredPresence64, requiredCharacterFlags, surrogate != 0);
  }

  private static boolean requiresUtf16(String expression) {
    for (int index = 0; index < expression.length(); index++) {
      char value = expression.charAt(index);
      if (Character.isHighSurrogate(value)) {
        if (++index == expression.length() || !Character.isLowSurrogate(expression.charAt(index))) {
          return true;
        }
      } else if (Character.isLowSurrogate(value)) {
        return true;
      }
    }
    return false;
  }

  private static String readExpression(ByteBuffer input, boolean utf16) {
    if (utf16) {
      int units = boundedCount(input, "expression UTF-16", MAX_EXPRESSION_BYTES / Character.BYTES);
      return new String(readChars(input, units, "expression UTF-16"));
    }
    int length = boundedCount(input, "expression", MAX_EXPRESSION_BYTES);
    requireRemaining(input, length, "expression");
    byte[] bytes = new byte[length];
    input.get(bytes);
    String expression = new String(bytes, StandardCharsets.UTF_8);
    if (!Arrays.equals(expression.getBytes(StandardCharsets.UTF_8), bytes)) {
      throw new IllegalArgumentException("regex image expression UTF-8");
    }
    return expression;
  }

  private static int imageVersion(ByteBuffer encoded, int legacy, int utf16) {
    ByteBuffer input = Objects.requireNonNull(encoded, "encoded").asReadOnlyBuffer();
    requireRemaining(input, Integer.BYTES * 2, "header");
    int version = input.order(ByteOrder.BIG_ENDIAN).getInt(input.position() + Integer.BYTES);
    if (version != legacy && version != utf16) {
      throw new IllegalArgumentException("regex image version");
    }
    return version;
  }

  private static void requireNestedVersion(ByteBuffer encoded, int expected) {
    imageVersion(encoded, expected, expected);
  }

  private static byte[] envelope(int version, byte[] payload, int maximumBytes) {
    int total = Math.addExact(HEADER_BYTES, payload.length);
    if (total > maximumBytes) {
      throw new IllegalArgumentException("regex image exceeds codec byte budget");
    }
    ByteBuffer encoded = ByteBuffer.allocate(total).order(ByteOrder.BIG_ENDIAN);
    encoded.putInt(MAGIC);
    encoded.putInt(version);
    encoded.putInt(payload.length);
    encoded.put(sha256(payload));
    encoded.put(payload);
    return encoded.array();
  }

  private static ByteBuffer payload(ByteBuffer encoded, int version, int maximumBytes) {
    ByteBuffer input = Objects.requireNonNull(encoded, "encoded")
        .asReadOnlyBuffer()
        .order(ByteOrder.BIG_ENDIAN);
    if (input.remaining() < HEADER_BYTES || input.remaining() > maximumBytes) {
      throw new IllegalArgumentException("regex image byte geometry");
    }
    if (input.getInt() != MAGIC) throw new IllegalArgumentException("regex image magic");
    if (input.getInt() != version) throw new IllegalArgumentException("regex image version");
    int payloadLength = input.getInt();
    if (payloadLength < 0 || payloadLength != input.remaining() - DIGEST_BYTES) {
      throw new IllegalArgumentException("regex image payload length");
    }
    byte[] expectedDigest = new byte[DIGEST_BYTES];
    input.get(expectedDigest);
    ByteBuffer payload = input.slice().order(ByteOrder.BIG_ENDIAN);
    payload.limit(payloadLength);
    if (!MessageDigest.isEqual(expectedDigest, sha256(payload.asReadOnlyBuffer()))) {
      throw new IllegalArgumentException("regex image checksum");
    }
    return payload;
  }

  private static void writeCountedInts(DataOutputStream out, int[] values) throws IOException {
    out.writeInt(values.length);
    writeInts(out, values);
  }

  private static void writeCountedLongs(DataOutputStream out, long[] values) throws IOException {
    out.writeInt(values.length);
    writeLongs(out, values);
  }

  private static void writeCompactDfa(
      DataOutputStream out, MIndexCompactAsciiDfaImage image) throws IOException {
    out.writeBoolean(image != null);
    if (image == null) return;
    out.writeInt(image.startState());
    out.writeInt(image.classCount());
    byte[] classes = image.asciiClassByValue();
    out.writeInt(classes.length);
    out.write(classes);
    short[] transitions = image.transitions();
    out.writeInt(transitions.length);
    for (short transition : transitions) out.writeShort(transition);
    byte[] accepting = image.accepting();
    out.writeInt(accepting.length);
    out.write(accepting);
  }

  private static MIndexCompactAsciiDfaImage readCompactDfa(
      ByteBuffer input, String lane) {
    requireRemaining(input, 1, lane + " compact DFA presence");
    byte present = input.get();
    if (present == 0) return null;
    if (present != 1) {
      throw new IllegalArgumentException("regex compact DFA presence");
    }
    int start = getInt(input, lane + " compact DFA start");
    int classCount = boundedCount(input, lane + " compact DFA classes", 128);
    int mapCount = boundedCount(input, lane + " compact DFA map", 128);
    requireRemaining(input, mapCount, lane + " compact DFA map");
    byte[] classes = new byte[mapCount];
    input.get(classes);
    int transitionCount =
        boundedCount(input, lane + " compact DFA transitions",
            MAX_EXECUTION_IMAGE_BYTES / Short.BYTES);
    requireRemaining(
        input, Math.multiplyExact(transitionCount, Short.BYTES),
        lane + " compact DFA transitions");
    short[] transitions = new short[transitionCount];
    for (int index = 0; index < transitionCount; index++) {
      transitions[index] = input.getShort();
    }
    int acceptingCount =
        boundedCount(input, lane + " compact DFA accepting", MAX_EXECUTION_IMAGE_BYTES);
    requireRemaining(input, acceptingCount, lane + " compact DFA accepting");
    byte[] accepting = new byte[acceptingCount];
    input.get(accepting);
    return new MIndexCompactAsciiDfaImage(
        start, classes, classCount, transitions, accepting);
  }

  private static void writeDfa(
      DataOutputStream out, MIndexRegexProgram.AsciiDfaImage image) throws IOException {
    out.writeBoolean(image != null);
    if (image == null) return;
    out.writeInt(image.startState());
    short[] transitions = image.transitions();
    byte[] accepting = image.accepting();
    out.writeInt(transitions.length);
    for (short transition : transitions) out.writeShort(transition);
    out.writeInt(accepting.length);
    out.write(accepting);
  }

  private static void writeInts(DataOutputStream out, int[] values) throws IOException {
    for (int value : values) out.writeInt(value);
  }

  private static void writeLongs(DataOutputStream out, long[] values) throws IOException {
    for (long value : values) out.writeLong(value);
  }

  private static void writeChars(DataOutputStream out, char[] values) throws IOException {
    for (char value : values) out.writeChar(value);
  }

  private static int[] readCountedInts(ByteBuffer input, String lane) {
    int count = boundedCount(input, lane, MAX_EXECUTION_IMAGE_BYTES / Integer.BYTES);
    return readInts(input, count, lane);
  }

  private static long[] readCountedLongs(ByteBuffer input, String lane) {
    int count = boundedCount(input, lane, MAX_EXECUTION_IMAGE_BYTES / Long.BYTES);
    return readLongs(input, count, lane);
  }

  private static MIndexRegexProgram.AsciiDfaImage readDfa(
      ByteBuffer input, String lane) {
    requireRemaining(input, 1, lane + " DFA presence");
    byte present = input.get();
    if (present == 0) return null;
    if (present != 1) throw new IllegalArgumentException("regex execution DFA presence");
    int start = getInt(input, lane + " DFA start");
    int transitionCount =
        boundedCount(input, lane + " DFA transitions", MAX_EXECUTION_IMAGE_BYTES / Short.BYTES);
    requireRemaining(
        input, Math.multiplyExact(transitionCount, Short.BYTES), lane + " DFA transitions");
    short[] transitions = new short[transitionCount];
    for (int index = 0; index < transitionCount; index++) {
      transitions[index] = input.getShort();
    }
    int acceptingCount =
        boundedCount(input, lane + " DFA accepting", MAX_EXECUTION_IMAGE_BYTES);
    requireRemaining(input, acceptingCount, lane + " DFA accepting");
    byte[] accepting = new byte[acceptingCount];
    input.get(accepting);
    return new MIndexRegexProgram.AsciiDfaImage(start, transitions, accepting);
  }

  private static int[] readInts(ByteBuffer input, int count, String lane) {
    requireRemaining(input, Math.multiplyExact(count, Integer.BYTES), lane);
    int[] result = new int[count];
    for (int index = 0; index < count; index++) result[index] = input.getInt();
    return result;
  }

  private static long[] readLongs(ByteBuffer input, int count, String lane) {
    requireRemaining(input, Math.multiplyExact(count, Long.BYTES), lane);
    long[] result = new long[count];
    for (int index = 0; index < count; index++) result[index] = input.getLong();
    return result;
  }

  private static char[] readChars(ByteBuffer input, int count, String lane) {
    requireRemaining(input, Math.multiplyExact(count, Character.BYTES), lane);
    char[] result = new char[count];
    for (int index = 0; index < count; index++) result[index] = input.getChar();
    return result;
  }

  private static int boundedCount(ByteBuffer input, String lane, int maximum) {
    int value = getInt(input, lane);
    requireCount(lane, value, maximum);
    return value;
  }

  private static void requireCount(String lane, int value, int maximum) {
    if (value < 0 || value > maximum) {
      throw new IllegalArgumentException("regex image " + lane + " count");
    }
  }

  private static int getInt(ByteBuffer input, String lane) {
    requireRemaining(input, Integer.BYTES, lane);
    return input.getInt();
  }

  private static long getLong(ByteBuffer input, String lane) {
    requireRemaining(input, Long.BYTES, lane);
    return input.getLong();
  }

  private static void requireRemaining(ByteBuffer input, int bytes, String lane) {
    if (bytes < 0 || input.remaining() < bytes) {
      throw new IllegalArgumentException("truncated regex image " + lane);
    }
  }

  private static byte[] sha256(byte[] bytes) {
    return sha256(ByteBuffer.wrap(bytes));
  }

  private static byte[] sha256(ByteBuffer bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(bytes);
      return digest.digest();
    } catch (NoSuchAlgorithmException impossible) {
      throw new ExceptionInInitializerError(impossible);
    }
  }
}
