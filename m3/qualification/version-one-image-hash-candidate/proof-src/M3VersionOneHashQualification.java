// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;
import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** QA of one format-specific validation guard. Instrumentation never changes shipped source. */
public final class M3VersionOneHashQualification {
    private static Path fixture;
    private static long v1Calls = -1, v2Calls = -1;
    private static volatile Object blackhole;
    private static final List<String> WORDS = words();
    private static final List<String> TYPES = List.of("SharedLexiconImage", "LocalM3Arena",
            "LocalM3StringPiece", "M3StringPiece", "JoinedM3StringPiece", "StorageIdentity");

    private static List<String> words() {
        var values = new TreeSet<String>(List.of("", "A", "A\u030a", "Stra\u00dfe", "\u00c5",
                "\u0905", "\ud800", "\ud800x", "\ud83d\ude00", "\udfff", "\uffff"));
        for (int i = 0; i < 128; i++) values.add("w" + String.format(Locale.ROOT, "%03d", i));
        return List.copyOf(values);
    }
    private static Class<?> imageType() throws Exception {
        return Class.forName("jdk.m3.text.SharedLexiconImage");
    }
    private static byte[] image(int version, List<String> words) throws Exception {
        Class<?> reference = Class.forName("SharedLexiconImageContractTest");
        var method = reference.getDeclaredMethod("image", int.class, List.class);
        method.setAccessible(true);
        return (byte[])method.invoke(null, version, words);
    }
    private static byte[] seal(byte[] bytes) throws Exception {
        Class<?> reference = Class.forName("SharedLexiconImageContractTest");
        var method = reference.getDeclaredMethod("seal", byte[].class);
        method.setAccessible(true);
        return (byte[])method.invoke(null, (Object)bytes);
    }
    private static MappedByteBuffer buffer(byte[] bytes) {
        ByteBuffer value = ByteBuffer.allocateDirect(bytes.length);
        value.put(bytes).flip();
        return (MappedByteBuffer)value.asReadOnlyBuffer();
    }
    private static Object open(Class<?> type, MappedByteBuffer bytes) throws Exception {
        var constructor = type.getDeclaredConstructor(MappedByteBuffer.class);
        constructor.setAccessible(true);
        try { return constructor.newInstance(bytes); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Exception exception) throw exception;
            if (failure.getCause() instanceof Error error) throw error;
            throw failure;
        }
    }
    private static Object open(byte[] bytes) throws Exception {
        return open(imageType(), buffer(bytes));
    }
    private static Object call(Object target, String method, Class<?>[] parameters, Object... args)
            throws Exception {
        var operation = target.getClass().getDeclaredMethod(method, parameters);
        operation.setAccessible(true);
        try { return operation.invoke(target, args); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Exception exception) throw exception;
            if (failure.getCause() instanceof Error error) throw error;
            throw failure;
        }
    }

    /** Retrieves exact RAM-compiled bytes from the pinned compiler's owned output map. */
    private static Map<String, byte[]> compiledOwners() throws Exception {
        var result = new HashMap<String, byte[]>();
        ClassLoader loader = M3VersionOneHashQualification.class.getClassLoader();
        for (var field : loader.getClass().getDeclaredFields()) {
            if (!Map.class.isAssignableFrom(field.getType())) continue;
            field.setAccessible(true);
            if (!(field.get(loader) instanceof Map<?, ?> values)) continue;
            for (String name : TYPES) {
                String full = "jdk.m3.text." + name;
                Object bytes = values.get(full);
                if (bytes instanceof ByteArrayOutputStream output) result.put(full, output.toByteArray());
            }
        }
        assertEquals(TYPES.size(), result.size(), "exact compiler output owner set");
        return result;
    }

    /** Adds one counter increment at javaHash(I)I entry in a separate QA-only class loader. */
    private static Class<?> countedImage() throws Exception {
        Map<String, byte[]> classes = compiledOwners();
        String name = "jdk.m3.text.SharedLexiconImage";
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        new ClassReader(classes.get(name)).accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override public void visitEnd() {
                super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                        "qaJavaHashCalls", "J", null, null).visitEnd();
                super.visitEnd();
            }
            @Override public MethodVisitor visitMethod(int access, String n, String descriptor,
                    String signature, String[] exceptions) {
                var parent = super.visitMethod(access, n, descriptor, signature, exceptions);
                if (!n.equals("javaHash") || !descriptor.equals("(I)I")) return parent;
                return new MethodVisitor(Opcodes.ASM9, parent) {
                    @Override public void visitCode() {
                        super.visitCode();
                        super.visitFieldInsn(Opcodes.GETSTATIC, "jdk/m3/text/SharedLexiconImage",
                                "qaJavaHashCalls", "J");
                        super.visitInsn(Opcodes.LCONST_1);
                        super.visitInsn(Opcodes.LADD);
                        super.visitFieldInsn(Opcodes.PUTSTATIC, "jdk/m3/text/SharedLexiconImage",
                                "qaJavaHashCalls", "J");
                    }
                };
            }
        }, 0);
        classes.put(name, writer.toByteArray());
        ClassLoader loader = new ClassLoader(M3VersionOneHashQualification.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String n, boolean resolve) throws ClassNotFoundException {
                synchronized (getClassLoadingLock(n)) {
                    byte[] bytes = classes.get(n);
                    if (bytes == null) return super.loadClass(n, resolve);
                    Class<?> found = findLoadedClass(n);
                    if (found == null) found = defineClass(n, bytes, 0, bytes.length);
                    if (resolve) resolveClass(found);
                    return found;
                }
            }
        };
        Class<?> counted = loader.loadClass(name);
        assertNotSame(imageType().getClassLoader(), counted.getClassLoader());
        return counted;
    }

    @Test void unusedVersionOneHashPassIsElided() throws Exception {
        Class<?> type = countedImage();
        open(type, buffer(image(1, WORDS)));
        v1Calls = type.getField("qaJavaHashCalls").getLong(null);
        assertEquals(0, v1Calls, "version 1 does not store Java hashes");
    }
    @Test void versionTwoStillHashesEveryRecord() throws Exception {
        Class<?> type = countedImage();
        open(type, buffer(image(2, WORDS)));
        v2Calls = type.getField("qaJavaHashCalls").getLong(null);
        assertEquals(WORDS.size(), v2Calls);
    }
    @Test void versionTwoPreservesPayloadThenStoredHashReadOrder() throws Exception {
        var calls = new ArrayList<String>();
        new ClassReader(compiledOwners().get("jdk.m3.text.SharedLexiconImage"))
                .accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public MethodVisitor visitMethod(int access, String name, String descriptor,
                            String signature, String[] exceptions) {
                        if (!name.equals("<init>")) return null;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override public void visitMethodInsn(int opcode, String owner, String name,
                                    String descriptor, boolean isInterface) {
                                calls.add(name + descriptor);
                            }
                        };
                    }
                }, 0);
        int hash = calls.indexOf("javaHash(I)I");
        assertTrue(hash >= 0 && hash + 1 < calls.size());
        assertEquals("getInt(I)I", calls.get(hash + 1), "preserve existing v2 payload-before-lane evaluation");
    }
    @Test void bothFormatsKeepExactUtf16IdentityAndLookup() throws Exception {
        for (int version : List.of(1, 2)) {
            byte[] bytes = image(version, WORDS);
            Object value = open(bytes);
            assertEquals(version, call(value, "version", new Class<?>[]{}));
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes, 0, 32);
            digest.update(bytes, 64, bytes.length - 64);
            assertEquals(HexFormat.of().formatHex(digest.digest()),
                    call(value, "imageIdentity", new Class<?>[]{}));
            for (int row = 0; row < WORDS.size(); row++) {
                assertEquals(WORDS.get(row), call(value, "recordText", new Class<?>[]{int.class}, row));
                assertEquals(row, call(value, "findRecord", new Class<?>[]{String.class}, WORDS.get(row)));
            }
            assertEquals(-1, call(value, "findRecord", new Class<?>[]{String.class}, "absent"));
        }
    }
    private static void refuses(byte[] bytes, String message) {
        var exception = assertThrows(java.io.IOException.class, () -> open(bytes));
        assertTrue(exception.getMessage().contains(message), exception.toString());
    }
    @Test void formatDimensionOrderingAndHashRefusalsRemain() throws Exception {
        byte[] v2 = image(2, WORDS);
        byte[] wrongHash = v2.clone();
        ByteBuffer.wrap(wrongHash).putInt(64 + 8, 123);
        refuses(seal(wrongHash), "record Java hash mismatch");
        byte[] invalidVersion = v2.clone();
        ByteBuffer.wrap(invalidVersion).putInt(8, 3);
        refuses(invalidVersion, "unsupported image version");
        byte[] invalidRange = v2.clone();
        ByteBuffer.wrap(invalidRange).putInt(64, -1);
        refuses(seal(invalidRange), "invalid record range");
        byte[] invalidDimensions = v2.clone();
        ByteBuffer.wrap(invalidDimensions).putLong(16, 0);
        refuses(invalidDimensions, "invalid image dimensions");
        byte[] corrupted = image(1, WORDS);
        corrupted[corrupted.length - 1] ^= 1;
        refuses(corrupted, "image checksum mismatch");
        refuses(image(1, List.of("b", "a")), "records must be UTF-16 sorted");
        refuses(image(2, List.of("a", "a")), "records must be UTF-16 sorted");
    }
    @Test void recordMutationStillRefusesBeforeArenaOwnership() throws Exception {
        byte[] bytes = image(1, List.of("a", "b"));
        ByteBuffer mutable = ByteBuffer.allocateDirect(bytes.length);
        mutable.put(bytes).flip();
        Object value = open(imageType(), (MappedByteBuffer)mutable.asReadOnlyBuffer());
        int payload = Math.toIntExact(ByteBuffer.wrap(bytes).getLong(16));
        mutable.put(payload, (byte)'x');
        Class<?> arena = Class.forName("jdk.m3.text.LocalM3Arena");
        Object local = arena.getConstructor().newInstance();
        var failure = assertThrows(java.io.IOException.class,
                () -> call(value, "copyRecord", new Class<?>[]{int.class, arena}, 0, local));
        assertEquals("mapped record changed", failure.getMessage());
    }
    @Test void missingShaProviderStillRefuses() throws Exception {
        byte[] bytes = image(1, WORDS);
        Provider[] providers = Security.getProviders();
        var positions = new ArrayList<Integer>();
        for (int i = 0; i < providers.length; i++)
            if (providers[i].getService("MessageDigest", "SHA-256") != null) positions.add(i);
        for (int position : positions) Security.removeProvider(providers[position].getName());
        try {
            var failure = assertThrows(AssertionError.class, () -> open(bytes));
            assertInstanceOf(NoSuchAlgorithmException.class, failure.getCause());
        } finally {
            for (int position : positions) Security.insertProviderAt(providers[position], position + 1);
        }
        assertNotNull(MessageDigest.getInstance("SHA-256"));
    }
    @Test void publicFileOpenAndCopiesRemainExact() throws Exception {
        Object value = imageType().getMethod("open", Path.class).invoke(null, fixture);
        assertEquals(WORDS.size(), call(value, "size", new Class<?>[]{}));
        Class<?> arena = Class.forName("jdk.m3.text.LocalM3Arena");
        Object local = arena.getConstructor().newInstance();
        for (int row = 0; row < WORDS.size(); row++)
            assertEquals(WORDS.get(row), call(value, "copyRecord",
                    new Class<?>[]{int.class, arena}, row, local).toString());
    }

    /** Diagnostic timing of uninstrumented constructor only; no file-open/startup speed claim. */
    private static Map<String, Object> diagnostic() throws Exception {
        var words = new ArrayList<String>();
        for (int i = 0; i < 8192; i++)
            words.add("w" + String.format(Locale.ROOT, "%05d", i) + "-abcdefghijklmnopqrstuvwxyz012345");
        var result = new LinkedHashMap<String, Object>();
        var vm = ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);
        String perfShared = vm.getVMOption("PerfDisableSharedMem").getValue();
        String perfData = vm.getVMOption("UsePerfData").getValue();
        String savePerf = vm.getVMOption("PerfDataSaveToFile").getValue();
        assertEquals("true", perfShared);
        assertEquals("false", perfData);
        assertEquals("false", savePerf);
        result.put("vmFlags", Map.of("PerfDisableSharedMem", perfShared,
                "UsePerfData", perfData, "PerfDataSaveToFile", savePerf));
        var bean = ManagementFactory.getThreadMXBean();
        com.sun.management.ThreadMXBean allocation =
                bean instanceof com.sun.management.ThreadMXBean b && b.isThreadAllocatedMemorySupported() ? b : null;
        if (allocation != null && !allocation.isThreadAllocatedMemoryEnabled())
            allocation.setThreadAllocatedMemoryEnabled(true);
        for (int version : List.of(1, 2)) {
            MappedByteBuffer bytes = buffer(image(version, words));
            for (int i = 0; i < 2; i++) blackhole = open(imageType(), bytes);
            var times = new ArrayList<Long>();
            var allocations = new ArrayList<Long>();
            for (int i = 0; i < 5; i++) {
                long allocated = allocation == null ? -1 :
                        allocation.getThreadAllocatedBytes(Thread.currentThread().threadId());
                long started = System.nanoTime();
                blackhole = open(imageType(), bytes);
                times.add(System.nanoTime() - started);
                allocations.add(allocation == null ? -1 :
                        allocation.getThreadAllocatedBytes(Thread.currentThread().threadId()) - allocated);
            }
            result.put("v" + version, Map.of("records", words.size(), "mappedBytes", bytes.capacity(),
                    "utf16Units", words.stream().mapToLong(String::length).sum(),
                    "nanoseconds", times, "allocatedBytes", allocations));
        }
        return result;
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("mode, fixture, callback");
        fixture = Path.of(args[1]);
        if (args[0].equals("create")) {
            imageType().getMethod("create", Path.class, List.class).invoke(null, fixture, WORDS);
            System.out.println("M3_VERSION1_FIXTURE_CREATED records=" + WORDS.size());
            return;
        }
        if (!Set.of("before", "after").contains(args[0])) throw new IllegalArgumentException("mode");
        var listener = new SummaryGeneratingListener();
        var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(M3VersionOneHashQualification.class)).build());
        var summary = listener.getSummary();
        summary.printTo(new java.io.PrintWriter(System.out, true));
        summary.printFailuresTo(new java.io.PrintWriter(System.out, true));
        var report = new LinkedHashMap<String, Object>();
        report.put("mode", args[0]);
        report.put("found", summary.getTestsFoundCount());
        report.put("passed", summary.getTestsSucceededCount());
        report.put("failed", summary.getTestsFailedCount());
        report.put("aborted", summary.getTestsAbortedCount());
        report.put("v1JavaHashCalls", v1Calls);
        report.put("v2JavaHashCalls", v2Calls);
        report.put("fixtureRecords", WORDS.size());
        report.put("diagnostic", diagnostic());
        byte[] data = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(report);
        var connection = (HttpURLConnection)URI.create(args[2]).toURL().openConnection();
        connection.setRequestMethod("POST");connection.setDoOutput(true);
        connection.setConnectTimeout(10000);connection.setReadTimeout(10000);
        connection.setFixedLengthStreamingMode(data.length);
        try (var out = connection.getOutputStream()) { out.write(data); }
        if (connection.getResponseCode() != 204) throw new AssertionError("parent refused report");
        connection.disconnect();
        boolean pass = summary.getTestsFoundCount() == 8 && summary.getTestsSucceededCount() == 8
                && summary.getTestsFailedCount() == 0 && summary.getTestsAbortedCount() == 0;
        if (!pass) throw new AssertionError("M3_VERSION1_ELISION_DISCRIMINATOR_RED");
        System.out.println("M3_VERSION1_ELISION_PASS tests=8");
    }
}
