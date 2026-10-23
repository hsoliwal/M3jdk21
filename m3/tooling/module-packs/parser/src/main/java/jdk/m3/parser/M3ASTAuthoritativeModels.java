// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026-10-20: consume the original JDT rewrite plan without another parse.
package jdk.m3.parser;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Detached projections of *already constructed* parser/tooling-owned models.
 *
 * <p>Callers supply the original JDT/Groovy/Kotlin/Maven/Gradle objects. This
 * class never creates a second parser, invokes a Gradle task, resolves a Maven
 * artifact or retains a foreign compiler object. Only bounded primitive-node
 * coordinates and the existing immutable M3File owner survive publication.
 * Optional libraries are accessed through their public APIs at the invocation
 * boundary, without imposing a new JMOD dependency on every application.
 */
public final class M3ASTAuthoritativeModels {
    private M3ASTAuthoritativeModels() { }

    /**
     * Consume the original JDT DOM tree and exact source that was captured for it.
     * The calling JDT working-copy/ASTParser generation remains authoritative.
     * This overload refuses a changed source snapshot instead of using ranges
     * from an unrelated JDT AST.
     */
    public static M3ASTIndex eclipseJdt(M3File source, Object originalTree,
            CharSequence parsedSource, M3ASTIngress.Limits limits) {
        check(source, limits);
        Objects.requireNonNull(parsedSource, "JDT original parse characters");
        if (!contentEquals(source.text(), parsedSource)) {
            throw new IllegalArgumentException("JDT source generation changed");
        }
        return M3ASTIngress.eclipseJdt(source, originalTree, limits);
    }

    /**
     * Snapshot the structural result of an original JDT ASTRewrite plan without
     * applying text edits or parsing again. Child replacements and rewritten
     * lists are read from JDT's existing event owner; single-node copy/move
     * placeholders resolve through that rewriter's original-node accessor.
     * No foreign nodes or rewrite plan are retained by the returned index.
     *
     * <p>The source is the immutable <em>original parse</em> snapshot, not an
     * emitted document. All output ranges are unknown, including unchanged
     * nodes: their positions in the planned document have not been measured.
     * Simple properties, comments and binding objects remain with JDT. Existing
     * binding results still describe JDT's original resolution context; this
     * structural snapshot does not resolve transformed names or dependencies.
     *
     * <p>The caller must exclusively own the AST and plan during this call and
     * pass its current {@code AST.modificationCount()} after constructing all
     * planned new nodes. The counter is checked before and after traversal to
     * reject physical AST generation drift. It does not version rewrite-plan
     * edits, so callers must not cache a plan snapshot by that counter or source
     * hash alone. Every changed plan requires a new snapshot. Raw-string, group
     * and multi-node range placeholders are refused because they have no single
     * parsed semantic node. Single-node placeholders must belong to this plan.
     *
     * <p>This capability requires the qualified JDT core's public
     * {@code ASTRewrite.getOriginalNode(ASTNode)} method; older providers are
     * refused without a fallback parse. The existing parse-origin overload is
     * unchanged and remains available with those providers.
     */
    public static M3ASTIndex eclipseJdtRewrite(M3File source, Object originalTree,
            CharSequence parsedSource, Object rewrite, long expectedAstGeneration,
            M3ASTIngress.Limits limits) {
        check(source, limits);
        Objects.requireNonNull(parsedSource, "JDT original parse characters");
        if (!contentEquals(source.text(), parsedSource)) {
            throw new IllegalArgumentException("JDT source generation changed");
        }
        if (expectedAstGeneration < 0) throw new IllegalArgumentException("negative JDT AST generation");
        Class<?> nodeType = requireType(originalTree, "org.eclipse.jdt.core.dom.ASTNode");
        Class<?> rewriteType = requireType(rewrite, "org.eclipse.jdt.core.dom.rewrite.ASTRewrite");
        try {
            ClassLoader loader = nodeType.getClassLoader();
            Class<?> propertyType = load(loader, "org.eclipse.jdt.core.dom.StructuralPropertyDescriptor");
            Class<?> listPropertyType = load(loader, "org.eclipse.jdt.core.dom.ChildListPropertyDescriptor");
            Class<?> listRewriteType = load(loader, "org.eclipse.jdt.core.dom.rewrite.ListRewrite");
            Method astOwner = nodeType.getMethod("getAST");
            Object ast = astOwner.invoke(originalTree);
            if (ast != rewriteType.getMethod("getAST").invoke(rewrite)) {
                throw new IllegalArgumentException("JDT rewrite belongs to another AST");
            }
            Method generation = load(loader, "org.eclipse.jdt.core.dom.AST").getMethod("modificationCount");
            if ((Long) generation.invoke(ast) != expectedAstGeneration) {
                throw new IllegalArgumentException("JDT AST generation changed");
            }
            Method original = rewriteType.getMethod("getOriginalNode", nodeType);
            Method properties = nodeType.getMethod("structuralPropertiesForType");
            Method isChild = propertyType.getMethod("isChildProperty");
            Method isList = propertyType.getMethod("isChildListProperty");
            Method getChild = rewriteType.getMethod("get", nodeType, propertyType);
            Method getList = rewriteType.getMethod("getListRewrite", nodeType, listPropertyType);
            Method rewrittenList = listRewriteType.getMethod("getRewrittenList");
            Method getFlags = nodeType.getMethod("getFlags");
            int recoveredBits = nodeType.getField("RECOVERED").getInt(null)
                    | nodeType.getField("MALFORMED").getInt(null);
            M3ASTIndex.Builder out = M3ASTIndex.builder(source,
                    M3ASTIndex.Domain.ECLIPSE_JDT, limits.maxNodes());
            int capacity = Math.min(32, limits.maxNodes());
            Object[] pending = new Object[capacity];
            int[] parents = new int[capacity], depths = new int[capacity];
            pending[0] = originalTree; parents[0] = -1; depths[0] = 1;
            int size = 1, appended = 0;
            while (size != 0) {
                Object planned = pending[--size];
                int parent = parents[size], depth = depths[size];
                pending[size] = null;
                if (depth > limits.maxDepth()) throw new IllegalArgumentException("JDT rewrite depth budget");
                if (!nodeType.isInstance(planned)) throw new IllegalArgumentException("foreign JDT rewrite child");
                Object node = original.invoke(rewrite, planned);
                int flags = M3ASTIndex.SYNTAX_ONLY | M3ASTIndex.SOURCE_UNVERIFIED;
                if (((Integer) getFlags.invoke(node) & recoveredBits) != 0) flags |= M3ASTIndex.RECOVERED;
                int row = out.add(parent, "JDT_" + node.getClass().getSimpleName(), -1, -1, flags);
                appended++;
                List<?> descriptors = (List<?>) properties.invoke(node);
                // Reverse-push directly from JDT lists; do not copy child payloads.
                for (int property = descriptors.size() - 1; property >= 0; property--) {
                    Object descriptor = descriptors.get(property);
                    boolean list = (Boolean) isList.invoke(descriptor);
                    if (!list && !(Boolean) isChild.invoke(descriptor)) continue;
                    List<?> children = list
                            ? (List<?>) rewrittenList.invoke(getList.invoke(rewrite, node, descriptor)) : null;
                    Object single = list ? null : getChild.invoke(rewrite, node, descriptor);
                    int childCount = list ? children.size() : single == null ? 0 : 1;
                    for (int child = childCount - 1; child >= 0; child--) {
                        if (appended + size >= limits.maxNodes()) {
                            throw new IllegalArgumentException("JDT rewrite occurrence budget");
                        }
                        if (size == pending.length) {
                            int next = Math.min(limits.maxNodes(), pending.length + (pending.length >>> 1) + 1);
                            pending = Arrays.copyOf(pending, next);
                            parents = Arrays.copyOf(parents, next);
                            depths = Arrays.copyOf(depths, next);
                        }
                        pending[size] = list ? children.get(child) : single;
                        parents[size] = row;
                        depths[size++] = depth + 1;
                    }
                }
            }
            if ((Long) generation.invoke(ast) != expectedAstGeneration) {
                throw new IllegalArgumentException("JDT AST generation changed during projection");
            }
            return out.seal();
        } catch (ReflectiveOperationException problem) {
            Throwable reason = problem instanceof InvocationTargetException wrapped
                    ? wrapped.getCause() : problem;
            throw new IllegalArgumentException("JDT original rewrite owner is missing/incompatible", reason);
        }
    }

    /**
     * Project an actual Maven 3.x ModelBuildingResult. The *effective* model,
     * with its inherited/activated results, is the Maven ModelBuilder's result,
     * not a value inferred from raw XML. All positions intentionally stay unknown
     * because effective models can combine multiple source POMs and profiles.
     * Maven 4's distinct API needs an independently qualified provider.
     */
    public static M3ASTIndex mavenEffective(M3File source, Object modelBuildingResult,
            M3ASTIngress.Limits limits) {
        check(source, limits);
        Class<?> type = requireType(modelBuildingResult,
                "org.apache.maven.model.building.ModelBuildingResult");
        Object model = required(call(type, modelBuildingResult, "getEffectiveModel"),
                "completed Maven effective model");
        M3ASTIndex.Builder out = M3ASTIndex.builder(source,
                M3ASTIndex.Domain.MAVEN_POM, limits.maxNodes());
        int root = unknown(out, -1, "MAVEN_EFFECTIVE_MODEL");
        Object parent = optional(model, "getParent");
        if (parent != null) unknown(out, root, "MAVEN_INHERITED_PARENT");
        for (Object ignored : elements(optional(model, "getModules"))) {
            unknown(out, root, "MAVEN_MODULE");
        }
        append(out, root, optional(model, "getDependencies"), "MAVEN_DEPENDENCY");
        Object managed = optional(model, "getDependencyManagement");
        if (managed != null) {
            int node = unknown(out, root, "MAVEN_DEPENDENCY_MANAGEMENT");
            append(out, node, optional(managed, "getDependencies"), "MAVEN_MANAGED_DEPENDENCY");
        }
        Object build = optional(model, "getBuild");
        if (build != null) {
            int buildNode = unknown(out, root, "MAVEN_BUILD");
            for (Object plugin : elements(optional(build, "getPlugins"))) {
                int pluginNode = unknown(out, buildNode, "MAVEN_PLUGIN");
                append(out, pluginNode, optional(plugin, "getExecutions"), "MAVEN_PLUGIN_EXECUTION");
                append(out, pluginNode, optional(plugin, "getDependencies"), "MAVEN_PLUGIN_DEPENDENCY");
            }
            Object pluginManagement = optional(build, "getPluginManagement");
            if (pluginManagement != null) {
                int node = unknown(out, buildNode, "MAVEN_PLUGIN_MANAGEMENT");
                append(out, node, optional(pluginManagement, "getPlugins"), "MAVEN_MANAGED_PLUGIN");
            }
        }
        append(out, root, optional(model, "getProfiles"), "MAVEN_PROFILE");
        append(out, root, optional(model, "getRepositories"), "MAVEN_REPOSITORY");
        append(out, root, optional(model, "getPluginRepositories"), "MAVEN_PLUGIN_REPOSITORY");
        return out.seal();
    }

    /**
     * Consume Gradle's original read-only Tooling API GradleProject model. A
     * caller obtains this from a real Gradle connection; no build/daemon/task is
     * created here. This is the *resolved project hierarchy*, not a source AST.
     */
    public static M3ASTIndex gradleTooling(M3File source, Object gradleProject,
            M3ASTIngress.Limits limits) {
        Objects.requireNonNull(source, "source");
        String path = source.uri().getPath();
        return gradleTooling(source, gradleProject,
                path != null && path.endsWith(".gradle.kts"), limits);
    }

    /** Retain the original build-script language for read-only Tooling models. */
    public static M3ASTIndex gradleTooling(M3File source, Object gradleProject,
            boolean kotlin, M3ASTIngress.Limits limits) {
        check(source, limits);
        Class<?> type = requireType(gradleProject,
                "org.gradle.tooling.model.GradleProject");
        M3ASTIndex.Builder out = M3ASTIndex.builder(source,
                kotlin ? M3ASTIndex.Domain.GRADLE_KOTLIN : M3ASTIndex.Domain.GRADLE_GROOVY,
                limits.maxNodes());
        Object[] projects = new Object[16];
        int[] parents = new int[16], depths = new int[16];
        int size = 1;
        projects[0] = gradleProject;
        parents[0] = -1;
        depths[0] = 1;
        while (size != 0) {
            Object original = projects[--size];
            int parent = parents[size], depth = depths[size];
            projects[size] = null;
            if (depth > limits.maxDepth()) throw new IllegalArgumentException("Gradle project depth budget");
            if (!type.isInstance(original)) throw new IllegalArgumentException("foreign Gradle model child");
            Object projectName = call(type, original, "getName");
            if (!(projectName instanceof String name) || name.isBlank()) {
                throw new IllegalArgumentException("unnamed original Gradle project");
            }
            int row = unknown(out, parent, "GRADLE_TOOLING_PROJECT");
            append(out, row, call(type, original, "getTasks"), "GRADLE_TOOLING_TASK");
            List<?> children = elements(call(type, original, "getChildren"));
            for (int i = children.size() - 1; i >= 0; i--) {
                if (size == projects.length) {
                    projects = Arrays.copyOf(projects, projects.length * 2);
                    parents = Arrays.copyOf(parents, parents.length * 2);
                    depths = Arrays.copyOf(depths, depths.length * 2);
                }
                projects[size] = children.get(i);
                parents[size] = row;
                depths[size++] = depth + 1;
            }
        }
        return out.seal();
    }

    /**
     * Project Groovy's original ModuleNode from an already parsed Gradle Groovy
     * script. A bounded whitelist walks only original downward AST getters;
     * compiler and DSL semantics remain Groovy/Gradle-owned. Line/column ranges
     * are not claimed exact source spans without parser-origin verification.
     */
    public static M3ASTIndex gradleGroovyAst(M3File source, Object module,
            M3ASTIngress.Limits limits) {
        check(source, limits);
        Class<?> moduleType = requireType(module, "org.codehaus.groovy.ast.ModuleNode");
        Class<?> nodeType = load(moduleType.getClassLoader(), "org.codehaus.groovy.ast.ASTNode");
        M3ASTIndex.Builder out = M3ASTIndex.builder(source,
                M3ASTIndex.Domain.GRADLE_GROOVY, limits.maxNodes());
        Object[] pending = new Object[16];
        int[] parents = new int[16], depths = new int[16];
        pending[0] = module;
        parents[0] = -1;
        depths[0] = 1;
        int size = 1;
        ArrayList<Object> children = new ArrayList<>();
        while (size != 0) {
            Object value = pending[--size];
            int parent = parents[size], depth = depths[size];
            pending[size] = null;
            if (depth > limits.maxDepth()) throw new IllegalArgumentException("Groovy AST depth budget");
            if (!nodeType.isInstance(value)) throw new IllegalArgumentException("foreign Groovy AST node");
            int row = unknown(out, parent, "GROOVY_" + value.getClass().getSimpleName());
            children.clear();
            for (String accessor : groovyChildren(value)) {
                Object child = optional(value, accessor);
                if (nodeType.isInstance(child)) children.add(child);
                else if (child instanceof Iterable<?> list) {
                    for (Object entry : list) if (nodeType.isInstance(entry)) children.add(entry);
                }
            }
            for (int i = children.size() - 1; i >= 0; i--) {
                if (size == pending.length) {
                    pending = Arrays.copyOf(pending, pending.length * 2);
                    parents = Arrays.copyOf(parents, parents.length * 2);
                    depths = Arrays.copyOf(depths, depths.length * 2);
                }
                pending[size] = children.get(i);
                parents[size] = row;
                depths[size++] = depth + 1;
            }
        }
        return out.seal();
    }

    /**
     * Project an original Kotlin KtFile PSI root, supplied by Gradle Kotlin's
     * original compiler. Exact PSI text must equal the M3File source snapshot.
     * Its original TextRange, not a heuristic lexer, supplies UTF-16 offsets.
     * PSI and Kotlin compiler objects are released after traversal.
     */
    public static M3ASTIndex gradleKotlinPsi(M3File source, Object ktFile,
            M3ASTIngress.Limits limits) {
        check(source, limits);
        Class<?> ktType = requireType(ktFile, "org.jetbrains.kotlin.psi.KtFile");
        Object actualText = call(ktType, ktFile, "getText");
        if (!(actualText instanceof String text) || !text.equals(source.text())) {
            throw new IllegalArgumentException("Kotlin PSI source generation mismatch");
        }
        M3ASTIndex.Builder out = M3ASTIndex.builder(source,
                M3ASTIndex.Domain.GRADLE_KOTLIN, limits.maxNodes());
        Object[] pending = new Object[16];
        int[] parents = new int[16], depths = new int[16];
        pending[0] = ktFile; parents[0] = -1; depths[0] = 1;
        int size = 1;
        while (size != 0) {
            Object psi = pending[--size];
            int parent = parents[size], depth = depths[size];
            pending[size] = null;
            if (depth > limits.maxDepth()) throw new IllegalArgumentException("Kotlin PSI depth budget");
            Object range = required(call(psi, "getTextRange"), "Kotlin PSI TextRange");
            int start = (Integer) call(range, "getStartOffset");
            int end = (Integer) call(range, "getEndOffset");
            int row = out.add(parent, "KOTLIN_PSI_" + psi.getClass().getSimpleName(),
                    start, end, M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY);
            Object children = call(psi, "getChildren");
            if (children == null || !children.getClass().isArray()) {
                throw new IllegalArgumentException("Kotlin PSI children are not an array");
            }
            for (int i = Array.getLength(children) - 1; i >= 0; i--) {
                if (size == pending.length) {
                    pending = Arrays.copyOf(pending, pending.length * 2);
                    parents = Arrays.copyOf(parents, parents.length * 2);
                    depths = Arrays.copyOf(depths, depths.length * 2);
                }
                pending[size] = Array.get(children, i);
                parents[size] = row;
                depths[size++] = depth + 1;
            }
        }
        return out.seal();
    }

    private static String[] groovyChildren(Object node) {
        String name = node.getClass().getSimpleName();
        return switch (name) {
            case "ModuleNode" -> new String[] {"getStatementBlock", "getClasses"};
            case "BlockStatement" -> new String[] {"getStatements"};
            case "ExpressionStatement", "ReturnStatement", "ThrowStatement" -> new String[] {"getExpression"};
            case "MethodCallExpression", "StaticMethodCallExpression" ->
                    new String[] {"getObjectExpression", "getMethod", "getArguments"};
            case "ArgumentListExpression", "TupleExpression", "ListExpression", "GStringExpression" ->
                    new String[] {"getExpressions", "getValues"};
            case "BinaryExpression" -> new String[] {"getLeftExpression", "getRightExpression"};
            case "ClosureExpression" -> new String[] {"getCode"};
            case "BooleanExpression", "NotExpression", "UnaryMinusExpression" ->
                    new String[] {"getExpression"};
            case "IfStatement" -> new String[] {"getBooleanExpression", "getIfBlock", "getElseBlock"};
            case "ClassNode" -> new String[] {"getMethods", "getFields"};
            case "MethodNode" -> new String[] {"getCode"};
            case "FieldNode" -> new String[] {"getInitialExpression"};
            default -> new String[0];
        };
    }

    private static void append(M3ASTIndex.Builder out, int parent, Object original, String kind) {
        for (Object ignored : elements(original)) unknown(out, parent, kind);
    }
    private static int unknown(M3ASTIndex.Builder out, int parent, String kind) {
        return out.add(parent, kind, -1, -1, M3ASTIndex.SOURCE_UNVERIFIED);
    }
    private static void check(M3File source, M3ASTIngress.Limits limits) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(limits, "limits");
        if (source.length() > limits.maxSourceUnits()) {
            throw new IllegalArgumentException("AST input exceeds source budget");
        }
    }
    private static boolean contentEquals(String canonical, CharSequence other) {
        if (canonical.length() != other.length()) return false;
        for (int i = 0; i < canonical.length(); i++) if (canonical.charAt(i) != other.charAt(i)) return false;
        return true;
    }
    private static Class<?> requireType(Object value, String name) {
        Objects.requireNonNull(value, "original external parser/model object");
        Class<?> type = load(value.getClass().getClassLoader(), name);
        if (!type.isInstance(value)) throw new IllegalArgumentException("not original " + name);
        return type;
    }
    private static Class<?> load(ClassLoader loader, String name) {
        try { return Class.forName(name, false, loader); }
        catch (ClassNotFoundException missing) { throw new IllegalArgumentException("missing original provider API " + name, missing); }
    }
    private static Object call(Object target, String method) {
        Objects.requireNonNull(target, "original model value");
        return call(target.getClass(), target, method);
    }
    private static Object call(Class<?> type, Object target, String method) {
        try {
            Method m = type.getMethod(method);
            return m.invoke(target);
        } catch (ReflectiveOperationException problem) {
            Throwable reason = problem instanceof InvocationTargetException wrapped
                    ? wrapped.getCause() : problem;
            throw new IllegalArgumentException("original provider accessor " + type.getName()
                    + "." + method + " unavailable", reason);
        }
    }
    private static Object optional(Object target, String method) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(method);
            return m.invoke(target);
        } catch (NoSuchMethodException absent) {
            return null;
        } catch (ReflectiveOperationException problem) {
            Throwable reason = problem instanceof InvocationTargetException wrapped
                    ? wrapped.getCause() : problem;
            throw new IllegalArgumentException("original provider accessor " + method + " failed", reason);
        }
    }
    private static Object required(Object value, String message) {
        if (value == null) throw new IllegalArgumentException(message + " unavailable");
        return value;
    }
    private static List<?> elements(Object value) {
        if (value == null) return List.of();
        if (value instanceof List<?> list) return list;
        if (value instanceof Collection<?> collection) return List.copyOf(collection);
        if (value instanceof Iterable<?> iterable) {
            ArrayList<Object> answer = new ArrayList<>();
            for (Object element : iterable) answer.add(element);
            return answer;
        }
        throw new IllegalArgumentException("provider collection not iterable: " + value.getClass());
    }
}
