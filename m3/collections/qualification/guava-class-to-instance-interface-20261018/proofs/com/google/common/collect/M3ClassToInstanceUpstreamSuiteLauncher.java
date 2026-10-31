/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.google.common.collect;

import com.google.common.collect.ImmutableClassToInstanceMapTest.Impl;
import com.google.common.collect.ImmutableClassToInstanceMapTest.TestClassToInstanceMapGenerator;
import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.TestMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.primitives.Primitives;
import com.m3.collections.M3ArrayList;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.interop.M3GuavaClassToInstanceMaps;
import com.m3.collections.interop.M3GuavaClassToInstanceMapsSelfTest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestResult;

/** Uses unchanged donor sample/generator policy and original MapTestSuiteBuilder cases. */
public final class M3ClassToInstanceUpstreamSuiteLauncher {
    private static final String CONTRACT_FEATURES =
            "MapFeature.GENERAL_PURPOSE;MapFeature.RESTRICTS_KEYS;MapFeature.ALLOWS_NULL_VALUES;CollectionSize.ANY;CollectionFeature.SUPPORTS_ITERATOR_REMOVE;MapFeature.ALLOWS_ANY_NULL_QUERIES";
    private M3ClassToInstanceUpstreamSuiteLauncher() { }

    public static Test suite() {
        return MapTestSuiteBuilder.using(new TestClassToInstanceMapGenerator() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Map<Class, Impl> create(Object... elements) {
                ClassToInstanceMap<Impl> owner = M3GuavaClassToInstanceMaps.create();
                for (Object element : elements) {
                    Entry<Class, Impl> entry = (Entry<Class, Impl>) element;
                    owner.putInstance(entry.getKey(), entry.getValue());
                }
                return (Map) owner;
            }
        }).named("Existing M3 checked ClassToInstanceMap against Guava 33.4.8")
                .withFeatures(
                        MapFeature.GENERAL_PURPOSE,
                        MapFeature.RESTRICTS_KEYS,
                        MapFeature.ALLOWS_NULL_VALUES,
                        CollectionSize.ANY,
                        CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                        MapFeature.ALLOWS_ANY_NULL_QUERIES)
                .createTestSuite();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4 && args.length != 5) {
            throw new IllegalArgumentException("owner classes, original testlib JAR,"
                    + " qualified Guava JAR, original helper classes, optional frozen count required");
        }
        requireCodeSource(M3GuavaClassToInstanceMaps.class, Path.of(args[0]));
        requireCodeSource(M3GuavaClassToInstanceMapsSelfTest.class, Path.of(args[0]));
        requireCodeSource(MapTestSuiteBuilder.class, Path.of(args[1]));
        requireCodeSource(TestMapGenerator.class, Path.of(args[1]));
        requireCodeSource(ImmutableClassToInstanceMapTest.class, Path.of(args[3]));
        requireCodeSource(TestClassToInstanceMapGenerator.class, Path.of(args[3]));
        requireCodeSource(Impl.class, Path.of(args[3]));
        for (Class<?> type : List.of(ClassToInstanceMap.class, MutableClassToInstanceMap.class,
                ImmutableClassToInstanceMap.class, Primitives.class,
                M3LinkedHashMap.class, M3ArrayList.class)) {
            requireCodeSource(type, Path.of(args[2]));
        }
        System.out.println("UPSTREAM_FEATURE_DESCRIPTOR_SHA256=" + sha(CONTRACT_FEATURES));
        Test generated = suite();
        int discovered = generated.countTestCases();
        if (discovered <= 1) throw new AssertionError("generated coverage did not exceed existing test");
        if (args.length == 5 && discovered != Integer.parseInt(args[4])) {
            throw new AssertionError("frozen generated case count mismatch: " + discovered);
        }
        System.out.println("UPSTREAM_CLASS_INSTANCE_DISCOVERED=" + discovered);
        TestResult result = new TestResult();
        generated.run(result);
        printFailures("FAILURE", result.failures());
        printFailures("ERROR", result.errors());
        System.out.printf("UPSTREAM_CLASS_INSTANCE_TESTS found=%d run=%d failed=%d errors=%d%n",
                discovered, result.runCount(), result.failureCount(), result.errorCount());
        if (result.runCount() != discovered || result.failureCount() != 0
                || result.errorCount() != 0 || result.shouldStop()) {
            throw new AssertionError("generated Guava class-to-instance suite failed");
        }
    }

    private static void requireCodeSource(Class<?> type, Path expected) throws Exception {
        var code = type.getProtectionDomain().getCodeSource();
        if (code == null) throw new AssertionError("missing CodeSource: " + type.getName());
        Path actual = Path.of(code.getLocation().toURI()).toRealPath();
        if (!actual.equals(expected.toRealPath())) {
            throw new AssertionError(type.getName() + " CodeSource " + actual
                    + " does not match " + expected);
        }
        System.out.println("CODE_SOURCE " + type.getName() + "=" + actual);
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static void printFailures(String kind, Enumeration<TestFailure> failures) {
        int shown = 0;
        while (failures.hasMoreElements()) {
            TestFailure failure = failures.nextElement();
            if (shown++ < 8) {
                String trace = failure.trace();
                System.out.println(kind + " " + failure.failedTest());
                System.out.println(trace.substring(0, Math.min(trace.length(), 3000)));
            }
        }
    }
}
