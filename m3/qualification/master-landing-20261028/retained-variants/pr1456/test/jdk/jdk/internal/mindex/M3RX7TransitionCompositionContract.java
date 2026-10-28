/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary RX7 vocabulary closure and reusable transfer algebra with independent automata
 * @modules java.base/jdk.internal.mindex
 * @run main M3RX7TransitionCompositionContract
 */
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Pattern;
import jdk.internal.mindex.M3RX7;

public class M3RX7TransitionCompositionContract {
    private static final String ROOT = "7".repeat(64);
    private static long checks;
    private static long tokenRows;
    private static long oracleSequences;

    public static void main(String[] args) {
        incompleteVocabulary();
        maskBoundariesAndMutation();
        partitionWitnesses();
        randomizedNfaPartitions();
        identitiesAndBounds();
        retainedShape(M3RX7.TokenIndex.class);
        retainedShape(M3RX7.Transfer.class);
        retainedShape(M3RX7.VocabularyFilter.class);
        System.out.println("RX7 transfer contract checks=" + checks
                + " tokenRows=" + tokenRows + " independentNfaSequences=" + oracleSequences);
    }

    private static void incompleteVocabulary() {
        short[] rows = blankDfa(3);
        rows['a'] = 1;
        rows[128 + 'b'] = 2;
        M3RX7 plan = M3RX7.fromAsciiDfa(ROOT, 0, rows, new byte[] {0, 0, 1});
        M3RX7.TokenIndex prefix = packed(plan, new String[] {"a"});
        M3RX7.VocabularyFilter filter = require(prefix.prepareVocabularyFilter());
        equal(1, prefix.allowedMaskWord(0, 0), "old character-reachability mask");
        equal(0, filter.allowedMaskWord(0, 0), "incomplete vocabulary cannot finish ab");
        check(!filter.canReachFinal(0) && !filter.canReachFinal(1), "no token suffix reaches final");
        check(filter.canReachFinal(2), "empty suffix accepts at final");
        check(!filter.canReachFinal(-1) && !filter.allows(-1, 0), "dead state filter");
        expect(IndexOutOfBoundsException.class, () -> filter.allows(-1, 1));
        expect(IndexOutOfBoundsException.class, () -> filter.canReachFinal(-2));
        expect(IndexOutOfBoundsException.class, () -> filter.allowedMaskWord(-1, 0));
        expect(IndexOutOfBoundsException.class, () -> filter.allowedMaskWord(0, 1));

        M3RX7.TokenIndex complete = packed(plan, new String[] {"a", "b", "ab"});
        M3RX7.VocabularyFilter completeFilter = require(complete.prepareVocabularyFilter());
        check(completeFilter.allows(0, 0) && completeFilter.allows(0, 2), "admitted completion");
        check(completeFilter.allows(1, 1), "one token suffix");
        check(!completeFilter.allows(0, 1), "dead token excluded");
        checkMasks(complete, 3);

        M3RX7.TokenIndex empty = packed(plan, new String[0]);
        M3RX7.VocabularyFilter emptyFilter = require(empty.prepareVocabularyFilter());
        equal(0, empty.tokenCount(), "empty vocabulary token count");
        equal(0, emptyFilter.allowedMaskWords(), "empty vocabulary words");
        equal(3, emptyFilter.retainedPrimitiveBytes(), "empty vocabulary reachability bytes");
        check(!emptyFilter.canReachFinal(0) && emptyFilter.canReachFinal(2), "empty vocabulary finals only");
        expect(IndexOutOfBoundsException.class, () -> emptyFilter.allowedMaskWord(0, 0));
        expect(IndexOutOfBoundsException.class, () -> emptyFilter.allows(-1, 0));
        checkTransfer(require(empty.transfer(new int[0])), new short[] {0, 1, 2}, 0);
    }

    private static void maskBoundariesAndMutation() {
        short[] rows = blankDfa(4);
        rows['a'] = 0;
        rows['b'] = 1;
        rows['x'] = 2;
        rows[2 * 128 + 'x'] = 2;
        rows[3 * 128 + 'a'] = 1;
        M3RX7 plan = M3RX7.fromAsciiDfa(ROOT, 0, rows, new byte[] {0, 1, 0, 0});
        String[] basis = {"a", "b", "aa", "ab", "x", "ba", "aab", "z"};
        for (int count : new int[] {1, 2, 63, 64, 65, 127, 128, 129}) {
            String[] vocabulary = new String[count];
            for (int token = 0; token < count; token++) vocabulary[token] = basis[token % basis.length];
            M3RX7.TokenIndex index = packed(plan, vocabulary);
            checkRows(index, rows, vocabulary);
            checkMasks(index, 4);
        }

        byte[] bytes = {(byte) 0xFF, 'b', (byte) 0xFE, 'a', 'b', (byte) 0xFD};
        int[] offsets = {3, 1, 3, 3};
        int[] lengths = {1, 1, 2, 1};
        M3RX7.TokenIndex index = require(plan.prepareTokenIndex(bytes, offsets, lengths));
        M3RX7.VocabularyFilter filter = require(index.prepareVocabularyFilter());
        int[] ids = {0, 0, 1};
        M3RX7.Transfer transfer = require(index.transfer(ids));
        Arrays.fill(bytes, (byte) 0xFF);
        Arrays.fill(offsets, Integer.MAX_VALUE);
        Arrays.fill(lengths, -1);
        Arrays.fill(ids, 999);
        Arrays.fill(rows, (short) -1);
        check(index.matches(new int[] {3, 2}), "token preparation snapshots slices");
        check(transfer.matches(), "transfer snapshots IDs");
        check(filter.allows(0, 0), "filter remains immutable after source mutation");
        equal(12, transfer.retainedPrimitiveBytes(), "one short row plus shared acceptance byte per state");
        equal(4 + 4 * 8, filter.retainedPrimitiveBytes(), "filter primitive retention");
    }

    private static void partitionWitnesses() {
        // Test-only Thompson topology for (a|b)*abb, including epsilon and branch edges.
        Nfa nfa = new Nfa(5, 0, 1 << 4);
        nfa.epsilon[0] = (1 << 1);
        nfa.edges[1][0] = (1 << 1) | (1 << 2);
        nfa.edges[1][1] = (1 << 1);
        nfa.edges[2][1] = (1 << 3);
        nfa.edges[3][1] = (1 << 4);
        Pattern pattern = Pattern.compile("(a|b)*abb");
        short[] rows = nfa.dfa();
        M3RX7 plan = M3RX7.fromAsciiDfa(ROOT, nfa.initialMask(), rows, nfa.finals());
        String[] vocabulary = binaryVocabulary(4);
        M3RX7.TokenIndex index = packed(plan, vocabulary);
        checkRows(index, rows, vocabulary);
        checkMasks(index, 1 << nfa.states);
        for (int length = 0; length <= 7; length++) {
            for (int bits = 0; bits < (1 << length); bits++) {
                StringBuilder text = new StringBuilder(length);
                for (int i = 0; i < length; i++) text.append((bits & (1 << i)) == 0 ? 'a' : 'b');
                String value = text.toString();
                boolean accepted = nfa.matches(value);
                check(accepted == pattern.matcher(value).matches(), "NFA oracle versus Pattern");
                check(accepted == (plan.matchesCandidate(value) == 1), "DFA versus Pattern");
                partitions(index, vocabulary, value, 0, new int[length], 0, accepted);
            }
        }
    }

    private static void partitions(M3RX7.TokenIndex index, String[] vocabulary, String text,
            int offset, int[] ids, int count, boolean accepted) {
        if (offset == text.length()) {
            int[] sequence = Arrays.copyOf(ids, count);
            check(index.matches(sequence) == accepted, "all bounded token partitions");
            M3RX7.Transfer effect = require(index.transfer(sequence));
            check(effect.matches() == accepted, "partition transfer acceptance");
            equal(count, effect.length(), "partition token count");
            oracleSequences++;
            return;
        }
        for (int length = 1; length <= 4 && offset + length <= text.length(); length++) {
            String token = text.substring(offset, offset + length);
            int id = Arrays.asList(vocabulary).indexOf(token);
            check(id >= 0, "partition dictionary complete");
            ids[count] = id;
            partitions(index, vocabulary, text, offset + length, ids, count + 1, accepted);
        }
    }

    private static void randomizedNfaPartitions() {
        Random random = new Random(0x52_58_37_20261018L);
        String[] basis = binaryVocabulary(3);
        for (int fixture = 0; fixture < 48; fixture++) {
            int count = 2 + random.nextInt(5);
            Nfa nfa = new Nfa(count, random.nextInt(count), random.nextInt(1 << count));
            for (int state = 0; state < count; state++) {
                for (int target = 0; target < count; target++) {
                    if (random.nextInt(12) == 0) nfa.epsilon[state] |= 1 << target;
                    for (int letter = 0; letter < 2; letter++) {
                        if (random.nextInt(5) == 0) nfa.edges[state][letter] |= 1 << target;
                    }
                }
            }
            short[] rows = nfa.dfa();
            M3RX7 plan = M3RX7.fromAsciiDfa(ROOT, nfa.initialMask(), rows, nfa.finals());
            int vocabularyCount = fixture % 3 == 0 ? 65 : 1 + random.nextInt(19);
            String[] vocabulary = new String[vocabularyCount];
            for (int token = 0; token < vocabularyCount; token++) {
                vocabulary[token] = basis[random.nextInt(basis.length)];
            }
            M3RX7.TokenIndex index = packed(plan, vocabulary);
            checkRows(index, rows, vocabulary);
            checkMasks(index, 1 << count);
            for (int sample = 0; sample < 180; sample++) {
                int[] ids = new int[random.nextInt(13)];
                StringBuilder spelling = new StringBuilder();
                for (int i = 0; i < ids.length; i++) {
                    ids[i] = random.nextInt(vocabularyCount);
                    spelling.append(vocabulary[ids[i]]);
                }
                boolean expected = nfa.matches(spelling.toString());
                check(index.matches(ids) == expected, "independent NFA token match");
                check((plan.matchesCandidate(spelling) == 1) == expected, "independent NFA char match");
                M3RX7.Transfer whole = require(index.transfer(ids));
                check(whole.matches() == expected, "independent NFA transfer match");
                int first = random.nextInt(ids.length + 1);
                int second = first + random.nextInt(ids.length - first + 1);
                M3RX7.Transfer a = require(index.transfer(ids, 0, first));
                M3RX7.Transfer b = require(index.transfer(ids, first, second - first));
                M3RX7.Transfer c = require(index.transfer(ids, second, ids.length - second));
                sameTransfer(whole, a.then(b).then(c), 1 << count, "partition composition");
                sameTransfer(a.then(b).then(c), a.then(b.then(c)), 1 << count, "associativity");
                for (int state = 0; state < (1 << count); state++) {
                    equal(direct(rows, state, spelling), whole.nextState(state), "every transfer entry state");
                }
                int repeats = random.nextInt(8);
                M3RX7.Transfer power = whole.repeated(repeats);
                for (int state = 0; state < (1 << count); state++) {
                    int expectedState = state;
                    for (int r = 0; r < repeats && expectedState >= 0; r++) {
                        expectedState = direct(rows, expectedState, spelling);
                    }
                    equal(expectedState, power.nextState(state), "repetition versus direct DFA");
                }
                equal((long) ids.length * repeats, power.length(), "repeat token count");
                oracleSequences++;
            }
        }
    }

    private static void identitiesAndBounds() {
        short[] rows = blankDfa(3);
        rows['a'] = 1;
        rows[128 + 'a'] = 2;
        rows[256 + 'a'] = 0;
        byte[] finals = {1, 0, 0};
        M3RX7 plan = M3RX7.fromAsciiDfa(ROOT, 0, rows, finals);
        M3RX7.TokenIndex index = packed(plan, new String[] {"a", "aa", "b"});
        M3RX7.Transfer empty = require(index.transfer(new int[0]));
        M3RX7.Transfer a = require(index.transfer(new int[] {0}));
        checkTransfer(empty, new short[] {0, 1, 2}, 0);
        sameTransfer(a, empty.then(a), 3, "left identity");
        sameTransfer(a, a.then(empty), 3, "right identity");
        sameTransfer(empty, a.repeated(0), 3, "zeroth power identity");
        sameTransfer(a, a.repeated(1), 3, "first power");
        check(a.repeated(3).matches(), "cycle repetition");
        M3RX7.Transfer huge = a.repeated(Long.MAX_VALUE);
        equal(Long.MAX_VALUE, huge.length(), "nonexpanding long repetition");
        equal(Long.MAX_VALUE % 3, huge.nextState(0), "long power cycle");
        expect(ArithmeticException.class, () -> huge.then(a));
        expect(ArithmeticException.class, () -> a.then(a).repeated(Long.MAX_VALUE));
        expect(IllegalArgumentException.class, () -> a.repeated(-1));
        expect(NullPointerException.class, () -> a.then(null));
        equal(-1, a.nextState(-1), "absorbing transfer dead state");
        expect(IndexOutOfBoundsException.class, () -> a.nextState(-2));
        expect(IndexOutOfBoundsException.class, () -> a.nextState(3));

        M3RX7 sameRoot = M3RX7.fromAsciiDfa(ROOT, 0, rows, finals);
        M3RX7.Transfer foreign = require(packed(sameRoot, new String[] {"a"}).transfer(new int[] {0}));
        expect(IllegalArgumentException.class, () -> a.then(foreign));
        M3RX7.Transfer otherVocabulary = require(packed(plan, new String[] {"aa"}).transfer(new int[] {0}));
        sameTransfer(a.repeated(3), a.then(otherVocabulary), 3, "same owner different vocabulary");
        M3RX7.Transfer dead = require(index.transfer(new int[] {2}));
        sameTransfer(dead, dead.then(a), 3, "dead prefix absorbing");
        expect(IndexOutOfBoundsException.class, () -> index.transfer(new int[] {2, 3}));
        expect(IndexOutOfBoundsException.class, () -> index.transfer(new int[] {2, -1}));
        expect(NullPointerException.class, () -> index.transfer(null));
        expect(IndexOutOfBoundsException.class, () -> index.transfer(new int[1], -1, 1));
        expect(IndexOutOfBoundsException.class, () -> index.transfer(new int[1], 0, -1));
        expect(IndexOutOfBoundsException.class, () -> index.transfer(new int[1], 1, 1));
        expect(IndexOutOfBoundsException.class, () -> index.transfer(new int[1], Integer.MAX_VALUE, 1));
        sameTransfer(a, require(index.transfer(new int[] {999, 0, -999}, 1, 1)), 3, "selected coordinates only");
        sameTransfer(empty, require(index.transfer(new int[] {999}, 1, 0)), 3, "empty range at end");

        short[] maximal = blankDfa(4096);
        M3RX7 large = M3RX7.fromAsciiDfa(ROOT, 0, maximal, new byte[4096]);
        M3RX7.TokenIndex bounded = packed(large, new String[] {"a"});
        check(bounded.transfer(new int[8192]) != null, "inclusive transfer work limit");
        check(bounded.transfer(new int[8193]) == null, "transfer work refusal");
        int[] invalidBeyondBudget = new int[8193];
        invalidBeyondBudget[8192] = 1;
        expect(IndexOutOfBoundsException.class, () -> bounded.transfer(invalidBeyondBudget));
    }

    private static void checkRows(M3RX7.TokenIndex index, short[] dfa, String[] vocabulary) {
        int states = dfa.length / 128;
        for (int state = 0; state < states; state++) {
            for (int token = 0; token < vocabulary.length; token++) {
                equal(direct(dfa, state, vocabulary[token]), index.nextState(state, token), "token delta-star");
                tokenRows++;
            }
        }
    }

    private static void retainedShape(Class<?> owner) {
        for (var field : owner.getDeclaredFields()) {
            Class<?> type = field.getType();
            check(type.isPrimitive() || type == short[].class || type == byte[].class || type == long[].class,
                    "prepared metadata retains no original RX7 plan, text, source ID array or reference collection: "
                            + owner.getSimpleName() + "." + field.getName());
        }
    }

    private static void checkMasks(M3RX7.TokenIndex index, int states) {
        M3RX7.VocabularyFilter filter = require(index.prepareVocabularyFilter());
        boolean[] useful = new boolean[states];
        for (int state = 0; state < states; state++) useful[state] = index.isFinal(state);
        boolean changed;
        do {
            changed = false;
            for (int state = 0; state < states; state++) {
                for (int token = 0; !useful[state] && token < index.tokenCount(); token++) {
                    int target = index.nextState(state, token);
                    if (target >= 0 && useful[target]) { useful[state] = true; changed = true; }
                }
            }
        } while (changed);
        equal(index.tokenCount(), filter.tokenCount(), "filter vocabulary cardinality");
        equal(index.allowedMaskWords(), filter.allowedMaskWords(), "filter word cardinality");
        for (int state = 0; state < states; state++) {
            check(filter.canReachFinal(state) == useful[state], "Boolean fixed point reachability");
            for (int token = 0; token < index.tokenCount(); token++) {
                int target = index.nextState(state, token);
                check(filter.allows(state, token) == (target >= 0 && useful[target]), "exact vocabulary mask");
                if (filter.allows(state, token)) {
                    check((index.allowedMaskWord(state, token >>> 6) & (1L << (token & 63))) != 0,
                            "exact mask contained in old character mask");
                }
            }
            int remainder = index.tokenCount() & 63;
            if (remainder != 0) {
                equal(0, filter.allowedMaskWord(state, filter.allowedMaskWords() - 1) >>> remainder,
                        "unused mask bits zero");
            }
        }
    }

    private static M3RX7.TokenIndex packed(M3RX7 plan, String[] vocabulary) {
        int size = 1;
        for (String token : vocabulary) size += token.length() + 1;
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, (byte) 0xFF); // Unselected gaps are deliberately non-ASCII.
        int[] offsets = new int[vocabulary.length];
        int[] lengths = new int[vocabulary.length];
        int at = 1;
        for (int token = vocabulary.length - 1; token >= 0; token--) {
            String value = vocabulary[token];
            offsets[token] = at;
            lengths[token] = value.length();
            for (int unit = 0; unit < value.length(); unit++) bytes[at++] = (byte) value.charAt(unit);
            at++;
        }
        return require(plan.prepareTokenIndex(bytes, offsets, lengths));
    }

    private static String[] binaryVocabulary(int maxLength) {
        String[] tokens = new String[(1 << (maxLength + 1)) - 2];
        int token = 0;
        for (int length = 1; length <= maxLength; length++) {
            for (int bits = 0; bits < (1 << length); bits++) {
                StringBuilder value = new StringBuilder(length);
                for (int i = 0; i < length; i++) value.append((bits & (1 << i)) == 0 ? 'a' : 'b');
                tokens[token++] = value.toString();
            }
        }
        return tokens;
    }

    private static short[] blankDfa(int states) {
        short[] result = new short[states * 128];
        Arrays.fill(result, (short) -1);
        return result;
    }

    private static int direct(short[] dfa, int state, CharSequence text) {
        for (int at = 0; at < text.length() && state >= 0; at++) state = dfa[state * 128 + text.charAt(at)];
        return state;
    }

    private static void checkTransfer(M3RX7.Transfer effect, short[] rows, long length) {
        equal(length, effect.length(), "transfer length");
        for (int state = 0; state < rows.length; state++) equal(rows[state], effect.nextState(state), "transfer row");
    }

    private static void sameTransfer(M3RX7.Transfer left, M3RX7.Transfer right, int states, String label) {
        // Token counts may differ when two vocabularies spell the same text.
        check(left.matches() == right.matches(), label + " acceptance");
        for (int state = -1; state < states; state++) equal(left.nextState(state), right.nextState(state), label);
    }

    private static <T> T require(T value) {
        check(value != null, "small fixture preparation must fit resource budget");
        return value;
    }

    private static void equal(long expected, long actual, String label) {
        check(expected == actual, label + ": expected=" + expected + " actual=" + actual);
    }

    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    /** Independent test-only NFA; runtime matching uses boolean sets, DFA construction uses bits. */
    private static final class Nfa {
        final int states, start, accepting;
        final int[] epsilon;
        final int[][] edges;

        Nfa(int states, int start, int accepting) {
            this.states = states;
            this.start = start;
            this.accepting = accepting;
            epsilon = new int[states];
            edges = new int[states][2];
        }

        boolean matches(String text) {
            boolean[] active = new boolean[states];
            active[start] = true;
            close(active);
            for (int at = 0; at < text.length(); at++) {
                boolean[] next = new boolean[states];
                int letter = text.charAt(at) - 'a';
                for (int state = 0; state < states; state++) {
                    for (int target = 0; active[state] && target < states; target++) {
                        if ((edges[state][letter] & (1 << target)) != 0) next[target] = true;
                    }
                }
                close(next);
                active = next;
            }
            for (int state = 0; state < states; state++) {
                if (active[state] && (accepting & (1 << state)) != 0) return true;
            }
            return false;
        }

        private void close(boolean[] active) {
            boolean changed;
            do {
                changed = false;
                for (int state = 0; state < states; state++) {
                    for (int target = 0; active[state] && target < states; target++) {
                        if (!active[target] && (epsilon[state] & (1 << target)) != 0) {
                            active[target] = true;
                            changed = true;
                        }
                    }
                }
            } while (changed);
        }

        int initialMask() { return closeBits(1 << start); }

        private int closeBits(int mask) {
            int previous;
            do {
                previous = mask;
                for (int state = 0; state < states; state++) {
                    if ((mask & (1 << state)) != 0) mask |= epsilon[state];
                }
            } while (mask != previous);
            return mask;
        }

        short[] dfa() {
            short[] rows = blankDfa(1 << states);
            for (int mask = 0; mask < (1 << states); mask++) {
                int closed = closeBits(mask);
                for (int letter = 0; letter < 2; letter++) {
                    int targets = 0;
                    for (int state = 0; state < states; state++) {
                        if ((closed & (1 << state)) != 0) targets |= edges[state][letter];
                    }
                    targets = closeBits(targets);
                    rows[mask * 128 + 'a' + letter] = targets == 0 ? -1 : (short) targets;
                }
            }
            return rows;
        }

        byte[] finals() {
            byte[] flags = new byte[1 << states];
            for (int mask = 0; mask < flags.length; mask++) {
                flags[mask] = (byte) ((closeBits(mask) & accepting) == 0 ? 0 : 1);
            }
            return flags;
        }
    }
}
