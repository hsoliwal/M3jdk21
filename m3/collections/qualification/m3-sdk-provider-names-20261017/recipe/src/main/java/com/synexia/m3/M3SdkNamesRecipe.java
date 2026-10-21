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

/** Canonical Maven source-sealed recipe for ABSENT SDK API names and indexed multivalue owners.
 * Existing owners and vendor namespaces are never rewritten. Fix templates then rerun.
 * Future replacement of existing Java ASTs requires a qualified OpenRewrite recipe. */
public final class M3SdkNamesRecipe {
    private record Output(String template, String target, String digest) { }
    private static final List<Output> OUTPUTS = List.of(
        new Output("M3CollectionSdkNames.java", "m3/collections/src/main/java/com/m3/collections/M3CollectionSdkNames.java", "0e8d7bf9235d4075552c104d650290534b3b814014bcdb664de89fbc804fdaee"),
        new Output("M3IndexedMultiValueMap.java", "m3/collections/src/main/java/com/m3/collections/M3IndexedMultiValueMap.java", "0acfd80aee31b670b2dc0fa104d43b068ada2bd37781738170efdc7560f6fa82"),
        new Output("M3CollectionSdkContractSelfTest.java", "m3/collections/src/test/java/com/m3/collections/M3CollectionSdkContractSelfTest.java", "7d15455b3253291c7756b8d5e3abf33426885ba66600a63aba4e26109cba257d"),
        new Output("M3_SDK_FAMILY_CATALOG.tsv", "m3/collections/src/main/resources/com/m3/collections/M3_SDK_FAMILY_CATALOG.tsv", "e986838120c2558a145c6b16942475c8f5ff885b166526d635ef9c10360d8313")
    );
    private M3SdkNamesRecipe() { }
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--selftest")) { selfTest(); return; }
        if (args.length != 2 || !(args[0].equals("--apply") || args[0].equals("--check"))) {
            throw new IllegalArgumentException("Usage: --selftest | --check|--apply <m3jdk-root>");
        }
        run(Path.of(args[1]), args[0].equals("--apply"));
    }
    private static byte[] template(Output output) throws IOException {
        try (InputStream in = M3SdkNamesRecipe.class.getResourceAsStream("/templates/" + output.template)) {
            if (in == null) throw new IOException("MISSING_TEMPLATE:" + output.template);
            byte[] bytes = in.readAllBytes();
            String got;
            try { got = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
            catch (NoSuchAlgorithmException failure) { throw new IOException("SHA256_UNAVAILABLE", failure); }
            if (!got.equals(output.digest)) throw new IOException("TEMPLATE_HASH_DRIFT:" + output.template);
            return bytes;
        }
    }
    public static void run(Path rootArg, boolean apply) throws IOException {
        Path root = rootArg.toAbsolutePath().normalize();
        if (!Files.isDirectory(root.resolve("m3/collections"))) throw new IOException("NOT_M3JDK21_ROOT");
        // All-or-nothing preflight for versioned source bytes and occupied paths.
        for (Output output : OUTPUTS) {
            byte[] bytes = template(output);
            Path target = root.resolve(output.target).normalize();
            if (!target.startsWith(root)) throw new IOException("PATH_ESCAPE");
            for (Path next = target; !next.equals(root); next = next.getParent()) {
                if (Files.isSymbolicLink(next)) throw new IOException("SYMLINK:" + next);
            }
            if (Files.exists(target)) {
                if (!Files.isRegularFile(target) || !Arrays.equals(bytes, Files.readAllBytes(target))) {
                    throw new IOException("SOURCE_DRIFT:" + output.target);
                }
            } else if (!apply) throw new IOException("SOURCE_MISSING:" + output.target);
        }
        if (apply) {
            for (Output output : OUTPUTS) {
                Path target = root.resolve(output.target).normalize();
                if (Files.exists(target)) continue;
                Files.createDirectories(target.getParent());
                Path temp = Files.createTempFile(target.getParent(), ".m3-sdk-source-", ".tmp");
                try { Files.write(temp, template(output));
                    Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
                } finally { Files.deleteIfExists(temp); }
            }
        }
        System.out.println("PASS M3SdkNamesRecipe " + (apply ? "applied" : "fixed-point") + " files=" + OUTPUTS.size());
    }
    private static void selfTest() throws Exception {
        Path temp = Files.createTempDirectory("m3-sdk-names-");
        try {
            Files.createDirectories(temp.resolve("m3/collections"));
            try { run(temp, false); throw new AssertionError("missing source accepted"); }
            catch (IOException expected) {
                if (!expected.getMessage().startsWith("SOURCE_MISSING")) throw expected;
            }
            run(temp, true); run(temp, true); run(temp, false);
            Path victim = temp.resolve(OUTPUTS.get(0).target());
            Files.writeString(victim, "intentional drift");
            try { run(temp, true); throw new AssertionError("drift overwritten"); }
            catch (IOException expected) {
                if (!expected.getMessage().startsWith("SOURCE_DRIFT")) throw expected;
            }
            if (!Files.readString(victim).equals("intentional drift")) throw new AssertionError("drift changed");
            System.out.println("PASS M3SdkNamesRecipeSelfTest missing/drift/fixed-point");
        } finally {
            try (Stream<Path> paths = Files.walk(temp)) {
                for (Path path : paths.sorted((a,b) -> b.getNameCount() - a.getNameCount()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
