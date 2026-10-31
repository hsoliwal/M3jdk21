/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.LongPredicate;

/** Protected public-API oracle; its backing payload is the actual M3LongArrayDeque. */
public final class M3FilteredDequeDispatchProof {
    private int checks;

    private M3FilteredDequeDispatchProof() { }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("witness-first")) {
            witness(true);
        } else if (args.length == 1 && args[0].equals("witness-last")) {
            witness(false);
        } else if (args.length == 0) {
            System.out.println("FILTERED_DEQUE_DISPATCH checks=" + runContract()
                    + " traces=400 stepsPerTrace=80");
        } else {
            throw new IllegalArgumentException("expected no args, witness-first or witness-last");
        }
    }

    private static void witness(boolean first) {
        StringBuilder tape = new StringBuilder();
        TapDeque owner = new TapDeque(tape);
        owner.offerAccepted = false;
        owner.addFailure = new IllegalStateException("capacity refused");
        M3LongDeque view = M3PrimitiveFilteredSequences.deque(owner, value -> true);
        Throwable actual = thrown(() -> insert(view, first, false, 42L));
        if (actual != owner.addFailure) {
            throw new AssertionError((first ? "addFirst" : "addLast")
                    + " must invoke throwing owner; actual=" + actual + "; tape=" + tape);
        }
    }

    public static int runContract() {
        M3FilteredDequeDispatchProof proof = new M3FilteredDequeDispatchProof();
        proof.dispatchAndEffects();
        proof.admissionAndReadonly();
        proof.nestedFilters();
        proof.arrayOwnerWorkloads();
        return proof.checks;
    }

    private void dispatchAndEffects() {
        long[] values = {Long.MIN_VALUE, Long.MAX_VALUE, -1L, 0L, 1L, 42L};
        for (boolean first : new boolean[] {true, false}) {
            for (long value : values) {
                for (boolean offer : new boolean[] {false, true}) {
                    StringBuilder tape = new StringBuilder();
                    TapDeque owner = new TapDeque(tape);
                    M3LongDeque view = accepting(owner, tape, "P;");
                    check(insert(view, first, offer, value), "accepted insertion");
                    check(tape.toString().equals("P;" + operation(first, offer)), "matching dispatch");
                    equal(inserted(owner.initial, first, value), owner.toArray(), "accepted payload");
                }
                for (boolean partial : new boolean[] {false, true}) {
                    for (Throwable failure : new Throwable[] {
                            new IllegalStateException("owner refusal"), new AssertionError("owner error")}) {
                        StringBuilder tape = new StringBuilder();
                        TapDeque owner = new TapDeque(tape);
                        owner.addFailure = failure;
                        owner.partial = partial;
                        owner.offerAccepted = false;
                        M3LongDeque view = accepting(owner, tape, "P;");
                        check(thrown(() -> insert(view, first, false, value)) == failure,
                                "backing failure identity");
                        check(tape.toString().equals("P;" + operation(first, false)), "failure dispatch");
                        equal(partial ? inserted(owner.initial, first, value) : owner.initial,
                                owner.toArray(), "partial owner effects");
                    }
                }
                StringBuilder tape = new StringBuilder();
                TapDeque owner = new TapDeque(tape);
                owner.offerAccepted = false;
                owner.addFailure = new IllegalStateException("must not invoke add");
                check(!insert(accepting(owner, tape, "P;"), first, true, value), "false offer retained");
                check(tape.toString().equals("P;" + operation(first, true)), "offer dispatch retained");
                equal(owner.initial, owner.toArray(), "refused offer payload");
            }
        }
    }

    private void admissionAndReadonly() {
        for (boolean first : new boolean[] {true, false}) {
            for (boolean offer : new boolean[] {false, true}) {
                StringBuilder tape = new StringBuilder();
                TapDeque owner = new TapDeque(tape);
                M3LongDeque readonly = M3PrimitiveFilteredSequences.readOnlyDeque(owner, value -> {
                    tape.append("P;");
                    throw new AssertionError("readonly filter evaluated");
                });
                Throwable refused = thrown(() -> insert(readonly, first, offer, 2L));
                check(refused instanceof UnsupportedOperationException, "readonly refusal type");
                check("read-only filtered collection".equals(refused.getMessage()), "readonly message");
                check(tape.length() == 0, "readonly precedes predicate and owner");
                equal(owner.initial, owner.toArray(), "readonly payload");
                M3LongDeque rejected = M3PrimitiveFilteredSequences.deque(owner, value -> {
                    tape.append("P;");
                    return false;
                });
                Throwable denied = thrown(() -> insert(rejected, first, offer, 3L));
                check(denied instanceof IllegalArgumentException, "filter rejection type");
                check("value rejected by filtered collection".equals(denied.getMessage()), "filter message");
                check(tape.toString().equals("P;"), "rejection callback once");
                equal(owner.initial, owner.toArray(), "rejection payload");
                for (Throwable failure : new Throwable[] {
                        new IllegalArgumentException("predicate failure"), new AssertionError("predicate error")}) {
                    tape.setLength(0);
                    M3LongDeque throwing = M3PrimitiveFilteredSequences.deque(owner, value -> {
                        tape.append("P;");
                        raise(failure);
                        return true;
                    });
                    check(thrown(() -> insert(throwing, first, offer, 2L)) == failure,
                            "predicate failure identity");
                    check(tape.toString().equals("P;"), "predicate failure precedes owner");
                    equal(owner.initial, owner.toArray(), "predicate failure payload");
                }
            }
        }
    }

    private void nestedFilters() {
        for (boolean first : new boolean[] {true, false}) {
            for (boolean offer : new boolean[] {false, true}) {
                StringBuilder tape = new StringBuilder();
                TapDeque owner = new TapDeque(tape);
                M3LongDeque inner = accepting(owner, tape, "inner;");
                M3LongDeque outer = accepting(inner, tape, "outer;");
                check(insert(outer, first, offer, 2L), "nested insertion");
                check(tape.toString().equals("outer;inner;" + operation(first, offer)), "nested order");
                equal(inserted(owner.initial, first, 2L), owner.toArray(), "nested effects");
                tape.setLength(0);
                M3LongDeque innerReadonly = M3PrimitiveFilteredSequences.readOnlyDeque(owner, value -> {
                    tape.append("forbidden;");
                    return true;
                });
                M3LongDeque nestedReadonly = accepting(innerReadonly, tape, "outer;");
                check(thrown(() -> insert(nestedReadonly, first, offer, 2L))
                        instanceof UnsupportedOperationException, "inner readonly refusal");
                check(tape.toString().equals("outer;"), "inner readonly effect order");
                tape.setLength(0);
                M3LongDeque innerReject = M3PrimitiveFilteredSequences.deque(owner, value -> {
                    tape.append("inner;");
                    return false;
                });
                M3LongDeque nestedReject = accepting(innerReject, tape, "outer;");
                check(thrown(() -> insert(nestedReject, first, offer, 3L))
                        instanceof IllegalArgumentException, "inner rejection");
                check(tape.toString().equals("outer;inner;"), "nested rejection order");
            }
        }
    }

    private void arrayOwnerWorkloads() {
        for (int seed = 0; seed < 400; seed++) {
            Random random = new Random(0x6d337f00L + seed);
            M3LongArrayDeque owner = new M3LongArrayDeque(seed % 17);
            M3LongDeque view = M3PrimitiveFilteredSequences.deque(owner, value -> (value & 1L) == 0L);
            ArrayDeque<Long> model = new ArrayDeque<>();
            for (int step = 0; step < 80; step++) {
                long even = step % 29 == 0 ? Long.MIN_VALUE : (random.nextInt(9) - 4L) * 2L;
                switch (random.nextInt(10)) {
                    case 0 -> { view.addFirst(even); model.addFirst(even); }
                    case 1 -> { view.addLast(even); model.addLast(even); }
                    case 2 -> { check(view.offerFirst(even), "array offer first"); model.addFirst(even); }
                    case 3 -> { check(view.offerLast(even), "array offer last"); model.addLast(even); }
                    case 4 -> { owner.addFirst(even | 1L); model.addFirst(even | 1L); }
                    case 5 -> { owner.addLast(Long.MAX_VALUE); model.addLast(Long.MAX_VALUE); }
                    case 6 -> {
                        if (!model.isEmpty()) check(owner.removeFirstLong() == model.removeFirst(),
                                "external array removal");
                    }
                    case 7, 8 -> {
                        boolean first = (step & 1) == 0;
                        Long expected = removeVisible(model, first);
                        if (expected == null) {
                            check(thrown(() -> remove(view, first)) instanceof NoSuchElementException,
                                    "empty visible removal");
                        } else {
                            check(remove(view, first) == expected.longValue(), "visible duplicate removal");
                        }
                    }
                    case 9 -> { view.clear(); model.removeIf(value -> (value & 1L) == 0L); }
                    default -> throw new AssertionError("unreachable operation");
                }
                equal(model.stream().mapToLong(Long::longValue).toArray(), owner.toArray(), "live owner");
                equal(model.stream().mapToLong(Long::longValue).filter(value -> (value & 1L) == 0L).toArray(),
                        view.toArray(), "visible order and multiplicity");
            }
        }
    }

    private static Long removeVisible(ArrayDeque<Long> model, boolean first) {
        Iterator<Long> cursor = first ? model.iterator() : model.descendingIterator();
        while (cursor.hasNext()) {
            long value = cursor.next();
            if ((value & 1L) == 0L) { cursor.remove(); return value; }
        }
        return null;
    }

    private static long remove(M3LongDeque view, boolean first) {
        return first ? view.removeFirstLong() : view.removeLastLong();
    }

    private static M3LongDeque accepting(M3LongDeque owner, StringBuilder tape, String label) {
        return M3PrimitiveFilteredSequences.deque(owner, value -> { tape.append(label); return true; });
    }

    private static boolean insert(M3LongDeque view, boolean first, boolean offer, long value) {
        if (offer) return first ? view.offerFirst(value) : view.offerLast(value);
        if (first) view.addFirst(value); else view.addLast(value);
        return true;
    }

    private static String operation(boolean first, boolean offer) {
        return (offer ? "O" : "A") + (first ? "F;" : "L;");
    }

    private static long[] inserted(long[] before, boolean first, long value) {
        long[] after = new long[before.length + 1];
        System.arraycopy(before, 0, after, first ? 1 : 0, before.length);
        after[first ? 0 : before.length] = value;
        return after;
    }

    private void equal(long[] expected, long[] actual, String label) {
        check(Arrays.equals(expected, actual), label + " expected=" + Arrays.toString(expected)
                + " actual=" + Arrays.toString(actual));
    }

    private void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static Throwable thrown(Runnable action) {
        try { action.run(); return null; }
        catch (Throwable failure) { return failure; }
    }

    private static void raise(Throwable failure) {
        if (failure instanceof RuntimeException runtime) throw runtime;
        throw (Error) failure;
    }

    private static final class TapDeque implements M3LongDeque {
        private final StringBuilder tape;
        private final M3LongArrayDeque payload = new M3LongArrayDeque();
        private final long[] initial = {-3L, 2L, 7L, 2L};
        private boolean offerAccepted = true;
        private boolean partial;
        private Throwable addFailure;

        TapDeque(StringBuilder tape) {
            this.tape = tape;
            for (long value : initial) payload.addLast(value);
        }

        private void add(boolean first, long value) {
            tape.append(operation(first, false));
            if (addFailure == null || partial) {
                if (first) payload.addFirst(value); else payload.addLast(value);
            }
            if (addFailure != null) raise(addFailure);
        }

        private boolean offer(boolean first, long value) {
            tape.append(operation(first, true));
            if (!offerAccepted) return false;
            if (first) payload.addFirst(value); else payload.addLast(value);
            return true;
        }

        @Override public void addFirst(long value) { add(true, value); }
        @Override public void addLast(long value) { add(false, value); }
        @Override public boolean offerFirst(long value) { return offer(true, value); }
        @Override public boolean offerLast(long value) { return offer(false, value); }
        @Override public boolean offer(long value) { return offerLast(value); }
        @Override public boolean addLastIf(long value, LongPredicate predicate) {
            return predicate.test(value) && offerLast(value);
        }
        @Override public int size() { return payload.size(); }
        @Override public int capacity() { return payload.capacity(); }
        @Override public boolean contains(long value) { return payload.contains(value); }
        @Override public void clear() { payload.clear(); }
        @Override public M3LongIterator iterator() { return payload.iterator(); }
        @Override public M3LongIterator descendingIterator() { return payload.descendingIterator(); }
        @Override public long[] toArray() { return payload.toArray(); }
        @Override public long get(int index) { return payload.get(index); }
        @Override public long getFirst() { return payload.getFirst(); }
        @Override public long getLast() { return payload.getLast(); }
        @Override public long removeFirstLong() { return payload.removeFirstLong(); }
        @Override public long removeLastLong() { return payload.removeLastLong(); }
        @Override public boolean removeFirstOccurrence(long value) { return payload.removeFirstOccurrence(value); }
        @Override public boolean removeLastOccurrence(long value) { return payload.removeLastOccurrence(value); }
        @Override public int filterInPlace(LongPredicate predicate) { return payload.filterInPlace(predicate); }
        @Override public long peek() { return payload.peek(); }
        @Override public long poll() { return payload.poll(); }
    }
}
