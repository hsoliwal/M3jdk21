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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.stream.Stream;

/** Source-sealed Maven generator for new M3-owned Java indexed table files. */
public final class M3NativeIndexedTableRecipe {
    private static final String PREFIX = "m3/collections/";
    private static final String[] FILES = {
        "src/main/java/com/m3/collections/M3IndexedTable.java",
        "src/test/java/com/m3/collections/M3IndexedTableSelfTest.java",
        "src/test/java/com/m3/collections/M3IndexedTableTest.java"
    };
    private static final String[] HASHES = {
        "9b87cefcc65e295afe516a1d007e3b5acbc87a8035e1f55223d1b2558cf7858c",
        "b6a5ea9b9105b15856af1833026cbe665bc060faccefe7a63973a306de7980ca",
        "136b8859525e7a10e10e9fd5184fb6e02f056eb8b85f41a04867079aa1c4ee11"
    };

    private M3NativeIndexedTableRecipe() { }
    private static String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
    private static String template(int index) throws IOException {
        String name = Path.of(FILES[index]).getFileName() + ".tpl";
        try (InputStream stream = M3NativeIndexedTableRecipe.class
                .getResourceAsStream("/templates/" + name)) {
            if (stream == null) throw new IOException("missing template: " + name);
            String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if (!sha256(source).equals(HASHES[index])) {
                throw new IllegalStateException("template drift: " + name);
            }
            return source;
        }
    }
    private static void checkNoLinks(Path root, Path target) {
        if (Files.isSymbolicLink(root)) throw new IllegalStateException("symlink root");
        Path cursor = root;
        for (Path component : root.relativize(target)) {
            cursor = cursor.resolve(component);
            if (Files.isSymbolicLink(cursor)) {
                throw new IllegalStateException("symlink path: " + cursor);
            }
        }
    }

    /** Entire three-file set is preflighted before any write; no silent overwrite. */
    public static void run(Path root, boolean apply) throws IOException {
        Path base = root.toAbsolutePath().normalize();
        Path[] targets = new Path[FILES.length];
        String[] generated = new String[FILES.length];
        boolean[] missing = new boolean[FILES.length];
        for (int i = 0; i < FILES.length; i++) {
            targets[i] = base.resolve(PREFIX + FILES[i]).normalize();
            if (!targets[i].startsWith(base)) throw new IllegalStateException("path escape");
            checkNoLinks(base, targets[i]);
            generated[i] = template(i);
            missing[i] = !Files.exists(targets[i], LinkOption.NOFOLLOW_LINKS);
            if (missing[i] && !apply) {
                throw new IllegalStateException("receiver source absent: " + targets[i]);
            }
            if (!missing[i] && !Files.readString(targets[i], StandardCharsets.UTF_8)
                    .equals(generated[i])) {
                throw new IllegalStateException("receiver source drift: " + targets[i]);
            }
        }
        if (apply) {
            for (int i = 0; i < FILES.length; i++) {
                if (!missing[i]) continue;
                Files.createDirectories(targets[i].getParent());
                checkNoLinks(base, targets[i]);
                Files.writeString(targets[i], generated[i], StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE_NEW);
            }
        }
    }

    /** Stock Java21 executable tests for refusal, exact hashes and fixed point. */
    private static void selfTest() throws IOException {
        Path scratch = Files.createTempDirectory("m3-native-table-recipe-");
        try {
            boolean missing = false;
            try { run(scratch, false); }
            catch (IllegalStateException expected) { missing = true; }
            if (!missing) throw new AssertionError("missing target must refuse --check");
            run(scratch, true);
            run(scratch, true);
            run(scratch, false);
            Path first = scratch.resolve(PREFIX + FILES[0]);
            Files.writeString(first, "CORRUPTED", StandardCharsets.UTF_8);
            boolean drift = false;
            try { run(scratch, true); }
            catch (IllegalStateException expected) { drift = true; }
            if (!drift) throw new AssertionError("occupied/drifted target must refuse");
            Files.delete(first);
            boolean incomplete = false;
            try { run(scratch, false); }
            catch (IllegalStateException expected) { incomplete = true; }
            if (!incomplete) throw new AssertionError("partial target must refuse --check");
        } finally {
            try (Stream<Path> all = Files.walk(scratch)) {
                for (Path item : all.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(item);
                }
            }
        }
        System.out.println("PASS M3NativeIndexedTableRecipe exact hashes, drift refusal, fixed point");
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 1 && "--selftest".equals(args[0])) {
            selfTest(); return;
        }
        if (args.length != 2 || !("--apply".equals(args[0]) || "--check".equals(args[0]))) {
            throw new IllegalArgumentException("usage: --selftest OR (--apply|--check) REPO_ROOT");
        }
        run(Path.of(args[1]), "--apply".equals(args[0]));
        System.out.println("PASS M3NativeIndexedTableRecipe " + args[0]);
    }
}
