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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

/**
 * Source-sealed Maven task recipe. The Synexia crate owns future improvements;
 * this performs serial, exact-preimage Java/C replay, never guessed patching.
 */
public final class M3MaskBoundedCurrentRecipe {
    private record Spec(String path, String resource, String oldSha, String newSha) { }
    private record Pending(Path path, byte[] oldBytes, byte[] newBytes) { }
    private static final List<Spec> FILES = List.of(
            new Spec("m3/collections/src/main/java/jdk/m3/collections/M3IndexMaskWindow.java", "M3IndexMaskWindow.java",
                    "1dcb626c7ee37dec021bdcc8928b06445f218d387b6b9becfb8c9c099bb1e03a", "268f039e623166f1cf5b957ea8284b25e14558ad6ea43b917b379386bd437508"),
            new Spec("m3/collections/src/main/native/collections/m3_index_mask_window.c", "m3_index_mask_window.c",
                    "8359740f728309bf619aa570f4b26b47186b836f4c0e30d55abfd6e6cb9c6b0e", "e57a418a183326d17cd117bcb81c2ddd49202b8195778f38a06b970c1058f16d"));
    private M3MaskBoundedCurrentRecipe() { }

    private static byte[] resource(String name) throws IOException {
        try (InputStream in = M3MaskBoundedCurrentRecipe.class.getResourceAsStream(name)) {
            if (in == null) throw new IllegalStateException("missing resource: " + name);
            return in.readAllBytes();
        }
    }
    private static String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static byte[] image(Spec spec, boolean generated) throws IOException {
        byte[] bytes = resource(spec.resource() + (generated ? ".after.txt" : ".before.txt"));
        if (!digest(bytes).equals(generated ? spec.newSha() : spec.oldSha())) {
            throw new IllegalStateException("template SHA drift: " + spec.path());
        }
        return bytes;
    }
    private static List<Pending> inventory(Path root, boolean requireGenerated) throws IOException {
        List<Pending> changes = new ArrayList<>();
        for (Spec spec : FILES) {
            Path target = root.resolve(spec.path()).normalize();
            if (!target.startsWith(root) || Files.isSymbolicLink(target)) {
                throw new IllegalStateException("unsafe receiver path " + spec.path());
            }
            byte[] actual = Files.readAllBytes(target);
            byte[] oldBytes = image(spec, false), next = image(spec, true);
            if (requireGenerated) {
                if (!Arrays.equals(actual, next)) {
                    throw new IllegalStateException("postimage drift: " + spec.path());
                }
            } else if (Arrays.equals(actual, oldBytes)) {
                changes.add(new Pending(target, oldBytes, next));
            } else if (!Arrays.equals(actual, next)) {
                throw new IllegalStateException("unadmitted preimage: " + spec.path());
            }
        }
        return changes;
    }
    private static void replay(Path root) throws IOException {
        // Preflight every source before the first serial FILE mutation.
        for (Pending change : inventory(root, false)) {
            Path tmp = Files.createTempFile(change.path().getParent(), "m3-replay-", ".tmp");
            try {
                Files.write(tmp, change.newBytes());
                if (!Arrays.equals(Files.readAllBytes(change.path()), change.oldBytes())) {
                    throw new IllegalStateException("source changed during replay");
                }
                Files.move(tmp, change.path(), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(tmp); }
        }
        inventory(root, true);
    }
    private static void selftest() throws IOException {
        Path scratch = Files.createTempDirectory("m3-mask-bounded-").toAbsolutePath().normalize();
        try {
            for (Spec spec : FILES) {
                Path target = scratch.resolve(spec.path());
                Files.createDirectories(target.getParent());
                Files.write(target, image(spec, false));
            }
            replay(scratch);
            inventory(scratch, true);
            replay(scratch);
            inventory(scratch, true);
            Path c = scratch.resolve(FILES.get(1).path());
            Files.writeString(c, "tampered native source");
            boolean checkRefused = false, replayRefused = false;
            try { inventory(scratch, true); }
            catch (IllegalStateException expected) { checkRefused = true; }
            try { replay(scratch); }
            catch (IllegalStateException expected) { replayRefused = true; }
            if (!checkRefused || !replayRefused || !Files.readString(c).equals("tampered native source")) {
                throw new AssertionError("refusal or fixed point violated");
            }
            System.out.println("M3_MASK_BOUNDED_RECIPE_PASS files=2 fixed_point=true refusals=2");
        } finally {
            try (Stream<Path> paths = Files.walk(scratch)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--selftest")) {
            selftest(); return;
        }
        if (args.length != 2 || (!args[0].equals("--apply") && !args[0].equals("--check"))) {
            throw new IllegalArgumentException("use --selftest or --apply|--check REPO_ROOT");
        }
        Path root = Path.of(args[1]).toAbsolutePath().normalize();
        if (args[0].equals("--apply")) replay(root);
        else inventory(root, true);
        System.out.println("M3_MASK_BOUNDED_RECIPE_PASS " + args[0]);
    }
}
