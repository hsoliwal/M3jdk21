// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class A3InvTest {

    @TempDir
    Path root;

    @Test
    void inventoryIsStableAcrossProductNativeTestAndResourceTrees()
            throws Exception {
        write(
                "src/java.base/share/classes/example/A.java",
                "package example; final class A {}\n");
        write(
                "src/hotspot/share/runtime/a3_probe.cpp",
                "int a3_probe() { return 1; }\n");
        write(
                "src/java.base/share/classes/example/a3.properties",
                "a=b\n");
        write(
                "test/jdk/java/lang/A3Test.java",
                "class A3Test {}\n");

        List<A3Inv.Row> first = A3Inv.scan(root);
        List<A3Inv.Row> replay = A3Inv.scan(root);

        assertEquals(first, replay);
        assertEquals(4, first.size());
        assertTrue(
                first.stream()
                        .anyMatch(
                                row ->
                                        row.path()
                                                        .equals(
                                                                "src/java.base/share/classes/example/A.java")
                                                && row.module().equals("java.base")
                                                && row.area().equals("share")
                                                && row.kind() == A3Inv.Kind.JAVA));
        assertTrue(
                first.stream()
                        .anyMatch(
                                row ->
                                        row.path()
                                                        .equals(
                                                                "src/hotspot/share/runtime/a3_probe.cpp")
                                                && row.kind() == A3Inv.Kind.NATIVE));
        assertTrue(
                first.stream()
                        .anyMatch(
                                row ->
                                        row.path()
                                                        .equals(
                                                                "test/jdk/java/lang/A3Test.java")
                                                && row.tree().equals("test")
                                                && row.module().equals("jdk")));

        A3Inv.write(root, Path.of("m3/build/a3/inventory.tsv"));
        String tsv =
                Files.readString(
                        root.resolve("m3/build/a3/inventory.tsv"));
        assertTrue(tsv.startsWith("tree\tmodule\tarea\tpath\tkind"));
        assertTrue(tsv.contains("src/java.base/share/classes/example/A.java"));
    }

    private void write(String path, String content) throws Exception {
        Path target = root.resolve(path);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }
}
