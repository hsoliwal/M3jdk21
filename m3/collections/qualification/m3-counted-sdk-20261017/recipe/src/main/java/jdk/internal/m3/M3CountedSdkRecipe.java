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
 * Maven-bound source-sealed additive receiver packet using the existing
 * Synexia hash-pinned template admission pattern. Production files are solely
 * the exact template postimages, with preflight and fixed-point refusal.
 * No modification or bulk rewrite of incumbent JDK/M3/interop sources occurs.
 */
public final class M3CountedSdkRecipe {
    private record Output(String template, String path, String sha256) { }
    private static final List<Output> FILES = List.of(
        new Output("M3CountedBag.java", "m3/collections/src/main/java/jdk/m3/collections/M3CountedBag.java",
                "583e6b5dce178ee02c2efdb919271c81a00ad60771abd06f7c942330fdfc60d1"),
        new Output("M3CountedBagSelfTest.java", "m3/collections/src/test/java/jdk/m3/collections/M3CountedBagSelfTest.java",
                "44461bcb78fc6f2ba8af6d7ecfb39c5a4c325307d43bfb1926ef2b49af8e6e22"),
        new Output("M3CountedBagTest.java", "m3/collections/src/test/java/jdk/m3/collections/M3CountedBagTest.java",
                "96e3c656bcb894747bc1c94dcfd98cb77debd1858858733b32921e2e5ec4eca9"),
        new Output("M3GuavaMultiset.java", "m3/collections/interop/src/main/java/jdk/m3/collections/interop/M3GuavaMultiset.java",
                "75f4a003ff6a85cc6e06a5dd9da1962a0460c263210741c95b44450926cb10a8"),
        new Output("M3ApacheMultiSet.java", "m3/collections/interop/src/main/java/jdk/m3/collections/interop/M3ApacheMultiSet.java",
                "b13748791f45ba66fcbda8bb336bee778fe9a354de1b8e17bf90cd256ce2e861"),
        new Output("M3CountedProviderContractTest.java", "m3/collections/interop/src/test/java/jdk/m3/collections/interop/M3CountedProviderContractTest.java",
                "9d33e518ee8e2722bfb4672d41bca9cd8bd362fd9787c16f50c5131ab671463b"));

    private M3CountedSdkRecipe() { }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--selftest".equals(args[0])) { selfTest(); return; }
        if (args.length != 2 || !("--apply".equals(args[0]) || "--check".equals(args[0]))) {
            throw new IllegalArgumentException("Use --selftest or --check|--apply <M3JDK21-root>");
        }
        run(Path.of(args[1]), "--apply".equals(args[0]));
    }
    public static void run(Path repo, boolean apply) throws IOException {
        Path root = repo.toAbsolutePath().normalize();
        if (!Files.isDirectory(root.resolve("m3/collections/interop"))) {
            throw new IOException("REFUSE_WRONG_TARGET_REPOSITORY");
        }
        // Verify all inputs and receiver preimages before the first output write.
        for (Output item : FILES) {
            byte[] expected = template(item);
            Path target = root.resolve(item.path()).normalize();
            if (!target.startsWith(root)) throw new IOException("REFUSE_PATH_ESCAPE:" + item.path());
            for (Path cursor = target; !cursor.equals(root); cursor = cursor.getParent()) {
                if (Files.isSymbolicLink(cursor)) throw new IOException("REFUSE_SYMLINK:" + cursor);
            }
            if (Files.exists(target)) {
                if (!Files.isRegularFile(target) || !Arrays.equals(Files.readAllBytes(target), expected)) {
                    throw new IOException("REFUSE_CHANGED_POSTIMAGE:" + item.path());
                }
            } else if (!apply) {
                throw new IOException("REFUSE_MISSING_POSTIMAGE:" + item.path());
            }
        }
        if (apply) for (Output item : FILES) {
            byte[] bytes = template(item);
            Path target = root.resolve(item.path());
            if (Files.exists(target)) continue;
            Files.createDirectories(target.getParent());
            Path pending = Files.createTempFile(target.getParent(), ".m3-counted-", ".tmp");
            try {
                Files.write(pending, bytes);
                Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE);
            } finally { Files.deleteIfExists(pending); }
        }
        System.out.println("PASS M3CountedSdkRecipe " + (apply ? "apply" : "check")
                + " sealedFiles=" + FILES.size());
    }
    private static byte[] template(Output item) throws IOException {
        try (InputStream stream = M3CountedSdkRecipe.class.getResourceAsStream("/templates/" + item.template())) {
            if (stream == null) throw new IOException("REFUSE_ABSENT_TEMPLATE:" + item.template());
            byte[] bytes = stream.readAllBytes();
            final String sha;
            try { sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
            catch (NoSuchAlgorithmException unavailable) { throw new IOException(unavailable); }
            if (!sha.equals(item.sha256())) throw new IOException("REFUSE_TEMPLATE_DRIFT:" + item.template());
            return bytes;
        }
    }
    private static void selfTest() throws Exception {
        Path temp = Files.createTempDirectory("m3-counted-sdk-recipe-");
        try {
            Files.createDirectories(temp.resolve("m3/collections/interop"));
            try { run(temp, false); throw new AssertionError("missing admitted"); }
            catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_MISSING_POSTIMAGE")) throw expected;
            }
            run(temp, true);
            run(temp, true);
            run(temp, false);
            Path changed = temp.resolve(FILES.get(0).path());
            Files.writeString(changed, "tampered");
            try { run(temp, true); throw new AssertionError("drift overwritten"); }
            catch (IOException expected) {
                if (!expected.getMessage().startsWith("REFUSE_CHANGED_POSTIMAGE")) throw expected;
            }
            if (!"tampered".equals(Files.readString(changed))) throw new AssertionError("output overwrite");
            Files.write(changed, template(FILES.get(0)));
            run(temp, false);
            System.out.println("PASS M3CountedSdkRecipe selftest fixed-point/drift/missing");
        } finally {
            try (Stream<Path> paths = Files.walk(temp)) {
                for (Path p : paths.sorted((a,b) -> b.getNameCount()-a.getNameCount()).toList()) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }
}
