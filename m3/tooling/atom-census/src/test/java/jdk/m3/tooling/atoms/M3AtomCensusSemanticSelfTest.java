/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Dependency-free Java 21 proof of legacy parity, one parse and fail-closed semantic atoms. */
public final class M3AtomCensusSemanticSelfTest {
    private static int checks;
    private static final String FIXTURE = """
            package fixture;
            class Sample {
                static int counter = 5;
                int field = 2;
                int declaration;
                static { counter = 8; }
                { field++; }
                Sample() { this(1); }
                Sample(int value) { field = value; }
                static int hash(Object key) { return key == null ? 0 : key.hashCode(); }
                int instanceMethod(int x) { return x + field; }
                native long nativeCall(byte[] data);
                Runnable fn = () -> { counter++; };
                // non-BMP UTF-16 span probe 😀
                interface Inner { void empty(); default int answer() { return 42; } }
                class Child { String s = "😀"; }
            }
            """;

    private M3AtomCensusSemanticSelfTest() { }
    private static void check(boolean assertion, String label) {
        if (!assertion) throw new AssertionError(label);
        checks++;
    }

    private static boolean refusesIO(RunnableWithException job, String contains) throws Exception {
        try {
            job.run();
        } catch (IOException err) {
            return err.getMessage() != null && err.getMessage().contains(contains);
        }
        return false;
    }

    /** Replays all deterministic source inventory conditions on the actual parser. */
    public static int runAll() throws Exception {
        Path temp = Files.createTempDirectory("m3-semantic-census-proof-");
        try {
            Path src = Files.createDirectory(temp.resolve("src"));
            Path sample = src.resolve("Sample.java");
            Files.writeString(sample, FIXTURE, StandardCharsets.UTF_8);
            Path rules = temp.resolve("PATTERN_RULES.tsv");
            Files.writeString(rules, "pattern\tname\tbody\tcatalogue\n"
                    + "HASH_SPREAD\thash\t-\tLeetCode 706; HackerRank data structures; GfG hashing\n"
                    + "ARRAY_GROW\t-\tnewCapacity\tLeetCode arrays\n", StandardCharsets.UTF_8);
            Path legacy = temp.resolve("legacy");
            Path semantic = temp.resolve("semantic");
            M3AtomCensus.main(new String[]{"fixture", rules.toString(), legacy.toString(), src.toString()});
            M3AtomCensus.main(new String[]{"--semantic", "fixture", rules.toString(), semantic.toString(), src.toString()});
            check(Files.mismatch(legacy.resolve("ATOM_CENSUS_fixture.tsv"),
                    semantic.resolve("ATOM_CENSUS_fixture.tsv")) == -1, "legacy atom bytes unchanged");
            check(Files.mismatch(legacy.resolve("PATTERN_SUMMARY_fixture.tsv"),
                    semantic.resolve("PATTERN_SUMMARY_fixture.tsv")) == -1, "legacy summary bytes unchanged");
            List<String> rows = Files.readAllLines(semantic.resolve("SEMANTIC_CENSUS_fixture.tsv"),
                    StandardCharsets.UTF_8);
            check(rows.size() > 14, "multiple independent semantic leaves");
            check(rows.get(0).equals(M3AtomCensus.SEMANTIC_HEADER), "exact semantic schema");
            Set<String> kinds = new HashSet<>();
            Set<String> ids = new HashSet<>();
            List<Long> positions = new ArrayList<>();
            int staticMethods = 0;
            for (String line : rows.subList(1, rows.size())) {
                String[] c = line.split("\t", -1);
                check(c.length == 12, "twelve exact columns");
                check(c[0].matches("A[0-9a-f]{64}") && ids.add(c[0]), "stable unique source identity");
                kinds.add(c[3]);
                check(c[2].equals("Sample.java"), "relative source path");
                long start = Long.parseLong(c[7]);
                long end = Long.parseLong(c[8]);
                check(start >= 0 && end > start && end <= FIXTURE.length(), "UTF16 bounds");
                String slice = FIXTURE.substring((int) start, (int) end);
                check(c[9].equals(M3AtomCensus.digest(slice)), "exact UTF-16 slice SHA256");
                check(c[11].equals("true"), "contract locked");
                positions.add(start);
                if (c[3].equals("JAVA_METHOD") && c[5].equals("hash")) {
                    staticMethods++;
                    check(c[10].equals("HASH_SPREAD"), "existing LC/HR/GfG category used");
                }
            }
            check(kinds.containsAll(Set.of("FILE_CONTAINER", "DECLARATION_ONLY", "JAVA_METHOD",
                    "JAVA_CONSTRUCTOR", "JAVA_NATIVE_DECLARATION", "JAVA_LAMBDA",
                    "JAVA_FIELD_INITIALIZER", "JAVA_STATIC_INITIALIZER", "JAVA_INSTANCE_INITIALIZER")),
                    "all nine requested Java atom families covered");
            check(staticMethods == 1, "one hash pattern leaf");
            check(positions.equals(positions.stream().sorted().toList()), "source order deterministic");
            byte[] first = Files.readAllBytes(semantic.resolve("SEMANTIC_CENSUS_fixture.tsv"));
            M3AtomCensus.main(new String[]{"--semantic", "fixture", rules.toString(), semantic.toString(), src.toString()});
            check(java.util.Arrays.equals(first, Files.readAllBytes(
                    semantic.resolve("SEMANTIC_CENSUS_fixture.tsv"))), "second run fixed point");

            Path bad = Files.createDirectory(temp.resolve("bad"));
            Files.writeString(bad.resolve("Bad.java"), "class Bad { void broken( { }\n", StandardCharsets.UTF_8);
            Path failedOut = temp.resolve("bad-out");
            check(refusesIO(() -> M3AtomCensus.main(new String[]{"fixture", rules.toString(),
                    failedOut.toString(), bad.toString()}), "incomplete javac source parse"),
                    "parse errors are a hard failure");
            check(!Files.exists(failedOut), "parse error must not publish output");
            Path linkedRoot = Files.createDirectory(temp.resolve("linked"));
            Files.createSymbolicLink(linkedRoot.resolve("Aliased.java"), sample);
            check(refusesIO(() -> M3AtomCensus.main(new String[]{"fixture", rules.toString(),
                    temp.resolve("linked-out").toString(), linkedRoot.toString()}), "symlinked Java"),
                    "symlinked input refused");
            check(!Files.exists(temp.resolve("linked-out")), "symlink error must not publish output");
            return checks;
        } finally {
            try (var walk = Files.walk(temp)) {
                for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("M3_SEMANTIC_CENSUS_PROOF\tchecks=" + runAll() + "\tstatus=PASS");
    }

    @FunctionalInterface
    private interface RunnableWithException { void run() throws Exception; }
}
