// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026-10-20: enforce existing occurrence/depth budgets before unbounded retention.
package jdk.m3.parser;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
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
        int root = unknown(out, -1, "MAVEN_EFFECTIVE_MODEL", limits, 1);
        Object parent = optional(model, "getParent");
        if (parent != null) unknown(out, root, "MAVEN_INHERITED_PARENT", limits, 2);
        for (Object ignored : elements(optional(model, "getModules"), limits.maxNodes())) {
            unknown(out, root, "MAVEN_MODULE", limits, 2);
        }
        append(out, root, optional(model, "getDependencies"), "MAVEN_DEPENDENCY", limits, 2);
        Object managed = optional(model, "getDependencyManagement");
        if (managed != null) {
            int node = unknown(out, root, "MAVEN_DEPENDENCY_MANAGEMENT", limits, 2);
            append(out, node, optional(managed, "getDependencies"), "MAVEN_MANAGED_DEPENDENCY", limits, 3);
        }
        Object build = optional(model, "getBuild");
        if (build != null) {
            int buildNode = unknown(out, root, "MAVEN_BUILD", limits, 2);
            for (Object plugin : elements(optional(build, "getPlugins"), limits.maxNodes())) {
                int pluginNode = unknown(out, buildNode, "MAVEN_PLUGIN", limits, 3);
                append(out, pluginNode, optional(plugin, "getExecutions"), "MAVEN_PLUGIN_EXECUTION", limits, 4);
                append(out, pluginNode, optional(plugin, "getDependencies"), "MAVEN_PLUGIN_DEPENDENCY", limits, 4);
            }
            Object pluginManagement = optional(build, "getPluginManagement");
            if (pluginManagement != null) {
                int node = unknown(out, buildNode, "MAVEN_PLUGIN_MANAGEMENT", limits, 3);
                append(out, node, optional(pluginManagement, "getPlugins"), "MAVEN_MANAGED_PLUGIN", limits, 4);
            }
        }
        append(out, root, optional(model, "getProfiles"), "MAVEN_PROFILE", limits, 2);
        append(out, root, optional(model, "getRepositories"), "MAVEN_REPOSITORY", limits, 2);
        append(out, root, optional(model, "getPluginRepositories"), "MAVEN_PLUGIN_REPOSITORY", limits, 2);
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
        int capacity = Math.min(16, limits.maxNodes());
        Object[] projects = new Object[capacity];
        int[] parents = new int[capacity], depths = new int[capacity];
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
            append(out, row, call(type, original, "getTasks"), "GRADLE_TOOLING_TASK", limits, depth + 1);
            List<?> children = elements(call(type, original, "getChildren"), limits.maxNodes());
            for (int i = children.size() - 1; i >= 0; i--) {
                if (size == projects.length) {
                    int expanded = expandedCapacity(projects.length, limits.maxNodes());
                    projects = Arrays.copyOf(projects, expanded);
                    parents = Arrays.copyOf(parents, expanded);
                    depths = Arrays.copyOf(depths, expanded);
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
        int capacity = Math.min(16, limits.maxNodes());
        Object[] pending = new Object[capacity];
        int[] parents = new int[capacity], depths = new int[capacity];
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
                if (nodeType.isInstance(child)) addChild(children, child, limits.maxNodes());
                else if (child instanceof Iterable<?> list) {
                    for (Object entry : list) {
                        if (nodeType.isInstance(entry)) addChild(children, entry, limits.maxNodes());
                    }
                }
            }
            for (int i = children.size() - 1; i >= 0; i--) {
                if (size == pending.length) {
                    int expanded = expandedCapacity(pending.length, limits.maxNodes());
                    pending = Arrays.copyOf(pending, expanded);
                    parents = Arrays.copyOf(parents, expanded);
                    depths = Arrays.copyOf(depths, expanded);
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
        int capacity = Math.min(16, limits.maxNodes());
        Object[] pending = new Object[capacity];
        int[] parents = new int[capacity], depths = new int[capacity];
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
                    int expanded = expandedCapacity(pending.length, limits.maxNodes());
                    pending = Arrays.copyOf(pending, expanded);
                    parents = Arrays.copyOf(parents, expanded);
                    depths = Arrays.copyOf(depths, expanded);
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

    private static void append(M3ASTIndex.Builder out, int parent, Object original, String kind,
            M3ASTIngress.Limits limits, int depth) {
        for (Object ignored : elements(original, limits.maxNodes())) {
            unknown(out, parent, kind, limits, depth);
        }
    }
    private static int unknown(M3ASTIndex.Builder out, int parent, String kind,
            M3ASTIngress.Limits limits, int depth) {
        if (depth > limits.maxDepth()) throw new IllegalArgumentException("original provider depth budget");
        return unknown(out, parent, kind);
    }
    private static int expandedCapacity(int capacity, int maximum) {
        if (capacity == maximum) throw new IllegalArgumentException("original provider occurrence budget");
        return Math.min(maximum, capacity * 2);
    }
    private static void addChild(ArrayList<Object> children, Object child, int maximum) {
        if (children.size() == maximum) throw new IllegalArgumentException("Groovy child occurrence budget");
        children.add(child);
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
    private static List<?> elements(Object value, int maximum) {
        if (value == null) return List.of();
        if (value instanceof Collection<?> collection && collection.size() > maximum) {
            throw new IllegalArgumentException("original provider collection budget");
        }
        if (value instanceof List<?> list) return list;
        if (value instanceof Iterable<?> iterable) {
            ArrayList<Object> answer = new ArrayList<>();
            Iterator<?> entries = iterable.iterator();
            while (entries.hasNext()) {
                if (answer.size() == maximum) throw new IllegalArgumentException("original provider collection budget");
                Object element = entries.next();
                // Preserve the previous List.copyOf null policy for non-List Collections.
                answer.add(value instanceof Collection<?> ? Objects.requireNonNull(element) : element);
            }
            return answer;
        }
        throw new IllegalArgumentException("provider collection not iterable: " + value.getClass());
    }
}
