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
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

/**
 * Deterministic Maven-bound source-sealed M3 add-only Java recipe. New files
 * require no existing Java AST owner; an existing-class behavioral rewrite
 * must reuse the established OpenRewrite Java-LST recipe instead of this atom.
 * Every target is preflighted before emission, and the second run is a fixed point.
 */
public final class M3NativeFiveLibraryRecipe {
    private record Output(String file, String destination, String sha256) { }
    private static final List<Output> OUTPUTS = List.of(
        new Output("M3IndexedBiMap.java", "m3/collections/src/main/java/jdk/m3/collections/M3IndexedBiMap.java", "fbbde38cc4466530feaad9320edc7957c9e4c87b660a087bf0404e63f572610d"),
        new Output("M3IndexedSetMultiMap.java", "m3/collections/src/main/java/jdk/m3/collections/M3IndexedSetMultiMap.java", "ca81a01ffec6f72741b48bdb7e2a763dc72a0e9bf95a8ab8e8e7acd20d5626d8"),
        new Output("M3ListMultiValueMap.java", "m3/collections/src/main/java/jdk/m3/collections/M3ListMultiValueMap.java", "a31bbc27e2e83415d38985d2a90e245756a9e522e69ee91bfc0476795e2490c0"),
        new Output("M3NativeFiveLibrarySdk.java", "m3/collections/src/main/java/jdk/m3/collections/M3NativeFiveLibrarySdk.java", "b4b666d5b3b30da1ad66659522f9ac323744db498bdff76ce8ad871f48313e92"),
        new Output("M3NativeFiveLibrarySelfTest.java", "m3/collections/src/test/java/jdk/m3/collections/M3NativeFiveLibrarySelfTest.java", "b6318ca3c502b2b501e136dadd757dd4ec6bbb8e2bb36300308fad53d97a0c2f"));
    private M3NativeFiveLibraryRecipe() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--selftest".equals(args[0])) {
            selfTest(); return;
        }
        if (args.length != 2 || !("--apply".equals(args[0]) || "--check".equals(args[0]))) {
            throw new IllegalArgumentException("--selftest | --apply|--check receiver-root");
        }
        run(Path.of(args[1]), "--apply".equals(args[0]));
    }

    public static void run(Path receiver, boolean apply) throws IOException {
        Path root = receiver.toAbsolutePath().normalize();
        if (!Files.isDirectory(root.resolve("m3/collections"))) {
            throw new IOException("REFUSE_NON_M3_RECEIVER");
        }
        for (Output output : OUTPUTS) {
            byte[] bytes = template(output);
            Path target = root.resolve(output.destination()).normalize();
            if (!target.startsWith(root)) throw new IOException("REFUSE_TRAVERSAL");
            for (Path item = target; !item.equals(root); item = item.getParent()) {
                if (Files.isSymbolicLink(item)) throw new IOException("REFUSE_SYMLINK");
            }
            if (Files.exists(target)) {
                if (!Files.isRegularFile(target) || !Arrays.equals(Files.readAllBytes(target), bytes)) {
                    throw new IOException("REFUSE_DRIFT:" + output.destination());
                }
            } else if (!apply) throw new IOException("REFUSE_MISSING:" + output.destination());
        }
        if (apply) {
            for (Output output : OUTPUTS) {
                Path path = root.resolve(output.destination());
                if (Files.exists(path)) continue;
                Files.createDirectories(path.getParent());
                Path temporary = Files.createTempFile(path.getParent(), ".m3-five-", ".tmp");
                try {
                    Files.write(temporary, template(output));
                    Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
                } finally { Files.deleteIfExists(temporary); }
            }
        }
        System.out.println("PASS M3NativeFiveLibraryRecipe mode="
                + (apply ? "apply" : "check") + " files=" + OUTPUTS.size());
    }

    private static byte[] template(Output output) throws IOException {
        try (InputStream resource = M3NativeFiveLibraryRecipe.class.getResourceAsStream(
                "/templates/" + output.file())) {
            if (resource == null) throw new IOException("REFUSE_MISSING_RESOURCE:" + output.file());
            byte[] bytes = resource.readAllBytes();
            try {
                String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                if (!actual.equals(output.sha256())) throw new IOException("REFUSE_SHA256:" + output.file());
            } catch (NoSuchAlgorithmException failure) {
                throw new IOException("REFUSE_NO_SHA256", failure);
            }
            return bytes;
        }
    }

    private static void selfTest() throws Exception {
        Path root = Files.createTempDirectory("m3-five-native-recipe-");
        try {
            Files.createDirectories(root.resolve("m3/collections"));
            try { run(root, false); throw new AssertionError("missing output admitted"); }
            catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_MISSING")) throw expected;
            }
            run(root, true);
            run(root, true);
            run(root, false);
            Path owner = root.resolve(OUTPUTS.get(0).destination());
            byte[] original = Files.readAllBytes(owner);
            Files.writeString(owner, "drifted generated Java");
            try { run(root, true); throw new AssertionError("drift accepted"); }
            catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_DRIFT")) throw expected;
            }
            if (!"drifted generated Java".equals(Files.readString(owner))) {
                throw new AssertionError("drift overwritten");
            }
            Files.write(owner, original);
            run(root, false);
            System.out.println("PASS M3NativeFiveLibraryRecipeSelfTest missing/drift/fixed-point");
        } finally {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path path : files.sorted((a,b) -> b.getNameCount() - a.getNameCount()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
