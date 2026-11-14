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
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 * &#64;AIBusinessRule(name = "MAX_ORDER_VALUE", condition = "order.total <= 10000", action = "ALLOW", otherwise = "Requires manager approval")
 * public interface IOrderService {
 *
 *     OrderResult processOrder(Order order);
 * }
 *
 * @AIBusinessRule(name = "DISCOUNT_ELIGIBILITY", condition = "customer.loyaltyPoints >= 1000", action = "APPLY_DISCOUNT", category = Category.PRICING)
 * BigDecimal calculateDiscount(Customer customer, Order order);
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Business Rule Hints
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIBusinessRule.java}
 * blob {@code 7d3afe1f9d0de36ada7d9d9f88acd09ad28550f4} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(AIBusinessRules.class)
public @interface AIBusinessRule {

    /**
     * Rule name/identifier.
     *
     * @return rule name
     */
    String name();

    /**
     * Rule condition (pseudo-code or natural language).
     *
     * @return condition expression
     */
    String condition() default "";

    /**
     * Action when condition is true.
     *
     * @return action description
     */
    String action() default "";

    /**
     * Action when condition is false.
     *
     * @return otherwise description
     */
    String otherwise() default "";

    /**
     * Rule category.
     *
     * @return category
     */
    Category category() default Category.GENERAL;

    /**
     * Rule priority (higher = more important).
     *
     * @return priority
     */
    int priority() default 0;

    /**
     * Rule is enabled.
     *
     * @return enabled flag
     */
    boolean enabled() default true;

    /**
     * Rule description for documentation.
     *
     * @return description
     */
    String description() default "";

    /**
     * Related rules (for rule chaining).
     *
     * @return related rule names
     */
    String[] relatedRules() default {};

    /** Business rule categories. */
    enum Category {
        // --- CORE BUSINESS ---

        /** General/uncategorized rule. */
        GENERAL,

        /** Validation rules. */
        VALIDATION,

        /** Pricing/discount rules. */
        PRICING,

        /** Eligibility/qualification rules. */
        ELIGIBILITY,

        /** Approval/workflow rules. */
        APPROVAL,

        // --- COMPLIANCE ---

        /** Regulatory compliance. */
        COMPLIANCE,

        /** Audit requirements. */
        AUDIT,

        /** Data retention rules. */
        RETENTION,

        /** Privacy/GDPR rules. */
        PRIVACY,

        // --- OPERATIONAL ---

        /** Rate limiting rules. */
        RATE_LIMIT,

        /** Throttling rules. */
        THROTTLE,

        /** Circuit breaker rules. */
        CIRCUIT_BREAKER,

        /** Timeout rules. */
        TIMEOUT,

        // --- DOMAIN SPECIFIC ---

        /** Financial rules. */
        FINANCIAL,

        /** Inventory rules. */
        INVENTORY,

        /** Shipping/logistics rules. */
        SHIPPING,

        /** Customer/loyalty rules. */
        CUSTOMER,

        /** Product rules. */
        PRODUCT
    }
}
