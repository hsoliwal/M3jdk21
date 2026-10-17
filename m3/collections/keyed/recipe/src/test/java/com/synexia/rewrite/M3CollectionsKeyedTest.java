// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Reuses the sealed snapshot proof/application pattern; never copies templates as production. */
public final class M3CollectionsKeyedTest {
    private static final String CRATE = "collections-keyed";
    private static final String JAVA = "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/";
    private static final String TEXT = "/com/synexia/rewrite/hash-pinned-text/" + CRATE + "/";
    @Test void exactEmissionAndFixedPoint() throws Exception { emit(); }
    private static Map<String, String> emit() throws Exception {
        var ctx = context(); var recipe = named();
        var results = recipe.run(new InMemoryLargeSourceSet(parse(Map.of("Other.java", "class Other {}\n"))), ctx, 1)
                .getChangeset().getAllResults();
        Map<String, String> emitted = new TreeMap<>();
        for (var result : results) {
            assertNull(result.getBefore()); assertNotNull(result.getAfter());
            emitted.put(result.getAfter().getSourcePath().toString().replace('\\', '/'), result.getAfter().printAll());
        }
        assertEquals(19, emitted.size()); assertEquals(expected(), emitted);
        assertTrue(recipe.run(new InMemoryLargeSourceSet(parse(emitted)), ctx, 1).getChangeset().getAllResults().isEmpty());
        return emitted;
    }
    @Test void sortedManifestAndExactResourceHashes() throws Exception {
        for (String root : List.of(JAVA, TEXT)) {
            String last = "";
            for (String[] row : rows(root)) {
                assertEquals(4, row.length); assertTrue(last.compareTo(row[0]) < 0); last = row[0];
                assertEquals("ABSENT", row[1]); assertEquals(row[2], sha(resource(root + row[3])));
            }
        }
        assertEquals(14, new M3HashPinnedJavaSnapshotRecipe(CRATE).targetPaths().size());
        assertEquals(5, new M3HashPinnedTextSnapshotRecipe(CRATE).targetPaths().size());
    }
    @Test void inlineStorageAtomsMatchPinnedOwners() throws Exception {
        String synexia = resource(JAVA + "donors/PackedHashMap.java.txt");
        String m3 = resource(JAVA + "donors/M3LinkedHashMap.java.txt");
        String arrays = resource(JAVA + "donors/M3SlotArrays.java.txt");
        String cache = resource(JAVA + "M3PlanCache.java.txt");
        assertEquals("a7060599b2f33bc14c168e9e2394fc3f49526d2f5d7dbb249e3c580ebcdfec9e", sha(synexia));
        assertEquals("5495a8200ff4030a8faa66fde2f8bb9ccca52bdcbccd200a76147698289c2c0c", sha(m3));
        assertEquals("4e978861b03650cec9ab1f064648cc8d342117d8b4f0556152604707c83c2518", sha(arrays));
        for (String signature : List.of("int find(Object key, int hash)", "int vacant(int hash)",
                "void rehash(int capacity)", "void unlink(int s)")) {
            assertEquals(atom(synexia, signature), atom(m3, signature));
            assertEquals(atom(m3, signature), atom(cache, signature));
        }
        for (String signature : List.of("int hash(Object key)", "int doubled(int old)"))
            assertEquals(atom(arrays, signature), atom(cache, signature));
    }
    // Proof-only extraction for these sealed brace-balanced donor atoms; never a source transformer.
    private static String atom(String source, String signature) {
        int start = source.indexOf(signature); assertTrue(start >= 0, signature);
        assertEquals(start, source.lastIndexOf(signature), "duplicate " + signature);
        int open = source.indexOf('{', start), depth = 1, end = open + 1;
        while (depth != 0 && end < source.length()) {
            char c = source.charAt(end++); if (c == '{') depth++; else if (c == '}') depth--;
        }
        assertEquals(0, depth, signature);
        return source.substring(start, end).replaceAll("\\s+", " ").strip();
    }
    @Test void refusesOccupiedAndDriftedTargets() {
        expected().forEach((path, text) -> {
            refuses(path, text + "\n", false, "source drift");
            refuses(path, path.endsWith(".java") ? "class Occupied {}\n" : "occupied\n", false, "source drift");
        });
    }
    @Test void refusesDuplicateJavaAndTextTargets() {
        for (String path : List.of(new M3HashPinnedJavaSnapshotRecipe(CRATE).targetPaths().getFirst(),
                                  new M3HashPinnedTextSnapshotRecipe(CRATE).targetPaths().getFirst())) {
            refuses(path, expected().get(path), true, "duplicate target");
        }
    }
    @Test void refusesNonAstJavaTargets() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        for (String path : recipe.targetPaths()) {
            var ctx = context(); var inventory = recipe.getInitialValue(ctx);
            recipe.getScanner(inventory).visit(PlainText.builder().sourcePath(Path.of(path)).text(expected().get(path)).build(), ctx);
            assertTrue(assertThrows(IllegalStateException.class, () -> recipe.generate(inventory, ctx)).getMessage().contains("not a Java compilation unit"));
        }
    }
    @Test void filesystemDriftRefusesBeforeAnyWrite(@TempDir Path root) throws Exception {
        Map<String, String> after = expected(); String occupied = after.keySet().stream().toList().getLast();
        Path target = root.resolve(occupied); Files.createDirectories(target.getParent()); Files.writeString(target, "drift");
        assertThrows(AssertionError.class, () -> materialize(root, after));
        assertFalse(Files.exists(root.resolve(after.keySet().iterator().next()))); assertEquals("drift", Files.readString(target));
    }
    @Test void filesystemReplayAndReceipt(@TempDir Path root) throws Exception {
        materialize(root, expected()); materialize(root, expected());
        try (var paths = Files.walk(root)) { assertEquals(20L, paths.filter(Files::isRegularFile).count()); }
        for (var entry : expected().entrySet()) assertEquals(entry.getValue(), Files.readString(root.resolve(entry.getKey())));
    }
    @Test void filesystemSymlinkRefusal(@TempDir Path root) throws Exception {
        Path other = root.resolve("other"); Files.createDirectories(other);
        Files.createSymbolicLink(root.resolve("m3"), other);
        assertThrows(AssertionError.class, () -> materialize(root, expected()));
        try (var paths = Files.list(other)) { assertEquals(0L, paths.count()); }
    }
    @Test void filesystemNonDirectoryParentRefusesBeforeAnyWrite(@TempDir Path root) throws Exception {
        Path parent = root.resolve("m3/collections/keyed/src/test/java/com/m3/collections/parallel");
        Files.createDirectories(parent.getParent()); Files.writeString(parent, "occupied parent");
        assertThrows(AssertionError.class, () -> materialize(root, expected()));
        assertFalse(Files.exists(root.resolve("m3/collections/keyed/README.md")));
        assertEquals("occupied parent", Files.readString(parent));
    }
    @Test void filesystemReceiptDriftRefusesBeforeAnyWrite(@TempDir Path root) throws Exception {
        Path receipt = root.resolve("m3/collections/keyed/recipe-receipt.tsv");
        Files.createDirectories(receipt.getParent()); Files.writeString(receipt, "untrusted receipt");
        assertThrows(AssertionError.class, () -> materialize(root, expected()));
        assertFalse(Files.exists(root.resolve("m3/collections/keyed/README.md")));
    }
    @Test void filesystemRootFileRefused(@TempDir Path parent) throws Exception {
        Path root = parent.resolve("not-a-directory"); Files.writeString(root, "occupied");
        assertThrows(AssertionError.class, () -> materialize(root, expected()));
        assertEquals("occupied", Files.readString(root));
    }
    @Test void namedRecipeIsExistingEngineComposition() {
        List<String> engines = new ArrayList<>(); collect(named(), engines);
        assertEquals(Set.of("M3HashPinnedJavaSnapshotRecipe", "M3HashPinnedTextSnapshotRecipe"), Set.copyOf(engines));
        assertEquals(2, engines.size());
    }
    /** Invoked only by the post-test Maven verify profile, never by a test side effect. */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("output root and passing Surefire report required");
        requirePassingReport(Path.of(args[1]));
        materialize(Path.of(args[0]), emit());
    }
    private static void requirePassingReport(Path report) throws Exception {
        if (!Files.isRegularFile(report) || Files.isSymbolicLink(report))
            throw new IllegalStateException("missing or symlinked recipe proof report");
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
        var suite = factory.newDocumentBuilder().parse(report.toFile()).getDocumentElement();
        Set<String> required = new java.util.HashSet<>();
        for (var method : M3CollectionsKeyedTest.class.getDeclaredMethods())
            if (method.isAnnotationPresent(Test.class)) required.add(method.getName());
        if (!"testsuite".equals(suite.getTagName()) ||
                !M3CollectionsKeyedTest.class.getName().equals(suite.getAttribute("name")) ||
                !Integer.toString(required.size()).equals(suite.getAttribute("tests")) ||
                !"0".equals(suite.getAttribute("errors")) || !"0".equals(suite.getAttribute("failures")) ||
                !"0".equals(suite.getAttribute("skipped")))
            throw new IllegalStateException("recipe proof suite did not pass completely");
        var cases = suite.getElementsByTagName("testcase"); Set<String> found = new java.util.HashSet<>();
        for (int i = 0; i < cases.getLength(); i++) {
            var entry = (org.w3c.dom.Element) cases.item(i);
            if (!M3CollectionsKeyedTest.class.getName().equals(entry.getAttribute("classname")) ||
                    !found.add(proofMethod(entry.getAttribute("name"))) || entry.getElementsByTagName("failure").getLength() != 0 ||
                    entry.getElementsByTagName("error").getLength() != 0 || entry.getElementsByTagName("skipped").getLength() != 0)
                throw new IllegalStateException("invalid recipe testcase evidence");
        }
        if (!found.equals(required)) throw new IllegalStateException("recipe testcase inventory mismatch");
    }
    private static String proofMethod(String reported) {
        // JUnit Platform/Surefire may retain Java parameter signatures, not just method names.
        int bracket = reported.indexOf('(');
        if (bracket < 0) return reported;
        String name = reported.substring(0, bracket), parameters = reported.substring(bracket);
        for (var method : M3CollectionsKeyedTest.class.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Test.class) || !method.getName().equals(name)) continue;
            String qualified = java.util.Arrays.stream(method.getParameterTypes())
                    .map(Class::getName).collect(java.util.stream.Collectors.joining(", ", "(", ")"));
            String simple = java.util.Arrays.stream(method.getParameterTypes())
                    .map(Class::getSimpleName).collect(java.util.stream.Collectors.joining(", ", "(", ")"));
            if (parameters.equals(qualified) || parameters.equals(simple)) return name;
        }
        throw new IllegalStateException("unrecognized proof method signature");
    }
    @Test void refusesMissingFailedSkippedOrForeignProof(@TempDir Path root) throws Exception {
        Path report = root.resolve("report.xml");
        assertThrows(IllegalStateException.class, () -> requirePassingReport(report));
        String valid = samplePassingReport();
        for (String bad : List.of(valid.replace("failures=\"0\"", "failures=\"1\""),
                valid.replace("skipped=\"0\"", "skipped=\"1\""),
                valid.replace("errors=\"0\"", "errors=\"1\""),
                valid.replace("com.synexia.rewrite.M3CollectionsKeyedTest", "ForeignTest"))) {
            Files.writeString(report, bad);
            assertThrows(IllegalStateException.class, () -> requirePassingReport(report));
        }
    }
    @Test void proofInventoryAndSafeXmlParsing(@TempDir Path root) throws Exception {
        Path report = root.resolve("report.xml");
        Files.writeString(report, samplePassingReport()); requirePassingReport(report);
        Files.writeString(report, samplePassingReport()
                .replace("filesystemRootFileRefused", "filesystemRootFileRefused(java.nio.file.Path)")
                .replace("filesystemReplayAndReceipt", "filesystemReplayAndReceipt(Path)")
                .replace("exactEmissionAndFixedPoint", "exactEmissionAndFixedPoint()"));
        requirePassingReport(report);
        assertThrows(IllegalStateException.class, () -> proofMethod("filesystemRootFileRefused(String)"));
        Files.writeString(report, samplePassingReport().replace("exactEmissionAndFixedPoint", "unknownTest"));
        assertThrows(IllegalStateException.class, () -> requirePassingReport(report));
        Files.writeString(report, "<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///missing'>]><testsuite>&e;</testsuite>");
        assertThrows(org.xml.sax.SAXException.class, () -> requirePassingReport(report));
    }
    @Test void failedGateCannotCreateOutput(@TempDir Path root) throws Exception {
        Path output = root.resolve("output"), report = root.resolve("missing.xml");
        assertThrows(IllegalStateException.class, () -> main(new String[]{output.toString(), report.toString()}));
        assertFalse(Files.exists(output));
    }
    private static String samplePassingReport() {
        List<String> methods = java.util.Arrays.stream(M3CollectionsKeyedTest.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Test.class)).map(java.lang.reflect.Method::getName).sorted().toList();
        StringBuilder xml = new StringBuilder("<testsuite name=\"" + M3CollectionsKeyedTest.class.getName() +
                "\" tests=\"" + methods.size() + "\" errors=\"0\" failures=\"0\" skipped=\"0\">");
        for (String method : methods) xml.append("<testcase classname=\"").append(M3CollectionsKeyedTest.class.getName())
                .append("\" name=\"").append(method).append("\"/>");
        return xml.append("</testsuite>").toString();
    }
    private static void collect(Recipe recipe, List<String> engines) {
        if (recipe instanceof M3HashPinnedJavaSnapshotRecipe || recipe instanceof M3HashPinnedTextSnapshotRecipe) engines.add(recipe.getClass().getSimpleName());
        else recipe.getRecipeList().forEach(child -> collect(child, engines));
    }
    private static void refuses(String path, String text, boolean duplicate, String message) {
        var ctx = context(); SourceFile source = parse(Map.of(path, text)).getFirst();
        if (path.endsWith(".java")) {
            var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE); var state = recipe.getInitialValue(ctx); var scanner = recipe.getScanner(state);
            scanner.visit(source, ctx); if (duplicate) scanner.visit(source, ctx);
            assertTrue(assertThrows(IllegalStateException.class, () -> recipe.generate(state, ctx)).getMessage().contains(message));
        } else {
            var recipe = new M3HashPinnedTextSnapshotRecipe(CRATE); var state = recipe.getInitialValue(ctx); var scanner = recipe.getScanner(state);
            scanner.visit(source, ctx); if (duplicate) scanner.visit(source, ctx);
            assertTrue(assertThrows(IllegalStateException.class, () -> recipe.generate(state, ctx)).getMessage().contains(message));
        }
    }
    private static void materialize(Path destination, Map<String, String> emitted) throws Exception {
        Path root = destination.toAbsolutePath().normalize(); Map<String, String> all = new TreeMap<>(emitted);
        StringBuilder receipt = new StringBuilder("# Executed OpenRewrite; exact emitted UTF-8 postimages\n");
        for (var entry : emitted.entrySet()) receipt.append(entry.getKey()).append('\t').append(sha(entry.getValue())).append('\n');
        all.put("m3/collections/keyed/recipe-receipt.tsv", receipt.toString());
        // Global preflight, then serial application. No claim of a filesystem transaction.
        for (var entry : all.entrySet()) {
            Path path = root.resolve(entry.getKey()).normalize(); assertTrue(path.startsWith(root.resolve("m3/collections/keyed")));
            for (Path ancestor = path; ancestor != null; ancestor = ancestor.getParent()) {
                assertFalse(Files.isSymbolicLink(ancestor), ancestor.toString());
                if (!ancestor.equals(path) && Files.exists(ancestor))
                    assertTrue(Files.isDirectory(ancestor), "non-directory parent " + ancestor);
            }
            if (Files.exists(path)) { assertTrue(Files.isRegularFile(path)); assertEquals(entry.getValue(), Files.readString(path), "occupied target " + path); }
        }
        List<String> writeOrder = new ArrayList<>(emitted.keySet());
        writeOrder.add("m3/collections/keyed/recipe-receipt.tsv");
        for (String key : writeOrder) {
            var entry = Map.entry(key, all.get(key));
            Path path = root.resolve(entry.getKey()); Files.createDirectories(path.getParent());
            if (!Files.exists(path)) Files.writeString(path, entry.getValue(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE);
            assertEquals(entry.getValue(), Files.readString(path));
        }
    }
    private static Recipe named() { return Environment.builder().scanRuntimeClasspath("com.synexia.rewrite").build().activateRecipes("com.synexia.m3jdk.KeyedReceiver20261017"); }
    private static Map<String, String> expected() {
        Map<String, String> out = new TreeMap<>();
        for (String root : List.of(JAVA, TEXT)) for (String[] row : rows(root)) out.put(row[0], resource(root + row[3]));
        return out;
    }
    private static List<String[]> rows(String root) { return resource(root + "manifest.tsv").lines().filter(s -> !s.isBlank() && !s.startsWith("#")).map(s -> s.split("\t", -1)).toList(); }
    private static List<SourceFile> parse(Map<String, String> texts) {
        List<SourceFile> out = new ArrayList<>(); List<Parser.Input> java = new ArrayList<>();
        new TreeMap<>(texts).forEach((path, text) -> {
            if (path.endsWith(".java")) java.add(Parser.Input.fromString(Path.of(path), text));
            else out.add(PlainText.builder().sourcePath(Path.of(path)).text(text).build());
        });
        if (!java.isEmpty()) out.addAll(JavaParser.fromJavaVersion().build().parseInputs(java, null, context()).toList());
        return out;
    }
    private static InMemoryExecutionContext context() { return new InMemoryExecutionContext(error -> { throw new AssertionError(error); }); }
    private static String resource(String path) {
        try (var input = M3CollectionsKeyedTest.class.getResourceAsStream(path)) { assertNotNull(input, path); return new String(input.readAllBytes(), StandardCharsets.UTF_8); }
        catch (IOException error) { throw new IllegalStateException(error); }
    }
    private static String sha(String text) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
}
