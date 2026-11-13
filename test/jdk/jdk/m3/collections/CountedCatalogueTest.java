/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import jdk.m3.collections.tooling.M3CountedCatalogue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Owning-module test of the restored receiver ledger query (fold-close audit leaf L0054, PR #1084): the restored
 * catalogue.tsv is read whole, a literal query returns exactly its matching rows, and malformed rows or arguments refuse.
 */
//FLLDCIM: MRL - fold-close audit leaf L0054: NEW test of this recipe (the origin shipped no test of the main); recipe make/m3/collections/qualification/foldaudit-collections-20261107/apply.py v1
final class CountedCatalogueTest {
    private static final String LEDGER = "counts/catalogue.tsv";

    @Test void literalQueryReturnsOnlyMatchingRows(@TempDir Path dir) throws IOException {
        Path ledger = restoredLedger(dir);
        List<String> all = run(ledger.toString());
        List<String> lanes = run(ledger.toString(), "frequency.aligned-batch-algebra");
        assertEquals(18, all.size(), "every non-comment row of the restored ledger");
        assertEquals(2, lanes.size(), "M3CountLanes and m3_count_lanes.c rows");
        assertTrue(lanes.stream().allMatch(row -> row.contains("frequency.aligned-batch-algebra")));
        assertTrue(run(ledger.toString(), "no such category").isEmpty());
    }

    @Test void malformedRowsAndArgumentsRefuse(@TempDir Path dir) throws IOException {
        Path narrow = dir.resolve("narrow.tsv");
        Files.writeString(narrow, "a\tb\tc\n", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> M3CountedCatalogue.main(new String[] {narrow.toString()}));
        Path blank = dir.resolve("blank.tsv");
        Files.writeString(blank, "a\tb\tc\td\te\tf\tg\t\n", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> M3CountedCatalogue.main(new String[] {blank.toString()}));
        assertThrows(IllegalArgumentException.class, () -> M3CountedCatalogue.main(new String[0]));
        assertThrows(IllegalArgumentException.class, () -> M3CountedCatalogue.main(new String[] {"a", "b", "c"}));
    }

    private static Path restoredLedger(Path dir) throws IOException {
        Path ledger = dir.resolve("catalogue.tsv");
        try (InputStream in = CountedCatalogueTest.class.getResourceAsStream(LEDGER)) {
            if (in == null) throw new IOException("restored ledger resource missing: " + LEDGER);
            Files.copy(in, ledger);
        }
        return ledger;
    }

    private static List<String> run(String... args) throws IOException {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            M3CountedCatalogue.main(args);
        } finally {
            System.setOut(original);
        }
        String text = captured.toString(StandardCharsets.UTF_8);
        return text.isEmpty() ? List.of() : List.of(text.split("\\R"));
    }
}
