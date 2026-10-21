// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.jackson;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable, bounded regular-language matcher over immediate JSON AST child
 * occurrences. The input alphabet consists of field/array token attributes,
 * not UTF-16 characters. Compilation allocates plans; scanning allocates two
 * primitive state arrays per call, never one object per token or AST node.
 *
 * <p>Admitted syntax: [{kind:FIELD}&{name:status}&{value:failed}], [] wildcard,
 * whitespace concatenation, |, (...), ?, * and +. Attribute keys are kind,
 * name, value and valueKind. Quoted values support JSON-like escapes.
 * This is a deliberately bounded subset inspired by T-Rex, not its grammar
 * or a replacement for java.util.regex.Pattern/Matcher or M3RX7.
 *
 * <p>A field's value attribute denotes its immediate scalar child (if any),
 * and valueKind denotes the immediate child's kind. Matching is restricted to
 * direct ordered children of one OBJECT or ARRAY; no implicit descent or
 * match across unrelated nested container boundaries.
 */
public final class M3ASTTokenPattern {
    private static final int MAX_STATES = 63;
    private static final int MAX_CLAUSES = 256;
    private static final int MAX_PATTERN_CHARS = 4096;
    private static final byte TEST = 1, SPLIT = 2, JUMP = 3, ACCEPT = 4;
    private static final byte KIND = 1, NAME = 2, VALUE = 3, VALUE_KIND = 4;

    private final byte[] operations;
    private final int[] first;
    private final int[] second;
    private final int[] predicateId;
    private final int[] clauseOffset;
    private final int[] clauseCount;
    private final byte[] property;
    private final String[] expected;
    private final long[] epsilon;
    private final int start;
    private final int accept;

    private M3ASTTokenPattern(Compiler compiler, Fragment complete) {
        accept = compiler.emit(ACCEPT, -1);
        compiler.patch(complete.holes, accept);
        start = complete.start;
        operations = Arrays.copyOf(compiler.op, compiler.states);
        first = Arrays.copyOf(compiler.next, compiler.states);
        second = Arrays.copyOf(compiler.alt, compiler.states);
        predicateId = Arrays.copyOf(compiler.predicate, compiler.states);
        clauseOffset = Arrays.copyOf(compiler.offsets, compiler.predicates);
        clauseCount = Arrays.copyOf(compiler.lengths, compiler.predicates);
        property = Arrays.copyOf(compiler.properties, compiler.clauses);
        expected = Arrays.copyOf(compiler.values, compiler.clauses);
        epsilon = new long[operations.length];
        for (int i = 0; i < operations.length; i++) epsilon[i] = 1L << i;
        boolean changed;
        do {
            changed = false;
            for (int i = operations.length - 1; i >= 0; i--) {
                long nextMask = epsilon[i];
                if (operations[i] == JUMP || operations[i] == SPLIT) {
                    nextMask |= epsilon[first[i]];
                    if (operations[i] == SPLIT) nextMask |= epsilon[second[i]];
                }
                if (nextMask != epsilon[i]) {
                    epsilon[i] = nextMask;
                    changed = true;
                }
            }
        } while (changed);
    }

    /** Compile the admitted structural token-regular-expression subset. */
    public static M3ASTTokenPattern compile(String expression) {
        Objects.requireNonNull(expression, "expression");
        if (expression.isEmpty() || expression.length() > MAX_PATTERN_CHARS) {
            throw new IllegalArgumentException("token regex pattern length outside 1..4096");
        }
        Compiler compiler = new Compiler(expression);
        Fragment result = compiler.parseAlternative();
        compiler.space();
        if (!compiler.end()) throw compiler.error("unexpected trailing pattern character");
        return new M3ASTTokenPattern(compiler, result);
    }

    /** Full-match the direct child sequence (not the container node itself). */
    public boolean matches(M3AST ast, int containerAtom) {
        int size = requireContainer(ast, containerAtom);
        int[] active = new int[operations.length];
        int[] following = new int[operations.length];
        Arrays.fill(active, -1);
        addClosure(active, start, 0);
        for (int pos = 0; pos < size; pos++) {
            Arrays.fill(following, -1);
            advance(ast, ast.childAtomIndexAt(containerAtom, pos), active, following);
            int[] tmp = active;
            active = following;
            following = tmp;
        }
        return active[accept] >= 0;
    }

    /**
     * Earliest-completing span at or after fromOrdinal. The low/high 32 bits
     * encode the exclusive end and inclusive start, respectively. -1 means
     * absent. Within one ending boundary, the earliest starting origin wins.
     *
     * <p>This deliberately does not claim Java Matcher.find leftmost-first,
     * greedy captures, backreferences or lookaround behavior.
     */
    public long find(M3AST ast, int containerAtom, int fromOrdinal) {
        int size = requireContainer(ast, containerAtom);
        if (fromOrdinal < 0 || fromOrdinal > size) {
            throw new IndexOutOfBoundsException("start token ordinal: " + fromOrdinal);
        }
        int[] active = new int[operations.length];
        int[] following = new int[operations.length];
        Arrays.fill(active, -1);
        for (int pos = fromOrdinal; pos <= size; pos++) {
            addClosure(active, start, pos);
            if (active[accept] >= 0) {
                return ((long) active[accept] << 32) | (pos & 0xffffffffL);
            }
            if (pos == size) break;
            Arrays.fill(following, -1);
            advance(ast, ast.childAtomIndexAt(containerAtom, pos), active, following);
            int[] tmp = active;
            active = following;
            following = tmp;
        }
        return -1L;
    }

    public long find(M3AST ast, int containerAtom) {
        return find(ast, containerAtom, 0);
    }

    public static int startOrdinal(long span) {
        if (span == -1L) throw new IllegalArgumentException("no token match");
        return (int) (span >>> 32);
    }

    public static int endOrdinal(long span) {
        if (span == -1L) throw new IllegalArgumentException("no token match");
        return (int) span;
    }

    private static int requireContainer(M3AST ast, int atom) {
        Objects.requireNonNull(ast, "ast");
        String kind = ast.kindAt(atom);
        if (!"OBJECT".equals(kind) && !"ARRAY".equals(kind)) {
            throw new IllegalArgumentException("token sequence requires JSON OBJECT or ARRAY");
        }
        return ast.childCountAt(atom);
    }

    private void advance(M3AST ast, int atom, int[] active, int[] following) {
        for (int state = 0; state < operations.length; state++) {
            if (active[state] >= 0 && operations[state] == TEST
                    && test(ast, atom, predicateId[state])) {
                addClosure(following, first[state], active[state]);
            }
        }
    }

    private void addClosure(int[] positions, int state, int origin) {
        long mask = epsilon[state];
        while (mask != 0) {
            int reached = Long.numberOfTrailingZeros(mask);
            if (positions[reached] < 0 || positions[reached] > origin) {
                positions[reached] = origin;
            }
            mask &= mask - 1;
        }
    }

    private boolean test(M3AST ast, int atom, int predicateIndex) {
        for (int i = clauseOffset[predicateIndex],
                end = i + clauseCount[predicateIndex]; i < end; i++) {
            String actual = switch (property[i]) {
                case KIND -> ast.kindAt(atom);
                case NAME -> ast.nameAt(atom);
                case VALUE -> {
                    if ("FIELD".equals(ast.kindAt(atom))) {
                        yield ast.childCountAt(atom) == 1
                                ? ast.valueAt(ast.childAtomIndexAt(atom, 0)) : null;
                    }
                    yield ast.valueAt(atom);
                }
                case VALUE_KIND -> {
                    if ("FIELD".equals(ast.kindAt(atom))) {
                        yield ast.childCountAt(atom) == 1
                                ? ast.kindAt(ast.childAtomIndexAt(atom, 0)) : null;
                    }
                    yield ast.kindAt(atom);
                }
                default -> throw new AssertionError("unrecognized token property");
            };
            if (!expected[i].equals(actual)) return false;
        }
        return true;
    }

    /** Compilation-only patch list; not retained or instantiated during scanning. */
    private record Fragment(int start, int[] holes) { }

    private static final class Compiler {
        final String text;
        final byte[] op = new byte[MAX_STATES];
        final int[] next = new int[MAX_STATES];
        final int[] alt = new int[MAX_STATES];
        final int[] predicate = new int[MAX_STATES];
        final int[] offsets = new int[MAX_STATES];
        final int[] lengths = new int[MAX_STATES];
        final byte[] properties = new byte[MAX_CLAUSES];
        final String[] values = new String[MAX_CLAUSES];
        int position;
        int states;
        int clauses;
        int predicates;
        int depth;

        Compiler(String text) { this.text = text; }

        boolean end() { return position >= text.length(); }
        void space() {
            while (!end() && Character.isWhitespace(text.charAt(position))) position++;
        }
        boolean take(char expected) {
            space();
            if (!end() && text.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }
        void require(char expected) {
            if (!take(expected)) throw error("expected '" + expected + "'");
        }
        IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at char " + position);
        }
        int emit(byte kind, int value) {
            if (states >= MAX_STATES) throw error("pattern exceeds 63 NFA states");
            int index = states++;
            op[index] = kind;
            predicate[index] = value;
            next[index] = -1;
            alt[index] = -1;
            return index;
        }
        void patch(int[] holes, int target) {
            for (int encoded : holes) {
                int state = encoded >>> 1;
                if ((encoded & 1) == 0) next[state] = target;
                else alt[state] = target;
            }
        }
        int[] join(int[] left, int[] right) {
            int[] result = Arrays.copyOf(left, left.length + right.length);
            System.arraycopy(right, 0, result, left.length, right.length);
            return result;
        }
        Fragment sequence() {
            Fragment acc = null;
            space();
            while (!end()) {
                char ch = text.charAt(position);
                if (ch == ')' || ch == '|') break;
                Fragment item = quantified();
                if (acc == null) acc = item;
                else {
                    patch(acc.holes, item.start);
                    acc = new Fragment(acc.start, item.holes);
                }
                space();
            }
            if (acc != null) return acc;
            int state = emit(JUMP, -1);
            return new Fragment(state, new int[] {state * 2});
        }
        Fragment parseAlternative() {
            Fragment left = sequence();
            while (take('|')) {
                Fragment right = sequence();
                int state = emit(SPLIT, -1);
                next[state] = left.start;
                alt[state] = right.start;
                left = new Fragment(state, join(left.holes, right.holes));
            }
            return left;
        }
        Fragment quantified() {
            Fragment atom = atom();
            space();
            if (end()) return atom;
            char ch = text.charAt(position);
            if (ch != '*' && ch != '+' && ch != '?') return atom;
            position++;
            int split = emit(SPLIT, -1);
            if (ch == '*') {
                next[split] = atom.start;
                patch(atom.holes, split);
                atom = new Fragment(split, new int[] {split * 2 + 1});
            } else if (ch == '+') {
                next[split] = atom.start;
                patch(atom.holes, split);
                atom = new Fragment(atom.start, new int[] {split * 2 + 1});
            } else {
                next[split] = atom.start;
                atom = new Fragment(split, join(atom.holes, new int[] {split * 2 + 1}));
            }
            if (!end()) {
                char nextChar = text.charAt(position);
                if (nextChar == '*' || nextChar == '+' || nextChar == '?') {
                    throw error("stacked quantifiers unsupported");
                }
            }
            return atom;
        }
        Fragment atom() {
            space();
            if (take('(')) {
                if (++depth > 32) throw error("pattern grouping depth exceeded");
                Fragment body = parseAlternative();
                require(')');
                depth--;
                return body;
            }
            if (take('[')) {
                int index = predicateToken();
                int state = emit(TEST, index);
                return new Fragment(state, new int[] {state * 2});
            }
            throw error("expected token predicate, [] wildcard or group");
        }
        int predicateToken() {
            if (predicates >= MAX_STATES) throw error("too many predicate tokens");
            int id = predicates++;
            offsets[id] = clauses;
            if (take(']')) return id;  // Any single token.
            do {
                require('{');
                String key = identifier();
                require(':');
                String value = literal();
                require('}');
                if (clauses == MAX_CLAUSES) throw error("too many token constraints");
                properties[clauses] = switch (key) {
                    case "kind" -> KIND;
                    case "name" -> NAME;
                    case "value" -> VALUE;
                    case "valueKind" -> VALUE_KIND;
                    default -> throw error("unknown JSON token property " + key);
                };
                values[clauses++] = value;
                lengths[id]++;
            } while (take('&'));
            require(']');
            return id;
        }
        String identifier() {
            space();
            int start = position;
            if (end() || !(Character.isLetter(text.charAt(position))
                    || text.charAt(position) == '_')) throw error("expected property name");
            position++;
            while (!end()) {
                char ch = text.charAt(position);
                if (Character.isLetterOrDigit(ch) || ch == '_') position++;
                else break;
            }
            return text.substring(start, position);
        }
        String literal() {
            space();
            if (take('"')) {
                StringBuilder result = new StringBuilder();
                while (!end()) {
                    char ch = text.charAt(position++);
                    if (ch == '"') return result.toString();
                    if (ch != '\\') {
                        result.append(ch);
                        continue;
                    }
                    if (end()) throw error("truncated escape");
                    char escaped = text.charAt(position++);
                    switch (escaped) {
                        case 'n' -> result.append('\n');
                        case 't' -> result.append('\t');
                        case 'r' -> result.append('\r');
                        case 'b' -> result.append('\b');
                        case 'f' -> result.append('\f');
                        case '\\', '"' -> result.append(escaped);
                        case 'u' -> {
                            if (position + 4 > text.length()) throw error("truncated unicode escape");
                            int unit = 0;
                            for (int j = 0; j < 4; j++) {
                                int digit = Character.digit(text.charAt(position++), 16);
                                if (digit < 0) throw error("invalid unicode escape");
                                unit = unit * 16 + digit;
                            }
                            result.append((char) unit);
                        }
                        default -> throw error("invalid quoted literal escape");
                    }
                }
                throw error("unterminated quoted literal");
            }
            int start = position;
            while (!end() && !Character.isWhitespace(text.charAt(position))
                    && text.charAt(position) != '}') position++;
            if (start == position) throw error("empty literal");
            return text.substring(start, position);
        }
    }
}
