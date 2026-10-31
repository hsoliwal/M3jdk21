// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/marker/JavaSourceSet.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.marker;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SourceSet;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import static org.openrewrite.Tree.randomId;

public final class JavaSourceSet implements SourceSet {
    private final UUID id;
    private final String name;
    private final List<JavaType.FullyQualified> classpath;

    /**
     * Extract type information from the provided classpath.
     *
     * @param fullTypeInformation when false classpath will be filled with shallow types (effectively just fully-qualified names).
     *                            when true a much more memory-intensive, time-consuming approach will extract full type information
     */
    public static JavaSourceSet build(String sourceSetName, Collection<Path> classpath, JavaTypeCache ignore, boolean fullTypeInformation) {
        if (fullTypeInformation) {
            throw new UnsupportedOperationException();
        }
        List<String> typeNames;
        if (!classpath.iterator().hasNext()) {
            // Only load JRE-provided types
            try (ScanResult scanResult = new ClassGraph().enableClassInfo().enableSystemJarsAndModules().acceptPackages("java").ignoreClassVisibility().scan()) {
                typeNames = packagesToTypeDeclarations(scanResult);
            }
        } else {
            // Load types from the classpath
            try (ScanResult scanResult = new ClassGraph().overrideClasspath(classpath).enableSystemJarsAndModules().enableClassInfo().ignoreClassVisibility().scan()) {
                typeNames = packagesToTypeDeclarations(scanResult);
            }
        }
        // Peculiarly, Classgraph will not return a ClassInfo for java.lang.Object, although it does for all other java.lang types
        typeNames.add("java.lang.Object");
        return new JavaSourceSet(randomId(), sourceSetName, typesFrom(typeNames));
    }

    /*
     * Create a map of package names to types contained within that package. Type names are not fully qualified, except for type parameter bounds.
     * e.g.: "java.util" -> [List, Date]
     */
    private static List<String> packagesToTypeDeclarations(ScanResult scanResult) {
        List<String> result = new ArrayList<>();
        for (ClassInfo classInfo : scanResult.getAllClasses()) {
            // Skip private classes, allowing package-private
            if (classInfo.isAnonymousInnerClass() || classInfo.isPrivate() || classInfo.isSynthetic() || classInfo.getName().contains(".enum.")) {
                continue;
            }
            if (classInfo.isStandardClass() && !classInfo.getName().startsWith("java.")) {
                continue;
            }
            // Although the classfile says its bytecode version is 50 (within the range Java 8 supports),
            // the Java 8 compiler says these class files from kotlin-reflect are invalid
            // The error is severe enough that all subsequent stubs have missing type information, so exclude that package
            if (classInfo.getPackageName().startsWith("kotlin.reflect.jvm.internal.impl.resolve.jvm")) {
                continue;
            }
            String typeDeclaration = declarableFullyQualifiedName(classInfo);
            if (typeDeclaration == null) {
                continue;
            }
            result.add(typeDeclaration);
        }
        return result;
    }

    private static List<JavaType.FullyQualified> typesFrom(List<String> typeNames) {
        List<JavaType.FullyQualified> types = new ArrayList<>(typeNames.size());
        for (String typeName : typeNames) {
            types.add(JavaType.ShallowClass.build(typeName));
        }
        return types;
    }

    /**
     * Java allows "$" in class names, and also uses "$" as part of the names of inner classes. e.g.: OuterClass$InnerClass
     * So if you only look at the textual representation of a class name, you can't tell if "A$B" means "class A$B {}" or "class A { class B {}}"
     * The declarable name of "class A$B {}" is "A$B"
     * The declarable name of class B in "class A { class B {}}" is "A.B"
     * ClassInfo.getPackageName() does not understand this and will always replace "$" in names with "."
     * <p>
     * This method takes all of these considerations into account and returns a fully qualified name which replaces
     * inner-class signifying "$" with ".", while preserving
     */
    @Nullable
    private static String declarableFullyQualifiedName(ClassInfo classInfo) {
        String name;
        if (classInfo.getName().startsWith("java.") && !classInfo.isPublic()) {
            // Because we put java-supplied types into another package, we cannot access package-private types
            return null;
        }
        if (classInfo.isInnerClass()) {
            StringBuilder sb = new StringBuilder();
            ClassInfoList outerClasses = classInfo.getOuterClasses();
            // Classgraph orders this collection innermost -> outermost, but type names are declared outermost -> innermost
            for (int i = outerClasses.size() - 1; i >= 0; i--) {
                ClassInfo outerClass = outerClasses.get(i);
                if (outerClass.isPrivate() || outerClass.isAnonymousInnerClass() || outerClass.isSynthetic() || outerClass.isExternalClass()) {
                    return null;
                }
                if (i == outerClasses.size() - 1) {
                    sb.append(outerClass.getName()).append(".");
                } else if (!outerClass.getName().startsWith(sb.toString())) {
                    // Code obfuscators can generate inner classes which don't share a common package prefix with their outer class
                    return classInfo.getName();
                } else {
                    sb.append(outerClass.getName().substring(sb.length())).append(".");
                }
            }
            if (!classInfo.getName().startsWith(sb.toString())) {
                // Code obfuscators can generate inner classes which don't share a common package prefix with their outer class
                return classInfo.getName();
            }
            String nameFragment = classInfo.getName().substring(sb.length());
            if (isUndeclarable(nameFragment)) {
                return null;
            }
            sb.append(nameFragment);
            name = sb.toString();
        } else {
            name = classInfo.getName();
        }
        if (isUndeclarable(name)) {
            return null;
        }
        return name;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static boolean isUndeclarable(String className) {
        char firstChar = className.charAt(0);
        return !Character.isJavaIdentifierPart(firstChar) || Character.isDigit(firstChar);
    }

    public JavaSourceSet(final UUID id, final String name, final List<JavaType.FullyQualified> classpath) {
        this.id = id;
        this.name = name;
        this.classpath = classpath;
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public List<JavaType.FullyQualified> getClasspath() {
        return this.classpath;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "JavaSourceSet(id=" + this.getId() + ", name=" + this.getName() + ", classpath=" + this.getClasspath() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof JavaSourceSet)) return false;
        final JavaSourceSet other = (JavaSourceSet) o;
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
    public JavaSourceSet withId(final UUID id) {
        return this.id == id ? this : new JavaSourceSet(id, this.name, this.classpath);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public JavaSourceSet withName(final String name) {
        return this.name == name ? this : new JavaSourceSet(this.id, name, this.classpath);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public JavaSourceSet withClasspath(final List<JavaType.FullyQualified> classpath) {
        return this.classpath == classpath ? this : new JavaSourceSet(this.id, this.name, classpath);
    }
}
