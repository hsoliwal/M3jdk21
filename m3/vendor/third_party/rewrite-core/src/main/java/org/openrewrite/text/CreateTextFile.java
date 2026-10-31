// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/text/CreateTextFile.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.text;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

public final class CreateTextFile extends ScanningRecipe<AtomicBoolean> {
    @Option(displayName = "File contents", description = "Multiline text content for the file.", example = "Some text.")
    private final String fileContents;
    @Option(displayName = "Relative file path", description = "File path of new file.", example = "foo/bar/baz.txt")
    private final String relativeFileName;
    @Option(displayName = "Overwrite existing file", description = "If there is an existing file, should it be overwritten.", required = false)
    @Nullable
    private final Boolean overwriteExisting;

    @Override
    public String getDisplayName() {
        return "Create text file";
    }

    @Override
    public String getDescription() {
        return "Creates a new plain text file.";
    }

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(true);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean shouldCreate) {
        return new CreateFileVisitor(Paths.get(relativeFileName), shouldCreate);
    }

    @Override
    public Collection<SourceFile> generate(AtomicBoolean shouldCreate, ExecutionContext ctx) {
        if (shouldCreate.get()) {
            return PlainTextParser.builder().build().parse(fileContents).map(brandNewFile -> (SourceFile) brandNewFile.withSourcePath(Paths.get(relativeFileName))).collect(Collectors.toList());
        }
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean created) {
        Path path = Paths.get(relativeFileName);
        return new TreeVisitor<SourceFile, ExecutionContext>() {
            @Override
            public SourceFile visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if ((created.get() || Boolean.TRUE.equals(overwriteExisting)) && path.equals(sourceFile.getSourcePath())) {
                    if (sourceFile instanceof PlainText) {
                        return ((PlainText) sourceFile).withText(fileContents);
                    }
                    PlainText plainText = PlainText.builder().id(sourceFile.getId()).sourcePath(sourceFile.getSourcePath()).fileAttributes(sourceFile.getFileAttributes()).charsetBomMarked(sourceFile.isCharsetBomMarked()).text(fileContents).build();
                    if (sourceFile.getCharset() != null) {
                        return plainText.withCharset(sourceFile.getCharset());
                    }
                    return plainText;
                }
                return sourceFile;
            }
        };
    }

    public CreateTextFile(final String fileContents, final String relativeFileName, @Nullable final Boolean overwriteExisting) {
        this.fileContents = fileContents;
        this.relativeFileName = relativeFileName;
        this.overwriteExisting = overwriteExisting;
    }

    public String getFileContents() {
        return this.fileContents;
    }

    public String getRelativeFileName() {
        return this.relativeFileName;
    }

    @Nullable
    public Boolean getOverwriteExisting() {
        return this.overwriteExisting;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "CreateTextFile(fileContents=" + this.getFileContents() + ", relativeFileName=" + this.getRelativeFileName() + ", overwriteExisting=" + this.getOverwriteExisting() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof CreateTextFile)) return false;
        final CreateTextFile other = (CreateTextFile) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$overwriteExisting = this.getOverwriteExisting();
        final Object other$overwriteExisting = other.getOverwriteExisting();
        if (this$overwriteExisting == null ? other$overwriteExisting != null : !this$overwriteExisting.equals(other$overwriteExisting)) return false;
        final Object this$fileContents = this.getFileContents();
        final Object other$fileContents = other.getFileContents();
        if (this$fileContents == null ? other$fileContents != null : !this$fileContents.equals(other$fileContents)) return false;
        final Object this$relativeFileName = this.getRelativeFileName();
        final Object other$relativeFileName = other.getRelativeFileName();
        if (this$relativeFileName == null ? other$relativeFileName != null : !this$relativeFileName.equals(other$relativeFileName)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof CreateTextFile;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $overwriteExisting = this.getOverwriteExisting();
        result = result * PRIME + ($overwriteExisting == null ? 43 : $overwriteExisting.hashCode());
        final Object $fileContents = this.getFileContents();
        result = result * PRIME + ($fileContents == null ? 43 : $fileContents.hashCode());
        final Object $relativeFileName = this.getRelativeFileName();
        result = result * PRIME + ($relativeFileName == null ? 43 : $relativeFileName.hashCode());
        return result;
    }
}
