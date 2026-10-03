// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3EditScope;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3RecipeDagTest {

    @Test
    void canonicalDagIsDeterministicAndPromotionIsLast() {
        M3RecipeDag dag = M3RecipeDag.canonical();
        assertEquals(12, dag.size());
        assertEquals("inventory", dag.topologicalOrder().getFirst().id());
        assertEquals("promote", dag.topologicalOrder().getLast().id());
        assertTrue(dag.require("promote").serialPromotion());
        assertThrows(IllegalArgumentException.class, () -> dag.require("missing"));
    }

    @Test
    void independentFileRecipesShareOneParallelLayer() {
        M3RecipeDag dag = new M3RecipeDag(List.of(
                node("plan", M3DagKind.COMPATIBILITY, M3EditScope.FILE, false, false, false, "", List.of()),
                node("file-a", M3DagKind.RECIPE, M3EditScope.FILE, true, false, false, "recipe.A", List.of("plan")),
                node("file-b", M3DagKind.RECIPE, M3EditScope.FILE, true, false, false, "recipe.B", List.of("plan")),
                node("verify", M3DagKind.VERIFICATION, M3EditScope.FILE, false, false, false, "", List.of("file-a", "file-b")),
                node("promote", M3DagKind.PROMOTION, M3EditScope.FILE, false, true, false, "serial", List.of("verify"))));

        assertEquals(
                List.of("file-a", "file-b"),
                dag.layers().get(1).stream().map(M3DagNode::id).toList());
    }

    @Test
    void explicitScopePromotionIsRequired() {
        List<M3DagNode> rejected = List.of(
                node("file", M3DagKind.RECIPE, M3EditScope.FILE, true, false, false, "recipe.A", List.of()),
                node("package", M3DagKind.RECIPE, M3EditScope.PACKAGE, true, false, false, "recipe.B", List.of("file")),
                node("promote", M3DagKind.PROMOTION, M3EditScope.PACKAGE, false, true, false, "serial", List.of("package")));
        assertThrows(IllegalArgumentException.class, () -> new M3RecipeDag(rejected));

        M3RecipeDag accepted = new M3RecipeDag(List.of(
                node("file", M3DagKind.RECIPE, M3EditScope.FILE, true, false, false, "recipe.A", List.of()),
                node("package", M3DagKind.RECIPE, M3EditScope.PACKAGE, true, false, true, "recipe.B", List.of("file")),
                node("promote", M3DagKind.PROMOTION, M3EditScope.PACKAGE, false, true, false, "serial", List.of("package"))));
        assertEquals(M3EditScope.PACKAGE, accepted.require("package").scope());
    }

    @Test
    void structuralErrorsFailClosed() {
        M3DagNode a = node("a", M3DagKind.RECIPE, M3EditScope.FILE, true, false, false, "recipe.A", List.of());
        assertThrows(IllegalArgumentException.class, () -> new M3RecipeDag(List.of(a, a)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3RecipeDag(List.of(
                        a,
                        node("promote", M3DagKind.PROMOTION, M3EditScope.FILE, false, true, false, "serial", List.of("missing")))));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3RecipeDag(List.of(
                        node("a", M3DagKind.RECIPE, M3EditScope.FILE, true, false, false, "recipe.A", List.of("b")),
                        node("b", M3DagKind.RECIPE, M3EditScope.FILE, true, false, false, "recipe.B", List.of("a")),
                        node("promote", M3DagKind.PROMOTION, M3EditScope.FILE, false, true, false, "serial", List.of("a")))));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3RecipeDag(List.of(
                        a,
                        node("verify", M3DagKind.VERIFICATION, M3EditScope.FILE, false, false, false, "", List.of("a")))));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3RecipeDag(List.of(
                        node("promote", M3DagKind.PROMOTION, M3EditScope.FILE, false, true, false, "serial", List.of()),
                        node("z", M3DagKind.VERIFICATION, M3EditScope.FILE, false, false, false, "", List.of()))));
    }

    @Test
    void dagNodeAndAdmissionFactsValidateTheirOwnContracts() {
        assertThrows(
                IllegalArgumentException.class,
                () -> node("", M3DagKind.RECIPE, M3EditScope.FILE, true, false, false, "x", List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> node("x", M3DagKind.RECIPE, M3EditScope.FILE, true, false, false, "", List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> node("x", M3DagKind.RECIPE, M3EditScope.FILE, true, false, false, "x", List.of("x")));
        assertThrows(
                IllegalArgumentException.class,
                () -> node("x", M3DagKind.RECIPE, M3EditScope.FILE, true, false, false, "x", List.of(" ")));
        assertThrows(
                IllegalArgumentException.class,
                () -> node("x", M3DagKind.PROMOTION, M3EditScope.FILE, false, false, false, "serial", List.of()));

        assertThrows(
                IllegalArgumentException.class,
                () -> new M3AdmissionFact(" ", true, true, true, true, false, false, true, true));
        assertTrue(new M3AdmissionFact("p", true, true, true, true, false, false, true, true).promotable());
        assertTrue(new M3AdmissionFact("p", true, true, true, true, true, true, true, true).promotable());
        assertFalse(new M3AdmissionFact("p", true, true, true, true, true, false, true, true).promotable());
        assertFalse(new M3AdmissionFact("p", false, true, true, true, false, false, true, true).promotable());
        assertFalse(new M3AdmissionFact("p", true, false, true, true, false, false, true, true).promotable());
        assertFalse(new M3AdmissionFact("p", true, true, false, true, false, false, true, true).promotable());
        assertFalse(new M3AdmissionFact("p", true, true, true, false, false, false, true, true).promotable());
        assertFalse(new M3AdmissionFact("p", true, true, true, true, false, false, false, true).promotable());
    }

    private static M3DagNode node(
            String id,
            M3DagKind kind,
            M3EditScope scope,
            boolean mutating,
            boolean serialPromotion,
            boolean scopePromotionApproved,
            String workRef,
            List<String> dependencies) {
        return new M3DagNode(
                id,
                kind,
                scope,
                mutating,
                serialPromotion,
                scopePromotionApproved,
                workRef,
                dependencies);
    }
}
