// SPDX-License-Identifier: Apache-2.0
package org.eclipse.jdt.core.dom;
import java.util.ArrayList;
import java.util.List;
/** Test-only JDT DOM contract double, not an Eclipse implementation. */
public class ASTNode {
    public static final int RECOVERED = 1;
    public static final int MALFORMED = 2;
    private final int start, length;
    private int flags;
    private final List<StructuralPropertyDescriptor> properties = new ArrayList<>();
    private final List<Object> values = new ArrayList<>();
    public ASTNode(int start, int length) {
        this.start = start;
        this.length = length;
    }
    public int getStartPosition() { return start; }
    public int getLength() { return length; }
    public int getFlags() { return flags; }
    public void setFlags(int flags) { this.flags = flags; }
    public List<StructuralPropertyDescriptor> structuralPropertiesForType() { return properties; }
    public Object getStructuralProperty(StructuralPropertyDescriptor name) {
        int index = properties.indexOf(name);
        return index == -1 ? null : values.get(index);
    }
    public void add(String name, Object child) {
        properties.add(new StructuralPropertyDescriptor(name));
        values.add(child);
    }
}
