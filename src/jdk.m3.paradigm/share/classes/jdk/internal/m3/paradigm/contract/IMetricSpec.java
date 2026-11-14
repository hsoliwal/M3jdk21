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

import jdk.internal.m3.paradigm.annotation.Model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IMetricSpec - Metrics/observability specification for AI code generation.
 *
 * <h2>Metrics Patterns</h2>
 *
 * <pre>
 * Metrics libraries:
 *   - Micrometer
 *   - Prometheus
 *   - OpenTelemetry
 *   - Dropwizard Metrics
 *   - StatsD
 *
 * &#64;IMetricSpec specifies metrics collection in IOP style.
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * interface IOrderService {
 *
 * 	&#64;IMetricSpec(name = "orders.created", type = COUNTER, tags = {"region", "type"})
 * 	Order createOrder(CreateOrderRequest req);
 *
 * 	&#64;IMetricSpec(name = "order.processing.time", type = TIMER, percentiles = {0.5, 0.95, 0.99})
 * 	void processOrder(Order order);
 *
 * 	&#64;IMetricSpec(name = "orders.pending", type = GAUGE, description = "Number of pending orders")
 * 	int getPendingOrderCount();
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Metric Specification
 * @see ILogSpec
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares a method metric identifier, category, tag strings, distribution options, registry and generation-model token; it registers no instrument and collects no observation.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.METHOD}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IMetricSpec.java}
 * blob {@code fe119e891d054fefa748e24fb9f9f6741750ffb5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IMetricSpec {

    // --- METRIC IDENTITY ---

    /**
     * Metric identifier requested for the annotated method.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMetricSpec#name intent: Metric identifier requested for the annotated method. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; an explicit value is required.
     */
    String name();

    /**
     * Human-readable description requested for the metric.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMetricSpec#description intent: Human-readable description requested for the metric. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code ""}.
     */
    String description() default "";

    /**
     * Metric tag strings; this schema does not specify or validate a key/value pairing convention.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMetricSpec#tags intent: Metric tag strings; this schema does not specify or validate a key/value pairing convention. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}; default expression {@code {}}.
     */
    String[] tags() default {};

    // --- METRIC TYPE ---

    /**
     * Requested metric instrument category.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMetricSpec#type intent: Requested metric instrument category. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Type}; default expression {@code Type.COUNTER}.
     */
    Type type() default Type.COUNTER;

    /**
     * Metric instrument categories used by the type element.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates the IMetricSpec.Type choices described as Metric instrument categories used by the type element. Selection records an annotation value; execution remains the consumer responsibility.
     * @m3.dataStructures Nested enum containing 6 constants in the existing source order.
     */
    enum Type {
        /**
         * Requested accumulated event count category; the token allocates no instrument.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IMetricSpec.Type#COUNTER policy choice: Requested accumulated event count category; the token allocates no instrument. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IMetricSpec.Type; its declared order and identity are preserved.
         */
        COUNTER,
        /**
         * Requested sampled current value category; the token allocates no instrument.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IMetricSpec.Type#GAUGE policy choice: Requested sampled current value category; the token allocates no instrument. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IMetricSpec.Type; its declared order and identity are preserved.
         */
        GAUGE,
        /**
         * Requested operation duration category; the token allocates no instrument.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IMetricSpec.Type#TIMER policy choice: Requested operation duration category; the token allocates no instrument. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IMetricSpec.Type; its declared order and identity are preserved.
         */
        TIMER,
        /**
         * Requested bucketed observations category; the token allocates no instrument.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IMetricSpec.Type#HISTOGRAM policy choice: Requested bucketed observations category; the token allocates no instrument. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IMetricSpec.Type; its declared order and identity are preserved.
         */
        HISTOGRAM,
        /**
         * Requested summary statistics category; the token allocates no instrument.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IMetricSpec.Type#SUMMARY policy choice: Requested summary statistics category; the token allocates no instrument. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IMetricSpec.Type; its declared order and identity are preserved.
         */
        SUMMARY,
        /**
         * Requested distribution summary observations category; the token allocates no instrument.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Names the IMetricSpec.Type#DISTRIBUTION_SUMMARY policy choice: Requested distribution summary observations category; the token allocates no instrument. This enum token does not implement the selected runtime behavior.
         * @m3.dataStructures Existing constant of IMetricSpec.Type; its declared order and identity are preserved.
         */
        DISTRIBUTION_SUMMARY
    }

    // --- TIMER/HISTOGRAM OPTIONS ---

    /**
     * Requested percentile configuration values; the annotation imposes no range check.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMetricSpec#percentiles intent: Requested percentile configuration values; the annotation imposes no range check. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double[]}; default expression {@code {}}.
     */
    double[] percentiles() default {};

    /**
     * Whether histogram publication is requested.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMetricSpec#histogram intent: Whether histogram publication is requested. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}; default expression {@code false}.
     */
    boolean histogram() default false;

    /**
     * Requested service-level threshold values; this schema declares no units or validation.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMetricSpec#sla intent: Requested service-level threshold values; this schema declares no units or validation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code double[]}; default expression {@code {}}.
     */
    double[] sla() default {};

    // --- REGISTRY ---

    /**
     * Metric registry selector, defaulting to the literal name default.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMetricSpec#registry intent: Metric registry selector, defaulting to the literal name default. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}; default expression {@code "default"}.
     */
    String registry() default "default";

    // --- MODEL ---

    /**
     * Preferred code-generation model token for the metric implementation.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IMetricSpec#model intent: Preferred code-generation model token for the metric implementation. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Model.ModelType}; default expression {@code Model.ModelType.HAIKU}.
     */
    Model.ModelType model() default Model.ModelType.HAIKU;
}
