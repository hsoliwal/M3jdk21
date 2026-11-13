// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jackson;
/** Test-only public-shape double, not production Jackson M3AST. */
public final class M3AST {
    private final M3ASTAtom root;
    public M3AST(M3ASTAtom root) { this.root = root; }
    public M3ASTAtom root() { return root; }
}
