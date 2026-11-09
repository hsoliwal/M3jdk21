// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/yaml/DeleteKey.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.yaml;

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.yaml.tree.Yaml;
import java.util.concurrent.atomic.AtomicReference;

public final class DeleteKey extends Recipe {
    @Option(displayName = "Key path", description = "A [JsonPath](https://github.com/json-path/JsonPath) expression to locate a YAML entry.", example = "$.source.kind")
    private final String keyPath;

    @Override
    public String getDisplayName() {
        return "Delete key";
    }

    @Override
    public String getDescription() {
        return "Delete a YAML mapping entry key.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(keyPath);
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, ExecutionContext ctx) {
                if (matcher.matches(getCursor()) || matcher.matches(new Cursor(getCursor(), entry.getBlock()))) {
                    //noinspection ConstantConditions
                    return null;
                }
                return super.visitSequenceEntry(entry, ctx);
            }
            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
                Yaml.Mapping m = super.visitMapping(mapping, ctx);
                AtomicReference<String> copyFirstPrefix = new AtomicReference<>();
                m = m.withEntries(ListUtils.map(m.getEntries(), (i, e) -> {
                    if (matcher.matches(new Cursor(getCursor(), e))) {
                        if (i == 0 && getCursor().getParentOrThrow().getValue() instanceof Yaml.Sequence.Entry) {
                            copyFirstPrefix.set(e.getPrefix());
                        }
                        removeUnused(getCursor().getParent());
                        return null;
                    }
                    return e;
                }));
                if (!m.getEntries().isEmpty() && copyFirstPrefix.get() != null) {
                    m = m.withEntries(ListUtils.mapFirst(m.getEntries(), e -> e.withPrefix(copyFirstPrefix.get())));
                }
                return m;
            }
        };
    }

    public DeleteKey(final String keyPath) {
        this.keyPath = keyPath;
    }

    public String getKeyPath() {
        return this.keyPath;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "DeleteKey(keyPath=" + this.getKeyPath() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof DeleteKey)) return false;
        final DeleteKey other = (DeleteKey) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$keyPath = this.getKeyPath();
        final Object other$keyPath = other.getKeyPath();
        if (this$keyPath == null ? other$keyPath != null : !this$keyPath.equals(other$keyPath)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof DeleteKey;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $keyPath = this.getKeyPath();
        result = result * PRIME + ($keyPath == null ? 43 : $keyPath.hashCode());
        return result;
    }
}
