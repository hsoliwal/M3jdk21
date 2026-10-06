// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class A3PolishTest {

    @Test
    void allInventoryRowsMapToOnePublicPolishTarget() {
        List<A3Inv.Row> rows =
                List.of(
                        row("src", "java.base", "share", "src/java.base/share/classes/p/A.java", A3Inv.Kind.JAVA, "1"),
                        row("src", "java.base", "share", "src/java.base/share/native/libjava/a.c", A3Inv.Kind.NATIVE, "2"),
                        row("src", "jdk.compiler", "share", "src/jdk.compiler/share/classes/p/C.java", A3Inv.Kind.JAVA, "3"),
                        row("src", "hotspot", "share", "src/hotspot/share/runtime/vm.cpp", A3Inv.Kind.NATIVE, "4"),
                        row("test", "jdk", "java", "test/jdk/java/lang/Test.java", A3Inv.Kind.JAVA, "5"),
                        row("test", "hotspot", "jtreg", "test/hotspot/jtreg/runtime/Test.cpp", A3Inv.Kind.NATIVE, "6"));

        A3Polish.Workspace workspace =
                A3Polish.compile(rows, "f".repeat(64), true);

        assertEquals(rows.size(), workspace.inventoryFiles());
        assertEquals(rows.size(), workspace.coveredInventoryFiles());
        assertEquals(6, workspace.targetCount());
        assertEquals(3, workspace.candidateTargets());
        assertEquals(3, workspace.nativeGateTargets());
        assertEquals(64, workspace.inventoryRoot().length());
        assertEquals(64, workspace.targetRoot().length());
        assertFalse(workspace.sourceMutationAuthority());
        assertFalse(workspace.promotionAuthority());

        A3Polish.Target base = target(workspace, "java.base", "OPENJDK_SOURCE");
        assertEquals(A3Polish.Kind.OWNED_MIXED_NATIVE, base.kind());
        assertEquals(A3Polish.Action.DRY_RUN_CANDIDATE, base.action());
        assertEquals(A3Polish.RECIPE, base.recipe());
        assertTrue(base.nativeGate());

        A3Polish.Target hotspot = target(workspace, "hotspot", "OPENJDK_SOURCE");
        assertEquals(A3Polish.Kind.NATIVE_ONLY, hotspot.kind());
        assertEquals(A3Polish.Action.VERIFY_ONLY, hotspot.action());
        assertEquals("NONE", hotspot.recipe());
        assertTrue(hotspot.nativeGate());

        A3Polish.Target jdkTests = target(workspace, "jdk", "OPENJDK_TEST");
        assertEquals(A3Polish.Kind.TEST_SUITE, jdkTests.kind());
        assertEquals(A3Polish.Action.DRY_RUN_CANDIDATE, jdkTests.action());

        A3Polish.Target hotspotTests = target(workspace, "hotspot", "OPENJDK_TEST");
        assertEquals(A3Polish.Action.VERIFY_ONLY, hotspotTests.action());
        assertTrue(hotspotTests.nativeGate());

        assertTrue(
                workspace.targets().stream()
                        .anyMatch(target -> target.kind() == A3Polish.Kind.BUILD_METADATA));
    }

    private static A3Polish.Target target(
            A3Polish.Workspace workspace,
            String name,
            String family) {
        return workspace.targets().stream()
                .filter(target -> target.target().equals(name) && target.family().equals(family))
                .findFirst()
                .orElseThrow();
    }

    private static A3Inv.Row row(
            String tree,
            String module,
            String area,
            String path,
            A3Inv.Kind kind,
            String salt) {
        return new A3Inv.Row(
                tree,
                module,
                area,
                path,
                kind,
                1,
                (salt + "0".repeat(64)).substring(0, 64));
    }
}
