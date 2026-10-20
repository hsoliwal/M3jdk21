/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.mindex;

import java.util.Arrays;
import java.util.Objects;

/**
 * Primitive execution engine for {@link M3TokenPattern}.
 *
 * <p>The parsed pattern stays the immutable authority. Execution is forward-only over token
 * coordinates. At every node, paths reaching the same token endpoint are folded immediately while
 * preserving the first (highest-priority) capture state. That bounds duplicate path growth by
 * pattern states and token positions instead of retaining an exponential path tree. Greedy
 * repetitions publish deeper repetition levels first; reluctant repetitions publish shallower
 * levels first. A zero-width repetition result is published once and is never re-expanded.</p>
 *
 * <p>No token object is materialized. A significant-token view is one primitive lane of lossless
 * token coordinates; match/capture results are projected back to those original coordinates before
 * they leave this engine.</p>
 */
final class M3TokenEngine {
    private static final long MIN_WORK = 100_000L;
    private static final long MAX_WORK = 16_000_000L;
    private static final int REGISTER_LIMIT = 2 * (M3TokenPattern.MAX_GROUPS + 1);

    record Result(int[] registers) {
        Result {
            registers = Objects.requireNonNull(registers, "registers").clone();
        }

        @Override
        public int[] registers() {
            return registers.clone();
        }
    }

    private final M3TokenPredicateCache predicates;
    private final M3TokenPattern pattern;
    private final M3TokenIndex index;
    private final boolean significantOnly;
    private final int[] visible;
    private final int visibleCount;
    private final int stride;
    private final int[] scratch;

    private StateSet pending;
    private int pendingIndex;
    private int pendingStart;
    private int allSpanStart;
    private int nextFindStart;
    private int searchFrom;
    private long work;
    private long workLimit;

    M3TokenEngine(M3TokenPredicateCache predicates, boolean significantOnly) {
        this.predicates = Objects.requireNonNull(predicates, "predicates");
        this.pattern = predicates.pattern();
        this.index = predicates.index();
        this.significantOnly = significantOnly;
        this.stride = 2 * (pattern.groupCount() + 1);
        if (stride < 2 || stride > REGISTER_LIMIT) {
            throw new IllegalArgumentException("token regex capture register budget");
        }
        if (significantOnly) {
            this.visibleCount = index.significantCount();
            this.visible = new int[visibleCount];
            for (int token = 0; token < visibleCount; token++) {
                visible[token] = index.significantAt(token);
            }
        } else {
            this.visibleCount = index.count();
            this.visible = null;
        }
        this.scratch = new int[stride];
        reset();
    }

    void reset() {
        pending = null;
        pendingIndex = 0;
        pendingStart = 0;
        allSpanStart = 0;
        nextFindStart = 0;
        searchFrom = 0;
    }

    int searchFrom() {
        return searchFrom;
    }

    Result matches() {
        StateSet accepted = evaluateRoot(0);
        for (int state = 0; state < accepted.count; state++) {
            if (accepted.positions[state] == visibleCount) {
                return result(0, visibleCount, accepted, state);
            }
        }
        return null;
    }

    Result find(int mode) {
        return mode == M3TokenMatcher.FIND_ALL_SPANS ? findAllSpans() : findNext();
    }

    private Result findAllSpans() {
        for (;;) {
            if (pending != null && pendingIndex < pending.count) {
                int state = pendingIndex++;
                searchFrom = rawBoundary(pendingStart);
                return result(pendingStart, pending.positions[state], pending, state);
            }
            pending = null;
            pendingIndex = 0;
            if (allSpanStart > visibleCount) {
                searchFrom = index.count();
                return null;
            }
            int start = allSpanStart++;
            StateSet matches = evaluateRoot(start);
            if (matches.count != 0) {
                pending = matches;
                pendingStart = start;
            }
        }
    }

    private Result findNext() {
        while (nextFindStart <= visibleCount) {
            int start = nextFindStart;
            StateSet matches = evaluateRoot(start);
            if (matches.count == 0) {
                nextFindStart = start + 1;
                continue;
            }
            int end = matches.positions[0];
            if (end > start) {
                nextFindStart = end;
            } else if (start < visibleCount) {
                nextFindStart = start + 1;
            } else {
                nextFindStart = visibleCount + 1;
            }
            searchFrom = nextFindStart <= visibleCount
                    ? rawBoundary(nextFindStart) : index.count();
            return result(start, end, matches, 0);
        }
        searchFrom = index.count();
        return null;
    }

    private StateSet evaluateRoot(int start) {
        Arrays.fill(scratch, -1);
        work = 0L;
        long cells = Math.multiplyExact(
                (long) pattern.nodeCount() + 1L,
                (long) visibleCount + 1L);
        workLimit = Math.max(MIN_WORK, Math.min(MAX_WORK, Math.multiplyExact(cells, 64L)));
        return eval(pattern.rootNode(), start, scratch);
    }

    private StateSet eval(int node, int position, int[] registers) {
        charge();
        return switch (pattern.nodeOp(node)) {
            case M3TokenPattern.N_TOKEN -> token(node, position, registers);
            case M3TokenPattern.N_ANY -> any(position, registers);
            case M3TokenPattern.N_BOL -> assertion(position == 0, position, registers);
            case M3TokenPattern.N_EOL ->
                    assertion(position == visibleCount, position, registers);
            case M3TokenPattern.N_SEQ -> sequence(node, position, registers);
            case M3TokenPattern.N_ALT -> alternative(node, position, registers);
            case M3TokenPattern.N_REPEAT -> repeat(node, position, registers);
            case M3TokenPattern.N_GROUP -> group(node, position, registers);
            default -> throw new IllegalStateException(
                    "unknown token regex node opcode " + pattern.nodeOp(node));
        };
    }

    private StateSet token(int node, int position, int[] registers) {
        StateSet out = states();
        if (position < visibleCount
                && predicates.test(pattern.nodeX(node), rawToken(position))) {
            out.add(position + 1, registers);
        }
        return out;
    }

    private StateSet any(int position, int[] registers) {
        StateSet out = states();
        if (position < visibleCount) out.add(position + 1, registers);
        return out;
    }

    private StateSet assertion(boolean accepted, int position, int[] registers) {
        StateSet out = states();
        if (accepted) out.add(position, registers);
        return out;
    }

    private StateSet alternative(int node, int position, int[] registers) {
        StateSet out = states();
        out.addAll(eval(pattern.nodeX(node), position, registers));
        out.addAll(eval(pattern.nodeY(node), position, registers));
        return out;
    }

    private StateSet sequence(int node, int position, int[] registers) {
        StateSet current = states();
        current.add(position, registers);
        for (int child = pattern.nodeX(node); child >= 0; child = pattern.nodeNext(child)) {
            current = map(child, current);
            if (current.count == 0) break;
        }
        return current;
    }

    private StateSet group(int node, int position, int[] registers) {
        int group = pattern.nodeY(node);
        int startSlot = 2 * group;
        int endSlot = startSlot + 1;

        StateSet initial = states();
        initial.addModified(position, registers, startSlot, position);
        StateSet child = map(pattern.nodeX(node), initial);
        StateSet out = states();
        for (int state = 0; state < child.count; state++) {
            child.copyRegisters(state, scratch);
            scratch[endSlot] = child.positions[state];
            out.add(child.positions[state], scratch);
        }
        return out;
    }

    private StateSet repeat(int node, int position, int[] registers) {
        int min = pattern.nodeY(node);
        int declaredMax = pattern.nodeZ(node);
        int maximum = declaredMax < 0
                ? Math.addExact(visibleCount - position, 1)
                : Math.min(declaredMax, Math.addExact(visibleCount - position, 1));
        if (maximum < min) return states();

        StateSet[] levels = new StateSet[maximum + 1];
        levels[0] = states();
        levels[0].add(position, registers);
        StateSet frontier = levels[0];
        int produced = 0;

        for (int iteration = 1; iteration <= maximum; iteration++) {
            StateSet next = states();
            StateSet progressing = states();
            for (int state = 0; state < frontier.count; state++) {
                int before = frontier.positions[state];
                frontier.copyRegisters(state, scratch);
                StateSet one = eval(pattern.nodeX(node), before, scratch);
                for (int candidate = 0; candidate < one.count; candidate++) {
                    one.copyRegisters(candidate, scratch);
                    int after = one.positions[candidate];
                    next.add(after, scratch);
                    if (after != before) progressing.add(after, scratch);
                }
            }
            if (next.count == 0) break;
            levels[iteration] = next;
            produced = iteration;
            if (progressing.count == 0) break;
            frontier = progressing;
        }

        if (produced < min) return states();
        StateSet out = states();
        if (pattern.nodeReluctant(node)) {
            for (int iteration = min; iteration <= produced; iteration++) {
                if (levels[iteration] != null) out.addAll(levels[iteration]);
            }
        } else {
            for (int iteration = produced; iteration >= min; iteration--) {
                if (levels[iteration] != null) out.addAll(levels[iteration]);
            }
        }
        return out;
    }

    private StateSet map(int node, StateSet input) {
        StateSet out = states();
        for (int state = 0; state < input.count; state++) {
            input.copyRegisters(state, scratch);
            out.addAll(eval(node, input.positions[state], scratch));
        }
        return out;
    }

    private Result result(int start, int end, StateSet states, int state) {
        states.copyRegisters(state, scratch);
        scratch[0] = start;
        scratch[1] = end;
        int[] raw = Arrays.copyOf(scratch, stride);
        for (int group = 0; group <= pattern.groupCount(); group++) {
            int at = 2 * group;
            int from = raw[at];
            int to = raw[at + 1];
            if (from < 0 || to < 0) {
                raw[at] = -1;
                raw[at + 1] = -1;
                continue;
            }
            if (from > to || to > visibleCount) {
                throw new IllegalStateException("invalid token capture geometry");
            }
            if (from == to) {
                int boundary = rawBoundary(from);
                raw[at] = boundary;
                raw[at + 1] = boundary;
            } else {
                raw[at] = rawToken(from);
                raw[at + 1] = Math.addExact(rawToken(to - 1), 1);
            }
        }
        return new Result(raw);
    }

    private int rawToken(int projected) {
        Objects.checkIndex(projected, visibleCount);
        return significantOnly ? visible[projected] : projected;
    }

    private int rawBoundary(int projected) {
        Objects.checkFromToIndex(projected, projected, visibleCount);
        if (!significantOnly) return projected;
        return projected < visibleCount ? visible[projected] : index.count();
    }

    private StateSet states() {
        return new StateSet(visibleCount + 1, stride);
    }

    private void charge() {
        if (++work > workLimit) {
            throw new IllegalStateException(
                    "token regex work budget exceeded: " + workLimit);
        }
    }

    /** Endpoint-deduplicated primitive state lane; first arrival owns capture priority. */
    private static final class StateSet {
        private final int endpointCount;
        private final int stride;
        private final boolean[] seen;
        private int[] positions = new int[8];
        private int[] registers;
        private int[] copy;
        private int count;

        StateSet(int endpointCount, int stride) {
            this.endpointCount = endpointCount;
            this.stride = stride;
            this.seen = new boolean[endpointCount];
            this.registers = new int[Math.multiplyExact(8, stride)];
            this.copy = new int[stride];
        }

        void add(int position, int[] source) {
            Objects.checkIndex(position, endpointCount);
            if (seen[position]) return;
            ensure(count + 1);
            seen[position] = true;
            positions[count] = position;
            System.arraycopy(source, 0, registers, count * stride, stride);
            count++;
        }

        void addModified(int position, int[] source, int slot, int value) {
            Objects.checkIndex(slot, stride);
            System.arraycopy(source, 0, copy, 0, stride);
            copy[slot] = value;
            add(position, copy);
        }

        void addAll(StateSet other) {
            for (int state = 0; state < other.count; state++) {
                other.copyRegisters(state, copy);
                add(other.positions[state], copy);
            }
        }

        void copyRegisters(int state, int[] destination) {
            Objects.checkIndex(state, count);
            if (destination.length < stride) {
                throw new IllegalArgumentException("capture destination too small");
            }
            System.arraycopy(registers, state * stride, destination, 0, stride);
        }

        private void ensure(int required) {
            if (required <= positions.length) return;
            int grown = Math.max(required, positions.length << 1);
            positions = Arrays.copyOf(positions, grown);
            registers = Arrays.copyOf(registers, Math.multiplyExact(grown, stride));
        }
    }
}
