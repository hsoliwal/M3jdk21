// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jackson;
/** Test-only public-shape double, not the production Jackson M3AST pool. */
public final class M3ASTAtom {
    private final String kind;
    private final M3ASTAtom[] children;
    public M3ASTAtom(String kind, M3ASTAtom... children) {
        this.kind = kind;
        this.children = children.clone();
    }
    public String kind() { return kind; }
    public int childCount() { return children.length; }
    public M3ASTAtom child(int index) { return children[index]; }
}
