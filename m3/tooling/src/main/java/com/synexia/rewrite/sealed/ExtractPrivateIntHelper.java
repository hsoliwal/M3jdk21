// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.sealed;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

/** Original bounded recipe: extract one pure int expression, preserving Java int wrap/shift semantics. */
public final class ExtractPrivateIntHelper implements SealedRecipe {
    private final String path,owner,method;
    private final Identity identity;
    public ExtractPrivateIntHelper(String path,String owner,String method) {
        this.path=SealedSources.path(path); this.owner=identifier(owner);this.method=identifier(method);
        identity=new Identity("com.synexia.ExtractPrivateIntHelper","1/"+SealHash.frame(path,owner,method),
                CodeIdentity.of(ExtractPrivateIntHelper.class,JavaSourceUnit.class,JavaContractSurface.class,SealHash.class),"REFACTOR_PRIVATE_HELPER");
    }
    @Override public Identity identity() { return identity; }
    @Override public Output propose(SealedSources sources,Set<String> writable,SealedContract contract) {
        if (!writable.contains(path)) return Output.hold("TARGET_NOT_WRITABLE");
        if (!contract.permitsPrivateHelpers()) return Output.hold("PRIVATE_OBSERVATION_POLICY_REQUIRED");
        JavaSourceUnit parsed=JavaSourceUnit.parse(sources.require(path));
        List<ClassTree> owners=parsed.unit.getTypeDecls().stream().filter(t -> t instanceof ClassTree)
                .map(t -> (ClassTree)t).filter(t -> t.getSimpleName().contentEquals(owner)).toList();
        if (owners.size()!=1 || owners.getFirst().getKind()!=Tree.Kind.CLASS) return Output.hold("UNSUPPORTED_OWNER");
        ClassTree type=owners.getFirst();
        List<MethodTree> methods=type.getMembers().stream().filter(t -> t instanceof MethodTree).map(t -> (MethodTree)t)
                .filter(t -> t.getName().contentEquals(method)).toList();
        if (methods.size()!=1) return Output.hold("AMBIGUOUS_OR_MISSING_METHOD");
        MethodTree target=methods.getFirst();
        if (target.getBody()==null || !"int".equals(String.valueOf(target.getReturnType()))
                || !target.getModifiers().getFlags().contains(Modifier.STATIC)
                || !target.getTypeParameters().isEmpty() || !target.getThrows().isEmpty()
                || target.getParameters().stream().anyMatch(p -> !"int".equals(p.getType().toString()))
                || target.getBody().getStatements().size()!=1
                || !(target.getBody().getStatements().getFirst() instanceof ReturnTree returned)) {
            return Output.hold("UNSUPPORTED_PURE_INT_METHOD");
        }
        String helper="m3_"+method+"_"+SealHash.frame(owner,method,target.getParameters().toString()).substring(0,12);
        // Idempotence recognizes only the exact helper we previously introduced, never a fuzzy alias.
        String arguments=target.getParameters().stream().map(p -> p.getName().toString()).collect(Collectors.joining(", "));
        String call=helper+"("+arguments+")";
        boolean helperExists=type.getMembers().stream().anyMatch(t -> t instanceof MethodTree m && m.getName().contentEquals(helper));
        if (helperExists && parsed.slice(returned.getExpression()).equals(call)) return Output.unchanged();
        if (helperExists) return Output.hold("PRIVATE_HELPER_NAME_COLLISION");
        Set<String> params=target.getParameters().stream().map(p -> p.getName().toString()).collect(Collectors.toSet());
        int nodes=pure(returned.getExpression(),params,0);
        if (nodes<3) return Output.hold("UNSUPPORTED_OR_TRIVIAL_EXPRESSION");
        String expression=parsed.slice(returned.getExpression());
        int insertion=parsed.end(type)-1;
        if (parsed.source.charAt(insertion)!='}') return Output.hold("UNSUPPORTED_ESCAPED_TYPE_DELIMITER");
        String parameters=target.getParameters().stream().map(p -> "int "+p.getName()).collect(Collectors.joining(", "));
        String addition="/** Pure int arithmetic leaf; parameter values only; Java wrap/shift semantics. */\n"
                +"    private static int "+helper+"("+parameters+") {\n        return "+expression+";\n    }\n";
        String result=parsed.source.substring(0,parsed.start(returned.getExpression()))+call
                +parsed.source.substring(parsed.end(returned.getExpression()),insertion)+addition+parsed.source.substring(insertion);
        if (!JavaContractSurface.equivalent(parsed.source,result)) throw new IllegalStateException("EXTRACTION_SURFACE_DRIFT");
        return new Output(Map.of(path,result),"");
    }
    private static int pure(Tree tree,Set<String> parameters,int depth) {
        if (depth>128) return -1;
        if (tree instanceof IdentifierTree id) return parameters.contains(id.getName().toString())?1:-1;
        if (tree instanceof LiteralTree value) return value.getValue() instanceof Integer?1:-1;
        if (tree instanceof ParenthesizedTree p) return pure(p.getExpression(),parameters,depth+1);
        if (tree instanceof UnaryTree u) {
            if (!Set.of(Tree.Kind.UNARY_PLUS,Tree.Kind.UNARY_MINUS,Tree.Kind.BITWISE_COMPLEMENT).contains(u.getKind())) return -1;
            int operand=pure(u.getExpression(),parameters,depth+1);return operand<0?-1:operand+1;
        }
        if (tree instanceof BinaryTree b) {
            if (!Set.of(Tree.Kind.PLUS,Tree.Kind.MINUS,Tree.Kind.MULTIPLY,Tree.Kind.LEFT_SHIFT,Tree.Kind.RIGHT_SHIFT,
                    Tree.Kind.UNSIGNED_RIGHT_SHIFT,Tree.Kind.AND,Tree.Kind.OR,Tree.Kind.XOR).contains(b.getKind())) return -1;
            int left=pure(b.getLeftOperand(),parameters,depth+1),right=pure(b.getRightOperand(),parameters,depth+1);
            return left<0 || right<0 || left+right>4096?-1:1+left+right;
        }
        return -1;
    }
    private static String identifier(String value) {
        if (value==null || !value.matches("[A-Za-z_$][A-Za-z0-9_$]{0,127}")) throw new IllegalArgumentException("JAVA_IDENTIFIER");
        return value;
    }
}
