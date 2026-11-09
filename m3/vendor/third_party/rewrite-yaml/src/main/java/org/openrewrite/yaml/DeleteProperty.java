// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/yaml/DeleteProperty.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.yaml.tree.Yaml;
import java.util.*;
import java.util.stream.Collectors;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.openrewrite.Tree.randomId;

public final class DeleteProperty extends Recipe {
    @Option(displayName = "Property key", description = "The key to be deleted.", example = "management.metrics.binders.files.enabled")
    private final String propertyKey;
    @Deprecated
    @Option(displayName = "Coalesce", description = "(Deprecated: in a future version, this recipe will always use the `false` behavior)" + " Simplify nested map hierarchies into their simplest dot separated property form.", required = false)
    @Nullable
    private final Boolean coalesce;
    @Option(displayName = "Use relaxed binding", description = "Whether to match the `propertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) " + "rules. Defaults to `true`. If you want to use exact matching in your search, set this to `false`.", required = false)
    @Nullable
    private final Boolean relaxedBinding;

    @Override
    public String getDisplayName() {
        return "Delete property";
    }

    @Override
    public String getDescription() {
        return "Delete a YAML property. Nested YAML mappings are interpreted as dot separated property names, i.e. " + " as Spring Boot interprets application.yml files like `a.b.c.d` or `a.b.c:d`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
                // TODO: Update DeleteProperty to support documents having Anchor / Alias Pairs
                if (documents != new ReplaceAliasWithAnchorValueVisitor<ExecutionContext>().visit(documents, ctx)) {
                    return documents;
                }
                return super.visitDocuments(documents, ctx);
            }
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                Deque<Yaml.Mapping.Entry> propertyEntries = getCursor().getPathAsStream().filter(Yaml.Mapping.Entry.class::isInstance).map(Yaml.Mapping.Entry.class::cast).collect(Collectors.toCollection(ArrayDeque::new));
                String prop = stream(spliteratorUnknownSize(propertyEntries.descendingIterator(), 0), false).map(e2 -> e2.getKey().getValue()).collect(Collectors.joining("."));
                if (!Boolean.FALSE.equals(relaxedBinding) ? NameCaseConvention.equalsRelaxedBinding(prop, propertyKey) : prop.equals(propertyKey)) {
                    doAfterVisit(new DeletePropertyVisitor<>(entry));
                    if (Boolean.TRUE.equals(coalesce)) {
                        maybeCoalesceProperties();
                    }
                }
                return e;
            }
        };
    }


    private static class DeletePropertyVisitor<P> extends YamlVisitor<P> {
        private final Yaml.Mapping.Entry scope;

        private DeletePropertyVisitor(Yaml.Mapping.Entry scope) {
            this.scope = scope;
        }

        @Override
        public Yaml visitSequence(Yaml.Sequence sequence, P p) {
            sequence = (Yaml.Sequence) super.visitSequence(sequence, p);
            List<Yaml.Sequence.Entry> entries = sequence.getEntries();
            entries = ListUtils.map(entries, entry -> ToBeRemoved.hasMarker(entry) ? null : entry);
            return entries.isEmpty() ? ToBeRemoved.withMarker(sequence) : sequence.withEntries(entries);
        }

        @Override
        public Yaml visitSequenceEntry(Yaml.Sequence.Entry entry, P p) {
            entry = (Yaml.Sequence.Entry) super.visitSequenceEntry(entry, p);
            if (entry.getBlock() instanceof Yaml.Mapping) {
                Yaml.Mapping m = (Yaml.Mapping) entry.getBlock();
                if (ToBeRemoved.hasMarker(m)) {
                    return ToBeRemoved.withMarker(entry);
                }
            }
            return entry;
        }

        @Override
        public Yaml visitMapping(Yaml.Mapping mapping, P p) {
            Yaml.Mapping m = (Yaml.Mapping) super.visitMapping(mapping, p);
            boolean changed = false;
            List<Yaml.Mapping.Entry> entries = new ArrayList<>();
            String deletedPrefix = null;
            int count = 0;
            for (Yaml.Mapping.Entry entry : m.getEntries()) {
                if (ToBeRemoved.hasMarker(entry.getValue())) {
                    changed = true;
                    continue;
                }
                if (entry == scope || (entry.getValue() instanceof Yaml.Mapping && ((Yaml.Mapping) entry.getValue()).getEntries().isEmpty())) {
                    deletedPrefix = entry.getPrefix();
                    changed = true;
                } else {
                    if (deletedPrefix != null) {
                        if (count == 0 && containsOnlyWhitespace(entry.getPrefix())) {
                            // do this only if the entry will be the first element
                            entry = entry.withPrefix(deletedPrefix);
                        }
                        deletedPrefix = null;
                    }
                    entries.add(entry);
                    count++;
                }
            }
            if (changed) {
                m = m.withEntries(entries);
                if (entries.isEmpty()) {
                    m = ToBeRemoved.withMarker(m);
                }
                if (getCursor().getParentOrThrow().getValue() instanceof Yaml.Document) {
                    Yaml.Document document = getCursor().getParentOrThrow().getValue();
                    if (!document.isExplicit()) {
                        m = m.withEntries(m.getEntries());
                    }
                }
            }
            return m;
        }
    }

    private static boolean containsOnlyWhitespace(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }


    private static final class ToBeRemoved implements Marker {
        private final UUID id;

        static <Y2 extends Yaml> Y2 withMarker(Y2 y) {
            return y.withMarkers(y.getMarkers().addIfAbsent(new ToBeRemoved(randomId())));
        }

        static boolean hasMarker(Yaml y) {
            return y.getMarkers().findFirst(ToBeRemoved.class).isPresent();
        }

        public ToBeRemoved(final UUID id) {
            this.id = id;
        }

        public UUID getId() {
            return this.id;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof DeleteProperty.ToBeRemoved)) return false;
            final DeleteProperty.ToBeRemoved other = (DeleteProperty.ToBeRemoved) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "DeleteProperty.ToBeRemoved(id=" + this.getId() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public DeleteProperty.ToBeRemoved withId(final UUID id) {
            return this.id == id ? this : new DeleteProperty.ToBeRemoved(id);
        }
    }

    public DeleteProperty(final String propertyKey, @Nullable final Boolean coalesce, @Nullable final Boolean relaxedBinding) {
        this.propertyKey = propertyKey;
        this.coalesce = coalesce;
        this.relaxedBinding = relaxedBinding;
    }

    public String getPropertyKey() {
        return this.propertyKey;
    }

    @Deprecated
    @Nullable
    public Boolean getCoalesce() {
        return this.coalesce;
    }

    @Nullable
    public Boolean getRelaxedBinding() {
        return this.relaxedBinding;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "DeleteProperty(propertyKey=" + this.getPropertyKey() + ", coalesce=" + this.getCoalesce() + ", relaxedBinding=" + this.getRelaxedBinding() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof DeleteProperty)) return false;
        final DeleteProperty other = (DeleteProperty) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$coalesce = this.getCoalesce();
        final Object other$coalesce = other.getCoalesce();
        if (this$coalesce == null ? other$coalesce != null : !this$coalesce.equals(other$coalesce)) return false;
        final Object this$relaxedBinding = this.getRelaxedBinding();
        final Object other$relaxedBinding = other.getRelaxedBinding();
        if (this$relaxedBinding == null ? other$relaxedBinding != null : !this$relaxedBinding.equals(other$relaxedBinding)) return false;
        final Object this$propertyKey = this.getPropertyKey();
        final Object other$propertyKey = other.getPropertyKey();
        if (this$propertyKey == null ? other$propertyKey != null : !this$propertyKey.equals(other$propertyKey)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof DeleteProperty;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $coalesce = this.getCoalesce();
        result = result * PRIME + ($coalesce == null ? 43 : $coalesce.hashCode());
        final Object $relaxedBinding = this.getRelaxedBinding();
        result = result * PRIME + ($relaxedBinding == null ? 43 : $relaxedBinding.hashCode());
        final Object $propertyKey = this.getPropertyKey();
        result = result * PRIME + ($propertyKey == null ? 43 : $propertyKey.hashCode());
        return result;
    }
}
