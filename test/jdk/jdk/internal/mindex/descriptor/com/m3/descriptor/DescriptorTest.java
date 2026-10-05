/*
 * Copyright (c) 2026, Contributors. All rights reserved.
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
 */

/*
 * @test
 * @summary M3 descriptors agree with the VM for nominal and hidden types
 * @modules java.base/jdk.internal.mindex
 * @compile DescriptorTest.java
 * @run main/othervm com.m3.descriptor.DescriptorTest
 * @run main/othervm -Xint com.m3.descriptor.DescriptorTest
 * @run main/othervm -XX:-CompactStrings com.m3.descriptor.DescriptorTest
 */
package com.m3.descriptor;

import java.io.IOException;
import java.lang.constant.ClassDesc;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.List;
import jdk.internal.mindex.M3Descriptor;

/** Executes the actual internal owner against the independent JDK descriptor APIs. */
public final class DescriptorTest {
    private static int assertions;
    private static int initializations;

    private DescriptorTest() { }

    public static void main(String[] args) throws Throwable {
        boolean nominalOnly = args.length == 1 && "nominal".equals(args[0]);
        nominal();
        nulls();
        noInitialization();
        loaderNamespaces();
        if (!nominalOnly) {
            hidden();
            Runnable lambda = () -> { };
            require(lambda.getClass().isHidden(), "lambda fixture must be hidden");
            arrays(lambda.getClass());
            members(lambda.getClass());
        }
        System.out.println("DESCRIPTOR mode=" + (nominalOnly ? "nominal" : "all")
                + " assertions=" + assertions + " PASS");
    }

    private static void nominal() {
        List<Class<?>> roots = List.of(void.class, boolean.class, byte.class, char.class,
                short.class, int.class, long.class, float.class, double.class, Object.class,
                String.class, List.class, Runnable.class, Sample.class, Pair.class,
                Day.class, Tag.class);
        for (Class<?> root : roots) {
            arrays(root);
            members(root);
        }
        class Local { }
        arrays(Local.class);
        arrays(new Object() { }.getClass());
        equal("Ljava/lang/String;", M3Descriptor.type(String.class), "nominal spelling");
        equal("[[I", M3Descriptor.type(int[][].class), "primitive array spelling");
    }

    private static void arrays(Class<?> element) {
        Class<?> current = element;
        equal(current.descriptorString(), M3Descriptor.type(current), "type " + current.getName());
        if (element == void.class) {
            return;
        }
        for (int rank = 1; rank <= 255; rank++) {
            current = current.arrayType();
            equal(current.descriptorString(), M3Descriptor.type(current), "array rank " + rank);
        }
        Class<?> full = current;
        fails(UnsupportedOperationException.class, full::arrayType, null);
    }

    private static void members(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            equal(field.getType().descriptorString(), M3Descriptor.field(field), "field " + field.getName());
        }
        for (Method method : type.getDeclaredMethods()) {
            String expected = MethodType.methodType(method.getReturnType(),
                    method.getParameterTypes()).toMethodDescriptorString();
            equal(expected, M3Descriptor.method(method), "method " + method.getName());
        }
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            String expected = MethodType.methodType(void.class,
                    constructor.getParameterTypes()).toMethodDescriptorString();
            equal(expected, M3Descriptor.constructor(constructor), "constructor");
        }
        RecordComponent[] components = type.getRecordComponents();
        if (components != null) {
            for (RecordComponent component : components) {
                equal(component.getType().descriptorString(), M3Descriptor.recordComponent(component),
                        "record " + component.getName());
            }
        }
    }

    private static void hidden() throws IllegalAccessException, IOException {
        byte[] bytes = bytes("Hidden");
        Class<?> previous = null;
        for (int i = 0; i < 8; i++) {
            Class<?> type = MethodHandles.lookup().defineHiddenClass(bytes, false).lookupClass();
            require(type.isHidden(), "hidden class flag");
            require(type != previous, "each definition has its own identity");
            require(type.describeConstable().isEmpty(), "hidden class is not nominal");
            String descriptor = M3Descriptor.type(type);
            equal(type.descriptorString(), descriptor, "hidden descriptor");
            require(!descriptor.equals("L" + type.getName().replace('.', '/') + ";"),
                    "getName substitution is not the hidden descriptor contract");
            fails(IllegalArgumentException.class, () -> ClassDesc.ofDescriptor(descriptor), null);
            arrays(type);
            members(type);
            previous = type;
        }
    }

    private static void noInitialization() {
        require(initializations == 0, "dormant fixture initial state");
        arrays(Dormant.class);
        members(Dormant.class);
        require(initializations == 0, "metadata queries must not initialize classes");
    }

    private static void loaderNamespaces() throws IOException {
        byte[] bytes = bytes("Hidden");
        Class<?> first = new Loader().define(bytes);
        Class<?> second = new Loader().define(bytes);
        require(first != second, "same binary name in separate loaders is not class identity");
        require(!first.isAssignableFrom(second), "separate loader types are not assignable");
        equal(M3Descriptor.type(first), M3Descriptor.type(second),
                "nominal descriptor is not a loader namespace");
        arrays(first);
        arrays(second);
    }

    private static byte[] bytes(String nested) throws IOException {
        String path = "DescriptorTest$" + nested + ".class";
        try (var input = DescriptorTest.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("missing fixture " + path);
            return input.readAllBytes();
        }
    }

    private static void nulls() {
        fails(NullPointerException.class, () -> M3Descriptor.type(null), "type");
        fails(NullPointerException.class, () -> M3Descriptor.field(null), "field");
        fails(NullPointerException.class, () -> M3Descriptor.method(null), "method");
        fails(NullPointerException.class, () -> M3Descriptor.constructor(null), "constructor");
        fails(NullPointerException.class, () -> M3Descriptor.recordComponent(null), "component");
    }

    private static void fails(Class<? extends Throwable> expected, Runnable action, String message) {
        try {
            action.run();
        } catch (Throwable thrown) {
            require(expected.isInstance(thrown), "wrong exception: " + thrown);
            if (message != null) equal(message, thrown.getMessage(), "exception message");
            return;
        }
        throw new AssertionError("expected " + expected.getName());
    }

    private static void equal(String expected, String actual, String label) {
        require(expected.equals(actual), label + " expected=" + expected + " actual=" + actual);
    }

    private static void require(boolean condition, String label) {
        assertions++;
        if (!condition) throw new AssertionError(label);
    }

    static final class Hidden {
        int number;
        static Class<?> self() { return Hidden.class; }
        void mixed(boolean z, byte b, char c, short s, int i, long j, float f, double d,
                Object value, int[][] arrays) { }
    }

    static final class Sample {
        Sample() { }
        Sample(long wide, Object... values) { }
        char[] chars;
        Object[][] objects;
        static int primitive(long left, double right) { return 0; }
        void nothing() { }
    }

    record Pair(int count, String[] names) { }
    enum Day { MONDAY }
    @interface Tag { String value(); }

    static final class Dormant {
        static { initializations++; }
        int value;
    }

    private static final class Loader extends ClassLoader {
        Class<?> define(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }
}
