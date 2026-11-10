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

/** PatternKind families absorbed by IOP.
 * @m3.pattern synexia:iop
 * @m3.algorithm Declare four historical pattern-family labels in fixed order; this enum is separate from the broader executable PatternCatalog family registry.
 * @m3.dataStructures PatternKind enum singleton array in declaration order with final label fields.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/Pattern.java}
 * blob {@code c05389b7c8e2516b502279ffe7c85d3972260d40} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum PatternKind {

    /** Gang of Four design patterns.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify PatternKind#GOF with description equal to GoF - Gang of Four Design Patterns; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One PatternKind enum singleton with its exact constructor literals.
     */
    GOF("GoF - Gang of Four Design Patterns"),
    /** Enterprise Integration Patterns.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify PatternKind#EIP with description equal to EIP - Enterprise Integration Patterns; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One PatternKind enum singleton with its exact constructor literals.
     */
    EIP("EIP - Enterprise Integration Patterns"),
    /** Microservice resilience patterns.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify PatternKind#MICROSERVICE with description equal to Microservice Resilience Patterns; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One PatternKind enum singleton with its exact constructor literals.
     */
    MICROSERVICE("Microservice Resilience Patterns"),
    /** DAG flow and orchestration patterns.
     * @m3.pattern synexia:iop
     * @m3.algorithm Identify PatternKind#DAG_FLOW with description equal to DAG Flow Patterns; preserve this authored label without executing or enforcing its claimed rule.
     * @m3.dataStructures One PatternKind enum singleton with its exact constructor literals.
     */
    DAG_FLOW("DAG Flow Patterns");

    /**
     * Store PatternKind.description once from the matching constructor argument without validation or transformation.
     * @m3.pattern synexia:iop
     * @m3.algorithm Store PatternKind.description once from the matching constructor argument without validation or transformation.
     * @m3.dataStructures Final String reference for the description label.
     */
    private final String description;

    /**
     * Initialize PatternKind by directly assigning each constructor argument to its matching final field, preserving label references and any explicit order integer.
     * @m3.pattern synexia:iop
     * @m3.algorithm Initialize PatternKind by directly assigning each constructor argument to its matching final field, preserving label references and any explicit order integer.
     * @m3.dataStructures Constructor arguments and final PatternKind instance fields.
     */
    PatternKind(String description) {
        this.description = description;
    }

    /**
     * Return this PatternKind singleton's stored description value unchanged.
     * @m3.pattern synexia:iop
     * @m3.algorithm Return this PatternKind singleton's stored description value unchanged.
     * @m3.dataStructures Existing final description String reference; no allocation or transformation.
     */
    public String description() {
        return description;
    }
}
