// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import com.synexia.rewrite.M3RecipeMasteryPortableReceipt;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Target-side reader for one pinned, authority-free Synexia recipe-mastery receipt. */
final class A3Mastery {

    private A3Mastery() {
    }

    static M3RecipeMasteryPortableReceipt.Verified verify(
            Path root,
            Path receipt,
            String expectedRoot) throws IOException {
        Path checkedRoot = A3Fs.root(root);
        Path file = A3Fs.source(checkedRoot, Objects.requireNonNull(receipt, "receipt"));
        if (!Files.isRegularFile(file)) {
            throw new IOException("missing A3 mastery receipt: " + file);
        }
        String tsv = Files.readString(file, StandardCharsets.UTF_8);
        return A3RecipeHome.verifyMastery(tsv, expectedRoot);
    }
}
