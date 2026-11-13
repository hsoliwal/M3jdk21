// SPDX-License-Identifier: Apache-2.0
package org.eclipse.jdt.core.dom;
/** Test-only JDT-shaped source descriptor; never delivered in parser JMOD. */
public final class StructuralPropertyDescriptor {
    private final String id;
    public StructuralPropertyDescriptor(String id) { this.id = id; }
    public String getId() { return id; }
}
