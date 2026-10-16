// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class A3CompleteTest {
    @TempDir Path root;

    @Test
    void emitsDeterministicPreparationFrontierAndBroaderScopeHold() throws Exception {
        Path inventory = write(
                "m3/build/a3/inventory.tsv",
                """
                tree	module	area	path	kind	bytes	sha256
                src	java.base	share	src/java.base/share/classes/example/A.java	JAVA	10	%s
                src	hotspot	share	src/hotspot/share/runtime/a3.cpp	NATIVE	20	%s
                """.formatted("a".repeat(64), "b".repeat(64)));
        Path queue = write(
                "m3/build/backports/COMPATIBILITY_QUEUE.tsv",
                header()
                        + "\n"
                        + row(0, "MULTI_MODULE",
                                "src/java.base/share/classes/example/A.java,"
                                        + "src/hotspot/share/runtime/a3.cpp")
                        + "\n"
                        + row(1, "FILE", "src/java.base/share/classes/example/A.java")
                        + "\n");

        Path out = Path.of("m3/build/a3/completion");
        A3Complete.Result first = A3Complete.write(root, inventory, queue, out);
        String receipt = Files.readString(root.resolve(out).resolve("receipt.tsv"));
        String javaPaths =
                Files.readString(root.resolve(out).resolve("java-preparation-paths.txt"));
        String holds = Files.readString(root.resolve(out).resolve("scope-holds.tsv"));

        assertEquals(3, first.workRows());
        assertEquals(1, first.javaPreparationPaths());
        assertEquals(1, first.broaderScopeHolds());
        assertEquals("src/java.base/share/classes/example/A.java\n", javaPaths);
        assertTrue(holds.contains("\tMULTI_MODULE\t"));
        assertFalse(holds.contains("\tFILE\t"));
        assertTrue(receipt.contains("\tfalse\tfalse\n"));
        assertTrue(receipt.contains(first.receiptRoot()));

        A3Complete.Result replay = A3Complete.write(root, inventory, queue, out);
        assertEquals(first, replay);
    }

    @Test
    void cliCompleteConsumesExplicitInventoryAndQueue() throws Exception {
        Path inventory = write(
                "m3/build/a3/inventory.tsv",
                """
                tree\tmodule\tarea\tpath\tkind\tbytes\tsha256
                src\tjava.base\tshare\tsrc/java.base/share/classes/example/A.java\tJAVA\t10\t%s
                """.formatted("a".repeat(64)));
        Path queue = write(
                "m3/build/backports/COMPATIBILITY_QUEUE.tsv",
                header()
                        + "\n"
                        + row(0, "FILE", "src/java.base/share/classes/example/A.java")
                        + "\n");

        A3.main(
                new String[] {
                    "complete",
                    "--root", root.toString(),
                    "--inventory", root.relativize(inventory).toString(),
                    "--queue", root.relativize(queue).toString(),
                    "--out", "m3/build/a3/cli-completion"
                });

        assertTrue(Files.isRegularFile(
                root.resolve("m3/build/a3/cli-completion/receipt.tsv")));
        assertEquals(
                "src/java.base/share/classes/example/A.java\n",
                Files.readString(
                        root.resolve(
                                "m3/build/a3/cli-completion/java-preparation-paths.txt")));
    }

    @Test
    void outputCannotEscapeM3Build() throws Exception {
        Path inventory = write(
                "m3/build/a3/inventory.tsv",
                "tree\tmodule\tarea\tpath\tkind\tbytes\tsha256\n");
        Path queue = write(
                "m3/build/backports/COMPATIBILITY_QUEUE.tsv",
                header() + "\n");

        assertThrows(
                IllegalArgumentException.class,
                () -> A3Complete.write(
                        root,
                        inventory,
                        queue,
                        Path.of("src/completion")));
    }

    private Path write(String relative, String content) throws Exception {
        Path path = root.resolve(relative);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path;
    }

    private static String header() {
        return "order\trelease\tcommit\tjbs_ids\tsubject\tdomain\t"
                + "inventory_disposition\trisk\tscope_floor\tproof_lane\t"
                + "recipe_strategy\tpriority\tcompatibility_state\t"
                + "next_action\tpaths";
    }

    private static String row(int order, String scope, String paths) {
        return order
                + "\t27\t"
                + (order == 0 ? "1" : "2").repeat(40)
                + "\tJDK-"
                + (200 + order)
                + "\tFeature "
                + order
                + "\tcore-libs\tcandidate\tMEDIUM\t"
                + scope
                + "\tCORE_LIBRARY\tOPENREWRITE_OR_HASH_PINNED_JAVA\t35\t"
                + "PENDING_COMPATIBILITY_PROOF\tPROVE_CORE_LIBRARY_BACKPORT\t"
                + paths;
    }
}
