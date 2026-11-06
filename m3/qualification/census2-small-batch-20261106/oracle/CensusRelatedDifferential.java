/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import jdk.m3.text.SharedLexiconCatalog;
import jdk.m3.text.SharedRelatedLexemeCatalog;
import jdk.m3.text.SharedRelatedLexemeCatalog.Relation;

/**
 * census2 P2-025 differential: seeded catalogs (relations sorted as open() sorts them, built through the private
 * constructor) and seeded findBySource queries (present, absent between, before and after every source id). TRACE
 * records size, contents, the result class and whether the result is the List.of() instance; it must be identical
 * on the master and edited classes. ALLOC reports the bytes allocated by the empty-range queries (the intent of the
 * leaf; com.sun.management.ThreadMXBean), which may differ.
 * usage: java CensusRelatedDifferential <catalogs> <queries> <seed>
 */
public final class CensusRelatedDifferential {
    public static void main(String[] args) throws Exception {
        int catalogs = Integer.parseInt(args[0]);
        int queries = Integer.parseInt(args[1]);
        SplittableRandom random = new SplittableRandom(Long.decode(args[2]));
        Constructor<SharedRelatedLexemeCatalog> make =
                SharedRelatedLexemeCatalog.class.getDeclaredConstructor(List.class, Map.class);
        make.setAccessible(true);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long empty = 0, nonEmpty = 0;
        List<SharedRelatedLexemeCatalog> built = new ArrayList<>();
        List<List<String>> emptyKeys = new ArrayList<>();
        for (int c = 0; c < catalogs; c++) {
            List<Relation> relations = new ArrayList<>();
            int sources = random.nextInt(6);
            for (int s = 0; s < sources; s++) {
                String source = "s" + (2 * random.nextInt(20) + 1);   // odd ids; even ids are absent
                for (int r = random.nextInt(4); r >= 0; r--) {
                    relations.add(new Relation(source, "r" + random.nextInt(9), "w" + random.nextInt(9),
                            "v" + random.nextInt(9), new SharedLexiconCatalog.Coordinate(random.nextInt(3),
                            random.nextInt(50))));
                }
            }
            relations.sort(Comparator.comparing(Relation::sourceId).thenComparing(Relation::recordId)
                    .thenComparing(Relation::lexeme).thenComparing(Relation::relatedLexeme));
            Map<SharedLexiconCatalog.SourceIdentity, List<Relation>> byIdentity = new HashMap<>();
            for (Relation relation : relations) {
                byIdentity.computeIfAbsent(relation.identity(), k -> new ArrayList<>()).add(relation);
            }
            SharedRelatedLexemeCatalog catalog = make.newInstance(relations, byIdentity);
            List<String> absent = new ArrayList<>();
            for (int q = 0; q < queries; q++) {
                String key = random.nextInt(4) == 0 ? "" : "s" + random.nextInt(42);
                List<Relation> result = catalog.findBySource(key);
                if (result.isEmpty()) {
                    empty++;
                    absent.add(key);
                } else {
                    nonEmpty++;
                }
                String line = c + "/" + key + ":" + result.size() + ":" + result + ":" + result.getClass().getName()
                        + ":" + (result == (Object) List.of());
                digest.update(line.getBytes(StandardCharsets.UTF_8));
            }
            built.add(catalog);
            emptyKeys.add(absent);
        }
        System.out.println("TRACE catalogs=" + catalogs + " queries=" + (empty + nonEmpty) + " empty=" + empty
                + " nonEmpty=" + nonEmpty + " trace_sha256=" + HexFormat.of().formatHex(digest.digest()));
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long sink = 0;
        for (int warm = 0; warm < 20; warm++) sink += emptyRound(built, emptyKeys);
        long id = Thread.currentThread().threadId();
        long before = threads.getThreadAllocatedBytes(id);
        sink += emptyRound(built, emptyKeys);
        long bytes = threads.getThreadAllocatedBytes(id) - before;
        System.out.println("ALLOC empty_queries=" + empty + " bytes=" + bytes + " sink=" + sink);
    }

    private static long emptyRound(List<SharedRelatedLexemeCatalog> built, List<List<String>> keys) {
        long total = 0;
        for (int c = 0; c < built.size(); c++) {
            for (String key : keys.get(c)) total += built.get(c).findBySource(key).size() + 1;
        }
        return total;
    }
}
