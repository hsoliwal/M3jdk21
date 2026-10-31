// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Precomputed Thompson-style regular-expression automaton over Unicode code points.
 *
 * <p>The compiler accepts a deliberately regular subset: literals, dot, character classes,
 * grouping, alternation, anchors and bounded/unbounded repetition. The frozen runtime is primitive
 * arrays only. Matching uses two primitive bit sets plus precomputed ASCII acceptance masks and
 * never backtracks.</p>
 *
 * <p>Unsupported constructs are reported by {@link #tryCompile(CharSequence)} as an empty result so
 * the caller can select RE2/J. No unsupported syntax is silently widened to different semantics.</p>
 */
public final class MIndexRegexAutomaton {
    private static final byte OP_MATCH = 0;
    private static final byte OP_CHAR = 1;
    private static final byte OP_ANY = 2;
    private static final byte OP_CLASS = 3;
    private static final byte OP_SPLIT = 4;
    private static final byte OP_BEGIN = 5;
    private static final byte OP_END = 6;

    private static final int MAX_EXPRESSION_UNITS = 4096;
    private static final int MAX_DEPTH = 64;
    private static final int MAX_REPEAT = 128;
    private static final int MAX_STATES = 4096;

    private final MIndexString expression;
    private final MIndexStringMetadata metadata;
    private final byte[] ops;
    private final int[] out1;
    private final int[] out2;
    private final int[] args;
    private final int[] classOffsets;
    private final int[] classStarts;
    private final int[] classEnds;
    private final boolean[] classNegated;
    private final int startState;
    private final int matchState;
    private final int bitWords;
    private final long[] asciiAcceptMasks;

    private MIndexRegexAutomaton(
            MIndexString expression,
            MIndexStringMetadata metadata,
            ProgramBuilder program,
            int startState,
            int matchState) {
        this.expression = expression;
        this.metadata = metadata == null ? MIndexStringMetadata.NONE : metadata;
        ops = Arrays.copyOf(program.ops, program.size);
        out1 = Arrays.copyOf(program.out1, program.size);
        out2 = Arrays.copyOf(program.out2, program.size);
        args = Arrays.copyOf(program.args, program.size);
        this.startState = startState;
        this.matchState = matchState;

        int classCount = program.classes.size();
        classOffsets = new int[classCount + 1];
        classNegated = new boolean[classCount];
        int rangeCount = 0;
        for (int index = 0; index < classCount; index++) {
            ClassSpec spec = program.classes.get(index);
            classOffsets[index] = rangeCount;
            classNegated[index] = spec.negated();
            rangeCount = Math.addExact(rangeCount, spec.starts().length);
        }
        classOffsets[classCount] = rangeCount;
        classStarts = new int[rangeCount];
        classEnds = new int[rangeCount];
        int cursor = 0;
        for (ClassSpec spec : program.classes) {
            System.arraycopy(spec.starts(), 0, classStarts, cursor, spec.starts().length);
            System.arraycopy(spec.ends(), 0, classEnds, cursor, spec.ends().length);
            cursor += spec.starts().length;
        }

        bitWords = Math.max(1, (ops.length + Long.SIZE - 1) >>> 6);
        asciiAcceptMasks = new long[Math.multiplyExact(128, bitWords)];
        for (int state = 0; state < ops.length; state++) {
            if (!consuming(ops[state])) continue;
            for (int codePoint = 0; codePoint < 128; codePoint++) {
                if (accepts(state, codePoint)) {
                    int at = codePoint * bitWords + (state >>> 6);
                    asciiAcceptMasks[at] |= 1L << (state & 63);
                }
            }
        }
        validate();
    }

    public static Optional<MIndexRegexAutomaton> tryCompile(CharSequence expression) {
        Objects.requireNonNull(expression, "expression");
        return tryCompile(MIndexString.from(expression), MIndexStringMetadata.NONE);
    }

    /** Preserves pattern-side lexical/semantic/relation/source metadata when supplied. */
    public static Optional<MIndexRegexAutomaton> tryCompile(MIndexAnnotatedString expression) {
        Objects.requireNonNull(expression, "expression");
        return tryCompile(expression.materialize(), expression.metadata());
    }

    static Optional<MIndexRegexAutomaton> tryCompile(
            MIndexString expression,
            MIndexStringMetadata metadata) {
        if (expression.length() > MAX_EXPRESSION_UNITS) return Optional.empty();
        try {
            Parser parser = new Parser(expression.toString());
            Node root = parser.parse();
            ProgramBuilder program = new ProgramBuilder();
            int match = program.add(OP_MATCH, -1, -1, 0);
            int start = program.compile(root, match);
            return Optional.of(new MIndexRegexAutomaton(
                    expression, metadata, program, start, match));
        } catch (UnsupportedOperationException unsupported) {
            return Optional.empty();
        }
    }

    public MIndexString expression() { return expression; }
    public MIndexStringMetadata metadata() { return metadata; }
    public int stateCount() { return ops.length; }
    public int classCount() { return classNegated.length; }

    /** Primitive frozen payload, excluding the already-owned canonical expression spelling. */
    public long primitivePayloadBytes() {
        return ops.length
                + Integer.BYTES * (long) (out1.length + out2.length + args.length
                + classOffsets.length + classStarts.length + classEnds.length)
                + classNegated.length
                + Long.BYTES * (long) asciiAcceptMasks.length;
    }

    /**
     * Precompute the existing epsilon/assertion closure for every state and begin/end context.
     * The caller owns this optional table; legacy matching and its RE2/J-oriented semantics stay
     * unchanged. The byte budget covers retained primitive table payload, not build scratch/headers.
     */
    public Plan prepare(long maxBytes, MIndexProgressMonitor monitor) {
        if (maxBytes < 0) throw new IllegalArgumentException("negative closure budget");
        long cells = Math.multiplyExact(4L * ops.length, bitWords);
        long bytes = Math.multiplyExact(cells, Long.BYTES);
        if (bytes > maxBytes || cells > Integer.MAX_VALUE)
            throw new IllegalArgumentException("closure table exceeds budget");
        var progress = monitor == null ? MIndexProgressMonitor.none() : monitor;
        progress.checkCancelled();
        progress.begin("regex closure precompute", 4L * ops.length);
        try {
            long[] table = new long[(int) cells];
            Scratch scratch = new Scratch(ops.length, bitWords);
            for (int context = 0; context < 4; context++) {
                for (int state = 0; state < ops.length; state++) {
                    progress.checkCancelled();
                    Arrays.fill(scratch.current, 0L);
                    scratch.seeds[0] = state;
                    closure(scratch.current, scratch.seeds, 1,
                            (context & 2) != 0, (context & 1) != 0, scratch);
                    int row = (context * ops.length + state) * bitWords;
                    System.arraycopy(scratch.current, 0, table, row, bitWords);
                    progress.worked(1);
                }
            }
            progress.checkCancelled();
            return new Plan(this, table);
        } finally {
            progress.done();
        }
    }

    public Plan prepare(long maxBytes) { return prepare(maxBytes, null); }

    /**
     * Immutable caller-owned closure table over one exact automaton. Query scratch is invocation
     * local, so the plan may be shared across threads. No input spelling or result is retained.
     */
    public static final class Plan {
        private final MIndexRegexAutomaton automaton;
        private final long[] closures;

        private Plan(MIndexRegexAutomaton automaton, long[] closures) {
            this.automaton = automaton;
            this.closures = closures;
        }

        public MIndexRegexAutomaton automaton() { return automaton; }
        public long primitivePayloadBytes() { return (long) closures.length * Long.BYTES; }
        public boolean matches(CharSequence input) { return matches(input, null); }
        public boolean matches(CharSequence input, MIndexProgressMonitor monitor) {
            return automaton.matches(input, closures, monitor);
        }
        public boolean find(CharSequence input) { return find(input, null); }
        public boolean find(CharSequence input, MIndexProgressMonitor monitor) {
            return automaton.find(input, closures, monitor);
        }
        public boolean matchesUtf8(byte[] source) { return matchesUtf8(source, null); }
        public boolean matchesUtf8(byte[] source, MIndexProgressMonitor monitor) {
            Objects.requireNonNull(source, "source");
            return matchesUtf8(source, 0, source.length, monitor);
        }
        public boolean matchesUtf8(byte[] source, int offset, int length) {
            return matchesUtf8(source, offset, length, null);
        }
        public boolean matchesUtf8(byte[] source, int offset, int length, MIndexProgressMonitor monitor) {
            return automaton.matchesUtf8(source, offset, length, closures, monitor);
        }
        public boolean findUtf8(byte[] source) { return findUtf8(source, null); }
        public boolean findUtf8(byte[] source, MIndexProgressMonitor monitor) {
            Objects.requireNonNull(source, "source");
            return findUtf8(source, 0, source.length, monitor);
        }
        public boolean findUtf8(byte[] source, int offset, int length) {
            return findUtf8(source, offset, length, null);
        }
        public boolean findUtf8(byte[] source, int offset, int length, MIndexProgressMonitor monitor) {
            return automaton.findUtf8(source, offset, length, closures, monitor);
        }
    }

    public boolean matches(CharSequence input) {
        return matches(input, null, null);
    }

    private boolean matches(CharSequence input, long[] closures, MIndexProgressMonitor monitor) {
        Objects.requireNonNull(input, "input");
        if (monitor != null) {
            monitor.checkCancelled();
            monitor.begin("regex matches", input.length());
        }
        try {
            Scratch scratch = closures == null ? new Scratch(ops.length, bitWords)
                    : new PreparedScratch(ops.length, bitWords, closures);
            scratch.seeds[0] = startState;
            closure(
                    scratch.current,
                    scratch.seeds,
                    1,
                    true,
                    input.length() == 0,
                    scratch);

            int offset = 0;
            while (offset < input.length()) {
                if (monitor != null) monitor.checkCancelled();
                int codePoint = Character.codePointAt(input, offset);
                int nextOffset = offset + Character.charCount(codePoint);
                transition(
                        scratch.current,
                        codePoint,
                        nextOffset == input.length(),
                        scratch.next,
                        scratch);
                long[] swap = scratch.current;
                scratch.current = scratch.next;
                scratch.next = swap;
                offset = nextOffset;
                if (monitor != null) {
                    monitor.worked(Character.charCount(codePoint));
                    monitor.checkCancelled();
                }
            }
            return containsState(scratch.current, matchState);
        } finally {
            if (monitor != null) monitor.done();
        }
    }

    public boolean find(CharSequence input) {
        return find(input, null, null);
    }

    private boolean find(CharSequence input, long[] closures, MIndexProgressMonitor monitor) {
        Objects.requireNonNull(input, "input");
        if (monitor != null) {
            monitor.checkCancelled();
            monitor.begin("regex find", input.length());
        }
        try {
            Scratch scratch = closures == null ? new Scratch(ops.length, bitWords)
                    : new PreparedScratch(ops.length, bitWords, closures);
            int offset = 0;
            while (true) {
                if (monitor != null) monitor.checkCancelled();
                scratch.seeds[0] = startState;
                closure(
                        scratch.current,
                        scratch.seeds,
                        1,
                        offset == 0,
                        offset == input.length(),
                        scratch);
                if (containsState(scratch.current, matchState)) return true;
                if (offset == input.length()) return false;

                int codePoint = Character.codePointAt(input, offset);
                int nextOffset = offset + Character.charCount(codePoint);
                transition(
                        scratch.current,
                        codePoint,
                        nextOffset == input.length(),
                        scratch.next,
                        scratch);
                long[] swap = scratch.current;
                scratch.current = scratch.next;
                scratch.next = swap;
                offset = nextOffset;
                if (monitor != null) {
                    monitor.worked(Character.charCount(codePoint));
                    monitor.checkCancelled();
                }
                if (containsState(scratch.current, matchState)) return true;
            }
        } finally {
            if (monitor != null) monitor.done();
        }
    }

    public boolean matchesUtf8(byte[] source) {
        Objects.requireNonNull(source, "source");
        return matchesUtf8(source, 0, source.length);
    }

    public boolean matchesUtf8(byte[] source, int offset, int length) {
        return matchesUtf8(source, offset, length, null, null);
    }

    private boolean matchesUtf8(byte[] source, int offset, int length, long[] closures, MIndexProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        if (monitor != null) {
            monitor.checkCancelled();
            monitor.begin("regex matchesUtf8", length);
        }
        try {
            Scratch scratch = closures == null ? new Scratch(ops.length, bitWords)
                    : new PreparedScratch(ops.length, bitWords, closures);
            int end = offset + length;
            scratch.seeds[0] = startState;
            closure(scratch.current, scratch.seeds, 1, true, offset == end, scratch);

            int cursor = offset;
            while (cursor < end) {
                if (monitor != null) monitor.checkCancelled();
                long decoded = decodeUtf8(source, cursor, end);
                int width = (int) (decoded >>> 32);
                int codePoint = (int) decoded;
                cursor += width;
                if (monitor != null) {
                    monitor.worked(width);
                    monitor.checkCancelled();
                }
                transition(
                        scratch.current,
                        codePoint,
                        cursor == end,
                        scratch.next,
                        scratch);
                long[] swap = scratch.current;
                scratch.current = scratch.next;
                scratch.next = swap;
            }
            return containsState(scratch.current, matchState);
        } finally {
            if (monitor != null) monitor.done();
        }
    }

    public boolean findUtf8(byte[] source) {
        Objects.requireNonNull(source, "source");
        return findUtf8(source, 0, source.length);
    }

    public boolean findUtf8(byte[] source, int offset, int length) {
        return findUtf8(source, offset, length, null, null);
    }

    private boolean findUtf8(byte[] source, int offset, int length, long[] closures, MIndexProgressMonitor monitor) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        if (monitor != null) {
            monitor.checkCancelled();
            monitor.begin("regex findUtf8", length);
        }
        try {
            Scratch scratch = closures == null ? new Scratch(ops.length, bitWords)
                    : new PreparedScratch(ops.length, bitWords, closures);
            int end = offset + length;
            int cursor = offset;
            while (true) {
                if (monitor != null) monitor.checkCancelled();
                scratch.seeds[0] = startState;
                closure(
                        scratch.current,
                        scratch.seeds,
                        1,
                        cursor == offset,
                        cursor == end,
                        scratch);
                if (containsState(scratch.current, matchState)) return true;
                if (cursor == end) return false;

                long decoded = decodeUtf8(source, cursor, end);
                int width = (int) (decoded >>> 32);
                int codePoint = (int) decoded;
                cursor += width;
                if (monitor != null) {
                    monitor.worked(width);
                    monitor.checkCancelled();
                }
                transition(
                        scratch.current,
                        codePoint,
                        cursor == end,
                        scratch.next,
                        scratch);
                long[] swap = scratch.current;
                scratch.current = scratch.next;
                scratch.next = swap;
                if (containsState(scratch.current, matchState)) return true;
            }
        } finally {
            if (monitor != null) monitor.done();
        }
    }

    private void transition(
            long[] current,
            int codePoint,
            boolean atEnd,
            long[] next,
            Scratch scratch) {
        Arrays.fill(next, 0L);
        int seedCount = 0;
        if (codePoint >= 0 && codePoint < 128) {
            int maskBase = codePoint * bitWords;
            for (int word = 0; word < bitWords; word++) {
                long matched = current[word] & asciiAcceptMasks[maskBase + word];
                while (matched != 0L) {
                    int bit = Long.numberOfTrailingZeros(matched);
                    int state = (word << 6) + bit;
                    scratch.seeds[seedCount++] = out1[state];
                    matched &= matched - 1L;
                }
            }
        } else {
            for (int word = 0; word < bitWords; word++) {
                long active = current[word];
                while (active != 0L) {
                    int bit = Long.numberOfTrailingZeros(active);
                    int state = (word << 6) + bit;
                    if (state < ops.length && consuming(ops[state])
                            && accepts(state, codePoint)) {
                        scratch.seeds[seedCount++] = out1[state];
                    }
                    active &= active - 1L;
                }
            }
        }
        if (seedCount != 0) {
            closure(next, scratch.seeds, seedCount, false, atEnd, scratch);
        }
    }

    private void closure(
            long[] target,
            int[] seeds,
            int seedCount,
            boolean atBegin,
            boolean atEnd,
            Scratch scratch) {
        if (scratch instanceof PreparedScratch prepared) {
            int context = (atBegin ? 2 : 0) | (atEnd ? 1 : 0);
            int base = context * ops.length * bitWords;
            for (int index = 0; index < seedCount; index++) {
                int row = base + seeds[index] * bitWords;
                for (int word = 0; word < bitWords; word++)
                    target[word] |= prepared.closures[row + word];
            }
            return;
        }
        int generation = scratch.nextGeneration();
        int stackSize = 0;
        for (int index = 0; index < seedCount; index++) {
            stackSize = pushUnique(
                    seeds[index], scratch.stack, stackSize,
                    scratch.marks, generation);
        }
        while (stackSize != 0) {
            int state = scratch.stack[--stackSize];
            switch (ops[state]) {
                case OP_SPLIT -> {
                    stackSize = pushUnique(
                            out1[state], scratch.stack, stackSize,
                            scratch.marks, generation);
                    stackSize = pushUnique(
                            out2[state], scratch.stack, stackSize,
                            scratch.marks, generation);
                }
                case OP_BEGIN -> {
                    if (atBegin) {
                        stackSize = pushUnique(
                                out1[state], scratch.stack, stackSize,
                                scratch.marks, generation);
                    }
                }
                case OP_END -> {
                    if (atEnd) {
                        stackSize = pushUnique(
                                out1[state], scratch.stack, stackSize,
                                scratch.marks, generation);
                    }
                }
                default -> setState(target, state);
            }
        }
    }

    private int pushUnique(
            int state,
            int[] stack,
            int stackSize,
            int[] marks,
            int generation) {
        if (state < 0 || state >= ops.length) {
            throw new IllegalStateException("invalid automaton edge");
        }
        if (marks[state] == generation) return stackSize;
        marks[state] = generation;
        stack[stackSize] = state;
        return stackSize + 1;
    }

    private boolean accepts(int state, int codePoint) {
        return switch (ops[state]) {
            case OP_CHAR -> args[state] == codePoint;
            case OP_ANY -> codePoint != '\n';
            case OP_CLASS -> classMatches(args[state], codePoint);
            default -> false;
        };
    }

    private boolean classMatches(int classId, int codePoint) {
        int start = classOffsets[classId];
        int end = classOffsets[classId + 1];
        boolean contained = false;
        int low = start;
        int high = end;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (classStarts[middle] <= codePoint) low = middle + 1;
            else high = middle;
        }
        int candidate = low - 1;
        if (candidate >= start && codePoint <= classEnds[candidate]) contained = true;
        return classNegated[classId] ? !contained : contained;
    }

    private void validate() {
        if (ops.length == 0
                || startState < 0 || startState >= ops.length
                || matchState < 0 || matchState >= ops.length
                || ops[matchState] != OP_MATCH) {
            throw new IllegalStateException("invalid automaton endpoints");
        }
        for (int state = 0; state < ops.length; state++) {
            byte op = ops[state];
            if (op < OP_MATCH || op > OP_END) {
                throw new IllegalStateException("invalid automaton opcode");
            }
            if (op == OP_SPLIT) {
                requireState(out1[state]);
                requireState(out2[state]);
            } else if (op != OP_MATCH) {
                requireState(out1[state]);
            }
            if (op == OP_CLASS
                    && (args[state] < 0 || args[state] >= classNegated.length)) {
                throw new IllegalStateException("invalid class reference");
            }
        }
    }

    private void requireState(int state) {
        if (state < 0 || state >= ops.length) {
            throw new IllegalStateException("invalid automaton state");
        }
    }

    private static boolean consuming(byte op) {
        return op == OP_CHAR || op == OP_ANY || op == OP_CLASS;
    }

    private static void setState(long[] bits, int state) {
        bits[state >>> 6] |= 1L << (state & 63);
    }

    private static boolean containsState(long[] bits, int state) {
        return (bits[state >>> 6] & (1L << (state & 63))) != 0L;
    }

    private static long decodeUtf8(byte[] source, int offset, int end) {
        int head = Byte.toUnsignedInt(source[offset]);
        if (head < 0x80) return (1L << 32) | head;

        int width;
        int codePoint;
        int minimum;
        if (head >= 0xc2 && head <= 0xdf) {
            width = 2;
            codePoint = head & 0x1f;
            minimum = 0x80;
        } else if (head >= 0xe0 && head <= 0xef) {
            width = 3;
            codePoint = head & 0x0f;
            minimum = 0x800;
        } else if (head >= 0xf0 && head <= 0xf4) {
            width = 4;
            codePoint = head & 0x07;
            minimum = 0x10000;
        } else {
            throw invalidUtf8(offset);
        }
        if (width > end - offset) throw invalidUtf8(offset);
        for (int index = 1; index < width; index++) {
            int tail = Byte.toUnsignedInt(source[offset + index]);
            if ((tail & 0xc0) != 0x80) throw invalidUtf8(offset);
            codePoint = (codePoint << 6) | (tail & 0x3f);
        }
        if (codePoint < minimum
                || codePoint > Character.MAX_CODE_POINT
                || codePoint >= Character.MIN_SURROGATE
                && codePoint <= Character.MAX_SURROGATE) {
            throw invalidUtf8(offset);
        }
        return ((long) width << 32) | Integer.toUnsignedLong(codePoint);
    }

    private static IllegalArgumentException invalidUtf8(int offset) {
        return new IllegalArgumentException("invalid UTF-8 at byte offset " + offset);
    }

    private static class Scratch {
        private static final int[] EMPTY = new int[0];
        private long[] current;
        private long[] next;
        private final int[] seeds;
        private final int[] stack;
        private final int[] marks;
        private int generation;

        Scratch(int stateCount, int words) {
            this(stateCount, words, false);
        }

        Scratch(int stateCount, int words, boolean prepared) {
            current = new long[words];
            next = new long[words];
            seeds = new int[stateCount];
            stack = prepared ? EMPTY : new int[stateCount];
            marks = prepared ? EMPTY : new int[stateCount];
        }

        int nextGeneration() {
            generation++;
            if (generation == 0) {
                Arrays.fill(marks, 0);
                generation = 1;
            }
            return generation;
        }
    }

    /** Only explicitly prepared queries carry the optional table reference. */
    private static final class PreparedScratch extends Scratch {
        private final long[] closures;
        PreparedScratch(int stateCount, int words, long[] closures) {
            super(stateCount, words, true);
            this.closures = closures;
        }
    }

    private sealed interface Node permits EmptyNode, LiteralNode, AnyNode, ClassNode,
            BeginNode, EndNode, SequenceNode, AlternativeNode, RepeatNode {}

    private record EmptyNode() implements Node {}
    private record LiteralNode(int codePoint) implements Node {}
    private record AnyNode() implements Node {}
    private record ClassNode(ClassSpec spec) implements Node {}
    private record BeginNode() implements Node {}
    private record EndNode() implements Node {}
    private record SequenceNode(List<Node> children) implements Node {}
    private record AlternativeNode(List<Node> children) implements Node {}
    private record RepeatNode(Node child, int minimum, int maximum) implements Node {}

    private record IntRange(int start, int end) {
        IntRange {
            if (start < 0 || end < start || end > Character.MAX_CODE_POINT) {
                throw new IllegalArgumentException("invalid code-point range");
            }
        }
    }

    private record ClassSpec(int[] starts, int[] ends, boolean negated) {
        ClassSpec {
            if (starts.length == 0 || starts.length != ends.length) {
                throw new IllegalArgumentException("invalid class ranges");
            }
            starts = starts.clone();
            ends = ends.clone();
        }
    }

    private record ClassPiece(List<IntRange> ranges, int singleton) {}

    private static final class ProgramBuilder {
        private byte[] ops = new byte[64];
        private int[] out1 = new int[64];
        private int[] out2 = new int[64];
        private int[] args = new int[64];
        private final ArrayList<ClassSpec> classes = new ArrayList<>();
        private int size;

        int compile(Node node, int next) {
            if (node instanceof EmptyNode) return next;
            if (node instanceof LiteralNode literal) {
                return add(OP_CHAR, next, -1, literal.codePoint());
            }
            if (node instanceof AnyNode) return add(OP_ANY, next, -1, 0);
            if (node instanceof ClassNode characterClass) {
                int classId = classes.size();
                classes.add(characterClass.spec());
                return add(OP_CLASS, next, -1, classId);
            }
            if (node instanceof BeginNode) return add(OP_BEGIN, next, -1, 0);
            if (node instanceof EndNode) return add(OP_END, next, -1, 0);
            if (node instanceof SequenceNode sequence) {
                int start = next;
                List<Node> children = sequence.children();
                for (int index = children.size() - 1; index >= 0; index--) {
                    start = compile(children.get(index), start);
                }
                return start;
            }
            if (node instanceof AlternativeNode alternative) {
                List<Node> children = alternative.children();
                int start = compile(children.get(children.size() - 1), next);
                for (int index = children.size() - 2; index >= 0; index--) {
                    int left = compile(children.get(index), next);
                    start = add(OP_SPLIT, left, start, 0);
                }
                return start;
            }
            RepeatNode repeat = (RepeatNode) node;
            return compileRepeat(repeat, next);
        }

        private int compileRepeat(RepeatNode repeat, int next) {
            int minimum = repeat.minimum();
            int maximum = repeat.maximum();
            int start = next;
            if (maximum < 0) {
                int split = add(OP_SPLIT, -1, next, 0);
                int body = compile(repeat.child(), split);
                out1[split] = body;
                start = split;
            } else {
                for (int count = minimum; count < maximum; count++) {
                    int body = compile(repeat.child(), start);
                    start = add(OP_SPLIT, body, start, 0);
                }
            }
            for (int count = 0; count < minimum; count++) {
                start = compile(repeat.child(), start);
            }
            return start;
        }

        int add(byte op, int first, int second, int arg) {
            if (size == MAX_STATES) {
                throw new UnsupportedOperationException("regex automaton state budget");
            }
            grow(size + 1);
            int state = size++;
            ops[state] = op;
            out1[state] = first;
            out2[state] = second;
            args[state] = arg;
            return state;
        }

        private void grow(int required) {
            if (required <= ops.length) return;
            int capacity = Math.max(required, Math.multiplyExact(ops.length, 2));
            ops = Arrays.copyOf(ops, capacity);
            out1 = Arrays.copyOf(out1, capacity);
            out2 = Arrays.copyOf(out2, capacity);
            args = Arrays.copyOf(args, capacity);
        }
    }

    private static final class Parser {
        private final String regex;
        private int position;

        Parser(String regex) {
            this.regex = regex;
        }

        Node parse() {
            Node node = alternative(0);
            if (position != regex.length()) unsupported("trailing regex syntax");
            return node;
        }

        private Node alternative(int depth) {
            requireDepth(depth);
            ArrayList<Node> branches = new ArrayList<>();
            branches.add(sequence(depth));
            while (peek('|')) {
                position++;
                branches.add(sequence(depth));
            }
            return branches.size() == 1
                    ? branches.get(0)
                    : new AlternativeNode(List.copyOf(branches));
        }

        private Node sequence(int depth) {
            ArrayList<Node> children = new ArrayList<>();
            while (position < regex.length() && !peek('|') && !peek(')')) {
                Node atom = atom(depth);
                if (position < regex.length() && quantifierStart(regex.charAt(position))) {
                    atom = quantified(atom);
                }
                children.add(atom);
            }
            if (children.isEmpty()) return new EmptyNode();
            return children.size() == 1
                    ? children.get(0)
                    : new SequenceNode(List.copyOf(children));
        }

        private Node atom(int depth) {
            if (position == regex.length()) unsupported("missing regex atom");
            char value = regex.charAt(position++);
            return switch (value) {
                case '(' -> group(depth + 1);
                case '[' -> new ClassNode(characterClass());
                case '\\' -> escaped();
                case '^' -> new BeginNode();
                case '$' -> new EndNode();
                case '.' -> new AnyNode();
                case '*', '+', '?', '{', '}', ']' -> throw unsupported("unexpected regex operator");
                default -> {
                    int start = position - 1;
                    int codePoint = regex.codePointAt(start);
                    position = start + Character.charCount(codePoint);
                    yield new LiteralNode(codePoint);
                }
            };
        }

        private Node group(int depth) {
            requireDepth(depth);
            if (peek('?')) {
                position++;
                if (!peek(':')) unsupported("group extension requires residual regex engine");
                position++;
            }
            Node child = alternative(depth);
            if (!peek(')')) unsupported("unclosed group");
            position++;
            return child;
        }

        private Node escaped() {
            if (position == regex.length()) unsupported("trailing escape");
            char value = regex.charAt(position++);
            return switch (value) {
                case 'd' -> new ClassNode(digitClass(false));
                case 'D' -> new ClassNode(digitClass(true));
                case 's' -> new ClassNode(spaceClass(false));
                case 'S' -> new ClassNode(spaceClass(true));
                case 'w' -> new ClassNode(wordClass(false));
                case 'W' -> new ClassNode(wordClass(true));
                case 'n' -> new LiteralNode('\n');
                case 'r' -> new LiteralNode('\r');
                case 't' -> new LiteralNode('\t');
                case 'f' -> new LiteralNode('\f');
                case 'A' -> new BeginNode();
                case 'z' -> new EndNode();
                case 'x' -> new LiteralNode(hex(2));
                case 'Q' -> quoted();
                default -> {
                    if ("\\.^$|?*+()[]{}-".indexOf(value) < 0) {
                        unsupported("escape requires residual regex engine");
                    }
                    yield new LiteralNode(value);
                }
            };
        }

        private Node quoted() {
            int close = regex.indexOf("\\E", position);
            int end = close < 0 ? regex.length() : close;
            Node node = literalRun(regex.substring(position, end));
            position = close < 0 ? end : end + 2;
            return node;
        }

        private Node quantified(Node child) {
            if (containsAssertion(child)) {
                unsupported("quantified assertion requires residual regex engine");
            }
            int minimum;
            int maximum;
            char kind = regex.charAt(position++);
            if (kind == '*') {
                minimum = 0;
                maximum = -1;
            } else if (kind == '+') {
                minimum = 1;
                maximum = -1;
            } else if (kind == '?') {
                minimum = 0;
                maximum = 1;
            } else {
                minimum = decimal();
                maximum = minimum;
                if (peek(',')) {
                    position++;
                    maximum = peek('}') ? -1 : decimal();
                }
                if (!peek('}')) unsupported("unclosed counted repetition");
                position++;
            }
            if (minimum < 0 || minimum > MAX_REPEAT
                    || maximum >= 0 && (maximum < minimum || maximum > MAX_REPEAT)) {
                unsupported("counted repetition exceeds automaton budget");
            }
            if (peek('?')) {
                position++; // reluctant quantifiers accept the same language for boolean matching.
            } else if (peek('+')) {
                unsupported("possessive quantifier changes accepted language");
            }
            if (position < regex.length() && quantifierStart(regex.charAt(position))) {
                unsupported("nested quantifier");
            }
            return new RepeatNode(child, minimum, maximum);
        }

        private ClassSpec characterClass() {
            boolean negated = false;
            if (peek('^')) {
                negated = true;
                position++;
            }
            if (position == regex.length() || peek(']')) {
                unsupported("empty or leading-close character class");
            }

            ArrayList<IntRange> ranges = new ArrayList<>();
            boolean closed = false;
            while (position < regex.length()) {
                if (peek(']')) {
                    position++;
                    closed = true;
                    break;
                }
                ClassPiece left = classPiece();
                if (peek('-')
                        && position + 1 < regex.length()
                        && regex.charAt(position + 1) != ']') {
                    if (left.singleton() < 0) unsupported("range endpoint must be one scalar");
                    position++;
                    ClassPiece right = classPiece();
                    if (right.singleton() < 0) unsupported("range endpoint must be one scalar");
                    if (right.singleton() < left.singleton()) unsupported("descending character range");
                    ranges.add(new IntRange(left.singleton(), right.singleton()));
                } else {
                    ranges.addAll(left.ranges());
                }
            }
            if (!closed) unsupported("unclosed character class");
            return normalize(ranges, negated);
        }

        private ClassPiece classPiece() {
            if (position == regex.length()) unsupported("truncated character class");
            char value = regex.charAt(position++);
            if (value != '\\') {
                int start = position - 1;
                int codePoint = regex.codePointAt(start);
                position = start + Character.charCount(codePoint);
                return singleton(codePoint);
            }
            if (position == regex.length()) unsupported("trailing class escape");
            char escaped = regex.charAt(position++);
            return switch (escaped) {
                case 'd' -> piece(digitClass(false));
                case 's' -> piece(spaceClass(false));
                case 'w' -> piece(wordClass(false));
                case 'n' -> singleton('\n');
                case 'r' -> singleton('\r');
                case 't' -> singleton('\t');
                case 'f' -> singleton('\f');
                case 'x' -> singleton(hex(2));
                default -> {
                    if ("\\^-[]{}.?*+|()".indexOf(escaped) < 0) {
                        unsupported("class escape requires residual regex engine");
                    }
                    yield singleton(escaped);
                }
            };
        }

        private int hex(int digits) {
            if (digits > regex.length() - position) unsupported("truncated hex escape");
            int value = 0;
            for (int index = 0; index < digits; index++) {
                int digit = Character.digit(regex.charAt(position++), 16);
                if (digit < 0) unsupported("invalid hex escape");
                value = (value << 4) | digit;
            }
            if (!Character.isValidCodePoint(value)
                    || value >= Character.MIN_SURROGATE
                    && value <= Character.MAX_SURROGATE) {
                unsupported("invalid Unicode scalar");
            }
            return value;
        }

        private int decimal() {
            if (position == regex.length() || !Character.isDigit(regex.charAt(position))) {
                unsupported("missing repetition count");
            }
            int value = 0;
            while (position < regex.length() && Character.isDigit(regex.charAt(position))) {
                int digit = regex.charAt(position++) - '0';
                if (value > MAX_REPEAT) unsupported("repetition count budget");
                value = value * 10 + digit;
            }
            return value;
        }

        private boolean peek(char value) {
            return position < regex.length() && regex.charAt(position) == value;
        }

        private static boolean quantifierStart(char value) {
            return value == '*' || value == '+' || value == '?' || value == '{';
        }

        private static void requireDepth(int depth) {
            if (depth > MAX_DEPTH) unsupported("regex nesting budget");
        }
    }

    private static Node literalRun(String value) {
        if (value.isEmpty()) return new EmptyNode();
        ArrayList<Node> nodes = new ArrayList<>();
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            nodes.add(new LiteralNode(codePoint));
            offset += Character.charCount(codePoint);
        }
        return nodes.size() == 1
                ? nodes.get(0)
                : new SequenceNode(List.copyOf(nodes));
    }

    private static boolean containsAssertion(Node node) {
        if (node instanceof BeginNode || node instanceof EndNode) return true;
        if (node instanceof SequenceNode sequence) {
            for (Node child : sequence.children()) {
                if (containsAssertion(child)) return true;
            }
        } else if (node instanceof AlternativeNode alternative) {
            for (Node child : alternative.children()) {
                if (containsAssertion(child)) return true;
            }
        } else if (node instanceof RepeatNode repeat) {
            return containsAssertion(repeat.child());
        }
        return false;
    }

    private static ClassPiece singleton(int codePoint) {
        return new ClassPiece(List.of(new IntRange(codePoint, codePoint)), codePoint);
    }

    private static ClassPiece piece(ClassSpec spec) {
        if (spec.negated()) unsupported("negated shorthand inside class");
        ArrayList<IntRange> ranges = new ArrayList<>(spec.starts().length);
        for (int index = 0; index < spec.starts().length; index++) {
            ranges.add(new IntRange(spec.starts()[index], spec.ends()[index]));
        }
        int singleton = ranges.size() == 1
                && ranges.get(0).start() == ranges.get(0).end()
                ? ranges.get(0).start() : -1;
        return new ClassPiece(List.copyOf(ranges), singleton);
    }

    private static ClassSpec digitClass(boolean negated) {
        return new ClassSpec(new int[] {'0'}, new int[] {'9'}, negated);
    }

    private static ClassSpec wordClass(boolean negated) {
        return new ClassSpec(
                new int[] {'0', 'A', '_', 'a'},
                new int[] {'9', 'Z', '_', 'z'},
                negated);
    }

    private static ClassSpec spaceClass(boolean negated) {
        return new ClassSpec(
                new int[] {'\t', '\n', '\f', '\r', ' '},
                new int[] {'\t', '\n', '\f', '\r', ' '},
                negated);
    }

    private static ClassSpec normalize(List<IntRange> ranges, boolean negated) {
        if (ranges.isEmpty()) unsupported("empty character class");
        ArrayList<IntRange> ordered = new ArrayList<>(ranges);
        ordered.sort(Comparator.comparingInt(IntRange::start).thenComparingInt(IntRange::end));
        ArrayList<IntRange> merged = new ArrayList<>(ordered.size());
        for (IntRange range : ordered) {
            if (merged.isEmpty()) {
                merged.add(range);
                continue;
            }
            IntRange previous = merged.get(merged.size() - 1);
            boolean adjacent = previous.end() != Character.MAX_CODE_POINT
                    && range.start() == previous.end() + 1;
            if (range.start() <= previous.end() || adjacent) {
                merged.set(
                        merged.size() - 1,
                        new IntRange(previous.start(), Math.max(previous.end(), range.end())));
            } else {
                merged.add(range);
            }
        }
        int[] starts = new int[merged.size()];
        int[] ends = new int[merged.size()];
        for (int index = 0; index < merged.size(); index++) {
            starts[index] = merged.get(index).start();
            ends[index] = merged.get(index).end();
        }
        return new ClassSpec(starts, ends, negated);
    }

    private static UnsupportedOperationException unsupported(String message) {
        throw new UnsupportedOperationException(message);
    }
}
