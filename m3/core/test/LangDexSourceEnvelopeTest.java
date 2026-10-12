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

/** Focused grammar, receipt, and fail-closed tests for the non-admitting intake. */
public final class LangDexSourceEnvelopeTest {
    private static final String REVISION = "0123456789abcdef0123456789abcdef01234567";
    private static final String MAGIC = "#M3-LANGDEX-SOURCE-1";
    private static final String HEADER =
            "row_type\tlexeme_id\tglottocode_b64\tlemma_b64\tsource_b64\tconcept_id";
    private static int checks;

    public static void main(String[] args) throws Exception {
        String opaque = "Māori\tword\n😀";
        String wire = MAGIC + "\t" + REVISION + "\n"
                + HEADER + "\n"
                + "LEXEME\t9223372036854775807\t" + encoded("abc") + "\tb64:"
                + "\t" + encoded(opaque) + "\t-\n"
                + "LEXEME\t9223372036854775807\t~\t" + encoded("duplicate")
                + "\t~\t-\n"
                + "LEXEME\t~\t" + encoded("") + "\t~\t" + encoded("") + "\t-\n"
                + "CONCEPT_LINK\t9223372036854775807\t-\t-\t-\t-9223372036854775808\n"
                + "CONCEPT_LINK\t9223372036854775807\t-\t-\t-\t~\n";
        byte[] bytes = wire.getBytes(StandardCharsets.US_ASCII);
        Receipt expected = new Receipt(REVISION, 3, 2, sha256(bytes));
        Receipt actual = SynexiaLangDexSourceEnvelope.verify(
                new ByteArrayInputStream(bytes), expected);
        check(actual.equals(expected), "receipt round trip");

        reject("magic", expected, wire.replace(MAGIC, "#M3-LANGDEX-SOURCE-X"));
        reject("header", expected, wire.replace(HEADER, HEADER.replace("lemma_b64", "lemma")));
        reject("revision", expected,
                wire.replace(REVISION, "1123456789abcdef0123456789abcdef01234567"));
        reject("malformed Base64", expected, wire.replace(encoded("abc"), "b64:@@"));
        reject("noncanonical Base64", expected, wire.replace(encoded("abc"), "b64:Zh=="));
        reject("invalid UTF-8", expected, wire.replace(encoded("abc"), "b64:/w=="));
        reject("noncanonical integer", expected,
                wire.replace("9223372036854775807", "07"));
        reject("unused-field mutation", expected, wire.replace("\t-\n", "\tvalue\n"));
        reject("CRLF", expected, wire.replace("\n", "\r\n"));
        reject("missing final newline", expected, wire.substring(0, wire.length() - 1));

        byte[] changed = wire.replace(encoded("duplicate"), encoded("changed")).getBytes(StandardCharsets.US_ASCII);
        rejectBytes("digest mismatch", changed, expected);
        byte[] valid = wire.getBytes(StandardCharsets.US_ASCII);
        Receipt wrongCounts = new Receipt(REVISION, 2, 2, sha256(valid));
        rejectBytes("row-count mismatch", valid, wrongCounts);
        check(checks >= 12, "discriminator coverage");
        System.out.println("M3_LANGDEX_SOURCE_ENVELOPE_PASS checks=" + checks
                + " lexemeRows=3 conceptLinkRows=2 admissions=0");
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
