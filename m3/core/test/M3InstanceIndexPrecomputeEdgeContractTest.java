/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3InstanceIndexPrecompute;
import jdk.m3.text.M3InstanceIndexPrecompute.InstanceRecord;
import jdk.m3.text.M3InstanceIndexPrecompute.InstanceType;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Identity, normalization, UTF-8 ordering and refusal contract for the instance-index precompute
 * owner, including inputs from non-conforming collections that report the same key or element
 * twice.
 */
public final class M3InstanceIndexPrecomputeEdgeContractTest {
    private static int checks;

    private M3InstanceIndexPrecomputeEdgeContractTest() {}

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
                    && (fragment == null
                            || String.valueOf(thrown.getMessage()).contains(fragment))) {
                return;
            }
            throw new AssertionError(
                    "wrong refusal, wanted "
                            + type.getSimpleName()
                            + " containing '"
                            + fragment
                            + "'",
                    thrown);
        }
        throw new AssertionError(
                "missing " + type.getSimpleName() + " containing '" + fragment + "'");
    }

    /** A collection view whose iterator yields exactly the listed values, duplicates included. */
    private static <T> Set<T> repeatingSet(List<T> values) {
        return new AbstractSet<>() {
            @Override
            public Iterator<T> iterator() {
                return values.iterator();
            }

            @Override
            public int size() {
                return values.size();
            }
        };
    }

    /**
     * A map view whose forEach/entrySet reports the given entries verbatim, duplicate keys
     * included.
     */
    private static <K, V> Map<K, V> repeatingMap(List<Map.Entry<K, V>> entries) {
        return new AbstractMap<>() {
            @Override
            public Set<Map.Entry<K, V>> entrySet() {
                return repeatingSet(entries);
            }
        };
    }

    private static M3InstanceIndexPrecompute index(
            Set<Long> concepts, Map<String, InstanceRecord> records) {
        return new M3InstanceIndexPrecompute("src", "rev", "fp", "norm-v1", concepts, records);
    }

    public static void main(String[] args) {
        identityAndViews();
        utf8OrderAndNormalization();
        malformedProbesUseTheByteOrder();
        recordRefusals();
        indexRefusals();
        System.out.println("M3_INSTANCE_INDEX_EDGE_CONTRACT_PASS checks=" + checks);
    }

    private static void identityAndViews() {
        Map<String, InstanceRecord> records = new LinkedHashMap<>();
        records.put("zeta", new InstanceRecord("Zeta", InstanceType.PLACE, 7L, Map.of()));
        records.put("alpha", new InstanceRecord("ALPHA", InstanceType.PERSON, 7L, Map.of()));
        records.put("event one", new InstanceRecord("Event One", InstanceType.EVENT, 9L, Map.of()));
        M3InstanceIndexPrecompute index = index(new LinkedHashSet<>(List.of(9L, 7L, 13L)), records);
        check(
                index.sourceId().equals("src")
                        && index.sourceRevision().equals("rev")
                        && index.sourceFingerprint().equals("fp")
                        && index.normalizationVersion().equals("norm-v1"),
                "identity accessors");
        check(
                List.copyOf(index.frozenConceptX()).equals(List.of(7L, 9L, 13L)),
                "frozen concepts sorted");
        Map<Long, Set<String>> byConcept = index.namesByConceptX();
        check(byConcept.keySet().equals(Set.of(7L, 9L)), "only referenced concepts are indexed");
        check(
                List.copyOf(byConcept.get(7L)).equals(List.of("alpha", "zeta")),
                "normalized names per concept");
        check(index.referencedConceptCount() == 2 && index.size() == 3, "sizes");
        expect(UnsupportedOperationException.class, null, () -> byConcept.put(1L, Set.of()));
        expect(UnsupportedOperationException.class, null, () -> byConcept.get(7L).add("x"));
        check(index.appliesTo("src", "rev", "fp", "norm-v1"), "matching identity");
        check(!index.appliesTo("other", "rev", "fp", "norm-v1"), "source differs");
        check(!index.appliesTo("src", "other", "fp", "norm-v1"), "revision differs");
        check(!index.appliesTo("src", "rev", "other", "norm-v1"), "fingerprint differs");
        check(!index.appliesTo("src", "rev", "fp", "norm-v2"), "normalization differs");
        check(index.findByName("EVENT ONE").type() == InstanceType.EVENT, "case-folded lookup");
        check(index.findByName("missing") == null, "absent lookup");
        check(index.instancesOf(13L).isEmpty(), "frozen but unreferenced concept");
        check(index.instancesOf(42L).isEmpty(), "unknown concept");
        check(
                index.instancesOf(7L).stream()
                        .map(InstanceRecord::nameUtf8)
                        .toList()
                        .equals(List.of("ALPHA", "Zeta")),
                "instances in normalized-name order");
    }

    private static void utf8OrderAndNormalization() {
        check(
                M3InstanceIndexPrecompute.normalizedInstanceName("ÀBC Z").equals("Àbc z"),
                "only ASCII letters fold");
        String replacement = "�"; // UTF-8 EF BF BD
        String emoji = "😀"; // UTF-8 F0 9F 98 80; UTF-16 sorts it first
        Map<String, InstanceRecord> records = new LinkedHashMap<>();
        records.put(emoji, new InstanceRecord(emoji, InstanceType.CHARACTER, 3L, Map.of()));
        records.put(
                replacement, new InstanceRecord(replacement, InstanceType.CHARACTER, 3L, Map.of()));
        records.put("abc", new InstanceRecord("abc", InstanceType.AUTHOR, 3L, Map.of()));
        records.put("ab", new InstanceRecord("ab", InstanceType.AUTHOR, 3L, Map.of()));
        M3InstanceIndexPrecompute index = index(Set.of(3L), records);
        check(
                List.copyOf(index.recordsByName().keySet())
                        .equals(List.of("ab", "abc", replacement, emoji)),
                "unsigned UTF-8 byte order with shorter prefix first");
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put(emoji, "v1");
        metadata.put("b", "v2 " + emoji);
        metadata.put(replacement, "v3");
        InstanceRecord withMetadata =
                new InstanceRecord("Name", InstanceType.ORGANIZATION, 3L, metadata);
        check(
                List.copyOf(withMetadata.metadata().keySet())
                        .equals(List.of("b", replacement, emoji)),
                "metadata keys in UTF-8 order");
        metadata.clear();
        check(withMetadata.metadata().size() == 3, "metadata is copied");
        expect(
                UnsupportedOperationException.class,
                null,
                () -> withMetadata.metadata().put("x", "y"));
    }

    /**
     * Lookup probes are not validated, so malformed UTF-16 and null reach the comparator. Those take
     * its byte path: a probe matches a key exactly when their String.getBytes(UTF_8) forms are equal
     * (an unpaired surrogate encodes as '?'), and a null probe is refused.
     */
    private static void malformedProbesUseTheByteOrder() {
        String key = "a?b";
        Map<String, String> metadata =
                new InstanceRecord("Name", InstanceType.ORGANIZATION, 3L, Map.of(key, "v")).metadata();
        byte[] keyBytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (String probe : List.of("a\uD800b", "a\uD800", "\uDC00", "a\uDBFFb\uD800")) {
            boolean sameBytes = java.util.Arrays.equals(
                    probe.getBytes(java.nio.charset.StandardCharsets.UTF_8), keyBytes);
            check(metadata.containsKey(probe) == sameBytes, "byte-order lookup of a malformed probe");
        }
        check(metadata.containsKey("a\uD800b"), "unpaired surrogate compares as its '?' encoding");
        check(!metadata.containsKey("a\uD800"), "equal bytes, shorter probe");
        check(!metadata.containsKey("\uDC00"), "first byte differs");
        expect(NullPointerException.class, null, () -> metadata.containsKey(null));
    }

    private static void recordRefusals() {
        expect(
                IllegalArgumentException.class,
                "name must be nonblank UTF-8",
                () -> new InstanceRecord(null, InstanceType.DATE, 1L, Map.of()));
        expect(
                IllegalArgumentException.class,
                "name must be nonblank UTF-8",
                () -> new InstanceRecord("", InstanceType.DATE, 1L, Map.of()));
        for (String blank : List.of(" ", "\t", "\r", "\n", " \t\r\n")) {
            expect(
                    IllegalArgumentException.class,
                    "name must be nonblank UTF-8",
                    () -> new InstanceRecord(blank, InstanceType.DATE, 1L, Map.of()));
        }
        expect(
                IllegalArgumentException.class,
                "name contains an ASCII control",
                () -> new InstanceRecord("a\tb", InstanceType.DATE, 1L, Map.of()));
        expect(
                IllegalArgumentException.class,
                "name contains an ASCII control",
                () -> new InstanceRecord(" \u0001", InstanceType.DATE, 1L, Map.of()));
        expect(
                IllegalArgumentException.class,
                "name contains an unpaired surrogate",
                () -> new InstanceRecord("a\uD800", InstanceType.DATE, 1L, Map.of()));
        expect(
                IllegalArgumentException.class,
                "name contains an unpaired surrogate",
                () -> new InstanceRecord("\uD800a", InstanceType.DATE, 1L, Map.of()));
        expect(
                IllegalArgumentException.class,
                "name contains an unpaired surrogate",
                () -> new InstanceRecord("\uDC00a", InstanceType.DATE, 1L, Map.of()));
        expect(
                NullPointerException.class,
                "type",
                () -> new InstanceRecord("a", null, 1L, Map.of()));
        expect(
                IllegalArgumentException.class,
                "instance_of_x must be positive",
                () -> new InstanceRecord("a", InstanceType.DATE, 0L, Map.of()));
        expect(
                NullPointerException.class,
                "metadata",
                () -> new InstanceRecord("a", InstanceType.DATE, 1L, null));
        Map<String, String> nullValue = new HashMap<>();
        nullValue.put("k", null);
        expect(
                IllegalArgumentException.class,
                "metadata value is null",
                () -> new InstanceRecord("a", InstanceType.DATE, 1L, nullValue));
        expect(
                IllegalArgumentException.class,
                "metadata key must be nonblank UTF-8",
                () -> new InstanceRecord("a", InstanceType.DATE, 1L, Map.of(" ", "v")));
        expect(
                IllegalArgumentException.class,
                "metadata value contains an unpaired surrogate",
                () -> new InstanceRecord("a", InstanceType.DATE, 1L, Map.of("k", "\uDFFF")));
        expect(
                IllegalArgumentException.class,
                "duplicate metadata key",
                () ->
                        new InstanceRecord(
                                "a",
                                InstanceType.DATE,
                                1L,
                                repeatingMap(
                                        List.of(
                                                Map.entry("k", "first"),
                                                Map.entry("k", "second")))));
    }

    private static void indexRefusals() {
        Map<String, InstanceRecord> none = Map.of();
        for (String blank : new String[] {null, "", " \t"}) {
            expect(
                    IllegalArgumentException.class,
                    "sourceId is blank",
                    () -> new M3InstanceIndexPrecompute(blank, "r", "f", "n", Set.of(1L), none));
        }
        expect(
                IllegalArgumentException.class,
                "sourceRevision is blank",
                () -> new M3InstanceIndexPrecompute("s", " ", "f", "n", Set.of(1L), none));
        expect(
                IllegalArgumentException.class,
                "sourceFingerprint is blank",
                () -> new M3InstanceIndexPrecompute("s", "r", null, "n", Set.of(1L), none));
        expect(
                IllegalArgumentException.class,
                "normalizationVersion is blank",
                () -> new M3InstanceIndexPrecompute("s", "r", "f", "", Set.of(1L), none));
        expect(NullPointerException.class, "frozenConceptX", () -> index(null, none));
        Set<Long> withNull = new LinkedHashSet<>();
        withNull.add(null);
        expect(IllegalArgumentException.class, "unique positives", () -> index(withNull, none));
        expect(IllegalArgumentException.class, "unique positives", () -> index(Set.of(0L), none));
        expect(IllegalArgumentException.class, "unique positives", () -> index(Set.of(-5L), none));
        expect(
                IllegalArgumentException.class,
                "unique positives",
                () -> index(repeatingSet(List.of(5L, 5L)), none));
        expect(NullPointerException.class, "recordsByName", () -> index(Set.of(1L), null));
        InstanceRecord alice = new InstanceRecord("Alice", InstanceType.PERSON, 1L, Map.of());
        Map<String, InstanceRecord> nullKey = new HashMap<>();
        nullKey.put(null, alice);
        expect(NullPointerException.class, "record name key", () -> index(Set.of(1L), nullKey));
        Map<String, InstanceRecord> nullRecord = new HashMap<>();
        nullRecord.put("alice", null);
        expect(NullPointerException.class, "record", () -> index(Set.of(1L), nullRecord));
        expect(
                IllegalArgumentException.class,
                "record key does not match normalized name",
                () -> index(Set.of(1L), Map.of("bob", alice)));
        expect(
                IllegalArgumentException.class,
                "instance_of_x is not frozen",
                () -> index(Set.of(2L), Map.of("alice", alice)));
        Map<String, InstanceRecord> caseTwins = new LinkedHashMap<>();
        caseTwins.put("alice", alice);
        caseTwins.put("ALICE", new InstanceRecord("ALICE", InstanceType.AUTHOR, 1L, Map.of()));
        expect(
                IllegalArgumentException.class,
                "duplicate normalized instance name",
                () -> index(Set.of(1L), caseTwins));
        expect(
                IllegalArgumentException.class,
                "name must be nonblank UTF-8",
                () -> M3InstanceIndexPrecompute.normalizedInstanceName("\n"));
    }
}
