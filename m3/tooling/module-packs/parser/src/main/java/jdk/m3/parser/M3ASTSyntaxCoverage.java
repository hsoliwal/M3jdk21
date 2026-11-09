// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Exact full-fidelity syntax leaf coverage of the original M3File source.
 * This is a source-text coverage check, NOT parser/source-generation attestation
 * or semantic equivalence. All covered tokens/trivia are read from the one source.
 */
public final class M3ASTSyntaxCoverage {
    private M3ASTSyntaxCoverage() { }

    public record Proof(int tokens, int trivia, int inserted, int coveredUtf16,
                        String utf16Sha256) { }

    /**
     * Refuse non-contiguous, overlapping, out-of-order, missing, unverified
     * or unclassified source leaves. Missing zero-width tokens count as insertions
     * but never cover source. A root with no children may cover only empty source.
     * maxNodes/units are independent hard limits checked before payload traversal.
     */
    public static Proof verify(M3ASTIndex ast, int maxNodes, int maxSourceUnits,
                               boolean allowRecovered) {
        Objects.requireNonNull(ast, "ast");
        if (maxNodes < 0 || maxSourceUnits < 0 || ast.nodeCount() > maxNodes
                || ast.source().length() > maxSourceUnits) {
            throw new IllegalArgumentException("syntax coverage budgets exceeded");
        }
        if (ast.domain() != M3ASTIndex.Domain.EXTERNAL) {
            throw new IllegalArgumentException("coverage requires tagged external syntax occurrences");
        }
        M3File text = ast.source();
        int expectedStart = 0, tokenCount = 0, triviaCount = 0, insertedCount = 0;
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        for (int node = 0; node < ast.nodeCount(); node++) {
            int tags = ast.portableFlags(node);
            if ((ast.flags(node) & M3ASTIndex.RECOVERED) != 0 && !allowRecovered) {
                throw new IllegalArgumentException("recovered syntax disallowed by strict coverage");
            }
            if (ast.childCount(node) > 0) {
                if ((tags & (M3ASTIndex.TOKEN | M3ASTIndex.TRIVIA | M3ASTIndex.MISSING)) != 0) {
                    throw new IllegalArgumentException("non-leaf declared syntax token");
                }
                continue;
            }
            if ((tags & M3ASTIndex.MISSING) != 0) {
                if (!allowRecovered) throw new IllegalArgumentException("missing token in strict syntax");
                if (!ast.hasExactRange(node) || ast.start(node) != ast.end(node)
                        || ast.start(node) != expectedStart) {
                    throw new IllegalArgumentException("missing token is not at current source boundary");
                }
                insertedCount++;
                continue;
            }
            if (!ast.hasExactRange(node) || (tags & (M3ASTIndex.TOKEN | M3ASTIndex.TRIVIA)) == 0
                    || (tags & (M3ASTIndex.TOKEN | M3ASTIndex.TRIVIA))
                            == (M3ASTIndex.TOKEN | M3ASTIndex.TRIVIA)) {
                if (ast.nodeCount() == 1 && text.length() == 0 && ast.hasExactRange(node)
                        && ast.start(node) == 0 && ast.end(node) == 0 && tags == 0) continue;
                throw new IllegalArgumentException("syntax leaf lacks exactly one token or trivia tag");
            }
            int start = ast.start(node), end = ast.end(node);
            if (start != expectedStart || end <= start) {
                throw new IllegalArgumentException("non-contiguous source tokens/trivia");
            }
            for (int i = start; i < end; i++) {
                char c = text.charAt(i);
                digest.update((byte) (c >>> 8));
                digest.update((byte) c);
            }
            expectedStart = end;
            if ((tags & M3ASTIndex.TOKEN) != 0) tokenCount++;
            else triviaCount++;
        }
        if (expectedStart != text.length()) {
            throw new IllegalArgumentException("source not completely covered by syntax leaves");
        }
        String hash = HexFormat.of().formatHex(digest.digest());
        if (!hash.equals(text.utf16Sha256())) {
            throw new IllegalStateException("syntax leaf stream differs from admitted M3File source");
        }
        return new Proof(tokenCount, triviaCount, insertedCount, expectedStart, hash);
    }
}
