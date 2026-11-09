// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.rewrite.scope.M3EditScope;
import java.util.List;
import org.junit.jupiter.api.Test;

final class BackportPacketTest {
    @Test
    void strictTsvLoadsOnePacketInDeclaredAtomOrder() {
        String tsv =
                BackportPacketLoader.HEADER
                        + "\n"
                        + "jdk-x\tjava-a\tFILE\tfalse\trecipe.A\t\n"
                        + "jdk-x\ttext-b\tFILE\tfalse\trecipe.B\t\n"
                        + "jdk-x\tpackage-join\tPACKAGE\ttrue\trecipe.C\tjava-a,text-b\n";

        BackportPacket packet = BackportPacketLoader.parse(tsv);

        assertEquals("jdk-x", packet.packetId());
        assertEquals(List.of("java-a", "text-b", "package-join"),
                packet.atoms().stream().map(RecipeAtom::id).toList());
        assertEquals(M3EditScope.PACKAGE, packet.maximumScope());
        assertEquals(List.of("package-join"),
                packet.terminalAtoms().stream().map(RecipeAtom::id).toList());
        assertEquals("recipe.A", packet.require("java-a").workRef());
        assertThrows(IllegalArgumentException.class, () -> packet.require("missing"));
    }

    @Test
    void atomAndPacketContractsFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecipeAtom(
                        "Bad_Id",
                        M3EditScope.FILE,
                        false,
                        "recipe.A",
                        List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecipeAtom(
                        "a",
                        M3EditScope.FILE,
                        false,
                        "",
                        List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecipeAtom(
                        "a",
                        M3EditScope.FILE,
                        false,
                        "recipe.A",
                        List.of("a")));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RecipeAtom(
                        "a",
                        M3EditScope.FILE,
                        false,
                        "recipe.A",
                        List.of("b", "b")));

        RecipeAtom a =
                new RecipeAtom(
                        "a", M3EditScope.FILE, false, "recipe.A", List.of());
        assertThrows(
                IllegalArgumentException.class,
                () -> new BackportPacket("Bad_Id", List.of(a)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new BackportPacket("p", List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new BackportPacket("p", List.of(a, a)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new BackportPacket(
                        "p",
                        List.of(
                                new RecipeAtom(
                                        "b",
                                        M3EditScope.FILE,
                                        false,
                                        "recipe.B",
                                        List.of("missing")))));
    }

    @Test
    void cyclicPacketDependenciesAreRejectedEvenWhenScopeIsUnapproved() {
        RecipeAtom first =
                new RecipeAtom(
                        "a", M3EditScope.MODULE, false, "AUTHOR_RECIPE:a", List.of("b"));
        RecipeAtom second =
                new RecipeAtom(
                        "b", M3EditScope.MODULE, false, "AUTHOR_RECIPE:b", List.of("a"));

        assertThrows(
                IllegalArgumentException.class,
                () -> new BackportPacket("p", List.of(first, second)));
    }

    @Test
    void loaderRejectsMalformedRowsMixedPacketsAndInvalidBooleans() {
        assertThrows(
                IllegalArgumentException.class,
                () -> BackportPacketLoader.parse(""));
        assertThrows(
                IllegalArgumentException.class,
                () -> BackportPacketLoader.parse("wrong\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BackportPacketLoader.parse(
                                BackportPacketLoader.HEADER
                                        + "\np\ta\tFILE\tfalse\trecipe.A\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BackportPacketLoader.parse(
                                BackportPacketLoader.HEADER
                                        + "\np\ta\tFILE\tmaybe\trecipe.A\t\n"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BackportPacketLoader.parse(
                                BackportPacketLoader.HEADER
                                        + "\np\ta\tFILE\tfalse\trecipe.A\t\n"
                                        + "q\tb\tFILE\tfalse\trecipe.B\t\n"));
    }

    @Test
    void commentAndBlankRowsDoNotChangePacketIdentity() {
        String tsv =
                BackportPacketLoader.HEADER
                        + "\n# evidence only\n\n"
                        + "p\ta\tFILE\tfalse\trecipe.A\t\n";

        BackportPacket packet = BackportPacketLoader.parse(tsv);

        assertEquals("p", packet.packetId());
        assertEquals(1, packet.atoms().size());
        assertEquals(M3EditScope.FILE, packet.maximumScope());
        assertTrue(packet.terminalAtoms().contains(packet.atoms().getFirst()));
        assertFalse(packet.atoms().getFirst().scopePromotionApproved());
    }
}
