// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import org.junit.jupiter.api.Test;

/**
 * Differential proof for the one-probe {@code remove(int, int)} atom
 * (qualification/collections-atoms-20261106, com.synexia crate m3-intint-conditional-remove-20261030).
 *
 * <p>The live class, a {@link HashMap} reference and the pre-change class (master blob d50114519d36, compiled at
 * run time from the hash-pinned fixture into a separate package and class loader) must produce byte-identical
 * dumps over {@value #OPS} seeded operations drawn from put, remove(key), remove(key, expected), getOrDefault,
 * containsKey and size. The live bytecode must call {@code find} once and neither {@code containsKey} nor
 * {@code getOrDefault} before delegating to {@code remove(int)} (the class is final and {@code find} is private,
 * so the probe count is asserted on the javap listing rather than through a runtime hook).</p>
 */
final class M3IntIntHashMapConditionalRemoveDifferentialTest {
    static final long SEED = 0x4D33_494E_5449_4E54L;
    static final int OPS = 1_000_000;
    static final String FIXTURE = "/jdk/m3/collections/atoms/M3IntIntHashMap.preimage.java.txt";
    static final String FIXTURE_SHA256 = "9fb476f33f85532d0e8809cc107dcf270d38ff9381fb155a4d1131f13b0bbf5e";
    static final String OWNER_PACKAGE_LINE = "package jdk.m3.collections;";
    static final String PREIMAGE_PACKAGE = "jdk.m3.collections.atoms.preimage";
    static final int[] EXTREMES = {Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 1, 0x7feb352d, 0x40000000};

    interface Subject {
        boolean put(int key, int value);

        boolean remove(int key);

        boolean remove(int key, int expectedValue);

        int getOrDefault(int key, int defaultValue);

        boolean containsKey(int key);

        int size();
    }

    @Test
    void liveReferenceAndPreimageDumpsAreIdentical() throws Exception {
        String live = dump(live());
        String reference = dump(reference());
        String preimage = dump(preimage());
        assertEquals(reference, live, "live vs java.util.HashMap reference");
        assertEquals(preimage, live, "live vs pre-change class");
        System.out.println("M3_INTINT_CONDITIONAL_REMOVE_DIFFERENTIAL ops=" + OPS + " seed=" + Long.toHexString(SEED)
                + " live=" + live + " reference=" + reference + " preimage=" + preimage);
    }

    @Test
    void liveConditionalRemoveProbesOnceBeforeDelegating() throws Exception {
        java.util.spi.ToolProvider javap = java.util.spi.ToolProvider.findFirst("javap").orElseThrow();
        Path classFile = Path.of(M3IntIntHashMap.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("jdk/m3/collections/M3IntIntHashMap.class");
        assertTrue(Files.isRegularFile(classFile), classFile.toString());
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int rc = javap.run(new PrintWriter(out), new PrintWriter(err), "-c", "-p", classFile.toString());
        assertEquals(0, rc, err.toString());
        String listing = out.toString().replace("\r\n", "\n");
        int start = listing.indexOf("public boolean remove(int, int);");
        assertTrue(start >= 0, listing);
        int end = listing.indexOf("\n\n", start);
        String body = listing.substring(start, end < 0 ? listing.length() : end);
        assertEquals(1, count(body, "Method find:(I)I"), body);
        assertEquals(0, count(body, "containsKey"), body);
        assertEquals(0, count(body, "getOrDefault"), body);
        assertEquals(2, count(body, "Method remove:(I)Z"), body);
    }

    static int count(String text, String needle) {
        int found = 0;
        for (int at = text.indexOf(needle); at >= 0; at = text.indexOf(needle, at + needle.length())) {
            found++;
        }
        return found;
    }

    static String dump(Subject subject) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        SplittableRandom random = new SplittableRandom(SEED);
        byte[] record = new byte[13];
        for (int i = 0; i < OPS; i++) {
            int op = random.nextInt(16);
            int key = key(random);
            int arg = 0;
            int result;
            switch (op) {
                case 0, 1, 2, 3, 4 -> {
                    arg = random.nextInt(-8, 8);
                    result = subject.put(key, arg) ? 1 : 0;
                }
                case 5, 6 -> result = subject.remove(key) ? 1 : 0;
                case 7, 8, 9 -> {
                    arg = random.nextBoolean() ? subject.getOrDefault(key, Integer.MIN_VALUE) : random.nextInt(-8, 8);
                    result = subject.remove(key, arg) ? 1 : 0;
                }
                case 10, 11, 12 -> {
                    arg = random.nextInt();
                    result = subject.getOrDefault(key, arg);
                }
                case 13, 14 -> result = subject.containsKey(key) ? 1 : 0;
                default -> result = subject.size();
            }
            record[0] = (byte) op;
            putInt(record, 1, key);
            putInt(record, 5, arg);
            putInt(record, 9, result);
            digest.update(record);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    static int key(SplittableRandom random) {
        int shape = random.nextInt(16);
        if (shape == 0) {
            return 0;
        }
        if (shape == 1) {
            return EXTREMES[random.nextInt(EXTREMES.length)];
        }
        if (shape < 10) {
            return random.nextInt(-48, 48);
        }
        if (shape < 14) {
            return random.nextInt(-100_000, 100_000);
        }
        return random.nextInt();
    }

    static void putInt(byte[] record, int at, int value) {
        record[at] = (byte) (value >>> 24);
        record[at + 1] = (byte) (value >>> 16);
        record[at + 2] = (byte) (value >>> 8);
        record[at + 3] = (byte) value;
    }

    static Subject live() {
        M3IntIntHashMap map = new M3IntIntHashMap();
        return new Subject() {
            @Override public boolean put(int key, int value) { return map.put(key, value); }
            @Override public boolean remove(int key) { return map.remove(key); }
            @Override public boolean remove(int key, int expectedValue) { return map.remove(key, expectedValue); }
            @Override public int getOrDefault(int key, int defaultValue) { return map.getOrDefault(key, defaultValue); }
            @Override public boolean containsKey(int key) { return map.containsKey(key); }
            @Override public int size() { return map.size(); }
        };
    }

    static Subject reference() {
        Map<Integer, Integer> map = new HashMap<>();
        return new Subject() {
            @Override public boolean put(int key, int value) { return map.put(key, value) == null; }
            @Override public boolean remove(int key) { return map.remove(key) != null; }
            @Override public boolean remove(int key, int expectedValue) { return map.remove(key, expectedValue); }
            @Override public int getOrDefault(int key, int defaultValue) { return map.getOrDefault(key, defaultValue); }
            @Override public boolean containsKey(int key) { return map.containsKey(key); }
            @Override public int size() { return map.size(); }
        };
    }

    static Subject preimage() throws Exception {
        byte[] source;
        try (InputStream in = M3IntIntHashMapConditionalRemoveDifferentialTest.class.getResourceAsStream(FIXTURE)) {
            assertNotNull(in, FIXTURE);
            source = in.readAllBytes();
        }
        assertEquals(FIXTURE_SHA256, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source)),
                "fixture digest (master blob d50114519d36)");
        String text = new String(source, StandardCharsets.UTF_8);
        assertTrue(text.contains(OWNER_PACKAGE_LINE), OWNER_PACKAGE_LINE);
        String rewritten = text.replace(OWNER_PACKAGE_LINE, "package " + PREIMAGE_PACKAGE + ";\n\n"
                + "import jdk.m3.collections.M3IntIntConsumer;\nimport jdk.m3.collections.M3IntIterator;");
        Path out = Files.createTempDirectory("m3-intint-preimage");
        JavaCompiler javac = javax.tools.ToolProvider.getSystemJavaCompiler();
        assertNotNull(javac, "a JDK compiler is required");
        String classpath = Path.of(M3IntIntConsumer.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .toString();
        JavaFileObject unit = new SimpleJavaFileObject(
                URI.create("string:///" + PREIMAGE_PACKAGE.replace('.', '/') + "/M3IntIntHashMap.java"),
                JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return rewritten; }
        };
        StringWriter diagnostics = new StringWriter();
        Boolean ok = javac.getTask(diagnostics, null, null,
                List.of("-d", out.toString(), "-cp", classpath, "--release", "21", "-Xlint:all", "-Werror", "-proc:none"),
                null, List.of(unit)).call();
        assertTrue(ok, diagnostics.toString());
        URLClassLoader loader = new URLClassLoader(new URL[] {out.toUri().toURL()}, M3IntIntHashMap.class.getClassLoader());
        Class<?> type = Class.forName(PREIMAGE_PACKAGE + ".M3IntIntHashMap", true, loader);
        assertNotSame(M3IntIntHashMap.class, type);
        Object instance = type.getConstructor().newInstance();
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodHandle put = lookup.findVirtual(type, "put",
                MethodType.methodType(boolean.class, int.class, int.class)).bindTo(instance);
        MethodHandle remove = lookup.findVirtual(type, "remove",
                MethodType.methodType(boolean.class, int.class)).bindTo(instance);
        MethodHandle removeIf = lookup.findVirtual(type, "remove",
                MethodType.methodType(boolean.class, int.class, int.class)).bindTo(instance);
        MethodHandle getOrDefault = lookup.findVirtual(type, "getOrDefault",
                MethodType.methodType(int.class, int.class, int.class)).bindTo(instance);
        MethodHandle containsKey = lookup.findVirtual(type, "containsKey",
                MethodType.methodType(boolean.class, int.class)).bindTo(instance);
        MethodHandle size = lookup.findVirtual(type, "size", MethodType.methodType(int.class)).bindTo(instance);
        return new Subject() {
            @Override public boolean put(int key, int value) {
                try { return (boolean) put.invokeExact(key, value); } catch (Throwable failure) { throw new AssertionError(failure); }
            }
            @Override public boolean remove(int key) {
                try { return (boolean) remove.invokeExact(key); } catch (Throwable failure) { throw new AssertionError(failure); }
            }
            @Override public boolean remove(int key, int expectedValue) {
                try { return (boolean) removeIf.invokeExact(key, expectedValue); } catch (Throwable failure) { throw new AssertionError(failure); }
            }
            @Override public int getOrDefault(int key, int defaultValue) {
                try { return (int) getOrDefault.invokeExact(key, defaultValue); } catch (Throwable failure) { throw new AssertionError(failure); }
            }
            @Override public boolean containsKey(int key) {
                try { return (boolean) containsKey.invokeExact(key); } catch (Throwable failure) { throw new AssertionError(failure); }
            }
            @Override public int size() {
                try { return (int) size.invokeExact(); } catch (Throwable failure) { throw new AssertionError(failure); }
            }
        };
    }
}
