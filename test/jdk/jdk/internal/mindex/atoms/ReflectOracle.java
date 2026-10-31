/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import jdk.internal.mindex.M3CI;
import jdk.internal.mindex.M3ClassFlags;
import jdk.internal.mindex.M3MemberFlags;
import jdk.internal.mindex.M3MemberKind;

/**
 * java.lang.Class / java.lang.reflect oracle of the M3CI snapshot family (REFLECT ORACLE v1): fixture
 * types covering every class kind, member kind and flag, a snapshot of their closure, and the reflective
 * value of every lane for a snapshot row.
 */
final class ReflectOracle {
    private ReflectOracle() {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Tag {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Mark {
    }

    sealed interface Shape permits Circle, Square {
        double area();

        default String label() {
            return "shape";
        }
    }

    record Circle(@Tag("radius") double radius, String name) implements Shape {
        @Override
        public double area() {
            return radius * radius;
        }
    }

    static final class Square implements Shape {
        @Override
        public double area() {
            return 1;
        }
    }

    enum Color { RED, GREEN { }, BLUE }

    @Tag("base")
    @Mark
    static class Base<T> implements Comparable<Base<T>> {
        protected int count;
        @Tag("value")
        public T value;
        public static final String CONSTANT = "c";

        public Base() {
        }

        @Mark
        protected Base(int count, String... names) throws java.io.IOException, InterruptedException {
            this.count = count + names.length;
        }

        @Override
        public int compareTo(Base<T> other) {
            return Integer.compare(count, other.count);
        }

        @Mark
        public static int sum(int... values) {
            return Arrays.stream(values).sum();
        }

        public void work() throws Exception {
            Runnable lambda = () -> count++;
            lambda.run();
        }
    }

    static class Derived extends Base<String> implements Runnable, java.io.Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public void run() {
        }

        class Inner {
        }
    }

    static Object anonymous() {
        return new Object() { };
    }

    static Class<?> local() {
        class Local {
        }
        return Local.class;
    }

    /** Roots of the snapshot: the fixture holder (its nest brings every fixture) plus array types. */
    static final List<Class<?>> ROOTS = List.of(ReflectOracle.class, Derived[].class, int[][].class);

    static M3CI snapshot() {
        anonymous();
        local();
        return M3CI.compile(ROOTS, M3CI.Options.standard(), null);
    }

    /** Class of every row through the weak boundary of a standard snapshot. */
    static Class<?>[] classes(M3CI ci) {
        Class<?>[] out = new Class<?>[ci.size()];
        for (int row = 0; row < out.length; row++) {
            out[row] = ci.boundary().classAt(row).orElseThrow();
        }
        return out;
    }

    static Map<Class<?>, Integer> rows(M3CI ci) {
        Map<Class<?>, Integer> rows = new IdentityHashMap<>();
        Class<?>[] classes = classes(ci);
        for (int row = 0; row < classes.length; row++) {
            rows.put(classes[row], row);
        }
        return rows;
    }

    static int rowOf(Map<Class<?>, Integer> rows, Class<?> type) {
        return type == null ? -1 : rows.getOrDefault(type, -1);
    }

    static int classFlags(Class<?> c) {
        return (c.isInterface() ? M3ClassFlags.INTERFACE : 0) | (c.isEnum() ? M3ClassFlags.ENUM : 0)
                | (c.isAnnotation() ? M3ClassFlags.ANNOTATION : 0) | (c.isRecord() ? M3ClassFlags.RECORD : 0)
                | (c.isArray() ? M3ClassFlags.ARRAY : 0) | (c.isPrimitive() ? M3ClassFlags.PRIMITIVE : 0)
                | (c.isSealed() ? M3ClassFlags.SEALED : 0) | (c.isHidden() ? M3ClassFlags.HIDDEN : 0)
                | (c.isSynthetic() ? M3ClassFlags.SYNTHETIC : 0) | (c.isAnonymousClass() ? M3ClassFlags.ANONYMOUS : 0)
                | (c.isLocalClass() ? M3ClassFlags.LOCAL : 0) | (c.isMemberClass() ? M3ClassFlags.MEMBER : 0);
    }

    static Set<String> annotationNames(AnnotatedElement element) {
        return Arrays.stream(element.getDeclaredAnnotations()).map(a -> a.annotationType().getName())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** Transitive supertypes including the type itself; interfaces also reach Object (as M3CI documents). */
    static TreeSet<Integer> supertypes(Map<Class<?>, Integer> rows, Class<?> type) {
        TreeSet<Integer> out = new TreeSet<>();
        ArrayDeque<Class<?>> queue = new ArrayDeque<>(List.of(type));
        if (type.isInterface() && rows.containsKey(Object.class)) {
            queue.add(Object.class);
        }
        while (!queue.isEmpty()) {
            Class<?> c = queue.removeFirst();
            Integer row = rows.get(c);
            if (row == null || !out.add(row)) {
                continue;
            }
            if (c.getSuperclass() != null) {
                queue.add(c.getSuperclass());
            }
            queue.addAll(List.of(c.getInterfaces()));
        }
        return out;
    }

    /** One declared member of a class as (kind, name, descriptor) with its reflective element. */
    record Declared(M3MemberKind kind, String name, String descriptor, AnnotatedElement element) {
    }

    static List<Declared> declared(Class<?> c) {
        List<Declared> out = new ArrayList<>();
        for (Field f : c.getDeclaredFields()) {
            out.add(new Declared(M3MemberKind.FIELD, f.getName(), f.getType().descriptorString(), f));
        }
        for (Method m : c.getDeclaredMethods()) {
            out.add(new Declared(M3MemberKind.METHOD, m.getName(), descriptor(m.getParameterTypes(), m.getReturnType()), m));
        }
        for (Constructor<?> k : c.getDeclaredConstructors()) {
            out.add(new Declared(M3MemberKind.CONSTRUCTOR, k.getName(), descriptor(k.getParameterTypes(), void.class), k));
        }
        if (c.getRecordComponents() != null) {
            for (RecordComponent rc : c.getRecordComponents()) {
                out.add(new Declared(M3MemberKind.RECORD_COMPONENT, rc.getName(), rc.getType().descriptorString(), rc));
            }
        }
        return out;
    }

    /** JVM method descriptor spelled from Class.descriptorString of every parameter and the return type. */
    static String descriptor(Class<?>[] parameters, Class<?> returnType) {
        return Arrays.stream(parameters).map(Class::descriptorString).collect(Collectors.joining("", "(", ")"))
                + returnType.descriptorString();
    }

    static int modifiers(AnnotatedElement element) {
        return element instanceof Member member ? member.getModifiers() : 0;
    }

    static int memberFlags(AnnotatedElement element) {
        if (element instanceof Field f) {
            return M3MemberFlags.of(f);
        }
        if (element instanceof Method m) {
            return M3MemberFlags.of(m);
        }
        if (element instanceof Constructor<?> k) {
            return M3MemberFlags.of(k);
        }
        return M3MemberFlags.of((RecordComponent) element);
    }

    static List<String> exceptionDescriptors(AnnotatedElement element) {
        return element instanceof Executable e
                ? Arrays.stream(e.getExceptionTypes()).map(Class::descriptorString).toList() : List.of();
    }

    /** Row of the member (kind, name, descriptor) of class row {@code classRow}, or -1. */
    static int memberRow(M3CI ci, int classRow, Declared d) {
        return ci.memberRow(classRow, d.kind(), d.name(), d.descriptor());
    }

    static List<String> annotationList(M3CI ci, int memberRow) {
        List<String> out = new ArrayList<>();
        for (int k = 0; k < ci.memberAnnotationCount(memberRow); k++) {
            out.add(ci.memberAnnotationTypeName(memberRow, k));
        }
        return out;
    }

    static Annotation[] none() {
        return new Annotation[0];
    }

    /** First member row of the kind in the snapshot. */
    static int firstMember(M3CI ci, M3MemberKind kind) {
        for (int m = 0; m < ci.memberSize(); m++) {
            if (ci.memberKind(m) == kind) {
                return m;
            }
        }
        throw new AssertionError("snapshot has no " + kind);
    }

    @FunctionalInterface
    interface MemberCheck {
        void accept(int member, Declared declared) throws Throwable;
    }

    /** Runs {@code check} on every member row of the kind (null: every kind); returns the number checked. */
    static int forEachMember(M3CI ci, M3MemberKind kind, MemberCheck check) throws Throwable {
        Class<?>[] classes = classes(ci);
        int seen = 0;
        for (int row = 0; row < classes.length; row++) {
            for (Declared d : declared(classes[row])) {
                if (kind != null && d.kind() != kind) {
                    continue;
                }
                int member = memberRow(ci, row, d);
                if (member < 0) {
                    throw new AssertionError("member not indexed: " + d);
                }
                check.accept(member, d);
                seen++;
            }
        }
        return seen;
    }
}
