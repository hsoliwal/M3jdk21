// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/CreateEmptyJavaClass.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static java.util.Collections.emptyList;

public final class CreateEmptyJavaClass extends ScanningRecipe<AtomicBoolean> {
    @JsonIgnore
    private static final String NEW_CLASS_TEMPLATE = "package %s;\n\n%sclass %s {\n}";
    @Option(displayName = "Source root", description = "The source root of the new class file.", example = "src/main/java")
    private final String sourceRoot;
    @Option(displayName = "Package name", description = "The package of the new class.", example = "org.openrewrite.example")
    private final String packageName;
    @Option(displayName = "Modifier", description = "The class modifier.", valid = {"public", "private", "protected", "package-private"}, example = "public")
    private final String modifier;
    @Option(displayName = "Class name", description = "File path of new file.", example = "ExampleClass")
    private final String className;
    @Option(displayName = "Overwrite existing file", description = "If there is an existing file, should it be overwritten.", required = false)
    @Nullable
    private final Boolean overwriteExisting;
    @Option(displayName = "Relative directory path", description = "Directory path of new class.", required = false, example = "foo/bar")
    @Nullable
    private final String relativePath;

    @Override
    public String getDisplayName() {
        return "Create Java class";
    }

    @Override
    public String getDescription() {
        return "Create a new, empty Java class.";
    }

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(true);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean shouldCreate) {
        return new CreateFileVisitor(getSourcePath(), shouldCreate);
    }

    @Override
    public Collection<SourceFile> generate(AtomicBoolean shouldCreate, ExecutionContext ctx) {
        if (shouldCreate.get()) {
            return createEmptyClass().collect(Collectors.toList());
        }
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean created) {
        Path path = getSourcePath();
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                if ((created.get() || Boolean.TRUE.equals(overwriteExisting)) && path.equals(cu.getSourcePath())) {
                    Optional<SourceFile> sourceFile = createEmptyClass().findFirst();
                    if (sourceFile.isPresent() && sourceFile.get() instanceof J.CompilationUnit) {
                        J.CompilationUnit newCu = (J.CompilationUnit) sourceFile.get();
                        return cu.withClasses(newCu.getClasses()).withSourcePath(path);
                    }
                }
                return cu;
            }
        };
    }

    private Stream<SourceFile> createEmptyClass() {
        String packageModifier = "package-private".equals(modifier) ? "" : modifier + " ";
        return JavaParser.fromJavaVersion().build().parse(String.format(CreateEmptyJavaClass.NEW_CLASS_TEMPLATE, packageName, packageModifier, className)).map(brandNewFile -> brandNewFile.withSourcePath(getSourcePath()));
    }

    @SuppressWarnings("java:S1075")
    private Path getSourcePath() {
        String path = relativePath;
        if (path == null) {
            path = "";
        }
        if (!path.isEmpty() && !path.endsWith("/")) {
            path = path + "/";
        }
        return Paths.get(String.format("%s%s/%s/%s.java", path, sourceRoot, packageName.replace('.', '/'), className));
    }

    public CreateEmptyJavaClass(final String sourceRoot, final String packageName, final String modifier, final String className, @Nullable final Boolean overwriteExisting, @Nullable final String relativePath) {
        this.sourceRoot = sourceRoot;
        this.packageName = packageName;
        this.modifier = modifier;
        this.className = className;
        this.overwriteExisting = overwriteExisting;
        this.relativePath = relativePath;
    }

    public String getSourceRoot() {
        return this.sourceRoot;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public String getModifier() {
        return this.modifier;
    }

    public String getClassName() {
        return this.className;
    }

    @Nullable
    public Boolean getOverwriteExisting() {
        return this.overwriteExisting;
    }

    @Nullable
    public String getRelativePath() {
        return this.relativePath;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "CreateEmptyJavaClass(sourceRoot=" + this.getSourceRoot() + ", packageName=" + this.getPackageName() + ", modifier=" + this.getModifier() + ", className=" + this.getClassName() + ", overwriteExisting=" + this.getOverwriteExisting() + ", relativePath=" + this.getRelativePath() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof CreateEmptyJavaClass)) return false;
        final CreateEmptyJavaClass other = (CreateEmptyJavaClass) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$overwriteExisting = this.getOverwriteExisting();
        final Object other$overwriteExisting = other.getOverwriteExisting();
        if (this$overwriteExisting == null ? other$overwriteExisting != null : !this$overwriteExisting.equals(other$overwriteExisting)) return false;
        final Object this$sourceRoot = this.getSourceRoot();
        final Object other$sourceRoot = other.getSourceRoot();
        if (this$sourceRoot == null ? other$sourceRoot != null : !this$sourceRoot.equals(other$sourceRoot)) return false;
        final Object this$packageName = this.getPackageName();
        final Object other$packageName = other.getPackageName();
        if (this$packageName == null ? other$packageName != null : !this$packageName.equals(other$packageName)) return false;
        final Object this$modifier = this.getModifier();
        final Object other$modifier = other.getModifier();
        if (this$modifier == null ? other$modifier != null : !this$modifier.equals(other$modifier)) return false;
        final Object this$className = this.getClassName();
        final Object other$className = other.getClassName();
        if (this$className == null ? other$className != null : !this$className.equals(other$className)) return false;
        final Object this$relativePath = this.getRelativePath();
        final Object other$relativePath = other.getRelativePath();
        if (this$relativePath == null ? other$relativePath != null : !this$relativePath.equals(other$relativePath)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof CreateEmptyJavaClass;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $overwriteExisting = this.getOverwriteExisting();
        result = result * PRIME + ($overwriteExisting == null ? 43 : $overwriteExisting.hashCode());
        final Object $sourceRoot = this.getSourceRoot();
        result = result * PRIME + ($sourceRoot == null ? 43 : $sourceRoot.hashCode());
        final Object $packageName = this.getPackageName();
        result = result * PRIME + ($packageName == null ? 43 : $packageName.hashCode());
        final Object $modifier = this.getModifier();
        result = result * PRIME + ($modifier == null ? 43 : $modifier.hashCode());
        final Object $className = this.getClassName();
        result = result * PRIME + ($className == null ? 43 : $className.hashCode());
        final Object $relativePath = this.getRelativePath();
        result = result * PRIME + ($relativePath == null ? 43 : $relativePath.hashCode());
        return result;
    }
}
