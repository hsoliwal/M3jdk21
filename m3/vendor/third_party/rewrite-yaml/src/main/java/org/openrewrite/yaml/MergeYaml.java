// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/yaml/MergeYaml.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.tree.Yaml;

public final class MergeYaml extends Recipe {
    @Option(displayName = "Key path", description = "A [JsonPath](https://github.com/json-path/JsonPath) expression used to find matching keys.", example = "$.metadata")
    private final String key;
    @Option(displayName = "YAML snippet", description = "The YAML snippet to insert. The snippet will be indented to match the style of its surroundings.", example = "labels: \n\tlabel-one: \"value-one\"")
    @Language("yml")
    private final String yaml;
    @Option(displayName = "Accept theirs", description = "When the YAML snippet to insert conflicts with an existing key value pair and an existing key has a different value, prefer the original value.", required = false)
    @Nullable
    private final Boolean acceptTheirs;
    @Incubating(since = "7.30.0")
    @Option(displayName = "Object identifying property", description = "Name of a property which will be used to identify objects (mapping). This serves as the key to match on when merging entries of a sequence.", required = false, example = "name")
    @Nullable
    private final String objectIdentifyingProperty;

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test("yaml", "Must be valid YAML", yaml, y -> new YamlParser().parse(yaml).findFirst().map(doc -> !((Yaml.Documents) doc).getDocuments().isEmpty()).orElse(false)));
    }

    @Override
    public String getDisplayName() {
        return "Merge YAML snippet";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("at `%s`", key);
    }

    @Override
    public String getDescription() {
        return "Merge a YAML snippet with an existing YAML document.";
    }

    static final String FOUND_MATCHING_ELEMENT = "FOUND_MATCHING_ELEMENT";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(key);
        Yaml incoming = new YamlParser().parse(yaml).findFirst().map(Yaml.Documents.class::cast).orElseThrow(() -> new IllegalArgumentException("Could not parse as YAML")).getDocuments().get(0).getBlock();
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                if ("$".equals(key)) {
                    return document.withBlock((Yaml.Block) new MergeYamlVisitor<>(document.getBlock(), yaml, Boolean.TRUE.equals(acceptTheirs), objectIdentifyingProperty).visit(document.getBlock(), ctx, getCursor()));
                }
                Yaml.Document d = super.visitDocument(document, ctx);
                if (d == document && !getCursor().getMessage(FOUND_MATCHING_ELEMENT, false)) {
                    // No matching element already exists, attempt to construct one
                    String valueKey = maybeKeyFromJsonPath(key);
                    if (valueKey == null) {
                        return d;
                    }
                    // If there is no space between the colon and the value it will not be interpreted as a mapping
                    String snippet;
                    if (incoming instanceof Yaml.Mapping) {
                        snippet = valueKey + ":\n" + indent(yaml);
                    } else {
                        snippet = valueKey + ":" + (yaml.startsWith(" ") ? yaml : " " + yaml);
                    }
                    // No matching element already exists, so it must be constructed
                    return d.withBlock((Yaml.Block) new MergeYamlVisitor<>(d.getBlock(), snippet, Boolean.TRUE.equals(acceptTheirs), objectIdentifyingProperty).visit(d.getBlock(), ctx, getCursor()));
                }
                return d;
            }
            public String indent(String text) {
                int index = text.indexOf('\n');
                if (index == -1 || index == text.length() - 1) {
                    return text;
                }
                StringBuilder padding = new StringBuilder();
                for (int i = index + 1; i < text.length(); i++) {
                    if (!Character.isWhitespace(text.charAt(i))) {
                        break;
                    }
                    padding.append(text.charAt(i));
                }
                if (padding.length() == 0) {
                    padding.append("  ");
                }
                return text.replaceAll("(?m)^", padding.toString());
            }
            @Nullable
            private String maybeKeyFromJsonPath(String jsonPath) {
                if (!jsonPath.startsWith("$.")) {
                    return null;
                }
                return jsonPath.substring(2);
            }
            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
                Yaml.Mapping m = super.visitMapping(mapping, ctx);
                if (matcher.matches(getCursor())) {
                    getCursor().putMessageOnFirstEnclosing(Yaml.Document.class, FOUND_MATCHING_ELEMENT, true);
                    m = (Yaml.Mapping) new MergeYamlVisitor<>(mapping, incoming, Boolean.TRUE.equals(acceptTheirs), objectIdentifyingProperty).visitNonNull(mapping, ctx, getCursor().getParentOrThrow());
                }
                return m;
            }
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                if (matcher.matches(getCursor())) {
                    getCursor().putMessageOnFirstEnclosing(Yaml.Document.class, FOUND_MATCHING_ELEMENT, true);
                    // this tests for an awkward case that will be better handled by JsonPathMatcher.
                    // if it is a sequence, we want to insert into every sequence entry for now.
                    if (!(entry.getValue() instanceof Yaml.Sequence)) {
                        Yaml.Block value = (Yaml.Block) new MergeYamlVisitor<>(entry.getValue(), incoming, Boolean.TRUE.equals(acceptTheirs), objectIdentifyingProperty).visitNonNull(entry.getValue(), ctx, getCursor());
                        if (value instanceof Yaml.Scalar && value.getPrefix().isEmpty()) {
                            value = value.withPrefix(" ");
                        }
                        return entry.withValue(value);
                    }
                }
                return super.visitMappingEntry(entry, ctx);
            }
            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, ExecutionContext ctx) {
                if (matcher.matches(getCursor().getParentOrThrow())) {
                    getCursor().putMessageOnFirstEnclosing(Yaml.Document.class, FOUND_MATCHING_ELEMENT, true);
                    return sequence.withEntries(ListUtils.map(sequence.getEntries(), entry -> entry.withBlock((Yaml.Block) new MergeYamlVisitor<>(entry.getBlock(), incoming, Boolean.TRUE.equals(acceptTheirs), objectIdentifyingProperty).visit(entry.getBlock(), ctx, new Cursor(getCursor(), entry)))));
                }
                return super.visitSequence(sequence, ctx);
            }
        };
    }

    public MergeYaml(final String key, @Language("yml") final String yaml, @Nullable final Boolean acceptTheirs, @Nullable final String objectIdentifyingProperty) {
        this.key = key;
        this.yaml = yaml;
        this.acceptTheirs = acceptTheirs;
        this.objectIdentifyingProperty = objectIdentifyingProperty;
    }

    public String getKey() {
        return this.key;
    }

    @Language("yml")
    public String getYaml() {
        return this.yaml;
    }

    @Nullable
    public Boolean getAcceptTheirs() {
        return this.acceptTheirs;
    }

    @Nullable
    public String getObjectIdentifyingProperty() {
        return this.objectIdentifyingProperty;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "MergeYaml(key=" + this.getKey() + ", yaml=" + this.getYaml() + ", acceptTheirs=" + this.getAcceptTheirs() + ", objectIdentifyingProperty=" + this.getObjectIdentifyingProperty() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof MergeYaml)) return false;
        final MergeYaml other = (MergeYaml) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$acceptTheirs = this.getAcceptTheirs();
        final Object other$acceptTheirs = other.getAcceptTheirs();
        if (this$acceptTheirs == null ? other$acceptTheirs != null : !this$acceptTheirs.equals(other$acceptTheirs)) return false;
        final Object this$key = this.getKey();
        final Object other$key = other.getKey();
        if (this$key == null ? other$key != null : !this$key.equals(other$key)) return false;
        final Object this$yaml = this.getYaml();
        final Object other$yaml = other.getYaml();
        if (this$yaml == null ? other$yaml != null : !this$yaml.equals(other$yaml)) return false;
        final Object this$objectIdentifyingProperty = this.getObjectIdentifyingProperty();
        final Object other$objectIdentifyingProperty = other.getObjectIdentifyingProperty();
        if (this$objectIdentifyingProperty == null ? other$objectIdentifyingProperty != null : !this$objectIdentifyingProperty.equals(other$objectIdentifyingProperty)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof MergeYaml;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $acceptTheirs = this.getAcceptTheirs();
        result = result * PRIME + ($acceptTheirs == null ? 43 : $acceptTheirs.hashCode());
        final Object $key = this.getKey();
        result = result * PRIME + ($key == null ? 43 : $key.hashCode());
        final Object $yaml = this.getYaml();
        result = result * PRIME + ($yaml == null ? 43 : $yaml.hashCode());
        final Object $objectIdentifyingProperty = this.getObjectIdentifyingProperty();
        result = result * PRIME + ($objectIdentifyingProperty == null ? 43 : $objectIdentifyingProperty.hashCode());
        return result;
    }
}
