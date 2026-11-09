/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

/**
 * Focused original-owner cancellation/progress/memory correctness proof for the
 * existing typed range and postfix signal implementations; no mocking signal arrays.
 */
public final class SignalExecutionPressureSelfTest {
    private static final long VERSION = 42;
    private static final String SHA = "d".repeat(64);
    private static long checks;
    private SignalExecutionPressureSelfTest() { }

    private static void check(boolean condition, String why) {
        checks++;
        if (!condition) throw new AssertionError(why + " assertion=" + checks);
    }

    private static void refuse(Class<? extends Throwable> kind, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable caught) {
            if (kind.isInstance(caught)) return;
            throw new AssertionError("wrong exception instead of " + kind.getName(), caught);
        }
        throw new AssertionError("missing exception " + kind.getName());
    }

    /** Observe the existing SPI without coupling tests to a particular monitor impl. */
    private static final class Probe implements InvocationHandler {
        boolean canceled;
        int begin;
        int worked;
        int done;
        int total;
        int checkCount;
        int cancelAfterChecks = Integer.MAX_VALUE;
        int cancelAfterWork = Integer.MAX_VALUE;
        Runnable afterWork;

        ProgressMonitor monitor() {
            return (ProgressMonitor) Proxy.newProxyInstance(
                    ProgressMonitor.class.getClassLoader(),
                    new Class<?>[]{ProgressMonitor.class}, this);
        }

        @Override public Object invoke(Object target, Method method, Object[] args) {
            switch (method.getName()) {
                case "checkCanceled" -> {
                    if (++checkCount >= cancelAfterChecks || canceled) {
                        throw new CancellationException("M3 signal cancelled");
                    }
                    return null;
                }
                case "isCanceled" -> { return canceled; }
                case "beginTask" -> {
                    begin++;
                    total = Math.toIntExact((long) args[1]);
                    return null;
                }
                case "worked" -> {
                    worked += Math.toIntExact((long) args[0]);
                    if (afterWork != null) afterWork.run();
                    if (worked >= cancelAfterWork) canceled = true;
                    return null;
                }
                case "done" -> { done++; return null; }
                case "toString" -> { return "M3 test progress"; }
                default -> throw new AssertionError("unexpected SPI method " + method.getName());
            }
        }
    }

    private static SignalQualitySchema schema(int n) {
        int[] numbers = new int[n];
        for (int i = 0; i < n; i++) numbers[i] = i % 23;
        SignalColumns lanes = new SignalColumns(n,
                new SignalColumns.Column(SignalColumns.Kind.INTEGER,
                        SignalColumns.Origin.STATIC, 1, false,
                        SignalColumns.allValid(n), numbers, null, null));
        SignalRelations graph = SignalRelations.of(n,
                new int[0], new int[0], new byte[0], new int[0]);
        var freshness = SignalDependencyEpochs.from(graph, false);
        return SignalQualitySchema.declared(lanes, freshness, VERSION, SHA,
                new SignalQualitySchema.Spec("source.number",
                        SignalQualitySchema.Family.STRUCTURAL,
                        SignalQualitySchema.Phase.COMPILER,
                        SignalQualitySchema.Reliability.EXACT_FACT,
                        SignalQualitySchema.Invalidation.RECORDED_DEPENDENCIES,
                        "number", "M3SourceSignals/javac21", Double.NaN, ""));
    }

    private static SignalBooleanQuery.Term term(int lo, int hi) {
        return new SignalBooleanQuery.Term(0,
                new SignalBooleanQuery.IntBetween(lo, hi));
    }

    private static void simpleProgressAndCompatibility() {
        SignalQualitySchema facts = schema(129);
        var query = SignalBooleanQuery.build(facts, 0);
        var compiled = query.compile(term(5, 15), term(8, 12), SignalBooleanQuery.Logic.AND);
        int[] baseline = compiled.evaluate(VERSION, SHA);
        Probe progress = new Probe();
        check(Arrays.equals(compiled.evaluate(VERSION, SHA, progress.monitor()), baseline),
                "progress overload preserves the old original result");
        check(progress.begin == 1 && progress.total == compiled.instructionCount(),
                "original M3 progress records the correct task work estimate");
        check(progress.worked == compiled.instructionCount() && progress.done == 1,
                "credits only completed instructions then marks done exactly once");
        check(Arrays.equals(query.and(VERSION, SHA,
                new SignalBooleanQuery.IntBetween(5, 15)),
                query.compile(term(5, 15)).evaluate(VERSION, SHA)),
                "existing flat-query API matches the fused predicate result");
        check(Arrays.equals(query.compile(SignalBooleanQuery.Logic.NONE,
                SignalBooleanQuery.Logic.NOT).evaluate(VERSION, SHA),
                query.compile(SignalBooleanQuery.Logic.ALL).evaluate(VERSION, SHA)),
                "NOT masks absent last-word rows");
        refuse(IllegalStateException.class, () -> compiled.evaluate(VERSION + 1, SHA,
                new Probe().monitor()));
    }

    private static void cancellationAndMutationRefusal() {
        SignalQualitySchema facts = schema(130);
        var compiled = SignalBooleanQuery.build(facts, 0)
                .compile(term(0, 22), SignalBooleanQuery.Logic.NOT);
        Probe canceled = new Probe();
        canceled.canceled = true;
        refuse(CancellationException.class, () -> compiled.evaluate(VERSION, SHA, canceled.monitor()));
        check(canceled.begin == 0 && canceled.done == 0, "pre-cancel refuses before starting task");

        Probe oneInstruction = new Probe();
        oneInstruction.cancelAfterWork = 1;
        refuse(CancellationException.class, () ->
                compiled.evaluate(VERSION, SHA, oneInstruction.monitor()));
        check(oneInstruction.worked == 1 && oneInstruction.done == 0,
                "cancel after first instruction never publishes partial result or done");

        Probe reentrantMutation = new Probe();
        reentrantMutation.afterWork = facts.invalidationEpochsOwner()::invalidateAll;
        refuse(IllegalStateException.class, () ->
                compiled.evaluate(VERSION, SHA, reentrantMutation.monitor()));
        check(reentrantMutation.done == 0,
                "progress callback invalidation fails closed rather than completing");
        refuse(IllegalStateException.class, () -> compiled.evaluate(VERSION, SHA));
    }

    private static void largeScanCheckpoints() {
        int n = 131_085;
        var facts = schema(n);
        var plan = SignalBooleanQuery.build(facts, 0);
        var compiled = plan.compile(term(0, 22));
        Probe canceledDuringMark = new Probe();
        canceledDuringMark.cancelAfterChecks = 9;
        refuse(CancellationException.class, () ->
                compiled.evaluate(VERSION, SHA, canceledDuringMark.monitor()));
        check(canceledDuringMark.worked == 0 && canceledDuringMark.done == 0,
                "cancel during bounded primitive row scan before completing instruction");

        Probe normal = new Probe();
        int[] expected = compiled.evaluate(VERSION, SHA);
        int[] result = compiled.evaluate(VERSION, SHA, normal.monitor());
        check(Arrays.equals(result, expected) && result.length == n,
                "fused primitive mask retains every physical row without int[] predicate copy");
        check(normal.begin == 1 && normal.done == 1 && normal.worked == 1,
                "large primitive scan completes progress precisely");
        Probe cancelDuringBitLoop = new Probe();
        cancelDuringBitLoop.cancelAfterChecks = 5;
        var invert = plan.compile(SignalBooleanQuery.Logic.ALL, SignalBooleanQuery.Logic.NOT);
        refuse(CancellationException.class, () ->
                invert.evaluate(VERSION, SHA, cancelDuringBitLoop.monitor()));
        check(cancelDuringBitLoop.done == 0,
                "cancellation interrupts long primitive complement without delivering results");
    }

    public static void main(String[] args) {
        simpleProgressAndCompatibility();
        cancellationAndMutationRefusal();
        largeScanCheckpoints();
        System.out.println("M3_SIGNAL_EXECUTION_PRESSURE_PASS checks=" + checks);
    }
}
