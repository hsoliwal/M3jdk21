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
 * IIOPPattern - Union of PatternKind Families.
 *
 * <h2>PatternKind Absorption (273 Patterns - LOCKED)</h2>
 *
 * <pre>
 * IIOPPattern
 * ├── GoF           - Gang of Four (23 patterns)
 * ├── EIP           - Enterprise Integration (83 patterns)
 * ├── Microservice  - Microservice patterns (41 patterns)
 * ├── Distributed   - Distributed system patterns (35 patterns)
 * ├── DAG           - DAG flow patterns (40 patterns)
 * ├── Spring        - Spring framework patterns (30 patterns)
 * └── AOPAdvice     - AOP advice types (21 patterns)
 * </pre>
 *
 * @since 1.0 - AI IOP
  * @author Synexia
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares the sealed IIOPPattern metadata variants and the local signatures/defaults category, targetLayer, survival. The defaults project metadata or inspect the current variant and do not execute the described application operation.
 * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IIOPPattern.java}
 * blob {@code 478d2d47933214d4b78ae9f4f7f12390b096f84d} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public sealed interface IIOPPattern extends IIOPConcept
        permits IIOPPattern.GoF,
                IIOPPattern.EIP,
                IIOPPattern.Microservice,
                IIOPPattern.Distributed,
                IIOPPattern.DAG,
                IIOPPattern.Spring,
                IIOPPattern.AOPAdvice {

    /**
     * category metadata operation of IIOPPattern.
     * @m3.pattern synexia:iop
     * @m3.algorithm Returns the existing enum singleton <code>Category.PATTERN</code> without runtime enforcement.
     * @m3.dataStructures Declared Category result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
     */
    @Override
    default Category category() {
        return Category.PATTERN;
    }

    /**
     * Get the target layer for this pattern.
     *
     * @return target layer in paradigm stack
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract targetLayer interface signature declares a String result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-1760
     * @m3.dataStructures Abstract interface signature returning String. Implementations supply the result and its ownership behavior.
     */
    String targetLayer();

    /**
     * Get the pattern survival classification.
     *
     * @return survival type
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract survival interface signature declares a PatternSurvival result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-1887
     * @m3.dataStructures Abstract interface signature returning PatternSurvival. Implementations supply the result and its ownership behavior.
     */
    PatternSurvival survival();

    /** PatternKind survival classifications.
     * @m3.pattern synexia:iop
     * @m3.algorithm Defines the existing 4 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
     * @m3.dataStructures Enum values in source order: ALWAYS_HOLD, TRANSFORM, COLLAPSE, NOT_APPLICABLE.
     * @m3.member {@code FIELD:ALWAYS_HOLD} pattern=synexia:iop; algorithm=Initializes ALWAYS_HOLD from the existing expression <code>new PatternSurvival()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One PatternSurvival field with its declared initializer.
     * @m3.member {@code FIELD:TRANSFORM} pattern=synexia:iop; algorithm=Initializes TRANSFORM from the existing expression <code>new PatternSurvival()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One PatternSurvival field with its declared initializer.
     * @m3.member {@code FIELD:COLLAPSE} pattern=synexia:iop; algorithm=Initializes COLLAPSE from the existing expression <code>new PatternSurvival()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One PatternSurvival field with its declared initializer.
     * @m3.member {@code FIELD:NOT_APPLICABLE} pattern=synexia:iop; algorithm=Initializes NOT_APPLICABLE from the existing expression <code>new PatternSurvival()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One PatternSurvival field with its declared initializer.
     */
    enum PatternSurvival {
        /** Structurally invariant - always holds. */
        ALWAYS_HOLD,
        /** Holds but changes shape. */
        TRANSFORM,
        /** Disappears into better abstraction. */
        COLLAPSE,
        /** Anti-pattern, not applicable. */
        NOT_APPLICABLE
    }

    // --- GoF - Gang of Four Patterns (23) ---

    /**
     * GoF metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied GoF metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: GoFType type. References are retained directly and primitive component values are stored unchanged.
     * @m3.member {@code FIELD:type} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The type declaration supplies a stored GoFType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-2330; data.structures=One stored GoFType component or field. Reference values are retained and exposed without copying, including null.; description=Stored type metadata in GoF.
     */
    record GoF(GoFType type) implements IIOPPattern {
        /**
         * name metadata operation of GoF.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.name()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return type.name();
        }

        /**
         * description metadata operation of GoF.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.description()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return type.description();
        }

        /**
         * targetLayer metadata operation of GoF.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"Compile-time"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String targetLayer() {
            return "Compile-time";
        }

        /**
         * survival metadata operation of GoF.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.survival()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared PatternSurvival result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public PatternSurvival survival() {
            return type.survival();
        }

        /**
         * GoFType metadata enumeration.
         * @m3.pattern synexia:iop
         * @m3.algorithm Defines the existing 23 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
         * @m3.dataStructures Enum values in source order: FACTORY_METHOD, ABSTRACT_FACTORY, BUILDER, PROTOTYPE, SINGLETON, ADAPTER, BRIDGE, COMPOSITE, DECORATOR, FACADE, FLYWEIGHT, PROXY, CHAIN_OF_RESPONSIBILITY, COMMAND, INTERPRETER, ITERATOR, MEDIATOR, MEMENTO, OBSERVER, STATE, STRATEGY, TEMPLATE_METHOD, VISITOR.
         * @m3.member {@code FIELD:FACTORY_METHOD} pattern=synexia:iop; algorithm=Initializes FACTORY_METHOD from the existing expression <code>new GoFType("Create without specifying class", PatternSurvival.ALWAYS_HOLD)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored FACTORY_METHOD metadata in GoFType.
         * @m3.member {@code FIELD:ABSTRACT_FACTORY} pattern=synexia:iop; algorithm=Initializes ABSTRACT_FACTORY from the existing expression <code>new GoFType("Family of related objects", PatternSurvival.ALWAYS_HOLD)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored ABSTRACT_FACTORY metadata in GoFType.
         * @m3.member {@code FIELD:BUILDER} pattern=synexia:iop; algorithm=Initializes BUILDER from the existing expression <code>new GoFType("Complex construction", PatternSurvival.TRANSFORM)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored BUILDER metadata in GoFType.
         * @m3.member {@code FIELD:PROTOTYPE} pattern=synexia:iop; algorithm=Initializes PROTOTYPE from the existing expression <code>new GoFType("Clone existing", PatternSurvival.ALWAYS_HOLD)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored PROTOTYPE metadata in GoFType.
         * @m3.member {@code FIELD:SINGLETON} pattern=synexia:iop; algorithm=Initializes SINGLETON from the existing expression <code>new GoFType("Single instance", PatternSurvival.TRANSFORM)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored SINGLETON metadata in GoFType.
         * @m3.member {@code FIELD:ADAPTER} pattern=synexia:iop; algorithm=Initializes ADAPTER from the existing expression <code>new GoFType("Convert interface", PatternSurvival.ALWAYS_HOLD)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored ADAPTER metadata in GoFType.
         * @m3.member {@code FIELD:BRIDGE} pattern=synexia:iop; algorithm=Initializes BRIDGE from the existing expression <code>new GoFType("Separate abstraction from impl", PatternSurvival.ALWAYS_HOLD)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored BRIDGE metadata in GoFType.
         * @m3.member {@code FIELD:COMPOSITE} pattern=synexia:iop; algorithm=Initializes COMPOSITE from the existing expression <code>new GoFType("Tree structure", PatternSurvival.ALWAYS_HOLD)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored COMPOSITE metadata in GoFType.
         * @m3.member {@code FIELD:DECORATOR} pattern=synexia:iop; algorithm=Initializes DECORATOR from the existing expression <code>new GoFType("Add responsibilities", PatternSurvival.COLLAPSE)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored DECORATOR metadata in GoFType.
         * @m3.member {@code FIELD:FACADE} pattern=synexia:iop; algorithm=Initializes FACADE from the existing expression <code>new GoFType("Simplified interface", PatternSurvival.ALWAYS_HOLD)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored FACADE metadata in GoFType.
         * @m3.member {@code FIELD:FLYWEIGHT} pattern=synexia:iop; algorithm=Initializes FLYWEIGHT from the existing expression <code>new GoFType("Share fine-grained objects", PatternSurvival.ALWAYS_HOLD)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored FLYWEIGHT metadata in GoFType.
         * @m3.member {@code FIELD:PROXY} pattern=synexia:iop; algorithm=Initializes PROXY from the existing expression <code>new GoFType("Surrogate/placeholder", PatternSurvival.ALWAYS_HOLD)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored PROXY metadata in GoFType.
         * @m3.member {@code FIELD:CHAIN_OF_RESPONSIBILITY} pattern=synexia:iop; algorithm=Initializes CHAIN_OF_RESPONSIBILITY from the existing expression <code>new GoFType("Pass request along chain", PatternSurvival.ALWAYS_HOLD)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored CHAIN_OF_RESPONSIBILITY metadata in GoFType.
         * @m3.member {@code FIELD:COMMAND} pattern=synexia:iop; algorithm=Initializes COMMAND from the existing expression <code>new GoFType("Encapsulate request", PatternSurvival.ALWAYS_HOLD)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored COMMAND metadata in GoFType.
         * @m3.member {@code FIELD:INTERPRETER} pattern=synexia:iop; algorithm=Initializes INTERPRETER from the existing expression <code>new GoFType("Grammar interpreter", PatternSurvival.COLLAPSE)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored INTERPRETER metadata in GoFType.
         * @m3.member {@code FIELD:ITERATOR} pattern=synexia:iop; algorithm=Initializes ITERATOR from the existing expression <code>new GoFType("Sequential access", PatternSurvival.COLLAPSE)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored ITERATOR metadata in GoFType.
         * @m3.member {@code FIELD:MEDIATOR} pattern=synexia:iop; algorithm=Initializes MEDIATOR from the existing expression <code>new GoFType("Centralize communication", PatternSurvival.TRANSFORM)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored MEDIATOR metadata in GoFType.
         * @m3.member {@code FIELD:MEMENTO} pattern=synexia:iop; algorithm=Initializes MEMENTO from the existing expression <code>new GoFType("Capture/restore state", PatternSurvival.COLLAPSE)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored MEMENTO metadata in GoFType.
         * @m3.member {@code FIELD:OBSERVER} pattern=synexia:iop; algorithm=Initializes OBSERVER from the existing expression <code>new GoFType("One-to-many dependency", PatternSurvival.TRANSFORM)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored OBSERVER metadata in GoFType.
         * @m3.member {@code FIELD:STATE} pattern=synexia:iop; algorithm=Initializes STATE from the existing expression <code>new GoFType("Alter behavior with state", PatternSurvival.ALWAYS_HOLD)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored STATE metadata in GoFType.
         * @m3.member {@code FIELD:STRATEGY} pattern=synexia:iop; algorithm=Initializes STRATEGY from the existing expression <code>new GoFType("Family of algorithms", PatternSurvival.ALWAYS_HOLD)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored STRATEGY metadata in GoFType.
         * @m3.member {@code FIELD:TEMPLATE_METHOD} pattern=synexia:iop; algorithm=Initializes TEMPLATE_METHOD from the existing expression <code>new GoFType("Define skeleton", PatternSurvival.ALWAYS_HOLD)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored TEMPLATE_METHOD metadata in GoFType.
         * @m3.member {@code FIELD:VISITOR} pattern=synexia:iop; algorithm=Initializes VISITOR from the existing expression <code>new GoFType("Operation on elements", PatternSurvival.ALWAYS_HOLD)</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One GoFType field with its declared initializer.; description=Stored VISITOR metadata in GoFType.
         * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-4656; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in GoFType.
         * @m3.member {@code FIELD:survival} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The survival declaration supplies a stored PatternSurvival metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-4702; data.structures=One stored PatternSurvival component or field. Reference values are retained and exposed without copying, including null.; description=Stored survival metadata in GoFType.
         */
        public enum GoFType {

            // Creational
            FACTORY_METHOD("Create without specifying class", PatternSurvival.ALWAYS_HOLD),
            ABSTRACT_FACTORY("Family of related objects", PatternSurvival.ALWAYS_HOLD),
            BUILDER("Complex construction", PatternSurvival.TRANSFORM),
            PROTOTYPE("Clone existing", PatternSurvival.ALWAYS_HOLD),
            SINGLETON("Single instance", PatternSurvival.TRANSFORM),

            // Structural
            ADAPTER("Convert interface", PatternSurvival.ALWAYS_HOLD),
            BRIDGE("Separate abstraction from impl", PatternSurvival.ALWAYS_HOLD),
            COMPOSITE("Tree structure", PatternSurvival.ALWAYS_HOLD),
            DECORATOR("Add responsibilities", PatternSurvival.COLLAPSE),
            FACADE("Simplified interface", PatternSurvival.ALWAYS_HOLD),
            FLYWEIGHT("Share fine-grained objects", PatternSurvival.ALWAYS_HOLD),
            PROXY("Surrogate/placeholder", PatternSurvival.ALWAYS_HOLD),

            // Behavioral
            CHAIN_OF_RESPONSIBILITY("Pass request along chain", PatternSurvival.ALWAYS_HOLD),
            COMMAND("Encapsulate request", PatternSurvival.ALWAYS_HOLD),
            INTERPRETER("Grammar interpreter", PatternSurvival.COLLAPSE),
            ITERATOR("Sequential access", PatternSurvival.COLLAPSE),
            MEDIATOR("Centralize communication", PatternSurvival.TRANSFORM),
            MEMENTO("Capture/restore state", PatternSurvival.COLLAPSE),
            OBSERVER("One-to-many dependency", PatternSurvival.TRANSFORM),
            STATE("Alter behavior with state", PatternSurvival.ALWAYS_HOLD),
            STRATEGY("Family of algorithms", PatternSurvival.ALWAYS_HOLD),
            TEMPLATE_METHOD("Define skeleton", PatternSurvival.ALWAYS_HOLD),
            VISITOR("Operation on elements", PatternSurvival.ALWAYS_HOLD);

            private final String description;
            private final PatternSurvival survival;

            /**
             * GoFType descriptor-field constructor.
             * @m3.pattern synexia:iop
             * @m3.algorithm Assigns the supplied description, survival references to description, survival fields in source order without validation or defensive copying.
             * @m3.dataStructures Constructor parameters and corresponding final enum descriptor fields: description, survival.
             */
            GoFType(String description, PatternSurvival survival) {
                this.description = description;
                this.survival = survival;
            }

            /**
             * description metadata operation of GoFType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the stored description field or record component directly without copying, validation or policy execution.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            public String description() {
                return description;
            }

            /**
             * survival metadata operation of GoFType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the stored survival field or record component directly without copying, validation or policy execution.
             * @m3.dataStructures Declared PatternSurvival result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            public PatternSurvival survival() {
                return survival;
            }
        }

        /**
         * of metadata operation of GoF.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates and returns the value from <code>new GoF(type)</code>, evaluating constructor arguments in source order. Supplied references are retained directly and construction adds no validation.
         * @m3.dataStructures Declared GoF result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static GoF of(GoFType type) {
            return new GoF(type);
        }
    }

    // --- EIP - Enterprise Integration Patterns ---

    /**
     * EIP metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied EIP metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: EIPType type. References are retained directly and primitive component values are stored unchanged.
     * @m3.member {@code FIELD:type} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The type declaration supplies a stored EIPType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-5280; data.structures=One stored EIPType component or field. Reference values are retained and exposed without copying, including null.; description=Stored type metadata in EIP.
     */
    record EIP(EIPType type) implements IIOPPattern {
        /**
         * name metadata operation of EIP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.name()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return type.name();
        }

        /**
         * description metadata operation of EIP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.description()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return type.description();
        }

        /**
         * targetLayer metadata operation of EIP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"IOP"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String targetLayer() {
            return "IOP";
        }

        /**
         * survival metadata operation of EIP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>PatternSurvival.ALWAYS_HOLD</code> without runtime enforcement.
         * @m3.dataStructures Declared PatternSurvival result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public PatternSurvival survival() {
            return PatternSurvival.ALWAYS_HOLD;
        }

        /**
         * EIPType metadata enumeration.
         * @m3.pattern synexia:iop
         * @m3.algorithm Defines the existing 25 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
         * @m3.dataStructures Enum values in source order: MESSAGE_CHANNEL, PUBLISH_SUBSCRIBE, MESSAGE_ROUTER, MESSAGE_TRANSLATOR, MESSAGE_FILTER, CONTENT_BASED_ROUTER, SPLITTER, AGGREGATOR, SCATTER_GATHER, ROUTING_SLIP, PROCESS_MANAGER, CONTENT_ENRICHER, CONTENT_FILTER, NORMALIZER, CANONICAL_DATA_MODEL, MESSAGE_ENDPOINT, POLLING_CONSUMER, EVENT_DRIVEN_CONSUMER, COMPETING_CONSUMERS, MESSAGE_DISPATCHER, WIRE_TAP, MESSAGE_HISTORY, MESSAGE_STORE, DEAD_LETTER_CHANNEL, INVALID_MESSAGE_CHANNEL.
         * @m3.member {@code FIELD:MESSAGE_CHANNEL} pattern=synexia:iop; algorithm=Initializes MESSAGE_CHANNEL from the existing expression <code>new EIPType("Point-to-point channel")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored MESSAGE_CHANNEL metadata in EIPType.
         * @m3.member {@code FIELD:PUBLISH_SUBSCRIBE} pattern=synexia:iop; algorithm=Initializes PUBLISH_SUBSCRIBE from the existing expression <code>new EIPType("Pub/sub channel")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored PUBLISH_SUBSCRIBE metadata in EIPType.
         * @m3.member {@code FIELD:MESSAGE_ROUTER} pattern=synexia:iop; algorithm=Initializes MESSAGE_ROUTER from the existing expression <code>new EIPType("Route based on content")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored MESSAGE_ROUTER metadata in EIPType.
         * @m3.member {@code FIELD:MESSAGE_TRANSLATOR} pattern=synexia:iop; algorithm=Initializes MESSAGE_TRANSLATOR from the existing expression <code>new EIPType("Transform message")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored MESSAGE_TRANSLATOR metadata in EIPType.
         * @m3.member {@code FIELD:MESSAGE_FILTER} pattern=synexia:iop; algorithm=Initializes MESSAGE_FILTER from the existing expression <code>new EIPType("Filter messages")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored MESSAGE_FILTER metadata in EIPType.
         * @m3.member {@code FIELD:CONTENT_BASED_ROUTER} pattern=synexia:iop; algorithm=Initializes CONTENT_BASED_ROUTER from the existing expression <code>new EIPType("Route by content")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored CONTENT_BASED_ROUTER metadata in EIPType.
         * @m3.member {@code FIELD:SPLITTER} pattern=synexia:iop; algorithm=Initializes SPLITTER from the existing expression <code>new EIPType("Split message")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored SPLITTER metadata in EIPType.
         * @m3.member {@code FIELD:AGGREGATOR} pattern=synexia:iop; algorithm=Initializes AGGREGATOR from the existing expression <code>new EIPType("Combine messages")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored AGGREGATOR metadata in EIPType.
         * @m3.member {@code FIELD:SCATTER_GATHER} pattern=synexia:iop; algorithm=Initializes SCATTER_GATHER from the existing expression <code>new EIPType("Broadcast and collect")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored SCATTER_GATHER metadata in EIPType.
         * @m3.member {@code FIELD:ROUTING_SLIP} pattern=synexia:iop; algorithm=Initializes ROUTING_SLIP from the existing expression <code>new EIPType("Dynamic routing")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored ROUTING_SLIP metadata in EIPType.
         * @m3.member {@code FIELD:PROCESS_MANAGER} pattern=synexia:iop; algorithm=Initializes PROCESS_MANAGER from the existing expression <code>new EIPType("Orchestrate process")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored PROCESS_MANAGER metadata in EIPType.
         * @m3.member {@code FIELD:CONTENT_ENRICHER} pattern=synexia:iop; algorithm=Initializes CONTENT_ENRICHER from the existing expression <code>new EIPType("Add data to message")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored CONTENT_ENRICHER metadata in EIPType.
         * @m3.member {@code FIELD:CONTENT_FILTER} pattern=synexia:iop; algorithm=Initializes CONTENT_FILTER from the existing expression <code>new EIPType("Remove data from message")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored CONTENT_FILTER metadata in EIPType.
         * @m3.member {@code FIELD:NORMALIZER} pattern=synexia:iop; algorithm=Initializes NORMALIZER from the existing expression <code>new EIPType("Convert to common format")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored NORMALIZER metadata in EIPType.
         * @m3.member {@code FIELD:CANONICAL_DATA_MODEL} pattern=synexia:iop; algorithm=Initializes CANONICAL_DATA_MODEL from the existing expression <code>new EIPType("Standard format")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored CANONICAL_DATA_MODEL metadata in EIPType.
         * @m3.member {@code FIELD:MESSAGE_ENDPOINT} pattern=synexia:iop; algorithm=Initializes MESSAGE_ENDPOINT from the existing expression <code>new EIPType("Connect to channel")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored MESSAGE_ENDPOINT metadata in EIPType.
         * @m3.member {@code FIELD:POLLING_CONSUMER} pattern=synexia:iop; algorithm=Initializes POLLING_CONSUMER from the existing expression <code>new EIPType("Pull messages")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored POLLING_CONSUMER metadata in EIPType.
         * @m3.member {@code FIELD:EVENT_DRIVEN_CONSUMER} pattern=synexia:iop; algorithm=Initializes EVENT_DRIVEN_CONSUMER from the existing expression <code>new EIPType("Push messages")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored EVENT_DRIVEN_CONSUMER metadata in EIPType.
         * @m3.member {@code FIELD:COMPETING_CONSUMERS} pattern=synexia:iop; algorithm=Initializes COMPETING_CONSUMERS from the existing expression <code>new EIPType("Parallel processing")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored COMPETING_CONSUMERS metadata in EIPType.
         * @m3.member {@code FIELD:MESSAGE_DISPATCHER} pattern=synexia:iop; algorithm=Initializes MESSAGE_DISPATCHER from the existing expression <code>new EIPType("Route to handlers")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored MESSAGE_DISPATCHER metadata in EIPType.
         * @m3.member {@code FIELD:WIRE_TAP} pattern=synexia:iop; algorithm=Initializes WIRE_TAP from the existing expression <code>new EIPType("Monitor messages")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored WIRE_TAP metadata in EIPType.
         * @m3.member {@code FIELD:MESSAGE_HISTORY} pattern=synexia:iop; algorithm=Initializes MESSAGE_HISTORY from the existing expression <code>new EIPType("Track message path")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored MESSAGE_HISTORY metadata in EIPType.
         * @m3.member {@code FIELD:MESSAGE_STORE} pattern=synexia:iop; algorithm=Initializes MESSAGE_STORE from the existing expression <code>new EIPType("Persist messages")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored MESSAGE_STORE metadata in EIPType.
         * @m3.member {@code FIELD:DEAD_LETTER_CHANNEL} pattern=synexia:iop; algorithm=Initializes DEAD_LETTER_CHANNEL from the existing expression <code>new EIPType("Handle failures")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored DEAD_LETTER_CHANNEL metadata in EIPType.
         * @m3.member {@code FIELD:INVALID_MESSAGE_CHANNEL} pattern=synexia:iop; algorithm=Initializes INVALID_MESSAGE_CHANNEL from the existing expression <code>new EIPType("Handle invalid")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EIPType field with its declared initializer.; description=Stored INVALID_MESSAGE_CHANNEL metadata in EIPType.
         * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-7158; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in EIPType.
         */
        public enum EIPType {

            // Messaging
            MESSAGE_CHANNEL("Point-to-point channel"),
            PUBLISH_SUBSCRIBE("Pub/sub channel"),
            MESSAGE_ROUTER("Route based on content"),
            MESSAGE_TRANSLATOR("Transform message"),
            MESSAGE_FILTER("Filter messages"),
            CONTENT_BASED_ROUTER("Route by content"),
            SPLITTER("Split message"),
            AGGREGATOR("Combine messages"),
            SCATTER_GATHER("Broadcast and collect"),
            ROUTING_SLIP("Dynamic routing"),
            PROCESS_MANAGER("Orchestrate process"),

            // Transformation
            CONTENT_ENRICHER("Add data to message"),
            CONTENT_FILTER("Remove data from message"),
            NORMALIZER("Convert to common format"),
            CANONICAL_DATA_MODEL("Standard format"),

            // Endpoints
            MESSAGE_ENDPOINT("Connect to channel"),
            POLLING_CONSUMER("Pull messages"),
            EVENT_DRIVEN_CONSUMER("Push messages"),
            COMPETING_CONSUMERS("Parallel processing"),
            MESSAGE_DISPATCHER("Route to handlers"),

            // System Management
            WIRE_TAP("Monitor messages"),
            MESSAGE_HISTORY("Track message path"),
            MESSAGE_STORE("Persist messages"),
            DEAD_LETTER_CHANNEL("Handle failures"),
            INVALID_MESSAGE_CHANNEL("Handle invalid");

            private final String description;

            /**
             * EIPType descriptor-field constructor.
             * @m3.pattern synexia:iop
             * @m3.algorithm Assigns the supplied description references to description fields in source order without validation or defensive copying.
             * @m3.dataStructures Constructor parameters and corresponding final enum descriptor fields: description.
             */
            EIPType(String description) {
                this.description = description;
            }

            /**
             * description metadata operation of EIPType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the stored description field or record component directly without copying, validation or policy execution.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            public String description() {
                return description;
            }
        }

        /**
         * of metadata operation of EIP.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates and returns the value from <code>new EIP(type)</code>, evaluating constructor arguments in source order. Supplied references are retained directly and construction adds no validation.
         * @m3.dataStructures Declared EIP result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static EIP of(EIPType type) {
            return new EIP(type);
        }
    }

    // --- MICROSERVICE PATTERNS ---

    /**
     * Microservice metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied Microservice metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: MicroserviceType type. References are retained directly and primitive component values are stored unchanged.
     * @m3.member {@code FIELD:type} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The type declaration supplies a stored MicroserviceType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-7559; data.structures=One stored MicroserviceType component or field. Reference values are retained and exposed without copying, including null.; description=Stored type metadata in Microservice.
     */
    record Microservice(MicroserviceType type) implements IIOPPattern {
        /**
         * name metadata operation of Microservice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.name()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return type.name();
        }

        /**
         * description metadata operation of Microservice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.description()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return type.description();
        }

        /**
         * targetLayer metadata operation of Microservice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"COP/AOP"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String targetLayer() {
            return "COP/AOP";
        }

        /**
         * survival metadata operation of Microservice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>PatternSurvival.ALWAYS_HOLD</code> without runtime enforcement.
         * @m3.dataStructures Declared PatternSurvival result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public PatternSurvival survival() {
            return PatternSurvival.ALWAYS_HOLD;
        }

        /**
         * MicroserviceType metadata enumeration.
         * @m3.pattern synexia:iop
         * @m3.algorithm Defines the existing 20 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
         * @m3.dataStructures Enum values in source order: CIRCUIT_BREAKER, BULKHEAD, RETRY, TIMEOUT, FALLBACK, RATE_LIMITER, SERVICE_REGISTRY, SERVICE_DISCOVERY, LOAD_BALANCER, API_GATEWAY, SIDECAR, DATABASE_PER_SERVICE, SAGA, EVENT_SOURCING, CQRS, OUTBOX, DISTRIBUTED_TRACING, HEALTH_CHECK, LOG_AGGREGATION, METRICS.
         * @m3.member {@code FIELD:CIRCUIT_BREAKER} pattern=synexia:iop; algorithm=Initializes CIRCUIT_BREAKER from the existing expression <code>new MicroserviceType("Fail fast on failure")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored CIRCUIT_BREAKER metadata in MicroserviceType.
         * @m3.member {@code FIELD:BULKHEAD} pattern=synexia:iop; algorithm=Initializes BULKHEAD from the existing expression <code>new MicroserviceType("Isolate failures")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored BULKHEAD metadata in MicroserviceType.
         * @m3.member {@code FIELD:RETRY} pattern=synexia:iop; algorithm=Initializes RETRY from the existing expression <code>new MicroserviceType("Retry on failure")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored RETRY metadata in MicroserviceType.
         * @m3.member {@code FIELD:TIMEOUT} pattern=synexia:iop; algorithm=Initializes TIMEOUT from the existing expression <code>new MicroserviceType("Limit wait time")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored TIMEOUT metadata in MicroserviceType.
         * @m3.member {@code FIELD:FALLBACK} pattern=synexia:iop; algorithm=Initializes FALLBACK from the existing expression <code>new MicroserviceType("Default on failure")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored FALLBACK metadata in MicroserviceType.
         * @m3.member {@code FIELD:RATE_LIMITER} pattern=synexia:iop; algorithm=Initializes RATE_LIMITER from the existing expression <code>new MicroserviceType("Limit request rate")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored RATE_LIMITER metadata in MicroserviceType.
         * @m3.member {@code FIELD:SERVICE_REGISTRY} pattern=synexia:iop; algorithm=Initializes SERVICE_REGISTRY from the existing expression <code>new MicroserviceType("Register services")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored SERVICE_REGISTRY metadata in MicroserviceType.
         * @m3.member {@code FIELD:SERVICE_DISCOVERY} pattern=synexia:iop; algorithm=Initializes SERVICE_DISCOVERY from the existing expression <code>new MicroserviceType("Find services")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored SERVICE_DISCOVERY metadata in MicroserviceType.
         * @m3.member {@code FIELD:LOAD_BALANCER} pattern=synexia:iop; algorithm=Initializes LOAD_BALANCER from the existing expression <code>new MicroserviceType("Distribute load")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored LOAD_BALANCER metadata in MicroserviceType.
         * @m3.member {@code FIELD:API_GATEWAY} pattern=synexia:iop; algorithm=Initializes API_GATEWAY from the existing expression <code>new MicroserviceType("Single entry point")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored API_GATEWAY metadata in MicroserviceType.
         * @m3.member {@code FIELD:SIDECAR} pattern=synexia:iop; algorithm=Initializes SIDECAR from the existing expression <code>new MicroserviceType("Attach functionality")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored SIDECAR metadata in MicroserviceType.
         * @m3.member {@code FIELD:DATABASE_PER_SERVICE} pattern=synexia:iop; algorithm=Initializes DATABASE_PER_SERVICE from the existing expression <code>new MicroserviceType("Isolated data")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored DATABASE_PER_SERVICE metadata in MicroserviceType.
         * @m3.member {@code FIELD:SAGA} pattern=synexia:iop; algorithm=Initializes SAGA from the existing expression <code>new MicroserviceType("Distributed transaction")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored SAGA metadata in MicroserviceType.
         * @m3.member {@code FIELD:EVENT_SOURCING} pattern=synexia:iop; algorithm=Initializes EVENT_SOURCING from the existing expression <code>new MicroserviceType("Store events")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored EVENT_SOURCING metadata in MicroserviceType.
         * @m3.member {@code FIELD:CQRS} pattern=synexia:iop; algorithm=Initializes CQRS from the existing expression <code>new MicroserviceType("Separate read/write")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored CQRS metadata in MicroserviceType.
         * @m3.member {@code FIELD:OUTBOX} pattern=synexia:iop; algorithm=Initializes OUTBOX from the existing expression <code>new MicroserviceType("Reliable events")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored OUTBOX metadata in MicroserviceType.
         * @m3.member {@code FIELD:DISTRIBUTED_TRACING} pattern=synexia:iop; algorithm=Initializes DISTRIBUTED_TRACING from the existing expression <code>new MicroserviceType("Track requests")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored DISTRIBUTED_TRACING metadata in MicroserviceType.
         * @m3.member {@code FIELD:HEALTH_CHECK} pattern=synexia:iop; algorithm=Initializes HEALTH_CHECK from the existing expression <code>new MicroserviceType("Monitor health")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored HEALTH_CHECK metadata in MicroserviceType.
         * @m3.member {@code FIELD:LOG_AGGREGATION} pattern=synexia:iop; algorithm=Initializes LOG_AGGREGATION from the existing expression <code>new MicroserviceType("Centralize logs")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored LOG_AGGREGATION metadata in MicroserviceType.
         * @m3.member {@code FIELD:METRICS} pattern=synexia:iop; algorithm=Initializes METRICS from the existing expression <code>new MicroserviceType("Collect metrics")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One MicroserviceType field with its declared initializer.; description=Stored METRICS metadata in MicroserviceType.
         * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-9106; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in MicroserviceType.
         */
        public enum MicroserviceType {

            // Resilience
            CIRCUIT_BREAKER("Fail fast on failure"),
            BULKHEAD("Isolate failures"),
            RETRY("Retry on failure"),
            TIMEOUT("Limit wait time"),
            FALLBACK("Default on failure"),
            RATE_LIMITER("Limit request rate"),

            // Discovery & Routing
            SERVICE_REGISTRY("Register services"),
            SERVICE_DISCOVERY("Find services"),
            LOAD_BALANCER("Distribute load"),
            API_GATEWAY("Single entry point"),
            SIDECAR("Attach functionality"),

            // Data Management
            DATABASE_PER_SERVICE("Isolated data"),
            SAGA("Distributed transaction"),
            EVENT_SOURCING("Store events"),
            CQRS("Separate read/write"),
            OUTBOX("Reliable events"),

            // Observability
            DISTRIBUTED_TRACING("Track requests"),
            HEALTH_CHECK("Monitor health"),
            LOG_AGGREGATION("Centralize logs"),
            METRICS("Collect metrics");

            private final String description;

            /**
             * MicroserviceType descriptor-field constructor.
             * @m3.pattern synexia:iop
             * @m3.algorithm Assigns the supplied description references to description fields in source order without validation or defensive copying.
             * @m3.dataStructures Constructor parameters and corresponding final enum descriptor fields: description.
             */
            MicroserviceType(String description) {
                this.description = description;
            }

            /**
             * description metadata operation of MicroserviceType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the stored description field or record component directly without copying, validation or policy execution.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            public String description() {
                return description;
            }
        }

        /**
         * of metadata operation of Microservice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates and returns the value from <code>new Microservice(type)</code>, evaluating constructor arguments in source order. Supplied references are retained directly and construction adds no validation.
         * @m3.dataStructures Declared Microservice result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static Microservice of(MicroserviceType type) {
            return new Microservice(type);
        }
    }

    // --- DISTRIBUTED PATTERNS ---

    /**
     * Distributed metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied Distributed metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: DistributedType type. References are retained directly and primitive component values are stored unchanged.
     * @m3.member {@code FIELD:type} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The type declaration supplies a stored DistributedType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-9541; data.structures=One stored DistributedType component or field. Reference values are retained and exposed without copying, including null.; description=Stored type metadata in Distributed.
     */
    record Distributed(DistributedType type) implements IIOPPattern {
        /**
         * name metadata operation of Distributed.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.name()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return type.name();
        }

        /**
         * description metadata operation of Distributed.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.description()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return type.description();
        }

        /**
         * targetLayer metadata operation of Distributed.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"COP/IOP"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String targetLayer() {
            return "COP/IOP";
        }

        /**
         * survival metadata operation of Distributed.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>PatternSurvival.ALWAYS_HOLD</code> without runtime enforcement.
         * @m3.dataStructures Declared PatternSurvival result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public PatternSurvival survival() {
            return PatternSurvival.ALWAYS_HOLD;
        }

        /**
         * DistributedType metadata enumeration.
         * @m3.pattern synexia:iop
         * @m3.algorithm Defines the existing 10 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
         * @m3.dataStructures Enum values in source order: LEADER_ELECTION, CONSENSUS, SHARDING, REPLICATION, CONSISTENT_HASHING, TWO_PHASE_COMMIT, THREE_PHASE_COMMIT, VECTOR_CLOCK, CRDT, GOSSIP_PROTOCOL.
         * @m3.member {@code FIELD:LEADER_ELECTION} pattern=synexia:iop; algorithm=Initializes LEADER_ELECTION from the existing expression <code>new DistributedType("Elect coordinator")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DistributedType field with its declared initializer.; description=Stored LEADER_ELECTION metadata in DistributedType.
         * @m3.member {@code FIELD:CONSENSUS} pattern=synexia:iop; algorithm=Initializes CONSENSUS from the existing expression <code>new DistributedType("Agree on value")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DistributedType field with its declared initializer.; description=Stored CONSENSUS metadata in DistributedType.
         * @m3.member {@code FIELD:SHARDING} pattern=synexia:iop; algorithm=Initializes SHARDING from the existing expression <code>new DistributedType("Distribute data")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DistributedType field with its declared initializer.; description=Stored SHARDING metadata in DistributedType.
         * @m3.member {@code FIELD:REPLICATION} pattern=synexia:iop; algorithm=Initializes REPLICATION from the existing expression <code>new DistributedType("Copy data")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DistributedType field with its declared initializer.; description=Stored REPLICATION metadata in DistributedType.
         * @m3.member {@code FIELD:CONSISTENT_HASHING} pattern=synexia:iop; algorithm=Initializes CONSISTENT_HASHING from the existing expression <code>new DistributedType("Distribute evenly")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DistributedType field with its declared initializer.; description=Stored CONSISTENT_HASHING metadata in DistributedType.
         * @m3.member {@code FIELD:TWO_PHASE_COMMIT} pattern=synexia:iop; algorithm=Initializes TWO_PHASE_COMMIT from the existing expression <code>new DistributedType("Atomic transaction")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DistributedType field with its declared initializer.; description=Stored TWO_PHASE_COMMIT metadata in DistributedType.
         * @m3.member {@code FIELD:THREE_PHASE_COMMIT} pattern=synexia:iop; algorithm=Initializes THREE_PHASE_COMMIT from the existing expression <code>new DistributedType("Non-blocking commit")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DistributedType field with its declared initializer.; description=Stored THREE_PHASE_COMMIT metadata in DistributedType.
         * @m3.member {@code FIELD:VECTOR_CLOCK} pattern=synexia:iop; algorithm=Initializes VECTOR_CLOCK from the existing expression <code>new DistributedType("Track causality")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DistributedType field with its declared initializer.; description=Stored VECTOR_CLOCK metadata in DistributedType.
         * @m3.member {@code FIELD:CRDT} pattern=synexia:iop; algorithm=Initializes CRDT from the existing expression <code>new DistributedType("Conflict-free replicated")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DistributedType field with its declared initializer.; description=Stored CRDT metadata in DistributedType.
         * @m3.member {@code FIELD:GOSSIP_PROTOCOL} pattern=synexia:iop; algorithm=Initializes GOSSIP_PROTOCOL from the existing expression <code>new DistributedType("Spread information")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DistributedType field with its declared initializer.; description=Stored GOSSIP_PROTOCOL metadata in DistributedType.
         * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-10527; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in DistributedType.
         */
        public enum DistributedType {
            LEADER_ELECTION("Elect coordinator"),
            CONSENSUS("Agree on value"),
            SHARDING("Distribute data"),
            REPLICATION("Copy data"),
            CONSISTENT_HASHING("Distribute evenly"),
            TWO_PHASE_COMMIT("Atomic transaction"),
            THREE_PHASE_COMMIT("Non-blocking commit"),
            VECTOR_CLOCK("Track causality"),
            CRDT("Conflict-free replicated"),
            GOSSIP_PROTOCOL("Spread information");

            private final String description;

            /**
             * DistributedType descriptor-field constructor.
             * @m3.pattern synexia:iop
             * @m3.algorithm Assigns the supplied description references to description fields in source order without validation or defensive copying.
             * @m3.dataStructures Constructor parameters and corresponding final enum descriptor fields: description.
             */
            DistributedType(String description) {
                this.description = description;
            }

            /**
             * description metadata operation of DistributedType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the stored description field or record component directly without copying, validation or policy execution.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            public String description() {
                return description;
            }
        }

        /**
         * of metadata operation of Distributed.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates and returns the value from <code>new Distributed(type)</code>, evaluating constructor arguments in source order. Supplied references are retained directly and construction adds no validation.
         * @m3.dataStructures Declared Distributed result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static Distributed of(DistributedType type) {
            return new Distributed(type);
        }
    }

    // --- DAG FLOW PATTERNS ---

    /**
     * DAG metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied DAG metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: DAGType type. References are retained directly and primitive component values are stored unchanged.
     * @m3.member {@code FIELD:type} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The type declaration supplies a stored DAGType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-10947; data.structures=One stored DAGType component or field. Reference values are retained and exposed without copying, including null.; description=Stored type metadata in DAG.
     */
    record DAG(DAGType type) implements IIOPPattern {
        /**
         * name metadata operation of DAG.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.name()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return type.name();
        }

        /**
         * description metadata operation of DAG.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.description()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return type.description();
        }

        /**
         * targetLayer metadata operation of DAG.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"DAG/COP"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String targetLayer() {
            return "DAG/COP";
        }

        /**
         * survival metadata operation of DAG.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>PatternSurvival.ALWAYS_HOLD</code> without runtime enforcement.
         * @m3.dataStructures Declared PatternSurvival result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public PatternSurvival survival() {
            return PatternSurvival.ALWAYS_HOLD;
        }

        /**
         * DAGType metadata enumeration.
         * @m3.pattern synexia:iop
         * @m3.algorithm Defines the existing 10 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
         * @m3.dataStructures Enum values in source order: PIPELINE, FORK_JOIN, MAP_REDUCE, SCATTER_GATHER, SAGA, REACT, PLAN_EXECUTE, HIERARCHICAL, WORKFLOW, STATE_MACHINE.
         * @m3.member {@code FIELD:PIPELINE} pattern=synexia:iop; algorithm=Initializes PIPELINE from the existing expression <code>new DAGType("Sequential stages")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DAGType field with its declared initializer.; description=Stored PIPELINE metadata in DAGType.
         * @m3.member {@code FIELD:FORK_JOIN} pattern=synexia:iop; algorithm=Initializes FORK_JOIN from the existing expression <code>new DAGType("Parallel then merge")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DAGType field with its declared initializer.; description=Stored FORK_JOIN metadata in DAGType.
         * @m3.member {@code FIELD:MAP_REDUCE} pattern=synexia:iop; algorithm=Initializes MAP_REDUCE from the existing expression <code>new DAGType("Distribute and aggregate")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DAGType field with its declared initializer.; description=Stored MAP_REDUCE metadata in DAGType.
         * @m3.member {@code FIELD:SCATTER_GATHER} pattern=synexia:iop; algorithm=Initializes SCATTER_GATHER from the existing expression <code>new DAGType("Broadcast and collect")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DAGType field with its declared initializer.; description=Stored SCATTER_GATHER metadata in DAGType.
         * @m3.member {@code FIELD:SAGA} pattern=synexia:iop; algorithm=Initializes SAGA from the existing expression <code>new DAGType("Compensating transactions")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DAGType field with its declared initializer.; description=Stored SAGA metadata in DAGType.
         * @m3.member {@code FIELD:REACT} pattern=synexia:iop; algorithm=Initializes REACT from the existing expression <code>new DAGType("Reason-Act loop")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DAGType field with its declared initializer.; description=Stored REACT metadata in DAGType.
         * @m3.member {@code FIELD:PLAN_EXECUTE} pattern=synexia:iop; algorithm=Initializes PLAN_EXECUTE from the existing expression <code>new DAGType("Plan then execute")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DAGType field with its declared initializer.; description=Stored PLAN_EXECUTE metadata in DAGType.
         * @m3.member {@code FIELD:HIERARCHICAL} pattern=synexia:iop; algorithm=Initializes HIERARCHICAL from the existing expression <code>new DAGType("Nested sub-tasks")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DAGType field with its declared initializer.; description=Stored HIERARCHICAL metadata in DAGType.
         * @m3.member {@code FIELD:WORKFLOW} pattern=synexia:iop; algorithm=Initializes WORKFLOW from the existing expression <code>new DAGType("Complex orchestration")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DAGType field with its declared initializer.; description=Stored WORKFLOW metadata in DAGType.
         * @m3.member {@code FIELD:STATE_MACHINE} pattern=synexia:iop; algorithm=Initializes STATE_MACHINE from the existing expression <code>new DAGType("State transitions")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One DAGType field with its declared initializer.; description=Stored STATE_MACHINE metadata in DAGType.
         * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-11912; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in DAGType.
         */
        public enum DAGType {
            PIPELINE("Sequential stages"),
            FORK_JOIN("Parallel then merge"),
            MAP_REDUCE("Distribute and aggregate"),
            SCATTER_GATHER("Broadcast and collect"),
            SAGA("Compensating transactions"),
            REACT("Reason-Act loop"),
            PLAN_EXECUTE("Plan then execute"),
            HIERARCHICAL("Nested sub-tasks"),
            WORKFLOW("Complex orchestration"),
            STATE_MACHINE("State transitions");

            private final String description;

            /**
             * DAGType descriptor-field constructor.
             * @m3.pattern synexia:iop
             * @m3.algorithm Assigns the supplied description references to description fields in source order without validation or defensive copying.
             * @m3.dataStructures Constructor parameters and corresponding final enum descriptor fields: description.
             */
            DAGType(String description) {
                this.description = description;
            }

            /**
             * description metadata operation of DAGType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the stored description field or record component directly without copying, validation or policy execution.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            public String description() {
                return description;
            }
        }

        /**
         * of metadata operation of DAG.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates and returns the value from <code>new DAG(type)</code>, evaluating constructor arguments in source order. Supplied references are retained directly and construction adds no validation.
         * @m3.dataStructures Declared DAG result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static DAG of(DAGType type) {
            return new DAG(type);
        }
    }

    // --- SPRING PATTERNS ---

    /**
     * Spring metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied Spring metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: SpringType type. References are retained directly and primitive component values are stored unchanged.
     * @m3.member {@code FIELD:type} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The type declaration supplies a stored SpringType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-12301; data.structures=One stored SpringType component or field. Reference values are retained and exposed without copying, including null.; description=Stored type metadata in Spring.
     */
    record Spring(SpringType type) implements IIOPPattern {
        /**
         * name metadata operation of Spring.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.name()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return type.name();
        }

        /**
         * description metadata operation of Spring.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.description()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return type.description();
        }

        /**
         * targetLayer metadata operation of Spring.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"AOP/IOP"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String targetLayer() {
            return "AOP/IOP";
        }

        /**
         * survival metadata operation of Spring.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>PatternSurvival.TRANSFORM</code> without runtime enforcement.
         * @m3.dataStructures Declared PatternSurvival result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public PatternSurvival survival() {
            return PatternSurvival.TRANSFORM;
        }

        /**
         * SpringType metadata enumeration.
         * @m3.pattern synexia:iop
         * @m3.algorithm Defines the existing 10 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
         * @m3.dataStructures Enum values in source order: DEPENDENCY_INJECTION, IOC_CONTAINER, MVC, REPOSITORY, SERVICE, COMPONENT, CONFIGURATION, TEMPLATE_METHOD, PROFILE, CONDITIONAL.
         * @m3.member {@code FIELD:DEPENDENCY_INJECTION} pattern=synexia:iop; algorithm=Initializes DEPENDENCY_INJECTION from the existing expression <code>new SpringType("Invert control")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One SpringType field with its declared initializer.; description=Stored DEPENDENCY_INJECTION metadata in SpringType.
         * @m3.member {@code FIELD:IOC_CONTAINER} pattern=synexia:iop; algorithm=Initializes IOC_CONTAINER from the existing expression <code>new SpringType("Manage beans")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One SpringType field with its declared initializer.; description=Stored IOC_CONTAINER metadata in SpringType.
         * @m3.member {@code FIELD:MVC} pattern=synexia:iop; algorithm=Initializes MVC from the existing expression <code>new SpringType("Model-View-Controller")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One SpringType field with its declared initializer.; description=Stored MVC metadata in SpringType.
         * @m3.member {@code FIELD:REPOSITORY} pattern=synexia:iop; algorithm=Initializes REPOSITORY from the existing expression <code>new SpringType("Data access")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One SpringType field with its declared initializer.; description=Stored REPOSITORY metadata in SpringType.
         * @m3.member {@code FIELD:SERVICE} pattern=synexia:iop; algorithm=Initializes SERVICE from the existing expression <code>new SpringType("Business logic")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One SpringType field with its declared initializer.; description=Stored SERVICE metadata in SpringType.
         * @m3.member {@code FIELD:COMPONENT} pattern=synexia:iop; algorithm=Initializes COMPONENT from the existing expression <code>new SpringType("Generic bean")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One SpringType field with its declared initializer.; description=Stored COMPONENT metadata in SpringType.
         * @m3.member {@code FIELD:CONFIGURATION} pattern=synexia:iop; algorithm=Initializes CONFIGURATION from the existing expression <code>new SpringType("Java config")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One SpringType field with its declared initializer.; description=Stored CONFIGURATION metadata in SpringType.
         * @m3.member {@code FIELD:TEMPLATE_METHOD} pattern=synexia:iop; algorithm=Initializes TEMPLATE_METHOD from the existing expression <code>new SpringType("JdbcTemplate etc")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One SpringType field with its declared initializer.; description=Stored TEMPLATE_METHOD metadata in SpringType.
         * @m3.member {@code FIELD:PROFILE} pattern=synexia:iop; algorithm=Initializes PROFILE from the existing expression <code>new SpringType("Environment config")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One SpringType field with its declared initializer.; description=Stored PROFILE metadata in SpringType.
         * @m3.member {@code FIELD:CONDITIONAL} pattern=synexia:iop; algorithm=Initializes CONDITIONAL from the existing expression <code>new SpringType("Conditional beans")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One SpringType field with its declared initializer.; description=Stored CONDITIONAL metadata in SpringType.
         * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-13237; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in SpringType.
         */
        public enum SpringType {
            DEPENDENCY_INJECTION("Invert control"),
            IOC_CONTAINER("Manage beans"),
            MVC("Model-View-Controller"),
            REPOSITORY("Data access"),
            SERVICE("Business logic"),
            COMPONENT("Generic bean"),
            CONFIGURATION("Java config"),
            TEMPLATE_METHOD("JdbcTemplate etc"),
            PROFILE("Environment config"),
            CONDITIONAL("Conditional beans");

            private final String description;

            /**
             * SpringType descriptor-field constructor.
             * @m3.pattern synexia:iop
             * @m3.algorithm Assigns the supplied description references to description fields in source order without validation or defensive copying.
             * @m3.dataStructures Constructor parameters and corresponding final enum descriptor fields: description.
             */
            SpringType(String description) {
                this.description = description;
            }

            /**
             * description metadata operation of SpringType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the stored description field or record component directly without copying, validation or policy execution.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            public String description() {
                return description;
            }
        }

        /**
         * of metadata operation of Spring.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates and returns the value from <code>new Spring(type)</code>, evaluating constructor arguments in source order. Supplied references are retained directly and construction adds no validation.
         * @m3.dataStructures Declared Spring result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static Spring of(SpringType type) {
            return new Spring(type);
        }
    }

    // --- AOP ADVICE PATTERNS ---

    /**
     * AOPAdvice metadata value record.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied AOPAdvice metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: AOPAdviceType type. References are retained directly and primitive component values are stored unchanged.
     * @m3.member {@code FIELD:type} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The type declaration supplies a stored AOPAdviceType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-13645; data.structures=One stored AOPAdviceType component or field. Reference values are retained and exposed without copying, including null.; description=Stored type metadata in AOPAdvice.
     */
    record AOPAdvice(AOPAdviceType type) implements IIOPPattern {
        /**
         * name metadata operation of AOPAdvice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.name()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String name() {
            return type.name();
        }

        /**
         * description metadata operation of AOPAdvice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Calls <code>type.description()</code> on the stored enum reference and returns the result. A null type throws NullPointerException at the dereference.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String description() {
            return type.description();
        }

        /**
         * targetLayer metadata operation of AOPAdvice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed String literal <code>"AOP"</code> directly.
         * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public String targetLayer() {
            return "AOP";
        }

        /**
         * survival metadata operation of AOPAdvice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>PatternSurvival.TRANSFORM</code> without runtime enforcement.
         * @m3.dataStructures Declared PatternSurvival result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public PatternSurvival survival() {
            return PatternSurvival.TRANSFORM;
        }

        /**
         * AOPAdviceType metadata enumeration.
         * @m3.pattern synexia:iop
         * @m3.algorithm Defines the existing 9 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
         * @m3.dataStructures Enum values in source order: BEFORE, AFTER, AFTER_RETURNING, AFTER_THROWING, AROUND, POINTCUT, ASPECT, INTRODUCTION, WEAVING.
         * @m3.member {@code FIELD:BEFORE} pattern=synexia:iop; algorithm=Initializes BEFORE from the existing expression <code>new AOPAdviceType("Before method execution")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One AOPAdviceType field with its declared initializer.; description=Stored BEFORE metadata in AOPAdviceType.
         * @m3.member {@code FIELD:AFTER} pattern=synexia:iop; algorithm=Initializes AFTER from the existing expression <code>new AOPAdviceType("After method execution")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One AOPAdviceType field with its declared initializer.; description=Stored AFTER metadata in AOPAdviceType.
         * @m3.member {@code FIELD:AFTER_RETURNING} pattern=synexia:iop; algorithm=Initializes AFTER_RETURNING from the existing expression <code>new AOPAdviceType("After successful return")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One AOPAdviceType field with its declared initializer.; description=Stored AFTER_RETURNING metadata in AOPAdviceType.
         * @m3.member {@code FIELD:AFTER_THROWING} pattern=synexia:iop; algorithm=Initializes AFTER_THROWING from the existing expression <code>new AOPAdviceType("After exception")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One AOPAdviceType field with its declared initializer.; description=Stored AFTER_THROWING metadata in AOPAdviceType.
         * @m3.member {@code FIELD:AROUND} pattern=synexia:iop; algorithm=Initializes AROUND from the existing expression <code>new AOPAdviceType("Wrap method execution")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One AOPAdviceType field with its declared initializer.; description=Stored AROUND metadata in AOPAdviceType.
         * @m3.member {@code FIELD:POINTCUT} pattern=synexia:iop; algorithm=Initializes POINTCUT from the existing expression <code>new AOPAdviceType("Define join points")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One AOPAdviceType field with its declared initializer.; description=Stored POINTCUT metadata in AOPAdviceType.
         * @m3.member {@code FIELD:ASPECT} pattern=synexia:iop; algorithm=Initializes ASPECT from the existing expression <code>new AOPAdviceType("Cross-cutting concern")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One AOPAdviceType field with its declared initializer.; description=Stored ASPECT metadata in AOPAdviceType.
         * @m3.member {@code FIELD:INTRODUCTION} pattern=synexia:iop; algorithm=Initializes INTRODUCTION from the existing expression <code>new AOPAdviceType("Add interface")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One AOPAdviceType field with its declared initializer.; description=Stored INTRODUCTION metadata in AOPAdviceType.
         * @m3.member {@code FIELD:WEAVING} pattern=synexia:iop; algorithm=Initializes WEAVING from the existing expression <code>new AOPAdviceType("Combine aspects")</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One AOPAdviceType field with its declared initializer.; description=Stored WEAVING metadata in AOPAdviceType.
         * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPPattern-14561; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in AOPAdviceType.
         */
        public enum AOPAdviceType {
            BEFORE("Before method execution"),
            AFTER("After method execution"),
            AFTER_RETURNING("After successful return"),
            AFTER_THROWING("After exception"),
            AROUND("Wrap method execution"),
            POINTCUT("Define join points"),
            ASPECT("Cross-cutting concern"),
            INTRODUCTION("Add interface"),
            WEAVING("Combine aspects");

            private final String description;

            /**
             * AOPAdviceType descriptor-field constructor.
             * @m3.pattern synexia:iop
             * @m3.algorithm Assigns the supplied description references to description fields in source order without validation or defensive copying.
             * @m3.dataStructures Constructor parameters and corresponding final enum descriptor fields: description.
             */
            AOPAdviceType(String description) {
                this.description = description;
            }

            /**
             * description metadata operation of AOPAdviceType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the stored description field or record component directly without copying, validation or policy execution.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            public String description() {
                return description;
            }
        }

        /**
         * of metadata operation of AOPAdvice.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates and returns the value from <code>new AOPAdvice(type)</code>, evaluating constructor arguments in source order. Supplied references are retained directly and construction adds no validation.
         * @m3.dataStructures Declared AOPAdvice result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static AOPAdvice of(AOPAdviceType type) {
            return new AOPAdvice(type);
        }
    }
}
