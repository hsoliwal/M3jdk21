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
 * @summary Object adapter identity, capability, override and array dispatch
 * @run main ObjectAdapterDispatchTest
 */

import java.io.Serializable;
import java.util.Arrays;

public final class ObjectAdapterDispatchTest {
    private interface Marker {}

    private static class Base implements Marker, Cloneable {
        @Override
        public Base clone() throws CloneNotSupportedException {
            return (Base) super.clone();
        }
    }

    private static final class AdapterToken {}

    private static final class Overrides extends Base {
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

        @Override
        public int hashCode() {
            return 73;
        }

        @Override
        public boolean equals(Object other) {
            return this == other;
        }

        @Override
        public String toString() {
            return "adapter-override";
        }
    }

    public static void main(String[] args) throws Exception {
        ordinaryReceivers();
        subclassDispatch();
        arrayDispatch();
        expectNullPointer(() -> adapt(null, M3Object.class));
    }

    /*
     * Keeping the receiver statically typed as Object makes every call here
     * exercise the exact Object.toAdapter invokevirtual entry.
     */
    private static <T> T adapt(Object receiver, Class<T> type) {
        return receiver.toAdapter(type);
    }

    private static void ordinaryReceivers() {
        Object plain = new Object();
        same(plain, adapt(plain, Object.class), "Object identity");
        same(plain, adapt(plain, plain.getClass()), "exact class identity");
        check(adapt(plain, String.class) == null, "unsupported reference type");
        check(adapt(plain, int.class) == null, "primitive request");
        check(adapt(plain, void.class) == null, "void request");
        check(adapt(plain, ObjectEvent.class) == null, "unrelated capability");
        check(adapt(plain, M3Object.Field.class) == null, "field is not an adapter");
        expectNullPointer(() -> adapt(plain, null));
        capability(plain);

        String text = "adapter-dispatch";
        same(text, adapt(text, String.class), "String identity");
        same(text, adapt(text, CharSequence.class), "interface identity");
        Number number = Integer.valueOf(17);
        same(number, adapt(number, Number.class), "superclass identity");
        check(adapt(number, int.class) == null, "no unboxing adaptation");
        Class<?> type = String.class;
        same(type, adapt(type, Class.class), "Class receiver identity");

        M3Object owner = adapt(plain, M3Object.class);
        same(owner, adapt(owner, M3Object.class), "assignability before capability");
        same(owner, adapt(owner, Object.class), "capability Object identity");
        check(adapt(owner, Runnable.class) == null, "capability unsupported type");
    }

    private static void subclassDispatch() throws CloneNotSupportedException {
        Base inherited = new Base();
        Object receiver = inherited;
        same(inherited, adapt(receiver, Base.class), "inherited class identity");
        same(inherited, adapt(receiver, Marker.class), "inherited interface identity");
        check(adapt(receiver, AdapterToken.class) == null, "base unsupported type");
        capability(receiver);

        Base clone = inherited.clone();
        check(clone != inherited && clone.getClass() == Base.class,
                "Object.clone dispatch");
        capability(clone);

        Overrides overrides = new Overrides();
        receiver = overrides;
        same(overrides.token, adapt(receiver, AdapterToken.class),
                "override through Object receiver");
        check(overrides.calls == 1, "Object call bypassed override");
        same(overrides, adapt(receiver, Marker.class), "override delegates identity");
        M3Object owner = adapt(receiver, M3Object.class);
        same(overrides, owner.source(), "override delegates capability");
        same(owner, M3Object.of(overrides), "override capability canonical identity");
        check(adapt(receiver, Runnable.class) == null, "override delegates unsupported");
        Object selected = receiver;
        expectNullPointer(() -> adapt(selected, null));
        check(overrides.calls == 5, "delegated Object calls bypassed override");

        check(receiver.hashCode() == 73, "hashCode virtual dispatch");
        check(receiver.equals(overrides) && !receiver.equals(inherited),
                "equals virtual dispatch");
        check(receiver.toString().equals("adapter-override"), "toString virtual dispatch");
        Base overrideClone = overrides.clone();
        check(overrideClone != overrides && overrideClone.getClass() == Overrides.class,
                "subclass clone dispatch");
        same(overrides.token, adapt(overrideClone, AdapterToken.class),
                "cloned subclass adapter dispatch");
    }

    private static void arrayDispatch() {
        Object[] arrays = {
                new boolean[0], new byte[1], new short[1], new char[1],
                new int[1], new long[1], new float[1], new double[1],
                new Object[1], new String[1], new int[1][1], new Object[1][1]
        };
        for (Object array : arrays) {
            same(array, adapt(array, Object.class), "array Object identity");
            same(array, adapt(array, array.getClass()), "array exact class identity");
            same(array, adapt(array, Cloneable.class), "array Cloneable identity");
            same(array, adapt(array, Serializable.class), "array Serializable identity");
            if (array instanceof Object[]) {
                same(array, adapt(array, Object[].class), "reference array covariance");
            } else {
                check(adapt(array, Object[].class) == null, "primitive array covariance");
            }
            check(adapt(array, String.class) == null, "array unsupported type");
            expectNullPointer(() -> adapt(array, null));
            capability(array);
            check(array.equals(array) && !array.equals(new Object()),
                    "array equals dispatch");
            check(array.hashCode() == System.identityHashCode(array),
                    "array hashCode dispatch");
            check(array.toString().startsWith(array.getClass().getName() + "@"),
                    "array toString dispatch");
        }

        int[] values = {1, 2, 3};
        int[] valuesClone = values.clone();
        check(valuesClone != values && Arrays.equals(values, valuesClone),
                "primitive array clone");
        Object payload = new Object();
        Object[] references = {payload};
        Object[] referencesClone = references.clone();
        check(referencesClone != references && referencesClone[0] == payload,
                "reference array clone");
        capability(valuesClone);
        capability(referencesClone);
    }

    private static void capability(Object source) {
        M3Object owner = adapt(source, M3Object.class);
        check(owner != null, "missing M3Object capability");
        same(source, owner.source(), "capability source");
        same(owner, adapt(source, M3Object.class), "retained adapter identity");
        same(owner, M3Object.of(source), "direct and inherited adaptation identity");
    }

    private static void expectNullPointer(Runnable action) {
        try {
            action.run();
        } catch (NullPointerException expected) {
            return;
        }
        throw new AssertionError("expected NullPointerException");
    }

    private static void same(Object expected, Object actual, String message) {
        check(expected == actual, message);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
