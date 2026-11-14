// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/remote/RemoteArchive.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Represents a source to be extracted from within an archive hosted at a remote URI.
 * If you want to download and retain the entire archive, use {@link RemoteFile}.
 * Useful when a Recipe wishes to create a SourceFile based on something specific from within a remote archive, but not
 * the entire archive.
 * <p>
 * Downloading and extracting the correct file from within the archive are not handled during Recipe execution.
 * Post-processing of Recipe results by a build plugin or other caller of OpenRewrite is responsible for this.
 */
public final class RemoteArchive implements Remote {
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
    /**
     * A set of regular expressions that match consecutively nested paths within an archive, starting
     * with the path of the topmost archive itself. For example:
     * <p/>
     * <pre>
     *     gradle-[^\/]+\/(?:.*\/)+gradle-wrapper-(?!shared).*\.jar
     *     gradle-wrapper\.jar
     * </pre>
     */
    private final List<String> paths;
    @Nullable
    private final Checksum checksum;

    @Override
    public InputStream getInputStream(ExecutionContext ctx) {
        HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getLargeFileHttpSender();
        RemoteArtifactCache cache = RemoteExecutionContextView.view(ctx).getArtifactCache();
        try {
            Path localArchive = cache.compute(uri, () -> {
                //noinspection resource
                HttpSender.Response response = httpSender.send(httpSender.get(uri.toString()).build());
                return response.getBody();
            }, ctx.getOnError());
            if (localArchive == null) {
                throw new IllegalStateException("Failed to download " + uri + " to artifact cache");
            }
            InputStream body = Files.newInputStream(localArchive);
            InputStream inner = readIntoArchive(body, paths, 0);
            if (inner == null) {
                throw new IllegalArgumentException("Unable to find path " + paths + " in zip file " + uri);
            }
            return inner;
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to download " + uri + " to file", e);
        }
    }

    @Nullable
    private InputStream readIntoArchive(InputStream body, List<String> paths, int index) {
        ZipInputStream zis = new ZipInputStream(body);
        Pattern pattern = Pattern.compile(paths.get(index));
        try {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (pattern.matcher(entry.getName()).matches()) {
                    if (paths.size() == index + 1) {
                        return new InputStream() {
                            @Override
                            public int read() throws IOException {
                                return zis.read();
                            }
                            @Override
                            public void close() throws IOException {
                                zis.closeEntry();
                                zis.close();
                            }
                        };
                    } else {
                        InputStream maybeInputStream = readIntoArchive(zis, paths, index + 1);
                        if (maybeInputStream != null) {
                            return maybeInputStream;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to load path " + paths + " in zip file " + uri, e);
        }
        return null;
    }

    public RemoteArchive(final UUID id, final Path sourcePath, final Markers markers, final URI uri, @Nullable final Charset charset, final boolean charsetBomMarked, @Nullable final FileAttributes fileAttributes, @Language("markdown") final String description, final List<String> paths, @Nullable final Checksum checksum) {
        this.id = id;
        this.sourcePath = sourcePath;
        this.markers = markers;
        this.uri = uri;
        this.charset = charset;
        this.charsetBomMarked = charsetBomMarked;
        this.fileAttributes = fileAttributes;
        this.description = description;
        this.paths = paths;
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

    /**
     * A set of regular expressions that match consecutively nested paths within an archive, starting
     * with the path of the topmost archive itself. For example:
     * <p/>
     * <pre>
     *     gradle-[^\/]+\/(?:.*\/)+gradle-wrapper-(?!shared).*\.jar
     *     gradle-wrapper\.jar
     * </pre>
     */
    public List<String> getPaths() {
        return this.paths;
    }

    @Nullable
    public Checksum getChecksum() {
        return this.checksum;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "RemoteArchive(id=" + this.getId() + ", sourcePath=" + this.getSourcePath() + ", markers=" + this.getMarkers() + ", uri=" + this.getUri() + ", charset=" + this.getCharset() + ", charsetBomMarked=" + this.isCharsetBomMarked() + ", fileAttributes=" + this.getFileAttributes() + ", description=" + this.getDescription() + ", paths=" + this.getPaths() + ", checksum=" + this.getChecksum() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof RemoteArchive)) return false;
        final RemoteArchive other = (RemoteArchive) o;
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
    public RemoteArchive withId(final UUID id) {
        return this.id == id ? this : new RemoteArchive(id, this.sourcePath, this.markers, this.uri, this.charset, this.charsetBomMarked, this.fileAttributes, this.description, this.paths, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteArchive withSourcePath(final Path sourcePath) {
        return this.sourcePath == sourcePath ? this : new RemoteArchive(this.id, sourcePath, this.markers, this.uri, this.charset, this.charsetBomMarked, this.fileAttributes, this.description, this.paths, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteArchive withMarkers(final Markers markers) {
        return this.markers == markers ? this : new RemoteArchive(this.id, this.sourcePath, markers, this.uri, this.charset, this.charsetBomMarked, this.fileAttributes, this.description, this.paths, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteArchive withUri(final URI uri) {
        return this.uri == uri ? this : new RemoteArchive(this.id, this.sourcePath, this.markers, uri, this.charset, this.charsetBomMarked, this.fileAttributes, this.description, this.paths, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteArchive withCharset(@Nullable final Charset charset) {
        return this.charset == charset ? this : new RemoteArchive(this.id, this.sourcePath, this.markers, this.uri, charset, this.charsetBomMarked, this.fileAttributes, this.description, this.paths, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteArchive withCharsetBomMarked(final boolean charsetBomMarked) {
        return this.charsetBomMarked == charsetBomMarked ? this : new RemoteArchive(this.id, this.sourcePath, this.markers, this.uri, this.charset, charsetBomMarked, this.fileAttributes, this.description, this.paths, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteArchive withFileAttributes(@Nullable final FileAttributes fileAttributes) {
        return this.fileAttributes == fileAttributes ? this : new RemoteArchive(this.id, this.sourcePath, this.markers, this.uri, this.charset, this.charsetBomMarked, fileAttributes, this.description, this.paths, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteArchive withDescription(@Language("markdown") final String description) {
        return this.description == description ? this : new RemoteArchive(this.id, this.sourcePath, this.markers, this.uri, this.charset, this.charsetBomMarked, this.fileAttributes, description, this.paths, this.checksum);
    }

    /**
     * A set of regular expressions that match consecutively nested paths within an archive, starting
     * with the path of the topmost archive itself. For example:
     * <p/>
     * <pre>
     *     gradle-[^\/]+\/(?:.*\/)+gradle-wrapper-(?!shared).*\.jar
     *     gradle-wrapper\.jar
     * </pre>
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteArchive withPaths(final List<String> paths) {
        return this.paths == paths ? this : new RemoteArchive(this.id, this.sourcePath, this.markers, this.uri, this.charset, this.charsetBomMarked, this.fileAttributes, this.description, paths, this.checksum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public RemoteArchive withChecksum(@Nullable final Checksum checksum) {
        return this.checksum == checksum ? this : new RemoteArchive(this.id, this.sourcePath, this.markers, this.uri, this.charset, this.charsetBomMarked, this.fileAttributes, this.description, this.paths, checksum);
    }
}
