/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.M3InstanceIndex;
import com.m3.text.SharedLexiconCatalog;

import java.util.List;
import java.util.Map;

public final class M3InstanceIndexTest {
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
        SharedLexiconCatalog.SourceIdentity einstein =
                new SharedLexiconCatalog.SourceIdentity("wikidata", "Q937");
        SharedLexiconCatalog.SourceIdentity paris =
                new SharedLexiconCatalog.SourceIdentity("wikidata", "Q90");
        SharedLexiconCatalog.SourceIdentity moby =
                new SharedLexiconCatalog.SourceIdentity("gutenberg", "10681");
        M3InstanceIndex.InstanceRecord einsteinRecord = new M3InstanceIndex.InstanceRecord(
                einstein, "en", "Albert Einstein", "PERSON", 10500,
                List.of("Einstein", "Albert Einstein", "Einstein"),
                Map.of("occupation", "physicist"), "wikidata-export-1");
        M3InstanceIndex.InstanceRecord parisRecord = new M3InstanceIndex.InstanceRecord(
                paris, "en", "Paris", "PLACE", 70000, List.of("Paris"),
                Map.of("country", "France"), "wikidata-export-1");
        M3InstanceIndex.InstanceRecord mobyRecord = new M3InstanceIndex.InstanceRecord(
                moby, "en", "Moby-Dick", "BOOK_TITLE", 650001, List.of("Moby Dick"),
                Map.of("year", "1851"), "gutenberg-10681");
        M3InstanceIndex.TitleRecord title = new M3InstanceIndex.TitleRecord(
                new SharedLexiconCatalog.SourceIdentity("gutenberg", "title-10681"),
                moby, "en", "Moby-Dick", "BOOK_TITLE",
                List.of("Herman Melville", "Herman Melville"), "gutenberg-10681");
        M3InstanceIndex index = M3InstanceIndex.of(
                List.of(parisRecord, mobyRecord, einsteinRecord), List.of(title));

        check(index.size() == 3);
        check(index.find(einstein).orElseThrow().instanceOfX() == 10500);
        check(index.findByName("en", "Paris").size() == 1);
        check(index.findByName("en", "paris").isEmpty());
        check(index.instancesOf(650001).get(0).identity().equals(moby));
        check(index.titlesFor(moby).size() == 1);
        check(index.titlesFor(moby).get(0).precompute().utf16Units() == 8);
        check(index.find(einstein).orElseThrow().precompute().codePoints() == 14);
        check(index.find(einstein).orElseThrow().aliases().equals(List.of("Albert Einstein", "Einstein")));
        check(index.find(einstein).orElseThrow().metadata().keySet().iterator().next().equals("occupation"));
        expect(UnsupportedOperationException.class,
                () -> index.find(einstein).orElseThrow().aliases().add("x"));
        expect(IllegalArgumentException.class, () -> M3InstanceIndex.of(
                List.of(einsteinRecord, einsteinRecord), List.of()));
        expect(IllegalArgumentException.class, () -> M3InstanceIndex.of(
                List.of(einsteinRecord), List.of(title)));
        expect(IllegalArgumentException.class, () -> new M3InstanceIndex.InstanceRecord(
                einstein, "en", "", "PERSON", 10500, List.of(), Map.of(), "source"));
        System.out.println("M3_INSTANCE_INDEX_CONTRACT_PASS checks=" + checks + "/15");
    }
}
