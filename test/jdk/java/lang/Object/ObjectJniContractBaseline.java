/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: GPL-2.0-only
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary Standard JNI virtual/nonvirtual calls, exception cleanup and live reference identities
 * @run main/othervm/native -Xcheck:jni ObjectJniContractBaseline
 * @run main/othervm/native -Xcheck:jni -Xint ObjectJniContractBaseline
 * @run main/othervm/native -Xcheck:jni -Xcomp -Xbatch -XX:-TieredCompilation ObjectJniContractBaseline
 */

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.reflect.Array;

/**
 * Independent standard-JNI baseline. These ordinary callbacks contain no M3 adapter calls.
 * Passing this test does not qualify the separate Object adapter extension.
 */
public final class ObjectJniContractBaseline {
    private static int checks;

    private static native Object invokeBaseline0(Object receiver, Class<?> type, int selector,
                                                   int mode, boolean expectException);
    private static native Object copyReferences0(Object value, int count, Throwable pending);
    private static native void lookupMissing0(Object receiver);

    public static void main(String[] args) throws Exception {
        System.loadLibrary("ObjectAdapterInteropTest");
        for (int selector : new int[] {0, 1}) {
            for (int mode : new int[] {0, 1, 2, 3, 4, 5}) {
                invocation(selector, mode);
            }
        }
        references();
        loaders();
        expect(NoSuchMethodError.class, () -> lookupMissing0(new Callback()));
        expect(IllegalArgumentException.class, () -> invokeBaseline0(new Callback(), Object.class, 2, 0, false));
        expect(IllegalArgumentException.class, () -> invokeBaseline0(new Callback(), Object.class, 0, 6, false));
        expect(IllegalArgumentException.class, () -> copyReferences0(new Object(), 0, null));
        expect(IllegalArgumentException.class, () -> copyReferences0(new Object(), 8193, null));
        System.out.println("STANDARD_JNI_CONTRACT_BASELINE_PASS checks=" + checks
                + " adapter_qualification=false");
    }

    private static void invocation(int selector, int mode) {
        boolean reachesOverride = mode < 3 || selector == 1;
        Callback plain = new Callback();
        check(invokeBaseline0(plain, String.class, selector, mode, false) == String.class,
                "ordinary base callback result");
        Returning returning = new Returning();
        Object payload = new Object();
        returning.value = payload;
        check(invokeBaseline0(returning, String.class, selector, mode, false)
                        == (reachesOverride ? payload : String.class), "virtual/nonvirtual result");
        check(returning.calls == (reachesOverride ? 1 : 0), "virtual/nonvirtual call count");
        check(returning.lastType == (reachesOverride ? String.class : null), "class token identity");

        returning.value = null;
        check(invokeBaseline0(returning, String.class, selector, mode, false)
                        == (reachesOverride ? null : String.class), "successful null has no pending exception");
        check(invokeBaseline0(returning, null, selector, mode, false) == null, "null argument crosses JNI unchanged");

        AliasOverride alias = new AliasOverride();
        boolean aliasOverride = selector == 1 && mode < 3;
        check(invokeBaseline0(alias, String.class, selector, mode, false)
                        == (aliasOverride ? alias.marker : String.class), "independent alias override");
        check(alias.calls == (aliasOverride ? 1 : 0), "alias callback count");

        for (Throwable failure : new Throwable[] {
                new IllegalStateException("runtime sentinel"), new AssertionError("error sentinel")}) {
            Returning throwing = new Returning();
            throwing.failure = failure;
            if (reachesOverride) {
                exact(failure, () -> invokeBaseline0(throwing, Object.class, selector, mode, true));
                check(throwing.calls == 1, "throwing callback runs once");
            } else {
                check(invokeBaseline0(throwing, Object.class, selector, mode, false) == Object.class,
                        "nonvirtual direct entry bypasses throwing override");
                check(throwing.calls == 0, "bypassed throwing callback does not run");
            }
        }
        expect(NullPointerException.class, () -> invokeBaseline0(null, Object.class, selector, mode, false));
        expect(AssertionError.class, () -> invokeBaseline0(plain, Object.class, selector, mode, true));
    }

    private static void references() {
        Object[] values = {null, new Object(), "JNI text \u0000 \ud83d\ude00",
                new boolean[0], new byte[1], new short[1], new char[1], new int[1],
                new long[1], new float[1], new double[1], new Object[1], new String[1],
                new int[1][1], new Object[1][1]};
        for (Object value : values) {
            check(copyReferences0(value, 64, null) == value, "local/global/weak reference round trip");
            Returning callback = new Returning();
            callback.value = value;
            for (int selector : new int[] {0, 1}) {
                for (int mode : new int[] {0, 1, 2, 3, 4, 5}) {
                    check(invokeBaseline0(callback, Object.class, selector, mode, false)
                            == (mode < 3 || selector == 1 ? value : Object.class), "array/null callback identity");
                }
            }
            Reference.reachabilityFence(value);
        }
        Object retained = new Object();
        check(copyReferences0(retained, 4096, null) == retained, "bounded repeated local frames");
        for (Throwable failure : new Throwable[] {
                new IllegalArgumentException("native runtime sentinel"), new AssertionError("native error sentinel")}) {
            exact(failure, () -> copyReferences0(retained, 256, failure));
        }
        Reference.reachabilityFence(retained);
    }

    private static void loaders() throws Exception {
        String name = LoaderPayload.class.getName();
        byte[] bytes;
        try (var input = ObjectJniContractBaseline.class.getResourceAsStream(
                "/" + name.replace('.', '/') + ".class")) {
            if (input == null) throw new IOException("missing loader fixture");
            bytes = input.readAllBytes();
        }
        Class<?> first = new IsolatedLoader().define(bytes);
        Class<?> second = new IsolatedLoader().define(bytes);
        check(first != second && first.getName().equals(second.getName()), "loader-separated class identities");
        Object instance = first.getConstructor().newInstance();
        Object array = Array.newInstance(first, 2);
        check(copyReferences0(instance, 64, null) == instance, "loader-specific object reference");
        check(copyReferences0(first, 64, null) == first, "Class object reference");
        check(copyReferences0(array, 64, null) == array, "loader-specific array reference");
        Returning callback = new Returning();
        callback.value = instance;
        for (int selector : new int[] {0, 1}) {
            for (int mode : new int[] {0, 1, 2, 3, 4, 5}) {
                callback.lastType = null;
                check(invokeBaseline0(callback, second, selector, mode, false)
                                == (mode < 3 || selector == 1 ? instance : second), "opaque loader token crossing");
                check(callback.lastType == (mode < 3 || selector == 1 ? second : null),
                        "JNI does not resolve Class values by spelling");
            }
        }
        Reference.reachabilityFence(first);
        Reference.reachabilityFence(second);
    }

    public static class Callback {
        public Object bounce(Class<?> type) { return type; }
        public Object alias(Class<?> type) { return bounce(type); }
    }

    private static final class Returning extends Callback {
        private Object value;
        private Throwable failure;
        private int calls;
        private Class<?> lastType;

        @Override public Object bounce(Class<?> type) {
            calls++;
            lastType = type;
            if (failure instanceof RuntimeException runtime) throw runtime;
            if (failure instanceof Error error) throw error;
            return value;
        }
    }

    private static final class AliasOverride extends Callback {
        private final Object marker = new Object();
        private int calls;
        @Override public Object alias(Class<?> type) { calls++; return marker; }
    }

    public static final class LoaderPayload {
        public LoaderPayload() { }
    }

    private static final class IsolatedLoader extends ClassLoader {
        IsolatedLoader() { super(null); }
        Class<?> define(byte[] bytes) { return defineClass(null, bytes, 0, bytes.length); }
    }

    @FunctionalInterface private interface Action { void run() throws Throwable; }

    private static void exact(Throwable expected, Action action) {
        try {
            action.run();
        } catch (Throwable actual) {
            check(actual == expected, "pending Throwable identity survives native cleanup");
            return;
        }
        throw new AssertionError("failure was converted into successful return");
    }

    private static void expect(Class<? extends Throwable> type, Action action) {
        try {
            action.run();
        } catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type.getName() + ", got " + failure);
            return;
        }
        throw new AssertionError("missing expected " + type.getName());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
