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

/** Source-sealed Maven direct-provider recipe. Original algorithms stay Synexia-owned. */
public final class M3SpringDirect20261017Recipe {
    private record Source(String target, String file, String pre, String post) { }
    private record Change(Path path, byte[] pre, byte[] post) { }
    private static final List<Source> OUTPUTS = List.of(
        new Source("m3/collections/interop/src/main/java/com/m3/collections/interop/M3SpringArrayMultiValueMap.java", "M3SpringArrayMultiValueMap.java.txt", "ABSENT", "8062247a92ee9dc90d313b715ba9158e47c53b3e7848508a5fdd54ddaaaecb96"),
        new Source("m3/collections/interop/src/main/java/com/m3/collections/interop/M3LibraryCollections.java", "M3LibraryCollections.java.after.txt", "76609b94a23a682d98d541b5a0421f8c0bdfce05dce6f9cb253fa2e6bf89f62b", "832bb1e12699a8b023329fb68bb60f4a75bb1bd91344344763b3500a0f8747e6"),
        new Source("m3/collections/interop/sdk-family-bindings.tsv", "sdk-family-bindings.tsv.after.txt", "3eec888606c333effe39255423d1f25e2c4276716e8be357c0667e1fedb7fbd5", "56e074a4570969bec955f277e89b29e37ba8c0e661f6e529ea4c4846b8b71325"),
        new Source("m3/collections/interop/src/test/java/com/m3/collections/interop/M3SpringArrayContractTest.java", "M3SpringArrayContractTest.java.txt", "ABSENT", "a8b537381b64899e263eb481a95a83ae40a9b7ed8109fe401f2329c750419267"));
    private M3SpringDirect20261017Recipe() { }
    private static String sha(byte[] contents) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contents)); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static byte[] source(String file, String expected) throws IOException {
        try (InputStream in = M3SpringDirect20261017Recipe.class.getResourceAsStream(file)) {
            if (in == null) throw new IllegalStateException("missing canonical template " + file);
            byte[] content = in.readAllBytes();
            if (!sha(content).equals(expected)) throw new IllegalStateException("template drift " + file);
            return content;
        }
    }
    private static List<Change> inventory(Path root, boolean mustBeInstalled) throws IOException {
        List<Change> changes = new ArrayList<>();
        for (Source target : OUTPUTS) {
            Path path = root.resolve(target.target()).normalize();
            if (!path.startsWith(root) || Files.isSymbolicLink(path)) {
                throw new IllegalStateException("unsafe receiver " + target.target());
            }
            byte[] generated = source(target.file(), target.post());
            if (!Files.exists(path)) {
                if (!target.pre().equals("ABSENT") || mustBeInstalled) {
                    throw new IllegalStateException("missing expected pre/post source " + target.target());
                }
                changes.add(new Change(path, null, generated));
                continue;
            }
            byte[] actual = Files.readAllBytes(path);
            if (Arrays.equals(actual, generated)) continue;
            if (!mustBeInstalled && !target.pre().equals("ABSENT")
                    && sha(actual).equals(target.pre())) {
                changes.add(new Change(path, actual, generated));
            } else throw new IllegalStateException("unqualified source drift " + target.target());
        }
        return changes;
    }
    private static void apply(Path root) throws IOException {
        // All existing and ABSENT-before targets must pass admission BEFORE the first edit.
        for (Change change : inventory(root, false)) {
            Path path = change.path();
            if (change.pre() == null) {
                Files.createDirectories(path.getParent());
                Files.write(path, change.post(), java.nio.file.StandardOpenOption.CREATE_NEW);
            } else {
                Path tmp = Files.createTempFile(path.getParent(), "m3-spring-", ".tmp");
                try {
                    Files.write(tmp, change.post());
                    if (!Arrays.equals(Files.readAllBytes(path), change.pre())) {
                        throw new IllegalStateException("concurrent source change");
                    }
                    Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } finally { Files.deleteIfExists(tmp); }
            }
        }
        inventory(root, true);
    }
    private static void selftest() throws IOException {
        Path root = Files.createTempDirectory("m3-spring-direct-").toAbsolutePath().normalize();
        try {
            for (Source output : OUTPUTS) {
                Path target = root.resolve(output.target());
                Files.createDirectories(target.getParent());
                if (!output.pre().equals("ABSENT")) {
                    byte[] data = source("before/" + output.file(), output.pre());
                    Files.write(target, data);
                }
            }
            apply(root); inventory(root, true);
            apply(root); inventory(root, true);
            Path newSource = root.resolve(OUTPUTS.get(0).target());
            Files.writeString(newSource, "occupied");
            boolean refused = false;
            try { apply(root); } catch (IllegalStateException expected) { refused = true; }
            if (!refused) throw new AssertionError("occupied source accepted");
            Files.delete(newSource);
            Path oldSource = root.resolve(OUTPUTS.get(1).target());
            Files.writeString(oldSource, "unknown preimage");
            refused = false;
            try { apply(root); } catch (IllegalStateException expected) { refused = true; }
            if (!refused) throw new AssertionError("unknown preimage accepted");
            System.out.println("M3_SPRING_DIRECT_RECIPE_PASS targets=4 fixed_point=true refusals=2");
        } finally {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--selftest")) { selftest(); return; }
        if (args.length != 2 || !(args[0].equals("--apply") || args[0].equals("--check"))) {
            throw new IllegalArgumentException("--selftest or --apply|--check REPOSITORY_ROOT");
        }
        Path root = Path.of(args[1]).toAbsolutePath().normalize();
        if (args[0].equals("--apply")) apply(root); else inventory(root, true);
        System.out.println("M3_SPRING_DIRECT_RECIPE_PASS mode=" + args[0]);
    }
}
