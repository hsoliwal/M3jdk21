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
package com.synexia.proof;

import com.m3.arrays.M3ArrayNative;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.Arrays;

/** Independent boundary contract; production array allocation must happen in JNI. */
public final class M3BackingArrayContractV2 {
    public static final IllegalStateException MARKER = new IllegalStateException("JNI allocation marker");
    private static int checks;
    private static final Class<?>[] TYPES = {Object[].class, byte[].class, int[].class, long[].class};
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static long collections() {
        return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(bean -> Math.max(0L, bean.getCollectionCount())).sum();
    }
    private static void directNativeNegative(int type, int length) throws Exception {
        String[] names = {"nativeAllocateObjects", "nativeAllocateBytes", "nativeAllocateInts", "nativeAllocateLongs"};
        var method = M3ArrayNative.class.getDeclaredMethod(names[type], int.class);
        method.setAccessible(true);
        Throwable observed = null;
        try { method.invoke(null, length); }
        catch (java.lang.reflect.InvocationTargetException wrapped) { observed = wrapped.getCause(); }
        check(observed != null && observed.getClass() == NegativeArraySizeException.class,
                "direct native negative type=" + type + " length=" + length);
    }
    private static Object allocate(int type, int length) {
        return switch (type) {
            case 0 -> M3ArrayNative.allocateObjectsRequired(length);
            case 1 -> M3ArrayNative.allocateBytesRequired(length);
            case 2 -> M3ArrayNative.allocateIntsRequired(length);
            default -> M3ArrayNative.allocateLongsRequired(length);
        };
    }
    private static void failure(int type, int length, Class<? extends Throwable> expected) {
        Throwable observed = null;
        try { allocate(type, length); }
        catch (Throwable error) { observed = error; }
        check(observed != null && observed.getClass() == expected,
                "allocation failure type=" + type + " length=" + length + " actual=" + observed);
        if (expected == IllegalStateException.class) {
            check(observed == MARKER, "exact pending throwable identity");
            check(observed.getMessage().equals("JNI allocation marker"), "pending message");
        }
    }
    private static void original(boolean loaded) {
        check(M3ArrayNative.isLoaded() == loaded, "native load status");
        char[] a = {'a', '\u0000', '\ud800', 'z'};
        char[] b = {'a', '\u0000', '\ud800', 'y'};
        check(M3ArrayNative.compareUtf16(a, 0, b, 0, 3) == 0, "original equal region");
        check(M3ArrayNative.compareUtf16(a, 3, b, 3, 1) == 1, "original comparison sign");
        char[] joined = M3ArrayNative.concatenateUtf16(a, new char[0], b);
        check(joined.length == 8 && Arrays.equals(Arrays.copyOfRange(joined, 0, 4), a)
                && Arrays.equals(Arrays.copyOfRange(joined, 4, 8), b), "original concatenation");
        joined[0] = 'x';
        check(a[0] == 'a', "original detached result");
    }
    public static void main(String[] args) throws Exception {
        String mode = args[0];
        Path expectedProvider = Path.of(args[1]).toRealPath();
        Path provider = Path.of(M3ArrayNative.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        check(provider.equals(expectedProvider), "exact actual-recipe source provider");
        check(mode.equals("unloaded") || mode.equals("native") || mode.equals("legacy")
                || mode.equals("silent") || mode.equals("pending"), "explicit lane");
        if (!mode.equals("unloaded")) M3ArrayNative.load(Path.of(args[2]));
        for (int type = 0; type < 4; type++) {
            failure(type, -1, NegativeArraySizeException.class);
            failure(type, Integer.MIN_VALUE, NegativeArraySizeException.class);
            if (mode.equals("unloaded") || mode.equals("legacy")) {
                for (int n : new int[] {0, 1, 257}) failure(type, n, UnsatisfiedLinkError.class);
            } else if (mode.equals("silent") || mode.equals("pending")) {
                for (int n : new int[] {0, 1, 257}) {
                    failure(type, n, mode.equals("silent") ? OutOfMemoryError.class : IllegalStateException.class);
                }
            } else {
                // The fixed -Xmx256m JVM plus VM length checks bound this failure test.
                failure(type, Integer.MAX_VALUE, OutOfMemoryError.class);
                directNativeNegative(type, -1);
                directNativeNegative(type, Integer.MIN_VALUE);
                for (int n : new int[] {0, 1, 16, 257, 4097}) {
                    Object first = allocate(type, n), second = allocate(type, n);
                    check(first != second, "detached identities including empty");
                    check(first.getClass() == TYPES[type] && second.getClass() == TYPES[type], "exact runtime array types");
                    check(Array.getLength(first) == n && Array.getLength(second) == n, "exact lengths");
                    for (int i = 0; i < n; i++) {
                        Object value = Array.get(first, i);
                        check(type == 0 ? value == null : ((Number) value).longValue() == 0L, "zero initialization");
                    }
                    if (n > 0) {
                        if (type == 0) ((Object[]) first)[n - 1] = new String("owned");
                        else if (type == 1) ((byte[]) first)[n - 1] = Byte.MIN_VALUE;
                        else if (type == 2) ((int[]) first)[n - 1] = Integer.MIN_VALUE;
                        else ((long[]) first)[n - 1] = Long.MIN_VALUE;
                        Object value = Array.get(second, n - 1);
                        check(type == 0 ? value == null : ((Number) value).longValue() == 0L, "writes are detached");
                    }
                }
            }
        }
        if (mode.equals("native")) {
            Object[] roots = M3ArrayNative.allocateObjectsRequired(1024);
            for (int i = 0; i < roots.length; i++) roots[i] = new String("root-" + i);
            long beforeGc = collections();
            System.gc();
            check(collections() > beforeGc, "VM-observed garbage collection");
            for (int i = 0; i < roots.length; i++) check(roots[i].equals("root-" + i), "Java reference survival");
            java.lang.ref.Reference.reachabilityFence(roots);
        }
        if (mode.equals("unloaded") || mode.equals("native") || mode.equals("legacy")) original(!mode.equals("unloaded"));
        System.out.println("M3_BACKING_ARRAY_CONTRACT_V2_PASS mode=" + mode + " checks=" + checks);
    }
}
