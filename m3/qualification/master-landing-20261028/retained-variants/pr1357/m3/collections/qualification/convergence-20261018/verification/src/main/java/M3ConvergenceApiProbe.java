// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

/** Compare the effective callable surface, including inherited protected members. */
public final class M3ConvergenceApiProbe {
    private M3ConvergenceApiProbe() { }

    private static Set<String> surface(Class<?> type) {
        Set<String> result = new TreeSet<>();
        for (var method : type.getMethods()) {
            result.add("public " + method.getName() + MethodType.methodType(
                    method.getReturnType(), method.getParameterTypes()).descriptorString());
        }
        for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
            for (var method : owner.getDeclaredMethods()) {
                if (Modifier.isProtected(method.getModifiers())) {
                    result.add("protected " + method.getName() + MethodType.methodType(
                            method.getReturnType(), method.getParameterTypes()).descriptorString());
                }
            }
        }
        for (var constructor : type.getDeclaredConstructors()) {
            if (Modifier.isPublic(constructor.getModifiers())
                    || Modifier.isProtected(constructor.getModifiers())) {
                result.add("constructor " + MethodType.methodType(void.class,
                        constructor.getParameterTypes()).descriptorString());
            }
        }
        for (var field : type.getFields()) {
            result.add("field " + field.getName() + ":" + field.getType().descriptorString());
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        Path before = Path.of(args[0]).toRealPath();
        Path after = Path.of(args[1]).toRealPath();
        try (var oldLoader = new URLClassLoader(new java.net.URL[] {before.toUri().toURL()},
                     ClassLoader.getPlatformClassLoader());
             var newLoader = new URLClassLoader(new java.net.URL[] {after.toUri().toURL()},
                     ClassLoader.getPlatformClassLoader())) {
            for (int i = 2; i < args.length; i++) {
                Class<?> oldType = Class.forName(args[i], false, oldLoader);
                Class<?> newType = Class.forName(args[i], false, newLoader);
                if (!oldType.getProtectionDomain().getCodeSource().getLocation().toURI().equals(before.toUri())
                        || !newType.getProtectionDomain().getCodeSource().getLocation().toURI().equals(after.toUri())) {
                    throw new AssertionError("unexpected loaded class origin: " + args[i]);
                }
                if (Modifier.isPublic(oldType.getModifiers())) {
                    Set<String> expected = surface(oldType);
                    Set<String> actual = surface(newType);
                    if (!expected.equals(actual)) {
                        Set<String> removed = new TreeSet<>(expected);
                        removed.removeAll(actual);
                        Set<String> added = new TreeSet<>(actual);
                        added.removeAll(expected);
                        throw new AssertionError(args[i] + " removed=" + removed + " added=" + added);
                    }
                    System.out.println(args[i] + " EXACT_EFFECTIVE_API members=" + actual.size());
                } else {
                    System.out.println(args[i] + " NON_PUBLIC_OWNER");
                }
            }
        }
    }
}
