// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import com.synexia.job.IProgressMonitor;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, non-backtracking regular-expression program specialized for {@link CharSequence}
 * inputs such as {@link MIndexString}.
 *
 * <p>The parser intentionally accepts a conservative regular subset: literals (including
 * \Q...\E quoted text), dot, character classes, ASCII \d/\w/\s families, alternation,
 * grouping, greedy/non-greedy repetition, absolute anchors, and RE2 ASCII \b/\B boundaries.
 * The compiled runtime is a Thompson NFA stored in primitive arrays. Unsupported or
 * semantics-sensitive constructs are left to RE2/J or the explicit JDK compatibility path; they
 * are never approximated.</p>
 *
 * <p>Runtime matching does not materialize a String and does not backtrack. Character classes are
 * precompiled to two ASCII bit masks plus sorted UTF-16 ranges, with a bounded paged direct-address
 * BMP class table when profitable. Active NFA states are primitive long[] bit sets, so the hot path
 * is integer/bit arithmetic plus {@code charAt} on the original indexed value.</p>
 */
public final class MIndexRegexProgram {
  public enum Semantics {
    /** RE2/J-compatible line behavior for the supported subset. */
    RE2,
    /** java.util.regex-compatible line behavior for the supported subset. */
    JDK
  }

  private static final byte OP_CHAR = 1;
  private static final byte OP_SPLIT = 2;
  private static final byte OP_JUMP = 3;
  private static final byte OP_ASSERT_START = 4;
  private static final byte OP_ASSERT_END = 5;
  private static final byte OP_MATCH = 6;
  private static final byte OP_ASSERT_WORD_BOUNDARY = 7;
  private static final byte OP_ASSERT_NOT_WORD_BOUNDARY = 8;
  private static final int ASCII_CARDINALITY = 128;
  private static final long MAX_ASCII_STATE_MASK_BYTES = 4L * 1024L * 1024L;
  private static final long MAX_EPSILON_CLOSURE_BYTES = 4L * 1024L * 1024L;
  private static final int MIN_SUBSET_BITS = 4;
  private static final int MAX_SUBSET_BITS = 8;
  private static final long MAX_ASCII_SUBSET_TRANSITION_BYTES = 8L * 1024L * 1024L;
  private static final long MAX_BMP_CLASS_MASK_BYTES = 4L * 1024L * 1024L;
  private static final long MAX_BMP_CLASS_MASK_ASSIGNMENTS = 4_000_000L;
  private static final long MAX_ASCII_DFA_BYTES = 2L * 1024L * 1024L;
  private static final long MAX_ASCII_DFA_BUILD_BYTES = 8L * 1024L * 1024L;
  private static final int MAX_ASCII_DFA_STATES = 4_096;
  private static final int DFA_UNSUPPORTED = -1;
  private static final int MAX_TRIGRAM_EXACT_STRINGS = 64;
  private static final int MAX_TRIGRAM_EXACT_UTF16 = 256;
  private static final int MAX_TRIGRAM_CLASS_LITERALS = 16;
  private static final int MAX_TRIGRAM_REPEAT_EXPANSION = 8;

  private final String expression;
  private final Semantics semantics;
  private final byte[] op;
  private final int[] out1;
  private final int[] out2;
  private final int[] classId;
  private final long[] asciiLow;
  private final long[] asciiHigh;
  private final int[] rangeOffset;
  private final int[] rangeCount;
  private final char[] rangeLow;
  private final char[] rangeHigh;
  private final int startState;
  private final int matchState;
  private final int minLength;
  private final int maxLength;
  private final int captureCount;
  private final long requiredPresence64;
  private final int requiredCharacterFlags;
  private final boolean mayConsumeSurrogateUnit;
  private final long firstAsciiLow;
  private final long firstAsciiHigh;
  private final int branchingStateCount;
  private final int stateWordCount;
  private final long[] asciiStateMasks;
  private final EpsilonClosureTable epsilonClosures;
  private final int asciiSubsetBits;
  private final int asciiSubsetMask;
  private final int sourceSubsetsPerWord;
  private final int sourceSubsetCount;
  private final long[] asciiSubsetTransitions;
  private final int bmpClassWordCount;
  private final int[] bmpPageByHighByte;
  private final long[] bmpClassMasks;
  private final AsciiDfaTable anchoredAsciiDfa;
  private final AsciiDfaTable searchAsciiDfa;
  private volatile MIndexRegexTrigramQuery trigramQuery;

  /**
   * Portable immutable primitive image of one compiled regex program.
   *
   * <p>The image is suitable for javac/code-generation output. Array accessors return defensive
   * copies so callers cannot mutate compiled program identity after publication.</p>
   */
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

  /**
   * Fully precomputed immutable execution image.
   *
   * <p>Unlike {@link Image}, this form carries every bounded deterministic runtime table produced
   * by compilation. Restoring it requires no regex parsing, DFA determinization, epsilon-closure
   * construction, subset-table generation, or BMP class-page construction.</p>
   */
  public record ExecutionImage(
      Image program,
      long[] asciiStateMasks,
      int epsilonWordsPerRow,
      int[] epsilonRowByState,
      long[] epsilonTerminals,
      int asciiSubsetBits,
      int asciiSubsetSourceCount,
      long[] asciiSubsetTransitions,
      int bmpClassWordCount,
      int[] bmpPageByHighByte,
      long[] bmpClassMasks,
      AsciiDfaImage anchoredAsciiDfa,
      AsciiDfaImage searchAsciiDfa) {

    public ExecutionImage {
      program = Objects.requireNonNull(program, "program");
      asciiStateMasks = Objects.requireNonNull(asciiStateMasks, "asciiStateMasks").clone();
      epsilonRowByState =
          Objects.requireNonNull(epsilonRowByState, "epsilonRowByState").clone();
      epsilonTerminals =
          Objects.requireNonNull(epsilonTerminals, "epsilonTerminals").clone();
      asciiSubsetTransitions =
          Objects.requireNonNull(asciiSubsetTransitions, "asciiSubsetTransitions").clone();
      bmpPageByHighByte =
          Objects.requireNonNull(bmpPageByHighByte, "bmpPageByHighByte").clone();
      bmpClassMasks = Objects.requireNonNull(bmpClassMasks, "bmpClassMasks").clone();

      int states = program.op().length;
      int stateWords = (states + 63) >>> 6;
      if (asciiStateMasks.length != 0
          && asciiStateMasks.length != Math.multiplyExact(ASCII_CARDINALITY, stateWords)) {
        throw new IllegalArgumentException("execution image ASCII state-mask geometry");
      }

      if (epsilonWordsPerRow == 0) {
        if (epsilonRowByState.length != 0 || epsilonTerminals.length != 0) {
          throw new IllegalArgumentException("execution image epsilon geometry");
        }
      } else {
        if (epsilonWordsPerRow != stateWords || epsilonRowByState.length != states
            || epsilonTerminals.length % epsilonWordsPerRow != 0) {
          throw new IllegalArgumentException("execution image epsilon geometry");
        }
        int rows = epsilonTerminals.length / epsilonWordsPerRow;
        for (int row : epsilonRowByState) {
          if (row < -1 || row >= rows) {
            throw new IllegalArgumentException("execution image epsilon row");
          }
        }
      }

      if (asciiSubsetBits == 0) {
        if (asciiSubsetSourceCount != 0 || asciiSubsetTransitions.length != 0) {
          throw new IllegalArgumentException("execution image ASCII subset geometry");
        }
      } else {
        if ((asciiSubsetBits != MIN_SUBSET_BITS && asciiSubsetBits != MAX_SUBSET_BITS)
            || asciiSubsetSourceCount != (states + asciiSubsetBits - 1) / asciiSubsetBits) {
          throw new IllegalArgumentException("execution image ASCII subset geometry");
        }
        long expected =
            Math.multiplyExact(
                Math.multiplyExact(
                    Math.multiplyExact(
                        (long) ASCII_CARDINALITY,
                        asciiSubsetSourceCount),
                    1L << asciiSubsetBits),
                stateWords);
        if (expected != asciiSubsetTransitions.length) {
          throw new IllegalArgumentException("execution image ASCII subset lane length");
        }
      }

      if (bmpClassWordCount == 0) {
        if (bmpPageByHighByte.length != 0 || bmpClassMasks.length != 0) {
          throw new IllegalArgumentException("execution image BMP geometry");
        }
      } else {
        if (bmpPageByHighByte.length != 256
            || bmpClassWordCount != (program.asciiLow().length + 63) >>> 6) {
          throw new IllegalArgumentException("execution image BMP geometry");
        }
        int pageCount = 0;
        boolean[] seen = new boolean[256];
        for (int page : bmpPageByHighByte) {
          if (page < -1 || page >= 256) {
            throw new IllegalArgumentException("execution image BMP page");
          }
          if (page >= 0) {
            if (seen[page]) {
              throw new IllegalArgumentException("execution image duplicate BMP page");
            }
            seen[page] = true;
            pageCount = Math.max(pageCount, page + 1);
          }
        }
        long expected =
            Math.multiplyExact(
                Math.multiplyExact((long) pageCount, 256L),
                bmpClassWordCount);
        if (expected != bmpClassMasks.length) {
          throw new IllegalArgumentException("execution image BMP mask length");
        }
      }
    }

    @Override public long[] asciiStateMasks() { return asciiStateMasks.clone(); }
    @Override public int[] epsilonRowByState() { return epsilonRowByState.clone(); }
    @Override public long[] epsilonTerminals() { return epsilonTerminals.clone(); }
    @Override public long[] asciiSubsetTransitions() { return asciiSubsetTransitions.clone(); }
    @Override public int[] bmpPageByHighByte() { return bmpPageByHighByte.clone(); }
    @Override public long[] bmpClassMasks() { return bmpClassMasks.clone(); }

    public String rootHash() {
      return executionImageRoot(this);
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

  private static String executionImageRoot(ExecutionImage image) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      updateUtf8(digest, "M_INDEX_REGEX_EXECUTION_IMAGE_V2");
      updateUtf8(digest, image.program().rootHash());
      updateLongs(digest, image.asciiStateMasks());
      updateInt(digest, image.epsilonWordsPerRow());
      updateInts(digest, image.epsilonRowByState());
      updateLongs(digest, image.epsilonTerminals());
      updateInt(digest, image.asciiSubsetBits());
      updateInt(digest, image.asciiSubsetSourceCount());
      updateLongs(digest, image.asciiSubsetTransitions());
      updateInt(digest, image.bmpClassWordCount());
      updateInts(digest, image.bmpPageByHighByte());
      updateLongs(digest, image.bmpClassMasks());
      updateDfaDigest(digest, image.anchoredAsciiDfa());
      updateDfaDigest(digest, image.searchAsciiDfa());
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException impossible) {
      throw new ExceptionInInitializerError(impossible);
    }
  }

  private static void updateDfaDigest(MessageDigest digest, AsciiDfaImage image) {
    updateInt(digest, image == null ? 0 : 1);
    if (image == null) return;
    updateInt(digest, image.startState());
    updateShorts(digest, image.transitions());
    updateBytes(digest, image.accepting());
  }

  private static void updateUtf8(MessageDigest digest, String value) {
    updateBytes(digest, value.getBytes(StandardCharsets.UTF_8));
  }

  private static void updateBytes(MessageDigest digest, byte[] values) {
    updateInt(digest, values.length);
    digest.update(values);
  }

  private static void updateShorts(MessageDigest digest, short[] values) {
    updateInt(digest, values.length);
    for (short value : values) updateInt(digest, value);
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

  /** Portable immutable bounded ASCII DFA image for native/GPU batch execution. */
  public record AsciiDfaImage(int startState, short[] transitions, byte[] accepting) {
    public AsciiDfaImage {
      transitions = Objects.requireNonNull(transitions, "transitions").clone();
      accepting = Objects.requireNonNull(accepting, "accepting").clone();
      if (accepting.length == 0 || startState < 0 || startState >= accepting.length) {
        throw new IllegalArgumentException("invalid ASCII DFA state geometry");
      }
      if (transitions.length != Math.multiplyExact(accepting.length, ASCII_CARDINALITY)) {
        throw new IllegalArgumentException("invalid ASCII DFA transition geometry");
      }
      for (short transition : transitions) {
        if (transition < -1 || transition >= accepting.length) {
          throw new IllegalArgumentException("invalid ASCII DFA transition target");
        }
      }
      for (byte value : accepting) {
        if (value != 0 && value != 1) {
          throw new IllegalArgumentException("invalid ASCII DFA accepting lane");
        }
      }
    }

    @Override public short[] transitions() { return transitions.clone(); }

    @Override public byte[] accepting() { return accepting.clone(); }

    public int stateCount() { return accepting.length; }

    public long primitivePayloadBytes() {
      return (long) transitions.length * Short.BYTES + accepting.length;
    }
  }

  private MIndexRegexProgram(
      String expression,
      Semantics semantics,
      byte[] op,
      int[] out1,
      int[] out2,
      int[] classId,
      ClassTable classes,
      int startState,
      int matchState,
      int minLength,
      int maxLength,
      int captureCount,
      long requiredPresence64,
      int requiredCharacterFlags,
      boolean mayConsumeSurrogateUnit,
      long[] asciiStateMasks,
      EpsilonClosureTable epsilonClosures,
      AsciiSubsetTable subsetTable,
      BmpClassTable bmpClassTable,
      AsciiDfaTable anchoredAsciiDfa,
      AsciiDfaTable searchAsciiDfa) {
    this(expression, semantics, op, out1, out2, classId, classes, startState, matchState, minLength, maxLength, captureCount, requiredPresence64, requiredCharacterFlags, mayConsumeSurrogateUnit, asciiStateMasks, epsilonClosures, subsetTable, bmpClassTable, anchoredAsciiDfa, searchAsciiDfa, null);
  }

  private MIndexRegexProgram(
      String expression,
      Semantics semantics,
      byte[] op,
      int[] out1,
      int[] out2,
      int[] classId,
      ClassTable classes,
      int startState,
      int matchState,
      int minLength,
      int maxLength,
      int captureCount,
      long requiredPresence64,
      int requiredCharacterFlags,
      boolean mayConsumeSurrogateUnit,
      long[] asciiStateMasks,
      EpsilonClosureTable epsilonClosures,
      AsciiSubsetTable subsetTable,
      BmpClassTable bmpClassTable,
      AsciiDfaTable anchoredAsciiDfa,
      AsciiDfaTable searchAsciiDfa, PrecomputedImage precomputed) {
    this.expression = expression;
    this.semantics = semantics;
    this.op = op;
    this.out1 = out1;
    this.out2 = out2;
    this.classId = classId;
    this.asciiLow = classes.asciiLow;
    this.asciiHigh = classes.asciiHigh;
    this.rangeOffset = classes.rangeOffset;
    this.rangeCount = classes.rangeCount;
    this.rangeLow = classes.rangeLow;
    this.rangeHigh = classes.rangeHigh;
    this.startState = startState;
    this.matchState = matchState;
    this.minLength = minLength;
    this.maxLength = maxLength;
    this.captureCount = captureCount;
    this.requiredPresence64 = requiredPresence64;
    this.requiredCharacterFlags = requiredCharacterFlags;
    this.mayConsumeSurrogateUnit = mayConsumeSurrogateUnit;
    if (precomputed == null) {
      long[] firstAscii =
          firstAsciiMask(op, out1, out2, classId, classes.asciiLow, classes.asciiHigh, startState);
      this.firstAsciiLow = firstAscii[0];
      this.firstAsciiHigh = firstAscii[1];
      this.branchingStateCount = countOpcode(op, OP_SPLIT);
    } else {
      this.firstAsciiLow = precomputed.firstAsciiLow();
      this.firstAsciiHigh = precomputed.firstAsciiHigh();
      this.branchingStateCount = precomputed.branchingStateCount();
    }
    this.stateWordCount = (op.length + 63) >>> 6;
    this.asciiStateMasks = Objects.requireNonNull(asciiStateMasks, "asciiStateMasks");
    this.epsilonClosures = epsilonClosures;
    AsciiSubsetTable checkedSubsetTable =
        Objects.requireNonNull(subsetTable, "subsetTable");
    this.asciiSubsetBits = checkedSubsetTable.bits();
    this.asciiSubsetMask =
        asciiSubsetBits == 0 ? 0 : (1 << asciiSubsetBits) - 1;
    this.sourceSubsetsPerWord =
        asciiSubsetBits == 0 ? 0 : Long.SIZE / asciiSubsetBits;
    this.sourceSubsetCount = checkedSubsetTable.sourceSubsetCount();
    this.asciiSubsetTransitions = checkedSubsetTable.transitions();
    BmpClassTable checkedBmpClassTable =
        Objects.requireNonNull(bmpClassTable, "bmpClassTable");
    this.bmpClassWordCount = checkedBmpClassTable.classWordCount();
    this.bmpPageByHighByte = checkedBmpClassTable.pageByHighByte();
    this.bmpClassMasks = checkedBmpClassTable.masks();
    this.anchoredAsciiDfa = anchoredAsciiDfa;
    this.searchAsciiDfa = searchAsciiDfa;
  }

  private static long[] firstAsciiMask(
      byte[] op,
      int[] out1,
      int[] out2,
      int[] classId,
      long[] asciiLow,
      long[] asciiHigh,
      int startState) {
    boolean[] visited = new boolean[op.length];
    int[] stack = new int[Math.addExact(1, Math.multiplyExact(op.length, 2))];
    int size = 0;
    stack[size++] = startState;
    long low = 0L;
    long high = 0L;

    while (size != 0) {
      int state = stack[--size];
      if (state < 0 || visited[state]) continue;
      visited[state] = true;
      switch (op[state]) {
        case OP_CHAR -> {
          int id = classId[state];
          low |= asciiLow[id];
          high |= asciiHigh[id];
        }
        case OP_SPLIT -> {
          stack[size++] = out1[state];
          stack[size++] = out2[state];
        }
        case OP_JUMP,
            OP_ASSERT_START,
            OP_ASSERT_END,
            OP_ASSERT_WORD_BOUNDARY,
            OP_ASSERT_NOT_WORD_BOUNDARY -> stack[size++] = out1[state];
        case OP_MATCH -> {
          // No consuming first character on this path.
        }
        default -> throw new IllegalStateException("unknown regex opcode: " + op[state]);
      }
    }
    return new long[] {low, high};
  }

  private static int countOpcode(byte[] op, byte wanted) {
    int count = 0;
    for (byte actual : op) {
      if (actual == wanted) count++;
    }
    return count;
  }

  /** Compile the supported RE2-style subset, rejecting unsupported constructs. */
  public static MIndexRegexProgram compile(CharSequence expression) {
    return compile(expression, null);
  }

  /** Compile with caller-owned progress/cancellation, using a no-op monitor when null. */
  public static MIndexRegexProgram compile(
      CharSequence expression, IProgressMonitor monitor) {
    MIndexRegexProgram program =
        tryCompile(expression, Semantics.RE2, 100_000, monitor);
    if (program == null) {
      throw new IllegalArgumentException(
          "regex requires a backend outside the MIndex automaton subset");
    }
    return program;
  }

  /**
   * Attempt to compile the conservative subset. Returns {@code null} only for syntax/features that
   * must be delegated to another backend.
   */
  public static MIndexRegexProgram tryCompile(
      CharSequence expression, Semantics semantics, int maxStates) {
    return tryCompile(expression, semantics, maxStates, null);
  }

  /**
   * Attempt to compile with caller-owned progress/cancellation.
   *
   * <p>A null monitor preserves the allocation-free compatibility boundary by using the shared
   * no-op monitor.</p>
   */
  public static MIndexRegexProgram tryCompile(
      CharSequence expression,
      Semantics semantics,
      int maxStates,
      IProgressMonitor monitor) {
    return compileInternal(expression, semantics, maxStates, monitor, null, false);
  }

  private static MIndexRegexProgram compileInternal(CharSequence expression, Semantics semantics,
      int maxStates, IProgressMonitor monitor, CompilationSession session, boolean admit) {
    Objects.requireNonNull(expression, "expression");
    Objects.requireNonNull(semantics, "semantics");
    if (maxStates < 1) throw new IllegalArgumentException("maxStates must be positive");
    IProgressMonitor progress = monitor == null ? IProgressMonitor.noop() : monitor;
    PhaseCpu cpu = session != null && session.clock != null ? new PhaseCpu(session.clock) : null;
    boolean completed = false;
    String snapshot = expression.toString();
    progress.beginTask("compile MIndex regex", IProgressMonitor.UNKNOWN);
    try {
      progress.subTask("parse regex");
      progress.checkCanceled();
      ParsedSyntax syntax;
      if (session == null) {
        Parser parser = new Parser(snapshot, semantics, maxStates, progress);
        Node parsed = parser.parse();
        syntax = new ParsedSyntax(parsed, parser.classes, parser.captureCount,
            mayConsumeSurrogateUnit(parsed, parser.classes));
      } else syntax = session.parse(snapshot, semantics, maxStates, progress, admit);
      Node root = syntax.root;
      if (cpu != null) cpu.parsed();
      progress.subTask("lower primitive automaton");
      progress.checkCanceled();
      Compiler compiler = new Compiler(maxStates, progress);
      Fragment fragment = compiler.compile(root);
      int match = compiler.add(OP_MATCH, -1, -1, -1);
      compiler.patch(fragment.outs, match);
      if (cpu != null) cpu.lowered();
      progress.subTask("freeze character classes");
      progress.checkCanceled();
      ClassTable table = syntax.classes.freeze();
      BmpClassTable bmpClassTable =
          buildBmpClassMasks(table, progress);
      byte[] ops = compiler.ops();
      int[] first = compiler.out1();
      int[] second = compiler.out2();
      int[] classes = compiler.classIds();
      progress.subTask("precompute regex bit kernels");
      progress.checkCanceled();
      long[] asciiStateMasks =
          buildAsciiStateMasks(ops, classes, table, progress);
      EpsilonClosureTable epsilonClosures =
          EpsilonClosureTable.tryBuild(
              ops, first, second, MAX_EPSILON_CLOSURE_BYTES, progress);
      AsciiSubsetTable subsetTable =
          buildAsciiSubsetTransitions(
              ops,
              first,
              asciiStateMasks,
              (ops.length + 63) >>> 6,
              epsilonClosures,
              progress);
      progress.subTask("determinize bounded ASCII regex tables");
      progress.checkCanceled();
      AsciiDfaTable anchoredAsciiDfa =
          AsciiDfaTable.tryBuild(
              ops,
              first,
              fragment.start,
              match,
              asciiStateMasks,
              epsilonClosures,
              false,
              MAX_ASCII_DFA_STATES,
              MAX_ASCII_DFA_BYTES,
              progress);
      AsciiDfaTable searchAsciiDfa =
          AsciiDfaTable.tryBuild(
              ops,
              first,
              fragment.start,
              match,
              asciiStateMasks,
              epsilonClosures,
              true,
              MAX_ASCII_DFA_STATES,
              MAX_ASCII_DFA_BYTES,
              progress);
      progress.worked(Math.max(1, snapshot.length()));
      if (cpu != null) cpu.tabled();
      completed = true;
      return new MIndexRegexProgram(
          snapshot,
          semantics,
          ops,
          first,
          second,
          classes,
          table,
          fragment.start,
          match,
          root.minLength(),
          root.maxLength(),
          syntax.captures,
          root.requiredPresence64(),
          root.requiredCharacterFlags(),
          syntax.surrogate,
          asciiStateMasks,
          epsilonClosures,
          subsetTable,
          bmpClassTable,
          anchoredAsciiDfa,
          searchAsciiDfa);
    } catch (UnsupportedRegex ignored) {
      return null;
    } finally {
      if (cpu != null) session.lastTimings = cpu.finish(completed);
      progress.done();
    }
  }

  /** Per-attempt thread CPU; -1 means unavailable. Preparation includes parsing/remapping. */
  public record CompilationTimings(long parseCpuNanos, long loweringCpuNanos,
      long tableCpuNanos, long totalCpuNanos, boolean completed) {}

  /**
   * Caller-owned bounded primitive syntax arena. No programs, States, Fragments or patch lists are
   * retained. Expressions are evictable; canonical node/class IDs are private and append-only until
   * clear. Programs already returned own their arrays and remain valid after clear/close/eviction.
   * Calls are serialized; reentrant compilation or mutation from a progress callback is rejected.
   */
  public static final class CompilationSession implements AutoCloseable {
    public enum Policy { LRU, MRU }
    /** Fixed capacities and accounted bytes, excluding VM headers; see accountedBytes(). */
    public record Limits(int expressions, int nodes, int childLinks, int classes, int ranges,
        long accountedBytes) {
      public Limits {
        if (expressions < 0 || nodes < 0 || childLinks < 0 || classes < 0 || ranges < 0 || accountedBytes < 0)
          throw new IllegalArgumentException("negative syntax arena limit");
      }
    }
    public record Statistics(int expressions, int nodes, int classes, long accountedBytes,
        long hits, long bypasses, long evictions) {}
    private final Limits limits;
    private final Policy policy;
    private final com.synexia.common.collections.PackedLongLongOrderedMap expressionIndex;
    private final com.synexia.common.collections.PackedLongLongOrderedMap nodeIndex;
    private final com.synexia.common.collections.PackedLongLongOrderedMap classIndex;
    private final com.synexia.common.collections.PackedLongLongOrderedMap recency;
    private final String[] sources;
    private final int[][] sourceClasses;
    private final int[] sourceStates, sourceRoots, sourceCaptures, sourceNext;
    private final byte[] sourceSemantics;
    private final boolean[] sourceSurrogates;
    private final byte[] kinds;
    private final int[] argA, argB, childOffset, childCount, nodeNext, nodeFlags, children;
    private final long[] nodePresence;
    private final int[] classOffset, classCount, classNext;
    private final char[] lows, highs;
    private int nodeSize, childSize, classSize, rangeSize;
    private long sourceBytes, hits, bypasses, evictions;
    private boolean closed, busy;
    private final java.lang.management.ThreadMXBean clock;
    private CompilationTimings lastTimings;

    public CompilationSession(Limits limits, Policy policy) { this(limits, policy, false); }
    public CompilationSession(Limits limits, Policy policy, boolean measureCpu) {
      this.limits = Objects.requireNonNull(limits, "limits");
      this.policy = Objects.requireNonNull(policy, "policy");
      int e=limits.expressions, n=limits.nodes, c=limits.classes;
      expressionIndex = new com.synexia.common.collections.PackedLongLongOrderedMap(e);
      nodeIndex = new com.synexia.common.collections.PackedLongLongOrderedMap(n);
      classIndex = new com.synexia.common.collections.PackedLongLongOrderedMap(c);
      recency = new com.synexia.common.collections.PackedLongLongOrderedMap(e, true);
      sources=new String[e]; sourceClasses=new int[e][];
      sourceStates=new int[e]; sourceRoots=new int[e]; sourceCaptures=new int[e]; sourceNext=new int[e];
      sourceSemantics=new byte[e]; sourceSurrogates=new boolean[e];
      kinds=new byte[n]; argA=new int[n]; argB=new int[n]; childOffset=new int[n]; childCount=new int[n];
      nodeNext=new int[n]; nodeFlags=new int[n]; nodePresence=new long[n]; children=new int[limits.childLinks];
      classOffset=new int[c]; classCount=new int[c]; classNext=new int[c];
      lows=new char[limits.ranges]; highs=new char[limits.ranges];
      if (accountedBytes() > limits.accountedBytes) throw new IllegalArgumentException("budget below reserved metadata");
      clock=measureCpu ? java.lang.management.ManagementFactory.getThreadMXBean() : null;
    }

    /** Admit=false bypasses new retention, while permitting existing exact hits. */
    public synchronized MIndexRegexProgram tryCompile(CharSequence expression, Semantics semantics,
        int maxStates, IProgressMonitor monitor, boolean admit) {
      if (closed || busy) throw new IllegalStateException("closed or reentrant compilation session");
      busy=true;
      try { return compileInternal(expression, semantics, maxStates, monitor, this, admit); }
      finally { busy=false; }
    }
    public MIndexRegexProgram tryCompile(CharSequence expression, Semantics semantics,
        int maxStates, IProgressMonitor monitor) {
      return tryCompile(expression, semantics, maxStates, monitor, true);
    }
    /**
     * Exact documented accounting: all reserved primitive array payload plus eight bytes per
     * reference slot, two bytes per retained source UTF-16 unit, and four per source class ID.
     * Includes spare capacity and all four primitive indexes. Excludes VM object/array headers,
     * scalar fields, monitor/clock objects and transient compilation memory; not a heap-size claim.
     */
    public synchronized long accountedBytes() {
      return 34L*sources.length + 33L*kinds.length + 4L*children.length + 12L*classOffset.length
          + 4L*lows.length + expressionIndex.arrayPayloadBytes() + nodeIndex.arrayPayloadBytes()
          + classIndex.arrayPayloadBytes() + recency.arrayPayloadBytes() + sourceBytes;
    }
    public synchronized Statistics statistics() {
      return new Statistics(recency.size(),nodeSize,classSize,accountedBytes(),hits,bypasses,evictions);
    }
    public synchronized CompilationTimings lastTimings() { return lastTimings; }
    public synchronized void clear() {
      if (busy) throw new IllegalStateException("mutation during compilation");
      Arrays.fill(sources,null); Arrays.fill(sourceClasses,null);
      expressionIndex.clear();nodeIndex.clear();classIndex.clear();recency.clear();
      nodeSize=childSize=classSize=rangeSize=0;sourceBytes=hits=bypasses=evictions=0;lastTimings=null;
    }
    @Override public synchronized void close() { clear();closed=true; }

    private static long expressionHash(String text, Semantics semantics, int states) {
      return ((long)text.hashCode()<<32) ^ ((long)states<<1) ^ semantics.ordinal();
    }
    private ParsedSyntax parse(String text, Semantics semantics, int states, IProgressMonitor progress, boolean admit) {
      long hash=expressionHash(text,semantics,states);
      for(int slot=(int)expressionIndex.peekOrDefault(hash,-1);slot>=0;slot=sourceNext[slot]) {
        if(sourceStates[slot]==states && sourceSemantics[slot]==semantics.ordinal() && sources[slot].equals(text)) {
          recency.getOrDefault(slot,0);hits++;return restore(slot,progress);
        }
      }
      Parser parser=new Parser(text,semantics,states,progress);
      Node root=parser.parse();
      ParsedSyntax exact=new ParsedSyntax(root,parser.classes,parser.captureCount,mayConsumeSurrogateUnit(root,parser.classes));
      long bytes=2L*text.length()+4L*parser.classes.specs.size();
      if(!admit || sources.length==0 || bytes>limits.accountedBytes-(accountedBytes()-sourceBytes)) {bypasses++;return exact;}
      try {
        int[] ids=new int[parser.classes.specs.size()];
        for(int i=0;i<ids.length;i++){progress.checkCanceled();ids[i]=internClass(parser.classes.specs.get(i));}
        int canonical=internNode(root,ids,progress);
        while(recency.size()>=sources.length || bytes>limits.accountedBytes-accountedBytes()) evict();
        int slot=0;while(sources[slot]!=null)slot++;
        sources[slot]=text;sourceClasses[slot]=ids;sourceStates[slot]=states;sourceRoots[slot]=canonical;
        sourceCaptures[slot]=parser.captureCount;sourceSurrogates[slot]=exact.surrogate;sourceSemantics[slot]=(byte)semantics.ordinal();
        sourceNext[slot]=(int)expressionIndex.peekOrDefault(hash,-1);expressionIndex.put(hash,slot);
        sourceBytes+=bytes;recency.put(slot,bytes);
      } catch(SharingLimit exhausted){bypasses++;}
      return exact;
    }
    private void evict() {
      int slot=(int)(policy==Policy.LRU?recency.firstKey():recency.lastKey());
      long hash=expressionHash(sources[slot],Semantics.values()[sourceSemantics[slot]],sourceStates[slot]);
      int at=(int)expressionIndex.peekOrDefault(hash,-1),previous=-1;
      while(at!=slot){previous=at;at=sourceNext[at];}
      if(previous>=0)sourceNext[previous]=sourceNext[slot];
      else if(sourceNext[slot]<0)expressionIndex.remove(hash);else expressionIndex.put(hash,sourceNext[slot]);
      sourceBytes-=recency.peekOrDefault(slot,0);recency.remove(slot);
      sources[slot]=null;sourceClasses[slot]=null;evictions++;
    }
    private int internClass(ClassSpec spec) {
      int hash=31*Arrays.hashCode(spec.lows)+Arrays.hashCode(spec.highs);
      for(int id=(int)classIndex.peekOrDefault(hash,-1);id>=0;id=classNext[id]) {
        if(classCount[id]!=spec.lows.length)continue;
        boolean equal=true;for(int i=0;i<classCount[id];i++)if(lows[classOffset[id]+i]!=spec.lows[i]||highs[classOffset[id]+i]!=spec.highs[i]){equal=false;break;}
        if(equal)return id;
      }
      if(classSize==classOffset.length || spec.lows.length>lows.length-rangeSize)throw new SharingLimit();
      int id=classSize++;classOffset[id]=rangeSize;classCount[id]=spec.lows.length;
      System.arraycopy(spec.lows,0,lows,rangeSize,spec.lows.length);System.arraycopy(spec.highs,0,highs,rangeSize,spec.highs.length);rangeSize+=spec.lows.length;
      classNext[id]=(int)classIndex.peekOrDefault(hash,-1);classIndex.put(hash,id);return id;
    }
    private int internNode(Node node,int[] classes,IProgressMonitor progress) {
      progress.checkCanceled();int kind,a=0,b=0;List<Node> childNodes=List.of();
      switch(node) {
        case EmptyNode ignored -> kind=0;
        case AtomNode atom -> {kind=1;a=classes[atom.classId];}
        case QuotedNode quoted -> {kind=2;childNodes=quoted.children;}
        case StartNode ignored -> kind=3;
        case EndNode ignored -> kind=4;
        case WordBoundaryNode boundary -> {kind=5;a=boundary.boundary?1:0;}
        case ConcatNode concat -> {kind=6;childNodes=concat.children;}
        case AltNode alt -> {kind=7;childNodes=alt.children;}
        case RepeatNode repeat -> {kind=8;a=repeat.min;b=repeat.max;childNodes=List.of(repeat.child);}
      }
      int[] edges=new int[childNodes.size()];for(int i=0;i<edges.length;i++)edges[i]=internNode(childNodes.get(i),classes,progress);
      int flags=node.requiredCharacterFlags();long presence=node.requiredPresence64();
      int hash=31*(31*(31*(31*kind+a)+b)+flags)+Long.hashCode(presence);hash=31*hash+Arrays.hashCode(edges);
      for(int id=(int)nodeIndex.peekOrDefault(hash,-1);id>=0;id=nodeNext[id]) {
        if(kinds[id]!=kind||argA[id]!=a||argB[id]!=b||nodeFlags[id]!=flags||nodePresence[id]!=presence||childCount[id]!=edges.length)continue;
        boolean equal=true;for(int i=0;i<edges.length;i++)if(children[childOffset[id]+i]!=edges[i]){equal=false;break;}
        if(equal)return id;
      }
      if(nodeSize==kinds.length||edges.length>children.length-childSize)throw new SharingLimit();
      int id=nodeSize++;kinds[id]=(byte)kind;argA[id]=a;argB[id]=b;nodeFlags[id]=flags;nodePresence[id]=presence;
      childOffset[id]=childSize;childCount[id]=edges.length;System.arraycopy(edges,0,children,childSize,edges.length);childSize+=edges.length;
      nodeNext[id]=(int)nodeIndex.peekOrDefault(hash,-1);nodeIndex.put(hash,id);return id;
    }
    private ParsedSyntax restore(int slot,IProgressMonitor progress) {
      ClassTableBuilder local=new ClassTableBuilder();int[] remap=new int[classSize];Arrays.fill(remap,-1);
      for(int canonical:sourceClasses[slot]) {
        progress.checkCanceled();int start=classOffset[canonical],end=start+classCount[canonical];
        remap[canonical]=local.add(new ClassSpec(Arrays.copyOfRange(lows,start,end),Arrays.copyOfRange(highs,start,end)));
      }
      return new ParsedSyntax(restoreNode(sourceRoots[slot],remap,new Node[nodeSize],progress),local,sourceCaptures[slot],sourceSurrogates[slot]);
    }
    private Node restoreNode(int id,int[] remap,Node[] memo,IProgressMonitor progress) {
      progress.checkCanceled();if(memo[id]!=null)return memo[id];
      List<Node> childNodes=new ArrayList<>(childCount[id]);
      for(int i=0;i<childCount[id];i++)childNodes.add(restoreNode(children[childOffset[id]+i],remap,memo,progress));
      Node result=switch(kinds[id]) {
        case 0 -> new EmptyNode();
        case 1 -> {int local=remap[argA[id]];if(local<0)throw new IllegalStateException("unmapped syntax class");yield new AtomNode(local,nodePresence[id],nodeFlags[id]);}
        case 2 -> new QuotedNode(childNodes);
        case 3 -> new StartNode();case 4 -> new EndNode();case 5 -> new WordBoundaryNode(argA[id]!=0);
        case 6 -> new ConcatNode(childNodes);case 7 -> new AltNode(childNodes);
        case 8 -> new RepeatNode(childNodes.getFirst(),argA[id],argB[id]);
        default -> throw new IllegalStateException("unknown syntax kind");
      };
      memo[id]=result;return result;
    }
  }
  private record ParsedSyntax(Node root,ClassTableBuilder classes,int captures,boolean surrogate) {}
  private static final class SharingLimit extends RuntimeException {
    private static final long serialVersionUID=1L;
    SharingLimit(){super(null,null,false,false);}
  }
  private static final class PhaseCpu {
    private final java.lang.management.ThreadMXBean clock;
    private final long start;
    private long phaseStart,parse=-1,lower=-1,tables=-1;
    PhaseCpu(java.lang.management.ThreadMXBean clock){this.clock=clock;start=phaseStart=now();}
    private long now(){return clock.isCurrentThreadCpuTimeSupported()&&clock.isThreadCpuTimeEnabled()?clock.getCurrentThreadCpuTime():-1;}
    private long elapsed(){long end=now(),value=phaseStart<0||end<0?-1:end-phaseStart;phaseStart=end;return value;}
    void parsed(){parse=elapsed();}void lowered(){lower=elapsed();}void tabled(){tables=elapsed();}
    CompilationTimings finish(boolean completed){long end=now();return new CompilationTimings(parse,lower,tables,start<0||end<0?-1:end-start,completed);}
  }

  /** Freeze this already-compiled program into a portable primitive image. */
  public Image image() {
    return new Image(
        expression,
        semantics,
        op,
        out1,
        out2,
        classId,
        asciiLow,
        asciiHigh,
        rangeOffset,
        rangeCount,
        rangeLow,
        rangeHigh,
        startState,
        matchState,
        minLength,
        maxLength,
        captureCount,
        requiredPresence64,
        requiredCharacterFlags,
        mayConsumeSurrogateUnit);
  }

  /** Complete execution tables plus the dispatch metadata computed by the cold compiler. */
  public record PrecomputedImage(ExecutionImage execution, long firstAsciiLow,
                                 long firstAsciiHigh, int branchingStateCount) {
    public PrecomputedImage {
      Objects.requireNonNull(execution, "execution");
      if (branchingStateCount < 0 || branchingStateCount > execution.program().op().length) {
        throw new IllegalArgumentException("precomputed branching count");
      }
    }
  }

  /** Freeze dispatch metadata along with all existing execution tables. */
  public PrecomputedImage precomputedImage() {
    return new PrecomputedImage(executionImage(), firstAsciiLow, firstAsciiHigh, branchingStateCount);
  }

  /** Restore without parsing, table construction, first-character traversal or opcode counting. */
  public static MIndexRegexProgram fromPrecomputedImage(PrecomputedImage image) {
    PrecomputedImage checked = Objects.requireNonNull(image, "image");
    return fromExecutionImage(checked.execution(), checked);
  }

  /** Freeze this program including every bounded derived execution table. */
  public ExecutionImage executionImage() {
    return new ExecutionImage(
        image(),
        asciiStateMasks,
        epsilonClosures == null ? 0 : epsilonClosures.wordsPerRow,
        epsilonClosures == null ? new int[0] : epsilonClosures.rowByState,
        epsilonClosures == null ? new long[0] : epsilonClosures.terminals,
        asciiSubsetBits,
        sourceSubsetCount,
        asciiSubsetTransitions,
        bmpClassWordCount,
        bmpPageByHighByte,
        bmpClassMasks,
        anchoredAsciiDfa == null ? null : anchoredAsciiDfa.image(),
        searchAsciiDfa == null ? null : searchAsciiDfa.image());
  }

  /**
   * Restore a fully precomputed execution image without reconstructing any derived table.
   */
  public static MIndexRegexProgram fromExecutionImage(ExecutionImage image) {
    return fromExecutionImage(image, null);
  }

  private static MIndexRegexProgram fromExecutionImage(ExecutionImage image,
                                                       PrecomputedImage precomputed) {
    ExecutionImage checked = Objects.requireNonNull(image, "image");
    Image program = checked.program();
    byte[] ops = program.op();
    int[] first = program.out1();
    int[] second = program.out2();
    int[] classIds = program.classId();
    ClassTable classes =
        new ClassTable(
            program.asciiLow(),
            program.asciiHigh(),
            program.rangeOffset(),
            program.rangeCount(),
            program.rangeLow(),
            program.rangeHigh());

    EpsilonClosureTable epsilon =
        checked.epsilonWordsPerRow() == 0
            ? null
            : new EpsilonClosureTable(
                checked.epsilonWordsPerRow(),
                checked.epsilonRowByState(),
                checked.epsilonTerminals());
    AsciiSubsetTable subset =
        checked.asciiSubsetBits() == 0
            ? AsciiSubsetTable.NONE
            : new AsciiSubsetTable(
                checked.asciiSubsetBits(),
                checked.asciiSubsetSourceCount(),
                checked.asciiSubsetTransitions());
    BmpClassTable bmp =
        checked.bmpClassWordCount() == 0
            ? BmpClassTable.NONE
            : new BmpClassTable(
                checked.bmpClassWordCount(),
                checked.bmpPageByHighByte(),
                checked.bmpClassMasks());

    return new MIndexRegexProgram(
        program.expression(),
        program.semantics(),
        ops,
        first,
        second,
        classIds,
        classes,
        program.startState(),
        program.matchState(),
        program.minLength(),
        program.maxLength(),
        program.captureCount(),
        program.requiredPresence64(),
        program.requiredCharacterFlags(),
        program.mayConsumeSurrogateUnit(),
        checked.asciiStateMasks(),
        epsilon,
        subset,
        bmp,
        AsciiDfaTable.fromImage(checked.anchoredAsciiDfa()),
        AsciiDfaTable.fromImage(checked.searchAsciiDfa()), precomputed);
  }

  /** Restore an immutable program from compiler-generated primitive data with no regex parsing. */
  public static MIndexRegexProgram fromImage(Image image) {
    return fromImage(image, null);
  }

  /** Restore an immutable program while reporting/canceling derived-table reconstruction. */
  public static MIndexRegexProgram fromImage(
      Image image, IProgressMonitor monitor) {
    Image checked = Objects.requireNonNull(image, "image");
    IProgressMonitor progress = monitor == null ? IProgressMonitor.noop() : monitor;
    progress.beginTask("restore MIndex regex image", IProgressMonitor.UNKNOWN);
    try {
      progress.subTask("restore primitive regex lanes");
      progress.checkCanceled();
      byte[] ops = checked.op();
      int[] first = checked.out1();
      int[] second = checked.out2();
      int[] classIds = checked.classId();
      ClassTable classes =
          new ClassTable(
              checked.asciiLow(),
              checked.asciiHigh(),
              checked.rangeOffset(),
              checked.rangeCount(),
              checked.rangeLow(),
              checked.rangeHigh());

      progress.subTask("rebuild regex direct-address kernels");
      progress.checkCanceled();
      BmpClassTable bmpClassTable =
          buildBmpClassMasks(classes, progress);
      long[] asciiStateMasks =
          buildAsciiStateMasks(ops, classIds, classes, progress);
      EpsilonClosureTable epsilonClosures =
          EpsilonClosureTable.tryBuild(
              ops, first, second, MAX_EPSILON_CLOSURE_BYTES, progress);
      AsciiSubsetTable subsetTable =
          buildAsciiSubsetTransitions(
              ops,
              first,
              asciiStateMasks,
              (ops.length + 63) >>> 6,
              epsilonClosures,
              progress);
      AsciiDfaTable anchoredAsciiDfa =
          AsciiDfaTable.tryBuild(
              ops,
              first,
              checked.startState(),
              checked.matchState(),
              asciiStateMasks,
              epsilonClosures,
              false,
              MAX_ASCII_DFA_STATES,
              MAX_ASCII_DFA_BYTES,
              progress);
      AsciiDfaTable searchAsciiDfa =
          AsciiDfaTable.tryBuild(
              ops,
              first,
              checked.startState(),
              checked.matchState(),
              asciiStateMasks,
              epsilonClosures,
              true,
              MAX_ASCII_DFA_STATES,
              MAX_ASCII_DFA_BYTES,
              progress);
      progress.worked(Math.max(1, ops.length));
      return new MIndexRegexProgram(
          checked.expression(),
          checked.semantics(),
          ops,
          first,
          second,
          classIds,
          classes,
          checked.startState(),
          checked.matchState(),
          checked.minLength(),
          checked.maxLength(),
          checked.captureCount(),
          checked.requiredPresence64(),
          checked.requiredCharacterFlags(),
          checked.mayConsumeSurrogateUnit(),
          asciiStateMasks,
          epsilonClosures,
          subsetTable,
          bmpClassTable,
          anchoredAsciiDfa,
          searchAsciiDfa);
    } finally {
      progress.done();
    }
  }

  public String expression() {
    return expression;
  }

  public Semantics semantics() {
    return semantics;
  }

  public int stateCount() {
    return op.length;
  }

  public int characterClassCount() {
    return asciiLow.length;
  }

  public int captureCount() {
    return captureCount;
  }

  /**
   * Conservative trigram candidate query derived from the same regex parser used by this program.
   *
   * <p>The query is lazily prepared because direct single-value matching does not need an inverted
   * index. It is a necessary condition only: false rejects safely; true still requires this program
   * (or its configured fallback) as the semantic verifier.</p>
   */
  public MIndexRegexTrigramQuery trigramQuery() {
    MIndexRegexTrigramQuery prepared = trigramQuery;
    if (prepared != null) return prepared;
    synchronized (this) {
      prepared = trigramQuery;
      if (prepared != null) return prepared;
      try {
        Parser parser =
            new Parser(
                expression,
                semantics,
                Integer.MAX_VALUE - 8,
                IProgressMonitor.noop());
        Node root = parser.parse();
        prepared = trigramQuery(root, parser.classes);
      } catch (UnsupportedRegex unsupported) {
        // A restored image can outlive parser-surface evolution. Candidate optimization must
        // always fail open rather than changing or disabling authoritative regex execution.
        prepared = MIndexRegexTrigramQuery.all();
      }
      trigramQuery = prepared;
      return prepared;
    }
  }

  /**
   * Necessary UTF-16 presence signal proven from the compiled regex AST.
   *
   * <p>Concatenation unions requirements, alternation intersects branch requirements, zero-minimum
   * repetition contributes none, and positive repetition preserves its child proof. Character
   * classes contribute only signal bits shared by every unit they can match. A missing bit proves
   * the regex cannot match; matching bits are never treated as proof because the compact signal
   * intentionally permits collisions.</p>
   */
  public long requiredPresence64() {
    return requiredPresence64;
  }

  /** Planner-safe character-class presence flags required on every accepting path. */
  public int requiredCharacterFlags() {
    return requiredCharacterFlags;
  }

  /**
   * Whether any native character class can consume a UTF-16 surrogate code unit.
   *
   * <p>Only these programs need the supplementary-code-point fallback. Literal/positive ASCII
   * classes cannot consume either half of a surrogate pair, so their boolean execution remains
   * equivalent while scanning the original UTF-16 sequence directly.</p>
   */
  public boolean mayConsumeSurrogateUnit() {
    return mayConsumeSurrogateUnit;
  }

  /** ASCII code units that may be consumed by the first consuming state on an accepting path. */
  public long firstAsciiLow() {
    return firstAsciiLow;
  }

  /** ASCII code units 64..127 that may be consumed first on an accepting path. */
  public long firstAsciiHigh() {
    return firstAsciiHigh;
  }

  public int branchingStateCount() {
    return branchingStateCount;
  }

  /** Structural single-path signal analogous to a regex tree determinism study. */
  public boolean structurallyDeterministic() {
    return branchingStateCount == 0;
  }

  public boolean nullable() {
    return minLength == 0;
  }

  public boolean firstAsciiAllows(char value) {
    if (value >= ASCII_CARDINALITY || minLength == 0) return true;
    return value < Long.SIZE
        ? (firstAsciiLow & (1L << value)) != 0L
        : (firstAsciiHigh & (1L << (value - Long.SIZE))) != 0L;
  }

  public int minLength() {
    return minLength;
  }

  /** Maximum UTF-16 length, or -1 when unbounded. */
  public int maxLength() {
    return maxLength;
  }

  public long primitivePayloadBytes() {
    return op.length
        + Integer.BYTES * (long) (out1.length + out2.length + classId.length)
        + Long.BYTES
            * (long)
                (asciiLow.length
                    + asciiHigh.length
                    + asciiStateMasks.length
                    + asciiSubsetTransitions.length
                    + bmpClassMasks.length)
        + Integer.BYTES
            * (long)
                (rangeOffset.length
                    + rangeCount.length
                    + bmpPageByHighByte.length)
        + Character.BYTES * (long) (rangeLow.length + rangeHigh.length)
        + epsilonClosurePayloadBytes()
        + asciiDfaPayloadBytes();
  }

  /** True when ASCII input can filter active character states by word-wise bitset AND. */
  public boolean hasAsciiStateMasks() {
    return asciiStateMasks.length != 0;
  }

  public long asciiStateMaskPayloadBytes() {
    return Long.BYTES * (long) asciiStateMasks.length;
  }

  /**
   * True when the assertion-free ASCII hot path can map an adaptive 8-state or 4-state active
   * source subset through one direct-address transition row.
   */
  public boolean hasAsciiSubsetTransitions() {
    return asciiSubsetTransitions.length != 0;
  }

  /** Number of source-state bits encoded in one direct-address subset key, or zero when absent. */
  public int asciiSubsetBits() {
    return asciiSubsetBits;
  }

  /** Retained bytes of the bounded direct-address ASCII subset transition table. */
  public long asciiSubsetTransitionPayloadBytes() {
    return Long.BYTES * (long) asciiSubsetTransitions.length;
  }

  /** True when non-ASCII BMP class membership can use a two-level direct-address bit table. */
  public boolean hasBmpClassMasks() {
    return bmpClassMasks.length != 0;
  }

  /** Retained bytes of the high-byte page map plus dense BMP class-bit pages. */
  public long bmpClassMaskPayloadBytes() {
    return Integer.BYTES * (long) bmpPageByHighByte.length
        + Long.BYTES * (long) bmpClassMasks.length;
  }

  /**
   * True when assertion-free epsilon closures were precomputed under the fixed memory budget.
   *
   * <p>Programs with positional assertions or closure tables above the budget retain the existing
   * bounded stack walker; semantics never depend on this optimization.</p>
   */
  public boolean hasPrecomputedEpsilonClosures() {
    return epsilonClosures != null;
  }

  public long epsilonClosurePayloadBytes() {
    return epsilonClosures == null ? 0L : epsilonClosures.payloadBytes();
  }

  /** True when bounded assertion-free ASCII determinization succeeded. */
  public boolean hasAsciiDfa() {
    return anchoredAsciiDfa != null;
  }

  /** Number of retained anchored DFA states, or zero when determinization fell back. */
  public int asciiDfaStateCount() {
    return anchoredAsciiDfa == null ? 0 : anchoredAsciiDfa.stateCount();
  }

  /** Retained bytes of anchored and unanchored DFA tables. */
  public long asciiDfaPayloadBytes() {
    long anchored = anchoredAsciiDfa == null ? 0L : anchoredAsciiDfa.payloadBytes();
    long search = searchAsciiDfa == null ? 0L : searchAsciiDfa.payloadBytes();
    return anchored + search;
  }

  /** Defensive anchored DFA snapshot for native/GPU MATCHES and LOOKING_AT execution. */
  public AsciiDfaImage anchoredAsciiDfaImage() {
    return anchoredAsciiDfa == null ? null : anchoredAsciiDfa.image();
  }

  /** Defensive restart/search DFA snapshot for native/GPU FIND execution. */
  public AsciiDfaImage searchAsciiDfaImage() {
    return searchAsciiDfa == null ? null : searchAsciiDfa.image();
  }

  /**
   * Whether this input can execute on the primitive UTF-16 machine without changing regex truth.
   *
   * <p>Programs whose character classes cannot consume a surrogate code unit are transparent to
   * surrogate pairs: the two UTF-16 units simply fail those atoms and matching continues around
   * them, so literals and positive ASCII classes can remain native even when emoji are present.
   * Programs that could consume a surrogate unit (dot, negated classes, or ranges crossing the
   * surrogate block) require single-unit code points and otherwise delegate to RE2/J or the JDK.</p>
   */
  public boolean supportsInput(CharSequence input) {
    Objects.requireNonNull(input, "input");
    if (!mayConsumeSurrogateUnit) return true;
    if (input instanceof MIndexString indexed) {
      return supportsMetrics(indexed.metrics());
    }
    if (input instanceof IndexLexiconWordView word) {
      return supportsMetrics(word.metrics());
    }
    if (input instanceof IndexUtf16SearchImage.RowView row
        && row.hasPrecomputedRegexFacts()) {
      return supportsMetrics(row.metrics());
    }
    for (int i = 0; i < input.length(); i++) {
      if (Character.isSurrogate(input.charAt(i))) return false;
    }
    return true;
  }

  private static boolean supportsMetrics(IndexTextMetrics metrics) {
    return metrics.utf16Length() == metrics.codePointCount()
        && metrics.unpairedSurrogateCount() == 0;
  }

  /** Full-region match with no String materialization and no backtracking. */
  public boolean matches(CharSequence input) {
    Objects.requireNonNull(input, "input");
    requireSupportedInput(input);
    int decision = fastMatches(input);
    if (decision != DFA_UNSUPPORTED) return decision != 0;
    return matchesNfa(input, new Scratch(op.length, epsilonClosures == null));
  }

  private boolean matches(CharSequence input, Scratch scratch) {
    Objects.requireNonNull(input, "input");
    requireSupportedInput(input);
    int decision = fastMatches(input);
    if (decision != DFA_UNSUPPORTED) return decision != 0;
    return matchesNfa(input, scratch);
  }

  private int fastMatches(CharSequence input) {
    int length = input.length();
    if (length < minLength || (maxLength >= 0 && length > maxLength)) return 0;
    if (!requiredFactsPresent(input)) return 0;
    if (anchoredAsciiDfa == null) return DFA_UNSUPPORTED;
    int ascii = asciiKnowledge(input);
    if (ascii == 0) return DFA_UNSUPPORTED;
    return ascii > 0 ? anchoredAsciiDfa.matchesKnownAscii(input) : anchoredAsciiDfa.matches(input);
  }

  private boolean matchesNfa(CharSequence input, Scratch scratch) {
    int length = input.length();
    scratch.reset();
    scratch.beginClosure();
    addClosure(scratch.active, startState, 0, input, scratch);
    for (int position = 0; position < length; position++) {
      step(scratch, input.charAt(position), position + 1, input);
      if (scratch.isEmpty()) return false;
    }
    return scratch.contains(matchState);
  }

  /** Prefix match equivalent to Matcher.lookingAt() for the accepted subset. */
  public boolean lookingAt(CharSequence input) {
    Objects.requireNonNull(input, "input");
    requireSupportedInput(input);
    int decision = fastLookingAt(input);
    if (decision != DFA_UNSUPPORTED) return decision != 0;
    return lookingAtNfa(input, new Scratch(op.length, epsilonClosures == null));
  }

  private boolean lookingAt(CharSequence input, Scratch scratch) {
    Objects.requireNonNull(input, "input");
    requireSupportedInput(input);
    int decision = fastLookingAt(input);
    if (decision != DFA_UNSUPPORTED) return decision != 0;
    return lookingAtNfa(input, scratch);
  }

  private int fastLookingAt(CharSequence input) {
    if (input.length() < minLength) return 0;
    if (!requiredFactsPresent(input)) return 0;
    if (anchoredAsciiDfa == null) return DFA_UNSUPPORTED;
    int ascii = asciiKnowledge(input);
    if (ascii == 0) return DFA_UNSUPPORTED;
    return ascii > 0
        ? anchoredAsciiDfa.lookingAtKnownAscii(input)
        : anchoredAsciiDfa.lookingAt(input);
  }

  private boolean lookingAtNfa(CharSequence input, Scratch scratch) {
    int length = input.length();
    scratch.reset();
    scratch.beginClosure();
    addClosure(scratch.active, startState, 0, input, scratch);
    if (scratch.contains(matchState)) return true;
    for (int position = 0; position < length; position++) {
      step(scratch, input.charAt(position), position + 1, input);
      if (scratch.contains(matchState)) return true;
      if (scratch.isEmpty()) return false;
    }
    return false;
  }

  /** Linear scan with NFA restart at each position; no per-start backtracking or substring copy. */
  public boolean find(CharSequence input) {
    Objects.requireNonNull(input, "input");
    requireSupportedInput(input);
    int decision = fastFind(input);
    if (decision != DFA_UNSUPPORTED) return decision != 0;
    return findNfa(input, new Scratch(op.length, epsilonClosures == null));
  }

  private boolean find(CharSequence input, Scratch scratch) {
    Objects.requireNonNull(input, "input");
    requireSupportedInput(input);
    int decision = fastFind(input);
    if (decision != DFA_UNSUPPORTED) return decision != 0;
    return findNfa(input, scratch);
  }

  private int fastFind(CharSequence input) {
    if (input.length() < minLength) return 0;
    if (!requiredFactsPresent(input)) return 0;
    if (searchAsciiDfa == null) return DFA_UNSUPPORTED;
    int ascii = asciiKnowledge(input);
    if (ascii == 0) return DFA_UNSUPPORTED;
    return ascii > 0 ? searchAsciiDfa.findKnownAscii(input) : searchAsciiDfa.find(input);
  }

  private boolean findNfa(CharSequence input, Scratch scratch) {
    int length = input.length();
    scratch.reset();
    for (int position = 0; position <= length; position++) {
      scratch.beginClosure();
      addClosure(scratch.active, startState, position, input, scratch);
      if (scratch.contains(matchState)) return true;
      if (position == length) break;
      step(scratch, input.charAt(position), position + 1, input);
      if (scratch.contains(matchState)) return true;
    }
    return false;
  }

  /**
   * Allocate one primitive execution workspace for repeated calls.
   *
   * <p>The compiled program remains immutable and thread-safe. A workspace is intentionally
   * thread-confined and not thread-safe; callers doing hot repeated matching should keep one per
   * worker rather than allocate NFA scratch lanes per value.</p>
   */
  public Workspace workspace() {
    return new Workspace(this);
  }

  /**
   * Returns 1 when precomputed facts prove ASCII, 0 when they prove non-ASCII, and -1 when unknown.
   */
  private static int asciiKnowledge(CharSequence input) {
    if (input instanceof IndexLexiconWordView word) {
      if (!word.lexicon().hasPrecomputedRegexFacts()) return -1;
      return (word.characterFlags() & IndexCharacterFlags.ASCII) != 0 ? 1 : 0;
    }
    if (input instanceof IndexUtf16SearchImage.RowView row) {
      if (!row.hasPrecomputedRegexFacts()) return -1;
      return (row.characterFlags() & IndexCharacterFlags.ASCII) != 0 ? 1 : 0;
    }
    if (!(input instanceof MIndexString indexed)
        || !(indexed.resolver() instanceof IndexCharacterFlagsResolver flags)) {
      return -1;
    }
    for (int token = 0; token < indexed.tokenCount(); token++) {
      if ((flags.characterFlags(indexed.tokenIdAt(token)) & IndexCharacterFlags.ASCII) == 0) {
        return 0;
      }
    }
    return 1;
  }

  private boolean requiredFactsPresent(CharSequence input) {
    if (requiredPresence64 != 0L) {
      long signal;
      if (input instanceof MIndexString indexed) {
        signal = indexed.bitSignal64();
      } else if (input instanceof IndexLexiconWordView word
          && word.lexicon().hasPrecomputedRegexFacts()) {
        signal = word.bitSignal64();
      } else if (input instanceof IndexUtf16SearchImage.RowView row
          && row.hasPrecomputedRegexFacts()) {
        signal = row.bitSignal64();
      } else {
        signal = -1L;
      }
      if (signal != -1L && !MIndexStringBitOps.mayContain(signal, requiredPresence64)) {
        return false;
      }
    }

    if (requiredCharacterFlags != 0) {
      int available = characterFlags(input);
      if (available >= 0
          && (available & requiredCharacterFlags) != requiredCharacterFlags) {
        return false;
      }
    }
    return true;
  }

  private static int characterFlags(CharSequence input) {
    if (input instanceof IndexLexiconWordView word) {
      return word.lexicon().hasPrecomputedRegexFacts() ? word.characterFlags() : -1;
    }
    if (input instanceof IndexUtf16SearchImage.RowView row) {
      return row.hasPrecomputedRegexFacts() ? row.characterFlags() : -1;
    }
    if (!(input instanceof MIndexString indexed)
        || !(indexed.resolver() instanceof IndexCharacterFlagsResolver flags)) {
      return -1;
    }

    int available = 0;
    for (int token = 0; token < indexed.tokenCount(); token++) {
      available |= flags.characterFlags(indexed.tokenIdAt(token));
    }
    return available;
  }

  private void requireSupportedInput(CharSequence input) {
    if (!supportsInput(input)) {
      throw new IllegalArgumentException(
          "primitive MIndex regex requires single-unit UTF-16 input; delegate supplementary text");
    }
  }

  private void step(
      Scratch scratch, char value, int nextPosition, CharSequence input) {
    Arrays.fill(scratch.next, 0L);
    scratch.beginClosure();
    if (value < ASCII_CARDINALITY && asciiSubsetTransitions.length != 0) {
      stepAsciiSubsetTable(scratch, value);
    } else if (value < ASCII_CARDINALITY && asciiStateMasks.length != 0) {
      int maskBase = value * stateWordCount;
      for (int word = 0; word < scratch.active.length; word++) {
        long bits = scratch.active[word] & asciiStateMasks[maskBase + word];
        while (bits != 0L) {
          int bit = Long.numberOfTrailingZeros(bits);
          int state = (word << 6) + bit;
          bits &= bits - 1;
          addClosure(scratch.next, out1[state], nextPosition, input, scratch);
        }
      }
    } else {
      for (int word = 0; word < scratch.active.length; word++) {
        long bits = scratch.active[word];
        while (bits != 0L) {
          int bit = Long.numberOfTrailingZeros(bits);
          int state = (word << 6) + bit;
          bits &= bits - 1;
          if (state >= op.length || op[state] != OP_CHAR) continue;
          if (classMatches(classId[state], value)) {
            addClosure(scratch.next, out1[state], nextPosition, input, scratch);
          }
        }
      }
    }
    long[] swap = scratch.active;
    scratch.active = scratch.next;
    scratch.next = swap;
  }

  private void addClosure(
      long[] target, int initial, int position, CharSequence input, Scratch scratch) {
    if (initial < 0) return;
    if (epsilonClosures != null) {
      epsilonClosures.orInto(initial, target);
      return;
    }
    int length = input.length();
    if (scratch.mark[initial] == scratch.stamp) return;
    int top = 0;
    scratch.mark[initial] = scratch.stamp;
    scratch.stack[top++] = initial;
    while (top != 0) {
      int state = scratch.stack[--top];
      switch (op[state]) {
        case OP_SPLIT -> {
          top = pushClosureState(scratch, top, out2[state]);
          top = pushClosureState(scratch, top, out1[state]);
        }
        case OP_JUMP -> top = pushClosureState(scratch, top, out1[state]);
        case OP_ASSERT_START -> {
          if (position == 0) top = pushClosureState(scratch, top, out1[state]);
        }
        case OP_ASSERT_END -> {
          if (position == length) top = pushClosureState(scratch, top, out1[state]);
        }
        case OP_ASSERT_WORD_BOUNDARY -> {
          if (isAsciiWordBoundary(input, position)) {
            top = pushClosureState(scratch, top, out1[state]);
          }
        }
        case OP_ASSERT_NOT_WORD_BOUNDARY -> {
          if (!isAsciiWordBoundary(input, position)) {
            top = pushClosureState(scratch, top, out1[state]);
          }
        }
        case OP_CHAR, OP_MATCH -> set(target, state);
        default -> throw new IllegalStateException("unknown regex opcode " + op[state]);
      }
    }
  }

  private static int pushClosureState(Scratch scratch, int top, int state) {
    if (state < 0 || scratch.mark[state] == scratch.stamp) return top;
    scratch.mark[state] = scratch.stamp;
    scratch.stack[top] = state;
    return top + 1;
  }

  private static boolean isAsciiWordBoundary(CharSequence input, int position) {
    boolean left = position > 0 && isAsciiWord(input.charAt(position - 1));
    boolean right = position < input.length() && isAsciiWord(input.charAt(position));
    return left != right;
  }

  private static boolean isAsciiWord(char value) {
    return (value >= 'A' && value <= 'Z')
        || (value >= 'a' && value <= 'z')
        || (value >= '0' && value <= '9')
        || value == '_';
  }

  private void stepAsciiSubsetTable(Scratch scratch, char value) {
    int rowWords = stateWordCount;
    int subsetCardinality = 1 << asciiSubsetBits;
    int sourceStride = subsetCardinality * rowWords;
    int characterStride = sourceSubsetCount * sourceStride;
    int characterBase = value * characterStride;

    for (int sourceSubset = 0; sourceSubset < sourceSubsetCount; sourceSubset++) {
      int sourceWord = sourceSubset / sourceSubsetsPerWord;
      int shift =
          (sourceSubset % sourceSubsetsPerWord) * asciiSubsetBits;
      int activeSubset =
          (int) ((scratch.active[sourceWord] >>> shift) & asciiSubsetMask);
      if (activeSubset == 0) continue;

      int row =
          characterBase
              + sourceSubset * sourceStride
              + activeSubset * rowWords;
      for (int word = 0; word < rowWords; word++) {
        scratch.next[word] |= asciiSubsetTransitions[row + word];
      }
    }
  }

  private boolean classMatches(int id, char value) {
    int c = value;
    if (c < 64) return ((asciiLow[id] >>> c) & 1L) != 0L;
    if (c < 128) return ((asciiHigh[id] >>> (c - 64)) & 1L) != 0L;
    if (bmpClassMasks.length != 0) {
      int page = bmpPageByHighByte[c >>> 8];
      if (page < 0) return false;
      int row = ((page << 8) + (c & 0xff)) * bmpClassWordCount;
      return (bmpClassMasks[row + (id >>> 6)] & (1L << (id & 63))) != 0L;
    }
    int low = rangeOffset[id];
    int high = low + rangeCount[id] - 1;
    while (low <= high) {
      int mid = (low + high) >>> 1;
      if (value < rangeLow[mid]) high = mid - 1;
      else if (value > rangeHigh[mid]) low = mid + 1;
      else return true;
    }
    return false;
  }

  private static void set(long[] bits, int state) {
    bits[state >>> 6] |= 1L << (state & 63);
  }

  private static long[] buildAsciiStateMasks(
      byte[] op, int[] classId, ClassTable classes, IProgressMonitor progress) {
    int words = (op.length + 63) >>> 6;
    long cells = Math.multiplyExact((long) ASCII_CARDINALITY, words);
    long bytes = Math.multiplyExact(cells, Long.BYTES);
    if (bytes > MAX_ASCII_STATE_MASK_BYTES || cells > Integer.MAX_VALUE) {
      return new long[0];
    }
    long[] masks = new long[(int) cells];
    for (int state = 0; state < op.length; state++) {
      if ((state & 0x3ff) == 0) progress.checkCanceled();
      if (op[state] != OP_CHAR) continue;
      int id = classId[state];
      long low = classes.asciiLow[id];
      while (low != 0L) {
        int value = Long.numberOfTrailingZeros(low);
        masks[value * words + (state >>> 6)] |= 1L << (state & 63);
        low &= low - 1;
      }
      long high = classes.asciiHigh[id];
      while (high != 0L) {
        int value = 64 + Long.numberOfTrailingZeros(high);
        masks[value * words + (state >>> 6)] |= 1L << (state & 63);
        high &= high - 1;
      }
    }
    return masks;
  }

  private static BmpClassTable buildBmpClassMasks(
      ClassTable classes, IProgressMonitor progress) {
    int classCount = classes.asciiLow.length;
    if (classCount == 0 || classes.rangeLow.length == 0) {
      return BmpClassTable.NONE;
    }

    boolean[] usedPages = new boolean[256];
    long assignments = 0L;
    for (int id = 0; id < classCount; id++) {
      int from = classes.rangeOffset[id];
      int to = from + classes.rangeCount[id];
      for (int range = from; range < to; range++) {
        int low = classes.rangeLow[range];
        int high = classes.rangeHigh[range];
        assignments = Math.addExact(assignments, (long) high - low + 1L);
        if (assignments > MAX_BMP_CLASS_MASK_ASSIGNMENTS) {
          return BmpClassTable.NONE;
        }
        for (int page = low >>> 8; page <= (high >>> 8); page++) {
          usedPages[page] = true;
        }
      }
    }

    int pageCount = 0;
    for (boolean used : usedPages) if (used) pageCount++;
    if (pageCount == 0) return BmpClassTable.NONE;

    int classWordCount = (classCount + 63) >>> 6;
    long cells =
        Math.multiplyExact(
            Math.multiplyExact((long) pageCount, 256L),
            classWordCount);
    long bytes =
        Math.addExact(
            Math.multiplyExact(cells, Long.BYTES),
            256L * Integer.BYTES);
    if (bytes > MAX_BMP_CLASS_MASK_BYTES || cells > Integer.MAX_VALUE) {
      return BmpClassTable.NONE;
    }

    int[] pageByHighByte = new int[256];
    Arrays.fill(pageByHighByte, -1);
    int page = 0;
    for (int highByte = 0; highByte < usedPages.length; highByte++) {
      if (usedPages[highByte]) pageByHighByte[highByte] = page++;
    }

    long[] masks = new long[(int) cells];
    long visited = 0L;
    for (int id = 0; id < classCount; id++) {
      int from = classes.rangeOffset[id];
      int to = from + classes.rangeCount[id];
      for (int range = from; range < to; range++) {
        int low = classes.rangeLow[range];
        int high = classes.rangeHigh[range];
        for (int value = low; value <= high; value++) {
          if ((visited++ & 0x3fffL) == 0L) progress.checkCanceled();
          int mappedPage = pageByHighByte[value >>> 8];
          int row =
              ((mappedPage << 8) + (value & 0xff)) * classWordCount;
          masks[row + (id >>> 6)] |= 1L << (id & 63);
        }
      }
    }
    return new BmpClassTable(classWordCount, pageByHighByte, masks);
  }

  private record BmpClassTable(
      int classWordCount, int[] pageByHighByte, long[] masks) {
    private static final BmpClassTable NONE =
        new BmpClassTable(0, new int[0], new long[0]);

    private BmpClassTable {
      pageByHighByte = Objects.requireNonNull(pageByHighByte, "pageByHighByte");
      masks = Objects.requireNonNull(masks, "masks");
    }
  }

  private static AsciiSubsetTable buildAsciiSubsetTransitions(
      byte[] op,
      int[] out1,
      long[] asciiStateMasks,
      int stateWordCount,
      EpsilonClosureTable epsilonClosures,
      IProgressMonitor progress) {
    if (op.length == 0 || asciiStateMasks.length == 0 || epsilonClosures == null) {
      return AsciiSubsetTable.NONE;
    }

    AsciiSubsetTable wide =
        tryBuildAsciiSubsetTransitions(
            MAX_SUBSET_BITS,
            op,
            out1,
            asciiStateMasks,
            stateWordCount,
            epsilonClosures,
            progress);
    if (wide.transitions().length != 0) return wide;

    return tryBuildAsciiSubsetTransitions(
        MIN_SUBSET_BITS,
        op,
        out1,
        asciiStateMasks,
        stateWordCount,
        epsilonClosures,
        progress);
  }

  private static AsciiSubsetTable tryBuildAsciiSubsetTransitions(
      int subsetBits,
      byte[] op,
      int[] out1,
      long[] asciiStateMasks,
      int stateWordCount,
      EpsilonClosureTable epsilonClosures,
      IProgressMonitor progress) {
    int sourceSubsetCount = (op.length + subsetBits - 1) / subsetBits;
    int subsetCardinality = 1 << subsetBits;
    long rows =
        Math.multiplyExact(
            Math.multiplyExact((long) ASCII_CARDINALITY, sourceSubsetCount),
            subsetCardinality);
    long cells = Math.multiplyExact(rows, stateWordCount);
    long bytes = Math.multiplyExact(cells, Long.BYTES);
    if (bytes > MAX_ASCII_SUBSET_TRANSITION_BYTES || cells > Integer.MAX_VALUE) {
      return AsciiSubsetTable.NONE;
    }

    long[] transitions = new long[(int) cells];
    int sourceStride = subsetCardinality * stateWordCount;
    int characterStride = sourceSubsetCount * sourceStride;

    for (int value = 0; value < ASCII_CARDINALITY; value++) {
      int maskBase = value * stateWordCount;
      int characterBase = value * characterStride;
      for (int sourceSubset = 0; sourceSubset < sourceSubsetCount; sourceSubset++) {
        if ((sourceSubset & 0xff) == 0) progress.checkCanceled();
        int sourceBase = characterBase + sourceSubset * sourceStride;

        for (int subset = 1; subset < subsetCardinality; subset++) {
          int row = sourceBase + subset * stateWordCount;
          int previous = subset & (subset - 1);
          if (previous != 0) {
            System.arraycopy(
                transitions,
                sourceBase + previous * stateWordCount,
                transitions,
                row,
                stateWordCount);
          }

          int bit = Integer.numberOfTrailingZeros(subset);
          int state = sourceSubset * subsetBits + bit;
          if (state >= op.length || op[state] != OP_CHAR) continue;

          long accepted =
              asciiStateMasks[maskBase + (state >>> 6)]
                  & (1L << (state & 63));
          if (accepted == 0L) continue;
          epsilonClosures.orInto(out1[state], transitions, row);
        }
      }
    }
    return new AsciiSubsetTable(subsetBits, sourceSubsetCount, transitions);
  }

  private record AsciiSubsetTable(
      int bits, int sourceSubsetCount, long[] transitions) {
    private static final AsciiSubsetTable NONE =
        new AsciiSubsetTable(0, 0, new long[0]);

    private AsciiSubsetTable {
      transitions = Objects.requireNonNull(transitions, "transitions");
    }
  }

  /**
   * Bounded deterministic ASCII projection of the assertion-free Thompson machine.
   *
   * <p>The builder reuses the existing ASCII state masks and epsilon-closure table. Construction
   * state is entirely primitive: flat subset and transition arenas plus an open-addressed integer
   * interner. A refusal is an optimization miss only; execution falls through to the existing
   * subset-DP/state-mask NFA.</p>
   */
  private static final class AsciiDfaTable {
    private static final int TRANSITION_BYTES_PER_STATE =
        ASCII_CARDINALITY * Short.BYTES + Byte.BYTES;

    private final int startState;
    private final short[] transitions;
    private final byte[] accepting;

    private AsciiDfaTable(int startState, short[] transitions, byte[] accepting) {
      this.startState = startState;
      this.transitions = transitions;
      this.accepting = accepting;
    }

    static AsciiDfaTable fromImage(AsciiDfaImage image) {
      if (image == null) return null;
      return new AsciiDfaTable(
          image.startState(), image.transitions(), image.accepting());
    }

    static AsciiDfaTable tryBuild(
        byte[] op,
        int[] out1,
        int nfaStart,
        int matchState,
        long[] asciiStateMasks,
        EpsilonClosureTable epsilonClosures,
        boolean reinjectStart,
        int maxStates,
        long maxBytes,
        IProgressMonitor progress) {
      if (op.length == 0 || asciiStateMasks.length == 0 || maxStates < 1 || maxBytes < 1) {
        return null;
      }

      boolean hasEpsilon = false;
      for (byte opcode : op) {
        if (opcode == OP_ASSERT_START
            || opcode == OP_ASSERT_END
            || opcode == OP_ASSERT_WORD_BOUNDARY
            || opcode == OP_ASSERT_NOT_WORD_BOUNDARY) {
          return null;
        }
        if (opcode == OP_SPLIT || opcode == OP_JUMP) hasEpsilon = true;
      }
      if (hasEpsilon && epsilonClosures == null) return null;

      int words = (op.length + 63) >>> 6;
      if (asciiStateMasks.length != ASCII_CARDINALITY * words) return null;

      int stateLimit =
          boundedStateLimit(
              maxStates,
              words,
              maxBytes,
              MAX_ASCII_DFA_BUILD_BYTES);
      if (stateLimit < 1) return null;

      int transitionCells = Math.multiplyExact(stateLimit, ASCII_CARDINALITY);
      int subsetCells = Math.multiplyExact(stateLimit, words);
      int slotCapacity = hashCapacity(stateLimit);

      short[] transitions = new short[transitionCells];
      Arrays.fill(transitions, (short) -1);
      byte[] accepting = new byte[stateLimit];
      long[] subsets = new long[subsetCells];
      int[] slots = new int[slotCapacity];
      long[] start = new long[words];
      long[] next = new long[words];

      closureInto(nfaStart, start, epsilonClosures);
      if (empty(start)) return null;
      System.arraycopy(start, 0, subsets, 0, words);
      insertState(subsets, words, 0, slots);

      int stateCount = 1;
      for (int dfaState = 0; dfaState < stateCount; dfaState++) {
        if ((dfaState & 0x3f) == 0) progress.checkCanceled();
        int activeOffset = dfaState * words;
        accepting[dfaState] =
            containsState(subsets, activeOffset, matchState) ? (byte) 1 : (byte) 0;
        int row = dfaState * ASCII_CARDINALITY;

        for (int value = 0; value < ASCII_CARDINALITY; value++) {
          Arrays.fill(next, 0L);
          int maskBase = value * words;
          for (int word = 0; word < words; word++) {
            long bits = subsets[activeOffset + word] & asciiStateMasks[maskBase + word];
            while (bits != 0L) {
              int bit = Long.numberOfTrailingZeros(bits);
              int state = (word << 6) + bit;
              bits &= bits - 1;
              closureInto(out1[state], next, epsilonClosures);
            }
          }

          if (reinjectStart) {
            for (int word = 0; word < words; word++) next[word] |= start[word];
          }
          if (empty(next)) continue;

          int target = findState(subsets, words, stateCount, slots, next);
          if (target < 0) {
            if (stateCount == stateLimit) return null;
            target = stateCount++;
            System.arraycopy(next, 0, subsets, target * words, words);
            insertState(subsets, words, target, slots);
          }
          transitions[row + value] = (short) target;
        }
      }

      return new AsciiDfaTable(
          0,
          Arrays.copyOf(transitions, Math.multiplyExact(stateCount, ASCII_CARDINALITY)),
          Arrays.copyOf(accepting, stateCount));
    }

    private static int boundedStateLimit(
        int requestedStates, int words, long retainedBudget, long buildBudget) {
      long retainedLimit = retainedBudget / TRANSITION_BYTES_PER_STATE;
      int requested =
          (int)
              Math.min(
                  Math.min((long) requestedStates, Short.MAX_VALUE),
                  Math.min(retainedLimit, Integer.MAX_VALUE));
      int low = 0;
      int high = requested;
      while (low < high) {
        int candidate = low + ((high - low + 1) >>> 1);
        if (fitsBuildBudget(candidate, words, retainedBudget, buildBudget)) {
          low = candidate;
        } else {
          high = candidate - 1;
        }
      }
      return low;
    }

    private static boolean fitsBuildBudget(
        int states, int words, long retainedBudget, long buildBudget) {
      if (states < 1) return false;
      try {
        long transitionBytes =
            Math.addExact(
                Math.multiplyExact(
                    Math.multiplyExact((long) states, ASCII_CARDINALITY),
                    Short.BYTES),
                states);
        if (transitionBytes > retainedBudget) return false;

        long subsetCells = Math.multiplyExact((long) states, words);
        if (subsetCells > Integer.MAX_VALUE) return false;
        long subsetBytes = Math.multiplyExact(subsetCells, Long.BYTES);
        int slots = hashCapacity(states);
        long slotBytes = Math.multiplyExact((long) slots, Integer.BYTES);
        long scratchBytes =
            Math.multiplyExact(
                Math.multiplyExact((long) words, 2L),
                (long) Long.BYTES);
        long total =
            Math.addExact(
                Math.addExact(transitionBytes, subsetBytes),
                Math.addExact(slotBytes, scratchBytes));
        return total <= buildBudget;
      } catch (ArithmeticException overflow) {
        return false;
      }
    }

    private static int hashCapacity(int states) {
      long required = Math.max(2L, Math.multiplyExact((long) states, 2L));
      int capacity = 2;
      while (capacity < required) {
        if (capacity >= (1 << 30)) throw new IllegalArgumentException("DFA hash table too large");
        capacity <<= 1;
      }
      return capacity;
    }

    private static int findState(
        long[] subsets,
        int words,
        int stateCount,
        int[] slots,
        long[] candidate) {
      int mask = slots.length - 1;
      int slot = mixSubsetHash(hashSubset(candidate)) & mask;
      for (int probe = 0; probe < slots.length; probe++) {
        int encoded = slots[slot];
        if (encoded == 0) return -1;
        int state = encoded - 1;
        if (state < stateCount
            && sameSubset(subsets, state * words, candidate, words)) {
          return state;
        }
        slot = (slot + 1) & mask;
      }
      return -1;
    }

    private static void insertState(
        long[] subsets, int words, int state, int[] slots) {
      int mask = slots.length - 1;
      int offset = state * words;
      int slot = mixSubsetHash(hashSubset(subsets, offset, words)) & mask;
      while (slots[slot] != 0) slot = (slot + 1) & mask;
      slots[slot] = state + 1;
    }

    private static int hashSubset(long[] bits) {
      int hash = 1;
      for (long value : bits) {
        hash = 31 * hash + (int) (value ^ (value >>> 32));
      }
      return hash;
    }

    private static int hashSubset(long[] bits, int offset, int length) {
      int hash = 1;
      for (int index = 0; index < length; index++) {
        long value = bits[offset + index];
        hash = 31 * hash + (int) (value ^ (value >>> 32));
      }
      return hash;
    }

    private static int mixSubsetHash(int value) {
      int x = value;
      x ^= x >>> 16;
      x *= 0x7feb_352d;
      x ^= x >>> 15;
      x *= 0x846c_a68b;
      return x ^ (x >>> 16);
    }

    private static boolean sameSubset(
        long[] subsets, int offset, long[] candidate, int words) {
      for (int word = 0; word < words; word++) {
        if (subsets[offset + word] != candidate[word]) return false;
      }
      return true;
    }

    private static boolean containsState(long[] bits, int offset, int state) {
      return (bits[offset + (state >>> 6)] & (1L << (state & 63))) != 0L;
    }

    private static void closureInto(
        int state, long[] target, EpsilonClosureTable epsilonClosures) {
      if (state < 0) return;
      if (epsilonClosures != null) {
        epsilonClosures.orInto(state, target);
      } else {
        set(target, state);
      }
    }

    int matches(CharSequence input) {
      int state = startState;
      for (int index = 0; index < input.length(); index++) {
        char value = input.charAt(index);
        if (value >= ASCII_CARDINALITY) return DFA_UNSUPPORTED;
        state = transitions[state * ASCII_CARDINALITY + value];
        if (state < 0) return 0;
      }
      return accepting[state];
    }

    int matchesKnownAscii(CharSequence input) {
      int state = startState;
      for (int index = 0; index < input.length(); index++) {
        state = transitions[state * ASCII_CARDINALITY + input.charAt(index)];
        if (state < 0) return 0;
      }
      return accepting[state];
    }

    int lookingAt(CharSequence input) {
      int state = startState;
      if (accepting[state] != 0) return 1;
      for (int index = 0; index < input.length(); index++) {
        char value = input.charAt(index);
        if (value >= ASCII_CARDINALITY) return DFA_UNSUPPORTED;
        state = transitions[state * ASCII_CARDINALITY + value];
        if (state < 0) return 0;
        if (accepting[state] != 0) return 1;
      }
      return 0;
    }

    int lookingAtKnownAscii(CharSequence input) {
      int state = startState;
      if (accepting[state] != 0) return 1;
      for (int index = 0; index < input.length(); index++) {
        state = transitions[state * ASCII_CARDINALITY + input.charAt(index)];
        if (state < 0) return 0;
        if (accepting[state] != 0) return 1;
      }
      return 0;
    }

    int find(CharSequence input) {
      int state = startState;
      if (accepting[state] != 0) return 1;
      for (int index = 0; index < input.length(); index++) {
        char value = input.charAt(index);
        if (value >= ASCII_CARDINALITY) return DFA_UNSUPPORTED;
        state = transitions[state * ASCII_CARDINALITY + value];
        if (state < 0) return 0;
        if (accepting[state] != 0) return 1;
      }
      return 0;
    }

    int findKnownAscii(CharSequence input) {
      int state = startState;
      if (accepting[state] != 0) return 1;
      for (int index = 0; index < input.length(); index++) {
        state = transitions[state * ASCII_CARDINALITY + input.charAt(index)];
        if (state < 0) return 0;
        if (accepting[state] != 0) return 1;
      }
      return 0;
    }

    int stateCount() {
      return accepting.length;
    }

    long payloadBytes() {
      return Short.BYTES * (long) transitions.length + accepting.length;
    }

    AsciiDfaImage image() {
      return new AsciiDfaImage(startState, transitions, accepting);
    }

    private static boolean empty(long[] bits) {
      for (long word : bits) if (word != 0L) return false;
      return true;
    }
  }

  private static final class EpsilonClosureTable {
    private final int wordsPerRow;
    private final int[] rowByState;
    private final long[] terminals;

    private EpsilonClosureTable(int wordsPerRow, int[] rowByState, long[] terminals) {
      this.wordsPerRow = wordsPerRow;
      this.rowByState = rowByState;
      this.terminals = terminals;
    }

    static EpsilonClosureTable tryBuild(
        byte[] op,
        int[] out1,
        int[] out2,
        long maxBytes,
        IProgressMonitor progress) {
      int epsilonRows = 0;
      int[] rowByState = new int[op.length];
      Arrays.fill(rowByState, -1);
      for (int state = 0; state < op.length; state++) {
        byte opcode = op[state];
        if (opcode == OP_ASSERT_START
            || opcode == OP_ASSERT_END
            || opcode == OP_ASSERT_WORD_BOUNDARY
            || opcode == OP_ASSERT_NOT_WORD_BOUNDARY) {
          return null;
        }
        if (opcode == OP_SPLIT || opcode == OP_JUMP) {
          rowByState[state] = epsilonRows++;
        }
      }
      if (epsilonRows == 0 || op.length == 0) return null;

      int words = (op.length + 63) >>> 6;
      long cells = Math.multiplyExact((long) epsilonRows, words);
      long bytes =
          Math.addExact(
              Math.multiplyExact(cells, Long.BYTES),
              Math.multiplyExact((long) rowByState.length, Integer.BYTES));
      if (bytes > maxBytes || cells > Integer.MAX_VALUE) return null;

      long[] terminals = new long[(int) cells];
      int[] mark = new int[op.length];
      int[] stack = new int[Math.max(1, op.length)];
      int stamp = 0;
      for (int seed = 0; seed < op.length; seed++) {
        int rowIndex = rowByState[seed];
        if (rowIndex < 0) continue;
        if ((rowIndex & 0xff) == 0) progress.checkCanceled();
        if (++stamp == 0) {
          Arrays.fill(mark, 0);
          stamp = 1;
        }
        int top = 0;
        mark[seed] = stamp;
        stack[top++] = seed;
        int row = rowIndex * words;
        while (top != 0) {
          int state = stack[--top];
          switch (op[state]) {
            case OP_SPLIT -> {
              top = pushStatic(stack, mark, stamp, top, out2[state]);
              top = pushStatic(stack, mark, stamp, top, out1[state]);
            }
            case OP_JUMP -> top = pushStatic(stack, mark, stamp, top, out1[state]);
            case OP_CHAR, OP_MATCH ->
                terminals[row + (state >>> 6)] |= 1L << (state & 63);
            default ->
                throw new IllegalStateException(
                    "dynamic assertion reached static epsilon table: " + op[state]);
          }
        }
      }
      return new EpsilonClosureTable(words, rowByState, terminals);
    }

    private static int pushStatic(
        int[] stack, int[] mark, int stamp, int top, int state) {
      if (state < 0 || mark[state] == stamp) return top;
      mark[state] = stamp;
      stack[top] = state;
      return top + 1;
    }

    void orInto(int state, long[] target) {
      orInto(state, target, 0);
    }

    void orInto(int state, long[] target, int targetOffset) {
      int rowIndex = rowByState[state];
      if (rowIndex < 0) {
        target[targetOffset + (state >>> 6)] |= 1L << (state & 63);
        return;
      }
      int row = rowIndex * wordsPerRow;
      for (int word = 0; word < wordsPerRow; word++) {
        target[targetOffset + word] |= terminals[row + word];
      }
    }

    long payloadBytes() {
      return Long.BYTES * (long) terminals.length
          + Integer.BYTES * (long) rowByState.length;
    }
  }

  public static final class Workspace {
    private final MIndexRegexProgram program;
    private final Scratch scratch;

    private Workspace(MIndexRegexProgram program) {
      this.program = program;
      this.scratch = new Scratch(program.op.length, program.epsilonClosures == null);
    }

    public MIndexRegexProgram program() {
      return program;
    }

    public boolean matches(CharSequence input) {
      return program.matches(input, scratch);
    }

    public boolean lookingAt(CharSequence input) {
      return program.lookingAt(input, scratch);
    }

    public boolean find(CharSequence input) {
      return program.find(input, scratch);
    }

    public long primitivePayloadBytes() {
      return Long.BYTES * (long) (scratch.active.length + scratch.next.length)
          + Integer.BYTES * (long) (scratch.mark.length + scratch.stack.length);
    }
  }

  private static final class Scratch {
    private long[] active;
    private long[] next;
    private final int[] mark;
    private final int[] stack;
    private int stamp;

    Scratch(int states, boolean needsClosureWalker) {
      int words = (states + 63) >>> 6;
      this.active = new long[words];
      this.next = new long[words];
      this.mark = needsClosureWalker ? new int[states] : new int[0];
      this.stack = needsClosureWalker ? new int[Math.max(1, states)] : new int[0];
      this.stamp = 0;
    }

    void reset() {
      Arrays.fill(active, 0L);
      Arrays.fill(next, 0L);
    }

    void beginClosure() {
      if (mark.length == 0) return;
      if (++stamp == 0) {
        Arrays.fill(mark, 0);
        stamp = 1;
      }
    }

    boolean contains(int state) {
      return (active[state >>> 6] & (1L << (state & 63))) != 0L;
    }

    boolean isEmpty() {
      for (long word : active) if (word != 0L) return false;
      return true;
    }
  }

  private static MIndexRegexTrigramQuery trigramQuery(
      Node node, ClassTableBuilder classes) {
    List<String> exact = trigramExact(node, classes);
    if (exact != null) return MIndexRegexTrigramQuery.fromExact(exact);

    return switch (node) {
      case EmptyNode ignored -> MIndexRegexTrigramQuery.all();
      case StartNode ignored -> MIndexRegexTrigramQuery.all();
      case EndNode ignored -> MIndexRegexTrigramQuery.all();
      case WordBoundaryNode ignored -> MIndexRegexTrigramQuery.all();
      case AtomNode ignored -> MIndexRegexTrigramQuery.all();
      case QuotedNode quoted ->
          MIndexRegexTrigramQuery.and(
              quoted.children().stream()
                  .map(child -> trigramQuery(child, classes))
                  .toList());
      case ConcatNode concat ->
          MIndexRegexTrigramQuery.and(
              concat.children().stream()
                  .map(child -> trigramQuery(child, classes))
                  .toList());
      case AltNode alt ->
          MIndexRegexTrigramQuery.or(
              alt.children().stream()
                  .map(child -> trigramQuery(child, classes))
                  .toList());
      case RepeatNode repeat ->
          repeat.min() == 0
              ? MIndexRegexTrigramQuery.all()
              : trigramQuery(repeat.child(), classes);
    };
  }

  /**
   * Returns a bounded exact language for trigram planning, or null when exact expansion would be
   * too large. This is planner-only state and never changes regex acceptance semantics.
   */
  private static List<String> trigramExact(Node node, ClassTableBuilder classes) {
    return switch (node) {
      case EmptyNode ignored -> List.of("");
      case StartNode ignored -> List.of("");
      case EndNode ignored -> List.of("");
      case WordBoundaryNode ignored -> List.of("");
      case AtomNode atom -> classes.smallExactStrings(atom.classId(), MAX_TRIGRAM_CLASS_LITERALS);
      case QuotedNode quoted -> trigramConcatExact(quoted.children(), classes);
      case ConcatNode concat -> trigramConcatExact(concat.children(), classes);
      case AltNode alt -> trigramAlternateExact(alt.children(), classes);
      case RepeatNode repeat -> trigramRepeatExact(repeat, classes);
    };
  }

  private static List<String> trigramConcatExact(
      List<Node> children, ClassTableBuilder classes) {
    List<String> result = List.of("");
    for (Node child : children) {
      List<String> right = trigramExact(child, classes);
      if (right == null) return null;
      result = trigramCross(result, right);
      if (result == null) return null;
    }
    return result;
  }

  private static List<String> trigramAlternateExact(
      List<Node> children, ClassTableBuilder classes) {
    java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
    for (Node child : children) {
      List<String> exact = trigramExact(child, classes);
      if (exact == null) return null;
      result.addAll(exact);
      if (result.size() > MAX_TRIGRAM_EXACT_STRINGS) return null;
    }
    return List.copyOf(result);
  }

  private static List<String> trigramRepeatExact(
      RepeatNode repeat, ClassTableBuilder classes) {
    List<String> child = trigramExact(repeat.child(), classes);
    if (child == null) return null;

    if (repeat.max() == repeat.min()
        && repeat.max() >= 0
        && child.size() == 1) {
      String unit = child.getFirst();
      long length = (long) unit.length() * repeat.max();
      if (length > MAX_TRIGRAM_EXACT_UTF16) return null;
      return List.of(unit.repeat(repeat.max()));
    }

    if (repeat.max() < 0 || repeat.max() > MAX_TRIGRAM_REPEAT_EXPANSION) return null;
    List<String> power = List.of("");
    java.util.LinkedHashSet<String> accepted = new java.util.LinkedHashSet<>();
    for (int count = 0; count <= repeat.max(); count++) {
      if (count >= repeat.min()) {
        accepted.addAll(power);
        if (accepted.size() > MAX_TRIGRAM_EXACT_STRINGS) return null;
      }
      if (count == repeat.max()) break;
      power = trigramCross(power, child);
      if (power == null) return null;
    }
    return List.copyOf(accepted);
  }

  private static List<String> trigramCross(List<String> left, List<String> right) {
    long combinations = (long) left.size() * right.size();
    if (combinations > MAX_TRIGRAM_EXACT_STRINGS) return null;
    java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
    for (String prefix : left) {
      for (String suffix : right) {
        if ((long) prefix.length() + suffix.length() > MAX_TRIGRAM_EXACT_UTF16) return null;
        result.add(prefix + suffix);
        if (result.size() > MAX_TRIGRAM_EXACT_STRINGS) return null;
      }
    }
    return List.copyOf(result);
  }

  private sealed interface Node
      permits EmptyNode, AtomNode, QuotedNode, ConcatNode, AltNode, RepeatNode, StartNode, EndNode,
          WordBoundaryNode {
    int minLength();
    int maxLength();
    long requiredPresence64();
    int requiredCharacterFlags();
  }

  private record EmptyNode() implements Node {
    public int minLength() { return 0; }
    public int maxLength() { return 0; }
    public long requiredPresence64() { return 0L; }
    public int requiredCharacterFlags() { return 0; }
  }

  private record AtomNode(
      int classId, long requiredPresence64, int requiredCharacterFlags) implements Node {
    public int minLength() { return 1; }
    public int maxLength() { return 1; }
  }

  /**
   * Parser-only marker for a \Q...\E literal run.
   *
   * <p>RE2/J emits each quoted code point as a literal atom before repetition is parsed. Keeping
   * this marker until parseRepeated lets a following quantifier bind to only the final literal,
   * e.g. \Qab\E+ == ab+, rather than incorrectly turning the whole quote into (ab)+.</p>
   */
  private record QuotedNode(
      List<Node> children,
      int minLength,
      int maxLength,
      long requiredPresence64,
      int requiredCharacterFlags) implements Node {
    QuotedNode(List<Node> children) {
      this(
          List.copyOf(children),
          sumMin(children),
          sumMax(children),
          concatRequiredPresence(children),
          concatRequiredFlags(children));
    }
  }

  private record StartNode() implements Node {
    public int minLength() { return 0; }
    public int maxLength() { return 0; }
    public long requiredPresence64() { return 0L; }
    public int requiredCharacterFlags() { return 0; }
  }

  private record EndNode() implements Node {
    public int minLength() { return 0; }
    public int maxLength() { return 0; }
    public long requiredPresence64() { return 0L; }
    public int requiredCharacterFlags() { return 0; }
  }

  private record WordBoundaryNode(boolean boundary) implements Node {
    public int minLength() { return 0; }
    public int maxLength() { return 0; }
    public long requiredPresence64() { return 0L; }
    public int requiredCharacterFlags() { return 0; }
  }

  private record ConcatNode(
      List<Node> children,
      int minLength,
      int maxLength,
      long requiredPresence64,
      int requiredCharacterFlags) implements Node {
    ConcatNode(List<Node> children) {
      this(
          List.copyOf(children),
          sumMin(children),
          sumMax(children),
          concatRequiredPresence(children),
          concatRequiredFlags(children));
    }
  }

  private record AltNode(
      List<Node> children,
      int minLength,
      int maxLength,
      long requiredPresence64,
      int requiredCharacterFlags) implements Node {
    AltNode(List<Node> children) {
      this(
          List.copyOf(children),
          altMin(children),
          altMax(children),
          altRequiredPresence(children),
          altRequiredFlags(children));
    }
  }

  private record RepeatNode(
      Node child,
      int min,
      int max,
      int minLength,
      int maxLength,
      long requiredPresence64,
      int requiredCharacterFlags) implements Node {
    RepeatNode(Node child, int min, int max) {
      this(
          child,
          min,
          max,
          multiplyMin(child.minLength(), min),
          multiplyMax(child.maxLength(), max),
          min == 0 ? 0L : child.requiredPresence64(),
          min == 0 ? 0 : child.requiredCharacterFlags());
    }
  }

  private static boolean mayConsumeSurrogateUnit(
      Node node, ClassTableBuilder classes) {
    return switch (node) {
      case AtomNode atom -> classes.canConsumeSurrogateUnit(atom.classId());
      case QuotedNode quoted -> anyMayConsumeSurrogate(quoted.children(), classes);
      case ConcatNode concat -> anyMayConsumeSurrogate(concat.children(), classes);
      case AltNode alt -> anyMayConsumeSurrogate(alt.children(), classes);
      case RepeatNode repeat ->
          repeat.max() != 0 && mayConsumeSurrogateUnit(repeat.child(), classes);
      case EmptyNode ignored -> false;
      case StartNode ignored -> false;
      case EndNode ignored -> false;
      case WordBoundaryNode ignored -> false;
    };
  }

  private static boolean anyMayConsumeSurrogate(
      List<Node> nodes, ClassTableBuilder classes) {
    for (Node node : nodes) {
      if (mayConsumeSurrogateUnit(node, classes)) return true;
    }
    return false;
  }

  private static long concatRequiredPresence(List<Node> nodes) {
    long required = 0L;
    for (Node node : nodes) required |= node.requiredPresence64();
    return required;
  }

  private static long altRequiredPresence(List<Node> nodes) {
    if (nodes.isEmpty()) return 0L;
    long required = nodes.getFirst().requiredPresence64();
    for (int index = 1; index < nodes.size(); index++) {
      required &= nodes.get(index).requiredPresence64();
    }
    return required;
  }

  private static int concatRequiredFlags(List<Node> nodes) {
    int flags = 0;
    for (Node node : nodes) flags |= node.requiredCharacterFlags();
    return flags;
  }

  private static int altRequiredFlags(List<Node> nodes) {
    if (nodes.isEmpty()) return 0;
    int flags = nodes.getFirst().requiredCharacterFlags();
    for (int index = 1; index < nodes.size(); index++) {
      flags &= nodes.get(index).requiredCharacterFlags();
    }
    return flags;
  }

  private static int sumMin(List<Node> nodes) {
    long value = 0;
    for (Node node : nodes) value += node.minLength();
    return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
  }

  private static int sumMax(List<Node> nodes) {
    long value = 0;
    for (Node node : nodes) {
      if (node.maxLength() < 0) return -1;
      value += node.maxLength();
      if (value > Integer.MAX_VALUE) return -1;
    }
    return (int) value;
  }

  private static int altMin(List<Node> nodes) {
    int min = Integer.MAX_VALUE;
    for (Node node : nodes) min = Math.min(min, node.minLength());
    return min == Integer.MAX_VALUE ? 0 : min;
  }

  private static int altMax(List<Node> nodes) {
    int max = 0;
    for (Node node : nodes) {
      if (node.maxLength() < 0) return -1;
      max = Math.max(max, node.maxLength());
    }
    return max;
  }

  private static int multiplyMin(int value, int count) {
    long result = (long) value * count;
    return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
  }

  private static int multiplyMax(int value, int count) {
    if (count < 0 || value < 0) return -1;
    long result = (long) value * count;
    return result > Integer.MAX_VALUE ? -1 : (int) result;
  }

  private static final class Parser {
    private final String regex;
    private final Semantics semantics;
    private final int maxStates;
    private final IProgressMonitor progress;
    private final ClassTableBuilder classes = new ClassTableBuilder();
    private int cursor;
    private int captureCount;

    Parser(
        String regex,
        Semantics semantics,
        int maxStates,
        IProgressMonitor progress) {
      this.regex = regex;
      this.semantics = semantics;
      this.maxStates = maxStates;
      this.progress = progress;
    }

    Node parse() {
      Node node = parseAlternation();
      if (cursor != regex.length()) throw unsupported();
      return node;
    }

    private Node parseAlternation() {
      List<Node> choices = new ArrayList<>();
      choices.add(parseConcatenation());
      while (peek('|')) {
        cursor++;
        choices.add(parseConcatenation());
      }
      return choices.size() == 1 ? choices.get(0) : new AltNode(choices);
    }

    private Node parseConcatenation() {
      List<Node> nodes = new ArrayList<>();
      while (cursor < regex.length() && regex.charAt(cursor) != ')' && regex.charAt(cursor) != '|') {
        if ((cursor & 0x3ff) == 0) progress.checkCanceled();
        nodes.add(parseRepeated());
      }
      if (nodes.isEmpty()) return new EmptyNode();
      return nodes.size() == 1 ? nodes.get(0) : new ConcatNode(nodes);
    }

    private Node parseRepeated() {
      Node atom = parseAtom();
      if (cursor >= regex.length()) return atom;
      char marker = regex.charAt(cursor);
      int min;
      int max;
      switch (marker) {
        case '*' -> { min = 0; max = -1; cursor++; }
        case '+' -> { min = 1; max = -1; cursor++; }
        case '?' -> { min = 0; max = 1; cursor++; }
        case '{' -> {
          int[] bounds = parseBounds();
          min = bounds[0];
          max = bounds[1];
        }
        default -> { return atom; }
      }
      if (atom.minLength() == 0) throw unsupported();
      if (semantics == Semantics.RE2
          && (min > 1_000 || (max >= 0 && max > 1_000))) {
        throw unsupported();
      }
      if (min > maxStates || (max >= 0 && max > maxStates)) throw unsupported();
      if (cursor < regex.length() && regex.charAt(cursor) == '?') {
        // RE2/J's non-greedy suffix changes match extent/captures, not boolean language truth.
        cursor++;
      } else if (cursor < regex.length() && regex.charAt(cursor) == '+') {
        // Possessive repetition changes accepted behavior and is not part of RE2/J's language.
        throw unsupported();
      }
      if (atom instanceof QuotedNode quoted) {
        List<Node> children = new ArrayList<>(quoted.children());
        if (children.isEmpty()) throw unsupported();
        int last = children.size() - 1;
        Node tail = children.get(last);
        if (tail.minLength() == 0) throw unsupported();
        children.set(last, new RepeatNode(tail, min, max));
        return children.size() == 1 ? children.get(0) : new ConcatNode(children);
      }
      return new RepeatNode(atom, min, max);
    }

    private int[] parseBounds() {
      cursor++;
      int min = parseDecimal();
      if (min < 0) throw unsupported();
      if (peek('}')) {
        cursor++;
        return new int[] {min, min};
      }
      if (!peek(',')) throw unsupported();
      cursor++;
      if (peek('}')) {
        cursor++;
        return new int[] {min, -1};
      }
      int max = parseDecimal();
      if (max < min || !peek('}')) throw unsupported();
      cursor++;
      return new int[] {min, max};
    }

    private int parseDecimal() {
      if (cursor >= regex.length() || !Character.isDigit(regex.charAt(cursor))) return -1;
      long value = 0;
      while (cursor < regex.length() && Character.isDigit(regex.charAt(cursor))) {
        value = value * 10 + (regex.charAt(cursor++) - '0');
        if (value > Integer.MAX_VALUE) throw unsupported();
      }
      return (int) value;
    }

    private Node parseAtom() {
      if (cursor >= regex.length()) throw unsupported();
      char value = regex.charAt(cursor++);
      if (Character.isSurrogate(value)) throw unsupported();
      return switch (value) {
        case '.' -> atom(classes.dot(semantics));
        case '^' -> new StartNode();
        case '$' -> {
          if (semantics == Semantics.JDK) throw unsupported();
          yield new EndNode();
        }
        case '[' -> parseClass();
        case '(' -> parseGroup();
        case '\\' -> parseEscape(false);
        case ')', '|', '*', '+', '?', '{' -> throw unsupported();
        default -> atom(classes.literal(value));
      };
    }

    private AtomNode atom(int classId) {
      return new AtomNode(
          classId,
          classes.requiredPresence64(classId),
          classes.requiredFlags(classId));
    }

    private Node parseGroup() {
      if (peek('?')) {
        if (cursor + 1 < regex.length() && regex.charAt(cursor + 1) == ':') {
          cursor += 2;
        } else {
          throw unsupported();
        }
      } else {
        captureCount++;
      }
      Node body = parseAlternation();
      if (!peek(')')) throw unsupported();
      cursor++;
      return body;
    }

    private Node parseEscape(boolean inClass) {
      if (cursor >= regex.length()) throw unsupported();
      char escaped = regex.charAt(cursor++);
      return switch (escaped) {
        case 'd' -> atom(classes.digits(false));
        case 'D' -> atom(classes.digits(true));
        case 'w' -> atom(classes.word(false));
        case 'W' -> atom(classes.word(true));
        case 's' -> atom(classes.space(semantics, false));
        case 'S' -> atom(classes.space(semantics, true));
        case 'A' -> {
          if (inClass) throw unsupported();
          yield new StartNode();
        }
        case 'b' -> {
          if (inClass || semantics != Semantics.RE2) throw unsupported();
          yield new WordBoundaryNode(true);
        }
        case 'B' -> {
          if (inClass || semantics != Semantics.RE2) throw unsupported();
          yield new WordBoundaryNode(false);
        }
        case 'z' -> {
          if (inClass) throw unsupported();
          yield new EndNode();
        }
        case 'Q' -> {
          if (inClass) throw unsupported();
          yield parseQuotedLiteral();
        }
        case 'n' -> atom(classes.literal('\n'));
        case 'r' -> atom(classes.literal('\r'));
        case 't' -> atom(classes.literal('\t'));
        case 'f' -> atom(classes.literal('\f'));
        case 'x' -> atom(classes.literal(parseHex(2)));
        case 'u' -> atom(classes.literal(parseHex(4)));
        case '.', '^', '$', '|', '?', '*', '+', '(', ')', '[', ']', '{', '}', '\\', '-' ->
            atom(classes.literal(escaped));
        default -> throw unsupported();
      };
    }

    private Node parseQuotedLiteral() {
      List<Node> literals = new ArrayList<>();
      while (cursor < regex.length()) {
        progress.checkCanceled();
        if (regex.charAt(cursor) == '\\'
            && cursor + 1 < regex.length()
            && regex.charAt(cursor + 1) == 'E') {
          cursor += 2;
          break;
        }
        char value = regex.charAt(cursor++);
        if (Character.isSurrogate(value)) throw unsupported();
        literals.add(atom(classes.literal(value)));
      }
      if (literals.isEmpty()) return new EmptyNode();
      return new QuotedNode(literals);
    }

    private char parseHex(int digits) {
      if (cursor + digits > regex.length()) throw unsupported();
      int value = 0;
      for (int i = 0; i < digits; i++) {
        int digit = Character.digit(regex.charAt(cursor++), 16);
        if (digit < 0) throw unsupported();
        value = (value << 4) | digit;
      }
      char result = (char) value;
      if (Character.isSurrogate(result)) throw unsupported();
      return result;
    }

    private Node parseClass() {
      if (cursor >= regex.length()) throw unsupported();
      boolean negated = false;
      if (peek('^')) {
        negated = true;
        cursor++;
      }
      boolean[] allowed = new boolean[Character.MAX_VALUE + 1];
      boolean first = true;
      boolean closed = false;
      while (cursor < regex.length()) {
        if ((cursor & 0x3ff) == 0) progress.checkCanceled();
        if (peek(']') && !first) {
          cursor++;
          closed = true;
          break;
        }
        if (semantics == Semantics.JDK
            && cursor + 1 < regex.length()
            && regex.charAt(cursor) == '&'
            && regex.charAt(cursor + 1) == '&') {
          throw unsupported();
        }
        ClassPiece left = tryParsePosixClass();
        if (left == null) left = parseClassPiece(first);
        first = false;
        if (left.single < 0
            && peek('-')
            && cursor + 1 < regex.length()
            && regex.charAt(cursor + 1) != ']') {
          throw unsupported();
        }
        if (left.single >= 0
            && peek('-')
            && cursor + 1 < regex.length()
            && regex.charAt(cursor + 1) != ']') {
          cursor++;
          ClassPiece right = parseClassPiece(false);
          if (right.single < 0 || right.single < left.single) throw unsupported();
          Arrays.fill(allowed, left.single, right.single + 1, true);
        } else {
          left.apply(allowed);
        }
      }
      if (!closed) throw unsupported();
      if (negated) {
        for (int i = 0; i < allowed.length; i++) allowed[i] = !allowed[i];
      }
      return atom(classes.fromAllowed(allowed));
    }

    private ClassPiece tryParsePosixClass() {
      if (semantics != Semantics.RE2
          || cursor + 3 >= regex.length()
          || regex.charAt(cursor) != '['
          || regex.charAt(cursor + 1) != ':') {
        return null;
      }

      int end = regex.indexOf(":]", cursor + 2);
      if (end < 0) throw unsupported();
      String name = regex.substring(cursor + 2, end);
      if (name.isEmpty()) throw unsupported();
      cursor = end + 2;
      return ClassPiece.spec(classes.specPosix(name));
    }

    private ClassPiece parseClassPiece(boolean first) {
      if (cursor >= regex.length()) throw unsupported();
      char value = regex.charAt(cursor++);
      if (value == ']' && first) return ClassPiece.single(']');
      if (value == '\\') {
        if (cursor >= regex.length()) throw unsupported();
        char escaped = regex.charAt(cursor++);
        return switch (escaped) {
          case 'd' -> ClassPiece.spec(classes.specDigits(false));
          case 'D' -> ClassPiece.spec(classes.specDigits(true));
          case 'w' -> ClassPiece.spec(classes.specWord(false));
          case 'W' -> ClassPiece.spec(classes.specWord(true));
          case 's' -> ClassPiece.spec(classes.specSpace(semantics, false));
          case 'S' -> ClassPiece.spec(classes.specSpace(semantics, true));
          case 'n' -> ClassPiece.single('\n');
          case 'r' -> ClassPiece.single('\r');
          case 't' -> ClassPiece.single('\t');
          case 'f' -> ClassPiece.single('\f');
          case 'x' -> ClassPiece.single(parseHex(2));
          case 'u' -> ClassPiece.single(parseHex(4));
          case '\\', ']', '[', '-', '^' -> ClassPiece.single(escaped);
          default -> throw unsupported();
        };
      }
      if (value == '[' || Character.isSurrogate(value)) throw unsupported();
      return ClassPiece.single(value);
    }

    private boolean peek(char expected) {
      return cursor < regex.length() && regex.charAt(cursor) == expected;
    }
  }

  private record ClassPiece(int single, ClassSpec spec) {
    static ClassPiece single(char value) { return new ClassPiece(value, null); }
    static ClassPiece spec(ClassSpec value) { return new ClassPiece(-1, value); }
    void apply(boolean[] allowed) {
      if (single >= 0) allowed[single] = true;
      else spec.apply(allowed);
    }
  }

  private record ClassSpec(char[] lows, char[] highs) {
    ClassSpec {
      if (lows.length != highs.length) throw new IllegalArgumentException("range shape");
    }

    void apply(boolean[] allowed) {
      for (int i = 0; i < lows.length; i++) {
        Arrays.fill(allowed, lows[i], highs[i] + 1, true);
      }
    }

    String key() {
      StringBuilder key = new StringBuilder(lows.length * 10);
      for (int i = 0; i < lows.length; i++) {
        key.append((int) lows[i]).append(':').append((int) highs[i]).append(';');
      }
      return key.toString();
    }
  }

  private static final class ClassTableBuilder {
    private final List<ClassSpec> specs = new ArrayList<>();
    private final Map<String, Integer> ids = new HashMap<>();

    int literal(char value) {
      return add(new ClassSpec(new char[] {value}, new char[] {value}));
    }

    int dot(Semantics semantics) {
      boolean[] allowed = new boolean[Character.MAX_VALUE + 1];
      Arrays.fill(allowed, true);
      allowed['\n'] = false;
      if (semantics == Semantics.JDK) {
        allowed['\r'] = false;
        allowed[0x0085] = false;
        allowed[0x2028] = false;
        allowed[0x2029] = false;
      }
      return fromAllowed(allowed);
    }

    int digits(boolean negated) { return add(specDigits(negated)); }
    int word(boolean negated) { return add(specWord(negated)); }
    int space(Semantics semantics, boolean negated) {
      return add(specSpace(semantics, negated));
    }

    ClassSpec specDigits(boolean negated) {
      return maybeNegate(new ClassSpec(new char[] {'0'}, new char[] {'9'}), negated);
    }

    ClassSpec specWord(boolean negated) {
      return maybeNegate(
          new ClassSpec(
              new char[] {'0', 'A', '_', 'a'},
              new char[] {'9', 'Z', '_', 'z'}),
          negated);
    }

    ClassSpec specSpace(Semantics semantics, boolean negated) {
      ClassSpec base =
          semantics == Semantics.RE2
              ? new ClassSpec(
                  new char[] {'\t', '\f', ' '},
                  new char[] {'\n', '\r', ' '})
              : new ClassSpec(
                  new char[] {'\t', ' '},
                  new char[] {'\r', ' '});
      return maybeNegate(base, negated);
    }

    ClassSpec specPosix(String rawName) {
      boolean negated = rawName.charAt(0) == '^';
      String name = negated ? rawName.substring(1) : rawName;
      ClassSpec base =
          switch (name) {
            case "alnum" ->
                new ClassSpec(
                    new char[] {'0', 'A', 'a'},
                    new char[] {'9', 'Z', 'z'});
            case "alpha" ->
                new ClassSpec(
                    new char[] {'A', 'a'},
                    new char[] {'Z', 'z'});
            case "ascii" ->
                new ClassSpec(
                    new char[] {0x0000},
                    new char[] {0x007f});
            case "blank" ->
                new ClassSpec(
                    new char[] {'\t', ' '},
                    new char[] {'\t', ' '});
            case "cntrl" ->
                new ClassSpec(
                    new char[] {0x0000, 0x007f},
                    new char[] {0x001f, 0x007f});
            case "digit" ->
                new ClassSpec(
                    new char[] {'0'},
                    new char[] {'9'});
            case "graph" ->
                new ClassSpec(
                    new char[] {0x0021},
                    new char[] {0x007e});
            case "lower" ->
                new ClassSpec(
                    new char[] {'a'},
                    new char[] {'z'});
            case "print" ->
                new ClassSpec(
                    new char[] {0x0020},
                    new char[] {0x007e});
            case "punct" ->
                new ClassSpec(
                    new char[] {0x0021, 0x003a, 0x005b, 0x007b},
                    new char[] {0x002f, 0x0040, 0x0060, 0x007e});
            case "space" ->
                new ClassSpec(
                    new char[] {'\t', ' '},
                    new char[] {'\r', ' '});
            case "upper" ->
                new ClassSpec(
                    new char[] {'A'},
                    new char[] {'Z'});
            case "word" ->
                new ClassSpec(
                    new char[] {'0', 'A', '_', 'a'},
                    new char[] {'9', 'Z', '_', 'z'});
            case "xdigit" ->
                new ClassSpec(
                    new char[] {'0', 'A', 'a'},
                    new char[] {'9', 'F', 'f'});
            default -> throw unsupported();
          };
      return maybeNegate(base, negated);
    }

    int fromAllowed(boolean[] allowed) {
      return add(compress(allowed));
    }

    List<String> smallExactStrings(int id, int maximum) {
      if (maximum < 1) throw new IllegalArgumentException("maximum");
      ClassSpec spec = specs.get(Objects.checkIndex(id, specs.size()));
      long cardinality = 0;
      for (int range = 0; range < spec.lows.length; range++) {
        cardinality += (long) spec.highs[range] - spec.lows[range] + 1L;
        if (cardinality > maximum) return null;
      }
      ArrayList<String> result = new ArrayList<>((int) cardinality);
      for (int range = 0; range < spec.lows.length; range++) {
        int low = spec.lows[range];
        int high = spec.highs[range];
        for (int value = low; value <= high; value++) {
          result.add(String.valueOf((char) value));
        }
      }
      return List.copyOf(result);
    }

    boolean canConsumeSurrogateUnit(int id) {
      ClassSpec spec = specs.get(Objects.checkIndex(id, specs.size()));
      int surrogateLow = Character.MIN_HIGH_SURROGATE;
      int surrogateHigh = Character.MAX_LOW_SURROGATE;
      for (int range = 0; range < spec.lows.length; range++) {
        if (spec.highs[range] >= surrogateLow && spec.lows[range] <= surrogateHigh) {
          return true;
        }
      }
      return false;
    }

    long requiredPresence64(int id) {
      ClassSpec spec = specs.get(Objects.checkIndex(id, specs.size()));
      long common = -1L;
      boolean any = false;
      for (int range = 0; range < spec.lows.length; range++) {
        int low = spec.lows[range];
        int high = spec.highs[range];
        for (int value = low; value <= high; value++) {
          common &= MIndexStringBitOps.addSignal(0L, (char) value);
          any = true;
          if (common == 0L) return 0L;
        }
      }
      return any ? common : 0L;
    }

    int requiredFlags(int id) {
      ClassSpec spec = specs.get(Objects.checkIndex(id, specs.size()));
      if (spec.lows.length == 0) return 0;

      int flags = 0;
      if (allWithin(spec, 'A', 'Z')) flags |= IndexCharacterFlags.HAS_ASCII_UPPER;
      if (allWithin(spec, 'a', 'z')) flags |= IndexCharacterFlags.HAS_ASCII_LOWER;
      if (allWithin(spec, '0', '9')) flags |= IndexCharacterFlags.HAS_ASCII_DIGIT;
      if (allWord(spec)) flags |= IndexCharacterFlags.HAS_ASCII_WORD;
      if (allSpace(spec)) flags |= IndexCharacterFlags.HAS_ASCII_SPACE;
      return flags;
    }

    private static boolean allWithin(ClassSpec spec, char low, char high) {
      for (int i = 0; i < spec.lows.length; i++) {
        if (spec.lows[i] < low || spec.highs[i] > high) return false;
      }
      return true;
    }

    private static boolean allWord(ClassSpec spec) {
      for (int i = 0; i < spec.lows.length; i++) {
        char low = spec.lows[i];
        char high = spec.highs[i];
        if (!rangeWithin(low, high, '0', '9')
            && !rangeWithin(low, high, 'A', 'Z')
            && !(low == '_' && high == '_')
            && !rangeWithin(low, high, 'a', 'z')) {
          return false;
        }
      }
      return true;
    }

    private static boolean allSpace(ClassSpec spec) {
      for (int i = 0; i < spec.lows.length; i++) {
        char low = spec.lows[i];
        char high = spec.highs[i];
        if (!rangeWithin(low, high, '\t', '\r')
            && !(low == ' ' && high == ' ')) {
          return false;
        }
      }
      return true;
    }

    private static boolean rangeWithin(
        char low, char high, char allowedLow, char allowedHigh) {
      return low >= allowedLow && high <= allowedHigh;
    }

    private ClassSpec maybeNegate(ClassSpec base, boolean negated) {
      if (!negated) return base;
      boolean[] allowed = new boolean[Character.MAX_VALUE + 1];
      base.apply(allowed);
      for (int i = 0; i < allowed.length; i++) allowed[i] = !allowed[i];
      return compress(allowed);
    }

    private int add(ClassSpec spec) {
      String key = spec.key();
      Integer existing = ids.get(key);
      if (existing != null) return existing;
      int id = specs.size();
      specs.add(spec);
      ids.put(key, id);
      return id;
    }

    ClassTable freeze() {
      long[] asciiLow = new long[specs.size()];
      long[] asciiHigh = new long[specs.size()];
      int[] offsets = new int[specs.size()];
      int[] counts = new int[specs.size()];
      List<Character> lows = new ArrayList<>();
      List<Character> highs = new ArrayList<>();
      for (int id = 0; id < specs.size(); id++) {
        ClassSpec spec = specs.get(id);
        offsets[id] = lows.size();
        for (int r = 0; r < spec.lows.length; r++) {
          int lo = spec.lows[r];
          int hi = spec.highs[r];
          int asciiEnd = Math.min(hi, 127);
          for (int c = lo; c <= asciiEnd; c++) {
            if (c < 64) asciiLow[id] |= 1L << c;
            else asciiHigh[id] |= 1L << (c - 64);
          }
          if (hi >= 128) {
            lows.add((char) Math.max(128, lo));
            highs.add((char) hi);
            counts[id]++;
          }
        }
      }
      char[] lowArray = new char[lows.size()];
      char[] highArray = new char[highs.size()];
      for (int i = 0; i < lowArray.length; i++) {
        lowArray[i] = lows.get(i);
        highArray[i] = highs.get(i);
      }
      return new ClassTable(asciiLow, asciiHigh, offsets, counts, lowArray, highArray);
    }

    private static ClassSpec compress(boolean[] allowed) {
      List<Character> lows = new ArrayList<>();
      List<Character> highs = new ArrayList<>();
      int at = 0;
      while (at < allowed.length) {
        while (at < allowed.length && !allowed[at]) at++;
        if (at == allowed.length) break;
        int low = at++;
        while (at < allowed.length && allowed[at]) at++;
        lows.add((char) low);
        highs.add((char) (at - 1));
      }
      char[] lowArray = new char[lows.size()];
      char[] highArray = new char[highs.size()];
      for (int i = 0; i < lowArray.length; i++) {
        lowArray[i] = lows.get(i);
        highArray[i] = highs.get(i);
      }
      return new ClassSpec(lowArray, highArray);
    }
  }

  private record ClassTable(
      long[] asciiLow,
      long[] asciiHigh,
      int[] rangeOffset,
      int[] rangeCount,
      char[] rangeLow,
      char[] rangeHigh) {}

  private static final class Compiler {
    private final int maxStates;
    private final IProgressMonitor progress;
    private final List<State> states = new ArrayList<>();

    Compiler(int maxStates, IProgressMonitor progress) {
      this.maxStates = maxStates;
      this.progress = progress;
    }

    Fragment compile(Node node) {
      return switch (node) {
        case EmptyNode ignored -> epsilon();
        case AtomNode atom -> atom(atom.classId());
        case QuotedNode quoted -> concat(quoted.children());
        case StartNode ignored -> assertion(OP_ASSERT_START);
        case EndNode ignored -> assertion(OP_ASSERT_END);
        case WordBoundaryNode boundary ->
            assertion(
                boundary.boundary()
                    ? OP_ASSERT_WORD_BOUNDARY
                    : OP_ASSERT_NOT_WORD_BOUNDARY);
        case ConcatNode concat -> concat(concat.children());
        case AltNode alt -> alternate(alt.children());
        case RepeatNode repeat -> repeat(repeat.child(), repeat.min(), repeat.max());
      };
    }

    private Fragment epsilon() {
      int state = add(OP_JUMP, -1, -1, -1);
      return new Fragment(state, List.of(new Patch(state, 1)));
    }

    private Fragment atom(int classId) {
      int state = add(OP_CHAR, -1, -1, classId);
      return new Fragment(state, List.of(new Patch(state, 1)));
    }

    private Fragment assertion(byte opcode) {
      int state = add(opcode, -1, -1, -1);
      return new Fragment(state, List.of(new Patch(state, 1)));
    }

    private Fragment concat(List<Node> nodes) {
      Fragment result = epsilon();
      for (Node node : nodes) result = concatenate(result, compile(node));
      return result;
    }

    private Fragment alternate(List<Node> nodes) {
      Fragment result = compile(nodes.get(0));
      for (int i = 1; i < nodes.size(); i++) {
        Fragment right = compile(nodes.get(i));
        int split = add(OP_SPLIT, result.start, right.start, -1);
        List<Patch> outs = new ArrayList<>(result.outs.size() + right.outs.size());
        outs.addAll(result.outs);
        outs.addAll(right.outs);
        result = new Fragment(split, List.copyOf(outs));
      }
      return result;
    }

    private Fragment repeat(Node child, int min, int max) {
      Fragment result = epsilon();
      for (int i = 0; i < min; i++) result = concatenate(result, compile(child));
      if (max < 0) {
        Fragment body = compile(child);
        int split = add(OP_SPLIT, body.start, -1, -1);
        patch(body.outs, split);
        result = concatenate(result, new Fragment(split, List.of(new Patch(split, 2))));
      } else {
        for (int i = min; i < max; i++) {
          Fragment body = compile(child);
          int split = add(OP_SPLIT, body.start, -1, -1);
          List<Patch> outs = new ArrayList<>(body.outs.size() + 1);
          outs.addAll(body.outs);
          outs.add(new Patch(split, 2));
          result = concatenate(result, new Fragment(split, List.copyOf(outs)));
        }
      }
      return result;
    }

    private Fragment concatenate(Fragment left, Fragment right) {
      patch(left.outs, right.start);
      return new Fragment(left.start, right.outs);
    }

    int add(byte opcode, int first, int second, int classId) {
      if ((states.size() & 0x3ff) == 0) progress.checkCanceled();
      if (states.size() >= maxStates) throw unsupported();
      states.add(new State(opcode, first, second, classId));
      return states.size() - 1;
    }

    void patch(List<Patch> patches, int target) {
      for (Patch patch : patches) {
        State state = states.get(patch.state);
        if (patch.slot == 1) {
          if (state.out1 >= 0) throw new IllegalStateException("regex patch already bound");
          state.out1 = target;
        } else {
          if (state.out2 >= 0) throw new IllegalStateException("regex patch already bound");
          state.out2 = target;
        }
      }
    }

    byte[] ops() {
      byte[] result = new byte[states.size()];
      for (int i = 0; i < result.length; i++) result[i] = states.get(i).op;
      return result;
    }

    int[] out1() {
      int[] result = new int[states.size()];
      for (int i = 0; i < result.length; i++) result[i] = states.get(i).out1;
      return result;
    }

    int[] out2() {
      int[] result = new int[states.size()];
      for (int i = 0; i < result.length; i++) result[i] = states.get(i).out2;
      return result;
    }

    int[] classIds() {
      int[] result = new int[states.size()];
      for (int i = 0; i < result.length; i++) result[i] = states.get(i).classId;
      return result;
    }
  }

  private static final class State {
    private final byte op;
    private int out1;
    private int out2;
    private final int classId;

    State(byte op, int out1, int out2, int classId) {
      this.op = op;
      this.out1 = out1;
      this.out2 = out2;
      this.classId = classId;
    }
  }

  private record Fragment(int start, List<Patch> outs) {}
  private record Patch(int state, int slot) {}

  private static UnsupportedRegex unsupported() {
    return new UnsupportedRegex();
  }

  private static final class UnsupportedRegex extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }
}
