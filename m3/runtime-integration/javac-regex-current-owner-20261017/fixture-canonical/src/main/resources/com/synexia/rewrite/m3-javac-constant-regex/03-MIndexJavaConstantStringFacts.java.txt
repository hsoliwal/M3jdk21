// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.compiler;

import com.synexia.indexstring.MIndexJdkConstantRegexPlan;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Javac-symbol-resolved constant String and regex evidence for one Java source FILE.
 *
 * <p>Source analysis is read-only. The Java compiler owns symbol resolution,
 * overloads and compile-time constants. This class does not rewrite code,
 * initialize Patterns earlier, infer mutable values or authorize a JNI backend.
 * Exact source-byte and UTF-16 length-framed expression digests remain separate.
 *
 * <p>All returned regexes are verified against the running JDK Pattern engine.
 * Invalid constants are recorded as ineligible candidates rather than causing
 * a new compile-time exception in otherwise valid source code.
 */
public final class MIndexJavaConstantStringFacts {
    public enum Operation {
        STRING_MATCHES(true), STRING_SPLIT(true), STRING_REPLACE_ALL(true),
        STRING_REPLACE_FIRST(true), PATTERN_COMPILE(true), PATTERN_MATCHES(true),
        STRING_CONTAINS(false), STRING_STARTS_WITH(false),
        STRING_ENDS_WITH(false), STRING_INDEX_OF(false), STRING_LAST_INDEX_OF(false);

        private final boolean regex;
        Operation(boolean regex) { this.regex = regex; }
        public boolean regex() { return regex; }
    }

    public record Site(Path source, long line, long column, Operation operation,
                       String constant, int flags, boolean eligible,
                       String rejection, String sourceSha256, String planSha256) {
        public Site {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(operation, "operation");
            Objects.requireNonNull(constant, "constant");
            Objects.requireNonNull(rejection, "rejection");
        }

        /** Exact UTF-16 payload encoding; unlike UTF-8, it preserves lone surrogates. */
        public String constantUtf16Hex() {
            StringBuilder hex = new StringBuilder(constant.length() * 4);
            for (int i = 0; i < constant.length(); i++) {
                String value = Integer.toHexString(constant.charAt(i));
                hex.append("0".repeat(4 - value.length())).append(value);
            }
            return hex.toString();
        }

        /**
         * Bind proven, validated regex evidence to the JDK semantic owner.
         * Calling this is explicit; class-init exception timing is unchanged.
         */
        public MIndexJdkConstantRegexPlan prepareRegex() {
            if (!operation.regex() || !eligible) {
                throw new IllegalStateException("unqualified M3 regex evidence");
            }
            return MIndexJdkConstantRegexPlan.compile(constant, flags);
        }

        public String tsv() {
            return line + "\t" + column + "\t" + operation + "\t" + flags
                    + "\t" + eligible + "\t" + constant.length() + "\t"
                    + constantUtf16Hex() + "\t" + planSha256
                    + "\t" + sourceSha256 + "\t" + rejection;
        }
    }

    public record Report(Path source, String sourceSha256, List<Site> sites,
                         int dynamicSites) {
        public Report { sites = List.copyOf(sites); }
        public long eligibleSites() { return sites.stream().filter(Site::eligible).count(); }

        /**
         * Explicitly prepare each distinct, eligible JDK regex exactly once
         * within this report. A fingerprint is an address, not permission to
         * conflate different pattern strings or flags on a hash collision.
         * No process-wide cache or surprising class initialization is added.
         */
        public Map<String, MIndexJdkConstantRegexPlan> preparedRegexPlans() {
            LinkedHashMap<String, MIndexJdkConstantRegexPlan> result = new LinkedHashMap<>();
            for (Site site : sites) {
                if (!site.operation().regex() || !site.eligible()) continue;
                MIndexJdkConstantRegexPlan existing = result.get(site.planSha256());
                if (existing == null) {
                    result.put(site.planSha256(), site.prepareRegex());
                } else if (!existing.expression().equals(site.constant())
                        || existing.flags() != site.flags()) {
                    throw new IllegalStateException("M3 regex fingerprint collision");
                }
            }
            return Collections.unmodifiableMap(result);
        }

        public String tsv() {
            StringBuilder text = new StringBuilder(
                    "line\tcolumn\toperation\tflags\teligible\tutf16Units\tutf16Hex"
                    + "\tplanSha256\tsourceSha256\trejection\n");
            for (Site site : sites) text.append(site.tsv()).append('\n');
            return text.toString();
        }
    }

    private static final int MAX_CONSTANT_UNITS = 8192;
    private static final int MAX_SITES = 10_000;

    private record RegexKey(String source, int flags) {}
    private record Validation(boolean eligible, String rejection) {}

    private static Validation validateRegex(RegexKey key) {
        try {
            Pattern.compile(key.source(), key.flags());
            return new Validation(true, "");
        } catch (PatternSyntaxException invalid) {
            return new Validation(false, "invalid-regex");
        } catch (IllegalArgumentException invalid) {
            return new Validation(false, "invalid-flags");
        }
    }

    private MIndexJavaConstantStringFacts() {}

    /**
     * Compile against the caller's actual Java classpath. An unresolved
     * compiler error refuses admission; no invented type or partial symbol
     * evidence is returned.
     */
    public static Report analyze(Path source, List<String> javacOptions) throws IOException {
        Path file = Objects.requireNonNull(source, "source").toAbsolutePath().normalize();
        Objects.requireNonNull(javacOptions, "javacOptions");
        byte[] bytes = Files.readAllBytes(file);
        String sourceDigest = sha256(bytes);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("full JDK javac required");
        DiagnosticCollector<JavaFileObject> errors = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager =
                 compiler.getStandardFileManager(errors, Locale.ROOT, StandardCharsets.UTF_8)) {
            List<String> options = new ArrayList<>(javacOptions);
            options.add("-proc:none");
            Iterable<? extends JavaFileObject> input =
                    manager.getJavaFileObjectsFromPaths(List.of(file));
            JavacTask task = (JavacTask) compiler.getTask(
                    null, manager, errors, options, null, input);
            var units = task.parse();
            task.analyze();
            for (Diagnostic<? extends JavaFileObject> message : errors.getDiagnostics()) {
                if (message.getKind() == Diagnostic.Kind.ERROR) {
                    throw new IllegalArgumentException("javac refused constant facts: "
                            + message.getLineNumber() + ": " + message.getMessage(Locale.ROOT));
                }
            }
            Trees trees = Trees.instance(task);
            ArrayList<Site> sites = new ArrayList<>();
            Map<RegexKey, Validation> checkedRegex = new HashMap<>();
            int[] skipped = {0};
            for (CompilationUnitTree unit : units) {
                SourcePositions positions = trees.getSourcePositions();
                new TreePathScanner<Void, Void>() {
                    @Override public Void visitMethodInvocation(
                            MethodInvocationTree invocation, Void unused) {
                        Element element = trees.getElement(getCurrentPath());
                        if (!(element instanceof ExecutableElement method)) {
                            return super.visitMethodInvocation(invocation, unused);
                        }
                        Operation operation = operation(method);
                        if (operation == null) {
                            return super.visitMethodInvocation(invocation, unused);
                        }
                        var arguments = invocation.getArguments();
                        if (arguments.isEmpty()) return super.visitMethodInvocation(invocation, unused);
                        Object expression = constant(new TreePath(
                                getCurrentPath(), arguments.get(0)), trees);
                        if (!(expression instanceof String value) || value.length() > MAX_CONSTANT_UNITS) {
                            skipped[0]++;
                            return super.visitMethodInvocation(invocation, unused);
                        }
                        int flags = 0;
                        if (operation == Operation.PATTERN_COMPILE && arguments.size() == 2) {
                            Object maybe = constant(new TreePath(
                                    getCurrentPath(), arguments.get(1)), trees);
                            if (!(maybe instanceof Integer intFlags)) {
                                skipped[0]++;
                                return super.visitMethodInvocation(invocation, unused);
                            }
                            flags = intFlags;
                        }
                        long offset = positions.getStartPosition(unit, invocation);
                        long line = offset < 0 ? -1 : unit.getLineMap().getLineNumber(offset);
                        long col = offset < 0 ? -1 : unit.getLineMap().getColumnNumber(offset);
                        Validation validation = operation.regex()
                                ? checkedRegex.computeIfAbsent(
                                    new RegexKey(value, flags),
                                    MIndexJavaConstantStringFacts::validateRegex)
                                : new Validation(true, "");
                        if (sites.size() == MAX_SITES) {
                            throw new IllegalArgumentException("M3 compile-time evidence site budget");
                        }
                        sites.add(new Site(file, line, col, operation, value,
                                flags, validation.eligible(), validation.rejection(),
                                sourceDigest, digest(operation, value, flags)));
                        return super.visitMethodInvocation(invocation, unused);
                    }
                }.scan(unit, null);
            }
            sites.sort(Comparator.comparingLong(Site::line)
                    .thenComparingLong(Site::column).thenComparing(Site::operation));
            // A source edited concurrently while javac was reading it cannot
            // be assigned the earlier byte-hash as if the facts were proven.
            if (!Arrays.equals(bytes, Files.readAllBytes(file))) {
                throw new IllegalStateException("source changed during M3 javac evidence scan");
            }
            return new Report(file, sourceDigest, sites, skipped[0]);
        }
    }

    private static Operation operation(ExecutableElement method) {
        if (!(method.getEnclosingElement() instanceof TypeElement owner)) return null;
        String name = method.getSimpleName().toString();
        int parameters = method.getParameters().size();
        String className = owner.getQualifiedName().toString();
        if (className.equals("java.lang.String")) {
            if (parameters == 1) {
                return switch (name) {
                    case "matches" -> Operation.STRING_MATCHES;
                    case "split" -> Operation.STRING_SPLIT;
                    case "contains" -> Operation.STRING_CONTAINS;
                    case "startsWith" -> Operation.STRING_STARTS_WITH;
                    case "endsWith" -> Operation.STRING_ENDS_WITH;
                    case "indexOf" -> stringParameter(method) ? Operation.STRING_INDEX_OF : null;
                    case "lastIndexOf" -> stringParameter(method)
                            ? Operation.STRING_LAST_INDEX_OF : null;
                    default -> null;
                };
            }
            if (parameters == 2) {
                return switch (name) {
                    case "split" -> Operation.STRING_SPLIT;
                    case "replaceAll" -> Operation.STRING_REPLACE_ALL;
                    case "replaceFirst" -> Operation.STRING_REPLACE_FIRST;
                    default -> null;
                };
            }
        }
        if (className.equals("java.util.regex.Pattern")) {
            if (name.equals("compile") && (parameters == 1 || parameters == 2)) {
                return Operation.PATTERN_COMPILE;
            }
            if (name.equals("matches") && parameters == 2) return Operation.PATTERN_MATCHES;
        }
        return null;
    }

    private static boolean stringParameter(ExecutableElement method) {
        return method.getParameters().get(0).asType().toString().equals("java.lang.String");
    }

    private static Object constant(TreePath path, Trees trees) {
        if (path == null) return null;
        ExpressionTree tree = (ExpressionTree) path.getLeaf();
        return switch (tree.getKind()) {
            case STRING_LITERAL, CHAR_LITERAL, INT_LITERAL, LONG_LITERAL,
                    BOOLEAN_LITERAL, FLOAT_LITERAL, DOUBLE_LITERAL ->
                    ((LiteralTree) tree).getValue();
            case PARENTHESIZED -> constant(
                    new TreePath(path, ((ParenthesizedTree) tree).getExpression()), trees);
            case IDENTIFIER, MEMBER_SELECT -> {
                Element symbol = trees.getElement(path);
                yield symbol instanceof VariableElement variable ? variable.getConstantValue() : null;
            }
            case PLUS, OR, AND, XOR, LEFT_SHIFT, RIGHT_SHIFT,
                    UNSIGNED_RIGHT_SHIFT -> binary(path, (BinaryTree) tree, trees);
            case UNARY_MINUS, UNARY_PLUS, BITWISE_COMPLEMENT ->
                    unary(path, (UnaryTree) tree, trees);
            case CONDITIONAL_EXPRESSION -> {
                ConditionalExpressionTree conditional = (ConditionalExpressionTree) tree;
                Object condition = constant(new TreePath(path, conditional.getCondition()), trees);
                yield condition instanceof Boolean answer
                        ? constant(new TreePath(path, answer
                            ? conditional.getTrueExpression()
                            : conditional.getFalseExpression()), trees) : null;
            }
            default -> null;
        };
    }

    private static Object binary(TreePath path, BinaryTree expr, Trees trees) {
        Object left = constant(new TreePath(path, expr.getLeftOperand()), trees);
        Object right = constant(new TreePath(path, expr.getRightOperand()), trees);
        if (left == null || right == null) return null;
        if (expr.getKind() == com.sun.source.tree.Tree.Kind.PLUS
                && (left instanceof String || right instanceof String)) {
            return String.valueOf(left) + right;
        }
        if (!(left instanceof Integer a) || !(right instanceof Integer b)) return null;
        return switch (expr.getKind()) {
            case PLUS -> a + b;
            case OR -> a | b;
            case AND -> a & b;
            case XOR -> a ^ b;
            case LEFT_SHIFT -> a << b;
            case RIGHT_SHIFT -> a >> b;
            case UNSIGNED_RIGHT_SHIFT -> a >>> b;
            default -> null;
        };
    }

    private static Object unary(TreePath path, UnaryTree expr, Trees trees) {
        Object input = constant(new TreePath(path, expr.getExpression()), trees);
        if (!(input instanceof Integer value)) return null;
        return switch (expr.getKind()) {
            case UNARY_MINUS -> -value;
            case UNARY_PLUS -> value;
            case BITWISE_COMPLEMENT -> ~value;
            default -> null;
        };
    }

    private static String digest(Operation operation, String constant, int flags) {
        MessageDigest sha = digest();
        // One prepared regex can serve matches/split/replace call sites
        // whose exact UTF-16 pattern and flags are identical.
        framed(sha, operation.regex()
                ? "M3/Java21/regex-plan/v1" : "M3/Java21/literal-plan/v1");
        sha.update((byte) (flags >>> 24));
        sha.update((byte) (flags >>> 16));
        sha.update((byte) (flags >>> 8));
        sha.update((byte) flags);
        framed(sha, constant);
        return HexFormat.of().formatHex(sha.digest());
    }

    private static void framed(MessageDigest digest, String value) {
        int length = value.length();
        for (int shift = 24; shift >= 0; shift -= 8) {
            digest.update((byte) (length >>> shift));
        }
        for (int i = 0; i < length; i++) {
            char unit = value.charAt(i);
            digest.update((byte) (unit >>> 8));
            digest.update((byte) unit);
        }
    }

    private static String sha256(byte[] bytes) {
        return HexFormat.of().formatHex(digest().digest(bytes));
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** Read-only single-source CLI: caller supplies the module classpath to javac. */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: <one Java source file>");
        System.out.print(analyze(Path.of(args[0]), List.of("--release", "21")).tsv());
    }
}
