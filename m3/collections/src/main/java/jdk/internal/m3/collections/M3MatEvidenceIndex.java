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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Frozen evidence rows in primitive columns.
 *
 * <p>Statement hashes narrow candidates; exact coordinates determine support/contradiction.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatEvidenceIndex.java}
 * blob {@code 93dc0ca01445ee73297ae785930276719a0fcb7b} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatEvidenceIndex {
    public static final long DIRECT = 1L;
    public static final long CORPUS = 1L << 1;
    public static final long MEASUREMENT = 1L << 2;
    public static final long DERIVED = 1L << 3;

    private final long[] hashes;
    private final long[] subjects;
    private final int[] predicates;
    private final long[] objects;
    private final long[] contexts;
    private final long[] sourceIds;
    private final int[] confidenceQ31;
    private final long[] evidenceFlags;

    private M3MatEvidenceIndex(List<Evidence> rows) {
        hashes = new long[rows.size()];
        subjects = new long[rows.size()];
        predicates = new int[rows.size()];
        objects = new long[rows.size()];
        contexts = new long[rows.size()];
        sourceIds = new long[rows.size()];
        confidenceQ31 = new int[rows.size()];
        evidenceFlags = new long[rows.size()];
        for (int row = 0; row < rows.size(); row++) {
            Evidence evidence = rows.get(row);
            M3MatStatement statement = evidence.statement();
            hashes[row] = statement.hash64();
            subjects[row] = statement.subject();
            predicates[row] = statement.predicate();
            objects[row] = statement.object();
            contexts[row] = statement.context();
            sourceIds[row] = evidence.sourceId();
            confidenceQ31[row] = evidence.confidenceQ31();
            evidenceFlags[row] = evidence.flags();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public int size() {
        return hashes.length;
    }

    public long sourceId(int row) {
        return sourceIds[Objects.checkIndex(row, hashes.length)];
    }

    public Assessment assess(M3MatStatement target, M3MatPredicateLexicon predicateLexicon) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(predicateLexicon, "predicateLexicon");
        predicateLexicon.flags(target.predicate());

        Aggregate support = exact(target);
        int inverse = predicateLexicon.inverseId(target.predicate());
        if (inverse >= 0) {
            M3MatStatement inverseStatement = target.reverse(inverse);
            if (!inverseStatement.equals(target)) {
                support = support.plus(exact(inverseStatement));
            }
        }

        Aggregate contradiction = Aggregate.EMPTY;
        int opposite = predicateLexicon.oppositeId(target.predicate());
        if (opposite >= 0) {
            M3MatStatement oppositeStatement = new M3MatStatement(
                    target.subject(), opposite, target.object(), target.context());
            contradiction = contradiction.plus(exact(oppositeStatement));
            int inverseOpposite = predicateLexicon.inverseId(opposite);
            if (inverseOpposite >= 0) {
                M3MatStatement inverseOppositeStatement = target.reverse(inverseOpposite);
                if (!inverseOppositeStatement.equals(oppositeStatement)) {
                    contradiction = contradiction.plus(exact(inverseOppositeStatement));
                }
            }
        }

        return new Assessment(
                support.rows(),
                contradiction.rows(),
                support.maxConfidenceQ31(),
                contradiction.maxConfidenceQ31(),
                support.flags(),
                contradiction.flags());
    }

    private Aggregate exact(M3MatStatement statement) {
        long hash = statement.hash64();
        int index = lowerBound(hash);
        int rows = 0;
        int maxConfidence = 0;
        long flags = 0L;
        while (index < hashes.length && Long.compareUnsigned(hashes[index], hash) == 0) {
            if (subjects[index] == statement.subject()
                    && predicates[index] == statement.predicate()
                    && objects[index] == statement.object()
                    && contexts[index] == statement.context()) {
                rows++;
                maxConfidence = Math.max(maxConfidence, confidenceQ31[index]);
                flags |= evidenceFlags[index];
            }
            index++;
        }
        return new Aggregate(rows, maxConfidence, flags);
    }

    private int lowerBound(long hash) {
        int low = 0;
        int high = hashes.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (Long.compareUnsigned(hashes[middle], hash) < 0) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    public record Evidence(
            M3MatStatement statement,
            long sourceId,
            int confidenceQ31,
            long flags) {
        public Evidence {
            Objects.requireNonNull(statement, "statement");
            if (confidenceQ31 < 0) {
                throw new IllegalArgumentException("confidenceQ31 must be non-negative");
            }
        }
    }

    public record Assessment(
            int supportRows,
            int contradictionRows,
            int maxSupportQ31,
            int maxContradictionQ31,
            long supportFlags,
            long contradictionFlags) {
        public Assessment {
            if (supportRows < 0
                    || contradictionRows < 0
                    || maxSupportQ31 < 0
                    || maxContradictionQ31 < 0) {
                throw new IllegalArgumentException("negative evidence assessment");
            }
        }
    }

    public static final class Builder {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public Builder() {
        }

        private final List<Evidence> rows = new ArrayList<>();

        public Builder add(
                M3MatStatement statement,
                long sourceId,
                int confidenceQ31,
                long flags) {
            rows.add(new Evidence(statement, sourceId, confidenceQ31, flags));
            return this;
        }

        public M3MatEvidenceIndex build() {
            List<Evidence> sorted = new ArrayList<>(rows);
            sorted.sort(Comparator
                    .comparingLong((Evidence evidence) -> evidence.statement().hash64())
                    .thenComparingLong(evidence -> evidence.statement().subject())
                    .thenComparingInt(evidence -> evidence.statement().predicate())
                    .thenComparingLong(evidence -> evidence.statement().object())
                    .thenComparingLong(evidence -> evidence.statement().context())
                    .thenComparingLong(Evidence::sourceId));
            // Java's comparingLong is signed. Re-sort hash-equivalent ordering with unsigned hash
            // through a stable comparator that leaves exact row tie-breaks deterministic.
            sorted.sort((left, right) -> {
                int compared = Long.compareUnsigned(
                        left.statement().hash64(),
                        right.statement().hash64());
                if (compared != 0) {
                    return compared;
                }
                compared = Long.compare(left.statement().subject(), right.statement().subject());
                if (compared != 0) {
                    return compared;
                }
                compared = Integer.compare(left.statement().predicate(), right.statement().predicate());
                if (compared != 0) {
                    return compared;
                }
                compared = Long.compare(left.statement().object(), right.statement().object());
                if (compared != 0) {
                    return compared;
                }
                compared = Long.compare(left.statement().context(), right.statement().context());
                if (compared != 0) {
                    return compared;
                }
                return Long.compare(left.sourceId(), right.sourceId());
            });
            return new M3MatEvidenceIndex(sorted);
        }
    }

    private record Aggregate(int rows, int maxConfidenceQ31, long flags) {
        private static final Aggregate EMPTY = new Aggregate(0, 0, 0L);

        private Aggregate plus(Aggregate other) {
            return new Aggregate(
                    Math.addExact(rows, other.rows),
                    Math.max(maxConfidenceQ31, other.maxConfidenceQ31),
                    flags | other.flags);
        }
    }
}
