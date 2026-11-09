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
 * Layer - Marks a class as a COP layer.
 *
 * <h2>ContextJ 2.0 - Layer Declaration</h2>
 * <p>
 * A Layer provides behavioral variations that can be dynamically
 * activated/deactivated at runtime. Layers compose via priority ordering.
 *
 * <h2>Layer Model</h2>
 *
 * <pre>
 * {@literal @}Layer(name = "security", priority = 100)
 * public class SecurityLayer extends AbstractLayer&lt;SecurityLayer&gt; {
 *
 *     {@literal @}Partial(target = Service.class, method = "save")
 *     public Result save(Proceed&lt;Result&gt; proceed, Entity e) {
 *         checkPermission(e);
 *         return proceed.call();
 *     }
 * }
 * </pre>
 *
 * <h2>Layer Composition with Strength</h2>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  LAYER STRENGTH MODEL (from Dict DSL)                                   │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                                                                         │
 * │  Strength affects HOW MUCH the layer modifies behavior:                 │
 * │                                                                         │
 * │  MINIMAL (0.25) ─── Layer barely affects execution                      │
 * │  NORMAL  (0.50) ─── Layer applies standard modification                 │
 * │  STRONG  (0.75) ─── Layer significantly alters behavior                 │
 * │  MAXIMUM (1.00) ─── Layer fully controls execution                      │
 * │                                                                         │
 * │  Example: CachingLayer with STRONG skips DB on cache hit                │
 * │           CachingLayer with MINIMAL logs but always hits DB             │
 * │                                                                         │
 * └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Related Layers (Verb Relationship Pattern)</h2>
 * <p>
 * Layers can declare related layers that commonly compose together. This
 * mirrors the Dict DSL verb relationship model where verbs have associated
 * verbs with strengths.
 *
 * <pre>
 * {@literal @}Layer(
 *     name = "auditing",
 *     relatedLayers = {
 *         {@literal @}LayerRelation(layer = SecurityLayer.class, strength = STRONG),
 *         {@literal @}LayerRelation(layer = LoggingLayer.class, strength = NORMAL)
 *     }
 * )
 * public class AuditingLayer extends AbstractLayer&lt;AuditingLayer&gt; { }
 * </pre>
 *
 * <h2>AnOP Compliance</h2>
 * <p>
 * SOURCE retention - compile-time code generation creates layer registration
 * and metadata. No runtime annotation processing.
 *
 * @since ContextOS 1.0 - ContextJ 2.0
 * @see Partial
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Layer.java}
 * blob {@code 08c6d5ca16654e124d7f5cace35730af09df0972} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Layer {

    /**
     * Layer name. Empty string means use class simple name.
     *
     * @return layer name
     */
    String name() default "";

    /**
     * Layer priority for composition ordering. Higher priority = outer wrapper
     * (executes first/last).
     *
     * @return priority
     */
    int priority() default 0;

    /**
     * Default strength when activated without explicit strength.
     *
     * @return default strength
     */
    Strength defaultStrength() default Strength.NORMAL;

    /**
     * Whether this layer is enabled by default.
     *
     * @return true if enabled
     */
    boolean enabled() default true;

    /**
     * Condition expression (JEXL) for automatic activation. Empty string means
     * manual activation only.
     *
     * @return activation condition
     */
    String activateWhen() default "";

    /**
     * Related layers that commonly compose with this layer.
     *
     * @return layer relations
     */
    LayerRelation[] relatedLayers() default {};

    /**
     * Description for documentation.
     *
     * @return description
     */
    String description() default "";

    // --- NESTED TYPES ---

    /** Strength levels for layer activation. Mirrors Dict DSL IStrength pattern. */
    enum Strength {

        /** Minimal intensity - layer barely affects execution. */
        MINIMAL(0.25),
        /** Normal intensity - standard modification. */
        NORMAL(0.50),
        /** Strong intensity - significant alteration. */
        STRONG(0.75),
        /** Maximum intensity - full control. */
        MAXIMUM(1.00);

        private static final double LAYER_7_THRESHOLD = 0.875;
        private static final double LAYER_5_THRESHOLD = 0.625;
        private static final double LAYER_3_THRESHOLD = 0.375;

        private final double level;

        Strength(double level) {
            this.level = level;
        }

        /**
         * Get the strength level value.
         *
         * @return the level value between 0.0 and 1.0
         */
        public double level() {
            return level;
        }

        /**
         * Combine strengths (multiply).
         *
         * @param other
         *            strength to combine
         * @return combined strength
         */
        public Strength combine(Strength other) {
            double combined = this.level * other.level;
            if (combined >= LAYER_7_THRESHOLD) {
                return MAXIMUM;
            }
            if (combined >= LAYER_5_THRESHOLD) {
                return STRONG;
            }
            if (combined >= LAYER_3_THRESHOLD) {
                return NORMAL;
            }
            return MINIMAL;
        }
    }

    /**
     * Layer relationship declaration. Models "verb applies with strength" pattern
     * from Dict DSL.
     */
    @interface LayerRelation {

        /**
         * Related layer class.
         *
         * @return layer class
         */
        Class<?> layer();

        /**
         * Relationship strength.
         *
         * @return strength
         */
        Strength strength() default Strength.NORMAL;

        /**
         * Relationship type (e.g., "requires", "enhances", "conflicts").
         *
         * @return relationship type
         */
        String type() default "enhances";
    }
}
