/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.text;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Fold-close audit L0064 differential for M3PrecomputePayload.parse (the field-name check of Parser.object()).
 *
 * <p>Seeded payloads: 0 to 6 fields whose names come from valid [a-z][a-z0-9_]* spellings, the known field-map names,
 * and invalid spellings (empty, upper-case, digit-first, hyphen, dot, space, underscore-first, Latin-1 and CJK letters,
 * paired and lone surrogates, control characters); the JSON spelling escapes some ordinary letters as \\uXXXX so the
 * check sees the decoded name. Values mix integers (with the long bounds and one past them), decimals, booleans,
 * strings, integer arrays, and the refused null, nested, decimal-array and trailing-text forms. A fixed sweep parses
 * every UTF-16 unit alone and after "a" as the only field name (131,072 payloads). REFERENCE counts name probes whose
 * outcome disagrees with String.matches("[a-z][a-z0-9_]*") on the decoded name (0 on both sides): a rejected name
 * must raise "invalid payload field name" and an accepted name must not. TRACE is a sha256 over every outcome
 * (canonical JSON or exception class and message); it must be identical for the master and restored class. TIMING
 * (the restored behaviour: one compiled Pattern instead of a String.matches compile per field) is reported, not
 * compared: ns per parse and bytes allocated per parse of the #2447 four-field benchmark payload.</p>
 *
 * usage: java -cp core-classes;oracle jdk.m3.text.FoldAuditPayloadDifferential rounds seed [timing]
 */
public final class FoldAuditPayloadDifferential {
    private static final String REGEX = "[a-z][a-z0-9_]*";
    private static final String NAME_ERROR = "invalid payload field name";
    private static final String BENCH = "{\"camel_breaks_utf16\":[4],\"pascal_breaks_utf16\":[4],"
            + "\"hyphen_positions_utf16\":[2,4],\"hyphen_language\":\"en-US\"}";
    private static final String[] KNOWN = {"language_id", "flags", "frequency_rank", "code_point_length",
        "si_decimal_exponent", "corpus_count", "stem_id", "sim_hash64", "langdex_concept_id", "si_dimension_packed",
        "lexicon_fingerprint", "source_id", "record_id", "source_revision", "canonical_decimal_spelling", "si_offset",
        "si_prefixable", "memberships", "concept_ids", "subjects", "expansion_word_ids", "unknown_future"};
    private static final char[] VALID_FIRST = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final char[] VALID_TAIL = "abcdefghijklmnopqrstuvwxyz0123456789_".toCharArray();
    private static final char[] INVALID = {'A', 'Z', '-', '.', ' ', '"', '\\', '/', 0x00, 0x1f, 0x7f, 0xe9, 0xff,
        0x100, 0x939, 0x4e00, 0xd83d, 0xde00, 0xd800, 0xdfff, 0xfffe, 0xffff};

    private final MessageDigest trace;
    private final SplittableRandom random;
    private long parses, accepted, refused, mismatches;

    private FoldAuditPayloadDifferential(SplittableRandom random) throws NoSuchAlgorithmException {
        this.random = random;
        this.trace = MessageDigest.getInstance("SHA-256");
    }

    public static void main(String[] args) throws Exception {
        FoldAuditPayloadDifferential run =
                new FoldAuditPayloadDifferential(new SplittableRandom(Long.decode(args[1])));
        run.sweep();
        for (int i = 0; i < Integer.parseInt(args[0]); i++) run.round();
        System.out.printf("REFERENCE mismatches=%d%n", run.mismatches);
        System.out.printf("TRACE parses=%d accepted=%d refused=%d sha256=%s%n", run.parses, run.accepted,
                run.refused, HexFormat.of().formatHex(run.trace.digest()));
        if (args.length > 2 && args[2].equals("timing")) timing();
    }

    /** Every UTF-16 unit alone and after "a" as the only field name. */
    private void sweep() {
        for (int unit = 0; unit <= Character.MAX_VALUE; unit++) {
            String one = String.valueOf((char) unit);
            probe(one, escapeAll(one), "1");
            probe("a" + one, "a" + escapeAll(one), "1");
        }
    }

    private void round() {
        int fields = random.nextInt(7);
        List<String> names = new ArrayList<>();
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < fields; i++) {
            String name = name();
            if (i > 0) json.append(random.nextInt(4) == 0 ? " , " : ",");
            json.append('"').append(spell(name)).append('"').append(random.nextInt(4) == 0 ? " : " : ":").append(value());
            names.add(name);
        }
        json.append('}');
        if (random.nextInt(16) == 0) json.append(random.nextBoolean() ? " " : " trailing");
        record(json.toString());
        if (fields == 1) probe(names.get(0), spell(names.get(0)), value());
    }

    private String name() {
        switch (random.nextInt(6)) {
            case 0: return KNOWN[random.nextInt(KNOWN.length)];
            case 1: {
                StringBuilder name = new StringBuilder();
                int length = 1 + random.nextInt(12);
                for (int i = 0; i < length; i++) name.append(pick(i == 0 ? VALID_FIRST : VALID_TAIL));
                return name.toString();
            }
            case 2: return "";
            case 3: return pick(INVALID) + validTail();
            case 4: return validTail() + pick(INVALID) + validTail();
            default: return validTail() + pick(INVALID);
        }
    }

    private String validTail() {
        StringBuilder name = new StringBuilder();
        int length = random.nextInt(5);
        for (int i = 0; i < length; i++) name.append(pick(VALID_TAIL));
        return name.toString();
    }

    private char pick(char[] pool) {
        return pool[random.nextInt(pool.length)];
    }

    /** JSON spelling of a name without its quotes; some ordinary letters are escaped so the decoded name is what the check sees. */
    private String spell(String name) {
        StringBuilder out = new StringBuilder();
        for (int at = 0; at < name.length(); at++) {
            char current = name.charAt(at);
            boolean mustEscape = current < 0x20 || current == '"' || current == '\\' || current > 0x7e;
            if (mustEscape || random.nextInt(8) == 0) out.append(escape(current));
            else out.append(current);
        }
        return out.toString();
    }

    private static String escapeAll(String text) {
        StringBuilder out = new StringBuilder();
        for (int at = 0; at < text.length(); at++) out.append(escape(text.charAt(at)));
        return out.toString();
    }

    private static String escape(char unit) {
        return String.format("\\u%04x", (int) unit);
    }

    private String value() {
        switch (random.nextInt(12)) {
            case 0: return Long.toString(random.nextLong());
            case 1: return Integer.toString(random.nextInt());
            case 2: return random.nextBoolean() ? "9223372036854775807" : "-9223372036854775808";
            case 3: return "9223372036854775808";
            case 4: return random.nextBoolean() ? "1.25e2" : "-0.0";
            case 5: return random.nextBoolean() ? "true" : "false";
            case 6: return "\"text" + random.nextInt(100) + "\"";
            case 7: return "[" + random.nextInt(100) + "," + random.nextLong() + "]";
            case 8: return "[]";
            case 9: return "null";
            case 10: return random.nextBoolean() ? "[1.5]" : "{}";
            default: return random.nextBoolean() ? "1e999" : "2147483648";
        }
    }

    /** A single-field payload whose decoded name decides the field-name verdict. */
    private void probe(String name, String spelledName, String value) {
        String outcome = record("{" + "\"" + spelledName + "\":" + value + "}");
        boolean nameRefused = outcome.startsWith("ERR java.lang.IllegalArgumentException: " + NAME_ERROR);
        if (nameRefused == name.matches(REGEX)) mismatches++;
    }

    private String record(String json) {
        parses++;
        String outcome;
        try {
            outcome = "OK " + M3PrecomputePayload.parse(json).canonicalJson();
            accepted++;
        } catch (RuntimeException failure) {
            outcome = "ERR " + failure.getClass().getName() + ": " + failure.getMessage();
            refused++;
        }
        trace.update(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        trace.update((byte) 0);
        trace.update(outcome.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        trace.update((byte) '\n');
        return outcome;
    }

    private static volatile Object sink;

    /** Reported, not compared: the restored behaviour is one compiled Pattern per class instead of one per field. */
    private static void timing() {
        for (int i = 0; i < 20_000; i++) sink = M3PrecomputePayload.parse(BENCH);
        long[] ns = new long[9];
        for (int round = 0; round < ns.length; round++) {
            long start = System.nanoTime();
            for (int i = 0; i < 20_000; i++) sink = M3PrecomputePayload.parse(BENCH);
            ns[round] = (System.nanoTime() - start) / 20_000;
        }
        Arrays.sort(ns);
        long bytes = -1;
        java.lang.management.ThreadMXBean bean = java.lang.management.ManagementFactory.getThreadMXBean();
        if (bean instanceof com.sun.management.ThreadMXBean allocation && allocation.isThreadAllocatedMemorySupported()) {
            allocation.setThreadAllocatedMemoryEnabled(true);
            long thread = Thread.currentThread().threadId();
            long[] samples = new long[7];
            for (int sample = 0; sample < samples.length; sample++) {
                long before = allocation.getThreadAllocatedBytes(thread);
                for (int i = 0; i < 1024; i++) sink = M3PrecomputePayload.parse(BENCH);
                samples[sample] = (allocation.getThreadAllocatedBytes(thread) - before) / 1024;
            }
            Arrays.sort(samples);
            bytes = samples[3];
        }
        System.out.printf("TIMING parse_ns_median=%d parse_ns_min=%d alloc_bytes_per_parse_median=%d payload=%s%n",
                ns[4], ns[0], bytes, BENCH);
    }
}
