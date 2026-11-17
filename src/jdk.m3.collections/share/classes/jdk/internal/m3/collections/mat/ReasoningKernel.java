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
import java.util.Objects;

/**
 * One explicit facade for the three deterministic statement operations:
 * peer comparison, evidence lookup, and bounded logical derivation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/ReasoningKernel.java}
 * blob {@code 6acbd5f0323130544a8b5d98301aadb08eb1c69c} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ReasoningKernel {
    private final PredicateLexicon predicates;
    private final LogicEngine logic;

    public ReasoningKernel(
            PredicateLexicon predicates,
            LogicEngine logic) {
        this.predicates = Objects.requireNonNull(predicates, "predicates");
        this.logic = Objects.requireNonNull(logic, "logic");
    }

    public StatementComparison.Comparison compare(
            Statement statement,
            Statement peer) {
        return StatementComparison.compare(statement, peer, predicates);
    }

    public EvidenceIndex.Assessment evidence(
            Statement statement,
            EvidenceIndex evidence) {
        return Objects.requireNonNull(evidence, "evidence")
                .assess(Objects.requireNonNull(statement, "statement"), predicates);
    }

    public StatementEvaluator.Assessment logic(
            Statement statement,
            EvidenceIndex evidence,
            Collection<Statement> logicSeeds) {
        return new StatementEvaluator(predicates, logic)
                .evaluate(statement, evidence, logicSeeds);
    }

    public Frame analyze(
            Statement statement,
            Statement peer,
            EvidenceIndex evidence,
            Collection<Statement> logicSeeds) {
        StatementComparison.Comparison comparison = compare(statement, peer);
        StatementEvaluator.Assessment assessment =
                logic(statement, evidence, logicSeeds);
        return new Frame(
                comparison,
                assessment.evidence(),
                assessment.derivable(),
                assessment.contradictionDerivable(),
                assessment.closureStatements(),
                assessment.derivedStatements());
    }

    public record Frame(
            StatementComparison.Comparison comparison,
            EvidenceIndex.Assessment evidence,
            boolean derivable,
            boolean contradictionDerivable,
            int closureStatements,
            int derivedStatements) {
        public Frame {
            Objects.requireNonNull(comparison, "comparison");
            Objects.requireNonNull(evidence, "evidence");
            if (closureStatements < 0 || derivedStatements < 0) {
                throw new IllegalArgumentException("negative reasoning count");
            }
        }
    }
}
