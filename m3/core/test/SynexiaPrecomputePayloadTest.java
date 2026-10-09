/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.SynexiaPrecomputePayload;

import java.util.Arrays;
import java.util.List;

public final class SynexiaPrecomputePayloadTest {
    private static int checks;

    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }

    private static void expect(Class<? extends Throwable> type, Runnable body) {
        try {
            body.run();
            throw new AssertionError("missing " + type.getName());
        } catch (Throwable failure) {
            check(type.isInstance(failure));
        }
    }

    public static void main(String[] args) {
        String json = "{\"canonical_decimal_spelling\":\"10000\",\"code_point_length\":2,"
                + "\"concept_ids\":[11,0,19],\"flags\":7,\"language_id\":3,"
                + "\"max_value\":10000,\"min_value\":0,\"precomputed_value_count\":10001,"
                + "\"record_id\":\"0\",\"si_offset\":1.5,\"si_prefixable\":true,"
                + "\"shared_utf16_storage\":true,\"source_id\":\"dictlang.numbers.0-10000\","
                + "\"translation_grammar_supported\":true,\"unknown_future\":-9}";
        SynexiaPrecomputePayload payload = SynexiaPrecomputePayload.parse(json);
        check(payload.fieldNames().equals(new java.util.TreeSet<>(List.of(
                "canonical_decimal_spelling", "code_point_length", "concept_ids", "flags",
                "language_id", "max_value", "min_value", "precomputed_value_count", "record_id",
                "si_offset", "si_prefixable", "shared_utf16_storage", "source_id",
                "translation_grammar_supported", "unknown_future"))));
        check(payload.requireInt("flags") == 7);
        check(payload.requireLong("language_id") == 3L);
        check(payload.requireInt("min_value") == 0);
        check(payload.requireInt("max_value") == 10000);
        check(payload.requireInt("precomputed_value_count") == 10001);
        check(payload.requireString("canonical_decimal_spelling").equals("10000"));
        check(payload.requireString("source_id").equals("dictlang.numbers.0-10000"));
        check(payload.requireString("record_id").equals("0"));
        check(payload.requireDouble("si_offset") == 1.5d);
        check(payload.requireBoolean("si_prefixable"));
        check(payload.requireBoolean("shared_utf16_storage"));
        check(payload.requireBoolean("translation_grammar_supported"));
        check(Arrays.equals(payload.requireLongArray("concept_ids"), new long[]{11, 0, 19}));
        check(payload.canonicalJson().equals(
                "{\"canonical_decimal_spelling\":\"10000\",\"code_point_length\":2,"
                        + "\"concept_ids\":[11,0,19],\"flags\":7,\"language_id\":3,"
                        + "\"max_value\":10000,\"min_value\":0,\"precomputed_value_count\":10001,"
                        + "\"record_id\":\"0\",\"shared_utf16_storage\":true,"
                        + "\"si_offset\":1.5,\"si_prefixable\":true,"
                        + "\"source_id\":\"dictlang.numbers.0-10000\","
                        + "\"translation_grammar_supported\":true,\"unknown_future\":-9}"));
        check(SynexiaPrecomputePayload.parse(payload.canonicalJson()).canonicalJson()
                .equals(payload.canonicalJson()));
        check(SynexiaPrecomputePayload.parse("{}").fieldNames().isEmpty());
        expect(IllegalArgumentException.class, () -> SynexiaPrecomputePayload.parse(
                "{\"flags\":1,\"flags\":2}"));
        expect(IllegalArgumentException.class, () -> SynexiaPrecomputePayload.parse(
                "{\"flags\":1.5}"));
        expect(IllegalArgumentException.class, () -> SynexiaPrecomputePayload.parse(
                "{\"memberships\":[2147483648]}"));
        expect(IllegalArgumentException.class, () -> SynexiaPrecomputePayload.parse(
                "{\"si_offset\":1e999}"));
        expect(IllegalArgumentException.class, () -> SynexiaPrecomputePayload.parse(
                "{\"canonical_decimal_spelling\":1}"));
        expect(IllegalArgumentException.class, () -> SynexiaPrecomputePayload.parse(
                "{\"flags\":null}"));
        expect(IllegalArgumentException.class, () -> SynexiaPrecomputePayload.parse(
                "{\"flags\":{\"nested\":1}}"));
        expect(IllegalArgumentException.class, () -> payload.requireBoolean("flags"));
        System.out.println("M3JDK_SYNXIA_PAYLOAD_PASS checks=" + checks
                + " fields=" + payload.fieldNames().size());
    }
}
