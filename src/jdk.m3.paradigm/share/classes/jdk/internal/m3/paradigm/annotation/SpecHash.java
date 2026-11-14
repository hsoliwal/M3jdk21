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

package jdk.internal.m3.paradigm.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SpecHash - Tracks specification hash for IOP implementation stability.
 *
 * <h2>IOP Principle: Spec Hash → Implementation Tracking</h2>
 *
 * <pre>
 * +-------------------------------------------------------------------------+
 * |  SPEC HASH RULE                                                         |
 * +-------------------------------------------------------------------------+
 * |                                                                         |
 * |  "If the specification (JavaDoc/contract) changes,                      |
 * |   the implementation MUST be regenerated.                               |
 * |   If the spec hash is unchanged, the implementation remains valid."     |
 * |                                                                         |
 * |  Workflow:                                                              |
 * |    1. Developer writes interface + JavaDoc (spec)                       |
 * |    2. Hash of spec is computed: SHA-256(interface + javadoc)            |
 * |    3. AI generates implementation                                       |
 * |    4. @SpecHash(value="abc123...") recorded on implementation           |
 * |    5. On rebuild:                                                       |
 * |       - If spec hash unchanged → skip regeneration                      |
 * |       - If spec hash changed → regenerate implementation                |
 * |                                                                         |
 * |  Benefits:                                                              |
 * |    - Deterministic builds (same spec = same impl)                       |
 * |    - Incremental generation (only changed specs)                        |
 * |    - Audit trail (spec hash proves provenance)                          |
 * |                                                                         |
 * +-------------------------------------------------------------------------+
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     // On the generated implementation class
 *         value = "sha256:a1b2c3d4e5f6...",
 *         source = "com.example.IUserService",
 *         generatedAt = "2025-01-15T10:30:00Z"
 *     )
 *     &#64;Generated("claude-opus-4")
 *     public class UserServiceImpl implements IUserService {
 *         // Implementation generated from spec
 *     }
 *
 *     // In the interface (optional, for documentation)
 *     &#64;SpecHash.Contract("sha256:a1b2c3d4e5f6...")
 *     public interface IUserService {
 *         // Contract definition
 *     }
 * }
 * </pre>
 *
 * <h2>Hash Computation</h2>
 *
 * <pre>
 * Input for hash:
 *   - Interface fully qualified name
 *   - All method signatures (name, params, return type)
 *   - All JavaDoc content (normalized whitespace)
 *   - All annotation values on interface/methods
 *
 * Hash algorithm: SHA-256
 * Format: "sha256:{hex-digest}"
 * </pre>
 *
 * @since ContextOS 1.0 - IOP Spec Tracking
 * @see Model
 * @see AIGenClass
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/SpecHash.java}
 * blob {@code 27213ebd3c05d985a733c9bcfec5897f3479346c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface SpecHash {

    /**
     * The hash of the specification. Format: "sha256:{64-char-hex}"
     *
     * @return spec hash
     */
    String value();

    /**
     * Source interface/class that defines the specification.
     *
     * @return fully qualified source name
     */
    String source() default "";

    /**
     * Timestamp when the implementation was generated. ISO-8601 format.
     *
     * @return generation timestamp
     */
    String generatedAt() default "";

    /**
     * Model that generated the implementation.
     *
     * @return model identifier
     */
    String model() default "";

    /** Contract marker - placed on interface to document its hash. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @interface Contract {

        /**
         * The contract hash.
         *
         * @return hash value
         */
        String value();
    }
}
