/*
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
 */

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * JUnit tests of the four generator atoms of pattern DECORATOR_FILL:
 * <ul>
 * <li>M3DecoratorRuleCheck: one test per classifier side-condition check;</li>
 * <li>M3DecoratorFill: one test per owned class, refilled from its scaffold in a fresh JVM and
 * byte-identical to the committed file (recipe replay is a fixed point);</li>
 * <li>M3DecoratorJdkStandard: one test per owned class, its layout is a fixed point;</li>
 * <li>M3DecoratorTemplates: each helper template compiles in a host class.</li>
 * </ul>
 * System properties: {@code m3.decorator.dir}, {@code m3.commons.sources} (the unpacked
 * commons-collections4-4.5.0 sources, required) and {@code m3.scaffold.rev} (default: the
 * scaffold revision of run.sh). Run from the repository root: the scaffolds are read with
 * {@code git show}, and the replay JVM uses this JVM's class path.
 */
final class M3DecoratorGeneratorTest {
    /** RULE_CHECKS.tsv columns: check, refutation, commons, input, expected, actual, result. */
    static final int COL_CHECK = 0;
    static final int COL_EXPECTED = 4;
    static final int COL_ACTUAL = 5;
    static final int COL_RESULT = 6;
    /** SCAFFOLD_SURFACE.tsv columns: module, Commons class, owned class. */
    static final int COL_MODULE = 0;
    static final int COL_COMMONS = 1;
    static final int COL_OWNED = 2;
    static final int SURFACE_COLUMNS = 3;
    static final int HEADER_ROWS = 1;
    static final int EXIT_OK = 0;
    static final String TAB = "\t";
    static final String PASS = "PASS";
    static final String EXPECTED = " expected ";
    static final String ACTUAL = " actual ";
    static final String JAVA_SUFFIX = ".java";
    static final String APACHE_MODULE = "apache-";
    static final String IMPLEMENTATIONS = "m3/collections/implementations/";
    static final String MAIN_JAVA = "/src/main/java/";
    static final String PROP_SOURCES = "m3.commons.sources";
    static final String PROP_REV = "m3.scaffold.rev";
    static final String PROP_OWNED = "m3.owned";
    static final String PROP_CLASS_PATH = "java.class.path";
    static final String PROP_JAVA_HOME = "java.home";
    static final String DEFAULT_REV = "bb66a8c16642a4e898dbc5fc7c3af0d5ed572136";
    static final String SURFACE_TSV =
            "m3/collections/qualification/five-provider-inventory-20261018/SCAFFOLD_SURFACE.tsv";
    static final String OUTPUT_CONTRACT = "OUTPUT_CONTRACT.tsv";
    static final String GENERATOR = "M3DecoratorFill";
    static final String SCAFFOLD = "scaffold.java";
    static final String ATOMS = "atoms.tsv";
    static final String HOST = "Host.java";
    static final String HOST_OPEN = "final class Host<K, V> {\n"
            + "    private V checkSetValue(final V value) {\n        return value;\n    }\n";
    static final String HOST_CLOSE = "}\n";
    static final String MAP_ITERATOR = "MapIterator";
    static final String ORDERED_MAP_ITERATOR = "OrderedMapIterator";
    static final String NO_MEMBERS = "";
    static final Path SOURCES = Path.of(System.getProperty(PROP_SOURCES, "."));
    static final String REV = System.getProperty(PROP_REV, DEFAULT_REV);

    @TestFactory
    Stream<DynamicTest> everyRuleCheck() throws Exception {
        Path out = Files.createTempFile(GENERATOR, ATOMS);
        M3DecoratorRuleCheck.run(SOURCES, out);
        return Files.readAllLines(out, StandardCharsets.UTF_8).stream().skip(HEADER_ROWS)
                .map(l -> l.split(TAB, -1)).map(c -> DynamicTest.dynamicTest(c[COL_CHECK],
                        () -> assertEquals(PASS, c[COL_RESULT], c[COL_CHECK] + EXPECTED
                                + c[COL_EXPECTED] + ACTUAL + c[COL_ACTUAL])));
    }

    @TestFactory
    Stream<DynamicTest> everyOwnedClassReplaysByteIdentical() throws IOException {
        Map<String, String> commons = surface();
        return owned().map(p -> DynamicTest.dynamicTest(p.getFileName().toString(), () -> {
            String path = p.toString().replace('\\', '/');
            Path work = Files.createTempDirectory(GENERATOR);
            Path scaffold = work.resolve(SCAFFOLD);
            Path out = work.resolve(p.getFileName());
            assertEquals(EXIT_OK, exec(List.of("git", "show", REV + ":" + path), scaffold));
            assertEquals(EXIT_OK, exec(List.of(java(), "-cp", System.getProperty(PROP_CLASS_PATH),
                    GENERATOR, SOURCES.toString(), scaffold.toString(), commons.get(path),
                    out.toString(), work.resolve(ATOMS).toString()), null));
            assertArrayEquals(Files.readAllBytes(p), Files.readAllBytes(out));
        }));
    }

    @TestFactory
    Stream<DynamicTest> everyOwnedClassLayoutIsAFixedPoint() throws IOException {
        return owned().map(p -> DynamicTest.dynamicTest(p.getFileName().toString(), () -> {
            String text = Files.readString(p, StandardCharsets.UTF_8);
            assertEquals(text, M3DecoratorJdkStandard.layout(text));
        }));
    }

    @TestFactory
    Stream<DynamicTest> everyHelperTemplateCompiles() {
        return Stream.of(
                Map.entry("H2 MAP_ITERATOR_ADAPTER", M3DecoratorTemplates.MAP_ITERATOR_HELPER),
                Map.entry("H3 CHECKED_ENTRY_SET", M3DecoratorTemplates.CHECKED_ENTRY_SET_HELPER),
                Map.entry("H4 UNMODIFIABLE_MAP_ITERATOR", M3DecoratorTemplates
                        .UNMODIFIABLE_MAP_ITERATOR_HELPER.formatted(MAP_ITERATOR, NO_MEMBERS)),
                Map.entry("H4 ordered", M3DecoratorTemplates.UNMODIFIABLE_MAP_ITERATOR_HELPER
                        .formatted(ORDERED_MAP_ITERATOR,
                        M3DecoratorTemplates.ORDERED_MAP_ITERATOR_MEMBERS)))
                .map(t -> DynamicTest.dynamicTest(t.getKey(),
                        () -> assertTrue(compiles(t.getValue()))));
    }

    /**
     * The owned classes: the list file {@code m3.owned} (one path per line, as run.sh writes it
     * during a run), else the Java rows of the last run's OUTPUT_CONTRACT.tsv.
     */
    private static Stream<Path> owned() throws IOException {
        String list = System.getProperty(PROP_OWNED, NO_MEMBERS);
        Stream<String> paths = list.isEmpty()
                ? Files.readAllLines(M3DecoratorAtomTest.DIR.resolve(OUTPUT_CONTRACT),
                        StandardCharsets.UTF_8).stream().skip(HEADER_ROWS)
                        .map(l -> l.split(TAB)[0])
                : Files.readAllLines(Path.of(list), StandardCharsets.UTF_8).stream();
        return paths.map(String::strip).filter(f -> f.endsWith(JAVA_SUFFIX)).map(Path::of);
    }

    /** Owned file path -> Commons class name, from the scaffold surface. */
    private static Map<String, String> surface() throws IOException {
        return Files.readAllLines(Path.of(SURFACE_TSV), StandardCharsets.UTF_8).stream()
                .skip(HEADER_ROWS).map(l -> l.split(TAB, -1))
                .filter(c -> c.length >= SURFACE_COLUMNS && c[COL_MODULE].startsWith(APACHE_MODULE))
                .collect(Collectors.toMap(c -> IMPLEMENTATIONS + c[COL_MODULE] + MAIN_JAVA
                        + c[COL_OWNED].replace('.', '/') + JAVA_SUFFIX, c -> c[COL_COMMONS],
                        (a, b) -> a));
    }

    private static boolean compiles(String helper) throws IOException {
        Path dir = Files.createTempDirectory(GENERATOR);
        Path host = dir.resolve(HOST);
        Files.writeString(host, HOST_OPEN + helper + HOST_CLOSE, StandardCharsets.UTF_8);
        return ToolProvider.getSystemJavaCompiler().run(null, null, null, "-proc:none",
                "-cp", System.getProperty(PROP_CLASS_PATH), "-d", dir.toString(),
                host.toString()) == EXIT_OK;
    }

    private static int exec(List<String> cmd, Path stdout) throws IOException,
            InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.DISCARD);
        pb.redirectOutput(stdout == null ? ProcessBuilder.Redirect.DISCARD
                : ProcessBuilder.Redirect.to(stdout.toFile()));
        return pb.start().waitFor();
    }

    private static String java() {
        return Path.of(System.getProperty(PROP_JAVA_HOME), "bin", "java").toString();
    }
}
