// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.internal.mindex;

import java.util.Objects;

/**
 * Immutable receive-only view of a Synexia regex v7 precomputed image.
 *
 * <p>This is transport/precompute state, not java.lang.String state and not Matcher state.
 * Primitive lanes are defensive snapshots. The source expression is metadata only and is never
 * attached to M3String/M3StringFacts. Runtime truth remains owned by java.util.regex.</p>
 */
public final class M3RX7Image {
    static final byte OP_CHAR = 1;
    static final byte OP_SPLIT = 2;
    static final byte OP_JUMP = 3;
    static final byte OP_ASSERT_START = 4;
    static final byte OP_ASSERT_END = 5;
    static final byte OP_MATCH = 6;
    static final byte OP_ASSERT_WORD_BOUNDARY = 7;
    static final byte OP_ASSERT_NOT_WORD_BOUNDARY = 8;

    private static final int ASCII = 128;
    private static final int MAX_DFA_STATES = 4096;

    private M3RX7Image() {}

    public enum Semantics {
        RE2,
        JDK
    }

    /** Portable primitive program image (Synexia image families v1/v4). */
    public record Program(
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

        public Program {
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
            if (states == 0
                    || out1.length != states
                    || out2.length != states
                    || classId.length != states
                    || startState < 0
                    || startState >= states
                    || matchState < 0
                    || matchState >= states) {
                throw new IllegalArgumentException("invalid RX7 program state geometry");
            }

            int classes = asciiLow.length;
            if (asciiHigh.length != classes
                    || rangeOffset.length != classes
                    || rangeCount.length != classes
                    || rangeLow.length != rangeHigh.length) {
                throw new IllegalArgumentException("invalid RX7 program class/range geometry");
            }

            for (int index = 0; index < classes; index++) {
                int offset = rangeOffset[index];
                int count = rangeCount[index];
                if (offset < 0
                        || count < 0
                        || offset > rangeLow.length
                        || count > rangeLow.length - offset) {
                    throw new IllegalArgumentException("invalid RX7 character range coordinate");
                }
            }
            for (int index = 0; index < rangeLow.length; index++) {
                if (rangeLow[index] > rangeHigh[index]) {
                    throw new IllegalArgumentException("invalid RX7 character range");
                }
            }

            for (int state = 0; state < states; state++) {
                int opcode = Byte.toUnsignedInt(op[state]);
                if (opcode < OP_CHAR || opcode > OP_ASSERT_NOT_WORD_BOUNDARY) {
                    throw new IllegalArgumentException("invalid RX7 opcode");
                }
                if (out1[state] < -1
                        || out1[state] >= states
                        || out2[state] < -1
                        || out2[state] >= states) {
                    throw new IllegalArgumentException("invalid RX7 transition");
                }
                if (op[state] == OP_CHAR) {
                    if (classId[state] < 0 || classId[state] >= classes || out1[state] < 0) {
                        throw new IllegalArgumentException("invalid RX7 character state");
                    }
                } else if (classId[state] != -1) {
                    throw new IllegalArgumentException("non-character RX7 state carries class id");
                }
                if (op[state] == OP_SPLIT && (out1[state] < 0 || out2[state] < 0)) {
                    throw new IllegalArgumentException("invalid RX7 split state");
                }
                if ((op[state] == OP_JUMP
                                || op[state] == OP_ASSERT_START
                                || op[state] == OP_ASSERT_END
                                || op[state] == OP_ASSERT_WORD_BOUNDARY
                                || op[state] == OP_ASSERT_NOT_WORD_BOUNDARY)
                        && out1[state] < 0) {
                    throw new IllegalArgumentException("invalid RX7 epsilon/assert state");
                }
            }

            if (op[matchState] != OP_MATCH) {
                throw new IllegalArgumentException("RX7 match coordinate is not MATCH");
            }
            if (minLength < 0 || maxLength < -1 || captureCount < 0) {
                throw new IllegalArgumentException("invalid RX7 program metadata");
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

        public int stateCount() { return op.length; }
        public int classCount() { return asciiLow.length; }
    }

    /** Exact 128-way ASCII DFA lane embedded by the donor execution image. */
    public record Dfa(int startState, short[] transitions, byte[] accepting) {
        public Dfa {
            transitions = Objects.requireNonNull(transitions, "transitions").clone();
            accepting = Objects.requireNonNull(accepting, "accepting").clone();
            int states = accepting.length;
            if (states < 1
                    || states > MAX_DFA_STATES
                    || startState < 0
                    || startState >= states
                    || transitions.length != Math.multiplyExact(states, ASCII)) {
                throw new IllegalArgumentException("invalid RX7 DFA geometry");
            }
            for (byte value : accepting) {
                if (value != 0 && value != 1) {
                    throw new IllegalArgumentException("invalid RX7 DFA accepting lane");
                }
            }
            for (short target : transitions) {
                if (target < -1 || target >= states) {
                    throw new IllegalArgumentException("invalid RX7 DFA transition");
                }
            }
        }

        @Override public short[] transitions() { return transitions.clone(); }
        @Override public byte[] accepting() { return accepting.clone(); }
        public int stateCount() { return accepting.length; }

        M3RX7 candidate(String transportRoot) {
            return M3RX7.fromAsciiDfa(transportRoot, startState, transitions, accepting);
        }
    }

    /** Fully precomputed donor execution image (families v2/v5 nested by v7). */
    public record Execution(
            Program program,
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
            Dfa anchoredAsciiDfa,
            Dfa searchAsciiDfa) {

        public Execution {
            program = Objects.requireNonNull(program, "program");
            asciiStateMasks =
                    Objects.requireNonNull(asciiStateMasks, "asciiStateMasks").clone();
            epsilonRowByState =
                    Objects.requireNonNull(epsilonRowByState, "epsilonRowByState").clone();
            epsilonTerminals =
                    Objects.requireNonNull(epsilonTerminals, "epsilonTerminals").clone();
            asciiSubsetTransitions =
                    Objects.requireNonNull(asciiSubsetTransitions, "asciiSubsetTransitions").clone();
            bmpPageByHighByte =
                    Objects.requireNonNull(bmpPageByHighByte, "bmpPageByHighByte").clone();
            bmpClassMasks = Objects.requireNonNull(bmpClassMasks, "bmpClassMasks").clone();

            int states = program.stateCount();
            int stateWords = (states + 63) >>> 6;
            if (asciiStateMasks.length != 0
                    && asciiStateMasks.length != Math.multiplyExact(ASCII, stateWords)) {
                throw new IllegalArgumentException("RX7 ASCII state-mask geometry");
            }

            if (epsilonWordsPerRow == 0) {
                if (epsilonRowByState.length != 0 || epsilonTerminals.length != 0) {
                    throw new IllegalArgumentException("RX7 epsilon geometry");
                }
            } else {
                if (epsilonWordsPerRow != stateWords
                        || epsilonRowByState.length != states
                        || epsilonTerminals.length % epsilonWordsPerRow != 0) {
                    throw new IllegalArgumentException("RX7 epsilon geometry");
                }
                int rows = epsilonTerminals.length / epsilonWordsPerRow;
                for (int row : epsilonRowByState) {
                    if (row < -1 || row >= rows) {
                        throw new IllegalArgumentException("RX7 epsilon row");
                    }
                }
            }

            if (asciiSubsetBits == 0) {
                if (asciiSubsetSourceCount != 0 || asciiSubsetTransitions.length != 0) {
                    throw new IllegalArgumentException("RX7 ASCII subset geometry");
                }
            } else {
                if ((asciiSubsetBits != 4 && asciiSubsetBits != 8)
                        || asciiSubsetSourceCount
                                != (states + asciiSubsetBits - 1) / asciiSubsetBits) {
                    throw new IllegalArgumentException("RX7 ASCII subset geometry");
                }
                long expected =
                        Math.multiplyExact(
                                Math.multiplyExact(
                                        Math.multiplyExact(
                                                (long) ASCII,
                                                asciiSubsetSourceCount),
                                        1L << asciiSubsetBits),
                                stateWords);
                if (expected != asciiSubsetTransitions.length) {
                    throw new IllegalArgumentException("RX7 ASCII subset lane length");
                }
            }

            if (bmpClassWordCount == 0) {
                if (bmpPageByHighByte.length != 0 || bmpClassMasks.length != 0) {
                    throw new IllegalArgumentException("RX7 BMP geometry");
                }
            } else {
                if (bmpPageByHighByte.length != 256
                        || bmpClassWordCount != (program.classCount() + 63) >>> 6) {
                    throw new IllegalArgumentException("RX7 BMP geometry");
                }
                int pageCount = 0;
                boolean[] seen = new boolean[256];
                for (int page : bmpPageByHighByte) {
                    if (page < -1 || page >= 256) {
                        throw new IllegalArgumentException("RX7 BMP page");
                    }
                    if (page >= 0) {
                        if (seen[page]) {
                            throw new IllegalArgumentException("RX7 duplicate BMP page");
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
                    throw new IllegalArgumentException("RX7 BMP mask length");
                }
            }
        }

        @Override public long[] asciiStateMasks() { return asciiStateMasks.clone(); }
        @Override public int[] epsilonRowByState() { return epsilonRowByState.clone(); }
        @Override public long[] epsilonTerminals() { return epsilonTerminals.clone(); }
        @Override public long[] asciiSubsetTransitions() { return asciiSubsetTransitions.clone(); }
        @Override public int[] bmpPageByHighByte() { return bmpPageByHighByte.clone(); }
        @Override public long[] bmpClassMasks() { return bmpClassMasks.clone(); }
    }

    /** Version-7 dispatch packet. The transport root is SHA-256 of the exact encoded v7 bytes. */
    public record Precomputed(
            Execution execution,
            long firstAsciiLow,
            long firstAsciiHigh,
            int branchingStateCount,
            String transportRoot) {

        public Precomputed {
            execution = Objects.requireNonNull(execution, "execution");
            if (branchingStateCount < 0 || branchingStateCount > execution.program().stateCount()) {
                throw new IllegalArgumentException("invalid RX7 branching-state count");
            }
            transportRoot = Objects.requireNonNull(transportRoot, "transportRoot");
            if (!transportRoot.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("invalid RX7 transport root");
            }
        }

        /** Anchored capture-free ASCII candidate lane, or null when the donor image omitted it. */
        public M3RX7 anchoredCandidate() {
            Dfa dfa = execution.anchoredAsciiDfa();
            return dfa == null ? null : dfa.candidate(transportRoot);
        }

        /** Search DFA stays data-only until a separately qualified search executor is received. */
        public Dfa searchDfa() {
            return execution.searchAsciiDfa();
        }
    }
}
