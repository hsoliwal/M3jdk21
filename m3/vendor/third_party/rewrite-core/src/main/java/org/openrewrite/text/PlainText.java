// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/text/PlainText.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.text;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import static java.util.Collections.emptyList;
import static org.openrewrite.internal.ListUtils.nullIfEmpty;

/**
 * The simplest of all ASTs representing nothing more than just unstructured text.
 */
public final class PlainText implements SourceFile, Tree {
    private final UUID id;
    private final Path sourcePath;
    private final Markers markers;
    // for backwards compatibility
    @Nullable
    private final String charsetName;
    private final boolean charsetBomMarked;

    @Override
    public Charset getCharset() {
        return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SourceFile withCharset(Charset charset) {
        return withCharsetName(charset.name());
    }

    @Nullable
    private final FileAttributes fileAttributes;
    @Nullable
    private final Checksum checksum;
    private final String text;

    public PlainText withText(String text) {
        if (!text.equals(this.text)) {
            return new PlainText(this.id, this.sourcePath, this.markers, this.charsetName, this.charsetBomMarked, this.fileAttributes, this.checksum, text, this.snippets);
        }
        return this;
    }

    private final List<Snippet> snippets;

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(PlainTextVisitor.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) v.adapt(PlainTextVisitor.class).visitText(this, p);
    }

    @Override
    public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
        return new PlainTextPrinter<>();
    }

    @Override
    public long getWeight(Predicate<Object> uniqueIdentity) {
        return text.length() / 10;
    }

    public List<Snippet> getSnippets() {
        if (snippets == null) {
            return emptyList();
        }
        return snippets;
    }

    public PlainText withSnippets(@Nullable List<Snippet> snippets) {
        snippets = nullIfEmpty(snippets);
        if (this.snippets == snippets) {
            return this;
        }
        return new PlainText(id, sourcePath, markers, charsetName, charsetBomMarked, fileAttributes, checksum, text, snippets);
    }


    public static final class Snippet implements Tree {
        private final UUID id;
        private final Markers markers;
        private final String text;

        @Override
        public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
            return v.isAdaptableTo(PlainTextVisitor.class);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
            return (R) v.adapt(PlainTextVisitor.class).visitSnippet(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new PlainTextPrinter<>();
        }

        public Snippet(final UUID id, final Markers markers, final String text) {
            this.id = id;
            this.markers = markers;
            this.text = text;
        }

        public UUID getId() {
            return this.id;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public String getText() {
            return this.text;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof PlainText.Snippet)) return false;
            final PlainText.Snippet other = (PlainText.Snippet) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            final Object this$markers = this.getMarkers();
            final Object other$markers = other.getMarkers();
            if (this$markers == null ? other$markers != null : !this$markers.equals(other$markers)) return false;
            final Object this$text = this.getText();
            final Object other$text = other.getText();
            if (this$text == null ? other$text != null : !this$text.equals(other$text)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            final Object $markers = this.getMarkers();
            result = result * PRIME + ($markers == null ? 43 : $markers.hashCode());
            final Object $text = this.getText();
            result = result * PRIME + ($text == null ? 43 : $text.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "PlainText.Snippet(id=" + this.getId() + ", markers=" + this.getMarkers() + ", text=" + this.getText() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public PlainText.Snippet withId(final UUID id) {
            return this.id == id ? this : new PlainText.Snippet(id, this.markers, this.text);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public PlainText.Snippet withMarkers(final Markers markers) {
            return this.markers == markers ? this : new PlainText.Snippet(this.id, markers, this.text);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public PlainText.Snippet withText(final String text) {
            return this.text == text ? this : new PlainText.Snippet(this.id, this.markers, text);
        }
    }

    private static UUID $default$id() {
        return Tree.randomId();
    }

    private static Markers $default$markers() {
        return Markers.EMPTY;
    }

    private static String $default$text() {
        return "";
    }


    public static class PlainTextBuilder {
        private boolean id$set;
        private UUID id$value;
        private Path sourcePath;
        private boolean markers$set;
        private Markers markers$value;
        private String charsetName;
        private boolean charsetBomMarked;
        private FileAttributes fileAttributes;
        private Checksum checksum;
        private boolean text$set;
        private String text$value;
        private List<Snippet> snippets;

        PlainTextBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @org.openrewrite.internal.lang.NonNull
        public PlainText.PlainTextBuilder id(final UUID id) {
            this.id$value = id;
            id$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @org.openrewrite.internal.lang.NonNull
        public PlainText.PlainTextBuilder sourcePath(final Path sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @org.openrewrite.internal.lang.NonNull
        public PlainText.PlainTextBuilder markers(final Markers markers) {
            this.markers$value = markers;
            markers$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @org.openrewrite.internal.lang.NonNull
        public PlainText.PlainTextBuilder charsetName(@Nullable final String charsetName) {
            this.charsetName = charsetName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @org.openrewrite.internal.lang.NonNull
        public PlainText.PlainTextBuilder charsetBomMarked(final boolean charsetBomMarked) {
            this.charsetBomMarked = charsetBomMarked;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @org.openrewrite.internal.lang.NonNull
        public PlainText.PlainTextBuilder fileAttributes(@Nullable final FileAttributes fileAttributes) {
            this.fileAttributes = fileAttributes;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @org.openrewrite.internal.lang.NonNull
        public PlainText.PlainTextBuilder checksum(@Nullable final Checksum checksum) {
            this.checksum = checksum;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @org.openrewrite.internal.lang.NonNull
        public PlainText.PlainTextBuilder text(final String text) {
            this.text$value = text;
            text$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @org.openrewrite.internal.lang.NonNull
        public PlainText.PlainTextBuilder snippets(final List<Snippet> snippets) {
            this.snippets = snippets;
            return this;
        }

        @org.openrewrite.internal.lang.NonNull
        public PlainText build() {
            UUID id$value = this.id$value;
            if (!this.id$set) id$value = PlainText.$default$id();
            Markers markers$value = this.markers$value;
            if (!this.markers$set) markers$value = PlainText.$default$markers();
            String text$value = this.text$value;
            if (!this.text$set) text$value = PlainText.$default$text();
            return new PlainText(id$value, this.sourcePath, markers$value, this.charsetName, this.charsetBomMarked, this.fileAttributes, this.checksum, text$value, this.snippets);
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "PlainText.PlainTextBuilder(id$value=" + this.id$value + ", sourcePath=" + this.sourcePath + ", markers$value=" + this.markers$value + ", charsetName=" + this.charsetName + ", charsetBomMarked=" + this.charsetBomMarked + ", fileAttributes=" + this.fileAttributes + ", checksum=" + this.checksum + ", text$value=" + this.text$value + ", snippets=" + this.snippets + ")";
        }
    }

    @org.openrewrite.internal.lang.NonNull
    public static PlainText.PlainTextBuilder builder() {
        return new PlainText.PlainTextBuilder();
    }

    public UUID getId() {
        return this.id;
    }

    public Path getSourcePath() {
        return this.sourcePath;
    }

    public Markers getMarkers() {
        return this.markers;
    }

    @Nullable
    public String getCharsetName() {
        return this.charsetName;
    }

    public boolean isCharsetBomMarked() {
        return this.charsetBomMarked;
    }

    @Nullable
    public FileAttributes getFileAttributes() {
        return this.fileAttributes;
    }

    public String getText() {
        return this.text;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof PlainText)) return false;
        final PlainText other = (PlainText) o;
        if (this.isCharsetBomMarked() != other.isCharsetBomMarked()) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$sourcePath = this.getSourcePath();
        final Object other$sourcePath = other.getSourcePath();
        if (this$sourcePath == null ? other$sourcePath != null : !this$sourcePath.equals(other$sourcePath)) return false;
        final Object this$markers = this.getMarkers();
        final Object other$markers = other.getMarkers();
        if (this$markers == null ? other$markers != null : !this$markers.equals(other$markers)) return false;
        final Object this$charsetName = this.getCharsetName();
        final Object other$charsetName = other.getCharsetName();
        if (this$charsetName == null ? other$charsetName != null : !this$charsetName.equals(other$charsetName)) return false;
        final Object this$fileAttributes = this.getFileAttributes();
        final Object other$fileAttributes = other.getFileAttributes();
        if (this$fileAttributes == null ? other$fileAttributes != null : !this$fileAttributes.equals(other$fileAttributes)) return false;
        final Object this$checksum = this.getChecksum();
        final Object other$checksum = other.getChecksum();
        if (this$checksum == null ? other$checksum != null : !this$checksum.equals(other$checksum)) return false;
        final Object this$text = this.getText();
        final Object other$text = other.getText();
        if (this$text == null ? other$text != null : !this$text.equals(other$text)) return false;
        final Object this$snippets = this.getSnippets();
        final Object other$snippets = other.getSnippets();
        if (this$snippets == null ? other$snippets != null : !this$snippets.equals(other$snippets)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isCharsetBomMarked() ? 79 : 97);
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $sourcePath = this.getSourcePath();
        result = result * PRIME + ($sourcePath == null ? 43 : $sourcePath.hashCode());
        final Object $markers = this.getMarkers();
        result = result * PRIME + ($markers == null ? 43 : $markers.hashCode());
        final Object $charsetName = this.getCharsetName();
        result = result * PRIME + ($charsetName == null ? 43 : $charsetName.hashCode());
        final Object $fileAttributes = this.getFileAttributes();
        result = result * PRIME + ($fileAttributes == null ? 43 : $fileAttributes.hashCode());
        final Object $checksum = this.getChecksum();
        result = result * PRIME + ($checksum == null ? 43 : $checksum.hashCode());
        final Object $text = this.getText();
        result = result * PRIME + ($text == null ? 43 : $text.hashCode());
        final Object $snippets = this.getSnippets();
        result = result * PRIME + ($snippets == null ? 43 : $snippets.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "PlainText(id=" + this.getId() + ", sourcePath=" + this.getSourcePath() + ", markers=" + this.getMarkers() + ", charsetName=" + this.getCharsetName() + ", charsetBomMarked=" + this.isCharsetBomMarked() + ", fileAttributes=" + this.getFileAttributes() + ", checksum=" + this.getChecksum() + ", text=" + this.getText() + ", snippets=" + this.getSnippets() + ")";
    }

    public PlainText(final UUID id, final Path sourcePath, final Markers markers, @Nullable final String charsetName, final boolean charsetBomMarked, @Nullable final FileAttributes fileAttributes, @Nullable final Checksum checksum, final String text, final List<Snippet> snippets) {
        this.id = id;
        this.sourcePath = sourcePath;
        this.markers = markers;
        this.charsetName = charsetName;
        this.charsetBomMarked = charsetBomMarked;
        this.fileAttributes = fileAttributes;
        this.checksum = checksum;
        this.text = text;
        this.snippets = snippets;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public PlainText withId(final UUID id) {
        return this.id == id ? this : new PlainText(id, this.sourcePath, this.markers, this.charsetName, this.charsetBomMarked, this.fileAttributes, this.checksum, this.text, this.snippets);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public PlainText withSourcePath(final Path sourcePath) {
        return this.sourcePath == sourcePath ? this : new PlainText(this.id, sourcePath, this.markers, this.charsetName, this.charsetBomMarked, this.fileAttributes, this.checksum, this.text, this.snippets);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public PlainText withMarkers(final Markers markers) {
        return this.markers == markers ? this : new PlainText(this.id, this.sourcePath, markers, this.charsetName, this.charsetBomMarked, this.fileAttributes, this.checksum, this.text, this.snippets);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    private PlainText withCharsetName(@Nullable final String charsetName) {
        return this.charsetName == charsetName ? this : new PlainText(this.id, this.sourcePath, this.markers, charsetName, this.charsetBomMarked, this.fileAttributes, this.checksum, this.text, this.snippets);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public PlainText withCharsetBomMarked(final boolean charsetBomMarked) {
        return this.charsetBomMarked == charsetBomMarked ? this : new PlainText(this.id, this.sourcePath, this.markers, this.charsetName, charsetBomMarked, this.fileAttributes, this.checksum, this.text, this.snippets);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public PlainText withFileAttributes(@Nullable final FileAttributes fileAttributes) {
        return this.fileAttributes == fileAttributes ? this : new PlainText(this.id, this.sourcePath, this.markers, this.charsetName, this.charsetBomMarked, fileAttributes, this.checksum, this.text, this.snippets);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public PlainText withChecksum(@Nullable final Checksum checksum) {
        return this.checksum == checksum ? this : new PlainText(this.id, this.sourcePath, this.markers, this.charsetName, this.charsetBomMarked, this.fileAttributes, checksum, this.text, this.snippets);
    }

    @Nullable
    public Checksum getChecksum() {
        return this.checksum;
    }
}
