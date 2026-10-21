// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.google.BiMapTestSuiteBuilder;
import com.google.common.collect.testing.google.TestStringBiMapGenerator;
import com.m3.collections.M3ArrayList;
import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.interop.M3BiMap;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map.Entry;
import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestResult;

/** Delegates contract case generation and execution to unchanged Guava/JUnit test libraries. */
public final class M3BiMapUpstreamSuiteLauncher {
    private static final String CONTRACT_FEATURES =
            "CollectionSize.ANY;CollectionFeature.SUPPORTS_ITERATOR_REMOVE;CollectionFeature.KNOWN_ORDER;MapFeature.ALLOWS_NULL_KEYS;MapFeature.ALLOWS_NULL_VALUES;MapFeature.ALLOWS_ANY_NULL_QUERIES;MapFeature.GENERAL_PURPOSE";

    private M3BiMapUpstreamSuiteLauncher() { }

    public static Test suite() {
        return BiMapTestSuiteBuilder.using(new TestStringBiMapGenerator() {
            @Override protected BiMap<String, String> create(Entry<String, String>[] entries) {
                M3BiMap<String, String> owner = M3BiMap.create();
                for (Entry<String, String> entry : entries) {
                    owner.put(entry.getKey(), entry.getValue());
                }
                return owner;
            }

            /** Existing M3BiMap forward iteration follows first key insertion order. */
            @Override public Iterable<Entry<String, String>> order(
                    List<Entry<String, String>> insertionOrder) {
                return insertionOrder;
            }
        }).named("Existing M3BiMap against Guava 33.4.8 interface contract")
                .withFeatures(
                        CollectionSize.ANY,
                        CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                        CollectionFeature.KNOWN_ORDER,
                        MapFeature.ALLOWS_NULL_KEYS,
                        MapFeature.ALLOWS_NULL_VALUES,
                        MapFeature.ALLOWS_ANY_NULL_QUERIES,
                        MapFeature.GENERAL_PURPOSE)
                .createTestSuite();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3 && args.length != 4) {
            throw new IllegalArgumentException(
                    "owner classes directory, original testlib JAR, qualified Guava JAR"
                            + " and optional frozen expected count required");
        }
        requireCodeSource(M3BiMap.class, Path.of(args[0]));
        requireCodeSource(BiMapTestSuiteBuilder.class, Path.of(args[1]));
        requireCodeSource(TestStringBiMapGenerator.class, Path.of(args[1]));
        for (Class<?> type : List.of(BiMap.class, HashBiMap.class,
                M3LinkedHashMap.class, M3ArrayList.class)) {
            requireCodeSource(type, Path.of(args[2]));
        }
        System.out.println("UPSTREAM_FEATURE_DESCRIPTOR_SHA256=" + sha(CONTRACT_FEATURES));
        Test generated = suite();
        int discovered = generated.countTestCases();
        if (discovered <= 22) {
            throw new AssertionError("generated upstream coverage did not exceed existing 22 tests: "
                    + discovered);
        }
        if (args.length == 4 && discovered != Integer.parseInt(args[3])) {
            throw new AssertionError("frozen generated case count mismatch: " + discovered);
        }
        System.out.println("UPSTREAM_BIMAP_DISCOVERED=" + discovered);
        TestResult result = new TestResult();
        generated.run(result);
        printFailures("FAILURE", result.failures());
        printFailures("ERROR", result.errors());
        System.out.printf("UPSTREAM_BIMAP_TESTS found=%d run=%d failed=%d errors=%d%n",
                discovered, result.runCount(), result.failureCount(), result.errorCount());
        if (result.runCount() != discovered || result.failureCount() != 0
                || result.errorCount() != 0 || result.shouldStop()) {
            throw new AssertionError("generated Guava BiMap suite failed exact-count contract");
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

    /** Bound diagnostic detail while retaining the complete failure/error counts. */
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
