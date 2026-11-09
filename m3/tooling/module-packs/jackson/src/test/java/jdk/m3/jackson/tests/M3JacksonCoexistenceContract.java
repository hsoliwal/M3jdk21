// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jackson.tests;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.io.Serial;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jdk.m3.jackson.M3File;
import jdk.m3.jackson.M3JsonLimits;
import jdk.m3.jackson.M3ObjectMapper;

/** Coexistence regressions for the two previously conflicting Jackson source families. */
public final class M3JacksonCoexistenceContract {
    private static int checks;

    private M3JacksonCoexistenceContract() { }

    public static void main(String[] args) throws Exception {
        checks = 0;
        Path directory = Files.createTempDirectory("m3-jackson-coexistence-");
        Path input = directory.resolve("unicode.txt");
        try {
            Files.writeString(input, "é", StandardCharsets.UTF_8);
            equal("é", M3File.read(input, StandardCharsets.UTF_8, 1).toString(), "existing UTF-16 ceiling");
            refuses(() -> M3File.readBytes(input, StandardCharsets.UTF_8, 1), "byte ceiling is distinct");
            M3File bytes = M3File.readBytes(input, StandardCharsets.UTF_8, 2);
            equal("é", bytes.source(), "strict byte snapshot content");
            equal(input.toAbsolutePath().normalize().toUri(), bytes.sourceUri(), "byte source URI");
            check(bytes.byteSha256() != null && bytes.byteSha256().length() == 64, "exact byte identity exists");
            equal(bytes.sourceSha256(), M3File.of(URI.create("memory:unicode"), "é").sourceSha256(),
                    "source identity ignores path and encoded-byte identity");

            String atom = new String(new char[] {'a', 'b'});
            M3File indexed = M3File.of(atom, atom, "");
            M3File labelled = M3File.of(URI.create("memory:indexed"), indexed);
            check(labelled.sourceTokenAt(0) == atom, "label keeps original canonical atom reference");
            equal(indexed.sourceTokenCount(), labelled.sourceTokenCount(), "label keeps occurrence lane");
            equal(indexed.sourceRetainedPrimitiveBytes(), labelled.sourceRetainedPrimitiveBytes(), "label keeps index payload");
            M3File sliced = labelled.slice(1, 3);
            equal("ba", sliced.source(), "snapshot API exports exact indexed view");
            equal(labelled.sourceUri(), sliced.sourceUri(), "slice keeps source label");
            equal(null, sliced.byteSha256(), "slice does not invent encoded bytes");
            equal(M3File.of(URI.create("memory:expected"), "ba").sourceSha256(), sliced.sourceSha256(), "slice hash is view-specific");
            try (var reader = sliced.openReader()) {
                equal((int) 'b', reader.read(), "openReader alias starts at view boundary");
                equal((int) 'a', reader.read(), "openReader alias keeps atom seam");
                equal(-1, reader.read(), "openReader alias stops at view end");
            }
            CountingCharacters mutable = new CountingCharacters("[1]");
            M3File snapshot = M3File.of(URI.create("memory:mutable"), mutable);
            mutable.text.append("changed");
            equal(1, mutable.snapshots, "mutable CharSequence snapshots once");
            equal("[1]", snapshot.source(), "mutable owner cannot change accepted snapshot");

            M3ObjectMapper mapper = new M3ObjectMapper(null);
            equal("JSON", mapper.getFactory().getFormatName(), "old null JsonFactory constructor remains unambiguous");
            equal(Map.of("n", 1), mapper.readValue(M3File.of("{\"n\":", "1}"), Map.class), "indexed binding retained");
            equal(List.of(1), mapper.readTreeValue(mapper.readAST(snapshot), new TypeReference<List<Integer>>() { }),
                    "new AST binding consumes existing M3File");
            equal("[1]", mapper.writeValueAsM3File(mapper.readAST(snapshot)).toString(), "AST and indexed output coexist");
            equal(List.of(1, 2), mapper.prepare(new TypeReference<List<Integer>>() { }).readValue(M3File.of("[1,2]")),
                    "prepared typed binding retained");
            try (var values = mapper.readValues(M3File.of("1 ", "2"), Integer.class)) {
                equal(List.of(1, 2), values.readAll(), "owned sequence binding retained");
            }
            M3JsonLimits limits = new M3JsonLimits(100, 100, 100, 100, 4);
            M3ObjectMapper limited = M3ObjectMapper.withLimits(limits);
            equal(limits, limited.copy().m3Limits(), "copy keeps selected AST limits");
            equal(limits, limited.copyWith(null).m3Limits(), "factory copy keeps selected limits");
            Derived copied = new Derived(limited);
            equal(limits, copied.m3Limits(), "existing protected copy constructor retained");
            JsonFactory replacement = new JsonFactory();
            Derived replaced = new Derived(limited, replacement);
            check(replaced.getFactory() == replacement, "existing protected factory-copy constructor retained");
            equal("[1]", replaced.writeValueAsString(replaced.readAST("[1]")), "protected copy retains AST datatype binding");
            refuses(() -> limited.readAST("[[[[[0]]]]]"), "copy composition retains limits refusal");
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(directory);
        }
        System.out.println("M3_JACKSON_COEXISTENCE_PASS checks=" + checks);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void equal(Object expected, Object actual, String message) {
        check(Objects.equals(expected, actual), message + ": expected=" + expected + ", actual=" + actual);
    }

    @FunctionalInterface private interface Operation { void run() throws IOException; }

    private static void refuses(Operation operation, String message) throws IOException {
        checks++;
        try { operation.run(); }
        catch (IOException expected) { return; }
        throw new AssertionError(message);
    }

    private static final class CountingCharacters implements CharSequence {
        private final StringBuilder text;
        private int snapshots;
        private CountingCharacters(String value) { text = new StringBuilder(value); }
        @Override public int length() { return text.length(); }
        @Override public char charAt(int index) { return text.charAt(index); }
        @Override public CharSequence subSequence(int start, int end) { return text.subSequence(start, end); }
        @Override public String toString() { snapshots++; return text.toString(); }
    }

    private static final class Derived extends M3ObjectMapper {
        @Serial private static final long serialVersionUID = 1L;
        private Derived(M3ObjectMapper source) { super(source); }
        private Derived(M3ObjectMapper source, JsonFactory factory) { super(source, factory); }
    }
}
