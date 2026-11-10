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
 * IIOPConcept - Root Union of All IOP Concepts.
 *
 * <h2>Union of Union PatternKind</h2>
 *
 * <pre>
 * IIOPConcept
 * ├── IIOPSpec         - What to generate (specifications)
 * ├── IIOPOperation    - How to change (operations)
 * ├── IIOPFailure      - What can fail (failure semantics)
 * ├── IIOPTarget       - Where to output (materialization)
 * ├── IIOPContract     - What guarantees (determinism/purity)
 * ├── IIOPParadigm     - Which paradigm (COP/AOP/OOP/FOP/IOP/AnOP)
 * └── IIOPPattern      - Which patterns (GoF/EIP/Micro/DAG/Distributed)
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 *
 * // PatternKind matching on IOP concepts
 * void process(IIOPConcept concept) {
 * 	switch (concept) {
 * 		case IIOPSpec spec -> handleSpec(spec);
 * 		case IIOPOperation op -> handleOperation(op);
 * 		case IIOPFailure fail -> handleFailure(fail);
 * 		case IIOPTarget target -> handleTarget(target);
 * 		case IIOPContract contract -> handleContract(contract);
 * 		case IIOPParadigm paradigm -> handleParadigm(paradigm);
 * 		case IIOPPattern pattern -> handlePattern(pattern);
 * 	}
 * }
 * }</pre>
 *
 * @since 1.0 - AI IOP
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares the sealed root for specification, operation, failure, target, contract, paradigm and pattern metadata. The interface supplies no processing implementation.
 * @m3.dataStructures Sealed interface with seven permitted subinterfaces, three abstract signatures and a nested Category enum.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IIOPConcept.java}
 * blob {@code 1cc0de191d2bf09fa554714166d383a9d17262b1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public sealed interface IIOPConcept
        permits IIOPSpec, IIOPOperation, IIOPFailure, IIOPTarget, IIOPContract, IIOPParadigm, IIOPPattern {

    /**
     * Get the name of this concept.
     *
     * @return concept name
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract interface signature has no method body or algorithm implementation.; review=iop-contract-declarations-20261009/reviewed-declarations.json#IIOPConcept-name
     * @m3.dataStructures Abstract zero-argument method returning a String reference.
     */
    String name();

    /**
     * Get description of this concept.
     *
     * @return description
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract interface signature has no method body or algorithm implementation.; review=iop-contract-declarations-20261009/reviewed-declarations.json#IIOPConcept-description
     * @m3.dataStructures Abstract zero-argument method returning a String reference.
     */
    String description();

    /**
     * Get the category this concept belongs to.
     *
     * @return category
     * @m3.pattern synexia:iop
     * @m3.applicability algorithm=not-applicable; reason=This abstract interface signature has no method body or algorithm implementation.; review=iop-contract-declarations-20261009/reviewed-declarations.json#IIOPConcept-category
     * @m3.dataStructures Abstract zero-argument method returning a Category reference.
     */
    Category category();

    /** IOP Concept categories.
     * @m3.pattern synexia:iop
     * @m3.algorithm Enumerates seven concept categories in their existing declaration order.
     * @m3.dataStructures Enum constants SPECIFICATION, OPERATION, FAILURE, TARGET, CONTRACT, PARADIGM and PATTERN.
     * @m3.member {@code FIELD:SPECIFICATION} pattern=synexia:iop; algorithm=Names the SPECIFICATION metadata category without implementing that category or its policy.; data.structures=Existing Category enum constant with its identity and declaration order preserved.
     * @m3.member {@code FIELD:OPERATION} pattern=synexia:iop; algorithm=Names the OPERATION metadata category without implementing that category or its policy.; data.structures=Existing Category enum constant with its identity and declaration order preserved.
     * @m3.member {@code FIELD:FAILURE} pattern=synexia:iop; algorithm=Names the FAILURE metadata category without implementing that category or its policy.; data.structures=Existing Category enum constant with its identity and declaration order preserved.
     * @m3.member {@code FIELD:TARGET} pattern=synexia:iop; algorithm=Names the TARGET metadata category without implementing that category or its policy.; data.structures=Existing Category enum constant with its identity and declaration order preserved.
     * @m3.member {@code FIELD:CONTRACT} pattern=synexia:iop; algorithm=Names the CONTRACT metadata category without implementing that category or its policy.; data.structures=Existing Category enum constant with its identity and declaration order preserved.
     * @m3.member {@code FIELD:PARADIGM} pattern=synexia:iop; algorithm=Names the PARADIGM metadata category without implementing that category or its policy.; data.structures=Existing Category enum constant with its identity and declaration order preserved.
     * @m3.member {@code FIELD:PATTERN} pattern=synexia:iop; algorithm=Names the PATTERN metadata category without implementing that category or its policy.; data.structures=Existing Category enum constant with its identity and declaration order preserved.
     */
    enum Category {
        /** Specification concepts (what to generate). */
        SPECIFICATION,

        /** Operation concepts (how to change). */
        OPERATION,

        /** Failure concepts (what can fail). */
        FAILURE,

        /** Target concepts (where to output). */
        TARGET,

        /** Contract concepts (what guarantees). */
        CONTRACT,

        /** Paradigm concepts (which paradigm). */
        PARADIGM,

        /** PatternKind concepts (which patterns). */
        PATTERN
    }
}
