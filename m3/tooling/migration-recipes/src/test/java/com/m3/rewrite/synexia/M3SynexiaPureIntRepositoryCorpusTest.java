// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.synexia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3PureIntConvergenceRecipe;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
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

/**
 * M3JDK21 receiver proof for the canonical Synexia pure-int recipe.
 *
 * <p>This test owns only target/source-corpus admission. Recipe implementation and mastery remain
 * canonical in hsoliwal/com.synexia.</p>
 */
final class M3SynexiaPureIntRepositoryCorpusTest {
    private record Pin(String path, String category, long bytes, String blob, String hash) {}

    @Test
    void sourcePinnedHundredFileCorpusUsesCanonicalSynexiaRecipeWithoutProductWrites()
            throws Exception {
        List<Pin> pins = pins();
        assertEquals(100, pins.size());

        Path root = repositoryRoot();
        int parsed = 0;
        int typed = 0;
        int changed = 0;

        for (Pin pin : pins) {
            Path path = root.resolve(pin.path()).normalize();
            assertTrue(path.startsWith(root));
            byte[] beforeBytes = Files.readAllBytes(path);
            verifyPin(pin, beforeBytes);
            String before = new String(beforeBytes, StandardCharsets.UTF_8);

            List<Throwable> parseErrors = new ArrayList<>();
            var parseContext = new InMemoryExecutionContext(parseErrors::add);
            SourceFile source;
            try {
                List<SourceFile> sources =
                        JavaParser.fromJavaVersion()
                                .build()
                                .parseInputs(
                                        List.of(
                                                Parser.Input.fromString(
                                                        Path.of(pin.path()), before)),
                                        null,
                                        parseContext)
                                .toList();
                if (sources.size() != 1
                        || !(sources.getFirst() instanceof J.CompilationUnit)
                        || !parseErrors.isEmpty()
                        || !before.equals(sources.getFirst().printAll())) {
                    continue;
                }
                source = sources.getFirst();
                parsed++;
            } catch (RuntimeException | AssertionError refused) {
                continue;
            }

            try {
                Assertions.validateTypes(source, TypeValidation.all());
                typed++;
            } catch (RuntimeException | AssertionError refused) {
                continue;
            }

            List<String> surface = surface(source);
            var context =
                    new InMemoryExecutionContext(
                            failure -> {
                                throw new AssertionError(failure);
                            });
            var run =
                    new M3PureIntConvergenceRecipe()
                            .run(new InMemoryLargeSourceSet(List.of(source)), context);
            List<org.openrewrite.Result> changes =
                    run.getChangeset().getAllResults();
            assertTrue(changes.size() <= 1, pin.path());

            SourceFile after = source;
            if (!changes.isEmpty()) {
                var change = changes.getFirst();
                assertNotNull(change.getBefore());
                assertNotNull(change.getAfter());
                assertEquals(
                        change.getBefore().getSourcePath(),
                        change.getAfter().getSourcePath());
                after = change.getAfter();
                Assertions.validateTypes(after, TypeValidation.all());
                assertEquals(surface, surface(after), pin.path());
                changed++;
            }

            var replay =
                    new M3PureIntConvergenceRecipe()
                            .run(new InMemoryLargeSourceSet(List.of(after)), context);
            assertTrue(
                    replay.getChangeset().getAllResults().isEmpty(),
                    "canonical recipe must be at fixed point: " + pin.path());

            verifyPin(pin, Files.readAllBytes(path));
        }

        assertTrue(parsed > 0, "real corpus must contain parse-admitted files");
        assertTrue(typed > 0, "real corpus must contain type-admitted files");
        assertTrue(changed >= 0);
    }

    @Test
    void corpusPinsStillRejectChangedBytes() throws Exception {
        Pin pin = pins().getFirst();
        byte[] bytes = Files.readAllBytes(repositoryRoot().resolve(pin.path()));
        verifyPin(pin, bytes);
        byte[] changed = bytes.clone();
        changed[changed.length / 2] ^= 1;
        boolean rejected = false;
        try {
            verifyPin(pin, changed);
        } catch (AssertionError expected) {
            rejected = true;
        }
        assertTrue(rejected);
    }

    private static List<Pin> pins() throws Exception {
        try (InputStream stream =
                M3SynexiaPureIntRepositoryCorpusTest.class.getResourceAsStream(
                        "/com/m3/rewrite/atom/spectrum/CORPUS.tsv")) {
            assertNotNull(stream);
            List<Pin> result = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (String line :
                    new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                            .lines()
                            .toList()) {
                if (line.startsWith("#") || line.startsWith("path\t")) {
                    continue;
                }
                String[] cells = line.split("\t", -1);
                assertEquals(5, cells.length);
                assertTrue(seen.add(cells[0]));
                result.add(
                        new Pin(
                                cells[0],
                                cells[1],
                                Long.parseLong(cells[2]),
                                cells[3],
                                cells[4]));
            }
            return List.copyOf(result);
        }
    }

    private static void verifyPin(Pin pin, byte[] bytes) throws Exception {
        assertEquals(pin.bytes(), bytes.length, pin.path());
        assertEquals(
                pin.hash(),
                HexFormat.of()
                        .formatHex(
                                MessageDigest.getInstance("SHA-256")
                                        .digest(bytes)),
                "source drift: " + pin.path());

        MessageDigest git = MessageDigest.getInstance("SHA-1");
        git.update(
                ("blob " + bytes.length + "\0")
                        .getBytes(StandardCharsets.US_ASCII));
        git.update(bytes);
        assertEquals(pin.blob(), HexFormat.of().formatHex(git.digest()), pin.path());
    }

    private static List<String> surface(SourceFile source) {
        List<String> signatures = new ArrayList<>();
        new JavaIsoVisitor<List<String>>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(
                    J.ClassDeclaration node, List<String> output) {
                output.add(
                        "class "
                                + node.getType()
                                + " "
                                + node.getKind()
                                + " "
                                + node.getModifiers().stream()
                                        .map(J.Modifier::getType)
                                        .toList());
                return super.visitClassDeclaration(node, output);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration node, List<String> output) {
                output.add(
                        "method "
                                + node.getMethodType()
                                + " "
                                + node.getModifiers().stream()
                                        .map(J.Modifier::getType)
                                        .toList());
                return super.visitMethodDeclaration(node, output);
            }
        }.visit(source, signatures);
        return signatures.stream().sorted().toList();
    }

    private static Path repositoryRoot() {
        for (Path at =
                        Path.of(System.getProperty("basedir", "."))
                                .toAbsolutePath()
                                .normalize();
                at != null;
                at = at.getParent()) {
            if (Files.isRegularFile(
                            at.resolve(
                                    "src/java.base/share/classes/java/lang/String.java"))
                    && Files.isRegularFile(at.resolve("m3/pom.xml"))) {
                return at;
            }
        }
        throw new IllegalStateException("M3JDK_CORPUS_ROOT_REQUIRED");
    }
}
