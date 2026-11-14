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
 * AIPerformanceIntent - AnOP annotation for performance tradeoff preference.
 *
 * <h2>AnOP Advisory: Performance Intent</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Not optimization — tradeoff preference.                                  ║
 * ║  Advisory only - no behavior change without legal ops.                    ║
 * ║  Never justifies unsafe buffering.                                        ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;ISpec(value = "Real-time event processor")
 * 	&#64;AIPerformanceIntent(PerformanceIntent.LATENCY_SENSITIVE)
 * 	public interface IEventProcessor&lt;Self extends IEventProcessor&lt;Self>> extends IContext&lt;Self> {
 *
 * 		void process(Event event);
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Advisory Layer
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares a required performance-tradeoff preference without measuring latency, throughput, memory consumption or processor utilization.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code {ElementType.TYPE, ElementType.METHOD}}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIPerformanceIntent.java}
 * blob {@code 2b79f1f96ba04da12cbcb56b3df010a271f9dcda} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIPerformanceIntent {

    /**
     * Performance tradeoff preference.
     *
     * @return performance intent
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares value metadata for Performance tradeoff preference. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code PerformanceIntent}, required explicitly with no default value.
     */
    PerformanceIntent value();

    /** Performance intent types.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 4 symbolic performance-tradeoff preferences for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIPerformanceIntent.PerformanceIntent enum with constants in existing declaration order.
     */
    enum PerformanceIntent {
        /** Minimize latency - fast response time.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the LATENCY_SENSITIVE metadata choice: Minimize latency - fast response time. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        LATENCY_SENSITIVE,

        /** Maximize throughput - high volume processing.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the THROUGHPUT_SENSITIVE metadata choice: Maximize throughput - high volume processing. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        THROUGHPUT_SENSITIVE,

        /** Minimize memory usage.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the MEMORY_CONSTRAINED metadata choice: Minimize memory usage. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        MEMORY_CONSTRAINED,

        /** CPU-intensive computation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CPU_BOUND metadata choice: CPU-intensive computation. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        CPU_BOUND
    }
}
