// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.parser.openrewrite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.Modifier;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

/**
 * Optional typed projection of an existing OpenRewrite Java compilation unit.
 *
 * <p>OpenRewrite owns parsing, attribution, rewriting and printing. This adapter
 * does none of them. It visits existing J occurrences once and publishes only
 * detached scalar metadata in the existing M3ASTIndex. No J, JavaType, Cursor or
 * marker is retained. Parent and enclosing coordinates are occurrence rows,
 * never globally interned identities.
 *
 * <p>An external J may already have been rewritten. Its immutable M3File argument
 * therefore does not prove that this tree represents those source characters.
 * Every row has SOURCE_UNVERIFIED and SYNTAX_ONLY, and all ranges/lines are -1.
 * Source positions are never guessed from printed text, UUIDs or checksums.
 * Enclosing types/executables name strict represented declaration/lambda ancestors;
 * they do not substitute for OpenRewrite's semantic owner or binding model.
 */
public final class M3OpenRewriteIngress {
    private M3OpenRewriteIngress() { }

    /** Project an already parsed tree, refusing input, depth and occurrence budget overflow. */
    public static M3ASTIndex openrewriteJ(M3File source,J.CompilationUnit root,M3ASTIngress.Limits limits) {
        Objects.requireNonNull(source,"source");
        Objects.requireNonNull(root,"OpenRewrite compilation unit");
        Objects.requireNonNull(limits,"limits");
        if(source.length()>limits.maxSourceUnits()) throw new IllegalArgumentException("OpenRewrite source budget");
        M3ASTIndex.Builder output=M3ASTIndex.builder(source,M3ASTIndex.Domain.OPENREWRITE,limits.maxNodes());
        Pending pending=new Pending(limits.maxNodes());
        pending.push(root,-1,-1,-1,1,0);
        Children collector=new Children(limits.maxNodes());
        while(pending.size>0) {
            int slot=--pending.size;
            J node=pending.nodes[slot];
            pending.nodes[slot]=null;
            int level=pending.depths[slot];
            if(level>limits.maxDepth()) throw new IllegalArgumentException("OpenRewrite depth budget");
            int type=pending.types[slot], executable=pending.executables[slot];
            int flags=M3ASTIndex.SYNTAX_ONLY|M3ASTIndex.SOURCE_UNVERIFIED;
            if(node instanceof J.Unknown || node instanceof J.Unknown.Source) flags|=M3ASTIndex.RECOVERED;
            Literal literal=literal(node);
            int row=output.add(pending.parents[slot],kind(node,literal.kind()),-1,-1,flags);
            long modifiers=modifiers(node);
            if(node instanceof J.VariableDeclarations.NamedVariable) modifiers=pending.modifiers[slot];
            output.details(row,-1,-1,name(node),literal.kind(),literal.value(),modifiers,type,executable);
            if(node instanceof J.ClassDeclaration) type=row;
            if(node instanceof J.MethodDeclaration || node instanceof J.Lambda) executable=row;
            collector.children.clear();
            node.acceptJava(collector,null);
            long inherited=node instanceof J.VariableDeclarations ? modifiers : 0;
            for(int child=collector.children.size()-1;child>=0;child--) {
                pending.push(collector.children.get(child),row,type,executable,level+1,inherited);
            }
        }
        return output.seal();
    }

    /** Uses the original visitor's immediate-child contract without recursive descent. */
    private static final class Children extends JavaVisitor<Void> {
        private final int maxNodes;
        private final ArrayList<J> children=new ArrayList<>();
        private Children(int maxNodes) { this.maxNodes=maxNodes; }
        @Override public J visit(Tree tree,Void unused) {
            if(tree==null) return null;
            if(!(tree instanceof J java)) throw new IllegalArgumentException("non-J child in Java compilation unit");
            if(children.size()==maxNodes) throw new IllegalArgumentException("OpenRewrite child budget");
            children.add(java);
            return java;
        }
        @Override public Space visitSpace(Space space,Space.Location location,Void unused) { return space; }
        @Override public JavaType visitType(JavaType type,Void unused) { return type; }
        @Override public Markers visitMarkers(Markers markers,Void unused) { return markers; }
    }

    private static final class Pending {
        private final int maxNodes;
        private J[] nodes;
        private int[] parents,types,executables,depths;
        private long[] modifiers;
        private int size;
        private Pending(int maxNodes) {
            this.maxNodes=maxNodes;
            int initial=Math.min(maxNodes,32);
            nodes=new J[initial];parents=new int[initial];types=new int[initial];
            executables=new int[initial];depths=new int[initial];modifiers=new long[initial];
        }
        private void push(J node,int parent,int type,int executable,int depth,long modifierBits) {
            if(size==nodes.length) {
                int capacity=Math.min(maxNodes,nodes.length+(nodes.length>>>1)+1);
                if(capacity<=size) throw new IllegalArgumentException("OpenRewrite pending occurrence budget");
                nodes=Arrays.copyOf(nodes,capacity);parents=Arrays.copyOf(parents,capacity);
                types=Arrays.copyOf(types,capacity);executables=Arrays.copyOf(executables,capacity);
                depths=Arrays.copyOf(depths,capacity);modifiers=Arrays.copyOf(modifiers,capacity);
            }
            nodes[size]=node;parents[size]=parent;types[size]=type;executables[size]=executable;
            depths[size]=depth;modifiers[size]=modifierBits;size++;
        }
    }

    private static String name(J node) {
        if(node instanceof J.ClassDeclaration declaration) return declaration.getSimpleName();
        if(node instanceof J.MethodDeclaration declaration) return declaration.getSimpleName();
        if(node instanceof J.VariableDeclarations.NamedVariable variable) return variable.getSimpleName();
        if(node instanceof J.Identifier identifier) return identifier.getSimpleName();
        if(node instanceof J.MethodInvocation invocation) return invocation.getSimpleName();
        if(node instanceof J.FieldAccess access) return access.getSimpleName();
        return null;
    }

    private static String kind(J node,M3ASTIndex.LiteralKind literal) {
        if(node instanceof J.CompilationUnit) return "COMPILATION_UNIT";
        if(node instanceof J.ClassDeclaration declaration) return switch(declaration.getKind()) {
            case Class -> "CLASS";
            case Interface -> "INTERFACE";
            case Enum -> "ENUM";
            case Annotation -> "ANNOTATION_TYPE";
            case Record -> "RECORD";
            default -> "OPENREWRITE_ClassDeclaration_"+declaration.getKind().name();
        };
        if(node instanceof J.MethodDeclaration) return "METHOD";
        if(node instanceof J.VariableDeclarations.NamedVariable) return "VARIABLE";
        if(node instanceof J.Identifier) return "IDENTIFIER";
        if(node instanceof J.Literal) return literal.name()+"_LITERAL";
        if(node instanceof J.MethodInvocation) return "METHOD_INVOCATION";
        if(node instanceof J.FieldAccess) return "MEMBER_SELECT";
        if(node instanceof J.ForLoop) return "FOR_LOOP";
        if(node instanceof J.ForEachLoop) return "ENHANCED_FOR_LOOP";
        if(node instanceof J.WhileLoop) return "WHILE_LOOP";
        if(node instanceof J.DoWhileLoop) return "DO_WHILE_LOOP";
        if(node instanceof J.Return) return "RETURN";
        if(node instanceof J.Lambda) return "LAMBDA_EXPRESSION";
        if(node instanceof J.NewClass) return "NEW_CLASS";
        String full=node.getClass().getName();
        String prefix=J.class.getName()+"$";
        return "OPENREWRITE_"+(full.startsWith(prefix)?full.substring(prefix.length()):full).replace('$','_');
    }

    private static long modifiers(J node) {
        List<J.Modifier> values=List.of();
        long bits=0;
        if(node instanceof J.ClassDeclaration declaration) {
            values=declaration.getModifiers();
            if(declaration.getKind()==J.ClassDeclaration.Kind.Type.Interface
                    || declaration.getKind()==J.ClassDeclaration.Kind.Type.Annotation) bits|=M3ASTIndex.INTERFACE;
        } else if(node instanceof J.MethodDeclaration declaration) values=declaration.getModifiers();
        else if(node instanceof J.VariableDeclarations declaration) values=declaration.getModifiers();
        else if(node instanceof J.Modifier modifier) values=List.of(modifier);
        for(J.Modifier modifier:values) bits|=M3ASTIndex.modifierBit(switch(modifier.getType()) {
            case Public -> Modifier.PUBLIC;
            case Protected -> Modifier.PROTECTED;
            case Private -> Modifier.PRIVATE;
            case Abstract -> Modifier.ABSTRACT;
            case Static -> Modifier.STATIC;
            case Final -> Modifier.FINAL;
            case Transient -> Modifier.TRANSIENT;
            case Volatile -> Modifier.VOLATILE;
            case Synchronized -> Modifier.SYNCHRONIZED;
            case Native -> Modifier.NATIVE;
            case Strictfp -> Modifier.STRICTFP;
            case Default -> Modifier.DEFAULT;
            case Sealed -> Modifier.SEALED;
            case NonSealed -> Modifier.NON_SEALED;
            default -> throw new IllegalArgumentException("non-Java OpenRewrite modifier: "+modifier.getType().name());
        });
        return bits;
    }

    private record Literal(M3ASTIndex.LiteralKind kind,String value) { }
    private static final Literal NO_LITERAL=new Literal(M3ASTIndex.LiteralKind.NONE,null);

    private static Literal literal(J node) {
        if(!(node instanceof J.Literal literal)) return NO_LITERAL;
        Object value=literal.getValue();
        JavaType.Primitive type=Objects.requireNonNull(literal.getType(),"literal primitive type");
        return switch(type) {
            case Null -> { require(value==null); yield new Literal(M3ASTIndex.LiteralKind.NULL,null); }
            case Boolean -> { require(value instanceof Boolean); yield new Literal(M3ASTIndex.LiteralKind.BOOLEAN,value.toString()); }
            case Char -> { require(value instanceof Character); yield new Literal(M3ASTIndex.LiteralKind.CHAR,String.valueOf((Character)value)); }
            case String -> { require(value instanceof String); yield new Literal(M3ASTIndex.LiteralKind.STRING,(String)value); }
            case Byte,Short,Int -> { require(value instanceof Byte || value instanceof Short || value instanceof Integer);
                yield new Literal(M3ASTIndex.LiteralKind.INT,Integer.toString(((Number)value).intValue())); }
            case Long -> { require(value instanceof Long); yield new Literal(M3ASTIndex.LiteralKind.LONG,Long.toString((Long)value)); }
            case Float -> { require(value instanceof Float); yield new Literal(M3ASTIndex.LiteralKind.FLOAT,
                    Integer.toUnsignedString(Float.floatToRawIntBits((Float)value))); }
            case Double -> { require(value instanceof Double); yield new Literal(M3ASTIndex.LiteralKind.DOUBLE,
                    Long.toUnsignedString(Double.doubleToRawLongBits((Double)value))); }
            default -> throw new IllegalArgumentException("unsupported OpenRewrite literal type: "+type.name());
        };
    }

    private static void require(boolean condition) {
        if(!condition) throw new IllegalArgumentException("OpenRewrite literal value/type mismatch");
    }
}
