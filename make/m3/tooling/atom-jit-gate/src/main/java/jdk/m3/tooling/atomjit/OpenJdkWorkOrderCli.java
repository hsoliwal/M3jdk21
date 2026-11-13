// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atomjit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Consumes the existing complete Git-tree inventory without mutating M3JDK21. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-openrewrite-recipes/crates/m3jdk21-openjdk-atom-pattern-20261031/src/main/java/com/synexia/m3/openjdk/M3OpenJdkWorkOrderCli.java blob 4a92ec275e4d; recipe make/m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
public final class OpenJdkWorkOrderCli {
    private OpenJdkWorkOrderCli() { }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "usage: <owner/name> <sealed SOURCE_WORK_QUEUE.tsv> <sha256> <new output file>");
        }
        Path source = Path.of(args[1]).toAbsolutePath().normalize();
        Path destination = Path.of(args[3]).toAbsolutePath().normalize();
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)
                || source.equals(destination)
                || !args[2].matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("missing hash seal or output already exists");
        }
        if (Files.isSymbolicLink(source) || !Files.isRegularFile(source)) {
            throw new IllegalArgumentException("missing or symlinked source inventory");
        }
        byte[] sourceSnapshot = Files.readAllBytes(source);
        if (!args[2].equals(sha256(sourceSnapshot))) {
            throw new IllegalArgumentException("whole-estate Git source inventory drift");
        }
        List<OpenJdkAtomPatternGate.Order> orders =
                OpenJdkAtomPatternGate.readSnapshot(args[0], sourceSnapshot);
        StringBuilder out = new StringBuilder(
                "repository\towner_path\tgit_blob_sha1\tlane\tstage_order\t" +
                "atomizer\tpre_pattern_proof\tpatternizer\tpost_pattern_proof\tstatus\n");
        orders.forEach(order -> out.append(order.tsv()).append('\n'));
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, out.toString(), StandardOpenOption.CREATE_NEW);
        System.out.println("SOURCE_WORK_ORDERS=" + orders.size() +
                " PATTERNIZER_HELD_UNTIL_EXECUTED_JIT_JUNIT_JTREG_NATIVE_PROOFS");
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException missing) {
            throw new AssertionError(missing);
        }
    }
}
