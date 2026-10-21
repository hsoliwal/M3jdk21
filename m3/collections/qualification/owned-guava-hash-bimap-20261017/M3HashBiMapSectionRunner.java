/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.guava;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Executes selected unchanged strict groups from the frozen comprehensive test.
 * Reflection accesses only this test's methods/counters, never production state.
 * Root binds actual provider bytes/classpath and passes its artifact label.
 * No exception, result, identity, trace or invariant mismatch is permitted here.
 */
public final class M3HashBiMapSectionRunner {
    private static int groups;

    private M3HashBiMapSectionRunner() { }

    private record Counts(long checks, long operations, long cases, long donorCases, long randomOperations) {
        Counts minus(Counts before) {
            return new Counts(checks - before.checks, operations - before.operations, cases - before.cases,
                    donorCases - before.donorCases, randomOperations - before.randomOperations);
        }
    }

    private static long count(String name) throws ReflectiveOperationException {
        Field field = M3HashBiMapSelfTest.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getLong(null);
    }

    private static Counts counts() throws ReflectiveOperationException {
        return new Counts(count("checks"), count("operations"), count("cases"), count("donorCases"), count("randomOperations"));
    }

    private static void invoke(String name, Class<?>[] signature, Object[] arguments) throws Exception {
        Method method = M3HashBiMapSelfTest.class.getDeclaredMethod(name, signature);
        method.setAccessible(true);
        try { method.invoke(null, arguments); }
        catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw new AssertionError("unexpected callback throwable", cause);
        }
    }

    private static void group(String name) throws Exception {
        Counts before = counts();
        System.out.println("BEGIN unchangedGroup=" + name);
        invoke(name, new Class<?>[0], new Object[0]);
        groups++;
        System.out.println("PASS unchangedGroup=" + name + " delta=" + counts().minus(before));
    }

    private static void serialization(boolean candidate, boolean inverseFirst) throws Exception {
        Counts before = counts();
        String label = "serializationGraph candidate=" + candidate + " inverseFirst=" + inverseFirst;
        System.out.println("BEGIN unchangedGroup=" + label);
        invoke("serializationGraph", new Class<?>[] {boolean.class, boolean.class}, new Object[] {candidate, inverseFirst});
        groups++;
        System.out.println("PASS unchangedGroup=" + label + " delta=" + counts().minus(before));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("profile and actual provider artifact label are required");
        String profile = args[0]; String runtime = args[1];
        Field label = M3HashBiMapSelfTest.class.getDeclaredField("runtimeVariant");
        label.setAccessible(true); label.set(null, runtime);
        System.out.println("BEGIN M3HashBiMapSectionRunner profile=" + profile + " runtime=" + runtime + " strict=true");
        switch (profile) {
            case "android-valid" -> {
                group("factoriesAndRepresentatives");
                group("donorThirteen");
                group("viewMutationsAndIteratorStates");
                group("arraysStreamsAndSetters");
                group("mapDefaultsAndCallbacks");
                serialization(false, false); serialization(true, false);
                serialization(false, true); serialization(true, true);
            }
            case "jre-common" -> {
                group("forcePutPositionsAndNulls");
                group("denseGrowthAndDeletion");
            }
            default -> throw new IllegalArgumentException("unknown strict profile: " + profile);
        }
        System.out.println("PASS M3HashBiMapSectionRunner profile=" + profile + " runtime=" + runtime
                + " unchangedGroups=" + groups + " totals=" + counts() + " strict=true"
                + " originalWholeSelfTest=NOT_CLAIMED originalRandomTrace=NOT_EXECUTED"
                + " wireIdentity=NOT_CLAIMED performance=NOT_MEASURED");
    }
}
