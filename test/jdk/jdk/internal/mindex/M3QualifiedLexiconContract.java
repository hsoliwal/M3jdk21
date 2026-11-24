/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details.
 */

/*
 * @test
 * @summary Qualified backing reuse honors exact UTF-16, owner generations, and shared key ABI
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm M3QualifiedLexiconContract
 */
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jdk.internal.mindex.M3LexiconKey;
import jdk.internal.mindex.M3OccurrenceKey;
import jdk.internal.mindex.M3QualifiedLexicon;
import jdk.internal.mindex.M3StringBacking;

public final class M3QualifiedLexiconContract {
    private static long checks;

    private M3QualifiedLexiconContract() { }

    public static void main(String[] arguments) throws Exception {
        StoredText backing = new StoredText(Map.of(
                11L, "alpha\ud83d\ude03z",
                12L, "alpha\ud83d\ude03z",
                13L, "alpha\ud83d\ude03x",
                14L, "beta",
                15L, "\ud800z\udc00",
                16L, "\ud800z\udc00",
                17L, "\ud800z\udc01"));
        M3QualifiedLexicon lexicon = new M3QualifiedLexicon(backing);
        long namespace = M3LexiconKey.newNamespace();
        M3LexiconKey key = new M3LexiconKey(
                M3LexiconKey.Domain.CLASS, namespace, 42L, 1L, 777L);
        M3QualifiedLexicon.Entry first = lexicon.intern(key, 11L);
        same("backing id", first.backingId(), 11L);
        same("UTF-16 length", first.utf16Length(), "alpha\ud83d\ude03z".length());
        same("find stable identity", lexicon.find(key).orElseThrow() == first, true);
        same("canonical identical text", lexicon.intern(key, 12L) == first, true);
        same("canonical same coordinate", lexicon.intern(key, 11L) == first, true);
        same("one canonical entry", lexicon.size(), 1);
        expect(IllegalArgumentException.class, () -> lexicon.intern(key, 13L));
        expect(IllegalArgumentException.class, () -> lexicon.intern(key, 14L));
        same("collision retains incumbent", lexicon.find(key).orElseThrow() == first, true);

        M3LexiconKey differentQualifier = new M3LexiconKey(
                M3LexiconKey.Domain.CLASS, namespace, 43L, 1L, 777L);
        M3LexiconKey differentSchema = new M3LexiconKey(
                M3LexiconKey.Domain.CLASS, namespace, 42L, 2L, 777L);
        M3LexiconKey differentDomain = new M3LexiconKey(
                M3LexiconKey.Domain.PATH, namespace, 42L, 1L, 777L);
        M3LexiconKey differentNamespace = new M3LexiconKey(
                M3LexiconKey.Domain.CLASS, M3LexiconKey.newNamespace(), 42L, 1L, 777L);
        M3QualifiedLexicon.Entry qualifierEntry = lexicon.intern(differentQualifier, 12L);
        M3QualifiedLexicon.Entry schemaEntry = lexicon.intern(differentSchema, 12L);
        M3QualifiedLexicon.Entry domainEntry = lexicon.intern(differentDomain, 12L);
        M3QualifiedLexicon.Entry namespaceEntry = lexicon.intern(differentNamespace, 12L);
        same("qualifier disambiguates", qualifierEntry != first, true);
        same("schema disambiguates", schemaEntry != first, true);
        same("domain disambiguates", domainEntry != first, true);
        same("namespace disambiguates", namespaceEntry != first, true);
        same("entry IDs distinct", qualifierEntry.entryId() != first.entryId(), true);
        same("entry IDs allocated without losing writers", qualifierEntry.entryId(), 2L);
        same("qualified entry count", lexicon.size(), 5);
        same("unknown key not fabricated", lexicon.find(new M3LexiconKey(
                M3LexiconKey.Domain.AST, namespace, 42L, 1L, 777L)).isEmpty(), true);

        M3OccurrenceKey occurrence = lexicon.occurrence(first, 900L, 7L);
        M3OccurrenceKey otherOwner = lexicon.occurrence(first, 901L, 7L);
        M3OccurrenceKey otherKey = lexicon.occurrence(qualifierEntry, 900L, 7L);
        same("occurrence includes qualified entry", occurrence.lexiconEntry(), key);
        same("owner disambiguates", !occurrence.equals(otherOwner), true);
        same("content disambiguates", !occurrence.equals(otherKey), true);
        same("initial generation", occurrence.generation(), 0L);
        same("current occurrence", lexicon.isCurrent(first, occurrence), true);
        same("wrong key rejected", lexicon.isCurrent(qualifierEntry, occurrence), false);
        same("new generation", lexicon.invalidateOwner(900L), 1L);
        same("old occurrence stale", lexicon.isCurrent(first, occurrence), false);
        same("unrelated owner current", lexicon.isCurrent(first, otherOwner), true);
        same("other key same owner stale", lexicon.isCurrent(qualifierEntry, otherKey), false);
        M3OccurrenceKey updated = lexicon.occurrence(first, 900L, 7L);
        same("new owner version", updated.generation(), 1L);
        same("new occurrence current", lexicon.isCurrent(first, updated), true);
        same("immutable entry stable", lexicon.intern(key, 12L) == first, true);

        expect(IllegalArgumentException.class, () -> lexicon.generation(-1L));
        expect(IllegalArgumentException.class, () -> lexicon.invalidateOwner(-1L));
        expect(IllegalArgumentException.class, () -> lexicon.occurrence(first, -1L, 0L));
        expect(IllegalArgumentException.class, () -> lexicon.occurrence(first, 1L, -1L));
        expect(NullPointerException.class, () -> lexicon.intern(null, 11L));
        expect(NullPointerException.class, () -> lexicon.find(null));
        expect(NullPointerException.class, () -> new M3LexiconKey(null, 0L, 0L, 1L, 0L));
        expect(IllegalArgumentException.class, () -> new M3LexiconKey(
                M3LexiconKey.Domain.STRING, -1L, 0L, 1L, 0L));
        expect(IllegalArgumentException.class, () -> new M3LexiconKey(
                M3LexiconKey.Domain.STRING, 0L, 0L, 0L, 0L));
        expect(NullPointerException.class, () -> new M3OccurrenceKey(null, 0L, 0L, 0L));
        expect(IllegalArgumentException.class, () -> new M3OccurrenceKey(key, -1L, 0L, 0L));

        M3QualifiedLexicon foreign = new M3QualifiedLexicon(backing);
        M3QualifiedLexicon.Entry foreignEntry = foreign.intern(key, 11L);
        expect(IllegalArgumentException.class, () -> lexicon.occurrence(foreignEntry, 1L, 1L));
        expect(IllegalArgumentException.class, () -> lexicon.isCurrent(foreignEntry, occurrence));
        same("separate registry stable internal entries", foreignEntry != first, true);

        M3LexiconKey surrogateKey = new M3LexiconKey(
                M3LexiconKey.Domain.STRING, namespace, 0L, 1L, 0xabcdL);
        M3QualifiedLexicon.Entry surrogate = lexicon.intern(surrogateKey, 15L);
        same("unpaired surrogates length", surrogate.utf16Length(), 3);
        same("equal unpaired surrogates", lexicon.intern(surrogateKey, 16L) == surrogate, true);
        expect(IllegalArgumentException.class, () -> lexicon.intern(surrogateKey, 17L));

        ExecutorService workers = Executors.newFixedThreadPool(8);
        try {
            List<Callable<M3QualifiedLexicon.Entry>> jobs = new ArrayList<>();
            for (int i = 0; i < 4_000; i++) {
                final long id = (i & 1) == 0 ? 11L : 12L;
                jobs.add(() -> lexicon.intern(key, id));
            }
            List<Future<M3QualifiedLexicon.Entry>> results = workers.invokeAll(jobs);
            for (Future<M3QualifiedLexicon.Entry> result : results) {
                same("concurrent canonical identity", result.get() == first, true);
            }
        } finally {
            workers.shutdownNow();
        }
        same("concurrent reuse does not allocate entries", lexicon.size(), 6);
        same("entry ID count", surrogate.entryId(), 6L);
        System.out.println("M3QualifiedLexiconContract: " + checks + " checks passed");
    }

    private static void same(String label, Object actual, Object expected) {
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError(label + ": expected=" + expected + " actual=" + actual);
        }
        checks++;
    }

    private interface Throwing {
        void run() throws Exception;
    }

    private static void expect(Class<? extends Throwable> expected, Throwing callback) {
        try {
            callback.run();
        } catch (Throwable exception) {
            same("expected rejection", exception.getClass(), expected);
            return;
        }
        throw new AssertionError("expected " + expected.getName());
    }

    private static final class StoredText implements M3StringBacking {
        private final Map<Long, String> immutable;

        StoredText(Map<Long, String> source) {
            immutable = Map.copyOf(source);
        }

        private String text(long id) {
            String value = immutable.get(id);
            if (value == null) {
                throw new IllegalArgumentException("unknown backing id " + id);
            }
            return value;
        }

        @Override public int length(long id) { return text(id).length(); }
        @Override public int utf8Length(long id) { return text(id).length(); }
        @Override public int codePointCount(long id) {
            return text(id).codePointCount(0, text(id).length());
        }
        @Override public int unpairedSurrogateCount(long id) { return 0; }
        @Override public int hashCode(long id) { return text(id).hashCode(); }
        @Override public char charAt(long id, int index) { return text(id).charAt(index); }
        @Override public CharBuffer utf16View(long id) {
            throw new AssertionError("UTF-16 materialization is forbidden in this oracle");
        }
        @Override public ByteBuffer utf8View(long id) {
            throw new AssertionError("UTF-8 materialization is forbidden in this oracle");
        }
        @Override public String materialize(long id) {
            throw new AssertionError("String materialization is forbidden in this oracle");
        }
        @Override public void close() { }
    }
}
