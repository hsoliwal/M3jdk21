// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import jdk.m3.collections.M3ProgressMonitor;
import java.io.IOException;
import java.util.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Restricted, javac-attributed path-sensitive local reference facts.
 * Unsupported effects fail closed. No object ownership, GC, JNI pin,
 * reference-count elimination, or source-transformation authority.
 */
public final class CRefPathCertainty {
    private CRefPathCertainty() { }
    private static final int MAX_DEPTH=8, MAX_STATEMENTS=256, MAX_ORIGINS=128;

    public record Snapshot(long position, String phase, int[] mayLocal, int[] mustLocal) {
        public Snapshot {
            mayLocal=Objects.requireNonNull(mayLocal).clone();
            mustLocal=Objects.requireNonNull(mustLocal).clone();
            Objects.requireNonNull(phase);
            if(mayLocal.length==0 || mustLocal.length!=mayLocal.length)
                throw new IllegalArgumentException("alias-count dimensions");
            for(int i=0;i<mayLocal.length;i++)
                if(mayLocal[i]<0 || mustLocal[i]<0 || mustLocal[i]>mayLocal[i])
                    throw new IllegalArgumentException("invalid may/must alias counts");
            if(mustLocal[0]!=0)throw new IllegalArgumentException("unknown cannot be a definite origin");
        }
        @Override public int[] mayLocal(){return mayLocal.clone();}
        @Override public int[] mustLocal(){return mustLocal.clone();}
        /** Local diagnostic only; no heap uniqueness or ownership assertion. */
        public boolean singleDefiniteLocalCandidate(int origin){
            return origin>0 && origin<mayLocal.length && mayLocal[0]==0
                && mayLocal[origin]==1 && mustLocal[origin]==1;
        }
        public boolean authorizesReclamation(){return false;}
    }
    public record Method(String source, String signature, long position,
            List<Snapshot> snapshots, List<String> unresolved) {
        public Method {
            snapshots=List.copyOf(snapshots);
            unresolved=List.copyOf(unresolved);
        }
        public boolean complete(){return unresolved.isEmpty();}
        public boolean authorizesReclamation(){return false;}
    }
    public record Result(String sourceDigest,String referenceProofKey,String flowProofKey,
            List<Method> methods) {
        public Result { methods=List.copyOf(methods); }
        public boolean authorizesReclamation(){return false;}
        public boolean authorizesSourceMutation(){return false;}
    }

    public static Result analyze(Map<String,String> sources, ReferenceFlow.Result refs,
            SourceLiveness.Result flow, M3ProgressMonitor monitor) throws IOException {
        Objects.requireNonNull(sources); Objects.requireNonNull(refs); Objects.requireNonNull(flow);
        // Original reference-flow authority verifies compiler snapshot and CFG.
        ReferenceFlow.project(refs, flow, monitor);
        Map<String,ReferenceFlow.MethodReferences> known=new HashMap<>();
        for(var method:refs.methods()) {
            if(known.putIfAbsent(key(method.source(),method.signature(),method.position()),method)!=null)
                throw new IllegalArgumentException("duplicate compiler method identity");
        }
        // Preserve the compiler's CFG completeness per method, not just the source digest.
        Map<String,SourceLiveness.MethodFlow> cfgMethods=new HashMap<>();
        for(var method:flow.methods()) {
            if(cfgMethods.putIfAbsent(key(method.source(),method.signature(),method.position()),method)!=null)
                throw new IllegalArgumentException("duplicate compiler CFG method");
        }
        List<Method> results=new ArrayList<>();
        var audit=CompilerFactsAudit.analyze(sources,monitor,(unit,trees,progress)-> {
            new TreePathScanner<Void,Void>() {
                @Override public Void visitMethod(MethodTree tree, Void unused){
                    if(progress!=null)progress.checkCanceled();
                    // Same inventory admission as ReferenceFlow. Do not
                    // invent source identity for synthesized constructors.
                    if(trees.getSourcePositions().getEndPosition(unit,tree)<0)
                        return super.visitMethod(tree,unused);
                    Element symbol=trees.getElement(getCurrentPath());
                    if(symbol==null)throw new IllegalArgumentException("unattributed method");
                    String source=unit.getSourceFile().getName();
                    String signature=symbol.getEnclosingElement()+"#"+symbol;
                    long position=trees.getSourcePositions().getStartPosition(unit,tree);
                    var original=known.remove(key(source,signature,position));
                    if(original==null)
                        throw new IllegalArgumentException("method not in compiler origin ledger");
                    var cfg=cfgMethods.remove(key(source,signature,position));
                    if(cfg==null)throw new IllegalArgumentException("method missing original CFG");
                    if(!original.complete() || !cfg.complete()) {
                        List<String> unresolved=new ArrayList<>(original.unresolved());
                        unresolved.addAll(cfg.unresolved());
                        if(unresolved.isEmpty())unresolved.add("original source CFG is incomplete");
                        results.add(new Method(source,signature,position,List.of(),unresolved));
                        return super.visitMethod(tree,unused);
                    }
                    try {
                        results.add(new Runner(unit,trees,getCurrentPath(),tree,original,progress)
                                .run(source,signature,position));
                    } catch(Rejected refusal) {
                        results.add(new Method(source,signature,position,List.of(),List.of(refusal.getMessage())));
                    }
                    return super.visitMethod(tree,unused);
                }
            }.scan(unit,null);
        });
        if(!audit.baselineCompiles() || !audit.sourceDigest().equals(refs.facts().sourceDigest())
                || !audit.proofKey().equals(refs.facts().proofKey()) || !known.isEmpty() || !cfgMethods.isEmpty())
            throw new IllegalArgumentException("compiler reference source/proof drift");
        return new Result(audit.sourceDigest(),refs.proofKey(),flow.proofKey(),results);
    }
    private static String key(String source,String signature,long position){
        return source.length()+":"+source+signature.length()+":"+signature+":"+position;
    }
    private static final class Rejected extends RuntimeException {
        private static final long serialVersionUID=1L;
        Rejected(String reason){super(reason);}
    }
    private record Value(BitSet may,BitSet must) {
        Value { may=(BitSet)may.clone(); must=(BitSet)must.clone(); }
        static Value empty(){return new Value(new BitSet(),new BitSet());}
        static Value external(){
            BitSet may=new BitSet(); may.set(0);
            return new Value(may,new BitSet());
        }
        static Value allocation(int site){
            BitSet origins=new BitSet(); origins.set(site);
            return new Value(origins,origins);
        }
        Value join(Value other){
            BitSet mayJoin=(BitSet)may.clone(); mayJoin.or(other.may);
            BitSet mustJoin=(BitSet)must.clone(); mustJoin.and(other.must);
            return new Value(mayJoin,mustJoin);
        }
    }
    private static final class Runner {
        final CompilationUnitTree unit;
        final Trees trees;
        final MethodTree tree;
        final ReferenceFlow.MethodReferences original;
        final M3ProgressMonitor monitor;
        final Map<Long,Integer> siteByPosition=new HashMap<>();
        final List<Snapshot> snapshots=new ArrayList<>();
        int statements;
        Runner(CompilationUnitTree unit,Trees trees,TreePath methodPath,MethodTree tree,
                ReferenceFlow.MethodReferences original,M3ProgressMonitor monitor){
            this.unit=unit;this.trees=trees;this.tree=tree;
            this.original=original;this.monitor=monitor;
            for(var site:original.sites()) {
                if(site.origin()<1 || site.origin()>MAX_ORIGINS
                        || siteByPosition.putIfAbsent(site.position(),site.origin())!=null)
                    throw new Rejected("unsupported allocation origin ledger");
            }
        }
        Method run(String source,String signature,long position){
            if(tree.getBody()==null || tree.getReturnType()==null
                    || tree.getReturnType().getKind()!=Tree.Kind.PRIMITIVE_TYPE
                    || ((PrimitiveTypeTree)tree.getReturnType()).getPrimitiveTypeKind()!=TypeKind.VOID
                    || !tree.getModifiers().getFlags().contains(Modifier.STATIC)
                    || tree.getModifiers().getFlags().contains(Modifier.SYNCHRONIZED))
                throw new Rejected("restricted static void body required");
            Map<VariableElement,Value> state=new LinkedHashMap<>();
            for(var param:tree.getParameters()){
                VariableElement symbol=variable(param);
                TypeMirror type=symbol.asType();
                if(type.getKind()==TypeKind.BOOLEAN)continue;
                if(!primitiveArray(type))throw new Rejected("unsupported parameter type");
                state.put(symbol,Value.external());
            }
            apply(tree.getBody(),state,0,true);
            return new Method(source,signature,position,snapshots,List.of());
        }
        private VariableElement variable(Tree tree){
            TreePath path=TreePath.getPath(unit,tree);
            Element symbol=path==null?null:trees.getElement(path);
            if(!(symbol instanceof VariableElement v))
                throw new Rejected("unattributed local symbol");
            return v;
        }
        private boolean primitiveArray(TypeMirror type){
            if(!(type instanceof ArrayType array))return false;
            // A Java primitive-array component includes boolean, byte,
            // short, char, int, long, float and double. This original
            // compiler type fact is safer than a hand-maintained whitelist.
            return array.getComponentType().getKind().isPrimitive();
        }
        private long position(Tree node){
            return trees.getSourcePositions().getStartPosition(unit,node);
        }
        private boolean pureBooleanCondition(ExpressionTree predicate,int depth){
            if(depth>MAX_DEPTH)return false;
            if(predicate instanceof ParenthesizedTree parent)
                return pureBooleanCondition(parent.getExpression(),depth+1);
            if(predicate instanceof LiteralTree literal)
                return literal.getValue() instanceof Boolean;
            if(predicate instanceof IdentifierTree){
                VariableElement symbol=variable(predicate);
                return symbol.getKind()==javax.lang.model.element.ElementKind.PARAMETER
                        && symbol.asType().getKind()==TypeKind.BOOLEAN;
            }
            if(predicate instanceof UnaryTree unary && unary.getKind()==Tree.Kind.LOGICAL_COMPLEMENT)
                return pureBooleanCondition(unary.getExpression(),depth+1);
            if(predicate instanceof BinaryTree binary
                    && (binary.getKind()==Tree.Kind.CONDITIONAL_AND
                        || binary.getKind()==Tree.Kind.CONDITIONAL_OR))
                return pureBooleanCondition(binary.getLeftOperand(),depth+1)
                        && pureBooleanCondition(binary.getRightOperand(),depth+1);
            return false;
        }
        // A three-valued, compiler-attributed static Boolean signal:
        // +1 is true on every path, -1 false on every path, 0 unknown.
        // The predicate must FIRST pass pureBooleanCondition; even a
        // short-circuited or dead operand may not hide unsupported effects.
        private int staticBoolean(ExpressionTree predicate,int depth){
            if(depth>MAX_DEPTH)throw new Rejected("Boolean precompute depth budget exceeded");
            if(predicate instanceof ParenthesizedTree parent)
                return staticBoolean(parent.getExpression(),depth+1);
            if(predicate instanceof LiteralTree literal
                    && literal.getValue() instanceof Boolean flag)
                return flag ? 1 : -1;
            if(predicate instanceof IdentifierTree) {
                VariableElement symbol=variable(predicate);
                if(symbol.getKind()!=javax.lang.model.element.ElementKind.PARAMETER
                        || symbol.asType().getKind()!=TypeKind.BOOLEAN)
                    throw new Rejected("predicate not an attributed primitive Boolean parameter");
                return 0;
            }
            if(predicate instanceof UnaryTree unary
                    && unary.getKind()==Tree.Kind.LOGICAL_COMPLEMENT)
                return -staticBoolean(unary.getExpression(),depth+1);
            if(predicate instanceof BinaryTree binary) {
                int left=staticBoolean(binary.getLeftOperand(),depth+1);
                int right=staticBoolean(binary.getRightOperand(),depth+1);
                if(binary.getKind()==Tree.Kind.CONDITIONAL_AND)
                    return left==-1 || right==-1 ? -1
                            : left==1 && right==1 ? 1 : 0;
                if(binary.getKind()==Tree.Kind.CONDITIONAL_OR)
                    return left==1 || right==1 ? 1
                            : left==-1 && right==-1 ? -1 : 0;
            }
            throw new Rejected("Boolean precompute requires effect-free admitted syntax");
        }

        private Value expression(ExpressionTree expr,Map<VariableElement,Value> state){
            return expression(expr,state,0);
        }
        private Value expression(ExpressionTree expr,Map<VariableElement,Value> state,int depth){
            if(depth>MAX_DEPTH)throw new Rejected("conditional expression depth budget exceeded");
            if(expr instanceof ParenthesizedTree parent)
                return expression(parent.getExpression(),state,depth+1);
            if(expr instanceof ConditionalExpressionTree conditional){
                if(!pureBooleanCondition(conditional.getCondition(),depth+1))
                    throw new Rejected("effectful conditional alias predicate");
                // Both arms MUST be modeled before admitting either, even
                // where the static predicate is constant. This preserves
                // refusal for unsupported effects in untaken source syntax.
                Value left=expression(conditional.getTrueExpression(),state,depth+1);
                Value right=expression(conditional.getFalseExpression(),state,depth+1);
                int truth=staticBoolean(conditional.getCondition(),depth+1);
                return truth==1 ? left : truth==-1 ? right : left.join(right);
            }
            if(expr instanceof LiteralTree literal && literal.getValue()==null)
                return Value.empty();
            if(expr instanceof IdentifierTree) {
                Value value=state.get(variable(expr));
                if(value==null)throw new Rejected("uninitialized or external local reference");
                return value;
            }
            if(expr instanceof NewArrayTree array) {
                TreePath path=TreePath.getPath(unit,expr);
                TypeMirror type=path==null?null:trees.getTypeMirror(path);
                if(type==null || !primitiveArray(type) || array.getInitializers()!=null
                        || array.getDimensions().size()!=1
                        || !(array.getDimensions().getFirst() instanceof LiteralTree dimension)
                        || !(dimension.getValue() instanceof Integer size)
                        || size<0 || size>64)
                    throw new Rejected("only bounded primitive array allocations supported");
                Integer origin=siteByPosition.get(position(expr));
                if(origin==null)throw new Rejected("allocation not in compiler origin ledger");
                return Value.allocation(origin);
            }
            throw new Rejected("unmodelled alias source expression");
        }
        private void apply(StatementTree statement,Map<VariableElement,Value> state,
                int depth,boolean emit){
            if(monitor!=null)monitor.checkCanceled();
            if(depth>MAX_DEPTH || ++statements>MAX_STATEMENTS)
                throw new Rejected("path budget exceeded");
            if(statement instanceof BlockTree block){
                // Environments are insertion-ordered LinkedHashMaps. Track
                // lexical declarations without manufacturing live aliases
                // beyond the end of a nested block.
                int outerSlots=state.size();
                for(StatementTree child:block.getStatements())
                    apply(child,state,depth+1,emit);
                if(state.size()>outerSlots){
                    var cursor=state.keySet().iterator();
                    for(int index=0;cursor.hasNext();index++){
                        cursor.next();
                        if(index>=outerSlots)cursor.remove();
                    }
                    if(emit && depth>0)
                        snapshots.add(snapshot(position(statement),"SCOPE_EXIT",state));
                }
                return;
            }
            if(statement instanceof VariableTree declaration){
                VariableElement symbol=variable(declaration);
                if(!primitiveArray(symbol.asType()) || state.containsKey(symbol)
                        || declaration.getInitializer()==null)
                    throw new Rejected("unmodelled local declaration");
                state.put(symbol,expression(declaration.getInitializer(),state));
                if(emit)snapshots.add(snapshot(position(statement),"DECLARE",state));
                return;
            }
            if(statement instanceof ExpressionStatementTree expr
                    && expr.getExpression() instanceof AssignmentTree assignment
                    && assignment.getVariable() instanceof IdentifierTree){
                VariableElement symbol=variable(assignment.getVariable());
                if(!state.containsKey(symbol))
                    throw new Rejected("assignment to undeclared local");
                state.put(symbol,expression(assignment.getExpression(),state));
                if(emit)snapshots.add(snapshot(position(statement),"ASSIGN",state));
                return;
            }
            if(statement instanceof IfTree conditional){
                if(!pureBooleanCondition(conditional.getCondition(),0))
                    throw new Rejected("effectful if predicate");
                Map<VariableElement,Value> left=new LinkedHashMap<>(state);
                Map<VariableElement,Value> right=new LinkedHashMap<>(state);
                apply(conditional.getThenStatement(),left,depth+1,false);
                if(conditional.getElseStatement()!=null)
                    apply(conditional.getElseStatement(),right,depth+1,false);
                if(!left.keySet().equals(state.keySet())
                        || !right.keySet().equals(state.keySet()))
                    throw new Rejected("branch-local declaration unavailable");
                int truth=staticBoolean(conditional.getCondition(),0);
                for(var symbol:state.keySet()){
                    // Literal/pure tautologies are definite compile-time
                    // path facts; unknown conditions retain MAY union and
                    // MUST intersection as in the original model.
                    Value fromTrue=left.get(symbol),fromFalse=right.get(symbol);
                    state.put(symbol,truth==1 ? fromTrue
                            : truth==-1 ? fromFalse : fromTrue.join(fromFalse));
                }
                if(emit)snapshots.add(snapshot(position(statement),"IF_JOIN",state));
                return;
            }
            if(statement instanceof EmptyStatementTree)return;
            throw new Rejected("unsupported effect or control-flow statement: "+statement.getKind());
        }
        private Snapshot snapshot(long position,String phase,Map<VariableElement,Value> state){
            int[] may=new int[original.sites().size()+1];
            int[] must=new int[may.length];
            for(Value value:state.values()){
                for(int i=value.may.nextSetBit(0);i>=0;i=value.may.nextSetBit(i+1)){
                    if(i>=may.length)throw new Rejected("origin outside ledger");
                    may[i]++;
                }
                for(int i=value.must.nextSetBit(1);i>=0;i=value.must.nextSetBit(i+1)){
                    if(i>=must.length)throw new Rejected("origin outside ledger");
                    must[i]++;
                }
            }
            if(snapshots.size()>=MAX_STATEMENTS)
                throw new Rejected("snapshot budget");
            return new Snapshot(position,phase,may,must);
        }
    }
}
