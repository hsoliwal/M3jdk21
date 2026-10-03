// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/**
 * Exact JDK21-preimage to donor-postimage OpenRewrite replay for one Java file.
 *
 * <p>This recipe is deliberately stronger than a fuzzy patch: the target path and complete JDK21
 * preimage are hash pinned. Divergent source fails closed. The recipe does not claim semantic
 * equivalence; Java-21 compilation, JUnit contract proof and higher M3 gates remain authoritative.
 */
public final class M3VerbatimJavaPairRecipe extends Recipe {
    private final String targetPath;
    private final String baselineRef;
    private final String donorRef;
    private final String upstreamId;
    private final String beforeSha256;
    private final String afterSha256;
    private final String afterSource;

    public M3VerbatimJavaPairRecipe(
            String targetPath,
            String baselineRef,
            String donorRef,
            String upstreamId,
            String beforeSource,
            String afterSource) {
        this.targetPath = canonicalPath(targetPath);
        this.baselineRef = descriptor(baselineRef, "baselineRef");
        this.donorRef = descriptor(donorRef, "donorRef");
        this.upstreamId = descriptor(upstreamId, "upstreamId");
        String before = Objects.requireNonNull(beforeSource, "beforeSource");
        this.afterSource = Objects.requireNonNull(afterSource, "afterSource");
        this.beforeSha256 = sha256(before);
        this.afterSha256 = sha256(afterSource);
        if (beforeSha256.equals(afterSha256)) {
            throw new IllegalArgumentException("M3_VERBATIM_NO_DELTA");
        }
    }

    public String targetPath() {
        return targetPath;
    }

    public String baselineRef() {
        return baselineRef;
    }

    public String donorRef() {
        return donorRef;
    }

    public String upstreamId() {
        return upstreamId;
    }

    public String beforeSha256() {
        return beforeSha256;
    }

    public String afterSha256() {
        return afterSha256;
    }

    @Override
    public String getDisplayName() {
        return "M3 replay exact JDK21 Java donor pair";
    }

    @Override
    public String getDescription() {
        return "Replays one exact hash-pinned JDK21 Java preimage to a reviewed donor postimage.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "openrewrite",
                "verbatim-preimage",
                "file-local",
                "fail-closed");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(
                    J.CompilationUnit cu, ExecutionContext context) {
                J.CompilationUnit candidate = super.visitCompilationUnit(cu, context);
                String path = candidate.getSourcePath().toString().replace('\\', '/');
                if (!targetPath.equals(path)) {
                    return candidate;
                }

                String current = sha256(candidate.printAll());
                if (afterSha256.equals(current)) {
                    return candidate;
                }
                if (!beforeSha256.equals(current)) {
                    throw new IllegalStateException(
                            "M3_VERBATIM_PREIMAGE_DRIFT:" + targetPath);
                }

                List<SourceFile> parsed =
                        JavaParser.fromJavaVersion()
                                .build()
                                .parse(context, afterSource)
                                .toList();
                if (parsed.size() != 1
                        || !(parsed.getFirst() instanceof J.CompilationUnit replacement)) {
                    throw new IllegalStateException(
                            "M3_VERBATIM_DONOR_NOT_JAVA:" + targetPath);
                }
                if (!afterSha256.equals(sha256(replacement.printAll()))) {
                    throw new IllegalStateException(
                            "M3_VERBATIM_DONOR_ROUNDTRIP_DRIFT:" + targetPath);
                }

                return replacement
                        .withId(candidate.getId())
                        .withSourcePath(candidate.getSourcePath())
                        .withMarkers(candidate.getMarkers());
            }
        };
    }

    static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String canonicalPath(String value) {
        String path = Objects.requireNonNull(value, "targetPath").replace('\\', '/');
        if (path.isBlank()
                || path.startsWith("/")
                || path.matches("^[A-Za-z]:/.*")
                || path.contains("/../")
                || path.endsWith("/..")
                || path.contains("/./")
                || path.contains("//")
                || !path.endsWith(".java")) {
            throw new IllegalArgumentException("M3_VERBATIM_TARGET_PATH:" + value);
        }
        return path.startsWith("./") ? path.substring(2) : path;
    }

    private static String descriptor(String value, String name) {
        String descriptor = Objects.requireNonNull(value, name);
        if (!descriptor.matches("[A-Za-z0-9_.:/+@-]{1,200}")) {
            throw new IllegalArgumentException("M3_VERBATIM_" + name.toUpperCase());
        }
        return descriptor;
    }
}
