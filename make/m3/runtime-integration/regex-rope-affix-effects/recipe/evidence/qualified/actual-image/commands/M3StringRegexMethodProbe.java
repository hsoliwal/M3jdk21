/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Runs explicitly selected methods from the unchanged, SDK-emitted jtreg class. */
public final class M3StringRegexMethodProbe {
    public static void main(String[] arguments) throws Exception {
        Class<?> target = Class.forName("M3StringRegexDpTest");
        for (String name : arguments) {
            Method method = target.getDeclaredMethod(name);
            method.setAccessible(true);
            try {
                method.invoke(null);
            } catch (InvocationTargetException failure) {
                Throwable cause = failure.getCause();
                if (cause instanceof Exception exception) throw exception;
                if (cause instanceof Error error) throw error;
                throw failure;
            }
            System.out.println("M3_REGEX_ACTUAL_IMAGE_METHOD_PASS method=" + name);
        }
        var checks = target.getDeclaredField("checks");
        checks.setAccessible(true);
        System.out.println("M3_REGEX_ACTUAL_IMAGE_SELECTED_METHODS_PASS checks="
                + checks.getLong(null) + " methods=" + arguments.length
                + " java_home=" + System.getProperty("java.home"));
    }
}
