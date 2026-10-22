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
import java.util.Objects;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/**
 * Exact structured-Java candidate replay for one reviewed Nebula FILE preimage.
 *
 * <p>The recipe owns no filesystem writes and never edits plain text. A matching Java LST must
 * equal either the reviewed preimage or the reviewed postimage. The postimage is reparsed as Java
 * before replacement and the exact postimage is a fixed point.</p>
 */
public abstract class NebulaM3ExactJavaSnapshotRecipe extends Recipe {
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
            throw new IllegalStateException("M3 exact Java postimage resource drift: " + repositoryPath());
        }

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof J.CompilationUnit unit)
                        || !matches(unit.getSourcePath())) {
                    return tree;
                }
                stopAfterPreVisit();

                String current = unit.printAll();
                String hash = sha256(current);
                if (afterSha256().equals(hash)) {
                    return unit;
                }
                if (!beforeSha256().equals(hash)) {
                    context.getOnError().accept(new IllegalStateException(
                            "M3 exact Java preimage drift: " + normalized(unit.getSourcePath())));
                    return unit;
                }

                SourceFile parsed = parse(after, context);
                SourceFile replacement = parsed.withId(unit.getId());
                replacement = replacement.withSourcePath(unit.getSourcePath());
                replacement = replacement.withMarkers(unit.getMarkers());
                replacement = replacement.withFileAttributes(unit.getFileAttributes());
                replacement = replacement.withCharset(unit.getCharset());
                replacement = replacement.withCharsetBomMarked(unit.isCharsetBomMarked());
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

    private SourceFile parse(String body, ExecutionContext context) {
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(moduleRelativePath()), body)),
                                null,
                                context)
                        .toList();
        if (parsed.size() != 1
                || !(parsed.getFirst() instanceof J.CompilationUnit)
                || !body.equals(parsed.getFirst().printAll())) {
            throw new IllegalStateException(
                    "M3 exact Java parse/format drift: " + repositoryPath());
        }
        return parsed.getFirst();
    }

    private static String resource(String name) {
        try (InputStream input =
                NebulaM3ExactJavaSnapshotRecipe.class.getResourceAsStream(name)) {
            if (input == null) {
                throw new IllegalStateException("missing exact Java resource: " + name);
            }
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(input.readAllBytes()))
                    .toString();
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read exact Java resource", failure);
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
