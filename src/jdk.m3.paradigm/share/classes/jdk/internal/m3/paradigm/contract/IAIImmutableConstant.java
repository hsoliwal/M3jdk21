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
 * AIImmutableConstant - IOP annotation marking an immutable constant.
 *
 * <h2>IOP Authority: Immutable Constant Exception</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  By default, constants with collection types (List, Set, Map) are REJECTED║
 * ║  because collections are mutable.                                         ║
 * ║                                                                           ║
 * ║  This annotation EXPLICITLY marks a constant as immutable, allowing:      ║
 * ║    • List.of(...)                                                         ║
 * ║    • Set.of(...)                                                          ║
 * ║    • Map.of(...)                                                          ║
 * ║    • Collections.unmodifiable*(...)                                       ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	public final class AllowedRoles {
 *
 * 		&#64;AIImmutableConstant
 * 		public static final List&lt;String> ADMIN_ROLES = List.of("ADMIN", "SUPER_ADMIN");
 *
 * 		&#64;AIImmutableConstant
 * 		public static final Set&lt;Integer> VALID_CODES = Set.of(200, 201, 204);
 *
 * 		&#64;AIImmutableConstant
 * 		public static final Map&lt;String, String> ERROR_MESSAGES = Map.of("E001", "Invalid input", "E002", "Timeout");
 *
 * 		private AllowedRoles() {
 * 		}
 * 	}
 * }
 * </pre>
 *
 * <h2>Checkstyle Integration</h2>
 * <p>
 * Custom AST rule checks:
 * <ul>
 * <li>Field must be static final</li>
 * <li>Type must be List, Set, or Map</li>
 * <li>Initializer must use immutable factory (List.of, Set.of, Map.of)</li>
 * <li>Without this annotation, collection constants are REJECTED</li>
 * </ul>
 *
 * @since IOP 1.0 - Immutable Constants
 * @see IAIConstant
 * @see IAICheckstyleRule
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares an explicit immutable-collection constant exception, its rationale and expected factory form. The annotation records metadata and does not itself perform enforcement or generation.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.FIELD}, preserving the declared element and nested-enum schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIImmutableConstant.java}
 * blob {@code 66c68ca570c105f305849f464ab4f636c5de52b9} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIImmutableConstant {

    /**
     * Reason for using collection type (documentation).
     *
     * @return reason
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares IAIImmutableConstant#reason metadata for Reason for using collection type (documentation). Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String reason() default "";

    /**
     * Expected factory method used.
     *
     * @return factory type
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares IAIImmutableConstant#factory metadata for Expected factory method used. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Factory}, with exact default {@code Factory.INFERRED}.
     */
    Factory factory() default Factory.INFERRED;

    /** Immutable collection factory methods. */
    /**
     * Factory.
     * @author Synexia
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 10 symbolic choices for expected immutable collection factory form; interpretation belongs to consuming tooling.
     * @m3.dataStructures Nested IAIImmutableConstant.Factory enum with the existing 10 constants in declaration order.
     */
    enum Factory {
        /** Infer from code.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIImmutableConstant.Factory#INFERRED choice for expected immutable collection factory form: Infer from code. The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIImmutableConstant.Factory, preserving its declared position and identity.
         */
        INFERRED,

        /** List.of(...).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIImmutableConstant.Factory#LIST_OF choice for expected immutable collection factory form: List.of(...). The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIImmutableConstant.Factory, preserving its declared position and identity.
         */
        LIST_OF,

        /** Set.of(...).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIImmutableConstant.Factory#SET_OF choice for expected immutable collection factory form: Set.of(...). The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIImmutableConstant.Factory, preserving its declared position and identity.
         */
        SET_OF,

        /** Map.of(...) or Map.ofEntries(...).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIImmutableConstant.Factory#MAP_OF choice for expected immutable collection factory form: Map.of(...) or Map.ofEntries(...). The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIImmutableConstant.Factory, preserving its declared position and identity.
         */
        MAP_OF,

        /** Collections.unmodifiableList(...).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIImmutableConstant.Factory#UNMODIFIABLE_LIST choice for expected immutable collection factory form: Collections.unmodifiableList(...). The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIImmutableConstant.Factory, preserving its declared position and identity.
         */
        UNMODIFIABLE_LIST,

        /** Collections.unmodifiableSet(...).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIImmutableConstant.Factory#UNMODIFIABLE_SET choice for expected immutable collection factory form: Collections.unmodifiableSet(...). The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIImmutableConstant.Factory, preserving its declared position and identity.
         */
        UNMODIFIABLE_SET,

        /** Collections.unmodifiableMap(...).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIImmutableConstant.Factory#UNMODIFIABLE_MAP choice for expected immutable collection factory form: Collections.unmodifiableMap(...). The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIImmutableConstant.Factory, preserving its declared position and identity.
         */
        UNMODIFIABLE_MAP,

        /** Guava ImmutableList.of(...).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIImmutableConstant.Factory#GUAVA_IMMUTABLE_LIST choice for expected immutable collection factory form: Guava ImmutableList.of(...). The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIImmutableConstant.Factory, preserving its declared position and identity.
         */
        GUAVA_IMMUTABLE_LIST,

        /** Guava ImmutableSet.of(...).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIImmutableConstant.Factory#GUAVA_IMMUTABLE_SET choice for expected immutable collection factory form: Guava ImmutableSet.of(...). The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIImmutableConstant.Factory, preserving its declared position and identity.
         */
        GUAVA_IMMUTABLE_SET,

        /** Guava ImmutableMap.of(...).
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the IAIImmutableConstant.Factory#GUAVA_IMMUTABLE_MAP choice for expected immutable collection factory form: Guava ImmutableMap.of(...). The token does not itself validate, generate, freeze or restrict code.
         * @m3.dataStructures Existing enum constant in IAIImmutableConstant.Factory, preserving its declared position and identity.
         */
        GUAVA_IMMUTABLE_MAP
    }
}
