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

import java.lang.annotation.*;

/**
 * AIHint - AnOP annotation for LLM implementation guidance.
 *
 * <h2>IOP + AnOP Working Together</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  IOP (Authority Layer)     → WHAT is legal, WHAT the contract says        ║
 * ║  AnOP (Advisory Layer)     → HOW to implement, guidance for the LLM       ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                           ║
 * ║  IOP decides legality.                                                    ║
 * ║  AnOP suggests strategy.                                                  ║
 * ║  JDT decides reality.                                                     ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Hint Categories</h2>
 * <ul>
 * <li><b>Algorithm</b> - Prefer specific algorithm approach</li>
 * <li><b>Complexity</b> - Time/space complexity targets</li>
 * <li><b>Style</b> - Code style preference (functional, imperative,
 * fluent)</li>
 * <li><b>Avoid</b> - What NOT to do (anti-patterns)</li>
 * <li><b>Example</b> - Code snippet to follow</li>
 * <li><b>Reference</b> - External reference to learn from</li>
 * </ul>
 *
 * <h2>Rules (Non-Negotiable)</h2>
 * <ul>
 * <li>Hints are ADVISORY - LLM may ignore if better approach exists</li>
 * <li>Hints never override IOP contracts or JavaDoc</li>
 * <li>Hints never add dependencies</li>
 * <li>Hints never change what operation is allowed</li>
 * <li>If hint conflicts with contract → contract wins</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;ISpec(value = "Binary search in sorted array")
 *     &#64;AIHint(algorithm = "binary search", complexity = "O(log n) time, O(1) space", style = Style.IMPERATIVE, avoid = {
 *             "linear scan", "recursion"}, example = """
 *                     int lo = 0, hi = arr.length - 1;
 *                     while (lo &lt;= hi) {
 *                         int mid = lo + (hi - lo) / 2;
 *                         if (arr[mid] == target) return mid;
 *                         if (arr[mid] &lt; target) lo = mid + 1;
 *                         else hi = mid - 1;
 *                     }
 *                     return -1;
 *                     """)
 *     public interface IBinarySearch {
 *
 *         int search(int[] arr, int target);
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Implementation Hints
 * @see AICapabilities
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIHint.java}
 * blob {@code 1d6caa3c265ca3bfc5c8cf09ae8c1ecb567c2a47} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(AIHints.class)
public @interface AIHint {

    // --- ALGORITHM & COMPLEXITY ---

    /**
     * Preferred algorithm approach (natural language description). Example:
     * "binary search", "merge sort", "BFS traversal"
     *
     * @return algorithm hint
     */
    String algorithm() default "";

    /**
     * Target time/space complexity. Example: "O(log n) time, O(1) space"
     *
     * @return complexity requirement
     */
    String complexity() default "";

    /**
     * Data structure preference (natural language). Example: "use HashMap for
     * O(1) lookup", "prefer TreeMap for sorted keys"
     *
     * @return data structure hint
     */
    String dataStructure() default "";

    // --- CODE STYLE ---

    /**
     * Preferred code style.
     *
     * @return style preference
     */
    Style style() default Style.DEFAULT;

    /**
     * Custom style description (if Style.CUSTOM).
     *
     * @return custom style description
     */
    String customStyle() default "";

    // --- GUIDANCE ---

    /**
     * Example code snippet to follow. LLM should use this as a template or
     * reference.
     *
     * @return code snippet
     */
    String example() default "";

    /**
     * External reference (URL or file path) to learn from.
     *
     * @return reference location
     */
    String reference() default "";

    /**
     * Natural language guidance for the LLM. Use this for complex guidance that
     * doesn't fit other attributes.
     *
     * @return guidance text
     */
    String guidance() default "";

    // --- ANTI-PATTERNS ---

    /**
     * What NOT to do - anti-patterns to avoid.
     *
     * @return things to avoid
     */
    String[] avoid() default {};

    /**
     * Security concerns to address.
     *
     * @return security hints
     */
    String[] security() default {};

    // --- PERFORMANCE ---

    /**
     * Performance considerations.
     *
     * @return performance hints
     */
    String performance() default "";

    /**
     * Memory usage considerations.
     *
     * @return memory hints
     */
    String memory() default "";

    /**
     * Concurrency/threading guidance.
     *
     * @return concurrency hints
     */
    String concurrency() default "";

    // --- STYLE ENUM ---

    /** Code style preferences. */
    /**
     * Style.
     * @author Synexia
     */
    enum Style {
        /** Use default/natural style for the context. */
        DEFAULT,

        /** Functional style - streams, lambdas, immutability. */
        FUNCTIONAL,

        /** Imperative style - loops, mutable state. */
        IMPERATIVE,

        /** Fluent/builder style - method chaining. */
        FLUENT,

        /** Declarative style - describe what, not how. */
        DECLARATIVE,

        /** Reactive style - observables, publishers. */
        REACTIVE,

        /** Custom style - see customStyle attribute. */
        CUSTOM
    }
}
