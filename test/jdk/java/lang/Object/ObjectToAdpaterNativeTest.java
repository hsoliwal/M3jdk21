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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details.
 */

/*
 * @test
 * @summary Both overridable Object adapters preserve Java, JNI, reflection, method-handle and interface dispatch
 * @run main/othervm/native -Xcheck:jni ObjectToAdpaterNativeTest disabled
 * @run main/othervm/native -Xcheck:jni -Djdk.m3.objectEvents.enabled=true ObjectToAdpaterNativeTest enabled
 * @run main/othervm/native -Xcheck:jni -Xint -Djdk.m3.objectEvents.enabled=false ObjectToAdpaterNativeTest disabled
 * @run main/othervm/native -Xcheck:jni -Xint -Djdk.m3.objectEvents.enabled=true ObjectToAdpaterNativeTest enabled
 * @run main/othervm/native -Xcheck:jni -Xcomp -Xbatch -XX:-TieredCompilation -Djdk.m3.objectEvents.enabled=false ObjectToAdpaterNativeTest disabled
 * @run main/othervm/native -Xcheck:jni -Xcomp -Xbatch -XX:-TieredCompilation -Djdk.m3.objectEvents.enabled=true ObjectToAdpaterNativeTest enabled
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

public final class ObjectToAdpaterNativeTest {
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

    private static final class LegacyOverride {
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

    private static final class ThrowsFromAdapter {
        final IllegalStateException sentinel = new IllegalStateException("toAdapter override");
        int calls;

        @Override
        public <T> T toAdapter(Class<T> type) {
            calls++;
            throw sentinel;
        }
    }

    private interface AliasApi {
        <T> T toAdpater(Class<T> type);
    }

    private static final class AliasApiImpl implements AliasApi {}

    private static final class AliasOverride {
        final AdapterToken token = new AdapterToken();
        int calls;

        @Override
        public <T> T toAdpater(Class<T> type) {
            calls++;
            if (type == AdapterToken.class) {
                return type.cast(token);
            }
            return super.toAdpater(type);
        }
    }

    private static final class ThrowsFromAlias {
        final IllegalStateException failure = new IllegalStateException("toAdpater override");
        int calls;

        @Override
        public <T> T toAdpater(Class<T> type) {
            calls++;
            throw failure;
        }
    }

    private static final class AliasAcceptsNull {
        final Object result = new Object();
        int calls;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T toAdpater(Class<T> type) {
            calls++;
            if (type == null) {
                return (T) result;
            }
            return super.toAdpater(type);
        }
    }

    private static final class AliasWrongResult {
        final Object result = new Object();

        @Override
        @SuppressWarnings("unchecked")
        public <T> T toAdpater(Class<T> type) {
            return (T) result;
        }
    }

    private static final class BothOverrides {
        final AdapterToken token = new AdapterToken();
        final Object nullResult = new Object();
        final IllegalStateException failure = new IllegalStateException("both overrides sentinel");
        int adapterCalls;
        int aliasCalls;

        @Override
        public <T> T toAdpater(Class<T> type) {
            aliasCalls++;
            return super.toAdpater(type);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T toAdapter(Class<T> type) {
            adapterCalls++;
            if (type == null) {
                return (T) nullResult;
            }
            if (type == AdapterToken.class) {
                return type.cast(token);
            }
            if (type == Runnable.class) {
                throw failure;
            }
            return super.toAdapter(type);
        }
    }

    private static final class AcceptsNull {
        final Object result = new Object();
        int calls;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T toAdapter(Class<T> type) {
            calls++;
            // Delegation must reach this override before Object validates type.
            if (type == null) {
                return (T) result;
            }
            return super.toAdapter(type);
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

    private static native Object invoke0(Object receiver, Class<?> type, int mode, boolean alias);

    private static native int repeat0(Object receiver, Class<?> type, Object expected,
                                      int count, int mode, boolean alias);

    public static void main(String[] args) throws Throwable {
        if (args.length > 1 || (args.length == 1
                && !(args[0].equals("enabled") || args[0].equals("disabled")))) {
            throw new IllegalArgumentException("expected optional enabled or disabled");
        }
        String observation = args.length == 0
                ? (Boolean.getBoolean("jdk.m3.objectEvents.enabled") ? "enabled" : "disabled")
                : args[0];
        System.loadLibrary("ObjectToAdpaterNativeTest");
        boolean enabled = observation.equals("enabled");
        check(M3Object.observationEnabled() == enabled, "startup observation setting");
        interfaceDispatch();

        for (boolean alias : new boolean[] {false, true}) {
            String methodName = alias ? "toAdpater" : "toAdapter";
            int previousAssertions = assertions;
            long previousRepeatedCalls = repeatedCalls;
            javaDispatch(alias);
            for (NativeMode mode : NativeMode.values()) {
                ordinaryReceivers(mode, alias);
                arrayReceivers(mode, alias);
                overrideDispatch(mode, alias);
                boundedNativeFrame(mode, alias);
            }

            Method method = Object.class.getDeclaredMethod(methodName, Class.class);
            genericSignature(method, alias);
            reflectionDispatch(method, alias);
            MethodHandle handle = MethodHandles.publicLookup().findVirtual(Object.class,
                    methodName, MethodType.methodType(Object.class, Class.class));
            methodHandleDispatch(handle, alias);
            classLoaderIdentity(method, handle, alias);
            System.out.println("Object." + methodName + " passed: "
                    + (assertions - previousAssertions) + " assertions, "
                    + (repeatedCalls - previousRepeatedCalls) + " repeated JNI calls");
        }
        System.out.println("ObjectToAdpaterNativeTest PASS checks=" + assertions
                + " repeatedJNI=" + repeatedCalls + " observation=" + observation);
    }

    private static Object rawAdapt(Object receiver, Class<?> type, NativeMode mode, boolean alias) {
        // CallObjectMethod requires a non-null receiver. Reject it in Java;
        // the type token and raw return value cross JNI without extra checks.
        return invoke0(Objects.requireNonNull(receiver, "receiver"), type, mode.value, alias);
    }

    private static <T> T adapt(Object receiver, Class<T> type, NativeMode mode, boolean alias) {
        return type.cast(rawAdapt(receiver, type, mode, alias));
    }

    private static <T> T javaAdapt(Object receiver, Class<T> type, boolean alias) {
        return alias ? receiver.toAdpater(type) : receiver.toAdapter(type);
    }

    private static void interfaceDispatch() throws ReflectiveOperationException {
        AliasApi value = new AliasApiImpl();
        same(value, value.toAdpater(AliasApi.class), "interface invokes inherited Object alias");
        same(value, ((Object) value).toAdpater(AliasApi.class), "Object invokes interface owner's alias");
        same(value, ((Object) value).toAdapter(AliasApi.class), "canonical interface owner identity");
        same(Object.class, AliasApiImpl.class.getMethod("toAdpater", Class.class).getDeclaringClass(),
                "interface implementation inherits the Object method");
    }

    private static void javaDispatch(boolean alias) throws Throwable {
        Object source = new Object();
        same(source, javaAdapt(source, Object.class, alias), "Java identity");
        same(null, javaAdapt(source, String.class, alias), "Java unsupported type");
        same(null, javaAdapt(source, Runnable.class, alias), "Java unsupported interface type");
        M3Object owner = M3Object.of(source);
        same(owner, javaAdapt(source, M3Object.class, alias), "Java retained capability");
        same(owner, source.toAdapter(M3Object.class), "Java existing adapter consistency");
        expect(NullPointerException.class, () -> javaAdapt(source, null, alias),
                "Java null token");
        LegacyOverride overridden = new LegacyOverride();
        same(overridden.token, javaAdapt(overridden, AdapterToken.class, alias),
                "Java invokes subclass override");
        check(overridden.calls == 1, "Java invokes override exactly once");
        AcceptsNull acceptsNull = new AcceptsNull();
        same(acceptsNull.result, javaAdapt(acceptsNull, null, alias),
                "Java null token reaches the overriding method");
        check(acceptsNull.calls == 1, "Java null-token override called once");
        ThrowsFromAdapter throwing = new ThrowsFromAdapter();
        same(throwing.sentinel,
                expect(IllegalStateException.class,
                        () -> javaAdapt(throwing, Object.class, alias), "Java override exception"),
                "Java exception identity");
        check(throwing.calls == 1, "Java throwing override called once");
        javaAliasDispatch(alias);
    }

    private static void ordinaryReceivers(NativeMode mode, boolean alias) throws Throwable {
        Object source = new Object();
        same(source, adapt(source, Object.class, mode, alias), mode + " Object identity");
        String text = "native-adapter";
        same(text, adapt(text, String.class, mode, alias), mode + " String identity");
        same(text, adapt(text, CharSequence.class, mode, alias), mode + " interface identity");
        Integer number = Integer.valueOf(37);
        same(number, adapt(number, Number.class, mode, alias), mode + " superclass identity");
        same(String.class, adapt(String.class, Class.class, mode, alias), mode + " Class identity");
        for (Class<?> type : new Class<?>[] {String.class, Runnable.class, int.class,
                void.class, ObjectEvent.class, M3Object.Field.class}) {
            same(null, rawAdapt(source, type, mode, alias), mode + " unsupported " + type);
        }
        expect(NullPointerException.class, () -> rawAdapt(source, null, mode, alias),
                mode + " null type token");
        expect(NullPointerException.class, () -> rawAdapt(null, Object.class, mode, alias),
                mode + " Java wrapper rejects null receiver");
        capability(source, mode, alias);
    }

    private static void arrayReceivers(NativeMode mode, boolean alias) throws Throwable {
        Object[] arrays = {
                new boolean[0], new byte[1], new short[1], new char[1],
                new int[0], new int[1], new long[1], new float[1], new double[1],
                new Object[1], new String[1], new int[1][1], new Object[1][1]
        };
        for (Object array : arrays) {
            String label = mode + " " + array.getClass().getTypeName();
            same(array, rawAdapt(array, array.getClass(), mode, alias), label + " exact identity");
            same(array, adapt(array, Object.class, mode, alias), label + " Object identity");
            same(array, adapt(array, Cloneable.class, mode, alias), label + " Cloneable identity");
            same(array, adapt(array, Serializable.class, mode, alias), label + " Serializable identity");
            same(array instanceof Object[] ? array : null,
                    adapt(array, Object[].class, mode, alias), label + " reference covariance");
            same(null, adapt(array, String.class, mode, alias), label + " unsupported type");
            expect(NullPointerException.class, () -> rawAdapt(array, null, mode, alias),
                    label + " null type token");
            capability(array, mode, alias);
        }
    }

    private static void capability(Object source, NativeMode mode, boolean alias) {
        M3Object owner = adapt(source, M3Object.class, mode, alias);
        check(owner != null, mode + " exact M3Object capability");
        same(source, owner.source(), mode + " capability source");
        same(owner, adapt(source, M3Object.class, mode, alias), mode + " retained capability identity");
        same(owner, M3Object.of(source), mode + " existing owner identity");
        same(owner, adapt(owner, M3Object.class, mode, alias), mode + " assignability before capability");
        same(owner, adapt(owner, Object.class, mode, alias), mode + " capability Object identity");
    }

    private static void overrideDispatch(NativeMode mode, boolean alias) throws Throwable {
        LegacyOverride source = new LegacyOverride();
        same(source.token, adapt(source, AdapterToken.class, mode, alias), mode + " virtual override");
        check(source.calls == 1, mode + " legacy override dispatch count");
        same(source, adapt(source, Object.class, mode, alias), mode + " delegated identity");
        M3Object owner = adapt(source, M3Object.class, mode, alias);
        same(source, owner.source(), mode + " delegated capability source");
        same(owner, M3Object.of(source), mode + " delegated existing owner");
        same(null, adapt(source, Runnable.class, mode, alias), mode + " delegated unsupported type");
        expect(NullPointerException.class, () -> rawAdapt(source, null, mode, alias),
                mode + " delegated null type token");
        check(source.calls == 5, mode + " each call reaches the override exactly once");

        AcceptsNull acceptsNull = new AcceptsNull();
        same(acceptsNull.result, rawAdapt(acceptsNull, null, mode, alias),
                mode + " null token reaches the overriding method");
        check(acceptsNull.calls == 1, mode + " null-token override called once");

        ThrowsFromAdapter throwing = new ThrowsFromAdapter();
        same(throwing.sentinel,
                expect(IllegalStateException.class,
                        () -> rawAdapt(throwing, Object.class, mode, alias), mode + " override exception"),
                mode + " exception identity survives JNI");
        check(throwing.calls == 1, mode + " throwing override called once");

        WrongResult wrong = new WrongResult();
        same(wrong.result, rawAdapt(wrong, String.class, mode, alias), mode + " raw override result");
        expect(ClassCastException.class, () -> adapt(wrong, String.class, mode, alias),
                mode + " checked Java wrapper rejects wrong result");
        expect(ClassCastException.class, () -> {
            if (alias) {
                ((Object) wrong).toAdpater(String.class).length();
            } else {
                ((Object) wrong).toAdapter(String.class).length();
            }
        }, mode + " ordinary typed Java consumer rejects wrong result");
        nativeAliasDispatch(mode, alias);
    }

    private static void boundedNativeFrame(NativeMode mode, boolean alias) throws Throwable {
        Object source = new Object();
        M3Object owner = M3Object.of(source);
        repeat(source, Object.class, source, mode, alias, "identity");
        repeat(source, M3Object.class, owner, mode, alias, "retained capability");
        repeat(source, String.class, null, mode, alias, "unsupported type");
        int[] array = {3, 5, 7};
        repeat(array, int[].class, array, mode, alias, "primitive array");
        LegacyOverride overridden = new LegacyOverride();
        repeat(overridden, AdapterToken.class, overridden.token, mode, alias, "virtual override");
        check(overridden.calls == REPEATED_CALLS, mode + " repeated override call count");
        AliasOverride aliasOverride = new AliasOverride();
        repeat(aliasOverride, AdapterToken.class, alias ? aliasOverride.token : null,
                mode, alias, "own-alias override");
        check(aliasOverride.calls == (alias ? REPEATED_CALLS : 0),
                mode + " repeated own-alias call count and canonical independence");
        BothOverrides both = new BothOverrides();
        repeat(both, AdapterToken.class, both.token, mode, alias, "super alias to canonical override");
        bothCounts(both, REPEATED_CALLS, alias, mode + " repeated super delegation");
        same(source, owner.source(), mode + " capability remains retained throughout native loop");

        ThrowsFromAdapter throwing = new ThrowsFromAdapter();
        same(throwing.sentinel,
                expect(IllegalStateException.class,
                        () -> repeat0(throwing, Object.class, null, REPEATED_CALLS, mode.value, alias),
                        mode + " repeated override exception"),
                mode + " pending exception survives local-frame cleanup");
        check(throwing.calls == 1, mode + " native loop stops at its first exception");
        if (alias) {
            ThrowsFromAlias aliasThrowing = new ThrowsFromAlias();
            same(aliasThrowing.failure,
                    expect(IllegalStateException.class,
                            () -> repeat0(aliasThrowing, Object.class, null,
                                    REPEATED_CALLS, mode.value, true),
                            mode + " repeated own-alias exception"),
                    mode + " own-alias exception survives local-frame cleanup");
            check(aliasThrowing.calls == 1, mode + " native loop stops at first own-alias exception");
        }
        expect(NullPointerException.class,
                () -> repeat0(source, null, null, REPEATED_CALLS, mode.value, alias),
                mode + " null token survives local-frame cleanup");
    }

    private static void repeat(Object source, Class<?> type, Object expected,
                               NativeMode mode, boolean alias, String label) {
        int completed = repeat0(Objects.requireNonNull(source), type, expected,
                REPEATED_CALLS, mode.value, alias);
        check(completed == REPEATED_CALLS, mode + " repeated " + label
                + ": completed " + completed + " of " + REPEATED_CALLS);
        repeatedCalls += completed;
    }

    private static void genericSignature(Method method, boolean alias) {
        same(Object.class, method.getDeclaringClass(), "reflection declares Object method");
        same(Object.class, method.getReturnType(), "erased return type is Object");
        check(method.getName().equals(alias ? "toAdpater" : "toAdapter"),
                "reflection resolves selected entrypoint");
        check(Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())
                && !Modifier.isNative(method.getModifiers()), "public Java instance method");
        check(!Modifier.isFinal(method.getModifiers()),
                "both adapter entrypoints remain overridable");
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

    private static void reflectionDispatch(Method method, boolean alias) throws Throwable {
        Object source = new Object();
        M3Object owner = M3Object.of(source);
        same(source, method.invoke(source, Object.class), "reflection identity");
        same(null, method.invoke(source, String.class), "reflection unsupported type");
        same(owner, method.invoke(source, M3Object.class), "reflection retained capability");
        InvocationTargetException nullToken = expect(InvocationTargetException.class,
                () -> method.invoke(source, new Object[] {null}), "reflection null token");
        check(nullToken.getCause() instanceof NullPointerException, "reflection wraps null-token NPE");
        LegacyOverride overridden = new LegacyOverride();
        same(overridden.token, method.invoke(overridden, AdapterToken.class),
                "Object Method invokes subclass override");
        check(overridden.calls == 1, "reflection invokes override exactly once");
        AcceptsNull acceptsNull = new AcceptsNull();
        same(acceptsNull.result, method.invoke(acceptsNull, new Object[] {null}),
                "reflection null token reaches the overriding method");
        check(acceptsNull.calls == 1, "reflection null-token override called once");
        ThrowsFromAdapter throwing = new ThrowsFromAdapter();
        InvocationTargetException failure = expect(InvocationTargetException.class,
                () -> method.invoke(throwing, Object.class), "reflection wraps override exception");
        same(throwing.sentinel, failure.getCause(), "reflection exception cause identity");
        check(throwing.calls == 1, "reflection throwing override called once");
        WrongResult wrong = new WrongResult();
        same(wrong.result, method.invoke(wrong, String.class), "reflection returns raw override result");
        reflectionAliasDispatch(method, alias);
    }

    private static void methodHandleDispatch(MethodHandle handle, boolean alias) throws Throwable {
        Object source = new Object();
        M3Object owner = M3Object.of(source);
        same(source, handleAdapt(handle, source, Object.class), "method handle identity");
        same(null, handleAdapt(handle, source, String.class), "method handle unsupported type");
        same(owner, handleAdapt(handle, source, M3Object.class), "method handle retained capability");
        expect(NullPointerException.class, () -> handleAdapt(handle, source, null),
                "method handle null token");
        LegacyOverride overridden = new LegacyOverride();
        same(overridden.token, handleAdapt(handle, overridden, AdapterToken.class),
                "Object method handle invokes subclass override");
        check(overridden.calls == 1, "method handle invokes override exactly once");
        AcceptsNull acceptsNull = new AcceptsNull();
        same(acceptsNull.result, handleAdapt(handle, acceptsNull, null),
                "method handle null token reaches the overriding method");
        check(acceptsNull.calls == 1, "method handle null-token override called once");
        ThrowsFromAdapter throwing = new ThrowsFromAdapter();
        same(throwing.sentinel,
                expect(IllegalStateException.class,
                        () -> handleAdapt(handle, throwing, Object.class), "method handle exception"),
                "method handle exception identity");
        check(throwing.calls == 1, "method handle throwing override called once");
        WrongResult wrong = new WrongResult();
        same(wrong.result, handleAdapt(handle, wrong, String.class), "method handle raw override result");
        MethodHandle checked = handle.asType(MethodType.methodType(String.class,
                Object.class, Class.class));
        expect(ClassCastException.class, () -> {
            String ignored = (String) checked.invokeExact((Object) wrong, (Class<?>) String.class);
        }, "typed method handle rejects wrong result");
        methodHandleAliasDispatch(handle, alias);
    }

    private static Object handleAdapt(MethodHandle handle, Object receiver, Class<?> type)
            throws Throwable {
        return (Object) handle.invokeExact(receiver, type);
    }

    private static void classLoaderIdentity(Method method, MethodHandle handle, boolean alias)
            throws Throwable {
        String name = LoaderPayload.class.getName();
        byte[] bytes;
        try (InputStream input = ObjectToAdpaterNativeTest.class.getResourceAsStream(
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
            same(first, rawAdapt(first, firstType, mode, alias), mode + " first loader identity");
            same(second, rawAdapt(second, secondType, mode, alias), mode + " second loader identity");
            same(null, rawAdapt(first, secondType, mode, alias), mode + " rejects same-name foreign class");
            same(null, rawAdapt(second, firstType, mode, alias), mode + " rejects reverse foreign class");
            same(firstArray, rawAdapt(firstArray, firstArray.getClass(), mode, alias),
                    mode + " loader array identity");
            same(null, rawAdapt(firstArray, secondArray.getClass(), mode, alias),
                    mode + " rejects same-name foreign array class");
            same(firstArray, adapt(firstArray, Object[].class, mode, alias),
                    mode + " loader array reference covariance");
            same(firstOwner, adapt(first, M3Object.class, mode, alias), mode + " first retained owner");
            same(secondOwner, adapt(second, M3Object.class, mode, alias), mode + " second retained owner");
        }
        same(first, firstOwner.source(), "first loader capability source");
        same(second, secondOwner.source(), "second loader capability source");
        same(first, method.invoke(first, firstType), "reflection loader identity");
        same(null, method.invoke(first, secondType), "reflection rejects foreign class identity");
        same(first, handleAdapt(handle, first, firstType), "method handle loader identity");
        same(null, handleAdapt(handle, first, secondType),
                "method handle rejects foreign class identity");
    }

    private static void javaAliasDispatch(boolean alias) throws Throwable {
        AliasOverride source = new AliasOverride();
        same(alias ? source.token : null, javaAdapt(source, AdapterToken.class, alias),
                "selected own-alias override");
        check(source.calls == (alias ? 1 : 0), "Java own-alias initial dispatch count");
        same(source, javaAdapt(source, Object.class, alias), "own-alias delegated identity");
        same(M3Object.of(source), javaAdapt(source, M3Object.class, alias),
                "own-alias delegated capability");
        same(null, javaAdapt(source, Runnable.class, alias), "own-alias unsupported type");
        expect(NullPointerException.class, () -> javaAdapt(source, null, alias),
                "own-alias delegated null token");
        check(source.calls == (alias ? 5 : 0), "own-alias count and canonical independence");

        AliasAcceptsNull acceptsNull = new AliasAcceptsNull();
        if (alias) {
            same(acceptsNull.result, javaAdapt(acceptsNull, null, alias),
                    "own-alias null-token override");
        } else {
            expect(NullPointerException.class, () -> javaAdapt(acceptsNull, null, alias),
                    "canonical null validation is independent of own-alias override");
        }
        check(acceptsNull.calls == (alias ? 1 : 0), "own-alias null-token call count");
        ThrowsFromAlias throwing = new ThrowsFromAlias();
        if (alias) {
            same(throwing.failure, expect(IllegalStateException.class,
                    () -> javaAdapt(throwing, Object.class, alias), "own-alias exception"),
                    "own-alias exception identity");
        } else {
            same(throwing, javaAdapt(throwing, Object.class, alias),
                    "canonical API ignores own-alias exception");
        }
        check(throwing.calls == (alias ? 1 : 0), "own-alias throwing call count");
        AliasWrongResult wrong = new AliasWrongResult();
        same(alias ? wrong.result : null, javaAdapt(wrong, String.class, alias),
                "own-alias raw result and canonical independence");
        if (alias) {
            expect(ClassCastException.class, () -> ((Object) wrong).toAdpater(String.class).length(),
                    "typed Java consumer rejects own-alias wrong result");
        } else {
            same(null, ((Object) wrong).toAdapter(String.class),
                    "typed canonical result is independent of own-alias wrong result");
        }

        BothOverrides both = new BothOverrides();
        same(both.token, javaAdapt(both, AdapterToken.class, alias),
                "super alias reaches canonical override");
        bothCounts(both, 1, alias, "super token delegation");
        same(both.nullResult, javaAdapt(both, null, alias), "super alias preserves null acceptance");
        bothCounts(both, 2, alias, "super null delegation");
        same(both.failure, expect(IllegalStateException.class,
                () -> javaAdapt(both, Runnable.class, alias), "super alias exception"),
                "super alias preserves exception identity");
        bothCounts(both, 3, alias, "super throwing delegation");
    }

    private static void nativeAliasDispatch(NativeMode mode, boolean alias) throws Throwable {
        AliasOverride source = new AliasOverride();
        same(alias ? source.token : null, adapt(source, AdapterToken.class, mode, alias),
                mode + " selected own-alias override");
        check(source.calls == (alias ? 1 : 0), mode + " own-alias initial dispatch count");
        same(source, adapt(source, Object.class, mode, alias), mode + " own-alias delegated identity");
        same(M3Object.of(source), adapt(source, M3Object.class, mode, alias),
                mode + " own-alias delegated capability");
        same(null, adapt(source, Runnable.class, mode, alias), mode + " own-alias unsupported type");
        expect(NullPointerException.class, () -> rawAdapt(source, null, mode, alias),
                mode + " own-alias delegated null token");
        check(source.calls == (alias ? 5 : 0), mode + " own-alias count and canonical independence");

        AliasAcceptsNull acceptsNull = new AliasAcceptsNull();
        if (alias) {
            same(acceptsNull.result, rawAdapt(acceptsNull, null, mode, alias),
                    mode + " own-alias null-token override");
        } else {
            expect(NullPointerException.class, () -> rawAdapt(acceptsNull, null, mode, alias),
                    mode + " canonical null validation is independent of own-alias override");
        }
        check(acceptsNull.calls == (alias ? 1 : 0), mode + " own-alias null-token call count");
        ThrowsFromAlias throwing = new ThrowsFromAlias();
        if (alias) {
            same(throwing.failure, expect(IllegalStateException.class,
                    () -> rawAdapt(throwing, Object.class, mode, alias), mode + " own-alias exception"),
                    mode + " own-alias exception identity");
        } else {
            same(throwing, rawAdapt(throwing, Object.class, mode, alias),
                    mode + " canonical API ignores own-alias exception");
        }
        check(throwing.calls == (alias ? 1 : 0), mode + " own-alias throwing call count");
        AliasWrongResult wrong = new AliasWrongResult();
        same(alias ? wrong.result : null, rawAdapt(wrong, String.class, mode, alias),
                mode + " own-alias raw result and canonical independence");
        if (alias) {
            expect(ClassCastException.class, () -> adapt(wrong, String.class, mode, alias),
                    mode + " checked wrapper rejects own-alias wrong result");
        } else {
            same(null, adapt(wrong, String.class, mode, alias),
                    mode + " checked canonical result is independent of own-alias wrong result");
        }

        BothOverrides both = new BothOverrides();
        same(both.token, adapt(both, AdapterToken.class, mode, alias),
                mode + " super alias reaches canonical override");
        bothCounts(both, 1, alias, mode + " super token delegation");
        same(both.nullResult, rawAdapt(both, null, mode, alias), mode + " super alias preserves null acceptance");
        bothCounts(both, 2, alias, mode + " super null delegation");
        same(both.failure, expect(IllegalStateException.class,
                () -> rawAdapt(both, Runnable.class, mode, alias), mode + " super alias exception"),
                mode + " super alias preserves exception identity");
        bothCounts(both, 3, alias, mode + " super throwing delegation");
    }

    private static void reflectionAliasDispatch(Method method, boolean alias) throws Throwable {
        AliasOverride source = new AliasOverride();
        same(alias ? source.token : null, method.invoke(source, AdapterToken.class),
                "reflection selected own-alias override");
        check(source.calls == (alias ? 1 : 0), "reflection own-alias initial dispatch count");
        same(source, method.invoke(source, Object.class), "reflection own-alias delegated identity");
        same(M3Object.of(source), method.invoke(source, M3Object.class),
                "reflection own-alias delegated capability");
        same(null, method.invoke(source, Runnable.class), "reflection own-alias unsupported type");
        InvocationTargetException nullToken = expect(InvocationTargetException.class,
                () -> method.invoke(source, new Object[] {null}), "reflection own-alias null token");
        check(nullToken.getCause() instanceof NullPointerException, "reflection wraps own-alias null-token NPE");
        check(source.calls == (alias ? 5 : 0), "reflection own-alias count and canonical independence");

        AliasAcceptsNull acceptsNull = new AliasAcceptsNull();
        if (alias) {
            same(acceptsNull.result, method.invoke(acceptsNull, new Object[] {null}),
                    "reflection own-alias null-token override");
        } else {
            InvocationTargetException failure = expect(InvocationTargetException.class,
                    () -> method.invoke(acceptsNull, new Object[] {null}),
                    "reflection canonical null validation is independent of own-alias override");
            check(failure.getCause() instanceof NullPointerException, "canonical null-token NPE cause");
        }
        check(acceptsNull.calls == (alias ? 1 : 0), "reflection own-alias null-token call count");
        ThrowsFromAlias throwing = new ThrowsFromAlias();
        if (alias) {
            InvocationTargetException failure = expect(InvocationTargetException.class,
                    () -> method.invoke(throwing, Object.class), "reflection own-alias exception");
            same(throwing.failure, failure.getCause(), "reflection own-alias exception identity");
        } else {
            same(throwing, method.invoke(throwing, Object.class),
                    "reflection canonical API ignores own-alias exception");
        }
        check(throwing.calls == (alias ? 1 : 0), "reflection own-alias throwing call count");
        AliasWrongResult wrong = new AliasWrongResult();
        same(alias ? wrong.result : null, method.invoke(wrong, String.class),
                "reflection own-alias raw result and canonical independence");

        BothOverrides both = new BothOverrides();
        same(both.token, method.invoke(both, AdapterToken.class), "reflection super alias reaches canonical override");
        bothCounts(both, 1, alias, "reflection super token delegation");
        same(both.nullResult, method.invoke(both, new Object[] {null}), "reflection super alias preserves null acceptance");
        bothCounts(both, 2, alias, "reflection super null delegation");
        InvocationTargetException failure = expect(InvocationTargetException.class,
                () -> method.invoke(both, Runnable.class), "reflection super alias exception");
        same(both.failure, failure.getCause(), "reflection super alias preserves exception identity");
        bothCounts(both, 3, alias, "reflection super throwing delegation");
    }

    private static void methodHandleAliasDispatch(MethodHandle handle, boolean alias) throws Throwable {
        AliasOverride source = new AliasOverride();
        same(alias ? source.token : null, handleAdapt(handle, source, AdapterToken.class),
                "method handle selected own-alias override");
        check(source.calls == (alias ? 1 : 0), "method handle own-alias initial dispatch count");
        same(source, handleAdapt(handle, source, Object.class), "method handle own-alias delegated identity");
        same(M3Object.of(source), handleAdapt(handle, source, M3Object.class),
                "method handle own-alias delegated capability");
        same(null, handleAdapt(handle, source, Runnable.class), "method handle own-alias unsupported type");
        expect(NullPointerException.class, () -> handleAdapt(handle, source, null),
                "method handle own-alias delegated null token");
        check(source.calls == (alias ? 5 : 0), "method handle own-alias count and canonical independence");

        AliasAcceptsNull acceptsNull = new AliasAcceptsNull();
        if (alias) {
            same(acceptsNull.result, handleAdapt(handle, acceptsNull, null),
                    "method handle own-alias null-token override");
        } else {
            expect(NullPointerException.class, () -> handleAdapt(handle, acceptsNull, null),
                    "method handle canonical null validation is independent of own-alias override");
        }
        check(acceptsNull.calls == (alias ? 1 : 0), "method handle own-alias null-token call count");
        ThrowsFromAlias throwing = new ThrowsFromAlias();
        if (alias) {
            same(throwing.failure, expect(IllegalStateException.class,
                    () -> handleAdapt(handle, throwing, Object.class), "method handle own-alias exception"),
                    "method handle own-alias exception identity");
        } else {
            same(throwing, handleAdapt(handle, throwing, Object.class),
                    "method handle canonical API ignores own-alias exception");
        }
        check(throwing.calls == (alias ? 1 : 0), "method handle own-alias throwing call count");
        AliasWrongResult wrong = new AliasWrongResult();
        same(alias ? wrong.result : null, handleAdapt(handle, wrong, String.class),
                "method handle own-alias raw result and canonical independence");
        MethodHandle checked = handle.asType(MethodType.methodType(String.class,
                Object.class, Class.class));
        if (alias) {
            expect(ClassCastException.class, () -> {
                String ignored = (String) checked.invokeExact((Object) wrong, (Class<?>) String.class);
            }, "typed method handle rejects own-alias wrong result");
        } else {
            String result = (String) checked.invokeExact((Object) wrong, (Class<?>) String.class);
            same(null, result, "typed canonical handle is independent of own-alias wrong result");
        }

        BothOverrides both = new BothOverrides();
        same(both.token, handleAdapt(handle, both, AdapterToken.class),
                "method handle super alias reaches canonical override");
        bothCounts(both, 1, alias, "method handle super token delegation");
        same(both.nullResult, handleAdapt(handle, both, null), "method handle super alias preserves null acceptance");
        bothCounts(both, 2, alias, "method handle super null delegation");
        same(both.failure, expect(IllegalStateException.class,
                () -> handleAdapt(handle, both, Runnable.class), "method handle super alias exception"),
                "method handle super alias preserves exception identity");
        bothCounts(both, 3, alias, "method handle super throwing delegation");
    }

    private static void bothCounts(BothOverrides source, int expected, boolean alias, String label) {
        check(source.adapterCalls == expected, label + " canonical exactly-once count");
        check(source.aliasCalls == (alias ? expected : 0), label + " own-alias exactly-once count");
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
