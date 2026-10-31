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

/** Source-hash-sealed Maven generator for an additive M3 indexed-bitmap algebra atom.
 * OpenRewrite is required for future changes to existing Java AST owners; this
 * recipe installs new files only and refuses existing, differing contents. */
public final class M3LongBitmapFilterPlanRecipe {
    private record Output(String file, String target, String sha256) { }
    private static final List<Output> OUTPUTS = List.of(
        new Output("M3LongBitmapFilterPlan.java",
                "m3/collections/src/main/java/com/m3/collections/M3LongBitmapFilterPlan.java",
                "bd7969cfc42e26171b9904c65eb12f3321b2fbffe77a34f6d76f4db15c11c1d3"),
        new Output("M3LongBitmapFilterPlanSelfTest.java",
                "m3/collections/src/test/java/com/m3/collections/M3LongBitmapFilterPlanSelfTest.java",
                "fa87bbf51b5a58b6b1562b909c3c16d0b670168165db57e6bb4430158de11fd7"));

    private M3LongBitmapFilterPlanRecipe() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--selftest".equals(args[0])) {
            selfTest(); return;
        }
        if (args.length != 2 || !("--apply".equals(args[0]) || "--check".equals(args[0]))) {
            throw new IllegalArgumentException("Usage: --selftest | --check|--apply <target-repo-root>");
        }
        run(Path.of(args[1]), "--apply".equals(args[0]));
    }

    public static void run(Path targetRoot, boolean apply) throws IOException {
        Path root = targetRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root.resolve("m3/collections"))) {
            throw new IOException("REFUSE_NOT_M3JDK21_RECEIVER");
        }
        // Source-check every item before touching the receiver: no partial drift repair.
        for (Output output : OUTPUTS) {
            byte[] expected = template(output);
            Path target = root.resolve(output.target()).normalize();
            if (!target.startsWith(root)) throw new IOException("REFUSE_TRAVERSAL");
            for (Path scan = target; !scan.equals(root); scan = scan.getParent()) {
                if (Files.isSymbolicLink(scan)) throw new IOException("REFUSE_SYMLINK:" + scan);
            }
            if (Files.exists(target)) {
                if (!Files.isRegularFile(target) || !Arrays.equals(Files.readAllBytes(target), expected)) {
                    throw new IOException("REFUSE_DRIFT:" + output.target());
                }
            } else if (!apply) throw new IOException("REFUSE_MISSING:" + output.target());
        }
        if (apply) {
            for (Output output : OUTPUTS) {
                byte[] bytes = template(output);
                Path dest = root.resolve(output.target());
                if (Files.exists(dest)) continue;
                Files.createDirectories(dest.getParent());
                Path temp = Files.createTempFile(dest.getParent(), ".m3-bits-", ".tmp");
                try {
                    Files.write(temp, bytes);
                    Files.move(temp, dest, StandardCopyOption.ATOMIC_MOVE);
                } finally { Files.deleteIfExists(temp); }
            }
        }
        System.out.println("PASS M3LongBitmapFilterPlanRecipe mode="
                + (apply ? "apply" : "check") + " files=" + OUTPUTS.size());
    }

    private static byte[] template(Output output) throws IOException {
        try (InputStream stream = M3LongBitmapFilterPlanRecipe.class.getResourceAsStream(
                "/templates/" + output.file())) {
            if (stream == null) throw new IOException("REFUSE_MISSING_TEMPLATE:" + output.file());
            byte[] bytes = stream.readAllBytes();
            String hash;
            try {
                hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            } catch (NoSuchAlgorithmException problem) {
                throw new IOException("REFUSE_SHA256_UNAVAILABLE", problem);
            }
            if (!hash.equals(output.sha256())) {
                throw new IOException("REFUSE_TEMPLATE_HASH:" + output.file());
            }
            return bytes;
        }
    }

    private static void selfTest() throws Exception {
        Path root = Files.createTempDirectory("m3-long-bitmap-algebra-");
        try {
            Files.createDirectories(root.resolve("m3/collections"));
            try { run(root, false); throw new AssertionError("missing file admitted"); }
            catch (IOException e) { if (!e.getMessage().startsWith("REFUSE_MISSING")) throw e; }
            run(root, true);
            run(root, true); // no drift on second generation
            run(root, false); // exact fixed point
            Path first = root.resolve(OUTPUTS.get(0).target());
            byte[] known = Files.readAllBytes(first);
            Files.writeString(first, "changed generated production");
            try { run(root, true); throw new AssertionError("drift overwritten"); }
            catch (IOException e) { if (!e.getMessage().startsWith("REFUSE_DRIFT")) throw e; }
            if (!"changed generated production".equals(Files.readString(first))) {
                throw new AssertionError("drifted source overwritten");
            }
            Files.write(first, known);
            run(root, false);
            System.out.println("PASS M3LongBitmapFilterPlanRecipeSelfTest fixed-point/missing/drift");
        } finally {
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path p : paths.sorted((a,b) -> b.getNameCount() - a.getNameCount()).toList()) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }
}
