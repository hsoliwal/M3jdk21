/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.repository;

import jdk.m3.tooling.atoms.contract.AtomCoverageLedger;
import jdk.m3.tooling.atoms.contract.CoverageGate;
import jdk.m3.tooling.atoms.contract.GateStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Dependency-free executable proof for the committed-tree repository file census. */
public final class RepositoryFileCensusSelfTest {
    private static int checks;

    private RepositoryFileCensusSelfTest() {
    }

    /** Runs all dependency-free checks. */
    public static void run() throws Exception {
        checks = 0;
        Path temporary = Files.createTempDirectory("m3-repository-file-census-");
        try {
            Path repository = temporary.resolve("repository");
            Files.createDirectories(repository);
            git(repository, "init", "-q");
            git(repository, "config", "user.name", "M3 Census Test");
            git(repository, "config", "user.email", "m3-census@example.invalid");
            git(repository, "config", "core.autocrlf", "false");

            String sourcePath = "src/java.base/share/classes/sample/Example.java";
            String stableAtomId = "m3:file:reviewed-example";
            byte[] javaBytes = "package sample; final class Example { static int value() { return 1; } }\n"
                    .getBytes(StandardCharsets.UTF_8);
            byte[] binaryBytes = new byte[] {0, 1, 2, 3, 10, 13, 127, (byte) 0xff};
            write(repository, RepositoryFileCensus.CONTRACT_PATH,
                    "# M3JDK21\n\n## Repository-complete invariant\nEvery tracked file has a FILE_CONTAINER.\n"
                            .getBytes(StandardCharsets.UTF_8));
            write(repository, sourcePath, javaBytes);
            write(repository, FileAtomIdentities.PATH,
                    (FileAtomIdentities.HEADER + "\n" + sourcePath + "\t" + stableAtomId + "\n")
                            .getBytes(StandardCharsets.UTF_8));
            write(repository, "make/Makefile", "all:\n\t@true\n".getBytes(StandardCharsets.UTF_8));
            write(repository, "docs/space name.md", "documented path\n".getBytes(StandardCharsets.UTF_8));
            write(repository, "resources/data.bin", binaryBytes);
            commit(repository, "fixture");

            Path first = temporary.resolve("first");
            Path second = temporary.resolve("second");
            RepositoryFileCensus.Result firstResult = RepositoryFileCensus.generate(repository, "HEAD", first);
            RepositoryFileCensus.Result secondResult = RepositoryFileCensus.generate(repository, "HEAD", second);
            String exactCommit = git(repository, "rev-parse", "HEAD");
            Path exact = temporary.resolve("exact");
            RepositoryFileCensus.generate(repository, exactCommit, exact);
            check(firstResult.fileCount() == 6, "six tracked files are inventoried");
            check(firstResult.evidenceCount() == 66, "every file has eleven common evidence rows");
            check(firstResult.semanticSha256().equals(secondResult.semanticSha256()),
                    "independent runs have one semantic identity");
            for (String name : List.of("FILES.tsv", "ATOMS.tsv", "EVIDENCE.tsv",
                    "REPOSITORY_CENSUS_RECEIPT.tsv")) {
                check(Arrays.equals(Files.readAllBytes(first.resolve(name)), Files.readAllBytes(second.resolve(name))),
                        name + " is byte-identical across repeated runs");
                check(Arrays.equals(Files.readAllBytes(first.resolve(name)), Files.readAllBytes(exact.resolve(name))),
                        name + " is independent of the revision alias");
            }

            AtomCoverageLedger ledger = AtomCoverageLedger.read(first.resolve("ATOMS.tsv"),
                    first.resolve("EVIDENCE.tsv"));
            check(ledger.atoms().size() == 6, "strict ledger reader accepts all file atoms");
            check(ledger.evidenceCount() == 66, "strict ledger reader accepts all evidence rows");
            check(ledger.admittedAtomCount() == 0, "unexecuted proof cannot be admitted");
            check(ledger.admissionFindings().size() == 42,
                    "seven explicit pending gates remain visible for each file");

            Map<String, String[]> files = rowsByColumn(first.resolve("FILES.tsv"), 0);
            check(files.get(sourcePath)[6].equals("JAVA"), "Java source family is deterministic");
            check(files.get("make/Makefile")[6].equals("BUILD"), "Makefile source family is deterministic");
            check(files.get("resources/data.bin")[6].equals("RESOURCE"),
                    "known binary resource family is deterministic");
            check(files.get("resources/data.bin")[5].equals(sha256(binaryBytes)),
                    "blob SHA-256 uses exact committed binary bytes");
            check(files.get(sourcePath)[7].equals("module:java.base"),
                    "module owner is derived mechanically");
            check(files.get(sourcePath)[8].equals(stableAtomId),
                    "reviewed identity override replaces the path-derived identity");

            String evidenceText = Files.readString(first.resolve("EVIDENCE.tsv"), StandardCharsets.UTF_8);
            check(count(evidenceText, "\tCONTRACT_BOUND\tPASS\t") == 6,
                    "global repository contract is hash-bound for every file atom");
            check(count(evidenceText, "\tLICENSE_VERIFIED\tNOT_RUN\t-\t-\t") == 6,
                    "licence verification is never inferred from inventory");

            write(repository, sourcePath,
                    "package sample; final class Example { static int value() { return 99; } }\n"
                            .getBytes(StandardCharsets.UTF_8));
            Path dirty = temporary.resolve("dirty");
            RepositoryFileCensus.generate(repository, "HEAD", dirty);
            check(Arrays.equals(Files.readAllBytes(first.resolve("FILES.tsv")),
                    Files.readAllBytes(dirty.resolve("FILES.tsv"))),
                    "uncommitted working-tree drift cannot alter the census");

            commit(repository, "change source");
            Path changed = temporary.resolve("changed");
            RepositoryFileCensus.generate(repository, "HEAD", changed);
            Map<String, String[]> firstAtoms = rowsByColumn(first.resolve("ATOMS.tsv"), 1);
            Map<String, String[]> changedAtoms = rowsByColumn(changed.resolve("ATOMS.tsv"), 1);
            check(firstAtoms.get(sourcePath)[0].equals(changedAtoms.get(sourcePath)[0]),
                    "file atom identity survives content changes");
            check(!firstAtoms.get(sourcePath)[5].equals(changedAtoms.get(sourcePath)[5]),
                    "source identity changes when committed bytes change");

            String movedPath = "src/java.base/share/classes/sample/MovedExample.java";
            Files.move(repository.resolve(sourcePath), repository.resolve(movedPath));
            write(repository, FileAtomIdentities.PATH,
                    (FileAtomIdentities.HEADER + "\n" + movedPath + "\t" + stableAtomId + "\n")
                            .getBytes(StandardCharsets.UTF_8));
            commit(repository, "reviewed move");
            Path moved = temporary.resolve("moved");
            RepositoryFileCensus.generate(repository, "HEAD", moved);
            Map<String, String[]> movedAtoms = rowsByColumn(moved.resolve("ATOMS.tsv"), 1);
            check(!movedAtoms.containsKey(sourcePath), "old path disappears after the committed move");
            check(movedAtoms.get(movedPath)[0].equals(stableAtomId),
                    "reviewed move preserves the stable atom identity");

            expectFailure(() -> new GitTreeEntry("100644", "blob", "0".repeat(40), "bad\tpath", 0,
                    "0".repeat(64)), "ledger-incompatible Git paths fail closed");
            check(SourceFamily.classify("src/hotspot/cpu/x86/x86_64.ad") == SourceFamily.HOTSPOT_AD,
                    "HotSpot architecture description is classified");
            check(SourceFamily.classify("src/hotspot/cpu/x86/stubGenerator_x86_64.cpp") == SourceFamily.CPP,
                    "C++ source is classified");
            check(SourceFamily.classify("configure") == SourceFamily.BUILD,
                    "configure is classified as build source");
            check(SourceFamily.classify("src/java.desktop/macosx/native/libawt/AppDelegate.m")
                            == SourceFamily.C,
                    "Objective-C source is dispatched with C source");
            check(SourceFamily.classify("test/jdk/tools/jpackage/helpers/main.js")
                            == SourceFamily.SCRIPT,
                    "JavaScript test source is classified as script source");
        } finally {
            deleteRecursively(temporary);
        }
    }

    /** Runs the proof as a standalone Java 21 program. */
    public static void main(String[] args) throws Exception {
        run();
        System.out.println("M3_REPOSITORY_FILE_CENSUS_SELF_TEST\tchecks=" + checks + "\tPASS");
    }

    private static void write(Path repository, String path, byte[] bytes) throws IOException {
        Path target = repository.resolve(path);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
    }

    private static void commit(Path repository, String message) throws IOException {
        git(repository, "add", "-A");
        git(repository, "commit", "-q", "-m", message);
    }

    private static String git(Path repository, String... arguments) throws IOException {
        String[] command = new String[arguments.length + 3];
        command[0] = System.getProperty("m3.git.executable", "git");
        command[1] = "-C";
        command[2] = repository.toString();
        System.arraycopy(arguments, 0, command, 3, arguments.length);
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().put("LC_ALL", "C");
        builder.environment().put("LANG", "C");
        Process process = builder.start();
        byte[] output = process.getInputStream().readAllBytes();
        int exit;
        try {
            exit = process.waitFor();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while running Git", exception);
        }
        if (exit != 0) {
            throw new IOException("Git failed (" + exit + "): " + String.join(" ", command) + "\n"
                    + new String(output, StandardCharsets.UTF_8));
        }
        return new String(output, StandardCharsets.UTF_8).strip();
    }

    private static Map<String, String[]> rowsByColumn(Path path, int keyColumn) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        Map<String, String[]> rows = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.split("\t", -1);
            rows.put(cells[keyColumn], cells);
        }
        return rows;
    }

    private static int count(String text, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is required by the JDK", exception);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    private static void expectFailure(ThrowingRunnable runnable, String message) throws Exception {
        try {
            runnable.run();
            throw new AssertionError(message + ": expected failure");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted((left, right) -> right.getNameCount() - left.getNameCount()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
