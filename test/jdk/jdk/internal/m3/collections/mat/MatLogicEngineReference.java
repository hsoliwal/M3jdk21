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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * MatLogicEngine as on master before the closure index leaf, renamed: the reference of
 * MatClosureIndexTest; test only.
 */
final class MatLogicEngineReference {
    private final MatPredicateLexicon predicates;
    private final int maxRounds;
    private final int maxStatements;

    public MatLogicEngineReference(
            MatPredicateLexicon predicates,
            int maxRounds,
            int maxStatements) {
        this.predicates = Objects.requireNonNull(predicates, "predicates");
        if (maxRounds < 1 || maxStatements < 1) {
            throw new IllegalArgumentException("logic bounds must be positive");
        }
        this.maxRounds = maxRounds;
        this.maxStatements = maxStatements;
    }

    public Result closure(Collection<MatStatement> seeds) {
        Objects.requireNonNull(seeds, "seeds");
        Set<MatStatement> all = new LinkedHashSet<>();
        for (MatStatement seed : seeds) {
            MatStatement statement = Objects.requireNonNull(seed, "seed");
            predicates.flags(statement.predicate());
            addBounded(all, statement);
        }
        int initial = all.size();

        for (int round = 1; round <= maxRounds; round++) {
            List<MatStatement> snapshot = List.copyOf(all);
            LinkedHashSet<MatStatement> additions = new LinkedHashSet<>();

            for (MatStatement statement : snapshot) {
                int predicate = statement.predicate();
                int flags = predicates.flags(predicate);
                if ((flags & MatPredicateFlags.SYMMETRIC) != 0) {
                    addCandidate(all, additions, statement.reverse(predicate));
                }
                int inverse = predicates.inverseId(predicate);
                if (inverse >= 0) {
                    addCandidate(all, additions, statement.reverse(inverse));
                }
            }

            for (MatStatement left : snapshot) {
                if (!predicates.hasFlag(left.predicate(), MatPredicateFlags.TRANSITIVE)) {
                    continue;
                }
                for (MatStatement right : snapshot) {
                    if (left.predicate() == right.predicate()
                            && left.context() == right.context()
                            && left.object() == right.subject()) {
                        addCandidate(
                                all,
                                additions,
                                new MatStatement(
                                        left.subject(),
                                        left.predicate(),
                                        right.object(),
                                        left.context()));
                    }
                }
            }

            if (additions.isEmpty()) {
                return new Result(
                        List.copyOf(all),
                        round - 1,
                        all.size() - initial);
            }
            for (MatStatement addition : additions) {
                addBounded(all, addition);
            }
        }

        // A bounded engine must not silently claim a fixed point when it did not reach one.
        List<MatStatement> snapshot = List.copyOf(all);
        if (hasNewConsequence(snapshot, all)) {
            throw new IllegalStateException("Logical closure exceeded maxRounds=" + maxRounds);
        }
        return new Result(List.copyOf(all), maxRounds, all.size() - initial);
    }

    private boolean hasNewConsequence(List<MatStatement> snapshot, Set<MatStatement> all) {
        for (MatStatement statement : snapshot) {
            int predicate = statement.predicate();
            int flags = predicates.flags(predicate);
            if ((flags & MatPredicateFlags.SYMMETRIC) != 0
                    && !all.contains(statement.reverse(predicate))) {
                return true;
            }
            int inverse = predicates.inverseId(predicate);
            if (inverse >= 0 && !all.contains(statement.reverse(inverse))) {
                return true;
            }
        }
        for (MatStatement left : snapshot) {
            if (!predicates.hasFlag(left.predicate(), MatPredicateFlags.TRANSITIVE)) {
                continue;
            }
            for (MatStatement right : snapshot) {
                if (left.predicate() == right.predicate()
                        && left.context() == right.context()
                        && left.object() == right.subject()
                        && !all.contains(new MatStatement(
                                left.subject(),
                                left.predicate(),
                                right.object(),
                                left.context()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addCandidate(
            Set<MatStatement> existing,
            Set<MatStatement> additions,
            MatStatement candidate) {
        if (!existing.contains(candidate) && additions.add(candidate)) {
            if ((long) existing.size() + additions.size() > maxStatements) {
                throw new IllegalStateException(
                        "Logical closure exceeded maxStatements=" + maxStatements);
            }
        }
    }

    private void addBounded(Set<MatStatement> statements, MatStatement statement) {
        if (statements.add(statement) && statements.size() > maxStatements) {
            throw new IllegalStateException(
                    "Logical closure exceeded maxStatements=" + maxStatements);
        }
    }

    public record Result(
            List<MatStatement> statements,
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
