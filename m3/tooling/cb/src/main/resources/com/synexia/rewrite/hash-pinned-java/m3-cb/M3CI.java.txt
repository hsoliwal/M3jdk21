// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Immutable primitive projection of Java class and declared-member metadata.
 *
 * <p>Build-time reflection objects are discarded after freeze. The retained authority is one set of
 * primitive lanes plus snapshot-local references to existing JDK String owners. A separate weak boundary permits
 * round-tripping to JVM Class objects without pinning class loaders.</p>
 */
public final class M3CI {
    public record Options(int maxClasses, boolean retainWeakJvmBoundary) {
        public Options {
            if (maxClasses < 1) throw new IllegalArgumentException("maxClasses");
        }

        public static Options standard() {
            return new Options(16_384, true);
        }

        /** Maximum footprint mode: primitive snapshot only, no WeakReference boundary objects. */
        public static Options compact() {
            return new Options(16_384, false);
        }
    }

    private static final int NONE = -1;
    private static final long HASH_SEED = 0xcbf29ce484222325L;
    private static final long HASH_PRIME = 0x100000001b3L;

    private final M3Boundary boundary;
    private final String[] texts;
    private final int[] textOrder;

    private final int[] loaderSpaces;
    private final int[] binaryNameIds;
    private final int[] simpleNameIds;
    private final int[] canonicalNameIds;
    private final int[] packageNameIds;
    private final int[] moduleNameIds;
    private final int[] typeNameIds;
    private final int[] modifiers;
    private final int[] flags;
    private final int[] superclassRows;
    private final int[] componentTypeRows;
    private final int[] nestHostRows;
    private final int[] declaringClassRows;
    private final int[] enclosingClassRows;
    private final long[] structuralHashes;

    private final int[] interfaceOffsets;
    private final int[] interfaceRows;
    private final int[] directSubtypeOffsets;
    private final int[] directSubtypeRows;
    private final int[] supertypeOffsets;
    private final int[] supertypeRows;
    private final int[] classAnnotationOffsets;
    private final int[] classAnnotationNameIds;
    private final int[] permittedOffsets;
    private final int[] permittedNameIds;

    private final int[] nameKeys;
    private final int[] nameOffsets;
    private final int[] nameRows;

    private final int[] memberOffsets;
    private final int[] memberDeclaringRows;
    private final byte[] memberKinds;
    private final int[] memberNameIds;
    private final int[] memberDescriptorIds;
    private final int[] memberModifiers;
    private final int[] memberFlags;
    private final long[] memberStructuralHashes;
    private final int[] memberAnnotationOffsets;
    private final int[] memberAnnotationNameIds;
    private final int[] memberExceptionOffsets;
    private final int[] memberExceptionDescriptorIds;

    private final byte[] fingerprintSha256;

    private M3CI(Frozen frozen) {
        boundary = frozen.boundary;
        texts = frozen.texts;
        textOrder = frozen.textOrder;
        loaderSpaces = frozen.loaderSpaces;
        binaryNameIds = frozen.binaryNameIds;
        simpleNameIds = frozen.simpleNameIds;
        canonicalNameIds = frozen.canonicalNameIds;
        packageNameIds = frozen.packageNameIds;
        moduleNameIds = frozen.moduleNameIds;
        typeNameIds = frozen.typeNameIds;
        modifiers = frozen.modifiers;
        flags = frozen.flags;
        superclassRows = frozen.superclassRows;
        componentTypeRows = frozen.componentTypeRows;
        nestHostRows = frozen.nestHostRows;
        declaringClassRows = frozen.declaringClassRows;
        enclosingClassRows = frozen.enclosingClassRows;
        structuralHashes = frozen.structuralHashes;
        interfaceOffsets = frozen.interfaceOffsets;
        interfaceRows = frozen.interfaceRows;
        directSubtypeOffsets = frozen.directSubtypeOffsets;
        directSubtypeRows = frozen.directSubtypeRows;
        supertypeOffsets = frozen.supertypeOffsets;
        supertypeRows = frozen.supertypeRows;
        classAnnotationOffsets = frozen.classAnnotationOffsets;
        classAnnotationNameIds = frozen.classAnnotationNameIds;
        permittedOffsets = frozen.permittedOffsets;
        permittedNameIds = frozen.permittedNameIds;
        nameKeys = frozen.nameKeys;
        nameOffsets = frozen.nameOffsets;
        nameRows = frozen.nameRows;
        memberOffsets = frozen.memberOffsets;
        memberDeclaringRows = frozen.memberDeclaringRows;
        memberKinds = frozen.memberKinds;
        memberNameIds = frozen.memberNameIds;
        memberDescriptorIds = frozen.memberDescriptorIds;
        memberModifiers = frozen.memberModifiers;
        memberFlags = frozen.memberFlags;
        memberStructuralHashes = frozen.memberStructuralHashes;
        memberAnnotationOffsets = frozen.memberAnnotationOffsets;
        memberAnnotationNameIds = frozen.memberAnnotationNameIds;
        memberExceptionOffsets = frozen.memberExceptionOffsets;
        memberExceptionDescriptorIds = frozen.memberExceptionDescriptorIds;
        fingerprintSha256 = frozen.fingerprintSha256;
    }

    public static M3CI compile(Class<?>... roots) {
        Objects.requireNonNull(roots, "roots");
        return compile(List.of(roots), Options.standard(), M3VI.Progress.none());
    }

    public static M3CI compile(
            Collection<Class<?>> roots,
            Options options,
            M3VI.Progress progress) {
        Objects.requireNonNull(roots, "roots");
        Options checkedOptions = Objects.requireNonNull(options, "options");
        if (roots.isEmpty()) throw new IllegalArgumentException("at least one root class is required");

        M3VI.Progress monitor = progress == null ? M3VI.Progress.none() : progress;
        monitor.begin("M3CI.compile", -1L);
        try {
            Discovery discovery = discover(roots, checkedOptions.maxClasses(), monitor);
            Frozen frozen =
                    freeze(discovery, checkedOptions.retainWeakJvmBoundary(), monitor);
            monitor.checkCancelled();
            return new M3CI(frozen);
        } finally {
            monitor.done();
        }
    }

    public int size() {
        return binaryNameIds.length;
    }

    public int memberSize() {
        return memberNameIds.length;
    }

    public M3Boundary boundary() {
        return boundary;
    }

    public M3Class classAt(int row) {
        return new M3Class(this, Objects.checkIndex(row, size()));
    }

    public M3Member memberAt(int memberRow) {
        return new M3Member(this, Objects.checkIndex(memberRow, memberSize()));
    }

    public int loaderSpaceId(int row) {
        return loaderSpaces[checkClassRow(row)];
    }

    public String binaryName(int row) {
        return text(binaryNameIds[checkClassRow(row)]);
    }

    public String simpleName(int row) {
        return text(simpleNameIds[checkClassRow(row)]);
    }

    public Optional<String> canonicalName(int row) {
        return optionalText(canonicalNameIds[checkClassRow(row)]);
    }

    public String packageName(int row) {
        return text(packageNameIds[checkClassRow(row)]);
    }

    public Optional<String> moduleName(int row) {
        return optionalText(moduleNameIds[checkClassRow(row)]);
    }

    public String typeName(int row) {
        return text(typeNameIds[checkClassRow(row)]);
    }

    public int modifiers(int row) {
        return modifiers[checkClassRow(row)];
    }

    public int flags(int row) {
        return flags[checkClassRow(row)];
    }

    public int superclassRow(int row) {
        return superclassRows[checkClassRow(row)];
    }

    public int componentTypeRow(int row) {
        return componentTypeRows[checkClassRow(row)];
    }

    public int nestHostRow(int row) {
        return nestHostRows[checkClassRow(row)];
    }

    public int declaringClassRow(int row) {
        return declaringClassRows[checkClassRow(row)];
    }

    public int enclosingClassRow(int row) {
        return enclosingClassRows[checkClassRow(row)];
    }

    public long structuralHash64(int row) {
        return structuralHashes[checkClassRow(row)];
    }

    public int interfaceCount(int row) {
        int checked = checkClassRow(row);
        return interfaceOffsets[checked + 1] - interfaceOffsets[checked];
    }

    public int interfaceRow(int row, int ordinal) {
        int checked = checkClassRow(row);
        int start = interfaceOffsets[checked];
        int count = interfaceOffsets[checked + 1] - start;
        return interfaceRows[start + Objects.checkIndex(ordinal, count)];
    }

    public int directSubtypeCount(int row) {
        int checked = checkClassRow(row);
        return directSubtypeOffsets[checked + 1] - directSubtypeOffsets[checked];
    }

    public int directSubtypeRow(int row, int ordinal) {
        int checked = checkClassRow(row);
        int start = directSubtypeOffsets[checked];
        int count = directSubtypeOffsets[checked + 1] - start;
        return directSubtypeRows[start + Objects.checkIndex(ordinal, count)];
    }

    public int supertypeCount(int row) {
        int checked = checkClassRow(row);
        return supertypeOffsets[checked + 1] - supertypeOffsets[checked];
    }

    public int supertypeRow(int row, int ordinal) {
        int checked = checkClassRow(row);
        int start = supertypeOffsets[checked];
        int count = supertypeOffsets[checked + 1] - start;
        return supertypeRows[start + Objects.checkIndex(ordinal, count)];
    }

    /**
     * String-compatible type assignability inside this snapshot.
     *
     * <p>Ordinary classes/interfaces use the precomputed transitive-supertype CSR. Array covariance
     * is recursive over the indexed component rows because the JVM does not expose Object[] as the
     * direct superclass of String[].</p>
     */
    public boolean isAssignableFrom(int supertypeRow, int candidateRow) {
        int wanted = checkClassRow(supertypeRow);
        int candidate = checkClassRow(candidateRow);
        if (wanted == candidate) return true;

        boolean wantedArray = (flags[wanted] & M3ClassFlags.ARRAY) != 0;
        boolean candidateArray = (flags[candidate] & M3ClassFlags.ARRAY) != 0;
        if (wantedArray && candidateArray) {
            int wantedComponent = componentTypeRows[wanted];
            int candidateComponent = componentTypeRows[candidate];
            if (wantedComponent < 0 || candidateComponent < 0) return false;
            boolean wantedPrimitive =
                    (flags[wantedComponent] & M3ClassFlags.PRIMITIVE) != 0;
            boolean candidatePrimitive =
                    (flags[candidateComponent] & M3ClassFlags.PRIMITIVE) != 0;
            if (wantedPrimitive || candidatePrimitive) {
                return wantedComponent == candidateComponent;
            }
            return isAssignableFrom(wantedComponent, candidateComponent);
        }

        int start = supertypeOffsets[candidate];
        int end = supertypeOffsets[candidate + 1];
        int position = lowerBound(supertypeRows, start, end, wanted);
        return position < end && supertypeRows[position] == wanted;
    }

    public int classAnnotationCount(int row) {
        int checked = checkClassRow(row);
        return classAnnotationOffsets[checked + 1] - classAnnotationOffsets[checked];
    }

    public String classAnnotationTypeName(int row, int ordinal) {
        int checked = checkClassRow(row);
        int start = classAnnotationOffsets[checked];
        int count = classAnnotationOffsets[checked + 1] - start;
        return text(classAnnotationNameIds[start + Objects.checkIndex(ordinal, count)]);
    }

    public int permittedSubclassCount(int row) {
        int checked = checkClassRow(row);
        return permittedOffsets[checked + 1] - permittedOffsets[checked];
    }

    public String permittedSubclassName(int row, int ordinal) {
        int checked = checkClassRow(row);
        int start = permittedOffsets[checked];
        int count = permittedOffsets[checked + 1] - start;
        return text(permittedNameIds[start + Objects.checkIndex(ordinal, count)]);
    }

    /** All rows with this binary name, potentially from different class-loader spaces. */
    public int[] classRows(CharSequence binaryName) {
        int contentId = findText(
                Objects.requireNonNull(binaryName, "binaryName"));
        if (contentId < 0) return new int[0];
        int key = Arrays.binarySearch(nameKeys, contentId);
        if (key < 0) return new int[0];
        return Arrays.copyOfRange(nameRows, nameOffsets[key], nameOffsets[key + 1]);
    }

    /** Exact snapshot row for loader-space + binary-name, or -1. */
    public int classRow(int loaderSpace, CharSequence binaryName) {
        if (loaderSpace < 0 || loaderSpace >= boundary.loaderSpaceCount()) return NONE;
        int[] rows = classRows(binaryName);
        for (int row : rows) {
            if (loaderSpaces[row] == loaderSpace) return row;
        }
        return NONE;
    }

    public Optional<M3Class> fromClass(Class<?> type) {
        Class<?> checked = Objects.requireNonNull(type, "type");
        int loaderSpace = boundary.loaderSpaceOf(checked.getClassLoader());
        if (loaderSpace < 0) return Optional.empty();
        int row = classRow(loaderSpace, checked.getName());
        if (row < 0) return Optional.empty();
        Optional<Class<?>> boundaryClass = boundary.classAt(row);
        return boundaryClass.filter(candidate -> candidate == checked).map(ignored -> classAt(row));
    }

    public int memberCount(int classRow) {
        int checked = checkClassRow(classRow);
        return memberOffsets[checked + 1] - memberOffsets[checked];
    }

    public int[] declaredClassRows(int classRow) {
        int checked = checkClassRow(classRow);
        int count = 0;
        for (int row = 0; row < size(); row++) {
            if (declaringClassRows[row] == checked) count++;
        }
        int[] result = new int[count];
        int output = 0;
        for (int row = 0; row < size(); row++) {
            if (declaringClassRows[row] == checked) result[output++] = row;
        }
        return result;
    }

    public int[] nestMemberRows(int classRow) {
        int checked = checkClassRow(classRow);
        int host = nestHostRows[checked];
        if (host < 0) return new int[0];
        int count = 0;
        for (int row = 0; row < size(); row++) {
            if (nestHostRows[row] == host) count++;
        }
        int[] result = new int[count];
        int output = 0;
        for (int row = 0; row < size(); row++) {
            if (nestHostRows[row] == host) result[output++] = row;
        }
        return result;
    }

    public int[] permittedSubclassRows(int classRow) {
        int checked = checkClassRow(classRow);
        int start = permittedOffsets[checked];
        int end = permittedOffsets[checked + 1];
        int[] result = new int[end - start];
        int output = 0;
        for (int at = start; at < end; at++) {
            String binaryName = text(permittedNameIds[at]).toString();
            int row = classRow(loaderSpaces[checked], binaryName);
            if (row >= 0) result[output++] = row;
        }
        return output == result.length ? result : Arrays.copyOf(result, output);
    }

    public M3Member memberAt(int classRow, int ordinal) {
        int checked = checkClassRow(classRow);
        int start = memberOffsets[checked];
        int count = memberOffsets[checked + 1] - start;
        return memberAt(start + Objects.checkIndex(ordinal, count));
    }

    public int[] memberRows(int classRow, CharSequence name) {
        int checked = checkClassRow(classRow);
        int nameId = findText(Objects.requireNonNull(name, "name"));
        if (nameId < 0) return new int[0];
        int start = memberOffsets[checked];
        int end = memberOffsets[checked + 1];
        int from = lowerBound(memberNameIds, start, end, nameId);
        if (from == end || memberNameIds[from] != nameId) return new int[0];
        int to = upperBound(memberNameIds, from, end, nameId);
        int[] result = new int[to - from];
        for (int i = 0; i < result.length; i++) result[i] = from + i;
        return result;
    }

    public int[] memberRows(int classRow, M3MemberKind kind) {
        int checked = checkClassRow(classRow);
        Objects.requireNonNull(kind, "kind");
        int start = memberOffsets[checked];
        int end = memberOffsets[checked + 1];
        int count = 0;
        for (int row = start; row < end; row++) {
            if (Byte.toUnsignedInt(memberKinds[row]) == kind.code()) count++;
        }
        int[] result = new int[count];
        int output = 0;
        for (int row = start; row < end; row++) {
            if (Byte.toUnsignedInt(memberKinds[row]) == kind.code()) {
                result[output++] = row;
            }
        }
        return result;
    }

    public int memberRow(
            int classRow,
            M3MemberKind kind,
            CharSequence name,
            CharSequence descriptor) {
        Objects.requireNonNull(kind, "kind");
        int descriptorId = findText(
                Objects.requireNonNull(descriptor, "descriptor"));
        if (descriptorId < 0) return NONE;
        for (int row : memberRows(classRow, name)) {
            if (Byte.toUnsignedInt(memberKinds[row]) == kind.code()
                    && memberDescriptorIds[row] == descriptorId) {
                return row;
            }
        }
        return NONE;
    }

    public int memberDeclaringClassRow(int row) {
        return memberDeclaringRows[checkMemberRow(row)];
    }

    public M3MemberKind memberKind(int row) {
        return M3MemberKind.fromCode(Byte.toUnsignedInt(memberKinds[checkMemberRow(row)]));
    }

    public String memberName(int row) {
        return text(memberNameIds[checkMemberRow(row)]);
    }

    public String memberDescriptor(int row) {
        return text(memberDescriptorIds[checkMemberRow(row)]);
    }

    public int memberModifiers(int row) {
        return memberModifiers[checkMemberRow(row)];
    }

    public int memberFlags(int row) {
        return memberFlags[checkMemberRow(row)];
    }

    public long memberStructuralHash64(int row) {
        return memberStructuralHashes[checkMemberRow(row)];
    }

    public int memberAnnotationCount(int row) {
        int checked = checkMemberRow(row);
        return memberAnnotationOffsets[checked + 1] - memberAnnotationOffsets[checked];
    }

    public String memberAnnotationTypeName(int row, int ordinal) {
        int checked = checkMemberRow(row);
        int start = memberAnnotationOffsets[checked];
        int count = memberAnnotationOffsets[checked + 1] - start;
        return text(memberAnnotationNameIds[start + Objects.checkIndex(ordinal, count)]);
    }

    public int memberExceptionCount(int row) {
        int checked = checkMemberRow(row);
        return memberExceptionOffsets[checked + 1] - memberExceptionOffsets[checked];
    }

    public String memberExceptionTypeDescriptor(int row, int ordinal) {
        int checked = checkMemberRow(row);
        int start = memberExceptionOffsets[checked];
        int count = memberExceptionOffsets[checked + 1] - start;
        return text(memberExceptionDescriptorIds[start + Objects.checkIndex(ordinal, count)]);
    }

    public Optional<AnnotatedElement> resolveReflectiveElement(int memberRow) {
        int checked = checkMemberRow(memberRow);
        Optional<Class<?>> ownerClass = boundary.classAt(memberDeclaringRows[checked]);
        if (ownerClass.isEmpty()) return Optional.empty();

        Class<?> type = ownerClass.get();
        String name = memberName(checked).toString();
        String descriptor = memberDescriptor(checked).toString();
        try {
            return switch (memberKind(checked)) {
                case FIELD -> findField(type, name, descriptor);
                case METHOD -> findMethod(type, name, descriptor);
                case CONSTRUCTOR -> findConstructor(type, name, descriptor);
                case RECORD_COMPONENT -> findRecordComponent(type, name, descriptor);
            };
        } catch (LinkageError | SecurityException unavailable) {
            return Optional.empty();
        }
    }

    public byte[] fingerprintSha256() {
        return fingerprintSha256.clone();
    }

    public String fingerprintSha256Hex() {
        return HexFormat.of().formatHex(fingerprintSha256);
    }

    /** Primitive payload only; excludes object/array headers and the optional weak JVM boundary. */
    public long primitivePayloadBytes() {
        long ints =
                totalLength(
                        loaderSpaces, binaryNameIds, simpleNameIds, canonicalNameIds,
                        packageNameIds, moduleNameIds, typeNameIds, modifiers, flags,
                        superclassRows, componentTypeRows, nestHostRows, declaringClassRows,
                        enclosingClassRows, interfaceOffsets, interfaceRows,
                        directSubtypeOffsets, directSubtypeRows, supertypeOffsets, supertypeRows,
                        classAnnotationOffsets, classAnnotationNameIds, permittedOffsets,
                        permittedNameIds, nameKeys, nameOffsets, nameRows, memberOffsets,
                        memberDeclaringRows, memberNameIds, memberDescriptorIds, memberModifiers,
                        memberFlags, memberAnnotationOffsets, memberAnnotationNameIds,
                        memberExceptionOffsets, memberExceptionDescriptorIds);
        long longs = (long) structuralHashes.length + memberStructuralHashes.length;
        return Math.addExact(
                Math.addExact(ints * Integer.BYTES, longs * Long.BYTES),
                memberKinds.length);
    }

    int binaryNameKeyCount() {
        return nameKeys.length;
    }

    String binaryNameKey(int keyIndex) {
        return text(nameKeys[Objects.checkIndex(keyIndex, nameKeys.length)]);
    }

    int[] rowsForBinaryNameKey(int keyIndex) {
        int checked = Objects.checkIndex(keyIndex, nameKeys.length);
        return Arrays.copyOfRange(nameRows, nameOffsets[checked], nameOffsets[checked + 1]);
    }

    int[] uniqueMemberNameIds() {
        if (memberNameIds.length == 0) return new int[0];
        int[] sorted = memberNameIds.clone();
        Arrays.sort(sorted);
        int unique = 1;
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i] != sorted[unique - 1]) sorted[unique++] = sorted[i];
        }
        return Arrays.copyOf(sorted, unique);
    }

    private int checkClassRow(int row) {
        return Objects.checkIndex(row, size());
    }

    private int checkMemberRow(int row) {
        return Objects.checkIndex(row, memberSize());
    }

    private String text(int contentId) {
        return texts[Objects.checkIndex(contentId, texts.length)];
    }

    private Optional<String> optionalText(int contentId) {
        return contentId < 0 ? Optional.empty() : Optional.of(text(contentId));
    }

    private static Optional<AnnotatedElement> findField(
            Class<?> type, String name, String descriptor) {
        for (Field field : type.getDeclaredFields()) {
            if (field.getName().equals(name)
                    && M3Descriptor.field(field).equals(descriptor)) {
                return Optional.of(field);
            }
        }
        return Optional.empty();
    }

    private static Optional<AnnotatedElement> findMethod(
            Class<?> type, String name, String descriptor) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name)
                    && M3Descriptor.method(method).equals(descriptor)) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    private static Optional<AnnotatedElement> findConstructor(
            Class<?> type, String name, String descriptor) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (constructor.getName().equals(name)
                    && M3Descriptor.constructor(constructor).equals(descriptor)) {
                return Optional.of(constructor);
            }
        }
        return Optional.empty();
    }

    private static Optional<AnnotatedElement> findRecordComponent(
            Class<?> type, String name, String descriptor) {
        RecordComponent[] components = type.getRecordComponents();
        if (components == null) return Optional.empty();
        for (RecordComponent component : components) {
            if (component.getName().equals(name)
                    && M3Descriptor.recordComponent(component).equals(descriptor)) {
                return Optional.of(component);
            }
        }
        return Optional.empty();
    }

    private static Discovery discover(
            Collection<Class<?>> roots, int maxClasses, M3VI.Progress monitor) {
        IdentityHashMap<Class<?>, Boolean> seen = new IdentityHashMap<>();
        ArrayDeque<Class<?>> queue = new ArrayDeque<>();
        IdentityHashMap<ClassLoader, Integer> loaderIds = new IdentityHashMap<>();
        ArrayList<ClassLoader> loaders = new ArrayList<>();
        loaderIds.put(null, 0);
        loaders.add(null);

        for (Class<?> root : roots) {
            enqueue(Objects.requireNonNull(root, "root"), seen, queue, maxClasses);
        }

        ArrayList<Class<?>> classes = new ArrayList<>();
        while (!queue.isEmpty()) {
            monitor.checkCancelled();
            Class<?> type = queue.removeFirst();
            classes.add(type);
            ClassLoader loader = type.getClassLoader();
            if (!loaderIds.containsKey(loader)) {
                loaderIds.put(loader, loaders.size());
                loaders.add(loader);
            }

            enqueue(type.getSuperclass(), seen, queue, maxClasses);
            for (Class<?> iface : type.getInterfaces()) enqueue(iface, seen, queue, maxClasses);
            enqueue(type.getComponentType(), seen, queue, maxClasses);
            Class<?> nestHost = type.getNestHost();
            if (nestHost != type) enqueue(nestHost, seen, queue, maxClasses);
            enqueue(type.getDeclaringClass(), seen, queue, maxClasses);
            enqueue(type.getEnclosingClass(), seen, queue, maxClasses);
            for (Class<?> declared : type.getDeclaredClasses()) {
                enqueue(declared, seen, queue, maxClasses);
            }
            for (Class<?> nestMember : type.getNestMembers()) {
                enqueue(nestMember, seen, queue, maxClasses);
            }
            Class<?>[] permitted = type.getPermittedSubclasses();
            if (permitted != null) {
                for (Class<?> subtype : permitted) {
                    enqueue(subtype, seen, queue, maxClasses);
                }
            }
            if (type.isInterface()) enqueue(Object.class, seen, queue, maxClasses);
            monitor.worked(1L);
        }

        classes.sort(
                Comparator.comparingInt((Class<?> type) -> loaderIds.get(type.getClassLoader()))
                        .thenComparing(Class::getName));

        return new Discovery(
                classes.toArray(Class<?>[]::new),
                loaders.toArray(ClassLoader[]::new),
                loaderIds);
    }

    private static void enqueue(
            Class<?> type,
            IdentityHashMap<Class<?>, Boolean> seen,
            ArrayDeque<Class<?>> queue,
            int maxClasses) {
        if (type == null || seen.put(type, Boolean.TRUE) != null) return;
        if (seen.size() > maxClasses) {
            throw new IllegalArgumentException(
                    "M3Class closure exceeds maxClasses=" + maxClasses);
        }
        queue.addLast(type);
    }

    private static Frozen freeze(
            Discovery discovery,
            boolean retainWeakJvmBoundary,
            M3VI.Progress monitor) {
        Symbols symbols = new Symbols();
        Class<?>[] classes = discovery.classes;
        int classCount = classes.length;
        IdentityHashMap<Class<?>, Integer> rowByClass = new IdentityHashMap<>();
        for (int row = 0; row < classCount; row++) rowByClass.put(classes[row], row);

        int[] loaderSpaces = new int[classCount];
        int[] binaryNameIds = new int[classCount];
        int[] simpleNameIds = new int[classCount];
        int[] canonicalNameIds = filled(classCount, NONE);
        int[] packageNameIds = new int[classCount];
        int[] moduleNameIds = filled(classCount, NONE);
        int[] typeNameIds = new int[classCount];
        int[] modifiers = new int[classCount];
        int[] flags = new int[classCount];
        int[] superclassRows = filled(classCount, NONE);
        int[] componentTypeRows = filled(classCount, NONE);
        int[] nestHostRows = filled(classCount, NONE);
        int[] declaringClassRows = filled(classCount, NONE);
        int[] enclosingClassRows = filled(classCount, NONE);
        long[] structuralHashes = new long[classCount];

        int[][] interfaces = new int[classCount][];
        int[][] classAnnotations = new int[classCount][];
        int[][] permitted = new int[classCount][];
        ArrayList<MemberDraft> members = new ArrayList<>();

        for (int row = 0; row < classCount; row++) {
            monitor.checkCancelled();
            Class<?> type = classes[row];
            loaderSpaces[row] = discovery.loaderIds.get(type.getClassLoader());
            binaryNameIds[row] = symbols.add(type.getName());
            simpleNameIds[row] = symbols.add(type.getSimpleName());
            String canonical = type.getCanonicalName();
            if (canonical != null) canonicalNameIds[row] = symbols.add(canonical);
            packageNameIds[row] = symbols.add(type.getPackageName());
            String module = type.getModule().getName();
            if (module != null) moduleNameIds[row] = symbols.add(module);
            typeNameIds[row] = symbols.add(type.getTypeName());
            modifiers[row] = type.getModifiers();
            flags[row] = M3ClassFlags.of(type);
            superclassRows[row] = rowOf(rowByClass, type.getSuperclass());
            componentTypeRows[row] = rowOf(rowByClass, type.getComponentType());
            nestHostRows[row] = rowOf(rowByClass, type.getNestHost());
            declaringClassRows[row] = rowOf(rowByClass, type.getDeclaringClass());
            enclosingClassRows[row] = rowOf(rowByClass, type.getEnclosingClass());

            Class<?>[] directInterfaces = type.getInterfaces();
            int[] interfaceRow = new int[directInterfaces.length];
            for (int i = 0; i < directInterfaces.length; i++) {
                interfaceRow[i] = requiredRow(rowByClass, directInterfaces[i]);
            }
            // Class.getInterfaces() exposes declaration order; preserve it exactly.
            interfaces[row] = interfaceRow;
            classAnnotations[row] = annotationNameIds(symbols, type);
            permitted[row] = permittedNameIds(symbols, type);

            admitMembers(symbols, row, type, members);
            monitor.worked(1L);
        }

        int[] interfaceOffsets = offsets(interfaces);
        int[] interfaceRows = flatten(interfaces, interfaceOffsets[classCount]);
        int[][] directSubtypes = reverseHierarchy(classCount, superclassRows, interfaces);
        int[] directSubtypeOffsets = offsets(directSubtypes);
        int[] directSubtypeRows =
                flatten(directSubtypes, directSubtypeOffsets[classCount]);

        int objectRow = rowByClass.getOrDefault(Object.class, NONE);
        int[][] supertypes =
                transitiveSupertypes(classCount, superclassRows, interfaces, flags, objectRow);
        int[] supertypeOffsets = offsets(supertypes);
        int[] supertypeRows = flatten(supertypes, supertypeOffsets[classCount]);

        int[] classAnnotationOffsets = offsets(classAnnotations);
        int[] classAnnotationNameIds =
                flatten(classAnnotations, classAnnotationOffsets[classCount]);
        int[] permittedOffsets = offsets(permitted);
        int[] permittedNameIds = flatten(permitted, permittedOffsets[classCount]);

        members.sort(
                Comparator.comparingInt((MemberDraft draft) -> draft.owner)
                        .thenComparingInt(draft -> draft.nameId)
                        .thenComparingInt(draft -> draft.kind.code())
                        .thenComparingInt(draft -> draft.descriptorId));

        int memberCount = members.size();
        int[] memberOffsets = new int[classCount + 1];
        int[] memberDeclaringRows = new int[memberCount];
        byte[] memberKinds = new byte[memberCount];
        int[] memberNameIds = new int[memberCount];
        int[] memberDescriptorIds = new int[memberCount];
        int[] memberModifiers = new int[memberCount];
        int[] memberFlags = new int[memberCount];
        long[] memberStructuralHashes = new long[memberCount];
        int[][] memberAnnotations = new int[memberCount][];
        int[][] memberExceptions = new int[memberCount][];

        for (MemberDraft draft : members) memberOffsets[draft.owner + 1]++;
        prefixSum(memberOffsets);
        for (int row = 0; row < memberCount; row++) {
            MemberDraft draft = members.get(row);
            memberDeclaringRows[row] = draft.owner;
            memberKinds[row] = (byte) draft.kind.code();
            memberNameIds[row] = draft.nameId;
            memberDescriptorIds[row] = draft.descriptorId;
            memberModifiers[row] = draft.modifiers;
            memberFlags[row] = draft.flags;
            memberAnnotations[row] = draft.annotationNameIds;
            memberExceptions[row] = draft.exceptionDescriptorIds;
            memberStructuralHashes[row] = memberHash(draft);
        }

        int[] memberAnnotationOffsets = offsets(memberAnnotations);
        int[] memberAnnotationNameIds =
                flatten(memberAnnotations, memberAnnotationOffsets[memberCount]);
        int[] memberExceptionOffsets = offsets(memberExceptions);
        int[] memberExceptionDescriptorIds =
                flatten(memberExceptions, memberExceptionOffsets[memberCount]);

        NameIndex nameIndex = buildNameIndex(binaryNameIds);
        for (int row = 0; row < classCount; row++) {
            structuralHashes[row] =
                    classHash(
                            row,
                            loaderSpaces,
                            binaryNameIds,
                            simpleNameIds,
                            canonicalNameIds,
                            packageNameIds,
                            moduleNameIds,
                            typeNameIds,
                            modifiers,
                            flags,
                            superclassRows,
                            componentTypeRows,
                            nestHostRows,
                            declaringClassRows,
                            enclosingClassRows,
                            interfaceOffsets,
                            interfaceRows,
                            classAnnotationOffsets,
                            classAnnotationNameIds,
                            permittedOffsets,
                            permittedNameIds,
                            memberOffsets,
                            memberKinds,
                            memberNameIds,
                            memberDescriptorIds,
                            memberModifiers,
                            memberFlags);
        }

        M3Boundary boundary =
                M3Boundary.of(
                        classes, discovery.loaders, retainWeakJvmBoundary);

        Frozen frozen =
                new Frozen(
                        symbols.values.toArray(String[]::new),
                        symbols.order(),
                        boundary,
                        loaderSpaces,
                        binaryNameIds,
                        simpleNameIds,
                        canonicalNameIds,
                        packageNameIds,
                        moduleNameIds,
                        typeNameIds,
                        modifiers,
                        flags,
                        superclassRows,
                        componentTypeRows,
                        nestHostRows,
                        declaringClassRows,
                        enclosingClassRows,
                        structuralHashes,
                        interfaceOffsets,
                        interfaceRows,
                        directSubtypeOffsets,
                        directSubtypeRows,
                        supertypeOffsets,
                        supertypeRows,
                        classAnnotationOffsets,
                        classAnnotationNameIds,
                        permittedOffsets,
                        permittedNameIds,
                        nameIndex.keys,
                        nameIndex.offsets,
                        nameIndex.rows,
                        memberOffsets,
                        memberDeclaringRows,
                        memberKinds,
                        memberNameIds,
                        memberDescriptorIds,
                        memberModifiers,
                        memberFlags,
                        memberStructuralHashes,
                        memberAnnotationOffsets,
                        memberAnnotationNameIds,
                        memberExceptionOffsets,
                        memberExceptionDescriptorIds,
                        new byte[0]);

        return frozen.withFingerprint(fingerprint(frozen));
    }

    private static void admitMembers(Symbols symbols, int owner, Class<?> type, List<MemberDraft> out) {
        try {
            for (Field field : type.getDeclaredFields()) {
                out.add(
                        new MemberDraft(
                                owner,
                                M3MemberKind.FIELD,
                                symbols.add(field.getName()),
                                symbols.add(M3Descriptor.field(field)),
                                field.getModifiers(),
                                M3MemberFlags.of(field),
                                annotationNameIds(symbols, field),
                                new int[0]));
            }
            for (Method method : type.getDeclaredMethods()) {
                out.add(
                        new MemberDraft(
                                owner,
                                M3MemberKind.METHOD,
                                symbols.add(method.getName()),
                                symbols.add(M3Descriptor.method(method)),
                                method.getModifiers(),
                                M3MemberFlags.of(method),
                                annotationNameIds(symbols, method),
                                exceptionDescriptorIds(symbols, method.getExceptionTypes())));
            }
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                out.add(
                        new MemberDraft(
                                owner,
                                M3MemberKind.CONSTRUCTOR,
                                symbols.add(constructor.getName()),
                                symbols.add(M3Descriptor.constructor(constructor)),
                                constructor.getModifiers(),
                                M3MemberFlags.of(constructor),
                                annotationNameIds(symbols, constructor),
                                exceptionDescriptorIds(symbols, constructor.getExceptionTypes())));
            }
            RecordComponent[] components = type.getRecordComponents();
            if (components != null) {
                for (RecordComponent component : components) {
                    out.add(
                            new MemberDraft(
                                    owner,
                                    M3MemberKind.RECORD_COMPONENT,
                                    symbols.add(component.getName()),
                                    symbols.add(M3Descriptor.recordComponent(component)),
                                    0,
                                    M3MemberFlags.of(component),
                                    annotationNameIds(symbols, component),
                                    new int[0]));
                }
            }
        } catch (LinkageError | SecurityException failure) {
            throw new IllegalArgumentException(
                    "cannot freeze declared members of " + type.getName(), failure);
        }
    }

    private static int[] annotationNameIds(Symbols symbols, AnnotatedElement element) {
        Annotation[] annotations = element.getDeclaredAnnotations();
        int[] ids = new int[annotations.length];
        for (int i = 0; i < annotations.length; i++) {
            ids[i] = symbols.add(annotations[i].annotationType().getName());
        }
        return sortedUnique(ids);
    }

    private static int[] permittedNameIds(Symbols symbols, Class<?> type) {
        Class<?>[] classes = type.getPermittedSubclasses();
        if (classes == null || classes.length == 0) return new int[0];
        int[] ids = new int[classes.length];
        for (int i = 0; i < classes.length; i++) ids[i] = symbols.add(classes[i].getName());
        // Class.getPermittedSubclasses() order is retained as observed at the JVM boundary.
        return ids;
    }

    private static int[] exceptionDescriptorIds(Symbols symbols, Class<?>[] exceptionTypes) {
        int[] result = new int[exceptionTypes.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = symbols.add(M3Descriptor.type(exceptionTypes[i]));
        }
        return result;
    }

    private static int[][] reverseHierarchy(
            int classCount, int[] superclassRows, int[][] interfaces) {
        int[] counts = new int[classCount];
        for (int row = 0; row < classCount; row++) {
            if (superclassRows[row] >= 0) counts[superclassRows[row]]++;
            for (int iface : interfaces[row]) counts[iface]++;
        }
        int[][] result = new int[classCount][];
        for (int row = 0; row < classCount; row++) result[row] = new int[counts[row]];
        Arrays.fill(counts, 0);
        for (int row = 0; row < classCount; row++) {
            int superclass = superclassRows[row];
            if (superclass >= 0) result[superclass][counts[superclass]++] = row;
            for (int iface : interfaces[row]) result[iface][counts[iface]++] = row;
        }
        for (int[] rows : result) Arrays.sort(rows);
        return result;
    }

    private static int[][] transitiveSupertypes(
            int classCount,
            int[] superclassRows,
            int[][] interfaces,
            int[] flags,
            int objectRow) {
        int[][] result = new int[classCount][];
        for (int row = 0; row < classCount; row++) {
            BitSet visited = new BitSet(classCount);
            ArrayDeque<Integer> stack = new ArrayDeque<>();
            stack.addLast(row);
            if ((flags[row] & M3ClassFlags.INTERFACE) != 0 && objectRow >= 0) {
                stack.addLast(objectRow);
            }
            while (!stack.isEmpty()) {
                int current = stack.removeLast();
                if (visited.get(current)) continue;
                visited.set(current);
                int superclass = superclassRows[current];
                if (superclass >= 0) stack.addLast(superclass);
                for (int iface : interfaces[current]) stack.addLast(iface);
            }
            int[] rows = new int[visited.cardinality()];
            int output = 0;
            for (int bit = visited.nextSetBit(0); bit >= 0; bit = visited.nextSetBit(bit + 1)) {
                rows[output++] = bit;
            }
            result[row] = rows;
        }
        return result;
    }

    private static NameIndex buildNameIndex(int[] binaryNameIds) {
        TreeMap<Integer, ArrayList<Integer>> rows = new TreeMap<>();
        for (int row = 0; row < binaryNameIds.length; row++) {
            rows.computeIfAbsent(binaryNameIds[row], ignored -> new ArrayList<>()).add(row);
        }
        int[] keys = new int[rows.size()];
        int[] offsets = new int[rows.size() + 1];
        int total = 0;
        int keyIndex = 0;
        for (var entry : rows.entrySet()) {
            keys[keyIndex] = entry.getKey();
            offsets[keyIndex] = total;
            total = Math.addExact(total, entry.getValue().size());
            keyIndex++;
        }
        offsets[keyIndex] = total;
        int[] flattened = new int[total];
        int output = 0;
        for (ArrayList<Integer> values : rows.values()) {
            for (Integer row : values) flattened[output++] = row;
        }
        return new NameIndex(keys, offsets, flattened);
    }

    private static long classHash(
            int row,
            int[] loaderSpaces,
            int[] binaryNameIds,
            int[] simpleNameIds,
            int[] canonicalNameIds,
            int[] packageNameIds,
            int[] moduleNameIds,
            int[] typeNameIds,
            int[] modifiers,
            int[] flags,
            int[] superclassRows,
            int[] componentTypeRows,
            int[] nestHostRows,
            int[] declaringClassRows,
            int[] enclosingClassRows,
            int[] interfaceOffsets,
            int[] interfaceRows,
            int[] annotationOffsets,
            int[] annotationIds,
            int[] permittedOffsets,
            int[] permittedIds,
            int[] memberOffsets,
            byte[] memberKinds,
            int[] memberNameIds,
            int[] memberDescriptorIds,
            int[] memberModifiers,
            int[] memberFlags) {
        long hash = HASH_SEED;
        hash = hash(hash, loaderSpaces[row]);
        hash = hash(hash, binaryNameIds[row]);
        hash = hash(hash, simpleNameIds[row]);
        hash = hash(hash, canonicalNameIds[row]);
        hash = hash(hash, packageNameIds[row]);
        hash = hash(hash, moduleNameIds[row]);
        hash = hash(hash, typeNameIds[row]);
        hash = hash(hash, modifiers[row]);
        hash = hash(hash, flags[row]);
        hash = hash(hash, superclassRows[row]);
        hash = hash(hash, componentTypeRows[row]);
        hash = hash(hash, nestHostRows[row]);
        hash = hash(hash, declaringClassRows[row]);
        hash = hash(hash, enclosingClassRows[row]);
        for (int i = interfaceOffsets[row]; i < interfaceOffsets[row + 1]; i++) {
            hash = hash(hash, interfaceRows[i]);
        }
        for (int i = annotationOffsets[row]; i < annotationOffsets[row + 1]; i++) {
            hash = hash(hash, annotationIds[i]);
        }
        for (int i = permittedOffsets[row]; i < permittedOffsets[row + 1]; i++) {
            hash = hash(hash, permittedIds[i]);
        }
        for (int member = memberOffsets[row]; member < memberOffsets[row + 1]; member++) {
            hash = hash(hash, Byte.toUnsignedInt(memberKinds[member]));
            hash = hash(hash, memberNameIds[member]);
            hash = hash(hash, memberDescriptorIds[member]);
            hash = hash(hash, memberModifiers[member]);
            hash = hash(hash, memberFlags[member]);
        }
        return hash;
    }

    private static long memberHash(MemberDraft draft) {
        long hash = HASH_SEED;
        hash = hash(hash, draft.owner);
        hash = hash(hash, draft.kind.code());
        hash = hash(hash, draft.nameId);
        hash = hash(hash, draft.descriptorId);
        hash = hash(hash, draft.modifiers);
        hash = hash(hash, draft.flags);
        for (int annotation : draft.annotationNameIds) hash = hash(hash, annotation);
        for (int exception : draft.exceptionDescriptorIds) hash = hash(hash, exception);
        return hash;
    }

    private static long hash(long current, int value) {
        long hash = current;
        for (int shift = 0; shift < 32; shift += 8) {
            hash ^= (value >>> shift) & 0xffL;
            hash *= HASH_PRIME;
        }
        return hash;
    }

    private static byte[] fingerprint(Frozen frozen) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Owner-local integer coordinates alone do not identify textual content.
            update(digest, new int[] {frozen.texts.length});
            for (String value : frozen.texts) {
                update(digest, new int[] {value.length()});
                for (int i = 0; i < value.length(); i++) {
                    char c = value.charAt(i);
                    digest.update((byte) (c >>> 8)); digest.update((byte) c);
                }
            }
            update(digest, frozen.loaderSpaces);
            update(digest, frozen.binaryNameIds);
            update(digest, frozen.simpleNameIds);
            update(digest, frozen.canonicalNameIds);
            update(digest, frozen.packageNameIds);
            update(digest, frozen.moduleNameIds);
            update(digest, frozen.typeNameIds);
            update(digest, frozen.modifiers);
            update(digest, frozen.flags);
            update(digest, frozen.superclassRows);
            update(digest, frozen.componentTypeRows);
            update(digest, frozen.nestHostRows);
            update(digest, frozen.declaringClassRows);
            update(digest, frozen.enclosingClassRows);
            update(digest, frozen.structuralHashes);
            update(digest, frozen.interfaceOffsets);
            update(digest, frozen.interfaceRows);
            update(digest, frozen.directSubtypeOffsets);
            update(digest, frozen.directSubtypeRows);
            update(digest, frozen.supertypeOffsets);
            update(digest, frozen.supertypeRows);
            update(digest, frozen.classAnnotationOffsets);
            update(digest, frozen.classAnnotationNameIds);
            update(digest, frozen.permittedOffsets);
            update(digest, frozen.permittedNameIds);
            update(digest, frozen.nameKeys);
            update(digest, frozen.nameOffsets);
            update(digest, frozen.nameRows);
            update(digest, frozen.memberOffsets);
            update(digest, frozen.memberDeclaringRows);
            digest.update(frozen.memberKinds);
            update(digest, frozen.memberNameIds);
            update(digest, frozen.memberDescriptorIds);
            update(digest, frozen.memberModifiers);
            update(digest, frozen.memberFlags);
            update(digest, frozen.memberStructuralHashes);
            update(digest, frozen.memberAnnotationOffsets);
            update(digest, frozen.memberAnnotationNameIds);
            update(digest, frozen.memberExceptionOffsets);
            update(digest, frozen.memberExceptionDescriptorIds);
            return digest.digest();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void update(MessageDigest digest, int[] values) {
        for (int value : values) {
            digest.update((byte) (value >>> 24));
            digest.update((byte) (value >>> 16));
            digest.update((byte) (value >>> 8));
            digest.update((byte) value);
        }
    }

    private static void update(MessageDigest digest, long[] values) {
        for (long value : values) {
            for (int shift = 56; shift >= 0; shift -= 8) {
                digest.update((byte) (value >>> shift));
            }
        }
    }

    private static int[] offsets(int[][] rows) {
        int[] offsets = new int[rows.length + 1];
        for (int row = 0; row < rows.length; row++) {
            offsets[row + 1] = Math.addExact(offsets[row], rows[row].length);
        }
        return offsets;
    }

    private static int[] flatten(int[][] rows, int total) {
        int[] result = new int[total];
        int output = 0;
        for (int[] row : rows) {
            System.arraycopy(row, 0, result, output, row.length);
            output += row.length;
        }
        return result;
    }

    private static void prefixSum(int[] offsets) {
        for (int i = 1; i < offsets.length; i++) offsets[i] += offsets[i - 1];
    }

    private static int[] sortedUnique(int[] values) {
        if (values.length < 2) return values;
        Arrays.sort(values);
        int size = 1;
        for (int i = 1; i < values.length; i++) {
            if (values[i] != values[size - 1]) values[size++] = values[i];
        }
        return Arrays.copyOf(values, size);
    }

    private static int rowOf(IdentityHashMap<Class<?>, Integer> rows, Class<?> type) {
        if (type == null) return NONE;
        Integer row = rows.get(type);
        return row == null ? NONE : row;
    }

    private static int requiredRow(IdentityHashMap<Class<?>, Integer> rows, Class<?> type) {
        int row = rowOf(rows, type);
        if (row < 0) throw new IllegalStateException("hierarchy closure missing " + type.getName());
        return row;
    }



    private static int[] filled(int size, int value) {
        int[] result = new int[size];
        Arrays.fill(result, value);
        return result;
    }

    private static long totalLength(int[]... arrays) {
        long result = 0L;
        for (int[] array : arrays) result = Math.addExact(result, array.length);
        return result;
    }


    /** Snapshot-local reference coordinates; existing JDK Strings own all text payload. */
    private int findText(CharSequence value) {
        Objects.requireNonNull(value, "value");
        int low = 0, high = textOrder.length;
        while (low < high) {
            int mid = (low + high) >>> 1;
            int compared = compareText(texts[textOrder[mid]], value);
            if (compared < 0) low = mid + 1; else high = mid;
        }
        return low < textOrder.length && compareText(texts[textOrder[low]], value) == 0 ? textOrder[low] : NONE;
    }

    private static int compareText(String left, CharSequence right) {
        int length = Math.min(left.length(), right.length());
        for (int i = 0; i < length; i++) {
            int compared = Character.compare(left.charAt(i), right.charAt(i));
            if (compared != 0) return compared;
        }
        return Integer.compare(left.length(), right.length());
    }

    /** Builder only. Its map and boxed rows are discarded when the primitive snapshot freezes. */
    private static final class Symbols {
        private final java.util.HashMap<String, Integer> ids = new java.util.HashMap<>();
        private final ArrayList<String> values = new ArrayList<>();
        int add(String value) {
            Objects.requireNonNull(value, "value");
            Integer existing = ids.get(value);
            if (existing != null) return existing;
            int id = values.size(); ids.put(value, id); values.add(value); return id;
        }
        int[] order() {
            Integer[] order = new Integer[values.size()];
            for (int i = 0; i < order.length; i++) order[i] = i;
            Arrays.sort(order, Comparator.comparing(values::get));
            int[] result = new int[order.length];
            for (int i = 0; i < result.length; i++) result[i] = order[i];
            return result;
        }
    }

  private static int lowerBound(int[] sorted, int fromInclusive, int toExclusive, int value) {
    Objects.requireNonNull(sorted, "sorted");
    Objects.checkFromToIndex(fromInclusive, toExclusive, sorted.length);
    int low = fromInclusive;
    int high = toExclusive;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (sorted[middle] < value) {
        low = middle + 1;
      } else {
        high = middle;
      }
    }
    return low;
  }

  private static int upperBound(int[] sorted, int fromInclusive, int toExclusive, int value) {
    Objects.requireNonNull(sorted, "sorted");
    Objects.checkFromToIndex(fromInclusive, toExclusive, sorted.length);
    int low = fromInclusive;
    int high = toExclusive;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (sorted[middle] <= value) {
        low = middle + 1;
      } else {
        high = middle;
      }
    }
    return low;
  }

    private record Discovery(
            Class<?>[] classes,
            ClassLoader[] loaders,
            IdentityHashMap<ClassLoader, Integer> loaderIds) {
    }

    private record MemberDraft(
            int owner,
            M3MemberKind kind,
            int nameId,
            int descriptorId,
            int modifiers,
            int flags,
            int[] annotationNameIds,
            int[] exceptionDescriptorIds) {
    }

    private record NameIndex(int[] keys, int[] offsets, int[] rows) {
    }

    private record Frozen(
            String[] texts,
            int[] textOrder,
            M3Boundary boundary,
            int[] loaderSpaces,
            int[] binaryNameIds,
            int[] simpleNameIds,
            int[] canonicalNameIds,
            int[] packageNameIds,
            int[] moduleNameIds,
            int[] typeNameIds,
            int[] modifiers,
            int[] flags,
            int[] superclassRows,
            int[] componentTypeRows,
            int[] nestHostRows,
            int[] declaringClassRows,
            int[] enclosingClassRows,
            long[] structuralHashes,
            int[] interfaceOffsets,
            int[] interfaceRows,
            int[] directSubtypeOffsets,
            int[] directSubtypeRows,
            int[] supertypeOffsets,
            int[] supertypeRows,
            int[] classAnnotationOffsets,
            int[] classAnnotationNameIds,
            int[] permittedOffsets,
            int[] permittedNameIds,
            int[] nameKeys,
            int[] nameOffsets,
            int[] nameRows,
            int[] memberOffsets,
            int[] memberDeclaringRows,
            byte[] memberKinds,
            int[] memberNameIds,
            int[] memberDescriptorIds,
            int[] memberModifiers,
            int[] memberFlags,
            long[] memberStructuralHashes,
            int[] memberAnnotationOffsets,
            int[] memberAnnotationNameIds,
            int[] memberExceptionOffsets,
            int[] memberExceptionDescriptorIds,
            byte[] fingerprintSha256) {

        Frozen withFingerprint(byte[] fingerprint) {
            return new Frozen(
                    texts,
                    textOrder,
                    boundary,
                    loaderSpaces,
                    binaryNameIds,
                    simpleNameIds,
                    canonicalNameIds,
                    packageNameIds,
                    moduleNameIds,
                    typeNameIds,
                    modifiers,
                    flags,
                    superclassRows,
                    componentTypeRows,
                    nestHostRows,
                    declaringClassRows,
                    enclosingClassRows,
                    structuralHashes,
                    interfaceOffsets,
                    interfaceRows,
                    directSubtypeOffsets,
                    directSubtypeRows,
                    supertypeOffsets,
                    supertypeRows,
                    classAnnotationOffsets,
                    classAnnotationNameIds,
                    permittedOffsets,
                    permittedNameIds,
                    nameKeys,
                    nameOffsets,
                    nameRows,
                    memberOffsets,
                    memberDeclaringRows,
                    memberKinds,
                    memberNameIds,
                    memberDescriptorIds,
                    memberModifiers,
                    memberFlags,
                    memberStructuralHashes,
                    memberAnnotationOffsets,
                    memberAnnotationNameIds,
                    memberExceptionOffsets,
                    memberExceptionDescriptorIds,
                    fingerprint.clone());
        }
    }
}
