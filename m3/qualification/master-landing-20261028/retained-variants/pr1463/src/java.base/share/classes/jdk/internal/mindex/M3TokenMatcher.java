/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.PatternSyntaxException;

/**
 * Match state of one {@link M3TokenPattern} over one {@link M3TokenIndex}.
 * Groups are half-open lossless token positions, with corresponding source spans.
 *
 * <p>Group zero is the complete match. A repeated group retains its last participating
 * iteration; an absent group has both positions -1. Written alternatives and greedy or
 * reluctant repetition define path priority. An iteration consuming no token exits that
 * repetition; it cannot satisfy further required iterations. Significant-only matching skips
 * trivia without renumbering the returned token coordinates. Empty captures use the next
 * significant token boundary, or the lossless tape end.
 *
 * <p>The pattern owns one compiled, immutable tagged Thompson program. Matchers execute it
 * with ordered primitive state and capture lanes, merging equivalent states at each token
 * boundary. Retained accepting leaves preserve path priority across different match lengths;
 * FIND_NEXT retains only its best acceptance and higher-priority live states, bounding its
 * capture storage by program size. FIND_ALL_SPANS retains distinct accepting endpoints.
 * no recursive backtracking or per-token thread objects are used. The TAG/SPLIT construction
 * follows the existing Synexia tagged-regex algorithm; its one-pass restrictions are not
 * silently applied to this broader token API.
 *
 * <p>Compilation is limited to 32768 instructions. Each matcher and predicate table has a
 * separate 64 MiB primitive-payload budget, and one execution has a 200 million instruction
 * budget. Refusal is explicit, never a partial match or a change of matching semantics.
 * Matchers are mutable and are not safe for concurrent use; patterns and bound predicates are
 * immutable and shareable. Calling matches resets the find traversal.
 */
public final class M3TokenMatcher {
    /** Every distinct span per start, including overlapping spans, in path-priority order. */
    public static final int FIND_ALL_SPANS = 0;
    /** Continue after the previous match; advance one projected token after an empty match. */
    public static final int FIND_NEXT = 1;

    private static final int MAX_PROGRAM = 32768;
    private static final long MAX_BYTES = 64L << 20;
    private static final long MAX_STEPS = 200_000_000L;
    private static final byte TOKEN = 1, ANY = 2, SPLIT = 3, TAG = 4;
    private static final byte BOL = 5, EOL = 6, MARK = 7, PROGRESS = 8, ACCEPT = 9;

    private final M3TokenPattern pattern;
    private final M3TokenIndex index;
    private final M3TokenPredicateCache predicates;
    private final int mode;
    private final boolean significantOnly;
    private final int[] registers;
    private final Machine machine;
    private int nextProjected;
    private int spanStartProjected;
    private int searchFrom;
    private boolean spansReady;
    private boolean matched;

    private M3TokenMatcher(M3TokenPredicateCache predicates, int mode, boolean significantOnly) {
        this.predicates = predicates;
        this.pattern = predicates.pattern();
        this.index = predicates.index();
        this.mode = mode;
        this.significantOnly = significantOnly;
        this.registers = new int[2 * (pattern.groupCount() + 1)];
        Arrays.fill(registers, -1);
        this.machine = new Machine(pattern.program(), predicates, significantOnly, registers.length);
    }

    /** @param mode {@link #FIND_ALL_SPANS} or {@link #FIND_NEXT} */
    public static M3TokenMatcher of(M3TokenPredicateCache predicates, int mode, boolean significantOnly) {
        Objects.requireNonNull(predicates, "predicates");
        if (mode != FIND_ALL_SPANS && mode != FIND_NEXT) {
            throw new IllegalArgumentException("unknown find mode " + mode);
        }
        return new M3TokenMatcher(predicates, mode, significantOnly);
    }

    public int mode() { return mode; }
    public boolean significantOnly() { return significantOnly; }
    public int groupCount() { return pattern.groupCount(); }

    /** Match the complete projected tape using the highest-priority accepting path. */
    public boolean matches() {
        reset();
        if (!machine.execute(0, true, false, false)) return false;
        int start = machine.resultStart();
        int end = machine.resultEnd();
        publish();
        nextProjected = end == start ? end + 1 : end;
        searchFrom = boundary(nextProjected);
        return true;
    }

    /** Find the next match under the selected mode. Exhaustion invalidates previous groups. */
    public boolean find() {
        matched = false;
        Arrays.fill(registers, -1);
        if (mode == FIND_ALL_SPANS) return findSpan();
        if (nextProjected > machine.length
                || !machine.execute(nextProjected, false, true, false)) {
            nextProjected = machine.length + 1;
            searchFrom = boundary(nextProjected);
            return false;
        }
        int start = machine.resultStart();
        int end = machine.resultEnd();
        publish();
        nextProjected = end == start ? end + 1 : end;
        searchFrom = boundary(nextProjected);
        return true;
    }

    private boolean findSpan() {
        while (nextProjected <= machine.length) {
            if (!spansReady) {
                spanStartProjected = nextProjected;
                if (!machine.execute(nextProjected, false, false, true)) {
                    nextProjected++;
                    continue;
                }
                spansReady = true;
            }
            publish();
            if (!machine.hasResult()) {
                spansReady = false;
                nextProjected = spanStartProjected + 1;
            }
            searchFrom = boundary(nextProjected);
            return true;
        }
        searchFrom = boundary(nextProjected);
        return false;
    }

    private void publish() {
        machine.takeResult(registers);
        for (int r = 0; r < registers.length; r += 2) {
            int start = registers[r];
            int end = registers[r + 1];
            if (start >= 0) {
                registers[r] = boundary(start);
                registers[r + 1] = end == start ? registers[r] : token(end - 1) + 1;
            }
        }
        matched = true;
    }

    private int token(int projected) {
        return significantOnly ? index.significantAt(projected) : projected;
    }

    private int boundary(int projected) {
        return projected < machine.length ? token(projected)
                : index.count() + (projected > machine.length ? 1 : 0);
    }

    /** Forget prior groups and start find from the first token again. */
    public void reset() {
        nextProjected = 0;
        searchFrom = 0;
        spansReady = false;
        matched = false;
        Arrays.fill(registers, -1);
        machine.clear();
    }

    /** Lossless token position where the next find starts; tape length + 1 means exhausted. */
    public int searchFrom() { return searchFrom; }
    public int start() { return start(0); }
    public int end() { return end(0); }

    public int start(int group) {
        requireMatch();
        return registers[2 * Objects.checkIndex(group, pattern.groupCount() + 1)];
    }

    public int end(int group) {
        requireMatch();
        return registers[2 * Objects.checkIndex(group, pattern.groupCount() + 1) + 1];
    }

    public int start(String name) { requireMatch(); return start(named(name)); }
    public int end(String name) { requireMatch(); return end(named(name)); }

    public long sourceStart(int group) {
        int start = start(group);
        return start < 0 ? -1 : index.spanStart(start, end(group));
    }

    public long sourceEnd(int group) {
        int start = start(group);
        return start < 0 ? -1 : index.spanEnd(start, end(group));
    }

    private int named(String name) {
        int group = pattern.groupIndex(name);
        if (group < 0) throw new IllegalArgumentException("no group named " + name);
        return group;
    }

    private void requireMatch() {
        if (!matched) throw new IllegalStateException("no match available; call matches() or find() first");
    }

    public M3TokenPredicateCache predicates() { return predicates; }

    /** Immutable execution metadata owned by the existing compiled token pattern. */
    static final class Program {
        final byte[] op;
        final int[] next, other, argument;
        final int start, progressStates;

        Program(Compiler compiler, int start) {
            this.op = Arrays.copyOf(compiler.op, compiler.count);
            this.next = Arrays.copyOf(compiler.next, compiler.count);
            this.other = Arrays.copyOf(compiler.other, compiler.count);
            this.argument = Arrays.copyOf(compiler.argument, compiler.count);
            this.start = start;
            this.progressStates = compiler.repeatDepth + 1;
        }
    }

    static Program compile(M3TokenPattern pattern) {
        Compiler compiler = new Compiler(pattern);
        int accept = compiler.emit(ACCEPT, -1, -1, 0);
        return new Program(compiler, compiler.node(pattern.rootNode(), accept, 0));
    }

    /** Backward TAG/SPLIT construction, including explicit nullable-repeat progress guards. */
    private static final class Compiler {
        private final M3TokenPattern pattern;
        private byte[] op = new byte[32];
        private int[] next = new int[32], other = new int[32], argument = new int[32];
        private int count, repeatDepth;

        Compiler(M3TokenPattern pattern) { this.pattern = pattern; }

        int emit(byte opcode, int successor, int alternative, int value) {
            if (count == MAX_PROGRAM) {
                throw new PatternSyntaxException("token program exceeds " + MAX_PROGRAM
                        + " instructions", pattern.source(), -1);
            }
            if (count == op.length) {
                int size = Math.min(MAX_PROGRAM, count * 2);
                op = Arrays.copyOf(op, size);
                next = Arrays.copyOf(next, size);
                other = Arrays.copyOf(other, size);
                argument = Arrays.copyOf(argument, size);
            }
            op[count] = opcode;
            next[count] = successor;
            other[count] = alternative;
            argument[count] = value;
            return count++;
        }

        int node(int n, int successor, int depth) {
            return switch (pattern.nodeOp(n)) {
                case M3TokenPattern.N_TOKEN -> emit(TOKEN, successor, -1, pattern.nodeX(n));
                case M3TokenPattern.N_ANY -> emit(ANY, successor, -1, 0);
                case M3TokenPattern.N_BOL -> emit(BOL, successor, -1, 0);
                case M3TokenPattern.N_EOL -> emit(EOL, successor, -1, 0);
                case M3TokenPattern.N_SEQ -> sequence(n, successor, depth);
                case M3TokenPattern.N_ALT -> emit(SPLIT,
                        node(pattern.nodeX(n), successor, depth),
                        node(pattern.nodeY(n), successor, depth), 0);
                case M3TokenPattern.N_GROUP -> {
                    int register = 2 * pattern.nodeY(n);
                    int end = emit(TAG, successor, -1, register + 1);
                    yield emit(TAG, node(pattern.nodeX(n), end, depth), -1, register);
                }
                default -> repetition(n, successor, depth);
            };
        }

        private int sequence(int n, int successor, int depth) {
            int[] children = new int[pattern.nodeY(n)];
            int at = 0;
            for (int child = pattern.nodeX(n); child >= 0; child = pattern.nodeNext(child)) {
                children[at++] = child;
            }
            int start = successor;
            for (int i = children.length - 1; i >= 0; i--) start = node(children[i], start, depth);
            return start;
        }

        private int repetition(int n, int successor, int depth) {
            repeatDepth = Math.max(repeatDepth, depth + 1);
            int minimum = pattern.nodeY(n), maximum = pattern.nodeZ(n);
            int child = pattern.nodeX(n), start = successor;
            boolean reluctant = pattern.nodeReluctant(n);
            if (maximum < 0) {
                int loop = emit(SPLIT, -1, -1, 0);
                int guard = emit(PROGRESS, loop, successor, depth);
                int body = emit(MARK, node(child, guard, depth + 1), -1, depth);
                next[loop] = reluctant ? successor : body;
                other[loop] = reluctant ? body : successor;
                start = loop;
            } else {
                for (int i = maximum - 1; i >= minimum; i--) {
                    int guard = emit(PROGRESS, start, successor, depth);
                    int body = emit(MARK, node(child, guard, depth + 1), -1, depth);
                    start = emit(SPLIT, reluctant ? successor : body,
                            reluctant ? body : successor, 0);
                }
            }
            for (int i = minimum - 1; i >= 0; i--) {
                int guard = emit(PROGRESS, start, i + 1 == minimum ? successor : -1, depth);
                start = emit(MARK, node(child, guard, depth + 1), -1, depth);
            }
            return start;
        }
    }

    /** Ordered leaf list: live states are replaced in place; accepted endpoints retain order. */
    private static final class Machine {
        private final Program program;
        private final M3TokenPredicateCache predicates;
        private final M3TokenIndex index;
        private final boolean significantOnly;
        private final int registerCount, length;
        private final int[] seen;
        private final int[] scratch, initial;
        private int[] links = new int[32], previous = new int[32], pc = new int[32];
        private int[] captures;
        private int[] current = new int[32], following = new int[32];
        private int[] stackPc = new int[32], stackProgress = new int[32], stackCaptures;
        private int used = 1, free, currentCount, followingCount, stackSize, epoch, acceptCount;
        private long steps;
        private boolean whole;

        Machine(Program program, M3TokenPredicateCache predicates, boolean significantOnly,
                int registerCount) {
            this.program = program;
            this.predicates = predicates;
            this.index = predicates.index();
            this.significantOnly = significantOnly;
            this.length = significantOnly ? index.significantCount() : index.count();
            this.registerCount = registerCount;
            long cells = (long) program.op.length * program.progressStates;
            if (cells * Integer.BYTES > MAX_BYTES / 2) throw budget();
            this.seen = new int[(int) cells];
            this.scratch = new int[registerCount];
            this.initial = new int[registerCount];
            this.captures = new int[32 * registerCount];
            this.stackCaptures = new int[32 * registerCount];
        }

        void clear() {
            used = 1;
            free = 0;
            links[0] = previous[0] = 0;
            currentCount = followingCount = stackSize = acceptCount = 0;
        }

        boolean execute(int start, boolean whole, boolean search, boolean allSpans) {
            clear();
            this.whole = whole;
            this.steps = 0;
            int position = start;
            newEpoch();
            seed(position);
            if (!allSpans) pruneAfterAcceptance();
            swap();
            while (true) {
                if (!whole && !allSpans && hasResult()) return true;
                if (position == length) {
                    for (int i = 0; i < currentCount; i++) discard(current[i]);
                    currentCount = 0;
                    return hasResult();
                }
                if (currentCount == 0 && !search) return hasResult();
                int token = significantOnly ? index.significantAt(position) : position;
                newEpoch();
                followingCount = 0;
                for (int i = 0; i < currentCount; i++) {
                    int leaf = current[i], instruction = pc[leaf];
                    tick();
                    if (program.op[instruction] == ANY
                            || predicates.test(program.argument[instruction], token)) {
                        expand(program.next[instruction], position + 1, -1,
                                captures, leaf * registerCount, leaf);
                    }
                    discard(leaf);
                }
                position++;
                // Earlier starts dominate every later start at an equivalent program state.
                // Once any accepting leaf exists, adding still later starts cannot improve it.
                if (search && !containsAccept()) seed(position);
                if (!allSpans) pruneAfterAcceptance();
                swap();
            }
        }

        private void seed(int position) {
            Arrays.fill(initial, -1);
            initial[0] = position;
            expand(program.start, position, -1, initial, 0, 0);
        }

        private void swap() {
            int[] swap = current;
            current = following;
            following = swap;
            currentCount = followingCount;
            followingCount = 0;
        }

        private void newEpoch() {
            if (++epoch == 0) {
                Arrays.fill(seen, 0);
                epoch = 1;
            }
        }

        private void expand(int instruction, int position, int progress, int[] from,
                int offset, int anchor) {
            System.arraycopy(from, offset, scratch, 0, registerCount);
            stackSize = 0;
            while (true) {
                while (instruction >= 0) {
                    tick();
                    byte op = program.op[instruction];
                    // Consuming a token makes every enclosing repetition progress. Its prior
                    // progress state can therefore be merged at a consuming instruction.
                    int lane = op == TOKEN || op == ANY ? 0 : progress + 1;
                    int key = instruction * program.progressStates + lane;
                    if (seen[key] == epoch) break;
                    seen[key] = epoch;
                    int value = program.argument[instruction];
                    if (op == TOKEN || op == ANY) {
                        int leaf = appendBefore(anchor, instruction);
                        if (followingCount == following.length) growThreads();
                        following[followingCount++] = leaf;
                        break;
                    }
                    if (op == ACCEPT) {
                        if (!whole || position == length) {
                            scratch[1] = position;
                            appendBefore(anchor, -1);
                        }
                        break;
                    }
                    if (op == SPLIT) {
                        push(program.other[instruction], progress);
                    } else if (op == TAG) {
                        scratch[value] = position;
                    } else if (op == BOL) {
                        if (position != 0) break;
                    } else if (op == EOL) {
                        if (position != length) break;
                    } else if (op == MARK) {
                        if (progress < 0) progress = value;
                    } else if (op == PROGRESS) {
                        boolean empty = progress >= 0 && progress <= value;
                        if (progress == value) progress = -1;
                        instruction = empty ? program.other[instruction] : program.next[instruction];
                        continue;
                    }
                    instruction = program.next[instruction];
                }
                if (stackSize == 0) return;
                int row = --stackSize;
                instruction = stackPc[row];
                progress = stackProgress[row];
                System.arraycopy(stackCaptures, row * registerCount, scratch, 0, registerCount);
            }
        }

        private int appendBefore(int anchor, int instruction) {
            int leaf;
            if (free != 0) {
                leaf = free;
                free = links[leaf];
            } else {
                if (used == links.length) growLeaves();
                leaf = used++;
            }
            int left = previous[anchor];
            links[left] = leaf;
            previous[leaf] = left;
            links[leaf] = anchor;
            previous[anchor] = leaf;
            pc[leaf] = instruction;
            if (instruction < 0) acceptCount++;
            System.arraycopy(scratch, 0, captures, leaf * registerCount, registerCount);
            return leaf;
        }

        private void discard(int leaf) {
            if (pc[leaf] < 0) acceptCount--;
            links[previous[leaf]] = links[leaf];
            previous[links[leaf]] = previous[leaf];
            links[leaf] = free;
            free = leaf;
            pc[leaf] = -2;
        }

        boolean hasResult() { return links[0] != 0 && pc[links[0]] < 0; }

        private boolean containsAccept() { return acceptCount != 0; }

        private void pruneAfterAcceptance() {
            if (acceptCount == 0) return;
            int accepted = links[0];
            while (pc[accepted] >= 0) accepted = links[accepted];
            int leaf = links[accepted];
            while (leaf != 0) {
                int nextLeaf = links[leaf];
                discard(leaf);
                leaf = nextLeaf;
            }
            int keep = 0;
            for (int i = 0; i < followingCount; i++) {
                if (pc[following[i]] >= 0) following[keep++] = following[i];
            }
            followingCount = keep;
        }

        int resultStart() { return captures[links[0] * registerCount]; }
        int resultEnd() { return captures[links[0] * registerCount + 1]; }

        void takeResult(int[] target) {
            if (!hasResult()) throw new IllegalStateException("no accepting token state");
            int leaf = links[0];
            System.arraycopy(captures, leaf * registerCount, target, 0, registerCount);
            discard(leaf);
        }

        private void push(int instruction, int progress) {
            if (stackSize == stackPc.length) growStack();
            stackPc[stackSize] = instruction;
            stackProgress[stackSize] = progress;
            System.arraycopy(scratch, 0, stackCaptures, stackSize * registerCount, registerCount);
            stackSize++;
        }

        private void growLeaves() {
            int size = Math.multiplyExact(links.length, 2);
            reserve(size, Math.max(current.length, following.length), stackPc.length);
            links = Arrays.copyOf(links, size);
            previous = Arrays.copyOf(previous, size);
            pc = Arrays.copyOf(pc, size);
            captures = Arrays.copyOf(captures, Math.multiplyExact(size, registerCount));
        }

        private void growThreads() {
            int size = Math.multiplyExact(following.length, 2);
            reserve(links.length, Math.max(current.length, size), stackPc.length);
            following = Arrays.copyOf(following, size);
        }

        private void growStack() {
            int size = Math.multiplyExact(stackPc.length, 2);
            reserve(links.length, Math.max(current.length, following.length), size);
            stackPc = Arrays.copyOf(stackPc, size);
            stackProgress = Arrays.copyOf(stackProgress, size);
            stackCaptures = Arrays.copyOf(stackCaptures, Math.multiplyExact(size, registerCount));
        }

        private void reserve(int leaves, int threads, int stack) {
            long cells = seen.length + 2L * registerCount + 2L * threads
                    + (3L + registerCount) * leaves + (2L + registerCount) * stack;
            if (cells * Integer.BYTES > MAX_BYTES) throw budget();
        }

        private void tick() {
            if (++steps > MAX_STEPS) throw new IllegalStateException("token matcher instruction budget exhausted");
            if ((steps & 4095) == 0 && Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException("token matching interrupted");
            }
        }

        private static IllegalStateException budget() {
            return new IllegalStateException("token matcher primitive payload exceeds 64 MiB");
        }
    }
}
