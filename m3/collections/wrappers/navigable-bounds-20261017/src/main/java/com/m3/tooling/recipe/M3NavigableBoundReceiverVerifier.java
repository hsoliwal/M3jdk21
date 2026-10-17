// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.recipe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Compile before/after; prove public descriptors unchanged; run Java21 differential oracle. */
public final class M3NavigableBoundReceiverVerifier {
    private M3NavigableBoundReceiverVerifier() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: receiver.java proof.java");
        }
        Path receiver = Path.of(args[0]).toAbsolutePath().normalize();
        Path proof = Path.of(args[1]).toAbsolutePath().normalize();
        M3NavigableBoundRecipe.main(new String[] {"--verify", receiver.toString()});
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) throw new IllegalStateException("JDK21_JAVAC_REQUIRED");
        Path output = Files.createTempDirectory("m3-navigation-proof-");
        try {
            Path beforeSource = output.resolve(
                    "before-src/com/m3/collections/M3FilteredNavigableSet.java");
            Files.createDirectories(beforeSource.getParent());
            String generated = Files.readString(receiver, StandardCharsets.UTF_8);
            if (!generated.contains(M3NavigableBoundRecipe.AFTER)) {
                throw new IllegalStateException("M3_NAV_BOUND_POSTIMAGE_NOT_INSTALLED");
            }
            Files.writeString(beforeSource, generated.replace(
                    M3NavigableBoundRecipe.AFTER, M3NavigableBoundRecipe.BEFORE),
                    StandardCharsets.UTF_8);
            Path before = Files.createDirectories(output.resolve("before"));
            Path after = Files.createDirectories(output.resolve("after"));
            compile(javac, before, beforeSource);
            compile(javac, after, receiver, proof);
            try (URLClassLoader oldLoader = new URLClassLoader(
                         new URL[] {before.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
                 URLClassLoader newLoader = new URLClassLoader(
                         new URL[] {after.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
                String className = "com.m3.collections.M3FilteredNavigableSet";
                Class<?> oldClass = Class.forName(className, false, oldLoader);
                Class<?> newClass = Class.forName(className, false, newLoader);
                Set<String> oldApi = exportedDescriptors(oldClass);
                Set<String> newApi = exportedDescriptors(newClass);
                if (!oldApi.equals(newApi)) {
                    throw new AssertionError(
                            "M3_NAV_BOUND_PUBLIC_API_DRIFT:before=" + oldApi
                                    + ":after=" + newApi);
                }
                System.out.println("M3_NAV_BOUND_PUBLIC_API_EQUIVALENCE_PASS descriptors="
                        + oldApi.size());
                Class<?> corpus = Class.forName(
                        "com.m3.collections.M3NavigableBoundaryProof", true, newLoader);
                try {
                    corpus.getMethod("main", String[].class)
                            .invoke(null, (Object) new String[0]);
                } catch (InvocationTargetException failure) {
                    if (failure.getCause() instanceof Exception ex) throw ex;
                    if (failure.getCause() instanceof Error ex) throw ex;
                    throw failure;
                }
            }
        } finally {
            try (var paths = Files.walk(output)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void compile(JavaCompiler javac, Path output, Path... sources) {
        List<String> command = new ArrayList<>(List.of(
                "--release", "21", "-Xlint:all", "-Werror", "-d", output.toString()));
        for (Path source : sources) command.add(source.toString());
        int status = javac.run(null, System.out, System.err, command.toArray(String[]::new));
        if (status != 0) throw new AssertionError("M3_NAV_BOUND_JAVAC_FAILED:" + status);
    }

    private static Set<String> exportedDescriptors(Class<?> owner) {
        TreeSet<String> descriptors = new TreeSet<>();
        descriptors.add("super=" + owner.getSuperclass().getName());
        for (Class<?> type : owner.getInterfaces()) {
            descriptors.add("interface=" + type.getName());
        }
        for (Constructor<?> constructor : owner.getDeclaredConstructors()) {
            int modifiers = constructor.getModifiers();
            if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
                descriptors.add(constructor.toGenericString());
            }
        }
        for (Field field : owner.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
                descriptors.add(field.toGenericString());
            }
        }
        for (Method method : owner.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
                descriptors.add(method.toGenericString());
            }
        }
        return descriptors;
    }
}
