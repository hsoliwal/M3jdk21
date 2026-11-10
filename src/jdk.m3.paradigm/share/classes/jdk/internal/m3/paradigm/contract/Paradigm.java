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

/**
 * The Paradigm Stack - COP -> AOP -> OOP -> FOP -> IOP -> AnOP.
 *
 * <p>
 * Each paradigm builds on the previous. Order matters. All paradigms ultimately
 * serve IOP (Interface-Oriented Programming).
 * @m3.pattern synexia:iop
 * @m3.algorithm Declare six paradigm tokens with fixed names, rule text and explicit stack-order integers; these records do not enforce architecture or annotation retention.
 * @m3.dataStructures Paradigm enum singleton array in declaration order with final label fields.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/Paradigm.java}
 * blob {@code 7369139de662a521ca7a534b96066139866bc2fe} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum Paradigm {

    /**
     * COP - Context-Oriented Programming.
     * <ul>
     * <li>All execution happens inside a context</li>
     * <li>Effects as data (Koffect pattern)</li>
     * <li>Layer activation (ContextJS pattern)</li>
     * <li>Proceed chain (ContextR pattern)</li>
     * <li>Dynamic scoping (ContextL pattern)</li>
     * </ul>
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Paradigm#COP with fullName equal to Context-Oriented Programming; keyRule equal to All execution happens inside a context; order equal to 0; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Paradigm enum singleton with its exact constructor literals.
     */
    COP("Context-Oriented Programming", "All execution happens inside a context", 0),

    /**
     * AOP - Aspect-Oriented Programming.
     * <ul>
     * <li>Compile-time transformation only</li>
     * <li>AOP ends when wrappers are generated</li>
     * <li>No runtime weaving</li>
     * <li>Cross-cutting concerns as compile-time code generation</li>
     * </ul>
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Paradigm#AOP with fullName equal to Aspect-Oriented Programming; keyRule equal to AOP ends when wrappers are generated; order equal to 1; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Paradigm enum singleton with its exact constructor literals.
     */
    AOP("Aspect-Oriented Programming", "AOP ends when wrappers are generated", 1),

    /**
     * OOP - Object-Oriented Programming.
     * <ul>
     * <li>Objects own state, not behavior policy</li>
     * <li>Encapsulation and invariants</li>
     * <li>Polymorphism through interfaces</li>
     * <li>Composition over inheritance</li>
     * </ul>
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Paradigm#OOP with fullName equal to Object-Oriented Programming; keyRule equal to Objects own state, not behavior policy; order equal to 2; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Paradigm enum singleton with its exact constructor literals.
     */
    OOP("Object-Oriented Programming", "Objects own state, not behavior policy", 2),

    /**
     * FOP - Functional-Oriented Programming.
     * <ul>
     * <li>Glue, not architecture</li>
     * <li>If a lambda feels like a program, it's wrong</li>
     * <li>Pure functions for transformations</li>
     * <li>Streams and Optional for composition</li>
     * </ul>
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Paradigm#FOP with fullName equal to Functional-Oriented Programming; keyRule equal to If a lambda feels like a program, it's wrong; order equal to 3; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Paradigm enum singleton with its exact constructor literals.
     */
    FOP("Functional-Oriented Programming", "If a lambda feels like a program, it's wrong", 3),

    /**
     * IOP - Interface-Oriented Programming (Authority Layer).
     * <ul>
     * <li>Interfaces define what the system is</li>
     * <li>JavaDoc becomes the contract</li>
     * <li>Code is instruction, state, implementation, memory</li>
     * <li>AI reads interfaces, not implementations</li>
     * </ul>
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Paradigm#IOP with fullName equal to Interface-Oriented Programming; keyRule equal to Interfaces define what the system is; order equal to 4; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Paradigm enum singleton with its exact constructor literals.
     */
    IOP("Interface-Oriented Programming", "Interfaces define what the system is", 4),

    /**
     * AnOP - Annotation-Oriented Programming.
     * <ul>
     * <li>Annotations are syntax only</li>
     * <li>Must behave like @Buffered (compile-time, no runtime)</li>
     * <li>SOURCE retention only</li>
     * <li>No runtime effects, no hidden threads, no queues</li>
     * </ul>
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify Paradigm#ANOP with fullName equal to Annotation-Oriented Programming; keyRule equal to Annotations must behave like @Buffered; order equal to 5; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One Paradigm enum singleton with its exact constructor literals.
     */
    ANOP("Annotation-Oriented Programming", "Annotations must behave like @Buffered", 5);

    /**
     * Store Paradigm.fullName once from the matching constructor argument without validation or transformation.
     * @m3.pattern synexia:iop
     * @m3.algorithm Store Paradigm.fullName once from the matching constructor argument without validation or transformation.
     * @m3.dataStructures Final String reference for the fullName label.
     */
    private final String fullName;
    /**
     * Store Paradigm.keyRule once from the matching constructor argument without validation or transformation.
     * @m3.pattern synexia:iop
     * @m3.algorithm Store Paradigm.keyRule once from the matching constructor argument without validation or transformation.
     * @m3.dataStructures Final String reference for the keyRule label.
     */
    private final String keyRule;
    /**
     * Store Paradigm.order once from the matching constructor argument without validation or transformation.
     * @m3.pattern synexia:iop
     * @m3.algorithm Store Paradigm.order once from the matching constructor argument without validation or transformation.
     * @m3.dataStructures Final int stack-order field independent of ordinal.
     */
    private final int order;

    /**
     * Initialize Paradigm by directly assigning each constructor argument to its matching final field, preserving label references and any explicit order integer.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initialize Paradigm by directly assigning each constructor argument to its matching final field, preserving label references and any explicit order integer.
     * @m3.dataStructures Constructor arguments and final Paradigm instance fields.
     */
    Paradigm(String fullName, String keyRule, int order) {
        this.fullName = fullName;
        this.keyRule = keyRule;
        this.order = order;
    }

    /** Full name.
     * @m3.pattern synexia:iop
     * @m3.algorithm Return this Paradigm singleton's stored fullName value unchanged.
     * @m3.dataStructures Existing final fullName String reference; no allocation or transformation.
     */
    public String fullName() {
        return fullName;
    }

    /** Key rule.
     * @m3.pattern synexia:iop
     * @m3.algorithm Return this Paradigm singleton's stored keyRule value unchanged.
     * @m3.dataStructures Existing final keyRule String reference; no allocation or transformation.
     */
    public String keyRule() {
        return keyRule;
    }

    /** Order in stack.
     * @m3.pattern synexia:iop
     * @m3.algorithm Return this Paradigm singleton's stored order value unchanged.
     * @m3.dataStructures Existing int stack-order value.
     */
    public int order() {
        return order;
    }
}
