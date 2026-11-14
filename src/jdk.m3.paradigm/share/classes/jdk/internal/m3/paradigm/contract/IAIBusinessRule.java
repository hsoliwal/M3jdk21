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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIBusinessRule - AnOP annotation for business rule hints.
 *
 * <h2>AnOP Advisory: Business Rule Hints</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Business rules describe DOMAIN LOGIC constraints.                        ║
 * ║  Rules are HINTS for the LLM to understand business intent.               ║
 * ║  Rules do not execute - they inform code generation.                      ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * &#64;ISpec(value = "Order processing service")
 * &#64;AIBusinessRule(name = "MAX_ORDER_VALUE", condition = "order.total &lt;= 10000", action = "ALLOW", otherwise = "Requires manager approval")
 * public interface IOrderService&lt;Self extends IOrderService&lt;Self&gt;&gt; extends IContext&lt;Self&gt; {
 *
 * 	OrderResult processOrder(Order order);
 * }
 *
 * &#64;AIBusinessRule(name = "DISCOUNT_ELIGIBILITY", condition = "customer.loyaltyPoints &gt;= 1000", action = "APPLY_DISCOUNT", category = Category.PRICING)
 * BigDecimal calculateDiscount(Customer customer, Order order);
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Business Rule Hints
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares business-rule name, condition, actions, category, priority, enabled state, description and related-rule hints without evaluating or firing rules.
 * @m3.dataStructures SOURCE-retained annotation with exact target {@code {ElementType.TYPE, ElementType.METHOD}}, preserving existing elements, defaults and nested declarations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIBusinessRule.java}
 * blob {@code 84ad9a0d927e30279bb3c6b2409f21452856a550} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IAIBusinessRules.class)
public @interface IAIBusinessRule {

    /**
     * Rule name/identifier.
     *
     * @return rule name
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares name metadata for Rule name/identifier. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, required explicitly with no default value.
     */
    String name();

    /**
     * Rule condition (pseudo-code or natural language).
     *
     * @return condition expression
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares condition metadata for Rule condition (pseudo-code or natural language). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String condition() default "";

    /**
     * Action when condition is true.
     *
     * @return action description
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares action metadata for Action when condition is true. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String action() default "";

    /**
     * Action when condition is false.
     *
     * @return otherwise description
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares otherwise metadata for Action when condition is false. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String otherwise() default "";

    /**
     * Rule category.
     *
     * @return category
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares category metadata for Rule category. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Category}, with exact default {@code Category.GENERAL}.
     */
    Category category() default Category.GENERAL;

    /**
     * Rule priority (higher = more important).
     *
     * @return priority
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares priority metadata for Rule priority (higher = more important). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code int}, with exact default {@code 0}.
     */
    int priority() default 0;

    /**
     * Rule is enabled.
     *
     * @return enabled flag
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares enabled metadata for Rule is enabled. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code true}.
     */
    boolean enabled() default true;

    /**
     * Rule description for documentation.
     *
     * @return description
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares description metadata for Rule description for documentation. Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String description() default "";

    /**
     * Related rules (for rule chaining).
     *
     * @return related rule names
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares relatedRules metadata for Related rules (for rule chaining). Consuming tooling interprets this value; the annotation element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String[]}, with exact default {@code {}}.
     */
    String[] relatedRules() default {};

    /** Business rule categories.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates 18 symbolic business-rule categories for metadata consumers; enum tokens implement no runtime policy.
     * @m3.dataStructures Nested IAIBusinessRule.Category enum with constants in existing declaration order.
     */
    enum Category {
        // --- CORE BUSINESS ---

        /** General/uncategorized rule.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the GENERAL metadata choice: General/uncategorized rule. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        GENERAL,

        /** Validation rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the VALIDATION metadata choice: Validation rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        VALIDATION,

        /** Pricing/discount rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PRICING metadata choice: Pricing/discount rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        PRICING,

        /** Eligibility/qualification rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the ELIGIBILITY metadata choice: Eligibility/qualification rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        ELIGIBILITY,

        /** Approval/workflow rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the APPROVAL metadata choice: Approval/workflow rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        APPROVAL,

        // --- COMPLIANCE ---

        /** Regulatory compliance.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the COMPLIANCE metadata choice: Regulatory compliance. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        COMPLIANCE,

        /** Audit requirements.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the AUDIT metadata choice: Audit requirements. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        AUDIT,

        /** Data retention rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the RETENTION metadata choice: Data retention rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        RETENTION,

        /** Privacy/GDPR rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PRIVACY metadata choice: Privacy/GDPR rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        PRIVACY,

        // --- OPERATIONAL ---

        /** Rate limiting rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the RATE_LIMIT metadata choice: Rate limiting rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        RATE_LIMIT,

        /** Throttling rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the THROTTLE metadata choice: Throttling rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        THROTTLE,

        /** Circuit breaker rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CIRCUIT_BREAKER metadata choice: Circuit breaker rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        CIRCUIT_BREAKER,

        /** Timeout rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the TIMEOUT metadata choice: Timeout rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        TIMEOUT,

        // --- DOMAIN SPECIFIC ---

        /** Financial rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the FINANCIAL metadata choice: Financial rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        FINANCIAL,

        /** Inventory rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the INVENTORY metadata choice: Inventory rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        INVENTORY,

        /** Shipping/logistics rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the SHIPPING metadata choice: Shipping/logistics rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        SHIPPING,

        /** Customer/loyalty rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the CUSTOMER metadata choice: Customer/loyalty rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        CUSTOMER,

        /** Product rules.
         * @m3.pattern synexia:iop
         * @m3.algorithm Names the PRODUCT metadata choice: Product rules. Naming the token performs no rule evaluation, validation, component initialization or generation operation.
         * @m3.dataStructures Existing nested enum constant preserving its declared identity and position.
         */
        PRODUCT
    }
}
