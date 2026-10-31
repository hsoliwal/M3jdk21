/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3PrecomputePayload;

import java.util.Arrays;
import java.util.List;

public final class M3PrecomputePayloadTest {
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
        String json = "{\"code_point_length\":2,\"concept_ids\":[11,0,19],"
                + "\"flags\":7,\"language_id\":3,\"si_offset\":1.5,"
                + "\"si_prefixable\":true,\"unknown_future\":-9}";
        M3PrecomputePayload payload = M3PrecomputePayload.parse(json);
        check(payload.fieldNames().equals(new java.util.TreeSet<>(List.of(
                "code_point_length", "concept_ids", "flags", "language_id",
                "si_offset", "si_prefixable", "unknown_future"))));
        check(payload.requireInt("flags") == 7);
        check(payload.requireLong("language_id") == 3L);
        check(payload.requireDouble("si_offset") == 1.5d);
        check(payload.requireBoolean("si_prefixable"));
        check(Arrays.equals(payload.requireLongArray("concept_ids"), new long[]{11, 0, 19}));
        check(payload.canonicalJson().equals(
                "{\"code_point_length\":2,\"concept_ids\":[11,0,19],\"flags\":7,"
                        + "\"language_id\":3,\"si_offset\":1.5,\"si_prefixable\":true,"
                        + "\"unknown_future\":-9}"));
        check(M3PrecomputePayload.parse(payload.canonicalJson()).canonicalJson()
                .equals(payload.canonicalJson()));
        check(M3PrecomputePayload.parse("{}").fieldNames().isEmpty());
        expect(IllegalArgumentException.class, () -> M3PrecomputePayload.parse(
                "{\"flags\":1,\"flags\":2}"));
        expect(IllegalArgumentException.class, () -> M3PrecomputePayload.parse(
                "{\"flags\":1.5}"));
        expect(IllegalArgumentException.class, () -> M3PrecomputePayload.parse(
                "{\"memberships\":[2147483648]}"));
        expect(IllegalArgumentException.class, () -> M3PrecomputePayload.parse(
                "{\"si_offset\":1e999}"));
        expect(IllegalArgumentException.class, () -> M3PrecomputePayload.parse(
                "{\"flags\":null}"));
        expect(IllegalArgumentException.class, () -> M3PrecomputePayload.parse(
                "{\"flags\":{\"nested\":1}}"));
        expect(IllegalArgumentException.class, () -> payload.requireBoolean("flags"));
        System.out.println("M3JDK_SYNXIA_PAYLOAD_PASS checks=" + checks
                + " fields=" + payload.fieldNames().size());
    }
}
