// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/yaml/ChangeKey.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2020 the original author or authors.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.yaml.tree.Yaml;

public final class ChangeKey extends Recipe {
    @Option(displayName = "Old key path", description = "A [JsonPath](https://github.com/json-path/JsonPath) expression to locate a YAML entry.", example = "$.subjects.kind")
    private final String oldKeyPath;
    @Option(displayName = "New key", description = "The new name for the key selected by the `oldKeyPath`.", example = "kind")
    private final String newKey;

    @Override
    public String getDisplayName() {
        return "Change key";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", oldKeyPath, newKey);
    }

    @Override
    public String getDescription() {
        return "Change a YAML mapping entry key while leaving the value intact.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(oldKeyPath);
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                if (matcher.matches(getCursor())) {
                    if (e.getKey() instanceof Yaml.Scalar) {
                        e = e.withKey(((Yaml.Scalar) e.getKey()).withValue(newKey));
                    }
                }
                return e;
            }
        };
    }

    public ChangeKey(final String oldKeyPath, final String newKey) {
        this.oldKeyPath = oldKeyPath;
        this.newKey = newKey;
    }

    public String getOldKeyPath() {
        return this.oldKeyPath;
    }

    public String getNewKey() {
        return this.newKey;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ChangeKey(oldKeyPath=" + this.getOldKeyPath() + ", newKey=" + this.getNewKey() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangeKey)) return false;
        final ChangeKey other = (ChangeKey) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$oldKeyPath = this.getOldKeyPath();
        final Object other$oldKeyPath = other.getOldKeyPath();
        if (this$oldKeyPath == null ? other$oldKeyPath != null : !this$oldKeyPath.equals(other$oldKeyPath)) return false;
        final Object this$newKey = this.getNewKey();
        final Object other$newKey = other.getNewKey();
        if (this$newKey == null ? other$newKey != null : !this$newKey.equals(other$newKey)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ChangeKey;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $oldKeyPath = this.getOldKeyPath();
        result = result * PRIME + ($oldKeyPath == null ? 43 : $oldKeyPath.hashCode());
        final Object $newKey = this.getNewKey();
        result = result * PRIME + ($newKey == null ? 43 : $newKey.hashCode());
        return result;
    }
}
