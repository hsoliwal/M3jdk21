// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.internal.m3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Single-purpose, Maven-built, SHA-256-sealed M3 source admission recipe.
 * Existing paths are accepted only at the exact fixed-point postimage.
 * Initial additive Java and JNI/C source admission reuses the existing M3
 * generator mechanics; changes to an existing Java AST require the canonical
 * OpenRewrite recipe owner. This generator never overwrites a differing file.
 */
public final class M3LongIndexPathJNIRecipe {
    private static final String[] NAMES = {
        "M3LongIndexPathMasksJNI.java", "M3LongIndexPathMasksJNISelfTest.java",
        "m3_long_index_path_masks_jni.c"
    };
    private static final String[] HASHES = {
        "d82e38a2773b65ea66a534f8ff16f9ec5e65fbcb0aff1c4db163d6c688f0df55",
        "02a3a0c1b34a4b643f0795614db0c029addb1fd3c3f03047eada8ab47cdc9b02",
        "95a8ee0c924b570ef19a8403ac9f5ee2d970e9d27d1df562cb87d40b5e5b2ba4"
    };
    private static final String[] TARGETS = {
        "m3/collections/src/main/java/jdk/m3/collections/M3LongIndexPathMasksJNI.java",
        "m3/collections/src/test/java/jdk/m3/collections/M3LongIndexPathMasksJNISelfTest.java",
        "m3/collections/src/main/native/collections/m3_long_index_path_masks_jni.c"
    };

    private M3LongIndexPathJNIRecipe() { }

    /** Usage: --apply|--check TARGET_CHECKOUT CANONICAL_TEMPLATE_DIRECTORY. */
    public static void main(String[] args) throws IOException {
        if (args.length != 3 || !(args[0].equals("--apply") || args[0].equals("--check")))
            throw new IllegalArgumentException("usage: --apply|--check checkout templates");
        Path root = Path.of(args[1]).toRealPath(LinkOption.NOFOLLOW_LINKS);
        Path templates = Path.of(args[2]).toRealPath(LinkOption.NOFOLLOW_LINKS);
        boolean checkOnly = args[0].equals("--check");
        byte[][] sealed = new byte[NAMES.length][];
        Path[] receivers = new Path[NAMES.length];
        // Preflight *all* inputs and occupied outputs before the first write.
        for (int i = 0; i < NAMES.length; i++) {
            Path source = templates.resolve(NAMES[i]);
            refuseSymbolicSegments(templates, source);
            sealed[i] = Files.readAllBytes(source);
            if (!HASHES[i].equals(sha256(sealed[i])))
                throw new IOException("source template SHA-256 mismatch: " + NAMES[i]);
            Path target = root.resolve(TARGETS[i]);
            refuseSymbolicSegments(root, target);
            receivers[i] = target;
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (!Arrays.equals(sealed[i], Files.readAllBytes(target)))
                    throw new IOException("occupied/drifted target: " + TARGETS[i]);
            } else if (checkOnly) {
                throw new IOException("missing generated target: " + TARGETS[i]);
            }
        }
        if (!checkOnly) {
            for (int i = 0; i < TARGETS.length; i++) {
                if (!Files.exists(receivers[i], LinkOption.NOFOLLOW_LINKS)) {
                    Files.createDirectories(receivers[i].getParent());
                    Files.write(receivers[i], sealed[i], StandardOpenOption.CREATE_NEW);
                }
            }
        }
        System.out.println("M3_LONG_INDEX_PATH_JNI_RECIPE_PASS|mode=" + args[0]
                + "|files=" + TARGETS.length);
    }

    private static void refuseSymbolicSegments(Path root, Path target) throws IOException {
        if (Files.isSymbolicLink(root) || !target.normalize().startsWith(root))
            throw new IOException("untrusted path root");
        Path part = root;
        for (Path component : root.relativize(target)) {
            part = part.resolve(component);
            if (Files.isSymbolicLink(part)) throw new IOException("symbolic path: " + part);
        }
    }

    private static String sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
