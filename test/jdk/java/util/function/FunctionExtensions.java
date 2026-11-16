/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Check extended and exception-typed functional interface contracts
 * @run main FunctionExtensions
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ext.BooleanUnaryOperator;
import java.util.function.ext.ByteComparator;
import java.util.function.ext.ByteConsumer;
import java.util.function.ext.ByteUnaryOperator;
import java.util.function.ext.CharUnaryOperator;
import java.util.function.ext.FloatComparator;
import java.util.function.ext.FloatPredicate;
import java.util.function.ext.FloatUnaryOperator;
import java.util.function.ext.Function16;
import java.util.function.ext.IntByteConsumer;
import java.util.function.ext.IntLongToDoubleFunction;
import java.util.function.ext.ObjIntFunction;
import java.util.function.ext.ShortUnaryOperator;
import java.util.function.ext.TriFunction;

public class FunctionExtensions {
    private static int checks;

    public static void main(String[] args) throws Throwable {
        consumers();
        predicates();
        functions();
        primitiveEdges();
        comparators();
        checkedFunctions();
        checkedFailures();
        System.out.println("FunctionExtensions: " + checks + " checks passed");
    }

    private static void consumers() throws Throwable {
        List<String> trace = new ArrayList<>();
        ByteConsumer first = value -> trace.add("a" + value);
        ByteConsumer second = value -> trace.add("b" + value);
        ByteConsumer combined = first.andThen(second);
        equal(List.of(), trace);
        combined.accept(Byte.MIN_VALUE);
        equal(List.of("a-128", "b-128"), trace);
        trace.clear();
        expect(NullPointerException.class, () -> first.andThen(null));
        equal(List.of(), trace);
        RuntimeException failure = new RuntimeException("first consumer");
        ByteConsumer broken = value -> { throw failure; };
        same(failure, expect(RuntimeException.class,
                () -> broken.andThen(second).accept((byte) 1)));
        equal(List.of(), trace);
        int[] received = new int[2];
        IntByteConsumer indexed = (index, value) -> {
            received[0] = index;
            received[1] = value;
        };
        indexed.accept(Integer.MIN_VALUE, Byte.MAX_VALUE);
        equal(Integer.MIN_VALUE, received[0]);
        equal(Byte.MAX_VALUE, (byte) received[1]);
    }

    private static void predicates() throws Throwable {
        int[] rightCalls = {0};
        FloatPredicate yes = value -> true;
        FloatPredicate no = value -> false;
        FloatPredicate right = value -> { rightCalls[0]++; return true; };
        check(!no.and(right).test(Float.NaN));
        check(yes.or(right).test(-0.0f));
        equal(0, rightCalls[0]);
        check(yes.and(right).test(Float.POSITIVE_INFINITY));
        check(no.or(right).test(Float.NEGATIVE_INFINITY));
        equal(2, rightCalls[0]);
        check(!yes.negate().test(0));
        check(no.negate().test(0));
        expect(NullPointerException.class, () -> no.and(null));
        expect(NullPointerException.class, () -> yes.or(null));
        equal(2, rightCalls[0]);
        RuntimeException failure = new RuntimeException("first predicate");
        FloatPredicate broken = value -> { throw failure; };
        same(failure, expect(RuntimeException.class,
                () -> broken.or(right).test(1)));
        equal(2, rightCalls[0]);
    }

    private static void functions() throws Throwable {
        Object marker = new Object();
        int[] captured = {7};
        ObjIntFunction<Object, Object> operation = (value, index) -> {
            same(marker, value);
            equal(captured[0], index);
            return value;
        };
        same(marker, operation.andThen(value -> value).apply(marker, 7));
        captured[0] = 9;
        same(marker, operation.apply(marker, 9));
        expect(NullPointerException.class, () -> operation.andThen(null));
        ObjIntFunction<Object, Object> nullable = (value, index) -> value;
        same(marker, nullable.andThen(value -> value == null ? marker : null)
                .apply(null, -1));
        TriFunction<String, String, String, String> join =
                (first, second, third) -> first + second + third;
        equal("abc", join.apply("a", "b", "c"));
        equal(3, join.andThen(String::length).apply("a", "b", "c"));
        Function16<Integer, Integer, Integer, Integer, Integer, Integer,
                Integer, Integer, Integer, Integer, Integer, Integer,
                Integer, Integer, Integer, Integer, String> all =
                (a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p) ->
                        "" + a + b + c + d + e + f + g + h
                                + i + j + k + l + m + n + o + p;
        equal("12345678910111213141516",
                all.apply(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16));
        IntLongToDoubleFunction mixed = (first, second) -> {
            equal(Integer.MIN_VALUE, first);
            equal(Long.MAX_VALUE, second);
            return -0.0d;
        };
        equal(Double.doubleToRawLongBits(-0.0d), Double.doubleToRawLongBits(
                mixed.applyAsDouble(Integer.MIN_VALUE, Long.MAX_VALUE)));
    }

    private static void primitiveEdges() throws Throwable {
        ByteUnaryOperator bytes = ByteUnaryOperator.identity();
        equal(Byte.MIN_VALUE, bytes.applyAsByte(Byte.MIN_VALUE));
        equal(Byte.MAX_VALUE, bytes.applyAsByte(Byte.MAX_VALUE));
        ShortUnaryOperator shorts = ShortUnaryOperator.identity();
        equal(Short.MIN_VALUE, shorts.applyAsShort(Short.MIN_VALUE));
        equal(Short.MAX_VALUE, shorts.applyAsShort(Short.MAX_VALUE));
        CharUnaryOperator chars = CharUnaryOperator.identity();
        for (char value : new char[] {0, '\ud800', '\udfff', '\uffff'}) {
            equal(value, chars.applyAsChar(value));
        }
        BooleanUnaryOperator booleans = BooleanUnaryOperator.identity();
        check(booleans.applyAsBoolean(true));
        check(!booleans.applyAsBoolean(false));
        FloatUnaryOperator floats = FloatUnaryOperator.identity();
        for (float value : new float[] {-0.0f, 0.0f, Float.MIN_VALUE,
                Float.MAX_VALUE, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY}) {
            equal(Float.floatToRawIntBits(value),
                    Float.floatToRawIntBits(floats.applyAsFloat(value)));
        }
        check(Float.isNaN(floats.applyAsFloat(Float.NaN)));
        List<String> trace = new ArrayList<>();
        ByteUnaryOperator increment = value -> {
            trace.add("increment");
            return (byte) (value + 1);
        };
        ByteUnaryOperator twice = value -> {
            trace.add("twice");
            return (byte) (value * 2);
        };
        equal((byte) 8, increment.andThen(twice).applyAsByte((byte) 3));
        equal(List.of("increment", "twice"), trace);
        trace.clear();
        equal((byte) 7, increment.compose(twice).applyAsByte((byte) 3));
        equal(List.of("twice", "increment"), trace);
        expect(NullPointerException.class, () -> increment.compose(null));
        expect(NullPointerException.class, () -> increment.andThen(null));
    }

    private static void comparators() throws Throwable {
        ByteComparator natural = ByteComparator.naturalOrder();
        check(natural.compare(Byte.MIN_VALUE, Byte.MAX_VALUE) < 0);
        ByteComparator extreme = (left, right) -> left == right ? 0
                : left < right ? Integer.MIN_VALUE : 1;
        equal(1, extreme.reversed().compare((byte) 1, (byte) 2));
        equal(Integer.MIN_VALUE, extreme.reversed().compare((byte) 2, (byte) 1));
        int[] ties = {0};
        ByteComparator tie = (left, right) -> { ties[0]++; return 17; };
        equal(1, extreme.thenComparing(tie).compare((byte) 2, (byte) 1));
        equal(0, ties[0]);
        equal(17, extreme.thenComparing(tie).compare((byte) 1, (byte) 1));
        equal(1, ties[0]);
        expect(NullPointerException.class, () -> extreme.thenComparing(null));
        equal(1, ties[0]);
        FloatComparator floating = FloatComparator.naturalOrder();
        check(floating.compare(-0.0f, 0.0f) < 0);
        check(floating.compare(Float.NaN, Float.POSITIVE_INFINITY) > 0);
        equal(0, floating.compare(Float.NaN, Float.NaN));
        java.util.function.checked.IntComparator<IOException> throwing =
                (left, right) -> left == right ? 0
                        : left < right ? Integer.MIN_VALUE : 1;
        equal(1, throwing.reversed().compare(1, 2));
        equal(Integer.MIN_VALUE, throwing.reversed().compare(2, 1));
    }

    private static void checkedFunctions() throws Throwable {
        List<String> trace = new ArrayList<>();
        java.util.function.checked.Function<String, Integer, IOException> length =
                value -> { trace.add("length"); return value.length(); };
        java.util.function.checked.Function<Object, String, IOException> before =
                value -> { trace.add("before"); return value.toString(); };
        java.util.function.checked.Function<Integer, String, IOException> after =
                value -> { trace.add("after"); return "size=" + value; };
        equal("size=3", length.compose(before).andThen(after).apply("abc"));
        equal(List.of("before", "length", "after"), trace);
        trace.clear();
        expect(NullPointerException.class, () -> length.compose(null));
        expect(NullPointerException.class, () -> length.andThen(null));
        equal(List.of(), trace);
        IOException failure = new IOException("checked");
        java.util.function.checked.Function<String, Integer, IOException> broken =
                value -> { throw failure; };
        same(failure, expect(IOException.class,
                () -> broken.andThen(after).apply("abc")));
        equal(List.of(), trace);
        java.util.function.checked.BytePredicate<IOException> no = value -> false;
        java.util.function.checked.BytePredicate<IOException> fail = value -> {
            throw failure;
        };
        check(!no.and(fail).test((byte) 0));
        check(no.negate().or(fail).test((byte) 0));
        same(failure, expect(IOException.class, () -> no.or(fail).test((byte) 0)));
    }

    private static void checkedFailures() throws Throwable {
        Signal signal = new Signal();
        java.util.function.checked.Supplier<Object, Signal> supplier = () -> {
            throw signal;
        };
        same(signal, expect(Signal.class, supplier::get));
        AssertionError error = new AssertionError("unchanged error");
        java.util.function.checked.Runnable<IOException> task = () -> { throw error; };
        same(error, expect(AssertionError.class, task::run));
        boolean originalInterrupt = Thread.interrupted();
        try {
            InterruptedException interrupted = new InterruptedException("identity");
            java.util.function.checked.Runnable<InterruptedException> operation =
                    () -> { throw interrupted; };
            same(interrupted, expect(InterruptedException.class, operation::run));
            check(!Thread.currentThread().isInterrupted());
            Thread.currentThread().interrupt();
            same(interrupted, expect(InterruptedException.class, operation::run));
            check(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            if (originalInterrupt) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class Signal extends Throwable {
        private static final long serialVersionUID = 1L;
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Throwable;
    }

    private static Throwable expect(Class<? extends Throwable> type, Action action)
            throws Throwable {
        try {
            action.run();
        } catch (Throwable caught) {
            check(type.isInstance(caught));
            return caught;
        }
        throw new AssertionError("Expected " + type.getName());
    }

    private static void same(Object expected, Object actual) {
        check(expected == actual);
    }

    private static void equal(Object expected, Object actual) {
        check(java.util.Objects.equals(expected, actual));
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("Check " + checks + " failed");
        }
    }
}
