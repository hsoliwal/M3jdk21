/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.synexia.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Maven/Surefire admission gate; no replacement for donor compilation and runtime proof. */
final class M3FiveProviderInventoryTest {
    private static final String[] PROVIDERS =
            {"GUAVA", "ECLIPSE", "APACHE", "SPRING", "JDK"};

    @Test void allPinnedSourceBlobsPresent() throws IOException {
        assertEquals(3214, M3FiveProviderInventoryProof.verify(Path.of("file-census")));
    }

    @Test void refuseDriftedSourceReceipt() throws IOException {
        Path root = Files.createTempDirectory("m3-five-provider-census");
        try {
            for (String provider : PROVIDERS) {
                Path file = Path.of("file-census", provider + "_INVENTORY.tsv");
                Files.copy(file, root.resolve(file.getFileName()));
            }
            Path corrupted = root.resolve("GUAVA_INVENTORY.tsv");
            Files.writeString(corrupted, Files.readString(corrupted) + "\n");
            assertThrows(IOException.class, () -> M3FiveProviderInventoryProof.verify(root));
        } finally {
            for (String provider : PROVIDERS) {
                Files.deleteIfExists(root.resolve(provider + "_INVENTORY.tsv"));
            }
            Files.deleteIfExists(root);
        }
    }
}
