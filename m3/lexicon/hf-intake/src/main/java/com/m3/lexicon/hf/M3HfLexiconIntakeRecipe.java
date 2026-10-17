// SPDX-License-Identifier: Apache-2.0
package com.m3.lexicon.hf;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Offline, bounded Maven-invocable recipe for an operator-approved Hugging Face snapshot.
 * Produces v2 records for the EXISTING Synexia M3LEX exporter. Owns no lexicon,
 * string interner, payload arena, or JNI handle. Never fetches data implicitly.
 */
public final class M3HfLexiconIntakeRecipe {
    private static final String SOURCE = "synexia-dictlang/shared/bridge-hf/src/main/java/"
            + "com/synexia/dictshared/hf/DatasetLexiconImport.java";
    private static final String TARGET = "M3StringFacts + LangDexCoordinate";
    private static final String HEADER = "source_id\tsource_path\tsource_kind\tlanguage_tag\t"
            + "record_id\tlexeme\tmapping_id\tmapping_name\ttranslation_profile\t"
            + "precompute_profile\tprecompute_payload";
    private static final String APPROVAL_HEADER = "dataset\trevision\tfile\tsha256\tlicense\t"
            + "attribution\trights_evidence\tlanguage\tkind\tword_column\t"
            + "frequency_column\thas_header\tmax_bytes\tmax_rows\tapproval";
    public static final int MAX_ROW_BYTES = 1024 * 1024;

    public enum Kind { WORDS, FREQUENCY_TSV, GRAMMAR_CONLLU }

    /** Admission is a reviewed operator assertion, NOT inferred from a Hub license tag. */
    public record Approval(String dataset, String revision, String file, String sha256,
            String license, String attribution, URI rightsEvidence, String language,
            Kind kind, int wordColumn, int frequencyColumn, boolean hasHeader, long maxBytes,
            long maxRows, String approval) {
        public Approval {
            if (!safe(dataset, 160) || !dataset.matches("[A-Za-z0-9_-]+/[A-Za-z0-9_.-]+")
                    || !safe(file, 512) || file.startsWith("/") || file.contains("..")
                    || file.contains("\\\\") || file.contains("//")
                    || !file.matches("[A-Za-z0-9_./-]+")
                    || revision == null || !revision.matches("[a-fA-F0-9]{40}")
                    || sha256 == null || !sha256.matches("[a-f0-9]{64}")
                    || !safe(license, 256) || license.equalsIgnoreCase("unknown")
                    || license.equalsIgnoreCase("other") || !safe(attribution, 1024)
                    || rightsEvidence == null || !"https".equals(rightsEvidence.getScheme())
                    || rightsEvidence.getHost() == null || !safe(language, 64)
                    || !language.matches("[A-Za-z]{2,3}([_-][A-Za-z0-9]{2,8})*")
                    || !"APPROVED".equals(approval) || kind == null
                    || wordColumn < 0 || wordColumn > 4096
                    || frequencyColumn < -1 || frequencyColumn > 4096
                    || (kind == Kind.FREQUENCY_TSV && frequencyColumn < 0)
                    || (kind == Kind.GRAMMAR_CONLLU && (wordColumn != 1 || frequencyColumn != -1))
                    || maxBytes < 1 || maxBytes > 1_073_741_824L
                    || maxRows < 1 || maxRows > 100_000_000L) {
                throw new IllegalArgumentException("unapproved or malformed source specification");
            }
        }

        /** A discoverable URL is not permission to redistribute the resource. */
        public URI pinnedUrl() {
            return URI.create("https://huggingface.co/datasets/" + dataset + "/resolve/"
                    + revision + "/" + file);
        }

        public static Approval read(Path path) throws IOException {
            if (Files.size(path) > 8192) throw new IOException("approval metadata too large");
            var lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            if (lines.size() != 2 || !APPROVAL_HEADER.equals(lines.get(0))) {
                throw new IOException("approval requires the exact header and one reviewed source");
            }
            String[] v = lines.get(1).split("\t", -1);
            if (v.length != 15) throw new IOException("approval column count");
            try {
                return new Approval(v[0], v[1], v[2], v[3], v[4], v[5],
                        URI.create(v[6]), v[7], Kind.valueOf(v[8]),
                        Integer.parseInt(v[9]), Integer.parseInt(v[10]), checkedBool(v[11]),
                        Long.parseLong(v[12]), Long.parseLong(v[13]), v[14]);
            } catch (IllegalArgumentException ex) {
                throw new IOException("invalid reviewed approval", ex);
            }
        }
    }

    public record Receipt(long bytes, long sourceLines, long exportedRows, String sha256) { }

    private static boolean checkedBool(String value) {
        if (!value.equals("true") && !value.equals("false")) {
            throw new IllegalArgumentException("invalid has_header boolean");
        }
        return Boolean.parseBoolean(value);
    }

    private M3HfLexiconIntakeRecipe() { }

    private static boolean safe(String text, int max) {
        return text != null && !text.isBlank() && text.length() <= max
                && text.chars().noneMatch(Character::isISOControl);
    }

    /** Fail closed: stage, validate all bytes, then link-create the immutable product. */
    public static Receipt apply(Approval approval, Path input, Path target) throws IOException {
        Objects.requireNonNull(approval);
        Objects.requireNonNull(input);
        Objects.requireNonNull(target);
        if (Files.exists(target)) throw new IOException("output already published");
        if (Files.size(input) > approval.maxBytes()) throw new IOException("source byte budget");
        Path parent = target.toAbsolutePath().getParent();
        if (parent == null || !Files.isDirectory(parent)) throw new IOException("output parent missing");
        Path staged = Files.createTempFile(parent, ".m3-hf-recipe-", ".tsv.tmp");
        long bytes = 0, lines = 0, rows = 0;
        MessageDigest hash = digest();
        try {
            try (InputStream file = new BufferedInputStream(Files.newInputStream(input));
                    DigestInputStream stream = new DigestInputStream(file, hash);
                    Writer writer = Files.newBufferedWriter(staged, StandardCharsets.UTF_8,
                            StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write(HEADER);
                writer.write('\n');
                ByteArrayOutputStream row = new ByteArrayOutputStream();
                int ch;
                while ((ch = stream.read()) != -1) {
                    if (++bytes > approval.maxBytes()) throw new IOException("source byte budget");
                    if (ch == '\n') {
                        if (++lines > approval.maxRows()) throw new IOException("source row budget");
                        if (emit(approval, lines, row.toByteArray(), writer)) rows++;
                        row.reset();
                    } else {
                        if (row.size() >= MAX_ROW_BYTES) throw new IOException("source row byte budget");
                        row.write(ch);
                    }
                }
                if (row.size() > 0) {
                    if (++lines > approval.maxRows()) throw new IOException("source row budget");
                    if (emit(approval, lines, row.toByteArray(), writer)) rows++;
                }
            }
            if (rows == 0) throw new IOException("no admissible lexical rows");
            String actual = HexFormat.of().formatHex(hash.digest());
            if (!actual.equals(approval.sha256())) throw new IOException("source sha256 mismatch");
            // A hard-link publish is create-only, including on a competing writer. A filesystem
            // that cannot link staging to output fails rather than overwriting a generation.
            Files.createLink(target, staged);
            return new Receipt(bytes, lines, rows, actual);
        } finally {
            Files.deleteIfExists(staged);
        }
    }

    private static boolean emit(Approval a, long line, byte[] original, Writer out) throws IOException {
        int length = original.length;
        if (length > 0 && original[length - 1] == '\r') length--;
        if (length == 0) return false;
        String text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(original, 0, length)).toString();
        if (text.startsWith("\ufeff")) throw new IOException("unexpected UTF-8 BOM");
        if (a.hasHeader() && line == 1) return false;
        String lexeme;
        Map<String, Object> facts = new TreeMap<>();
        switch (a.kind()) {
            case WORDS -> {
                if (a.wordColumn() != 0) throw new IOException("word list uses column zero");
                lexeme = text;
            }
            case FREQUENCY_TSV -> {
                String[] cols = text.split("\t", -1);
                lexeme = cell(cols, a.wordColumn());
                String frequency = cell(cols, a.frequencyColumn());
                try {
                    long value = Long.parseLong(frequency);
                    if (value < 0) throw new NumberFormatException("negative");
                    facts.put("frequency_count", value);
                } catch (NumberFormatException ex) {
                    throw new IOException("invalid frequency at source row " + line, ex);
                }
            }
            case GRAMMAR_CONLLU -> {
                if (text.startsWith("#")) return false;
                String[] cols = text.split("\t", -1);
                if (cols.length != 10) throw new IOException("invalid CoNLL-U row " + line);
                if (cols[0].contains("-") || cols[0].contains(".")) return false;
                try {
                    if (Long.parseLong(cols[0]) < 1) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    throw new IOException("invalid CoNLL-U token ID", ex);
                }
                lexeme = cols[1];
                facts.put("lemma", cols[2]);
                facts.put("upos", cols[3]);
                facts.put("xpos", cols[4]);
                facts.put("morphology", cols[5]);
                facts.put("dependency", cols[7]);
            }
            default -> throw new AssertionError(a.kind());
        }
        if (lexeme.isEmpty() || lexeme.chars().anyMatch(c -> c < 32 || c == 127)) {
            throw new IOException("invalid lexical value at row " + line);
        }
        String identity = a.dataset() + "@" + a.revision() + "/" + a.file() + "#" + line;
        facts.put("attribution", a.attribution());
        facts.put("dataset", a.dataset());
        facts.put("license", a.license());
        facts.put("rights_evidence", a.rightsEvidence().toString());
        facts.put("source_revision", a.revision());
        facts.put("source_file", a.file());
        facts.put("raw_sha256", a.sha256());
        facts.put("source_row", line);
        facts.put("family", a.kind().name());
        String[] fields = {"dictlang.huggingface", SOURCE, "word", a.language(), identity,
                lexeme, identity, "HF_" + a.kind().name(), "-", TARGET, json(facts)};
        out.write(String.join("\t", fields));
        out.write('\n');
        return true;
    }

    private static String cell(String[] fields, int index) throws IOException {
        if (index >= fields.length) throw new IOException("missing field at index " + index);
        return fields[index];
    }

    /** Matches the exporter's compact sorted-key ASCII JSON canonicalization. */
    private static String json(Map<String, Object> fields) {
        StringBuilder value = new StringBuilder("{");
        for (var entry : fields.entrySet()) {
            if (value.length() > 1) value.append(',');
            quoted(value, entry.getKey());
            value.append(':');
            if (entry.getValue() instanceof Number n) value.append(n);
            else quoted(value, (String) entry.getValue());
        }
        return value.append('}').toString();
    }

    private static void quoted(StringBuilder b, String input) {
        b.append('"');
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' || c == '"') b.append('\\').append(c);
            else if (c == '\n') b.append("\\n");
            else if (c == '\r') b.append("\\r");
            else if (c == '\t') b.append("\\t");
            else if (c < 32 || c >= 127) {
                String hex = Integer.toHexString(c);
                b.append("\\u").append("0".repeat(4 - hex.length())).append(hex);
            } else b.append(c);
        }
        b.append('"');
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) throw new IllegalArgumentException(
                "usage: approval.tsv operator-supplied-snapshot output.records.tsv");
        Receipt result = apply(Approval.read(Path.of(args[0])), Path.of(args[1]), Path.of(args[2]));
        System.out.printf("M3_HF_INTAKE_PASS|rows=%d|source_lines=%d|bytes=%d|sha256=%s%n",
                result.exportedRows(), result.sourceLines(), result.bytes(), result.sha256());
    }
}
