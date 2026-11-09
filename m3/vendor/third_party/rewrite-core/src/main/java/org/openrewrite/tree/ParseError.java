// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/tree/ParseError.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.tree;

import org.openrewrite.*;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import static java.util.Collections.singletonList;

public final class ParseError implements SourceFile {
    private final UUID id;
    private final Markers markers;
    private final Path sourcePath;
    @Nullable
    private final FileAttributes fileAttributes;
    // for backwards compatibility
    @Nullable
    private final String charsetName;

    @Override
    public Charset getCharset() {
        return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
    }

    @Override
    public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
        return new ParseErrorPrinter<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public SourceFile withCharset(Charset charset) {
        return withCharsetName(charset.name());
    }

    private final boolean charsetBomMarked;
    @Nullable
    private final Checksum checksum;
    private final String text;
    /**
     * A parsed LST that was determined at parsing time to be erroneous, for
     * example if it doesn't faithfully produce the original source text at
     * printing time.
     */
    @Nullable
    private final SourceFile erroneous;

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(ParseErrorVisitor.class);
    }

    @Override
    public <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        //noinspection unchecked
        return (R) v.adapt(ParseErrorVisitor.class).visitParseError(this, p);
    }

    public static ParseError build(Parser parser, Parser.Input input, @Nullable Path relativeTo, ExecutionContext ctx, Throwable t) {
        EncodingDetectingInputStream is = input.getSource(ctx);
        return new ParseError(Tree.randomId(), new Markers(Tree.randomId(), singletonList(ParseExceptionResult.build(parser, t))), input.getRelativePath(relativeTo), input.getFileAttributes(), parser.getCharset(ctx).name(), is.isCharsetBomMarked(), null, is.readFully(), null);
    }

    public IllegalStateException toException() {
        if (getErroneous() != null) {
            return new IllegalStateException("Failed to parse " + getSourcePath() + " because of parse-to-print idempotence on: " + getText());
        } else {
            ParseExceptionResult ex = getMarkers().findFirst(ParseExceptionResult.class).orElseThrow(() -> new IllegalStateException("No ParseExceptionResult marker on parser failure"));
            return new IllegalStateException(ex.getExceptionType() + ": " + ex.getMessage());
        }
    }

    public ParseError(final UUID id, final Markers markers, final Path sourcePath, @Nullable final FileAttributes fileAttributes, @Nullable final String charsetName, final boolean charsetBomMarked, @Nullable final Checksum checksum, final String text, @Nullable final SourceFile erroneous) {
        this.id = id;
        this.markers = markers;
        this.sourcePath = sourcePath;
        this.fileAttributes = fileAttributes;
        this.charsetName = charsetName;
        this.charsetBomMarked = charsetBomMarked;
        this.checksum = checksum;
        this.text = text;
        this.erroneous = erroneous;
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

    public String getText() {
        return this.text;
    }

    /**
     * A parsed LST that was determined at parsing time to be erroneous, for
     * example if it doesn't faithfully produce the original source text at
     * printing time.
     */
    @Nullable
    public SourceFile getErroneous() {
        return this.erroneous;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ParseError)) return false;
        final ParseError other = (ParseError) o;
        if (this.isCharsetBomMarked() != other.isCharsetBomMarked()) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$markers = this.getMarkers();
        final Object other$markers = other.getMarkers();
        if (this$markers == null ? other$markers != null : !this$markers.equals(other$markers)) return false;
        final Object this$sourcePath = this.getSourcePath();
        final Object other$sourcePath = other.getSourcePath();
        if (this$sourcePath == null ? other$sourcePath != null : !this$sourcePath.equals(other$sourcePath)) return false;
        final Object this$fileAttributes = this.getFileAttributes();
        final Object other$fileAttributes = other.getFileAttributes();
        if (this$fileAttributes == null ? other$fileAttributes != null : !this$fileAttributes.equals(other$fileAttributes)) return false;
        final Object this$charsetName = this.getCharsetName();
        final Object other$charsetName = other.getCharsetName();
        if (this$charsetName == null ? other$charsetName != null : !this$charsetName.equals(other$charsetName)) return false;
        final Object this$checksum = this.getChecksum();
        final Object other$checksum = other.getChecksum();
        if (this$checksum == null ? other$checksum != null : !this$checksum.equals(other$checksum)) return false;
        final Object this$text = this.getText();
        final Object other$text = other.getText();
        if (this$text == null ? other$text != null : !this$text.equals(other$text)) return false;
        final Object this$erroneous = this.getErroneous();
        final Object other$erroneous = other.getErroneous();
        if (this$erroneous == null ? other$erroneous != null : !this$erroneous.equals(other$erroneous)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isCharsetBomMarked() ? 79 : 97);
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $markers = this.getMarkers();
        result = result * PRIME + ($markers == null ? 43 : $markers.hashCode());
        final Object $sourcePath = this.getSourcePath();
        result = result * PRIME + ($sourcePath == null ? 43 : $sourcePath.hashCode());
        final Object $fileAttributes = this.getFileAttributes();
        result = result * PRIME + ($fileAttributes == null ? 43 : $fileAttributes.hashCode());
        final Object $charsetName = this.getCharsetName();
        result = result * PRIME + ($charsetName == null ? 43 : $charsetName.hashCode());
        final Object $checksum = this.getChecksum();
        result = result * PRIME + ($checksum == null ? 43 : $checksum.hashCode());
        final Object $text = this.getText();
        result = result * PRIME + ($text == null ? 43 : $text.hashCode());
        final Object $erroneous = this.getErroneous();
        result = result * PRIME + ($erroneous == null ? 43 : $erroneous.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ParseError(id=" + this.getId() + ", markers=" + this.getMarkers() + ", sourcePath=" + this.getSourcePath() + ", fileAttributes=" + this.getFileAttributes() + ", charsetName=" + this.getCharsetName() + ", charsetBomMarked=" + this.isCharsetBomMarked() + ", checksum=" + this.getChecksum() + ", text=" + this.getText() + ", erroneous=" + this.getErroneous() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ParseError withId(final UUID id) {
        return this.id == id ? this : new ParseError(id, this.markers, this.sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.text, this.erroneous);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ParseError withMarkers(final Markers markers) {
        return this.markers == markers ? this : new ParseError(this.id, markers, this.sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.text, this.erroneous);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ParseError withSourcePath(final Path sourcePath) {
        return this.sourcePath == sourcePath ? this : new ParseError(this.id, this.markers, sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.text, this.erroneous);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ParseError withFileAttributes(@Nullable final FileAttributes fileAttributes) {
        return this.fileAttributes == fileAttributes ? this : new ParseError(this.id, this.markers, this.sourcePath, fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.text, this.erroneous);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ParseError withCharsetBomMarked(final boolean charsetBomMarked) {
        return this.charsetBomMarked == charsetBomMarked ? this : new ParseError(this.id, this.markers, this.sourcePath, this.fileAttributes, this.charsetName, charsetBomMarked, this.checksum, this.text, this.erroneous);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ParseError withChecksum(@Nullable final Checksum checksum) {
        return this.checksum == checksum ? this : new ParseError(this.id, this.markers, this.sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, checksum, this.text, this.erroneous);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ParseError withText(final String text) {
        return this.text == text ? this : new ParseError(this.id, this.markers, this.sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, text, this.erroneous);
    }

    /**
     * A parsed LST that was determined at parsing time to be erroneous, for
     * example if it doesn't faithfully produce the original source text at
     * printing time.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ParseError withErroneous(@Nullable final SourceFile erroneous) {
        return this.erroneous == erroneous ? this : new ParseError(this.id, this.markers, this.sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.text, erroneous);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    private ParseError withCharsetName(@Nullable final String charsetName) {
        return this.charsetName == charsetName ? this : new ParseError(this.id, this.markers, this.sourcePath, this.fileAttributes, charsetName, this.charsetBomMarked, this.checksum, this.text, this.erroneous);
    }
}
