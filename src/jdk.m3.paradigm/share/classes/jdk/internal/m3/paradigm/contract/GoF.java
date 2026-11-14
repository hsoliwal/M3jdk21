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
 * GoF - Gang of Four Design Patterns (23 patterns).
 *
 * <p>
 * Creational, Structural, and Behavioral patterns from the classic "Design
 * Patterns: Elements of Reusable Object-Oriented Software" book.
 * @m3.pattern synexia:iop
 * @m3.algorithm Defines 23 singleton tokens in declaration order, with generated name, ordinal, values and valueOf behavior. The declared descriptor constructor retains its supplied references and getters return them directly. Naming a pattern or annotation does not implement that named behavior.
 * @m3.dataStructures Enum values in source order: ABSTRACT_FACTORY, BUILDER, FACTORY_METHOD, PROTOTYPE, SINGLETON, ADAPTER, BRIDGE, COMPOSITE, DECORATOR, FACADE, FLYWEIGHT, PROXY, CHAIN_OF_RESPONSIBILITY, COMMAND, INTERPRETER, ITERATOR, MEDIATOR, MEMENTO, OBSERVER, STATE, STRATEGY, TEMPLATE_METHOD, VISITOR.
 * @m3.member {@code FIELD:ABSTRACT_FACTORY} pattern=synexia:iop; algorithm=Initializes ABSTRACT_FACTORY using the existing expression <code>new GoF("Creational", "Provide interface for creating families of related objects")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:BUILDER} pattern=synexia:iop; algorithm=Initializes BUILDER using the existing expression <code>new GoF("Creational", "Separate complex object construction from representation")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:FACTORY_METHOD} pattern=synexia:iop; algorithm=Initializes FACTORY_METHOD using the existing expression <code>new GoF("Creational", "Define interface for creating objects, let subclasses decide")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:PROTOTYPE} pattern=synexia:iop; algorithm=Initializes PROTOTYPE using the existing expression <code>new GoF("Creational", "Create objects by copying existing prototype")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:SINGLETON} pattern=synexia:iop; algorithm=Initializes SINGLETON using the existing expression <code>new GoF("Creational", "Ensure class has only one instance")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:ADAPTER} pattern=synexia:iop; algorithm=Initializes ADAPTER using the existing expression <code>new GoF("Structural", "Convert interface to one client expects")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:BRIDGE} pattern=synexia:iop; algorithm=Initializes BRIDGE using the existing expression <code>new GoF("Structural", "Decouple abstraction from implementation")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:COMPOSITE} pattern=synexia:iop; algorithm=Initializes COMPOSITE using the existing expression <code>new GoF("Structural", "Compose objects into tree structures")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:DECORATOR} pattern=synexia:iop; algorithm=Initializes DECORATOR using the existing expression <code>new GoF("Structural", "Attach additional responsibilities dynamically")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:FACADE} pattern=synexia:iop; algorithm=Initializes FACADE using the existing expression <code>new GoF("Structural", "Provide unified interface to subsystem")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:FLYWEIGHT} pattern=synexia:iop; algorithm=Initializes FLYWEIGHT using the existing expression <code>new GoF("Structural", "Share fine-grained objects efficiently")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:PROXY} pattern=synexia:iop; algorithm=Initializes PROXY using the existing expression <code>new GoF("Structural", "Provide surrogate for another object")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:CHAIN_OF_RESPONSIBILITY} pattern=synexia:iop; algorithm=Initializes CHAIN_OF_RESPONSIBILITY using the existing expression <code>new GoF("Behavioral", "Pass request along chain of handlers")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:COMMAND} pattern=synexia:iop; algorithm=Initializes COMMAND using the existing expression <code>new GoF("Behavioral", "Encapsulate request as object")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:INTERPRETER} pattern=synexia:iop; algorithm=Initializes INTERPRETER using the existing expression <code>new GoF("Behavioral", "Define grammar representation for language")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:ITERATOR} pattern=synexia:iop; algorithm=Initializes ITERATOR using the existing expression <code>new GoF("Behavioral", "Access elements sequentially without exposing representation")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:MEDIATOR} pattern=synexia:iop; algorithm=Initializes MEDIATOR using the existing expression <code>new GoF("Behavioral", "Define object that encapsulates interaction")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:MEMENTO} pattern=synexia:iop; algorithm=Initializes MEMENTO using the existing expression <code>new GoF("Behavioral", "Capture and externalize object\'s internal state")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:OBSERVER} pattern=synexia:iop; algorithm=Initializes OBSERVER using the existing expression <code>new GoF("Behavioral", "Define one-to-many dependency for state changes")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:STATE} pattern=synexia:iop; algorithm=Initializes STATE using the existing expression <code>new GoF("Behavioral", "Allow object to alter behavior when state changes")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:STRATEGY} pattern=synexia:iop; algorithm=Initializes STRATEGY using the existing expression <code>new GoF("Behavioral", "Encapsulate family of algorithms")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:TEMPLATE_METHOD} pattern=synexia:iop; algorithm=Initializes TEMPLATE_METHOD using the existing expression <code>new GoF("Behavioral", "Define algorithm skeleton, let subclasses override steps")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:VISITOR} pattern=synexia:iop; algorithm=Initializes VISITOR using the existing expression <code>new GoF("Behavioral", "Define new operation without changing classes")</code>, preserving descriptor literals, argument order, enum singleton identity and generated ordinal.; data.structures=One GoF enum token or field with its declared initializer. Any constructor-owned ElementType array remains mutable and is returned directly.
 * @m3.member {@code FIELD:category} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The category declaration is a stored String field or record component with no initializer and no executable algorithm body.; algorithm.review=iop-annotation-contracts-declarations/reviewed-declarations.json#GoF-3775; data.structures=One stored String field or component. The enum or record constructor retains the supplied reference directly, without validation or defensive copying.; description=Stored category descriptor in GoF.
 * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration is a stored String field or record component with no initializer and no executable algorithm body.; algorithm.review=iop-annotation-contracts-declarations/reviewed-declarations.json#GoF-3810; data.structures=One stored String field or component. The enum or record constructor retains the supplied reference directly, without validation or defensive copying.; description=Stored description descriptor in GoF.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/GoF.java}
 * blob {@code 332171f12387f8c17eebee33a956d48cf0340652} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum GoF {

    // --- CREATIONAL PATTERNS (5) ---
    /** Create objects without specifying exact class. */
    ABSTRACT_FACTORY("Creational", "Provide interface for creating families of related objects"),
    /** Separate construction from representation. */
    BUILDER("Creational", "Separate complex object construction from representation"),
    /** Create objects by cloning. */
    FACTORY_METHOD("Creational", "Define interface for creating objects, let subclasses decide"),
    /** Clone existing objects. */
    PROTOTYPE("Creational", "Create objects by copying existing prototype"),
    /** Ensure single instance. */
    SINGLETON("Creational", "Ensure class has only one instance"),

    // --- STRUCTURAL PATTERNS (7) ---
    /** Convert interface to another. */
    ADAPTER("Structural", "Convert interface to one client expects"),
    /** Separate abstraction from implementation. */
    BRIDGE("Structural", "Decouple abstraction from implementation"),
    /** Treat individual and composite uniformly. */
    COMPOSITE("Structural", "Compose objects into tree structures"),
    /** Add responsibilities dynamically. */
    DECORATOR("Structural", "Attach additional responsibilities dynamically"),
    /** Simplified interface to complex subsystem. */
    FACADE("Structural", "Provide unified interface to subsystem"),
    /** Share fine-grained objects efficiently. */
    FLYWEIGHT("Structural", "Share fine-grained objects efficiently"),
    /** Placeholder for another object. */
    PROXY("Structural", "Provide surrogate for another object"),

    // --- BEHAVIORAL PATTERNS (11) ---
    /** Pass request along chain. */
    CHAIN_OF_RESPONSIBILITY("Behavioral", "Pass request along chain of handlers"),
    /** Encapsulate request as object. */
    COMMAND("Behavioral", "Encapsulate request as object"),
    /** Define language grammar. */
    INTERPRETER("Behavioral", "Define grammar representation for language"),
    /** Access elements sequentially. */
    ITERATOR("Behavioral", "Access elements sequentially without exposing representation"),
    /** Define object interaction. */
    MEDIATOR("Behavioral", "Define object that encapsulates interaction"),
    /** Capture and restore state. */
    MEMENTO("Behavioral", "Capture and externalize object's internal state"),
    /** Define one-to-many dependency. */
    OBSERVER("Behavioral", "Define one-to-many dependency for state changes"),
    /** Alter behavior when state changes. */
    STATE("Behavioral", "Allow object to alter behavior when state changes"),
    /** Encapsulate algorithm family. */
    STRATEGY("Behavioral", "Encapsulate family of algorithms"),
    /** Define algorithm skeleton. */
    TEMPLATE_METHOD("Behavioral", "Define algorithm skeleton, let subclasses override steps"),
    /** Separate algorithm from objects. */
    VISITOR("Behavioral", "Define new operation without changing classes");

    private final String category;
    private final String description;

    /**
     * GoF descriptor reference constructor.
     * @m3.pattern synexia:iop
     * @m3.algorithm Assigns the supplied category, description references directly to category, description fields in source order, without validation or defensive copying.
     * @m3.dataStructures Constructor parameters and corresponding final descriptor fields: category, description.
     */
    GoF(String category, String description) {
        this.category = category;
        this.description = description;
    }

    /** PatternKind category (Creational, Structural, Behavioral).
     * @m3.pattern synexia:iop
     * @m3.algorithm Returns the stored category reference directly, without copying, validating or performing the named pattern or annotation behavior.
     * @m3.dataStructures One final String reference. ElementType arrays remain shared mutable aliases where present.
     */
    public String category() {
        return category;
    }

    /** PatternKind description.
     * @m3.pattern synexia:iop
     * @m3.algorithm Returns the stored description reference directly, without copying, validating or performing the named pattern or annotation behavior.
     * @m3.dataStructures One final String reference. ElementType arrays remain shared mutable aliases where present.
     */
    public String description() {
        return description;
    }
}
