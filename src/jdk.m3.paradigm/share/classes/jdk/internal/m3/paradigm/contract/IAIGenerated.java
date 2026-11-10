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
 * IAIGenerated - Marks code as AI-generated (manifestation marker).
 *
 * <h2>IOP: INPUT vs OUTPUT</h2>
 *
 * <pre>
 * &#64;I*Spec = INPUT  (what to generate)
 * &#64;AI*    = OUTPUT (how it manifested)
 *
 * &#64;IAIGenerated captures:
 *   - Which model generated the code
 *   - When it was generated
 *   - What spec hash it was generated from
 *   - How many tokens were used
 *   - How many iterations
 * </pre>
 *
 * <h2>Usage (Auto-Applied)</h2>
 *
 * <pre>{@code
 * // Applied automatically by IOP generator:
 * &#64;IAIGenerated(model = "OPUS", specHash = "sha256:a1b2c3d4...", timestamp = "2026-01-15T10:30:00Z", genCount = 3, tokensUsed = 2847, durationMs = 4521)
 * public class UserServiceImpl implements IUserService {
 * 	// Generated implementation
 * }
 * }</pre>
 *
 * <h2>Purpose</h2>
 * <ul>
 * <li>Track which code is AI-generated vs human-written</li>
 * <li>Enable incremental regeneration (spec hash comparison)</li>
 * <li>Audit trail for compliance/review</li>
 * <li>Cost tracking (tokens used)</li>
 * <li>Performance monitoring (generation time)</li>
 * </ul>
 *
 * @since IOP 1.0 - Manifestation Tracking
 * @see IAIReviewed
 * @see IAIIteration
 * @m3.pattern synexia:aiop
 * @m3.algorithm Records reported generation model, specification hash, timestamp, counters, duration and test result; CLASS retention preserves those supplied values but the annotation authenticates no provenance and computes no hash.
 * @m3.dataStructures CLASS-retained annotation targeting {@code {ElementType.TYPE, ElementType.METHOD}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIGenerated.java}
 * blob {@code 7ab75c47b5601b737918fb1825d4ac9123c1db4d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface IAIGenerated {

    /**
     * Model that generated this code.
     *
     * @return model name (OPUS, SONNET, HAIKU, OLLAMA)
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenerated#model intent: Model that generated this code. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String model();

    /**
     * Spec hash this was generated from. Format: "sha256:{hex}"
     *
     * @return spec hash
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenerated#specHash intent: Spec hash this was generated from. Format: "sha256:{hex}" Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String specHash();

    /**
     * Generation timestamp. Format: ISO-8601 (e.g., "2026-01-15T10:30:00Z")
     *
     * @return timestamp
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenerated#timestamp intent: Generation timestamp. Format: ISO-8601 (e.g., "2026-01-15T10:30:00Z") Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String timestamp();

    /**
     * Generation iteration count. Incremented each time the spec is regenerated.
     *
     * @return generation count
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenerated#genCount intent: Generation iteration count. Incremented each time the spec is regenerated. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 1}.
     */
    int genCount() default 1;

    /**
     * Tokens used for generation.
     *
     * @return token count
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenerated#tokensUsed intent: Tokens used for generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int tokensUsed() default 0;

    /**
     * Generation duration in milliseconds.
     *
     * @return duration in ms
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenerated#durationMs intent: Generation duration in milliseconds. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int durationMs() default 0;

    /**
     * Number of retries before success.
     *
     * @return retry count
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenerated#retries intent: Number of retries before success. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}; default expression {@code 0}.
     */
    int retries() default 0;

    /**
     * Whether tests passed after generation.
     *
     * @return true if tests passed
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenerated#testsPassed intent: Whether tests passed after generation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean testsPassed() default false;

    /**
     * Generator version.
     *
     * @return generator version
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAIGenerated#generatorVersion intent: Generator version. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String generatorVersion() default "";
}
