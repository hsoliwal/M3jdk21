// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/RecipeMarkupDemonstration.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markup;
import static java.util.Objects.requireNonNull;

public final class RecipeMarkupDemonstration extends Recipe {
    @Option(displayName = "Level", description = "The `Markup#Level` to add.", valid = {"debug", "info", "warning", "error"})
    private final String level;

    @Override
    public String getDisplayName() {
        return "Demonstrate rendering of `Markup` markers";
    }

    @Override
    public String getDescription() {
        return "Tooling may decide to elide or display differently markup of different levels.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                switch (level) {
                case "info": 
                    return Markup.info(sourceFile, "This is an info message.");
                case "warning": 
                    return Markup.warn(sourceFile, new IllegalStateException("This is a warning message."));
                case "error": 
                    return Markup.error(sourceFile, new IllegalStateException("This is an error message."));
                case "debug": 
                default: 
                    return Markup.debug(sourceFile, "This is a debug message.");
                }
            }
        };
    }

    public RecipeMarkupDemonstration(final String level) {
        this.level = level;
    }

    public String getLevel() {
        return this.level;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "RecipeMarkupDemonstration(level=" + this.getLevel() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof RecipeMarkupDemonstration)) return false;
        final RecipeMarkupDemonstration other = (RecipeMarkupDemonstration) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$level = this.getLevel();
        final Object other$level = other.getLevel();
        if (this$level == null ? other$level != null : !this$level.equals(other$level)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof RecipeMarkupDemonstration;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $level = this.getLevel();
        result = result * PRIME + ($level == null ? 43 : $level.hashCode());
        return result;
    }
}
