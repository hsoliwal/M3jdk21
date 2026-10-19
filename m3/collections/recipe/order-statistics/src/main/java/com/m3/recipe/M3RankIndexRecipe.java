// SPDX-License-Identifier: Apache-2.0
package com.m3.recipe;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/**
 * Hash-sealed Maven-bound source-generation recipe for the M3 rank/select lane.
 *
 * <p>Templates are the only editable source authority. The product outputs are generated
 * byte-for-byte, and an incorrect output must be fixed in the template and regenerated,
 * never hand-edited. Existing differing files fail closed (no destructive replacement).
 * OpenRewrite remains the owner for transformations of existing Java ASTs; this recipe
 * installs new Java/C files and therefore requires no AST rewriter.</p>
 */
public final class M3RankIndexRecipe {
    private record Install(String resource, String target, String sha256) { }
    private static final List<Install> INPUTS = List.of(
            new Install("M3LongRankIndex.java",
                    "m3/collections/src/main/java/com/m3/collections/M3LongRankIndex.java",
                    "d1011f31158fa96c23722e6df2fd7ec9e9c1592fba860580330c42fb3f25f4f9"),
            new Install("M3LongRankBatchNative.java",
                    "m3/collections/src/main/java/com/m3/collections/M3LongRankBatchNative.java",
                    "a199d9601ca4c36d8dbe3d1b95a8dd7da49d5f22a15ed29a7112665a38288c82"),
            new Install("m3_long_rank_batch_jni.c",
                    "m3/collections/src/main/native/collections/m3_long_rank_batch_jni.c",
                    "50c741073d6ef0b4efbd4a90c1f336f42945fa3bcd5824455f213f7509a52388"),
            new Install("M3LongRankIndexSelfTest.java",
                    "m3/collections/src/test/java/com/m3/collections/M3LongRankIndexSelfTest.java",
                    "63fd0c55d60419a6ca3669bc5f877357566f06072cfdad553e9824767836e3d1"),
            new Install("M3LongRankIndexTest.java",
                    "m3/collections/src/test/java/com/m3/collections/M3LongRankIndexTest.java",
                    "b204714a64abb5a6e73c23cf91c1399096c7a2246a714d7bc783568ede131b39"));

    private M3RankIndexRecipe() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !("--apply".equals(args[0]) || "--check".equals(args[0]))) {
            throw new IllegalArgumentException("Usage: M3RankIndexRecipe --check|--apply <repo-root>");
        }
        run(Path.of(args[1]), "--apply".equals(args[0]));
    }

    public static void run(Path repoRoot, boolean apply) throws IOException {
        Path root = repoRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root.resolve("m3/collections"))) {
            throw new IOException("M3_COLLECTIONS_REPO_ROOT_NOT_FOUND");
        }
        int matched = 0;
        for (Install item : INPUTS) {
            byte[] expected = template(item.resource());
            if (!sha256(expected).equals(item.sha256())) {
                throw new IOException("TEMPLATE_SHA_MISMATCH:" + item.resource());
            }
            Path destination = root.resolve(item.target()).normalize();
            if (!destination.startsWith(root)) {
                throw new IOException("TARGET_ESCAPES_REPO");
            }
            if (Files.exists(destination)) {
                if (Files.isSymbolicLink(destination)
                        || !Files.isRegularFile(destination)
                        || !Arrays.equals(Files.readAllBytes(destination), expected)) {
                    throw new IOException("PRODUCT_POSTIMAGE_DIFFERS:" + item.target());
                }
            } else if (apply) {
                Files.createDirectories(destination.getParent());
                Path temp = Files.createTempFile(destination.getParent(), ".m3-rank-", ".tmp");
                try {
                    Files.write(temp, expected);
                    Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    Files.deleteIfExists(temp);
                }
            } else {
                throw new IOException("PRODUCT_POSTIMAGE_MISSING:" + item.target());
            }
            matched++;
        }
        System.out.println("M3 rank/select recipe " + (apply ? "applied" : "checked")
                + "; sealed paths=" + matched);
    }

    private static byte[] template(String name) throws IOException {
        try (InputStream in = M3RankIndexRecipe.class.getResourceAsStream("/templates/" + name)) {
            if (in == null) throw new IOException("TEMPLATE_NOT_FOUND:" + name);
            return in.readAllBytes();
        }
    }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA_256_UNAVAILABLE", exception);
        }
    }
}
