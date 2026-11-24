// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Test-only, fail-closed discoverability gate for JUnit tests patched into the
 * platform-loaded jdk.m3.collections module. Does not execute assertions.
 */
public final class M3ProviderJUnitDiscoveryProbe {
    private M3ProviderJUnitDiscoveryProbe() { }

    public static void main(String[] names) throws Exception {
        if (names.length == 0) {
            throw new AssertionError("No selected provider test classes");
        }
        Class<?> marker = Class.forName("org.junit.jupiter.api.Test", false,
                ClassLoader.getPlatformClassLoader());
        if (marker.getClassLoader() != null) {
            throw new AssertionError(
                    "JUnit @Test is invisible to the JDK platform module class loader");
        }
        int methods = 0;
        for (String name : names) {
            Class<?> type = Class.forName(name, false,
                    Thread.currentThread().getContextClassLoader());
            if (!type.getModule().isNamed()) {
                throw new AssertionError(
                        "Provider test escaped its original named module: " + name);
            }
            int found = 0;
            for (Method method : type.getDeclaredMethods()) {
                for (Annotation annotation : method.getDeclaredAnnotations()) {
                    if (annotation.annotationType() == marker) {
                        found++;
                    }
                }
            }
            if (found == 0) {
                throw new AssertionError(
                        "No visible original JUnit @Test annotation in " + name);
            }
            methods += found;
        }
        System.out.printf(
                "M3_PROVIDER_JUNIT_DISCOVERY_PASS classes=%d methods=%d%n",
                names.length, methods);
    }
}
