// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class NebulaM3ApacheHandoffBindingTest {
    @Test
    void bindingIsExactQualificationOnlyAndCannotRelicenseNebula() {
        assertEquals("hsoliwal/com.synexia", NebulaM3ApacheHandoffBinding.UPSTREAM_REPOSITORY);
        assertEquals(9642, NebulaM3ApacheHandoffBinding.UPSTREAM_PR);
        assertTrue(NebulaM3ApacheHandoffBinding.UPSTREAM_COMMIT.matches("[0-9a-f]{40}"));
        assertTrue(NebulaM3ApacheHandoffBinding.MANIFEST_SHA256.matches("[0-9a-f]{64}"));
        assertEquals("Apache-2.0", NebulaM3ApacheHandoffBinding.SOURCE_LICENSE);
        assertEquals("EPL-2.0", NebulaM3ApacheHandoffBinding.NEBULA_RETAINED_LICENSE);
        assertEquals("SOURCE_MERGED_PROOF_PENDING", NebulaM3ApacheHandoffBinding.DELIVERY_STATE);
        assertEquals("QUALIFICATION_INPUT_ONLY", NebulaM3ApacheHandoffBinding.TARGET_ROLE);
        assertFalse(NebulaM3ApacheHandoffBinding.automaticApplication());
        assertFalse(NebulaM3ApacheHandoffBinding.targetRelicenseAuthority());
        assertFalse(NebulaM3ApacheHandoffBinding.sourceMutationAuthority());
        assertFalse(NebulaM3ApacheHandoffBinding.promotionAuthority());
    }

    @Test
    void checkedInLedgerMatchesBindingOwnerExactly() throws Exception {
        Path ledger = repositoryRoot().resolve("m3/catalogue/apache-handoff-binding.tsv");
        assertTrue(Files.isRegularFile(ledger));
        assertEquals(NebulaM3ApacheHandoffBinding.tsv(), Files.readString(ledger));
    }

    @Test
    void driftFailsClosed() {
        NebulaM3ApacheHandoffBinding.requireExact(
                NebulaM3ApacheHandoffBinding.UPSTREAM_REPOSITORY,
                NebulaM3ApacheHandoffBinding.UPSTREAM_COMMIT,
                NebulaM3ApacheHandoffBinding.MANIFEST_SHA256,
                NebulaM3ApacheHandoffBinding.SOURCE_LICENSE,
                NebulaM3ApacheHandoffBinding.NEBULA_RETAINED_LICENSE);

        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaM3ApacheHandoffBinding.requireExact(
                        NebulaM3ApacheHandoffBinding.UPSTREAM_REPOSITORY,
                        "0".repeat(40),
                        NebulaM3ApacheHandoffBinding.MANIFEST_SHA256,
                        NebulaM3ApacheHandoffBinding.SOURCE_LICENSE,
                        NebulaM3ApacheHandoffBinding.NEBULA_RETAINED_LICENSE));
    }

    @Test
    void repositoryLicenseRemainsEpl2() throws Exception {
        String license = Files.readString(repositoryRoot().resolve("LICENSE"));
        assertTrue(license.startsWith("Eclipse Public License - v 2.0"));
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("pom.xml"))
                    && Files.isDirectory(current.resolve("widgets"))
                    && Files.isDirectory(current.resolve("m3"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Nebula repository root not found");
    }
}
