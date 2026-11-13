/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.m3.graphics.impl.M3Cleanable;
import jdk.m3.graphics.impl.M3Library;
import jdk.m3.graphics.impl.M3ReferenceUtil;
import jdk.m3.graphics.impl.M3ResourceFinder;

/**
 * Run-time probe of the Skija leaf-1 absorption, gates G5 and G6 of {@code absorb_leaf1.sh}. On the running JDK it
 * checks the three classes MR_SELECT v1 takes from {@code META-INF/versions/9} and the class-file name STRING_RENAME v1
 * fixes in {@code M3Library}. It prints one PASS or FAIL line per check and exits with status 1 if a check fails.
 *
 * <pre>
 * java --module-path &lt;module&gt; --add-modules jdk.m3.graphics -cp &lt;probe&gt; M3GraphicsLeaf1Probe
 * &lt;jlink image&gt;/bin/java -cp &lt;probe&gt; M3GraphicsLeaf1Probe jrt
 * </pre>
 */
public final class M3GraphicsLeaf1Probe {
    private static final String JRT = "jrt";
    private static final int GC_ROUNDS = 50;
    private static final long GC_WAIT_MILLIS = 100;
    private static final String SYSTEM_MODULE = "java.base";
    private static final String ENCAPSULATED_RESOURCE = "java/lang/uniName.dat";
    private static final String REFERENCE_CLASS = "java/lang/ref/Reference";
    private static final String FENCE_METHOD = "reachabilityFence";
    private static final String CLASS_FILE = "M3Library.class";
    private static final String DONOR_CLASS_FILE = "Library.class";
    private static final String LOG_LEVEL_PROPERTY = "skija.logLevel";
    private static final String LOG_LEVEL = "DEBUG";
    private static final String BUNDLED_BRANCH_LOG = "Loading skija native library from library path";
    private static final int CLASS_FILE_MAGIC = 0xCAFEBABE;
    // constant pool tags and entry sizes after the tag byte (JVMS 4.4)
    private static final int TAG_UTF8 = 1;
    private static final int TAG_INTEGER = 3;
    private static final int TAG_FLOAT = 4;
    private static final int TAG_LONG = 5;
    private static final int TAG_DOUBLE = 6;
    private static final int TAG_CLASS = 7;
    private static final int TAG_STRING = 8;
    private static final int TAG_FIELDREF = 9;
    private static final int TAG_METHODREF = 10;
    private static final int TAG_INTERFACE_METHODREF = 11;
    private static final int TAG_NAME_AND_TYPE = 12;
    private static final int TAG_METHOD_HANDLE = 15;
    private static final int TAG_METHOD_TYPE = 16;
    private static final int TAG_DYNAMIC = 17;
    private static final int TAG_INVOKE_DYNAMIC = 18;
    private static final int TAG_MODULE = 19;
    private static final int TAG_PACKAGE = 20;
    private static final int CONSTANT_BYTES_REFERENCE = 2;
    private static final int CONSTANT_BYTES_HANDLE = 3;
    private static final int CONSTANT_BYTES_PAIR = 4;
    private static final int CONSTANT_BYTES_WIDE = 8;
    private static final Set<Integer> REFERENCE_TAGS = Set.of(TAG_CLASS, TAG_STRING, TAG_METHOD_TYPE, TAG_MODULE,
            TAG_PACKAGE);
    private static final Set<Integer> PAIR_TAGS = Set.of(TAG_INTEGER, TAG_FLOAT, TAG_FIELDREF, TAG_METHODREF,
            TAG_INTERFACE_METHODREF, TAG_NAME_AND_TYPE, TAG_DYNAMIC, TAG_INVOKE_DYNAMIC);

    private static int failures;

    private M3GraphicsLeaf1Probe() {
    }

    /**
     * Runs the checks.
     *
     * @param args {@code jrt} for the checks inside a runtime image, none for the module-path checks
     * @throws Exception if a check cannot run at all
     */
    public static void main(String[] args) throws Exception {
        System.out.println("java.version=" + System.getProperty("java.version"));
        if (args.length > 0 && JRT.equals(args[0])) {
            checkBundledBranch();
        } else {
            checkCleanable();
            checkReferenceUtil();
            checkResourceFinder();
            checkLibraryClassFileName();
        }
        System.out.println(failures == 0 ? "PROBE PASS" : "PROBE FAIL " + failures);
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String id, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + id + " " + detail);
        failures += ok ? 0 : 1;
    }

    /** F1: the class initializes on this JDK and cleans explicitly, through the GC, and rejects null. */
    private static void checkCleanable() {
        try {
            Class.forName(M3Cleanable.class.getName(), true, M3Cleanable.class.getClassLoader());
            check("F1.init", true, "M3Cleanable initialized");
            AtomicInteger runs = new AtomicInteger();
            M3Cleanable cleanable = M3Cleanable.register(new Object(), runs::incrementAndGet);
            cleanable.clean();
            cleanable.clean();
            check("F1.clean", runs.get() == 1, "two clean() calls ran the action " + runs.get() + " time(s)");
            check("F1.gc", cleanedByGc(), "the action ran after the referent became unreachable");
            check("F1.null", rejectsNull(), "register(null, action) and register(object, null) throw NPE");
        } catch (Throwable t) {
            check("F1.init", false, t.toString());
        }
    }

    private static boolean cleanedByGc() throws InterruptedException {
        CountDownLatch cleaned = new CountDownLatch(1);
        M3Cleanable cleanable = registerUnreachable(cleaned);
        for (int round = 0; round < GC_ROUNDS && cleaned.getCount() > 0; round++) {
            System.gc();
            cleaned.await(GC_WAIT_MILLIS, TimeUnit.MILLISECONDS);
        }
        M3ReferenceUtil.reachabilityFence(cleanable);
        return cleaned.getCount() == 0;
    }

    private static M3Cleanable registerUnreachable(CountDownLatch cleaned) {
        return M3Cleanable.register(new Object(), cleaned::countDown);
    }

    private static boolean rejectsNull() {
        return throwsNullPointer(() -> M3Cleanable.register(null, () -> { }))
                && throwsNullPointer(() -> M3Cleanable.register(new Object(), null));
    }

    private static boolean throwsNullPointer(Runnable call) {
        try {
            call.run();
            return false;
        } catch (NullPointerException expected) {
            return true;
        }
    }

    /** F2: the fence forwards to Reference.reachabilityFence (the root variant has an empty body). */
    private static void checkReferenceUtil() throws IOException {
        M3ReferenceUtil.reachabilityFence(new Object());
        Set<String> utf8 = utf8Constants(M3ReferenceUtil.class);
        check("F2.fence", utf8.contains(REFERENCE_CLASS) && utf8.contains(FENCE_METHOD),
                "M3ReferenceUtil.reachabilityFence calls " + REFERENCE_CLASS + "." + FENCE_METHOD);
    }

    /** F3: a resource the class loader does not return is found inside the named module (the root variant asks only
     * the class loader). */
    private static void checkResourceFinder() throws IOException {
        URL byLoader = M3ResourceFinder.class.getClassLoader().getResource(ENCAPSULATED_RESOURCE);
        try (M3ResourceFinder finder = new M3ResourceFinder(SYSTEM_MODULE)) {
            URL found = finder.findResource(ENCAPSULATED_RESOURCE);
            check("F3.module", byLoader == null && found != null,
                    ENCAPSULATED_RESOURCE + ": class loader " + byLoader + ", module " + SYSTEM_MODULE + " " + found);
        }
    }

    /** F4: M3Library looks for its own class file, which exists next to it. */
    private static void checkLibraryClassFileName() throws IOException {
        Set<String> utf8 = utf8Constants(M3Library.class);
        check("F4.name", utf8.contains(CLASS_FILE) && !utf8.contains(DONOR_CLASS_FILE),
                "M3Library looks up \"" + CLASS_FILE + "\", not \"" + DONOR_CLASS_FILE + "\"");
        check("F4.resource", M3Library.class.getResource(CLASS_FILE) != null,
                CLASS_FILE + " -> " + M3Library.class.getResource(CLASS_FILE));
    }

    /** F4 in a runtime image: load() takes the JDK-bundled branch; without leaf 2 natives it then fails to link. */
    private static void checkBundledBranch() {
        System.setProperty(LOG_LEVEL_PROPERTY, LOG_LEVEL);
        URL self = M3Library.class.getResource(CLASS_FILE);
        check("F4.jrt", self != null && JRT.equals(self.getProtocol()), "M3Library is in the runtime image: " + self);
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        PrintStream out = System.out;
        Throwable thrown = null;
        System.setOut(new PrintStream(log, true, StandardCharsets.UTF_8));
        try {
            M3Library.load();
        } catch (Throwable t) {
            thrown = t;
        } finally {
            System.setOut(out);
        }
        check("F4.bundled", log.toString(StandardCharsets.UTF_8).contains(BUNDLED_BRANCH_LOG),
                "load() logged \"" + BUNDLED_BRANCH_LOG + "\", then " + thrown);
    }

    private static Set<String> utf8Constants(Class<?> type) throws IOException {
        try (InputStream in = type.getResourceAsStream(type.getSimpleName() + ".class");
                DataInputStream data = new DataInputStream(in)) {
            if (data.readInt() != CLASS_FILE_MAGIC) {
                throw new IOException("not a class file: " + type.getName());
            }
            data.readUnsignedShort();
            data.readUnsignedShort();
            return readUtf8(data, data.readUnsignedShort());
        }
    }

    private static Set<String> readUtf8(DataInputStream data, int count) throws IOException {
        Set<String> utf8 = new HashSet<>();
        for (int index = 1; index < count; index++) {
            int tag = data.readUnsignedByte();
            if (tag == TAG_UTF8) {
                utf8.add(data.readUTF());
            } else {
                data.skipNBytes(constantSize(tag));
                index += (tag == TAG_LONG || tag == TAG_DOUBLE) ? 1 : 0;
            }
        }
        return utf8;
    }

    private static int constantSize(int tag) throws IOException {
        if (REFERENCE_TAGS.contains(tag)) {
            return CONSTANT_BYTES_REFERENCE;
        } else if (PAIR_TAGS.contains(tag)) {
            return CONSTANT_BYTES_PAIR;
        } else if (tag == TAG_LONG || tag == TAG_DOUBLE) {
            return CONSTANT_BYTES_WIDE;
        } else if (tag == TAG_METHOD_HANDLE) {
            return CONSTANT_BYTES_HANDLE;
        }
        throw new IOException("unknown constant pool tag " + tag);
    }
}
