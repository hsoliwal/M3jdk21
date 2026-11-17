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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Bounded offline closure over explicitly declared predicate mechanics.
 *
 * <p>No semantic rule is inferred from spelling, similarity, statistics or model output.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code
 * m3-mat-value/src/main/java/jdk/internal/m3/matvalue/LogicEngine.java}
 * blob {@code 4ff6f042abf7d34b63ddd886c865ec31a72772cb} at com.synexia develop {@code
 * 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source
 * modulo names).
 */
public final class LogicEngine {
    private final PredicateLexicon predicates;
    private final int maxRounds;
    private final int maxStatements;

    public LogicEngine(
            PredicateLexicon predicates,
            int maxRounds,
            int maxStatements) {
        this.predicates = Objects.requireNonNull(predicates, "predicates");
        if (maxRounds < 1 || maxStatements < 1) {
            throw new IllegalArgumentException("logic bounds must be positive");
        }
        this.maxRounds = maxRounds;
        this.maxStatements = maxStatements;
    }

    public Result closure(Collection<Statement> seeds) {
        Objects.requireNonNull(seeds, "seeds");
        Set<Statement> all = seedAll(seeds);
        int initial = all.size();
        TransitiveIndex index = new TransitiveIndex();
        for (int round = 1; round <= maxRounds; round++) {
            if (!step(all, index)) {
                return new Result(List.copyOf(all), round - 1, all.size() - initial);
            }
        }
        // A bounded engine must not silently claim a fixed point when it did not reach one.
        if (scan(List.copyOf(all), index, all, null)) {
            throw new IllegalStateException("Logical closure exceeded maxRounds=" + maxRounds);
        }
        return new Result(List.copyOf(all), maxRounds, all.size() - initial);
    }

    private Set<Statement> seedAll(Collection<Statement> seeds) {
        Set<Statement> all = new LinkedHashSet<>();
        for (Statement seed : seeds) {
            Statement statement = Objects.requireNonNull(seed, "seed");
            predicates.flags(statement.predicate());
            addBounded(all, statement);
        }
        return all;
    }

    /** One round: emit every missing consequence of the snapshot in scan order, then merge. */
    private boolean step(Set<Statement> all, TransitiveIndex index) {
        LinkedHashSet<Statement> additions = new LinkedHashSet<>();
        scan(List.copyOf(all), index, all, additions);
        for (Statement addition : additions) {
            addBounded(all, addition);
        }
        return !additions.isEmpty();
    }

    /**
     * Semi-naive scan: reflects only the statements appended since the previous scan (all of
     * them on the first scan) and joins only the pairs with at least one appended member.
     * Older pairs were joined by an earlier round, so their consequences are already present
     * and {@code addCandidate} would ignore them; the emitted sequence equals the full
     * {@code S x S} scan. With {@code additions == null} it is a pure missing-consequence probe.
     */
    private boolean scan(
            List<Statement> snapshot,
            TransitiveIndex index,
            Set<Statement> all,
            Set<Statement> additions) {
        int start = index.indexed;
        index.reserve(snapshot.size() - start);
        for (int position = start; position < snapshot.size(); position++) {
            if (reflect(snapshot.get(position), position, index, all, additions)) {
                return true;
            }
        }
        index.seal(snapshot.size());
        return join(snapshot, index, start, all, additions);
    }

    private boolean reflect(
            Statement statement,
            int position,
            TransitiveIndex index,
            Set<Statement> all,
            Set<Statement> additions) {
        int predicate = statement.predicate();
        int flags = predicates.flags(predicate);
        if ((flags & PredicateFlags.TRANSITIVE) != 0) {
            index.add(position, keyHash(predicate, statement.context(), statement.subject()));
        }
        if ((flags & PredicateFlags.SYMMETRIC) != 0
                && emit(all, additions, statement.reverse(predicate))) {
            return true;
        }
        int inverse = predicates.inverseId(predicate);
        return inverse >= 0 && emit(all, additions, statement.reverse(inverse));
    }

    private boolean join(
            List<Statement> snapshot,
            TransitiveIndex index,
            int start,
            Set<Statement> all,
            Set<Statement> additions) {
        for (int slot = 0; slot < index.leftCount; slot++) {
            int left = index.lefts[slot];
            int from = left < start ? start : 0;
            if (joinLeft(snapshot, snapshot.get(left), from, index, all, additions)) {
                return true;
            }
        }
        return false;
    }

    private boolean joinLeft(
            List<Statement> snapshot,
            Statement left,
            int from,
            TransitiveIndex index,
            Set<Statement> all,
            Set<Statement> additions) {
        int hash = keyHash(left.predicate(), left.context(), left.object());
        if (from > 0 && !index.mayMatchDelta(hash)) {
            return false;
        }
        for (int slot = index.lowerBound(hash, from); index.inRun(slot, hash); slot++) {
            Statement right = snapshot.get(index.positionAt(slot));
            if (left.predicate() == right.predicate()
                    && left.context() == right.context()
                    && left.object() == right.subject()
                    && emit(all, additions, new Statement(
                            left.subject(), left.predicate(), right.object(), left.context()))) {
                return true;
            }
        }
        return false;
    }

    private boolean emit(
            Set<Statement> all,
            Set<Statement> additions,
            Statement candidate) {
        if (additions == null) {
            return !all.contains(candidate);
        }
        addCandidate(all, additions, candidate);
        return false;
    }

    private static int keyHash(int predicate, long context, long node) {
        long hash = (context * 0x9e3779b97f4a7c15L + predicate) * 0xc2b2ae3d27d4eb4fL;
        hash = (Long.rotateLeft(hash, 31) ^ node) * 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        hash *= 0xc4ceb9fe1a85ec53L;
        return (int) (hash ^ (hash >>> 32));
    }

    private void addCandidate(
            Set<Statement> existing,
            Set<Statement> additions,
            Statement candidate) {
        if (!existing.contains(candidate) && additions.add(candidate)) {
            if ((long) existing.size() + additions.size() > maxStatements) {
                throw new IllegalStateException(
                        "Logical closure exceeded maxStatements=" + maxStatements);
            }
        }
    }

    private void addBounded(Set<Statement> statements, Statement statement) {
        if (statements.add(statement) && statements.size() > maxStatements) {
            throw new IllegalStateException(
                    "Logical closure exceeded maxStatements=" + maxStatements);
        }
    }

    /**
     * CSR join index over the transitive statements of the growing closure snapshot.
     *
     * <p>{@code entries} is one sorted lane of {@code keyHash << 32 | position}: every key hash
     * owns a contiguous run (implicit offsets) whose positions ascend, so one lower bound on
     * {@code (hash, from)} lands on the first right at or after snapshot position {@code from}.
     * {@code lefts} lists transitive positions in ascending snapshot order; {@code filter} is a
     * power-of-two bit mask over the key hashes appended by the latest scan.</p>
     */
    private static final class TransitiveIndex {
        private static final long[] NO_LONGS = new long[0];
        private long[] entries = NO_LONGS;
        private long[] pending = NO_LONGS;
        private long[] filter = NO_LONGS;
        private int pendingCount;
        private int[] lefts = new int[0];
        private int leftCount;
        private int indexed;

        private void reserve(int extra) {
            if (pending.length < extra) {
                pending = new long[extra];
            }
            if (lefts.length - leftCount < extra) {
                lefts = Arrays.copyOf(lefts, leftCount + extra);
            }
        }

        private void add(int position, int hash) {
            pending[pendingCount++] = ((long) hash << 32) | position;
            lefts[leftCount++] = position;
        }

        private void seal(int end) {
            filter = new long[Math.max(1, Integer.highestOneBit(pendingCount >>> 2))];
            if (pendingCount > 0) {
                Arrays.sort(pending, 0, pendingCount);
                for (int slot = 0; slot < pendingCount; slot++) {
                    mark((int) (pending[slot] >> 32));
                }
                entries = merge(entries, pending, pendingCount);
                pendingCount = 0;
            }
            indexed = end;
        }

        private void mark(int hash) {
            filter[(hash >>> 6) & (filter.length - 1)] |= 1L << hash;
        }

        private boolean mayMatchDelta(int hash) {
            return (filter[(hash >>> 6) & (filter.length - 1)] & (1L << hash)) != 0;
        }

        private int lowerBound(int hash, int from) {
            long target = ((long) hash << 32) | from;
            int low = 0;
            int high = entries.length;
            while (low < high) {
                int mid = (low + high) >>> 1;
                if (entries[mid] < target) {
                    low = mid + 1;
                } else {
                    high = mid;
                }
            }
            return low;
        }

        private boolean inRun(int slot, int hash) {
            return slot < entries.length && (int) (entries[slot] >> 32) == hash;
        }

        private int positionAt(int slot) {
            return (int) entries[slot];
        }

        private static long[] merge(long[] runA, long[] runB, int countB) {
            long[] merged = new long[runA.length + countB];
            int a = 0;
            int b = 0;
            for (int out = 0; out < merged.length; out++) {
                merged[out] = b == countB || (a < runA.length && runA[a] < runB[b])
                        ? runA[a++] : runB[b++];
            }
            return merged;
        }
    }

    public record Result(
            List<Statement> statements,
            int rounds,
            int derivedStatements) {
        public Result {
            statements = List.copyOf(Objects.requireNonNull(statements, "statements"));
            if (rounds < 0 || derivedStatements < 0) {
                throw new IllegalArgumentException("negative logic result count");
            }
        }
    }
}
