/*
 * Copyright 2026 Hitesh Soliwal and contributors.
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
import java.util.stream.Stream;

/** Exact-preimage Maven source recipe: one existing FILE and one new differential oracle. */
public final class M3LinkedListLiveMaskRecipe {
    public static final String TARGET = "m3/collections/src/main/java/com/m3/collections/M3LinkedList.java";
    public static final String ORACLE =
            "m3/collections/src/test/java/com/m3/collections/M3LinkedListLiveMaskContract.java";
    public static final String BEFORE = "31efffe0d25132b393178bec38906e5b977d8939a246a59115afeccb6a23e11b";
    public static final String AFTER = "1f9a2315db0f9a695402b47a7f6f1e124ed08e186e3460f9ce9757b324bd43c0";
    public static final String TEST = "56253c26a1f4e13c7c314642f328ea754d8135e9839606f9a71a55eecbf75919";
    private M3LinkedListLiveMaskRecipe() { }
    private static byte[] template(String name, String expected) throws IOException {
        try (InputStream in = M3LinkedListLiveMaskRecipe.class.getResourceAsStream(name)) {
            if (in == null) throw new IllegalStateException("missing sealed template: " + name);
            byte[] bytes = in.readAllBytes();
            if (!hash(bytes).equals(expected)) throw new IllegalStateException("template drift: " + name);
            return bytes;
        }
    }
    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static void check(Path root) throws IOException {
        Path runtime = root.resolve(TARGET), test = root.resolve(ORACLE);
        if (Files.isSymbolicLink(runtime) || Files.isSymbolicLink(test)) {
            throw new IllegalStateException("symlink receiver refused");
        }
        if (!Arrays.equals(Files.readAllBytes(runtime), template("M3LinkedList.java.after.txt", AFTER))
                || !Arrays.equals(Files.readAllBytes(test), template("M3LinkedListLiveMaskContract.java.txt", TEST))) {
            throw new IllegalStateException("unqualified receiver postimage");
        }
    }
    private static void apply(Path root) throws IOException {
        Path runtime = root.resolve(TARGET), test = root.resolve(ORACLE);
        if (!runtime.normalize().startsWith(root) || !test.normalize().startsWith(root)
                || Files.isSymbolicLink(runtime) || Files.isSymbolicLink(test)) {
            throw new IllegalStateException("unqualified output path");
        }
        byte[] before = template("M3LinkedList.java.before.txt", BEFORE);
        byte[] after = template("M3LinkedList.java.after.txt", AFTER);
        byte[] expectedTest = template("M3LinkedListLiveMaskContract.java.txt", TEST);
        byte[] actual = Files.readAllBytes(runtime);
        if (!Arrays.equals(actual, before) && !Arrays.equals(actual, after)) {
            throw new IllegalStateException("unknown M3LinkedList preimage");
        }
        if (Files.exists(test)) {
            if (!Arrays.equals(Files.readAllBytes(test), expectedTest)) {
                throw new IllegalStateException("oracle occupied with unapproved contents");
            }
        }
        if (Arrays.equals(actual, after) && Files.exists(test)) return;
        if (Arrays.equals(actual, before)) {
            Path tmp = Files.createTempFile(runtime.getParent(), "m3-list-mask-", ".tmp");
            try {
                Files.write(tmp, after);
                if (!Arrays.equals(Files.readAllBytes(runtime), before)) {
                    throw new IllegalStateException("source modified during recipe");
                }
                Files.move(tmp, runtime, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(tmp); }
        }
        if (!Files.exists(test)) Files.write(test, expectedTest);
        check(root);
    }
    private static void selftest() throws IOException {
        Path root = Files.createTempDirectory("m3-list-mask-").toAbsolutePath().normalize();
        try {
            Path runtime = root.resolve(TARGET), test = root.resolve(ORACLE);
            Files.createDirectories(runtime.getParent());
            Files.createDirectories(test.getParent());
            Files.write(runtime, template("M3LinkedList.java.before.txt", BEFORE));
            apply(root);
            check(root);
            apply(root); // fixed point
            check(root);
            Files.writeString(runtime, "tamper");
            boolean blocked = false;
            try { apply(root); } catch (IllegalStateException expected) { blocked = true; }
            if (!blocked) throw new AssertionError("source drift not refused");
            Files.write(runtime, template("M3LinkedList.java.before.txt", BEFORE));
            Files.writeString(test, "occupied");
            blocked = false;
            try { apply(root); } catch (IllegalStateException expected) { blocked = true; }
            if (!blocked) throw new AssertionError("occupied oracle not refused");
            System.out.println("M3_LINKED_LIST_MASK_RECIPE_PASS fixed_point=1 refusals=2");
        } finally {
            try (Stream<Path> all = Files.walk(root)) {
                for (Path path : all.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--selftest")) { selftest(); return; }
        if (args.length != 2 || (!args[0].equals("--apply") && !args[0].equals("--check"))) {
            throw new IllegalArgumentException("usage: --selftest | --apply|--check REPO_ROOT");
        }
        Path root = Path.of(args[1]).toAbsolutePath().normalize();
        if (args[0].equals("--apply")) apply(root); else check(root);
        System.out.println("M3_LINKED_LIST_MASK_RECIPE_PASS mode=" + args[0]);
    }
}
