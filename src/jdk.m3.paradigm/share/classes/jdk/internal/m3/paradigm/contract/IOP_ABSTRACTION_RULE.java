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

import jdk.internal.m3.collections.PlaceholderLogger;

/**
 * IOP v1 - Mandatory Abstraction Rule (SAVED).
 *
 * <h2>NORMATIVE RULE (MUST)</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                                                                           ║
 * ║   In IOP, every behavioral class MUST have at least one abstract         ║
 * ║   contract. The contract MUST be either:                                 ║
 * ║                                                                           ║
 * ║     • an interface, or                                                   ║
 * ║     • a generated abstract base class                                    ║
 * ║                                                                           ║
 * ║   A concrete behavioral class without an abstract contract is illegal.  ║
 * ║                                                                           ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <p>
 * This rule applies <strong>even if the concrete class is empty or
 * trivial</strong>.
 *
 * <h2>CANONICAL IOP SENTENCE (LOCKED)</h2>
 *
 * <pre>
 * Behavior requires intent.
 * Intent requires abstraction.
 * Abstraction is mandatory in IOP.
 * </pre>
 *
 * @since IOP 1.0
 * @m3.pattern synexia:iop
 * @m3.algorithm Groups exact law-text constants, abstraction/scope enums, a validation interface and record descriptors. Helpers use suffix checks, String concatenation and Set membership without source inspection or enforcing the textual laws.
 * @m3.dataStructures Static String constants, nested enums/interface/records, primitive flags, String arrays and caller-owned sets, plus a Lombok-generated static logger.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IOP_ABSTRACTION_RULE.java}
 * blob {@code a3872d0463bf440df14391d8dc94a5c94a5d1d68} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IOP_ABSTRACTION_RULE {
    private static final PlaceholderLogger log = PlaceholderLogger.of(IOP_ABSTRACTION_RULE.class);


    /**
     * Private constructor for the static declaration owner.
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty private constructor body, preventing ordinary caller construction.
     * @m3.dataStructures No constructor parameters or declared instance state.
     */
    private IOP_ABSTRACTION_RULE() {}

    // --- THE NORMATIVE RULE ---

    /**
     * The mandatory abstraction rule.
     *
     * <p>
     * Every behavioral class MUST have at least one abstract contract.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String NORMATIVE_RULE = """
			In IOP, every behavioral class MUST have at least one abstract contract.
			The contract MUST be either:
			  • an interface, or
			  • a generated abstract base class
			A concrete behavioral class without an abstract contract is illegal.
			""";

    // --- CLARIFICATION 1: PURPOSE OF THE ABSTRACTION ---

    /**
     * Purpose of the abstraction.
     *
     * <ul>
     * <li>The abstraction represents <strong>intent</strong>, not variability</li>
     * <li>It anchors semantics, boundaries, and invariants</li>
     * <li>It is the <strong>primary unit of reasoning</strong> for tools and
     * agents</li>
     * </ul>
     *
     * <p>
     * Concrete classes are <strong>replaceable realizations</strong>, not sources
     * of truth.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String PURPOSE = """
			The abstraction represents intent, not variability.
			It anchors semantics, boundaries, and invariants.
			It is the primary unit of reasoning for tools and agents.
			Concrete classes are replaceable realizations, not sources of truth.
			""";

    // --- CLARIFICATION 2: INTERFACE VS ABSTRACT CLASS ---

    /**
     * Interface vs Abstract Class (explicit allowance).
     *
     * <p>
     * IOP allows <strong>either</strong>, based on intent:
     *
     * <h2>Interface</h2>
     * <ul>
     * <li>Pure behavioral intent</li>
     * <li>No default behavior required</li>
     * <li>Preferred for public or stable APIs</li>
     * </ul>
     *
     * <h3>Generated Abstract Class</h3>
     * <ul>
     * <li>Canonical defaults exist</li>
     * <li>Partial implementation is intentional</li>
     * <li>CPU/templates may generate this automatically</li>
     * </ul>
     *
     * <p>
     * Both satisfy the rule.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares 2 enum singleton values in source order. The values label intent and perform no independent validation or enforcement.
     * @m3.dataStructures Ordered enum singleton values and compiler-generated values/valueOf accessors.
     */
    public enum AbstractionType {
        /**
         * Interface - Pure behavioral intent.
         *
         * <ul>
         * <li>No default behavior required</li>
         * <li>Preferred for public or stable APIs</li>
         * </ul>
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the INTERFACE enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        INTERFACE,

        /**
         * Generated Abstract Class.
         *
         * <ul>
         * <li>Canonical defaults exist</li>
         * <li>Partial implementation is intentional</li>
         * <li>CPU/templates may generate automatically</li>
         * </ul>
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares the GENERATED_ABSTRACT_CLASS enum singleton in its original source position, retaining its name and ordinal without additional state.
         * @m3.dataStructures One enum singleton reference and its inherited name and ordinal.
         */
        GENERATED_ABSTRACT_CLASS
    }

    // --- CLARIFICATION 3: GENERATED ABSTRACT CLASS IS FIRST-CLASS ---

    /**
     * Generated abstract class is first-class.
     *
     * <pre>
     * A generated abstract class is considered a valid IOP contract,
     * equal in status to a hand-written interface.
     * </pre>
     *
     * <p>
     * This enables:
     * <ul>
     * <li>CPU/template-driven scaffolding</li>
     * <li>Deterministic defaults</li>
     * <li>Reduced human effort</li>
     * <li>Zero violation of SOLID</li>
     * </ul>
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String GENERATED_IS_FIRST_CLASS = """
			A generated abstract class is considered a valid IOP contract,
			equal in status to a hand-written interface.
			""";

    // --- CLARIFICATION 4: SCOPE OF THE RULE ---

    /** Scope of the rule - where it applies.
     * @m3.pattern synexia:iop
     * @m3.algorithm Constructs 11 ordered enum values with the supplied boolean flag, stores it unchanged and exposes it through requiresAbstraction. The descriptor values do not enforce source restrictions.
     * @m3.dataStructures Enum singleton values with one final boolean field each.
     */
    public enum RuleScope {
        // --- APPLIES TO (behavioral classes) ---

        /** Services
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the SERVICES singleton with the boolean literal true in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        SERVICES(true),

        /** Processors
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the PROCESSORS singleton with the boolean literal true in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        PROCESSORS(true),

        /** Handlers
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the HANDLERS singleton with the boolean literal true in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        HANDLERS(true),

        /** Coordinators
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the COORDINATORS singleton with the boolean literal true in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        COORDINATORS(true),

        /** Adapters
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the ADAPTERS singleton with the boolean literal true in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        ADAPTERS(true),

        /** Any class with behavior
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the ANY_BEHAVIORAL_CLASS singleton with the boolean literal true in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        ANY_BEHAVIORAL_CLASS(true),

        // --- DOES NOT APPLY TO (data classes) ---

        /** DTOs / Records
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the DTOS_RECORDS singleton with the boolean literal false in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        DTOS_RECORDS(false),

        /** Value objects (no behavior)
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the VALUE_OBJECTS singleton with the boolean literal false in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        VALUE_OBJECTS(false),

        /** Enums
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the ENUMS singleton with the boolean literal false in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        ENUMS(false),

        /** Pure data holders
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the DATA_HOLDERS singleton with the boolean literal false in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        DATA_HOLDERS(false),

        /** Private anonymous helpers
         * @m3.pattern synexia:iop
         * @m3.algorithm Constructs the PRIVATE_HELPERS singleton with the boolean literal false in its original ordinal position.
         * @m3.dataStructures One enum singleton and one final boolean flag.
         */
        PRIVATE_HELPERS(false);

        /**
         * Immutable requiresAbstraction descriptor flag.
         * @m3.pattern synexia:iop
         * @m3.algorithm Retains the constructor-supplied requiresAbstraction boolean value in the corresponding final field without validation or normalization.
         * @m3.dataStructures One final boolean field per instance.
         */
        private final boolean requiresAbstraction;

        /**
         * Initializes one RuleScope descriptor flag.
         * @m3.pattern synexia:bop
         * @m3.algorithm Assigns the supplied boolean requiresAbstraction parameter to its final field without validation.
         * @m3.dataStructures One boolean parameter and one final boolean instance field.
         */
        RuleScope(boolean requiresAbstraction) {
            this.requiresAbstraction = requiresAbstraction;
        }

        /**
         * Whether this scope requires an abstraction.
         *
         * @return true if abstraction is required
         * @m3.pattern synexia:bop
         * @m3.algorithm Returns the existing requiresAbstraction field directly without conversion, validation or mutation.
         * @m3.dataStructures One stored boolean value and its returned value.
         */
        public boolean requiresAbstraction() {
            return requiresAbstraction;
        }
    }

    /**
     * Behavioral intent is the key discriminator.
     *
     * @param className
     *            class name to check
     * @param hasBehavior
     *            whether the class has behavioral methods
     * @return true if abstraction is required
     * @m3.pattern synexia:bop
     * @m3.algorithm Returns false when className ends with case-sensitive DTO or Record, otherwise returns hasBehavior. Dereferences the name before checking the flag, so null names throw NullPointerException even when hasBehavior is false.
     * @m3.dataStructures One String reference, one boolean argument and one boolean result.
     */
    public static boolean requiresAbstraction(String className, boolean hasBehavior) {
        // Data classes don't require abstraction
        if (className.endsWith("DTO") || className.endsWith("Record")) {
            return false;
        }
        // Behavioral intent is the key
        return hasBehavior;
    }

    // --- TOOLING ENFORCEMENT ---

    /**
     * Tooling enforcement rules.
     *
     * <p>
     * A build or IDE <strong>MUST fail or warn</strong> if:
     * <ul>
     * <li>A behavioral concrete class has no interface or abstract base</li>
     * <li>A class implements behavior directly without an abstract intent</li>
     * <li>The abstract contract is bypassed by direct usage</li>
     * </ul>
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares a bodyless checkAbstraction method and a nested ValidationResult record. Implementations choose the validation behavior, so this interface performs no checking itself.
     * @m3.dataStructures One String-argument method signature and a nested boolean/String/AbstractionType record.
     */
    public interface ToolingEnforcement {

        /**
         * Check if a class has required abstraction.
         *
         * @param className
         *            class to check
         * @return validation result
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares a String-to-ValidationResult method without a body. It provides no validation algorithm, null policy or exception handling independently of an implementation.
         * @m3.dataStructures One String parameter and one ValidationResult return contract.
         */
        ValidationResult checkAbstraction(String className);

        /** Validation result.
         * @m3.pattern synexia:iop
         * @m3.algorithm Declares a record with the supplied component values retained without a custom canonical constructor or validation. Compiler-generated accessors and record equality/hash/string methods retain ordinary record semantics.
         * @m3.dataStructures Record components: boolean valid, String message, AbstractionType foundType.
         * @m3.member {@code FIELD:valid} pattern=synexia:iop; algorithm=Stores the canonical-constructor component value unchanged. No explicit initializer, copying, validation or normalization is declared.; data.structures=One record component value of type boolean.; description=valid component of the ValidationResult record.
         * @m3.member {@code FIELD:message} pattern=synexia:iop; algorithm=Stores the canonical-constructor component value unchanged. No explicit initializer, copying, validation or normalization is declared.; data.structures=One record component value of type String.; description=message component of the ValidationResult record.
         * @m3.member {@code FIELD:foundType} pattern=synexia:iop; algorithm=Stores the canonical-constructor component value unchanged. No explicit initializer, copying, validation or normalization is declared.; data.structures=One record component value of type AbstractionType.; description=foundType component of the ValidationResult record.
         */
        record ValidationResult(boolean valid, String message, AbstractionType foundType) {}
    }

    // --- THE MINIMUM REQUIREMENT (LOCKED - NON-NEGOTIABLE) ---

    /**
     * IOP Minimum Requirement.
     *
     * <pre>
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║                                                                           ║
     * ║   🔒 IN IOP THERE IS AT MINIMUM:                                         ║
     * ║                                                                           ║
     * ║      1. AN INTERFACE                                                      ║
     * ║      2. ONE GENERATED ABSTRACT CLASS                                      ║
     * ║                                                                           ║
     * ║   This is the ABSOLUTE MINIMUM. Always. No exceptions.                   ║
     * ║   Even if they are empty, they MUST exist.                               ║
     * ║                                                                           ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     * </pre>
     *
     * <p>
     * The interface defines the contract. The abstract class provides the canonical
     * implementation structure. Together they form the minimum viable IOP type.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String MINIMUM_REQUIREMENT = """
			IN IOP THERE IS AT MINIMUM:
			  1. AN INTERFACE
			  2. ONE GENERATED ABSTRACT CLASS

			This is the ABSOLUTE MINIMUM. Always. No exceptions.
			Even if they are empty, they MUST exist.
			""";

    /**
     * IOP Two-Layer Minimum (MANDATORY).
     *
     * <p>
     * This is the absolute minimum for any IOP type:
     *
     * <pre>
     * I{Name}.java        + Abstract{Name}.java
     * (interface)           (generated abstract class)
     * </pre>
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares a record with the supplied component values retained without a custom canonical constructor or validation. Compiler-generated accessors and record equality/hash/string methods retain ordinary record semantics.
     * @m3.dataStructures Record components: String interfaceName, String abstractClassName.
     * @m3.member {@code FIELD:interfaceName} pattern=synexia:iop; algorithm=Stores the canonical-constructor component value unchanged. No explicit initializer, copying, validation or normalization is declared.; data.structures=One record component value of type String.; description=interfaceName component of the TwoLayerMinimum record.
     * @m3.member {@code FIELD:abstractClassName} pattern=synexia:iop; algorithm=Stores the canonical-constructor component value unchanged. No explicit initializer, copying, validation or normalization is declared.; data.structures=One record component value of type String.; description=abstractClassName component of the TwoLayerMinimum record.
     */
    public record TwoLayerMinimum(
            /* Interface name (e.g., IUserService) */
            String interfaceName,
            /* Abstract class name (e.g., AbstractUserService) */
            String abstractClassName) {

        /**
         * Creates two-layer minimum from base name.
         *
         * @param baseName
         *            base type name (e.g., "UserService")
         * @return two-layer minimum
         * @m3.pattern synexia:bop
         * @m3.algorithm Returns a fresh TwoLayerMinimum record using String concatenation for I plus baseName and Abstract plus baseName. Null baseName is converted to the literal text null, and no identifier or name validation occurs.
         * @m3.dataStructures One String input, generated String references and one new record instance.
         */
        public static TwoLayerMinimum fromBaseName(String baseName) {
            return new TwoLayerMinimum("I" + baseName, "Abstract" + baseName);
        }

        /**
         * Validates that both layers exist.
         *
         * @param existingClasses
         *            set of existing class names
         * @return true if both layers exist
         * @m3.pattern synexia:bop
         * @m3.algorithm Calls existingClasses.contains in source order for interfaceName and abstractClassName, returning false at the first missing name and true only if every check succeeds. A null set fails on the first call, and caller-set effects or exceptions propagate.
         * @m3.dataStructures One caller-owned Set of String names, stored component references and boolean membership results. The set is not copied or modified by this method.
         */
        public boolean validate(java.util.Set<String> existingClasses) {
            return existingClasses.contains(interfaceName) && existingClasses.contains(abstractClassName);
        }

        /**
         * Returns file paths for the two mandatory files.
         *
         * @param packagePath
         *            package path (e.g., "jdk/internal/m3/service")
         * @return array of file paths
         * @m3.pattern synexia:bop
         * @m3.algorithm Allocates a new two-element String array containing packagePath plus slash plus each stored layer name plus .java. Null references concatenate as the text null, and no path normalization or file IO occurs.
         * @m3.dataStructures One String input, two generated String values and a fresh two-element String array.
         */
        public String[] filePaths(String packagePath) {
            return new String[] {
                packagePath + "/" + interfaceName + ".java", packagePath + "/" + abstractClassName + ".java"
            };
        }
    }

    // --- THE THREE-LAYER HIERARCHY (FULL) ---

    /**
     * IOP Three-Layer Hierarchy (Full).
     *
     * <p>
     * Every generated type MUST produce AT MINIMUM:
     *
     * <pre>
     * 1. I{Name}.java        - Interface (contract, LOCKED)
     * 2. Abstract{Name}.java - Abstract class (common logic, LOCKED)
     * 3. {Name}Impl.java     - Implementation (LLM can enhance) [OPTIONAL]
     * </pre>
     *
     * <p>
     * Layers 1 and 2 are MANDATORY. Layer 3 is generated when needed. Even if
     * layers are empty, layers 1 and 2 MUST exist.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares a record with the supplied component values retained without a custom canonical constructor or validation. Compiler-generated accessors and record equality/hash/string methods retain ordinary record semantics.
     * @m3.dataStructures Record components: String interfaceName, String abstractClassName, String implName.
     * @m3.member {@code FIELD:interfaceName} pattern=synexia:iop; algorithm=Stores the canonical-constructor component value unchanged. No explicit initializer, copying, validation or normalization is declared.; data.structures=One record component value of type String.; description=interfaceName component of the ThreeLayerHierarchy record.
     * @m3.member {@code FIELD:abstractClassName} pattern=synexia:iop; algorithm=Stores the canonical-constructor component value unchanged. No explicit initializer, copying, validation or normalization is declared.; data.structures=One record component value of type String.; description=abstractClassName component of the ThreeLayerHierarchy record.
     * @m3.member {@code FIELD:implName} pattern=synexia:iop; algorithm=Stores the canonical-constructor component value unchanged. No explicit initializer, copying, validation or normalization is declared.; data.structures=One record component value of type String.; description=implName component of the ThreeLayerHierarchy record.
     */
    public record ThreeLayerHierarchy(
            /* Interface name (e.g., IUserService) */
            String interfaceName,
            /* Abstract class name (e.g., AbstractUserService) */
            String abstractClassName,
            /* Implementation name (e.g., UserServiceImpl) */
            String implName) {

        /**
         * Creates three-layer hierarchy from base name.
         *
         * @param baseName
         *            base type name (e.g., "UserService")
         * @return three-layer hierarchy
         * @m3.pattern synexia:bop
         * @m3.algorithm Returns a fresh ThreeLayerHierarchy record using String concatenation for I plus baseName, Abstract plus baseName and baseName plus Impl. Null baseName is converted to the literal text null, and no identifier or name validation occurs.
         * @m3.dataStructures One String input, generated String references and one new record instance.
         */
        public static ThreeLayerHierarchy fromBaseName(String baseName) {
            return new ThreeLayerHierarchy("I" + baseName, "Abstract" + baseName, baseName + "Impl");
        }

        /**
         * Validates that all three layers exist.
         *
         * @param existingClasses
         *            set of existing class names
         * @return true if all three layers exist
         * @m3.pattern synexia:bop
         * @m3.algorithm Calls existingClasses.contains in source order for interfaceName, abstractClassName and implName, returning false at the first missing name and true only if every check succeeds. A null set fails on the first call, and caller-set effects or exceptions propagate.
         * @m3.dataStructures One caller-owned Set of String names, stored component references and boolean membership results. The set is not copied or modified by this method.
         */
        public boolean validate(java.util.Set<String> existingClasses) {
            return existingClasses.contains(interfaceName)
                    && existingClasses.contains(abstractClassName)
                    && existingClasses.contains(implName);
        }
    }

    // --- CANONICAL SENTENCE ---

    /** Canonical IOP sentence (locked).
     * @m3.pattern synexia:iop
     * @m3.algorithm Initializes one static final String from its exact source literal, retaining all text, punctuation and text-block newline or indentation semantics. The stored text does not enforce its stated law.
     * @m3.dataStructures One immutable String reference with the exact literal payload.
     */
    public static final String CANONICAL_SENTENCE = """
			Behavior requires intent.
			Intent requires abstraction.
			Abstraction is mandatory in IOP.
			""";
}
