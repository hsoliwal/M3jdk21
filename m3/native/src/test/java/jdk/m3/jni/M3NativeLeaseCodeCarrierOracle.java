// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Deterministic in-memory Java21 proof that code-looking text/regex remains data. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/test/java/com/synexia/primitives/NativeLeaseCodeCarrierOracle.java blob f0811c10d5f1; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
public final class M3NativeLeaseCodeCarrierOracle {
    private M3NativeLeaseCodeCarrierOracle() { }

    // The fixture carries a literal nested Java text block, so construct it without
    // embedding unescaped triple-quote delimiters in this outer Java source.
    private static final String DATA =
            "package proof;\n"
            + "import java.util.regex.Pattern;\n"
            + "public final class TextPayload {\n"
            + "  // private static native int collectNative(long[] a); // decoy only\n"
            + "  public static final String S = \"JNIEXPORT jint JNICALL (*env)->GetArrayLength\";\n"
            + "  public static final String BLOCK = \"\"\"\n"
            + "    // if (count > ids.length) { throw new Error(); }\n"
            + "    (?:[A-Z]{2,}|[0-9]{3,})\n"
            + "    \"\"\";\n"
            + "  public static boolean matches(String s) {\n"
            + "    return Pattern.compile(\"(?:[A-Z]{2,}|[0-9]{3,})\").matcher(s).matches();\n"
            + "  }\n"
            + "}\n";
    private static final String BRIDGE = """
            package proof;
            import jdk.m3.jni.M3NativeLeaseScan;
            public final class NativeBridge {
                public static int execute() {
                    long[] ids = {1L,2L,3L,4L};
                    int[] leases = {0,1,0,0};
                    int[] pins = {0,0,1,0};
                    byte[] states = {0,0,0,2};
                    long[] output = {-1,-1,-1,-1};
                    if (!TextPayload.matches("JNI") || !TextPayload.matches("1234")
                            || TextPayload.matches("bad") || !TextPayload.BLOCK.contains("if (count")) {
                        throw new AssertionError("text carriers lost");
                    }
                    int copied = M3NativeLeaseScan.collect(ids, leases, pins, states, 4, output, true);
                    if (copied != 1 || output[0] != 1L || output[1] != -1L) {
                        throw new AssertionError("native result changed");
                    }
                    return copied;
                }
            }
            """;
    private static final String ENTRY = """
            package proof;
            public final class Entry {
                public static int call() { return NativeBridge.execute(); }
            }
            """;

    private static class TextUnit extends SimpleJavaFileObject {
        private final String text;
        TextUnit(String fqcn, String text) {
            super(URI.create("string:///" + fqcn.replace('.','/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.text = text;
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return text; }
    }
    private static class ByteUnit extends SimpleJavaFileObject {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ByteUnit(String fqcn) {
            super(URI.create("mem:///" + fqcn.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
        }
        @Override public OutputStream openOutputStream() { return bytes; }
    }
    private static class Manager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        final Map<String, ByteUnit> classes = new HashMap<>();
        Manager(StandardJavaFileManager delegate) { super(delegate); }
        @Override public JavaFileObject getJavaFileForOutput(Location location, String name,
                JavaFileObject.Kind kind, FileObject sibling) {
            ByteUnit result = new ByteUnit(name);
            classes.put(name, result);
            return result;
        }
    }
    private static class Loader extends ClassLoader {
        final Map<String, ByteUnit> classes;
        Loader(Map<String, ByteUnit> classes) {
            super(M3NativeLeaseCodeCarrierOracle.class.getClassLoader());
            this.classes = classes;
        }
        @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
            ByteUnit entry = classes.get(name);
            if (entry == null) throw new ClassNotFoundException(name);
            byte[] data = entry.bytes.toByteArray();
            return defineClass(name, data, 0, data.length);
        }
    }
    public static void main(String[] args) throws Exception {
        if (!M3NativeLeaseScan.nativeAvailable()) throw new AssertionError("native unavailable");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new AssertionError("full JDK21 javac required");
        List<TextUnit> originals = List.of(
                new TextUnit("proof.TextPayload", DATA),
                new TextUnit("proof.NativeBridge", BRIDGE),
                new TextUnit("proof.Entry", ENTRY));
        String first = null;
        for (int i = 0; i < 6; i++) {
            List<TextUnit> inputs = new ArrayList<>(originals);
            if ((i & 1) != 0) Collections.reverse(inputs);
            if (i > 1) Collections.rotate(inputs, i % inputs.size());
            var diagnostics = new DiagnosticCollector<JavaFileObject>();
            try (var standard = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);
                 var manager = new Manager(standard)) {
                var flags = List.of("--release", "21", "-Xlint:all", "-Werror",
                        "-classpath", System.getProperty("java.class.path"));
                boolean ok = Boolean.TRUE.equals(compiler.getTask(null, manager,
                        diagnostics, flags, null, inputs).call());
                if (!ok) throw new AssertionError("permutation " + i + " " + diagnostics.getDiagnostics());
                var entry = new Loader(manager.classes).loadClass("proof.Entry");
                Object value = entry.getMethod("call").invoke(null);
                if (!Integer.valueOf(1).equals(value)) throw new AssertionError("runtime " + value);
                var hasher = MessageDigest.getInstance("SHA-256");
                var classNames = new ArrayList<>(manager.classes.keySet());
                Collections.sort(classNames);
                for (String name : classNames) {
                    hasher.update(name.getBytes(StandardCharsets.UTF_8));
                    hasher.update(manager.classes.get(name).bytes.toByteArray());
                }
                String sha = HexFormat.of().formatHex(hasher.digest());
                if (first == null) first = sha;
                else if (!first.equals(sha)) throw new AssertionError("permutation drift at " + i);
            }
        }
        System.out.println("M3_LEASE_CODE_CARRIER_PASS permutations=6 sha256=" + first);
    }
}
