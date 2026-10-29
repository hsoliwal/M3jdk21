/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  This particular file is
 * subject to the "Classpath" exception as provided in the LICENSE file
 * that accompanied this code.
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
 */
package java.util.regex;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Stateful token-index matching for a {@link TokenPattern}.
 *
 * <p>The input list is defensively shallow-copied when the matcher is created
 * or reset with a new input. Null elements are allowed. Captured lists and
 * {@link #input()} are unmodifiable views of that private snapshot. Such a
 * captured list remains associated with its original snapshot after subsequent
 * matcher operations or resets. The referenced token objects are not cloned.
 *
 * <p>Search progress, region handling, and successful capture selection follow
 * {@link Matcher}. A match becomes unavailable whenever a matching operation
 * returns false, including an exhausted empty match at the region end.
 * All positions count tokens, with starts inclusive and ends
 * exclusive. A nonparticipating group has start and end {@code -1} and a null
 * captured list or span. Accessing a group before a successful matching operation
 * or after a failed one throws {@link IllegalStateException}. Invalid numbered
 * groups throw {@link IndexOutOfBoundsException}; unknown names throw
 * {@link IllegalArgumentException}.
 *
 * <p>A matcher is not safe for concurrent use. Predicates must not reenter or
 * mutate their active matcher. Predicate exceptions propagate; reset the matcher
 * before using it again after an exceptional matching operation.
 *
 * @param <T> the token type
 * @since 21
 */
public final class TokenMatcher<T> {
    private final TokenPattern<T> pattern;
    private final Matcher engine;
    private TokenInput<T> input;
    private boolean matched;

    TokenMatcher(TokenPattern<T> pattern, List<? extends T> tokens) {
        this.pattern = pattern;
        input = TokenInput.snapshot(tokens);
        engine = pattern.newMatcher(input);
    }

    /**
     * Returns the immutable token pattern used by this matcher.
     *
     * @return the immutable token pattern used by this matcher
     */
    public TokenPattern<T> pattern() { return pattern; }

    /**
     * Returns an unmodifiable view of the current defensive input snapshot.
     *
     * @return an unmodifiable view of the current defensive input snapshot
     */
    public List<T> input() { return input.tokens; }

    /**
     * Attempts to match the entire region against the pattern.
     *
     * @return whether the entire region matches the pattern
     */
    public boolean matches() {
        matched = false;
        return matched = engine.matches();
    }

    /**
     * Attempts to match a prefix of the region against the pattern.
     *
     * @return whether a prefix of the region matches the pattern
     */
    public boolean lookingAt() {
        matched = false;
        return matched = engine.lookingAt();
    }

    /**
     * Finds the next nonoverlapping match. After an empty match, the next search
     * advances by one token; the empty boundary at the region end is tried once.
     *
     * @return whether another match was found
     */
    public boolean find() {
        matched = false;
        return matched = engine.find();
    }

    /**
     * Resets this matcher, restores the full input region, and searches from
     * the supplied token position, as {@link Matcher#find(int)} does.
     *
     * @param start the search start, from zero through the input size
     * @return whether a match was found
     * @throws IndexOutOfBoundsException if start is outside the input
     */
    public boolean find(int start) {
        Objects.checkFromToIndex(start, start, input.length());
        matched = false;
        return matched = engine.find(start);
    }

    /**
     * Discards match state and restores the full region over the same snapshot.
     *
     * @return this matcher
     */
    public TokenMatcher<T> reset() {
        engine.reset();
        matched = false;
        return this;
    }

    /**
     * Resets this matcher over a new defensive shallow snapshot. Snapshot
     * creation completes before the current matcher state changes.
     *
     * @param tokens the new token list
     * @return this matcher
     * @throws NullPointerException if tokens is null
     */
    public TokenMatcher<T> reset(List<? extends T> tokens) {
        TokenInput<T> replacement = TokenInput.snapshot(tokens);
        engine.reset(replacement);
        input = replacement;
        matched = false;
        return this;
    }

    /**
     * Sets the matching region and discards previous match state. Positions
     * remain absolute indices into the input snapshot.
     *
     * @param start the inclusive region start
     * @param end the exclusive region end
     * @return this matcher
     * @throws IndexOutOfBoundsException if the region is invalid
     */
    public TokenMatcher<T> region(int start, int end) {
        engine.region(start, end);
        matched = false;
        return this;
    }

    /**
     * Returns the inclusive start of the matching region.
     *
     * @return the inclusive region start
     */
    public int regionStart() { return engine.regionStart(); }

    /**
     * Returns the exclusive end of the matching region.
     *
     * @return the exclusive region end
     */
    public int regionEnd() { return engine.regionEnd(); }

    /**
     * Reports whether the last matching operation succeeded.
     *
     * @return whether the last matching operation succeeded
     */
    public boolean hasMatch() { return matched; }

    /**
     * Returns the number of capturing groups, excluding group zero.
     *
     * @return the number of capturing groups, excluding group zero
     */
    public int groupCount() { return engine.groupCount(); }

    /**
     * Returns the map of capture names to group numbers.
     *
     * @return the unmodifiable map of capture names to group numbers
     */
    public Map<String, Integer> namedGroups() { return pattern.namedGroups(); }

    /**
     * Returns the inclusive start of group zero.
     *
     * @return the inclusive start of group zero
     */
    public int start() {
        requireMatch();
        return engine.start();
    }

    /**
     * Returns the exclusive end of group zero.
     *
     * @return the exclusive end of group zero
     */
    public int end() {
        requireMatch();
        return engine.end();
    }

    /**
     * Returns the inclusive start of a numbered group.
     *
     * @param group a group number, including zero
     * @return its inclusive start, or -1 if it did not participate
     */
    public int start(int group) {
        requireMatch();
        return engine.start(group);
    }

    /**
     * Returns the exclusive end of a numbered group.
     *
     * @param group a group number, including zero
     * @return its exclusive end, or -1 if it did not participate
     */
    public int end(int group) {
        requireMatch();
        return engine.end(group);
    }

    /**
     * Returns the inclusive start of a named group.
     *
     * @param name the capture name
     * @return its inclusive start, or -1 if it did not participate
     * @throws NullPointerException if name is null
     */
    public int start(String name) {
        Objects.requireNonNull(name, "name");
        requireMatch();
        return engine.start(name);
    }

    /**
     * Returns the exclusive end of a named group.
     *
     * @param name the capture name
     * @return its exclusive end, or -1 if it did not participate
     * @throws NullPointerException if name is null
     */
    public int end(String name) {
        Objects.requireNonNull(name, "name");
        requireMatch();
        return engine.end(name);
    }

    /**
     * Returns the captured tokens for group zero.
     *
     * @return an unmodifiable captured list for group zero
     */
    public List<T> group() { return group(0); }

    /**
     * Returns the captured tokens for a numbered group.
     *
     * @param group a group number, including zero
     * @return an unmodifiable captured list, or null if the group did not participate
     */
    public List<T> group(int group) {
        int start = start(group);
        return start < 0 ? null : input.tokens.subList(start, end(group));
    }

    /**
     * Returns the captured tokens for a named group.
     *
     * @param name the capture name
     * @return an unmodifiable captured list, or null if the group did not participate
     * @throws NullPointerException if name is null
     */
    public List<T> group(String name) {
        int start = start(name);
        return start < 0 ? null : input.tokens.subList(start, end(name));
    }

    /**
     * Returns the captured span for group zero.
     *
     * @return an immutable span for group zero
     */
    public Span span() { return span(0); }

    /**
     * Returns the captured span for a numbered group.
     *
     * @param group a group number, including zero
     * @return an immutable span, or null if the group did not participate
     */
    public Span span(int group) {
        int start = start(group);
        return start < 0 ? null : new Span(start, end(group));
    }

    /**
     * Returns the captured span for a named group.
     *
     * @param name the capture name
     * @return an immutable span, or null if the group did not participate
     * @throws NullPointerException if name is null
     */
    public Span span(String name) {
        int start = start(name);
        return start < 0 ? null : new Span(start, end(name));
    }

    private void requireMatch() {
        if (!matched) throw new IllegalStateException("No match available");
    }

    /**
     * An immutable half-open range of token indices.
     *
     * @param start the inclusive, nonnegative start
     * @param end the exclusive end, at least start
     */
    public record Span(int start, int end) {
        /**
         * Creates a validated token span.
         *
         * @throws IllegalArgumentException if the bounds are invalid
         */
        public Span {
            if (start < 0 || end < start) throw new IllegalArgumentException("invalid token span");
        }
    }

    /** One non-surrogate carrier unit per token; never a token's spelling. */
    static final class TokenInput<T> implements CharSequence {
        final List<T> tokens;

        private TokenInput(List<T> tokens) {
            this.tokens = tokens;
        }

        @SuppressWarnings("unchecked")
        static <T> TokenInput<T> snapshot(List<? extends T> tokens) {
            Object[] supplied = Objects.requireNonNull(tokens, "tokens").toArray();
            Object[] snapshot = Arrays.copyOf(supplied, supplied.length, Object[].class);
            List<T> view = (List<T>) (List<?>) Arrays.asList(snapshot);
            return new TokenInput<>(Collections.unmodifiableList(view));
        }

        T tokenAt(int index) { return tokens.get(index); }

        @Override public int length() { return tokens.size(); }

        @Override public char charAt(int index) {
            Objects.checkIndex(index, tokens.size());
            return '\u0001';
        }

        @Override public CharSequence subSequence(int start, int end) {
            Objects.checkFromToIndex(start, end, tokens.size());
            return new TokenInput<>(tokens.subList(start, end));
        }

        @Override public String toString() { return "\u0001".repeat(tokens.size()); }
    }
}
