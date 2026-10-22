// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.util.LinkedHashMap;
import java.util.Map;

/** Exact source-image manifest for Nebula method-signal evidence. */
final class NebulaM3MethodSignalEvidenceManifest {

    record Target(
            String before,
            String after,
            String beforeResource,
            String afterResource,
            boolean required) {}

    private NebulaM3MethodSignalEvidenceManifest() {}

    static Map<String, Target> targets() {
        LinkedHashMap<String, Target> result = new LinkedHashMap<>();
        existing(result,
                "src/main/java/org/eclipse/nebula/m3/NebulaM3InventoryRecipe.java",
                "2ee89377d788b4bb8a603f9c8c8d01c7cac75caa",
                "3c822f858a925a75074639c34a73f376554e5534",
                "NebulaM3InventoryRecipe.java");
        existing(result,
                "src/main/java/org/eclipse/nebula/m3/NebulaM3InventoryCli.java",
                "201cc6f670a13e3c519156eac088cb303b96bfc5",
                "9fb866368e057bf53f6d1b872a836ba5a3a9293c",
                "NebulaM3InventoryCli.java");
        existing(result,
                "src/main/java/org/eclipse/nebula/m3/NebulaM3FastSearchReviewPolicy.java",
                "ac43d4d08e273d8715ea7afb1bcdc8b8e7450fdc",
                "348d7c2ea188db165626763396de7a7c1cd6ba58",
                "NebulaM3FastSearchReviewPolicy.java");
        existing(result,
                "src/main/java/org/eclipse/nebula/m3/NebulaM3FastSearchReviewRecipe.java",
                "bd01f6398e95532d439f40cc672fef98f1c3464b",
                "6cf6ef10820f5c659c3730d260e0270dfdb307c6",
                "NebulaM3FastSearchReviewRecipe.java");
        existing(result,
                "src/main/java/org/eclipse/nebula/m3/NebulaM3JavaBeforeJniPolicy.java",
                "1617a9c15e365f27634b0bfa3eb58be201305194",
                "561207c038d6cda57301f353c9d05226cd1eafa5",
                "NebulaM3JavaBeforeJniPolicy.java");
        existing(result,
                "src/main/java/org/eclipse/nebula/m3/NebulaM3JavaBeforeJniReviewRecipe.java",
                "6fa6e9964c1304cc84c66ab394da4f6402041b4f",
                "b7c853ae932f80f04413b9ea46cebf8218400870",
                "NebulaM3JavaBeforeJniReviewRecipe.java");
        existing(result,
                "src/test/java/org/eclipse/nebula/m3/NebulaM3InventoryRecipeTest.java",
                "e5bc832919f551f535a3406facf6cd51436b0de0",
                "7f1776df0cd42532685a02432f8f340423cb17e5",
                "NebulaM3InventoryRecipeTest.java");
        existing(result,
                "src/test/java/org/eclipse/nebula/m3/NebulaM3RepositoryReviewRecipeTest.java",
                "261e559fefab8ab65785c4f04d65d53ea6923ed8",
                "3b0b268ffa5b36659e9d78806a3d8f6b1d63128d",
                "NebulaM3RepositoryReviewRecipeTest.java");
        additive(result,
                "src/main/java/org/eclipse/nebula/m3/NebulaM3MethodInventoryTable.java",
                "beeef99ab20a3ec1a5c41cddd2f13349c8720255",
                "NebulaM3MethodInventoryTable.java");
        additive(result,
                "src/main/java/org/eclipse/nebula/m3/NebulaM3MethodFastSearchReviewTable.java",
                "88e47a170799028c2159b106eaed71564751ceb6",
                "NebulaM3MethodFastSearchReviewTable.java");
        additive(result,
                "src/main/java/org/eclipse/nebula/m3/NebulaM3MethodJavaBeforeJniReviewTable.java",
                "e65c0ec479f1ccb51a3e9855a79d5955088080a4",
                "NebulaM3MethodJavaBeforeJniReviewTable.java");
        return Map.copyOf(result);
    }

    private static void existing(
            Map<String, Target> result,
            String path,
            String before,
            String after,
            String file) {
        result.put(
                path,
                new Target(
                        before,
                        after,
                        "before/" + file,
                        "after/" + file,
                        true));
    }

    private static void additive(
            Map<String, Target> result,
            String path,
            String after,
            String file) {
        result.put(
                path,
                new Target(
                        "ABSENT",
                        after,
                        null,
                        "after/" + file,
                        false));
    }
}
