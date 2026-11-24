// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Fail-closed proof that original JPMS-patched provider tests have runtime-visible
 * Jupiter annotations before the JUnit ConsoleLauncher attempts discovery.
 */
public final class ProviderJUnitDiscoveryProbe {
    private static final Set<String> JUPITER_METHODS = Set.of(
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.RepeatedTest",
            "org.junit.jupiter.params.ParameterizedTest",
            "org.junit.jupiter.api.TestFactory",
            "org.junit.jupiter.api.TestTemplate");

    private ProviderJUnitDiscoveryProbe() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException(
                    "usage: ProviderJUnitDiscoveryProbe MODULE EXPECTED_COUNT CLASS...");
        }
        String expectedModule = args[0];
        int expectedCount = Integer.parseInt(args[1]);
        if (expectedCount <= 0 || args.length - 2 != expectedCount) {
            throw new IllegalArgumentException("Unexpected provider JUnit class count");
        }
        ClassLoader loader = ClassLoader.getSystemClassLoader();
        Class<?> testAnnotation = Class.forName("org.junit.jupiter.api.Test", false, loader);
        System.out.println("Jupiter @Test loader=" + testAnnotation.getClassLoader());
        int annotatedMethods = 0;
        Set<String> seen = new HashSet<>();
        for (int i = 2; i < args.length; i++) {
            String name = args[i];
            if (!seen.add(name)) {
                throw new IllegalStateException("Duplicate provider test selector: " + name);
            }
            Class<?> klass = Class.forName(name, false, loader);
            Module module = klass.getModule();
            if (!module.isNamed() || !expectedModule.equals(module.getName())) {
                throw new IllegalStateException("Provider test loaded from wrong module: "
                        + name + " module=" + module);
            }
            int found = 0;
            for (Method method : klass.getDeclaredMethods()) {
                for (Annotation annotation : method.getDeclaredAnnotations()) {
                    if (JUPITER_METHODS.contains(annotation.annotationType().getName())) {
                        found++;
                    }
                }
            }
            if (found == 0) {
                throw new IllegalStateException("No runtime-visible Jupiter methods: "
                        + name + " loader=" + klass.getClassLoader());
            }
            annotatedMethods += found;
            System.out.println("Provider Jupiter class=" + name + " methods=" + found
                    + " loader=" + klass.getClassLoader());
        }
        System.out.printf("M3_PROVIDER_JUNIT_ANNOTATIONS_VISIBLE module=%s classes=%d methods=%d%n",
                expectedModule, expectedCount, annotatedMethods);
    }
}
