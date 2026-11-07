/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.repository;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import jdk.m3.tooling.atoms.contract.M3AtomKind;

/**
 * Extracts explicit Java semantic atoms using javac's parse-only AST, not source-text matching.
 *
 * <p>This is an additive semantic leaf of the existing repository file census. It never resolves a JNI symbol,
 * attributes a type, transforms source, or claims contract equivalence. Malformed/undecodable source returns a
 * conspicuous parse error and no supposedly complete atom set.</p>
 */
public final class M3JavaSemanticExtractor {
    private M3JavaSemanticExtractor() {
    }

    /**
     * Parses one exact committed UTF-8 Java blob with the JDK 21 parser.
     *
     * @param sourcePath canonical repository-relative Java path
     * @param exactBytes exact committed Git-blob bytes
     * @return atom rows and an explicit parse state
     */
    public static ParseResult extract(String sourcePath, byte[] exactBytes) {
        Objects.requireNonNull(sourcePath, "sourcePath");
        Objects.requireNonNull(exactBytes, "exactBytes");
        if (!sourcePath.toLowerCase(Locale.ROOT).endsWith(".java") || sourcePath.startsWith("/") || sourcePath.contains("\\")
                || sourcePath.contains("../") || sourcePath.contains("/./")) {
            throw new IllegalArgumentException("invalid Java source path: " + sourcePath);
        }
        final String source;
        try {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(exactBytes));
            source = decoded.toString();
        } catch (CharacterCodingException exception) {
            return new ParseResult(List.of(), "INVALID_UTF8");
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new ParseResult(List.of(), "JDK_COMPILER_UNAVAILABLE");
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics,
                Locale.ROOT, StandardCharsets.UTF_8)) {
            JavaFileObject input = new InMemoryJavaFile(sourcePath, source);
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none", "--release", "21"), null, List.of(input));
            SourcePositions positions = Trees.instance(task).getSourcePositions();
            List<Atom> atoms = new ArrayList<>();
            for (CompilationUnitTree unit : task.parse()) {
                new AtomScanner(sourcePath, source, unit, positions, atoms).scan(unit, null);
            }
            List<String> errors = diagnostics.getDiagnostics().stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                    .map(d -> d.getCode() + "@" + d.getLineNumber() + ":" + d.getColumnNumber())
                    .sorted().toList();
            if (!errors.isEmpty()) {
                return new ParseResult(List.of(), "JAVAC_PARSE_ERROR:" + String.join(",", errors));
            }
            return new ParseResult(List.copyOf(atoms), "PASS");
        } catch (IOException exception) {
            return new ParseResult(List.of(), "JAVA_IO_ERROR:" + safe(exception.getClass().getSimpleName()));
        } catch (RuntimeException exception) {
            return new ParseResult(List.of(), "JAVAC_AST_UNSUPPORTED:" + safe(exception.getClass().getSimpleName()));
        }
    }

    /**
     * One exact explicit source atom, with source offsets measured in UTF-16 code units.
     *
     * @param atomId stable identifier within this lexical ownership model
     * @param sourcePath owning committed file path
     * @param kind closed atom kind
     * @param owner lexical owner
     * @param signature explicit AST-derived semantic signature
     * @param sourceSha256 SHA-256 of exact UTF-8 slice for the AST position
     * @param startOffset first source UTF-16 offset
     * @param endOffset exclusive final source UTF-16 offset
     */
    public record Atom(String atomId, String sourcePath, M3AtomKind kind, String owner,
            String signature, String sourceSha256, long startOffset, long endOffset) {
    }

    /**
     * Atom sequence and complete parse status for one source file.
     *
     * @param atoms explicit atoms, or empty on a rejected parse
     * @param status {@code PASS} or an explicit non-pass diagnostic
     */
    public record ParseResult(List<Atom> atoms, String status) {
        /**
         * Returns whether the source parsed with no compiler errors.
         *
         * @return parse completion
         */
        public boolean complete() {
            return "PASS".equals(status);
        }
    }

    private static final class InMemoryJavaFile extends SimpleJavaFileObject {
        private final String text;

        InMemoryJavaFile(String sourcePath, String text) {
            super(URI.create("string:///" + uriPath(sourcePath)), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }

        private static String uriPath(String path) {
            StringBuilder out = new StringBuilder();
            for (byte b : path.getBytes(StandardCharsets.UTF_8)) {
                int c = b & 0xff;
                if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                        || (c >= '0' && c <= '9') || c == '/' || c == '_' || c == '-' || c == '.') {
                    out.append((char) c);
                } else {
                    out.append('%').append(Character.forDigit(c >>> 4, 16))
                            .append(Character.forDigit(c & 15, 16));
                }
            }
            return out.toString();
        }
    }

    private static final class AtomScanner extends TreePathScanner<Void, Void> {
        private final String sourcePath;
        private final String text;
        private final CompilationUnitTree unit;
        private final SourcePositions positions;
        private final List<Atom> atoms;
        private final Deque<String> owners = new ArrayDeque<>();
        private final Map<String, Integer> occurrences = new HashMap<>();
        private int anonymousOrdinal;

        AtomScanner(String sourcePath, String text, CompilationUnitTree unit, SourcePositions positions,
                List<Atom> atoms) {
            this.sourcePath = sourcePath;
            this.text = text;
            this.unit = unit;
            this.positions = positions;
            this.atoms = atoms;
            if (unit.getPackageName() != null) {
                owners.addLast("package:" + safe(unit.getPackageName().toString()));
            }
        }

        @Override
        public Void visitClass(ClassTree tree, Void unused) {
            String simpleName = tree.getSimpleName().toString();
            if (simpleName.isEmpty()) {
                simpleName = "anonymous#" + (++anonymousOrdinal);
            }
            String signature = "type:" + safe(tree.getKind().name()) + ":" + safe(simpleName);
            emit(tree, M3AtomKind.DECLARATION_ONLY, signature);
            owners.addLast(signature);
            try {
                return super.visitClass(tree, unused);
            } finally {
                owners.removeLast();
            }
        }

        @Override
        public Void visitMethod(MethodTree tree, Void unused) {
            boolean nativeMethod = tree.getModifiers().getFlags().contains(Modifier.NATIVE);
            boolean constructor = tree.getReturnType() == null;
            M3AtomKind kind = nativeMethod ? M3AtomKind.JAVA_NATIVE_DECLARATION
                    : constructor && tree.getBody() != null ? M3AtomKind.JAVA_CONSTRUCTOR
                    : tree.getBody() != null ? M3AtomKind.JAVA_METHOD : M3AtomKind.DECLARATION_ONLY;
            String parameters = tree.getParameters().stream().map(p -> safe(p.getType().toString()))
                    .reduce((left, right) -> left + "," + right).orElse("");
            String method = constructor ? "<init>" : safe(tree.getName().toString());
            String signature = "method:" + method + "(" + parameters + "):"
                    + (constructor ? "constructor" : safe(tree.getReturnType().toString()));
            emit(tree, kind, signature);
            owners.addLast(signature);
            try {
                return super.visitMethod(tree, unused);
            } finally {
                owners.removeLast();
            }
        }

        @Override
        public Void visitVariable(VariableTree tree, Void unused) {
            TreePath parent = getCurrentPath().getParentPath();
            if (parent != null && parent.getLeaf() instanceof ClassTree) {
                M3AtomKind kind = tree.getInitializer() == null ? M3AtomKind.DECLARATION_ONLY
                        : M3AtomKind.JAVA_FIELD_INITIALIZER;
                emit(tree, kind, "field:" + safe(tree.getName().toString()) + ":"
                        + (tree.getType() == null ? "inferred" : safe(tree.getType().toString())));
            }
            return super.visitVariable(tree, unused);
        }

        @Override
        public Void visitBlock(BlockTree tree, Void unused) {
            TreePath parent = getCurrentPath().getParentPath();
            if (parent != null && parent.getLeaf() instanceof ClassTree) {
                emit(tree, tree.isStatic() ? M3AtomKind.JAVA_STATIC_INITIALIZER
                        : M3AtomKind.JAVA_INSTANCE_INITIALIZER, "initializer:"
                        + (tree.isStatic() ? "static" : "instance"));
            }
            return super.visitBlock(tree, unused);
        }

        @Override
        public Void visitLambdaExpression(LambdaExpressionTree tree, Void unused) {
            String signature = "lambda:" + safe(tree.getParameters().stream()
                    .map(p -> p.getType() == null ? "inferred" : p.getType().toString())
                    .reduce((left, right) -> left + "," + right).orElse(""));
            emit(tree, M3AtomKind.JAVA_LAMBDA, signature);
            owners.addLast(signature);
            try {
                return super.visitLambdaExpression(tree, unused);
            } finally {
                owners.removeLast();
            }
        }

        @Override
        public Void visitModule(ModuleTree tree, Void unused) {
            emit(tree, M3AtomKind.DECLARATION_ONLY,
                    "module:" + safe(tree.getName().toString()));
            for (Tree directive : tree.getDirectives()) {
                emit(directive, M3AtomKind.DECLARATION_ONLY,
                        "module-directive:" + safe(directive.getKind().name()));
            }
            return super.visitModule(tree, unused);
        }

        private void emit(Tree tree, M3AtomKind kind, String signature) {
            long start = positions.getStartPosition(unit, tree);
            long end = positions.getEndPosition(unit, tree);
            if (start < 0 || end <= start || end > text.length()) {
                throw new IllegalStateException("source offsets unavailable for " + tree.getKind());
            }
            String owner = owners.isEmpty() ? "unit:" + sourcePath : String.join("/", owners);
            String qualifiedSignature = safe(owner + "::" + signature);
            String identity = kind.name() + "\n" + qualifiedSignature;
            int occurrence = occurrences.merge(identity, 1, Integer::sum);
            String id = "m3:java:" + sha256("M3-JAVA-ATOM-ID/1\n" + sourcePath + "\n"
                    + identity + "\n" + occurrence + "\n");
            String sourceSlice = text.substring((int) start, (int) end);
            atoms.add(new Atom(id, sourcePath, kind, owner, qualifiedSignature,
                    sha256(sourceSlice), start, end));
        }
    }

    private static String safe(String value) {
        return value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ').strip();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("JDK is required to provide SHA-256", exception);
        }
    }
}
