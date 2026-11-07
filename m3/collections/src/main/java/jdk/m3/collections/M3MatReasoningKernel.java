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
package jdk.m3.collections;

import java.util.Collection;
import java.util.Objects;

/**
 * One explicit facade for the three deterministic statement operations:
 * peer comparison, evidence lookup, and bounded logical derivation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatReasoningKernel.java}
 * blob {@code 6acbd5f0323130544a8b5d98301aadb08eb1c69c} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatReasoningKernel {
    private final M3MatPredicateLexicon predicates;
    private final M3MatLogicEngine logic;

    public M3MatReasoningKernel(
            M3MatPredicateLexicon predicates,
            M3MatLogicEngine logic) {
        this.predicates = Objects.requireNonNull(predicates, "predicates");
        this.logic = Objects.requireNonNull(logic, "logic");
    }

    public M3MatStatementComparison.Comparison compare(
            M3MatStatement statement,
            M3MatStatement peer) {
        return M3MatStatementComparison.compare(statement, peer, predicates);
    }

    public M3MatEvidenceIndex.Assessment evidence(
            M3MatStatement statement,
            M3MatEvidenceIndex evidence) {
        return Objects.requireNonNull(evidence, "evidence")
                .assess(Objects.requireNonNull(statement, "statement"), predicates);
    }

    public M3MatStatementEvaluator.Assessment logic(
            M3MatStatement statement,
            M3MatEvidenceIndex evidence,
            Collection<M3MatStatement> logicSeeds) {
        return new M3MatStatementEvaluator(predicates, logic)
                .evaluate(statement, evidence, logicSeeds);
    }

    public Frame analyze(
            M3MatStatement statement,
            M3MatStatement peer,
            M3MatEvidenceIndex evidence,
            Collection<M3MatStatement> logicSeeds) {
        M3MatStatementComparison.Comparison comparison = compare(statement, peer);
        M3MatStatementEvaluator.Assessment assessment =
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
            M3MatStatementComparison.Comparison comparison,
            M3MatEvidenceIndex.Assessment evidence,
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
