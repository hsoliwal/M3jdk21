// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/search/FindBuildMetadata.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.search;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.BuildMetadata;
import org.openrewrite.marker.SearchResult;

public final class FindBuildMetadata extends Recipe {
    @Option(displayName = "Build metadata key", description = "The key to search for in the build metadata.", example = "lstFormatVersion")
    private final String key;
    @Option(displayName = "Build metadata value", description = "The value to search for in the build metadata.", example = "2")
    private final String value;

    @Override
    public String getDisplayName() {
        return "Find build metadata";
    }

    @Override
    public String getDescription() {
        return "Find source files with matching build metadata.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    for (BuildMetadata buildMetadata : tree.getMarkers().findAll(BuildMetadata.class)) {
                        if (buildMetadata.getMetadata().containsKey(key)) {
                            if (buildMetadata.getMetadata().get(key).equals(value)) {
                                return SearchResult.found(tree, "Found build metadata");
                            }
                        }
                    }
                }
                return tree;
            }
        };
    }

    public FindBuildMetadata(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindBuildMetadata(key=" + this.getKey() + ", value=" + this.getValue() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindBuildMetadata)) return false;
        final FindBuildMetadata other = (FindBuildMetadata) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$key = this.getKey();
        final Object other$key = other.getKey();
        if (this$key == null ? other$key != null : !this$key.equals(other$key)) return false;
        final Object this$value = this.getValue();
        final Object other$value = other.getValue();
        if (this$value == null ? other$value != null : !this$value.equals(other$value)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindBuildMetadata;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $key = this.getKey();
        result = result * PRIME + ($key == null ? 43 : $key.hashCode());
        final Object $value = this.getValue();
        result = result * PRIME + ($value == null ? 43 : $value.hashCode());
        return result;
    }
}
