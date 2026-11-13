/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
// Modified 2026-10-18: reject blocked parent directories before creating any output.
package jdk.m3.recipe;

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
                    "m3/collections/src/main/java/jdk/m3/collections/M3LongRankIndex.java",
                    "eb083c80456c7b0bb2c3ad89df21631b4bc329178c905491bd9841c15588d265"),
            new Install("M3LongRankBatchNative.java",
                    "m3/collections/src/main/java/jdk/m3/collections/M3LongRankBatchNative.java",
                    "a071aca93b32a37ac4f6efbe3f2c008732d7af05278e34d1480c117d5c635940"),
            new Install("m3_long_rank_batch_jni.c",
                    "m3/collections/src/main/native/collections/m3_long_rank_batch_jni.c",
                    "75fca9fea6741cb1b13e1b96fcc34d560a5e3bba3ee59b3b485d1ca69b72aee0"),
            new Install("M3LongRankIndexSelfTest.java",
                    "m3/collections/src/test/java/jdk/m3/collections/M3LongRankIndexSelfTest.java",
                    "a3d9afad57dc08fb2d994ab00e909070ace0ef3e4f7479139844c4fe8594cf47"),
            new Install("M3LongRankIndexTest.java",
                    "m3/collections/src/test/java/jdk/m3/collections/M3LongRankIndexTest.java",
                    "11aef7f8999e8d5b5477ef72e8fc19040057f7376f90baa7ca2c881563de7139"));

    private M3RankIndexRecipe() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !("--apply".equals(args[0]) || "--check".equals(args[0]))) {
            throw new IllegalArgumentException("Usage: M3RankIndexRecipe --check|--apply <repo-root>");
        }
        run(Path.of(args[1]), "--apply".equals(args[0]));
    }

    public static void run(Path repoRoot, boolean apply) throws IOException {
        Path root = repoRoot.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root.resolve("m3/collections"))) {
            throw new IOException("M3_COLLECTIONS_REPO_ROOT_NOT_FOUND");
        }

        // Prepare and validate every target before any destination is written.
        // The original installer could create earlier files before a later drift refused.
        byte[][] verified = new byte[INPUTS.size()][];
        for (int index = 0; index < INPUTS.size(); index++) {
            Install item = INPUTS.get(index);
            byte[] expected = template(item.resource());
            if (!sha256(expected).equals(item.sha256())) {
                throw new IOException("TEMPLATE_SHA_MISMATCH:" + item.resource());
            }
            Path destination = root.resolve(item.target()).normalize();
            if (!destination.startsWith(root)) {
                throw new IOException("TARGET_ESCAPES_REPO");
            }
            for (Path part = destination; !part.equals(root); part = part.getParent()) {
                if (Files.isSymbolicLink(part)) {
                    throw new IOException("SYMLINK_PATH:" + item.target());
                }
                if (!part.equals(destination) && Files.exists(part) && !Files.isDirectory(part)) {
                    throw new IOException("PRODUCT_PARENT_NOT_DIRECTORY:" + item.target());
                }
            }
            if (Files.exists(destination)) {
                if (!Files.isRegularFile(destination)
                        || !Arrays.equals(Files.readAllBytes(destination), expected)) {
                    throw new IOException("PRODUCT_POSTIMAGE_DIFFERS:" + item.target());
                }
            } else if (!apply) {
                throw new IOException("PRODUCT_POSTIMAGE_MISSING:" + item.target());
            }
            verified[index] = expected;
        }

        if (apply) {
            for (int index = 0; index < INPUTS.size(); index++) {
                Install item = INPUTS.get(index);
                Path destination = root.resolve(item.target()).normalize();
                for (Path part = destination; !part.equals(root); part = part.getParent()) {
                    if (Files.isSymbolicLink(part)) {
                        throw new IOException("SYMLINK_PATH:" + item.target());
                    }
                    if (!part.equals(destination) && Files.exists(part) && !Files.isDirectory(part)) {
                        throw new IOException("PRODUCT_PARENT_NOT_DIRECTORY:" + item.target());
                    }
                }
                if (Files.exists(destination)) {
                    if (!Files.isRegularFile(destination)
                            || !Arrays.equals(Files.readAllBytes(destination), verified[index])) {
                        throw new IOException("PRODUCT_POSTIMAGE_DIFFERS:" + item.target());
                    }
                    continue;
                }
                Files.createDirectories(destination.getParent());
                Path temp = Files.createTempFile(destination.getParent(), ".m3-rank-", ".tmp");
                try {
                    Files.write(temp, verified[index]);
                    Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    Files.deleteIfExists(temp);
                }
            }
        }
        System.out.println("M3 rank/select recipe " + (apply ? "applied" : "checked")
                + "; sealed paths=" + INPUTS.size());
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
