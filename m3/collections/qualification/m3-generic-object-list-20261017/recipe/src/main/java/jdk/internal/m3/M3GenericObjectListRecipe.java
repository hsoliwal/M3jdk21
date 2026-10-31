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
package jdk.internal.m3;

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
    private static final String JAVA = "m3/collections/src/main/java/jdk/m3/collections/";
    private static final String TEST = "m3/collections/src/test/java/jdk/m3/collections/";
    private static final List<Output> OUTPUTS = List.of(
        new Output("M3ArrayList.java", JAVA + "M3ArrayList.java",
                "78340d2c41e0ade88474171636a02da07ecae0860c6988acaefac5c1fdf0fa03"),
        new Output("M3IndexedListSnapshot.java", JAVA + "M3IndexedListSnapshot.java",
                "75c8d36856751cbc19d81845606c57daa468b0f6008dbccc1ae009094060c50b"),
        new Output("M3GenericCollections.java", JAVA + "M3GenericCollections.java",
                "325dc5eee8f95173a430bde278bdf46011ec804b74c9ae2ca3c79ef71613da24"),
        new Output("M3GenericArrayListSelfTest.java", TEST + "M3GenericArrayListSelfTest.java",
                "79cf753a3bc3acee6c545b4599d6ee86b776f6cd37f78fe8b3ed43c269f71149"),
        new Output("M3GenericArrayListTest.java", TEST + "M3GenericArrayListTest.java",
                "3649f527ba7716d01374fa6d348b1b9cc37730deb6b0db6f01260827054f6d7d"));

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
            Path absentParent = root.resolve("m3/collections/src/test/java/jdk/m3/collections");
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
