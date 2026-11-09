// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/yaml/search/FindKey.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.yaml.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;
import java.util.HashSet;
import java.util.Set;

public final class FindKey extends Recipe {
    @Option(displayName = "Path", description = "A JsonPath expression used to find matching keys.", example = "$.subjects.kind")
    private final String key;

    @Override
    public String getDisplayName() {
        return "Find YAML entries";
    }

    @Override
    public String getDescription() {
        return "Find YAML entries that match the specified [JsonPath](https://github.com/json-path/JsonPath) expression.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(key);
        return new YamlVisitor<ExecutionContext>() {
            @Override
            public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = (Yaml.Mapping.Entry) super.visitMappingEntry(entry, ctx);
                if (matcher.matches(getCursor())) {
                    e = SearchResult.found(e);
                }
                return e;
            }
            @Override
            public Yaml visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
                Yaml.Mapping m = (Yaml.Mapping) super.visitMapping(mapping, ctx);
                if (matcher.matches(getCursor())) {
                    m = SearchResult.found(m);
                }
                return m;
            }
        };
    }

    public static Set<Yaml> find(Yaml y, String jsonPath) {
        JsonPathMatcher matcher = new JsonPathMatcher(jsonPath);
        YamlVisitor<Set<Yaml>> findVisitor = new YamlVisitor<Set<Yaml>>() {
            @Override
            public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, Set<Yaml> es) {
                Yaml.Mapping.Entry e = (Yaml.Mapping.Entry) super.visitMappingEntry(entry, es);
                if (matcher.matches(getCursor())) {
                    es.add(e);
                }
                return e;
            }
            @Override
            public Yaml visitMapping(Yaml.Mapping mapping, Set<Yaml> es) {
                Yaml.Mapping m = (Yaml.Mapping) super.visitMapping(mapping, es);
                if (matcher.matches(getCursor())) {
                    es.add(m);
                }
                return m;
            }
        };
        Set<Yaml> es = new HashSet<>();
        findVisitor.visit(y, es);
        return es;
    }

    public FindKey(final String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindKey(key=" + this.getKey() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindKey)) return false;
        final FindKey other = (FindKey) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$key = this.getKey();
        final Object other$key = other.getKey();
        if (this$key == null ? other$key != null : !this$key.equals(other$key)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindKey;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $key = this.getKey();
        result = result * PRIME + ($key == null ? 43 : $key.hashCode());
        return result;
    }
}
