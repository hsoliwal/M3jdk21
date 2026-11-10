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
package jdk.internal.m3.paradigm.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIReviewed - Marks AI-generated code as human-reviewed.
 *
 * <h2>Human in the Loop</h2>
 *
 * <pre>
 * AI generates code → Human reviews → Code is approved/rejected
 *
 * &#64;AIReviewed captures:
 *   - Who reviewed the code
 *   - When it was reviewed
 *   - Whether it was approved
 *   - Review comments/notes
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * &#64;IAIGenerated(model = "OPUS", specHash = "sha256:abc...", timestamp = "2026-01-15T10:30:00Z")
 * @AIReviewed(reviewer = "john.doe@example.com", date = "2026-01-15", approved = true, comments = {"Code looks good",
 * 		"Added null check manually"})
 * public class UserServiceImpl implements IUserService {
 * 	// Reviewed and approved implementation
 * }
 * }
 * </pre>
 *
 * <h2>Review Workflow</h2>
 *
 * <pre>
 * 1. AI generates code with @IAIGenerated
 * 2. Code enters review queue
 * 3. Human reviews code
 * 4. If approved: Add @AIReviewed(approved = true)
 * 5. If rejected: Regenerate with feedback
 * </pre>
 *
 * @since IOP 1.0 - Human Review Tracking
 * @see IAIGenerated
 * @see IAIShortcoming
 * @m3.pattern synexia:aiop
 * @m3.algorithm Records reviewer identity, date, approval, outcome, comments and manual changes; CLASS retention preserves reported review values without authenticating the reviewer or authorizing publication.
 * @m3.dataStructures CLASS-retained annotation targeting {@code {ElementType.TYPE, ElementType.METHOD}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIReviewed.java}
 * blob {@code fd64177fe374732199477c3a8574802b58a1de60} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface IAIReviewed {

    /**
     * Reviewer identifier (email, username, etc.).
     *
     * @return reviewer ID
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIReviewed#reviewer intent: Reviewer identifier (email, username, etc.). Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String reviewer();

    /**
     * Review date. Format: ISO-8601 date (e.g., "2026-01-15")
     *
     * @return review date
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIReviewed#date intent: Review date. Format: ISO-8601 date (e.g., "2026-01-15") Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String date();

    /**
     * Whether the review approved the code.
     *
     * @return true if approved
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIReviewed#approved intent: Whether the review approved the code. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code true}.
     */
    boolean approved() default true;

    /**
     * Review comments or notes.
     *
     * @return comments
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIReviewed#comments intent: Review comments or notes. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] comments() default {};

    /**
     * Review outcome details.
     *
     * @return outcome
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIReviewed#outcome intent: Review outcome details. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Outcome}; default expression {@code Outcome.APPROVED}.
     */
    Outcome outcome() default Outcome.APPROVED;

    /** Review outcomes. */
    /**
     * Outcome.
     * @author Synexia
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IAIReviewed.Outcome choices described as Outcome. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 5 constants in the existing source order.
     */
    enum Outcome {
        /** Code approved as-is.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIReviewed.Outcome#APPROVED policy choice: Code approved as-is. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIReviewed.Outcome; its declared order and identity are preserved.
         */
        APPROVED,
        /** Code approved with minor modifications.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIReviewed.Outcome#APPROVED_WITH_CHANGES policy choice: Code approved with minor modifications. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIReviewed.Outcome; its declared order and identity are preserved.
         */
        APPROVED_WITH_CHANGES,
        /** Code needs regeneration.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIReviewed.Outcome#NEEDS_REGENERATION policy choice: Code needs regeneration. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIReviewed.Outcome; its declared order and identity are preserved.
         */
        NEEDS_REGENERATION,
        /** Code rejected - manual implementation required.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIReviewed.Outcome#REJECTED policy choice: Code rejected - manual implementation required. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIReviewed.Outcome; its declared order and identity are preserved.
         */
        REJECTED,
        /** Review pending.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IAIReviewed.Outcome#PENDING policy choice: Review pending. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IAIReviewed.Outcome; its declared order and identity are preserved.
         */
        PENDING
    }

    /**
     * Time spent reviewing in minutes.
     *
     * @return review time
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIReviewed#reviewTimeMinutes intent: Time spent reviewing in minutes. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int reviewTimeMinutes() default 0;

    /**
     * Manual changes made by reviewer.
     *
     * @return list of changes
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIReviewed#manualChanges intent: Manual changes made by reviewer. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] manualChanges() default {};
}
