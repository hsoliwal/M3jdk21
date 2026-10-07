// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.scope.M3EditScope;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3BackportPacketTest {
    @Test
    void strictTsvLoadsOnePacketInDeclaredAtomOrder() {
        String tsv =
                M3BackportPacketLoader.HEADER
                        + "\n"
                        + "jdk-x\tjava-a\tFILE\tfalse\trecipe.A\t\n"
                        + "jdk-x\ttext-b\tFILE\tfalse\trecipe.B\t\n"
                        + "jdk-x\tpackage-join\tPACKAGE\ttrue\trecipe.C\tjava-a,text-b\n";

        M3BackportPacket packet = M3BackportPacketLoader.parse(tsv);

        assertEquals("jdk-x", packet.packetId());
        assertEquals(List.of("java-a", "text-b", "package-join"),
                packet.atoms().stream().map(M3RecipeAtom::id).toList());
        assertEquals(M3EditScope.PACKAGE, packet.maximumScope());
        assertEquals(List.of("package-join"),
                packet.terminalAtoms().stream().map(M3RecipeAtom::id).toList());
        assertEquals("recipe.A", packet.require("java-a").workRef());
        assertThrows(IllegalArgumentException.class, () -> packet.require("missing"));
    }

    @Test
    void atomAndPacketContractsFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3RecipeAtom(
                        "Bad_Id",
                        M3EditScope.FILE,
                        false,
                        "recipe.A",
                        List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3RecipeAtom(
                        "a",
                        M3EditScope.FILE,
                        false,
                        "",
                        List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3RecipeAtom(
                        "a",
                        M3EditScope.FILE,
                        false,
                        "recipe.A",
                        List.of("a")));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3RecipeAtom(
                        "a",
                        M3EditScope.FILE,
                        false,
                        "recipe.A",
                        List.of("b", "b")));

        M3RecipeAtom a =
                new M3RecipeAtom(
                        "a", M3EditScope.FILE, false, "recipe.A", List.of());
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3BackportPacket("Bad_Id", List.of(a)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3BackportPacket("p", List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3BackportPacket("p", List.of(a, a)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new M3BackportPacket(
                        "p",
                        List.of(
                                new M3RecipeAtom(
                                        "b",
                                        M3EditScope.FILE,
                                        false,
                                        "recipe.B",
                                        List.of("missing")))));
    }

    @Test
    void loaderRejectsMalformedRowsMixedPacketsAndInvalidBooleans() {
        assertThrows(
                IllegalArgumentException.class,
                () -> M3BackportPacketLoader.parse(""));
        assertThrows(
                IllegalArgumentException.class,
                () -> M3BackportPacketLoader.parse("wrong\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3BackportPacketLoader.parse(
                                M3BackportPacketLoader.HEADER
                                        + "\np\ta\tFILE\tfalse\trecipe.A\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3BackportPacketLoader.parse(
                                M3BackportPacketLoader.HEADER
                                        + "\np\ta\tFILE\tmaybe\trecipe.A\t\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        M3BackportPacketLoader.parse(
                                M3BackportPacketLoader.HEADER
                                        + "\np\ta\tFILE\tfalse\trecipe.A\t\n"
                                        + "q\tb\tFILE\tfalse\trecipe.B\t\n"));
    }

    @Test
    void commentAndBlankRowsDoNotChangePacketIdentity() {
        String tsv =
                M3BackportPacketLoader.HEADER
                        + "\n# evidence only\n\n"
                        + "p\ta\tFILE\tfalse\trecipe.A\t\n";

        M3BackportPacket packet = M3BackportPacketLoader.parse(tsv);

        assertEquals("p", packet.packetId());
        assertEquals(1, packet.atoms().size());
        assertEquals(M3EditScope.FILE, packet.maximumScope());
        assertTrue(packet.terminalAtoms().contains(packet.atoms().getFirst()));
        assertFalse(packet.atoms().getFirst().scopePromotionApproved());
    }
}
