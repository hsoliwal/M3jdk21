/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3PrecomputePayload;
import jdk.m3.text.M3PrecomputePayload.Field;
import jdk.m3.text.M3PrecomputePayload.Kind;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Edge and refusal contract for the canonical precompute_payload receiver: the
 * typed Field value model, JSON grammar refusals, declared field-type checks,
 * typed accessors and canonical JSON escaping/round trip.
 */
public final class M3PrecomputePayloadParserContractTest {
    private static int checks;

    private M3PrecomputePayloadParserContractTest() { }

    private static void check(boolean condition, String what) {
        checks++;
        if (!condition) throw new AssertionError(what + " [check " + checks + "]");
    }

    private static void expect(Class<? extends Throwable> type, String fragment, Runnable action) {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)
                    && (fragment == null || String.valueOf(thrown.getMessage()).contains(fragment))) {
                return;
            }
            throw new AssertionError("wrong refusal, wanted " + type.getSimpleName()
                    + " containing '" + fragment + "'", thrown);
        }
        throw new AssertionError("missing " + type.getSimpleName() + " containing '" + fragment + "'");
    }

    private static void refuse(String json, String fragment) {
        expect(IllegalArgumentException.class, fragment, () -> M3PrecomputePayload.parse(json));
    }

    public static void main(String[] args) {
        fieldValueModel();
        fieldEqualityAndHash();
        grammarRefusals();
        declaredFieldTypes();
        typedAccessors();
        canonicalJsonEscapingAndRoundTrip();
        System.out.println("M3_PRECOMPUTE_PAYLOAD_PARSER_CONTRACT_PASS checks=" + checks);
    }

    private static void fieldValueModel() {
        Field integer = new Field(Kind.INTEGER, false, 5L, 0.0d, null);
        check(integer.stringValue() == null && integer.integerArrayValue() == null,
                "five-argument constructor leaves string/array absent");
        long[] source = {1L, 2L};
        Field array = new Field(Kind.INTEGER_ARRAY, false, 0L, 0.0d, source);
        source[0] = 99L;
        long[] read = array.integerArrayValue();
        check(Arrays.equals(read, new long[] {1L, 2L}), "array input is copied");
        read[1] = 77L;
        check(Arrays.equals(array.integerArrayValue(), new long[] {1L, 2L}), "array output is copied");
        check(new Field(Kind.INTEGER_ARRAY, false, 3L, 0.0d, new long[0]).integerValue() == 3L,
                "array kind may carry an integer value");
        check(new Field(Kind.DECIMAL, false, 0L, 0.0d, null).decimalValue() == 0.0d, "decimal zero");
        check(new Field(Kind.DECIMAL, false, 0L, -2.5d, null).decimalValue() == -2.5d, "decimal value");
        check(new Field(Kind.STRING, false, 0L, 0.0d, null, "s").stringValue().equals("s"), "string value");
        check(new Field(Kind.BOOLEAN, true, 0L, 0.0d, null).booleanValue(), "boolean value");

        expect(NullPointerException.class, "kind", () -> new Field(null, false, 0L, 0.0d, null));
        expect(NullPointerException.class, "integerArrayValue",
                () -> new Field(Kind.INTEGER_ARRAY, false, 0L, 0.0d, null));
        expect(IllegalArgumentException.class, "array value on non-array field",
                () -> new Field(Kind.INTEGER, false, 1L, 0.0d, new long[] {1L}));
        expect(NullPointerException.class, "stringValue",
                () -> new Field(Kind.STRING, false, 0L, 0.0d, null, null));
        expect(IllegalArgumentException.class, "string value on non-string field",
                () -> new Field(Kind.BOOLEAN, true, 0L, 0.0d, null, "x"));
        expect(IllegalArgumentException.class, "decimal value on non-decimal field",
                () -> new Field(Kind.INTEGER, false, 1L, 1.5d, null));
        expect(IllegalArgumentException.class, "integer value on non-integer field",
                () -> new Field(Kind.BOOLEAN, false, 3L, 0.0d, null));
        expect(IllegalArgumentException.class, "integer value on non-integer field",
                () -> new Field(Kind.DECIMAL, false, 1L, 2.0d, null));
        expect(IllegalArgumentException.class, "integer value on non-integer field",
                () -> new Field(Kind.STRING, false, 4L, 0.0d, null, "s"));
    }

    private static void fieldEqualityAndHash() {
        Field a = new Field(Kind.INTEGER_ARRAY, false, 0L, 0.0d, new long[] {4L, 5L});
        Field b = new Field(Kind.INTEGER_ARRAY, false, 0L, 0.0d, new long[] {4L, 5L});
        check(a.equals(a), "reflexive");
        check(a.equals(b) && b.equals(a), "array content equality, not identity");
        check(a.hashCode() == b.hashCode(), "equal arrays hash equally");
        check(!a.equals("not a field") && !a.equals(null), "foreign values are unequal");
        check(!new Field(Kind.INTEGER, false, 0L, 0.0d, null)
                .equals(new Field(Kind.BOOLEAN, false, 0L, 0.0d, null)), "kind differs");
        check(!new Field(Kind.BOOLEAN, true, 0L, 0.0d, null)
                .equals(new Field(Kind.BOOLEAN, false, 0L, 0.0d, null)), "boolean differs");
        check(!new Field(Kind.INTEGER, false, 1L, 0.0d, null)
                .equals(new Field(Kind.INTEGER, false, 2L, 0.0d, null)), "integer differs");
        check(!new Field(Kind.DECIMAL, false, 0L, 1.0d, null)
                .equals(new Field(Kind.DECIMAL, false, 0L, 2.0d, null)), "decimal differs");
        check(new Field(Kind.DECIMAL, false, 0L, Double.NaN, null)
                .equals(new Field(Kind.DECIMAL, false, 0L, Double.NaN, null)), "NaN bit-equality");
        check(!a.equals(new Field(Kind.INTEGER_ARRAY, false, 0L, 0.0d, new long[] {4L})),
                "array differs");
        Field s1 = new Field(Kind.STRING, false, 0L, 0.0d, null, "x");
        check(s1.equals(new Field(Kind.STRING, false, 0L, 0.0d, null, "x"))
                && s1.hashCode() == new Field(Kind.STRING, false, 0L, 0.0d, null, "x").hashCode(),
                "string equality and hash");
        check(!s1.equals(new Field(Kind.STRING, false, 0L, 0.0d, null, "y")), "string differs");
        check(M3PrecomputePayload.parse("{\"q\":[1,2]}").field("q").orElseThrow()
                .equals(new Field(Kind.INTEGER_ARRAY, false, 0L, 0.0d, new long[] {1L, 2L})),
                "parsed field equals constructed field");
    }

    private static void grammarRefusals() {
        check(M3PrecomputePayload.parse("  { }  ").fieldNames().isEmpty(), "empty object with whitespace");
        check(M3PrecomputePayload.parse("{\n\"a\" : 1 ,\t\"b\":2}").fieldNames().size() == 2,
                "whitespace between tokens");
        refuse("[1]", "expected '{'");
        refuse("{} x", "trailing JSON");
        refuse("{\"A\":1}", "invalid payload field name");
        refuse("{\"1a\":1}", "invalid payload field name");
        refuse("{\"a\":1,\"a\":2}", "duplicate payload field");
        refuse("{\"a\" 1}", "expected ':'");
        refuse("{\"a\":1 \"b\":2}", "expected ','");
        refuse("{1:2}", "expected '\"'");
        refuse("{\"a\":", "unexpected end");
        refuse("{\"a\":1", "expected ','");
        refuse("{\"a\":tru}", "invalid boolean");
        refuse("{\"a\":fals}", "invalid boolean");
        refuse("{\"a\":-}", "digits required");
        refuse("{\"a\":x}", "digits required");
        refuse("{\"a\":1.}", "digits required");
        refuse("{\"a\":1e}", "digits required");
        refuse("{\"a\":1e+}", "digits required");
        refuse("{\"a\":1e999}", "non-finite number");
        refuse("{\"a\":99999999999999999999}", "integer outside long range");
        refuse("{\"a\":[1.5]}", "arrays must contain integers");
        refuse("{\"a\":[1 2]}", "expected ','");
        refuse("{\"a\":[", "unexpected end");
        refuse("{\"a\":\"x", "unterminated string");
        refuse("{\"a\":\"x\\", "unterminated escape");
        refuse("{\"a\":\"\\q\"}", "invalid escape");
        refuse("{\"a\":\"\\u12", "short unicode escape");
        refuse("{\"a\":\"\\u12zz\"}", "invalid unicode escape");
        refuse("{\"a\":\"x\u0001\"}", "unescaped control character");
    }

    private static void declaredFieldTypes() {
        refuse("{\"si_prefixable\":1}", "payload type mismatch for si_prefixable");
        check(M3PrecomputePayload.parse("{\"si_offset\":2}").requireDouble("si_offset") == 2.0d,
                "integer accepted for double field");
        check(M3PrecomputePayload.parse("{\"si_offset\":2.5}").requireDouble("si_offset") == 2.5d,
                "decimal accepted for double field");
        refuse("{\"si_offset\":\"x\"}", "payload type mismatch for si_offset");
        refuse("{\"flags\":3000000000}", "payload type mismatch for flags");
        refuse("{\"flags\":-3000000000}", "payload type mismatch for flags");
        refuse("{\"flags\":\"x\"}", "payload type mismatch for flags");
        check(M3PrecomputePayload.parse("{\"flags\":-2147483648}").requireInt("flags") == Integer.MIN_VALUE,
                "int lower bound");
        check(M3PrecomputePayload.parse("{\"stem_id\":9000000000}").requireLong("stem_id") == 9_000_000_000L,
                "long field");
        refuse("{\"stem_id\":1.5}", "payload type mismatch for stem_id");
        refuse("{\"source_id\":5}", "payload type mismatch for source_id");
        check(M3PrecomputePayload.parse("{\"lexicon_fingerprint\":\"abc\"}")
                .requireString("lexicon_fingerprint").equals("abc"), "fingerprint as string");
        check(M3PrecomputePayload.parse("{\"lexicon_fingerprint\":12}")
                .requireLong("lexicon_fingerprint") == 12L, "fingerprint as long");
        refuse("{\"lexicon_fingerprint\":true}", "payload type mismatch for lexicon_fingerprint");
        check(Arrays.equals(M3PrecomputePayload.parse("{\"memberships\":[1,-2]}")
                .requireIntArray("memberships"), new int[] {1, -2}), "int array field");
        refuse("{\"memberships\":5}", "payload type mismatch for memberships");
        refuse("{\"memberships\":[3000000000]}", "payload type mismatch for memberships");
        refuse("{\"memberships\":[-3000000000]}", "payload type mismatch for memberships");
        check(Arrays.equals(M3PrecomputePayload.parse("{\"subjects\":[3000000000]}")
                .requireLongArray("subjects"), new long[] {3_000_000_000L}), "long array field");
        refuse("{\"subjects\":7}", "payload type mismatch for subjects");
        check(M3PrecomputePayload.parse("{\"si_prefixable\":false}").requireBoolean("si_prefixable") == false,
                "false literal");
    }

    private static void typedAccessors() {
        M3PrecomputePayload payload = M3PrecomputePayload.parse(
                "{\"big_value\":3000000000,\"small_value\":-3000000000,\"ok_value\":7,"
                        + "\"wide\":[1,3000000000],\"narrow\":[1,-3000000000],\"label\":\"x\","
                        + "\"ratio\":1E+2,\"tiny\":2e-3,\"scaled\":3E2}");
        Map<String, Field> fields = payload.fields();
        check(fields.keySet().equals(payload.fieldNames()) && fields.size() == 9, "fields view");
        expect(UnsupportedOperationException.class, null,
                () -> fields.put("z", new Field(Kind.INTEGER, false, 0L, 0.0d, null)));
        check(payload.field("ok_value").map(Field::integerValue).equals(Optional.of(7L)), "field lookup");
        check(payload.field("absent").isEmpty(), "absent field lookup");
        check(payload.requireInt("ok_value") == 7, "requireInt");
        expect(IllegalArgumentException.class, "outside int range: big_value",
                () -> payload.requireInt("big_value"));
        expect(IllegalArgumentException.class, "outside int range: small_value",
                () -> payload.requireInt("small_value"));
        check(payload.requireDouble("ok_value") == 7.0d, "integer widened to double");
        check(payload.requireDouble("ratio") == 100.0d && payload.requireDouble("tiny") == 0.002d
                && payload.requireDouble("scaled") == 300.0d, "exponent forms");
        expect(NullPointerException.class, "missing field: absent", () -> payload.requireDouble("absent"));
        expect(IllegalArgumentException.class, "field is not numeric: label",
                () -> payload.requireDouble("label"));
        expect(IllegalArgumentException.class, "array element is outside int range: wide",
                () -> payload.requireIntArray("wide"));
        expect(IllegalArgumentException.class, "array element is outside int range: narrow",
                () -> payload.requireIntArray("narrow"));
        expect(IllegalArgumentException.class, "missing field: absent", () -> payload.requireLong("absent"));
        expect(IllegalArgumentException.class, "field has kind STRING: label",
                () -> payload.requireLong("label"));
        expect(NullPointerException.class, "name", () -> payload.requireString(null));
    }

    private static void canonicalJsonEscapingAndRoundTrip() {
        String json = "{\"source_id\":\"q\\\"b\\\\s\\/\\b\\f\\n\\r\\t\\u0001\\u00e9\u007f\","
                + "\"si_prefixable\":true,\"si_offset\":0.5,\"empty\":[],\"ids\":[3,-4],\"n\":-6}";
        M3PrecomputePayload payload = M3PrecomputePayload.parse(json);
        check(payload.requireString("source_id").equals("q\"b\\s/\b\f\n\r\t\u0001\u00e9\u007f"),
                "all JSON escapes decoded");
        String canonical = payload.canonicalJson();
        check(canonical.equals("{\"empty\":[],\"ids\":[3,-4],\"n\":-6,\"si_offset\":0.5,"
                + "\"si_prefixable\":true,\"source_id\":\"q\\\"b\\\\s/\\b\\f\\n\\r\\t\\u0001\\u00e9\u007f\"}"),
                "canonical JSON escapes and sorted names: " + canonical);
        M3PrecomputePayload again = M3PrecomputePayload.parse(canonical);
        check(again.fields().equals(payload.fields()), "canonical JSON round trip");
        check(again.canonicalJson().equals(canonical), "canonical JSON is a fixed point");
        check(M3PrecomputePayload.parse("{}").canonicalJson().equals("{}"), "empty canonical object");
    }
}
