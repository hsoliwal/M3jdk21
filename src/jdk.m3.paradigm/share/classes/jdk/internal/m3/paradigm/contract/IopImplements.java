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
 * IopImplements - Marks a class as implementing a behavioral contract.
 *
 * <h2>THE LINK BETWEEN CONTRACT AND IMPLEMENTATION</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║  @IopBehavioral interface IPaymentNormalizer { ... }                      ║
 * ║                           │                                               ║
 * ║                           ▼                                               ║
 * ║  @IopImplements(IPaymentNormalizer.class)                                 ║
 * ║  public final class DefaultPaymentNormalizer implements IPaymentNormalizer║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>WHAT THIS ENABLES</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║  1. Tooling can find all implementations of a contract                    ║
 * ║  2. LLM agents know which class to modify                                 ║
 * ║  3. JDT can validate implementation completeness                          ║
 * ║  4. Template generator knows the skeleton shape                           ║
 * ║  5. Refactoring tools can update all implementations                      ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * @IopImplements(IUserService.class)
 * public final class UserServiceImpl extends AbstractUserService {
 * 	// Implementation here
 * }
 * }</pre>
 *
 * @since IOP 1.0 - Implementation Marker
 * @see IopBehavioral
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares the link from a concrete type to its behavioral contract, primary status and variant name. The annotation records metadata and does not itself perform enforcement or generation.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code ElementType.TYPE}, preserving the declared element and nested-enum schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IopImplements.java}
 * blob {@code afc577975794e3c62a85306281d662d2dc50054c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IopImplements {

    /**
     * The behavioral contract this class implements.
     *
     * @return the contract interface/abstract class
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares IopImplements#value metadata for The behavioral contract this class implements. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code Class<?>}, requiring an explicit value with no default.
     */
    Class<?> value();

    /**
     * Whether this is the default/primary implementation.
     *
     * @return true if primary implementation
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares IopImplements#primary metadata for Whether this is the default/primary implementation. Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code boolean}, with exact default {@code false}.
     */
    boolean primary() default false;

    /**
     * Implementation variant name (for multiple implementations).
     *
     * @return variant name
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares IopImplements#variant metadata for Implementation variant name (for multiple implementations). Consuming tooling must interpret this value; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code String}, with exact default {@code ""}.
     */
    String variant() default "";
}
