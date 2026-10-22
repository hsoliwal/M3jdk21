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
 * Exact XML-LST Maven profile atom for the backport DAG integrity recipe.
 *
 * <p>Accepts only the reviewed POM before/after hashes. A root POM is never rewritten merely
 * because its filename is {@code pom.xml}. No lifecycle-bound rewrite or transfer is authorized.</p>
 */
public final class NebulaM3BackportDagMavenProfileRecipe extends Recipe {
    public static final String REPOSITORY_PATH = "m3/recipe-first/pom.xml";
    public static final String BEFORE_SHA256 =
            "0b418ca099d48decd5d521929c48b48899679879d206325e12d99dca1bd1b99f";
    public static final String AFTER_SHA256 =
            "46b3eb273ec86cebd01a2b6210b8366e586620bd10864a16dd1c6f775a27e9da";
    public static final String POSTIMAGE =
            "/org/eclipse/nebula/m3/rewrite/exact/backport-dag-integrity/pom.after.xml.txt";

    @Override
    public String getDisplayName() {
        return "Wire exact Nebula backport DAG recipe profile";
    }

    @Override
    public String getDescription() {
        return "Replay the reviewed opt-in Maven/OpenRewrite profile and XML parser dependency "
                + "only from the exact pinned Nebula recipe module POM preimage.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("m3", "nebula", "backport", "maven", "xml", "recipe-first",
                "file-local", "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String postimage = resource();
        if (!AFTER_SHA256.equals(sha256(postimage))) {
            throw new IllegalStateException("backport Maven profile resource drift");
        }
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext context) {
                String path = normalized(document.getSourcePath());
                String current = document.printAll();
                String hash = sha256(current);
                boolean exactOwner = REPOSITORY_PATH.equals(path);
                boolean modulePom = "pom.xml".equals(path)
                        && (BEFORE_SHA256.equals(hash) || AFTER_SHA256.equals(hash));
                if (!exactOwner && !modulePom) {
                    return document;
                }
                if (document.isCharsetBomMarked()
                        || (document.getCharset() != null
                                && !StandardCharsets.UTF_8.equals(document.getCharset()))) {
                    throw new IllegalStateException("backport Maven profile requires UTF-8 no BOM");
                }
                if (AFTER_SHA256.equals(hash)) {
                    return document;
                }
                if (!BEFORE_SHA256.equals(hash)) {
                    context.getOnError().accept(
                            new IllegalStateException("backport Maven profile preimage drift"));
                    return document;
                }
                Xml.Document parsed = parse(postimage, context);
                return parsed.withId(document.getId())
                        .withSourcePath(document.getSourcePath())
                        .withFileAttributes(document.getFileAttributes());
            }
        };
    }

    private static Xml.Document parse(String content, ExecutionContext context) {
        List<SourceFile> files = XmlParser.builder().build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of("pom.xml"), content)),
                        null,
                        context)
                .toList();
        if (files.size() != 1
                || !(files.getFirst() instanceof Xml.Document parsed)
                || !content.equals(parsed.printAll())) {
            throw new IllegalStateException("backport Maven XML parse/round-trip drift");
        }
        return parsed;
    }

    private static String resource() {
        try (InputStream input =
                NebulaM3BackportDagMavenProfileRecipe.class.getResourceAsStream(POSTIMAGE)) {
            if (input == null) {
                throw new IllegalStateException("missing backport Maven profile resource");
            }
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(input.readAllBytes()))
                    .toString();
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read backport Maven profile resource", failure);
        }
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
