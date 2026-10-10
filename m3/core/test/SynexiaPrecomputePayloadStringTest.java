/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.SynexiaPrecomputePayload;

/** Focused proof for the reviewed String payload type admitted by the exporter contract. */
public final class SynexiaPrecomputePayloadStringTest {
    private static int checks;

    public static void main(String[] args) {
        SynexiaPrecomputePayload payload = SynexiaPrecomputePayload.parse(
                "{\"language\":\"en\",\"mapping_id\":\"en\\\"id\","
                        + "\"source_revision\":\"rev-1\",\"frequency_rank\":7}");
        check(payload.requireString("source_revision").equals("rev-1"), "typed string accessor");
        check(payload.field("language").orElseThrow().kind()
                == SynexiaPrecomputePayload.Kind.STRING, "unknown string remains readable");
        check(payload.requireString("mapping_id").equals("en\"id"), "escaped string round trip");
        check(payload.canonicalJson().equals(
                "{\"frequency_rank\":7,\"language\":\"en\","
                        + "\"mapping_id\":\"en\\\"id\",\"source_revision\":\"rev-1\"}"),
                "canonical string ordering");
        expectFailure(() -> SynexiaPrecomputePayload.parse(
                "{\"source_revision\":7}"), "known string type rejects number");
        expectFailure(() -> SynexiaPrecomputePayload.parse(
                "{\"source_revision\":\"rev\",\"source_revision\":\"dup\"}"),
                "duplicate string field rejects");
        check(new SynexiaPrecomputePayload.Field(
                SynexiaPrecomputePayload.Kind.STRING, false, 0L, 0.0d, null, "x")
                .stringValue().equals("x"), "string field remains immutable");
        check(new SynexiaPrecomputePayload.Field(
                SynexiaPrecomputePayload.Kind.INTEGER, false, 3L, 0.0d, null).integerValue() == 3L,
                "legacy field constructor remains compatible");
        System.out.println("M3JDK_PAYLOAD_STRING_PASS checks=" + checks);
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static void expectFailure(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }
}
