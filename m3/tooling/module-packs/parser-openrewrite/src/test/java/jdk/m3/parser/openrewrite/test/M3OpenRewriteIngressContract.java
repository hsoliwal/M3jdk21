// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.parser.openrewrite.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import javax.lang.model.element.Modifier;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.internal.RecipeRunException;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

/** Real OpenRewrite fixture parsing; the production ingress must only consume the resulting J. */
public final class M3OpenRewriteIngressContract {
    private static int checks;
    private static int passed;
    private static int failed;

    private M3OpenRewriteIngressContract() { }

    public static void main(String[] args) throws Exception {
        test("existing changed J is authoritative", M3OpenRewriteIngressContract::changedTree);
        test("detached scalar metadata and ownership", M3OpenRewriteIngressContract::metadata);
        test("unknown ranges and source mismatch stay explicit", M3OpenRewriteIngressContract::unknownSource);
        test("exact primitive literal encodings", M3OpenRewriteIngressContract::literalValues);
        test("input node depth and source budgets", M3OpenRewriteIngressContract::budgets);
        test("interface and annotation capabilities", M3OpenRewriteIngressContract::interfaceModifiers);
        test("core optional bridge agrees", M3OpenRewriteIngressContract::bridge);
        test("incompatible literal refuses", M3OpenRewriteIngressContract::badLiteral);
        test("existing J is never printed", M3OpenRewriteIngressContract::neverPrint);
        System.out.println("OPENREWRITE_CONTRACT tests=" + (passed + failed) + " passed=" + passed
                + " failed=" + failed + " checks=" + checks);
        if (failed != 0) throw new AssertionError("OpenRewrite ingress contract failures=" + failed);
    }

    private static void changedTree() throws Exception {
        String text = "class Original { int oldName(){return 7;} }";
        J.CompilationUnit original = parse(text);
        J.CompilationUnit changed = (J.CompilationUnit) new JavaIsoVisitor<Void>() {
            @Override public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Void unused) {
                J.MethodDeclaration visited = super.visitMethodDeclaration(method, unused);
                return visited.withName(visited.getName().withSimpleName("providedTreeName"));
            }
        }.visitNonNull(original, null);
        M3File source = source(text);
        M3ASTIndex index = project(source, changed, M3ASTIngress.Limits.defaults());
        check(index.source() == source, "retains the supplied immutable source owner");
        check(index.domain().name().equals("OPENREWRITE"), "explicit frontend domain");
        check(find(index, "METHOD", "providedTreeName") >= 0, "reads edited J instead of reparsing old text");
        check(find(index, "METHOD", "oldName") == -1, "does not substitute source-text parse");
        check(original.getClasses().getFirst().getBody().getStatements().getFirst()
                instanceof J.MethodDeclaration method && method.getSimpleName().equals("oldName"),
                "original J remains unchanged");
        check(find(index, "METHOD", "providedTreeName") >= 0, "published index remains detached");
    }

    private static void metadata() throws Exception {
        String text = "public final class Sample { private static final int LIMIT=7; "
                + "String answer(){ return \"kept\"; } class Nested { int n(){return 9;} } }";
        M3ASTIndex index = project(source(text), parse(text), M3ASTIngress.Limits.defaults());
        int type = find(index, "CLASS", "Sample");
        int method = find(index, "METHOD", "answer");
        int variable = find(index, "VARIABLE", "LIMIT");
        int literal = literal(index, "STRING", "kept");
        check(type > 0 && method > type && variable > type && literal > method, "named occurrences exist");
        check(((Long) call(index, "modifierBits", type) & bit(Modifier.PUBLIC)) != 0, "public modifier");
        check(((Long) call(index, "modifierBits", type) & bit(Modifier.FINAL)) != 0, "final modifier");
        check(((Long) call(index, "modifierBits", variable) & bit(Modifier.STATIC)) != 0,
                "named variable inherits declaration static modifier");
        check(((Long) call(index, "modifierBits", variable) & bit(Modifier.FINAL)) != 0,
                "named variable inherits declaration final modifier");
        check((Integer) call(index, "enclosingType", literal) == type, "literal type owner");
        check((Integer) call(index, "enclosingExecutable", literal) == method, "literal executable owner");
        int nested = find(index, "CLASS", "Nested");
        int nestedMethod = find(index, "METHOD", "n");
        check((Integer) call(index, "enclosingType", nested) == type, "nested type strict owner");
        check((Integer) call(index, "enclosingType", nestedMethod) == nested, "nested method type owner");
        for (int row = 1; row < index.nodeCount(); row++) {
            check(index.parent(row) >= 0 && index.parent(row) < row, "parent precedes child");
        }
    }

    private static void unknownSource() throws Exception {
        J.CompilationUnit tree = parse("class Actual { int n(){return 11;} }");
        M3ASTIndex index = project(source("different immutable source"), tree, M3ASTIngress.Limits.defaults());
        check(find(index, "CLASS", "Actual") > 0, "mismatched source never silently reparsed");
        for (int row = 0; row < index.nodeCount(); row++) {
            check(index.start(row) == -1 && index.end(row) == -1 && !index.hasExactRange(row),
                    "unavailable positions remain unknown");
            check((Integer) call(index, "startLine", row) == -1
                    && (Integer) call(index, "endLine", row) == -1, "line coordinates remain unknown");
            check((index.flags(row) & M3ASTIndex.SOURCE_UNVERIFIED) != 0, "unverified external source binding");
            check((index.flags(row) & M3ASTIndex.SYNTAX_ONLY) != 0, "no semantic authority claim");
        }
        expect(IllegalStateException.class, () -> index.sourceText(0));
    }

    private static void literalValues() throws Exception {
        String text = "class Literals { Object[] values={null,true,'x',\"\",12,13L,1.25f,2.5d}; }";
        M3ASTIndex index = project(source(text), parse(text), M3ASTIngress.Limits.defaults());
        check(literal(index, "NULL", null) >= 0, "null literal");
        check(literal(index, "BOOLEAN", "true") >= 0, "boolean literal");
        check(literal(index, "CHAR", "x") >= 0, "UTF-16 char literal");
        check(literal(index, "STRING", "") >= 0, "empty String is known, not missing");
        check(literal(index, "INT", "12") >= 0, "integer literal");
        check(literal(index, "LONG", "13") >= 0, "long literal");
        check(literal(index, "FLOAT", Integer.toUnsignedString(Float.floatToRawIntBits(1.25f))) >= 0,
                "raw float bits");
        check(literal(index, "DOUBLE", Long.toUnsignedString(Double.doubleToRawLongBits(2.5d))) >= 0,
                "raw double bits");
    }

    private static void budgets() throws Exception {
        String text = "class B { void m(){ if(true){ while(false){} } } }";
        J.CompilationUnit tree = parse(text);
        expect(IllegalArgumentException.class, () -> project(source(text), tree, new M3ASTIngress.Limits(1,100,100)));
        expect(IllegalArgumentException.class, () -> project(source(text), tree, new M3ASTIngress.Limits(1000,1,100)));
        expect(IllegalArgumentException.class, () -> project(source(text), tree, new M3ASTIngress.Limits(1000,100,1)));
        expect(NullPointerException.class, () -> project(source(text), null, M3ASTIngress.Limits.defaults()));
    }

    private static void interfaceModifiers() throws Exception {
        String text = "interface Api { int value(); } @interface Tag { String value(); }";
        M3ASTIndex index = project(source(text), parse(text), M3ASTIngress.Limits.defaults());
        long interfaceBit = M3ASTIndex.class.getField("INTERFACE").getLong(null);
        check(((Long) call(index,"modifierBits",find(index,"INTERFACE","Api")) & interfaceBit) != 0,
                "interface capability");
        check(((Long) call(index,"modifierBits",find(index,"ANNOTATION_TYPE","Tag")) & interfaceBit) != 0,
                "annotation interface capability");
    }

    private static void bridge() throws Exception {
        String text = "class Bridge {}";
        M3File source = source(text);
        J.CompilationUnit tree = parse(text);
        Method hook = M3ASTIngress.class.getMethod("openrewriteJ", M3File.class, Object.class, M3ASTIngress.Limits.class);
        M3ASTIndex index = (M3ASTIndex) invoke(hook, null, source, tree, M3ASTIngress.Limits.defaults());
        check(find(index,"CLASS","Bridge") > 0, "core delegates to actual optional typed provider");
        expect(IllegalArgumentException.class,
                () -> invoke(hook,null,source,new Object(),M3ASTIngress.Limits.defaults()));
    }

    private static void badLiteral() throws Exception {
        String text = "class Broken { int n=1; }";
        J.CompilationUnit tree = (J.CompilationUnit) new JavaIsoVisitor<Void>() {
            @Override public J.Literal visitLiteral(J.Literal value, Void unused) {
                return value.getType() == JavaType.Primitive.Int ? value.withValue("not an int") : value;
            }
        }.visitNonNull(parse(text), null);
        expect(IllegalArgumentException.class, () -> project(source(text),tree,M3ASTIngress.Limits.defaults()));
    }

    private static void neverPrint() throws Exception {
        String text="class NoPrinting {}";
        J.CompilationUnit tree=parse(text).withMarkers(Markers.build(List.of(new NoPrint(UUID.randomUUID()))));
        try { tree.printAll();throw new AssertionError("fixture print guard did not fire"); }
        catch(RecipeRunException refused) {
            check(refused.getCause() instanceof AssertionError
                    && "printing forbidden".equals(refused.getCause().getMessage()),"actual printer reaches guard");
        }
        M3ASTIndex index=project(source(text),tree,M3ASTIngress.Limits.defaults());
        check(find(index,"CLASS","NoPrinting")>0,"typed ingress never invokes the actual J printer");
    }

    private record NoPrint(UUID id) implements Marker {
        @Override public UUID getId() { return id; }
        @Override public <M extends Marker> M withId(UUID value) { throw new AssertionError("marker mutation forbidden"); }
        @Override public String print(Cursor cursor,UnaryOperator<String> wrapper,boolean verbose) {
            throw new AssertionError("printing forbidden");
        }
    }

    private static M3File source(String text) { return M3File.of(URI.create("memory:/Fixture.java"),text); }

    private static J.CompilationUnit parse(String text) {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError("fixture parser failure",error); });
        try (var trees = JavaParser.fromJavaVersion().build().parse(context,text)) {
            Tree tree = trees.findFirst().orElseThrow();
            if (!(tree instanceof J.CompilationUnit unit)) throw new AssertionError("fixture is not Java J");
            return unit;
        }
    }

    private static M3ASTIndex project(M3File source,J.CompilationUnit tree,M3ASTIngress.Limits limits) throws Exception {
        final Class<?> provider;
        try { provider=Class.forName("jdk.m3.parser.openrewrite.M3OpenRewriteIngress"); }
        catch (ClassNotFoundException missing) { throw new AssertionError("missing optional typed OpenRewrite ingress",missing); }
        Method method=provider.getMethod("openrewriteJ",M3File.class,J.CompilationUnit.class,M3ASTIngress.Limits.class);
        return (M3ASTIndex) invoke(method,null,source,tree,limits);
    }

    private static Object invoke(Method method,Object receiver,Object... arguments) throws Exception {
        try { return method.invoke(receiver,arguments); }
        catch (InvocationTargetException wrapped) {
            Throwable cause=wrapped.getCause();
            if(cause instanceof Error error) throw error;
            if(cause instanceof Exception exception) throw exception;
            throw new AssertionError(cause);
        }
    }

    private static Object call(M3ASTIndex index,String name,int row) throws Exception {
        return invoke(M3ASTIndex.class.getMethod(name,int.class),index,row);
    }

    private static long bit(Modifier modifier) throws Exception {
        return (Long) invoke(M3ASTIndex.class.getMethod("modifierBit",Modifier.class),null,modifier);
    }

    private static int find(M3ASTIndex index,String kind,String name) throws Exception {
        for(int row=0;row<index.nodeCount();row++) {
            if(index.kind(row).equals(kind) && call(index,"name",row).equals(Optional.ofNullable(name))) return row;
        }
        return -1;
    }

    private static int literal(M3ASTIndex index,String kind,String value) throws Exception {
        for(int row=0;row<index.nodeCount();row++) {
            if(call(index,"literalKind",row).toString().equals(kind)
                    && call(index,"literalValue",row).equals(Optional.ofNullable(value))) return row;
        }
        return -1;
    }

    private static void check(boolean condition,String message) {
        checks++;
        if(!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type,Action action) throws Exception {
        try { action.run(); }
        catch(Throwable problem) {
            check(type.isInstance(problem),"expected "+type.getSimpleName()+" but got "+problem);
            return;
        }
        throw new AssertionError("expected "+type.getSimpleName());
    }

    private static void test(String name,Action action) {
        try { action.run();passed++;System.out.println("PASS "+name); }
        catch(Throwable problem) { failed++;System.out.println("FAIL "+name+": "+problem);problem.printStackTrace(System.out); }
    }

    @FunctionalInterface private interface Action { void run() throws Exception; }
}
