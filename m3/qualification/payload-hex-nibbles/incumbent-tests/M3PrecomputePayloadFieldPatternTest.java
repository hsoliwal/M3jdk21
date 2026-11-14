/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3PrecomputePayload;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Fold-close audit L0064: the payload field-name check runs on one compiled Pattern (#2447 d58ff13e954, Synexia crate
 * m3-payload-field-pattern-20261104). Ported from that crate's M3PayloadFieldPatternTest: the shared pattern keeps the
 * fixed expression and flags (structural, through reflection; the master preimage has no FIELD_NAME_PATTERN field and
 * fails here first), every UTF-16 unit alone and after "a" is accepted or refused exactly as String.matches decides,
 * the valid and invalid donor payloads keep their verdicts, six concurrent readers agree, and array ownership stays
 * defensive. Needs --add-opens jdk.m3.text/jdk.m3.text=ALL-UNNAMED for the structural check.
 */
public final class M3PrecomputePayloadFieldPatternTest {
    private static final String REGEX = "[a-z][a-z0-9_]*";
    private static final String NAME_ERROR = "invalid payload field name";
    private static int checks;

    private static void check(boolean value, String what) {
        checks++;
        if (!value) throw new AssertionError("check " + checks + ": " + what);
    }

    public static void main(String[] args) throws Exception {
        sharedPattern();
        utf16Sweep();
        donorPayloads();
        concurrentReaders();
        defensiveOwnership();
        System.out.println("M3_PAYLOAD_FIELD_PATTERN_PASS checks=" + checks);
    }

    private static void sharedPattern() throws Exception {
        Field field;
        try {
            field = M3PrecomputePayload.class.getDeclaredField("FIELD_NAME_PATTERN");
        } catch (NoSuchFieldException missing) {
            throw new AssertionError("FIELD_NAME_PATTERN missing: the field-name check compiles per field (master preimage)");
        }
        int modifiers = field.getModifiers();
        check(Modifier.isPrivate(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers), "modifiers");
        field.setAccessible(true);
        Pattern pattern = (Pattern) field.get(null);
        check(pattern == field.get(null), "one shared instance");
        check(REGEX.equals(pattern.pattern()), "fixed expression " + pattern.pattern());
        check(pattern.flags() == 0, "no flags");
    }

    /** Every UTF-16 unit alone and after "a" as the only field name: 131,072 payloads. */
    private static void utf16Sweep() {
        for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
            String escaped = String.format("\\u%04x", unit);
            String one = String.valueOf((char) unit);
            verdict(one, "{\"" + escaped + "\":1}");
            verdict("a" + one, "{\"a" + escaped + "\":1}");
        }
    }

    private static void verdict(String name, String json) {
        boolean expectedRefusal = !name.matches(REGEX);
        boolean refusedByName;
        try {
            M3PrecomputePayload.parse(json);
            refusedByName = false;
        } catch (IllegalArgumentException failure) {
            refusedByName = failure.getMessage().startsWith(NAME_ERROR);
        }
        check(refusedByName == expectedRefusal, "name verdict for " + json);
    }

    private static void donorPayloads() {
        for (String input : List.of("{}",
                "{\"camel_breaks_utf16\":[4],\"pascal_breaks_utf16\":[4],\"hyphen_positions_utf16\":[2,4],"
                        + "\"hyphen_left_min\":2,\"hyphen_right_min\":3,\"hyphen_language\":\"en-US\"}",
                "{\"source_id\":\"donor\",\"record_id\":\"same\",\"frequency_rank\":2147483647,"
                        + "\"corpus_count\":9223372036854775807,\"si_offset\":-0.0,\"si_prefixable\":true,"
                        + "\"memberships\":[-2147483648,0,2147483647],"
                        + "\"concept_ids\":[-9223372036854775808,9223372036854775807]}",
                "{\"unicode_value\":\"" + String.format("\\u%04x\\u%04x\\u%04x\\u%04x", 0xd83d, 0xde00, 0x939, 0x93f)
                        + "\",\"x\":[],\"z\":1.25e2}")) {
            M3PrecomputePayload payload = M3PrecomputePayload.parse(input);
            check(M3PrecomputePayload.parse(payload.canonicalJson()).canonicalJson().equals(payload.canonicalJson()),
                    "round trip " + input);
        }
        for (char first = 'a'; first <= 'z'; first++) {
            for (char tail : "abcdefghijklmnopqrstuvwxyz0123456789_".toCharArray()) {
                String name = "" + first + tail;
                check(M3PrecomputePayload.parse("{\"" + name + "\":false}").fieldNames().equals(java.util.Set.of(name)),
                        "two-unit name " + name);
            }
        }
        for (String input : Arrays.asList("", "[]", "{\"\":1}", "{\"Camel\":1}", "{\"1x\":1}", "{\"a-b\":1}",
                "{\"a.b\":1}", "{\"a\":null}", "{\"a\":[1.5]}", "{\"a\":{}}", "{\"a\":1,\"a\":2}",
                "{\"a\":9223372036854775808}", "{\"frequency_rank\":2147483648}", "{\"si_prefixable\":1}",
                "{\"a\":1} trailing", "{\"a\":1e9999}", "{\"source_id\":false}")) {
            try {
                M3PrecomputePayload.parse(input);
                throw new AssertionError("accepted " + input);
            } catch (IllegalArgumentException refused) {
                check(true, "refused " + input);
            }
        }
        try {
            M3PrecomputePayload.parse(null);
            throw new AssertionError("accepted null");
        } catch (NullPointerException refused) {
            check(true, "refused null");
        }
        for (String input : List.of("{\"Camel\":1}", "{\"1x\":1}", "{\"a-b\":1}", "{\"\":1}")) {
            try {
                M3PrecomputePayload.parse(input);
                throw new AssertionError("accepted " + input);
            } catch (IllegalArgumentException refused) {
                check(refused.getMessage().startsWith(NAME_ERROR), "name message for " + input);
            }
        }
    }

    private static void concurrentReaders() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int lane = 0; lane < 6; lane++) {
                final int id = lane;
                tasks.add(() -> {
                    int agreed = 0;
                    for (int n = 0; n < 1024; n++) {
                        String json = "{\"field_" + id + "_" + n + "\":[" + n + "]}";
                        if (M3PrecomputePayload.parse(json).canonicalJson().equals(json)) agreed++;
                    }
                    return agreed;
                });
            }
            for (Future<Integer> result : pool.invokeAll(tasks)) check(result.get() == 1024, "concurrent lane");
        } finally {
            pool.shutdown();
            check(pool.awaitTermination(30, TimeUnit.SECONDS), "pool termination");
        }
    }

    private static void defensiveOwnership() {
        M3PrecomputePayload payload = M3PrecomputePayload.parse("{\"camel_breaks_utf16\":[4]}");
        int[] first = payload.requireIntArray("camel_breaks_utf16");
        first[0] = 99;
        check(Arrays.equals(payload.requireIntArray("camel_breaks_utf16"), new int[]{4}), "array ownership");
    }
}
