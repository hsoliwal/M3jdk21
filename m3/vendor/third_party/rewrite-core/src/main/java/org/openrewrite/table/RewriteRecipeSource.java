// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/table/RewriteRecipeSource.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class RewriteRecipeSource extends DataTable<RewriteRecipeSource.Row> {
    public RewriteRecipeSource(Recipe recipe) {
        super(recipe, "Rewrite recipe source code", "This table contains the source code of recipes along with their metadata " + "for use in an experiment fine-tuning large language models to produce more recipes.");
    }


    public static final class Row {
        @Column(displayName = "Recipe name", description = "The name of the recipe.")
        private final String displayName;
        @Column(displayName = "Recipe description", description = "The description of the recipe.")
        private final String description;
        @Column(displayName = "Recipe type", description = "Differentiate between Java and YAML recipes, as they may be " + "two independent data sets used in LLM fine-tuning.")
        private final RecipeType recipeType;
        @Column(displayName = "Recipe source code", description = "The full source code of the recipe.")
        private final String sourceCode;
        @Column(displayName = "Recipe options", description = "JSON format of recipe options.")
        private final String options;

        public Row(final String displayName, final String description, final RecipeType recipeType, final String sourceCode, final String options) {
            this.displayName = displayName;
            this.description = description;
            this.recipeType = recipeType;
            this.sourceCode = sourceCode;
            this.options = options;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getDescription() {
            return this.description;
        }

        public RecipeType getRecipeType() {
            return this.recipeType;
        }

        public String getSourceCode() {
            return this.sourceCode;
        }

        public String getOptions() {
            return this.options;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof RewriteRecipeSource.Row)) return false;
            final RewriteRecipeSource.Row other = (RewriteRecipeSource.Row) o;
            final Object this$displayName = this.getDisplayName();
            final Object other$displayName = other.getDisplayName();
            if (this$displayName == null ? other$displayName != null : !this$displayName.equals(other$displayName)) return false;
            final Object this$description = this.getDescription();
            final Object other$description = other.getDescription();
            if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
            final Object this$recipeType = this.getRecipeType();
            final Object other$recipeType = other.getRecipeType();
            if (this$recipeType == null ? other$recipeType != null : !this$recipeType.equals(other$recipeType)) return false;
            final Object this$sourceCode = this.getSourceCode();
            final Object other$sourceCode = other.getSourceCode();
            if (this$sourceCode == null ? other$sourceCode != null : !this$sourceCode.equals(other$sourceCode)) return false;
            final Object this$options = this.getOptions();
            final Object other$options = other.getOptions();
            if (this$options == null ? other$options != null : !this$options.equals(other$options)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $displayName = this.getDisplayName();
            result = result * PRIME + ($displayName == null ? 43 : $displayName.hashCode());
            final Object $description = this.getDescription();
            result = result * PRIME + ($description == null ? 43 : $description.hashCode());
            final Object $recipeType = this.getRecipeType();
            result = result * PRIME + ($recipeType == null ? 43 : $recipeType.hashCode());
            final Object $sourceCode = this.getSourceCode();
            result = result * PRIME + ($sourceCode == null ? 43 : $sourceCode.hashCode());
            final Object $options = this.getOptions();
            result = result * PRIME + ($options == null ? 43 : $options.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "RewriteRecipeSource.Row(displayName=" + this.getDisplayName() + ", description=" + this.getDescription() + ", recipeType=" + this.getRecipeType() + ", sourceCode=" + this.getSourceCode() + ", options=" + this.getOptions() + ")";
        }
    }


    public enum RecipeType {
        Java, Yaml;
    }
}
