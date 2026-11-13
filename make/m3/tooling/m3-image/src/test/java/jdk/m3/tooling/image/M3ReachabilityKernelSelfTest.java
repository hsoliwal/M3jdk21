/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Dependency-free Java/JNI parity and refusal suite for the reachability kernel. */
public final class M3ReachabilityKernelSelfTest {
    private int checks;

    /** Runs Java reference checks and optional JNI parity checks. */
    public static int runAll(Path nativeLibrary) {
        M3ReachabilityKernelSelfTest suite = new M3ReachabilityKernelSelfTest();
        suite.run(nativeLibrary);
        return suite.checks;
    }

    /** Command-line test entry. Pass one native-library path to enable JNI checks. */
    public static void main(String[] args) {
        if (args.length > 1) {
            throw new IllegalArgumentException("Usage: M3ReachabilityKernelSelfTest [native-library]");
        }
        Path nativeLibrary = args.length == 0 ? null : Path.of(args[0]);
        int checks = runAll(nativeLibrary);
        System.out.println("M3_REACHABILITY_KERNEL_SELF_TEST_OK checks=" + checks
                + " native=" + (nativeLibrary != null));
    }

    private void run(Path nativeLibrary) {
        M3ReachabilityGraph small = smallGraph();
        int[] expected = {0, 1, 2, 3, 4, 5};
        M3ReachabilityKernel javaKernel = M3ReachabilityKernels.javaKernel();
        check(java.util.Arrays.equals(expected, javaKernel.close(small)), "small Java closure");
        check("java-csr-bitset-bfs-v1".equals(javaKernel.id()), "Java kernel identity");
        check(small.namesFor(expected).equals(Set.of("A", "B", "C", "D", "E", "F")),
                "index-to-name closure");

        M3ReachabilityGraph large = largeGraph(8192);
        int[] largeJava = javaKernel.close(large);
        check(largeJava.length == 8192, "large Java closure cardinality");
        check(largeJava[0] == 0 && largeJava[largeJava.length - 1] == 8191,
                "large Java closure bounds");

        if (nativeLibrary != null) {
            M3ReachabilityKernel nativeKernel = M3ReachabilityKernels.nativeKernel(nativeLibrary);
            check(java.util.Arrays.equals(expected, nativeKernel.close(small)), "small JNI parity");
            check(java.util.Arrays.equals(largeJava, nativeKernel.close(large)), "large JNI parity");
            check(nativeKernel.id().matches("jni-csr-bitset-bfs-v1:[0-9a-f]{64}"),
                    "JNI kernel identity and library seal");
            expectIllegalArgument(() -> M3JniReachabilityKernel.closeRawForTesting(
                    2, new int[]{0, 1}, new int[]{1}, new int[]{0}));
            expectIllegalArgument(() -> M3JniReachabilityKernel.closeRawForTesting(
                    2, new int[]{0, 1, 1}, new int[]{2}, new int[]{0}));
        }
    }

    private static M3ReachabilityGraph smallGraph() {
        Map<String, M3ClassInfo> classes = new TreeMap<>();
        classes.put("A", info("A", Set.of("B", "C")));
        classes.put("B", info("B", Set.of("D")));
        classes.put("C", info("C", Set.of("D", "E")));
        classes.put("D", info("D", Set.of()));
        classes.put("E", info("E", Set.of("F")));
        classes.put("F", info("F", Set.of()));
        classes.put("G", info("G", Set.of("A")));
        return M3ReachabilityGraph.from(classes, Set.of("A"));
    }

    private static M3ReachabilityGraph largeGraph(int count) {
        Map<String, M3ClassInfo> classes = new TreeMap<>();
        for (int index = 0; index < count; index++) {
            TreeSet<String> references = new TreeSet<>();
            if (index + 1 < count) {
                references.add(name(index + 1));
            }
            if (index + 17 < count) {
                references.add(name(index + 17));
            }
            classes.put(name(index), info(name(index), references));
        }
        return M3ReachabilityGraph.from(classes, Set.of(name(0)));
    }

    private static String name(int index) {
        return "C" + String.format(java.util.Locale.ROOT, "%05d", index);
    }

    private static M3ClassInfo info(String name, Set<String> references) {
        return new M3ClassInfo(name, "input", name + ".class", "0".repeat(64),
                new TreeSet<>(references), new TreeSet<>(), new TreeSet<>());
    }

    private void expectIllegalArgument(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    private void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError("Check failed: " + name);
        }
        checks++;
    }
}
