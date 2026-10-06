// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Generates authority-free FILE atom evidence by comparing one pinned upstream OpenJDK checkout
 * with the current M3JDK21 tree.
 *
 * <p>This generator does not adapt code and never writes into either source tree. It copies exact
 * upstream bytes into an external evidence directory so each path can be reviewed, adapted and
 * promoted through a later hash-pinned OpenRewrite recipe. Java/text/binary classification is
 * mechanical only and grants no compatibility, copy-to-target, execution or promotion authority.</p>
 */
public final class M3JepFileAtomCandidateGenerator {
    public static final String HEADER =
            "ordinal\tpath\tkind\tcurrent_state\tupstream_state\tcurrent_sha256"
                    + "\tupstream_sha256\trelation\tpostimage_path\tsource_mutation_authority"
                    + "\tpromotion_authority\trow_root";

    private M3JepFileAtomCandidateGenerator() {}

    public static Report generate(
            Path currentRoot,
            Path upstreamRoot,
            List<String> paths,
            Path outputRoot) {
        Path current = absoluteDirectory(currentRoot, "currentRoot");
        Path upstream = absoluteDirectory(upstreamRoot, "upstreamRoot");
        Path output = Objects.requireNonNull(outputRoot, "outputRoot").toAbsolutePath().normalize();
        List<String> checkedPaths = canonicalPaths(paths);

        ArrayList<Row> rows = new ArrayList<>(checkedPaths.size());
        for (int ordinal = 0; ordinal < checkedPaths.size(); ordinal++) {
            String path = checkedPaths.get(ordinal);
            Path upstreamFile = resolveInside(upstream, path);
            if (!Files.isRegularFile(upstreamFile)) {
                throw new IllegalArgumentException("upstream file missing: " + path);
            }
            Path currentFile = resolveInside(current, path);
            if (Files.exists(currentFile) && !Files.isRegularFile(currentFile)) {
                throw new IllegalArgumentException("current path is not a regular file: " + path);
            }

            byte[] upstreamBytes = read(upstreamFile);
            byte[] currentBytes = Files.isRegularFile(currentFile) ? read(currentFile) : null;
            String upstreamHash = sha256(upstreamBytes);
            String currentHash = currentBytes == null ? "" : sha256(currentBytes);
            String relation =
                    currentBytes == null
                            ? "ADDITIVE"
                            : java.util.Arrays.equals(currentBytes, upstreamBytes)
                                    ? "ALREADY_EQUIVALENT"
                                    : "CHANGED";
            String kind = kind(path, upstreamBytes);
            String postimage = "postimages/" + path;
            String rowRoot =
                    root(
                            ordinal,
                            path,
                            kind,
                            currentBytes == null ? "ABSENT" : "PRESENT",
                            "PRESENT",
                            currentHash,
                            upstreamHash,
                            relation,
                            postimage);
            rows.add(
                    new Row(
                            ordinal,
                            path,
                            kind,
                            currentBytes == null ? "ABSENT" : "PRESENT",
                            "PRESENT",
                            currentHash,
                            upstreamHash,
                            relation,
                            postimage,
                            false,
                            false,
                            rowRoot));
        }

        String tsv = tsv(rows);
        String reportRoot = sha256(tsv.getBytes(StandardCharsets.UTF_8));
        writeEvidence(output, rows, upstream, tsv, reportRoot);
        return new Report(rows, reportRoot);
    }

    public static List<String> parsePaths(String text) {
        return canonicalPaths(
                Objects.requireNonNull(text, "text").lines()
                        .map(String::strip)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .toList());
    }

    public record Report(List<Row> rows, String root) {
        public Report {
            rows = List.copyOf(Objects.requireNonNull(rows, "rows"));
            root = hash(root, "root");
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("empty file-atom report");
            }
        }

        public String tsv() {
            return M3JepFileAtomCandidateGenerator.tsv(rows);
        }
    }

    public record Row(
            int ordinal,
            String path,
            String kind,
            String currentState,
            String upstreamState,
            String currentSha256,
            String upstreamSha256,
            String relation,
            String postimagePath,
            boolean sourceMutationAuthority,
            boolean promotionAuthority,
            String rowRoot) {
        public Row {
            if (ordinal < 0) throw new IllegalArgumentException("ordinal");
            path = canonicalPath(path);
            kind = oneOf(kind, "JAVA", "TEXT", "BINARY_HOLD");
            currentState = oneOf(currentState, "PRESENT", "ABSENT");
            upstreamState = oneOf(upstreamState, "PRESENT");
            currentSha256 =
                    currentState.equals("ABSENT")
                            ? empty(currentSha256, "currentSha256")
                            : hash(currentSha256, "currentSha256");
            upstreamSha256 = hash(upstreamSha256, "upstreamSha256");
            relation = oneOf(relation, "ADDITIVE", "ALREADY_EQUIVALENT", "CHANGED");
            postimagePath = canonicalPath(postimagePath);
            if (!postimagePath.equals("postimages/" + path)) {
                throw new IllegalArgumentException("postimage path mismatch");
            }
            if (sourceMutationAuthority || promotionAuthority) {
                throw new IllegalArgumentException("file atom candidate may not carry authority");
            }
            rowRoot = hash(rowRoot, "rowRoot");
            if (currentState.equals("ABSENT") != relation.equals("ADDITIVE")) {
                throw new IllegalArgumentException("current state/relation mismatch");
            }
        }
    }

    private static String tsv(List<Row> rows) {
        StringBuilder out = new StringBuilder(HEADER).append('\n');
        for (Row row : rows) {
            out.append(row.ordinal()).append('\t')
                    .append(row.path()).append('\t')
                    .append(row.kind()).append('\t')
                    .append(row.currentState()).append('\t')
                    .append(row.upstreamState()).append('\t')
                    .append(row.currentSha256()).append('\t')
                    .append(row.upstreamSha256()).append('\t')
                    .append(row.relation()).append('\t')
                    .append(row.postimagePath()).append('\t')
                    .append(row.sourceMutationAuthority()).append('\t')
                    .append(row.promotionAuthority()).append('\t')
                    .append(row.rowRoot()).append('\n');
        }
        return out.toString();
    }

    private static void writeEvidence(
            Path output,
            List<Row> rows,
            Path upstreamRoot,
            String tsv,
            String root) {
        try {
            Files.createDirectories(output);
            for (Row row : rows) {
                Path target = resolveInside(output, row.postimagePath());
                Files.createDirectories(target.getParent());
                Files.write(target, read(resolveInside(upstreamRoot, row.path())));
            }
            Files.writeString(output.resolve("FILE_ATOMS.tsv"), tsv, StandardCharsets.UTF_8);
            Files.writeString(output.resolve("FILE_ATOMS.sha256"), root + "\n", StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot write JEP file-atom evidence", failure);
        }
    }

    private static List<String> canonicalPaths(List<String> paths) {
        List<String> values =
                List.copyOf(Objects.requireNonNull(paths, "paths")).stream()
                        .map(M3JepFileAtomCandidateGenerator::canonicalPath)
                        .toList();
        if (values.isEmpty()) throw new IllegalArgumentException("empty path list");
        for (int index = 1; index < values.size(); index++) {
            if (values.get(index - 1).compareTo(values.get(index)) >= 0) {
                throw new IllegalArgumentException("paths must be unique and lexicographically sorted");
            }
        }
        return values;
    }

    private static String canonicalPath(String path) {
        String value = Objects.requireNonNull(path, "path").strip().replace('\\', '/');
        if (value.isEmpty()
                || value.startsWith("/")
                || value.indexOf('\0') >= 0
                || value.indexOf('\t') >= 0
                || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("invalid relative path");
        }
        for (String part : value.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
                throw new IllegalArgumentException("noncanonical relative path: " + value);
            }
        }
        return value;
    }

    private static Path resolveInside(Path root, String relative) {
        Path resolved = root.resolve(canonicalPath(relative)).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("path escapes root: " + relative);
        }
        return resolved;
    }

    private static Path absoluteDirectory(Path path, String field) {
        Path value = Objects.requireNonNull(path, field).toAbsolutePath().normalize();
        if (!Files.isDirectory(value)) {
            throw new IllegalArgumentException(field + " is not a directory: " + value);
        }
        return value;
    }

    private static String kind(String path, byte[] bytes) {
        if (path.endsWith(".java")) return "JAVA";
        return utf8(bytes) ? "TEXT" : "BINARY_HOLD";
    }

    private static boolean utf8(byte[] bytes) {
        try {
            StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (java.nio.charset.CharacterCodingException invalid) {
            return false;
        }
    }

    private static String root(
            int ordinal,
            String path,
            String kind,
            String currentState,
            String upstreamState,
            String currentHash,
            String upstreamHash,
            String relation,
            String postimage) {
        StringBuilder value = new StringBuilder();
        append(value, "M3_JEP_FILE_ATOM_CANDIDATE_V1");
        append(value, Integer.toString(ordinal));
        append(value, path);
        append(value, kind);
        append(value, currentState);
        append(value, upstreamState);
        append(value, currentHash);
        append(value, upstreamHash);
        append(value, relation);
        append(value, postimage);
        append(value, "sourceMutationAuthority=false");
        append(value, "promotionAuthority=false");
        return sha256(value.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void append(StringBuilder target, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        target.append(bytes.length).append(':').append(value);
    }

    private static byte[] read(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException failure) {
            throw new IllegalArgumentException("cannot read " + path, failure);
        }
    }

    private static String oneOf(String value, String... allowed) {
        String checked = Objects.requireNonNull(value, "value");
        for (String candidate : allowed) {
            if (candidate.equals(checked)) return checked;
        }
        throw new IllegalArgumentException("unexpected value: " + checked);
    }

    private static String empty(String value, String field) {
        if (!Objects.requireNonNull(value, field).isEmpty()) {
            throw new IllegalArgumentException(field + " must be empty");
        }
        return "";
    }

    private static String hash(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }
}
