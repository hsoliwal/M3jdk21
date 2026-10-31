// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/quark/Quark.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.quark;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.UUID;

public final class Quark implements SourceFile {
    private final UUID id;
    private final Path sourcePath;
    private final Markers markers;
    @Nullable
    private final Checksum checksum;
    @Nullable
    private final FileAttributes fileAttributes;

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(QuarkVisitor.class);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    @Nullable
    public <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        //noinspection unchecked
        return (R) v.adapt(QuarkVisitor.class).visitQuark(this, p);
    }

    @Override
    public Charset getCharset() {
        throw new UnsupportedOperationException("The contents of a quark are unknown, so the charset is unknown.");
    }

    @SuppressWarnings("unchecked")
    @Override
    public SourceFile withCharset(Charset charset) {
        throw new UnsupportedOperationException("The contents of a quark are unknown, so the charset is unknown.");
    }

    @Override
    public boolean isCharsetBomMarked() {
        throw new UnsupportedOperationException("The contents of a quark are unknown, so the charset is unknown.");
    }

    @SuppressWarnings("unchecked")
    @Override
    public SourceFile withCharsetBomMarked(boolean marked) {
        throw new UnsupportedOperationException("The contents of a quark are unknown, so the charset is unknown.");
    }

    @Override
    public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
        return new QuarkPrinter<>();
    }

    public Quark(final UUID id, final Path sourcePath, final Markers markers, @Nullable final Checksum checksum, @Nullable final FileAttributes fileAttributes) {
        this.id = id;
        this.sourcePath = sourcePath;
        this.markers = markers;
        this.checksum = checksum;
        this.fileAttributes = fileAttributes;
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
    public Checksum getChecksum() {
        return this.checksum;
    }

    @Nullable
    public FileAttributes getFileAttributes() {
        return this.fileAttributes;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof Quark)) return false;
        final Quark other = (Quark) o;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$sourcePath = this.getSourcePath();
        final Object other$sourcePath = other.getSourcePath();
        if (this$sourcePath == null ? other$sourcePath != null : !this$sourcePath.equals(other$sourcePath)) return false;
        final Object this$markers = this.getMarkers();
        final Object other$markers = other.getMarkers();
        if (this$markers == null ? other$markers != null : !this$markers.equals(other$markers)) return false;
        final Object this$checksum = this.getChecksum();
        final Object other$checksum = other.getChecksum();
        if (this$checksum == null ? other$checksum != null : !this$checksum.equals(other$checksum)) return false;
        final Object this$fileAttributes = this.getFileAttributes();
        final Object other$fileAttributes = other.getFileAttributes();
        if (this$fileAttributes == null ? other$fileAttributes != null : !this$fileAttributes.equals(other$fileAttributes)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $sourcePath = this.getSourcePath();
        result = result * PRIME + ($sourcePath == null ? 43 : $sourcePath.hashCode());
        final Object $markers = this.getMarkers();
        result = result * PRIME + ($markers == null ? 43 : $markers.hashCode());
        final Object $checksum = this.getChecksum();
        result = result * PRIME + ($checksum == null ? 43 : $checksum.hashCode());
        final Object $fileAttributes = this.getFileAttributes();
        result = result * PRIME + ($fileAttributes == null ? 43 : $fileAttributes.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "Quark(id=" + this.getId() + ", sourcePath=" + this.getSourcePath() + ", markers=" + this.getMarkers() + ", checksum=" + this.getChecksum() + ", fileAttributes=" + this.getFileAttributes() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public Quark withId(final UUID id) {
        return this.id == id ? this : new Quark(id, this.sourcePath, this.markers, this.checksum, this.fileAttributes);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public Quark withSourcePath(final Path sourcePath) {
        return this.sourcePath == sourcePath ? this : new Quark(this.id, sourcePath, this.markers, this.checksum, this.fileAttributes);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public Quark withMarkers(final Markers markers) {
        return this.markers == markers ? this : new Quark(this.id, this.sourcePath, markers, this.checksum, this.fileAttributes);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public Quark withChecksum(@Nullable final Checksum checksum) {
        return this.checksum == checksum ? this : new Quark(this.id, this.sourcePath, this.markers, checksum, this.fileAttributes);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public Quark withFileAttributes(@Nullable final FileAttributes fileAttributes) {
        return this.fileAttributes == fileAttributes ? this : new Quark(this.id, this.sourcePath, this.markers, this.checksum, fileAttributes);
    }
}
