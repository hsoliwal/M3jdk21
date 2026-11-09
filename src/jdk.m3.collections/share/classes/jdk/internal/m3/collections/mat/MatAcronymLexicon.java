/* *****************************************************************************
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
package jdk.internal.m3.collections.mat;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Frozen acronym spellings mapped to caller-owned concept coordinates.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatAcronymLexicon.java}
 * blob {@code fbb4d9f05f7634065aaedfb6af1b244f7e0313e7} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatAcronymLexicon {
    private final MatStaticLexicon lexicon;

    private MatAcronymLexicon(MatStaticLexicon lexicon) {
        this.lexicon = lexicon;
    }

    public static MatAcronymLexicon compile(Collection<Entry> entries) {
        Objects.requireNonNull(entries, "entries");
        List<MatStaticLexicon.Entry> canonical = entries.stream()
                .map(entry -> {
                    Objects.requireNonNull(entry, "entry");
                    return new MatStaticLexicon.Entry(
                            normalize(entry.acronym()),
                            entry.conceptId(),
                            entry.flags());
                })
                .toList();
        return new MatAcronymLexicon(MatStaticLexicon.compile("acronym", canonical));
    }

    public int size() {
        return lexicon.size();
    }

    public int idOf(String acronym) {
        return lexicon.idOf(normalize(acronym));
    }

    public int requireId(String acronym) {
        return lexicon.requireId(normalize(acronym));
    }

    public String acronym(int id) {
        return lexicon.token(id);
    }

    public long conceptId(int id) {
        return lexicon.semanticId(id);
    }

    public long flags(int id) {
        return lexicon.flags(id);
    }

    private static String normalize(String value) {
        String acronym = Objects.requireNonNull(value, "acronym").strip();
        if (acronym.isEmpty()) {
            throw new IllegalArgumentException("acronym must not be blank");
        }
        return acronym.toUpperCase(Locale.ROOT);
    }

    public record Entry(String acronym, long conceptId, long flags) {
        public Entry {
            Objects.requireNonNull(acronym, "acronym");
        }
    }
}
