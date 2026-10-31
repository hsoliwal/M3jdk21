// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.tests;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jdk.m3.parser.M3ASTAuthoritativeModels;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;

/** Real JDT nodes and event stores; exactly one parse supplies every plan below. */
public final class M3JdtRewriteContract {
    private static final M3ASTIngress.Limits LIMITS = new M3ASTIngress.Limits(100_000, 10_000, 100);
    private static final String CODE = "class Sample { void left(){ int n=1; n++; if(n>0) n--; }"
            + " void right(){ return; } }";
    private static final M3File SOURCE = M3File.of(URI.create("memory:/Sample.java"), CODE);
    private static int checks, parses, emissionParserEntries;
    private M3JdtRewriteContract() { }

    private static void check(boolean accepted, String description) {
        checks++;
        if (!accepted) throw new AssertionError(description);
    }
    private static void refuse(Runnable task, String description) {
        int before = M3JdtRewriteAgent.PARSE_ENTRIES.get();
        try { task.run(); throw new AssertionError("admitted " + description); }
        catch (IllegalArgumentException expected) { checks++; }
        check(M3JdtRewriteAgent.PARSE_ENTRIES.get() == before, "refusal reparsed: " + description);
    }
    private static CompilationUnit parseOnce() {
        ASTParser parser = ASTParser.newParser(AST.JLS21);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(CODE.toCharArray());
        parser.setUnitName("Sample.java");
        parser.setEnvironment(new String[0], new String[0], null, true);
        parser.setResolveBindings(true);
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_21, options);
        parser.setCompilerOptions(options);
        parses++;
        CompilationUnit unit = (CompilationUnit) parser.createAST(null);
        for (var problem : unit.getProblems()) check(!problem.isError(), problem.toString());
        return unit;
    }
    private static M3ASTIndex project(CompilationUnit tree, ASTRewrite plan) {
        int before = M3JdtRewriteAgent.PARSE_ENTRIES.get();
        M3ASTIndex index = M3ASTAuthoritativeModels.eclipseJdtRewrite(
                SOURCE, tree, CODE, plan, tree.getAST().modificationCount(), LIMITS);
        check(M3JdtRewriteAgent.PARSE_ENTRIES.get() == before, "projection invoked ASTParser again");
        check(index.source() == SOURCE, "original source owner identity");
        for (int row = 0; row < index.nodeCount(); row++) {
            check(index.start(row) == -1 && index.end(row) == -1 && !index.hasExactRange(row),
                    "planned document ranges must remain unknown");
            check((index.flags(row) & (M3ASTIndex.SYNTAX_ONLY | M3ASTIndex.SOURCE_UNVERIFIED))
                    == (M3ASTIndex.SYNTAX_ONLY | M3ASTIndex.SOURCE_UNVERIFIED), "honest rewrite flags");
        }
        return index;
    }
    private static int count(M3ASTIndex index, String kind) {
        int count = 0;
        for (int row = 0; row < index.nodeCount(); row++) if (index.kind(row).equals(kind)) count++;
        return count;
    }
    private static int nth(M3ASTIndex index, String kind, int ordinal) {
        for (int row = 0; row < index.nodeCount(); row++) {
            if (index.kind(row).equals(kind) && ordinal-- == 0) return row;
        }
        throw new AssertionError("missing " + kind);
    }
    private static List<String> children(M3ASTIndex index, int row) {
        List<String> result = new ArrayList<>();
        for (int child = index.firstChild(row); child != -1; child = index.nextSibling(child)) {
            check(index.parent(child) == row, "occurrence parent follows the plan");
            result.add(index.kind(child));
        }
        check(result.size() == index.childCount(row), "child count and sibling chain agree");
        return result;
    }
    private static void structuralPlans(CompilationUnit tree) throws Exception {
        AST ast = tree.getAST();
        TypeDeclaration type = (TypeDeclaration) tree.types().get(0);
        Block left = type.getMethods()[0].getBody();
        Block right = type.getMethods()[1].getBody();
        ASTNode first = (ASTNode) left.statements().get(0);
        ASTNode increment = (ASTNode) left.statements().get(1);
        ASTNode condition = (ASTNode) left.statements().get(2);
        VariableDeclarationFragment variable = (VariableDeclarationFragment)
                ((VariableDeclarationStatement) first).fragments().get(0);
        IVariableBinding binding = variable.resolveBinding();
        check(binding != null && !binding.isRecovered(), "original binding resolved once");
        M3ASTIndex parsed = M3ASTAuthoritativeModels.eclipseJdt(SOURCE, tree, CODE, LIMITS);
        check(parsed.hasExactRange(0) && parsed.sourceText(0).equals(CODE), "parse-origin range contract");

        ASTRewrite empty = ASTRewrite.create(ast);
        M3ASTIndex unchanged = project(tree, empty);
        check(unchanged.nodeCount() == parsed.nodeCount(), "unchanged plan preserves occurrences");
        try { unchanged.sourceText(0); throw new AssertionError("invented rewrite text"); }
        catch (IllegalStateException expected) { checks++; }

        ASTRewrite insert = ASTRewrite.create(ast);
        EmptyStatement inserted = ast.newEmptyStatement();
        ListRewrite insertList = insert.getListRewrite(left, Block.STATEMENTS_PROPERTY);
        long planGeneration = ast.modificationCount();
        M3ASTIndex beforeInsertion = project(tree, insert);
        insertList.insertLast(inserted, null);
        check(ast.modificationCount() == planGeneration, "plan changes have independent identity");
        M3ASTIndex afterInsertion = project(tree, insert);
        check(count(beforeInsertion, "JDT_EmptyStatement") == 0, "published earlier plan stays immutable");
        check(count(afterInsertion, "JDT_EmptyStatement") == 1, "new planned node indexed without parsing");
        check(insert.getOriginalNode(inserted) == inserted, "new node identity preserved");

        ASTRewrite remove = ASTRewrite.create(ast);
        remove.remove(condition, null);
        check(count(project(tree, remove), "JDT_IfStatement") == 0, "removed subtree excluded");

        ASTRewrite reorder = ASTRewrite.create(ast);
        ASTNode reordered = reorder.createMoveTarget(condition);
        reorder.remove(condition, null);
        reorder.getListRewrite(left, Block.STATEMENTS_PROPERTY).insertFirst(reordered, null);
        check(reorder.getOriginalNode(reordered) == condition, "move origin is the actual node");
        M3ASTIndex reorderedIndex = project(tree, reorder);
        check(children(reorderedIndex, nth(reorderedIndex, "JDT_Block", 0)).equals(List.of(
                "JDT_IfStatement", "JDT_VariableDeclarationStatement", "JDT_ExpressionStatement")),
                "same-parent move order preserved");

        ASTRewrite move = ASTRewrite.create(ast);
        ASTNode moved = move.createMoveTarget(first);
        move.remove(first, null);
        move.getListRewrite(right, Block.STATEMENTS_PROPERTY).insertFirst(moved, null);
        M3ASTIndex movedIndex = project(tree, move);
        check(children(movedIndex, nth(movedIndex, "JDT_Block", 0)).equals(List.of(
                "JDT_ExpressionStatement", "JDT_IfStatement")), "cross-parent source no longer contains move");
        check(children(movedIndex, nth(movedIndex, "JDT_Block", 1)).equals(List.of(
                "JDT_VariableDeclarationStatement", "JDT_ReturnStatement")), "cross-parent target contains move");
        check(first.getParent() == left && left.statements().size() == 3 && right.statements().size() == 1,
                "original DOM ownership remains unchanged");
        check(move.getOriginalNode(moved) == first && variable.resolveBinding() == binding,
                "actual origin and old binding context retained, not rebound");
        // Independent upstream emission oracle, outside the direct node projection.
        // Its formatter may parse snippets internally; report that existing gap.
        Document emitted = new Document(CODE);
        int beforeEmission = M3JdtRewriteAgent.PARSE_ENTRIES.get();
        check(beforeEmission == 1, "all pre-emission plans use the one authoritative parse");
        move.rewriteAST(emitted, JavaCore.getOptions()).apply(emitted);
        emissionParserEntries = M3JdtRewriteAgent.PARSE_ENTRIES.get() - beforeEmission;
        String normalized = emitted.get().replaceAll("\\s+", "");
        check(normalized.contains("voidleft(){n++;if(n>0)n--;}"), "actual JDT edits remove from source parent");
        check(normalized.contains("voidright(){intn=1;return;}"), "actual JDT edits insert at target parent");
        check(parses == 1, "test never reparses emitted document");

        ASTRewrite copy = ASTRewrite.create(ast);
        ASTNode copied = copy.createCopyTarget(condition);
        copy.getListRewrite(right, Block.STATEMENTS_PROPERTY).insertFirst(copied, null);
        check(copy.getOriginalNode(copied) == condition, "copy origin identity");
        M3ASTIndex copiedIndex = project(tree, copy);
        check(count(copiedIndex, "JDT_IfStatement") == 2, "copy creates two indexed occurrences");
        check(children(copiedIndex, nth(copiedIndex, "JDT_Block", 1)).equals(List.of(
                "JDT_IfStatement", "JDT_ReturnStatement")), "copied subtree follows target occurrence");

        ASTRewrite child = ASTRewrite.create(ast);
        PrefixExpression replacement = ast.newPrefixExpression();
        replacement.setOperator(PrefixExpression.Operator.MINUS);
        replacement.setOperand(ast.newNumberLiteral("2"));
        child.set(variable, VariableDeclarationFragment.INITIALIZER_PROPERTY, replacement, null);
        M3ASTIndex childIndex = project(tree, child);
        check(count(childIndex, "JDT_PrefixExpression") == 1, "child property replacement uses ASTRewrite.get");
        check(variable.getInitializer().toString().equals("1"), "original child property unchanged");
        check(Objects.equals(variable.resolveBinding().getKey(), binding.getKey()), "original binding key unchanged");
        check(increment.getParent() == left, "other original symbols and parents remain given");
    }

    private static void refusalPlans(CompilationUnit tree) {
        AST ast = tree.getAST();
        Block left = ((TypeDeclaration) tree.types().get(0)).getMethods()[0].getBody();
        ASTNode first = (ASTNode) left.statements().get(0);
        ASTNode last = (ASTNode) left.statements().get(2);
        ASTRewrite ordinary = ASTRewrite.create(ast);
        long expected = ast.modificationCount();
        refuse(() -> M3ASTAuthoritativeModels.eclipseJdtRewrite(SOURCE, tree, CODE, ordinary,
                expected - 1, LIMITS), "stale AST generation");
        refuse(() -> M3ASTAuthoritativeModels.eclipseJdtRewrite(SOURCE, tree, CODE, ordinary,
                -1, LIMITS), "negative AST generation");
        refuse(() -> M3ASTAuthoritativeModels.eclipseJdtRewrite(
                M3File.of(SOURCE.uri(), CODE + "//new"), tree, CODE, ordinary,
                ast.modificationCount(), LIMITS), "different source generation");
        ASTRewrite foreign = ASTRewrite.create(AST.newAST(AST.JLS21, false));
        refuse(() -> M3ASTAuthoritativeModels.eclipseJdtRewrite(SOURCE, tree, CODE, foreign,
                ast.modificationCount(), LIMITS), "another AST owner");
        refuse(() -> ordinary.getOriginalNode(null), "null original node");
        refuse(() -> ordinary.getOriginalNode(foreign.getAST().newEmptyStatement()), "foreign node");

        ASTRewrite string = ASTRewrite.create(ast);
        ASTNode raw = string.createStringPlaceholder("int hidden=7;", ASTNode.VARIABLE_DECLARATION_STATEMENT);
        string.getListRewrite(left, Block.STATEMENTS_PROPERTY).insertLast(raw, null);
        refuse(() -> string.getOriginalNode(raw), "raw text original node");
        refuse(() -> project(tree, string), "raw text pretending to be semantic nodes");
        ASTRewrite group = ASTRewrite.create(ast);
        ASTNode grouped = group.createGroupNode(new ASTNode[] { ast.newEmptyStatement(), ast.newEmptyStatement() });
        group.getListRewrite(left, Block.STATEMENTS_PROPERTY).insertLast(grouped, null);
        refuse(() -> group.getOriginalNode(grouped), "group original node");
        refuse(() -> project(tree, group), "group pretending to be one semantic node");
        ASTRewrite range = ASTRewrite.create(ast);
        ListRewrite rangeList = range.getListRewrite(left, Block.STATEMENTS_PROPERTY);
        ASTNode rangeCopy = rangeList.createCopyTarget(first, last);
        rangeList.insertLast(rangeCopy, null);
        refuse(() -> range.getOriginalNode(rangeCopy), "multi-node range original node");
        refuse(() -> project(tree, range), "multi-node range pretending to be one semantic node");
        refuse(() -> M3ASTAuthoritativeModels.eclipseJdtRewrite(SOURCE, tree, CODE, ordinary,
                ast.modificationCount(), new M3ASTIngress.Limits(100_000, 1, 100)), "node budget");
        refuse(() -> M3ASTAuthoritativeModels.eclipseJdtRewrite(SOURCE, tree, CODE, ordinary,
                ast.modificationCount(), new M3ASTIngress.Limits(100_000, 10_000, 1)), "depth budget");
        check(parses == 1, "refusals never fall back to another parser");
    }

    public static void main(String[] args) throws Exception {
        check(M3JdtRewriteAgent.active, "real ASTParser entry instrumentation required");
        CompilationUnit tree = parseOnce();
        if (args.length == 1 && args[0].equals("--legacy-provider")) {
            M3ASTIndex parsed = M3ASTAuthoritativeModels.eclipseJdt(SOURCE, tree, CODE, LIMITS);
            check(parsed.hasExactRange(0), "legacy parse-origin API remains supported");
            ASTRewrite original = ASTRewrite.create(tree.getAST());
            refuse(() -> project(tree, original), "provider lacking original-node capability");
            check(M3JdtRewriteAgent.PARSE_ENTRIES.get() == 1, "missing API has no fallback parse");
            check(M3JdtRewriteAgent.SOURCE_BUILT_OWNERS.get() == 0, "unmodified legacy provider qualified");
            System.out.println("M3_JDT_LEGACY_PROVIDER_PASS checks=" + checks + " parserEntries=1");
            return;
        }
        structuralPlans(tree);
        refusalPlans(tree);
        check(parses == 1 && M3JdtRewriteAgent.PARSE_ENTRIES.get() == 1 + emissionParserEntries,
                "no ASTParser entries outside original parse and separately measured upstream renderer");
        check(M3JdtRewriteAgent.SOURCE_BUILT_OWNERS.get() == 2, "both source-built JDT owners loaded");
        System.out.println("M3_JDT_REWRITE_CONTRACT_PASS checks=" + checks
                + " explicitParseEntries=" + parses + " projectionParserEntries=0 emissionParserEntries="
                + emissionParserEntries + " totalParserEntries=" + M3JdtRewriteAgent.PARSE_ENTRIES.get()
                + " sourceBuiltOwners=" + M3JdtRewriteAgent.SOURCE_BUILT_OWNERS.get());
    }
}
