/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.SynexiaLangDexSourceEnvelope;
import com.m3.text.SynexiaLangDexSourceEnvelope.Receipt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/** Focused grammar, receipt, and fail-closed tests for the non-admitting intake. */
public final class LangDexSourceEnvelopeTest {
    private static final String REVISION = "e4e1d8aacd13eff1f8ba7f3df89bc872ba68f349";
    private static final String MAGIC = "#M3-LANGDEX-SOURCE-1";
    private static final String HEADER =
            "row_type\tlexeme_id\tglottocode_b64\tlemma_b64\tsource_b64\tconcept_id";
    private static int checks;

    public static void main(String[] args) throws Exception {
        // These six lexeme and six concept-link rows mirror the exact expected
        // lines in the pinned Synexia LangDexSourceExportWriterTest. This is a
        // source-backed fixture, not an artifact captured from executing that writer.
        String unusualSource = "dictionary\tname\nsecond line";
        long largeId = 9_007_199_254_740_993L;
        String producerWire = String.join("\n", List.of(
                MAGIC + "\t" + REVISION,
                HEADER,
                lexeme("~", "zxx", "no-id", "no-id-source"),
                lexeme("7", "jpn", "水", unusualSource),
                lexeme("7", "jpn", "水", unusualSource),
                lexeme("11", null, null, null),
                lexeme(Long.toString(largeId), "eng", "bank", "panlex:bank"),
                lexeme(Long.toString(largeId + 1), "eng", "bank", ""),
                link("~", "999"),
                link("7", "101"),
                link("7", "101"),
                link(Long.toString(largeId), "100"),
                link(Long.toString(largeId), "101"),
                link(Long.toString(largeId + 1), "100"))) + "\n";
        byte[] producerBytes = producerWire.getBytes(StandardCharsets.US_ASCII);
        Receipt producerReceipt = new Receipt(REVISION, 6, 6, sha256(producerBytes));
        Receipt producerResult = SynexiaLangDexSourceEnvelope.verify(
                new ByteArrayInputStream(producerBytes), producerReceipt);
        check(producerResult.equals(producerReceipt), "pinned Synexia expected-wire fixture");

        // Preserve receiver-specific signed-long extrema beyond the producer fixture.
        String wire = producerWire
                + lexeme(Long.toString(Long.MAX_VALUE), "eng", "max-id", "m3-boundary") + "\n"
                + link(Long.toString(Long.MAX_VALUE), Long.toString(Long.MIN_VALUE)) + "\n";
        byte[] bytes = wire.getBytes(StandardCharsets.US_ASCII);
        Receipt expected = new Receipt(REVISION, 7, 7, sha256(bytes));
        Receipt actual = SynexiaLangDexSourceEnvelope.verify(
                new ByteArrayInputStream(bytes), expected);
        check(actual.equals(expected), "receipt round trip");

        reject("magic", expected, wire.replace(MAGIC, "#M3-LANGDEX-SOURCE-X"));
        reject("header", expected, wire.replace(HEADER, HEADER.replace("lemma_b64", "lemma")));
        reject("revision", expected,
                wire.replace(REVISION, "1123456789abcdef0123456789abcdef01234567"));
        reject("malformed Base64", expected, wire.replace(encoded("no-id-source"), "b64:@@"));
        reject("noncanonical Base64", expected, wire.replace(encoded("no-id-source"), "b64:Zh=="));
        reject("invalid UTF-8", expected, wire.replace(encoded("no-id-source"), "b64:/w=="));
        reject("noncanonical integer", expected,
                wire.replace(Long.toString(Long.MAX_VALUE), "07"));
        reject("unused-field mutation", expected, wire.replace("\t-\n", "\tvalue\n"));
        reject("CRLF", expected, wire.replace("\n", "\r\n"));
        reject("missing final newline", expected, wire.substring(0, wire.length() - 1));

        byte[] changed = wire.replace(encoded("no-id-source"), encoded("changed-source")).getBytes(StandardCharsets.US_ASCII);
        rejectBytes("digest mismatch", changed, expected);
        byte[] valid = wire.getBytes(StandardCharsets.US_ASCII);
        Receipt wrongCounts = new Receipt(REVISION, 2, 2, sha256(valid));
        rejectBytes("row-count mismatch", valid, wrongCounts);
        check(checks >= 14, "discriminator coverage");
        System.out.println("M3_LANGDEX_SOURCE_ENVELOPE_PASS checks=" + checks
                + " lexemeRows=7 conceptLinkRows=7 admissions=0");
    }

    private static String lexeme(
            String id, String glottocode, String lemma, String source) {
        return "LEXEME\t" + id + "\t" + encodedNullable(glottocode)
                + "\t" + encodedNullable(lemma) + "\t" + encodedNullable(source) + "\t-";
    }

    private static String link(String lexemeId, String conceptId) {
        return "CONCEPT_LINK\t" + lexemeId + "\t-\t-\t-\t" + conceptId;
    }

    private static String encodedNullable(String value) {
        return value == null ? "~" : encoded(value);
    }

    private static String encoded(String value) {
        return "b64:" + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }

    private static void reject(String label, Receipt expected, String changed) throws Exception {
        rejectBytes(label, changed.getBytes(StandardCharsets.US_ASCII), expected);
    }

    private static void rejectBytes(String label, byte[] value, Receipt expected) throws Exception {
        try {
            SynexiaLangDexSourceEnvelope.verify(new ByteArrayInputStream(value), expected);
            throw new AssertionError("accepted " + label);
        } catch (IOException expectedFailure) {
            checks++;
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
