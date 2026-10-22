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
import java.util.Objects;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;

/**
 * Exact PlainText candidate replay for one reviewed Nebula FILE preimage.
 *
 * <p>This is the non-Java companion to {@link NebulaM3ExactJavaSnapshotRecipe}. The recipe never
 * edits Java LSTs and owns no filesystem writes. A matching PlainText source must equal either the
 * reviewed preimage or the reviewed postimage. The postimage is source-sealed and idempotent.</p>
 */
public abstract class NebulaM3ExactTextSnapshotRecipe extends Recipe {
    protected abstract String repositoryPath();

    protected abstract String moduleRelativePath();

    protected abstract String beforeSha256();

    protected abstract String afterSha256();

    protected abstract String afterResource();

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String after = resource(afterResource());
        if (!afterSha256().equals(sha256(after))) {
            throw new IllegalStateException(
                    "M3 exact text postimage resource drift: " + repositoryPath());
        }

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof PlainText text)
                        || !matches(text.getSourcePath())) {
                    return tree;
                }
                stopAfterPreVisit();

                String current = text.getText();
                String hash = sha256(current);
                if (afterSha256().equals(hash)) {
                    return text;
                }
                if (!beforeSha256().equals(hash)) {
                    context.getOnError().accept(
                            new IllegalStateException(
                                    "M3 exact text preimage drift: "
                                            + normalized(text.getSourcePath())));
                    return text;
                }

                SourceFile replacement =
                        PlainText.builder()
                                .sourcePath(text.getSourcePath())
                                .text(after)
                                .build();
                replacement = (SourceFile) replacement.withId(text.getId());
                replacement = (SourceFile) replacement.withMarkers(text.getMarkers());
                replacement = replacement.withFileAttributes(text.getFileAttributes());
                replacement = replacement.withCharset(text.getCharset());
                replacement = replacement.withCharsetBomMarked(text.isCharsetBomMarked());
                return replacement.withChecksum(null);
            }
        };
    }

    public final boolean matches(Path sourcePath) {
        String path = normalized(Objects.requireNonNull(sourcePath, "sourcePath"));
        return path.equals(repositoryPath()) || path.equals(moduleRelativePath());
    }

    public final String expectedBeforeSha256() {
        return beforeSha256();
    }

    public final String expectedAfterSha256() {
        return afterSha256();
    }

    protected static String resource(String name) {
        try (InputStream input =
                NebulaM3ExactTextSnapshotRecipe.class.getResourceAsStream(name)) {
            if (input == null) {
                throw new IllegalStateException("missing exact text resource: " + name);
            }
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(input.readAllBytes()))
                    .toString();
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read exact text resource", failure);
        }
    }

    public static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            Objects.requireNonNull(value, "value")
                                                    .getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }
}
