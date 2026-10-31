/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary M3 implementation structures stay internal to canonical M3JDK21 APIs
 * @run main/othervm M3CanonicalApiBoundaryTest
 */

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Target-image guard, intentionally independent of Synexia's OpenRewrite recipe.
 *
 * <p>Checks exported JDK signatures and selected concrete implementation owners.
 * Does not establish performance, behavior, or completeness of all M3 modules.
 */
public final class M3CanonicalApiBoundaryTest {
    private static final String STRING_OWNER = "java.lang.M3String";
    private static final String CLASS_OWNER = "jdk.internal.mindex.M3Class";
    private static final String CLASS_OWNER_PACKAGE = "jdk.internal.mindex";
    private static final String[] INTERNAL_MODULE_PACKAGES = {
        "jdk.internal.m3", "jdk.internal.mindex", "com.sun.tools.javac.m3"
    };
    private static final String[] INTERNAL_PREFIXES = {
        "M3", "MIndex", "MatIndex"
    };

    private M3CanonicalApiBoundaryTest() {}

    public static void main(String[] args) throws Exception {
        Class<?> stringOwner = Class.forName(STRING_OWNER, false, null);
        Class<?> classOwner = Class.forName(CLASS_OWNER, false, null);
        Module javaBase = String.class.getModule();
        if (stringOwner.getModule() != javaBase || classOwner.getModule() != javaBase) {
            throw new AssertionError("internal implementation escaped java.base");
        }
        if (Modifier.isPublic(stringOwner.getModifiers())) {
            throw new AssertionError("java.lang.M3String must be package-private");
        }
        ModuleDescriptor module = javaBase.getDescriptor();
        if (module == null || module.isOpen()) {
            throw new AssertionError("missing or open java.base module descriptor");
        }
        if (module.exports().stream().anyMatch(e -> e.source().equals(CLASS_OWNER_PACKAGE))
                || module.opens().stream().anyMatch(o -> o.source().equals(CLASS_OWNER_PACKAGE))) {
            throw new AssertionError("internal class-index package is exported or opened");
        }
        Module consumer = M3CanonicalApiBoundaryTest.class.getModule();
        if (javaBase.isExported(CLASS_OWNER_PACKAGE, consumer)
                || javaBase.isOpen(CLASS_OWNER_PACKAGE, consumer)) {
            throw new AssertionError("runtime options exposed the internal class-index package");
        }
        // A renamed helper remains internal because package identity is permanent.
        verifyModulePackages("java.base");
        verifyModulePackages("jdk.compiler");
        rejectInternalSurface("com.m3.collections.SlotArray", "self-test");
        rejectInternalSurface("jdk.internal.mindex.IndexRows", "self-test");
        rejectInternalSurface("com.sun.tools.javac.m3.ParsedRows", "self-test");
        if (isInternalSurface("java.util.List<java.lang.String>")) {
            throw new AssertionError("ordinary JDK type incorrectly rejected");
        }
        for (Class<?> canonical : new Class<?>[] {
                String.class, Class.class, InputStream.class, OutputStream.class,
                ArrayList.class, HashMap.class, TreeMap.class, HashSet.class,
                TreeSet.class, LinkedList.class, ArrayDeque.class,
                PriorityQueue.class, Arrays.class, Collections.class,
                ConcurrentHashMap.class, Pattern.class, Matcher.class,
                Path.class, Files.class
        }) {
            verifyCanonical(canonical);
        }
        auditExportedJavaPackages();
        System.out.println("M3JDK_CANONICAL_API_BOUNDARY_PASS 19 canonical owners, 2 required internal owners");
    }

    /** Fail closed for M3 internal implementation modules, including qualified exports. */
    private static void verifyModulePackages(String moduleName) {
        ModuleDescriptor descriptor = ModuleFinder.ofSystem().find(moduleName)
                .orElseThrow(() -> new AssertionError("module not installed: " + moduleName))
                .descriptor();
        if (descriptor.isOpen()) {
            throw new AssertionError("unexpected open JDK module: " + moduleName);
        }
        for (ModuleDescriptor.Exports directive : descriptor.exports()) {
            if (isInternalPackage(directive.source())) {
                throw new AssertionError("internal M3 package exported: " + directive.source());
            }
        }
        for (ModuleDescriptor.Opens directive : descriptor.opens()) {
            if (isInternalPackage(directive.source())) {
                throw new AssertionError("internal M3 package opened: " + directive.source());
            }
        }
    }

    private static boolean isInternalPackage(String name) {
        for (String prefix : INTERNAL_MODULE_PACKAGES) {
            if (name.equals(prefix) || name.startsWith(prefix + ".")) return true;
        }
        return false;
    }

    /** Validate top-level M3 classes in every exported or opened java.base package. */
    private static void auditExportedJavaPackages() throws Exception {
        ModuleDescriptor descriptor = String.class.getModule().getDescriptor();
        Set<String> visiblePackages = new HashSet<>();
        for (ModuleDescriptor.Exports exp : descriptor.exports()) {
            visiblePackages.add(exp.source());
        }
        for (ModuleDescriptor.Opens op : descriptor.opens()) {
            visiblePackages.add(op.source());
        }
        try (ModuleReader reader = ModuleFinder.ofSystem().find("java.base")
                .orElseThrow().open();
             var entries = reader.list()) {
            for (String resource : entries.filter(entry -> entry.endsWith(".class")
                    && !entry.contains("$")).sorted().toList()) {
                int lastSlash = resource.lastIndexOf('/');
                if (lastSlash < 0) {
                    continue;
                }
                String simple = resource.substring(lastSlash + 1,
                        resource.length() - ".class".length());
                if (!internalSpelling(simple)) {
                    continue;
                }
                String packageName = resource.substring(0, lastSlash).replace('/', '.');
                if (!visiblePackages.contains(packageName)) {
                    continue;
                }
                String binaryName = packageName + "." + simple;
                Class<?> internal = Class.forName(binaryName, false, null);
                if (Modifier.isPublic(internal.getModifiers())) {
                    throw new AssertionError("exported JDK package exposes M3 class: " + binaryName);
                }
            }
        }
    }

    private static void verifyCanonical(Class<?> canonical) {
        if (!Modifier.isPublic(canonical.getModifiers())
                || !canonical.getModule().isExported(canonical.getPackageName())) {
            throw new AssertionError("canonical JDK type not publicly accessible: " + canonical);
        }
        if (internalSpelling(canonical.getName())) {
            throw new AssertionError("M3 implementation published as a canonical JDK type: " + canonical);
        }
        for (Type iface : canonical.getGenericInterfaces()) {
            assertPublicType(iface, canonical.getName() + " interface");
        }
        assertPublicType(canonical.getGenericSuperclass(), canonical.getName() + " superclass");
        for (TypeVariable<?> variable : canonical.getTypeParameters()) {
            for (Type bound : variable.getBounds()) {
                assertPublicType(bound, canonical.getName() + " type variable");
            }
        }
        for (Constructor<?> ctor : canonical.getDeclaredConstructors()) {
            if (isExternallyVisible(ctor.getModifiers())) {
                for (Type type : ctor.getGenericParameterTypes()) {
                    assertPublicType(type, ctor + " parameter");
                }
                for (Type type : ctor.getGenericExceptionTypes()) {
                    assertPublicType(type, ctor + " checked exception");
                }
            }
        }
        for (Method method : canonical.getDeclaredMethods()) {
            if (isExternallyVisible(method.getModifiers())) {
                assertPublicType(method.getGenericReturnType(), method + " return");
                for (Type type : method.getGenericParameterTypes()) {
                    assertPublicType(type, method + " parameter");
                }
                for (Type type : method.getGenericExceptionTypes()) {
                    assertPublicType(type, method + " checked exception");
                }
                for (TypeVariable<Method> variable : method.getTypeParameters()) {
                    for (Type bound : variable.getBounds()) {
                        assertPublicType(bound, method + " type variable");
                    }
                }
            }
        }
        for (Field field : canonical.getDeclaredFields()) {
            if (isExternallyVisible(field.getModifiers())) {
                assertPublicType(field.getGenericType(), field + " field");
            }
        }
        for (Class<?> inner : canonical.getDeclaredClasses()) {
            if (isExternallyVisible(inner.getModifiers()) && internalSpelling(inner.getSimpleName())) {
                throw new AssertionError("public nested M3 structure in " + canonical.getName());
            }
        }
    }

    private static boolean isExternallyVisible(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    private static boolean internalSpelling(String text) {
        String[] tokens = text.split("[^A-Za-z0-9_$]+");
        for (String token : tokens) {
            for (String prefix : INTERNAL_PREFIXES) {
                if (token.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isInternalSurface(String typeName) {
        if (internalSpelling(typeName)) return true;
        for (String prefix : INTERNAL_MODULE_PACKAGES) {
            if (typeName.contains(prefix + ".")) return true;
        }
        return typeName.contains("com.m3.");
    }

    private static void rejectInternalSurface(String typeName, String location) {
        if (!isInternalSurface(typeName)) {
            throw new AssertionError(location + " failed to identify M3 implementation " + typeName);
        }
    }

    private static void assertPublicType(Type type, String location) {
        if (type != null && isInternalSurface(type.getTypeName())) {
            throw new AssertionError(location + " exposes internal M3 type " + type);
        }
    }
}
