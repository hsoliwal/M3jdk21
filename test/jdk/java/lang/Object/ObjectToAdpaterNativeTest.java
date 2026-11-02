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
 * @summary Object.toAdpater alias preserves Object.toAdapter semantics and JNI virtual dispatch
 * @run main/othervm/native -Xcheck:jni ObjectToAdpaterNativeTest
 * @run main/othervm/native -Xcheck:jni -Xint ObjectToAdpaterNativeTest
 * @run main/othervm/native -Xcheck:jni -Xcomp -Xbatch -XX:-TieredCompilation ObjectToAdpaterNativeTest
 */

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;

public final class ObjectToAdpaterNativeTest {
    private static int checks;

    private static native Object invoke0(Object receiver, Class<?> type, int mode);

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

    private static final class ThrowsFromAdapter {
        final IllegalStateException sentinel = new IllegalStateException("toAdapter override");

        @Override
        public <T> T toAdapter(Class<T> type) {
            throw sentinel;
        }
    }

    private static void same(Object expected, Object actual, String message) {
        check(expected == actual, message);
    }

    private static void check(boolean valid, String message) {
        checks++;
        if (!valid) {
            throw new AssertionError(message);
        }
    }

    private static void expectNullToken(Object receiver, int mode) {
        try {
            invoke0(receiver, null, mode);
            throw new AssertionError("missing NullPointerException");
        } catch (NullPointerException expected) {
            checks++;
        }
    }

    public static void main(String[] ignored) throws Exception {
        System.loadLibrary("ObjectToAdpaterNativeTest");
        Method alias = Object.class.getDeclaredMethod("toAdpater", Class.class);
        Method existing = Object.class.getDeclaredMethod("toAdapter", Class.class);
        check(Modifier.isPublic(alias.getModifiers()), "alias must be public");
        check(!Modifier.isFinal(alias.getModifiers()), "alias must be overridable");
        check(alias.getReturnType() == Object.class, "erased return is Object");
        TypeVariable<Method>[] typeVariables = alias.getTypeParameters();
        check(typeVariables.length == 1 && alias.getGenericReturnType().equals(typeVariables[0]),
                "return T is method type variable");
        check(existing.getDeclaringClass() == Object.class, "existing entry remains declared");

        Object plain = new Object();
        same(plain, plain.toAdpater(Object.class), "Java identity");
        same(null, plain.toAdpater(Runnable.class), "Java unsupported type");
        M3Object capability = plain.toAdpater(M3Object.class);
        same(M3Object.of(plain), capability, "lazy M3Object identity");
        same(plain.toAdapter(M3Object.class), capability, "existing adapter consistency");

        for (int mode = 0; mode < 3; mode++) {
            same(plain, invoke0(plain, Object.class, mode), "JNI identity " + mode);
            same(null, invoke0(plain, Runnable.class, mode), "JNI unsupported " + mode);
            same(capability, invoke0(plain, M3Object.class, mode), "JNI capability " + mode);
            expectNullToken(plain, mode);

            for (Object array : new Object[] {new int[0], new String[1], new Object[1]}) {
                same(array, invoke0(array, Object.class, mode), "JNI array identity " + mode);
                same(array, invoke0(array, Cloneable.class, mode), "JNI array interface " + mode);
                same(M3Object.of(array), invoke0(array, M3Object.class, mode),
                        "JNI array capability " + mode);
            }

            LegacyOverride old = new LegacyOverride();
            Object receiver = old;
            same(old.token, invoke0(receiver, AdapterToken.class, mode),
                    "alias delegates to existing virtual override " + mode);
            check(old.calls == 1, "legacy override dispatch count " + mode);

            AliasOverride current = new AliasOverride();
            receiver = current;
            same(current.token, invoke0(receiver, AdapterToken.class, mode),
                    "alias supports own virtual override " + mode);
            check(current.calls == 1, "alias override dispatch count " + mode);

            ThrowsFromAdapter throwsFromAdapter = new ThrowsFromAdapter();
            try {
                invoke0(throwsFromAdapter, Object.class, mode);
                throw new AssertionError("exception swallowed by JNI " + mode);
            } catch (IllegalStateException exception) {
                same(throwsFromAdapter.sentinel, exception,
                        "JNI preserves subclass exception identity " + mode);
            }
        }
        System.out.println("ObjectToAdpaterNativeTest PASS checks=" + checks);
    }
}
