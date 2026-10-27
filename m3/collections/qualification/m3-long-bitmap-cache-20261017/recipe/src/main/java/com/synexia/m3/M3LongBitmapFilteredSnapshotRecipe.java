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
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/**
 * Canonical Maven-bound, deterministic source-sealed installation recipe.
 * A new source owner is absent-or-identical; no source file is patched manually.
 * Existing Java AST transformations belong to OpenRewrite recipes. This crate
 * only admits two new first-party source files without altering their ABI peers.
 */
public final class M3LongBitmapFilteredSnapshotRecipe {
    private record Output(String template, String receiver, String sha256) { }
    private static final List<Output> OUTPUTS = List.of(
            new Output("M3LongBitmapFilteredSnapshot.java",
                    "m3/collections/src/main/java/com/m3/collections/M3LongBitmapFilteredSnapshot.java",
                    "ef4e4f4b7a8871cba0e631b96480ad3cf1e43640e5bf84a89cc91806df2d54d9"),
            new Output("M3LongBitmapFilteredSnapshotSelfTest.java",
                    "m3/collections/src/test/java/com/m3/collections/M3LongBitmapFilteredSnapshotSelfTest.java",
                    "de0be6b6fa43ea9b824db0f79106213976e94f8f7cdf6a84f15378a6c090cc37"));

    private M3LongBitmapFilteredSnapshotRecipe() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--selftest")) {
            selfTest();
            return;
        }
        if (args.length != 2 || !(args[0].equals("--check") || args[0].equals("--apply"))) {
            throw new IllegalArgumentException("usage: --selftest | --check|--apply <M3JDK21-root>");
        }
        execute(Path.of(args[1]), args[0].equals("--apply"));
    }

    /** All templates, paths, and installed contents are admitted before first write. */
    public static void execute(Path receiverRoot, boolean apply) throws IOException {
        Path root = receiverRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root.resolve("m3/collections"))) {
            throw new IOException("REFUSE_NOT_M3JDK21_RECEIVER");
        }
        byte[][] bytes = new byte[OUTPUTS.size()][];
        for (int n = 0; n < OUTPUTS.size(); n++) {
            Output item = OUTPUTS.get(n);
            bytes[n] = verifiedResource(item);
            Path target = checkedPath(root, item.receiver());
            if (Files.exists(target)) {
                if (!Files.isRegularFile(target)
                        || !Arrays.equals(bytes[n], Files.readAllBytes(target))) {
                    throw new IOException("REFUSE_PRODUCT_DRIFT:" + item.receiver());
                }
            } else if (!apply) {
                throw new IOException("REFUSE_PRODUCT_MISSING:" + item.receiver());
            }
        }
        if (apply) {
            for (int n = 0; n < OUTPUTS.size(); n++) {
                Path target = checkedPath(root, OUTPUTS.get(n).receiver());
                if (Files.exists(target)) continue;
                Files.createDirectories(target.getParent());
                Path temp = Files.createTempFile(target.getParent(), ".m3-bitmap-recipe-", ".tmp");
                try {
                    Files.write(temp, bytes[n]);
                    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    Files.deleteIfExists(temp);
                }
            }
        }
        System.out.println("PASS M3LongBitmapFilteredSnapshotRecipe "
                + (apply ? "apply" : "check") + " files=" + OUTPUTS.size());
    }

    private static Path checkedPath(Path root, String relativePath) throws IOException {
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new IOException("REFUSE_TARGET_ESCAPE");
        }
        for (Path current = target; !current.equals(root); current = current.getParent()) {
            if (Files.isSymbolicLink(current)) {
                throw new IOException("REFUSE_SYMLINK:" + relativePath);
            }
        }
        return target;
    }

    private static byte[] verifiedResource(Output item) throws IOException {
        try (InputStream input = M3LongBitmapFilteredSnapshotRecipe.class.getResourceAsStream(
                "/templates/" + item.template())) {
            if (input == null) throw new IOException("REFUSE_MISSING_TEMPLATE:" + item.template());
            byte[] bytes = input.readAllBytes();
            try {
                String actual = HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(bytes));
                if (!actual.equals(item.sha256())) {
                    throw new IOException("REFUSE_TEMPLATE_HASH:" + item.template());
                }
            } catch (NoSuchAlgorithmException exception) {
                throw new IOException("SHA_256_UNAVAILABLE", exception);
            }
            return bytes;
        }
    }

    private static void expectRefusal(Path root, boolean apply, String error) throws IOException {
        try {
            execute(root, apply);
            throw new AssertionError("expected refusal " + error);
        } catch (IOException e) {
            if (!e.getMessage().startsWith(error)) throw e;
        }
    }

    private static void selfTest() throws IOException {
        Path root = Files.createTempDirectory("m3-bitmap-filter-recipe-");
        try {
            Path owner = root.resolve("m3/collections");
            Files.createDirectories(owner);
            expectRefusal(root, false, "REFUSE_PRODUCT_MISSING");
            execute(root, true);
            execute(root, true); // idempotent reapply
            execute(root, false); // fixed point
            Path a = root.resolve(OUTPUTS.get(0).receiver());
            Path b = root.resolve(OUTPUTS.get(1).receiver());
            Files.writeString(a, "drift");
            expectRefusal(root, true, "REFUSE_PRODUCT_DRIFT");
            if (!Files.readString(a).equals("drift")
                    || !Arrays.equals(Files.readAllBytes(b), verifiedResource(OUTPUTS.get(1)))) {
                throw new AssertionError("drift repair/partial edits prohibited");
            }
            Files.write(a, verifiedResource(OUTPUTS.get(0)));
            Files.delete(b);
            expectRefusal(root, false, "REFUSE_PRODUCT_MISSING");
            // An occupied preimage elsewhere cannot be overwritten by --apply.
            Files.writeString(b, "occupied");
            expectRefusal(root, true, "REFUSE_PRODUCT_DRIFT");
            if (!Files.readString(b).equals("occupied")) throw new AssertionError("occupied output changed");
            Files.delete(b);
            execute(root, true);
            execute(root, false);
            System.out.println("PASS M3LongBitmapFilteredSnapshotRecipeSelfTest "
                    + "missing/drift/refusal/idempotence/fixed-point");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
