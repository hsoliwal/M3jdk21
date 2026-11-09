// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/table/SourcesFileResults.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.table;

import org.openrewrite.*;
import org.openrewrite.config.RecipeDescriptor;
import java.util.Stack;

public class SourcesFileResults extends DataTable<SourcesFileResults.Row> {
    public SourcesFileResults(Recipe recipe) {
        super(recipe, "Source files that had results", "Source files that were modified by the recipe run.");
    }


    public static final class Row {
        @Column(displayName = "Source path before the run", description = "The source path of the file before the run.")
        private final String sourcePath;
        @Column(displayName = "Source path after the run", description = "A recipe may modify the source path. This is the path after the run.")
        private final String afterSourcePath;
        @Column(displayName = "Parent of the recipe that made changes", description = "In a hierarchical recipe, the parent of the recipe that made a change. Empty if " + "this is the root of a hierarchy or if the recipe is not hierarchical at all.")
        private final String parentRecipe;
        @Column(displayName = "Recipe that made changes", description = "The specific recipe that made a change.")
        private final String recipe;
        @Column(displayName = "Estimated time saving", description = "An estimated effort that a developer to fix manually instead of using this recipe," + " in unit of seconds.")
        private final Long estimatedTimeSaving;
        @Column(displayName = "Cycle", description = "The recipe cycle in which the change was made.")
        private final int cycle;

        public Row(final String sourcePath, final String afterSourcePath, final String parentRecipe, final String recipe, final Long estimatedTimeSaving, final int cycle) {
            this.sourcePath = sourcePath;
            this.afterSourcePath = afterSourcePath;
            this.parentRecipe = parentRecipe;
            this.recipe = recipe;
            this.estimatedTimeSaving = estimatedTimeSaving;
            this.cycle = cycle;
        }

        public String getSourcePath() {
            return this.sourcePath;
        }

        public String getAfterSourcePath() {
            return this.afterSourcePath;
        }

        public String getParentRecipe() {
            return this.parentRecipe;
        }

        public String getRecipe() {
            return this.recipe;
        }

        public Long getEstimatedTimeSaving() {
            return this.estimatedTimeSaving;
        }

        public int getCycle() {
            return this.cycle;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof SourcesFileResults.Row)) return false;
            final SourcesFileResults.Row other = (SourcesFileResults.Row) o;
            if (this.getCycle() != other.getCycle()) return false;
            final Object this$estimatedTimeSaving = this.getEstimatedTimeSaving();
            final Object other$estimatedTimeSaving = other.getEstimatedTimeSaving();
            if (this$estimatedTimeSaving == null ? other$estimatedTimeSaving != null : !this$estimatedTimeSaving.equals(other$estimatedTimeSaving)) return false;
            final Object this$sourcePath = this.getSourcePath();
            final Object other$sourcePath = other.getSourcePath();
            if (this$sourcePath == null ? other$sourcePath != null : !this$sourcePath.equals(other$sourcePath)) return false;
            final Object this$afterSourcePath = this.getAfterSourcePath();
            final Object other$afterSourcePath = other.getAfterSourcePath();
            if (this$afterSourcePath == null ? other$afterSourcePath != null : !this$afterSourcePath.equals(other$afterSourcePath)) return false;
            final Object this$parentRecipe = this.getParentRecipe();
            final Object other$parentRecipe = other.getParentRecipe();
            if (this$parentRecipe == null ? other$parentRecipe != null : !this$parentRecipe.equals(other$parentRecipe)) return false;
            final Object this$recipe = this.getRecipe();
            final Object other$recipe = other.getRecipe();
            if (this$recipe == null ? other$recipe != null : !this$recipe.equals(other$recipe)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + this.getCycle();
            final Object $estimatedTimeSaving = this.getEstimatedTimeSaving();
            result = result * PRIME + ($estimatedTimeSaving == null ? 43 : $estimatedTimeSaving.hashCode());
            final Object $sourcePath = this.getSourcePath();
            result = result * PRIME + ($sourcePath == null ? 43 : $sourcePath.hashCode());
            final Object $afterSourcePath = this.getAfterSourcePath();
            result = result * PRIME + ($afterSourcePath == null ? 43 : $afterSourcePath.hashCode());
            final Object $parentRecipe = this.getParentRecipe();
            result = result * PRIME + ($parentRecipe == null ? 43 : $parentRecipe.hashCode());
            final Object $recipe = this.getRecipe();
            result = result * PRIME + ($recipe == null ? 43 : $recipe.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "SourcesFileResults.Row(sourcePath=" + this.getSourcePath() + ", afterSourcePath=" + this.getAfterSourcePath() + ", parentRecipe=" + this.getParentRecipe() + ", recipe=" + this.getRecipe() + ", estimatedTimeSaving=" + this.getEstimatedTimeSaving() + ", cycle=" + this.getCycle() + ")";
        }
    }

    public static SourcesFileResults build(Changeset changeset, int cycle, ExecutionContext ctx) {
        SourcesFileResults resultsTable = new SourcesFileResults(Recipe.noop());
        for (Result result : changeset.getAllResults()) {
            Stack<RecipeDescriptor[]> recipeStack = new Stack<>();
            for (RecipeDescriptor rd : result.getRecipeDescriptorsThatMadeChanges()) {
                recipeStack.push(new RecipeDescriptor[] {null, rd});
            }
            while (!recipeStack.isEmpty()) {
                RecipeDescriptor[] recipeThatMadeChange = recipeStack.pop();
                resultsTable.insertRow(ctx, new SourcesFileResults.Row(result.getBefore() == null ? "" : result.getBefore().getSourcePath().toString(), result.getAfter() == null ? "" : result.getAfter().getSourcePath().toString(), recipeThatMadeChange[0] == null ? "" : recipeThatMadeChange[0].getName(), recipeThatMadeChange[1].getName(), result.getTimeSavings() == null ? 0 : result.getTimeSavings().getSeconds(), cycle));
                for (int i = recipeThatMadeChange[1].getRecipeList().size() - 1; i >= 0; i--) {
                    RecipeDescriptor rd = recipeThatMadeChange[1].getRecipeList().get(i);
                    recipeStack.push(new RecipeDescriptor[] {recipeThatMadeChange[1], rd});
                }
            }
        }
        return resultsTable;
    }
}
