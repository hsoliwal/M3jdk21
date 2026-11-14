// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** Public optional-period codec contracts; each owned fixture uses at most one 4 KiB cluster. */
public final class M3CorpusOptionalPeriodQualification {
    private static Path root;
    private static String seed;
    private static int absentPlanes;
    private static int incumbentRefusalChecks;
    private static final Map<String, String> canonicalRoots = new TreeMap<>();
    private static final String DICT = "d".repeat(64), OUTPUT = "e".repeat(64);
    private interface FileAction { void run(Path file) throws Exception; }

    private static M3LanguageCorpusManifest.Artifact artifact(
            M3LanguageCorpusManifest.Plane plane, String tag, String period) {
        return new M3LanguageCorpusManifest.Artifact(plane, "hi",
                "fixture-" + seed + "-" + tag, "word", "word-v1", "nfc-v1",
                7L, -1L, DICT, OUTPUT,
                new M3CorpusSourceReceipt("HUGGING_FACE",
                        URI.create("https://huggingface.co/datasets/example/synthetic"),
                        "c".repeat(40), "synthetic-only", DICT),
                "synthetic test; no dataset payload", URI.create("https://example.org/terms"),
                plane == M3LanguageCorpusManifest.Plane.WORD_NGRAM ? 3 : 0, period);
    }
    private static M3LanguageCorpusManifest manifest(String tag, String period) {
        return M3LanguageCorpusManifest.of(List.of(artifact(
                M3LanguageCorpusManifest.Plane.WORD_FACTS, tag, period)));
    }

    private static void withFile(M3LanguageCorpusManifest manifest, FileAction action) throws Exception {
        byte[] bytes = manifest.canonicalBytes();
        assertTrue(bytes.length > 0 && bytes.length <= 4096, "fixture budget before publication");
        Path expected = root.resolve("corpus-" + manifest.root() + ".m3corpus").normalize();
        assertEquals(root, expected.getParent());
        assertTrue(Files.notExists(expected), "refuse preexisting fixture");
        Path created = manifest.writeNew(root);
        assertEquals(expected, created);
        try {
            assertArrayEquals(bytes, Files.readAllBytes(created));
            action.run(created);
        } finally {
            assertEquals(expected, created.normalize());
            Files.delete(created);
        }
    }

    @Test void absentPeriodRoundTripsEveryPlane() throws Exception {
        for (var plane : M3LanguageCorpusManifest.Plane.values()) {
            var original = M3LanguageCorpusManifest.of(List.of(artifact(plane, "absent-" + plane, null)));
            assertEquals("", original.artifacts().getFirst().period());
            canonicalRoots.put("absent-" + plane, original.root());
            withFile(original, path -> {
                var loaded = M3LanguageCorpusManifest.read(path);
                assertEquals(original.artifacts(), loaded.artifacts());
                assertEquals(original.root(), loaded.root());
                assertArrayEquals(original.canonicalBytes(), loaded.canonicalBytes());
                assertEquals("", loaded.artifacts().getFirst().period());
                assertEquals(original.artifacts(), loaded.requireBoundArtifactsForPlanes(
                        "hi", 7L, DICT, -1L, "word-v1", "nfc-v1", Set.of(plane)));
            });
            absentPlanes++;
        }
    }

    @Test void mixedAbsentAndPresentPeriodsRoundTrip() throws Exception {
        var artifacts = new ArrayList<M3LanguageCorpusManifest.Artifact>();
        int i = 0;
        for (var plane : M3LanguageCorpusManifest.Plane.values())
            artifacts.add(artifact(plane, "mixed", i++ % 2 == 0 ? null : "2020-2026"));
        var original = M3LanguageCorpusManifest.of(artifacts);
        canonicalRoots.put("mixed", original.root());
        withFile(original, path -> {
            var loaded = M3LanguageCorpusManifest.read(path);
            assertEquals(original.artifacts(), loaded.artifacts());
            assertEquals(original.root(), loaded.root());
            assertArrayEquals(original.canonicalBytes(), loaded.canonicalBytes());
            assertEquals(original.artifacts(), loaded.requireBoundArtifactsForPlanes(
                    "hi", 7L, DICT, -1L, "word-v1", "nfc-v1",
                    EnumSet.allOf(M3LanguageCorpusManifest.Plane.class)));
        });
    }

    @Test void presentPeriodCodeUnitsAndWhitespaceRemainExact() throws Exception {
        int i = 0;
        for (String period : List.of("2026", "2020-2026", " 2026 ", "\u4ee4\u548c8\u5e74")) {
            var original = manifest("present-" + i++, period);
            withFile(original, path -> {
                var loaded = M3LanguageCorpusManifest.read(path);
                assertEquals(period, loaded.artifacts().getFirst().period());
                assertArrayEquals(original.canonicalBytes(), loaded.canonicalBytes());
                assertEquals(original.root(), loaded.root());
            });
        }
    }

    @Test void explicitEmptyBlankAndMultilinePeriodsStillRefuse() {
        for (String period : List.of("", " ", "\t", "\r", "\n", "20\n26", "20\t26")) {
            var failure = assertThrows(IllegalArgumentException.class,
                    () -> artifact(M3LanguageCorpusManifest.Plane.WORD_FACTS, "bad-period", period));
            assertEquals("period must be single-line", failure.getMessage());
        }
        assertEquals("", artifact(M3LanguageCorpusManifest.Plane.WORD_FACTS, "null", null).period());
    }

    @Test void malformedOrMissingPeriodColumnStillRefuses() throws Exception {
        var original = manifest("malformed", "2026");
        String canonical = new String(original.canonicalBytes(), StandardCharsets.UTF_8);
        String[] lines = canonical.split("\n", -1);
        String[] cells = lines[1].split("\t", -1);
        assertEquals(20, cells.length);
        for (String replacement : List.of("%", "IA", "CQ", "Cg")) {
            String[] bad = cells.clone();
            bad[19] = replacement;
            byte[] malformed = (lines[0] + "\n" + String.join("\t", bad) + "\n")
                    .getBytes(StandardCharsets.UTF_8);
            withFile(original, path -> {
                Files.write(path, malformed, StandardOpenOption.TRUNCATE_EXISTING);
                var failure = assertThrows(IOException.class, () -> M3LanguageCorpusManifest.read(path));
                assertEquals("invalid corpus manifest line 2", failure.getMessage());
            });
        }
        String missing = lines[0] + "\n"
                + String.join("\t", Arrays.copyOf(cells, 19)) + "\n";
        withFile(original, path -> {
            Files.writeString(path, missing, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            var failure = assertThrows(IOException.class, () -> M3LanguageCorpusManifest.read(path));
            assertEquals("invalid corpus manifest line 2", failure.getMessage());
        });
    }

    @Test void noncanonicalPresentWireEncodingStillRefuses() throws Exception {
        var original = manifest("noncanonical", "2026");
        String text = new String(original.canonicalBytes(), StandardCharsets.UTF_8);
        String[] lines = text.split("\n", -1);
        String[] cells = lines[1].split("\t", -1);
        cells[19] += "==";
        String padded = lines[0] + "\n" + String.join("\t", cells) + "\n";
        for (String bad : List.of(text + "\n", padded)) {
            withFile(original, path -> {
                Files.writeString(path, bad, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
                var failure = assertThrows(IOException.class, () -> M3LanguageCorpusManifest.read(path));
                assertEquals("corpus manifest is not canonical", failure.getMessage());
            });
        }
    }

    @Test void writeNewIdempotenceAndCollisionRulesRemain() throws Exception {
        var original = manifest("write-new", null);
        withFile(original, path -> {
            assertEquals(path, original.writeNew(root));
            byte[] malformed = Arrays.copyOf(original.canonicalBytes(), original.canonicalBytes().length + 1);
            malformed[malformed.length - 1] = '\n';
            Files.write(path, malformed, StandardOpenOption.TRUNCATE_EXISTING);
            var failure = assertThrows(IOException.class, () -> original.writeNew(root));
            assertEquals("corpus manifest root collision", failure.getMessage());
            assertArrayEquals(malformed, Files.readAllBytes(path));
        });
    }

    @Test void nullConstructorKeepsExistingCanonicalEncoding() {
        var original = manifest("canonical-absence", null);
        String text = new String(original.canonicalBytes(), StandardCharsets.UTF_8);
        assertTrue(text.endsWith("\t\n"), "optional column retained with empty wire value");
        assertEquals(20, text.split("\n", -1)[1].split("\t", -1).length);
        assertEquals(M3CorpusProjectionDigest.bytes(original.canonicalBytes()), original.root());
        byte[] escaped = original.canonicalBytes();
        escaped[0] ^= 1;
        assertNotEquals(escaped[0], original.canonicalBytes()[0], "canonical bytes remain defensive");
        canonicalRoots.put("canonical-absence", original.root());
    }

    @Test void incumbentWriteFreeRefusalContractsRunUnmodified() throws Exception {
        Class<?> old = Class.forName("M3LanguageCorpusManifestRefusalContractTest");
        var checks = old.getDeclaredField("checks");checks.setAccessible(true);
        int before = checks.getInt(null);
        for (String name : List.of("artifactFields", "binding", "scopes", "receipts", "projectionDigest")) {
            var method = old.getDeclaredMethod(name);method.setAccessible(true);
            try { method.invoke(null); }
            catch (InvocationTargetException failure) { throw new AssertionError(name, failure.getCause()); }
        }
        incumbentRefusalChecks = checks.getInt(null) - before;
        assertTrue(incumbentRefusalChecks > 50, "actual incumbent checks executed");
    }

    @Test void fullIncumbentMainRunsUnmodifiedWithinOneFileBudget() throws Exception {
        Class<?> old = Class.forName("M3LanguageCorpusManifestTest");
        var row = old.getDeclaredMethod("row", M3LanguageCorpusManifest.Plane.class, int.class, String.class);
        row.setAccessible(true);
        var values = new ArrayList<M3LanguageCorpusManifest.Artifact>();
        for (var plane : M3LanguageCorpusManifest.Plane.values())
            values.add((M3LanguageCorpusManifest.Artifact)row.invoke(
                    null, plane, plane == M3LanguageCorpusManifest.Plane.WORD_NGRAM ? 3 : 0, "a".repeat(64)));
        assertTrue(M3LanguageCorpusManifest.of(values).canonicalBytes().length <= 4096);
        try { old.getMethod("main", String[].class).invoke(null, (Object)new String[]{}); }
        catch (InvocationTargetException failure) { throw new AssertionError("incumbent main", failure.getCause()); }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("mode, temp root, callback, seed");
        root = Path.of(args[1]).toAbsolutePath().normalize();seed = args[3];
        assertTrue(seed.matches("[0-9a-f]{32}"));
        assertTrue(Files.isDirectory(root));
        var vm = java.lang.management.ManagementFactory.getPlatformMXBean(
                com.sun.management.HotSpotDiagnosticMXBean.class);
        assertEquals("true", vm.getVMOption("PerfDisableSharedMem").getValue());
        assertEquals("false", vm.getVMOption("UsePerfData").getValue());
        assertEquals("false", vm.getVMOption("PerfDataSaveToFile").getValue());
        var listener = new SummaryGeneratingListener();
        var launcher = LauncherFactory.create();launcher.registerTestExecutionListeners(listener);
        launcher.execute(LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(M3CorpusOptionalPeriodQualification.class)).build());
        var summary = listener.getSummary();
        summary.printTo(new java.io.PrintWriter(System.out, true));
        summary.printFailuresTo(new java.io.PrintWriter(System.out, true));
        var report = new LinkedHashMap<String, Object>();
        report.put("mode", args[0]);report.put("found", summary.getTestsFoundCount());
        report.put("passed", summary.getTestsSucceededCount());report.put("failed", summary.getTestsFailedCount());
        report.put("aborted", summary.getTestsAbortedCount());report.put("absentPlanes", absentPlanes);
        report.put("incumbentRefusalChecks", incumbentRefusalChecks);report.put("canonicalRoots", canonicalRoots);
        report.put("failures", summary.getFailures().stream()
                .map(f -> f.getTestIdentifier().getDisplayName()).toList());
        byte[] data = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(report);
        var connection = (HttpURLConnection)URI.create(args[2]).toURL().openConnection();
        connection.setRequestMethod("POST");connection.setDoOutput(true);
        connection.setConnectTimeout(10000);connection.setReadTimeout(10000);
        connection.setFixedLengthStreamingMode(data.length);
        try (var out = connection.getOutputStream()) { out.write(data); }
        if (connection.getResponseCode() != 204) throw new AssertionError("parent refused report");
        connection.disconnect();
        if (summary.getTestsFoundCount() != 10 || summary.getTestsSucceededCount() != 10
                || summary.getTestsFailedCount() != 0 || summary.getTestsAbortedCount() != 0)
            throw new AssertionError("M3_CORPUS_OPTIONAL_PERIOD_DISCRIMINATOR_RED");
        System.out.println("M3_CORPUS_OPTIONAL_PERIOD_PASS tests=10 planes=" + absentPlanes);
    }
}
