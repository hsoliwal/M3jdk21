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

import java.util.Collection;
import java.util.Objects;

/**
 * Keeps direct evidence, contradiction and explicit logical derivation as separate signals.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatStatementEvaluator.java}
 * blob {@code 776f080603743aae3ce253bfae0dd38c8be9e0a2} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatStatementEvaluator {
    private final M3MatPredicateLexicon predicates;
    private final M3MatLogicEngine logic;

    public M3MatStatementEvaluator(
            M3MatPredicateLexicon predicates,
            M3MatLogicEngine logic) {
        this.predicates = Objects.requireNonNull(predicates, "predicates");
        this.logic = Objects.requireNonNull(logic, "logic");
    }

    public Assessment evaluate(
            M3MatStatement target,
            M3MatEvidenceIndex evidence,
            Collection<M3MatStatement> logicSeeds) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(evidence, "evidence");
        Objects.requireNonNull(logicSeeds, "logicSeeds");

        M3MatEvidenceIndex.Assessment evidenceAssessment = evidence.assess(target, predicates);
        M3MatLogicEngine.Result closure = logic.closure(logicSeeds);
        boolean derivable = closure.statements().contains(target);
        boolean contradictionDerivable = false;

        int opposite = predicates.oppositeId(target.predicate());
        if (opposite >= 0) {
            M3MatStatement direct = new M3MatStatement(
                    target.subject(), opposite, target.object(), target.context());
            contradictionDerivable = closure.statements().contains(direct);
            if (!contradictionDerivable) {
                int inverse = predicates.inverseId(opposite);
                if (inverse >= 0) {
                    contradictionDerivable =
                            closure.statements().contains(target.reverse(inverse));
                }
            }
        }

        return new Assessment(
                evidenceAssessment,
                derivable,
                contradictionDerivable,
                closure.statements().size(),
                closure.derivedStatements());
    }

    public record Assessment(
            M3MatEvidenceIndex.Assessment evidence,
            boolean derivable,
            boolean contradictionDerivable,
            int closureStatements,
            int derivedStatements) {
        public Assessment {
            Objects.requireNonNull(evidence, "evidence");
            if (closureStatements < 0 || derivedStatements < 0) {
                throw new IllegalArgumentException("negative closure count");
            }
        }
    }
}
