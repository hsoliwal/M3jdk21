// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only product-side custodian for a frozen M3JDK21 open-PR census.
 * This is source verification inside the one integrated M3JDK21 SDK, not a
 * dependency on com.synexia or an independent Git merge implementation.
 */
public final class M3PrFleetSourceProof {
    private static final String DIR = "m3/qualification/chronological-prfleet-20261021/";
    private static final String PRODUCT = "hsoliwal/M3jdk21";
    private static final String SNAPSHOT_SHA = "7f657d6ca05760af7bee7d4d2d9e14ae91ddd942";

    private M3PrFleetSourceProof() { }

    private static void require(boolean value, String message) throws IOException {
        if (!value) throw new IOException("M3_PRFLEET_HOLD: " + message);
    }

    private static List<String> data(Path path) throws IOException {
        return Files.readAllLines(path, StandardCharsets.UTF_8).stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#")
                        && !line.startsWith("repository\t") && !line.startsWith("source_path\t"))
                .toList();
    }

    private static String gitBlob(Path path) throws IOException {
        byte[] content = Files.readAllBytes(path);
        byte[] header = ("blob " + content.length + "\0")
                .getBytes(StandardCharsets.US_ASCII);
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-1");
            hash.update(header);
            return HexFormat.of().formatHex(hash.digest(content));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public static String verify(Path repository) throws IOException {
        Path root = repository.toRealPath();
        Path dir = root.resolve(DIR);
        require(Files.isDirectory(dir), "missing exact SDK receipt directory");
        Map<String, String[]> originalHeads = new HashMap<>();
        String previousCreated = "";
        for (String line : data(dir.resolve("M3JDK21_PR_HEADS.tsv"))) {
            String[] cells = line.split("\t", -1);
            require(cells.length == 11 && PRODUCT.equals(cells[0]), "malformed product PR row");
            require(cells[2].compareTo(previousCreated) >= 0, "nonchronological source");
            previousCreated = cells[2];
            require(cells[6].matches("[a-f0-9]{40}")
                    && cells[8].matches("[a-f0-9]{40}"), "PR source SHA");
            require(originalHeads.putIfAbsent(cells[1], cells) == null, "duplicate PR");
        }
        require(originalHeads.size() == 101, "frozen PR cardinality changed");
        Set<String> reviewed = new HashSet<>();
        for (String line : data(dir.resolve("M3JDK21_MERGE_GATES.tsv"))) {
            String[] row = line.split("\t", -1);
            require(row.length == 13 && PRODUCT.equals(row[0]), "malformed HOLD row");
            String[] original = originalHeads.get(row[1]);
            require(original != null && reviewed.add(row[1]), "missing/duplicate PR HOLD");
            require(original[8].equals(row[7]) && original[6].equals(row[5]),
                    "PR head/base identity drift");
            require(row[10].startsWith("HOLD_"), "unqualified merge authorization");
            try {
                require(Integer.parseInt(row[2]) > 0, "missing DAG review wave");
            } catch (NumberFormatException invalid) {
                throw new IOException("M3_PRFLEET_HOLD: invalid review wave", invalid);
            }
        }
        require(reviewed.size() == originalHeads.size(), "unreviewed original head");
        Set<String> owned = new HashSet<>();
        for (String line : data(dir.resolve("M3JDK21_SOURCE_PINNED_OWNERS.tsv"))) {
            String[] cells = line.split("\t", -1);
            require(cells.length == 5 && SNAPSHOT_SHA.equals(cells[2]),
                    "unsealed product source owner");
            Path relative = Path.of(cells[0]);
            require(!relative.isAbsolute() && !cells[0].contains("..")
                    && cells[0].startsWith("m3/collections/"), "unsafe native/Java path");
            Path file = root.resolve(relative).normalize();
            require(file.startsWith(root) && Files.isRegularFile(file)
                    && !Files.isSymbolicLink(file), "unreadable/symlinked owner");
            require(cells[3].matches("[a-f0-9]{40}")
                    && cells[3].equals(gitBlob(file)), "original source Git blob drift");
            require(owned.add(cells[0]), "duplicate original Java/JNI owner");
        }
        require(owned.size() == 6, "missing Java/JNI owners");
        return "M3_PRFLEET_PRODUCT_SOURCE_PASS heads=" + reviewed.size()
                + " pinnedOwners=" + owned.size();
    }

    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("exact repository root required");
        }
        System.out.println(verify(Path.of(arguments[0])));
    }
}
