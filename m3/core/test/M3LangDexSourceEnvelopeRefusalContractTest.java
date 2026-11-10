/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3LangDexSourceEnvelope;
import jdk.m3.text.M3LangDexSourceEnvelope.Receipt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Fail-closed grammar and receipt refusals of the LangDex source intake.
 *
 * <p>Every malformed wire is checked for the exact refusal reason, so each
 * refusal is proven to come from the intended grammar rule rather than from an
 * earlier, unrelated check.</p>
 */
public final class M3LangDexSourceEnvelopeRefusalContractTest {
    private static final String REVISION = "0123456789abcdef0123456789abcdef01234567";
    private static final String MAGIC = "#M3-LANGDEX-SOURCE-1";
    private static final String HEADER =
            "row_type\tlexeme_id\tglottocode_b64\tlemma_b64\tsource_b64\tconcept_id";
    private static final String LEXEME = "LEXEME\t7\tb64:" + b64("stan1293")
            + "\tb64:" + b64("water") + "\t~\t-";
    private static final String LINK = "CONCEPT_LINK\t7\t-\t-\t-\t1234";
    private static int checks;

    private M3LangDexSourceEnvelopeRefusalContractTest() { }

    private static String b64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private interface Action { void run() throws Exception; }

    private static void refused(Class<? extends Throwable> type, String reason, Action action) {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown) && (reason == null || reason.equals(thrown.getMessage()))) return;
            throw new AssertionError("expected " + type.getSimpleName() + " '" + reason + "'", thrown);
        }
        throw new AssertionError("accepted, expected refusal: " + reason);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static byte[] wire(String... rows) {
        StringBuilder out = new StringBuilder(MAGIC).append('\t').append(REVISION).append('\n')
                .append(HEADER).append('\n');
        for (String row : rows) out.append(row).append('\n');
        return out.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    /** Verify the wire against a receipt whose digest matches the bytes exactly. */
    private static Receipt verify(byte[] bytes, long lexemes, long links) throws Exception {
        return M3LangDexSourceEnvelope.verify(new ByteArrayInputStream(bytes),
                new Receipt(REVISION, lexemes, links, sha256(bytes)));
    }

    private static void rowRefused(String reason, long lexemes, long links, String... rows) {
        byte[] bytes = wire(rows);
        refused(IOException.class, reason, () -> verify(bytes, lexemes, links));
    }

    private static void receipts() {
        String sha = "a".repeat(64);
        Receipt accepted = new Receipt(REVISION, 0, 0, sha);
        check(accepted.lexemeRows() == 0 && accepted.sha256().equals(sha), "zero-row receipt");
        String revision = "sourceRevision must be a lowercase 40-digit Git SHA";
        refused(IllegalArgumentException.class, revision, () -> new Receipt(null, 0, 0, sha));
        refused(IllegalArgumentException.class, revision,
                () -> new Receipt(REVISION.toUpperCase(java.util.Locale.ROOT), 0, 0, sha));
        refused(IllegalArgumentException.class, revision, () -> new Receipt(REVISION + "0", 0, 0, sha));
        String counts = "row counts must be nonnegative";
        refused(IllegalArgumentException.class, counts, () -> new Receipt(REVISION, -1, 0, sha));
        refused(IllegalArgumentException.class, counts, () -> new Receipt(REVISION, 0, -1, sha));
        String digest = "sha256 must be lowercase 64-digit hex";
        refused(IllegalArgumentException.class, digest, () -> new Receipt(REVISION, 0, 0, null));
        refused(IllegalArgumentException.class, digest, () -> new Receipt(REVISION, 0, 0, "A".repeat(64)));
        refused(IllegalArgumentException.class, digest, () -> new Receipt(REVISION, 0, 0, "a".repeat(63)));
    }

    private static void acceptedWire() throws Exception {
        byte[] bytes = wire(LEXEME, LINK, "LEXEME\t~\t~\tb64:\tb64:" + b64("café") + "\t-",
                "CONCEPT_LINK\t-9223372036854775808\t-\t-\t-\t~");
        Receipt result = verify(bytes, 2, 2);
        check(result.lexemeRows() == 2 && result.conceptLinkRows() == 2, "counted rows");
        check(result.sha256().equals(sha256(bytes)), "returned receipt carries the wire digest");
        Receipt empty = verify(wire(), 0, 0);
        check(empty.lexemeRows() == 0 && empty.conceptLinkRows() == 0, "header-only wire");
        refused(NullPointerException.class, "input",
                () -> M3LangDexSourceEnvelope.verify(null, new Receipt(REVISION, 0, 0, "a".repeat(64))));
        refused(NullPointerException.class, "expected",
                () -> M3LangDexSourceEnvelope.verify(new ByteArrayInputStream(bytes), null));
    }

    private static void framing() throws Exception {
        refused(IOException.class, "LangDex source magic or revision mismatch", () -> verify(new byte[0], 0, 0));
        byte[] otherRevision = (MAGIC + "\t" + "f".repeat(40) + "\n" + HEADER + "\n")
                .getBytes(StandardCharsets.US_ASCII);
        refused(IOException.class, "LangDex source magic or revision mismatch",
                () -> verify(otherRevision, 0, 0));
        byte[] otherHeader = (MAGIC + "\t" + REVISION + "\nrow_type\n").getBytes(StandardCharsets.US_ASCII);
        refused(IOException.class, "LangDex source header mismatch", () -> verify(otherHeader, 0, 0));
        byte[] unterminated = (MAGIC + "\t" + REVISION + "\n" + HEADER).getBytes(StandardCharsets.US_ASCII);
        refused(IOException.class, "LangDex source line is not LF-terminated", () -> verify(unterminated, 0, 0));
        String ascii = "LangDex source wire must be ASCII with LF newlines";
        byte[] crlf = (MAGIC + "\t" + REVISION + "\r\n").getBytes(StandardCharsets.US_ASCII);
        refused(IOException.class, ascii, () -> verify(crlf, 0, 0));
        byte[] latin = wire("LEXEME\t7\t~\té\t~\t-");
        refused(IOException.class, ascii, () -> verify(latin, 1, 0));
    }

    private static void rows() {
        rowRefused("LangDex source row must have exactly six columns", 1, 0, "LEXEME\t7\t~\t~\t~");
        rowRefused("LangDex source row must have exactly six columns", 1, 0, LEXEME + "\t");
        rowRefused("Unknown LangDex source row type", 1, 0, "lexeme\t7\t~\t~\t~\t-");
        rowRefused("LangDex lexeme row count exceeds receipt", 1, 0, LEXEME, LEXEME);
        rowRefused("LangDex concept-link row count exceeds receipt", 0, 0, LINK);
        rowRefused("LangDex source row counts differ from receipt", 2, 0, LEXEME);
        rowRefused("LangDex source row counts differ from receipt", 0, 2, LINK);
        rowRefused("LangDex integer is not canonical decimal", 1, 0, "LEXEME\t007\t~\t~\t~\t-");
        rowRefused("LangDex integer is not canonical decimal", 0, 1, "CONCEPT_LINK\t7\t-\t-\t-\t+3");
        String range = "LangDex integer is outside canonical signed-64-bit range";
        rowRefused(range, 1, 0, "LEXEME\tseven\t~\t~\t~\t-");
        rowRefused(range, 0, 1, "CONCEPT_LINK\t9223372036854775808\t-\t-\t-\t1");
        rowRefused(range, 0, 1, "CONCEPT_LINK\t7\t-\t-\t-\t");
        rowRefused("LangDex text must use the null marker or b64: encoding", 1, 0,
                "LEXEME\t7\twater\t~\t~\t-");
        rowRefused("LangDex text is not valid standard Base64", 1, 0, "LEXEME\t7\t~\tb64:%%%%\t~\t-");
        rowRefused("LangDex text Base64 is not canonical", 1, 0, "LEXEME\t7\t~\t~\tb64:YQ\t-");
        rowRefused("LangDex text is not valid UTF-8", 1, 0, "LEXEME\t7\tb64:/w==\t~\t~\t-");
        rowRefused("LangDex unused field must be '-'", 1, 0, "LEXEME\t7\t~\t~\t~\t1");
        rowRefused("LangDex unused field must be '-'", 0, 1, "CONCEPT_LINK\t7\t~\t-\t-\t1");
        rowRefused("LangDex unused field must be '-'", 0, 1, "CONCEPT_LINK\t7\t-\t~\t-\t1");
        rowRefused("LangDex unused field must be '-'", 0, 1, "CONCEPT_LINK\t7\t-\t-\t~\t1");
    }

    private static void digest() throws Exception {
        byte[] bytes = wire(LEXEME);
        Receipt wrong = new Receipt(REVISION, 1, 0, sha256(wire(LINK)));
        refused(IOException.class, "LangDex source digest differs from receipt",
                () -> M3LangDexSourceEnvelope.verify(new ByteArrayInputStream(bytes), wrong));
    }

    public static void main(String[] args) throws Exception {
        receipts();
        acceptedWire();
        framing();
        rows();
        digest();
        System.out.println("M3_LANGDEX_SOURCE_REFUSAL_PASS checks=" + checks);
    }
}
