/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Independently enumerate and verify every supported functional shape
 * @run main FunctionShapeCoverage
 */

import java.io.Serializable;
import java.lang.module.ModuleReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FunctionShapeCoverage {
    private static final String STANDARD = "java.util.function.";
    private static final String EXTENDED = "java.util.function.ext.";
    private static final String CHECKED = "java.util.function.checked.";
    private static final Set<String> EXISTING = Set.of(
            "BiConsumer", "BiFunction", "BinaryOperator", "BiPredicate",
            "BooleanSupplier", "Consumer", "DoubleBinaryOperator", "DoubleConsumer",
            "DoubleFunction", "DoublePredicate", "DoubleSupplier", "DoubleToIntFunction",
            "DoubleToLongFunction", "DoubleUnaryOperator", "Function", "IntBinaryOperator",
            "IntConsumer", "IntFunction", "IntPredicate", "IntSupplier", "IntToDoubleFunction",
            "IntToLongFunction", "IntUnaryOperator", "LongBinaryOperator", "LongConsumer",
            "LongFunction", "LongPredicate", "LongSupplier", "LongToDoubleFunction",
            "LongToIntFunction", "LongUnaryOperator", "ObjDoubleConsumer", "ObjIntConsumer",
            "ObjLongConsumer", "Predicate", "Supplier", "ToDoubleBiFunction",
            "ToDoubleFunction", "ToIntBiFunction", "ToIntFunction", "ToLongBiFunction",
            "ToLongFunction", "UnaryOperator");
    private static final Set<String> EXPECTED = new HashSet<>();
    private static int checks;
    private static boolean component;

    public static void main(String[] args) throws Exception {
        if (args.length > 1 || args.length == 1 && !"--component".equals(args[0])) {
            throw new IllegalArgumentException("Expected no arguments or --component");
        }
        component = args.length == 1;
        for (boolean checked : new boolean[] {false, true}) {
            for (Kind result : Kind.values()) {
                verifyShape(new Kind[0], result, checked);
                for (Kind first : Kind.inputs()) {
                    verifyShape(new Kind[] {first}, result, checked);
                    for (Kind second : Kind.inputs()) {
                        verifyShape(new Kind[] {first, second}, result, checked);
                    }
                }
            }
            for (Kind primitive : Kind.primitives()) {
                verify(primitive.prefix + "Comparator", "compare", int.class,
                        new Class<?>[] {primitive.type, primitive.type}, 0, checked);
            }
            verify("BooleanUnaryOperator", "applyAsBoolean", boolean.class,
                    new Class<?>[] {boolean.class}, 0, checked);
            verify("BooleanBinaryOperator", "applyAsBoolean", boolean.class,
                    new Class<?>[] {boolean.class, boolean.class}, 0, checked);
            verify("UnaryOperator", "apply", Object.class,
                    new Class<?>[] {Object.class}, 1, checked);
            verify("BinaryOperator", "apply", Object.class,
                    new Class<?>[] {Object.class, Object.class}, 1, checked);
            verify("Comparator", "compare", int.class,
                    new Class<?>[] {Object.class, Object.class}, 1, checked);
            for (int arity = 3; arity <= 16; arity++) {
                Class<?>[] parameters = new Class<?>[arity];
                Arrays.fill(parameters, Object.class);
                String suffix = arity == 3 ? "" : Integer.toString(arity);
                String prefix = arity == 3 ? "Tri" : "";
                verify(prefix + "Function" + suffix, "apply", Object.class,
                        parameters, arity + 1, checked);
                verify(prefix + "Consumer" + suffix, "accept", void.class,
                        parameters, arity, checked);
                verify(prefix + "Predicate" + suffix, "test", boolean.class,
                        parameters, arity, checked);
            }
        }
        verifyPackageContents();
        System.out.println("FunctionShapeCoverage " + (component ? "COMPONENT" : "INSTALLED")
                + ": " + EXPECTED.size()
                + " interfaces, " + checks + " checks passed");
    }

    private static void verifyShape(Kind[] arguments, Kind result, boolean checked)
            throws Exception {
        String name;
        String method;
        if (arguments.length == 0) {
            name = result == Kind.VOID ? "Runnable"
                    : (result == Kind.OBJECT ? "" : result.prefix) + "Supplier";
            method = result == Kind.VOID ? "run"
                    : result == Kind.OBJECT ? "get" : "getAs" + result.prefix;
        } else {
            String stem = argumentPrefix(arguments);
            if (result == Kind.VOID) {
                name = stem + "Consumer";
                method = "accept";
            } else if (result == Kind.BOOLEAN) {
                name = stem + "Predicate";
                method = "test";
            } else if (result == Kind.OBJECT) {
                name = stem + "Function";
                method = "apply";
            } else {
                boolean same = Arrays.stream(arguments).allMatch(kind -> kind == result);
                if (same) {
                    name = result.prefix + (arguments.length == 1 ? "Unary" : "Binary")
                            + "Operator";
                } else if (Arrays.stream(arguments).allMatch(kind -> kind == Kind.OBJECT)) {
                    name = "To" + result.prefix + stem + "Function";
                } else {
                    name = stem + "To" + result.prefix + "Function";
                }
                method = "applyAs" + result.prefix;
            }
        }
        int typeParameters = result == Kind.OBJECT ? 1 : 0;
        for (Kind argument : arguments) {
            if (argument == Kind.OBJECT) {
                typeParameters++;
            }
        }
        Class<?>[] parameters = Arrays.stream(arguments).map(kind -> kind.type)
                .toArray(Class<?>[]::new);
        verify(name, method, result.type, parameters, typeParameters, checked);
    }

    private static String argumentPrefix(Kind[] arguments) {
        if (arguments.length == 1) {
            return arguments[0] == Kind.OBJECT ? "" : arguments[0].prefix;
        }
        Kind first = arguments[0];
        Kind second = arguments[1];
        if (first == second) {
            return first == Kind.OBJECT ? "Bi" : first.prefix + "Bi";
        }
        return first.prefix + second.prefix;
    }

    private static void verify(String name, String methodName, Class<?> result,
            Class<?>[] parameters, int typeParameters, boolean checked) throws Exception {
        String fullName = checked ? CHECKED + name : "Runnable".equals(name)
                ? "java.lang.Runnable" : "Comparator".equals(name) ? "java.util.Comparator"
                : (EXISTING.contains(name) ? STANDARD : EXTENDED) + name;
        require(EXPECTED.add(fullName), "Duplicate expected interface: " + fullName);
        Class<?> type = Class.forName(fullName);
        require(type.isInterface(), fullName + " must be an interface");
        require(type.isAnnotationPresent(FunctionalInterface.class),
                fullName + " must declare @FunctionalInterface");
        require(!type.isSealed(), fullName + " must be usable by lambdas");
        require(!Serializable.class.isAssignableFrom(type),
                fullName + " must not impose serialization");
        require(type.getModule() == Object.class.getModule(),
                fullName + " must belong to java.base");
        require(component ? type.getModule().isExported(type.getPackageName(),
                FunctionShapeCoverage.class.getModule())
                : type.getModule().isExported(type.getPackageName()),
                fullName + " must be exported without command-line overrides");
        require(type.getTypeParameters().length == typeParameters + (checked ? 1 : 0),
                fullName + " has the wrong generic arity");
        Method[] abstractMethods = Arrays.stream(type.getMethods())
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .filter(method -> !isObjectMethod(method))
                .toArray(Method[]::new);
        require(abstractMethods.length == 1, fullName + " must have one abstract method");
        Method method = abstractMethods[0];
        require(method.getName().equals(methodName), fullName + " has the wrong SAM name");
        require(method.getReturnType() == result, fullName + " boxes or changes the result");
        require(Arrays.equals(method.getParameterTypes(), parameters),
                fullName + " boxes, reorders or changes its arguments");
        require(method.getTypeParameters().length == 0,
                fullName + " must not use a method-generic SAM");
        require(Arrays.equals(method.getExceptionTypes(),
                checked ? new Class<?>[] {Throwable.class} : new Class<?>[0]),
                fullName + " has the wrong erased throws contract");
        if (checked) {
            var exception = type.getTypeParameters()[type.getTypeParameters().length - 1];
            require(Arrays.equals(exception.getBounds(), new Class<?>[] {Throwable.class}),
                    fullName + " must admit every Throwable subtype");
        }
    }

    private static boolean isObjectMethod(Method method) {
        try {
            Object.class.getMethod(method.getName(), method.getParameterTypes());
            return true;
        } catch (NoSuchMethodException absent) {
            return false;
        }
    }

    private static void verifyPackageContents() throws Exception {
        Set<String> expected = EXPECTED.stream().filter(name -> name.startsWith(EXTENDED)
                || name.startsWith(CHECKED)).collect(Collectors.toSet());
        try (ModuleReader reader = ModuleLayer.boot().configuration().findModule("java.base")
                .orElseThrow().reference().open(); var resources = reader.list()) {
            Set<String> actual = resources.filter(name -> name.startsWith("java/util/function/ext/")
                    || name.startsWith("java/util/function/checked/"))
                    .filter(name -> name.endsWith(".class"))
                    .filter(name -> !name.endsWith("package-info.class") && !name.contains("$"))
                    .map(name -> name.substring(0, name.length() - 6).replace('/', '.'))
                    .collect(Collectors.toSet());
            Set<String> missing = new HashSet<>(expected);
            missing.removeAll(actual);
            Set<String> extra = new HashSet<>(actual);
            extra.removeAll(expected);
            require(missing.isEmpty() && extra.isEmpty(),
                    "Installed package mismatch: missing=" + missing + ", extra=" + extra);
        }
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private enum Kind {
        OBJECT(Object.class, "Obj"), BOOLEAN(boolean.class, "Boolean"),
        BYTE(byte.class, "Byte"), SHORT(short.class, "Short"), CHAR(char.class, "Char"),
        INT(int.class, "Int"), LONG(long.class, "Long"), FLOAT(float.class, "Float"),
        DOUBLE(double.class, "Double"), VOID(void.class, "Void");

        final Class<?> type;
        final String prefix;

        Kind(Class<?> type, String prefix) {
            this.type = type;
            this.prefix = prefix;
        }

        static Kind[] inputs() {
            return Arrays.stream(values()).filter(kind -> kind != VOID).toArray(Kind[]::new);
        }

        static Kind[] primitives() {
            return Arrays.stream(values()).filter(kind -> kind != VOID && kind != OBJECT)
                    .toArray(Kind[]::new);
        }
    }
}
