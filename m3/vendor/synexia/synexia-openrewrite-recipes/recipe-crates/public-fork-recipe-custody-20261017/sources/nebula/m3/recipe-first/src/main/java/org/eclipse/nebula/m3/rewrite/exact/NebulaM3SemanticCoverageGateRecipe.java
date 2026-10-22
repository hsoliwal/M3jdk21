// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

/**
 * Exact XML-LST admission for the Nebula recipe DAG's 99% line and branch coverage gate.
 *
 * <p>Only the pinned Maven recipe-module POM may change. No widget source, Tycho build,
 * other Maven project, or broader scope is writable by this recipe. An exact postimage is a
 * fixed point. The recipe cannot waive JUnit, Java 21, native or product-level proof.</p>
 */
public final class NebulaM3SemanticCoverageGateRecipe extends Recipe {
    public static final String REPOSITORY_PATH = "m3/recipe-first/pom.xml";
    public static final String BEFORE_SHA256 =
            "46b3eb273ec86cebd01a2b6210b8366e586620bd10864a16dd1c6f775a27e9da";
    public static final String AFTER_SHA256 =
            "d2a189df6c1285f56299e74f509bbb0d6291bb0c822c53d2d6ce80f738a0632c";
    private static final String RESOURCE_ROOT =
            "/org/eclipse/nebula/m3/rewrite/exact/semantic-coverage-gate/";
    public static final String PREIMAGE = RESOURCE_ROOT + "pom.before.xml.txt";
    public static final String POSTIMAGE = RESOURCE_ROOT + "pom.after.xml.txt";

    @Override
    public String getDisplayName() {
        return "Require 99 percent line and branch coverage for Nebula M3 recipes";
    }

    @Override
    public String getDescription() {
        return "Add the 99 percent JaCoCo BRANCH proof gate to the existing semantic coverage "
                + "profile using one exact XML-LST, source-sealed Maven recipe-module change.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("nebula", "m3", "openrewrite", "maven", "jacoco",
                "atomization", "patternization", "iop", "file-local", "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String before = resource(PREIMAGE);
        String after = resource(POSTIMAGE);
        if (!BEFORE_SHA256.equals(sha256(before)) || !AFTER_SHA256.equals(sha256(after))) {
            throw new IllegalStateException("M3_SEMANTIC_COVERAGE_RESOURCE_DRIFT");
        }
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext context) {
                String path = normalized(document.getSourcePath());
                String current = document.printAll();
                String hash = sha256(current);

                boolean exactOwner = REPOSITORY_PATH.equals(path);
                boolean moduleRelativeOwner = "pom.xml".equals(path)
                        && (BEFORE_SHA256.equals(hash) || AFTER_SHA256.equals(hash));
                if (!exactOwner && !moduleRelativeOwner) {
                    return document;
                }
                if (document.isCharsetBomMarked()
                        || (document.getCharset() != null
                                && !StandardCharsets.UTF_8.equals(document.getCharset()))) {
                    throw new IllegalStateException("M3_SEMANTIC_COVERAGE_REQUIRES_UTF8_NO_BOM");
                }
                if (AFTER_SHA256.equals(hash)) {
                    return document;
                }
                if (!BEFORE_SHA256.equals(hash) || !before.equals(current)) {
                    throw new IllegalStateException("M3_SEMANTIC_COVERAGE_PREIMAGE_DRIFT:" + path);
                }
                Xml.Document parsed = parse(after, context);
                return parsed.withId(document.getId())
                        .withSourcePath(document.getSourcePath())
                        .withFileAttributes(document.getFileAttributes());
            }
        };
    }

    /** Neither source execution nor canonical promotion is owned by this recipe. */
    public boolean promotionAuthority() {
        return false;
    }

    public static String resource(String name) {
        try (InputStream input =
                NebulaM3SemanticCoverageGateRecipe.class.getResourceAsStream(name)) {
            if (input == null) {
                throw new IllegalStateException("M3_SEMANTIC_COVERAGE_RESOURCE_MISSING");
            }
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(input.readAllBytes()))
                    .toString();
        } catch (IOException failure) {
            throw new IllegalStateException("M3_SEMANTIC_COVERAGE_RESOURCE_READ", failure);
        }
    }

    private static Xml.Document parse(String content, ExecutionContext context) {
        List<SourceFile> parsed = XmlParser.builder().build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of("pom.xml"), content)),
                        null,
                        context)
                .toList();
        if (parsed.size() != 1
                || !(parsed.getFirst() instanceof Xml.Document xml)
                || !content.equals(xml.printAll())) {
            throw new IllegalStateException("M3_SEMANTIC_COVERAGE_XML_ROUNDTRIP");
        }
        return xml;
    }

    public static String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }
}
