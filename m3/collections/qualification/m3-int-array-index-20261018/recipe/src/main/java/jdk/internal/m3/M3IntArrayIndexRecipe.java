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
            new Spec("m3/collections/src/main/java/jdk/m3/collections/M3IntList.java", "M3IntList.java.txt", "b16100e301609a1dab858c81748954ce226bab78641f65142700c9d064f360fa"),
            new Spec("m3/collections/src/main/java/jdk/m3/collections/M3IntArrayList.java", "M3IntArrayList.java.txt", "1414a1dcc62a9f6c7f9a09accc308425300306a8a4cf1fc5a53fa8dffd60c60e"),
            new Spec("m3/collections/src/main/java/jdk/m3/collections/M3IntIndexedSnapshot.java", "M3IntIndexedSnapshot.java.txt", "a4fe27ca132e64075b1de1b487b627285e327ac58e3738a77becba9d560a5f4b"),
            new Spec("m3/collections/src/test/java/jdk/m3/collections/M3IntArrayListContractProof.java", "M3IntArrayListContractProof.java.txt", "4a7e4aee3ec8d02dc6934bdabca7d3bb227feb22e456051675074dbb142fcea3"));
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
