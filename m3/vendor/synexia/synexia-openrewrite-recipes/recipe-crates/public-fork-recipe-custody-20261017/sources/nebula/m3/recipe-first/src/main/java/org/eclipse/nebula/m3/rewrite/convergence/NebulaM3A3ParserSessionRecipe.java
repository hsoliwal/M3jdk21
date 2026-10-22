// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import java.util.List;
import org.eclipse.nebula.m3.rewrite.exact.NebulaM3ExactJavaSnapshotRecipe;
import org.openrewrite.Recipe;

/** Exact upgrade of the existing serial laboratory; all compiler/behavior gates stay independent. */
public final class NebulaM3A3ParserSessionRecipe extends Recipe {
    @Override public String getDisplayName() { return "Reuse a bounded A3 fixture parser"; }
    @Override public String getDescription() {
        return "Replays only the reviewed lab and its discriminator test through the existing exact Java owner.";
    }
    @Override public List<Recipe> getRecipeList() { return List.of(new Lab(), new LabTest()); }

    private static final class Lab extends NebulaM3ExactJavaSnapshotRecipe {
        @Override public String getDisplayName() { return "Upgrade A3 Lab parser session proof"; }
        @Override public String getDescription() { return "Apply the exact reviewed pre/post Java image."; }
        @Override protected String repositoryPath() { return "m3/recipe-first/src/main/java/org/eclipse/nebula/m3/NebulaM3A3Lab.java"; }
        @Override protected String moduleRelativePath() { return "src/main/java/org/eclipse/nebula/m3/NebulaM3A3Lab.java"; }
        @Override protected String beforeSha256() { return "22367b0de2c62fbea056dc245572d5663f1f827d1a5e9549fb54afc0fe6406af"; }
        @Override protected String afterSha256() { return "7a79e69ebc297d96dae4ca48c1960e5541f37e4036c5e60569a865a1553ba23f"; }
        @Override protected String afterResource() {
            return "/org/eclipse/nebula/m3/rewrite/convergence/a3-parser-session/NebulaM3A3Lab.after.java.txt";
        }
    }

    private static final class LabTest extends NebulaM3ExactJavaSnapshotRecipe {
        @Override public String getDisplayName() { return "Upgrade A3 LabTest parser session proof"; }
        @Override public String getDescription() { return "Apply the exact reviewed pre/post Java image."; }
        @Override protected String repositoryPath() { return "m3/recipe-first/src/test/java/org/eclipse/nebula/m3/NebulaM3A3LabTest.java"; }
        @Override protected String moduleRelativePath() { return "src/test/java/org/eclipse/nebula/m3/NebulaM3A3LabTest.java"; }
        @Override protected String beforeSha256() { return "02a00ab8c02d13d7c148e591e042a8b3de0be4609284373243b9e23794955968"; }
        @Override protected String afterSha256() { return "811aa8359febc385dd05b732e482a45e1c14469f7ff9fadace349b0143dc9c96"; }
        @Override protected String afterResource() {
            return "/org/eclipse/nebula/m3/rewrite/convergence/a3-parser-session/NebulaM3A3LabTest.after.java.txt";
        }
    }
}
