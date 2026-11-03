/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: GPL-2.0-only
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @test
 * @summary Object.toAdapter preserves its contract across JNI, reflection and method handles
 * @run main/othervm/native -Xcheck:jni ObjectAdapterNativeTest disabled
 * @run main/othervm/native -Xcheck:jni -Djdk.m3.objectEvents.enabled=true ObjectAdapterNativeTest enabled
 * @run main/othervm/native -Xcheck:jni -Xint -Djdk.m3.objectEvents.enabled=false ObjectAdapterNativeTest disabled
 * @run main/othervm/native -Xcheck:jni -Xint -Djdk.m3.objectEvents.enabled=true ObjectAdapterNativeTest enabled
 * @run main/othervm/native -Xcheck:jni -Xcomp -Xbatch -XX:-TieredCompilation -Djdk.m3.objectEvents.enabled=false ObjectAdapterNativeTest disabled
 * @run main/othervm/native -Xcheck:jni -Xcomp -Xbatch -XX:-TieredCompilation -Djdk.m3.objectEvents.enabled=true ObjectAdapterNativeTest enabled
 */

import java.io.InputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;

public final class ObjectAdapterNativeTest {
    private static final int REPEATED_CALLS = 4_096;
    private static int assertions;
    private static long repeatedCalls;

    private enum NativeMode {
        CALL_OBJECT(0), CALL_OBJECT_A(1), CALL_OBJECT_V(2);

        final int value;

        NativeMode(int value) {
            this.value = value;
        }
    }

    private static final class AdapterToken {}

    private static final class Overrides {
        final AdapterToken token = new AdapterToken();
        int calls;

        @Override
        public <T> T toAdapter(Class<T> type) {
            calls++;
            if (type == AdapterToken.class) {
                return type.cast(token);
            }
            return super.toAdapter(type);
        }
    }

    private static final class Throws {
        final IllegalStateException failure = new IllegalStateException("adapter sentinel");
        int calls;

        @Override
        public <T> T toAdapter(Class<T> type) {
            calls++;
            throw failure;
        }
    }

    private static final class WrongResult {
        final Object result = new Object();

        @Override
        @SuppressWarnings("unchecked")
        public <T> T toAdapter(Class<T> type) {
            // An overriding method can violate its generic promise. JNI must
            // expose that raw result; a checked Java consumer detects the error.
            return (T) result;
        }
    }

    public static final class LoaderPayload {
        public LoaderPayload() {}
    }

    private static final class PayloadLoader extends ClassLoader {
        PayloadLoader() {
            super(null);
        }

        Class<?> define(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Throwable;
    }

    private static native Object invoke0(Object receiver, Class<?> type, int mode);

    private static native int repeat0(Object receiver, Class<?> type, Object expected,
                                      int count, int mode);

    public static void main(String[] args) throws Throwable {
        if (args.length != 1 || !(args[0].equals("enabled") || args[0].equals("disabled"))) {
            throw new IllegalArgumentException("expected enabled or disabled");
        }
        System.loadLibrary("ObjectAdapterNativeTest");
        boolean enabled = args[0].equals("enabled");
        check(M3Object.observationEnabled() == enabled, "startup observation setting");

        for (NativeMode mode : NativeMode.values()) {
            ordinaryReceivers(mode);
            arrayReceivers(mode);
            overrideDispatch(mode);
            boundedNativeFrame(mode);
        }

        Method method = Object.class.getDeclaredMethod("toAdapter", Class.class);
        genericSignature(method);
        reflectionDispatch(method);
        MethodHandle handle = MethodHandles.publicLookup().findVirtual(Object.class,
                "toAdapter", MethodType.methodType(Object.class, Class.class));
        methodHandleDispatch(handle);
        classLoaderIdentity(method, handle);
        System.out.println("ObjectAdapterNativeTest passed: " + assertions
                + " assertions, " + repeatedCalls + " repeated JNI calls, "
                + "observation " + args[0]);
    }

    private static Object rawAdapt(Object receiver, Class<?> type, NativeMode mode) {
        // CallObjectMethod requires a non-null receiver. Reject it in Java;
        // the type token and raw return value cross JNI without extra checks.
        return invoke0(Objects.requireNonNull(receiver, "receiver"), type, mode.value);
    }

    private static <T> T adapt(Object receiver, Class<T> type, NativeMode mode) {
        return type.cast(rawAdapt(receiver, type, mode));
    }

    private static void ordinaryReceivers(NativeMode mode) throws Throwable {
        Object source = new Object();
        same(source, adapt(source, Object.class, mode), mode + " Object identity");
        String text = "native-adapter";
        same(text, adapt(text, String.class, mode), mode + " String identity");
        same(text, adapt(text, CharSequence.class, mode), mode + " interface identity");
        Integer number = Integer.valueOf(37);
        same(number, adapt(number, Number.class, mode), mode + " superclass identity");
        same(String.class, adapt(String.class, Class.class, mode), mode + " Class identity");
        for (Class<?> type : new Class<?>[] {String.class, Runnable.class, int.class,
                void.class, ObjectEvent.class, M3Object.Field.class}) {
            same(null, rawAdapt(source, type, mode), mode + " unsupported " + type);
        }
        expect(NullPointerException.class, () -> rawAdapt(source, null, mode),
                mode + " null type token");
        expect(NullPointerException.class, () -> rawAdapt(null, Object.class, mode),
                mode + " Java wrapper rejects null receiver");
        capability(source, mode);
    }

    private static void arrayReceivers(NativeMode mode) throws Throwable {
        Object[] arrays = {
                new boolean[0], new byte[1], new short[1], new char[1],
                new int[1], new long[1], new float[1], new double[1],
                new Object[1], new String[1], new int[1][1], new Object[1][1]
        };
        for (Object array : arrays) {
            String label = mode + " " + array.getClass().getTypeName();
            same(array, rawAdapt(array, array.getClass(), mode), label + " exact identity");
            same(array, adapt(array, Object.class, mode), label + " Object identity");
            same(array, adapt(array, Cloneable.class, mode), label + " Cloneable identity");
            same(array, adapt(array, Serializable.class, mode), label + " Serializable identity");
            same(array instanceof Object[] ? array : null,
                    adapt(array, Object[].class, mode), label + " reference covariance");
            same(null, adapt(array, String.class, mode), label + " unsupported type");
            expect(NullPointerException.class, () -> rawAdapt(array, null, mode),
                    label + " null type token");
            capability(array, mode);
        }
    }

    private static void capability(Object source, NativeMode mode) {
        M3Object owner = adapt(source, M3Object.class, mode);
        check(owner != null, mode + " exact M3Object capability");
        same(source, owner.source(), mode + " capability source");
        same(owner, adapt(source, M3Object.class, mode), mode + " retained capability identity");
        same(owner, M3Object.of(source), mode + " existing owner identity");
        same(owner, adapt(owner, M3Object.class, mode), mode + " assignability before capability");
        same(owner, adapt(owner, Object.class, mode), mode + " capability Object identity");
    }

    private static void overrideDispatch(NativeMode mode) throws Throwable {
        Overrides source = new Overrides();
        same(source.token, adapt(source, AdapterToken.class, mode), mode + " virtual override");
        same(source, adapt(source, Object.class, mode), mode + " delegated identity");
        M3Object owner = adapt(source, M3Object.class, mode);
        same(source, owner.source(), mode + " delegated capability source");
        same(owner, M3Object.of(source), mode + " delegated existing owner");
        same(null, adapt(source, Runnable.class, mode), mode + " delegated unsupported type");
        expect(NullPointerException.class, () -> rawAdapt(source, null, mode),
                mode + " delegated null type token");
        check(source.calls == 5, mode + " each call reaches the override exactly once");

        Throws throwing = new Throws();
        same(throwing.failure,
                expect(IllegalStateException.class,
                        () -> rawAdapt(throwing, Object.class, mode), mode + " override exception"),
                mode + " exception identity survives JNI");
        check(throwing.calls == 1, mode + " throwing override called once");

        WrongResult wrong = new WrongResult();
        same(wrong.result, rawAdapt(wrong, String.class, mode), mode + " raw override result");
        expect(ClassCastException.class, () -> adapt(wrong, String.class, mode),
                mode + " checked Java wrapper rejects wrong result");
        expect(ClassCastException.class, () -> ((Object) wrong).toAdapter(String.class).length(),
                mode + " ordinary typed Java consumer rejects wrong result");
    }

    private static void boundedNativeFrame(NativeMode mode) throws Throwable {
        Object source = new Object();
        M3Object owner = M3Object.of(source);
        repeat(source, Object.class, source, mode, "identity");
        repeat(source, M3Object.class, owner, mode, "retained capability");
        repeat(source, String.class, null, mode, "unsupported type");
        int[] array = {3, 5, 7};
        repeat(array, int[].class, array, mode, "primitive array");
        Overrides overridden = new Overrides();
        repeat(overridden, AdapterToken.class, overridden.token, mode, "virtual override");
        check(overridden.calls == REPEATED_CALLS, mode + " repeated override call count");
        same(source, owner.source(), mode + " capability remains retained throughout native loop");

        Throws throwing = new Throws();
        same(throwing.failure,
                expect(IllegalStateException.class,
                        () -> repeat0(throwing, Object.class, null, REPEATED_CALLS, mode.value),
                        mode + " repeated override exception"),
                mode + " pending exception survives local-frame cleanup");
        check(throwing.calls == 1, mode + " native loop stops at its first exception");
        expect(NullPointerException.class,
                () -> repeat0(source, null, null, REPEATED_CALLS, mode.value),
                mode + " null token survives local-frame cleanup");
    }

    private static void repeat(Object source, Class<?> type, Object expected,
                               NativeMode mode, String label) {
        int completed = repeat0(Objects.requireNonNull(source), type, expected,
                REPEATED_CALLS, mode.value);
        check(completed == REPEATED_CALLS, mode + " repeated " + label
                + ": completed " + completed + " of " + REPEATED_CALLS);
        repeatedCalls += completed;
    }

    private static void genericSignature(Method method) {
        same(Object.class, method.getDeclaringClass(), "reflection declares Object method");
        same(Object.class, method.getReturnType(), "erased return type is Object");
        check(Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && !Modifier.isFinal(method.getModifiers()), "public overridable instance method");
        TypeVariable<Method>[] variables = method.getTypeParameters();
        check(variables.length == 1, "one generic type parameter");
        check(variables[0].getName().equals("T"), "generic parameter T");
        check(variables[0].getGenericDeclaration().equals(method), "method owns T");
        check(variables[0].getBounds().length == 1, "one bound for T");
        same(Object.class, variables[0].getBounds()[0], "T has Object upper bound");
        check(method.getGenericReturnType().equals(variables[0]), "generic return type is T");
        Type[] parameters = method.getGenericParameterTypes();
        check(parameters.length == 1 && parameters[0] instanceof ParameterizedType,
                "one parameterized Class parameter");
        ParameterizedType parameter = (ParameterizedType) parameters[0];
        same(Class.class, parameter.getRawType(), "generic parameter raw type is Class");
        check(parameter.getActualTypeArguments().length == 1
                && parameter.getActualTypeArguments()[0].equals(variables[0]),
                "generic parameter is Class<T>");
    }

    private static void reflectionDispatch(Method method) throws Throwable {
        Object source = new Object();
        M3Object owner = M3Object.of(source);
        same(source, method.invoke(source, Object.class), "reflection identity");
        same(null, method.invoke(source, String.class), "reflection unsupported type");
        same(owner, method.invoke(source, M3Object.class), "reflection retained capability");
        InvocationTargetException nullToken = expect(InvocationTargetException.class,
                () -> method.invoke(source, new Object[] {null}), "reflection null token");
        check(nullToken.getCause() instanceof NullPointerException, "reflection wraps null-token NPE");
        Overrides overridden = new Overrides();
        same(overridden.token, method.invoke(overridden, AdapterToken.class),
                "Object Method invokes subclass override");
        check(overridden.calls == 1, "reflection invokes override exactly once");
        Throws throwing = new Throws();
        InvocationTargetException failure = expect(InvocationTargetException.class,
                () -> method.invoke(throwing, Object.class), "reflection wraps override exception");
        same(throwing.failure, failure.getCause(), "reflection exception cause identity");
        WrongResult wrong = new WrongResult();
        same(wrong.result, method.invoke(wrong, String.class), "reflection returns raw override result");
    }

    private static void methodHandleDispatch(MethodHandle handle) throws Throwable {
        Object source = new Object();
        M3Object owner = M3Object.of(source);
        same(source, handleAdapt(handle, source, Object.class), "method handle identity");
        same(null, handleAdapt(handle, source, String.class), "method handle unsupported type");
        same(owner, handleAdapt(handle, source, M3Object.class), "method handle retained capability");
        expect(NullPointerException.class, () -> handleAdapt(handle, source, null),
                "method handle null token");
        Overrides overridden = new Overrides();
        same(overridden.token, handleAdapt(handle, overridden, AdapterToken.class),
                "Object method handle invokes subclass override");
        check(overridden.calls == 1, "method handle invokes override exactly once");
        Throws throwing = new Throws();
        same(throwing.failure,
                expect(IllegalStateException.class,
                        () -> handleAdapt(handle, throwing, Object.class), "method handle exception"),
                "method handle exception identity");
        WrongResult wrong = new WrongResult();
        same(wrong.result, handleAdapt(handle, wrong, String.class), "method handle raw override result");
        MethodHandle checked = handle.asType(MethodType.methodType(String.class,
                Object.class, Class.class));
        expect(ClassCastException.class, () -> {
            String ignored = (String) checked.invokeExact((Object) wrong, (Class<?>) String.class);
        }, "typed method handle rejects wrong result");
    }

    private static Object handleAdapt(MethodHandle handle, Object receiver, Class<?> type)
            throws Throwable {
        return (Object) handle.invokeExact(receiver, type);
    }

    private static void classLoaderIdentity(Method method, MethodHandle handle) throws Throwable {
        String name = LoaderPayload.class.getName();
        byte[] bytes;
        try (InputStream input = ObjectAdapterNativeTest.class.getResourceAsStream(
                "/" + name.replace('.', '/') + ".class")) {
            check(input != null, "loader payload class resource exists");
            bytes = input.readAllBytes();
        }
        Class<?> firstType = new PayloadLoader().define(name, bytes);
        Class<?> secondType = new PayloadLoader().define(name, bytes);
        check(firstType != secondType && firstType.getName().equals(secondType.getName()),
                "same binary name has two distinct Class identities");
        Object first = firstType.getConstructor().newInstance();
        Object second = secondType.getConstructor().newInstance();
        Object firstArray = Array.newInstance(firstType, 1);
        Object secondArray = Array.newInstance(secondType, 1);
        M3Object firstOwner = M3Object.of(first);
        M3Object secondOwner = M3Object.of(second);
        check(firstOwner != secondOwner, "separately loaded instances retain distinct owners");
        for (NativeMode mode : NativeMode.values()) {
            same(first, rawAdapt(first, firstType, mode), mode + " first loader identity");
            same(second, rawAdapt(second, secondType, mode), mode + " second loader identity");
            same(null, rawAdapt(first, secondType, mode), mode + " rejects same-name foreign class");
            same(null, rawAdapt(second, firstType, mode), mode + " rejects reverse foreign class");
            same(firstArray, rawAdapt(firstArray, firstArray.getClass(), mode),
                    mode + " loader array identity");
            same(null, rawAdapt(firstArray, secondArray.getClass(), mode),
                    mode + " rejects same-name foreign array class");
            same(firstArray, adapt(firstArray, Object[].class, mode),
                    mode + " loader array reference covariance");
            same(firstOwner, adapt(first, M3Object.class, mode), mode + " first retained owner");
            same(secondOwner, adapt(second, M3Object.class, mode), mode + " second retained owner");
        }
        same(first, firstOwner.source(), "first loader capability source");
        same(second, secondOwner.source(), "second loader capability source");
        same(first, method.invoke(first, firstType), "reflection loader identity");
        same(null, method.invoke(first, secondType), "reflection rejects foreign class identity");
        same(first, handleAdapt(handle, first, firstType), "method handle loader identity");
        same(null, handleAdapt(handle, first, secondType),
                "method handle rejects foreign class identity");
    }

    private static <T extends Throwable> T expect(Class<T> type, Action action, String label)
            throws Throwable {
        try {
            action.run();
        } catch (Throwable actual) {
            check(type.isInstance(actual), label + ": expected " + type.getName()
                    + ", got " + actual);
            return type.cast(actual);
        }
        throw new AssertionError(label + ": expected " + type.getName());
    }

    private static void same(Object expected, Object actual, String label) {
        check(expected == actual, label);
    }

    private static void check(boolean condition, String label) {
        assertions++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
