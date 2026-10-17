/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Source-sealed Maven transformation recipe for additive M3JDK21 Java owners.
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
 * Installs only the two sealed, previously absent M3 state/index manipulation
 * files. Does not modify existing OpenJDK or M3 Java production classes; if
 * modifying attributed Java ASTs becomes necessary, use an OpenRewrite recipe
 * rather than broad text search/replacement.
 */
public final class M3StateMaskArraysRecipe {
    private record Output(String resource, String target, String sha256) { }

    private static final List<Output> OUTPUTS = List.of(
        new Output("M3StateMaskArrays.java",
            "m3/collections/src/main/java/com/m3/collections/M3StateMaskArrays.java",
            "38df0ab511c2cdb43f957c16878da572484495f2d94ce851ad8342ca526b08a1"),
        new Output("M3StateMaskArraysSelfTest.java",
            "m3/collections/src/test/java/com/m3/collections/M3StateMaskArraysSelfTest.java",
            "7608a36e5f06d2c1595bc8f251a9734270e68eb7573de84277fb459d790821b0"));

    private M3StateMaskArraysRecipe() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--selftest".equals(args[0])) {
            selfTest();
            return;
        }
        if (args.length != 2 || !("--apply".equals(args[0]) || "--check".equals(args[0]))) {
            throw new IllegalArgumentException("Usage: --selftest | --check|--apply <M3JDK21-root>");
        }
        run(Path.of(args[1]), "--apply".equals(args[0]));
    }

    public static void run(Path repository, boolean apply) throws IOException {
        Path root = repository.toAbsolutePath().normalize();
        if (!Files.isDirectory(root.resolve("m3/collections"))) {
            throw new IOException("REFUSE_NOT_COLLECTIONS_RECEIVER");
        }
        // Preflight all templates and all destinations before any writing.
        for (Output output : OUTPUTS) {
            byte[] expected = template(output);
            Path target = root.resolve(output.target()).normalize();
            if (!target.startsWith(root)) throw new IOException("REFUSE_ESCAPING_DESTINATION");
            for (Path p = target; !p.equals(root); p = p.getParent()) {
                if (Files.isSymbolicLink(p)) throw new IOException("REFUSE_SYMLINK:" + p);
            }
            if (Files.exists(target)) {
                if (!Files.isRegularFile(target) || !Arrays.equals(expected, Files.readAllBytes(target))) {
                    throw new IOException("REFUSE_DRIFT:" + output.target());
                }
            } else if (!apply) {
                throw new IOException("REFUSE_MISSING:" + output.target());
            }
        }
        if (apply) {
            for (Output output : OUTPUTS) {
                Path target = root.resolve(output.target()).normalize();
                if (Files.exists(target)) continue;
                Files.createDirectories(target.getParent());
                Path temporary = Files.createTempFile(target.getParent(), ".m3-state-mask-", ".tmp");
                try {
                    Files.write(temporary, template(output));
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    Files.deleteIfExists(temporary);
                }
            }
        }
        System.out.println("PASS M3StateMaskArraysRecipe "
                + (apply ? "apply" : "check") + " paths=" + OUTPUTS.size());
    }

    private static byte[] template(Output output) throws IOException {
        try (InputStream stream = M3StateMaskArraysRecipe.class.getResourceAsStream(
                "/templates/" + output.resource())) {
            if (stream == null) throw new IOException("REFUSE_MISSING_TEMPLATE:" + output.resource());
            byte[] bytes = stream.readAllBytes();
            try {
                String digest = HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(bytes));
                if (!digest.equals(output.sha256())) {
                    throw new IOException("REFUSE_TEMPLATE_HASH:" + output.resource());
                }
            } catch (NoSuchAlgorithmException unavailable) {
                throw new IOException("SHA_256_UNAVAILABLE", unavailable);
            }
            return bytes;
        }
    }

    private static void selfTest() throws IOException {
        Path root = Files.createTempDirectory("m3-state-mask-recipe-");
        try {
            Files.createDirectories(root.resolve("m3/collections"));
            expectRefusal("REFUSE_MISSING", () -> run(root, false));
            run(root, true);
            run(root, false);
            run(root, true); // fixed point, no output changed
            Path owner = root.resolve(OUTPUTS.get(0).target());
            Path proof = root.resolve(OUTPUTS.get(1).target());
            Files.writeString(proof, "unexpected target state");
            expectRefusal("REFUSE_DRIFT", () -> run(root, true));
            if (!Arrays.equals(Files.readAllBytes(owner), template(OUTPUTS.get(0)))) {
                throw new AssertionError("drift admission changed owner");
            }
            Files.delete(owner);
            expectRefusal("REFUSE_DRIFT", () -> run(root, true));
            if (Files.exists(owner)) throw new AssertionError("partial write before preflight");
            Files.write(proof, template(OUTPUTS.get(1)));
            Files.createSymbolicLink(owner, proof);
            expectRefusal("REFUSE_SYMLINK", () -> run(root, true));
            Files.delete(owner);
            run(root, true);
            run(root, false);
            System.out.println("PASS M3StateMaskArraysRecipe selftest "
                    + "missing/drift/all-preflight/symlink/idempotence");
        } finally {
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    private interface Attempt { void run() throws IOException; }

    private static void expectRefusal(String prefix, Attempt attempt) throws IOException {
        try {
            attempt.run();
        } catch (IOException failure) {
            if (failure.getMessage().startsWith(prefix)) return;
            throw failure;
        }
        throw new AssertionError("expected refusal " + prefix);
    }
}
