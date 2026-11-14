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
 * IIOPOperation - Union of Operation Types.
 *
 * <h2>Operation Hierarchy</h2>
 *
 * <pre>
 * IIOPOperation
 * ├── Generation       - GENERATE_IMPLEMENTATION, GENERATE_TESTS, GENERATE_JAVADOC
 * ├── Refinement       - REFINE_METHOD, OPTIMIZE_PERFORMANCE
 * ├── Structural       - INTRODUCE_TYPE, MOVE_TYPE, RENAME_SYMBOL, EXTRACT_METHOD, INLINE_METHOD
 * └── Cleanup          - REMOVE_DEAD_CODE, ORGANIZE_IMPORTS, FORMAT_CODE
 * </pre>
 *
 * <h2>IOP v1 Core Principle</h2> <blockquote> "No change may occur unless it is
 * the result of a declared, legal operation under explicit intent."
 * </blockquote>
 *
 * @since 1.0 - AI IOP
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares the sealed IIOPOperation metadata variants and the local signatures/defaults category, requiresLLM, isToolExecutable, modifiesCode. The defaults project metadata or inspect the current variant and do not execute the described application operation.
 * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IIOPOperation.java}
 * blob {@code 08083ae92f5b3fa715e3fec7b2b7b1fa8b33552b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public sealed interface IIOPOperation extends IIOPConcept
        permits IIOPOperation.Generation, IIOPOperation.Refinement, IIOPOperation.Structural, IIOPOperation.Cleanup {

    /**
     * category metadata operation of IIOPOperation.
     * @m3.pattern synexia:iop
     * @m3.algorithm Returns the existing enum singleton <code>Category.OPERATION</code> without runtime enforcement.
     * @m3.dataStructures Declared Category result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
     */
    @Override
    default Category category() {
        return Category.OPERATION;
    }

    /**
     * Check if this operation requires LLM.
     *
     * @return true if LLM is required
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract requiresLLM interface signature declares a boolean result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-1658
     * @m3.dataStructures Abstract interface signature returning boolean. Implementations supply the result and its ownership behavior.
     */
    boolean requiresLLM();

    /**
     * Check if this operation is tool-executable (IDE/AST).
     *
     * @return true if can be done by tools
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract isToolExecutable interface signature declares a boolean result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-1814
     * @m3.dataStructures Abstract interface signature returning boolean. Implementations supply the result and its ownership behavior.
     */
    boolean isToolExecutable();

    /**
     * Check if this operation modifies code.
     *
     * @return true if operation changes files
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract modifiesCode interface signature declares a boolean result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-1963
     * @m3.dataStructures Abstract interface signature returning boolean. Implementations supply the result and its ownership behavior.
     */
    boolean modifiesCode();

    // --- GENERATION OPERATIONS ---

    /** Generation operations - create new code.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the sealed Generation metadata variants and the local signatures/defaults requiresLLM, isToolExecutable, modifiesCode. The defaults project metadata or inspect the current variant and do not execute the described application operation.
     * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
     */
    sealed interface Generation extends IIOPOperation
            permits Generation.GenerateImpl, Generation.GenerateTests, Generation.GenerateJavadoc {

        /**
         * requiresLLM metadata operation of Generation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean requiresLLM() {
            return true;
        }

        /**
         * isToolExecutable metadata operation of Generation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>false</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isToolExecutable() {
            return false;
        }

        /**
         * modifiesCode metadata operation of Generation.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean modifiesCode() {
            return true;
        }

        /** Generate implementation from spec.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied GenerateImpl metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String targetInterface. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:targetInterface} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The targetInterface declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-2600; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored targetInterface metadata in GenerateImpl.
         */
        record GenerateImpl(String targetInterface) implements Generation {

            /**
             * name metadata operation of GenerateImpl.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"GENERATE_IMPLEMENTATION"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "GENERATE_IMPLEMENTATION";
            }

            /**
             * description metadata operation of GenerateImpl.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Generate implementation from interface + JavaDoc + constraints"</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Generate implementation from interface + JavaDoc + constraints";
            }
        }

        /** Generate tests from spec.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied GenerateTests metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String targetClass, String[] testCases. Arrays remain caller-owned aliases and generated record equality uses array identity.
         * @m3.member {@code FIELD:targetClass} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The targetClass declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-3019; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored targetClass metadata in GenerateTests.
         * @m3.member {@code FIELD:testCases} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The testCases declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-3039; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored testCases metadata in GenerateTests.
         */
        record GenerateTests(String targetClass, String[] testCases) implements Generation {

            /**
             * name metadata operation of GenerateTests.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"GENERATE_TESTS"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "GENERATE_TESTS";
            }

            /**
             * description metadata operation of GenerateTests.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"Generate unit tests from interface specification"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Generate unit tests from interface specification";
            }
        }

        /** Generate JavaDoc.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied GenerateJavadoc metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String targetElement. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:targetElement} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The targetElement declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-3425; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored targetElement metadata in GenerateJavadoc.
         */
        record GenerateJavadoc(String targetElement) implements Generation {

            /**
             * name metadata operation of GenerateJavadoc.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"GENERATE_JAVADOC"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "GENERATE_JAVADOC";
            }

            /**
             * description metadata operation of GenerateJavadoc.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"Generate or update JavaDoc documentation"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Generate or update JavaDoc documentation";
            }
        }
    }

    // --- REFINEMENT OPERATIONS ---

    /** Refinement operations - improve existing code.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the sealed Refinement metadata variants and the local signatures/defaults requiresLLM, isToolExecutable, modifiesCode. The defaults project metadata or inspect the current variant and do not execute the described application operation.
     * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
     */
    sealed interface Refinement extends IIOPOperation permits Refinement.RefineMethod, Refinement.OptimizePerformance {

        /**
         * requiresLLM metadata operation of Refinement.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean requiresLLM() {
            return true;
        }

        /**
         * isToolExecutable metadata operation of Refinement.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>false</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isToolExecutable() {
            return false;
        }

        /**
         * modifiesCode metadata operation of Refinement.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean modifiesCode() {
            return true;
        }

        /** Refine existing method (same contract).
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied RefineMethod metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String targetMethod, String[] improvements. Arrays remain caller-owned aliases and generated record equality uses array identity.
         * @m3.member {@code FIELD:targetMethod} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The targetMethod declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-4338; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored targetMethod metadata in RefineMethod.
         * @m3.member {@code FIELD:improvements} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The improvements declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-4359; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored improvements metadata in RefineMethod.
         */
        record RefineMethod(String targetMethod, String[] improvements) implements Refinement {

            /**
             * name metadata operation of RefineMethod.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"REFINE_METHOD"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "REFINE_METHOD";
            }

            /**
             * description metadata operation of RefineMethod.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"Refine existing method implementation (same contract)"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Refine existing method implementation (same contract)";
            }
        }

        /** Optimize for performance.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied OptimizePerformance metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String targetElement, String[] metrics. Arrays remain caller-owned aliases and generated record equality uses array identity.
         * @m3.member {@code FIELD:targetElement} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The targetElement declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-4764; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored targetElement metadata in OptimizePerformance.
         * @m3.member {@code FIELD:metrics} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The metrics declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-4786; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored metrics metadata in OptimizePerformance.
         */
        record OptimizePerformance(String targetElement, String[] metrics) implements Refinement {

            /**
             * name metadata operation of OptimizePerformance.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"OPTIMIZE_PERFORMANCE"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "OPTIMIZE_PERFORMANCE";
            }

            /**
             * description metadata operation of OptimizePerformance.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"Optimize performance without changing behavior"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Optimize performance without changing behavior";
            }
        }
    }

    // --- STRUCTURAL OPERATIONS ---

    /** Structural operations - change code structure.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the sealed Structural metadata variants and the local signatures/defaults requiresLLM, isToolExecutable, modifiesCode. The defaults project metadata or inspect the current variant and do not execute the described application operation.
     * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
     */
    sealed interface Structural extends IIOPOperation
            permits Structural.IntroduceType,
                    Structural.MoveType,
                    Structural.RenameSymbol,
                    Structural.ExtractMethod,
                    Structural.InlineMethod {

        /**
         * requiresLLM metadata operation of Structural.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>false</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean requiresLLM() {
            return false;
        }

        /**
         * isToolExecutable metadata operation of Structural.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isToolExecutable() {
            return true;
        }

        /**
         * modifiesCode metadata operation of Structural.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean modifiesCode() {
            return true;
        }

        /** Introduce new type.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied IntroduceType metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String typeName, String targetPackage, TypeKind kind. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:typeName} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The typeName declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-5844; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored typeName metadata in IntroduceType.
         * @m3.member {@code FIELD:targetPackage} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The targetPackage declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-5861; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored targetPackage metadata in IntroduceType.
         * @m3.member {@code FIELD:kind} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The kind declaration supplies a stored TypeKind metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-5883; data.structures=One stored TypeKind component or field. Reference values are retained and exposed without copying, including null.; description=Stored kind metadata in IntroduceType.
         */
        record IntroduceType(String typeName, String targetPackage, TypeKind kind) implements Structural {

            /**
             * name metadata operation of IntroduceType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"INTRODUCE_TYPE"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "INTRODUCE_TYPE";
            }

            /**
             * description metadata operation of IntroduceType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"Create new type (interface, class, record, enum)"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Create new type (interface, class, record, enum)";
            }
        }

        /** Move type to different package.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied MoveType metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String sourceType, String targetPackage. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:sourceType} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The sourceType declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-6271; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored sourceType metadata in MoveType.
         * @m3.member {@code FIELD:targetPackage} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The targetPackage declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-6290; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored targetPackage metadata in MoveType.
         */
        record MoveType(String sourceType, String targetPackage) implements Structural {

            /**
             * name metadata operation of MoveType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"MOVE_TYPE"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "MOVE_TYPE";
            }

            /**
             * description metadata operation of MoveType.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"Relocate type to different package"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Relocate type to different package";
            }
        }

        /** Rename symbol.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied RenameSymbol metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String oldName, String newName, SymbolKind kind. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:oldName} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The oldName declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-6653; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored oldName metadata in RenameSymbol.
         * @m3.member {@code FIELD:newName} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The newName declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-6669; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored newName metadata in RenameSymbol.
         * @m3.member {@code FIELD:kind} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The kind declaration supplies a stored SymbolKind metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-6685; data.structures=One stored SymbolKind component or field. Reference values are retained and exposed without copying, including null.; description=Stored kind metadata in RenameSymbol.
         */
        record RenameSymbol(String oldName, String newName, SymbolKind kind) implements Structural {

            /**
             * name metadata operation of RenameSymbol.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"RENAME_SYMBOL"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "RENAME_SYMBOL";
            }

            /**
             * description metadata operation of RenameSymbol.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"Rename symbol (class, method, field, parameter)"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Rename symbol (class, method, field, parameter)";
            }
        }

        /** Extract method.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied ExtractMethod metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String sourceMethod, int startLine, int endLine, String newMethodName. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:sourceMethod} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The sourceMethod declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-7062; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored sourceMethod metadata in ExtractMethod.
         * @m3.member {@code FIELD:startLine} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The startLine declaration supplies a stored int metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-7083; data.structures=One stored int primitive component or field, accepted without numeric or policy validation.; description=Stored startLine metadata in ExtractMethod.
         * @m3.member {@code FIELD:endLine} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The endLine declaration supplies a stored int metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-7098; data.structures=One stored int primitive component or field, accepted without numeric or policy validation.; description=Stored endLine metadata in ExtractMethod.
         * @m3.member {@code FIELD:newMethodName} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The newMethodName declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-7111; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored newMethodName metadata in ExtractMethod.
         */
        record ExtractMethod(String sourceMethod, int startLine, int endLine, String newMethodName)
                implements Structural {

            /**
             * name metadata operation of ExtractMethod.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"EXTRACT_METHOD"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "EXTRACT_METHOD";
            }

            /**
             * description metadata operation of ExtractMethod.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"Extract code block into new method"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Extract code block into new method";
            }
        }

        /** Inline method.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied InlineMethod metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String methodToInline. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:methodToInline} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The methodToInline declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-7495; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored methodToInline metadata in InlineMethod.
         */
        record InlineMethod(String methodToInline) implements Structural {

            /**
             * name metadata operation of InlineMethod.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"INLINE_METHOD"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "INLINE_METHOD";
            }

            /**
             * description metadata operation of InlineMethod.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"Inline method body at call sites"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Inline method body at call sites";
            }
        }

        /** Type kinds.
         * @m3.pattern synexia:iop
         * @m3.algorithm Defines the existing 5 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
         * @m3.dataStructures Enum values in source order: INTERFACE, CLASS, RECORD, ENUM, ANNOTATION.
         * @m3.member {@code FIELD:INTERFACE} pattern=synexia:iop; algorithm=Initializes INTERFACE from the existing expression <code>new TypeKind()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One TypeKind field with its declared initializer.; description=Stored INTERFACE metadata in TypeKind.
         * @m3.member {@code FIELD:CLASS} pattern=synexia:iop; algorithm=Initializes CLASS from the existing expression <code>new TypeKind()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One TypeKind field with its declared initializer.; description=Stored CLASS metadata in TypeKind.
         * @m3.member {@code FIELD:RECORD} pattern=synexia:iop; algorithm=Initializes RECORD from the existing expression <code>new TypeKind()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One TypeKind field with its declared initializer.; description=Stored RECORD metadata in TypeKind.
         * @m3.member {@code FIELD:ENUM} pattern=synexia:iop; algorithm=Initializes ENUM from the existing expression <code>new TypeKind()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One TypeKind field with its declared initializer.; description=Stored ENUM metadata in TypeKind.
         * @m3.member {@code FIELD:ANNOTATION} pattern=synexia:iop; algorithm=Initializes ANNOTATION from the existing expression <code>new TypeKind()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One TypeKind field with its declared initializer.; description=Stored ANNOTATION metadata in TypeKind.
         */
        enum TypeKind {
            INTERFACE,
            CLASS,
            RECORD,
            ENUM,
            ANNOTATION
        }

        /** Symbol kinds.
         * @m3.pattern synexia:iop
         * @m3.algorithm Defines the existing 5 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
         * @m3.dataStructures Enum values in source order: TYPE, METHOD, FIELD, PARAMETER, LOCAL_VARIABLE.
         * @m3.member {@code FIELD:TYPE} pattern=synexia:iop; algorithm=Initializes TYPE from the existing expression <code>new SymbolKind()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One SymbolKind field with its declared initializer.; description=Stored TYPE metadata in SymbolKind.
         * @m3.member {@code FIELD:METHOD} pattern=synexia:iop; algorithm=Initializes METHOD from the existing expression <code>new SymbolKind()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One SymbolKind field with its declared initializer.; description=Stored METHOD metadata in SymbolKind.
         * @m3.member {@code FIELD:FIELD} pattern=synexia:iop; algorithm=Initializes FIELD from the existing expression <code>new SymbolKind()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One SymbolKind field with its declared initializer.; description=Stored FIELD metadata in SymbolKind.
         * @m3.member {@code FIELD:PARAMETER} pattern=synexia:iop; algorithm=Initializes PARAMETER from the existing expression <code>new SymbolKind()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One SymbolKind field with its declared initializer.; description=Stored PARAMETER metadata in SymbolKind.
         * @m3.member {@code FIELD:LOCAL_VARIABLE} pattern=synexia:iop; algorithm=Initializes LOCAL_VARIABLE from the existing expression <code>new SymbolKind()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One SymbolKind field with its declared initializer.; description=Stored LOCAL_VARIABLE metadata in SymbolKind.
         */
        enum SymbolKind {
            TYPE,
            METHOD,
            FIELD,
            PARAMETER,
            LOCAL_VARIABLE
        }
    }

    // --- CLEANUP OPERATIONS ---

    /** Cleanup operations - clean code.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the sealed Cleanup metadata variants and the local signatures/defaults requiresLLM, isToolExecutable, modifiesCode. The defaults project metadata or inspect the current variant and do not execute the described application operation.
     * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
     */
    sealed interface Cleanup extends IIOPOperation
            permits Cleanup.RemoveDeadCode, Cleanup.OrganizeImports, Cleanup.FormatCode {

        /**
         * requiresLLM metadata operation of Cleanup.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>false</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean requiresLLM() {
            return false;
        }

        /**
         * isToolExecutable metadata operation of Cleanup.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isToolExecutable() {
            return true;
        }

        /**
         * modifiesCode metadata operation of Cleanup.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean modifiesCode() {
            return true;
        }

        /** Remove dead code.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied RemoveDeadCode metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String targetFile. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:targetFile} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The targetFile declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-8720; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored targetFile metadata in RemoveDeadCode.
         */
        record RemoveDeadCode(String targetFile) implements Cleanup {

            /**
             * name metadata operation of RemoveDeadCode.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"REMOVE_DEAD_CODE"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "REMOVE_DEAD_CODE";
            }

            /**
             * description metadata operation of RemoveDeadCode.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"Remove unreachable/unused code"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Remove unreachable/unused code";
            }
        }

        /** Organize imports.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied OrganizeImports metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String targetFile. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:targetFile} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The targetFile declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-9086; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored targetFile metadata in OrganizeImports.
         */
        record OrganizeImports(String targetFile) implements Cleanup {

            /**
             * name metadata operation of OrganizeImports.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"ORGANIZE_IMPORTS"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "ORGANIZE_IMPORTS";
            }

            /**
             * description metadata operation of OrganizeImports.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"Sort and clean imports"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Sort and clean imports";
            }
        }

        /** Format code.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied FormatCode metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String targetFile, String style. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:targetFile} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The targetFile declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-9434; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored targetFile metadata in FormatCode.
         * @m3.member {@code FIELD:style} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The style declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPOperation-9453; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored style metadata in FormatCode.
         */
        record FormatCode(String targetFile, String style) implements Cleanup {

            /**
             * name metadata operation of FormatCode.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"FORMAT_CODE"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "FORMAT_CODE";
            }

            /**
             * description metadata operation of FormatCode.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"Apply code style formatting"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Apply code style formatting";
            }
        }
    }
}
