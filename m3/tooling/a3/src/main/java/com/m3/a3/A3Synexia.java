// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Exact Apache-2.0 Synexia convergence/recipe donor snapshot admitted into the A3 tool plane.
 *
 * <p>The snapshot is review and adaptation input only. It can be copied into the M3 Apache-2.0
 * tooling subtree because every admitted row is pinned to the reviewed Synexia revision and carries
 * an Apache-2.0 license. It never grants authority to mutate OpenJDK product sources or promote a
 * candidate.</p>
 */
public final class A3Synexia {
    public static final String REPOSITORY = "hsoliwal/com.synexia";
    public static final String REVISION = "ae955ea9b7e246d780269525b15fa38b95ffba67";
    public static final String LICENSE = "Apache-2.0";
    public static final String MANIFEST =
            "m3/tooling/a3/synexia/SOURCE_MANIFEST.tsv";
    private static final String LOCAL_PREFIX =
            "m3/tooling/a3/synexia/upstream/";

    public enum Kind {
        LICENSE,
        PUBLIC_POLISH,
        CONVERGENCE,
        DONOR,
        RECIPE,
        MASTERY
    }

    public record Entry(
            int ordinal,
            String module,
            String sourcePath,
            String gitBlob,
            String license,
            Kind kind,
            String localPath) {
        public Entry {
            if (ordinal < 1) {
                throw new IllegalArgumentException("ordinal");
            }
            module = text(module, "module");
            sourcePath = path(sourcePath, "sourcePath");
            gitBlob = gitBlob(gitBlob, "gitBlob");
            license = text(license, "license");
            if (!LICENSE.equals(license)) {
                throw new IllegalArgumentException("unapproved Synexia license: " + license);
            }
            kind = Objects.requireNonNull(kind, "kind");
            localPath = path(localPath, "localPath");
            if (!localPath.startsWith(LOCAL_PREFIX)) {
                throw new IllegalArgumentException("Synexia local path is outside A3 snapshot");
            }
        }
    }

    public record Report(
            String repository,
            String revision,
            int entries,
            int recipeEntries,
            int masteryEntries,
            int publicPolishEntries,
            String root) {
        public Report {
            repository = text(repository, "repository");
            revision = gitBlob(revision, "revision");
            if (entries < 1
                    || recipeEntries < 1
                    || masteryEntries < 1
                    || publicPolishEntries < 1) {
                throw new IllegalArgumentException("incomplete Synexia donor report");
            }
            root = sha(root, "root");
        }

        public boolean toolingSourceBorrowAuthority() {
            return true;
        }

        public boolean productSourceMutationAuthority() {
            return false;
        }

        public boolean promotionAuthority() {
            return false;
        }
    }

    private A3Synexia() {}

    public static List<Entry> load(Path root) throws IOException {
        Path checkedRoot = A3Fs.root(root);
        Path manifest = A3Fs.source(checkedRoot, Path.of(MANIFEST));
        if (!Files.isRegularFile(manifest)) {
            throw new IOException("missing A3 Synexia manifest: " + manifest);
        }
        List<String> lines = Files.readAllLines(manifest, StandardCharsets.UTF_8);
        if (lines.isEmpty()
                || !"ordinal\tmodule\tsource_path\tgit_blob\tlicense\tkind\tlocal_path"
                        .equals(lines.getFirst())) {
            throw new IOException("invalid A3 Synexia manifest header");
        }

        ArrayList<Entry> entries = new ArrayList<>();
        int expectedOrdinal = 1;
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) {
                continue;
            }
            String[] cells = line.split("\t", -1);
            if (cells.length != 7) {
                throw new IOException("malformed A3 Synexia row " + (index + 1));
            }
            int ordinal;
            try {
                ordinal = Integer.parseInt(cells[0]);
            } catch (NumberFormatException failure) {
                throw new IOException("invalid A3 Synexia ordinal " + (index + 1), failure);
            }
            if (ordinal != expectedOrdinal++) {
                throw new IOException("non-contiguous A3 Synexia ordinal");
            }
            entries.add(
                    new Entry(
                            ordinal,
                            cells[1],
                            cells[2],
                            cells[3],
                            cells[4],
                            Kind.valueOf(cells[5]),
                            cells[6]));
        }
        entries.sort(Comparator.comparingInt(Entry::ordinal));
        requireUnique(entries);
        return List.copyOf(entries);
    }

    public static Report verify(Path root) throws IOException {
        Path checkedRoot = A3Fs.root(root);
        List<Entry> entries = load(checkedRoot);
        StringBuilder material =
                new StringBuilder("A3-SYNEXIA-BORROW/1\n")
                        .append(REPOSITORY)
                        .append('\n')
                        .append(REVISION)
                        .append('\n');

        int recipes = 0;
        int mastery = 0;
        int publicPolish = 0;
        for (Entry entry : entries) {
            Path local = A3Fs.source(checkedRoot, Path.of(entry.localPath()));
            if (!Files.isRegularFile(local)) {
                throw new IOException("missing A3 Synexia snapshot: " + entry.localPath());
            }
            byte[] bytes = Files.readAllBytes(local);
            String actual = gitBlob(bytes);
            if (!actual.equals(entry.gitBlob())) {
                throw new IOException(
                        "A3 Synexia snapshot drift: "
                                + entry.localPath()
                                + " expected="
                                + entry.gitBlob()
                                + " actual="
                                + actual);
            }
            if (entry.kind() == Kind.RECIPE) {
                recipes++;
            } else if (entry.kind() == Kind.MASTERY) {
                mastery++;
            } else if (entry.kind() == Kind.PUBLIC_POLISH) {
                publicPolish++;
            }
            material.append(entry.ordinal())
                    .append('\t')
                    .append(entry.module())
                    .append('\t')
                    .append(entry.sourcePath())
                    .append('\t')
                    .append(entry.gitBlob())
                    .append('\t')
                    .append(entry.kind())
                    .append('\n');
        }
        return new Report(
                REPOSITORY,
                REVISION,
                entries.size(),
                recipes,
                mastery,
                publicPolish,
                A3Fs.sha(material.toString()));
    }

    public static Report write(Path root, Path out) throws IOException {
        Path checkedRoot = A3Fs.root(root);
        Path checkedOut = A3Fs.out(checkedRoot, out);
        Report report = verify(checkedRoot);
        String text =
                "repository\trevision\tentries\trecipes\tmastery\tpublic_polish\troot"
                        + "\ttooling_source_borrow\tproduct_mutation\tpromotion\n"
                        + report.repository()
                        + "\t"
                        + report.revision()
                        + "\t"
                        + report.entries()
                        + "\t"
                        + report.recipeEntries()
                        + "\t"
                        + report.masteryEntries()
                        + "\t"
                        + report.publicPolishEntries()
                        + "\t"
                        + report.root()
                        + "\t"
                        + report.toolingSourceBorrowAuthority()
                        + "\t"
                        + report.productSourceMutationAuthority()
                        + "\t"
                        + report.promotionAuthority()
                        + "\n";
        A3Fs.write(checkedRoot, checkedOut.resolve("synexia.tsv"), text);
        return report;
    }

    private static void requireUnique(List<Entry> entries) {
        java.util.HashSet<String> source = new java.util.HashSet<>();
        java.util.HashSet<String> local = new java.util.HashSet<>();
        for (Entry entry : entries) {
            String sourceKey = entry.module() + "\u0000" + entry.sourcePath();
            if (!source.add(sourceKey) || !local.add(entry.localPath())) {
                throw new IllegalArgumentException("duplicate A3 Synexia snapshot row");
            }
        }
    }

    private static String gitBlob(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(
                    ("blob " + bytes.length + "\0").getBytes(StandardCharsets.UTF_8));
            digest.update(bytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static String gitBlob(String value, String field) {
        String checked = text(value, field).toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = text(value, field).toLowerCase(java.util.Locale.ROOT);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String path(String value, String field) {
        String checked = text(value, field).replace('\\', '/');
        if (checked.startsWith("/")) {
            throw new IllegalArgumentException(field);
        }
        for (String part : checked.split("/", -1)) {
            if (part.isBlank() || ".".equals(part) || "..".equals(part)) {
                throw new IllegalArgumentException(field);
            }
        }
        return checked;
    }

    private static String text(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
