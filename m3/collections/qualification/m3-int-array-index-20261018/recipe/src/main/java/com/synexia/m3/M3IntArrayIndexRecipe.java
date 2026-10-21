/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.m3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

/**
 * Canonical Maven-executed exact-source recipe, not a competing AST/atomizer engine.
 * New owners are admitted only from ABSENT state or their exact SHA-256 postimage.
 * A future change MUST update the Synexia recipe and regenerate receiver sources.
 */
public final class M3IntArrayIndexRecipe {
    private record Spec(String destination, String file, String postSha) { }
    private static final List<Spec> FILES = List.of(
            new Spec("m3/collections/src/main/java/com/m3/collections/M3IntList.java", "M3IntList.java.txt", "7c780a7301695f2948d008dfe88a6619e2eed5567aee79576ccce7e51071b30f"),
            new Spec("m3/collections/src/main/java/com/m3/collections/M3IntArrayList.java", "M3IntArrayList.java.txt", "ba4a2f428ad628b6c54d13b3cf63429e87cf8f8ecaa4da8ae39bff4e12433efe"),
            new Spec("m3/collections/src/main/java/com/m3/collections/M3IntIndexedSnapshot.java", "M3IntIndexedSnapshot.java.txt", "d84c88910a7855fd29596054ab6d30cba48affe4baee6f58e58c3d1dff9943fa"),
            new Spec("m3/collections/src/test/java/com/m3/collections/M3IntArrayListContractProof.java", "M3IntArrayListContractProof.java.txt", "2c9eedd77db33d285e25e40c324ee5d338bb2d606b23eb727f00a282f1cf4d68"));
    private M3IntArrayIndexRecipe() { }

    private static String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static byte[] image(Spec spec) throws IOException {
        try (InputStream in = M3IntArrayIndexRecipe.class.getResourceAsStream(spec.file())) {
            if (in == null) throw new IllegalStateException("missing canonical recipe " + spec.file());
            byte[] bytes = in.readAllBytes();
            if (!digest(bytes).equals(spec.postSha())) {
                throw new IllegalStateException("canonical source SHA drift " + spec.file());
            }
            return bytes;
        }
    }
    /** All-file preflight before first source mutation; independent per-FILE commits are safe. */
    private static List<Spec> inventory(Path root, boolean exactOutput) throws IOException {
        List<Spec> missing = new ArrayList<>();
        for (Spec spec : FILES) {
            Path path = root.resolve(spec.destination()).normalize();
            if (!path.startsWith(root) || Files.isSymbolicLink(path)) {
                throw new IllegalStateException("unsafe receiver " + spec.destination());
            }
            byte[] canonical = image(spec);
            if (!Files.exists(path)) {
                if (exactOutput) throw new IllegalStateException("missing emitted source " + path);
                missing.add(spec);
            } else if (!java.util.Arrays.equals(Files.readAllBytes(path), canonical)) {
                throw new IllegalStateException("occupied or changed source " + path);
            }
        }
        return missing;
    }
    private static void apply(Path root) throws IOException {
        List<Spec> pending = inventory(root, false);
        for (Spec spec : pending) {
            Path output = root.resolve(spec.destination());
            Files.createDirectories(output.getParent());
            Files.write(output, image(spec), StandardOpenOption.CREATE_NEW);
        }
        inventory(root, true);
    }
    private static void selftest() throws IOException {
        Path root = Files.createTempDirectory("m3-int-index-").toAbsolutePath().normalize();
        try {
            apply(root);
            inventory(root, true);
            apply(root);
            inventory(root, true); // deterministic fixed point
            Path changed = root.resolve(FILES.get(1).destination());
            Files.writeString(changed, "poisoned source");
            boolean refused = false;
            try { apply(root); } catch (IllegalStateException expected) { refused = true; }
            if (!refused) throw new AssertionError("modified source admitted");
            Files.delete(changed);
            Path other = root.resolve(FILES.get(0).destination());
            Files.writeString(other, "colliding interface");
            refused = false;
            try { apply(root); } catch (IllegalStateException expected) { refused = true; }
            if (!refused || !Files.readString(other).equals("colliding interface")) {
                throw new AssertionError("occupied-source preflight failed");
            }
            System.out.println("M3_INT_ARRAY_RECIPE_PASS generated=4 fixed_point=true refusal=2");
        } finally {
            try (Stream<Path> all = Files.walk(root)) {
                for (Path path : all.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--selftest")) { selftest(); return; }
        if (args.length != 2 || (!args[0].equals("--apply") && !args[0].equals("--check"))) {
            throw new IllegalArgumentException("usage --apply|--check REPOSITORY_ROOT or --selftest");
        }
        Path root = Path.of(args[1]).toAbsolutePath().normalize();
        if (args[0].equals("--apply")) apply(root);
        else inventory(root, true);
        System.out.println("M3_INT_ARRAY_RECIPE_PASS mode=" + args[0]);
    }
}
