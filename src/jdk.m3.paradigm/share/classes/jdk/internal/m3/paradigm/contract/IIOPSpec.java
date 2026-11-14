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
 * IIOPSpec - Union of Specification Types.
 *
 * <h2>Specification Hierarchy</h2>
 *
 * <pre>
 * IIOPSpec
 * ├── ClassSpec        - Interface/class specification (@AIGenClass)
 * ├── MethodSpec       - Method specification (@AIGenMethod)
 * ├── FieldSpec        - Field specification
 * ├── ApiSpec          - REST API specification
 * ├── DataSpec         - Data/entity specification
 * ├── EventSpec        - Event specification
 * ├── FlowSpec         - Flow/workflow specification
 * ├── AgentSpec        - AI agent specification
 * ├── ToolSpec         - Tool function specification
 * ├── PromptSpec       - Prompt template specification
 * └── TestSpec         - Test specification
 * </pre>
 *
 * @since 1.0 - AI IOP
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares the sealed IIOPSpec metadata variants and the local signatures/defaults category, targetElement, value, constraints, invariants. The defaults project metadata or inspect the current variant and do not execute the described application operation.
 * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IIOPSpec.java}
 * blob {@code a01749c784ad060cb0de4584358a0be6b09305b7} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public sealed interface IIOPSpec extends IIOPConcept
        permits IIOPSpec.ClassSpec,
                IIOPSpec.MethodSpec,
                IIOPSpec.FieldSpec,
                IIOPSpec.ApiSpec,
                IIOPSpec.DataSpec,
                IIOPSpec.EventSpec,
                IIOPSpec.FlowSpec,
                IIOPSpec.AgentSpec,
                IIOPSpec.ToolSpec,
                IIOPSpec.PromptSpec,
                IIOPSpec.TestSpec {

    /**
     * category metadata operation of IIOPSpec.
     * @m3.pattern synexia:iop
     * @m3.algorithm Returns the existing enum singleton <code>Category.SPECIFICATION</code> without runtime enforcement.
     * @m3.dataStructures Declared Category result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
     */
    @Override
    default Category category() {
        return Category.SPECIFICATION;
    }

    /**
     * Get the target element type for this spec.
     *
     * @return target element type
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract targetElement interface signature declares a TargetElement result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-2026
     * @m3.dataStructures Abstract interface signature returning TargetElement. Implementations supply the result and its ownership behavior.
     */
    TargetElement targetElement();

    /**
     * Get the value/description of this spec.
     *
     * @return spec value
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract value interface signature declares a String result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-2158
     * @m3.dataStructures Abstract interface signature returning String. Implementations supply the result and its ownership behavior.
     */
    String value();

    /**
     * Get constraints for this spec.
     *
     * @return constraints array
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract constraints interface signature declares a String[] result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-2273
     * @m3.dataStructures Abstract interface signature returning String[]. Implementations supply the result and its ownership behavior.
     */
    String[] constraints();

    /**
     * Get invariants for this spec.
     *
     * @return invariants array
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract invariants interface signature declares a String[] result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-2394
     * @m3.dataStructures Abstract interface signature returning String[]. Implementations supply the result and its ownership behavior.
     */
    String[] invariants();

    /** Target element types.
     * @m3.pattern synexia:iop
     * @m3.algorithm Defines the existing 6 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
     * @m3.dataStructures Enum values in source order: TYPE, METHOD, FIELD, PARAMETER, PACKAGE, MODULE.
     * @m3.member {@code FIELD:TYPE} pattern=synexia:iop; algorithm=Initializes TYPE from the existing expression <code>new TargetElement()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One TargetElement field with its declared initializer.; description=Stored TYPE metadata in TargetElement.
     * @m3.member {@code FIELD:METHOD} pattern=synexia:iop; algorithm=Initializes METHOD from the existing expression <code>new TargetElement()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One TargetElement field with its declared initializer.; description=Stored METHOD metadata in TargetElement.
     * @m3.member {@code FIELD:FIELD} pattern=synexia:iop; algorithm=Initializes FIELD from the existing expression <code>new TargetElement()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One TargetElement field with its declared initializer.; description=Stored FIELD metadata in TargetElement.
     * @m3.member {@code FIELD:PARAMETER} pattern=synexia:iop; algorithm=Initializes PARAMETER from the existing expression <code>new TargetElement()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One TargetElement field with its declared initializer.; description=Stored PARAMETER metadata in TargetElement.
     * @m3.member {@code FIELD:PACKAGE} pattern=synexia:iop; algorithm=Initializes PACKAGE from the existing expression <code>new TargetElement()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One TargetElement field with its declared initializer.; description=Stored PACKAGE metadata in TargetElement.
     * @m3.member {@code FIELD:MODULE} pattern=synexia:iop; algorithm=Initializes MODULE from the existing expression <code>new TargetElement()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One TargetElement field with its declared initializer.; description=Stored MODULE metadata in TargetElement.
     */
    enum TargetElement {
        TYPE,
        METHOD,
        FIELD,
        PARAMETER,
        PACKAGE,
        MODULE
    }

    // --- SPEC RECORDS (Product Types) ---

    /** Class/interface specification.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied ClassSpec metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String name, String description, String value, String[] constraints, String[] invariants, String[] examples, ModelType model, StabilityLevel stability, Class (unbounded type argument) testClass, String targetPackage, String classSuffix. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:name} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The name declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-2700; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored name metadata in ClassSpec.
     * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-2725; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in ClassSpec.
     * @m3.member {@code FIELD:value} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The value declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-2757; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored value metadata in ClassSpec.
     * @m3.member {@code FIELD:constraints} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The constraints declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-2783; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored constraints metadata in ClassSpec.
     * @m3.member {@code FIELD:invariants} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The invariants declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-2817; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored invariants metadata in ClassSpec.
     * @m3.member {@code FIELD:examples} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The examples declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-2850; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored examples metadata in ClassSpec.
     * @m3.member {@code FIELD:model} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The model declaration supplies a stored ModelType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-2881; data.structures=One stored ModelType component or field. Reference values are retained and exposed without copying, including null.; description=Stored model metadata in ClassSpec.
     * @m3.member {@code FIELD:stability} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The stability declaration supplies a stored StabilityLevel metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-2910; data.structures=One stored StabilityLevel component or field. Reference values are retained and exposed without copying, including null.; description=Stored stability metadata in ClassSpec.
     * @m3.member {@code FIELD:testClass} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The testClass declaration supplies a stored Class reference with an unbounded type argument metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-2948; data.structures=One stored Class reference with an unbounded type argument component or field. Reference values are retained and exposed without copying, including null.; description=Stored testClass metadata in ClassSpec.
     * @m3.member {@code FIELD:targetPackage} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The targetPackage declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-2980; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored targetPackage metadata in ClassSpec.
     * @m3.member {@code FIELD:classSuffix} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The classSuffix declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-3014; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored classSuffix metadata in ClassSpec.
     */
    record ClassSpec(
            String name,
            String description,
            String value,
            String[] constraints,
            String[] invariants,
            String[] examples,
            ModelType model,
            StabilityLevel stability,
            Class<?> testClass,
            String targetPackage,
            String classSuffix)
            implements IIOPSpec {

        /**
         * targetElement metadata operation of ClassSpec.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>TargetElement.TYPE</code> without runtime enforcement.
         * @m3.dataStructures Declared TargetElement result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public TargetElement targetElement() {
            return TargetElement.TYPE;
        }

        /**
         * of metadata operation of ClassSpec.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates and returns the value from <code>new ClassSpec(name, "Class specification for " + name, value, new String[0], new String[0], new String[0], ModelType.SONNET, StabilityLevel.ALPHA, Void.class, "", "Impl")</code>, evaluating constructor arguments in source order. Supplied references are retained directly and construction adds no validation.
         * @m3.dataStructures Declared ClassSpec result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static ClassSpec of(String name, String value) {
            return new ClassSpec(
                    name,
                    "Class specification for " + name,
                    value,
                    new String[0],
                    new String[0],
                    new String[0],
                    ModelType.SONNET,
                    StabilityLevel.ALPHA,
                    Void.class,
                    "",
                    "Impl");
        }
    }

    /** Method specification.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied MethodSpec metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String name, String description, String value, String[] constraints, String[] invariants, String[] preconditions, String[] postconditions, String[] examples, ModelType model, String testMethod, int maxComplexity. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:name} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The name declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-3745; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored name metadata in MethodSpec.
     * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-3770; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in MethodSpec.
     * @m3.member {@code FIELD:value} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The value declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-3802; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored value metadata in MethodSpec.
     * @m3.member {@code FIELD:constraints} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The constraints declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-3828; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored constraints metadata in MethodSpec.
     * @m3.member {@code FIELD:invariants} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The invariants declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-3862; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored invariants metadata in MethodSpec.
     * @m3.member {@code FIELD:preconditions} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The preconditions declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-3895; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored preconditions metadata in MethodSpec.
     * @m3.member {@code FIELD:postconditions} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The postconditions declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-3931; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored postconditions metadata in MethodSpec.
     * @m3.member {@code FIELD:examples} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The examples declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-3968; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored examples metadata in MethodSpec.
     * @m3.member {@code FIELD:model} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The model declaration supplies a stored ModelType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-3999; data.structures=One stored ModelType component or field. Reference values are retained and exposed without copying, including null.; description=Stored model metadata in MethodSpec.
     * @m3.member {@code FIELD:testMethod} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The testMethod declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-4028; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored testMethod metadata in MethodSpec.
     * @m3.member {@code FIELD:maxComplexity} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The maxComplexity declaration supplies a stored int metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-4059; data.structures=One stored int primitive component or field, accepted without numeric or policy validation.; description=Stored maxComplexity metadata in MethodSpec.
     */
    record MethodSpec(
            String name,
            String description,
            String value,
            String[] constraints,
            String[] invariants,
            String[] preconditions,
            String[] postconditions,
            String[] examples,
            ModelType model,
            String testMethod,
            int maxComplexity)
            implements IIOPSpec {

        /**
         * targetElement metadata operation of MethodSpec.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>TargetElement.METHOD</code> without runtime enforcement.
         * @m3.dataStructures Declared TargetElement result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public TargetElement targetElement() {
            return TargetElement.METHOD;
        }

        /**
         * of metadata operation of MethodSpec.
         * @m3.pattern synexia:iop
         * @m3.algorithm Creates and returns the value from <code>new MethodSpec(name, "Method specification for " + name, value, new String[0], new String[0], new String[0], new String[0], new String[0], ModelType.SONNET, "", 10)</code>, evaluating constructor arguments in source order. Supplied references are retained directly and construction adds no validation.
         * @m3.dataStructures Declared MethodSpec result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        public static MethodSpec of(String name, String value) {
            return new MethodSpec(
                    name,
                    "Method specification for " + name,
                    value,
                    new String[0],
                    new String[0],
                    new String[0],
                    new String[0],
                    new String[0],
                    ModelType.SONNET,
                    "",
                    10);
        }
    }

    /** Field specification.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied FieldSpec metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String name, String description, String value, String[] constraints, String[] invariants, String defaultValue, boolean required, boolean immutable. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:name} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The name declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-4784; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored name metadata in FieldSpec.
     * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-4809; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in FieldSpec.
     * @m3.member {@code FIELD:value} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The value declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-4841; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored value metadata in FieldSpec.
     * @m3.member {@code FIELD:constraints} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The constraints declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-4867; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored constraints metadata in FieldSpec.
     * @m3.member {@code FIELD:invariants} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The invariants declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-4901; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored invariants metadata in FieldSpec.
     * @m3.member {@code FIELD:defaultValue} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The defaultValue declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-4934; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored defaultValue metadata in FieldSpec.
     * @m3.member {@code FIELD:required} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The required declaration supplies a stored boolean metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-4967; data.structures=One stored boolean primitive component or field, accepted without numeric or policy validation.; description=Stored required metadata in FieldSpec.
     * @m3.member {@code FIELD:immutable} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The immutable declaration supplies a stored boolean metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-4997; data.structures=One stored boolean primitive component or field, accepted without numeric or policy validation.; description=Stored immutable metadata in FieldSpec.
     */
    record FieldSpec(
            String name,
            String description,
            String value,
            String[] constraints,
            String[] invariants,
            String defaultValue,
            boolean required,
            boolean immutable)
            implements IIOPSpec {

        /**
         * targetElement metadata operation of FieldSpec.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>TargetElement.FIELD</code> without runtime enforcement.
         * @m3.dataStructures Declared TargetElement result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public TargetElement targetElement() {
            return TargetElement.FIELD;
        }
    }

    /** REST API specification.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied ApiSpec metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String name, String description, String value, String[] constraints, String[] invariants, String path, HttpMethod method, String[] consumes, String[] produces. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:name} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The name declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5240; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored name metadata in ApiSpec.
     * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5265; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in ApiSpec.
     * @m3.member {@code FIELD:value} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The value declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5297; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored value metadata in ApiSpec.
     * @m3.member {@code FIELD:constraints} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The constraints declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5323; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored constraints metadata in ApiSpec.
     * @m3.member {@code FIELD:invariants} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The invariants declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5357; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored invariants metadata in ApiSpec.
     * @m3.member {@code FIELD:path} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The path declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5390; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored path metadata in ApiSpec.
     * @m3.member {@code FIELD:method} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The method declaration supplies a stored HttpMethod metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5415; data.structures=One stored HttpMethod component or field. Reference values are retained and exposed without copying, including null.; description=Stored method metadata in ApiSpec.
     * @m3.member {@code FIELD:consumes} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The consumes declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5446; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored consumes metadata in ApiSpec.
     * @m3.member {@code FIELD:produces} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The produces declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5477; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored produces metadata in ApiSpec.
     */
    record ApiSpec(
            String name,
            String description,
            String value,
            String[] constraints,
            String[] invariants,
            String path,
            HttpMethod method,
            String[] consumes,
            String[] produces)
            implements IIOPSpec {

        /**
         * targetElement metadata operation of ApiSpec.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>TargetElement.METHOD</code> without runtime enforcement.
         * @m3.dataStructures Declared TargetElement result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public TargetElement targetElement() {
            return TargetElement.METHOD;
        }
    }

    /** Data/entity specification.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied DataSpec metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String name, String description, String value, String[] constraints, String[] invariants, String tableName, String[] indexes, boolean auditable. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:name} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The name declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5725; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored name metadata in DataSpec.
     * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5750; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in DataSpec.
     * @m3.member {@code FIELD:value} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The value declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5782; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored value metadata in DataSpec.
     * @m3.member {@code FIELD:constraints} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The constraints declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5808; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored constraints metadata in DataSpec.
     * @m3.member {@code FIELD:invariants} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The invariants declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5842; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored invariants metadata in DataSpec.
     * @m3.member {@code FIELD:tableName} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The tableName declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5875; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored tableName metadata in DataSpec.
     * @m3.member {@code FIELD:indexes} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The indexes declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5905; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored indexes metadata in DataSpec.
     * @m3.member {@code FIELD:auditable} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The auditable declaration supplies a stored boolean metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-5935; data.structures=One stored boolean primitive component or field, accepted without numeric or policy validation.; description=Stored auditable metadata in DataSpec.
     */
    record DataSpec(
            String name,
            String description,
            String value,
            String[] constraints,
            String[] invariants,
            String tableName,
            String[] indexes,
            boolean auditable)
            implements IIOPSpec {

        /**
         * targetElement metadata operation of DataSpec.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>TargetElement.TYPE</code> without runtime enforcement.
         * @m3.dataStructures Declared TargetElement result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public TargetElement targetElement() {
            return TargetElement.TYPE;
        }
    }

    /** Event specification.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied EventSpec metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String name, String description, String value, String[] constraints, String[] invariants, String topic, EventType eventType, boolean persistent. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:name} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The name declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6176; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored name metadata in EventSpec.
     * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6201; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in EventSpec.
     * @m3.member {@code FIELD:value} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The value declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6233; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored value metadata in EventSpec.
     * @m3.member {@code FIELD:constraints} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The constraints declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6259; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored constraints metadata in EventSpec.
     * @m3.member {@code FIELD:invariants} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The invariants declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6293; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored invariants metadata in EventSpec.
     * @m3.member {@code FIELD:topic} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The topic declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6326; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored topic metadata in EventSpec.
     * @m3.member {@code FIELD:eventType} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The eventType declaration supplies a stored EventType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6352; data.structures=One stored EventType component or field. Reference values are retained and exposed without copying, including null.; description=Stored eventType metadata in EventSpec.
     * @m3.member {@code FIELD:persistent} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The persistent declaration supplies a stored boolean metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6385; data.structures=One stored boolean primitive component or field, accepted without numeric or policy validation.; description=Stored persistent metadata in EventSpec.
     */
    record EventSpec(
            String name,
            String description,
            String value,
            String[] constraints,
            String[] invariants,
            String topic,
            EventType eventType,
            boolean persistent)
            implements IIOPSpec {

        /**
         * targetElement metadata operation of EventSpec.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>TargetElement.TYPE</code> without runtime enforcement.
         * @m3.dataStructures Declared TargetElement result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public TargetElement targetElement() {
            return TargetElement.TYPE;
        }
    }

    /** Flow/workflow specification.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied FlowSpec metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String name, String description, String value, String[] constraints, String[] invariants, String[] steps, boolean parallel, boolean compensatable. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:name} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The name declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6634; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored name metadata in FlowSpec.
     * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6659; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in FlowSpec.
     * @m3.member {@code FIELD:value} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The value declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6691; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored value metadata in FlowSpec.
     * @m3.member {@code FIELD:constraints} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The constraints declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6717; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored constraints metadata in FlowSpec.
     * @m3.member {@code FIELD:invariants} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The invariants declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6751; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored invariants metadata in FlowSpec.
     * @m3.member {@code FIELD:steps} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The steps declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6784; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored steps metadata in FlowSpec.
     * @m3.member {@code FIELD:parallel} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The parallel declaration supplies a stored boolean metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6812; data.structures=One stored boolean primitive component or field, accepted without numeric or policy validation.; description=Stored parallel metadata in FlowSpec.
     * @m3.member {@code FIELD:compensatable} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The compensatable declaration supplies a stored boolean metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-6842; data.structures=One stored boolean primitive component or field, accepted without numeric or policy validation.; description=Stored compensatable metadata in FlowSpec.
     */
    record FlowSpec(
            String name,
            String description,
            String value,
            String[] constraints,
            String[] invariants,
            String[] steps,
            boolean parallel,
            boolean compensatable)
            implements IIOPSpec {

        /**
         * targetElement metadata operation of FlowSpec.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>TargetElement.TYPE</code> without runtime enforcement.
         * @m3.dataStructures Declared TargetElement result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public TargetElement targetElement() {
            return TargetElement.TYPE;
        }
    }

    /** AI agent specification.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied AgentSpec metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String name, String description, String value, String[] constraints, String[] invariants, String[] tools, String systemPrompt, ModelType model, int maxIterations. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:name} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The name declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7090; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored name metadata in AgentSpec.
     * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7115; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in AgentSpec.
     * @m3.member {@code FIELD:value} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The value declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7147; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored value metadata in AgentSpec.
     * @m3.member {@code FIELD:constraints} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The constraints declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7173; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored constraints metadata in AgentSpec.
     * @m3.member {@code FIELD:invariants} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The invariants declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7207; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored invariants metadata in AgentSpec.
     * @m3.member {@code FIELD:tools} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The tools declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7240; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored tools metadata in AgentSpec.
     * @m3.member {@code FIELD:systemPrompt} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The systemPrompt declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7268; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored systemPrompt metadata in AgentSpec.
     * @m3.member {@code FIELD:model} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The model declaration supplies a stored ModelType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7301; data.structures=One stored ModelType component or field. Reference values are retained and exposed without copying, including null.; description=Stored model metadata in AgentSpec.
     * @m3.member {@code FIELD:maxIterations} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The maxIterations declaration supplies a stored int metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7330; data.structures=One stored int primitive component or field, accepted without numeric or policy validation.; description=Stored maxIterations metadata in AgentSpec.
     */
    record AgentSpec(
            String name,
            String description,
            String value,
            String[] constraints,
            String[] invariants,
            String[] tools,
            String systemPrompt,
            ModelType model,
            int maxIterations)
            implements IIOPSpec {

        /**
         * targetElement metadata operation of AgentSpec.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>TargetElement.TYPE</code> without runtime enforcement.
         * @m3.dataStructures Declared TargetElement result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public TargetElement targetElement() {
            return TargetElement.TYPE;
        }
    }

    /** Tool function specification.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied ToolSpec metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String name, String description, String value, String[] constraints, String[] invariants, String[] parameters, String returnType, boolean async. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:name} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The name declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7578; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored name metadata in ToolSpec.
     * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7603; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in ToolSpec.
     * @m3.member {@code FIELD:value} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The value declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7635; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored value metadata in ToolSpec.
     * @m3.member {@code FIELD:constraints} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The constraints declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7661; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored constraints metadata in ToolSpec.
     * @m3.member {@code FIELD:invariants} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The invariants declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7695; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored invariants metadata in ToolSpec.
     * @m3.member {@code FIELD:parameters} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The parameters declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7728; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored parameters metadata in ToolSpec.
     * @m3.member {@code FIELD:returnType} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The returnType declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7761; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored returnType metadata in ToolSpec.
     * @m3.member {@code FIELD:async} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The async declaration supplies a stored boolean metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-7792; data.structures=One stored boolean primitive component or field, accepted without numeric or policy validation.; description=Stored async metadata in ToolSpec.
     */
    record ToolSpec(
            String name,
            String description,
            String value,
            String[] constraints,
            String[] invariants,
            String[] parameters,
            String returnType,
            boolean async)
            implements IIOPSpec {

        /**
         * targetElement metadata operation of ToolSpec.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>TargetElement.METHOD</code> without runtime enforcement.
         * @m3.dataStructures Declared TargetElement result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public TargetElement targetElement() {
            return TargetElement.METHOD;
        }
    }

    /** Prompt template specification.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied PromptSpec metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String name, String description, String value, String[] constraints, String[] invariants, String template, String[] variables, ModelType targetModel. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:name} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The name declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8042; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored name metadata in PromptSpec.
     * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8067; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in PromptSpec.
     * @m3.member {@code FIELD:value} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The value declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8099; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored value metadata in PromptSpec.
     * @m3.member {@code FIELD:constraints} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The constraints declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8125; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored constraints metadata in PromptSpec.
     * @m3.member {@code FIELD:invariants} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The invariants declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8159; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored invariants metadata in PromptSpec.
     * @m3.member {@code FIELD:template} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The template declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8192; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored template metadata in PromptSpec.
     * @m3.member {@code FIELD:variables} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The variables declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8221; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored variables metadata in PromptSpec.
     * @m3.member {@code FIELD:targetModel} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The targetModel declaration supplies a stored ModelType metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8253; data.structures=One stored ModelType component or field. Reference values are retained and exposed without copying, including null.; description=Stored targetModel metadata in PromptSpec.
     */
    record PromptSpec(
            String name,
            String description,
            String value,
            String[] constraints,
            String[] invariants,
            String template,
            String[] variables,
            ModelType targetModel)
            implements IIOPSpec {

        /**
         * targetElement metadata operation of PromptSpec.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>TargetElement.TYPE</code> without runtime enforcement.
         * @m3.dataStructures Declared TargetElement result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public TargetElement targetElement() {
            return TargetElement.TYPE;
        }
    }

    /** Test specification.
     * @m3.pattern synexia:iop
     * @m3.algorithm Carries the supplied TestSpec metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
     * @m3.dataStructures Record components: String name, String description, String value, String[] constraints, String[] invariants, Class (unbounded type argument) targetClass, String[] testCases, boolean generateMocks. Arrays remain caller-owned aliases and generated record equality uses array identity.
     * @m3.member {@code FIELD:name} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The name declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8496; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored name metadata in TestSpec.
     * @m3.member {@code FIELD:description} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The description declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8521; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored description metadata in TestSpec.
     * @m3.member {@code FIELD:value} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The value declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8553; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored value metadata in TestSpec.
     * @m3.member {@code FIELD:constraints} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The constraints declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8579; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored constraints metadata in TestSpec.
     * @m3.member {@code FIELD:invariants} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The invariants declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8613; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored invariants metadata in TestSpec.
     * @m3.member {@code FIELD:targetClass} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The targetClass declaration supplies a stored Class reference with an unbounded type argument metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8646; data.structures=One stored Class reference with an unbounded type argument component or field. Reference values are retained and exposed without copying, including null.; description=Stored targetClass metadata in TestSpec.
     * @m3.member {@code FIELD:testCases} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The testCases declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8680; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored testCases metadata in TestSpec.
     * @m3.member {@code FIELD:generateMocks} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The generateMocks declaration supplies a stored boolean metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPSpec-8712; data.structures=One stored boolean primitive component or field, accepted without numeric or policy validation.; description=Stored generateMocks metadata in TestSpec.
     */
    record TestSpec(
            String name,
            String description,
            String value,
            String[] constraints,
            String[] invariants,
            Class<?> targetClass,
            String[] testCases,
            boolean generateMocks)
            implements IIOPSpec {

        /**
         * targetElement metadata operation of TestSpec.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>TargetElement.TYPE</code> without runtime enforcement.
         * @m3.dataStructures Declared TargetElement result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        public TargetElement targetElement() {
            return TargetElement.TYPE;
        }
    }

    // --- SUPPORTING ENUMS ---

    /** HTTP methods for API specs.
     * @m3.pattern synexia:iop
     * @m3.algorithm Defines the existing 7 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
     * @m3.dataStructures Enum values in source order: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS.
     * @m3.member {@code FIELD:GET} pattern=synexia:iop; algorithm=Initializes GET from the existing expression <code>new HttpMethod()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One HttpMethod field with its declared initializer.; description=Stored GET metadata in HttpMethod.
     * @m3.member {@code FIELD:POST} pattern=synexia:iop; algorithm=Initializes POST from the existing expression <code>new HttpMethod()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One HttpMethod field with its declared initializer.; description=Stored POST metadata in HttpMethod.
     * @m3.member {@code FIELD:PUT} pattern=synexia:iop; algorithm=Initializes PUT from the existing expression <code>new HttpMethod()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One HttpMethod field with its declared initializer.; description=Stored PUT metadata in HttpMethod.
     * @m3.member {@code FIELD:DELETE} pattern=synexia:iop; algorithm=Initializes DELETE from the existing expression <code>new HttpMethod()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One HttpMethod field with its declared initializer.; description=Stored DELETE metadata in HttpMethod.
     * @m3.member {@code FIELD:PATCH} pattern=synexia:iop; algorithm=Initializes PATCH from the existing expression <code>new HttpMethod()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One HttpMethod field with its declared initializer.; description=Stored PATCH metadata in HttpMethod.
     * @m3.member {@code FIELD:HEAD} pattern=synexia:iop; algorithm=Initializes HEAD from the existing expression <code>new HttpMethod()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One HttpMethod field with its declared initializer.; description=Stored HEAD metadata in HttpMethod.
     * @m3.member {@code FIELD:OPTIONS} pattern=synexia:iop; algorithm=Initializes OPTIONS from the existing expression <code>new HttpMethod()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One HttpMethod field with its declared initializer.; description=Stored OPTIONS metadata in HttpMethod.
     */
    enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE,
        PATCH,
        HEAD,
        OPTIONS
    }

    /** Event types.
     * @m3.pattern synexia:iop
     * @m3.algorithm Defines the existing 5 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
     * @m3.dataStructures Enum values in source order: DOMAIN, INTEGRATION, COMMAND, QUERY, NOTIFICATION.
     * @m3.member {@code FIELD:DOMAIN} pattern=synexia:iop; algorithm=Initializes DOMAIN from the existing expression <code>new EventType()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EventType field with its declared initializer.; description=Stored DOMAIN metadata in EventType.
     * @m3.member {@code FIELD:INTEGRATION} pattern=synexia:iop; algorithm=Initializes INTEGRATION from the existing expression <code>new EventType()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EventType field with its declared initializer.; description=Stored INTEGRATION metadata in EventType.
     * @m3.member {@code FIELD:COMMAND} pattern=synexia:iop; algorithm=Initializes COMMAND from the existing expression <code>new EventType()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EventType field with its declared initializer.; description=Stored COMMAND metadata in EventType.
     * @m3.member {@code FIELD:QUERY} pattern=synexia:iop; algorithm=Initializes QUERY from the existing expression <code>new EventType()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EventType field with its declared initializer.; description=Stored QUERY metadata in EventType.
     * @m3.member {@code FIELD:NOTIFICATION} pattern=synexia:iop; algorithm=Initializes NOTIFICATION from the existing expression <code>new EventType()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One EventType field with its declared initializer.; description=Stored NOTIFICATION metadata in EventType.
     */
    enum EventType {
        DOMAIN,
        INTEGRATION,
        COMMAND,
        QUERY,
        NOTIFICATION
    }

    /** Model types for generation.
     * @m3.pattern synexia:iop
     * @m3.algorithm Defines the existing 6 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
     * @m3.dataStructures Enum values in source order: OPUS, SONNET, HAIKU, OLLAMA, OPENNLP, DEFAULT.
     * @m3.member {@code FIELD:OPUS} pattern=synexia:iop; algorithm=Initializes OPUS from the existing expression <code>new ModelType()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ModelType field with its declared initializer.; description=Stored OPUS metadata in ModelType.
     * @m3.member {@code FIELD:SONNET} pattern=synexia:iop; algorithm=Initializes SONNET from the existing expression <code>new ModelType()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ModelType field with its declared initializer.; description=Stored SONNET metadata in ModelType.
     * @m3.member {@code FIELD:HAIKU} pattern=synexia:iop; algorithm=Initializes HAIKU from the existing expression <code>new ModelType()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ModelType field with its declared initializer.; description=Stored HAIKU metadata in ModelType.
     * @m3.member {@code FIELD:OLLAMA} pattern=synexia:iop; algorithm=Initializes OLLAMA from the existing expression <code>new ModelType()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ModelType field with its declared initializer.; description=Stored OLLAMA metadata in ModelType.
     * @m3.member {@code FIELD:OPENNLP} pattern=synexia:iop; algorithm=Initializes OPENNLP from the existing expression <code>new ModelType()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ModelType field with its declared initializer.; description=Stored OPENNLP metadata in ModelType.
     * @m3.member {@code FIELD:DEFAULT} pattern=synexia:iop; algorithm=Initializes DEFAULT from the existing expression <code>new ModelType()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One ModelType field with its declared initializer.; description=Stored DEFAULT metadata in ModelType.
     */
    enum ModelType {
        OPUS,
        SONNET,
        HAIKU,
        OLLAMA,
        OPENNLP,
        DEFAULT
    }

    /** Stability levels.
     * @m3.pattern synexia:iop
     * @m3.algorithm Defines the existing 6 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
     * @m3.dataStructures Enum values in source order: EXPERIMENTAL, ALPHA, BETA, STABLE, FROZEN, DEPRECATED.
     * @m3.member {@code FIELD:EXPERIMENTAL} pattern=synexia:iop; algorithm=Initializes EXPERIMENTAL from the existing expression <code>new StabilityLevel()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One StabilityLevel field with its declared initializer.; description=Stored EXPERIMENTAL metadata in StabilityLevel.
     * @m3.member {@code FIELD:ALPHA} pattern=synexia:iop; algorithm=Initializes ALPHA from the existing expression <code>new StabilityLevel()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One StabilityLevel field with its declared initializer.; description=Stored ALPHA metadata in StabilityLevel.
     * @m3.member {@code FIELD:BETA} pattern=synexia:iop; algorithm=Initializes BETA from the existing expression <code>new StabilityLevel()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One StabilityLevel field with its declared initializer.; description=Stored BETA metadata in StabilityLevel.
     * @m3.member {@code FIELD:STABLE} pattern=synexia:iop; algorithm=Initializes STABLE from the existing expression <code>new StabilityLevel()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One StabilityLevel field with its declared initializer.; description=Stored STABLE metadata in StabilityLevel.
     * @m3.member {@code FIELD:FROZEN} pattern=synexia:iop; algorithm=Initializes FROZEN from the existing expression <code>new StabilityLevel()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One StabilityLevel field with its declared initializer.; description=Stored FROZEN metadata in StabilityLevel.
     * @m3.member {@code FIELD:DEPRECATED} pattern=synexia:iop; algorithm=Initializes DEPRECATED from the existing expression <code>new StabilityLevel()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One StabilityLevel field with its declared initializer.; description=Stored DEPRECATED metadata in StabilityLevel.
     */
    enum StabilityLevel {
        EXPERIMENTAL,
        ALPHA,
        BETA,
        STABLE,
        FROZEN,
        DEPRECATED
    }
}
