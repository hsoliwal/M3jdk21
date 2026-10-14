// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class A3WorkTest {

    @TempDir
    Path root;

    @Test
    void expandsQueueIntoSerialFileAtomsWithoutWeakeningJoinScope()
            throws Exception {
        Path inventory = writeInventory();
        Path queue = writeQueue(false);

        List<A3Work.Row> rows = A3Work.load(root, inventory, queue);

        assertEquals(5, rows.size());
        A3Work.Row make = require(rows, "make/autoconf/flags.m4");
        A3Work.Row java =
                require(
                        rows,
                        "src/java.base/share/classes/example/A.java");
        A3Work.Row nativeRow =
                require(rows, "src/hotspot/share/runtime/a3.cpp");
        A3Work.Row added =
                require(rows, "test/jdk/example/NewTest.java");
        A3Work.Row feature =
                rows.stream()
                        .filter(
                                row ->
                                        row.targetState()
                                                == A3Work.TargetState.FEATURE)
                        .findFirst()
                        .orElseThrow();

        assertEquals(A3Work.TargetState.OUTSIDE_A3, make.targetState());
        assertEquals(
                A3Work.PrepareLane.OUTSIDE_A3_REVIEW,
                make.prepareLane());

        assertEquals(A3Work.TargetState.PRESENT, java.targetState());
        assertEquals(A3Inv.Kind.JAVA, java.kind());
        assertEquals(
                A3Work.PrepareLane.A3_JAVA_ATOMIZE_PATTERNIZE,
                java.prepareLane());

        assertEquals(A3Work.TargetState.PRESENT, nativeRow.targetState());
        assertEquals(A3Inv.Kind.NATIVE, nativeRow.kind());
        assertEquals(
                A3Work.PrepareLane.NATIVE_ATOM_REVIEW,
                nativeRow.prepareLane());

        assertEquals(A3Work.TargetState.ABSENT, added.targetState());
        assertEquals(
                A3Work.PrepareLane.ADD_JAVA_REVIEW,
                added.prepareLane());

        assertEquals(A3Work.PrepareLane.FEATURE_REVIEW, feature.prepareLane());
        assertEquals("", feature.path());

        for (A3Work.Row row : rows) {
            assertEquals("FILE", row.atomScope());
        }
        assertEquals("MULTI_MODULE", java.joinScope());
        assertEquals("MULTI_MODULE", nativeRow.joinScope());

        assertEquals(
                List.of("src/java.base/share/classes/example/A.java"),
                A3Work.javaPreparationPaths(rows));
    }

    @Test
    void outputIsDeterministicAndLivesOnlyUnderM3Build() throws Exception {
        Path inventory = writeInventory();
        Path queue = writeQueue(false);
        Path output = Path.of("m3/build/a3/work.tsv");

        A3Work.write(root, inventory, queue, output);
        String first = Files.readString(root.resolve(output));
        A3Work.write(root, inventory, queue, output);
        String replay = Files.readString(root.resolve(output));

        assertEquals(first, replay);
        assertTrue(first.startsWith("feature_order\tfile_order\trelease"));
        assertTrue(first.contains("A3_JAVA_ATOMIZE_PATTERNIZE"));
        assertTrue(first.contains("\tMULTI_MODULE\t"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        A3Work.write(
                                root,
                                inventory,
                                queue,
                                Path.of("src/work.tsv")));
    }

    @Test
    void directRowsCanonicalizeEvidenceCoordinates() {
        A3Work.Row row =
                new A3Work.Row(
                        3,
                        2,
                        27,
                        "A".repeat(40),
                        " JDK-1 ",
                        " Subject ",
                        " core-libs ",
                        " MEDIUM ",
                        "MULTI_MODULE",
                        " CORE_LIBRARY ",
                        " OPENREWRITE_OR_HASH_PINNED_JAVA ",
                        35,
                        " PENDING_COMPATIBILITY_PROOF ",
                        " PROVE ",
                        "src\\java.base\\share\\classes\\A.java",
                        A3Work.TargetState.PRESENT,
                        A3Inv.Kind.JAVA,
                        "B".repeat(64),
                        A3Work.PrepareLane.A3_JAVA_ATOMIZE_PATTERNIZE,
                        "FILE");

        assertEquals("a".repeat(40), row.commit());
        assertEquals("src/java.base/share/classes/A.java", row.path());
        assertEquals("b".repeat(64), row.targetSha256());
        assertEquals("JDK-1", row.jbsIds());
        assertEquals("Subject", row.subject());
    }

    @Test
    void queueDriftAndDuplicatePathsFailClosed() throws Exception {
        Path inventory = writeInventory();
        Path duplicateQueue = writeQueue(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> A3Work.load(root, inventory, duplicateQueue));

        Path badOrder =
                write(
                        "m3/build/backports/BAD_QUEUE.tsv",
                        queueHeader()
                                + "\n"
                                + queueRow(
                                        2,
                                        "src/java.base/share/classes/example/A.java")
                                + "\n"
                                + queueRow(
                                        1,
                                        "src/hotspot/share/runtime/a3.cpp")
                                + "\n");
        assertThrows(
                java.io.IOException.class,
                () -> A3Work.load(root, inventory, badOrder));
    }

    @Test
    void cliWorkCommandConsumesExplicitPrecomputedInputs() throws Exception {
        Path inventory = writeInventory();
        Path queue = writeQueue(false);
        Path out = Path.of("m3/build/a3/cli-work.tsv");

        A3.main(
                new String[] {
                    "work",
                    "--root",
                    root.toString(),
                    "--inventory",
                    root.relativize(inventory).toString(),
                    "--queue",
                    root.relativize(queue).toString(),
                    "--out",
                    out.toString()
                });

        String tsv = Files.readString(root.resolve(out));
        assertTrue(tsv.contains("NATIVE_ATOM_REVIEW"));
        assertTrue(tsv.contains("OUTSIDE_A3_REVIEW"));
    }

    private Path writeInventory() throws Exception {
        String inventory =
                """
                tree	module	area	path	kind	bytes	sha256
                src	java.base	share	src/java.base/share/classes/example/A.java	JAVA	10	%s
                src	hotspot	share	src/hotspot/share/runtime/a3.cpp	NATIVE	20	%s
                """
                        .formatted("a".repeat(64), "b".repeat(64));
        return write("m3/build/a3/inventory.tsv", inventory);
    }

    private Path writeQueue(boolean duplicate) throws Exception {
        String paths =
                duplicate
                        ? "src/java.base/share/classes/example/A.java,"
                                + "src/java.base/share/classes/example/A.java"
                        : "src/hotspot/share/runtime/a3.cpp,"
                                + "make/autoconf/flags.m4,"
                                + "test/jdk/example/NewTest.java,"
                                + "src/java.base/share/classes/example/A.java";
        String queue =
                queueHeader()
                        + "\n"
                        + queueRow(0, paths)
                        + "\n"
                        + queueRow(1, "")
                        + "\n";
        return write("m3/build/backports/COMPATIBILITY_QUEUE.tsv", queue);
    }

    private static String queueHeader() {
        return "order\trelease\tcommit\tjbs_ids\tsubject\tdomain\t"
                + "inventory_disposition\trisk\tscope_floor\tproof_lane\t"
                + "recipe_strategy\tpriority\tcompatibility_state\t"
                + "next_action\tpaths";
    }

    private static String queueRow(int order, String paths) {
        return order
                + "\t27\t"
                + (order == 0 ? "1" : "2").repeat(40)
                + "\tJDK-"
                + (100 + order)
                + "\tFeature "
                + order
                + "\tcore-libs\tcandidate\tMEDIUM\tMULTI_MODULE\t"
                + "CORE_LIBRARY\tMIXED_PACKET_OPENREWRITE_PLUS_VERBATIM\t35\t"
                + "PENDING_COMPATIBILITY_PROOF\tPROVE_CORE_LIBRARY_BACKPORT\t"
                + paths;
    }

    private Path write(String relative, String content) throws Exception {
        Path path = root.resolve(relative);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path;
    }

    private static A3Work.Row require(
            List<A3Work.Row> rows,
            String path) {
        return rows.stream()
                .filter(row -> row.path().equals(path))
                .findFirst()
                .orElseThrow();
    }
}
