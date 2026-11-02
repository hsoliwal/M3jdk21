/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.ToolProvider;

/** Bounded independent Java21 javac -h evidence for JNI declaration source identities. */
public final class M3JniDeclarationProjectionSelfTest {
    private static final String JAVA = """
            package demo.pkg;
            public class Outer {
                public static native int count(int[] values);
                public static native int ping();
                public native int overloaded(int n);
                public native int overloaded(byte[] n);
                public static class Inner { public native int do_Thing(int x); }
                void local() { class Local { native int hidden(int n); } }
            }
            """;
    private int checks;

    private M3JniDeclarationProjectionSelfTest() { }

    private void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }

    /** Executes the compiler, source-span and cross-mode projection contracts. */
    public static int runAll() throws Exception {
        M3JniDeclarationProjectionSelfTest self = new M3JniDeclarationProjectionSelfTest();
        self.run();
        return self.checks;
    }

    private void run() throws Exception {
        Path tmp = Files.createTempDirectory("m3-jni-binding-");
        try {
            Path source = tmp.resolve("src/demo/pkg/Outer.java");
            Files.createDirectories(source.getParent());
            Files.writeString(source, JAVA, StandardCharsets.UTF_8);
            Path rules = tmp.resolve("PATTERN_RULES.tsv");
            Files.writeString(rules, "pattern\tname\tbody\tcatalogue\n", StandardCharsets.UTF_8);
            Path legacy = tmp.resolve("legacy");
            Path semantic = tmp.resolve("semantic");
            Path jni = tmp.resolve("jni");
            M3AtomCensus.main(new String[]{"fixture", rules.toString(), legacy.toString(),
                    tmp.resolve("src").toString()});
            M3AtomCensus.main(new String[]{"--semantic", "fixture", rules.toString(),
                    semantic.toString(), tmp.resolve("src").toString()});
            M3AtomCensus.main(new String[]{"--jni", "fixture", rules.toString(),
                    jni.toString(), tmp.resolve("src").toString()});
            for (String filename : List.of("ATOM_CENSUS_fixture.tsv", "PATTERN_SUMMARY_fixture.tsv")) {
                check(Files.mismatch(legacy.resolve(filename), jni.resolve(filename)) == -1L,
                        "default/opt-in legacy bytes: " + filename);
            }
            check(Files.mismatch(semantic.resolve("SEMANTIC_CENSUS_fixture.tsv"),
                    jni.resolve("SEMANTIC_CENSUS_fixture.tsv")) == -1L,
                    "opt-in JNI preserves all semantic atom bytes");
            List<String> rows = Files.readAllLines(jni.resolve("JNI_DECLARATIONS_fixture.tsv"),
                    StandardCharsets.UTF_8);
            check(rows.size() == 7, "exactly six native methods and one header");
            check(rows.getFirst().equals(M3JniDeclarationProjection.HEADER), "closed TSV schema");
            Map<String, String[]> byMethod = new HashMap<>();
            Map<String, String[]> semanticById = new HashMap<>();
            for (String line : Files.readAllLines(jni.resolve("SEMANTIC_CENSUS_fixture.tsv"),
                    StandardCharsets.UTF_8).subList(1,
                    Files.readAllLines(jni.resolve("SEMANTIC_CENSUS_fixture.tsv")).size())) {
                String[] cells = line.split("\t", -1);
                semanticById.put(cells[0], cells);
            }
            Set<String> ids = new HashSet<>();
            for (String line : rows.subList(1, rows.size())) {
                String[] f = line.split("\t", -1);
                check(f.length == 11, "exact JNI candidate column count");
                check(ids.add(f[0]), "one record per source atom");
                String[] semanticRow = semanticById.get(f[0]);
                check(semanticRow != null && semanticRow[3].equals("JAVA_NATIVE_DECLARATION"),
                        "JNI joins the existing native-declaration atom");
                check(f[8].equals(semanticRow[9]), "JNI source digest is original atom digest");
                int first = Integer.parseInt(f[6]);
                int last = Integer.parseInt(f[7]);
                check(f[8].equals(M3AtomCensus.digest(JAVA.substring(first, last))),
                        "exact UTF-16 native declaration slice");
                byMethod.put(f[3] + "." + f[4] + f[5], f);
            }
            String[] count = byMethod.get("demo.pkg.Outer.count(int[])int");
            String[] ping = byMethod.get("demo.pkg.Outer.ping()int");
            String[] nested = byMethod.get("demo.pkg.Outer$Inner.do_Thing(int)int");
            String[] local = byMethod.get("demo.pkg.Outer$Local.hidden(int)int");
            check(count != null && count[9].equals("Java_demo_pkg_Outer_count")
                    && count[10].equals("SHORT_SYMBOL_CANDIDATE"), "primitive-array JNI short name");
            check(ping != null && ping[9].equals("Java_demo_pkg_Outer_ping"), "zero-arg JNI short name");
            check(nested != null && nested[9].equals("Java_demo_pkg_Outer_00024Inner_do_1Thing"),
                    "inner-class dollar and method underscore mangling");
            check(local != null && local[9].equals("-")
                    && local[10].equals("UNRESOLVED_LOCAL_CLASS"), "local binary name must not be invented");
            for (String name : List.of("overloaded(int)int", "overloaded(byte[])int")) {
                String[] row = byMethod.get("demo.pkg.Outer." + name);
                check(row != null && row[10].equals("OVERLOAD_REQUIRES_JAVAC_H"),
                        "native overload requires independent long-name proof: " + name);
            }
            check(M3JniDeclarationProjection.encode("é", false).equals("_000e9"),
                    "UTF-16 non-ASCII escape");
            check(M3JniDeclarationProjection.encode("_[];", false).equals("_1_3_0005d_2"),
                    "JNI special escapes and non-special code units");
            Path classes = Files.createDirectory(tmp.resolve("classes"));
            Path headers = Files.createDirectory(tmp.resolve("headers"));
            int compile = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                    "-encoding", "UTF-8", "-d", classes.toString(), "-h", headers.toString(), source.toString());
            check(compile == 0, "independent javac -h compiler completed");
            String allHeaders;
            try (var walk = Files.list(headers)) {
                allHeaders = String.join("\n", walk.sorted().map(path -> {
                    try {
                        return Files.readString(path, StandardCharsets.UTF_8);
                    } catch (IOException exception) {
                        throw new IllegalStateException(exception);
                    }
                }).toList());
            }
            for (String expected : List.of(count[9], ping[9], nested[9])) {
                check(allHeaders.contains("JNICALL " + expected), "JNI short name in actual javac -h: " + expected);
            }
            check(allHeaders.contains("JNICALL Java_demo_pkg_Outer_overloaded__I"),
                    "actual overload long symbol (int) requires header");
            check(allHeaders.contains("JNICALL Java_demo_pkg_Outer_overloaded___3B"),
                    "actual overload long symbol (byte[]) requires header");
            byte[] first = Files.readAllBytes(jni.resolve("JNI_DECLARATIONS_fixture.tsv"));
            M3AtomCensus.main(new String[]{"--jni", "fixture", rules.toString(),
                    jni.toString(), tmp.resolve("src").toString()});
            check(Arrays.equals(first, Files.readAllBytes(jni.resolve("JNI_DECLARATIONS_fixture.tsv"))),
                    "second-run JNI mapping is a byte-exact fixed point");
            Path invalid = Files.createDirectory(tmp.resolve("invalid"));
            Files.writeString(invalid.resolve("Bad.java"), "class Bad { native void x( }", StandardCharsets.UTF_8);
            Path rejected = tmp.resolve("rejected");
            boolean refused = false;
            try {
                M3AtomCensus.main(new String[]{"--jni", "fixture", rules.toString(),
                        rejected.toString(), invalid.toString()});
            } catch (IOException expected) {
                refused = expected.getMessage().contains("incomplete javac source parse");
            }
            check(refused && !Files.exists(rejected), "parse errors refuse before any output is published");
            Path aliasRoot = Files.createDirectory(tmp.resolve("aliases"));
            Files.createSymbolicLink(aliasRoot.resolve("Alias.java"), source);
            Path symlinkOutput = tmp.resolve("symlink-output");
            refused = false;
            try {
                M3AtomCensus.main(new String[]{"--jni", "fixture", rules.toString(),
                        symlinkOutput.toString(), aliasRoot.toString()});
            } catch (IOException expected) {
                refused = expected.getMessage().contains("symlinked Java");
            }
            check(refused && !Files.exists(symlinkOutput), "symlink source refused before publication");
            check(M3JniDeclarationProjection.shortName("demo/pkg/Outer$Inner", "do_Thing")
                    .equals(nested[9]), "pure JNI name helper is deterministic");
        } finally {
            try (var walk = Files.walk(tmp)) {
                for (Path file : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("M3_JNI_PROJECTION_PASS|checks=" + runAll());
    }
}
