/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
/*
 * @test
 * @summary Explicit lookup authority and array/mask contracts of Reflections
 * @run main ReflectionsContract
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Reflections;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ReflectionsContract {
    private static long checks;
    private static final Exception FAILURE = new Exception("target identity");

    public static final class Target {
        public int publicValue = 3;
        private int privateValue;
        public static int staticValue = 11;
        public final int finalValue = 37;

        private Target(int value) { privateValue = value; }
        private int plus(int value) { return privateValue + value; }
        private int fail() throws Exception { throw FAILURE; }
        private static int sum(int... values) {
            int sum = 0;
            for (int value : values) sum += value;
            return sum;
        }
    }

    private static final class RecordedMember implements Member {
        final int modifiers;
        final int label;
        final List<Integer> trace;
        int calls;
        RuntimeException failure;
        RecordedMember(int modifiers, int label, List<Integer> trace) {
            this.modifiers = modifiers;
            this.label = label;
            this.trace = trace;
        }
        public Class<?> getDeclaringClass() { return Target.class; }
        public String getName() { return "member" + label; }
        public int getModifiers() {
            calls++;
            if (trace != null) trace.add(label);
            if (failure != null) throw failure;
            return modifiers;
        }
        public boolean isSynthetic() { return false; }
    }

    @FunctionalInterface
    private interface Action { void run() throws Throwable; }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static Throwable expect(Class<? extends Throwable> type, Action action)
            throws Throwable {
        try {
            action.run();
        } catch (Throwable failure) {
            check(type.isInstance(failure), "expected " + type + ", got " + failure);
            return failure;
        }
        throw new AssertionError("missing " + type);
    }

    private static void masks() throws Throwable {
        Random random = new Random(0x4d334a444b3231L);
        for (int index = 0; index < 8192; index++) {
            int modifiers = random.nextInt();
            int required = random.nextInt();
            int forbidden = random.nextInt();
            if ((index & 3) == 0) required &= modifiers;
            if ((index & 7) == 0) forbidden &= ~modifiers;
            RecordedMember member = new RecordedMember(modifiers, index, null);
            boolean expected = (modifiers & required) == required
                    && (modifiers & forbidden) == 0;
            check(Reflections.hasModifiers(member, required, forbidden) == expected,
                    "literal mask match");
            check(member.calls == 1, "single modifier observation");
        }
        expect(NullPointerException.class, () -> Reflections.hasModifiers(null, 0, 0));

        List<Integer> trace = new ArrayList<>();
        RecordedMember first = new RecordedMember(Modifier.PUBLIC, 1, trace);
        RecordedMember second = new RecordedMember(Modifier.PRIVATE, 2, trace);
        RecordedMember third = new RecordedMember(Modifier.PUBLIC | Modifier.STATIC, 3, trace);
        RecordedMember[] source = {null, first, second, third, first, null};
        List<RecordedMember> selected = Reflections.filterMembers(
                source, 1, 5, Modifier.PUBLIC, Modifier.STATIC);
        check(selected.size() == 2 && selected.get(0) == first && selected.get(1) == first,
                "range, stable order, duplicate identity");
        check(trace.equals(List.of(1, 2, 3, 1)), "one observation per visited slot");
        check(first.calls == 2 && second.calls == 1 && third.calls == 1,
                "slot observations, not unique-member observations");
        source[1] = second;
        check(selected.get(0) == first, "detached selection");
        expect(UnsupportedOperationException.class, () -> selected.add(second));
        expect(UnsupportedOperationException.class, () -> selected.set(0, second));
        expect(UnsupportedOperationException.class, selected::clear);
        check(Reflections.filterMembers(source, 3, 3, 0, 0).isEmpty(), "empty range");
        trace.clear();
        check(Reflections.filterMembers(source, 1, 5, Modifier.PUBLIC, Modifier.PUBLIC)
                .isEmpty(), "overlapping masks");
        check(trace.equals(List.of(2, 2, 3, 1)), "overlap still observes each slot");
        expect(NullPointerException.class, () -> Reflections.filterMembers(source, 0, 0));
        expect(NullPointerException.class,
                () -> Reflections.filterMembers((Member[]) null, 0, 0));
        expect(IndexOutOfBoundsException.class,
                () -> Reflections.filterMembers(source, -1, 1, 0, 0));
        expect(IndexOutOfBoundsException.class,
                () -> Reflections.filterMembers(source, 3, 2, 0, 0));
        expect(IndexOutOfBoundsException.class,
                () -> Reflections.filterMembers(source, 0, 7, 0, 0));
        check(Reflections.filterMembers(new Member[0], 0, 0).isEmpty(), "whole empty array");
        RuntimeException sentinel = new IllegalStateException("member callback identity");
        second.failure = sentinel;
        trace.clear();
        check(expect(IllegalStateException.class,
                () -> Reflections.filterMembers(source, 1, 5, 0, 0)) == sentinel,
                "member callback exception identity");
        check(trace.equals(List.of(2)), "failure stops at its original slot");
    }

    @SuppressWarnings("deprecation") // Inspect, but never infer, the actual override bit.
    private static void handles() throws Throwable {
        MethodHandles.Lookup caller = MethodHandles.lookup();
        MethodHandles.Lookup publicOnly = MethodHandles.publicLookup();
        Constructor<Target> constructor = Target.class.getDeclaredConstructor(int.class);
        Method plus = Target.class.getDeclaredMethod("plus", int.class);
        Method fail = Target.class.getDeclaredMethod("fail");
        Method sum = Target.class.getDeclaredMethod("sum", int[].class);
        Field privateField = Target.class.getDeclaredField("privateValue");
        Field publicField = Target.class.getDeclaredField("publicValue");
        Field staticField = Target.class.getDeclaredField("staticValue");
        Field finalField = Target.class.getDeclaredField("finalValue");

        Target target = (Target) Reflections.unreflectConstructor(caller, constructor)
                .invokeExact(17);
        check((int) Reflections.unreflect(caller, plus).invokeExact(target, 5) == 22,
                "private nest method");
        check((int) Reflections.unreflectGetter(caller, privateField).invokeExact(target) == 17,
                "private getter");
        Reflections.unreflectSetter(caller, privateField).invokeExact(target, 23);
        check(target.privateValue == 23, "private setter");
        check((int) Reflections.unreflectGetter(publicOnly, publicField)
                .invokeExact(target) == 3, "public lookup public getter");
        Reflections.unreflectSetter(publicOnly, staticField).invokeExact(29);
        check((int) Reflections.unreflectGetter(publicOnly, staticField).invokeExact() == 29,
                "static getter and setter shape");
        VarHandle handle = Reflections.unreflectVarHandle(caller, privateField);
        check(handle.coordinateTypes().equals(List.of(Target.class)), "instance coordinates");
        check(handle.compareAndSet(target, 23, 31), "variable handle compare-and-set");
        check((int) handle.get(target) == 31, "variable handle read");
        VarHandle staticHandle = Reflections.unreflectVarHandle(publicOnly, staticField);
        check(staticHandle.coordinateTypes().isEmpty(), "static coordinates");
        check((int) staticHandle.get() == 29, "static variable handle");
        VarHandle readOnly = Reflections.unreflectVarHandle(caller, finalField);
        check(!readOnly.isAccessModeSupported(VarHandle.AccessMode.SET), "final remains read-only");
        expect(IllegalAccessException.class,
                () -> Reflections.unreflectSetter(caller, finalField));
        MethodHandle variable = Reflections.unreflect(caller, sum);
        check(variable.isVarargsCollector(), "variable arity retained");
        check((int) variable.invokeWithArguments(1, 2, 3) == 6, "variable arity invocation");
        check(expect(Exception.class,
                () -> Reflections.unreflect(caller, fail).invoke(target)) == FAILURE,
                "target exception remains unwrapped");

        expect(IllegalAccessException.class, () -> Reflections.unreflect(publicOnly, plus));
        expect(IllegalAccessException.class,
                () -> Reflections.unreflectConstructor(publicOnly, constructor));
        expect(IllegalAccessException.class,
                () -> Reflections.unreflectGetter(publicOnly, privateField));
        expect(IllegalAccessException.class,
                () -> Reflections.unreflectSetter(publicOnly, privateField));
        expect(IllegalAccessException.class,
                () -> Reflections.unreflectVarHandle(publicOnly, privateField));
        check(!plus.isAccessible() && !constructor.isAccessible()
                && !privateField.isAccessible() && !publicField.isAccessible(),
                "no access override created");

        privateField.setAccessible(true);
        try {
            check((int) Reflections.unreflectGetter(publicOnly, privateField)
                    .invokeExact(target) == 31, "existing explicit override follows Lookup");
            expect(IllegalAccessException.class,
                    () -> Reflections.unreflectVarHandle(publicOnly, privateField));
            check(privateField.isAccessible(), "existing override retained");
        } finally {
            privateField.setAccessible(false);
        }
        Method forName = Class.class.getMethod("forName", String.class);
        expect(IllegalAccessException.class, () -> Reflections.unreflect(publicOnly, forName));
        Class<?> loaded = (Class<?>) Reflections.unreflect(caller, forName)
                .invokeExact(ReflectionsContract.class.getName());
        check(loaded == ReflectionsContract.class, "caller-sensitive lookup class retained");

        expect(NullPointerException.class, () -> Reflections.unreflect(null, plus));
        expect(NullPointerException.class, () -> Reflections.unreflect(caller, null));
        expect(NullPointerException.class,
                () -> Reflections.unreflectConstructor(null, constructor));
        expect(NullPointerException.class,
                () -> Reflections.unreflectConstructor(caller, null));
        expect(NullPointerException.class, () -> Reflections.unreflectGetter(null, privateField));
        expect(NullPointerException.class, () -> Reflections.unreflectGetter(caller, null));
        expect(NullPointerException.class, () -> Reflections.unreflectSetter(null, privateField));
        expect(NullPointerException.class, () -> Reflections.unreflectSetter(caller, null));
        expect(NullPointerException.class,
                () -> Reflections.unreflectVarHandle(null, privateField));
        expect(NullPointerException.class, () -> Reflections.unreflectVarHandle(caller, null));
    }

    public static void main(String[] args) throws Throwable {
        masks();
        handles();
        System.out.println("REFLECTIONS_CONTRACT_PASS checks=" + checks);
    }
}
