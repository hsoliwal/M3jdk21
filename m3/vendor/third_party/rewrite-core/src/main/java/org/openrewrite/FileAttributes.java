// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/FileAttributes.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class FileAttributes {
    @Nullable
    private final ZonedDateTime creationTime;
    @Nullable
    private final ZonedDateTime lastModifiedTime;
    @Nullable
    private final ZonedDateTime lastAccessTime;
    private final boolean isReadable;
    private final boolean isWritable;
    private final boolean isExecutable;
    private final long size;

    @Nullable
    public static FileAttributes fromPath(Path path) {
        if (Files.exists(path)) {
            try {
                BasicFileAttributes basicFileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
                return new FileAttributes(ZonedDateTime.from(basicFileAttributes.creationTime().toInstant().atZone(ZoneId.systemDefault())), ZonedDateTime.from(basicFileAttributes.lastAccessTime().toInstant().atZone(ZoneId.systemDefault())), ZonedDateTime.from(basicFileAttributes.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault())), Files.isReadable(path), Files.isWritable(path), Files.isExecutable(path), basicFileAttributes.size());
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    public FileAttributes(@Nullable final ZonedDateTime creationTime, @Nullable final ZonedDateTime lastModifiedTime, @Nullable final ZonedDateTime lastAccessTime, final boolean isReadable, final boolean isWritable, final boolean isExecutable, final long size) {
        this.creationTime = creationTime;
        this.lastModifiedTime = lastModifiedTime;
        this.lastAccessTime = lastAccessTime;
        this.isReadable = isReadable;
        this.isWritable = isWritable;
        this.isExecutable = isExecutable;
        this.size = size;
    }

    @Nullable
    public ZonedDateTime getCreationTime() {
        return this.creationTime;
    }

    @Nullable
    public ZonedDateTime getLastModifiedTime() {
        return this.lastModifiedTime;
    }

    @Nullable
    public ZonedDateTime getLastAccessTime() {
        return this.lastAccessTime;
    }

    public boolean isReadable() {
        return this.isReadable;
    }

    public boolean isWritable() {
        return this.isWritable;
    }

    public boolean isExecutable() {
        return this.isExecutable;
    }

    public long getSize() {
        return this.size;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FileAttributes)) return false;
        final FileAttributes other = (FileAttributes) o;
        if (this.isReadable() != other.isReadable()) return false;
        if (this.isWritable() != other.isWritable()) return false;
        if (this.isExecutable() != other.isExecutable()) return false;
        if (this.getSize() != other.getSize()) return false;
        final Object this$creationTime = this.getCreationTime();
        final Object other$creationTime = other.getCreationTime();
        if (this$creationTime == null ? other$creationTime != null : !this$creationTime.equals(other$creationTime)) return false;
        final Object this$lastModifiedTime = this.getLastModifiedTime();
        final Object other$lastModifiedTime = other.getLastModifiedTime();
        if (this$lastModifiedTime == null ? other$lastModifiedTime != null : !this$lastModifiedTime.equals(other$lastModifiedTime)) return false;
        final Object this$lastAccessTime = this.getLastAccessTime();
        final Object other$lastAccessTime = other.getLastAccessTime();
        if (this$lastAccessTime == null ? other$lastAccessTime != null : !this$lastAccessTime.equals(other$lastAccessTime)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isReadable() ? 79 : 97);
        result = result * PRIME + (this.isWritable() ? 79 : 97);
        result = result * PRIME + (this.isExecutable() ? 79 : 97);
        final long $size = this.getSize();
        result = result * PRIME + (int) ($size >>> 32 ^ $size);
        final Object $creationTime = this.getCreationTime();
        result = result * PRIME + ($creationTime == null ? 43 : $creationTime.hashCode());
        final Object $lastModifiedTime = this.getLastModifiedTime();
        result = result * PRIME + ($lastModifiedTime == null ? 43 : $lastModifiedTime.hashCode());
        final Object $lastAccessTime = this.getLastAccessTime();
        result = result * PRIME + ($lastAccessTime == null ? 43 : $lastAccessTime.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FileAttributes(creationTime=" + this.getCreationTime() + ", lastModifiedTime=" + this.getLastModifiedTime() + ", lastAccessTime=" + this.getLastAccessTime() + ", isReadable=" + this.isReadable() + ", isWritable=" + this.isWritable() + ", isExecutable=" + this.isExecutable() + ", size=" + this.getSize() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public FileAttributes withCreationTime(@Nullable final ZonedDateTime creationTime) {
        return this.creationTime == creationTime ? this : new FileAttributes(creationTime, this.lastModifiedTime, this.lastAccessTime, this.isReadable, this.isWritable, this.isExecutable, this.size);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public FileAttributes withLastModifiedTime(@Nullable final ZonedDateTime lastModifiedTime) {
        return this.lastModifiedTime == lastModifiedTime ? this : new FileAttributes(this.creationTime, lastModifiedTime, this.lastAccessTime, this.isReadable, this.isWritable, this.isExecutable, this.size);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public FileAttributes withLastAccessTime(@Nullable final ZonedDateTime lastAccessTime) {
        return this.lastAccessTime == lastAccessTime ? this : new FileAttributes(this.creationTime, this.lastModifiedTime, lastAccessTime, this.isReadable, this.isWritable, this.isExecutable, this.size);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public FileAttributes withReadable(final boolean isReadable) {
        return this.isReadable == isReadable ? this : new FileAttributes(this.creationTime, this.lastModifiedTime, this.lastAccessTime, isReadable, this.isWritable, this.isExecutable, this.size);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public FileAttributes withWritable(final boolean isWritable) {
        return this.isWritable == isWritable ? this : new FileAttributes(this.creationTime, this.lastModifiedTime, this.lastAccessTime, this.isReadable, isWritable, this.isExecutable, this.size);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public FileAttributes withExecutable(final boolean isExecutable) {
        return this.isExecutable == isExecutable ? this : new FileAttributes(this.creationTime, this.lastModifiedTime, this.lastAccessTime, this.isReadable, this.isWritable, isExecutable, this.size);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public FileAttributes withSize(final long size) {
        return this.size == size ? this : new FileAttributes(this.creationTime, this.lastModifiedTime, this.lastAccessTime, this.isReadable, this.isWritable, this.isExecutable, size);
    }
}
