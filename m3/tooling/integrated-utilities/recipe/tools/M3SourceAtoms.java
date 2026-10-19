// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Read-only method extent seals from the JDK compiler parser; no source rewriting. */
public final class M3SourceAtoms {
    private M3SourceAtoms() {}

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32) out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String textSha(String text) {
        try {
            return sha(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: M3SourceAtoms ROOT RELATIVE_JAVA_PATH [RELATIVE_JAVA_PATH ...]");
        }
        Path root = Path.of(args[0]).toRealPath();
        List<Path> files = new ArrayList<>();
        Set<Path> unique = new HashSet<>();
        for (int i = 1; i < args.length; i++) {
            Path argument = Path.of(args[i]);
            if (argument.isAbsolute() || !args[i].endsWith(".java")) {
                throw new IllegalArgumentException("Expected a relative Java source path: " + args[i]);
            }
            Path file = root.resolve(argument).toRealPath();
            if (!file.startsWith(root) || !Files.isRegularFile(file) || !unique.add(file)) {
                throw new IllegalArgumentException("Invalid or duplicate source path: " + args[i]);
            }
            files.add(file);
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Run with a full JDK 21 toolchain");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none", "--source", "21"), null, manager.getJavaFileObjectsFromPaths(files));
            List<CompilationUnitTree> units = new ArrayList<>();
            task.parse().forEach(units::add);
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    throw new IllegalArgumentException("Compiler parser refused source: " + diagnostic);
                }
            }
            SourcePositions positions = Trees.instance(task).getSourcePositions();
            List<String> atoms = new ArrayList<>();
            List<String> fileRecords = new ArrayList<>();
            Set<String> identities = new HashSet<>();
            for (CompilationUnitTree unit : units) {
                Path path = Path.of(unit.getSourceFile().toUri()).toRealPath();
                String relative = root.relativize(path).toString().replace('\\', '/');
                String source = Files.readString(path, StandardCharsets.UTF_8);
                fileRecords.add("{\"path\":" + quote(relative) + ",\"sha256\":" + quote(sha(Files.readAllBytes(path))) + "}");
                String packageName = unit.getPackageName() == null ? "" : unit.getPackageName() + ".";
                new TreePathScanner<Void, String>() {
                    @Override public Void visitClass(ClassTree node, String outer) {
                        String simple = node.getSimpleName().toString();
                        if (simple.isEmpty()) simple = "anonymous@" + positions.getStartPosition(unit, node);
                        String owner = outer == null ? simple : outer + "." + simple;
                        return super.visitClass(node, owner);
                    }

                    @Override public Void visitMethod(MethodTree node, String owner) {
                        long startLong = positions.getStartPosition(unit, node);
                        long endLong = positions.getEndPosition(unit, node);
                        if (startLong < 0 || endLong <= startLong || endLong > source.length()) {
                            throw new IllegalArgumentException("Missing compiler extent for " + relative + ": " + owner + "." + node.getName());
                        }
                        int start = Math.toIntExact(startLong);
                        int end = Math.toIntExact(endLong);
                        String parameters = String.join(",", node.getParameters().stream().map(p -> p.getType().toString()).toList());
                        String signature = packageName + owner + "#" + node.getName() + "(" + parameters + ")";
                        String identity = relative + "#" + signature + "@" + start;
                        if (!identities.add(identity)) throw new IllegalArgumentException("Duplicate compiler method extent: " + identity);
                        String methodName = node.getName().contentEquals("<init>") ? "construct" : node.getName().toString();
                        String compactName = owner + "." + methodName + "." + textSha(signature).substring(0, 10);
                        atoms.add("{\"name\":" + quote(compactName) + ",\"kind\":\"METHOD\",\"path\":" + quote(relative)
                                + ",\"signature\":" + quote(signature) + ",\"start_utf16\":" + start + ",\"end_utf16_exclusive\":" + end
                                + ",\"start_line\":" + unit.getLineMap().getLineNumber(start) + ",\"end_line\":" + unit.getLineMap().getLineNumber(end - 1)
                                + ",\"sha256\":" + quote(textSha(source.substring(start, end))) + "}");
                        return super.visitMethod(node, owner);
                    }
                }.scan(unit, null);
            }
            System.out.println("{\"schema\":\"M3_SOURCE_BEHAVIORAL_ATOMS_V1\",\"parser\":\"JDK21_JAVAC_COMPILER_TREE_API\","
                    + "\"offset_unit\":\"UTF16_CODE_UNITS\",\"hash_encoding\":\"UTF8\",\"files\":[" + String.join(",", fileRecords)
                    + "],\"atoms\":[" + String.join(",", atoms) + "]}");
        }
    }
}
