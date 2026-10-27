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
package com.synexia.m3;

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
 * Maven-bound source-sealed additive M3 recipe. Existing production source
 * is never patched; unexpected preimages refuse. Java AST rewriting remains
 * OpenRewrite-owned when a later rule needs to transform an existing class.
 */
public final class M3LongIndexProjectionRecipe {
    private record Output(String resource, String relativePath, String sha256) { }
    private static final List<Output> OUTPUTS = List.of(
        new Output("M3LongIndexProjection.java",
            "m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java",
            "fc4874e5db5472d8b2b79c122878c8531c536c4fe0b6d3ddfcce6b9ec6055eaf"),
        new Output("M3LongIndexProjectionSelfTest.java",
            "m3/collections/src/test/java/com/m3/collections/M3LongIndexProjectionSelfTest.java",
            "e611a47ae6362c7dc86557db99593b9242ea4cf3e94169138c4b4a45208e48cd"));

    private M3LongIndexProjectionRecipe() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--selftest".equals(args[0])) {
            selfTest();
            return;
        }
        if (args.length != 2 || !("--apply".equals(args[0]) || "--check".equals(args[0]))) {
            throw new IllegalArgumentException("Usage: --selftest | --check|--apply <target-repo-root>");
        }
        run(Path.of(args[1]), "--apply".equals(args[0]));
    }

    public static void run(Path targetRoot, boolean apply) throws IOException {
        Path root = targetRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root.resolve("m3/collections"))) {
            throw new IOException("REFUSE_NOT_M3JDK21_RECEIVER");
        }
        // Admission preflights *all* targets before writing any output.
        for (Output output : OUTPUTS) {
            byte[] expected = source(output);
            Path target = root.resolve(output.relativePath()).normalize();
            if (!target.startsWith(root)) throw new IOException("REFUSE_PATH_TRAVERSAL");
            for (Path scan = target; !scan.equals(root); scan = scan.getParent()) {
                if (Files.isSymbolicLink(scan)) throw new IOException("REFUSE_SYMLINK:" + scan);
            }
            if (Files.exists(target)) {
                if (!Files.isRegularFile(target) || !Arrays.equals(Files.readAllBytes(target), expected)) {
                    throw new IOException("REFUSE_DRIFT:" + output.relativePath());
                }
            } else if (!apply) {
                throw new IOException("REFUSE_MISSING:" + output.relativePath());
            }
        }
        if (apply) {
            for (Output output : OUTPUTS) {
                byte[] expected = source(output);
                Path target = root.resolve(output.relativePath()).normalize();
                if (Files.exists(target)) continue;
                Files.createDirectories(target.getParent());
                Path temporary = Files.createTempFile(target.getParent(), ".m3-index-", ".tmp");
                try {
                    Files.write(temporary, expected);
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    Files.deleteIfExists(temporary);
                }
            }
        }
        System.out.println("PASS M3LongIndexProjectionRecipe mode=" +
                (apply ? "apply" : "check") + " files=" + OUTPUTS.size());
    }

    private static byte[] source(Output item) throws IOException {
        try (InputStream stream = M3LongIndexProjectionRecipe.class.getResourceAsStream(
                "/templates/" + item.resource())) {
            if (stream == null) throw new IOException("MISSING_TEMPLATE:" + item.resource());
            byte[] source = stream.readAllBytes();
            try {
                String actual = HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(source));
                if (!actual.equals(item.sha256())) throw new IOException("TEMPLATE_HASH_MISMATCH");
            } catch (NoSuchAlgorithmException problem) {
                throw new IOException("SHA256_NOT_AVAILABLE", problem);
            }
            return source;
        }
    }

    private static void selfTest() throws IOException {
        Path root = Files.createTempDirectory("m3-index-projection-recipe-");
        try {
            Path collectionRoot = root.resolve("m3/collections");
            Files.createDirectories(collectionRoot);
            try {
                run(root, false);
                throw new AssertionError("missing output must refuse --check");
            } catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_MISSING")) throw expected;
            }
            run(root, true);
            run(root, true); // idempotence
            run(root, false); // fixed point
            Path target = root.resolve(OUTPUTS.get(0).relativePath());
            Files.writeString(target, "drift");
            try {
                run(root, true);
                throw new AssertionError("drifted target must refuse --apply");
            } catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_DRIFT")) throw expected;
            }
            // Preflight must not silently repair a drifted target.
            if (!"drift".equals(Files.readString(target))) throw new AssertionError("drift overwritten");
            System.out.println("PASS M3LongIndexProjectionRecipeSelfTest missing/drift/fixed-point");
        } finally {
            for (Output output : OUTPUTS) {
                Files.deleteIfExists(root.resolve(output.relativePath()));
            }
            Files.deleteIfExists(root.resolve("m3/collections/src/main/java/com/m3/collections"));
            Files.deleteIfExists(root.resolve("m3/collections/src/main/java/com/m3"));
            Files.deleteIfExists(root.resolve("m3/collections/src/main/java/com"));
            Files.deleteIfExists(root.resolve("m3/collections/src/main/java"));
            Files.deleteIfExists(root.resolve("m3/collections/src/main"));
            Files.deleteIfExists(root.resolve("m3/collections/src/test/java/com/m3/collections"));
            Files.deleteIfExists(root.resolve("m3/collections/src/test/java/com/m3"));
            Files.deleteIfExists(root.resolve("m3/collections/src/test/java/com"));
            Files.deleteIfExists(root.resolve("m3/collections/src/test/java"));
            Files.deleteIfExists(root.resolve("m3/collections/src/test"));
            Files.deleteIfExists(root.resolve("m3/collections/src"));
            Files.deleteIfExists(root.resolve("m3/collections"));
            Files.deleteIfExists(root.resolve("m3"));
            Files.deleteIfExists(root);
        }
    }
}
