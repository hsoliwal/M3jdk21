/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

/** Exact observable indexed-iterator transcripts, shared by predecessor and successor. */
public final class M3ReadonlyIndexedIteratorSelfTest {
    private static long checks;
    private static final long SEED = 0x4d33524f49545221L;

    private M3ReadonlyIndexedIteratorSelfTest() { }

    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }

    private static final class Trace {
        final StringBuilder events = new StringBuilder();
        final RuntimeException failure;
        final char category;
        final int at;
        int count;
        boolean injected;

        Trace(char category, int at, boolean bounds) {
            this.category = category;
            this.at = at;
            failure = bounds ? new IndexOutOfBoundsException("same-bounds-marker")
                    : new IllegalStateException("same-callback-marker");
        }

        void event(char kind, Object value) {
            events.append(kind).append('(').append(value).append(");");
            if (kind == category && ++count == at) {
                injected = true;
                throw failure;
            }
        }

        void result(Object result) {
            events.append('=').append(result).append(';');
        }

        void exception(RuntimeException problem) {
            events.append('!').append(problem.getClass().getName()).append(':')
                    .append(problem.getMessage()).append(':').append(problem == failure);
            Throwable cause = problem.getCause();
            if (cause != null) events.append('/').append(cause.getClass().getName())
                    .append(':').append(cause.getMessage()).append(':').append(cause == failure);
            events.append(';');
        }
    }

    private static final class ObservedList extends AbstractList<Integer> {
        final List<Integer> values = new ArrayList<>(Arrays.asList(-3, 2, null, 7, 4, 4));
        final Trace trace;

        ObservedList(Trace trace) { this.trace = trace; }

        @Override public Integer get(int index) {
            trace.event('g', index);
            return values.get(index);
        }

        @Override public int size() {
            trace.event('s', values.size());
            return values.size();
        }

        void change(int operation) {
            if ((operation & 1) == 0) values.add(operation);
            else if (!values.isEmpty()) values.remove(0);
            modCount++;
        }
    }

    private static String scenario(int number) {
        char category = new char[] {'p', 'g', 's', 'a'}[number & 3];
        Trace trace = new Trace(category, 1 + number % 19, (number & 4) != 0);
        ObservedList owner = new ObservedList(trace);
        Predicate<Integer> predicate = value -> {
            trace.event('p', value);
            return value == null || (value & 1) == 0;
        };
        List<Integer> view = M3FilteredLists.readOnlyFiltered(owner, predicate);
        if ((number & 8) != 0) {
            view = M3FilteredLists.readOnlyFiltered(view, value -> {
                trace.event('q', value);
                return value == null || value >= 0;
            });
        }
        Iterator<Integer> iterator = view.iterator();
        Random random = new Random(SEED + number);
        for (int step = 0; step < 32; step++) {
            int operation = step < 4 ? step : random.nextInt(7);
            trace.events.append('[').append(operation).append(']');
            try {
                switch (operation) {
                    case 0 -> trace.result(iterator.hasNext());
                    case 1 -> trace.result(iterator.next());
                    case 2 -> iterator.remove();
                    case 3 -> {
                        iterator.forEachRemaining(value -> trace.event('a', value));
                        trace.result("drained");
                    }
                    case 4 -> {
                        owner.change(step);
                        trace.result("owner-changed");
                    }
                    case 5 -> {
                        iterator = view.iterator();
                        trace.result("new-iterator");
                    }
                    case 6 -> iterator.forEachRemaining(null);
                    default -> throw new AssertionError();
                }
            } catch (RuntimeException failure) {
                trace.exception(failure);
            }
        }
        check(trace.injected || trace.count < trace.at, "one-shot callback failure");
        return number + ":" + trace.events + "\n";
    }

    private static void readonlyPolicyAndFailureIdentity() {
        for (List<Integer> source : List.of(new ArrayList<Integer>(), new ArrayList<>(List.of(1, 2, 3)))) {
            List<Integer> view = M3FilteredLists.readOnlyFiltered(source, value -> (value & 1) == 0);
            Iterator<Integer> cursor = view.iterator();
            for (int phase = 0; phase < 3; phase++) {
                try {
                    cursor.remove();
                    throw new AssertionError("readonly remove");
                } catch (UnsupportedOperationException expected) {
                    check(true, "readonly remove always refuses");
                }
                if (cursor.hasNext()) check(Objects.equals(cursor.next(), 2), "visible value");
            }
            cursor.forEachRemaining(value -> { throw new AssertionError("already exhausted"); });
        }
        RuntimeException sentinel = new IndexOutOfBoundsException("bounds from predicate");
        Iterator<Integer> cursor = M3FilteredLists.readOnlyFiltered(List.of(1), value -> {
            throw sentinel;
        }).iterator();
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                cursor.next();
                throw new AssertionError("callback must throw");
            } catch (NoSuchElementException expected) {
                check(expected.getCause() == sentinel, "same wrapped callback, retry preserved");
            }
        }
        List<Integer> source = new ArrayList<>(List.of(1, 2, 3, 4));
        List<Integer> view = M3FilteredLists.readOnlyFiltered(source, value -> (value & 1) == 0);
        check(new ArrayList<>(view).equals(List.of(2, 4)), "encounter order");
        source.add(6);
        check(new ArrayList<>(view).equals(List.of(2, 4, 6)), "live indexed source");
    }

    private static void reentrantCursorAndErrorIdentity() {
        final class State {
            Iterator<Integer> cursor;
            boolean entered;
        }
        State state = new State();
        state.cursor = M3FilteredLists.readOnlyFiltered(List.of(1, 2, 3), value -> {
            if (!state.entered) {
                state.entered = true;
                check(state.cursor.next() == 1, "reentrant first indexed read");
                check(state.cursor.next() == 2, "reentrant second indexed read");
            }
            return true;
        }).iterator();
        check(state.cursor.next() == 1, "outer read retains captured ordinal");
        check(state.cursor.next() == 2, "outer completion restores captured successor");
        check(state.cursor.next() == 3, "remaining encounter order");
        check(!state.cursor.hasNext(), "reentrant exhaustion");

        AssertionError marker = new AssertionError("opaque error identity");
        boolean[] first = {true};
        Iterator<Integer> cursor = M3FilteredLists.readOnlyFiltered(List.of(1), value -> {
            if (first[0]) {
                first[0] = false;
                throw marker;
            }
            return true;
        }).iterator();
        try {
            cursor.next();
            throw new AssertionError("predicate Error must escape");
        } catch (AssertionError failure) {
            check(failure == marker, "exact Error instance");
        }
        check(cursor.next() == 1, "Error leaves current ordinal unconsumed");
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        if (args.length > 1 || args.length == 1 && !args[0].equals("--tapes")) {
            throw new IllegalArgumentException("no argument or --tapes");
        }
        checks = 0;
        readonlyPolicyAndFailureIdentity();
        reentrantCursorAndErrorIdentity();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int scenario = 0; scenario < 512; scenario++) {
            String tape = scenario(scenario);
            digest.update(tape.getBytes(StandardCharsets.UTF_8));
            if (args.length == 1) System.out.print(tape);
        }
        System.out.println("M3_READONLY_INDEXED_ITERATOR_PASS checks=" + checks
                + " scenarios=512 seed=" + SEED + " transcript="
                + HexFormat.of().formatHex(digest.digest()));
    }
}
