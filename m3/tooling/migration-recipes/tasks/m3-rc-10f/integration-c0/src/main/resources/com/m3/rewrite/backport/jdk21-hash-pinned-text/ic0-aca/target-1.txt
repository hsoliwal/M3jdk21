// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Validates reference-only challenge evidence and license-bound GitHub algorithm lineage. */
public final class A3Alg {

    public enum Reuse {
        JDK_OWNED,
        ADAPT_PERMISSIVE,
        REFERENCE_ONLY
    }

    public record Row(
            String atomId,
            String category,
            String technique,
            String disposition,
            String scope,
            String targetOwner,
            String leetcodeRef,
            String hackerrankRef,
            String geeksforgeeksRef,
            String githubRef,
            String githubLicense,
            Reuse reuse,
            String nextProof) {

        public Row(
                String atomId,
                String category,
                String technique,
                String disposition,
                String scope,
                String targetOwner,
                String leetcodeRef,
                String hackerrankRef,
                String geeksforgeeksRef,
                String githubRef,
                String githubLicense,
                Reuse reuse,
                String nextProof) {
            this.atomId = text(atomId, "atomId");
            this.category = text(category, "category");
            this.technique = text(technique, "technique");
            this.disposition = text(disposition, "disposition");
            this.scope = text(scope, "scope");
            this.targetOwner = ref(targetOwner, "targetOwner");
            this.leetcodeRef = ref(leetcodeRef, "leetcodeRef");
            this.hackerrankRef = ref(hackerrankRef, "hackerrankRef");
            this.geeksforgeeksRef = ref(geeksforgeeksRef, "geeksforgeeksRef");
            this.githubRef = ref(githubRef, "githubRef");
            this.githubLicense = ref(githubLicense, "githubLicense");
            this.reuse = Objects.requireNonNull(reuse, "reuse");
            this.nextProof = text(nextProof, "nextProof");
            validate(this);
        }

        public boolean triPlatform() {
            return present(leetcodeRef)
                    && present(hackerrankRef)
                    && present(geeksforgeeksRef);
        }

        public boolean challengeSourceCopyAuthority() {
            return false;
        }

        public boolean implementationLineageAvailable() {
            return reuse == Reuse.JDK_OWNED
                    || reuse == Reuse.ADAPT_PERMISSIVE;
        }
    }

    private A3Alg() {
    }

    public static List<Row> load(Path root) throws IOException {
        Path path =
                A3Fs.root(root)
                        .resolve("m3/backports/ALGORITHM_CATALOGUE.tsv");
        if (!Files.isRegularFile(path)) {
            throw new IOException("missing A3 algorithm catalogue: " + path);
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()
                || !lines.getFirst()
                        .startsWith("atom_id\tcategory\ttechnique\tdisposition\t")) {
            throw new IOException("invalid A3 algorithm catalogue header");
        }

        ArrayList<Row> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            if (fields.length != 13) {
                throw new IOException(
                        "malformed A3 algorithm row " + (index + 1));
            }
            rows.add(
                    new Row(
                            fields[0],
                            fields[1],
                            fields[2],
                            fields[3],
                            fields[4],
                            fields[5],
                            fields[6],
                            fields[7],
                            fields[8],
                            fields[9],
                            fields[10],
                            reuse(fields[11]),
                            fields[12]));
        }
        rows.sort(Comparator.comparing(Row::atomId));
        requireUnique(rows);
        return List.copyOf(rows);
    }

    public static void write(Path root, Path out) throws IOException {
        StringBuilder tsv =
                new StringBuilder(
                        "atom_id\tcategory\ttechnique\tdisposition\tscope\t"
                                + "target_owner\ttri_platform\timplementation_lineage\t"
                                + "challenge_source_copy_authority\tnext_proof\n");
        for (Row row : load(root)) {
            tsv.append(A3Fs.cell(row.atomId()))
                    .append('\t')
                    .append(A3Fs.cell(row.category()))
                    .append('\t')
                    .append(A3Fs.cell(row.technique()))
                    .append('\t')
                    .append(A3Fs.cell(row.disposition()))
                    .append('\t')
                    .append(A3Fs.cell(row.scope()))
                    .append('\t')
                    .append(A3Fs.cell(row.targetOwner()))
                    .append('\t')
                    .append(row.triPlatform())
                    .append('\t')
                    .append(row.implementationLineageAvailable())
                    .append('\t')
                    .append(row.challengeSourceCopyAuthority())
                    .append('\t')
                    .append(A3Fs.cell(row.nextProof()))
                    .append('\n');
        }
        A3Fs.write(root, out, tsv.toString());
    }

    private static void validate(Row row) {
        boolean strict = strictDisposition(row.disposition());
        if (strict && !row.triPlatform()) {
            throw new IllegalArgumentException(
                    "strict A3 algorithm row lacks tri-platform evidence: "
                            + row.atomId());
        }
        requireHttpsOrNone(row.leetcodeRef(), "leetcodeRef");
        requireHttpsOrNone(row.hackerrankRef(), "hackerrankRef");
        requireHttpsOrNone(row.geeksforgeeksRef(), "geeksforgeeksRef");
        requireHttpsOrNone(row.githubRef(), "githubRef");

        boolean github = present(row.githubRef());
        boolean license = present(row.githubLicense());
        if (github != license) {
            throw new IllegalArgumentException(
                    "GitHub reference/license pair drift: " + row.atomId());
        }
        if (row.reuse() == Reuse.JDK_OWNED
                && (!github
                        || !row.githubRef().equals("https://github.com/openjdk/jdk")
                        || !present(row.targetOwner()))) {
            throw new IllegalArgumentException(
                    "JDK-owned algorithm lineage is not canonical: "
                            + row.atomId());
        }
        if (row.reuse() == Reuse.ADAPT_PERMISSIVE
                && (!github || !permissive(row.githubLicense()))) {
            throw new IllegalArgumentException(
                    "permissive adaptation lacks reviewed license: "
                            + row.atomId());
        }
        if (row.reuse() == Reuse.REFERENCE_ONLY
                && row.disposition().equals("candidate")) {
            throw new IllegalArgumentException(
                    "reference-only evidence cannot directly admit implementation: "
                            + row.atomId());
        }
    }

    private static boolean strictDisposition(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.equals("admitted")
                || normalized.equals("candidate")
                || normalized.equals("candidate-adapted");
    }

    private static boolean permissive(String license) {
        String value = license.toUpperCase(Locale.ROOT);
        return value.equals("APACHE-2.0")
                || value.equals("MIT")
                || value.equals("BSD-2-CLAUSE")
                || value.equals("BSD-3-CLAUSE");
    }

    private static Reuse reuse(String value) {
        try {
            return Reuse.valueOf(text(value, "reusePolicy"));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(
                    "unknown A3 algorithm reuse policy: " + value, failure);
        }
    }

    private static void requireUnique(List<Row> rows) {
        String previous = null;
        for (Row row : rows) {
            if (row.atomId().equals(previous)) {
                throw new IllegalArgumentException(
                        "duplicate A3 algorithm atom: " + row.atomId());
            }
            previous = row.atomId();
        }
    }

    private static void requireHttpsOrNone(String value, String field) {
        if (present(value) && !value.startsWith("https://")) {
            throw new IllegalArgumentException(field);
        }
    }

    private static boolean present(String value) {
        return !"NONE".equals(value);
    }

    private static String ref(String value, String field) {
        String checked = text(value, field);
        if (checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String text(String value, String field) {
        String checked =
                Objects.toString(value, "")
                        .strip()
                        .replace('\t', ' ')
                        .replace('\r', ' ')
                        .replace('\n', ' ');
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
