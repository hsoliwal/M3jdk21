// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import com.m3.indexdb.M3IndexDbSemanticFingerprint;
import com.m3.indexdb.M3IndexDbSemanticIndex;
import com.m3.indexdb.M3IndexDbSemanticKind;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

/**
 * Non-mutating OpenRewrite semantic-index recipe.
 *
 * <p>Each Java file is analyzed independently. The recipe emits ATOM/FIELD/METHOD/FILE/type/doc
 * rows plus deterministic scope identities for PACKAGE/MODULE/LIBRARY/PROJECT/REPOSITORY. The
 * database materializer deduplicates those scope identities and recomputes their child-first
 * aggregate fingerprints after fan-in.
 */
public final class M3SemanticIndexRecipe extends Recipe {
    private static final String DEFAULT_REPOSITORY = "hsoliwal/M3jdk21";
    private static final String DEFAULT_PROJECT = "M3JDK21";
    private static final String DEFAULT_LIBRARY = "M3JDK21";

    private final String repository;
    private final String project;
    private final String library;

    private transient M3SemanticNodeTable nodes = new M3SemanticNodeTable(this);
    private transient M3SemanticEdgeTable edges = new M3SemanticEdgeTable(this);

    public M3SemanticIndexRecipe() {
        this(DEFAULT_REPOSITORY, DEFAULT_PROJECT, DEFAULT_LIBRARY);
    }

    public M3SemanticIndexRecipe(String repository, String project, String library) {
        this.repository = token(repository, "repository");
        this.project = token(project, "project");
        this.library = token(library, "library");
    }

    @Override
    public String getDisplayName() {
        return "Build M3 semantic atom index";
    }

    @Override
    public String getDescription() {
        return "Inventories Java atoms, fields, methods, files, interface/implementation types, "
                + "documentation and containing scopes with structural, normalized logic and SimHash "
                + "fingerprints; source is never modified.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "inventory",
                "semantic-index",
                "atomization",
                "patternization",
                "iop",
                "logic-hash",
                "structural-hash",
                "simhash",
                "non-mutating",
                "file-local-analysis");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(
                    J.CompilationUnit compilationUnit, ExecutionContext ctx) {
                J.CompilationUnit result = super.visitCompilationUnit(compilationUnit, ctx);
                emit(result, ctx);
                return result;
            }
        };
    }

    private void emit(J.CompilationUnit unit, ExecutionContext ctx) {
        String sourcePath = normalized(unit.getSourcePath());
        String packageName = packageName(unit);
        String moduleName = moduleName(sourcePath);

        Node repositoryNode = scope(M3IndexDbSemanticKind.REPOSITORY, repository, repository, "");
        Node projectNode = scope(
                M3IndexDbSemanticKind.PROJECT,
                repository + "/project/" + project,
                project,
                "");
        Node libraryNode = scope(
                M3IndexDbSemanticKind.LIBRARY,
                repository + "/project/" + project + "/library/" + library,
                library,
                "");
        Node moduleNode = scope(
                M3IndexDbSemanticKind.MODULE,
                repository + "/project/" + project + "/library/" + library + "/module/" + moduleName,
                moduleName,
                "");
        Node packageNode = scope(
                M3IndexDbSemanticKind.PACKAGE,
                moduleNode.semanticKey() + "/package/" + packageName,
                packageName,
                "");

        emitNode(repositoryNode, ctx);
        emitNode(projectNode, ctx);
        emitNode(libraryNode, ctx);
        emitNode(moduleNode, ctx);
        emitNode(packageNode, ctx);
        emitEdge(repositoryNode, projectNode, "PROJECT", 0, ctx);
        emitEdge(projectNode, libraryNode, "LIBRARY", 0, ctx);
        emitEdge(libraryNode, moduleNode, "MODULE", 0, ctx);
        emitEdge(moduleNode, packageNode, "PACKAGE", 0, ctx);

        String fileKey = packageNode.semanticKey() + "/file/" + sourcePath;
        List<Node> fileChildren = new ArrayList<>();

        List<String> comments = comments(unit);
        if (!comments.isEmpty()) {
            M3IndexDbSemanticFingerprint docsFingerprint = M3IndexDbSemanticFingerprint.leaf(
                    "DOCUMENTATION",
                    List.of("COMMENT_COUNT:" + comments.size()),
                    comments,
                    String.join("\n", comments));
            Node docs = node(
                    M3IndexDbSemanticKind.DOCUMENTATION,
                    fileKey + "#docs",
                    sourcePath,
                    "documentation",
                    docsFingerprint);
            emitNode(docs, ctx);
            fileChildren.add(docs);
        }

        for (J.ClassDeclaration declaration : unit.getClasses()) {
            String qualifiedName = qualifiedName(packageName, declaration.getSimpleName());
            Node type = emitType(fileKey, sourcePath, qualifiedName, declaration, ctx);
            fileChildren.add(type);
        }

        M3IndexDbSemanticFingerprint fileFingerprint =
                M3JavaSemanticHasher.fingerprint("FILE", unit);
        Node file = node(M3IndexDbSemanticKind.FILE, fileKey, sourcePath, sourcePath, fileFingerprint);
        emitNode(file, ctx);
        emitEdge(packageNode, file, "FILE", 0, ctx);
        for (int ordinal = 0; ordinal < fileChildren.size(); ordinal++) {
            Node child = fileChildren.get(ordinal);
            emitEdge(file, child, child.kind().name(), ordinal, ctx);
        }
    }

    private Node emitType(
            String fileKey,
            String sourcePath,
            String qualifiedName,
            J.ClassDeclaration declaration,
            ExecutionContext ctx) {
        M3IndexDbSemanticKind kind = declaration.getKind() == J.ClassDeclaration.Kind.Type.Interface
                ? M3IndexDbSemanticKind.INTERFACE
                : M3IndexDbSemanticKind.IMPLEMENTATION;
        String typeKey = repository + "/type/" + qualifiedName;
        List<Node> children = new ArrayList<>();


        for (J statement : declaration.getBody().getStatements()) {
            if (statement instanceof J.VariableDeclarations fields) {
                for (J.VariableDeclarations.NamedVariable variable : fields.getVariables()) {
                    Node field = emitField(
                            typeKey,
                            sourcePath,
                            variable,
                            ctx);
                    children.add(field);
                }
            } else if (statement instanceof J.MethodDeclaration method) {
                Node methodNode = emitMethod(
                        typeKey,
                        sourcePath,
                        method,
                        ctx);
                children.add(methodNode);
            } else if (statement instanceof J.ClassDeclaration nested) {
                Node nestedNode = emitType(
                        fileKey,
                        sourcePath,
                        qualifiedName + "$" + nested.getSimpleName(),
                        nested,
                        ctx);
                children.add(nestedNode);
            }
        }

        M3IndexDbSemanticFingerprint fingerprint =
                M3JavaSemanticHasher.fingerprint(kind.name(), declaration);
        Node type = node(kind, typeKey, sourcePath, qualifiedName, fingerprint);
        emitNode(type, ctx);
        for (int ordinal = 0; ordinal < children.size(); ordinal++) {
            emitEdge(type, children.get(ordinal), children.get(ordinal).kind().name(), ordinal, ctx);
        }
        return type;
    }

    private Node emitField(
            String typeKey,
            String sourcePath,
            J.VariableDeclarations.NamedVariable variable,
            ExecutionContext ctx) {
        String fieldKey = typeKey + "#field/" + variable.getSimpleName();
        List<Node> atoms = new ArrayList<>();
        Expression initializer = variable.getInitializer();
        if (initializer != null) {
            M3IndexDbSemanticFingerprint atomFingerprint =
                    M3JavaSemanticHasher.fingerprint("FIELD_INITIALIZER", initializer);
            Node atom = node(
                    M3IndexDbSemanticKind.ATOM,
                    fieldKey + "#atom/initializer",
                    sourcePath,
                    "FIELD_INITIALIZER",
                    atomFingerprint);
            emitNode(atom, ctx);
            atoms.add(atom);
        }

        M3IndexDbSemanticFingerprint fieldFingerprint =
                M3JavaSemanticHasher.fingerprint("FIELD", variable);
        Node field = node(
                M3IndexDbSemanticKind.FIELD,
                fieldKey,
                sourcePath,
                variable.getSimpleName(),
                fieldFingerprint);
        emitNode(field, ctx);
        for (int index = 0; index < atoms.size(); index++) {
            emitEdge(field, atoms.get(index), "ATOM", index, ctx);
        }
        return field;
    }

    private Node emitMethod(
            String typeKey,
            String sourcePath,
            J.MethodDeclaration method,
            ExecutionContext ctx) {
        String signature = methodSignature(method);
        String methodKey = typeKey + "#method/" + signature;
        List<Node> atoms = new ArrayList<>();
        if (method.getBody() != null) {
            int atomOrdinal = 0;
            for (J statement : method.getBody().getStatements()) {
                M3IndexDbSemanticFingerprint atomFingerprint =
                        M3JavaSemanticHasher.fingerprint("METHOD_ATOM", statement);
                Node atom = node(
                        M3IndexDbSemanticKind.ATOM,
                        methodKey + "#atom/" + atomOrdinal,
                        sourcePath,
                        statement.getClass().getSimpleName(),
                        atomFingerprint);
                emitNode(atom, ctx);
                atoms.add(atom);
                atomOrdinal++;
            }
        }

        M3IndexDbSemanticFingerprint methodFingerprint =
                M3JavaSemanticHasher.fingerprint("METHOD", method);
        Node methodNode =
                node(M3IndexDbSemanticKind.METHOD, methodKey, sourcePath, signature, methodFingerprint);
        emitNode(methodNode, ctx);
        for (int index = 0; index < atoms.size(); index++) {
            emitEdge(methodNode, atoms.get(index), "ATOM", index, ctx);
        }
        return methodNode;
    }

    private void emitNode(Node node, ExecutionContext ctx) {
        nodes.insertRow(
                ctx,
                new M3SemanticNodeTable.Row(
                        node.id(),
                        node.kind().name(),
                        node.semanticKey(),
                        node.sourcePath(),
                        node.symbol(),
                        node.fingerprint()));
    }

    private void emitEdge(Node parent, Node child, String role, int ordinal, ExecutionContext ctx) {
        edges.insertRow(ctx, new M3SemanticEdgeTable.Row(parent.id(), child.id(), role, ordinal));
    }

    private static Node scope(
            M3IndexDbSemanticKind kind,
            String semanticKey,
            String symbol,
            String sourcePath) {
        M3IndexDbSemanticFingerprint fingerprint = M3IndexDbSemanticFingerprint.leaf(
                "SCOPE_IDENTITY:" + kind,
                List.of(kind.name()),
                List.of(semanticKey),
                semanticKey);
        return node(kind, semanticKey, sourcePath, symbol, fingerprint);
    }

    private static Node node(
            M3IndexDbSemanticKind kind,
            String semanticKey,
            String sourcePath,
            String symbol,
            M3IndexDbSemanticFingerprint fingerprint) {
        return new Node(
                M3IndexDbSemanticIndex.nodeId(kind, semanticKey),
                kind,
                semanticKey,
                sourcePath,
                symbol,
                fingerprint);
    }

    private static String qualifiedName(String packageName, String simpleName) {
        return "<default>".equals(packageName)
                ? simpleName
                : packageName + "." + simpleName;
    }

    private static String methodSignature(J.MethodDeclaration method) {
        List<String> parameters = new ArrayList<>();
        for (J parameter : method.getParameters()) {
            if (parameter instanceof J.VariableDeclarations declarations
                    && declarations.getTypeExpression() != null) {
                @SuppressWarnings("deprecation")
                String printed = declarations.getTypeExpression().printTrimmed();
                parameters.add(printed);
            } else {
                parameters.add("?");
            }
        }
        return method.getSimpleName() + "(" + String.join(",", parameters) + ")";
    }

    private static List<String> comments(J.CompilationUnit unit) {
        List<String> result = new ArrayList<>();
        new JavaIsoVisitor<List<String>>() {
            @Override
            public J preVisit(J tree, List<String> sink) {
                for (Comment comment : tree.getPrefix().getComments()) {
                    String text = comment.printComment(getCursor());
                    if (!text.isBlank()) sink.add(text);
                }
                return super.preVisit(tree, sink);
            }
        }.visit(unit, result);
        return List.copyOf(result);
    }

    private static String packageName(J.CompilationUnit unit) {
        if (unit.getPackageDeclaration() == null) return "<default>";
        @SuppressWarnings("deprecation")
        String printed = unit.getPackageDeclaration().getExpression().printTrimmed();
        return printed.isBlank() ? "<default>" : printed;
    }

    private static String moduleName(String sourcePath) {
        String path = sourcePath.replace('\\', '/');
        String[] parts = path.split("/");
        if (parts.length > 1 && "src".equals(parts[0])) return parts[1];

        int marker = path.indexOf("/src/main/java/");
        if (marker > 0) {
            String prefix = path.substring(0, marker);
            int slash = prefix.lastIndexOf('/');
            return slash < 0 ? prefix : prefix.substring(slash + 1);
        }
        if (path.startsWith("test/")) return "test";
        return "<root>";
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String token(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) throw new IllegalArgumentException(field);
        return checked;
    }

    private record Node(
            String id,
            M3IndexDbSemanticKind kind,
            String semanticKey,
            String sourcePath,
            String symbol,
            M3IndexDbSemanticFingerprint fingerprint) {}
}
