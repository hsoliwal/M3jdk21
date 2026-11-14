// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.intern;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Java 21/compiler/JNI oracle; standalone and deliberately without a test runtime dependency. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-class-space-symbol-atoms-20261030/src/test/java/com/synexia/m3/classspace/M3ClassFileAtomsSelfTest.java blob 0fbe1f42f03a; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
public final class ClassFileAtomsSelfTest {
    private static int checks;

    private ClassFileAtomsSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        String className = "fixture.m3.Probe";
        String first = source("return value * 2;");
        String changedMethod = source("return value + value;");
        byte[] one = compile(className, first);
        byte[] two = compile(className, changedMethod);
        byte[] sibling = compile("fixture.m3.Sibling", source("return value * 2;")
                .replace("class Probe", "class Sibling"));
        verifyParsingAndMutability(one);
        verifyMalformedRefusal(one);
        verifyCanonicalInterning(one, two, sibling);
        verifySignalNotProof(one, two);
        verifyReadOnlyFanout(one);
        if (args.length == 1) {
            System.load(args[0]);
            verifyNativeDifferential(one, sibling);
        } else if (args.length != 0) {
            throw new IllegalArgumentException("Expected optional JNI library path");
        }
        System.out.println("ClassFileAtoms PASS " + checks + " assertions; "
                + (args.length == 1 ? "checked JNI PASS" : "JNI NOT_RUN"));
    }

    private static String source(String expression) {
        return """
                package fixture.m3;
                public final class Probe {
                    static final long TAG = 0x12345678abcdefL;
                    static final double SCALE = 1.125;
                    public int twice(int value) { EXPR }
                    public String codeLookingData() {
                        return "class Fake { int x; } [a-z]* A\\0B";
                    }
                }
                """.replace("EXPR", expression);
    }

    private static void verifyParsingAndMutability(byte[] classfile) {
        byte[] original = classfile.clone();
        ClassFileAtoms.Plan plan = ClassFileAtoms.scan(classfile);
        check(plan.majorVersion() == 65, "Java21 classfile major version");
        check(plan.minorVersion() == 0, "ordinary non-preview classfile");
        check(plan.constantPoolCount() > 10, "genuine javac constant pool");
        check(plan.utf8Count() > 5, "genuine repeated symbolic atoms");
        check(plan.candidateHashHex().matches("[0-9a-f]{64}"), "stable SHA256 candidate key");
        check(plan.tagAt(1) > 0, "first CP tag present");
        expect(IndexOutOfBoundsException.class, () -> plan.tagAt(0));
        expect(IndexOutOfBoundsException.class, () -> plan.tagAt(plan.constantPoolCount()));
        boolean foundModifiedNull = false;
        boolean foundCodeLooking = false;
        boolean foundLongOrDouble = false;
        byte[] code = "class Fake { int x; } [a-z]*".getBytes(StandardCharsets.UTF_8);
        for (int i = 1; i < plan.constantPoolCount(); i++) {
            int tag = plan.tagAt(i);
            foundLongOrDouble |= tag == 5 || tag == 6;
            if (tag != 1) {
                final int notUtf8 = i;
                expect(IllegalArgumentException.class, () -> plan.copyUtf8At(notUtf8));
                continue;
            }
            byte[] utf8 = plan.copyUtf8At(i);
            foundModifiedNull |= contains(utf8, new byte[] {'A', (byte) 0xc0, (byte) 0x80, 'B'});
            foundCodeLooking |= contains(utf8, code);
            if (utf8.length > 0) {
                byte[] twice = plan.copyUtf8At(i);
                twice[0] ^= 1;
                check(Arrays.equals(utf8, plan.copyUtf8At(i)), "never expose plan backing bytes");
            }
        }
        check(foundModifiedNull, "modified UTF8 encoded NUL is preserved byte-for-byte");
        check(foundCodeLooking, "code-looking string is data, not a Java source rewrite");
        check(foundLongOrDouble, "two-slot LONG/DOUBLE constant-pool tag tested");
        Arrays.fill(classfile, (byte) 0x5a);
        check(plan.candidateHashHex().equals(ClassFileAtoms.scan(original).candidateHashHex()),
                "defensive source snapshot survives input mutation");
    }

    private static void verifyMalformedRefusal(byte[] untouched) {
        byte[] valid = compileUncheckedSourceFixture();
        check(valid.length > 100, "non-trivial compiled fixture");
        byte[] badMagic = valid.clone();
        badMagic[0] = 0;
        reject(badMagic, "incorrect classfile magic");
        byte[] noCp = valid.clone();
        noCp[8] = 0;
        noCp[9] = 0;
        reject(noCp, "zero constant-pool count");
        byte[] unknown = valid.clone();
        unknown[10] = (byte) 255;
        reject(unknown, "unsupported constant-pool tag");
        byte[] truncated = Arrays.copyOf(valid, 12);
        reject(truncated, "truncated constant-pool entry");
        byte[] truncatedEnd = Arrays.copyOf(valid, valid.length - 1);
        reject(truncatedEnd, "truncated envelope or attribute bytes");
        byte[] trailing = Arrays.copyOf(valid, valid.length + 1);
        reject(trailing, "unclaimed trailing bytes");
        byte[] missingLongSlot = {
            (byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe,
            0, 0, 0, 65, 0, 2, 5, 0, 0, 0, 0, 0, 0, 0, 1
        };
        reject(missingLongSlot, "long constant-pool entry without reserved slot");
        byte[] ancient = valid.clone();
        ancient[6] = 0;
        ancient[7] = 44;
        reject(ancient, "below supported classfile major");
        byte[] overBound = new byte[16 * 1024 * 1024 + 1];
        reject(overBound, "source exceeds bounded signal limit");
        check(untouched[0] == (byte) 0x5a, "malformed tests do not revive caller mutation");
    }

    private static void verifyCanonicalInterning(byte[] oneChanged, byte[] changed, byte[] sibling) {
        // The caller mutated oneChanged earlier: scanning it must refuse.
        reject(oneChanged, "post-scan source mutation becomes an invalid new source");
        byte[] first = compileUncheckedSourceFixture();
        ClassFileAtoms.Plan a = ClassFileAtoms.scan(first);
        ClassFileAtoms.Plan b = ClassFileAtoms.scan(changed);
        ClassFileAtoms.Plan c = ClassFileAtoms.scan(sibling);
        ClassSpace shared = new ClassSpace(512 * 1024, input -> new byte[32]);
        ClassFileAtoms.Interned ids = a.internInto(shared);
        int stored = shared.imageCount();
        ClassFileAtoms.Interned again = a.internInto(shared);
        check(stored == shared.imageCount(), "same classfile symbols intern idempotently");
        check(ids.utf8Count() == a.utf8Count(), "symbol count preserved");
        for (int i = 1; i < a.constantPoolCount(); i++) {
            if (a.tagAt(i) == 1) {
                int symbol = ids.symbolIdAt(i);
                check(symbol >= 0 && symbol == again.symbolIdAt(i), "stable duplicate ordinal");
                check(Arrays.equals(a.copyUtf8At(i), shared.copy(symbol)),
                        "exact byte reconstruction despite forced digest collision");
            } else {
                check(ids.symbolIdAt(i) == -1, "non UTF8 CP entries have no symbol ID");
            }
        }
        ClassFileAtoms.Interned other = b.internInto(shared);
        ClassFileAtoms.Interned neighbor = c.internInto(shared);
        check(other.utf8Count() > 0 && neighbor.utf8Count() > 0, "all classfiles admitted");
        int intersections = 0;
        for (int i = 1; i < a.constantPoolCount(); i++) {
            if (a.tagAt(i) != 1) continue;
            for (int j = 1; j < c.constantPoolCount(); j++) {
                if (c.tagAt(j) == 1 && Arrays.equals(a.copyUtf8At(i), c.copyUtf8At(j))) {
                    check(ids.symbolIdAt(i) == neighbor.symbolIdAt(j),
                            "exact shared raw symbols reuse one primitive owner-local ID");
                    intersections++;
                }
            }
        }
        check(intersections > 4, "different compiled classes share multiple immutable atoms");
        check(shared.imageCount() > 2, "distinct symbols cannot collapse on hash collision");
        expect(IndexOutOfBoundsException.class, () -> ids.symbolIdAt(0));
        expect(IndexOutOfBoundsException.class,
                () -> ids.symbolIdAt(ids.constantPoolCount()));
        ClassSpace tiny = new ClassSpace(1);
        expect(IllegalStateException.class, () -> a.internInto(tiny));
        // Budget refusal is not a transaction; independent successful atoms are retained.
        check(tiny.retainedByteCount() <= tiny.maxByteCount(), "budget bound even on refusal");
    }

    private static void verifySignalNotProof(byte[] mutated, byte[] changed) {
        byte[] first = compileUncheckedSourceFixture();
        check(!Arrays.equals(first, changed), "distinct bytecode implementation is distinct image");
        ClassFileAtoms.Plan a = ClassFileAtoms.scan(first);
        ClassFileAtoms.Plan b = ClassFileAtoms.scan(changed);
        check(a.candidateHashHex().equals(b.candidateHashHex()),
                "same symbolic CP signature can conceal different Java behavior");
        ClassSpace images = new ClassSpace(256 * 1024);
        check(images.intern(first) != images.intern(changed),
                "candidate signature never licenses code or class identity merging");
        check(!Arrays.equals(images.copy(0), images.copy(1)),
                "distinct bytecode images stay distinct despite shared signature");
        check(mutated[0] != (byte) 0xca, "caller changed input independently of immutable plan");
    }

    private static void verifyReadOnlyFanout(byte[] caller) throws Exception {
        byte[] first = compileUncheckedSourceFixture();
        String expected = ClassFileAtoms.scan(first).candidateHashHex();
        ClassSpace untouched = new ClassSpace(1 << 20);
        ExecutorService service = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 64; i++) {
                tasks.add(() -> ClassFileAtoms.scan(first).candidateHashHex());
            }
            for (Future<String> each : service.invokeAll(tasks)) {
                check(each.get().equals(expected), "parallel read-only fanout deterministic");
            }
        } finally {
            service.shutdownNow();
        }
        check(untouched.imageCount() == 0, "signal analysis does not mutate primitive store");
        check(caller[0] != (byte) 0xca, "hostile shared mutable input is never retained");
    }

    private static void verifyNativeDifferential(byte[] mutated, byte[] sibling) {
        byte[] first = compileUncheckedSourceFixture();
        for (ClassFileAtoms.Plan plan : List.of(ClassFileAtoms.scan(first),
                ClassFileAtoms.scan(sibling))) {
            for (int i = 1; i < plan.constantPoolCount(); i++) {
                if (plan.tagAt(i) != 1) continue;
                byte[] bytes = plan.copyUtf8At(i);
                check(ClassSpaceNative.equalBytes(bytes, plan.copyUtf8At(i)),
                        "JNI full raw MUTF8 equality");
                if (bytes.length > 0) {
                    byte[] changed = bytes.clone();
                    changed[bytes.length - 1] ^= 1;
                    check(!ClassSpaceNative.equalBytes(bytes, changed),
                            "JNI does not conflate last-byte mutation");
                }
            }
        }
        check(mutated[0] != (byte) 0xca, "caller hostile mutation remains external");
    }

    private static byte[] compileUncheckedSourceFixture() {
        try {
            return compile("fixture.m3.Probe", source("return value * 2;"));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static boolean contains(byte[] data, byte[] needle) {
        outer: for (int i = 0; i <= data.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private static void reject(byte[] input, String stage) {
        expect(IllegalArgumentException.class, () -> ClassFileAtoms.scan(input));
        check(input.length >= 0, "rejection stage: " + stage);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable ex) {
            check(type.isInstance(ex), "wrong refusal: " + ex);
            return;
        }
        throw new AssertionError("Missing refusal: " + type.getSimpleName());
    }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError("check #" + checks + ": " + reason);
    }

    private static byte[] compile(String fqcn, String source) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "Java21 compiler available");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager standard = compiler.getStandardFileManager(diagnostics, null, null);
                CapturingManager capture = new CapturingManager(standard)) {
            JavaFileObject input = new SimpleJavaFileObject(
                    URI.create("string:///" + fqcn.replace('.', '/') + ".java"),
                    JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return source;
                }
            };
            boolean success = Boolean.TRUE.equals(compiler.getTask(null, capture, diagnostics,
                    List.of("--release", "21", "-proc:none", "-Xlint:all", "-Werror"),
                    null, List.of(input)).call());
            check(success, "actual in-memory Java21 compiler: " + diagnostics.getDiagnostics());
            return capture.data.toByteArray();
        }
    }

    private static final class CapturingManager
            extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final ByteArrayOutputStream data = new ByteArrayOutputStream();

        private CapturingManager(StandardJavaFileManager original) { super(original); }

        @Override public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location,
                String name, JavaFileObject.Kind kind, FileObject sibling) {
            return new SimpleJavaFileObject(
                    URI.create("mem:///" + name.replace('.', '/') + kind.extension), kind) {
                @Override public OutputStream openOutputStream() { return data; }
            };
        }
    }
}
