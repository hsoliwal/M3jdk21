// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package com.synexia.m3;

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
 * Initial source admission uses templates; transformation of an existing
 * production AST must instead use the canonical OpenRewrite recipe owner.
 */
public final class M3LongIndexPathMasksRecipe {
    private static final String[] NAMES = {
        "M3LongIndexPathMasks.java", "M3LongIndexPathMasksSelfTest.java"
    };
    private static final String[] HASHES = {
        "c7692b6d8d6f6ea6b586580b351747165e4a9a7d9bb7b37979dd737d4ea258e9",
        "4bf27c96e87145256b7d72ecd3e593da3035425fa896f576aad3defb542ae82b"
    };
    private static final String[] TARGETS = {
        "m3/collections/src/main/java/com/m3/collections/M3LongIndexPathMasks.java",
        "m3/collections/src/test/java/com/m3/collections/M3LongIndexPathMasksSelfTest.java"
    };

    private M3LongIndexPathMasksRecipe() { }

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
        System.out.println("M3_LONG_INDEX_PATH_RECIPE_PASS|mode=" + args[0]
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
