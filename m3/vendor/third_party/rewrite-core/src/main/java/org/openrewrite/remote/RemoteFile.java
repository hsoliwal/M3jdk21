// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/remote/RemoteFile.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.remote;

import org.intellij.lang.annotations.Language;
import org.openrewrite.Checksum;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.marker.Markers;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class RemoteFile implements Remote {
    private final UUID id;
    private final Path sourcePath;
    private final Markers markers;
    private final URI uri;
    @Nullable
    private final Charset charset;
    private final boolean charsetBomMarked;
    @Nullable
    private final FileAttributes fileAttributes;
    @Language("markdown")
    private final String description;
    @Nullable
    private final Checksum checksum;

    @Override
    public InputStream getInputStream(ExecutionContext ctx) {
        HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getLargeFileHttpSender();
        RemoteArtifactCache cache = RemoteExecutionContextView.view(ctx).getArtifactCache();
        try {
            Path localFile = cache.compute(uri, () -> {
                //noinspection resource
                HttpSender.Response response = httpSender.get(uri.toString()).send();
                return response.getBody();
            }, ctx.getOnError());
            if (localFile == null) {
                throw new IllegalStateException("Failed to download " + uri + " to artifact cache");
            }
            return Files.newInputStream(localFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to download " + uri + " to temporary file", e);
        }
    }

    public RemoteFile(final UUID id, final Path sourcePath, final Markers markers, final URI uri, @Nullable final Charset charset, final boolean charsetBomMarked, @Nullable final FileAttributes fileAttributes, @Language("markdown") final String description, @Nullable final Checksum checksum) {
        this.id = id;
        this.sourcePath = sourcePath;
        this.markers = markers;
        this.uri = uri;
        this.charset = charset;
        this.charsetBomMarked = charsetBomMarked;
        this.fileAttributes = fileAttributes;
        this.description = description;
        this.checksum = checksum;
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

    public URI getUri() {
        return this.uri;
    }

    @Nullable
    public Charset getCharset() {
        return this.charset;
    }

    public boolean isCharsetBomMarked() {
        return this.charsetBomMarked;
    }

    @Nullable
    public FileAttributes getFileAttributes() {
        return this.fileAttributes;
    }

    @Language("markdown")
    public String getDescription() {
        return this.description;
    }

    @Nullable
    public Checksum getChecksum() {
        return this.checksum;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "RemoteFile(id=" + this.getId() + ", sourcePath=" + this.getSourcePath() + ", markers=" + this.getMarkers() + ", uri=" + this.getUri() + ", charset=" + this.getCharset() + ", charsetBomMarked=" + this.isCharsetBomMarked() + ", fileAttributes=" + this.getFileAttributes() + ", description=" + this.getDescription() + ", checksum=" + this.getChecksum() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof RemoteFile)) return false;
        final RemoteFile other = (RemoteFile) o;
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
    public RemoteFile withId(final UUID id) {
        return this.id == id ? this : new RemoteFile(id, this.sourcePath, this.markers, this.uri, this.charset, this.charsetBomMarked, this.fileAttributes, this.description, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteFile withSourcePath(final Path sourcePath) {
        return this.sourcePath == sourcePath ? this : new RemoteFile(this.id, sourcePath, this.markers, this.uri, this.charset, this.charsetBomMarked, this.fileAttributes, this.description, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteFile withMarkers(final Markers markers) {
        return this.markers == markers ? this : new RemoteFile(this.id, this.sourcePath, markers, this.uri, this.charset, this.charsetBomMarked, this.fileAttributes, this.description, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteFile withUri(final URI uri) {
        return this.uri == uri ? this : new RemoteFile(this.id, this.sourcePath, this.markers, uri, this.charset, this.charsetBomMarked, this.fileAttributes, this.description, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteFile withCharset(@Nullable final Charset charset) {
        return this.charset == charset ? this : new RemoteFile(this.id, this.sourcePath, this.markers, this.uri, charset, this.charsetBomMarked, this.fileAttributes, this.description, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteFile withCharsetBomMarked(final boolean charsetBomMarked) {
        return this.charsetBomMarked == charsetBomMarked ? this : new RemoteFile(this.id, this.sourcePath, this.markers, this.uri, this.charset, charsetBomMarked, this.fileAttributes, this.description, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteFile withFileAttributes(@Nullable final FileAttributes fileAttributes) {
        return this.fileAttributes == fileAttributes ? this : new RemoteFile(this.id, this.sourcePath, this.markers, this.uri, this.charset, this.charsetBomMarked, fileAttributes, this.description, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteFile withDescription(@Language("markdown") final String description) {
        return this.description == description ? this : new RemoteFile(this.id, this.sourcePath, this.markers, this.uri, this.charset, this.charsetBomMarked, this.fileAttributes, description, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteFile withChecksum(@Nullable final Checksum checksum) {
        return this.checksum == checksum ? this : new RemoteFile(this.id, this.sourcePath, this.markers, this.uri, this.charset, this.charsetBomMarked, this.fileAttributes, this.description, checksum);
    }
}
