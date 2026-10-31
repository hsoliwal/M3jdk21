// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.function.LongPredicate;

/** Count transcript proof over actual array owners; no substituted production dependencies. */
public final class M3PrimitiveSequenceCountProof {
    private static final StringBuilder TAPE = new StringBuilder();
    private static long checks;

    private M3PrimitiveSequenceCountProof() { }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail + " check=" + checks);
    }

    private static final class Observation {
        final StringBuilder events = new StringBuilder();
        final RuntimeException marker = new RuntimeException("count source or predicate marker");
        final char failurePhase;
        final int failureOrdinal;
        int iterators;
        int otherSourceCalls;
        int predicateCalls;

        Observation(char failurePhase, int failureOrdinal) {
            this.failurePhase = failurePhase;
            this.failureOrdinal = failureOrdinal;
        }

        void failIf(char phase, int ordinal) {
            if (failurePhase == phase && failureOrdinal == ordinal) throw marker;
        }
    }

    /** Forward every operation to the actual owner, observing its real public iterator. */
    private static <T extends M3LongCollection> T observe(
            Class<T> contract, T owner, Observation observation) {
        return contract.cast(Proxy.newProxyInstance(contract.getClassLoader(),
                new Class<?>[]{contract}, (proxy, method, arguments) -> {
                    boolean traversal = method.getName().equals("iterator")
                            && method.getParameterCount() == 0;
                    observation.events.append('S').append(method.getName()).append(';');
                    if (traversal) observation.iterators++;
                    else observation.otherSourceCalls++;
                    Object result;
                    try {
                        result = method.invoke(owner, arguments);
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                    if (!traversal) return result;
                    M3LongIterator cursor = (M3LongIterator) result;
                    return new M3LongIterator() {
                        private int ordinal;

                        @Override public boolean hasNext() {
                            observation.events.append('H').append(ordinal).append(';');
                            observation.failIf('H', ordinal);
                            return cursor.hasNext();
                        }

                        @Override public long nextLong() {
                            observation.events.append('N').append(ordinal).append(';');
                            observation.failIf('N', ordinal);
                            long value = cursor.nextLong();
                            ordinal++;
                            return value;
                        }
                    };
                }));
    }

    private static boolean accepted(long value, int mode) {
        return switch (mode) {
            case 0 -> false;
            case 1 -> true;
            case 2 -> (value & 1L) == 0;
            default -> value < 0;
        };
    }

    private static boolean second(long value) { return value % 3L != 0; }

    private static int expected(long[] values, int mode, boolean nested) {
        int count = 0;
        for (long value : values) {
            if (accepted(value, mode) && (!nested || second(value))) count++;
        }
        return count;
    }

    private static void runCase(long[] values, int kind, boolean readOnly,
                                boolean nested, int mode, char failurePhase, int failureOrdinal) {
        Observation observation = new Observation(failurePhase, failureOrdinal);
        LongPredicate primary = value -> {
            int ordinal = observation.predicateCalls++;
            observation.events.append('P').append(ordinal).append('=').append(value).append(';');
            observation.failIf('P', ordinal);
            return accepted(value, mode);
        };
        LongPredicate secondary = value -> {
            observation.events.append('Q').append(value).append(';');
            return second(value);
        };
        LongPredicate last = nested ? secondary : primary;
        M3LongCollection view;
        Runnable append;
        if (kind == 0 || kind == 3) {
            M3LongArrayList owner = new M3LongArrayList();
            owner.addAll(values);
            append = () -> owner.add(2L);
            M3LongList source = observe(M3LongList.class, owner, observation);
            if (nested) source = readOnly ? M3PrimitiveFilteredSequences.readOnlyList(source, primary)
                    : M3PrimitiveFilteredSequences.list(source, primary);
            if (kind == 0) view = readOnly ? M3PrimitiveFilteredSequences.readOnlyList(source, last)
                    : M3PrimitiveFilteredSequences.list(source, last);
            else view = readOnly ? M3PrimitiveFilteredSequences.readOnlyStack(source, last)
                    : M3PrimitiveFilteredSequences.stack(source, last);
        } else {
            M3LongArrayDeque owner = new M3LongArrayDeque(4);
            for (long value : values) owner.addLast(value);
            append = () -> owner.addLast(2L);
            M3LongDeque source = observe(M3LongDeque.class, owner, observation);
            if (nested) source = readOnly ? M3PrimitiveFilteredSequences.readOnlyDeque(source, primary)
                    : M3PrimitiveFilteredSequences.deque(source, primary);
            if (kind == 1) view = readOnly ? M3PrimitiveFilteredSequences.readOnlyDeque(source, last)
                    : M3PrimitiveFilteredSequences.deque(source, last);
            else view = readOnly ? M3PrimitiveFilteredSequences.readOnlyQueue(source, last)
                    : M3PrimitiveFilteredSequences.queue(source, last);
        }
        check(observation.events.length() == 0, "view construction must stay lazy");
        TAPE.append("\ncase:").append(values.length).append(':').append(kind).append(':')
                .append(readOnly).append(':').append(nested).append(':').append(mode).append(':')
                .append(failurePhase).append(':').append(failureOrdinal).append(';');
        if (failurePhase == '-') {
            int expectedCount = expected(values, mode, nested);
            check(view.size() == expectedCount, "first live count");
            observation.events.append("end-first;");
            check(view.size() == expectedCount, "repeated count must remain live");
            observation.events.append("end-repeat;");
            append.run();
            if (accepted(2L, mode) && (!nested || second(2L))) expectedCount++;
            check(view.size() == expectedCount, "count after backing mutation");
            check(observation.iterators == 3, "one real backing iterator per count");
        } else {
            try {
                view.size();
                throw new AssertionError("expected count failure was lost");
            } catch (RuntimeException caught) {
                check(caught == observation.marker, "source or predicate failure identity");
            }
            check(observation.iterators == 1, "one real backing iterator on failure");
        }
        check(observation.otherSourceCalls == 0, "count must not query size, get, or materialize source");
        TAPE.append(observation.events);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        long[] boundary = new long[129];
        for (int i = 0; i < boundary.length; i++) boundary[i] = i % 17 - 8;
        long[][] inputs = {{}, {0}, {Long.MIN_VALUE, Long.MAX_VALUE, -2, -1, 0, 1, 2, 2}, boundary};
        for (int kind = 0; kind < 4; kind++) {
            for (boolean readOnly : new boolean[]{false, true}) {
                for (boolean nested : new boolean[]{false, true}) {
                    for (long[] values : inputs) {
                        for (int mode = 0; mode < 4; mode++) {
                            runCase(values, kind, readOnly, nested, mode, '-', -1);
                        }
                    }
                    for (char phase : new char[]{'H', 'N', 'P'}) {
                        for (int ordinal : new int[]{0, 3, 7}) {
                            runCase(inputs[2], kind, readOnly, nested, 2, phase, ordinal);
                        }
                    }
                    runCase(inputs[2], kind, readOnly, nested, 2, 'H', inputs[2].length);
                }
            }
        }
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(TAPE.toString().getBytes(StandardCharsets.UTF_8)));
        System.out.println("PRIMITIVE_SEQUENCE_COUNT_PASS checks=" + checks
                + " tape_sha256=" + digest + " tape_chars=" + TAPE.length());
    }
}
