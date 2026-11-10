/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Evidence inventory that deliberately does not force a final semantic label.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/locality/OpenVerdict.java}
 * blob {@code 9149b97531aba7294fc01c315da94109e00aad84} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class OpenVerdict {
    /** Immutable open-verdict receipt. */
    public record Receipt(
            Set<String> supportedFacts,
            Set<String> candidateLabels,
            String relationType,
            boolean closed,
            boolean voteIsTruth) {
        public Receipt {
            supportedFacts = immutable(supportedFacts);
            candidateLabels = immutable(candidateLabels);
            if (!"OPEN".equals(relationType) || closed || voteIsTruth) {
                throw new IllegalArgumentException("OPEN_VERDICT_CONTRACT");
            }
        }

        private static Set<String> immutable(Set<String> values) {
            return Collections.unmodifiableSet(new TreeSet<>(values == null ? Set.of() : values));
        }
    }

    private OpenVerdict() {
    }

    public static Receipt hold(Set<String> supportedFacts, Set<String> candidateLabels) {
        return new Receipt(supportedFacts, candidateLabels, "OPEN", false, false);
    }
}
