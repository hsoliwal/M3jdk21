/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Frozen predicate vocabulary with explicit inverse/opposite and algebraic contracts.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatPredicateLexicon.java}
 * blob {@code 7697533102c662f197c2e0639d61c632ffd00b7c} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatPredicateLexicon {
    private final M3MatStaticLexicon lexicon;
    private final int[] inverseIds;
    private final int[] oppositeIds;

    private M3MatPredicateLexicon(
            M3MatStaticLexicon lexicon,
            int[] inverseIds,
            int[] oppositeIds) {
        this.lexicon = lexicon;
        this.inverseIds = inverseIds;
        this.oppositeIds = oppositeIds;
    }

    public static M3MatPredicateLexicon compile(Collection<Entry> entries) {
        Objects.requireNonNull(entries, "entries");
        List<Entry> normalized = new ArrayList<>(entries.size());
        List<M3MatStaticLexicon.Entry> lexiconEntries = new ArrayList<>(entries.size());
        Map<String, Entry> byName = new HashMap<>();
        for (Entry raw : entries) {
            Objects.requireNonNull(raw, "entry");
            Entry entry = new Entry(
                    normalize(raw.name()),
                    M3MatPredicateFlags.requireKnown(raw.flags()),
                    normalizeNullable(raw.inverse()),
                    normalizeNullable(raw.opposite()));
            Entry previous = byName.putIfAbsent(entry.name(), entry);
            if (previous != null) {
                if (!previous.equals(entry)) {
                    throw new IllegalArgumentException(
                            "Conflicting predicate contract for " + entry.name());
                }
                continue;
            }
            normalized.add(entry);
            lexiconEntries.add(new M3MatStaticLexicon.Entry(entry.name(), 0L, entry.flags()));
        }

        M3MatStaticLexicon lexicon = M3MatStaticLexicon.compile("predicate", lexiconEntries);
        int[] inverse = new int[lexicon.size()];
        int[] opposite = new int[lexicon.size()];
        java.util.Arrays.fill(inverse, -1);
        java.util.Arrays.fill(opposite, -1);

        for (Entry entry : normalized) {
            int id = lexicon.requireId(entry.name());
            if (entry.inverse() != null) {
                inverse[id] = lexicon.requireId(entry.inverse());
            }
            if (entry.opposite() != null) {
                opposite[id] = lexicon.requireId(entry.opposite());
            }
        }
        return new M3MatPredicateLexicon(lexicon, inverse, opposite);
    }

    public static M3MatPredicateLexicon builtins() {
        return Builtins.INSTANCE;
    }

    public int size() {
        return lexicon.size();
    }

    public int idOf(String name) {
        return lexicon.idOf(normalize(name));
    }

    public int requireId(String name) {
        return lexicon.requireId(normalize(name));
    }

    public String name(int id) {
        return lexicon.token(id);
    }

    public int flags(int id) {
        return Math.toIntExact(lexicon.flags(id));
    }

    public boolean hasFlag(int id, int flag) {
        return (flags(id) & flag) != 0;
    }

    public int inverseId(int id) {
        return inverseIds[checkedId(id)];
    }

    public int oppositeId(int id) {
        return oppositeIds[checkedId(id)];
    }

    private int checkedId(int id) {
        return Objects.checkIndex(id, inverseIds.length);
    }

    private static String normalize(String value) {
        String name = Objects.requireNonNull(value, "name").strip();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("predicate name must not be blank");
        }
        return name.toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        return value == null ? null : normalize(value);
    }

    public record Entry(String name, int flags, String inverse, String opposite) {
        public Entry {
            Objects.requireNonNull(name, "name");
        }
    }

    private static final class Builtins {
        private static final M3MatPredicateLexicon INSTANCE = compile(List.of(
                new Entry(
                        "EQUALS",
                        M3MatPredicateFlags.SYMMETRIC
                                | M3MatPredicateFlags.TRANSITIVE
                                | M3MatPredicateFlags.REFLEXIVE,
                        "EQUALS",
                        "NOT_EQUALS"),
                new Entry(
                        "NOT_EQUALS",
                        M3MatPredicateFlags.SYMMETRIC,
                        "NOT_EQUALS",
                        "EQUALS"),
                new Entry("LESS_THAN", 0, "GREATER_THAN", "GREATER_OR_EQUAL"),
                new Entry("GREATER_THAN", 0, "LESS_THAN", "LESS_OR_EQUAL"),
                new Entry("LESS_OR_EQUAL", 0, "GREATER_OR_EQUAL", "GREATER_THAN"),
                new Entry("GREATER_OR_EQUAL", 0, "LESS_OR_EQUAL", "LESS_THAN"),
                new Entry("IS_A", M3MatPredicateFlags.TRANSITIVE, null, null),
                new Entry("PART_OF", M3MatPredicateFlags.TRANSITIVE, null, null),
                new Entry("IMPLIES", M3MatPredicateFlags.TRANSITIVE, null, null),
                new Entry("HAS_UNIT", 0, null, null),
                new Entry("SUPPORTS", 0, null, null),
                new Entry("CONTRADICTS", M3MatPredicateFlags.SYMMETRIC, "CONTRADICTS", null)));
    }
}
