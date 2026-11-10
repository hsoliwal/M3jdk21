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
 * IAIHint - AnOP annotation for LLM implementation guidance.
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
 * 	&#64;code
 * 	&#64;ISpec(value = "Binary search in sorted array")
 * 	&#64;IAIHint(algorithm = "binary search", complexity = "O(log n) time, O(1) space", style = Style.IMPERATIVE, avoid = {
 * 			"linear scan", "recursion"}, example = """
 * 					int lo = 0, hi = arr.length - 1;
 * 					while (lo &lt;= hi) {
 * 					    int mid = lo + (hi - lo) / 2;
 * 					    if (arr[mid] == target) return mid;
 * 					    if (arr[mid] &lt; target) lo = mid + 1;
 * 					    else hi = mid - 1;
 * 					}
 * 					return -1;
 * 					""")
 * 	public interface IBinarySearch&lt;Self extends IBinarySearch&lt;Self>> extends IContext&lt;Self> {
 *
 * 		int search(int[] arr, int target);
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Implementation Hints
 * @see IAICapabilities
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares implementation algorithm, complexity, coding-style, avoidance, example and reference guidance; this annotation records guidance rather than implementing or verifying the requested behavior.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.TYPE, ElementType.METHOD}; preserves its declared element and nested-token schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIHint.java}
 * blob {@code 44d452161e16f6026965f6b696473eacecceb6b3} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
@Repeatable(IAIHints.class)
public @interface IAIHint {

    // --- ALGORITHM & COMPLEXITY ---

    /**
     * Preferred algorithm approach (natural language description). Example: "binary
     * search", "merge sort", "BFS traversal"
     *
     * @return algorithm hint
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIHint#algorithm advisory metadata for Preferred algorithm approach (natural language description). Example: "binary search", "merge sort", "BFS traversal". The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String algorithm() default "";

    /**
     * Target time/space complexity. Example: "O(log n) time, O(1) space"
     *
     * @return complexity requirement
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIHint#complexity advisory metadata for Target time/space complexity. Example: "O(log n) time, O(1) space". The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String complexity() default "";

    /**
     * Data structure preference (natural language). Example: "use HashMap for O(1)
     * lookup", "prefer TreeMap for sorted keys"
     *
     * @return data structure hint
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIHint#dataStructure advisory metadata for Data structure preference (natural language). Example: "use HashMap for O(1) lookup", "prefer TreeMap for sorted keys". The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String dataStructure() default "";

    // --- CODE STYLE ---

    /**
     * Preferred code style.
     *
     * @return style preference
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIHint#style advisory metadata for Preferred code style. The element itself neither executes nor enforces that request.
     * @m3.dataStructures Style annotation element; exact default expression Style.DEFAULT.
     */
    Style style() default Style.DEFAULT;

    /**
     * Custom style description (if Style.CUSTOM).
     *
     * @return custom style description
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIHint#customStyle advisory metadata for Custom style description (if Style.CUSTOM). The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String customStyle() default "";

    // --- GUIDANCE ---

    /**
     * Example code snippet to follow. LLM should use this as a template or
     * reference.
     *
     * @return code snippet
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIHint#example advisory metadata for Example code snippet to follow. LLM should use this as a template or reference. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String example() default "";

    /**
     * External reference (URL or file path) to learn from.
     *
     * @return reference location
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIHint#reference advisory metadata for External reference (URL or file path) to learn from. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String reference() default "";

    /**
     * Natural language guidance for the LLM. Use this for complex guidance that
     * doesn't fit other attributes.
     *
     * @return guidance text
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIHint#guidance advisory metadata for Natural language guidance for the LLM. Use this for complex guidance that doesn't fit other attributes. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String guidance() default "";

    // --- ANTI-PATTERNS ---

    /**
     * What NOT to do - anti-patterns to avoid.
     *
     * @return things to avoid
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIHint#avoid advisory metadata for What NOT to do - anti-patterns to avoid. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String[] annotation element; exact default expression {}.
     */
    String[] avoid() default {};

    /**
     * Security concerns to address.
     *
     * @return security hints
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIHint#security advisory metadata for Security concerns to address. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String[] annotation element; exact default expression {}.
     */
    String[] security() default {};

    // --- PERFORMANCE ---

    /**
     * Performance considerations.
     *
     * @return performance hints
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIHint#performance advisory metadata for Performance considerations. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String performance() default "";

    /**
     * Memory usage considerations.
     *
     * @return memory hints
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIHint#memory advisory metadata for Memory usage considerations. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String memory() default "";

    /**
     * Concurrency/threading guidance.
     *
     * @return concurrency hints
     * @m3.pattern synexia:aiop
     * @m3.algorithm Carries IAIHint#concurrency advisory metadata for Concurrency/threading guidance. The element itself neither executes nor enforces that request.
     * @m3.dataStructures String annotation element; exact default expression "".
     */
    String concurrency() default "";

    // --- STYLE ENUM ---

    /** Code style preferences.
     * @m3.pattern synexia:aiop
     * @m3.algorithm Enumerates 7 symbolic choices for Code style preferences within IAIHint advisory metadata; no selected strategy executes in this enum.
     * @m3.dataStructures Nested IAIHint.Style enum with 7 source-declared constants in existing order.
     */
    enum Style {
        /** Use default/natural style for the context.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIHint.Style#DEFAULT token within Code style preferences: Use default/natural style for the context. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIHint.Style enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DEFAULT,

        /** Functional style - streams, lambdas, immutability.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIHint.Style#FUNCTIONAL token within Code style preferences: Functional style - streams, lambdas, immutability. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIHint.Style enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FUNCTIONAL,

        /** Imperative style - loops, mutable state.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIHint.Style#IMPERATIVE token within Code style preferences: Imperative style - loops, mutable state. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIHint.Style enum constant, retaining declared order and identity; it stores no implementation object.
         */
        IMPERATIVE,

        /** Fluent/builder style - method chaining.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIHint.Style#FLUENT token within Code style preferences: Fluent/builder style - method chaining. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIHint.Style enum constant, retaining declared order and identity; it stores no implementation object.
         */
        FLUENT,

        /** Declarative style - describe what, not how.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIHint.Style#DECLARATIVE token within Code style preferences: Declarative style - describe what, not how. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIHint.Style enum constant, retaining declared order and identity; it stores no implementation object.
         */
        DECLARATIVE,

        /** Reactive style - observables, publishers.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIHint.Style#REACTIVE token within Code style preferences: Reactive style - observables, publishers. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIHint.Style enum constant, retaining declared order and identity; it stores no implementation object.
         */
        REACTIVE,

        /** Custom style - see customStyle attribute.
         * @m3.pattern synexia:aiop
         * @m3.algorithm Selects the IAIHint.Style#CUSTOM token within Code style preferences: Custom style - see customStyle attribute. This is an advisory choice, not execution of the named behavior.
         * @m3.dataStructures Existing IAIHint.Style enum constant, retaining declared order and identity; it stores no implementation object.
         */
        CUSTOM
    }
}
