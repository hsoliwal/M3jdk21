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
 * IIOPFailure - Union of Failure Types.
 *
 * <h2>Failure Hierarchy</h2>
 *
 * <pre>
 * IIOPFailure
 * ├── SpecificationFailure  - Missing, ambiguous, incomplete, conflicting
 * ├── OperationFailure      - Illegal, unsupported, precondition failed
 * ├── TargetFailure         - Ambiguous, not found, locked
 * ├── GenerationFailure     - Constraint violation, invariant violation, non-deterministic
 * ├── VerificationFailure   - Test failure, compile failure, timeout
 * └── ResourceFailure       - Model unavailable, token limit, rate limit
 * </pre>
 *
 * <h2>IOP v1 Failure Contract</h2> <blockquote> "AI must fail explicitly with a
 * declared reason, never silently or with generic errors." </blockquote>
 *
 * @since 1.0 - AI IOP
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares the sealed IIOPFailure metadata variants and the local signatures/defaults category, isRetriable, isBlocking, severity. The defaults project metadata or inspect the current variant and do not execute the described application operation.
 * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IIOPFailure.java}
 * blob {@code d682ae81fa4bd58a708e5954b57a173b22fe9554} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public sealed interface IIOPFailure extends IIOPConcept
        permits IIOPFailure.SpecificationFailure,
                IIOPFailure.OperationFailure,
                IIOPFailure.TargetFailure,
                IIOPFailure.GenerationFailure,
                IIOPFailure.VerificationFailure,
                IIOPFailure.ResourceFailure {

    /**
     * category metadata operation of IIOPFailure.
     * @m3.pattern synexia:iop
     * @m3.algorithm Returns the existing enum singleton <code>Category.FAILURE</code> without runtime enforcement.
     * @m3.dataStructures Declared Category result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
     */
    @Override
    default Category category() {
        return Category.FAILURE;
    }

    /**
     * Check if this failure is retriable.
     *
     * @return true if operation can be retried
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract isRetriable interface signature declares a boolean result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-1930
     * @m3.dataStructures Abstract interface signature returning boolean. Implementations supply the result and its ownership behavior.
     */
    boolean isRetriable();

    /**
     * Check if this failure is blocking.
     *
     * @return true if failure prevents progress
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract isBlocking interface signature declares a boolean result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-2072
     * @m3.dataStructures Abstract interface signature returning boolean. Implementations supply the result and its ownership behavior.
     */
    boolean isBlocking();

    /**
     * Get the severity of this failure.
     *
     * @return severity level
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract severity interface signature declares a Severity result without a method body or algorithm implementation.; review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-2193
     * @m3.dataStructures Abstract interface signature returning Severity. Implementations supply the result and its ownership behavior.
     */
    Severity severity();

    /** Severity levels.
     * @m3.pattern synexia:iop
     * @m3.algorithm Defines the existing 3 enum singleton values in declaration order and supplies generated name, ordinal, values and valueOf behavior. Any local constructor stores descriptor arguments without performing the operations named by those descriptors.
     * @m3.dataStructures Enum values in source order: RETRIABLE, WARNING, BLOCKING.
     * @m3.member {@code FIELD:RETRIABLE} pattern=synexia:iop; algorithm=Initializes RETRIABLE from the existing expression <code>new Severity()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One Severity field with its declared initializer.; description=Stored RETRIABLE metadata in Severity.
     * @m3.member {@code FIELD:WARNING} pattern=synexia:iop; algorithm=Initializes WARNING from the existing expression <code>new Severity()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One Severity field with its declared initializer.; description=Stored WARNING metadata in Severity.
     * @m3.member {@code FIELD:BLOCKING} pattern=synexia:iop; algorithm=Initializes BLOCKING from the existing expression <code>new Severity()</code>, preserving literal values, constructor argument order and enum singleton identity.; data.structures=One Severity field with its declared initializer.; description=Stored BLOCKING metadata in Severity.
     */
    enum Severity {
        RETRIABLE,
        WARNING,
        BLOCKING
    }

    // --- SPECIFICATION FAILURES ---

    /**
     * SpecificationFailure sealed metadata interface.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the sealed SpecificationFailure metadata variants and the local signatures/defaults isRetriable, isBlocking, severity. The defaults project metadata or inspect the current variant and do not execute the described application operation.
     * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
     */
    sealed interface SpecificationFailure extends IIOPFailure
            permits SpecificationFailure.MissingSpec,
                    SpecificationFailure.AmbiguousSpec,
                    SpecificationFailure.IncompleteSpec,
                    SpecificationFailure.ConflictingConstraints {

        /**
         * isRetriable metadata operation of SpecificationFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>false</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isRetriable() {
            return false;
        }

        /**
         * isBlocking metadata operation of SpecificationFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isBlocking() {
            return true;
        }

        /**
         * severity metadata operation of SpecificationFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>Severity.BLOCKING</code> without runtime enforcement.
         * @m3.dataStructures Declared Severity result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default Severity severity() {
            return Severity.BLOCKING;
        }

        /**
         * MissingSpec metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied MissingSpec metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String element. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:element} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The element declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-2978; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored element metadata in MissingSpec.
         */
        record MissingSpec(String element) implements SpecificationFailure {

            /**
             * name metadata operation of MissingSpec.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"MISSING_SPECIFICATION"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "MISSING_SPECIFICATION";
            }

            /**
             * description metadata operation of MissingSpec.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"No specification provided for " + element</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "No specification provided for " + element;
            }
        }

        /**
         * AmbiguousSpec metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied AmbiguousSpec metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String element, String[] interpretations. Arrays remain caller-owned aliases and generated record equality uses array identity.
         * @m3.member {@code FIELD:element} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The element declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-3334; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored element metadata in AmbiguousSpec.
         * @m3.member {@code FIELD:interpretations} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The interpretations declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-3350; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored interpretations metadata in AmbiguousSpec.
         */
        record AmbiguousSpec(String element, String[] interpretations) implements SpecificationFailure {

            /**
             * name metadata operation of AmbiguousSpec.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"AMBIGUOUS_SPECIFICATION"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "AMBIGUOUS_SPECIFICATION";
            }

            /**
             * description metadata operation of AmbiguousSpec.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Specification has multiple interpretations for " + element</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Specification has multiple interpretations for " + element;
            }
        }

        /**
         * IncompleteSpec metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied IncompleteSpec metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String element, String[] missingFields. Arrays remain caller-owned aliases and generated record equality uses array identity.
         * @m3.member {@code FIELD:element} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The element declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-3736; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored element metadata in IncompleteSpec.
         * @m3.member {@code FIELD:missingFields} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The missingFields declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-3752; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored missingFields metadata in IncompleteSpec.
         */
        record IncompleteSpec(String element, String[] missingFields) implements SpecificationFailure {

            /**
             * name metadata operation of IncompleteSpec.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"INCOMPLETE_SPECIFICATION"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "INCOMPLETE_SPECIFICATION";
            }

            /**
             * description metadata operation of IncompleteSpec.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Specification missing required information for " + element</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Specification missing required information for " + element;
            }
        }

        /**
         * ConflictingConstraints metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied ConflictingConstraints metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String[] constraints. Arrays remain caller-owned aliases and generated record equality uses array identity.
         * @m3.member {@code FIELD:constraints} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The constraints declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-4145; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored constraints metadata in ConflictingConstraints.
         */
        record ConflictingConstraints(String[] constraints) implements SpecificationFailure {

            /**
             * name metadata operation of ConflictingConstraints.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"CONFLICTING_CONSTRAINTS"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "CONFLICTING_CONSTRAINTS";
            }

            /**
             * description metadata operation of ConflictingConstraints.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"Constraints contradict each other"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Constraints contradict each other";
            }
        }
    }

    // --- OPERATION FAILURES ---

    /**
     * OperationFailure sealed metadata interface.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the sealed OperationFailure metadata variants and the local signatures/defaults isRetriable, isBlocking, severity. The defaults project metadata or inspect the current variant and do not execute the described application operation.
     * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
     */
    sealed interface OperationFailure extends IIOPFailure
            permits OperationFailure.IllegalOp, OperationFailure.UnsupportedOp, OperationFailure.PreconditionFailed {

        /**
         * isRetriable metadata operation of OperationFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>false</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isRetriable() {
            return false;
        }

        /**
         * isBlocking metadata operation of OperationFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isBlocking() {
            return true;
        }

        /**
         * severity metadata operation of OperationFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>Severity.BLOCKING</code> without runtime enforcement.
         * @m3.dataStructures Declared Severity result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default Severity severity() {
            return Severity.BLOCKING;
        }

        /**
         * IllegalOp metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied IllegalOp metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String operation, String[] allowedOperations. Arrays remain caller-owned aliases and generated record equality uses array identity.
         * @m3.member {@code FIELD:operation} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The operation declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-5009; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored operation metadata in IllegalOp.
         * @m3.member {@code FIELD:allowedOperations} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The allowedOperations declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-5027; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored allowedOperations metadata in IllegalOp.
         */
        record IllegalOp(String operation, String[] allowedOperations) implements OperationFailure {

            /**
             * name metadata operation of IllegalOp.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"ILLEGAL_OPERATION"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "ILLEGAL_OPERATION";
            }

            /**
             * description metadata operation of IllegalOp.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Operation " + operation + " not in allowed set"</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Operation " + operation + " not in allowed set";
            }
        }

        /**
         * UnsupportedOp metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied UnsupportedOp metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String operation. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:operation} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The operation declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-5393; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored operation metadata in UnsupportedOp.
         */
        record UnsupportedOp(String operation) implements OperationFailure {

            /**
             * name metadata operation of UnsupportedOp.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"UNSUPPORTED_OPERATION"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "UNSUPPORTED_OPERATION";
            }

            /**
             * description metadata operation of UnsupportedOp.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Operation " + operation + " not yet implemented"</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Operation " + operation + " not yet implemented";
            }
        }

        /**
         * PreconditionFailed metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied PreconditionFailed metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String operation, String precondition. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:operation} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The operation declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-5759; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored operation metadata in PreconditionFailed.
         * @m3.member {@code FIELD:precondition} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The precondition declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-5777; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored precondition metadata in PreconditionFailed.
         */
        record PreconditionFailed(String operation, String precondition) implements OperationFailure {

            /**
             * name metadata operation of PreconditionFailed.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"PRECONDITION_FAILED"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "PRECONDITION_FAILED";
            }

            /**
             * description metadata operation of PreconditionFailed.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Precondition \'" + precondition + "\' not met for " + operation</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Precondition '" + precondition + "' not met for " + operation;
            }
        }
    }

    // --- TARGET FAILURES ---

    /**
     * TargetFailure sealed metadata interface.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the sealed TargetFailure metadata variants and the local signatures/defaults isRetriable, isBlocking, severity. The defaults project metadata or inspect the current variant and do not execute the described application operation.
     * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
     */
    sealed interface TargetFailure extends IIOPFailure
            permits TargetFailure.AmbiguousTarget, TargetFailure.TargetNotFound, TargetFailure.TargetLocked {

        /**
         * isRetriable metadata operation of TargetFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>false</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isRetriable() {
            return false;
        }

        /**
         * isBlocking metadata operation of TargetFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isBlocking() {
            return true;
        }

        /**
         * severity metadata operation of TargetFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>Severity.BLOCKING</code> without runtime enforcement.
         * @m3.dataStructures Declared Severity result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default Severity severity() {
            return Severity.BLOCKING;
        }

        /**
         * AmbiguousTarget metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied AmbiguousTarget metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String target, String[] candidates. Arrays remain caller-owned aliases and generated record equality uses array identity.
         * @m3.member {@code FIELD:target} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The target declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-6651; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored target metadata in AmbiguousTarget.
         * @m3.member {@code FIELD:candidates} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The candidates declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-6666; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored candidates metadata in AmbiguousTarget.
         */
        record AmbiguousTarget(String target, String[] candidates) implements TargetFailure {

            /**
             * name metadata operation of AmbiguousTarget.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"AMBIGUOUS_TARGET"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "AMBIGUOUS_TARGET";
            }

            /**
             * description metadata operation of AmbiguousTarget.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Multiple elements match target: " + target</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Multiple elements match target: " + target;
            }
        }

        /**
         * TargetNotFound metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied TargetNotFound metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String target. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:target} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The target declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-7017; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored target metadata in TargetNotFound.
         */
        record TargetNotFound(String target) implements TargetFailure {

            /**
             * name metadata operation of TargetNotFound.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"TARGET_NOT_FOUND"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "TARGET_NOT_FOUND";
            }

            /**
             * description metadata operation of TargetNotFound.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Target not found: " + target</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Target not found: " + target;
            }
        }

        /**
         * TargetLocked metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied TargetLocked metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String target, String reason. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:target} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The target declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-7346; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored target metadata in TargetLocked.
         * @m3.member {@code FIELD:reason} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The reason declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-7361; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored reason metadata in TargetLocked.
         */
        record TargetLocked(String target, String reason) implements TargetFailure {

            /**
             * name metadata operation of TargetLocked.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"TARGET_LOCKED"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "TARGET_LOCKED";
            }

            /**
             * description metadata operation of TargetLocked.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Target is frozen: " + target + " (" + reason + ")"</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Target is frozen: " + target + " (" + reason + ")";
            }
        }
    }

    // --- GENERATION FAILURES ---

    /**
     * GenerationFailure sealed metadata interface.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the sealed GenerationFailure metadata variants and the local signatures/defaults isRetriable, isBlocking, severity. The defaults project metadata or inspect the current variant and do not execute the described application operation.
     * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
     */
    sealed interface GenerationFailure extends IIOPFailure
            permits GenerationFailure.ConstraintViolation,
                    GenerationFailure.InvariantViolation,
                    GenerationFailure.NonDeterministic,
                    GenerationFailure.UnsatisfiableIntent {

        /**
         * isRetriable metadata operation of GenerationFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates <code>this instanceof ConstraintViolation || this instanceof InvariantViolation</code> against the current sealed variant and returns the boolean result without performing retries or blocking work.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isRetriable() {
            return this instanceof ConstraintViolation || this instanceof InvariantViolation;
        }

        /**
         * isBlocking metadata operation of GenerationFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates <code>this instanceof UnsatisfiableIntent</code> against the current sealed variant and returns the boolean result without performing retries or blocking work.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isBlocking() {
            return this instanceof UnsatisfiableIntent;
        }

        /**
         * severity metadata operation of GenerationFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns BLOCKING for UnsatisfiableIntent, WARNING for NonDeterministic, and RETRIABLE for the other permitted generation-failure variants, checking those runtime types in source order.
         * @m3.dataStructures Current sealed failure variant and one Severity enum singleton.
         */
        @Override
        default Severity severity() {
            if (this instanceof UnsatisfiableIntent) return Severity.BLOCKING;
            if (this instanceof NonDeterministic) return Severity.WARNING;
            return Severity.RETRIABLE;
        }

        /**
         * ConstraintViolation metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied ConstraintViolation metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String constraint, String violatedBy. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:constraint} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The constraint declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-8598; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored constraint metadata in ConstraintViolation.
         * @m3.member {@code FIELD:violatedBy} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The violatedBy declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-8617; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored violatedBy metadata in ConstraintViolation.
         */
        record ConstraintViolation(String constraint, String violatedBy) implements GenerationFailure {

            /**
             * name metadata operation of ConstraintViolation.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"CONSTRAINT_VIOLATION"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "CONSTRAINT_VIOLATION";
            }

            /**
             * description metadata operation of ConstraintViolation.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Constraint \'" + constraint + "\' violated by: " + violatedBy</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Constraint '" + constraint + "' violated by: " + violatedBy;
            }
        }

        /**
         * InvariantViolation metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied InvariantViolation metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String invariant, String violatedBy. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:invariant} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The invariant declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-8995; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored invariant metadata in InvariantViolation.
         * @m3.member {@code FIELD:violatedBy} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The violatedBy declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-9013; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored violatedBy metadata in InvariantViolation.
         */
        record InvariantViolation(String invariant, String violatedBy) implements GenerationFailure {

            /**
             * name metadata operation of InvariantViolation.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"INVARIANT_VIOLATION"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "INVARIANT_VIOLATION";
            }

            /**
             * description metadata operation of InvariantViolation.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Invariant \'" + invariant + "\' violated by: " + violatedBy</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Invariant '" + invariant + "' violated by: " + violatedBy;
            }
        }

        /**
         * NonDeterministic metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied NonDeterministic metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String element, String[] outputs. Arrays remain caller-owned aliases and generated record equality uses array identity.
         * @m3.member {@code FIELD:element} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The element declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-9386; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored element metadata in NonDeterministic.
         * @m3.member {@code FIELD:outputs} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The outputs declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-9402; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored outputs metadata in NonDeterministic.
         */
        record NonDeterministic(String element, String[] outputs) implements GenerationFailure {

            /**
             * name metadata operation of NonDeterministic.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"NON_DETERMINISTIC_RESULT"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "NON_DETERMINISTIC_RESULT";
            }

            /**
             * description metadata operation of NonDeterministic.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Same input produces different outputs for: " + element</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Same input produces different outputs for: " + element;
            }
        }

        /**
         * UnsatisfiableIntent metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied UnsatisfiableIntent metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String intent, String reason. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:intent} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The intent declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-9779; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored intent metadata in UnsatisfiableIntent.
         * @m3.member {@code FIELD:reason} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The reason declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-9794; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored reason metadata in UnsatisfiableIntent.
         */
        record UnsatisfiableIntent(String intent, String reason) implements GenerationFailure {

            /**
             * name metadata operation of UnsatisfiableIntent.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"UNSATISFIABLE_INTENT"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "UNSATISFIABLE_INTENT";
            }

            /**
             * description metadata operation of UnsatisfiableIntent.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Intent cannot be satisfied: " + intent + " - " + reason</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Intent cannot be satisfied: " + intent + " - " + reason;
            }
        }
    }

    // --- VERIFICATION FAILURES ---

    /**
     * VerificationFailure sealed metadata interface.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the sealed VerificationFailure metadata variants and the local signatures/defaults isRetriable, isBlocking, severity. The defaults project metadata or inspect the current variant and do not execute the described application operation.
     * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
     */
    sealed interface VerificationFailure extends IIOPFailure
            permits VerificationFailure.TestFailed,
                    VerificationFailure.CompileFailed,
                    VerificationFailure.VerificationTimeout {

        /**
         * isRetriable metadata operation of VerificationFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>true</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isRetriable() {
            return true;
        }

        /**
         * isBlocking metadata operation of VerificationFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the fixed primitive value <code>false</code> as metadata without executing the described operation.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isBlocking() {
            return false;
        }

        /**
         * severity metadata operation of VerificationFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns the existing enum singleton <code>Severity.RETRIABLE</code> without runtime enforcement.
         * @m3.dataStructures Declared Severity result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default Severity severity() {
            return Severity.RETRIABLE;
        }

        /**
         * TestFailed metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied TestFailed metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String testClass, String[] failedTests, String[] errors. Arrays remain caller-owned aliases and generated record equality uses array identity.
         * @m3.member {@code FIELD:testClass} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The testClass declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-10725; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored testClass metadata in TestFailed.
         * @m3.member {@code FIELD:failedTests} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The failedTests declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-10743; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored failedTests metadata in TestFailed.
         * @m3.member {@code FIELD:errors} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The errors declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-10765; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored errors metadata in TestFailed.
         */
        record TestFailed(String testClass, String[] failedTests, String[] errors) implements VerificationFailure {

            /**
             * name metadata operation of TestFailed.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"TEST_FAILURE"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "TEST_FAILURE";
            }

            /**
             * description metadata operation of TestFailed.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Tests failed in " + testClass</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Tests failed in " + testClass;
            }
        }

        /**
         * CompileFailed metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied CompileFailed metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String file, String[] errors. Arrays remain caller-owned aliases and generated record equality uses array identity.
         * @m3.member {@code FIELD:file} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The file declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-11100; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored file metadata in CompileFailed.
         * @m3.member {@code FIELD:errors} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The errors declaration supplies a stored String[] metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-11113; data.structures=One stored String[] component or field. Reference values are retained and exposed without copying, including null.; description=Stored errors metadata in CompileFailed.
         */
        record CompileFailed(String file, String[] errors) implements VerificationFailure {

            /**
             * name metadata operation of CompileFailed.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"COMPILE_FAILURE"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "COMPILE_FAILURE";
            }

            /**
             * description metadata operation of CompileFailed.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Compilation failed for " + file</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Compilation failed for " + file;
            }
        }

        /**
         * VerificationTimeout metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied VerificationTimeout metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String element, long timeoutMs. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:element} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The element declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-11459; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored element metadata in VerificationTimeout.
         * @m3.member {@code FIELD:timeoutMs} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The timeoutMs declaration supplies a stored long metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-11475; data.structures=One stored long primitive component or field, accepted without numeric or policy validation.; description=Stored timeoutMs metadata in VerificationTimeout.
         */
        record VerificationTimeout(String element, long timeoutMs) implements VerificationFailure {

            /**
             * name metadata operation of VerificationTimeout.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"VERIFICATION_TIMEOUT"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "VERIFICATION_TIMEOUT";
            }

            /**
             * description metadata operation of VerificationTimeout.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Verification timed out after " + timeoutMs + "ms for " + element</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Verification timed out after " + timeoutMs + "ms for " + element;
            }
        }
    }

    // --- RESOURCE FAILURES ---

    /**
     * ResourceFailure sealed metadata interface.
     * @m3.pattern synexia:iop
     * @m3.algorithm Declares the sealed ResourceFailure metadata variants and the local signatures/defaults isRetriable, isBlocking, severity. The defaults project metadata or inspect the current variant and do not execute the described application operation.
     * @m3.dataStructures Sealed interface with explicit permitted implementations, nested metadata types and the existing IIOPConcept contract.
     */
    sealed interface ResourceFailure extends IIOPFailure
            permits ResourceFailure.ModelUnavailable,
                    ResourceFailure.TokenLimitExceeded,
                    ResourceFailure.RateLimitExceeded {

        /**
         * isRetriable metadata operation of ResourceFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates <code>this instanceof RateLimitExceeded || this instanceof ModelUnavailable</code> against the current sealed variant and returns the boolean result without performing retries or blocking work.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isRetriable() {
            return this instanceof RateLimitExceeded || this instanceof ModelUnavailable;
        }

        /**
         * isBlocking metadata operation of ResourceFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Evaluates <code>this instanceof TokenLimitExceeded</code> against the current sealed variant and returns the boolean result without performing retries or blocking work.
         * @m3.dataStructures Declared boolean result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
         */
        @Override
        default boolean isBlocking() {
            return this instanceof TokenLimitExceeded;
        }

        /**
         * severity metadata operation of ResourceFailure.
         * @m3.pattern synexia:iop
         * @m3.algorithm Returns BLOCKING for TokenLimitExceeded and RETRIABLE for the other permitted resource-failure variants.
         * @m3.dataStructures Current sealed failure variant and one Severity enum singleton.
         */
        @Override
        default Severity severity() {
            if (this instanceof TokenLimitExceeded) return Severity.BLOCKING;
            return Severity.RETRIABLE;
        }

        /**
         * ModelUnavailable metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied ModelUnavailable metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String model, String fallback. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:model} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The model declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-12585; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored model metadata in ModelUnavailable.
         * @m3.member {@code FIELD:fallback} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The fallback declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-12599; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored fallback metadata in ModelUnavailable.
         */
        record ModelUnavailable(String model, String fallback) implements ResourceFailure {

            /**
             * name metadata operation of ModelUnavailable.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"MODEL_UNAVAILABLE"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "MODEL_UNAVAILABLE";
            }

            /**
             * description metadata operation of ModelUnavailable.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Model " + model + " unavailable, fallback: " + fallback</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Model " + model + " unavailable, fallback: " + fallback;
            }
        }

        /**
         * TokenLimitExceeded metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied TokenLimitExceeded metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String model, int limit, int required. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:model} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The model declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-12966; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored model metadata in TokenLimitExceeded.
         * @m3.member {@code FIELD:limit} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The limit declaration supplies a stored int metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-12980; data.structures=One stored int primitive component or field, accepted without numeric or policy validation.; description=Stored limit metadata in TokenLimitExceeded.
         * @m3.member {@code FIELD:required} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The required declaration supplies a stored int metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-12991; data.structures=One stored int primitive component or field, accepted without numeric or policy validation.; description=Stored required metadata in TokenLimitExceeded.
         */
        record TokenLimitExceeded(String model, int limit, int required) implements ResourceFailure {

            /**
             * name metadata operation of TokenLimitExceeded.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"TOKEN_LIMIT_EXCEEDED"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "TOKEN_LIMIT_EXCEEDED";
            }

            /**
             * description metadata operation of TokenLimitExceeded.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Token limit " + limit + " exceeded, required: " + required</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Token limit " + limit + " exceeded, required: " + required;
            }
        }

        /**
         * RateLimitExceeded metadata value record.
         * @m3.pattern synexia:iop
         * @m3.algorithm Carries the supplied RateLimitExceeded metadata through its canonical record constructor, generated accessors and the locally declared projection/factory methods. Construction accepts component values without validation and retains reference components directly, including null.
         * @m3.dataStructures Record components: String model, long retryAfterMs. References are retained directly and primitive component values are stored unchanged.
         * @m3.member {@code FIELD:model} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The model declaration supplies a stored String metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-13360; data.structures=One stored String component or field. Reference values are retained and exposed without copying, including null.; description=Stored model metadata in RateLimitExceeded.
         * @m3.member {@code FIELD:retryAfterMs} pattern=synexia:iop; algorithm.applicability=not-applicable; algorithm.reason=The retryAfterMs declaration supplies a stored long metadata component or field without an initializer or executable algorithm body.; algorithm.review=iop-sealed-values-declarations/reviewed-declarations.json#IIOPFailure-13374; data.structures=One stored long primitive component or field, accepted without numeric or policy validation.; description=Stored retryAfterMs metadata in RateLimitExceeded.
         */
        record RateLimitExceeded(String model, long retryAfterMs) implements ResourceFailure {

            /**
             * name metadata operation of RateLimitExceeded.
             * @m3.pattern synexia:iop
             * @m3.algorithm Returns the fixed String literal <code>"RATE_LIMIT_EXCEEDED"</code> directly.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String name() {
                return "RATE_LIMIT_EXCEEDED";
            }

            /**
             * description metadata operation of RateLimitExceeded.
             * @m3.pattern synexia:iop
             * @m3.algorithm Evaluates the existing projection or concatenation expression <code>"Rate limit exceeded, retry after " + retryAfterMs + "ms"</code> in Java source order and returns the result. String concatenation renders null references as null text and performs no validation.
             * @m3.dataStructures Declared String result, existing receiver metadata and signature parameters. Any referenced record arrays remain directly aliased.
             */
            @Override
            public String description() {
                return "Rate limit exceeded, retry after " + retryAfterMs + "ms";
            }
        }
    }
}
