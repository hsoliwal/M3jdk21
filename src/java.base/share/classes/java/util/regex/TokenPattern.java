/*
 * Copyright (c) 1999, 2023, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.util.regex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * An immutable, programmatically constructed regular expression over tokens.
 * A token is one list element, including a null element. Its text, length,
 * lexical kind, and attributes are interpreted only by the supplied predicates.
 * Matching never calls a token's {@code toString()} method or tokenizes text.
 *
 * <p>Patterns support concatenation, ordered alternatives, greedy, reluctant,
 * and possessive repetition, and numbered or named captures. Capture and region
 * coordinates are list indices. Captures are numbered from one in construction
 * order: left to right through sequences and alternatives, with an enclosing
 * capture numbered before its contents. Group zero denotes the entire match.
 * Composing patterns assigns fresh group numbers to their occurrences.
 *
 * <p>For example, if {@code Lexeme} has a {@code kind()} method, a pattern for an
 * identifier followed by optional whitespace can be constructed as follows:
 * <pre>{@code
 * TokenPattern<Lexeme> identifier = TokenPattern.token(t -> t.kind() == IDENTIFIER);
 * TokenPattern<Lexeme> whitespace = TokenPattern.token(t -> t.kind() == WHITESPACE);
 * TokenPattern<Lexeme> expression = identifier.capture("name")
 *         .then(whitespace.zeroOrMore());
 * TokenMatcher<Lexeme> matcher = expression.matcher(lexemes);
 * if (matcher.find()) {
 *     List<Lexeme> name = matcher.group("name");
 * }
 * }</pre>
 *
 * <p>Predicates may be evaluated repeatedly during backtracking. They must give
 * stable results during a matching operation; no evaluation count is promised.
 * Exceptions from predicates or literal equality comparisons propagate unchanged.
 * Patterns may be shared between threads when their predicates and literal
 * values support concurrent use. Each matcher has independent mutable state and
 * owns a defensive shallow snapshot of its input list.
 *
 * @implNote The existing {@link Pattern} and {@link Matcher} graph nodes own
 * alternation, repetition, captures, region handling, and search progress. A
 * token predicate adds a single consuming node to that graph. No textual pattern
 * language, backreference, lookaround, or lexer is introduced by this API. The
 * existing backtracking engine's time, stack, and memory limits still apply;
 * this API does not promise linear-time matching.
 *
 * @param <T> the token type
 * @since 21
 */
public final class TokenPattern<T> {
    /** The policy used when a repetition can consume more than one length. */
    public enum Quantifier {
        /** Prefer more repetitions, allowing backtracking. */
        GREEDY,
        /** Prefer fewer repetitions, allowing backtracking. */
        RELUCTANT,
        /** Prefer more repetitions and do not backtrack out of the repetition. */
        POSSESSIVE
    }

    private final Plan plan;
    private final Pattern engine;
    private final Map<String, Integer> names;

    private TokenPattern(Plan plan) {
        this.plan = plan;
        Build build = new Build();
        Fragment fragment = plan.build(build);
        Pattern.Node end = new Pattern.LastNode();
        Pattern.Node matchRoot = fragment.head == null ? end : fragment.head;
        if (fragment.tail != null) fragment.tail.next = end;

        // The empty constructor installs no character-derived M3 query plans.
        // This private owner is used only through new Matcher(owner, input),
        // never through Pattern.matcher(), which would invoke the text compiler.
        Pattern owner = Pattern.compile("");
        owner.matchRoot = matchRoot;
        owner.root = new Pattern.Start(matchRoot);
        owner.capturingGroupCount = build.groups;
        owner.localCount = build.locals;
        names = Map.copyOf(build.names);
        owner.namedGroups = names;
        engine = owner;
    }

    /**
     * Creates a pattern that consumes exactly one token accepted by a predicate.
     * The predicate receives null when the input element is null.
     *
     * @param <T> the token type
     * @param predicate the token predicate
     * @return the token pattern
     * @throws NullPointerException if the predicate is null
     */
    public static <T> TokenPattern<T> token(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return new TokenPattern<>(build -> {
            Pattern.Node node = new TokenNode<>(predicate);
            return new Fragment(node, node);
        });
    }

    /**
     * Creates a pattern that consumes one token, including a null token.
     *
     * @param <T> the token type
     * @return the token pattern
     */
    public static <T> TokenPattern<T> anyToken() {
        return token(value -> true);
    }

    /**
     * Creates a pattern that consumes one token for which
     * {@code Objects.equals(value, token)} is true. The literal reference is
     * retained; it is not cloned. Null is a valid literal.
     *
     * @param <T> the token type
     * @param value the literal token
     * @return the token pattern
     */
    public static <T> TokenPattern<T> literal(T value) {
        return token(candidate -> Objects.equals(value, candidate));
    }

    /**
     * Creates a pattern that matches an empty sequence.
     *
     * @param <T> the token type
     * @return the empty pattern
     */
    public static <T> TokenPattern<T> empty() {
        return new TokenPattern<>(build -> new Fragment(null, null));
    }

    /**
     * Concatenates patterns in encounter order. An empty argument list creates
     * the empty pattern. Arguments and their order are snapshotted.
     *
     * @param <T> the token type
     * @param expressions the successive patterns
     * @return the concatenation
     * @throws NullPointerException if the array or an element is null
     * @throws IllegalArgumentException if a capture name occurs more than once
     */
    @SafeVarargs
    @SuppressWarnings("varargs") // plans reads the array and snapshots only immutable plan references
    public static <T> TokenPattern<T> sequence(TokenPattern<? super T>... expressions) {
        List<Plan> plans = plans(expressions);
        return new TokenPattern<>(build -> {
            Pattern.Node head = null;
            Pattern.Node tail = null;
            for (Plan plan : plans) {
                Fragment part = plan.build(build);
                if (part.head != null) {
                    if (tail == null) head = part.head;
                    else tail.next = part.head;
                    tail = part.tail;
                }
            }
            return new Fragment(head, tail);
        });
    }

    /**
     * Creates ordered alternatives. Earlier alternatives are attempted first;
     * a later alternative may be tried if the remainder of a match fails.
     *
     * @param <T> the token type
     * @param expressions one or more alternative patterns
     * @return the alternation
     * @throws NullPointerException if the array or an element is null
     * @throws IllegalArgumentException if there are no alternatives, or a
     *         capture name occurs more than once
     */
    @SafeVarargs
    @SuppressWarnings("varargs") // plans reads the array and snapshots only immutable plan references
    public static <T> TokenPattern<T> alternation(TokenPattern<? super T>... expressions) {
        List<Plan> plans = plans(expressions);
        if (plans.isEmpty()) throw new IllegalArgumentException("no alternatives");
        return new TokenPattern<>(build -> {
            if (plans.size() == 1) return plans.getFirst().build(build);
            Pattern.BranchConn connection = new Pattern.BranchConn();
            List<Pattern.Node> alternatives = new ArrayList<>(plans.size());
            for (Plan plan : plans) {
                Fragment part = plan.build(build);
                if (part.tail != null) part.tail.next = connection;
                alternatives.add(part.head);
            }
            Pattern.Branch branch = new Pattern.Branch(
                    alternatives.get(0), alternatives.get(1), connection);
            for (int i = 2; i < alternatives.size(); i++) branch.add(alternatives.get(i));
            return new Fragment(branch, connection);
        });
    }

    /**
     * Concatenates this pattern and another pattern.
     *
     * @param following the following pattern
     * @return the concatenation
     * @throws NullPointerException if following is null
     * @throws IllegalArgumentException if a capture name occurs more than once
     */
    public TokenPattern<T> then(TokenPattern<? super T> following) {
        return TokenPattern.<T>sequence(this, following);
    }

    /**
     * Creates an alternation with this pattern first.
     *
     * @param alternative the second alternative
     * @return the alternation
     * @throws NullPointerException if alternative is null
     * @throws IllegalArgumentException if a capture name occurs more than once
     */
    public TokenPattern<T> or(TokenPattern<? super T> alternative) {
        return TokenPattern.<T>alternation(this, alternative);
    }

    /**
     * Repeats this pattern greedily between the inclusive bounds.
     *
     * @param minimum the nonnegative minimum repetition count
     * @param maximum the maximum repetition count, at least minimum
     * @return the repetition
     * @throws IllegalArgumentException if the bounds are invalid
     */
    public TokenPattern<T> repeat(int minimum, int maximum) {
        return repeat(minimum, maximum, Quantifier.GREEDY);
    }

    /**
     * Repeats this pattern between inclusive bounds under the selected policy.
     * A maximum of {@link Integer#MAX_VALUE} is the engine's unbounded count.
     * Repeated captures retain the last participating capture according to the
     * existing JDK group semantics, including backtracking and empty matches.
     *
     * @param minimum the nonnegative minimum repetition count
     * @param maximum the maximum repetition count, at least minimum
     * @param quantifier the repetition policy
     * @return the repetition
     * @throws IllegalArgumentException if the bounds are invalid
     * @throws NullPointerException if quantifier is null
     */
    public TokenPattern<T> repeat(int minimum, int maximum, Quantifier quantifier) {
        if (minimum < 0 || maximum < minimum) {
            throw new IllegalArgumentException("invalid repetition bounds");
        }
        Objects.requireNonNull(quantifier, "quantifier");
        Plan source = plan;
        return new TokenPattern<>(build -> repeatFragment(
                source, build, minimum, maximum, quantifier));
    }

    /**
     * Returns a greedy optional repetition of this pattern.
     *
     * @return this pattern repeated zero or one times, greedily
     */
    public TokenPattern<T> optional() { return optional(Quantifier.GREEDY); }

    /**
     * Returns an optional repetition using the supplied repetition policy.
     *
     * @param quantifier the repetition policy
     * @return this pattern repeated zero or one times
     * @throws NullPointerException if quantifier is null
     */
    public TokenPattern<T> optional(Quantifier quantifier) { return repeat(0, 1, quantifier); }

    /**
     * Returns a greedy repetition of zero or more occurrences of this pattern.
     *
     * @return this pattern repeated zero or more times, greedily
     */
    public TokenPattern<T> zeroOrMore() { return zeroOrMore(Quantifier.GREEDY); }

    /**
     * Returns a repetition of zero or more occurrences using the supplied policy.
     *
     * @param quantifier the repetition policy
     * @return this pattern repeated zero or more times
     * @throws NullPointerException if quantifier is null
     */
    public TokenPattern<T> zeroOrMore(Quantifier quantifier) {
        return repeat(0, Integer.MAX_VALUE, quantifier);
    }

    /**
     * Returns a greedy repetition of one or more occurrences of this pattern.
     *
     * @return this pattern repeated one or more times, greedily
     */
    public TokenPattern<T> oneOrMore() { return oneOrMore(Quantifier.GREEDY); }

    /**
     * Returns a repetition of one or more occurrences using the supplied policy.
     *
     * @param quantifier the repetition policy
     * @return this pattern repeated one or more times
     * @throws NullPointerException if quantifier is null
     */
    public TokenPattern<T> oneOrMore(Quantifier quantifier) {
        return repeat(1, Integer.MAX_VALUE, quantifier);
    }

    /**
     * Captures this pattern's token range as a numbered group.
     *
     * @return the capturing pattern
     */
    public TokenPattern<T> capture() {
        return capturePlan(null);
    }

    /**
     * Captures this pattern's token range as a numbered and named group.
     * A name starts with an ASCII letter and contains only ASCII letters and
     * digits, as in {@link Pattern}. Names must be unique in the composed pattern.
     *
     * @param name the capture name
     * @return the capturing pattern
     * @throws NullPointerException if name is null
     * @throws IllegalArgumentException if the name is invalid or already used
     */
    public TokenPattern<T> capture(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isEmpty() || !asciiLetter(name.charAt(0))) {
            throw new IllegalArgumentException("invalid capture name: " + name);
        }
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!asciiLetter(c) && (c < '0' || c > '9')) {
                throw new IllegalArgumentException("invalid capture name: " + name);
            }
        }
        return capturePlan(name);
    }

    /**
     * Creates an independent matcher over a defensive shallow snapshot.
     * Null elements are retained; mutations to the caller's list do not change
     * the snapshot. Token objects themselves are not cloned.
     *
     * @param input the token list
     * @return the new matcher
     * @throws NullPointerException if input is null
     */
    public TokenMatcher<T> matcher(List<? extends T> input) {
        return new TokenMatcher<>(this, input);
    }

    /**
     * Returns the number of capturing groups, excluding group zero.
     *
     * @return the number of capturing groups, excluding group zero
     */
    public int groupCount() { return engine.capturingGroupCount - 1; }

    /**
     * Returns the map of capture names to their group numbers.
     *
     * @return an unmodifiable map from capture names to their group numbers
     */
    public Map<String, Integer> namedGroups() { return names; }

    Matcher newMatcher(TokenMatcher.TokenInput<T> input) {
        return new Matcher(engine, input);
    }

    private TokenPattern<T> capturePlan(String name) {
        Plan source = plan;
        return new TokenPattern<>(build -> {
            int group = build.groups++;
            int local = build.locals++;
            if (name != null && build.names.putIfAbsent(name, group) != null) {
                throw new IllegalArgumentException("duplicate capture name: " + name);
            }
            return groupFragment(source, build, local, group);
        });
    }

    private static Fragment groupFragment(Plan plan, Build build, int local, int group) {
        Pattern.GroupHead head = new Pattern.GroupHead(local);
        Pattern.GroupTail tail = new Pattern.GroupTail(local, group);
        head.tail = tail;
        Fragment child = plan.build(build);
        head.next = child.head == null ? tail : child.head;
        if (child.tail != null) child.tail.next = tail;
        return new Fragment(head, tail);
    }

    // Adapt the existing Pattern.group0 quantified-group construction so that
    // alternative bodies retain the owner engine's backtracking semantics.
    private static Fragment repeatFragment(Plan plan, Build build, int minimum,
                                           int maximum, Quantifier quantifier) {
        int local = build.locals++;
        Fragment group = groupFragment(plan, build, local, 0);
        Pattern.GroupHead head = (Pattern.GroupHead) group.head;
        Pattern.GroupTail tail = (Pattern.GroupTail) group.tail;
        Pattern.Qtype type = switch (quantifier) {
            case GREEDY -> Pattern.Qtype.GREEDY;
            case RELUCTANT -> Pattern.Qtype.LAZY;
            case POSSESSIVE -> Pattern.Qtype.POSSESSIVE;
        };
        if (type == Pattern.Qtype.POSSESSIVE) {
            Pattern.Curly curly = new Pattern.Curly(head, minimum, maximum, type);
            return new Fragment(curly, curly);
        }
        if (minimum == 0 && maximum == 1) {
            Pattern.BranchConn connection = new Pattern.BranchConn();
            tail.next = connection;
            Pattern.Branch branch = type == Pattern.Qtype.GREEDY
                    ? new Pattern.Branch(head, null, connection)
                    : new Pattern.Branch(null, head, connection);
            return new Fragment(branch, connection);
        }
        Pattern.TreeInfo info = new Pattern.TreeInfo();
        if (head.study(info)) {
            Pattern.GroupCurly curly = new Pattern.GroupCurly(
                    head.next, minimum, maximum, type, local, 0, false);
            return new Fragment(curly, curly);
        }
        int count = build.locals++;
        Pattern.Loop loop = type == Pattern.Qtype.GREEDY
                ? new Pattern.Loop(count, local) : new Pattern.LazyLoop(count, local);
        loop.cmin = minimum;
        loop.cmax = maximum;
        loop.body = head;
        tail.next = loop;
        return new Fragment(new Pattern.Prolog(loop), loop);
    }

    @SafeVarargs
    private static <T> List<Plan> plans(TokenPattern<? super T>... expressions) {
        Objects.requireNonNull(expressions, "expressions");
        List<Plan> result = new ArrayList<>(expressions.length);
        for (TokenPattern<?> expression : expressions) {
            result.add(Objects.requireNonNull(expression, "expression").plan);
        }
        return List.copyOf(result);
    }

    private static boolean asciiLetter(char value) {
        return value >= 'a' && value <= 'z' || value >= 'A' && value <= 'Z';
    }

    @FunctionalInterface
    private interface Plan {
        Fragment build(Build build);
    }

    private record Fragment(Pattern.Node head, Pattern.Node tail) {}

    private static final class Build {
        int groups = 1;
        int locals;
        final Map<String, Integer> names = new LinkedHashMap<>();
    }

    private static final class TokenNode<T> extends Pattern.Node {
        private final Predicate<? super T> predicate;

        TokenNode(Predicate<? super T> predicate) {
            this.predicate = predicate;
        }

        @Override
        @SuppressWarnings("unchecked")
        boolean match(Matcher matcher, int index, CharSequence input) {
            if (index >= matcher.to) {
                matcher.hitEnd = true;
                return false;
            }
            T value = (T) ((TokenMatcher.TokenInput<?>) input).tokenAt(index);
            return predicate.test(value) && next.match(matcher, index + 1, input);
        }

        @Override
        boolean study(Pattern.TreeInfo info) {
            info.minLength++;
            info.maxLength++;
            return next.study(info);
        }
    }
}
