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
/*
 * Target-local replay of the canonical Synexia SDK bulk-capture recipe.
 */
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

/**
 * An exact-source, Maven-executed transformation; source changes belong to
 * Synexia's com.synexia.m3.SdkFilterWindow20261017 recipe, not this receiver.
 */
public final class M3SdkBulkCaptureRecipe {
    private static final String TARGET =
            "m3/collections/src/main/java/com/m3/collections/M3MutableFilteredViews.java";
    private static final String BEFORE =
            "bbc218ecd8c87648976501a5f09aa8baa5af94e1ea79fdef1647ac4248f59e65";
    private static final String AFTER =
            "ff73119862556f307cc3e6af918336793faae0c2575f2cedda34b4890ba916b0";

    private M3SdkBulkCaptureRecipe() { }

    private static byte[] resource(String name) throws IOException {
        try (InputStream stream = M3SdkBulkCaptureRecipe.class.getResourceAsStream(name)) {
            if (stream == null) throw new IllegalStateException("missing sealed resource " + name);
            return stream.readAllBytes();
        }
    }

    private static String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void assertDigest(String expected, byte[] bytes) {
        if (!digest(bytes).equals(expected)) {
            throw new IllegalStateException("source SHA-256 drift: expected " + expected
                    + ", found " + digest(bytes));
        }
    }

    private static byte[] oldSource() throws IOException {
        byte[] source = resource("M3MutableFilteredViews.before.txt");
        assertDigest(BEFORE, source);
        return source;
    }

    private static byte[] generatedSource() throws IOException {
        byte[] source = resource("M3MutableFilteredViews.after.txt");
        assertDigest(AFTER, source);
        return source;
    }

    private static void check(Path target) throws IOException {
        if (Files.isSymbolicLink(target)) throw new IllegalStateException("symlink receiver rejected");
        assertDigest(AFTER, Files.readAllBytes(target));
    }

    private static void apply(Path target) throws IOException {
        byte[] generated = generatedSource();
        byte[] before = oldSource();
        if (Files.isSymbolicLink(target)) throw new IllegalStateException("symlink receiver rejected");
        byte[] current = Files.readAllBytes(target);
        if (Arrays.equals(current, generated)) return;  // fixed point
        if (!Arrays.equals(current, before)) throw new IllegalStateException(
                "source changed; canonical recipe must be repaired before forwarding");
        Path temporary = Files.createTempFile(target.getParent(), "m3-sdk-recipe-", ".tmp");
        try {
            Files.write(temporary, generated);
            if (!Arrays.equals(Files.readAllBytes(target), before)) {
                throw new IllegalStateException("source changed during transformation");
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
        check(target);
    }

    private static void selftest() throws IOException {
        Path workspace = Files.createTempDirectory("m3-sdk-bulk-recipe-");
        Path trial = workspace.resolve("receiver.java");
        try {
            Files.write(trial, oldSource());
            apply(trial);
            check(trial);
            byte[] first = Files.readAllBytes(trial);
            apply(trial);
            if (!Arrays.equals(first, Files.readAllBytes(trial))) {
                throw new AssertionError("not a fixed point");
            }
            Files.writeString(trial, "tampered target");
            boolean refused = false;
            try { check(trial); } catch (IllegalStateException expected) { refused = true; }
            if (!refused) throw new AssertionError("tampered output accepted");
            refused = false;
            try { apply(trial); } catch (IllegalStateException expected) { refused = true; }
            if (!refused) throw new AssertionError("unknown preimage accepted");
            if (!Files.readString(trial).equals("tampered target")) {
                throw new AssertionError("unknown source overwritten");
            }
            System.out.println("M3_SDK_BULK_RECIPE_PASS fixed_point=1 refusal=2 seals=2");
        } finally {
            Files.deleteIfExists(trial);
            Files.deleteIfExists(workspace);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--selftest")) {
            selftest();
            return;
        }
        if (args.length != 2 || (!args[0].equals("--apply") && !args[0].equals("--check"))) {
            throw new IllegalArgumentException("usage: --apply|--check REPOSITORY_ROOT or --selftest");
        }
        Path receiver = Path.of(args[1]).toAbsolutePath().normalize().resolve(TARGET);
        if (args[0].equals("--apply")) apply(receiver);
        else check(receiver);
        System.out.println("M3_SDK_BULK_REPLAY_PASS mode=" + args[0] + " source=" + AFTER);
    }
}
