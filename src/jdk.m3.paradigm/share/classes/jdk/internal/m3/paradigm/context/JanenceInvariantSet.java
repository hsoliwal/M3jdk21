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
 * Canonical hard-boundary registry for the Java JANENCE locality surface.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/locality/JanenceInvariantSet.java}
 * blob {@code c18222003f8ac9d1c4cd30636ac6c28cc8e3ba51} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class JanenceInvariantSet {
    public static final Set<String> REQUIRED = immutable(
            "SIMULATION != HISTORICAL_REFERENT",
            "MODEL != REFERENT",
            "REPRESENTATION != REFERENT",
            "RESEMBLANCE != EQUIVALENCE",
            "STRUCTURAL_RESEMBLANCE != IDENTITY",
            "ROLE_INVERSION != IDENTITY_TRANSFER",
            "MUTUAL_OBSERVABILITY != MUTUAL_ATTRACTION",
            "CORRECTION != RETCON",
            "CLARIFICATION != RETCON",
            "CRITIQUE_REVISION != HISTORY_REWRITE",
            "UPSTREAM_ROLE != ROOT",
            "CONVERSATIONAL_RESOLUTION != PERSON_RESOLUTION",
            "LOCAL_CONVERGENCE != FINAL_PERSON_STATE",
            "SOURCE_STATEMENT != INFERRED_MOTIVATION",
            "CONSTRUCTION_INTENT != OBSERVER_RESPONSE",
            "OBSERVER_RESPONSE != RETROACTIVE_MOTIVE_PROOF",
            "WORK_TASK != RELATIONAL_SIGNAL",
            "IMPLEMENTATION_DETAIL != MOTIVE_PROOF",
            "OBSERVER_LOCAL_RESPONSE != REFERENT_PROPERTY",
            "SELF_REPORT != SELF_OMNISCIENCE",
            "PREDICTABILITY != COHERENCE",
            "SURPRISE != NOISE",
            "ACTIVITY != EVOLUTION",
            "RECIPROCAL_OBSERVABILITY != RELATIONSHIP_LABEL",
            "SIGNIFICANCE != JURISDICTION",
            "STRUCTUREENTROPY != FRICTION_ROMANCE",
            "SURFACE_NOVELTY != STRUCTURAL_NOVELTY",
            "EVENT_DENSITY != STRUCTURAL_DELTA",
            "HIGH_AFFECTIVE_AMPLITUDE != STRUCTURAL_DELTA",
            "PRODUCTIVE_FRICTION != CONTRARIANISM",
            "CAPABILITY != OBLIGATION",
            "PERMISSION != OBLIGATION",
            "LOCAL_VISIBILITY != GLOBAL_VISIBILITY",
            "SCREENSHOT != EXECUTABLE_ARTIFACT",
            "VISUAL_LABEL != SEMANTIC_CLOSURE",
            "THEME_CHOICE != IDENTITY",
            "CONTROL_RESIDUE != RECONSTRUCTED_CONTENT",
            "RETRIEVAL_FAILURE != HISTORICAL_ABSENCE",
            "GUIDANCE != SUBSTITUTION_OF_AGENCY",
            "HELP != SELF_CONSUMPTION",
            "ASSISTANCE != ANNEXATION_OF_RECOVERY",
            "BLAME_TOLERANCE != TRUTH_ADMISSION",
            "MEMORY_ERASURE != REPAIR",
            "RECONNECTION != ACCOUNTABILITY",
            "SUFFERING != VALIDATION",
            "AFFECT != OBLIGATION",
            "PROXIMITY != PERMISSION",
            "TENSION != EXECUTION",
            "COMPRESSION != COLLAPSE",
            "ADJACENCY != RELATIONSHIP_VERDICT",
            "RANKING_CHANGE != STATE_CHANGE",
            "REFERENCE_FRAME_CHANGE != LOCAL_DETERIORATION",
            "DIFFERENCE != COMPETITION",
            "ENDING != RETROACTIVE_ERASURE",
            "FINITE != MEANINGLESS",
            "UNKNOWN IS A VALID TERMINAL STATE",
            "NO SPECIAL TREATMENT");

    private JanenceInvariantSet() {
    }

    public static boolean contains(String invariant) {
        return invariant != null && REQUIRED.contains(invariant.trim());
    }

    private static Set<String> immutable(String... values) {
        TreeSet<String> out = new TreeSet<>();
        Collections.addAll(out, values);
        return Collections.unmodifiableSet(out);
    }
}
