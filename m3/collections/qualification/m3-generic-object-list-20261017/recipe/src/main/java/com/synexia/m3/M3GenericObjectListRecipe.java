/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
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
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

/**
 * Reusable Maven-bound, source-hash-sealed add-only collection recipe.
 *
 * <p>This uses the established Synexia exact-template receiving pattern: qualify
 * original template bytes, preflight every target path, refuse non-absent drift,
 * and materialize generated sources with the same hashes. OpenRewrite is the
 * separately maintained owner for any future transformation of an existing
 * Java AST; this bounded recipe does not rewrite incumbent production files.
 */
public final class M3GenericObjectListRecipe {
    private record Output(String name, String relative, String sha256) { }
    private static final String JAVA = "m3/collections/src/main/java/com/m3/collections/";
    private static final String TEST = "m3/collections/src/test/java/com/m3/collections/";
    private static final List<Output> OUTPUTS = List.of(
        new Output("M3ArrayList.java", JAVA + "M3ArrayList.java",
                "ae1092d66bb27e18c0f435d9dc148bd0f46fbc33f9731d9e2d2194142ccc3d32"),
        new Output("M3IndexedListSnapshot.java", JAVA + "M3IndexedListSnapshot.java",
                "0f0939615884c4d8bbfe2a3c9bbc5072d49faa6c20636c2eadac3104269aefaa"),
        new Output("M3GenericCollections.java", JAVA + "M3GenericCollections.java",
                "7f44e22663f67a7e69108fd097807c36bcf0e62a421481a16dc0fbfd28435f6a"),
        new Output("M3GenericArrayListSelfTest.java", TEST + "M3GenericArrayListSelfTest.java",
                "3ef322ba4992a0fd4c60e5e96c34f99c68137f5879e809e46d773b472f652035"),
        new Output("M3GenericArrayListTest.java", TEST + "M3GenericArrayListTest.java",
                "b3c599bbb1e1300517ee269c9024f07a591f5cc3178abbb665c555bb5a96bf02"));

    private M3GenericObjectListRecipe() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--selftest".equals(args[0])) {
            selfTest();
            return;
        }
        if (args.length != 2 || !("--apply".equals(args[0]) || "--check".equals(args[0]))) {
            throw new IllegalArgumentException("Usage: --selftest | --check|--apply <repo-root>");
        }
        run(Path.of(args[1]), "--apply".equals(args[0]));
    }

    public static void run(Path repoRoot, boolean apply) throws IOException {
        Path root = repoRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root.resolve("m3/collections"))) {
            throw new IOException("REFUSE_NOT_M3JDK21_ROOT");
        }
        // Preflight every template and every destination before writing any output.
        for (Output output : OUTPUTS) {
            byte[] expected = template(output);
            Path destination = root.resolve(output.relative()).normalize();
            if (!destination.startsWith(root)) throw new IOException("REFUSE_PATH_ESCAPE");
            for (Path scan = destination; !scan.equals(root); scan = scan.getParent()) {
                if (Files.isSymbolicLink(scan)) throw new IOException("REFUSE_SYMLINK:" + scan);
            }
            if (Files.exists(destination)) {
                if (!Files.isRegularFile(destination)
                        || !Arrays.equals(Files.readAllBytes(destination), expected)) {
                    throw new IOException("REFUSE_DRIFT:" + output.relative());
                }
            } else if (!apply) {
                throw new IOException("REFUSE_MISSING:" + output.relative());
            }
        }
        if (apply) {
            for (Output output : OUTPUTS) {
                Path dest = root.resolve(output.relative());
                if (Files.exists(dest)) continue;
                Files.createDirectories(dest.getParent());
                Path temp = Files.createTempFile(dest.getParent(), ".m3-generic-", ".tmp");
                try {
                    Files.write(temp, template(output));
                    Files.move(temp, dest, StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    Files.deleteIfExists(temp);
                }
            }
        }
        System.out.println("PASS M3GenericObjectListRecipe mode=" + (apply ? "apply" : "check")
                + " sealed=" + OUTPUTS.size());
    }

    private static byte[] template(Output output) throws IOException {
        try (InputStream in = M3GenericObjectListRecipe.class.getResourceAsStream(
                "/templates/" + output.name())) {
            if (in == null) throw new IOException("REFUSE_MISSING_TEMPLATE:" + output.name());
            byte[] expected = in.readAllBytes();
            try {
                String actual = HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(expected));
                if (!actual.equals(output.sha256())) {
                    throw new IOException("REFUSE_TEMPLATE_HASH:" + output.name());
                }
            } catch (NoSuchAlgorithmException problem) {
                throw new IOException("REFUSE_SHA256_UNAVAILABLE", problem);
            }
            return expected;
        }
    }

    private static void selfTest() throws Exception {
        Path root = Files.createTempDirectory("m3-generic-object-list-recipe-");
        try {
            Files.createDirectories(root.resolve("m3/collections"));
            try {
                run(root, false);
                throw new AssertionError("missing file admitted");
            } catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_MISSING:")) throw expected;
            }
            run(root, true);
            run(root, false);
            run(root, true); // Idempotent second materialization.
            Path first = root.resolve(OUTPUTS.get(0).relative());
            byte[] original = Files.readAllBytes(first);
            Files.writeString(first, "unqualified manual production patch");
            try {
                run(root, true);
                throw new AssertionError("drift must refuse");
            } catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_DRIFT:")) throw expected;
            }
            if (!"unqualified manual production patch".equals(Files.readString(first))) {
                throw new AssertionError("drift incorrectly overwritten");
            }
            Files.write(first, original);
            run(root, false);
            Path absentParent = root.resolve("m3/collections/src/test/java/com/m3/collections");
            Path second = root.resolve(OUTPUTS.get(4).relative());
            Files.delete(second);
            Path foreign = Files.createTempDirectory("m3-recipe-symlink-");
            try {
                // Symlink obstruction at final destination, no write allowed.
                Files.createSymbolicLink(second, foreign.resolve("missing.java"));
                try {
                    run(root, true);
                    throw new AssertionError("symlink must refuse");
                } catch (IOException expected) {
                    if (!expected.getMessage().startsWith("REFUSE_SYMLINK:")) throw expected;
                }
            } finally {
                Files.deleteIfExists(second);
                Files.deleteIfExists(foreign);
            }
            run(root, true);
            run(root, false);
            System.out.println("PASS M3GenericObjectListRecipeSelfTest missing/drift/symlink/fixed-point");
        } finally {
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
