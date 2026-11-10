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
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.internal.m3.paradigm.contract;

import java.lang.annotation.*;

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
 * &#64;AIGenerated(model = "OPUS", specHash = "sha256:abc...", timestamp = "2026-01-15T10:30:00Z")
 * &#64;AIReviewed(reviewer = "john.doe@example.com", date = "2026-01-15", approved = true, comments = {
 *         "Code looks good", "Added null check manually"})
 * public class UserServiceImpl implements IUserService {
 *     // Reviewed and approved implementation
 * }
 * }
 * </pre>
 *
 * <h2>Review Workflow</h2>
 *
 * <pre>
 * 1. AI generates code with @AIGenerated
 * 2. Code enters review queue
 * 3. Human reviews code
 * 4. If approved: Add @AIReviewed(approved = true)
 * 5. If rejected: Regenerate with feedback
 * </pre>
 *
 * @since IOP 1.0 - Human Review Tracking
 * @see AIShortcoming
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIReviewed.java}
 * blob {@code 8f8164dba6e85178cca88752e0f7ef46f301d162} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface AIReviewed {

    /**
     * Reviewer identifier (email, username, etc.).
     *
     * @return reviewer ID
     */
    String reviewer();

    /**
     * Review date. Format: ISO-8601 date (e.g., "2026-01-15")
     *
     * @return review date
     */
    String date();

    /**
     * Whether the review approved the code.
     *
     * @return true if approved
     */
    boolean approved() default true;

    /**
     * Review comments or notes.
     *
     * @return comments
     */
    String[] comments() default {};

    /**
     * Review outcome details.
     *
     * @return outcome
     */
    Outcome outcome() default Outcome.APPROVED;

    /** Review outcomes. */
    enum Outcome {
        /** Code approved as-is. */
        APPROVED,
        /** Code approved with minor modifications. */
        APPROVED_WITH_CHANGES,
        /** Code needs regeneration. */
        NEEDS_REGENERATION,
        /** Code rejected - manual implementation required. */
        REJECTED,
        /** Review pending. */
        PENDING
    }

    /**
     * Time spent reviewing in minutes.
     *
     * @return review time
     */
    int reviewTimeMinutes() default 0;

    /**
     * Manual changes made by reviewer.
     *
     * @return list of changes
     */
    String[] manualChanges() default {};
}
