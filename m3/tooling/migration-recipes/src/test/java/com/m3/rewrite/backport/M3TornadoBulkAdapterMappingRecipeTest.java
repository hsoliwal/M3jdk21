// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3TornadoBulkAdapterMappingRecipeTest {
    @Test
    void addsProviderOwnerAndCatalogueRowThenReachesFixedPoint() {
        PlainText map =
                text(
                        "m3/docs/name-mapping.json",
                        """
                        {
                          "family_name_mapping": [
                            {
                              "source_family": "Synexia BulkTask/BulkExecutor/M3BulkExecutor + TornadoVM verified RV32IM bulk provider",
                              "target_family": "M3 internal bulk scheduler",
                              "target_owners": [
                                "jdk.internal.vm.parallel.BulkTask",
                                "jdk.internal.vm.parallel.BulkExecutor",
                                "jdk.internal.vm.parallel.BulkExecution"
                              ],
                              "rule": "provider-specific proof"
                            }
                          ]
                        }
                        """);
        PlainText catalogue =
                text(
                        "m3/tooling/recipe-catalogue.tsv",
                        "recipe_id\tclass\tscope\tcontract\tstatus\tverification\n");

        var first =
                new M3TornadoBulkAdapterMappingRecipe()
                        .run(
                                new InMemoryLargeSourceSet(List.of(map, catalogue)),
                                new InMemoryExecutionContext(),
                                1);
        assertEquals(2, first.getChangeset().getAllResults().size());
        List<org.openrewrite.SourceFile> after =
                first.getChangeset().getAllResults().stream()
                        .map(result -> result.getAfter())
                        .toList();
        assertTrue(after.stream().anyMatch(file -> file.printAll().contains(
                "com.m3.tornado.Rv32iBulkExecutorAdapter")));
        assertTrue(after.stream().anyMatch(file -> file.printAll().contains(
                "m3-tornadovm-bulk-adapter")));

        var second =
                new M3TornadoBulkAdapterMappingRecipe()
                        .run(
                                new InMemoryLargeSourceSet(after),
                                new InMemoryExecutionContext(),
                                1);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
        assertFalse(new M3TornadoBulkAdapterMappingRecipe().promotionAuthority());
    }

    private static PlainText text(String path, String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }
}
