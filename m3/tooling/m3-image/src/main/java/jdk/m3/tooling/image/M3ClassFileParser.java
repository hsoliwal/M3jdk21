/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minimal class-file parser used for reachability and dynamic-boundary detection. */
public final class M3ClassFileParser {
    private static final int CLASS_MAGIC = 0xCAFEBABE;
    private static final int JAVA_21_CLASS_MAJOR = 65;
    private static final int ACC_NATIVE = 0x0100;
    private static final Pattern DESCRIPTOR_CLASS = Pattern.compile("L([^;:<]+)");
    private static final Set<String> STANDARD_BOOTSTRAP_OWNERS = Set.of(
            "java/lang/invoke/StringConcatFactory",
            "java/lang/invoke/LambdaMetafactory",
            "java/lang/invoke/ConstantBootstraps",
            "java/lang/runtime/ObjectMethods",
            "java/lang/runtime/SwitchBootstraps");

    private M3ClassFileParser() {
    }

    /** Parses one class file without defining or initializing the class. */
    public static M3ClassInfo parse(byte[] bytes, String inputId, String entryName) throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (input.readInt() != CLASS_MAGIC) {
                throw new IOException("Not a class file: " + entryName);
            }
            input.readUnsignedShort(); // minor
            int major = input.readUnsignedShort();
            if (major < 45 || major > JAVA_21_CLASS_MAJOR) {
                throw new IOException("Unsupported class-file major " + major + " for Java 21: " + entryName);
            }
            int constantPoolCount = input.readUnsignedShort();
            Object[] pool = new Object[constantPoolCount];
            String[] utf8 = new String[constantPoolCount];
            List<CpDynamic> dynamicConstants = new ArrayList<>();
            List<CpInvokeDynamic> invokeDynamics = new ArrayList<>();
            for (int index = 1; index < constantPoolCount; index++) {
                int tag = input.readUnsignedByte();
                switch (tag) {
                    case 1 -> {
                        String value = input.readUTF();
                        utf8[index] = value;
                        pool[index] = value;
                    }
                    case 3, 4 -> input.readInt();
                    case 5, 6 -> {
                        input.readLong();
                        index++;
                    }
                    case 7 -> pool[index] = new CpClass(input.readUnsignedShort());
                    case 8 -> pool[index] = new CpString(input.readUnsignedShort());
                    case 9, 10, 11 -> pool[index] = new CpRef(input.readUnsignedShort(), input.readUnsignedShort());
                    case 12 -> pool[index] = new CpNameType(input.readUnsignedShort(), input.readUnsignedShort());
                    case 15 -> pool[index] = new CpMethodHandle(input.readUnsignedByte(), input.readUnsignedShort());
                    case 16 -> pool[index] = new CpMethodType(input.readUnsignedShort());
                    case 17 -> {
                        CpDynamic dynamic = new CpDynamic(input.readUnsignedShort(), input.readUnsignedShort());
                        pool[index] = dynamic;
                        dynamicConstants.add(dynamic);
                    }
                    case 18 -> {
                        CpInvokeDynamic dynamic = new CpInvokeDynamic(input.readUnsignedShort(), input.readUnsignedShort());
                        pool[index] = dynamic;
                        invokeDynamics.add(dynamic);
                    }
                    case 19, 20 -> input.readUnsignedShort();
                    default -> throw new IOException("Unsupported constant-pool tag " + tag + " in " + entryName);
                }
            }

            input.readUnsignedShort(); // access flags
            int thisClassIndex = input.readUnsignedShort();
            input.readUnsignedShort(); // super class
            String internalName = className(pool, utf8, thisClassIndex);
            if (internalName == null || internalName.isBlank()) {
                throw new IOException("Missing this_class name in " + entryName);
            }
            String binaryName = internalName.replace('/', '.');

            SortedSet<String> references = new TreeSet<>();
            SortedSet<M3FeatureKind> features = new TreeSet<>();
            SortedSet<String> nativeMethods = new TreeSet<>();
            for (String value : utf8) {
                addTypeReferences(value, references);
            }
            for (Object constant : pool) {
                if (constant instanceof CpClass classConstant) {
                    addClassReference(utf8At(utf8, classConstant.nameIndex()), references);
                } else if (constant instanceof CpNameType nameType) {
                    addTypeReferences(utf8At(utf8, nameType.descriptorIndex()), references);
                } else if (constant instanceof CpMethodType methodType) {
                    addTypeReferences(utf8At(utf8, methodType.descriptorIndex()), references);
                }
            }
            references.remove(binaryName);

            int interfaceCount = input.readUnsignedShort();
            for (int index = 0; index < interfaceCount; index++) {
                input.readUnsignedShort();
            }
            skipMembers(input, utf8, references, null, features);
            skipMembers(input, utf8, references, nativeMethods, features);
            BootstrapTable bootstrapTable = readClassAttributes(input, utf8, entryName);
            detectDynamicBoundaries(pool, utf8, bootstrapTable, invokeDynamics, dynamicConstants, features);
            return new M3ClassInfo(binaryName, inputId, entryName, M3Hashing.sha256(bytes),
                    references, features, nativeMethods);
        } catch (EOFException exception) {
            throw new IOException("Truncated class file: " + entryName, exception);
        }
    }

    private static void detectDynamicBoundaries(Object[] pool, String[] utf8, BootstrapTable table,
                                                List<CpInvokeDynamic> invokeDynamics,
                                                List<CpDynamic> dynamicConstants,
                                                SortedSet<M3FeatureKind> features) throws IOException {
        Set<Integer> bootstrapHandleIndexes = new HashSet<>();
        Set<Integer> bootstrapArgumentIndexes = new HashSet<>();
        Set<Integer> standardBootstrapReferenceIndexes = new HashSet<>();
        for (BootstrapMethod method : table.methods()) {
            bootstrapHandleIndexes.add(method.methodHandleIndex());
            bootstrapArgumentIndexes.addAll(method.argumentIndexes());
            CpMethodHandle handle = methodHandleAt(pool, method.methodHandleIndex());
            String owner = referenceOwner(pool, utf8, handle.referenceIndex());
            if (STANDARD_BOOTSTRAP_OWNERS.contains(owner)) {
                standardBootstrapReferenceIndexes.add(handle.referenceIndex());
            }
        }
        for (int index = 1; index < pool.length; index++) {
            Object constant = pool[index];
            if (constant instanceof CpRef reference) {
                detectMethodBoundary(pool, utf8, reference, features,
                        standardBootstrapReferenceIndexes.contains(index));
            } else if (constant instanceof CpMethodHandle
                    && !bootstrapHandleIndexes.contains(index)
                    && !bootstrapArgumentIndexes.contains(index)) {
                features.add(M3FeatureKind.METHOD_HANDLES);
            }
        }
        for (CpInvokeDynamic dynamic : invokeDynamics) {
            String owner = bootstrapOwner(pool, utf8, table, dynamic.bootstrapMethodIndex());
            if (!STANDARD_BOOTSTRAP_OWNERS.contains(owner)) {
                features.add(M3FeatureKind.INVOKEDYNAMIC);
            }
        }
        for (CpDynamic dynamic : dynamicConstants) {
            String owner = bootstrapOwner(pool, utf8, table, dynamic.bootstrapMethodIndex());
            if (!STANDARD_BOOTSTRAP_OWNERS.contains(owner)) {
                features.add(M3FeatureKind.DYNAMIC_CONSTANT);
            }
        }
    }

    private static String bootstrapOwner(Object[] pool, String[] utf8, BootstrapTable table, int index)
            throws IOException {
        if (index < 0 || index >= table.methods().size()) {
            throw new IOException("Invalid bootstrap-method index " + index);
        }
        CpMethodHandle handle = methodHandleAt(pool, table.methods().get(index).methodHandleIndex());
        return referenceOwner(pool, utf8, handle.referenceIndex());
    }

    private static CpMethodHandle methodHandleAt(Object[] pool, int index) throws IOException {
        Object value = poolAt(pool, index);
        if (!(value instanceof CpMethodHandle handle)) {
            throw new IOException("Bootstrap method does not reference a method handle at cp#" + index);
        }
        return handle;
    }

    private static String referenceOwner(Object[] pool, String[] utf8, int referenceIndex) throws IOException {
        Object value = poolAt(pool, referenceIndex);
        if (!(value instanceof CpRef reference)) {
            throw new IOException("Method handle does not reference a method/field constant at cp#" + referenceIndex);
        }
        return className(pool, utf8, reference.classIndex());
    }

    private static void skipMembers(DataInputStream input, String[] utf8, SortedSet<String> references,
                                    SortedSet<String> nativeMethods, SortedSet<M3FeatureKind> features)
            throws IOException {
        int count = input.readUnsignedShort();
        for (int index = 0; index < count; index++) {
            int access = input.readUnsignedShort();
            int nameIndex = input.readUnsignedShort();
            int descriptorIndex = input.readUnsignedShort();
            String descriptor = utf8At(utf8, descriptorIndex);
            addTypeReferences(descriptor, references);
            if (nativeMethods != null && (access & ACC_NATIVE) != 0) {
                String method = utf8At(utf8, nameIndex) + descriptor;
                nativeMethods.add(method);
                features.add(M3FeatureKind.JNI_NATIVE_METHOD);
            }
            skipAttributes(input);
        }
    }

    private static BootstrapTable readClassAttributes(DataInputStream input, String[] utf8, String entryName)
            throws IOException {
        List<BootstrapMethod> methods = List.of();
        int count = input.readUnsignedShort();
        for (int index = 0; index < count; index++) {
            String name = utf8At(utf8, input.readUnsignedShort());
            long length = Integer.toUnsignedLong(input.readInt());
            if (!name.equals("BootstrapMethods")) {
                skipFully(input, length);
                continue;
            }
            if (!methods.isEmpty()) {
                throw new IOException("Duplicate BootstrapMethods attribute in " + entryName);
            }
            if (length > Integer.MAX_VALUE) {
                throw new IOException("Oversized BootstrapMethods attribute in " + entryName);
            }
            byte[] payload = input.readNBytes((int) length);
            if (payload.length != (int) length) {
                throw new EOFException("Truncated BootstrapMethods attribute");
            }
            methods = parseBootstrapMethods(payload, entryName);
        }
        return new BootstrapTable(methods);
    }

    private static List<BootstrapMethod> parseBootstrapMethods(byte[] payload, String entryName)
            throws IOException {
        List<BootstrapMethod> methods = new ArrayList<>();
        try (DataInputStream data = new DataInputStream(new ByteArrayInputStream(payload))) {
            int count = data.readUnsignedShort();
            for (int index = 0; index < count; index++) {
                int methodHandle = data.readUnsignedShort();
                int argumentCount = data.readUnsignedShort();
                List<Integer> arguments = new ArrayList<>(argumentCount);
                for (int argument = 0; argument < argumentCount; argument++) {
                    arguments.add(data.readUnsignedShort());
                }
                methods.add(new BootstrapMethod(methodHandle, List.copyOf(arguments)));
            }
            if (data.available() != 0) {
                throw new IOException("Trailing BootstrapMethods bytes in " + entryName);
            }
        }
        return List.copyOf(methods);
    }

    private static void skipAttributes(DataInputStream input) throws IOException {
        int count = input.readUnsignedShort();
        for (int index = 0; index < count; index++) {
            input.readUnsignedShort();
            skipFully(input, Integer.toUnsignedLong(input.readInt()));
        }
    }

    private static void skipFully(DataInputStream input, long length) throws IOException {
        long skipped = 0;
        while (skipped < length) {
            long step = input.skip(length - skipped);
            if (step <= 0) {
                if (input.read() < 0) {
                    throw new EOFException("Truncated attribute");
                }
                step = 1;
            }
            skipped += step;
        }
    }

    private static void detectMethodBoundary(Object[] pool, String[] utf8, CpRef reference,
                                             SortedSet<M3FeatureKind> features,
                                             boolean standardBootstrapReference) throws IOException {
        String owner = className(pool, utf8, reference.classIndex());
        Object nameTypeObject = poolAt(pool, reference.nameTypeIndex());
        if (!(nameTypeObject instanceof CpNameType nameType)) {
            return;
        }
        String name = utf8At(utf8, nameType.nameIndex());
        if (owner == null) {
            return;
        }
        if (owner.startsWith("java/lang/reflect/")) {
            features.add(M3FeatureKind.REFLECTION);
        }
        if (owner.startsWith("java/lang/invoke/") && !standardBootstrapReference) {
            features.add(M3FeatureKind.METHOD_HANDLES);
        }
        if (owner.equals("java/lang/Class") && (name.startsWith("getDeclared")
                || name.startsWith("getMethod") || name.startsWith("getField")
                || name.equals("newInstance"))) {
            features.add(M3FeatureKind.REFLECTION);
        }
        if (owner.equals("java/lang/Class") && name.equals("forName")) {
            features.add(M3FeatureKind.CLASS_LOADING);
            features.add(M3FeatureKind.REFLECTION);
        }
        if (owner.equals("java/lang/ClassLoader") && name.equals("loadClass")) {
            features.add(M3FeatureKind.CLASS_LOADING);
        }
        if (owner.equals("java/lang/reflect/Proxy")) {
            features.add(M3FeatureKind.DYNAMIC_PROXY);
        }
        if (owner.equals("java/util/ServiceLoader")) {
            features.add(M3FeatureKind.SERVICE_LOADER);
        }
        if ((owner.equals("java/lang/ClassLoader") || owner.equals("java/lang/Class"))
                && (name.equals("getResource") || name.equals("getResources")
                || name.equals("getResourceAsStream"))) {
            features.add(M3FeatureKind.RESOURCE_LOOKUP);
        }
        if ((owner.equals("java/lang/System") || owner.equals("java/lang/Runtime"))
                && (name.equals("load") || name.equals("loadLibrary")
                || name.equals("load0") || name.equals("loadLibrary0"))) {
            features.add(M3FeatureKind.NATIVE_LIBRARY_LOAD);
        }
    }

    /**
     * Adds an edge witnessed by CONSTANT_Class_info, not by an arbitrary UTF-8 token.
     * JVMS 4.4.1 allows an unqualified class name here: I is a class, while [I is
     * a primitive-array descriptor. Keep the legacy descriptor over-approximation.
     */
    private static void addClassReference(String name, SortedSet<String> references) {
        addTypeReferences(name, references);
        if (!name.isBlank() && name.charAt(0) != '[' && name.indexOf('/') < 0) {
            references.add(name);
        }
    }

    private static void addTypeReferences(String descriptorOrName, SortedSet<String> references) {
        if (descriptorOrName == null || descriptorOrName.isBlank()) {
            return;
        }
        if (descriptorOrName.charAt(0) == '[' || descriptorOrName.indexOf(';') >= 0
                || descriptorOrName.indexOf('<') >= 0) {
            Matcher matcher = DESCRIPTOR_CLASS.matcher(descriptorOrName);
            while (matcher.find()) {
                references.add(matcher.group(1).replace('/', '.'));
            }
        } else if (descriptorOrName.indexOf('/') >= 0 && descriptorOrName.indexOf('(') < 0) {
            references.add(descriptorOrName.replace('/', '.'));
        }
    }

    private static String className(Object[] pool, String[] utf8, int classIndex) throws IOException {
        Object value = poolAt(pool, classIndex);
        if (!(value instanceof CpClass classConstant)) {
            return null;
        }
        return utf8At(utf8, classConstant.nameIndex());
    }

    private static Object poolAt(Object[] pool, int index) throws IOException {
        if (index <= 0 || index >= pool.length) {
            throw new IOException("Invalid constant-pool index " + index);
        }
        return pool[index];
    }

    private static String utf8At(String[] utf8, int index) throws IOException {
        if (index <= 0 || index >= utf8.length || utf8[index] == null) {
            throw new IOException("Invalid UTF-8 constant-pool index " + index);
        }
        return utf8[index];
    }

    private record CpClass(int nameIndex) {
    }

    private record CpString(int valueIndex) {
    }

    private record CpRef(int classIndex, int nameTypeIndex) {
    }

    private record CpNameType(int nameIndex, int descriptorIndex) {
    }

    private record CpMethodHandle(int referenceKind, int referenceIndex) {
    }

    private record CpMethodType(int descriptorIndex) {
    }

    private record CpDynamic(int bootstrapMethodIndex, int nameTypeIndex) {
    }

    private record CpInvokeDynamic(int bootstrapMethodIndex, int nameTypeIndex) {
    }

    private record BootstrapMethod(int methodHandleIndex, List<Integer> argumentIndexes) {
    }

    private record BootstrapTable(List<BootstrapMethod> methods) {
    }
}
