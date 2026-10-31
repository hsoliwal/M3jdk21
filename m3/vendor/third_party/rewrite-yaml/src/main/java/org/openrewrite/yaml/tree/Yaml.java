// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/yaml/tree/Yaml.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.yaml.tree;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.internal.YamlPrinter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

public interface Yaml extends Tree {
    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptYaml(v.adapt(YamlVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(YamlVisitor.class);
    }

    @Nullable
    default <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    /**
     * @return A new deep copy of this block with different IDs.
     */
    Yaml copyPaste();

    String getPrefix();

    Yaml withPrefix(String prefix);


    final class Documents implements Yaml, SourceFile {
        private final UUID id;
        private final Markers markers;
        private final Path sourcePath;
        @Nullable
        private final FileAttributes fileAttributes;
        // for backwards compatibility
        @Nullable
        private final String charsetName;
        private final boolean charsetBomMarked;
        @Nullable
        private final Checksum checksum;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        private final List<? extends Document> documents;

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            return v.visitDocuments(this, p);
        }

        @Override
        public Documents copyPaste() {
            return new Documents(randomId(), Markers.EMPTY, sourcePath, fileAttributes, charsetName, charsetBomMarked, checksum, documents.stream().map(Document::copyPaste).collect(toList()));
        }

        /**
         * Prefixes will always be on {@link Mapping.Entry}.
         *
         * @return The empty string.
         */
        @Override
        public String getPrefix() {
            return "";
        }

        @Override
        public Documents withPrefix(String prefix) {
            if (!prefix.isEmpty()) {
                throw new UnsupportedOperationException("Yaml.Documents may not have a non-empty prefix");
            }
            return this;
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new YamlPrinter<>();
        }

        public Documents(final UUID id, final Markers markers, final Path sourcePath, @Nullable final FileAttributes fileAttributes, @Nullable final String charsetName, final boolean charsetBomMarked, @Nullable final Checksum checksum, final List<? extends Document> documents) {
            this.id = id;
            this.markers = markers;
            this.sourcePath = sourcePath;
            this.fileAttributes = fileAttributes;
            this.charsetName = charsetName;
            this.charsetBomMarked = charsetBomMarked;
            this.checksum = checksum;
            this.documents = documents;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public Path getSourcePath() {
            return this.sourcePath;
        }

        @Nullable
        public FileAttributes getFileAttributes() {
            return this.fileAttributes;
        }

        @Nullable
        public String getCharsetName() {
            return this.charsetName;
        }

        public boolean isCharsetBomMarked() {
            return this.charsetBomMarked;
        }

        @Nullable
        public Checksum getChecksum() {
            return this.checksum;
        }

        public List<? extends Document> getDocuments() {
            return this.documents;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Yaml.Documents(id=" + this.getId() + ", markers=" + this.getMarkers() + ", sourcePath=" + this.getSourcePath() + ", fileAttributes=" + this.getFileAttributes() + ", charsetName=" + this.getCharsetName() + ", charsetBomMarked=" + this.isCharsetBomMarked() + ", checksum=" + this.getChecksum() + ", documents=" + this.getDocuments() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Yaml.Documents)) return false;
            final Yaml.Documents other = (Yaml.Documents) o;
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

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Documents withId(final UUID id) {
            return this.id == id ? this : new Yaml.Documents(id, this.markers, this.sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.documents);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Documents withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Yaml.Documents(this.id, markers, this.sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.documents);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Documents withSourcePath(final Path sourcePath) {
            return this.sourcePath == sourcePath ? this : new Yaml.Documents(this.id, this.markers, sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.documents);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Documents withFileAttributes(@Nullable final FileAttributes fileAttributes) {
            return this.fileAttributes == fileAttributes ? this : new Yaml.Documents(this.id, this.markers, this.sourcePath, fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.documents);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Documents withCharsetBomMarked(final boolean charsetBomMarked) {
            return this.charsetBomMarked == charsetBomMarked ? this : new Yaml.Documents(this.id, this.markers, this.sourcePath, this.fileAttributes, this.charsetName, charsetBomMarked, this.checksum, this.documents);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Documents withChecksum(@Nullable final Checksum checksum) {
            return this.checksum == checksum ? this : new Yaml.Documents(this.id, this.markers, this.sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, checksum, this.documents);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Documents withDocuments(final List<? extends Document> documents) {
            return this.documents == documents ? this : new Yaml.Documents(this.id, this.markers, this.sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, documents);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        private Yaml.Documents withCharsetName(@Nullable final String charsetName) {
            return this.charsetName == charsetName ? this : new Yaml.Documents(this.id, this.markers, this.sourcePath, this.fileAttributes, charsetName, this.charsetBomMarked, this.checksum, this.documents);
        }
    }


    final class Document implements Yaml {
        private final UUID id;
        private final String prefix;
        private final Markers markers;
        private final boolean explicit;
        private final Block block;
        private final End end;

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            return v.visitDocument(this, p);
        }

        @Override
        public Document copyPaste() {
            return new Document(randomId(), prefix, Markers.EMPTY, explicit, block.copyPaste(), end == null ? null : end.copyPaste());
        }


        /**
         * <a href="https://yaml.org/spec/1.1/#c-document-end">Document end</a> specification.
         */
        public static final class End implements Yaml {
            private final UUID id;
            private final String prefix;
            private final Markers markers;
            private final boolean explicit;

            @Override
            public End copyPaste() {
                return new End(randomId(), prefix, Markers.EMPTY, explicit);
            }

            public End(final UUID id, final String prefix, final Markers markers, final boolean explicit) {
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.explicit = explicit;
            }

            public UUID getId() {
                return this.id;
            }

            public String getPrefix() {
                return this.prefix;
            }

            public Markers getMarkers() {
                return this.markers;
            }

            public boolean isExplicit() {
                return this.explicit;
            }

            @org.openrewrite.internal.lang.NonNull
            @Override
            public String toString() {
                return "Yaml.Document.End(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", explicit=" + this.isExplicit() + ")";
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof Yaml.Document.End)) return false;
                final Yaml.Document.End other = (Yaml.Document.End) o;
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

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Document.End withId(final UUID id) {
                return this.id == id ? this : new Yaml.Document.End(id, this.prefix, this.markers, this.explicit);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Document.End withPrefix(final String prefix) {
                return this.prefix == prefix ? this : new Yaml.Document.End(this.id, prefix, this.markers, this.explicit);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Document.End withMarkers(final Markers markers) {
                return this.markers == markers ? this : new Yaml.Document.End(this.id, this.prefix, markers, this.explicit);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Document.End withExplicit(final boolean explicit) {
                return this.explicit == explicit ? this : new Yaml.Document.End(this.id, this.prefix, this.markers, explicit);
            }
        }

        public Document(final UUID id, final String prefix, final Markers markers, final boolean explicit, final Block block, final End end) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.explicit = explicit;
            this.block = block;
            this.end = end;
        }

        public UUID getId() {
            return this.id;
        }

        public String getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public boolean isExplicit() {
            return this.explicit;
        }

        public Block getBlock() {
            return this.block;
        }

        public End getEnd() {
            return this.end;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Yaml.Document(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", explicit=" + this.isExplicit() + ", block=" + this.getBlock() + ", end=" + this.getEnd() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Yaml.Document)) return false;
            final Yaml.Document other = (Yaml.Document) o;
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

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Document withId(final UUID id) {
            return this.id == id ? this : new Yaml.Document(id, this.prefix, this.markers, this.explicit, this.block, this.end);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Document withPrefix(final String prefix) {
            return this.prefix == prefix ? this : new Yaml.Document(this.id, prefix, this.markers, this.explicit, this.block, this.end);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Document withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Yaml.Document(this.id, this.prefix, markers, this.explicit, this.block, this.end);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Document withExplicit(final boolean explicit) {
            return this.explicit == explicit ? this : new Yaml.Document(this.id, this.prefix, this.markers, explicit, this.block, this.end);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Document withBlock(final Block block) {
            return this.block == block ? this : new Yaml.Document(this.id, this.prefix, this.markers, this.explicit, block, this.end);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Document withEnd(final End end) {
            return this.end == end ? this : new Yaml.Document(this.id, this.prefix, this.markers, this.explicit, this.block, end);
        }
    }


    /**
     * Yaml documents may be explicitly ended with "..."
     * When this is set to "true" the "..." will be printed out.
     * When this is set to "false" no "..." will be printed, but a comment at the end of the document still will be
     * @see <a href="https://yaml.org/spec/1.2/spec.html#id2800401">YAML specification</a>
     */
    final class Scalar implements Block, YamlKey {
        private final UUID id;
        private final String prefix;
        private final Markers markers;
        private final Style style;
        @Nullable
        private final Anchor anchor;
        private final String value;


        public enum Style {
            DOUBLE_QUOTED, SINGLE_QUOTED, LITERAL, FOLDED, PLAIN;
        }

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            return v.visitScalar(this, p);
        }

        @Override
        public Scalar copyPaste() {
            return new Scalar(randomId(), prefix, Markers.EMPTY, style, anchor, value);
        }

        public String toString() {
            return "Yaml.Scalar(" + value + ")";
        }

        public Scalar(final UUID id, final String prefix, final Markers markers, final Style style, @Nullable final Anchor anchor, final String value) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.style = style;
            this.anchor = anchor;
            this.value = value;
        }

        public UUID getId() {
            return this.id;
        }

        public String getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public Style getStyle() {
            return this.style;
        }

        @Nullable
        public Anchor getAnchor() {
            return this.anchor;
        }

        public String getValue() {
            return this.value;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Yaml.Scalar)) return false;
            final Yaml.Scalar other = (Yaml.Scalar) o;
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

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Scalar withId(final UUID id) {
            return this.id == id ? this : new Yaml.Scalar(id, this.prefix, this.markers, this.style, this.anchor, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Scalar withPrefix(final String prefix) {
            return this.prefix == prefix ? this : new Yaml.Scalar(this.id, prefix, this.markers, this.style, this.anchor, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Scalar withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Yaml.Scalar(this.id, this.prefix, markers, this.style, this.anchor, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Scalar withStyle(final Style style) {
            return this.style == style ? this : new Yaml.Scalar(this.id, this.prefix, this.markers, style, this.anchor, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Scalar withAnchor(@Nullable final Anchor anchor) {
            return this.anchor == anchor ? this : new Yaml.Scalar(this.id, this.prefix, this.markers, this.style, anchor, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Scalar withValue(final String value) {
            return this.value == value ? this : new Yaml.Scalar(this.id, this.prefix, this.markers, this.style, this.anchor, value);
        }
    }


    class Mapping implements Block {
        private final UUID id;
        private final Markers markers;
        @Nullable
        private final String openingBracePrefix;
        private final List<Entry> entries;
        @Nullable
        private final String closingBracePrefix;
        @Nullable
        private final Anchor anchor;

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            return v.visitMapping(this, p);
        }

        @Override
        public Mapping copyPaste() {
            return new Mapping(randomId(), Markers.EMPTY, openingBracePrefix, entries.stream().map(Entry::copyPaste).collect(toList()), closingBracePrefix, anchor);
        }

        /**
         * Prefixes will always be on {@link Entry}.
         *
         * @return The empty string.
         */
        @Override
        public String getPrefix() {
            return "";
        }

        @Override
        public Mapping withPrefix(String prefix) {
            if (!prefix.isEmpty()) {
                throw new UnsupportedOperationException("Yaml.Mapping may not have a non-empty prefix");
            }
            return this;
        }


        public static final class Entry implements Yaml {
            private final UUID id;
            private final String prefix;
            private final Markers markers;
            private final YamlKey key;
            // https://yaml.org/spec/1.2/spec.html#:%20mapping%20value//
            private final String beforeMappingValueIndicator;
            private final Block value;

            @Override
            public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
                return v.visitMappingEntry(this, p);
            }

            @Override
            public Entry copyPaste() {
                return new Entry(randomId(), prefix, Markers.EMPTY, key.copyPaste(), beforeMappingValueIndicator, value.copyPaste());
            }

            public Entry(final UUID id, final String prefix, final Markers markers, final YamlKey key, final String beforeMappingValueIndicator, final Block value) {
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.key = key;
                this.beforeMappingValueIndicator = beforeMappingValueIndicator;
                this.value = value;
            }

            public UUID getId() {
                return this.id;
            }

            public String getPrefix() {
                return this.prefix;
            }

            public Markers getMarkers() {
                return this.markers;
            }

            public YamlKey getKey() {
                return this.key;
            }

            public String getBeforeMappingValueIndicator() {
                return this.beforeMappingValueIndicator;
            }

            public Block getValue() {
                return this.value;
            }

            @org.openrewrite.internal.lang.NonNull
            @Override
            public String toString() {
                return "Yaml.Mapping.Entry(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", key=" + this.getKey() + ", beforeMappingValueIndicator=" + this.getBeforeMappingValueIndicator() + ", value=" + this.getValue() + ")";
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof Yaml.Mapping.Entry)) return false;
                final Yaml.Mapping.Entry other = (Yaml.Mapping.Entry) o;
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

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Mapping.Entry withId(final UUID id) {
                return this.id == id ? this : new Yaml.Mapping.Entry(id, this.prefix, this.markers, this.key, this.beforeMappingValueIndicator, this.value);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Mapping.Entry withPrefix(final String prefix) {
                return this.prefix == prefix ? this : new Yaml.Mapping.Entry(this.id, prefix, this.markers, this.key, this.beforeMappingValueIndicator, this.value);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Mapping.Entry withMarkers(final Markers markers) {
                return this.markers == markers ? this : new Yaml.Mapping.Entry(this.id, this.prefix, markers, this.key, this.beforeMappingValueIndicator, this.value);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Mapping.Entry withKey(final YamlKey key) {
                return this.key == key ? this : new Yaml.Mapping.Entry(this.id, this.prefix, this.markers, key, this.beforeMappingValueIndicator, this.value);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Mapping.Entry withBeforeMappingValueIndicator(final String beforeMappingValueIndicator) {
                return this.beforeMappingValueIndicator == beforeMappingValueIndicator ? this : new Yaml.Mapping.Entry(this.id, this.prefix, this.markers, this.key, beforeMappingValueIndicator, this.value);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Mapping.Entry withValue(final Block value) {
                return this.value == value ? this : new Yaml.Mapping.Entry(this.id, this.prefix, this.markers, this.key, this.beforeMappingValueIndicator, value);
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Yaml.Mapping)) return false;
            final Yaml.Mapping other = (Yaml.Mapping) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
            return other instanceof Yaml.Mapping;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Mapping(final UUID id, final Markers markers, @Nullable final String openingBracePrefix, final List<Entry> entries, @Nullable final String closingBracePrefix, @Nullable final Anchor anchor) {
            this.id = id;
            this.markers = markers;
            this.openingBracePrefix = openingBracePrefix;
            this.entries = entries;
            this.closingBracePrefix = closingBracePrefix;
            this.anchor = anchor;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        @Nullable
        public String getOpeningBracePrefix() {
            return this.openingBracePrefix;
        }

        public List<Entry> getEntries() {
            return this.entries;
        }

        @Nullable
        public String getClosingBracePrefix() {
            return this.closingBracePrefix;
        }

        @Nullable
        public Anchor getAnchor() {
            return this.anchor;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Yaml.Mapping(id=" + this.getId() + ", markers=" + this.getMarkers() + ", openingBracePrefix=" + this.getOpeningBracePrefix() + ", entries=" + this.getEntries() + ", closingBracePrefix=" + this.getClosingBracePrefix() + ", anchor=" + this.getAnchor() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Mapping withId(final UUID id) {
            return this.id == id ? this : new Yaml.Mapping(id, this.markers, this.openingBracePrefix, this.entries, this.closingBracePrefix, this.anchor);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Mapping withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Yaml.Mapping(this.id, markers, this.openingBracePrefix, this.entries, this.closingBracePrefix, this.anchor);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Mapping withOpeningBracePrefix(@Nullable final String openingBracePrefix) {
            return this.openingBracePrefix == openingBracePrefix ? this : new Yaml.Mapping(this.id, this.markers, openingBracePrefix, this.entries, this.closingBracePrefix, this.anchor);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Mapping withEntries(final List<Entry> entries) {
            return this.entries == entries ? this : new Yaml.Mapping(this.id, this.markers, this.openingBracePrefix, entries, this.closingBracePrefix, this.anchor);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Mapping withClosingBracePrefix(@Nullable final String closingBracePrefix) {
            return this.closingBracePrefix == closingBracePrefix ? this : new Yaml.Mapping(this.id, this.markers, this.openingBracePrefix, this.entries, closingBracePrefix, this.anchor);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Mapping withAnchor(@Nullable final Anchor anchor) {
            return this.anchor == anchor ? this : new Yaml.Mapping(this.id, this.markers, this.openingBracePrefix, this.entries, this.closingBracePrefix, anchor);
        }
    }


    class Sequence implements Block {
        private final UUID id;
        private final Markers markers;
        /**
         * Will contain the whitespace preceding the '[' in an inline sequence like [a, b].
         * Will be null if the sequence is not an inline sequence, e.g.:
         *  - foo
         *  - bar
         */
        @Nullable
        private final String openingBracketPrefix;
        private final List<Entry> entries;
        @Nullable
        private final String closingBracketPrefix;
        @Nullable
        private final Anchor anchor;

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            return v.visitSequence(this, p);
        }

        @Override
        public Sequence copyPaste() {
            return new Sequence(randomId(), Markers.EMPTY, openingBracketPrefix, entries.stream().map(Entry::copyPaste).collect(toList()), closingBracketPrefix, anchor);
        }

        @Override
        public String getPrefix() {
            return "";
        }

        @Override
        public Sequence withPrefix(String prefix) {
            if (!prefix.isEmpty()) {
                throw new UnsupportedOperationException("Yaml.Sequence may not have a non-empty prefix");
            }
            return this;
        }


        public static final class Entry implements Yaml {
            private final UUID id;
            private final String prefix;
            private final Markers markers;
            private final Block block;
            /**
             * Set to true when this entry is part of a sequence like:
             * - 1
             * - 2
             *
             * And false when this entry is part of a sequence like:
             * [1, 2]
             */
            private final boolean dash;
            @Nullable
            private final String trailingCommaPrefix;

            @Override
            public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
                return v.visitSequenceEntry(this, p);
            }

            @Override
            public Entry copyPaste() {
                return new Entry(randomId(), prefix, Markers.EMPTY, block.copyPaste(), dash, trailingCommaPrefix);
            }

            public Entry(final UUID id, final String prefix, final Markers markers, final Block block, final boolean dash, @Nullable final String trailingCommaPrefix) {
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.block = block;
                this.dash = dash;
                this.trailingCommaPrefix = trailingCommaPrefix;
            }

            public UUID getId() {
                return this.id;
            }

            public String getPrefix() {
                return this.prefix;
            }

            public Markers getMarkers() {
                return this.markers;
            }

            public Block getBlock() {
                return this.block;
            }

            public boolean isDash() {
                return this.dash;
            }

            @Nullable
            public String getTrailingCommaPrefix() {
                return this.trailingCommaPrefix;
            }

            @org.openrewrite.internal.lang.NonNull
            @Override
            public String toString() {
                return "Yaml.Sequence.Entry(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", block=" + this.getBlock() + ", dash=" + this.isDash() + ", trailingCommaPrefix=" + this.getTrailingCommaPrefix() + ")";
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof Yaml.Sequence.Entry)) return false;
                final Yaml.Sequence.Entry other = (Yaml.Sequence.Entry) o;
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

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Sequence.Entry withId(final UUID id) {
                return this.id == id ? this : new Yaml.Sequence.Entry(id, this.prefix, this.markers, this.block, this.dash, this.trailingCommaPrefix);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Sequence.Entry withDash(final boolean dash) {
                return this.dash == dash ? this : new Yaml.Sequence.Entry(this.id, this.prefix, this.markers, this.block, dash, this.trailingCommaPrefix);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Sequence.Entry withPrefix(final String prefix) {
                return this.prefix == prefix ? this : new Yaml.Sequence.Entry(this.id, prefix, this.markers, this.block, this.dash, this.trailingCommaPrefix);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Sequence.Entry withMarkers(final Markers markers) {
                return this.markers == markers ? this : new Yaml.Sequence.Entry(this.id, this.prefix, markers, this.block, this.dash, this.trailingCommaPrefix);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Sequence.Entry withBlock(final Block block) {
                return this.block == block ? this : new Yaml.Sequence.Entry(this.id, this.prefix, this.markers, block, this.dash, this.trailingCommaPrefix);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public Yaml.Sequence.Entry withTrailingCommaPrefix(@Nullable final String trailingCommaPrefix) {
                return this.trailingCommaPrefix == trailingCommaPrefix ? this : new Yaml.Sequence.Entry(this.id, this.prefix, this.markers, this.block, this.dash, trailingCommaPrefix);
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Yaml.Sequence)) return false;
            final Yaml.Sequence other = (Yaml.Sequence) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
            return other instanceof Yaml.Sequence;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Sequence(final UUID id, final Markers markers, @Nullable final String openingBracketPrefix, final List<Entry> entries, @Nullable final String closingBracketPrefix, @Nullable final Anchor anchor) {
            this.id = id;
            this.markers = markers;
            this.openingBracketPrefix = openingBracketPrefix;
            this.entries = entries;
            this.closingBracketPrefix = closingBracketPrefix;
            this.anchor = anchor;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * Will contain the whitespace preceding the '[' in an inline sequence like [a, b].
         * Will be null if the sequence is not an inline sequence, e.g.:
         *  - foo
         *  - bar
         */
        @Nullable
        public String getOpeningBracketPrefix() {
            return this.openingBracketPrefix;
        }

        public List<Entry> getEntries() {
            return this.entries;
        }

        @Nullable
        public String getClosingBracketPrefix() {
            return this.closingBracketPrefix;
        }

        @Nullable
        public Anchor getAnchor() {
            return this.anchor;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "Yaml.Sequence(id=" + this.getId() + ", markers=" + this.getMarkers() + ", openingBracketPrefix=" + this.getOpeningBracketPrefix() + ", entries=" + this.getEntries() + ", closingBracketPrefix=" + this.getClosingBracketPrefix() + ", anchor=" + this.getAnchor() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Sequence withId(final UUID id) {
            return this.id == id ? this : new Yaml.Sequence(id, this.markers, this.openingBracketPrefix, this.entries, this.closingBracketPrefix, this.anchor);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Sequence withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Yaml.Sequence(this.id, markers, this.openingBracketPrefix, this.entries, this.closingBracketPrefix, this.anchor);
        }

        /**
         * Will contain the whitespace preceding the '[' in an inline sequence like [a, b].
         * Will be null if the sequence is not an inline sequence, e.g.:
         *  - foo
         *  - bar
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Sequence withOpeningBracketPrefix(@Nullable final String openingBracketPrefix) {
            return this.openingBracketPrefix == openingBracketPrefix ? this : new Yaml.Sequence(this.id, this.markers, openingBracketPrefix, this.entries, this.closingBracketPrefix, this.anchor);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Sequence withEntries(final List<Entry> entries) {
            return this.entries == entries ? this : new Yaml.Sequence(this.id, this.markers, this.openingBracketPrefix, entries, this.closingBracketPrefix, this.anchor);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Sequence withClosingBracketPrefix(@Nullable final String closingBracketPrefix) {
            return this.closingBracketPrefix == closingBracketPrefix ? this : new Yaml.Sequence(this.id, this.markers, this.openingBracketPrefix, this.entries, closingBracketPrefix, this.anchor);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Sequence withAnchor(@Nullable final Anchor anchor) {
            return this.anchor == anchor ? this : new Yaml.Sequence(this.id, this.markers, this.openingBracketPrefix, this.entries, this.closingBracketPrefix, anchor);
        }
    }


    /**
     * Holds the whitespace between this entry and its trailing comma, if any.
     *   v
     * [1 , 2]
     * null if there is no trailing comma.
     */
    final class Alias implements Block, YamlKey {
        private final UUID id;
        private final String prefix;
        private final Markers markers;
        private final Anchor anchor;

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            return v.visitAlias(this, p);
        }

        @Override
        public String getValue() {
            return anchor.key;
        }

        @Override
        public Alias copyPaste() {
            return new Alias(randomId(), prefix, Markers.EMPTY, anchor);
        }

        public String toString() {
            return "Yaml.Alias(" + anchor + ")";
        }

        public Alias(final UUID id, final String prefix, final Markers markers, final Anchor anchor) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.anchor = anchor;
        }

        public UUID getId() {
            return this.id;
        }

        public String getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public Anchor getAnchor() {
            return this.anchor;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Yaml.Alias)) return false;
            final Yaml.Alias other = (Yaml.Alias) o;
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

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Alias withId(final UUID id) {
            return this.id == id ? this : new Yaml.Alias(id, this.prefix, this.markers, this.anchor);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Alias withPrefix(final String prefix) {
            return this.prefix == prefix ? this : new Yaml.Alias(this.id, prefix, this.markers, this.anchor);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Alias withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Yaml.Alias(this.id, this.prefix, markers, this.anchor);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Alias withAnchor(final Anchor anchor) {
            return this.anchor == anchor ? this : new Yaml.Alias(this.id, this.prefix, this.markers, anchor);
        }
    }


    final class Anchor implements Yaml {
        private final UUID id;
        private final String prefix;
        private final String postfix;
        private final Markers markers;
        private final String key;

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            return v.visitAnchor(this, p);
        }

        @Override
        public Anchor copyPaste() {
            return new Anchor(randomId(), prefix, postfix, Markers.EMPTY, key);
        }

        public String toString() {
            return "Yaml.Anchor(" + key + ")";
        }

        public Anchor(final UUID id, final String prefix, final String postfix, final Markers markers, final String key) {
            this.id = id;
            this.prefix = prefix;
            this.postfix = postfix;
            this.markers = markers;
            this.key = key;
        }

        public UUID getId() {
            return this.id;
        }

        public String getPrefix() {
            return this.prefix;
        }

        public String getPostfix() {
            return this.postfix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public String getKey() {
            return this.key;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof Yaml.Anchor)) return false;
            final Yaml.Anchor other = (Yaml.Anchor) o;
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

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Anchor withId(final UUID id) {
            return this.id == id ? this : new Yaml.Anchor(id, this.prefix, this.postfix, this.markers, this.key);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Anchor withPrefix(final String prefix) {
            return this.prefix == prefix ? this : new Yaml.Anchor(this.id, prefix, this.postfix, this.markers, this.key);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Anchor withPostfix(final String postfix) {
            return this.postfix == postfix ? this : new Yaml.Anchor(this.id, this.prefix, postfix, this.markers, this.key);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Anchor withMarkers(final Markers markers) {
            return this.markers == markers ? this : new Yaml.Anchor(this.id, this.prefix, this.postfix, markers, this.key);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public Yaml.Anchor withKey(final String key) {
            return this.key == key ? this : new Yaml.Anchor(this.id, this.prefix, this.postfix, this.markers, key);
        }
    }


    interface Block extends Yaml {
        /**
         * @return A new deep copy of this block with different IDs.
         */
        Block copyPaste();

        Block withPrefix(String prefix);
    }
}
