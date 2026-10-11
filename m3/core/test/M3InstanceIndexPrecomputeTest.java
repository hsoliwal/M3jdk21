/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3InstanceIndexPrecompute;
import com.m3.text.M3InstanceIndexPrecompute.InstanceRecord;
import com.m3.text.M3InstanceIndexPrecompute.InstanceType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class M3InstanceIndexPrecomputeTest {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("check " + checks);
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
            throw new AssertionError("missing " + type.getName());
        } catch (Throwable failure) {
            check(type.isInstance(failure));
        }
    }

    public static void main(String[] args) {
        Set<Long> concepts = new LinkedHashSet<>(List.of(7L, 11L));
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("authority", "fixture");
        InstanceRecord paris = new InstanceRecord(
                "Paris", InstanceType.PLACE, 7L, metadata);
        InstanceRecord alice = new InstanceRecord(
                "Alice", InstanceType.PERSON, 7L, Map.of("authority", "fixture"));
        InstanceRecord book = new InstanceRecord(
                "The Book", InstanceType.BOOK_TITLE, 11L, Map.of());

        Map<String, InstanceRecord> records = new LinkedHashMap<>();
        records.put("paris", paris);
        records.put("alice", alice);
        records.put("the book", book);

        M3InstanceIndexPrecompute index = new M3InstanceIndexPrecompute(
                "unicodex-instance-index", "v1", "sha256:fixture",
                "ascii-fold-utf8-v1", concepts, records);

        check(index.size() == 3);
        check(index.referencedConceptCount() == 2);
        check(index.frozenConceptX().contains(7L));
        check(index.findByName("PARIS").equals(paris));
        check(index.findByName("missing") == null);
        check(index.instancesOf(7L).size() == 2);
        check(index.instancesOf(7L).get(0).nameUtf8().equals("Alice"));
        check(index.instancesOf(7L).get(1).nameUtf8().equals("Paris"));
        check(index.instancesOf(99L).isEmpty());
        check(index.findByName("The Book").type() == InstanceType.BOOK_TITLE);
        check(index.appliesTo("unicodex-instance-index", "v1",
                "sha256:fixture", "ascii-fold-utf8-v1"));
        check(!index.appliesTo("unicodex-instance-index", "v2",
                "sha256:fixture", "ascii-fold-utf8-v1"));
        check(M3InstanceIndexPrecompute.normalizedInstanceName("A\u00e9")
                .equals("a\u00e9"));

        metadata.put("mutated", "must not leak");
        records.put("new", new InstanceRecord(
                "New", InstanceType.PERSON, 7L, Map.of()));
        check(index.findByName("mutated") == null);
        check(index.findByName("new") == null);
        expect(UnsupportedOperationException.class,
                () -> index.recordsByName().put("x", paris));
        expect(UnsupportedOperationException.class,
                () -> index.frozenConceptX().add(99L));
        expect(UnsupportedOperationException.class,
                () -> index.instancesOf(7L).add(paris));
        expect(UnsupportedOperationException.class,
                () -> index.recordsByName().get("paris").metadata().put("x", "y"));

        expect(IllegalArgumentException.class, () ->
                new M3InstanceIndexPrecompute("id", "r", "f", "n",
                        Set.of(7L), Map.of("wrong", paris)));
        expect(IllegalArgumentException.class, () ->
                new M3InstanceIndexPrecompute("id", "r", "f", "n",
                        Set.of(8L), Map.of("paris", paris)));
        expect(IllegalArgumentException.class, () ->
                new InstanceRecord(" ", InstanceType.PERSON, 7L, Map.of()));
        expect(IllegalArgumentException.class, () ->
                new InstanceRecord("bad\u0001", InstanceType.PERSON, 7L, Map.of()));
        expect(IllegalArgumentException.class, () ->
                new InstanceRecord("bad\uD800", InstanceType.PERSON, 7L, Map.of()));
        expect(IllegalArgumentException.class, () ->
                new InstanceRecord("good", InstanceType.PERSON, 0L, Map.of()));

        System.out.println("M3JDK_INSTANCE_INDEX_PRECOMPUTE_PASS checks=" + checks);
    }
}
