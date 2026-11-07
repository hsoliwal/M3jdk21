// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Standalone Java 21 contract and in-memory compiler/JNI oracle, with no test-runtime dependency. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-class-space-canonical-images-20261030/src/test/java/com/synexia/m3/classspace/M3ClassSpaceSelfTest.java blob 5be4cb4045a9; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
public final class M3ClassSpaceSelfTest {
    private static int checks;

    private M3ClassSpaceSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        verifyCanonicalBytes();
        verifyExactCollisionRefusal();
        verifyBudgetsAndErrors();
        verifyConcurrentReuse();
        verifyIndependentClassLoaderIdentity();
        if (args.length != 0) {
            System.load(args[0]);
            verifyNativeParity();
        }
        System.out.println("M3ClassSpace Java21 self-test PASS: " + checks + " assertions"
                + (args.length == 0 ? "; JNI NOT_RUN" : "; checked JNI PASS"));
    }

    private static void verifyCanonicalBytes() {
        M3ClassSpace space = new M3ClassSpace(1 << 20);
        byte[] seed = "abc".getBytes(StandardCharsets.UTF_8);
        int id = space.intern(seed);
        check(id == space.intern(seed.clone()), "exact duplicate must reuse image ID");
        check(space.imageCount() == 1, "one content image");
        check(space.retainedByteCount() == 3, "one byte image");
        check(space.digestHex(id).equals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),
                "SHA-256 known-answer test");
        seed[0] = 0;
        check(space.copy(id)[0] == 'a', "input must be copied, not retained");
        byte[] copy = space.copy(id);
        copy[0] = 0;
        check(space.copy(id)[0] == 'a', "mutable outputs must be defensive copies");

        Random random = new Random(0x3aa20261030L);
        int[] sizes = {0, 1, 2, 3, 31, 32, 33, 255, 4095, 4096, 4097, 8193};
        for (int pass = 0; pass < 8; pass++) {
            for (int length : sizes) {
                byte[] source = new byte[length];
                random.nextBytes(source);
                int observed = space.intern(source);
                check(Arrays.equals(source, space.copy(observed)), "lossless byte reconstruction");
                check(space.intern(source.clone()) == observed, "copy identity equivalence");
                if (source.length > 0) {
                    source[source.length - 1] ^= 1;
                    check(space.intern(source) != observed, "last-byte mutation must not alias");
                }
            }
        }
    }

    private static void verifyExactCollisionRefusal() {
        M3ClassSpace space = new M3ClassSpace(8 * 1024, data -> new byte[32]);
        byte[] alpha = {0, 1, -1, 7};
        byte[] beta = {0, 1, -1, 8};
        int a = space.intern(alpha);
        int b = space.intern(beta);
        check(a != b, "same 256-bit candidate hash does not establish equality");
        check(space.intern(alpha.clone()) == a, "collision-chain lookup still deduplicates");
        check(space.intern(beta.clone()) == b, "collision-chain lookup preserves distinct data");
        // Exercise multiple bucket rebuilds under a deliberately constant candidate digest.
        for (int i = 0; i < 100; i++) {
            int id = space.intern(new byte[]{(byte) i, (byte) (i >>> 8)});
            check(Arrays.equals(space.copy(id), new byte[]{(byte) i, (byte) (i >>> 8)}),
                    "rehash must preserve collisions and ordinal offsets");
        }
    }

    private static void verifyBudgetsAndErrors() {
        M3ClassSpace space = new M3ClassSpace(3);
        int first = space.intern(new byte[]{1, 2, 3});
        check(space.intern(new byte[]{1, 2, 3}) == first, "duplicate at full quota succeeds");
        expect(IllegalStateException.class, () -> space.intern(new byte[]{4}));
        expect(NullPointerException.class, () -> space.intern(null));
        expect(IndexOutOfBoundsException.class, () -> space.copy(-1));
        expect(IndexOutOfBoundsException.class, () -> space.digestHex(1));
        expect(NullPointerException.class, () -> space.bind(null, first));
        expect(IllegalArgumentException.class, () -> new M3ClassSpace(0));
        expect(IllegalArgumentException.class, () -> {
            M3ClassSpace invalidHash = new M3ClassSpace(10, bytes -> new byte[31]);
            invalidHash.intern(new byte[]{1});
        });
    }

    private static void verifyConcurrentReuse() throws Exception {
        M3ClassSpace space = new M3ClassSpace(256 * 1024);
        byte[] data = new byte[4099];
        new Random(31).nextBytes(data);
        int expected = space.intern(data);
        ExecutorService service = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < 96; i++) {
                tasks.add(() -> space.intern(data.clone()));
            }
            for (Future<Integer> future : service.invokeAll(tasks)) {
                check(future.get() == expected, "concurrent identical content single owner ID");
            }
            check(space.imageCount() == 1, "concurrent reuse does not duplicate storage");
        } finally {
            service.shutdownNow();
        }
    }

    private static void verifyIndependentClassLoaderIdentity() throws Exception {
        String className = "m3.fixture.Probe";
        String source = """
                package m3.fixture;
                public final class Probe {
                    public int twice(int input) { return input * 2; }
                    public String codeLikeString() {
                        return "class Fake { public void run() {} } [a-z]+";
                    }
                }
                """;
        byte[] classfile = inMemoryCompile(className, source);
        M3ClassSpace space = new M3ClassSpace(1 << 20);
        int imageId = space.intern(classfile);
        Class<?> leftClass = new BytesLoader(className, classfile).define();
        Class<?> rightClass = new BytesLoader(className, classfile).define();
        M3ClassSpace.Binding left = space.bind(leftClass, imageId);
        M3ClassSpace.Binding right = space.bind(rightClass, imageId);
        check(left.imageId() == right.imageId(), "same classfile bytes may share immutable image");
        check(left.runtimeClass() != right.runtimeClass(), "loader-scoped Class identity is distinct");
        check(left.runtimeClass().getClassLoader() != right.runtimeClass().getClassLoader(),
                "class loader objects remain distinct");
        Method twice = left.runtimeClass().getMethod("twice", int.class);
        check(twice.invoke(left.runtimeClass().getConstructor().newInstance(), 13).equals(26),
                "runtime invocation preserved");
        check(right.runtimeClass().getMethod("codeLikeString")
                .invoke(right.runtimeClass().getConstructor().newInstance())
                .equals("class Fake { public void run() {} } [a-z]+"),
                "code-looking string remains data");
        check(space.imageCount() == 1, "pool must not retain duplicate image");
    }

    private static void verifyNativeParity() {
        Random random = new Random(0x5350414345L);
        byte[][] nullCases = {null, new byte[0], new byte[]{1}};
        for (byte[] left : nullCases) {
            for (byte[] right : nullCases) {
                check(M3ClassSpaceNative.equalBytes(left, right) == Arrays.equals(left, right),
                        "JNI null and zero-length semantics");
            }
        }
        int[] sizes = {0, 1, 2, 31, 32, 33, 4095, 4096, 4097, 8192, 8193, 32771};
        for (int pass = 0; pass < 20; pass++) {
            for (int size : sizes) {
                byte[] a = new byte[size];
                random.nextBytes(a);
                byte[] b = a.clone();
                check(M3ClassSpaceNative.equalBytes(a, b), "JNI equal content");
                if (size > 0) {
                    b[size - 1] ^= 1;
                    check(!M3ClassSpaceNative.equalBytes(a, b), "JNI last-byte difference");
                    b[size - 1] ^= 1;
                    b[0] ^= 1;
                    check(!M3ClassSpaceNative.equalBytes(a, b), "JNI first-byte difference");
                }
                check(M3ClassSpaceNative.equalBytes(a, a), "JNI same object");
            }
        }
    }

    private static byte[] inMemoryCompile(String className, String source) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "JDK JavaCompiler required");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager standard = compiler.getStandardFileManager(diagnostics, null, null);
                MemoryManager files = new MemoryManager(standard)) {
            JavaFileObject unit = new SimpleJavaFileObject(
                    URI.create("string:///" + className.replace('.', '/') + ".java"),
                    JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return source;
                }
            };
            boolean ok = Boolean.TRUE.equals(compiler.getTask(null, files, diagnostics,
                    List.of("--release", "21", "-proc:none", "-Xlint:all", "-Werror"),
                    null, List.of(unit)).call());
            check(ok, "in-memory javac failed: " + diagnostics.getDiagnostics());
            byte[] bytes = files.output.toByteArray();
            check(bytes.length > 4 && bytes[0] == (byte) 0xca && bytes[1] == (byte) 0xfe,
                    "compiler emitted a classfile image");
            return bytes;
        }
    }

    private static final class MemoryManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        MemoryManager(StandardJavaFileManager underlying) {
            super(underlying);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className,
                JavaFileObject.Kind kind, FileObject sibling) {
            return new SimpleJavaFileObject(
                    URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind) {
                @Override
                public OutputStream openOutputStream() {
                    return output;
                }
            };
        }
    }

    private static final class BytesLoader extends ClassLoader {
        private final String name;
        private final byte[] bytes;

        BytesLoader(String name, byte[] bytes) {
            super(null);
            this.name = name;
            this.bytes = bytes.clone();
        }

        Class<?> define() {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    private static void expect(Class<? extends Throwable> kind, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable ex) {
            check(kind.isInstance(ex), "expected " + kind.getName() + " got " + ex);
            return;
        }
        throw new AssertionError("expected " + kind.getName());
    }

    private static void check(boolean assertion, String message) {
        checks++;
        if (!assertion) {
            throw new AssertionError(message);
        }
    }
}
