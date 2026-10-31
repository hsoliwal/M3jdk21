// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/yaml/CommentOutProperty.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.yaml;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.tree.Yaml;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Collectors;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

public final class CommentOutProperty extends Recipe {
    @Option(displayName = "Property key", description = "The key to be commented out.", example = "applicability.singleSource")
    private final String propertyKey;
    @Option(displayName = "comment text", description = "The comment text to be added before the specified key.", example = "The `foo` property is deprecated, please migrate")
    private final String commentText;

    @Override
    public String getDisplayName() {
        return "Comment out property";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s`", propertyKey);
    }

    @Override
    public String getDescription() {
        return "Comment out a YAML property and add a comment in front.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new YamlIsoVisitor<ExecutionContext>() {
            private String comment = "";
            private String indentation = "";
            @Override
            public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, ExecutionContext ctx) {
                indentation = entry.getPrefix();
                if (!comment.isEmpty()) {
                    // add comment and return
                    String newPrefix = entry.getPrefix() + "# " + commentText + comment + entry.getPrefix();
                    comment = "";
                    return entry.withPrefix(newPrefix);
                }
                return super.visitSequenceEntry(entry, ctx);
            }
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                String lastIndentation = indentation;
                indentation = entry.getPrefix();
                if (!comment.isEmpty()) {
                    String newPrefix = entry.getPrefix() + "# " + commentText + comment + entry.getPrefix();
                    comment = "";
                    return entry.withPrefix(newPrefix);
                }
                Deque<Yaml.Mapping.Entry> propertyEntries = getCursor().getPathAsStream().filter(Yaml.Mapping.Entry.class::isInstance).map(Yaml.Mapping.Entry.class::cast).collect(Collectors.toCollection(ArrayDeque::new));
                String prop = stream(spliteratorUnknownSize(propertyEntries.descendingIterator(), 0), false).map(e2 -> e2.getKey().getValue()).collect(Collectors.joining("."));
                if (prop.equals(propertyKey)) {
                    String prefix = entry.getPrefix();
                    if (prefix.contains("\n")) {
                        comment = entry.print(getCursor()).replace(prefix, prefix + "# ");
                    } else {
                        // getCursor().getParent().getValue()
                        comment = lastIndentation + "#" + entry.print(getCursor());
                    }
                    doAfterVisit(new DeleteProperty(propertyKey, null, null).getVisitor());
                    return entry;
                }
                return super.visitMappingEntry(entry, ctx);
            }
        };
    }

    public CommentOutProperty(final String propertyKey, final String commentText) {
        this.propertyKey = propertyKey;
        this.commentText = commentText;
    }

    public String getPropertyKey() {
        return this.propertyKey;
    }

    public String getCommentText() {
        return this.commentText;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "CommentOutProperty(propertyKey=" + this.getPropertyKey() + ", commentText=" + this.getCommentText() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof CommentOutProperty)) return false;
        final CommentOutProperty other = (CommentOutProperty) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$propertyKey = this.getPropertyKey();
        final Object other$propertyKey = other.getPropertyKey();
        if (this$propertyKey == null ? other$propertyKey != null : !this$propertyKey.equals(other$propertyKey)) return false;
        final Object this$commentText = this.getCommentText();
        final Object other$commentText = other.getCommentText();
        if (this$commentText == null ? other$commentText != null : !this$commentText.equals(other$commentText)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof CommentOutProperty;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $propertyKey = this.getPropertyKey();
        result = result * PRIME + ($propertyKey == null ? 43 : $propertyKey.hashCode());
        final Object $commentText = this.getCommentText();
        result = result * PRIME + ($commentText == null ? 43 : $commentText.hashCode());
        return result;
    }
}
