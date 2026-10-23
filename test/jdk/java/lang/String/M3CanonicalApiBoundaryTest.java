/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary M3 implementation structures stay internal to canonical M3JDK21 APIs
 * @run main/othervm M3CanonicalApiBoundaryTest
 */

import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
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
    private static final String[] INTERNAL_PREFIXES = {
        "M3", "MIndex", "MatIndex"
    };

    private M3CanonicalApiBoundaryTest() {}

    public static void main(String[] args) throws Exception {
        Class<?> stringOwner = Class.forName(STRING_OWNER);
        Class<?> classOwner = Class.forName(CLASS_OWNER);
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
        for (Class<?> canonical : new Class<?>[] {
                String.class, Class.class, ArrayList.class, HashMap.class,
                TreeMap.class, Pattern.class, Matcher.class
        }) {
            verifyCanonical(canonical);
        }
        System.out.println("M3JDK_CANONICAL_API_BOUNDARY_PASS 7 canonical owners, 2 internal owners");
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

    private static void assertPublicType(Type type, String location) {
        if (type != null && internalSpelling(type.getTypeName())) {
            throw new AssertionError(location + " exposes internal M3 type " + type);
        }
    }
}
