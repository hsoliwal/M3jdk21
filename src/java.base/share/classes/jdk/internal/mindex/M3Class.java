// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Objects;
import java.util.Optional;

/** Two-field immutable class view over one frozen M3CI row. */
public final class M3Class {
    private final M3CI owner;
    private final int row;

    M3Class(M3CI owner, int row) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.row = Objects.checkIndex(row, owner.size());
    }

    public M3CI owner() {
        return owner;
    }

    public int row() {
        return row;
    }

    public int loaderSpaceId() {
        return owner.loaderSpaceId(row);
    }

    public String name() {
        return owner.binaryName(row);
    }

    public String getName() {
        return name().toString();
    }

    public String simpleName() {
        return owner.simpleName(row);
    }

    public String getSimpleName() {
        return simpleName().toString();
    }

    public Optional<String> canonicalName() {
        return owner.canonicalName(row);
    }

    public String getCanonicalName() {
        return canonicalName().map(Object::toString).orElse(null);
    }

    public String packageName() {
        return owner.packageName(row);
    }

    public String getPackageName() {
        return packageName().toString();
    }

    public Optional<String> moduleName() {
        return owner.moduleName(row);
    }

    public String typeName() {
        return owner.typeName(row);
    }

    public String getTypeName() {
        return typeName().toString();
    }

    public int modifiers() {
        return owner.modifiers(row);
    }

    public int getModifiers() {
        return modifiers();
    }

    public int flags() {
        return owner.flags(row);
    }

    public boolean hasFlag(int flag) {
        return (flags() & flag) != 0;
    }

    public boolean isInterface() { return hasFlag(M3ClassFlags.INTERFACE); }
    public boolean isEnum() { return hasFlag(M3ClassFlags.ENUM); }
    public boolean isAnnotation() { return hasFlag(M3ClassFlags.ANNOTATION); }
    public boolean isRecord() { return hasFlag(M3ClassFlags.RECORD); }
    public boolean isArray() { return hasFlag(M3ClassFlags.ARRAY); }
    public boolean isPrimitive() { return hasFlag(M3ClassFlags.PRIMITIVE); }
    public boolean isSealed() { return hasFlag(M3ClassFlags.SEALED); }
    public boolean isHidden() { return hasFlag(M3ClassFlags.HIDDEN); }
    public boolean isSynthetic() { return hasFlag(M3ClassFlags.SYNTHETIC); }
    public boolean isAnonymousClass() { return hasFlag(M3ClassFlags.ANONYMOUS); }
    public boolean isLocalClass() { return hasFlag(M3ClassFlags.LOCAL); }
    public boolean isMemberClass() { return hasFlag(M3ClassFlags.MEMBER); }

    public Optional<M3Class> superclass() {
        int superRow = owner.superclassRow(row);
        return superRow < 0 ? Optional.empty() : Optional.of(owner.classAt(superRow));
    }

    public M3Class getSuperclass() {
        return superclass().orElse(null);
    }

    public Optional<M3Class> componentType() {
        int componentRow = owner.componentTypeRow(row);
        return componentRow < 0 ? Optional.empty() : Optional.of(owner.classAt(componentRow));
    }

    public M3Class getComponentType() {
        return componentType().orElse(null);
    }

    public Optional<M3Class> nestHost() {
        int hostRow = owner.nestHostRow(row);
        return hostRow < 0 ? Optional.empty() : Optional.of(owner.classAt(hostRow));
    }

    public M3Class getNestHost() {
        return nestHost().orElse(this);
    }

    public Optional<M3Class> declaringClass() {
        int declaringRow = owner.declaringClassRow(row);
        return declaringRow < 0 ? Optional.empty() : Optional.of(owner.classAt(declaringRow));
    }

    public M3Class getDeclaringClass() {
        return declaringClass().orElse(null);
    }

    public Optional<M3Class> enclosingClass() {
        int enclosingRow = owner.enclosingClassRow(row);
        return enclosingRow < 0 ? Optional.empty() : Optional.of(owner.classAt(enclosingRow));
    }

    public M3Class getEnclosingClass() {
        return enclosingClass().orElse(null);
    }

    public int interfaceCount() {
        return owner.interfaceCount(row);
    }

    public M3Class interfaceAt(int ordinal) {
        return owner.classAt(owner.interfaceRow(row, ordinal));
    }

    public M3Class[] getInterfaces() {
        M3Class[] result = new M3Class[interfaceCount()];
        for (int i = 0; i < result.length; i++) result[i] = interfaceAt(i);
        return result;
    }

    public int directSubtypeCount() {
        return owner.directSubtypeCount(row);
    }

    public M3Class directSubtypeAt(int ordinal) {
        return owner.classAt(owner.directSubtypeRow(row, ordinal));
    }

    public int supertypeCount() {
        return owner.supertypeCount(row);
    }

    public M3Class supertypeAt(int ordinal) {
        return owner.classAt(owner.supertypeRow(row, ordinal));
    }

    public boolean isAssignableFrom(M3Class candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (candidate.owner != owner) {
            throw new IllegalArgumentException("classes belong to different M3CI snapshots");
        }
        return owner.isAssignableFrom(row, candidate.row);
    }

    public int annotationCount() {
        return owner.classAnnotationCount(row);
    }

    public String annotationTypeNameAt(int ordinal) {
        return owner.classAnnotationTypeName(row, ordinal);
    }

    public int permittedSubclassCount() {
        return owner.permittedSubclassCount(row);
    }

    public String permittedSubclassNameAt(int ordinal) {
        return owner.permittedSubclassName(row, ordinal);
    }

    public int memberCount() {
        return owner.memberCount(row);
    }

    public M3Member memberAt(int ordinal) {
        return owner.memberAt(row, ordinal);
    }

    public M3Field[] getDeclaredFields() {
        int[] rows = owner.memberRows(row, M3MemberKind.FIELD);
        M3Field[] result = new M3Field[rows.length];
        for (int i = 0; i < rows.length; i++) result[i] = new M3Field(owner, rows[i]);
        return result;
    }

    public M3Method[] getDeclaredMethods() {
        int[] rows = owner.memberRows(row, M3MemberKind.METHOD);
        M3Method[] result = new M3Method[rows.length];
        for (int i = 0; i < rows.length; i++) result[i] = new M3Method(owner, rows[i]);
        return result;
    }

    public M3Constructor[] getDeclaredConstructors() {
        int[] rows = owner.memberRows(row, M3MemberKind.CONSTRUCTOR);
        M3Constructor[] result = new M3Constructor[rows.length];
        for (int i = 0; i < rows.length; i++) result[i] = new M3Constructor(owner, rows[i]);
        return result;
    }

    public M3RecordComponent[] getRecordComponents() {
        if (!isRecord()) return null;
        int[] rows = owner.memberRows(row, M3MemberKind.RECORD_COMPONENT);
        M3RecordComponent[] result = new M3RecordComponent[rows.length];
        for (int i = 0; i < rows.length; i++) result[i] = new M3RecordComponent(owner, rows[i]);
        return result;
    }

    public M3Class[] getDeclaredClasses() {
        int[] rows = owner.declaredClassRows(row);
        M3Class[] result = new M3Class[rows.length];
        for (int i = 0; i < rows.length; i++) result[i] = owner.classAt(rows[i]);
        return result;
    }

    public M3Class[] getNestMembers() {
        int[] rows = owner.nestMemberRows(row);
        M3Class[] result = new M3Class[rows.length];
        for (int i = 0; i < rows.length; i++) result[i] = owner.classAt(rows[i]);
        return result;
    }

    public M3Class[] getPermittedSubclasses() {
        if (!isSealed()) return null;
        int[] rows = owner.permittedSubclassRows(row);
        M3Class[] result = new M3Class[rows.length];
        for (int i = 0; i < rows.length; i++) result[i] = owner.classAt(rows[i]);
        return result;
    }

    public int[] memberRows(CharSequence name) {
        return owner.memberRows(row, name);
    }

    public long structuralHash64() {
        return owner.structuralHash64(row);
    }

    public Optional<Class<?>> tryClass() {
        return owner.boundary().classAt(row);
    }

    public Class<?> toClass() {
        return tryClass().orElseThrow(
                () -> new IllegalStateException("JVM class boundary is no longer live for row " + row));
    }

    /** JVM boundary operation; compact/detached snapshots deliberately cannot satisfy it. */
    public boolean isInstance(Object value) {
        return toClass().isInstance(value);
    }

    /** JVM boundary operation matching Class.cast semantics. */
    public Object cast(Object value) {
        return toClass().cast(value);
    }

    public ClassLoader getClassLoader() { return toClass().getClassLoader(); }
    public Module getModule() { return toClass().getModule(); }
    public Package getPackage() { return toClass().getPackage(); }
    public ProtectionDomain getProtectionDomain() { return toClass().getProtectionDomain(); }
    public boolean desiredAssertionStatus() { return toClass().desiredAssertionStatus(); }
    public URL getResource(String name) { return toClass().getResource(name); }
    public InputStream getResourceAsStream(String name) { return toClass().getResourceAsStream(name); }
    public Object[] getSigners() { return toClass().getSigners(); }
    public Type getGenericSuperclass() { return toClass().getGenericSuperclass(); }
    public Type[] getGenericInterfaces() { return toClass().getGenericInterfaces(); }
    public TypeVariable<?>[] getTypeParameters() { return toClass().getTypeParameters(); }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return toClass().isAnnotationPresent(annotationClass);
    }

    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return toClass().getAnnotation(annotationClass);
    }

    public Annotation[] getAnnotations() { return toClass().getAnnotations(); }
    public Annotation[] getDeclaredAnnotations() { return toClass().getDeclaredAnnotations(); }

    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass) {
        return toClass().getAnnotationsByType(annotationClass);
    }

    public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass) {
        return toClass().getDeclaredAnnotation(annotationClass);
    }

    public <A extends Annotation> A[] getDeclaredAnnotationsByType(Class<A> annotationClass) {
        return toClass().getDeclaredAnnotationsByType(annotationClass);
    }

    public M3Field getDeclaredField(String name) throws NoSuchFieldException {
        Objects.requireNonNull(name, "name");
        for (int memberRow : owner.memberRows(row, name)) {
            if (owner.memberKind(memberRow) == M3MemberKind.FIELD) {
                return new M3Field(owner, memberRow);
            }
        }
        throw new NoSuchFieldException(getName() + "." + name);
    }

    public M3Method getDeclaredMethod(String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method =
                toClass().getDeclaredMethod(
                        Objects.requireNonNull(name, "name"),
                        Objects.requireNonNull(parameterTypes, "parameterTypes"));
        return methodView(method);
    }

    public M3Constructor getDeclaredConstructor(Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Constructor<?> constructor =
                toClass().getDeclaredConstructor(
                        Objects.requireNonNull(parameterTypes, "parameterTypes"));
        return constructorView(constructor);
    }

    public M3Field getField(String name) throws NoSuchFieldException {
        return fieldView(toClass().getField(Objects.requireNonNull(name, "name")));
    }

    public M3Method getMethod(String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        return methodView(
                toClass().getMethod(
                        Objects.requireNonNull(name, "name"),
                        Objects.requireNonNull(parameterTypes, "parameterTypes")));
    }

    public M3Constructor getConstructor(Class<?>... parameterTypes)
            throws NoSuchMethodException {
        return constructorView(
                toClass().getConstructor(
                        Objects.requireNonNull(parameterTypes, "parameterTypes")));
    }

    public M3Field[] getFields() {
        Field[] fields = toClass().getFields();
        M3Field[] result = new M3Field[fields.length];
        for (int i = 0; i < fields.length; i++) result[i] = fieldView(fields[i]);
        return result;
    }

    public M3Method[] getMethods() {
        Method[] methods = toClass().getMethods();
        M3Method[] result = new M3Method[methods.length];
        for (int i = 0; i < methods.length; i++) result[i] = methodView(methods[i]);
        return result;
    }

    public M3Constructor[] getConstructors() {
        Constructor<?>[] constructors = toClass().getConstructors();
        M3Constructor[] result = new M3Constructor[constructors.length];
        for (int i = 0; i < constructors.length; i++) {
            result[i] = constructorView(constructors[i]);
        }
        return result;
    }

    private M3Field fieldView(Field field) {
        M3Class declaring =
                owner.fromClass(field.getDeclaringClass())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "declaring class is outside M3Class snapshot: "
                                                        + field.getDeclaringClass().getName()));
        int memberRow =
                owner.memberRow(
                        declaring.row(),
                        M3MemberKind.FIELD,
                        field.getName(),
                        M3Descriptor.field(field));
        if (memberRow < 0) {
            throw new IllegalStateException("indexed field metadata is missing: " + field);
        }
        return new M3Field(owner, memberRow);
    }

    private M3Method methodView(Method method) {
        M3Class declaring =
                owner.fromClass(method.getDeclaringClass())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "declaring class is outside M3Class snapshot: "
                                                        + method.getDeclaringClass().getName()));
        int memberRow =
                owner.memberRow(
                        declaring.row(),
                        M3MemberKind.METHOD,
                        method.getName(),
                        M3Descriptor.method(method));
        if (memberRow < 0) {
            throw new IllegalStateException("indexed method metadata is missing: " + method);
        }
        return new M3Method(owner, memberRow);
    }

    private M3Constructor constructorView(Constructor<?> constructor) {
        M3Class declaring =
                owner.fromClass(constructor.getDeclaringClass())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "declaring class is outside M3Class snapshot: "
                                                        + constructor.getDeclaringClass().getName()));
        int memberRow =
                owner.memberRow(
                        declaring.row(),
                        M3MemberKind.CONSTRUCTOR,
                        constructor.getName(),
                        M3Descriptor.constructor(constructor));
        if (memberRow < 0) {
            throw new IllegalStateException(
                    "indexed constructor metadata is missing: " + constructor);
        }
        return new M3Constructor(owner, memberRow);
    }

    @Override
    public String toString() {
        return name().toString();
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(owner) + row;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof M3Class that && owner == that.owner && row == that.row;
    }
}
