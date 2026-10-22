// SPDX-License-Identifier: Apache-2.0
package com.synexia.recipe;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.Java21Parser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Javadoc;

class JavaLineTerminatorsTest {
    private static final String SNIPPET =
            "examples/org.eclipse.nebula.snippets/src/org/eclipse/nebula/snippets/gallery/SnippetTooltip.java";
    private static final String ORIGINAL_SHA256 =
            "836b4a2e35dc446fbe3dd7d0931d97fcfdce6467c8d5871d7c90ec10a501ee96";

    @Test void commentTerminatorsMatchJavaLexicalRules() {
        for (String newline : List.of("\n", "\r\n", "\r")) {
            String input = " \t// misleading ; }" + newline + " /* { ; */ int";
            assertEquals(input.indexOf("int"), StringUtils.indexOfNextNonWhitespace(0, input));
            assertFalse(StringUtils.containsOnlyWhitespaceAndComments("// comment" + newline + "int"));
            assertTrue(StringUtils.containsOnlyWhitespaceAndComments("// comment" + newline + " \t"));
        }
        assertEquals(7, StringUtils.indexOfNextNonWhitespace(0, "// tail"));
        assertEquals(0, StringUtils.indexOfNextNonWhitespace(0, ""));
        assertEquals(2, StringUtils.indexOfNextNonWhitespace(0, " \t"));
        // Unicode line separator is not a Java source line terminator.
        String unicode = "// comment\u2028still-comment";
        assertEquals(unicode.length(), StringUtils.indexOfNextNonWhitespace(0, unicode));
    }

    @Test void actualJava21ParserPreservesLfCrLfCrAndEofComments() {
        for (String newline : List.of("\n", "\r\n", "\r")) {
            String input = "class Contract { // misleading ; }" + newline
                    + " int negative() { // misleading ; }" + newline
                    + " return -7; // misleading ; }" + newline
                    + " } /* mixed block " + newline + " ; } */" + newline
                    + " String text() { return \"http://example/*literal*/\"; }" + newline
                    + " } // comment at EOF";
            assertPrintIdempotent(Path.of("Contract.java"), input);
        }
        assertPrintIdempotent(Path.of("End.java"), "class End { int f() { return -7; } }// tail");
    }

    @Test void originalSnippetRemainsAll4025BytesAndNoInputIsWritten() throws Exception {
        Path root = Path.of(System.getProperty("m3.nebula.root"));
        Path file = root.resolve(SNIPPET);
        byte[] before = Files.readAllBytes(file);
        assertEquals(4025, before.length);
        assertEquals(ORIGINAL_SHA256, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(before)));
        String source = new String(before, StandardCharsets.UTF_8);
        assertEquals(123, source.chars().filter(c -> c == '\r').count());
        assertEquals(0, source.chars().filter(c -> c == '\n').count());
        assertPrintIdempotent(Path.of(SNIPPET), source);
        assertArrayEquals(before, Files.readAllBytes(file));
    }

    @Test void isolatedVisitorUsesTheReviewedOverlayResource() throws Exception {
        var parser = Java21Parser.builder().build();
        var field = Java21Parser.class.getDeclaredField("delegate");
        field.setAccessible(true);
        Object delegate = field.get(parser);
        assertNotSame(Java21Parser.class.getClassLoader(), delegate.getClass().getClassLoader());
        var resource = Java21Parser.class.getClassLoader().getResource(
                "org/openrewrite/java/isolated/ReloadableJava21ParserVisitor.class");
        assertNotNull(resource);
        assertTrue(resource.toString().replace('\\', '/').contains("/target/classes/"), resource.toString());
        assertEquals("org.openrewrite.java.isolated.ReloadableJava21Parser",
                delegate.getClass().getName());
    }

    @Test void normalizedBlankLineWhitespaceRetainsSourceAndTypedJavadocs() {
        for (String newline : List.of("\n", "\r\n")) {
            for (String whitespace : List.of("", "     ", "\t\t", " \t ")) {
                String source = "class Doc {" + newline
                        + "/** title" + newline + whitespace + newline
                        + "    retained no-star body" + newline + " */" + newline
                        + "int f() { return -7; }" + newline + "}" + newline;
                var unit = assertPrintIdempotent(Path.of("Doc.java"), source);
                var method = unit.getClasses().getFirst().getBody().getStatements().stream()
                        .filter(J.MethodDeclaration.class::isInstance)
                        .map(J.MethodDeclaration.class::cast).findFirst().orElseThrow();
                assertTrue(method.getComments().stream().anyMatch(Javadoc.DocComment.class::isInstance));
            }
        }
    }

    @Test void originalRoundScaleRetainsAllBytesAndTypedJavadocBody() throws Exception {
        String relative = "widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/figureparts/RoundScale.java";
        for (Path file : List.of(Path.of(System.getProperty("m3.nebula.root")).resolve(relative),
                Path.of(getClass().getResource("/roundscale/Git.java.txt").toURI()),
                Path.of(getClass().getResource("/roundscale/Checkout.java.txt").toURI()))) {
        byte[] before = Files.readAllBytes(file);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(before));
        assertTrue((before.length == 9350 && hash.equals("09fa50c810f66c351000b350588d36716fd02d93657f626ce0fa1498f0a109a8"))
                || (before.length == 9687 && hash.equals("2b8574dbbde69addfe473d96a6767cdf635ac09b2ac8b8cfcba817b237ee4c32")),
                "RoundScale must match the exact Git LF or checkout CRLF source identity");
        var unit = assertPrintIdempotent(Path.of(relative), new String(before, StandardCharsets.UTF_8));
        var method = unit.getClasses().getFirst().getBody().getStatements().stream()
                .filter(J.MethodDeclaration.class::isInstance).map(J.MethodDeclaration.class::cast)
                .filter(m -> m.getSimpleName().equals("useLocalCoordinates")).findFirst().orElseThrow();
        assertTrue(method.getComments().stream().anyMatch(Javadoc.DocComment.class::isInstance));
        assertArrayEquals(before, Files.readAllBytes(file));
        }
    }

    private static J.CompilationUnit assertPrintIdempotent(Path path, String input) {
        List<Throwable> errors = new ArrayList<>();
        var context = new InMemoryExecutionContext(errors::add);
        var parser = Java21Parser.builder().build();
        List<SourceFile> files;
        try (var parsed = parser.parseInputs(List.of(Parser.Input.fromString(path, input)), null, context)) {
            files = parsed.toList();
        }
        assertTrue(errors.isEmpty(), () -> errors.toString());
        assertEquals(1, files.size());
        var unit = assertInstanceOf(J.CompilationUnit.class, files.getFirst());
        assertEquals(input, files.getFirst().printAll());
        return unit;
    }
}
