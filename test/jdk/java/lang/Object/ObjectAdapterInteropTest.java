/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
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
 * @summary Preserve public adapter identity, failure, loader and virtual invocation contracts
 * @run main/othervm/native -Xcheck:jni ObjectAdapterInteropTest
 * @run main/othervm/native -Xcheck:jni -Xint ObjectAdapterInteropTest
 * @run main/othervm/native -Xcheck:jni -Xcomp -Xbatch -XX:-TieredCompilation ObjectAdapterInteropTest
 */

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/** A Java contract corpus. No foreign runtime or foreign coercion rule is introduced. */
public final class ObjectAdapterInteropTest {
    private static int checks;

    public static void main(String[] args) throws Throwable {
        assignabilityAndPrimitives();
        independentLoaders();
        virtualInvocation();
        nativeInvocation();
        System.out.println("OBJECT_ADAPTER_INTEROP_PASS checks=" + checks);
    }

    private static native Object invokeAdapter0(Object receiver, Class<?> type,
                                                 int selector, int mode,
                                                 boolean expectException);

    private static native Object copyReferences0(Object value, int count, Throwable pending);

    private static void nativeInvocation() throws Throwable {
        System.loadLibrary("ObjectAdapterInteropTest");
        for (int selector = 0; selector < 2; selector++) {
            for (int mode = 0; mode < 6; mode++) {
                nativeDispatch(selector, mode);
                nativeArrays(selector, mode);
            }
        }
        nativeReferences();
        nativeLoaders();
        expect(IllegalArgumentException.class,
                () -> invokeAdapter0(new Object(), Object.class, 2, 0, false));
        expect(IllegalArgumentException.class,
                () -> invokeAdapter0(new Object(), Object.class, 0, 6, false));
        expect(IllegalArgumentException.class, () -> copyReferences0(new Object(), 0, null));
        expect(IllegalArgumentException.class, () -> copyReferences0(new Object(), 8_193, null));
    }

    private static void nativeDispatch(int selector, int mode) {
        String label = "JNI selector=" + selector + " mode=" + mode;
        Hostile hostile = new Hostile();
        check(invokeAdapter0(hostile, Object.class, selector, mode, false) == hostile,
                label + " base identity without Object hooks");
        check(invokeAdapter0(hostile, Marker.class, selector, mode, false) == hostile,
                label + " interface identity");
        check(invokeAdapter0(hostile, String.class, selector, mode, false) == null,
                label + " unsupported is successful null");
        for (Class<?> primitive : List.of(boolean.class, byte.class, short.class, char.class,
                int.class, long.class, float.class, double.class, void.class)) {
            check(invokeAdapter0(hostile, primitive, selector, mode, false) == null,
                    label + " primitive token remains unsupported");
        }
        expect(NullPointerException.class,
                () -> invokeAdapter0(hostile, null, selector, mode, true));
        expect(NullPointerException.class,
                () -> invokeAdapter0(null, Object.class, selector, mode, true));

        // A base nonvirtual alias body still makes a virtual toAdapter call.
        boolean reachesOverride = mode < 3 || selector == 1;
        OverrideReceiver receiver = new OverrideReceiver();
        check(invokeAdapter0(receiver, String.class, selector, mode, false) == null,
                label + " null override result is not a pending exception");
        check(receiver.calls == (reachesOverride ? 1 : 0), label + " exact override count");
        String value = new String("native-value");
        receiver.value = value;
        check(invokeAdapter0(receiver, String.class, selector, mode, false)
                == (reachesOverride ? value : null), label + " nonvirtual entry semantics");
        check(receiver.calls == (reachesOverride ? 2 : 0), label + " exact repeated count");
        if (reachesOverride) {
            check(invokeAdapter0(receiver, null, selector, mode, false) == null
                    && receiver.lastType == null, label + " override controls null token");
        } else {
            expect(NullPointerException.class,
                    () -> invokeAdapter0(receiver, null, selector, mode, true));
        }
        int beforeFailure = receiver.calls;
        for (Throwable failure : new Throwable[] {
                new IllegalStateException("native-runtime"), new AssertionError("native-error")}) {
            receiver.failure = failure;
            if (reachesOverride) {
                exactFailure(failure,
                        () -> invokeAdapter0(receiver, String.class, selector, mode, true));
                beforeFailure++;
            } else {
                check(invokeAdapter0(receiver, String.class, selector, mode, false) == null,
                        label + " direct base invocation bypasses throwing override");
            }
            check(receiver.calls == beforeFailure, label + " failure call count");
        }

        NativeAliasReceiver alias = new NativeAliasReceiver();
        boolean reachesAlias = selector == 1 && mode < 3;
        check(invokeAdapter0(alias, String.class, selector, mode, false)
                == (reachesAlias ? alias.value : null), label + " alias override dispatch");
        check(alias.calls == (reachesAlias ? 1 : 0), label + " exact alias count");
        alias.failure = new IllegalArgumentException("alias-sentinel");
        if (reachesAlias) {
            exactFailure(alias.failure,
                    () -> invokeAdapter0(alias, String.class, selector, mode, true));
        } else {
            check(invokeAdapter0(alias, String.class, selector, mode, false) == null,
                    label + " nonvirtual alias bypasses alias override");
        }
        check(alias.calls == (reachesAlias ? 2 : 0), label + " alias failure count");

        NativeWrongResult wrong = new NativeWrongResult();
        check(invokeAdapter0(wrong, String.class, selector, mode, false)
                == (reachesOverride ? wrong.value : null), label + " erased result is preserved");
        if (reachesOverride) {
            expect(ClassCastException.class,
                    () -> String.class.cast(invokeAdapter0(wrong, String.class, selector, mode, false)));
        }
        expect(AssertionError.class,
                () -> invokeAdapter0(hostile, String.class, selector, mode, true));
        java.lang.ref.Reference.reachabilityFence(hostile);
        java.lang.ref.Reference.reachabilityFence(receiver);
    }

    private static void nativeArrays(int selector, int mode) {
        Object[] arrays = {new boolean[1], new byte[1], new short[1], new char[1],
                new int[1], new long[1], new float[1], new double[1],
                new Object[1], new String[1], new int[1][1], new Object[1][1]};
        for (Object array : arrays) {
            check(invokeAdapter0(array, array.getClass(), selector, mode, false) == array,
                    "JNI exact array identity");
            check(invokeAdapter0(array, Cloneable.class, selector, mode, false) == array,
                    "JNI array Cloneable identity");
            check(invokeAdapter0(array, java.io.Serializable.class, selector, mode, false) == array,
                    "JNI array Serializable identity");
            check(invokeAdapter0(array, Object[].class, selector, mode, false)
                    == (array instanceof Object[] ? array : null), "JNI array covariance");
            check(copyReferences0(array, 64, null) == array, "JNI array reference lifetime");
            java.lang.ref.Reference.reachabilityFence(array);
        }
    }

    private static void nativeReferences() {
        check(copyReferences0(null, 64, null) == null, "JNI successful null reference roundtrip");
        Object value = new Hostile();
        check(copyReferences0(value, 4_096, null) == value,
                "JNI bounded local frames retain exact live object");
        for (Throwable failure : new Throwable[] {
                new IllegalStateException("reference-runtime"), new AssertionError("reference-error")}) {
            exactFailure(failure, () -> copyReferences0(value, 256, failure));
            java.lang.ref.Reference.reachabilityFence(failure);
        }
        java.lang.ref.Reference.reachabilityFence(value);
    }

    private static void nativeLoaders() throws Exception {
        String resource = "/" + LoaderValue.class.getName().replace('.', '/') + ".class";
        byte[] bytes;
        try (var input = ObjectAdapterInteropTest.class.getResourceAsStream(resource)) {
            if (input == null) throw new IOException("missing native loader fixture");
            bytes = input.readAllBytes();
        }
        Class<?> first = new IsolatedLoader().define(bytes);
        Class<?> second = new IsolatedLoader().define(bytes);
        Object value = first.getConstructor().newInstance();
        Object array = Array.newInstance(first, 1);
        Class<?> foreignArray = Array.newInstance(second, 1).getClass();
        check(first != second && first.getName().equals(second.getName()),
                "JNI loader fixture distinguishes name from identity");
        for (int selector = 0; selector < 2; selector++) {
            for (int mode = 0; mode < 6; mode++) {
                check(invokeAdapter0(value, first, selector, mode, false) == value,
                        "JNI defining-loader identity");
                check(invokeAdapter0(value, second, selector, mode, false) == null,
                        "JNI same-name foreign class refuses");
                check(invokeAdapter0(array, array.getClass(), selector, mode, false) == array,
                        "JNI defining-loader array identity");
                check(invokeAdapter0(array, foreignArray, selector, mode, false) == null,
                        "JNI foreign array class refuses");
            }
        }
        check(copyReferences0(first, 64, null) == first, "JNI Class reference identity");
        check(copyReferences0(value, 64, null) == value, "JNI isolated instance reference identity");
        check(copyReferences0(array, 64, null) == array, "JNI isolated array reference identity");
        java.lang.ref.Reference.reachabilityFence(first);
        java.lang.ref.Reference.reachabilityFence(second);
        java.lang.ref.Reference.reachabilityFence(value);
        java.lang.ref.Reference.reachabilityFence(array);
    }

    private static final class NativeAliasReceiver {
        private final String value = new String("native-alias");
        private int calls;
        private RuntimeException failure;

        @Override public <T> T toAdpater(Class<T> type) {
            calls++;
            if (failure != null) throw failure;
            return type.cast(value);
        }
    }

    private static final class NativeWrongResult {
        private final Object value = new Object();

        @Override @SuppressWarnings("unchecked") public <T> T toAdapter(Class<T> type) {
            return (T) value;
        }
    }


    private static void assignabilityAndPrimitives() {
        Hostile hostile = new Hostile();
        check(hostile.toAdapter(Object.class) == hostile, "Object identity");
        check(hostile.toAdapter(Hostile.class) == hostile, "concrete identity");
        check(hostile.toAdapter(Marker.class) == hostile, "interface identity");
        check(hostile.toAdapter(CharSequence.class) == null, "unsupported reference target");
        check(hostile.toAdpater(Marker.class) == hostile, "legacy alias identity");
        Object integer = Integer.valueOf(123);
        check(integer.toAdapter(Integer.class) == integer, "wrapper identity");
        check(integer.toAdapter(Number.class) == integer, "superclass identity");
        check(integer.toAdapter(Long.class) == null, "no numeric wrapper coercion");
        for (Class<?> primitive : List.of(boolean.class, byte.class, short.class, char.class,
                int.class, long.class, float.class, double.class, void.class)) {
            check(integer.toAdapter(primitive) == null, "primitive tokens are not conversions");
            check(hostile.toAdapter(primitive) == null, "unsupported primitive never calls receiver hooks");
        }
        Object strings = new String[] {"value"};
        check(strings.toAdapter(String[].class) == strings, "array identity");
        check(strings.toAdapter(Object[].class) == strings, "array covariance identity");
        check(strings.toAdapter(Cloneable.class) == strings, "array interface identity");
        check(strings.toAdapter(Integer[].class) == null, "incompatible array type");
        Object ints = new int[] {1};
        check(ints.toAdapter(int[].class) == ints, "primitive-array identity");
        check(ints.toAdapter(Object[].class) == null, "primitive array is not reference array");
        check("abc".toAdapter(java.security.SHA256Adapter.class) != null, "existing explicit SHA adapter route");
        expect(NullPointerException.class, () -> hostile.toAdapter(null));
        expect(NullPointerException.class, () -> hostile.toAdpater(null));
    }

    private static void independentLoaders() throws Exception {
        String resource = "/" + LoaderValue.class.getName().replace('.', '/') + ".class";
        byte[] bytes;
        try (var input = ObjectAdapterInteropTest.class.getResourceAsStream(resource)) {
            if (input == null) throw new IOException("missing loader fixture class");
            bytes = input.readAllBytes();
        }
        Class<?> first = new IsolatedLoader().define(bytes);
        Class<?> second = new IsolatedLoader().define(bytes);
        Object receiver = first.getConstructor().newInstance();
        check(first.getName().equals(second.getName()) && first != second, "same name, independent class identity");
        check(first.getClassLoader() != second.getClassLoader(), "independent defining loaders");
        check(receiver.toAdapter(first) == receiver, "defining-loader identity");
        check(receiver.toAdapter(second) == null, "same spelling does not imply conversion");
        Object array = Array.newInstance(first, 1);
        Class<?> otherArray = Array.newInstance(second, 1).getClass();
        check(array.toAdapter(array.getClass()) == array, "defining-loader array identity");
        check(array.toAdapter(otherArray) == null, "loader-distinct arrays refuse");
        check(receiver.toAdapter(Object.class) == receiver, "bootstrap supertype remains assignable");
    }

    private static void virtualInvocation() throws Throwable {
        Method current = Object.class.getMethod("toAdapter", Class.class);
        Method legacy = Object.class.getMethod("toAdpater", Class.class);
        MethodType signature = MethodType.methodType(Object.class, Class.class);
        MethodHandle currentHandle = MethodHandles.lookup().findVirtual(Object.class, "toAdapter", signature);
        MethodHandle legacyHandle = MethodHandles.lookup().findVirtual(Object.class, "toAdpater", signature);
        List<Invoker> invokers = List.of(
                (receiver, type) -> receiver.toAdapter(type),
                (receiver, type) -> receiver.toAdpater(type),
                (receiver, type) -> reflected(current, receiver, type),
                (receiver, type) -> reflected(legacy, receiver, type),
                (receiver, type) -> (Object) currentHandle.invokeExact(receiver, type),
                (receiver, type) -> (Object) legacyHandle.invokeExact(receiver, type));
        RuntimeException runtimeFailure = new IllegalStateException("sentinel-runtime");
        Error errorFailure = new AssertionError("sentinel-error");
        String value = new String("converted");
        for (Invoker invoke : invokers) {
            Object base = new Object();
            check(invoke.apply(base, Object.class) == base, "base identity through invocation surface");
            check(invoke.apply(base, String.class) == null, "unsupported result remains null");
            expect(NullPointerException.class, () -> invoke.apply(base, null));
            expect(NullPointerException.class, () -> invoke.apply(null, String.class));

            OverrideReceiver receiver = new OverrideReceiver();
            check(invoke.apply(receiver, String.class) == null, "override may successfully return null");
            check(receiver.calls == 1 && receiver.lastType == String.class, "one virtual override call");
            check(invoke.apply(receiver, null) == null && receiver.calls == 2 && receiver.lastType == null,
                    "override retains control of null class token");
            receiver.value = value;
            check(invoke.apply(receiver, String.class) == value && receiver.calls == 3,
                    "override return identity retained");
            receiver.failure = runtimeFailure;
            exactFailure(runtimeFailure, () -> invoke.apply(receiver, String.class));
            check(receiver.calls == 4, "runtime failure invokes once");
            receiver.failure = errorFailure;
            exactFailure(errorFailure, () -> invoke.apply(receiver, String.class));
            check(receiver.calls == 5, "Error invokes once");
        }
    }

    private static Object reflected(Method method, Object receiver, Class<?> type) throws Throwable {
        try {
            return method.invoke(receiver, (Object) type);
        } catch (InvocationTargetException invocation) {
            // Reflection wraps target failures; compare the exact original target object.
            throw invocation.getCause();
        }
    }

    private interface Marker { }

    private static final class Hostile implements Marker {
        @Override public int hashCode() { throw new AssertionError("hashCode must not run"); }
        @Override public boolean equals(Object other) { throw new AssertionError("equals must not run"); }
        @Override public String toString() { throw new AssertionError("toString must not run"); }
    }

    private static final class OverrideReceiver {
        private int calls;
        private Class<?> lastType;
        private Object value;
        private Throwable failure;

        @Override public <T> T toAdapter(Class<T> type) {
            calls++;
            lastType = type;
            if (failure instanceof RuntimeException runtime) throw runtime;
            if (failure instanceof Error error) throw error;
            return type == null ? null : type.cast(value);
        }
    }

    public static final class LoaderValue {
        public LoaderValue() { }
    }

    private static final class IsolatedLoader extends ClassLoader {
        IsolatedLoader() { super(null); }
        Class<?> define(byte[] bytes) { return defineClass(null, bytes, 0, bytes.length); }
    }

    @FunctionalInterface private interface Invoker {
        Object apply(Object receiver, Class<?> type) throws Throwable;
    }

    @FunctionalInterface private interface Action { void run() throws Throwable; }

    private static void expect(Class<? extends Throwable> type, Action action) {
        try {
            action.run();
        } catch (Throwable failure) {
            check(type.isInstance(failure), "expected failure type " + type.getName());
            return;
        }
        throw new AssertionError("missing failure " + type.getName());
    }

    private static void exactFailure(Throwable expected, Action action) {
        try {
            action.run();
        } catch (Throwable actual) {
            check(actual == expected, "failure object identity retained");
            return;
        }
        throw new AssertionError("exception must not become successful null");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
