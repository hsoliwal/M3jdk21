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
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

/**
 * Maven-bound exact-source recipe for three additive Java classes. The existing
 * M3 five-family indexed owner is untouched; existing-file Java AST edits require
 * the incumbent OpenRewrite hash-pinned visitor, not this additive installer.
 * Original donor package identities and licenses are not transformed here.
 */
public final class M3FiveForkPreparedRecipe {
    private record Output(String filename, String path, String sha256) { }
    private static final String MAIN =
            "m3/collections/interop/src/main/java/com/m3/collections/interop/";
    private static final String TEST =
            "m3/collections/interop/src/test/java/com/m3/collections/interop/";
    private static final List<Output> OUTPUTS = List.of(
            new Output("M3FiveFamilyPreparedSelection.java", MAIN + "M3FiveFamilyPreparedSelection.java",
                    "8b9ba8edf03de5ccce15b1be104ba846b53a5e39fcda15bdb22a741d9575db04"),
            new Output("M3FiveFamilyPreparedSelectionSelfTest.java", TEST + "M3FiveFamilyPreparedSelectionSelfTest.java",
                    "9ceb788e6c6cdca0596879eff680cb6fb4591663a7379f868d4f1b0afd5bbd52"),
            new Output("M3FiveFamilyPreparedSelectionTest.java", TEST + "M3FiveFamilyPreparedSelectionTest.java",
                    "2113f5a2dc439f59e653127900c01ac55ae547d05a93436ed46c7ca269220bf7"));

    private M3FiveForkPreparedRecipe() { }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--selftest".equals(args[0])) { selfTest(); return; }
        if (args.length != 2 || !("--apply".equals(args[0]) || "--check".equals(args[0]))) {
            throw new IllegalArgumentException("--selftest | --apply|--check target-root");
        }
        run(Path.of(args[1]), "--apply".equals(args[0]));
    }

    public static void run(Path candidate, boolean apply) throws IOException {
        Path root = candidate.toAbsolutePath().normalize();
        if (!Files.isDirectory(root.resolve("m3/collections/interop"))) {
            throw new IOException("REFUSE_WRONG_RECEIVER_ROOT");
        }
        // All paths, byte identities, and symlink boundaries are preflighted before writes.
        for (Output item : OUTPUTS) {
            byte[] expected = template(item);
            Path target = root.resolve(item.path()).normalize();
            if (!target.startsWith(root)) throw new IOException("REFUSE_PATH_ESCAPE");
            for (Path scan = target; !scan.equals(root); scan = scan.getParent()) {
                if (Files.isSymbolicLink(scan)) throw new IOException("REFUSE_SYMLINK:" + scan);
            }
            if (Files.exists(target)) {
                if (!Files.isRegularFile(target) || !Arrays.equals(Files.readAllBytes(target), expected)) {
                    throw new IOException("REFUSE_SOURCE_DRIFT:" + item.path());
                }
            } else if (!apply) throw new IOException("REFUSE_MISSING:" + item.path());
        }
        if (apply) {
            for (Output item : OUTPUTS) {
                Path target = root.resolve(item.path());
                if (Files.exists(target)) continue;
                Files.createDirectories(target.getParent());
                Path temp = Files.createTempFile(target.getParent(), ".m3-five-forks-", ".tmp");
                try {
                    Files.write(temp, template(item));
                    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
                } finally { Files.deleteIfExists(temp); }
            }
        }
        System.out.println("PASS M3FiveForkPreparedRecipe " + (apply ? "apply" : "check")
                + " outputs=" + OUTPUTS.size());
    }

    private static byte[] template(Output output) throws IOException {
        try (InputStream stream = M3FiveForkPreparedRecipe.class.getResourceAsStream(
                "/templates/" + output.filename())) {
            if (stream == null) throw new IOException("REFUSE_MISSING_TEMPLATE:" + output.filename());
            byte[] data = stream.readAllBytes();
            String actual;
            try { actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
            catch (NoSuchAlgorithmException failure) { throw new IOException("SHA256_UNAVAILABLE", failure); }
            if (!actual.equals(output.sha256())) throw new IOException("REFUSE_TEMPLATE_HASH:" + output.filename());
            return data;
        }
    }

    private static void selfTest() throws IOException {
        Path root = Files.createTempDirectory("m3-five-forks-recipe-");
        try {
            Files.createDirectories(root.resolve("m3/collections/interop"));
            try { run(root, false); throw new AssertionError("missing files permitted"); }
            catch (IOException valid) {
                if (!valid.getMessage().startsWith("REFUSE_MISSING:")) throw valid;
            }
            run(root, true);
            run(root, true);
            run(root, false);
            Path first = root.resolve(OUTPUTS.get(0).path());
            byte[] original = Files.readAllBytes(first);
            Files.writeString(first, "modified output");
            try { run(root, true); throw new AssertionError("generated output rewritten"); }
            catch (IOException valid) {
                if (!valid.getMessage().startsWith("REFUSE_SOURCE_DRIFT:")) throw valid;
            }
            if (!"modified output".equals(Files.readString(first))) {
                throw new AssertionError("modified output was overwritten");
            }
            Files.write(first, original);
            run(root, false);
            System.out.println("PASS M3FiveForkPreparedRecipeSelfTest fixed-point/missing/drift");
        } finally {
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path entry : paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                    Files.deleteIfExists(entry);
                }
            }
        }
    }
}
