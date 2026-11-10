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

import jdk.internal.m3.paradigm.annotation.Stability;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IopBehavioral - Marks a type as having behavior (requiring abstract
 * contract).
 *
 * <h2>🔒 THE IOP BEHAVIORAL LAW 🔒</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║  IF A CLASS HAS BEHAVIOR, IT MUST HAVE A NAMED ABSTRACT INTENT.           ║
 * ║  IF IT HAS NO INTENT, IT MUST NOT HAVE BEHAVIOR.                          ║
 * ║                                                                           ║
 * ║  A behavioral class without an abstract intent is ILLEGAL in IOP.         ║
 * ║  A contract without behavior is documentation, not code.                  ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>WHEN TO USE</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  USE @IopBehavioral ON:                                                   ║
 * ║  ✓ Interfaces with methods that DO something                              ║
 * ║  ✓ Abstract classes with behavior                                         ║
 * ║  ✓ Services, repositories, handlers, processors                           ║
 * ║  ✓ Any type that represents a CAPABILITY                                  ║
 * ║                                                                           ║
 * ║  DO NOT USE ON:                                                           ║
 * ║  ✗ Records (pure data carriers)                                           ║
 * ║  ✗ DTOs (data transfer objects)                                           ║
 * ║  ✗ Value objects (no behavior)                                            ║
 * ║  ✗ Enums (closed set of values)                                           ║
 * ║  ✗ Private inner classes (no semantic surface)                            ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>WHAT THIS ENFORCES</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║  When a type is marked @IopBehavioral:                                    ║
 * ║                                                                           ║
 * ║  1. It MUST be an interface or abstract class                             ║
 * ║  2. It MUST have at least one implementation                              ║
 * ║  3. Implementations MUST use @IopImplements                               ║
 * ║  4. Empty implementations ARE VALID (intent matters, not body)            ║
 * ║  5. LLM agents can ONLY modify implementation, not contract               ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>WHY ABSTRACTION IS ABOUT INTENT, NOT VARIATION</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║  WRONG (classic advice):                                                  ║
 * ║  "Use interfaces when you need polymorphism."                             ║
 * ║                                                                           ║
 * ║  IOP RULE:                                                                ║
 * ║  "Use abstraction to declare intent, always."                             ║
 * ║                                                                           ║
 * ║  That single change:                                                      ║
 * ║  • Removes ambiguity                                                      ║
 * ║  • Stabilizes architecture                                                ║
 * ║  • Eliminates refactor anxiety                                            ║
 * ║  • Makes tooling deterministic                                            ║
 * ║  • Makes LLM agents safe                                                  ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Example</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	// ═══════════════════════════════════════════════════════════════════════
 * 	// INTERFACE (the contract - WHAT)
 * 	// ═══════════════════════════════════════════════════════════════════════
 * 	&#64;IopBehavioral
 * 	&#64;IopBoundary(PUBLIC)
 * 	&#64;IopStability(STABLE)
 * 	public interface IPaymentNormalizer {
 *
 * 		Result normalize(Input input);
 * 	}
 *
 * 	// ═══════════════════════════════════════════════════════════════════════
 * 	// IMPLEMENTATION (the current HOW - even if trivial)
 * 	// ═══════════════════════════════════════════════════════════════════════
 * 	&#64;IopImplements(IPaymentNormalizer.class)
 * 	public final class DefaultPaymentNormalizer implements IPaymentNormalizer {
 *
 * 		&#64;Override
 * 		public Result normalize(Input input) {
 * 			return Result.ok(); // Yes, even this is valid
 * 		}
 * 	}
 *
 * 	// The value is NOT in the body.
 * 	// The value is in the NAME + BOUNDARY + CONTRACT.
 * }
 * </pre>
 *
 * @since IOP 1.0 - Behavioral Type Law
 * @see IopImplements
 * @see IopBoundary
 * @see IOP_TYPE_HIERARCHY
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares the named behavioral intent and stability of an abstract contract. The annotation records metadata and does not itself perform enforcement or generation.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}, preserving the declared element and nested-enum schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IopBehavioral.java}
 * blob {@code ac530f023c7ed565cec216e7f20b8c321fc538ad} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IopBehavioral {

    /**
     * Brief description of the behavioral intent.
     *
     * @return intent description
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares IopBehavioral#value metadata for Brief description of the behavioral intent. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String value() default "";

    /**
     * Stability level of the behavioral contract.
     *
     * @return stability level
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares IopBehavioral#stability metadata for Stability level of the behavioral contract. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Stability.Level}, with exact default {@code Stability.Level.STABLE}.
     */
    Stability.Level stability() default Stability.Level.STABLE;
}
