// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package org.codehaus.groovy.ast;

/** API-shape fixture for the existing downward-getter projection. */
public final class ModuleNode extends ASTNode {
    private final Object classes;
    public ModuleNode(Object classes) { this.classes = classes; }
    public Object getStatementBlock() { return null; }
    public Object getClasses() { return classes; }
}
