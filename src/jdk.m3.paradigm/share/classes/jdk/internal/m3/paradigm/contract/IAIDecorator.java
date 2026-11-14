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
 * AIDecorator - AnOP annotation for decorator pattern hints.
 *
 * <h2>Jakarta CDI / GoF Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ CDI: @Decorator → decorator class wrapping target ║
 * ║ GoF: Decorator PatternKind → add behavior dynamically ║
 * ║ AnOP: @AIDecorator → compile-time decorator declaration ║
 * ║ ║
 * ║ CDI says: "This decorates the target, enable in beans.xml" ║
 * ║ AnOP says: "This class decorates another" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT apply decorators. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;AIDecorator(decorates = IUserService.class, priority = 100)
 * 	public class LoggingUserServiceDecorator implements IUserService {
 *
 * 		&#64;AIDelegate
 * 		private IUserService delegate;
 *
 * 		&#64;Override
 * 		public User findById(String id) {
 * 			log.info("Finding user: {}", id);
 * 			return delegate.findById(id);
 * 		}
 * 	}
 *
 * 	&#64;AIDecorator(decorates = IPaymentService.class, purpose = Purpose.SECURITY)
 * 	public class AuditingPaymentDecorator implements IPaymentService {
 * 		// Adds audit logging to all payment operations
 * 	}
 * }
 * </pre>
 *
 * @m3.pattern gof:decorator
 * @m3.algorithm Declares type-specific wrapper intent only; no injection, decoration or runtime dispatch occurs here.
 * @m3.dataStructures SOURCE-retained TYPE annotation with Class, int, Purpose enum and boolean elements.
 *
 * @since IOP 1.0 - AnOP Decorator Hints (CDI/GoF-inspired)
 * @see <a href=
 *      "https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html">Jakarta
 *      CDI Decorators</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIDecorator.java}
 * blob {@code 8a5d47e8c2df70e010fe6472462fa3214cf26af4} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIDecorator {

    /**
     * Interface/class being decorated.
     *
     * @m3.pattern gof:decorator
     * @m3.algorithm Declares the decorated component type token, default Object.class; does not resolve or instantiate it.
     * @m3.dataStructures Class&lt;?> annotation element with Object.class default.
     *
     * @return decorated type
     */
    Class<?> decorates() default Object.class;

    /**
     * Decorator priority (higher = applied later, wraps earlier).
     *
     * @m3.pattern gof:decorator
     * @m3.algorithm Declares wrapping order, default zero; the annotation does not order or apply wrappers.
     * @m3.dataStructures Primitive int annotation element with zero default.
     *
     * @return priority
     */
    int priority() default 0;

    /**
     * Decorator purpose.
     *
     * @m3.pattern gof:decorator
     * @m3.algorithm Selects the declared wrapping purpose, default GENERAL; does not add behavior.
     * @m3.dataStructures Purpose enum annotation element with Purpose.GENERAL default.
     *
     * @return purpose
     */
    Purpose purpose() default Purpose.GENERAL;

    /**
     * Enabled by default.
     *
     * @m3.pattern gof:decorator
     * @m3.algorithm Declares default enablement true; does not activate a decorator.
     * @m3.dataStructures Primitive boolean annotation element with true default.
     *
     * @return true if enabled
     */
    boolean enabled() default true;

    /** Decorator purposes.
     * @m3.pattern gof:decorator
     * @m3.algorithm Enumerates eight wrapper-purpose tokens; does not implement any of the named responsibilities.
     * @m3.dataStructures Nested Purpose enum with 8 declared constants.
     */
    enum Purpose {
        /** General-purpose decorator.
         * @m3.pattern gof:decorator
         * @m3.algorithm Declares GENERAL as the general wrapping purpose token; no corresponding behavior executes here.
         * @m3.dataStructures One Purpose enum constant; identity token rather than a callback or stored service.
         */
        GENERAL,

        /** Logging/tracing.
         * @m3.pattern gof:decorator
         * @m3.algorithm Declares LOGGING as the logging or tracing purpose token; no corresponding behavior executes here.
         * @m3.dataStructures One Purpose enum constant; identity token rather than a callback or stored service.
         */
        LOGGING,

        /** Security/authorization.
         * @m3.pattern gof:decorator
         * @m3.algorithm Declares SECURITY as the authorization or security purpose token; no corresponding behavior executes here.
         * @m3.dataStructures One Purpose enum constant; identity token rather than a callback or stored service.
         */
        SECURITY,

        /** Caching.
         * @m3.pattern gof:decorator
         * @m3.algorithm Declares CACHING as the caching purpose token; no corresponding behavior executes here.
         * @m3.dataStructures One Purpose enum constant; identity token rather than a callback or stored service.
         */
        CACHING,

        /** Validation.
         * @m3.pattern gof:decorator
         * @m3.algorithm Declares VALIDATION as the validation purpose token; no corresponding behavior executes here.
         * @m3.dataStructures One Purpose enum constant; identity token rather than a callback or stored service.
         */
        VALIDATION,

        /** Metrics/monitoring.
         * @m3.pattern gof:decorator
         * @m3.algorithm Declares METRICS as the metrics or monitoring purpose token; no corresponding behavior executes here.
         * @m3.dataStructures One Purpose enum constant; identity token rather than a callback or stored service.
         */
        METRICS,

        /** Error handling/retry.
         * @m3.pattern gof:decorator
         * @m3.algorithm Declares RESILIENCE as the error handling or retry purpose token; no corresponding behavior executes here.
         * @m3.dataStructures One Purpose enum constant; identity token rather than a callback or stored service.
         */
        RESILIENCE,

        /** Transaction management.
         * @m3.pattern gof:decorator
         * @m3.algorithm Declares TRANSACTION as the transaction management purpose token; no corresponding behavior executes here.
         * @m3.dataStructures One Purpose enum constant; identity token rather than a callback or stored service.
         */
        TRANSACTION
    }
}
