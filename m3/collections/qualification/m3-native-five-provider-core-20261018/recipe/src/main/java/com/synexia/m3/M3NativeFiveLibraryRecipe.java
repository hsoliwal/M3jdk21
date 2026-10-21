/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
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
        new Output("M3IndexedBiMap.java", "m3/collections/src/main/java/com/m3/collections/M3IndexedBiMap.java", "60345effe659d22bd0cb756264bf17de89611a67ee40537bc25d6ccf77593e5c"),
        new Output("M3IndexedSetMultiMap.java", "m3/collections/src/main/java/com/m3/collections/M3IndexedSetMultiMap.java", "18348390c8c7b50233ccab50b4b1b1887f8ce583c36cdab6498fc06eacddebc0"),
        new Output("M3ListMultiValueMap.java", "m3/collections/src/main/java/com/m3/collections/M3ListMultiValueMap.java", "ae4a6501d69a07e416c9e3ecf5791ea47c70e9f37035027259a0aab5f868b307"),
        new Output("M3NativeFiveLibrarySdk.java", "m3/collections/src/main/java/com/m3/collections/M3NativeFiveLibrarySdk.java", "250b9cf332fbe07048884790ae701f3270e84252024511ee67f57cb3904a73ed"),
        new Output("M3NativeFiveLibrarySelfTest.java", "m3/collections/src/test/java/com/m3/collections/M3NativeFiveLibrarySelfTest.java", "3b45199d9643e7d2f8fb09a28c407aa469e67c706e0f213ed3e2103ac4a065d0"));
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
