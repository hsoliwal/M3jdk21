// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast.partial;

import com.synexia.mindex.ast.MIndexASTLimits;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Exact immutable cache identity for one partial-AST request.
 *
 * <p>Source bytes, language/profile, coordinate range, scope, evidence mode and AST limits are all
 * part of identity. Syntax-only requests deliberately canonicalize the semantic environment to the
 * empty string so unrelated classpath changes do not invalidate purely syntactic projections.</p>
 */
public record MIndexPartialAstKey(
        String sourceSha256,
        String languageProfileSha256,
        String environmentSha256,
        long sourceRevision,
        long startByte,
        long endByte,
        int startUtf16,
        int endUtf16,
        MIndexPartialAstScope scope,
        MIndexPartialAstMode mode,
        MIndexASTLimits limits) {

    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    public MIndexPartialAstKey {
        sourceSha256 = sha(sourceSha256, "sourceSha256");
        languageProfileSha256 = sha(languageProfileSha256, "languageProfileSha256");
        scope = Objects.requireNonNull(scope, "scope");
        mode = Objects.requireNonNull(mode, "mode");
        limits = Objects.requireNonNull(limits, "limits");
        if (sourceRevision < 0) throw new IllegalArgumentException("negative source revision");
        if (startByte < 0 || endByte < startByte) {
            throw new IllegalArgumentException("invalid byte range");
        }
        if (startUtf16 < 0 || endUtf16 < startUtf16) {
            throw new IllegalArgumentException("invalid UTF-16 range");
        }
        if (mode == MIndexPartialAstMode.SYNTAX) {
            environmentSha256 = "";
        } else {
            environmentSha256 = sha(environmentSha256, "environmentSha256");
        }
    }

    public MIndexPartialAstKey withScope(MIndexPartialAstScope next) {
        return new MIndexPartialAstKey(
                sourceSha256, languageProfileSha256, environmentSha256, sourceRevision,
                startByte, endByte, startUtf16, endUtf16,
                Objects.requireNonNull(next, "next"), mode, limits);
    }

    public MIndexPartialAstKey withMode(MIndexPartialAstMode next, String environment) {
        return new MIndexPartialAstKey(
                sourceSha256, languageProfileSha256, environment, sourceRevision,
                startByte, endByte, startUtf16, endUtf16,
                scope, Objects.requireNonNull(next, "next"), limits);
    }

    public MIndexPartialAstKey withSourceSha256(String next) {
        return new MIndexPartialAstKey(
                next, languageProfileSha256, environmentSha256, sourceRevision,
                startByte, endByte, startUtf16, endUtf16, scope, mode, limits);
    }

    private static String sha(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).strip().toLowerCase(Locale.ROOT);
        if (!SHA256.matcher(normalized).matches()) {
            throw new IllegalArgumentException(name + " must be 64 lowercase hexadecimal digits");
        }
        return normalized;
    }
}
