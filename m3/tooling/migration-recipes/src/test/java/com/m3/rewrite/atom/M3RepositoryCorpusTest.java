// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.Assertions;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.TypeValidation;

/** Source-sealed real-code admission survey; it does not claim JDK compilation or runtime proof. */
final class M3RepositoryCorpusTest {
    private record Pin(String path, String category, long bytes, String blob, String hash) { }

    @Test
    void sourcePinnedHundredFileCorpusRecordsAdmissionAndRefusalWithoutProductWrites() throws Exception {
        List<Pin> pins = pins();
        assertEquals(100, pins.size());
        Map<String, Long> categories = new TreeMap<>();
        for (Pin pin : pins) categories.merge(pin.category(), 1L, Long::sum);
        assertEquals(Map.of("java.base", 50L, "jdk.compiler", 25L, "m3", 25L), categories);
        Path root = root();
        StringBuilder report = new StringBuilder(
                "path\tcategory\tbefore_sha256\tstatus\tcandidates\tafter_sha256\tdetail\n");
        Map<String, Integer> counts = new TreeMap<>();
        int admitted = 0;
        for (Pin pin : pins) {
            Path path = root.resolve(pin.path()).normalize();
            assertTrue(path.startsWith(root));
            byte[] bytes = Files.readAllBytes(path);
            check(pin, bytes);
            String original = new String(bytes, StandardCharsets.UTF_8);
            List<Throwable> parseErrors = new ArrayList<>();
            var parseContext = new InMemoryExecutionContext(parseErrors::add);
            SourceFile parsed;
            try {
                List<SourceFile> sources;
                try (var stream = JavaParser.fromJavaVersion().build().parseInputs(
                        List.of(Parser.Input.fromString(Path.of(pin.path()), original)), null, parseContext)) {
                    sources = stream.toList();
                }
                if (sources.size() != 1 || !(sources.getFirst() instanceof J.CompilationUnit)
                        || !parseErrors.isEmpty() || !original.equals(sources.getFirst().printAll())) {
                    row(report, counts, pin, "PARSE_REFUSED", 0, pin.hash(),
                            parseErrors.isEmpty() ? "non-LST or format drift" : parseErrors.getFirst().toString());
                    continue;
                }
                parsed = sources.getFirst();
            } catch (RuntimeException | AssertionError failure) {
                row(report, counts, pin, "PARSE_REFUSED", 0, pin.hash(), failure.toString());
                continue;
            }
            try {
                Assertions.validateTypes(parsed, TypeValidation.all());
            } catch (RuntimeException | AssertionError failure) {
                row(report, counts, pin, "TYPE_REFUSED", 0, pin.hash(), failure.toString());
                continue;
            }
            admitted++;
            var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
            int[] candidateCount = {0};
            new JavaIsoVisitor<int[]>() {
                @Override public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, int[] count) {
                    J.MethodDeclaration result = super.visitMethodDeclaration(method, count);
                    if (M3PureIntAtomEligibility.eligible(result)) count[0]++;
                    return result;
                }
            }.visit(parsed, candidateCount);
            var inventory = new M3InventoryPureIntAtomCandidates()
                    .run(new InMemoryLargeSourceSet(List.of(parsed)), context, 1);
            assertTrue(inventory.getChangeset().getAllResults().isEmpty(), pin.path());
            List<String> surface = surface(parsed);
            var run = new M3PureIntConvergenceRecipe()
                    .run(new InMemoryLargeSourceSet(List.of(parsed)), context, 3);
            SourceFile after = parsed;
            var changes = run.getChangeset().getAllResults();
            assertTrue(changes.size() <= 1, pin.path());
            if (!changes.isEmpty()) {
                var change = changes.getFirst();
                assertNotNull(change.getBefore()); assertNotNull(change.getAfter());
                assertEquals(change.getBefore().getSourcePath(), change.getAfter().getSourcePath());
                after = change.getAfter();
                Assertions.validateTypes(after, TypeValidation.all());
                assertEquals(surface, surface(after), pin.path());
            }
            var replay = new M3PureIntConvergenceRecipe()
                    .run(new InMemoryLargeSourceSet(List.of(after)), context, 3);
            assertTrue(replay.getChangeset().getAllResults().isEmpty(), pin.path());
            String status = changes.isEmpty() ? "NOOP_FIXED_POINT" : "CANDIDATE_FIXED_POINT";
            row(report, counts, pin, status, candidateCount[0], hash(after.printAll().getBytes(StandardCharsets.UTF_8)),
                    "source-only; product compile/runtime not executed");
            check(pin, Files.readAllBytes(path));
        }
        Path out = Path.of("target/m3-spectrum");
        Files.createDirectories(out);
        Files.writeString(out.resolve("CORPUS.tsv"), report, StandardCharsets.UTF_8);
        StringBuilder summary = new StringBuilder("status\tfiles\n");
        counts.forEach((status, count) -> summary.append(status).append('\t').append(count).append('\n'));
        Files.writeString(out.resolve("CORPUS_COUNTS.tsv"), summary, StandardCharsets.UTF_8);
        assertEquals(100, counts.values().stream().mapToInt(Integer::intValue).sum());
        assertTrue(admitted > 0, "a completely refused real corpus is not a useful survey");
    }

    @Test
    void changedSourceCannotBorrowAnOldPin() throws Exception {
        Pin pin = pins().getFirst();
        byte[] original = Files.readAllBytes(root().resolve(pin.path()));
        check(pin, original);
        byte[] mutant = original.clone(); mutant[mutant.length / 2] ^= 1;
        assertThrows(AssertionError.class, () -> check(pin, mutant));
    }

    @Test
    void completePinnedM3GroupCompilesAndRunsItsRealPrefixAlgorithm() throws Exception {
        List<Pin> group = pins().stream().filter(pin -> pin.category().equals("m3")).toList();
        assertEquals(25, group.size());
        Map<String, String> sources = new TreeMap<>();
        for (Pin pin : group) {
            byte[] bytes = Files.readAllBytes(root().resolve(pin.path()));
            check(pin, bytes);
            sources.put(pin.path(), new String(bytes, StandardCharsets.UTF_8));
        }
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<Parser.Input> inputs = sources.entrySet().stream()
                .map(entry -> Parser.Input.fromString(Path.of(entry.getKey()), entry.getValue())).toList();
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build().parseInputs(inputs, null, context).toList();
        assertEquals(25, parsed.size());
        for (SourceFile source : parsed) {
            assertTrue(source instanceof J.CompilationUnit);
            assertEquals(sources.get(source.getSourcePath().toString()), source.printAll());
            Assertions.validateTypes(source, TypeValidation.all());
        }
        ClassLoader real = MemJava.compile(sources);
        Class<?> arenaType = real.loadClass("com.m3.text.LocalM3Arena");
        Object arena = arenaType.getConstructor().newInstance();
        var copy = arenaType.getMethod("copyUtf16", char[].class);
        Class<?> pieceType = real.loadClass("com.m3.text.M3StringPiece");
        Class<?> prefixType = real.loadClass("com.m3.algorithm.M3PrefixZ");
        var analyze = prefixType.getMethod("analyze", pieceType);
        var length = prefixType.getMethod("length");
        var prefixAt = prefixType.getMethod("prefixLengthAt", int.class);
        var sum = prefixType.getMethod("similaritySum");
        var border = prefixType.getMethod("longestProperBorderLength");
        List<char[]> texts = new ArrayList<>();
        for (int size = 0; size <= 8; size++) for (int bits = 0; bits < (1 << size); bits++) {
            char[] text = new char[size];
            for (int i = 0; i < size; i++) text[i] = (char) ('a' + ((bits >>> i) & 1));
            texts.add(text);
        }
        for (String text : List.of("\0a\0", "\ud800x\ud800", "a\udc00a", "\ud83d\ude00\ud83d\ude00")) {
            texts.add(text.toCharArray());
        }
        assertEquals(515, texts.size());
        for (char[] text : texts) {
            Object piece = copy.invoke(arena, (Object) text);
            Object result = analyze.invoke(null, piece);
            assertEquals(text.length, length.invoke(result));
            long total = 0; int properBorder = 0;
            for (int offset = 0; offset < text.length; offset++) {
                int count = 0;
                while (offset + count < text.length && text[count] == text[offset + count]) count++;
                assertEquals(count, prefixAt.invoke(result, offset));
                total += count;
                if (offset > 0 && offset + count == text.length) properBorder = Math.max(properBorder, count);
            }
            assertEquals(total, sum.invoke(result));
            assertEquals(properBorder, border.invoke(result));
        }
        for (Pin pin : group) check(pin, Files.readAllBytes(root().resolve(pin.path())));
        Path out = Path.of("target/m3-spectrum"); Files.createDirectories(out);
        Files.writeString(out.resolve("M3_GROUP.tsv"),
                "source_units\tstrict_type_admitted\tcompiled\tutf16_cases\tproof\n"
                + "25\t25\t25\t515\toriginal_M3_sidecar_source_and_PrefixZ_runtime\n", StandardCharsets.UTF_8);
    }

    private static List<Pin> pins() throws Exception {
        String text;
        try (InputStream stream = M3RepositoryCorpusTest.class.getResourceAsStream(
                "/com/m3/rewrite/atom/spectrum/CORPUS.tsv")) {
            assertNotNull(stream); text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<Pin> result = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (String line : text.lines().toList()) {
            if (line.startsWith("#") || line.startsWith("path\t")) continue;
            String[] cells = line.split("\t", -1);
            assertEquals(5, cells.length);
            assertTrue(cells[0].matches("[A-Za-z0-9_./$-]+\\.java") && !cells[0].contains(".."));
            assertTrue(seen.add(cells[0]));
            assertTrue(cells[3].matches("[0-9a-f]{40}"));
            assertTrue(cells[4].matches("[0-9a-f]{64}"));
            result.add(new Pin(cells[0], cells[1], Long.parseLong(cells[2]), cells[3], cells[4]));
        }
        return List.copyOf(result);
    }

    private static Path root() {
        String configured = System.getProperty("m3.spectrum.corpusRoot");
        if (configured != null) return Path.of(configured).toAbsolutePath().normalize();
        for (Path at = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize();
                at != null; at = at.getParent()) {
            if (Files.isRegularFile(at.resolve("src/java.base/share/classes/java/lang/String.java"))
                    && Files.isRegularFile(at.resolve("m3/pom.xml"))) return at;
        }
        throw new IllegalStateException("M3JDK_CORPUS_ROOT_REQUIRED");
    }

    private static void check(Pin pin, byte[] bytes) throws Exception {
        assertEquals(pin.bytes(), bytes.length, pin.path());
        assertEquals(pin.hash(), hash(bytes), "source drift: " + pin.path());
        MessageDigest git = MessageDigest.getInstance("SHA-1");
        git.update(("blob " + bytes.length + "\0").getBytes(StandardCharsets.US_ASCII));
        assertEquals(pin.blob(), HexFormat.of().formatHex(git.digest(bytes)), pin.path());
    }

    private static List<String> surface(SourceFile source) {
        List<String> signatures = new ArrayList<>();
        new JavaIsoVisitor<List<String>>() {
            @Override public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration node, List<String> output) {
                output.add("class " + node.getType() + " " + node.getKind() + " "
                        + node.getModifiers().stream().map(J.Modifier::getType).toList());
                return super.visitClassDeclaration(node, output);
            }
            @Override public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration node, List<String> output) {
                output.add("method " + node.getMethodType() + " "
                        + node.getModifiers().stream().map(J.Modifier::getType).toList());
                return super.visitMethodDeclaration(node, output);
            }
        }.visit(source, signatures);
        return signatures.stream().sorted().toList();
    }

    private static void row(StringBuilder out, Map<String, Integer> counts, Pin pin, String status,
            int candidates, String after, String detail) {
        counts.merge(status, 1, Integer::sum);
        String clean = detail.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
        if (clean.length() > 800) clean = clean.substring(0, 800);
        out.append(pin.path()).append('\t').append(pin.category()).append('\t').append(pin.hash())
                .append('\t').append(status).append('\t').append(candidates).append('\t').append(after)
                .append('\t').append(clean).append('\n');
    }

    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
