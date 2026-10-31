// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.jackson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bounded token-level regex over original JSON AST occurrences, not raw text characters.
 *
 * <p>The parsed pattern is immutable. Execution visits M3ASTPool's existing primitive
 * coordinates without constructing M3ASTAtom, Map or per-token match objects. Thompson
 * NFA states and live origins occupy primitive arrays. Java Pattern is used only for
 * optional regex predicates on an individual token's existing String attribute.
 *
 * <p>Supported syntax: bare kind names, wildcard {@code []} or {@code .}, and
 * {@code [{kind:FIELD}&{name:/id|age/}]} predicates; concatenation, {@code |},
 * grouping, {@code ? * +}, and token-stream anchors {@code ^ $}. Parentheses are
 * noncapturing in this bounded implementation. Whole-match coordinates are half-open
 * token occurrence offsets (not byte/UTF-16 source offsets).
 *
 * <p>Not a replacement for java.util.regex.Pattern/Matcher or a full T-Rex grammar.
 */
public final class M3TokenRegex {
    private static final int CONSUME = 1, SPLIT = 2, JUMP = 3;
    private static final int BEGIN = 4, END = 5, ACCEPT = 6;
    private static final int MAX_PATTERN_CHARS = 4096;
    private static final int MAX_NFA_STATES = 512;
    private static final long NOT_ACTIVE = Long.MAX_VALUE;

    /** Half-open [start,end) token-occurrence span. */
    public record Span(long startToken, long endToken) {
        public Span {
            if (startToken < 0 || endToken < startToken) {
                throw new IllegalArgumentException("invalid token occurrence span");
            }
        }
    }

    private final String expression;
    private final int entry;
    private final int accept;
    private final int[] operation;
    private final int[] next;
    private final int[] alternate;
    private final Rule[] rules;
    private final Pattern[] attributePatterns;

    private M3TokenRegex(String expression, Compiler c, int entry, int accept) {
        this.expression = expression;
        this.entry = entry;
        this.accept = accept;
        operation = Arrays.copyOf(c.operation, c.size);
        next = Arrays.copyOf(c.next, c.size);
        alternate = Arrays.copyOf(c.alternate, c.size);
        rules = Arrays.copyOf(c.rules, c.size);
        attributePatterns = c.attributePatterns.toArray(Pattern[]::new);
    }

    /** Compile a bounded regular expression whose atomic unit is one AST token event. */
    public static M3TokenRegex compile(String expression) {
        Objects.requireNonNull(expression, "expression");
        if (expression.length() > MAX_PATTERN_CHARS) {
            throw new IllegalArgumentException("token expression too long");
        }
        Compiler compiler = new Compiler();
        Parser parser = new Parser(expression, compiler);
        Frag parsed = parser.alternation();
        parser.spaces();
        if (parser.offset != expression.length()) throw parser.error("unexpected token");
        int accept = compiler.state(ACCEPT, null, -1, -1);
        compiler.patch(parsed.holes, accept);
        return new M3TokenRegex(expression, compiler, parsed.start, accept);
    }

    public String expression() { return expression; }

    /**
     * Finds the leftmost-longest match. Returns null when none exists.
     * The token visit and tree-depth budgets protect large or cyclic-by-error inputs.
     */
    public Span findFirst(M3AST ast, long maxTokenEvents, int maxDepth) {
        return evaluate(ast, maxTokenEvents, maxDepth, true);
    }

    /** True only if the entire emitted token-event sequence matches the expression. */
    public boolean matches(M3AST ast, long maxTokenEvents, int maxDepth) {
        return evaluate(ast, maxTokenEvents, maxDepth, false) != null;
    }

    private Span evaluate(M3AST ast, long maxTokenEvents, int maxDepth, boolean search) {
        Objects.requireNonNull(ast, "ast");
        if (maxTokenEvents < 1 || maxDepth < 1) {
            throw new IllegalArgumentException("positive token/depth budgets required");
        }
        Cursor cursor = new Cursor(ast, maxTokenEvents, maxDepth);
        Matcher[] matchers = new Matcher[attributePatterns.length];
        for (int i = 0; i < matchers.length; ++i) {
            matchers[i] = attributePatterns[i].matcher("");
        }
        long[] current = new long[operation.length];
        long[] following = new long[operation.length];
        Arrays.fill(current, NOT_ACTIVE);
        Arrays.fill(following, NOT_ACTIVE);
        long first = NOT_ACTIVE;
        long last = -1;
        long position = 0;
        add(current, entry, 0, 0, false);
        while (cursor.advance()) {
            if (search) add(current, entry, position, position, false);
            if (search && current[accept] != NOT_ACTIVE
                    && (current[accept] < first
                    || (current[accept] == first && position > last))) {
                first = current[accept];
                last = position;
            }
            Arrays.fill(following, NOT_ACTIVE);
            for (int state = 0; state < operation.length; ++state) {
                if (operation[state] == CONSUME && current[state] != NOT_ACTIVE
                        && rules[state].accepts(cursor, matchers)) {
                    add(following, next[state], current[state], position + 1, false);
                }
            }
            long[] temporary = current;
            current = following;
            following = temporary;
            position++;
        }
        if (search) add(current, entry, position, position, false);
        // End anchors are enabled only after the cursor proves end of input.
        for (int state = 0; state < operation.length; ++state) {
            if (operation[state] == END && current[state] != NOT_ACTIVE) {
                add(current, next[state], current[state], position, true);
            }
        }
        if (!search) {
            return current[accept] == 0 ? new Span(0, position) : null;
        }
        if (current[accept] != NOT_ACTIVE && (current[accept] < first
                || (current[accept] == first && position > last))) {
            first = current[accept];
            last = position;
        }
        return first == NOT_ACTIVE ? null : new Span(first, last);
    }

    /** Epsilon closure; earliest origin per state suffices for leftmost-longest selection. */
    private void add(long[] active, int state, long origin, long position, boolean atEnd) {
        if (state < 0 || origin >= active[state]) return;
        active[state] = origin;
        switch (operation[state]) {
            case SPLIT -> {
                add(active, next[state], origin, position, atEnd);
                add(active, alternate[state], origin, position, atEnd);
            }
            case JUMP -> add(active, next[state], origin, position, atEnd);
            case BEGIN -> {
                if (position == 0) add(active, next[state], origin, position, atEnd);
            }
            case END -> {
                if (atEnd) add(active, next[state], origin, position, true);
            }
            default -> { } // CONSUME and ACCEPT are NFA frontiers.
        }
    }

    private record Clause(int attribute, String exact, int patternIndex) {
        boolean accepts(Cursor token, Matcher[] matchers) {
            String value = switch (attribute) {
                case 0 -> token.kind;
                case 1 -> token.name;
                case 2 -> token.value;
                default -> throw new AssertionError("unknown attribute");
            };
            if (value == null) return false;
            return patternIndex < 0 ? exact.equals(value)
                    : matchers[patternIndex].reset(value).matches();
        }
    }

    private record Rule(Clause[] conjunction) {
        boolean accepts(Cursor token, Matcher[] matchers) {
            for (Clause clause : conjunction) {
                if (!clause.accepts(token, matchers)) return false;
            }
            return true;
        }
    }

    /** Iterative primitive stack over original occurrence paths, even for hash-consed DAGs. */
    private static final class Cursor {
        private final M3ASTPool pool;
        private final int root;
        private final long limit;
        private final int maxDepth;
        private int[] atoms = new int[16];
        private int[] childPositions = new int[16];
        private int depth;
        private boolean started;
        private long seen;
        String kind;
        String name;
        String value;

        Cursor(M3AST ast, long limit, int maxDepth) {
            pool = ast.tokenPool();
            root = ast.rootAtomIndex();
            this.limit = limit;
            this.maxDepth = maxDepth;
        }

        boolean advance() {
            if (!started) {
                started = true;
                push(root);
            }
            while (depth != 0) {
                int top = depth - 1;
                int atom = atoms[top];
                String atomKind = pool.kind(atom);
                if (childPositions[top] == -1) {
                    childPositions[top] = 0;
                    return emit(atom, atomKind, false);
                }
                if (childPositions[top] < pool.childCount(atom)) {
                    push(pool.childAt(atom, childPositions[top]++));
                    continue;
                }
                depth--;
                if (atomKind.equals("OBJECT") || atomKind.equals("ARRAY")) {
                    return emit(atom, atomKind.equals("OBJECT") ? "END_OBJECT" : "END_ARRAY", true);
                }
            }
            kind = null;
            name = null;
            value = null;
            return false;
        }

        private void push(int atom) {
            if (depth >= maxDepth) throw new IllegalArgumentException("token traversal depth exceeded");
            if (depth == atoms.length) {
                int expanded = (int) Math.min((long) maxDepth, (long) atoms.length * 2);
                atoms = Arrays.copyOf(atoms, expanded);
                childPositions = Arrays.copyOf(childPositions, expanded);
            }
            atoms[depth] = atom;
            childPositions[depth] = -1;
            depth++;
        }

        private boolean emit(int atom, String eventKind, boolean closing) {
            if (seen == limit) throw new IllegalArgumentException("token event limit exceeded");
            seen++;
            kind = eventKind;
            name = closing ? null : pool.name(atom);
            value = closing ? null : pool.value(atom);
            return true;
        }
    }

    private record Frag(int start, int[] holes) { }

    /** Builder holds only bounded primitive opcode and transition lanes. */
    private static final class Compiler {
        private int[] operation = new int[16];
        private int[] next = new int[16];
        private int[] alternate = new int[16];
        private Rule[] rules = new Rule[16];
        private final ArrayList<Pattern> attributePatterns = new ArrayList<>();
        private int size;

        int state(int op, Rule predicate, int target, int other) {
            if (size >= MAX_NFA_STATES) throw new IllegalArgumentException("token regex NFA state budget");
            if (size == operation.length) {
                operation = Arrays.copyOf(operation, size * 2);
                next = Arrays.copyOf(next, size * 2);
                alternate = Arrays.copyOf(alternate, size * 2);
                rules = Arrays.copyOf(rules, size * 2);
            }
            int id = size++;
            operation[id] = op;
            next[id] = target;
            alternate[id] = other;
            rules[id] = predicate;
            return id;
        }

        void patch(int[] holes, int destination) {
            for (int encoded : holes) {
                int state = encoded >>> 1;
                if ((encoded & 1) == 0) {
                    if (next[state] >= 0) throw new AssertionError("already patched");
                    next[state] = destination;
                } else {
                    if (alternate[state] >= 0) throw new AssertionError("already patched");
                    alternate[state] = destination;
                }
            }
        }

        static int[] union(int[] left, int[] right) {
            int[] both = Arrays.copyOf(left, left.length + right.length);
            System.arraycopy(right, 0, both, left.length, right.length);
            return both;
        }

        Frag atom(Rule rule) {
            int state = state(CONSUME, rule, -1, -1);
            return new Frag(state, new int[] { state << 1 });
        }

        Frag assertion(int code) {
            int state = state(code, null, -1, -1);
            return new Frag(state, new int[] { state << 1 });
        }

        Frag epsilon() { return assertion(JUMP); }

        Frag concat(Frag left, Frag right) {
            patch(left.holes, right.start);
            return new Frag(left.start, right.holes);
        }

        Frag alternate(Frag left, Frag right) {
            int split = state(SPLIT, null, left.start, right.start);
            return new Frag(split, union(left.holes, right.holes));
        }

        Frag quantify(Frag child, char op) {
            int split;
            return switch (op) {
                case '?' -> {
                    split = state(SPLIT, null, child.start, -1);
                    yield new Frag(split, union(child.holes, new int[] { (split << 1) | 1 }));
                }
                case '*' -> {
                    split = state(SPLIT, null, child.start, -1);
                    patch(child.holes, split);
                    yield new Frag(split, new int[] { (split << 1) | 1 });
                }
                case '+' -> {
                    split = state(SPLIT, null, child.start, -1);
                    patch(child.holes, split);
                    yield new Frag(child.start, new int[] { (split << 1) | 1 });
                }
                default -> throw new AssertionError("invalid quantifier");
            };
        }
    }

    private static final class Parser {
        private final String input;
        private final Compiler compiler;
        private int offset;
        private int nesting;
        Parser(String input, Compiler compiler) {
            this.input = input;
            this.compiler = compiler;
        }
        IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at token pattern offset " + offset);
        }
        void spaces() {
            while (offset < input.length() && Character.isWhitespace(input.charAt(offset))) offset++;
        }
        boolean take(char symbol) {
            spaces();
            if (offset < input.length() && input.charAt(offset) == symbol) {
                offset++;
                return true;
            }
            return false;
        }
        void expect(char symbol) {
            if (!take(symbol)) throw error("expected '" + symbol + "'");
        }
        Frag alternation() {
            Frag first = sequence();
            while (take('|')) first = compiler.alternate(first, sequence());
            return first;
        }
        Frag sequence() {
            Frag result = null;
            while (true) {
                spaces();
                if (offset == input.length() || input.charAt(offset) == ')'
                        || input.charAt(offset) == '|') break;
                Frag next = quantified();
                result = result == null ? next : compiler.concat(result, next);
            }
            return result == null ? compiler.epsilon() : result;
        }
        Frag quantified() {
            Frag atom = primary();
            spaces();
            if (offset < input.length() && "?*+".indexOf(input.charAt(offset)) >= 0) {
                atom = compiler.quantify(atom, input.charAt(offset++));
                spaces();
                if (offset < input.length() && "?*+".indexOf(input.charAt(offset)) >= 0) {
                    throw error("nested and lazy quantifiers not in bounded grammar");
                }
            }
            return atom;
        }
        Frag primary() {
            spaces();
            if (take('(')) {
                if (++nesting > 64) throw error("group nesting budget exceeded");
                Frag nested = alternation();
                expect(')');
                nesting--;
                return nested;
            }
            if (take('^')) return compiler.assertion(BEGIN);
            if (take('$')) return compiler.assertion(END);
            if (take('.')) return compiler.atom(new Rule(new Clause[0]));
            if (take('[')) return predicate();
            String kind = word();
            return compiler.atom(new Rule(new Clause[] {new Clause(0, kind, -1)}));
        }
        Frag predicate() {
            ArrayList<Clause> clauses = new ArrayList<>();
            if (take(']')) return compiler.atom(new Rule(new Clause[0]));
            while (true) {
                boolean braces = take('{');
                String key = word();
                int attribute = switch (key) {
                    case "kind" -> 0;
                    case "name" -> 1;
                    case "value" -> 2;
                    default -> throw error("unknown token attribute: " + key);
                };
                expect(':');
                spaces();
                int patternIndex = -1;
                String exact;
                if (take('/')) {
                    String regex = delimited('/');
                    patternIndex = compiler.attributePatterns.size();
                    compiler.attributePatterns.add(Pattern.compile(regex));
                    exact = null;
                } else if (take('"')) {
                    exact = delimited('"');
                } else {
                    exact = word();
                }
                if (braces) expect('}');
                clauses.add(new Clause(attribute, exact, patternIndex));
                if (take(']')) break;
                if (!take('&') && !take(',')) throw error("expected '&', ',' or ']' between token attributes");
            }
            return compiler.atom(new Rule(clauses.toArray(Clause[]::new)));
        }
        String word() {
            spaces();
            int start = offset;
            while (offset < input.length()) {
                char c = input.charAt(offset);
                if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-')) break;
                offset++;
            }
            if (start == offset) throw error("expected token identifier or unquoted value");
            return input.substring(start, offset);
        }
        String delimited(char closing) {
            StringBuilder result = new StringBuilder();
            boolean escaped = false;
            while (offset < input.length()) {
                char c = input.charAt(offset++);
                if (escaped) {
                    if (closing == '"') {
                        if (c != '"' && c != '\\') throw error("unsupported quoted escape");
                        result.append(c);
                    } else {
                        // Preserve regex syntax escaping, including escaped '/' text.
                        if (c != '/') result.append('\\');
                        result.append(c);
                    }
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == closing) {
                    return result.toString();
                } else {
                    result.append(c);
                }
            }
            throw error("unterminated attribute value");
        }
    }
}
