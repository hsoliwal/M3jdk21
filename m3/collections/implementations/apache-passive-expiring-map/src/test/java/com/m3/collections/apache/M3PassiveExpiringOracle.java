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
package com.m3.collections.apache;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.ResettableIterator;

/**
 * The Apache Commons Collections 4.5.0 {@code PassiveExpiringMap} as a test oracle on the fake clock.
 *
 * <p>The classes come unmodified from {@code commons-collections4-4.5.0.jar}, loaded child-first by a private loader,
 * except for one constant: in {@code PassiveExpiringMap} and {@code PassiveExpiringMap$ConstantTimeToLiveExpirationPolicy}
 * the {@code CONSTANT_Utf8} {@code java/lang/System} is renamed to {@link M3PassiveExpiringTestClock}. The rename is
 * refused unless that Utf8 exists once, is named by exactly one {@code CONSTANT_Class} and by nothing else in the pool,
 * and every member reference through that class is the method {@code currentTimeMillis()J}. Every other byte, and so the
 * provider's logic, stays as released. Test policy classes named at construction are defined inside the loader too, so
 * they implement the oracle's own {@code ExpirationPolicy}.
 */
final class M3PassiveExpiringOracle {
    static final String PROVIDER = "org.apache.commons.collections4.map.PassiveExpiringMap";
    static final String CONSTANT_POLICY = PROVIDER + "$ConstantTimeToLiveExpirationPolicy";
    private static final String JAR = "commons-collections4-4.5.0.jar";
    private static final String SYSTEM = "java/lang/System";
    private static final String CLOCK = M3PassiveExpiringTestClock.class.getName().replace('.', '/');

    private final PatchedLoader loader;
    private final Class<?> mapClass;
    private final Class<?> policyType;
    private final Method mapIterator;
    private final Method[] cursorMethods;

    /** Creates an oracle loader; {@code isolated} names test classes to define inside it. */
    M3PassiveExpiringOracle(String... isolated) {
        loader = new PatchedLoader(Set.of(isolated));
        try {
            mapClass = loader.loadClass(PROVIDER);
            loader.loadClass(CONSTANT_POLICY);
            policyType = loader.loadClass(PROVIDER + "$ExpirationPolicy");
            mapIterator = mapClass.getMethod("mapIterator");
            Class<?> mi = loader.loadClass("org.apache.commons.collections4.MapIterator");
            Class<?> ri = loader.loadClass("org.apache.commons.collections4.ResettableIterator");
            cursorMethods = new Method[] {mi.getMethod("hasNext"), mi.getMethod("next"), mi.getMethod("getKey"),
                mi.getMethod("getValue"), mi.getMethod("setValue", Object.class), mi.getMethod("remove"),
                ri.getMethod("reset")};
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        if (mapClass.getClassLoader() != loader || loader.patched != 2) {
            throw new IllegalStateException("oracle not isolated or not patched: " + loader.patched);
        }
    }

    /** Number of classes whose clock constant was redirected; 2 when both provider classes are loaded. */
    int patchedClasses() {
        return loader.patched;
    }

    Class<?> mapClass() {
        return mapClass;
    }

    /** {@code new PassiveExpiringMap<>()}. */
    <K, V> Map<K, V> create() {
        return construct(new Class<?>[0]);
    }

    /** {@code new PassiveExpiringMap<>(long)}. */
    <K, V> Map<K, V> create(long ttl) {
        return construct(new Class<?>[] {long.class}, ttl);
    }

    /** {@code new PassiveExpiringMap<>(long, TimeUnit)}. */
    <K, V> Map<K, V> create(long ttl, TimeUnit unit) {
        return construct(new Class<?>[] {long.class, TimeUnit.class}, ttl, unit);
    }

    /** {@code new PassiveExpiringMap<>(long, Map)}. */
    <K, V> Map<K, V> create(long ttl, Map<K, V> map) {
        return construct(new Class<?>[] {long.class, Map.class}, ttl, map);
    }

    /** {@code new PassiveExpiringMap<>(long, TimeUnit, Map)}. */
    <K, V> Map<K, V> create(long ttl, TimeUnit unit, Map<K, V> map) {
        return construct(new Class<?>[] {long.class, TimeUnit.class, Map.class}, ttl, unit, map);
    }

    /** {@code new PassiveExpiringMap<>(Map)}. */
    <K, V> Map<K, V> create(Map<K, V> map) {
        return construct(new Class<?>[] {Map.class}, map);
    }

    /** {@code new PassiveExpiringMap<>(ExpirationPolicy)} with a policy instance from {@link #isolated}. */
    <K, V> Map<K, V> createWithPolicy(Object policy) {
        return construct(new Class<?>[] {policyType}, policy);
    }

    /** {@code new PassiveExpiringMap<>(ExpirationPolicy, Map)}. */
    <K, V> Map<K, V> createWithPolicy(Object policy, Map<K, V> map) {
        return construct(new Class<?>[] {policyType, Map.class}, policy, map);
    }

    /** An instance of an isolated class (defined inside the oracle loader), built with its public constructor. */
    Object isolated(String className, Class<?>[] types, Object... args) {
        try {
            return loader.loadClass(className).getConstructor(types).newInstance(args);
        } catch (InvocationTargetException e) {
            throw rethrow(e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The provider's {@code ConstantTimeToLiveExpirationPolicy(long)} on the fake clock. */
    Object constantPolicy(long ttl) {
        try {
            return loader.loadClass(CONSTANT_POLICY).getConstructor(long.class).newInstance(ttl);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@code oracle.mapIterator()} behind a loader-neutral cursor. */
    MapCursor cursor(Map<?, ?> oracle) {
        Object it = call(mapIterator, oracle);
        return new MapCursor() {
            @Override
            public boolean hasNext() {
                return (Boolean) call(cursorMethods[0], it);
            }

            @Override
            public Object next() {
                return call(cursorMethods[1], it);
            }

            @Override
            public Object getKey() {
                return call(cursorMethods[2], it);
            }

            @Override
            public Object getValue() {
                return call(cursorMethods[3], it);
            }

            @Override
            public Object setValue(Object value) {
                return call(cursorMethods[4], it, value);
            }

            @Override
            public void remove() {
                call(cursorMethods[5], it);
            }

            @Override
            public void reset() {
                call(cursorMethods[6], it);
            }
        };
    }

    /** An M3 map iterator behind the same cursor. */
    static <K, V> MapCursor cursor(MapIterator<K, V> it) {
        @SuppressWarnings("unchecked")
        ResettableIterator<K> resettable = (ResettableIterator<K>) it;
        return new MapCursor() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Object next() {
                return it.next();
            }

            @Override
            public Object getKey() {
                return it.getKey();
            }

            @Override
            public Object getValue() {
                return it.getValue();
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object setValue(Object value) {
                return it.setValue((V) value);
            }

            @Override
            public void remove() {
                it.remove();
            }

            @Override
            public void reset() {
                resettable.reset();
            }
        };
    }

    /** Commons {@code MapIterator} operations without loader-specific types. */
    interface MapCursor {
        boolean hasNext();

        Object next();

        Object getKey();

        Object getValue();

        Object setValue(Object value);

        void remove();

        void reset();
    }

    @SuppressWarnings("unchecked")
    private <K, V> Map<K, V> construct(Class<?>[] types, Object... args) {
        try {
            return (Map<K, V>) mapClass.getConstructor(types).newInstance(args);
        } catch (InvocationTargetException e) {
            throw rethrow(e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object call(Method m, Object target, Object... args) {
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw rethrow(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RuntimeException rethrow(InvocationTargetException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException r) {
            return r;
        }
        if (cause instanceof Error err) {
            throw err;
        }
        return new IllegalStateException(cause);
    }

    /**
     * Renames the one {@code CONSTANT_Utf8 java/lang/System} of a class file to the test clock. Refuses unless that
     * entry exists exactly once, exactly one {@code CONSTANT_Class} and nothing else in the pool names it, and every
     * field, method or interface-method reference through that class is the method {@code currentTimeMillis()J}.
     */
    static byte[] redirectClock(byte[] classFile) {
        ByteBuffer in = ByteBuffer.wrap(classFile);
        if (in.getInt() != 0xCAFEBABE) {
            throw new IllegalStateException("not a class file");
        }
        in.getInt();
        int count = in.getShort() & 0xFFFF;
        int[] tag = new int[count];
        int[] start = new int[count];
        int[] end = new int[count];
        int[] utf8A = new int[count];
        int[] utf8B = new int[count];
        int[] memberClass = new int[count];
        int[] memberType = new int[count];
        String[] text = new String[count];
        int target = -1;
        for (int i = 1; i < count; i++) {
            start[i] = in.position();
            tag[i] = in.get() & 0xFF;
            switch (tag[i]) {
                case 1 -> {
                    byte[] bytes = new byte[in.getShort() & 0xFFFF];
                    in.get(bytes);
                    text[i] = new String(bytes, StandardCharsets.UTF_8);
                    if (SYSTEM.equals(text[i])) {
                        if (target >= 0) {
                            throw new IllegalStateException("java/lang/System twice in the pool");
                        }
                        target = i;
                    }
                }
                case 3, 4 -> in.getInt();
                case 5, 6 -> {
                    in.getLong();
                    i++;
                }
                case 7, 8, 16, 19, 20 -> utf8A[i] = in.getShort() & 0xFFFF;
                case 12 -> {
                    utf8A[i] = in.getShort() & 0xFFFF;
                    utf8B[i] = in.getShort() & 0xFFFF;
                }
                case 9, 10, 11 -> {
                    memberClass[i] = in.getShort() & 0xFFFF;
                    memberType[i] = in.getShort() & 0xFFFF;
                }
                case 17, 18 -> in.getInt();
                case 15 -> {
                    in.get();
                    in.getShort();
                }
                default -> throw new IllegalStateException("unknown constant tag " + tag[i] + " at " + i);
            }
            end[i] = in.position();
        }
        if (target < 0) {
            throw new IllegalStateException("no java/lang/System constant");
        }
        int systemClass = -1;
        for (int i = 1; i < count; i++) {
            boolean names = (tag[i] == 7 || tag[i] == 8 || tag[i] == 12 || tag[i] == 16 || tag[i] == 19
                    || tag[i] == 20) && (utf8A[i] == target || utf8B[i] == target);
            if (names && (tag[i] != 7 || systemClass >= 0)) {
                throw new IllegalStateException("java/lang/System also named by constant " + i + " tag " + tag[i]);
            }
            systemClass = names ? i : systemClass;
        }
        if (systemClass < 0) {
            throw new IllegalStateException("java/lang/System named by no class constant");
        }
        int clockCalls = 0;
        for (int i = 1; i < count; i++) {
            if ((tag[i] == 9 || tag[i] == 10 || tag[i] == 11) && memberClass[i] == systemClass) {
                int nat = memberType[i];
                if (tag[i] != 10 || !"currentTimeMillis".equals(text[utf8A[nat]]) || !"()J".equals(text[utf8B[nat]])) {
                    throw new IllegalStateException("java/lang/System member other than currentTimeMillis()J at " + i);
                }
                clockCalls++;
            }
        }
        if (clockCalls != 1) {
            throw new IllegalStateException("expected one currentTimeMillis()J reference, found " + clockCalls);
        }
        byte[] name = CLOCK.getBytes(StandardCharsets.UTF_8);
        ByteBuffer out = ByteBuffer.allocate(classFile.length - (end[target] - start[target]) + 3 + name.length);
        out.put(classFile, 0, start[target]).put((byte) 1).putShort((short) name.length).put(name);
        out.put(classFile, end[target], classFile.length - end[target]);
        return out.array();
    }

    /** Child-first loader for the Commons classes and the named test classes; parent-first for everything else. */
    private static final class PatchedLoader extends ClassLoader {
        private final Set<String> isolated;
        private int patched;

        PatchedLoader(Set<String> isolated) {
            super("m3-commons-4.5.0-oracle", M3PassiveExpiringOracle.class.getClassLoader());
            this.isolated = isolated;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("org.apache.commons.collections4.") && !isolated.contains(name)) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    byte[] bytes = read(name);
                    if (name.equals(PROVIDER) || name.equals(CONSTANT_POLICY)) {
                        bytes = redirectClock(bytes);
                        patched++;
                    }
                    c = defineClass(name, bytes, 0, bytes.length);
                }
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
        }

        private byte[] read(String name) throws ClassNotFoundException {
            String path = name.replace('.', '/') + ".class";
            URL url = getParent().getResource(path);
            if (url == null || name.startsWith("org.") && !url.toString().contains(JAR)) {
                throw new ClassNotFoundException(name + " not found in " + JAR + ": " + url);
            }
            try (InputStream in = url.openStream()) {
                return in.readAllBytes();
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
