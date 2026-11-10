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
 * AIErrorPolicy - AnOP annotation for error handling semantic expectation.
 *
 * <h2>AnOP Advisory: Error Semantics</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Not retry logic — semantic expectation.                                  ║
 * ║  Avoids agents inventing retries.                                         ║
 * ║  Avoids silent swallowing.                                                ║
 * ║  Keeps semantics explicit.                                                ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * @ISpec(value = "Payment processor")
 * @AIErrorPolicy(ErrorPolicy.FAIL_FAST) // No silent failures
 * public interface IPaymentProcessor<Self extends IPaymentProcessor<Self>> extends IContext<Self> {
 *
 * 	PaymentResult process(Payment payment);
 * }
 * }</pre>
 *
 * @since IOP 1.0 - AnOP Advisory Layer
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares a required error-policy expectation for types and methods without implementing retries, propagation or exception handling.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code {ElementType.TYPE, ElementType.METHOD}}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIErrorPolicy.java}
 * blob {@code ca90c542425b3ea816e8279c7a7073a7029ee083} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIErrorPolicy {

    /**
     * Error handling semantic expectation.
     *
     * @return error policy
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares value metadata for Error handling semantic expectation. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code ErrorPolicy}, required explicitly with no default value.
     */
    ErrorPolicy value();

    /** Error policy types. */
    /**
     * ErrorPolicy.
     * @author Synexia
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 3 symbolic error-policy expectations for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIErrorPolicy.ErrorPolicy enum with constants in existing declaration order.
     */
    enum ErrorPolicy {
        /** Fail immediately on first error.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the FAIL_FAST metadata choice: Fail immediately on first error. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        FAIL_FAST,

        /** Propagate errors to caller.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PROPAGATE metadata choice: Propagate errors to caller. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        PROPAGATE,

        /** Continue processing, log errors.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the BEST_EFFORT metadata choice: Continue processing, log errors. Naming the token performs no scheduling, retry, interception or transaction operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        BEST_EFFORT
    }
}
