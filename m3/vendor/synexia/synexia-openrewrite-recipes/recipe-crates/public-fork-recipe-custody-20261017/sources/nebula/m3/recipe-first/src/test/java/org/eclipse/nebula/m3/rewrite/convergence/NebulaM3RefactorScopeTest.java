// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class NebulaM3RefactorScopeTest {
    @Test
    void canonicalOrderMatchesTheM3AuthorityLadderExactly() {
        assertEquals(
                List.of(
                        NebulaM3RefactorScope.FILE,
                        NebulaM3RefactorScope.VISIBILITY,
                        NebulaM3RefactorScope.PACKAGE,
                        NebulaM3RefactorScope.MODULE,
                        NebulaM3RefactorScope.MULTI_MODULE,
                        NebulaM3RefactorScope.LIBRARY_API),
                NebulaM3RefactorScope.canonicalOrder());
    }

    @Test
    void authorityContainsOnlyNarrowerOrEqualScopes() {
        for (NebulaM3RefactorScope authority : NebulaM3RefactorScope.values()) {
            for (NebulaM3RefactorScope required : NebulaM3RefactorScope.values()) {
                assertEquals(
                        authority.ordinal() >= required.ordinal(),
                        authority.canContain(required),
                        authority + " -> " + required);
            }
        }
        assertFalse(
                NebulaM3RefactorScope.FILE.canContain(
                        NebulaM3RefactorScope.VISIBILITY));
        assertTrue(
                NebulaM3RefactorScope.LIBRARY_API.canContain(
                        NebulaM3RefactorScope.FILE));
    }

    @Test
    void promotionSelectsTheSmallestAuthorityContainingBothRequirements() {
        assertEquals(
                NebulaM3RefactorScope.FILE,
                NebulaM3RefactorScope.max(
                        NebulaM3RefactorScope.FILE,
                        NebulaM3RefactorScope.FILE));
        assertEquals(
                NebulaM3RefactorScope.VISIBILITY,
                NebulaM3RefactorScope.max(
                        NebulaM3RefactorScope.FILE,
                        NebulaM3RefactorScope.VISIBILITY));
        assertEquals(
                NebulaM3RefactorScope.MULTI_MODULE,
                NebulaM3RefactorScope.max(
                        NebulaM3RefactorScope.PACKAGE,
                        NebulaM3RefactorScope.MULTI_MODULE));
        assertEquals(
                NebulaM3RefactorScope.LIBRARY_API,
                NebulaM3RefactorScope.max(
                        NebulaM3RefactorScope.LIBRARY_API,
                        NebulaM3RefactorScope.MODULE));
    }

    @Test
    void nullAuthorityFailsClosed() {
        assertThrows(
                NullPointerException.class,
                () -> NebulaM3RefactorScope.FILE.canContain(null));
        assertThrows(
                NullPointerException.class,
                () -> NebulaM3RefactorScope.max(
                        null,
                        NebulaM3RefactorScope.FILE));
        assertThrows(
                NullPointerException.class,
                () -> NebulaM3RefactorScope.max(
                        NebulaM3RefactorScope.FILE,
                        null));
    }

    @Test
    void checkedInPolicyAndPlanMatchTheAuthorityLadder() throws Exception {
        Path root = repositoryRoot();
        String policy =
                Files.readString(root.resolve(".m3/atom-pattern/policy.properties"));
        String plan = Files.readString(root.resolve("m3/convergence/PLAN.tsv"));

        assertTrue(
                policy.contains(
                        "refactor.scope.order=FILE,VISIBILITY,PACKAGE,MODULE,MULTI_MODULE,LIBRARY_API"));
        assertTrue(policy.contains("proof.outerScopes=PROJECT,REPOSITORY"));
        assertTrue(policy.contains("scope.promoteOnlyOnBoundaryCrossing=true"));
        assertTrue(policy.contains("scope.repositorySizeDoesNotPromote=true"));

        int file = plan.indexOf("\tFILE_FIXED_POINT\t");
        int visibility = plan.indexOf("\tVISIBILITY_FAN_IN\t");
        int pkg = plan.indexOf("\tPACKAGE_FAN_IN\t");
        int module = plan.indexOf("\tMODULE_FAN_IN\t");
        int multi = plan.indexOf("\tMULTI_MODULE_FAN_IN\t");
        int library = plan.indexOf("\tLIBRARY_API_FAN_IN\t");
        int project = plan.indexOf("\tPROJECT_FAN_IN\t");
        int repository = plan.indexOf("\tREPOSITORY_FAN_IN\t");

        assertTrue(file >= 0);
        assertTrue(file < visibility);
        assertTrue(visibility < pkg);
        assertTrue(pkg < module);
        assertTrue(module < multi);
        assertTrue(multi < library);
        assertTrue(library < project);
        assertTrue(project < repository);
    }

    @Test
    void canonicalFileDagNeverAcquiresBroaderAuthority() {
        assertEquals(
                NebulaM3RefactorScope.FILE,
                NebulaM3FileConvergenceRecipeDag.maximumEditScope());
        assertTrue(
                NebulaM3RecipeDagManifest.nodes().stream()
                        .allMatch(
                                node ->
                                        node.scope()
                                                .equals(
                                                        NebulaM3RefactorScope.FILE
                                                                .name())));
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("m3"))
                    && Files.isDirectory(current.resolve("widgets"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Nebula repository root not found");
    }
}
