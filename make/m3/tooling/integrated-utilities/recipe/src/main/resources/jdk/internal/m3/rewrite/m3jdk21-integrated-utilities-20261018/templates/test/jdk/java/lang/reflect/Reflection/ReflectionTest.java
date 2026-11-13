/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

/*
 * @test
 * @summary Reflection metadata utilities preserve type identity, bounds, annotations, and access state
 * @run main ReflectionTest
 */

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

public class ReflectionTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        hierarchies();
        genericTypes();
        annotationsAndExecutables();
        hostileDescriptors();
        accessAndInitialization();
        System.out.println("ReflectionTest checks=" + checks);
    }

    private static void hierarchies() {
        check(Reflection.hierarchy(Child.class).toList().equals(List.of(Child.class, Parent.class, Object.class)), "class chain");
        check(Reflection.hierarchy(int.class).toList().equals(List.of(int.class)), "primitive chain");
        check(Reflection.hierarchy(void.class).toList().equals(List.of(void.class)), "void chain");
        check(Reflection.hierarchy(Right.class).toList().equals(List.of(Right.class)), "interface chain");
        check(Reflection.hierarchy(String[].class).toList().equals(List.of(String[].class, Object.class)), "array chain");
        check(Reflection.interfaces(Child.class).toList().equals(List.of(Right.class, Runnable.class, Left.class, Root.class)), "interface discovery and diamond deduplication");
        check(Reflection.interfaces(Right.class).toList().equals(List.of(Root.class)), "interface input excludes itself");
        check(Reflection.interfaces(int.class).findAny().isEmpty(), "primitive interfaces");
        check(Reflection.interfaces(Object.class).findAny().isEmpty(), "Object interfaces");
        check(Reflection.interfaces(String[].class).toList().equals(List.of(Cloneable.class, Serializable.class)), "array interfaces");
        check(!Reflection.hierarchy(Child.class).isParallel() && !Reflection.interfaces(Child.class).isParallel(), "sequential metadata streams");
        expect(NullPointerException.class, () -> Reflection.hierarchy(null));
        expect(NullPointerException.class, () -> Reflection.interfaces(null));
    }

    private static void genericTypes() throws Exception {
        for (Class<?> type : new Class<?>[] {Object.class, String.class, int.class, void.class, int[][].class}) {
            check(Reflection.rawType(type).orElseThrow() == type, "raw Class identity");
        }
        Type named = GenericShape.class.getDeclaredField("named").getGenericType();
        Type namedArrays = GenericShape.class.getDeclaredField("namedArrays").getGenericType();
        Type variableArrays = GenericShape.class.getDeclaredField("variableArrays").getGenericType();
        TypeVariable<?> variable = GenericShape.class.getTypeParameters()[0];
        check(Reflection.rawType(named).orElseThrow() == List.class, "parameterized raw class");
        check(Reflection.rawType(namedArrays).orElseThrow() == List[].class, "parameterized generic array");
        check(Reflection.rawType(variable).isEmpty() && Reflection.rawType(variableArrays).isEmpty(), "unresolved variable is not guessed");
        check(Reflection.componentType(namedArrays).orElseThrow().equals(named), "generic component retained");
        check(Reflection.componentType(variableArrays).orElseThrow().equals(variable), "unresolved component retained");
        check(Reflection.componentType(int[][].class).orElseThrow() == int[].class, "immediate Class component");
        check(Reflection.componentType(String.class).isEmpty(), "non-array component absent");
        check(Reflection.typeArguments(named).toList().equals(List.of(String.class)), "actual argument");
        Type map = GenericShape.class.getDeclaredField("map").getGenericType();
        check(Reflection.typeArguments(map).toList().equals(List.of(String.class, Integer.class)), "actual argument order");
        check(Reflection.typeArguments(List.class).findAny().isEmpty(), "raw Class arguments not invented");
        check(Reflection.upperBounds(variable).toList().equals(Arrays.asList(variable.getBounds())), "ordered variable bounds");
        Type upper = ((ParameterizedType) GenericShape.class.getDeclaredField("upper").getGenericType()).getActualTypeArguments()[0];
        Type lower = ((ParameterizedType) GenericShape.class.getDeclaredField("lower").getGenericType()).getActualTypeArguments()[0];
        check(Reflection.upperBounds(upper).toList().equals(List.of(Number.class)), "wildcard upper bound");
        check(Reflection.lowerBounds(upper).findAny().isEmpty(), "no lower bound");
        check(Reflection.lowerBounds(lower).toList().equals(List.of(Integer.class)), "wildcard lower bound");
        check(Reflection.upperBounds(lower).toList().equals(List.of(Object.class)), "implicit Object bound preserved");
        check(Reflection.rawType(upper).isEmpty() && Reflection.rawType(lower).isEmpty(), "wildcard is not an exact class");
        check(Reflection.upperBounds(String.class).findAny().isEmpty(), "ordinary class is not a variable");
        check(Reflection.lowerBounds(variable).findAny().isEmpty(), "type variable has no wildcard lower bounds");
        expect(NullPointerException.class, () -> Reflection.rawType(null));
        expect(NullPointerException.class, () -> Reflection.componentType(null));
        expect(NullPointerException.class, () -> Reflection.typeArguments(null));
        expect(NullPointerException.class, () -> Reflection.upperBounds(null));
        expect(NullPointerException.class, () -> Reflection.lowerBounds(null));
    }

    private static void annotationsAndExecutables() throws Exception {
        check(Reflection.annotations(Inherits.class, Flag.class).map(Flag::value).toList().equals(List.of("base")), "inherited annotation");
        check(Reflection.annotations(Child.class, Flag.class).map(Flag::value).toList().equals(List.of("one", "two")), "repeatable container order");
        check(new HashSet<>(Reflection.annotations(Child.class).toList())
                .equals(new HashSet<>(Arrays.asList(Child.class.getAnnotations()))), "annotation presence parity");
        Method method = HiddenFixture.class.getDeclaredMethod("hidden", List.class, Number[].class);
        check(Reflection.parameters(method).toList().equals(Arrays.asList(method.getParameters())), "method parameter metadata");
        check(Reflection.parameterTypes(method).toList().equals(Arrays.asList(method.getGenericParameterTypes())), "generic method parameters");
        check(Reflection.exceptionTypes(method).toList().equals(Arrays.asList(method.getGenericExceptionTypes())), "method exception metadata");
        Constructor<?> constructor = HiddenFixture.class.getDeclaredConstructor();
        check(Reflection.parameters(constructor).findAny().isEmpty(), "constructor parameters");
        check(Reflection.parameterTypes(constructor).findAny().isEmpty(), "constructor generic parameters");
        check(Reflection.exceptionTypes(constructor).findAny().isEmpty(), "constructor exception metadata");
        expect(NullPointerException.class, () -> Reflection.annotations((AnnotatedElement) null));
        expect(NullPointerException.class, () -> Reflection.annotations(Child.class, null));
        expect(NullPointerException.class, () -> Reflection.parameters(null));
        expect(NullPointerException.class, () -> Reflection.parameterTypes(null));
        expect(NullPointerException.class, () -> Reflection.exceptionTypes(null));
    }

    private static void hostileDescriptors() {
        GenericArrayType cyclic = new GenericArrayType() {
            @Override public Type getGenericComponentType() { return this; }
        };
        check(Reflection.rawType(cyclic).isEmpty(), "recursive descriptor terminates");
        GenericArrayType voidArray = () -> void.class;
        expect(UnsupportedOperationException.class, () -> Reflection.rawType(voidArray));
        GenericArrayType nullComponent = () -> null;
        expect(NullPointerException.class, () -> Reflection.rawType(nullComponent));
        expect(NullPointerException.class, () -> Reflection.componentType(nullComponent));
        Type deep = String.class;
        for (int i = 0; i < 256; i++) {
            Type next = deep;
            deep = (GenericArrayType) () -> next;
        }
        Type tooDeep = deep;
        expect(UnsupportedOperationException.class, () -> Reflection.rawType(tooDeep));
        Type opaque = new Type() {};
        check(Reflection.rawType(opaque).isEmpty(), "opaque Type is not guessed");
        RuntimeException failure = new RuntimeException("metadata failure");
        ParameterizedType broken = new ParameterizedType() {
            @Override public Type[] getActualTypeArguments() { throw failure; }
            @Override public Type getRawType() { throw failure; }
            @Override public Type getOwnerType() { return null; }
        };
        check(expect(RuntimeException.class, () -> Reflection.rawType(broken)) == failure, "raw accessor exception identity");
        check(expect(RuntimeException.class, () -> Reflection.typeArguments(broken)) == failure, "argument accessor exception identity");
        ParameterizedType nonClass = new ParameterizedType() {
            @Override public Type[] getActualTypeArguments() { return new Type[0]; }
            @Override public Type getRawType() { return opaque; }
            @Override public Type getOwnerType() { return null; }
        };
        check(Reflection.rawType(nonClass).isEmpty(), "non-Class raw metadata refused");
    }

    private static void accessAndInitialization() throws Exception {
        HiddenFixture target = new HiddenFixture();
        Method method = HiddenFixture.class.getDeclaredMethod("hidden", List.class, Number[].class);
        check(!method.canAccess(target), "private member initially inaccessible");
        Reflection.parameters(method).toList();
        Reflection.annotations(method).toList();
        Reflection.parameterTypes(method).toList();
        Reflection.exceptionTypes(method).toList();
        check(!method.canAccess(target), "private member access state retained");
        expect(IllegalAccessException.class, () -> method.invoke(target, List.of(), new Number[0]));
        Field value = String.class.getDeclaredField("value");
        check(!value.canAccess("x"), "closed java.base member initially inaccessible");
        Reflection.annotations(value).toList();
        check(!value.canAccess("x"), "closed java.base member remains inaccessible");
        expect(IllegalAccessException.class, () -> value.get("x"));
        check(!InitTracker.initialized, "fixture starts uninitialized");
        Class<?> type = InitProbe.class;
        Reflection.hierarchy(type).toList();
        Reflection.interfaces(type).toList();
        Reflection.annotations(type).toList();
        Reflection.rawType(type);
        check(!InitTracker.initialized, "metadata does not initialize class");
    }

    interface Root {}
    interface Left extends Root {}
    interface Right extends Root {}
    @Flag("base") static class Parent implements Left {}
    @Flag("one") @Flag("two") static class Child extends Parent implements Right, Runnable {
        @Override public void run() {}
    }
    static class Inherits extends Parent {}
    @Inherited @Retention(RetentionPolicy.RUNTIME) @Repeatable(Flags.class)
    @interface Flag { String value(); }
    @Inherited @Retention(RetentionPolicy.RUNTIME)
    @interface Flags { Flag[] value(); }
    static class GenericShape<T extends Number & Comparable<T>> {
        List<String> named;
        List<String>[] namedArrays;
        T[] variableArrays;
        List<? extends Number> upper;
        List<? super Integer> lower;
        Map<String, Integer> map;
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static <T extends Throwable> T expect(Class<T> type, Action action) {
        checks++;
        try { action.run(); }
        catch (Throwable actual) {
            if (type.isInstance(actual)) return type.cast(actual);
            throw new AssertionError("expected " + type + ", got " + actual, actual);
        }
        throw new AssertionError("expected " + type);
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
}

class HiddenFixture {
    private <U extends Number & Comparable<U>> List<? super Integer> hidden(
            List<? extends Number> items, U[] values) throws IOException, IllegalArgumentException {
        return List.of();
    }
}
class InitTracker { static boolean initialized; }
class InitProbe implements Runnable {
    static { InitTracker.initialized = true; }
    @Override public void run() {}
}
