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
 * AOP Advice Types - Cross-cutting concern advice.
 *
 * <p>
 * Defines when and how aspects are applied in Aspect-Oriented Programming.
 * These advice types are used in Spring AOP and AspectJ.
 *
 * @see <a href=
 *      "https://www.geeksforgeeks.org/advance-java/aspect-oriented-programming-aop-in-spring-framework/">AOP
 *      in Spring</a>
 * @m3.pattern synexia:aop
 * @m3.algorithm Defines 21 singleton tokens in declaration order, with generated name, ordinal, values and valueOf behavior. The declared descriptor constructor retains its supplied references and getters return them directly. Naming a pattern or annotation does not implement that named behavior.
 * @m3.dataStructures Enum values in source order: BEFORE, AFTER, AFTER_RETURNING, AFTER_THROWING, AROUND, EXECUTION, WITHIN, THIS, TARGET, ARGS, ANNOTATION, BEAN, COMPILE_TIME_WEAVING, LOAD_TIME_WEAVING, RUNTIME_WEAVING, JDK_PROXY, CGLIB_PROXY, SINGLETON_ASPECT, PER_THIS, PER_TARGET, PER_TYPE_WITHIN.
 * @m3.member {@code FIELD:BEFORE} pattern=synexia:aop; algorithm=Initializes BEFORE using the existing expression <code>new AOPAdvice("Advice", "Executes before the target method")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:AFTER} pattern=synexia:aop; algorithm=Initializes AFTER using the existing expression <code>new AOPAdvice("Advice", "Executes after method completion (finally)")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:AFTER_RETURNING} pattern=synexia:aop; algorithm=Initializes AFTER_RETURNING using the existing expression <code>new AOPAdvice("Advice", "Executes after successful method return")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:AFTER_THROWING} pattern=synexia:aop; algorithm=Initializes AFTER_THROWING using the existing expression <code>new AOPAdvice("Advice", "Executes when method throws exception")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:AROUND} pattern=synexia:aop; algorithm=Initializes AROUND using the existing expression <code>new AOPAdvice("Advice", "Wraps method, controls if/when it executes")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:EXECUTION} pattern=synexia:aop; algorithm=Initializes EXECUTION using the existing expression <code>new AOPAdvice("Pointcut", "Matches method execution join points")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:WITHIN} pattern=synexia:aop; algorithm=Initializes WITHIN using the existing expression <code>new AOPAdvice("Pointcut", "Matches join points within specified types")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:THIS} pattern=synexia:aop; algorithm=Initializes THIS using the existing expression <code>new AOPAdvice("Pointcut", "Matches where AOP proxy is instance of type")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:TARGET} pattern=synexia:aop; algorithm=Initializes TARGET using the existing expression <code>new AOPAdvice("Pointcut", "Matches where target is instance of type")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:ARGS} pattern=synexia:aop; algorithm=Initializes ARGS using the existing expression <code>new AOPAdvice("Pointcut", "Matches based on method argument types")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:ANNOTATION} pattern=synexia:aop; algorithm=Initializes ANNOTATION using the existing expression <code>new AOPAdvice("Pointcut", "Matches methods/classes with annotation")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:BEAN} pattern=synexia:aop; algorithm=Initializes BEAN using the existing expression <code>new AOPAdvice("Pointcut", "Matches by Spring bean name")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:COMPILE_TIME_WEAVING} pattern=synexia:aop; algorithm=Initializes COMPILE_TIME_WEAVING using the existing expression <code>new AOPAdvice("Weaving", "Aspects woven at compile time (AspectJ)")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:LOAD_TIME_WEAVING} pattern=synexia:aop; algorithm=Initializes LOAD_TIME_WEAVING using the existing expression <code>new AOPAdvice("Weaving", "Aspects woven when classes are loaded")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:RUNTIME_WEAVING} pattern=synexia:aop; algorithm=Initializes RUNTIME_WEAVING using the existing expression <code>new AOPAdvice("Weaving", "Aspects woven via proxies at runtime")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:JDK_PROXY} pattern=synexia:aop; algorithm=Initializes JDK_PROXY using the existing expression <code>new AOPAdvice("Proxy", "Interface-based dynamic proxy")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:CGLIB_PROXY} pattern=synexia:aop; algorithm=Initializes CGLIB_PROXY using the existing expression <code>new AOPAdvice("Proxy", "Subclass-based proxy for concrete classes")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SINGLETON_ASPECT} pattern=synexia:aop; algorithm=Initializes SINGLETON_ASPECT using the existing expression <code>new AOPAdvice("Aspect", "Single aspect instance shared across targets")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:PER_THIS} pattern=synexia:aop; algorithm=Initializes PER_THIS using the existing expression <code>new AOPAdvice("Aspect", "One aspect instance per AOP proxy")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:PER_TARGET} pattern=synexia:aop; algorithm=Initializes PER_TARGET using the existing expression <code>new AOPAdvice("Aspect", "One aspect instance per target object")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:PER_TYPE_WITHIN} pattern=synexia:aop; algorithm=Initializes PER_TYPE_WITHIN using the existing expression <code>new AOPAdvice("Aspect", "One aspect instance per matched type")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One AOPAdvice enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:category} pattern=synexia:aop; algorithm.applicability=not-applicable; algorithm.reason=The category declaration is a stored String field or record component with no initializer and no executable algorithm body.; algorithm.review=iop-annotation-contracts-declarations/reviewed-declarations.json#AOPAdvice-3839; data.structures=One stored String field or component. The enum or record constructor retains the supplied reference directly, without validation or defensive copying.; description=Stored category descriptor in AOPAdvice.
 * @m3.member {@code FIELD:description} pattern=synexia:aop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration is a stored String field or record component with no initializer and no executable algorithm body.; algorithm.review=iop-annotation-contracts-declarations/reviewed-declarations.json#AOPAdvice-3874; data.structures=One stored String field or component. The enum or record constructor retains the supplied reference directly, without validation or defensive copying.; description=Stored description descriptor in AOPAdvice.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AOPAdvice.java}
 * blob {@code 0f39feffc02cfc8eb7f004158fcb60a843e82e78} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum AOPAdvice {

    // --- ADVICE TYPES ---
    /** Before - executes before method invocation. */
    BEFORE("Advice", "Executes before the target method"),
    /** After - executes after method, regardless of outcome. */
    AFTER("Advice", "Executes after method completion (finally)"),
    /** After Returning - executes after successful return. */
    AFTER_RETURNING("Advice", "Executes after successful method return"),
    /** After Throwing - executes when method throws exception. */
    AFTER_THROWING("Advice", "Executes when method throws exception"),
    /** Around - surrounds method, controls execution. */
    AROUND("Advice", "Wraps method, controls if/when it executes"),

    // --- POINTCUT DESIGNATORS ---
    /** Execution - matches method execution join points. */
    EXECUTION("Pointcut", "Matches method execution join points"),
    /** Within - matches join points within types. */
    WITHIN("Pointcut", "Matches join points within specified types"),
    /** This - matches where bean reference is instance of type. */
    THIS("Pointcut", "Matches where AOP proxy is instance of type"),
    /** Target - matches where target object is instance of type. */
    TARGET("Pointcut", "Matches where target is instance of type"),
    /** Args - matches where arguments are instances of types. */
    ARGS("Pointcut", "Matches based on method argument types"),
    /** Annotation - matches where subject has annotation. */
    ANNOTATION("Pointcut", "Matches methods/classes with annotation"),
    /** Bean - matches by bean name (Spring-specific). */
    BEAN("Pointcut", "Matches by Spring bean name"),

    // --- WEAVING TYPES ---
    /** Compile-time weaving - AspectJ compiler. */
    COMPILE_TIME_WEAVING("Weaving", "Aspects woven at compile time (AspectJ)"),
    /** Load-time weaving - class loader weaving. */
    LOAD_TIME_WEAVING("Weaving", "Aspects woven when classes are loaded"),
    /** Runtime weaving - proxy-based (Spring AOP). */
    RUNTIME_WEAVING("Weaving", "Aspects woven via proxies at runtime"),

    // --- PROXY TYPES ---
    /** JDK Dynamic Proxy - interface-based proxies. */
    JDK_PROXY("Proxy", "Interface-based dynamic proxy"),
    /** CGLIB Proxy - class-based proxies. */
    CGLIB_PROXY("Proxy", "Subclass-based proxy for concrete classes"),

    // --- ASPECT TYPES ---
    /** Singleton Aspect - one instance shared. */
    SINGLETON_ASPECT("Aspect", "Single aspect instance shared across targets"),
    /** Per-This Aspect - one instance per proxy. */
    PER_THIS("Aspect", "One aspect instance per AOP proxy"),
    /** Per-Target Aspect - one instance per target. */
    PER_TARGET("Aspect", "One aspect instance per target object"),
    /** Per-Type-Within Aspect - one instance per type. */
    PER_TYPE_WITHIN("Aspect", "One aspect instance per matched type");

    private final String category;
    private final String description;

    /**
     * AOPAdvice descriptor reference constructor.
     * @m3.pattern synexia:aop
     * @m3.algorithm Assigns the supplied category, description references directly to category, description fields in source order, without validation or defensive copying.
     * @m3.dataStructures Constructor parameters and corresponding final descriptor fields: category, description.
     */
    AOPAdvice(String category, String description) {
        this.category = category;
        this.description = description;
    }

    /** Advice category.
     * @m3.pattern synexia:aop
     * @m3.algorithm Returns the stored category reference directly, without copying, validating or performing the named pattern or annotation behavior.
     * @m3.dataStructures One final String reference. ElementType arrays remain shared mutable aliases where present.
     */
    public String category() {
        return category;
    }

    /** Advice description.
     * @m3.pattern synexia:aop
     * @m3.algorithm Returns the stored description reference directly, without copying, validating or performing the named pattern or annotation behavior.
     * @m3.dataStructures One final String reference. ElementType arrays remain shared mutable aliases where present.
     */
    public String description() {
        return description;
    }
}
