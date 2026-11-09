// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import javax.tools.ToolProvider;
import org.apache.lucene.analysis.compound.hyphenation.Hyphenation;
import org.apache.lucene.analysis.compound.hyphenation.HyphenationTree;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Integration qualification only: no new product API or automatic language-profile admission. */
final class M3CaseHyphenTransportTest {
    static final String PROFILE = "case-hyphen-transport-proof-v1";
    static final String PATTERN_SHA = "f4ffcd96c5cbc886bdad23f95dcae8edc3cd3620eae62f7946eceda97c4e68f8";
    static final List<String> WORDS = List.of("HTTPServer", "PowerShot", "associate", "declination",
            "present", "table", "\u0915\u093F\u0924\u093E\u092C");
    static final Path PROOF = Path.of(System.getProperty("m3.proof.root"));
    static final String MUTANT = System.getProperty("m3.transport.mutant", "none");
    static Path fixture;
    static URLClassLoader loader;
    static Class<?> catalog;
    static Class<?> payload;
    static Method parser;
    static final Map<String, String> payloads = new TreeMap<>();
    static int donorCalls;

    static Object invoke(Object receiver, String name, Class<?>[] signature, Object... args) throws Exception {
        return invokeMethod(receiver.getClass().getMethod(name, signature), receiver, args);
    }
    static Object invokeMethod(Method method, Object receiver, Object... args) throws Exception {
        try { return method.invoke(receiver, args); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Exception cause) throw cause;
            if (failure.getCause() instanceof Error cause) throw cause;
            throw failure;
        }
    }
    static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    static String git(Path file) throws Exception {
        byte[] bytes = Files.readAllBytes(file);
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(("blob " + bytes.length + "\0").getBytes(StandardCharsets.US_ASCII));
        return HexFormat.of().formatHex(digest.digest(bytes));
    }
    static Path input(String property, String expected) throws Exception {
        Path path = Path.of(System.getProperty(property)).toRealPath();
        assertEquals(expected, git(path), property);
        return path;
    }
    @BeforeAll static void setup() throws Exception {
        assertTrue(List.of("none", "omit-context").contains(MUTANT));
        Path jar = Path.of(System.getProperty("m3.transport.jar")).toRealPath();
        assertEquals("b453fc1064e1470bdaa42ac6ae6fb7c9e1d77eca85d62282e3270365b21ab176",
                sha(Files.readAllBytes(jar)));
        List<Path> sources = List.of(
            input("m3.foundation.source", "6074d4cc060caa6c2b4588027dd25580ead900ea"),
            input("m3.window.source", "6f4c00a2bb3ad76e2788f74d3a7e5aac04478d7b"),
            input("m3.kind.source", "4e15c42958aec822cc156ded09eec90f56d0c1bd"),
            input("m3.bits.source", "5fd0d823c25c259ffbce93810d63492ec3b04e73"));
        Path classes = PROOF.resolve("transport-" + MUTANT + "/classes");
        Files.createDirectories(classes);
        List<String> argv = new ArrayList<>(List.of("--release", "21", "--enable-preview",
                "-Xlint:all,-preview", "-Werror", "-cp", jar.toString(), "-d", classes.toString()));
        for (Path source : sources) argv.add(source.toString());
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        int exit = ToolProvider.getSystemJavaCompiler().run(null, log, log, argv.toArray(String[]::new));
        Files.write(classes.getParent().resolve("javac.log"), log.toByteArray());
        assertEquals(0, exit, log.toString(StandardCharsets.UTF_8));
        loader = new URLClassLoader(new URL[] {classes.toUri().toURL(), jar.toUri().toURL()},
                ClassLoader.getPlatformClassLoader());
        catalog = loader.loadClass("jdk.m3.text.SharedLexiconCatalog");
        payload = loader.loadClass("jdk.m3.text.M3PrecomputePayload");
        assertEquals(jar.toUri().toURL(), catalog.getProtectionDomain().getCodeSource().getLocation());
        parser = payload.getMethod("parse", String.class);
        Method factory = HyphenationDonorTest.class.getDeclaredMethod("englishFixture");
        factory.setAccessible(true);
        Object english = factory.invoke(null);
        Method getter = english.getClass().getDeclaredMethod("tree");
        getter.setAccessible(true);
        HyphenationTree tree = (HyphenationTree) getter.invoke(english);
        fixture = classes.getParent().resolve("image");
        assertFalse(Files.exists(fixture), "fresh proof root required");
        Files.createDirectory(fixture);
        Class<?> foundation = loader.loadClass("FoundationTest");
        Method writer = foundation.getDeclaredMethod("writePrefixFixture", Path.class, List.class, boolean.class);
        writer.setAccessible(true);
        invokeMethod(writer, null, fixture, List.of(WORDS), false);
        List<String> mappings = Files.readAllLines(fixture.resolve("m3.records.tsv"));
        for (int i = 0; i < WORDS.size(); i++) {
            String word = WORDS.get(i);
            boolean languageWord = word.equals("associate") || word.equals("declination")
                    || word.equals("present") || word.equals("table");
            boolean unsupported = i == WORDS.size() - 1;
            int[] hyphens = new int[0];
            if (languageWord) {
                Hyphenation h = tree.hyphenate(word, 2, 3);
                donorCalls++;
                if (h != null) {
                    int[] raw = h.getHyphenationPoints();
                    hyphens = Arrays.stream(raw).filter(x -> x > 0 && x < word.length()).toArray();
                }
            }
            String source = languageWord || unsupported ? "tex-hyphen.en-us.fixture" : "m3.case.fixture";
            String record = word.equals("HTTPServer") || word.equals("associate") ? "collision" : word;
            int[] camel = boundary(0, word), pascal = boundary(1, word);
            String raw = "{\"source_id\":\"" + source + "\",\"record_id\":\"" + record
                    + "\",\"case_policy\":\"m3-unit-type-v1\",\"normalization\":\"identity-utf16\""
                    + ",\"requested_language\":\"" + (unsupported ? "hi" : languageWord ? "en-US" : "und")
                    + "\",\"resolved_language\":\"" + (languageWord ? "en-US" : "und")
                    + "\",\"hyphenation_supported\":" + languageWord
                    + ",\"pattern_sha256\":\"" + PATTERN_SHA + "\",\"engine_revision\":\"695c0ac84508438302cd346a812cfa2fdc5a10df\""
                    + ",\"left_minimum\":2,\"right_minimum\":3,\"coordinate_unit\":\"original-utf16\""
                    + ",\"utf16_length\":" + word.length() + ",\"camel_offsets\":" + Arrays.toString(camel)
                    + ",\"pascal_offsets\":" + Arrays.toString(pascal)
                    + ",\"hyphen_offsets\":" + Arrays.toString(hyphens) + "}";
            if (MUTANT.equals("omit-context")) raw = raw.replace(",\"pattern_sha256\":\"" + PATTERN_SHA + "\"", "");
            Object parsed = invokeMethod(parser, null, raw);
            String canonical = (String) invoke(parsed, "canonicalJson", new Class<?>[0]);
            payloads.put(word, canonical);
            String[] cells = mappings.get(i + 1).split("\t", -1);
            cells[0] = source; cells[2] = "qualification-fixture";
            cells[3] = languageWord ? "en-US" : unsupported ? "hi" : "und";
            cells[4] = record; cells[8] = "fixture_" + i; cells[9] = "FIXTURE_" + i;
            cells[11] = PROFILE; cells[12] = canonical;
            mappings.set(i + 1, String.join("\t", cells));
        }
        Files.write(fixture.resolve("m3.records.tsv"), mappings);
        Path facts = fixture.resolve("m3.precompute.tsv");
        Files.writeString(facts, Files.readString(facts).replace("prefix-proof", PROFILE));
        Path index = fixture.resolve("m3.precompute-index.tsv");
        String fingerprint = sha((PROFILE + "\t7\t7\n").getBytes(StandardCharsets.UTF_8));
        Files.writeString(index, "precompute_profile\tsource_records\timage_records\tsha256\n"
                + PROFILE + "\t7\t7\t" + fingerprint + "\n");
        Files.writeString(classes.getParent().resolve("production-scope.txt"),
                "Existing core JAR and boundary owner only. No product adapter, semantic profile validator, "
                + "new native path or corpus admission. donorCalls=" + donorCalls + "\n");
    }
    static int[] boundary(int kind, String text) throws Exception {
        Class<?> summary = loader.loadClass("jdk.m3.lexicon.M3WindowBoundarySummary");
        Class<?> enumeration = loader.loadClass("jdk.m3.lexicon.M3SeparatorKind");
        Object value = invokeMethod(summary.getMethod("of", enumeration, CharSequence.class),
                null, enumeration.getEnumConstants()[kind], text);
        long[] lanes = (long[]) invoke(value, "finish", new Class<?>[0]);
        return java.util.stream.IntStream.range(0, text.length())
                .filter(at -> (lanes[at >>> 6] & (1L << at)) != 0).toArray();
    }
    static Object open(boolean lazy) throws Exception {
        return invokeMethod(catalog.getMethod(lazy ? "openLazy" : "open", Path.class), null, fixture);
    }
    static Object lookup(Object c, String source, String record) throws Exception {
        Optional<?> result = (Optional<?>) invoke(c, "findPrecomputePayload",
                new Class<?>[] {String.class, String.class}, source, record);
        return result.orElseThrow();
    }
    static Object word(Object c, String text) throws Exception {
        boolean language = !text.equals("HTTPServer") && !text.equals("PowerShot");
        return lookup(c, language ? "tex-hyphen.en-us.fixture" : "m3.case.fixture",
                text.equals("HTTPServer") || text.equals("associate") ? "collision" : text);
    }
    static String string(Object p, String field) throws Exception {
        return (String) invoke(p, "requireString", new Class<?>[] {String.class}, field);
    }
    static int[] offsets(Object p, String field) throws Exception {
        return (int[]) invoke(p, "requireIntArray", new Class<?>[] {String.class}, field);
    }
    static void close(Object value) throws Exception { invoke(value, "close", new Class<?>[0]); }
    @AfterAll static void teardown() throws Exception { if (loader != null) loader.close(); }

    @Test void mappedCatalogRetainsContextAndOriginalSourceIdentity() throws Exception {
        for (boolean lazy : List.of(false, true)) {
            Object c = open(lazy);
            try {
                Object a = word(c, "HTTPServer"), b = word(c, "associate");
                assertEquals("collision", string(a, "record_id"));
                assertEquals("collision", string(b, "record_id"));
                assertEquals("m3.case.fixture", string(a, "source_id"));
                assertEquals("tex-hyphen.en-us.fixture", string(b, "source_id"));
                assertNotSame(a, b);
                for (String value : WORDS) {
                    Object p = word(c, value);
                    @SuppressWarnings("unchecked")
                    java.util.Set<String> names = (java.util.Set<String>) invoke(p, "fieldNames", new Class<?>[0]);
                    assertTrue(names.contains("pattern_sha256"), "Missing behavior-bearing context");
                    assertEquals(PATTERN_SHA, string(p, "pattern_sha256"));
                    assertEquals("original-utf16", string(p, "coordinate_unit"));
                    assertEquals(payloads.get(value), invoke(p, "canonicalJson", new Class<?>[0]));
                }
            } finally { close(c); }
        }
    }
    @Test void realPrecomputedOffsetsMatchLiteralWordAndIdentifierCuts() throws Exception {
        for (boolean lazy : List.of(false, true)) {
            Object c = open(lazy);
            try {
                assertArrayEquals(new int[] {4}, offsets(word(c, "HTTPServer"), "pascal_offsets"));
                assertArrayEquals(new int[0], offsets(word(c, "HTTPServer"), "camel_offsets"));
                assertArrayEquals(new int[] {5}, offsets(word(c, "PowerShot"), "camel_offsets"));
                assertArrayEquals(new int[] {2, 4}, offsets(word(c, "associate"), "hyphen_offsets"));
                assertArrayEquals(new int[] {3, 5, 7}, offsets(word(c, "declination"), "hyphen_offsets"));
                String original = "\uD83D\uDE80::associate/end";
                int[] points = offsets(word(c, "associate"), "hyphen_offsets");
                assertArrayEquals(new int[] {6, 8}, Arrays.stream(points).map(x -> x + 4).toArray());
                assertEquals("so", original.substring(6, 8));
            } finally { close(c); }
        }
    }
    @Test void supportedEmptyAndUnsupportedRemainDistinct() throws Exception {
        Object c = open(true);
        try {
            Object present = word(c, "present"), unsupported = word(c, WORDS.getLast()), table = word(c, "table");
            assertEquals(true, invoke(present, "requireBoolean", new Class<?>[] {String.class}, "hyphenation_supported"));
            assertEquals(false, invoke(unsupported, "requireBoolean", new Class<?>[] {String.class}, "hyphenation_supported"));
            assertArrayEquals(new int[0], offsets(present, "hyphen_offsets"));
            assertArrayEquals(new int[0], offsets(unsupported, "hyphen_offsets"));
            assertArrayEquals(new int[0], offsets(table, "hyphen_offsets"));
            assertEquals("hi", string(unsupported, "requested_language"));
            assertEquals("und", string(unsupported, "resolved_language"));
        } finally { close(c); }
    }
    @Test void warmReadsReusePayloadWithoutCallingProducerOrLeakingArrays() throws Exception {
        int before = donorCalls;
        for (boolean lazy : List.of(false, true)) {
            Object c = open(lazy);
            try {
                Object p = word(c, "associate");
                int[] copy = offsets(p, "hyphen_offsets"); copy[0] = 999;
                for (int i = 0; i < 10000; i++) assertSame(p, word(c, "associate"));
                assertArrayEquals(new int[] {2, 4}, offsets(p, "hyphen_offsets"));
                assertEquals(before, donorCalls);
            } finally { close(c); }
            assertThrows(IllegalStateException.class, () -> word(c, "associate"));
        }
        assertEquals(4, donorCalls);
    }
    @Test void changingContextIsLosslessButDoesNotClaimSemanticRuntimeValidation() throws Exception {
        Object c = open(false);
        try {
            String original = payloads.get("associate");
            for (String altered : List.of(original.replace(PATTERN_SHA, "different-pattern"),
                    original.replace("\"left_minimum\":2", "\"left_minimum\":3"),
                    original.replace("en-US", "en-GB"))) {
                Object changed = invokeMethod(parser, null, altered);
                assertNotEquals(original, invoke(changed, "canonicalJson", new Class<?>[0]));
            }
            assertSame(word(c, "associate"), word(c, "associate"));
            assertThrows(IllegalArgumentException.class, () -> invokeMethod(parser, null,
                    "{\"hyphen_offsets\":[[2,4]]}"));
            assertThrows(IllegalArgumentException.class, () -> invokeMethod(parser, null,
                    "{\"hyphen_offsets\":null}"));
        } finally { close(c); }
    }
}
