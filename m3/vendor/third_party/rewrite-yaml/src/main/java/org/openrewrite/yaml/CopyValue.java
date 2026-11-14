// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/yaml/CopyValue.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.tree.Yaml;
import java.nio.file.Path;

@SuppressWarnings("LanguageMismatch")
public final class CopyValue extends ScanningRecipe<CopyValue.Accumulator> {
    @Option(displayName = "Old key path", description = "A [JsonPath](https://github.com/json-path/JsonPath) expression to locate a YAML key/value pair to copy.", example = "$.source.kind")
    private final String oldKeyPath;
    @Option(displayName = "Old file path", description = "The file path to the YAML file to copy the value from. " + "If `null` then the value will be copied from any yaml file it appears within.", example = "src/main/resources/application.yaml", required = false)
    @Nullable
    private final String oldFilePath;
    @Option(displayName = "New key path", description = "A [JsonPath](https://github.com/json-path/JsonPath) expression defining where the value should be written.", example = "$.dest.kind")
    private final String newKey;
    @Option(displayName = "New file path", description = "The file path to the YAML file to copy the value to. " + "If `null` then the value will be copied only into the same file it was found in.", example = "src/main/resources/application.yaml", required = false)
    @Nullable
    private final String newFilePath;

    @Override
    public String getDisplayName() {
        return "Copy YAML value";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("%s`%s` to %s`%s`", (oldFilePath == null) ? "" : oldFilePath + ":", oldKeyPath, (newFilePath == null) ? "" : newFilePath + ":", newKey);
    }

    @Override
    public String getDescription() {
        return "Copies a YAML value from one key to another. " + "The existing key/value pair remains unaffected by this change. " + "Attempts to merge the copied value into the new key if it already exists. " + "Attempts to create the new key if it does not exist.";
    }


    public static class Accumulator {
        @Nullable
        String snippet;
        Path path;

        public Accumulator() {
        }

        @Nullable
        public String getSnippet() {
            return this.snippet;
        }

        public Path getPath() {
            return this.path;
        }

        public void setSnippet(@Nullable final String snippet) {
            this.snippet = snippet;
        }

        public void setPath(final Path path) {
            this.path = path;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof CopyValue.Accumulator)) return false;
            final CopyValue.Accumulator other = (CopyValue.Accumulator) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$snippet = this.getSnippet();
            final Object other$snippet = other.getSnippet();
            if (this$snippet == null ? other$snippet != null : !this$snippet.equals(other$snippet)) return false;
            final Object this$path = this.getPath();
            final Object other$path = other.getPath();
            if (this$path == null ? other$path != null : !this$path.equals(other$path)) return false;
            return true;
        }

        protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
            return other instanceof CopyValue.Accumulator;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $snippet = this.getSnippet();
            result = result * PRIME + ($snippet == null ? 43 : $snippet.hashCode());
            final Object $path = this.getPath();
            result = result * PRIME + ($path == null ? 43 : $path.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "CopyValue.Accumulator(snippet=" + this.getSnippet() + ", path=" + this.getPath() + ")";
        }
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        TreeVisitor<?, ExecutionContext> visitor = new YamlIsoVisitor<ExecutionContext>() {
            final JsonPathMatcher oldPathMatcher = new JsonPathMatcher(oldKeyPath);
            @Override
            public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext executionContext) {
                if (acc.snippet == null) {
                    return super.visitDocuments(documents, executionContext);
                }
                return documents;
            }
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry source = super.visitMappingEntry(entry, ctx);
                if (oldPathMatcher.matches(getCursor())) {
                    acc.snippet = entry.getValue().print(getCursor());
                    acc.path = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath();
                }
                return source;
            }
        };
        if (oldFilePath != null) {
            visitor = Preconditions.check(new FindSourceFiles(oldFilePath).getVisitor(), visitor);
        }
        return visitor;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if (acc.snippet == null) {
            return TreeVisitor.noop();
        }
        TreeVisitor<?, ExecutionContext> visitor = new MergeYaml(newKey, acc.snippet, false, null).getVisitor();
        if (newFilePath == null) {
            visitor = Preconditions.check(new FindSourceFiles(acc.path.toString()).getVisitor(), visitor);
        } else {
            visitor = Preconditions.check(new FindSourceFiles(newFilePath).getVisitor(), visitor);
        }
        return visitor;
    }

    public CopyValue(final String oldKeyPath, @Nullable final String oldFilePath, final String newKey, @Nullable final String newFilePath) {
        this.oldKeyPath = oldKeyPath;
        this.oldFilePath = oldFilePath;
        this.newKey = newKey;
        this.newFilePath = newFilePath;
    }

    public String getOldKeyPath() {
        return this.oldKeyPath;
    }

    @Nullable
    public String getOldFilePath() {
        return this.oldFilePath;
    }

    public String getNewKey() {
        return this.newKey;
    }

    @Nullable
    public String getNewFilePath() {
        return this.newFilePath;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "CopyValue(oldKeyPath=" + this.getOldKeyPath() + ", oldFilePath=" + this.getOldFilePath() + ", newKey=" + this.getNewKey() + ", newFilePath=" + this.getNewFilePath() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof CopyValue)) return false;
        final CopyValue other = (CopyValue) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$oldKeyPath = this.getOldKeyPath();
        final Object other$oldKeyPath = other.getOldKeyPath();
        if (this$oldKeyPath == null ? other$oldKeyPath != null : !this$oldKeyPath.equals(other$oldKeyPath)) return false;
        final Object this$oldFilePath = this.getOldFilePath();
        final Object other$oldFilePath = other.getOldFilePath();
        if (this$oldFilePath == null ? other$oldFilePath != null : !this$oldFilePath.equals(other$oldFilePath)) return false;
        final Object this$newKey = this.getNewKey();
        final Object other$newKey = other.getNewKey();
        if (this$newKey == null ? other$newKey != null : !this$newKey.equals(other$newKey)) return false;
        final Object this$newFilePath = this.getNewFilePath();
        final Object other$newFilePath = other.getNewFilePath();
        if (this$newFilePath == null ? other$newFilePath != null : !this$newFilePath.equals(other$newFilePath)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof CopyValue;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $oldKeyPath = this.getOldKeyPath();
        result = result * PRIME + ($oldKeyPath == null ? 43 : $oldKeyPath.hashCode());
        final Object $oldFilePath = this.getOldFilePath();
        result = result * PRIME + ($oldFilePath == null ? 43 : $oldFilePath.hashCode());
        final Object $newKey = this.getNewKey();
        result = result * PRIME + ($newKey == null ? 43 : $newKey.hashCode());
        final Object $newFilePath = this.getNewFilePath();
        result = result * PRIME + ($newFilePath == null ? 43 : $newFilePath.hashCode());
        return result;
    }
}
