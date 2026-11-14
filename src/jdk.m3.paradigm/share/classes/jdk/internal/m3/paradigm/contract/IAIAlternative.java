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
 * AIAlternative - AnOP annotation for alternative implementation hints.
 *
 * <h2>Jakarta CDI Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ CDI: @Alternative → alternative implementation (disabled by default) ║
 * ║ AnOP: @AIAlternative → compile-time alternative declaration ║
 * ║ ║
 * ║ CDI says: "This is an alternative, enable it in beans.xml" ║
 * ║ AnOP says: "This is an alternative implementation" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT select alternatives. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	// Default implementation
 * 	&#64;AIStereotype(Role.REPOSITORY)
 * 	public class JpaUserRepository implements IUserRepository {
 * 	}
 *
 * 	// Alternative for testing
 * 	&#64;AIAlternative(priority = 100)
 * 	&#64;AIStereotype(Role.REPOSITORY)
 * 	public class MockUserRepository implements IUserRepository {
 * 	}
 *
 * 	// Alternative for production with specific config
 * 	&#64;AIAlternative(priority = 200, enabledWhen = "profile == 'production'")
 * 	public class CachedUserRepository implements IUserRepository {
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Alternative Hints (CDI-inspired)
 * @see <a href=
 *      "https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html">Jakarta
 *      CDI</a>
 * @m3.pattern spring:conditional
 * @m3.algorithm Declares an alternative implementation with selection priority and activation intent; this annotation evaluates no condition or alternative.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.TYPE, ElementType.METHOD}; its elements and nested declarations are metadata only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIAlternative.java}
 * blob {@code 445c57f616b097ae28fce008f82c7d14edff190f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIAlternative {

    /**
     * Priority for alternative selection. Higher priority wins when multiple
     * alternatives are enabled. Similar to CDI's @Priority.
     *
     * @return priority value
     * @m3.pattern spring:conditional
     * @m3.algorithm Declares alternative selection priority, with higher values intended to win; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures int annotation element; default expression 0.
     */
    int priority() default 0;

    /**
     * Condition when this alternative is enabled. Expression-like (for
     * documentation, not runtime evaluation).
     *
     * @return condition expression
     * @m3.pattern spring:conditional
     * @m3.algorithm Carries the documented activation condition text; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String enabledWhen() default "";

    /**
     * Description of why this alternative exists.
     *
     * @return description
     * @m3.pattern spring:conditional
     * @m3.algorithm Carries the reason for providing the alternative; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; default expression "".
     */
    String description() default "";

    /**
     * Alternative use case.
     *
     * @return use case
     * @m3.pattern spring:conditional
     * @m3.algorithm Selects the alternative implementation use case; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures UseCase annotation element; default expression UseCase.GENERAL.
     */
    UseCase useCase() default UseCase.GENERAL;

    /** Alternative use cases.
     * @m3.pattern spring:conditional
     * @m3.algorithm Enumerates 6 declared alternative implementation use cases; selecting a token performs no corresponding runtime operation.
     * @m3.dataStructures Nested IAIAlternative.UseCase enum with 6 source-declared constants in existing order.
     */
    enum UseCase {
        /** General-purpose alternative.
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIAlternative.UseCase#GENERAL for alternative implementation use cases: General-purpose alternative. The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIAlternative.UseCase enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        GENERAL,

        /** Testing alternative (mocks, stubs).
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIAlternative.UseCase#TESTING for alternative implementation use cases: Testing alternative (mocks, stubs). The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIAlternative.UseCase enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        TESTING,

        /** Development alternative (logging, debugging).
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIAlternative.UseCase#DEVELOPMENT for alternative implementation use cases: Development alternative (logging, debugging). The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIAlternative.UseCase enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        DEVELOPMENT,

        /** Production alternative (optimized, secured).
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIAlternative.UseCase#PRODUCTION for alternative implementation use cases: Production alternative (optimized, secured). The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIAlternative.UseCase enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        PRODUCTION,

        /** Performance alternative (caching, pooling).
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIAlternative.UseCase#PERFORMANCE for alternative implementation use cases: Performance alternative (caching, pooling). The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIAlternative.UseCase enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        PERFORMANCE,

        /** Fallback alternative (when primary fails).
         * @m3.pattern spring:conditional
         * @m3.algorithm Declares IAIAlternative.UseCase#FALLBACK for alternative implementation use cases: Fallback alternative (when primary fails). The token executes no lifecycle or selection operation.
         * @m3.dataStructures One IAIAlternative.UseCase enum constant with its existing declared order; no provider, instance or callback is stored.
         */
        FALLBACK
    }
}
