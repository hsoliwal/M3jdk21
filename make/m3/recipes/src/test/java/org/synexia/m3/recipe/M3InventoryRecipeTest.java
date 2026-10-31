/*
 * Copyright (c) 2026.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.synexia.m3.recipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class M3InventoryRecipeTest {
    private M3InventoryRecipeTest() { }

    public static void main(String[] args) throws Exception {
        final Path root = Files.createTempDirectory("m3inventory");
        try {
            final Path file = root.resolve("Case.java");
            Files.writeString(file, """
                    class Case {
                        String s = "Arrays.copyOf(a, 2); key ^= value; counts.merge(k,1,f);";
                        // Something.filter(x -> true); counts.containsKey(x); 
                        // key ^= 42;
                        String t = """ + "\"\"\"\n"
                        + "         .substring(0, 4)\n"
                        + "         \"\"\";\n"
                        + "    void x() {\n"
                        + "        int[] array = new int[3];\n"
                        + "        int xor = 0; xor ^= 1;\n"
                        + "        java.util.Map.of().containsKey(3);\n"
                        + "        System.arraycopy(array, 0, array, 1, 2);\n"
                        + "    }\n"
                        + "}\n");
            final Path output = root.resolve("inventory.jsonl");
            M3InventoryRecipe.inventory(root, output);
            final String first = Files.readString(output);
            require(first.contains("\"category\":\"dynamic_arrays\""), "array category");
            require(first.contains("\"kind\":\"METHOD\""), "method atom");
            require(first.contains("\"category\":\"xor_candidate\""),
                    "XOR node category");
            require(first.contains("\"category\":\"membership_candidate\""),
                    "AST membership category");
            require(!first.contains("\"category\":\"frequency_candidate\""),
                    "frequency token is in a string, not code");
            require(!first.contains("\"category\":\"filtered_views\""),
                    "comments are not program AST");
            require(!first.contains("\"category\":\"composite_strings\""),
                    "strings are not program AST");
            M3InventoryRecipe.inventory(root, output);
            require(first.equals(Files.readString(output)), "recipe fixed point");
            Files.writeString(file, "class Case { void broken( { }");
            try {
                M3InventoryRecipe.inventory(root, output);
                throw new AssertionError("Expected parse error");
            } catch (IOException expected) {
                require(first.equals(Files.readString(output)),
                        "failed inventory must not overwrite baseline");
            }
            System.out.println("M3InventoryRecipeTest PASS");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted((a, b) -> b.compareTo(a)).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void require(boolean truth, String detail) {
        if (!truth) {
            throw new AssertionError(detail);
        }
    }
}
