/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.tools.ToolProvider;

/** Independent checked javac -h, C candidate and refusal fixture for the exact join tool. */
public final class JniExportJoinSelfTest {
    private static final String SOURCE = """
            package demo.pkg;
            public class Outer {
                public static native int count(int[] values);
                public static native int ping();
                public native int overloaded(int n);
                public native int overloaded(byte[] n);
                public static class Inner { public native int do_Thing(int x); }
                void local() { class Local { native int hidden(int n); } }
            }
            """;
    private static final String C = """
            #include <jni.h>
            JNIEXPORT jint JNICALL Java_demo_pkg_Outer_count(JNIEnv *env, jclass type, jintArray a) {
                (void)type; return (*env)->GetArrayLength(env, a);
            }
            JNIEXPORT jint JNICALL Java_demo_pkg_Outer_ping(JNIEnv *env, jclass type) {
                (void)env; (void)type; return 7;
            }
            JNIEXPORT jint JNICALL Java_demo_pkg_Outer_overloaded__I(JNIEnv *env, jobject self, jint x) {
                (void)env; (void)self; return x + 2;
            }
            JNIEXPORT jint JNICALL Java_demo_pkg_Outer_overloaded___3B(JNIEnv *env, jobject self, jbyteArray x) {
                (void)self; return (*env)->GetArrayLength(env, x);
            }
            JNIEXPORT jint JNICALL Java_demo_pkg_Outer_00024Inner_do_1Thing(JNIEnv *env, jobject self, jint x) {
                (void)env; (void)self; return x + 3;
            }
            JNIEXPORT jint JNICALL Java_demo_pkg_Outer_orphan(JNIEnv *env, jclass type) {
                (void)env; (void)type; return 8;
            }
            JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
                (void)vm; (void)reserved; return JNI_VERSION_1_6;
            }
            """;
    private int checks;

    private JniExportJoinSelfTest() {
    }

    private void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    private static String digest(byte[] input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private void rejects(Class<? extends Throwable> exception, Checked action, String name) throws Exception {
        try {
            action.run();
        } catch (Throwable actual) {
            if (exception.isInstance(actual)) { checks++; return; }
            throw new AssertionError(name + ": unexpected failure " + actual, actual);
        }
        throw new AssertionError(name + ": expected refusal");
    }

    @FunctionalInterface
    private interface Checked { void run() throws Exception; }

    private static String csv(Path source, String file) throws IOException {
        String sha = digest(Files.readAllBytes(source));
        StringBuilder rows = new StringBuilder("source,sourceSHA256,line,kind,symbol,status\n");
        Map<String, String> sorted = new TreeMap<>();
        String[] lines = file.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!line.startsWith("JNIEXPORT")) continue;
            int at = line.indexOf("JNICALL ");
            if (at < 0) continue;
            String symbol = line.substring(at + "JNICALL ".length()).split("\\(", 2)[0];
            String kind = symbol.startsWith("Java_") ? "JNI_ENTRY" : "JNI_LIFECYCLE";
            sorted.put(symbol, "\"native.c\"," + sha + ',' + (i + 1) + ',' + kind + ",\""
                    + symbol + "\",UNVERIFIED_NATIVE_JNI_CONTRACT");
        }
        for (String row : sorted.values()) rows.append(row).append('\n');
        return rows.toString();
    }

    private static void compileJava(Path source, Path classes, Path headers) throws IOException {
        Files.createDirectories(classes);
        Files.createDirectories(headers);
        int code = ToolProvider.getSystemJavaCompiler().run(null, null, null, "-encoding", "UTF-8",
                "-d", classes.toString(), "-h", headers.toString(), source.toString());
        if (code != 0) throw new IOException("independent javac -h failed");
    }

    private static void census(Path rules, Path javaRoot, Path output) throws IOException {
        AtomCensus.main(new String[]{"--jni", "demo", rules.toString(),
                output.toString(), javaRoot.toString()});
    }

    private static List<String> report(Path javaTsv, Path nativeCsv, Path javaRoot,
            Path nativeRoot, Path headerRoot) throws IOException {
        return JniExportJoin.join(javaTsv, nativeCsv, javaRoot,
                nativeRoot, headerRoot);
    }

    private void run() throws Exception {
        Path root = Files.createTempDirectory("m3-jni-export-join-");
        try {
            Path javaRoot = Files.createDirectories(root.resolve("java"));
            Path source = javaRoot.resolve("demo/pkg/Outer.java");
            Files.createDirectories(source.getParent());
            Files.writeString(source, SOURCE);
            Path nativeRoot = Files.createDirectories(root.resolve("native"));
            Path nativeSource = nativeRoot.resolve("native.c");
            Files.writeString(nativeSource, C);
            Path csv = root.resolve("native.csv");
            Files.writeString(csv, csv(nativeSource, C));
            Path rules = root.resolve("rules.tsv");
            Files.writeString(rules, "pattern\tname\tbody\tcatalogue\n");
            Path headers = root.resolve("headers");
            compileJava(source, root.resolve("classes"), headers);
            Path sourceOutput = root.resolve("census");
            census(rules, javaRoot, sourceOutput);
            Path javaTsv = sourceOutput.resolve("JNI_DECLARATIONS_demo.tsv");
            List<String> joined = report(javaTsv, csv, javaRoot, nativeRoot, headers);
            check(joined.getFirst().equals(JniExportJoin.HEADER), "exact closed join schema");
            check(joined.size() == 9, "six Java + one orphan + one lifecycle + header");
            check(joined.stream().filter(s -> s.endsWith("\tEXACT_EXPORT_CANDIDATE")).count() == 5,
                    "all five source-and-javac-bound exports match, including long overload names");
            check(joined.stream().filter(s -> s.endsWith("\tJAVA_DECLARATION_UNRESOLVED")).count() == 1,
                    "local native identity remains unresolved");
            check(joined.stream().filter(s -> s.endsWith("\tORPHAN_NATIVE_EXPORT")).count() == 1,
                    "orphan native source is visible");
            check(joined.stream().filter(s -> s.endsWith("\tJNI_LIFECYCLE_UNSCOPED")).count() == 1,
                    "JNI_OnLoad remains unscoped");
            check(joined.stream().anyMatch(s -> s.contains("Java_demo_pkg_Outer_overloaded__I")
                    && s.endsWith("\tEXACT_EXPORT_CANDIDATE")), "int overload long symbol");
            check(joined.stream().anyMatch(s -> s.contains("Java_demo_pkg_Outer_overloaded___3B")
                    && s.endsWith("\tEXACT_EXPORT_CANDIDATE")), "byte array overload long symbol");
            check(joined.stream().anyMatch(s -> s.contains("Java_demo_pkg_Outer_00024Inner_do_1Thing")
                    && s.endsWith("\tEXACT_EXPORT_CANDIDATE")), "inner class JNI escaped dollar");
            check(joined.stream().allMatch(s -> s.split("\t", -1).length == 13),
                    "every output row has canonical fixed width");
            check(JniExportJoin.descriptor("(int[],byte)void").equals("([IB)V"),
                    "primitive and array descriptor");
            check(JniExportJoin.descriptor("(int[][])java.lang.String")
                    .equals("([[I)Ljava/lang/String;"), "fully qualified reference descriptor");
            check(JniExportJoin.descriptor("(String)int") == null,
                    "unqualified possibly shadowed types not guessed");
            check(JniExportJoin.descriptor("(java.util.List<String>)void") == null,
                    "generic source signature requires attribution");
            Path output = root.resolve("JOIN.tsv");
            JniExportJoin.main(new String[]{javaTsv.toString(), csv.toString(), javaRoot.toString(),
                    nativeRoot.toString(), headers.toString(), output.toString()});
            byte[] first = Files.readAllBytes(output);
            JniExportJoin.main(new String[]{javaTsv.toString(), csv.toString(), javaRoot.toString(),
                    nativeRoot.toString(), headers.toString(), output.toString()});
            check(Arrays.equals(first, Files.readAllBytes(output)), "exact repeated output fixed point");
            rejects(IllegalStateException.class, () -> JniExportJoin.main(new String[]{
                    javaTsv.toString(), csv.toString(), javaRoot.toString(), nativeRoot.toString(),
                    headers.toString(), output.toString(), "--require-candidate-match"}),
                    "strict candidate matching refuses unresolved and orphan");
            check(Arrays.equals(first, Files.readAllBytes(output)), "failed strict mode never clobbers output");
            String originalCsv = Files.readString(csv);
            Files.writeString(csv, originalCsv.replaceFirst("[0-9a-f]{64}", "0".repeat(64)));
            rejects(IOException.class, () -> report(javaTsv,csv,javaRoot,nativeRoot,headers),
                    "native whole-source hash forged");
            Files.writeString(csv, originalCsv);
            String originalTsv = Files.readString(javaTsv);
            String[] changedLines = originalTsv.split("\n", -1);
            String[] tampered = changedLines[1].split("\t", -1);
            tampered[8] = "0".repeat(64);
            changedLines[1] = String.join("\t", tampered);
            Files.writeString(javaTsv, String.join("\n", changedLines));
            rejects(IOException.class, () -> report(javaTsv,csv,javaRoot,nativeRoot,headers),
                    "Java native atom identity/digest forged");
            Files.writeString(javaTsv, originalTsv);
            // One symbol cannot be declared twice and still count as one-to-one.
            String[] duplicateLines = originalTsv.split("\n", -1);
            String firstRow = duplicateLines[1];
            String[] duplicateAtom = firstRow.split("\t", -1);
            duplicateAtom[0] = "A" + "f".repeat(64);
            String[] repeatedRows = new String[duplicateLines.length + 1];
            System.arraycopy(duplicateLines, 0, repeatedRows, 0, 2);
            repeatedRows[2] = String.join("\t", duplicateAtom);
            System.arraycopy(duplicateLines, 2, repeatedRows, 3, duplicateLines.length - 2);
            Files.writeString(javaTsv, String.join("\n", repeatedRows));
            check(report(javaTsv,csv,javaRoot,nativeRoot,headers).stream()
                    .filter(x -> x.endsWith("\tAMBIGUOUS_JAVA_SYMBOL")).count() == 2,
                    "forged duplicate Java symbol cannot be promoted as a bijective match");
            Files.writeString(javaTsv, originalTsv);
            String brokenCsv = originalCsv.replaceFirst("\"native.c\"", "\"native.c\"\"\"");
            Files.writeString(csv, brokenCsv);
            rejects(IOException.class, () -> report(javaTsv,csv,javaRoot,nativeRoot,headers),
                    "invalid quoted native path is rejected or missing source refused");
            Files.writeString(csv, originalCsv);
            // Forcing a valid-looking header method descriptor change must not promote the symbol.
            Path mainHeader = headers.resolve("demo_pkg_Outer.h");
            String oldHeader = Files.readString(mainHeader);
            Files.writeString(mainHeader, oldHeader.replace("Signature: ([I)I", "Signature: ([J)I"));
            check(report(javaTsv,csv,javaRoot,nativeRoot,headers).stream()
                    .anyMatch(s -> s.contains("Java_demo_pkg_Outer_count")
                            && s.endsWith("\tHEADER_UNCONFIRMED")),
                    "forged javac header signature is not silently admitted");
            Files.writeString(mainHeader, oldHeader);
            // Duplicate candidate exports across two native files must never count as unique ABI.
            String duplicate = "JNIEXPORT jint JNICALL Java_demo_pkg_Outer_ping(JNIEnv* e, jclass c) {"
                    + " (void)e; (void)c; return 1; }\n";
            Path extra = nativeRoot.resolve("other.c");
            Files.writeString(extra, duplicate);
            String secondCsv = originalCsv + "\"other.c\"," + digest(Files.readAllBytes(extra))
                    + ",1,JNI_ENTRY,\"Java_demo_pkg_Outer_ping\",UNVERIFIED_NATIVE_JNI_CONTRACT\n";
            // Canonical native scanner sorts source paths before symbols.
            Files.writeString(csv, secondCsv);
            check(report(javaTsv,csv,javaRoot,nativeRoot,headers).stream()
                    .anyMatch(s -> s.contains("Java_demo_pkg_Outer_ping")
                            && s.endsWith("\tDUPLICATE_NATIVE_EXPORT")),
                    "duplicate source export blocks unique matching");
            Files.delete(extra);
            Files.writeString(csv, originalCsv);
            Path linked = root.resolve("source-link.java");
            Files.createSymbolicLink(linked, source);
            Path fakeJavaRoot = Files.createDirectory(root.resolve("symlink-root"));
            Files.createDirectories(fakeJavaRoot.resolve("demo/pkg"));
            Files.createSymbolicLink(fakeJavaRoot.resolve("demo/pkg/Outer.java"), source);
            rejects(IOException.class, () -> report(javaTsv,csv,fakeJavaRoot,nativeRoot,headers),
                    "symbolic Java input refused");
            rejects(IOException.class, () -> JniExportJoin.main(new String[]{javaTsv.toString(),
                    csv.toString(), javaRoot.toString(), nativeRoot.toString(), headers.toString(),
                    javaTsv.toString()}), "input-output collision refused");
            check(originalTsv.equals(Files.readString(javaTsv)), "input not clobbered");
            // Scoped five-declaration inventory should pass a strict *candidate* join.
            Files.writeString(source, SOURCE.replace(
                    "    void local() { class Local { native int hidden(int n); } }\n", ""));
            Path cleanHeaders = root.resolve("clean-headers");
            compileJava(source, root.resolve("clean-classes"), cleanHeaders);
            Path cleanOutput = root.resolve("clean-census");
            census(rules, javaRoot, cleanOutput);
            Path cleanTsv = cleanOutput.resolve("JNI_DECLARATIONS_demo.tsv");
            Path cleanNative = nativeRoot.resolve("native.c");
            String cleanC = C.replace("JNIEXPORT jint JNICALL Java_demo_pkg_Outer_orphan(JNIEnv *env, jclass type) {\n"
                    + "    (void)env; (void)type; return 8;\n}\n", "");
            Files.writeString(cleanNative, cleanC);
            Files.writeString(csv, csv(cleanNative, cleanC));
            Path cleanJoin = root.resolve("clean-join.tsv");
            JniExportJoin.main(new String[]{cleanTsv.toString(), csv.toString(), javaRoot.toString(),
                    nativeRoot.toString(), cleanHeaders.toString(), cleanJoin.toString(),
                    "--require-candidate-match"});
            List<String> cleanRows = Files.readAllLines(cleanJoin);
            check(cleanRows.size() == 7, "five matches plus lifecycle plus header");
            check(cleanRows.stream().filter(x -> x.endsWith("\tEXACT_EXPORT_CANDIDATE")).count() == 5,
                    "strict mode proves all five scoped source/header/export candidates only");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path file : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
    }

    /** Executes the bounded fixture and returns the number of assertions actually checked. */
    public static int runAll() throws Exception {
        JniExportJoinSelfTest test = new JniExportJoinSelfTest();
        test.run();
        return test.checks;
    }

    /** Dependency-free proof entrypoint for the canonical recipe and receiver. */
    public static void main(String[] args) throws Exception {
        System.out.println("M3_JNI_EXPORT_JOIN_PROOF\tchecks=" + runAll() + "\tstatus=PASS");
    }
}
