// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.precompute;

import java.util.Arrays;
import java.util.Objects;

/**
 * An isolated, array-backed token-sequence regex candidate, not java.util.regex.Pattern.
 *
 * <p>Units are lexer/AST tokens, never UTF-16 characters. The canonical source owner
 * is strongly referenced by the immutable tape; token kind, canonical atom identifier,
 * behavioral masks and source UTF-16 spans are parallel int arrays. No token, AST node
 * or Map.Entry instance is retained per occurrence. The source text is never copied.
 *
 * <p>Syntax: [], ., [kind=2], [atom=7], [all=0x4,none=0x8], concatenation,
 * grouping, alternation, ?, *, +, ^, and $. A predicate can combine kind,
 * atom, all and none. This deliberately excludes arbitrary character regex,
 * captures, lookarounds and backreferences. Those stay with the existing JDK
 * Pattern/Matcher owner; this class does not claim Java regex compatibility.
 *
 * <p>Matching uses a bounded Thompson epsilon NFA and primitive per-operation
 * work arrays, with no backtracking recursion or per-token objects. find()
 * selects the leftmost-longest token interval (not Java Matcher precedence).
 * Compiled plans are immutable and safe to share between threads.
 */
public final class M3TokenRegex {
    private static final int TOKEN = 1;
    private static final int SPLIT = 2;
    private static final int JUMP = 3;
    private static final int ACCEPT = 4;
    private static final int BEGIN = 5;
    private static final int END = 6;
    private static final int MAX_PATTERN_UTF16 = 4096;
    private static final int MAX_STATES = 8192;
    private static final int MAX_GROUP_DEPTH = 64;

    /** Half-open token and source UTF-16 offsets; no retained token objects. */
    public record Span(int tokenStart, int tokenEnd, int utf16Start, int utf16End) {
        public Span {
            if (tokenStart < 0 || tokenEnd < tokenStart || utf16Start < 0
                    || utf16End < utf16Start) throw new IllegalArgumentException("invalid token span");
        }
    }

    /**
     * Snapshot of lexer/AST metadata; owner is the canonical text/AST lifetime anchor.
     * Clients must tokenize comments and string literals as distinct kinds (rather
     * than silently dropping them) when matches must not cross those boundaries.
     */
    public static final class Tape {
        private final Object owner;
        private final int sourceLength;
        private final int[] kinds;
        private final int[] atoms;
        private final int[] masks;
        private final int[] starts;
        private final int[] ends;

        public Tape(Object owner, int sourceLength, int[] kinds, int[] atoms,
                    int[] masks, int[] starts, int[] ends) {
            this.owner = Objects.requireNonNull(owner, "owner");
            if (sourceLength < 0) throw new IllegalArgumentException("sourceLength");
            this.kinds = Objects.requireNonNull(kinds, "kinds").clone();
            this.atoms = Objects.requireNonNull(atoms, "atoms").clone();
            this.masks = Objects.requireNonNull(masks, "masks").clone();
            this.starts = Objects.requireNonNull(starts, "starts").clone();
            this.ends = Objects.requireNonNull(ends, "ends").clone();
            this.sourceLength = sourceLength;
            if (atoms.length != kinds.length || masks.length != kinds.length
                    || starts.length != kinds.length || ends.length != kinds.length) {
                throw new IllegalArgumentException("unaligned token lanes");
            }
            int previousEnd = 0;
            for (int i = 0; i < kinds.length; i++) {
                if (this.kinds[i] < 0 || this.atoms[i] < 0
                        || this.starts[i] < previousEnd || this.ends[i] < this.starts[i]
                        || this.ends[i] > sourceLength) {
                    throw new IllegalArgumentException("invalid token metadata at " + i);
                }
                previousEnd = this.ends[i];
            }
        }

        public static Tape ofKinds(Object owner, int sourceLength, int[] kinds,
                                   int[] starts, int[] ends) {
            Objects.requireNonNull(kinds, "kinds");
            return new Tape(owner, sourceLength, kinds, new int[kinds.length],
                    new int[kinds.length], starts, ends);
        }

        public Object owner() { return owner; }
        public int sourceLengthUtf16() { return sourceLength; }
        public int size() { return kinds.length; }
        public int kind(int index) { return kinds[Objects.checkIndex(index, size())]; }
        public int atom(int index) { return atoms[Objects.checkIndex(index, size())]; }
        public int stateMask(int index) { return masks[Objects.checkIndex(index, size())]; }
        public int utf16Start(int index) { return starts[Objects.checkIndex(index, size())]; }
        public int utf16End(int index) { return ends[Objects.checkIndex(index, size())]; }
    }

    private final String expression;
    private final int[] op;
    private final int[] next;
    private final int[] other;
    private final int[] kind;
    private final int[] atom;
    private final int[] all;
    private final int[] none;
    private final int first;
    private final int accept;

    private M3TokenRegex(String expression, Builder builder, int first, int accept) {
        this.expression = expression;
        this.op = Arrays.copyOf(builder.op, builder.count);
        this.next = Arrays.copyOf(builder.next, builder.count);
        this.other = Arrays.copyOf(builder.other, builder.count);
        this.kind = Arrays.copyOf(builder.kind, builder.count);
        this.atom = Arrays.copyOf(builder.atom, builder.count);
        this.all = Arrays.copyOf(builder.all, builder.count);
        this.none = Arrays.copyOf(builder.none, builder.count);
        this.first = first;
        this.accept = accept;
    }

    public static M3TokenRegex compile(String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.length() > MAX_PATTERN_UTF16) {
            throw new IllegalArgumentException("token regex pattern budget");
        }
        Parser parser = new Parser(pattern);
        Frag fragment = parser.alternative();
        parser.space();
        if (parser.index != pattern.length()) {
            throw parser.invalid("unexpected token");
        }
        int accept = parser.builder.emit(ACCEPT, -1, -1, -1, -1, 0, 0);
        parser.builder.patch(fragment.head, accept);
        return new M3TokenRegex(pattern, parser.builder, fragment.start, accept);
    }

    public String expression() { return expression; }
    public int stateCount() { return op.length; }

    /** Full-token-sequence match; it never interprets source characters directly. */
    public boolean matches(Tape tape) {
        Objects.requireNonNull(tape, "tape");
        Span candidate = find(tape);
        return candidate != null && candidate.tokenStart == 0 && candidate.tokenEnd == tape.size();
    }

    /** Leftmost-longest match; null when absent. Returns offsets into the source owner. */
    public Span find(Tape tape) { return find(tape, 0); }

    /**
     * Starts a new search at an explicit token ordinal, including size() for
     * empty end-of-input matches. Callers advance past a zero-width result.
     */
    public Span find(Tape tape, int fromToken) {
        Objects.requireNonNull(tape, "tape");
        Objects.checkFromToIndex(fromToken, fromToken, tape.size());
        StateSet current = new StateSet(op.length);
        StateSet following = new StateSet(op.length);
        current.begin();
        int bestStart = Integer.MAX_VALUE;
        int bestEnd = -1;
        for (int position = fromToken; position <= tape.size(); position++) {
            current.seed(first, position);
            current.expand(this, position, tape.size());
            for (int i = 0; i < current.readyCount; i++) {
                int state = current.ready[i];
                if (state == accept) {
                    int start = current.bestOrigin[state];
                    if (start < bestStart || start == bestStart && position > bestEnd) {
                        bestStart = start;
                        bestEnd = position;
                    }
                }
            }
            boolean canExtend = false;
            if (bestEnd >= 0) {
                for (int i = 0; i < current.readyCount; i++) {
                    int state = current.ready[i];
                    if (op[state] == TOKEN && current.bestOrigin[state] <= bestStart) {
                        canExtend = true;
                        break;
                    }
                }
                if (!canExtend) return span(tape, bestStart, bestEnd);
            }
            if (position == tape.size()) break;
            following.begin();
            for (int i = 0; i < current.readyCount; i++) {
                int state = current.ready[i];
                if (op[state] == TOKEN && accepts(state, tape, position)) {
                    following.seed(next[state], current.bestOrigin[state]);
                }
            }
            following.expand(this, position + 1, tape.size());
            StateSet swap = current;
            current = following;
            following = swap;
        }
        return bestEnd < 0 ? null : span(tape, bestStart, bestEnd);
    }

    private Span span(Tape tape, int from, int to) {
        int start = from == tape.size() ? tape.sourceLength : tape.starts[from];
        int end = to == from ? start : tape.ends[to - 1];
        return new Span(from, to, start, end);
    }

    private boolean accepts(int state, Tape tape, int position) {
        return (kind[state] < 0 || kind[state] == tape.kinds[position])
                && (atom[state] < 0 || atom[state] == tape.atoms[position])
                && (tape.masks[position] & all[state]) == all[state]
                && (tape.masks[position] & none[state]) == 0;
    }

    /** Per-call primitive states and epsilon closure queue, never retained by a plan. */
    private static final class StateSet {
        final int[] seen;
        final int[] queued;
        final int[] reported;
        final int[] bestOrigin;
        final int[] queue;
        final int[] ready;
        int epoch;
        int head;
        int tail;
        int readyCount;

        StateSet(int states) {
            seen = new int[states];
            queued = new int[states];
            reported = new int[states];
            bestOrigin = new int[states];
            queue = new int[states + 1];
            ready = new int[states];
        }

        void begin() {
            if (++epoch <= 0) {
                Arrays.fill(seen, 0);
                Arrays.fill(queued, 0);
                Arrays.fill(reported, 0);
                epoch = 1;
            }
            head = 0;
            tail = 0;
            readyCount = 0;
        }

        void seed(int state, int origin) {
            if (state < 0) throw new IllegalStateException("unpatched NFA edge");
            if (seen[state] == epoch && bestOrigin[state] <= origin) return;
            seen[state] = epoch;
            bestOrigin[state] = origin;
            if (queued[state] != epoch) {
                queued[state] = epoch;
                queue[tail] = state;
                tail = (tail + 1) % queue.length;
                if (tail == head) throw new IllegalStateException("epsilon queue overflow");
            }
        }

        void expand(M3TokenRegex plan, int position, int total) {
            while (head != tail) {
                int state = queue[head];
                head = (head + 1) % queue.length;
                queued[state] = 0;
                int origin = bestOrigin[state];
                switch (plan.op[state]) {
                    case SPLIT -> {
                        seed(plan.next[state], origin);
                        seed(plan.other[state], origin);
                    }
                    case JUMP -> seed(plan.next[state], origin);
                    case BEGIN -> {
                        if (position == 0) seed(plan.next[state], origin);
                    }
                    case END -> {
                        if (position == total) seed(plan.next[state], origin);
                    }
                    case TOKEN, ACCEPT -> {
                        if (reported[state] != epoch) {
                            reported[state] = epoch;
                            ready[readyCount++] = state;
                        }
                    }
                    default -> throw new IllegalStateException("unknown NFA opcode");
                }
            }
        }
    }

    /** Transient compiler fragments; runtime retains only parallel primitive arrays. */
    private record Frag(int start, int head, int tail) { }

    private static final class Builder {
        int count;
        int[] op = new int[32];
        int[] next = new int[32];
        int[] other = new int[32];
        int[] kind = new int[32];
        int[] atom = new int[32];
        int[] all = new int[32];
        int[] none = new int[32];
        int[] patchNext = new int[64];

        int emit(int opcode, int first, int second, int k, int a, int required, int forbidden) {
            if (count == MAX_STATES) throw new IllegalArgumentException("NFA state budget");
            if (count == op.length) grow();
            int state = count++;
            op[state] = opcode;
            next[state] = first;
            other[state] = second;
            kind[state] = k;
            atom[state] = a;
            all[state] = required;
            none[state] = forbidden;
            return state;
        }

        void grow() {
            int newSize = Math.min(MAX_STATES, op.length * 2);
            op = Arrays.copyOf(op, newSize);
            next = Arrays.copyOf(next, newSize);
            other = Arrays.copyOf(other, newSize);
            kind = Arrays.copyOf(kind, newSize);
            atom = Arrays.copyOf(atom, newSize);
            all = Arrays.copyOf(all, newSize);
            none = Arrays.copyOf(none, newSize);
            patchNext = Arrays.copyOf(patchNext, newSize * 2);
        }

        Frag edge(int state, int branch) {
            int pointer = state * 2 + branch + 1;
            return new Frag(state, pointer, pointer);
        }

        void patch(int pointer, int target) {
            while (pointer != 0) {
                int edge = pointer - 1;
                int following = patchNext[edge];
                int state = edge >>> 1;
                if ((edge & 1) == 0) {
                    if (next[state] != -1) throw new IllegalStateException("double patch");
                    next[state] = target;
                } else {
                    if (other[state] != -1) throw new IllegalStateException("double patch");
                    other[state] = target;
                }
                pointer = following;
            }
        }

        Frag concat(Frag a, Frag b) {
            patch(a.head, b.start);
            return new Frag(a.start, b.head, b.tail);
        }

        Frag join(Frag a, Frag b) {
            if (a.head == 0) return b;
            if (b.head == 0) return a;
            patchNext[a.tail - 1] = b.head;
            return new Frag(a.start, a.head, b.tail);
        }

        Frag either(Frag a, Frag b) {
            int start = emit(SPLIT, a.start, b.start, -1, -1, 0, 0);
            Frag pending = join(a, b);
            return new Frag(start, pending.head, pending.tail);
        }

        Frag repeat(Frag f, char quantifier) {
            int branch = emit(SPLIT, f.start, -1, -1, -1, 0, 0);
            Frag exit = edge(branch, 1);
            if (quantifier == '?') {
                Frag pending = join(f, exit);
                return new Frag(branch, pending.head, pending.tail);
            }
            patch(f.head, branch);
            return new Frag(quantifier == '*' ? branch : f.start, exit.head, exit.tail);
        }
    }

    private static final class Parser {
        final String pattern;
        final Builder builder = new Builder();
        int index;
        int depth;

        Parser(String pattern) { this.pattern = pattern; }

        void space() {
            while (index < pattern.length() && Character.isWhitespace(pattern.charAt(index))) index++;
        }

        boolean consume(char symbol) {
            space();
            if (index < pattern.length() && pattern.charAt(index) == symbol) {
                index++;
                return true;
            }
            return false;
        }

        Frag alternative() {
            Frag left = sequence();
            while (consume('|')) left = builder.either(left, sequence());
            return left;
        }

        Frag sequence() {
            Frag result = null;
            while (true) {
                space();
                if (index >= pattern.length() || pattern.charAt(index) == ')'
                        || pattern.charAt(index) == '|') break;
                Frag nextFrag = quantified();
                result = result == null ? nextFrag : builder.concat(result, nextFrag);
            }
            if (result != null) return result;
            return builder.edge(builder.emit(JUMP, -1, -1, -1, -1, 0, 0), 0);
        }

        Frag quantified() {
            Frag result = primary();
            space();
            if (index < pattern.length()) {
                char operator = pattern.charAt(index);
                if (operator == '?' || operator == '*' || operator == '+') {
                    index++;
                    result = builder.repeat(result, operator);
                }
            }
            return result;
        }

        Frag primary() {
            space();
            if (index == pattern.length()) throw invalid("missing atom");
            char marker = pattern.charAt(index++);
            if (marker == '(') {
                if (++depth > MAX_GROUP_DEPTH) throw invalid("group depth budget");
                Frag sub = alternative();
                if (!consume(')')) throw invalid("unclosed group");
                depth--;
                return sub;
            }
            if (marker == '.') return builder.edge(builder.emit(TOKEN, -1, -1, -1, -1, 0, 0), 0);
            if (marker == '^') return builder.edge(builder.emit(BEGIN, -1, -1, -1, -1, 0, 0), 0);
            if (marker == '$') return builder.edge(builder.emit(END, -1, -1, -1, -1, 0, 0), 0);
            if (marker == '[') return predicate();
            throw invalid("expected token predicate or group");
        }

        Frag predicate() {
            int finish = pattern.indexOf(']', index);
            if (finish < 0) throw invalid("unclosed token predicate");
            String inside = pattern.substring(index, finish).trim();
            index = finish + 1;
            int k = -1, a = -1, required = 0, forbidden = 0, encountered = 0;
            if (!inside.isEmpty()) {
                for (String spec : inside.split(",", -1)) {
                    String[] pair = spec.trim().split("=", -1);
                    if (pair.length != 2) throw invalid("invalid predicate field");
                    String field = pair[0].trim();
                    String value = pair[1].trim();
                    switch (field) {
                        case "kind" -> {
                            if ((encountered & 1) != 0) throw invalid("duplicate kind");
                            k = integer(value);
                            encountered |= 1;
                        }
                        case "atom" -> {
                            if ((encountered & 2) != 0) throw invalid("duplicate atom");
                            a = integer(value);
                            encountered |= 2;
                        }
                        case "all" -> {
                            if ((encountered & 4) != 0) throw invalid("duplicate all");
                            required = unsigned(value);
                            encountered |= 4;
                        }
                        case "none" -> {
                            if ((encountered & 8) != 0) throw invalid("duplicate none");
                            forbidden = unsigned(value);
                            encountered |= 8;
                        }
                        default -> throw invalid("unknown token attribute");
                    }
                }
            }
            if ((required & forbidden) != 0) throw invalid("contradictory token masks");
            return builder.edge(builder.emit(TOKEN, -1, -1, k, a, required, forbidden), 0);
        }

        int integer(String value) {
            try {
                int decoded = Integer.decode(value);
                if (decoded < 0) throw new NumberFormatException("negative");
                return decoded;
            } catch (NumberFormatException invalid) {
                throw invalid("invalid kind/atom identifier");
            }
        }

        int unsigned(String value) {
            try {
                return value.startsWith("0x") || value.startsWith("0X")
                        ? Integer.parseUnsignedInt(value.substring(2), 16)
                        : Integer.parseUnsignedInt(value);
            } catch (NumberFormatException invalid) {
                throw invalid("invalid bit mask");
            }
        }

        IllegalArgumentException invalid(String reason) {
            return new IllegalArgumentException(reason + " at pattern UTF-16 position " + index);
        }
    }
}
